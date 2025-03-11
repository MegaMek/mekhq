/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.unit.actions;

import megamek.common.EquipmentType;
import mekhq.TestUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StripUnitActionTest {
    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @Test
    public void strippedMekHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        action.execute(campaign, unit);
        assertTrue(unit.getSalvageableParts().isEmpty());
    }

    @Test
    public void strippedLAMHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getWaspLAMMk1());
        action.execute(campaign, unit);
        assertTrue(unit.getSalvageableParts().isEmpty());
    }

    @Test
    public void strippedQuadVeeHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getArionStandard());
        action.execute(campaign, unit);
        assertTrue(unit.getSalvageableParts().isEmpty());
    }

    @Test
    public void strippedUnitIsSalvaged() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        action.execute(campaign, unit);
        assertTrue(unit.isSalvage());
    }
}
