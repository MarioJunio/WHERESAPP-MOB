package br.com.widgets;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.View;

import com.br.wheresapp.R;

/**
 * Created by MarioJ on 10/01/16.
 */
public class PopupChats extends PopupMenu {

    public PopupChats(Context context, View anchor, int gravity) {
        super(context, anchor, gravity);
    }

    public void inflate() {
        getMenuInflater().inflate(R.menu.chats_poupmenu, getMenu());
    }

    public void setClickListener(OnMenuItemClickListener clickListener) {
        setOnMenuItemClickListener(clickListener);
    }
}
