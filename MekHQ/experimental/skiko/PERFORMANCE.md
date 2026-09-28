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

The latest opt-in stage instrumentation identifies Java2D's buffered-image/GDI
surface path and measures native playback separately from synchronized flush.
Both consume significant time; the flush wrapper is the larger measured stage.
Exact separation of native submission work from GPU waiting remains unimplemented.
See "Direct stage timing" below before interpreting the earlier trace hypotheses.

## Reproduce

### Motion-cache prototype

The opt-in `SKIKO_MOTION_CACHE=true` prototype permits fractional territory-tile
reuse during camera movement, then requests direct vectors on mouse release or
120 ms inactivity. It retains the aligned-only experiment as a separate option.
Zoom still rebuilds tiles; stars, labels, and HPG links are drawn directly.

Run `20260914-234553` completed 16 instrumented passes at display scale 1.75.
For 240 acknowledged updates, native cartography detail pans took 1.359-1.403 s
(prior direct run: 5.290-5.421 s); paired Java2D took 1.990-2.111 s. Native HPG
pans took 4.682-4.967 s versus paired Java2D 2.184-2.479 s. These are callback
workloads, not measured displayed FPS or guarantees of live smoothness.
Native zoom request-to-callback p95 remained 134.8-147.4 ms for cartography and
157.0-157.5 ms for HPG: smooth zoom is not achieved.

The current motion smoke passes 24 fixtures, worst moving mean RGB error 0.9307
(in-motion limit 3), exact settled PNG equality, automatic timer settling,
territory disabling, and disposal. This deliberately permits in-motion resampling;
it does not weaken the separate aligned-cache fidelity gate. Actual GPU pixels
and live interaction still require visual acceptance. No GPU or synchronization
settings were changed. The following sections preserve earlier experiment results.

### Retained territory prototype: aligned reuse passes raster gate

`SKIKO_RETAIN_TERRITORIES=true` enables an experimental bounded picture-shader
territory cache; the default remains direct vectors. The initial cached benchmark
`20260914-221740` is **invalid as an optimization result**: its cached territories
were blank. Do not use its apparent throughput or stage-time reductions as evidence
of a valid performance gain.

The blank-image cause was verified in Skiko 0.150.1's
[Picture JNI bridge](https://github.com/JetBrains/skiko/blob/v0.150.1/skiko/src/jvmMain/cpp/common/Picture.cc):
the optional tile is constructed with `MakeLTRB(tileLeft, tileRight, tileBottom,
tileTop)`, producing an empty rectangle for this tile. Passing no explicit tile
uses the picture's correctly recorded cull bounds and restores visible content.
The prototype records a zero-origin padded picture and the fixed map background,
matching the territory layer's first-over-background compositing order.

After restoring visible content, fractional-pan sampling still exceeded the
existing fidelity limit. Cache reuse now requires integer device-pixel translation
on both axes (with a 0.0001-pixel floating-point tolerance). Fractional translations
draw the original vectors without discarding the cached tile or changing the
camera position. Returning to aligned movement resumes reuse. New tiles are still
built when the camera exceeds the padded bounds or the cache key changes.
Both raster captures and native rendering supply their actual target scale to
the cache key and alignment check. Diagnostics report builds, hits, and fractional
fallbacks separately; `active=true` means a tile exists, not that every frame used it.

The expanded `--offscreen --retained-only` check passes 36 fixtures at world scales
0.8 and 6 and display scales 1 and 1.75. It covers both pan axes, fractional
fallbacks, return to aligned reuse, and tile rebuilds. Worst mean RGB channel error
is 0.7056 (limit 0.75); worst fraction above 24 channel levels is 0.000246 (limit
0.01). Fallback frames match direct vectors exactly. Unchanged-camera reuse,
territory disable/re-enable, and native disposal assertions also pass.

The cache remains opt-in. The corrected-cache benchmark below shows no compelling
benefit under the aligned-only policy. GPU cache memory behavior, full invalidation
coverage, and desktop pixel comparison remain unverified. GPU selection and
synchronization are unchanged.

### Fixed-label panning investigation

After the user reported severe live panning choppiness, runs `20260914-233958`
(direct vectors) and `20260914-234135` (corrected retained cache) repeated
cartography/HPG detail pans with throughput pacing and stage instrumentation.
Both use the fixed-label implementation, 32-pixel GUI-scaled system overscan,
1018 x 549 logical viewport, and Direct3D on Intel Graphics. Each run completed
eight passes with 240 acknowledged callbacks per pass; stage analysis accepted
both recordings with 2,588 stage events and eight measured windows each.

| Scene | Java2D direct-run elapsed | Native direct elapsed | Native retained elapsed |
| --- | --- | --- | --- |
| Cartography | 2.117-2.328 s | 5.290-5.421 s | 5.240-5.285 s |
| HPG | 2.584-2.721 s | 8.796-9.161 s | 8.325-8.585 s |

Ranges are two repeats, not confidence intervals or displayed FPS. Java2D controls
in the retained run took 1.943-2.173 s and 2.159-2.486 s respectively, so small
between-run differences must not be attributed solely to the cache.

Across four native passes, retained counters recorded 40 builds, 86 hits, and
1,164 fractional-translation fallbacks (including warmup/setup). Roughly 90% of
draws fell back. At 175% display scale, integer logical pans frequently fail the
device-pixel alignment requirement on one or both axes. The fidelity-preserving
policy therefore does not supply consistent reuse during representative movement.

Direct native playback wall medians were 3.19-3.35 ms for cartography and
8.67-9.43 ms for HPG. Synchronized-flush medians were 16.46-16.91 ms and
23.59-24.03 ms. Retained flush medians remained 16.67-17.07 ms and 22.04-22.17 ms.
Flush includes CPU work and synchronous GPU completion waiting; these timings
do not isolate GPU execution. Label placement cannot explain these later native
stage costs, though its individual CPU contribution was not isolated.

No renderer changes were made in this investigation. A possible next experiment
is fractional image reuse during drag with a sharp vector redraw on release;
that changes the in-motion fidelity contract and is not implemented or accepted.

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

### Screen-sampled map updates (experimental)

Set `JAVA_TOOL_OPTIONS=-Dmekhq.skiko.visibleFrames=true` when running the paired
performance task to sample a centered 256x128 physical-pixel map region from an
independent screen-capture thread. The optional `visible-frames.csv` records
observed distinct-map updates per second, sample rate, and p95 update gaps for
each scene/motion/renderer; companion PNGs identify the sampled region. No admin
access is required. The same workload runs on Java2D and both native backends;
set `SKIKO_RENDER_API=OPENGL` for the OpenGL process and leave it unset for
Direct3D. Gradle's smoke task does not forward `-Dskiko.renderApi` to its forked JVM.

This is a **lower bound on visible region changes**, not swapchain-present FPS or
an instrumented count of every displayed frame. Motion may leave the small
region unchanged, while a sample may skip multiple frames or capture compositor
artifacts. Confirm sample rate approaches display refresh and inspect its map
crop before comparing runs. Screen readback adds workload and may itself affect
frame pacing. Do not compare these rates with unsampled callback-only runs as
though they measured the same workload.

On the 240 Hz Intel display (1018x549 logical viewport at 175% scale), two
repeats each of 240 detail-pan requests in `throughput` mode gave these sampled
map-update rates; ranges are observed Hz across the two repeats:

| Scene | Java2D | Intel Direct3D | Intel OpenGL |
| --- | ---: | ---: | ---: |
| Cartography | 100.8-117.1 / 107.4-109.7 | 181.2-202.5 | 98.6-100.3 |
| HPG | 92.4-94.7 / 89.5-95.0 | 128.7-133.5 | 71.1-71.3 |

Java2D's first range is paired with the Direct3D run, its second with OpenGL.
The sampled rate was generally 235-240 Hz (one Java2D cartography pass 228 Hz).
These short, small-viewport results show Direct3D delivering more visible map
updates for this scripted detail-pan workload. They do not establish live
full-window smoothness, a steady input-paced FPS, or the root cause of stalls.

The same sampler also measured 240 zoom requests per pass (two repeats per
renderer), with these observed update rates and p95 gaps between detected changes:

| Scene | Java2D Hz / p95 gap | Intel Direct3D Hz / p95 gap | Intel OpenGL Hz / p95 gap |
| --- | ---: | ---: | ---: |
| Cartography zoom | 48-52 Hz / 29-33 ms | 53-60 Hz / 121-129 ms | 61-63 Hz / 54-55 ms |
| HPG zoom | 24-25 Hz / 104-117 ms | 43-48 Hz / 137-150 ms | 50 Hz / 70-74 ms |

The zoom rate alone hides important jitter: Direct3D showed much longer p95
visible-update gaps even while acknowledging all camera requests. OpenGL had
steadier sampled zoom updates, but was worse than Direct3D for HPG/detail-pan.
The region may miss changes in other parts of the window, and a 240 Hz sampled
desktop image cannot prove scanout or latency to the user's eyes. In particular,
these results are not a justification to change the renderer default or claim
that the native map is now smooth at a full-sized window.

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

Before the external capture below, GPU/playback/display timing was blocked. The pinned
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
exclude setup, warmup, and renderer switches. The benchmark now writes
`pass-windows.csv` with PID, repeat, scene, motion, renderer, measured start UTC,
and last acknowledged callback UTC. Each pass brackets an `Instant.now()` reading
with monotonic readings and maps callback times from their midpoint; the recorded
`clock_bracket_ns` describes that sampling bracket, not total wall-clock accuracy.
These are callback workload boundaries, not final-presentation boundaries. Account
for trailing native playback/presents separately and do not silently include the
next renderer's work. PresentMon `--date_time` output must be normalized to UTC
before joining. Capture analysis and runtime verification of these markers remain
pending; the instrumentation compiles with `:MekHQ:compileSkikoStressJava`.

The user has prepared a separate elevated PowerShell terminal. Installed collector:
`C:\Program Files\Intel\PresentMon\PresentMonConsoleApplication\PresentMon-2.5.1-x64.exe`.
Its `--help` confirms `--process_name`, `--date_time`, `--v2_metrics`,
`--no_track_input`, `--timed`, and `--terminate_after_timed` support. Start capture
before launching the benchmark, then filter Java process rows by its recorded PID
and swapchain. Leave GPU/display tracking enabled and retain dropped frames.
Do not claim step 1 is complete or proceed to retained-layer conclusions yet.

### First external PresentMon capture: diagnostic only

Benchmark `20260914-215556` completed eight cartography/HPG detail-pan passes,
two opposite renderer orders, 240 callbacks per pass, PID 35044. All native passes
reported Direct3D on Intel(R) Graphics. The new pass-window timestamps were written
for all eight passes; clock sampling brackets ranged from 6.1 to 25.1 microseconds.
Capture `presentmon-20260914-215455.csv` contains 1,286 rows, all for that PID,
across four DXGI swapchains consistent with the four newly created native surfaces.
No Java2D presentation rows were captured. That absence does not identify Java2D's
pipeline or establish that Java2D rendering is entirely CPU-based.

The user reported **1,277 lost ETW events** when stopping the collector. Their
timing is unknown; do not treat any pass as loss-free or missing display values as
proven dropped frames. This capture is not an accepted displayed-FPS comparison.

PresentMon 2.5.1's live `--date_time` conversion also applies the local offset twice:
`PMTraceSession::Start` stores a local FILETIME and `TimestampToLocalSystemTime`
converts it to local time again. See the pinned
[source](https://github.com/GameTechDev/PresentMon/blob/v2.5.1/PresentData/PresentMonTraceSession.cpp).
The verified offset for this capture is UTC-7; printed times were corrected by
adding 14 hours to obtain UTC, not by ordinary local-to-UTC conversion alone.
Future captures should use QPC timestamps with a bracketed QPC/UTC anchor instead.

For provisional diagnosis, select frames with corrected CPU start at least 100 ms
after measured start and CPU start plus `CPUBusy` at least 100 ms before the last
callback. Each native interval selects exactly one swapchain. Results below are
medians across rows, with ranges spanning the two repeats; all values are ms.

| Native scene | Rows per repeat | Frame time | GPU busy | GPU latency | Present CPU wait |
| --- | --- | --- | --- | --- | --- |
| Cartography | 231 / 233 | 22.01-22.71 | 4.86-4.92 | 16.92-17.47 | 0.097-0.099 |
| HPG | 234 / 234 | 33.78-34.32 | 6.58-7.04 | 27.03-27.40 | 0.112-0.113 |

These metrics suggest investigating work or delay before GPU execution rather
than assuming GPU saturation. `GPULatency` includes CPU generation/submission and
queue delay; `CPUBusy` is not thread CPU utilization. They do not isolate native
playback, fence waits, or vsync, and differences of medians do not form a timing
breakdown. HPG has 2 / 3 rows with unknown display timing inside these windows.
ETW loss and hardware-scheduling accuracy caveats prevent firm attribution. The
same-run JFR summary invocation returned no results and adds no new evidence.
Next: a quiet-console, QPC-correlated capture with no reported ETW loss, plus
independent identification of Java2D's active pipeline. Step 1 remains incomplete.

### QPC retry and synchronized native submission

Benchmark `20260914-220441` repeated the same eight passes successfully, PID 7876,
with Intel Direct3D unchanged. Capture `presentmon-20260914-220401.csv` used
`--qpc_time --no_console_stats` and its companion `-clock.json` stores a bracketed
Stopwatch QPC/UTC anchor. The user reported **308 lost ETW events** on stop, so this
retry also fails the loss-free capture criterion. Do not infer that quiet console
output caused the reduction or keep repeating this unchanged capture procedure.

The anchor bracket is 2.0675 ms. A later independent QPC/UTC reading agreed within
0.6514 ms. PowerShell automatically parses the JSON UTC value as `System.DateTime`;
preserve it with `([datetimeoffset]$anchor.utc).UtcDateTime`. Passing that typed value
through `DateTimeOffset.Parse` first coerces it to text and lost fractional seconds
in this environment. Initial misaligned summaries from that parser error were
discarded. No actual system clock jump was established.

With the corrected anchor and the same 100 ms boundary guards, the 1,286 PID rows
resolve to four native swapchains and no Java2D rows inside measured intervals.
Each guarded native pass uses exactly one swapchain. Provisional medians (ms):

| Native scene | Rows per repeat | Frame time | GPU busy | GPU latency | Present CPU wait |
| --- | --- | --- | --- | --- | --- |
| Cartography | 231 / 231 | 21.57-21.86 | 4.25-4.65 | 17.19-17.38 | 0.096-0.098 |
| HPG | 235 / 235 | 36.79-37.58 | 8.51-8.55 | 28.26-29.06 | 0.109-0.121 |

Both HPG intervals contain two unknown display rows; cartography contains none.
Event loss prevents interpreting these as complete dropped-frame statistics or
reporting accepted displayed FPS. Hardware scheduling accuracy is not verified.

The same-run streaming JFR summary completed. Across the entire recording,
including setup/warmup, native method sample counts on the Skiko dispatcher were
472 in `Direct3DContextHandler.flush`, 124 in `CanvasKt._nDrawPicture`, and 3 in
`Direct3DRedrawer.swap`. These are samples, not method durations or CPU utilization.
Java2D samples include Marlin rasterization and software transform/blit loops;
that identifies some CPU rendering work, not the complete active display pipeline.

Pinned Skiko 0.150.1
[native flush source](https://github.com/JetBrains/skiko/blob/v0.150.1/skiko/src/awtMain/cpp/windows/direct3DContext.cc)
calls `context->flush(surface, ...); context->submit(GrSyncCpu::kYes);` on each
flush. Thus this stage explicitly requests synchronous CPU/GPU completion, and
its sampled time can include both flush work and waiting. A short PresentMon
`CPUWait` does not rule out waiting here before the Present call. This is a concrete
reason to instrument playback and synchronized submission separately, not proof
that synchronization alone explains the regression. Do not change the flag
without checking buffer lifetime and fence requirements.

Recommended next measurement is direct timing of those native stages (or a trace
with CPU stacks and GPU queues) plus Java2D pipeline identification. Keep the
Intel baseline; no GPU preference, vsync, renderer, or dependency change was made.
Step 1 is still incomplete, and no retained-layer optimization is selected yet.

### Direct stage timing

Run `20260914-221208` completed eight instrumented cartography/HPG detail-pan passes
with 240 callbacks each at 1018 x 549 logical pixels. All native passes remained on
Intel Direct3D. The benchmark-only Byte Buddy 1.17.7 startup agent successfully
transformed the three intended classes. It times the existing
`SkiaLayer.draw$skiko(Canvas)` playback wrapper and
`Direct3DContextHandler.flush(LayerDrawScope)` synchronized native-flush wrapper;
it does not replace calls, alter synchronization, or rebuild the native library.

`mekhq.SkikoStage` duration events and `mekhq.MapMeasuredPass` intervals share JFR's
clock. The analyzer includes only stages fully within a measured pass, with 100 ms
guards at both ends, and rejects reported `jdk.DataLoss`, failed stages, or missing
expected stage names. It found 2,588 stage events and eight pass intervals, with
no reported JFR data loss. Unlike the preceding ETW captures, these are direct
method durations, not native sample-count estimates. They still are not displayed
FPS or a measurement of GPU execution alone.

| Scene | Stage | Count per repeat | Wall p50 (ms) | Wall p95 (ms) | Aggregate wall (ms) | Aggregate thread CPU (ms) |
| --- | --- | --- | --- | --- | --- | --- |
| Cartography | Playback | 233 / 233 | 3.14-3.33 | 4.39-4.55 | 765-802 | 594-781 |
| Cartography | Synchronized flush | 232 / 233 | 16.63-18.33 | 30.18-32.13 | 3843-4034 | 2906-3219 |
| HPG | Playback | 235 / 236 | 9.38-9.69 | 11.85-12.92 | 2223-2312 | 1891-2188 |
| HPG | Synchronized flush | 235 / 235 | 23.29-24.56 | 40.04-40.71 | 5875-6013 | 4563-4672 |

Ranges span two repeats, not confidence intervals. Boundary exclusion can yield
different counts for sequential stages; do not add medians or treat those rows as
paired frames. Thread CPU counters have coarse Windows granularity; aggregate
values provide supporting evidence only. Wall minus thread CPU is not GPU wait:
it can include scheduler delays and other blocking, while driver spinning counts
as CPU time. Instrumentation and JFR add overhead; that overhead has not been
isolated in a controlled paired run. In particular, instrumented HPG callback
throughput was 21.0-23.2/sec, slower than the preceding separate capture run.

The actual Java2D paint graphics reported `sun.awt.image.BufImgSurfaceData` and
`BufferedImageGraphicsConfig`; the screen graphics reported
`sun.java2d.windows.GDIWindowSurfaceData` and `sun.awt.Win32GraphicsConfig`.
Together with prior Marlin/software-loop samples, this identifies the observed
Java2D map as buffered-image rendering with GDI window output, not an observed
Java2D Direct3D swapchain. Windows desktop composition may still use the GPU;
this does not mean the entire display path is CPU-only. Surface inspection occurs
once per pass, outside steady-state timing. The final diagnostic code persists
the identities to `java2d-surfaces.txt`; this persistence follow-up compiled after
the measured run, whose identities were observed in console output.

To reproduce, select `SKIKO_PERF_SCENES=cartography,hpg`,
`SKIKO_PERF_MOTIONS=detail-pan`, and `SKIKO_PERF_PACING=throughput`, preserving and
restoring existing environment values. From the repository root:

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke -PskikoStageTiming --args=--performance
java MekHQ/experimental/skiko/src/mekhq/gui/SkikoMapProfile.java MekHQ/build/skiko-perf/<run>/renderers.jfr --stages
```

The agent is packaged by `:MekHQ:skikoStageAgent`, attached only when the Gradle
property is present, and opens `java.desktop/sun.java2d` only in that diagnostic
process. Byte Buddy stays in the experimental configuration. Normal runtime and
distributions are unchanged. `stages.csv` contains per-stage timestamps, thread,
duration, and CPU delta, grouped by native measured pass. The final analyzer was
rerun successfully with strict stage-name and JFR-loss checks; the final source
and agent package compiled successfully. No unit suites or publication gates ran.

Conclusion: expensive work exists in both playback and synchronized flush; simply
assuming an idle CPU waiting on a saturated GPU is not supported. Removing the
synchronous-submit flag is not justified by these timings. An exact three-way
split of native flush, submission, and GPU-completion waiting requires additional
native instrumentation; the packaged Java agent cannot see inside that JNI call.
A bounded GPU-backed retained-cartography prototype is now a reasonable next
optimization experiment to reduce repeated drawing and flush work, with Intel
as the target. It is not implemented here. Clean displayed-FPS comparison and
the internal native wait split remain outstanding; step 1 is only partially complete.

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
