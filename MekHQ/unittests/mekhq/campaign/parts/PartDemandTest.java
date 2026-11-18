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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Stripping a unit is often cannibalising it to repair another, so a salvage task names the other units that are
 * missing the part it would take.
 */
class PartDemandTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static String plainText(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    private static List<EquipmentPart> equipmentNamed(Unit unit, String name) {
        return PartsScenario.unitParts(unit, EquipmentPart.class)
                     .stream()
                     .filter(equipmentPart -> name.equals(equipmentPart.getName()))
                     .toList();
    }

    private static EquipmentPart mediumLaser(Unit unit) {
        return equipmentNamed(unit, "Medium Laser").getFirst();
    }

    private Unit beingStripped(UnitFixture fixture) {
        Unit unit = scenario.withUnit(fixture);
        unit.setSalvage(true);
        return unit;
    }

    private Unit missingItsMediumLaser(UnitFixture fixture) {
        Unit unit = scenario.withUnit(fixture);
        mediumLaser(unit).remove(false);
        return unit;
    }

    @Test
    void aUnitMissingThePartIsCountedAndNamed() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);

        PartDemand demand = mediumLaser(stripped).countDemand();

        assertEquals("Needed by 1 unit", demand.describe());
        assertEquals("Wolverine WVR-6R needs one", demand.describeUnits());
    }

    @Test
    void noUnitNeedsAPartNoOneIsMissing() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

        PartDemand demand = mediumLaser(stripped).countDemand();

        assertFalse(demand.isNeeded());
        assertEquals("", demand.describe());
        assertNull(demand.describeUnits());
    }

    @Test
    void theTaskNamesNoUnitsUntilTheirDemandIsSet() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        EquipmentPart laser = mediumLaser(stripped);

        assertFalse(plainText(laser.getDesc()).contains("Needed by"), laser.getDesc());

        laser.setTaskDemand(laser.countDemand());

        String task = laser.getDesc();
        assertTrue(task.contains("<br>Needed by 1 unit<br>"), task);
    }

    @Test
    void gapsGatheredOnceServeEveryPart() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        Unit damaged = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        equipmentNamed(damaged, "Machine Gun").getFirst().remove(false);
        List<MissingPart> gaps = PartDemand.findGaps(scenario.getCampaign().getPlayerForce().getHangar().getUnits());

        assertEquals(2, gaps.size());
        assertEquals("Wolverine WVR-6R needs one", mediumLaser(stripped).countDemand(gaps).describeUnits());
        assertEquals("Locust LCT-1V #2 needs one",
              equipmentNamed(stripped, "Machine Gun").getFirst().countDemand(gaps).describeUnits());
    }

    @Test
    void aUnitMissingSeveralSaysHowMany() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        Unit damaged = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        for (EquipmentPart machineGun : equipmentNamed(damaged, "Machine Gun")) {
            machineGun.remove(false);
        }

        PartDemand demand = equipmentNamed(stripped, "Machine Gun").getFirst().countDemand();

        // The campaign numbers a second unit of the same model
        assertEquals("Needed by 1 unit", demand.describe());
        assertEquals("Locust LCT-1V #2 needs 2", demand.describeUnits());
    }

    @Test
    void eachUnitHasItsOwnTooltipLine() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        missingItsMediumLaser(UnitFixture.HUNCHBACK_HBK_4G);

        PartDemand demand = mediumLaser(stripped).countDemand();

        assertEquals("Needed by 2 units", demand.describe());
        assertEquals("Hunchback HBK-4G needs one<br>Wolverine WVR-6R needs one", demand.describeUnits());
    }

    @Test
    void moreThanTwoUnitsAreCounted() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        missingItsMediumLaser(UnitFixture.HUNCHBACK_HBK_4G);
        missingItsMediumLaser(UnitFixture.LOCUST_LCT_1V);

        PartDemand demand = mediumLaser(stripped).countDemand();

        assertEquals("Needed by 3 units", demand.describe());
    }

    @Test
    void theUnitBeingStrippedDoesNotNeedItsOwnParts() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        List<EquipmentPart> machineGuns = equipmentNamed(stripped, "Machine Gun");
        machineGuns.get(0).remove(false);

        assertFalse(machineGuns.get(1).countDemand().isNeeded());
    }

    @Test
    void anotherUnitBeingStrippedNeedsNothing() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        Unit alsoStripped = missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        alsoStripped.setSalvage(true);

        assertFalse(mediumLaser(stripped).countDemand().isNeeded());
    }

    @Test
    void aMothballedUnitNeedsNothing() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        Unit mothballed = missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);
        mothballed.setMothballed(true);

        assertFalse(mediumLaser(stripped).countDemand().isNeeded());
    }

    @Test
    void aDifferentPartIsNotNeeded() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        missingItsMediumLaser(UnitFixture.WOLVERINE_WVR_6R);

        assertFalse(equipmentNamed(stripped, "Machine Gun").getFirst().countDemand().isNeeded());
    }

    @Test
    void ammoBinsAreNeverNeeded() {
        Unit stripped = beingStripped(UnitFixture.LOCUST_LCT_1V);
        Unit damaged = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        PartsScenario.unitParts(damaged, AmmoBin.class).getFirst().remove(false);

        AmmoBin strippedBin = PartsScenario.unitParts(stripped, AmmoBin.class).getFirst();

        assertFalse(strippedBin.countDemand().isNeeded());
    }
}
