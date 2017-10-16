package com.firebase.ui.database;

import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.database.DataSnapshot;

public interface SnapshotParser<T> extends BaseSnapshotParser<DataSnapshot, T> {}
