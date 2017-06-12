/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.ui.phone;

import android.os.CountDownTimer;

abstract class CustomCountDownTimer {
    private final long mMillisInFuture;
    private final long mCountDownInterval;
    private CountDownTimer mCountDownTimer;

    CustomCountDownTimer(long millisInFuture, long countDownInterval) {
        mMillisInFuture = millisInFuture;
        mCountDownInterval = countDownInterval;
        mCountDownTimer = create(millisInFuture, countDownInterval);
    }

    public void update(long millisInFuture) {
        mCountDownTimer.cancel();
        mCountDownTimer = create(millisInFuture, mCountDownInterval);
        mCountDownTimer.start();
    }

    public void renew() {
        update(mMillisInFuture);
    }

    CountDownTimer create(long millisInFuture, long counDownInterval) {
        return new CountDownTimer(millisInFuture, counDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                CustomCountDownTimer.this.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                CustomCountDownTimer.this.onFinish();
            }
        };
    }

    public void cancel() {
        mCountDownTimer.cancel();
    }

    public void start() {
        mCountDownTimer.start();
    }

    protected abstract void onFinish();

    protected abstract void onTick(long millisUntilFinished);
}
