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

package com.firebase.ui.auth.util;

import android.os.CountDownTimer;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class CustomCountDownTimer {
    private final long mMillisInFuture;
    private final long mCountDownInterval;
    private CountDownTimer mCountDownTimer;

    protected CustomCountDownTimer(long millisInFuture, long countDownInterval) {
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

    private CountDownTimer create(long millisInFuture, long countDownInterval) {
        return new CountDownTimer(millisInFuture, countDownInterval) {
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

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public abstract void onFinish();

    protected abstract void onTick(long millisUntilFinished);
}
