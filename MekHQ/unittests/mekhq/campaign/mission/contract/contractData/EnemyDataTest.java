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
package mekhq.campaign.mission.contract.contractData;

import static megamek.client.ui.util.PlayerColour.RED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.enums.DragoonRating;
import org.junit.jupiter.api.Test;

/**
 * Verifies the convenience and copy constructors of {@link EnemyData}, including the two targeted copy constructors
 * that override only the force ratings or only the batchall flag. Camouflage and faction lookups are pass-through data,
 * so no campaign or universe fixtures are needed.
 */
class EnemyDataTest {

    private static final Camouflage CAMOUFLAGE = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, "Red");

    private static EnemyData sampleEnemy(String sponsorFactionCode) {
        return new EnemyData("DC", sponsorFactionCode, "Draconis Combine", null, CAMOUFLAGE);
    }

    @Test
    void convenienceConstructorAppliesDefaults() {
        EnemyData enemy = sampleEnemy(null);

        assertEquals("DC", enemy.factionCode());
        assertNull(enemy.sponsorFactionCode(), "an enemy fielding its own troops has no mercenary sponsor");
        assertEquals("Draconis Combine", enemy.displayName());
        assertEquals(SkillLevel.REGULAR, enemy.forceSkill(), "default force skill");
        assertEquals(DragoonRating.DRAGOON_C.getRating(), enemy.equipmentRating(), "default equipment rating");
        assertNull(enemy.opposingCommander(), "no opposing commander was supplied");
        assertEquals(CAMOUFLAGE, enemy.camouflage(), "camouflage is passed straight through");
        assertEquals(RED, enemy.color(), "enemy forces default to the opposing colour");
        assertTrue(enemy.batchallAccepted(), "batchall is accepted by default");
    }

    @Test
    void sponsorFactionCodeIsPreservedWhenPresent() {
        EnemyData enemy = sampleEnemy("MERC");
        assertEquals("MERC", enemy.sponsorFactionCode());
    }

    @Test
    void ratingCopyConstructorOverridesRatingsAndPreservesEverythingElse() {
        EnemyData original = sampleEnemy("MERC");

        EnemyData updated = new EnemyData(original, SkillLevel.ELITE, 7);

        assertEquals(SkillLevel.ELITE, updated.forceSkill());
        assertEquals(7, updated.equipmentRating());

        assertEquals(original.factionCode(), updated.factionCode());
        assertEquals(original.sponsorFactionCode(), updated.sponsorFactionCode());
        assertEquals(original.displayName(), updated.displayName());
        assertEquals(original.opposingCommander(), updated.opposingCommander());
        assertEquals(original.camouflage(), updated.camouflage());
        assertEquals(original.color(), updated.color());
        assertEquals(original.batchallAccepted(), updated.batchallAccepted());
    }

    @Test
    void batchallCopyConstructorOverridesOnlyTheBatchallFlag() {
        EnemyData original = sampleEnemy(null);

        EnemyData updated = new EnemyData(original, false);

        assertFalse(updated.batchallAccepted(), "batchall copy constructor sets the flag");

        assertEquals(original.factionCode(), updated.factionCode());
        assertEquals(original.sponsorFactionCode(), updated.sponsorFactionCode());
        assertEquals(original.displayName(), updated.displayName());
        assertEquals(original.forceSkill(), updated.forceSkill());
        assertEquals(original.equipmentRating(), updated.equipmentRating());
        assertEquals(original.opposingCommander(), updated.opposingCommander());
        assertEquals(original.camouflage(), updated.camouflage());
        assertEquals(original.color(), updated.color());
    }
}
