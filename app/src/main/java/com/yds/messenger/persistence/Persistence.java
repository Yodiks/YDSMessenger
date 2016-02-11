package com.yds.messenger.persistence;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.yds.messenger.R;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.Message;
import com.yds.messenger.utils.RandomStrings;
import com.yds.messenger.utils.crypto.KeyGeneratorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class that serves as an interface to the persistence mechanism.
 * Currently uses SharedPreferences, Could be migrated to SQLLite later while keeping the interface
 * of the class same.
 */
public class Persistence {

    private static final String key = "yds";
    private static final String[] persistenceKeys = {"USERNAME", "SECRET_USERNAME", "REGISTRATION_ID", "DEBUG_MESSAGES", "LAST_VERSION", "CONTACTS", "KEYCACHE"};

    public static String getUsername(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        return shared.getString("USERNAME", null);
    }

    public static void setUsername(Activity activity, String username) {
        SharedPreferences shared = activity.getSharedPreferences(key, 0);
        SharedPreferences.Editor editor = shared.edit();
        editor.putString("USERNAME", username);
        editor.commit();
    }

    public static void updateContactUsername(Context context, String secretUsername, String newUsername) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String contactsStr = sharedPreferences.getString("CONTACTS", "[]");
        try {
            JSONArray contactsArr = new JSONArray(contactsStr);
            JSONArray newContactsArr = new JSONArray();
            for (int i = 0; i < contactsArr.length(); i++) {
                JSONObject contactOb = contactsArr.getJSONObject(i);
                String targetSecretUsername = contactOb.getString("secretUsername");
                if (targetSecretUsername.equals(secretUsername)) {
                    contactOb.put("username", newUsername);
                }
                newContactsArr.put(contactOb);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("CONTACTS", newContactsArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static String getSecretUsername(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String secretUsername = shared.getString("SECRET_USERNAME", null);
        if (secretUsername != null) {
            return secretUsername;
        } else {
            secretUsername = new RandomStrings(20).nextString();
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("SECRET_USERNAME", secretUsername);
            editor.commit();
            return secretUsername;
        }
    }

    public static String getRegistrationID(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        return shared.getString("REGISTRATION_ID", null);
    }

    public static void setRegistrationID(Context context, String registrationID) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        SharedPreferences.Editor editor = shared.edit();
        editor.putString("REGISTRATION_ID", registrationID);
        editor.commit();
    }

    public static String getSenderID(Context context) {
        return context.getString(R.string.sender_id);
    }

    public static String getAPIKey(Context context) {
        return context.getString(R.string.api_key);
    }

    public static String getDebugMessages(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        String debugMessages = shared.getString("DEBUG_MESSAGES", null);
        String debugTemp = "";
        if (debugMessages != null)
            debugTemp = debugMessages;
        return debugTemp;
    }

    public static void addDebugMessages(Context context, String dm) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        String debugMessages = "";
        try {
            debugMessages = shared.getString("DEBUG_MESSAGES", "");
        } catch (ClassCastException cce) {
            debugMessages = "";
        }
        debugMessages += "\n\n" + (dm);
        SharedPreferences.Editor editor = shared.edit();
        editor.putString("DEBUG_MESSAGES", debugMessages);
        editor.commit();
    }

    public static void clearDebugMessages(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        SharedPreferences.Editor editor = shared.edit();
        editor.putString("DEBUG_MESSAGES", "");
        editor.commit();
    }

    public static int getLastVersionNumber(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        return shared.getInt("LAST_VERSION", Integer.MIN_VALUE);
    }

    public static void setLastVersionNumber(Context context, int lastVersionNumber) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt("LAST_VERSION", lastVersionNumber);
        editor.commit();
    }

    public static void addNewContact(Context context, Contact newContact) {
        Contact alreadyExisting = getUser(context, newContact.secretUsername);
        if (alreadyExisting != null) {
            return;
        }
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        String contactsStr = shared.getString("CONTACTS", "[]");
        try {
            JSONArray contactsArr = new JSONArray(contactsStr);
            JSONObject newContactOb = new JSONObject();
            newContactOb.put("username", newContact.username);
            newContactOb.put("secretUsername", newContact.secretUsername);
            newContactOb.put("registrationID", newContact.registrationID);
            if (newContact.encKey != null) {
                newContactOb.put("encKey", newContact.encKey);
            }
            contactsArr.put(newContactOb);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("CONTACTS", contactsArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static Contact[] getContacts(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        String contactsStr = shared.getString("CONTACTS", "[]");
        try {
            JSONArray contactsArr = new JSONArray(contactsStr);
            Contact[] contacts = new Contact[contactsArr.length()];
            for (int i = 0; i < contacts.length; i++) {
                contacts[i] = new Contact();
                JSONObject contactOb = contactsArr.getJSONObject(i);
                contacts[i].username = contactOb.getString("username");
                contacts[i].secretUsername = contactOb.getString("secretUsername");
                contacts[i].registrationID = contactOb.getString("registrationID");
                contacts[i].lastMessage = getLastMessage(context, contacts[i].secretUsername);
                contacts[i].encKey = contactOb.optString("encKey", null);
            }
            return contacts;
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return new Contact[0];
    }

    public static Contact getUser(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        Contact[] contacts = getContacts(context);
        for (Contact c : contacts) {
            if (c.secretUsername.equals(secretUsername)) {
                return c;
            }
        }
        return null;
    }

    public static void clearContacts(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        Contact[] contacts = getContacts(context);
        for (Contact c : contacts) {
            clearContactMessages(context, c.secretUsername);
        }
        editor.putString("CONTACTS", "[]");
        editor.commit();
    }

    public static void clearContact(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String contactsStr = shared.getString("CONTACTS", "[]");
        try {
            JSONArray contactsArr = new JSONArray(contactsStr);
            JSONArray newContactsArr = new JSONArray();
            for (int i = 0; i < contactsArr.length(); i++) {
                JSONObject contactOb = contactsArr.getJSONObject(i);
                String targetSecretUsername = contactOb.getString("secretUsername");
                if (!targetSecretUsername.equals(secretUsername)) {
                    newContactsArr.put(contactOb);
                }
            }
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("CONTACTS", newContactsArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
        cleanup(context);
    }

    public static Message[] getMessages(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String messageQueueKey = secretUsername + "_QUEUED";
        try {
            JSONArray messagesArr = new JSONArray(shared.getString(secretUsername, "[]"));
            JSONArray queuedMessagesArr = new JSONArray(shared.getString(messageQueueKey, "[]"));
            int l1 = messagesArr.length();
            int l2 = queuedMessagesArr.length();
            Message[] messages = new Message[l1 + l2];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = new Message();
                JSONObject messageOb;
                if (i < l1) {
                    messageOb = messagesArr.getJSONObject(i);
                } else {
                    messageOb = queuedMessagesArr.getJSONObject(i - l1);
                }
                messages[i].messageText = messageOb.getString("MESSAGE_TEXT");
                messages[i].receivedMessage = messageOb.getBoolean("RECD_MESSAGE");
                messages[i].timestamp = messageOb.getLong("TIMESTAMP");
            }
            return messages;
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    public static void addMessage(Context context, String secretUsername, Message message) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        try {
            JSONArray messagesArr = new JSONArray(shared.getString(secretUsername, "[]"));
            JSONObject messageOb = new JSONObject();
            messageOb.put("MESSAGE_TEXT", message.messageText);
            messageOb.put("RECD_MESSAGE", message.receivedMessage);
            messageOb.put("TIMESTAMP", message.timestamp);
            messagesArr.put(messageOb);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString(secretUsername, messagesArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static Message getLastMessage(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        try {
            JSONArray messagesArr = new JSONArray(shared.getString(secretUsername, "[]"));
            if (messagesArr.length() < 1) {
                return null;
            }
            Message message = new Message();
            JSONObject messageOb = messagesArr.getJSONObject(messagesArr.length() - 1);
            message.messageText = messageOb.getString("MESSAGE_TEXT");
            message.receivedMessage = messageOb.getBoolean("RECD_MESSAGE");
            message.timestamp = messageOb.getLong("TIMESTAMP");
            return message;
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    public static void clearContactMessages(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.remove(secretUsername);
        editor.commit();
    }

    public static String[] getAllKeys(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String[] temp = shared.getAll().keySet().toArray(new String[0]);
        return temp;
    }

    public static void cleanup(Context context) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        Contact[] contacts = getContacts(context);
        ArrayList<String> temp = new ArrayList<String>();
        for (Contact c : contacts) {
            temp.add(c.secretUsername);
            temp.add(c.secretUsername + "_QUEUED");
        }
        String[] validSecrets = temp.toArray(new String[0]);
        String[] storedKeys = getAllKeys(context);
        temp.clear();
        String persistenceKeysStr = Arrays.toString(persistenceKeys);
        String validSecretsStr = Arrays.toString(validSecrets);
        for (String k : storedKeys) {
            if (!(persistenceKeysStr.contains(k) || validSecretsStr.contains(k))) {
                temp.add(k);
            }
        }
        for (String t : temp) {
            SharedPreferences.Editor editor = shared.edit();
            editor.remove(t);
            editor.commit();
        }
    }

    public static void updateRegistrationID(Context context, String secretUsername, String newRegId) {
        SharedPreferences shared = context.getSharedPreferences(key, 0);
        String contactsStr = shared.getString("CONTACTS", "[]");
        try {
            JSONArray contactsArr = new JSONArray(contactsStr);
            for (int i = 0; i < contactsArr.length(); i++) {
                JSONObject contactOb = contactsArr.getJSONObject(i);
                if (contactOb.get("secretUsername").equals(secretUsername)) {
                    contactOb.put("registrationID", newRegId);
                    contactsArr.put(i, contactOb);
                    SharedPreferences.Editor editor = shared.edit();
                    editor.putString("CONTACTS", contactsArr.toString());
                    editor.commit();
                    return;
                }
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    //Queued messages
    public static void addMessageToQueue(Context context, String secretUsername, Message message) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String messageQueueKey = secretUsername + "_QUEUED";
        try {
            JSONArray messagesArr = new JSONArray(shared.getString(messageQueueKey, "[]"));
            JSONObject messageOb = new JSONObject();
            messageOb.put("MESSAGE_TEXT", message.messageText);
            messageOb.put("RECD_MESSAGE", message.receivedMessage);
            messageOb.put("TIMESTAMP", -1l);
            messagesArr.put(messageOb);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString(messageQueueKey, messagesArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static Message getMessageFromQueue(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String messageQueueKey = secretUsername + "_QUEUED";
        try {
            JSONArray messagesArr = new JSONArray(shared.getString(messageQueueKey, "[]"));
            if (messagesArr.length() < 1) {
                return null;
            }
            JSONObject messageOb = messagesArr.getJSONObject(0);
            Message message = new Message();
            message.messageText = messageOb.getString("MESSAGE_TEXT");
            message.receivedMessage = messageOb.getBoolean("RECD_MESSAGE");
            message.timestamp = -1l;
            return message;
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    public static void removeMessageFromQueue(Context context, String secretUsername) {
        SharedPreferences shared = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String messageQueueKey = secretUsername + "_QUEUED";
        try {
            JSONArray oldMessagesArr = new JSONArray(shared.getString(messageQueueKey, "[]"));
            if (oldMessagesArr.length() < 1) {
                return;
            }
            JSONArray messagesArr = new JSONArray();
            for (int i = 1; i < oldMessagesArr.length(); i++) {
                messagesArr.put(oldMessagesArr.getJSONObject(i));
            }
            SharedPreferences.Editor editor = shared.edit();
            editor.putString(messageQueueKey, messagesArr.toString());
            editor.commit();
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static String getNewKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String newKey = KeyGeneratorUtils.getNewKey(Persistence.getSecretUsername(context));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("KEYCACHE", newKey);
        editor.commit();
        return newKey;
    }

    public static String getCachedKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        String cachedKey = sharedPreferences.getString("KEYCACHE", null);
        return cachedKey;
    }

    public static void clearKeyCache(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("KEYCACHE");
        editor.commit();
    }
}
/**
 * Created by yds on 30/11/15.
 */