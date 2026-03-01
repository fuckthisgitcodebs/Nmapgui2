package com.example.nmapgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.nmapgui.ui.theme.DarkCard
import com.example.nmapgui.ui.theme.Green400

@Composable
fun CommandPreview(command: String, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val scroll = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$ ",
                style = MaterialTheme.typography.bodySmall,
                color = Green400,
            )
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scroll),
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(command)) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy command",
                    tint = Green400,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
