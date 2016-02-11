package com.yds.messenger.models;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Constants for each of the message types
 */
public class MessageTypes {
    public static final int DEBUG_MESSAGE = 0;
    public static final int PAIRING_MESSAGE = 1; //Used for initial pairing
    public static final int MORNAL_MESSAGE = 2; //Regular chat messages
    public static final int REMOVE_CONTACT = 3; //Un-pairing request
    public static final int REGISTRATION_UPDATE = 4; //Inform others of change in Registration ID
    public static final int SELF_DESTRUCTING_MESSAGE = 5; //Messages that are not saved by receiver
    public static final int USERNAME_UPDATE = 6; //Inform others of change in Username
    public static final int REJECT_CONNECTION_KEY_INVALID = 7; //When key used in pairing is invalid
}
/**
 * Created by yds on 30/11/15.
 */