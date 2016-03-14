package br.com.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.model.domain.Contact;
import br.com.net.Http;
import br.com.service.DateService;

/**
 * Created by MarioJ on 27/05/15.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "SyncAdapter";
    public static final String OPERATION = "operation";
    public static final String CHECKSUM = "checksum";

    /**
     * SYNC_CONTACTS is used to syncronize all contacts in provider with news informations in server
     * SYNC_CONTACT is used to syncronize contact number with server
     */
    public static final int SYNC_CONTACTS = 1, SYNC_CONTACT = 2;

    private ContentResolver contentResolver;
    private ContactDAO contactDAO;
    private PhoneContactDAO phoneContactDAO;
    private String userPhoneNumber;
    private Application session;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        init(context);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        init(context);

    }

    public void init(Context context) {

        session = (Application) context.getApplicationContext();

        // content resolver to using with syncrinozation
        contentResolver = context.getContentResolver();
        contactDAO = ContactDAO.instance(context);
        phoneContactDAO = PhoneContactDAO.instance(context);
        userPhoneNumber = session.getCurrentUser().getDdi() + session.getCurrentUser().getPhone();

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        if (extras != Bundle.EMPTY) {

            try {

                // get operation to perform
                final int operation = extras.getInt(OPERATION);

                switch (operation) {

                    case SYNC_CONTACT:
                        syncronizeContact(extras.getInt(Contact.ID), extras.getString(Contact.PHONE));
                        break;

                    case SYNC_CONTACTS:
                        syncronizeContacts(extras.getString(CHECKSUM));
                        break;

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Log.d("ContactsSyncAdapter [operation]", "Empty");
        }

    }

    private void syncronizeContact(int id, String phone) {

        try {

            // make http request to retrieve contact by number
            Contact contactRes = Http.checkContact(phone);

            // check if contact retrieved by http requests exists on server
            if (contactRes != null) {

                // set contact retrieved with id number
                contactRes.setId(id);

                // retrieve phone contact to checking
                Contact contactPhone = phoneContactDAO.getContact(contactRes.getId());

                /** check if contact wheresapp don't exists in wheresapp database because whether it exists is not necessary persist again, and
                 * check if contact phone yet exists on phone database, because if it don't exists is not necessary persist on wheresapp database.
                 */
                if (contactPhone != null) {

                    contactRes.setModification(DateService.getTimeNow());
                    contactRes.setLastSee(null);
                    contactDAO.persist(contactRes);

                }

            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage() + " - " + e.getCause());
        }

    }

    private void syncronizeContacts(String checksum) throws IOException {

        Set<Contact> contactsDevice = phoneContactDAO.all();
        Map<String, Integer> mapContactsDevice = new HashMap<>();

        Set<String> phonesDevice = new HashSet<>();
        Set<String> phonesWheresapp = contactDAO.getPhones();

        for (Contact c : contactsDevice) {

            String contactPhone = c.getPhone();

            if (!contactPhone.equals(userPhoneNumber)) {
                mapContactsDevice.put(contactPhone, c.getId());
                phonesDevice.add(contactPhone);
            }
        }

        // difference between sets
        Sets.SetView<String> difference = Sets.difference(phonesDevice, phonesWheresapp);

        if (!difference.isEmpty()) {

            // get numbers as string
            String numbers = difference.toString();

            // remove brackets to format numbers
            numbers = numbers.substring(1, numbers.length() - 1).replaceAll("[\\s]", "");

            // send to server and get synchronized contacts
            List<Contact> contacts = Http.syncronizeContacts(numbers);

            // check if there are contacts retrieved from server
            if (contacts != null && !contacts.isEmpty()) {

                // loop through contacts to persist in wheresapp database
                for (Contact c : contacts) {

                    // check if contact yet not exists on wheresapp
                    if (!contactDAO.exists(c.getDdi(), c.getPhone())) {

                        c.setModification(DateService.getTimeNow());

                        // get id from contact
                        c.setId(mapContactsDevice.get(c.getDdi() + c.getPhone()));

                        // persist to contacts wheresapp
                        c.setModification(DateService.getTimeNow());

                        // set null last because contact was never online
                        c.setLastSee(null);

                        contactDAO.persist(c);
                    }
                }

            }

        }

        // set new checksum
        session.getCurrentUser().setContactsCheckSum(checksum);

        // update in database
        session.update();
    }
}
