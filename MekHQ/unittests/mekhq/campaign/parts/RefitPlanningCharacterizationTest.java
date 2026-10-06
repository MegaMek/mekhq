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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Pins down what refit planning produces today for each canon refit pair in {@link UnitFixture#REFIT_PAIRS}: the
 * refit class, the time, the cost, the parts to buy, the parts removed and the armor to order. Each refit is built the
 * way the Choose Configuration dialog builds it, against a real campaign and warehouse.
 *
 * <p>These are characterization tests. They record current behaviour, including behaviour the refit audit found to
 * be wrong; such assertions are named after the audit finding and change when that finding is fixed. A kit refit
 * ({@code custom} is {@code false}) halves the time and adds 10 percent to the cost of the parts.</p>
 */
// getOldUnitParts is deprecated only because nothing but tests read it; reading it is the point here
@SuppressWarnings("deprecation")
class RefitPlanningCharacterizationTest {
    private static final boolean REFIT_KIT = false;
    private static final boolean CUSTOM_JOB = true;

    private static Refit planRefit(Unit unit, UnitFixture target, boolean isCustomJob) {
        return new Refit(unit, target.loadEntity(), isCustomJob, false, false);
    }

    private static Refit planRefit(UnitFixture.RefitPair refitPair, boolean isCustomJob) {
        PartsScenario scenario = PartsScenario.create();
        Unit unit = scenario.withUnit(refitPair.original());
        return planRefit(unit, refitPair.target(), isCustomJob);
    }

    private static UnitFixture.RefitPair refitPairStartingWith(UnitFixture original) {
        for (UnitFixture.RefitPair refitPair : UnitFixture.REFIT_PAIRS) {
            if (refitPair.original() == original) {
                return refitPair;
            }
        }
        throw new IllegalArgumentException("No refit pair starts with " + original);
    }

    @Test
    void locustLct1vToLct1eKit() {
        // No worked rulebook figure for this pair. Class D because two lasers go into each arm where one Machine Gun
        // was (StratOps p.188 kit rule, as RefitTest cites). Time is seven 120-minute items at the halved Class D rate.
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.LOCUST_LCT_1V), REFIT_KIT);

        assertEquals(Refit.CLASS_D, refit.getRefitClass());
        assertEquals(3360, refit.getTime());
        assertEquals(Money.of(62500).multipliedBy(1.1), refit.getCost());
        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("Machine Gun", 2, "Machine Gun Ammo [Full] Bin", 1),
              PartsCensus.ofParts(refit.getOldUnitParts()));
        assertTrue(refit.isSameArmorType());
        assertNull(refit.getNewArmorSupplies());
    }

    @Test
    void locustLct1vToLct1eCustom() {
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.LOCUST_LCT_1V), CUSTOM_JOB);

        assertEquals(Refit.CLASS_D, refit.getRefitClass());
        assertEquals(6720, refit.getTime());
        assertEquals(Money.of(62500), refit.getCost());
        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("Machine Gun", 2, "Machine Gun Ammo [Full] Bin", 1),
              PartsCensus.ofParts(refit.getOldUnitParts()));
        assertNull(refit.getNewArmorSupplies());
    }

    @Test
    void wolverineWvr6rToWvr6mKitIsClassDSeeGen1() {
        // Campaign Operations p.211-212 works this refit through: Class C, 1,100 item minutes. MekHQ gives Class D
        // today and multiplies by the Class D rate (8, halved for a kit); current behaviour, see GEN-1; changes when
        // that is fixed.
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.WOLVERINE_WVR_6R), REFIT_KIT);

        assertEquals(Refit.CLASS_D, refit.getRefitClass(), "GEN-1: book class is C");
        assertEquals(4400, refit.getTime(), "GEN-1: 1,100 item minutes at the Class D kit rate");
        assertEquals(Money.of(154000).multipliedBy(1.1), refit.getCost());
        assertEquals(Map.of("Heat Sink", 2, "Large Laser", 1, "Medium Laser", 1),
              PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("AC/5", 1, "AC/5 Ammo Bin", 1, "Armor (IS Standard)", 92),
              PartsCensus.ofParts(refit.getOldUnitParts()));
        assertWolverineArmorOrder(refit);
    }

    @Test
    void wolverineWvr6rToWvr6mCustomIsClassDSeeGen1() {
        // Campaign Operations p.211-212: Class C, 1,100 item minutes. The audit found Class D and 8,800 minutes for
        // the custom refit; current behaviour, see GEN-1; changes when that is fixed.
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.WOLVERINE_WVR_6R), CUSTOM_JOB);

        assertEquals(Refit.CLASS_D, refit.getRefitClass(), "GEN-1: book class is C");
        assertEquals(8800, refit.getTime(), "GEN-1: 1,100 item minutes at the Class D rate");
        assertEquals(Money.of(154000), refit.getCost());
        assertEquals(Map.of("Heat Sink", 2, "Large Laser", 1, "Medium Laser", 1),
              PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("AC/5", 1, "AC/5 Ammo Bin", 1, "Armor (IS Standard)", 92),
              PartsCensus.ofParts(refit.getOldUnitParts()));
        assertWolverineArmorOrder(refit);
    }

    /**
     * The WVR-6M carries 16 more points of the same standard armor, so the refit orders that much and nothing else.
     */
    private static void assertWolverineArmorOrder(Refit refit) {
        assertTrue(refit.isSameArmorType());
        Armor armorSupplies = refit.getNewArmorSupplies();
        assertNotNull(armorSupplies);
        assertEquals("Armor (IS Standard)", armorSupplies.getName());
        assertEquals(0, armorSupplies.getAmount());
        assertEquals(16, armorSupplies.getAmountNeeded());
    }

    @Test
    void hunchbackHbk4gToHbk4pKit() {
        // No rulebook figure for this pair. Six Medium Lasers and ten Heat Sinks replace the AC/20 and its ammo.
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.HUNCHBACK_HBK_4G), REFIT_KIT);

        assertEquals(Refit.CLASS_D, refit.getRefitClass());
        assertEquals(7920, refit.getTime());
        assertEquals(Money.of(260000).multipliedBy(1.1), refit.getCost());
        assertEquals(Map.of("Heat Sink", 10, "Medium Laser", 6), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("AC/20", 1, "AC/20 Ammo Bin", 2), PartsCensus.ofParts(refit.getOldUnitParts()));
        assertTrue(refit.isSameArmorType());
        assertNull(refit.getNewArmorSupplies());
    }

    @Test
    void hunchbackHbk4gToHbk4pCustom() {
        Refit refit = planRefit(refitPairStartingWith(UnitFixture.HUNCHBACK_HBK_4G), CUSTOM_JOB);

        assertEquals(Refit.CLASS_D, refit.getRefitClass());
        assertEquals(15840, refit.getTime());
        assertEquals(Money.of(260000), refit.getCost());
        assertEquals(Map.of("Heat Sink", 10, "Medium Laser", 6), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of("AC/20", 1, "AC/20 Ammo Bin", 2), PartsCensus.ofParts(refit.getOldUnitParts()));
        assertNull(refit.getNewArmorSupplies());
    }

    @Test
    void spareMediumLaserInTheWarehouseIsPlannedInsteadOfBought() {
        PartsScenario scenario = PartsScenario.create();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        EquipmentPart installedMediumLaser = mediumLaserOf(locust);
        Part spareMediumLaser = installedMediumLaser.clone();
        scenario.withSpare(spareMediumLaser, 1);

        Refit refit = planRefit(locust, UnitFixture.LOCUST_LCT_1E, CUSTOM_JOB);

        assertEquals(Map.of("Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Money.of(22500), refit.getCost());
        assertTrue(refit.getNewUnitParts().contains(spareMediumLaser),
              "The spare Medium Laser should be planned into the refitted unit");
        assertEquals(Refit.CLASS_D, refit.getRefitClass());
        assertEquals(6720, refit.getTime());
        // Planning alone reserves nothing: the spare is still free for other work until the refit begins
        assertEquals(Map.of("Medium Laser", 1), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
    }

    private static EquipmentPart mediumLaserOf(Unit unit) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("No Medium Laser on " + unit.getName());
    }
}
