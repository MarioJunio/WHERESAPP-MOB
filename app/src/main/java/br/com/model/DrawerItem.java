package br.com.model;

/**
 * Created by MarioJ on 19/03/15.
 */
public class DrawerItem {

    private int icon;
    private String title;
    private int count;
    private boolean isCunterVisible = false;

    public DrawerItem() {
    }

    public DrawerItem(int icon, String title, int count, boolean isCunterVisible) {
        this.icon = icon;
        this.title = title;
        this.count = count;
        this.isCunterVisible = isCunterVisible;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isCunterVisible() {
        return isCunterVisible;
    }

    public void setCunterVisible(boolean isCunterVisible) {
        this.isCunterVisible = isCunterVisible;
    }
}
