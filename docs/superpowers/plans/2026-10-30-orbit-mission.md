# Orbit Mission Implementation Plan

**Goal:** Implement the approved immersive solar-system orbit mission and deliver a debug APK with verified runtime evidence.
**Architecture:** Official PICO Stage scaffold, native Spatial SDK ECS 3D assets, Compose attachment instruments under PicoTheme. Pure Kotlin SI-unit simulation, MVI-lite ViewModel, resource repository and lifecycle-owned input/audio services.
**Tech Stack:** Kotlin, Compose, PICO Spatial 6.1, GLB assets, public ECS/input/audio APIs.
**Spec:** User's original requirements, approved `ASSET_REVIEW.md`, `assets/ui/screens/`; Chinese voices shared from zh-Hans as explicitly approved.

## Constraints and rulings
- Package `com.example.orbitmission`; reuse current workspace and approved assets. No existing Git checkout to isolate.
- Stage FULL, 3D ECS content; existing failed Editor authoring is documented. Approved local GLB assets are integrated directly.
- Custom angular UI is explicitly required and overrides generic native-style component preferences; retain PicoTheme, public SpatialUI Text, hover and haptics.
- Chinese UI uses separate Simplified/Traditional strings; both play zh-Hans voices. English uses en.
- Physics in SI, separate render scale and simulation time multiplier. Sum Sun and target gravity; Earth surface acceleration is a check, not a mass-only approximation.
- Never claim eye tracking or physical controller verified from emulator-only evidence.

## Review focus
- Zero-distance / NaN state never propagates into scene transforms.
- A path crossing a body between time steps counts as collision.
- Success requires a stable full revolution, with partial progress invalidated after unstable flight.
- Input/audio/coroutines stop on lifecycle pause and release on dispose.
- Language change uses correct display text and Chinese shared audio mapping.

## Tasks
- [x] 1. CLI scaffold and baseline build/launch; record onboarding evidence. Verify generated toolchain and SDK.
- [x] 2. Pure simulation model and meaningful unit tests: inverse-square acceleration, circular orbit energy/eccentricity, one revolution, collision, timeout and boundary. Domain consumes Planet and Vector3D, exports FlightSnapshot via GravitySimulationUseCase.
- [x] 3. Repository, language/audio mapping and mission ViewModel. Test startup, selection, placement, retry, other planet, language sharing and pause semantics.
- [x] 4. Scene integration: load all ten GLBs, loading/error handling, transforms, animated opening and solar particles, saturation and real-period scaled spin, orbit guide/path and rocket velocity orientation. Wire public input APIs after source verification.
- [x] 5. Custom Compose attachment UI: logo/tutorial/instruction, telemetry/countdown, settings and results; all three text languages with shared Chinese voice.
- [x] 6. Integrate input and spatial audio with cleanup, build/tests/style/architecture gates, install/launch/logs/screenshots, independently review physics and lifecycle; fix concrete findings.
- [x] 7. Copy verified APK and screenshots to output, document controls, physical scaling, tested target and remaining device-only verification.
