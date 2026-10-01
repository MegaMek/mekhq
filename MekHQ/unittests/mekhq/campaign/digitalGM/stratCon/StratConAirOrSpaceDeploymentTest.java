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
package mekhq.campaign.digitalGM.stratCon;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.isFormationDeployableToAirOrSpace;
import static mekhq.campaign.mission.scenarios.ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX;
import static mekhq.campaign.mission.scenarios.ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.TransportShipAssignment;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link StratConRulesManager#isFormationDeployableToAirOrSpace(List, int, MapLocation)}: every unit in a
 * formation must either deploy to a low altitude or space scenario itself, or be carried by a unit in the same
 * formation that can.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConAirOrSpaceDeploymentTest {

    private static Unit createUnit(int unitType, boolean isAerospace, boolean doomedInAtmosphere,
          boolean doomedInSpace) {
        Entity entity = mock(Entity.class);
        when(entity.getUnitType()).thenReturn(unitType);
        when(entity.isAerospace()).thenReturn(isAerospace);
        when(entity.doomedInAtmosphere()).thenReturn(doomedInAtmosphere);
        when(entity.doomedInSpace()).thenReturn(doomedInSpace);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        return unit;
    }

    private static Unit createSmallCraft() {
        return createUnit(UnitType.SMALL_CRAFT, true, false, false);
    }

    private static Unit createInfantry() {
        return createUnit(UnitType.INFANTRY, false, false, true);
    }

    private static void loadAboardShip(Unit cargo, Unit transport) {
        TransportShipAssignment assignment = mock(TransportShipAssignment.class);
        when(assignment.getTransport()).thenReturn(transport);
        when(cargo.getTransportShipAssignment()).thenReturn(assignment);
    }

    private static void loadTactically(Unit cargo, Unit transport) {
        ITransportAssignment assignment = mock(ITransportAssignment.class);
        when(assignment.getTransport()).thenReturn(transport);
        when(cargo.getTacticalTransportAssignment()).thenReturn(assignment);
    }

    @Test
    void allAerospaceFormationIsDeployable() {
        List<Unit> units = List.of(createSmallCraft(), createUnit(UnitType.AEROSPACE_FIGHTER, true, false, false));

        assertTrue(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX, MapLocation.Space));
    }

    @Test
    void emptyFormationIsNotDeployable() {
        assertFalse(isFormationDeployableToAirOrSpace(new ArrayList<>(), SPECIAL_UNIT_TYPE_ATB_AERO_MIX,
              MapLocation.Space));
    }

    @Test
    void groundOnlyFormationIsNotDeployable() {
        List<Unit> units = List.of(createInfantry(), createInfantry());

        assertFalse(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_MIX, MapLocation.LowAtmosphere));
    }

    @Test
    void mixedFormationWithAllGroundUnitsShipTransportedIsDeployable() {
        Unit smallCraft = createSmallCraft();
        Unit infantryOne = createInfantry();
        Unit infantryTwo = createInfantry();
        loadAboardShip(infantryOne, smallCraft);
        loadAboardShip(infantryTwo, smallCraft);

        List<Unit> units = List.of(smallCraft, infantryOne, infantryTwo);

        assertTrue(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX, MapLocation.Space));
    }

    @Test
    void mixedFormationWithTacticallyTransportedGroundUnitIsDeployable() {
        Unit smallCraft = createSmallCraft();
        Unit infantry = createInfantry();
        loadTactically(infantry, smallCraft);

        List<Unit> units = List.of(smallCraft, infantry);

        assertTrue(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX,
              MapLocation.LowAtmosphere));
    }

    @Test
    void mixedFormationWithOneUntransportedGroundUnitIsNotDeployable() {
        Unit smallCraft = createSmallCraft();
        Unit loadedInfantry = createInfantry();
        Unit strandedInfantry = createInfantry();
        loadAboardShip(loadedInfantry, smallCraft);

        List<Unit> units = List.of(smallCraft, loadedInfantry, strandedInfantry);

        assertFalse(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX, MapLocation.Space));
    }

    @Test
    void groundUnitCarriedByTransportOutsideFormationIsNotDeployable() {
        Unit outsideSmallCraft = createSmallCraft();
        Unit infantry = createInfantry();
        loadAboardShip(infantry, outsideSmallCraft);

        List<Unit> units = List.of(createSmallCraft(), infantry);

        assertFalse(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX, MapLocation.Space));
    }

    @Test
    void groundUnitCarriedByTransportThatCannotDeployIsNotDeployable() {
        // A craft that cannot survive in atmosphere cannot carry anything into a low altitude scenario
        Unit unstreamlinedCraft = createUnit(UnitType.SMALL_CRAFT, true, true, false);
        Unit infantry = createInfantry();
        loadAboardShip(infantry, unstreamlinedCraft);

        List<Unit> units = List.of(unstreamlinedCraft, infantry);

        assertFalse(isFormationDeployableToAirOrSpace(units, SPECIAL_UNIT_TYPE_ATB_AERO_MIX,
              MapLocation.LowAtmosphere));
    }

    @Test
    void aerospaceUnitDoomedInSpaceIsNotDeployableToSpace() {
        Unit conventionalFighter = createUnit(UnitType.CONV_FIGHTER, true, false, true);

        assertFalse(isFormationDeployableToAirOrSpace(List.of(conventionalFighter), SPECIAL_UNIT_TYPE_ATB_AERO_MIX,
              MapLocation.Space));
        assertTrue(isFormationDeployableToAirOrSpace(List.of(conventionalFighter), SPECIAL_UNIT_TYPE_ATB_AERO_MIX,
              MapLocation.LowAtmosphere));
    }

    @Test
    void aerospaceUnitNotMatchingTemplateTypeIsNotDeployable() {
        List<Unit> units = List.of(createSmallCraft());

        assertFalse(isFormationDeployableToAirOrSpace(units, UnitType.AEROSPACE_FIGHTER, MapLocation.Space));
    }
}
