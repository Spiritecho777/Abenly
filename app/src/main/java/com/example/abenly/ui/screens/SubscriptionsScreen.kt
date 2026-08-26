package com.example.abenly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.abenly.R
import com.example.abenly.model.SubscriptionItem
import com.example.abenly.utils.SubscriptionPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Formateur monétaire natif et symbole de devise selon la région du téléphone
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val currencySymbol = remember {
        try {
            val currency = Currency.getInstance(Locale.getDefault())
            when (currency.currencyCode) {
                "USD" -> "$"
                "EUR" -> "€"
                else -> currency.symbol.takeIf { it != "¤" } ?: "€"
            }
        } catch (e: Exception) {
            "€"
        }
    }

    val subscriptions = remember { mutableStateListOf<SubscriptionItem>() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedIndexForPicker by remember { mutableStateOf<Int?>(null) }

    // Chargement des données
    LaunchedEffect(Unit) {
        val customItemsSet = SubscriptionPreferences.getCustomSubscriptions(context).firstOrNull() ?: emptySet()
        customItemsSet.forEach { itemString ->
            val parts = itemString.split("|")
            val name = parts.getOrNull(0) ?: ""
            val months = parts.getOrNull(1)?.toLongOrNull() ?: 1L
            val price = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            if (name.isNotBlank() && subscriptions.none { it.key == name }) {
                subscriptions.add(SubscriptionItem(key = name, price = price, maxMonthsAllowed = months))
            }
        }

        subscriptions.forEachIndexed { index, item ->
            val savedTimestamp = SubscriptionPreferences.getLastDate(context, "sub_${item.key}").firstOrNull()
            if (savedTimestamp != null) {
                val localDate = Instant.ofEpochMilli(savedTimestamp)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                subscriptions[index] = subscriptions[index].copy(lastDoneDate = localDate)
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

            // En-tête
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.btn_subscriptions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = stringResource(id = R.string.last_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
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

            // Liste des éléments
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(subscriptions) { index, item ->
                    val nextPaymentDate = remember(item.lastDoneDate, item.maxMonthsAllowed) {
                        item.lastDoneDate?.plusMonths(item.maxMonthsAllowed)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndexForPicker = index }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nom & Prix formaté
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = item.key,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.price > 0.0) {
                                Text(
                                    text = currencyFormatter.format(item.price),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Dates de réalisation & d'échéance
                        Column(modifier = Modifier.weight(1.2f)) {
                            val doneDateText = item.lastDoneDate?.format(dateFormatter) ?: stringResource(id = R.string.select)
                            Text(
                                text = stringResource(id = R.string.maintenance_done, doneDateText),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.lastDoneDate != null) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (nextPaymentDate != null) {
                                Text(
                                    text = stringResource(id = R.string.subscription_next_payment, nextPaymentDate.format(dateFormatter)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Action suppression
                        IconButton(
                            onClick = {
                                val itemToRemove = subscriptions[index]
                                subscriptions.removeAt(index)
                                coroutineScope.launch {
                                    val itemKeyFormatted = "${itemToRemove.key}|${itemToRemove.maxMonthsAllowed}|${itemToRemove.price}"
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
        }

        // Dialogue de saisie
        if (showAddDialog) {
            AddSubscriptionDialog(
                currencySymbol = currencySymbol,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, price, dateMillis, months ->
                    val localDate = Instant.ofEpochMilli(dateMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()

                    val newItem = SubscriptionItem(
                        key = name,
                        price = price,
                        maxMonthsAllowed = months,
                        lastDoneDate = localDate
                    )

                    subscriptions.add(newItem)

                    coroutineScope.launch {
                        val customString = "$name|$months|$price"
                        SubscriptionPreferences.addCustomSubscription(context, customString)
                        SubscriptionPreferences.saveLastDate(context, "sub_$name", dateMillis)
                    }

                    showAddDialog = false
                }
            )
        }

        // Picker de modification de date
        selectedIndexForPicker?.let { index ->
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { selectedIndexForPicker = null },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()

                            subscriptions[index] = subscriptions[index].copy(lastDoneDate = selectedDate)

                            coroutineScope.launch {
                                SubscriptionPreferences.saveLastDate(context, "sub_${subscriptions[index].key}", millis)
                            }
                        }
                        selectedIndexForPicker = null
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedIndexForPicker = null }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, lastDateMillis: Long, months: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var monthsText by remember { mutableStateOf("1") }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

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

                OutlinedTextField(
                    value = monthsText,
                    onValueChange = { monthsText = it.filter { char -> char.isDigit() } },
                    label = { Text(text = stringResource(id = R.string.frequencySubscription)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && monthsText.isNotBlank(),
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val months = monthsText.toLongOrNull() ?: 1L
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), price, millis, months)
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