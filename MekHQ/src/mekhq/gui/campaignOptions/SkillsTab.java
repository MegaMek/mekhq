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
package mekhq.gui.campaignOptions;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.sort;
import static megamek.common.enums.SkillLevel.*;
import static mekhq.campaign.personnel.SkillType.isCombatSkill;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.*;

public class SkillsTab {
    private Map<String, JSpinner> allTargetNumbers;
    private Map<String, List<JLabel>> allSkillLevels;
    private Map<String, List<JSpinner>> allSkillCosts;
    private Map<String, List<JComboBox<SkillLevel>>> allSkillMilestones;
    private double storedTargetNumber = 0;
    private List<Double> storedValuesSpinners = new ArrayList<>();
    private List<SkillLevel> storedValuesComboBoxes = new ArrayList<>();

    private static final MMLogger logger = MMLogger.create(SkillsTab.class);

    SkillsTab() {
        initialize();
    }

    private void initialize() {
        initializeGeneral();
    }

    private void initializeGeneral() {
        allTargetNumbers = new HashMap<>();
        allSkillLevels = new HashMap<>();
        allSkillCosts = new HashMap<>();
        allSkillMilestones = new HashMap<>();
        storedTargetNumber = 0;
        storedValuesSpinners = new ArrayList<>();
        storedValuesComboBoxes = new ArrayList<>();
    }

    JPanel createSkillsTab(boolean isCombatTab) {
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

    private void setVisibleForAll(boolean visible) {
        setVisibleForAll(allSkillLevels, visible);
        setVisibleForAll(allSkillCosts, visible);
        setVisibleForAll(allSkillMilestones, visible);
    }

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

    void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(new HashMap<>());
    }

    void loadValuesFromCampaignOptions(Map<String, SkillType> presetSkillValues) {
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

    void applyCampaignOptionsToCampaign(@Nullable Map<String, SkillType> presetSkills) {
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

    private void updateTargetNumber(SkillType type) {
        int targetNumber = (int) allTargetNumbers.get(type.getName()).getValue();
        type.setTarget(targetNumber);
    }

    private void updateSkillCosts(String skillName) {
        List<JSpinner> costs = allSkillCosts.get(skillName);

        for (int level = 0; level < costs.size(); level++) {
            int cost = (int) costs.get(level).getValue();
            SkillType.setCost(skillName, cost, level);
        }
    }

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
