package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CelestialFocusUseCaseTest {
    private fun same(a:Vector3D,b:Vector3D){assertEquals(a.x,b.x,1e-9);assertEquals(a.y,b.y,1e-9);assertEquals(a.z,b.z,1e-9)}
    @Test fun `every focused planet has five times overview Jupiter volume with proportional Sun`() {
        for(p in Planet.entries){
            val focus=CelestialFocusUseCase()
            val ratio=focus.sunRadius/focus.radius(p)
            focus.select(p)
            repeat(12){focus.step(.1);assertEquals(ratio,focus.sunRadius/focus.radius(p),ratio*.000001f);assertEquals(1.55,focus.position(p).y,0.0);assertEquals(focus.sunPosition.y,focus.position(p).y,0.0)}
            assertEquals(0.0,focus.position(p).x,1e-9);assertEquals(-2.1,focus.position(p).z,1e-9)
            val radiusRatio=focus.radius(p)/Planet.JUPITER.displayRadiusMeters
            assertEquals(5.0,(radiusRatio*radiusRatio*radiusRatio).toDouble(),.000002)
            assertTrue(focus.sunPosition.x<focus.position(p).x)
            assertTrue((focus.position(p)-focus.sunPosition).length()>focus.sunRadius+focus.radius(p))
        }
    }
    @Test fun `unselected bodies stay in place and fade together while pair travels`() {
        val focus=CelestialFocusUseCase();val positions=Planet.entries.associateWith(focus::position)
        focus.select(Planet.EARTH);focus.step(.6)
        assertEquals(.5,focus.opacity(Planet.MARS),1e-8)
        for(p in Planet.entries.filter {it!=Planet.EARTH}){same(positions.getValue(p),focus.position(p));assertEquals(focus.opacity(Planet.MARS),focus.opacity(p),1e-9)}
        assertEquals(1.0,focus.opacity(Planet.EARTH),0.0)
        focus.step(.6);assertEquals(0.0,focus.opacity(Planet.MARS),1e-9)
    }
    @Test fun `pair spacing grows by the same factor as body radii throughout focus and return`() {
        for(planet in Planet.entries){
            val focus=CelestialFocusUseCase();val spacing=(focus.position(planet)-focus.sunPosition).length()
            val originalRadius=focus.radius(planet)
            focus.select(planet)
            repeat(12){focus.step(.1);assertEquals(focus.radius(planet)/originalRadius,( (focus.position(planet)-focus.sunPosition).length()/spacing).toFloat(),.00001f)}
            focus.select(null)
            repeat(12){focus.step(.1);assertEquals(focus.radius(planet)/originalRadius,((focus.position(planet)-focus.sunPosition).length()/spacing).toFloat(),.00001f)}
        }
    }
    @Test fun `return restores exact initial layout for repeated planet choices`() {
        val focus=CelestialFocusUseCase();val initial=Planet.entries.associateWith(focus::position);val sun=focus.sunPosition
        repeat(3){for(p in Planet.entries){
            focus.select(p);focus.step(1.2);focus.select(null);focus.step(.6)
            assertTrue(focus.opacity(Planet.MARS)>0)
            focus.step(.6);same(sun,focus.sunPosition)
            assertEquals(Planet.OVERVIEW_SUN_RADIUS,focus.sunRadius,0f)
            Planet.entries.forEach {same(initial.getValue(it),focus.position(it));assertEquals(1.0,focus.opacity(it),0.0);assertEquals(it.displayRadiusMeters,focus.radius(it),0f)}
        }}
    }
    @Test fun `reversing mid travel starts from current visible positions`() {
        val focus=CelestialFocusUseCase();focus.select(Planet.NEPTUNE);focus.step(.4)
        val before=focus.position(Planet.NEPTUNE);val sun=focus.sunPosition;val radius=focus.radius(Planet.NEPTUNE);val sunRadius=focus.sunRadius
        focus.select(null);same(before,focus.position(Planet.NEPTUNE));same(sun,focus.sunPosition);assertEquals(radius,focus.radius(Planet.NEPTUNE),0f);assertEquals(sunRadius,focus.sunRadius,0f)
        focus.step(1.2);same(CelestialFocusUseCase().position(Planet.NEPTUNE),focus.position(Planet.NEPTUNE))
    }
    @Test fun `invalid deltas cannot corrupt transforms`() {
        val focus=CelestialFocusUseCase();focus.select(Planet.JUPITER);val before=focus.sunPosition
        listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0).forEach {focus.step(it);same(before,focus.sunPosition)}
    }
}
