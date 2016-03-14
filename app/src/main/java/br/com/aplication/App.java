package br.com.aplication;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;

import br.com.util.AndroidUtilities;

/**
 * Created by MarioJ on 23/07/15.
 */
public class App {

    private static final String TAG = "App";

    // delay to wait to create xmpp service
    public static final int SLEEP_TIME = 1000;

    // delay to connect on server
    public static final int HTTP_TIMEOUT = 5000;
    public static final int JABBER_TIMEOUT = 10000;

    public static final String DB_NAME = "wheresapp.db";
    public static final int DB_VERSION = 1;

    // Emoji Font Metrics Size
    public static final int Emoji_FONT_METRICS_SIZE = AndroidUtilities.dp(17);

    public enum Colors {

        APP_COLOR("#5e7703"), APP_LIGHT_COLOR("#85a904"), GRAY("#757575"), BLACK("#313030"), LIGHT_GREEN("#8BC34A"), LIGHT_RED("#F44336");

        private String code;

        Colors(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static Thread runBackgroundService(Runnable runnable) {

        Thread t = new Thread(runnable);
        t.start();

        return t;
    }

    public static float getDeviceWidthDIP(Activity activity) {

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = activity.getResources().getDisplayMetrics().density;
        return outMetrics.widthPixels / density;
    }

    public static float getDeviceHeightDIP(Activity activity) {

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = activity.getResources().getDisplayMetrics().density;
        return outMetrics.heightPixels / density;
    }

    /**
     * Get the system status bar height
     *
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        //TODO: rever
        int result = 0;

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0)
            result = context.getResources().getDimensionPixelSize(resourceId);

        return 200;
    }

    public static int getAPILevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

}
