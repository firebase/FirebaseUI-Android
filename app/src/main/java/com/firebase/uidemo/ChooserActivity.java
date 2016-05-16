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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.uidemo.auth.AuthUiActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

public class ChooserActivity extends AppCompatActivity {

    private static final Class[] CLASSES = new Class[]{
            ChatActivity.class,
            AuthUiActivity.class,
    };

    private static final int[] DESCRIPTION_NAMES = new int[] {
            R.string.name_chat,
            R.string.name_auth_ui
    };

    private static final int[] DESCRIPTION_IDS = new int[] {
            R.string.desc_chat,
            R.string.desc_auth_ui
    };

    @BindView(R.id.list_view)
    ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);
        ButterKnife.bind(this);

        mListView.setAdapter(new MyArrayAdapter(
                this,
                android.R.layout.simple_list_item_2,
                CLASSES));
    }

    @OnItemClick(R.id.list_view)
    public void onItemClick(int position) {
        Class clicked = CLASSES[position];
        startActivity(new Intent(this, clicked));
    }

    public static class MyArrayAdapter extends ArrayAdapter<Class> {

        private Context mContext;
        private Class[] mClasses;

        public MyArrayAdapter(Context context, int resource, Class[] objects) {
            super(context, resource, objects);

            mContext = context;
            mClasses = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, null);
            }

            ((TextView) view.findViewById(android.R.id.text1)).setText(DESCRIPTION_NAMES[position]);
            ((TextView) view.findViewById(android.R.id.text2)).setText(DESCRIPTION_IDS[position]);

            return view;
        }
    }
}
