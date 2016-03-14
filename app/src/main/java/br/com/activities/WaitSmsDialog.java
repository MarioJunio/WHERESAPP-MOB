package br.com.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.br.wheresapp.R;

import java.util.Timer;
import java.util.TimerTask;

import br.com.model.ListenerSuccess;
import br.com.service.PhoneFormatTextWatcher;
import br.com.aplication.Application;
import br.com.aplication.Phones;

/**
 * Created by MarioJ on 13/04/15.
 */
public class WaitSmsDialog extends AlertDialog implements View.OnClickListener {

    public static final String TAG = "WaitSmsDialog";

    // resources to handler logic
    private Handler handler;
    private Timer timer;
    private SharedPreferences sharedPreferences;
    private ListenerSuccess listenerSuccess;
    private Application session;

    // Android widgets
    private TextView phone, time;
    private ImageButton btEditPhone;
    private ProgressBar progressTime;

    // 600s equivale a 10m
    private final int MAX_TIME = 600;
    private int progress = 0;
    private int currentTime = MAX_TIME;

    // sms shared preferences
    public static final String SMS_CONTEXT = "SMS_AUTH";
    public static final String SMS_DDI_DEST = "SMS_DDI_DESTINATION";
    public static final String SMS_PHONE_DEST = "SMS_PHONE_DESTINATION";
    public static final String SMS_ACC_ACTIVED = "SMS_ACCOUNT_ACTIVED";

    public static WaitSmsDialog newInstance(Activity context) {
        return new WaitSmsDialog(context);
    }

    public WaitSmsDialog(Activity context) {
        super(context);

        handler = new Handler();
        timer = new Timer();
        session = (Application) context.getApplication();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        initWidgets(inflater);
        initListeners();

        sharedPreferences = getContext().getSharedPreferences(SMS_CONTEXT, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SMS_DDI_DEST, session.getCurrentUser().getDdi());
        editor.putString(SMS_PHONE_DEST, session.getCurrentUser().getPhone());
        editor.putBoolean(SMS_ACC_ACTIVED, false);
        editor.commit();

        startTimer();
    }

    private void initWidgets(LayoutInflater inflater) {

        View view = inflater.inflate(R.layout.dialog_wait_sms, null);

        phone = (TextView) view.findViewById(R.id.phone);
        btEditPhone = (ImageButton) view.findViewById(R.id.btEditPhone);
        progressTime = (ProgressBar) view.findViewById(R.id.progressTime);
        time = (TextView) view.findViewById(R.id.time);

        String countryISO = Phones.getCountryISO(getContext(), session.getCurrentUser().getDdi());

        phone.setText(Phones.INTERNATIONAL_IDENTIFIER + session.getCurrentUser().getDdi() + " " + PhoneFormatTextWatcher.formatNumber(session.getCurrentUser().getPhone(), countryISO));

        setView(view);

        progressTime.setProgress(0);
        progressTime.setMax(MAX_TIME);

    }

    private void initListeners() {
        btEditPhone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        stopTimer();
        cancel();
        dismiss();
    }

    private void startTimer() {

//        getContext().sendBroadcast(new Intent(getContext(), IncomingSms.class));

        timer.schedule(initializeTimerTask(), 0, 1000);
    }

    private void stopTimer() {
        timer.cancel();
    }

    public void setListenerOnAuthSuccess(ListenerSuccess listenerSuccess) {
        this.listenerSuccess = listenerSuccess;
    }

    private TimerTask initializeTimerTask() {

        return new TimerTask() {

            @Override
            public void run() {

                currentTime--;
                progress++;

                if (currentTime == 0) {

                    stopTimer();
                    cancel();
                    dismiss();

                } else if (sharedPreferences.getBoolean(SMS_ACC_ACTIVED, false)) {

                    // para o temporizador
                    stopTimer();

                    // limpa os dados de ativacao da conta
                    sharedPreferences.edit().clear().commit();

                    // Executa evento de sucesso
                    listenerSuccess.execute();

                    // close dialog
                    cancel();
                    dismiss();
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        time.setText(String.format("%d:%02d", currentTime / 60, currentTime % 60));
                        progressTime.setProgress(progress);
                    }
                });


            }
        };

    }
}
