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
 * Created by MarioJ on 19/08/15.
 */
public class UserProvider extends ContentProvider {

    private final String TAG = "UserProvider";

    public static final UriMatcher uriMatcher;
    private SQLiteDatabase database;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(UserContract.AUTHORITY, UserContract.PROVIDER_PATH, UserContract.USER);
        uriMatcher.addURI(UserContract.AUTHORITY, UserContract.PROVIDER_PATH + "/#", UserContract.USER_ID);
    }

    @Override
    public boolean onCreate() {
        return (database = new UserDatabaseHelper(getContext()).getWritableDatabase()) != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return database.query(UserDatabaseHelper.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)) {
            /**
             * Get all student records
             */
            case UserContract.USER:
                return "vnd.android.cursor.dir/vnd.wheresapp3." + UserContract.PROVIDER_PATH;
            /**
             * Get a particular student
             */
            case UserContract.USER_ID:
                return "vnd.android.cursor.item/vnd.wheresapp3." + UserContract.PROVIDER_PATH;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return ContentUris.withAppendedId(UserContract.CONTENT_URI, database.insert(UserDatabaseHelper.TABLE, null, contentValues));
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return database.delete(UserDatabaseHelper.TABLE, selection, args);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] args) {
        return database.update(UserDatabaseHelper.TABLE, contentValues, selection, args);
    }

    static class UserDatabaseHelper extends SQLiteOpenHelper {

        final static String TABLE = UserContract.PROVIDER_PATH;

        public static final String CREATE_TABLE = String.format("CREATE TABLE %s (%s, %s, %s, %s, %s, %s, %s, %s)", TABLE, UserContract.Column._ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                UserContract.Column.DDI + " VARCHAR(3)", UserContract.Column.PHONE + " VARCHAR(20)", UserContract.Column.NAME + " VARCHAR (60)"
                , UserContract.Column.PHOTO_THUMB + " BLOB", UserContract.Column.STATUS + " VARCHAR(255)", UserContract.Column.STATE + " INTEGER", UserContract.Column.CONTACTS_CHECKSUM + " VARCHAR(40)");

        public UserDatabaseHelper(Context context) {
            super(context, Utils.isExternalStorageWritable() ? Environment.getExternalStorageDirectory() + File.separator + Storage.ROOT_DIR + File.separator +
                    App.DB_NAME : App.DB_NAME, null, App.DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // drop table if exists
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);

            // recreate table
            onCreate(db);
        }
    }
}
