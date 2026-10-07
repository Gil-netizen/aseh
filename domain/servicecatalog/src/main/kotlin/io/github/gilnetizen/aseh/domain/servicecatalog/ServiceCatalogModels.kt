package io.github.gilnetizen.aseh.domain.servicecatalog

import java.time.Duration
import java.time.LocalTime

/**
 * A localization-ready user-facing string. Android callers should resolve [resourceKey] to a
 * localized resource and use [fallbackEnglish] only when that resource is unavailable.
 */
data class UserFacingText(
    val resourceKey: String,
    val fallbackEnglish: String,
) {
    init {
        require(resourceKey.isNotBlank()) { "A user-facing string requires a resource key." }
        require(fallbackEnglish.isNotBlank()) { "A user-facing string requires an English fallback." }
    }
}

enum class ServiceDayKind {
    WEEKDAY,
    SHABBAT,
    FESTIVAL,
}

enum class ServiceKind {
    MORNING,
    ADDITIONAL,
    AFTERNOON,
    EVENING,
}

enum class ServiceSetting {
    INDIVIDUAL,
    HOUSEHOLD,
    COMMUNAL,
}

enum class CalendarRegion {
    ISRAEL,
    DIASPORA,
}

/**
 * The selector needs a resolved service date, but it must not silently choose a halakhic
 * day-boundary opinion. Development data therefore uses [CIVIL_DATE_DEVELOPMENT_ONLY].
 */
enum class CalendarDateBasis {
    CIVIL_DATE_DEVELOPMENT_ONLY,
    RESOLVED_LOCAL_JEWISH_DATE,
}

enum class CatalogMaturity {
    DEVELOPMENT_SYNTHETIC,
    REVIEWED,
}

data class OpinionProfile(
    val id: String,
    val title: UserFacingText,
    val calendarRegion: CalendarRegion,
    val calendarDateBasis: CalendarDateBasis,
    val dayBoundaryOpinionId: String,
    val zmanimOpinionId: String,
    val serviceTimingOpinionId: String,
    val maturity: CatalogMaturity,
    val notice: UserFacingText,
) {
    init {
        requireStableId(id, "opinion profile")
        requireStableId(dayBoundaryOpinionId, "day-boundary opinion")
        requireStableId(zmanimOpinionId, "zmanim opinion")
        requireStableId(serviceTimingOpinionId, "service-timing opinion")
    }
}

enum class PreparationHorizonKind {
    TODAY,
    BEFORE_SHABBAT,
    BEFORE_FESTIVAL,
}

data class PreparationPolicy(
    val kind: PreparationHorizonKind,
    val leadDays: Int,
    val title: UserFacingText,
) {
    init {
        require(leadDays >= 0) { "Preparation lead days cannot be negative." }
        require(kind != PreparationHorizonKind.TODAY || leadDays == 0) {
            "Today's preparation horizon must open on the service date."
        }
        require(kind == PreparationHorizonKind.TODAY || leadDays > 0) {
            "Advance preparation horizons must open at least one day before the service."
        }
    }
}

sealed interface ServiceContentState {
    /** The metadata and referenced content are ready for this service capability. */
    data object Ready : ServiceContentState

    /** The occurrence can be planned, but there is no reviewed service content to conduct. */
    data class MetadataOnly(
        val reason: UserFacingText,
    ) : ServiceContentState

    /** Conducting requires all listed content bundles to be installed and verified. */
    data class RequiresBundles(
        val bundleIds: Set<String>,
        val reason: UserFacingText,
    ) : ServiceContentState {
        init {
            require(bundleIds.isNotEmpty()) { "Required content bundles cannot be empty." }
            bundleIds.forEach { id -> requireStableId(id, "content bundle") }
        }
    }
}

data class ServiceDefinition(
    val id: String,
    val title: UserFacingText,
    val serviceKind: ServiceKind,
    val supportedDayKinds: Set<ServiceDayKind>,
    val supportedSettings: Set<ServiceSetting>,
    val scheduledLocalTime: LocalTime,
    val nominalDuration: Duration,
    val preparationPolicy: PreparationPolicy,
    val contentState: ServiceContentState,
) {
    init {
        requireStableId(id, "service definition")
        require(supportedDayKinds.isNotEmpty()) { "A service must support at least one day kind." }
        require(supportedSettings.isNotEmpty()) { "A service must support at least one setting." }
        require(!nominalDuration.isNegative && !nominalDuration.isZero) {
            "A service's nominal duration must be positive."
        }
        require(nominalDuration <= Duration.ofHours(12)) {
            "A service's nominal duration must not exceed twelve hours."
        }
    }
}

enum class CalendarAdditionKind {
    DAY_CONTEXT,
    SEASONAL_REQUEST,
    SPECIAL_EVENT,
}

enum class CalendarAdditionActivation {
    AUTOMATIC_FOR_DAY_KIND,
    EXPLICIT_CALENDAR_RECORD,
}

/** Metadata about a possible addition. It deliberately contains no prayer or Torah text. */
data class CalendarAdditionDefinition(
    val id: String,
    val title: UserFacingText,
    val summary: UserFacingText,
    val kind: CalendarAdditionKind,
    val activation: CalendarAdditionActivation,
    val applicableDayKinds: Set<ServiceDayKind>,
    val applicableRegions: Set<CalendarRegion> = CalendarRegion.entries.toSet(),
) {
    init {
        requireStableId(id, "calendar addition")
        require(applicableDayKinds.isNotEmpty()) {
            "A calendar addition must apply to at least one day kind."
        }
        require(applicableRegions.isNotEmpty()) {
            "A calendar addition must apply to at least one calendar region."
        }
    }
}

data class ServiceCatalog(
    val id: String,
    val title: UserFacingText,
    val maturity: CatalogMaturity,
    val notice: UserFacingText,
    val opinionProfiles: List<OpinionProfile>,
    val serviceDefinitions: List<ServiceDefinition>,
    val calendarAdditions: List<CalendarAdditionDefinition>,
) {
    init {
        requireStableId(id, "service catalog")
        require(opinionProfiles.isNotEmpty()) { "A service catalog requires an opinion profile." }
        require(serviceDefinitions.isNotEmpty()) { "A service catalog requires a service definition." }
        requireUniqueIds(opinionProfiles.map(OpinionProfile::id), "opinion profile")
        requireUniqueIds(serviceDefinitions.map(ServiceDefinition::id), "service definition")
        requireUniqueIds(calendarAdditions.map(CalendarAdditionDefinition::id), "calendar addition")
    }
}

internal fun requireStableId(id: String, kind: String) {
    require(STABLE_ID.matches(id)) {
        "A $kind ID must contain lowercase letters, digits, periods, underscores, or hyphens: '$id'."
    }
}

private fun requireUniqueIds(ids: List<String>, kind: String) {
    val duplicates = ids.groupingBy { it }.eachCount().filterValues { count -> count > 1 }.keys
    require(duplicates.isEmpty()) { "Duplicate $kind IDs: ${duplicates.sorted().joinToString()}." }
}

private val STABLE_ID = Regex("[a-z0-9][a-z0-9._-]*")
