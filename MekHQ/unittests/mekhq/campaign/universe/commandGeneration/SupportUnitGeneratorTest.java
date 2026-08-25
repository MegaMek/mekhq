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

import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.force.PlayerForce;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator.SecurityTier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies the force-size scaling that drives the standalone capability vehicles, in particular the
 * commissary canteen count produced by {@link SupportUnitGenerator#vehiclesForCoverage(int, int)}.
 * The full {@code commissaryUnitCount} path needs a loaded unit cache and a populated campaign
 * roster, so these tests target the deterministic scaling formula it delegates to.
 */
class SupportUnitGeneratorTest {

    /** Each canteen counts as one field kitchen; at the default capacity that feeds 150 personnel. */
    private static final int CANTEEN_COVERAGE = 150;

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
    }

    @Test
    void countGeneratedUnitsNamedMatchesByEntityName() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());

        assertEquals(2, SupportUnitGenerator.countGeneratedUnitsNamed(campaign, "Locust LCT-1V"),
              "both matching units are counted, so reconciliation subtracts them from the target");
        assertEquals(0, SupportUnitGenerator.countGeneratedUnitsNamed(campaign,
              "Sherpa Armored Truck (Mobile Canteen)"), "a support unit not present counts zero");
    }

    @Test
    void exactMultipleFillsWithoutRoundingUp() {
        assertEquals(1, SupportUnitGenerator.vehiclesForCoverage(CANTEEN_COVERAGE, CANTEEN_COVERAGE));
        assertEquals(2, SupportUnitGenerator.vehiclesForCoverage(2 * CANTEEN_COVERAGE, CANTEEN_COVERAGE));
        assertEquals(3, SupportUnitGenerator.vehiclesForCoverage(3 * CANTEEN_COVERAGE, CANTEEN_COVERAGE));
    }

    @Test
    void partialCoverageRoundsUp() {
        assertEquals(2, SupportUnitGenerator.vehiclesForCoverage(CANTEEN_COVERAGE + 1, CANTEEN_COVERAGE));
        assertEquals(3, SupportUnitGenerator.vehiclesForCoverage((2 * CANTEEN_COVERAGE) + 1, CANTEEN_COVERAGE));
    }

    @Test
    void neverFewerThanOneVehicle() {
        assertEquals(1, SupportUnitGenerator.vehiclesForCoverage(0, CANTEEN_COVERAGE));
        assertEquals(1, SupportUnitGenerator.vehiclesForCoverage(1, CANTEEN_COVERAGE));
    }

    @Test
    void nonPositiveCoveragePerVehicleFallsBackToOne() {
        assertEquals(1, SupportUnitGenerator.vehiclesForCoverage(500, 0));
        assertEquals(1, SupportUnitGenerator.vehiclesForCoverage(500, -10));
    }

    @Test
    void largerForceYieldsMoreCanteens() {
        int smallForce = SupportUnitGenerator.vehiclesForCoverage(CANTEEN_COVERAGE, CANTEEN_COVERAGE);
        int mediumForce = SupportUnitGenerator.vehiclesForCoverage(3 * CANTEEN_COVERAGE, CANTEEN_COVERAGE);
        int largeForce = SupportUnitGenerator.vehiclesForCoverage(10 * CANTEEN_COVERAGE, CANTEEN_COVERAGE);

        assertTrue(mediumForce > smallForce, "a larger force must require at least as many canteens");
        assertTrue(largeForce > mediumForce, "a larger force must require at least as many canteens");
        assertEquals(10, largeForce);
    }

    @Test
    void combatPersonnelCountCountsOnlyCombatants() {
        Campaign campaign = campaignWithPersonnel(3, 2);
        assertEquals(3, SupportUnitGenerator.combatPersonnelCount(campaign));
    }

    @Test
    void securityDetailTracksForceEchelon() {
        // Non-combatants never drive the tier, so only the combatant total matters.
        assertEquals(SecurityTier.SQUAD, SupportUnitGenerator.securityTier(campaignWithPersonnel(12, 50)),
              "a company-sized force gets a squad");
        assertEquals(SecurityTier.PLATOON, SupportUnitGenerator.securityTier(campaignWithPersonnel(36, 0)),
              "a battalion-sized force gets a platoon");
        assertEquals(SecurityTier.COMPANY, SupportUnitGenerator.securityTier(campaignWithPersonnel(108, 0)),
              "a regiment-sized force gets a company");
    }

    @Test
    void securityTierBreakpointsAreInclusiveOfEachEchelonCeiling() {
        assertEquals(SecurityTier.PLATOON, SupportUnitGenerator.securityTier(campaignWithPersonnel(13, 0)),
              "one combatant above a company steps up to a platoon");
        assertEquals(SecurityTier.COMPANY, SupportUnitGenerator.securityTier(campaignWithPersonnel(37, 0)),
              "one combatant above a battalion steps up to a company");
    }

    @Test
    void emptyForceStillGetsASquadDetail() {
        assertEquals(SecurityTier.SQUAD, SupportUnitGenerator.securityTier(campaignWithPersonnel(0, 40)));
    }

    @Test
    void securityUnitNameIsFactionAndTierAppropriate() {
        assertEquals("Foot Squad (Rifle)", SupportUnitGenerator.securityUnitName(SecurityTier.SQUAD, false));
        assertEquals("Clan Foot Squad (Rifle)", SupportUnitGenerator.securityUnitName(SecurityTier.SQUAD, true));
        assertEquals("Foot Platoon (Rifle)", SupportUnitGenerator.securityUnitName(SecurityTier.PLATOON, false));
        // The company tier is fielded as repeated platoons, so it reuses the platoon unit name.
        assertEquals("Foot Platoon (Rifle)", SupportUnitGenerator.securityUnitName(SecurityTier.COMPANY, false));
        assertEquals("Clan Foot Point (Rifle Light)", SupportUnitGenerator.securityUnitName(SecurityTier.COMPANY, true));
    }

    /** Builds a campaign whose active roster holds {@code combatants} combat and {@code others} non-combat personnel. */
    private static Campaign campaignWithPersonnel(int combatants, int others) {
        List<Person> roster = new ArrayList<>();
        for (int index = 0; index < combatants; index++) {
            Person person = mock(Person.class);
            when(person.isCombat()).thenReturn(true);
            roster.add(person);
        }
        for (int index = 0; index < others; index++) {
            Person person = mock(Person.class);
            when(person.isCombat()).thenReturn(false);
            roster.add(person);
        }

        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getActivePersonnel(false, false)).thenReturn(roster);
        return campaign;
    }
}
