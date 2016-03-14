package br.com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

import br.com.activities.WaitSmsDialog;
import br.com.net.Http;
import br.com.aplication.App;

/*
 * Created by MarioJ on 16/04/15.
 */
public class IncomingSms extends BroadcastReceiver {

    private final String TAG = "IncomingSms";

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences(WaitSmsDialog.SMS_CONTEXT, Context.MODE_PRIVATE);
        final String getDDI = sharedPreferences.getString(WaitSmsDialog.SMS_DDI_DEST, null);
        final String getPhone = sharedPreferences.getString(WaitSmsDialog.SMS_PHONE_DEST, null);

        Log.d(TAG, "Current number is " + getDDI + " " + getPhone);

        Bundle extras = intent.getExtras();

        if (extras != null) {

            final Object[] pdus = (Object[]) extras.get("pdus");

            SmsMessage smsMessage = null;

            if (Build.VERSION.SDK_INT >= 19)
                smsMessage = Telephony.Sms.Intents.getMessagesFromIntent(intent)[0];
            else
                smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);

            final SmsMessage finalSmsMessage = smsMessage;

            App.runBackgroundService(new Runnable() {

                @Override
                public void run() {

                    try {

                        if (checkSmsReceived(getDDI, getPhone, finalSmsMessage.getDisplayMessageBody()))
                            sharedPreferences.edit().putBoolean(WaitSmsDialog.SMS_ACC_ACTIVED, true).commit();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    public boolean checkSmsReceived(String ddi, String phone, String message) throws IOException, JSONException {

        Log.d(TAG, "DDI: " + ddi);
        Log.d(TAG, "PHONE: " + phone);

        if (message.contains(":")) {

            String confirmCode = message.split(":")[1];
            Log.d(TAG, "CONFIRM CODE: " + confirmCode);

            return Http.confirmCode(ddi, phone, confirmCode);
        }

        return false;

    }

}
