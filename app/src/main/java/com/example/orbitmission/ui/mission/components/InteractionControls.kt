package com.example.orbitmission.ui.mission.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.ui.mission.MissionUiState
import com.example.orbitmission.ui.mission.MissionViewModel
import com.example.orbitmission.ui.theme.OrbitColors
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import java.util.Locale

@Composable
internal fun PredictionReadout(state:MissionUiState,vm:MissionViewModel) {
    val prediction=state.prediction?:return
    val key=when(prediction.quality){OrbitQuality.SAFE->"plan_safe";OrbitQuality.ADJUST->"plan_adjust";OrbitQuality.IMPACT->"plan_impact";OrbitQuality.ESCAPE->"plan_escape"}
    val color=when(prediction.quality){OrbitQuality.SAFE->OrbitColors.safe;OrbitQuality.ADJUST->OrbitColors.amber;else->OrbitColors.danger}
    Text(vm.text(if(state.predictionPending)"forecast_updating" else key),color=color,style=PicoTheme.typography.bodyLarge.copy(fontSize=19.sp))
    Text(vm.text("forecast_coast"),color=OrbitColors.muted,style=PicoTheme.typography.bodyLarge.copy(fontSize=16.sp))
}

@Composable
internal fun LaunchPlanningControls(state:MissionUiState,vm:MissionViewModel) {
    val enabled=!state.sceneTransitioning && !state.grabbing && !state.aiming
    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        MissionButton(vm.text("guided_mode"),{vm.setMode(MissionMode.GUIDED)},Modifier.weight(1f),enabled,state.mode==MissionMode.GUIDED)
        MissionButton(vm.text("challenge_mode"),{vm.setMode(MissionMode.CHALLENGE)},Modifier.weight(1f),enabled,state.mode==MissionMode.CHALLENGE)
    }
    if(!state.placementConfirmed){
        MissionButton(vm.text("confirm_position"),vm::confirmPlacement,Modifier.fillMaxWidth(),enabled)
    }else{
        Text(vm.text("launch_speed")+String.format(Locale.ROOT," %.0f%%",state.launchSpeedRatio*100),
            color=OrbitColors.foreground,style=PicoTheme.typography.bodyLarge.copy(fontSize=20.sp))
        val velocity=vm.launchVelocity()
        Text(vm.text("launch_angle")+String.format(Locale.ROOT," %+.0f°",kotlin.math.atan2(velocity.x,velocity.y)*180/kotlin.math.PI),
            color=OrbitColors.foreground,style=PicoTheme.typography.bodyLarge.copy(fontSize=20.sp))
        Text(vm.text(if(state.handTrackingAvailable)"hand_aim_help" else "hand_aim_fallback"),
            color=OrbitColors.cyan,style=PicoTheme.typography.bodyLarge.copy(fontSize=18.sp))
    }
    PredictionReadout(state,vm)
    MissionButton(vm.text("launch_confirm"),vm::launch,Modifier.fillMaxWidth(),state.canLaunch)
    MissionButton(vm.text("other_planet"),vm::chooseOtherPlanet,Modifier.fillMaxWidth(),enabled)
}

@Composable
internal fun FlightCorrectionControls(state:MissionUiState,vm:MissionViewModel) {
    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        MissionButton(vm.text("burn_up"),{vm.pulseThrust(1)},Modifier.weight(1f),selected=state.thrustCommand>0)
        MissionButton(vm.text("burn_down"),{vm.pulseThrust(-1)},Modifier.weight(1f),selected=state.thrustCommand<0)
    }
    Text(vm.text(if(state.thrustDirection>0)"trigger_up" else "trigger_down"),color=OrbitColors.muted,
        style=PicoTheme.typography.bodyLarge.copy(fontSize=18.sp))
    PredictionReadout(state,vm)
    MissionButton(vm.text("retry"),vm::retry,Modifier.fillMaxWidth())
    MissionButton(vm.text("other_planet"),vm::chooseOtherPlanet,Modifier.fillMaxWidth())
}
