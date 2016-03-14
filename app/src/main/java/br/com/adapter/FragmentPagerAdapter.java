package br.com.adapter;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.astuetz.PagerSlidingTabStrip;

import com.br.wheresapp.R;
import br.com.fragments.ChatsFragment;
import br.com.fragments.GeoMapFragment;

/**
 * Created by MarioJ on 09/09/15.
 */
public class FragmentPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

    private static final String TAG = "FragmentPagerAdapter";

    private final int CHATS = 0;

    private int tabs_icon[] = {R.drawable.ic_chat, R.drawable.ic_locate};
    private String tabs[];
    private Resources resources;

    public FragmentPagerAdapter(FragmentManager fm, Resources resources) {
        super(fm);

        this.resources = resources;

        // load tabs from resource
        tabs = resources.getStringArray(R.array.application_tabs);

    }

    @Override
    public Fragment getItem(int i) {

        Fragment view = null;

        if (i == CHATS)
            view = ChatsFragment.newInstance();
        else
            view = GeoMapFragment.newInstance();

        return view;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public int getPageIconResId(int position) {
        return tabs_icon[position];
    }
}
