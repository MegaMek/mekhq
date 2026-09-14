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

**View > Show Empty Systems** controls both renderers and stays synchronized with
the Java2D Layers checkbox. Empty systems are hidden by default, except selected,
current, and route systems. Hidden systems are excluded from native hit-testing.
Names, faction colors, and empty-system classification reuse a cached read-only
snapshot of Java2D's prepared campaign data; drawing remains entirely native.

This replaces the earlier hybrid experiment: no Java2D map painting or raster
uploads run inside the native renderer. Layers and legend controls are disabled
in Skia mode. Territories, route lines, analytical map modes, overlays, map export,
and other map gestures have not been ported. Apart from empty-system visibility,
Java2D layer settings do not apply to Skia. Switch back to
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
It samples sidebar pixels during 80 Map/Locations tab hovers and then performs
six additional renderer switches. It exits nonzero on failure.

The sequence passed on Windows / Direct3D at 175% display scale, including the
tab-hover regression that failed with the hybrid renderer. This is a bounded
integration check, not proof against every flicker, a memory-leak test, or
cross-platform acceptance. The two renderers currently draw different workloads,
so their timings are not a like-for-like performance comparison.

Both smoke drivers require an unlocked interactive desktop and temporarily place
their windows on top. Do not interact with them during a run; the real-map check
also moves the pointer. Captures in `MekHQ/build/skiko-stress/` may include other
windows if the test is occluded. Inspect them before sharing.

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
