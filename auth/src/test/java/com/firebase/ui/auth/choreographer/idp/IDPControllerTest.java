package com.firebase.ui.auth.choreographer.idp;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static junit.framework.Assert.assertEquals;

import android.app.PendingIntent;
import android.content.Intent;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.api.FactoryHeadlessAPIShadow;
import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.choreographer.email.EmailFlowController;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {FactoryHeadlessAPIShadow.class}, sdk = 21)

public class IDPControllerTest {

    public static final String EMAIL = "serikb@google.com";
    public static final int RANDOM_NUMBER = 914159393;

    public ArrayList<String> mProvider;
    public ArrayList<String> mProviders;

    private EmailFlowController mEmailFlowController;
    @Mock private HeadlessAPIWrapper mMockHeadlessApiWrapper;
    @Mock private Result mMockResult;
    @Mock private Intent mMockIntent;
    @Mock private PendingIntent mMockPendingIntent;
    @Mock private FirebaseUser mMockFirebaseUser;

    @Before
    public void setUp () {
        MockitoAnnotations.initMocks(this);
        FactoryHeadlessAPIShadow.setHeadlessAPIWrapper(mMockHeadlessApiWrapper);
        mEmailFlowController
                = new EmailFlowController(RuntimeEnvironment.application, ControllerConstants.APP_NAME);

        mProvider = new ArrayList<>();
        mProvider.add("first_provider");

        mProviders = new ArrayList<>();
        mProviders.add("first_provider");
        mProviders.add("second_provider");
    }

    void initResultWithConditions(int resultId, int resultCode, Intent resultData) {
        when(mMockResult.getId()).thenReturn(resultId);
        when(mMockResult.getResultCode()).thenReturn(resultCode);
        when(mMockResult.getData()).thenReturn(resultData);
    }

    void validateAction(Action nextAction, int finishResultCode, int nextId) {
        validateAction(nextAction, true, finishResultCode, nextId);
    }

    void validateAction(
            Action nextAction,
            boolean hasNextAction,
            int finishResultCode,
            int nextId) {
        assertEquals(hasNextAction, nextAction.hasNextAction());
        assertEquals(finishResultCode, nextAction.getFinishResultCode());
        assertEquals(nextId, nextAction.getNextId());
    }

}
