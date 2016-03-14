package br.com.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.br.wheresapp.R;

import br.com.activities.WaitSmsDialog;
import br.com.aplication.Application;
import br.com.aplication.Phones;
import br.com.aplication.Storage;
import br.com.model.Confirmation;
import br.com.model.ListenerSuccess;
import br.com.model.Transaction;
import br.com.model.domain.User;
import br.com.net.Http;
import br.com.service.PhoneFormatTextWatcher;
import br.com.service.TaskService;
import br.com.util.Utils;

/**
 * Created by MarioJ on 09/03/15.
 */
public class ConfigurationFragment extends Fragment implements Transaction {

    private final String TAG = "ConfigurationFragment";

    private Application session;
    private EditText inDDI, inPhone;
    private String countryISO;
    private PhoneFormatTextWatcher phoneFormatTextWatcher;

    public ConfigurationFragment() {
        super();
    }

    public static ConfigurationFragment newInstance() {
        return new ConfigurationFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        session = ((Application) getActivity().getApplication());
        session.createUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuration, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        countryISO = Phones.getPhoneCountryISO(getActivity()).toUpperCase();

        phoneFormatTextWatcher = new PhoneFormatTextWatcher(countryISO);
        final String ddi = Phones.getCountryCode(getActivity(), countryISO);

        // get widgets from layout
        inDDI = (EditText) view.findViewById(R.id.inDDI);
        inPhone = (EditText) view.findViewById(R.id.inNumber);
        inPhone.addTextChangedListener(phoneFormatTextWatcher);

        // set up widgets
        inDDI.setText(ddi);

        inDDI.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                countryISO = Phones.getCountryISO(getActivity(), editable.toString());

                if (countryISO != null && !countryISO.isEmpty()) {

                    phoneFormatTextWatcher.setTypeFormatter(countryISO);

                    if (!inPhone.getText().toString().isEmpty())
                        inPhone.setText(phoneFormatTextWatcher.formatNumber(inPhone.getText().toString()));
                }

            }
        });

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_configuration, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int selectId = item.getItemId();

        if (selectId == R.id.menu_next)
            checkPhoneNumber();

        return true;
    }

    private void checkPhoneNumber() {

        if (!Utils.isNetworkAvailable(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.network_not_available), Toast.LENGTH_LONG).show();
        } else {

            String phone = inPhone.getText().toString();

            if (phone.isEmpty())
                Toast.makeText(getActivity(), getString(R.string.phone_number_empty_warning), Toast.LENGTH_LONG).show();
            else {

                Storage.createDirectoriesApplication(getActivity());

                String ddi = inDDI.getText().toString().replace(Phones.INTERNATIONAL_IDENTIFIER, "").trim();
                phone = Phones.formatNumber(phone).replace(Phones.INTERNATIONAL_IDENTIFIER, "");

                session.getCurrentUser().setDdi(ddi);
                session.getCurrentUser().setPhone(phone);

                Utils.hideKeyboard(getActivity());

                TaskService.start(this, getActivity(), R.string.checking);
            }
        }

    }

    @Override
    public void init() {
        // DO Nothing
    }

    @Override
    public Object perform() {

        Confirmation httpResponse = null;

        try {
            httpResponse = Http.checkAccount(session.getCurrentUser().getDdi(), session.getCurrentUser().getPhone());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return httpResponse;
    }

    @Override
    public void updateView(Object o) {

        Confirmation httpResponse = (Confirmation) o;

        // Se o HTTP Request foi realizada com sucesso
        if (httpResponse != null) {

            if (!httpResponse.isOk()) {
                Toast.makeText(getContext(), getResources().getString(R.string.error_checkaccount), Toast.LENGTH_LONG).show();
                return;
            }

            // Se a conta e nova no servidor, se não a conta já existe
            if (httpResponse.isNew()) {

                if (Utils.DEBUG) {
                    nextStep();
                    return;
                }

                // wait SMS
                waitSmsView();

                // send SMS for itself
//                sendSms();

            } else {

                // Busca no banco de dados local o usuario correspondente ao numero DDI e Phone
                User getUser = session.getUserDAO().getUser(session.getCurrentUser().getDdi(), session.getCurrentUser().getPhone());

                // Se o usuario existir
                if (getUser != null && getUser.getState() == User.State.ACTIVE) {

                    // altera o estado da conta para config de perfil
                    getUser.setState(User.State.CONFIG_PROFILE);
                    session.setUser(getUser);

                    // salva novo estado
                    session.save();

                    // encaminha para a tela de perfil
                    goProfileView();

                } else {

                    if (Utils.DEBUG) {
                        nextStep();
                        return;
                    }

                    // wait SMS
                    waitSmsView();

                    // send SMS for itself
//                    sendSms();
                }
            }

        } else {
            Toast.makeText(getContext(), "Não foi possível criar sua conta, tente novamente em alguns minutos.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Nao foi possivel criar sua conta");
        }

    }

    private void waitSmsView() {

        WaitSmsDialog alert = WaitSmsDialog.newInstance(getActivity());
        alert.setCancelable(false);
        alert.setListenerOnAuthSuccess(new ListenerSuccess() {

            @Override
            public void execute() {
                nextStep();
            }
        });

        alert.show();
    }


    public void nextStep() {

        // set next step state
        session.getCurrentUser().setState(User.State.CONFIG_PROFILE);

        // Salva o usuario
        session.save();

        // go next step
        goProfileView();
    }

    private void goProfileView() {

        try {
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content, ConfigurationProfileFragment.newInstance()).commit();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            System.exit(0);
        }
    }

    /*
    private void sendSms() {

        App.runBackgroundService(new Runnable() {
            @Override
            public void run() {

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(session.getCurrentUser().getDdi() + session.getCurrentUser().getPhone(), null, "WheresApp Code:" + serverConfirmation.getConfirmCode(), null, null);
                } catch (final Exception e) {

                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Exception Send SMS -> " + e.getCause(), Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        });

    } */

}
