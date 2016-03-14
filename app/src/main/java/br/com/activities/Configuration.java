package br.com.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import java.io.Serializable;

import com.br.wheresapp.R;

import br.com.fragments.ConfigurationFragment;
import br.com.fragments.ConfigurationProfileFragment;
import br.com.model.domain.User;

public class Configuration extends ActionBarActivity implements Serializable {

    public static final String STEP = "step";

    private Toolbar toolbar;

    public Configuration() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        init();

        if (extras != null) {

            int getStep = extras.getInt(STEP);

            if (getStep == User.State.DESACTIVE.ordinal())
                getSupportFragmentManager().beginTransaction().add(R.id.content, ConfigurationFragment.newInstance()).commit();
            else if (getStep == User.State.RESTORE.ordinal())
                getSupportFragmentManager().beginTransaction().add(R.id.content, ConfigurationFragment.newInstance()).commit();
            else if (getStep == User.State.CONFIG_PROFILE.ordinal())
                getSupportFragmentManager().beginTransaction().add(R.id.content, ConfigurationProfileFragment.newInstance()).commit();

        } else
            getSupportFragmentManager().beginTransaction().add(R.id.content, ConfigurationFragment.newInstance()).commit();

    }

    private void init() {

        setContentView(R.layout.activity_configuration);
        customBar();
    }


    private void customBar() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(this.getString(R.string.verify_your_number));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {

            case KeyEvent.KEYCODE_BACK:
                return false;
        }

        return super.onKeyDown(keyCode, event);
    }
}
