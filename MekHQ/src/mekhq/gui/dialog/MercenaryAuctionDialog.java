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

import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;

import megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutPanel;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * A dialog for handling mercenary unit auctions in the campaign.
 *
 * <p>This dialog allows players to interact with an immersive bidding system for mercenary units.
 * It provides an in-character message about the auction, an adjustable spinner to set the bid percentage, and buttons
 * to confirm or cancel the auction. Upon confirmation, additional actions like viewing detailed data about the unit
 * (TRO View) are available.</p>
 */
public class MercenaryAuctionDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MercenaryAuctionDialog";

    private Entity entity;

    /**
     * Constructs a new auction dialog for mercenary units in the campaign.
     *
     * <p>The dialog presents an immersive interface for players to bid on mercenary units,
     * including in-character and out-of-character messages, action buttons, and an adjustable spinner for bid
     * percentage control. The dialog operates as a modal window.</p>
     *
     * @param campaign       The {@link Campaign} instance containing information about the auction.
     * @param entity         The {@link Entity} representing the mercenary unit being auctioned.
     * @param minimumBid     The minimum allowable bid percentage.
     * @param maximumBid     The maximum allowable bid percentage.
     * @param percentPerStep The percentage increment for the bid adjustments.
     * @param stepSize       The step size for each spinner adjustment.
     */
    public MercenaryAuctionDialog(Campaign campaign, Entity entity, int minimumBid, int maximumBid, int percentPerStep,
          int stepSize) {
        super(campaign,
              campaign.getSeniorAdminPerson(TRANSPORT),
              null,
              createCenterMessage(campaign, entity.getShortName()),
              createButtons(),
              createOutOfCharacterMessage(minimumBid, maximumBid, percentPerStep),
              null,
              false,
              createJSpinnerPanel(minimumBid, minimumBid, maximumBid, stepSize),
              null,
              false);

        // This setup ensures the dialogs both operate as modal and also assign the entity being
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
     * Creates a center-aligned in-character message for the auction dialog.
     *
     * <p>The message typically informs players about the context of the auction,
     * using details from the campaign and the short name of the entity being auctioned.</p>
     *
     * @param campaign  The {@link Campaign} instance providing context for the message.
     * @param shortName The short name of the entity being auctioned.
     *
     * @return A formatted in-character message for display in the auction dialog.
     */
    private static String createCenterMessage(Campaign campaign, String shortName) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "auction.ic.hasFunds",
              campaign.getCommanderAddress(),
              shortName);
    }

    /**
     * Creates the list of action buttons for the auction dialog.
     *
     * <p>This includes the "Confirm" and "Cancel" buttons, along with their respective labels.</p>
     *
     * @return A {@link List} of {@link ButtonLabelTooltipPair} objects for the dialog.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnCancel = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "cancel.button"), null);
        ButtonLabelTooltipPair btnConfirm = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "confirm.button"), null);

        return List.of(btnCancel, btnConfirm);
    }

    /**
     * Creates an out-of-character message for the auction dialog.
     *
     * <p>This message provides players with information about the auction rules and ranges,
     * including the minimum and maximum bid percentages as well as the bid increment steps.</p>
     *
     * @param minimumBid     The minimum allowable bid percentage.
     * @param maximumBid     The maximum allowable bid percentage.
     * @param percentPerStep The increment step size for bidding percentages.
     *
     * @return A formatted out-of-character message for display in the auction dialog.
     */
    private static String createOutOfCharacterMessage(int minimumBid, int maximumBid, int percentPerStep) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "auction.ooc.hasFunds", minimumBid, maximumBid, percentPerStep);
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
     *
     * @return The JPanel containing the spinner, or {@code null} if the default value is invalid.
     */
    private static @Nullable JPanel createJSpinnerPanel(int defaultValue, int minimumValue, int maximumValue,
          int stepSize) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, minimumValue, maximumValue, stepSize));
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
        EntityReadoutPanel panelTROView = new EntityReadoutPanel();
        panelTROView.showEntity(entity, troView);

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
