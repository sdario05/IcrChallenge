package com.icp.icpchallenge

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.icp.icpchallenge.common.*
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {
    private val callbackManager = CallbackManager.Factory.create()
    private var  forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationId = ""
    private lateinit var firebaseAuth: FirebaseAuth
    private val phoneNumberCallback = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            resetViews()
            if (exception is FirebaseAuthInvalidCredentialsException) {
                showToast(this@LoginActivity, R.string.invalid_request)
            } else if (exception is FirebaseTooManyRequestsException) {
                showToast(this@LoginActivity, R.string.quote_exceeded)
            }
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(verificationId, token)
            this@LoginActivity.verificationId = verificationId
            this@LoginActivity.forceResendingToken = token
            codeEditText.isEnabled = true
            sendCodeButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        title = getString(R.string.login)
        firebaseAuth = FirebaseAuth.getInstance()
        setListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

    }

    private fun setListeners() {
        facebookButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            phoneButton.isEnabled = false
            facebookButton.isEnabled = false
            LoginManager.getInstance().logInWithReadPermissions(this, listOf(EMAIL))
            LoginManager.getInstance().registerCallback(callbackManager,
                object: FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult?) {
                        result?.let {
                            val token = it.accessToken
                            val credential = FacebookAuthProvider.getCredential(token.token)
                            firebaseAuth.signInWithCredential(credential).addOnCompleteListener {task ->
                                if (task.isSuccessful) {
                                    goToHome(Provider.FACEBOOK)
                                } else {
                                    facebookButton.isEnabled = true
                                    phoneButton.isEnabled = true
                                    progressBar.visibility = View.GONE
                                    showMessage()
                                }
                            }
                        }
                    }

                    override fun onCancel() {
                        facebookButton.isEnabled = true
                        phoneButton.isEnabled = true
                        progressBar.visibility = View.GONE
                    }

                    override fun onError(error: FacebookException?) {
                        facebookButton.isEnabled = true
                        phoneButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        showMessage()
                    }
                }
            )
        }
        phoneButton.setOnClickListener {
            facebookButton.isEnabled = false
            phoneButton.isEnabled = false
            phoneContainer.visibility = View.VISIBLE
        }
        requestCodeButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotEmpty()) {
                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(phoneNumberCallback)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
                requestCodeButton.isEnabled = false
                phoneNumberEditText.isEnabled = false
                showToast(this, R.string.code_requested)
                hideKeyboard(this)
            } else {
                showToast(this, R.string.phone_number_empty)
            }
        }
        sendCodeButton.setOnClickListener {
            val code = codeEditText.text.toString()
            if (code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, codeEditText.text.toString())
                signInWithPhoneAuthCredential(credential)
                codeEditText.isEnabled = false
                sendCodeButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                hideKeyboard(this)
            } else {
                showToast(this, R.string.code_empty)
            }
        }
    }

    private fun showMessage() {
        val builder = AlertDialog.Builder(this)
        builder.run {
            title = getString(R.string.authentication_error_title)
            setMessage(getString(R.string.authentication_error_message))
            setPositiveButton(getString(R.string.accept), null)
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun goToHome(provider: Provider) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(PROVIDER, provider.value)
        startActivity(intent)
        finish()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    goToHome(Provider.PHONE_NUMBER)
                } else {
                    resetViews()
                    showMessage()
                }
            }
    }

    private fun resetViews() {
        facebookButton.isEnabled = true
        phoneButton.isEnabled = true
        phoneContainer.visibility = View.GONE
        requestCodeButton.isEnabled = true
        phoneNumberEditText.isEnabled = true
        codeEditText.isEnabled = false
        sendCodeButton.isEnabled = false
        progressBar.visibility = View.GONE
    }
}