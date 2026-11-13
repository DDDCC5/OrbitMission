package com.example.orbitmission

import android.content.ComponentName
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIdentityTest {
    @Test
    fun applicationAndLauncherNamesFollowSystemLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activity = context.packageManager.getActivityInfo(
            ComponentName(context.packageName, "${context.packageName}.platform.LaunchActivity"), 0)
        val labelRes = if (activity.labelRes != 0) activity.labelRes else context.applicationInfo.labelRes
        for (tag in listOf("zh-CN", "zh-TW", "zh-HK", "zh-SG", "en-US", "en-GB")) {
            val config = Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(tag))
            }
            val localized = context.createConfigurationContext(config)
            val expected = if (tag.startsWith("zh")) "轨道使命" else "Orbit Mission"
            assertEquals(tag, expected, localized.getString(context.applicationInfo.labelRes))
            assertEquals("launcher $tag", expected, localized.getString(labelRes))
        }
    }

    @Test
    fun bilingualOpeningArtworkIsNotPackaged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("The old bitmap contains both names and must never reach the APK", 0,
            context.resources.getIdentifier("orbit_logo", "drawable", context.packageName))
    }
}
