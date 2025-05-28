/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
