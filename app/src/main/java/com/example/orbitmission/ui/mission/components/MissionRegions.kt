package com.example.orbitmission.ui.mission.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.orbitmission.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.spatial.scene.OrbitScene
import com.example.orbitmission.ui.mission.components.*
import com.example.orbitmission.ui.theme.OrbitColors
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.content.SpatialView
import kotlinx.coroutines.CancellationException
import java.util.Locale

import com.example.orbitmission.ui.mission.MissionUiState
import com.example.orbitmission.ui.mission.MissionViewModel

@Composable
internal fun MainPanel(state:MissionUiState,vm:MissionViewModel,onEnableEye:()->Unit) {
    val text:(String)->String=vm::text
    if(state.settingsOpen){Column {SettingsPanel(listOf("简体中文","繁體中文","English"),state.language.ordinal,
        {vm.setLanguage(AppLanguage.entries[it])},listOf(text("music"),text("voice"),text("sound")),
        listOf(state.preferences.music,state.preferences.voice,state.preferences.sfx),vm::setAudio,
        vm::toggleSettings,text("back"),Modifier.width(540.dp),title=text("settings"),languageTitle=text("language"),onLabel=text("on"),offLabel=text("off"));MissionButton(text("enable_eye"),onEnableEye,Modifier.width(540.dp))};return}
    when(state.phase){
        MissionPhase.LOADING->Column {MissionLogo(Modifier.width(620.dp));InstructionPanel("${stringResource(R.string.app_name)}  ${state.loadedCount}/10",modifier=Modifier.width(620.dp))}
        MissionPhase.SPLASH->MissionLogo(Modifier.width(620.dp).graphicsLayer {alpha=if(state.phaseSeconds<4)1f else (5-state.phaseSeconds).coerceIn(0.0,1.0).toFloat()})
        MissionPhase.TUTORIAL->BriefingPanel(text("tutorial"),listOf(text("tutorial_select"),text("tutorial_grab"),text("tutorial_thrust")),text("continue"),vm::finishTutorial,Modifier.width(760.dp))
        MissionPhase.FAILURE->ResultPanel(text("mission_failure"),text(when(state.flight?.status){FlightStatus.COLLISION->"collision";FlightStatus.ESCAPED->"escaped";else->"timeout"}),text("retry"),text("other_planet"),vm::retry,vm::chooseOtherPlanet,modifier=Modifier.width(640.dp))
        MissionPhase.ERROR->AerospacePanel(text("mission_failure"),Modifier.width(700.dp)){Text(state.error.orEmpty(),color=OrbitColors.foreground);MissionButton(text("retry"),{ /* close/relaunch preserves a clear error without constructing duplicate native resources */ vm.chooseOtherPlanet() },enabled=false)}
        else->Spacer(Modifier.size(1.dp))
    }
}
@Composable
internal fun FlightTelemetry(state:MissionUiState,vm:MissionViewModel) {
    val f=state.flight?:return
    if(state.phase !in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS))return
    fun format(n:Double,unit:String)=if(n.isFinite())String.format(Locale.ROOT,"%.3f %s",n,unit)else "—"
    TelemetryPanel(listOf("speed","radius","gravity","elapsed","eccentricity").map(vm::text),
        listOf(format(f.speedKmS,"km/s"),format(f.centerDistanceKm,"km"),format(f.forceNewtons,"N"),format(f.elapsedSeconds,"s"),format(f.eccentricity,"")),f.remainingSeconds,f.orbitProgressDegrees/360,
        Modifier.width(300.dp),title=vm.text(state.selectedPlanet?.id?:"app_name"))
}
@Composable
internal fun MissionControls(state:MissionUiState,vm:MissionViewModel) {
    if(state.phase in listOf(MissionPhase.LOADING,MissionPhase.SPLASH,MissionPhase.ASSEMBLING))return
    AerospacePanel("",Modifier.width(if(state.phase==MissionPhase.SELECTING)440.dp else 310.dp)) {
        MissionButton(vm.text("settings"),vm::toggleSettings,Modifier.fillMaxWidth())
        when(state.phase){
            MissionPhase.SELECTING->Planet.entries.chunked(2).forEach {pair->
                Row(horizontalArrangement=Arrangement.spacedBy(20.dp)){
                    pair.forEach {p->MissionButton(vm.text(p.id),{vm.selectPlanet(p)},Modifier.weight(1f),enabled=!state.sceneTransitioning,selected=state.hoveredPlanet==p)}
                }
            }
            MissionPhase.PLACEMENT->{MissionButton(vm.text("start"),vm::launch,Modifier.fillMaxWidth(),enabled=!state.sceneTransitioning);MissionButton(vm.text("other_planet"),vm::chooseOtherPlanet,Modifier.fillMaxWidth())}
            MissionPhase.FLYING,MissionPhase.SUCCESS->{
                MissionButton(vm.text("thrust"),{vm.setThrust(!state.thrust)},Modifier.fillMaxWidth(),selected=state.thrust)
                MissionButton(vm.text("retry"),vm::retry,Modifier.fillMaxWidth());MissionButton(vm.text("other_planet"),vm::chooseOtherPlanet,Modifier.fillMaxWidth())
            }
            else->Unit
        }
    }
}
@Composable
internal fun MissionPrompt(state:MissionUiState,vm:MissionViewModel) {
    val key=if(state.sceneTransitioning){if(state.selectedPlanet==null)"returning_to_overview" else "moving_to_focus"}else when(state.phase){MissionPhase.SELECTING->"select_planet";MissionPhase.PLACEMENT->"place_rocket";MissionPhase.FLYING->"stable";MissionPhase.SUCCESS->"orbit_success";else->return}
    val secondary=if(state.phase==MissionPhase.SELECTING && state.hoveredPlanet!=null)"${vm.text(state.hoveredPlanet.id)}  ${(state.gazeProgress*2).toString().take(3)} / 2 s · ${vm.text("overview_scale")}" else if(state.phase==MissionPhase.SELECTING)vm.text("overview_scale")else "${state.selectedPlanet?.let {vm.text(it.id)}.orEmpty()} · ${vm.text("task_scale")}"
    InstructionPanel(vm.text(key),secondary,Modifier.width(720.dp))
}
