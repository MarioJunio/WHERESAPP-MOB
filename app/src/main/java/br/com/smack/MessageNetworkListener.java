package br.com.smack;

import android.content.Context;
import android.util.Log;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

import br.com.dao.ContactDAO;
import br.com.dao.MessageDAO;
import br.com.listener.LocationMessageListener;
import br.com.notifications.MessageNotification;
import br.com.service.DateService;
import br.com.service.LocationService;
import br.com.aplication.Application;
import br.com.aplication.App;

/**
 * Created by MarioJ on 13/08/15.
 */
public class MessageNetworkListener implements ChatMessageListener {

    private final String TAG = "MessageNetworkListener";

    private Context context;
    private MessageDAO messageDAO;
    private ContactDAO contactDAO;

    public MessageNetworkListener(Context context) {
        this.context = context;
        this.messageDAO = MessageDAO.instance(context);
        this.contactDAO = ContactDAO.instance(context);
    }

    @Override
    public void processMessage(Chat chat, final Message message) {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                Application session = (Application) context;

                String tokens[] = Smack.parseSmackUser(message.getFrom()).split(Smack.WHERESAPP_USER_SEPARATOR);
                String ddi = tokens[0];
                String phone = tokens[1];

                if (!message.getStanzaId().equals(LocationMessageListener.COORDINATE_ID)) {

                    if (message.getBody() == null || message.getBody().isEmpty()) {
                        Log.d(TAG, "Mensagem sem corpo de " + message.getFrom());
                        return;
                    }

                    br.com.model.domain.Message m = new br.com.model.domain.Message();
                    m.setDdi(ddi);
                    m.setPhone(phone);
                    m.setDate(DateService.getTimeNow());
                    m.setDelivery(br.com.model.domain.Message.Delivery.DELIVERED);
                    m.setModification(DateService.getTimeNow());
                    m.setBody(message.getBody());
                    m.setRead(false);

                    messageDAO.insert(m);

                    Log.d(TAG, "ChatContact is null ? " + session.getChatContact() + "");
                    Log.d(TAG, "Message -> " + m.toString());

                    if (session.getChatContact() == null || !(session.getChatContact().getDdi() + session.getChatContact().getPhone()).equals(m.getDdi() + m.getPhone()))
                        MessageNotification.displayNotification(context, m.getDdi(), m.getPhone());

                } else {

                    // split location message
                    String[] locationTokens = message.getBody().split(LocationService.COORD_SEPARATOR);

                    // update location contact
                    contactDAO.updateLocation(ddi, phone, Double.valueOf(locationTokens[0]), Double.valueOf(locationTokens[1]));
                }

            }

        });

    }
}
