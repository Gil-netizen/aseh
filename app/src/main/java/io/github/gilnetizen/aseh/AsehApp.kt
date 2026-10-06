package io.github.gilnetizen.aseh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.core.ui.AsehAdaptiveNavigationShell
import io.github.gilnetizen.aseh.core.ui.AsehDestination
import io.github.gilnetizen.aseh.feature.build.BuildScreen
import io.github.gilnetizen.aseh.feature.now.NowScreen
import io.github.gilnetizen.aseh.feature.practice.PracticeScreen
import io.github.gilnetizen.aseh.feature.prayer.PrayerScreen
import io.github.gilnetizen.aseh.feature.study.StudyScreen
import java.time.Clock
import java.time.ZoneId

@Composable
fun AsehApp(
  clock: Clock,
  deviceTimeZone: () -> ZoneId,
  interfacePreferencesRepository: InterfacePreferencesRepository,
  onSelectedDestinationChanged: (String) -> Unit,
) {
  val storedPreferences by interfacePreferencesRepository.preferences.collectAsState(
    initial = InterfacePreferences(),
  )
  var selectedDestinationId by rememberSaveable {
    mutableStateOf(storedPreferences.selectedDestinationId)
  }

  LaunchedEffect(storedPreferences.selectedDestinationId) {
    selectedDestinationId = storedPreferences.selectedDestinationId
  }

  val selectedDestination = AsehDestination.fromPersistedId(selectedDestinationId)
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
          selectedDestinationId = destination.persistedId
          onSelectedDestinationChanged(destination.persistedId)
        },
        modifier = Modifier.testTag("aseh-root"),
      ) { destination ->
        when (destination) {
          AsehDestination.NOW -> NowScreen(
            clock = clock,
            timeZone = deviceTimeZone,
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
