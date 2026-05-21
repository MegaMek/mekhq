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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.SourceableValue;
import org.junit.jupiter.api.Test;

class PlanetarySystemSystemEventsPanelTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3050, 1, 1);
    private static final String TEST_SOURCE = "Unit Test Source";
    private static final String TEST_VERSION = "2026.05";

    @Test
    void triStateChargeEditorCanClearOneFieldWithoutDeletingTheEvent() throws Exception {
        AtomicInteger changeCount = new AtomicInteger();
        PlanetarySystem system = new PlanetarySystem("Test System");
        PlanetarySystemEvent event = new PlanetarySystemEvent();
        event.date = TEST_DATE;
        event.nadirCharge = SourceableValue.of(TEST_SOURCE, TEST_VERSION, Boolean.TRUE);
        event.zenithCharge = SourceableValue.of(TEST_SOURCE, TEST_VERSION, Boolean.FALSE);
        system.putEvent(event);

        runOnEdt(() -> {
            PlanetarySystemSystemEventsPanel panel = createPanel(system, changeCount);
            panel.refresh(null);
            JTable table = findComponent(panel, "tblPlanetarySystemSystemEvents", JTable.class);

            editChargeState(table, 0, 2, 0);

            assertNotNull(event.nadirCharge);
            assertEquals(Boolean.TRUE, event.nadirCharge.getValue());
            assertNull(event.zenithCharge);
            assertEquals(1, system.getEvents().size());
            assertEquals(1, changeCount.get());
        });
    }

    private static PlanetarySystemSystemEventsPanel createPanel(PlanetarySystem system, AtomicInteger changeCount) {
        return new PlanetarySystemSystemEventsPanel(null, ResourceBundle.getBundle("mekhq.resources.GUI"),
              () -> TEST_DATE, () -> system, () -> true, changeCount::incrementAndGet);
    }

    private static void editChargeState(JTable table, int row, int column, int stateIndex) {
        assertTrue(table.editCellAt(row, column));
        Component editor = table.getEditorComponent();
        assertTrue(editor instanceof JComboBox<?>);
        ((JComboBox<?>) editor).setSelectedIndex(stateIndex);
        TableCellEditor cellEditor = table.getCellEditor();
        if (cellEditor != null) {
            assertTrue(cellEditor.stopCellEditing());
        }
    }

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

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        T component = findComponentOrNull(root, name, type);
        if (component == null) {
            throw new AssertionError("Missing component named " + name);
        }
        return component;
    }

    private static <T extends Component> T findComponentOrNull(Container root, String name, Class<T> type) {
        if (name.equals(root.getName()) && type.isInstance(root)) {
            return type.cast(root);
        }
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container childContainer) {
                T match = findComponentOrNull(childContainer, name, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}