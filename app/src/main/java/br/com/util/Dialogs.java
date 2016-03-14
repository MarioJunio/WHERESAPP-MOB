package br.com.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.br.wheresapp.R;
import br.com.adapter.CategoryAdapter;

/**
 * Created by MarioJ on 04/04/15.
 */
public class Dialogs {

    public static AlertDialog choosePictureCreate(final Context context, AdapterView.OnItemClickListener listener) {

        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_category, null);

        GridView gridView = (GridView) view.findViewById(R.id.gridview);
        gridView.setAdapter(new CategoryAdapter(context));
        gridView.setOnItemClickListener(listener);

        alert.setView(view);

        return alert.create();
    }

}
