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
