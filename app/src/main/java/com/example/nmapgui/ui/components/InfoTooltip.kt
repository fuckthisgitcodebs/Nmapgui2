package com.example.nmapgui.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoIcon(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    var show by remember { mutableStateOf(false) }

    Icon(
        imageVector = Icons.Default.Info,
        contentDescription = "Info: $title",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .size(18.dp)
            .clickable { show = true },
    )

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { show = false }) { Text("Got it") }
            },
        )
    }
}
