package com.stusoft.abenly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stusoft.abenly.R
import com.stusoft.abenly.utils.SubscriptionPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// Taux de conversion fixe (1 € = X $)
private const val EUR_TO_USD_RATE = 1.08

data class SimpleSubscription(
    val name: String,
    val priceInEur: Double,
    val periodicityRes: Int
)

enum class SubscriptionPeriodicity(
    val id: String,
    val labelRes: Int,
    val annualFactor: Double
) {
    DAILY("daily", R.string.periodicity_daily, 365.0),
    MONTHLY("monthly", R.string.periodicity_monthly, 12.0),
    QUARTERLY("quarterly", R.string.periodicity_quarterly, 4.0),
    SEMIANNUALLY("semiannually", R.string.periodicity_semiannually, 2.0),
    ANNUALLY("annually", R.string.periodicity_annually, 1.0);

    companion object {
        fun fromId(id: String): SubscriptionPeriodicity {
            return entries.find { it.id == id } ?: MONTHLY
        }

        fun fromResId(resId: Int): SubscriptionPeriodicity {
            return entries.find { it.labelRes == resId } ?: MONTHLY
        }
    }
}

@Composable
fun SubscriptionsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Détection de la langue/devise actuelle
    val currentLocale = Locale.getDefault()
    val isDollarZone = currentLocale.language == "en" || currentLocale.country == "US"

    // Formateur monétaire selon le système
    val currencyFormatter = remember(currentLocale) { NumberFormat.getCurrencyInstance(currentLocale) }
    val currencySymbol = remember(currentLocale) {
        try {
            val currency = Currency.getInstance(currentLocale)
            when (currency.currencyCode) {
                "USD" -> "$"
                "EUR" -> "€"
                else -> currency.symbol.takeIf { it != "¤" } ?: (if (isDollarZone) "$" else "€")
            }
        } catch (e: Exception) {
            if (isDollarZone) "$" else "€"
        }
    }

    // Convertisseur selon la langue (les prix en base restent stockés en EUR)
    fun convertPrice(priceInEur: Double): Double {
        return if (isDollarZone) priceInEur * EUR_TO_USD_RATE else priceInEur
    }

    fun convertToEur(displayPrice: Double): Double {
        return if (isDollarZone) displayPrice / EUR_TO_USD_RATE else displayPrice
    }

    val subscriptions = remember { mutableStateListOf<SimpleSubscription>() }
    var showAddDialog by remember { mutableStateOf(false) }

    // Calcul du total annuel converti
    val totalAnnuallyInEur = subscriptions.sumOf { item ->
        val periodicity = SubscriptionPeriodicity.fromResId(item.periodicityRes)
        item.priceInEur * periodicity.annualFactor
    }
    val totalAnnuallyConverted = convertPrice(totalAnnuallyInEur)

    // Chargement initial des données
    LaunchedEffect(Unit) {
        val customItemsSet = SubscriptionPreferences.getCustomSubscriptions(context).firstOrNull() ?: emptySet()
        customItemsSet.forEach { itemString ->
            val parts = itemString.split("|")
            val name = parts.getOrNull(0) ?: ""
            val periodicityId = parts.getOrNull(1) ?: "monthly"
            val priceEur = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            if (name.isNotBlank() && subscriptions.none { it.name == name }) {
                val periodicity = SubscriptionPeriodicity.fromId(periodicityId)
                subscriptions.add(
                    SimpleSubscription(
                        name = name,
                        priceInEur = priceEur,
                        periodicityRes = periodicity.labelRes
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // En-tête du tableau
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.btn_subscriptions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = stringResource(id = R.string.periodicity_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(25.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_subscription),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            // Liste d'abonnements
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(subscriptions) { index, item ->
                    val convertedPrice = convertPrice(item.priceInEur)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nom & Prix (converti dynamiquement selon la langue)
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.priceInEur > 0.0) {
                                Text(
                                    text = currencyFormatter.format(convertedPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Périodicité
                        Text(
                            text = stringResource(id = item.periodicityRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.2f)
                        )

                        // Supprimer
                        IconButton(
                            onClick = {
                                val itemToRemove = subscriptions[index]
                                subscriptions.removeAt(index)
                                coroutineScope.launch {
                                    val periodicityEnum = SubscriptionPeriodicity.fromResId(itemToRemove.periodicityRes)
                                    val itemKeyFormatted = "${itemToRemove.name}|${periodicityEnum.id}|${itemToRemove.priceInEur}"
                                    SubscriptionPreferences.removeCustomSubscription(context, itemKeyFormatted)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            // Total Annuel
            if (subscriptions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_annually),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(totalAnnuallyConverted),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Dialogue d'ajout
        if (showAddDialog) {
            AddSubscriptionDialog(
                currencySymbol = currencySymbol,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, inputPrice, periodicity ->
                    // Si l'utilisateur saisit en $, on convertit la valeur saisie en EUR avant de la stocker
                    val priceInEur = convertToEur(inputPrice)

                    subscriptions.add(
                        SimpleSubscription(
                            name = name,
                            priceInEur = priceInEur,
                            periodicityRes = periodicity.labelRes
                        )
                    )

                    coroutineScope.launch {
                        val customString = "$name|${periodicity.id}|$priceInEur"
                        SubscriptionPreferences.addCustomSubscription(context, customString)
                    }

                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, periodicity: SubscriptionPeriodicity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedPeriodicity by remember { mutableStateOf(SubscriptionPeriodicity.MONTHLY) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_subscription)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(id = R.string.nameSubscription)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.replace(',', '.') },
                    label = { Text(text = stringResource(id = R.string.price_label, currencySymbol)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = stringResource(id = selectedPeriodicity.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(id = R.string.periodicity_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        SubscriptionPeriodicity.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = option.labelRes)) },
                                onClick = {
                                    selectedPeriodicity = option
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), price, selectedPeriodicity)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}