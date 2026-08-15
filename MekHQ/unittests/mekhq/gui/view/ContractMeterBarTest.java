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
package mekhq.gui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.BorderLayout;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;

class ContractMeterBarTest {
    private static final int REQUIRED_SCORE = 5;

    @ParameterizedTest
    @CsvSource({ "4, false", "5, true", "8, true" })
    void contractWithOutstandingObjectivesDistinguishesTargetFromEarlyVictory(final int currentScore,
          final boolean targetReached) {
        final ContractMeterBar meter = ContractMeterBar.victoryPoints(currentScore, REQUIRED_SCORE,
              campaignState(true, false));
        final String expectedTitle;
        final String expectedState;

        if (targetReached) {
            expectedTitle = "<html><center><b>Victory Points</b><br>"
                  + "<b>(target reached, objectives outstanding)</b></center></html>";
            expectedState = "Victory Point target reached, but early victory is not yet secured. At "
                  + "least one strategic objective must exist, and every objective must "
                  + "be resolved (completed or failed).";
        } else {
            expectedTitle = "<html><nobr><b>Victory Points</b></nobr></html>";
            expectedState = "Early victory requires the Victory Point target to be reached, at least "
                  + "one strategic objective to exist, and every objective to be "
                  + "resolved (completed or failed).";
        }

        assertEquals(expectedTitle, titleText(meter));
        assertEquals(scorePrefix(currentScore) + expectedState, tooltipText(meter));
    }

    @ParameterizedTest
    @CsvSource({ "4, false", "5, true", "8, true" })
    void contractWithSatisfiedObjectivesDistinguishesTargetFromSecuredEarlyVictory(final int currentScore,
          final boolean targetReached) {
        final ContractMeterBar meter = ContractMeterBar.victoryPoints(currentScore, REQUIRED_SCORE,
              campaignState(true, true));
        final String expectedTitle;
        final String expectedState;

        if (targetReached) {
            expectedTitle = "<html><nobr><b>Victory Points</b> "
                  + "<b>(early victory secured)</b></nobr></html>";
            expectedState = "Victory Point target reached. At least one strategic objective exists "
                  + "and every objective is resolved (completed or failed). Early "
                  + "victory is secured; daily processing will schedule the contract "
                  + "end automatically.";
        } else {
            expectedTitle = "<html><nobr><b>Victory Points</b></nobr></html>";
            expectedState = "At least one strategic objective exists and every objective is resolved "
                  + "(completed or failed). Reach the Victory Point target to secure "
                  + "early victory; daily processing will schedule the contract end "
                  + "automatically.";
        }

        assertEquals(expectedTitle, titleText(meter));
        assertEquals(scorePrefix(currentScore) + expectedState, tooltipText(meter));
    }

    @ParameterizedTest
    @CsvSource({ "4, false", "5, true", "8, true" })
    void fullTermContractDistinguishesTargetFromFinalOutcome(final int currentScore,
          final boolean targetReached) {
        final ContractMeterBar meter = ContractMeterBar.victoryPoints(currentScore, REQUIRED_SCORE,
              campaignState(false, false));
        final String expectedTitle;
        final String expectedState;

        if (targetReached) {
            expectedTitle = "<html><nobr><b>Victory Points</b> "
                  + "<b>(target reached, full term)</b></nobr></html>";
            expectedState = "Victory Point target reached. This contract must run its full term; "
                  + "Victory Points still count toward its final outcome.";
        } else {
            expectedTitle = "<html><nobr><b>Victory Points</b> <b>(full term)</b></nobr></html>";
            expectedState = "This contract cannot be ended early, but Victory Points still count "
                  + "toward its final outcome.";
        }

        assertEquals(expectedTitle, titleText(meter));
        assertEquals(scorePrefix(currentScore) + expectedState, tooltipText(meter));
    }

    @Test
    void missingCampaignStateUsesConservativeNonSecuredStatus() {
        final ContractMeterBar meter = ContractMeterBar.victoryPoints(REQUIRED_SCORE, REQUIRED_SCORE, null);
        final String expectedTitle = "<html><center><b>Victory Points</b><br>"
              + "<b>(target reached, objectives outstanding)</b></center></html>";
        final String expectedState = "Victory Point target reached, but early victory is not yet secured. At least one "
              + "strategic objective must exist, and every objective must be resolved (completed or failed).";

        assertEquals(expectedTitle, titleText(meter));
        assertEquals(scorePrefix(REQUIRED_SCORE) + expectedState, tooltipText(meter));
    }

    private static StratConCampaignState campaignState(final boolean allowEarlyVictory,
          final boolean canEndContractEarly) {
        final StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.allowEarlyVictory()).thenReturn(allowEarlyVictory);
        when(campaignState.canEndContractEarly()).thenReturn(canEndContractEarly);
        return campaignState;
    }

    private static String scorePrefix(final int currentScore) {
        return "Contract Victory Points: " + currentScore + " of " + REQUIRED_SCORE + ". ";
    }

    private static String titleText(final ContractMeterBar meter) {
        final BorderLayout layout = (BorderLayout) meter.getLayout();
        return ((JLabel) layout.getLayoutComponent(BorderLayout.NORTH)).getText();
    }

    private static String tooltipText(final ContractMeterBar meter) {
        return meter.getToolTipText().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
