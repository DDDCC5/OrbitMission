package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import kotlin.math.*

data class PinchSample(val hand:String,val position:Vector3D,val strength:Double,val rayOrigin:Vector3D?=null,val rayDirection:Vector3D?=null)
data class PinchDragResult(val activeHand:String?=null,val tip:Vector3D?=null,val started:Boolean=false,val released:Boolean=false,val cancelled:Boolean=false)

/** One sticky hand, pinch hysteresis, relative XY drag and release-to-rearm after tracking loss. */
class PinchVelocityUseCase {
    private var owner:String?=null
    private var startHand:Vector3D?=null
    private var startTip:Vector3D?=null
    private var filteredTip:Vector3D?=null
    private val blocked=mutableSetOf<String>()
    fun reset(requireRelease:Boolean=true){owner=null;startHand=null;startTip=null;filteredTip=null;blocked.clear();if(requireRelease)blocked.addAll(listOf("left","right"))}
    fun update(samples:List<PinchSample>,tip:Vector3D,dt:Double):PinchDragResult {
        val valid=samples.filter {listOf(it.position.x,it.position.y,it.position.z,it.strength).all(Double::isFinite)}
        valid.filter {it.strength<.35}.forEach {blocked.remove(it.hand)}
        val current=owner
        if(current!=null){
            val sample=valid.firstOrNull {it.hand==current}
            if(sample==null){blocked.add(current);blocked.addAll(listOf("left","right"));owner=null;return PinchDragResult(cancelled=true)}
            if(sample.strength<.35){owner=null;valid.filter {it.strength>=.85}.forEach {blocked.add(it.hand)};return PinchDragResult(released=true)}
            val motion=sample.position-startHand!!
            val target=startTip!!+Vector3D(motion.x*1.5,motion.y*1.5,0.0)
            val alpha=1-exp(-(if(dt.isFinite())dt.coerceIn(0.0,.1)else 0.0)/.04)
            filteredTip=filteredTip!!+(target-filteredTip!!)*alpha
            return PinchDragResult(current,if(motion.length()>.003)filteredTip else null)
        }
        val sample=valid.firstOrNull {it.hand !in blocked && it.strength>=.85 && targets(it,tip)}?:return PinchDragResult()
        owner=sample.hand;startHand=sample.position;startTip=tip;filteredTip=tip
        return PinchDragResult(sample.hand,started=true)
    }
    private fun targets(sample:PinchSample,tip:Vector3D):Boolean {
        if((sample.position-tip).length()<.08)return true
        val origin=sample.rayOrigin?:return false;val raw=sample.rayDirection?:return false
        if(!listOf(origin.x,origin.y,origin.z,raw.x,raw.y,raw.z).all(Double::isFinite) || raw.length()<.1)return false
        val direction=raw.normalized();val along=(tip-origin).dot(direction)
        return along in 0.0..10.0 && (origin+direction*along-tip).length()<.055
    }
}
