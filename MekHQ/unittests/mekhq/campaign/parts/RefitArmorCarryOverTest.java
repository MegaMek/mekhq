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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * What a refit does with the armor around the changes: armor on a location the refit leaves alone keeps its damage, a
 * location the new design adds gets its armor part at once, and armor that comes with a refit kit is held for the refit
 * (issue #10205).
 */
class RefitArmorCarryOverTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static void complete(Refit refit) throws Exception {
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    @Test
    void armorOnAnUnchangedLocationKeepsItsDamage() throws Exception {
        // The WVR-6M keeps the WVR-6R's 20 points of right torso armor
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Entity entity = wolverine.getEntity();
        entity.setArmor(entity.getArmor(Mek.LOC_RIGHT_TORSO) - 10, Mek.LOC_RIGHT_TORSO);
        for (Part part : wolverine.getParts()) {
            part.updateConditionFromEntity(false);
        }
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), true, false, false);

        complete(refit);

        assertEquals(20, wolverine.getEntity().getOArmor(Mek.LOC_RIGHT_TORSO));
        assertEquals(10, wolverine.getEntity().getArmor(Mek.LOC_RIGHT_TORSO),
              "Finishing the refit does not repair armor the refit never touched");
    }

    @Test
    void aNewTurretGetsItsArmorAtOnce() throws Exception {
        Unit apc = scenario.withUnit(UnitFixture.APC_HOVER_LRM);
        Refit refit = new Refit(apc, UnitFixture.APC_HOVER_MG.loadEntity(), true, false, false);

        complete(refit);

        Unit freshApc = scenario.withUnit(UnitFixture.APC_HOVER_MG);
        assertEquals(PartsCensus.ofUnit(freshApc), PartsCensus.ofUnit(apc),
              "The refitted APC has the same parts as a factory-fresh one, turret armor included");
    }

    @Test
    void kitArmorIsHeldForTheRefit() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        refit.begin();

        refit.addRefitKitParts(3);

        Armor armorSupplies = refit.getNewArmorSupplies();
        assertNotNull(armorSupplies);
        assertEquals(0, armorSupplies.getAmountNeeded(), "The kit carries all the armor the refit needs");
        assertTrue(armorSupplies.isReservedForRefit(), "The kit's armor is held for the refit");
        assertTrue(refit.partsInTransit(), "The refit waits for the kit to arrive");
        for (Part part : scenario.getWarehouse().getParts()) {
            boolean isFreeSpareArmor = (part instanceof Armor) && (part.getUnit() == null)
                  && !part.isReservedForRefit();
            assertFalse(isFreeSpareArmor, "No kit armor is left where a repair could take it: " + part.getName());
        }
    }
}
