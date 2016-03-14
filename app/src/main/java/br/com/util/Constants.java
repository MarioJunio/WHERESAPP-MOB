package br.com.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by madhur on 3/1/15.
 */
public class Constants {

    public static final String TAG = "chatbubbles";

    public static final List<String> defaultStatus = new ArrayList<>();

    static {
        defaultStatus.add("Olá compartilhe o WheresApp e ganhe a versão premium !!!");
    }

    public static String WheresAppStandardStatus() {
        return defaultStatus.get(0);
    }
}

