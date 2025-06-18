/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.MercenaryRelations.CLAN_FALLBACK_VALUE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.CLIMATE_FACTION_STANDING_MODIFIERS;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.INNER_SPHERE_FALLBACK_VALUE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.NO_STARTING_DATE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.StandingModifier.ABOVE_AVERAGE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.getMercenaryRelationsModifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MercenaryRelationsTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);

    @Test
    void testNullFactionReturnsInnerSphereFallback() {
        // Act
        double actual = getMercenaryRelationsModifier(null, today);
        double expected = INNER_SPHERE_FALLBACK_VALUE;
        // Assert
        assertEquals(expected, actual, "Expected " + expected + " for null faction. Got " + actual);
    }

    @Test
    void testInnerSphereFactionReturnsInnerSphereFallbackWhenNoRelation() {
        // Setup
        Faction testFaction = mock(Faction.class);
        when(testFaction.isClan()).thenReturn(false);
        when(testFaction.getShortName()).thenReturn("AWDJAJDFLEKQhjdn");
        // Act
        double actual = getMercenaryRelationsModifier(testFaction, today);
        double expected = INNER_SPHERE_FALLBACK_VALUE;
        // Assert
        assertEquals(expected, actual, "Expected " + expected + " fallback for Inner Sphere faction. Got " + actual);
    }

    @Test
    void testClanFactionReturnsClanFallbackWhenNoRelation() {
        // Setup
        Faction testFaction = mock(Faction.class);
        when(testFaction.isClan()).thenReturn(true);
        when(testFaction.getShortName()).thenReturn("AWDJAJDFLEKQhjdn");
        // Act
        double actual = getMercenaryRelationsModifier(testFaction, today);
        double expected = CLAN_FALLBACK_VALUE;
        // Assert
        assertEquals(expected, actual, "Expected " + expected + " fallback for Clan faction. Got " + actual);
    }

    @Test
    void testReturnsModifierFromMatchingRelation() {
        // Setup
        Faction testFaction = mock(Faction.class);
        when(testFaction.isClan()).thenReturn(true);
        when(testFaction.getShortName()).thenReturn("FS");
        // Act
        double actual = getMercenaryRelationsModifier(testFaction, NO_STARTING_DATE);
        double expected = ABOVE_AVERAGE.getModifier();
        // Assert
        assertEquals(expected, actual, "Expected " + expected + " for Federated Suns faction. Got " + actual);
    }

    static Stream<Arguments> chronologicalOrderProvider() {
        List<Arguments> args = new ArrayList<>();
        for (Map.Entry<String, List<MercenaryRelations.MercenaryRelation>> entry : CLIMATE_FACTION_STANDING_MODIFIERS.entrySet()) {
            LocalDate lastStartingDate = NO_STARTING_DATE;
            for (MercenaryRelations.MercenaryRelation relation : entry.getValue()) {
                LocalDate currentStartingDate = relation.startingDate();
                if (lastStartingDate == NO_STARTING_DATE && currentStartingDate == NO_STARTING_DATE) {
                    continue;
                }
                args.add(Arguments.of(lastStartingDate, currentStartingDate, entry.getKey()));
                lastStartingDate = currentStartingDate;
            }
        }
        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("chronologicalOrderProvider")
    void testValuesInChronologicalOrder(LocalDate lastStartingDate, LocalDate currentStartingDate, String factionKey) {
        assertTrue(lastStartingDate.isBefore(currentStartingDate),
              "Starting date " + currentStartingDate + " for " + factionKey + " is before " + lastStartingDate);
    }

    static Stream<Arguments> noOverlapProvider() {
        List<Arguments> args = new ArrayList<>();
        for (Map.Entry<String, List<MercenaryRelations.MercenaryRelation>> entry : CLIMATE_FACTION_STANDING_MODIFIERS.entrySet()) {
            List<MercenaryRelations.MercenaryRelation> relations = entry.getValue();
            if (relations.isEmpty()) {
                continue;
            }
            LocalDate lastEndingDate = NO_STARTING_DATE;
            for (MercenaryRelations.MercenaryRelation relation : relations) {
                LocalDate currentStartingDate = relation.startingDate();
                if (lastEndingDate == NO_STARTING_DATE && currentStartingDate == NO_STARTING_DATE) {
                    // Skip values that have no start and end
                    lastEndingDate = relation.endingDate();
                    continue;
                }
                args.add(Arguments.of(lastEndingDate, currentStartingDate, entry.getKey()));
                lastEndingDate = relation.endingDate();
            }
        }
        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("noOverlapProvider")
    void testValuesHaveNoOverlaps(LocalDate lastEndingDate, LocalDate currentStartingDate, String factionKey) {
        assertTrue(lastEndingDate.isBefore(currentStartingDate),
              "Starting date " + currentStartingDate + " for " + factionKey +
                    " is before previous ending date " + lastEndingDate);
    }

    static Stream<Arguments> startAndEndDatesProvider() {
        List<Arguments> args = new ArrayList<>();
        for (Map.Entry<String, List<MercenaryRelations.MercenaryRelation>> entry : CLIMATE_FACTION_STANDING_MODIFIERS.entrySet()) {
            String key = entry.getKey();
            for (MercenaryRelations.MercenaryRelation relation : entry.getValue()) {
                LocalDate start = relation.startingDate();
                LocalDate end = relation.endingDate();
                args.add(Arguments.of(start, end, key));
            }
        }
        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("startAndEndDatesProvider")
    void testStartDateIsBeforeEndDate(LocalDate startingDate, LocalDate endingDate, String factionKey) {
        assertTrue(startingDate.isBefore(endingDate),
              "Starting date " + startingDate + " for " + factionKey + " is before " + endingDate);
    }
}
