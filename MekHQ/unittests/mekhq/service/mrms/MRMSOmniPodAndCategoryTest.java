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

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import megamek.common.equipment.WeaponType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mass Repair puts each task under the right category row, and damaged OmniPod equipment is repaired rather than left
 * for a pod swap that will never run (issue #10216).
 */
class MRMSOmniPodAndCategoryTest {
    private static final int FULL_ASTECH_TEAM = 6;

    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private static EquipmentPart findWeapon(Unit unit, boolean isPodMounted) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            boolean isWeapon = equipmentPart.getType() instanceof WeaponType;
            if (isWeapon && (equipmentPart.isOmniPodded() == isPodMounted)) {
                return equipmentPart;
            }
        }
        return null;
    }

    @Test
    void aDestroyedWeaponFallsUnderWeaponsLikeADamagedOne() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        EquipmentPart weapon = findWeapon(locust, false);
        assertNotNull(weapon, "The Locust carries weapons");
        assertEquals(PartRepairType.WEAPON, IPartWork.findCorrectMRMSType(weapon));

        weapon.remove(false);
        MissingEquipmentPart destroyedWeapon = PartsScenario.unitParts(locust, MissingEquipmentPart.class).getFirst();

        assertEquals(PartRepairType.WEAPON, IPartWork.findCorrectMRMSType(destroyedWeapon));
    }

    @Test
    void podWorkFallsUnderTheOmniPodRow() {
        Unit epona = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);
        PodSpace podSpace = epona.getPodSpace().getFirst();

        assertEquals(PartRepairType.POD_SPACE, IPartWork.findCorrectMRMSType(podSpace));
    }

    @Test
    void withPodSwapsOffADamagedPodWeaponIsRepairedInPlace() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_REPLACE_POD, false);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        Person mechanic = scenario.withTech(EXP_REGULAR);
        mechanic.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        Unit epona = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);
        EquipmentPart podWeapon = findWeapon(epona, true);
        assertNotNull(podWeapon, "The Epona Prime carries pod-mounted weapons");
        Part spareWeapon = podWeapon.clone();
        scenario.withSpare(spareWeapon, 1);
        podWeapon.setHits(1);

        FixedDieRolls.everyDieShows(6);
        MRMSService.mrmsUnits(campaign, List.of(epona), new MRMSConfiguredOptions(campaign));

        assertEquals(0, podWeapon.getHits(), "With pod swaps off, the damaged pod weapon is repaired where it is");
        assertFalse(podWeapon.needsFixing());
    }
}
