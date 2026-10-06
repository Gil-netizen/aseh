package io.github.gilnetizen.aseh.feature.now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val civilDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

private val localTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)

internal data class NowUiState(
    val civilDate: String,
    val weekday: String,
    val localTime: String,
    val timeZone: String,
)

/**
 * Creates the display state without consulting Android services or the network.
 *
 * Keeping the instant and zone explicit makes this formatter deterministic and
 * lets callers decide which clock represents "now".
 */
internal fun formatNowUiState(
    instant: Instant,
    zoneId: ZoneId,
): NowUiState {
    val zonedDateTime = instant.atZone(zoneId)
    return NowUiState(
        civilDate = civilDateFormatter.format(zonedDateTime),
        weekday = weekdayFormatter.format(zonedDateTime),
        localTime = localTimeFormatter.format(zonedDateTime),
        timeZone = zoneId.id,
    )
}

internal fun currentNowUiState(
    clock: Clock,
    timeZone: () -> ZoneId = { clock.zone },
): NowUiState =
    formatNowUiState(
        instant = clock.instant(),
        zoneId = timeZone(),
    )

internal class NowScreenState(
    private val clock: Clock,
    private val timeZone: () -> ZoneId = { clock.zone },
) {
    var snapshot by mutableStateOf(currentNowUiState(clock, timeZone))
        private set

    fun refresh() {
        snapshot = currentNowUiState(clock, timeZone)
    }
}

@Composable
fun NowScreen(
    clock: Clock,
    timeZone: () -> ZoneId = { clock.zone },
    modifier: Modifier = Modifier,
) {
    val state = remember(clock, timeZone) { NowScreenState(clock, timeZone) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 24.dp,
                    top = 32.dp,
                    end = 24.dp,
                    bottom = 32.dp,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_now_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .testTag("now-heading")
                .semantics { heading() },
        )
        Text(
            text = stringResource(R.string.feature_now_summary),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("now-summary"),
        )

        NowValue(
            label = stringResource(R.string.feature_now_civil_date_label),
            value = state.snapshot.civilDate,
            testTag = "now-date",
        )
        NowValue(
            label = stringResource(R.string.feature_now_weekday_label),
            value = state.snapshot.weekday,
            testTag = "now-weekday",
        )
        NowValue(
            label = stringResource(R.string.feature_now_local_time_label),
            value = state.snapshot.localTime,
            testTag = "now-time",
        )
        NowValue(
            label = stringResource(R.string.feature_now_time_zone_label),
            value = state.snapshot.timeZone,
            testTag = "now-time-zone",
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("now-location-status")
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_now_location_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.feature_now_location_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Button(
            onClick = state::refresh,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("now-refresh"),
        ) {
            Text(text = stringResource(R.string.feature_now_refresh))
        }
    }
}

@Composable
private fun NowValue(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.merge(
                TextStyle(textDirection = TextDirection.Ltr),
            ),
        )
    }
}

@Preview(
    name = "English phone, 200 percent",
    locale = "en",
    fontScale = 2f,
    widthDp = 360,
    heightDp = 640,
    showBackground = true,
)
@Composable
private fun NowEnglishPreview() {
    NowScreen(clock = Clock.systemDefaultZone())
}

@Preview(
    name = "Hebrew RTL tablet, English fallback",
    locale = "he",
    widthDp = 800,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun NowHebrewPreview() {
    NowScreen(clock = Clock.systemDefaultZone())
}
