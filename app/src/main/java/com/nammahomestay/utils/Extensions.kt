package com.nammahomestay.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun View.snack(message: String) = Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
fun Long.shortTime(): String = SimpleDateFormat("dd MMM, h:mm a", Locale.US).format(Date(this))
