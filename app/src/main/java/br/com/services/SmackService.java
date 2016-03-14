package br.com.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.iqlast.LastActivityManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.binder.LocalBinder;
import br.com.net.Xmpp;
import br.com.receivers.NetworkReceiver;
import br.com.service.DateService;
import br.com.smack.MessageNetworkListener;
import br.com.smack.Smack;
import br.com.util.Utils;

/**
 * Created by MarioJ on 25/01/16.
 */
public class SmackService extends Service implements ConnectionListener, InvitationListener, NetworkReceiver.NetworkStateReceiverListener, StanzaListener, ReceiptReceivedListener {

    private static final String TAG = "SmackService";
    /**
     * Sessao atual do usuario
     */
    private Application application;

    /**
     * Conexão com o ejabber openfire
     */
    private XMPPTCPConnection connection;

    /**
     * Usuario e senha do ejabber openfire
     */
    private String username, password;

    /**
     * Gerencie os pacotes recebidos
     */
    private DeliveryReceiptManager receiptManager;

    /**
     * Trata eventos de conexão com a internet
     */
    private NetworkReceiver networkReceiver;
    private boolean connected;

    /**
     * Observadores de presenças
     */
    private SubjectPresence presenceObservable;


    @Override
    public void onCreate() {
        super.onCreate();

        networkReceiver = new NetworkReceiver();
        presenceObservable = new SubjectPresence();

        try {
            Class.forName("org.jivesoftware.smack.ReconnectionManager");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        this.application = (Application) getApplication();
        this.username = application.getXmppUser();
        this.password = Utils.genKey(application.getCurrentUser().getDdi(), application.getCurrentUser().getPhone());

        // configura xmpp tcp connection
        try {
            xmppInitialise();
//            connect();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        networkReceiver.addListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");

        registerReceiver(networkReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder<SmackService>(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        App.runBackgroundService(new Runnable() {
//            @Override
//            public void run() {
//                connection.disconnect();
//            }
//        });
//        unregisterReceiver(networkReceiver);
    }

    @Override
    public void connected(XMPPConnection connection) {

        // se a conexão nao estiver logada, entao faça
        if (!connection.isAuthenticated())
            doLogin();

        // trata pacotes de entrada
        addAsyncStanzaListener(this, null);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {

        try {
            addMessageListener(new MessageNetworkListener(getApplicationContext()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {

    }

    @Override
    public void reconnectionSuccessful() {

    }

    @Override
    public void reconnectingIn(int seconds) {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }

    @Override
    public void invitationReceived(XMPPConnection conn, MultiUserChat room, String inviter, String reason, String password, Message message) {

    }

    @Override
    public void processPacket(final Stanza packet) {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                try {

                    if (packet instanceof Presence) {

                        if (packet.getFrom().startsWith(application.getXmppUser()))
                            return;

                        final Presence received = (Presence) packet;

                        Presence.Type presenceType = received.getType();

                        // notifica os observador que houve mudança na presença
                        if (presenceType == Presence.Type.available || presenceType == Presence.Type.unavailable) {

                            presenceObservable.change();
                            presenceObservable.notifyObservers(received);

                            Log.d(TAG, "AVAILABLE OR UNAVAILABLE FROM " + received.getFrom());

                        } else if (presenceType == Presence.Type.subscribe) {

                            Presence subscribed = new Presence(Presence.Type.subscribe);
                            subscribed.setTo(received.getFrom());
                            connection.sendStanza(subscribed);

                            Log.d(TAG, "SUBSCRIBE FROM " + received.getFrom());

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    public void onReceiptReceived(String fromJid, String toJid, String receiptId, Stanza receipt) {
        Log.d(TAG, "[onReceiptReceived] -> FROM JID: " + fromJid + ", TO JID: " + toJid + ", ReceiptID: " + receiptId + ", Recepted: " + receipt.getStanzaId());
    }

    @Override
    public void networkAvailable() {

        try {

            if (!connected && Utils.isNetworkAvailable(getApplicationContext())) {

                // reinicia objeto de conexão com ejabber
                xmppInitialise();

                // tenta se conectar
                connect();

                // altera status da conexão
                connected = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnavailable() {

        if (connected)
            connected = false;
    }

    /**
     * Inicializa os objetos de conexão com o servidor ejabber
     */
    private void xmppInitialise() throws KeyManagementException, NoSuchAlgorithmException {

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setServiceName(Xmpp.TAG);
        config.setHost(Xmpp.HOST);
        config.setPort(Xmpp.PORT);
        config.setDebuggerEnabled(false);

        TLSUtils.acceptAllCertificates(config);
        TLSUtils.disableHostnameVerificationForTlsCertificicates(config);

        SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");

        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);

        connection = new XMPPTCPConnection(config.build());
        connection.addConnectionListener(this);

        receiptManager = DeliveryReceiptManager.getInstanceFor(connection);
        receiptManager.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
        receiptManager.addReceiptReceivedListener(this);
    }

    /**
     * Conecta no servidor ejabber
     */
    private void connect() {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                if (connection.isConnected())
                    return;

                try {
                    connection.connect();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * Realiza login no servidor ejabber
     */
    private void doLogin() {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                if (connection.isAuthenticated())
                    return;

                try {
                    connection.login(username, password);
                    turnOn();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /**
     * Adiciona listener para capturar pacotes de entrada e filtra-los antes de serem processados
     *
     * @param stanzaFilter   filtra pacotes de entrada antes de serem processados
     * @param stanzaListener captura e processa pacotes de entrada
     */
    public void addAsyncStanzaListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter) {
        connection.addAsyncStanzaListener(stanzaListener, stanzaFilter);
    }

    /**
     * Obtem o Roster
     *
     * @return Instancia do roster
     */
    public Roster getRoster() {
        return Roster.getInstanceFor(connection);
    }

    public Date getLastActivity(String ddi, String phone) throws Exception {
        return lastActivity(Smack.parseContact(ddi, phone));
    }

    public Date getLastActivity(String jid) throws Exception {
        return lastActivity(jid);
    }

    private Date lastActivity(String jid) throws Exception {

        if (!isOnline())
            throw new Exception("Conexão Smack offline");

        LastActivity lastActivity = LastActivityManager.getInstanceFor(connection).getLastActivity(jid);

        Date idleTime;

        if (lastActivity != null) {

            long idle = lastActivity.getIdleTime() * 1000;

            idleTime = (idle <= 0) ? null : new Date(DateService.getTimeNow().getTime() - idle);

        } else
            throw new Exception("Instancia de LastActivity não criada !");

        return idleTime;

    }

    public void subscribe(String ddi, String phone) throws SmackException.NotConnectedException {

        Presence subscribe = new Presence(Presence.Type.subscribe);
        subscribe.setTo(Smack.parseContact(ddi, phone));

        connection.sendStanza(subscribe);
    }

    public void unsubscribe(String jid) throws SmackException.NotConnectedException {

        Presence unsubscribe = new Presence(Presence.Type.unsubscribe);
        unsubscribe.setTo(jid);

        connection.sendStanza(unsubscribe);
    }

    public boolean isOnline() {

        Log.d(TAG, "Connected: " + connection.isConnected() + " - Authenticated: " + connection.isAuthenticated());

        return connection.isConnected() && connection.isAuthenticated();
    }

    public void updateName(final String name) throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {

        // espera até se conectar e autenticar
        waitUntilOnline();

        VCardManager vCardManager = VCardManager.getInstanceFor(connection);
        VCard vCard = vCardManager.loadVCard();
        vCard.setFirstName(name);
        vCard.setNickName(name);
        vCardManager.saveVCard(vCard);
    }

    public boolean sendMessage(Message messageSmack) {

        try {
            DeliveryReceiptRequest.addTo(messageSmack);
            connection.sendStanza(messageSmack);
            return true;
        } catch (SmackException.NotConnectedException e) {
            return false;
        }
    }

    public void addMessageListener(final ChatMessageListener chatMessageListener) throws InterruptedException {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                // obtem instancia do chat
                ChatManager chatManager = ChatManager.getInstanceFor(connection);

                // adiciona chat message listener
                chatManager.addChatListener(new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean createdLocally) {
                        chat.addMessageListener(chatMessageListener);
                    }
                });

            }
        });

    }

    public void removeMessageListener(final ChatMessageListener chatMessageListener) {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                // obtem instancia do chat
                ChatManager chatManager = ChatManager.getInstanceFor(connection);

                // remove chat message listener
                chatManager.removeChatListener(new ChatManagerListener() {

                    @Override
                    public void chatCreated(Chat chat, boolean createdLocally) {
                        chat.removeMessageListener(chatMessageListener);
                    }
                });

            }
        });

    }

    /**
     * Aguarda conexão com o ejabber e autenticação do usuario local
     */
    public void waitUntilOnline() {

        int count = 0;

        while (!isOnline()) {

            try {
                Log.d(TAG, "[WAITING ONLINE]");

                Thread.sleep(App.SLEEP_TIME);
                count++;

                if (count == 5) {

                    // tenta se conectar novamente
                    connected(connection);

                    // reinicia contador
                    count = 0;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public LastActivityManager getLastActivityManager() {
        return LastActivityManager.getInstanceFor(connection);
    }

    /**
     * Adiciona objeto observador ao objeto observavél de presença
     *
     * @param observer observador para ser adicionado
     */
    public void addPresenceObserver(Observer observer) {
        presenceObservable.addObserver(observer);
    }

    /**
     * Remove objeto observador do objeto observavél de presença
     *
     * @param observer observador a ser removido
     */
    public void removePresenceObserver(Observer observer) {
        presenceObservable.deleteObserver(observer);
    }

    public void turnOn() throws SmackException.NotConnectedException {

        Log.d(TAG, "Meu Status: " + application.getCurrentUser().getStatus());

        // altera presença ao se autenticar
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(application.getCurrentUser().getStatus());
        connection.sendStanza(presence);
    }

    private class SubjectPresence extends Observable {

        public void change() {
            setChanged();
        }
    }
}
