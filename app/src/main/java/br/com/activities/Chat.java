package br.com.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.br.wheresapp.R;

import java.io.Serializable;

import br.com.adapter.ChatPagerAdapter;
import br.com.aplication.App;
import br.com.aplication.Application;
import br.com.dao.MessageDAO;
import br.com.model.domain.Contact;

public class Chat extends AppCompatActivity implements Serializable {

    private final String TAG = "Chat";

    private Application application;
    private ViewPager viewPager;
    private ChatPagerAdapter pagerAdapter;
    private boolean notification;
    private int countBackPressed = 0;

    // contato selecionado
    private Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (Application) getApplicationContext();

        Bundle extras = getIntent().getExtras();

        if (extras != null) {

            notification = extras.getBoolean("notification");
            contact = (Contact) extras.getSerializable("contact");
            final boolean isRead = extras.getBoolean("is_read");

            // atualiza mensagens para lidas até o momento
            App.runBackgroundService(new Runnable() {

                @Override
                public void run() {

                    // checa se ha alguma mensagem na janela enviada pelo contato selecionado, se sim marca suas mensagens como lidas
                    if (!isRead)
                        MessageDAO.instance(getApplicationContext()).read(contact.getDdi(), contact.getPhone());
                }
            });

        }

        // set view
        setContentView(R.layout.activity_chat);

        // inicializa fragments
        init();
    }

    private void init() {

        viewPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                if (position > 0)
                    countBackPressed = 2;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    public void toChat() {
        viewPager.setCurrentItem(0, true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && countBackPressed > 0) {
            toChat();
            countBackPressed--;
            return false;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {

            if (notification)
                startActivity(new Intent(this, Main.class));
            else if (viewPager.getCurrentItem() > 0)
                toChat();
            else
                finish();

        }

        return super.onOptionsItemSelected(item);
    }

    public Contact getContact() {
        return contact;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (contact != null)
            application.setChatContact(contact);

    }

    @Override
    protected void onPause() {
        super.onPause();

        application.setChatContact(null);
    }

    @Override
    protected void onStop() {
        super.onStop();

        application.setChatContact(null);
    }
}
