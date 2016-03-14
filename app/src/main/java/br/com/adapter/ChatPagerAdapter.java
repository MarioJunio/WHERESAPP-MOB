package br.com.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import br.com.fragments.ChatFragment;
import br.com.fragments.ProfileContactFragment;

/**
 * Created by MarioJ on 23/03/15.
 */
public class ChatPagerAdapter extends FragmentPagerAdapter {

    private static final int CHAT_VIEW = 0;
    private static final int CONTACT_VIEW = 1;

    public ChatPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment = null;

        if (position == CHAT_VIEW)
            fragment = ChatFragment.newInstance();
        else if (position == CONTACT_VIEW)
            fragment = ProfileContactFragment.newInstance();

        return fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
