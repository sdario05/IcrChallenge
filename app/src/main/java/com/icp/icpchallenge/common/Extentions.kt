package com.icp.icpchallenge.common

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun hideKeyboard(activity: AppCompatActivity) {
    val inputMethodManager: InputMethodManager = activity.getSystemService(
        AppCompatActivity.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    if (inputMethodManager.isAcceptingText) {
        inputMethodManager.hideSoftInputFromWindow(
            activity.currentFocus?.windowToken,
            0
        )
    }
}

fun showToast(context: Context, text: Int) {
    Toast.makeText(context, context.getString(text), Toast.LENGTH_SHORT).show()
}