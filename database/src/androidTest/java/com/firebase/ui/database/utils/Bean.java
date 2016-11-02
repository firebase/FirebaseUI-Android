package com.firebase.ui.database.utils;

public class Bean {
    private int number;
    private String text;
    private boolean bool;

    public Bean() {
    }

    public Bean(int number, String text, boolean bool) {
        this.number = number;
        this.text = text;
        this.bool = bool;
    }

    public Bean(int index) {
        this(index, "Text " + index, index % 2 == 0);
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public boolean isBool() {
        return bool;
    }
}