/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.R

private fun glassLabelRes(level: Int): Int =
    when (level.coerceIn(0, 2)) {
        0 -> R.string.glass_level_transparent
        1 -> R.string.glass_level_soft
        else -> R.string.glass_level_frosted
    }

@Composable
fun glassLevelLabel(level: Int): String = stringResource(glassLabelRes(level))

@Composable
fun GlassLevelDialog(
    title: String,
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                (0..2).forEach { level ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelect(level)
                                    onDismiss()
                                }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current.coerceIn(0, 2) == level, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(glassLabelRes(level)))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
