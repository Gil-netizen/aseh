package io.github.gilnetizen.aseh.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.gilnetizen.aseh.core.ui.AsehFeaturePlaceholder

@Composable
fun PracticeScreen(modifier: Modifier = Modifier) {
    AsehFeaturePlaceholder(
        title = stringResource(R.string.feature_practice_title),
        placeholderText = stringResource(R.string.feature_practice_placeholder),
        mixedScriptFixture = stringResource(R.string.feature_practice_mixed_script_fixture),
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
private fun PracticeEnglishPreview() {
    PracticeScreen()
}

@Preview(
    name = "Hebrew RTL tablet fixture",
    locale = "he",
    widthDp = 800,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun PracticeHebrewPreview() {
    PracticeScreen()
}
