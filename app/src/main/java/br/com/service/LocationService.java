package br.com.service;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.jivesoftware.smack.packet.Message;

import java.util.List;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.listener.LocationMessageListener;
import br.com.model.domain.Contact;
import br.com.smack.Smack;

/**
 * Created by MarioJ on 24/03/15.
 */
public class LocationService {

    public static final String COORD_SEPARATOR = ":";

    // STATUS
    public static final int OUT_OF_SERVICE = 0;
    public static final int TEMPORALY_UNAVAILABLE = 1;
    public static final int AVAILABLE = 2;

    private final String TAG = "LocationService";

    private Application application;
    private List<Contact> contacts;
    private LocationManager locationManager;
    private MyLocationListener myLocationListener;

    // ultimas coordenadas enviadas
    private double latitude, longitude;

    public LocationService(Context context) {
        this.application = (Application) context.getApplicationContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        myLocationListener = new MyLocationListener();
    }

    public void start(List<Contact> contacts) {

        this.contacts = contacts;

        // obtem o melhor provedor baseado nas configurações do celular
        String provider = getBestProvider();

        Log.d(TAG, "Provedor de local escolhido:" + provider);

        if (provider != null)
            locationManager.requestLocationUpdates(provider, 2000, 1, myLocationListener);
        else
            Log.d(TAG, "Nenhum provedor escolhido ! (GPS desativado | Obtendo GPS)");
    }

    private String getBestProvider() {

        if (isGPSEnabled())
            return LocationManager.GPS_PROVIDER;
        else if (isNetworkEnabled())
            return LocationManager.NETWORK_PROVIDER;
        else
            return LocationManager.PASSIVE_PROVIDER;
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static void turnOnGPS(Context context) {

        if (App.getAPILevel() < Build.VERSION_CODES.KITKAT) {

            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", true);
            context.sendBroadcast(intent);

            String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            if (!provider.contains("gps")) { //if gps is disabled

                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }

        } else
            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

    }

    public static void turnOffGPS(Context context) {

        if (App.getAPILevel() < Build.VERSION_CODES.KITKAT) {

            String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            if (provider.contains("gps")) { //if gps is enabled

                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }

        } else
            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

    }

    public static String[] split(String body) {
        return body.split(COORD_SEPARATOR);
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            if (location != null && latitude != location.getLatitude() && longitude != location.getLongitude()) {

                double lat = location.getLatitude();
                double lon = location.getLongitude();

                Log.d(TAG, "Localizacao mudou: " + lat + " - " + lon);

                broadcast(lat, lon);

                // set last sent coords
                latitude = lat;
                longitude = lon;

            }

        }

        private void broadcast(double lat, double lon) {

            for (Contact c : contacts) {

                Message message = new Message();
                message.setStanzaId(LocationMessageListener.COORDINATE_ID);
                message.setTo(Smack.parseContact(c.getDdi(), c.getPhone()));
                message.setBody(lat + COORD_SEPARATOR + lon);

                application.smackService.sendMessage(message);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

}
