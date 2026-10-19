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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.Mek;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingMekLocation;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Mek limb with a broken shoulder or hip cannot be repaired, only replaced. It is not missing, so the parts needed
 * list never named it, and the player had to look up its weight and side to buy one by hand (issue #3699). The
 * replacement limb is now listed.
 */
class PartsNeededBadHipOrShoulderTest {
    private static final int SHOULDER_SLOT = 0;
    private static final int HIP_SLOT = 0;

    private final PartsScenario scenario = PartsScenario.create();
    private final Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

    private void breakTheActuatorIn(int location, int slot) {
        wolverine.getEntity().getCritical(location, slot).setHit(true);
    }

    private List<MissingMekLocation> limbsNeeded() {
        return wolverine.getPartsNeeded().stream()
                     .filter(MissingMekLocation.class::isInstance)
                     .map(MissingMekLocation.class::cast)
                     .toList();
    }

    private MekLocation locationOf(int location) {
        return PartsScenario.unitParts(wolverine, MekLocation.class).stream()
                     .filter(part -> part.getLoc() == location)
                     .findFirst()
                     .orElseThrow();
    }

    @Test
    void aBrokenShoulderListsTheArmToBuy() {
        breakTheActuatorIn(Mek.LOC_RIGHT_ARM, SHOULDER_SLOT);

        List<MissingMekLocation> limbsNeeded = limbsNeeded();

        assertEquals(1, limbsNeeded.size());
        assertTrue(limbsNeeded.getFirst().isAcceptableReplacement(locationOf(Mek.LOC_RIGHT_ARM).clone(), false),
              "The listed limb is a right arm for a 55 ton Mek");
    }

    @Test
    void aBrokenHipListsTheLegToBuy() {
        breakTheActuatorIn(Mek.LOC_LEFT_LEG, HIP_SLOT);

        List<MissingMekLocation> limbsNeeded = limbsNeeded();

        assertEquals(1, limbsNeeded.size());
        assertTrue(limbsNeeded.getFirst().isAcceptableReplacement(locationOf(Mek.LOC_LEFT_LEG).clone(), false),
              "The listed limb is a left leg for a 55 ton Mek");
    }

    @Test
    void noLimbIsListedWhenAReplacementIsInStock() {
        scenario.withSpare(locationOf(Mek.LOC_RIGHT_ARM).clone(), 1);
        breakTheActuatorIn(Mek.LOC_RIGHT_ARM, SHOULDER_SLOT);

        assertEquals(List.of(), limbsNeeded());
    }

    @Test
    void anUndamagedMekNeedsNoLimbs() {
        assertEquals(List.of(), limbsNeeded());
    }
}
