package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
    modifier = modifier
      .fillMaxWidth()
      .testTag("app-place-context-bar"),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    tonalElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = stringResource(
          if (placeContext == null) {
            R.string.app_place_context_missing_title
          } else {
            R.string.app_place_context_active_title
          },
        ),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag("app-place-context-status"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (placeContext == null) {
        Text(
          text = stringResource(R.string.app_place_context_missing_summary),
          style = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(textDirection = TextDirection.ContentOrLtr),
          ),
          modifier = Modifier.testTag("app-place-context-summary"),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      } else {
        // Keep the user-entered label and the LTR IANA identifier in separate
        // paragraphs so either script can retain its natural bidi ordering.
        Text(
          text = if (placeContext.source == PlaceContextSource.DEVICE) {
            stringResource(R.string.app_place_context_device_label)
          } else {
            placeContext.label
          },
          style = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(textDirection = TextDirection.ContentOrLtr),
          ),
          modifier = Modifier.testTag("app-place-context-label"),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = placeContext.timeZoneId,
          style = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(textDirection = TextDirection.Ltr),
          ),
          modifier = Modifier.testTag("app-place-context-time-zone"),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Button(
        onClick = onEdit,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("app-place-context-action"),
      ) {
        Text(
          text = stringResource(
            if (placeContext == null) {
              R.string.app_place_context_setup
            } else {
              R.string.app_place_context_change
            },
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
