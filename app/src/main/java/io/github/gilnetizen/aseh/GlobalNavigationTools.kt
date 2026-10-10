package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordAddress
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot

internal enum class GlobalSearchDestination {
  PRACTICE,
  PRAYER,
  STUDY_SOURCE,
  BUILD,
}

internal data class GlobalSearchEntry(
  val id: String,
  val title: String,
  val detail: String,
  val category: String,
  val destination: GlobalSearchDestination,
  val workspaceKind: WorkspaceKind? = null,
  val workspaceRecordAddress: WorkspaceRecordAddress? = null,
)

internal fun searchCatalog(
  catalog: DemonstratorCatalog?,
  query: String,
  limit: Int = 30,
): List<GlobalSearchEntry> {
  if (catalog == null) return emptyList()
  val terms = query.trim()
    .lowercase()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
  if (terms.isEmpty()) return emptyList()

  fun matches(vararg fields: String): Boolean {
    val searchable = fields.joinToString(" ").lowercase()
    return terms.all { term -> searchable.contains(term) }
  }

  return buildList {
    catalog.practiceCards.forEach { card ->
      if (
        matches(
          card.title,
          card.topic,
          card.summary,
          card.action,
          card.context,
          card.purpose,
          card.reasoning,
        )
      ) {
        add(
          GlobalSearchEntry(
            id = card.id,
            title = card.title,
            detail = card.summary,
            category = "Practice",
            destination = GlobalSearchDestination.PRACTICE,
          ),
        )
      }
    }
    if (
      matches(
        catalog.service.title,
        catalog.service.subtitle,
        catalog.service.segments.joinToString(" ") { segment ->
          "${segment.phase} ${segment.title} ${segment.summary}"
        },
      )
    ) {
      add(
        GlobalSearchEntry(
          id = catalog.service.id,
          title = catalog.service.title,
          detail = catalog.service.subtitle,
          category = "Prayer",
          destination = GlobalSearchDestination.PRAYER,
        ),
      )
    }
    catalog.sources.forEach { source ->
      if (matches(source.title, source.locator, source.body, source.language, source.edition)) {
        add(
          GlobalSearchEntry(
            id = source.id,
            title = source.title,
            detail = "${source.locator} · ${source.body}",
            category = "Source",
            destination = GlobalSearchDestination.STUDY_SOURCE,
          ),
        )
      }
    }
  }.take(limit.coerceAtLeast(0))
}

@Composable
internal fun GlobalNavigationTools(
  workspaceKind: WorkspaceKind,
  onOpenContextChooser: () -> Unit,
  onOpenSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(
        onClick = onOpenContextChooser,
        modifier = Modifier
          .weight(1f)
          .testTag("app-global-context"),
      ) {
        Text(
          text = stringResource(
            R.string.app_global_context_value,
            workspaceKind.localizedLabel(),
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      TextButton(
        onClick = onOpenSearch,
        modifier = Modifier
          .weight(1f)
          .testTag("app-global-search"),
      ) {
        Text(text = stringResource(R.string.app_global_search))
      }
    }
  }
}

@Composable
internal fun ContextChooserDialog(
  selected: WorkspaceKind,
  onSelected: (WorkspaceKind) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.app_context_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.app_context_dialog_detail),
          style = MaterialTheme.typography.bodyMedium,
        )
        WorkspaceKind.entries.forEach { kind ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .selectable(
                selected = kind == selected,
                role = Role.RadioButton,
                onClick = {
                  onSelected(kind)
                  onDismiss()
                },
              )
              .testTag("app-context-${kind.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = kind == selected,
              onClick = null,
            )
            Text(kind.localizedLabel())
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.app_close))
      }
    },
  )
}

@Composable
internal fun GlobalSearchDialog(
  catalog: DemonstratorCatalog?,
  workspaceSnapshot: WorkspaceSnapshot = WorkspaceSnapshot(),
  onOpenEntry: (GlobalSearchEntry) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val results = (
    searchWorkspace(workspaceSnapshot, query).map { entry ->
      GlobalSearchEntry(
        id = entry.id,
        title = entry.title,
        detail = entry.detail,
        category = entry.category,
        destination = GlobalSearchDestination.BUILD,
        workspaceKind = entry.workspaceKind,
        workspaceRecordAddress = entry.address,
      )
    } + searchCatalog(catalog, query)
  ).take(30)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.app_global_search)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 220.dp, max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("app-global-search-input"),
          label = { Text(stringResource(R.string.app_search_label)) },
          supportingText = { Text(stringResource(R.string.app_search_supporting)) },
          singleLine = true,
        )
        when {
          query.isBlank() -> Text(
            text = stringResource(R.string.app_search_prompt),
            style = MaterialTheme.typography.bodyMedium,
          )
          results.isEmpty() -> Text(
            text = stringResource(R.string.app_search_empty),
            style = MaterialTheme.typography.bodyMedium,
          )
          else -> LazyColumn {
            items(results, key = { entry -> "${entry.destination}:${entry.id}" }) { entry ->
              TextButton(
                onClick = { onOpenEntry(entry) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("app-global-search-result-${entry.id}"),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.Start,
                ) {
                  Text(
                    text = stringResource(R.string.app_search_english_fallback, entry.category),
                    style = MaterialTheme.typography.labelMedium,
                  )
                  Text(
                    text = "\u2068${entry.title}\u2069",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = stringResource(R.string.app_search_english_fallback, entry.detail),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
              HorizontalDivider()
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.app_close))
      }
    },
  )
}

@Composable
private fun WorkspaceKind.localizedLabel(): String = when (this) {
  WorkspaceKind.SELF -> stringResource(R.string.app_context_self)
  WorkspaceKind.HOUSEHOLD -> stringResource(R.string.app_context_household)
  WorkspaceKind.QAHAL -> stringResource(R.string.app_context_qahal)
}
