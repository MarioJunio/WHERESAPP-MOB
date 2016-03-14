package br.com.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.br.wheresapp.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Map;

import br.com.model.domain.Contact;
import br.com.service.ImageService;
import br.com.smack.Smack;
import br.com.widgets.MapCircleImageView;

/**
 * Created by MarioJ on 26/09/15.
 */
public class ContactGeoMapAdapter extends RecyclerView.Adapter<ContactGeoMapAdapter.ViewHolder> {

    private static final String TAG = "ContactGeoMapAdapter";

    private Activity activity;
    private List<Contact> listContacts;
    private Map<String, Contact> mapContacts;
    private Activity parent;
    private GoogleMap map;
    private OnItemClickListener listener;
    private Handler handler;

    public ContactGeoMapAdapter(Activity activity, List<Contact> listContacts, Map<String, Contact> mapContacts, Activity parent, GoogleMap map) {
        this.activity = activity;
        this.listContacts = listContacts;
        this.mapContacts = mapContacts;
        this.parent = parent;
        this.map = map;
        handler = new Handler(Looper.getMainLooper());
    }

    public boolean userExists(String ddi, String phone) {
        return mapContacts.containsKey(Smack.toSmackUser(ddi, phone));
    }

    public void add(Contact contact) {

        listContacts.add(contact);
        mapContacts.put(Smack.toSmackUser(contact.getDdi(), contact.getPhone()), contact);
    }

    public void updateLocation(String from, final double latitude, final double longitude) {

        final Contact contact = mapContacts.get(from);
        contact.setLatitude(latitude);
        contact.setLongitude(longitude);

        if (contact.getMarker() != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    contact.getMarker().remove();
                    contact.getMarker().setPosition(new LatLng(latitude, longitude));
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        return new ViewHolder(inflater.inflate(R.layout.item_contact_geo_map, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        Contact contact = listContacts.get(position);

        // get bitmap image from contact photo
        Bitmap photo = getPhotoBitmap(contact.getPhoto());

        // add marker in map and set into contact marker
        contact.handlerMarker(createMarker(contact.getPhoto() == null, photo, contact.getName(), contact.getLatitude(), contact.getLongitude()));

        // set photo
        holder.photo.setImageBitmap(photo);

        if (contact.getPhoto() == null)
            holder.photo.setColorFilter(Color.parseColor("#E0E0E0"));
    }

    private Marker createMarker(boolean defaultPhoto, Bitmap photo, String name, double latitude, double longitude) {

        View view = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.map_custom_marker, null);

        ImageView photoMarker = (ImageView) view.findViewById(R.id.photo);
        photoMarker.setImageBitmap(photo);

        if (defaultPhoto)
            photoMarker.setColorFilter(Color.parseColor("#E0E0E0"));

        return map.addMarker(new MarkerOptions()
                .title(name)
                .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(view)))
                .position(new LatLng(latitude, longitude))
                .flat(true));

    }

    private Bitmap getPhotoBitmap(byte[] photo) {

        if (photo != null)
            return ImageService.byteToImage(photo);
        else
            return BitmapFactory.decodeResource(parent.getResources(), R.drawable.ic_contact);

    }

    private Bitmap createDrawableFromView(View view) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;

    }

    @Override
    public int getItemCount() {
        return listContacts.size();
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public MapCircleImageView photo;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {

            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            photo = (MapCircleImageView) itemView.findViewById(R.id.contact_photo);

            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    listener.onItemClick(v, getPosition());
                }
            });
        }

    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }


}
