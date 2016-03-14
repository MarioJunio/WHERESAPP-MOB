package br.com.model;

/**
 * Created by MarioJ on 11/04/15.
 */
public class Confirmation {

    public static String OK = "ok";
    public static String NEW = "new";

    private boolean ok;
    private boolean isNew;

    public Confirmation(boolean ok, boolean isNew) {
        this.ok = ok;
        this.isNew = isNew;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }
}
