package com.example.orbitmission.ui.mission

import com.example.orbitmission.data.repository.BundledMissionRepository
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.MissionSessionUseCase
import org.junit.Assert.*
import org.junit.Test

class MissionViewModelTest {
    private fun model(tutorialSeen:Boolean=true)=MissionViewModel(MissionSessionUseCase(BundledMissionRepository(emptyMap(), MissionPreferences(tutorialSeen=tutorialSeen))))
    @Test fun `startup honors five second splash and ordered assembly`() {
        val vm=model();vm.assetsReady();assertEquals(MissionPhase.SPLASH,vm.uiState.value.phase)
        vm.tick(4.9);assertEquals(MissionPhase.SPLASH,vm.uiState.value.phase)
        vm.tick(.2);assertEquals(MissionPhase.ASSEMBLING,vm.uiState.value.phase)
        vm.tick(6.0);assertEquals(MissionPhase.SELECTING,vm.uiState.value.phase)
    }
    @Test fun `all planets select launch retry without retaining old flight`() {
        val vm=model();vm.assetsReady();vm.tick(5.1);vm.tick(6.0)
        for(p in Planet.entries) {
            vm.selectPlanet(p);assertEquals(MissionPhase.PLACEMENT,vm.uiState.value.phase)
            vm.tick(1.2);vm.launch();vm.tick(1.0);assertTrue(vm.uiState.value.flight!!.elapsedSeconds>0)
            vm.retry();assertEquals(MissionPhase.PLACEMENT,vm.uiState.value.phase)
            assertEquals(0.0,vm.uiState.value.flight!!.elapsedSeconds,0.0)
            vm.chooseOtherPlanet();assertEquals(MissionPhase.SELECTING,vm.uiState.value.phase);vm.tick(1.2)
        }
    }
    @Test fun `settings freezes clock and changing language shares chinese voice`() {
        val vm=model();vm.assetsReady();vm.tick(5.1);vm.tick(6.0);vm.selectPlanet(Planet.EARTH);vm.tick(1.2);vm.launch()
        vm.toggleSettings();vm.tick(40.0);assertEquals(0.0,vm.uiState.value.flight!!.elapsedSeconds,0.0)
        vm.setLanguage(AppLanguage.ZH_HANT);assertEquals("zh-Hans",vm.uiState.value.language.voiceFolder)
        vm.toggleSettings();vm.tick(1.0);assertTrue(vm.uiState.value.flight!!.elapsedSeconds>0)
    }
    @Test fun `first launch requires tutorial and selection ignores stale hover`() {
        val vm=model(false);vm.assetsReady();vm.tick(5.1);vm.tick(6.0);assertEquals(MissionPhase.TUTORIAL,vm.uiState.value.phase)
        vm.finishTutorial();vm.selectPlanet(Planet.MARS);vm.chooseOtherPlanet()
        assertNull(vm.uiState.value.selectedPlanet);assertNull(vm.uiState.value.hoveredPlanet)
    }
    @Test fun `background pause does not consume mission clock`() {
        val vm=model();vm.assetsReady();vm.tick(5.1);vm.tick(6.0);vm.selectPlanet(Planet.EARTH);vm.tick(1.2);vm.launch();vm.setActive(false);vm.tick(91.0)
        assertEquals(MissionPhase.FLYING,vm.uiState.value.phase);assertEquals(90.0,vm.uiState.value.flight!!.remainingSeconds,1e-8)
    }
    @Test fun `focus and return animations block launch and reselection until settled`() {
        val vm=model();vm.assetsReady();vm.tick(5.1);vm.tick(6.0)
        vm.selectPlanet(Planet.EARTH);assertTrue(vm.currentState.sceneTransitioning)
        vm.launch();assertEquals(MissionPhase.PLACEMENT,vm.currentState.phase)
        vm.tick(1.2);assertFalse(vm.currentState.sceneTransitioning);vm.launch()
        assertEquals(MissionPhase.FLYING,vm.currentState.phase)
        vm.chooseOtherPlanet();vm.selectPlanet(Planet.MARS);assertNull(vm.currentState.selectedPlanet)
        vm.tick(1.2);vm.selectPlanet(Planet.MARS);assertEquals(Planet.MARS,vm.currentState.selectedPlanet)
    }
    @Test fun `pause and settings preserve transition progress`() {
        val vm=model();vm.assetsReady();vm.tick(5.1);vm.tick(6.0);vm.selectPlanet(Planet.EARTH)
        vm.tick(.4);vm.setActive(false);vm.tick(30.0);assertTrue(vm.currentState.sceneTransitioning)
        vm.setActive(true);vm.toggleSettings();vm.tick(30.0);assertTrue(vm.currentState.sceneTransitioning)
        vm.toggleSettings();vm.tick(.4);assertTrue(vm.currentState.sceneTransitioning)
        vm.tick(.5);assertFalse(vm.currentState.sceneTransitioning)
    }
}
