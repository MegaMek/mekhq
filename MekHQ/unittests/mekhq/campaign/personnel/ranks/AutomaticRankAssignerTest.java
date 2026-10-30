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

package mekhq.campaign.personnel.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers issue 10756: the Command Generator ranked the crews of its support vehicles on whatever rank table they
 * were created with, the campaign's default, while every other person it generated had been put on the faction's
 * table. In a Draconis Combine company the recovery vehicle crews came out as Sergeants beside the Gunsho everyone
 * else was.
 */
class AutomaticRankAssignerTest {
    private static final String COMBINE_RANK_SYSTEM = "DCMS";
    private static final String COMBINE_SERGEANT = "Gunsho";
    private static final int CORPORAL_INDEX = 8;
    private static final int SERGEANT_INDEX = 12;

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    void anUnrankedCrewIsRankedOnTheFactionsRankSystem() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit vehicle = crewedVehicle(campaign);
        assertNotEquals(COMBINE_RANK_SYSTEM, campaign.getPlayerForce().getRankSystem().getCode(),
              "this test is only meaningful while the campaign is not on the Combine's rank table");

        AutomaticRankAssigner.assignRanks(campaign, vehicle, combine());

        for (Person person : vehicle.getCrew()) {
            assertEquals(COMBINE_RANK_SYSTEM, person.getRankSystem().getCode(),
                  person.getFullName() + " is ranked on the Combine's table like the rest of the command");
            assertFalse(hasNoRank(person), person.getFullName() + " holds a rank");
        }
        Person commander = vehicle.getCommander();
        assertNotNull(commander);
        assertEquals(COMBINE_SERGEANT, commander.getRankName(),
              "the commander's rank index reads from the Combine's table, not as Sergeant");
    }

    @Test
    void aCrewMemberWhoAlreadyHoldsARankKeepsItAndTheirRankSystem() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit vehicle = crewedVehicle(campaign);
        // The commander is whoever outranks the rest, and is always given the commander's rank. So the commander is
        // ranked above the veteran first, or the veteran would become the commander and be re-ranked for it.
        Person commander = vehicle.getCommander();
        assertNotNull(commander);
        commander.setRank(SERGEANT_INDEX);
        Person veteran = crewMemberWhoIsNotTheCommander(vehicle);
        veteran.setRank(CORPORAL_INDEX);
        String rankBefore = veteran.getRankName();
        RankSystem rankSystemBefore = veteran.getRankSystem();

        AutomaticRankAssigner.assignRanks(campaign, vehicle, combine());

        assertEquals(rankSystemBefore, veteran.getRankSystem(), "an existing rank is read from the table it was given on");
        assertEquals(rankBefore, veteran.getRankName(), "and the rank itself is preserved");
        assertEquals(COMBINE_RANK_SYSTEM, commander.getRankSystem().getCode(),
              "the commander, who is always given the commander's rank, reads it from the faction's table");
        assertEquals(COMBINE_SERGEANT, commander.getRankName());
    }

    @Test
    void aFactionWithoutAUsableRankSystemLeavesTheCrewOnTheirOwnTableButStillRanksThem() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit vehicle = crewedVehicle(campaign);
        RankSystem campaignRankSystem = campaign.getPlayerForce().getRankSystem();
        Faction factionWithoutRanks = mock(Faction.class);
        when(factionWithoutRanks.getRankSystem()).thenReturn(null);
        when(factionWithoutRanks.getShortName()).thenReturn("XX");
        when(factionWithoutRanks.getRankSystemCode()).thenReturn("MISSING");

        AutomaticRankAssigner.assignRanks(campaign, vehicle, factionWithoutRanks);

        for (Person person : vehicle.getCrew()) {
            assertEquals(campaignRankSystem, person.getRankSystem(),
                  person.getFullName() + " stays on the campaign's table when the faction offers none");
            assertFalse(hasNoRank(person), person.getFullName() + " is still ranked");
        }
    }

    private static Faction combine() {
        Faction combine = mock(Faction.class);
        when(combine.getRankSystem()).thenReturn(Ranks.getRankSystemFromCode(COMBINE_RANK_SYSTEM));
        when(combine.getRankSystemCode()).thenReturn(COMBINE_RANK_SYSTEM);
        when(combine.getShortName()).thenReturn("DC");
        return combine;
    }

    /** A vehicle with a driver and a gunner, both freshly hired and unranked, as the Command Generator crews one. */
    private static Unit crewedVehicle(Campaign campaign) {
        Unit vehicle = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        Person driver = humanResources.newPerson(campaign, PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.NONE);
        humanResources.recruitPerson(campaign, driver, true, true);
        vehicle.addDriver(driver);
        Person gunner = humanResources.newPerson(campaign, PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.NONE);
        humanResources.recruitPerson(campaign, gunner, true, true);
        vehicle.addGunner(gunner);
        return vehicle;
    }

    private static Person crewMemberWhoIsNotTheCommander(Unit vehicle) {
        Person commander = vehicle.getCommander();
        for (Person person : vehicle.getCrew()) {
            if (person != commander) {
                return person;
            }
        }
        throw new AssertionError("the vehicle needs a crew member besides its commander");
    }

    private static boolean hasNoRank(Person person) {
        String rankName = person.getRankName();
        return (rankName == null) || rankName.isBlank() || rankName.equals("None") || rankName.equals("-");
    }
}
