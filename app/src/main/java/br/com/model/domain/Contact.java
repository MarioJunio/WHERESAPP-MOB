package br.com.model.domain;

import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by MarioJ on 04/03/15.
 */
public class Contact extends Person implements Serializable {

    public static final String TAG = "contact";

    public static final String ID = "id";
    public static final String LAST_MODIFIED = "last_modified";
    public static final String CONFIRM_CODE = "confirm_code";
    public static final String LAST_SEE = "last_see";
    public static final String ONLINE = "online";

    public static final String CONTACTS_LIST = "contacts";

    private Date lastModified;
    private Date lastSee;
    private Date modification;
    private List<Message> messages;

    // transient
    private String presence;
    private Marker marker;

    public Contact(int id) {
        this.setId(id);
    }

    public static Contact create(int id, byte[] photoTn) {

        Contact contact = new Contact();
        contact.setId(id);
        contact.setPhoto(photoTn);

        return contact;
    }

    public enum StatusType {
        OFFLINE, ONLINE, NETWORK_UNAVAILABLE
    }

    public Contact() {
    }

    public Contact(String ddi, String phone) {
        setDdi(ddi);
        setPhone(phone);
    }

    public Contact(String ddi, String phone, double lat, double lon) {
        setDdi(ddi);
        setPhone(phone);
        setLatitude(lat);
        setLongitude(lon);
    }

    public Contact(Integer id, String name, String cn, String phone, byte[] photoURL, String status, Date lastSee, Date lastModified) {
        super(id, name, cn, phone, photoURL, status);

        setLastSee(lastSee);
        setLastModified(lastModified);
    }

    public Contact(Integer id, String ddi, String phone) {
        setId(id);
        setDdi(ddi);
        setPhone(phone);
    }

    public void handlerMarker(Marker marker) {
        this.marker = marker;
    }

    public Date getLastSee() {
        return lastSee;
    }

    public void setLastSee(Date lastSee) {
        this.lastSee = lastSee;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getModification() {
        return modification;
    }

    public void setModification(Date modification) {
        this.modification = modification;
    }

    public String getPresence() {
        return presence;
    }

    public void setPresence(String presence) {
        this.presence = presence;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public boolean equals(Object o) {
        return getId() == ((Contact) o).getId();
    }

    public static Contact create(int id, String ddi, String phone, String status, Date lastModified, double latitude, double longitude) {

        Contact c = new Contact();
        c.setId(id);
        c.setDdi(ddi);
        c.setPhone(phone);
        c.setStatus(status);
        c.setLastModified(lastModified);
        c.setLatitude(latitude);
        c.setLongitude(longitude);

        return c;
    }

    public Marker getMarker() {
        return marker;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
