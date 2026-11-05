package com.example.orbitmission.domain.model

import kotlin.math.hypot

data class Vector3D(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vector3D) = Vector3D(x+other.x,y+other.y,z+other.z)
    operator fun minus(other: Vector3D) = Vector3D(x-other.x,y-other.y,z-other.z)
    operator fun times(scale: Double) = Vector3D(x*scale,y*scale,z*scale)
    operator fun div(scale: Double) = Vector3D(x/scale,y/scale,z/scale)
    fun length(): Double = hypot(hypot(x,y),z)
    fun normalized(): Vector3D {
        val magnitude=length()
        return if(magnitude>0.0 && magnitude.isFinite()) this/magnitude else Vector3D(0.0,0.0,0.0)
    }
    fun dot(other: Vector3D): Double = x*other.x+y*other.y+z*other.z
    fun cross(other: Vector3D) = Vector3D(y*other.z-z*other.y,z*other.x-x*other.z,x*other.y-y*other.x)
}
