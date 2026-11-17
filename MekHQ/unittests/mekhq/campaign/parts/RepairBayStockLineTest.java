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

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Stripping a unit counted spares still on their way as in stock, while repairing the same part said none were in
 * stock and listed them as in transit (#11071). Every task now reads its stock the same way.
 */
class RepairBayStockLineTest {
    private static final int DAYS_TO_ARRIVE = 5;

    private final PartsScenario scenario = PartsScenario.create();
    private final Campaign campaign = scenario.getCampaign();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static String plainText(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    private static EquipmentPart mediumLaser(Unit unit) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no Medium Laser");
    }

    private Unit locustBeingStripped() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        locust.setSalvage(true);
        return locust;
    }

    private void mediumLasersOnTheirWay(Unit source, int quantity) {
        Part lasers = mediumLaser(source).clone();
        lasers.setCampaign(campaign);
        lasers.setQuantity(quantity);
        campaign.getQuartermaster().addPart(lasers, DAYS_TO_ARRIVE, false);
    }

    @Test
    void strippingAUnitCountsPartsOnTheirWayAsInTransitNotInStock() {
        Unit locust = locustBeingStripped();
        mediumLasersOnTheirWay(locust, 8);

        assertEquals("None in stock (8 arriving in 5 days)", plainText(mediumLaser(locust).countStock().describe()));
    }

    @Test
    void repairingAndStrippingShowTheSameStockLine() {
        Unit stripped = locustBeingStripped();
        Unit damaged = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        mediumLaser(damaged).remove(false);
        MissingPart missingLaser = PartsScenario.unitParts(damaged, MissingPart.class).getFirst();
        mediumLasersOnTheirWay(stripped, 8);

        assertEquals(plainText(missingLaser.countStock().describe()),
              plainText(mediumLaser(stripped).countStock().describe()));
    }

    @Test
    void partsThatHaveArrivedAreInStock() {
        Unit locust = locustBeingStripped();
        scenario.withSpare(mediumLaser(locust).clone(), 8);

        assertEquals("8 in stock", plainText(mediumLaser(locust).countStock().describe()));
    }

    @Test
    void strippedAmmunitionShowsTheShotsOnTheirWay() {
        Unit locust = locustBeingStripped();
        AmmoBin machineGunAmmo = PartsScenario.unitParts(locust, AmmoBin.class).getFirst();
        campaign.getQuartermaster()
              .addPart(new AmmoStorage(0, machineGunAmmo.getType(), 100, campaign), DAYS_TO_ARRIVE, false);

        assertEquals("None in stock (100 shots arriving in 5 days)",
              plainText(machineGunAmmo.countStock().describe()));
    }

    @Test
    void strippedArmorShowsItsStockOnce() {
        Unit locust = locustBeingStripped();
        Armor armor = PartsScenario.unitParts(locust, Armor.class).getFirst();

        String task = plainText(armor.getDesc());

        assertEquals(1, task.split("in stock", -1).length - 1, task);
    }

    @Test
    void aStrippingTaskHasNoBlankLine() {
        Unit locust = locustBeingStripped();
        Armor armor = PartsScenario.unitParts(locust, Armor.class).getFirst();

        String task = armor.getDesc();

        assertFalse(task.contains("<br><br>"), task);
    }

    @Test
    void aRepairTaskHasNoBlankLine() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        EquipmentPart laser = mediumLaser(locust);
        laser.setHits(1);

        String task = laser.getDesc();

        assertFalse(task.contains("<br><br>"), task);
    }
}
