package br.com.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.model.domain.Message;
import br.com.net.CheckUpdates;
import br.com.service.DateService;
import br.com.widgets.Emoji;

/**
 * Created by MarioJ on 04/03/15.
 */
public class ChatsAdapter extends RecyclerView.Adapter<ViewHolderSimple> {

    private final String TAG = "ChatsAdapter";

    private List<Message> messages;
    private Application session;
    private Handler handler;
    private int resource;
    private HashMap<String, Integer> map = new HashMap<>();
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;

    public ChatsAdapter(Application session, int resource) {

        this.messages = new ArrayList<>();
        this.session = session;
        this.resource = resource;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void set(List<Message> messages) {

        for (Message m : messages)
            add(m);
    }

    public Integer get(String place) {
        return map.get(place);
    }

    public Message get(int position) {
        return messages.get(position);
    }

    public void newMessage(Message message) {

        Integer index = get(key(message));

        if (index != null)
            update(message, index);
        else
            add(message);

    }

    public void markUnread(int position) {
        notifyItemChanged(position);
    }

    /**
     * Atualiza a ultima mensagem recebida do contato
     * @param message ultima mensagem recebida
     * @param position posicao do contato
     */
    private void update(Message message, int position) {

        // get message from adapter to update object
        Message getMessage = messages.get(position);
        getMessage.setBody(message.getBody());
        getMessage.setRead(message.isRead());

        // atualiza a view
        notifyItemChanged(position);
    }

    public void clear() {

        int size = getItemCount();

        messages.clear();
        map.clear();

        // atualiza a view
        notifyItemRangeRemoved(0, size);
    }

    public void add(Message message) {

        messages.add(message);
        map.put(key(message), getItemCount() - 1);

        // atualiza a view
        notifyItemInserted(getItemCount() - 1);
    }

    private void addNewChat(Message message) {

        messages.add(message);
        map.put(key(message), getItemCount() - 1);
    }

    public void delete(int position) {

        Message message = messages.get(position);
        messages.remove(position);
        map.remove(key(message));

        notifyItemRemoved(position);
    }

    private String key(Message message) {
        return message.getContact().getDdi() + message.getContact().getPhone();
    }

    @Override
    public ViewHolderSimple onCreateViewHolder(ViewGroup parent, int viewType) {

        // instancia o layout
        View view = LayoutInflater.from(session.getApplicationContext()).inflate(resource, parent, false);

        // associa evento de click a view
        view.setOnClickListener(onClickListener);

        // cria o manipulador do layout
        ViewHolderSimple viewHolderSimple = null;

        if (view != null) {
            viewHolderSimple = new ViewHolderSimple(view);
            viewHolderSimple.setOnLongClickListener(onLongClickListener);
        }

        return viewHolderSimple;
    }

    @Override
    public void onBindViewHolder(ViewHolderSimple holder, final int position) {

        // checa se o manipulador do layout foi criado anteriormente
        if (holder != null) {

            // pega mensagem da lista na posicao especificada
            final Message message = messages.get(position);

            try {
                setMessageAndTip(message, holder);
                CheckUpdates.update(session, message.getContact(), holder);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!message.isRead())
                unread(holder);
        }

    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    private void setMessageAndTip(Message message, ViewHolderSimple viewHolder) {
        viewHolder.message.setText(Emoji.replaceEmoji(message.getBody(), viewHolder.message.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
        viewHolder.tip.setText(DateService.formatMessage(message.getDate()));

        if (message.isRead())
            read(viewHolder);
        else
            unread(viewHolder);
    }

    private void unread(ViewHolderSimple holder) {
        holder.message.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        holder.tip.setTextColor(Color.parseColor("#5e7703"));
        holder.tip.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    }

    private void read(ViewHolderSimple holder) {
        holder.message.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        holder.tip.setTextColor(Color.parseColor("#757575"));
        holder.tip.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
    }

}