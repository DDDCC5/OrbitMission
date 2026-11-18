package com.example.orbitmission.domain.usecase
import com.example.orbitmission.domain.model.Vector3D
import org.junit.Assert.*
import org.junit.Test
class PinchVelocityUseCaseTest {
    private val tip=Vector3D(.2,1.6,-2.1)
    private fun sample(hand:String="right",strength:Double=1.0,dx:Double=0.0,dy:Double=0.0)=PinchSample(hand,Vector3D(dx,1.3+dy,-.4),strength,Vector3D(0.0,1.3,0.0),(tip-Vector3D(0.0,1.3,0.0)).normalized())
    @Test fun handRayTargetsWithoutEyeGazeAndPinchDoesNotSnap() {
        val c=PinchVelocityUseCase();val first=c.update(listOf(sample()),tip,.016)
        assertTrue(first.started);assertNull(first.tip)
        var r=first;repeat(20){r=c.update(listOf(sample(dx=.1,dy=.1)),tip,.016)}
        assertTrue(r.tip!!.x>tip.x+.14);assertTrue(r.tip!!.y>tip.y+.14)
        assertEquals(tip.z,r.tip!!.z,0.0)
        assertTrue(c.update(listOf(sample(strength=0.0)),tip,.016).released)
    }
    @Test fun ownerIsStickyAndMissingHandCancelsUntilItOpens() {
        val c=PinchVelocityUseCase();c.update(listOf(sample("left")),tip,.016)
        assertEquals("left",c.update(listOf(sample("right"),sample("left",dx=.1)),tip,.016).activeHand)
        assertTrue(c.update(emptyList(),tip,.016).cancelled)
        assertNull(c.update(listOf(sample("left")),tip,.016).activeHand)
        c.update(listOf(sample("left",0.0)),tip,.016)
        assertTrue(c.update(listOf(sample("left")),tip,.016).started)
    }
    @Test fun pinchJitterAndOffTargetPinchesCannotGrab() {
        val c=PinchVelocityUseCase();assertNull(c.update(listOf(sample(strength=.6)),tip,.016).activeHand)
        c.update(listOf(sample()),tip,.016)
        assertEquals("right",c.update(listOf(sample(strength=.5)),tip,.016).activeHand)
        val wrong=PinchSample("left",Vector3D(0.0,0.0,0.0),1.0)
        val other=PinchVelocityUseCase();assertNull(other.update(listOf(wrong),tip,.016).activeHand)
    }
    @Test fun resetRequiresAReleaseBeforeRearming() {
        val c=PinchVelocityUseCase();c.reset();assertNull(c.update(listOf(sample()),tip,.016).activeHand)
        c.update(listOf(sample(strength=0.0)),tip,.016);assertTrue(c.update(listOf(sample()),tip,.016).started)
    }
    @Test fun lossDoesNotHandOwnershipToAnotherAlreadyPinchedHand() {
        val c=PinchVelocityUseCase();c.update(listOf(sample("left")),tip,.016)
        c.update(listOf(sample("left"),sample("right")),tip,.016)
        assertTrue(c.update(listOf(sample("right")),tip,.016).cancelled)
        assertNull(c.update(listOf(sample("right")),tip,.016).activeHand)
        c.update(listOf(sample("right",0.0)),tip,.016)
        assertTrue(c.update(listOf(sample("right")),tip,.016).started)
    }
    @Test fun completeTrackingLossRequiresEitherHandToOpenBeforeReacquisition() {
        val c=PinchVelocityUseCase();c.update(listOf(sample("left")),tip,.016)
        c.update(emptyList(),tip,.016)
        assertNull(c.update(listOf(sample("right")),tip,.016).activeHand)
        c.update(listOf(sample("right",0.0)),tip,.016)
        assertTrue(c.update(listOf(sample("right")),tip,.016).started)
    }
}
