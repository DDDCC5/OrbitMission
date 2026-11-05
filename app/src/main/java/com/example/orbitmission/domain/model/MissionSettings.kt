package com.example.orbitmission.domain.model

enum class AppLanguage(val tag:String,val voiceFolder:String) {
    ZH_HANS("zh-Hans","zh-Hans"), ZH_HANT("zh-Hant","zh-Hans"), EN("en","en")
}
data class MissionPreferences(val language:AppLanguage=AppLanguage.ZH_HANS,val tutorialSeen:Boolean=false,
    val music:Boolean=true,val voice:Boolean=true,val sfx:Boolean=true)
enum class MissionPhase { LOADING, SPLASH, ASSEMBLING, TUTORIAL, SELECTING, PLACEMENT, FLYING, SUCCESS, FAILURE, ERROR }
