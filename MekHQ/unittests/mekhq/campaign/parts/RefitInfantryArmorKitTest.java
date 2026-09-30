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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.ConvInfantry;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingInfantryArmorPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A conventional platoon's armor parts are its armor kit: a refit that swaps one kit for another sees the change and
 * pays MegaMek's price for the new kit, even when the two kits have the same special properties (issue #10207).
 */
class RefitInfantryArmorKitTest {
    private static final String OTHER_PLAIN_KIT = "Free Rasalhague Republic Infantry Kit";

    private final PartsScenario scenario = PartsScenario.create();

    private static long countOf(Refit refit, Class<? extends Part> partType) {
        long count = 0;
        for (Part part : refit.getShoppingList()) {
            if (partType.isInstance(part)) {
                count++;
            }
        }
        return count;
    }

    private static InfantryArmorPart anArmorPartOf(Unit unit) {
        return PartsScenario.unitParts(unit, InfantryArmorPart.class).getFirst();
    }

    @Test
    void aPlatoonsArmorPartIsItsKitAtMegaMeksPrice() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        EquipmentType kit = ((ConvInfantry) platoon.getEntity()).getArmorKit();
        assertNotNull(kit, "The DCMS platoon wears an armor kit");

        InfantryArmorPart armorPart = anArmorPartOf(platoon);

        assertEquals(kit.getName(), armorPart.getName());
        assertEquals(Money.of(kit.getCost(platoon.getEntity(), false, ConvInfantry.LOC_INFANTRY)),
              armorPart.getStickerPrice(), "The kit costs what MegaMek says it costs");
    }

    @Test
    void swappingToAnotherKitWithTheSamePropertiesIsARefit() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        EquipmentType otherKit = EquipmentType.get(OTHER_PLAIN_KIT);
        assertNotNull(otherKit);
        ConvInfantry design = (ConvInfantry) UnitFixture.FOOT_PLATOON_LASER.loadEntity();
        design.setArmorKit(otherKit);

        Refit refit = new Refit(platoon, design, true, false, false);

        int troopers = platoon.getEntity().getOInternal(ConvInfantry.LOC_INFANTRY);
        assertEquals(troopers, countOf(refit, MissingInfantryArmorPart.class), "Every trooper gets the new kit");
        Money kitPrice = Money.of(otherKit.getCost(design, false, ConvInfantry.LOC_INFANTRY));
        assertEquals(kitPrice.multipliedBy(troopers), refit.getCost());
    }

    @Test
    void armorSavedWithoutItsKitTakesThePlatoonsKitOnLoad() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        EquipmentType kit = ((ConvInfantry) platoon.getEntity()).getArmorKit();
        InfantryArmorPart armorPart = anArmorPartOf(platoon);
        InfantryArmorPart savedWithoutKit = new InfantryArmorPart(0, scenario.getCampaign(), 1.0, false, false, false,
              false, false, false);
        platoon.removePart(armorPart);
        platoon.addPart(savedWithoutKit);

        platoon.initializeParts(true);

        assertTrue(InfantryArmorPart.isSameKit(kit, savedWithoutKit.getArmorKit()));
    }

    @Test
    void anInfraredSneakSuitStaysInfrared() {
        InfantryArmorPart infraredSuit = new InfantryArmorPart(0, scenario.getCampaign(), 1.0, false, false, false,
              true, false, false);

        InfantryArmorPart copy = (InfantryArmorPart) infraredSuit.clone();
        MissingPart missingSuit = infraredSuit.getMissingPart();

        assertTrue(copy.isSneakIR() && !copy.isSneakECM(), "A copy of an IR sneak suit is an IR sneak suit");
        assertTrue(missingSuit.isAcceptableReplacement(copy, false), "A spare IR suit replaces a missing IR suit");
    }
}
