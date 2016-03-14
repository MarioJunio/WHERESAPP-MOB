package br.com.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.br.wheresapp.R;

import java.io.Serializable;
import java.util.List;

import br.com.activities.Chat;
import br.com.activities.Perfil;
import br.com.adapter.ChatSearchAdapter;
import br.com.adapter.ChatsAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.ContactDAO;
import br.com.dao.MessageDAO;
import br.com.decoration.LineDividerRecyclerView;
import br.com.listener.ChatSearch;
import br.com.model.domain.Message;
import br.com.observers.ChatsObserver;
import br.com.providers.MessageContract;
import br.com.util.AndroidUtilities;
import br.com.util.NotificationCenter;
import br.com.widgets.EmojiView;
import br.com.widgets.PopupChats;

/**
 * Created by MarioJ on 03/03/15.
 */
public class ChatsFragment extends Fragment implements Serializable, NotificationCenter.NotificationCenterDelegate {

    private final String TAG = "ChatsFragment";

    private ChatsAdapter chatsAdapter;
    private ChatsObserver chatsObserver;
    private RecyclerView chatsView;
    private MessageDAO messageDAO;
    private Application session;
    private Handler handler;
    private ChatSearch chatSearch;
    private FloatingActionButton fabNewMessage;

    private EmojiView emojiView;

    private View emptyStateView;

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        session = (Application) getActivity().getApplication();
        messageDAO = MessageDAO.instance(getActivity().getApplicationContext());
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        AndroidUtilities.statusBarHeight = App.getStatusBarHeight(getContext());

        emptyStateView = view.findViewById(R.id.empty_state);

        chatsView = (RecyclerView) view.findViewById(R.id.chats_view);
        chatsView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatsView.addItemDecoration(new LineDividerRecyclerView(getContext(), R.drawable.list_divider_chats));
        chatsView.setItemAnimator(new DefaultItemAnimator());

        fabNewMessage = (FloatingActionButton) view.findViewById(R.id.fab_chats);

        chatsAdapter = new ChatsAdapter(session, R.layout.row_chats);

        chatsView.setAdapter(chatsAdapter);

        // register menu options for context menu
        registerForContextMenu(chatsView);

        // instance observer for these chats
        chatsObserver = new ChatsObserver(session, new Handler(), messageDAO, chatsAdapter);

        // create chat search
        chatSearch = new ChatSearch(session, ContactDAO.instance(getContext()), messageDAO, chatsView);

        // init listeners
        listener();

        emojiView = new EmojiView(getActivity());

        // register observer to chats
        getActivity().getContentResolver().registerContentObserver(MessageContract.CONTENT_URI, true, chatsObserver);

        // registra ao observador para carregar os emojis
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();

        // carrega conversas
        prepareChats();

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.chats, menu);

        if (chatsView.getAdapter() instanceof ChatSearchAdapter)
            initMessagesAdapter();

        MenuItem item = menu.findItem(R.id.search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint("Buscar mensagem...");

        searchView.setOnQueryTextListener(chatSearch);

        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                session.counterAndShow();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                searchView.setQuery(null, false);
                initMessagesAdapter();
                return true;
            }
        });

        MenuItemCompat.collapseActionView(item);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.add_user:
                addContact();
                break;
            case R.id.edit_perfil:
                startActivity(new Intent(getActivity(), Perfil.class));

        }

        return super.onOptionsItemSelected(item);
    }

    private void addContact() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        startActivity(intent);
    }

    private void prepareChats() {

        App.runBackgroundService(new Runnable() {

            @Override
            public void run() {

                final List<Message> messages = messageDAO.getConversations();

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        toogleEmptyState(messages.isEmpty());

                        chatsAdapter.clear();
                        chatsAdapter.set(messages);
                    }
                });
            }
        });

    }

    private void listener() {

        // Quando uma chat, for selecionado
        chatsAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                session.counterAndShow();

                // get position at view
                int position = chatsView.getChildAdapterPosition(v);

                // Get message at position in the array
                Message me = chatsAdapter.get(position);
                boolean oldRead = me.isRead();

                // change to read
                me.setRead(true);

                // Instance contact
                Intent i = new Intent(getActivity(), Chat.class);
                i.putExtra("contact", me.getContact());
                i.putExtra("is_read", oldRead);

                startActivity(i);

                // atualiza viewholder
                chatsAdapter.notifyItemChanged(position);

            }
        });

        // ao segura o toque no chat
        chatsAdapter.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {

                session.counterAndShow();

                Context wrapper = new ContextThemeWrapper(getContext(), R.style.PopupMenu);

                PopupChats popupChats = new PopupChats(wrapper, v, Gravity.CENTER);
                popupChats.inflate();

                popupChats.setClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        // posicao da conversa selecionada
                        int position = chatsView.getChildAdapterPosition(v);

                        // numero do item de menu clicado
                        char shortchut = item.getNumericShortcut();

                        if (shortchut == getResources().getString(R.string.open_perfil_code).charAt(0)) {

                            //TODO: implementar tela de perfil
                            Toast.makeText(getContext(), "Desenvolvimento", Toast.LENGTH_LONG).show();

                        } else if (shortchut == getResources().getString(R.string.clear_chat_code).charAt(0)) {

                            // pega a mensagem na posicao
                            Message message = chatsAdapter.get(position);

                            Log.d(TAG, "Apagar mensagens de " + message.getContact().getDdi() + message.getContact().getPhone());

                            // exclui mensagens do numero
                            int rows = messageDAO.delete(message.getContact().getDdi(), message.getContact().getPhone());

                            // apaga conversa da lista
                            if (rows > 0)
                                chatsAdapter.delete(position);

                            toogleEmptyState(chatsAdapter.getItemCount() <= 0);

                        } else if (shortchut == getResources().getString(R.string.mark_unread_code).charAt(0)) {

                            Message message = chatsAdapter.get(position);

                            int rows = messageDAO.unread(message.getId());

                            if (rows > 0) {
                                message.setRead(false);
                                chatsAdapter.markUnread(position);
                            }

                        } else if (shortchut == getResources().getString(R.string.send_to_email_code).charAt(0)) {
                            //TODO: implementar metodo para enviar as mensagens do numero via email
                            Toast.makeText(getContext(), "Desenvolvimento", Toast.LENGTH_LONG).show();
                        }

                        return false;
                    }
                });

                popupChats.show();

                return true;
            }
        });

        fabNewMessage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ((DrawerLayout) getActivity().findViewById(R.id.drawer_applicaton)).openDrawer(Gravity.START);
            }
        });

    }

    private void initMessagesAdapter() {
        chatsView.setAdapter(chatsAdapter);
        listener();
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

        if (id == NotificationCenter.emojiDidLoaded) {

            emojiView.invalidateViews();

            // atualiza views
            chatsAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        getActivity().getContentResolver().unregisterContentObserver(chatsObserver);
    }

    private void toogleEmptyState(boolean empty) {

        if (empty)
            emptyStateView.setVisibility(View.VISIBLE);
        else
            emptyStateView.setVisibility(View.GONE);
    }
}
