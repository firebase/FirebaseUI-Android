package com.firebase.ui.auth.ui.accountmanagement;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.firebase.ui.auth.R;

/**
 * TODO javadoc
 */

public abstract class SaveFieldActivity extends UpEnabledActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_save) {
            onSaveMenuItem();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void onSaveMenuItem();
}
