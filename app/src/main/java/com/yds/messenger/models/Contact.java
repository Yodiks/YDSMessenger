package com.yds.messenger.models;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class to hold properties of a contact
 */
public class Contact {
    public String username;
    public String secretUsername;
    public String registrationID;
    public Message lastMessage = null; //null os interpreted as no message
    public String encKey = null; //null is interpreted as no encryption available
}
/**
 * Created by yds on 30/11/15.
 */