package br.com.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import br.com.aplication.Application;
import br.com.observers.PhoneContactsObserver;

/**
 * Created by MarioJ on 03/06/15.
 */
public class PhoneContactsService extends Service {

    private final String TAG = "PhoneContactsService";

    private Application application;
    private PhoneContactsObserver observer;

    @Override
    public void onCreate() {

        application = (Application) getApplicationContext();

        observer = new PhoneContactsObserver(new Handler(Looper.getMainLooper()), getApplicationContext());
        getContentResolver().registerContentObserver(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, true, observer);
        Log.d(TAG, "[STARTED]");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

}
