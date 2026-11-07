/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.aaditya.melyvio.R
import kotlinx.coroutines.delay
import java.util.Calendar

enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

fun dayPartOf(hour: Int): DayPart =
    when (hour) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..20 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

private fun nowDayPart(): DayPart = dayPartOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

/** Day part from the phone's own clock; re-checked every minute. */
@Composable
fun rememberDayPart(): DayPart {
    val part: State<DayPart> =
        produceState(initialValue = nowDayPart()) {
            while (true) {
                delay(60_000L)
                value = nowDayPart()
            }
        }
    return part.value
}

@Composable
fun greetingBase(part: DayPart): String =
    stringResource(
        when (part) {
            DayPart.MORNING -> R.string.greeting_morning
            DayPart.AFTERNOON -> R.string.greeting_afternoon
            DayPart.EVENING -> R.string.greeting_evening
            DayPart.NIGHT -> R.string.greeting_night
        },
    )

/** "Good evening" or "Good evening, Name" (name in the theme's accent colour). */
@Composable
fun greetingAnnotated(
    name: String,
    part: DayPart,
): AnnotatedString {
    val base = greetingBase(part)
    val accent = MaterialTheme.colorScheme.primary
    val cleaned = name.trim()
    return buildAnnotatedString {
        append(base)
        if (cleaned.isNotEmpty()) {
            append(", ")
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append(cleaned) }
        }
    }
}
