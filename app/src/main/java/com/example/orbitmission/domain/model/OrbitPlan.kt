package com.example.orbitmission.domain.model

enum class MissionMode { GUIDED, CHALLENGE }
enum class OrbitQuality { SAFE, ADJUST, IMPACT, ESCAPE }
data class OrbitPrediction(val points:List<Vector3D>,val quality:OrbitQuality,
    val periapsisKm:Double,val apoapsisKm:Double?,val eccentricity:Double)
