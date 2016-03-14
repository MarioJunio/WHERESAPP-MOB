package br.com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.com.model.domain.User;
import br.com.aplication.Application;
import br.com.sync.Sync;

/**
 * Created by MarioJ on 03/06/15.
 */
public class Boot extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        Application session = (Application) context.getApplicationContext();

        User user = session.getCurrentUser();

        if (user != null && user.getState() == User.State.ACTIVE)
            Sync.start(context);

        Log.d("Boot", "[STARTED]");
    }
}
