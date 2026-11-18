package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import kotlin.math.*

data class StartupAnchor(val position:Vector3D,val yawRadians:Double) {
    fun point(local:Vector3D):Vector3D {
        val c=cos(yawRadians);val s=sin(yawRadians)
        return position+Vector3D(c*local.x+s*local.z,local.y,-s*local.x+c*local.z)
    }
}
/** Wait for fresh settled tracking; latch once rather than making a head-locked scene. */
class ColdStartAnchorUseCase {
    private var elapsed=0.0
    private var stable=0.0
    private var previousPosition:Vector3D?=null
    private var previousForward:Vector3D?=null
    private var previousSampleId:Long?=null
    var anchor:StartupAnchor?=null;private set
    fun observe(position:Vector3D?,forward:Vector3D?,deltaSeconds:Double,sampleId:Long?=null):StartupAnchor? {
        anchor?.let {return it}
        val dt=if(deltaSeconds.isFinite())deltaSeconds.coerceIn(0.0,.1)else 0.0
        elapsed+=dt
        if(position==null || forward==null || !listOf(position.x,position.y,position.z,forward.x,forward.y,forward.z).all {it.isFinite()}) {
            stable=0.0;previousPosition=null;previousForward=null;return null
        }
        if(sampleId!=null && sampleId==previousSampleId)return null
        previousSampleId=sampleId
        val flat=Vector3D(forward.x,0.0,forward.z)
        if(flat.length()<.1){stable=0.0;return null}
        val heading=flat.normalized()
        val oldPosition=previousPosition;val oldHeading=previousForward
        stable=if(oldPosition!=null && oldHeading!=null && (position-oldPosition).length()<.035 && heading.dot(oldHeading)>cos(3*PI/180))stable+dt else 0.0
        previousPosition=position;previousForward=heading
        if(elapsed>=.8 && stable>=.2)anchor=StartupAnchor(position-Vector3D(0.0,1.55,0.0),atan2(-heading.x,-heading.z))
        return anchor
    }
}
