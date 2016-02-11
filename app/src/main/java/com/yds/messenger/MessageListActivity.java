package com.yds.messenger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.yds.messenger.listadapters.MessageListAdapter;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageListItem;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.services.DispatchRegUpdateMessageIntentService;
import com.yds.messenger.utils.CheckPlayServices;

import java.io.IOException;
/**
 * Created by yds on 30/11/15.
 */
/**
 * Screen displaying list of all contacts.
 */
public class MessageListActivity extends ActionBarActivity {

    GoogleCloudMessaging gcm;
    Context context;
    String regid;

    String TAG = "yds";

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_top);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_message_list);

        context = this;

        /**
         * Check if play services available
         */
        if (!CheckPlayServices.check(this)) {
            Toast.makeText(this, getString(R.string.play_services_not_supported), Toast.LENGTH_LONG).show();
            finish();
        }

        if (true) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        /**
         * Check whether username is set. If not, prompt for it.
         */
        initialCheck();

        gcm = GoogleCloudMessaging.getInstance(this);

        //Get Registration ID that has been saved by the persistence layer
        regid = getRegistrationId(context);

        /**
         * If not registered, do it
         */
        if (regid.isEmpty()) {
            registerInBackground();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            //Show settings screen
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        } else if (id == R.id.action_addContact) {
            //Show screen that performs pairing
            Intent intent = new Intent(this, AddContactActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check whether username is set, if not prompt for it to be set
     */
    public void initialCheck() {
        String username = Persistence.getUsername(this);
        if (username == null) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Choose username");
            alert.setMessage(getResources().getString(R.string.message_username_not_set));
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String chosenUsername = input.getText().toString();
                    Persistence.setUsername(MessageListActivity.this, chosenUsername);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });
            alert.show();
        }
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    private String getRegistrationId(Context context) {
        String registrationId = Persistence.getRegistrationID(context);
        if (registrationId == null) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = Persistence.getLastVersionNumber(context);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            Persistence.setLastVersionNumber(context, currentVersion);
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask() {
            boolean propagateChange = false;

            @Override
            protected Object doInBackground(Object[] objects) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(Persistence.getSenderID(context));
                    msg = "Device registered, registration ID=" + regid;

                    String oldRegId = Persistence.getRegistrationID(context);
                    if (oldRegId != null) {
                        if (!oldRegId.equals(regid)) {
                            propagateChange = true;
                        }
                    }

                    // Persist the regID - no need to register again.
                    Persistence.setRegistrationID(context, regid);

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(context, getString(R.string.registering_device), Toast.LENGTH_SHORT).show();
                propagateChange = false;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                String msg = o.toString();
//                if (msg.equals(""))
//                    Toast.makeText(context, getString(R.string.registration_completed), Toast.LENGTH_SHORT).show();
//                else
//                    Toast.makeText(context, getString(R.string.registration_error), Toast.LENGTH_SHORT).show();
                if (propagateChange) {
                    Toast.makeText(context, R.string.sending_regid_contacts, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, DispatchRegUpdateMessageIntentService.class);
                    startService(intent);
                }
            }
        }.execute(null, null, null);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_message_list, container, false);

            //Get list of all contacts
            Contact[] contacts = Persistence.getContacts(getActivity());
            MessageListItem[] messages = new MessageListItem[contacts.length];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = new MessageListItem();
                messages[i].contactName = contacts[i].username;
                messages[i].messagePreview = "";
                if (contacts[i].lastMessage != null) {
                    messages[i].messagePreview = contacts[i].lastMessage.messageText;
                }
                messages[i].secretUsername = contacts[i].secretUsername;
                messages[i].registrationID = contacts[i].registrationID;
                messages[i].encKey = contacts[i].encKey;
            }

            ListView messagesList = (ListView) rootView.findViewById(R.id.listView_MessageList);
            MessageListAdapter mla = new MessageListAdapter(getActivity(), messages);
            messagesList.setAdapter(mla);

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

}
/**
 * Created by yds on 30/11/15.
 */