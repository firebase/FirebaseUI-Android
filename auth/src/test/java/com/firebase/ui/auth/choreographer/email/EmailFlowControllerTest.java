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

    public static final String TEST_EMAIL = "jane.doe@example.com";
    public static final String TEST_PASSWORD = "securePassword1";
    public static final int TEST_RESULT_CODE = 101;


    public static final Intent NO_RESULT_DATA = new Intent();
    public static final String NO_EMAIL = null;
    public static final String NO_PASSWORD = null;
    public static final Boolean NO_RESTORE_PASSWORD_FLAG = null;

    public ArrayList<String> mProvider;
    public ArrayList<String> mProviders;

    private EmailFlowController mEmailFlowController;
    @Mock private HeadlessAPIWrapper mMockHeadlessApiWrapper;
    @Mock private Result mMockResult;
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
        assertEquals(hasNextAction ? "next action expected" : "no next action expected",
                hasNextAction, nextAction.hasNextAction());
        assertEquals("finish result code mismatch",
                finishResultCode, nextAction.getFinishResultCode());
        assertEquals("next state ID mismatch",
                nextId, nextAction.getNextId());
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
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);
        validateFinish(nextAction, Activity.RESULT_CANCELED);
    }

    @Test
    public void testIdSelectEmailResultCancelled() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                Activity.RESULT_CANCELED,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SELECT_EMAIL);
    }

    @Test
    public void testIdSelectEmailAccountDoesNotExist() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                TEST_RESULT_CODE,
                resultData(TEST_EMAIL, NO_PASSWORD, NO_RESTORE_PASSWORD_FLAG));

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_REGISTER_EMAIL);
        // TODO: Validate data
    }

    @Test
    public void testIdSelectEmailAccountExistsOneProvider() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                TEST_RESULT_CODE,
                resultData(TEST_EMAIL, NO_PASSWORD, NO_RESTORE_PASSWORD_FLAG));
        when(mMockHeadlessApiWrapper.isAccountExists(TEST_EMAIL)).thenReturn(true);
        when(mMockHeadlessApiWrapper.getProviderList(TEST_EMAIL)).thenReturn(mProvider);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, Controller.START_NEW_FLOW_ID);
        // TODO: Validate data
    }

    @Test
    public void testIdSelectEmailAccountExistsMultipleProviders() {
        initResultWithConditions(
                EmailFlowController.ID_SELECT_EMAIL,
                TEST_RESULT_CODE,
                resultData(TEST_EMAIL, NO_PASSWORD, NO_RESTORE_PASSWORD_FLAG));
        when(mMockHeadlessApiWrapper.isAccountExists(TEST_EMAIL)).thenReturn(true);
        when(mMockHeadlessApiWrapper.getProviderList(TEST_EMAIL)).thenReturn(mProviders);

        Action nextAction = mEmailFlowController.next(mMockResult);

//        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_WELCOME_BACK);
    }

    @Test
    public void testIdSignInBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                BaseActivity.BACK_IN_FLOW,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateFinish(nextAction, Activity.RESULT_CANCELED);
    }

    @Test
    public void testIdSignInRestorePassword() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                TEST_RESULT_CODE,
                resultData(NO_EMAIL, NO_PASSWORD, true));

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_RECOVER_PASSWORD);
        // TODO: Validate data
    }

    @Test
    public void testIdSignInNotRestorePasswordFailLogin() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                TEST_RESULT_CODE,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_FIRST_USER, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdSignInNotRestorePasswordSuccessLoginNoGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                TEST_RESULT_CODE,
                resultData(TEST_EMAIL, TEST_PASSWORD, NO_RESTORE_PASSWORD_FLAG));
        when(mMockHeadlessApiWrapper.signInWithEmailPassword(TEST_EMAIL, TEST_PASSWORD))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(false);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_OK, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdSignInNotRestorePasswordSuccessLoginGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_SIGN_IN,
                TEST_RESULT_CODE,
                resultData(TEST_EMAIL, TEST_PASSWORD, NO_RESTORE_PASSWORD_FLAG));
        when(mMockHeadlessApiWrapper.signInWithEmailPassword(TEST_EMAIL, TEST_PASSWORD))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(true);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, Controller.START_NEW_FLOW_ID);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                BaseActivity.BACK_IN_FLOW,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_SELECT_EMAIL);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailSuccessLoginNoGMSCore() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                TEST_RESULT_CODE,
                NO_RESULT_DATA);
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
                TEST_RESULT_CODE,
                NO_RESULT_DATA);
        when(mMockHeadlessApiWrapper.createEmailWithPassword(anyString(), anyString()))
                .thenReturn(mMockFirebaseUser);
        when(mMockHeadlessApiWrapper.isGMSCorePresent(any(Context.class))).thenReturn(true);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, Controller.START_NEW_FLOW_ID);
        // TODO: Validate data
    }

    @Test
    public void testIdRegisterEmailFailLogin() {
        initResultWithConditions(
                EmailFlowController.ID_REGISTER_EMAIL,
                TEST_RESULT_CODE,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, false, Activity.RESULT_FIRST_USER, Controller.FINISH_FLOW_ID);
    }

    @Test
    public void testIdRecoverPasswordBackInFlow() {
        initResultWithConditions(
                EmailFlowController.ID_RECOVER_PASSWORD,
                BaseActivity.BACK_IN_FLOW,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_SIGN_IN);
        // TODO: Validate data
    }

    @Test
    public void testIdRecoverPassword() {
        initResultWithConditions(
                EmailFlowController.ID_RECOVER_PASSWORD,
                TEST_RESULT_CODE,
                NO_RESULT_DATA);

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
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_BACK, EmailFlowController.ID_RECOVER_PASSWORD);
        // TODO: Validate data
    }

    @Test
    public void testIdConfirmRecoverPassword() {
        initResultWithConditions(
                EmailFlowController.ID_CONFIRM_RECOVER_PASSWORD,
                TEST_RESULT_CODE,
                NO_RESULT_DATA);

        Action nextAction = mEmailFlowController.next(mMockResult);

        validateAction(nextAction, Action.ACTION_NEXT, EmailFlowController.ID_SIGN_IN);
    }

    private Intent resultData(String email, String password, Boolean restorePassword) {
        Intent data = new Intent();
        if (email != null) {
            data.putExtra(ControllerConstants.EXTRA_EMAIL, email);
        }

        if (password != null) {
            data.putExtra(ControllerConstants.EXTRA_PASSWORD, password);
        }

        if (restorePassword != null) {
            data.putExtra(
                    ControllerConstants.EXTRA_RESTORE_PASSWORD_FLAG,
                    restorePassword.booleanValue());
        }
        return data;
    }
}
