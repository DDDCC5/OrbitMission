package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GravitySimulationUseCaseTest {
    @Test fun vectorArithmeticPreservesThreeDimensionalGeometry() {
        val x = Vector3D(3.0, 0.0, 0.0)
        val y = Vector3D(0.0, 4.0, 0.0)
        assertEquals(5.0, (x + y).length(), 1e-12)
        assertEquals(Vector3D(0.0, 0.0, 12.0), x.cross(y))
        assertEquals(0.0, x.dot(y), 0.0)
        assertEquals(Vector3D(1.0,0.0,0.0), x.normalized())
        assertEquals(Vector3D(0.0,0.0,0.0), Vector3D(0.0,0.0,0.0).normalized())
        assertEquals(x, ((x+y)-y)*2.0/2.0)
    }
    @Test fun earthGravityMatchesSurfaceGravityAndInverseSquare() {
        val sim = GravitySimulationUseCase(Planet.EARTH, 1.0)
        val atSurface = sim.launch(Vector3D(0.0,6_378_000.0,0.0),Vector3D(0.0,0.0,0.0)).forceNewtons
        val twice = sim.launch(Vector3D(0.0,12_756_000.0,0.0),Vector3D(0.0,0.0,0.0)).forceNewtons
        assertEquals(9.8, atSurface,0.05)
        assertEquals(4.0, atSurface/twice,0.001)
    }
    @Test fun sunlightContributesRealAccelerationBeyondPlanetOnlyForce() {
        val sim = GravitySimulationUseCase(Planet.EARTH,1.0)
        val far = sim.launch(Vector3D(1_000_000_000.0,0.0,0.0),Vector3D(0.0,0.0,0.0))
        assertTrue(far.forceNewtons > 0.006)
    }
    @Test fun allEightRecommendedOrbitsStayBoundAndCompleteAFullRevolution() {
        for (planet in Planet.entries) {
            val sim = GravitySimulationUseCase(planet)
            val p = sim.recommendedPosition()
            var s = sim.launch(p,sim.circularVelocity(p))
            repeat(1800) { s=sim.step(1.0/60.0,false) }
            assertTrue("$planet progress=${s.orbitProgressDegrees}, e=${s.eccentricity}, status=${s.status}, radius=${s.centerDistanceKm}",s.orbitProgressDegrees>300.0)
            repeat(900) { s=sim.step(1.0/60.0,false) }
            assertEquals("$planet",FlightStatus.SUCCESS,s.status)
            assertTrue("$planet eccentricity ${s.eccentricity}",s.eccentricity<=0.2)
            assertTrue("$planet distance",s.centerDistanceKm>planet.radiusMeters/1000.0)
            assertTrue(sim.simulationTimeScale>1.0)
        }
    }
    @Test fun partialOrbitCannotSucceedAndSuccessfulOrbitKeepsMovingPastTimeout() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        val p=sim.recommendedPosition()
        sim.launch(p,sim.circularVelocity(p))
        repeat(900) { sim.step(1.0/60.0,false) }
        assertEquals(FlightStatus.FLYING,sim.snapshot().status)
        assertTrue(sim.snapshot().orbitProgressDegrees in 160.0..200.0)
        repeat(1500) { sim.step(1.0/60.0,false) }
        val before=sim.snapshot().position
        repeat(3700) { sim.step(1.0/60.0,false) }
        assertEquals(FlightStatus.SUCCESS,sim.snapshot().status)
        assertTrue((sim.snapshot().position-before).length()>100.0)
        assertTrue(sim.snapshot().elapsedSeconds>90.0)
    }
    @Test fun inclinedAndRetrogradeOrbitsCountTheFullThreeDimensionalAngle() {
        val sim=GravitySimulationUseCase(Planet.MARS)
        val p=sim.recommendedPosition()
        val speed=sim.circularVelocity(p).length()
        sim.launch(p,Vector3D(0.0,0.0,-speed))
        repeat(2100) { sim.step(1.0/60.0,false) }
        assertEquals(FlightStatus.SUCCESS,sim.snapshot().status)
    }
    @Test fun sweptCollisionCatchesAPlanetCrossingBetweenEndpoints() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        sim.launch(Vector3D(-20_000_000.0,0.0,0.0),Vector3D(1e10,0.0,0.0))
        assertEquals(FlightStatus.COLLISION,sim.step(1.0,false).status)
    }
    @Test fun sweptCollisionCatchesSunCrossing() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        sim.launch(Vector3D(-Planet.EARTH.sunDistanceMeters-2e9,0.0,0.0),Vector3D(1e11,0.0,0.0))
        assertEquals(FlightStatus.COLLISION,sim.step(1.0,false).status)
    }
    @Test fun unboundFlightTimesOutAtNinetyWallSeconds() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        sim.launch(Vector3D(0.0,1e10,0.0),Vector3D(1000.0,0.0,0.0))
        sim.step(89.0,false)
        assertEquals(FlightStatus.FLYING,sim.snapshot().status)
        val s=sim.step(2.0,false)
        assertEquals(FlightStatus.TIMEOUT,s.status)
        assertEquals(90.0,s.elapsedSeconds,1e-8)
        assertEquals(0.0,s.remainingSeconds,0.0)
        assertEquals(0.0,s.orbitProgressDegrees,0.0)
    }
    @Test fun solarBoundaryEndsEscapingFlight() {
        val sim=GravitySimulationUseCase(Planet.NEPTUNE)
        assertEquals(FlightStatus.ESCAPED,sim.launch(Vector3D(2e12,0.0,0.0),Vector3D(0.0,0.0,0.0)).status)
    }
    @Test fun triggerIncreasesSpeedComparedWithCoasting() {
        val coast=GravitySimulationUseCase(Planet.EARTH)
        val boost=GravitySimulationUseCase(Planet.EARTH)
        val p=coast.recommendedPosition()
        val v=coast.circularVelocity(p)
        coast.launch(p,v); boost.launch(p,v)
        assertTrue(boost.step(0.1,true).speedKmS>coast.step(0.1,false).speedKmS)
    }
    @Test(timeout = 2000) fun absurdDeltaAfterSuccessDoesNotHangTheFrame() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        val p=sim.recommendedPosition(); sim.launch(p,sim.circularVelocity(p))
        sim.step(40.0,false)
        assertEquals(FlightStatus.SUCCESS,sim.snapshot().status)
        val before=sim.snapshot()
        assertEquals(before,sim.step(Double.MAX_VALUE,false))
    }
    @Test fun unstableOrbitResetsAccumulatedRevolution() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        val p=sim.recommendedPosition(); sim.launch(p,sim.circularVelocity(p))
        sim.step(10.0,false)
        assertTrue(sim.snapshot().orbitProgressDegrees>110.0)
        sim.step(0.5,true)
        assertTrue(sim.snapshot().eccentricity>0.2)
        assertEquals(0.0,sim.snapshot().orbitProgressDegrees,0.0)
    }
    @Test fun invalidDeltaDoesNotAdvanceOrCorruptTheState() {
        val sim=GravitySimulationUseCase(Planet.EARTH)
        val p=sim.recommendedPosition(); sim.launch(p,sim.circularVelocity(p))
        val initial=sim.snapshot()
        for (dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertEquals(initial,sim.step(dt,true))
        val center=sim.launch(Vector3D(0.0,0.0,0.0),Vector3D(0.0,0.0,0.0))
        assertEquals(FlightStatus.COLLISION,center.status)
        assertTrue(center.forceNewtons.isFinite())
        assertTrue(center.eccentricity.isFinite())
    }
}
