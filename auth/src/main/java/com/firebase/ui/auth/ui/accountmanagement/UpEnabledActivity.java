package com.firebase.ui.auth.ui.accountmanagement;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.auth.ui.HelperActivityBase;

/**
 * TODO javadoc
 */
public class UpEnabledActivity extends HelperActivityBase {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
