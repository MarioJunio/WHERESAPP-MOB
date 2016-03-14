package br.com.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.br.wheresapp.R;

import org.jivesoftware.smack.SmackException;

import java.io.File;
import java.io.IOException;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.aplication.Storage;
import br.com.aplication.Tag;
import br.com.model.Category;
import br.com.model.Transaction;
import br.com.model.domain.Person;
import br.com.net.Http;
import br.com.net.HttpFileUpload;
import br.com.service.ImageService;
import br.com.service.TaskLight;
import br.com.util.AndroidUtilities;
import br.com.util.Constants;
import br.com.util.Dialogs;
import br.com.util.NotificationCenter;
import br.com.util.Utils;
import br.com.widgets.Emoji;
import br.com.widgets.EmojiView;
import br.com.widgets.SizeNotifierRelativeLayout;

/**
 * Created by MarioJ on 27/03/15.
 */
public class ConfigurationProfileFragment extends Fragment implements AdapterView.OnItemClickListener, SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate, NotificationCenter.NotificationCenterDelegate {

    public static final String TAG = "ConfigurationProfileFragment";

    private Application application;
    private AlertDialog dialog;
    private File takePictureImage;
    private View framePicture;
    private EditText inNome;
    private EditText inStatus;
    private ImageView imgProfile, imgEmotion;
    private ProgressBar progressUploadPhoto;

    private EmojiView emojiView;
    private WindowManager.LayoutParams windowLayoutParams;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private boolean showingEmoji;
    private int keyboardHeight;
    private boolean keyboardVisible;

    private View.OnKeyListener backButton = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                hideEmojiPopup();
                return true;
            }

            return false;
        }
    };

    public static ConfigurationProfileFragment newInstance() {
        return new ConfigurationProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        try {
            application = (Application) getActivity().getApplicationContext();
            application.doBindSmackService(getContext());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuration_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // trata evento de click
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (showingEmoji)
                    hideEmojiPopup();

                Utils.hideKeyboard(getActivity());

            }
        });

        // trata evento de back button
        view.setOnKeyListener(backButton);

        application = (Application) getActivity().getApplication();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getActivity().getString(R.string.profile_photo));

        AndroidUtilities.statusBarHeight = App.getStatusBarHeight(getContext());

        framePicture = view.findViewById(R.id.frame_picture);
        inNome = (EditText) view.findViewById(R.id.field_nome);
        inStatus = (EditText) view.findViewById(R.id.field_status);
        imgProfile = (ImageView) view.findViewById(R.id.picture);
        imgEmotion = (ImageView) view.findViewById(R.id.ic_emoji);
        progressUploadPhoto = (ProgressBar) view.findViewById(R.id.progress_loading);
        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) view.findViewById(R.id.root);

        initListener();

        if (application.getCurrentUser().getPhoto() != null) {
            imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgProfile.setImageBitmap(ImageService.byteToImage(application.getCurrentUser().getPhoto()));
        } else {
            // verifica se o usuario possui foto no servidor
            downloadUserPicture();
        }

        inNome.setText(application.getCurrentUser().getName());
        inStatus.setText(Emoji.replaceEmoji(application.getCurrentUser().getStatus(), inStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));

        sizeNotifierRelativeLayout.delegate = this;
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
    }

    private void downloadUserPicture() {

        TaskLight.start(new Transaction() {

            @Override
            public void init() {
                progressUploadPhoto.setVisibility(View.VISIBLE);
            }

            @Override
            public Object perform() {

                Bitmap bitmap = null;

                try {

                    bitmap = Http.downloadOriginalPicture(application.getCurrentUser().getDdi(), application.getCurrentUser().getPhone());

                    if (bitmap != null) {

                        // SET: photo bytes para usuario
                        application.getCurrentUser().setPhoto(ImageService.imageToByte(bitmap));

                        // atualiza photo do usuario atual
                        application.updatePhoto();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                return bitmap;
            }

            @Override
            public void updateView(Object o) {

                if (o != null) {
                    imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgProfile.setImageBitmap((Bitmap) o);
                } else
                    imgProfile.setImageBitmap(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_account_gray));

                progressUploadPhoto.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_configuration, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int selectedID = item.getItemId();

        if (selectedID == R.id.menu_next)
            saveProfile();

        return true;
    }

    private void initListener() {

        framePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (showingEmoji)
                    hideEmojiPopup();

                dialog = Dialogs.choosePictureCreate(getActivity(), ConfigurationProfileFragment.this);
                dialog.show();
            }
        });

        imgEmotion.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showEmojiPopup(!showingEmoji);
            }
        });

        inNome.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (showingEmoji)
                    hideEmojiPopup();
            }
        });

        inStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (showingEmoji)
                    hideEmojiPopup();
            }
        });

        inNome.setOnKeyListener(backButton);
        inStatus.setOnKeyListener(backButton);
    }

    private void saveProfile() {

        if (showingEmoji)
            hideEmojiPopup();

        // set user name to get name from input
        application.getCurrentUser().setName(inNome.getText().toString());

        // set user status to get name from input
        application.getCurrentUser().setStatus(inStatus.getText().toString().isEmpty() ? Constants.WheresAppStandardStatus() : inStatus.getText().toString());

        // update user
        application.update();

        if (Utils.isNetworkAvailable(getActivity().getApplicationContext())) {

            App.runBackgroundService(new Runnable() {

                @Override
                public void run() {

                    try {
                        application.smackService.turnOn();
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }
            });

            // go to next step
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content, ConfigurationInitializingFragment.newInstance()).commit();
        } else
            Toast.makeText(getActivity().getApplicationContext(), R.string.network_not_available, Toast.LENGTH_LONG).show();

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (id == Category.GALLERY) {
            System.out.println("Galeria");

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");

            startActivityForResult(intent, Category.GALLERY);

        } else if (id == Category.TAKE_PICTURE) {
            System.out.println("Tirar foto");

            takePictureImage = Storage.getGeneratedImage();

            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(takePictureImage));

            startActivityForResult(i, Category.TAKE_PICTURE);

        } else if (id == Category.REMOVE_PICTURE) {
            System.out.println("Remover foto");

            imgProfile.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imgProfile.setImageResource(R.drawable.ic_account_gray);
            application.getCurrentUser().setPhoto(null);

            // atualiza photo do usuario atual
            application.updatePhoto();
        }

        dialog.dismiss();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        TaskLight.start(new Transaction() {

            @Override
            public void init() {
            }

            @Override
            public Object perform() {

                try {

                    // result code
                    final boolean OK = resultCode == getActivity().RESULT_OK;

                    Bitmap img = null, imgTb = null;

                    if (requestCode == Category.GALLERY && OK) {

                        if (data != null) {

                            img = ImageService.compress(Utils.getRealPath(getActivity(), data.getData()));
                            imgTb = ImageService.makeThumb(Utils.getRealPath(getActivity(), data.getData()));

                        } else
                            return null;

                    } else if (requestCode == Category.TAKE_PICTURE && OK) {

                        if (takePictureImage != null) {

                            img = ImageService.compress(takePictureImage.getAbsolutePath());
                            imgTb = ImageService.makeThumb(takePictureImage.getAbsolutePath());

                        } else
                            return null;

                    }

                    return new Bitmap[]{img, imgTb};

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            public void updateView(Object o) {

                final Bitmap[] imgs = (Bitmap[]) o;

                if (!Utils.isNetworkAvailable(getActivity().getApplicationContext()))
                    Toast.makeText(getActivity().getApplicationContext(), R.string.network_not_available, Toast.LENGTH_LONG).show();
                else if (imgs[0] == null || imgs[1] == null)
                    Toast.makeText(getActivity().getApplicationContext(), R.string.error_load_photo, Toast.LENGTH_LONG).show();
                else
                    uploadPhoto(imgs[0], imgs[1]);

            }
        });
    }

    private void uploadPhoto(final Bitmap image, final Bitmap imageThumb) {

        // Envia a foto de perfil e seu thumbnail de modo assincrono ao servidor para armazenamento
        TaskLight.start(new Transaction() {

            byte[] imageBytes, imageThumbBytes;

            @Override
            public void init() {

                progressUploadPhoto.setVisibility(View.VISIBLE);

                imageBytes = ImageService.imageToByte(image);
                imageThumbBytes = ImageService.imageToByte(imageThumb);

                // set photo to imageview
                imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgProfile.setImageBitmap(image);

            }

            @Override
            public Object perform() {

                HttpFileUpload httpFileUpload = new HttpFileUpload(Http.HOST_CONTACT_API);

                boolean success = false;

                try {

                    httpFileUpload.connectForMultipart();
                    httpFileUpload.addFormPart(Http.TAG_PARAMETER, Tag.HTTP.PHOTO_UPDATE.ordinal() + "");
                    httpFileUpload.addFormPart(Person.DDI, application.getCurrentUser().getDdi());
                    httpFileUpload.addFormPart(Person.PHONE, application.getCurrentUser().getPhone());
                    httpFileUpload.addFilePart(Person.PHOTO_URI, null, imageBytes);
                    httpFileUpload.addFilePart(Person.PHOTO_URI_THUMB, null, imageThumbBytes);
                    httpFileUpload.finishMultipart();

                    success = httpFileUpload.success();
                    httpFileUpload.close();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return success;
            }

            @Override
            public void updateView(Object o) {

                if ((Boolean) o) {

                    // SET: photo bytes para usuario
                    application.getCurrentUser().setPhoto(imageBytes);

                    // atualiza photo do usuario atual
                    application.updatePhoto();

                } else
                    Toast.makeText(getActivity().getApplicationContext(), R.string.error_load_photo, Toast.LENGTH_LONG).show();

                progressUploadPhoto.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

        if (id == NotificationCenter.emojiDidLoaded) {

            if (emojiView != null) {
                emojiView.invalidateViews();
            }
        }

    }

    @Override
    public void onSizeChanged(int height) {

        Rect localRect = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);

        WindowManager wm = (WindowManager) Application.getInstance().getSystemService(Activity.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null) {
            return;
        }


        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            keyboardHeight = height;
            Application.getInstance().getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).commit();
        }


        if (showingEmoji) {
            int newHeight = 0;

            newHeight = keyboardHeight;

            if (windowLayoutParams.width != AndroidUtilities.displaySize.x || windowLayoutParams.height != newHeight) {
                windowLayoutParams.width = AndroidUtilities.displaySize.x;
                windowLayoutParams.height = newHeight;

                wm.updateViewLayout(emojiView, windowLayoutParams);
                if (!keyboardVisible) {
                    sizeNotifierRelativeLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            if (sizeNotifierRelativeLayout != null) {
                                sizeNotifierRelativeLayout.setPadding(0, 0, 0, windowLayoutParams.height);
                                sizeNotifierRelativeLayout.requestLayout();
                            }
                        }
                    });
                }
            }
        }


        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;

        if (keyboardVisible && sizeNotifierRelativeLayout.getPaddingBottom() > 0) {
            showEmojiPopup(false);
        } else if (!keyboardVisible && keyboardVisible != oldValue && showingEmoji) {
            showEmojiPopup(false);
        }

    }

    private void showEmojiPopup(boolean show) {

        showingEmoji = show;

        if (show) {

            if (emojiView == null) {

                if (getActivity() == null)
                    return;

                emojiView = new EmojiView(getActivity());

                emojiView.setListener(new EmojiView.Listener() {

                    public void onBackspace() {
                        inStatus.dispatchKeyEvent(new KeyEvent(0, 67));
                    }

                    public void onEmojiSelected(String symbol) {

                        int i = inStatus.getSelectionEnd();

                        if (i < 0) {
                            i = 0;
                        }
                        try {

                            CharSequence localCharSequence = Emoji.replaceEmoji(symbol, inStatus.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20));

                            inStatus.setText(inStatus.getText().insert(i, localCharSequence));

                            int j = i + localCharSequence.length();

                            inStatus.setSelection(j, j);

                        } catch (Exception e) {
                            Log.e(Constants.TAG, "Error showing emoji");
                        }
                    }
                });

                windowLayoutParams = new WindowManager.LayoutParams();
                windowLayoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;

                if (Build.VERSION.SDK_INT >= 21) {
                    windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                } else {
                    windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
                    windowLayoutParams.token = getActivity().getWindow().getDecorView().getWindowToken();
                }
                windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }

            // muda imagem para keyboard
            imgEmotion.setImageResource(R.drawable.ic_keyboard);

            final int currentHeight;

            if (keyboardHeight <= 0)
                keyboardHeight = Application.getInstance().getSharedPreferences("emoji", 0).getInt("kbd_height", AndroidUtilities.dp(200));

            currentHeight = keyboardHeight;

            WindowManager wm = (WindowManager) Application.getInstance().getSystemService(Activity.WINDOW_SERVICE);

            windowLayoutParams.height = currentHeight;
            windowLayoutParams.width = AndroidUtilities.displaySize.x;

            try {

                if (emojiView.getParent() != null)
                    wm.removeViewImmediate(emojiView);

            } catch (Exception e) {
                Log.e(Constants.TAG, e.getMessage());
            }

            try {
                wm.addView(emojiView, windowLayoutParams);
            } catch (Exception e) {
                Log.e(Constants.TAG, e.getMessage());
                return;
            }

            if (!keyboardVisible) {
                if (sizeNotifierRelativeLayout != null) {
                    sizeNotifierRelativeLayout.setPadding(0, 0, 0, currentHeight);
                }

                return;
            }

        } else {

            removeEmojiWindow();

            // muda imagem para emoticon
            imgEmotion.setImageResource(R.drawable.ic_emoticon);

            if (sizeNotifierRelativeLayout != null) {
                sizeNotifierRelativeLayout.post(new Runnable() {
                    public void run() {
                        if (sizeNotifierRelativeLayout != null) {
                            sizeNotifierRelativeLayout.setPadding(0, 0, 0, 0);
                        }
                    }
                });
            }
        }


    }

    private void removeEmojiWindow() {

        if (emojiView == null) {
            return;
        }
        try {

            if (emojiView.getParent() != null) {

                WindowManager wm = (WindowManager) Application.getInstance().getSystemService(Context.WINDOW_SERVICE);
                wm.removeViewImmediate(emojiView);

            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.getMessage());
        }
    }


    /**
     * Hides the emoji popup
     */
    public void hideEmojiPopup() {

        if (showingEmoji) {
            showEmojiPopup(false);
        }
    }

}
