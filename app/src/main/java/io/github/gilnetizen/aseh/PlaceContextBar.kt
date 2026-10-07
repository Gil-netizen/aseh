package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.PlaceContextSource

/**
 * Keeps the active place inspectable and editable from every top-level destination.
 */
@Composable
internal fun PlaceContextBar(
  placeContext: ManualPlaceContext?,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onEdit,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .testTag("app-place-context-bar"),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = if (placeContext == null) {
            stringResource(R.string.app_place_context_missing_title)
          } else if (placeContext.source == PlaceContextSource.DEVICE) {
            stringResource(R.string.app_place_context_device_label)
          } else {
            placeContext.label
          },
          style = MaterialTheme.typography.titleSmall.merge(
            TextStyle(textDirection = TextDirection.ContentOrLtr),
          ),
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.testTag(
            if (placeContext == null) {
              "app-place-context-status"
            } else {
              "app-place-context-label"
            },
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (placeContext == null) {
            stringResource(R.string.app_place_context_missing_summary)
          } else {
            placeContext.timeZoneId
          },
          style = MaterialTheme.typography.bodySmall.merge(
            TextStyle(
              textDirection = if (placeContext == null) {
                TextDirection.ContentOrLtr
              } else {
                TextDirection.Ltr
              },
            ),
          ),
          modifier = Modifier.testTag(
            if (placeContext == null) {
              "app-place-context-summary"
            } else {
              "app-place-context-time-zone"
            },
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Box(
        modifier = Modifier
          .widthIn(min = 48.dp)
          .heightIn(min = 48.dp)
          .testTag("app-place-context-action"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(
            if (placeContext == null) {
              R.string.app_place_context_setup
            } else {
              R.string.app_place_context_change
            },
          ),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
