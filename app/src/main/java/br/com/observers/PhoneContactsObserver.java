package br.com.observers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.dao.UserDAO;
import br.com.model.domain.Contact;
import br.com.service.PhoneContactsBridge;
import br.com.aplication.Application;
import br.com.sync.Sync;
import br.com.sync.SyncAdapter;

/**
 * Created by MarioJ on 01/06/15.
 */
public class PhoneContactsObserver extends ContentObserver {

    private final String TAG = "PhoneContactsObserver";

    private Context context;
    private ContactDAO contactDAO;
    private PhoneContactDAO phoneContactDAO;
    private UserDAO userDAO;
    private PhoneContactsBridge phoneContactsBridge;
    private int ammount;
    private long delay;
    private Timer timerUpdateSync;
    private boolean isActived;

    private final int DELAY_EDIT = 3000, DELAY_UPDATE_SYNC = 5000;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public PhoneContactsObserver(Handler handler, Context context) {
        super(handler);

        this.context = context;
        contactDAO = ContactDAO.instance(context);
        phoneContactDAO = PhoneContactDAO.instance(context);
        userDAO = UserDAO.getInstance(context);
        phoneContactsBridge = PhoneContactsBridge.newInstance(context, userDAO, contactDAO, phoneContactDAO);
        ammount = phoneContactDAO.countContacts();
        delay = System.currentTimeMillis();
        timerUpdateSync = new Timer();
    }

    @Override
    public void onChange(boolean selfChange) {

        int currentAmmount = phoneContactDAO.countContacts();

        try {

            // contact inserted, otherwise updated or deleted
            if (currentAmmount > ammount) {

                // syncronize ammount with current ammount on phone contacts
                ammount = currentAmmount;
                syncAdd();

            } else if (currentAmmount < ammount) {
                // syncronize ammount with current ammount on phone contacts
                ammount = currentAmmount;
                syncDelete();

            } else {

                if (System.currentTimeMillis() > delay) {

                    Log.d(TAG, "EDIT");

                    if (!isActived) {

                        timerUpdateSync.schedule(new TimerTask() {

                            @Override
                            public void run() {

                                try {
                                    phoneContactsBridge.syncEdit();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } finally {
                                    isActived = !isActived;
                                }

                            }

                        }, DELAY_UPDATE_SYNC);

                        isActived = !isActived;

                    } else {
                        Log.d(TAG, "EDIT ALREADY ACTIVE");
                    }

                }
            }

            delay = System.currentTimeMillis() + DELAY_EDIT;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void syncAdd() {

        // get last contact inserted
        Contact lastContact = phoneContactDAO.getLastContact();

        // search contact phone in wheresapp contacts
        Contact contactWheresapp = contactDAO.getContact(lastContact.getPhone());

        // get current session
        Application session = (Application) context;

        if (lastContact.getPhone().equals(session.getCurrentUser().getDdi() + session.getCurrentUser().getPhone()) || contactWheresapp != null) {
            Log.d("ContactObserver [insert]", "Wheresapp session number is equal or wheresapp contact with same number already exists.");
            return;
        }

        // create a bundle package with attributes to sync adapter request
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putInt(SyncAdapter.OPERATION, SyncAdapter.SYNC_CONTACT);
        bundle.putInt(Contact.ID, lastContact.getId());
        bundle.putString(Contact.PHONE, lastContact.getPhone());

        // call sync adapter to make http request
        Sync.requestSync(context, bundle);

    }

    /**
     * Syncronize the contact deleted on phone with wheresapp database using _id to find which contact was deleted
     */
    private void syncDelete() {

        // wheresapp contact's id
        Set<Integer> ids = contactDAO.ids();

        // phone filtered contact's id
        Set<Integer> phoneIDs = phoneContactDAO.ids(ids);

        Sets.SetView<Integer> difference = Sets.difference(ids, phoneIDs);

//      loop throught difference and delete all ids that are not syncronizeds
        for (Integer id : difference) {
            ContactsObserver.deletedID = id;
            contactDAO.delete(id);
        }
    }

}
