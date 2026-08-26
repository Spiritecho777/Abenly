package com.stusoft.abenly.model

import java.time.LocalDate

data class SubscriptionItem(
    val key: String,
    val price: Double,
    val maxMonthsAllowed: Long = 1, // Ex: Prélèvement tous les X mois
    val lastDoneDate: LocalDate? = null
)