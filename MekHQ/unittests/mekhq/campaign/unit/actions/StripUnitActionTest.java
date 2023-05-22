/*
 * StripUnitActionTest.java
 *
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
