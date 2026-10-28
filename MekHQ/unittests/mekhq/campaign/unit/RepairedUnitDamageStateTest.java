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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Mek brought in from the battlefield and fully repaired by techs reads as undamaged in the hangar, as it does after
 * the campaign is reloaded (issue #5789).
 */
class RepairedUnitDamageStateTest {
    private static final int SUCCESSFUL_DIE_FACE = 6;
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int MAXIMUM_REPAIR_DAYS = 30;
    private static final int SPARE_ARMOR_POINTS = 100;
    private static final int RIGHT_TORSO_STRUCTURE_LOST = 5;
    private static final int RIGHT_LEG_STRUCTURE_LOST = 1;

    private PartsScenario scenario;
    private Campaign campaign;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        tech = scenario.withTech(SkillType.EXP_ELITE);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    /** Battle damage on a Locust: armor and structure lost, one machine gun destroyed, its ammunition used up. */
    private static Entity damagedLocust() {
        Entity locust = UnitFixture.LOCUST_LCT_1V.loadEntity();
        locust.setArmor(locust.getOArmor(Mek.LOC_CENTER_TORSO) / 2, Mek.LOC_CENTER_TORSO);
        locust.setArmor(0, Mek.LOC_LEFT_ARM);
        locust.setInternal(locust.getOInternal(Mek.LOC_LEFT_ARM) - 1, Mek.LOC_LEFT_ARM);
        WeaponMounted machineGun = locust.getWeaponList()
                                         .stream()
                                         .filter(weapon -> weapon.getType().getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                                         .findFirst()
                                         .orElseThrow();
        destroy(locust, machineGun);
        for (AmmoMounted ammunition : locust.getAmmo()) {
            ammunition.setShotsLeft(0);
        }
        return locust;
    }

    private static void destroy(Entity entity, WeaponMounted weapon) {
        weapon.setHit(true);
        weapon.setDestroyed(true);
        int location = weapon.getLocation();
        for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
            CriticalSlot slot = entity.getCritical(location, slotIndex);
            if ((slot != null) && (slot.getMount() == weapon)) {
                slot.setHit(true);
                slot.setDestroyed(true);
            }
        }
    }

    /** Stocks the warehouse with whatever the part needs, then has the tech work on it for a day. */
    private void repair(Part part) {
        if ((part instanceof MissingPart missingPart) && (missingPart.findReplacement(false) == null)) {
            scenario.withSpare(missingPart.getNewPart(), 1);
        } else if (part instanceof AmmoBin ammunitionBin) {
            scenario.withSpare(new AmmoStorage(0, ammunitionBin.getType(), ammunitionBin.getShotsNeeded(),
                  campaign), 1);
        } else if (part instanceof Armor armor) {
            Armor spareArmor = armor.clone();
            spareArmor.setAmount(SPARE_ARMOR_POINTS);
            scenario.withSpare(spareArmor, 1);
        }
        tech.resetMinutesLeft(false);
        campaign.fixPart(part, tech);
    }

    private static List<Part> partsNeedingWork(Unit unit) {
        return unit.getParts().stream().filter(Part::needsFixing).toList();
    }

    /** The Longbow from the report, as it was mid-repair: structure lost in the right torso and the right leg. */
    private static Entity damagedLongbow() {
        Entity longbow = UnitFixture.LONGBOW_LGB_8C.loadEntity();
        longbow.setInternal(longbow.getOInternal(Mek.LOC_RIGHT_TORSO) - RIGHT_TORSO_STRUCTURE_LOST,
              Mek.LOC_RIGHT_TORSO);
        longbow.setInternal(longbow.getOInternal(Mek.LOC_RIGHT_LEG) - RIGHT_LEG_STRUCTURE_LOST, Mek.LOC_RIGHT_LEG);
        return longbow;
    }

    private void repairEverything(Unit unit) {
        for (int day = 0; (day < MAXIMUM_REPAIR_DAYS) && !partsNeedingWork(unit).isEmpty(); day++) {
            for (Part part : partsNeedingWork(unit)) {
                repair(part);
            }
        }
        assertTrue(partsNeedingWork(unit).isEmpty(), "Every part is repaired, replaced or reloaded");
    }

    @Test
    void theLongbowFromTheReportShowsNoDamageOnceRepaired() {
        Unit longbow = campaign.addNewUnit(damagedLongbow(), false, 0, PartQuality.QUALITY_D);
        assertEquals(Entity.DMG_MODERATE, longbow.getDamageState(), "A leg with structure damage is moderate damage");

        repairEverything(longbow);

        assertEquals(Entity.DMG_NONE, longbow.getDamageState(), "A fully repaired Mek shows no damage");
    }

    @Test
    void aFullyRepairedSalvagedMekShowsNoDamage() {
        Unit locust = campaign.addNewUnit(damagedLocust(), false, 0, PartQuality.QUALITY_D);
        assertNotEquals(Entity.DMG_NONE, locust.getDamageState(), "The Locust comes in damaged");

        repairEverything(locust);

        assertEquals(Entity.DMG_NONE, locust.getDamageState(), "A fully repaired Mek shows no damage");
    }
}
