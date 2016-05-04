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

package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseActivity extends android.support.v7.app.AppCompatActivity {
    public static final String EXTRA_ID = "BaseActivity.current_state_id";
    public static final int BACK_IN_FLOW = Activity.RESULT_FIRST_USER + 1;

    private static final int NEXT_FLOW = 1000;

    private Controller mController;

    protected int mId;
    protected String mAppName;
    protected AtomicBoolean isPendingFinishing = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent previousIntent = getIntent();
        mAppName = getIntent().getStringExtra(ControllerConstants.EXTRA_APP_NAME);
        mId = previousIntent.getIntExtra(EXTRA_ID, Controller.DEFAULT_INIT_FLOW_ID);
        mController = setUpController();
    }

    protected abstract Controller setUpController();

    @Override
    public void onBackPressed() {
        // If you need to provide additional data back to previous activity
        // Override method from within the activity
        finish(BACK_IN_FLOW, new Intent());
    }

    public void finish(int resultCode, Intent data) {
        Result result = new Result(mId, resultCode, data);
        new StateTransitionTask().execute(result);
    }

    private class StateTransitionTask extends AsyncTask<Result, Void, Action> {

        @Override
        protected Action doInBackground(Result... results) {
            isPendingFinishing.set(true);
            return mController.next(results[0]);
        }

        @Override
        protected void onPostExecute(Action result) {
            doAction(result);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEXT_FLOW) {
            if (requestCode != BaseActivity.RESULT_CANCELED) {
                setResult(resultCode, data);
                finish();
                return;
            }
        }
    }

    protected void doAction(Action action) {
        if (action.getNextId() == Controller.FINISH_FLOW_ID) {
            setResult(action.getFinishResultCode(), action.getFinishData());
            finish();
            return;
        } else if (action.getNextId() == Controller.BLOCK_AT_CURRENT_ACTIVITY_ID) {
            isPendingFinishing.set(false);
            blockHandling(action.getNextIntent());
            return;
        } else if (action.getNextId() == Controller.START_NEW_FLOW_ID) {
           Intent newFlowIntent = action.getNextIntent();
            newFlowIntent.putExtra(ControllerConstants.EXTRA_APP_NAME, mAppName);
            this.startActivityForResult(newFlowIntent, NEXT_FLOW);
            return;
        }
        if (action.hasNextAction()) {
            this.startActivity(action.getNextIntent()
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra(ControllerConstants.EXTRA_APP_NAME, mAppName)
                    .putExtra(EXTRA_ID, action.getNextId()));
        } else {
            this.setResult(action.getFinishResultCode(), action.getFinishData());
        }
        this.finish();
    }

    /**
     * Override this method to do extra handling (Error notification, update UI ,etc) when the flow
     * is blocked at current activity.
     * @param nextIntent extra data concerning the block
     */
    protected void blockHandling(Intent nextIntent) {
    }
}
