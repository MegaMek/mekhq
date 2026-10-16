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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * An OmniFighter's pod-mounted ammunition sits in the fuselage, which has no location of its own. When every pod there
 * was lost, checking the fuselage pod space asked the fighter about that missing location, and the Repair tab and Mass
 * Repair stopped with an error on every redraw (issue #7049).
 */
class FighterFuselagePodSpaceTest {
    private Unit turk;

    @BeforeEach
    void setUp() {
        turk = PartsScenario.create().withUnit(UnitFixture.TURK_A);
    }

    private PodSpace fuselagePodSpace() {
        PodSpace fuselage = turk.getPodSpace()
                                  .stream()
                                  .filter(podSpace -> podSpace.getLocation() == Entity.LOC_NONE)
                                  .findFirst()
                                  .orElseThrow();
        fuselage.updateConditionFromEntity(false);
        return fuselage;
    }

    private List<AmmoBin> podAmmunition() {
        return PartsScenario.unitParts(turk, AmmoBin.class);
    }

    @Test
    void aFuselageWithEveryPodLostCanBeChecked() {
        for (AmmoBin ammunitionBin : podAmmunition()) {
            ammunitionBin.remove(false);
        }
        PodSpace fuselage = fuselagePodSpace();
        assertTrue(fuselage.needsFixing(), "The lost ammunition pods need replacing");

        assertDoesNotThrow(fuselage::checkFixable);
    }

    @Test
    void aFuselageWithOnePodLeftCanBeChecked() {
        List<AmmoBin> ammunition = podAmmunition();
        assertTrue(ammunition.size() > 1, "The Turk A carries several pod-mounted ammunition bins");
        ammunition.getFirst().remove(false);
        PodSpace fuselage = fuselagePodSpace();
        assertTrue(fuselage.needsFixing(), "The lost ammunition pod needs replacing");

        assertDoesNotThrow(fuselage::checkFixable);
    }

    @Test
    void aLocationTheFighterDoesNotHaveIsNeitherBreachedNorDestroyed() {
        int pastTheLastLocation = turk.getEntity().locations();

        assertFalse(turk.isLocationBreached(Entity.LOC_NONE));
        assertFalse(turk.isLocationBreached(pastTheLastLocation));
        assertFalse(turk.isLocationDestroyed(pastTheLastLocation));
    }
}
