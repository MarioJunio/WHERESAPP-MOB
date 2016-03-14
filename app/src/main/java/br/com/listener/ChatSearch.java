package br.com.listener;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;

import java.util.List;

import br.com.adapter.ChatSearchAdapter;
import br.com.adapter.ChatsAdapter;
import br.com.dao.ContactDAO;
import br.com.dao.MessageDAO;
import br.com.decoration.LineDividerRecyclerView;
import br.com.activities.Chat;
import br.com.model.ChatSearchItem;
import br.com.model.domain.Message;
import br.com.aplication.Application;
import br.com.aplication.App;
import com.br.wheresapp.R;

/**
 * Created by MarioJ on 13/10/15.
 */
public class ChatSearch implements SearchView.OnQueryTextListener {

    private static final String TAG = "ChatSearch";

    private Application session;
    private ContactDAO contactDAO;
    private MessageDAO messageDAO;
    private RecyclerView resultsView;
    private ChatSearchAdapter chatSearchAdapter;
    private Handler handler;

    public ChatSearch(Application session, ContactDAO contactDAO, MessageDAO messageDAO, RecyclerView resultsView) {
        this.session = session;
        this.contactDAO = contactDAO;
        this.messageDAO = messageDAO;
        this.resultsView = resultsView;
        this.handler = new Handler();
    }

    public void init() {

        chatSearchAdapter = new ChatSearchAdapter(session, contactDAO);

        resultsView.setLayoutManager(new LinearLayoutManager(session.getApplicationContext()));
        resultsView.addItemDecoration(new LineDividerRecyclerView(session.getApplicationContext(), R.drawable.list_divider_chat_search));
        resultsView.setItemAnimator(new DefaultItemAnimator());

        resultsView.setAdapter(chatSearchAdapter);

        chatSearchAdapter.setClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                int position = resultsView.getChildAdapterPosition(v);

                ChatSearchItem item = chatSearchAdapter.get(position);

                if (!item.isType()) {

                    Message me = item.getMessage();

                    // Instance new intent
                    Intent i = new Intent(session, Chat.class);
                    i.putExtra("contact", me.getContact());
                    i.putExtra("is_read", me.isRead());
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    session.startActivity(i);
                }
            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        if (query != null && !query.isEmpty())
            search(query);

        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {

        if (query != null && !query.isEmpty())
            search(query);

        return false;
    }

    private void search(final String query) {

        App.runBackgroundService(new Runnable() {
            @Override
            public void run() {

                final List<ChatSearchItem> queryResults = messageDAO.getMessages(query.toLowerCase());

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (resultsView.getAdapter() instanceof ChatsAdapter)
                            init();

                        chatSearchAdapter.clear();
                        chatSearchAdapter.set(queryResults);
                    }
                });

            }
        });

    }

}
