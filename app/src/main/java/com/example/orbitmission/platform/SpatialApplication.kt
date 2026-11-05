package com.example.orbitmission.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.orbitmission.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
