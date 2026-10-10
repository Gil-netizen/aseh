package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.DisputedPracticeDossier
import io.github.gilnetizen.aseh.core.model.DossierAdoptionOption
import io.github.gilnetizen.aseh.core.model.DossierArgument
import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.EditorialReviewState

/**
 * A synthetic development dossier that exercises the disputed-practice
 * workflow without asserting a religious conclusion or importing unlicensed
 * source text.
 */
internal fun developmentLightingDossier() = DisputedPracticeDossier(
    id = "dossier.shabbat-electric-lighting",
    title = "Electricity and lighting on Shabbat",
    question = "How will this qahal prepare and operate electric lighting for its Shabbat-morning gathering?",
    scope = "One local qahal's interim operating record. It is not a universal ruling or a source conclusion.",
    factualQuestions = listOf(
        "Which lights, switches, timers, sensors, emergency systems, and accessibility devices are present?",
        "Which actions change a circuit, current draw, stored state, display, motor, heat, or automated schedule?",
        "What lighting and device access is needed for safe movement, readable print, and equal participation?",
        "Which facts have been verified by a qualified technical person, and which remain assumptions?",
    ),
    historicalPosition = "NOT ESTABLISHED in this development pack. Exact Rambam, Geonic, responsa, and competing-source records require licensed editions and named human review.",
    arguments = listOf(
        DossierArgument(
            id = "argument.prepared-precaution",
            title = "Interim prepared-lighting precaution",
            claim = "Prepare needed light and accessibility equipment before the gathering and avoid planned changes while the group follows the packet.",
            supports = listOf(
                "It gives the qahal a concrete offline plan while the source question remains unresolved.",
                "It surfaces safety and accessibility needs during preparation instead of during the service.",
            ),
            challenges = listOf(
                "Precaution does not establish the legal category of electricity or any particular device action.",
                "A blanket avoidance plan may fail to address emergency, safety, or access facts.",
            ),
            sourceIds = listOf("source.demo.dossier-method", "source.demo.access-path"),
        ),
        DossierArgument(
            id = "argument.fact-specific-review",
            title = "Fact-specific review before a broad rule",
            claim = "Document each device and action separately, then require reviewed source analysis before treating one answer as covering all electric lighting.",
            supports = listOf(
                "Switches, sensors, timers, displays, and emergency systems can involve different physical facts.",
                "It prevents a local adoption from being mistaken for evidence or universal authority.",
            ),
            challenges = listOf(
                "It leaves the group needing an interim operational plan for the next gathering.",
                "Technical detail alone cannot supply the missing historical and legal analysis.",
            ),
            sourceIds = listOf("source.demo.dossier-method", "source.demo.local-choice"),
        ),
    ),
    editorialConclusion = "UNRESOLVED. This pack establishes only the questions, competing operational paths, and review work. It does not establish a religious conclusion about electricity or lighting.",
    conclusionStatus = ConclusionStatus.UNRESOLVED,
    reviewState = EditorialReviewState.DRAFTED,
    sourceIds = listOf("source.demo.dossier-method", "source.demo.local-choice"),
    reviewRequirement = "Before stable release, add verified factual models, exact licensed source units, competing interpretations, and named human approval bound to the content digest.",
    adoptionOptions = listOf(
        DossierAdoptionOption(
            id = "adoption.lighting.prepared-precaution",
            title = "Prepared-lighting precaution",
            practice = "Prepare required lighting and accessibility devices before the gathering; avoid planned interactive changes during the service; stop for safety or emergency needs.",
            scopeNote = "Temporary local operating policy pending source review. It does not change the dossier's UNRESOLVED status.",
        ),
        DossierAdoptionOption(
            id = "adoption.lighting.device-register",
            title = "Device-by-device register",
            practice = "Record each light, switch, sensor, timer, and access device; make no blanket adoption until each intended action has factual and source review.",
            scopeNote = "Local research and preparation policy. It does not establish a permissive or prohibitive result.",
        ),
    ),
)
