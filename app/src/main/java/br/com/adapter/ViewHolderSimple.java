package br.com.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.br.wheresapp.R;

/**
 * Created by MarioJ on 07/08/15.
 */
public class ViewHolderSimple extends RecyclerView.ViewHolder {

    private static final String TAG = "ViewHolderSimple";
    public View view;
    public TextView name, message, tip;
    public ImageView img;

    public ViewHolderSimple(View view) {
        super(view);

        this.view = view.findViewById(R.id.frame);
        name = (TextView) view.findViewById(R.id.name);
        message = (TextView) view.findViewById(R.id.message);
        tip = (TextView) view.findViewById(R.id.tip);
        img = (ImageView) view.findViewById(R.id.picture);
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        view.setOnLongClickListener(onLongClickListener);
    }
}
