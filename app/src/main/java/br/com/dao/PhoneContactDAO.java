package br.com.dao;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import br.com.aplication.Application;
import br.com.aplication.Phones;
import br.com.model.domain.Contact;

/**
 * Created by MarioJ on 06/06/15.
 */
public class PhoneContactDAO {

    private static final String TAG = "PhoneContactDAO";
    private Context context;
    private Application session;
    private String phoneDDD;
    private Uri CONTACTS_URI;

    public PhoneContactDAO(Context context) {
        this.context = context;
        this.session = (Application) context.getApplicationContext();
        this.phoneDDD = Phones.extractDDD(session.getCurrentUser().getPhone(), Phones.getCountryISO(context, session.getCurrentUser().getDdi()));
        this.CONTACTS_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    }

    public static PhoneContactDAO instance(Context context) {
        return new PhoneContactDAO(context);
    }

    public int countContacts() {

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    public Contact getLastContact() {

        String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection, null, null, ContactsContract.CommonDataKinds.Phone._ID + " DESC LIMIT 1");
        Contact contact = null;

        try {

            if (cursor.moveToFirst()) {

                contact = new Contact();
                contact.setId(cursor.getInt(0));
                contact.setPhone(Phones.parseNumber(session.getCurrentUser().getDdi(), phoneDDD, cursor.getString(1)));
            }

        } finally {
            cursor.close();
        }

        return contact;

    }


    public Contact getContact(int id) {

        String projection[] = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection,
                ContactsContract.CommonDataKinds.Phone._ID + "=?", new String[]{String.valueOf(id)}, null);

        Contact contact = null;

        try {

            if (cursor.moveToFirst()) {

                Application session = (Application) context.getApplicationContext();

                contact = new Contact();
                contact.setId(id);
                contact.setPhone(Phones.parseNumber(session.getCurrentUser().getDdi(), phoneDDD, cursor.getString(0)));
            }

        } finally {
            cursor.close();
        }

        return contact;
    }

    public String getContactName(int id) {

        String projection[] = new String[]{ContactsContract.Contacts.DISPLAY_NAME};

        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection, ContactsContract.Contacts._ID + "=?", new String[]{id + ""}, null);

        String name = null;

        try {

            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }

        } finally {
            cursor.close();
        }

        return name;
    }

    public Set<Contact> all() {

        Set<Contact> contacts = new LinkedHashSet<>();

        String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection, null, null, null);

        if (cursor.moveToFirst()) {

            do {

                Contact c = new Contact();
                c.setId(cursor.getInt(0));
                c.setPhone(Phones.parseNumber(session.getCurrentUser().getDdi(), phoneDDD, cursor.getString(1)));
                contacts.add(c);

            } while (cursor.moveToNext());

        }

        return contacts;
    }

    public Set<Integer> ids(Set<Integer> ids) {

        Set<Integer> contacs = new HashSet<>();

        String projection[] = new String[]{ContactsContract.Contacts._ID};

        String idsStr = TextUtils.join(",", ids);

        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection, ContactsContract.Contacts._ID + " in (" + idsStr + ")", null, null);

        try {

            if (cursor.moveToFirst()) {

                do {
                    contacs.add(cursor.getInt(0));
                } while (cursor.moveToNext());

            }

        } finally {
            cursor.close();
        }

        return contacs;
    }

    public String getContactPhone(int id) {

        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(this.CONTACTS_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, ContactsContract.Contacts._ID + "=?",
                    new String[]{String.valueOf(id)}, null);

            if (cursor.moveToFirst())
                return Phones.parseNumber(session.getCurrentUser().getDdi(), phoneDDD, cursor.getString(0));

        } finally {
            cursor.close();
        }

        return null;
    }

    public Uri lookupURI(int id) {

        String projection[] = new String[]{ContactsContract.Contacts.LOOKUP_KEY};

        Cursor cursor = context.getContentResolver().query(this.CONTACTS_URI, projection,
                ContactsContract.CommonDataKinds.Phone._ID + "=?", new String[]{String.valueOf(id)}, null);

        if (cursor.moveToFirst()) {

            try {
                return ContactsContract.Contacts.getLookupUri(id, cursor.getString(0));
            } finally {
                cursor.close();
            }
        }

        return null;
    }
}
