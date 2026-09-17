/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.optionChangeDialogs;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.SupportCapability;
import mekhq.campaign.universe.commandGeneration.SupportCarrierReconciler;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.VehicleCrewSource;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Offers the free support vehicles that come with a capability the player has just switched on mid-campaign.
 *
 * <p>One dialog serves every capability. Which text is shown, and which vehicles are granted on Accept, both come from
 * the {@link SupportCapability} it is given, so a capability is described in one place rather than in a dialog class of
 * its own.</p>
 *
 * <p>Two choices are offered, each only when it applies. A campaign using support teams can crew the new vehicles from
 * the staff it already has, which puts them in the same place generation would have. A campaign using temporary crews
 * for the crew role can take one named crew member per vehicle and fill the rest of the seats from the pool, as its
 * infantry and vehicles are already crewed.</p>
 *
 * @since 0.51.01
 */
public class SupportCapabilityGrantDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SupportCapabilityGrantDialog.class);

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final Campaign campaign;
    private final SupportCapability capability;

    private JCheckBox chkCrewFromExistingStaff;
    private JCheckBox chkUseTemporaryCrews;

    /**
     * Shows the offer for one capability's free vehicles.
     *
     * @param campaign   the campaign the vehicles would be granted to
     * @param capability the capability the player has just switched on
     */
    public SupportCapabilityGrantDialog(Campaign campaign, SupportCapability capability) {
        this.campaignIcon = campaign.getCampaignFactionIcon();
        this.campaign = campaign;
        this.capability = capability;

        populateDialog();
        initializeDialog();
    }

    void initializeDialog() {
        setTitle(getText("accessingTerminal.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for campaign icon
        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 1;
        mainPanel.add(pnlLeft, constraints);
        gridx++;

        // Center box for the message
        JPanel pnlCenter = populateCenterPanel();
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 2;
        constraints.weighty = 2;
        mainPanel.add(pnlCenter, constraints);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel pnlCampaign = new JPanel();
        pnlCampaign.setLayout(new BoxLayout(pnlCampaign, BoxLayout.Y_AXIS));
        pnlCampaign.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCampaign.setMaximumSize(new Dimension(IMAGE_WIDTH, scaleForGUI(MAX_VALUE)));

        campaignIcon = scaleImageIcon(campaignIcon, IMAGE_WIDTH, true);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(campaignIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlCampaign.add(imageLabel);

        return pnlCampaign;
    }

    private JPanel populateCenterPanel() {
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        String description = getFormattedTextAt(capability.resourceBundle(),
              capability.resourceKeyPrefix() + ".description",
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG);
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        pnlCenter.add(editorPane);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        addCrewingChoices(pnlCenter);
        pnlCenter.add(createButtonPanel());

        return pnlCenter;
    }

    /**
     * Adds the crewing choices that apply to this campaign. A choice the campaign cannot make is left out rather than
     * shown disabled, so the dialog stays as short as the decision in front of the player.
     */
    private void addCrewingChoices(JPanel pnlCenter) {
        if (canCrewFromExistingStaff()) {
            chkCrewFromExistingStaff = new JCheckBox(getText("supportCapabilityGrant.crewFromExistingStaff.text"));
            chkCrewFromExistingStaff.setToolTipText(getText("supportCapabilityGrant.crewFromExistingStaff.toolTipText"));
            chkCrewFromExistingStaff.setAlignmentX(Component.LEFT_ALIGNMENT);
            chkCrewFromExistingStaff.setSelected(true);
            pnlCenter.add(chkCrewFromExistingStaff);
        }

        if (usesTemporaryCrews()) {
            chkUseTemporaryCrews = new JCheckBox(getText("supportCapabilityGrant.useTemporaryCrews.text"));
            chkUseTemporaryCrews.setToolTipText(getText("supportCapabilityGrant.useTemporaryCrews.toolTipText"));
            chkUseTemporaryCrews.setAlignmentX(Component.LEFT_ALIGNMENT);
            chkUseTemporaryCrews.setSelected(true);
            pnlCenter.add(chkUseTemporaryCrews);
        }

        if ((chkCrewFromExistingStaff != null) || (chkUseTemporaryCrews != null)) {
            pnlCenter.add(Box.createVerticalStrut(PADDING));
        }
    }

    /** Whether this campaign could seat the new vehicles from staff it already has. */
    private boolean canCrewFromExistingStaff() {
        return (capability.crewSection() != null)
                     && SupportCarrierReconciler.isEnabled(campaign)
                     && (campaign.getPlayerForce().getSupportCommandFormation() != null);
    }

    /** Whether this capability's units are crewed from the temporary crew pool in this campaign. */
    private boolean usesTemporaryCrews() {
        PersonnelRole crewRole = capability.crewRole();
        return (crewRole != null)
                     && campaign.getPlayerForce()
                              .getHumanResources()
                              .isBlobCrewEnabled(crewRole, campaign.getCampaignOptions());
    }

    private JPanel createButtonPanel() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedJButton btnCancel = new RoundedJButton(buttonLabel("cancel"));
        btnCancel.addActionListener(event -> dispose());

        RoundedJButton btnConfirm = new RoundedJButton(buttonLabel("confirm"));
        btnConfirm.addActionListener(event -> {
            grantVehicles();
            dispose();
        });

        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createRigidArea(new Dimension(PADDING, 0)));
        pnlButtons.add(btnConfirm);

        return pnlButtons;
    }

    /** Grants the vehicles the way the player asked for them, and says in the log which way that was. */
    private void grantVehicles() {
        boolean fromExistingStaff = (chkCrewFromExistingStaff != null) && chkCrewFromExistingStaff.isSelected();
        boolean temporaryCrews = (chkUseTemporaryCrews != null) && chkUseTemporaryCrews.isSelected();

        if (fromExistingStaff) {
            int built = SupportPersonnelToTOE.topUpCapabilityVehicles(campaign, capability,
                  VehicleCrewSource.EXISTING_STAFF);
            LOGGER.info("[SupportTeams] {}: granted {} vehicle(s) into the support sections, crewed from existing"
                              + " staff", capability, built);
            return;
        }

        VehicleCrewSource crewSource = temporaryCrews ? VehicleCrewSource.TEMPORARY_CREW : VehicleCrewSource.NEW_CREW;
        LOGGER.info("[SupportTeams] {}: granted standalone, crewed as {}", capability, crewSource);
        processFreeUnits(campaign, campaign.getPlayerForce().getFaction(), true, capability, crewSource);
    }

    /** One capability's button text, from the bundle that capability names. */
    private String buttonLabel(String suffix) {
        return getTextAt(capability.resourceBundle(), capability.resourceKeyPrefix() + '.' + suffix);
    }

    /**
     * Grants the capability's free vehicles, topping the campaign up to what a command its size should field.
     *
     * @param campaign                   the campaign the vehicles are granted to
     * @param faction                    the faction whose ranks the crews are given
     * @param isAutomaticallyAssignRanks whether generated crews have ranks assigned automatically
     * @param capability                 the capability being granted
     * @param crewSource                 the crewing to use, or {@code null} to follow the campaign's temporary crew
     *                                   options
     */
    public static void processFreeUnits(Campaign campaign, Faction faction, boolean isAutomaticallyAssignRanks,
          SupportCapability capability, @Nullable VehicleCrewSource crewSource) {
        SupportUnitGenerator.generate(capability, campaign, faction, isAutomaticallyAssignRanks, crewSource);
    }
}
