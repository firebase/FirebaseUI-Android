package com.firebase.ui.auth.util.ui;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImeHelper {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface DonePressedListener {
        void onDonePressed();
    }

    public static void setImeOnDoneListener(EditText doneEditText,
                                            final DonePressedListener listener) {
        doneEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    listener.onDonePressed();
                }

                // We need to return true even if we didn't handle the event to continue
                // receiving future callbacks.
                return true;
            } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                listener.onDonePressed();
                return true;
            }
            return false;
        });
    }
}
