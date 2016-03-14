package br.com.providers;

import android.net.Uri;

/**
 * Created by MarioJ on 23/07/15.
 */
public abstract class MessageContract {

    public static final String PROVIDER_PATH = "messages";
    public static final String AUTHORITY = String.format("com.br.wheresapp.%s_provider", PROVIDER_PATH);
    public static final String URI = String.format("content://%s/%s", AUTHORITY, PROVIDER_PATH);
    public static final Uri CONTENT_URI = Uri.parse(URI);

    public static final int MESSAGES = 3;
    public static final int MESSAGES_INSERT = 4;
    public static final int MESSAGES_UPDATE = 5;
    public static final int MESSAGES_CONVERSATIONS = 6;
    public static final int MESSAGES_SEARCH = 8;
    public static final int LOAD_LAST_MESSAGE_DELIVERED = 7;

    public enum Column {
        _ID, DDI, PHONE, DATE, DELIVERY, READ, MODIFICATION, BODY
    }


}
