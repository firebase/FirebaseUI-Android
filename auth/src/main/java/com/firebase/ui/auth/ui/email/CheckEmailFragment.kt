package com.firebase.ui.auth.ui.email

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.FragmentBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.util.ui.ImeHelper
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckEmailFragment : FragmentBase(), View.OnClickListener, ImeHelper.DonePressedListener {

    companion object {
        const val TAG = "CheckEmailFragment"
        @JvmStatic
        fun newInstance(email: String?): CheckEmailFragment {
            val fragment = CheckEmailFragment()
            val args = Bundle()
            args.putString(ExtraConstants.EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var mHandler: CheckEmailHandler
    private lateinit var mSignInButton: Button
    private lateinit var mSignUpButton: Button
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mEmailEditText: EditText
    private lateinit var mEmailLayout: TextInputLayout
    private lateinit var mEmailFieldValidator: EmailFieldValidator
    private lateinit var mListener: CheckEmailListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fui_check_email_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSignInButton = view.findViewById(R.id.button_sign_in)
        mSignUpButton = view.findViewById(R.id.button_sign_up)
        mProgressBar = view.findViewById(R.id.top_progress_bar)

        mEmailLayout = view.findViewById(R.id.email_layout)
        mEmailEditText = view.findViewById(R.id.email)
        mEmailFieldValidator = EmailFieldValidator(mEmailLayout)
        mEmailLayout.setOnClickListener(this)
        mEmailEditText.setOnClickListener(this)

        val headerText: TextView? = view.findViewById(R.id.header_text)
        headerText?.visibility = View.GONE

        ImeHelper.setImeOnDoneListener(mEmailEditText, this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mEmailEditText.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

        mSignInButton.setOnClickListener(this)
        mSignUpButton.setOnClickListener(this)

        val termsText: TextView? = view.findViewById(R.id.email_tos_and_pp_text)
        val footerText: TextView? = view.findViewById(R.id.email_footer_tos_and_pp_text)
        val flowParameters: FlowParameters = getFlowParams()

        if (!flowParameters.shouldShowProviderChoice()) {
            PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(
                requireContext(),
                flowParameters,
                termsText
            )
        } else {
            termsText?.visibility = View.GONE
            PrivacyDisclosureUtils.setupTermsOfServiceFooter(
                requireContext(),
                flowParameters,
                footerText
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mHandler = ViewModelProvider(this).get(CheckEmailHandler::class.java)
        mHandler.init(getFlowParams())

        val activity = activity
        if (activity !is CheckEmailListener) {
            throw IllegalStateException("Activity must implement CheckEmailListener")
        }
        mListener = activity

        if (savedInstanceState == null) {
            val email = arguments?.getString(ExtraConstants.EMAIL)
            if (!TextUtils.isEmpty(email)) {
                mEmailEditText.setText(email)
            } else if (getFlowParams().enableCredentials) {
                mHandler.fetchCredential()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mHandler.onActivityResult(requestCode, resultCode, data)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_sign_in -> signIn()
            R.id.button_sign_up -> signUp()
            R.id.email_layout, R.id.email -> mEmailLayout.error = null
        }
    }

    override fun onDonePressed() {
        // When the user hits "done" on the keyboard, default to sign‑in.
        signIn()
    }

    private fun getEmailProvider(): String {
        // Iterate through all IdpConfig entries
        for (config in getFlowParams().providers) {
            if (EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD == config.providerId) {
                return EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD
            }
        }
        // Default to standard email/password
        return EmailAuthProvider.PROVIDER_ID
    }

    private fun signIn() {
        val email = mEmailEditText.text.toString()
        if (mEmailFieldValidator.validate(email)) {
            val provider = getEmailProvider()
            val user = User.Builder(provider, email).build()
            mListener.onExistingEmailUser(user)
        }
    }

    private fun signUp() {
        val email = mEmailEditText.text.toString()
        if (mEmailFieldValidator.validate(email)) {
            val provider = getEmailProvider()
            val user = User.Builder(provider, email).build()
            mListener.onNewUser(user)
        }
    }

    override fun showProgress(message: Int) {
        mSignInButton.isEnabled = false
        mSignUpButton.isEnabled = false
        mProgressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        mSignInButton.isEnabled = true
        mSignUpButton.isEnabled = true
        mProgressBar.visibility = View.INVISIBLE
    }

    interface CheckEmailListener {
        fun onExistingEmailUser(user: User)
        fun onExistingIdpUser(user: User)
        fun onNewUser(user: User)
        fun onDeveloperFailure(e: Exception)
    }
}