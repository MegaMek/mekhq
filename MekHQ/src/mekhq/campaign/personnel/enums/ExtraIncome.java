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
package mekhq.campaign.personnel.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;

/**
 * Represents extra income or expenses associated with a person's trait level within MekHQ.
 *
 * <p>Each enum instance corresponds to a trait level ranging from -10 to +10, with an associated {@code lookupKey}
 * for identification and a {@code monthlyIncome} value that determines the expected extra income (positive) or cost
 * (negative) for that level.</p>
 *
 * <p>The enum provides methods for retrieving lookup information, trait levels, and monetary values, as well as
 * resolving instances by their lookup key, enum name, or trait level.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum ExtraIncome {
    // These values are copied from those found in A Time of War
    NEGATIVE_TEN("NEGATIVE_TEN", -10, Money.of(-5000)),
    NEGATIVE_NINE("NEGATIVE_NINE", -9, Money.of(-4000)),
    NEGATIVE_EIGHT("NEGATIVE_EIGHT", -8, Money.of(-3000)),
    NEGATIVE_SEVEN("NEGATIVE_SEVEN", -7, Money.of(-2250)),
    NEGATIVE_SIX("NEGATIVE_SIX", -6, Money.of(-1750)),
    NEGATIVE_FIVE("NEGATIVE_FIVE", -5, Money.of(-1500)),
    NEGATIVE_FOUR("NEGATIVE_FOUR", -4, Money.of(-1000)),
    NEGATIVE_THREE("NEGATIVE_THREE", -3, Money.of(-750)),
    NEGATIVE_TWO("NEGATIVE_TWO", -2, Money.of(-500)),
    NEGATIVE_ONE("NEGATIVE_ONE", -1, Money.of(-250)),
    ZERO("ZERO", 0, Money.of(0)),
    POSITIVE_ONE("POSITIVE_ONE", 1, Money.of(250)),
    POSITIVE_TWO("POSITIVE_TWO", 2, Money.of(500)),
    POSITIVE_THREE("POSITIVE_THREE", 3, Money.of(750)),
    POSITIVE_FOUR("POSITIVE_FOUR", 4, Money.of(1000)),
    POSITIVE_FIVE("POSITIVE_FIVE", 5, Money.of(1500)),
    POSITIVE_SIX("POSITIVE_SIX", 6, Money.of(1750)),
    POSITIVE_SEVEN("POSITIVE_SEVEN", 7, Money.of(2250)),
    POSITIVE_EIGHT("POSITIVE_EIGHT", 8, Money.of(3000)),
    POSITIVE_NINE("POSITIVE_NINE", 9, Money.of(4000)),
    POSITIVE_TEN("POSITIVE_TEN", 10, Money.of(5000));

    final private static String RESOURCE_BUNDLE = "mekhq.resources.ExtraIncome";

    final private static int BETTER_MONTHLY_INCOME_MULTIPLIER = 100;

    /** Lookup key for matching or identification in configs or serialization. */
    private final String lookupKey;
    /** The trait level corresponding to this extra income entry. */
    private final int traitLevel;
    /** The monthly income or expense associated with this trait level. */
    private final Money monthlyIncome;

    /**
     * Constructs an {@link ExtraIncome} enum entry.
     *
     * @param lookupKey     the string used for lookup or identification
     * @param traitLevel    the integer trait level (-10 through +10)
     * @param monthlyIncome the associated monthly income or expense
     *
     * @author Illiani
     * @since 0.50.07
     */
    ExtraIncome(String lookupKey, int traitLevel, Money monthlyIncome) {
        this.lookupKey = lookupKey;
        this.traitLevel = traitLevel;
        this.monthlyIncome = monthlyIncome;
    }

    /**
     * Returns the unique lookup key for this enum entry.
     *
     * @return the lookup key string
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupKey() {
        return lookupKey;
    }

    /**
     * Returns the trait level represented by this entry.
     *
     * @return the trait level (-10 to +10)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getTraitLevel() {
        return traitLevel;
    }

    /**
     * Returns the monthly extra income or expense for this trait level.
     *
     * <p><b>Note:</b> This fetches the monthly income directly without any adjustments. Generally you want to use
     * {@link #getMonthlyIncomeAdjusted(boolean)} instead.</p>
     *
     * @return a {@link Money} object representing the monthly income (or expense)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Money getMonthlyIncomeDirect() {
        return monthlyIncome;
    }

    /**
     * Retrieves the monthly income, optionally adjusted by a multiplier.
     *
     * <p>When better monthly income is enabled, the base monthly income is multiplied by
     * {@link #BETTER_MONTHLY_INCOME_MULTIPLIER} to provide an enhanced income value. Otherwise, the unadjusted base
     * monthly income is returned.</p>
     *
     * @param useBetterMonthlyIncome {@code true} to apply the better monthly income multiplier; {@code false} to return
     *                               the base monthly income
     *
     * @return the monthly income, either adjusted or unadjusted based on the parameter
     *
     * @author Illiani
     * @since 0.50.10
     */
    public Money getMonthlyIncomeAdjusted(boolean useBetterMonthlyIncome) {
        if (useBetterMonthlyIncome) {
            return monthlyIncome.multipliedBy(BETTER_MONTHLY_INCOME_MULTIPLIER);
        }
        return monthlyIncome;
    }

    /**
     * Parses an {@link ExtraIncome} object from an {@code Integer} entry.
     *
     * <p>This method converts the given {@code Integer} to a string and delegates parsing to
     * {@link #extraIncomeParseFromString(String)}.</p>
     *
     * @param entry the integer value representing extra income data to be parsed.
     *
     * @return the parsed {@link ExtraIncome} object.
     *
     * @throws IllegalArgumentException if no matching entry is found
     * @author Illiani
     * @since 0.50.07
     */
    public static ExtraIncome extraIncomeParseFromInteger(Integer entry) {
        return extraIncomeParseFromString(entry.toString());
    }

    /**
     * Attempts to resolve an {@code ExtraIncome} instance given a string input.
     *
     * <p>The input string may be matched against the lookup key, enum constant name, or a stringified trait level.</p>
     *
     * <p>Matching proceeds in the following order:</p>
     * <ol>
     *   <li>Lookup key</li>
     *   <li>Enum constant name</li>
     *   <li>Trait level as string</li>
     * </ol>
     *
     * <p>If no match is found, an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param entry the string to resolve
     *
     * @return the matched {@code ExtraIncome} entry
     *
     * @throws IllegalArgumentException if no matching entry is found
     * @author Illiani
     * @since 0.50.07
     */
    public static ExtraIncome extraIncomeParseFromString(String entry) throws IllegalArgumentException {
        for (ExtraIncome extraIncome : values()) {
            if (extraIncome.getLookupKey().equals(entry)) {
                return extraIncome;
            }
        }

        for (ExtraIncome extraIncome : values()) {
            if (extraIncome.name().equals(entry)) {
                return extraIncome;
            }
        }

        for (ExtraIncome extraIncome : values()) {
            if (extraIncome.getTraitLevel() == MathUtility.parseInt(entry)) {
                return extraIncome;
            }
        }

        throw new IllegalArgumentException("Invalid ExtraIncome lookup key: " + entry);
    }

    /**
     * Processes a {@link Person}'s extra income for the given date and applies financial and reporting logic.
     *
     * <ul>
     *     <li>If the person has no extra income, or if the extra income for the month is zero, this method returns
     *     an empty string.</li>
     *     <li>For non-commander adults, the extra income is paid directly to the person and no campaign report is
     *     generated.</li>
     *     <li>For commanders and children, the method updates the {@link Finances} object with a credit or debit
     *     transaction and generates a campaign report string summarizing the financial change.</li>
     * </ul>
     *
     * @param finances               The {@link Finances} object to update with any transaction that occurs.
     * @param person                 The {@link Person} whose extra income is to be processed.
     * @param today                  The {@link LocalDate} representing the current date for this transaction.
     * @param useBetterMonthlyIncome {@code true} to apply the better monthly income multiplier.
     *
     * @return A formatted campaign report string, or an empty string if there is no relevant transaction to report.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String processExtraIncome(Finances finances, Person person, LocalDate today,
          boolean useBetterMonthlyIncome) {
        ExtraIncome extraIncome = person.getExtraIncome();

        // If the character has no extra income, can expect no financial change, or it's not the first of the month
        // then early exit.
        if (extraIncome == null || extraIncome.getMonthlyIncomeDirect().isZero() || today.getDayOfMonth() != 1) {
            return "";
        }

        Money financialChange = extraIncome.getMonthlyIncomeAdjusted(useBetterMonthlyIncome);

        boolean isCampaignCommander = person.isCommander();
        boolean isChild = person.isChild(today);

        // No campaign reporting for non-commander adults
        if (!isCampaignCommander) {
            // Children don't benefit from extra income unless they are the campaign commander
            if (!isChild) {
                // Adjust the character's earnings based on their Extra Income
                person.payPerson(financialChange);
            }
            return "";
        }

        String campaignReport;
        String personTitle = person.getFullName();

        boolean financialChangeIsPositive = financialChange.isPositiveOrZero();
        String transactionMessageKey = financialChangeIsPositive
                                             ? "ExtraIncome.monthlyTransaction.positive"
                                             : "ExtraIncome.monthlyTransaction.negative";
        String reportMessageKey = financialChangeIsPositive
                                        ? "ExtraIncome.monthlyReport.positive"
                                        : "ExtraIncome.monthlyReport.negative";
        String reportColor = financialChangeIsPositive
                                   ? getPositiveColor()
                                   : getNegativeColor();

        String transactionMessage = getFormattedTextAt(RESOURCE_BUNDLE, transactionMessageKey, personTitle);

        if (financialChange.isPositiveOrZero()) {
            finances.credit(TransactionType.WEALTH, today, financialChange, transactionMessage);
        } else {
            finances.debit(TransactionType.WEALTH, today, financialChange, transactionMessage);
        }

        campaignReport = getFormattedTextAt(
              RESOURCE_BUNDLE,
              reportMessageKey,
              personTitle,
              spanOpeningWithCustomColor(reportColor),
              extraIncome.traitLevel,
              CLOSING_SPAN_TAG,
              financialChange.toAmountString()
        );

        return campaignReport;
    }
}
