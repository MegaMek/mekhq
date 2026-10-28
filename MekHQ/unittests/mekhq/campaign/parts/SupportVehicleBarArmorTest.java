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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.enums.TechRating;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Support vehicles with BAR armor get support vehicle armor parts, which weigh and price the armor by its BAR and
 * armor tech rating, rather than plain armor at 16 points per ton (issue #5571). The Cellco Ranger UPU-3000 carries
 * BAR 8 armor at tech rating D.
 */
class SupportVehicleBarArmorTest {
    private static final int RANGER_BAR = 8;

    private final PartsScenario scenario = PartsScenario.create();

    private static double weightPerPoint(int bar, TechRating techRating) {
        return ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
    }

    @Test
    void supportVehicleArmorIsWeighedByItsBar() {
        Unit ranger = scenario.withUnit(UnitFixture.CELLCO_RANGER_UPU_3000);

        List<Armor> armorParts = PartsScenario.unitParts(ranger, Armor.class);
        assertFalse(armorParts.isEmpty());
        for (Armor armor : armorParts) {
            SVArmor supportVehicleArmor = assertInstanceOf(SVArmor.class, armor, armor.getName());
            assertEquals(RANGER_BAR, supportVehicleArmor.getBAR());
            assertEquals(TechRating.D, supportVehicleArmor.getTechRating());
            assertEquals(armor.getAmount() * weightPerPoint(RANGER_BAR, TechRating.D), armor.getTonnage(), 0.0001);
        }
    }

    @Test
    void addedArmorIsBoughtAsSupportVehicleArmor() {
        Unit ranger = scenario.withUnit(UnitFixture.CELLCO_RANGER_UPU_3000);
        Entity design = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        int frontLocation = design.firstArmorIndex();
        design.initializeArmor(design.getOArmor(frontLocation) + 10, frontLocation);

        Refit refit = new Refit(ranger, design, true, false, false);

        SVArmor armorSupplies = assertInstanceOf(SVArmor.class, refit.getNewArmorSupplies());
        assertEquals(RANGER_BAR, armorSupplies.getBAR());
        assertEquals(10, armorSupplies.getAmountNeeded());
        assertEquals(10 * weightPerPoint(RANGER_BAR, TechRating.D), armorSupplies.getTonnageNeeded(), 0.0001,
              "Ten points of BAR 8 armor weigh far less than the 0.625 tons plain armor would");
    }

    @Test
    void changingTheArmorTechRatingIsNotTheSameArmor() {
        Entity oldDesign = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        Entity sameDesign = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        Entity newDesign = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        newDesign.setArmorTechRating(TechRating.E);

        assertTrue(Refit.hasSameArmor(oldDesign, sameDesign));
        assertFalse(Refit.hasSameArmor(oldDesign, newDesign), "BAR 8 at D and BAR 8 at E weigh differently");
    }

    @Test
    void plainArmorFromAnOlderSaveIsReplacedWithSupportVehicleArmor() {
        Unit ranger = scenario.withUnit(UnitFixture.CELLCO_RANGER_UPU_3000);
        Entity entity = ranger.getEntity();
        SVArmor frontArmor = PartsScenario.unitParts(ranger, SVArmor.class).getFirst();
        int location = frontArmor.getLocation();
        ranger.removePart(frontArmor);
        scenario.getWarehouse().removePart(frontArmor);
        // Older versions gave a support vehicle a plain armor part with the BAR armor type
        Armor oldArmor = new Armor(0, entity.getArmorType(location), entity.getOArmor(location), location, false,
              false, scenario.getCampaign());
        ranger.addPart(oldArmor);
        scenario.getCampaign().getQuartermaster().addPart(oldArmor, 0, false);

        ranger.initializeParts(true);

        Armor locationArmor = PartsScenario.unitParts(ranger, Armor.class)
                                    .stream()
                                    .filter(armor -> armor.getLocation() == location)
                                    .findFirst()
                                    .orElse(null);
        assertNotNull(locationArmor);
        assertInstanceOf(SVArmor.class, locationArmor);
        assertFalse(scenario.getWarehouse().getParts().contains(oldArmor), "The plain armor part is gone");
    }

    @Test
    void boughtSupportVehicleArmorIsFiveTonsAtItsPrice() {
        SVArmor armor = new SVArmor(RANGER_BAR, TechRating.D, 0, Entity.LOC_NONE, scenario.getCampaign());
        int fiveTonsOfPoints = (int) Math.round(5 / weightPerPoint(RANGER_BAR, TechRating.D));

        SVArmor order = assertInstanceOf(SVArmor.class, armor.getAcquisitionWork());

        assertEquals(fiveTonsOfPoints, order.getAmount(), "An order is five tons of armor, as for other armor");
        assertTrue(armor.getStickerPrice().isPositive(), "Support vehicle armor is not free");
    }

    @Test
    void standardArmorOnAnArmoredChassisIsNotBarArmor() {
        Entity design = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        design.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        design.setBARRating(10);

        assertFalse(SVArmor.usesBarArmor(design, design.firstArmorIndex()),
              "Standard armor at BAR 10 is military armor, weighed by points per ton");
    }

    @Test
    void standardArmorLoadedWithoutABarIsNotBarArmor() {
        Entity design = UnitFixture.CELLCO_RANGER_UPU_3000.loadEntity();
        design.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        // A unit file with standard armor has no BAR rating, which leaves a support vehicle's BAR at 0
        design.setBARRating(0);

        assertFalse(SVArmor.usesBarArmor(design, design.firstArmorIndex()));
    }

    @Test
    void standardArmorOnACombatVehicleIsNotBarArmor() {
        Entity tank = UnitFixture.VEDETTE_MEDIUM_TANK.loadEntity();
        tank.setArmorType(EquipmentType.T_ARMOR_STANDARD);

        assertFalse(SVArmor.usesBarArmor(tank, tank.firstArmorIndex()));
    }

    @Test
    void armorThatDoesNotExistAtItsTechRatingOrdersNoPoints() {
        // BAR 10 armor only exists from tech rating D
        SVArmor armor = new SVArmor(10, TechRating.C, 0, Entity.LOC_NONE, scenario.getCampaign());

        assertEquals(0.0, armor.getArmorPointsPerTon());
        assertEquals(0, ((Armor) armor.getNewPart()).getAmount(),
              "An empty order rather than -1 points, which kept a refit ordering armor forever");
        assertEquals(0, ((SVArmor) armor.getAcquisitionWork()).getAmount());
    }

    @Test
    void armorThatExistsAtItsTechRatingHasPointsPerTon() {
        SVArmor armor = new SVArmor(10, TechRating.E, 0, Entity.LOC_NONE, scenario.getCampaign());

        assertEquals(1.0 / weightPerPoint(10, TechRating.E), armor.getArmorPointsPerTon(), 0.0001);
        assertTrue(((Armor) armor.getNewPart()).getAmount() > 0);
    }
}
