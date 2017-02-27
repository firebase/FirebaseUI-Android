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

    public void setNumber(int number) {
        this.mNumber = number;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public void setBool(boolean bool) {
        this.mBool = bool;
    }
}
