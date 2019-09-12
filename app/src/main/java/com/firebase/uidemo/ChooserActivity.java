/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.firebase.uidemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.uidemo.auth.AnonymousUpgradeActivity;
import com.firebase.uidemo.auth.AuthUiActivity;
import com.firebase.uidemo.database.firestore.FirestoreChatActivity;
import com.firebase.uidemo.database.firestore.FirestorePagingActivity;
import com.firebase.uidemo.database.realtime.FirebaseDbPagingActivity;
import com.firebase.uidemo.database.realtime.RealtimeDbChatActivity;
import com.firebase.uidemo.storage.ImageActivity;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ChooserActivity extends AppCompatActivity {
    @BindView(R.id.activities)
    RecyclerView mActivities;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthUI.canHandleIntent(getIntent())) {
            Intent intent = new Intent(ChooserActivity.this, AuthUiActivity
                    .class);
            intent.putExtra(ExtraConstants.EMAIL_LINK_SIGN_IN, getIntent().getData().toString());
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_chooser);
        ButterKnife.bind(this);

        mActivities.setLayoutManager(new LinearLayoutManager(this));
        mActivities.setAdapter(new ActivityChooserAdapter());
        mActivities.setHasFixedSize(true);
    }

    private static class ActivityChooserAdapter
            extends RecyclerView.Adapter<ActivityStarterHolder> {
        private static final Class[] CLASSES = new Class[]{
                AuthUiActivity.class,
                AnonymousUpgradeActivity.class,
                FirestoreChatActivity.class,
                FirestorePagingActivity.class,
                RealtimeDbChatActivity.class,
                FirebaseDbPagingActivity.class,
                ImageActivity.class,
        };

        private static final int[] DESCRIPTION_NAMES = new int[]{
                R.string.title_auth_activity,
                R.string.title_anonymous_upgrade,
                R.string.title_firestore_activity,
                R.string.title_firestore_paging_activity,
                R.string.title_realtime_database_activity,
                R.string.title_realtime_database_paging_activity,
                R.string.title_storage_activity
        };

        private static final int[] DESCRIPTION_IDS = new int[]{
                R.string.desc_auth,
                R.string.desc_anonymous_upgrade,
                R.string.desc_firestore,
                R.string.desc_firestore_paging,
                R.string.desc_realtime_database,
                R.string.desc_realtime_database_paging,
                R.string.desc_storage
        };

        @Override
        public ActivityStarterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ActivityStarterHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.activity_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ActivityStarterHolder holder, int position) {
            holder.bind(CLASSES[position], DESCRIPTION_NAMES[position], DESCRIPTION_IDS[position]);
        }

        @Override
        public int getItemCount() {
            return CLASSES.length;
        }
    }

    private static class ActivityStarterHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private TextView mTitle;
        private TextView mDescription;

        private Class mStarterClass;

        public ActivityStarterHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.text1);
            mDescription = itemView.findViewById(R.id.text2);
        }

        private void bind(Class aClass, @StringRes int name, @StringRes int description) {
            mStarterClass = aClass;

            mTitle.setText(name);
            mDescription.setText(description);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            itemView.getContext().startActivity(new Intent(itemView.getContext(), mStarterClass));
        }
    }
}
