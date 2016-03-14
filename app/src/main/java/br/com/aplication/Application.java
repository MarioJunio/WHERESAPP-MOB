package br.com.aplication;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.br.wheresapp.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import br.com.activities.Configuration;
import br.com.activities.Main;
import br.com.binder.LocalBinder;
import br.com.dao.UserDAO;
import br.com.model.domain.Contact;
import br.com.model.domain.User;
import br.com.services.SmackService;
import br.com.smack.Smack;
import br.com.sync.Sync;
import br.com.util.NativeLoader;

/**
 * Created by MarioJ on 17/03/15.
 */
public class Application extends android.app.Application {

    private final String TAG = "Application";

    private User user;
    private UserDAO userDAO;
    private Map<String, List<String>> countries;
    private Contact chatContact;
    private Random random = new Random();

    // ad
    private InterstitialAd interstitialAd;
    private int counterAd;
    private final int MIN_COUNT = 10, MAX_COUNT = 18;

    // load emoji
    private static Application instance;
    public static volatile Handler applicationHandler = null;

    // smack service
    public SmackService smackService;
    public boolean smackServiceBounded;
    private SmackServiceConnection smackServiceConnection = new SmackServiceConnection();

    @Override
    public void onCreate() {

        super.onCreate();

        try {

            // instance native loader
            this.instance = this;

            this.applicationHandler = new Handler(getInstance().getMainLooper());

            // instancia DAO de usuario
            this.userDAO = UserDAO.getInstance(getApplicationContext());

            // pega a conta ativa do celular
            this.user = userDAO.getLocalUser();

            // carrega todos os paises
            loadCountries();

            init();

            NativeLoader.initNativeLibs(Application.getInstance());
            createInterstialAd();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupActiveUser() {
        user = activePhoneUser();
    }

    public void createUser() {
        user = new User();
    }

    public void save() {
        setUser(userDAO.save(user));
    }

    public void update() {
        userDAO.update(user);
    }

    public void updatePhoto() {
        userDAO.updatePhoto(user);
    }

    public User activePhoneUser() {
        return userDAO.getLocalUser();
    }

    public User getCurrentUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    private void loadCountries() {

        countries = new HashMap<>();

        String[] getCountries = getApplicationContext().getResources().getStringArray(R.array.countries);

        for (String country : getCountries) {

            String tokens[] = country.split(",");

            String ddi = tokens[0];
            String iso = tokens[1];

            List<String> listIso = countries.get(ddi);

            if (listIso != null && !listIso.isEmpty())
                listIso.add(iso);
            else {

                listIso = new ArrayList<>();
                listIso.add(iso);

                countries.put(ddi, listIso);
            }
        }
    }

    private void createInterstialAd() {

        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getResources().getString(R.string.intersticial_ad_unit_id));

        interstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                resetAdCounter();
            }
        });

        requestNewInterstitial();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        interstitialAd.loadAd(adRequest);
    }

    public void counterAndShow() {

        counterAd++;

        int next = random.nextInt(MAX_COUNT - MIN_COUNT) + MIN_COUNT;

        if (counterAd >= next && interstitialAd.isLoaded())
            interstitialAd.show();
    }

    private void init() throws InterruptedException {

        User user = getCurrentUser();
        Intent i;

        if (user == null || user.getState() != User.State.ACTIVE) {
            i = new Intent(this, Configuration.class);

            if (user != null)
                i.putExtra(Configuration.STEP, user.getState().ordinal());

        } else {

            i = new Intent(this, Main.class);

            // start sync services
            Sync.start(getApplicationContext());

            // inicia o smack service
            doBindSmackService(getApplicationContext());
        }

        if (i != null) {
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

    }

    public void doBindSmackService(final Context context) throws InterruptedException {

        if (smackService == null && !smackServiceBounded)
            context.bindService(new Intent(context, SmackService.class), smackServiceConnection, Context.BIND_AUTO_CREATE);

    }

    public void doUnbindSmackService(Context context) {

        if (smackService != null && smackServiceBounded)
            context.unbindService(smackServiceConnection);
    }

    public void waitSmackService() {

        while (!smackServiceBounded) {

            try {
                Log.d(TAG, "WAITING SMACK SERVICE");
                Thread.sleep(App.SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void resetAdCounter() {
        counterAd = 0;
    }

    public Map<String, List<String>> getCountries() {
        return countries;
    }

    public String getXmppUser() {
        return Smack.toSmackUser(user.getDdi(), user.getPhone());
    }

    public Contact getChatContact() {
        return chatContact;
    }

    public void setChatContact(Contact chatContact) {
        this.chatContact = chatContact;
    }

    public static Application getInstance() {
        return instance;
    }

    private class SmackServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            try {

                LocalBinder binder = (LocalBinder) service;
                smackService = (SmackService) binder.getService();
                smackServiceBounded = true;

                Log.d(TAG, "SMACK SERVICE CONNECTED");

            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            smackService = null;
            smackServiceBounded = false;
        }
    }

}
