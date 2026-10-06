package io.github.gilnetizen.aseh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val NavigationRailBreakpoint = 600.dp

/**
 * Five-destination navigation that keeps the same logical order in LTR and RTL.
 *
 * Compact windows use equal-width, wrap-content tabs. Wider windows use an
 * adaptive rail at logical start, which Android mirrors in RTL. Neither layout
 * fixes its item height, so labels can reflow when font scaling reaches 200%.
 * The content slot is already inset from app chrome and receives the selected
 * destination.
 */
@Composable
fun AsehAdaptiveNavigationShell(
    selectedDestination: AsehDestination,
    onDestinationSelected: (AsehDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (AsehDestination) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= NavigationRailBreakpoint
        val navigationRailWidth = (maxWidth * 0.30f).coerceIn(120.dp, 200.dp)

        Scaffold(
            bottomBar = {
                if (!useNavigationRail) {
                    AsehCompactNavigation(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = onDestinationSelected,
                    )
                }
            },
        ) { innerPadding ->
            if (useNavigationRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    AsehNavigationRail(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = onDestinationSelected,
                        modifier = Modifier
                            .width(navigationRailWidth),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("navigation-content"),
                    ) {
                        content(selectedDestination)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("navigation-content"),
                ) {
                    content(selectedDestination)
                }
            }
        }
    }
}

@Composable
private fun AsehCompactNavigation(
    selectedDestination: AsehDestination,
    onDestinationSelected: (AsehDestination) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .selectableGroup()
                .testTag("navigation-compact")
                .padding(horizontal = 2.dp, vertical = 4.dp),
        ) {
            AsehDestination.entries.forEach { destination ->
                AsehCompactDestination(
                    destination = destination,
                    selected = destination == selectedDestination,
                    onSelected = { onDestinationSelected(destination) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun AsehCompactDestination(
    destination: AsehDestination,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(destination.labelRes)
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .testTag(destination.testTag)
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.Tab,
            )
            .semantics {
                contentDescription = label
            },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            DestinationSymbol(destination)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun AsehNavigationRail(
    selectedDestination: AsehDestination,
    onDestinationSelected: (AsehDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .testTag("navigation-rail"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .selectableGroup()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AsehDestination.entries.forEach { destination ->
                AsehRailDestination(
                    destination = destination,
                    selected = destination == selectedDestination,
                    onSelected = { onDestinationSelected(destination) },
                )
            }
        }
    }
}

@Composable
private fun AsehRailDestination(
    destination: AsehDestination,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val label = stringResource(destination.labelRes)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(destination.testTag)
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.Tab,
            )
            .semantics {
                contentDescription = label
            },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DestinationSymbol(destination)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun DestinationSymbol(destination: AsehDestination) {
    Text(
        text = stringResource(destination.symbolRes),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

private val AsehDestination.testTag: String
    get() = "destination-$persistedId"
