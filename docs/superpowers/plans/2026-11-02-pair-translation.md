# Pair translation repair / 1.2

User removes all solar corona attachments and replaces focus teleport/enlargement with common translation of the Sun and selected planet. Interpret the first reference to Earth as Sun, consistent with the rest of the request. Preserve the existing overview, current near panels and physical celestial size ratios.

The selected globe and Sun receive exactly the same 1.2 second eased translation. The selected globe ends at (0, 1.55, -2.1) Stage meters; neither globe changes radius, both keep y=1.55, their separation vector stays unchanged. Other planets stay at their overview positions and fade to zero over that same interval. Returning reverses from current visible positions to the original layout and restores all eight opacities. Block new selection/launch until motion settles; pause/settings freeze it. Repeated selection/return must never accumulate offset.

1. Implement and test pure transition state (all eight targets, fade, exact return, interruption, invalid time).
2. Wire scene transforms and dynamic flight origin/radius; remove all corona nodes/materials/animation and obsolete magnified-view copy. Preserve the SI physics engine. Scale rocket/path markers down with the selected globe and retain usable invisible ray hit areas and explicit launch control.
3. Test transition-aware ViewModel interaction gates and native selection/flight/return cycles.
4. Managed scene build and normal machine gates; install 1.2 on connected PICO; inspect real screenshots; update APK/source delivery.

Small terrestrial planets intentionally remain small. No independent target or Sun enlargement is introduced.
