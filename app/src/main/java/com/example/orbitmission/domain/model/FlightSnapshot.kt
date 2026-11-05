package com.example.orbitmission.domain.model

enum class FlightStatus { READY, FLYING, SUCCESS, COLLISION, ESCAPED, TIMEOUT }

data class FlightSnapshot(
    val position: Vector3D,
    val velocity: Vector3D,
    val speedKmS: Double,
    val centerDistanceKm: Double,
    val forceNewtons: Double,
    val elapsedSeconds: Double,
    val eccentricity: Double,
    val remainingSeconds: Double,
    val orbitProgressDegrees: Double,
    val status: FlightStatus
)
