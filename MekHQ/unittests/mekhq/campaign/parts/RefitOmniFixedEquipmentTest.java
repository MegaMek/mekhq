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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.Utilities;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * An omni refit that changes equipment fixed to the base chassis is not a pod swap (issues #5748 and #4148). The check
 * compared the old design with itself, so it always passed: a Fire Moth refitted to single heat sinks, or an Epona
 * given a fixed laser, was treated as a pod swap and its fixed parts were skipped. Altering fixed components is a
 * Class F refit worked like any other, with every part compared (Campaign Operations p.212). Canon pod swaps that
 * differ only in one-shot launchers' ammunition or in the munitions loaded in fixed bins stay pod swaps.
 *
 * <p>The Fire Moth Prime, Batu E and Heimdall Prime and B are pinned copies from mm-data. The Fire Moth SHS (single
 * heat sinks) and the Epona with a fixed ER small laser are test designs built from the Prime files.</p>
 */
class RefitOmniFixedEquipmentTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static Entity load(String unitName, boolean isBlk) {
        Entity entity = MHQTestUtilities.getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, unitName);
        return entity;
    }

    @Test
    void changingAFireMothsFixedHeatSinksIsNotAPodSwap() {
        Entity prime = load("Dasher (Fire Moth) Prime", false);
        Entity singleHeatSinks = load("Dasher (Fire Moth) SHS", false);

        assertFalse(Utilities.isOmniVariant(prime, singleHeatSinks));
    }

    @Test
    void anOmniRefitThatChangesFixedEquipmentIsClassF() {
        Unit epona = scenario.getCampaign()
                           .addNewUnit(load("Epona Pursuit Tank Prime", true), false, 0, PartQuality.QUALITY_D);

        Refit refit = new Refit(epona, load("Epona Pursuit Tank Fixed Laser", true), true, false, false);

        assertNull(refit.checkFixable(), "Altering fixed components is allowed as a Class F refit");
        assertEquals(Refit.CLASS_F, refit.getRefitClass());
        boolean isBuyingTheLaser = refit.getShoppingList()
                                         .stream()
                                         .anyMatch(part -> part.getName().contains("Small Laser"));
        assertTrue(isBuyingTheLaser, "The fixed laser is bought like any new component");
    }

    @Test
    void aPodSwapIsQuickerThanAnyClass() {
        Unit epona = scenario.getCampaign()
                           .addNewUnit(load("Epona Pursuit Tank Prime", true), false, 0, PartQuality.QUALITY_D);

        Refit refit = new Refit(epona, load("Epona Pursuit Tank A", true), false, false, false);

        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass());
    }

    @Test
    void aCanonPodSwapIsStillAllowed() {
        Unit epona = scenario.getCampaign()
                           .addNewUnit(load("Epona Pursuit Tank Prime", true), false, 0, PartQuality.QUALITY_D);

        Refit refit = new Refit(epona, load("Epona Pursuit Tank A", true), false, false, false);

        assertNull(refit.checkFixable());
    }

    @Test
    void aPodSwapSaysWhatEachLocationsPodsBecome() {
        Unit batu = scenario.getCampaign().addNewUnit(load("Batu Prime", true), false, 0, PartQuality.QUALITY_D);

        Refit refit = new Refit(batu, load("Batu E", true), false, false, false);

        String reasons = String.join("; ", refit.getRefitClassReasons());
        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass(), reasons);
        assertTrue(reasons.contains(" becomes "), reasons);
        assertFalse(reasons.contains("Swapping"), "No line for each single pod: " + reasons);
    }

    @Test
    void oneShotLauncherAmmunitionDoesNotCountAsFixed() {
        // The Batu E's improved one-shot Streak launchers carry ammunition MegaMek adds with no location
        assertTrue(Utilities.isOmniVariant(load("Batu Prime", true), load("Batu E", true)));
    }

    @Test
    void aDifferentMunitionInAFixedBinIsNotAChange() {
        // Both carry three tons of fixed LB 10-X ammunition, loaded with a different mix of slug and cluster rounds
        assertTrue(Utilities.isOmniVariant(load("Heimdall Ground Monitor Tank Prime", true),
              load("Heimdall Ground Monitor Tank B", true)));
    }
}
