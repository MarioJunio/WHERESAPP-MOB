package br.com.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import br.com.aplication.Api;
import br.com.aplication.App;
import br.com.aplication.Tag;
import br.com.model.Confirmation;
import br.com.model.domain.Contact;
import br.com.model.domain.Person;

/**
 * Created by MarioJ on 01/06/15.
 */
public class Http {

    public static String TAG = "Http";
    public static String HOST_HTTP = "http://three.primeirabusca.com.br:8080/WheresApp/";
    public static String HOST_CONTACT_API = HOST_HTTP + Api.CONTACT_API.getName();
    public static String TAG_PARAMETER = "TAG";
    public static String POST_REQUEST = "POST";
    public static String GET_REQUEST = "GET";
    public static String CHARSET = "UTF-8";
    private static Gson gson;

    static {
        gson = new Gson();
    }

    public static Confirmation checkAccount(String ddi, String phone) throws IOException, JSONException {

        String param = String.format("%s=%s&%s=%s&%s=%s", TAG_PARAMETER, encodeData(String.valueOf(Tag.HTTP.CHECK_ACCOUNT.ordinal())), Person.DDI, encodeData(ddi),
                Person.PHONE, encodeData(phone));

        System.out.println("CHECK_ACCOUNT [params] -> " + param);

        HttpURLConnection connection = (HttpURLConnection) new URL(HOST_HTTP.concat(Api.CONTACT_API.getName())).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(POST_REQUEST);
        connection.setFixedLengthStreamingMode(param.getBytes().length);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // send parameters to server
        connection.getOutputStream().write(param.getBytes());

        // get response from server
        String data = getResponse(connection.getInputStream());

        // close connection with server
        connection.disconnect();

        // check if response is empty, whether yes return null to indicate that there is not response
        if (data.isEmpty())
            return null;

        // resposta do webservice
        JSONObject response = new JSONObject(data);

        Log.d(TAG, response.toString());

        return new Confirmation(response.getBoolean(Confirmation.OK), response.getBoolean(Confirmation.NEW));
    }

    public static boolean confirmCode(String ddi, String phone, String confirmCode) throws IOException, JSONException {

        String url = String.format("%s?%s=%d&%s=%s&%s=%s&%s=%s", HOST_HTTP + Api.CONTACT_API.getName(), TAG_PARAMETER, Tag.HTTP.CONFIRM_CODE.ordinal(), Contact.DDI, ddi, Contact.PHONE, phone, Contact.CONFIRM_CODE, confirmCode);

        Log.d(TAG, url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(GET_REQUEST);
        connection.connect();

        int code = connection.getResponseCode();
        String response = getResponse(connection.getInputStream());

        connection.disconnect();

        if (code == HttpURLConnection.HTTP_OK && response != null && !response.isEmpty())
            return new JSONObject(response).getBoolean(Confirmation.OK);

        return false;
    }

    public static Contact checkContact(String phone) throws IOException {

        String url = String.format("%s?%s=%d&%s=%s", HOST_HTTP + Api.CONTACT_API.getName(), TAG_PARAMETER, Tag.HTTP.ACCOUNT_RETRIEVE.ordinal(), Contact.PHONE, phone);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(GET_REQUEST);
        connection.connect();

        int code = connection.getResponseCode();
        String response = getResponse(connection.getInputStream());

        connection.disconnect();

        if (code == HttpURLConnection.HTTP_OK && response != null && !response.isEmpty())
            return new Gson().fromJson(response, Contact.class);

        return null;
    }

    public static long getLastModifiedPhotoTb(String ddi, String phone, String lastModified) throws IOException {

        long getLastModified = 0l;

        String param = String.format("?%s=%s&%s=%s&%s=%s&%s=%s", TAG_PARAMETER, Tag.HTTP.LAST_MODIFIED_TN_RETRIEVE.ordinal(), Contact.DDI, encodeData(ddi), Contact.PHONE, encodeData(phone),
                Contact.LAST_MODIFIED, encodeData(lastModified));

        HttpURLConnection connection = (HttpURLConnection) new URL(HOST_CONTACT_API.concat(param)).openConnection();
        connection.setRequestMethod(GET_REQUEST);
        connection.setConnectTimeout(App.HTTP_TIMEOUT);
        connection.connect();

        int code = connection.getResponseCode();
        String response = getResponse(connection.getInputStream());

        connection.disconnect();

        if (code == HttpURLConnection.HTTP_OK && response != null && !response.isEmpty()) {
            getLastModified = Long.parseLong(response);
        }

        return getLastModified;
    }


    public static List<Contact> syncronizeContacts(String phones) {

        List<Contact> contacts = null;

        try {
            String url = HOST_HTTP + Api.CONTACT_API.getName();
            String param = String.format("%s=%s&%s=%s", TAG_PARAMETER, encodeData(String.valueOf(Tag.HTTP.SYNCRONIZE_CONTACTS.ordinal())), Contact.CONTACTS_LIST, encodeData(phones));

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST);
            connection.setFixedLengthStreamingMode(param.getBytes().length);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.getOutputStream().write(param.getBytes());

            String response = getResponse(connection.getInputStream());

            int responseCode = connection.getResponseCode();

            connection.disconnect();

            Log.d(TAG, "[syncronizeContacts] response -> " + response);

            if (response != null && responseCode == HttpURLConnection.HTTP_OK) {

                contacts = new Gson().fromJson(response, new TypeToken<ArrayList<Contact>>() {
                }.getType());

            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return contacts;
    }

    public static Bitmap downloadPictureThumb(String ddi, String phone, String lastModified) throws IOException {

        String param = String.format("?%s=%s&%s=%s&%s=%s&%s=%s", TAG_PARAMETER, Tag.HTTP.PHOTO_TN_RETRIEVE.ordinal(), Contact.DDI, encodeData(ddi), Contact.PHONE, encodeData(phone), Contact.LAST_MODIFIED, encodeData(lastModified));

        HttpURLConnection connection = (HttpURLConnection) new URL(HOST_CONTACT_API.concat(param)).openConnection();
        connection.setDoInput(true);
        connection.setRequestMethod(GET_REQUEST);
        connection.setConnectTimeout(App.HTTP_TIMEOUT);
        connection.connect();

        // InputStream dos dados
        InputStream inputStream = null;

        // imagem bitmap para retornar
        Bitmap thumbPicture = null;

        try {

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                inputStream = connection.getInputStream();

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inSampleSize = 1;

                thumbPicture = BitmapFactory.decodeStream(inputStream, null, bmOptions);
            }

        } finally {

            if (inputStream != null)
                inputStream.close();

            connection.disconnect();
        }

        return thumbPicture;
    }

    public static Bitmap downloadOriginalPicture(String ddi, String phone) throws IOException {

        String param = String.format("?%s=%s&%s=%s&%s=%s", TAG_PARAMETER, Tag.HTTP.PHOTO_RETRIEVE.ordinal(), Contact.DDI, encodeData(ddi), Contact.PHONE, encodeData(phone));

        Log.d(TAG, "downloadOriginalPicture -> " + param);

        HttpURLConnection connection = (HttpURLConnection) new URL(HOST_CONTACT_API.concat(param)).openConnection();
        connection.setDoInput(true);
        connection.setRequestMethod(GET_REQUEST);
        connection.setConnectTimeout(App.HTTP_TIMEOUT);
        connection.connect();

        InputStream inputStream = null;
        Bitmap picture = null;

        try {

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                inputStream = connection.getInputStream();

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inSampleSize = 1;

                picture = BitmapFactory.decodeStream(inputStream, null, bmOptions);
            }

        } finally {

            if (inputStream != null)
                inputStream.close();

            connection.disconnect();
        }

        return picture;
    }

    private static String getResponse(InputStream inputStream) throws IOException {

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, CHARSET);

        StringBuffer buffer = new StringBuffer();
        Scanner scanner = new Scanner(inputStreamReader);

        while (scanner.hasNext())
            buffer.append(scanner.nextLine());

        // close stream reader
        inputStreamReader.close();

        return buffer.toString();
    }

    private static String encodeData(String data) throws UnsupportedEncodingException {
        return URLEncoder.encode(data, CHARSET);
    }

}
