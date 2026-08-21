package com.example.abenly.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

fun changeLanguage(context: Context, languageCode: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)

    // Si la langue est déjà celle demandée, on ne fait rien pour éviter un reload inutile
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (currentLocale == languageCode) return

    // Applique la langue : Android gère la recréation propre de l'UI en arrière-plan
    AppCompatDelegate.setApplicationLocales(appLocale)
}