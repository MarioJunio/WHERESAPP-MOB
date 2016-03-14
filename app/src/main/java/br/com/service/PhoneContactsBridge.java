package br.com.service;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.dao.UserDAO;
import br.com.model.domain.Contact;
import br.com.observers.ContactsObserver;
import br.com.sync.Sync;
import br.com.sync.SyncAdapter;
import br.com.aplication.Phones;

/**
 * Created by MarioJ on 11/09/15.
 */
public class PhoneContactsBridge {

    private final String TAG = "PhoneContactsBridge";

    private Context context;
    private UserDAO userDAO;
    private ContactDAO contactDAO;
    private PhoneContactDAO phoneContactDAO;

    public PhoneContactsBridge(Context context, UserDAO userDAO, ContactDAO contactDAO, PhoneContactDAO phoneContactDAO) {
        this.context = context;
        this.userDAO = userDAO;
        this.contactDAO = contactDAO;
        this.phoneContactDAO = phoneContactDAO;
    }

    public static PhoneContactsBridge newInstance(Context context, UserDAO userDAO, ContactDAO contactDAO, PhoneContactDAO phoneContactDAO) {
        return new PhoneContactsBridge(context, userDAO, contactDAO, phoneContactDAO);
    }

    public void syncEdit() throws IOException, NoSuchAlgorithmException {

        // calculate the contact's checksum
        String checksum = Phones.getContactsCheckSum(phoneContactDAO.all());

        // check whether checksum was change
        if (hasEdited(checksum)) {

            // delete wheresapp contacts, that are not in phone
            deleteUnsyncContacts();

            // create a bundle package with attributes to sync adapter request
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            bundle.putInt(SyncAdapter.OPERATION, SyncAdapter.SYNC_CONTACTS);
            bundle.putString(SyncAdapter.CHECKSUM, checksum);

            // call sync adapter to make http request
            Sync.requestSync(context, bundle);
        }
    }

    public void deleteUnsyncContacts() {

        // non sync contacts list
        List<Integer> contactsToSync = new ArrayList<>();

        // get wheresapp contacts 'id' and 'number'
        List<Contact> contactsWheresapp = contactDAO.all2Edit();

        // loop throught phone contacts until find edited number
        for (Contact c : contactsWheresapp) {

            // get contact phone by id
            String phone = phoneContactDAO.getContactPhone(c.getId());

            // check if contact phone retrieved by id exists and phone is not equal phone contact wheresapp
            if (phone != null && !phone.equals(c.getDdi() + c.getPhone())) {
                contactsToSync.add(c.getId());
            }

        }

        Log.d(TAG, contactsToSync.toString());

        // check if there is contacts to sync with wheresapp
        if (!contactsToSync.isEmpty()) {

            for (Integer id : contactsToSync) {

                ContactsObserver.deletedID = id;

                // delete contact by id from wheresapp
                contactDAO.delete(id);
            }
        }

    }

    public boolean hasEdited(String currentChecksum) throws NoSuchAlgorithmException {

        Application session = (Application) context;

        // get the last checksum
        String checksum = userDAO.getChecksum(session.getCurrentUser().getId());

        // set new checksum
        session.getCurrentUser().setContactsCheckSum(checksum);

        if (currentChecksum == null)
            return false;

        return !currentChecksum.equals(session.getCurrentUser().getContactsCheckSum());

    }

}
