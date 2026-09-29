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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;

/**
 * Reconfiguring an OmniMek swaps only its pod-mounted equipment; its fixed parts (engine, gyro, structure, actuators)
 * stay on the unit (issue #10201, reported in #5748 as the engine being wiped by a simple pod change).
 */
class RefitOmniReconfigurationTest {
    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void aReconfiguredOmniMekKeepsItsFixedPartsAndMatchesTheNewConfiguration() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        Unit masakari = addUnit(campaign, UnitTestUtilities.getMasakariWarhawkA());
        Refit refit = new Refit(masakari, UnitTestUtilities.getMasakariWarhawkB(), false, false, false);
        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass(), "A to B is an Omni reconfiguration");
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        Unit freshMasakariB = addUnit(campaign, UnitTestUtilities.getMasakariWarhawkB());
        assertEquals("B", masakari.getEntity().getModel());
        assertEquals(PartsCensus.ofUnit(freshMasakariB), PartsCensus.ofUnit(masakari),
              "The reconfigured unit has the same parts as a factory-fresh Masakari B, fixed parts included");
    }

    private static Unit addUnit(Campaign campaign, Entity entity) {
        Unit unit = campaign.addNewUnit(entity, false, 0, PartQuality.QUALITY_D);
        assertNotNull(unit);
        return unit;
    }
}
