package br.com.binder;

import android.os.Binder;

import java.lang.ref.WeakReference;

/**
 * Created by MarioJ on 25/01/16.
 */
public class LocalBinder<E> extends Binder {

    private final WeakReference<E> service;

    public LocalBinder(E service) {
        this.service = new WeakReference<E>(service);
    }

    public E getService() {
        return service.get();
    }
}
