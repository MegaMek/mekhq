# Native map performance investigation

## Recommendation

Keep Java2D as default. Skia reduces render-callback work substantially, but native
HPG rendering still loses requested updates at detailed zoom. Explicit dash
batches improved HPG detail delivery from 8.5 to 27-28 callbacks/sec, but Java2D
still handles about 33. Continue profiling before considering a default switch.
Territory zoom also needs investigation. This is a promising prototype, not a
demonstrated end-to-end performance win across the supported layers.

The initial investigation added optional observers, a reproducible benchmark,
and a streaming JFR summary utility. The subsequent HPG fix replaces native dash
effects with explicit, viewport-bounded dash segments; see the follow-up below.
Normal rendering has only a null-observer guard; no profiling recording is started
outside the benchmark. Swing, the default renderer, and portraits are unchanged.

## Reproduce

### Adapter identification and callback-paced mode

Step-1 diagnostic run `MekHQ/build/skiko-perf/20260914-182531/` confirms that
Skiko's active renderer is **Direct3D on Intel(R) Graphics**, not the RTX 5090.
The runtime reports 128 MB total VRAM; this is not the integrated GPU's total
available shared-memory budget. No GPU preference or system setting was changed.

The benchmark now records the actual `SkiaLayer.getRenderInfo()` after warmup in
`render-info.txt`, once per pass. `environment.txt` also records the application
PID and pacing mode. Java2D's active pipeline remains unidentified.

```powershell
$env:SKIKO_PERF_SCENES = 'core'
$env:SKIKO_PERF_MOTIONS = 'detail-pan'
$env:SKIKO_PERF_PACING = 'throughput'
try {
  .\gradlew.bat :MekHQ:runSkikoMapSmoke --args=--performance
} finally {
  Remove-Item Env:SKIKO_PERF_SCENES,Env:SKIKO_PERF_MOTIONS,Env:SKIKO_PERF_PACING
}
```

`throughput` permits one pending camera request and schedules the next on the EDT
after callback completion. The existing timer still handles settling/watchdog
checks but no longer sets the minimum interval between requests. It does not
disable display vsync or native frame limiting. `timer` (the default) retains the
previous 16 ms requested cadence. Invalid pacing values fail before execution.
Elapsed time now ends at the final acknowledged callback rather than the later
summary timer tick. Neither mode measures actual displayed FPS.

The four-pass core/detail check acknowledged all 240 requests per pass:

| Renderer | Elapsed seconds | Acknowledged callbacks/sec | Request-to-callback p95 |
| --- | --- | --- | --- |
| Java2D | 2.218-2.296 | 104.5-108.2 | 11.2-11.8 ms |
| Skia (Intel) | 1.615-1.621 | 148.1-148.6 | 9.7-11.2 ms |

This disproves a roughly 34 callbacks/sec maximum for either renderer. It is a
short, closed-loop throughput diagnostic, not a fixed-speed interaction workload;
the camera advances faster on the faster renderer. There are no competing camera
requests to coalesce in this mode. Do not compare its rates directly with the
timer-paced baseline or interpret zero coalescing as evidence of no dropped
presentations. Heavy-layer callback-paced results follow below.

### Heavy-layer callback-paced results

Run `MekHQ/build/skiko-perf/20260914-193046/` completed all 36 passes in
6 minutes 54 seconds with exit code zero. All passes acknowledged 240 of 240
requests at 1018 x 549 logical pixels. Every native pass reported Direct3D on
Intel(R) Graphics. GPU preferences and renderer code were unchanged.

The command uses `SKIKO_PERF_PACING=throughput`,
`SKIKO_PERF_SCENES=cartography,hpg,overlays`, and
`SKIKO_PERF_MOTIONS=overview-pan,detail-pan,zoom`. Each combination runs twice
with renderer order reversed. The following ranges span those two repeats;
they are not confidence intervals. HPG includes territories and emblems.

| Scene / motion | Java2D callbacks/sec | Skia callbacks/sec | Java2D latency p95 (ms) | Skia latency p95 (ms) |
| --- | --- | --- | --- | --- |
| Cartography overview pan | 114.1-116.1 | 51.6-56.5 | 8.3-9.2 | 22.5-24.9 |
| Cartography detail pan | 111.2-115.3 | 41.2-41.4 | 8.8-9.1 | 37.4-38.9 |
| Cartography zoom | 51.8-52.5 | 22.1-23.8 | 27.2-29.4 | 121.6-133.6 |
| HPG overview pan | 125.0-133.9 | 27.1-29.4 | 7.4-8.4 | 45.0-47.2 |
| HPG detail pan | 97.0-103.8 | 26.0-28.3 | 10.1-10.6 | 48.2-55.7 |
| HPG zoom | 25.2-26.7 | 16.8-18.9 | 94.5-107.8 | 129.5-143.2 |
| All overlays overview pan | 85.5-98.0 | 24.0-26.1 | 10.9-12.1 | 51.3-58.2 |
| All overlays detail pan | 92.8-103.0 | 22.6-22.9 | 10.8-11.3 | 56.8-56.9 |
| All overlays zoom | 24.3-25.3 | 14.0-16.0 | 97.3-102.4 | 140.1-167.0 |

Latency here remains request-to-callback completion, not input-to-photon.
The one-pending-request design means zero coalesced requests by construction;
it does not prove that every callback was presented. These are closed-loop
throughput results with different camera speeds, not a fixed-speed user-input
test. Vsync, native frame limits, JFR overhead, and existing Java2D retained or
provisional imagery remain in play. The earlier core check was a separate run,
not a simultaneous control.

Java2D leads in every heavy-layer combination in this run. Cartography alone
already produces a substantial native slowdown; remaining work is not confined
to HPG. Short native command-recording callbacks still do not explain the longer
intervals between callbacks. Presentation/playback tracing is the next diagnostic
step before selecting a retained-layer optimization. This run does not identify
a GPU saturation or driver defect, and does not measure displayed FPS.

Actual GPU/playback/display timing is **blocked, not measured**. The pinned
Skiko analytics interfaces expose initialization events, not per-frame present
completion. JFR native stack sampling does not supply those durations. WPR is
installed, but PresentMon is not on PATH. The current Windows token is neither
an elevated administrator nor a member of Performance Log Users. PresentMon's
[documented prerequisite](https://github.com/GameTechDev/PresentMon#user-access-denied)
therefore prevents a live ETW capture from this session. No elevation, trace
session, installation, or group-membership change was attempted.

To finish step 1, the user must run a standalone PresentMon capture from an
elevated terminal (or arrange Performance Log Users access). Keep the application
and VS Code unprivileged. Correlate the capture with the benchmark PID and measured
pass intervals, distinguishing the native map swapchain from other UI windows;
exclude setup, warmup, and renderer switches. Per-pass trace time markers and
capture analysis still need to be added before reporting presentation results.
Do not claim step 1 is complete or proceed to retained-layer conclusions yet.

### Original timer-paced comparison

From the MekHQ repository root, with Java 21 and an available, unminimized desktop:

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args=--performance
```

The driver opens a fresh in-memory campaign, runs sequentially, closes its own
window, and exits. It does not load/save campaigns or capture screenshots. Avoid
concurrent workloads and interaction with the benchmark window. A missing callback
for ten seconds fails the run, provided the EDT is still processing timer events.

Outputs: `MekHQ/build/skiko-perf/<timestamp>/summary.csv`, `frames.csv`,
`environment.txt`, and `renderers.jfr`. A failure can leave partial results and
`FAILED.txt`; partial runs are not complete comparisons. JFR files may contain
machine/environment metadata; inspect them before sharing.

Isolate overlay cost with the same workload and both renderer orders:

```powershell
$env:SKIKO_PERF_SCENES = 'administrative,hpg,landmarks,reachability'
$env:SKIKO_PERF_MOTIONS = 'detail-pan'
try {
    .\gradlew.bat :MekHQ:runSkikoMapSmoke --args=--performance
} finally {
    Remove-Item Env:SKIKO_PERF_SCENES,Env:SKIKO_PERF_MOTIONS
}
```

Allowed scenes: `core`, `cartography`, `overlays`, `administrative`, `hpg`,
`landmarks`, `reachability`. Allowed motions: `overview-pan`, `detail-pan`, `zoom`.
Without these environment variables, the first three scenes and all motions run.

Summarize a recording with bounded memory using the JDK consumer API:

```powershell
java MekHQ/experimental/skiko/src/mekhq/gui/SkikoMapProfile.java MekHQ/build/skiko-perf/<timestamp>/renderers.jfr
```

## Method

- Two sequential repeats, reversing Java2D/Skia order in the second repeat.
  Each pass settles for 1.5 seconds, warms up for 80 camera requests, then measures
  240 requests. Default matrix: 36 passes; isolated-overlay matrix: 16 passes.
- Faction map, hidden empty systems, current system selected. Core disables
  territories, emblems, administrative borders, capitals, recharge, HPG, and
  reachability. Cartography adds territories/emblems. Overlays additionally enables
  districts, all capitals, recharge, HPG (default A+B), and reachability (one hop).
  Each isolated scene adds just its named group to cartography. Operations is off.
- Matched 1018 x 549 logical-pixel drawable viewports in the measured runs. The
  benchmark removes the Java2D wrapper's one-pixel border. Native surfaces are
  recreated between passes; initialization is outside measured samples.
- Overview scale 0.8, detail scale 6. Pan follows a repeated integer logical-pixel
  trajectory. Zoom uses quantized 1.175 wheel steps from 0.8 to about 7.65; Java2D
  receives actual wheel events so its interaction cache is exercised. Scale is
  checked against the native target. Measurement restarts the trajectory after
  warmup, so the first sample can include a camera jump.
- Swing timer requests 16 ms, but observed delivery is usually near 30 ms on this
  machine. This is not a 60 Hz or 240 Hz benchmark. Timer coalescing under EDT load
  means Java2D's zero recorded coalesced requests does not imply no missed ticks.
- An observer measures elapsed time, thread CPU, and Java allocation around the
  Java2D wrapper paint including children, or around Skia's draw-command callback.
  The boundaries differ: native playback, GPU completion, and presentation follow
  the callback and are not included in its elapsed time or thread allocation.
- `frames` counts callbacks acknowledging a pending request, not displayed frames.
  `coalesced` counts later delivered requests before that callback. Latency is from
  the oldest pending request to callback completion, not input-to-photon latency.
  Extra callbacks without a pending request are excluded. Coalescing also means
  the renderers can sample different subsets of the camera trajectory.
- Percentiles use nearest rank per pass. Process CPU and GC deltas include all
  application threads, not only rendering. Windows thread CPU granularity produced
  many zero medians; these do not mean zero CPU work. JFR `profile` is enabled for
  both renderers. Results include its overhead and are not microbenchmarks.

## Results

Local runs on 2026-09-14: Windows 11, Java 21.0.12+8, 4 GiB maximum heap,
Intel Core Ultra 9 290HX Plus (24 logical processors), 175% display scale,
2560 x 1600 desktop reporting 240 Hz. Skiko 0.150.1 uses Direct3D.
The machine reports Intel Graphics (driver 32.0.101.8724) and an NVIDIA RTX 5090
Laptop GPU (32.0.15.9202). The selected native adapter was not captured, so these
results must not be attributed specifically to the NVIDIA GPU.
Campaign date 3067-01-01, camera anchored on Galatea.

Corrected complete run: `MekHQ/build/skiko-perf/20260914-172339/`.
Ranges below cover the two repeats, not confidence intervals or pooled percentiles.
J2D/S columns show Java2D then Skia; all timing columns are milliseconds.

| Scene / motion | Callback p95 J2D / S | Request-to-callback p95 J2D / S | Callbacks/sec J2D / S |
| --- | --- | --- | --- |
| Core overview | 6.7-6.9 / 2.2-2.9 | 6.9-7.0 / 2.2-3.1 | 33.5-34.0 / 33.6-34.1 |
| Core detail | 6.5 / 1.5-1.6 | 6.6 / 1.6-1.7 | 33.7-33.8 / 33.4-34.6 |
| Core zoom | 16.3-16.7 / 1.9-2.0 | 16.5-16.8 / 2.0-2.1 | 33.4-33.8 / 33.9-34.2 |
| Cartography overview | 8.0-8.1 / 2.5-2.6 | 8.1-8.2 / 3.2-3.8 | 34.0-34.1 / 34.4-35.4 |
| Cartography detail | 8.3-8.5 / 1.6-1.7 | 8.4-8.6 / 14.2-21.6 | 34.4-34.5 / 31.6-33.9 |
| Cartography zoom | 25.5-25.8 / 2.2 | 25.6-25.9 / 70.3-82.1 | 32.0-32.7 / 27.1 |
| All overlays overview | 10.5-10.8 / 3.9-4.1 | 10.5-10.9 / 45.7-47.6 | 33.8 / 24.5-25.9 |
| All overlays detail | 10.5-12.2 / 3.6-4.4 | 10.6-12.3 / 138.5-142.2 | 33.9-35.1 / 8.0 |
| All overlays zoom | 98.4-107.0 / 3.6-4.0 | 98.4-107.1 / 178.6-179.7 | 21.0-22.1 / 12.6 |

Core pan callback medians are 3.8-4.2 ms for Java2D versus 1.0-1.9 ms for Skia.
However, median measured-thread allocation is about 196-204 KiB/callback for
Java2D versus 451-1922 KiB for Skia: native rendering does not automatically mean
less Java allocation. Process CPU is also not uniformly lower for Skia.
Native all-overlay detail coalesces 182-183 of 240 requests; native all-overlay
zoom coalesces 148-151. Those overloaded cases are retained, not discarded.

### Isolated overlays

Complete run: `MekHQ/build/skiko-perf/20260914-173416/`. All use detail scale and
territories/emblems; each adds only the named group. Java2D delivers roughly
34 callbacks/sec throughout, with request-to-callback p95 between 8.4 and 12.5 ms.

| Added group | Native callbacks/sec | Native request-to-callback p95 | Native coalesced / 240 |
| --- | --- | --- | --- |
| Administrative borders | 31.5-31.9 | 31.9-32.8 ms | 12-20 |
| HPG | 8.5-8.6 | 136.9-139.1 ms | 178-179 |
| Capitals and recharge | 33.5-33.7 | 17.9-18.4 ms | 1 |
| Reachability | 33.6-33.8 | 18.5-24.0 ms | 2-3 |

This isolates the HPG group, not links versus station badges or class A versus B.
Those finer ablations and native playback/presentation tracing are the next step.

## HPG dash fix follow-up

The native Class B `PathEffect` was isolated by temporarily drawing the same
links solid, leaving station badges and Class A links intact. Delivery rose to
28-29 callbacks/sec (`20260914-180621`), strongly implicating the dash path rather
than station badges. That appearance-changing probe was removed.

Clipping the original dashed lines alone recovered only about 10.5 callbacks/sec
(`20260914-180743`). The retained fix clips to the padded viewport and rounds the
clipped start back to a whole 16-unit dash period, preserving the original phase.
It submits the visible 8-unit dash segments through `Canvas.drawLines`, once per
link. Direction, original endpoints, partial final dashes, per-link alpha overlap,
link order, Class A strokes, and badges are preserved. No Java2D frame uploads,
raster caches, timers, or persistent native resources were added.

Focused optimized run: `20260914-180912`. Broader 24-pass verification:
`MekHQ/build/skiko-perf/20260914-181206/`, same viewport, camera workload, JFR
settings, and reversed renderer order as the baseline. Rates count acknowledged
callbacks, not presented frames. Ranges span two repeats.

| Workload | Native before callbacks/sec | Native after callbacks/sec | Java2D in after run | Native after latency p95 |
| --- | --- | --- | --- | --- |
| HPG detail pan | 8.5-8.6 | 27.1-27.6 | 33.1 | 41.4-44.8 ms |
| All overlays detail pan | 8.0 | 23.6-24.3 | 33.4-34.0 | 50.0-52.8 ms |
| All overlays zoom | 12.6 | 16.8-18.2 | 21.0-21.6 | 133.9-140.9 ms |

HPG detail latency was previously 136.9-139.1 ms. Native still coalesces 43-45 of
240 requests in HPG detail and 64-70 with all overlays. Overview uses Class A
only and is unchanged by this Class B optimization. Allocation increases somewhat
because explicit segment arrays are created per visible link: HPG detail median
is now about 1890-1894 KiB/callback versus about 1706-1708 before. This is a
measured throughput improvement, not allocation or complete performance parity.

Ten reference fixtures at content scales 1 and 1.75 compare the optimized native
raster against the original native dash effect. They include long offscreen
endpoints, horizontal/vertical/diagonal and reversed links, fractional endpoints,
partial dashes, viewport edges, wholly offscreen lines, and zero-length lines.
Each permits fewer than 20 pixels with a channel difference greater than 8 and
less than one accumulated channel-difference unit per image pixel overall.
These permit minor antialiasing differences, not shifted dash phases.

Focused HPG smoke and full offscreen smoke both passed after the fix. Full run:
zero failures, 481 frames, 118425 accumulated native stars, six renderer toggles.
Desktop GPU pixel equivalence and actual presentation latency remain unmeasured.
The remaining territory/playback stalls require further profiling; do not claim
this eliminates every native performance regression.

## Profile interpretation and next work

The corrected recording samples the native render thread predominantly inside
Direct3D `flush` (1089 native samples) and native `Canvas._nDrawPicture` (360).
These samples may include blocking; JFR cannot distinguish GPU saturation, driver
waits, and native tessellation cost here. HPG's ablation result establishes which
layer triggers the problem, not the low-level cause. Do not infer GPU FPS from
callback timing or call this an identified driver bug.

Java allocation samples point to repeated numeric parsing beneath
`SkiaMap.systemArtRadius`, `drawStar`, `focusRadius`, and `markerRadius`, plus
`Paint.getColor4f`. Capturing GUI scale once per frame and avoiding unnecessary
native paint reads are candidates, but have not been implemented or benchmarked.
They do not explain the large HPG callback-delivery stalls on their own.

Prioritize HPG-only native traces and class/link/badge ablations, then evaluate a
native retained layer or less expensive line submission while preserving dash
phase, alpha overlap, zoom fidelity, and resource disposal. Check which GPU is
actually selected. Rerun the same matrix after any optimization, including visual
smoke checks. Measure real presentation/input latency before a promotion decision.

## Rejected comparison and coverage limits

The initial complete run (`20260914-170838`) used fractional-pixel synthetic pans
and direct scale restoration. This bypassed Java2D's integer-pan reuse and zoom
interaction path, producing roughly 52-119 ms medians and tens of MiB allocated per
callback. JFR identified repeated fallback/background territory raster allocation.
That run is a cache-miss stress diagnostic, not representative drag/zoom evidence.
It must not support claims of a 20-50x native speedup. Two earlier partial runs
also failed viewport/sample-count checks and are not accepted comparisons.

The corrected suite covers one date, one location, one desktop size and DPI,
faction mode, and stationary fleet selection. It does not benchmark routes,
transit effects, other analytical modes, cold startup/date changes, continuous
high-rate physical input, minimized/locked desktops, native memory, GPU memory,
power consumption, or other operating systems. Rendered detail is close but not
pixel-identical; Java2D uses retained and provisional imagery during interaction.

After instrumentation, the full offscreen map smoke passed with zero failures:
500 frames, 119612 accumulated native stars, and six final renderer toggles.
This checks behavior and native resource lifecycle, not desktop presentation speed.
Unit suites and Checkstyle were not run; no publication was requested.
