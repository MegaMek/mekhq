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

import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathRecord;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

public class LifePathBuilderTabRequirements {
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private final Map<Integer, RequirementsTabStorage> requirementsTabStorageMap = new HashMap<>();
    private final Map<Integer, String> requirementsTabTextMap = new HashMap<>();
    private final ArrayList<String> level3Abilities = new ArrayList<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    public Map<Integer, RequirementsTabStorage> getRequirementsTabStorageMap() {
        return requirementsTabStorageMap;
    }

    public Map<Integer, String> getRequirementsTabTextMap() {
        return requirementsTabTextMap;
    }

    public LifePathBuilderTabRequirements(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear) {
        this.parent = parent;

        buildAllAbilityInfo();

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

        // The actual tabbed pane and any button action listeners (we add them here to avoid a situation where they
        // can be called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveRequirementGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddRequirementGroup.addActionListener(e -> {
            createRequirementsTab(tabbedPane, gameYear);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
        btnRemoveRequirementGroup.addActionListener(e -> removeRequirementGroup(tabbedPane));

        // Add 'Group 0' - this group is required
        createRequirementsTab(tabbedPane, gameYear);

        tabRequirements.add(buttonPanel, BorderLayout.NORTH);
        tabRequirements.add(tabbedPane, BorderLayout.CENTER);
    }

    private void removeRequirementGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // Remove the current tab, unless it's Group 0
        if (selectedIndex > 0) {
            // We need to remove the tab's storage data from the storge map and then re-add the tabs in the
            // correct order (since we're removing a tab, the indexes will shift)
            Map<Integer, RequirementsTabStorage> tempStorageMap = new HashMap<>();
            for (Map.Entry<Integer, RequirementsTabStorage> entry : requirementsTabStorageMap.entrySet()) {
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

            // Update the names of the remaining tabs
            int tabCount = tabbedPane.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                String titleTab = getFormattedTextAt(RESOURCE_BUNDLE,
                      "LifePathBuilderDialog.tab." + (i == 0 ? "compulsory" : "optional") + ".formattedLabel", i);
                tabbedPane.setTitleAt(i, titleTab);
            }

            // Update the progress panel
            parent.updateTxtProgress();
        }
    }

    private void createRequirementsTab(EnhancedTabbedPane tabbedPane, int gameYear) {
        int index = tabbedPane.getTabCount();

        // Create the panel to be used in the tab
        JPanel requirementGroupPanel = new JPanel();
        requirementGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
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
        List<CampaignOptionsAbilityInfo> abilities = new ArrayList<>();
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
        txtRequirements.setContentType("text/html");
        txtRequirements.setEditable(false);
        RequirementsTabStorage initialStorage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
              attributes, traits, skills, abilities);
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

            LifePathAttributePicker picker = new LifePathAttributePicker(attributes);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);
            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo);
            abilities.clear();
            abilities.addAll(picker.getSelectedAbilities());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);
            LifePathFactionPicker picker = new LifePathFactionPicker(factions, gameYear);
            factions.clear();
            factions.addAll(picker.getSelectedFactions());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            txtRequirements.setText(requirementsText);
            parent.setVisible(true);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the current requirements and allows the user to remove one

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);
            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(categories);
            categories.clear();
            categories.putAll(picker.getSelectedCategoryCounts());

            RequirementsTabStorage storage = getRequirementsTabStorage(gameYear, factions, lifePaths, categories,
                  attributes, traits, skills, abilities);
            String requirementsText = buildRequirementText(storage);
            txtRequirements.setText(requirementsText);

            requirementsTabStorageMap.put(index, initialStorage);
            requirementsTabTextMap.put(index, requirementsText);
            parent.updateTxtProgress();

            parent.setVisible(true);
        });

        // Add panels and then add Tab
        requirementGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        requirementGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        int count = tabbedPane.getComponentCount();
        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab." + (count == 0 ? "compulsory" : "optional") + ".formattedLabel", count);
        tabbedPane.addTab(titleTab, requirementGroupPanel);
    }

    public void buildAllAbilityInfo() {
        // Remove old data
        allAbilityInfo.clear();
        level3Abilities.clear();

        // Build list of Level 3 abilities
        PersonnelOptions personnelOptions = new PersonnelOptions();
        for (final Enumeration<IOptionGroup> i = personnelOptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                level3Abilities.add(option.getName());
            }
        }

        // Build abilities
        buildAbilityInfo(SpecialAbility.getSpecialAbilities(), true);

        Map<String, SpecialAbility> allSpecialAbilities = SpecialAbility.getDefaultSpecialAbilities();
        Map<String, SpecialAbility> missingAbilities = new HashMap<>();

        for (SpecialAbility ability : allSpecialAbilities.values()) {
            if (!allAbilityInfo.containsKey(ability.getName())) {
                missingAbilities.put(ability.getName(), ability);
            }
        }

        if (!missingAbilities.isEmpty()) {
            buildAbilityInfo(missingAbilities, false);
        }
    }

    private void buildAbilityInfo(Map<String, SpecialAbility> abilities, boolean isEnabled) {
        for (Map.Entry<String, SpecialAbility> entry : abilities.entrySet()) {
            SpecialAbility clonedAbility = entry.getValue().clone();
            String abilityName = clonedAbility.getName();
            AbilityCategory category = getSpaCategory(clonedAbility);

            if (!level3Abilities.contains(abilityName)) {
                continue;
            }

            // Mark the ability as active
            allAbilityInfo.put(abilityName,
                  new CampaignOptionsAbilityInfo(abilityName, clonedAbility, isEnabled, category));
        }
    }

    private static RequirementsTabStorage getRequirementsTabStorage(int gameYear, List<Faction> factions,
          List<LifePathRecord> lifePaths, Map<LifePathCategory, Integer> categories,
          Map<SkillAttribute, Integer> attributes, Map<LifePathEntryDataTraitLookup, Integer> traits,
          Map<SkillType, Integer> skills, List<CampaignOptionsAbilityInfo> abilities) {
        return new RequirementsTabStorage(gameYear,
              factions,
              lifePaths,
              categories,
              attributes,
              traits,
              skills,
              abilities);
    }

    private static String buildRequirementText(RequirementsTabStorage storage) {
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
            int length = traits.size();
            for (Map.Entry<SkillType, Integer> entry : skills.entrySet()) {
                progressText.append(entry.getKey().getName());
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
        List<CampaignOptionsAbilityInfo> selectedSPAs = storage.abilities();
        if (!selectedSPAs.isEmpty()) {
            appendComma(progressText);

            for (int i = 0; i < selectedSPAs.size(); i++) {
                SpecialAbility ability = selectedSPAs.get(i).getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
                progressText.append(label);
                if (i != selectedSPAs.size() - 1) {
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
