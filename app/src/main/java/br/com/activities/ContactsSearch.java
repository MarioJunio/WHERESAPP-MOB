package br.com.activities;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

/**
 * Created by MarioJ on 12/10/15.
 */
public class ContactsSearch extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Override
    public boolean onQueryTextSubmit(String query) {
        Toast.makeText(getApplicationContext(), "Text Submit -> " + query, Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Toast.makeText(getApplicationContext(), "Text Change -> " + newText, Toast.LENGTH_LONG).show();
        return false;
    }
}
