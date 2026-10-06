package io.github.gilnetizen.aseh.feature.study

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.gilnetizen.aseh.core.ui.AsehFeaturePlaceholder

@Composable
fun StudyScreen(modifier: Modifier = Modifier) {
    AsehFeaturePlaceholder(
        title = stringResource(R.string.feature_study_title),
        placeholderText = stringResource(R.string.feature_study_placeholder),
        mixedScriptFixture = stringResource(R.string.feature_study_mixed_script_fixture),
        modifier = modifier,
    )
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
private fun StudyEnglishPreview() {
    StudyScreen()
}

@Preview(
    name = "Hebrew RTL tablet fixture",
    locale = "he",
    widthDp = 800,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun StudyHebrewPreview() {
    StudyScreen()
}
