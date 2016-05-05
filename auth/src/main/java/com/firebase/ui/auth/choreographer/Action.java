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

package com.firebase.ui.auth.choreographer;

import android.content.Intent;

public final class Action {

    private final int mNextId;
    private final Intent mNextIntent;

    private final int mFinishResultCode;
    private final Intent mFinishData;

    public static final int ACTION_NEXT = 0;
    public static final int ACTION_BACK = 1;


    /**
     *  Returns action that goes forward to an activity
     * @param id Action ID for the next activity
     * @param intent Intent used to launch the activity
     * @return Action to execute
     */
    public static Action next(int id, Intent intent) {
        return new Action(id, intent, ACTION_NEXT, null);
    }

    /**
     *  Returns action that transition to an activity
     * @param id Action ID for the next activity
     * @param intent Intent used to launch the activity
     * @return Action to execute
     */
    //TODO: (zhaojiac) remove this if we don't need to distinguish back and forward action. (No
    // UI transition difference or other concerns)
    public static Action back(int id, Intent intent) {
        return new Action(id, intent, ACTION_BACK, null);
    }

    public static Action finish(int resultCode, Intent data) {
        return new Action(Controller.FINISH_FLOW_ID, null, resultCode, data);
    }

    /**
     * Block the flow at the current activity without doing anything. Information regarding the
     * block is contained in the Intent.
     * @param data Data contains updated status for the current activity.
     */
    public static Action block(Intent data) {
        return new Action(Controller.BLOCK_AT_CURRENT_ACTIVITY_ID, data, 0, null);
    }

    /**
     * Create an action that starts another flow specified by the intent at the end of the current
     * flow.
     * @param newFlow Intent to start the new flow
     * @return Action that starts the new flow
     */
    public static Action startFlow(Intent newFlow) {
        return Action.next(Controller.START_NEW_FLOW_ID, newFlow);
    }

    private Action(
            int nextId,
            Intent nextIntent,
            int finishResultCode,
            Intent finishData) {
        mNextId = nextId;
        mNextIntent = nextIntent;
        mFinishResultCode = finishResultCode;
        mFinishData = finishData;
    }

    public boolean hasNextAction() {
        return mNextIntent != null;
    }

    public int getNextId() {
        return mNextId;
    }

    public Intent getNextIntent() {
        return mNextIntent;
    }

    public int getFinishResultCode() {
        return mFinishResultCode;
    }

    public Intent getFinishData() {
        return mFinishData;
    }
}
