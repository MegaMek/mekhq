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
package mekhq.service.mrms;

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Aero;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mass Repair on a self-crewed ship: its engineer takes the jobs, as in the Repair tab, because the crew is the ship's
 * only tech team (issue #4081).
 */
class MRMSShipCrewTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int PREFERRED_TARGET_NUMBER = 4;
    private static final int MAXIMUM_TARGET_NUMBER = 12;
    private static final int SUCCESSFUL_DIE_FACE = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit leopard;
    private EquipmentPart damagedWeapon;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.MRMS_USE_REPAIR, true);
        List<MRMSOption> repairTypeOptions = new ArrayList<>();
        for (PartRepairType repairType : PartRepairType.getMRMSValidTypes()) {
            repairTypeOptions.add(new MRMSOption(repairType, true, EXP_ULTRA_GREEN, EXP_LEGENDARY,
                  PREFERRED_TARGET_NUMBER, MAXIMUM_TARGET_NUMBER, 0));
        }
        options.set(CampaignOption.MRMS_OPTIONS, repairTypeOptions);

        leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        damagedWeapon = PartsScenario.unitParts(leopard, EquipmentPart.class).getFirst();
        damagedWeapon.setHits(1);
        // the ship's own record of the hit, or the diagnostic would read the weapon back as undamaged
        damagedWeapon.updateConditionFromPart();
        leopard.runDiagnostic(false);
        assertTrue(damagedWeapon.needsFixing(), "The Leopard starts with a damaged weapon");
        scenario.withAsTechs(FULL_ASTECH_TEAM);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private Person crewTheShip() {
        Person crewMember = scenario.withTech(EXP_REGULAR);
        crewMember.setPrimaryRoleDirect(PersonnelRole.VESSEL_CREW);
        leopard.addVesselCrew(crewMember);
        leopard.resetEngineer();
        Person engineer = leopard.getEngineer();
        assertNotNull(engineer, "The crewed Leopard has an engineer");
        return engineer;
    }

    @Test
    void massRepairOffersACrewedShip() {
        crewTheShip();

        assertTrue(MRMSService.isMassRepairableUnit(leopard));
    }

    @Test
    void massRepairHasTheEngineerRepairTheShip() {
        Person engineer = crewTheShip();
        int engineerMinutesBefore = engineer.getMinutesLeft();

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertFalse(damagedWeapon.needsFixing(), "The engineer fixed the damaged weapon");
        assertTrue(engineer.getMinutesLeft() < engineerMinutesBefore, "The engineer spent time on the job");
    }

    @Test
    void massRepairNeverSendsOtherTechsAboardTheShip() {
        crewTheShip();
        Person aerospaceTech = scenario.withTech(EXP_REGULAR);
        aerospaceTech.setPrimaryRoleDirect(PersonnelRole.AERO_TEK);
        int aerospaceTechMinutesBefore = aerospaceTech.getMinutesLeft();

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertEquals(aerospaceTechMinutesBefore, aerospaceTech.getMinutesLeft(),
              "The aerospace tech is not part of the ship's crew");
    }

    @Test
    void massRepairLeavesAShipWithNoCrewAlone() {
        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertTrue(damagedWeapon.needsFixing(), "Nobody aboard can repair the weapon");
    }

    @Test
    void shipJobLongerThanTheEngineersDayStartsEvenWithCarryoverOff() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_ALLOW_CARRYOVER, false);
        Person engineer = crewTheShip();
        StructuralIntegrity structure = PartsScenario.unitParts(leopard, StructuralIntegrity.class).getFirst();
        Aero dropShip = (Aero) leopard.getEntity();
        dropShip.setSI(dropShip.getOSI() - 2);
        structure.updateConditionFromEntity(false);
        assertTrue(structure.getActualTime() > (Person.PRIMARY_ROLE_SUPPORT_TIME
                                                     + Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME),
              "Two points of structure take longer than a full day");

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertSame(engineer, structure.getTech(), "The engineer starts the structure repair and carries it over");
    }

    @Test
    void shipJobThatFitsInADayWaitsForTomorrowWithCarryoverOff() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_ALLOW_CARRYOVER, false);
        Person engineer = crewTheShip();
        engineer.setMinutesLeft(1);
        engineer.setOvertimeLeft(0);

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertTrue(damagedWeapon.needsFixing(), "The short job waits for the engineer's next day");
        assertNull(damagedWeapon.getTech());
    }
}
