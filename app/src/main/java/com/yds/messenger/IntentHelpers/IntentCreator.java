package com.yds.messenger.IntentHelpers;

import android.content.Context;
import android.content.Intent;

import com.yds.messenger.ChatThreadActivity;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class ini untuk membantu membuat intent yang benar untuk melewati semua parameter yang diperlukan/
 * This class helps to correctly create a intent that properly passes all the required extra
 * parameters
 */
public class IntentCreator {
    /**
     * Metode utuk membantu membuat sebuah Intent untuk chat thread disaat chat window menjadi auto populated dengan message di message field.
     * Method helps create a Intent for a chat thread when the resulting chat window has to be auto populated by a message in the message field.
     *
     * It is intetnded to be used in situations when the activity is invoked to enable sharing from other apps passed
     *
     * @param context             - The current context
     * @param secretUsername      - The secret username of the contact whose chat thread will be opened
     * @param prepopulatedMessage - The message that will be populated in the message box of the
     *                            opened activity
     * @return - The constructed intent
     */
    public static Intent getChatThreadIntent(Context context, String secretUsername, String prepopulatedMessage) {
        Intent chatThreadIntent = new Intent(context, ChatThreadActivity.class);
        chatThreadIntent.putExtra("secretUsername", secretUsername);
        /**
         * If the prepopulatedMessage string is null, it indicates that there is no pre-populated
         * message.
         */
        if (prepopulatedMessage != null) {
            chatThreadIntent.putExtra("prepopulatedMessage", prepopulatedMessage);
        }
        return chatThreadIntent;
    }

    /**
     * Method helps create a Intent for a chat thread with no pre-populated message
     *
     * @param context        - The current context
     * @param secretUsername - The secret username of the contact whose chat thread will be opened
     * @return - The constructed intent
     */
    public static Intent getChatThreadIntent(Context context, String secretUsername) {
        /**
         * The version of the getChatThreadIntent method that takes a pre-populated message option
         * interprets a null value in the prepopulatedMessage field as an absence of any
         * pre-populated messages.
         * Hence this message simply has to call that version of the method with a null value for
         * prepopulatedMessage.
         */
        return getChatThreadIntent(context, secretUsername, null);
    }
}
/**
 * Created by yds on 30/11/15.
 */