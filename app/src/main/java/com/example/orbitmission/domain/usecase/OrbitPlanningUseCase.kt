package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.*
import kotlin.math.*

/** Ballistic engine-off forecast using the same two-source integrator as live flight. */
object OrbitPlanningUseCase {
    fun predict(planet:Planet,position:Vector3D,velocity:Vector3D):OrbitPrediction {
        val mu=GravitySimulationUseCase.G*planet.massKg
        val radius=max(position.length(),1.0)
        val h=position.cross(velocity)
        val e=(velocity.cross(h)/mu-position/radius).length()
        val peri=h.dot(h)/(mu*(1.0+e))
        val energy=velocity.dot(velocity)/2-mu/radius
        val a=if(energy<0)-mu/(2*energy)else Double.POSITIVE_INFINITY
        val apo=if(a.isFinite())a*(1+e)else null
        val quality=when {
            radius<=planet.radiusMeters -> OrbitQuality.IMPACT
            energy>=0 && position.dot(velocity)>=0 -> OrbitQuality.ESCAPE
            peri<=planet.radiusMeters*1.02 -> OrbitQuality.IMPACT
            energy>=0 -> OrbitQuality.ESCAPE
            e<=.2 -> OrbitQuality.SAFE
            else -> OrbitQuality.ADJUST
        }
        val simulation=GravitySimulationUseCase(planet)
        var snapshot=simulation.launch(position,velocity)
        val period=if(a.isFinite())2*PI*sqrt(a.pow(3)/mu)/simulation.simulationTimeScale else 30.0
        val delta=period.coerceIn(8.0,45.0)/95.0
        val points=mutableListOf(position)
        repeat(95) {
            if(snapshot.status in listOf(FlightStatus.FLYING,FlightStatus.SUCCESS) && snapshot.position.length()<planet.radiusMeters*12) {
                snapshot=simulation.step(delta,false);points+=snapshot.position
            }
        }
        return OrbitPrediction(points,quality,peri/1000,apo?.div(1000),e)
    }
}
