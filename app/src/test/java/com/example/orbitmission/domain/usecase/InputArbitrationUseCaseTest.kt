package com.example.orbitmission.domain.usecase
import com.example.orbitmission.domain.model.Planet
import org.junit.Assert.*
import org.junit.Test
class InputArbitrationUseCaseTest {
 @Test fun `controller and gaze disagreement never confirms hidden gaze target`() {
  val input=InputArbitrationUseCase()
  repeat(30){assertNull(input.target(Planet.EARTH,Planet.MARS,.1).confirmed)}
  val s=input.target(Planet.EARTH,Planet.MARS,.1);assertEquals(Planet.EARTH,s.hovered);assertEquals(0f,s.progress,0f)
 }
 @Test fun `gaze needs continuous two seconds and changing target resets dwell`() {
  val input=InputArbitrationUseCase();repeat(15){input.target(null,Planet.EARTH,.1)}
  assertNull(input.target(null,Planet.MARS,.1).confirmed)
  var result=input.target(null,Planet.MARS,.1);repeat(20){result=input.target(null,Planet.MARS,.1)}
  assertEquals(Planet.MARS,result.confirmed)
 }
 @Test fun `loss or changed controller cancels grip rather than releasing rocket`() {
  val input=InputArbitrationUseCase();input.beginGrab("right")
  assertEquals(GrabAction.CANCEL,input.grab(null,false));input.beginGrab("right")
  assertEquals(GrabAction.CANCEL,input.grab("left",false));input.beginGrab("right")
  assertEquals(GrabAction.RELEASE,input.grab("right",false))
 }
 @Test fun `pause discards held grip and dwell`() {
  val input=InputArbitrationUseCase();input.beginGrab("right");input.target(null,Planet.EARTH,1.9);input.reset()
  assertEquals(GrabAction.NONE,input.grab("right",false));assertNull(input.target(null,Planet.EARTH,.2).confirmed)
 }
}
