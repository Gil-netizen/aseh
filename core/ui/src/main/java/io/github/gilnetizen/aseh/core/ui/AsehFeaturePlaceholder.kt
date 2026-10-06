package io.github.gilnetizen.aseh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * A deliberately non-normative feature placeholder used by the walking shell.
 *
 * Vertical scrolling keeps every string reachable at 200% font scale. The
 * fixture card exercises mixed-script shaping without introducing religious or
 * editorial content.
 */
@Composable
fun AsehFeaturePlaceholder(
    title: String,
    placeholderText: String,
    mixedScriptFixture: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 24.dp,
                    top = 32.dp,
                    end = 24.dp,
                    bottom = 32.dp,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodyLarge,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            val paragraphDirection = when (LocalLayoutDirection.current) {
                LayoutDirection.Rtl -> TextDirection.ContentOrRtl
                LayoutDirection.Ltr -> TextDirection.ContentOrLtr
            }
            Text(
                text = mixedScriptFixture,
                style = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(textDirection = paragraphDirection),
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
