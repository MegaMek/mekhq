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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.InfantryWeaponPart;
import mekhq.campaign.parts.equipment.MissingInfantryWeaponPart;
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
 * Instant Mass Repair on a conventional infantry platoon (issue #8824). When Techs maintain conventional infantry it
 * fixes the platoon's damaged weapons and replaces lost ones; otherwise the platoon looks after itself and Instant Mass
 * Repair leaves it alone.
 */
class MRMSInfantryTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int PREFERRED_TARGET_NUMBER = 4;
    private static final int MAXIMUM_TARGET_NUMBER = 8;
    private static final int SUCCESSFUL_DIE_FACE = 6;

    private PartsScenario scenario;
    private Campaign campaign;

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
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    /**
     * Adds a foot platoon with one damaged laser rifle and one lost, a spare to replace the lost one, and a mechanic
     * with a full team of AsTechs.
     */
    private Unit damagedPlatoon(boolean isTechsMaintainInfantry) {
        campaign.getCampaignOptions().set(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY, isTechsMaintainInfantry);
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        List<InfantryWeaponPart> rifles = PartsScenario.unitParts(platoon, InfantryWeaponPart.class);
        rifles.getFirst().setHits(1);
        rifles.getLast().remove(false);
        MissingInfantryWeaponPart lostRifle = PartsScenario.unitParts(platoon, MissingInfantryWeaponPart.class)
                                                    .getFirst();
        scenario.withSpare(lostRifle.getNewPart(), 1);
        platoon.runDiagnostic(false);

        scenario.withAsTechs(FULL_ASTECH_TEAM);
        Person mechanic = scenario.withTech(EXP_REGULAR);
        mechanic.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        return platoon;
    }

    private static int damagedRifles(Unit platoon) {
        int damagedRifles = 0;
        for (InfantryWeaponPart rifle : PartsScenario.unitParts(platoon, InfantryWeaponPart.class)) {
            if (rifle.needsFixing()) {
                damagedRifles++;
            }
        }
        return damagedRifles;
    }

    @Test
    void instantMassRepairFixesAndReplacesWeaponsWhenTechsMaintainInfantry() {
        Unit platoon = damagedPlatoon(true);
        assertTrue(MRMSService.isMassRepairableUnit(platoon));

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertEquals(0, damagedRifles(platoon), "The damaged laser rifle is fixed");
        assertEquals(0, PartsScenario.unitParts(platoon, MissingInfantryWeaponPart.class).size(),
              "The lost laser rifle is replaced from the spare");
    }

    @Test
    void instantMassRepairLeavesAPlatoonThatMaintainsItselfAlone() {
        Unit platoon = damagedPlatoon(false);
        assertTrue(platoon.isSelfCrewed(), "With the option off the platoon looks after itself");

        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
        MRMSService.mrmsAllUnits(campaign);

        assertEquals(1, damagedRifles(platoon), "The damaged laser rifle is left for the platoon");
        assertEquals(1, PartsScenario.unitParts(platoon, MissingInfantryWeaponPart.class).size(),
              "The lost laser rifle is left for the platoon");
    }
}
