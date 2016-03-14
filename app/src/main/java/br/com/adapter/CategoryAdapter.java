package br.com.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.br.wheresapp.R;

import br.com.model.Category;
import br.com.util.Utils;

/**
 * Created by MarioJ on 04/04/15.
 */
public class CategoryAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private Category[] categories = new Category[]{new Category(Category.GALLERY, R.drawable.ic_images, "Galeria"),
            new Category(Category.TAKE_PICTURE, R.drawable.ic_camera, "Camera"),
            new Category(Category.REMOVE_PICTURE, R.drawable.ic_close, "Sem foto")};

    public CategoryAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return categories.length;
    }

    @Override
    public Object getItem(int position) {
        return categories[position];
    }

    @Override
    public long getItemId(int position) {
        return categories[position].getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.grid_item_category, null);
            convertView.setLayoutParams(new GridView.LayoutParams(Utils.dpToPixel(context, 80), Utils.dpToPixel(context, 60)));

            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.ic);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.title.setText(categories[position].getName());
        viewHolder.icon.setAdjustViewBounds(true);
        viewHolder.icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.icon.setImageResource(categories[position].getDrawable());
        viewHolder.icon.setColorFilter(Color.parseColor("#7c9d04"));

        return convertView;
    }

    private class ViewHolder {
        TextView title;
        ImageView icon;
    }
}
