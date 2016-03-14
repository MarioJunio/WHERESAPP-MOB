package br.com.providers;

import android.net.Uri;

/**
 * Created by MarioJ on 18/05/15.
 */
public abstract class ContactContract {

    public static final String PROVIDER_PATH = "contacts";
    public static final String AUTHORITY = String.format("com.br.wheresapp.%s_provider", PROVIDER_PATH);
    public static final String URI = String.format("content://%s/%s", AUTHORITY, PROVIDER_PATH);
    public static final Uri CONTENT_URI = Uri.parse(URI);

    public static final int CONTACTS = 1;
    public static final int CONTACT_ID = 2;
    public static final int CONTACT_EXISTS = 3;

    public enum Column {
        _ID, DDI, PHONE_NUMBER, NAME, STATUS, LAST_MODIFIED, LAST_SEE, PHOTO_TN, LATITUDE, LONGITUDE, MODIFICATION
    }

}
