package com.example.orbitmission.domain.usecase

import com.example.orbitmission.data.repository.MissionRepository
import com.example.orbitmission.domain.model.*

class MissionSessionUseCase(private val repository:MissionRepository) {
    fun preferences()=repository.readPreferences()
    fun save(preferences:MissionPreferences)=repository.savePreferences(preferences)
    fun text(language:AppLanguage,key:String)=repository.text(language,key)
}
