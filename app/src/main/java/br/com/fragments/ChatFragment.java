package br.com.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.br.wheresapp.R;

import org.jivesoftware.smack.packet.Presence;

import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import br.com.activities.Chat;
import br.com.adapter.ChatAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.MessageDAO;
import br.com.model.domain.Contact;
import br.com.model.domain.Message;
import br.com.notifications.MessageNotification;
import br.com.observers.ChatObserver;
import br.com.providers.MessageContract;
import br.com.service.DateService;
import br.com.service.ImageService;
import br.com.smack.Smack;
import br.com.util.AndroidUtilities;
import br.com.util.Constants;
import br.com.util.NotificationCenter;
import br.com.widgets.Emoji;
import br.com.widgets.EmojiView;
import br.com.widgets.SizeNotifierRelativeLayout;

public class ChatFragment extends Fragment implements SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate, NotificationCenter.NotificationCenterDelegate, Observer {

    static final String TAG = "ChatFragment";

    // viewholder para gerenciar a ActionBar
    private ActionBarHolder actionBarHolder;

    // recyclerview adapter
    private ChatAdapter chatAdapter;

    // widgets do layout
    private RecyclerView messages;
    private ImageButton emojiButton;
    private EditText textMessage;
    private ImageView btSend;

    // gerenciadores
    private Application application;
    private Contact contact;
    private MessageDAO messageDAO;
    private ChatObserver messagesObserver;
    private EmojiView emojiView;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private WindowManager.LayoutParams windowLayoutParams;
    private Handler handler;

    // propriedades utilitarias
    private boolean showingEmoji;
    private int keyboardHeight;
    private boolean keyboardVisible;

    // ringtone ao enviar mensagem pela rede
    MediaPlayer mpRingtone;

    public ChatFragment() {
    }

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    private final TextWatcher textMessageWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            if (textMessage.getText().toString().isEmpty())
                btSend.setVisibility(View.GONE);
            else
                btSend.setVisibility(View.VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // instance handler to UI
        handler = new Handler();

        application = (Application) getActivity().getApplicationContext();

        // cria dao para messagens
        messageDAO = MessageDAO.instance(getActivity().getApplicationContext());

        // cria adaptador para as conversas
        chatAdapter = new ChatAdapter(getActivity().getApplicationContext());

        // instancia ringtone para reproduzir o audio
        mpRingtone = MediaPlayer.create(getContext(), R.raw.ringtone_pop);

        // pega contato
        contact = ((Chat) getActivity()).getContact();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AndroidUtilities.statusBarHeight = App.getStatusBarHeight(getContext());

        // cria e configura a actionbar
        createActionBar();

        // add presence observer
        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                while (application.smackService == null) {

                    try {
                        Thread.sleep(App.SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // add presence observer
                application.smackService.addPresenceObserver(ChatFragment.this);

            }
        });

        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) view.findViewById(R.id.chat_layout);
        messages = (RecyclerView) view.findViewById(R.id.list_messages);
        emojiButton = (ImageButton) view.findViewById(R.id.emojiButton);
        textMessage = (EditText) view.findViewById(R.id.text_message);
        btSend = (ImageView) view.findViewById(R.id.bt_send);

        // configura layout do recyclerview para as mensagens
        messages.setLayoutManager(new LinearLayoutManager(getContext()));
        messages.setItemAnimator(new DefaultItemAnimator());

        // configura adapter para o recyclerview que exibe mensagens
        messages.setAdapter(chatAdapter);

        // observador de mensagens
        messagesObserver = new ChatObserver(contact, messageDAO, handler, messages, chatAdapter);

        // manipula teclas no fragment
        view.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == event.KEYCODE_BACK && showingEmoji)
                    hideEmojiPopup();
                else if (keyCode == event.KEYCODE_BACK)
                    getActivity().finish();

                return false;
            }
        });

        // configura watcher para o botao enviar
        textMessage.addTextChangedListener(textMessageWatcher);

        textMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (showingEmoji)
                    hideEmojiPopup();
            }
        });

        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messages.scrollToPosition(chatAdapter.getItemCount() - 1);
                showEmojiPopup(!showingEmoji);
            }
        });

        btSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String message = textMessage.getText().toString();

                if (message.isEmpty())
                    return;

                final Message m = Message.create(contact.getDdi(), contact.getPhone(), DateService.getTimeNow(), Message.Delivery.PENDING, true, message);

                // send and save owner message
                send(m);

                // clear text field
                textMessage.setText("");
            }
        });

        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) view.findViewById(R.id.chat_layout);
        sizeNotifierRelativeLayout.delegate = this;

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        // carrega e prepara as mensagens para exibição
        prepareMessages();

        // registra observador de mensagens
        getActivity().getContentResolver().registerContentObserver(MessageContract.CONTENT_URI, true, messagesObserver);

        // visto por ultimo
        setupLastActivity();

        // remove notificação
        if (contact != null)
            MessageNotification.cancelNotification(getContext());
    }

    /**
     * Carrega a actionbar configurando seus widgets
     */
    private void createActionBar() {

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.chat_toolbar);
        ImageView photo = (ImageView) toolbar.findViewById(R.id.photo);
        TextView title = (TextView) toolbar.findViewById(R.id.title);
        TextView subtitle = (TextView) toolbar.findViewById(R.id.subtitle);

        actionBarHolder = new ActionBarHolder(photo, title, subtitle);

        title.setText(contact.getName());

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (contact.getPhoto() != null)
            photo.setImageBitmap(ImageService.byteToImage(contact.getPhoto()));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    /**
     * Prepara as mensagens para serem exibidas ao usuario, e verifica se há alguma mensagem não lida para marcar como lida
     */
    private void prepareMessages() {

        List<Message> loadedMessages = messageDAO.getMessages(contact.getDdi(), contact.getPhone());

        // entrega as mensagens carregadas ao adaptador de mensagens
        chatAdapter.setup(loadedMessages);

        // move a lista para seu fim, para mostrar a ultima mensagem enviada ou recebida
        messages.scrollToPosition(chatAdapter.getItemCount() - 1);
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
                            actionBarHolder.getSubtitle().setText(finalStatus);

                        }
                    });

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Persiste a mensagem enviada no banco de dados loca, e notifica o observador, para ser enviada pela rede ao destinatario
     *
     * @param message mensagem a ser persistida
     */
    private void send(final Message message) {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                // instancia objeto mensagem do Smack
                org.jivesoftware.smack.packet.Message messageSmack = new org.jivesoftware.smack.packet.Message(Smack.parseContact(message.getDdi(), message.getPhone()), org.jivesoftware.smack.packet.Message.Type.chat);
                messageSmack.setStanzaId(String.valueOf(message.getId()));
                messageSmack.setBody(message.getBody());

                // mensagem ja lida por padrao
                message.setRead(true);

                // se a mensagem for enviada pela rede, altera o status para 'submetido' e reproduz o ringtone, caso contrario altera o icone do balao de mensagem para aguardando rede
                if (application.smackService.sendMessage(messageSmack)) {
                    message.setDelivery(Message.Delivery.SUBMITED);
                    mpRingtone.start();
                } else {
                    message.setDelivery(Message.Delivery.PENDING);
                    Log.i(TAG, "Mensagem não enviada !");
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        chatAdapter.add(message);
                        messages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });

                messageDAO.insert(message);

                Log.d(TAG, "Mensagem " + message.toString() + " inserida");
            }
        });
    }

    /**
     * Show or hide the emoji popup
     *
     * @param show
     */
    public void showEmojiPopup(boolean show) {

        showingEmoji = show;

        if (show) {

            if (emojiView == null) {

                if (getActivity() == null) {
                    return;
                }

                emojiView = new EmojiView(getActivity());

                emojiView.setListener(new EmojiView.Listener() {
                    public void onBackspace() {
                        textMessage.dispatchKeyEvent(new KeyEvent(0, 67));
                    }

                    public void onEmojiSelected(String symbol) {

                        int i = textMessage.getSelectionEnd();
                        if (i < 0) {
                            i = 0;
                        }
                        try {
                            CharSequence localCharSequence = Emoji.replaceEmoji(symbol, textMessage.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20));
                            textMessage.setText(textMessage.getText().insert(i, localCharSequence));
                            int j = i + localCharSequence.length();
                            textMessage.setSelection(j, j);
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

            // muda para icone de teclado
            emojiButton.setImageResource(R.drawable.ic_keyboard);

            final int currentHeight;

            if (keyboardHeight <= 0)
                keyboardHeight = Application.getInstance().getSharedPreferences("emoji", 0).getInt("kbd_height", AndroidUtilities.dp(200));

            currentHeight = keyboardHeight;

            WindowManager wm = (WindowManager) Application.getInstance().getSystemService(Activity.WINDOW_SERVICE);

            windowLayoutParams.height = currentHeight;
            windowLayoutParams.width = AndroidUtilities.displaySize.x;

            try {
                if (emojiView.getParent() != null) {
                    wm.removeViewImmediate(emojiView);
                }
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
                if (sizeNotifierRelativeLayout != null)
                    sizeNotifierRelativeLayout.setPadding(0, 0, 0, currentHeight);

                return;
            }

        } else {

            removeEmojiWindow();

            // muda icone para emoticon
            emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);

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


    /**
     * Remove emoji window
     */
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


    /**
     * Updates emoji views when they are complete loading
     *
     * @param id
     * @param args
     */
    @Override
    public void didReceivedNotification(int id, Object... args) {

        if (id == NotificationCenter.emojiDidLoaded) {

            if (emojiView != null) {
                emojiView.invalidateViews();
            }

            if (messages != null) {
                messages.invalidate();
            }

            chatAdapter.notifyDataSetChanged();
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

        if (keyboardVisible)
            messages.scrollToPosition(chatAdapter.getItemCount() - 1);

        if (keyboardVisible && sizeNotifierRelativeLayout.getPaddingBottom() > 0) {
            showEmojiPopup(false);
        } else if (!keyboardVisible && keyboardVisible != oldValue && showingEmoji) {
            showEmojiPopup(false);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getActivity().getContentResolver().unregisterContentObserver(messagesObserver);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
    }

    @Override
    public void onPause() {
        super.onPause();

        // desfoca esta janela
        application.setChatContact(null);

        // esconde a barra de emoji
        hideEmojiPopup();
    }

    @Override
    public void onStop() {
        super.onStop();

        // remove observador de presença para este chat
        application.smackService.removePresenceObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {

        final Presence presence = (Presence) data;

        handler.post(new Runnable() {

            @Override
            public void run() {

                String la = presence.isAvailable() ? getResources().getString(R.string.online) : DateService.formatLastActiviy(new Date());

                actionBarHolder.getSubtitle().setText(la);
            }
        });

    }

    public class ActionBarHolder {

        public ImageView photo;
        public TextView title;
        public TextView subtitle;

        public ActionBarHolder(ImageView photo, TextView title, TextView subtitle) {
            this.photo = photo;
            this.title = title;
            this.subtitle = subtitle;
        }

        public ImageView getPhoto() {
            return photo;
        }

        public void setPhoto(ImageView photo) {
            this.photo = photo;
        }

        public TextView getTitle() {
            return title;
        }

        public void setTitle(TextView title) {
            this.title = title;
        }

        public TextView getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(TextView subtitle) {
            this.subtitle = subtitle;
        }
    }
}
