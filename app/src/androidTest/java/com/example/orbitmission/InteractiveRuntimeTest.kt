package com.example.orbitmission

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.platform.LaunchActivity
import com.example.orbitmission.ui.mission.MissionRuntimeHandle
import com.example.orbitmission.ui.mission.MissionViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real scene clock/integrator, driven through production actions; no synthetic controller claims. */
@RunWith(AndroidJUnit4::class)
class InteractiveRuntimeTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun main(action:()->Unit)=instrumentation.runOnMainSync(action)
    private fun await(timeout:Long=65000,predicate:()->Boolean) {
        val end=SystemClock.elapsedRealtime()+timeout
        while(SystemClock.elapsedRealtime()<end){var ready=false;main {ready=predicate()};if(ready)return;Thread.sleep(100)}
        error("Interactive native state did not arrive")
    }
    private fun marker(name:String){
        Thread.sleep(800)
        val run=InstrumentationRegistry.getArguments().getString("evidenceRun","interactive140")
        java.io.File(instrumentation.targetContext.cacheDir,"interactive-state.txt").writeText("$run:$name")
        Log.i("OrbitInteractive",name);Thread.sleep(5000)
    }
    @Test fun planningCorrectionAndChallengeUseTheNativeScene() {
        val context=instrumentation.targetContext
        context.startActivity(Intent(context,LaunchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await {MissionRuntimeHandle.current()?.currentState?.phase in listOf(MissionPhase.TUTORIAL,MissionPhase.SELECTING)}
        lateinit var vm:MissionViewModel
        main {vm=MissionRuntimeHandle.current()!!;if(vm.currentState.phase==MissionPhase.TUTORIAL)vm.finishTutorial();vm.selectPlanet(Planet.EARTH)}
        await {!vm.currentState.sceneTransitioning}
        main {assertEquals(OrbitQuality.IMPACT,vm.currentState.prediction?.quality);vm.confirmPlacement();vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)}
        marker("UNSAFE_PLAN")
        main {vm.setAiming(true);vm.adjustLaunchSpeed(.05);vm.tick(.26);vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase);vm.setAiming(false);assertTrue(vm.currentState.canLaunch);assertEquals(OrbitQuality.SAFE,vm.currentState.prediction?.quality)}
        marker("SAFE_PLAN")
        main {vm.launch()};await {vm.currentState.phase==MissionPhase.FLYING}
        Thread.sleep(1500)
        main {vm.pulseThrust(-1);assertTrue(vm.currentState.thrustCommand<0)}
        await {vm.currentState.thrustCommand==0.0 && vm.currentState.correctionUsed}
        marker("CORRECTION_COAST")
        await {vm.currentState.phase==MissionPhase.SUCCESS}
        main {assertTrue(vm.currentFlight!!.orbitProgressDegrees>=360)}
        marker("ORBIT_SUCCESS")
        main {vm.retry();vm.setMode(MissionMode.CHALLENGE);vm.confirmPlacement();vm.adjustLaunchSpeed(.01);vm.tick(.26);assertTrue(vm.currentState.canLaunch);assertEquals(OrbitQuality.IMPACT,vm.currentState.prediction?.quality)}
        marker("CHALLENGE_READY")
        main {vm.launch()};await {vm.currentState.phase==MissionPhase.FAILURE}
        main {assertEquals(FlightStatus.COLLISION,vm.currentFlight?.status)}
        marker("CHALLENGE_FAILURE")
        main {vm.retry();vm.setMode(MissionMode.GUIDED);vm.setLanguage(AppLanguage.EN)}
        marker("ENGLISH_PLAN")
        main {vm.setLanguage(AppLanguage.ZH_HANS)}
    }
}
