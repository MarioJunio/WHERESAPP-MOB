package br.com.aplication;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

import com.br.wheresapp.R;
import br.com.model.domain.Contact;
import br.com.service.PhoneFormatTextWatcher;
import br.com.util.Utils;

/**
 * Created by MarioJ on 08/05/15.
 */
public class Phones {

    public static final String INTERNATIONAL_IDENTIFIER = "+";
    private static final String TAG = "Phones";

    public static String formatNumber(String phone) {

        phone = phone.replaceAll("[^\\d" + INTERNATIONAL_IDENTIFIER + "]", "").trim();

        while (phone.startsWith("0")) {
            phone = phone.replaceFirst("0", "");
        }

        return phone;
    }

    public static String extractDDD(String phoneNumber, String countryISO) {

        String phone;
        int index;

        if (countryISO.equals("BR")) {
            phone = phoneNumber.replaceAll("[^\\d]", "");
            index = 2;
        } else {
            phone = PhoneFormatTextWatcher.formatNumber(phoneNumber, countryISO).replaceAll("[^\\d\\s]", "");
            index = phone.indexOf(" ");
        }

        if (index > 0)
            return phone.substring(0, index);
        else
            return "";

    }

    public static String parseNumber(String ddi, String ddd, String number) {

        number = formatNumber(number);

        // if not starts with '+', check whether starts with DDD or DDI local
        if (!number.startsWith(INTERNATIONAL_IDENTIFIER) && !number.startsWith(ddi + ddd)) {

            String tmpNumber = INTERNATIONAL_IDENTIFIER;

            if (number.startsWith(ddd) || ddd.isEmpty())
                tmpNumber += ddi + number;
            else
                tmpNumber += ddi + ddd + number;

            return tmpNumber.substring(INTERNATIONAL_IDENTIFIER.length());
        }

        return number.replace(INTERNATIONAL_IDENTIFIER, "");

    }

    public static String getPhoneCountryISO(Context context) {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimCountryIso();
    }

    public static String getCountryCode(Context context, String countryISO) {

        String[] countries = context.getResources().getStringArray(R.array.countries);

        for (String country : countries) {

            String[] tokens = country.split(",");

            if (countryISO.equalsIgnoreCase(tokens[1]))
                return tokens[0];
        }

        return null;
    }

    public static String getCountryISO(Context context, String countryCode) {

        String[] countries = context.getResources().getStringArray(R.array.countries);

        for (String country : countries) {

            String tokens[] = country.split(",");

            if (countryCode.equals(tokens[0]))
                return tokens[1];

        }

        return null;

    }

    public static String getPhoneNumber(Context context, String countryCode) {

        TelephonyManager telemamanger = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String line1Number = telemamanger.getLine1Number();

        if (line1Number != null && !line1Number.isEmpty()) {
            return line1Number.replaceFirst(countryCode, "");
        }

        return "";
    }

    public static String getContactsCheckSum(Set<Contact> contacts) throws NoSuchAlgorithmException {

        String phones = "";

        if (contacts != null && !contacts.isEmpty()) {

            for (Contact c : contacts)
                phones += c.getPhone();

            return Utils.sha1(phones).toString();
        }

        return null;
    }
}
