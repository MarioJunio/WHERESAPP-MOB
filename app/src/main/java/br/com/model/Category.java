package br.com.model;

/**
 * Created by MarioJ on 04/04/15.
 */
public class Category {

    public static int GALLERY = 1, TAKE_PICTURE = 2, REMOVE_PICTURE = 3;

    private int id;
    private int drawable;
    private String name;

    public Category(int id, int drawable, String name) {
        this.id = id;
        this.drawable = drawable;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
