package br.com.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by MarioJ on 27/05/15.
 */
public class SyncService extends Service {

    private SyncAdapter syncAdapter;
    private final Object syncAdapterLock = new Object();

    @Override
    public void onCreate() {

        synchronized (syncAdapterLock) {

            if (syncAdapter == null)
                syncAdapter = new SyncAdapter(getApplicationContext(), true);
        }

        Log.d("SyncService [create]", "OK");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }

}
