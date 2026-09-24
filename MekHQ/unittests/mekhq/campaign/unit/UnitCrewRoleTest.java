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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

/**
 * The crew roles a {@link Unit} reports for its seats must be the roles {@code Person.canDrive} and
 * {@code Person.canGun} accept for that unit, or the seat can never be filled by the person the roster
 * says belongs in it.
 */
class UnitCrewRoleTest {

    /**
     * A small craft flies on Piloting/Spacecraft like a DropShip, so its pilot is a vessel pilot. Reporting
     * an aerospace pilot here put a person in the seat who could not fly the craft.
     */
    @Test
    void smallCraftTakesAVesselPilotAndVesselGunners() {
        Unit unit = unitFor(mock(SmallCraft.class));

        assertEquals(PersonnelRole.VESSEL_PILOT, unit.getDriverRole());
        assertEquals(PersonnelRole.VESSEL_GUNNER, unit.getGunnerRole());
    }

    @Test
    void dropShipTakesAVesselPilotAndVesselGunners() {
        Unit unit = unitFor(mock(Dropship.class));

        assertEquals(PersonnelRole.VESSEL_PILOT, unit.getDriverRole());
        assertEquals(PersonnelRole.VESSEL_GUNNER, unit.getGunnerRole());
    }

    @Test
    void jumpShipTakesAVesselPilotAndVesselGunners() {
        Unit unit = unitFor(mock(Jumpship.class));

        assertEquals(PersonnelRole.VESSEL_PILOT, unit.getDriverRole());
        assertEquals(PersonnelRole.VESSEL_GUNNER, unit.getGunnerRole());
    }

    /** The vessel rule must not widen to fighters, which keep their aerospace pilot. */
    @Test
    void aerospaceFighterKeepsItsAerospacePilot() {
        AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
        when(fighter.isAerospace()).thenReturn(true);
        Unit unit = unitFor(fighter);

        assertEquals(PersonnelRole.AEROSPACE_PILOT, unit.getDriverRole());
        assertEquals(PersonnelRole.AEROSPACE_PILOT, unit.getGunnerRole());
    }

    private static Unit unitFor(Entity entity) {
        Campaign campaign = mock(Campaign.class);
        return new Unit(entity, campaign);
    }
}
