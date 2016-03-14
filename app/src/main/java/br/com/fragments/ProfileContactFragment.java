package br.com.fragments;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.br.wheresapp.R;

import org.jivesoftware.smack.packet.Presence;

import java.io.IOException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import br.com.activities.Chat;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.aplication.Phones;
import br.com.aplication.Storage;
import br.com.dao.PhoneContactDAO;
import br.com.listener.AppBarStateChangeListener;
import br.com.model.Transaction;
import br.com.model.domain.Contact;
import br.com.net.Http;
import br.com.service.DateService;
import br.com.service.ImageService;
import br.com.service.PhoneFormatTextWatcher;
import br.com.service.TaskLight;
import br.com.smack.Smack;
import br.com.util.NotificationCenter;
import br.com.util.Utils;
import br.com.widgets.Emoji;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileContactFragment extends Fragment implements Observer, NotificationCenter.NotificationCenterDelegate {

    private final String TAG = "ProfileContactFragment";

    private Application application;
    private Contact contact;
    private Handler handler;

    // widgets
    private AppBarLayout appBarLayout;
    private ImageView profilePhoto;
    private TextView textLastActivity;
    private TextView textStatus;
    private TextView textPhoneNumber;

    // Gerenciador da actionbar
    private ActionBar actionBar;
    private ActionBarHolder actionBarHolder;

    // DAO
    private PhoneContactDAO phoneContactDAO;
    private Toolbar toolbar;

    public ProfileContactFragment() {
    }

    public static Fragment newInstance() {
        return new ProfileContactFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        handler = new Handler(Looper.getMainLooper());
        application = (Application) getActivity().getApplicationContext();
        phoneContactDAO = PhoneContactDAO.instance(getContext());

        // pega contato atual
        contact = ((Chat) getActivity()).getContact();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appBarLayout = (AppBarLayout) view.findViewById(R.id.app_bar);
        profilePhoto = (ImageView) view.findViewById(R.id.profile_photo);
        textLastActivity = (TextView) view.findViewById(R.id.last_activity);
        textStatus = (TextView) view.findViewById(R.id.text_status);
        textPhoneNumber = (TextView) view.findViewById(R.id.text_phone);

        // download da imagem original do contato
        getOriginalContactPicture();

        // checa se o contato possui foto de perfil e atribui ao widget
        if (contact.getPhoto() != null)
            profilePhoto.setImageBitmap(ImageService.byteToImage(contact.getPhoto()));
        else
            profilePhoto.setColorFilter(Color.parseColor(App.Colors.APP_COLOR.getCode()));

        // cria e configura a actionbar
        createActionBar(view);

        // setup ultima vez online
        setupLastActivity();

        textStatus.setText(Emoji.replaceEmoji(contact.getStatus(), textStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
        textPhoneNumber.setText(Phones.INTERNATIONAL_IDENTIFIER + contact.getDdi() + " " + PhoneFormatTextWatcher.formatNumber(contact.getPhone(), Phones.getCountryISO(getContext(), contact.getDdi())));

        appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {

            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {

                if (state == State.EXPANDED)
                    textLastActivity.setVisibility(View.VISIBLE);
                else if (state == State.COLLAPSED)
                    textLastActivity.setVisibility(View.GONE);

            }
        });

        application.smackService.addPresenceObserver(this);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contact_profile, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int menuID = item.getItemId();

        if (menuID == R.id.edit_contact) {

            Uri lookupUri = phoneContactDAO.lookupURI(contact.getId());

            if (lookupUri != null) {

                Intent editIntent = new Intent(Intent.ACTION_EDIT);
                editIntent.setDataAndType(lookupUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                editIntent.putExtra("finishActivityOnSaveCompleted", true);

                startActivity(editIntent);
            }

        }

        return false;
    }

    private void createActionBar(View view) {

        toolbar = (Toolbar) view.findViewById(R.id.profile_toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(contact.getName());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

//        TextView name = (TextView) view.findViewById(R.id.name);
        textLastActivity = (TextView) view.findViewById(R.id.last_activity);

        // setup nome do contato
//        name.setText(contact.getName());

        // cria holder para ActionBar
        actionBarHolder = new ActionBarHolder(null, textLastActivity);

    }

    private void setupLastActivity() {

        try {

            App.runBackgroundService(new Runnable() {

                @Override
                public void run() {

                    // espera se conectar
                    application.smackService.waitUntilOnline();

                    Date lastLogin = null;
                    String status;

                    // tenta obter a data do ultimo login do contato
                    try {
                        lastLogin = application.smackService.getLastActivity(Smack.parseContact(contact.getDdi(), contact.getPhone()));
                        status = lastLogin == null ? getString(R.string.online) : DateService.formatLastActiviy(lastLogin);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                        status = "";
                    }

                    final String finalStatus = status;

                    // atualiza UI
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            // atualiza GUI
                            actionBarHolder.getLastActivity().setText(finalStatus);
                        }
                    });

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getOriginalContactPicture() {

        TaskLight.start(new Transaction() {

            @Override
            public void init() {
                // DO NOTHING
            }

            @Override
            public Object perform() {

                Bitmap picture = null;

                try {

                    if (Utils.isNetworkAvailable(getContext())) {

                        // download da imagem
                        picture = Http.downloadOriginalPicture(contact.getDdi(), contact.getPhone());

                        // escreve a imagem no disco
                        ImageService.writeContactPictureOnDisk(picture, contact.getDdi(), contact.getPhone());

                    } else
                        picture = ImageService.readImageFromDisk(Storage.getContactPictureFilePath(contact.getDdi(), contact.getPhone()));

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                return picture;
            }

            @Override
            public void updateView(Object o) {

                if (o != null)
                    profilePhoto.setImageBitmap((Bitmap) o);
            }
        });
    }

    @Override
    public void update(Observable observable, Object data) {

        final Presence presence = (Presence) data;

        handler.post(new Runnable() {

            @Override
            public void run() {

                String la = presence.isAvailable() ? getResources().getString(R.string.online) : DateService.formatLastActiviy(new Date());

                actionBarHolder.getLastActivity().setText(la);
            }
        });

    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

        if (id == NotificationCenter.emojiDidLoaded)
            textStatus.setText(Emoji.replaceEmoji(contact.getStatus(), textStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
    }

    public class ActionBarHolder {

        private TextView name;
        private TextView lastActivity;

        public ActionBarHolder(TextView name, TextView lastActivity) {
            this.name = name;
            this.lastActivity = lastActivity;
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }

        public TextView getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(TextView lastActivity) {
            this.lastActivity = lastActivity;
        }
    }

}
