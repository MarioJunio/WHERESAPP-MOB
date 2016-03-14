package br.com.resources;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.br.wheresapp.R;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.iqlast.LastActivityManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import br.com.adapter.ContactsAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.model.domain.Contact;
import br.com.service.DateService;
import br.com.service.LocationService;
import br.com.smack.Smack;
import br.com.util.Utils;

/**
 * Created by MarioJ on 26/08/15.
 */
public class Contacts extends ContextWrapper implements Observer {

    private final String TAG = "Contacts";
    private final long TIME_WAIT_LOAD = 1000l;

    private Application application;
    private RecyclerView recyclerContacts;
    private ContactsAdapter contactsAdapter;
    private ContactDAO contactDAO;
    private List<Contact> contacts;
    private LocationService locationService;

    // flag to indicate when contacts load is done
    public static boolean contactsLoaded;

    public Contacts(final Context context, LocationService locationService) {
        super(context);

        this.contactDAO = ContactDAO.instance(getApplicationContext());
        this.locationService = locationService;
        this.application = (Application) context;
    }

    public void waitLoad() throws InterruptedException {
        // wait load contacts from database
        while (!contactsLoaded)
            Thread.sleep(TIME_WAIT_LOAD);
    }

    public void load() {

        // cria handler para atualizar widgets na UI
        final Handler handler = new Handler(Looper.getMainLooper());

        // insere null para o header do recyclerview
        contactsAdapter.add(null, true);

        // insere null para o footer do recyclerview
        contactsAdapter.add(null, true);

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                contacts = contactDAO.all();

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        //
                        contactsAdapter.set(contacts);
                        contactsLoaded = true;

                        // contatos a serem enviados em broadcast a nova localização
                        locationService.start(contacts);
                    }
                });
            }
        });
    }

    public void loadRostersStatus() {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                // aguarda
                application.waitSmackService();

                // carrega o status de cada contato que é um roster do usuario atual
                prepareStatus();

                // adiciona observador ao objeto que vai ser observado
                application.smackService.addPresenceObserver(Contacts.this);

            }
        });

    }

    public void prepareStatus() {

        Handler handler = new Handler(Looper.getMainLooper());

        // aguarda se autenticar no ejabber server
        application.smackService.waitUntilOnline();

        // aguarda o carregamento dos contatos
        try {
            waitLoad();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!Utils.isNetworkAvailable(getApplicationContext())) {
            Log.d(TAG, "Network is not available !");
            return;
        } else if (!application.smackService.isOnline()) {
            Log.d(TAG, "Smack não esta conectado ou logado");
            return;
        }

        Roster roster = application.smackService.getRoster();

        // checa se os roster estão carregados, se não espera carregar
        if (roster.isLoaded()) {

            try {
                roster.reloadAndWait();
            } catch (SmackException.NotLoggedInException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }

        // instancia de LastActivityManager
        LastActivityManager lastActivityManager = application.smackService.getLastActivityManager();

        // itera sobre os rosters
        for (final RosterEntry user : roster.getEntries()) {

            String status = null, message = null;

            try {

                Presence presence = roster.getPresence(user.getUser());
                status = presence.isAvailable() ? getResources().getString(R.string.online) : getResources().getString(R.string.offline);
                message = (presence.getStatus() == null || presence.getStatus().isEmpty()) ? lastActivityManager.getLastActivity(user.getUser()).getStatusMessage() : presence.getStatus();

            } catch (Exception e) {
                Log.d(TAG, "User: " + user.getUser() + " - " + " não subescrito");
            }

            // obtem o numero do roster
            final String number = Smack.formatOnlyNumber(user.getUser());


            // posicao do roster no adapter
            final int position = contactsAdapter.getPosition(number);

            if (position < 1)
                return;

            // contato para ser atualizado
            final Contact c = contactsAdapter.getContact(position);

            if (c != null) {
                c.setPresence(status);
                c.setStatus(message == null || message.isEmpty() ? c.getStatus() : message);
            }

            // altera na UI, o status do contato associado ao roster atual
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // change list view at row
                    Log.d(TAG, "GET FROM " + c.getDdi() + c.getPhone() + " [" + c.getPresence() + "] " + "[" + c.getStatus() + "]");
                    contactsAdapter.notifyItemChanged(position);
                }
            });


            // checa se o status do contato não é vazio
            if (c.getStatus() != null && !c.getStatus().isEmpty()) {

                Log.d(TAG, "PERSISTINDO NOVO STATUS DE " + c.getDdi() + c.getPhone() + " -> " + status + " -> " + message);

                // atualiza status
                contactDAO.updateStatus(c);

            }
        }
    }

    public void search(String query) {

        if (query == null || query.isEmpty()) {

            // limpa o adaptador de contatos
            contactsAdapter.clear();

            // readiciona o header
            contactsAdapter.add(null, true);

            // preenche os contatos
            contactsAdapter.set(contacts);

        } else if (!query.isEmpty()) {

            List<Contact> contactsRetrieved = new ArrayList<>();

            for (Contact c : contacts) {

                if (c.getName() != null && c.getName().toLowerCase().startsWith(query)) {
                    contactsRetrieved.add(c);
                }

            }

            // armazena quantidade de items na lista
            int count = contactsAdapter.getItemCount();

            // remove sempre o segundo elemento da lista, pois ela atualiza seu indice a cada remoção
            for (int i = 1; i < count; i++)
                contactsAdapter.remove(1);

            contactsAdapter.set(contactsRetrieved);
        }
    }

    public void offlineStatus() {

        // inicia no indice 1, pois o indice 0 está o header
        for (int i = 1; i < contactsAdapter.getItemCount(); i++)
            contactsAdapter.setStatusView((ContactsAdapter.ContactViewHolder) recyclerContacts.findViewHolderForLayoutPosition(i), getResources().getString(R.string.out_network));

    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setRecyclerContacts(RecyclerView recyclerContacts) {
        this.recyclerContacts = recyclerContacts;
    }

    public void setContactsAdapter(ContactsAdapter contactsAdapter) {
        this.contactsAdapter = contactsAdapter;
    }

    public ContactsAdapter getContactsAdapter() {
        return contactsAdapter;
    }

    @Override
    public void update(Observable observable, final Object data) {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                final Presence getPresence = (Presence) data;

                final String number = Smack.formatOnlyNumber(getPresence.getFrom());
                final String status = getPresence.isAvailable() ? getResources().getString(R.string.online) : DateService.formatLastActiviy(new Date()), message = getPresence.getStatus();

                final int position = contactsAdapter.getPosition(number);

                if (position > 0) {

                    final Contact contact = contactsAdapter.getContact(position);

                    if (contact != null) {
                        contact.setPresence(status);
                        contact.setStatus(message == null || message.isEmpty() ? contact.getStatus() : message);
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            contactsAdapter.notifyItemChanged(position);
                            Log.d(TAG, "NEW UPDATE FROM " + number + " STATUS -: " + status + " MESSAGE -: " + message);
                        }
                    });
                }

            }
        });

    }
}
