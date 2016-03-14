package br.com.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.br.wheresapp.R;

import br.com.adapter.ContactsAdapter;
import br.com.adapter.FragmentPagerAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.dao.UserDAO;
import br.com.decoration.LineDividerRecyclerView;
import br.com.observers.ContactsObserver;
import br.com.providers.ContactContract;
import br.com.receivers.NetworkReceiver;
import br.com.resources.Contacts;
import br.com.service.LocationService;
import br.com.service.PhoneContactsBridge;
import br.com.util.NotificationCenter;
import br.com.util.Utils;


public class Main extends AppCompatActivity implements NetworkReceiver.NetworkStateReceiverListener, NotificationCenter.NotificationCenterDelegate {

    // Tag used to Logging debug
    private final String TAG = "Main";

    private Toolbar toolbar;
    private RecyclerView contactsRecyclerView;

    private ViewPager viewPager;
    private DrawerLayout drawerLayout;
    private PagerSlidingTabStrip tabLayout;
    private Contacts contacts;

    // serviço de GPS
    private LocationService locationService;

    // contador para verificar o estado da internet
    private int counter;
    private boolean connected;

    private Application application;

    // observers
    private ContactsObserver contactsObserver;

    // receivers
    private NetworkReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_application);

        application = (br.com.aplication.Application) getApplicationContext();
        locationService = new LocationService(this);

        // instances network receiver
        networkReceiver = new NetworkReceiver();

        // add state change listener
        networkReceiver.addListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");

        registerReceiver(networkReceiver, intentFilter);

        counter = 0;

        // init application
        createToolbar();
        createDrawer();
        createDrawerListItem();
        listenerRefresh();
        createViewPager();
        createTabs();

        // registra ao observador para carregar os emojis
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        // instance the contacts uri observer to update list view when data changes
        contactsObserver = new ContactsObserver(new Handler(Looper.getMainLooper()), getApplicationContext(), contactsRecyclerView);

        // register observer contacts
        getContentResolver().registerContentObserver(ContactContract.CONTENT_URI, true, contactsObserver);
    }

    public void createToolbar() {

        // get toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        // set toolbar to replace default actionbar
        setSupportActionBar(toolbar);

        // display drawer toggle
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    public void createDrawerListItem() {

        counter++;

        // get recycler from view
        contactsRecyclerView = (RecyclerView) findViewById(R.id.drawer_box);

        // cria serviço de para buscar os contatos
        contacts = new Contacts(getApplicationContext(), locationService);

        // cria adapter
        final ContactsAdapter contactsAdapter = new ContactsAdapter(application, this, contacts, R.layout.row_contacts);

        // set adapter to recycler
        contactsRecyclerView.setAdapter(contactsAdapter);

        contacts.setRecyclerContacts(contactsRecyclerView);
        contacts.setContactsAdapter(contactsAdapter);

        // load contact from database
        contacts.load();

        // create layout manager
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        // set layout manager
        contactsRecyclerView.setLayoutManager(linearLayoutManager);
//        contactsRecyclerView.addItemDecoration(new LineDividerRecyclerView(getApplicationContext(), R.drawable.list_divider_contacts));

        // setup drawer width
        setupDrawerWidth(App.getDeviceWidthDIP(this) * 0.9f);

        // Abre o chat selecionado
        contactsAdapter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                application.counterAndShow();

                int position = contactsRecyclerView.getChildAdapterPosition(v);

                final Intent intent = new Intent(getApplicationContext(), Chat.class);
                intent.putExtra("contact", contactsAdapter.getContact(position));

                startActivity(intent);
            }
        });
    }

    private void listenerRefresh() {

        ((ContactsAdapter) contactsRecyclerView.getAdapter()).setBtRefreshClickListener(new View.OnClickListener() {

            private ContactsAdapter.HeaderViewHolder headerViewHolder;

            @Override
            public void onClick(View v) {

                headerViewHolder = (ContactsAdapter.HeaderViewHolder) contactsRecyclerView.findViewHolderForLayoutPosition(0);

                if (!Utils.isNetworkAvailable(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "Sem conexão com a internet", Toast.LENGTH_LONG).show();
                } else if (!Contacts.contactsLoaded) {
                    Toast.makeText(getApplicationContext(), "Contatos não carregados", Toast.LENGTH_LONG).show();
                } else {

                    // esconde
                    headerViewHolder.toggleBtRefresh(false);

                    // mostra
                    headerViewHolder.toggleProgressRefresh(true);

                    App.runBackgroundService(new Runnable() {
                        @Override
                        public void run() {

                            try {

                                Context context = Main.this.getApplicationContext();
                                PhoneContactsBridge.newInstance(context, UserDAO.getInstance(context), ContactDAO.instance(context), PhoneContactDAO.instance(context)).syncEdit();
                                contacts.prepareStatus();

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {

                                        // esconde
                                        headerViewHolder.toggleProgressRefresh(false);

                                        // mostra
                                        headerViewHolder.toggleBtRefresh(true);
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }
            }
        });
    }

    public void createDrawer() {

        // get drawer layout
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_applicaton);

        // instancia drawer toogle
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.app_name) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                // esconde o teclado
                Utils.hideKeyboard(Main.this);
            }
        };

        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        actionBarDrawerToggle.syncState();

    }

    public void createViewPager() {

        // set view pager for this activity
        viewPager = (ViewPager) findViewById(R.id.content);
        viewPager.setOffscreenPageLimit(3);

        // set adapter to view pager to handler fragment inside it
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(), getResources()));

    }

    private void createTabs() {

        tabLayout = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabLayout.setViewPager(viewPager);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {

            case KeyEvent.KEYCODE_BACK:
                moveTaskToBack(true);
                return true;

        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        application.counterAndShow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister network receiver
        this.unregisterReceiver(networkReceiver);

    }

    @Override
    public void networkAvailable() {

        if (!connected) {
            contacts.loadRostersStatus();
            Log.d(TAG, "Network available");
            connected = true;
        }

        counter++;
    }

    @Override
    public void networkUnavailable() {

        if (connected) {
            contacts.offlineStatus();
            connected = false;
            Log.d(TAG, "Network unavailable");
        }
    }

    private void setupDrawerWidth(float widthValue) {

        Resources resources = getResources();
        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthValue, resources.getDisplayMetrics());
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) contactsRecyclerView.getLayoutParams();
        params.width = (int) (width);
        contactsRecyclerView.setLayoutParams(params);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

        // Atualiza os emojis no recyclerview
        if (id == NotificationCenter.emojiDidLoaded)
            contacts.getContactsAdapter().notifyDataSetChanged();

    }

}
