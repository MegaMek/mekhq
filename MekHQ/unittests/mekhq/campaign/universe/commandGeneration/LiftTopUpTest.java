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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.client.ratgenerator.ExistingLift;
import megamek.common.bays.ASFBay;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers how the lift top-up reads the hangar: ships offer lift, everything else wants it, and a ship's bays are
 * offered whole even while the units they will carry sit beside them in the hangar.
 */
class LiftTopUpTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void shipsOfferLiftAndEverythingElseWantsIt() {
        Dropship leopard = new Dropship();
        leopard.addTransporter(new ASFBay(2, 1, 1));
        AeroSpaceFighter fighter = new AeroSpaceFighter();

        LiftTopUp.Hangar hangar = LiftTopUp.Hangar.of(List.of(unitOf(leopard), unitOf(fighter), unitOf(null)));

        assertEquals(List.of(leopard), hangar.ships());
        assertEquals(List.of(fighter), hangar.units());
    }

    @Test
    void theShipsBaysAreOfferedWholeSoTheUnitsBesideThemClaimThemFirst() {
        Dropship leopard = new Dropship();
        leopard.addTransporter(new ASFBay(2, 1, 1));
        LiftTopUp.Hangar hangar = LiftTopUp.Hangar.of(List.of(unitOf(leopard), unitOf(new AeroSpaceFighter())));

        ExistingLift owned = ExistingLift.of(hangar.ships());

        assertEquals(2, owned.freeBays().get(UnitType.AEROSPACE_FIGHTER),
              "the fighter beside the ship is demand for the calculator, not a deduction here");
    }

    private static Unit unitOf(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }
}
