package com.example.orbitmission.domain.usecase

import com.example.orbitmission.domain.model.Vector3D
import org.junit.Assert.*
import org.junit.Test

class OrbitAssistUseCaseTest {
    private val assist=OrbitAssistUseCase()
    @Test fun `assist tolerance scales with tiny physical display globes`() {
        val factor=.01
        val result=assist.snap(Vector3D(.87,.03,.04)*factor,Vector3D(0.0,0.0,2.0)*factor,Vector3D(.9,0.0,-2.0)*factor,.9*factor)
        assertEquals(.009,result.x,1e-10);assertEquals(0.0,result.y,1e-10)
        val far=Vector3D(.05,0.0,0.0)
        assertEquals(far,assist.snap(far,Vector3D(0.0,0.0,2.0),Vector3D(.009,0.0,-2.0),.009))
    }
    @Test fun `only gaze at nearby annulus locks grab`() {
        val point=Vector3D(.87,.03,.04)
        assertEquals(Vector3D(.9,0.0,0.0),assist.snap(point,Vector3D(0.0,0.0,2.0),Vector3D(.9,0.0,-2.0),.9))
        assertEquals(point,assist.snap(point,Vector3D(0.0,0.0,2.0),Vector3D(0.0,0.0,-1.0),.9))
        assertEquals(point,assist.snap(point,Vector3D(0.0,0.0,2.0),Vector3D(-.9,0.0,-2.0),.9))
    }
    @Test fun `parallel backward or distant gaze cannot move rocket`() {
        val point=Vector3D(.9,0.0,0.0)
        for(ray in listOf(Vector3D(1.0,0.0,0.0),Vector3D(.9,0.0,2.0)))
            assertEquals(point,assist.snap(point,Vector3D(0.0,0.0,2.0),ray,.9))
        val far=Vector3D(2.0,0.0,0.0)
        assertEquals(far,assist.snap(far,Vector3D(0.0,0.0,2.0),Vector3D(.9,0.0,-2.0),.9))
    }
}
