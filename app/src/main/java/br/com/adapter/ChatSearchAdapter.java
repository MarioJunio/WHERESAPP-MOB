package br.com.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.br.wheresapp.R;
import br.com.dao.ContactDAO;
import br.com.model.ChatSearchItem;
import br.com.net.CheckUpdates;
import br.com.service.DateService;
import br.com.aplication.Application;

/**
 * Created by MarioJ on 13/10/15.
 */
public class ChatSearchAdapter extends RecyclerView.Adapter<ChatSearchAdapter.ViewHolder> {

    private List<ChatSearchItem> searchItems;
    private Application session;
    private ContactDAO contactDAO;

    // listener
    private View.OnClickListener clickListener;

    // VIEW TYPES
    private int NORMAL_ROW = 0;
    private int SEARCH_ROW = 1;

    public ChatSearchAdapter(Application session, ContactDAO contactDAO) {

        this.session = session;
        this.contactDAO = contactDAO;
        this.searchItems = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view;
        ViewHolder viewHolder;

        if (viewType == NORMAL_ROW)
            view = LayoutInflater.from(session.getApplicationContext()).inflate(R.layout.row_contacts, parent, false);
        else {
            view = LayoutInflater.from(session.getApplicationContext()).inflate(R.layout.message_row_search, parent, false);
            view.setOnClickListener(clickListener);
        }

        viewHolder = new ViewHolder(view);

        if (viewType == NORMAL_ROW)
            viewHolder.photo = (ImageView) view.findViewById(R.id.picture);

        viewHolder.name = (TextView) view.findViewById(R.id.name);
        viewHolder.message = (TextView) view.findViewById(R.id.message);
        viewHolder.tip = (TextView) view.findViewById(R.id.tip);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);
        ChatSearchItem item = searchItems.get(position);

        if (viewType == NORMAL_ROW) {

            // set contact name and photo whether necessary
            CheckUpdates.update(session, contactDAO, item.getContact(), holder, item.isType());
            holder.message.setText(item.getContact().getStatus());

        } else if (viewType == SEARCH_ROW) {

            // set contact name and photo whether necessary
            CheckUpdates.update(session, contactDAO, item.getMessage().getContact(), holder, item.isType());

            holder.message.setText(item.getMessage().getBody());
            holder.tip.setText(DateService.formatMessage(item.getMessage().getDate()));
        }


    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    @Override
    public int getItemViewType(int position) {

        ChatSearchItem chatSearchItem = searchItems.get(position);

        return chatSearchItem.isType() ? NORMAL_ROW : SEARCH_ROW;
    }

    public ChatSearchItem get(int position) {
        return searchItems.get(position);
    }

    public void clear() {
        searchItems.clear();
        notifyDataSetChanged();
    }

    public void set(List<ChatSearchItem> items) {

        for (ChatSearchItem item : items)
            add(item);

    }

    public void add(ChatSearchItem item) {
        searchItems.add(item);
        notifyItemInserted(searchItems.size() - 1);
    }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView photo;
        public TextView name, message, tip;

        public ViewHolder(View itemView) {
            super(itemView);
        }

    }
}
