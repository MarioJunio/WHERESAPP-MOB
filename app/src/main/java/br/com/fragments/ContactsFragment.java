package br.com.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.br.wheresapp.R;

import br.com.adapter.ContactsAdapter;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.dao.UserDAO;
import br.com.decoration.LineDividerRecyclerView;
import br.com.activities.Chat;
import br.com.activities.Main;
import br.com.observers.ContactsObserver;
import br.com.providers.ContactContract;
import br.com.receivers.NetworkReceiver;
import br.com.resources.Contacts;
import br.com.service.LocationService;
import br.com.service.PhoneContactsBridge;
import br.com.aplication.Application;
import br.com.aplication.App;
import br.com.util.Utils;

/**
 * Created by MarioJ on 06/03/15.
 */
public class ContactsFragment extends Fragment implements NetworkReceiver.NetworkStateReceiverListener {

    private String TAG = "ContactsFragment";

    private Main application;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerContacts;
    private ContactsAdapter contactsAdapter;
    private ContactsObserver contactsObserver;
    private Application session;
    private Contacts contacts;
    private NetworkReceiver networkReceiver;
    private LocationService locationService;

    // counter
    private short counter;

    // DAO
    private ContactDAO contactDAO;

    public ContactsFragment() {
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        application = (Main) getActivity();
        session = (Application) getActivity().getApplication();
        locationService = new LocationService(getActivity().getApplicationContext());

        counter = 0;

        // instances network receiver
        networkReceiver = new NetworkReceiver();

        // add state change listener
        networkReceiver.addListener(this);

        // register receiver for this activity
        getActivity().registerReceiver(networkReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        contactDAO = ContactDAO.instance(getContext());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        counter++;

        // get swipe refresh layout
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.contacts_swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.app_color, R.color.black, R.color.light_blue);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                try {

                    if (!Utils.isNetworkAvailable(getContext())) {
                        Toast.makeText(getContext(), "Sem conexão com a internet", Toast.LENGTH_LONG).show();
                    } else if (!Contacts.contactsLoaded) {
                        Toast.makeText(getContext(), "Contatos não carregados", Toast.LENGTH_LONG).show();
                    } else {

                        App.runBackgroundService(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    Context context = getActivity().getApplicationContext();
                                    PhoneContactsBridge.newInstance(context, UserDAO.getInstance(context), ContactDAO.instance(context), PhoneContactDAO.instance(context)).syncEdit();
                                    contacts.prepareStatus();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                    }
                } finally {

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });

        // get list view for contaxts
        recyclerContacts = (RecyclerView) view.findViewById(R.id.recycler_contacts);
        recyclerContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerContacts.addItemDecoration(new LineDividerRecyclerView(getContext(), R.drawable.list_divider_contacts));
        recyclerContacts.setItemAnimator(new DefaultItemAnimator());

        // cria o adaptador para contatos
        contactsAdapter = new ContactsAdapter(session, getActivity(), contacts, R.layout.row_contacts);

        // seta adaptador a lista de contatos
        recyclerContacts.setAdapter(contactsAdapter);

        // instance contact resources
//        contacts = new Contacts(getActivity().getApplicationContext(), recyclerContacts, locationService);

        // load contact from database
        contacts.load();

        // load status
        if (counter > 0)
            contacts.loadRostersStatus();

        // set listener to start activity chat
        contactsAdapter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                int position = recyclerContacts.getChildAdapterPosition(v);

                final Intent intent = new Intent(getActivity(), Chat.class);
                intent.putExtra("contact", contactsAdapter.getContact(position));

                startActivity(intent);
            }
        });

//         instance the contacts uri observer to update list view when data changes
        contactsObserver = new ContactsObserver(new Handler(), getActivity().getApplicationContext(), recyclerContacts);

        // register observer contacts
        getActivity().getContentResolver().registerContentObserver(ContactContract.CONTENT_URI, true, contactsObserver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister network receiver
        getActivity().unregisterReceiver(networkReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contacts, menu);

        MenuItem item = menu.findItem(R.id.search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint("Buscar contato");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                contacts.search(query.toLowerCase());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                contacts.search(newText.toLowerCase());
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                contacts.search(null);
                return true;
            }
        });

        MenuItemCompat.collapseActionView(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.add_user)
            newContact();

        return super.onOptionsItemSelected(item);
    }

    private void newContact() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        startActivity(intent);
    }

    @Override
    public void networkAvailable() {

        if (counter > 1)
            contacts.loadRostersStatus();

        counter++;
    }

    @Override
    public void networkUnavailable() {
        contacts.offlineStatus();
    }

}
