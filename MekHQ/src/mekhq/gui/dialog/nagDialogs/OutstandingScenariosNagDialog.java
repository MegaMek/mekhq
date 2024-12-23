/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

/**
 * This class represents a nag dialog displayed when the campaign one or more unresolved scenarios.
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class OutstandingScenariosNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "OutstandingScenariosNagDialog";
    private static String DIALOG_TITLE = "OutstandingScenariosNagDialog.title";
    private static String DIALOG_BODY = "OutstandingScenariosNagDialog.text";

    /**
     * Checks if there are any outstanding scenarios in the given campaign.
     * An outstanding scenario is defined as a scenario whose date is the same as the current date.
     *
     * @param campaign the campaign to check for outstanding scenarios
     * @return {@code true} if there are outstanding scenarios, {@code false} otherwise
     */
    static boolean checkForOutstandingScenarios(Campaign campaign) {
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts(true);

        LocalDate today = campaign.getLocalDate();

        for (AtBContract contract : activeContracts) {
            for (AtBScenario scenario : contract.getCurrentAtBScenarios()) {
                LocalDate scenarioDate = scenario.getDate();

                if (scenarioDate.equals(today)) {
                    if (scenario.getHasTrack()) {
                        for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                            for (StratconScenario stratconScenario : track.getScenarios().values()) {
                                if (stratconScenario.getBackingScenario().equals(scenario)) {
                                    return stratconScenario.getCurrentState() != ScenarioState.UNRESOLVED;
                                }
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link OutstandingScenariosNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public OutstandingScenariosNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_OUTSTANDING_SCENARIOS);
        pack();
    }
    //endregion Constructors

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && checkForOutstandingScenarios(getCampaign());
    }
}
