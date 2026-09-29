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

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public final class SkikoMapProfile {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 2 && arguments[1].equals("--stages")) {
            printStages(Path.of(arguments[0]));
            return;
        }
        Map<String, Long> allocationSites = new HashMap<>();
        Map<String, Long> allocationTraces = new HashMap<>();
        Map<String, Long> execution = new HashMap<>();
        Map<String, Long> nativeSamples = new HashMap<>();
        try (RecordingFile recording = new RecordingFile(Path.of(arguments[0]))) {
            while (recording.hasMoreEvents()) {
                var event = recording.readEvent();
                String type = event.getEventType().getName();
                if (!type.equals("jdk.ObjectAllocationSample") && !type.equals("jdk.ExecutionSample")
                      && !type.equals("jdk.NativeMethodSample")) {
                    continue;
                }
                var stack = event.getStackTrace();
                if (stack == null || stack.getFrames().isEmpty()) {
                    continue;
                }
                String owner = "other";
                StringBuilder trace = new StringBuilder();
                int depth = 0;
                for (RecordedFrame frame : stack.getFrames()) {
                    String method = frame.getMethod().getType().getName() + "." + frame.getMethod().getName();
                    if (owner.equals("other") && (method.contains("InterstellarMapPanel") || method.contains("SkiaMap"))) {
                        owner = method;
                    }
                    if (depth++ < 12) {
                        trace.append("\n    ").append(method).append(':').append(frame.getLineNumber());
                    }
                }
                RecordedFrame leaf = stack.getFrames().getFirst();
                String site = leaf.getMethod().getType().getName() + "." + leaf.getMethod().getName();
                if (type.equals("jdk.ObjectAllocationSample")) {
                    long weight = event.getLong("weight");
                    allocationSites.merge(owner + " -> " + site, weight, Long::sum);
                    allocationTraces.merge(owner + trace, weight, Long::sum);
                } else {
                    String thread = event.getThread("sampledThread").getJavaName();
                    (type.equals("jdk.ExecutionSample") ? execution : nativeSamples)
                          .merge(thread + " | " + owner + " -> " + site, 1L, Long::sum);
                }
            }
        }
        print("Allocation sites (estimated MiB, entire recording including setup/warmup)", allocationSites, 1048576.0, 20);
        print("Allocation stacks (estimated MiB)", allocationTraces, 1048576.0, 8);
        print("Java execution samples (counts, not durations)", execution, 1, 25);
        print("Native method samples (counts, may include waits)", nativeSamples, 1, 20);
    }

    private static void print(String heading, Map<String, Long> values, double divisor, int limit) {
        System.out.println(heading);
        values.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(limit)
              .forEach(entry -> System.out.printf(java.util.Locale.ROOT, "%.3f %s%n", entry.getValue() / divisor, entry.getKey()));
    }

    private static void printStages(Path path) throws Exception {
        List<RecordedEvent> stages = new ArrayList<>();
        List<RecordedEvent> passes = new ArrayList<>();
        try (RecordingFile recording = new RecordingFile(path)) {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                switch (event.getEventType().getName()) {
                    case "mekhq.SkikoStage" -> stages.add(event);
                    case "mekhq.MapMeasuredPass" -> passes.add(event);
                    case "jdk.DataLoss" -> throw new IllegalStateException("JFR reported data loss; reject stage timings");
                    default -> { }
                }
            }
        }
        if (stages.isEmpty() || passes.isEmpty()) {
            throw new IllegalStateException("Missing stage or measured-pass events");
        }
        stages.sort(java.util.Comparator.comparing(RecordedEvent::getStartTime));
        passes.sort(java.util.Comparator.comparing(RecordedEvent::getStartTime));
        Path output = path.resolveSibling("stages.csv");
        try (var writer = Files.newBufferedWriter(output)) {
            writer.write("repeat,scene,motion,stage,thread,start_utc,wall_ns,cpu_ns\n");
            for (RecordedEvent pass : passes) {
                if (!pass.getString("renderer").equals("Skia")) {
                    continue;
                }
                var start = pass.getStartTime().plusMillis(100);
                var end = pass.getEndTime().minusMillis(100);
                Map<String, List<RecordedEvent>> grouped = new java.util.TreeMap<>();
                for (RecordedEvent stage : stages) {
                    if (stage.getStartTime().isBefore(start) || stage.getEndTime().isAfter(end)) {
                        continue;
                    }
                    if (stage.getBoolean("failed")) {
                        throw new IllegalStateException("Instrumented stage threw an exception");
                    }
                    String name = stage.getString("stage");
                    grouped.computeIfAbsent(name, ignored -> new ArrayList<>()).add(stage);
                    writer.write(String.format(Locale.ROOT, "%d,%s,%s,%s,%s,%s,%d,%d%n", pass.getInt("repeat"),
                          pass.getString("scene"), pass.getString("motion"), name, stage.getThread().getJavaName(),
                          stage.getStartTime(), stage.getDuration().toNanos(), stage.getLong("cpuNanos")));
                }
                    if (!grouped.keySet().equals(java.util.Set.of("org.jetbrains.skiko.SkiaLayer.draw$skiko",
                        "org.jetbrains.skiko.context.Direct3DContextHandler.flush"))) {
                    throw new IllegalStateException("Expected playback and flush events for each native pass: " + grouped.keySet());
                }
                for (var entry : grouped.entrySet()) {
                    List<Long> walls = new ArrayList<>();
                    long cpuTotal = 0;
                    long wallTotal = 0;
                    for (RecordedEvent stage : entry.getValue()) {
                        long wall = stage.getDuration().toNanos();
                        walls.add(wall);
                        wallTotal += wall;
                        long cpu = stage.getLong("cpuNanos");
                        if (cpu < 0) {
                            throw new IllegalStateException("Thread CPU timing unavailable");
                        }
                        cpuTotal += cpu;
                    }
                    walls.sort(Long::compareTo);
                    int count = walls.size();
                    System.out.printf(Locale.ROOT,
                          "STAGE repeat=%d scene=%s name=%s count=%d wall_p50_ms=%.3f wall_p95_ms=%.3f wall_total_ms=%.3f cpu_total_ms=%.3f%n",
                          pass.getInt("repeat"), pass.getString("scene"), entry.getKey(), count,
                          walls.get((int) Math.ceil(count * .5) - 1) / 1e6,
                          walls.get((int) Math.ceil(count * .95) - 1) / 1e6, wallTotal / 1e6, cpuTotal / 1e6);
                }
            }
        }
        System.out.println("STAGE_COMPLETE events=" + stages.size() + " passes=" + passes.size() + " output=" + output);
    }
}
