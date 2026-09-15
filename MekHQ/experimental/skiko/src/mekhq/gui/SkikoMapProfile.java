package mekhq.gui;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingFile;

public final class SkikoMapProfile {
    public static void main(String[] arguments) throws Exception {
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
}
