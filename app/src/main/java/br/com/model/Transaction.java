package br.com.model;

/**
 * Created by MarioJ on 29/03/15.
 */
public interface Transaction {

    void init();

    Object perform();

    void updateView(Object o);

}
