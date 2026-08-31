/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.miguelthemann.remarkable.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DigitalClock(
    now: ZonedDateTime,
    use24Hour: Boolean,
    showSeconds: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val pattern = buildString {
        append(if (use24Hour) "HH:mm" else "h:mm")
        if (showSeconds) append(":ss")
        if (!use24Hour) append(" a")
    }
    val formatted = now.format(DateTimeFormatter.ofPattern(pattern))
    val description = stringResource(R.string.clock_content_description, formatted)
    Column(
        modifier = if (compact) modifier else modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = when {
                    compact -> 42.sp
                    showSeconds -> 64.sp
                    else -> 80.sp
                },
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}
