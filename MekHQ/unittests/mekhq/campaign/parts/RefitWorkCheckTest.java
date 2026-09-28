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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit only goes ahead with a tech who can do the work. When the target number is Impossible, the day's work is
 * skipped with a reason in the daily report instead of being done and failing its check at the end (issue #10197).
 */
class RefitWorkCheckTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private Refit refit;
    private Person tech;

    @BeforeEach
    void setUp() throws Exception {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(SkillType.EXP_REGULAR);
        refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        refit.setTech(tech);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts(), "The kit has arrived, so only the tech decides whether work happens");
    }

    @Test
    void aTechWhoCanDoTheWorkIsAllowedAndWorks() {
        assertNull(RefitWorkCheck.reasonTechCannotWork(campaign, refit, tech));

        campaign.refit(refit);

        assertTrue(refit.getTimeSpent() > 0, "A day of work was done");
    }

    @Test
    void aTechWithoutARequiredToolKitCannotWorkAndTheRefitPauses() {
        campaign.getCampaignOptions().set(CampaignOption.TECHS_NEED_TOOL_KIT, true);

        String reason = RefitWorkCheck.reasonTechCannotWork(campaign, refit, tech);
        campaign.refit(refit);

        assertNotNull(reason);
        assertTrue(reason.contains("tool kit"), reason);
        assertEquals(0, refit.getTimeSpent(), "No work is done and nothing is rolled");
        assertSame(refit, refit.getUnit().getRefit(), "The refit is paused, not completed or cancelled");
    }

    @Test
    void aTechAwayFromTheUnitCannotWorkAndTheRefitPauses() {
        tech.setParent(new PlayerBase(new FixedLocation(mock(PlanetarySystem.class))));

        String reason = RefitWorkCheck.reasonTechCannotWork(campaign, refit, tech);
        campaign.refit(refit);

        assertNotNull(reason);
        assertEquals(0, refit.getTimeSpent(), "No work is done while the tech is elsewhere");
        assertSame(refit, refit.getUnit().getRefit());
    }

    @Test
    void aShipRefitFollowsANewEngineer() throws Exception {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        Person firstEngineer = scenario.withTech(SkillType.EXP_REGULAR);
        Person secondEngineer = scenario.withTech(SkillType.EXP_REGULAR);
        leopard.addVesselCrew(firstEngineer);
        leopard.addVesselCrew(secondEngineer);
        leopard.resetEngineer();
        Person engineerBefore = leopard.getEngineer();
        assertNotNull(engineerBefore, "The ship has an engineer");
        Refit shipRefit = new Refit(leopard, UnitFixture.LEOPARD_DROPSHIP.loadEntity(), false, false, false);
        shipRefit.setTech(engineerBefore);
        leopard.setRefit(shipRefit);

        leopard.remove(engineerBefore, false);
        // choosing the new engineer is what moves the ship's work over to them
        leopard.resetEngineer();

        Person engineerAfter = leopard.getEngineer();
        assertNotNull(engineerAfter, "The other crew member becomes the engineer");
        assertSame(engineerAfter, shipRefit.getTech(), "The refit is now worked by the new engineer");
    }
}
