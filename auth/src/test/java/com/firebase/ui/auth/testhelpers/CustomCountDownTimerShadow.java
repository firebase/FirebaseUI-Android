package com.firebase.ui.auth.testhelpers;

import android.os.CountDownTimer;

import org.mockito.Mock;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(CountDownTimer.class)
public class CustomCountDownTimerShadow {
    private boolean started;
    private long countDownInterval;
    private long millisInFuture;

    @Mock
    public CountDownTimer countDownTimer;

    public CustomCountDownTimerShadow() {
    }

    @SuppressWarnings("checkstyle:methodname")
    public void __constructor__(long millisInFuture, long countDownInterval) {
        this.countDownInterval = countDownInterval;
        this.millisInFuture = millisInFuture;
        this.started = false;
    }

    @Implementation
    public final synchronized CountDownTimer start() {
        this.started = true;
        return this.countDownTimer;
    }

    @Implementation
    public final void cancel() {
        this.started = false;
    }

    public void invokeTick(long millisUntilFinished) {
        this.countDownTimer.onTick(millisUntilFinished);
    }

    public void invokeFinish() {
        this.countDownTimer.onFinish();
    }

    public boolean hasStarted() {
        return this.started;
    }

    public long getCountDownInterval() {
        return this.countDownInterval;
    }

    public long getMillisInFuture() {
        return this.millisInFuture;
    }
}
