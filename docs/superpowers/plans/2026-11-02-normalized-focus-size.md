# Normalized selected globe size / 1.3

User accepts the existing selection transition, requests every selected planet have five times the current overview Jupiter **volume**, with Sun enlarged by the selected globe's same linear factor. User explicitly approved expanding Sun–planet spacing by that factor to prevent overlap.

Reference is frozen overview Jupiter radius, not the newly enlarged Jupiter. Target radius = overview Jupiter radius × cubeRoot(5), about 0.091375 m; diameter about 0.18275 m. Sphere volume is intended, excluding Saturn rings. Each selected globe gets that exact radius; Sun and pair separation use `targetRadius / originalPlanetRadius`. Interpolate both radii and separation through the existing 1.2 s easing and preserve horizontal alignment. Other planets remain at their original overview sizes/positions and fade as before. Return restores every original radius and position without accumulating scale.

1. Tests verify common selected volume, proportional Sun and spacing at every intermediate step, uninterrupted reverse, and exact repeated reset.
2. Keep physics in SI and use rendered focus radius for rocket, guide and display conversion.
3. Update localized scale notice, version, managed scene build and validation.
4. Native focus/return smoke, recording/snapshots, install on connected PICO, refresh APK and source archive.
