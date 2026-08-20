package com.example.abenly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.abenly.R
import com.example.abenly.utils.changeLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbenlyTopAppBar(
    titleRes: Int,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLanguage = configuration.locales.get(0).language.lowercase()

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