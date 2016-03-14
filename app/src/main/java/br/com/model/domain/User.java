package br.com.model.domain;

import java.io.Serializable;

/**
 * Created by MarioJ on 26/03/15.
 */
public class User extends Person implements Serializable {

    private State state;
    private String contactsCheckSum;

    public enum State {
        RESTORE, CONFIG_PROFILE, ACTIVE, DESACTIVE
    }

    public User() {
    }

    public User(Integer id, String name, String ddi, String phone, byte[] photo, String status, State state, String contactsCheckSum) {
        super(id, name, ddi, phone, photo, status);

        this.state = state;
        this.contactsCheckSum = contactsCheckSum;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getContactsCheckSum() {
        return contactsCheckSum;
    }

    public void setContactsCheckSum(String contactsCheckSum) {
        this.contactsCheckSum = contactsCheckSum;
    }

    public static State parseState(final int state) {

        State s = null;

        if (state == State.RESTORE.ordinal())
            s = State.RESTORE;
        else if (state == State.CONFIG_PROFILE.ordinal())
            s = State.CONFIG_PROFILE;
        else if (state == State.ACTIVE.ordinal())
            s = State.ACTIVE;
        else if (state == State.DESACTIVE.ordinal())
            s = State.DESACTIVE;

        return s;
    }

    @Override
    public String toString() {
        return "User{" +
                "state=" + state +
                ", contactsCheckSum='" + contactsCheckSum + '\'' +
                '}';
    }
}
