package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.core.ui.AsehAdaptiveNavigationShell
import io.github.gilnetizen.aseh.core.ui.AsehDestination
import io.github.gilnetizen.aseh.feature.build.BuildScreen
import io.github.gilnetizen.aseh.feature.now.NowScreen
import io.github.gilnetizen.aseh.feature.now.NowPlaceContext
import io.github.gilnetizen.aseh.feature.practice.PracticeScreen
import io.github.gilnetizen.aseh.feature.prayer.PrayerScreen
import io.github.gilnetizen.aseh.feature.study.StudyScreen
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.collect

@Composable
fun AsehApp(
  clock: Clock,
  deviceTimeZone: () -> ZoneId,
  interfacePreferencesRepository: InterfacePreferencesRepository,
  manualPlaceContextRepository: ManualPlaceContextRepository,
  onSelectedDestinationChanged: (String) -> Unit,
) {
  val loadedPreferences: InterfacePreferences? by
    interfacePreferencesRepository.preferences.collectAsState(initial = null)
  val storedPreferences = loadedPreferences ?: InterfacePreferences()
  var storedManualPlaceContext by remember(manualPlaceContextRepository) {
    mutableStateOf<ManualPlaceContext?>(null)
  }
  var manualPlaceContextReady by remember(manualPlaceContextRepository) {
    mutableStateOf(false)
  }

  LaunchedEffect(manualPlaceContextRepository) {
    manualPlaceContextRepository.context.collect { context ->
      storedManualPlaceContext = context
      manualPlaceContextReady = true
    }
  }
  // A local choice takes precedence after the persisted value hydrates. The
  // repository writer is asynchronous, so reflecting every later emission
  // here could replay an older destination over a newer tap.
  var selectedDestinationOverride by rememberSaveable {
    mutableStateOf<String?>(null)
  }
  var openPlaceEditorRequest by rememberSaveable { mutableIntStateOf(0) }

  val selectedDestination = AsehDestination.fromPersistedId(
    selectedDestinationOverride ?: storedPreferences.selectedDestinationId,
  )
  val currentDensity = LocalDensity.current
  val scaledDensity = Density(
    density = currentDensity.density,
    fontScale = currentDensity.fontScale * storedPreferences.textScale,
  )

  CompositionLocalProvider(LocalDensity provides scaledDensity) {
    AsehTheme {
      AsehAdaptiveNavigationShell(
        selectedDestination = selectedDestination,
        onDestinationSelected = { destination ->
          selectedDestinationOverride = destination.persistedId
          if (destination != AsehDestination.NOW) {
            openPlaceEditorRequest = 0
          }
          onSelectedDestinationChanged(destination.persistedId)
        },
        modifier = Modifier.testTag("aseh-root"),
      ) { destination ->
        Column(modifier = Modifier.fillMaxSize()) {
          if (manualPlaceContextReady && destination != AsehDestination.NOW) {
            PlaceContextBar(
              placeContext = storedManualPlaceContext,
              onEdit = {
                selectedDestinationOverride = AsehDestination.NOW.persistedId
                onSelectedDestinationChanged(AsehDestination.NOW.persistedId)
                openPlaceEditorRequest += 1
              },
            )
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          ) {
            when (destination) {
              AsehDestination.NOW -> NowScreen(
                clock = clock,
                timeZone = deviceTimeZone,
                placeContextReady = manualPlaceContextReady,
                placeContext = storedManualPlaceContext?.toNowPlaceContext(),
                openEditorRequest = openPlaceEditorRequest,
                onOpenEditorRequestConsumed = { openPlaceEditorRequest = 0 },
                onSavePlaceContext = { context ->
                  manualPlaceContextRepository.save(context.toStoredManualPlaceContext())
                },
                onClearPlaceContext = manualPlaceContextRepository::clear,
              )
              AsehDestination.PRACTICE -> PracticeScreen()
              AsehDestination.PRAYER -> PrayerScreen()
              AsehDestination.STUDY -> StudyScreen()
              AsehDestination.BUILD -> BuildScreen()
            }
          }
        }
      }
    }
  }
}

private fun ManualPlaceContext.toNowPlaceContext(): NowPlaceContext = NowPlaceContext(
  label = label,
  latitudeDegrees = latitudeDegrees,
  longitudeDegrees = longitudeDegrees,
  elevationMeters = elevationMeters,
  timeZoneId = timeZoneId,
)

private fun NowPlaceContext.toStoredManualPlaceContext(): ManualPlaceContext = ManualPlaceContext(
  label = label,
  latitudeDegrees = latitudeDegrees,
  longitudeDegrees = longitudeDegrees,
  elevationMeters = elevationMeters,
  timeZoneId = timeZoneId,
)
