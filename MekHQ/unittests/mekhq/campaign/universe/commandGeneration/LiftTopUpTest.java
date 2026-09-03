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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
 * Covers how the lift top-up reads the hangar: only the units the build just created want lift, and everything
 * already there - ships and the units they will carry - sets what is free.
 */
class LiftTopUpTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void onlyTheNewUnitsWantLift() {
        Dropship leopard = new Dropship();
        leopard.addTransporter(new ASFBay(2, 1, 1));
        AeroSpaceFighter oldFighter = new AeroSpaceFighter();
        AeroSpaceFighter newFighter = new AeroSpaceFighter();
        Unit newUnit = unitOf(newFighter);

        LiftTopUp.Hangar hangar = LiftTopUp.Hangar.of(
              List.of(unitOf(leopard), unitOf(oldFighter), newUnit, unitOf(null)), Set.of(newUnit.getId()));

        assertEquals(List.of(newFighter), hangar.wantingLift());
        assertEquals(List.of(leopard, oldFighter), hangar.alreadyThere());
    }

    @Test
    void aNewShipOffersLiftRatherThanWantingIt() {
        Dropship leopard = new Dropship();
        leopard.addTransporter(new ASFBay(2, 1, 1));
        Unit newShip = unitOf(leopard);

        LiftTopUp.Hangar hangar = LiftTopUp.Hangar.of(List.of(newShip), Set.of(newShip.getId()));

        assertTrue(hangar.wantingLift().isEmpty());
        assertEquals(List.of(leopard), hangar.alreadyThere());
    }

    @Test
    void whatIsAlreadyThereComesOffTheFreeLiftFirst() {
        Dropship leopard = new Dropship();
        leopard.addTransporter(new ASFBay(2, 1, 1));
        Unit newUnit = unitOf(new AeroSpaceFighter());
        LiftTopUp.Hangar hangar = LiftTopUp.Hangar.of(
              List.of(unitOf(leopard), unitOf(new AeroSpaceFighter()), newUnit), Set.of(newUnit.getId()));

        ExistingLift owned = ExistingLift.of(hangar.alreadyThere());

        assertEquals(1, owned.freeBays().get(UnitType.AEROSPACE_FIGHTER),
              "the fighter already in the hangar holds one of the two bays before the new one is considered");
    }

    private static Unit unitOf(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }
}
