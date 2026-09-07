# Interstellar Map Rendering Improvement Ideas

## Objective

Build on the retained Java2D renderer toward three independent outcomes:

1. Reduce remaining paint work, cache regeneration, EDT delay, and allocation cost where measurement supports a change.
2. Restore restrained JumpShip navigation lights if they have no significant performance or responsiveness impact.
3. Restore a sparse, visibly smooth pulse on the active jump path if it has no significant performance or responsiveness impact.

The central rule is to animate a small amount of changing content, not repeatedly recompute or repaint the entire map to animate a few pixels. Preserve the accepted semantic zoom, intrinsic stars, crisp rings, and navigation information.

## Scope and Status (2026-09-05)

This is an optional improvement backlog, not a replacement for the [original performance plan](interstellar-map-rendering-performance-plan.md). It records follow-up implementation status and measurement summaries. The original remains the canonical status and historical record for the ringless experiments, restored detail rendering, and Phase 3 architecture.

- Retained cartography, system-art, and navigation caches, spatial indexing, exposed-strip refresh, and asynchronous cartography preparation are already present.
- The controlled `pan-v2` dense-medium baseline has a 6.4 ms median average, 7.1 ms median p95, 7.8 ms median p99, 9.4 ms median run maximum, and no frame above 16 ms across three runs with approximately 583 visible systems.
- The controlled post-fix detail baseline has a 7.9 ms median average, 8.5 ms median p95, 10.2 ms median p99, 13.8 ms median run maximum, and no frame above 16 ms across three runs with 167 live-query systems. Left-edge popping was not observed after the query fix.
- The controlled atlas baseline has a 7.0 ms median average, 7.8 ms median p95, 8.5 ms median p99, 10.3 ms median run maximum, and no frame above 16 ms across three runs with 1,229 live-query systems.
- The pre-optimization detail-view mode transition had a 449 ms median wall time, 22.4 ms median average paint, 44.3 ms median p95/p99/maximum, and two paints above 33 ms per run.
- The final retained transition verification has a 396 ms median wall time, 14.3 ms median average paint, 13.6 ms median p50, 27.1 ms median p95/p99/maximum, and no paint above 33 ms. Animation cache-hit frames had 14.2-14.4 ms maxima; cold preparation accounts for the remaining aggregate tail. Same-view regeneration has a 94 ms median wall time and 17.7 ms median average, with one 39.7 ms first-run outlier.
- These results are map paint durations, not delivered FPS or end-to-end input latency. They are repeatable development-machine measurements; external Mac timing is deferred and non-blocking, while the target user's positive panning report remains qualitative evidence.
- Deterministic `pan-v2` camera playback, separate `transition-cold-v1` mode-transition/cache-regeneration measurement, and cursor-anchored `zoom-v1` measurement are implemented. Retained endpoint crossfading is implemented. Active zoom keeps systems/navigation live and now composites the last compatible cartography snapshot directly while an exact replacement prepares. This removed all active full fallback renders and reduced the three-run median average/p95/maximum to 12.6/21.0/22.6 ms without visual defects or frames above 33 ms. A focused phase split attributes 10.0 ms of each 17.1 ms new-scale frame to bilinear snapshot scaling; JFR confirms software `TransformHelper` work and 30.6 MiB background exact-raster allocations. A pixel-identical source-clipping probe did not improve transform time and was removed. Further zoom work would require a fidelity tradeoff or platform-sensitive acceleration, so visual/HiDPI acceptance, first-render analysis, regeneration follow-up, and the approximately 50% local warm-pan stretch target now take priority.
- Continuous star shimmer, HPG packets, route pulses, and ship lights are currently removed. Finite selection, route activation, and actual jump transitions remain.
- Track 0 now has an initial playback harness plus dense, detail, and atlas baselines. Track 1 has an initial presentation snapshot included in those current-renderer measurements, but no paired run isolates its individual delta. The remaining work is independently gated and does not form a commitment to implement the entire document.

This backlog revisits only the original prohibition on continuous ship lights and active-route pulses, as bounded experiments requested for visual evaluation and subject to acceptance. It does not authorize restoring star shimmer, HPG packets, or unrestricted ambient animation.

Stay on the original Phase 3 Java2D architecture unless new evidence meets its escalation gates. Missing optimizations alone do not justify tiles, pipeline changes, or a GPU rewrite.

## Decision Rule

- Performance improvements may ship without either visual effect when they produce a repeatable benefit and preserve correctness.
- Ship lights and route pulses are accepted independently. Either effect may remain removed if its appearance, smoothness, lifecycle, or performance gate fails.
- Animation support must reuse the smallest correct rendering mechanism. Do not build a general partial-repaint framework merely because it is architecturally attractive; first prove that the real Swing pipeline can keep damage local.
- "No significant impact" means no statistically or operationally meaningful regression in paired deterministic runs, with the absolute targets still passing. Results that cannot distinguish the candidate from measurement noise are inconclusive, not automatic passes.

## Current Code Opportunities

The following observations come from [InterstellarMapPanel.java](../src/mekhq/gui/InterstellarMapPanel.java), not from a fresh profile. Measure their significance before assuming a particular millisecond saving.

| Surface | Observed behavior | Opportunity |
| --- | --- | --- |
| Main map paint pass | Processes viewport systems even when retained layers can be reused | Make localized repaint cost depend on damaged content, not viewport population |
| `buildStrategicMarkers`, `playerBaseCountsBySystem`, and restriction checks | Rebuild collections or evaluate campaign-dependent facts during painting | Prepare presentation data when its actual inputs change |
| `drawOutlinedTextWithAlpha` | Draws each string five times; suffixes add more text work | Retain ordinary labels at settled detail zoom |
| `RetainedCartographyKey` | Includes settings for layers beyond the territory/emblem content of the underlying cartography surface | Narrow each cache's dependencies without missing real invalidations |
| Map-mode transitions | Stable cartography/system art and previous/target overlays are staged as exact-scale retained surfaces; dynamic markers remain live | Consider asynchronous cold preparation only if repeated first-render or visual hitch evidence justifies its complexity |
| `canUseMergedNavigation` | Route plotting and activation temporarily leave the merged navigation path | Keep new ambient accents independent of these finite transition gates |

## Non-Negotiable Behavior

- Never hide systems, labels, routes, overlays, or controls merely because a pan or zoom is in progress.
- Keep intrinsic spectral stars and sharp ownership/analytical rings at detail zoom, with equal segmentation for shared factions.
- Preserve selection, hover, current location, route warnings and waypoints, capitals, operations, restrictions, GM overrides, bases, HPG, and navigation semantics.
- Hidden empty systems must not leave black circles, stale labels, or animation trails. Required navigation contacts remain available.
- Keep star shimmer and HPG information packets removed. Static HPG information remains unchanged.
- New animation must not change campaign state, route eligibility, travel timing, or the fleet's actual position.
- Animation phase must never invalidate cartography, system art, ordinary labels, or the static route merely because time advanced.
- Existing finite transitions remain finite and stop scheduling updates when settled.
- Provide a reduced-motion/off path that preserves all information with static visuals. Start restoration experiments disabled by default; change that default only after explicit visual and performance acceptance.
- Do not degrade detail or stop an otherwise visible effect specifically because the user is dragging. Visibility follows viewport and semantic zoom, not interaction-quality shortcuts.

## Performance Targets

Retain the original dense development-laptop targets after warm-up, at the same window size, date, map layer, route state, and fixed camera path:

- Approximately 500 visible systems at medium zoom, continuously panned for at least 10 seconds per run.
- Average full-map paint time below 10 ms.
- p95 full-map paint time below 16.7 ms.
- No warm interaction paint above 33 ms without an independently identified explanation. An allocation-driven GC regression is still a renderer regression, not an automatic exemption.
- No disappearing content, clipping, or input responsiveness regression.

Those are minimum guardrails. The next optimization stretch target is approximately a 50% reduction in the current three-run median average and p95 for each warm-pan scenario: dense medium 3.2/3.6 ms, detail 4.0/4.3 ms, and atlas 3.5/3.9 ms. External Mac benchmarking is deferred; use these deterministic local comparisons as the repeatable proxy without claiming hardware equivalence.

### Proposed Animation Budgets

These are initial acceptance budgets, not measured results or promises of zero overhead:

| Metric | Initial budget |
| --- | --- |
| Each effect and both effects enabled versus disabled during identical warm pan | Target a delta within measurement noise; reject above 0.5 ms in average or 1.0 ms in p95, and require the absolute targets to pass |
| Stationary-camera animation-only paint | p95 below 1.0 ms on the development laptop, including background restoration and intersecting content |
| Static cache regeneration caused solely by an animation tick | Zero |
| Scheduled ambient repaints with no visible enabled effect | Zero |
| Pulse smoothness | No visible stepping at accepted speed; target no more than one device pixel of travel between updates and verify on normal and high-refresh displays |
| Idle behavior | No permanent 60 Hz full-map repaint loop; a visible bounded overlay may update at display-suitable cadence. Record CPU, EDT time, and allocations against animation disabled |

Use deterministic camera playback for sub-millisecond comparisons. If measurement noise is larger than the permitted delta, extend the paired runs rather than declaring a pass. Report per-run results as well as medians across runs. Validate responsiveness through the real Swing display path and preserve the local guardrails before enabling effects by default.

## Improvement Tracks

### Track 0: Establish a Current Baseline

Status: the opt-in `pan-v2` harness replays a closed, elapsed-time path from the current camera using absolute whole-screen-pixel samples, so delayed callbacks cannot accumulate camera drift and retained layers follow the same translation rules as mouse panning. It performs a 5-second warm-up followed by one 30-second aggregate measurement, suppresses ordinary five-second profiler reports during that run, restores the exact starting camera, and cancels if the viewport changes, another map animation starts, the map is hidden, or the shortcut is pressed again. Dense-medium, detail, and atlas three-run baselines are recorded below. The opt-in `transition-cold-v1` harness reports map-mode transition and same-view exact-scale cache regeneration as separate phases and restores the starting mode; pre-optimization and final retained three-run detail results are recorded below.

The deterministic `zoom-v1` path uses wheel-like held steps around a fixed off-center viewport anchor. It crosses atlas, navigation, and detail semantic bands in both directions, restores the exact camera and scale, and reports active scale changes separately from final exact-scale regeneration. Its pre-optimization three-run baseline recorded a 34.0 ms median average, 65.4 ms median p95, 73.2 ms median maximum, and 125 full renders during active zoom. Keeping systems/navigation live first reduced full renders to 42. Directly compositing the compatible cartography snapshot then eliminated the remaining active full renders and produced a 12.6 ms median average, 21.0 ms median p95, 22.6 ms median maximum, and no frame above 33 ms. Settled regeneration remained bounded at a 13.2 ms median average and 16.7 ms median maximum in the final runs. Focused instrumentation attributes the remaining new-scale frame to 10.0 ms bilinear transform, 2.1 ms background, 4.1 ms live systems, and 0.9 ms residual work. JFR and a rejected clipped-source probe show that another Phase 3 source-bounds optimization is not available without changing interpolation or introducing an accelerated surface lifecycle.

1. Add deterministic camera-path playback or an equivalent repeatable harness, then capture an exact post-merge build and fixed campaign/date, camera positions, pan paths, viewport dimensions, and layer/route settings.
2. Run the dense, atlas, detail, and transition scenarios at least three times with all new animation disabled.
3. Extend profiling to distinguish full interaction paints, localized animation paints, cold renders, and cache regeneration. Do not mix cheap animation paints into the dense-pan aggregate and make its average appear better.
4. Record repaint requests versus actual paints, actual clip area, systems visited/drawn, labels drawn, and cache hits, strip refreshes, and full regenerations by layer.
5. Use JFR to identify CPU and allocation contributors. Separate paint duration from EDT queue delay and interaction responsiveness.

Exit gate: a repeatable current-build baseline and enough evidence to choose the next bounded optimization. Preserve the original historical results rather than relabeling them as current.

### Track 1: Prepare Remaining Presentation Data

1. Extend the existing prepared-data approach with operation markers, base counts, relevant contract faction sets, and restriction-display results where profiling supports it.
2. Derive these facts outside painting from canonical campaign APIs. This is presentation caching, not a replacement for domain rules or action-time validation.
3. Invalidate on their actual dependencies: date, contracts/scenarios, base location, player faction, faction standings, system ownership/population, overrides, and relevant options.
4. Prepare stable route membership, warning destinations, and requested-waypoint information on route or assessment changes instead of rebuilding equivalent collections each frame.
5. Reuse prepared screen layouts while camera, marker metrics, and relevant interaction state are unchanged. Separate stable world data from camera-dependent geometry.
6. Keep Swing state on the EDT. Any background preparation must consume immutable captured inputs and reject obsolete results before installation.

Exit gate: unchanged marker semantics and lower measured paint work or allocations. An animation-only paint must not rescan contracts, scenarios, bases, or the entire system catalog.

### Track 2: Prove a Minimum Localized-Repaint Path

1. First prove a damage-aware path for one fixed marker footprint. Consume the actual Swing graphics clip and restore the underlying map before drawing changing content; widen the mechanism only after real display-path measurement confirms bounded work.
2. Limit system, label, route, and instrument work to content intersecting the damage. Include conservative visual bounds for text, strokes, glows, and markers whose centers lie outside the clip.
3. Reuse existing retained surfaces where valid. Do not assume they contain live labels and overlays; reconstruct all intersecting content in its original paint order.
4. Invalidate both old and new visual bounds when a marker moves or changes. Account for antialiasing and device scaling so previous pixels cannot survive as trails.
5. Preserve a correct full-paint fallback for invalid caches, camera changes, resize, and display-scale changes. Never use missing cache content as permission to leave a blank patch.
6. Instrument actual dirty-region merging. Swing may merge distant rectangles into one large region, and transparent child components can trigger parent painting. Neither an overlay component nor `repaint(rect)` alone proves bounded work.
7. If distant effects routinely produce near-full-window damage, measure a bounded damage compositor or a different placement strategy before accepting the animation. Avoid an unbounded set of per-effect components.

Exit gate: a tiny fixed-location update visits and paints only intersecting content, is visually equivalent to a full repaint, and does not regenerate static layers. Confirm this through the real Swing display path, not only off-screen rendering. If Swing expands the update into near-full-map work, stop this track rather than building further animation infrastructure on a false premise.

### Track 3: Restore JumpShip Navigation Lights

This is the first visual restoration experiment because its affected area and object count are small.

1. Keep the existing cached ship icon. Draw a bounded pair of light accents over it; do not recreate or retint the whole icon per update.
2. Use a restrained deterministic blink cycle. Schedule updates at visible state changes; a hard blink needs no continuous frame loop. If a short fade is desirable, update only during that fade.
3. Repaint the padded light/ship footprint through Track 2. Clear the previous state correctly on blink-off, pan, semantic-zoom change, and actual jump transitions.
4. Use monotonic elapsed time rather than incrementing a frame counter, so delayed callbacks do not speed up, queue catch-up frames, or replay old blinks.
5. Schedule only when the detailed ship is visible and motion is enabled. Stop on hidden map, minimized window, removal, and shutdown; resume at the current phase without replay.
6. During existing full-map paints, draw the current light state without requesting another full-map repaint.

Exit gate: accepted appearance, no clipping or trails at normal/HiDPI scale, no timer leaks, zero phase-driven cache invalidation, and the animation budgets pass for lights alone.

### Track 4: Restore a Smooth Sparse Active-Route Pulse

1. Keep the solid amber active route retained and readable at all times. Animate only one or two short highlights over the visible active route, with an explicit global count cap rather than one effect per leg.
2. Prepare segment geometry and cumulative lengths when the route changes. Derive camera-dependent placement when the camera changes; the timer must not resolve route systems or perform campaign lookups.
3. Use elapsed time to move highlights along prepared geometry. Treat the motion as an activity/direction cue, not actual transit progress or a moving fleet position.
4. Derive cadence from on-screen speed and device-pixel displacement instead of fixing it at 15-20 updates per second. Start in the 30-60 Hz range only for the bounded visible pulse, target at most one device pixel of travel per update, and use elapsed time so delayed callbacks skip ahead rather than queue catch-up frames. A visibly choppy low-cadence result fails even when its timing budget passes.
5. Damage the old and new highlight footprints, including their entire trail/glow extent. Check widely separated highlights for Swing dirty-region amplification.
6. Preserve paint order relative to systems, labels, badges, and warnings. An always-on-top overlay must not obscure navigation information.
7. Keep ambient phase out of cache keys and out of `canUseMergedNavigation` eligibility. Existing finite plotting/activation behavior remains distinct and takes visual precedence where effects would overlap.
8. Stop scheduling for an absent/off-screen route, hidden/minimized map, or reduced motion. Preserve the static route in every case.

Exit gate: route-only and combined light/pulse scenarios meet the budgets, remain legible, and do not rebuild retained navigation merely because a highlight moved. Reject or revise pulses independently if ship lights pass but route damage remains too expensive.

### Track 5: Retain Ordinary Labels Where Valuable

This primarily targets detail zoom. Do not claim a dense-medium improvement when ordinary labels are already hidden there.

1. Retain settled ordinary labels, including outlines and spectral suffixes, at the exact device resolution and scale at which they will be displayed.
2. Keep labels whose style, position, or priority changes frequently in an appropriately separate path. Selection/hover promotion must not leave a second cached label underneath.
3. Include text extending beyond system centers in cache query and exposed-strip bounds. Preserve labels crossing viewport/cache edges and labels shifted by HPG or route-status markers.
4. Invalidate for date/name/suffix, font and display metrics, semantic alpha, relevant marker placement, and empty-system visibility changes.
5. Measure memory and compositing cost against saved text rendering. Reject a full-surface label cache if its blit cost exceeds the text work it removes in the intended scenario.

Exit gate: a repeatable detail-view benefit with identical priority-label behavior and live normal/HiDPI sharpness. Never substitute tiny scaled glyph rasters for crisp rings or text.

### Track 6: Refine Cache Dependencies and Transitions

Treat these as separate measured changes, not one broad cache rewrite.

1. Give territory/emblem, system-art, navigation, and label caches only the dependencies that affect their pixels. Verify both necessary invalidation and reuse when an unrelated setting changes.
2. Keep viewport, map scale, device-pixel scale, date/data revision, and relevant style metrics explicit where they affect a surface. Do not remove a dependency merely to improve hit rate.
3. For expensive map-mode/layer transitions, prepare previous and target retained surfaces and crossfade their composites instead of rerasterizing both modes every tick.
4. Preserve existing paint order and group-alpha semantics. Keep current content visible until its valid replacement is ready; reject stale results after rapid mode/date/camera changes or hide/show cycles.
5. Test reversals and A-B-A requests. Bound simultaneous old/new/preparing surfaces and release superseded buffers.
6. Add semantic-boundary hysteresis only if measured boundary churn warrants it, with deterministic tests and no interaction-dependent band changes.

Exit gate: reduced regeneration or transition cost with no stale layers, extra blur, missing content, or unbounded memory. Settled warm-pan gains and transition gains must be reported separately.

## Shared Rendering and Scheduling Rules

- Prefer one small coalescing scheduler for new ambient effects, with a next-visible-change deadline per effect. Ship lights alone must not inherit the route pulse's faster cadence.
- Derive visual state from one monotonic time sample per paint. Drop redundant requests instead of accumulating callbacks.
- During pan/zoom, use existing paints for the current animation phase and coalesce duplicate requests. This avoids extra work without hiding or freezing visible content as a drag-only shortcut.
- Skip work for off-screen effects, but keep elapsed-time state so re-entry does not replay missed frames.
- Respect the incoming clip. Do not reset it to full viewport while restoring cached content.
- Keep reuse exact-scale and device-aligned. Fractional device-pixel movement requires a proven alignment strategy or a correct refresh, not silent rounding that shifts rings and labels.
- Bound additional memory, including transition surfaces and temporary buffers. Use the original 192 MiB incremental-cache budget at 4K as an initial ceiling to measure against, not as a presumed allocation allowance.
- Keep screenshots, exports, and headless rendering deterministic through an explicit animation time or settled state.

## Benchmark Matrix

Run at least three repetitions per candidate/configuration, comparing animation off, lights only, route pulses only, and both wherever applicable. Use the same camera path and settings for each paired comparison.

| Scenario | Main evidence |
| --- | --- |
| Stationary detail view with visible ship | Blink update cost, idle CPU/EDT activity, actual dirty area, trails |
| Stationary active route, short and long | Bounded pulse work, distant-damage merging, no per-leg growth in effect count |
| Dense medium warm pan, approximately 500 systems | Average/p95/p99/max, threshold counts, animation delta, cache outcomes |
| Detail warm pan | Label cost, full stars/rings, service markers, cache-edge equivalence |
| Atlas view and off-screen fleet/route | Correct semantic visibility and absence of unnecessary scheduled paints |
| Hover/selection plus active and proposed routes | Priority labels, warnings, badges, paint order, input responsiveness |
| Slow boundary zoom and rapid pan/zoom | Hysteresis need, cache churn, marker clipping, continuous content |
| Faction/analytical and layer transitions | Cold preparation versus animation cost, reversal and stale-result rejection |
| Date, resize, display-scale, empty-system changes | Correct invalidation and memory release |
| Hide/show, minimize/restore, removal, reduced motion | Scheduler lifecycle, no replay or background repaint loop |
| Inspector scrolling | No regression in scrolling responsiveness or dossier reveal cancellation |

Record build commit plus dirty-state identity, campaign/date, camera path, layers/options, active/proposed route IDs, visible-system count, semantic zoom, viewport logical/device dimensions, display resolution/scaling/refresh rate, font/GUI scaling, OS, CPU/GPU/RAM, power mode, Java vendor/version, JVM flags, and observed Java2D pipeline.

Report average, median, p95, p99, maximum, counts above 16.7 ms and 33 ms, actual paint count, requested repaint count, clip-area distribution, systems visited/drawn, labels drawn, per-layer cache outcomes, phase timings, allocation rate, process CPU measurement convention, and EDT delay. Keep cold render/regeneration and animation-only paints separate from full warm interaction paints.

The existing profiler uses `-Dmekhq.map.renderProfiling=true`. Its `>16ms` counter means 16.0 ms, not 16.7 ms; do not relabel it. Add or derive the exact acceptance counter when extending measurement. Use `-Dsun.java2d.trace=count` only in separate pipeline-diagnostic runs because tracing distorts timing.

### Deterministic Pan Harness

From the repository root, enable both profiling and the opt-in harness before starting MekHQ:

```powershell
$env:JAVA_TOOL_OPTIONS="-Dmekhq.map.renderProfiling=true -Dmekhq.map.renderBenchmark=true"
.\gradlew.bat :MekHQ:run
```

Open the interstellar map, establish the intended campaign, date, zoom, viewport, route, and layers, then press `Ctrl+Shift+B`. Do not interact with or resize the map until the camera returns to its exact origin. The harness traverses fixed logical-pixel offsets of up to 640 horizontally and 360 vertically around that origin. Pressing the shortcut again cancels the run.

The useful output is the single `Map render benchmark result:` line in `MekHQ/logs/mekhq.log`. It includes the `pan-v2` path ID, viewport, date, map mode, layer state, camera origin and scale, actual measured duration, and the existing aggregate frame percentiles, thresholds, phase timings, visible-system count, and cache outcomes. Repeat each unchanged configuration at least three times and retain every result line; the harness measures paint duration, not delivered FPS or end-to-end input latency.

## Validation

### Required Automated Coverage

- Presentation snapshots update for every real dependency and remain unchanged for irrelevant events; compare restriction-display facts with canonical rule results.
- Partial repaint output matches full repaint output after lights change, pulses move, hover/selection changes, and overlapping labels/markers are restored.
- Old and new damage bounds cover strokes, glow, text, viewport edges, and normal/HiDPI transforms.
- Ambient ticks do not invalidate static caches or visit the full system set in the localized path.
- Pulse count remains bounded across empty, single-system, long, replaced, and partially visible routes; zero-length segments are handled safely.
- Elapsed-time animation remains deterministic under delayed callbacks, route changes, reduced motion, and hide/show/minimize/removal lifecycle changes.
- Labels retain correct priority, placement, suffixes, empty-system behavior, and exposed-strip equivalence.
- Narrowed keys and prepared crossfades handle every relevant invalidation, reversal, A-B-A request, and stale completion.
- Existing route, territory, capital, operation, navigation, and shared-faction segmentation coverage remains applicable.

Add focused coverage with each implementation slice. Follow repository policy for execution: full MekHQ tests and required build/Checkstyle gates belong on the final unchanged publication candidate; earlier unit-test execution is reserved for explicitly requested diagnosis. Run `git diff --check` before publication.

### Visual and Proxy Acceptance

Use the real Swing display path at normal and HiDPI scaling. Confirm sharp stars/rings/text, no trails or clipping, stable paint order, legible static routes, restrained animation speed, correct reduced-motion behavior, and uninterrupted pan/zoom and inspector scrolling. Pure visual acceptance belongs to the user.

The target user's lower-end Mac report says panning looks good, but no repeatable timings were captured. Defer external Mac benchmarking and do not block acceptance on it. Use the fixed development-machine matrix and approximately 50% warm-pan improvement targets as the repeatable performance proxy; pure visual acceptance remains with the user.

## Recommended Sequence and Decision Gates

1. Complete Track 0 with the deterministic zoom baseline before choosing the next renderer optimization. Qualitative external feedback may inform priorities but does not replace repeatable development-machine comparisons.
2. Use zoom cache outcomes and phase timings to choose the smallest controlling-path change. Apply only profiler-supported portions of Track 1 and keep any repeatable general performance win independently of later animation decisions.
3. Prove the smallest Track 2 localized update around the JumpShip, then make Track 3 lights the first visual experiment.
4. Attempt Track 4 only after the localized path and lights pass. Evaluate pulse smoothness and cost independently, then evaluate both effects together.
5. Pursue Tracks 5 and 6 in the order supported by measured remaining costs. They are candidates, not mandatory complexity or prerequisites for the visual effects.
6. If a small effect triggers a full-map scan, cache rebuild, or large merged dirty region, fix that controlling path only when the fix is proportionate and useful. Otherwise leave that effect disabled.
7. If warm pan is satisfactory but regeneration still hitches, use the original tiling/background-preparation gate. Investigate Java2D pipeline or GPU alternatives only when profiling shows the retained architecture cannot meet the target.
8. Do not remove accepted information or detail to make room for an optional effect.

## Results Record

### Track 0: Deterministic Pan Harness (2026-09-05)

- Added opt-in playback gated by both `mekhq.map.renderProfiling=true` and `mekhq.map.renderBenchmark=true`.
- The path is derived from its captured origin for every update, includes horizontal and vertical traversal, and restores the exact origin after completion or cancellation.
- Initial `pan-v1` playback interpolated fractional screen-pixel positions. Retained layers intentionally reject fractional translations, so three diagnostic runs forced two full cache renders on every paint: 514 frames/1,028 full renders, 533/1,066, and 542/1,084. Territory work dominated at 50.6-52.9 ms per frame. The first run used a different origin; the latter two reproduced the defect from the same origin. These results are excluded from performance baselines.
- `pan-v2` rounds each absolute path sample to a whole screen pixel, matching mouse-pan cache requirements without accumulating movement. Focused coverage verifies whole-pixel samples and retained-raster reuse at the reported viewport, origin, and scale.
- `transition-cold-v1`, started with `Ctrl+Shift+T`, waits for an exact retained frame, measures a deterministic Faction/Technology mode transition through the first exact target frame, then clears and measures the exact-scale render caches separately. It reports elapsed wall time alongside paint aggregates, rejects configuration changes or unrelated animations, and restores the original mode.
- `zoom-v1`, started with `Ctrl+Shift+Z`, waits for an exact retained frame and sweeps around a fixed off-center anchor through atlas and full-detail scales in both directions. It reports active zoom separately from cold exact-scale regeneration, rejects configuration or camera changes, and restores the exact starting camera.
- Three valid pre-optimization zoom runs recorded a 34.0 ms median average, 65.4 ms median p95, 73.2 ms median maximum, 50 median frames above 16 ms, 42 above 33 ms, and 125 full renders. Median settled regeneration was 88 ms wall time, 13.4 ms average, and 17.1 ms maximum with no frame above 33 ms. Visual review found no flashes after the harness changed from continuous samples to deterministic wheel-like held steps.
- Three active-zoom live-system runs recorded a 30.9 ms median average, 63.6 ms median p95, 67.8 ms median maximum, 42 frames above 33 ms, and 42 full renders. Visual review found no flashes, missing content, blur, or settlement jump. Cache-hit frames averaged approximately 9.2 ms, but each new-scale cartography fallback remained approximately 52 ms; avoid its full-size synchronous allocation/reprojection next.
- Three dense-medium runs at 1032x555, scale 1.7859556018152165, and approximately 582-583 visible systems recorded 6.3-6.4 ms average, 7.1 ms p95, 7.6-7.9 ms p99, 9.0-11.6 ms maximum, zero frames above 16 ms or 33 ms, and zero full cache renders. Median results are 6.4 ms average, 7.1 ms p95, 7.8 ms p99, and 9.4 ms run maximum. This scenario passes the current paint-duration and cache-reuse gates.
- Ordinary labels and ownership rings were intentionally absent at this semantic zoom. A separate closer detail-view baseline is required to measure those visuals; it does not need to retain approximately 500 visible systems.
- The detail query now accounts for the asymmetric rightward label extent using the actual label anchor and cached maximum rendered label width. Focused renderer-cache coverage passes 42/42, including the left-edge query bound.
- Three post-fix scale-4.7 detail runs with 167 live-query systems recorded median results of 7.9 ms average, 8.5 ms p95, 10.2 ms p99, and a 13.8 ms run maximum, with no frame above 16 ms and no full cache renders. Visual review found no remaining popping. This scenario passes its timing, cache-reuse, and normal-scale horizontal-pan visual gates.
- Three scale-0.9369559896737012 atlas runs with 1,229 live-query systems recorded median results of 7.0 ms average, 7.8 ms p95, 8.5 ms p99, and a 10.3 ms run maximum, with no frame above 16 ms and no full cache renders. The supplied screenshot shows the intended compact-contact and strategic-emblem presentation without a static seam or clipping defect, and all three replays remained free of popping, clipping, black seams, and disappearing content. This scenario passes its timing, cache-reuse, static-presentation, and horizontal-pan visual gates.
- Three scale-4.7 Faction-to-Technology transition runs with 184 visible systems recorded a 449 ms median wall time, 22.4 ms median average paint, 17.9 ms median p50, and 44.3 ms median p95/p99/maximum. Every paint exceeded 16 ms, two paints per run exceeded 33 ms, and only two frames per run used retained cartography and merged navigation. Median territory and systems phases were both 8.8 ms. This scenario fails the interaction timing gate; initial visual review looked smooth but was not certain enough for final acceptance.
- The paired same-view Technology regeneration phases recorded a 99 ms median wall time, 20.1 ms median average paint, 19.3 ms median p50, and 20.7 ms median p95/p99/maximum. Both paints per run exceeded 16 ms, none exceeded 33 ms, and median territory cost was 14.7 ms. Regeneration is bounded but remains a secondary optimization candidate behind repeated transition-time cache bypass.
- The final retained verification used 1032x561, scale 4.7, and 183 visible systems. Its three transitions recorded a 396 ms median wall time, 14.3 ms median average paint, 13.6 ms median p50, 27.1 ms median p95/p99/maximum, two median paints above 16 ms, and none above 33 ms. All frames used retained cartography and merged navigation; animation cache-hit maxima were 14.2-14.4 ms. Camera and viewport differences make the improvement over the original baseline directional rather than strictly paired.
- The final paired regeneration phases recorded a 94 ms median wall time, 17.7 ms median average paint, 17.3 ms median p50, and 18.1 ms median p95/p99/maximum. One first-run paint reached 39.7 ms; the other four paints were 17.2-20.2 ms. This unexplained outlier remains recorded for repeated first-render and visual-hitch follow-up.

### Track 1: Initial Presentation Snapshot (2026-09-04)

- Operation-marker aggregation, player-base counts, and active-contract employer/target sets are now captured in one immutable presentation snapshot instead of being rebuilt during every map paint.
- The snapshot refreshes on campaign/system refresh, navigation-analysis refresh, mission and scenario events, and player-base location add/remove events. Restriction decisions remain live per visible system because their standing and ownership dependencies need a separate proven invalidation contract.
- Focused operation and snapshot coverage passes 8/8. Current dense, detail, and atlas baselines include the snapshot, but no paired before/after benchmark isolates its individual performance effect.

For each subsequent candidate, append the exact build/configuration, paired individual runs, run medians, cold/regeneration results, profiler/JFR evidence locations, memory/CPU observations, visual decisions, any optional external results, and an explicit pass/fail/inconclusive decision against the gates above.

The desired outcome is a measurably cheaper renderer that can support restrained ship lights and one smooth active-route pulse. The performance work and both visual effects remain independently valuable and independently rejectable; none is a reason to reopen the removed ambient effects or compromise map readability.
