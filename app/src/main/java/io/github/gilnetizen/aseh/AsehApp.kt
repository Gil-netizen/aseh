package io.github.gilnetizen.aseh

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.core.ui.AsehAdaptiveNavigationShell
import io.github.gilnetizen.aseh.core.ui.AsehDestination
import io.github.gilnetizen.aseh.feature.build.BuildScreen
import io.github.gilnetizen.aseh.feature.now.NowScreen
import io.github.gilnetizen.aseh.feature.now.NowPlaceContext
import io.github.gilnetizen.aseh.feature.now.NowPlaceSource
import io.github.gilnetizen.aseh.feature.now.DeviceLocationUiState
import io.github.gilnetizen.aseh.feature.practice.PracticeScreen
import io.github.gilnetizen.aseh.feature.prayer.PrayerScreen
import io.github.gilnetizen.aseh.feature.study.StudyScreen
import io.github.gilnetizen.aseh.location.DeviceLocationClient
import io.github.gilnetizen.aseh.location.DeviceLocationOutcome
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun AsehApp(
  clock: Clock,
  deviceTimeZone: () -> ZoneId,
  interfacePreferencesRepository: InterfacePreferencesRepository,
  manualPlaceContextRepository: ManualPlaceContextRepository,
  deviceLocationClient: DeviceLocationClient? = null,
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
  var requestedLocationPermissionBefore by rememberSaveable { mutableStateOf(false) }
  var permissionWasPreviouslyRequestedForCurrentLaunch by rememberSaveable {
    mutableStateOf(false)
  }
  var deviceLocationState by remember { mutableStateOf<DeviceLocationUiState>(DeviceLocationUiState.Idle) }
  var locationRequestJob by remember { mutableStateOf<Job?>(null) }
  val locationScope = rememberCoroutineScope()
  val context = LocalContext.current
  val activity = context.findActivity()
  val lifecycleOwner = LocalLifecycleOwner.current

  val acquireDeviceLocation: () -> Unit = {
    locationRequestJob?.cancel()
    if (deviceLocationClient == null) {
      deviceLocationState = DeviceLocationUiState.Unavailable
    } else {
      deviceLocationState = DeviceLocationUiState.Locating
      locationRequestJob = locationScope.launch {
        deviceLocationState = when (val outcome = deviceLocationClient.getCurrentLocation()) {
          is DeviceLocationOutcome.Success -> DeviceLocationUiState.Preview(
            context = NowPlaceContext(
              label = "",
              latitudeDegrees = outcome.fix.latitudeDegrees,
              longitudeDegrees = outcome.fix.longitudeDegrees,
              elevationMeters = outcome.fix.altitudeMeters,
              timeZoneId = deviceTimeZone().id,
              source = NowPlaceSource.DEVICE,
              horizontalAccuracyMeters = outcome.fix.horizontalAccuracyMeters,
            ),
            isApproximate = outcome.fix.isApproximate,
          )
          DeviceLocationOutcome.PermissionDenied -> DeviceLocationUiState.PermissionDenied(
            openSettingsRequired = requestedLocationPermissionBefore &&
              activity?.hasNoLocationPermissionRationale() == true,
          )
          DeviceLocationOutcome.LocationDisabled -> DeviceLocationUiState.LocationDisabled
          DeviceLocationOutcome.Timeout -> DeviceLocationUiState.TimedOut
          DeviceLocationOutcome.Unavailable,
          DeviceLocationOutcome.Failure,
          -> DeviceLocationUiState.Unavailable
        }
        locationRequestJob = null
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_STOP -> {
          locationRequestJob?.cancel()
          locationRequestJob = null
          if (deviceLocationState == DeviceLocationUiState.Locating) {
            deviceLocationState = DeviceLocationUiState.Idle
          }
        }
        Lifecycle.Event.ON_RESUME -> {
          val permissionState = deviceLocationState as? DeviceLocationUiState.PermissionDenied
          if (permissionState?.openSettingsRequired == true &&
            context.hasForegroundLocationPermission()
          ) {
            acquireDeviceLocation()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      locationRequestJob?.cancel()
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    val permissionGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
      result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      context.hasForegroundLocationPermission()
    if (permissionGranted) {
      acquireDeviceLocation()
    } else {
      deviceLocationState = DeviceLocationUiState.PermissionDenied(
        openSettingsRequired = permissionWasPreviouslyRequestedForCurrentLaunch &&
          activity?.hasNoLocationPermissionRationale() == true,
      )
    }
  }

  val requestDeviceLocation: () -> Unit = {
    if (context.hasForegroundLocationPermission()) {
      acquireDeviceLocation()
    } else {
      permissionWasPreviouslyRequestedForCurrentLaunch =
        requestedLocationPermissionBefore
      requestedLocationPermissionBefore = true
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
        ),
      )
    }
  }

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
            locationRequestJob?.cancel()
            locationRequestJob = null
            deviceLocationState = DeviceLocationUiState.Idle
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
                deviceLocationState = deviceLocationState,
                onRequestDeviceLocation = requestDeviceLocation,
                onCancelDeviceLocation = {
                  locationRequestJob?.cancel()
                  locationRequestJob = null
                  deviceLocationState = DeviceLocationUiState.Idle
                },
                onOpenAppSettings = {
                  context.startActivity(
                    Intent(
                      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                      Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                  )
                },
                onOpenLocationSettings = {
                  context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                  )
                },
                onSavePlaceContext = { context ->
                  manualPlaceContextRepository.save(context.toStoredManualPlaceContext())
                  deviceLocationState = DeviceLocationUiState.Idle
                },
                onClearPlaceContext = {
                  manualPlaceContextRepository.clear()
                  deviceLocationState = DeviceLocationUiState.Idle
                },
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
  source = when (source) {
    PlaceContextSource.MANUAL -> NowPlaceSource.MANUAL
    PlaceContextSource.DEVICE -> NowPlaceSource.DEVICE
  },
  horizontalAccuracyMeters = horizontalAccuracyMeters,
)

private fun NowPlaceContext.toStoredManualPlaceContext(): ManualPlaceContext = ManualPlaceContext(
  label = label,
  latitudeDegrees = latitudeDegrees,
  longitudeDegrees = longitudeDegrees,
  elevationMeters = elevationMeters,
  timeZoneId = timeZoneId,
  source = when (source) {
    NowPlaceSource.MANUAL -> PlaceContextSource.MANUAL
    NowPlaceSource.DEVICE -> PlaceContextSource.DEVICE
  },
  horizontalAccuracyMeters = horizontalAccuracyMeters,
)

private fun Context.hasForegroundLocationPermission(): Boolean =
  ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.ACCESS_COARSE_LOCATION,
  ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

private fun Activity.hasNoLocationPermissionRationale(): Boolean =
  !ActivityCompat.shouldShowRequestPermissionRationale(
    this,
    Manifest.permission.ACCESS_COARSE_LOCATION,
  ) && !ActivityCompat.shouldShowRequestPermissionRationale(
    this,
    Manifest.permission.ACCESS_FINE_LOCATION,
  )

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
