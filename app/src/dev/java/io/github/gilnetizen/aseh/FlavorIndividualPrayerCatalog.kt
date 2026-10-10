package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.ExplanationTrace
import io.github.gilnetizen.aseh.core.model.IndividualPrayerKind
import io.github.gilnetizen.aseh.core.model.IndividualPrayerService
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.core.model.ServiceSegmentContent
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceTextField
import io.github.gilnetizen.aseh.core.model.ServiceTextProvenance
import io.github.gilnetizen.aseh.core.model.SourceUnit

/** Readable, offline weekday individual-prayer research flows for the development review build. */
internal fun flavorIndividualPrayerServices(): List<IndividualPrayerService> = listOf(
    shacharitService(),
    minchaService(),
    arvitService(),
)

private fun shacharitService(): IndividualPrayerService {
    val serviceId = "prayer.weekday.shacharit.rambam-baladi-research-v2"
    val segments = listOf(
        prayerSegment(
            serviceId = serviceId,
            suffix = "morning",
            phase = "השכמת הבוקר",
            title = "השכמה וברכות השחר",
            summary = "From waking through the ordinary morning blessings.",
            text = SHACHARIT_MORNING_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Sections השכמת הבוקר and ברכות השחר",
            movement = "Use the posture appropriate to each morning action; pause before a blessing when needed.",
            alternative = "Every text may be read seated or in another stable posture.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "torah-offerings",
            phase = "ברכות התורה",
            title = "ברכות התורה וסדר התמיד",
            summary = "Torah blessings, the daily offering passages, and the recorded opening study order.",
            text = SHACHARIT_TORAH_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Sections ברכות התורה ופרשת התמיד through רבון כל העולמים",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "zemirot-one",
            phase = "זמירות",
            title = "ברוך שאמר ותחילת הזמירות",
            summary = "The opening blessing and first half of the complete weekday song sequence.",
            text = SHACHARIT_ZEMIROT_ONE_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Section זמירות through תהלה לדוד",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "zemirot-two",
            phase = "זמירות",
            title = "פרקי הללויה עד ישתבח",
            summary = "The remaining psalms, Song at the Sea, and Yishtabach.",
            text = SHACHARIT_ZEMIROT_TWO_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Sections פרקי הללויה through ישתבח",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "shema",
            phase = "קריאת שמע וברכותיה",
            title = "ברכות קריאת שמע ושלוש הפרשיות",
            summary = "The complete morning blessings and all three biblical paragraphs of Shema.",
            text = SHACHARIT_SHEMA_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Section קריאת שמע וברכותיה",
            movement = "Covering the eyes for the first verse is a personal-practice choice.",
            alternative = "Keep your hands in any comfortable position and focus on the first verse.",
            voice = "Read clearly enough to hear your own words.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "amidah",
            phase = "תפילת העמידה",
            title = "תשע עשרה ברכות ליום חול",
            summary = "The full ordinary weekday Amidah; summer and winter wording remains visibly labeled.",
            text = WEEKDAY_AMIDAH_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Section תפילת עמידה; ordinary individual lines selected",
            movement = "Stand with feet together if standing is available and safe.",
            alternative = "Pray seated or in another stable posture; the text remains the same.",
            voice = "Pray quietly at a personally audible level.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "tachanun",
            phase = "תחנון",
            title = "נפילת אפים",
            summary = "Rambam's explicit individual supplication after the Amidah.",
            text = RAMBAM_TACHANUN_TEXT,
            source = RAMBAM_ORDER_THREE_SOURCE,
            locator = "Mishneh Torah, Order of Prayer 3, נפילת אפים",
            movement = "Bow or lower the head only when that movement is part of your practice and is safe.",
            alternative = "Remain upright or seated and read the same supplication.",
        ),
    )
    return individualService(
        id = serviceId,
        kind = IndividualPrayerKind.SHACHARIT,
        title = "שחרית ליחיד",
        subtitle = "סדר הרמב״ם · השלמה בלדית לקריאה רצופה",
        segments = segments,
    )
}

private fun minchaService(): IndividualPrayerService {
    val serviceId = "prayer.weekday.mincha.rambam-baladi-research-v2"
    val segments = listOf(
        prayerSegment(
            serviceId = serviceId,
            suffix = "opening",
            phase = "פתיחת מנחה",
            title = "פסוקי פתיחה",
            summary = "The ordinary verses immediately before the weekday Mincha prayer.",
            text = MINCHA_OPENING_TEXT,
            source = MINCHA_SOURCE,
            locator = "Weekday Mincha, verses before the Amidah",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "amidah",
            phase = "תפילת העמידה",
            title = "תשע עשרה ברכות ליום חול",
            summary = "The full ordinary weekday Amidah; summer and winter wording remains visibly labeled.",
            text = WEEKDAY_AMIDAH_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Pinned Baladi weekday Amidah; ordinary individual lines selected",
            movement = "Stand with feet together if standing is available and safe.",
            alternative = "Pray seated or in another stable posture; the text remains the same.",
            voice = "Pray quietly at a personally audible level.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "tachanun",
            phase = "תחנון",
            title = "נפילת אפים ותחנונים",
            summary = "The complete ordinary supplication, without the separately labeled Ten Days of Repentance addition.",
            text = MINCHA_TACHANUN_TEXT,
            source = MINCHA_SOURCE,
            locator = "Weekday Mincha, individual supplication after the Amidah",
            movement = "Bow or lower the head only when that movement is part of your practice and is safe.",
            alternative = "Remain upright or seated and read the same supplication.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "psalms",
            phase = "סיום מנחה",
            title = "מזמורי הערב",
            summary = "Psalms 141 and 142 from the pinned Baladi Mincha page.",
            text = MINCHA_EVENING_PSALMS_TEXT,
            source = MINCHA_SOURCE,
            locator = "Weekday Mincha, Psalms 141–142",
        ),
    )
    return individualService(
        id = serviceId,
        kind = IndividualPrayerKind.MINCHA,
        title = "מנחה ליחיד",
        subtitle = "סדר הרמב״ם · השלמה בלדית לקריאה רצופה",
        segments = segments,
    )
}

private fun arvitService(): IndividualPrayerService {
    val serviceId = "prayer.weekday.arvit.rambam-baladi-research-v2"
    val segments = listOf(
        prayerSegment(
            serviceId = serviceId,
            suffix = "blessings",
            phase = "ברכות קריאת שמע",
            title = "מעריב ערבים ואהבת עולם",
            summary = "The evening opening and two blessings before Shema, with the communal call omitted.",
            text = ARVIT_BLESSINGS_TEXT,
            source = ARVIT_SOURCE,
            locator = "Weekday Arvit, opening and blessings before Shema",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "shema",
            phase = "קריאת שמע וברכותיה",
            title = "שלוש פרשיות שמע וברכות הערב",
            summary = "The complete three paragraphs of Shema and the following evening blessings.",
            text = ARVIT_SHEMA_TEXT,
            source = ARVIT_SOURCE,
            locator = "Weekday Arvit, Shema through the closing evening verses",
            movement = "Covering the eyes for the first verse is a personal-practice choice.",
            alternative = "Keep your hands in any comfortable position and focus on the first verse.",
            voice = "Read clearly enough to hear your own words.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "amidah",
            phase = "תפילת העמידה",
            title = "תשע עשרה ברכות ליום חול",
            summary = "The full ordinary weekday Amidah; summer and winter wording remains visibly labeled.",
            text = WEEKDAY_AMIDAH_TEXT,
            source = SHACHARIT_SOURCE,
            locator = "Pinned Baladi weekday Amidah; ordinary individual lines selected",
            movement = "Stand with feet together if standing is available and safe.",
            alternative = "Pray seated or in another stable posture; the text remains the same.",
            voice = "Pray quietly at a personally audible level.",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "psalms",
            phase = "סיום ערבית",
            title = "מזמורי שמירה",
            summary = "Psalms 8 and 121 from the pinned Arvit page.",
            text = ARVIT_CLOSE_PSALMS_TEXT,
            source = ARVIT_SOURCE,
            locator = "Weekday Arvit, concluding Psalms 8 and 121",
        ),
        prayerSegment(
            serviceId = serviceId,
            suffix = "aleinu",
            phase = "סיום ערבית",
            title = "עלינו לשבח",
            summary = "The complete ordinary Aleinu text; a source-side seasonal shorthand was omitted.",
            text = ALEINU_TEXT,
            source = ARVIT_SOURCE,
            locator = "Weekday Arvit, עלינו לשבח",
        ),
    )
    return individualService(
        id = serviceId,
        kind = IndividualPrayerKind.ARVIT,
        title = "ערבית ליחיד",
        subtitle = "סדר הרמב״ם · השלמה בלדית לקריאה רצופה",
        segments = segments,
    )
}

private fun individualService(
    id: String,
    kind: IndividualPrayerKind,
    title: String,
    subtitle: String,
    segments: List<ServiceSegment>,
): IndividualPrayerService {
    val sourceRefs = (
        listOf(RAMBAM_ANCHOR_SOURCE) + segments.mapNotNull { segment ->
            SOURCES_BY_ID[segment.content.provenance.sourceUnitId]
        }
        ).distinctBy(PrayerSourceRef::id)
    val segmentIdsBySource = segments.groupBy { segment -> segment.content.provenance.sourceUnitId }
    return IndividualPrayerService(
        id = id,
        kind = kind,
        title = title,
        subtitle = subtitle,
        notice = "Development research edition. The service structure is compared with " +
            "Rambam's Order of Prayer. The readable Hebrew comes primarily from pinned, " +
            "unreviewed Yemenite Baladi pages; each segment identifies its actual text source. " +
            "This is not yet a human-approved canonical Nusach HaRambam edition.",
        segments = segments,
        sources = sourceRefs.map { source ->
            sourceUnit(
                source = source,
                relatedSegmentIds = if (source.id == RAMBAM_ANCHOR_SOURCE.id) {
                    segments.map(ServiceSegment::id)
                } else {
                    segmentIdsBySource[source.id].orEmpty().map(ServiceSegment::id)
                },
            )
        },
    )
}

private fun prayerSegment(
    serviceId: String,
    suffix: String,
    phase: String,
    title: String,
    summary: String,
    text: String,
    source: PrayerSourceRef,
    locator: String,
    movement: String = "Use a comfortable posture that supports attention.",
    alternative: String = "Remain seated or use any stable posture.",
    voice: String = "Read privately at a quiet, personally audible level.",
): ServiceSegment = ServiceSegment(
    id = "$serviceId.$suffix",
    phase = phase,
    title = title,
    summary = summary,
    movementCue = movement,
    accessibleAlternative = alternative,
    voiceCue = voice,
    roleCues = emptyMap(),
    explanation = ExplanationTrace(
        result = "Included in this ordinary weekday individual-prayer service.",
        facts = listOf(
            "Setting: individual",
            "Service: ${serviceId.substringAfter("prayer.weekday.").substringBefore('.')}",
            "Runtime text: pinned development witness",
        ),
        rule = "Include the ordinary individual passage; omit communal leader and response text.",
        sourceIds = listOf(source.id, RAMBAM_ANCHOR_SOURCE.id).distinct(),
    ),
    content = ServiceSegmentContent(
        sourceText = ServiceTextField(
            availability = ServiceTextAvailability.AVAILABLE,
            text = text,
            languageTag = "he",
            detail = "Pinned development research text.",
        ),
        translation = unavailableCompanion("No separately licensed English translation is bundled."),
        transliteration = unavailableCompanion("No reviewed transliteration is bundled."),
        provenance = ServiceTextProvenance(
            editionId = source.editionId,
            editionTitle = source.editionTitle,
            sourceUnitId = source.id,
            locator = locator,
            provenance = source.provenance,
            license = CC_BY_SA_NOTICE,
            conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
            reviewState = EditorialReviewState.DRAFTED,
            editorialTreatment = source.editorialTreatment,
            punctuationSource = source.editionTitle,
            vocalizationSource = source.editionTitle,
            variantNotes = source.variantNotes,
        ),
    ),
)

private fun unavailableCompanion(reason: String) = ServiceTextField(
    availability = ServiceTextAvailability.NOT_APPLICABLE,
    detail = reason,
)

private fun sourceUnit(
    source: PrayerSourceRef,
    relatedSegmentIds: List<String>,
): SourceUnit = SourceUnit(
    id = source.id,
    title = source.editionTitle,
    locator = source.locator,
    language = "Hebrew",
    body = source.body,
    edition = source.editionId,
    provenance = source.provenance,
    license = CC_BY_SA_NOTICE,
    conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
    reviewState = EditorialReviewState.DRAFTED,
    relatedPracticeIds = emptyList(),
    relatedSegmentIds = relatedSegmentIds,
)

private data class PrayerSourceRef(
    val id: String,
    val editionId: String,
    val editionTitle: String,
    val locator: String,
    val body: String,
    val provenance: String,
    val editorialTreatment: String,
    val variantNotes: String,
)

private val SHACHARIT_SOURCE = PrayerSourceRef(
    id = "source.siddur.baladi.shacharit.rev3081633",
    editionId = "wikisource_yemenite_baladi_shacharit_rev3081633",
    editionTitle = "Hebrew Wikisource · Yemenite Baladi weekday Shacharit · revision 3081633",
    locator = "סידור/נוסח תימן בלדי/שחרית לחול",
    body = "Pinned wikitext and rendered display snapshot retrieved 2026-10-10.",
    provenance = "Hebrew Wikisource contributors. Raw and display checksums are retained under content/sources/baladi-wikisource.",
    editorialTreatment = "Rendered templates captured; display controls removed; Unicode NFKC applied; ordinary individual lines selected.",
    variantNotes = "Collaborative unreviewed Baladi page, used as a completion witness rather than a canonical Rambam recension.",
)

private val MINCHA_SOURCE = PrayerSourceRef(
    id = "source.siddur.baladi.mincha.rev1076941",
    editionId = "wikisource_yemenite_baladi_mincha_rev1076941",
    editionTitle = "Hebrew Wikisource · Yemenite Baladi weekday Mincha · revision 1076941",
    locator = "סידור/נוסח תימן בלדי/מנחה לחול",
    body = "Pinned wikitext and display text retrieved 2026-10-10.",
    provenance = "Hebrew Wikisource contributors. Raw and display checksums are retained under content/sources/baladi-wikisource.",
    editorialTreatment = "Display controls removed; Unicode NFKC applied; communal Kaddish, repetition, and seasonal addition lines omitted.",
    variantNotes = "Collaborative unreviewed Baladi page, used as a completion witness rather than a canonical Rambam recension.",
)

private val ARVIT_SOURCE = PrayerSourceRef(
    id = "source.siddur.baladi.arvit.rev2947373",
    editionId = "wikisource_yemenite_baladi_arvit_rev2947373",
    editionTitle = "Hebrew Wikisource · Yemenite Baladi weekday Arvit · revision 2947373",
    locator = "סידור/נוסח תימן בלדי/ערבית לחול",
    body = "Pinned wikitext and display text retrieved 2026-10-10.",
    provenance = "Hebrew Wikisource contributors. Raw and display checksums are retained under content/sources/baladi-wikisource.",
    editorialTreatment = "Display controls removed; Unicode NFKC applied; communal call/Kaddish and a seasonal shorthand omitted.",
    variantNotes = "Collaborative unreviewed Baladi page, used as a completion witness rather than a canonical Rambam recension.",
)

private val RAMBAM_ORDER_THREE_SOURCE = PrayerSourceRef(
    id = "source.rambam.order-prayer.3.rev2870237",
    editionId = "rambam_order_prayer_he_wikisource_rev2870237",
    editionTitle = "Wikisource Mishneh Torah · Order of Prayer 3 · revision 2870237",
    locator = "Mishneh Torah, The Order of Prayer 3",
    body = "Pinned Rambam Order of Prayer source page retrieved 2026-10-08.",
    provenance = "Hebrew Wikisource contributors; exact revision and checksum retained under content/sources/rambam-wikisource.",
    editorialTreatment = "The explicit supplication is grouped as one reader segment; source wording is otherwise retained.",
    variantNotes = "The source page is marked unreviewed. This explicit passage is not used to fill other source abbreviations.",
)

private val RAMBAM_ANCHOR_SOURCE = PrayerSourceRef(
    id = "source.rambam.order-prayer.anchor.20261008",
    editionId = "rambam_order_prayer_he_wikisource_research_20261008",
    editionTitle = "Wikisource Mishneh Torah · pinned Order of Prayer revisions",
    locator = "Order of Prayer 1–3 · revisions 2998697, 2870245, and 2870237",
    body = "Service-level structural comparison only. This unit is not the text source for Baladi-completed segments; each segment identifies its runtime text witness separately.",
    provenance = "Hebrew Wikisource contributors; exact revisions and checksums retained under content/sources/rambam-wikisource.",
    editorialTreatment = "Used to compare broad order and identify abbreviations. It does not establish wording attributed to the Baladi witnesses.",
    variantNotes = "Unreviewed witness and incomplete as a standalone siddur. Human editorial selection remains unresolved.",
)

private val SOURCES_BY_ID = listOf(
    SHACHARIT_SOURCE,
    MINCHA_SOURCE,
    ARVIT_SOURCE,
    RAMBAM_ORDER_THREE_SOURCE,
    RAMBAM_ANCHOR_SOURCE,
).associateBy(PrayerSourceRef::id)

private const val CC_BY_SA_NOTICE =
    "Creative Commons Attribution-ShareAlike 4.0 International · Hebrew Wikisource contributors"

private val RAMBAM_TACHANUN_TEXT = """
    מנהגנו להתחנן בנפילת פנים בדברים ופסוקים אלו פעמים בכולן ופעמים במקצתן לפיכך אני כורע ומשתחוה ומתחנן לפניך אדון העולם אלהי האלהים ואדוני האדונים כי לא על צדקותינו אנחנו מפילים תחנונינו לפניך כי על רחמיך הרבים יי' שמעה יי' סלחה יי' הקשיבה ועשה אל תאחר מה נאמר לפניך השם מה נדבר ומה נצטדק חטאנו עוינו והרשענו ומרדנו וסרנו ממצותיך וממשפטיך לך יי' הצדקה ולנו בשת הפנים הושחרו פנינו מפני חטאתינו ונכפפה קומתנו מפני אשמותינו אין לנו פה להשיב ולא מצח להרים ראש אלהי בושתי ונכלמתי להרים אלהי פני אליך כי עוונותינו רבו עד למעלה ראש ואשמתנו גדלה עד לשמים אין בנו מעשים עשה עמנו צדקה למען שמך והושיענו כמו שהבטחתנו על ידי נביאך למען שמי אאריך אפי ותהלתי אחטם לך לבלתי הכריתך לא למענכם אני עושה בית ישראל כי אם לשם קדשי אשר חללתם בגוים אשר באתם שם לא לנו יי' לא לנו כי לשמך תן כבוד על חסדך על אמתך למה יאמרו הגוים איה נא אלהיהם אנא יי' אל תפן אל קשי העם הזה ואל רשעו ואל חטאתו סלח נא לעון העם הזה כגודל חסדך וכאשר נשאתה לעם הזה ממצרים ועד הנה וסלחת לעוננו כי רב הוא יי' שמעה יי' סלחה יי' הקשיבה ועשה ואל תאחר למענך אלהי כי שמך נקרא על עירך ועל עמך:
""".trimIndent()
