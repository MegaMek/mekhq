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
package mekhq.gui.dialog.reportDialogs.FactionStanding.gmToolsDialog;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
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

import megamek.logging.MMLogger;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;
import mekhq.gui.dialog.reportDialogs.FactionStanding.manualMissionDialogs.StandingUpdateConfirmationDialog;

/**
 * GMTools allows Game Masters to adjust Faction Standings through various operations. These operations include zeroing
 * all standings, resetting to climate regard, updating for historic contracts, and applying specific values to selected
 * factions.
 *
 * <p>This dialog provides options as buttons for each supported action, along with localized descriptions and
 * reporting of performed actions.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class GMTools extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(GMTools.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final Faction campaignFaction;
    private final LocalDate today;
    private final int gameYear;
    private final FactionStandings factionStandings;
    private final List<Mission> missions;

    private final List<String> reports = new ArrayList<>();

    /**
     * Constructs a new {@link GMTools} dialog window.
     *
     * @param parent           the parent {@link JDialog} for modality
     * @param campaignIcon     icon representing the campaign's faction
     * @param campaignFaction  the main faction of the campaign
     * @param today            the current in-game date
     * @param factionStandings the {@link FactionStandings} object to be modified
     * @param missions         list of missions to support historic contract updates
     *
     * @author Illiani
     * @since 0.50.07
     */
    public GMTools(JDialog parent, ImageIcon campaignIcon, Faction campaignFaction, LocalDate today,
          FactionStandings factionStandings, List<Mission> missions) {
        this.campaignIcon = campaignIcon;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.gameYear = today.getYear();
        this.factionStandings = factionStandings;
        this.missions = missions;

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
     * Initializes and displays the dialog window.
     *
     * @param parent the parent {@link JDialog} to relate positioning to
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
     * Creates and arranges the dialog's main panel, including the campaign icon and the action options for GM tools.
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
     * Creates the left panel of the dialog containing the scaled campaign icon.
     *
     * @return {@link JPanel} representing the left-side campaign icon display
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
     * Creates the center panel of the dialog containing available GM tool options.
     *
     * @return {@link JPanel} containing option panels for each supported GM action
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateCenterPanel() {
        JPanel pnlParent = new JPanel();
        pnlParent.setLayout(new BoxLayout(pnlParent, BoxLayout.Y_AXIS));

        for (FactionStandingsGMToolsActionType actionType : FactionStandingsGMToolsActionType.values()) {
            JPanel pnlTool = populateGMToolOption(actionType);
            pnlParent.add(Box.createVerticalStrut(PADDING));
            pnlParent.add(pnlTool);
        }

        return pnlParent;
    }

    /**
     * Constructs an option panel for a specific GM tool type, including a localized description and an action button.
     *
     * @param actionType the {@link FactionStandingsGMToolsActionType} to construct the panel for
     *
     * @return {@link JPanel} with description and button for the given action type
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateGMToolOption(final FactionStandingsGMToolsActionType actionType) {
        String actionTypeName = actionType.name();
        String actionTypeKeyPrefix = "gmTools." + actionTypeName + ".";

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String description = getTextAt(RESOURCE_BUNDLE, actionTypeKeyPrefix + "description");
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);

        RoundedJButton button = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, actionTypeKeyPrefix + "button"));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(evt -> updateStandingForChosenAction(actionType));

        panel.add(editorPane);
        panel.add(Box.createVerticalStrut(PADDING));
        panel.add(button);

        return panel;
    }


    /**
     * Performs the specific GM tool operation based on the selected actionType.
     *
     * <p>Opens the necessary dialogs for input or confirmation, updates faction standings, and generates appropriate
     * report entries.</p>
     *
     * @param actionType the selected {@link FactionStandingsGMToolsActionType}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void updateStandingForChosenAction(FactionStandingsGMToolsActionType actionType) {
        setVisible(false);

        FactionSelectionDialog factionSelectionDialog = null;
        String chosenFactionName = null;
        if (actionType == FactionStandingsGMToolsActionType.SET_SPECIFIC_REGARD) {
            factionSelectionDialog = new FactionSelectionDialog(this, campaignIcon, factionStandings, today);

            if (!factionSelectionDialog.wasActionConfirmed()) {
                setVisible(true);
                return;
            }


            Faction chosenFaction = factionSelectionDialog.getSelectedFaction();
            if (chosenFaction == null) {
                LOGGER.warn(new NullPointerException(), "Failed to find faction for dialog");
                return;
            }

            chosenFactionName = chosenFaction.getFullName(gameYear);
        }

        GMToolsConfirmationDialog GMToolsConfirmationDialog = new GMToolsConfirmationDialog(this,
              campaignIcon,
              actionType,
              chosenFactionName);

        if (GMToolsConfirmationDialog.wasActionConfirmed()) {
            new StandingUpdateConfirmationDialog(this, campaignIcon, true);

            switch (actionType) {
                case RESET_ALL_REGARD -> {
                    reports.add(getTextAt(RESOURCE_BUNDLE, "gmTools.ZERO_ALL_REGARD.report"));
                    factionStandings.resetAllFactionStandings();
                    factionStandings.updateClimateRegard(campaignFaction, today);

                    reports.addAll(factionStandings.initializeStartingRegardValues(campaignFaction, today));
                }
                case ZERO_ALL_REGARD -> {
                    reports.add(getTextAt(RESOURCE_BUNDLE, "gmTools.ZERO_ALL_REGARD.report"));
                    factionStandings.resetAllFactionStandings();
                    factionStandings.updateClimateRegard(campaignFaction, today);
                }
                case SET_SPECIFIC_REGARD -> {
                    Faction selectedFaction = factionSelectionDialog.getSelectedFaction();
                    if (selectedFaction == null) {
                        LOGGER.warn(new NullPointerException(), "Failed to find faction for GM Tool");
                        return;
                    }

                    double newRegard = factionSelectionDialog.getSelectedRegard();
                    reports.add(factionStandings.setRegardForFaction(campaignFaction.getShortName(),
                          selectedFaction.getShortName(), newRegard, gameYear, true));
                }
                case UPDATE_HISTORIC_CONTRACTS -> {
                    reports.add(getTextAt(RESOURCE_BUNDLE, "gmTools.ZERO_ALL_REGARD.report"));
                    factionStandings.resetAllFactionStandings();
                    factionStandings.updateClimateRegard(campaignFaction, today);
                    reports.addAll(factionStandings.updateCampaignForPastMissions(missions,
                          campaignIcon,
                          campaignFaction,
                          today));
                }
            }

            dispose();
        } else {
            setVisible(true);
        }
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
