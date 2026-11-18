package com.example.orbitmission.domain.usecase
import com.example.orbitmission.domain.model.Vector3D
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
class ColdStartAnchorUseCaseTest {
    @Test fun logoAndPlanetAreInFrontForStandingSeatedAndRotatedUsers() {
        for(height in listOf(.85,1.55,1.9))for(yaw in listOf(0.0,PI/2,PI,-PI/2)){
            val head=Vector3D(2.0,height,-3.0);val forward=Vector3D(-sin(yaw),0.0,-cos(yaw))
            val useCase=ColdStartAnchorUseCase();repeat(14){useCase.observe(head,forward,.1)}
            val anchor=useCase.anchor!!
            for(distance in listOf(1.15,2.1)){
                val actual=anchor.point(Vector3D(0.0,1.55,-distance));val expected=head+forward*distance
                assertEquals(expected.x,actual.x,1e-8);assertEquals(expected.y,actual.y,1e-8);assertEquals(expected.z,actual.z,1e-8)
            }
        }
    }
    @Test fun doesNotShowAtOriginBeforeValidTrackingOrFollowLaterHeadMovement() {
        val c=ColdStartAnchorUseCase();repeat(15){assertNull(c.observe(null,null,.1))}
        val head=Vector3D(0.0,1.1,0.0);val forward=Vector3D(0.0,0.0,-1.0)
        repeat(4){c.observe(head,forward,.1)};val a=c.anchor!!
        assertEquals(a,c.observe(Vector3D(9.0,1.9,9.0),Vector3D(1.0,0.0,0.0),.1))
    }
    @Test fun unsettledOrInvalidPoseCannotLatch() {
        val c=ColdStartAnchorUseCase();repeat(20){assertNull(c.observe(Vector3D(it.toDouble(),1.0,0.0),Vector3D(0.0,0.0,-1.0),.1))}
        assertNull(c.observe(Vector3D(Double.NaN,0.0,0.0),Vector3D(0.0,0.0,-1.0),.1))
    }
    @Test fun aFrozenProviderSampleCannotPretendToBeSettledTracking() {
        val c=ColdStartAnchorUseCase();val head=Vector3D(0.0,1.2,0.0);val forward=Vector3D(0.0,0.0,-1.0)
        repeat(50){assertNull(c.observe(head,forward,.016,1L))}
        repeat(20){c.observe(head,forward,.016,it+2L)}
        assertNotNull(c.anchor)
    }
}
