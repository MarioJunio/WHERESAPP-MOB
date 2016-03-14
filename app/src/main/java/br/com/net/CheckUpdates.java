package br.com.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Date;

import com.br.wheresapp.R;
import br.com.adapter.ChatSearchAdapter;
import br.com.adapter.ContactsAdapter;
import br.com.adapter.ViewHolderSimple;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.model.Transaction;
import br.com.model.domain.Contact;
import br.com.service.ImageService;
import br.com.service.PhoneFormatTextWatcher;
import br.com.aplication.Application;
import br.com.service.TaskLight;
import br.com.util.Animations;
import br.com.aplication.Phones;
import br.com.util.Utils;

/**
 * Created by MarioJ on 13/05/15.
 */
public class CheckUpdates {

    private static String TAG = "CheckUpdates";

    public static void update(final Application session, final Contact contact, final ContactsAdapter.ContactViewHolder view) throws IOException {

        final ContactDAO contactDAO = ContactDAO.instance(session.getApplicationContext());

        TaskLight.start(new Transaction() {

            @Override
            public void init() {

                Bitmap photo = null;

                // if contact is not in wheresapp contacts
                if (contact.getId() == null || contact.getId() == 0) {

                    // format incoming number
                    contact.setName(PhoneFormatTextWatcher.formatNumber(Phones.INTERNATIONAL_IDENTIFIER + contact.getDdi() + contact.getPhone(), session.getCountries().get(contact.getDdi()).get(0)));

                    Log.d(TAG, "Contact -> " + contact.getName());

                    if (contact.getPhoto() != null)
                        photo = ImageService.byteToImage(contact.getPhoto());

                } else {

                    // get contact name if exists in device or format number using country iso
                    contact.setName(PhoneContactDAO.instance(session.getApplicationContext()).getContactName(contact.getId()));

                    // get photo in database
                    photo = contactDAO.getImage(contact.getId());

                    if (photo != null)
                        contact.setPhoto(ImageService.imageToByte(photo));
                }

                // set view name
                view.name.setText(contact.getName());

                if (photo != null)
                    setViewImage(view.img, photo);
                else
                    view.img.setImageBitmap(BitmapFactory.decodeResource(session.getApplicationContext().getResources(), R.drawable.ic_contact));

            }

            @Override
            public Object perform() {

                // if network is not available, return null
                if (!Utils.isNetworkAvailable(session.getApplicationContext())) {
                    // if entered here, because dont have updates in server then load local bitmap
                    return null;
                }

                try {

                    String lastModifiedContact = String.valueOf(contact.getLastModified() != null ? contact.getLastModified().getTime() : 0l);

                    // download image from server whether there is
                    Bitmap bitmap = Http.downloadPictureThumb(contact.getDdi(), contact.getPhone(), lastModifiedContact);

                    // check for updates in server
                    long getLastModified = Http.getLastModifiedPhotoTb(contact.getDdi(), contact.getPhone(), lastModifiedContact);

                    // check if contact retrieved from server is not null
                    if (bitmap != null && getLastModified != 0) {

                        contact.setPhoto(ImageService.imageToByte(bitmap));
                        contact.setLastModified(new Date(getLastModified));

                        if (contact.getId() != null && contact.getId() != 0)
                            contactDAO.updateContactAndLastModified(contact);

                        return bitmap;
                    }

                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }

                return null;
            }

            @Override
            public void updateView(Object o) {
                setViewImage(view.img, (Bitmap) o);
            }

        });

    }

    public static void update(final Application session, final Contact contact, final ViewHolderSimple view) throws IOException {

        final ContactDAO contactDAO = ContactDAO.instance(session.getApplicationContext());

        TaskLight.start(new Transaction() {

            @Override
            public void init() {

                Bitmap photo = null;

                // if contact is not in wheresapp contacts
                if (contact.getId() == null || contact.getId() == 0) {

                    // format incoming number
                    contact.setName(PhoneFormatTextWatcher.formatNumber(Phones.INTERNATIONAL_IDENTIFIER + contact.getDdi() + contact.getPhone(), session.getCountries().get(contact.getDdi()).get(0)));

                    Log.d(TAG, "Contact -> " + contact.getName());

                    if (contact.getPhoto() != null)
                        photo = ImageService.byteToImage(contact.getPhoto());

                } else {

                    // get contact name if exists in device or format number using country iso
                    contact.setName(PhoneContactDAO.instance(session.getApplicationContext()).getContactName(contact.getId()));

                    // get photo in database
                    photo = contactDAO.getImage(contact.getId());

                    if (photo != null)
                        contact.setPhoto(ImageService.imageToByte(photo));
                }

                // set view name
                view.name.setText(contact.getName());

                if (photo != null)
                    setViewImage(view.img, photo);
                else
                    view.img.setImageBitmap(BitmapFactory.decodeResource(session.getApplicationContext().getResources(), R.drawable.ic_contact));

            }

            @Override
            public Object perform() {

                // if network is not available, return null
                if (!Utils.isNetworkAvailable(session.getApplicationContext())) {
                    // if entered here, because dont have updates in server then load local bitmap
                    return null;
                }

                try {

                    String lastModifiedContact = String.valueOf(contact.getLastModified() != null ? contact.getLastModified().getTime() : 0l);

                    // download image from server whether there is
                    Bitmap bitmap = Http.downloadPictureThumb(contact.getDdi(), contact.getPhone(), lastModifiedContact);

                    // check for updates in server
                    long getLastModified = Http.getLastModifiedPhotoTb(contact.getDdi(), contact.getPhone(), lastModifiedContact);

                    // check if contact retrieved from server is not null
                    if (bitmap != null && getLastModified != 0) {

                        contact.setPhoto(ImageService.imageToByte(bitmap));
                        contact.setLastModified(new Date(getLastModified));

                        if (contact.getId() != null && contact.getId() != 0)
                            contactDAO.updateContactAndLastModified(contact);

                        return bitmap;
                    }

                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }

                return null;
            }

            @Override
            public void updateView(Object o) {
                setViewImage(view.img, (Bitmap) o);
            }

        });

    }

    public static void update(Application session, ContactDAO contactDAO, Contact contact, ChatSearchAdapter.ViewHolder viewHolder, boolean type) {

        Bitmap photo = null;

        // if contact is not in wheresapp contacts
        if (contact.getId() == null || contact.getId() == 0) {

            // format incoming number
            contact.setName(PhoneFormatTextWatcher.formatNumber(Phones.INTERNATIONAL_IDENTIFIER + contact.getDdi() + contact.getPhone(), session.getCountries().get(contact.getDdi()).get(0)));

            Log.d(TAG, "Contact -> " + contact.getName());

            if (contact.getPhoto() != null && type)
                photo = ImageService.byteToImage(contact.getPhoto());

        } else {

            // get contact name if exists in device or format number using country iso
            contact.setName(PhoneContactDAO.instance(session.getApplicationContext()).getContactName(contact.getId()));

            if (type) {

                // get photo in database
                photo = contactDAO.getImage(contact.getId());

                if (photo != null)
                    contact.setPhoto(ImageService.imageToByte(photo));
            }
        }

        // set view name
        viewHolder.name.setText(contact.getName());

        if (type) {

            if (photo != null)
                setViewImage(viewHolder.photo, photo);
            else
                viewHolder.photo.setImageBitmap(BitmapFactory.decodeResource(session.getApplicationContext().getResources(), R.drawable.ic_contact));
        }

    }

    private static void setViewImage(ImageView view, Bitmap image) {

        if (image != null) {
            view.startAnimation(Animations.fadeIn());
            view.setImageBitmap(image);
        }

    }

}
