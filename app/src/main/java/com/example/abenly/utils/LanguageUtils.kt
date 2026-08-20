package com.example.abenly.utils

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

fun changeLanguage(context: Context, languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
    val activity = context as? Activity
    activity?.let {
        it.overridePendingTransition(0, 0)
        it.recreate()
    }
}