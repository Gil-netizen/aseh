package io.github.gilnetizen.aseh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

/**
 * Shows the complete packet before any content leaves the app. The caller owns
 * the actual share or print action, which is reachable only through confirm.
 */
@Composable
fun PacketExportPreviewDialog(
    packet: String,
    title: String,
    accessibleEquivalentLabel: String,
    disclosure: String,
    confirmLabel: String,
    cancelLabel: String,
    testTagPrefix: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("$testTagPrefix-dialog"),
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = accessibleEquivalentLabel,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("$testTagPrefix-accessible-equivalent"),
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = packet,
                        style = MaterialTheme.typography.bodySmall.merge(
                            TextStyle(textDirection = TextDirection.ContentOrLtr),
                        ),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .testTag("$testTagPrefix-payload"),
                    )
                }
                Text(
                    text = disclosure,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("$testTagPrefix-confirm"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("$testTagPrefix-cancel"),
            ) {
                Text(cancelLabel)
            }
        },
    )
}
