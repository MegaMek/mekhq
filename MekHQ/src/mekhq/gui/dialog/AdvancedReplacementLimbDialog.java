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

import static java.lang.Math.ceil;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_BIONIC_HATE;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_BIOLOGICAL_MACHINIST;
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.CLONED_LIMB_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.COSMETIC_SURGERY_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.EI_IMPLANT_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.ELECTIVE_IMPLANT_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.FAILED_SURGERY_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.PAIN_SHUNT_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.REPLACEMENT_LIMB_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.REPLACEMENT_ORGAN_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.IMPLANT_VDNI;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.COSMETIC_SURGERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.ENHANCED_IMAGING;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.PAIN_SHUNT;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryEntryDialog;
import mekhq.gui.view.PaperDoll;

/**
 * Dialog for planning and executing advanced replacement limb and prosthetic surgeries for a single patient.
 *
 * <p>The dialog displays the patient's injuries, available prosthetic options per body location, projected surgery
 * difficulty, and total cost. It also performs surgery skill checks, applies resulting injuries or recovery effects,
 * and debits campaign funds when the user confirms the plan.</p>
 *
 * <p>This dialog is modal and is shown immediately from its constructor when a non-{@code null} patient is
 * supplied.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class AdvancedReplacementLimbDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AdvancedReplacementLimbDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedReplacementLimbDialog";

    private static final int PADDING = scaleForGUI(10);
    private static final String MALE_PAPER_DOLL = "default_male_paperdoll";
    private static final String FEMALE_PAPER_DOLL = "default_female_paperdoll";

    /**
     * Valid body locations that can be targeted by replacement or prosthetic treatments. The order of this list
     * controls the ordering in the GUI.
     */
    private static final List<BodyLocation> VALID_BODY_LOCATIONS = List.of(
          BRAIN,
          FACE,
          MOUTH,
          EYES,
          EARS,
          LEFT_ARM,
          RIGHT_ARM,
          LEFT_HAND,
          RIGHT_HAND,
          CHEST,
          HEART,
          LUNGS,
          ORGANS,
          ABDOMEN,
          RUMP,
          LEFT_LEG,
          RIGHT_LEG,
          LEFT_FOOT,
          RIGHT_FOOT,
          BONES,
          INTERNAL
    );

    private final Campaign campaign;
    private final Person patient;
    private final boolean isGMMode;
    private Person surgeon; // can be null
    private int surgeryLevelNeeded = 0;
    private boolean isUseLocalSurgeon;
    private Money totalCost = Money.zero();

    private PaperDoll doll;
    private PaperDoll defaultMaleDoll;
    private PaperDoll defaultFemaleDoll;
    private ActionListener dollActionListener;

    private final Map<BodyLocation, List<Injury>> relevantInjuries = new HashMap<>();
    private final Map<BodyLocation, List<Injury>> injuriesMappedToPrimaryLocations = new HashMap<>();
    private final Map<BodyLocation, List<ProstheticType>> treatmentOptions = new HashMap<>();
    private final Map<BodyLocation, JComboBox<ProstheticType>> treatmentSelections = new HashMap<>();

    private RoundedJButton confirmButton;
    private JLabel summaryLabel;

    /**
     * Represents a planned surgery consisting of a prosthetic type and a specific body location.
     *
     * @param type     the prosthetic type to be installed
     * @param location the body location being treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record PlannedSurgery(ProstheticType type, BodyLocation location) {
        /**
         * Builds a simple label combining the prosthetic display name and the location name, for use in reports.
         *
         * @return a human-readable label for the planned surgery
         *
         * @author Illiani
         * @since 0.50.10
         */
        public String getLabel() {
            return type.toString() + "-" + location.locationName();
        }
    }

    /**
     * Creates and displays a new {@link AdvancedReplacementLimbDialog} for the given patient.
     *
     * <p>If the patient is {@code null}, the dialog performs no initialization and returns without being shown.</p>
     *
     * @param campaign the active campaign context
     * @param patient  the patient undergoing treatment, or {@code null}
     * @param isGMMode whether the dialog has been launched in GM Mode (bypassing all restrictions)
     *
     * @author Illiani
     * @since 0.50.10
     */
    public AdvancedReplacementLimbDialog(Campaign campaign, @Nullable Person patient, boolean isGMMode) {
        this.patient = patient;
        this.campaign = campaign;
        this.isGMMode = isGMMode;
        surgeon = getSurgeon(campaign.getDoctors()); // can return null

        if (patient == null) {
            return;
        }

        gatherRelevantInjuries(patient.getInjuries());
        gatherTreatmentOptions();
        paperDoll();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initializeUI();
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setPreferences(this); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Initializes the dialog UI, assembling the instructions, prosthetic selection grid, paper doll, summary, and
     * button panel.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void initializeUI() {
        setTitle(getText("accessingTerminal.title"));
        setLayout(new BorderLayout());

        // Create center container to hold main panel and right panel side by side
        JPanel centerContainer = new JPanel(new BorderLayout());

        // Create left container for tutorial and main panel
        JPanel leftContainer = new JPanel(new BorderLayout());

        // Create tutorial panel at the top of left container
        JPanel tutorialPanel = new JPanel(new BorderLayout());
        tutorialPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        JTextArea tutorialText = new JTextArea(getTextAt(RESOURCE_BUNDLE,
              "AdvancedReplacementLimbDialog.instructions"));
        tutorialText.setWrapStyleWord(true);
        tutorialText.setLineWrap(true);
        tutorialText.setEditable(false);
        tutorialText.setOpaque(false);
        tutorialPanel.add(tutorialText, BorderLayout.CENTER);
        leftContainer.add(tutorialPanel, BorderLayout.NORTH);

        // Create main panel for injuries and treatments
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets =
              new Insets(0, PADDING, PADDING / 2, PADDING);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel patientName = new JLabel("<html><h2>" + patient.getFullTitle() + "</h2></html>");
        mainPanel.add(patientName, gridBagConstraints);

        // Add location labels and treatment combos
        addSurgeryComboBoxes(gridBagConstraints, mainPanel);
        leftContainer.add(mainPanel, BorderLayout.CENTER);
        centerContainer.add(leftContainer, BorderLayout.CENTER);

        // Create right panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        rightPanel.setPreferredSize(scaleForGUI(250, 0));

        fillDoll(rightPanel);
        centerContainer.add(rightPanel, BorderLayout.EAST);

        // Wrap entire center container in scroll pane
        JScrollPane scrollPane = new JScrollPane(centerContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Create summary panel above buttons
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        summaryLabel = new JLabel();
        summaryLabel.setVerticalAlignment(SwingConstants.TOP);
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);

        // Create button panel at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(PADDING / 2, PADDING, PADDING, PADDING));

        RoundedJButton cancelButton = new RoundedJButton(getTextAt(
              RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.cancel"));
        cancelButton.addActionListener(evt -> dispose());

        RoundedJButton documentationButton = new RoundedJButton(getTextAt(
              RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.documentation"));
        documentationButton.addActionListener(this::onDocumentation);

        confirmButton = new RoundedJButton(getTextAt(
              RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.confirm"));
        confirmButton.addActionListener(this::onConfirm);

        RoundedJButton gmButton = new RoundedJButton(getTextAt(
              RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.gm"));
        gmButton.setEnabled(campaign.isGM());
        gmButton.addActionListener(this::onGMConfirm);

        buttonPanel.add(cancelButton);
        buttonPanel.add(documentationButton);
        buttonPanel.add(confirmButton);
        buttonPanel.add(gmButton);

        if (isGMMode) {
            RoundedJButton normalModeButton = new RoundedJButton(getTextAt(
                  RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.normalMode"));
            normalModeButton.addActionListener(evt -> {
                dispose();
                new AdvancedReplacementLimbDialog(campaign, patient, false);
            });
            buttonPanel.add(normalModeButton);
        } else {
            RoundedJButton gmModeButton = new RoundedJButton(getTextAt(
                  RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.button.gmMode"));
            gmModeButton.setEnabled(campaign.isGM());
            gmModeButton.addActionListener(evt -> {
                dispose();
                new AdvancedReplacementLimbDialog(campaign, patient, true);
            });
            buttonPanel.add(gmModeButton);
        }

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(summaryPanel, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);

        // Initialize summary (and other important values)
        updateSummary();
    }

    /**
     * Adds a label and prosthetic selection combo box row for each valid body location to the main panel, wiring the
     * combo boxes into {@link #treatmentSelections}.
     *
     * @param gridBagConstraints the constraints template used when laying out components
     * @param mainPanel          the panel that will host the surgery selection rows
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addSurgeryComboBoxes(GridBagConstraints gridBagConstraints, JPanel mainPanel) {
        int i = 1;
        for (BodyLocation bodyLocation : VALID_BODY_LOCATIONS) {
            // Label
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            JLabel location = new JLabel(bodyLocation.locationName());
            mainPanel.add(location, gridBagConstraints);

            // ProstheticType combobox
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            List<ProstheticType> options = treatmentOptions.get(bodyLocation);
            JComboBox<ProstheticType> treatmentComboBox = createTreatmentComboBox(options);
            treatmentSelections.put(bodyLocation, treatmentComboBox);
            mainPanel.add(treatmentComboBox, gridBagConstraints);
            i++;
        }
    }

    /**
     * Creates a combo box for selecting a prosthetic treatment at a given body location, including tooltips and
     * availability-based disabling.
     *
     * @param options the list of candidate prosthetic types for this location
     *
     * @return a configured {@link JComboBox} instance
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JComboBox<ProstheticType> createTreatmentComboBox(List<ProstheticType> options) {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();
        boolean isOnPlanet = campaign.getLocation().isOnPlanet();
        boolean isUseKinderMode = campaign.getCampaignOptions().isUseKinderAlternativeAdvancedMedical();

        JComboBox<ProstheticType> comboBox = new JComboBox<>();

        // Add null option first
        comboBox.addItem(null);

        // Add all available treatments
        for (ProstheticType treatment : options) {
            comboBox.addItem(treatment);
        }

        // Custom renderer to display "None" for null option and disable items
        // based on location
        String defaultTooltip = getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.combo.none.tooltip");
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                  Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                boolean enabled = true;
                String tooltip;

                if (value == null) {
                    setText(getTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.combo.none.label"));
                    tooltip = defaultTooltip;
                } else {
                    ProstheticType type = (ProstheticType) value;
                    setText(type.toString());

                    // Build tooltip with base info and exclusions
                    String baseTooltip = type.getTooltip(campaignFaction, today, isUseKinderMode);
                    String exclusions = getExclusions(isOnPlanet, type, campaignFaction, today);

                    if (!exclusions.isBlank()) {
                        enabled = false;
                        tooltip = wordWrap(baseTooltip + exclusions);
                    } else {
                        tooltip = wordWrap(baseTooltip);
                    }
                }

                setEnabled(enabled);
                setToolTipText(tooltip);

                return this;
            }
        });

        // Add listener to update tooltip and summary based on selection
        comboBox.addActionListener(e -> {
            ProstheticType selected = (ProstheticType) comboBox.getSelectedItem();
            if (selected == null) {
                comboBox.setToolTipText(defaultTooltip);
            } else {
                String baseTooltip = selected.getTooltip(campaignFaction, today, isUseKinderMode);
                baseTooltip += getExclusions(isOnPlanet, selected, campaignFaction, today);
                comboBox.setToolTipText(wordWrap(baseTooltip));
            }
            updateSummary(); // Update summary when selection changes
        });

        // Set initial tooltip
        comboBox.setToolTipText(defaultTooltip);

        return comboBox;
    }

    /**
     * Updates the summary panel to reflect the current prosthetic selections, including the number of surgeries,
     * required surgery level, surgeon choice, and total cost. Also enables or disables the confirm button based on
     * available campaign funds.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSummary() {
        if (summaryLabel == null || confirmButton == null) {
            return;
        }

        totalCost = Money.zero(); // Reset cost
        surgeryLevelNeeded = 0; // Reset surgery level needs
        int selectedCount = getSelectedTreatments().size();

        getSurgeryCostAndSkillRequirements();

        updateUseLocalSurgeonFlag();
        if (isUseLocalSurgeon) {
            totalCost = totalCost.multipliedBy(10);
        }

        confirmButton.setEnabled(campaign.getFunds().isGreaterOrEqualThan(totalCost) && selectedCount > 0);

        List<String> summary = new ArrayList<>();
        if (selectedCount > 0) {
            summary.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedReplacementLimbDialog.status.selected",
                  selectedCount));

            summary.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedReplacementLimbDialog.status.difficulty",
                  surgeryLevelNeeded));

            if (isUseLocalSurgeon) {
                if (campaign.getLocation().isOnPlanet()) {
                    summary.add(getTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.status.localSurgeon"));
                } else {
                    summary.add(getTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.status.localSurgeon.transit"));
                }
            } else {
                int targetNumber = surgeon.getSkill(S_SURGERY).getFinalSkillValue(surgeon.getSkillModifierData());
                targetNumber -= surgeon.getOptions().booleanOption(UNOFFICIAL_BIOLOGICAL_MACHINIST) ? -2 : 0;

                summary.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.status.surgeon",
                      surgeon.getFullTitle(), targetNumber));
            }

            if (totalCost.isPositive()) {
                summary.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.status.total",
                      totalCost.toAmountString()));
            }
        } else {
            // These just ensure the gui stays at the same height
            summary.add(" ");
            summary.add(" ");
            summary.add(" ");
            summary.add(" ");
            summary.add(" ");
        }

        summaryLabel.setText("<html>" + String.join("<br>", summary) + "</html>");
    }

    /**
     * Aggregates the total surgery cost and determines the highest surgery level required among all selected prosthetic
     * treatments.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void getSurgeryCostAndSkillRequirements() {
        for (ProstheticType surgeryType : getSelectedTreatments().values()) {
            int neededSurgeryLevel = surgeryType.getSurgeryLevel();
            if (neededSurgeryLevel > surgeryLevelNeeded) {
                surgeryLevelNeeded = neededSurgeryLevel;
            }

            Money cost = surgeryType.getCost(campaign.getFaction(), campaign.getLocalDate());
            if (cost != null) {
                totalCost = totalCost.plus(cost);
            }
        }
    }

    /**
     * Determines whether a local (NPC) surgeon must be used instead of a campaign doctor, based on availability and
     * surgery skill.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateUseLocalSurgeonFlag() {
        isUseLocalSurgeon = surgeon == null;
        if (!isUseLocalSurgeon) {
            Skill surgerySkill = surgeon.getSkill(S_SURGERY);
            if (surgerySkill != null) {
                SkillModifierData modifierData = surgeon.getSkillModifierData();
                int surgeonTotalSkill = surgerySkill.getTotalSkillLevel(modifierData);
                isUseLocalSurgeon = surgeonTotalSkill < surgeryLevelNeeded;
            } else {
                isUseLocalSurgeon = true;
            }
        }
    }

    /**
     * Builds a localized HTML-formatted snippet explaining why the selected prosthetic is currently unavailable to the
     * patient. This method evaluates all restriction layers—including faction access rules, planetary location, tech
     * rating requirements, personal prohibitions, and era-based availability—and produces a concatenated set of warning
     * or exclusion messages.
     *
     * <p>Messages are color-coded to distinguish between:
     * <ul>
     *     <li><b>Warnings:</b> Soft restrictions that can be resolved by the player (e.g., not being on a planet,
     *     insufficient local tech level).</li>
     *     <li><b>Exclusions:</b> Hard restrictions that completely prevent access (e.g., faction incompatibility,
     *     personal prohibitions such as Hatred of Bionics).</li>
     * </ul>
     *
     * <p>If GM Mode is enabled, no restrictions apply and an empty string is returned.</p>
     *
     * @param isOnPlanet      {@code true} if the campaign is currently located on a planet, which affects access to
     *                        certain prosthetic types
     * @param selected        the prosthetic type being evaluated for availability
     * @param campaignFaction the faction of the campaign, used to determine faction-based access
     * @param today           the current in-game date, used to check era-based availability
     *
     * @return an HTML-formatted string containing all applicable restriction messages, or an empty string if the
     *       prosthetic is fully allowed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private String getExclusions(boolean isOnPlanet, ProstheticType selected, Faction campaignFaction,
          LocalDate today) {
        if (isGMMode) {
            return "";
        }

        String tooltip = "";

        // Something that the player is currently blocked by, but there is a way for them to bypass (such as changing
        // location)
        String warningColor = spanOpeningWithCustomColor(getWarningColor());
        // Something the player is completely blocked by with no recourse
        String exclusionColor = spanOpeningWithCustomColor(getNegativeColor());

        // Check if selection should be disabled
        if (selected != null) {
            int atowProstheticType = selected.getProstheticType();
            boolean hasHatredOfBionics = patient.getOptions().booleanOption(COMPULSION_BIONIC_HATE);
            if (hasHatredOfBionics && atowProstheticType > 2) {
                tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.exclusions.refused", exclusionColor, CLOSING_SPAN_TAG);
            }

            if (!isOnPlanet && atowProstheticType > 2) {
                tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.exclusions.planet", warningColor, CLOSING_SPAN_TAG);
            }

            if (!selected.isAvailableToFaction(campaign.getFaction(), campaign.getLocalDate())) {
                if (selected.isClanOnly()) {
                    tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.exclusions.faction.clan", exclusionColor, CLOSING_SPAN_TAG);
                } else if (selected.isComStarOnly()) {
                    tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.exclusions.faction.comstar", exclusionColor, CLOSING_SPAN_TAG);
                } else if (selected.isWordOfBlakeOnly()) {
                    tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.exclusions.faction.wob", exclusionColor, CLOSING_SPAN_TAG);
                } else {
                    tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.exclusions.faction.generic", exclusionColor, CLOSING_SPAN_TAG);
                }
            }

            if (!selected.isAvailableInCurrentLocation(campaign.getLocation(), campaign.getLocalDate())) {
                tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.exclusions.tech", warningColor, CLOSING_SPAN_TAG);
            }

            if (selected.getCost(campaignFaction, today) == null) {
                tooltip += getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedReplacementLimbDialog.exclusions.year", warningColor, CLOSING_SPAN_TAG);
            }
        }

        return tooltip;
    }

    /**
     * Opens the glossary entry describing prosthetics when the documentation button is pressed.
     *
     * @param event the originating action event
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void onDocumentation(ActionEvent event) {
        GlossaryEntry glossaryEntry = GlossaryEntry.PROSTHETICS;

        try {
            new NewGlossaryEntryDialog(this, glossaryEntry);
        } catch (Exception ex) {
            LOGGER.error("Failed to open Glossary Entry", ex);
        }
    }

    /**
     * Handles the confirm action. This method disposes the dialog, charges the campaign, determines the surgeon,
     * performs surgery skill checks, applies surgery and recovery injuries, and reports results back to the campaign
     * log.
     *
     * @param event the originating action event
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void onConfirm(ActionEvent event) {
        dispose();

        // Pay for everything
        payForSurgeries();

        // Get a local surgeon (if applicable)
        getLocalSurgeon();

        // First, prioritize surgeries based on difficulty and expense. This is so that any available Edge is used on
        // the more important surgeries first.
        List<PlannedSurgery> prioritizedSurgeries = getPrioritizedSurgeries();

        // Then perform the surgery skill checks
        List<PlannedSurgery> successfulSurgeries = new ArrayList<>();
        List<PlannedSurgery> unsuccessfulSurgeries = new ArrayList<>();
        performSurgerySkillChecks(prioritizedSurgeries, successfulSurgeries, unsuccessfulSurgeries);

        // Then perform the actual surgeries
        boolean useKinderMode = campaign.getCampaignOptions().isUseKinderAlternativeAdvancedMedical();
        for (PlannedSurgery surgery : prioritizedSurgeries) {
            performSurgery(surgery, useKinderMode, successfulSurgeries);
        }

        // Notify the player of the results
        if (!successfulSurgeries.isEmpty()) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedReplacementLimbDialog.report.successful",
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                  String.join(", ",
                        successfulSurgeries.stream()
                              .map(PlannedSurgery::getLabel)
                              .toList())
            ));
        }

        if (!unsuccessfulSurgeries.isEmpty()) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedReplacementLimbDialog.report.unsuccessful",
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                  String.join(", ",
                        unsuccessfulSurgeries.stream()
                              .map(PlannedSurgery::getLabel)
                              .toList())
            ));
        }

        // Check to see if the patient died on the operating table
        checkForDeath();
    }

    /**
     * Handles the GM confirm action by applying all planned surgeries without performing any rolls or charging the
     * campaign. This is intended for manual GM adjudication.
     *
     * <p>For each planned surgery this method removes applicable injuries, records a marker injury, and grants any
     * associated implants and abilities before notifying the campaign that the patient has been updated.</p>
     *
     * @param event the originating action event
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void onGMConfirm(ActionEvent event) {
        if (!campaign.isGM()) {
            return;
        }
        dispose();

        for (PlannedSurgery surgery : getPlannedSurgeries()) {
            BodyLocation location = surgery.location;
            ProstheticType type = surgery.type;

            processOldInjuryRemoval(type, location);
            addMarkerInjury(type, location);
            addImplantsAndAbilities(type);
        }

        campaign.personUpdated(patient);
    }

    /**
     * Debits the campaign finances for the total cost of all planned surgeries.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void payForSurgeries() {
        Finances finances = campaign.getFinances();
        LocalDate today = campaign.getLocalDate();
        finances.debit(TransactionType.REPAIRS, today, totalCost,
              getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.transaction",
                    patient.getFullTitle()));
    }

    /**
     * Applies the outcome of a single planned surgery. On success, removes applicable injuries, adds a record injury,
     * and adds recovery injuries. On failure, adds a failed-surgery recovery injury.
     *
     * @param surgery             the surgery being performed
     * @param useKinderMode       whether to halve recovery times
     * @param successfulSurgeries the list of surgeries that passed their skill checks
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void performSurgery(PlannedSurgery surgery, boolean useKinderMode,
          List<PlannedSurgery> successfulSurgeries) {
        BodyLocation location = surgery.location;
        ProstheticType type = surgery.type;

        if (successfulSurgeries.contains(surgery)) {
            // Remove injuries based on the surgery type
            processOldInjuryRemoval(type, location);

            // Add the new surgery 'injury'. This is a semi-permanent record of the surgery on the character
            addMarkerInjury(type, location);

            // Add recovery period injuries
            InjuryType recoveryInjuryType = getRecoveryInjuryType(surgery);
            Injury recoveryInjury = recoveryInjuryType.newInjury(campaign, patient, GENERIC, 1);
            adjustForKinderMode(useKinderMode, recoveryInjury);
            patient.addInjury(recoveryInjury);

            addImplantsAndAbilities(type);
        } else {
            // Add failed surgery injury
            Injury recoveryInjury = FAILED_SURGERY_RECOVERY.newInjury(campaign, patient, GENERIC, 1);
            adjustForKinderMode(useKinderMode, recoveryInjury);
            patient.addInjury(recoveryInjury);
        }

        campaign.personUpdated(patient);
    }

    /**
     * Grants any pilot and personnel abilities associated with the given prosthetic type, honoring the campaign options
     * that govern implants and SPAs.
     *
     * @param type the prosthetic type being applied to the patient
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addImplantsAndAbilities(ProstheticType type) {
        if (campaign.getCampaignOptions().isUseImplants()) {
            for (String implant : type.getAssociatedPilotOptions()) {
                patient.getOptions().acquireAbility(LVL3_ADVANTAGES, implant, true);
            }
        }

        if (campaign.getCampaignOptions().isUseAbilities()) {
            for (String option : type.getAssociatedPersonnelOptions()) {
                patient.getOptions().acquireAbility(LVL3_ADVANTAGES, option, true);
            }
        }
    }

    /**
     * Adds a zero-severity marker injury representing the installed prosthetic at the specified body location. This
     * serves as a semi-permanent record of the surgery.
     *
     * @param type     the prosthetic type that was installed
     * @param location the body location that was treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addMarkerInjury(ProstheticType type, BodyLocation location) {
        InjuryType injuryType = type.getInjuryType();
        Injury injury = injuryType.newInjury(campaign, patient, location, 0);
        patient.addInjury(injury);
    }

    /**
     * Removes existing injuries at the given location that are resolved by the specified prosthetic type. Some
     * prosthetics can only clear burn injuries, while others remove all mapped injuries at that location.
     *
     * @param type     the prosthetic type being applied
     * @param location the body location being treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void processOldInjuryRemoval(ProstheticType type, BodyLocation location) {
        boolean isBurnRemovalOnly = type.isBurnRemoveOnly();
        InjuryType surgeryInjuryType = type.getInjuryType();
        InjurySubType subType = surgeryInjuryType.getSubType();
        boolean surgeryIsImplant = subType.isImplant(); // Includes VDNI implants
        boolean surgeryIsVDNI = subType == IMPLANT_VDNI;

        // Implants remove any other instances of the same implant, otherwise no removal occurs. Non-implants remove
        // all relevant injuries.
        if (surgeryIsImplant) {
            for (Injury injury : patient.getInjuries()) {
                InjurySubType injurySubType = injury.getSubType();
                if (injurySubType.isImplant()) {
                    // We only remove implants if we're adding an identical implant, or another VDNI implant
                    if ((surgeryIsVDNI && injurySubType == IMPLANT_VDNI) ||
                              (injury.getType() == surgeryInjuryType)) {
                        patient.removeInjury(injury);
                    }
                }
            }
        } else {
            for (Injury injury : relevantInjuries.getOrDefault(location, new ArrayList<>())) {
                if (injury != null) {
                    if (injury.getSubType().isBurn() || !isBurnRemovalOnly) {
                        patient.removeInjury(injury);
                    }
                }
            }
        }
    }

    /**
     * Determines whether accumulated injury hits after surgery results in the patient's death (medical complications)
     * and updates their status accordingly.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void checkForDeath() {
        if (patient.getTotalInjurySeverity() > 5) {
            patient.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
        }
    }

    /**
     * Selects the appropriate recovery injury type to apply after a successful surgery, based on prosthetic generation
     * and whether a limb or organ was treated.
     *
     * @param surgery the planned surgery
     *
     * @return the corresponding recovery {@link InjuryType}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static InjuryType getRecoveryInjuryType(PlannedSurgery surgery) {
        ProstheticType type = surgery.type;
        if (type == COSMETIC_SURGERY) {
            return COSMETIC_SURGERY_RECOVERY;
        } else if (surgery.location.isLimb()) {
            if (type.isElectiveImplant()) {
                return ELECTIVE_IMPLANT_RECOVERY;
            } else if (type.getProstheticType() >= 6) {
                return CLONED_LIMB_RECOVERY;
            } else {
                return REPLACEMENT_LIMB_RECOVERY;
            }
        } else if (type == PAIN_SHUNT) {
            return PAIN_SHUNT_RECOVERY;
        } else if (type == ENHANCED_IMAGING) {
            return EI_IMPLANT_RECOVERY;
        } else {
            return REPLACEMENT_ORGAN_RECOVERY;
        }
    }

    /**
     * Adjusts an injury's recovery time when "kinder" advanced medical rules are enabled by halving both the original
     * and current recovery time.
     *
     * @param useKinderMode whether kinder mode is active
     * @param injury        the injury whose recovery time should be adjusted
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void adjustForKinderMode(boolean useKinderMode,
          Injury injury) {
        if (useKinderMode) {
            int originalRecoveryTime = injury.getOriginalTime();
            int newRecoveryTime = (int) ceil(originalRecoveryTime / 2.0);
            injury.setOriginalTime(newRecoveryTime);
            injury.setTime(newRecoveryTime);
        }
    }

    /**
     * Creates a temporary local surgeon if needed, with sufficient surgery skill and a small amount of Edge so that
     * local surgeons are not completely ineffective.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void getLocalSurgeon() {
        if (isUseLocalSurgeon) {
            surgeon = new Person(campaign);
            surgeon.addSkill(S_SURGERY, surgeryLevelNeeded + 1, 0);
            surgeon.setEdge(1); // We don't want Local Surgeons to be completely ineffective
            surgeon.setCurrentEdge(1);
        }
    }

    /**
     * Performs surgery skill checks for each planned surgery and populates the lists of successful and unsuccessful
     * surgeries. Each check generates a report entry in the campaign log.
     *
     * @param prioritizedSurgeries  the surgeries ordered by importance
     * @param successfulSurgeries   out parameter for successful surgeries
     * @param unsuccessfulSurgeries out parameter for unsuccessful surgeries
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void performSurgerySkillChecks(List<PlannedSurgery> prioritizedSurgeries,
          List<PlannedSurgery> successfulSurgeries, List<PlannedSurgery> unsuccessfulSurgeries) {
        boolean hasMachinistSPA = surgeon.getOptions().booleanOption(UNOFFICIAL_BIOLOGICAL_MACHINIST);

        for (PlannedSurgery surgery : new ArrayList<>(prioritizedSurgeries)) {
            int spaModifier = surgery.type != COSMETIC_SURGERY && hasMachinistSPA ? -2 : 0;
            SkillCheckUtility skillCheckUtility = new SkillCheckUtility(
                  getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.skillCheck"),
                  surgeon,
                  S_SURGERY,
                  List.of(),
                  spaModifier,
                  true,
                  false);
            campaign.addReport(SKILL_CHECKS, skillCheckUtility.getResultsText());
            if (skillCheckUtility.isSuccess()) {
                successfulSurgeries.add(surgery);
                MedicalLogger.successfulSurgery(patient, campaign.getLocalDate(), surgery.type.toString());
            } else {
                unsuccessfulSurgeries.add(surgery);
                MedicalLogger.failedSurgery(patient, campaign.getLocalDate(), surgery.type.toString());
            }
        }
    }

    /**
     * Builds and returns a list of planned surgeries sorted by surgery difficulty and then by cost, from highest to
     * lowest. This is used to prioritize which surgeries consume Edge first.
     *
     * @return a sorted list of {@link PlannedSurgery} instances
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<PlannedSurgery> getPrioritizedSurgeries() {
        List<PlannedSurgery> prioritizedSurgeries = getPlannedSurgeries();

        prioritizedSurgeries.sort(
              Comparator.<PlannedSurgery>comparingInt(
                          s -> s.type().getSurgeryLevel())
                    .reversed() // highest surgery level first
                    .thenComparing(
                          // We shouldn't hit `null` at this point, as any null selections should have been filtered
                          // out
                          s -> Objects.requireNonNull(
                                s.type().getCost(campaign.getFaction(), campaign.getLocalDate())),
                          Comparator.reverseOrder() // highest cost first
                    )
        );

        return prioritizedSurgeries;
    }

    /**
     * Builds the list of surgeries currently planned based on the selected treatments in the UI.
     *
     * @return a list of {@link PlannedSurgery} instances representing the current plan
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<PlannedSurgery> getPlannedSurgeries() {
        Map<BodyLocation, ProstheticType> scheduledSurgeries = getSelectedTreatments();

        List<PlannedSurgery> prioritizedSurgeries = new ArrayList<>();
        for (Map.Entry<BodyLocation, ProstheticType> entry : scheduledSurgeries.entrySet()) {
            prioritizedSurgeries.add(new PlannedSurgery(entry.getValue(), entry.getKey()));
        }
        return prioritizedSurgeries;
    }

    /**
     * Determines the senior surgeon from the supplied personnel list, based on doctor status and rank/skill
     * tiebreakers.
     *
     * @param activePersonnel the list of active personnel to search
     *
     * @return the most senior surgeon, or {@code null} if none are available
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable Person getSurgeon(List<Person> activePersonnel) {
        Person seniorSurgeon = null;
        for (Person person : activePersonnel) {
            if (person.isDoctor()) {
                if (seniorSurgeon == null) {
                    seniorSurgeon = person;
                    continue;
                }

                if (person.outRanksUsingSkillTiebreaker(campaign,
                      seniorSurgeon)) {
                    seniorSurgeon = person;
                }
            }
        }

        return seniorSurgeon;
    }

    /**
     * Populates maps of injuries relevant to replacement surgery, both by individual location and by primary location,
     * to drive the UI and paper doll overlay.
     *
     * @param injuries the list of injuries on the patient
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void gatherRelevantInjuries(List<Injury> injuries) {
        injuries.sort(Comparator.comparing(Injury::getName)); // consistent order
        for (Injury injury : injuries) {
            BodyLocation location = injury.getLocation();
            for (BodyLocation mappedLocation : BodyLocation.values()) {
                if (VALID_BODY_LOCATIONS.contains(mappedLocation)) {
                    boolean isSameLocation = location.equals(mappedLocation);
                    boolean childOf = location.isChildOf(mappedLocation);

                    // Head and chest are special cases, as 'prosthetics' used
                    // there won't remove all injuries in that location,
                    // just the localized ones
                    boolean locationIsHead = mappedLocation.equals(HEAD);
                    boolean locationIsChest = mappedLocation.equals(CHEST);
                    if (isSameLocation
                              || (childOf && !(locationIsHead || locationIsChest))) {
                        // If a BodyLocation is the child of multiple valid
                        // locations, we want it added to each, so we don't
                        // break after finding one match
                        relevantInjuries
                              .computeIfAbsent(mappedLocation,
                                    k -> new ArrayList<>())
                              .add(injury);
                    }
                }

                if (PRIMARY_LOCATIONS.contains(mappedLocation)) {
                    if (location.isImmediateChildOf(mappedLocation) || location.equals(mappedLocation)) {
                        injuriesMappedToPrimaryLocations
                              .computeIfAbsent(mappedLocation,
                                    k -> new ArrayList<>())
                              .add(injury);
                    }
                }
            }
        }
    }

    /**
     * Builds the list of available prosthetic treatment options for each valid body location and populates
     * {@link #treatmentOptions}.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void gatherTreatmentOptions() {
        for (BodyLocation location : VALID_BODY_LOCATIONS) {
            List<ProstheticType> eligibleTreatments = getEligibleTreatments(location);
            treatmentOptions.put(location, eligibleTreatments);
        }
    }

    /**
     * Determines which prosthetic types are eligible for a given body location based on their configured allowed
     * locations.
     *
     * @param injuryLocation the location being evaluated
     *
     * @return a list of prosthetic types that can treat the location
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ProstheticType> getEligibleTreatments(
          BodyLocation injuryLocation) {
        List<ProstheticType> eligibleTreatments = new ArrayList<>();
        for (ProstheticType type : ProstheticType.values()) {
            if (type.getEligibleLocations().contains(injuryLocation)) {
                eligibleTreatments.add(type);
            }
        }
        return eligibleTreatments;
    }

    /**
     * Returns the currently selected prosthetic treatment for each body location that has a non-null selection in the
     * UI.
     *
     * @return a map of body location to chosen {@link ProstheticType}
     *
     * @author Illiani
     * @since 0.50.10
     */
    public Map<BodyLocation, ProstheticType> getSelectedTreatments() {
        Map<BodyLocation, ProstheticType> selections = new HashMap<>();
        boolean isPlanetside = campaign.getLocation().isOnPlanet();
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();
        for (Map.Entry<BodyLocation, JComboBox<ProstheticType>> entry :
              treatmentSelections.entrySet()) {
            ProstheticType selectedTreatment = (ProstheticType) entry.getValue().getSelectedItem();

            String exclusions = getExclusions(isPlanetside, selectedTreatment, campaignFaction, today);
            if (selectedTreatment != null && exclusions.isBlank()) {
                selections.put(entry.getKey(), selectedTreatment);
            }
        }
        return selections;
    }

    /**
     * Loads default male and female paper dolls and sets up the action listener used to display injury details when the
     * user clicks on a body location.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void paperDoll() {
        // Preload default paper dolls
        try (InputStream fis = new FileInputStream(campaign.getApp()
                                                         .getIconPackage()
                                                         .getGuiElement(MALE_PAPER_DOLL))) {
            defaultMaleDoll = new PaperDoll(fis);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        try (InputStream fis = new FileInputStream(campaign.getApp()
                                                         .getIconPackage()
                                                         .getGuiElement(FEMALE_PAPER_DOLL))) {
            defaultFemaleDoll = new PaperDoll(fis);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        dollActionListener = ae -> {
            final BodyLocation bodyLocation =
                  BodyLocation.of(ae.getActionCommand());
            final boolean locationPicked =
                  !bodyLocation.locationName().isEmpty();
            Point mousePos = doll.getMousePosition();

            // getMousePosition() can return null if the mouse isn’t over the component (or in some edge timing cases).
            if (mousePos == null) {
                return;
            }

            JPopupMenu popup = new JPopupMenu();
            if (locationPicked) {
                JLabel header = new JLabel(
                      Utilities.capitalize(bodyLocation.locationName()));
                header.setFont(UIManager.getDefaults()
                                     .getFont("Menu.font")
                                     .deriveFont(Font.BOLD));
                popup.add(header);
                popup.addSeparator();

                if (injuriesMappedToPrimaryLocations.containsKey(
                      bodyLocation)) {
                    for (Injury injury :
                          injuriesMappedToPrimaryLocations.get(
                                bodyLocation)) {
                        popup.add(injury.getName());
                    }
                }
            }
            Dimension popupSize = popup.getPreferredSize();
            popup.show(doll,
                  (int) (mousePos.getX() - popupSize.getWidth())
                        + PADDING,
                  (int) mousePos.getY() - PADDING);
        };
    }

    /**
     * Populates the right-hand panel with a paper doll representation of the patient, including highlighted injuries
     * and interactive click handlers.
     *
     * @param panel the panel to populate with the paper doll
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void fillDoll(JPanel panel) {
        panel.removeAll();

        if (null != doll) {
            doll.removeActionListener(dollActionListener);
        }
        doll = patient.getGender().isMale()
                     ? defaultMaleDoll
                     : defaultFemaleDoll;

        int dollWidth = scaleForGUI(200);
        int dollHeight = scaleForGUI(600);
        doll.setSize(dollWidth, dollHeight);
        doll.setPreferredSize(new Dimension(dollWidth, dollHeight));

        doll.clearLocColors();
        doll.clearLocTags();
        doll.setHighlightColor(new Color(170, 170, 255));
        PRIMARY_LOCATIONS.forEach(bodyLocation -> {
            if (patient.isLocationMissing(bodyLocation)
                      && !patient.isLocationMissing(bodyLocation.getParent())) {
                doll.setLocTag(bodyLocation, "lost");
            } else if (!patient.isLocationMissing(bodyLocation)) {
                InjuryLevel level = MedicalViewDialog.getMaxInjuryLevel(bodyLocation, injuriesMappedToPrimaryLocations);
                Color color = switch (level) {
                    case CHRONIC -> new Color(255, 204, 255, 128); // 50% alpha
                    case DEADLY -> new Color(Color.RED.getRed(),
                          Color.RED.getGreen(), Color.RED.getBlue(), 128);
                    case MAJOR -> new Color(Color.ORANGE.getRed(),
                          Color.ORANGE.getGreen(),
                          Color.ORANGE.getBlue(), 128);
                    case MINOR -> new Color(Color.YELLOW.getRed(),
                          Color.YELLOW.getGreen(),
                          Color.YELLOW.getBlue(), 128);
                    default -> null;
                };
                doll.setLocColor(bodyLocation, color);
            }
        });

        doll.addActionListener(dollActionListener);
        panel.add(doll);
    }

    /**
     * Forces the preferences for this dialog to be tracked in MekHQ instead of MegaMek, allowing its position and size
     * to be remembered between runs.
     *
     * @param dialog the dialog whose preferences are being managed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences =
                  MekHQ.getMHQPreferences()
                        .forClass(AdvancedReplacementLimbDialog.class);
            dialog.setName("AdvancedReplacementLimbDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
