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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;

/**
 * Regression tests for how MASC spares stack in the warehouse. A MASC used to join any stack of the same MASC,
 * whatever its quality or delivery status, so a quality A MASC vanished into a quality D stack and an ordered MASC
 * still in transit merged into the stack on hand and was usable at once.
 */
class MASCTest {
    private static final int UNIT_TONNAGE = 55;
    private static final int ENGINE_RATING = 275;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static MASC newMasc(PartsScenario scenario, int engineRating, PartQuality quality) {
        MASC masc = new MASC(UNIT_TONNAGE, EquipmentType.get(EquipmentTypeLookup.IS_MASC), -1,
              scenario.getCampaign(), engineRating, false);
        masc.setQuality(quality);
        return masc;
    }

    private static boolean hasStack(List<Part> spares, Predicate<Part> condition) {
        for (Part spare : spares) {
            if (condition.test(spare)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void matchingSparesShareAStack() {
        PartsScenario scenario = PartsScenario.create();

        scenario.withSpare(newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D), 1);
        scenario.withSpare(newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D), 1);

        assertEquals(1, scenario.getSpareParts().size());
        assertEquals(2, scenario.countSpareParts(MASC.class));
    }

    @Test
    void spareOfAnotherQualityKeepsItsOwnStack() {
        PartsScenario scenario = PartsScenario.create();

        scenario.withSpare(newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D), 1);
        scenario.withSpare(newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_A), 1);

        List<Part> spares = scenario.getSpareParts();
        assertEquals(2, spares.size());
        assertTrue(hasStack(spares, spare -> spare.getQuality() == PartQuality.QUALITY_A));
        assertTrue(hasStack(spares, spare -> spare.getQuality() == PartQuality.QUALITY_D));
    }

    @Test
    void orderedSpareInTransitDoesNotJoinTheStackOnHand() {
        PartsScenario scenario = PartsScenario.create();
        scenario.withSpare(newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D), 1);

        MASC orderedMasc = newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D);
        scenario.getCampaign().getQuartermaster().addPart(orderedMasc, 14, false);

        List<Part> spares = scenario.getSpareParts();
        assertEquals(2, spares.size());
        assertTrue(hasStack(spares, spare -> spare.isPresent() && (spare.getQuantity() == 1)));
        assertTrue(hasStack(spares, spare -> spare.getDaysToArrival() == 14));
    }

    @Test
    void differentEngineRatingIsNotTheSamePartType() {
        PartsScenario scenario = PartsScenario.create();
        MASC masc = newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D);
        MASC otherRating = newMasc(scenario, ENGINE_RATING + 25, PartQuality.QUALITY_D);

        assertTrue(masc.isSamePartType(masc.clone()));
        assertFalse(masc.isSamePartType(otherRating));
        assertFalse(masc.isSamePartTypeAndStatus(otherRating));
    }

    @Test
    void damagedSpareNeverSharesAStack() {
        PartsScenario scenario = PartsScenario.create();
        MASC masc = newMasc(scenario, ENGINE_RATING, PartQuality.QUALITY_D);
        MASC damaged = masc.clone();
        damaged.setHits(1);
        MASC alsoDamaged = masc.clone();
        alsoDamaged.setHits(1);

        assertFalse(masc.isSamePartTypeAndStatus(damaged));
        assertFalse(damaged.isSamePartTypeAndStatus(alsoDamaged));
    }
}
