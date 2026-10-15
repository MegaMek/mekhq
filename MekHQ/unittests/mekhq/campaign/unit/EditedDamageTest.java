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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import megamek.common.equipment.IArmorState;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingMekLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Game Master who destroys a Mek's location in Edit Damage and then restores it gets the location back, rather
 * than a "replace" task that cannot be cleared (issue #3422).
 */
class EditedDamageTest {
    private PartsScenario scenario;
    private Unit locust;
    private Entity entity;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        entity = locust.getEntity();
        entity.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_ARM);
        locust.runDiagnostic(false);
        assertTrue(missingLeftArm().isPresent(), "Destroying the left arm leaves it to be replaced");
    }

    /** What closing Edit Damage does. */
    private void closeEditDamage() {
        EditedDamage.reinstateRestoredLocations(locust);
        locust.runDiagnostic(false);
    }

    private Optional<MissingMekLocation> missingLeftArm() {
        return PartsScenario.unitParts(locust, MissingMekLocation.class)
                     .stream()
                     .filter(location -> location.getLocation() == Mek.LOC_LEFT_ARM)
                     .findFirst();
    }

    private Optional<MekLocation> leftArm() {
        return PartsScenario.unitParts(locust, MekLocation.class)
                     .stream()
                     .filter(location -> location.getLoc() == Mek.LOC_LEFT_ARM)
                     .findFirst();
    }

    @Test
    void restoringTheStructureBringsTheLocationBack() {
        int sparesBefore = scenario.getSpareParts().size();
        entity.setInternal(entity.getOInternal(Mek.LOC_LEFT_ARM), Mek.LOC_LEFT_ARM);

        closeEditDamage();

        assertTrue(missingLeftArm().isEmpty(), "No replace task is left behind");
        assertTrue(leftArm().isPresent(), "The left arm is back on the Locust");
        assertFalse(leftArm().orElseThrow().needsFixing(), "It is back at full structure");
        assertEquals(sparesBefore, scenario.getSpareParts().size(), "No spare was used");
    }

    @Test
    void aPartlyRestoredLocationKeepsTheStructureTheGameMasterGaveIt() {
        int partStructure = entity.getOInternal(Mek.LOC_LEFT_ARM) - 1;
        entity.setInternal(partStructure, Mek.LOC_LEFT_ARM);

        closeEditDamage();

        assertEquals(partStructure, entity.getInternal(Mek.LOC_LEFT_ARM));
        assertTrue(leftArm().orElseThrow().needsFixing(), "The rest of the structure still needs repair");
    }

    @Test
    void aLocationStillDestroyedStaysToBeReplaced() {
        closeEditDamage();

        assertTrue(missingLeftArm().isPresent(), "The left arm still needs replacing");
    }
}
