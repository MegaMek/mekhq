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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.enums.ExtraIncome.NEGATIVE_TEN;
import static mekhq.campaign.personnel.enums.ExtraIncome.POSITIVE_TEN;
import static mekhq.campaign.personnel.enums.ExtraIncome.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.enums.ExtraIncome;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ExtraIncomeTest {
    private static LocalDate TODAY = LocalDate.of(3151, 1, 1);

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testAllLookupKeysUnique(ExtraIncome extraIncome) {
        for (ExtraIncome otherExtraIncome : ExtraIncome.values()) {
            if (extraIncome != otherExtraIncome) {
                assertNotEquals(extraIncome.getLookupKey(), otherExtraIncome.getLookupKey(),
                      "Expected lookup keys to be unique: " +
                            extraIncome.getLookupKey() +
                            " and " +
                            otherExtraIncome.getLookupKey() +
                            " had identical lookup keys levels");
            } else {
                assertEquals(extraIncome.getLookupKey(), otherExtraIncome.getLookupKey(),
                      "Expected lookup keys to be the same: " +
                            extraIncome.getLookupKey() +
                            " and " +
                            otherExtraIncome.getLookupKey() +
                            " had different lookup keys levels");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testAllTraitLevelsUnique(ExtraIncome extraIncome) {
        for (ExtraIncome otherExtraIncome : ExtraIncome.values()) {
            if (extraIncome != otherExtraIncome) {
                assertNotEquals(extraIncome.getTraitLevel(), otherExtraIncome.getTraitLevel(),
                      "Expected trait levels to be unique: " +
                            extraIncome.getLookupKey() +
                            " and " +
                            otherExtraIncome.getLookupKey() +
                            " had identical trait levels");
            } else {
                assertEquals(extraIncome.getLookupKey(), otherExtraIncome.getLookupKey(),
                      "Expected trait levels to be the same: " +
                            extraIncome.getLookupKey() +
                            " and " +
                            otherExtraIncome.getLookupKey() +
                            " had different trait levels");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testAllFinancesImprove(ExtraIncome extraIncome) {
        int currentTraitLevel = extraIncome.getTraitLevel();

        int priorTraitLevel = currentTraitLevel - 1;
        Money priorFunds = Money.of(-100000000);
        try {
            ExtraIncome otherExtraIncome = ExtraIncome.extraIncomeParseFromString(priorTraitLevel + "");
            priorFunds = otherExtraIncome.getMonthlyIncome();
        } catch (Exception ignored) {}

        Money currentFunds = extraIncome.getMonthlyIncome();
        assertTrue(currentFunds.isGreaterThan(priorFunds),
              "Expected " + extraIncome.name() + " to improve from " + priorFunds + " to " + currentFunds);
    }

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testExtraIncomeParseFromString_withValidLookupKey(ExtraIncome extraIncome) {
        ExtraIncome result = ExtraIncome.extraIncomeParseFromString(extraIncome.getLookupKey());
        assertEquals(extraIncome,
              result,
              "Expected ExtraIncome: " + extraIncome.name() + " (was: " + result.name() + ")");
    }

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testExtraIncomeParseFromString_withValidName(ExtraIncome extraIncome) {
        ExtraIncome result = ExtraIncome.extraIncomeParseFromString(extraIncome.name());
        assertEquals(extraIncome,
              result,
              "Expected ExtraIncome: " + extraIncome.name() + " (was: " + result.name() + ")");
    }

    @ParameterizedTest
    @EnumSource(value = ExtraIncome.class)
    void testExtraIncomeParseFromString_withValidTraitLevel(ExtraIncome extraIncome) {
        ExtraIncome result = ExtraIncome.extraIncomeParseFromString(extraIncome.getTraitLevel() + "");
        assertEquals(extraIncome,
              result,
              "Expected ExtraIncome: " + extraIncome.name() + " (was: " + result.name() + ")");
    }

    @Test
    void testExtraIncomeParseFromString_withInvalidLookupKey() {
        ExtraIncome result = ExtraIncome.extraIncomeParseFromString("INVALID_KEY");
        assertEquals(ZERO, result, "Expected ExtraIncome: " + ZERO.name() + " (was: " + result.name() + ")");
    }

    @Test
    void testExtraIncomeParseFromString_withInvalidName() {
        ExtraIncome result = ExtraIncome.extraIncomeParseFromString("NOT_AN_ENUM");
        assertEquals(ZERO, result, "Expected ExtraIncome: " + ZERO.name() + " (was: " + result.name() + ")");
    }

    @Test
    void testExtraIncomeParseFromInteger_withInvalidTraitLevel_tooHigh() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                                                                                 ExtraIncome.extraIncomeParseFromInteger(
                                                                                       POSITIVE_TEN.getTraitLevel() +
                                                                                             1));
        assertEquals("Invalid ExtraIncome lookup key: 11", exception.getMessage());
    }

    @Test
    void testExtraIncomeParseFromInteger_withInvalidTraitLevel_tooLow() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                                                                                 ExtraIncome.extraIncomeParseFromInteger(
                                                                                       NEGATIVE_TEN.getTraitLevel() -
                                                                                             1));
        assertEquals("Invalid ExtraIncome lookup key: -11", exception.getMessage());
    }

    @Test
    void testProcessExtraIncome_zeroExtraIncome_returnsEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(ZERO);

        String result = ExtraIncome.processExtraIncome(finances, person, TODAY);
        assertEquals("", result, "Expected empty string for zero extra income");
    }

    @Test
    void testProcessExtraIncome_isChild_isNotCommander_returnsEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(15));

        String result = ExtraIncome.processExtraIncome(finances, person, TODAY);
        assertEquals("", result, "Expected empty string for child extra income, instead found: " + result);
    }

    @Test
    void testProcessExtraIncome_isNotFirstOfMonth_returnsEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(30));
        person.setCommander(true);

        String result = ExtraIncome.processExtraIncome(finances, person, LocalDate.of(3151, 1, 2));
        assertEquals("", result, "Expected empty string for for not first of month, instead found: " + result);
    }

    @Test
    void testProcessExtraIncome_isChild_isCommander_returnsNotEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(15));
        person.setCommander(true);

        String result = ExtraIncome.processExtraIncome(finances, person, TODAY);
        assertNotEquals("", result, "Expected non-empty string for child extra income");
    }

    @Test
    void testProcessExtraIncome_isAdult_isNotCommander_returnsEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(30));

        String result = ExtraIncome.processExtraIncome(finances, person, TODAY);
        assertEquals("", result, "Expected empty string for adult non-commander extra income");
    }

    @Test
    void testProcessExtraIncome_isAdult_isCommander_returnsNotEmptyString() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(30));
        person.setCommander(true);

        String result = ExtraIncome.processExtraIncome(finances, person, TODAY);
        assertNotEquals("", result, "Expected non-empty string for adult commander extra income");
    }

    @Test
    void testProcessExtraIncome_isChild_isNotCommander_noFinancialChange() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(15));

        Money campaignFinancesBefore = mockCampaign.getFinances().getBalance();
        Money personalFinancesBefore = person.getTotalEarnings();

        ExtraIncome.processExtraIncome(finances, person, TODAY);

        Money campaignFinancesAfter = mockCampaign.getFinances().getBalance();
        Money personalFinancesAfter = person.getTotalEarnings();

        Money campaignFinancesExpected = campaignFinancesBefore.plus(Money.of(0));
        Money personalFinancesExpected = personalFinancesBefore.plus(Money.of(0));

        assertEquals(campaignFinancesExpected, campaignFinancesAfter);
        assertEquals(personalFinancesExpected, personalFinancesAfter);
    }

    @Test
    void testProcessExtraIncome_isChild_isCommander_campaignFinancesChangeOnly() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(15));
        person.setCommander(true);

        Money campaignFinancesBefore = mockCampaign.getFinances().getBalance();
        Money personalFinancesBefore = person.getTotalEarnings();

        ExtraIncome.processExtraIncome(finances, person, TODAY);

        Money campaignFinancesAfter = mockCampaign.getFinances().getBalance();
        Money personalFinancesAfter = person.getTotalEarnings();

        Money campaignFinancesExpected = campaignFinancesBefore.plus(POSITIVE_TEN.getMonthlyIncome());
        Money personalFinancesExpected = personalFinancesBefore.plus(Money.of(0));

        assertEquals(campaignFinancesExpected, campaignFinancesAfter);
        assertEquals(personalFinancesExpected, personalFinancesAfter);
    }

    @Test
    void testProcessExtraIncome_isAdult_isNotCommander_personalFinancesChangeOnly() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(30));

        Money campaignFinancesBefore = mockCampaign.getFinances().getBalance();
        Money personalFinancesBefore = person.getTotalEarnings();

        ExtraIncome.processExtraIncome(finances, person, TODAY);

        Money campaignFinancesAfter = mockCampaign.getFinances().getBalance();
        Money personalFinancesAfter = person.getTotalEarnings();

        Money campaignFinancesExpected = campaignFinancesBefore.plus(Money.of(0));
        Money personalFinancesExpected = personalFinancesBefore.plus(POSITIVE_TEN.getMonthlyIncome());

        assertEquals(campaignFinancesExpected, campaignFinancesAfter);
        assertEquals(personalFinancesExpected, personalFinancesAfter);
    }

    @Test
    void testProcessExtraIncome_isAdult_isCommander_personalFinancesChangeOnly() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Finances finances = new Finances();

        when(mockCampaign.getFinances()).thenReturn(finances);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.setExtraIncomeDirect(POSITIVE_TEN);
        person.setDateOfBirth(TODAY.minusYears(30));
        person.setCommander(true);

        Money campaignFinancesBefore = mockCampaign.getFinances().getBalance();
        Money personalFinancesBefore = person.getTotalEarnings();

        ExtraIncome.processExtraIncome(finances, person, TODAY);

        Money campaignFinancesAfter = mockCampaign.getFinances().getBalance();
        Money personalFinancesAfter = person.getTotalEarnings();

        Money campaignFinancesExpected = campaignFinancesBefore.plus(POSITIVE_TEN.getMonthlyIncome());
        Money personalFinancesExpected = personalFinancesBefore.plus(Money.of(0));

        assertEquals(campaignFinancesExpected, campaignFinancesAfter);
        assertEquals(personalFinancesExpected, personalFinancesAfter);
    }
}
