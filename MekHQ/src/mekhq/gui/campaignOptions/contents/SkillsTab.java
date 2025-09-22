/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.HEROIC;
import static megamek.common.enums.SkillLevel.LEGENDARY;
import static megamek.common.enums.SkillLevel.NONE;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.ULTRA_GREEN;
import static megamek.common.enums.SkillLevel.VETERAN;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_ART;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_INTEREST;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_SCIENCE;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_SECURITY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT_COMMAND;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * SkillsTab is a component of the campaign options user interface that allows players to configure the rules and costs
 * associated with skills in their campaign.
 * <p>
 * This tab can be configured for either combat or support skills. It allows users to:
 * </p>
 * <ul>
 *   <li>Set skill target numbers.</li>
 *   <li>Specify skill costs at different levels.</li>
 *   <li>Define milestones for the progression of skills by skill levels.</li>
 *   <li>Copy and paste configurations for skill settings.</li>
 * </ul>
 * <p>
 * The interface is dynamically created to display all relevant skill options for the
 * selected tab (combat or support).
 * </p>
 */
public class SkillsTab {
    private final CampaignOptions campaignOptions;

    private Map<String, JSpinner> allTargetNumbers;
    private Map<String, List<JLabel>> allSkillLevels;
    private Map<String, List<JSpinner>> allSkillCosts;
    private Map<String, List<JComboBox<SkillLevel>>> allSkillMilestones;
    private int storedTargetNumber = 0;
    private List<Integer> storedValuesSpinners = new ArrayList<>();
    private List<SkillLevel> storedValuesComboBoxes = new ArrayList<>();

    private JPanel pnlEdgeCost;
    private JLabel lblEdgeCost;
    private JSpinner spnEdgeCost;

    private static final MMLogger logger = MMLogger.create(SkillsTab.class);

    /**
     * Constructs a new `SkillsTab` instance and initializes the necessary data structures for managing skill
     * configurations.
     *
     * @param campaignOptions the {@code CampaignOptions} instance that holds the settings to be modified or displayed
     *                        in this tab.
     */
    public SkillsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
        initialize();
    }

    /**
     * Initializes the SkillsTab by setting up the necessary data structures and default values for skill
     * configuration.
     */
    private void initialize() {
        initializeGeneral();
    }

    /**
     * Sets up the general data structures needed for skill configuration in the SkillsTab. This includes collections
     * for tracking target numbers, costs, and milestones for every skill.
     */
    private void initializeGeneral() {
        allTargetNumbers = new HashMap<>();
        allSkillLevels = new HashMap<>();
        allSkillCosts = new HashMap<>();
        allSkillMilestones = new HashMap<>();
        storedTargetNumber = 0;
        storedValuesSpinners = new ArrayList<>();
        storedValuesComboBoxes = new ArrayList<>();

        pnlEdgeCost = new JPanel();
        lblEdgeCost = new JLabel();
        spnEdgeCost = new JSpinner();
    }

    /**
     * Creates the main panel for the SkillsTab UI based on the provided {@link SkillSubType} category.
     *
     * <p>This method dynamically generates a tab for either combat, support, or roleplay skills. The tab displays
     * skill panels associated with the specified category, allowing users to configure target numbers, costs, and
     * milestones. It also includes buttons to toggle visibility of skill panels and manages layout to organize various
     * UI components.</p>
     *
     * @param category the {@link SkillSubType} representing the skill category. This determines which set of skills
     *                 will be displayed (e.g., {@code COMBAT_GUNNERY}, {@code COMBAT_PILOTING}, {@code SUPPORT}, or
     *                 {@code ROLEPLAY}).
     *
     * @return a {@link JPanel} containing the dynamically generated skill options and UI components for the selected
     *       category.
     */
    public JPanel createSkillsTab(SkillSubType category) {
        // Header
        CampaignOptionsHeaderPanel headerPanel;
        String panelName;
        switch (category) {
            case COMBAT_GUNNERY -> {
                headerPanel = new CampaignOptionsHeaderPanel("GunnerySkillsTab",
                      getImageDirectory() + "logo_clan_diamond_sharks.png");
                panelName = "GunnerySkillsTab";
            }
            case COMBAT_PILOTING -> {
                headerPanel = new CampaignOptionsHeaderPanel("PilotingSkillsTab",
                      getImageDirectory() + "logo_capellan_confederation.png");
                panelName = "PilotingSkillsTab";
            }
            case SUPPORT -> {
                headerPanel = new CampaignOptionsHeaderPanel("SupportSkillsTab",
                      getImageDirectory() + "logo_clan_goliath_scorpion.png");
                panelName = "SupportSkillsTab";
            }
            default -> { // ROLEPLAY
                headerPanel = new CampaignOptionsHeaderPanel("RoleplaySkillsTab",
                      getImageDirectory() + "logo_clan_jade_falcon.png");
                panelName = "RoleplaySkillsTab";
            }
        }

        // Contents
        List<SkillType> relevantSkills = new ArrayList<>();
        for (String skillName : SkillType.getSkillList()) {
            SkillType skill = SkillType.getType(skillName);
            SkillSubType subType = skill.getSubType();

            boolean isCorrectType = switch (category) {
                case NONE, COMBAT_GUNNERY -> subType == COMBAT_GUNNERY;
                case COMBAT_PILOTING -> subType == COMBAT_PILOTING;
                case SUPPORT -> subType == SUPPORT || subType == SUPPORT_COMMAND;
                case ROLEPLAY_GENERAL -> subType == ROLEPLAY_GENERAL ||
                                               subType == ROLEPLAY_ART ||
                                               subType == ROLEPLAY_INTEREST ||
                                               subType == ROLEPLAY_SCIENCE ||
                                               subType == ROLEPLAY_SECURITY;
                // These next few cases shouldn't get hit, but we include them just in case
                case SUPPORT_COMMAND -> subType == SUPPORT_COMMAND;
                case ROLEPLAY_ART -> subType == ROLEPLAY_ART;
                case ROLEPLAY_INTEREST -> subType == ROLEPLAY_INTEREST;
                case ROLEPLAY_SCIENCE -> subType == ROLEPLAY_SCIENCE;
                case ROLEPLAY_SECURITY -> subType == ROLEPLAY_SECURITY;
            };

            // If the type is {@code null} for some reason, dump it into the combat category
            if (isCorrectType || (subType == null && category == COMBAT_GUNNERY)) {
                relevantSkills.add(skill);
            }
        }

        List<JPanel> skillPanels = new ArrayList<>();

        for (SkillType skill : relevantSkills) {
            JPanel skillPanel = createSkillPanel(skill);
            skillPanels.add(skillPanel);
        }

        // Content
        pnlEdgeCost = createEdgeCostPanel();
        pnlEdgeCost.setVisible(category == COMBAT_GUNNERY);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel(panelName, true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        RoundedJButton hideAllButton = new RoundedJButton(getTextAt(getCampaignOptionsResourceBundle(),
              "btnHideAll.text"));
        hideAllButton.addActionListener(e -> {
            setVisibleForAll(false);

            panel.revalidate();
            panel.repaint();
        });

        // Create a button to toggle the table
        RoundedJButton showAllButton = new RoundedJButton(getTextAt(getCampaignOptionsResourceBundle(),
              "btnDisplayAll.text"));
        showAllButton.addActionListener(e -> {
            setVisibleForAll(true);

            panel.revalidate();
            panel.repaint();
        });

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 2;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(pnlEdgeCost, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(showAllButton, layout);
        layout.gridx++;
        panel.add(hideAllButton, layout);

        layout.gridx = 0;
        layout.gridy++;
        int rows = (int) Math.ceil(skillPanels.size() / 5.0);
        for (int i = 0; i < rows; i++) {
            layout.gridy++;
            layout.gridx = 0;
            for (int j = 0; j < 5; j++) {
                int index = i * 5 + j;
                if (index < skillPanels.size()) {
                    panel.add(skillPanels.get(index), layout);
                }
                layout.gridx++;
            }
        }

        // Create Parent Panel
        return createParentPanel(panel, "CombatSkillsTab");
    }

    /**
     * Creates a panel to configure and display the edge cost option in the SkillsTab.
     *
     * @return A {@link JPanel} containing the label and spinner for the edge cost configuration.
     */
    private JPanel createEdgeCostPanel() {
        lblEdgeCost = new CampaignOptionsLabel("EdgeCost");
        spnEdgeCost = new CampaignOptionsSpinner("EdgeCost", 100, 0, 500, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("EdgeCostPanel", true, "EdgeCostPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblEdgeCost, layout);
        layout.gridx++;
        panel.add(spnEdgeCost, layout);

        return panel;
    }

    /**
     * Toggles the visibility of all components related to skills in the SkillsTab UI.
     * <p>
     * This can be used to either hide or display all components for easier navigation or cleaner representation of the
     * tab.
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
     * This method allows visibility control over target numbers, costs, and skill level milestones based on the
     * provided component map.
     * </p>
     *
     * @param componentsMap a map containing the components to show or hide.
     * @param visible       a boolean indicating whether to show or hide the components.
     * @param <T>           the type of the component to toggle visibility for (e.g., {@link JSpinner},
     *                      {@link JComboBox}).
     */
    private <T extends JComponent> void setVisibleForAll(Map<String, List<T>> componentsMap, boolean visible) {
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
     * <li>Setting the target number for the skill.</li>
     * <li>Configuring costs of the skill at different levels.</li>
     * <li>Defining milestones for skill progression (e.g., from Green to Veteran).</li>
     * </p>
     * Copy and paste buttons are available for transferring configurations between skills.
     * </p>
     *
     * @param skill the {@link SkillType} object representing the skill to be configured.
     *
     * @return a {@link JPanel} containing the UI components for the given skill.
     */
    private JPanel createSkillPanel(SkillType skill) {
        String panelName = "SkillPanel" + skill.getName().replace(" ", "");

        // Create the target number spinner
        JLabel lblTargetNumber = new CampaignOptionsLabel("SkillPanelTargetNumber");
        JSpinner spnTargetNumber = new CampaignOptionsSpinner("SkillPanelTargetNumber", 0, 0, 12, 1);
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

            JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + i, null, 0, -1, 9999, 1, true);
            spinner.setVisible(false);
            spinners.add(spinner);
            skillCosts.add(spinner);

            JComboBox<SkillLevel> comboBox = new JComboBox<>();
            comboBox.addItem(ULTRA_GREEN);
            comboBox.addItem(GREEN);
            comboBox.addItem(REGULAR);
            comboBox.addItem(VETERAN);
            comboBox.addItem(ELITE);
            comboBox.addItem(HEROIC);
            comboBox.addItem(LEGENDARY);
            comboBox.addActionListener(e -> milestoneActionListener(comboBoxes, comboBox));
            comboBox.setVisible(false);
            comboBoxes.add(comboBox);
            skillMilestones.add(comboBox);
        }

        allSkillLevels.put(skill.getName(), skillLevels);
        allSkillCosts.put(skill.getName(), skillCosts);
        allSkillMilestones.put(skill.getName(), skillMilestones);

        RoundedJButton copyButton = new RoundedJButton(getTextAt(getCampaignOptionsResourceBundle(), "btnCopy.text"));
        copyButton.addActionListener(e -> {
            storedTargetNumber = (Integer) spnTargetNumber.getValue();

            storedValuesSpinners = new ArrayList<>();
            storedValuesComboBoxes = new ArrayList<>();

            for (int i = 0; i < labels.size(); i++) {
                storedValuesSpinners.add((Integer) spinners.get(i).getValue());
                storedValuesComboBoxes.add((SkillLevel) comboBoxes.get(i).getSelectedItem());
            }
        });

        RoundedJButton pasteButton = new RoundedJButton(getTextAt(getCampaignOptionsResourceBundle(), "btnPaste.text"));
        pasteButton.addActionListener(e -> {
            spnTargetNumber.setValue(storedTargetNumber);

            for (int i = 0; i < labels.size(); i++) {
                spinners.get(i).setValue(storedValuesSpinners.get(i));
                comboBoxes.get(i).setSelectedItem(storedValuesComboBoxes.get(i));
            }
        });

        final JPanel panel = new CampaignOptionsStandardPanel(panelName);
        panel.setBorder(RoundedLineBorder.createRoundedLineBorder(skill.getName()));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        RoundedJButton toggleButton = new RoundedJButton(getTextAt(getCampaignOptionsResourceBundle(),
              "btnToggle.text"));
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
     * When the user changes the value of a milestone, subsequent milestones are automatically adjusted to ensure
     * logical skill progression (e.g., later milestones cannot precede earlier ones). This method enforces such
     * constraints.
     * </p>
     *
     * @param comboBoxes the list of combo boxes representing milestones for a single skill.
     * @param comboBox   the combo box that triggered the action.
     */
    private static void milestoneActionListener(List<JComboBox<SkillLevel>> comboBoxes,
          JComboBox<SkillLevel> comboBox) {
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
     * A version of this method without parameters that uses default skill values defined in the campaign.
     * </p>
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, new HashMap<>());
    }

    /**
     * Loads skill values into the UI components from the campaign options.
     * <p>
     * The method populates the spinners, labels, and combo boxes for all skills with their corresponding properties
     * (e.g., target numbers, costs, and milestones) retrieved from the campaign's configuration.
     * </p>
     *
     * @param presetSkillValues an optional map of preset skill values. If null or empty, default skill values are used
     *                          instead.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
          Map<String, SkillType> presetSkillValues) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        String[] skills = SkillType.getSkillList(); // default skills

        for (String skillName : skills) {
            // Fetch the skill, either from presetSkillValues or default
            SkillType skill = presetSkillValues.getOrDefault(skillName, SkillType.getType(skillName));

            // Skip outdated or missing skills
            if (allTargetNumbers.get(skillName) == null) {
                logger.info("(loadValuesFromCampaignOptions) Skipping outdated or missing skill: {}", skillName);
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
                int heroicIndex = skill.getHeroicLevel();
                int legendaryIndex = skill.getLegendaryLevel();

                for (int i = 0; i < milestones.size(); i++) {
                    SkillLevel levelToSet = determineMilestoneLevel(i,
                          greenIndex,
                          regularIndex,
                          veteranIndex,
                          eliteIndex,
                          heroicIndex,
                          legendaryIndex);
                    milestones.get(i).setSelectedItem(levelToSet);
                }
            }
        }

        // Edge Costs
        spnEdgeCost.setValue(options.getEdgeCost());
    }

    /**
     * Determines the skill level milestone based on progress indices.
     * <p>
     * It evaluates whether the milestone falls under Green, Regular, Veteran, or Elite levels, ensuring proper
     * assignments for milestone thresholds.
     * </p>
     *
     * @param currentIndex   the position in the milestone sequence.
     * @param greenIndex     the index where Green begins.
     * @param regularIndex   the index where Regular begins.
     * @param veteranIndex   the index where Veteran begins.
     * @param eliteIndex     the index where Elite begins.
     * @param heroicIndex    the index where Heroic begins.
     * @param legendaryIndex the index where Legendary begins.
     *
     * @return the corresponding {@link SkillLevel} for the given milestone.
     */
    private SkillLevel determineMilestoneLevel(int currentIndex, int greenIndex, int regularIndex, int veteranIndex,
          int eliteIndex, int heroicIndex, int legendaryIndex) {
        if (currentIndex < greenIndex) {
            return ULTRA_GREEN;
        }
        if (currentIndex < regularIndex) {
            return GREEN;
        }
        if (currentIndex < veteranIndex) {
            return REGULAR;
        }
        if (currentIndex < eliteIndex) {
            return VETERAN;
        }
        if (currentIndex < heroicIndex) {
            return ELITE;
        }
        if (currentIndex < legendaryIndex) {
            return HEROIC;
        }
        return LEGENDARY;
    }

    /**
     * Transfers the configured skill values from the SkillsTab UI into the campaign's underlying data model.
     * <p>
     * The method iterates through all configurable skills, updating the campaign with the configured target numbers,
     * costs, and milestones for each skill.
     * </p>
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to save settings to, or {@code null} to update
     *                              the current campaign options.
     * @param presetSkills          an optional map of preset skill values. Overrides default values if provided. Null
     *                              values will use the campaign's default values.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
          @Nullable Map<String, SkillType> presetSkills) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        for (final String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (presetSkills != null) {
                type = presetSkills.get(skillName);
            }

            if (type == null) {
                logger.info("(applyCampaignOptionsToCampaign) Skipping outdated or missing skill: {}", skillName);
                continue;
            }

            // Update Target Number
            updateTargetNumber(type);

            // Update Skill Costs
            updateSkillCosts(skillName);

            // Update Skill Milestones
            updateSkillMilestones(type);
        }

        // Edge Costs
        options.setEdgeCost((int) spnEdgeCost.getValue());
    }

    /**
     * Updates the target number for a given skill in the campaign based on the corresponding user input from the
     * SkillsTab UI.
     * <p>
     * The target number determines the difficulty level for the specified skill in the campaign. This value is fetched
     * from the associated spinner component in the UI and is applied to the given {@link SkillType}.
     * </p>
     *
     * @param type the {@link SkillType} object representing the skill whose target number needs to be updated.
     */
    private void updateTargetNumber(SkillType type) {
        int targetNumber = (int) allTargetNumbers.get(type.getName()).getValue();
        type.setTarget(targetNumber);
    }

    /**
     * Updates the costs associated with a given skill based on the user input from the SkillsTab UI.
     * <p>
     * For each level of the specified skill, the cost values are retrieved from the corresponding spinner components in
     * the UI and stored in the campaign's configuration. The costs represent the resource requirements for acquiring
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
     * Updates the skill milestones for a given skill in the campaign based on user input from the SkillsTab UI.
     * <p>
     * Milestones represent the thresholds required to reach certain skill levels (e.g., Green, Regular, Veteran,
     * Elite). The method processes these values from the associated combo boxes in the UI and applies them to the
     * provided {@link SkillType}.
     * <p>
     * The method ensures logical milestone progression and assigns default values if necessary.
     * </p>
     *
     * @param type the {@link SkillType} object representing the skill whose milestones are to be updated.
     */
    private void updateSkillMilestones(SkillType type) {
        List<JComboBox<SkillLevel>> skillMilestones = allSkillMilestones.get(type.getName());

        // These allow us to ensure the full array of milestones has been assigned
        type.setGreenLevel(skillMilestones.size() - 6);
        type.setRegularLevel(skillMilestones.size() - 5);
        type.setVeteranLevel(skillMilestones.size() - 4);
        type.setEliteLevel(skillMilestones.size() - 3);
        type.setHeroicLevel(skillMilestones.size() - 2);
        type.setLegendaryLevel(skillMilestones.size() - 1);

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
                        case HEROIC -> type.setHeroicLevel(i);
                        case LEGENDARY -> type.setLegendaryLevel(i);
                        default -> {
                        }
                    }
                }
            }
        }
    }
}
