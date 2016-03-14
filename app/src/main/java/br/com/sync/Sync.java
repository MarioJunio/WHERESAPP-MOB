package br.com.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import br.com.services.PhoneContactsService;
import br.com.aplication.App;

/**
 * Created by MarioJ on 27/05/15.
 */
public class Sync {

    public static final String AUTHORITY = "com.br.wheresapp.contacts_provider";
    public static final String ACCOUNT_TYPE = "com.br.wheresapp";
    public static final String ACCOUNT = "contacts";

    private static Account account;

    public static void start(final Context context) {

        // create sync account if no exists
        if (account == null)
            account = createContactsSync(context);

        try {
            // inicializa serviços da aplicação
            context.startService(new Intent(context, PhoneContactsService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static Account createContactsSync(Context context) {

        Account account = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(account, null, null);

        return account;
    }

    public static void requestSync(Context context, Bundle extras) {
        context.getContentResolver().requestSync(account, Sync.AUTHORITY, extras);
    }

}
