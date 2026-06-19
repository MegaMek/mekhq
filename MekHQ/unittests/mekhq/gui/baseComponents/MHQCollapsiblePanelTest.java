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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
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
        runOnEDT(() -> {
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section");
            assertTrue(panel.isExpanded());
        });
    }

    @Test
    void collapsingHidesContentAndFiresExpandedEvent() throws Exception {
        runOnEDT(() -> {
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
        runOnEDT(() -> {
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
        runOnEDT(() -> {
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
        runOnEDT(() -> {
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
        runOnEDT(() -> {
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(321, 50));
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section", content);

            // The content's preferred width must be reflected (the panel adds left padding, so allow >=).
            assertTrue(panel.getContentPreferredWidth() >= 321);
        });
    }
    @Test
    void titleStaysLeftAlignedWhenNoSummaryIsSet() throws Exception {
        runOnEDT(() -> {
            MHQCollapsiblePanel panel = new MHQCollapsiblePanel("Section");
            // Lay the panel out far wider than its content so a centering regression is obvious: the only header cell
            // carrying horizontal weight is the (possibly empty) summary, and if it stops holding that weight the
            // GridBagLayout centers the icon+title instead of left-aligning them.
            panel.setSize(600, Math.max(40, panel.getPreferredSize().height));
            layoutTree(panel);

            JLabel title = findLabelByText(panel, "Section");
            assertNotNull(title, "the title label should exist");
            Point titleInPanel = SwingUtilities.convertPoint(title.getParent(), title.getLocation(), panel);
            assertTrue(titleInPanel.x < 150,
                    "the title should be left-aligned, but was rendered at x=" + titleInPanel.x);
        });
    }

    /**
     * Lays out a component tree top-down without needing a displayable peer: each container positions its children,
     * then we recurse so those children lay out their own descendants at the size they were just given.
     */
    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static JLabel findLabelByText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (child instanceof Container container) {
                JLabel match = findLabelByText(container, text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
    /**
     * Runs the supplied work synchronously on the Event Dispatch Thread, unwrapping any assertion failure or exception
     * so JUnit reports it directly rather than as an {@link InvocationTargetException}.
     */
    private static void runOnEDT(CheckedRunnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    // Catch Throwable, not just Exception, so an AssertionError from a failed assertion is carried
                    // back across the EDT boundary too. It is unwrapped below so JUnit sees the original failure.
                    throw new RuntimeException(throwable);
                }
            });
        } catch (InvocationTargetException ex) {
            // invokeAndWait reports our wrapper RuntimeException as the cause; the real failure is one level deeper.
            Throwable failure = ex.getCause();
            if (failure instanceof RuntimeException && failure.getCause() != null) {
                failure = failure.getCause();
            }
            if (failure instanceof Error error) {
                // e.g. a JUnit AssertionError - rethrow so it surfaces as the original assertion failure.
                throw error;
            }
            if (failure instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
