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
package mekhq.campaign.personnel;

import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.personnel.enums.ExtraIncome;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

/**
 * Tests the {@link ATOWTraits} mechanics that live on {@link Person}: trait bounds, the adjusted Connections, Fame,
 * Edge and Reputation values, and the monthly Connections donation, burn and re-establish checks.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PersonTraitsTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 1);

    private Person person;

    @BeforeEach
    void beforeEach() {
        Campaign campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = new Person(campaign);
    }

    private void grantAbility(String ability) {
        person.getOptions().acquireAbility(LVL3_ADVANTAGES, ability, true);
    }

    @Nested
    class TraitBounds {
        @ParameterizedTest
        @EnumSource(value = ATOWTraits.class, names = { "CONNECTIONS", "WEALTH", "FAME", "UNLUCKY", "BLOODMARK",
                                                        "EXTRA_INCOME" })
        void setterClampsAboveMaximum(ATOWTraits trait) {
            setTrait(trait, trait.getMaximum() + 5);
            assertEquals(trait.getMaximum(), getTrait(trait));
        }

        @ParameterizedTest
        @EnumSource(value = ATOWTraits.class, names = { "CONNECTIONS", "WEALTH", "FAME", "UNLUCKY", "BLOODMARK",
                                                        "EXTRA_INCOME" })
        void setterClampsBelowMinimum(ATOWTraits trait) {
            setTrait(trait, trait.getMinimum() - 5);
            assertEquals(trait.getMinimum(), getTrait(trait));
        }

        @ParameterizedTest
        @EnumSource(value = ATOWTraits.class, names = { "CONNECTIONS", "WEALTH", "FAME", "UNLUCKY", "BLOODMARK" })
        void changeClampsAtBothEnds(ATOWTraits trait) {
            setTrait(trait, trait.getMaximum());
            changeTrait(trait, 3);
            assertEquals(trait.getMaximum(), getTrait(trait));

            setTrait(trait, trait.getMinimum());
            changeTrait(trait, -3);
            assertEquals(trait.getMinimum(), getTrait(trait));
        }

        @ParameterizedTest
        @EnumSource(value = ATOWTraits.class, names = { "CONNECTIONS", "WEALTH", "FAME", "UNLUCKY", "BLOODMARK" })
        void changeAppliesDeltaWithinBounds(ATOWTraits trait) {
            setTrait(trait, 0);
            changeTrait(trait, trait.getMaximum() > 0 ? 1 : -1);
            assertEquals(trait.getMaximum() > 0 ? 1 : -1, getTrait(trait));
        }

        @Test
        void extraIncomeTraitLevelMapsToMatchingEnum() {
            person.setExtraIncomeFromTraitLevel(-3);
            assertEquals(ExtraIncome.NEGATIVE_THREE, person.getExtraIncome());

            person.setExtraIncomeFromTraitLevel(7);
            assertEquals(ExtraIncome.POSITIVE_SEVEN, person.getExtraIncome());
        }

        private void setTrait(ATOWTraits trait, int value) {
            switch (trait) {
                case CONNECTIONS -> person.setConnections(value);
                case WEALTH -> person.setWealth(value);
                case FAME -> person.setFame(value);
                case UNLUCKY -> person.setUnlucky(value);
                case BLOODMARK -> person.setBloodmark(value);
                case EXTRA_INCOME -> person.setExtraIncomeFromTraitLevel(value);
                default -> throw new IllegalArgumentException("Unsupported trait " + trait);
            }
        }

        private void changeTrait(ATOWTraits trait, int delta) {
            switch (trait) {
                case CONNECTIONS -> person.changeConnections(delta);
                case WEALTH -> person.changeWealth(delta);
                case FAME -> person.changeFame(delta);
                case UNLUCKY -> person.changeUnlucky(delta);
                case BLOODMARK -> person.changeBloodmark(delta);
                default -> throw new IllegalArgumentException("Unsupported trait " + trait);
            }
        }

        private int getTrait(ATOWTraits trait) {
            return switch (trait) {
                case CONNECTIONS -> person.getConnections();
                case WEALTH -> person.getWealth();
                case FAME -> person.getFame();
                case UNLUCKY -> person.getUnlucky();
                case BLOODMARK -> person.getBloodmark();
                case EXTRA_INCOME -> person.getExtraIncomeTraitLevel();
                default -> throw new IllegalArgumentException("Unsupported trait " + trait);
            };
        }
    }

    @Nested
    class AdjustedConnections {
        @Test
        void noModifiersReturnsBaseValue() {
            person.setConnections(4);
            assertEquals(4, person.getAdjustedConnections(false));
        }

        @Test
        void burnedConnectionsAreZeroUnlessTemporaryAdjustmentsExcluded() {
            person.setConnections(4);
            person.setBurnedConnectionsEndDate(TODAY.plusMonths(2));

            assertEquals(0, person.getAdjustedConnections(false));
            assertEquals(4, person.getAdjustedConnections(true));
        }

        @Test
        void clinicalParanoiaZeroesConnectionsUnlessTemporaryAdjustmentsExcluded() {
            person.setConnections(4);
            person.setSufferingFromClinicalParanoia(true);

            assertEquals(0, person.getAdjustedConnections(false));
            assertEquals(4, person.getAdjustedConnections(true));
        }

        @ParameterizedTest
        @CsvSource({ COMPULSION_XENOPHOBIA + ", 3", ATOW_CITIZENSHIP + ", 5", COMPULSION_MILD_PARANOIA + ", 3",
                     FLAW_IN_FOR_LIFE + ", 3" })
        void singleModifierIsApplied(String ability, int expected) {
            person.setConnections(4);
            grantAbility(ability);
            assertEquals(expected, person.getAdjustedConnections(false));
        }

        @Test
        void modifiersStack() {
            person.setConnections(4);
            grantAbility(COMPULSION_XENOPHOBIA);
            grantAbility(COMPULSION_MILD_PARANOIA);
            grantAbility(FLAW_IN_FOR_LIFE);
            assertEquals(1, person.getAdjustedConnections(false));
        }

        @Test
        void resultIsClampedToTraitBounds() {
            person.setConnections(0);
            grantAbility(COMPULSION_XENOPHOBIA);
            assertEquals(ATOWTraits.CONNECTIONS.getMinimum(), person.getAdjustedConnections(false));

            person.setConnections(10);
            grantAbility(ATOW_CITIZENSHIP);
            person.getOptions().acquireAbility(LVL3_ADVANTAGES, COMPULSION_XENOPHOBIA, false);
            assertEquals(ATOWTraits.CONNECTIONS.getMaximum(), person.getAdjustedConnections(false));
        }
    }

    @Nested
    class AdjustedFame {
        @Test
        void noModifiersReturnsBaseValue() {
            person.setFame(3);
            assertEquals(3, person.getAdjustedFame(false, false, TODAY));
        }

        @ParameterizedTest
        @CsvSource({ COMPULSION_RACISM + ", 2", COMPULSION_PATHOLOGIC_RACISM + ", 1", COMPULSION_XENOPHOBIA + ", 2" })
        void singleModifierIsApplied(String ability, int expected) {
            person.setFame(3);
            grantAbility(ability);
            assertEquals(expected, person.getAdjustedFame(false, false, TODAY));
        }

        @Test
        void resultIsClampedToMinimum() {
            person.setFame(-5);
            grantAbility(COMPULSION_PATHOLOGIC_RACISM);
            assertEquals(ATOWTraits.FAME.getMinimum(), person.getAdjustedFame(false, false, TODAY));
        }

        @Test
        void agingIsIgnoredOutsideClanCampaigns() {
            person.setDateOfBirth(TODAY.minusYears(60));
            person.setFame(2);
            assertEquals(2, person.getAdjustedFame(true, false, TODAY));
        }
    }

    @Nested
    class AdjustedEdge {
        @Test
        void unluckySubtractsFromEdge() {
            person.setEdge(5);
            person.setUnlucky(2);
            assertEquals(3, person.getAdjustedEdge());
        }

        @Test
        void unluckyStacksWithOtherEdgePenalties() {
            person.setEdge(5);
            person.setUnlucky(1);
            grantAbility(COMPULSION_TRAUMATIC_PAST);
            grantAbility(FLAW_IN_FOR_LIFE);
            grantAbility(UNOFFICIAL_DOBROWSKI_SYNDROME);
            assertEquals(1, person.getAdjustedEdge());
        }
    }

    @Nested
    class AdjustedReputation {
        @Test
        void fameAndConnectionsAreAdded() {
            person.setFame(2);
            person.setConnections(3);
            assertEquals(5, person.getAdjustedReputation(false, false, TODAY));
        }

        @Test
        void dontYouKnowWhoIAmBoostsFame() {
            person.setFame(4);
            grantAbility(DONT_YOU_KNOW_WHO_I_AM);
            assertEquals(5, person.getAdjustedReputation(false, false, TODAY));
        }

        @Test
        void certifiedNobodyReducesFame() {
            person.setFame(4);
            grantAbility(CERTIFIED_NOBODY);
            assertEquals(3, person.getAdjustedReputation(false, false, TODAY));
        }

        @Test
        void importantFriendsBoostsConnections() {
            person.setConnections(4);
            grantAbility(IMPORTANT_FRIENDS);
            assertEquals(5, person.getAdjustedReputation(false, false, TODAY));
        }

        @Test
        void forgetsToReplyReducesConnections() {
            person.setConnections(4);
            grantAbility(FORGETS_TO_REPLY);
            assertEquals(3, person.getAdjustedReputation(false, false, TODAY));
        }

        @Test
        void burnedConnectionsContributeNothing() {
            person.setConnections(4);
            person.setBurnedConnectionsEndDate(TODAY.plusMonths(1));
            assertEquals(0, person.getAdjustedReputation(false, false, TODAY));
        }
    }

    @Nested
    class ConnectionsWealthCheck {
        private Finances finances;

        @BeforeEach
        void beforeEach() {
            finances = mock(Finances.class);
            person.setCommander(true);
            person.setConnections(5);
        }

        @Test
        void nonCommanderGainsNothing() {
            person.setCommander(false);
            assertTrue(rollWealthCheck(12).isEmpty());
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void burnedConnectionsGainNothing() {
            person.setBurnedConnectionsEndDate(TODAY.plusMonths(1));
            assertTrue(rollWealthCheck(12).isEmpty());
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void zeroConnectionsGainNothing() {
            person.setConnections(0);
            assertTrue(rollWealthCheck(12).isEmpty());
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void failedRollGainsNothing() {
            assertTrue(rollWealthCheck(ATOWTraits.CONNECTIONS_TARGET_NUMBER - 1).isEmpty());
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void successfulRollCreditsLevelDonation() {
            String report = rollWealthCheck(ATOWTraits.CONNECTIONS_TARGET_NUMBER);

            assertFalse(report.isEmpty());
            Money expected = ConnectionsLevel.CONNECTIONS_FIVE.getWealth();
            verify(finances).credit(eq(TransactionType.WEALTH), eq(TODAY), eq(expected), anyString());
        }

        @Test
        void donationUsesAdjustedConnections() {
            grantAbility(ATOW_CITIZENSHIP); // 5 -> 6
            rollWealthCheck(12);

            Money expected = ConnectionsLevel.CONNECTIONS_SIX.getWealth();
            verify(finances).credit(eq(TransactionType.WEALTH), eq(TODAY), eq(expected), anyString());
        }

        private String rollWealthCheck(int roll) {
            try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                compute.when(() -> Compute.d6(2)).thenReturn(roll);
                return person.performConnectionsWealthCheck(TODAY, finances);
            }
        }
    }

    @Nested
    class BurnedContacts {
        @Test
        void zeroConnectionsNeverBurn() {
            person.setConnections(0);
            assertTrue(rollBurnCheck(2, 1).isEmpty());
            assertNull(person.getBurnedConnectionsEndDate());
        }

        @Test
        void rollAtBurnChanceBurnsForOneD6Months() {
            person.setConnections(1);
            int burnChance = ConnectionsLevel.CONNECTIONS_ONE.getBurnChance();

            String report = rollBurnCheck(burnChance, 3);

            assertFalse(report.isEmpty());
            assertEquals(TODAY.plusMonths(3), person.getBurnedConnectionsEndDate());
        }

        @Test
        void rollAboveBurnChanceDoesNotBurn() {
            person.setConnections(1);
            int burnChance = ConnectionsLevel.CONNECTIONS_ONE.getBurnChance();

            assertTrue(rollBurnCheck(burnChance + 1, 3).isEmpty());
            assertNull(person.getBurnedConnectionsEndDate());
        }

        @Test
        void higherConnectionsBurnLessOften() {
            person.setConnections(10);
            int lowLevelBurnChance = ConnectionsLevel.CONNECTIONS_ONE.getBurnChance();

            assertTrue(rollBurnCheck(lowLevelBurnChance, 3).isEmpty());
            assertNull(person.getBurnedConnectionsEndDate());
        }

        private String rollBurnCheck(int burnRoll, int months) {
            try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
                compute.when(() -> Compute.d6(2)).thenReturn(burnRoll);
                compute.when(() -> Compute.d6(1)).thenReturn(months);
                return person.checkForBurnedContacts(TODAY);
            }
        }
    }

    @Nested
    class ReestablishContact {
        @Test
        void expiredBurnIsCleared() {
            person.setBurnedConnectionsEndDate(TODAY.minusDays(1));

            assertFalse(person.checkForConnectionsReestablishContact(TODAY).isEmpty());
            assertNull(person.getBurnedConnectionsEndDate());
        }

        @Test
        void burnEndingTodayIsKept() {
            person.setBurnedConnectionsEndDate(TODAY);

            assertTrue(person.checkForConnectionsReestablishContact(TODAY).isEmpty());
            assertNotNull(person.getBurnedConnectionsEndDate());
        }

        @Test
        void futureBurnIsKept() {
            person.setBurnedConnectionsEndDate(TODAY.plusMonths(1));

            assertTrue(person.checkForConnectionsReestablishContact(TODAY).isEmpty());
            assertNotNull(person.getBurnedConnectionsEndDate());
        }

        @Test
        void unburnedConnectionsReportNothing() {
            assertTrue(person.checkForConnectionsReestablishContact(TODAY).isEmpty());
        }
    }
}
