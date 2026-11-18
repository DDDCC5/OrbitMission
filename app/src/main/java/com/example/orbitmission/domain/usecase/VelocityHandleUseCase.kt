package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import kotlin.math.abs

/** Display-space XY aim plane; only the returned velocity is in SI units. */
object VelocityHandleUseCase {
    const val METERS_PER_CIRCULAR_SPEED=.22
    fun drag(origin:Vector3D,direction:Vector3D,rocket:Vector3D,circularSpeed:Double):Vector3D? {
        if(!listOf(origin.x,origin.y,origin.z,direction.x,direction.y,direction.z,circularSpeed).all {it.isFinite()} || circularSpeed<=0 || abs(direction.z)<.001)return null
        val t=(rocket.z-origin.z)/direction.z
        if(t !in .05..10.0)return null
        val delta=origin+direction*t-rocket
        if(delta.length()<.025)return null
        return delta.normalized()*(circularSpeed*(delta.length()/METERS_PER_CIRCULAR_SPEED).coerceIn(.35,1.6))
    }
}
