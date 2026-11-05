package com.example.orbitmission.ui.mission

import com.example.orbitmission.domain.model.*

data class MissionUiState(val phase:MissionPhase=MissionPhase.LOADING,val phaseSeconds:Double=0.0,
    val preferences:MissionPreferences=MissionPreferences(),val selectedPlanet:Planet?=null,
    val hoveredPlanet:Planet?=null,val gazeProgress:Float=0f,val flight:FlightSnapshot?=null,
    val settingsOpen:Boolean=false,val paused:Boolean=false,val grabbing:Boolean=false,
    val error:String?=null,val loadedCount:Int=0,val thrust:Boolean=false,val sceneTransitioning:Boolean=false) {
    val language get()=preferences.language
}

sealed interface MissionEvent {
    data class SelectPlanet(val planet:Planet):MissionEvent
    data class SetLanguage(val language:AppLanguage):MissionEvent
    data class SetAudio(val index:Int,val enabled:Boolean):MissionEvent
    data class SetThrust(val enabled:Boolean):MissionEvent
    data object Launch:MissionEvent
    data object Retry:MissionEvent
    data object ChooseOtherPlanet:MissionEvent
    data object ToggleSettings:MissionEvent
    data object FinishTutorial:MissionEvent
}
