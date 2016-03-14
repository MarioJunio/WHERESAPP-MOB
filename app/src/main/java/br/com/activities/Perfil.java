package br.com.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.br.wheresapp.R;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.File;
import java.io.IOException;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.aplication.Storage;
import br.com.aplication.Tag;
import br.com.dao.UserDAO;
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
 * Created by MarioJ on 26/02/16.
 */
public class Perfil extends AppCompatActivity implements AdapterView.OnItemClickListener, SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate, NotificationCenter.NotificationCenterDelegate {

    private static final String TAG = "Perfil";

    private Application application;
    private View framePicture;
    private ProgressBar progressUploadPicture;
    private ImageView picture, btEmoji;
    private EditText inStatus, inNome;
    private ImageButton btStatusUpdate, btNomeUpdate;
    private Handler handler;

    // emoji
    private EmojiView emojiView;
    private WindowManager.LayoutParams windowLayoutParams;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private boolean showingEmoji;
    private int keyboardHeight;
    private boolean keyboardVisible;

    // Status e Nome salvos
    private String status, nome;

    // AlertDialog para selecionar a galera de fotos para seleção
    private AlertDialog alertDialog;

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

    private TextWatcher inStatusWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            String current = s.toString();

            if (!current.isEmpty() && !current.equals(status))
                btStatusUpdate.setVisibility(View.VISIBLE);
            else
                btStatusUpdate.setVisibility(View.GONE);

        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private TextWatcher inNomeWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            String current = s.toString();

            if (!current.isEmpty() && !current.equals(nome))
                btNomeUpdate.setVisibility(View.VISIBLE);
            else
                btNomeUpdate.setVisibility(View.GONE);

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    // Imagem carregada para upload
    private File takePictureImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(getMainLooper());
        application = (Application) getApplicationContext();

        // inicializa o status e nome antes de qualquer alteracao
        status = application.getCurrentUser().getStatus();
        nome = application.getCurrentUser().getName();

        setContentView(R.layout.activity_perfil);

        initializeWidgets();
        setupActionBarOptions();

        load();

        init();

        getWindow().getDecorView().setOnKeyListener(backButton);
        inStatus.setOnKeyListener(backButton);
        inNome.setOnKeyListener(backButton);

    }

    public void initializeWidgets() {

        framePicture = findViewById(R.id.frame_picture);
        progressUploadPicture = (ProgressBar) findViewById(R.id.progress_loading);
        picture = (ImageView) findViewById(R.id.picture);
        btEmoji = (ImageView) findViewById(R.id.ic_emoji);
        inStatus = (EditText) findViewById(R.id.in_status);
        inNome = (EditText) findViewById(R.id.in_nome);
        btNomeUpdate = (ImageButton) findViewById(R.id.bt_update_name);
        btStatusUpdate = (ImageButton) findViewById(R.id.bt_update_status);
    }

    private void init() {

        inStatus.setText(Emoji.replaceEmoji(application.getCurrentUser().getStatus(), inStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
        inNome.setText(application.getCurrentUser().getName());

        inStatus.addTextChangedListener(inStatusWatcher);
        inNome.addTextChangedListener(inNomeWatcher);

        btEmoji.setOnClickListener(new View.OnClickListener() {
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

        btNomeUpdate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateNameEvent();
            }
        });

        inStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (showingEmoji)
                    hideEmojiPopup();
            }
        });

        btStatusUpdate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TODO: Atualizar Status no Openfire Server
                updateStatusEvent();
            }
        });

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicou na imagem de perfil");
            }
        });

    }

    private void updateNameEvent() {

        // verifica se a conexao com a internet
        if (!Utils.isNetworkAvailable(getApplicationContext())) {
            resetInputNome();
            Toast.makeText(getApplicationContext(), getString(R.string.network_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {


                try {
                    // atualiza nome no openfire
                    application.smackService.updateName(inNome.getText().toString());

                    // reinicia os campos de alteracao do nome
                    nome = inNome.getText().toString();

                    // altera nome no database local
                    application.getCurrentUser().setName(nome);
                    application.update();

                    // atualiza GUI
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            btNomeUpdate.setVisibility(View.GONE);
                        }
                    });

                } catch (SmackException.NotConnectedException e) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resetInputNome();
                        }
                    });

                    Log.d(TAG, "Nao conectado ao openfire");
                } catch (XMPPException.XMPPErrorException e) {
                    Log.d(TAG, e.getMessage());
                } catch (SmackException.NoResponseException e) {
                    Log.d(TAG, e.getMessage());
                }

            }
        });
    }

    private void updateStatusEvent() {

        // verifica se a conexao com a internet
        if (!Utils.isNetworkAvailable(getApplicationContext())) {
            resetInputStatus();
            Toast.makeText(getApplicationContext(), getString(R.string.network_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                try {

                    application.getCurrentUser().setStatus(inStatus.getText().toString());
                    application.update();

                    application.smackService.turnOn();

                    status = inStatus.getText().toString();

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            btStatusUpdate.setVisibility(View.GONE);
                        }
                    });

                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * Reinicia o campo nome e seu botao de checagem
     */
    private void resetInputNome() {

        // reinicia os campos de alteracao do nome
        inNome.setText(nome);
        btNomeUpdate.setVisibility(View.GONE);

    }

    /**
     * Reinicia campo status e seu botao de checagem
     */
    private void resetInputStatus() {
        // reinicia os campos de alteracao do nome
        inStatus.setText(status);
        btStatusUpdate.setVisibility(View.GONE);
    }

    public void setupActionBarOptions() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        toolbar.bringToFront();

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.hideKeyboard(Perfil.this);
                hideEmojiPopup();
                finish();
            }
        });
    }

    private void load() {

        TaskLight.start(new Transaction() {

            @Override
            public void init() {
            }

            @Override
            public Object perform() {

                if (application.getCurrentUser().getPhoto() != null)
                    return ImageService.byteToImage(application.getCurrentUser().getPhoto());
                else
                    return null;
            }

            @Override
            public void updateView(Object o) {

                Bitmap bitmap = (Bitmap) o;

                if (bitmap != null)
                    picture.setImageBitmap(bitmap);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.perfil, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.edit_picture:
                showChoosePictureDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showChoosePictureDialog() {

        if (showingEmoji)
            hideEmojiPopup();

        alertDialog = Dialogs.choosePictureCreate(Perfil.this, Perfil.this);
        alertDialog.show();
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

        if (id == NotificationCenter.emojiDidLoaded) {

            if (emojiView != null) {
                emojiView.invalidateViews();
                inStatus.setText(Emoji.replaceEmoji(application.getCurrentUser().getStatus(), inStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
            }
        }

    }

    @Override
    public void onSizeChanged(int height) {

        Rect localRect = new Rect();
        this.getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);

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

            inStatus.setFocusableInTouchMode(true);
            inStatus.requestFocus();

            if (emojiView == null) {

                if (this == null)
                    return;

                emojiView = new EmojiView(this);

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

                            CharSequence localCharSequence = Emoji.replaceEmoji(symbol, inStatus.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE);

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
                    windowLayoutParams.token = this.getWindow().getDecorView().getWindowToken();
                }
                windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }

            // muda imagem para keyboard
            btEmoji.setImageResource(R.drawable.ic_keyboard);

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
            btEmoji.setImageResource(R.drawable.ic_msg_panel_smiles);

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

        if (showingEmoji)
            showEmojiPopup(false);
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

            picture.setImageResource(R.drawable.ic_account_gray);
            application.getCurrentUser().setPhoto(null);

            // atualiza photo do usuario atual
            application.updatePhoto();
        }

        alertDialog.dismiss();
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
                    final boolean OK = resultCode == RESULT_OK;

                    Bitmap img = null, imgTb = null;

                    if (requestCode == Category.GALLERY && OK) {

                        if (data != null) {

                            img = ImageService.compress(Utils.getRealPath(Perfil.this, data.getData()));
                            imgTb = ImageService.makeThumb(Utils.getRealPath(Perfil.this, data.getData()));

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

                if (!Utils.isNetworkAvailable(getApplicationContext()))
                    Toast.makeText(getApplicationContext(), R.string.network_not_available, Toast.LENGTH_LONG).show();
                else if (imgs[0] == null || imgs[1] == null)
                    Toast.makeText(getApplicationContext(), R.string.error_load_photo, Toast.LENGTH_LONG).show();
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

                progressUploadPicture.setVisibility(View.VISIBLE);

                imageBytes = ImageService.imageToByte(image);
                imageThumbBytes = ImageService.imageToByte(imageThumb);

                // set photo to imageview
                picture.setImageBitmap(image);

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
                    Toast.makeText(getApplicationContext(), R.string.error_load_photo, Toast.LENGTH_LONG).show();

                progressUploadPicture.setVisibility(View.GONE);
            }
        });

    }
}
