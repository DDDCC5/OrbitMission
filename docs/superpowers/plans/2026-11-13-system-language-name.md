# System-language app identity repair

Goal: Chinese system locales show only 轨道使命; English locales show only Orbit Mission, including the launch entry and opening identity. Release 1.3.2 / versionCode 6.

Root cause: Launcher label resources were localized in 1.3.1, but MissionLogo still rendered the original baked bilingual bitmap and used a bilingual accessibility description. Loading identity also followed the independently saved in-app language rather than Android system resources.

Scope: Keep the accepted emblem and spatial window layout. Replace the bilingual image with the text-free emblem plus one system-resource title; use the same resource for loading identity and explicit launcher Activity label. Preserve in-app language settings for mission content. No physics/scene changes.

Verification: Add Android instrumentation covering Chinese CN/TW/HK/SG and English US/GB application/launcher resources, and the absence of legacy bilingual drawable. Assemble, JVM tests, targeted device instrumentation, APK locale/signature inspection, then publish source and 1.3.2 APK to existing public GitHub repository. Return fixed release commit and Vibe Lab update copy.
