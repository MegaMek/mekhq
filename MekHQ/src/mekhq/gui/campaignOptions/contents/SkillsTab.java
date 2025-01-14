/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.contents;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static java.util.Arrays.sort;
import static megamek.common.enums.SkillLevel.*;
import static mekhq.campaign.personnel.SkillType.isCombatSkill;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * SkillsTab is a component of the campaign options user interface that allows players
 * to configure the rules and costs associated with skills in their campaign.
 * <p>
 * This tab can be configured for either combat or support skills. It allows users to:
 * <p>
 *   <li>Set skill target numbers.</li>
 *   <li>Specify skill costs at different levels.</li>
 *   <li>Define milestones for the progression of skills by skill levels.</li>
 *   <li>Copy and paste configurations for skill settings.</li>
 * </p>
 * The interface is dynamically created to display all relevant skill options for the
 * selected tab (combat or support).
 * </p>
 */
public class SkillsTab {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private Map<String, JSpinner> allTargetNumbers;
    private Map<String, List<JLabel>> allSkillLevels;
    private Map<String, List<JSpinner>> allSkillCosts;
    private Map<String, List<JComboBox<SkillLevel>>> allSkillMilestones;
    private double storedTargetNumber = 0;
    private List<Double> storedValuesSpinners = new ArrayList<>();
    private List<SkillLevel> storedValuesComboBoxes = new ArrayList<>();

    private static final MMLogger logger = MMLogger.create(SkillsTab.class);

    /**
     * Constructs a new `SkillsTab` instance and initializes the necessary data
     * structures for managing skill configurations.
     */
    public SkillsTab() {
        initialize();
    }

    /**
     * Initializes the SkillsTab by setting up the necessary data structures and
     * default values for skill configuration.
     */
    private void initialize() {
        initializeGeneral();
    }

    /**
     * Sets up the general data structures needed for skill configuration in the
     * SkillsTab. This includes collections for tracking target numbers, costs,
     * and milestones for every skill.
     */
    private void initializeGeneral() {
        allTargetNumbers = new HashMap<>();
        allSkillLevels = new HashMap<>();
        allSkillCosts = new HashMap<>();
        allSkillMilestones = new HashMap<>();
        storedTargetNumber = 0;
        storedValuesSpinners = new ArrayList<>();
        storedValuesComboBoxes = new ArrayList<>();
    }

    /**
     * Creates the main panel for the SkillsTab UI.
     * <p>
     * Depending on the `isCombatTab` argument, it creates a tab for either combat
     * or support skills. The tab displays skill panels associated with the selected
     * category, allowing users to configure their target numbers, costs, and milestones.
     * </p>
     *
     * @param isCombatTab a boolean indicating whether this is a combat tab
     *                    (true for combat skills, false for support skills).
     * @return a {@link JPanel} containing the dynamically generated skill options
     *         for the selected tab.
     */
    public JPanel createSkillsTab(boolean isCombatTab) {
        // Header
        JPanel headerPanel;
        if (isCombatTab) {
            headerPanel = new CampaignOptionsHeaderPanel("CombatSkillsTab",
                getImageDirectory() + "logo_clan_diamond_sharks.png");
        } else {
            headerPanel = new CampaignOptionsHeaderPanel("SupportSkillsTab",
                getImageDirectory() + "logo_free_worlds_league.png");
        }

        // Contents
        String[] allSkills = SkillType.getSkillList();
        sort(allSkills);

        List<SkillType> relevantSkills = new ArrayList<>();
        for (String skillName : SkillType.getSkillList()) {
            SkillType skill = SkillType.getType(skillName);
            boolean isCombatSkill = isCombatSkill(skill);

            if (isCombatSkill == isCombatTab) {
                relevantSkills.add(skill);
            }
        }

        List<JPanel> skillPanels = new ArrayList<>();

        for (SkillType skill : relevantSkills) {
            JPanel skillPanel = createSkillPanel(skill);
            skillPanels.add(skillPanel);
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel(isCombatTab ?
            "CombatSkillsTab" : "SupportSkillsTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        JButton hideAllButton = new JButton(resources.getString("btnHideAll.text"));
        hideAllButton.addActionListener(e -> {
            setVisibleForAll(false);

            panel.revalidate();
            panel.repaint();
        });

        // Create a button to toggle the table
        JButton showAllButton = new JButton(resources.getString("btnDisplayAll.text"));
        showAllButton.addActionListener(e -> {
            setVisibleForAll(true);

            panel.revalidate();
            panel.repaint();
        });

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(showAllButton, layout);
        layout.gridx++;
        panel.add(hideAllButton, layout);

        layout.gridx = 0;
        layout.gridy++;
        int tableCounter = 0;
        for (int i = 0; i < 4; i++) {
            layout.gridy++;
            layout.gridx = 0;
            for (int j = 0; j < 5; j++) {
                if (tableCounter < skillPanels.size()) {
                    panel.add(skillPanels.get(tableCounter), layout);
                    layout.gridx++;
                }
                tableCounter++;
            }
        }

        // Create Parent Panel
        return createParentPanel(panel, "CombatSkillsTab");
    }

    /**
     * Toggles the visibility of all components related to skills in the SkillsTab UI.
     * <p>
     * This can be used to either hide or display all components for easier navigation
     * or cleaner representation of the tab.
     * </p>
     *
     * @param visible a boolean indicating whether to show or hide all components.
     */
    private void setVisibleForAll(boolean visible) {
        setVisibleForAll(allSkillLevels, visible);
        setVisibleForAll(allSkillCosts, visible);
        setVisibleForAll(allSkillMilestones, visible);
    }

    /**
     * Toggles the visibility of a specific category of components in the SkillsTab UI.
     * <p>
     * This method allows visibility control over target numbers, costs, and skill
     * level milestones based on the provided component map.
     * </p>
     *
     * @param componentsMap a map containing the components to show or hide.
     * @param visible        a boolean indicating whether to show or hide the components.
     * @param <T>            the type of the component to toggle visibility for
     *                       (e.g., {@link JSpinner}, {@link JComboBox}).
     */
    private <T extends JComponent> void setVisibleForAll(Map<String, List<T>> componentsMap,
                                                         boolean visible) {
        for (String SkillName : componentsMap.keySet()) {
            List<T> components = componentsMap.get(SkillName);
            if (components != null) {
                for (T component : components) {
                    component.setVisible(visible);
                }
            }
        }
    }

    /**
     * Creates a dynamic panel for configuring the specified skill.
     * <p>
     * Each skill panel includes controls for:
     * <p>
     *   <li>Setting the target number for the skill.</li>
     *   <li>Configuring costs of the skill at different levels.</li>
     *   <li>Defining milestones for skill progression (e.g., from Green to Veteran).</li>
     * </p>
     * Copy and paste buttons are available for transferring configurations between skills.
     * </p>
     *
     * @param skill the {@link SkillType} object representing the skill to be configured.
     * @return a {@link JPanel} containing the UI components for the given skill.
     */
    private JPanel createSkillPanel(SkillType skill) {
        String panelName = "SkillPanel" + skill.getName().replace(" ", "");

        // Create the target number spinner
        JLabel lblTargetNumber = new CampaignOptionsLabel("SkillPanelTargetNumber");
        JSpinner spnTargetNumber = new CampaignOptionsSpinner("SkillPanelTargetNumber",
            0, 0, 12, 1);
        allTargetNumbers.put(skill.getName(), spnTargetNumber);

        List<JLabel> labels = new ArrayList<>();
        List<JSpinner> spinners = new ArrayList<>();
        List<JComboBox<SkillLevel>> comboBoxes = new ArrayList<>();

        List<JLabel> skillLevels = new ArrayList<>();
        List<JSpinner> skillCosts = new ArrayList<>();
        List<JComboBox<SkillLevel>> skillMilestones = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            JLabel label = new CampaignOptionsLabel("SkillLevel" + i, null, true);
            label.setVisible(false);
            labels.add(label);
            skillLevels.add(label);

            JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + i,
                null, 0, -1, 9999, 1, true);
            spinner.setVisible(false);
            spinners.add(spinner);
            skillCosts.add(spinner);

            JComboBox<SkillLevel> comboBox = new JComboBox<>();
            comboBox.addItem(ULTRA_GREEN);
            comboBox.addItem(GREEN);
            comboBox.addItem(REGULAR);
            comboBox.addItem(VETERAN);
            comboBox.addItem(ELITE);
            comboBox.addActionListener(e -> milestoneActionListener(comboBoxes, comboBox));
            comboBox.setVisible(false);
            comboBoxes.add(comboBox);
            skillMilestones.add(comboBox);
        }

        allSkillLevels.put(skill.getName(), skillLevels);
        allSkillCosts.put(skill.getName(), skillCosts);
        allSkillMilestones.put(skill.getName(), skillMilestones);

        JButton copyButton = new JButton(resources.getString("btnCopy.text"));
        copyButton.addActionListener(e -> {
            storedTargetNumber = (Double) spnTargetNumber.getValue();

            storedValuesSpinners = new ArrayList<>();
            storedValuesComboBoxes = new ArrayList<>();

            for (int i = 0; i < labels.size(); i++) {
                storedValuesSpinners.add((Double) spinners.get(i).getValue());
                storedValuesComboBoxes.add((SkillLevel) comboBoxes.get(i).getSelectedItem());
            }
        });

        JButton pasteButton = new JButton(resources.getString("btnPaste.text"));
        pasteButton.addActionListener(e -> {
            spnTargetNumber.setValue(storedTargetNumber);

            for (int i = 0; i < labels.size(); i++) {
                spinners.get(i).setValue(storedValuesSpinners.get(i));
                comboBoxes.get(i).setSelectedItem(storedValuesComboBoxes.get(i));
            }
        });

        final JPanel panel = new CampaignOptionsStandardPanel(panelName, true, panelName);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        JButton toggleButton = new JButton(resources.getString("btnToggle.text"));
        toggleButton.addActionListener(e -> {
            for (JLabel label : labels) {
                label.setVisible(!label.isVisible());
            }

            for (JComboBox<SkillLevel> comboBox : comboBoxes) {
                comboBox.setVisible(!comboBox.isVisible());
            }

            for (JSpinner spinner : spinners) {
                spinner.setVisible(!spinner.isVisible());
            }
        });

        layout.gridy = 0;
        layout.gridx = 1;
        layout.gridwidth = 2;
        panel.add(toggleButton, layout);
        layout.gridy++;

        layout.gridy++;
        layout.gridx = 1;
        layout.gridwidth = 1;
        panel.add(copyButton, layout);
        layout.gridx++;
        panel.add(pasteButton, layout);

        layout.gridy++;
        layout.gridx = 1;
        panel.add(lblTargetNumber, layout);
        layout.gridx++;
        panel.add(spnTargetNumber, layout);

        for (int i = 0; i < labels.size(); i++) {
            layout.gridy++;
            layout.gridx = 0;
            layout.gridwidth = 1;
            panel.add(labels.get(i), layout);
            layout.gridx++;
            panel.add(spinners.get(i), layout);
            layout.gridx++;
            panel.add(comboBoxes.get(i), layout);
        }

        return panel;
    }

    /**
     * Action listener for milestone combo boxes to synchronize their values sequentially.
     * <p>
     * When the user changes the value of a milestone, subsequent milestones are
     * automatically adjusted to ensure logical skill progression (e.g., later
     * milestones cannot precede earlier ones). This method enforces such constraints.
     * </p>
     *
     * @param comboBoxes the list of combo boxes representing milestones for a single skill.
     * @param comboBox   the combo box that triggered the action.
     */
    private static void milestoneActionListener(List<JComboBox<SkillLevel>> comboBoxes, JComboBox<SkillLevel> comboBox) {
        int originIndex = comboBoxes.indexOf(comboBox);

        SkillLevel currentSelection = (SkillLevel) comboBox.getSelectedItem();

        if (currentSelection == null) {
            return;
        }

        if (originIndex != 0) {
            JComboBox<SkillLevel> previousComboBox = comboBoxes.get(originIndex - 1);
            SkillLevel previousSelection = (SkillLevel) previousComboBox.getSelectedItem();

            if (previousSelection != null) {
                if (previousSelection.ordinal() > currentSelection.ordinal()) {
                    comboBox.setSelectedItem(previousSelection);
                } else if (currentSelection.ordinal() > previousSelection.ordinal() + 1) {
                    comboBox.setSelectedItem(parseFromInteger(previousSelection.ordinal() + 1));
                }
            }
        } else {
            if (comboBox.getSelectedItem() != NONE) {
                comboBox.setSelectedItem(ULTRA_GREEN);
            }
        }

        if (originIndex < comboBoxes.size() - 1) {
            originIndex++;
            JComboBox<SkillLevel> nextComboBox = comboBoxes.get(originIndex);
            nextComboBox.setSelectedItem(comboBox.getSelectedItem());
        }
    }

    /**
     * Loads skill values from the default campaign options.
     * <p>
     * A version of this method without parameters that uses default skill values
     * defined in the campaign.
     * </p>
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(new HashMap<>());
    }

    /**
     * Loads skill values into the UI components from the campaign options.
     * <p>
     * The method populates the spinners, labels, and comboboxes for all skills
     * with their corresponding properties (e.g., target numbers, costs, and milestones)
     * retrieved from the campaign's configuration.
     * </p>
     *
     * @param presetSkillValues an optional map of preset skill values. If null
     *                          or empty, default skill values are used instead.
     */
    public void loadValuesFromCampaignOptions(Map<String, SkillType> presetSkillValues) {
        String[] skills = SkillType.getSkillList(); // default skills

        for (String skillName : skills) {
            // Fetch the skill, either from presetSkillValues or default
            SkillType skill = presetSkillValues.getOrDefault(skillName, SkillType.getType(skillName));

            // Skip outdated or missing skills
            if (allTargetNumbers.get(skillName) == null) {
                logger.info(String.format("Skipping outdated or missing skill: %s", skillName));
                continue;
            }

            // Update Target Number
            JSpinner spinner = allTargetNumbers.get(skillName);
            spinner.setValue(skill.getTarget());

            // Costs
            List<JSpinner> skillCosts = allSkillCosts.get(skillName);
            Integer[] costs = skill.getCosts();
            if (costs != null && skillCosts != null) {
                for (int i = 0; i < Math.min(costs.length, skillCosts.size()); i++) {
                    skillCosts.get(i).setValue(costs[i]);
                }
            }

            // Milestones
            List<JComboBox<SkillLevel>> milestones = allSkillMilestones.get(skillName);
            if (milestones != null) {
                int greenIndex = skill.getGreenLevel();
                int regularIndex = skill.getRegularLevel();
                int veteranIndex = skill.getVeteranLevel();
                int eliteIndex = skill.getEliteLevel();

                for (int i = 0; i < milestones.size(); i++) {
                    SkillLevel levelToSet = determineMilestoneLevel(i, greenIndex, regularIndex,
                        veteranIndex, eliteIndex);
                    milestones.get(i).setSelectedItem(levelToSet);

                    logger.debug(String.format("Updated milestone at index %d for skill: %s to %s",
                        i, skillName, levelToSet));
                }
            }
        }
    }

    /**
     * Determines the skill level milestone based on progress indices.
     * <p>
     * It evaluates whether the milestone falls under Green, Regular, Veteran, or Elite
     * levels, ensuring proper assignments for milestone thresholds.
     * </p>
     *
     * @param index        the position in the milestone sequence.
     * @param greenIndex   the index where Green begins.
     * @param regularIndex the index where Regular begins.
     * @param veteranIndex the index where Veteran begins.
     * @param eliteIndex   the index where Elite begins.
     * @return the corresponding {@link SkillLevel} for the given milestone.
     */
    private SkillLevel determineMilestoneLevel(int index, int greenIndex, int regularIndex,
                                               int veteranIndex, int eliteIndex) {
        if (index < greenIndex) {
            return ULTRA_GREEN;
        }
        if (index < regularIndex) {
            return GREEN;
        }
        if (index < veteranIndex) {
            return REGULAR;
        }
        if (index < eliteIndex) {
            return VETERAN;
        }
        return ELITE;
    }

    /**
     * Transfers the configured skill values from the SkillsTab UI into the campaign's
     * underlying data model.
     * <p>
     * The method iterates through all configurable skills, updating the campaign
     * with the configured target numbers, costs, and milestones for each skill.
     * </p>
     *
     * @param presetSkills an optional map of preset skill values. Overrides default
     *                     values if provided. Null values will use the campaign's
     *                     default values.
     */
    public void applyCampaignOptionsToCampaign(@Nullable Map<String, SkillType> presetSkills) {
        for (final String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (presetSkills != null) {
                type = presetSkills.get(skillName);
            }

            if (type == null) {
                logger.info(String.format("Skipping outdated or missing skill: %s", skillName));
                continue;
            }

            // Update Target Number
            updateTargetNumber(type);

            // Update Skill Costs
            updateSkillCosts(skillName);

            // Update Skill Milestones
            updateSkillMilestones(type);
        }
    }

    /**
     * Updates the target number for a given skill in the campaign based on the
     * corresponding user input from the SkillsTab UI.
     * <p>
     * The target number determines the difficulty level for the specified skill
     * in the campaign. This value is fetched from the associated spinner component
     * in the UI and is applied to the given {@link SkillType}.
     * </p>
     *
     * @param type the {@link SkillType} object representing the skill whose target
     *             number needs to be updated.
     */
    private void updateTargetNumber(SkillType type) {
        int targetNumber = (int) allTargetNumbers.get(type.getName()).getValue();
        type.setTarget(targetNumber);
    }

    /**
     * Updates the costs associated with a given skill based on the user input
     * from the SkillsTab UI.
     * <p>
     * For each level of the specified skill, the cost values are retrieved from
     * the corresponding spinner components in the UI and stored in the campaign's
     * configuration. The costs represent the resource requirements for acquiring
     * the skill at various levels.
     * </p>
     *
     * @param skillName the name of the skill whose cost values are to be updated.
     */
    private void updateSkillCosts(String skillName) {
        List<JSpinner> costs = allSkillCosts.get(skillName);

        for (int level = 0; level < costs.size(); level++) {
            int cost = (int) costs.get(level).getValue();
            SkillType.setCost(skillName, cost, level);
        }
    }

    /**
     * Updates the skill milestones for a given skill in the campaign
     * based on user input from the SkillsTab UI.
     * <p>
     * Milestones represent the thresholds required to reach
     * certain skill levels (e.g., Green, Regular, Veteran, Elite).
     * The method processes these values from the associated combo boxes
     * in the UI and applies them to the provided {@link SkillType}.
     * <p>
     * The method ensures logical milestone progression and assigns
     * default values if necessary.
     * </p>
     *
     * @param type the {@link SkillType} object representing the skill whose
     *             milestones are to be updated.
     */
    private void updateSkillMilestones(SkillType type) {
        List<JComboBox<SkillLevel>> skillMilestones = allSkillMilestones.get(type.getName());

        // These allow us to ensure the full array of milestones has been assigned
        type.setGreenLevel(skillMilestones.size() -4);
        type.setRegularLevel(skillMilestones.size() -3);
        type.setVeteranLevel(skillMilestones.size() -2);
        type.setEliteLevel(skillMilestones.size() -1);

        // Then we overwrite those insurance values with the actual values
        SkillLevel lastAssignment = ULTRA_GREEN;
        for (int i = 0; i < skillMilestones.size(); i++) {

            JComboBox<SkillLevel> milestoneCombo = skillMilestones.get(i);
            SkillLevel selectedSkillLevel = (SkillLevel) milestoneCombo.getSelectedItem();

            if (selectedSkillLevel != lastAssignment) {
                lastAssignment = selectedSkillLevel;

                if (selectedSkillLevel != null) {
                    switch (selectedSkillLevel) {
                        case GREEN -> type.setGreenLevel(i);
                        case REGULAR -> type.setRegularLevel(i);
                        case VETERAN -> type.setVeteranLevel(i);
                        case ELITE -> type.setEliteLevel(i);
                        default -> {}
                    }
                }
            }
        }
    }
}
