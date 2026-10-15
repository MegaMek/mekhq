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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A save prepared for a bug report carries the designs needed to load it on another machine: those of units kept at a
 * base and the design each refit is turning a unit into (issue #10351).
 */
class BugReportSaveDesignsTest {
    @Test
    void aBugReportSaveCarriesBaseUnitsAndRefitTargets() {
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);

        Unit locustAtBase = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        campaign.getPlayerForce().getHangar().removeUnit(locustAtBase.getId());
        base.getBaseHangar().addUnit(locustAtBase);
        for (Part part : List.copyOf(locustAtBase.getParts())) {
            campaign.getPlayerForce().getWarehouse().removePart(part);
            base.getBaseWarehouse().addPart(part);
        }
        Unit refittingWolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        refittingWolverine.setRefit(new Refit(refittingWolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false,
              false, false));

        StringWriter savedCampaign = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(savedCampaign)) {
            campaign.writeToXML(printWriter, true);
        }

        assertTrue(savedCampaign.toString().contains("<name>Locust LCT-1V</name>"), "The base unit's design");
        assertTrue(savedCampaign.toString().contains("<name>Wolverine WVR-6M</name>"), "The refit's target design");
    }
}
