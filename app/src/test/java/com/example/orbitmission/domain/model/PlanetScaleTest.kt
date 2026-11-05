package com.example.orbitmission.domain.model

import org.junit.Assert.*
import org.junit.Test

class PlanetScaleTest {
    @Test fun `overview uses one common physical scale for Sun and every planet`() {
        for (planet in Planet.entries) {
            val visualRatio=planet.displayRadiusMeters / Planet.OVERVIEW_SUN_RADIUS
            assertEquals(planet.radiusMeters / Planet.SUN_RADIUS_METERS,visualRatio.toDouble(),1e-8)
        }
    }
    @Test fun `Earth is about one hundred and ninth of Sun diameter`() {
        assertEquals(109.08,(Planet.OVERVIEW_SUN_RADIUS / Planet.EARTH.displayRadiusMeters).toDouble(),.02)
        assertTrue(Planet.EARTH.displayRadiusMeters<.005f)
        assertEquals(11.21,(Planet.JUPITER.displayRadiusMeters / Planet.EARTH.displayRadiusMeters).toDouble(),.01)
    }
}
