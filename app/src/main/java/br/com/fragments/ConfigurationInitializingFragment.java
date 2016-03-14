package br.com.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.br.wheresapp.R;

import org.jivesoftware.smack.SmackException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.activities.Main;
import br.com.aplication.Application;
import br.com.aplication.Phones;
import br.com.dao.ContactDAO;
import br.com.dao.PhoneContactDAO;
import br.com.model.Transaction;
import br.com.model.domain.Contact;
import br.com.model.domain.User;
import br.com.net.Http;
import br.com.service.DateService;
import br.com.service.TaskLight;
import br.com.util.Utils;

/**
 * Created by MarioJ on 27/04/15.
 */
public class ConfigurationInitializingFragment extends Fragment {

    private final String TAG = "ConfigurationInitializingFragment";

    private Application application;

    public static ConfigurationInitializingFragment newInstance() {
        return new ConfigurationInitializingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // aplicação
        application = (Application) getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuration_initializing, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Bem Vindo ao WheresApp");

        initialize();
    }

    private void initialize() {

        TaskLight.start(new Transaction() {

            @Override
            public void init() {
            }

            @Override
            public Object perform() {

                try {

                    // get own phone number
                    String myPhone = application.getCurrentUser().getDdi() + application.getCurrentUser().getPhone();

                    // Storage number as String to send to the server
                    String numbers = "";

                    // map to bind contact id and number
                    Map<String, Integer> mapNumbers = new HashMap<>();

                    // instance contacts DAO
                    ContactDAO contactDAO = ContactDAO.instance(getActivity().getApplicationContext());

                    // instance phone contacts DAO
                    PhoneContactDAO phoneContactDAO = PhoneContactDAO.instance(getActivity().getApplicationContext());

                    // clear table contacts
                    contactDAO.delete();

                    // get all phone contacts
                    Set<Contact> phoneContacts = phoneContactDAO.all();

                    // map phone to contact id
                    for (Contact contact : phoneContacts) {

                        if (!contact.getPhone().equals(myPhone)) {
                            mapNumbers.put(contact.getPhone(), contact.getId());
                            numbers += contact.getPhone() + ",";
                        }
                    }

                    // check if phone has contacts, if no, then continue
                    if (mapNumbers.isEmpty())
                        return true;

                    // remove the last separator ','
                    numbers = numbers.substring(0, numbers.length() - 1);

                    // send to server my contacts to syncronize
                    List<Contact> contacts = Http.syncronizeContacts(numbers);

                    if (!Utils.isNetworkAvailable(getActivity().getApplicationContext())) {
                        Toast.makeText(getContext(), getResources().getString(R.string.network_not_available), Toast.LENGTH_LONG).show();
                        return false;
                    }

                    // espera se conectar e autenticar
                    application.smackService.waitUntilOnline();

                    if (contacts != null && !contacts.isEmpty()) {

                        // iterate over contacts returned from server and persist in device
                        for (Contact contact : contacts) {
                            contact.setId(mapNumbers.get(contact.getDdi() + contact.getPhone()));
                            contact.setModification(DateService.getTimeNow());

                            contactDAO.persist(contact);

                            application.smackService.subscribe(contact.getDdi(), contact.getPhone());
                        }

                        application.getCurrentUser().setContactsCheckSum(Phones.getContactsCheckSum(phoneContactDAO.all()));
                    }

                    // atualiza nickname no ejabber
                    application.smackService.updateName(application.getCurrentUser().getName());

                } catch (SmackException.NotConnectedException note) {
                    note.printStackTrace();
                    return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                return true;

            }

            @Override
            public void updateView(Object o) {

                // se ok, prossiga, se nao retorne a etapa anterior
                if (((Boolean) o).booleanValue()) {

                    // update state user to active
                    application.getCurrentUser().setState(User.State.ACTIVE);
                    application.update();

                    startActivity(new Intent(getActivity(), Main.class));

                } else
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content, ConfigurationProfileFragment.newInstance()).commit();
            }
        });
    }
}
