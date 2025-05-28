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