package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Planet

enum class GrabAction { NONE, MOVE, RELEASE, CANCEL }
data class TargetSelection(val hovered:Planet?,val progress:Float,val confirmed:Planet?)
/** Resolve multimodal input before it can mutate a mission. Tracking loss is never a release. */
class InputArbitrationUseCase {
    private var dwell:Planet?=null
    private var seconds=0.0
    private var hand:String?=null
    fun target(controller:Planet?,gaze:Planet?,dt:Double):TargetSelection {
        val target=if(controller!=null && controller!=gaze)null else gaze
        if(target!=dwell){dwell=target;seconds=0.0}
        if(target!=null && dt.isFinite() && dt>0)seconds+=dt
        return TargetSelection(controller?:gaze,if(target==null)0f else (seconds/2).coerceIn(0.0,1.0).toFloat(),target?.takeIf {seconds>=2})
    }
    fun beginGrab(controllerHand:String){hand=controllerHand}
    fun grab(controllerHand:String?,pressed:Boolean):GrabAction {
        if(hand==null)return GrabAction.NONE
        if(controllerHand==null || controllerHand!=hand){hand=null;return GrabAction.CANCEL}
        if(!pressed){hand=null;return GrabAction.RELEASE}
        return GrabAction.MOVE
    }
    fun reset(){dwell=null;seconds=0.0;hand=null}
}
