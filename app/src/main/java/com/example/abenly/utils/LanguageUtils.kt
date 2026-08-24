package com.example.abenly.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

fun changeLanguage(context: Context, languageCode: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)

    // Force l'application de la langue sélectionnée
    AppCompatDelegate.setApplicationLocales(appLocale)
}