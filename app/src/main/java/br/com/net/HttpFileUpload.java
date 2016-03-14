package br.com.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by MarioJ on 24/04/15.
 */
public class HttpFileUpload {

    private String url;
    private HttpURLConnection con;
    private OutputStream os;

    private String delimiter = "--";
    private String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";

    public HttpFileUpload(String url) {
        this.url = url;
    }

    public byte[] downloadImage(String imgName) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            System.out.println("URL [" + url + "] - Name [" + imgName + "]");

            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();
            con.getOutputStream().write(("name=" + imgName).getBytes());

            InputStream is = con.getInputStream();
            byte[] b = new byte[1024];

            while (is.read(b) != -1)
                baos.write(b);

            con.disconnect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return baos.toByteArray();
    }

    public void connectForMultipart() throws Exception {

        con = (HttpURLConnection) (new URL(url)).openConnection();

        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.connect();

        os = con.getOutputStream();
    }

    public void addFormPart(String paramName, String value) throws Exception {
        writeParamData(paramName, value);
    }

    public void addFilePart(String paramName, String fileName, byte[] data) throws Exception {

        os.write((delimiter + boundary + "\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
        os.write(("Content-Type: application/octet-stream\r\n").getBytes());
        os.write(("Content-Transfer-Encoding: binary\r\n").getBytes());
        os.write("\r\n".getBytes());

        os.write(data);

        os.write("\r\n".getBytes());
    }

    public void finishMultipart() throws Exception {

        os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
        os.flush();
    }

    public boolean success() throws IOException {
        return con.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    public String getResponse() throws Exception {

        InputStream is = con.getInputStream();
        byte[] b1 = new byte[1024];
        StringBuffer buffer = new StringBuffer();

        while (is.read(b1) != -1)
            buffer.append(new String(b1));

        is.close();
        os.close();
        con.disconnect();

        return buffer.toString();
    }


    private void writeParamData(String paramName, String value) throws Exception {

        os.write((delimiter + boundary + "\r\n").getBytes());
        os.write("Content-Type: text/plain\r\n".getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());
        os.write(("\r\n" + value + "\r\n").getBytes());

    }

    public void close() throws IOException {
        os.close();
        con.disconnect();
    }

}
