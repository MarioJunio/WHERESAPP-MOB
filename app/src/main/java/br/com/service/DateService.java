package br.com.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by MarioJ on 19/03/15.
 */
public class DateService {

    private enum Dates {
        TODAY, YESTERDAY, ANOTHER_DAY
    }

    private static SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
    private static SimpleDateFormat formatHour = new SimpleDateFormat("HH:mm");

    public static Dates getDateLiteral(Calendar calendar) {

        Calendar now = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_YEAR);

        int yearNow = now.get(Calendar.YEAR);
        int dayNow = now.get(Calendar.DAY_OF_YEAR);


        if (yearNow == year) {

            if (dayNow == day)
                return Dates.TODAY;
            else if ((dayNow - day) == 1)
                return Dates.YESTERDAY;
            else
                return Dates.ANOTHER_DAY;

        } else if ((yearNow - year) == 1) {

            boolean isLeap = new GregorianCalendar().isLeapYear(year);

            if (!isLeap && dayNow == 1 && (day == 365)) {
                return Dates.YESTERDAY;
            } else if (isLeap && dayNow == 1 && day == 366)
                return Dates.YESTERDAY;
            else
                return Dates.ANOTHER_DAY;

        } else
            return Dates.ANOTHER_DAY;

    }

    public static Dates getDateLiteral(Calendar start, Calendar end) {

        int year = end.get(Calendar.YEAR);
        int day = end.get(Calendar.DAY_OF_YEAR);

        int yearNow = start.get(Calendar.YEAR);
        int dayNow = start.get(Calendar.DAY_OF_YEAR);


        if (yearNow == year) {

            if (dayNow == day)
                return Dates.TODAY;
            else if ((dayNow - day) == 1)
                return Dates.YESTERDAY;
            else
                return Dates.ANOTHER_DAY;

        } else if ((yearNow - year) == 1) {

            boolean isLeap = new GregorianCalendar().isLeapYear(year);

            if (!isLeap && dayNow == 1 && (day == 365)) {
                return Dates.YESTERDAY;
            } else if (isLeap && dayNow == 1 && day == 366)
                return Dates.YESTERDAY;
            else
                return Dates.ANOTHER_DAY;

        } else
            return Dates.ANOTHER_DAY;

    }

    public static String formatMessage(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Dates literal = getDateLiteral(calendar);

        if (literal == Dates.TODAY)
            return formatHour.format(date);
        else if (literal == Dates.YESTERDAY)
            return "ONTEM";
        else
            return formatDate.format(date);

    }

    public static String formatLastActiviy(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Dates literal = getDateLiteral(calendar);

        if (literal == Dates.TODAY)
            return "Hoje as " + formatHour.format(date);
        else if (literal == Dates.YESTERDAY)
            return "Ontem as " + formatHour.format(date);
        else
            return "Visto " + formatDate.format(date) + " as " + formatHour.format(date);

    }

    public static String getTime(Date date) {
        return formatHour.format(date);
    }

    public static Date getTimeNow() {
        return Calendar.getInstance().getTime();
    }

}
