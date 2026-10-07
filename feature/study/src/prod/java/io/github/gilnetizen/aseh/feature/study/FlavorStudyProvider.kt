package io.github.gilnetizen.aseh.feature.study

import android.content.Context

/** No answer provider ships until a reviewed non-development provider is configured. */
internal fun flavorStudyProvider(): AskAsehProvider? = null

/** Synthetic pack assets, keys, and runtimes are absent from production. */
internal fun flavorDevelopmentContentPackRuntime(
    @Suppress("UNUSED_PARAMETER") context: Context,
): DevelopmentContentPackRuntime? = null
