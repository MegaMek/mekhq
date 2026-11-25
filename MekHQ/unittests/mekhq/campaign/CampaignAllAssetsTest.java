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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collection;

import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Units and spares travelling to a base sit on a travel node that is both registered with the campaign and hung under
 * the base's location, so the campaign-wide lists must still name each of them once.
 */
class CampaignAllAssetsTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private GroundTransitLocation travelNode;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        travelNode = new GroundTransitLocation(mock(PlanetarySystem.class), 2d);
        assertTrue(travelNode.setParent(base), "travel node should attach under the base");
        campaign.getCampaignLocationManager().addLocation(travelNode);
    }

    @Test
    void unitTravellingToABase_isListedOnce() {
        Unit unit = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        assertTrue(unit.setParent(travelNode), "unit should board the travel node");

        assertEquals(1, countOccurrences(campaign.getAllUnits(), unit));
    }

    @Test
    void spareTravellingToABase_isListedOnce() {
        MekSensor spare = new MekSensor();
        spare.setCampaign(campaign);
        assertTrue(spare.setParent(travelNode), "spare should board the travel node");

        assertEquals(1, countOccurrences(campaign.getAllParts(), spare));
    }

    private static int countOccurrences(Collection<?> items, Object item) {
        int count = 0;
        for (Object candidate : items) {
            if (candidate == item) {
                count++;
            }
        }
        return count;
    }
}
