package com.firebase.ui.database;

import com.google.firebase.database.DataSnapshot;

interface IndexMismatchListener {
    void onIndexMismatch(int index, DataSnapshot snapshot);
}
