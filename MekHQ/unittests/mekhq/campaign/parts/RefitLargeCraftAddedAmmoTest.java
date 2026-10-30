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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Adding LRM-10 ammunition to a Leopard CV in the Mek Lab stopped with an error, and the ammunition never appeared
 * (issue #5555). The player had swapped the ship's LRM-20 launchers for LRM-10s. The refit took the new LRM-10
 * ammunition for one of the old LRM-20 bins being moved, and asked large craft ammunition for an install time it does
 * not have. Issue #3698 fixed that crash for the Union; these pin it for this ship.
 */
class RefitLargeCraftAddedAmmoTest {
    private static final String LRM_10_AMMO = "IS Ammo LRM-10";

    /** The Leopard CV as the player had it: LRM-10 launchers in the LRM bays and no LRM ammunition yet. */
    private static Entity leopardWithLrm10Launchers() throws EntityLoadingException {
        String unitFile = UnitFixture.LEOPARD_CV_DROPSHIP.readUnitFile();
        String editedFile = unitFile.replaceAll("IS Ammo LRM-20:12\\R", "").replace("LRM 20", "LRM 10");
        return new MekFileParser(editedFile).getEntity();
    }

    /** Adds ammunition the way the Mek Lab's equipment list does: unallocated, not yet in any bay. */
    private static Entity withAddedLrm10Ammo(Entity ship) throws LocationFullException {
        Mounted<?> ammo = Mounted.createMounted(ship, EquipmentType.get(LRM_10_AMMO));
        ammo.setShotsLeft(((AmmoType) ammo.getType()).getShots());
        ship.addEquipment(ammo, Entity.LOC_NONE, false);
        return ship;
    }

    private static boolean isLrm10Bin(Part part) {
        return (part instanceof LargeCraftAmmoBin bin) && LRM_10_AMMO.equals(bin.getType().getInternalName());
    }

    @Test
    void addingAmmunitionAfterSwappingLaunchersCanBePlanned() throws Exception {
        Unit leopard = PartsScenario.create().withUnit(UnitFixture.LEOPARD_CV_DROPSHIP);
        Entity edited = withAddedLrm10Ammo(leopardWithLrm10Launchers());

        assertDoesNotThrow(() -> new Refit(leopard, edited, true, false, true));
    }

    @Test
    void addingAmmunitionToTheStockShipCanBePlanned() throws Exception {
        Unit leopard = PartsScenario.create().withUnit(UnitFixture.LEOPARD_CV_DROPSHIP);
        Entity edited = withAddedLrm10Ammo(UnitFixture.LEOPARD_CV_DROPSHIP.loadEntity());

        assertDoesNotThrow(() -> new Refit(leopard, edited, true, false, true));
    }

    @Test
    void addedAmmunitionIsBought() throws Exception {
        Unit leopard = PartsScenario.create().withUnit(UnitFixture.LEOPARD_CV_DROPSHIP);

        Refit refit = new Refit(leopard, withAddedLrm10Ammo(leopardWithLrm10Launchers()), true, false, true);

        assertTrue(refit.getShoppingList().stream().anyMatch(RefitLargeCraftAddedAmmoTest::isLrm10Bin),
              "the LRM-10 bin must be bought");
    }
}
