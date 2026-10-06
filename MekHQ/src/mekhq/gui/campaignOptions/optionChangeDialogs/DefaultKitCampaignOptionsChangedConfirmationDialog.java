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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.personnel.quartermaster.AbstractKitIssuer.KitIssueTotals;
import mekhq.campaign.personnel.quartermaster.DefaultKitChanges;
import mekhq.campaign.personnel.quartermaster.DefaultKitChanges.Change;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Confirmation dialog shown when one or more default kit options are switched from one real kit to another, offering
 * to move everyone carrying the old default onto the new one. The new kits come from stores; any shortfall is ordered
 * and paid for as usual.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DefaultKitCampaignOptionsChangedConfirmationDialog extends JDialog {
    private static final String RESOURCE_BUNDLE =
          "mekhq.resources.DefaultKitCampaignOptionsChangedConfirmationDialog";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final Campaign campaign;
    private final List<Change> changes;

    /**
     * @param campaign the campaign whose options changed
     * @param changes  the switched defaults; the dialog should only be shown when this is non-empty
     *
     * @author Illiani
     * @since 0.51.01
     */
    public DefaultKitCampaignOptionsChangedConfirmationDialog(Campaign campaign, List<Change> changes) {
        this.campaignIcon = campaign.getCampaignFactionIcon();
        this.campaign = campaign;
        this.changes = changes;

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

        StringBuilder changeList = new StringBuilder();
        for (Change change : changes) {
            String group = (change.profession() != null)
                                 ? getTextAt(RESOURCE_BUNDLE, "profession." + change.profession().name())
                                 : getTextAt(RESOURCE_BUNDLE, "group." + change.category().name());
            changeList.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "DefaultKitCampaignOptionsChangedConfirmationDialog.change",
                  group,
                  change.oldKitDisplayName(),
                  change.newKit().getName()));
        }

        String description = getFormattedTextAt(RESOURCE_BUNDLE,
              "DefaultKitCampaignOptionsChangedConfirmationDialog.description",
              changeList.toString());
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        pnlCenter.add(editorPane);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(createButtonPanel());

        return pnlCenter;
    }

    private JPanel createButtonPanel() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedJButton btnCancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "DefaultKitCampaignOptionsChangedConfirmationDialog.cancel"));
        btnCancel.addActionListener(evt -> dispose());

        RoundedJButton btnConfirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "DefaultKitCampaignOptionsChangedConfirmationDialog.confirm"));
        btnConfirm.addActionListener(evt -> {
            KitIssueTotals totals = DefaultKitChanges.apply(campaign, changes);
            if ((totals.issued > 0) || (totals.ordered > 0)) {
                campaign.addReport(DailyReportType.PERSONNEL,
                      getFormattedTextAt("mekhq.resources.IssueEquipmentDialog", "report.issued",
                            totals.issued + totals.ordered, totals.issued, totals.ordered));
            }
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
            dispose();
        });

        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createRigidArea(new Dimension(PADDING, 0)));
        pnlButtons.add(btnConfirm);

        return pnlButtons;
    }
}
