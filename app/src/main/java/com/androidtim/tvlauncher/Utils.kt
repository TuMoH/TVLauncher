package com.androidtim.tvlauncher

import android.content.SharedPreferences
import android.text.TextUtils

fun SharedPreferences.getStringList(key: String): List<String> {
    return TextUtils.split(getString(key, ""), "‚‗‚").toList()
}

fun SharedPreferences.putStringList(key: String, value: List<String>) {
    edit().putString(
        key,
        TextUtils.join("‚‗‚", value.toTypedArray())
    ).apply()
}