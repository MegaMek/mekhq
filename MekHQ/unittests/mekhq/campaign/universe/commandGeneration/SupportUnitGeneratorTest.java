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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
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
        // Recruiting a crew member rolls their skills, which needs the skill table loaded.
        SkillType.initializeTypes();
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

    // --- Sizing support vehicles by the force they support (issue #10088) ---
    //
    // Before this, salvage and logistics used a flat count: four vehicles for any Inner Sphere command and ten for
    // any Clan one, so a lance and a battalion were given the same convoy. They are now sized by what the force
    // needs and fielded as whole lances or Stars of a single vehicle type.

    /** A Locust weighs twenty tons, which makes the arithmetic in these tests checkable by hand. */
    private static final int LOCUST_TONNAGE = 20;

    @Test
    void anInnerSphereFormationIsALance() {
        // The Clan side of this is a vehicle Star of ten, which the rounding cases below exercise directly: a Clan
        // Point is two vehicles, so a Star of five Points is ten.
        assertEquals(4, SupportUnitGenerator.supportFormationSize(MHQTestUtilities.getTestCampaign()),
              "an Inner Sphere command fields support vehicles by the lance");
    }

    @Test
    void aRequirementRoundsUpToWholeFormations() {
        assertEquals(4, SupportUnitGenerator.roundUpToWholeFormations(1, 4), "one truck still fields a full lance");
        assertEquals(4, SupportUnitGenerator.roundUpToWholeFormations(4, 4), "an exact lance is not rounded up");
        assertEquals(8, SupportUnitGenerator.roundUpToWholeFormations(5, 4), "five trucks means two lances");
        assertEquals(12, SupportUnitGenerator.roundUpToWholeFormations(11, 4),
              "the battalion convoy of eleven trucks is fielded as three lances");
        assertEquals(20, SupportUnitGenerator.roundUpToWholeFormations(11, 10),
              "the same requirement is two Stars for a Clan command");
    }

    @Test
    void anEmptyRequirementStillFieldsOneFormation() {
        assertEquals(4, SupportUnitGenerator.roundUpToWholeFormations(0, 4),
              "an enabled capability always fields at least one formation");
        assertEquals(3, SupportUnitGenerator.roundUpToWholeFormations(3, 0),
              "an unresolved formation size falls back on the bare requirement rather than zero");
    }

    @Test
    void theCombatTallyCountsTheFightingForce() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());

        SupportUnitGenerator.CombatForceTally tally = SupportUnitGenerator.tallyCombatForce(campaign);

        assertEquals(2, tally.units(), "both Meks count towards the force being supported");
        assertEquals(2 * LOCUST_TONNAGE, tally.tonnage(), 0.001, "tonnage is what the convoy is sized against");
    }

    @Test
    void theCombatTallyIgnoresUnitsAlreadyFiledIntoASupportFormation() {
        // The unit used here is an ordinary Tank, not a support vehicle by construction, which is exactly the case
        // that matters: a BattleMek Recovery Vehicle is a plain fifty-ton Tank, so the equipment check alone does
        // not exclude it. Without the formation check each capability inflates the next, because they are generated
        // one after another - found in a campaign log where a command sized its convoy against its own twelve
        // recovery vehicles and was given twelve trucks where eight were needed.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        Unit alreadyGranted = unitWhoseNameContains(campaign, "APC");
        assertFalse(alreadyGranted.getEntity().isSupportVehicle(),
              "this test is only meaningful while the stand-in is not caught by the support vehicle check");

        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(alreadyGranted),
              SupportTOEFormationTypes.SALVAGE_FORMATION);
        SupportUnitGenerator.CombatForceTally tally = SupportUnitGenerator.tallyCombatForce(campaign);

        assertEquals(1, tally.units(), "the granted support vehicle is not part of the force being supported");
        assertEquals(LOCUST_TONNAGE, tally.tonnage(), 0.001,
              "its tonnage must not inflate the convoy the command is given");
    }

    @Test
    void aBiggerForceGetsMoreRecoveryVehicles() {
        Campaign smallForce = campaignWithMeks(4);
        Campaign largeForce = campaignWithMeks(36);

        int smallCount = SupportUnitGenerator.salvageUnitCount(smallForce);
        int largeCount = SupportUnitGenerator.salvageUnitCount(largeForce);

        assertEquals(4, smallCount, "a lance needs one recovery vehicle, so it fields one lance of them");
        assertEquals(12, largeCount,
              "a battalion of thirty-six meets about thirty-six units, recovers about nine, and so fields three "
                    + "lances");
        assertTrue(largeCount > smallCount, "this is the defect: a battalion used to get a lance's worth");
    }

    @Test
    void aBiggerForceGetsMoreCargoTrucks() {
        // Sizes are far enough apart that the comparison holds whatever the truck's cargo bay is in the data: a
        // lance of Locusts hauls under three tons, a regiment's worth hauls about seventy.
        Campaign smallForce = campaignWithMeks(4);
        Campaign largeForce = campaignWithMeks(108);

        assertTrue(SupportUnitGenerator.logisticsUnitCount(largeForce)
                         > SupportUnitGenerator.logisticsUnitCount(smallForce),
              "a larger command hauls more supply, so it needs more trucks - this is the defect, both used to get "
                    + "four");
        assertEquals(4, SupportUnitGenerator.logisticsUnitCount(smallForce),
              "a lance still fields one full lance of trucks");
    }

    @Test
    void theCargoTruckHasAKnownCapacityToDivideBy() {
        assertTrue(SupportUnitGenerator.cargoCapacity(SupportUnitGenerator.LOGISTICS_UNIT) > 0,
              "the convoy is sized by dividing the haul by this, so a zero would silently field one lance for every "
                    + "command regardless of size");
        assertEquals(0, SupportUnitGenerator.cargoCapacity("No Such Unit At All"), 0.001,
              "an unresolvable unit is treated as no capacity rather than throwing");
    }

    @Test
    void everyCountIsAWholeNumberOfFormations() {
        for (int meks : new int[] { 1, 4, 12, 36, 108 }) {
            Campaign campaign = campaignWithMeks(meks);
            assertEquals(0, SupportUnitGenerator.salvageUnitCount(campaign) % 4,
                  meks + " Meks must field whole lances of recovery vehicles");
            assertEquals(0, SupportUnitGenerator.logisticsUnitCount(campaign) % 4,
                  meks + " Meks must field whole lances of trucks");
        }
    }

    @Test
    void recoveryVehiclesAndTrucksAreFiledAsLances() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        assertEquals(4, SupportUnitGenerator.subFormationSize(campaign,
              SupportTOEFormationTypes.SALVAGE_FORMATION), "recovery vehicles are fielded as lances");
        assertEquals(4, SupportUnitGenerator.subFormationSize(campaign,
              SupportTOEFormationTypes.LOGISTICS_FORMATION), "cargo trucks are fielded as lances");
    }

    @Test
    void mashTrucksAndCanteensAreNotBrokenIntoLances() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        // These are sized to exact need rather than to whole formations, so a pair of MASH trucks reads better
        // listed together than split across lance markers.
        assertEquals(0, SupportUnitGenerator.subFormationSize(campaign,
              SupportTOEFormationTypes.MEDICAL_FORMATION), "MASH trucks are filed flat");
        assertEquals(0, SupportUnitGenerator.subFormationSize(campaign,
              SupportTOEFormationTypes.COMMISSARY_FORMATION), "canteens are filed flat");
        assertEquals(0, SupportUnitGenerator.subFormationSize(campaign,
              SupportTOEFormationTypes.SECURITY_FORMATION), "the security detail is already platoons");
    }

    @Test
    void anInnerSphereCommandFilesLancesAndTheNameCarriesANumber() {
        String pattern = SupportUnitGenerator.subFormationPattern(MHQTestUtilities.getTestCampaign());

        assertTrue(pattern.contains("{0}"),
              "the sub-formation name must carry its number, or every lance would be filed under one name");
        assertTrue(pattern.toLowerCase().contains("lance"),
              "an Inner Sphere command files its support vehicles as lances, was: " + pattern);
    }

    @Test
    void twelveRecoveryVehiclesBecomeThreeLancesRatherThanOneLongList() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Unit> recoveryVehicles = vehiclesInHangar(campaign, 12);

        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, recoveryVehicles,
              SupportTOEFormationTypes.SALVAGE_FORMATION, 4, "Lance {0}");

        List<Formation> lances = new ArrayList<>();
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            if (formation.getName().startsWith("Lance ")) {
                lances.add(formation);
            }
        }

        assertEquals(3, lances.size(), "twelve vehicles are three lances, not one list of twelve");
        for (Formation lance : lances) {
            assertEquals(4, lance.getUnits().size(), lance.getName() + " holds a full lance of four");
        }
    }

    @Test
    void aTopUpFillsThePartLanceBeforeOpeningANewOne() {
        // Regenerating support against a grown force must land in the lance that has room, not open a fourth
        // lance beside three full ones.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Unit> first = vehiclesInHangar(campaign, 6);
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, first,
              SupportTOEFormationTypes.SALVAGE_FORMATION, 4, "Lance {0}");

        List<Unit> topUp = new ArrayList<>(vehiclesInHangar(campaign, 1));
        topUp.removeAll(first);
        assertEquals(1, topUp.size(), "the top-up must be a unit that was not already filed");
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, topUp,
              SupportTOEFormationTypes.SALVAGE_FORMATION, 4, "Lance {0}");

        int lances = 0;
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            if (formation.getName().startsWith("Lance ")) {
                lances++;
            }
        }
        assertEquals(2, lances, "seven vehicles are two lances, the second holding three");
    }

    /**
     * Adds {@code count} vehicles and returns every unit in the hangar.
     *
     * <p>Built from the hangar rather than from the return of
     * {@link UnitTestUtilities#addAndGetUnit(Campaign, megamek.common.units.Entity)}, which hands back the first unit
     * in the hangar rather than the one it just added. Collecting its return value gives the same unit N times, and a
     * formation holds unit IDs, so the duplicates collapse into one.</p>
     */
    private static List<Unit> vehiclesInHangar(Campaign campaign, int count) {
        for (int index = 0; index < count; index++) {
            UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        }
        return new ArrayList<>(campaign.getPlayerForce().getHangar().getUnits());
    }

    /** The one unit in the hangar whose name contains {@code namePart}, so a test can name the unit it means. */
    private static Unit unitWhoseNameContains(Campaign campaign, String namePart) {
        for (Unit unit : campaign.getUnits()) {
            if (unit.getName().contains(namePart)) {
                return unit;
            }
        }
        throw new AssertionError("no unit in the hangar is named like '" + namePart + "'");
    }

    /** A campaign holding {@code mekCount} Locusts, so the force being supported has a known size and tonnage. */
    private static Campaign campaignWithMeks(int mekCount) {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        for (int index = 0; index < mekCount; index++) {
            UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        }
        return campaign;
    }

    // --- Crewing a granted support vehicle (issue #10076) ---
    //
    // The rule is the one the crew assembler already applies to infantry during generation: a role that uses
    // temporary crews gets one named crew member and fills the rest of its seats from the pool, and every other role
    // gets a full crew of individual personnel.

    @Test
    void temporaryCrewsLeaveOneNamedCrewMemberAboard() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, true);
        Unit vehicle = supportVehicle(campaign);

        PersonnelRole pooledRole = SupportUnitGenerator.crewSupportUnit(campaign, vehicle,
              campaign.getPlayerForce().getFaction(), null);

        assertEquals(PersonnelRole.VEHICLE_CREW_GROUND, pooledRole,
              "the ground vehicle crew role is reported as filled from the pool");
        assertEquals(1, vehicle.getActiveCrew().size(),
              "exactly one named crew member is aboard, as the crew assembler leaves an infantry platoon");
        assertTrue(vehicle.getFullCrewSize() > 1, "the test vehicle must have more than one seat");
    }

    @Test
    void fullCrewsAreIndividualPersonnel() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, false);
        Unit vehicle = supportVehicle(campaign);

        PersonnelRole pooledRole = SupportUnitGenerator.crewSupportUnit(campaign, vehicle,
              campaign.getPlayerForce().getFaction(), null);

        assertNull(pooledRole, "nothing was drawn from the temporary crew pool");
        assertEquals(vehicle.getFullCrewSize(), vehicle.getActiveCrew().size(),
              "every seat is filled by an individual person");
    }

    @Test
    void anExplicitChoiceOverridesTheCampaignOption() {
        Campaign withTemporaryCrewsOff = MHQTestUtilities.getTestCampaign();
        withTemporaryCrewsOff.getCampaignOptions().set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, false);
        Unit pooled = supportVehicle(withTemporaryCrewsOff);

        assertEquals(PersonnelRole.VEHICLE_CREW_GROUND,
              SupportUnitGenerator.crewSupportUnit(withTemporaryCrewsOff, pooled,
                    withTemporaryCrewsOff.getPlayerForce().getFaction(),
                    SupportPersonnelToTOE.VehicleCrewSource.TEMPORARY_CREW),
              "the player asked for temporary crews even though the campaign does not use them");
        assertEquals(1, pooled.getActiveCrew().size(), "one named crew member aboard");

        Campaign withTemporaryCrewsOn = MHQTestUtilities.getTestCampaign();
        withTemporaryCrewsOn.getCampaignOptions().set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, true);
        Unit fullyCrewed = supportVehicle(withTemporaryCrewsOn);

        assertNull(SupportUnitGenerator.crewSupportUnit(withTemporaryCrewsOn, fullyCrewed,
                    withTemporaryCrewsOn.getPlayerForce().getFaction(),
                    SupportPersonnelToTOE.VehicleCrewSource.NEW_CREW),
              "the player asked for individual personnel even though the campaign uses temporary crews");
        assertEquals(fullyCrewed.getFullCrewSize(), fullyCrewed.getActiveCrew().size(),
              "every seat is filled by an individual person");
    }

    /** A multi-seat ground support vehicle, added to the campaign without a crew. */
    private static Unit supportVehicle(Campaign campaign) {
        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Prime Mover", true);
        assertNotNull(entity, "Prime Mover.blk must be present in the test resources");
        Unit unit = campaign.addNewUnit(entity, false, 0, PartQuality.QUALITY_D);
        assertEquals(0, unit.getActiveCrew().size(), "the vehicle starts crewless");
        return unit;
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
