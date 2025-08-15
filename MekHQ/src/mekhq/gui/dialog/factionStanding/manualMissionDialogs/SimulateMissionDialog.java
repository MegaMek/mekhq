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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;

/**
 * Dialog window to simulate missions and adjust faction standings based on employer, enemy, and mission status
 * selections within a campaign.
 *
 * <p>This dialog allows users to choose employer and enemy factions, set the current mission status, and provides
 * contextual instructions and options relevant to faction interactions for the campaign. It presents UI components for
 * these selections and manages associated logic.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SimulateMissionDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(400);
    public final static int UNTRACKED_FACTION_INDEX = 0;

    private ImageIcon campaignIcon;
    private final Faction campaignFaction;
    private final LocalDate today;
    private final int gameYear;

    private final List<Faction> allFactions = new ArrayList<>();
    private Faction employerChoice = null;
    private MMComboBox<String> comboEmployerFaction;
    private Faction enemyChoice = null;
    private MMComboBox<String> comboEnemyFaction;
    private JSpinner spnDuration;
    private int durationChoice = 1;

    private final List<MissionStatus> allStatuses = new ArrayList<>();
    private MMComboBox<String> comboMissionStatus = null;
    private MissionStatus statusChoice = MissionStatus.SUCCESS;

    /**
     * Constructs a dialog to simulate a mission for the given campaign, using the supplied campaign icon, faction, and
     * date.
     *
     * @param campaignIcon        {@link ImageIcon} representing the campaign.
     * @param campaignFaction     {@link Faction} representing the campaign.
     * @param today               The current {@link LocalDate}.
     * @param defaultStatusChoice The default mission status to be selected.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public SimulateMissionDialog(ImageIcon campaignIcon, Faction campaignFaction, LocalDate today,
          MissionStatus defaultStatusChoice) {
        this.campaignIcon = campaignIcon;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.gameYear = today.getYear();
        this.statusChoice = defaultStatusChoice;
    }

    /**
     * Constructs a dialog to simulate a mission for the given campaign, providing a parent frame for modality.
     *
     * @param parent          The parent {@link JFrame} for the dialog.
     * @param campaignIcon    {@link ImageIcon} representing the campaign.
     * @param campaignFaction {@link Faction} representing the campaign.
     * @param today           The current {@link LocalDate}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public SimulateMissionDialog(JFrame parent, ImageIcon campaignIcon, Faction campaignFaction, LocalDate today) {
        this.campaignIcon = campaignIcon;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.gameYear = today.getYear();

        populateFactionsList();
        populateStatusList();
        populateDialog();
        initializeDialog(parent);
    }

    /**
     * Returns the faction chosen as the employer in this simulation dialog.
     *
     * @return the selected employer {@link Faction}, or {@code null} if none is selected.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Faction getEmployerChoice() {
        return employerChoice;
    }

    /**
     * Returns the faction chosen as the enemy in this simulation dialog.
     *
     * @return the selected enemy {@link Faction}, or {@code null} if none is selected.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Faction getEnemyChoice() {
        return enemyChoice;
    }

    /**
     * Returns the mission status selected in the dialog.
     *
     * @return the selected {@link MissionStatus}, or {@code null} if none is selected.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable MissionStatus getStatusChoice() {
        return statusChoice;
    }

    /**
     * Retrieves the duration choice selected in the simulation dialog.
     *
     * @return the duration choice as an {@link Integer} value.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getDurationChoice() {
        return durationChoice;
    }

    /**
     * Sets the duration choice for the mission simulation dialog.
     *
     * @param durationChoice the duration choice to be set, represented as an {@link Integer}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setDurationChoice(int durationChoice) {
        this.durationChoice = durationChoice;
    }

    /**
     * Initializes the dialog with components, setting the parent if provided.
     *
     * @param parent the parent {@link JFrame} for the dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void initializeDialog(JFrame parent) {
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
     * Populates the dialog UI with relevant factions and mission status options.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, 0, PADDING, 0);
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
        pnlCenter.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 2;
        constraints.weighty = 2;
        mainPanel.add(pnlCenter, constraints);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Fills the internal list of all available factions for selection.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void populateFactionsList() {
        Factions factions = Factions.getInstance();
        List<Faction> activeFactions = new ArrayList<>(factions.getActiveFactions(today));

        activeFactions.removeIf(Faction::isAggregate);
        activeFactions.sort(Comparator.comparing(faction -> faction.getFullName(today.getYear())));

        // This is a placeholder to ensure that the indexes of the combos and the list remain in sync
        allFactions.add(UNTRACKED_FACTION_INDEX, factions.getFaction("NONE"));

        allFactions.addAll(activeFactions);
    }

    /**
     * Fills the internal list of possible mission statuses for selection.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void populateStatusList() {
        Collections.addAll(allStatuses, MissionStatus.values());
    }

    /**
     * Builds and returns the panel displayed on the left side of the dialog, which include campaign-related graphics.
     *
     * @return the constructed left-side {@link JPanel}.
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
     * Builds and returns the center panel containing the main input controls.
     *
     * @return the constructed center {@link JPanel}.
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

        JPanel pnlFactions = populateContractPanel();
        pnlFactions.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCenter.add(pnlInstructions);
        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(pnlFactions);
        pnlParent.add(pnlCenter);

        JPanel pnlButton = populateButtonPanel();
        pnlButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlParent.add(pnlButton);

        return pnlParent;
    }

    /**
     * Creates a panel with instructions for using the dialog and making selections.
     *
     * @return the constructed instructions {@link JPanel}.
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
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String instructions = getFormattedTextAt(RESOURCE_BUNDLE,
              "simulateContractDialog.instructions",
              getMissionName());
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, instructions));
        setFontScaling(editorPane, false, 1.1);

        pnlInstructions.add(editorPane);

        return pnlInstructions;
    }

    /**
     * Returns the display name for the current mission, as used within the dialog.
     *
     * @return the name of the mission.
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected String getMissionName() {
        return getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.instructions.noMission");
    }

    /**
     * Populates and returns the contract (mission) details panel for the dialog.
     *
     * @return the constructed contract {@link JPanel}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateContractPanel() {
        JPanel pnlFactions = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblEmployer = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.employer"));
        pnlFactions.add(lblEmployer, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        comboEmployerFaction = new MMComboBox<>("comboEmployerFaction", buildFactionModel());
        if (allFactions.contains(campaignFaction)) {
            comboEmployerFaction.setSelectedItem(campaignFaction.getFullName(gameYear));
        }
        pnlFactions.add(comboEmployerFaction, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblEnemy = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.enemy"));
        pnlFactions.add(lblEnemy, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        comboEnemyFaction = new MMComboBox<>("comboEnemyFaction", buildFactionModel());
        pnlFactions.add(comboEnemyFaction, gbc);

        comboMissionStatus = getComboMissionStatus();
        if (comboMissionStatus != null) { // This will be null if we're suppressing the combo box
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.LINE_END;
            JLabel lblMissionStatus = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.status"));
            pnlFactions.add(lblMissionStatus, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.LINE_START;
            pnlFactions.add(comboMissionStatus, gbc);
        }

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblDuration = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.duration"));
        pnlFactions.add(lblDuration, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        spnDuration = new JSpinner(new SpinnerNumberModel(durationChoice, 1, 120, 1));
        pnlFactions.add(spnDuration, gbc);

        return pnlFactions;
    }

    /**
     * Returns the combo box UI element used for mission status selection, if present.
     *
     * @return the {@link MMComboBox} for mission status selection, or {@code null} if not created.
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected @Nullable MMComboBox<String> getComboMissionStatus() {
        comboMissionStatus = new MMComboBox<>("comboMissionStatus", buildMissionStatusModel());

        if (allStatuses.contains(statusChoice)) {
            comboMissionStatus.setSelectedItem(statusChoice.toString());
        }

        return comboMissionStatus;
    }

    /**
     * Builds a combo box model containing all available factions for selection.
     *
     * @return a {@link DefaultComboBoxModel} containing faction names.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private DefaultComboBoxModel<String> buildFactionModel() {
        DefaultComboBoxModel<String> factionModel = new DefaultComboBoxModel<>();
        factionModel.addElement(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.combo.untracked"));

        for (Faction faction : allFactions) {
            if (faction.equals(allFactions.get(UNTRACKED_FACTION_INDEX))) {
                continue;
            }

            factionModel.addElement(faction.getFullName(gameYear));
        }

        return factionModel;
    }

    /**
     * Builds a combo box model containing all available mission status values.
     *
     * @return a {@link DefaultComboBoxModel} containing mission status names.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private DefaultComboBoxModel<String> buildMissionStatusModel() {
        DefaultComboBoxModel<String> missionStatusModel = new DefaultComboBoxModel<>();

        for (MissionStatus status : allStatuses) {
            missionStatusModel.addElement(status.toString());
        }

        return missionStatusModel;
    }

    /**
     * Creates and returns the panel containing action buttons for the dialog.
     *
     * @return the constructed button {@link JPanel}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateButtonPanel() {
        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        String lblConfirm = getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(lblConfirm);
        btnConfirm.addActionListener(evt -> {
            int employerChoiceIndex = comboEmployerFaction.getSelectedIndex();
            if (employerChoiceIndex != UNTRACKED_FACTION_INDEX) { // If it's untracked, leave the choice null
                employerChoice = allFactions.get(employerChoiceIndex);
            }

            int enemyChoiceIndex = comboEnemyFaction.getSelectedIndex();
            if (enemyChoiceIndex != UNTRACKED_FACTION_INDEX) { // If it's untracked, leave the choice null
                enemyChoice = allFactions.get(enemyChoiceIndex);
            }

            if (comboMissionStatus != null) {
                int missionStatusChoiceIndex = comboMissionStatus.getSelectedIndex();
                statusChoice = allStatuses.get(missionStatusChoiceIndex);
            }

            durationChoice = (int) spnDuration.getValue();

            boolean wasUpdated = false;
            if (enemyChoice != null) {
                wasUpdated = true;
            } else if (employerChoice != null && statusChoice != MissionStatus.ACTIVE) {
                wasUpdated = true;
            }

            // If we didn't successfully update, we want the player to have another chance. This is especially
            // important if this dialog is being triggered at the conclusion of a non-StratCon mission.
            if (wasUpdated) {
                dispose();
            } else {
                setVisible(false);
            }

            new StandingUpdateConfirmationDialog(this, campaignIcon, wasUpdated);

            if (!wasUpdated) {
                setVisible(true); // This should always be present otherwise we can lock up the player's client
            }
        });

        pnlButton.add(btnConfirm);

        String lblSkip = getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.button.skip");
        RoundedJButton btnSkip = new RoundedJButton(lblSkip);
        btnSkip.addActionListener(evt -> dispose());
        pnlButton.add(btnSkip);

        return pnlButton;
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

    /**
     * Use
     * {@link #handleFactionRegardUpdates(Faction, Faction, Faction, MissionStatus, LocalDate, FactionStandings, double,
     * int)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static List<String> handleFactionRegardUpdates(@Nullable Faction campaignFaction,
          @Nullable final Faction employer, @Nullable final Faction enemy, final MissionStatus status,
          final LocalDate today, final FactionStandings factionStandings) {
        return handleFactionRegardUpdates(campaignFaction, employer, enemy, status, today, factionStandings, 1.0, 1);
    }

    /**
     * Calculates and describes updates to faction regard values in response to mission simulation parameters, for both
     * employer and enemy.
     *
     * @param campaignFaction  the current campaign faction
     * @param employer         the employer faction, or {@code null} if not specified
     * @param enemy            the enemy faction, or {@code null} if not specified
     * @param status           the mission status applied to the simulation
     * @param today            the current date of the simulation
     * @param factionStandings the {@link FactionStandings} object holding all faction Regard data
     * @param regardMultiplier the regard multiplier set in campaign options
     * @param contractDuration how many months the contract or mission lasted
     *
     * @return a list of strings detailing each regard update performed as a result
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static List<String> handleFactionRegardUpdates(@Nullable Faction campaignFaction,
          @Nullable final Faction employer, @Nullable final Faction enemy, final MissionStatus status,
          final LocalDate today, final FactionStandings factionStandings, final double regardMultiplier,
          final int contractDuration) {
        List<String> reports = new ArrayList<>();
        if (enemy != null) { // Null means the faction isn't tracked
            String report = factionStandings.processContractAccept(campaignFaction.getShortName(), enemy, today,
                  regardMultiplier, contractDuration);
            if (report != null) {
                reports.add(report);
            }
        }

        if (employer != null) {
            reports.addAll(factionStandings.processContractCompletion(campaignFaction,
                  employer,
                  today,
                  status,
                  regardMultiplier,
                  contractDuration));
        }

        return reports;
    }
}
