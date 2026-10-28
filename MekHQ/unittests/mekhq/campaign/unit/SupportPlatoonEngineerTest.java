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
package mekhq.campaign.unit;

import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A support platoon is conventional infantry whose troopers are techs, each looking after their own units. When the
 * platoon maintains itself its ranking tech is its engineer, but must not be flagged as one: the flag hands the
 * engineer's minutes to every trooper, so the other techs lost their day whenever the ranking tech worked (#10654).
 */
class SupportPlatoonEngineerTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private Unit platoon;
    private Person rankingTech;
    private Person otherTech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        campaignOptions.set(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY, false);
        campaignOptions.set(CampaignOption.CHECK_MAINTENANCE, true);
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, false);

        platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        rankingTech = scenario.withTech(EXP_ELITE);
        otherTech = scenario.withTech(EXP_REGULAR);
        platoon.addPilotOrSoldier(rankingTech);
        platoon.addPilotOrSoldier(otherTech);
        platoon.resetEngineer();
    }

    @Test
    void thePlatoonIsStillMaintainedByItsRankingTech() {
        assertTrue(platoon.isSelfMaintainedInfantry());
        assertNotNull(platoon.getEngineer(), "The self-maintained platoon has a tech to maintain it");
    }

    @Test
    void thePlatoonsEngineerIsNotFlaggedAsAVesselEngineer() {
        Person engineer = platoon.getEngineer();
        assertNotNull(engineer);

        assertFalse(engineer.isEngineer(), "Only a vessel's engineer is flagged");
    }

    @Test
    void theEngineerUsingMinutesLeavesTheOtherTechsTheirOwn() {
        Person engineer = platoon.getEngineer();
        assertNotNull(engineer);
        Person trooper = (engineer == rankingTech) ? otherTech : rankingTech;
        int trooperMinutes = trooper.getMinutesLeft();

        engineer.setMinutesLeft(144);

        assertEquals(trooperMinutes, trooper.getMinutesLeft(), "The other tech keeps their own day");
    }

    @Test
    void aTrooperStillMaintainsTheirOwnUnitAfterTheEngineerHasWorked() {
        Person engineer = platoon.getEngineer();
        assertNotNull(engineer);
        Person trooper = (engineer == rankingTech) ? otherTech : rankingTech;
        Unit engineersMek = scenario.withUnit(UnitFixture.LONGBOW_LGB_8C);
        Unit troopersMek = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        engineersMek.setTech(engineer);
        troopersMek.setTech(trooper);
        campaign.getCampaignOptions().set(CampaignOption.MAINTENANCE_CYCLE_DAYS, 1);

        // Off contract the cycle advances a quarter of a day per day, so the fourth day brings the check
        for (int day = 0; day < 4; day++) {
            engineer.resetMinutesLeft(false);
            trooper.resetMinutesLeft(false);
            Maintenance.doMaintenance(campaign, engineersMek);
            Maintenance.doMaintenance(campaign, troopersMek);
        }

        assertTrue(troopersMek.getLastMaintenanceReport().contains(trooper.getFullTitle()),
              "The trooper made the check on their own unit");
    }
}
