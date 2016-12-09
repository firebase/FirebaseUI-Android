package com.firebase.ui.database;

import com.google.firebase.database.DataSnapshot;

interface OnIndexMismatchListener {
    void onIndexMismatch(int index, DataSnapshot snapshot);
}
