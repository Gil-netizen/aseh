package io.github.gilnetizen.aseh

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import io.github.gilnetizen.aseh.core.database.ExperienceStateRepository
import io.github.gilnetizen.aseh.core.database.LocalUserDataDeletion
import io.github.gilnetizen.aseh.core.database.LocalUserDataMutationGate
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.IndividualPrayerService
import io.github.gilnetizen.aseh.core.model.ServiceCoordinates
import io.github.gilnetizen.aseh.core.model.ServiceDateContext
import io.github.gilnetizen.aseh.core.model.ServiceDayKind
import io.github.gilnetizen.aseh.core.model.ServiceHebrewDateContext
import io.github.gilnetizen.aseh.core.model.ServiceInstanceKey
import io.github.gilnetizen.aseh.core.model.ServicePlaceAvailability
import io.github.gilnetizen.aseh.core.model.ServiceSolarEvent
import io.github.gilnetizen.aseh.core.model.ServiceSolarEventKind
import io.github.gilnetizen.aseh.core.model.ServiceZmanimContext
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.core.model.assembleService
import io.github.gilnetizen.aseh.core.model.forServiceInstance
import io.github.gilnetizen.aseh.core.model.nextShabbat
import io.github.gilnetizen.aseh.core.model.practiceProgress
import io.github.gilnetizen.aseh.core.model.serviceAssemblyContext
import io.github.gilnetizen.aseh.core.model.serviceDateLabel
import io.github.gilnetizen.aseh.core.model.serviceProgress
import io.github.gilnetizen.aseh.core.model.suggestedIndividualPrayerKind
import io.github.gilnetizen.aseh.core.model.withoutServiceInstanceProgress
import io.github.gilnetizen.aseh.core.ui.AsehAdaptiveNavigationShell
import io.github.gilnetizen.aseh.core.ui.AsehDestination
import io.github.gilnetizen.aseh.feature.build.BuildScreen
import io.github.gilnetizen.aseh.feature.now.NowScreen
import io.github.gilnetizen.aseh.feature.now.NowJourneyAction
import io.github.gilnetizen.aseh.feature.now.NowJourneySummary
import io.github.gilnetizen.aseh.feature.now.NowWorkspaceDueItem
import io.github.gilnetizen.aseh.feature.now.NowWorkspaceSummary
import io.github.gilnetizen.aseh.feature.now.NowPlaceContext
import io.github.gilnetizen.aseh.feature.now.NowPlaceSource
import io.github.gilnetizen.aseh.feature.now.DeviceLocationUiState
import io.github.gilnetizen.aseh.feature.practice.PracticeScreen
import io.github.gilnetizen.aseh.feature.prayer.IndividualPrayerScreen
import io.github.gilnetizen.aseh.feature.prayer.PrayerScreen
import io.github.gilnetizen.aseh.feature.study.StudyScreen
import io.github.gilnetizen.aseh.feature.workspace.ActiveWorkspaceContext
import io.github.gilnetizen.aseh.feature.workspace.WorkspaceDashboard
import io.github.gilnetizen.aseh.feature.workspace.WorkspaceDashboardFeedback
import io.github.gilnetizen.aseh.location.DeviceLocationClient
import io.github.gilnetizen.aseh.location.DeviceLocationOutcome
import io.github.gilnetizen.aseh.domain.zmanim.HebrewDateCalculation
import io.github.gilnetizen.aseh.domain.zmanim.KosherJavaZmanimEngine
import io.github.gilnetizen.aseh.domain.zmanim.ZmanCalculation
import io.github.gilnetizen.aseh.domain.zmanim.ZmanimEventType
import io.github.gilnetizen.aseh.domain.zmanim.ZmanimRequest
import io.github.gilnetizen.aseh.domain.calendar.CalendarContextRangeRequest
import io.github.gilnetizen.aseh.domain.calendar.KosherJavaCalendarContextEngine
import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarRegion as ServiceCalendarRegion
import io.github.gilnetizen.aseh.domain.servicecatalog.DatedServiceInstance
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceScheduleSelector
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceCatalog
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceDayKind as ScheduledServiceDayKind
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceKind
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceOperationalCapability
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionRequest
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceTemporalState
import io.github.gilnetizen.aseh.domain.workspace.DueTiming
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceOperationResult
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssue
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordAddress
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AsehApp(
  clock: Clock,
  deviceTimeZone: () -> ZoneId,
  interfacePreferencesRepository: InterfacePreferencesRepository,
  manualPlaceContextRepository: ManualPlaceContextRepository,
  experienceStateRepository: ExperienceStateRepository? = null,
  contentCatalog: DemonstratorCatalog? = null,
  individualPrayerServices: List<IndividualPrayerService> = emptyList(),
  serviceCatalog: ServiceCatalog? = null,
  workspaceReviewStateRepository: WorkspaceReviewStateRepository? = null,
  deviceLocationClient: DeviceLocationClient? = null,
  userDataMutationGate: LocalUserDataMutationGate? = null,
  onSelectedDestinationChanged: ((String) -> Unit)? = null,
  onUserDataMutation: (((suspend () -> Unit) -> Unit))? = null,
) {
  val loadedPreferences: InterfacePreferences? by
    interfacePreferencesRepository.preferences.collectAsState(initial = null)
  val storedPreferences = loadedPreferences ?: InterfacePreferences()
  val experienceStateFlow = remember(experienceStateRepository) {
    experienceStateRepository?.state ?: flowOf(ExperienceState())
  }
  val persistedExperienceState by experienceStateFlow.collectAsState(initial = ExperienceState())
  val workspaceStateFlow = remember(workspaceReviewStateRepository) {
    workspaceReviewStateRepository?.state ?: flowOf(WorkspaceSnapshot())
  }
  val workspaceSnapshot by workspaceStateFlow.collectAsState(initial = WorkspaceSnapshot())
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
  var requestedPracticeCardId by rememberSaveable { mutableStateOf<String?>(null) }
  var requestedStudySourceId by rememberSaveable { mutableStateOf<String?>(null) }
  var requestedWorkspaceId by rememberSaveable { mutableStateOf<String?>(null) }
  var requestedWorkspaceRecordId by rememberSaveable { mutableStateOf<String?>(null) }
  var requestedWorkspaceParentRecordId by rememberSaveable { mutableStateOf<String?>(null) }
  var requestedWorkspaceRecordKind by rememberSaveable { mutableStateOf<String?>(null) }
  var activeWorkspaceIdOverride by rememberSaveable { mutableStateOf<String?>(null) }
  var globalSearchOpen by rememberSaveable { mutableStateOf(false) }
  var contextChooserOpen by rememberSaveable { mutableStateOf(false) }
  var serviceSetupSelected by rememberSaveable { mutableStateOf(false) }
  var workspaceEditorOpen by rememberSaveable { mutableStateOf(false) }
  var dismissWorkspaceEditorRequest by rememberSaveable { mutableIntStateOf(0) }
  var workspaceRejectedIssues by remember { mutableStateOf<List<WorkspaceIssue>>(emptyList()) }
  var workspacePersistenceFailed by remember { mutableStateOf(false) }
  var selectedScheduledServiceIdOverride by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedIndividualPrayerServiceIdOverride by rememberSaveable {
    mutableStateOf<String?>(null)
  }
  var requestedLocationPermissionBefore by rememberSaveable { mutableStateOf(false) }
  var permissionWasPreviouslyRequestedForCurrentLaunch by rememberSaveable {
    mutableStateOf(false)
  }
  var deviceLocationState by remember { mutableStateOf<DeviceLocationUiState>(DeviceLocationUiState.Idle) }
  var locationRequestJob by remember { mutableStateOf<Job?>(null) }
  val locationScope = rememberCoroutineScope()
  val localUserDataMutationGate = remember(userDataMutationGate) {
    userDataMutationGate ?: LocalUserDataMutationGate()
  }
  val launchUserDataMutation: (suspend () -> Unit) -> Unit = { mutation ->
    if (onUserDataMutation != null) {
      onUserDataMutation(mutation)
    } else {
      locationScope.launch {
        localUserDataMutationGate.mutate(mutation)
      }
    }
  }
  var isDeletingLocalUserData by remember { mutableStateOf(false) }
  var localUserDataDeletionFailed by remember { mutableStateOf(false) }
  val localUserDataDeletion = remember(
    experienceStateRepository,
    manualPlaceContextRepository,
    interfacePreferencesRepository,
  ) {
    LocalUserDataDeletion(
      experienceStateRepository = experienceStateRepository,
      manualPlaceContextRepository = manualPlaceContextRepository,
      interfacePreferencesRepository = interfacePreferencesRepository,
    )
  }
  val context = LocalContext.current
  val activity = context.findActivity()
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(activity, persistedExperienceState.accessibilityProfile.keepScreenAwake) {
    val keepScreenAwake = persistedExperienceState.accessibilityProfile.keepScreenAwake
    if (keepScreenAwake) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      if (keepScreenAwake) {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }

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
  val navigateTo: (AsehDestination) -> Unit = navigate@{ destination ->
    if (isDeletingLocalUserData || localUserDataMutationGate.isDeletionRequested) {
      return@navigate
    }
    selectedDestinationOverride = destination.persistedId
    if (destination != AsehDestination.BUILD) workspaceEditorOpen = false
    if (destination != AsehDestination.NOW) {
      openPlaceEditorRequest = 0
      locationRequestJob?.cancel()
      locationRequestJob = null
      deviceLocationState = DeviceLocationUiState.Idle
    }
    if (onSelectedDestinationChanged != null) {
      onSelectedDestinationChanged(destination.persistedId)
    } else {
      launchUserDataMutation {
        interfacePreferencesRepository.setSelectedDestinationId(destination.persistedId)
      }
    }
  }
  BackHandler(
    enabled = isDeletingLocalUserData ||
      workspaceEditorOpen ||
      selectedDestination != AsehDestination.NOW,
  ) {
    if (!isDeletingLocalUserData) {
      when {
        selectedDestination == AsehDestination.BUILD && workspaceEditorOpen -> {
          dismissWorkspaceEditorRequest += 1
        }
        selectedDestination == AsehDestination.BUILD && serviceSetupSelected -> {
          serviceSetupSelected = false
        }
        else -> navigateTo(AsehDestination.NOW)
      }
    }
  }
  val experienceZone = storedManualPlaceContext?.timeZoneId?.let(ZoneId::of) ?: deviceTimeZone()
  var serviceScheduleInstant by remember(clock) { mutableStateOf(clock.instant()) }
  LaunchedEffect(clock) {
    while (true) {
      serviceScheduleInstant = clock.instant()
      delay(60_000L)
    }
  }
  var experienceLocalDate by remember(clock, experienceZone) {
    mutableStateOf(clock.instant().atZone(experienceZone).toLocalDate())
  }
  LaunchedEffect(clock, experienceZone) {
    while (true) {
      val now = clock.instant()
      experienceLocalDate = now.atZone(experienceZone).toLocalDate()
      val nextLocalDay = experienceLocalDate
        .plusDays(1)
        .atStartOfDay(experienceZone)
        .toInstant()
      delay(Duration.between(now, nextLocalDay).toMillis().coerceAtLeast(1L))
    }
  }
  DisposableEffect(lifecycleOwner, clock, experienceZone) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        experienceLocalDate = clock.instant().atZone(experienceZone).toLocalDate()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  val serviceScheduleSelector = remember(serviceCatalog) {
    serviceCatalog?.let(::ServiceScheduleSelector)
  }
  val localCalendarContextEngine = remember { KosherJavaCalendarContextEngine() }
  val serviceCalendarRegion = when (persistedExperienceState.calendarRegion) {
    CalendarRegion.ISRAEL -> ServiceCalendarRegion.ISRAEL
    CalendarRegion.DIASPORA -> ServiceCalendarRegion.DIASPORA
    CalendarRegion.UNSPECIFIED -> null
  }
  val calendarContextRange = remember(
    localCalendarContextEngine,
    experienceLocalDate,
    serviceCalendarRegion,
  ) {
    serviceCalendarRegion?.let { region ->
      localCalendarContextEngine.resolveRange(
        CalendarContextRangeRequest(
          startDate = experienceLocalDate,
          endDateInclusive = experienceLocalDate.plusDays(
            ServiceSelectionRequest.DEFAULT_LOOK_AHEAD_DAYS.toLong(),
          ),
          calendarRegion = region,
        ),
      )
    }
  }
  // An incomplete local-calendar range must not silently fall back to the
  // development Saturday/weekday classifier in ServiceScheduleSelector.
  val calendarOverrides = calendarContextRange
    ?.takeIf { range -> range.unavailableDays.isEmpty() }
    ?.serviceCatalogOverrides
  val serviceOpinionProfileId = when (persistedExperienceState.calendarRegion) {
    CalendarRegion.ISRAEL -> serviceCatalog?.opinionProfiles
      ?.firstOrNull { profile -> profile.calendarRegion.name == CalendarRegion.ISRAEL.name }
      ?.id
    CalendarRegion.DIASPORA -> serviceCatalog?.opinionProfiles
      ?.firstOrNull { profile -> profile.calendarRegion.name == CalendarRegion.DIASPORA.name }
      ?.id
    CalendarRegion.UNSPECIFIED -> null
  }
  val serviceSelection = remember(
    serviceScheduleSelector,
    serviceScheduleInstant,
    storedManualPlaceContext?.timeZoneId,
    serviceOpinionProfileId,
    calendarOverrides,
    calendarContextRange?.unavailableDays,
  ) {
    serviceScheduleSelector?.takeIf {
      calendarContextRange == null || calendarOverrides != null
    }?.select(
      ServiceSelectionRequest(
        now = serviceScheduleInstant,
        zoneId = storedManualPlaceContext?.timeZoneId?.let(ZoneId::of),
        opinionProfileId = serviceOpinionProfileId,
        calendarOverrides = calendarOverrides.orEmpty(),
      ),
    )
  }
  val requestedScheduledServiceId =
    selectedScheduledServiceIdOverride ?: persistedExperienceState.selectedServicePlanId
  val selectedScheduledService = remember(serviceSelection, requestedScheduledServiceId) {
    selectScheduledService(
      selection = serviceSelection,
      requestedServiceId = requestedScheduledServiceId,
    )
  }
  val selectedScheduledServiceId =
    selectedScheduledService?.id ?: requestedScheduledServiceId
  val selectedServiceInstanceKey = selectedScheduledService
    ?.rehearsalServiceInstanceKeyOrNull()
  val suggestedPrayerKind = suggestedIndividualPrayerKind(
    serviceScheduleInstant.atZone(experienceZone).toLocalTime(),
  )
  val suggestedIndividualPrayerService = remember(
    individualPrayerServices,
    suggestedPrayerKind,
  ) {
    individualPrayerServices.firstOrNull { service ->
      service.kind == suggestedPrayerKind
    }
  }
  val selectedIndividualPrayerService = remember(
    individualPrayerServices,
    selectedIndividualPrayerServiceIdOverride,
    suggestedIndividualPrayerService,
  ) {
    selectedIndividualPrayerServiceIdOverride?.let { selectedId ->
      individualPrayerServices.firstOrNull { service -> service.id == selectedId }
    } ?: suggestedIndividualPrayerService ?: individualPrayerServices.firstOrNull()
  }
  val individualPrayerInstanceKey = remember(
    selectedIndividualPrayerService,
    experienceLocalDate,
  ) {
    selectedIndividualPrayerService?.let { service ->
      individualPrayerServiceInstanceKey(service, experienceLocalDate)
    }
  }
  val requestedActiveServiceInstanceKey = if (
    selectedDestination == AsehDestination.PRAYER && individualPrayerInstanceKey != null
  ) {
    individualPrayerInstanceKey
  } else {
    selectedServiceInstanceKey
  }
  val selectedServiceSupportsRehearsal = selectedServiceInstanceKey != null
  val writableServiceInstanceKey = writableServiceInstanceKey(
    selected = selectedServiceInstanceKey,
    persistedActive = persistedExperienceState.serviceInstanceKey,
  )
  val selectedServiceOccurrenceReady = writableServiceInstanceKey != null
  val selectedServiceOccurrenceActivating =
    selectedServiceSupportsRehearsal && !selectedServiceOccurrenceReady
  val selectedServiceWriteKey = remember { AtomicReference<ServiceInstanceKey?>(null) }
  val writableIndividualPrayerInstanceKey = writableServiceInstanceKey(
    selected = individualPrayerInstanceKey,
    persistedActive = persistedExperienceState.serviceInstanceKey,
  )
  val individualPrayerWriteKey = remember { AtomicReference<ServiceInstanceKey?>(null) }
  SideEffect {
    selectedServiceWriteKey.set(writableServiceInstanceKey)
    individualPrayerWriteKey.set(writableIndividualPrayerInstanceKey)
  }
  LaunchedEffect(
    serviceSelection,
    requestedScheduledServiceId,
    persistedExperienceState.selectedServicePlanId,
  ) {
    val defaultServiceId = selectedScheduledService?.id
    val requestedIsAvailable = (serviceSelection as? ServiceSelectionResult.Available)
      ?.agenda
      ?.upcomingServices
      .orEmpty()
      .any { service -> service.id == requestedScheduledServiceId }
    if (defaultServiceId != null && !requestedIsAvailable) {
      selectedServiceWriteKey.set(null)
      selectedScheduledServiceIdOverride = defaultServiceId
      if (persistedExperienceState.selectedServicePlanId != defaultServiceId) {
        launchUserDataMutation {
          experienceStateRepository?.setSelectedServicePlan(defaultServiceId)
        }
      }
    }
  }
  val experienceServiceDate = selectedScheduledService?.serviceDate
    ?: nextShabbat(experienceLocalDate)
  val occurrenceExperienceState = remember(
    persistedExperienceState,
    selectedServiceInstanceKey,
  ) {
    selectedServiceInstanceKey?.let(persistedExperienceState::forServiceInstance)
      ?: persistedExperienceState.withoutServiceInstanceProgress()
  }
  val individualPrayerExperienceState = remember(
    persistedExperienceState,
    individualPrayerInstanceKey,
  ) {
    individualPrayerInstanceKey?.let(persistedExperienceState::forServiceInstance)
      ?: persistedExperienceState.withoutServiceInstanceProgress()
  }
  val activeWorkspaceContext = remember(
    workspaceSnapshot,
    occurrenceExperienceState.workspaceKind,
    activeWorkspaceIdOverride,
  ) {
    val requestedId = activeWorkspaceIdOverride?.let(::WorkspaceId)
    when (occurrenceExperienceState.workspaceKind) {
      WorkspaceKind.SELF -> (
        workspaceSnapshot.selfWorkspaces.firstOrNull { it.id == requestedId }
          ?: workspaceSnapshot.selfWorkspaces.firstOrNull()
        )?.let { workspace ->
        ActiveWorkspaceContext.Self(workspace.id)
      }
      WorkspaceKind.HOUSEHOLD -> (
        workspaceSnapshot.households.firstOrNull { it.id == requestedId }
          ?: workspaceSnapshot.households.firstOrNull()
        )?.let { workspace ->
        ActiveWorkspaceContext.Household(workspace.id)
      }
      WorkspaceKind.QAHAL -> (
        workspaceSnapshot.qahalWorkspaces.firstOrNull { it.id == requestedId }
          ?: workspaceSnapshot.qahalWorkspaces.firstOrNull()
        )?.let { workspace ->
        ActiveWorkspaceContext.Qahal(workspace.id)
      }
    }
  }
  val activeWorkspaceLabel = remember(workspaceSnapshot, activeWorkspaceContext) {
    when (val active = activeWorkspaceContext) {
      is ActiveWorkspaceContext.Self -> workspaceSnapshot.selfWorkspaces
        .firstOrNull { workspace -> workspace.id == active.workspaceId }
        ?.label
      is ActiveWorkspaceContext.Household -> workspaceSnapshot.households
        .firstOrNull { workspace -> workspace.id == active.workspaceId }
        ?.label
      is ActiveWorkspaceContext.Qahal -> workspaceSnapshot.qahalWorkspaces
        .firstOrNull { workspace -> workspace.id == active.workspaceId }
        ?.label
      null -> null
    }
  }
  val experienceState = remember(occurrenceExperienceState, activeWorkspaceLabel) {
    activeWorkspaceLabel
      ?.takeIf(String::isNotBlank)
      ?.let { label -> occurrenceExperienceState.copy(workspaceName = label) }
      ?: occurrenceExperienceState
  }
  val workspaceSummary = remember(
    workspaceReviewStateRepository,
    workspaceSnapshot,
    activeWorkspaceContext,
    experienceLocalDate,
    experienceState.workspaceKind,
  ) {
    val active = activeWorkspaceContext
    if (workspaceReviewStateRepository == null || active == null) {
      null
    } else {
      when (val due = LocalWorkspaceApi.dueSoon(workspaceSnapshot, experienceLocalDate)) {
        is WorkspaceOperationResult.Rejected -> null
        is WorkspaceOperationResult.Success -> NowWorkspaceSummary(
          contextLabel = experienceState.workspaceKind.label,
          dueItems = due.value.items
            .filter { item -> item.area == active.area && item.workspaceId == active.workspaceId }
            .map { item ->
              NowWorkspaceDueItem(
                label = item.label,
                dueLabel = item.dueOn.toString(),
                timingLabel = when (item.timing) {
                  DueTiming.OVERDUE -> "Overdue"
                  DueTiming.TODAY -> "Today"
                  DueTiming.UPCOMING -> "Upcoming"
                },
              )
            },
        )
      }
    }
  }
  LaunchedEffect(
    experienceStateRepository,
    manualPlaceContextReady,
    requestedActiveServiceInstanceKey,
    persistedExperienceState.serviceInstanceKey,
  ) {
    if (
      manualPlaceContextReady &&
      experienceStateRepository != null &&
      persistedExperienceState.serviceInstanceKey != requestedActiveServiceInstanceKey
    ) {
      localUserDataMutationGate.mutate {
        if (requestedActiveServiceInstanceKey == null) {
          experienceStateRepository.deactivateServiceInstance()
        } else {
          experienceStateRepository.activateServiceInstance(requestedActiveServiceInstanceKey)
        }
      }
    }
  }
  val experienceDateLabel = selectedScheduledService?.serviceDisplayLabel()
    ?: serviceDateLabel(experienceLocalDate)
  val experienceLocationLabel = storedManualPlaceContext?.label
    ?.takeIf(String::isNotBlank)
    ?: if (storedManualPlaceContext?.source == PlaceContextSource.DEVICE) {
      "Current device location"
    } else {
      "Location not set"
    }
  val serviceCalendarContext = remember(storedManualPlaceContext, experienceServiceDate) {
    calculateServiceCalendarContext(
      place = storedManualPlaceContext,
      serviceDate = experienceServiceDate,
    )
  }
  val experienceAssemblyContext = contentCatalog
    ?.takeIf { selectedServiceSupportsRehearsal }
    ?.let { catalog ->
      serviceAssemblyContext(
        catalog = catalog,
        state = experienceState,
        date = ServiceDateContext(
          civilDate = experienceServiceDate,
          displayLabel = experienceDateLabel,
          dayKind = selectedScheduledService?.toCoreServiceDayKind() ?: ServiceDayKind.SHABBAT,
          hebrewDate = serviceCalendarContext.hebrewDate,
        ),
        calendarRegion = experienceState.calendarRegion,
        place = storedManualPlaceContext?.let { place ->
          ServicePlaceAvailability.Available(
            label = experienceLocationLabel,
            timeZoneId = place.timeZoneId,
            coordinates = ServiceCoordinates(
              latitudeDegrees = place.latitudeDegrees,
              longitudeDegrees = place.longitudeDegrees,
              elevationMeters = place.elevationMeters,
              horizontalAccuracyMeters = place.horizontalAccuracyMeters,
            ),
          )
        } ?: ServicePlaceAvailability.Unavailable(
          reason = "Use device location or save a manual fallback in Now.",
        ),
        accessibility = experienceState.accessibilityProfile,
        zmanim = serviceCalendarContext.zmanim,
      )
    }
  val experienceAssembly = if (contentCatalog != null && experienceAssemblyContext != null) {
    assembleService(
      catalog = contentCatalog,
      state = experienceState,
      context = experienceAssemblyContext,
    )
  } else {
    null
  }
  val journeySummary = contentCatalog?.let { catalog ->
    val practice = practiceProgress(catalog, experienceState)
    val service = experienceAssembly?.let { assembly ->
      serviceProgress(assembly, experienceState)
    } ?: (0 to 0)
    val missingRole = experienceAssemblyContext?.roles?.missingRoles?.firstOrNull()
    val firstOpenReading = experienceAssemblyContext?.readings?.unassigned?.firstOrNull()
    val firstUnreadyReading = experienceAssemblyContext?.readings?.orderedAssignments
      ?.firstOrNull { assignment ->
        assignment.isAssigned &&
          assignment.preparationStatus != io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus.READY
      }
    val disputedPracticeDossier = catalog.disputedPracticeDossier
    val hasReviewedDossierFact = disputedPracticeDossier?.let { dossier ->
      dossier.factualQuestions.indices.any { index ->
        "${dossier.id}.fact.${index + 1}" in experienceState.reviewedDossierFactIds
      }
    } ?: true
    val hasDisputedPracticeAdoption =
      !experienceState.disputedPracticeAdoption.optionId.isNullOrBlank()
    val nextStep = when {
      storedManualPlaceContext == null -> Triple(
        "Use my location",
        "Allow one-time device location so ASEH can assemble the service for the correct place, date, and time zone.",
        NowJourneyAction.LOCATION,
      )
      experienceState.workspaceName.isBlank() -> Triple(
        "Name the rehearsal workspace",
        "Create the local household or qahal workspace that owns roles, readings, and community decisions.",
        NowJourneyAction.BUILD,
      )
      experienceState.calendarRegion == CalendarRegion.UNSPECIFIED -> Triple(
        "Choose Israel or diaspora",
        "The calendar region is required before service composition can be treated as current.",
        NowJourneyAction.BUILD,
      )
      !selectedServiceSupportsRehearsal -> Triple(
        "Choose the Shabbat morning rehearsal",
        "The selected service has schedule metadata, but the installed conductor supports only the dated Shabbat morning rehearsal.",
        NowJourneyAction.PRAYER,
      )
      missingRole != null -> Triple(
        "Assign ${missingRole.label.lowercase()}",
        "Leader, reader, and gabbai assignments drive the role-specific rehearsal views.",
        NowJourneyAction.BUILD,
      )
      firstOpenReading != null -> Triple(
        "Assign ${firstOpenReading.label.lowercase()}",
        "Every Torah-reading slot needs a named primary reader before the normal rehearsal can start.",
        NowJourneyAction.BUILD,
      )
      firstUnreadyReading != null -> Triple(
        "Confirm ${firstUnreadyReading.label.lowercase()} preparation",
        "Record the portion details, backup reader, and preparation status in the reading plan.",
        NowJourneyAction.BUILD,
      )
      !experienceState.accessibilityProfile.participantNeedsReviewed -> Triple(
        "Review participant access needs",
        "Choose movement, visual, text, contrast, screen, and low-light supports with the group.",
        NowJourneyAction.BUILD,
      )
      !experienceState.communityCharter.adopted -> Triple(
        "Review the community charter",
        "Complete and adopt the local purpose, authority limits, roles, access commitment, and review date.",
        NowJourneyAction.BUILD,
      )
      experienceState.selectedCommunityOptionId == null -> Triple(
        "Record the local service-order choice",
        "Review the available service-order options and record the option adopted for this workspace.",
        NowJourneyAction.BUILD,
      )
      disputedPracticeDossier != null && !hasReviewedDossierFact -> Triple(
        "Review the disputed-practice dossier",
        "Review at least one factual question before recording a separate local adoption.",
        NowJourneyAction.BUILD,
      )
      disputedPracticeDossier != null && !hasDisputedPracticeAdoption -> Triple(
        "Record the local disputed-practice adoption",
        "After reviewing the dossier facts and arguments, record the option adopted for this workspace.",
        NowJourneyAction.BUILD,
      )
      practice.first < practice.second -> Triple(
        "Continue preparation",
        "Complete the practical preparation cards before running the communal rehearsal.",
        NowJourneyAction.PRACTICE,
      )
      experienceAssemblyContext?.preflight?.isComplete == false -> Triple(
        "Complete service preflight",
        "Confirm roles, packet, access needs, and offline readiness in the service dashboard.",
        NowJourneyAction.PRAYER,
      )
      experienceAssembly?.readiness?.canBeginRehearsal == false -> {
        val issue = experienceAssembly.readiness.blockers.first()
        Triple(issue.title, issue.detail, NowJourneyAction.BUILD)
      }
      service.first > 0 && service.first < service.second -> Triple(
        "Resume service rehearsal",
        "Continue from the first unfinished assembled segment in conductor mode.",
        NowJourneyAction.PRAYER,
      )
      service.first == service.second && service.second > 0 -> Triple(
        "Review the completed rehearsal",
        "Inspect the assembled order, source traces, packet, and saved completion state.",
        NowJourneyAction.PRAYER,
      )
      else -> Triple(
        "Start service rehearsal",
        "The required setup is complete. Open the role-specific dashboard and begin conductor mode.",
        NowJourneyAction.PRAYER,
      )
    }
    NowJourneySummary(
      dateLabel = experienceDateLabel,
      locationLabel = experienceLocationLabel,
      workspaceLabel = experienceState.workspaceName.ifBlank { "Not configured" },
      practiceCompleted = practice.first,
      practiceTotal = practice.second,
      serviceCompleted = service.first,
      serviceTotal = service.second,
      nextActionTitle = nextStep.first,
      nextActionDetail = nextStep.second,
      nextAction = nextStep.third,
    )
  }
  val sharePacket: (String) -> Unit = { packet ->
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "ASEH service rehearsal packet")
      putExtra(Intent.EXTRA_TEXT, packet)
    }
    context.startActivity(
      Intent.createChooser(sendIntent, "Share packet"),
    )
  }
  val currentDensity = LocalDensity.current
  val systemDarkTheme = isSystemInDarkTheme()
  val accessTextScale = if (experienceState.accessibilityProfile.useLargeText) 1.3f else 1f
  val scaledDensity = Density(
    density = currentDensity.density,
    fontScale = currentDensity.fontScale * storedPreferences.textScale * accessTextScale,
  )

  CompositionLocalProvider(LocalDensity provides scaledDensity) {
    AsehTheme(
      darkTheme = systemDarkTheme || experienceState.accessibilityProfile.lowLightMode,
      highContrast = experienceState.accessibilityProfile.useHighContrast,
      lowLight = experienceState.accessibilityProfile.lowLightMode,
    ) {
      if (globalSearchOpen) {
        GlobalSearchDialog(
          catalog = contentCatalog,
          workspaceSnapshot = workspaceSnapshot,
          onOpenEntry = { entry ->
            globalSearchOpen = false
            when (entry.destination) {
              GlobalSearchDestination.PRACTICE -> {
                requestedPracticeCardId = entry.id
                navigateTo(AsehDestination.PRACTICE)
              }
              GlobalSearchDestination.PRAYER -> navigateTo(AsehDestination.PRAYER)
              GlobalSearchDestination.STUDY_SOURCE -> {
                requestedStudySourceId = entry.id
                navigateTo(AsehDestination.STUDY)
              }
              GlobalSearchDestination.BUILD -> {
                val workspaceKind = checkNotNull(entry.workspaceKind) {
                  "A workspace search result requires a workspace kind"
                }
                val address = checkNotNull(entry.workspaceRecordAddress) {
                  "A workspace search result requires an exact record address"
                }
                requestedWorkspaceId = address.workspaceId.value
                requestedWorkspaceRecordId = address.recordId?.value
                requestedWorkspaceParentRecordId = address.parentRecordId?.value
                requestedWorkspaceRecordKind = address.kind.name
                activeWorkspaceIdOverride = address.workspaceId.value
                serviceSetupSelected = false
                workspaceRejectedIssues = emptyList()
                launchUserDataMutation {
                  experienceStateRepository?.setWorkspace(
                    experienceState.workspaceName,
                    workspaceKind,
                  )
                }
                navigateTo(AsehDestination.BUILD)
              }
            }
          },
          onDismiss = { globalSearchOpen = false },
        )
      }
      if (contextChooserOpen) {
        ContextChooserDialog(
          selected = experienceState.workspaceKind,
          onSelected = { kind ->
            activeWorkspaceIdOverride = null
            serviceSetupSelected = false
            workspaceRejectedIssues = emptyList()
            launchUserDataMutation {
              experienceStateRepository?.setWorkspace(
                experienceState.workspaceName,
                kind,
              )
            }
          },
          onDismiss = { contextChooserOpen = false },
        )
      }
      AsehAdaptiveNavigationShell(
        selectedDestination = selectedDestination,
        onDestinationSelected = { destination ->
          if (destination == AsehDestination.BUILD) serviceSetupSelected = false
          navigateTo(destination)
        },
        modifier = Modifier.testTag("aseh-root"),
      ) { destination ->
        Column(modifier = Modifier.fillMaxSize()) {
          GlobalNavigationTools(
            workspaceKind = experienceState.workspaceKind,
            onOpenContextChooser = { contextChooserOpen = true },
            onOpenSearch = { globalSearchOpen = true },
          )
          if (manualPlaceContextReady && destination != AsehDestination.NOW) {
            PlaceContextBar(
              placeContext = storedManualPlaceContext,
              onEdit = {
                if (!isDeletingLocalUserData && !localUserDataMutationGate.isDeletionRequested) {
                  selectedDestinationOverride = AsehDestination.NOW.persistedId
                  if (onSelectedDestinationChanged != null) {
                    onSelectedDestinationChanged(AsehDestination.NOW.persistedId)
                  } else {
                    launchUserDataMutation {
                      interfacePreferencesRepository.setSelectedDestinationId(
                        AsehDestination.NOW.persistedId,
                      )
                    }
                  }
                  openPlaceEditorRequest += 1
                }
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
                onRequestDeviceLocation = if (deviceLocationClient == null) {
                  {}
                } else {
                  requestDeviceLocation
                },
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
                  val saved = localUserDataMutationGate.mutate {
                    manualPlaceContextRepository.save(context.toStoredManualPlaceContext())
                  }
                  if (saved) deviceLocationState = DeviceLocationUiState.Idle
                },
                onClearPlaceContext = {
                  val cleared = localUserDataMutationGate.mutate {
                    manualPlaceContextRepository.clear()
                  }
                  if (cleared) deviceLocationState = DeviceLocationUiState.Idle
                },
                journeySummary = journeySummary.takeIf { individualPrayerServices.isEmpty() },
                workspaceSummary = workspaceSummary.takeIf { individualPrayerServices.isEmpty() },
                serviceSelection = serviceSelection.takeIf { individualPrayerServices.isEmpty() },
                showFunctionalReviewNotice = contentCatalog != null,
                onOpenService = { serviceId ->
                  selectedServiceWriteKey.set(null)
                  selectedScheduledServiceIdOverride = serviceId
                  launchUserDataMutation {
                    experienceStateRepository?.setSelectedServicePlan(serviceId)
                  }
                  navigateTo(AsehDestination.PRAYER)
                },
                onOpenPractice = { navigateTo(AsehDestination.PRACTICE) },
                onOpenPrayer = { navigateTo(AsehDestination.PRAYER) },
                onOpenBuild = {
                  serviceSetupSelected = true
                  navigateTo(AsehDestination.BUILD)
                },
                onOpenWorkspace = {
                  serviceSetupSelected = false
                  navigateTo(AsehDestination.BUILD)
                },
              )
              AsehDestination.PRACTICE -> PracticeScreen(
                catalog = contentCatalog,
                state = experienceState,
                occurrenceProgressEnabled = selectedServiceOccurrenceReady,
                occurrenceProgressActivating = selectedServiceOccurrenceActivating,
                requestedCardId = requestedPracticeCardId,
                onRequestedCardConsumed = { requestedPracticeCardId = null },
                onStepCompleted = { id, completed ->
                  dispatchServiceOccurrenceWrite(selectedServiceWriteKey.get()) { serviceInstance ->
                    launchUserDataMutation {
                      if (selectedServiceWriteKey.get() == serviceInstance) {
                        experienceStateRepository?.setPracticeStepCompleted(
                          serviceInstance,
                          id,
                          completed,
                        )
                      }
                    }
                  }
                },
                onCardSaved = { id, saved ->
                  launchUserDataMutation {
                    experienceStateRepository?.setPracticeCardSaved(id, saved)
                  }
                },
                onOpenSource = { sourceId ->
                  requestedStudySourceId = sourceId
                  navigateTo(AsehDestination.STUDY)
                },
                onOpenPrayer = { navigateTo(AsehDestination.PRAYER) },
              )
              AsehDestination.PRAYER -> if (selectedIndividualPrayerService != null) {
                IndividualPrayerScreen(
                  services = individualPrayerServices,
                  selectedServiceId = selectedIndividualPrayerService.id,
                  suggestedServiceId = suggestedIndividualPrayerService?.id,
                  state = individualPrayerExperienceState,
                  dateLabel = serviceDateLabel(experienceLocalDate),
                  locationLabel = if (storedManualPlaceContext == null) {
                    ""
                  } else {
                    experienceLocationLabel
                  },
                  onServiceSelected = { serviceId ->
                    if (individualPrayerServices.any { service -> service.id == serviceId }) {
                      individualPrayerWriteKey.set(null)
                      selectedIndividualPrayerServiceIdOverride = serviceId
                    }
                  },
                  onUseDeviceLocation = if (deviceLocationClient != null) {
                    {
                      navigateTo(AsehDestination.NOW)
                      openPlaceEditorRequest += 1
                      requestDeviceLocation()
                    }
                  } else {
                    null
                  },
                  onSegmentCompleted = { id, completed ->
                    dispatchServiceOccurrenceWrite(individualPrayerWriteKey.get()) { serviceInstance ->
                      launchUserDataMutation {
                        if (individualPrayerWriteKey.get() == serviceInstance) {
                          experienceStateRepository?.setServiceSegmentCompleted(
                            serviceInstance,
                            id,
                            completed,
                          )
                        }
                      }
                    }
                  },
                  onSharePacket = sharePacket,
                  onPrintPacket = { packet ->
                    printServicePacket(
                      context = context,
                      title = "ASEH ${selectedIndividualPrayerService.title}",
                      packet = packet,
                      largeText = individualPrayerExperienceState.accessibilityProfile.useLargeText,
                    )
                  },
                )
              } else {
                PrayerScreen(
                  catalog = contentCatalog,
                  state = experienceState,
                  dateLabel = experienceDateLabel,
                  locationLabel = experienceLocationLabel,
                  assemblyContext = experienceAssemblyContext,
                  serviceSelection = serviceSelection,
                  selectedScheduledServiceId = selectedScheduledServiceId,
                  selectedScheduledService = selectedScheduledService,
                  selectedServiceContentSupported = selectedServiceSupportsRehearsal,
                  serviceOccurrenceReady = selectedServiceOccurrenceReady,
                  onScheduledServiceSelected = { serviceId ->
                    selectedServiceWriteKey.set(null)
                    selectedScheduledServiceIdOverride = serviceId
                    launchUserDataMutation {
                      experienceStateRepository?.setSelectedServicePlan(serviceId)
                    }
                  },
                  onRoleSelected = { role ->
                    launchUserDataMutation {
                      experienceStateRepository?.setSelectedRole(role)
                    }
                  },
                  onPreflightCompleted = { id, completed ->
                    dispatchServiceOccurrenceWrite(selectedServiceWriteKey.get()) { serviceInstance ->
                      launchUserDataMutation {
                        if (selectedServiceWriteKey.get() == serviceInstance) {
                          experienceStateRepository?.setPreflightStepCompleted(
                            serviceInstance,
                            id,
                            completed,
                          )
                        }
                      }
                    }
                  },
                  onSegmentCompleted = { id, completed ->
                    dispatchServiceOccurrenceWrite(selectedServiceWriteKey.get()) { serviceInstance ->
                      launchUserDataMutation {
                        if (selectedServiceWriteKey.get() == serviceInstance) {
                          experienceStateRepository?.setServiceSegmentCompleted(
                            serviceInstance,
                            id,
                            completed,
                          )
                        }
                      }
                    }
                  },
                  onOpenSource = { sourceId ->
                    requestedStudySourceId = sourceId
                    navigateTo(AsehDestination.STUDY)
                  },
                  onOpenPractice = { navigateTo(AsehDestination.PRACTICE) },
                  onOpenBuild = {
                    serviceSetupSelected = true
                    navigateTo(AsehDestination.BUILD)
                  },
                  onOpenNow = { navigateTo(AsehDestination.NOW) },
                  onSharePacket = sharePacket,
                  onPrintPacket = { packet ->
                    printServicePacket(
                      context = context,
                      title = "ASEH Shabbat rehearsal",
                      packet = packet,
                      largeText = experienceState.accessibilityProfile.useLargeText,
                    )
                  },
                )
              }
              AsehDestination.STUDY -> StudyScreen(
                catalog = contentCatalog,
                state = experienceState,
                requestedSourceId = requestedStudySourceId,
                onRequestedSourceConsumed = { requestedStudySourceId = null },
                onBookmarkChanged = { id, bookmarked ->
                  launchUserDataMutation {
                    experienceStateRepository?.setSourceBookmarked(id, bookmarked)
                  }
                },
              )
              AsehDestination.BUILD -> Column(modifier = Modifier.fillMaxSize()) {
                val workspaceRepository = workspaceReviewStateRepository
                val workspaceContext = activeWorkspaceContext
                val workspaceDashboardAvailable =
                  workspaceRepository != null && workspaceContext != null
                if (workspaceDashboardAvailable) {
                  WorkspaceBuildModeBar(
                    serviceSetupSelected = serviceSetupSelected,
                    fixtureNotice = requireNotNull(workspaceRepository).fixtureNotice,
                    onOpenWorkspace = { serviceSetupSelected = false },
                    onOpenServiceSetup = {
                      workspaceEditorOpen = false
                      serviceSetupSelected = true
                    },
                  )
                }
                if (workspaceDashboardAvailable && !serviceSetupSelected) {
                  WorkspaceDashboard(
                    snapshot = workspaceSnapshot,
                    activeContext = requireNotNull(workspaceContext),
                    asOf = experienceLocalDate,
                    onCommand = { command ->
                      workspacePersistenceFailed = false
                      workspaceRejectedIssues = emptyList()
                      locationScope.launch {
                        var updateResult: WorkspaceUpdateResult? = null
                        try {
                          val accepted = withContext(NonCancellable) {
                            localUserDataMutationGate.mutate {
                              updateResult = requireNotNull(workspaceRepository).apply(command)
                            }
                          }
                          currentCoroutineContext().ensureActive()
                          if (accepted) {
                            val rejectedIssues =
                              (updateResult as? WorkspaceUpdateResult.Rejected)?.issues.orEmpty()
                            workspacePersistenceFailed = rejectedIssues.any { issue ->
                              issue.path == WORKSPACE_LOCAL_PERSISTENCE_PATH
                            }
                            workspaceRejectedIssues = rejectedIssues.filterNot { issue ->
                              issue.path == WORKSPACE_LOCAL_PERSISTENCE_PATH
                            }
                          }
                        } catch (error: CancellationException) {
                          throw error
                        } catch (_: Exception) {
                          workspacePersistenceFailed = true
                          workspaceRejectedIssues = emptyList()
                        }
                      }
                    },
                    modifier = Modifier.weight(1f),
                    feedback = WorkspaceDashboardFeedback(
                      rejectedCommandIssues = workspaceRejectedIssues,
                      persistenceFailed = workspacePersistenceFailed,
                    ),
                    requestedRecord = requestedWorkspaceRecordKind?.let { kindName ->
                      val workspaceId = requestedWorkspaceId ?: return@let null
                      runCatching {
                        WorkspaceRecordAddress(
                          workspaceId = WorkspaceId(workspaceId),
                          kind = WorkspaceRecordKind.valueOf(kindName),
                          recordId = requestedWorkspaceRecordId?.let(::WorkspaceRecordId),
                          parentRecordId = requestedWorkspaceParentRecordId?.let(::WorkspaceRecordId),
                        )
                      }.getOrNull()
                    },
                    onRequestedRecordConsumed = {
                      requestedWorkspaceId = null
                      requestedWorkspaceRecordId = null
                      requestedWorkspaceParentRecordId = null
                      requestedWorkspaceRecordKind = null
                    },
                    dismissEditorRequest = dismissWorkspaceEditorRequest,
                    onEditorOpenChanged = { workspaceEditorOpen = it },
                  )
                } else {
                  BuildScreen(
                catalog = contentCatalog,
                serviceOccurrenceSupported = selectedServiceOccurrenceReady,
                serviceOccurrenceActivating = selectedServiceOccurrenceActivating,
                state = experienceState,
                dateLabel = experienceDateLabel,
                locationLabel = experienceLocationLabel,
                assemblyContext = experienceAssemblyContext,
                onWorkspaceSaved = { name, kind ->
                  launchUserDataMutation {
                    val renameCommand = when (kind) {
                      WorkspaceKind.SELF -> workspaceSnapshot.selfWorkspaces.firstOrNull()?.let {
                        io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand.RenameSelf(it.id, name)
                      }
                      WorkspaceKind.HOUSEHOLD -> workspaceSnapshot.households.firstOrNull()?.let {
                        io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand.RenameHousehold(it.id, name)
                      }
                      WorkspaceKind.QAHAL -> workspaceSnapshot.qahalWorkspaces.firstOrNull()?.let {
                        io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand.RenameQahal(it.id, name)
                      }
                    }
                    val renameResult = if (renameCommand != null && workspaceReviewStateRepository != null) {
                      workspaceReviewStateRepository.apply(renameCommand)
                    } else {
                      null
                    }
                    val rejectedIssues = (renameResult as? WorkspaceUpdateResult.Rejected)?.issues.orEmpty()
                    workspacePersistenceFailed = rejectedIssues.any { issue ->
                      issue.path == WORKSPACE_LOCAL_PERSISTENCE_PATH
                    }
                    workspaceRejectedIssues = rejectedIssues.filterNot { issue ->
                      issue.path == WORKSPACE_LOCAL_PERSISTENCE_PATH
                    }
                    if (renameResult !is WorkspaceUpdateResult.Rejected) {
                      experienceStateRepository?.setWorkspace(name, kind)
                    }
                  }
                },
                onRoleAssignmentChanged = { role, name ->
                  dispatchServiceOccurrenceWrite(selectedServiceWriteKey.get()) { serviceInstance ->
                    launchUserDataMutation {
                      if (selectedServiceWriteKey.get() == serviceInstance) {
                        experienceStateRepository?.setRoleAssignment(
                          serviceInstance,
                          role,
                          name,
                        )
                      }
                    }
                  }
                },
                onReadingPlanChanged = { slotId, plan ->
                  dispatchServiceOccurrenceWrite(selectedServiceWriteKey.get()) { serviceInstance ->
                    launchUserDataMutation {
                      if (selectedServiceWriteKey.get() == serviceInstance) {
                        experienceStateRepository?.setReadingPlan(
                          serviceInstance,
                          slotId,
                          plan,
                        )
                      }
                    }
                  }
                },
                onDeviceUseModeChanged = { mode ->
                  launchUserDataMutation {
                    experienceStateRepository?.setDeviceUseMode(mode)
                  }
                },
                onCalendarRegionChanged = { region: CalendarRegion ->
                  launchUserDataMutation {
                    experienceStateRepository?.setCalendarRegion(region)
                  }
                },
                onAccessibilityProfileChanged = { transform ->
                  launchUserDataMutation {
                    experienceStateRepository?.updateAccessibilityProfile(transform)
                  }
                },
                onCommunityOptionSelected = { optionId ->
                  launchUserDataMutation {
                    experienceStateRepository?.setCommunityOption(optionId)
                  }
                },
                onCommunityCharterSaved = { charter ->
                  launchUserDataMutation {
                    experienceStateRepository?.setCommunityCharter(charter)
                  }
                },
                onDossierAdoptionSaved = { adoption ->
                  launchUserDataMutation {
                    experienceStateRepository?.setDisputedPracticeAdoption(adoption)
                  }
                },
                onDossierFactReviewed = { factId, reviewed ->
                  launchUserDataMutation {
                    experienceStateRepository?.setDossierFactReviewed(factId, reviewed)
                  }
                },
                onOpenSource = { sourceId ->
                  requestedStudySourceId = sourceId
                  navigateTo(AsehDestination.STUDY)
                },
                onOpenNow = { navigateTo(AsehDestination.NOW) },
                onOpenPractice = { navigateTo(AsehDestination.PRACTICE) },
                onOpenPrayer = { navigateTo(AsehDestination.PRAYER) },
                onSharePacket = sharePacket,
                onPrintPacket = { packet ->
                  printServicePacket(
                    context = context,
                    title = "ASEH Shabbat rehearsal",
                    packet = packet,
                    largeText = experienceState.accessibilityProfile.useLargeText,
                  )
                },
                isDeletingLocalData = isDeletingLocalUserData,
                localDataDeletionFailed = localUserDataDeletionFailed,
                onDeleteAllLocalData = {
                  if (!isDeletingLocalUserData) {
                    isDeletingLocalUserData = true
                    localUserDataDeletionFailed = false
                    locationScope.launch {
                      try {
                        val (result, workspaceDeletionFailed) =
                          localUserDataMutationGate.deleteExclusively {
                            val coreResult = localUserDataDeletion.deleteAll()
                            val workspaceFailed = runCatching {
                              workspaceReviewStateRepository?.clear()
                            }.isFailure
                            coreResult to workspaceFailed
                        }
                        if (result.isComplete && !workspaceDeletionFailed) {
                          selectedDestinationOverride = null
                          selectedServiceWriteKey.set(null)
                          individualPrayerWriteKey.set(null)
                          selectedScheduledServiceIdOverride = null
                          globalSearchOpen = false
                          contextChooserOpen = false
                        } else {
                          localUserDataDeletionFailed = true
                        }
                      } finally {
                        isDeletingLocalUserData = false
                      }
                    }
                  }
                },
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

internal const val SHABBAT_MORNING_REHEARSAL_SERVICE_ID = "dev.service.shabbat.morning"

internal fun individualPrayerServiceInstanceKey(
  service: IndividualPrayerService,
  serviceDate: LocalDate,
): ServiceInstanceKey = ServiceInstanceKey(
  occurrenceId = "${service.id}@$serviceDate#individual",
  serviceDate = serviceDate,
)

private val serviceOccurrenceDateFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

internal fun selectScheduledService(
  selection: ServiceSelectionResult?,
  requestedServiceId: String?,
): DatedServiceInstance? {
  val upcoming = (selection as? ServiceSelectionResult.Available)
    ?.agenda
    ?.upcomingServices
    .orEmpty()
  return upcoming.firstOrNull { service -> service.id == requestedServiceId }
    ?: upcoming.firstOrNull(DatedServiceInstance::supportsShabbatMorningRehearsal)
    ?: upcoming.firstOrNull()
}

internal fun DatedServiceInstance.supportsShabbatMorningRehearsal(): Boolean =
    definition.id == SHABBAT_MORNING_REHEARSAL_SERVICE_ID &&
    definition.serviceKind == ServiceKind.MORNING &&
    calendarDay.dayKind == ScheduledServiceDayKind.SHABBAT &&
    ServiceOperationalCapability.SHABBAT_MORNING_REHEARSAL in
      definition.operationalCapabilities &&
    temporalState != ServiceTemporalState.PASSED

internal fun DatedServiceInstance.rehearsalServiceInstanceKeyOrNull(): ServiceInstanceKey? =
  takeIf(DatedServiceInstance::supportsShabbatMorningRehearsal)?.let { service ->
    ServiceInstanceKey(
      occurrenceId = service.id,
      serviceDate = service.serviceDate,
    )
  }

internal fun dispatchServiceOccurrenceWrite(
  serviceInstance: ServiceInstanceKey?,
  write: (ServiceInstanceKey) -> Unit,
): Boolean {
  val supportedInstance = serviceInstance ?: return false
  write(supportedInstance)
  return true
}

internal fun writableServiceInstanceKey(
  selected: ServiceInstanceKey?,
  persistedActive: ServiceInstanceKey?,
): ServiceInstanceKey? = selected?.takeIf { it == persistedActive }

internal fun DatedServiceInstance.serviceDisplayLabel(): String =
  "${definition.title.fallbackEnglish} · " + startsAt.format(serviceOccurrenceDateFormatter)

internal fun DatedServiceInstance.toCoreServiceDayKind(): ServiceDayKind =
  when (calendarDay.dayKind) {
    ScheduledServiceDayKind.WEEKDAY -> ServiceDayKind.WEEKDAY
    ScheduledServiceDayKind.SHABBAT -> ServiceDayKind.SHABBAT
    ScheduledServiceDayKind.FESTIVAL -> ServiceDayKind.FESTIVAL
  }

internal data class ServiceCalendarContext(
  val hebrewDate: ServiceHebrewDateContext,
  val zmanim: ServiceZmanimContext,
)

internal fun calculateServiceCalendarContext(
  place: ManualPlaceContext?,
  serviceDate: LocalDate,
): ServiceCalendarContext {
  if (place == null) {
    val reason = "A saved place is required for the local Hebrew date and solar calculation."
    return ServiceCalendarContext(
      hebrewDate = ServiceHebrewDateContext.Unavailable(reason),
      zmanim = unavailableServiceZmanim(serviceDate, null, reason),
    )
  }

  val zoneId = runCatching { ZoneId.of(place.timeZoneId) }.getOrElse {
    val reason = "The saved time zone is not available on this device."
    return ServiceCalendarContext(
      hebrewDate = ServiceHebrewDateContext.Unavailable(reason),
      zmanim = unavailableServiceZmanim(serviceDate, place.timeZoneId, reason),
    )
  }
  val referenceInstant = serviceDate.atTime(12, 0).atZone(zoneId).toInstant()
  val snapshot = runCatching {
    KosherJavaZmanimEngine().calculate(
      ZmanimRequest(
        date = serviceDate,
        referenceInstant = referenceInstant,
        latitude = place.latitudeDegrees,
        longitude = place.longitudeDegrees,
        elevationMeters = place.elevationMeters,
        zoneId = zoneId,
      ),
    )
  }.getOrElse {
    val reason = "The local calendar calculation could not be completed for this place."
    return ServiceCalendarContext(
      hebrewDate = ServiceHebrewDateContext.Unavailable(reason),
      zmanim = unavailableServiceZmanim(serviceDate, place.timeZoneId, reason),
    )
  }

  val hebrewDate = when (val result = snapshot.hebrewDate) {
    is HebrewDateCalculation.Available -> ServiceHebrewDateContext.Available(
      transliteratedLabel = result.date.transliterated,
      hebrewLabel = result.date.hebrew,
      boundaryLabel = result.boundary.name.lowercase().replace('_', ' '),
    )
    is HebrewDateCalculation.Unavailable -> ServiceHebrewDateContext.Unavailable(
      "${result.reason.name.lowercase().replace('_', ' ')} for this place and date.",
    )
  }
  return ServiceCalendarContext(
    hebrewDate = hebrewDate,
    zmanim = ServiceZmanimContext(
      date = snapshot.date,
      timeZoneId = snapshot.metadata.zoneId.id,
      events = listOf(
        snapshot.sunrise.toServiceSolarEvent(),
        snapshot.solarNoonChatzot.toServiceSolarEvent(),
        snapshot.sunset.toServiceSolarEvent(),
      ),
      methodLabel = buildString {
        append(snapshot.metadata.engineName)
        append(" ")
        append(snapshot.metadata.engineVersion)
        append("; ")
        append(snapshot.metadata.algorithmName)
        append("; elevation handling: ")
        append(snapshot.metadata.elevationHandling.name.lowercase().replace('_', ' '))
      },
    ),
  )
}

private fun unavailableServiceZmanim(
  serviceDate: LocalDate,
  timeZoneId: String?,
  reason: String,
): ServiceZmanimContext = ServiceZmanimContext(
  date = serviceDate,
  timeZoneId = timeZoneId,
  events = ServiceSolarEventKind.entries.map { kind ->
    ServiceSolarEvent.Unavailable(kind, reason)
  },
  methodLabel = "Not calculated: $reason",
)

private fun ZmanCalculation.toServiceSolarEvent(): ServiceSolarEvent {
  val kind = when (type) {
    ZmanimEventType.SUNRISE -> ServiceSolarEventKind.SUNRISE
    ZmanimEventType.SOLAR_NOON_CHATZOT -> ServiceSolarEventKind.SOLAR_NOON_CHATZOT
    ZmanimEventType.SUNSET -> ServiceSolarEventKind.SUNSET
  }
  return when (this) {
    is ZmanCalculation.Available -> ServiceSolarEvent.Available(kind, instant)
    is ZmanCalculation.Unavailable -> ServiceSolarEvent.Unavailable(
      kind = kind,
      reason = reason.name.lowercase().replace('_', ' '),
    )
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
