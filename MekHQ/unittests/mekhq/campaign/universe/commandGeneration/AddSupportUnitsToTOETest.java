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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.scenarios.salvage.SalvageSystem;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;
import mekhq.gui.campaignOptions.optionChangeDialogs.SupportCapabilityGrantDialog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies that support-vehicle placement reuses an existing sub-formation of the same type instead of
 * creating a duplicate, so a top-up generation lands in the existing formation.
 */
class AddSupportUnitsToTOETest {

    /** The real role lookup, put back after each test that stands in for the force generator. */
    private Function<String, Set<MissionRole>> realRoleLookup;

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @BeforeEach
    void rememberRoleLookup() {
        realRoleLookup = SupportVehicleSelector.roleLookup;
    }

    @AfterEach
    void restoreRoleLookup() {
        SupportVehicleSelector.roleLookup = realRoleLookup;
    }

    @Test
    void organize_putsAnIdleOwnedRecoveryVehicleToWork() {
        // Issue 10375: four recovery vehicles the player had bought and left in the hangar stayed there, crewless,
        // while new ones were built and crewed beside them. An owned vehicle must be crewed before anything is built.
        Campaign campaign = campaignWithCamOpsSalvage();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        Unit owned = unitNamedLike(campaign, "APC");
        assertTrue(owned.getCrew().isEmpty(), "this test is only meaningful while the owned vehicle is crewless");

        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.MECHANIC, 8), false,
              campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        assertTrue(supportCommand.getAllUnits(false).contains(owned.getId()),
              "the owned recovery vehicle must join the support teams");
        assertFalse(owned.getCrew().isEmpty(), "and be crewed from the maintenance staff");
        for (Person crewMember : owned.getCrew()) {
            assertEquals(PersonnelRole.MECHANIC, crewMember.getPrimaryRole(),
                  "its crew comes from the section, not from new hires");
        }
    }

    @Test
    void topUp_putsAnIdleOwnedRecoveryVehicleToWorkFromExistingStaff() {
        // Issue 10375: switching salvage on mid-campaign counted a recovery vehicle the player already owned towards
        // the target, built only the rest, and left the owned one in the hangar with nobody aboard.
        Campaign campaign = campaignWithCamOpsSalvage();
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.LEGACY);
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.MECHANIC, 8), false,
              campaign.getPlayerForce().getFaction());
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        Unit owned = unitNamedLike(campaign, "APC");

        SupportPersonnelToTOE.topUpCapabilityVehicles(campaign, SupportCapability.SALVAGE,
              SupportPersonnelToTOE.VehicleCrewSource.EXISTING_STAFF, campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertTrue(supportCommand.getAllUnits(false).contains(owned.getId()),
              "the owned recovery vehicle must join the support teams");
        assertFalse(owned.getCrew().isEmpty(), "and be crewed");
        for (Person crewMember : owned.getCrew()) {
            assertEquals(PersonnelRole.MECHANIC, crewMember.getPrimaryRole(),
                  "from the maintenance staff, as the player chose, not from new hires");
        }
    }

    @Test
    void grant_filesARecoveryVehicleInSupportCommandWhoeverCrewsIt() {
        // Granting recovery vehicles or MASH trucks with new hires used to stand them up in a formation of their own,
        // so a campaign with support teams ended up with a second Medical formation beside Support Command's.
        Campaign campaign = campaignWithCamOpsSalvage();
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.LEGACY);
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.MECHANIC, 8), false,
              campaign.getPlayerForce().getFaction());
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        Unit owned = unitNamedLike(campaign, "APC");

        SupportCapabilityGrantDialog.processFreeUnits(campaign, campaign.getPlayerForce().getFaction(), true,
              SupportCapability.SALVAGE, SupportPersonnelToTOE.VehicleCrewSource.NEW_CREW);

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertTrue(supportCommand.getAllUnits(false).contains(owned.getId()),
              "the recovery vehicle joins Support Command even though new hires crew it");
        assertFalse(owned.getCrew().isEmpty(), "and it is crewed");
        for (Person crewMember : owned.getCrew()) {
            assertFalse(crewMember.getPrimaryRole().isTech(), "by new hires, not by the maintenance staff");
        }
        String standaloneLabel = SupportTOEFormationTypes.SALVAGE_FORMATION.getLabel();
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            assertFalse(formation.getName().equalsIgnoreCase(standaloneLabel),
                  "no recovery formation may stand beside Support Command");
        }
    }

    @Test
    void topUp_withATemporaryCrewNeedsNoSectionStaff() {
        // The temporary crew's named member used to be taken from the section's staff, so a section with nobody left
        // to spare built nothing. A temporary crew is a new hire plus the pool, as everywhere else.
        Campaign campaign = campaignWithCamOpsSalvage();
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.LEGACY);
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.ADMINISTRATOR, 4), false,
              campaign.getPlayerForce().getFaction());
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        Unit owned = unitNamedLike(campaign, "APC");

        SupportPersonnelToTOE.topUpCapabilityVehicles(campaign, SupportCapability.SALVAGE,
              SupportPersonnelToTOE.VehicleCrewSource.TEMPORARY_CREW, campaign.getPlayerForce().getFaction());

        assertTrue(campaign.getPlayerForce().getSupportCommandFormation().getAllUnits(false).contains(owned.getId()),
              "the recovery vehicle joins Support Command");
        assertFalse(owned.getCrew().isEmpty(), "with its named temporary crew member aboard");
    }

    @Test
    void arrangeIntoLances_leavesOneLancesWorthFlatAndSplitsMore() {
        // Issue 10375: eleven MASH trucks sat directly under a Medical company, with no lances in it.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation small = groupWithVehicles(campaign, "Small Group", 4);
        Formation medical = groupWithVehicles(campaign, "Medical Group", 11);

        assertTrue(AddSupportUnitsToTOE.arrangeIntoLances(campaign, small, 4, AddSupportUnitsToTOETest::lanceName,
              FormationType.SUPPORT).isEmpty(), "a group of one lance's worth is that lance");
        assertEquals(4, small.getUnits().size());

        List<Formation> lances = AddSupportUnitsToTOE.arrangeIntoLances(campaign, medical, 4,
              AddSupportUnitsToTOETest::lanceName, FormationType.SUPPORT);

        assertEquals(3, lances.size(), "eleven vehicles make three lances");
        assertTrue(medical.getUnits().isEmpty(), "no vehicle is left directly under the company");
        assertEquals(List.of(4, 4, 3), lanceSizes(medical), "lances are filled in order");
    }

    @Test
    void arrangeIntoLances_dropsAnEmptyLanceAndMovesNoOne() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation group = groupWithVehicles(campaign, "Recovery Group", 8);
        AddSupportUnitsToTOE.arrangeIntoLances(campaign, group, 4, AddSupportUnitsToTOETest::lanceName,
              FormationType.SUPPORT);
        Formation second = group.getSubFormations().get(1);
        List<java.util.UUID> firstLance = new ArrayList<>(group.getSubFormations().get(0).getUnits());
        for (java.util.UUID unitId : new ArrayList<>(second.getUnits())) {
            campaign.getPlayerForce().removeUnitFromFormation(campaign.getUnit(unitId), campaign);
        }

        AddSupportUnitsToTOE.arrangeIntoLances(campaign, group, 4, AddSupportUnitsToTOETest::lanceName,
              FormationType.SUPPORT);

        assertEquals(1, group.getSubFormations().size(), "the emptied lance is removed");
        assertEquals(firstLance, group.getSubFormations().get(0).getUnits(), "and the other lance is not reshuffled");
    }

    @Test
    void resizeSupportEchelons_lancesAFlatGroupInSupportCommand() {
        // An older save, or a grant before this fix, left a Support Command vehicle group flat. Loading re-sizes the
        // teams, which now files those vehicles into lances.
        Campaign campaign = campaignWithCamOpsSalvage();
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.LEGACY);
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.ADMINISTRATOR, 4), false,
              campaign.getPlayerForce().getFaction());
        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        Formation recovery = new Formation("Recovery Company");
        campaign.getPlayerForce().addFormation(recovery, supportCommand, campaign);
        for (int index = 0; index < 6; index++) {
            UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        }
        for (Unit vehicle : unitsNamedLike(campaign, "APC")) {
            campaign.getPlayerForce().addUnitToFormation(vehicle, recovery.getId(), campaign);
        }

        SupportPersonnelToTOE.resizeSupportEchelons(campaign);

        assertTrue(recovery.getUnits().isEmpty(), "the vehicles moved into lances");
        assertEquals(List.of(4, 2), lanceSizes(recovery));
        assertEquals(FormationLevel.COMPANY, recovery.getFormationLevel(), "six vehicles in two lances is a company");
    }

    private static String lanceName(int position) {
        return "Lance " + position;
    }

    private static Formation groupWithVehicles(Campaign campaign, String name, int count) {
        Formation group = new Formation(name);
        campaign.getPlayerForce().addFormation(group,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        int before = campaign.getUnits().size();
        for (int index = 0; index < count; index++) {
            UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        }
        List<Unit> all = new ArrayList<>(campaign.getUnits());
        for (Unit vehicle : all.subList(before, all.size())) {
            campaign.getPlayerForce().addUnitToFormation(vehicle, group.getId(), campaign);
        }
        return group;
    }

    private static List<Integer> lanceSizes(Formation group) {
        List<Integer> sizes = new ArrayList<>();
        for (Formation lance : group.getSubFormations()) {
            sizes.add(lance.getUnits().size());
        }
        return sizes;
    }

    @Test
    void seat_aProfessionsFirstSquadGoesInItsSection() {
        // Every medic was crewing a MASH truck, so the next medic's new squad had no other squad to sit beside and was
        // filed at the top of Support Command. It belongs in the Medical section.
        Campaign campaign = campaignWithCamOpsSalvage();
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.ADMINISTRATOR, 4), false,
              campaign.getPlayerForce().getFaction());
        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        Person medic = newStaff(campaign, PersonnelRole.MEDIC, 1).get(0);
        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, medic, true, true);

        SupportCarrierReconciler.seatIfEligible(campaign, medic);

        assertNotNull(medic.getUnit(), "the medic is seated");
        Formation squadHome = campaign.getPlayerForce().getFormation(medic.getUnit().getFormationId());
        assertEquals("Medical", squadHome.getName(), "in the Medical section");
        assertEquals(supportCommand.getId(), squadHome.getParentFormation().getId(), "which sits in Support Command");
    }

    @Test
    void supportPasses_neverRemoveAnEmptyCombatLance() {
        // Only support formations are tidied. A combat lance that loses every unit in battle must survive, even one
        // whose name matches the support lance names, and even one under Headquarters.
        Campaign campaign = campaignWithCamOpsSalvage();
        SupportPersonnelToTOE.organize(campaign, newStaff(campaign, PersonnelRole.ADMINISTRATOR, 4), false,
              campaign.getPlayerForce().getFaction());
        Formation origin = campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN);
        Formation combatCompany = new Formation("Heavy Mek Company");
        campaign.getPlayerForce().addFormation(combatCompany, origin, campaign);
        Formation combatLance = new Formation("Able Lance");
        campaign.getPlayerForce().addFormation(combatLance, combatCompany, campaign);
        Formation headquarters = AddSupportUnitsToTOE.getHqFormation(campaign);
        Formation commandLance = new Formation("Able Lance");
        campaign.getPlayerForce().addFormation(commandLance, headquarters, campaign);

        SupportPersonnelToTOE.resizeSupportEchelons(campaign);
        SupportUnitGenerator.decorateGrantedSupportFormations(campaign);
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(UnitTestUtilities.addAndGetUnit(campaign,
              UnitTestUtilities.getHeavyTrackedApcStandard())), SupportTOEFormationTypes.LOGISTICS_FORMATION, 4,
              position -> SupportUnitGenerator.subFormationNamer(campaign.getPlayerForce().getFaction(), null)
                                .apply(position));

        assertNotNull(campaign.getPlayerForce().getFormation(combatLance.getId()), "the empty combat lance survives");
        assertNotNull(campaign.getPlayerForce().getFormation(commandLance.getId()),
              "and so does an empty lance under Headquarters");
    }

    @Test
    void idleOwnedVehicles_leavesAVehicleThePlayerHasPutToUseAlone() {
        // A vehicle the player crewed, or filed in a formation of their own, is in use. It counts towards the
        // target but is not taken over by the support teams.
        Campaign campaign = campaignWithCamOpsSalvage();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        List<Unit> recoveryVehicles = unitsNamedLike(campaign, "APC");
        Unit crewed = recoveryVehicles.get(0);
        Unit filed = recoveryVehicles.get(1);
        Unit idle = recoveryVehicles.get(2);
        crewed.addDriver(newStaff(campaign, PersonnelRole.VEHICLE_CREW_GROUND, 1).get(0));
        Formation origin = campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN);
        Formation playerLance = new Formation("Salvage Lance");
        campaign.getPlayerForce().addFormation(playerLance, origin, campaign);
        campaign.getPlayerForce().addUnitToFormation(filed, playerLance.getId(), campaign);

        List<Unit> idleVehicles = SupportPersonnelToTOE.idleOwnedVehicles(campaign, SupportSection.MAINTENANCE);

        assertEquals(List.of(idle), idleVehicles, "only the crewless vehicle filed nowhere is idle");
        assertTrue(SupportPersonnelToTOE.idleOwnedVehicles(campaign, SupportSection.MEDICAL).isEmpty(),
              "a recovery vehicle is never handed to the medical section");
    }

    @Test
    void reusesExistingSubFormationOfSameType() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit first = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        Unit second = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1E());

        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(first),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(second),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);

        String label = SupportTOEFormationTypes.COMMISSARY_FORMATION.getLabel();
        long matching = campaign.getPlayerForce().getAllFormations().stream()
              .filter(formation -> formation.getName().equalsIgnoreCase(label))
              .count();
        assertEquals(1, matching, "a second support batch of the same type must reuse the existing formation");
    }

    @Test
    void organize_collapsesASectionThatHoldsOneCompany() {
        // Twelve administrators pack into two squads, which become one Administrator company under the Command
        // section. That section then groups nothing, so it should hold the carriers itself rather than adding a layer
        // - every extra layer pushes the whole TOE up an echelon.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> administrators = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            administrators.add(campaign.getPlayerForce().getHumanResources()
                                    .newPerson(campaign, PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, administrators, false,
              campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand, "organize must record the Support Command formation");

        // Command is the only section, so it collapses into Support Command too, leaving the carriers one step down.
        assertTrue(supportCommand.getSubFormations().isEmpty(),
              "a Support Command holding a single section must not keep the section layer");
        assertEquals(2, supportCommand.getUnits().size(),
              "both administrator carriers should hang directly off Support Command");

        // Two squads is a platoon's worth, so that is what it says. Depth-based levels called this a Battalion.
        assertEquals(FormationLevel.LANCE, supportCommand.getFormationLevel(),
              "a support command holding two squads must be sized as a platoon, not by how deep the tree is");
    }

    @Test
    void organize_joinsTheCampaignsExistingSupportHome() {
        // A hand-built campaign keeps its support under whatever the player named it - here a Command Battalion with
        // a convoy and a salvage detachment. Support Command must join it rather than standing up a rival
        // Headquarters, which would leave the TOE with two support structures.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation origin = campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN);
        Formation commandBattalion = new Formation("Command Battalion");
        campaign.getPlayerForce().addFormation(commandBattalion, origin, campaign);
        for (String name : List.of("Logistics", "Salvage")) {
            Formation detachment = new Formation(name);
            campaign.getPlayerForce().addFormation(detachment, commandBattalion, campaign);
            detachment.setFormationType(name.equals("Salvage") ? FormationType.SALVAGE : FormationType.CONVOY, true);
        }

        List<Person> administrators = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            administrators.add(campaign.getPlayerForce().getHumanResources()
                                     .newPerson(campaign, PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE));
        }
        SupportPersonnelToTOE.organize(campaign, administrators, false,
              campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        assertEquals(commandBattalion.getId(), supportCommand.getParentFormation().getId(),
              "Support Command must join the existing support home");
        assertTrue(campaign.getPlayerForce().getAllFormations().stream()
                    .noneMatch(formation -> formation.getName().equalsIgnoreCase("Headquarters")),
              "no rival Headquarters may be created when the campaign already has a support home");
    }

    @Test
    void organize_sizesEachSupportFormationByWhatItHolds() {
        // 30 technicians pack into a full 28-person platoon plus a 2-person squad: 4 + 1 = 5 squads, which is more
        // than one platoon and so reads as a company.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> technicians = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            technicians.add(campaign.getPlayerForce().getHumanResources()
                                  .newPerson(campaign, PersonnelRole.MEK_TECH, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, technicians, false,
              campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        assertEquals(FormationLevel.COMPANY, supportCommand.getFormationLevel(),
              "a platoon plus a squad is five squads, which is a company's worth");
    }

    @Test
    void vehiclesStillNeeded_aRolledCapabilityNeverAsksForAVehicleCalledNull() {
        // With support teams on, salvage and medical are built here rather than standalone, and this path used to
        // resolve the model by name. Once those capabilities stopped naming one, it asked the cache for a unit
        // called null and a command silently lost its recovery vehicles and MASH trucks.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Faction faction = campaign.getPlayerForce().getFaction();
        assertNull(SupportCapability.SALVAGE.unitName(campaign),
              "this test is only meaningful while salvage rolls its vehicle rather than naming it");

        List<SupportPersonnelToTOE.VehicleSpec> specs = SupportPersonnelToTOE.vehiclesStillNeeded(campaign,
              SupportCapability.SALVAGE, faction, 4);

        for (SupportPersonnelToTOE.VehicleSpec spec : specs) {
            assertNotNull(spec.unitName(), "a spec with no unit name cannot be built from");
            assertTrue(spec.count() > 0, "a spec must ask for at least one vehicle");
        }
    }

    @Test
    void vehiclesStillNeeded_aNamedCapabilityStillResolvesByName() {
        // The security detail is infantry and still names its unit, so that path must be untouched.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Faction faction = campaign.getPlayerForce().getFaction();
        String expected = SupportCapability.SECURITY.unitName(campaign);
        assertNotNull(expected, "the security detail names its unit");

        List<SupportPersonnelToTOE.VehicleSpec> specs = SupportPersonnelToTOE.vehiclesStillNeeded(campaign,
              SupportCapability.SECURITY, faction, 2);

        assertEquals(1, specs.size());
        assertEquals(expected, specs.get(0).unitName());
    }

    @Test
    void vehiclesStillNeeded_freshCampaignGetsTheFullCount() {
        // A newly generated command owns no capability vehicles, so generation is unchanged: it builds them all.
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        List<SupportPersonnelToTOE.VehicleSpec> specs = SupportPersonnelToTOE.vehiclesStillNeeded(campaign,
              "Locust LCT-1V", 3);

        assertEquals(1, specs.size());
        assertEquals("Locust LCT-1V", specs.get(0).unitName());
        assertEquals(3, specs.get(0).count(), "a campaign that owns none must get the full count");
    }

    @Test
    void vehiclesStillNeeded_buildsOnlyWhatTheCampaignLacks() {
        // Issue 10037: converting a campaign to support teams built a full set of MASH and recovery vehicles on top
        // of the ones it already owned. Two owned against a target of three means one more, not three.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());

        List<SupportPersonnelToTOE.VehicleSpec> specs = SupportPersonnelToTOE.vehiclesStillNeeded(campaign,
              "Locust LCT-1V", 3);

        assertEquals(1, specs.size());
        assertEquals(1, specs.get(0).count(), "only the vehicle the campaign lacks may be built");
    }

    @Test
    void vehiclesStillNeeded_buildsNothingWhenTheTargetIsAlreadyOwned() {
        // An empty list, not a spec of zero: organizeSection creates the vehicle company for any non-empty list, so a
        // zero-count spec would leave an empty formation in the TOE.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());

        assertTrue(SupportPersonnelToTOE.vehiclesStillNeeded(campaign, "Locust LCT-1V", 2).isEmpty(),
              "a campaign that already fields the target must get no new vehicles");
    }

    @Test
    void organize_keepsTheLayersThatSeparateProfessions() {
        // Two professions means the companies actually tell them apart, so nothing collapses.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> staff = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            staff.add(campaign.getPlayerForce().getHumanResources()
                              .newPerson(campaign, PersonnelRole.MEK_TECH, PersonnelRole.NONE));
        }
        for (int index = 0; index < 12; index++) {
            staff.add(campaign.getPlayerForce().getHumanResources()
                              .newPerson(campaign, PersonnelRole.MECHANIC, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, staff, false,
              campaign.getPlayerForce().getFaction());

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        // Maintenance is still the only section, so it collapses into Support Command, but the two profession
        // companies below it survive: they are what separates the technicians from the mechanics.
        assertEquals(2, supportCommand.getSubFormations().size(),
              "the two profession companies must survive, one per profession");
    }

    /**
     * A test campaign with support teams and CamOps salvage on, so recovery vehicles join the maintenance section, and
     * with the APC standing in for a recovery vehicle. The force generator data that names the real ones is not staged
     * for tests.
     */
    private static Campaign campaignWithCamOpsSalvage() {
        SupportVehicleSelector.roleLookup = unitName -> unitName.contains("APC")
                                                              ? Set.of(MissionRole.RECOVERY)
                                                              : Set.of();
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        campaign.getCampaignOptions().set(CampaignOption.USE_SUPPORT_TEAMS, true);
        assertTrue(SupportCapability.SALVAGE.joinsSection(campaign) && SupportCapability.SALVAGE.isEnabled(campaign),
              "this test is only meaningful while recovery vehicles are on and join the maintenance section");
        return campaign;
    }

    private static List<Person> newStaff(Campaign campaign, PersonnelRole role, int count) {
        List<Person> staff = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            staff.add(campaign.getPlayerForce().getHumanResources().newPerson(campaign, role, PersonnelRole.NONE));
        }
        return staff;
    }

    private static Unit unitNamedLike(Campaign campaign, String namePart) {
        List<Unit> matches = unitsNamedLike(campaign, namePart);
        assertEquals(1, matches.size(), "expected exactly one unit named like '" + namePart + "'");
        return matches.get(0);
    }

    private static List<Unit> unitsNamedLike(Campaign campaign, String namePart) {
        List<Unit> matches = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (unit.getName().contains(namePart)) {
                matches.add(unit);
            }
        }
        return matches;
    }
}
