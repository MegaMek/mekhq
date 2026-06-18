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
package mekhq.gui.baseComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link MHQCollapsiblePanel}, covering its expand/collapse state machine, the
 * {@value MHQCollapsiblePanel#EXPANDED_PROPERTY} property change contract, the shared toggle {@link Action}, and that
 * supplied content is hosted and measured. Component work runs on the Event Dispatch Thread to mirror real Swing usage.
 */
class MHQCollapsiblePanelTest {

    @Test
    void newPanelIsExpandedByDefault() throws Exception {
        runOnEdt(() -> {
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section");
            assertTrue(panel.isExpanded());
        });
    }

    @Test
    void collapsingHidesContentAndFiresExpandedEvent() throws Exception {
        runOnEdt(() -> {
            JLabel content = new JLabel("Body");
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section", content);

            AtomicInteger eventCount = new AtomicInteger();
            AtomicReference<Object> lastNewValue = new AtomicReference<>();
            panel.addPropertyChangeListener(MHQCollapsiblePanel.EXPANDED_PROPERTY, event -> {
                eventCount.incrementAndGet();
                lastNewValue.set(event.getNewValue());
            });

            panel.setExpanded(false);

            assertFalse(panel.isExpanded());
            // The content lives inside the collapsible content panel (its parent), which is hidden when collapsed.
            assertFalse(content.getParent().isVisible());
            assertEquals(1, eventCount.get());
            assertEquals(Boolean.FALSE, lastNewValue.get());
        });
    }

    @Test
    void expandingAgainShowsContentAndFiresExpandedEvent() throws Exception {
        runOnEdt(() -> {
            JLabel content = new JLabel("Body");
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section", content);
            panel.setExpanded(false);

            AtomicInteger eventCount = new AtomicInteger();
            panel.addPropertyChangeListener(MHQCollapsiblePanel.EXPANDED_PROPERTY, event -> eventCount.incrementAndGet());

            panel.setExpanded(true);

            assertTrue(panel.isExpanded());
            assertTrue(content.getParent().isVisible());
            assertEquals(1, eventCount.get());
        });
    }

    @Test
    void redundantSetExpandedDoesNotFireEvent() throws Exception {
        runOnEdt(() -> {
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section");
            AtomicInteger eventCount = new AtomicInteger();
            panel.addPropertyChangeListener(MHQCollapsiblePanel.EXPANDED_PROPERTY, event -> eventCount.incrementAndGet());

            // The panel starts expanded, so expanding it again must be a no-op that fires no event.
            panel.setExpanded(true);

            assertTrue(panel.isExpanded());
            assertEquals(0, eventCount.get());
        });
    }

    @Test
    void toggleActionFlipsExpandedState() throws Exception {
        runOnEdt(() -> {
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section");
            Action toggleAction = panel.getActionMap().get(MHQCollapsiblePanel.TOGGLE_ACTION);

            toggleAction.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED,
                    MHQCollapsiblePanel.TOGGLE_ACTION));
            assertFalse(panel.isExpanded());

            toggleAction.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED,
                    MHQCollapsiblePanel.TOGGLE_ACTION));
            assertTrue(panel.isExpanded());
        });
    }

    @Test
    void contentPassedToConstructorIsMeasured() throws Exception {
        runOnEdt(() -> {
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(321, 50));
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section", content);

            // The content's preferred width must be reflected (the panel adds left padding, so allow >=).
            assertTrue(panel.getContentPreferredWidth() >= 321);
        });
    }

    /**
     * Runs the supplied work synchronously on the Event Dispatch Thread, unwrapping any assertion failure or exception
     * so JUnit reports it directly rather than as an {@link InvocationTargetException}.
     */
    private static void runOnEdt(CheckedRunnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException
                      && runtimeException.getCause() instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
