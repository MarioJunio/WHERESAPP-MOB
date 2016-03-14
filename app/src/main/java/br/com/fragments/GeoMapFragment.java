package br.com.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.br.wheresapp.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import br.com.activities.Perfil;
import br.com.adapter.ContactGeoMapAdapter;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.listener.LocationMessageListener;
import br.com.model.domain.Contact;
import br.com.service.LocationService;
import br.com.util.Utils;

/**
 * Created by MarioJ on 10/09/15.
 */
public class GeoMapFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private static final String TAG = "GeoMapFragment";

    private final int TIME_MINIMIZE = 2000;
    private final int TIME_MAXIMIZE = 3000;
    private final int DELAY = 150;

    // Application
    private Application application;

    // Google Maps API V2
    private MapView mapView;
    private GoogleMap map;

    // Gerenciador do GPS
    private LocationManager locationManager;

    // Widgets
    private RecyclerView contactsRecyclerView;

    // Utils
    private ContactGeoMapAdapter contactGeoMapAdapter;
    private LocationMessageListener locationMessageListener;
    private ContactDAO contactDAO;
    private boolean mapTerrain;

    // handler para atualizar UI
    private Handler handler;

    // controla se algum contato já esta selecionado
    private boolean alreadySelected;

    // contato selecionado
    private Contact contactSelected;

    // Gerencia MenuItem do GPS
    private MenuItem menuItemGPS;


    public static Fragment newInstance() {
        return new GeoMapFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        application = (Application) getActivity().getApplicationContext();
        handler = new Handler(Looper.getMainLooper());
        contactDAO = ContactDAO.instance(getActivity().getApplicationContext());
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        contactSelected = null;
        alreadySelected = false;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // remove listen de localizações, pois a view vai ser encerrada
        application.smackService.removeMessageListener(locationMessageListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_geo_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mapInitializer(view, savedInstanceState);
        recyclerInitializer(view);

        // instancia listen de pacotes de localização
        this.locationMessageListener = new LocationMessageListener(contactGeoMapAdapter, handler, contactDAO);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_map, menu);

        menuItemGPS = menu.getItem(1);

        if (Utils.isGPSEnable(getContext())) {
            menuItemGPS.setTitle("Desativar GPS");
            menuItemGPS.setChecked(true);
        } else {
            menuItemGPS.setTitle("Ativar GPS");
            menuItemGPS.setChecked(false);
        }

        super.onCreateOptionsMenu(menu, inflater);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();

        switch (id) {
            case R.id.menu_gps_actived:
                toggleGPS();
                break;
            case R.id.edit_perfil:
                startActivity(new Intent(getActivity(), Perfil.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleGPS() {

        if (menuItemGPS.isChecked())
            LocationService.turnOffGPS(getContext());
        else
            LocationService.turnOnGPS(getContext());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setBuildingsEnabled(true);
        map.setMyLocationEnabled(true);
        mapTerrain = true;

        // carrega contatos que possuem localizacao
        prepare();

        // adiciona listen de localização
        try {
            application.smackService.addMessageListener(locationMessageListener);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // inicia listeners para o map
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

                if (cameraPosition.zoom > (map.getMaxZoomLevel() / 2) && mapTerrain) {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    mapTerrain = !mapTerrain;
                } else if (!mapTerrain && cameraPosition.zoom < (map.getMaxZoomLevel() / 2)) {
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    mapTerrain = !mapTerrain;
                }

            }
        });
    }

    private void mapInitializer(View view, Bundle savedInstanceState) {

        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);
    }

    private void recyclerInitializer(View view) {

        contactsRecyclerView = (RecyclerView) view.findViewById(R.id.my_contacts);
        contactsRecyclerView.bringToFront();

        LinearLayoutManager linearLayout = new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        linearLayout.scrollToPosition(0);

        contactsRecyclerView.setLayoutManager(linearLayout);

    }

    private void prepare() {

        // busca contatos que já foram localizados
        Object[] contactsData = ContactDAO.instance(getActivity().getApplicationContext()).all2Map();

        if (contactsData == null)
            return;

        // prepara as estruturas de dados
        final List<Contact> listContacts = (List<Contact>) contactsData[0];
        final Map<String, Contact> mapContacts = (Map<String, Contact>) contactsData[1];

        // cria adaptador de contatos para o recycler
        contactGeoMapAdapter = new ContactGeoMapAdapter(getActivity(), listContacts, mapContacts, getActivity(), map);

        // setup adaptador de contatos
        contactsRecyclerView.setAdapter(contactGeoMapAdapter);

        // set listener for click in recycler
        contactGeoMapAdapter.setListener(new ContactGeoMapAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position) {

                // get contact at position
                final Contact currentContact = listContacts.get(position);

                if (alreadySelected) {

                    new Timer("Minimizing").schedule(new TimerTask() {

                        @Override
                        public void run() {

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    zoom(contactSelected.getLatitude(), contactSelected.getLongitude(), TIME_MINIMIZE, map.getMinZoomLevel());
                                    contactSelected = currentContact;
                                }
                            });
                        }

                    }, 0);

                } else
                    contactSelected = currentContact;

                new Timer("Maximizing").schedule(new TimerTask() {

                    @Override
                    public void run() {

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                zoom(currentContact.getLatitude(), currentContact.getLongitude(), TIME_MAXIMIZE, map.getMaxZoomLevel() / (float) 1.1);
                            }
                        });

                    }
                }, (alreadySelected ? (TIME_MINIMIZE + DELAY) : 0));

                alreadySelected = true;
            }
        });
    }

    private void zoom(final double lat, final double lon, final int duration, final float zoomLevel) {

        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoomLevel);

        map.moveCamera(center);
        map.animateCamera(zoom, duration, null);
    }


    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, final int status, Bundle extras) {

        switch (status) {

            case LocationService.OUT_OF_SERVICE:
                menuItemGPS.setTitle("GPS Sem serviço");
                menuItemGPS.setChecked(false);
                break;
            case LocationService.TEMPORALY_UNAVAILABLE:
                Log.d(TAG, "GPS TEMPORARIAMENTE INDISPONIVEL");
                menuItemGPS.setChecked(false);
                menuItemGPS.setTitle("GPS Indisponível");
                break;
            case LocationService.AVAILABLE:
                Log.d(TAG, "GPS HABILITADO");
                menuItemGPS.setTitle("Desativar GPS");
                menuItemGPS.setChecked(true);
        }

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "GPS HABILITADO");
        menuItemGPS.setTitle("Desativar GPS");
        menuItemGPS.setChecked(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "GPS Desabilitado");
        menuItemGPS.setTitle("Ativar GPS");
        menuItemGPS.setChecked(false);
    }
}
