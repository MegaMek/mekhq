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
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.CLONED_LIMB_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.COSMETIC_SURGERY_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.FAILED_SURGERY_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.REPLACEMENT_LIMB_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.REPLACEMENT_ORGAN_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.COSMETIC_SURGERY;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
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
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryEntryDialog;
import mekhq.gui.view.PaperDoll;

public class AdvancedReplacementLimbDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AdvancedReplacementLimbDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedReplacementLimbDialog";

    private static final int PADDING = scaleForGUI(10);
    private static final Dimension MAXIMUM_DIALOG_SIZE = scaleForGUI(600, 900);
    private static final String MALE_PAPER_DOLL = "default_male_paperdoll";
    private static final String FEMALE_PAPER_DOLL = "default_female_paperdoll";

    // The order here is important and any changes will be reflected in the gui
    private static final List<BodyLocation> VALID_BODY_LOCATIONS = List.of(
          HEAD,
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
          LEFT_LEG,
          RIGHT_LEG,
          LEFT_FOOT,
          RIGHT_FOOT
    );

    private final Campaign campaign;
    private final Person patient;
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

    private record PlannedSurgery(ProstheticType type, BodyLocation location) {
        public String getLabel() {
            return type.toString() + "-" + location.locationName();
        }
    }

    public AdvancedReplacementLimbDialog(Campaign campaign, Person patient) {
        this.patient = patient;
        this.campaign = campaign;
        surgeon = getSurgeon(campaign.getDoctors()); // can return null

        gatherRelevantInjuries(patient.getInjuries());
        gatherTreatmentOptions();
        paperDoll();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initializeUI();
        pack();
        setMaximumSize(MAXIMUM_DIALOG_SIZE);
        setLocationRelativeTo(null);
        setModal(true);
        setPreferences(this); // Must be before setVisible
        setVisible(true);
    }

    private void initializeUI() {
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
        gridBagConstraints.insets = new Insets(0, PADDING, PADDING / 2, PADDING);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        // Add location labels and treatment combos
        int i = 0;
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
        add(summaryPanel, BorderLayout.SOUTH);

        // Create button panel at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(PADDING / 2, PADDING, PADDING, PADDING));

        RoundedJButton cancelButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "AdvancedReplacementLimbDialog.button.cancel"));
        cancelButton.addActionListener(evt -> dispose());

        RoundedJButton documentationButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "AdvancedReplacementLimbDialog.button.documentation"));
        documentationButton.addActionListener(this::onDocumentation);

        confirmButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "AdvancedReplacementLimbDialog.button.confirm"));
        confirmButton.addActionListener(this::onConfirm);

        buttonPanel.add(cancelButton);
        buttonPanel.add(documentationButton);
        buttonPanel.add(confirmButton);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(summaryPanel, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);

        // Initialize summary (and other important values)
        updateSummary();
    }

    private JComboBox<ProstheticType> createTreatmentComboBox(List<ProstheticType> options) {
        int gameYear = campaign.getGameYear();
        boolean isOnPlanet = campaign.getLocation().isOnPlanet();

        JComboBox<ProstheticType> comboBox = new JComboBox<>();

        // Add null option first
        comboBox.addItem(null);

        // Add all available treatments
        for (ProstheticType treatment : options) {
            comboBox.addItem(treatment);
        }

        // Custom renderer to display "None" for null option and disable items based on location
        String defaultTooltip = getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.combo.none.tooltip");
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                boolean enabled = true;
                String tooltip;

                if (value == null) {
                    setText(getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.combo.none.label"));
                    tooltip = defaultTooltip;
                } else {
                    ProstheticType type = (ProstheticType) value;
                    setText(type.toString());

                    // Build tooltip with base info and exclusions
                    String baseTooltip = type.getTooltip(gameYear);
                    String exclusions = getExclusions(isOnPlanet, type, gameYear);

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
                String baseTooltip = selected.getTooltip(gameYear);
                baseTooltip += getExclusions(isOnPlanet, selected, gameYear);
                comboBox.setToolTipText(wordWrap(baseTooltip));
            }
            updateSummary(); // Update summary when selection changes
        });

        // Set initial tooltip
        comboBox.setToolTipText(defaultTooltip);

        return comboBox;
    }

    private void updateSummary() {
        if (summaryLabel == null) {
            return;
        }

        totalCost = Money.zero(); // Reset cost
        surgeryLevelNeeded = 0; // Reset surgery level needs
        int selectedCount = getSelectedTreatments().size();

        getSurgeryCostAndSkillRequirements();

        isUseLocalSurgeon();
        if (isUseLocalSurgeon) {
            totalCost = totalCost.multipliedBy(10);
        }

        confirmButton.setEnabled(campaign.getFunds().isGreaterOrEqualThan(totalCost));

        List<String> summary = new ArrayList<>();
        if (selectedCount > 0) {
            summary.add(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.status.selected",
                  selectedCount));

            summary.add(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.status.difficulty",
                  surgeryLevelNeeded));

            if (isUseLocalSurgeon) {
                if (campaign.getLocation().isOnPlanet()) {
                    summary.add(getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.status.localSurgeon"));
                } else {
                    summary.add(getTextAt(RESOURCE_BUNDLE,
                          "AdvancedReplacementLimbDialog.status.localSurgeon.transit"));
                }
            } else {
                summary.add(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.status.surgeon",
                      surgeon.getFullTitle(), surgeryLevelNeeded));
            }

            if (totalCost.isPositive()) {
                summary.add(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.status.total",
                      totalCost.toAmountString()));
            }
        } else {
            summary.add(" "); // These just ensure the gui stays at the same height
            summary.add(" ");
            summary.add(" ");
            summary.add(" ");
            summary.add(" ");
        }

        summaryLabel.setText("<html>" + String.join("<br>", summary) + "</html>");
    }

    private void getSurgeryCostAndSkillRequirements() {
        for (ProstheticType surgeryType : getSelectedTreatments().values()) {
            int neededSurgeryLevel = surgeryType.getSurgeryLevel();
            if (neededSurgeryLevel > surgeryLevelNeeded) {
                surgeryLevelNeeded = neededSurgeryLevel;
            }

            Money cost = surgeryType.getCost(campaign.getGameYear());
            if (cost != null) {
                totalCost = totalCost.plus(cost);
            }
        }
    }

    private void isUseLocalSurgeon() {
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

    private String getExclusions(boolean isOnPlanet, ProstheticType selected, int gameYear) {
        String tooltip = "";
        // Check if selection should be disabled
        if (!isOnPlanet && selected.getProstheticType() > 2) {
            tooltip += getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.exclusions.planet");
        }

        if (!selected.isAvailableToFaction(campaign.getFaction())) {
            tooltip += getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.exclusions.faction");
        }

        if (!selected.isAvailableInCurrentLocation(campaign.getLocation(), campaign.getLocalDate())) {
            tooltip += getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.exclusions.tech");
        }

        if (selected.getCost(gameYear) == null) {
            tooltip += getTextAt(RESOURCE_BUNDLE, "AdvancedReplacementLimbDialog.exclusions.year");
        }
        return tooltip;
    }

    private void onDocumentation(ActionEvent e) {
        GlossaryEntry glossaryEntry = GlossaryEntry.FATIGUE;

        try {
            new NewGlossaryEntryDialog(this, glossaryEntry);
        } catch (Exception ex) {
            LOGGER.error("Failed to open Glossary Entry", ex);
        }
    }

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
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                  "AdvancedReplacementLimbDialog.report.successful",
                  String.join(", ", successfulSurgeries.stream()
                                          .map(PlannedSurgery::getLabel)
                                          .toList())
            ));
        }

        if (!unsuccessfulSurgeries.isEmpty()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                  "AdvancedReplacementLimbDialog.report.unsuccessful",
                  String.join(", ", unsuccessfulSurgeries.stream()
                                          .map(PlannedSurgery::getLabel)
                                          .toList())
            ));
        }

        // Check to see if the patient died on the operating table
        checkForDeath();
    }

    private void payForSurgeries() {
        Finances finances = campaign.getFinances();
        LocalDate today = campaign.getLocalDate();
        finances.debit(TransactionType.REPAIRS, today, totalCost, getFormattedTextAt(RESOURCE_BUNDLE,
              "AdvancedReplacementLimbDialog.transaction", patient.getFullTitle()));
    }

    private void performSurgery(PlannedSurgery surgery, boolean useKinderMode,
          List<PlannedSurgery> successfulSurgeries) {
        BodyLocation location = surgery.location;
        ProstheticType type = surgery.type;

        if (successfulSurgeries.contains(surgery)) {
            // Remove injuries based on the surgery type
            boolean isBurnRemovalOnly = type == COSMETIC_SURGERY;
            for (Injury injury : relevantInjuries.getOrDefault(location, new ArrayList<>())) {
                if (injury != null && (injury.getSubType().isBurn() || !isBurnRemovalOnly)) {
                    patient.removeInjury(injury);
                }
            }

            // Add the new surgery 'injury'. This is a semi-permanent record of the surgery on the character
            InjuryType injuryType = type.getInjuryType();
            Injury injury = injuryType.newInjury(campaign, patient, location, 0);
            adjustForKinderMode(useKinderMode, injury);
            patient.addInjury(injury);

            // Add recovery period injuries
            InjuryType recoveryInjuryType = getRecoveryInjuryType(surgery);
            Injury recoveryInjury = recoveryInjuryType.newInjury(campaign, patient, INTERNAL, 1);
            adjustForKinderMode(useKinderMode, recoveryInjury);
            patient.addInjury(recoveryInjury);
        } else {
            // Add failed surgery injury
            Injury recoveryInjury = FAILED_SURGERY_RECOVERY.newInjury(campaign, patient, INTERNAL, 1);
            adjustForKinderMode(useKinderMode, recoveryInjury);
            patient.addInjury(recoveryInjury);
        }
    }

    private void checkForDeath() {
        int deathThreshold = 5;
        for (Injury injury : patient.getInjuries()) {
            deathThreshold -= injury.getHits();
        }

        if (deathThreshold < 0) {
            patient.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
        }
    }

    private static InjuryType getRecoveryInjuryType(PlannedSurgery surgery) {
        ProstheticType type = surgery.type;
        if (type == COSMETIC_SURGERY) {
            return COSMETIC_SURGERY_RECOVERY;
        } else if (surgery.location.isLimb()) {
            if (type.getProstheticType() >= 6) {
                return CLONED_LIMB_RECOVERY;
            } else {
                return REPLACEMENT_LIMB_RECOVERY;
            }
        } else {
            return REPLACEMENT_ORGAN_RECOVERY;
        }
    }

    private static void adjustForKinderMode(boolean useKinderMode, Injury injury) {
        if (useKinderMode) {
            int originalRecoveryTime = injury.getOriginalTime();
            int newRecoveryTime = (int) ceil(originalRecoveryTime / 2.0);
            injury.setOriginalTime(newRecoveryTime);
            injury.setTime(newRecoveryTime);
        }
    }

    private void getLocalSurgeon() {
        if (isUseLocalSurgeon) {
            surgeon = new Person(campaign);
            surgeon.addSkill(S_SURGERY, surgeryLevelNeeded + 1, 0);
            surgeon.setEdge(1); // We don't want Local Surgeons to be completely worthless
            surgeon.setCurrentEdge(1);
        }
    }

    private void performSurgerySkillChecks(List<PlannedSurgery> prioritizedSurgeries,
          List<PlannedSurgery> successfulSurgeries, List<PlannedSurgery> unsuccessfulSurgeries) {
        for (PlannedSurgery surgery : new ArrayList<>(prioritizedSurgeries)) {
            SkillCheckUtility skillCheckUtility = new SkillCheckUtility(surgeon, S_SURGERY, List.of(), 0, true, false);
            campaign.addReport(skillCheckUtility.getResultsText());
            if (skillCheckUtility.isSuccess()) {
                successfulSurgeries.add(surgery);
            } else {
                unsuccessfulSurgeries.add(surgery);
            }
        }
    }

    private List<PlannedSurgery> getPrioritizedSurgeries() {
        int gameYear = campaign.getGameYear();
        Map<BodyLocation, ProstheticType> scheduledSurgeries = getSelectedTreatments();

        List<PlannedSurgery> prioritizedSurgeries = new ArrayList<>();
        for (Map.Entry<BodyLocation, ProstheticType> entry : scheduledSurgeries.entrySet()) {
            prioritizedSurgeries.add(new PlannedSurgery(entry.getValue(), entry.getKey()));
        }

        prioritizedSurgeries.sort(
              Comparator.<PlannedSurgery>comparingInt(s -> s.type().getSurgeryLevel())
                    .reversed() // highest surgery level first
                    .thenComparing(
                          // We shouldn't hit `null` at this point, as any null selections should have been filtered out
                          s -> Objects.requireNonNull(s.type().getCost(gameYear)),
                          Comparator.reverseOrder() // highest cost first
                    )
        );

        return prioritizedSurgeries;
    }

    private @Nullable Person getSurgeon(List<Person> activePersonnel) {
        Person seniorSurgeon = null;
        for (Person person : activePersonnel) {
            if (person.isDoctor()) {
                if (seniorSurgeon == null) {
                    seniorSurgeon = person;
                    continue;
                }

                if (person.outRanksUsingSkillTiebreaker(campaign, seniorSurgeon)) {
                    seniorSurgeon = person;
                }
            }
        }

        return seniorSurgeon;
    }

    private void gatherRelevantInjuries(List<Injury> injuries) {
        injuries.sort(Comparator.comparing(Injury::getName)); // we want a consistent order
        for (Injury injury : injuries) {
            BodyLocation location = injury.getLocation();
            for (BodyLocation mappedLocation : BodyLocation.values()) {
                if (VALID_BODY_LOCATIONS.contains(mappedLocation)) {
                    boolean isSameLocation = location.equals(mappedLocation);
                    boolean childOf = location.isChildOf(mappedLocation);

                    // Head and chest are special cases, as 'prosthetics' used there won't remove all injuries in that
                    // location, just the localized ones
                    boolean locationIsHead = mappedLocation.equals(HEAD);
                    boolean locationIsChest = mappedLocation.equals(CHEST);
                    if (isSameLocation || (childOf && !(locationIsHead || locationIsChest))) {
                        // If a BodyLocation is the child of multiple valid locations, we want it added to each, so we
                        // don't break after finding one match
                        relevantInjuries.computeIfAbsent(mappedLocation, k -> new ArrayList<>()).add(injury);
                    }
                }

                if (PRIMARY_LOCATIONS.contains(mappedLocation)) {
                    if (location.isImmediateChildOf(mappedLocation)) {
                        injuriesMappedToPrimaryLocations.computeIfAbsent(mappedLocation, k -> new ArrayList<>())
                              .add(injury);
                    }
                }
            }
        }
    }

    private void gatherTreatmentOptions() {
        for (BodyLocation location : VALID_BODY_LOCATIONS) {
            List<ProstheticType> eligibleTreatments = getEligibleTreatments(location);
            treatmentOptions.put(location, eligibleTreatments);
        }
    }

    private static List<ProstheticType> getEligibleTreatments(BodyLocation injuryLocation) {
        List<ProstheticType> eligibleTreatments = new ArrayList<>();
        for (ProstheticType type : ProstheticType.values()) {
            if (type.getEligibleLocations().contains(injuryLocation)) {
                eligibleTreatments.add(type);
            }
        }
        return eligibleTreatments;
    }

    public Map<BodyLocation, ProstheticType> getSelectedTreatments() {
        Map<BodyLocation, ProstheticType> selections = new HashMap<>();
        for (Map.Entry<BodyLocation, JComboBox<ProstheticType>> entry : treatmentSelections.entrySet()) {
            ProstheticType selectedTreatment = (ProstheticType) entry.getValue().getSelectedItem();

            if (selectedTreatment != null) {
                selections.put(entry.getKey(), selectedTreatment);
            }
        }
        return selections;
    }

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
            final BodyLocation bodyLocation = BodyLocation.of(ae.getActionCommand());
            final boolean locationPicked = !bodyLocation.locationName().isEmpty();
            Point mousePos = doll.getMousePosition();
            JPopupMenu popup = new JPopupMenu();
            if (locationPicked) {
                JLabel header = new JLabel(Utilities.capitalize(bodyLocation.locationName()));
                header.setFont(UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.BOLD));
                popup.add(header);
                popup.addSeparator();

                if (injuriesMappedToPrimaryLocations.containsKey(bodyLocation)) {
                    for (Injury injury : injuriesMappedToPrimaryLocations.get(bodyLocation)) {
                        popup.add(injury.getName());
                    }
                }
            }
            Dimension popupSize = popup.getPreferredSize();
            popup.show(doll, (int) (mousePos.getX() - popupSize.getWidth()) + PADDING, (int) mousePos.getY() - PADDING);
        };
    }

    private void fillDoll(JPanel panel) {
        panel.removeAll();

        if (null != doll) {
            doll.removeActionListener(dollActionListener);
        }
        doll = patient.getGender().isMale() ? defaultMaleDoll : defaultFemaleDoll;

        int dollWidth = scaleForGUI(200);  // Adjust this value
        int dollHeight = scaleForGUI(600); // Adjust this value
        doll.setSize(dollWidth, dollHeight);
        doll.setPreferredSize(new Dimension(dollWidth, dollHeight));

        doll.clearLocColors();
        doll.clearLocTags();
        doll.setHighlightColor(new Color(170, 170, 255));
        PRIMARY_LOCATIONS.forEach(bodyLocation -> {
            if (patient.isLocationMissing(bodyLocation) && !patient.isLocationMissing(bodyLocation.Parent())) {
                doll.setLocTag(bodyLocation, "lost");
            } else if (!patient.isLocationMissing(bodyLocation)) {
                InjuryLevel level = getMaxInjuryLevel(bodyLocation);
                Color color = switch (level) {
                    case CHRONIC -> new Color(255, 204, 255, 128); // 50% alpha
                    case DEADLY -> new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), 128);
                    case MAJOR ->
                          new Color(Color.ORANGE.getRed(), Color.ORANGE.getGreen(), Color.ORANGE.getBlue(), 128);
                    case MINOR ->
                          new Color(Color.YELLOW.getRed(), Color.YELLOW.getGreen(), Color.YELLOW.getBlue(), 128);
                    default -> null;
                };
                doll.setLocColor(bodyLocation, color);
            }
        });

        doll.addActionListener(dollActionListener);
        panel.add(doll);
    }

    private InjuryLevel getMaxInjuryLevel(BodyLocation bodyLocation) {
        InjuryLevel maxLevel = InjuryLevel.NONE;

        for (Injury injury : injuriesMappedToPrimaryLocations.getOrDefault(bodyLocation, new ArrayList<>())) {
            if (!injury.isHidden()) {
                if (injury.getLevel().ordinal() > maxLevel.ordinal()) {
                    maxLevel = injury.getLevel();
                }
            }
        }

        return maxLevel;
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AdvancedReplacementLimbDialog.class);
            dialog.setName("AdvancedReplacementLimbDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}


