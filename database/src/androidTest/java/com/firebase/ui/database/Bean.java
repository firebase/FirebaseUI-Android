package com.firebase.ui.database;

public class Bean {
    private int mNumber;
    private String mText;
    private boolean mBool;

    public Bean() {
        // Needed for Firebase
    }

    public Bean(int number, String text, boolean bool) {
        mNumber = number;
        mText = text;
        mBool = bool;
    }

    public Bean(int index) {
        this(index, "Text " + index, index % 2 == 0);
    }

    public int getNumber() {
        return mNumber;
    }

    public String getText() {
        return mText;
    }

    public boolean isBool() {
        return mBool;
    }
}
