package br.com.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.br.wheresapp.R;

import java.util.ArrayList;
import java.util.List;

import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.model.domain.Contact;
import br.com.net.CheckUpdates;
import br.com.resources.Contacts;
import br.com.util.Utils;
import br.com.widgets.Emoji;

/**
 * Created by MarioJ on 08/03/15.
 */
public class ContactsAdapter extends RecyclerView.Adapter {

    private final String TAG = "ContactsAdapter";

    private Application application;
    private Activity activity;
    private Contacts contacts;
    private int resource;
    private List<Contact> listContacts;

    // ouvintes de eventos de click
    private View.OnClickListener onClickListener, btRefreshClickListener;

    // VIEW TYPES
    private int HEADER = 1;
    private int CONTACT = 2;
    private static final int EMPTY_STATE = 3;

    public ContactsAdapter(Application application, Activity activity, Contacts contacts, int resource) {
        this.application = application;
        this.activity = activity;
        this.contacts = contacts;
        this.resource = resource;
        this.listContacts = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder = null;

        if (viewType == HEADER) {

            // cria layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_header_contacts, parent, false);

            // instancia ViewHolder
            viewHolder = new HeaderViewHolder(view);

        } else if (viewType == EMPTY_STATE) {

            // cria layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_contacts_empty_state, parent, false);

            // instancia view holder
            viewHolder = new EmptyStateViewHolder(view);

        } else {

            // cria layout
            View view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
            view.setOnClickListener(onClickListener);

            // instancia ViewHolder
            viewHolder = new ContactViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (getItemViewType(position) == HEADER) {

            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;

            headerViewHolder.inSearch.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    if (headerViewHolder.inSearch.getText().toString().isEmpty())
                        Utils.hideKeyboard(activity);

                    contacts.search(headerViewHolder.inSearch.getText().toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            // bt refresh click listener
            headerViewHolder.btRefresh.setOnClickListener(btRefreshClickListener);

        } else if (getItemViewType(position) == EMPTY_STATE) {



        } else {

            try {

                ContactViewHolder contactViewHolder = (ContactViewHolder) holder;

                Contact contact = listContacts.get(position);

                // textview de presença
                setStatusView(contactViewHolder, contact.getPresence());

                // textview de status
                contactViewHolder.message.setText(Emoji.replaceEmoji(contact.getStatus(), contactViewHolder.message.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));

                // set name and photo thumb
                CheckUpdates.update(application, contact, (ContactViewHolder) holder);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public long getItemId(int position) {
        return listContacts.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return listContacts.size();
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0)
            return HEADER;
        else if (position == getItemCount() - 1)
            return EMPTY_STATE;
        else
            return CONTACT;
    }

    public void setStatusView(ContactViewHolder contactViewHolder, String status) {

        if (contactViewHolder != null) {

            contactViewHolder.tip.setText(status);

            if (status.equals(Contact.StatusType.ONLINE.name()))
                contactViewHolder.tip.setTextColor(Color.parseColor(App.Colors.LIGHT_GREEN.getCode()));
            else if (status.equals(Contact.StatusType.OFFLINE.name()))
                contactViewHolder.tip.setTextColor(Color.parseColor(App.Colors.LIGHT_RED.getCode()));
            else if (status.equals(Contact.StatusType.NETWORK_UNAVAILABLE.name())) {
                contactViewHolder.tip.setText(application.getString(R.string.out_network));
                contactViewHolder.tip.setTextColor(Color.parseColor(App.Colors.GRAY.getCode()));
            } else
                contactViewHolder.tip.setTextColor(Color.parseColor(App.Colors.GRAY.getCode()));
        }
    }

    public int getPosition(String number) {

        for (int i = 1; i < getItemCount(); i++) {

            Contact c = listContacts.get(i);

            if ((c.getDdi() + c.getPhone()).equals(number))
                return i;
        }

        return -1;
    }

    public int getPosition(long id) {

        for (int i = 1; i < getItemCount(); i++) {

            Contact c = listContacts.get(i);

            if (c.getId() == id)
                return i;

        }

        return -1;
    }

    public void add(Contact c, boolean update) {

//        listContacts.add(c);

        if (getItemCount() <= 0)
            listContacts.add(c);
        else
            listContacts.add(getItemCount() - 1, c);

        if (update) {

            if (getItemCount() <= 0)
                notifyItemInserted(listContacts.size() - 1);
            else
                notifyItemInserted(listContacts.size() - 2);
        }
    }

    public void remove(long id) {

        int position = getPosition(id);

        listContacts.remove(position);
        notifyItemRemoved(position);
    }

    public void remove(int position) {

        listContacts.remove(position);
        notifyItemRemoved(position);
    }

    public void set(List<Contact> contacts) {

        for (Contact c : contacts)
            add(c, true);

    }

    public void clear() {
        listContacts.clear();
        notifyDataSetChanged();
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setBtRefreshClickListener(View.OnClickListener btRefreshClickListener) {
        this.btRefreshClickListener = btRefreshClickListener;
    }

    public Contact getContact(int position) {
        return listContacts.get(position);
    }

    public static class EmptyStateViewHolder extends RecyclerView.ViewHolder {

        public EmptyStateViewHolder(View itemView) {
            super(itemView);

        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        public View view;
        public EditText inSearch;
        public ImageView btRefresh;
        public ProgressBar progressRefresh;

        public HeaderViewHolder(View view) {
            super(view);

            this.view = view;
            this.inSearch = (EditText) view.findViewById(R.id.in_search);
            this.btRefresh = (ImageView) view.findViewById(R.id.bt_refresh);
            this.progressRefresh = (ProgressBar) view.findViewById(R.id.progress_refresh);
        }

        public void toggleBtRefresh(boolean show) {

            if (btRefresh != null)
                btRefresh.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        public void toggleProgressRefresh(boolean show) {

            if (progressRefresh != null)
                progressRefresh.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {

        public View frame;
        public TextView name, message, tip;
        public ImageView img;

        public ContactViewHolder(View item) {
            super(item);

            this.frame = item.findViewById(R.id.frame);
            this.name = (TextView) item.findViewById(R.id.name);
            this.message = (TextView) item.findViewById(R.id.message);
            this.tip = (TextView) item.findViewById(R.id.tip);
            this.img = (ImageView) item.findViewById(R.id.picture);
        }

    }

}
