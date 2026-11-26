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
package mekhq.gui.utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * Runs work on the Event Dispatch Thread from a background thread, such as the campaign loader, and waits for it.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class EventDispatch {
    private EventDispatch() {}

    /**
     * Runs a task on the Event Dispatch Thread and waits for its result. Runs it directly if already on that thread,
     * so it is safe to call from anywhere. Use it to build and show windows, including modal ones, from a worker.
     *
     * @param task the task
     * @param <T>  the result type
     *
     * @return the task's result
     *
     * @throws InterruptedException if the waiting thread is interrupted; its interrupt flag is set again
     * @throws RuntimeException     whatever unchecked exception the task threw, unwrapped
     * @throws Error                whatever error the task threw, unwrapped
     */
    public static <T> T callAndWait(Supplier<T> task) throws InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(task.get()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            // The task is a Supplier, so it can only fail with an unchecked throwable; pass it on as it was thrown
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
        return result.get();
    }

    /**
     * Runs a task on the Event Dispatch Thread and waits for it to finish. Runs it directly if already on that thread,
     * so it is safe to call from anywhere. Use it to fire campaign events from a worker: their GUI subscribers handle
     * them synchronously and touch Swing components, which must only happen on the Event Dispatch Thread.
     *
     * @param task the task
     *
     * @throws InterruptedException if the waiting thread is interrupted; its interrupt flag is set again
     * @throws RuntimeException     whatever unchecked exception the task threw, unwrapped
     * @throws Error                whatever error the task threw, unwrapped
     */
    public static void runAndWait(Runnable task) throws InterruptedException {
        callAndWait(() -> {
            task.run();
            return null;
        });
    }
}
