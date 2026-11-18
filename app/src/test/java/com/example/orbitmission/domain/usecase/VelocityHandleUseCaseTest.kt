package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import org.junit.Assert.*
import org.junit.Test

class VelocityHandleUseCaseTest {
    @Test fun pointerDragSetsBothSpeedAndDirectionInTheRocketPlane() {
        val rocket=Vector3D(0.0,1.5,-2.1);val origin=Vector3D(0.0,1.5,0.0)
        val up=VelocityHandleUseCase.drag(origin,Vector3D(0.0,.22,-2.1).normalized(),rocket,8000.0)!!
        val right=VelocityHandleUseCase.drag(origin,Vector3D(.11,0.0,-2.1).normalized(),rocket,8000.0)!!
        assertEquals(8000.0,up.y,1e-6);assertEquals(0.0,up.x,1e-6)
        assertEquals(4000.0,right.x,1e-6);assertEquals(0.0,right.y,1e-6)
    }
    @Test fun invalidBehindAndParallelRaysCannotChangeVelocity() {
        val rocket=Vector3D(0.0,0.0,-2.1);val origin=Vector3D(0.0,0.0,0.0)
        assertNull(VelocityHandleUseCase.drag(origin,Vector3D(1.0,0.0,0.0),rocket,8000.0))
        assertNull(VelocityHandleUseCase.drag(origin,Vector3D(0.0,0.0,1.0),rocket,8000.0))
        assertNull(VelocityHandleUseCase.drag(origin,Vector3D(Double.NaN,0.0,-1.0),rocket,8000.0))
    }
}
