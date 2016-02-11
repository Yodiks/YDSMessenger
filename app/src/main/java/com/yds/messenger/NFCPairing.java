package com.yds.messenger;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.utils.MessageDispatcher;
import com.yds.messenger.utils.crypto.EncryptionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static android.nfc.NfcAdapter.CreateNdefMessageCallback;
import static android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
/**
 * Created by yds on 30/11/15.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NFCPairing extends ActionBarActivity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {

    private static final int MESSAGE_SENT = 1;
    NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcpairing);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            TextView mInfoText = (TextView) findViewById(R.id.textView);
            mInfoText.setText(R.string.nfc_not_available);
        } else {
            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nfcpairing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 0) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        /**
         * Create JSON object that holds fields necessary for pairing
         */
        JSONObject ob = new JSONObject();
        try {
            ob.put("USERNAME", Persistence.getUsername(this));
            ob.put("SECRET_USERNAME", Persistence.getSecretUsername(this));
            ob.put("GCM_REG_ID", Persistence.getRegistrationID(this));
            String encKey = Persistence.getNewKey(this);
            ob.put("ENC_KEY", encKey);
        } catch (JSONException je) {
            je.printStackTrace();
        }
        String text = ob.toString();
        NdefMessage msg = new NdefMessage(NdefRecord.createMime(
                "application/com.sumitgouthaman.raven", text.getBytes()));
        return msg;
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {

    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the pairing related info in the message and performs pairing
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        String pairingString = (new String(msg.getRecords()[0].getPayload()));
        try {
            /**
             * Interpret the JSON object and extract relevant pairing related data
             */
            JSONObject contactOb = new JSONObject(pairingString);

            /**
             * If the person tries adding their own device
             */
            if (contactOb.getString("SECRET_USERNAME").equals(Persistence.getSecretUsername(this))) {
                Toast.makeText(this, R.string.cannot_add_yourself, Toast.LENGTH_SHORT).show();
                return;
            }
            Contact newContact = new Contact();
            newContact.username = contactOb.getString("USERNAME");
            newContact.secretUsername = contactOb.getString("SECRET_USERNAME");
            newContact.registrationID = contactOb.getString("GCM_REG_ID");
            newContact.encKey = contactOb.optString("ENC_KEY", null);
            Persistence.addNewContact(this, newContact);

            /**
             * Create a new pairing request object to be sent over the network to the other person
             */
            JSONObject pairingRequest = new JSONObject();
            pairingRequest.put("username", Persistence.getUsername(this));
            pairingRequest.put("secretUsername", Persistence.getSecretUsername(this));
            pairingRequest.put("registrationID", Persistence.getRegistrationID(this));
            String tempPairingMessage = pairingRequest.toString();
            if (newContact.encKey != null) {
                String encryptedText = EncryptionUtils.encrypt(tempPairingMessage, newContact.encKey);
                JSONObject encPairingObject = new JSONObject();
                encPairingObject.put("cipherText", encryptedText);
                encPairingObject.put("registrationID", Persistence.getRegistrationID(this));
                tempPairingMessage = encPairingObject.toString();
            }
            final String pairingMessage = tempPairingMessage;
            final String targetRegId = newContact.registrationID;
            final String targetName = newContact.username;
            /**
             * Sent the pairing object
             */
            new AsyncTask() {
                private ProgressDialog progressDialog;

                @Override
                protected Object doInBackground(Object[] objects) {
                    return MessageDispatcher.dispatchMessage(NFCPairing.this, targetRegId, MessageTypes.PAIRING_MESSAGE, pairingMessage);
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progressDialog = new ProgressDialog(NFCPairing.this);
                    progressDialog.setMessage("Sending pairing request to " + targetName);
                    progressDialog.show();

                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    progressDialog.dismiss();
                    NFCPairing.this.finish();
                }
            }.execute(null, null, null);
        } catch (JSONException je) {
            je.printStackTrace();
            Toast.makeText(this, getString(R.string.qr_error), Toast.LENGTH_SHORT).show();
        }
    }

}
/**
 * Created by yds on 30/11/15.
 */