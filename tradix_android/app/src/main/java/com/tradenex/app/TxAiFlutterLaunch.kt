package com.tradenex.app

import android.content.Context
import android.content.Intent

/** Reflection keeps native-only builds working before the optional Flutter module is bootstrapped. */
object TxAiFlutterLaunch {
    fun open(context: Context): Boolean = runCatching {
        val activity = Class.forName("com.tradenex.app.TxAiFlutterActivity")
        context.startActivity(Intent(context, activity))
        true
    }.getOrDefault(false)
}
