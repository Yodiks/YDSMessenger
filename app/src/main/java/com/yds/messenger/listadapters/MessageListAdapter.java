package com.yds.messenger.listadapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yds.messenger.IntentHelpers.IntentCreator;
import com.yds.messenger.R;
import com.yds.messenger.models.MessageListItem;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.utils.MessageDispatcher;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Adapter class for displaying list of Contacts in the first screen
 */
public class MessageListAdapter extends ArrayAdapter<MessageListItem> {

    private final Activity context; //The current context
    private final MessageListItem[] messageListItems; //Lists of contacts to be displayed
    private final String mySecretUsername; //Secret username of current user
    /**
     * If prepopulatedMessage is not null, then the chat thread opened by clicking on any of the
     * contacts in the Contacts list will have that message already populated in the message field.
     */
    private String prepopulatedMessage = null;

    /**
     * Constructor
     * @param context - The current context
     * @param messageListItems - Objects representing contacts to be displayed
     * @param prepopulatedMessage - Message to be populated in the chat activity
     */
    public MessageListAdapter(Activity context, MessageListItem[] messageListItems, String prepopulatedMessage) {
        this(context, messageListItems);
        this.prepopulatedMessage = prepopulatedMessage;
    }

    /**
     * Version of constructor to be used in situations when no pre-populated message is needed.
     * @param context - The current context
     * @param messageListItems - Objects representing contacts to be displayed
     */
    public MessageListAdapter(Activity context, MessageListItem[] messageListItems) {
        super(context, R.layout.listitem_messagelist, messageListItems);
        this.context = context;
        this.messageListItems = messageListItems;
        this.mySecretUsername = Persistence.getSecretUsername(context);
    }

    /**
     * Overridden implementation of the getView method
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.listitem_messagelist, parent, false);

        final TextView contactNameField = (TextView) rowView.findViewById(R.id.textView_ContactName);
        TextView messagePreviewField = (TextView) rowView.findViewById(R.id.textView_messagePreview);

        contactNameField.setText(messageListItems[position].contactName);
        messagePreviewField.setText(messageListItems[position].messagePreview);

        if (messageListItems[position].unread) {
            contactNameField.setTypeface(null, Typeface.BOLD);
        }

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the chat thread for the contact
                Intent intent = IntentCreator.getChatThreadIntent(context, messageListItems[position].secretUsername, prepopulatedMessage);

                context.startActivity(intent);
                context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        /**
         * Long click triggers an option to un-pair from the contact
         */
        rowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.delete_contact) + ": " + messageListItems[position].contactName + "?")
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                /**
                                 * A message has to be sent to the contact to ensure that up-pairing
                                 * happens on the contact's device too
                                 */
                                new AsyncTask() {
                                    private ProgressDialog progressDialog;

                                    @Override
                                    protected Object doInBackground(Object[] objects) {
                                        return MessageDispatcher.dispatchMessage(context, messageListItems[position].registrationID, MessageTypes.REMOVE_CONTACT, mySecretUsername);
                                    }

                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        progressDialog = new ProgressDialog(context);
                                        progressDialog.setMessage(context.getString(R.string.sending_unpairing));
                                        progressDialog.show();

                                    }

                                    @Override
                                    protected void onPostExecute(Object o) {
                                        super.onPostExecute(o);
                                        progressDialog.dismiss();
                                        rowView.setOnClickListener(null);
                                        contactNameField.setTextColor(context.getResources().getColor(R.color.color_messagePreview));
                                        contactNameField.setPaintFlags(contactNameField.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                        Persistence.clearContact(context, messageListItems[position].secretUsername);
                                    }
                                }.execute(null, null, null);
                            }
                        })
                        .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .setIcon(context.getResources().getDrawable(R.drawable.ic_launcher))
                        .setTitle(R.string.dialogTitle_delete_contact);
                builder.show();
                return true;
            }
        });
        return rowView;
    }
}
/**
 * Created by yds on 30/11/15.
 */