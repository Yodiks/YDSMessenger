package com.yds.messenger;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.yds.messenger.IntentHelpers.IntentCreator;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.Message;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.services.DispatchRejectionMessageIntentService;
import com.yds.messenger.utils.SimpleNotificationMaker;
import com.yds.messenger.utils.SimpleSoundNotificationMaker;
import com.yds.messenger.utils.crypto.EncryptionUtils;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by yds on 30/11/15.
 */
/**
 * Receiver that is triggered when a message is received. It takes appropriate action based on the
 * type and validity of the message
 */
public class GCMBroadcastReceiver extends BroadcastReceiver {
    /**
     * inBackground determines if the receiver is running in the background, or it has been
     * registered by a foreground activity.
     * If in fact an activity has registered the receiver, it suppresses things like notifications
     * and triggers UI update.
     */
    boolean inBackground;

    ChatThreadActivity chatThreadActivity; //Reference to the activity that registered the receiver
    Context context; //Current context

    /**
     * Constructor called when the receiver is being invoked in the background.
     * It sets the inBackground variable to True
     */
    public GCMBroadcastReceiver() {
        inBackground = true;
    }

    /**
     * Constructor called when register is registered from an activity
     * @param notify - if false, no notifications are created for normal messages
     * @param cta - The activity registering the receiver
     */
    public GCMBroadcastReceiver(boolean notify, ChatThreadActivity cta) {
        this.inBackground = notify;
        this.chatThreadActivity = cta;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        this.context = context;
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Persistence.addDebugMessages(context, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                Persistence.addDebugMessages(context, "Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                String recd = extras.getString("data");
                int recdMessageType = -1;
                String recdMessageText = "----";

                //Try parsing the data object received
                try {
                    JSONObject data = new JSONObject(recd);
                    recdMessageType = data.getInt("messageType");
                    recdMessageText = data.getString("messageText");
                } catch (JSONException nfe) {
                    Persistence.addDebugMessages(context, "Coundn't parse message type");
                }

                if (recdMessageType == MessageTypes.DEBUG_MESSAGE) {
                    /**
                     * Debug messages can be used to check if messages are being received correctly
                     */
                    Persistence.addDebugMessages(context, "Received: " + "Message of type: " + recdMessageType + " => " + recdMessageText);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                            new Intent(context, DebugActivity.class), 0);
                    if (inBackground) {
                        SimpleNotificationMaker.sendNotification(context, "Raven: DEBUG MESSAGE", recdMessageText, contentIntent);
                    } else {
                        SimpleSoundNotificationMaker.sendNotification(context);
                    }
                } else if (recdMessageType == MessageTypes.PAIRING_MESSAGE) {
                    /**
                     * Message sent by a device after scanning code.
                     */
                    try {
                        /**
                         * First we check if the message has a cipherText field.
                         * If yes, it is attempting an encrypted connection. Else, it is a
                         * un-encrypted one.
                         */
                        JSONObject recdObject = new JSONObject(recdMessageText);
                        String cipherText = recdObject.optString("cipherText", null);
                        if (cipherText != null) {
                            //Encryption is supported
                            String cachedKey = Persistence.getCachedKey(context);
                            if (cachedKey == null) {
                                //No key cached
                                //Refuse connection
                                String targetRegID = recdObject.getString("registrationID");
                                Intent rejectionIntent = new Intent(context, DispatchRejectionMessageIntentService.class);
                                rejectionIntent.putExtra("registrationID", targetRegID);
                                context.startService(rejectionIntent);
                            } else {
                                //Key is present in cache
                                //Check if the key is the one which the new contact used
                                String plainText = EncryptionUtils.decrypt(cipherText, cachedKey);
                                try {
                                    /**
                                     * Parse necessary fields from the object and save contact
                                     */
                                    JSONObject pairingRequest = new JSONObject(plainText);
                                    Contact newContact = new Contact();
                                    newContact.username = pairingRequest.getString("username");
                                    newContact.secretUsername = pairingRequest.getString("secretUsername");
                                    newContact.registrationID = pairingRequest.getString("registrationID");
                                    newContact.encKey = cachedKey;
                                    Persistence.addNewContact(context, newContact);

                                    Intent chatThreadIntent = IntentCreator.getChatThreadIntent(context, newContact.secretUsername);
                                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                            chatThreadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                                    String notifMessage = String.format(context.getString(R.string.contact_has_paired_encrypted), newContact.username);
                                    SimpleNotificationMaker.sendNotification(context, context.getString(R.string.contact_added), notifMessage, contentIntent);
                                } catch (JSONException je) {
                                    //The key used was not the same
                                    //Refuse connection
                                    String targetRegID = recdObject.getString("registrationID");
                                    Intent rejectionIntent = new Intent(context, DispatchRejectionMessageIntentService.class);
                                    rejectionIntent.putExtra("registrationID", targetRegID);
                                    context.startService(rejectionIntent);
                                }
                            }
                        } else {
                            //Not an encrypted connection
                            JSONObject pairingRequest = new JSONObject(recdMessageText);
                            Contact newContact = new Contact();
                            newContact.username = pairingRequest.getString("username");
                            newContact.secretUsername = pairingRequest.getString("secretUsername");
                            newContact.registrationID = pairingRequest.getString("registrationID");
                            Persistence.addNewContact(context, newContact);
                            Intent chatThreadIntent = IntentCreator.getChatThreadIntent(context, newContact.secretUsername);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    chatThreadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            String notifMessage = String.format(context.getString(R.string.contact_has_paired), newContact.username);
                            SimpleNotificationMaker.sendNotification(context, context.getString(R.string.contact_added), notifMessage, contentIntent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (recdMessageType == MessageTypes.MORNAL_MESSAGE) {
                    /**
                     * Regular person to person message
                     */
                    try {
                        JSONObject newMessage = new JSONObject(recdMessageText);
                        String secretUsername = newMessage.getString("secretUsername");
                        Contact user = Persistence.getUser(context, secretUsername);

                        if (user != null) {
                            String username = user.username;
                            String encKey = user.encKey;
                            Message message = new Message();
                            message.messageText = newMessage.getString("messageText");
                            /**
                             * If encryption key is set for the contact, decrypt the messageText
                             * field
                             */
                            if (encKey != null) {
                                message.messageText = EncryptionUtils.decrypt(message.messageText, encKey);
                            }
                            message.receivedMessage = true;
                            message.timestamp = System.currentTimeMillis();

                            //Save the message against the appropriate contact
                            Persistence.addMessage(context, secretUsername, message);

                            //Trigger notification
                            Intent chatThreadIntent = IntentCreator.getChatThreadIntent(context, secretUsername);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    chatThreadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            if (!inBackground && chatThreadActivity != null && chatThreadActivity.secretUsername.equals(secretUsername)) {
                                SimpleSoundNotificationMaker.sendNotification(context);
                                chatThreadActivity.refreshThread();
                            } else {
                                SimpleNotificationMaker.sendNotification(context, context.getString(R.string.notif_title_messag_recd), username + ": " + message.messageText, contentIntent);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (recdMessageType == MessageTypes.REMOVE_CONTACT) {
                    /**
                     * Message sent for un-pairing
                     */
                    String toBeRemoved = recdMessageText;
                    Contact user = Persistence.getUser(context, toBeRemoved);
                    if (user != null) {
                        String username = user.username;

                        //Create notification informing of the un-pairing
                        String notifMessage = String.format(context.getString(R.string.contact_has_unpaired), username);
                        Intent messageListIntent = new Intent(context, MessageListActivity.class);
                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                messageListIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        SimpleNotificationMaker.sendNotification(context, context.getString(R.string.contact_unpaired), notifMessage, contentIntent);

                        //Delete the contact from message
                        Persistence.clearContact(context, toBeRemoved);
                    }
                } else if (recdMessageType == MessageTypes.REGISTRATION_UPDATE) {
                    /**
                     * Indicates that one of the contacts has had a change in its registration ID
                     */
                    try {
                        JSONObject registrationUpdateOb = new JSONObject(recdMessageText);
                        String contactSecretUsername = registrationUpdateOb.getString("secretUsername");
                        String newRegId = registrationUpdateOb.getString("registrationID");
                        Persistence.updateRegistrationID(context, contactSecretUsername, newRegId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (recdMessageType == MessageTypes.SELF_DESTRUCTING_MESSAGE) {
                    /**
                     * Message that only triggers a notification and a screen to display.
                     * This message is not persisted.
                     */
                    try {
                        JSONObject selfDestructingMessageOb = new JSONObject(recdMessageText);
                        String contactSecretUsername = selfDestructingMessageOb.getString("secretUsername");
                        String message = selfDestructingMessageOb.getString("message");

                        Contact user = Persistence.getUser(context, contactSecretUsername);
                        String encKey = user.encKey;

                        if (encKey != null) {
                            message = EncryptionUtils.decrypt(message, encKey);
                        }

                        int destryAfter = selfDestructingMessageOb.getInt("destroyAfter");
                        Intent selfDestructingMessageIntent = new Intent(context, SelfDestructingMessageDisplay.class);
                        selfDestructingMessageIntent.putExtra("secretUsername", contactSecretUsername);
                        selfDestructingMessageIntent.putExtra("destroyAfter", destryAfter);
                        selfDestructingMessageIntent.putExtra("message", message);
                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                selfDestructingMessageIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        SimpleNotificationMaker.sendNotification(context, context.getString(R.string.read_once_message), context.getString(R.string.can_read_only_once), contentIntent, true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (recdMessageType == MessageTypes.USERNAME_UPDATE) {
                    /**
                     * Indicates that one of the contacts has changed their username.
                     * It triggers a corresponding change in the persistence layer.
                     */
                    try {
                        JSONObject updateUsernameMessageOb = new JSONObject(recdMessageText);
                        String contactSecretUsername = updateUsernameMessageOb.getString("secretUsername");
                        String newUsername = updateUsernameMessageOb.getString("username");
                        String oldUsername = Persistence.getUser(context, contactSecretUsername).username;
                        Persistence.updateContactUsername(context, contactSecretUsername, newUsername);
                        Contact user = Persistence.getUser(context, contactSecretUsername);
                        if (user != null) {
                            String username = user.username;
                            String userRegID = user.registrationID;
                            String encKey = user.encKey;
                            Intent chatThreadIntent = IntentCreator.getChatThreadIntent(context, contactSecretUsername);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    chatThreadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            String message = oldUsername + " " + context.getString(R.string.is_now) + " " + newUsername;
                            SimpleNotificationMaker.sendNotification(context, context.getString(R.string.contact_renamed), message, contentIntent, true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (recdMessageType == MessageTypes.REJECT_CONNECTION_KEY_INVALID) {
                    /**
                     * Indicates that a pairing request this device sent to another device was
                     * rejected by the other device.
                     */
                    try {
                        JSONObject updateUsernameMessageOb = new JSONObject(recdMessageText);
                        String contactSecretUsername = updateUsernameMessageOb.getString("secretUsername");
                        Contact user = Persistence.getUser(context, contactSecretUsername);
                        if (user != null) {
                            String username = user.username;
                            Persistence.clearContact(context, contactSecretUsername);
                            String title = context.getString(R.string.pairing_rejected);
                            String message = String.format(context.getString(R.string.contect_claims_key_expired), username);
                            //Create notification
                            Intent messageListIntent = new Intent(context, MessageListActivity.class);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    messageListIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            SimpleNotificationMaker.sendNotification(context, title, message, contentIntent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //Consume the broadcast
        abortBroadcast();
    }


}
/**
 * Created by yds on 30/11/15.
 */