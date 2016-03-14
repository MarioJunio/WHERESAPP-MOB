package br.com.util;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by MarioJ on 12/03/16.
 */
public class Fragments {


    public static void setViewLayout(Fragment fragment, int id) {

        LayoutInflater inflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View mainView = inflater.inflate(id, null);

        ViewGroup rootView = (ViewGroup) fragment.getView();
        rootView.removeAllViews();
        rootView.addView(mainView);
    }


}
