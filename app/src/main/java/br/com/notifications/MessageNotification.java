package br.com.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.br.wheresapp.R;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import br.com.activities.Chat;
import br.com.activities.Main;
import br.com.aplication.Application;
import br.com.aplication.Phones;
import br.com.dao.ContactDAO;
import br.com.dao.MessageDAO;
import br.com.dao.PhoneContactDAO;
import br.com.model.Transaction;
import br.com.model.domain.Contact;
import br.com.model.domain.Message;
import br.com.net.Http;
import br.com.service.ImageService;
import br.com.service.PhoneFormatTextWatcher;
import br.com.service.TaskLight;

/**
 * Created by MarioJ on 30/08/15.
 */
public class MessageNotification {

    public static final String TAG = "MessageNotification";

    public static final String PHONE = "phone";

    public static final int NOTIFICATION_ID = 1;

    public static void displayNotification(final Context context, final String ddi, final String phone) {

        TaskLight.start(new Transaction() {

            Application session;
            MessageDAO messageDAO;
            ContactDAO contactDAO;
            PhoneContactDAO phoneContactDAO;

            @Override
            public void init() {

                session = (Application) context;
                messageDAO = MessageDAO.instance(context);
                contactDAO = ContactDAO.instance(context);
                phoneContactDAO = PhoneContactDAO.instance(context);

            }

            @Override
            public Object perform() {

                List<Message> messages = messageDAO.getUnread(ddi, phone);

                if (messages == null) {
                    Log.d(TAG, "Nao ha mensagens nao lidas <return>");
                    return null;
                }

                // sort messages by date
                Collections.sort(messages, new Comparator<Message>() {

                    @Override
                    public int compare(Message lhs, Message rhs) {
                        return lhs.getDate().compareTo(rhs.getDate());
                    }
                });

                Contact contact = contactDAO.get(ddi, phone);

                // se o contato ja faz parte da agenda, busque o nome dele
                if (contact != null)
                    contact.setName(phoneContactDAO.getContactName(contact.getId()));
                else {

                    // instancia novo contato
                    contact = new Contact();
                    contact.setName(PhoneFormatTextWatcher.formatNumber(Phones.INTERNATIONAL_IDENTIFIER + ddi + phone, session.getCountries().get(ddi).get(0)));
                    contact.setDdi(ddi);
                    contact.setPhone(phone);
                    contact.setMessages(messages);

                    // tenta baixar a foto de perfil do numero
                    try {
                        contact.setPhoto(ImageService.imageToByte(Http.downloadPictureThumb(ddi, phone, "0")));
                    } catch (IOException e) {
                        Log.d(TAG, e.toString());
                    }
                }

                return contact;
            }

            @Override
            public void updateView(Object o) {

                Contact contact = (Contact) o;

                if (contact == null)
                    return;

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.brand_wheresapp_2)
                        .setContentTitle(contact.getName())
                        .setContentText(String.format("%d mensagens não lidas.", contact.getMessages().size()))
                        .setLargeIcon(contact.getPhoto() != null ? ImageService.byteToImage(contact.getPhoto()) : BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.ic_contact))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setVibrate(new long[]{0, 600})
                        .setLights(Color.CYAN, 1500, 2000)
                        .setAutoCancel(true);

                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.setBigContentTitle("Mensagens não lidas");

                // add unread messages
                for (Message m : contact.getMessages()) {
                    inboxStyle.addLine(m.getBody());
                }

                // add inbox to notification
                mBuilder.setStyle(inboxStyle);

                // creates a action when user click
                Intent intent = new Intent(context, Chat.class);
                intent.putExtra("contact", contact);
                intent.putExtra("is_read", false);
                intent.putExtra("notification", true);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(Main.class);

                stackBuilder.addNextIntent(intent);

                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pendingIntent);

                // Gets an instance of the NotificationManager service
                NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
                // Builds the notification and issues it.
                mNotifyMgr.notify(NOTIFICATION_ID, mBuilder.build());

            }
        });

    }

    public static void cancelNotification(Context context) {

        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(NOTIFICATION_ID);
    }
}
