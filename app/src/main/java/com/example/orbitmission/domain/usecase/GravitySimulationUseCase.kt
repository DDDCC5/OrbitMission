package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.*
import kotlin.math.*

/**
 * Restricted-primary approximation, entirely in SI. The selected planet is at (0,0,0),
 * the Sun at (-sunDistance,0,0), with separation frozen during a flight. Velocities and
 * osculating elements share the accelerating planet-relative frame. Integration subtracts
 * the planet's own solar acceleration (the indirect term); physical force telemetry retains
 * the full vector sum of solar and planetary GMm/r² forces on the rocket.
 * Display scaling belongs to the caller; it must never be fed back into these equations.
 * A recommended Kepler orbit lasts 30 wall seconds, using [simulationTimeScale].
 */
class GravitySimulationUseCase(val planet: Planet, val rocketMassKg: Double = 10000.0) {
    companion object {
        const val G = 6.67430e-11
        // NASA Sun Fact Sheet: https://nssdc.gsfc.nasa.gov/planetary/factsheet/sunfact.html
        const val SUN_MASS_KG = 1.9884e30
        const val SUN_RADIUS_METERS = 695_700_000.0
        const val SOLAR_BOUNDARY_METERS = 4515.0e9 * 1.2
        private val ZERO = Vector3D(0.0,0.0,0.0)
    }
    init { require(rocketMassKg.isFinite() && rocketMassKg>0.0) }
    private val mu=G*planet.massKg
    private val sun=Vector3D(-planet.sunDistanceMeters,0.0,0.0)
    private val recommendedRadius=1.5*planet.radiusMeters
    private val recommendedPeriod=2.0*PI*sqrt(recommendedRadius.pow(3)/mu)
    val simulationTimeScale: Double = recommendedPeriod/30.0
    private var position=recommendedPosition()
    private var velocity=ZERO
    private var elapsed=0.0
    private var progress=0.0
    private var normal=Vector3D(0.0,0.0,1.0)
    private var status=FlightStatus.READY

    fun recommendedPosition()=Vector3D(recommendedRadius,0.0,0.0)

    /** Tangent in the XY plane, with a safe alternative for a polar launch point. */
    fun circularVelocity(position: Vector3D): Vector3D {
        val radius=position.length()
        if(radius<=0.0 || !radius.isFinite()) return ZERO
        var tangent=Vector3D(0.0,0.0,1.0).cross(position).normalized()
        if(tangent.length()==0.0) tangent=Vector3D(0.0,1.0,0.0).cross(position).normalized()
        return tangent*sqrt(mu/radius)
    }

    fun launch(position: Vector3D, velocity: Vector3D): FlightSnapshot {
        require(isFinite(position) && isFinite(velocity)) { "Launch vectors must be finite" }
        this.position=position
        this.velocity=velocity
        elapsed=0.0
        progress=0.0
        val angularMomentum=position.cross(velocity).normalized()
        normal=if(angularMomentum.length()>0.0) angularMomentum else Vector3D(0.0,0.0,1.0)
        status=when {
            position.length()<=planet.radiusMeters || (position-sun).length()<=SUN_RADIUS_METERS -> FlightStatus.COLLISION
            (position-sun).length()>SOLAR_BOUNDARY_METERS -> FlightStatus.ESCAPED
            else -> FlightStatus.FLYING
        }
        return snapshot()
    }

    /**
     * Invalid deltas (nonpositive, nonfinite, or over one hour) are ignored, so a corrupt
     * clock cannot monopolize the UI thread. Terminal failures freeze until launch().
     */
    fun step(wallDeltaSeconds: Double, thrust: Boolean): FlightSnapshot {
        if(!wallDeltaSeconds.isFinite() || wallDeltaSeconds<=0.0 || wallDeltaSeconds>3600.0 ||
            status !in listOf(FlightStatus.FLYING,FlightStatus.SUCCESS)) return snapshot()
        val wallBudget=if(status==FlightStatus.SUCCESS) wallDeltaSeconds else min(wallDeltaSeconds,90.0-elapsed)
        var remaining=wallBudget*simulationTimeScale
        while(remaining>1e-9 && status in listOf(FlightStatus.FLYING,FlightStatus.SUCCESS)) {
            // Bound both curvature error and travel angle; swept tests handle extreme linear speed.
            val localTime=sqrt(max(position.length(),planet.radiusMeters).pow(3)/mu)
            val h=min(remaining,min(recommendedPeriod/4096.0,localTime/128.0))
            val oldPosition=position
            val a0=acceleration(position)+thrustAcceleration(velocity,thrust)
            val candidate=position+velocity*h+a0*(0.5*h*h)
            if(!isFinite(candidate)) { status=FlightStatus.ESCAPED; break }
            val hitPlanet=intersectionFraction(position,candidate,ZERO,planet.radiusMeters)
            val hitSun=intersectionFraction(position,candidate,sun,SUN_RADIUS_METERS)
            val hit=min(hitPlanet,hitSun)
            val used=if(hit.isFinite()) h*hit else h
            position=if(hit.isFinite()) oldPosition+(candidate-oldPosition)*hit else candidate
            val a1=acceleration(position)+thrustAcceleration(velocity+a0*h,thrust)
            velocity=velocity+(a0+a1)*(0.5*used)
            if(!isFinite(velocity)) { velocity=ZERO; status=FlightStatus.ESCAPED }
            elapsed+=used/simulationTimeScale
            remaining-=h
            if(hit.isFinite()) { status=FlightStatus.COLLISION; break }
            if((position-sun).length()>SOLAR_BOUNDARY_METERS) { status=FlightStatus.ESCAPED; break }
            updateProgress(oldPosition)
        }
        if(status==FlightStatus.FLYING && elapsed>=90.0-1e-8) {
            elapsed=90.0
            status=FlightStatus.TIMEOUT
        }
        return snapshot()
    }

    fun snapshot(): FlightSnapshot = FlightSnapshot(
        position=position,velocity=velocity,speedKmS=velocity.length()/1000.0,
        centerDistanceKm=position.length()/1000.0,
        forceNewtons=physicalGravity(position).length()*rocketMassKg,
        elapsedSeconds=elapsed,eccentricity=elements().first,
        remainingSeconds=max(0.0,90.0-elapsed),orbitProgressDegrees=progress*180.0/PI,status=status
    )

    private fun physicalGravity(p: Vector3D): Vector3D =
        gravityTerm(ZERO-p,mu,planet.radiusMeters)+gravityTerm(sun-p,G*SUN_MASS_KG,SUN_RADIUS_METERS)

    private fun acceleration(p: Vector3D): Vector3D =
        physicalGravity(p)-gravityTerm(sun,G*SUN_MASS_KG,SUN_RADIUS_METERS)

    private fun gravityTerm(toward: Vector3D, gm: Double, bodyRadius: Double): Vector3D {
        val radius=max(toward.length(),bodyRadius*1e-6)
        return toward.normalized()*(gm/(radius*radius))
    }

    // Constant acceleration along instantaneous velocity, with no fuel limit (2% circular speed / simulated second).
    private fun thrustAcceleration(v: Vector3D, enabled: Boolean): Vector3D =
        if(enabled) v.normalized()*(sqrt(mu/recommendedRadius)*0.02) else ZERO

    /** Eccentricity and periapsis from planet-relative r and v (no heliocentric velocity mix). */
    private fun elements(): Pair<Double,Double> {
        val r=position.length()
        if(r<=0.0) return 1.0 to 0.0
        val h=position.cross(velocity)
        val e=(velocity.cross(h)/mu-position/r).length()
        val finiteE=if(e.isFinite()) e else 1e99
        val periapsis=h.dot(h)/(mu*(1.0+finiteE))
        return finiteE to if(periapsis.isFinite()) periapsis else 0.0
    }

    private fun updateProgress(oldPosition: Vector3D) {
        val (eccentricity,periapsis)=elements()
        if(status!=FlightStatus.SUCCESS && (eccentricity>0.2 || periapsis<=planet.radiusMeters)) {
            progress=0.0
            return
        }
        val oldProjected=(oldPosition-normal*oldPosition.dot(normal)).normalized()
        val newProjected=(position-normal*position.dot(normal)).normalized()
        if(oldProjected.length()==0.0 || newProjected.length()==0.0) { progress=0.0; return }
        val angle=atan2(normal.dot(oldProjected.cross(newProjected)),oldProjected.dot(newProjected))
        progress=max(0.0,progress+angle)
        if(status==FlightStatus.FLYING && progress>=2.0*PI) status=FlightStatus.SUCCESS
    }

    /** First segment/sphere entry, including paths with both endpoints outside the body. */
    private fun intersectionFraction(start: Vector3D,end: Vector3D,center: Vector3D,radius: Double): Double {
        val offset=start-center
        val direction=end-start
        val a=direction.dot(direction)
        val c=offset.dot(offset)-radius*radius
        if(c<=0.0) return 0.0
        if(a<=0.0 || !a.isFinite()) return Double.POSITIVE_INFINITY
        val b=offset.dot(direction)
        val discriminant=b*b-a*c
        if(discriminant<0.0) return Double.POSITIVE_INFINITY
        val t=(-b-sqrt(discriminant))/a
        return if(t in 0.0..1.0) t else Double.POSITIVE_INFINITY
    }
    private fun isFinite(v: Vector3D)=v.x.isFinite() && v.y.isFinite() && v.z.isFinite() && v.length().isFinite()
}
