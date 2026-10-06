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

import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A support vehicle's engine part weighs what its engine weighs, as MegaMek works it out, not what the whole vehicle
 * weighs (issue #8699; TM p.126).
 *
 * <p>The Cellco Ranger UPU-3000 is a 17-ton tracked support vehicle with an internal combustion engine.</p>
 */
class SupportVehicleEngineWeightTest {
    private static final double CELLCO_RANGER_TONS = 17.0;

    private Unit cellcoRanger() {
        return PartsScenario.create().withUnit(UnitFixture.CELLCO_RANGER_UPU_3000);
    }

    private static SVEnginePart engineOf(Unit unit) {
        return PartsScenario.unitParts(unit, SVEnginePart.class).getFirst();
    }

    @Test
    void theEnginePartWeighsWhatTheEngineWeighs() {
        Unit unit = cellcoRanger();
        Entity entity = unit.getEntity();
        double engineWeight = entity.getEngine().getWeightEngine(entity);
        assertTrue(engineWeight < CELLCO_RANGER_TONS, "The engine is lighter than the vehicle");

        SVEnginePart engine = engineOf(unit);

        assertEquals(engineWeight, engine.getTonnage(), 0.001);
        assertEquals(engineWeight, engine.getEngineTonnage(), 0.001);
    }

    @Test
    void aStrippedEngineKeepsItsOwnWeight() {
        Unit unit = cellcoRanger();
        double engineWeight = engineOf(unit).getTonnage();

        Part strippedEngine = engineOf(unit).clone();

        assertEquals(engineWeight, strippedEngine.getTonnage(), 0.001, "The spare weighs what the engine weighed");
    }

    @Test
    void anEngineSavedWithTheVehiclesWeightIsCorrectedOnItsVehicle() {
        Unit unit = cellcoRanger();
        SVEnginePart engine = engineOf(unit);
        double engineWeight = engine.getTonnage();
        SVEnginePart engineFromAnOldSave = new SVEnginePart(engine.getUnitTonnage(), CELLCO_RANGER_TONS,
              engine.getEType(), engine.getTechRating(), engine.getFuelType(), unit.getCampaign());
        engineFromAnOldSave.setUnit(unit);

        engineFromAnOldSave.updateConditionFromEntity(false);

        assertEquals(engineWeight, engineFromAnOldSave.getTonnage(), 0.001);
    }
}
