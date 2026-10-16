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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
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
 * Mass Repair reloads a field gun platoon's guns: its ranking soldier does it when the infantry maintains itself, and
 * a Mechanic does it when Techs maintain conventional infantry.
 */
class MRMSInfantryFieldGunTest {
    private static final int PREFERRED_TARGET_NUMBER = 4;
    private static final int MAXIMUM_TARGET_NUMBER = 12;
    private static final int SUCCESSFUL_DIE_FACE = 6;
    private static final int SPARE_AMMUNITION_TONS = 2;
    private static final int FULL_ASTECH_TEAM = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit platoon;
    private AmmoBin fieldGunAmmunition;

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

        platoon = scenario.withUnit(UnitFixture.FIELD_GUN_PLATOON_AC5);
        fieldGunAmmunition = PartsScenario.unitParts(platoon, AmmoBin.class).getFirst();
        fieldGunAmmunition.setShotsNeeded(fieldGunAmmunition.getFullShots());
        fieldGunAmmunition.updateConditionFromPart();
        assertTrue(fieldGunAmmunition.needsFixing(), "The field gun starts out of ammunition");
        scenario.withSpare(new AmmoStorage(0, fieldGunAmmunition.getType(),
              fieldGunAmmunition.getType().getShots(), campaign), SPARE_AMMUNITION_TONS);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private Person crewThePlatoon() {
        Person soldier = new Person("Test", "Soldier", campaign);
        soldier.setPrimaryRoleDirect(PersonnelRole.SOLDIER);
        campaign.importPerson(soldier);
        platoon.addPilotOrSoldier(soldier);
        platoon.resetEngineer();
        soldier.resetMinutesLeft(false);
        Person engineer = platoon.getEngineer();
        assertNotNull(engineer, "The crewed platoon is led by its ranking soldier");
        return engineer;
    }

    @Test
    void massRepairOffersInfantryThatMaintainsItself() {
        assertTrue(platoon.isSelfMaintainedInfantry());

        assertTrue(MRMSService.isMassRepairableUnit(platoon));
    }

    @Test
    void massRepairHasTheRankingSoldierReloadTheFieldGuns() {
        Person soldier = crewThePlatoon();
        int soldierMinutesBefore = soldier.getMinutesLeft();

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertFalse(fieldGunAmmunition.needsFixing(), "The field gun is reloaded");
        assertTrue(soldier.getMinutesLeft() < soldierMinutesBefore, "The platoon's own soldier did the reload");
    }

    @Test
    void massRepairNeverSendsATechToInfantryThatMaintainsItself() {
        crewThePlatoon();
        Person mechanic = scenario.withTech(EXP_REGULAR);
        mechanic.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        int mechanicMinutesBefore = mechanic.getMinutesLeft();

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertEquals(mechanicMinutesBefore, mechanic.getMinutesLeft(), "The platoon reloads its own guns");
    }

    @Test
    void massRepairHasAMechanicReloadTheFieldGunsWhenTechsMaintainInfantry() {
        campaign.getCampaignOptions().set(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY, true);
        assertFalse(platoon.isSelfMaintainedInfantry());
        Person mechanic = scenario.withTech(EXP_REGULAR);
        mechanic.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        scenario.withAsTechs(FULL_ASTECH_TEAM);

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertFalse(fieldGunAmmunition.needsFixing(), "The Mechanic reloaded the field gun");
    }
}
