package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
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
    private final Person surgeon; // can be null
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
    private boolean wasConfirmed = false;
    private JLabel summaryLabel;

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
        tutorialPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(0, PADDING, PADDING / 2, PADDING);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        // Add injury labels and treatment comboboxes
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

        // Add left container to center container
        centerContainer.add(leftContainer, BorderLayout.CENTER);

        // Create right panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        rightPanel.setPreferredSize(scaleForGUI(250, 0)); // Set preferred width, height will stretch

        // Add content to right panel
        fillDoll(rightPanel);

        centerContainer.add(rightPanel, BorderLayout.EAST);

        // Wrap entire center container in scroll pane
        JScrollPane scrollPane = new JScrollPane(centerContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Create summary panel above buttons
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING / 2, PADDING));
        summaryLabel = new JLabel(" "); // Initialize with empty space to maintain height
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

        // Create bottom container to hold summary and buttons
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(summaryPanel, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);

        // Initialize summary
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
                      surgeon.getFullTitle()));
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
                int totalLevel = surgerySkill.getTotalSkillLevel(surgeon.getSkillModifierData());
                isUseLocalSurgeon = totalLevel >= surgeryLevelNeeded;
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

    private void onConfirm(ActionEvent e) {
        wasConfirmed = true;
        dispose();
    }

    public boolean isWasConfirmed() {
        return wasConfirmed;
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
                    case CHRONIC -> new Color(255, 204, 255);
                    case DEADLY -> Color.RED;
                    case MAJOR -> Color.ORANGE;
                    case MINOR -> Color.YELLOW;
                    default -> null;
                };
                doll.setLocColor(bodyLocation, color);
            }
        });

        doll.addActionListener(dollActionListener);
        panel.add(doll);
        panel.add(Box.createVerticalGlue());
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


