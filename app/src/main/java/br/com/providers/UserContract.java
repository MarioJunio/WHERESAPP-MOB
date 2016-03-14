package br.com.providers;

import android.net.Uri;

/**
 * Created by MarioJ on 19/08/15.
 */
public abstract class UserContract {

    public static final String PROVIDER_PATH = "user";
    public static final String AUTHORITY = String.format("com.br.wheresapp.%s_provider", PROVIDER_PATH);
    public static final String URI = String.format("content://%s/%s", AUTHORITY, PROVIDER_PATH);
    public static final Uri CONTENT_URI = Uri.parse(URI);

    public static final int USER = 3;
    public static final int USER_ID = 4;

    public enum Column {
        _ID, DDI, PHONE, NAME, PHOTO_THUMB, STATUS, STATE, CONTACTS_CHECKSUM
    }
}
