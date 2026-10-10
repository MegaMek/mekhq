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
package mekhq.gui.campaignOptions.optionChangeDialogs;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.SupportCapability;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.VehicleCrewSource;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator.CapabilityShortfall;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Offers, in one place, the support units a campaign's rules call for and it does not yet have.
 *
 * <p>Shown once, straight after a campaign is organized into support teams. A capability's units are otherwise only
 * offered when its option is switched on, and never while a campaign is being set up, so a campaign that had fatigue,
 * StratCon or prisoner capture on from the start was never offered canteens, a convoy or a security detail. The
 * conversion itself only builds the recovery and MASH vehicles its sections can crew.</p>
 *
 * <p>Each capability still short of its target is listed with a box to tick. Ticked ones are granted exactly as the
 * capability's own grant dialog grants them when not crewed from existing staff: new hires, or one named crew member
 * and the temporary crew pool where the player asks for that and the campaign uses temporary crews. Declining grants
 * nothing, and the offer is not repeated; switching a capability's option off and on still opens its own dialog.</p>
 *
 * @since 0.51.01
 */
public class SupportCapabilityShortfallDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SupportCapabilityShortfallDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SupportCapabilityShortfallDialog";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final Campaign campaign;
    private final Faction faction;
    private final List<CapabilityShortfall> shortfalls;
    private final Map<SupportCapability, JCheckBox> capabilityBoxes = new LinkedHashMap<>();
    private JCheckBox chkUseTemporaryCrews;

    /**
     * Offers the missing support units if the campaign is short of any, and does nothing otherwise.
     *
     * @param campaign the campaign just organized into support teams
     */
    public static void offerIfShort(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        List<CapabilityShortfall> shortfalls = SupportUnitGenerator.capabilityShortfalls(campaign, faction);
        if (shortfalls.isEmpty()) {
            LOGGER.info("[SupportTeams] shortfall offer: every switched-on capability is met; nothing offered");
            return;
        }
        LOGGER.info("[SupportTeams] shortfall offer: {} capability(ies) short: {}", shortfalls.size(), shortfalls);
        new SupportCapabilityShortfallDialog(campaign, faction, shortfalls);
    }

    private SupportCapabilityShortfallDialog(Campaign campaign, Faction faction, List<CapabilityShortfall> shortfalls) {
        this.campaignIcon = campaign.getCampaignFactionIcon();
        this.campaign = campaign;
        this.faction = faction;
        this.shortfalls = shortfalls;

        populateDialog();
        initializeDialog();
    }

    private void initializeDialog() {
        setTitle(getText("accessingTerminal.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        mainPanel.add(pnlLeft, constraints);

        JPanel pnlCenter = populateCenterPanel();
        constraints.gridx = 1;
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
        // An HTML pane keeps its own font and colour unless told otherwise, which ignores the GUI scale and the theme.
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(UIManager.getFont("Label.font"));
        editorPane.setForeground(UIManager.getColor("Label.foreground"));
        String description = getTextAt(RESOURCE_BUNDLE, "SupportCapabilityShortfallDialog.description");
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlCenter.add(editorPane);
        pnlCenter.add(Box.createVerticalStrut(PADDING));

        boolean anyUsesTemporaryCrews = false;
        for (CapabilityShortfall shortfall : shortfalls) {
            SupportCapability capability = shortfall.capability();
            JCheckBox checkBox = new JCheckBox(rowText(shortfall));
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBox.setSelected(true);
            capabilityBoxes.put(capability, checkBox);
            pnlCenter.add(checkBox);
            if (SupportUnitGenerator.grantedCrewSource(campaign, capability, true)
                      == VehicleCrewSource.TEMPORARY_CREW) {
                anyUsesTemporaryCrews = true;
            }
        }

        if (anyUsesTemporaryCrews) {
            pnlCenter.add(Box.createVerticalStrut(PADDING));
            chkUseTemporaryCrews = new JCheckBox(getText("supportCapabilityGrant.useTemporaryCrews.text"));
            chkUseTemporaryCrews.setToolTipText(getText("supportCapabilityGrant.useTemporaryCrews.toolTipText"));
            chkUseTemporaryCrews.setAlignmentX(Component.LEFT_ALIGNMENT);
            chkUseTemporaryCrews.setSelected(true);
            pnlCenter.add(chkUseTemporaryCrews);
        }

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        JPanel pnlButtons = createButtonPanel();
        pnlButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlCenter.add(pnlButtons);

        return pnlCenter;
    }

    private JPanel createButtonPanel() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));

        RoundedJButton btnCancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "SupportCapabilityShortfallDialog.cancel"));
        btnCancel.addActionListener(event -> {
            LOGGER.info("[SupportTeams] shortfall offer declined; nothing granted");
            dispose();
        });

        RoundedJButton btnConfirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "SupportCapabilityShortfallDialog.confirm"));
        btnConfirm.addActionListener(event -> {
            grantSelected();
            dispose();
        });

        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createRigidArea(new Dimension(PADDING, 0)));
        pnlButtons.add(btnConfirm);
        pnlButtons.add(Box.createHorizontalGlue());

        return pnlButtons;
    }

    /** Grants the ticked capabilities and says in the daily report which ones were granted. */
    private void grantSelected() {
        boolean wantsTemporaryCrews = (chkUseTemporaryCrews != null) && chkUseTemporaryCrews.isSelected();
        List<SupportCapability> chosen = new ArrayList<>();
        for (Map.Entry<SupportCapability, JCheckBox> entry : capabilityBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                chosen.add(entry.getKey());
            }
        }
        if (chosen.isEmpty()) {
            LOGGER.info("[SupportTeams] shortfall offer accepted with nothing ticked; nothing granted");
            return;
        }
        List<String> labels = new ArrayList<>();
        for (SupportCapability capability : chosen) {
            grant(campaign, faction, capability, wantsTemporaryCrews);
            labels.add(capabilityLabel(capability));
        }
        campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "SupportCapabilityShortfallDialog.report",
              String.join(", ", labels)));
    }

    /**
     * Grants one capability's missing units the way its own grant dialog does when they are not crewed from existing
     * staff.
     *
     * @param campaign            the campaign
     * @param faction             the faction the crews are drawn from
     * @param capability          the capability to grant
     * @param wantsTemporaryCrews whether the player asked for temporary crews
     */
    static void grant(Campaign campaign, Faction faction, SupportCapability capability, boolean wantsTemporaryCrews) {
        VehicleCrewSource crewSource = SupportUnitGenerator.grantedCrewSource(campaign, capability,
              wantsTemporaryCrews);
        LOGGER.info("[SupportTeams] shortfall offer: granting {} crewed as {}", capability, crewSource);
        SupportCapabilityGrantDialog.processFreeUnits(campaign, faction, true, capability, crewSource);
    }

    /** One capability's line: what would be built, what of the player's own would be crewed, or both. */
    private static String rowText(CapabilityShortfall shortfall) {
        String label = capabilityLabel(shortfall.capability());
        String key;
        if (shortfall.idle() == 0) {
            key = "SupportCapabilityShortfallDialog.row.add";
        } else if (shortfall.missing() == 0) {
            key = "SupportCapabilityShortfallDialog.row.crew";
        } else {
            key = "SupportCapabilityShortfallDialog.row.addAndCrew";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, key, label, shortfall.missing(), shortfall.idle(),
              shortfall.owned(), shortfall.target());
    }

    /** The player-facing name of a capability's units, with the rule that calls for them. */
    private static String capabilityLabel(SupportCapability capability) {
        return getTextAt(RESOURCE_BUNDLE, "SupportCapabilityShortfallDialog.capability." + capability.name());
    }
}
