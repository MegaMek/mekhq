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
package mekhq.gui.dialog.factionStanding;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import mekhq.campaign.mission.Mission;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;

/**
 * This class is used to update Faction Standings when campaign options change.
 *
 * <p>It presents a dialog with a description and two buttons (Cancel and Confirm) to handle campaign option
 * modifications affecting faction standings.</p>
 *
 * <p>The Confirm button applies the changes, while the Cancel button discards them.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingCampaignOptionsChangedConfirmationDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final Faction campaignFaction;
    private final LocalDate today;
    private final FactionStandings factionStandings;
    private final List<Mission> missions;
    private final boolean isFactionStandingEnabled;
    private final double regardMultiplier;

    private final List<String> reports = new ArrayList<>();

    /**
     * Constructs a new confirmation dialog for changes in campaign options affecting faction standings.
     *
     * <p>This dialog displays a summary message and offers "Cancel" and "Confirm" buttons, allowing users to apply
     * or discard changes to faction standings as a result of campaign option modifications.</p>
     *
     * @param parent                   the parent {@link JDialog} for modality and positioning
     * @param campaignIcon             the icon representing the current campaign, shown in the dialog
     * @param campaignFaction          the faction associated with the campaign, used when updating standings
     * @param today                    the current date used for date-specific updates or reporting
     * @param factionStandings         the object holding and managing all faction standings data
     * @param missions                 the set of missions relevant for recalculating standings on confirmation
     * @param isFactionStandingEnabled {@code true} if faction standings are being enabled; {@code false} if being
     *                                 disabled
     * @param regardMultiplier         the regard multiplier set in campaign options
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingCampaignOptionsChangedConfirmationDialog(JDialog parent, ImageIcon campaignIcon, Faction campaignFaction,
                                                                   LocalDate today, FactionStandings factionStandings, Collection<Mission> missions,
                                                                   boolean isFactionStandingEnabled, double regardMultiplier) {
        this.campaignIcon = campaignIcon;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.factionStandings = factionStandings;
        this.missions = new ArrayList<>(missions);
        this.isFactionStandingEnabled = isFactionStandingEnabled;
        this.regardMultiplier = regardMultiplier;

        populateDialog();
        initializeDialog(parent);
    }

    /**
     * Gets the list of reports describing the faction standing changes performed.
     *
     * @return a list of report strings representing the actions performed
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> getReports() {
        return reports;
    }

    /**
     * Initializes the dialog window with standard properties, such as modality, title, size, close operation, and
     * visibility.
     *
     * @param parent the parent dialog for positioning and modality
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
     * Populates the dialog's main content area using a {@link GridBagLayout}, adding the campaign icon panel and the
     * central message/button panel.
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
     * Builds the left-side panel containing the scaled campaign icon.
     *
     * @return the configured {@link JPanel} containing the icon image
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
     * Populates the central panel of the dialog with a description and two horizontally arranged buttons: a Cancel
     * button and a Confirm button with 10px spacing between them.
     *
     * <p>The Cancel button closes the dialog.</p>
     *
     * <p>The Confirm button updates faction standing data and then closes the dialog.</p>
     *
     * @return a {@link JPanel} instance containing the description and the arranged buttons
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateCenterPanel() {
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String descriptionKeySuffix = isFactionStandingEnabled ? "enabled" : "disabled";
        String description = getFormattedTextAt(RESOURCE_BUNDLE,
              "campaignOptionsChanged.description." + descriptionKeySuffix,
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG);
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        pnlCenter.add(editorPane);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(createButtonPanel());

        return pnlCenter;
    }

    /**
     * Builds and returns the panel containing the Cancel and Confirm buttons, horizontally arranged with 10 pixels of
     * spacing between.
     *
     * @return a {@link JPanel} containing the button row
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createButtonPanel() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedJButton btnCancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.button.cancel"));
        btnCancel.addActionListener(evt -> dispose());

        RoundedJButton btnConfirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "gmTools.confirmation.button.confirm"));
        btnConfirm.addActionListener(evt -> {
            if (isFactionStandingEnabled) {
                reports.add(getTextAt(RESOURCE_BUNDLE, "gmTools.ZERO_ALL_REGARD.report"));
                factionStandings.resetAllFactionStandings();
                factionStandings.updateClimateRegard(campaignFaction, today, regardMultiplier, true);
                reports.addAll(factionStandings.updateCampaignForPastMissions(missions,
                      campaignIcon,
                      campaignFaction,
                      today,
                      regardMultiplier));
            } else {
                reports.add(getTextAt(RESOURCE_BUNDLE, "gmTools.ZERO_ALL_REGARD.report"));
                factionStandings.resetAllFactionStandings();
                factionStandings.setClimateRegard(new HashMap<>());
            }
            dispose();
        });

        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        pnlButtons.add(btnConfirm);

        return pnlButtons;
    }

    /**
     * Handles actions to perform when a hyperlink event occurs in the dialog, such as glossary lookups.
     *
     * @param evt the {@link HyperlinkEvent} that was received.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            NewGlossaryDialog.handleGlossaryHyperlinkClick(this, evt);
        }
    }
}
