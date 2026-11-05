package com.example.orbitmission.data.repository

import com.example.orbitmission.domain.model.*

interface MissionRepository {
    fun readPreferences():MissionPreferences
    fun savePreferences(preferences:MissionPreferences)
    fun text(language:AppLanguage,key:String):String
}
class BundledMissionRepository(private val translations:Map<AppLanguage,Map<String,String>>,
    initial:MissionPreferences=MissionPreferences(), private val persist:(MissionPreferences)->Unit={}) : MissionRepository {
    private var preferences=initial
    override fun readPreferences()=preferences
    override fun savePreferences(preferences:MissionPreferences) {this.preferences=preferences;persist(preferences)}
    override fun text(language:AppLanguage,key:String)=translations[language]?.get(key)
        ?:translations[AppLanguage.EN]?.get(key)?:key
}
