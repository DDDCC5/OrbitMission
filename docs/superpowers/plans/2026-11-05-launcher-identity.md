# Launcher identity implementation plan

**Goal:** Apply the user-approved dark rocket/orbit symbol to installed app icons and localize the launcher name.

**Architecture:** Resource-only patch; preserve Stage, runtime scenes and Compose. Keep the scaffold circular spatial SDF mask as a single layer, repaint its texture using the approved emblem, and remove the default foreground layer. Provide Android adaptive and round icons from the same vector source. Default app name is English; all `zh` locales use exactly `轨道使命`.

**Tech stack:** Android resource XML, existing SVG, resvg rasterization, Gradle/aapt2.

- [x] Add approved SVG source and repeatable icon resource generator; preserve the scaffold circular SDF mask.
- [x] Replace launcher resources and spatial icon arrays; add `values-zh/strings.xml`; bump to 1.3.1 / 5.
- [x] Assemble debug APK, inspect packaged Chinese/English labels and icons, check signature and write delivery checksum.

User approval: 2026-11-05, approved displayed emblem-only navy preview and localized launcher names. No runtime feature changes or device install requested in this patch.
