package com.example.orbitmission.spatial.scene

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.OpacityControllerComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.resource.Resource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.ecs.resource.MaterialCullingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.InputArbitrationUseCase
import com.example.orbitmission.domain.usecase.OrbitAssistUseCase
import com.example.orbitmission.domain.usecase.GrabAction
import com.example.orbitmission.domain.usecase.CelestialFocusUseCase
import com.example.orbitmission.ui.mission.MissionViewModel
import com.example.orbitmission.spatial.input.*
import com.example.orbitmission.spatial.audio.MissionAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.Closeable
import kotlin.math.*

/** Identity owner is Stage space, right handed meters. Physics positions never enter ECS unscaled. */
class OrbitScene(private val context:Context,private val vm:MissionViewModel):Closeable {
    val placementOwner=Entity().apply {setName("OrbitMissionOwner")}
    val input=SpatialInputController(context)
    val audio=MissionAudio()
    private val bodies=mutableMapOf<Planet,Entity>()
    private val panels=mutableMapOf<String,Entity>()
    private val resources=mutableListOf<Resource>()
    private val muted=mutableMapOf<Planet,TextureResource>()
    private val materials=mutableMapOf<Planet,List<Pair<PhysicallyBasedMaterial,TextureResource>>>()
    private val saturation=mutableMapOf<Planet,Boolean>()
    private val focus=CelestialFocusUseCase()
    private val guide=mutableListOf<Entity>()
    private val trail=mutableListOf<Entity>()
    private val predicted=mutableListOf<Entity>()
    private val velocityArrow=mutableListOf<Entity>()
    private var velocityHandle:Entity?=null
    private var predictionMaterial:UnlitMaterial?=null
    private var lastPrediction:OrbitPrediction?=null
    private var aiming=false
    private var hoveringHandle=false
    private var sun:Entity?=null
    private var rocket:Entity?=null
    private var previousPhase:MissionPhase?=null
    private var previousLanguage:AppLanguage?=null
    private var previousPreferences:MissionPreferences?=null
    private val arbitration=InputArbitrationUseCase()
    private val orbitAssist=OrbitAssistUseCase()
    private var eyeTarget:Planet?=null
    private var hoverSeconds=0.0
    private var clock=0.0
    private var previousConfirm=false
    private var previousGrip=false
    private var grabbed=false
    private var grabDistance=3f
    private var warningClock=-100.0
    private var trailIndex=0
    private var trailClock=0.0
    private var suspended=false
    private var ready=false
    private var disposed=false
    private var center=Vector3(0f,1.55f,-2.1f)
    private val planetRadius get()=vm.currentState.selectedPlanet?.let(focus::radius)?:Planet.EARTH.displayRadiusMeters
    private val rocketScale get()=max(.0015f,planetRadius*.045f)
    private var flightMode=false
    private var lastHover:Planet?=null
    private var motionWasActive=false
    private var guideVisible=false
    private var guidePlanet:Planet?=null

    suspend fun initialize(addEntity:(Entity)->Unit) {
        addEntity(placementOwner)
        // Main-thread native construction is staged; decoding muted textures is the only IO work.
        environment()
        val s=Entity.loadSuspend("asset://models/sun.glb");s.setName("Sun");placementOwner.addChild(s);sun=s
        s.components.set(OpacityControllerComponent(0f));vm.loading(1)
        for((index,p) in Planet.entries.withIndex()) {
            val e=Entity.loadSuspend("asset://models/${p.id}.glb");e.setName(p.id);placementOwner.addChild(e)
            e.components.set(OpacityControllerComponent(0f))
            e.components.set(CollisionComponent(collisionShape=listOf(ShapeResource.createSphere(1f)),physicsMaterial=PhysicsMaterialResource()))
            e.components.set(InteractableComponent());bodies[p]=e
            val textured=descendants(e).flatMap {child->child.components[ModelComponent::class.java]?.materials?.toList().orEmpty()}
                .filterIsInstance<PhysicallyBasedMaterial>().filter {it.getName() != "SaturnRings"}
            // Material replacement drops the SDK reference count. Persist both variants
            // until scene disposal so subsequent hover/selection can bind them again.
            materials[p]=textured.map {material->material to material.getBaseColorTexture().also {it.toGlobal();resources.add(it)}}
            val bitmap=withContext(Dispatchers.IO){mutedBitmap(p)}
            muted[p]=TextureResource.create(bitmap).also {it.toGlobal();resources.add(it)};bitmap.recycle()
            setSaturated(p,false);vm.loading(index+2);yield()
        }
        rocket=Entity.loadSuspend("asset://models/crew_rocket.glb").also {
            it.setName("CrewRocket");placementOwner.addChild(it);it.enabled=false
            it.components.set(CollisionComponent(collisionShape=listOf(ShapeResource.createSphere(1f)),physicsMaterial=PhysicsMaterialResource()))
            it.components.set(InteractableComponent())
        }
        val cyan=UnlitMaterial.create().also {it.setBaseColor(Color4(.20f,.80f,.95f,1f));resources.add(it)}
        val gold=UnlitMaterial.create().also {it.setBaseColor(Color4(1f,.60f,.15f,1f));resources.add(it)}
        val point=MeshResource.createSphere(.007f).also {resources.add(it)}
        repeat(120){i->guide+=Entity().also {it.setName("OrbitGuide_$i");it.components.set(ModelComponent(point,if(i in 0..12)gold else cyan));placementOwner.addChild(it);it.enabled=false}}
        repeat(100){i->trail+=Entity().also {it.setName("FlightTrail_$i");it.components.set(ModelComponent(point,cyan));placementOwner.addChild(it);it.enabled=false}}
        val predictionInk=UnlitMaterial.create().also {it.setBaseColor(Color4(.4f,.95f,.65f,1f));resources.add(it)}
        predictionMaterial=predictionInk
        val forecastPoint=MeshResource.createSphere(.0035f).also {resources.add(it)}
        val arrowPoint=MeshResource.createSphere(.0025f).also {resources.add(it)}
        repeat(96){i->predicted+=Entity().also {it.setName("PredictedPath_$i");it.components.set(ModelComponent(forecastPoint,predictionInk));placementOwner.addChild(it);it.enabled=false}}
        repeat(14){i->velocityArrow+=Entity().also {it.setName("VelocityArrow_$i");it.components.set(ModelComponent(arrowPoint,gold));placementOwner.addChild(it);it.enabled=false}}
        val handleMesh=MeshResource.createSphere(.014f).also {resources.add(it)}
        velocityHandle=Entity().also {it.setName("VelocityHandle");it.components.set(ModelComponent(handleMesh,gold));placementOwner.addChild(it);it.enabled=false}
        audio.initialize(placementOwner)
        ready=true;input.start();vm.assetsReady()
        logBounds()
    }
    fun attachPanel(id:String,entity:Entity?) {
        if(entity==null)return
        placementOwner.addChild(entity);panels[id]=entity
        val location=when(id){"telemetry"->Vector3(-.62f,1.60f,-1.35f);"controls"->Vector3(.62f,1.60f,-1.35f);"prompt"->Vector3(0f,1.02f,-1.30f);else->Vector3(0f,1.62f,-1.15f)}
        transform(entity).setPosition(location)
        val scale=when(id){"telemetry"->1.30f;"controls"->1.20f;"prompt"->1.30f;else->1.20f}
        transform(entity).setScaleVector(Vector3(scale,scale,scale))
        // A transparent ECS attachment; no native panel background is painted over the scene.
    }
    private fun environment() {
        val skyTexture=TextureResource("environment/stars.png").also {resources.add(it)}
        val skyMaterial=UnlitMaterial.create().also {it.setBaseColorTexture(skyTexture);it.setCullingMode(MaterialCullingMode.FRONT);resources.add(it)}
        val skyMesh=MeshResource.createSphere(60f).also {resources.add(it)}
        Entity().apply {setName("DeepSpaceSky");components.set(ModelComponent(skyMesh,skyMaterial));placementOwner.addChild(this)}
        Entity().apply {setName("SolarKeyLight");components.set(DirectionalLightComponent(Color4(1f,.94f,.86f,1f),2200f));transform(this).setEulerAngles(EulerAngles(-25f,-35f,0f));placementOwner.addChild(this)}
        Entity().apply {setName("BlueFillLight");components.set(DirectionalLightComponent(Color4(.35f,.55f,1f,1f),500f));transform(this).setEulerAngles(EulerAngles(15f,140f,0f));placementOwner.addChild(this)}
    }
    private fun mutedBitmap(p:Planet):Bitmap {
        val raw=context.assets.open("textures/${p.id}.png").use {BitmapFactory.decodeStream(it)}?:error("Missing planet texture ${p.id}")
        val output=Bitmap.createBitmap(raw.width,raw.height,Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(raw,0f,0f,Paint().apply {colorFilter=ColorMatrixColorFilter(ColorMatrix().apply {setSaturation(.5f)})})
        raw.recycle();return output
    }
    private fun setSaturated(p:Planet,full:Boolean){
        if(saturation[p]==full)return
        val idle=muted[p]?:return
        materials[p].orEmpty().forEach {(material,original)->material.setBaseColorTexture(if(full)original else idle)}
        saturation[p]=full
    }
    fun tick(dt:Double) {
        if(!ready||disposed)return
        val state=vm.currentState
        panels["controls"]?.enabled=!state.settingsOpen && state.phase !in listOf(MissionPhase.SPLASH,MissionPhase.LOADING,MissionPhase.ASSEMBLING)
        panels["telemetry"]?.enabled=!state.settingsOpen && !state.sceneTransitioning && state.phase in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS)
        panels["prompt"]?.enabled=!state.settingsOpen
        if(state.paused||state.settingsOpen){if(!suspended){input.stop();audio.pause();grabbed=false;aiming=false;arbitration.reset();vm.setGrabbing(false);vm.setAiming(false);previousGrip=false;previousConfirm=false;suspended=true};return}
        if(suspended){input.start();audio.resume();suspended=false}
        clock+=dt
        vm.tick(dt)
        val now=vm.currentState
        val frame=input.sample()
        if(now.phase!=previousPhase || now.language!=previousLanguage){onPhase(now.phase);previousPhase=now.phase;previousLanguage=now.language}
        if(now.preferences!=previousPreferences){audio.setEnabled(now.preferences.music,now.preferences.voice,now.preferences.sfx);previousPreferences=now.preferences}
        updateBodies(dt)
        if(!now.sceneTransitioning)when(now.phase){MissionPhase.SELECTING->select(frame,dt);MissionPhase.PLACEMENT->place(frame);MissionPhase.FLYING,MissionPhase.SUCCESS->flight(frame,dt);else->Unit}
        if(now.phase !in listOf(MissionPhase.FLYING,MissionPhase.SUCCESS))audio.setEngine(false,center)
        updatePlanningAids()
        previousConfirm=frame.confirmPressed;previousGrip=frame.gripPressed
        panels["telemetry"]?.enabled=!now.sceneTransitioning && now.phase in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS)
        panels["controls"]?.enabled=now.phase !in listOf(MissionPhase.SPLASH,MissionPhase.LOADING,MissionPhase.ASSEMBLING)
    }
    private fun onPhase(phase:MissionPhase){
        val selected=vm.currentState.selectedPlanet
        if(phase==MissionPhase.SELECTING){arbitration.reset();grabbed=false;eyeTarget=null;hoverSeconds=0.0;lastHover=null;trail.forEach {it.enabled=false};flightMode=false;audio.playVoice(vm.currentState.language.tag,"select_planet")}
        if(phase==MissionPhase.PLACEMENT){grabbed=false;trail.forEach {it.enabled=false};trailIndex=0;audio.playVoice(vm.currentState.language.tag,"place_rocket")}
        if(phase==MissionPhase.SUCCESS){audio.playEffect("orbit_success");audio.playVoice(vm.currentState.language.tag,"orbit_success")}
        if(phase==MissionPhase.FAILURE){audio.playEffect(if(vm.currentFlight?.status==FlightStatus.COLLISION)"collision" else "mission_failure");audio.playVoice(vm.currentState.language.tag,"mission_failure");audio.setEngine(false,center)}
        Log.i("OrbitMission","phase=$phase planet=${selected?.id}")
    }
    private fun updateBodies(dt:Double){
        val state=vm.currentState
        val intro=state.phase==MissionPhase.ASSEMBLING
        val showing=state.phase !in listOf(MissionPhase.LOADING,MissionPhase.SPLASH,MissionPhase.ERROR)
        focus.select(state.selectedPlanet);focus.step(dt)
        state.selectedPlanet?.let {center=vector(focus.position(it))}
        sun?.let {e->e.enabled=showing;transform(e).setPosition(vector(focus.sunPosition));val radius=focus.sunRadius;transform(e).setScaleVector(Vector3(radius,radius,radius));opacity(e,if(!showing)0f else if(intro)min(1f,state.phaseSeconds.toFloat()*2)else 1f);transform(e).setEulerAngles(EulerAngles(0f,(clock*360*3600/2192832).toFloat(),0f))}
        for((i,p) in Planet.entries.withIndex()) {
            val e=bodies[p]?:continue
            val visible=showing && focus.opacity(p)>.001
            e.enabled=visible
            if(!visible)continue
            val isTarget=p==state.selectedPlanet
            val appearance=if(intro)((state.phaseSeconds-(i+1)*.5)/.55).coerceIn(0.0,1.0).toFloat()else 1f
            opacity(e,appearance*focus.opacity(p).toFloat())
            val scale=focus.radius(p)
            transform(e).setPosition(vector(focus.position(p)));transform(e).setScaleVector(Vector3(scale,scale,scale))
            val full=(p==state.hoveredPlanet)||isTarget
            setSaturated(p,full)
            if(full){val t=transform(e);val previous=t.eulerAngles;t.setEulerAngles(EulerAngles(previous.pitch,previous.yaw+(dt*360*3600/p.rotationPeriodSeconds).toFloat(),previous.roll))}
        }
        val needRocket=!state.sceneTransitioning && state.phase in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS)
        rocket?.enabled=needRocket
        val orbitRadius=planetRadius*1.5f
        val markerScale=max(.02f,planetRadius/.24f)
        if(needRocket && guidePlanet!=state.selectedPlanet){
            guide.forEachIndexed {i,e->val angle=i*2*PI/guide.size;transform(e).setScaleVector(Vector3(markerScale,markerScale,markerScale));transform(e).setPosition(Vector3(center.x+orbitRadius*cos(angle).toFloat(),center.y+orbitRadius*sin(angle).toFloat(),center.z))}
            guidePlanet=state.selectedPlanet
        }
        if(guideVisible!=needRocket){guide.forEach {it.enabled=needRocket};guideVisible=needRocket}
        if(focus.moving)motionWasActive=true
        else if(motionWasActive){
            Log.i("OrbitFocus","settled selected=${state.selectedPlanet?.id} sun=${sun?.getVisualBounds(placementOwner)}")
            bodies.forEach {(planet,entity)->Log.i("OrbitFocus","${planet.id}: ${entity.getVisualBounds(placementOwner)}")}
            motionWasActive=false
        }
    }
    private fun select(frame:InputFrame,dt:Double){
        val controllerTarget=frame.controller?.let {hitPlanet(it)}
        val gazeTarget=frame.gaze?.let {hitPlanet(it)}
        val result=arbitration.target(controllerTarget,gazeTarget,dt)
        val hovered=result.hovered
        if(hovered!=lastHover){lastHover=hovered;if(hovered!=null)audio.playEffect("button_click")}
        vm.setHover(hovered,result.progress)
        if(frame.confirmPressed&&!previousConfirm&&controllerTarget!=null)vm.selectPlanet(controllerTarget)
        else if(result.confirmed!=null){vm.selectPlanet(result.confirmed);arbitration.reset()}
    }
    private fun hitPlanet(ray:InputRay):Planet?=Planet.entries.mapNotNull {p->
        val e=bodies[p]?:return@mapNotNull null
        if(!e.enabled)return@mapNotNull null
        // Keep tiny terrestrial globes physically scaled, with independent usable hit areas.
        raySphere(ray,transform(e).position,max(.055f,p.displayRadiusMeters*1.1f))?.let {p to it}
    }.minByOrNull {it.second}?.first
    private fun velocityEndpoint():Vector3? {
        val planet=vm.currentState.selectedPlanet?:return null
        val physical=vm.placementPosition()?:return null
        val start=toDisplay(physical,planet);val v=vm.launchVelocity().normalized()
        val length=.22*vm.currentState.launchSpeedRatio
        return Vector3(start.x+(v.x*length).toFloat(),start.y+(v.y*length).toFloat(),start.z+(v.z*length).toFloat())
    }
    private fun updatePlanningAids(){
        val state=vm.currentState
        val placing=state.phase==MissionPhase.PLACEMENT && !state.sceneTransitioning && !state.settingsOpen
        val visible=!state.settingsOpen && !state.sceneTransitioning && state.phase in listOf(MissionPhase.PLACEMENT,MissionPhase.FLYING,MissionPhase.SUCCESS)
        velocityHandle?.enabled=placing;velocityArrow.forEach {it.enabled=placing}
        if(placing){
            val planet=state.selectedPlanet
            val physical=vm.placementPosition();val end=velocityEndpoint()
            if(planet!=null && physical!=null && end!=null){
                val start=toDisplay(physical,planet)
                velocityHandle?.let {transform(it).setPosition(end);val scale=if(aiming || hoveringHandle)1.35f else 1f;transform(it).setScaleVector(Vector3(scale,scale,scale))}
                velocityArrow.forEachIndexed {index,e->val t=index.toFloat()/velocityArrow.size;transform(e).setPosition(Vector3(start.x+(end.x-start.x)*t,start.y+(end.y-start.y)*t,start.z+(end.z-start.z)*t))}
            }
        }
        val forecast=state.prediction
        if(!visible || forecast==null){predicted.forEach {it.enabled=false};lastPrediction=null;return}
        if(forecast!==lastPrediction){
            val color=when(forecast.quality){OrbitQuality.SAFE->Color4(.4f,.95f,.65f,1f);OrbitQuality.ADJUST->Color4(1f,.65f,.2f,1f);else->Color4(1f,.25f,.25f,1f)}
            predictionMaterial?.setBaseColor(color)
            val planet=state.selectedPlanet?:return
            predicted.forEachIndexed {index,e->
                val point=forecast.points.getOrNull(index)
                e.enabled=point!=null && point.length()<=planet.radiusMeters*12
                if(e.enabled && point!=null)transform(e).setPosition(toDisplay(point,planet))
            }
            lastPrediction=forecast
        }
    }
    private fun place(frame:InputFrame){
        val r=rocket?:return;val selected=vm.currentState.selectedPlanet?:return;val ray=frame.controller
        if(aiming && !vm.currentState.aiming){aiming=false;arbitration.reset()}
        if(grabbed && !vm.currentState.grabbing){grabbed=false;arbitration.reset()}
        hoveringHandle=ray?.let {sample->velocityEndpoint()?.let {raySphere(sample,it,.045f)!=null}}?:false
        if(frame.gripPressed&&!previousGrip && ray!=null){
            val handleHit=velocityEndpoint()?.let {raySphere(ray,it,.045f)}
            val rocketHit=raySphere(ray,transform(r).position,max(.04f,planetRadius*.45f))
            if(handleHit!=null && (rocketHit==null || handleHit<rocketHit)){
                aiming=true;vm.setAiming(true);arbitration.beginGrab(frame.controllerHand?:"controller");audio.playEffect("button_click")
            }else if(rocketHit!=null){
                grabbed=true;grabDistance=rocketHit;vm.setGrabbing(true);arbitration.beginGrab(frame.controllerHand?:"controller");audio.playEffect("button_click")
            }
        }
        val action=arbitration.grab(frame.controllerHand,frame.gripPressed)
        if(action==GrabAction.CANCEL){grabbed=false;aiming=false;vm.setGrabbing(false);vm.setAiming(false)}
        if(aiming && action==GrabAction.MOVE && ray!=null){
            val position=vm.placementPosition()
            if(position!=null){
                val start=toDisplay(position,selected)
                fun physicalVector(v:Vector3)=Vector3D(v.x.toDouble(),v.y.toDouble(),v.z.toDouble())
                val circular=vm.launchVelocity().length()/vm.currentState.launchSpeedRatio
                com.example.orbitmission.domain.usecase.VelocityHandleUseCase.drag(
                    physicalVector(ray.origin),physicalVector(ray.direction),physicalVector(start),circular)?.let(vm::setLaunchVelocity)
            }
        }
        if(grabbed && action==GrabAction.MOVE && ray!=null){
            var point=Vector3(ray.origin.x+ray.direction.x*grabDistance,ray.origin.y+ray.direction.y*grabDistance,ray.origin.z+ray.direction.z*grabDistance)
            frame.gaze?.let {gaze->
                fun relative(v:Vector3)=Vector3D((v.x-center.x).toDouble(),(v.y-center.y).toDouble(),(v.z-center.z).toDouble())
                val d=gaze.direction
                val n=orbitAssist.snap(relative(point),relative(gaze.origin),Vector3D(d.x.toDouble(),d.y.toDouble(),d.z.toDouble()),planetRadius.toDouble()*1.5)
                point=Vector3(center.x+n.x.toFloat(),center.y+n.y.toFloat(),center.z+n.z.toFloat())
            }
            val ratio=selected.radiusMeters/planetRadius
            vm.setPlacement(Vector3D((point.x-center.x)*ratio,(point.y-center.y)*ratio,(point.z-center.z)*ratio))
        }
        if(action==GrabAction.RELEASE){
            if(grabbed){grabbed=false;vm.setGrabbing(false);vm.confirmPlacement()}
            if(aiming){aiming=false;vm.setAiming(false)}
            audio.playEffect("button_click")
        }
        val physical=vm.placementPosition()?:return
        transform(r).setPosition(toDisplay(physical,selected));transform(r).setScaleVector(Vector3(rocketScale,rocketScale,rocketScale))
        transform(r).setQuaternion(directionQuat(vm.launchVelocity()))
        if(frame.confirmPressed&&!previousConfirm)vm.launch()
    }
    private fun flight(frame:InputFrame,dt:Double){
        vm.setControllerThrust(frame.triggerPressed)
        val snapshot=vm.currentFlight?:return;val p=vm.currentState.selectedPlanet?:return
        val position=toDisplay(snapshot.position,p)
        rocket?.let {transform(it).setPosition(position);transform(it).setScaleVector(Vector3(rocketScale,rocketScale,rocketScale));transform(it).setQuaternion(directionQuat(snapshot.velocity))}
        audio.setEngine(vm.currentState.thrust,position)
        trailClock+=dt
        if(trailClock>.10){val e=trail[trailIndex%trail.size];e.enabled=true;val scale=max(.02f,planetRadius/.24f);transform(e).setScaleVector(Vector3(scale,scale,scale));transform(e).setPosition(position);trailIndex++;trailClock=0.0}
        val r=snapshot.position.length()
        if(r<1.15*p.radiusMeters && clock-warningClock>4){audio.playVoice(vm.currentState.language.tag,"collision_warning");warningClock=clock}
        val sunDist=sqrt((snapshot.position.x+p.sunDistanceMeters).pow(2)+snapshot.position.y.pow(2)+snapshot.position.z.pow(2))
        if(sunDist>4.515e12*1.1 && clock-warningClock>4){audio.playVoice(vm.currentState.language.tag,"boundary_warning");warningClock=clock}
    }
    private fun toDisplay(p:Vector3D,planet:Planet):Vector3 {val ratio=planetRadius/planet.radiusMeters;return Vector3(center.x+(p.x*ratio).toFloat(),center.y+(p.y*ratio).toFloat(),center.z+(p.z*ratio).toFloat())}
    private fun vector(v:Vector3D)=Vector3(v.x.toFloat(),v.y.toFloat(),v.z.toFloat())
    private fun directionQuat(v:Vector3D):Quat {val n=v.normalized();if(n.length()==0.0)return Quat();if(n.y<-.999999)return Quat(1f,0f,0f,0f);val w=sqrt((1+n.y)*2);return Quat((n.z/w).toFloat(),0f,(-n.x/w).toFloat(),(w/2).toFloat())}
    private fun raySphere(ray:InputRay,center:Vector3,radius:Float):Float? {
        val dx=ray.origin.x-center.x;val dy=ray.origin.y-center.y;val dz=ray.origin.z-center.z
        val b=dx*ray.direction.x+dy*ray.direction.y+dz*ray.direction.z;val c=dx*dx+dy*dy+dz*dz-radius*radius;val discriminant=b*b-c
        if(discriminant<0)return null;val t=-b-sqrt(discriminant);return t.takeIf {it>=0f}
    }
    private fun opacity(e:Entity,value:Float){e.components[OpacityControllerComponent::class.java]?.opacity=value}
    private fun transform(e:Entity)=e.components[TransformComponent::class.java]?:error("Missing transform")
    private fun descendants(root:Entity):List<Entity> = listOf(root)+root.getChildren().flatMap {descendants(it)}
    private fun logBounds(){(listOfNotNull(sun,rocket)+bodies.values).forEach {Log.i("OrbitMission","bounds ${it.getName()} ${it.getVisualBounds(placementOwner)}")}}
    fun pause(){suspended=true;grabbed=false;aiming=false;vm.setAiming(false);arbitration.reset();vm.setGrabbing(false);input.stop();audio.pause();vm.setThrust(false);vm.setControllerThrust(false);previousGrip=false;previousConfirm=false}
    override fun close(){if(disposed)return;disposed=true;input.close();audio.close();placementOwner.destroy();resources.reversed().forEach {runCatching {it.close()}};resources.clear();bodies.clear();panels.clear()}
}
