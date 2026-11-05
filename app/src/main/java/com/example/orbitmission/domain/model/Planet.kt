package com.example.orbitmission.domain.model

// NASA Planetary Fact Sheet: https://nssdc.gsfc.nasa.gov/planetary/factsheet/
// SI values; equatorial radius = fact-sheet diameter / 2. Signed sidereal rotation.
enum class Planet(val id: String, val massKg: Double, val radiusMeters: Double,
    val sunDistanceMeters: Double, val rotationPeriodSeconds: Double) {
    MERCURY("mercury", 0.330e24, 2_439_500.0, 57.9e9, 5_067_360.0),
    VENUS("venus", 4.87e24, 6_052_000.0, 108.2e9, -20_997_000.0),
    EARTH("earth", 5.97e24, 6_378_000.0, 149.6e9, 86_164.2),
    MARS("mars", 0.642e24, 3_396_000.0, 228.0e9, 88_560.0),
    JUPITER("jupiter", 1898e24, 71_492_000.0, 778.5e9, 35_640.0),
    SATURN("saturn", 568e24, 60_268_000.0, 1432.0e9, 38_520.0),
    URANUS("uranus", 86.8e24, 25_559_000.0, 2867.0e9, -61_920.0),
    NEPTUNE("neptune", 102e24, 24_764_000.0, 4515.0e9, 57_960.0);

    /** One common physical radius scale, including the Sun, in overview mode. */
    val displayRadiusMeters:Float get()=(radiusMeters/SUN_RADIUS_METERS*OVERVIEW_SUN_RADIUS).toFloat()
    companion object {
        const val SUN_RADIUS_METERS=695_700_000.0
        const val OVERVIEW_SUN_RADIUS=.52f
    }
}
