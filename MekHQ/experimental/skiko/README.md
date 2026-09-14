# Swing / Skiko map prototypes

These developer-only prototypes use Skiko 0.150.1 from Java, without Compose.
The native implementations, resources, and dependencies are excluded from normal
application runtime and distributions. The normal map remains Java2D; portraits
are unchanged.

## Compare maps in a campaign

From the MekHQ repository root:

```powershell
.\gradlew.bat :MekHQ:runSkikoMap
```

Open a campaign, go to Navigation / Map, and toggle **View > Skia Map
(Experimental)**. Unchecked is the complete Java2D map; checked is the separate
native `SkiaMap`. The setting is session-only and always starts unchecked. Normal
launches do not expose the toggle. No restart or campaign reload is needed to
switch renderers.

The native map draws campaign stars with dated faction colors, native system-name
labels, the current-system highlight, and a selection ring. Disputed systems use
colored sectors for their factions. Drag to pan, use the wheel to zoom around the pointer, and
click a star to update the existing sidebar. Arrow keys pan; plus/minus zoom.
System search and Center on Fleet also work. Switching preserves center, zoom,
and selected system. The inactive map is removed from the component hierarchy;
native surfaces are disposed when removed and recreated when needed.

Labels use the same campaign-date names and ordinary-label zoom fade as Java2D.
Selected/current labels are placed first and remain visible at overview zoom;
route-system labels follow, then ordinary labels. Native font measurements keep
labels inside the viewport and prevent label overlap. Priority labels use a small
background plate over ordinary stars when needed; other labels avoid star markers.
Crowded labels with no valid placement are omitted.

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

Planned routes are dashed cyan; active routes are solid amber. Native waypoint
rings, double-ring destinations, and direction marks preserve the ordered paths
from the existing campaign planner. Both paths can be displayed together, and
the existing HUD/sidebar controls still own planning, editing, and beginning transit.
Switching renderers does not alter either route.

While the fleet is in in-system transit, an arc around its current system shows
its proximity to the destination world, using the campaign's transit fraction.
The arc decreases outbound toward the jump point and increases inbound toward
the world; it is not overall route completion or continuous travel between stars.
It refreshes on fleet transit events and campaign-day updates without an idle
animation timer. Arrival removes the arc. Route reveal animations, jump effects,
numbered waypoint badges, and route-constraint overlays are not yet ported.

**View > Show Empty Systems** controls both renderers and stays synchronized with
the Java2D Layers checkbox. Empty systems are hidden by default, except selected,
current, and route systems. Hidden systems are excluded from native hit-testing.
Names, faction colors, and empty-system classification reuse a cached read-only
snapshot of Java2D's prepared campaign data; drawing remains entirely native.

This replaces the earlier hybrid experiment: no Java2D map painting or raster-frame
uploads run inside the native renderer; emblem PNGs are ordinary native image
assets. The full Layers and legend controls are disabled
in Skia mode. Analytical map modes, other overlays, map export,
and other map gestures have not been ported. Only the shared View controls apply
to both renderers. Switch back to
Java2D for the complete map. Native initialization or drawing failures return to
Java2D and disable the experimental toggle for that campaign window.

### Real-map smoke check

```powershell
.\gradlew.bat :MekHQ:runSkikoMapSmoke
```

This creates a fresh in-memory campaign and opens the real campaign UI without
loading or saving campaign files. It verifies Java2D is the default, uses the real
menu checkbox, checks native stars and pan/zoom/selection, verifies camera and
selection round trips, changes tabs, resizes, and checks native peer disposal.
It checks native label bounds and collisions at detailed/overview zoom, compares
dated names and faction colors with campaign data before and after a temporary
date change, and verifies empty-system filter changes across renderer switches.
Territory checks compare sampled native interior pixels with expected faction
tints at detailed/overview zoom and a historical date, verify snapshot/path reuse
across camera changes, capture a disputed region, and check native path disposal.
Emblem checks verify dated asset selection, nonoverlapping placement, and pixels
changed by visibility toggles. Region/district checks verify additional native
paths and pixel changes. The real menu actions change all shared layers across
renderer round trips; image and administrative-path disposal are also checked.
It also plots and appends a route through MapTab, checks planned/active route
colors in a desktop capture, exercises fleet progress updates and renderer round
trips, and verifies clearing, cancellation, arrival, and single-system paths.
It samples sidebar pixels during 80 Map/Locations tab hovers and then performs
six additional renderer switches. It exits nonzero on failure.

The stars/labels/routes/territory sequence passed on Windows / Direct3D at 175%
display scale, including the tab-hover regression that failed with the hybrid
renderer. The later emblem/administrative/control pass has passed the offscreen
mode below; its desktop run was blocked by all-black desktop captures, including
Java2D. An unlocked-desktop rerun is still required for that pass. This is a bounded
integration check, not proof against every flicker, a memory-leak test, or
cross-platform acceptance. The two renderers currently draw different workloads,
so their timings are not a like-for-like performance comparison.

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
route changes, and surface recreation. It works without readable desktop pixels,
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
