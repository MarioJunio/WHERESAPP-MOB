package br.com.observers;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;

import com.google.common.collect.Sets;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.RosterEntry;

import java.util.HashSet;
import java.util.Set;

import br.com.adapter.ContactsAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.model.domain.Contact;
import br.com.net.Xmpp;
import br.com.providers.ContactContract;
import br.com.providers.ContactProvider;
import br.com.smack.Smack;

/**
 * Created by MarioJ on 26/07/15.
 */
public class ContactsObserver extends ContentObserver {

    private final String TAG = "ContactsObserver";

    private Application application;
    private ContactsAdapter contactsAdapter;
    private ContactDAO contactDAO;
    private int count = 0;

    public static long deletedID;

    public ContactsObserver(Handler handler, Context context, RecyclerView recyclerContacts) {
        super(handler);

        this.contactsAdapter = (ContactsAdapter) recyclerContacts.getAdapter();
        this.contactDAO = ContactDAO.instance(context);
        this.application = (Application) context;

        recount();
    }

    @Override
    public void onChange(boolean selfChange) {

        try {

            int newCount = contactDAO.getCount();

            if (newCount > count) {

                count = newCount;

                Contact c = contactDAO.getLastContactByModification();

                if (c != null)
                    add(c);

            } else
                updateContact(deletedID);

            recount();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {

        try {

            switch (ContactProvider.uriMatcher.match(uri)) {

                case ContactContract.CONTACT_ID:
                    updateContact(Integer.valueOf(uri.getLastPathSegment()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateContact(long id) throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {

        Contact c = contactDAO.getContact(id);

        if (c != null)
            add(c);
        else
            remove();

    }

    private void add(Contact c) {


        try {

            // adiciona ao recyclerview
            contactsAdapter.add(c, true);

            // SUBSCRIBE
            application.smackService.subscribe(c.getDdi(), c.getPhone());
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void remove() {

        // remove do recyclerview
        contactsAdapter.remove(deletedID);

        // UNSUBSCRIBE
        // conjunto para armazenar os rosters e os contatos locais
        Set<String> smackUsers = new HashSet<>(), wheresappUsers = new HashSet<>();

        // obtem o conjunto de rosters associados a este usuario
        Set<RosterEntry> entries = application.smackService.getRoster().getEntries();

        // itera sobre os rosters e adiciona-os ao conjunto do smack
        for (RosterEntry entry : entries)
            smackUsers.add(Smack.parseSmackUser(entry.getUser()));

        // itera sobre os contatos, e os adiciona ao conjunto dos contatos locais
        for (Contact c : contactDAO.all())
            wheresappUsers.add(Smack.toSmackUser(c.getDdi(), c.getPhone()));

        // calcula o conjunto de diferença entre os rosters, e os contatos locais
        final Sets.SetView<String> difference = Sets.difference(smackUsers, wheresappUsers);

        for (String user : difference) {

            try {
                application.smackService.unsubscribe(user.concat(Xmpp.DOMAIN));
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }

        }
    }

    private void recount() {
        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {
                count = contactDAO.getCount();
            }
        });
    }

}
