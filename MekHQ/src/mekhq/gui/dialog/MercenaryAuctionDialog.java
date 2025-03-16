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
 */
package mekhq.gui.dialog;

import megamek.client.ui.swing.MekViewPanel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.ViewFormatting;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
import java.util.List;

import static java.lang.Math.round;
import static megamek.common.UnitType.getTypeDisplayableName;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * A dialog for handling mercenary unit auctions in the campaign.
 *
 * This dialog allows players to interact with an immersive bidding system for mercenary units.
 * It provides an in-character message about the auction, an adjustable spinner to set the bid percentage,
 * and buttons to confirm or cancel the auction. Upon confirmation, additional actions like viewing
 * detailed data about the unit (TRO View) are available.
 */
public class MercenaryAuctionDialog extends MHQDialogImmersive {

    // Constants
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + MercenaryAuctionDialog.class.getSimpleName();

    private Entity entity;

    /**
     * Constructs a modal {@code MercenaryAuctionDialog} to allow bidding on a specific mercenary unit.
     *
     * The dialog initializes with predefined settings, including the player details, the entity being
     * auctioned, and a spinner for adjusting the bid percentage. The dialog blocks further execution
     * until the player interacts with it and either confirms or cancels their bid.
     *
     * @param campaign     The {@link Campaign} instance, representing the current state of the player's campaign.
     * @param entity       The {@link Entity} being auctioned, containing details about the mercenary unit.
     * @param defaultValue The default percentage value for the bid (e.g., 60).
     * @param minimumValue The minimum percentage value for the bid (e.g., 50).
     * @param maximumValue The maximum percentage value for the bid (e.g., 150).
     * @param stepSize     The step size for the percentage increments in the spinner (e.g., 5).
     */
    public MercenaryAuctionDialog(Campaign campaign, Entity entity, int defaultValue,
                                  int minimumValue, int maximumValue, int stepSize) {
        super(campaign, campaign.getSeniorAdminPerson(TRANSPORT), null,
              createInCharacterMessage(campaign.getCommanderAddress(false), entity),
              createButtons(), createOutOfCharacterMessage(entity.getShortName()), null,
              false, createJSpinnerPanel(defaultValue, minimumValue, maximumValue, stepSize),
              false
        );

        // This setup ensures the dialog both operates as modal and also assigns the entity being
        // auctioned. Just setting it to modal is not enough.
        setVisible(false);
        setEntity(entity);
        setModal(true);
        setVisible(true);
    }

    /**
     * Sets the {@link Entity} for the auction dialog.
     *
     * @param entity The {@link Entity} being set for the dialog.
     */
    private void setEntity(Entity entity) {
        this.entity = entity;
    }

    /**
     * Generates the in-character auction announcement message.
     *
     * <p>This message contains details such as the commanding officer's address, the name, type,
     * and cost range of the unit, making the interaction immersive for the user.</p>
     *
     * @param commanderAddress The email-like address of the commanding officer.
     * @param entity           The {@link Entity} being auctioned.
     * @return A formatted string representing the in-character message.
     */
    private static String createInCharacterMessage(String commanderAddress, Entity entity) {
        String unitName = entity.getShortName();
        int unitTypeCode = entity.getUnitType();
        String unitType = getTypeDisplayableName(unitTypeCode);

        double value = entity.getCost(false);

        return getFormattedTextAt(RESOURCE_BUNDLE, "auction.ic", commanderAddress, unitName,
              unitType, round(value * 0.5), round(value * 0.75), round(value), round(value * 1.25),
              round(value * 1.5));
    }

    /**
     * Creates the list of action buttons for the auction dialog.
     *
     * <p>This includes the "Confirm" and "Cancel" buttons, along with their respective labels.</p>
     *
     * @return A {@link List} of {@link ButtonLabelTooltipPair} objects for the dialog.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirm = new ButtonLabelTooltipPair(
              getFormattedTextAt(RESOURCE_BUNDLE, "confirm.button"), null);
        ButtonLabelTooltipPair btnCancel = new ButtonLabelTooltipPair(
              getFormattedTextAt(RESOURCE_BUNDLE, "cancel.button"), null);

        return List.of(btnConfirm, btnCancel);
    }

    /**
     * Generates the out-of-character auction announcement message, including a hyperlinked unit
     * label.
     *
     * @param shortName The short name of the unit being auctioned.
     * @return A formatted string representing the out-of-character auction details.
     */
    private static String createOutOfCharacterMessage(String shortName) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "auction.ooc", shortName);
    }

    /**
     * Creates a {@link JPanel} containing an adjustable spinner for entering the bid percentage.
     *
     * <p>This panel validates the input values to ensure the default value is within the acceptable
     * range and provides user controls for making percentage adjustments.</p>
     *
     * @param defaultValue The default percentage value in the spinner.
     * @param minimumValue The minimum allowable percentage value.
     * @param maximumValue The maximum allowable percentage value.
     * @param stepSize     The step size for adjusting the percentage.
     * @return The JPanel containing the spinner, or {@code null} if the default value is invalid.
     */
    private static @Nullable JPanel createJSpinnerPanel(int defaultValue, int minimumValue,
                                                        int maximumValue, int stepSize) {
        if ((defaultValue < minimumValue) || (defaultValue > maximumValue)) {
            return null;
        }

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 50, maximumValue, stepSize));
        JLabel label = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "spinner.label.auction"));

        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        spinnerPanel.add(label);
        spinnerPanel.add(spinner);

        return spinnerPanel;
    }

    @Override
    protected void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == EventType.ACTIVATED) {
            showTROView();
        }
    }

    /**
     * Displays a TRO (Technical Readout) View of the auctioned entity in a modal dialog.
     */
    private void showTROView() {
        JDialog dialog = new JDialog();

        // Create the TROView
        TROView troView = TROView.createView(entity, ViewFormatting.HTML);

        // Create the MekViewPanel and associate the entity and TROView
        MekViewPanel panelTROView = new MekViewPanel();
        panelTROView.setMek(entity, troView);

        // Set the MekViewPanel as the dialog's content
        dialog.setContentPane(panelTROView);

        // Configure dialog properties
        dialog.setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "troView.title"));
        dialog.setSize(UIUtil.scaleForGUI(800, 600));
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setVisible(true);
    }
}
