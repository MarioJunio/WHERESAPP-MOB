package br.com.aplication;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

import br.com.service.ImageService;
import br.com.util.Utils;

/**
 * Created by MarioJ on 09/03/15.
 */
public class Storage {

    public static final String ROOT_DIR = "WheresApp";
    public static final String PROFILE_PHOTOS_DIR = "Profile Photos";
    public static final String CONTACT_PHOTOS_DIR = "Contact Photos";
    public static final String IMAGES_DIR = "Wheresapp Images";

    public static void createDirectoriesApplication(Context context) {

        if (Utils.isExternalStorageWritable()) {

            File profilePicturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PROFILE_PHOTOS_DIR);
            File contactPicturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CONTACT_PHOTOS_DIR);
            File picturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGES_DIR);

            if (!profilePicturesDir.exists())
                profilePicturesDir.mkdir();

            if (!contactPicturesDir.exists())
                contactPicturesDir.mkdir();

            if (!picturesDir.exists())
                picturesDir.mkdir();

        } else {
            Toast.makeText(context, "Não há cartão de memória instalado no dispositivo", Toast.LENGTH_LONG).show();
        }

    }

    public static void deleteProfilePictures() {

        File profilePicturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PROFILE_PHOTOS_DIR);

        for (File file : profilePicturesDir.listFiles())
            file.delete();

    }

    public static String getContactPictureFilePath(String ddi, String phone) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Storage.CONTACT_PHOTOS_DIR);
        return file.getAbsolutePath() + File.separator + (ddi + phone) + ImageService.IMAGE_EXT;
    }

    public static File getGeneratedImage() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Storage.IMAGES_DIR);
        return new File(file.getAbsolutePath() + File.separator + "IMG_" + System.currentTimeMillis() + ".jpg");
    }
}
