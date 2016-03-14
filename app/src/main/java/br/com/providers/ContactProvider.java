package br.com.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

import br.com.aplication.Storage;
import br.com.aplication.App;
import br.com.util.Utils;

/**
 * Created by MarioJ on 18/05/15.
 */
public class ContactProvider extends ContentProvider {

    private static final String TAG = "ContactProvider";
    public static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ContactContract.AUTHORITY, ContactContract.PROVIDER_PATH, ContactContract.CONTACTS);
        uriMatcher.addURI(ContactContract.AUTHORITY, ContactContract.PROVIDER_PATH + "/#", ContactContract.CONTACT_ID);
        uriMatcher.addURI(ContactContract.AUTHORITY, ContactContract.PROVIDER_PATH + "/exists", ContactContract.CONTACT_EXISTS);
    }

    private SQLiteDatabase sqLiteDatabase;

    @Override
    public boolean onCreate() {

        ContactDatabaseHelper contactDatabaseHelper = new ContactDatabaseHelper(getContext());
        sqLiteDatabase = contactDatabaseHelper.getWritableDatabase();

        return sqLiteDatabase != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor c;

        switch (uriMatcher.match(uri)) {

            case ContactContract.CONTACTS:
                sortOrder = String.format("%s ASC", ContactContract.Column.NAME.name());
                break;
            case ContactContract.CONTACT_ID:

                if (selection.trim().isEmpty())
                    selection = ContactContract.Column._ID + " = " + uri.getLastPathSegment();
                else
                    selection += " AND " + ContactContract.Column._ID + " = " + uri.getLastPathSegment();

                break;

            case ContactContract.CONTACT_EXISTS:
                return sqLiteDatabase.rawQuery(String.format("SELECT EXISTS(SELECT 1 FROM %s WHERE %s LIMIT 1)", ContactContract.PROVIDER_PATH, selection), selectionArgs);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return sqLiteDatabase.query(ContactDatabaseHelper.TABLE_CONTACTS, projection, selection, selectionArgs, null, null, sortOrder);
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /**
         * Add a new student record
         */
        long rowID = sqLiteDatabase.insert(ContactDatabaseHelper.TABLE_CONTACTS, null, values);
        /**
         * If record is added successfully
         */

        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(ContactContract.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }

        return null;

    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int count;

        count = sqLiteDatabase.delete(ContactDatabaseHelper.TABLE_CONTACTS, selection, selectionArgs);

        if (count > 0) {

            Uri _uri = null;

            if (selectionArgs != null) {
                _uri = ContentUris.withAppendedId(ContactContract.CONTENT_URI, Integer.valueOf(selectionArgs[0]));
            }

            getContext().getContentResolver().notifyChange(_uri, null);
        }

        return count;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count;

        switch (uriMatcher.match(uri)) {

            case ContactContract.CONTACTS:
                count = sqLiteDatabase.update(ContactDatabaseHelper.TABLE_CONTACTS, values,
                        selection, selectionArgs);
                break;

            case ContactContract.CONTACT_ID:
                count = sqLiteDatabase.update(ContactDatabaseHelper.TABLE_CONTACTS, values, String.format("%s = %s", ContactContract.Column._ID, uri.getPathSegments().get(1)) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return count;

    }

    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)) {
            /**
             * Get all student records
             */
            case ContactContract.CONTACTS:
                return "vnd.android.cursor.dir/vnd.wheresapp3.contacts";
            /**
             * Get a particular student
             */
            case ContactContract.CONTACT_ID:
                return "vnd.android.cursor.item/vnd.wheresapp3.contacts";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        int c = 0;

        try {
            sqLiteDatabase.beginTransaction();

            for (ContentValues value : values) {
                long rowAff = sqLiteDatabase.insert(ContactDatabaseHelper.TABLE_CONTACTS, null, value);
                c += rowAff > 0 ? 1 : 0;
            }

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }


        return c;
    }

    public static Uri makeContactExistsUri() {
        return Uri.parse(ContactContract.URI + "/exists");
    }

    public static class ContactDatabaseHelper extends SQLiteOpenHelper {

        final static String TABLE_CONTACTS = ContactContract.PROVIDER_PATH;

        public static final String CREATE_TABLE = "create table " + TABLE_CONTACTS + " (" + ContactContract.Column._ID + " INTEGER, "
                + ContactContract.Column.DDI + " VARCHAR(3), " + ContactContract.Column.PHONE_NUMBER + " VARCHAR(16), " + ContactContract.Column.NAME + " VARCHAR(40), "
                + ContactContract.Column.STATUS + " VARCHAR(150), " + ContactContract.Column.LAST_MODIFIED + " INTEGER, "
                + ContactContract.Column.LAST_SEE + " INTEGER, " + ContactContract.Column.PHOTO_TN + " BLOB, "
                + ContactContract.Column.LATITUDE + " REAL, " + ContactContract.Column.LONGITUDE + " REAL, " + ContactContract.Column.MODIFICATION.name() + " INTEGER, "
                + "PRIMARY KEY (" + ContactContract.Column._ID + "));";

        public ContactDatabaseHelper(Context context) {
            super(context, Utils.isExternalStorageWritable() ? Environment.getExternalStorageDirectory() + File.separator + Storage.ROOT_DIR + File.separator +
                    App.DB_NAME : App.DB_NAME, null, App.DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "[onCreate]");
            db.execSQL(UserProvider.UserDatabaseHelper.CREATE_TABLE);
            db.execSQL(CREATE_TABLE);
            db.execSQL(MessageProvider.MessagesDatabaseHelper.CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Log.d(TAG, "[onUpgrade]");

            db.execSQL("DROP TABLE IF EXISTS " + UserProvider.UserDatabaseHelper.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
            db.execSQL("DROP TABLE IF EXISTS " + MessageProvider.MessagesDatabaseHelper.TABLE);

            onCreate(db);
        }
    }
}
