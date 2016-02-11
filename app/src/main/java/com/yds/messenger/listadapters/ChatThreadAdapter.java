package com.yds.messenger.listadapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.yds.messenger.models.Message;
import com.yds.messenger.services.TTSService;
import com.yds.messenger.utils.TimestampFormatter;
import com.yds.messenger.R;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Adaptor for displaying chat thread
 */
public class ChatThreadAdapter extends ArrayAdapter<Message> {
    private final Context context; //The current context
    private final Message[] messages; //List of messages in the chat thread

    /**
     * Constructor
     * @param context - The current context
     * @param messages - The list of mmessages to be displayed in the chat thread
     */
    public ChatThreadAdapter(Context context, Message[] messages) {
        super(context, R.layout.listitem_receivedmessage, messages);
        this.context = context;
        this.messages = messages;
    }

    /**
     * The overridden getView implementation
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;

        /**
         * The view used for displaying the message differs for received and sent messages
         */
        if (messages[position].receivedMessage) {
            rowView = inflater.inflate(R.layout.listitem_receivedmessage, parent, false);
        } else {
            rowView = inflater.inflate(R.layout.listitem_sentmessage, parent, false);
        }

        TextView messageField = (TextView) rowView.findViewById(R.id.textView_message);
        TextView timestampField = (TextView) rowView.findViewById(R.id.textView_timestamp);

        messageField.setText(messages[position].messageText);
        timestampField.setText(TimestampFormatter.getAppropriateFormat(messages[position].timestamp));

        /**
         * The timestamp of the message indicates the current state of the message in the system
         */
        if (messages[position].timestamp == 0l) {
            //The message failed to send, so display "not sent"
            timestampField.setText(context.getString(R.string.not_sent));
            timestampField.setTextColor(context.getResources().getColor(R.color.red));
            timestampField.setTypeface(null, Typeface.ITALIC);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, R.string.message_not_sent, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (messages[position].timestamp == -1l) {
            //The message is currently in the dispatch queue waiting for its turn to be sent
            timestampField.setText(context.getString(R.string.dispatched));
            timestampField.setTypeface(null, Typeface.ITALIC);
        }

        /**
         * Parts of the Text to Speech implementation depend on classes defined in API version 15.So
         * ignore this feature in versions with lower API level.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            rowView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    /**
                     * Will create a service that runs, starts up Text to Speech, utters the String
                     * passed as extra "text" and then finally shuts itself down.
                     */
                    Intent ttsIntent = new Intent(context, TTSService.class);
                    ttsIntent.putExtra("text", messages[position].messageText);
                    context.startService(ttsIntent);
                    return true;
                }
            });
        }

        return rowView;
    }

}
/**
 * Created by yds on 30/11/15.
 */