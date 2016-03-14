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

import java.io.File;

import br.com.aplication.Storage;
import br.com.aplication.App;
import br.com.util.Utils;

/**
 * Created by MarioJ on 23/07/15.
 */
public class MessageProvider extends ContentProvider {

    private static final String TAG = "MessageProvider";

    public static final UriMatcher uriMatcher;
    public static final String ALLOW_NOTIFY = "allow_notify";

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH, MessageContract.MESSAGES);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH + "/#", MessageContract.MESSAGES_INSERT);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH + "/u/#", MessageContract.MESSAGES_UPDATE);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH + "/conversations", MessageContract.MESSAGES_CONVERSATIONS);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH + "/messages_search", MessageContract.MESSAGES_SEARCH);
        uriMatcher.addURI(MessageContract.AUTHORITY, MessageContract.PROVIDER_PATH + "/load_message_delivered", MessageContract.LOAD_LAST_MESSAGE_DELIVERED);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        MessagesDatabaseHelper databaseHelper = new MessagesDatabaseHelper(getContext());
        database = databaseHelper.getWritableDatabase();

        return database != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String groupBy = null;
        String having = null;

        switch (uriMatcher.match(uri)) {

            case MessageContract.MESSAGES:

                if (sortOrder == null)
                    sortOrder = String.format("%s ASC", MessageContract.Column.DATE.name());

                break;

            case MessageContract.MESSAGES_CONVERSATIONS:

                return database.rawQuery(String.format("SELECT c.%s, c.%s, c.%s, c.%s, c.%s, m.%s, MAX(m.%s), m.%s, m.%s, m.%s, m.%s FROM %s m LEFT OUTER JOIN %s c ON m.%s=c.%s AND m.%s=c.%s GROUP BY m.%s ORDER BY m.%s DESC",
                        ContactContract.Column._ID.name(), ContactContract.Column.STATUS.name(), ContactContract.Column.LAST_MODIFIED.name(), ContactContract.Column.LATITUDE.name(),
                        ContactContract.Column.LONGITUDE.name(), MessageContract.Column._ID.name(), MessageContract.Column.DATE.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(),
                        MessageContract.Column.BODY.name(), MessageContract.Column.READ.name(), MessageContract.PROVIDER_PATH, ContactContract.PROVIDER_PATH, MessageContract.Column.DDI.name(), ContactContract.Column.DDI.name(),
                        MessageContract.Column.PHONE.name(), ContactContract.Column.PHONE_NUMBER.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.DATE.name()), null);

            case MessageContract.MESSAGES_SEARCH:

                return database.rawQuery(String.format("SELECT c.%s, m.%s, m.%s, m.%s, m.%s, m.%s, m.%s FROM %s m LEFT OUTER JOIN %s c ON m.%s=c.%s AND m.%s=c.%s WHERE %s",
                        ContactContract.Column._ID.name(), MessageContract.Column._ID.name(), MessageContract.Column.DATE.name(),
                        MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.BODY.name(), MessageContract.Column.READ.name(),
                        MessageContract.PROVIDER_PATH, ContactContract.PROVIDER_PATH, MessageContract.Column.DDI.name(), ContactContract.Column.DDI.name(),
                        MessageContract.Column.PHONE.name(), ContactContract.Column.PHONE_NUMBER.name(), selection), selectionArgs);

            case MessageContract.LOAD_LAST_MESSAGE_DELIVERED:

                return database.rawQuery(String.format("SELECT c.%s, c.%s, c.%s, c.%s, c.%s, m.%s, MAX(m.%s), m.%s, m.%s, m.%s, m.%s FROM %s m LEFT OUTER JOIN %s c ON m.%s=c.%s AND m.%s=c.%s WHERE " + selection,
                        ContactContract.Column._ID.name(), ContactContract.Column.STATUS.name(), ContactContract.Column.LAST_MODIFIED.name(), ContactContract.Column.LATITUDE.name(),
                        ContactContract.Column.LONGITUDE.name(), MessageContract.Column._ID.name(), MessageContract.Column.DATE.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(),
                        MessageContract.Column.BODY.name(), MessageContract.Column.READ.name(), MessageContract.PROVIDER_PATH, ContactContract.PROVIDER_PATH, MessageContract.Column.DDI.name(), ContactContract.Column.DDI.name(),
                        MessageContract.Column.PHONE.name(), ContactContract.Column.PHONE_NUMBER.name(), MessageContract.Column._ID, MessageContract.Column.DELIVERY.name()), selectionArgs);

            case MessageContract.MESSAGES_INSERT:

                if (selection == null || selection.trim().isEmpty())
                    selection = MessageContract.Column._ID + "=" + uri.getLastPathSegment();
                else
                    selection += " AND " + MessageContract.Column._ID + " = " + uri.getLastPathSegment();

                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor c = database.query(MessagesDatabaseHelper.TABLE, projection, selection, selectionArgs, groupBy, having, sortOrder);

        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)) {
            /**
             * Get all student records
             */
            case MessageContract.MESSAGES:
                return "vnd.android.cursor.dir/vnd.wheresapp3." + MessageContract.PROVIDER_PATH;
            /**
             * Get a particular student
             */
            case MessageContract.MESSAGES_INSERT:
                return "vnd.android.cursor.item/vnd.wheresapp3." + MessageContract.PROVIDER_PATH;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        long id = database.insert(MessagesDatabaseHelper.TABLE, null, contentValues);

        if (id > 0) {

            Uri _uri = makeInsertOrDeleteUri(id);
            notifyObservers(_uri);

            return _uri;
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] args) {

        int count;

        count = database.delete(MessagesDatabaseHelper.TABLE, s, args);

//        if (count > 0)
//            notifyObservers(makeInsertOrDeleteUri(Long.parseLong(args[0])));

        return count;

    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] args) {

        // get permission to notify observer
        boolean allowNotify = false;

        if (contentValues.containsKey(ALLOW_NOTIFY)) {

            allowNotify = contentValues.getAsBoolean(ALLOW_NOTIFY);

            // remove the allow notify at content values to update
            contentValues.remove(ALLOW_NOTIFY);
        }

        int rows = database.update(MessagesDatabaseHelper.TABLE, contentValues, selection, args);

        if (rows > 0 && allowNotify)
            notifyObservers(makeUpdateUri(Long.parseLong(args[0])));

        return rows;
    }

    public static Uri makeInsertOrDeleteUri(long id) {
        return ContentUris.withAppendedId(MessageContract.CONTENT_URI, id);
    }

    public static Uri makeUpdateUri(long id) {
        return Uri.parse(MessageContract.URI + "/u/" + id);
    }

    public static Uri makeConversationsUri() {
        return Uri.parse(MessageContract.URI + "/conversations");
    }

    public static Uri makeMessagesUri() {
        return Uri.parse(MessageContract.URI + "/messages_search");
    }

    public static Uri makeLastMessageDeliveredUri() {
        return Uri.parse(MessageContract.URI + "/load_message_delivered");
    }

    public void notifyObservers(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }

    public static class MessagesDatabaseHelper extends SQLiteOpenHelper {

        final static String TABLE = "messages";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE + " (" + MessageContract.Column._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MessageContract.Column.DDI + " VARCHAR(3), " + MessageContract.Column.PHONE + " VARCHAR(20), " + MessageContract.Column.DATE + " INTEGER, "
                + MessageContract.Column.DELIVERY + " INTEGER, " + MessageContract.Column.READ + " INTEGER, " + MessageContract.Column.BODY + " TEXT, "
                + MessageContract.Column.MODIFICATION + " INTEGER);";

        public MessagesDatabaseHelper(Context context) {
            super(context, Utils.isExternalStorageWritable() ? Environment.getExternalStorageDirectory() + File.separator + Storage.ROOT_DIR + File.separator +
                    App.DB_NAME : App.DB_NAME, null, App.DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

            // drop table if exists
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE);

            // recreate table
            onCreate(sqLiteDatabase);
        }
    }
}
