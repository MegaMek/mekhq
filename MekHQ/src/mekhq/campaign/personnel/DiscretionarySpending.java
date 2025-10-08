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

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.finances.enums.TransactionType.WEALTH;
import static mekhq.campaign.personnel.Person.MINIMUM_WEALTH;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.Map;

import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.utilities.ReportingUtilities;

/**
 * The {@code DiscretionarySpending} class manages the simulation of discretionary spending for a person based on their
 * wealth and spending limits defined by wealth levels.
 *
 * <p>It calculates total spending for major, moderate, and minor purchases based on dice rolls,
 * spending rules, and wealth modifiers.</p>
 *
 * <p>The rules in this class are based on those found in the ATOW: Companion, pages 53-54. Some liberties were
 * taken. We don't model increasing or degrading Wealth (for an explanation as to why, see the comments in
 * {@link #calculateSpending(int, int, int)}). Furthermore, the player is always assumed to be spending up to their
 * monthly limit. Any funds accumulated in this fashion are 'reinvested' back into the campaign, which is why only the
 * campaign commander uses their Wealth in this manner.</p>
 */
public class DiscretionarySpending {
    final private static String RESOURCE_BUNDLE = "mekhq.resources.DiscretionarySpending";

    /**
     * The maximum number of major purchases a person can make in a single discretionary spending calculation.
     */
    private static final int MAXIMUM_MAJOR_PURCHASES = 1;

    /**
     * The multiplier for calculating the number of moderate purchases. This is derived from the person's wealth.
     */
    private static final int MODERATE_PURCHASES_MULTIPLIER = 1;

    /**
     * The multiplier for calculating the number of minor purchases. This is derived from the person's wealth.
     */
    private static final int MINOR_PURCHASES_MULTIPLIER = 3;

    /**
     * The base target number for wealth checks. This value is used as the foundation before applying modifiers for
     * major, moderate, or minor purchases.
     */
    private static final int WEALTH_CHECK_TARGET_NUMBER = 12;

    /**
     * The modifier applied to the wealth check target number for major purchases. This represents the difficulty or
     * ease of passing the check.
     */
    private static final int WEALTH_CHECK_MAJOR_MODIFIER = -6;

    /**
     * The modifier applied to the wealth check target number for moderate purchases. This represents the difficulty or
     * ease of passing the check.
     */
    private static final int WEALTH_CHECK_MODERATE_MODIFIER = -4;

    /**
     * The modifier applied to the wealth check target number for minor purchases. This represents the difficulty or
     * ease of passing the check.
     */
    private static final int WEALTH_CHECK_MINOR_MODIFIER = -2;

    /**
     * A mapping of wealth levels to the spending limits for major, moderate, and minor purchases.
     *
     * <p><b>Source:</b> ATOW: Companion, pg 54</p>
     */
    private static final Map<Integer, SpendingLimits> discretionarySpendingTable = Map.ofEntries(Map.entry(-1,
                new SpendingLimits(21, 9, 4)),
          Map.entry(0, new SpendingLimits(200, 80, 33)),
          Map.entry(1, new SpendingLimits(469, 188, 75)),
          Map.entry(2, new SpendingLimits(875, 350, 138)),
          Map.entry(3, new SpendingLimits(1625, 600, 250)),
          Map.entry(4, new SpendingLimits(3750, 1500, 563)),
          Map.entry(5, new SpendingLimits(6875, 2750, 1000)),
          Map.entry(6, new SpendingLimits(12500, 5000, 1750)),
          Map.entry(7, new SpendingLimits(28125, 11250, 3750)),
          Map.entry(8, new SpendingLimits(50000, 20000, 6250)),
          Map.entry(9, new SpendingLimits(87500, 35000, 10000)),
          Map.entry(10, new SpendingLimits(150000, 60000, 15000)));

    private String reportMessage = "";

    /**
     * Constructs a {@code DiscretionarySpending} instance for the given {@code Person} and calculates their
     * discretionary spending based on their wealth level.
     *
     * @param person   The person performing discretionary spending, used to determine wealth.
     * @param finances The finances object managing transactions.
     * @param today    The date of the transaction.
     */
    public DiscretionarySpending(Person person, Finances finances, LocalDate today) {
        final String fullTitle = person.getHyperlinkedFullTitle();
        if (person.isHasPerformedExtremeExpenditure()) {
            final String openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());
            reportMessage = getFormattedTextAt(RESOURCE_BUNDLE, "report.format.exhausted",
                  fullTitle,
                  openingSpan,
                  CLOSING_SPAN_TAG);

            return;
        }

        int totalSpending = 0;
        final int wealth = person.getWealth();
        final SpendingLimits spendingLimits = discretionarySpendingTable.get(wealth);

        // Calculate total spending for major, moderate, and minor purchases
        totalSpending += calculateSpending(spendingLimits.major(),
              MAXIMUM_MAJOR_PURCHASES,
              WEALTH_CHECK_MAJOR_MODIFIER);
        totalSpending += calculateSpending(spendingLimits.moderate(),
              wealth * MODERATE_PURCHASES_MULTIPLIER,
              WEALTH_CHECK_MODERATE_MODIFIER);
        totalSpending += calculateSpending(spendingLimits.minor(),
              wealth * MINOR_PURCHASES_MULTIPLIER,
              WEALTH_CHECK_MINOR_MODIFIER);

        Money money = Money.of(totalSpending);

        // Generate the report message
        final String fullName = person.getFullName();
        final String openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());
        if (money.isZero()) {
            reportMessage = getFormattedTextAt(RESOURCE_BUNDLE, "report.format.no_spending", fullTitle);
        } else {
            reportMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  "report.format.monthly",
                  fullTitle,
                  openingSpan,
                  wealth,
                  CLOSING_SPAN_TAG,
                  money.toAmountString());

            // Credit finances with the calculated total spending
            String reason = getFormattedTextAt(RESOURCE_BUNDLE, "finance.format", fullName);
            finances.credit(WEALTH, today, money, reason);
        }
    }

    /**
     * Calculates the total spending for a specified type of purchase (major, moderate, or minor).
     *
     * @param spendingLimit  The spending limit for this type of purchase.
     * @param purchaseCount  The number of purchases to consider for this type.
     * @param wealthModifier The wealth modifier applied to the target number.
     *
     * @return The total spending for this type of purchase.
     */
    private int calculateSpending(int spendingLimit, int purchaseCount, int wealthModifier) {
        int total = 0;
        for (int purchase = 0; purchase < purchaseCount; purchase++) {
            int targetNumber = WEALTH_CHECK_TARGET_NUMBER + wealthModifier;

            // ATOW Companion states that on a fumble, the character's Wealth decreases by 1. As we're not giving the
            // player a choice whether they perform discretionary spending, we're not going to implement that rule.
            // Similarly, we're not going to implement the rule that passing the check by 6 margins of success
            // increases the character's Wealth score - for the same reason. If we implemented those rules Wealth
            // would quickly spike to either extreme end of the spectrum.
            if (d6(2) >= targetNumber) {
                total += spendingLimit;
            }
        }
        return total;
    }

    /**
     * Retrieves the report message summarizing the discretionary spending.
     *
     * @return The report message.
     */
    public String getReportMessage() {
        return reportMessage;
    }

    /**
     * Performs discretionary spending for a given {@code Person} and records the transactions, then returns the
     * generated report message.
     *
     * @param person   The person performing discretionary spending.
     * @param finances The finances object managing transactions.
     * @param today    The date of the transaction.
     *
     * @return A formatted report message summarizing the discretionary spending.
     */
    public static String performDiscretionarySpending(Person person, Finances finances, LocalDate today) {
        DiscretionarySpending spending = new DiscretionarySpending(person, finances, today);
        return spending.getReportMessage();
    }

    /**
     * Simulates and processes an extreme expenditure event for the given person.
     *
     * <p>During an extreme expenditure, the person's wealth decreases by one point. The spending is based on their
     * current wealth level, major spending limits, and their willpower attribute. The result is a formatted report
     * summarizing the transaction and its impact on the person's finances.</p>
     *
     * <p><b>Implementation:</b> according to ATOW: Companion, if a player does this, they cannot make any other
     * discretionary purchases for the rest of the month. </p>
     *
     * @param person   The person performing the extreme expenditure, whose wealth and attributes are used in
     *                 calculations.
     * @param finances The finances object that records the monetary transaction for the extreme expenditure.
     * @param today    The date the expenditure takes place.
     *
     * @return A formatted string summarizing the extreme expenditure report. Returns an empty string if the person has
     *       the minimum wealth level.
     */
    public static String performExtremeExpenditure(Person person, Finances finances, LocalDate today) {
        final int wealth = person.getWealth();
        if (wealth == MINIMUM_WEALTH) {
            return "";
        }

        person.changeWealth(-1);
        person.setHasPerformedExtremeExpenditure(true);

        int totalSpending = getExpenditure(person.getAttributeScore(WILLPOWER), wealth);

        Money money = Money.of(totalSpending);

        // Generate the report message
        final String fullTitle = person.getHyperlinkedFullTitle();
        final String fullName = person.getFullName();
        final String givenName = person.getGivenName();
        final String openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
        String reportMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "report.format.extreme",
              fullTitle,
              openingSpan,
              CLOSING_SPAN_TAG,
              money.toAmountString(),
              givenName,
              wealth - 1);

        // Credit finances with the calculated total spending
        String reason = getFormattedTextAt(RESOURCE_BUNDLE, "finance.format", fullName);
        finances.credit(WEALTH, today, money, reason);

        return reportMessage;
    }

    /**
     * Calculates the expenditure based on a person's willpower and wealth level.
     *
     * <p>Expenditure is determined by multiplying the person's willpower attribute by the major spending limit
     * associated with their current wealth level.</p>
     *
     * @param willpower The person's willpower attribute, which influences the total expenditure.
     * @param wealth    The person's current wealth level, used to determine the major spending limit.
     *
     * @return The total expenditure calculated as the product of the major spending limit and the willpower.
     */
    public static int getExpenditure(int willpower, int wealth) {
        final SpendingLimits spendingLimits = discretionarySpendingTable.get(wealth);
        final int major = spendingLimits.major();

        return major * willpower;
    }

    /**
     * Generates a report message indicating that expenditure has been exhausted for a given person.
     *
     * <p>The message is retrieved from a resource bundle and formatted with the given hyperlinked full title of the
     * person.</p>
     *
     * @param hyperlinkedFullTitle The full title of the person, formatted as a hyperlink, to be included in the report
     *                             message.
     *
     * @return A formatted report message indicating that expenditure has been exhausted.
     */
    public static String getExpenditureExhaustedReportMessage(String hyperlinkedFullTitle) {
        final String openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "report.format.exhausted",
              hyperlinkedFullTitle,
              openingSpan,
              CLOSING_SPAN_TAG);
    }

    /**
     * A record that defines spending limits for different types of purchases.
     *
     * @param major    The spending limit for major purchases.
     * @param moderate The spending limit for moderate purchases.
     * @param minor    The spending limit for minor purchases.
     */
    public record SpendingLimits(int major, int moderate, int minor) {
    }
}
