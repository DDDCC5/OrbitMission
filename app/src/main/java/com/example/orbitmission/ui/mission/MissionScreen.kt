package com.example.orbitmission.ui.mission

import android.util.Log
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.spatial.scene.OrbitScene
import com.example.orbitmission.ui.mission.components.*
import com.example.orbitmission.ui.theme.OrbitColors
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.content.SpatialView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.os.SystemClock
import java.util.Locale

@Composable
fun MissionScreen() {
    val context=LocalContext.current
    val vm:MissionViewModel=viewModel(factory=remember(context){MissionViewModelFactory(context)})
    val scene=remember(vm){OrbitScene(context,vm)}
    DisposableEffect(vm){MissionRuntimeHandle.attach(vm);onDispose {MissionRuntimeHandle.detach(vm)}}
    val lifecycleOwner=LocalLifecycleOwner.current
    val lifecycle=lifecycleOwner.lifecycle
    val activity=LocalActivity.current
    val requestEye:()->Unit={
        val permission="com.picovr.permission.EYE_TRACKING"
        if(activity!=null && activity.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED) activity.requestPermissions(arrayOf(permission),410)
    }
    DisposableEffect(scene,lifecycle){
        val observer=LifecycleEventObserver {_,event->
            Log.i("OrbitMission","lifecycle=$event")
            when(event){
            Lifecycle.Event.ON_PAUSE,Lifecycle.Event.ON_STOP->{vm.setActive(false);scene.pause()}
            Lifecycle.Event.ON_RESUME->vm.setActive(true)
            else->Unit
        }}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer);scene.close()}
    }
    LaunchedEffect(scene){
        // Stage's invisible root Compose surface can stop requesting frames while ECS keeps rendering.
        // Use a monotonic main-thread simulation clock independent of 2D recomposition.
        var last=SystemClock.elapsedRealtimeNanos()
        while(true){delay(16);val next=SystemClock.elapsedRealtimeNanos();val dt=((next-last)/1e9).coerceIn(0.0,.10);last=next;scene.tick(dt)}
    }
    SpatialView(modifier=Modifier.fillMaxSize(),
        attachments={
            // Each native attachment has its own composition/frame lifecycle. Observe state
            // in that composition instead of capturing a snapshot from the hidden Stage root.
            // A hidden/initially empty panel must still observe the owning Activity. The
            // transparent one-pixel minimum keeps its native composition measurable.
            AttachmentPanel("main"){Box(Modifier.defaultMinSize(1.dp,1.dp)){val state by vm.uiState.collectAsStateWithLifecycle(lifecycleOwner=lifecycleOwner,minActiveState=Lifecycle.State.CREATED);MainPanel(state,vm,requestEye)}}
            AttachmentPanel("telemetry"){Box(Modifier.defaultMinSize(1.dp,1.dp)){val state by vm.uiState.collectAsStateWithLifecycle(lifecycleOwner=lifecycleOwner,minActiveState=Lifecycle.State.CREATED);FlightTelemetry(state,vm)}}
            AttachmentPanel("controls"){Box(Modifier.defaultMinSize(1.dp,1.dp)){val state by vm.uiState.collectAsStateWithLifecycle(lifecycleOwner=lifecycleOwner,minActiveState=Lifecycle.State.CREATED);MissionControls(state,vm)}}
            AttachmentPanel("prompt"){Box(Modifier.defaultMinSize(1.dp,1.dp)){val state by vm.uiState.collectAsStateWithLifecycle(lifecycleOwner=lifecycleOwner,minActiveState=Lifecycle.State.CREATED);MissionPrompt(state,vm)}}
        },
        initial={content,attachments->
            try {
                // Make attachment loading/error states visible before any asynchronous asset load.
                content.addEntity(scene.placementOwner)
                listOf("main","telemetry","controls","prompt").forEach {scene.attachPanel(it,attachments.entity(it))}
                scene.initialize { /* owner was already attached above */ }
            }catch(e:CancellationException){throw e}
            catch(e:Exception){Log.e("OrbitMission","Scene initialization failed",e);vm.failLoading(e.message?:e.javaClass.simpleName)}
        }
    )
}
