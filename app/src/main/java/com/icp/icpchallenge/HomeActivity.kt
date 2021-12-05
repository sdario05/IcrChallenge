package com.icp.icpchallenge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.icp.icpchallenge.common.PROVIDER
import com.icp.icpchallenge.common.Provider
import com.icp.icpchallenge.common.USERS
import com.icp.icpchallenge.common.showToast
import com.icp.icpchallenge.model.User
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity: AppCompatActivity() {
    private val db = FirebaseDatabase.getInstance().getReference(USERS)
    private var maxKey = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        title = getString(R.string.user_add)
        setListeners()
    }

    private fun setListeners() {
        logoutButton.setOnClickListener {
            logout()
        }
        saveButton.setOnClickListener {
            if (allFieldsFilled()) {
                progressBar.visibility = View.VISIBLE
                it.isEnabled = false
                db.get()
                    .addOnSuccessListener {data ->
                        setMaxKey(data)
                        val user = getNewUser()
                        db.child(maxKey.toString()).setValue(user)
                            .addOnSuccessListener {
                                resetViews()
                            }
                            .addOnFailureListener {
                                showSavingErrorMessage()
                            }
                    }
                    .addOnFailureListener {
                        showSavingErrorMessage()
                    }
            } else {
                showToast(this, R.string.some_fields_empty)
            }
        }
        showUsersButton.setOnClickListener {
            db.get()
                .addOnSuccessListener { data ->
                    showUsers(data)
                }
                .addOnFailureListener {
                    showToast(this, R.string.getting_error)
                }
        }
    }

    private fun allFieldsFilled() =
        nameEditText.text.toString().isNotEmpty() &&
        surnameEditText.text.toString().isNotEmpty() &&
        ageEditText.text.toString().isNotEmpty() &&
        birthdateEditText.text.toString().isNotEmpty()

    private fun setMaxKey(data: DataSnapshot) {
        data.children.forEach {user ->
            user.key?.let {key ->
                if (key.toInt() > maxKey) {
                    maxKey = key.toInt()
                }
            }
        }
        maxKey += 1
    }

    private fun getNewUser() =
        User(
            maxKey.toString(),
            nameEditText.text.toString(),
            surnameEditText.text.toString(),
            ageEditText.text.toString().toInt(),
            birthdateEditText.text.toString()
        )

    private fun resetViews() {
        nameEditText.run {
            setText("")
            requestFocus()
        }
        surnameEditText.setText("")
        ageEditText.setText("")
        birthdateEditText.setText("")
        progressBar.visibility = View.GONE
        saveButton.isEnabled = true
    }

    private fun showSavingErrorMessage() {
        progressBar.visibility = View.GONE
        saveButton.isEnabled = true
        showToast(this, R.string.saving_error)
    }

    private fun logout() {
        if (intent.getStringExtra(PROVIDER) == Provider.FACEBOOK.value) {
            LoginManager.getInstance().logOut()
        } else {
            FirebaseAuth.getInstance().signOut()
        }
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showUsers(usersList: DataSnapshot) {
        val dialog = AlertDialog.Builder(this).create()
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_users, null)
        dialog.setView(dialogView)
        val usersTextView = dialogView.findViewById<TextView>(R.id.usersTextView)
        var users = ""
        usersList.children.forEach {user ->
            users += user.value.toString() + "\n\n"
        }
        usersTextView.text = users
        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}