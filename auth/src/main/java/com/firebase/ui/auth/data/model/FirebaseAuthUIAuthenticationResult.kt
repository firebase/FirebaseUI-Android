package com.firebase.ui.auth.data.model

import com.firebase.ui.auth.IdpResponse

/**
 * Result of launching a [FirebaseAuthUIActivityResultContract]
 */
data class FirebaseAuthUIAuthenticationResult(
    /**
     * The result code of the received activity result
     *
     * @see android.app.Activity.RESULT_CANCELED
     * @see android.app.Activity.RESULT_OK
     */
    val resultCode: Int,
    /**
     * The contained [IdpResponse] returned from the Firebase library
     */
    val idpResponse: IdpResponse?
)