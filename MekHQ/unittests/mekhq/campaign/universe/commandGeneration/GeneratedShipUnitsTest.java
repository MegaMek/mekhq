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

package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers issue 10756 for the ships the Command Generator adds already crewed: their crews were left on the campaign's
 * rank system while everyone else the generator made had been put on the faction's.
 */
class GeneratedShipUnitsTest {
    private static final String COMBINE_RANK_SYSTEM = "DCMS";

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    void aShipsCrewIsPutOnTheFactionsRankSystemWithoutBeingRanked() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit ship = crewedUnit(campaign);
        Faction combine = mock(Faction.class);
        when(combine.getRankSystem()).thenReturn(Ranks.getRankSystemFromCode(COMBINE_RANK_SYSTEM));

        GeneratedShipUnits.putCrewOnFactionRankSystem(ship, combine, "[Test]");

        assertFalse(ship.getCrew().isEmpty(), "this test is only meaningful with a crew aboard");
        for (Person person : ship.getCrew()) {
            assertEquals(COMBINE_RANK_SYSTEM, person.getRankSystem().getCode(),
                  person.getFullName() + " is on the Combine's rank table like the rest of the command");
            assertEquals(0, person.getRankNumeric(), person.getFullName() + " is given no rank by the move");
        }
    }

    @Test
    void withoutAFactionTheCrewKeepTheCampaignsRankSystem() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit ship = crewedUnit(campaign);
        RankSystem campaignRankSystem = campaign.getPlayerForce().getRankSystem();

        GeneratedShipUnits.putCrewOnFactionRankSystem(ship, null, "[Test]");

        for (Person person : ship.getCrew()) {
            assertEquals(campaignRankSystem, person.getRankSystem(),
                  person.getFullName() + " stays on the campaign's table when no faction is known");
        }
    }

    /** A crewed vehicle stands in for a ship: the move works on a unit's crew, whatever the unit is. */
    private static Unit crewedUnit(Campaign campaign) {
        Unit unit = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        Person driver = humanResources.newPerson(campaign, PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.NONE);
        humanResources.recruitPerson(campaign, driver, true, true);
        unit.addDriver(driver);
        Person gunner = humanResources.newPerson(campaign, PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.NONE);
        humanResources.recruitPerson(campaign, gunner, true, true);
        unit.addGunner(gunner);
        return unit;
    }
}
