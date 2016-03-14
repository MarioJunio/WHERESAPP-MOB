package br.com.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by MarioJ on 27/05/15.
 */
public class AuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {

        if (mAuthenticator == null) {
            // Create a new authenticator object
            mAuthenticator = new Authenticator(this);
        }

    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
