package com.example.orbitmission.ui.mission

import com.example.orbitmission.data.repository.BundledMissionRepository
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.MissionSessionUseCase
import org.junit.Assert.*
import org.junit.Test

class InteractiveMissionTest {
    private fun ready():MissionViewModel {
        val vm=MissionViewModel(MissionSessionUseCase(BundledMissionRepository(emptyMap(),MissionPreferences(tutorialSeen=true))))
        vm.assetsReady();vm.tick(5.1);vm.tick(6.0);vm.selectPlanet(Planet.EARTH);vm.tick(1.3)
        return vm
    }
    @Test fun releasePlacementDoesNotLaunchAndGuidedRequiresAnAdjustment() {
        val vm=ready();vm.setGrabbing(true);vm.setGrabbing(false);vm.confirmPlacement();vm.launch()
        assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)
        vm.adjustLaunchSpeed(.05);vm.tick(.26);assertTrue(vm.currentState.canLaunch);vm.launch()
        assertEquals(MissionPhase.FLYING,vm.currentState.phase)
    }
    @Test fun challengeCanLaunchUnsafePlanButGuidedCannot() {
        val vm=ready();vm.setMode(MissionMode.CHALLENGE);vm.confirmPlacement();vm.adjustLaunchSpeed(.01);vm.tick(.26)
        assertEquals(OrbitQuality.IMPACT,vm.currentState.prediction?.quality)
        assertTrue(vm.currentState.canLaunch);vm.launch()
        repeat(250){vm.tick(.05)}
        assertEquals(MissionPhase.FAILURE,vm.currentState.phase)
    }
    @Test fun predictionAdjustmentChangesTrajectoryAndFreezesBehindSettings() {
        val vm=ready();val before=vm.currentState.prediction;vm.confirmPlacement();vm.adjustLaunchSpeed(.05);vm.tick(.26)
        assertNotEquals(before,vm.currentState.prediction)
        vm.toggleSettings();vm.adjustLaunchSpeed(.1)
        val frozen=vm.currentState.prediction;vm.tick(1.0);assertEquals(frozen,vm.currentState.prediction)
    }
    @Test fun retryKeepsLaunchParametersAndClearsActiveThrust() {
        val vm=ready();vm.confirmPlacement();vm.adjustLaunchSpeed(.05);vm.tick(.26);val speed=vm.currentState.launchSpeedRatio
        vm.launch();vm.pulseThrust(-1);vm.retry()
        assertEquals(speed,vm.currentState.launchSpeedRatio,0.0)
        assertEquals(0.0,vm.currentState.thrustCommand,0.0)
        assertFalse(vm.currentState.velocityAdjusted)
    }
    @Test fun cancelInputNeverCarriesEngineAcrossPauseOrModeChange() {
        val vm=ready();vm.confirmPlacement();vm.adjustLaunchSpeed(.05);vm.tick(.26);vm.launch();vm.setControllerThrust(true)
        assertTrue(vm.currentState.thrust);vm.setActive(false);assertFalse(vm.currentState.thrust)
        vm.setActive(true);vm.tick(.2);assertEquals(0.0,vm.currentState.thrustCommand,0.0)
    }
    @Test fun staleGreenForecastCannotAuthorizeUnsafeHandleRelease() {
        val vm=ready();vm.confirmPlacement();vm.adjustLaunchSpeed(.05);vm.tick(.26)
        assertTrue(vm.currentState.canLaunch)
        vm.setAiming(true);vm.setLaunchVelocity(vm.launchVelocity()*.5);vm.setAiming(false)
        assertTrue(vm.currentState.predictionPending);assertFalse(vm.currentState.canLaunch)
        vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)
        vm.tick(.26);assertEquals(OrbitQuality.IMPACT,vm.currentState.prediction?.quality)
        assertFalse(vm.currentState.canLaunch)
    }
    @Test fun repeatedFineControlsShareTheQuarterSecondForecastBudget() {
        val vm=ready();vm.confirmPlacement();vm.adjustLaunchSpeed(.05);vm.tick(.26)
        val before=vm.currentState.prediction
        repeat(8){vm.adjustLaunchSpeed(.01)}
        assertSame(before,vm.currentState.prediction);assertTrue(vm.currentState.predictionPending)
        vm.tick(.26);assertNotSame(before,vm.currentState.prediction)
        assertFalse(vm.currentState.predictionPending)
    }
}
