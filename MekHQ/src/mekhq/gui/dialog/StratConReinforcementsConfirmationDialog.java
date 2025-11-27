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
package mekhq.gui.dialog;

import static java.lang.Math.floor;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.enums.PersonnelRole.INTELLIGENCE_ANALYST;
import static mekhq.campaign.personnel.enums.PersonnelRole.MILITARY_ANALYST;
import static mekhq.campaign.personnel.enums.PersonnelRole.MILITARY_THEORIST;
import static mekhq.campaign.personnel.enums.PersonnelRole.TACTICAL_ANALYST;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.common.TargetRollModifier;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * {@link StratConReinforcementsConfirmationDialog} displays a confirmation dialog for the player to deploy
 * reinforcements to StratCon scenarios. It allows the player to select the number of Support Points to spend and
 * handles the calculation of target number difficulties and summary breakdowns.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class StratConReinforcementsConfirmationDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConReinforcementsConfirmationDialog";

    /** The modifier applied per support point spent. */
    private final static int SUPPORT_POINTS_MODIFIER = -2;

    /** Indicates if a tactical officer is available for dialog flavor. */
    private boolean hasTacticalOfficer = false;
    /** The number of support points selected by the user. */
    private int supportPoints = 0;
    /** The result value for the dialog (type of action chosen). */
    private final ReinforcementDialogResponseType reinforcementDialogResponseType;
    /** Spinner component for selecting support points. */
    private JSpinner spnSupportPoints;
    /** Label for target number breakdown. */
    private JLabel lblBreakdown;

    /**
     * Types of responses that can be selected from the dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public enum ReinforcementDialogResponseType {
        /** The dialog was canceled. */
        CANCEL,
        /** Standard request for reinforcements. */
        REINFORCE,
        /** Instant request for reinforcements. */
        REINFORCE_INSTANTLY,
        /** GM reinforcements. */
        REINFORCE_GM,
        /** Instant GM reinforcements. */
        REINFORCE_GM_INSTANTLY
    }

    /**
     * Gets the response type selected by the user.
     *
     * @return the {@link ReinforcementDialogResponseType} chosen
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ReinforcementDialogResponseType getResponseType() {
        return reinforcementDialogResponseType;
    }

    /**
     * Gets the number of support points selected by the player.
     *
     * @return the number of support points chosen
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSupportPoints() {
        return supportPoints;
    }

    /**
     * Constructs and displays the StratCon reinforcements confirmation dialog.
     *
     * @param campaign             the current campaign context
     * @param targetNumber         the base {@link TargetRoll} representing the reaction difficulty
     * @param maximumSupportPoints the maximum number of support points that can be spent
     * @param costMultiplier       the multiplier applied to all support point costs
     *
     * @author Illiani
     * @since 0.50.07
     */
    public StratConReinforcementsConfirmationDialog(Campaign campaign, TargetRoll targetNumber,
          int maximumSupportPoints, int costMultiplier) {
        final String commanderAddress = campaign.getCommanderAddress();

        // Base reinforcement cost is equal to costMultiplier (which will always be at least 1)
        boolean canReinforce = costMultiplier <= maximumSupportPoints;
        int instantReinforcementCost = costMultiplier * 2;
        boolean canInstantReinforce = instantReinforcementCost <= maximumSupportPoints;

        ImmersiveDialogCore dialog = new ImmersiveDialogCore(campaign,
              getSpeaker(campaign),
              null,
              getInCharacterMessage(commanderAddress, canReinforce),
              getButtons(campaign.isGM(), canReinforce, canInstantReinforce),
              getOutOfCharacterMessage(),
              null,
              false,
              canReinforce ? getSpinnerPanel(maximumSupportPoints, targetNumber, costMultiplier) : null,
              null,
              true);

        reinforcementDialogResponseType = switch (dialog.getDialogChoice()) {
            case 0 -> ReinforcementDialogResponseType.CANCEL;
            case 1 -> ReinforcementDialogResponseType.REINFORCE;
            case 2 -> ReinforcementDialogResponseType.REINFORCE_INSTANTLY;
            case 3 -> ReinforcementDialogResponseType.REINFORCE_GM;
            case 4 -> ReinforcementDialogResponseType.REINFORCE_GM_INSTANTLY;
            default -> throw new IllegalStateException("Unexpected dialog choice value: "
                                                             +
                                                             dialog.getDialogChoice()
                                                             +
                                                             ". Valid choices are 0-4 (or 0-2 for non-GM users). This may occur if an invalid dialog choice is returned.");
        };
    }

    /**
     * Determines the appropriate speaker (either a tactical officer or command admin) to display as the source of the
     * dialog's in-character text.
     *
     * @param campaign the current campaign
     *
     * @return the selected speaker {@link Person}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Person getSpeaker(Campaign campaign) {
        final List<PersonnelRole> TACTICAL_PROFESSIONS = List.of(MILITARY_ANALYST, MILITARY_THEORIST,
              TACTICAL_ANALYST, INTELLIGENCE_ANALYST);
        Person speaker = null;
        for (Person potentialSpeaker : campaign.getActivePersonnel(false, false)) {
            if (!potentialSpeaker.isEmployed()) {
                continue;
            }

            if (!TACTICAL_PROFESSIONS.contains(potentialSpeaker.getPrimaryRole())
                      && !TACTICAL_PROFESSIONS.contains(potentialSpeaker.getSecondaryRole())) {
                continue;
            }

            if (speaker == null) {
                speaker = potentialSpeaker;
                continue;
            }

            if (potentialSpeaker.outRanksUsingSkillTiebreaker(campaign, speaker)) {
                speaker = potentialSpeaker;
            }
        }

        if (speaker != null) {
            hasTacticalOfficer = true;
            return speaker;
        }

        return campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
    }

    /**
     * Generates the in-character message to use in the dialog, based on the available speaker type.
     *
     * @param commanderAddress the address or title of the commander
     * @param canReinforce     {@code true} if the campaign has sufficient Support Points to make the attempt.
     *
     * @return the formatted, localized in-character message
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterMessage(String commanderAddress, boolean canReinforce) {
        final String key;
        if (canReinforce) {
            key = "StratConReinforcementsConfirmationDialog.inCharacter." +
                        (hasTacticalOfficer ? "tactical" : "transport");
        } else {
            key = "StratConReinforcementsConfirmationDialog.inCharacter.cannotReinforce";
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }

    /**
     * Builds the list of button definitions for the dialog, including any GM-specific options.
     *
     * @param isGM                {@code true} if the player is a GM, showing more buttons
     * @param canReinforce        {@code true} if the player has enough Support Points to reinforce
     * @param canInstantReinforce {@code true} if the player has enough Support Points to instantly reinforce
     *
     * @return list of button/tooltip pairs to show in the dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons(boolean isGM, boolean canReinforce,
          boolean canInstantReinforce) {
        List<ImmersiveDialogCore.ButtonLabelTooltipPair> buttons = new ArrayList<>();

        String label = getTextAt(RESOURCE_BUNDLE,
              "StratConReinforcementsConfirmationDialog.button.cancel");
        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));

        if (canReinforce) {
            label = getTextAt(RESOURCE_BUNDLE,
                  "StratConReinforcementsConfirmationDialog.button.reinforce");
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));
        }

        if (canInstantReinforce) {
            label = getTextAt(RESOURCE_BUNDLE,
                  "StratConReinforcementsConfirmationDialog.button.reinforce.instantly");
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));
        }

        if (isGM) {
            label = getTextAt(RESOURCE_BUNDLE,
                  "StratConReinforcementsConfirmationDialog.button.reinforce.gm");
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));

            label = getTextAt(RESOURCE_BUNDLE,
                  "StratConReinforcementsConfirmationDialog.button.reinforce.gm.instantly");
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));
        }

        return buttons;
    }

    /**
     * Generates the out-of-character message for the dialog, describing the mechanics.
     *
     * @return a localized out-of-character description string
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getOutOfCharacterMessage() {
        final String key = "StratConReinforcementsConfirmationDialog.outOfCharacter."
                                 + (hasTacticalOfficer ? "tactical" : "transport");
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * Builds the panel containing the spinner for support point selection and a breakdown of the resulting target
     * number calculation.
     *
     * @param maximumSupportPoints the max allowed support points
     * @param targetNumber         the base target number (before modifiers)
     * @param costMultiplier       the multiplier applied to all Support Point costs
     *
     * @return a {@link JPanel} for embedding in the main dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel getSpinnerPanel(double maximumSupportPoints, TargetRoll targetNumber, int costMultiplier) {
        final int PADDING = scaleForGUI(10);
        int maximum = (int) floor((maximumSupportPoints - costMultiplier) / costMultiplier);

        lblBreakdown = new JLabel("<html>" + getTargetNumberBreakdown(targetNumber, costMultiplier) + "</html>");

        JLabel lblSupportPoints = new JLabel(getTextAt(RESOURCE_BUNDLE,
              "StratConReinforcementsConfirmationDialog.inCharacter.supportPoints"));
        spnSupportPoints = new JSpinner(new SpinnerNumberModel(0, 0, maximum, costMultiplier));
        spnSupportPoints.addChangeListener(e -> {
            supportPoints = (int) spnSupportPoints.getValue();
            lblBreakdown.setText("<html>" + getTargetNumberBreakdown(targetNumber, costMultiplier) + "</html>");
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // The label takes up two spaces horizontally
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(PADDING, 0, PADDING, 0);
        panel.add(lblBreakdown, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(lblSupportPoints, gbc);

        gbc.gridx = 1;
        panel.add(spnSupportPoints, gbc);

        return panel;
    }

    /**
     * Produces a formatted HTML breakdown of the target number calculation, including all relevant modifiers and the
     * effect of current support point selection.
     *
     * @param targetNumber   base target roll object
     * @param costMultiplier the multiplier applied to all Support Point costs
     *
     * @return breakdown string in HTML format
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getTargetNumberBreakdown(TargetRoll targetNumber, int costMultiplier) {
        StringBuilder breakdown = new StringBuilder();
        for (TargetRollModifier modifier : targetNumber.getModifiers()) {
            breakdown.append("<br><b>").append(modifier.getDesc()).append(":</b> ").append(modifier.value());
        }

        int modifier = costMultiplier == 0 ? 0 : (supportPoints * SUPPORT_POINTS_MODIFIER) / costMultiplier;

        breakdown.append("<br><b>")
              .append(getTextAt(RESOURCE_BUNDLE,
                    "StratConReinforcementsConfirmationDialog.inCharacter.supportPoints"))
              .append(" </b> ")
              .append(modifier);

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConReinforcementsConfirmationDialog.inCharacter.total",
              targetNumber.getValue() + modifier));

        return breakdown.toString();
    }

}
