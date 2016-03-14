package br.com.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import br.com.model.ChatSearchItem;
import br.com.model.domain.Contact;
import br.com.model.domain.Message;
import br.com.providers.MessageContract;
import br.com.providers.MessageProvider;
import br.com.service.DateService;

/**
 * Created by MarioJ on 25/07/15.
 */
public class MessageDAO extends DAO {

    private final String TAG = "MessageDAO";

    private final int MESSAGE_LOAD_SIZE = 100;

    public static MessageDAO instance(Context context) {

        MessageDAO messageDAO = new MessageDAO();
        messageDAO.setContext(context);
        messageDAO.setContentUri(MessageContract.CONTENT_URI);

        return messageDAO;
    }

    public long insert(Message message) {

        ContentValues values = new ContentValues();
        values.put(MessageContract.Column.DDI.name(), message.getDdi());
        values.put(MessageContract.Column.PHONE.name(), message.getPhone());
        values.put(MessageContract.Column.DATE.name(), message.getDate().getTime());
        values.put(MessageContract.Column.DELIVERY.name(), message.getDelivery().ordinal());
        values.put(MessageContract.Column.READ.name(), message.isRead());
        values.put(MessageContract.Column.BODY.name(), message.getBody());
        values.put(MessageContract.Column.MODIFICATION.name(), DateService.getTimeNow().getTime());

        Uri uri = getContext().getContentResolver().insert(getContentUri(), values);

        if (uri != null) {
            return Long.parseLong(uri.getLastPathSegment());
        }

        return 0l;
    }

    public void update(Message message) {

        ContentValues values = new ContentValues();
        values.put(MessageContract.Column.DELIVERY.name(), message.getDelivery().ordinal());
        values.put(MessageContract.Column.READ.name(), message.isRead());

        getContext().getContentResolver().update(getContentUri(), values, MessageContract.Column._ID.name() + "=?", new String[]{String.valueOf(message.getId())});

    }

    public Message getMessage(Integer id) {

        Message message = null;

        String projection[] = new String[]{MessageContract.Column._ID.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.DATE.name(), MessageContract.Column.DELIVERY.name(),
                MessageContract.Column.READ.name(), MessageContract.Column.BODY.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, MessageContract.Column._ID.name() + "=?", new String[]{String.valueOf(id)}, null);

        try {

            if (c.moveToFirst()) {

                message = new Message();
                message.setId(c.getLong(0));
                message.setDdi(c.getString(1));
                message.setPhone(c.getString(2));
                message.setDate(new Date(c.getLong(3)));
                message.setDelivery(Message.Delivery.parse(c.getInt(4)));
                message.setRead(parseBoolean(c.getInt(5)));
                message.setBody(c.getString(6));

            }

        } finally {
            c.close();
        }

        return message;

    }

    public Message getMessageDelivered(Long id) {

        Message message = null;
        Cursor cursor = null;

        try {

            cursor = getContext().getContentResolver().query(MessageProvider.makeLastMessageDeliveredUri(), null, "m." + MessageContract.Column._ID.name() + "=? AND "
                    + "m." + MessageContract.Column.DELIVERY.name() + "=?", new String[]{String.valueOf(id), String.valueOf(Message.Delivery.DELIVERED.ordinal())}, null);

            if (cursor.moveToFirst()) {

                Contact contact = new Contact();
                contact.setId(cursor.getInt(0));
                contact.setStatus(cursor.getString(1));
                contact.setLastModified(new Date(cursor.getLong(2)));
                contact.setLatitude(cursor.getDouble(3));
                contact.setLongitude(cursor.getDouble(4));
                contact.setDdi(cursor.getString(7));
                contact.setPhone(cursor.getString(8));

                message = new Message();
                message.setId(cursor.getLong(5));
                message.setDate(new Date(cursor.getLong(6)));
                message.setBody(cursor.getString(9));
                message.setRead(parseBoolean(cursor.getInt(10)));
                message.setContact(contact);

            }

        } finally {

            if (cursor != null)
                cursor.close();
        }


        return message;
    }

    public boolean messagesEmpty() {

        String projection[] = new String[]{MessageContract.Column._ID.name()};

        Cursor cursor = getContext().getContentResolver().query(getContentUri(), projection, null, null, null);

        try {
            return cursor.getCount() <= 0;
        } finally {

            if (cursor != null)
                cursor.close();
        }

    }

    public List<Message> getMessages(String ddi, String phone) {

        List<Message> messages = new ArrayList<>();

        String projection[] = new String[]{MessageContract.Column._ID.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(),
                MessageContract.Column.DATE.name(), MessageContract.Column.DELIVERY.name(), MessageContract.Column.READ.name(), MessageContract.Column.BODY.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, MessageContract.Column.DDI.name() + "=? AND " + MessageContract.Column.PHONE.name()
                + "=?", new String[]{ddi, phone}, MessageContract.Column.DATE.name() + " DESC LIMIT " + MESSAGE_LOAD_SIZE);

        try {

            while (c.moveToNext()) {

                Message message = new Message();
                message.setId(c.getLong(0));
                message.setDdi(c.getString(1));
                message.setPhone(c.getString(2));
                message.setDate(new Date(c.getLong(3)));
                message.setDelivery(Message.Delivery.parse(c.getInt(4)));
                message.setRead(parseBoolean(c.getInt(5)));
                message.setBody(c.getString(6));

                messages.add(message);
            }

        } finally {
            c.close();
        }

        // inverte a lista para exibir ao usuario na ordem correta
        Collections.reverse(messages);

        return messages;

    }

    public List<Message> getMessages(Message.Delivery delivery) {

        List<Message> messages = new ArrayList<>();
        String projection[] = new String[]{MessageContract.Column._ID.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.BODY.name()};

        Cursor c = getContext().getContentResolver().query(getContentUri(), projection, MessageContract.Column.DELIVERY.name() + "=?", new String[]{String.valueOf(delivery.ordinal())}
                , MessageContract.Column.DATE.name() + " ASC");

        try {

            while (c.moveToNext()) {

                Message message = new Message();
                message.setId(c.getLong(0));
                message.setDdi(c.getString(1));
                message.setPhone(c.getString(2));
                message.setBody(c.getString(3));

                messages.add(message);
            }

        } finally {

            if (c != null)
                c.close();
        }

        return messages;
    }

    public Message getLastMessage() {

        Message message = null;

        String projection[] = new String[]{MessageContract.Column._ID.name(), MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.DATE.name(), MessageContract.Column.DELIVERY.name(),
                MessageContract.Column.READ.name(), MessageContract.Column.BODY.name(), "MAX(" + MessageContract.Column.MODIFICATION + ")"};

        Cursor c = null;

        try {

            c = getContext().getContentResolver().query(getContentUri(), projection, null, null, null);

            if (c.moveToFirst()) {

                message = new Message();
                message.setId(c.getLong(0));
                message.setDdi(c.getString(1));
                message.setPhone(c.getString(2));
                message.setDate(new Date(c.getLong(3)));
                message.setDelivery(Message.Delivery.parse(c.getInt(4)));
                message.setRead(parseBoolean(c.getInt(5)));
                message.setBody(c.getString(6));
            }

        } finally {

            if (c != null)
                c.close();
        }

        return message;
    }

    public List<Message> getConversations() {

        List<Message> conversations = new ArrayList<>();

        Cursor c = null;

        try {

            c = getContext().getContentResolver().query(MessageProvider.makeConversationsUri(), null, null, null, null);

            while (c.moveToNext()) {

                Contact contact = new Contact();
                contact.setId(c.getInt(0));
                contact.setStatus(c.getString(1));
                contact.setLastModified(new Date(c.getLong(2)));
                contact.setLatitude(c.getDouble(3));
                contact.setLongitude(c.getDouble(4));
                contact.setDdi(c.getString(7));
                contact.setPhone(c.getString(8));

                Message m = new Message();
                m.setId(c.getLong(5));
                m.setDate(new Date(c.getLong(6)));
                m.setBody(c.getString(9));
                m.setRead(parseBoolean(c.getInt(10)));
                m.setContact(contact);

                conversations.add(m);
            }

        } finally {

            if (c != null)
                c.close();
        }

        return conversations;
    }

    public Message getLastMessageDelivered() {

        Message message = null;
        Cursor c = null;

        try {

            c = getContext().getContentResolver().query(MessageProvider.makeLastMessageDeliveredUri(), null, MessageContract.Column.DELIVERY.name() + "=?", new String[]{String.valueOf(Message.Delivery.DELIVERED.ordinal())}, null);

            if (c.moveToFirst()) {

                Contact contact = new Contact();
                contact.setId(c.getInt(0));
                contact.setStatus(c.getString(1));
                contact.setLastModified(new Date(c.getLong(2)));
                contact.setLatitude(c.getDouble(3));
                contact.setLongitude(c.getDouble(4));
                contact.setDdi(c.getString(7));
                contact.setPhone(c.getString(8));

                message = new Message();
                message.setId(c.getLong(5));
                message.setDate(new Date(c.getLong(6)));
                message.setBody(c.getString(9));
                message.setRead(parseBoolean(c.getInt(10)));
                message.setContact(contact);
            }

        } finally {

            if (c != null)
                c.close();
        }

        return message;
    }

    private boolean parseBoolean(int i) {
        return i == 1 ? true : false;
    }

    public void read(String ddi, String phone) {

        ContentValues values = new ContentValues();
        values.put(MessageContract.Column.READ.name(), true);
        values.put(MessageProvider.ALLOW_NOTIFY, false);

        String where = String.format("%s=? AND %s=?", MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name());
        String args[] = new String[]{ddi, phone};

        getContext().getContentResolver().update(getContentUri(), values, where, args);
    }

    public void read(Long id) {

        ContentValues values = new ContentValues();
        values.put(MessageContract.Column.READ.name(), true);
        values.put(MessageProvider.ALLOW_NOTIFY, false);

        String where = String.format("%s=?", MessageContract.Column._ID.name());
        String args[] = new String[]{String.valueOf(id)};

        getContext().getContentResolver().update(getContentUri(), values, where, args);
    }

    public int unread(Long id) {

        ContentValues values = new ContentValues();
        values.put(MessageContract.Column.READ.name(), false);
        values.put(MessageProvider.ALLOW_NOTIFY, false);

        String where = String.format("%s=?", MessageContract.Column._ID.name());
        String args[] = new String[]{String.valueOf(id)};

        return getContext().getContentResolver().update(getContentUri(), values, where, args);
    }

    public List<Message> getUnread(String ddi, String phone) {

        String[] projection = new String[]{
                MessageContract.Column.BODY.name(),
                MessageContract.Column.DATE.name()
        };

        String where = String.format("%s=? AND %s=? AND %s=?", MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name(), MessageContract.Column.READ.name());
        String[] args = new String[]{ddi, phone, String.valueOf(0)};

        Cursor c = null;

        try {

            c = getContext().getContentResolver().query(getContentUri(), projection, where, args, null);

            if (c.moveToFirst()) {

                List<Message> messages = new ArrayList<>();

                do
                    messages.add(Message.create(c.getString(0), new Date(c.getLong(1))));
                while (c.moveToNext());

                return messages;
            }

        } finally {

            if (c != null)
                c.close();
        }

        return null;
    }

    public List<ChatSearchItem> getMessages(String query) {

        List<ChatSearchItem> searchItems = new ArrayList<>();
        Cursor cursor = getContext().getContentResolver().query(MessageProvider.makeMessagesUri(), null, String.format("%s LIKE ?", MessageContract.Column.BODY.name()), new String[]{"%" + query + "%"}, null);

        try {

            if (cursor.moveToFirst()) {

                do {

                    ChatSearchItem chatSearchItem = new ChatSearchItem();

                    Message message = new Message();
                    message.setId(cursor.getLong(1));
                    message.setDate(new Date(cursor.getLong(2)));
                    message.setBody(cursor.getString(5));
                    message.setRead(parseBoolean(cursor.getInt(6)));

                    Contact contact = new Contact();
                    contact.setId(cursor.getInt(0));
                    contact.setDdi(cursor.getString(3));
                    contact.setPhone(cursor.getString(4));

                    message.setContact(contact);

                    chatSearchItem.setMessage(message);

                    searchItems.add(chatSearchItem);

                } while (cursor.moveToNext());

            }

        } finally {
            closeCursor(cursor);
        }

        return searchItems;
    }

    public int delete(String ddi, String phone) {

        String where = String.format("%s=? AND %s=?", MessageContract.Column.DDI.name(), MessageContract.Column.PHONE.name());
        String args[] = new String[]{ddi, phone};

        return getContext().getContentResolver().delete(MessageProvider.makeMessagesUri(), where, args);
    }

    private void closeCursor(Cursor c) {

        if (c != null)
            c.close();
    }
}
