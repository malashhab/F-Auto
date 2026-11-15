package net.prototux.aare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/** Days from now until [due] (epoch millis); negative = overdue. */
private fun daysUntil(due: Long): Long {
    val diff = due - System.currentTimeMillis()
    return TimeUnit.MILLISECONDS.toDays(diff + if (diff >= 0) TimeUnit.DAYS.toMillis(1) - 1 else 0)
}

private fun formatted(due: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(due))

/** The reminder line for a set date, plus whether it's urgent (≤14 days or overdue). Null if unset. */
@Composable
private fun reminderText(due: Long): Pair<String, Boolean>? {
    if (due <= 0L) return null
    val days = daysUntil(due)
    val date = formatted(due)
    return when {
        days < 0 -> stringResource(R.string.service_reminder_overdue, -days, date) to true
        days == 0L -> stringResource(R.string.service_reminder_due_today, date) to true
        else -> stringResource(R.string.service_reminder_due_in, date, days) to (days <= 14)
    }
}

/**
 * The service / oil-change reminder control on the Car-data settings page: a user-set next-service
 * date (ported in spirit from StradaOS's oil-change reminder) with days remaining, set via a date
 * picker. Independent of the car connection — it's a personal reminder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServiceReminderSection(settings: Settings) {
    var due by remember { mutableLongStateOf(settings.serviceDueDate) }
    var showPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        val info = reminderText(due)
        Text(
            info?.first ?: stringResource(R.string.service_reminder_none),
            style = MaterialTheme.typography.bodyMedium,
            color = if (info?.second == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { showPicker = true }) {
                Text(stringResource(if (due > 0) R.string.service_reminder_change else R.string.service_reminder_set))
            }
            if (due > 0) OutlinedButton(onClick = { due = 0; settings.serviceDueDate = 0 }) {
                Text(stringResource(R.string.service_reminder_clear))
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (due > 0) due else System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { due = it; settings.serviceDueDate = it }
                    showPicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(android.R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/**
 * A compact reminder banner for the in-car Car-data screen: shown only when a service is due within
 * two weeks or overdue, so it nudges without nagging.
 */
@Composable
internal fun ServiceReminderBanner(settings: Settings, modifier: Modifier = Modifier) {
    val due = settings.serviceDueDate
    val info = reminderText(due)
    if (info == null || !info.second) return
    val tint = MaterialTheme.colorScheme.error
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.12f)).border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Build, null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(info.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
