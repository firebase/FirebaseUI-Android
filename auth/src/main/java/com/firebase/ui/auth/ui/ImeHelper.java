package com.firebase.ui.auth.ui;

import android.support.annotation.RestrictTo;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImeHelper {
    public interface DonePressedListener {
        void onDonePressed();
    }

    public static void setImeOnDoneListener(EditText doneEditText,
                                            final DonePressedListener listener) {
        doneEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || actionId == EditorInfo.IME_ACTION_DONE) {
                    listener.onDonePressed();
                    return true;
                }
                return false;
            }
        });
    }
}
