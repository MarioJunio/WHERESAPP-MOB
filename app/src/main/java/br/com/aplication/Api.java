package br.com.aplication;

/**
 * Created by MarioJ on 23/04/15.
 */
public enum Api {

    CONTACT_API("api/contact"), DEV_API("api/dev");

    private String name;

    private Api(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
