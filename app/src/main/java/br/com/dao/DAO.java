package br.com.dao;

import android.content.Context;
import android.net.Uri;

/**
 * Created by MarioJ on 25/07/15.
 */
public abstract class DAO {

    private Context context;
    private Uri contentUri;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

}
