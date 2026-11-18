package com.example.orbitmission.spatial.input

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.controller.ControllerActionData
import com.pico.spatial.tracking.controller.ControllerTrackingProvider
import com.pico.spatial.tracking.eye.EyeTrackingProvider
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import com.pico.spatial.tracking.hmd.HMDTrackingData
import com.pico.spatial.tracking.hand.HandTrackingProvider
import com.pico.spatial.tracking.hand.HandTrackingData
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandPose
import com.pico.spatial.tracking.hand.HandPinch
import com.pico.spatial.tracking.hand.HandAim
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

data class InputRay(val origin: Vector3, val direction: Vector3)

data class TrackedHead(val position:Vector3,val rotation:Quat,val sampleId:Long)
data class PinchInput(val hand:String,val position:Vector3,val strength:Double,val aim:InputRay?)

data class InputFrame(
    val gaze: InputRay?,
    val controller: InputRay?,
    val confirmPressed: Boolean,
    val gripPressed: Boolean,
    val triggerPressed: Boolean,
    val controllerRotation: Quat?,
    val controllerHand:String?=null,
    val head:TrackedHead?=null,
    val hands:List<PinchInput> = emptyList(),
    val handTrackingAvailable:Boolean=false,
)

/** Poses/rays are in current Stage coordinates. Consumers convert into their scene root. */
class SpatialInputController(context: Context) : AutoCloseable {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val eyeUpdateQueued = AtomicBoolean(false)
    private val eye = EyeTrackingProvider()
    private val controllers = ControllerTrackingProvider()
    private val hmd = HMDTrackingProvider()
    private val handProvider = HandTrackingProvider()
    private val headData = AtomicReference<HMDTrackingData?>(null)
    private val handData = AtomicReference<HandTrackingData?>(null)
    private val headReceived = AtomicLong(0)
    private val headSequence = AtomicLong(0)
    private val handReceived = AtomicLong(0)
    private val headListener = DataProvider.DataListener<HMDTrackingData> { data ->
        if(running && !closed){headData.set(data);headReceived.set(SystemClock.elapsedRealtimeNanos());headSequence.incrementAndGet()}
    }
    private val handListener = DataProvider.DataListener<HandTrackingData> { data ->
        if(running && !closed){handData.set(data);handReceived.set(SystemClock.elapsedRealtimeNanos())}
    }
    private val actions = AtomicReference<ControllerActionData?>(null)
    private val listener = ControllerTrackingProvider.ControllerActionListener { actions.set(it) }
    @Volatile private var running = false
    @Volatile private var eyeStarted = false
    @Volatile private var closed = false

    val eyeAvailable: Boolean
        get() = running && eyeStarted && eyePermissionGranted() &&
            eye.supportState == DataProvider.SupportState.SUPPORTED

    val controllerAvailable: Boolean
        get() = running && controllers.supportState == DataProvider.SupportState.SUPPORTED &&
            (controllers.latestData.right != null || controllers.latestData.left != null)

    fun start() = onMain {
        if (running) { updateEyeTracking(); return@onMain }
        running = true
        hmd.addListener(headListener);handProvider.addListener(handListener)
        runCatching {hmd.start()}.onFailure {Log.e(TAG,"HMD tracking could not start",it)}
        runCatching {handProvider.start()}.onFailure {Log.e(TAG,"Hand tracking could not start",it)}
        controllers.addControllerActionListener(listener)
        runCatching { controllers.start() }
            .onFailure { Log.e(TAG, "Controller tracking could not start", it) }
        updateEyeTracking()
    }

    fun sample(): InputFrame {
        if (!running || closed) return EMPTY
        // The rendering caller may be off-main; provider lifecycle mutations stay on main.
        if (Looper.myLooper() == Looper.getMainLooper()) updateEyeTracking()
        else if (eyeUpdateQueued.compareAndSet(false, true)) main.post {
            eyeUpdateQueued.set(false)
            if (running && !closed) updateEyeTracking()
        }
        val gaze = if (eyeAvailable) {
            eye.latestData.takeIf { it.timestamp > 0L }?.eyePose?.let {
                InputRay(it.position, it.rotation.rotateVector(FORWARD))
            }
        } else null
        val tracking = if (controllerAvailable) controllers.latestData else null
        val right = tracking?.right
        val pose = right ?: tracking?.left
        val action = if (pose == null) null else {
            actions.get()?.let { if (right != null) it.right else it.left }
        }
        if (tracking == null) actions.set(null)
        val now=SystemClock.elapsedRealtimeNanos()
        val head=if(hmd.state==DataProvider.State.STARTED && hmd.supportState==DataProvider.SupportState.SUPPORTED && headReceived.get()>0 && now-headReceived.get()<250_000_000L)
            headData.get()?.hmdPose?.let {TrackedHead(it.position,it.rotation,headSequence.get())} else null
        val handsReady=handProvider.state==DataProvider.State.STARTED && handProvider.supportState==DataProvider.SupportState.SUPPORTED && handReceived.get()>0 && now-handReceived.get()<250_000_000L
        val trackedHands=if(handsReady)handData.get() else null
        val handSamples=trackedHands?.let { data->listOfNotNull(
            pinchInput("right",data.right,data.rightPinch,data.rightAim),
            pinchInput("left",data.left,data.leftPinch,data.leftAim))}.orEmpty()
        return InputFrame(
            gaze = gaze,
            head = head,
            hands = handSamples,
            handTrackingAvailable = handsReady && handSamples.isNotEmpty(),
            controller = pose?.let { InputRay(it.position, it.rotation.rotateVector(FORWARD)) },
            confirmPressed = if (right != null) action?.aButtonPressed == true
                else action?.xButtonPressed == true,
            gripPressed = action?.gripPressed == true,
            triggerPressed = action?.triggerPressed == true,
            controllerRotation = pose?.rotation,
            controllerHand = if(pose==null)null else if(right!=null)"right" else "left",
        )
    }

    private fun pinchInput(id:String,pose:HandPose?,pinch:HandPinch?,aim:HandAim?):PinchInput? {
        if(pose==null)return null
        val thumb=pose.handJoints.firstOrNull {it.index==HandJoint.Index.THUMB_TIP}?.position?:return null
        val index=pose.handJoints.firstOrNull {it.index==HandJoint.Index.INDEX_TIP}?.position?:return null
        val dx=(thumb.x-index.x).toDouble();val dy=(thumb.y-index.y).toDouble();val dz=(thumb.z-index.z).toDouble()
        val distance=kotlin.math.sqrt(dx*dx+dy*dy+dz*dz)
        val official=pinch?.takeIf {it.isReady}
        if(!distance.isFinite() || distance<=.001 && official==null)return null
        val position=official?.pinchPose?.position?:Vector3((thumb.x+index.x)/2,(thumb.y+index.y)/2,(thumb.z+index.z)/2)
        val strength=official?.pinchValue?.toDouble()?:((.055-distance)/.04).coerceIn(0.0,1.0)
        val ray=aim?.takeIf {it.isReady}?.aimPose?.let {InputRay(it.position,it.rotation.rotateVector(FORWARD))}
        return PinchInput(id,position,strength,ray)
    }

    private fun eyePermissionGranted() =
        context.checkSelfPermission(EYE_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun updateEyeTracking() {
        if (!eyePermissionGranted()) {
            if (eyeStarted) eye.stop()
            eyeStarted = false
            return
        }
        if (!eyeStarted && eye.supportState == DataProvider.SupportState.SUPPORTED) {
            runCatching { eye.start(); eyeStarted = true }
                .onFailure { Log.e(TAG, "Eye tracking could not start", it) }
        }
    }

    fun stop() = onMain { stopInternal() }

    private fun stopInternal() {
        if (!running) return
        running = false
        hmd.removeListener(headListener);handProvider.removeListener(handListener)
        hmd.stop();handProvider.stop();headData.set(null);handData.set(null);headReceived.set(0);handReceived.set(0)
        controllers.removeControllerActionListener(listener)
        controllers.stop()
        if (eyeStarted) eye.stop()
        eyeStarted = false
        actions.set(null)
    }

    override fun close() = onMain {
        stopInternal()
        closed = true
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!closed) action()
        } else main.post { if (!closed) action() }
    }

    companion object {
        const val EYE_PERMISSION = "com.picovr.permission.EYE_TRACKING"
        private const val TAG = "OrbitInput"
        private val FORWARD = Vector3(0f, 0f, -1f)
        private val EMPTY = InputFrame(null, null, false, false, false, null)
    }
}
