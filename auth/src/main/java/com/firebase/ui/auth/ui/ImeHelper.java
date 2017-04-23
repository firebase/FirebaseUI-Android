package com.firebase.ui.auth.ui;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class ImeHelper {
    public static void addImeOnDoneListener(EditText doneEditText, final Runnable doneAction) {
        doneEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || actionId == EditorInfo.IME_ACTION_DONE) {
                    doneAction.run();
                    return true;
                }
                return false;
            }
        });
    }
}
