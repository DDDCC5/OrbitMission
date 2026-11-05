package com.example.orbitmission.domain.usecase
import com.example.orbitmission.domain.model.*
import com.example.orbitmission.data.repository.BundledMissionRepository
import org.junit.Assert.*
import org.junit.Test
class MissionSessionUseCaseTest {
 @Test fun `traditional text stays traditional while voice resolves to shared chinese`() {
  val usecase=MissionSessionUseCase(BundledMissionRepository(mapOf(AppLanguage.ZH_HANS to mapOf("select" to "请选择"),AppLanguage.ZH_HANT to mapOf("select" to "請選擇"))))
  assertEquals("請選擇",usecase.text(AppLanguage.ZH_HANT,"select"));assertEquals(AppLanguage.ZH_HANS.voiceFolder,AppLanguage.ZH_HANT.voiceFolder)
 }
 @Test fun `preferences are persisted and missing text has english fallback`() {
  var saved:MissionPreferences?=null
  val usecase=MissionSessionUseCase(BundledMissionRepository(mapOf(AppLanguage.EN to mapOf("retry" to "Try again")),persist={saved=it}))
  val next=MissionPreferences(language=AppLanguage.EN,tutorialSeen=true,music=false);usecase.save(next)
  assertEquals(next,saved);assertEquals(next,usecase.preferences());assertEquals("Try again",usecase.text(AppLanguage.ZH_HANT,"retry"));assertEquals("missing",usecase.text(AppLanguage.EN,"missing"))
 }
}
