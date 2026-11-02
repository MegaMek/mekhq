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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * Refitting between an Omni unit and a standard one is a Class F refit, in either direction and for every unit type
 * that can be an Omni. Converting changes which components are fixed to the chassis, and altering an Omni unit's fixed
 * components is Class F (Campaign Operations p.212). The refit was graded only by the parts it moved, so an Eisensturm
 * EST-O refitted to the EST-R3, its standard pre-production version with the same weapons and armor, came out Class B.
 *
 * <p>The Eisensturm EST-O and EST-R3 are pinned copies from mm-data. The standard Epona and Doloire are the Omni files
 * loaded with every pod made fixed.</p>
 */
class RefitOmniConversionTest {
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

    private static Entity loadAsStandard(String unitName, boolean isBlk) {
        Entity entity = load(unitName, isBlk);
        entity.setOmni(false);
        for (Mounted<?> mounted : entity.getEquipment()) {
            mounted.setOmniPodMounted(false);
        }
        return entity;
    }

    private Refit refit(Entity oldEntity, Entity newEntity) {
        Unit unit = scenario.getCampaign().addNewUnit(oldEntity, false, 0, PartQuality.QUALITY_D);
        return new Refit(unit, newEntity, true, false, false);
    }

    private static void assertClassFConversion(Refit refit) {
        String reasons = String.join("; ", refit.getRefitClassReasons());
        assertNull(refit.checkFixable(), "Converting is allowed as a Class F refit");
        assertEquals(Refit.CLASS_F, refit.getRefitClass(), reasons);
        assertTrue(reasons.contains("Omni unit and a standard unit"), reasons);
    }

    @Test
    void anOmniFighterRefittedToItsStandardVersionIsClassF() {
        assertClassFConversion(refit(load("Eisensturm EST-O", true), load("Eisensturm EST-R3", true)));
    }

    @Test
    void aStandardFighterRefittedToItsOmniVersionIsClassF() {
        assertClassFConversion(refit(load("Eisensturm EST-R3", true), load("Eisensturm EST-O", true)));
    }

    @Test
    void anOmniVehicleRefittedToAStandardOneIsClassF() {
        assertClassFConversion(refit(load("Epona Pursuit Tank Prime", true),
              loadAsStandard("Epona Pursuit Tank Prime", true)));
    }

    @Test
    void aStandardMekRefittedToAnOmniOneIsClassF() {
        assertClassFConversion(refit(loadAsStandard("Doloire DLR-OC", false), load("Doloire DLR-OC", false)));
    }

    @Test
    void anOmniPodSwapIsNotAConversion() {
        Refit refit = refit(load("Epona Pursuit Tank Prime", true), load("Epona Pursuit Tank A", true));

        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass());
    }
}
