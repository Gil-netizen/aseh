package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.ExplanationTrace
import io.github.gilnetizen.aseh.core.model.IndividualPrayerService
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.core.model.ServiceSegmentContent
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceTextField
import io.github.gilnetizen.aseh.core.model.ServiceTextProvenance
import io.github.gilnetizen.aseh.core.model.SourceUnit

/**
 * Development-only research reader for the first individual-prayer increment.
 *
 * This deliberately preserves every abbreviation in the pinned Rambam witness. It is a useful
 * source reader, not a human-approved reconstruction and not a claim that LITURGY-001 is settled.
 */
internal fun flavorIndividualPrayerService(): IndividualPrayerService =
    IndividualPrayerService(
        id = "prayer.weekday.shacharit.rambam-research-v1",
        title = "שחרית ליחיד · מחקר נוסח הרמב״ם",
        subtitle = "Weekday individual prayer · pinned Rambam research witness",
        notice = "Development research edition. The Rambam source pages are unreviewed and " +
            "abbreviate familiar passages with וכו׳ or וגו׳. ASEH leaves those gaps visible " +
            "instead of silently inventing a canonical reconstruction. This is not yet an " +
            "approved complete Nusach HaRambam.",
        segments = listOf(
            rambamSegment(
                id = "prayer.shacharit.opening",
                phase = "פתיחת שחרית",
                title = "לימוד ופתיחת היום",
                summary = "The opening order recorded in the pinned Rambam witness.",
                text = MORNING_OPENING,
                sourceId = RAMBAM_CHAPTER_1_ID,
                locator = "Order of Prayer 1, opening paragraphs",
                movement = "Prepare in a stable position and direct attention toward prayer.",
                alternative = "Remain seated or use any stable position that permits attentive prayer.",
                voice = "Read at a quiet, personally audible level.",
            ),
            rambamSegment(
                id = "prayer.shacharit.pesukei",
                phase = "פסוקי דזמרא",
                title = "ברוך שאמר וסדר המזמורים",
                summary = "Rambam's recorded opening blessing and directions for the psalm sequence.",
                text = BARUKH_SHEAMAR_AND_ORDER,
                sourceId = RAMBAM_CHAPTER_1_ID,
                locator = "Order of Prayer 1, first blessing before the songs",
                movement = "Use the posture that supports steady attention.",
                alternative = "The same text may be read seated without explanation.",
                voice = "Read privately; no communal response is required.",
            ),
            rambamSegment(
                id = "prayer.shacharit.shema-blessings",
                phase = "ברכות קריאת שמע",
                title = "ישתבח וברכות קריאת שמע",
                summary = "The witness records the sequence but abbreviates much of the familiar wording.",
                text = SHEMA_BLESSINGS,
                sourceId = RAMBAM_CHAPTER_1_ID,
                locator = "Order of Prayer 1, concluding song blessing and blessings around Shema",
                movement = "Continue in a comfortable attentive posture.",
                alternative = "No change of posture is required.",
                voice = "Read privately and pause where the visible source gap begins.",
            ),
            shemaSegment(),
            rambamSegment(
                id = "prayer.shacharit.amidah-1-9",
                phase = "תפילת העמידה",
                title = "ברכות א–ט",
                summary = "The first nine blessings in the order printed by the pinned Rambam witness.",
                text = AMIDAH_ONE_TO_NINE,
                sourceId = RAMBAM_CHAPTER_2_ID,
                locator = "Order of Prayer 2, blessings 1–9",
                movement = "Stand if standing is available to you.",
                alternative = "Pray seated or in another stable posture; the text remains the same.",
                voice = "Pray quietly at a personally audible level.",
            ),
            rambamSegment(
                id = "prayer.shacharit.amidah-10-19",
                phase = "תפילת העמידה",
                title = "ברכות י–יט",
                summary = "The remaining weekday blessings in the order printed by the pinned Rambam witness.",
                text = AMIDAH_TEN_TO_NINETEEN,
                sourceId = RAMBAM_CHAPTER_2_ID,
                locator = "Order of Prayer 2, blessings 10–19",
                movement = "Continue standing if available; finish in the posture you can sustain safely.",
                alternative = "Continue seated or in another stable posture.",
                voice = "Pray quietly at a personally audible level.",
            ),
            rambamSegment(
                id = "prayer.shacharit.tachanun",
                phase = "תחנון",
                title = "נפילת אפים",
                summary = "The supplication Rambam introduces as 'our custom,' with its source wording preserved.",
                text = TACHANUN,
                sourceId = RAMBAM_CHAPTER_3_ID,
                locator = "Order of Prayer 3, falling on the face",
                movement = "Bow or lower the head only if that movement is part of your practice and is safe.",
                alternative = "Remain upright or seated and read the same supplication.",
                voice = "Read privately, without a communal cue.",
            ),
            rambamSegment(
                id = "prayer.shacharit.supplication",
                phase = "תחנונים",
                title = "תחנונים לאחר נפילת אפים",
                summary = "Further individual supplications printed after Tachanun in the pinned witness.",
                text = AFTER_TACHANUN,
                sourceId = RAMBAM_CHAPTER_3_ID,
                locator = "Order of Prayer 3, supplications after falling on the face",
                movement = "Return to a comfortable upright position.",
                alternative = "Remain in the stable position already in use.",
                voice = "Read privately and at an unhurried pace.",
            ),
            rambamSegment(
                id = "prayer.shacharit.close",
                phase = "סיום",
                title = "סדר היום וסיום",
                summary = "The optional daily conclusion described by the pinned witness.",
                text = DAILY_CLOSE,
                sourceId = RAMBAM_CHAPTER_3_ID,
                locator = "Order of Prayer 3, daily concluding custom",
                movement = "No special movement is required.",
                alternative = "No movement is required.",
                voice = "Read privately; the source describes this section as a custom of some communities.",
            ),
        ),
        sources = listOf(
            rambamSource(
                id = RAMBAM_CHAPTER_1_ID,
                chapter = "1",
                revision = "2998697",
                sha256 = "2d5b9bc1ac76549e993d6820e021da46b38eb34416bd92dce1bc54be10d6d59e",
                relatedSegmentIds = listOf(
                    "prayer.shacharit.opening",
                    "prayer.shacharit.pesukei",
                    "prayer.shacharit.shema-blessings",
                ),
            ),
            rambamSource(
                id = RAMBAM_CHAPTER_2_ID,
                chapter = "2",
                revision = "2870245",
                sha256 = "e636300af3bc3815346365c105ba9e34a7f15c292a4504f3b7253ee516413580",
                relatedSegmentIds = listOf(
                    "prayer.shacharit.amidah-1-9",
                    "prayer.shacharit.amidah-10-19",
                ),
            ),
            rambamSource(
                id = RAMBAM_CHAPTER_3_ID,
                chapter = "3",
                revision = "2870237",
                sha256 = "6fb9733ec32e77f0e3a6cca34715de22e3dc772249360368861d2df6821d9f51",
                relatedSegmentIds = listOf(
                    "prayer.shacharit.tachanun",
                    "prayer.shacharit.supplication",
                    "prayer.shacharit.close",
                ),
            ),
            SourceUnit(
                id = SHEMA_SOURCE_ID,
                title = "Miqra according to the Masorah · Shema passages",
                locator = "Deuteronomy 6:4–9; Deuteronomy 11:13–21; Numbers 15:37–41",
                language = "Hebrew",
                body = "Exact Sefaria API v3 responses for the named Hebrew version, retrieved " +
                    "2026-10-08 with fill-in disabled. HTML presentation markup, page-only notes, " +
                    "and cantillation were removed; letters, vocalization, and verse punctuation remain.",
                edition = "Miqra according to the Masorah",
                provenance = "Hebrew Wikisource contributors via Sefaria. Raw responses and " +
                    "checksums are stored under content/sources/miqra-masorah.",
                license = CC_BY_SA_NOTICE,
                conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
                reviewState = EditorialReviewState.RESEARCHED,
                relatedPracticeIds = emptyList(),
                relatedSegmentIds = listOf("prayer.shacharit.shema"),
            ),
        ),
    )

private fun rambamSegment(
    id: String,
    phase: String,
    title: String,
    summary: String,
    text: String,
    sourceId: String,
    locator: String,
    movement: String,
    alternative: String,
    voice: String,
): ServiceSegment = ServiceSegment(
    id = id,
    phase = phase,
    title = title,
    summary = summary,
    movementCue = movement,
    accessibleAlternative = alternative,
    voiceCue = voice,
    roleCues = emptyMap(),
    explanation = ExplanationTrace(
        result = "Included in the ordinary weekday individual Shacharit research sequence.",
        facts = listOf(
            "Setting: individual",
            "Service: weekday morning",
            "Source status: pinned but not human-reviewed",
        ),
        rule = "Include in the development research sequence; preserve visible source abbreviations.",
        sourceIds = listOf(sourceId),
    ),
    content = ServiceSegmentContent(
        sourceText = availableHebrew(text),
        translation = unavailableCompanion("No separately licensed English translation is bundled."),
        transliteration = unavailableCompanion("No reviewed transliteration is bundled."),
        provenance = rambamProvenance(sourceId, locator),
    ),
)

private fun shemaSegment(): ServiceSegment = ServiceSegment(
    id = "prayer.shacharit.shema",
    phase = "קריאת שמע",
    title = "שלוש פרשיות שמע",
    summary = "The complete three biblical passages, from one exact allowlisted Hebrew edition.",
    movementCue = "Covering the eyes for the first verse is a personal-practice choice; no movement is required by this draft.",
    accessibleAlternative = "Keep your hands in any comfortable position and focus on the first verse.",
    voiceCue = "Read clearly enough to hear your own words.",
    roleCues = emptyMap(),
    explanation = ExplanationTrace(
        result = "Included because the selected service is weekday morning individual prayer.",
        facts = listOf(
            "Service: weekday morning",
            "Setting: individual",
            "Edition: Miqra according to the Masorah",
            "API fill-in: disabled",
        ),
        rule = "Include the three scriptural paragraphs of Shema in their recorded order.",
        sourceIds = listOf(SHEMA_SOURCE_ID),
    ),
    content = ServiceSegmentContent(
        sourceText = availableHebrew(SHEMA),
        translation = unavailableCompanion("No separately licensed English translation is bundled."),
        transliteration = unavailableCompanion("No reviewed transliteration is bundled."),
        provenance = ServiceTextProvenance(
            editionId = "tanakh_he_miqra_masorah_shema_20261008",
            editionTitle = "Miqra according to the Masorah",
            sourceUnitId = SHEMA_SOURCE_ID,
            locator = "Deuteronomy 6:4–9; 11:13–21; Numbers 15:37–41",
            provenance = "Exact Sefaria API v3 version selector; raw responses and checksums retained.",
            license = CC_BY_SA_NOTICE,
            conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
            reviewState = EditorialReviewState.RESEARCHED,
            editorialTreatment = "HTML display markup, page-only annotations, and cantillation removed; verses separated by line breaks.",
            punctuationSource = "Miqra according to the Masorah",
            vocalizationSource = "Miqra according to the Masorah",
            variantNotes = "This supplies the biblical passages only; it does not resolve the surrounding liturgical witness.",
        ),
    ),
)

private fun availableHebrew(text: String) = ServiceTextField(
    availability = ServiceTextAvailability.AVAILABLE,
    text = text.trimIndent(),
    languageTag = "he",
    detail = "Pinned development research text.",
)

private fun unavailableCompanion(reason: String) = ServiceTextField(
    availability = ServiceTextAvailability.NOT_APPLICABLE,
    detail = reason,
)

private fun rambamProvenance(sourceId: String, locator: String): ServiceTextProvenance {
    val revision = when (sourceId) {
        RAMBAM_CHAPTER_1_ID -> "2998697"
        RAMBAM_CHAPTER_2_ID -> "2870245"
        RAMBAM_CHAPTER_3_ID -> "2870237"
        else -> error("Unknown Rambam source ID: $sourceId")
    }
    return ServiceTextProvenance(
        editionId = "rambam_order_prayer_he_wikisource_rev$revision",
        editionTitle = "Wikisource Mishneh Torah · pinned revision $revision",
        sourceUnitId = sourceId,
        locator = locator,
        provenance = "Hebrew Wikisource contributors; exact revision retrieved 2026-10-08. The page labels itself not reviewed.",
        license = CC_BY_SA_NOTICE,
        conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
        reviewState = EditorialReviewState.RESEARCHED,
        editorialTreatment = "Verbatim paragraph grouping; MediaWiki templates/headings omitted; abbreviations preserved.",
        punctuationSource = "Pinned Wikisource revision $revision",
        vocalizationSource = "No vocalization supplied in the witness; none added.",
        variantNotes = "Unreviewed witness. Every וכו׳/וגו׳ remains an explicit unresolved gap. Not a canonical anchor.",
    )
}

private fun rambamSource(
    id: String,
    chapter: String,
    revision: String,
    sha256: String,
    relatedSegmentIds: List<String>,
): SourceUnit = SourceUnit(
    id = id,
    title = "Mishneh Torah · Order of Prayer $chapter",
    locator = "Hebrew Wikisource revision $revision",
    language = "Hebrew",
    body = "Pinned raw wikitext is retained at content/sources/rambam-wikisource. " +
        "SHA-256: $sha256. The source page is explicitly marked not reviewed and abbreviates " +
        "familiar passages; ASEH preserves those gaps.",
    edition = "Wikisource Mishneh Torah",
    provenance = "Hebrew Wikisource contributors; retrieved through the MediaWiki API on 2026-10-08.",
    license = CC_BY_SA_NOTICE,
    conclusionStatus = ConclusionStatus.SOURCE_EXPLICIT,
    reviewState = EditorialReviewState.RESEARCHED,
    relatedPracticeIds = emptyList(),
    relatedSegmentIds = relatedSegmentIds,
)

private const val RAMBAM_CHAPTER_1_ID = "source.rambam.order-prayer.1.rev2998697"
private const val RAMBAM_CHAPTER_2_ID = "source.rambam.order-prayer.2.rev2870245"
private const val RAMBAM_CHAPTER_3_ID = "source.rambam.order-prayer.3.rev2870237"
private const val SHEMA_SOURCE_ID = "source.tanakh.miqra-masorah.shema.20261008"
private const val CC_BY_SA_NOTICE =
    "CC BY-SA 4.0 · https://creativecommons.org/licenses/by-sa/4.0/ · attribution and ShareAlike apply"

private val MORNING_OPENING = """
    נהגו העם לקרות בכל יום בשחר אחר שקורין פרשת צו וברכת כהנים קורין משנה זו אלו דברים שאין להם שיעור הפאה והבכורים והראיון וגמילות חסדים ותלמוד תורה אלו דברים שאדם אוכל פירותיהן בעולם הזה והקרן קיימת לו לעולם הבא כיבוד אב ואם וגמילות חסדים ועיון תפלה וביקור חולים והשכמת בית המדרש והכנסת אורחים והבאת שלום בין אדם לחבירו ותלמוד תורה כנגד כולן אמר רבי זירא בנות ישראל הן החמירו על עצמן שאפילו רואות טיפת דם כחרדל יושבות עליה שבעה נקיים תנא דבי אליהו כל השונה הלכות בכל יום מובטח לו שהוא בן העולם הבא שנאמר הליכות עולם לו אל תקרי הליכות אלא הלכות אמר רבי אלעזר אמר רבי חנינא תלמידי חכמים מרבים שלום בעולם שנאמר וכל בניך למודי יי' וכו' מזמור לדוד יי' מי יגור באהלך מי ישכון בהר קדשך הולך תמים ופועל צדק לעולם יהא אדם ירא שמים בסתר ומודה על האמת ודובר אמת בלבבו וישכם ויאמר רבון העולמים לא על צדקותינו אנחנו מפילים תחנונינו לפניך כי על רחמיך הרבים מה אנו מה חיינו מה חסדנו מה צדקתנו מה כחנו ומה גבורתנו מה נאמר לפניך יי' אלהינו הלא כל הגבורים כאין לפניך ואנשי השם כלא היו וחכמים כבלי מדע ונבונים כבלי השכל כי כל מעשינו תהו ובהו וימי חיינו הבל לפניך כמו שכתוב בדברי קדשך ומותר האדם וגו' אבל אנחנו עמך בני בריתך בני אברהם אוהבך שנשבעת לו בהר המוריה זרע יצחק יחידך שנעקד על גבי מזבחך עדת יעקב בנך בכורך שמאהבתך שאהבת אותו ומשמחתך ששמחת בו קראת אותו ישראל וישורון:

    לפיכך אנו חייבין להודות לך ולשבחך ולפארך וליתן שבח והודאה לשמך וחייבין אנו לומר לפניך בכל יום ערב ובוקר שמע ישראל יי' אלהינו יי' אחד אשרנו מה טוב חלקנו מה נעים גורלנו מה יפה ירושתנו אשרנו שאנו משכימין ומעריבין בכל יום תמיד ערב ובוקר ואומרים שמע ישראל יי' אלהינו יי' אחד אתה הוא קודם שנברא העולם אתה הוא אחר שנברא העולם אתה הוא בעולם הזה ואתה הוא לעולם הבא אתה הוא ראשון ואתה הוא אחרון קדש שמך הגדול והקדוש בעולמך ובישועתך תרום ותגביה קרננו ברוך המקדש שמו ברבים אתה הוא יי' אלהינו בשמים ממעל ועל הארץ מתחת ובשמי השמים העליונים אתה הוא ראשון ואתה הוא אחרון ומבלעדיך אין אלהים קבץ קויך מארבע כנפות הארץ יכירו וידעו כל באי עולם כי אתה הוא האלהים לבדך לכל ממלכות הארץ אתה עשית את השמים ואת הארץ ומי בכל מעשה ידיך בעליונים או בתחתונים מי שיאמר לך מה תעשה אבינו שבשמים עשה עמנו כמו שהבטחתנו על ידי חוזך בעת ההיא אביא אתכם ובעת קבצי אתכם כי אתן אתכם לשם ולתהלה בכל עמי הארץ בשובי את שבותיכם לעיניכם אמר יי ונאמר אתה הוא יי' לבדך אתה עשית את השמים שמי השמים וכל צבאם הארץ וכל אשר עליה הימים וכל אשר בהם ואתה מחיה את כולם וצבא השמים לך משתחוים אתה הוא יי' האלהים אשר בחרת באברם והוצאתו מאור כשדים ושמת שמו אברהם אתה הוא ושנותיך לא יתמו יי' מלך יי מלך יי' ימלוך לעולם ועד יי' מלך כו' עד ברוך יי' לעולם אמן ואמן:
"""

private val BARUKH_SHEAMAR_AND_ORDER = """
    ברוך שאמר והיה העולם ברוך הוא ברוך אומר ועושה ברוך גוזר ומקיים ברוך מרחם על הארץ ברוך מרחם על הבריות ברוך מעביר אפלה ומביא אורה ברוך משלם שכר טוב ליראיו ברוך שאין לפניך לא עולה לא שכחה ולא כזב ולא מרמה לא משוא פנים ולא מקח שחד ברוך אל חי לעד וקיים לנצח ברוך אתה יי' אלהינו מלך העולם האל המהולל בפי עמו משובח ומפואר בלשון כל חסידיו ועבדיו ובשירי דוד עבדך משיחך נהללך יי' אלהינו בשבחות ובזמירות נודך ונשבחך ונפארך ונזכיר שמך מלכנו אלהינו יחיד חי העולמים משובח ומפואר עדי עד שמו ברוך אתה יי' מלך מהולל בתושבחות וקורין פסוקין אלו יהי כבוד יי' לעולם ישמח יי' במעשיו כו' אשרי כו' עד סוף תילים ואחר כך קורא פסוקים אלו ברוך יי' לעולם אמן ואמן ימלוך יי' לעולם אמן ואמן ויברך דויד את יי לעיני כל הקהל ויאמר דויד ברוך אתה יי' אלהי ישראל וכו':
"""

private val SHEMA_BLESSINGS = """
    ישתבח שמך לעד מלכנו האל המלך הגדול והקדוש בשמים ובארץ כי לך נאה יי' אלהינו ואלהי אבותינו וכו' וקורא השירה עד סופה כמנהג המקום.

    ברוך אתה יי' אלהינו מלך העולם יוצר אור ובורא חשך כו' עד יוצר המאורות. ברכה שנייה אהבת עולם אהבתנו יי' אלהינו חמלה גדולה ויתירה חמלת עלינו אבינו מלכנו כו' עד בעמו ישראל באהבה.

    אמת ויציב וכו' עד ברוך אתה יי' גאל ישראל.
"""

private val AMIDAH_ONE_TO_NINE = """
    (א) ברוך אתה יי' אלהינו ואלהי אבותינו אלהי אברהם אלהי יצחק ואלהי יעקב האל הגדול הגבור והנורא וכו':

    (ב) אתה גבור לעולם אדני מחיה מתים אתה רב להושיע (מוריד הטל) (משיב הרוח ומוריד הגשם) מכלכל חיים בחסד מחיה מתים ברחמים רבים סומך נופלים וכו':

    (ג) אתה קדוש ושמך קדוש וקדושים בכל יום יהללוך סלה ברוך אתה יי' האל הקדוש:

    (ד) אתה חונן לאדם דעת ומלמד לאנוש בינה חננו מאתך דעה חכמה ובינה והשכל ברוך אתה יי' חונן הדעת:

    (ה) השיבנו אבינו לתורתך ודבקנו במצותיך וקרבנו מלכנו לעבודתך והחזירנו בתשובה שלימה לפניך ברוך אתה יי' הרוצה בתשובה:

    (ו) סלח לנו אבינו כי חטאנו מחול לנו מלכנו כי פשענו לך כי אל טוב וסלח אתה ברוך אתה יי' חנון המרבה לסלוח:

    (ז) ראה נא בענינו וריבה ריבנו ודון דיננו ומהר לגאלנו כי אל מלך גואל חזק אתה ברוך אתה יי' גואל ישראל:

    (ח) רפאנו יי' אלהינו ונרפא הושיענו ונושעה כי תהלתנו אתה והעלה רפואה שלימה לכל תחלואינו כי אל רופא ורחמן אתה ברוך אתה יי' רופא חולי עמו ישראל:

    (ט) ברכנו יי' אלהינו בכל מעשה ידינו וברך את שנותינו ותן (טל ומטר ל) ברכה על כל פני האדמה ושבע את העולם מברכותיך ורוה פני תבל ברוך אתה יי' מברך השנים:
"""

private val AMIDAH_TEN_TO_NINETEEN = """
    (י) תקע בשופר גדול לחירותנו ושא נס לקבץ את כל גליותינו מארבע כנפות כל הארץ לארצנו ברוך אתה יי' מקבץ נדחי עמו ישראל:

    (יא) השיבה שופטינו כבראשונה ויועצינו כבתחילה והסר ממנו יגון ואנחה ומלוך עלינו אתה לבדך בחסד וברחמים בצדק ובמשפט ברוך אתה יי' מלך אוהב צדקה ומשפט:

    (יב) למלשינים אל תהי תקוה וכל האפיקורוסין כולם כרגע יאבדו ומלכות זדון תעקר ותשבר במהרה בימנו ברוך אתה יי' שובר רשעים ומכניע זדים:

    (יג) על החסידים ועל הצדיקים ועל גרי הצדק ועל שארית עמך בית ישראל יהמו רחמיך יי' אלהינו ותן שכר טוב לכל הבוטחים בשמך באמת וכו':

    (יד) תשכון בתוך ירושלים עירך כאשר דברת ובנה אותה בנין עולם במהרה בימינו ברוך אתה יי' בונה ירושלים:

    (טו) את צמח דוד במהרה תצמיח וקרנו תרום בישועתך ברוך אתה יי' מצמיח קרן ישועה:

    (טז) שמע קולנו יי' אלהינו וחוס ורחם עלינו וקבל ברחמים וברצון את תפלתינו מלכנו ריקם אל תשיבנו כי אתה שומע וכו':

    (יז) רצה יי' אלהינו בעמך ישראל ולתפלתם שעה והשב העבודה לדביר ביתך ואשי ישראל ותפלתם וכו' ברוך אתה יי' המחזיר שכינתו לציון:

    (יח) מודים אנחנו לך שאתה הוא יי' אלהינו ואלהי אבותינו צור חיינו ומגן ישענו אתה לדור ודור נודה לך ונספר תהלתך על חיינו וכו':

    (יט) שים שלום טובה וברכה חן וחסד ורחמים עלינו ועל ישראל עמך וברכנו כולנו ממאור פניך נתת לנו יי' אלהינו תורה וחיים אהבה וכו':
"""

private val TACHANUN = """
    מנהגנו להתחנן בנפילת פנים בדברים ופסוקים אלו פעמים בכולן ופעמים במקצתן לפיכך אני כורע ומשתחוה ומתחנן לפניך אדון העולם אלהי האלהים ואדוני האדונים כי לא על צדקותינו אנחנו מפילים תחנונינו לפניך כי על רחמיך הרבים יי' שמעה יי' סלחה יי' הקשיבה ועשה אל תאחר מה נאמר לפניך השם מה נדבר ומה נצטדק חטאנו עוינו והרשענו ומרדנו וסרנו ממצותיך וממשפטיך לך יי' הצדקה ולנו בשת הפנים הושחרו פנינו מפני חטאתינו ונכפפה קומתנו מפני אשמותינו אין לנו פה להשיב ולא מצח להרים ראש אלהי בושתי ונכלמתי להרים אלהי פני אליך כי עוונותינו רבו עד למעלה ראש ואשמתנו גדלה עד לשמים אין בנו מעשים עשה עמנו צדקה למען שמך והושיענו כמו שהבטחתנו על ידי נביאך למען שמי אאריך אפי ותהלתי אחטם לך לבלתי הכריתך לא למענכם אני עושה בית ישראל כי אם לשם קדשי אשר חללתם בגוים אשר באתם שם לא לנו יי' לא לנו כי לשמך תן כבוד על חסדך על אמתך למה יאמרו הגוים איה נא אלהיהם אנא יי' אל תפן אל קשי העם הזה ואל רשעו ואל חטאתו סלח נא לעון העם הזה כגודל חסדך וכאשר נשאתה לעם הזה ממצרים ועד הנה וסלחת לעוננו כי רב הוא יי' שמעה יי' סלחה יי' הקשיבה ועשה ואל תאחר למענך אלהי כי שמך נקרא על עירך ועל עמך:
"""

private val AFTER_TACHANUN = """
    ואנחנו לא נדע מה נעשה כי עליך עינינו זכר רחמיך יי' וחסדיך כי מעולם המה אל תזכור לנו עונות ראשונים מהר יקדמונו רחמיך כי דלונו מאד קומה עזרתה לנו ופדנו למען חסדך יהי חסדך יי' עלינו כאשר יחלנו לך אם עונות תשמר יה יי' מי יעמוד כי עמך הסליחה למען תורא יי' הושיעה המלך יעננו ביום קראנו כי הוא ידע יצרנו זכור כי עפר אנחנו עזרנו אלהי ישענו על דבר כבוד שמך והצילנו וכפר על חטאתינו למען שמך:

    יי' אלהי אברהם יצחק וישראל אבותינו שמרה זאת לעולם ליצר מחשבות לבב עמך והכן לבבם אליך והוא רחום יכפר עון וגו' כי אתה יי' טוב וסלח ורב חסד לכל קוראיך צדקתך צדק לעולם ותורתך אמת מי אל כמוך נושא עון ועובר על פשע ישוב ירחמנו יכבוש עוונותינו וגו' ברוך יי' יום יום יעמס לנו האל ישועתנו סלה יי' צבאות עמנו משגב לנו אלהי יעקב סלה יי' צבאות אשרי אדם בוטח בך ברוך אדוננו ברוך בוראנו ברוך שבראנו לכבודו והבדילנו מן התועים ונתן לנו תורת אמת ע"י משה רבינו וחיי עולם נטע בתוכנו הרחמן יפתח לבנו לתלמוד תורתו ויתן בלבנו אהבתו ויראתו ותורתו לעשות רצונו ולעובדו בלבב שלם ובנפש חפצה למען לא ניגע לריק ולא נלד לבהלה כן יהי רצון ורחמים מלפניך יי' אלהינו שנחיה לשמור חקיך בעולם הזה ולימות המשיח כדי שנזכה ונירש טוב לחיי העולם הבא למען יזמרך כבוד ולא ידום יי' אלהי לעולם אודך יהיו לרצון אמרי פי והגיון לבי לפניך יי' צורי וגואלי:
"""

private val DAILY_CLOSE = """
    נהגו מקצת העם לקרוא בכל יום אחר תחנונים אלו שיר מזמור שהיו הלוים אומרים בבית המקדש באותו היום וקורין לדוד אליך יי' נפשי אשא כל המזמור וקורין אמר רבי אלעזר אמר רבי חנינא תלמידי חכמים מרבים שלום וכו' אין כאלהינו אין כאדוננו אין כמלכנו אין כמושיענו מי כאלהינו מי כאדוננו מי כמלכנו מי כמושיענו נודה לאלהינו נודה לאדוננו נודה למלכנו נודה למושיענו אתה הוא אלהינו אתה הוא אדוננו אתה הוא מלכנו אתה הוא מושיענו אתה תקום תרחם ציון כי עת לחננה כי בא מועד אך צדיקים יודו לשמך ישבו ישרים את פניך ויבטחו בך יודעי שמך כי לא עזבת דורשיך יי' כי כל העמים ילכו איש בשם אלהיו ואנחנו נלך בשם יי אלהינו לעולם ועד:
"""

private val SHEMA = """
    שְׁמַע יִשְׂרָאֵל יְהֹוָה אֱלֹהֵינוּ יְהֹוָה ׀ אֶחָד׃
    וְאָהַבְתָּ אֵת יְהֹוָה אֱלֹהֶיךָ בְּכׇל־לְבָבְךָ וּבְכׇל־נַפְשְׁךָ וּבְכׇל־מְאֹדֶךָ׃
    וְהָיוּ הַדְּבָרִים הָאֵלֶּה אֲשֶׁר אָנֹכִי מְצַוְּךָ הַיּוֹם עַל־לְבָבֶךָ׃
    וְשִׁנַּנְתָּם לְבָנֶיךָ וְדִבַּרְתָּ בָּם בְּשִׁבְתְּךָ בְּבֵיתֶךָ וּבְלֶכְתְּךָ בַדֶּרֶךְ וּבְשׇׁכְבְּךָ וּבְקוּמֶךָ׃
    וּקְשַׁרְתָּם לְאוֹת עַל־יָדֶךָ וְהָיוּ לְטֹטָפֹת בֵּין עֵינֶיךָ׃
    וּכְתַבְתָּם עַל־מְזֻזוֹת בֵּיתֶךָ וּבִשְׁעָרֶיךָ׃

    וְהָיָה אִם־שָׁמֹעַ תִּשְׁמְעוּ אֶל־מִצְוֺתַי אֲשֶׁר אָנֹכִי מְצַוֶּה אֶתְכֶם הַיּוֹם לְאַהֲבָה אֶת־יְהֹוָה אֱלֹהֵיכֶם וּלְעׇבְדוֹ בְּכׇל־לְבַבְכֶם וּבְכׇל־נַפְשְׁכֶם׃
    וְנָתַתִּי מְטַר־אַרְצְכֶם בְּעִתּוֹ יוֹרֶה וּמַלְקוֹשׁ וְאָסַפְתָּ דְגָנֶךָ וְתִירֹשְׁךָ וְיִצְהָרֶךָ׃
    וְנָתַתִּי עֵשֶׂב בְּשָׂדְךָ לִבְהֶמְתֶּךָ וְאָכַלְתָּ וְשָׂבָעְתָּ׃
    הִשָּׁמְרוּ לָכֶם פֶּן יִפְתֶּה לְבַבְכֶם וְסַרְתֶּם וַעֲבַדְתֶּם אֱלֹהִים אֲחֵרִים וְהִשְׁתַּחֲוִיתֶם לָהֶם׃
    וְחָרָה אַף־יְהֹוָה בָּכֶם וְעָצַר אֶת־הַשָּׁמַיִם וְלֹא־יִהְיֶה מָטָר וְהָאֲדָמָה לֹא תִתֵּן אֶת־יְבוּלָהּ וַאֲבַדְתֶּם מְהֵרָה מֵעַל הָאָרֶץ הַטֹּבָה אֲשֶׁר יְהֹוָה נֹתֵן לָכֶם׃
    וְשַׂמְתֶּם אֶת־דְּבָרַי אֵלֶּה עַל־לְבַבְכֶם וְעַל־נַפְשְׁכֶם וּקְשַׁרְתֶּם אֹתָם לְאוֹת עַל־יֶדְכֶם וְהָיוּ לְטוֹטָפֹת בֵּין עֵינֵיכֶם׃
    וְלִמַּדְתֶּם אֹתָם אֶת־בְּנֵיכֶם לְדַבֵּר בָּם בְּשִׁבְתְּךָ בְּבֵיתֶךָ וּבְלֶכְתְּךָ בַדֶּרֶךְ וּבְשׇׁכְבְּךָ וּבְקוּמֶךָ׃
    וּכְתַבְתָּם עַל־מְזוּזוֹת בֵּיתֶךָ וּבִשְׁעָרֶיךָ׃
    לְמַעַן יִרְבּוּ יְמֵיכֶם וִימֵי בְנֵיכֶם עַל הָאֲדָמָה אֲשֶׁר נִשְׁבַּע יְהֹוָה לַאֲבֹתֵיכֶם לָתֵת לָהֶם כִּימֵי הַשָּׁמַיִם עַל־הָאָרֶץ׃

    וַיֹּאמֶר יְהֹוָה אֶל־מֹשֶׁה לֵּאמֹר׃
    דַּבֵּר אֶל־בְּנֵי יִשְׂרָאֵל וְאָמַרְתָּ אֲלֵהֶם וְעָשׂוּ לָהֶם צִיצִת עַל־כַּנְפֵי בִגְדֵיהֶם לְדֹרֹתָם וְנָתְנוּ עַל־צִיצִת הַכָּנָף פְּתִיל תְּכֵלֶת׃
    וְהָיָה לָכֶם לְצִיצִת וּרְאִיתֶם אֹתוֹ וּזְכַרְתֶּם אֶת־כׇּל־מִצְוֺת יְהֹוָה וַעֲשִׂיתֶם אֹתָם וְלֹא־תָתוּרוּ אַחֲרֵי לְבַבְכֶם וְאַחֲרֵי עֵינֵיכֶם אֲשֶׁר־אַתֶּם זֹנִים אַחֲרֵיהֶם׃
    לְמַעַן תִּזְכְּרוּ וַעֲשִׂיתֶם אֶת־כׇּל־מִצְוֺתָי וִהְיִיתֶם קְדֹשִׁים לֵאלֹהֵיכֶם׃
    אֲנִי יְהֹוָה אֱלֹהֵיכֶם אֲשֶׁר הוֹצֵאתִי אֶתְכֶם מֵאֶרֶץ מִצְרַיִם לִהְיוֹת לָכֶם לֵאלֹהִים אֲנִי יְהֹוָה אֱלֹהֵיכֶם׃
"""
