package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class OrbitPlanningUseCaseTest {
    @Test fun circularForecastIsSafeAndBoundedForEveryPlanet() {
        for (planet in Planet.entries) {
            val sim=GravitySimulationUseCase(planet); val p=sim.recommendedPosition()
            val forecast=OrbitPlanningUseCase.predict(planet,p,sim.circularVelocity(p))
            assertEquals(planet.id,OrbitQuality.SAFE,forecast.quality)
            assertTrue(forecast.points.size in 2..96)
            assertEquals(p,forecast.points.first())
            assertTrue(forecast.points.all {it.length().isFinite()})
        }
    }
    @Test fun lowSpeedAndEscapeGiveDifferentActionableFeedback() {
        val sim=GravitySimulationUseCase(Planet.EARTH);val p=sim.recommendedPosition();val v=sim.circularVelocity(p)
        assertEquals(OrbitQuality.IMPACT,OrbitPlanningUseCase.predict(Planet.EARTH,p,v*.7).quality)
        assertEquals(OrbitQuality.ESCAPE,OrbitPlanningUseCase.predict(Planet.EARTH,p,v*1.5).quality)
        assertEquals(OrbitQuality.ADJUST,OrbitPlanningUseCase.predict(Planet.EARTH,p,v*1.15).quality)
    }
    @Test fun forecastDoesNotMutateLiveFlight() {
        val sim=GravitySimulationUseCase(Planet.MARS);val p=sim.recommendedPosition();sim.launch(p,sim.circularVelocity(p))
        val before=sim.step(2.0,false)
        OrbitPlanningUseCase.predict(Planet.MARS,before.position,before.velocity)
        assertEquals(before,sim.snapshot())
    }
    @Test fun retrogradeReducesSpeedComparedWithCoasting() {
        val coast=GravitySimulationUseCase(Planet.EARTH);val brake=GravitySimulationUseCase(Planet.EARTH)
        val p=coast.recommendedPosition();val v=coast.circularVelocity(p);coast.launch(p,v);brake.launch(p,v)
        assertTrue(brake.step(.2,-1.0).speedKmS<coast.step(.2,0.0).speedKmS)
    }
    @Test fun thrustIsControllableInWallTimeAcrossPlanets() {
        for (planet in Planet.entries) {
            val sim=GravitySimulationUseCase(planet);val p=sim.recommendedPosition();val v=sim.circularVelocity(p)
            sim.launch(p,v);val result=sim.step(.1,1.0)
            assertTrue(result.speedKmS*1000<v.length()*1.03)
        }
    }
}
