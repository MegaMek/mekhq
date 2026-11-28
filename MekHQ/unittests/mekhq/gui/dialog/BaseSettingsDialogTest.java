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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Saving a base from the base settings dialog. */
class BaseSettingsDialogTest {
    private Campaign campaign;
    private CampaignLocationManager locationManager;
    private PlanetarySystem system;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        locationManager = new CampaignLocationManager();
        when(campaign.getCampaignLocationManager()).thenReturn(locationManager);
        system = mock(PlanetarySystem.class);
    }

    @Test
    void editingABase_keepsItsRegisteredLocation() {
        PlayerBase base = new PlayerBase(new FixedLocation(system));
        locationManager.addPlayerBase(base);
        ILocation originalParent = base.getParent();

        PlayerBase saved = BaseSettingsDialog.saveBase(campaign, base, mock(PlanetarySystem.class), null,
              "Renamed", "Depot");

        assertSame(base, saved);
        assertSame(originalParent, saved.getParent());
        assertTrue(locationManager.getLocations().contains(saved.getParent()));
        assertEquals("Renamed", saved.getDisplayName());
        assertEquals("Depot", saved.getDisplayType());
    }

    @Test
    void creatingABase_registersItAtTheChosenSystem() {
        Planet planet = mock(Planet.class);
        when(planet.getId()).thenReturn("Galatea");

        PlayerBase saved = BaseSettingsDialog.saveBase(campaign, null, system, planet, "Outpost", null);

        assertTrue(locationManager.getPlayerBases().contains(saved));
        assertTrue(locationManager.getLocations().contains(saved.getParent()));
        assertSame(system, saved.getCurrentSystem());
        assertEquals("Galatea", saved.getPlanetId());
        assertEquals("Outpost", saved.getDisplayName());
        assertNull(saved.getDisplayType());
    }
}
