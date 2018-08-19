/*
 * StripUnitActionTest.java
 *
 * Copyright (C) 2018 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit.actions;

import mekhq.TestUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.unit.Unit;

import org.junit.Assert;
import org.junit.Test;

public class StripUnitActionTest {
    @Test
    public void strippedMekHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        action.Execute(campaign, unit);

        Assert.assertTrue(0 == unit.getSalvageableParts().size());
    }

    @Test
    public void strippedLAMHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getWaspLAMMk1());
        action.Execute(campaign, unit);

        Assert.assertTrue(0 == unit.getSalvageableParts().size());
    }

    @Test
    public void strippedQuadVeeHasNoSalvageableParts() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getArionStandard());
        action.Execute(campaign, unit);

        Assert.assertTrue(0 == unit.getSalvageableParts().size());
    }

    @Test
    public void strippedUnitIsSalvaged() {
        StripUnitAction action = new StripUnitAction();
        Campaign campaign = TestUtilities.getTestCampaign();
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        action.Execute(campaign, unit);

        Assert.assertTrue(unit.isSalvage());
    }
}
