package com.firebase.ui.auth.configuration

import android.content.Context
import com.firebase.ui.auth.R

/**
 * An interface for providing localized string resources. This interface defines methods for all
 * user-facing strings, such as initializing(), signInWithGoogle(), invalidEmail(),
 * passwordsDoNotMatch(), etc., allowing for complete localization of the UI.
 */
interface AuthUIStringProvider {
    fun initializing(): String
    fun signInWithGoogle(): String
    fun invalidEmail(): String
    fun passwordsDoNotMatch(): String
}

class DefaultAuthUIStringProvider(private val context: Context) : AuthUIStringProvider {
    override fun initializing(): String = ""

    override fun signInWithGoogle(): String =
        context.getString(R.string.fui_sign_in_with_google)

    override fun invalidEmail(): String = ""

    override fun passwordsDoNotMatch(): String = ""
}
