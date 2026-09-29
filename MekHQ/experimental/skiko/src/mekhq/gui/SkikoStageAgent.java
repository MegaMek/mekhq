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

import java.awt.Graphics;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public final class SkikoStageAgent {
    private static final java.lang.management.ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final Set<String> SURFACES = ConcurrentHashMap.newKeySet();
    private static volatile boolean probePaint = true;

    @Name("mekhq.SkikoStage")
    @Label("Skiko existing rendering stage")
    @StackTrace(false)
    public static final class Stage extends Event {
        public String stage;
        public long cpuNanos;
        public boolean failed;
        private long cpuStart;
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        if (THREADS.isCurrentThreadCpuTimeSupported()) {
            THREADS.setThreadCpuTimeEnabled(true);
        }
        new AgentBuilder.Default()
              .disableClassFormatChanges()
              .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
              .type(named("org.jetbrains.skiko.SkiaLayer"))
              .transform((builder, type, loader, module, domain) -> builder.visit(
                    Advice.to(Timing.class).on(named("draw$skiko").and(takesArguments(1)))))
              .type(named("org.jetbrains.skiko.context.Direct3DContextHandler"))
              .transform((builder, type, loader, module, domain) -> builder.visit(
                    Advice.to(Timing.class).on(named("flush").and(not(isNative())).and(takesArguments(1)))))
              .type(named("mekhq.gui.InterstellarMapPanel"))
              .transform((builder, type, loader, module, domain) -> builder.visit(
                    Advice.to(PaintSurface.class).on(named("paint").and(takesArguments(Graphics.class)))))
              .installOn(instrumentation);
        System.setProperty("mekhq.skiko.stageTiming", "true");
    }

    public static Stage begin(String name) {
        Stage event = new Stage();
        if (!event.isEnabled()) {
            return null;
        }
        event.stage = name;
        event.cpuStart = THREADS.isThreadCpuTimeEnabled() ? THREADS.getCurrentThreadCpuTime() : -1;
        event.begin();
        return event;
    }

    public static void end(Stage event, Throwable failure) {
        if (event != null) {
            event.end();
            event.cpuNanos = event.cpuStart < 0 ? -1 : THREADS.getCurrentThreadCpuTime() - event.cpuStart;
            event.failed = failure != null;
            event.commit();
        }
    }

    public static void reportSurface(String source, Graphics graphics) {
        if (graphics == null) {
            throw new IllegalStateException("Missing " + source + " graphics");
        }
        try {
            Class<?> graphicsType = Class.forName("sun.java2d.SunGraphics2D");
            Object surface = graphicsType.getField("surfaceData").get(graphics);
            String description = source + " graphics=" + graphics.getClass().getName()
                  + " surface=" + surface.getClass().getName()
                  + " config=" + ((java.awt.Graphics2D) graphics).getDeviceConfiguration().getClass().getName();
            if (SURFACES.add(description)) {
                System.out.println("MAP_PERF_JAVA2D " + description);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Java2D surface probe unavailable", exception);
        }
    }

    public static final class Timing {
        @Advice.OnMethodEnter
        public static Stage enter(@Advice.Origin("#t.#m") String name) {
            return SkikoStageAgent.begin(name);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter Stage event, @Advice.Thrown Throwable failure) {
            SkikoStageAgent.end(event, failure);
        }
    }

    public static final class PaintSurface {
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) Graphics graphics) {
            SkikoStageAgent.reportPaint(graphics);
        }
    }

    public static void armSurfaceProbe() {
        probePaint = true;
    }

    public static String surfaceInfo() {
        return String.join("\n", new java.util.TreeSet<>(SURFACES)) + "\n";
    }

    public static void reportPaint(Graphics graphics) {
        if (probePaint) {
            probePaint = false;
            reportSurface("paint", graphics);
        }
    }
}
