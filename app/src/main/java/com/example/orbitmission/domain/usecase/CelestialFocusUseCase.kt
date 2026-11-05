package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Planet
import com.example.orbitmission.domain.model.Vector3D
import kotlin.math.pow

/** Focus normalizes globe volume; Sun radius and pair spacing share the same scale factor. */
class CelestialFocusUseCase {
    companion object {
        const val DURATION_SECONDS=1.2
        const val FOCUS_VOLUME_MULTIPLIER=5.0
        val FOCUSED_PLANET_RADIUS=Planet.JUPITER.displayRadiusMeters.toDouble()*FOCUS_VOLUME_MULTIPLIER.pow(1.0/3.0)
    }
    private val zero=Vector3D(0.0,0.0,0.0)
    private val overviewSun=Vector3D(-1.10,1.55,-3.2)
    private val overviewX=listOf(-.36,-.18,0.0,.18,.42,.66,.87,1.04)
    private val overview=Planet.entries.associateWith {Vector3D(overviewX[it.ordinal],1.55,-3.2)}
    private var selected:Planet?=null
    private var elapsed=DURATION_SECONDS
    private var startSun=overviewSun
    private var endSun=overviewSun
    private var startPositions=overview
    private var endPositions=overview
    private var startOpacity=Planet.entries.associateWith {1.0}
    private var endOpacity=startOpacity
    private var startRadii=Planet.entries.associateWith {it.displayRadiusMeters.toDouble()}
    private var endRadii=startRadii
    private var startSunRadius=Planet.OVERVIEW_SUN_RADIUS.toDouble()
    private var endSunRadius=startSunRadius
    private val progress:Double get(){val t=(elapsed/DURATION_SECONDS).coerceIn(0.0,1.0);return t*t*(3-2*t)}
    val moving get()=elapsed<DURATION_SECONDS
    val sunPosition get()=mix(startSun,endSun)
    val sunRadius get()=mix(startSunRadius,endSunRadius).toFloat()
    fun position(planet:Planet)=mix(startPositions.getValue(planet),endPositions.getValue(planet))
    fun opacity(planet:Planet):Double {val t=progress;return startOpacity.getValue(planet)*(1-t)+endOpacity.getValue(planet)*t}
    fun radius(planet:Planet)=mix(startRadii.getValue(planet),endRadii.getValue(planet)).toFloat()
    fun select(planet:Planet?){
        if(planet==selected)return
        startSun=sunPosition
        startPositions=Planet.entries.associateWith(::position)
        startOpacity=Planet.entries.associateWith(::opacity)
        startRadii=Planet.entries.associateWith {radius(it).toDouble()}
        startSunRadius=sunRadius.toDouble()
        selected=planet
        val offset=planet?.let {Vector3D(0.0,1.55,-2.1)-overview.getValue(it)}?:zero
        val scale=planet?.let {FOCUSED_PLANET_RADIUS/it.displayRadiusMeters}?:1.0
        endSun=if(planet==null)overviewSun else overview.getValue(planet)+offset-(overview.getValue(planet)-overviewSun)*scale
        endPositions=Planet.entries.associateWith {overview.getValue(it)+if(it==planet)offset else zero}
        endOpacity=Planet.entries.associateWith {if(planet==null || it==planet)1.0 else 0.0}
        endRadii=Planet.entries.associateWith {if(it==planet)FOCUSED_PLANET_RADIUS else it.displayRadiusMeters.toDouble()}
        endSunRadius=Planet.OVERVIEW_SUN_RADIUS*scale
        elapsed=0.0
    }
    fun step(seconds:Double){if(seconds.isFinite() && seconds>0)elapsed=(elapsed+seconds).coerceAtMost(DURATION_SECONDS)}
    private fun mix(a:Vector3D,b:Vector3D):Vector3D {val t=progress;return if(t>=1.0)b else a+(b-a)*t}
    private fun mix(a:Double,b:Double):Double {val t=progress;return if(t>=1.0)b else a+(b-a)*t}
}
