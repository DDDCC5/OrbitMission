package com.example.orbitmission.ui.mission.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitmission.R
import com.example.orbitmission.ui.theme.OrbitColors
import com.pico.spatial.ui.design.LocalDisableAlpha
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import java.util.Locale
import kotlin.math.min

/** User-approved custom angular silhouettes replace the standard rounded controls. */
private object AerospaceShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(bevelPath(size, with(density) { 18.dp.toPx() }))
}

private fun bevelPath(size: Size, bevel: Float): Path {
    val cut = min(bevel, min(size.width, size.height) / 3f)
    return Path().apply {
        moveTo(cut, 0f)
        lineTo(size.width - cut * 1.6f, 0f)
        lineTo(size.width, cut * 1.6f)
        lineTo(size.width, size.height - cut)
        lineTo(size.width - cut, size.height)
        lineTo(cut * 1.6f, size.height)
        lineTo(0f, size.height - cut * 1.6f)
        lineTo(0f, cut)
        close()
    }
}

/** Approved frame contour and instrument ticks are part of this bespoke visual design. */
private fun Modifier.aerospaceFrame(accent: Color = OrbitColors.cyan, selected: Boolean = false): Modifier =
    drawBehind {
        val path = bevelPath(size, 18.dp.toPx())
        drawPath(path, if (selected) Brush.verticalGradient(listOf(OrbitColors.selected, OrbitColors.panelBottom))
            else Brush.verticalGradient(listOf(OrbitColors.panelTop, OrbitColors.panelBottom)))
        drawPath(path, accent.copy(alpha = .72f), style = Stroke(1.dp.toPx()))
        val inset = 8.dp.toPx()
        val length = min(56.dp.toPx(), size.width * .2f)
        val stroke = 2.5.dp.toPx()
        drawLine(accent, Offset(26.dp.toPx(), inset), Offset(26.dp.toPx() + length, inset), stroke)
        drawLine(accent, Offset(size.width - 24.dp.toPx() - length, size.height - inset),
            Offset(size.width - 24.dp.toPx(), size.height - inset), stroke)
        if (size.height > 100.dp.toPx()) {
            drawLine(accent, Offset(size.width - inset, 38.dp.toPx()), Offset(size.width - inset, 82.dp.toPx()), stroke)
        }
    }

@Composable
fun MissionLogo(modifier: Modifier = Modifier) {
    // Keep identity tied to the system locale, independently of saved mission-language preferences.
    Row(modifier = modifier.defaultMinSize(minWidth = 340.dp).height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        Image(bitmap = ImageBitmap.imageResource(R.drawable.orbit_brand_mark),
            contentDescription = null, modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit)
        Text(stringResource(R.string.app_name), color = OrbitColors.foreground,
            style = PicoTheme.typography.titleLarge.copy(fontSize = 40.sp), maxLines = 1)
    }
}

@Composable
fun AerospacePanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.clip(AerospaceShape).aerospaceFrame().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (title.isNotBlank()) {
            Text(title, color = OrbitColors.cyan,
                style = PicoTheme.typography.titleLarge.copy(fontSize = 26.sp))
        }
        content()
    }
}

@Composable
fun MissionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val accent = if (selected) OrbitColors.amber else OrbitColors.cyan
    Box(
        modifier = modifier.defaultMinSize(minWidth = 180.dp, minHeight = 58.dp)
            .alpha(if (enabled) 1f else LocalDisableAlpha.current)
            .clip(AerospaceShape)
            .spatialHoverEffect(enabled = enabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .aerospaceFrame(accent, selected)
            .padding(horizontal = 28.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = accent, style = PicoTheme.typography.labelLarge.copy(fontSize = 20.sp),
            textAlign = TextAlign.Center)
    }
}

@Composable
fun TelemetryPanel(
    labels: List<String>,
    values: List<String>,
    remainingSeconds: Double,
    progress: Double,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    AerospacePanel(title, modifier) {
        labels.forEachIndexed { index, label ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = OrbitColors.muted,
                    style = PicoTheme.typography.bodyMedium.copy(fontSize = 18.sp))
                Text(values.getOrElse(index) { "—" }, color = OrbitColors.foreground,
                    style = PicoTheme.typography.headlineMedium.copy(fontSize = 30.sp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(String.format(Locale.ROOT, "%.1f s", remainingSeconds.coerceAtLeast(0.0)),
                color = if (remainingSeconds < 15.0) OrbitColors.amber else OrbitColors.cyan,
                style = PicoTheme.typography.titleMedium.copy(fontSize = 24.sp))
            Text(String.format(Locale.ROOT, "%.0f%%", progress.coerceIn(0.0, 1.0) * 100),
                color = OrbitColors.cyan, style = PicoTheme.typography.bodyMedium.copy(fontSize = 20.sp))
        }
        Box(Modifier.fillMaxWidth().height(5.dp).drawBehind {
            drawRect(OrbitColors.muted.copy(alpha = .25f))
            drawRect(OrbitColors.cyan, size = Size(size.width * progress.coerceIn(0.0, 1.0).toFloat(), size.height))
        })
    }
}

@Composable
fun SettingsPanel(
    languageLabels: List<String>,
    selectedLanguage: Int,
    onLanguage: (Int) -> Unit,
    audioLabels: List<String>,
    audioEnabled: List<Boolean>,
    onAudio: (Int, Boolean) -> Unit,
    onClose: () -> Unit,
    closeLabel: String,
    modifier: Modifier = Modifier,
    title: String = "",
    languageTitle: String = "",
    onLabel: String = "ON",
    offLabel: String = "OFF",
) {
    AerospacePanel(title, modifier) {
        if (languageTitle.isNotBlank()) Text(languageTitle, color = OrbitColors.muted,
            style = PicoTheme.typography.bodyLarge.copy(fontSize = 18.sp))
        languageLabels.forEachIndexed { index, label ->
            MissionButton(label, { onLanguage(index) }, Modifier.fillMaxWidth(), selected = selectedLanguage == index)
        }
        Spacer(Modifier.height(4.dp))
        audioLabels.forEachIndexed { index, label ->
            val checked = audioEnabled.getOrElse(index) { false }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f), color = OrbitColors.foreground,
                    style = PicoTheme.typography.bodyLarge.copy(fontSize = 20.sp))
                MissionButton(if (checked) onLabel else offLabel, { onAudio(index, !checked) },
                    Modifier.width(125.dp), selected = checked)
            }
        }
        MissionButton(closeLabel, onClose, Modifier.fillMaxWidth())
    }
}

@Composable
fun BriefingPanel(
    title: String,
    steps: List<String>,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AerospacePanel(title, modifier) {
        steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(String.format(Locale.ROOT, "%02d", index + 1), color = OrbitColors.cyan,
                    style = PicoTheme.typography.titleMedium.copy(fontSize = 22.sp))
                Text(step, color = OrbitColors.foreground,
                    style = PicoTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp))
            }
        }
        MissionButton(continueLabel, onContinue, Modifier.fillMaxWidth())
    }
}

@Composable
fun ResultPanel(
    title: String,
    detail: String,
    retryLabel: String,
    otherLabel: String,
    onRetry: () -> Unit,
    onOther: () -> Unit,
    modifier: Modifier = Modifier,
    success: Boolean = false,
) {
    AerospacePanel("", modifier) {
        Text(title, color = if (success) OrbitColors.cyan else OrbitColors.amber,
            style = PicoTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Medium))
        Text(detail, color = OrbitColors.foreground,
            style = PicoTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp))
        Spacer(Modifier.height(6.dp))
        MissionButton(retryLabel, onRetry, Modifier.fillMaxWidth())
        MissionButton(otherLabel, onOther, Modifier.fillMaxWidth())
    }
}

@Composable
fun InstructionPanel(text: String, secondary: String = "", modifier: Modifier = Modifier) {
    AerospacePanel("", modifier) {
        Text(text, modifier = Modifier.fillMaxWidth(), color = OrbitColors.foreground,
            style = PicoTheme.typography.titleLarge.copy(fontSize = 25.sp, lineHeight = 34.sp), textAlign = TextAlign.Center)
        if (secondary.isNotBlank()) {
            Text(secondary, modifier = Modifier.fillMaxWidth(), color = OrbitColors.muted,
                style = PicoTheme.typography.bodyLarge.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
        }
    }
}
