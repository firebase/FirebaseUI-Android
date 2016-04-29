package com.firebase.ui.auth.choreographer.email;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static junit.framework.Assert.assertEquals;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.api.FactoryHeadlessAPIShadow;
import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.ui.BaseActivity;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {FactoryHeadlessAPIShadow.class}, sdk = 21)

public class EmailFlowControllerTest {

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
        mEmailFlowController = new EmailFlowController(RuntimeEnvironment.application, ControllerConstants.APP_NAME);

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

    void validateFinish(Action action, int resultCode) {
        assertEquals(Controller.FINISH_FLOW_ID, action.getNextId());
        assertEquals(false, action.hasNextAction());
        assertEquals(resultCode, action.getFinishResultCode());
    }

    @Test
    public void testIdSelectEmailBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                BaseActivity.BACK_IN_FLOW,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);
        validateFinish(nextAction, Activity.RESULT_CANCELED);
    }

    @Test
    public void testIdSelectEmailResultCancelled() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                Activity.RESULT_CANCELED,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SELECT_EMAIL);
    }

    @Test
    public void testIdSelectEmailAccountDoesNotExist() {
        initResultWithConditions(EmailFlowController.ID_SELECT_EMAIL, RANDOM_NUMBER, mMockIntent);
        when(mMockIntent.getStringExtra(ControllerConstants.EXTRA_EMAIL)).thenReturn(EMAIL);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_REGISTER_EMAIL);
        // TODO: Validate data
    }

    @Test
    public void testIdSelectEmailAccountExistsOneProvider() {
        initResultWithConditions(EmailFlowController.ID_SELECT_EMAIL, RANDOM_NUMBER, mMockIntent);
        when(mMockIntent.getStringExtra(ControllerConstants.EXTRA_EMAIL)).thenReturn(EMAIL);
        when(mMockHeadlessApiWrapper.isAccountExists(EMAIL)).thenReturn(true);
        when(mMockHeadlessApiWrapper.getProviderList(EMAIL)).thenReturn(mProvider);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SIGN_IN);
        // TODO: Validate data
    }

    @Test
    public void testIdSelectEmailAccountExistsMultipleProviders() {
        initResultWithConditions(EmailFlowController.ID_SELECT_EMAIL, RANDOM_NUMBER, mMockIntent);
        when(mMockIntent.getStringExtra(ControllerConstants.EXTRA_EMAIL)).thenReturn(EMAIL);
        when(mMockHeadlessApiWrapper.isAccountExists(EMAIL)).thenReturn(true);
        when(mMockHeadlessApiWrapper.getProviderList(EMAIL)).thenReturn(mProviders);

        Action nextAction = mEmailFlowController.next(mMockResult);

//        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_WELCOME_BACK);
    }

    @Test
    public void testIdSignInBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                BaseActivity.BACK_IN_FLOW,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateFinish(nextAction, Activity.RESULT_CANCELED);
    }

    @Test
    public void testIdSignInRestorePassword() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                RANDOM_NUMBER,
                mMockIntent);
        when(mMockIntent.getBooleanExtra(ControllerConstants.EXTRA_RESTORE_PASSWORD_FLAG, false))
                .thenReturn(true);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_RECOVER_PASSWORD);
        // TODO: Validate data
    }

    @Test
    public void testIdSignInNotRestorePasswordFailLogin() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                RANDOM_NUMBER,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_FIRST_USER, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdSignInNotRestorePasswordSuccessLoginNoGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                RANDOM_NUMBER,
                mMockIntent);
        when(mMockHeadlessApiWrapper.signInWithEmailPassword(anyString(), anyString()))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(false);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_OK, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdSignInNotRestorePasswordSuccessLoginGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                RANDOM_NUMBER,
                mMockIntent);
        when(mMockHeadlessApiWrapper.signInWithEmailPassword(anyString(), anyString()))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(true);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SAVE_CREDENTIALS);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                BaseActivity.BACK_IN_FLOW,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_SELECT_EMAIL);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailSuccessLoginNoGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                RANDOM_NUMBER,
                mMockIntent);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(false);
        when(mMockHeadlessApiWrapper.createEmailWithPassword(anyString(), anyString()))
                .thenReturn(mMockFirebaseUser);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_OK, Controller.FINISH_FLOW_ID);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailSuccessLoginGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                RANDOM_NUMBER,
                mMockIntent);
        when(mMockHeadlessApiWrapper.createEmailWithPassword(anyString(), anyString()))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(true);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SAVE_CREDENTIALS);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailFailLogin() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                RANDOM_NUMBER,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_FIRST_USER, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdWelcomeBackBackInFlow() {
//        initResultWithConditions(
//                EmailFlowController.ID_WELCOME_BACK,
//                BaseActivity.BACK_IN_FLOW,
//                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateFinish(nextAction, Activity.RESULT_CANCELED);
    }

    @Test
    public void testIdWelcomeBack() {
//        initResultWithConditions(
//                EmailFlowController.ID_WELCOME_BACK,
//                RANDOM_NUMBER,
//                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_FIRST_USER, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdRecoverPasswordBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_RECOVER_PASSWORD,
                BaseActivity.BACK_IN_FLOW,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_SIGN_IN);
        // TODO: Validate data
    }

    @Test
    public void testIdRecoverPassword() {
        initResultWithConditions(
                EmailFlowController.ID_RECOVER_PASSWORD,
                RANDOM_NUMBER,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(
                nextAction,
                Action.ACTION_NEXT,
                EmailFlowController.ID_CONFIRM_RECOVER_PASSWORD);
        // TODO: Validate data
    }

    @Test
    public void testIdConfirmRecoverPasswordBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_CONFIRM_RECOVER_PASSWORD,
                BaseActivity.BACK_IN_FLOW,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_RECOVER_PASSWORD);
        // TODO: Validate data
    }

    @Test
    public void testIdConfirmRecoverPassword() {
        initResultWithConditions(
                EmailFlowController.ID_CONFIRM_RECOVER_PASSWORD,
                RANDOM_NUMBER,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SIGN_IN);
    }

    @Test
    public void testIdSaveCredentials() {
        initResultWithConditions(
                EmailFlowController.ID_SAVE_CREDENTIALS,
                RANDOM_NUMBER,
                mMockIntent);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, RANDOM_NUMBER, Controller.FINISH_FLOW_ID);
        // TODO: Validate data
    }
}
