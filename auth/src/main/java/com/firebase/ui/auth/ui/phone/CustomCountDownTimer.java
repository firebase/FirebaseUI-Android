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

    protected abstract void onFinish();

    protected abstract void onTick(long millisUntilFinished);
}
