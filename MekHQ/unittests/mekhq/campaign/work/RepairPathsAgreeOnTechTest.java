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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.work.RepairTechEligibility.Refusal;
import mekhq.campaign.work.RepairTechEligibility.TechListToggles;
import mekhq.service.mrms.MRMSConfiguredOptions;
import mekhq.service.mrms.MRMSOption;
import mekhq.service.mrms.MRMSService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The Repair tab's tech list, the to-hit check and Mass Repair apply the same rules about which tech may work on a task
 * (issue #10425).
 */
class RepairPathsAgreeOnTechTest {
    private static final TechListToggles TECHNICIANS_ONLY = new TechListToggles(false, false);
    private static final int SUCCESSFUL_FACE = 6;
    private static final int ONE_SHIFT_OF_OVERTIME = 240;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private EquipmentPart damagedPart;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        damagedPart = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst();
        damagedPart.setHits(1);
        tech = scenario.withTech(EXP_REGULAR);
        scenario.withAsTechs(6);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private Refusal repairTabRefusalFor(Person candidate) {
        return RepairTechEligibility.findRefusal(campaign, damagedPart, locust, candidate, TECHNICIANS_ONLY);
    }

    private boolean isImpossible(TargetRoll target) {
        return target.getValue() == TargetRoll.IMPOSSIBLE;
    }

    /** A tech who knows only aerospace work, the wrong type of tech for a Mek. */
    private Person aerospaceTech() {
        Person aerospaceTech = new Person("Aero", "Tech", campaign);
        aerospaceTech.setPrimaryRoleDirect(PersonnelRole.AERO_TEK);
        SkillType aerospaceSkill = SkillType.getType(SkillType.S_TECH_AERO);
        aerospaceTech.addSkill(SkillType.S_TECH_AERO, aerospaceSkill.getLevelFromExperience(EXP_REGULAR), 0);
        campaign.importPerson(aerospaceTech);
        aerospaceTech.resetMinutesLeft(false);
        return aerospaceTech;
    }

    private void tieUpWithARefitOfAnotherUnit(Person refitTech) {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        wolverine.setRefit(refit);
        refit.setTech(refitTech);
    }

    private void leaveOnlyOvertime(Person overtimeTech) {
        overtimeTech.setMinutesLeft(0);
        overtimeTech.setOvertimeLeft(ONE_SHIFT_OF_OVERTIME);
    }

    private void massRepairTheLocust() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        for (MRMSOption option : campaign.getCampaignOptions().get(CampaignOption.MRMS_OPTIONS)) {
            option.setActive(true);
        }
        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);
        MRMSService.mrmsUnits(campaign, List.of(locust), new MRMSConfiguredOptions(campaign));
    }

    @Test
    void theRepairTabDoesNotOfferATechBusyWithARefit() {
        tieUpWithARefitOfAnotherUnit(tech);

        assertEquals(Refusal.BUSY_WITH_REFIT, repairTabRefusalFor(tech));
        assertTrue(isImpossible(campaign.getTargetFor(damagedPart, tech)), "The to-hit check agrees");
    }

    @Test
    void theRepairTabOffersATechWithOnlyOvertimeLeftWhenOvertimeIsAllowed() {
        leaveOnlyOvertime(tech);

        campaign.setOvertime(true);
        assertNull(repairTabRefusalFor(tech));
        assertFalse(isImpossible(campaign.getTargetFor(damagedPart, tech)), "The to-hit check agrees");

        campaign.setOvertime(false);
        assertEquals(Refusal.NO_TIME_LEFT, repairTabRefusalFor(tech));
        assertTrue(isImpossible(campaign.getTargetFor(damagedPart, tech)), "The to-hit check agrees");
    }

    @Test
    void theToHitCheckRefusesATechAtAnotherBase() {
        assertFalse(isImpossible(campaign.getTargetFor(damagedPart, tech)), "At the unit the tech gets a number");

        LocationManager.setLocation(tech, new PlayerBase(new FixedLocation(mock(PlanetarySystem.class))));

        TargetRoll target = campaign.getTargetFor(damagedPart, tech);
        assertTrue(isImpossible(target));
        assertEquals(Refusal.AT_ANOTHER_LOCATION.getDescription(), target.getDesc());
    }

    @Test
    void theToHitCheckRefusesTheWrongTypeOfTech() {
        Person aerospaceTech = aerospaceTech();

        assertEquals(Refusal.WRONG_SKILL, repairTabRefusalFor(aerospaceTech));
        TargetRoll target = campaign.getTargetFor(damagedPart, aerospaceTech);
        assertTrue(isImpossible(target));
        assertEquals(Refusal.WRONG_SKILL.getDescription(), target.getDesc());
    }

    @Test
    void theToHitCheckRefusesVesselCrewOnAnotherUnit() {
        tech.setPrimaryRoleDirect(PersonnelRole.VESSEL_CREW);

        assertEquals(Refusal.VESSEL_CREW_ON_ANOTHER_UNIT, repairTabRefusalFor(tech));
        TargetRoll target = campaign.getTargetFor(damagedPart, tech);
        assertTrue(isImpossible(target));
        assertEquals(Refusal.VESSEL_CREW_ON_ANOTHER_UNIT.getDescription(), target.getDesc());
    }

    @Test
    void aJobLeftWithTheWrongTypeOfTechIsStoppedRatherThanRolled() {
        Person aerospaceTech = aerospaceTech();
        damagedPart.setTech(aerospaceTech);

        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);
        campaign.fixPart(damagedPart, aerospaceTech);

        assertEquals(1, damagedPart.getHits(), "No repair roll was made");
        assertNull(damagedPart.getTech(), "The job is stopped and left for the right tech");
    }

    @Test
    void massRepairUsesATechWithOnlyOvertimeLeftWhenOvertimeIsAllowed() {
        leaveOnlyOvertime(tech);
        campaign.setOvertime(true);

        massRepairTheLocust();

        // The repair takes longer than one shift of overtime, so it carries on tomorrow with the same tech
        assertSame(tech, damagedPart.getTech(), "Mass Repair put the tech on the job");
        assertEquals(0, tech.getOvertimeLeft(), "The tech worked their overtime on it");
    }

    @Test
    void massRepairLeavesATechWithOnlyOvertimeAloneWhenOvertimeIsOff() {
        leaveOnlyOvertime(tech);
        campaign.setOvertime(false);

        massRepairTheLocust();

        assertNull(damagedPart.getTech());
        assertEquals(1, damagedPart.getHits());
    }

    @Test
    void massRepairLetsAnyTechTryUnderDestroyByMargin() {
        damagedPart.setSkillMin(EXP_VETERAN);
        campaign.getCampaignOptions().set(CampaignOption.DESTROY_BY_MARGIN, true);
        assertNull(repairTabRefusalFor(tech), "The Repair tab lets the regular tech try");

        massRepairTheLocust();

        assertEquals(0, damagedPart.getHits(), "Mass Repair let the regular tech try too");
    }

    @Test
    void massRepairKeepsTheMinimumSkillWithoutDestroyByMargin() {
        damagedPart.setSkillMin(EXP_VETERAN);
        campaign.getCampaignOptions().set(CampaignOption.DESTROY_BY_MARGIN, false);
        assertEquals(Refusal.SKILL_TOO_LOW, repairTabRefusalFor(tech));

        massRepairTheLocust();

        assertEquals(1, damagedPart.getHits());
    }

    @Test
    void massRepairStillWorksOnAUnitAtABase() {
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        campaign.getPlayerForce().getHangar().removeUnit(locust.getId());
        base.getBaseHangar().addUnit(locust);
        for (Part part : List.copyOf(locust.getParts())) {
            campaign.getPlayerForce().getWarehouse().removePart(part);
            base.getBaseWarehouse().addPart(part);
        }
        LocationManager.setLocation(tech, base);

        massRepairTheLocust();

        // Mass Repair first asks whether anyone could do the job, using a stand-in tech who is at no base. Only the
        // real tech's location may count, or nothing at a base would ever be repaired.
        assertEquals(0, damagedPart.getHits(), "The base's tech repaired it");
    }

    @Test
    void everyRefusalHasAReasonInWords() {
        for (Refusal refusal : Refusal.values()) {
            String description = refusal.getDescription();
            assertFalse(description.isBlank(), refusal.name());
            assertNotEquals('!', description.charAt(0), refusal.name() + " has no text in the resource bundle");
        }
    }
}
