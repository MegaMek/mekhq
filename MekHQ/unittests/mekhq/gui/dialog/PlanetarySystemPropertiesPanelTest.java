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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Satellite;
import org.junit.jupiter.api.Test;

class PlanetarySystemPropertiesPanelTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3050, 1, 1);
    private static final String TEST_SOURCE = "Unit Test Source";
    private static final String TEST_VERSION = "2026.05";

    @Test
    void applyChangesUpdatesSourceablePlanetFields() throws Exception {
        AtomicInteger changeCount = new AtomicInteger();
        Planet planet = new Planet("Test Prime");

        runOnEdt(() -> {
            PlanetarySystemPropertiesPanel panel = createPanel(changeCount);
            panel.setSelection(new PlanetarySystem("Test System"), planet);

            findComponent(panel, "txtPlanetaryPropertyPlanetName", JTextField.class).setText("Edited Prime");
            findComponent(panel, "txtPlanetaryPropertyGravity", JTextField.class).setText("1.25");
            findComponent(panel, "txtPlanetaryPropertySource", JTextField.class).setText(TEST_SOURCE);
            findComponent(panel, "txtPlanetaryPropertyVersion", JTextField.class).setText(TEST_VERSION);
            findComponent(panel, "btnApplyPlanetaryProperties", JButton.class).doClick();

            assertEquals("Edited Prime", planet.getSourcedName(TEST_DATE).getValue());
            assertEquals(TEST_SOURCE, planet.getSourcedName(TEST_DATE).getSource());
            assertEquals(TEST_VERSION, planet.getSourcedName(TEST_DATE).getVersion());
            assertEquals(1.25, planet.getSourcedGravity().getValue());
            assertEquals(TEST_SOURCE, planet.getSourcedGravity().getSource());
            assertEquals(1, changeCount.get());
        });
    }

    @Test
    void landMassAndSatelliteButtonsMutateSelectedPlanet() throws Exception {
        AtomicInteger changeCount = new AtomicInteger();
        Planet planet = new Planet("Test Prime");

        runOnEdt(() -> {
            PlanetarySystemPropertiesPanel panel = createPanel(changeCount);
            panel.setSelection(new PlanetarySystem("Test System"), planet);

            findComponent(panel, "btnAddLandMass", JButton.class).doClick();
            LandMass landMass = planet.getLandMasses().getFirst();
            assertEquals("New Landmass", landMass.getSourcedName().getValue());
            assertEquals(TEST_SOURCE, landMass.getSourcedName().getSource());

            findComponent(panel, "btnAddSatellite", JButton.class).doClick();
            Satellite satellite = planet.getSatellites().getFirst();
            assertEquals("New Moon", satellite.getSourcedName().getValue());
            assertEquals("medium", satellite.getSourcedSize().getValue());

            JTable landMassTable = findComponent(panel, "tblPlanetaryPropertyLandMasses", JTable.class);
            landMassTable.setRowSelectionInterval(0, 0);
            findComponent(panel, "btnRemoveLandMass", JButton.class).doClick();
            assertNull(planet.getLandMasses());

            JTable satelliteTable = findComponent(panel, "tblPlanetaryPropertySatellites", JTable.class);
            satelliteTable.setRowSelectionInterval(0, 0);
            findComponent(panel, "btnRemoveSatellite", JButton.class).doClick();
            assertNull(planet.getSatellites());
            assertEquals(4, changeCount.get());
        });
    }

    private static PlanetarySystemPropertiesPanel createPanel(AtomicInteger changeCount) {
        return new PlanetarySystemPropertiesPanel(ResourceBundle.getBundle("mekhq.resources.GUI"), () -> TEST_DATE,
              () -> TEST_SOURCE, changeCount::incrementAndGet);
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
