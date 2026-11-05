package com.example.orbitmission.ui.mission

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.orbitmission.data.repository.BundledMissionRepository
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.domain.usecase.MissionSessionUseCase
import org.json.JSONObject

class MissionViewModelFactory(private val context:Context):ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T:ViewModel> create(modelClass:Class<T>):T {
        val p=context.getSharedPreferences("mission",Context.MODE_PRIVATE)
        val initial=MissionPreferences(language=AppLanguage.entries.firstOrNull {it.tag==p.getString("language",null)}?:AppLanguage.ZH_HANS,
            tutorialSeen=p.getBoolean("tutorial",false),music=p.getBoolean("music",true),voice=p.getBoolean("voice",true),sfx=p.getBoolean("sfx",true))
        val strings=AppLanguage.entries.associateWith {language->
            val json=JSONObject(context.assets.open("locales/${language.tag}.json").bufferedReader().use {it.readText()})
            json.keys().asSequence().associateWith {json.getString(it)}
        }
        val repository=BundledMissionRepository(strings,initial){v->p.edit().putString("language",v.language.tag).putBoolean("tutorial",v.tutorialSeen)
            .putBoolean("music",v.music).putBoolean("voice",v.voice).putBoolean("sfx",v.sfx).apply()}
        return MissionViewModel(MissionSessionUseCase(repository)) as T
    }
}
