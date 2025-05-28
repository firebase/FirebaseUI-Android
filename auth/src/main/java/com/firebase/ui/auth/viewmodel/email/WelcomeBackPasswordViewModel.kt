/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.viewmodel.email

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.Resource
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WelcomeBackPasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val handler = WelcomeBackPasswordHandler(application)
    
    private val _signInState = MutableStateFlow<Resource<IdpResponse>?>(null)
    val signInState: StateFlow<Resource<IdpResponse>?> = _signInState

    fun init(flowParams: com.firebase.ui.auth.data.model.FlowParameters) {
        handler.init(flowParams)
    }

    fun signIn(
        email: String,
        password: String,
        idpResponse: IdpResponse,
        credential: AuthCredential? = null
    ) {
        viewModelScope.launch {
            handler.startSignIn(email, password, idpResponse, credential)
            handler.getOperation().observeForever { resource ->
                _signInState.value = resource
            }
        }
    }

    fun getPendingPassword(): String {
        return handler.getPendingPassword()
    }
} 