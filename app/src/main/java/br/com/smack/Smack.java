package br.com.smack;

import br.com.net.Xmpp;

/**
 * Created by MarioJ on 14/08/15.
 */
public abstract class Smack {

    public static final String DOMAIN_SEPARATOR = "@";
    public static final String WHERESAPP_USER_SEPARATOR = "-";

    public static String parseContact(String ddi, String phone) {
        return String.format("%s%s%s%s", ddi, WHERESAPP_USER_SEPARATOR, phone, Xmpp.DOMAIN);
    }

    public static String parseSmackUser(String user) {
        return user.substring(0, user.indexOf(DOMAIN_SEPARATOR));
    }

    public static String toSmackUser(String ddi, String phone) {
        return String.format("%s%s%s", ddi, WHERESAPP_USER_SEPARATOR, phone);
    }

    public static String[] split(String user) {
        return parseSmackUser(user).split(WHERESAPP_USER_SEPARATOR);
    }

    public static String formatOnlyNumber(String from) {
        return from.replaceAll("[^0-9]", "");
    }
}
