package br.com.model;

import br.com.model.domain.Contact;
import br.com.model.domain.Message;

/**
 * Created by MarioJ on 13/10/15.
 */
public class ChatSearchItem {

    /**
     * Contact properties (name, statusMessage, statusType)
     */
    private Contact contact;

    /**
     * Message properties (Name, Message, Date)
     */
    private Message message;

    /**
     * Type
     */
    private boolean type;

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public boolean isType() {
        return type;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ChatSearchItem{" +
                "contact=" + contact +
                ", message=" + message +
                ", type=" + type +
                '}';
    }
}
