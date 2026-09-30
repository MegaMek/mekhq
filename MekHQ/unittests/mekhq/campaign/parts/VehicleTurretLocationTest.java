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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import megamek.common.equipment.IArmorState;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.parts.missing.MissingTurret;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.VehicleLocations;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A destroyed turret's missing part remembers which turret it stands for, so loading a campaign destroys that turret
 * again and nothing else, and a replacement goes back where the turret was. Checks of which locations a vehicle can
 * lose ask the vehicle, since VTOLs, superheavy tanks and other tanks number their locations differently (issue
 * #10208).
 */
class VehicleTurretLocationTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static void destroyLocation(Unit unit, int location) {
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, location);
        for (Part part : new ArrayList<>(unit.getParts())) {
            part.updateConditionFromEntity(false);
        }
    }

    /** What loading a campaign does: parts push their state back, then the unit rebuilds its parts. */
    private static void reload(Unit unit) {
        for (Part part : new ArrayList<>(unit.getParts())) {
            part.updateConditionFromPart();
        }
        unit.initializeParts(true);
        unit.runDiagnostic(false);
    }

    private static MissingTurret theMissingTurret(Unit unit) {
        return PartsScenario.unitParts(unit, MissingTurret.class).getFirst();
    }

    @Test
    void aVtolThatLostItsChinTurretKeepsItsRotorThroughALoad() {
        Unit redKite = scenario.withUnit(UnitFixture.RED_KITE_ATTACK_VTOL);
        destroyLocation(redKite, VTOL.LOC_TURRET);

        reload(redKite);

        Entity entity = redKite.getEntity();
        assertTrue(entity.getInternal(VTOL.LOC_ROTOR) > 0, "The rotor is not destroyed by the lost turret");
        assertTrue(redKite.isFunctional());
        assertEquals(1, PartsScenario.unitParts(redKite, MissingTurret.class).size());
    }

    @Test
    void aDualTurretTankThatLostOneTurretKeepsTheOtherThroughALoad() {
        Unit zephyros = scenario.withUnit(UnitFixture.ZEPHYROS_DUAL_TURRET);
        destroyLocation(zephyros, Tank.LOC_TURRET_2);

        reload(zephyros);

        assertTrue(zephyros.getEntity().getInternal(Tank.LOC_TURRET) > 0, "The other turret survives the load");
        assertEquals(1, PartsScenario.unitParts(zephyros, Turret.class).size());
        assertEquals(1, PartsScenario.unitParts(zephyros, MissingTurret.class).size());
    }

    @Test
    void aSuperheavyTankIsBuiltWithItsTurretWhereItIsAndSurvivesLosingIt() {
        Unit burke = scenario.withUnit(UnitFixture.BURKE_II_SUPERHEAVY_TANK);
        Tank tank = (Tank) burke.getEntity();
        Turret turret = PartsScenario.unitParts(burke, Turret.class).getFirst();
        assertEquals(tank.getLocTurret(), turret.getLoc(), "The turret part is at the superheavy's turret location");

        destroyLocation(burke, tank.getLocTurret());

        assertTrue(Unit.isRepairable(tank), "Losing only its turret leaves a superheavy tank repairable");
        assertEquals(tank.getLocTurret(), theMissingTurret(burke).getTurretLocation());
    }

    @Test
    void aReplacementTurretGoesWhereTheLostOneWas() {
        Unit zephyros = scenario.withUnit(UnitFixture.ZEPHYROS_DUAL_TURRET);
        Turret frontTurret = null;
        for (Turret turret : PartsScenario.unitParts(zephyros, Turret.class)) {
            if (turret.getLoc() == Tank.LOC_TURRET) {
                frontTurret = turret;
            }
        }
        destroyLocation(zephyros, Tank.LOC_TURRET_2);
        MissingTurret missingTurret = theMissingTurret(zephyros);
        Turret spare = assertInstanceOf(Turret.class, frontTurret.clone());
        spare.setUnit(null);
        scenario.withSpare(spare, 1);

        missingTurret.fix();

        boolean isTurretBackInPlace = false;
        for (Turret turret : PartsScenario.unitParts(zephyros, Turret.class)) {
            if (turret.getLoc() == Tank.LOC_TURRET_2) {
                isTurretBackInPlace = true;
            }
        }
        assertTrue(isTurretBackInPlace, "The replacement is fitted at the lost turret's location");
    }

    @Test
    void aMissingTurretKeepsItsLocationThroughASave() throws Exception {
        MissingTurret secondTurret = new MissingTurret(40, Tank.LOC_TURRET_2, 1.0, scenario.getCampaign());

        Part loaded = PartXmlRoundTripTest.saveAndLoad(secondTurret, scenario.getCampaign());

        assertEquals(Tank.LOC_TURRET_2, assertInstanceOf(MissingTurret.class, loaded).getTurretLocation());
    }

    @Test
    void aMissingTurretSavedWithoutItsLocationTakesTheDestroyedTurret() {
        Unit zephyros = scenario.withUnit(UnitFixture.ZEPHYROS_DUAL_TURRET);
        destroyLocation(zephyros, Tank.LOC_TURRET_2);
        MissingTurret savedWithoutLocation = new MissingTurret();
        savedWithoutLocation.setUnit(zephyros);

        assertEquals(Tank.LOC_TURRET_2, savedWithoutLocation.getTurretLocation());
    }

    @Test
    void vehiclesAreWreckedOnlyByLosingStructureLocations() {
        VTOL redKiteDesign = (VTOL) UnitFixture.RED_KITE_ATTACK_VTOL.loadEntity();
        Tank burkeDesign = (Tank) UnitFixture.BURKE_II_SUPERHEAVY_TANK.loadEntity();

        assertTrue(VehicleLocations.canLoseWithoutWrecking(redKiteDesign, VTOL.LOC_ROTOR), "A lost rotor is replaced");
        assertTrue(VehicleLocations.canLoseWithoutWrecking(redKiteDesign, VTOL.LOC_TURRET));
        assertTrue(VehicleLocations.canLoseWithoutWrecking(burkeDesign, burkeDesign.getLocTurret()));
        assertFalse(VehicleLocations.canLoseWithoutWrecking(burkeDesign, Tank.LOC_TURRET),
              "On a superheavy tank that number is a side location, not a turret");
    }
}
