package br.com.model.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by MarioJ on 24/07/15.
 */
public class Message implements Serializable {

    private long id;
    private Contact contact;
    private String ddi;
    private String phone;
    private Date date;
    private Delivery delivery;
    private boolean read;
    private String body;
    private Date modification;

    public enum Delivery {

        PENDING, SUBMITED, RECEIVED, SAW, DELIVERED;

        public static Delivery parse(int ordinal) {

            if (ordinal == PENDING.ordinal())
                return PENDING;
            else if (ordinal == SUBMITED.ordinal())
                return SUBMITED;
            else if (ordinal == RECEIVED.ordinal())
                return RECEIVED;
            else if (ordinal == SAW.ordinal())
                return SAW;
            else if (ordinal == DELIVERED.ordinal())
                return DELIVERED;

            return null;
        }
    }

    public Message() {
    }

    public static Message create(String ddi, String phone, Date timeNow, Delivery delivery, boolean read, String body) {

        Message m = new Message();
        m.setDdi(ddi);
        m.setPhone(phone);
        m.setDate(timeNow);
        m.setDelivery(delivery);
        m.setRead(read);
        m.setBody(body);

        return m;
    }

    public static Message create(Contact contact, Date date, String place, String body) {

        Message message = new Message();
        message.setContact(contact);
        message.setDate(date);
        message.setPhone(place);
        message.setBody(body);

        return message;
    }

    public static Message create(String body, Date date) {

        Message message = new Message();
        message.setBody(body);
        message.setDate(date);

        return message;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getModification() {
        return modification;
    }

    public void setModification(Date modification) {
        this.modification = modification;
    }

    public String getDdi() {
        return ddi;
    }

    public void setDdi(String ddi) {
        this.ddi = ddi;
    }

    @Override
    public String toString() {
        return "Message{" +
                "ddi='" + ddi + '\'' +
                ", phone='" + phone + '\'' +
                ", date=" + date +
                ", delivery=" + delivery +
                ", read=" + read +
                ", body='" + body + '\'' +
                ", modification=" + modification +
                '}';
    }
}
