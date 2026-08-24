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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.abenly.R
import com.example.abenly.model.MaintenanceItem
import com.example.abenly.utils.MaintenancePreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarMaintenanceScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val items = remember {
        mutableStateListOf(
            MaintenanceItem("vidange", R.string.maintenance_engine_oil, maxMonthsAllowed = 12),
            MaintenanceItem("controle_technique", R.string.maintenance_technical_control, maxMonthsAllowed = 24),
            MaintenanceItem("pneus", R.string.maintenance_tires, maxMonthsAllowed = 1),
            MaintenanceItem("freins", R.string.maintenance_brakes, maxMonthsAllowed = 24),
            MaintenanceItem("essuie_glaces", R.string.maintenance_wipers, maxMonthsAllowed = 12),
            MaintenanceItem("batterie", R.string.maintenance_battery, maxMonthsAllowed = 48)
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedIndexForPicker by remember { mutableStateOf<Int?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Charger les éléments personnalisés et les dates au démarrage
    LaunchedEffect(Unit) {
        val customItemsSet = MaintenancePreferences.getCustomItems(context).firstOrNull() ?: emptySet()
        customItemsSet.forEach { itemString ->
            val parts = itemString.split("|")
            if (parts.size == 2) {
                val name = parts[0]
                val months = parts[1].toLongOrNull() ?: 12L
                if (items.none { it.key == name }) {
                    items.add(MaintenanceItem(key = name, titleRes = 0, maxMonthsAllowed = months))
                }
            }
        }

        items.forEachIndexed { index, item ->
            val savedTimestamp = MaintenancePreferences.getLastDate(context, item.key).firstOrNull()
            if (savedTimestamp != null) {
                val localDate = Instant.ofEpochMilli(savedTimestamp)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                items[index] = items[index].copy(lastDoneDate = localDate)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.btn_car_maintenance)) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // En-tête du tableau réutilisant tes clés existantes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.btn_car_maintenance),
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
            }

            HorizontalDivider()

            // Liste des éléments
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items) { index, item ->
                    val dueDate = remember(item.lastDoneDate, item.maxMonthsAllowed) {
                        item.lastDoneDate?.plusMonths(item.maxMonthsAllowed)
                    }

                    val isOverdue = remember(dueDate) {
                        dueDate?.let { LocalDate.now().isAfter(it) } ?: false
                    }

                    val backgroundColor = if (isOverdue) Color(0xFFFFCDD2) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .clickable { selectedIndexForPicker = index }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Titre : ressource traduit ou nom perso
                        val titleText = if (item.titleRes != 0) stringResource(id = item.titleRes) else item.key
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1.2f),
                            color = if (isOverdue) Color(0xFFB71C1C) else Color.Unspecified
                        )

                        // Dates : Réalisée et Limite
                        Column(modifier = Modifier.weight(1.2f)) {
                            val doneDateText = item.lastDoneDate?.format(dateFormatter) ?: stringResource(id = R.string.select)

                            Text(
                                text = stringResource(id = R.string.maintenance_done, doneDateText),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.lastDoneDate != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOverdue) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary
                            )

                            if (dueDate != null) {
                                Text(
                                    text = stringResource(id = R.string.maintenance_limit, dueDate.format(dateFormatter)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverdue) Color(0xFFB71C1C) else Color.Gray
                                )
                            }
                        }

                        // Suppression pour les éléments personnalisés
                        if (item.titleRes == 0) {
                            IconButton(
                                onClick = {
                                    val itemToRemove = items[index]
                                    items.removeAt(index)
                                    coroutineScope.launch {
                                        MaintenancePreferences.removeCustomItem(
                                            context,
                                            itemToRemove.key,
                                            itemToRemove.maxMonthsAllowed
                                        )
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
                    }
                    HorizontalDivider()
                }
            }
        }

        // Dialogue d'ajout perso (+)
        if (showAddDialog) {
            AddCustomItemDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { customTitle, dateMillis, maxMonths ->
                    val localDate = Instant.ofEpochMilli(dateMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()

                    val newItem = MaintenanceItem(
                        key = customTitle,
                        titleRes = 0,
                        maxMonthsAllowed = maxMonths,
                        lastDoneDate = localDate
                    )

                    items.add(newItem)

                    coroutineScope.launch {
                        MaintenancePreferences.addCustomItem(context, customTitle, maxMonths)
                        MaintenancePreferences.saveLastDate(context, customTitle, dateMillis)
                    }

                    showAddDialog = false
                }
            )
        }

        // DatePicker pour modifier une date
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

                            items[index] = items[index].copy(lastDoneDate = selectedDate)

                            coroutineScope.launch {
                                MaintenancePreferences.saveLastDate(context, items[index].key, millis)
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
private fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, lastDateMillis: Long, maxMonths: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var maxMonthsText by remember { mutableStateOf("12") }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val selectedDateText = remember(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        Instant.ofEpochMilli(millis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.btn_car_maintenance)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(id = R.string.last_action)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = selectedDateText)
                }

                OutlinedTextField(
                    value = maxMonthsText,
                    onValueChange = { maxMonthsText = it.filter { char -> char.isDigit() } },
                    label = { Text(text = stringResource(id = R.string.mois)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && maxMonthsText.isNotBlank(),
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val months = maxMonthsText.toLongOrNull() ?: 12L
                    onConfirm(title.trim(), millis, months)
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}