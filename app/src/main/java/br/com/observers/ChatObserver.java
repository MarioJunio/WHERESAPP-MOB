package br.com.observers;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import br.com.adapter.ChatAdapter;
import br.com.dao.MessageDAO;
import br.com.model.domain.Contact;
import br.com.model.domain.Message;
import br.com.providers.MessageContract;
import br.com.providers.MessageProvider;

/**
 * Created by MarioJ on 26/07/15.
 */
public class ChatObserver extends ContentObserver {

    private final String TAG = "ChatObserver";

    private Contact currentChatContact;
    private RecyclerView messages;
    private ChatAdapter adapter;
    private MessageDAO messageDAO;

    public ChatObserver(Contact currentChatContact, MessageDAO messageDAO, Handler handler, RecyclerView messages, ChatAdapter adapter) {
        super(handler);

        this.currentChatContact = currentChatContact;
        this.messages = messages;
        this.adapter = adapter;
        this.messageDAO = messageDAO;

    }

    @Override
    public void onChange(boolean selfChange) {

        Message m = messageDAO.getLastMessage();

        if (m.getDelivery() == Message.Delivery.DELIVERED)
            process(m);
        else if (m.getDelivery().compareTo(Message.Delivery.SUBMITED) == 0 || m.getDelivery().compareTo(Message.Delivery.RECEIVED) == 0
                || m.getDelivery().compareTo(Message.Delivery.SAW) == 0)
            update(MessageProvider.makeUpdateUri(m.getId()));

    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {

        switch (MessageProvider.uriMatcher.match(uri)) {

            case MessageContract.MESSAGES_INSERT:
                process(messageDAO.getMessage(Integer.parseInt(uri.getLastPathSegment())));
                break;
            case MessageContract.MESSAGES_UPDATE:
                update(uri);

        }

    }

    private void process(final Message message) {

        if (message.getDelivery() == Message.Delivery.DELIVERED && (message.getDdi() + message.getPhone()).equals(currentChatContact.getDdi() + currentChatContact.getPhone())) {

            // adiciona mensagem lista ao adapter
            adapter.add(message);

            // move para a ultima mensagem entregue
            messages.scrollToPosition(adapter.getItemCount() - 1);

            messageDAO.read(message.getId());
        }
    }

    private void update(Uri uri) {
        Log.d(TAG, "[] " + uri.getLastPathSegment());
    }
}
