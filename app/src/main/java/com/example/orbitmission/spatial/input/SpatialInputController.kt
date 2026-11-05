package com.example.orbitmission.spatial.input

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.controller.ControllerActionData
import com.pico.spatial.tracking.controller.ControllerTrackingProvider
import com.pico.spatial.tracking.eye.EyeTrackingProvider
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

data class InputRay(val origin: Vector3, val direction: Vector3)

data class InputFrame(
    val gaze: InputRay?,
    val controller: InputRay?,
    val confirmPressed: Boolean,
    val gripPressed: Boolean,
    val triggerPressed: Boolean,
    val controllerRotation: Quat?,
    val controllerHand:String?=null,
)

/** Poses/rays are in current Stage coordinates. Consumers convert into their scene root. */
class SpatialInputController(context: Context) : AutoCloseable {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val eyeUpdateQueued = AtomicBoolean(false)
    private val eye = EyeTrackingProvider()
    private val controllers = ControllerTrackingProvider()
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
        return InputFrame(
            gaze = gaze,
            controller = pose?.let { InputRay(it.position, it.rotation.rotateVector(FORWARD)) },
            confirmPressed = if (right != null) action?.aButtonPressed == true
                else action?.xButtonPressed == true,
            gripPressed = action?.gripPressed == true,
            triggerPressed = action?.triggerPressed == true,
            controllerRotation = pose?.rotation,
            controllerHand = if(pose==null)null else if(right!=null)"right" else "left",
        )
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
