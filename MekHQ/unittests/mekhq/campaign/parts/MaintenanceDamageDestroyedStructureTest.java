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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.IArmorState;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A failed maintenance check damages a location's structure, but never below 1 point. A location already destroyed
 * used to be raised to 1 point by that rule, which made a wrecked unit repairable again (issue #5316). It now stays
 * destroyed.
 */
class MaintenanceDamageDestroyedStructureTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static MekLocation mekLocationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, MekLocation.class).stream()
                     .filter(part -> part.getLoc() == location)
                     .findFirst()
                     .orElseThrow();
    }

    private static TankLocation vehicleLocationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, TankLocation.class).stream()
                     .filter(part -> part.getLocation() == location)
                     .findFirst()
                     .orElseThrow();
    }

    private static ProtoMekLocation protoMekLocationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, ProtoMekLocation.class).stream()
                     .filter(part -> part.getLocation() == location)
                     .findFirst()
                     .orElseThrow();
    }

    @Test
    void aDestroyedMekLocationStaysDestroyed() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        MekLocation rightTorso = mekLocationOf(wolverine, Mek.LOC_RIGHT_TORSO);
        Entity entity = wolverine.getEntity();
        entity.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_RIGHT_TORSO);

        rightTorso.doMaintenanceDamage(1);

        assertTrue(entity.getInternal(Mek.LOC_RIGHT_TORSO) < 1, "The right torso is still destroyed");
    }

    @Test
    void maintenanceDamageStillStopsAtOnePointOnALocationThatIsIntact() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        MekLocation rightTorso = mekLocationOf(wolverine, Mek.LOC_RIGHT_TORSO);

        rightTorso.doMaintenanceDamage(100);

        assertEquals(1, wolverine.getEntity().getInternal(Mek.LOC_RIGHT_TORSO));
    }

    @Test
    void aDestroyedVehicleLocationStaysDestroyed() {
        Unit vedette = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        TankLocation front = vehicleLocationOf(vedette, Tank.LOC_FRONT);
        vedette.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, Tank.LOC_FRONT);

        front.doMaintenanceDamage(1);

        assertTrue(vedette.getEntity().getInternal(Tank.LOC_FRONT) < 1, "The front is still destroyed");
    }

    @Test
    void aDestroyedProtoMekLocationStaysDestroyed() {
        Unit minotaur = scenario.withUnit(UnitFixture.MINOTAUR_PROTOMEK);
        ProtoMekLocation leftArm = protoMekLocationOf(minotaur, ProtoMek.LOC_LEFT_ARM);
        minotaur.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, ProtoMek.LOC_LEFT_ARM);

        leftArm.doMaintenanceDamage(1);

        assertTrue(minotaur.getEntity().getInternal(ProtoMek.LOC_LEFT_ARM) < 1, "The left arm is still destroyed");
    }

    @Test
    void aFighterWithNoStructuralIntegrityLeftStaysDestroyed() {
        Unit shilone = scenario.withUnit(UnitFixture.SHILONE_SL_17);
        StructuralIntegrity structuralIntegrity = PartsScenario.unitParts(shilone, StructuralIntegrity.class)
                                                        .getFirst();
        Aero fighter = (Aero) shilone.getEntity();
        fighter.setSI(0);

        structuralIntegrity.doMaintenanceDamage(1);

        assertEquals(0, fighter.getSI());
    }
}
