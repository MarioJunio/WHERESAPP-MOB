package br.com.listener;

import android.os.Handler;
import android.util.Log;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

import br.com.adapter.ContactGeoMapAdapter;
import br.com.dao.ContactDAO;
import br.com.model.domain.Contact;
import br.com.service.LocationService;
import br.com.smack.Smack;

/**
 * Created by MarioJ on 29/09/15.
 */
public class LocationMessageListener implements ChatMessageListener {

    private static final String TAG = "LocationMessageListener";

    public static final String COORDINATE_ID = "coord";

    private ContactGeoMapAdapter contactGeoMapAdapter;
    private Handler handler;
    private ContactDAO contactDAO;

    public LocationMessageListener(ContactGeoMapAdapter contactGeoMapAdapter, Handler handler, ContactDAO contactDAO) {
        this.contactGeoMapAdapter = contactGeoMapAdapter;
        this.handler = handler;
        this.contactDAO = contactDAO;
    }

    @Override
    public void processMessage(Chat chat, final Message message) {
        handle(message);
    }

    private void handle(Message message) {

        Log.d(TAG, "ID -> " + message.getStanzaId());

        // check whether message contains location attributes
        if (message.getStanzaId().equals(COORDINATE_ID)) {

            Log.d(TAG, "Message location arrive\tfrom -> " + Smack.parseSmackUser(message.getFrom()) + " \tBody -> " + message.getBody());

            String[] tokens = Smack.split(message.getFrom());
            String[] coordTokens = LocationService.split(message.getBody());

            if (contactGeoMapAdapter == null) {
                Log.d(TAG, "GeoMapAdapter is null");
                return;
            }

            if (!contactGeoMapAdapter.userExists(tokens[0], tokens[1])) {

                Contact contact = new Contact(tokens[0], tokens[1], Double.valueOf(coordTokens[0]), Double.valueOf(coordTokens[1]));
                contact.setPhoto(contactDAO.getImage(tokens[0], tokens[1]));

                contactGeoMapAdapter.add(contact);

            } else
                contactGeoMapAdapter.updateLocation(Smack.toSmackUser(tokens[0], tokens[1]), Double.valueOf(coordTokens[0]), Double.valueOf(coordTokens[1]));


            // notify adapter
            handler.post(new Runnable() {

                @Override
                public void run() {
                    contactGeoMapAdapter.notifyDataSetChanged();
                }
            });

        } else {
            Log.d(TAG, "Message is not for location");
        }
    }
}
