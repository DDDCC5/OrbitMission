package com.example.orbitmission.ui.mission

import java.lang.ref.WeakReference

/** In-process instrumentation seam. No exported receiver, network endpoint or test controls in UI. */
internal object MissionRuntimeHandle {
    private var reference=WeakReference<MissionViewModel>(null)
    fun attach(model:MissionViewModel){reference=WeakReference(model)}
    fun detach(model:MissionViewModel){if(reference.get()===model)reference.clear()}
    fun current():MissionViewModel?=reference.get()
}
