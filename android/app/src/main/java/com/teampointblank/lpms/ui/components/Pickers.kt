@file:OptIn(ExperimentalMaterial3Api::class)

package com.teampointblank.lpms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.teampointblank.lpms.ui.theme.LpmsColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A field that looks like a text field but opens something (a menu, a date or time picker) when tapped. */
@Composable
fun PickerField(
    label: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LpmsTextField(
            label = label,
            value = text,
            onValueChange = {},
            placeholder = placeholder,
            error = error,
            enabled = enabled,
            readOnly = true,
            trailing = {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = LpmsColors.palette.textMuted)
            }
        )
        // Transparent layer over the field, so one tap anywhere on it opens the picker.
        // (It covers the label too, which is fine: tapping the label opens the field as well.)
        Box(modifier = Modifier.matchParentSize().clickable(enabled = enabled, onClick = onClick))
    }
}

/** Pick one item from a list, shown as a menu under the field. */
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val palette = LpmsColors.palette
    Box(modifier = modifier.fillMaxWidth()) {
        PickerField(
            label = label,
            text = selected?.let(optionLabel) ?: "",
            placeholder = placeholder,
            error = error,
            enabled = enabled,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = palette.card
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), color = palette.text) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

/** Date picker limited to [minDate]..[maxDate] (either may be null for "no limit"). */
@Composable
fun LpmsDatePickerDialog(
    initial: LocalDate?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LpmsColors.palette
    val minMillis = minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE
    val maxMillis = maxDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in minMillis..maxMillis
            override fun isSelectableYear(year: Int): Boolean =
                (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
        }
    )
    val colors = DatePickerDefaults.colors(
        containerColor = palette.card,
        titleContentColor = palette.textMuted,
        headlineContentColor = palette.text,
        weekdayContentColor = palette.textMuted,
        subheadContentColor = palette.textMuted,
        navigationContentColor = palette.text,
        yearContentColor = palette.text,
        currentYearContentColor = palette.accent,
        selectedYearContainerColor = palette.accent,
        selectedYearContentColor = palette.onAccent,
        dayContentColor = palette.text,
        disabledDayContentColor = palette.textMuted.copy(alpha = 0.4f),
        selectedDayContainerColor = palette.accent,
        selectedDayContentColor = palette.onAccent,
        todayContentColor = palette.accent,
        todayDateBorderColor = palette.accent
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null
            ) { Text("OK", color = palette.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textMuted) } }
    ) {
        DatePicker(state = pickerState, colors = colors)
    }
}

/** Clock-face time picker in a dialog (24-hour clock). */
@Composable
fun LpmsTimePickerDialog(
    title: String,
    initial: LocalTime?,
    onPicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LpmsColors.palette
    val start = initial ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text(title, color = palette.text) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = palette.chip,
                    clockDialSelectedContentColor = palette.onAccent,
                    clockDialUnselectedContentColor = palette.text,
                    selectorColor = palette.accent,
                    containerColor = palette.card,
                    periodSelectorBorderColor = palette.border,
                    timeSelectorSelectedContainerColor = palette.accent,
                    timeSelectorUnselectedContainerColor = palette.chip,
                    timeSelectorSelectedContentColor = palette.onAccent,
                    timeSelectorUnselectedContentColor = palette.text
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) {
                Text("OK", color = palette.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textMuted) } }
    )
}
