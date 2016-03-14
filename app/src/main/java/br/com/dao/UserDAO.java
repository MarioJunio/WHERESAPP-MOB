package br.com.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import br.com.model.domain.User;
import br.com.model.domain.User.State;
import br.com.providers.UserContract;

/**
 * Created by MarioJ on 26/03/15.
 */
public class UserDAO extends DAO {

    private final String TAG = "UserDAO";

    public static UserDAO getInstance(Context context) {

        UserDAO userDAO = new UserDAO();
        userDAO.setContext(context);
        userDAO.setContentUri(UserContract.CONTENT_URI);

        return userDAO;
    }

    public User save(User user) {

        ContentValues values = new ContentValues();
        values.put(UserContract.Column.DDI.name(), user.getDdi());
        values.put(UserContract.Column.PHONE.name(), user.getPhone());
        values.put(UserContract.Column.STATE.name(), user.getState().ordinal());

        // save user and get Uri record
        final Uri uri = getContext().getContentResolver().insert(UserContract.CONTENT_URI, values);

        // get saved user using Uri id
        return getUser(Long.valueOf(uri.getLastPathSegment()));
    }

    public User getUser(Long id) {

        User user = null;
        Cursor cursor = null;

        try {

            String projection[] = {UserContract.Column._ID.name(), UserContract.Column.NAME.name(), UserContract.Column.DDI.name(), UserContract.Column.PHONE.name(),
                    UserContract.Column.PHOTO_THUMB.name(), UserContract.Column.STATUS.name(), UserContract.Column.STATE.name(), UserContract.Column.CONTACTS_CHECKSUM.name()};

            String selection = UserContract.Column._ID.name() + "=?";
            String selectionArgs[] = new String[]{String.valueOf(id)};

            cursor = getContext().getContentResolver().query(UserContract.CONTENT_URI, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst())
                user = new User(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), User.parseState(cursor.getInt(6)), cursor.getString(7));

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return user;

    }

    public User getUser(String ddi, String phone) {

        User user = null;
        Cursor cursor = null;

        try {

            String projection[] = {UserContract.Column._ID.name(), UserContract.Column.NAME.name(), UserContract.Column.DDI.name(), UserContract.Column.PHONE.name(),
                    UserContract.Column.PHOTO_THUMB.name(), UserContract.Column.STATUS.name(), UserContract.Column.STATE.name(), UserContract.Column.CONTACTS_CHECKSUM.name()};

            String selection = UserContract.Column.DDI.name() + "=? AND " + UserContract.Column.PHONE.name() + "=?";
            String selectionArgs[] = new String[]{ddi, phone};

            cursor = getContext().getContentResolver().query(UserContract.CONTENT_URI, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst())
                user = new User(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), User.parseState(cursor.getInt(6)), cursor.getString(7));

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return user;
    }

    public String getChecksum(int id) {

        String checksum = null;
        Cursor cursor = null;

        try {

            String projection[] = {UserContract.Column.CONTACTS_CHECKSUM.name()};

            String selection = UserContract.Column._ID.name() + "=?";
            String selectionArgs[] = new String[]{String.valueOf(id)};

            cursor = getContext().getContentResolver().query(UserContract.CONTENT_URI, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst())
                checksum = cursor.getString(0);

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return checksum;

    }

    public void updatePhoto(User user) {

        ContentValues values = new ContentValues();
        values.put(UserContract.Column.PHOTO_THUMB.name(), user.getPhoto());

        String selection = UserContract.Column.DDI.name() + "=? AND " + UserContract.Column.PHONE.name() + "=?";
        String selectionArgs[] = new String[]{user.getDdi(), user.getPhone()};

        getContext().getContentResolver().update(UserContract.CONTENT_URI, values, selection, selectionArgs);
    }


    public void updateContactsCheckSum(User user) {

        ContentValues values = new ContentValues();
        values.put(UserContract.Column.CONTACTS_CHECKSUM.name(), user.getContactsCheckSum());

        String selection = UserContract.Column._ID.name() + "=?";
        String selectionArgs[] = new String[]{String.valueOf(user.getId())};

        getContext().getContentResolver().update(UserContract.CONTENT_URI, values, selection, selectionArgs);

    }

    public void update(User user) {

        ContentValues values = new ContentValues();
        values.put(UserContract.Column.DDI.name(), user.getDdi());
        values.put(UserContract.Column.PHONE.name(), user.getPhone());
        values.put(UserContract.Column.NAME.name(), user.getName());
        values.put(UserContract.Column.STATUS.name(), user.getStatus());

        if (user.getState() != null)
            values.put(UserContract.Column.STATE.name(), user.getState().ordinal());

        if (user.getContactsCheckSum() != null)
            values.put(UserContract.Column.CONTACTS_CHECKSUM.name(), user.getContactsCheckSum());

        String selection = String.format("%s=%s AND %s=%s", UserContract.Column.DDI.name(), user.getDdi(), UserContract.Column.PHONE.name(), user.getPhone());
        String selectionArgs[] = new String[]{};

        getContext().getContentResolver().update(UserContract.CONTENT_URI, values, selection, selectionArgs);
    }

    public User getLocalUser() {

        User user = null;

        String projection[] = {UserContract.Column._ID.name(), UserContract.Column.NAME.name(), UserContract.Column.DDI.name(), UserContract.Column.PHONE.name(),
                UserContract.Column.PHOTO_THUMB.name(), UserContract.Column.STATUS.name(), UserContract.Column.STATE.name(), UserContract.Column.CONTACTS_CHECKSUM.name()};

        String selection = String.format("%s<>?", UserContract.Column.STATE.name());
        String selectionArgs[] = new String[]{String.valueOf(State.DESACTIVE.ordinal())};

        Cursor cursor = null;

        try {

            cursor = getContext().getContentResolver().query(UserContract.CONTENT_URI, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst())
                user = new User(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), User.parseState(cursor.getInt(6)), cursor.getString(7));

        } finally {

            if (cursor != null)
                cursor.close();

        }

        return user;
    }


}
