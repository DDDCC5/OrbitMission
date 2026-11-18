# Interactive flight 1.4.0 implementation plan

Goal: turn launch-and-watch into place, aim, preview, launch and correct. User approved the recommended first iteration on 2026-11-18.

Design: retain existing Full Stage, four attachment panels, native bodies, identity and angular aerospace UI. Controls panel adds guided/challenge mode, placement confirmation, speed +/- and direction +/- fine controls, prediction status, explicit launch, prograde/retrograde controls. Prompt panel shows action-based numbered steps and failure guidance. Native scene adds reusable predicted-path markers, a velocity arrow and a grabbable velocity endpoint. Rocket release confirms placement; arrow release confirms aim; only explicit confirm launches. No new artwork or SDK upgrade.

Guided starts at 88% circular speed and requires placement plus a speed/direction adjustment and a safe preview before launch. Challenge starts at 70%, permits risky explicit launches, and requires the player to correct it. Reset clears thrust, grip and aim; retries preserve mode and useful launch settings. Settings/background freeze simulation and cancel active input.

Physics: keep SI two-source acceleration, use the existing integrator for prediction, add bounded signed thrust (-1 retrograde, +1 prograde), and add osculating periapsis/apoapsis/risk analysis. Stop prediction on collision, escape, full revolution or bounded horizon. Prediction throttled to 4 Hz with <=96 path points and a display bound; trajectory data never mutates live flight. Show indicative predictions, not guaranteed success.

Tasks:
1. Add pure launch planning, risk/prediction and signed-thrust tests; implement and prove dangerous/safe cases across all planets and live-state independence.
2. Add ViewModel mode/lesson/placement/aim/thrust state and tests. Manual + controller command reconciliation must stop on pause/settings/phase changes.
3. Add native pooled trajectory/arrow/handle using existing public Entity APIs, managed codegen handoff, and existing controller rays. No new provider methods.
4. Update existing controls and prompts with three-language text; preserve custom styling and current panel locations.
5. Build via managed workflow, run unit and native instrumentation, validate UI/architecture, install and capture native evidence where available, package 1.4.0 / code 7. Keep current 1.3.2 release intact until new build is verified.

Ruling: Guided starts at 88% instead of the first draft 92%, because 92% is already a stable orbit at the recommended radius. A +5% adjustment from 88% visibly changes the unsafe forecast to safe.
