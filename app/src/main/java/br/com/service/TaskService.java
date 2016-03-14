package br.com.service;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import br.com.model.Transaction;

/**
 * Created by MarioJ on 29/03/15.
 */
public class TaskService extends AsyncTask<Void, Void, Object> {

    private Context context;
    private Transaction transaction;
    private ProgressDialog progressDialog;
    private int message;

    public TaskService(Transaction transaction, Context context, int message) {
        this.transaction = transaction;
        this.context = context;
        this.message = message;
    }

    @Override
    protected void onPreExecute() {
        openDialog();
        transaction.init();
    }

    @Override
    protected Object doInBackground(Void... params) {
        return transaction.perform();
    }

    @Override
    protected void onPostExecute(Object o) {
        progressDialog.hide();
        transaction.updateView(o);
    }

    private void openDialog() {
        progressDialog = ProgressDialog.show(context, null, context.getString(message), true, false);
    }

    public static void start(Transaction transaction, Context context, int message) {
        new TaskService(transaction, context, message).execute();
    }
}
