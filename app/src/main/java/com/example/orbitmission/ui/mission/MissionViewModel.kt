package com.example.orbitmission.ui.mission

import androidx.lifecycle.ViewModel
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.GravitySimulationUseCase
import com.example.orbitmission.domain.usecase.MissionSessionUseCase
import com.example.orbitmission.domain.usecase.CelestialFocusUseCase
import com.example.orbitmission.domain.usecase.OrbitPlanningUseCase
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
    private var manualThrust=0.0
    private var pulseRemaining=0.0
    private var predictionClock=0.0
    private var predictionDirty=true
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
        phaseClock+=dt;uiClock+=dt;predictionClock+=dt
        if(pulseRemaining>0){pulseRemaining-=dt;if(pulseRemaining<=0){manualThrust=0.0;updateThrust()}}
        if(currentState.sceneTransitioning){
            sceneTransitionClock+=dt
            if(sceneTransitionClock>=CelestialFocusUseCase.DURATION_SECONDS)mutableState.value=currentState.copy(sceneTransitioning=false)
        }
        when(currentState.phase) {
            MissionPhase.SPLASH->if(phaseClock>=5.0)transition(MissionPhase.ASSEMBLING)
            MissionPhase.ASSEMBLING->if(phaseClock>=5.5)transition(if(currentState.preferences.tutorialSeen)MissionPhase.SELECTING else MissionPhase.TUTORIAL)
            MissionPhase.FLYING,MissionPhase.SUCCESS->{
                currentFlight=simulation?.step(dt,currentState.thrustCommand)
                val next=when(currentFlight?.status) {
                    FlightStatus.SUCCESS->MissionPhase.SUCCESS
                    FlightStatus.COLLISION,FlightStatus.ESCAPED,FlightStatus.TIMEOUT->MissionPhase.FAILURE
                    else->currentState.phase
                }
                if(next!=currentState.phase)transition(next)
            }
            else->Unit
        }
        if(predictionClock>=.25 && (predictionDirty || currentState.phase in listOf(MissionPhase.FLYING,MissionPhase.SUCCESS))) refreshPrediction()
        if(uiClock>=.1 || currentState.flight!=currentFlight && currentState.phase==MissionPhase.FAILURE){
            mutableState.value=currentState.copy(phaseSeconds=phaseClock,flight=currentFlight);uiClock=0.0
        }
    }
    private fun transition(phase:MissionPhase){manualThrust=0.0;pulseRemaining=0.0;controllerThrust=false;phaseClock=0.0;mutableState.value=currentState.copy(phase=phase,phaseSeconds=0.0,flight=currentFlight,thrust=false,thrustCommand=0.0)}
    fun finishTutorial(){save(currentState.preferences.copy(tutorialSeen=true));transition(MissionPhase.SELECTING)}
    fun selectPlanet(planet:Planet){
        if(currentState.phase!=MissionPhase.SELECTING || currentState.sceneTransitioning)return
        simulation=GravitySimulationUseCase(planet);placement=simulation!!.recommendedPosition();placementDirection=null
        currentFlight=simulation!!.snapshot();phaseClock=0.0
        sceneTransitionClock=0.0
        mutableState.value=currentState.copy(phase=MissionPhase.PLACEMENT,selectedPlanet=planet,hoveredPlanet=null,gazeProgress=0f,flight=currentFlight,phaseSeconds=0.0,sceneTransitioning=true,
            placementConfirmed=false,velocityAdjusted=false,aiming=false,correctionUsed=false,
            launchSpeedRatio=if(currentState.mode==MissionMode.GUIDED).88 else .70)
        refreshPrediction()
    }
    fun setHover(planet:Planet?,progress:Float){if(currentState.phase==MissionPhase.SELECTING)mutableState.value=currentState.copy(hoveredPlanet=planet,gazeProgress=progress.coerceIn(0f,1f))}
    private fun editable()=currentState.phase==MissionPhase.PLACEMENT && !currentState.sceneTransitioning && !currentState.paused && !currentState.settingsOpen
    fun setPlacement(position:Vector3D,direction:Vector3D?=null){
        if(!editable() || !listOf(position.x,position.y,position.z).all {it.isFinite()})return
        placement=position
        direction?.takeIf {it.length().isFinite() && it.length()>0.0}?.let {placementDirection=it}
        requestPrediction()
    }
    fun setGrabbing(grabbing:Boolean){mutableState.value=currentState.copy(grabbing=grabbing)}
    fun setAiming(aiming:Boolean){mutableState.value=currentState.copy(aiming=aiming);if(!aiming && predictionDirty)requestPrediction()}
    fun confirmPlacement(){if(editable()){mutableState.value=currentState.copy(placementConfirmed=true,grabbing=false);requestPrediction()}}
    fun placementPosition()=placement
    fun launchVelocity():Vector3D {
        val sim=simulation?:return Vector3D(0.0,0.0,0.0)
        val pos=placement?:sim.recommendedPosition();val circular=sim.circularVelocity(pos)
        return (placementDirection?.normalized()?:circular.normalized())*(circular.length()*currentState.launchSpeedRatio)
    }
    fun setLaunchVelocity(velocity:Vector3D){
        if(!editable() || !listOf(velocity.x,velocity.y,velocity.z).all {it.isFinite()} || velocity.length()<1e-8)return
        val sim=simulation?:return;val speed=sim.circularVelocity(placement?:sim.recommendedPosition()).length()
        if(speed<=0)return
        placementDirection=velocity.normalized()
        mutableState.value=currentState.copy(launchSpeedRatio=(velocity.length()/speed).coerceIn(.35,1.6),velocityAdjusted=true)
        requestPrediction()
    }
    fun adjustLaunchSpeed(delta:Double){
        if(!editable() || !delta.isFinite())return
        mutableState.value=currentState.copy(launchSpeedRatio=(currentState.launchSpeedRatio+delta).coerceIn(.35,1.6),velocityAdjusted=true)
        requestPrediction()
    }
    fun rotateLaunch(degrees:Double){
        if(!editable() || !degrees.isFinite())return
        val v=launchVelocity();val angle=degrees*PI/180
        setLaunchVelocity(Vector3D(v.x*cos(angle)-v.y*sin(angle),v.x*sin(angle)+v.y*cos(angle),v.z));requestPrediction()
    }
    fun setMode(mode:MissionMode){
        if(!editable() || mode==currentState.mode)return
        placementDirection=null;placement=simulation?.recommendedPosition()
        mutableState.value=currentState.copy(mode=mode,launchSpeedRatio=if(mode==MissionMode.GUIDED).88 else .70,
            placementConfirmed=false,velocityAdjusted=false,aiming=false,grabbing=false,correctionUsed=false)
        requestPrediction()
    }
    private fun requestPrediction(){
        predictionDirty=true
        mutableState.value=currentState.copy(predictionPending=true)
        if(predictionClock>=.25)refreshPrediction()
    }
    private fun refreshPrediction(){
        val planet=currentState.selectedPlanet?:return
        val point=if(currentState.phase==MissionPhase.PLACEMENT)placement else currentFlight?.position
        val velocity=if(currentState.phase==MissionPhase.PLACEMENT)launchVelocity() else currentFlight?.velocity
        if(point!=null && velocity!=null && currentState.phase in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS)) {
            if(currentState.phase==MissionPhase.PLACEMENT)
                currentFlight=GravitySimulationUseCase(planet).launch(point,velocity).copy(status=FlightStatus.READY)
            mutableState.value=currentState.copy(prediction=OrbitPlanningUseCase.predict(planet,point,velocity),predictionPending=false,flight=currentFlight)
        }
        predictionClock=0.0;predictionDirty=false
    }
    fun launch(){
        if(!currentState.canLaunch)return
        val sim=simulation?:return;val pos=placement?:sim.recommendedPosition()
        currentFlight=sim.launch(pos,launchVelocity());transition(if(currentFlight!!.status==FlightStatus.FLYING)MissionPhase.FLYING else MissionPhase.FAILURE)
        mutableState.value=currentState.copy(grabbing=false,aiming=false);requestPrediction()
    }
    fun retry(){
        if(currentState.sceneTransitioning)return
        val planet=currentState.selectedPlanet?:return;simulation=GravitySimulationUseCase(planet)
        if(placement==null)placement=simulation!!.recommendedPosition()
        currentFlight=simulation!!.snapshot();transition(MissionPhase.PLACEMENT)
        mutableState.value=currentState.copy(grabbing=false,aiming=false,placementConfirmed=true,velocityAdjusted=false,correctionUsed=false)
        requestPrediction()
    }
    fun chooseOtherPlanet(){
        val returning=currentState.selectedPlanet!=null;simulation=null;placement=null;placementDirection=null;currentFlight=null;phaseClock=0.0;sceneTransitionClock=0.0
        manualThrust=0.0;pulseRemaining=0.0;controllerThrust=false
        mutableState.value=currentState.copy(phase=MissionPhase.SELECTING,selectedPlanet=null,hoveredPlanet=null,gazeProgress=0f,flight=null,
            grabbing=false,aiming=false,thrust=false,thrustCommand=0.0,phaseSeconds=0.0,sceneTransitioning=returning,prediction=null,predictionPending=false,
            placementConfirmed=false,velocityAdjusted=false,correctionUsed=false)
    }
    fun setThrust(enabled:Boolean){manualThrust=if(enabled)1.0 else 0.0;pulseRemaining=if(enabled).35 else 0.0;updateThrust()}
    fun pulseThrust(direction:Int){
        if(currentState.phase !in listOf(MissionPhase.FLYING,MissionPhase.SUCCESS) || currentState.paused || currentState.settingsOpen)return
        val sign=if(direction<0)-1 else 1
        mutableState.value=currentState.copy(thrustDirection=sign)
        manualThrust=sign.toDouble();pulseRemaining=.35;updateThrust()
    }
    fun setControllerThrust(enabled:Boolean){controllerThrust=enabled;updateThrust()}
    private fun updateThrust(){
        val canBurn=currentState.phase in listOf(MissionPhase.FLYING,MissionPhase.SUCCESS) && !currentState.paused && !currentState.settingsOpen
        val active=if(!canBurn)0.0 else if(manualThrust!=0.0)manualThrust else if(controllerThrust)currentState.thrustDirection.toDouble() else 0.0
        if(currentState.thrustCommand!=active)mutableState.value=currentState.copy(thrust=active!=0.0,thrustCommand=active,
            correctionUsed=currentState.correctionUsed || active!=0.0)
    }
    fun setActive(active:Boolean){manualThrust=0.0;pulseRemaining=0.0;controllerThrust=false;mutableState.value=currentState.copy(paused=!active,thrust=false,thrustCommand=0.0,grabbing=false,aiming=false)}
    fun toggleSettings(){manualThrust=0.0;pulseRemaining=0.0;controllerThrust=false;mutableState.value=currentState.copy(settingsOpen=!currentState.settingsOpen,thrust=false,thrustCommand=0.0,grabbing=false,aiming=false)}
    fun setLanguage(language:AppLanguage)=save(currentState.preferences.copy(language=language))
    fun setAudio(index:Int,enabled:Boolean)=save(when(index){0->currentState.preferences.copy(music=enabled);1->currentState.preferences.copy(voice=enabled);else->currentState.preferences.copy(sfx=enabled)})
    private fun save(preferences:MissionPreferences){session.save(preferences);mutableState.value=currentState.copy(preferences=preferences)}
}
