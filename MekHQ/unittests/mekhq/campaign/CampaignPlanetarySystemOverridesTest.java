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
package mekhq.campaign;

import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import megamek.common.event.Subscribe;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.PlanetarySystemsChangedEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import mekhq.gui.view.CurrentLocationPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

class CampaignPlanetarySystemOverridesTest {
    private Campaign campaign;
    private PlanetarySystem originalSystem;
    private List<PlanetarySystem> canonicalSystems;
    private Systems activeSystems;
    private MockedStatic<Systems> systemsMock;

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
        originalSystem = campaign.getCurrentSystem();
        canonicalSystems = campaign.getSystems();
        activeSystems = new TestSystems();
        for (PlanetarySystem system : canonicalSystems) {
            activeSystems.getSystems().put(system.getId(), system);
        }
        systemsMock = mockSystemsRegistry();
    }

    private MockedStatic<Systems> mockSystemsRegistry() {
        MockedStatic<Systems> registry = mockStatic(Systems.class);
        registry.when(Systems::getInstance).thenAnswer(invocation -> activeSystems);
        registry.when(() -> Systems.activateCampaignSystems(anyCollection())).thenAnswer(invocation -> {
            TestSystems overlay = new TestSystems();
            for (PlanetarySystem system : canonicalSystems) {
                overlay.getSystems().put(system.getId(), system);
            }
            Collection<PlanetarySystem> overrides = invocation.getArgument(0);
            for (PlanetarySystem system : overrides) {
                overlay.getSystems().put(system.getId(), system);
            }
            activeSystems = overlay;
            return overlay;
        });
        return registry;
    }

    @AfterEach
    void tearDown() {
        if (systemsMock != null) {
            systemsMock.close();
        }
    }

    @Test
    void savingAndDeletingAnOverrideImmediatelySynchronizeAllLocations() throws IOException {
        AbstractLocation currentLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        currentLocation.setCurrentPlanet(originalSystem.getPrimaryPlanet());
        FixedLocation fixedLocation = new FixedLocation(originalSystem);
        campaign.getCampaignLocationManager().addLocation(fixedLocation);
        var originalDate = campaign.getLocalDate();
        ChangeRecorder recorder = new ChangeRecorder(campaign);
        MekHQ.registerHandler(recorder);
        try {
            PlanetarySystem savedSystem = campaign.putPlanetarySystemOverride(editedSystem());

            assertSame(savedSystem, campaign.getCurrentSystem());
            assertSame(savedSystem, fixedLocation.getCurrentSystem());
            assertSame(savedSystem.getPrimaryPlanet(), currentLocation.getCurrentPlanetDirect());
            assertEquals(-87, currentLocation.getPlanet().getTemperature(originalDate));
            assertSame(savedSystem, recorder.notifiedSystem);
            assertSame(currentLocation, campaign.getPlayerForce().getForceDetachment().getCurrentLocation());
            assertEquals(originalDate, campaign.getLocalDate());

            assertTrue(campaign.removePlanetarySystemOverride(originalSystem.getId()));

            assertSame(originalSystem, currentLocation.getCurrentSystem());
            assertSame(originalSystem, fixedLocation.getCurrentSystem());
            assertSame(originalSystem.getPrimaryPlanet(), currentLocation.getCurrentPlanetDirect());
            assertSame(originalSystem, recorder.notifiedSystem);
            assertEquals(2, recorder.eventCount);
        } finally {
            MekHQ.unregisterHandler(recorder);
        }
    }

    @Test
    void replacingAndClearingOverridesPreserveTravelProgressAndUpdateTheRoute() throws IOException {
        CurrentLocation location = new CurrentLocation(originalSystem, 2.5);
        location.setRechargeTime(12.0);
        JumpPath route = new JumpPath();
        route.addSystem(originalSystem);
        route.setTargetPlanet(originalSystem.getPrimaryPlanet());
        location.setJumpPath(route);
        campaign.setLocation(location);
        PlanetarySystem edited = editedSystem();

        campaign.setPlanetarySystemOverrides(List.of(edited));

        assertSame(edited, location.getCurrentSystem());
        assertSame(edited, route.getFirstSystem());
        assertSame(edited.getPrimaryPlanet(), route.getTargetPlanet());
        assertSame(route, location.getJumpPath());
        assertEquals(2.5, location.getTransitTime());
        assertEquals(12.0, location.getRechargeTime());
        assertNull(location.getCurrentPlanetDirect());

        campaign.setPlanetarySystemOverrides(List.of());

        assertSame(originalSystem, location.getCurrentSystem());
        assertSame(originalSystem, route.getFirstSystem());
        assertSame(originalSystem.getPrimaryPlanet(), route.getTargetPlanet());
        assertEquals(2.5, location.getTransitTime());
        assertEquals(12.0, location.getRechargeTime());
    }

    @Test
    void savingABatchInstallsAllCopiesBeforeEmittingOneEventAndPreservesUnrelatedOverrides() throws IOException {
        PlanetarySystem unrelatedSystem = PlanetarySystemYamlIO.read("""
              id: Unrelated
              planet:
                - name: unrelated-world
                  sysPos: 1
              """);
        PlanetarySystem unrelatedOverride = campaign.putPlanetarySystemOverride(unrelatedSystem);
        PlanetarySystem secondSystem = PlanetarySystemYamlIO.read("""
              id: Second
              planet:
                - name: second-world
                  sysPos: 1
                  temperature: 30
              """);
        FixedLocation secondLocation = new FixedLocation(secondSystem);
        campaign.getCampaignLocationManager().addLocation(secondLocation);
        PlanetarySystem edited = editedSystem();
        ChangeRecorder recorder = new ChangeRecorder(campaign);
        MekHQ.registerHandler(recorder);
        try {
            List<PlanetarySystem> saved = campaign.putPlanetarySystemOverrides(List.of(edited, secondSystem));

            assertEquals(1, recorder.eventCount);
            assertEquals(3, recorder.notifiedSystems.size());
            assertTrue(recorder.notifiedSystems.containsAll(saved));
            assertSame(saved.getFirst(), campaign.getCurrentSystem());
            assertSame(saved.getLast(), secondLocation.getCurrentSystem());
            assertSame(unrelatedOverride, campaign.getSystemById("Unrelated"));
            assertNotSame(edited, saved.getFirst());
            assertNotSame(secondSystem, saved.getLast());
        } finally {
            MekHQ.unregisterHandler(recorder);
        }
    }

    @Test
    void invalidBatchDoesNotInstallEarlierCopiesOrEmitAnEvent() throws IOException {
        PlanetarySystem existingOverride = campaign.putPlanetarySystemOverride(editedSystem());
        PlanetarySystem edited = editedSystem();
        edited.getPrimaryPlanet().setSourcedTemperature(SourceableValue.of(15));
        ChangeRecorder recorder = new ChangeRecorder(campaign);
        MekHQ.registerHandler(recorder);
        try {
            assertThrows(IOException.class, () -> campaign.putPlanetarySystemOverrides(
                  List.of(edited, new PlanetarySystem(" "))));

            assertSame(existingOverride, campaign.getCurrentSystem());
            assertEquals(-87, campaign.getCurrentSystem().getPrimaryPlanet().getTemperature(campaign.getLocalDate()));
            assertEquals(0, recorder.eventCount);
            assertEquals(List.of(existingOverride), List.copyOf(campaign.getPlanetarySystemOverrides()));
        } finally {
            MekHQ.unregisterHandler(recorder);
        }
    }

    @Test
    void copyFailureDoesNotInstallEarlierCopiesOrEmitAnEvent() throws IOException {
        PlanetarySystem edited = editedSystem();
        PlanetarySystem failingSystem = mock(PlanetarySystem.class);
        when(failingSystem.getId()).thenReturn("Failing");
        ChangeRecorder recorder = new ChangeRecorder(campaign);
        MekHQ.registerHandler(recorder);
        try (MockedStatic<PlanetarySystemYamlIO> yaml = mockStatic(PlanetarySystemYamlIO.class)) {
            yaml.when(() -> PlanetarySystemYamlIO.copy(edited)).thenReturn(edited);
            yaml.when(() -> PlanetarySystemYamlIO.copy(failingSystem)).thenThrow(new IOException("Copy failed"));

            assertThrows(IOException.class, () -> campaign.putPlanetarySystemOverrides(List.of(edited, failingSystem)));

            assertSame(originalSystem, campaign.getCurrentSystem());
            assertTrue(campaign.getPlanetarySystemOverrides().isEmpty());
            assertEquals(0, recorder.eventCount);
        } finally {
            MekHQ.unregisterHandler(recorder);
        }
    }

    @Test
    void emptyBatchDoesNotRefreshTheOverlayOrEmitAnEvent() throws IOException {
        ChangeRecorder recorder = new ChangeRecorder(campaign);
        MekHQ.registerHandler(recorder);
        try {
            assertTrue(campaign.putPlanetarySystemOverrides(List.of()).isEmpty());
            assertSame(originalSystem, campaign.getCurrentSystem());
            assertEquals(0, recorder.eventCount);
        } finally {
            MekHQ.unregisterHandler(recorder);
        }
    }

    @Test
    void savingAndDeletingAnOverrideRefreshTheDisplayedTopBarWithoutAdvancingADay() throws Exception {
        PlanetarySystem edited = editedSystem();
        SwingUtilities.invokeAndWait(() -> {
            campaign.getCampaignOptions().set(CampaignOption.PERSONNEL_MARKET_STYLE, PERSONNEL_MARKET_DISABLED);
            CurrentLocationPanel panel = new CurrentLocationPanel(365, 420, 100, campaign, () -> {});
            String originalConditions = panel.getPlanetaryConditionsInfo();
            JLabel conditionsLabel = conditionsLabel(panel, originalConditions);
            panel.addNotify();
            try (MockedStatic<Systems> registry = mockSystemsRegistry()) {
                campaign.putPlanetarySystemOverride(edited);

                assertNotEquals(originalConditions, conditionsLabel.getText());
                assertEquals(panel.getPlanetaryConditionsInfo(), conditionsLabel.getText());
                assertEquals(-87, campaign.getCurrentSystem().getPrimaryPlanet()
                                       .getTemperature(campaign.getLocalDate()));

                campaign.removePlanetarySystemOverride(originalSystem.getId());

                assertEquals(originalConditions, conditionsLabel.getText());

                conditionsLabel.setText("unchanged");
                panel.handle(new PlanetarySystemsChangedEvent(mock(Campaign.class)));
                assertEquals("unchanged", conditionsLabel.getText());
            } catch (IOException ex) {
                throw new AssertionError(ex);
            } finally {
                panel.removeNotify();
            }
        });
    }

    private PlanetarySystem editedSystem() throws IOException {
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(originalSystem);
        edited.getPrimaryPlanet().setSourcedTemperature(SourceableValue.of(-87));
        edited.getPrimaryPlanet().setSourcedAtmosphere(SourceableValue.of(AtmosphericTaint.BREATHABLE));
        edited.getPrimaryPlanet().setSourcedPressure(SourceableValue.of(Atmosphere.STANDARD));
        return edited;
    }

    private static JLabel conditionsLabel(CurrentLocationPanel panel, String expectedText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JLabel label && expectedText.equals(label.getText())) {
                return label;
            }
        }
        throw new AssertionError("The current-location conditions label was not found.");
    }

    public static class ChangeRecorder {
        private final Campaign campaign;
        private PlanetarySystem notifiedSystem;
        private List<PlanetarySystem> notifiedSystems;
        private int eventCount;

        ChangeRecorder(Campaign campaign) {
            this.campaign = campaign;
        }

        @Subscribe
        public void handle(PlanetarySystemsChangedEvent event) {
            if (event.getCampaign() == campaign) {
                notifiedSystem = campaign.getCurrentSystem();
                notifiedSystems = campaign.getSystems();
                eventCount++;
            }
        }
    }
}
