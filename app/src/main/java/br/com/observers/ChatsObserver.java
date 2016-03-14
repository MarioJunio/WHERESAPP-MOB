package br.com.observers;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import br.com.adapter.ChatsAdapter;
import br.com.dao.MessageDAO;
import br.com.model.domain.Message;
import br.com.aplication.Application;
import br.com.smack.Smack;

/**
 * Created by MarioJ on 05/08/15.
 */
public class ChatsObserver extends ContentObserver {

    private final String TAG = "ChatsObserver";

    private Application session;
    private MessageDAO messageDAO;
    private ChatsAdapter chatsAdapter;

    public ChatsObserver(Application session, Handler handler, MessageDAO messageDAO, ChatsAdapter chatsAdapter) {
        super(handler);

        this.session = session;
        this.messageDAO = messageDAO;
        this.chatsAdapter = chatsAdapter;
    }

    @Override
    public void onChange(boolean selfChange) {
        forward(messageDAO.getLastMessageDelivered());
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        forward(messageDAO.getMessageDelivered(Long.parseLong(uri.getLastPathSegment())));
    }

    private void forward(Message message) {

        if (discard(message))
            return;

        chatsAdapter.newMessage(message);

        if (chatsAdapter.getItemCount() <= 0) {
            //TODO: Altera tela para mostrar todas as conversas
        }
    }

    private boolean discard(Message message) {
        return (message.getContact().getDdi() == null && message.getContact().getPhone() == null) || Smack.toSmackUser(message.getContact().getDdi(), message.getContact().getPhone()).equals(session.getXmppUser());
    }

}
