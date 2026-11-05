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

/** Native Stage smoke test drives the same mission actions; it does not claim physical input validation. */
@RunWith(AndroidJUnit4::class)
class MissionRuntimeTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun onMain(action:()->Unit)=instrumentation.runOnMainSync(action)
    private fun await(timeoutMs:Long=60000,predicate:()->Boolean){val until=SystemClock.elapsedRealtime()+timeoutMs;while(SystemClock.elapsedRealtime()<until){var done=false;onMain {done=predicate()};if(done)return;Thread.sleep(100)};error("Stage did not reach expected state")}
    private fun marker(name:String,holdMs:Long=6500){Thread.sleep(3500);Log.i("OrbitSmoke",name);Thread.sleep(holdMs)}
    @Test fun pairMotionAndTelemetryAcrossTargets(){
        val context=instrumentation.targetContext
        context.startActivity(Intent(context,LaunchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await {MissionRuntimeHandle.current()?.currentState?.phase in listOf(MissionPhase.TUTORIAL,MissionPhase.SELECTING)}
        lateinit var vm:MissionViewModel
        onMain {vm=MissionRuntimeHandle.current()!!;if(vm.currentState.phase==MissionPhase.TUTORIAL)vm.finishTutorial();vm.selectPlanet(Planet.EARTH)}
        await {!vm.currentState.sceneTransitioning};Thread.sleep(2000)
        onMain {vm.chooseOtherPlanet()};await {!vm.currentState.sceneTransitioning}
        Log.i("OrbitSmoke","SELECT_READY");Thread.sleep(3000)
        Log.i("OrbitSmoke","PAIR_MOTION_START")
        onMain {vm.selectPlanet(Planet.JUPITER);vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)}
        await {!vm.currentState.sceneTransitioning};Thread.sleep(1000)
        Log.i("OrbitSmoke","JUPITER_READY");Thread.sleep(6000)
        onMain {assertEquals(Planet.JUPITER,vm.currentState.selectedPlanet);assertEquals(0.0,vm.currentFlight!!.elapsedSeconds,0.0);vm.chooseOtherPlanet()}
        await {!vm.currentState.sceneTransitioning};Thread.sleep(1000)
        Log.i("OrbitSmoke","RETURN_AGAIN_READY");Thread.sleep(11000)
        onMain {assertNull(vm.currentState.selectedPlanet)}
    }
    @Test fun nativeMissionCompletesOrbitAndSupportsLanguageSettings(){
        val context=instrumentation.targetContext
        context.startActivity(Intent(context,LaunchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await {MissionRuntimeHandle.current()?.currentState?.phase in listOf(MissionPhase.TUTORIAL,MissionPhase.SELECTING)}
        lateinit var vm:MissionViewModel
        onMain {vm=MissionRuntimeHandle.current()!!;if(vm.currentState.phase==MissionPhase.TUTORIAL)vm.finishTutorial()}
        marker("SELECT_READY")
        onMain {vm.selectPlanet(Planet.EARTH);assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)}
        marker("PLACEMENT_READY")
        onMain {vm.launch()}
        await {vm.currentState.phase==MissionPhase.FLYING && (vm.currentFlight?.elapsedSeconds?:0.0)>2}
        marker("FLIGHT_READY",7000)
        await {vm.currentState.phase==MissionPhase.SUCCESS}
        onMain {assertTrue(vm.currentFlight!!.orbitProgressDegrees>=360.0);assertTrue(vm.currentFlight!!.eccentricity<.2)}
        marker("SUCCESS_READY")
        onMain {vm.retry();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase);vm.chooseOtherPlanet();vm.toggleSettings();vm.setLanguage(AppLanguage.ZH_HANT)}
        marker("SETTINGS_READY")
        onMain {assertEquals("zh-Hans",vm.currentState.language.voiceFolder);vm.setLanguage(AppLanguage.ZH_HANS);vm.toggleSettings()}
        await {!vm.currentState.sceneTransitioning}
        marker("RETURN_READY")
        Log.i("OrbitSmoke","PAIR_MOTION_START")
        onMain {vm.selectPlanet(Planet.JUPITER);assertTrue(vm.currentState.sceneTransitioning);vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)}
        await {!vm.currentState.sceneTransitioning}
        marker("JUPITER_READY")
        onMain {vm.chooseOtherPlanet();assertTrue(vm.currentState.sceneTransitioning)}
        await {!vm.currentState.sceneTransitioning}
        marker("RETURN_AGAIN_READY")
    }
}
