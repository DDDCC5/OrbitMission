package com.example.orbitmission.ui.mission

import androidx.lifecycle.ViewModel
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.GravitySimulationUseCase
import com.example.orbitmission.domain.usecase.MissionSessionUseCase
import com.example.orbitmission.domain.usecase.CelestialFocusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/** Main-thread mission state machine; sensing/Entity updates remain in the scene owner. */
class MissionViewModel(private val session:MissionSessionUseCase):ViewModel() {
    private val mutableState=MutableStateFlow(MissionUiState(preferences=session.preferences()))
    val uiState=mutableState.asStateFlow()
    private var simulation:GravitySimulationUseCase?=null
    private var placement:Vector3D?=null
    private var placementDirection:Vector3D?=null
    private var phaseClock=0.0
    private var uiClock=0.0
    private var sceneTransitionClock=0.0
    private var manualThrust=false
    private var controllerThrust=false
    var currentFlight:FlightSnapshot?=null;private set
    val currentState get()=mutableState.value
    fun onEvent(event:MissionEvent){when(event){
        is MissionEvent.SelectPlanet->selectPlanet(event.planet)
        is MissionEvent.SetLanguage->setLanguage(event.language)
        is MissionEvent.SetAudio->setAudio(event.index,event.enabled)
        is MissionEvent.SetThrust->setThrust(event.enabled)
        MissionEvent.Launch->launch()
        MissionEvent.Retry->retry()
        MissionEvent.ChooseOtherPlanet->chooseOtherPlanet()
        MissionEvent.ToggleSettings->toggleSettings()
        MissionEvent.FinishTutorial->finishTutorial()
    }}
    fun text(key:String)=session.text(currentState.language,key)
    fun loading(count:Int){mutableState.value=currentState.copy(loadedCount=count)}
    fun assetsReady(){phaseClock=0.0;mutableState.value=currentState.copy(phase=MissionPhase.SPLASH,phaseSeconds=0.0,loadedCount=10)}
    fun failLoading(message:String){mutableState.value=currentState.copy(phase=MissionPhase.ERROR,error=message)}
    fun tick(dt:Double) {
        if(!dt.isFinite()||dt<=0||currentState.paused||currentState.settingsOpen)return
        phaseClock+=dt;uiClock+=dt
        if(currentState.sceneTransitioning){
            sceneTransitionClock+=dt
            if(sceneTransitionClock>=CelestialFocusUseCase.DURATION_SECONDS)mutableState.value=currentState.copy(sceneTransitioning=false)
        }
        when(currentState.phase) {
            MissionPhase.SPLASH->if(phaseClock>=5.0)transition(MissionPhase.ASSEMBLING)
            MissionPhase.ASSEMBLING->if(phaseClock>=5.5)transition(if(currentState.preferences.tutorialSeen)MissionPhase.SELECTING else MissionPhase.TUTORIAL)
            MissionPhase.FLYING,MissionPhase.SUCCESS->{
                currentFlight=simulation?.step(dt,currentState.thrust)
                val next=when(currentFlight?.status) {
                    FlightStatus.SUCCESS->MissionPhase.SUCCESS
                    FlightStatus.COLLISION,FlightStatus.ESCAPED,FlightStatus.TIMEOUT->MissionPhase.FAILURE
                    else->currentState.phase
                }
                if(next!=currentState.phase)transition(next)
            }
            else->Unit
        }
        if(uiClock>=.1 || currentState.flight!=currentFlight && currentState.phase==MissionPhase.FAILURE){
            mutableState.value=currentState.copy(phaseSeconds=phaseClock,flight=currentFlight);uiClock=0.0
        }
    }
    private fun transition(phase:MissionPhase){manualThrust=false;controllerThrust=false;phaseClock=0.0;mutableState.value=currentState.copy(phase=phase,phaseSeconds=0.0,flight=currentFlight,thrust=false)}
    fun finishTutorial(){save(currentState.preferences.copy(tutorialSeen=true));transition(MissionPhase.SELECTING)}
    fun selectPlanet(planet:Planet){
        if(currentState.phase!=MissionPhase.SELECTING || currentState.sceneTransitioning)return
        simulation=GravitySimulationUseCase(planet);placement=simulation!!.recommendedPosition();placementDirection=null
        currentFlight=simulation!!.snapshot();phaseClock=0.0
        sceneTransitionClock=0.0
        mutableState.value=currentState.copy(phase=MissionPhase.PLACEMENT,selectedPlanet=planet,hoveredPlanet=null,gazeProgress=0f,flight=currentFlight,phaseSeconds=0.0,sceneTransitioning=true)
    }
    fun setHover(planet:Planet?,progress:Float){if(currentState.phase==MissionPhase.SELECTING)mutableState.value=currentState.copy(hoveredPlanet=planet,gazeProgress=progress.coerceIn(0f,1f))}
    fun setPlacement(position:Vector3D,direction:Vector3D?=null){
        if(currentState.phase!=MissionPhase.PLACEMENT || currentState.sceneTransitioning || !listOf(position.x,position.y,position.z).all {it.isFinite()})return
        placement=position;placementDirection=direction?.takeIf {it.length()>0.0}
    }
    fun setGrabbing(grabbing:Boolean){mutableState.value=currentState.copy(grabbing=grabbing)}
    fun placementPosition()=placement
    fun launch(){
        if(currentState.phase!=MissionPhase.PLACEMENT || currentState.sceneTransitioning)return
        val sim=simulation?:return;val pos=placement?:sim.recommendedPosition()
        val circular=sim.circularVelocity(pos);val velocity=placementDirection?.normalized()?.times(circular.length())?:circular
        currentFlight=sim.launch(pos,velocity);transition(if(currentFlight!!.status==FlightStatus.FLYING)MissionPhase.FLYING else MissionPhase.FAILURE)
        mutableState.value=currentState.copy(grabbing=false)
    }
    fun retry(){if(currentState.sceneTransitioning)return;val planet=currentState.selectedPlanet?:return;simulation=GravitySimulationUseCase(planet);placement=simulation!!.recommendedPosition();placementDirection=null;currentFlight=simulation!!.snapshot();transition(MissionPhase.PLACEMENT)}
    fun chooseOtherPlanet(){val returning=currentState.selectedPlanet!=null;simulation=null;placement=null;placementDirection=null;currentFlight=null;phaseClock=0.0;sceneTransitionClock=0.0;manualThrust=false;controllerThrust=false;mutableState.value=currentState.copy(phase=MissionPhase.SELECTING,selectedPlanet=null,hoveredPlanet=null,gazeProgress=0f,flight=null,grabbing=false,thrust=false,phaseSeconds=0.0,sceneTransitioning=returning)}
    fun setThrust(enabled:Boolean){manualThrust=enabled;updateThrust()}
    fun setControllerThrust(enabled:Boolean){controllerThrust=enabled;updateThrust()}
    private fun updateThrust(){val active=manualThrust||controllerThrust;if(currentState.thrust!=active)mutableState.value=currentState.copy(thrust=active)}
    fun setActive(active:Boolean){manualThrust=false;controllerThrust=false;mutableState.value=currentState.copy(paused=!active,thrust=false)}
    fun toggleSettings(){manualThrust=false;controllerThrust=false;mutableState.value=currentState.copy(settingsOpen=!currentState.settingsOpen,thrust=false)}
    fun setLanguage(language:AppLanguage)=save(currentState.preferences.copy(language=language))
    fun setAudio(index:Int,enabled:Boolean)=save(when(index){0->currentState.preferences.copy(music=enabled);1->currentState.preferences.copy(voice=enabled);else->currentState.preferences.copy(sfx=enabled)})
    private fun save(preferences:MissionPreferences){session.save(preferences);mutableState.value=currentState.copy(preferences=preferences)}
}
