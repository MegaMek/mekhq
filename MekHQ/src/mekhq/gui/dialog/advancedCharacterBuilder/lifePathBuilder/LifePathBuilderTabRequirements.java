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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType.REQUIREMENTS;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathRecord;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTabStorage;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabRequirements {
    private final static MMLogger LOGGER = MMLogger.create(LifePathBuilderTabRequirements.class);

    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private final Map<Integer, LifePathTabStorage> requirementsTabStorageMap = new HashMap<>();
    private final Map<Integer, String> requirementsTabTextMap = new HashMap<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    public Map<Integer, LifePathTabStorage> getRequirementsTabStorageMap() {
        return requirementsTabStorageMap;
    }

    public void setRequirementsTabStorageMap(Map<Integer, LifePathTabStorage> requirementsTabStorageMap) {
        this.requirementsTabStorageMap.clear();
        this.requirementsTabStorageMap.putAll(requirementsTabStorageMap);
    }

    public Map<Integer, String> getRequirementsTabTextMap() {
        return requirementsTabTextMap;
    }

    LifePathBuilderTabRequirements(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo) {
        this.parent = parent;
        this.allAbilityInfo.putAll(allAbilityInfo);

        JPanel tabRequirements = new JPanel(new BorderLayout());
        tabRequirements.setName("requirements");
        String titleRequirements = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.requirements");
        tabMain.addTab(titleRequirements, tabRequirements);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.tooltip");
        RoundedJButton btnAddRequirementGroup = new RoundedJButton(titleAddGroup);
        btnAddRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddGroup)
        );
        buttonPanel.add(btnAddRequirementGroup);

        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.tooltip");
        RoundedJButton btnRemoveRequirementGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveRequirementGroup);

        String titleDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.duplicateGroup.label");
        String tooltipDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.duplicateGroup.tooltip");
        RoundedJButton btnDuplicateGroup = new RoundedJButton(titleDuplicateGroup);
        btnDuplicateGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipDuplicateGroup)
        );
        buttonPanel.add(btnDuplicateGroup);

        // The actual tabbed pane and any button action listeners (we add them here to avoid a situation where they
        // can be called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveRequirementGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddRequirementGroup.addActionListener(e -> {
            addRequirementsTab(tabbedPane, gameYear, null);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
        btnRemoveRequirementGroup.addActionListener(e -> removeRequirementGroup(tabbedPane));
        btnDuplicateGroup.addActionListener(e -> duplicateGroup(tabbedPane, gameYear));

        // Add 'Group 0' - this group is required
        addRequirementsTab(tabbedPane, gameYear, null);

        tabRequirements.add(buttonPanel, BorderLayout.NORTH);
        tabRequirements.add(tabbedPane, BorderLayout.CENTER);
    }

    private void removeRequirementGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // Remove the current tab, unless it's Group 0
        if (selectedIndex > 0) {
            // We need to remove the tab's storage data from the storge map and then re-add the tabs in the
            // correct order (since we're removing a tab, the indexes will shift)
            Map<Integer, LifePathTabStorage> tempStorageMap = new HashMap<>();
            for (Map.Entry<Integer, LifePathTabStorage> entry : requirementsTabStorageMap.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempStorageMap.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempStorageMap.put(entry.getKey() - 1, entry.getValue());
                }
            }

            requirementsTabStorageMap.clear();
            requirementsTabStorageMap.putAll(tempStorageMap);

            Map<Integer, String> tempTextMap = new HashMap<>();
            for (Map.Entry<Integer, String> entry : requirementsTabTextMap.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempTextMap.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempTextMap.put(entry.getKey() - 1, entry.getValue());
                }
            }

            requirementsTabTextMap.clear();
            requirementsTabTextMap.putAll(tempTextMap);

            // Remove the desired tab
            tabbedPane.remove(selectedIndex);

            // Update the progress panel
            parent.updateTxtProgress();
        }
    }

    private void duplicateGroup(EnhancedTabbedPane tabbedPane, int gameYear) {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return; // nothing selected, do nothing
        }

        LifePathTabStorage currentValues = requirementsTabStorageMap.get(selectedIndex);
        String currentText = requirementsTabTextMap.get(selectedIndex);

        addRequirementsTab(tabbedPane, gameYear, currentValues);
        int newIndex = tabbedPane.getTabCount() - 1;

        requirementsTabStorageMap.put(newIndex, currentValues);
        requirementsTabTextMap.put(newIndex, currentText);

        JPanel newTabPanel = (JPanel) tabbedPane.getComponentAt(newIndex);
        JPanel pnlRequirements = (JPanel) newTabPanel.getComponent(1);
        JEditorPane txtRequirements = findEditorPaneByName(pnlRequirements, "txtRequirements");
        if (txtRequirements != null) {
            txtRequirements.setText(currentText);
        } else {
            LOGGER.warn("Could not find txtRequirements in duplicateGroup");
        }

        parent.updateTxtProgress();

        tabbedPane.setSelectedIndex(newIndex);
    }

    private JEditorPane findEditorPaneByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JEditorPane && name.equals(component.getName())) {
                return (JEditorPane) component;
            } else if (component instanceof Container) {
                JEditorPane result = findEditorPaneByName((Container) component, name);
                if (result != null) {return result;}
            }
        }
        return null;
    }

    private void addRequirementsTab(EnhancedTabbedPane tabbedPane, int gameYear, @Nullable LifePathTabStorage storage) {
        boolean hasStorage = storage != null;

        int index = tabbedPane.getTabCount();

        // Create the panel to be used in the tab
        JPanel requirementGroupPanel = new JPanel();
        requirementGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        if (hasStorage) {
            attributes.putAll(storage.attributes());
        }

        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        if (hasStorage) {
            traits.putAll(storage.traits());
        }

        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<SkillType, Integer> skills = new HashMap<>();
        if (hasStorage) {
            skills.putAll(storage.skills());
        }

        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        Map<CampaignOptionsAbilityInfo, Integer> abilities = new HashMap<>();
        if (hasStorage) {
            abilities.putAll(storage.abilities());
        }

        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        List<Faction> factions = new ArrayList<>();
        if (hasStorage) {
            factions.addAll(storage.factions());
        }

        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddFaction)
        );
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        List<LifePathRecord> lifePaths = new ArrayList<>();
        if (hasStorage) {
            lifePaths.addAll(storage.lifePaths());
        }

        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddLifePath)
        );
        btnAddLifePath.setEnabled(false); // TODO Implement
        buttonsPanel.add(btnAddLifePath);

        // Categories
        Map<LifePathCategory, Integer> categories = new HashMap<>();
        if (hasStorage) {
            categories.putAll(storage.categories());
        }

        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddCategory)
        );
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtRequirements = new JEditorPane();
        txtRequirements.setName("txtRequirements");
        txtRequirements.setContentType("text/html");
        txtRequirements.setEditable(false);
        LifePathTabStorage initialStorage = getRequirementsTabStorage(
              gameYear,
              factions,
              lifePaths,
              categories,
              attributes,
              traits,
              skills,
              abilities);
        String initialRequirementsText = buildRequirementText(initialStorage);
        requirementsTabStorageMap.put(index, initialStorage);
        requirementsTabTextMap.put(index, initialRequirementsText);

        txtRequirements.setText(initialRequirementsText);

        FastJScrollPane scrollRequirements = new FastJScrollPane(txtRequirements);
        scrollRequirements.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setBorder(null);

        pnlDisplay.add(scrollRequirements, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            LifePathAttributePicker picker = new LifePathAttributePicker(attributes, REQUIREMENTS);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits, REQUIREMENTS);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills, REQUIREMENTS);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo, REQUIREMENTS);
            abilities.clear();
            abilities.putAll(picker.getSelectedAbilities());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);
            LifePathFactionPicker picker = new LifePathFactionPicker(factions, gameYear, REQUIREMENTS);
            factions.clear();
            factions.addAll(picker.getSelectedFactions());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the current requirements and allows the user to remove one

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);
            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(categories, REQUIREMENTS);
            categories.clear();
            categories.putAll(picker.getSelectedCategoryCounts());

            standardizedActions(gameYear, index, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtRequirements, initialStorage);
        });

        // Add panels and then add Tab
        requirementGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        requirementGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        int count = tabbedPane.getComponentCount();
        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab." + (count == 0 ? "compulsory" : "optional") + ".formattedLabel");
        tabbedPane.addTab(titleTab, requirementGroupPanel);
    }

    private void standardizedActions(int gameYear, int index, Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<SkillType, Integer> skills,
          Map<CampaignOptionsAbilityInfo, Integer> abilities, List<Faction> factions, List<LifePathRecord> lifePaths,
          Map<LifePathCategory, Integer> categories, JEditorPane txtRequirements,
          LifePathTabStorage initialStorage) {
        LifePathTabStorage storage = getRequirementsTabStorage(
              gameYear,
              factions,
              lifePaths,
              categories,
              attributes,
              traits,
              skills,
              abilities);
        String requirementsText = buildRequirementText(storage);
        txtRequirements.setText(requirementsText);

        requirementsTabStorageMap.put(index, initialStorage);
        requirementsTabTextMap.put(index, requirementsText);
        parent.updateTxtProgress();

        parent.setVisible(true);
    }

    private static LifePathTabStorage getRequirementsTabStorage(
          int gameYear, List<Faction> factions,
          List<LifePathRecord> lifePaths, Map<LifePathCategory, Integer> categories,
          Map<SkillAttribute, Integer> attributes, Map<LifePathEntryDataTraitLookup, Integer> traits,
          Map<SkillType, Integer> skills, Map<CampaignOptionsAbilityInfo, Integer> abilities) {
        return new LifePathTabStorage(gameYear,
              factions,
              lifePaths,
              categories,
              attributes,
              traits,
              skills,
              abilities);
    }

    private static String buildRequirementText(
          LifePathTabStorage storage) {
        StringBuilder progressText = new StringBuilder();

        // Factions
        List<Faction> factions = storage.factions();
        int gameYear = storage.gameYear();
        if (!factions.isEmpty()) {
            for (int i = 0; i < factions.size(); i++) {
                Faction faction = factions.get(i);
                progressText.append(faction.getFullName(gameYear));
                if (i != factions.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Life Paths
        List<LifePathRecord> lifePaths = storage.lifePaths();
        if (!lifePaths.isEmpty()) {
            appendComma(progressText);

            for (int i = 0; i < lifePaths.size(); i++) {
                LifePathRecord lifePath = lifePaths.get(i);
                progressText.append(lifePath.name());
                if (i != lifePaths.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Categories
        Map<LifePathCategory, Integer> categories = storage.categories();
        if (!categories.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = categories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : categories.entrySet()) {
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(" ");
                progressText.append(entry.getValue());
                progressText.append("+");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // Attributes
        Map<SkillAttribute, Integer> attributes = storage.attributes();
        if (!attributes.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = attributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : attributes.entrySet()) {
                progressText.append(entry.getKey().getLabel());
                progressText.append(" ");
                progressText.append(entry.getValue());
                progressText.append("+");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = storage.traits();
        if (!traits.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : traits.entrySet()) {
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(" ");
                progressText.append(entry.getValue());
                progressText.append("+");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // Skills
        Map<SkillType, Integer> skills = storage.skills();
        if (!skills.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = skills.size();
            for (Map.Entry<SkillType, Integer> entry : skills.entrySet()) {
                String label = entry.getKey().getName().replace(SkillType.RP_ONLY_TAG, "");

                progressText.append(label);
                progressText.append(" ");
                progressText.append(entry.getValue());
                progressText.append("+");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // SPAs
        Map<CampaignOptionsAbilityInfo, Integer> selectedSPAs = storage.abilities();
        if (!selectedSPAs.isEmpty()) {
            appendComma(progressText);

            List<CampaignOptionsAbilityInfo> spas = selectedSPAs.keySet().stream().toList();

            for (int i = 0; i < spas.size(); i++) {
                SpecialAbility ability = spas.get(i).getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
                progressText.append(label);
                if (i != spas.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        return progressText.toString();
    }

    private static void appendComma(StringBuilder progressText) {
        if (!progressText.isEmpty()) {
            progressText.append(", ");
        }
    }
}
