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
package mekhq.gui.dialog.factionStanding.gmToolsDialog;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFactionName;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
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
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;

/**
 * A confirmation dialog for {@link GMTools} actions in MekHQ's Faction Standing system.
 *
 * <p>This dialog presents a warning-styled confirmation message to the user before performing major changes (such as
 * resetting regard or updating contracts), with context-aware descriptions and visual cues.</p>
 *
 * <p>It also supports glossary hyperlink actions within its HTML message text.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class GMToolsConfirmationDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final FactionStandingsGMToolsActionType actionType;
    private final Faction selectedFaction;
    private final int currentGameYear;
    private boolean actionWasConfirmed = false;

    /**
     * Constructs a new confirmation dialog for a GMTools action.
     *
     * @param parent          parent dialog for positioning
     * @param campaignIcon    image icon for the campaign or faction
     * @param actionType      the action type being confirmed
     * @param selectedFaction an optional {@link Faction} object used to tailor the dialog
     * @param currentGameYear the current game year
     *
     * @author Illiani
     * @since 0.50.07
     */
    public GMToolsConfirmationDialog(JDialog parent, ImageIcon campaignIcon,
          FactionStandingsGMToolsActionType actionType, @Nullable Faction selectedFaction, int currentGameYear) {
        this.campaignIcon = campaignIcon;
        this.actionType = actionType;
        this.selectedFaction = selectedFaction;
        this.currentGameYear = currentGameYear;

        populateDialog();
        initializeDialog(parent);
    }

    /**
     * Returns whether the user confirmed the action.
     *
     * @return {@code true} if the action was confirmed and should proceed; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasActionConfirmed() {
        return actionWasConfirmed;
    }

    /**
     * Initializes dialog properties, such as title, close operation, modality, size, and visibility, and positions it
     * relative to the parent.
     *
     * @param parent the parent dialog for centering and modality
     *
     * @author Illiani
     * @since 0.50.07
     */
    void initializeDialog(JDialog parent) {
        setTitle(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    /**
     * Assembles and sets the dialog's main content panel, including the icon panel and the confirmation message/actions
     * panel.
     *
     * @author Illiani
     * @since 0.50.07
     */
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

    /**
     * Builds the left-side panel containing the (scaled) campaign or faction icon.
     *
     * @return the constructed left panel with icon
     */
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

    /**
     * Builds the center panel containing the confirmation message, cancel,  and confirm buttons. The panel includes a
     * styled HTML message describing the consequences of the action.
     *
     * @return the constructed main center panel
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateCenterPanel() {
        JPanel pnlCenter = new JPanel();
        pnlCenter.setBorder(RoundedLineBorder.createRoundedLineBorder());
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String description = getFormattedTextAt(RESOURCE_BUNDLE, "gmTools." + actionType.name() + ".confirmation",
              spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG, getFactionName(selectedFaction,
                    currentGameYear));
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        pnlCenter.add(editorPane);

        RoundedJButton btnCancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.button.cancel"));
        btnCancel.addActionListener(evt -> dispose());

        RoundedJButton btnConfirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "gmTools.confirmation.button.confirm"));
        btnConfirm.addActionListener(evt -> {
            actionWasConfirmed = true;
            dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(btnCancel);

        buttonPanel.add(Box.createRigidArea(new Dimension(PADDING, 0)));

        buttonPanel.add(btnConfirm);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(buttonPanel);

        return pnlCenter;
    }

    /**
     * Handles actions to perform when a hyperlink event occurs in the dialog, such as glossary lookups.
     *
     * @param evt the {@link HyperlinkEvent} that was received.
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            NewGlossaryDialog.handleGlossaryHyperlinkClick(this, evt);
        }
    }
}
