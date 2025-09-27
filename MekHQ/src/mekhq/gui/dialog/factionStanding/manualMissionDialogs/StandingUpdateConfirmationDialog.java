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
package mekhq.gui.dialog.factionStanding.manualMissionDialogs;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Displays a modal dialog to confirm the results of a {@link SimulateMissionDialog} or {@link ManualMissionDialog}.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class StandingUpdateConfirmationDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(300);

    private ImageIcon campaignIcon;
    private final boolean updateWasSuccess;

    /**
     * Constructs a SimulatedMissionConfirmationDialog.
     *
     * @param campaignIcon     {@link ImageIcon} to represent the campaign in the dialog.
     * @param updateWasSuccess {@code true} if the simulated mission was successful, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public StandingUpdateConfirmationDialog(JDialog parent, ImageIcon campaignIcon, boolean updateWasSuccess) {
        this.campaignIcon = campaignIcon;
        this.updateWasSuccess = updateWasSuccess;

        populateDialog();
        initializeDialog(parent);
    }

    /**
     * Initializes general properties of the dialog, such as the title, modality, screen location, close operation, and
     * visibility.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void initializeDialog(JDialog parent) {
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
     * Builds the main layout of the dialog, including left (icon) and center (text, button) panels, and adds them to
     * the dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, 0, PADDING, 0);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for campaign icon
        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(0, PADDING, 0, 0));
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
     * Builds the left-side GUI component containing the campaign icon.
     *
     * @return {@link JPanel} containing the scaled campaign icon.
     *
     * @author Illiani
     * @since 0.50.07
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
     * Builds the center panel containing informational message and control buttons.
     *
     * @return {@link JPanel} with message and button components.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateCenterPanel() {
        JPanel pnlParent = new JPanel();
        pnlParent.setLayout(new BoxLayout(pnlParent, BoxLayout.Y_AXIS));

        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlInstructions = populateInstructionsPanel();
        pnlParent.add(pnlInstructions);

        JPanel pnlButton = populateButtonPanel();
        pnlButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlParent.add(pnlButton);

        return pnlParent;
    }

    /**
     * Builds the panel displaying the confirmation message to the user.
     *
     * @return {@link JPanel} displaying the success or failure message in a styled editor pane.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateInstructionsPanel() {
        JPanel pnlInstructions = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        String instructionsKey = updateWasSuccess ?
                                       "simulateContractDialog.confirmation.success" :
                                       "simulateContractDialog.confirmation.failure";
        String instructions = getTextAt(RESOURCE_BUNDLE, instructionsKey);

        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, instructions));
        setFontScaling(editorPane, false, 1.1);

        pnlInstructions.add(editorPane);

        return pnlInstructions;
    }

    /**
     * Builds the panel containing the confirmation (OK) button.
     *
     * <p>When pressed, the dialog is closed.</p>
     *
     * @return {@link JPanel} containing the confirm button.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateButtonPanel() {
        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        String label = getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(label);
        btnConfirm.addActionListener(evt -> dispose());

        pnlButton.add(btnConfirm);

        return pnlButton;
    }
}
