package com.example.abenly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.abenly.model.MaintenanceItem
import com.example.abenly.utils.MaintenancePreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.example.abenly.R

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

    // Charger les dates sauvegardées au démarrage de l'écran
    LaunchedEffect(Unit) {
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

    var selectedIndexForPicker by remember { mutableStateOf<Int?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(id = R.string.last_action),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items) { index, item ->
                val isOverdue = remember(item.lastDoneDate) {
                    item.lastDoneDate?.let { date ->
                        ChronoUnit.MONTHS.between(date, LocalDate.now()) >= item.maxMonthsAllowed
                    } ?: false
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
                    Text(
                        text = stringResource(id = item.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (isOverdue) Color(0xFFB71C1C) else Color.Unspecified
                    )

                    Text(
                        text = item.lastDoneDate?.format(dateFormatter) ?: stringResource(id = R.string.select),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (item.lastDoneDate != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
            }
        }
    }

    selectedIndexForPicker?.let { index ->
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { selectedIndexForPicker = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Utiliser ZoneOffset.UTC pour être aligné avec Material 3 DatePicker
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()

                        // Mise à jour de l'état local immédiatement
                        items[index] = items[index].copy(lastDoneDate = selectedDate)

                        // Enregistrement asynchrone dans DataStore
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