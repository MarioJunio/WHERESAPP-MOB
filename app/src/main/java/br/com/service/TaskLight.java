package br.com.service;

import android.os.AsyncTask;

import br.com.model.Transaction;

/**
 * Created by MarioJ on 23/04/15.
 */
public class TaskLight extends AsyncTask<Void, Void, Object> {

    private Transaction transaction;

    public static void start(Transaction transaction) {
        new TaskLight(transaction).execute();
    }

    private TaskLight(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    protected void onPreExecute() {
        transaction.init();
    }

    @Override
    protected Object doInBackground(Void... params) {
        return transaction.perform();
    }

    @Override
    protected void onPostExecute(Object e) {
        transaction.updateView(e);
    }
}
