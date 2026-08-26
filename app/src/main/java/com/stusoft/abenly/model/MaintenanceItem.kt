package com.stusoft.abenly.model

import androidx.annotation.StringRes
import java.time.LocalDate

data class MaintenanceItem(
    val key: String,
    @StringRes val titleRes: Int,
    val maxMonthsAllowed: Long,
    var lastDoneDate: LocalDate? = null
)