package br.com.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.model.domain.Contact;
import br.com.providers.ContactContract;
import br.com.providers.ContactProvider;
import br.com.service.ImageService;
import br.com.smack.Smack;

/**
 * Created by MarioJ on 22/05/15.
 */
public class ContactDAO extends DAO {

    private final String TAG = "ContactDAO";

    private ContactDAO() {
    }

    public static ContactDAO instance(Context context) {

        ContactDAO contactDAO = new ContactDAO();
        contactDAO.setContext(context);
        contactDAO.setContentUri(ContactContract.CONTENT_URI);

        return contactDAO;
    }

    public List<Contact> all() {

        List<Contact> contacts = new ArrayList<>();

        String[] projection = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.NAME.name(), ContactContract.Column.DDI.name(),
                ContactContract.Column.PHONE_NUMBER.name(), ContactContract.Column.STATUS.name(), ContactContract.Column.LAST_MODIFIED.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, null, null, ContactContract.Column.NAME.name() + " ASC");

        try {

            if (c.moveToFirst()) {

                do {

                    Contact contact = new Contact();

                    contact.setId(c.getInt(0));
                    contact.setName(c.getString(1));
                    contact.setDdi(c.getString(2));
                    contact.setPhone(c.getString(3));
                    contact.setPresence(Contact.StatusType.NETWORK_UNAVAILABLE.name());
                    contact.setStatus(c.getString(4));
                    contact.setLastModified(new Date(c.getLong(5)));

                    contacts.add(contact);

                } while (c.moveToNext());

            }

        } finally {
            c.close();
        }

        return contacts;
    }

    public Object[] all2Map() {

        List<Contact> contacts = new ArrayList<>();
        Map<String, Contact> contactsMap = new HashMap<>();

        /*
        Random random = new Random();

        for (int i = 0; i < 50; i++) {

            Contact contact = new Contact();

            contact.setId(i);
            contact.setDdi("55");
            contact.setPhone(i + "" + (i + 1) + "" + (i + 2) + "" + (i + 3) + "" + (i + 4));
            contact.setName("Test");
            contact.setStatus("Debug test");
            contact.setLatitude((random.nextDouble() + 18) * -1);
            contact.setLongitude((random.nextDouble() + 47) * -1);
            contact.setPhoto(null);
            contact.setStatusType(Contact.StatusType.OUT_NETWORK);

            contactsMap.put(Smack.toSmackUser(contact.getDdi(), contact.getPhone()), contact);
            contacts.add(contact);

        } */

        String[] projection = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.DDI.name(),
                ContactContract.Column.PHONE_NUMBER.name(), ContactContract.Column.STATUS.name(), ContactContract.Column.LATITUDE.name(), ContactContract.Column.LONGITUDE.name(),
                ContactContract.Column.PHOTO_TN.name()};

        String where = String.format("%s is not null AND %s is not null", ContactContract.Column.LATITUDE.name(), ContactContract.Column.LONGITUDE.name());

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, where, null, ContactContract.Column.NAME.name() + " ASC");

        try {

            if (c.moveToFirst()) {

                do {

                    Contact contact = new Contact();

                    contact.setId(c.getInt(0));
                    contact.setDdi(c.getString(1));
                    contact.setPhone(c.getString(2));
                    contact.setPresence(Contact.StatusType.NETWORK_UNAVAILABLE.name());
                    contact.setStatus(c.getString(3));
                    contact.setLatitude(c.getDouble(4));
                    contact.setLongitude(c.getDouble(5));
                    contact.setPhoto(c.getBlob(6));

                    contactsMap.put(Smack.toSmackUser(contact.getDdi(), contact.getPhone()), contact);
                    contacts.add(contact);

                } while (c.moveToNext());

            }

        } finally {
            c.close();
        }

        return new Object[]{contacts, contactsMap};

    }

    public List<Contact> all2Edit() {

        List<Contact> contacts = new ArrayList<>();

        String[] projection = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, null, null, null);

        try {

            if (c.moveToFirst()) {

                do
                    contacts.add(new Contact(c.getInt(0), c.getString(1), c.getString(2)));
                while (c.moveToNext());

            }

        } finally {
            c.close();
        }

        return contacts;
    }

    public Set<Integer> ids() {

        Set<Integer> contacts = new HashSet<>();

        String[] projection = new String[]{ContactContract.Column._ID.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, null, null, null);

        try {

            if (c.moveToFirst()) {

                do
                    contacts.add(c.getInt(0));
                while (c.moveToNext());

            }

        } finally {
            c.close();
        }

        return contacts;
    }

    public Bitmap getImage(int id) {

        Cursor c = getContext().getContentResolver().query(getContentUri(), new String[]{ContactContract.Column.PHOTO_TN.name()}, ContactContract.Column._ID.name() + "=?",
                new String[]{String.valueOf(id)}, null);

        if (c.moveToFirst()) {

            byte[] bytes = c.getBlob(c.getColumnIndex(ContactContract.Column.PHOTO_TN.name()));

            if (bytes != null)
                return ImageService.byteToImage(bytes);
        }

        return null;
    }

    public Contact getContact(long id) {

        String projection[] = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name(), ContactContract.Column.NAME.name(),
                ContactContract.Column.STATUS.name(), ContactContract.Column.LATITUDE.name(), ContactContract.Column.LONGITUDE.name(), ContactContract.Column.LAST_MODIFIED.name()
                , ContactContract.Column.LAST_SEE.name()};

        Cursor cursor = getContext().getContentResolver().query(getContentUri(), projection, ContactContract.Column._ID.name() + "=?",
                new String[]{String.valueOf(id)}, null);

        Contact contact = null;

        try {

            if (cursor.moveToFirst()) {
                contact = new Contact();
                contact.setId(cursor.getInt(0));
                contact.setDdi(cursor.getString(1));
                contact.setPhone(cursor.getString(2));
                contact.setName(cursor.getString(3));
                contact.setPresence(Contact.StatusType.NETWORK_UNAVAILABLE.name());
                contact.setStatus(cursor.getString(4));
                contact.setLatitude(cursor.getDouble(5));
                contact.setLongitude(cursor.getDouble(6));
                contact.setLastModified(new Date(cursor.getInt(7)));
                contact.setLastSee(new Date(cursor.getInt(8)));
            }


        } finally {
            cursor.close();
        }

        return contact;
    }

    public Contact getContact(String phone) {

        String projection[] = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name(), ContactContract.Column.NAME.name(),
                ContactContract.Column.STATUS.name(), ContactContract.Column.LATITUDE.name(), ContactContract.Column.LONGITUDE.name(), ContactContract.Column.LAST_MODIFIED.name()
                , ContactContract.Column.LAST_SEE.name()};

        Cursor cursor = getContext().getContentResolver().query(getContentUri(), projection, String.format("%s || %s = ?", ContactContract.Column.DDI.name(),
                ContactContract.Column.PHONE_NUMBER.name()), new String[]{phone}, null);

        Contact contact = null;

        try {

            if (cursor.moveToFirst()) {
                contact = new Contact();
                contact.setId(cursor.getInt(0));
                contact.setDdi(cursor.getString(1));
                contact.setPhone(cursor.getString(2));
                contact.setName(cursor.getString(3));
                contact.setPresence(Contact.StatusType.NETWORK_UNAVAILABLE.name());
                contact.setStatus(cursor.getString(4));
                contact.setLatitude(cursor.getDouble(5));
                contact.setLongitude(cursor.getDouble(6));
                contact.setLastModified(new Date(cursor.getInt(7)));
                contact.setLastSee(new Date(cursor.getInt(8)));
            }

        } finally {
            cursor.close();
        }

        return contact;
    }

    public void persist(Contact contact) {

        ContentValues values = new ContentValues();
        values.put(ContactContract.Column._ID.name(), contact.getId());
        values.put(ContactContract.Column.DDI.name(), contact.getDdi());
        values.put(ContactContract.Column.PHONE_NUMBER.name(), contact.getPhone());
        values.put(ContactContract.Column.MODIFICATION.name(), contact.getModification().getTime());

        getContext().getContentResolver().insert(getContentUri(), values);
    }

    public void updateContactAndLastModified(Contact c) {

        ContentValues values = new ContentValues();
        values.put(ContactContract.Column.PHOTO_TN.name(), c.getPhoto());
        values.put(ContactContract.Column.LAST_MODIFIED.name(), c.getLastModified().getTime());

        getContext().getContentResolver().update(getContentUri(), values, ContactContract.Column._ID.name() + "=?", new String[]{String.valueOf(c.getId())});
    }

    public void updateStatus(Contact c) {

        ContentValues values = new ContentValues();
        values.put(ContactContract.Column.STATUS.name(), c.getStatus());

        getContext().getContentResolver().update(getContentUri(), values, ContactContract.Column._ID.name() + "=?", new String[]{String.valueOf(c.getId())});
    }

    public void updateLocation(String ddi, String phone, double latitude, double longitude) {

        ContentValues values = new ContentValues();
        values.put(ContactContract.Column.LATITUDE.name(), latitude);
        values.put(ContactContract.Column.LONGITUDE.name(), longitude);

        getContext().getContentResolver().update(getContentUri(), values, ContactContract.Column.DDI.name() + "=? AND " + ContactContract.Column.PHONE_NUMBER.name() + "=?", new String[]{ddi, phone});

        Log.d(TAG, "Saving location from " + ddi + phone);
    }

    public void delete(Integer id) {
        getContext().getContentResolver().delete(getContentUri(), ContactContract.Column._ID.name() + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * delete all contacts from wheresapp
     */
    public void delete() {
        getContext().getContentResolver().delete(ContactContract.CONTENT_URI, null, null);
    }

    public Set<String> getPhones() {

        Set<String> phoneNumbers = new HashSet<>();
        Cursor c = getContext().getContentResolver().query(getContentUri(), new String[]{ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name()}, null, null, null);

        try {

            if (c.moveToFirst()) {

                do {
                    phoneNumbers.add(c.getString(0) + c.getString(1));
                } while (c.moveToNext());
            }

        } finally {
            c.close();
        }

        return phoneNumbers;
    }

    public boolean exists(String ddi, String phone) {

        Cursor c = getContext().getContentResolver().query(ContactProvider.makeContactExistsUri(), null, ContactContract.Column.DDI + "=? AND " + ContactContract.Column.PHONE_NUMBER + "=?", new String[]{ddi, phone}, null);

        try {

            if (c.moveToFirst() && c.getInt(0) > 0)
                return true;

        } finally {
            c.close();
        }

        return false;
    }

    public Contact getLastContactByModification() {

        Contact contact = null;

        String projection[] = new String[]{ContactContract.Column._ID.name(), ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name(), ContactContract.Column.NAME.name(),
                ContactContract.Column.STATUS.name(), ContactContract.Column.LATITUDE.name(), ContactContract.Column.LONGITUDE.name(), ContactContract.Column.LAST_MODIFIED.name()
                , ContactContract.Column.LAST_SEE.name(), String.format("MAX(%s)", ContactContract.Column.MODIFICATION.name())};

        Cursor cursor = getContext().getContentResolver().query(getContentUri(), projection, null, null, "LIMIT 1");

        try {

            if (cursor.moveToFirst()) {
                contact = new Contact();
                contact.setId(cursor.getInt(0));
                contact.setDdi(cursor.getString(1));
                contact.setPhone(cursor.getString(2));
                contact.setName(cursor.getString(3));
                contact.setStatus(cursor.getString(4));
                contact.setLatitude(cursor.getDouble(5));
                contact.setLongitude(cursor.getDouble(6));
                contact.setLastModified(new Date(cursor.getInt(7)));
                contact.setLastSee(new Date(cursor.getInt(8)));
            }

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return contact;

    }

    public int getCount() {

        int count = 0;
        String projection[] = new String[]{ContactContract.Column._ID.name()};

        Cursor cursor = getContext().getContentResolver().query(getContentUri(), projection, null, null, null);

        try {
            count = cursor.getCount();
        } finally {

            if (cursor != null)
                cursor.close();
        }

        return count;

    }

    public Contact get(String ddi, String phone) {

        String[] projection = new String[]{
                ContactContract.Column._ID.name(),
                ContactContract.Column.PHOTO_TN.name()
        };

        String where = String.format("%s=? AND %s=?", ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name());
        String[] args = new String[]{ddi, phone};

        Cursor c = null;

        try {

            c = getContext().getContentResolver().query(getContentUri(), projection, where, args, null);

            if (c.moveToFirst())
                return Contact.create(c.getInt(0), c.getBlob(1));

        } finally {

            if (c != null)
                c.close();
        }

        return null;

    }

    public byte[] getImage(String ddi, String phone) {

        Cursor c = getContext().getContentResolver().query(getContentUri(), new String[]{ContactContract.Column.PHOTO_TN.name()},
                ContactContract.Column.DDI.name() + "=? AND " + ContactContract.Column.PHONE_NUMBER.name() + "=?", new String[]{ddi, phone}, null);

        byte[] bytes = null;

        if (c.moveToFirst())
            bytes = c.getBlob(c.getColumnIndex(ContactContract.Column.PHOTO_TN.name()));

        return bytes;
    }

    public void clear(String ddi, String phone) {
        String where = String.format("%s=? AND %s=?", ContactContract.Column.DDI.name(), ContactContract.Column.PHONE_NUMBER.name());
        getContext().getContentResolver().delete(getContentUri(), where, new String[]{ddi, phone});
    }
}
