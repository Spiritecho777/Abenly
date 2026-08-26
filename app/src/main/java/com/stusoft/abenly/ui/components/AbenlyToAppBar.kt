package com.stusoft.abenly.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stusoft.abenly.R
import com.stusoft.abenly.utils.changeLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbenlyTopAppBar(
    titleRes: Int,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Récupère la LANGUE EFFECTIVE de l'app (AppCompatDelegate d'abord, puis System Locale)
    val currentLanguage = remember(configuration) {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            appLocales.get(0)?.language?.lowercase() ?: "fr"
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                configuration.locales.get(0).language.lowercase()
            } else {
                @Suppress("DEPRECATION")
                configuration.locale.language.lowercase()
            }
        }
    }

    TopAppBar(
        title = { Text(text = stringResource(id = titleRes)) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.btn_back)
                    )
                }
            }
        },
        actions = {
            LanguageDropdownMenu(
                currentLanguage = currentLanguage,
                onLanguageSelected = { nextLanguage ->
                    changeLanguage(context, nextLanguage)
                }
            )
        }
    )
}