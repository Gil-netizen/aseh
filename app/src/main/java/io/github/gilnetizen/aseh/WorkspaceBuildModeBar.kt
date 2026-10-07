package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkspaceBuildModeBar(
  serviceSetupSelected: Boolean,
  fixtureNotice: String,
  onOpenWorkspace: () -> Unit,
  onOpenServiceSetup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = modifier
      .fillMaxWidth()
      .testTag("workspace-mode-bar"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = if (serviceSetupSelected) {
          stringResource(R.string.workspace_mode_service_title)
        } else {
          stringResource(R.string.workspace_mode_operations_title)
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )
      if (!serviceSetupSelected) {
        Text(
          text = fixtureNotice,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("workspace-fixture-notice"),
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (serviceSetupSelected) {
          Button(
            onClick = onOpenWorkspace,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("workspace-open-dashboard"),
          ) {
            Text(stringResource(R.string.workspace_mode_operations_action))
          }
          OutlinedButton(
            onClick = onOpenServiceSetup,
            enabled = false,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp),
          ) {
            Text(stringResource(R.string.workspace_mode_service_action))
          }
        } else {
          OutlinedButton(
            onClick = onOpenWorkspace,
            enabled = false,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp),
          ) {
            Text(stringResource(R.string.workspace_mode_operations_action))
          }
          Button(
            onClick = onOpenServiceSetup,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("workspace-open-service-setup"),
          ) {
            Text(stringResource(R.string.workspace_mode_service_action))
          }
        }
      }
    }
  }
}
