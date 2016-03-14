package br.com.service;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import br.com.aplication.Phones;

/**
 * Created by MarioJ on 30/07/15.
 */
public class PhoneFormatTextWatcher implements TextWatcher {

    private final String TAG = "PhoneFormatTextWatcher";

    String countryISO;
    private PhoneNumberUtil phoneNumberUtil;
    boolean editing;

    public PhoneFormatTextWatcher(String countryISO) {
        phoneNumberUtil = PhoneNumberUtil.getInstance();

        setTypeFormatter(countryISO);
    }

    public void setTypeFormatter(String countryISO) {
        this.countryISO = countryISO;
    }

    public String formatNumber(String number) {

        number = number.replaceAll("[^0-9" + Phones.INTERNATIONAL_IDENTIFIER + "]", "");

        AsYouTypeFormatter formatter = phoneNumberUtil.getAsYouTypeFormatter(countryISO);

        for (int i = 0; i < number.length() - 1; i++)
            formatter.inputDigit(number.charAt(i));

        return formatter.inputDigit(number.charAt(number.length() - 1));

    }

    public static String formatNumber(String number, String countryISO) {

        PhoneFormatTextWatcher formatter = new PhoneFormatTextWatcher(countryISO);
        return formatter.formatNumber(number);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {

        if (!editing) {

            // get the current phone in edittext
            String getPhone = editable.toString();

            editing = true;

            if (!getPhone.isEmpty()) {

                String number = formatNumber(getPhone);

                editable.clear();
                editable.append(number);
            }

            editing = false;

        }

    }
}
