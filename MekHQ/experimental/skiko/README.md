# Swing / Skiko map prototypes

These developer-only prototypes use Skiko 0.150.1 from Java, without Compose.
The native implementations, resources, and dependencies are excluded from normal
application runtime and distributions. The normal map remains Java2D; portraits
are unchanged.

## Performance comparison

Run `./gradlew.bat :MekHQ:runSkikoMapSmoke --args=--performance` from the repository
root with an available desktop. This runs a finite paired Java2D/native comparison
and writes CSV measurements and a JFR recording under `MekHQ/build/skiko-perf/`.
It does not load or save a campaign. Do not minimize, resize, or interact with the
benchmark window while it runs.

See [PERFORMANCE.md](PERFORMANCE.md) for methodology, isolated-layer commands,
measured results, and limitations. Viewport-bounded explicit HPG dash batches
improved detail delivery from about 8.5 to 27-28 callbacks/sec, but native playback
still trails Java2D in heavy-overlay cases. These results do not
justify changing the default renderer or claiming a presented-frame FPS advantage.

Add `-PskikoStageTiming` to the performance command to enable the benchmark-only
startup Java agent. It records playback and synchronized-flush duration events in
JFR and inspects Java2D paint/screen surfaces without changing rendering calls,
GPU selection, or synchronization. Analyze a recording with:

```powershell
java MekHQ/experimental/skiko/src/mekhq/gui/SkikoMapProfile.java MekHQ/build/skiko-perf/<run>/renderers.jfr --stages
```

The analyzer writes `stages.csv` and rejects reported JFR data loss or missing
expected stages. Native flush and GPU-completion waiting remain a combined metric.
The agent uses Byte Buddy only in the experimental source set; normal launches
do not attach it or open Java2D internals. See the performance report for overhead
and boundary limitations.

## Compare maps in a campaign

To try cached territories during camera movement, launch with
`SKIKO_MOTION_CACHE=true` (this also enables territory caching):

```powershell
$previous = $env:SKIKO_MOTION_CACHE
try {
   $env:SKIKO_MOTION_CACHE = 'true'
   .\gradlew.bat :MekHQ:runSkikoMap
} finally {
   $env:SKIKO_MOTION_CACHE = $previous
}
```

Fractional pans reuse the tile while moving; mouse release or 120 ms of camera
inactivity requests sharp vector rendering. Only territories are cached, not
stars, labels, or HPG links. Zoom changes rebuild the tile and can still stutter.
The 24-fixture motion smoke passes with exact settled output and automatic timer
settling. Use `--args="--offscreen --retained-only"` with the smoke task and the
same environment setting to run it. Desktop visual acceptance remains separate.

The optional `SKIKO_RETAIN_TERRITORIES=true` environment setting enables a bounded
native picture-shader territory cache. It reuses tiles only for device-pixel-aligned
pans and draws direct vectors for fractional pans, without snapping the camera.
It is disabled by default and has no validated performance gain yet. With the
setting enabled, run `:MekHQ:runSkikoMapSmoke --args="--offscreen --retained-only"`
to check raster fidelity, reuse/fallback selection, and disposal. The 36-fixture
raster check passes; it is not desktop GPU pixel validation. See the performance
report for the pinned JNI workaround and the rejected blank-cache benchmark.

From the MekHQ repository root:

```powershell
.\gradlew.bat :MekHQ:runSkikoMap
```

Open a campaign, go to Navigation / Map, and toggle **View > Skia Map
(Experimental)**. Unchecked is the complete Java2D map; checked is the separate
native `SkiaMap`. The setting is session-only and always starts unchecked. Normal
launches do not expose the toggle. No restart or campaign reload is needed to
switch renderers.

The native map draws compact faction-colored contacts at overview zoom, fading
into intrinsic spectral-color stars with luminous cores and radial auras at detail
zoom. Spectral palettes, luminosity-class scaling, and logarithmic marker sizing
share Java2D's calculations. Dated ownership rings replace contact colors as detail
appears; disputed systems divide contacts and rings into equal faction sectors.
Empty systems use muted contacts and optional gray rings; hidden empty route stops
retain neutral navigation contacts. Native hit-testing and
navigation-ring clearance account for the larger stellar footprint.

Radial glows use native Skia shaders cached by spectral color, reused across camera
changes, and disposed with the surface. The pinned Skiko 0.150.1 radial-gradient
factory has a Kotlin-mangled JVM name; a small reflective bridge invokes it from
Java. Failure follows the existing Java2D fallback. No Java2D stellar images are
uploaded to the renderer, and this adds no idle animation timer.

Selected systems use amber rings at distant zoom and corner brackets at navigation
zoom. Hover uses cyan rings/brackets immediately, independently of the delayed
tooltip, and is suppressed on the selected system. Both follow Java2D's semantic
zoom crossfade and shared corner geometry. Changing selection contracts the hover
brackets into amber selected brackets with Java2D's 260 ms ease-out timing. Repeated
selection does not replay the effect. The finite timer stops on completion, hiding,
or surface disposal; showing the map again does not replay old feedback. Label
placement reserves space for both focus states.

The settled selection glow is static: Java2D's subtle ambient breathing is not
ported, so there is still no continuous idle render timer. Stellar-class label
suffixes remain pending. Route and fleet behavior is described below.
Drag to pan, use the wheel to zoom around the pointer, and
click a star to update the existing sidebar. Arrow keys pan; plus/minus zoom.
System search and Center on Fleet also work. Switching preserves center, zoom,
and selected system. The inactive map is removed from the component hierarchy;
native surfaces are disposed when removed and recreated when needed.

Labels use the same campaign-date names and ordinary-label zoom fade as Java2D.
System culling includes 32 GUI-scaled pixels of overscan beyond the artwork radius
on every viewport edge; the canvas clips the final drawing. The pan smoke checks
all four edges at overview and detail zoom. Live GPU edge popping still needs
confirmation against the reported case.
Selected/current labels are placed first and remain visible at overview zoom;
route-system labels follow, then ordinary labels. All labels stay 16 GUI-scaled
pixels to the right of their planet, vertically centered, regardless of pan, zoom,
selection, or hover. They clip at viewport edges without switching sides. Zoom
visibility fades remain unchanged. Labels no longer avoid collisions or disappear
because a placement is crowded, so they can overlap labels or map markers.
Priority labels retain their background plate. Text measurements remain cached;
the per-frame candidate search and label collision scans have been removed.
The offscreen smoke checks fixed offsets during pan, zoom, and marker interactions;
the change has not yet been performance-benchmarked.

Territory fills and borders reuse the existing campaign-date territory atlas as
vector geometry. Sovereign regions use subdued faction colors; disputed regions
use native clipped diagonal bands and dashed neutral borders. Enclaves and
unclaimed pockets retain distinct boundary treatments. Curves, closed contours,
and winding rules are converted to native paths, cached across pan/zoom, and
released when the native surface is removed. Date/data changes rebuild the cache.
Only viewport-intersecting contours are drawn, beneath routes, stars, and labels.

If the dated atlas is missing, the snapshot prepares it on the EDT using the
existing map implementation, then reuses it. First display or date changes can
therefore still pause for atlas preparation; this is not an asynchronous geometry
pipeline or a performance benchmark.

Faction emblems use the existing dated asset resolver and territory anchors.
Skia decodes the original PNGs, tints their alpha masks, and draws aspect-preserving
images with the existing priority, minimum-area, collision, and zoom-fade rules.
Decoded images are cached across camera changes and disposed on atlas replacement
or native surface removal. Missing assets are skipped rather than replaced with
invented artwork. Administrative regions use solid borders; district detail adds
dashed boundaries. These reuse the atlas's vector paths, not a Java2D render.

The experimental launch adds shared View controls for **Show Territories**,
**Show Faction Emblems**, and **Administrative Borders** (Off, Regions, or Regions
and Districts). They stay synchronized with the Java2D Layers controls, including
the new Faction Emblems checkbox. Territories and emblems start enabled;
administrative borders start off. These choices survive renderer switches but
are session-only.

**View > Capitals** selects Off, National, National and Regional, or All Capitals.
It shares Java2D's capital checkbox/detail setting and starts at National. Dated
faction capitals take precedence over planetary regional/district designations;
multiple faction capitals retain their individual faction colors. National capitals
use five-point stars, regional capitals four-point stars, and districts small dots.
Capital names receive priority over ordinary system names. National and regional
markers remain at overview zoom; district detail fades out. Crowded markers that
cannot fit clear of contacts, other landmarks, and labels are omitted.

**View > Show Recharge Stations** adds battery symbols without changing the map
mode or faction colors. One or two interior bars represent the dated station count.
This overlay starts off, shares the Recharge Stations checkbox in Java2D's Layers
panel, and fades in at detail zoom. Both landmark settings survive renderer switches
but are session-only. System hover summaries include capital status and recharge
station counts. Recharge-time calculations and route-planner rules are unchanged.

Planned routes use 2 px dashed cyan lines; active routes use 3 px solid amber lines
(both GUI-scaled). Complete stop rings fade in at navigation detail using Java2D's
shared clearance geometry and thin planned/thicker active strokes. Overview keeps
only the endpoint ring. Requested planned stops receive sequential cyan badges;
active-route stops receive amber numbers. A planned badge takes priority when both
routes share its slot. Crowded badges are omitted; fixed labels may overlap them.
Both paths can be displayed together, and
the existing HUD/sidebar controls still own planning, editing, and beginning transit.
Switching renderers does not alter either route.

New plans reveal leg by leg with Java2D's adaptive duration and ease-out leg timing.
Beginning the matching plan sweeps cyan into amber over 550 ms, including the stop
rings and active badges. Fleet system changes during a trip use 520 ms departure
and arrival fades with endpoint shimmer, not travel along the interstellar line.
These finite timers stop when settled, hidden, or detached. Refreshing the same
route, panning, or reattaching the renderer does not replay completed feedback.

The current fleet is an amber JumpShip above-right of its system at navigation
detail, crossfading with an amber ring at overview. Skia decodes the existing fleet
PNG directly, applies native grayscale/amber lighting filters, rotates it, and
uses cubic resampling. The image and filter are disposed with the native surface;
a missing image uses a small chevron fallback. No Java2D fleet bitmap is uploaded.
Native image tint/filter rounding and font metrics are not exact Java2D pixel
parity. Fleet and shimmer bounds are reserved against labels.

Alt-click plots a course; Shift-click appends a waypoint. Dragging with either
modifier still pans without editing the route. Double-click opens the planetary
map. Right-click opens a heavyweight navigation menu with plot, append, trim,
remove, clear, cancel-trip, measurement, zoom, and centering actions. The context
menu key or Shift+F10 opens it at the selected system.

Choose Measure Distance in that menu, click a start system, and hover another
system to preview the distance, minimum standard jumps, and navigation assessment.
A second click locks the endpoint; another click starts a new measurement. Escape
stops measuring and dismisses the menu. Measurement state survives renderer
switches. Native hover summaries use dated system names and navigation assessments;
they are not full Java2D tooltip-detail parity. Hover and measurement boxes avoid
each other and stay within the viewport; hover is omitted if both cannot fit.

The selected-system jump-range circle honors the existing radius visibility,
minimum zoom, and color options. Planned and active routes reuse the campaign's
leg assessments for caution triangles and blocked-leg squares/dashed segments.
Transit changes refresh these assessments as well as the displayed route.

While the fleet is in in-system transit, an arc around its current system shows
its proximity to the destination world, using the campaign's transit fraction.
The arc decreases outbound toward the jump point and increases inbound toward
the world; it is not overall route completion or continuous travel between stars.
It refreshes on fleet transit events and campaign-day updates without an idle
animation timer. Arrival removes the arc; overview suppresses it. This additional
native transit arc is not a Java2D visual-parity claim.

**View > Show Empty Systems** controls both renderers and stays synchronized with
the Java2D Layers checkbox. Empty systems are hidden by default, except selected,
current, and route systems. Hidden systems are excluded from native hit-testing.
Names, faction colors, and empty-system classification reuse a cached read-only
snapshot of Java2D's prepared campaign data; drawing remains entirely native.

This replaces the earlier hybrid experiment: no Java2D map painting or raster-frame
uploads run inside the native renderer; emblem and fleet PNGs are ordinary native image
assets. The shared Layers and renderer-aware legend controls are available in
Skia mode as described below. Other service/mission markers, other overlays,
map export, and GM context actions have not been ported. Shared View controls and
the navigation options described above apply to both renderers. Switch back to
Java2D for the complete map. Native initialization or drawing failures return to
Java2D and disable the experimental toggle for that campaign window.

### Analytical map layers

With the experimental launch enabled, **View > MAP LAYER** selects Faction or any
of the eleven analytical modes: Technology, Industry, Raw Materials, Output,
Agriculture, Population, HPG, Recharge Stations, Academies, Hiring Halls, and Disease
Outbreaks. The menu and both renderers' Layers controls share one selected mode, preserved
across renderer switches. Labels and tooltips reuse the existing localized strings.

Native contacts and detail rings use Java2D's dated color resolver, including its
missing-data and zero-population behavior. Empty overview contacts remain muted;
hidden route stops remain neutral navigation contacts. Intrinsic stellar colors
are unchanged. Service modes use the existing service-ring zoom threshold; other
analytical rings use stellar detail zoom. Faction territories and emblems retain
their own colors. Switching mode is immediate in Skia; Java2D's animated palette
transition is not ported here.

The snapshot is reused for unchanged prepared data and mode, and rebuilt when
either changes. The focused smoke cycles all twelve modes through the real menu
at two campaign dates, compares every analytical snapshot color with Java2D,
checks native contact/ring and stellar-core pixels, and verifies renderer round
trips and snapshot reuse:

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args="--offscreen --modes-only"
```

This also writes native Population and Recharge Stations captures. It does not
establish desktop compositing or accessibility acceptance.

After recovery of an interrupted editor session, the analytical pass completed
both the full offscreen and desktop smoke sequences on Windows Direct3D at 175%
native content scale. The desktop run reported zero failures, 80 tab-hover samples,
and six final renderer switches. Analytical palette pixels are checked on native
offscreen surfaces even during the desktop run; this is not exact Java2D raster
or operating-system accessibility parity.

### Shared Layers and legend

The HUD Layers button opens the existing animated drawer in Java2D and an owned,
modeless Swing window in Skia. The latter keeps Swing controls off the heavyweight
native map surface while leaving the map interactive. Both reuse the same controls
and state: all twelve map modes, empty systems, territories, faction emblems,
administrative detail, recharge markers, capital detail, reachability, and measurement. The View
menu remains synchronized. HPG-network controls are also available; only the
Operations control remains hidden in the native window, with its selection
preserved for Java2D.

The information button opens the existing tabbed symbol legend, filtered for the
active renderer. Skia omits unsupported overlays and markers, plus measurement and
blocked-leg entries whose Java2D swatches do not yet match native styling. Native
recharge-marker and transit-arc legend entries are also pending. The legend reuses
Java2D symbol swatches; it is not a pixel-parity claim or a native map-frame upload.

Layers and legend can remain open independently. Escape, the window close button,
or the corresponding HUD button dismisses them. Renderer switches, fallback, and
leaving the map close both windows and restore the controls to the Java2D drawer.

The focused smoke uses the real HUD actions to check shared selections, all
supported overlay toggles, measurement updates, detail selectors and their popup,
filtered/full legends, window ownership, dismissal, and renderer/tab lifecycle:

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args="--offscreen --controls-only"
```

Omit `--offscreen` for desktop captures. Offscreen captures exercise real Swing
windows and native snapshots, but do not validate desktop compositing or keyboard
focus delivery. The same controls checks also run in the full smoke sequence.

The focused controls pass completed with zero failures in both offscreen and
desktop modes on Windows Direct3D at 175% native content scale. Desktop captures
show the Layers window, capital-detail popup, and filtered legend over the map.
These checks invoke Swing actions directly; operating-system keyboard focus and
screen-reader acceptance remain manual checks.

### Reachability

The shared Layers checkbox enables reachability from the selected system, or the
fleet's current system when there is no selection. The Hops spinner selects one
through three minimum-hop shells. Skia consumes the same cached campaign analysis
as Java2D; it does not implement another pathfinder or change routing rules.
Selection, routing constraints, and the normal campaign/date navigation refresh
recalculate the snapshot. Unchanged draws reuse the converted snapshot.

At navigation detail, one-hop systems have cyan circles, two-hop systems muted
blue-gray squares, and three-hop systems hexagons. Amber triangles replace ordinary
shells for cautions; dashed red diamonds show the blocked frontier. Geometry,
colors, strokes, and zoom fading reuse Java2D helpers. Hidden empty systems omit
optional reachability markers except for the selected/current system. Hidden empty
route stops can remain navigation contacts without gaining a reachability marker.

Shell numbers and the dated anchor/hop annotation reserve label space and are
omitted where they cannot fit; crowded neighboring shell outlines may intersect.
Skia font metrics and antialiasing are not exact Java2D raster parity. The legend
now includes reachability shells, cautions, and blocked-frontier entries.
Disabling reachability clears the native overlay; the choice and hop count survive
renderer switches. Native paths are drawn and closed locally with no image uploads
or continuous animation timer.

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args="--offscreen --reachability-only"
```

This focused smoke compares every snapshot entry with the campaign calculation
for all three hop limits, exercises anchor and route-option changes, and checks
native marker pixels, fixed label anchors, zoom fading, empty filtering, and renderer
round trips. It writes hop-shell and caution captures. Omit `--offscreen` for
desktop compositing captures; the same checks run in the full smoke sequence.

The reachability pass completed with zero failures in the focused offscreen and
desktop checks on Windows Direct3D at 175% native content scale. The integrated
offscreen sequence also passed all controls, analytical, stellar, focus,
cartography, landmark, route, and interaction checks, including six final renderer
switches. Abandonment-avoidance changes now trigger the same immediate navigation
refresh as the command-circuit option in both renderers.

### HPG network and stations

The shared Layers **HPG Network** checkbox enables native links and station badges
without changing the analytical map mode. It starts off, with **A-B network** as
the selected detail. **A only**, **A-B network**, and **A-D stations** retain the
same state in both renderers. The native legend now includes links and stations.

Skia reuses Java2D's dated `Systems.getHPGNetwork` results without modifying or
simplifying the network topology. Class A links are solid cyan at 1.35 GUI-scaled
pixels; Class B links are muted blue, 0.8 pixels wide, with 8/8 dashes. Links are
culled against the viewport even when both endpoints are offscreen, and are drawn
beneath routes, stars, and badges. The empty-system filter does not alter network
topology, matching Java2D.

Wide zoom limits the network to A links. Medium zoom allows B links and A badges;
close zoom reveals lower-class badges within the selected detail limit. Hexagonal
badges sit left of their system using the shared marker layout: A is cyan, B blue,
C amber, and D red. Their class letter uses native font metrics. Badge geometry,
colors, sizing, and zoom fades reuse Java2D helpers. Crowded badges that cannot fit
inside the viewport clear of other markers are omitted, and labels reserve their
bounds. Station visibility follows the native system/empty filter.

Dated station/link snapshots are reused across pan and zoom; date/prepared-data
changes rebuild them. Detail changes reuse the underlying lists. Disabling the
overlay clears native output immediately; Java2D's checkbox-transition animation
is not ported. Class B links use viewport-bounded explicit dash batches with the
original phase, avoiding expensive native dash effects. Native paths are disposed after drawing, with no
bitmap uploads or continuous idle timer. Font/raster output is not exact Java2D
pixel parity. The separate 50-ly HPG range ring remains pending.

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args="--offscreen --hpg-only"
```

The focused check compares full dated link and station snapshots with the shared
provider at two dates, inspects real A-D badge pixels, and checks detail limits,
Class B pixel contribution, zoom fading, empty filtering, fixed label anchors,
snapshot reuse, renderer switches, and clearing on disable. It writes overview,
station-class, and historical captures. Omit `--offscreen` for desktop captures.
These checks also run in the full smoke sequence.

The HPG pass completed with zero failures in focused offscreen and desktop runs
on Windows Direct3D at 175% native content scale. The full offscreen sequence also
passed, including shared controls, reachability, all existing rendering and
navigation checks, and six final renderer switches. Desktop captures show the
overview network and all four station classes; operating-system accessibility
acceptance remains a manual check.

### Real-map smoke check

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke
```

This creates a fresh in-memory campaign and opens the real campaign UI without
loading or saving campaign files. It verifies Java2D is the default, uses the real
menu checkbox, checks native stars and pan/zoom/selection, verifies camera and
selection round trips, changes tabs, resizes, and checks native peer disposal.
It checks fixed native label offsets at detailed/overview zoom and viewport edges, compares
dated names and faction colors with campaign data before and after a temporary
date change, and verifies empty-system filter changes across renderer switches.
Stellar checks compare isolated native core/aura pixels against Java2D's actual
intrinsic-star painter across available spectral/luminosity variants, excluding
the separate ownership-ring band and allowing bounded rasterization differences.
The comparison sheet places Java2D on the left and Skia on the right of each pair;
only the native sprite includes its ownership ring. Intermediate-zoom checks verify
the complementary contact/star crossfade, shader reuse, and surface disposal and
recreation. Desktop runs also capture both maps at the same camera and date.
Focus checks compare native rings, hover brackets, and selection animation samples
with Java2D's painters using its pure-stroke hint to preserve subpixel positions.
Java2D's default stroke normalization can otherwise snap thin strokes differently.
The checks exercise immediate hover, selected-system hover suppression, intermediate
animation frames, repeat selection, fixed label anchors during focus changes, ring/bracket zoom
crossfade, and timer cleanup across hiding and renderer switches.
Territory checks compare sampled native interior pixels with expected faction
tints at detailed/overview zoom and a historical date, verify snapshot/path reuse
across camera changes, capture a disputed region, and check native path disposal.
Emblem checks verify dated asset selection, nonoverlapping placement, and pixels
changed by visibility toggles. Region/district checks verify additional native
paths and pixel changes. The real menu actions change all shared layers across
renderer round trips; image and administrative-path disposal are also checked.
Landmark checks compare dated capital precedence/colors and recharge counts with
campaign data, exercise each capital hierarchy level and station visibility through
the real menus, verify changed pixels and nonoverlapping bounds, check zoom fade,
and preserve settings across Java2D/Skia round trips.
It also plots and appends a route through MapTab, checks planned/active route
colors in a desktop capture, exercises fleet progress updates and renderer round
trips, and verifies clearing, cancellation, arrival, and single-system paths.
Route checks sample rendered partial legs during reveal and activation, verify
requested-stop numbering, native fleet pixels and fixed label anchors, exercise a
fleet hop through in-memory campaign location events, and check overview/detail
crossfades. Hiding and detaching during feedback must stop timers and dispose the
fleet asset without replay on reattachment. The route slice can run independently:

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args="--offscreen --routes-only"
```

This focused mode still creates the real Swing window but skips desktop/tab-hover
pixel acceptance and unrelated map checks; omit `--offscreen` to capture desktop
pixels. Use the full run for integration coverage.

The route/fleet pass passed the focused offscreen and desktop checks and the full
desktop sequence on Windows Direct3D at 175% native content scale: zero failures,
80 tab-hover samples, and six final renderer toggles. Route sampling starts after
the rebuilt Swing map view has painted; the desktop blank-frame check remains
enabled for reveal, activation, and hop captures.

Navigation checks exercise modifier clicks, drag safety, route-menu edits,
heavyweight popup pixels, measurement preview/locking and renderer round trips,
nonoverlapping overlay bounds, Escape/context-menu key handlers, planetary-view
round trips, and blocked-leg refresh. Key-handler checks do not establish operating
system focus or accessibility behavior.
It samples sidebar pixels during 80 Map/Locations tab hovers and then performs
six additional renderer switches. It exits nonzero on failure.

The complete sequence, including emblems, administrative borders, shared controls,
and navigation interactions, passed both desktop and offscreen checks on Windows /
Direct3D at 175% display scale. The desktop run reported zero failures, 80 tab-hover
samples, and six additional renderer toggles, covering the tab-hover regression
that failed with the hybrid renderer. This is a bounded
integration check, not proof against every flicker, a memory-leak test, or
cross-platform acceptance. The two renderers currently draw different workloads,
so their timings are not a like-for-like performance comparison.

The subsequent capital/recharge-station pass also passed the expanded offscreen
and desktop sequences. The desktop run reported zero failures, 80 tab-hover
samples, and six additional renderer toggles.

The intrinsic-star pass has passed the expanded offscreen sequence with 38 stellar
variants: 22,697 of 22,704 core/aura samples were within 25 levels per color channel,
with mean absolute channel error 4.19. The expanded desktop sequence also passed
with zero failures, 80 tab-hover samples, and six additional renderer toggles.
Matched-camera captures show comparable stellar cores, glows, and ownership rings;
this does not establish full map, label, selection, or animation parity.

The subsequent focus-marker pass passed its expanded offscreen and desktop
sequences on Windows Direct3D at 175% native content scale, including focus/label
clearance, 80 desktop tab-hover samples, and six additional renderer toggles with
zero failures. Settled ambient breathing remains outside this pass.

The default smoke drivers require an unlocked interactive desktop and temporarily place
their windows on top. Do not interact with them during a run; the real-map check
also moves the pointer. Captures in `MekHQ/build/skiko-stress/` may include other
windows if the test is occluded. Inspect them before sharing.

### Unattended native checks

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke --args=--offscreen
```

This explicit alternative uses the same map drawing callback with a native Skia
raster surface for pixel assertions and PNG captures. It still creates the Swing
campaign window and native peers, and exercises menus, camera input, dated data,
route changes, navigation interactions, and surface recreation. Popup checks in
this mode verify behavior only, not popup pixels. It works without readable desktop pixels,
but is not a headless-JVM mode. It skips desktop capture and the tab-hover check;
its `OFFSCREEN_MAP_SMOKE_COMPLETE` report must not be treated as desktop GPU
compositing acceptance. Captures use the `offscreen-` prefix and contain only the
native map. No campaign files are loaded or saved.

## Isolated integration harness

The original synthetic harness remains available independently. It uses MekHQ's
localization facade, GUI scaling helper, and `JScrollablePanel`, but does not load
a campaign or use the production map.

From the MekHQ repository root:

```powershell
.\gradlew.bat :MekHQ:runSkikoStress
.\gradlew.bat :MekHQ:runSkikoStress --args=--smoke
```

### What the isolated smoke run checks

- Drawing callbacks and native lifecycle operations stay on the Swing EDT.
- Visible animation produces callbacks across three lifecycle cycles.
- Context popups open with both default/lightweight-requested and heavyweight
  modes. Swing may choose a heavyweight popup even when lightweight is requested.
- Application-modal dialogs open and close during animation.
- Tabs, viewport scrolling, visibility changes, resizing, and surface recreation
  execute without recorded exceptions.
- Captured desktop images contain the map background and star pixels, not just
  an unrelated foreground window.
- A fresh window produces frames after the original window and surface resources
  are disposed.

The status strip and console identify the actual backend (including fallback),
display scale, callback thread, draw-callback rate, and lifecycle generation.
Draw callbacks are **not** presented FPS. Gaps include intentionally hidden views
and synchronous screenshot capture; this is not a performance benchmark.

### Manual harness acceptance

1. Type into the callsign field over the animated canvas. Click Contact and check
   `OVERLAY_ACTION` in the console. Test Tab/Shift+Tab between controls and canvas.
2. Hover Contact and the callsign field; verify tooltips appear above the canvas.
3. Right-click the map in both popup modes. Exercise the combo dropdown and modal
   dialog; verify focus returns and input does not pass through to the canvas.
4. Drag the split divider, scroll, resize, minimize/restore, and switch tabs while
   animating. Check clipping and stale or blank areas. Toggle the Swing overlay.
5. Pause animation. The callback rate should settle near zero absent exposure,
   input, or resizing. Check process CPU/GPU usage rather than assuming idle power.
6. Close/reopen the window and recreate the surface repeatedly. Track native/process
   memory externally; this short smoke run is not a leak test.
7. Move between physical monitors with different DPI. Verify scale, text, mouse
   coordinates, and popup positions. Repeat on Windows, Linux, and macOS.

The harness deliberately preserves the heavyweight-canvas/lightweight-Swing
combination rather than hiding its limitations with offscreen image copies.
The separate real-map experiment above covers the `CampaignGUI` map hierarchy.

### Initial harness observation

The native smoke sequence completed on Intel Graphics using Direct3D at 175%
display scale, with EDT drawing callbacks, four surface generations followed by
a new window, and ten nonblank desktop captures. Opaque Swing overlays and a
context popup were visible in inspected captures. This does not establish input,
accessibility, mixed-monitor DPI, leak, cross-platform, or performance acceptance.
