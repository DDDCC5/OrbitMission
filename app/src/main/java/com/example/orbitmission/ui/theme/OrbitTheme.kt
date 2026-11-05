package com.example.orbitmission.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pico.spatial.ui.design.PicoTheme

/** Fixed colors from the accepted aerospace component board; native roles stay intact. */
object OrbitColors {
    val panelTop = Color(0xF0142B3D) // design-style: fixed-figma-color approved-components-board
    val panelBottom = Color(0xF006111F) // design-style: fixed-figma-color approved-components-board
    val cyan = Color(0xFF66DDEC) // design-style: fixed-figma-color approved-components-board
    val amber = Color(0xFFFFBF63) // design-style: fixed-figma-color approved-components-board
    val foreground = Color(0xFFE9F7FF) // design-style: fixed-figma-color approved-components-board
    val muted = Color(0xFF87A8BA) // design-style: fixed-figma-color approved-components-board
    val selected = Color(0xFF163544) // design-style: fixed-figma-color approved-components-board
}

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    PicoTheme(content = content)
}
