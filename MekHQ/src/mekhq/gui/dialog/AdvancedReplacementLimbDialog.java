package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.COSMETIC_SURGERY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;

public class AdvancedReplacementLimbDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedReplacementLimbDialog";

    private static final List<BodyLocation> VALID_BODY_LOCATIONS = List.of(LEFT_ARM, RIGHT_ARM, LEFT_HAND, RIGHT_HAND,
          LEFT_LEG, RIGHT_LEG, LEFT_FOOT, RIGHT_FOOT, EYES, EARS, HEART, LUNGS, ORGANS);

    private final Campaign campaign;
    private final Person surgeon;
    private final List<Injury> relevantInjuries = new ArrayList<>();
    private final Map<Injury, List<ProstheticType>> treatmentOptions = new HashMap<>();
    private final Map<Injury, JComboBox<ProstheticType>> treatmentSelections = new HashMap<>();
    private boolean wasConfirmed = false;

    public AdvancedReplacementLimbDialog(Campaign campaign, Person patient) {
        this.campaign = campaign;
        surgeon = getSurgeon(campaign.getDoctors());

        gatherRelevantInjuries(patient.getInjuries());
        if (relevantInjuries.isEmpty()) {
            // There is nothing to heal
            return;
        }

        gatherTreatmentOptions();

        initializeUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create main panel for injuries and treatments
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        // Add injury labels and treatment comboboxes
        for (int i = 0; i < relevantInjuries.size(); i++) {
            Injury injury = relevantInjuries.get(i);

            // Injury label
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            JLabel injuryLabel = new JLabel(injury.getName());
            mainPanel.add(injuryLabel, gridBagConstraints);

            // ProstheticType combobox
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            List<ProstheticType> options = treatmentOptions.get(injury);
            JComboBox<ProstheticType> treatmentComboBox = createTreatmentComboBox(options);
            treatmentSelections.put(injury, treatmentComboBox);
            mainPanel.add(treatmentComboBox, gridBagConstraints);
        }

        // Wrap main panel in scroll pane in case of many injuries
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::onCancel);

        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(this::onConfirm);

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
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
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                boolean enabled = true;
                String tooltip;

                if (value == null) {
                    setText("None");
                    tooltip = "No treatment selected";
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

        // Add listener to update tooltip based on selection
        comboBox.addActionListener(e -> {
            ProstheticType selected = (ProstheticType) comboBox.getSelectedItem();
            if (selected == null) {
                comboBox.setToolTipText("No treatment selected");
            } else {
                String baseTooltip = selected.getTooltip(gameYear);
                baseTooltip += getExclusions(isOnPlanet, selected, gameYear);
                comboBox.setToolTipText(wordWrap(baseTooltip));
            }
        });

        // Set initial tooltip
        comboBox.setToolTipText("No treatment selected");

        return comboBox;
    }

    private String getExclusions(boolean isOnPlanet, ProstheticType selected, int gameYear) {
        String tooltip = "";
        // Check if selection should be disabled
        if (!isOnPlanet && selected.getProstheticType() > 2) {
            tooltip += "<br>- Advanced prosthetics require planetary facilities.";
        }

        if (!selected.isAvailableToFaction(campaign.getFaction())) {
            tooltip += "<br>- Unavailable to current campaign faction.";
        }

        if (!selected.isAvailableInCurrentLocation(campaign.getLocation(), campaign.getLocalDate())) {
            tooltip += "<br>- Unavailable in current location due to planetary Tech Level.";
        }

        if (selected.getCost(gameYear) == null) {
            tooltip += "<br>- Unavailable for purchase in current game year.";
        }
        return tooltip;
    }

    private void onCancel(ActionEvent e) {
        wasConfirmed = false;
        dispose();
    }

    private void onConfirm(ActionEvent e) {
        wasConfirmed = true;
        dispose();
    }

    public boolean isWasConfirmed() {
        return wasConfirmed;
    }

    public Map<Injury, ProstheticType> getSelectedTreatments() {
        Map<Injury, ProstheticType> selections = new HashMap<>();
        for (Map.Entry<Injury, JComboBox<ProstheticType>> entry : treatmentSelections.entrySet()) {
            ProstheticType selectedTreatment = (ProstheticType) entry.getValue().getSelectedItem();
            selections.put(entry.getKey(), selectedTreatment);
        }
        return selections;
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
        for (Injury injury : injuries) {
            InjuryType injuryType = injury.getType();

            // Skip non-Alt Advanced Medical injuries
            if (!injuryType.getKey().contains("alt:")) {
                continue;
            }

            // Skip prosthetic injuries
            if (injuryType.getSubType().isProsthetic()) {
                continue;
            }

            // Skip non-permanent injuries
            if (!injury.isPermanent()) {
                continue;
            }

            // Add burn injuries
            if (injury.getSubType().isBurn()) {
                relevantInjuries.add(injury);
                continue;
            }

            // Add injuries with valid body locations
            BodyLocation location = getBodyLocation(injury);
            if (VALID_BODY_LOCATIONS.contains(location)) {
                relevantInjuries.add(injury);
            }
        }
    }

    private void gatherTreatmentOptions() {
        for (Injury injury : relevantInjuries) {
            InjuryType injuryType = injury.getType();
            if (injuryType.getSubType().isBurn()) {
                treatmentOptions.put(injury, List.of(COSMETIC_SURGERY));
                continue;
            }

            BodyLocation injuryLocation = getBodyLocation(injury);
            List<ProstheticType> eligibleTreatments = getEligibleTreatments(injuryLocation);
            treatmentOptions.put(injury, eligibleTreatments);
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

    private static BodyLocation getBodyLocation(Injury injury) {
        BodyLocation injuryLocation = injury.getLocation();
        if (injuryLocation.isChildOf(LEFT_HAND)) {
            injuryLocation = LEFT_HAND;
        } else if (injuryLocation.isChildOf(RIGHT_HAND)) {
            injuryLocation = RIGHT_HAND;
        } else if (injuryLocation.isChildOf(LEFT_FOOT)) {
            injuryLocation = LEFT_FOOT;
        } else if (injuryLocation.isChildOf(RIGHT_FOOT)) {
            injuryLocation = RIGHT_FOOT;
        } else if (injuryLocation.Parent() != null) {
            injuryLocation = injuryLocation.Parent();
        }
        return injuryLocation;
    }
}


