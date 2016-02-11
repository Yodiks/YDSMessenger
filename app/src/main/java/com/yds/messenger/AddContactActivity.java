package com.yds.messenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.utils.MessageDispatcher;
import com.yds.messenger.utils.StringToQRBitmap;
import com.yds.messenger.utils.crypto.EncryptionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
/**
 * Created by yds on 30/11/15.
 */
/**
 * Activity that shows the Add contact screen
 */
public class AddContactActivity extends ActionBarActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_pair_by_nfc) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
                if (nfc == null) {
                    Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(this, NFCPairing.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, R.string.nfc_version_limit, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            int layout = 0;
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    layout = R.layout.fragment_add_contact_mycode;
                    break;
                case 2:
                    layout = R.layout.fragment_add_contact_scancode;
                    break;
            }

            View rootView = inflater.inflate(layout, container, false);

            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                displayMyCode(rootView);
            } else if (getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
                displayScanCode(rootView);
            }

            return rootView;
        }

        /**
         * Creates a pairing QR code and displays it in a ImageView in the activity
         * @param rootView
         */
        public void displayMyCode(View rootView) {
            /**
             * JSONObject that holds the information needed for pairing
             */
            JSONObject ob = new JSONObject();
            try {
                ob.put("USERNAME", Persistence.getUsername(getActivity()));
                ob.put("SECRET_USERNAME", Persistence.getSecretUsername(getActivity()));
                ob.put("GCM_REG_ID", Persistence.getRegistrationID(getActivity()));
                //Generates a new key
                String encKey = Persistence.getNewKey(getActivity());
                ob.put("ENC_KEY", encKey);
            } catch (JSONException je) {
                je.printStackTrace();
            }
            //Convert from String to a bitmap image of a QR code
            Bitmap bitmap = StringToQRBitmap.sting2QRBitmap(ob.toString());
            ImageView imageview = (ImageView) rootView.findViewById(R.id.imageView_mycode);
            imageview.setImageBitmap(bitmap);

        }

        /**
         * Displays a scan code button that on clicked opens barcode reader
         * @param rootView
         */
        public void displayScanCode(View rootView) {
            //Shows a scan instruction image
            ImageView imageview = (ImageView) rootView.findViewById(R.id.imageView_scanIcon);
            imageview.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.scan1));
            Button scanButton = (Button) rootView.findViewById(R.id.button_startScan);
            scanButton.setOnClickListener(new View.OnClickListener() {
                //Open any available barcode reader app to scan pairing code from another device
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar code
                        startActivityForResult(intent, 0);
                    } catch (Exception e) {
                        Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                        startActivity(marketIntent);

                    }
                }
            });
        }

        /**
         * Called after a barcode is scanned
         * @param requestCode
         * @param resultCode
         * @param data
         */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 0) {

                if (resultCode == RESULT_OK) {
                    String contents = data.getStringExtra("SCAN_RESULT");
                    try {
                        //Get the transferred JSON pairing object
                        JSONObject contactOb = new JSONObject(contents);

                        //If own code is scanned
                        if (contactOb.getString("SECRET_USERNAME").equals(Persistence.getSecretUsername(getActivity()))) {
                            Toast.makeText(getActivity(), R.string.cannot_add_yourself, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        /**
                         * Extract relevant fields from the received object and store permanently
                         */
                        Contact newContact = new Contact();
                        newContact.username = contactOb.getString("USERNAME");
                        newContact.secretUsername = contactOb.getString("SECRET_USERNAME");
                        newContact.registrationID = contactOb.getString("GCM_REG_ID");
                        newContact.encKey = contactOb.optString("ENC_KEY", null);
                        Persistence.addNewContact(getActivity(), newContact);

                        /**
                         * Send pairing request to the new contact so that it can also add this
                         * device
                         */
                        JSONObject pairingRequest = new JSONObject();
                        pairingRequest.put("username", Persistence.getUsername(getActivity()));
                        pairingRequest.put("secretUsername", Persistence.getSecretUsername(getActivity()));
                        pairingRequest.put("registrationID", Persistence.getRegistrationID(getActivity()));
                        String tempPairingMessage = pairingRequest.toString();
                        if (newContact.encKey != null) {
                            String encryptedText = EncryptionUtils.encrypt(tempPairingMessage, newContact.encKey);
                            JSONObject encPairingObject = new JSONObject();
                            encPairingObject.put("cipherText", encryptedText);
                            encPairingObject.put("registrationID", Persistence.getRegistrationID(getActivity()));
                            tempPairingMessage = encPairingObject.toString();
                        }
                        final String pairingMessage = tempPairingMessage;
                        final String targetRegId = newContact.registrationID;
                        final String targetName = newContact.username;
                        new AsyncTask() {
                            private ProgressDialog progressDialog;

                            @Override
                            protected Object doInBackground(Object[] objects) {
                                return MessageDispatcher.dispatchMessage(getActivity(), targetRegId, MessageTypes.PAIRING_MESSAGE, pairingMessage);
                            }

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                progressDialog = new ProgressDialog(getActivity());
                                progressDialog.setMessage("Sending pairing request to " + targetName);
                                progressDialog.show();

                            }

                            @Override
                            protected void onPostExecute(Object o) {
                                super.onPostExecute(o);
                                progressDialog.dismiss();
                                getActivity().finish();
                            }
                        }.execute(null, null, null);
                    } catch (JSONException je) {
                        je.printStackTrace();
                        Toast.makeText(getActivity(), getString(R.string.qr_error), Toast.LENGTH_SHORT).show();
                    }
                }
                if (resultCode == RESULT_CANCELED) {
                    //handle cancel
                }
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_mycode).toUpperCase(l);
                case 1:
                    return getString(R.string.title_scancode).toUpperCase(l);
            }
            return null;
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */