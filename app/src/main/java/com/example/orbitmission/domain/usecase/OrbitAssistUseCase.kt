package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import kotlin.math.abs

/** Relative display coordinates: gaze must hit the nearby XY orbital annulus. */
class OrbitAssistUseCase {
    fun snap(grab:Vector3D,origin:Vector3D,direction:Vector3D,radius:Double):Vector3D {
        if(!radius.isFinite() || radius<=0 || abs(direction.z)<1e-6 || abs(grab.z)>radius*.2 || abs(grab.length()-radius)>radius/6)return grab
        val distance=-origin.z/direction.z
        if(!distance.isFinite() || distance<=0)return grab
        val gaze=origin+direction*distance
        if(abs(gaze.length()-radius)>radius/6)return grab
        val target=gaze.normalized()*radius
        return if((target-grab).length()<=radius*(.22/.9))target else grab
    }
}
