/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.Timer;

import com.sun.management.ThreadMXBean;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQInternationalization;

public final class SkikoMapPerformance implements ExperimentalMapView.RenderObserver {
    private static final int WARMUP = 80;
    private static final int SAMPLES = 240;
    private final JFrame frame;
    private final MapTab tab;
    private final Campaign campaign;
    private final InterstellarMapPanel java2d;
    private final ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private final com.sun.management.OperatingSystemMXBean operatingSystem =
          (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final List<Pass> passes = new ArrayList<>();
    private final List<Sample> samples = new ArrayList<>();
    private final List<String> summaries = new ArrayList<>();
    private final List<String> raw = new ArrayList<>();
    private final Timer timer = new Timer(16, event -> tick());
    private final boolean throughput = "throughput".equals(System.getenv("SKIKO_PERF_PACING"));
    private final Path output;
    private int passIndex = -1;
    private int requests;
    private int coalesced;
    private long pending;
    private long lastTick;
    private long tickGap;
    private long started;
    private long allocated;
    private long cpu;
    private long passStart;
    private Instant passClockUtc;
    private long passClockNano;
    private long passClockBracket;
    private long lastCompleted;
    private long processCpuStart;
    private long gcStart;
    private long settleUntil;
    private boolean measuring;
    private SkiaMap skia;
    private int mapWidth;
    private int mapHeight;
    private jdk.jfr.Recording recording;
    private PassWindow passWindow;
    private VisibleFrames visibleFrames;

    private record VisibleSample(long time, long fingerprint) {
    }

    private static final class VisibleFrames {
        private final Rectangle region;
        private final Robot robot;
        private final List<VisibleSample> samples = new ArrayList<>();
        private final Thread thread;
        private volatile boolean running = true;
        private volatile RuntimeException failure;
        private BufferedImage firstImage;

        VisibleFrames(Component map) throws Exception {
            Point position = map.getLocationOnScreen();
            int width = Math.min(256, map.getWidth());
            int height = Math.min(128, map.getHeight());
            region = new Rectangle(position.x + (map.getWidth() - width) / 2,
                  position.y + (map.getHeight() - height) / 2, width, height);
            robot = new Robot(map.getGraphicsConfiguration().getDevice());
            thread = new Thread(() -> {
                try {
                    while (running) {
                        BufferedImage image = robot.createScreenCapture(region);
                        long time = System.nanoTime();
                        if (firstImage == null) {
                            firstImage = image;
                        }
                        long fingerprint = 0xcbf29ce484222325L;
                        for (int y = 0; y < image.getHeight(); y += 4) {
                            for (int x = 0; x < image.getWidth(); x += 4) {
                                fingerprint = (fingerprint ^ image.getRGB(x, y)) * 0x100000001b3L;
                            }
                        }
                        samples.add(new VisibleSample(time, fingerprint));
                    }
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            }, "map-visible-frame-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        String stop(Path output, Pass pass, long start, long end) throws Exception {
            running = false;
            thread.join(2000);
            if (thread.isAlive() || failure != null) {
                throw new IllegalStateException("Visible map capture did not complete", failure);
            }
            if (firstImage != null) {
                ImageIO.write(firstImage, "png", output.resolve(String.format(Locale.ROOT,
                        "visible-region-%d-%s-%s-%s.png", pass.repeat(), pass.scene(), pass.motion(),
                        pass.renderer())).toFile());
            }
            long lower = start + 100_000_000L;
            long upper = end - 100_000_000L;
            List<VisibleSample> measured = new ArrayList<>();
            for (VisibleSample sample : samples) {
                if (sample.time() >= lower && sample.time() <= upper) {
                    measured.add(sample);
                }
            }
            if (measured.size() < 2) {
                throw new IllegalStateException("Insufficient visible map samples: " + measured.size());
            }
            List<Long> samplingGaps = new ArrayList<>();
            List<Long> updateGaps = new ArrayList<>();
            int updates = 0;
            long lastUpdate = 0;
            for (int index = 1; index < measured.size(); index++) {
                VisibleSample previous = measured.get(index - 1);
                VisibleSample current = measured.get(index);
                samplingGaps.add(current.time() - previous.time());
                if (current.fingerprint() != previous.fingerprint()) {
                    updates++;
                    if (lastUpdate != 0) {
                        updateGaps.add(current.time() - lastUpdate);
                    }
                    lastUpdate = current.time();
                }
            }
            samplingGaps.sort(Long::compareTo);
            updateGaps.sort(Long::compareTo);
            double seconds = (measured.getLast().time() - measured.getFirst().time()) / 1e9;
            return String.format(Locale.ROOT, "%d,%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%.2f,%.3f,%.3f",
                pass.repeat(), pass.scene(), pass.motion(), pass.renderer(), region.x, region.y, measured.size(), updates,
                  measured.size() - updates - 1, (measured.size() - 1) / seconds, updates / seconds,
                  samplingGaps.get((int) Math.ceil(samplingGaps.size() * .95) - 1) / 1e6,
                  updateGaps.isEmpty() ? Double.NaN
                        : updateGaps.get((int) Math.ceil(updateGaps.size() * .95) - 1) / 1e6);
        }
    }

    @jdk.jfr.Name("mekhq.MapMeasuredPass")
    @jdk.jfr.StackTrace(false)
    public static final class PassWindow extends jdk.jfr.Event {
        public int repeat;
        public String scene;
        public String motion;
        public String renderer;
    }

    private record Pass(int repeat, String scene, String motion, boolean nativeMap) {
        String renderer() {
            return nativeMap ? "Skia" : "Java2D";
        }
    }

    private record Sample(long paint, long cpu, long allocated, long latency, long tickGap) {
    }

    public static void start(JFrame frame, MapTab tab, Campaign campaign) throws Exception {
        new SkikoMapPerformance(frame, tab, campaign).start();
    }

    private SkikoMapPerformance(JFrame frame, MapTab tab, Campaign campaign) throws Exception {
        this.frame = frame;
        this.tab = tab;
        this.campaign = campaign;
        tab.setExperimentalMapActive(false);
        java2d = find(tab, InterstellarMapPanel.class);
        if (java2d == null || !threads.isThreadAllocatedMemorySupported() || !threads.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("Performance counters or Java2D map unavailable");
        }
        java2d.setBorder(null);
        threads.setThreadAllocatedMemoryEnabled(true);
        threads.setThreadCpuTimeEnabled(true);
        output = Path.of("build", "skiko-perf", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(output);
        if (Boolean.getBoolean("mekhq.skiko.visibleFrames")) {
            Files.writeString(output.resolve("visible-frames.csv"),
                "repeat,scene,motion,renderer,region_x,region_y,samples,updates,duplicates,sample_hz,observed_update_hz,sample_gap_p95_ms,update_gap_p95_ms\n");
        }
          Files.writeString(output.resolve("pass-windows.csv"),
              "pid,repeat,scene,motion,renderer,start_utc,last_callback_utc,clock_bracket_ns\n");
        String pacing = System.getenv("SKIKO_PERF_PACING");
        if (pacing != null && !List.of("timer", "throughput").contains(pacing)) {
            throw new IllegalArgumentException("Invalid SKIKO_PERF_PACING: " + pacing);
        }
        for (int repeat = 1; repeat <= 2; repeat++) {
            for (String scene : selection("SKIKO_PERF_SCENES", "core,cartography,overlays",
                List.of("core", "cartography", "overlays", "administrative", "hpg", "landmarks", "reachability"))) {
                for (String motion : selection("SKIKO_PERF_MOTIONS", "overview-pan,detail-pan,zoom",
                    List.of("overview-pan", "detail-pan", "zoom"))) {
                    passes.add(new Pass(repeat, scene, motion, repeat == 2));
                    passes.add(new Pass(repeat, scene, motion, repeat != 2));
                }
            }
        }
        summaries.add("repeat,scene,motion,renderer,width,height,requests,frames,coalesced,elapsed_s,paint_p50_ms,paint_p95_ms,paint_p99_ms,paint_max_ms,cpu_p50_ms,alloc_p50_kib,alloc_total_mib,latency_p95_ms,tick_p95_ms,paint_over_16ms_pct,process_cpu_s,gc_ms");
        raw.add("repeat,scene,motion,renderer,frame,paint_ns,cpu_ns,allocated_bytes,request_to_complete_ns,tick_gap_ns");
    }

    private static List<String> selection(String variable, String defaults, List<String> allowed) {
        String value = System.getenv(variable);
        List<String> selected = List.of((value == null ? defaults : value).split(","));
        if (selected.isEmpty() || !allowed.containsAll(selected)) {
            throw new IllegalArgumentException("Invalid " + variable + ": " + value);
        }
        return selected;
    }

    private void start() throws Exception {
        recording = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
        recording.setDestination(output.resolve("renderers.jfr"));
        recording.setDumpOnExit(true);
        recording.start();
        var scale = frame.getGraphicsConfiguration().getDefaultTransform();
        Files.writeString(output.resolve("environment.txt"), "java=" + System.getProperty("java.runtime.version")
              + "\nos=" + System.getProperty("os.name") + " " + System.getProperty("os.version")
              + "\ncpus=" + Runtime.getRuntime().availableProcessors()
              + "\nheap_max=" + Runtime.getRuntime().maxMemory()
              + "\ndisplay_scale=" + scale.getScaleX() + "," + scale.getScaleY()
              + "\ndate=" + campaign.getLocalDate() + "\nanchor=" + campaign.getCurrentSystem().getId()
              + "\npid=" + ProcessHandle.current().pid()
              + "\nstage_timing=" + Boolean.getBoolean("mekhq.skiko.stageTiming")
              + "\nretained_territories=" + (!"false".equals(System.getenv("SKIKO_MOTION_CACHE"))
                  || "true".equals(System.getenv("SKIKO_RETAIN_TERRITORIES")))
              + "\nmotion_cache=" + !"false".equals(System.getenv("SKIKO_MOTION_CACHE"))
              + "\npacing=" + (throughput ? "throughput: one pending request, next EDT turn after callback" : "timer")
              + "\ntimer_ms=16\nwarmup_requests=" + WARMUP + "\nsample_requests=" + SAMPLES
              + "\nrender_order=Java2D,Skia then Skia,Java2D"
              + "\ncamera=integer logical-pixel pans; wheel-step zoom through Java2D interaction handler"
              + "\nprofiling=JFR profile enabled\nmetric=paint callback CPU work, not GPU presentation\n");
        nextPass();
        timer.start();
    }

    private void nextPass() throws Exception {
        measuring = false;
        passWindow = null;
        pending = 0;
        java2d.setPerformanceObserver(null);
        if (skia != null) {
            skia.setPerformanceObserver(null);
        }
        if (++passIndex == passes.size()) {
            finish();
            return;
        }
        Pass pass = passes.get(passIndex);
        tab.setExperimentalMapActive(false);
        tab.setMapMode(InterstellarMapPanel.MapMode.FACTION);
        tab.setShowingEmptySystems(false);
        boolean cartography = !pass.scene().equals("core");
        boolean overlays = pass.scene().equals("overlays");
          boolean administrative = overlays || pass.scene().equals("administrative");
          boolean landmarks = overlays || pass.scene().equals("landmarks");
        tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(cartography, cartography,
              administrative ? ExperimentalMapView.BoundaryDetail.DISTRICTS : ExperimentalMapView.BoundaryDetail.OFF));
          tab.setLandmarkLayers(new ExperimentalMapView.LandmarkLayers(landmarks
              ? ExperimentalMapView.CapitalDetail.DISTRICTS : ExperimentalMapView.CapitalDetail.OFF, landmarks));
          setCheck("map.overlay.hpgNetwork.text", overlays || pass.scene().equals("hpg"));
          setCheck("map.overlay.reachability.text", overlays || pass.scene().equals("reachability"));
        setCheck("map.overlay.operations.text", false);
        java2d.setSelectedSystem(campaign.getCurrentSystem());
        java2d.restoreMapCenter(new InterstellarMapPanel.MapCenter(campaign.getCurrentSystem().getX(),
              campaign.getCurrentSystem().getY()));
        java2d.restoreMapScale(pass.motion().equals("detail-pan") ? 6 : 0.8);
        if (pass.nativeMap() && !tab.setExperimentalMapActive(true)) {
            throw new IllegalStateException("Native renderer unavailable");
        }
        skia = find(tab, SkiaMap.class);
        frame.validate();
        Component map = pass.nativeMap() ? skia : java2d;
        if (pass.nativeMap()) {
            skia.setPerformanceObserver(this);
        } else {
            java2d.setPerformanceObserver(this);
        }
        requests = 0;
        if (Boolean.getBoolean("mekhq.skiko.stageTiming")) {
            SkikoStageAgent.armSurfaceProbe();
        }
        coalesced = 0;
        samples.clear();
        lastTick = 0;
        settleUntil = System.nanoTime() + 1_500_000_000L;
        System.out.printf("MAP_PERF_START repeat=%d scene=%s motion=%s renderer=%s viewport=%dx%d%n",
              pass.repeat(), pass.scene(), pass.motion(), pass.renderer(), mapWidth, mapHeight);
    }

    private void setCheck(String key, boolean selected) {
        AbstractButton button = findButton(java2d, MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", key));
        if (button == null) {
            throw new IllegalStateException("Missing benchmark control " + key);
        }
        if (button.isSelected() != selected) {
            button.doClick(0);
        }
    }

    private void tick() {
        try {
            long now = System.nanoTime();
            if (now < settleUntil) {
                return;
            }
            if (pending != 0 && now - pending > 10_000_000_000L) {
                throw new IllegalStateException("Renderer stopped producing callbacks; desktop may be locked or unavailable");
            }
            if (throughput && pending != 0) {
                return;
            }
            if (requests == WARMUP + SAMPLES) {
                if (pending == 0) {
                    completePass();
                    nextPass();
                }
                return;
            }
            if (requests == WARMUP) {
                if (pending != 0) {
                    return;
                }
                Component map = passes.get(passIndex).nativeMap() ? skia : java2d;
                if (mapWidth == 0) {
                    mapWidth = map.getWidth();
                    mapHeight = map.getHeight();
                }
                if (map.getWidth() != mapWidth || map.getHeight() != mapHeight) {
                    throw new IllegalStateException("Unmatched map viewport: expected " + mapWidth + "x" + mapHeight
                          + " actual " + map.getWidth() + "x" + map.getHeight());
                }
                    Pass pass = passes.get(passIndex);
                    String info = pass.nativeMap() ? Objects.requireNonNull(find(skia, org.jetbrains.skiko.SkiaLayer.class)).getRenderInfo()
                        : Boolean.getBoolean("mekhq.skiko.stageTiming")
                            ? "Java2D: see java2d-surfaces.txt" : "Java2D: pipeline not identified";
                    Files.writeString(output.resolve("render-info.txt"), "repeat=" + pass.repeat() + " scene=" + pass.scene()
                        + " motion=" + pass.motion() + " renderer=" + pass.renderer() + "\n" + info + "\n",
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    System.out.println("MAP_PERF_DEVICE " + info.replace('\n', ' '));
                if (!pass.nativeMap() && Boolean.getBoolean("mekhq.skiko.stageTiming")) {
                    java.awt.Graphics screen = java2d.getGraphics();
                    try {
                        SkikoStageAgent.reportSurface("screen", screen);
                    } finally {
                        if (screen != null) {
                            screen.dispose();
                        }
                    }
                    Files.writeString(output.resolve("java2d-surfaces.txt"), SkikoStageAgent.surfaceInfo());
                }
                passWindow = new PassWindow();
                passWindow.repeat = pass.repeat();
                passWindow.scene = pass.scene();
                passWindow.motion = pass.motion();
                passWindow.renderer = pass.renderer();
                passWindow.begin();
                long clockBefore = System.nanoTime();
                passClockUtc = Instant.now();
                long clockAfter = System.nanoTime();
                passClockNano = clockBefore + (clockAfter - clockBefore) / 2;
                passClockBracket = clockAfter - clockBefore;
                now = System.nanoTime();
                    measuring = true;
                passStart = now;
                processCpuStart = operatingSystem.getProcessCpuTime();
                gcStart = gcMillis();
                samples.clear();
                coalesced = 0;
                lastTick = 0;
                if (Boolean.getBoolean("mekhq.skiko.visibleFrames")) {
                    visibleFrames = new VisibleFrames(map);
                }
            }
            tickGap = lastTick == 0 ? 0 : now - lastTick;
            lastTick = now;
            if (pending != 0) {
                coalesced++;
            } else {
                pending = now;
            }
            int index = requests < WARMUP ? requests : requests - WARMUP;
            double phase = index * Math.PI * 2 / SAMPLES;
            Pass pass = passes.get(passIndex);
            double scale = pass.motion().equals("overview-pan") ? 0.8
                : pass.motion().equals("detail-pan") ? 6 : 0.8 * Math.pow(1.175, Math.round(7 * (1 - Math.cos(phase))));
            double horizontal = campaign.getCurrentSystem().getX() + Math.rint(Math.sin(phase) * 180) / scale;
            double vertical = campaign.getCurrentSystem().getY() + Math.rint(Math.sin(phase * 2) * 90) / scale;
            requests++;
            if (pass.nativeMap()) {
                skia.setViewState(new ExperimentalMapView.ViewState(horizontal, vertical, scale, campaign.getCurrentSystem()));
            } else {
                if (pass.motion().equals("zoom")) {
                    int rotation = (int) Math.round(Math.log(java2d.getMapScale() / scale) / Math.log(1.175));
                    if (rotation != 0) {
                        java2d.dispatchEvent(new MouseWheelEvent(java2d, MouseWheelEvent.MOUSE_WHEEL,
                              System.currentTimeMillis(), 0, java2d.getWidth() / 2, java2d.getHeight() / 2,
                              0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation));
                    }
                    if (Math.abs(java2d.getMapScale() - scale) > 0.000001) {
                        throw new IllegalStateException("Wheel camera scale does not match native scale");
                    }
                }
                java2d.restoreMapCenter(new InterstellarMapPanel.MapCenter(horizontal, vertical));
                java2d.repaint();
            }
        } catch (Exception exception) {
            timer.stop();
            exception.printStackTrace();
            try {
                Files.write(output.resolve("summary.csv"), summaries);
                Files.writeString(output.resolve("FAILED.txt"), exception.toString());
            } catch (Exception writeFailure) {
                writeFailure.printStackTrace();
            }
            frame.dispose();
            System.exit(1);
        }
    }

    @Override
    public void frameStarted() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Benchmark assumes EDT rendering");
        }
        allocated = threads.getThreadAllocatedBytes(Thread.currentThread().threadId());
        cpu = threads.getCurrentThreadCpuTime();
        started = System.nanoTime();
    }

    @Override
    public void frameCompleted() {
        long completed = System.nanoTime();
        long cpuUsed = threads.getCurrentThreadCpuTime() - cpu;
        long bytes = threads.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated;
        if (measuring && pending != 0) {
            samples.add(new Sample(completed - started, cpuUsed, bytes, completed - pending, tickGap));
            lastCompleted = completed;
            if (requests == WARMUP + SAMPLES) {
                passWindow.end();
            }
        }
        boolean acknowledged = pending != 0;
        pending = 0;
        if (throughput && acknowledged) {
            javax.swing.SwingUtilities.invokeLater(this::tick);
        }
    }

    private void completePass() throws Exception {
        measuring = false;
        if (samples.isEmpty()) {
            throw new IllegalStateException("Insufficient rendered samples: " + samples.size());
        }
        Pass pass = passes.get(passIndex);
        if (visibleFrames != null) {
            String visible = visibleFrames.stop(output, pass, passStart, lastCompleted);
            visibleFrames = null;
            Files.writeString(output.resolve("visible-frames.csv"), visible + "\n",
                  java.nio.file.StandardOpenOption.APPEND);
            System.out.println("MAP_VISIBLE_FRAMES " + visible);
        }
        long[] paints = new long[samples.size()];
        passWindow.commit();
        long[] cpus = new long[samples.size()];
        long[] allocations = new long[samples.size()];
        long[] latencies = new long[samples.size()];
        long[] ticks = new long[samples.size()];
        long totalAllocated = 0;
        int slow = 0;
        for (int index = 0; index < samples.size(); index++) {
            Sample sample = samples.get(index);
            paints[index] = sample.paint();
            cpus[index] = sample.cpu();
            allocations[index] = sample.allocated();
            latencies[index] = sample.latency();
            ticks[index] = sample.tickGap();
            totalAllocated += sample.allocated();
            slow += sample.paint() > 16_666_667L ? 1 : 0;
            raw.add(String.format(Locale.ROOT, "%d,%s,%s,%s,%d,%d,%d,%d,%d,%d", pass.repeat(), pass.scene(),
                  pass.motion(), pass.renderer(), index, sample.paint(), sample.cpu(), sample.allocated(), sample.latency(), sample.tickGap()));
        }
        String row = String.format(Locale.ROOT,
              "%d,%s,%s,%s,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.3f,%d",
              pass.repeat(), pass.scene(), pass.motion(), pass.renderer(), mapWidth, mapHeight, SAMPLES,
              samples.size(), coalesced, (lastCompleted - passStart) / 1e9,
              percentile(paints, .5) / 1e6, percentile(paints, .95) / 1e6, percentile(paints, .99) / 1e6,
              percentile(paints, 1) / 1e6, percentile(cpus, .5) / 1e6, percentile(allocations, .5) / 1024.0,
              totalAllocated / 1048576.0, percentile(latencies, .95) / 1e6, percentile(ticks, .95) / 1e6,
              slow * 100.0 / samples.size(), (operatingSystem.getProcessCpuTime() - processCpuStart) / 1e9, gcMillis() - gcStart);
        summaries.add(row);
          if (pass.nativeMap()) {
            String cacheInfo = "repeat=" + pass.repeat() + " scene=" + pass.scene() + " motion=" + pass.motion()
                + " " + skia.getRetainedTerritoryInfo();
            Files.writeString(output.resolve("retained-territories.txt"), cacheInfo + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            System.out.println("MAP_PERF_CACHE " + cacheInfo);
          }
          Files.writeString(output.resolve("pass-windows.csv"), String.format(Locale.ROOT,
              "%d,%d,%s,%s,%s,%s,%s,%d%n", ProcessHandle.current().pid(), pass.repeat(), pass.scene(),
              pass.motion(), pass.renderer(), passClockUtc.plusNanos(passStart - passClockNano),
              passClockUtc.plusNanos(lastCompleted - passClockNano), passClockBracket),
              java.nio.file.StandardOpenOption.APPEND);
        Files.write(output.resolve("summary.csv"), summaries);
        Files.write(output.resolve("frames.csv"), raw);
        System.out.println("MAP_PERF_RESULT " + row);
    }

    private static long percentile(long[] values, double quantile) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[Math.max(0, (int) Math.ceil(sorted.length * quantile) - 1)];
    }

    private static long gcMillis() {
        long total = 0;
        for (var collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, collector.getCollectionTime());
        }
        return total;
    }

    private void finish() throws Exception {
        timer.stop();
        recording.stop();
        recording.close();
        frame.dispose();
        System.out.println("MAP_PERF_COMPLETE passes=" + passes.size() + " output=" + output.toAbsolutePath());
        System.exit(0);
    }

    private static AbstractButton findButton(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JCheckBox button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                AbstractButton found = findButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = find(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
