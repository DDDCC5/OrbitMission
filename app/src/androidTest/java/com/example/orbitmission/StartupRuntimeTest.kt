package com.example.orbitmission

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitmission.domain.model.MissionPhase
import com.example.orbitmission.platform.LaunchActivity
import com.example.orbitmission.ui.mission.MissionRuntimeHandle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Host force-stops the app before instrumentation, preserving data but exercising cold process startup. */
@RunWith(AndroidJUnit4::class)
class StartupRuntimeTest {
    @Test fun coldLogoWaitsForHeadAlignmentThenStartsItsFiveSecondDisplay() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        context.startActivity(Intent(context,LaunchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val end=SystemClock.elapsedRealtime()+45000;var found=false
        while(SystemClock.elapsedRealtime()<end){
            instrumentation.runOnMainSync {val state=MissionRuntimeHandle.current()?.currentState;found=state?.startupAligned==true && state.phase==MissionPhase.SPLASH}
            if(found)break;Thread.sleep(50)
        }
        assertTrue("Cold startup never reached an aligned Logo",found)
        val run=InstrumentationRegistry.getArguments().getString("evidenceRun","startup150")
        java.io.File(context.cacheDir,"interactive-state.txt").writeText("$run:STARTUP_LOGO")
        Thread.sleep(1500)
        instrumentation.runOnMainSync {
            val state=MissionRuntimeHandle.current()!!.currentState
            assertTrue(state.startupAligned);assertEquals(MissionPhase.SPLASH,state.phase)
            assertTrue(state.phaseSeconds<5.0)
        }
    }
}
