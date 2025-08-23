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

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType.EXCLUSIONS;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTabStorage;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabExclusions {
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private LifePathTabStorage exclusionsTabStorage;
    private String exclusionsTabTextStorage;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    public LifePathTabStorage getExclusionsTabStorage() {
        return exclusionsTabStorage;
    }

    public String getExclusionsTabTextStorage() {
        return exclusionsTabTextStorage;
    }

    LifePathBuilderTabExclusions(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo) {
        this.parent = parent;
        this.allAbilityInfo.putAll(allAbilityInfo);

        JPanel tabExclusions = new JPanel(new BorderLayout());
        tabExclusions.setName("exclusions");
        String titleExclusions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.exclusions");
        tabMain.addTab(titleExclusions, tabExclusions);

        JPanel pnlExclusions = buildExclusionsPanel(gameYear);
        tabExclusions.add(pnlExclusions, BorderLayout.CENTER);
    }

    private JPanel buildExclusionsPanel(int gameYear) {
        JPanel pnlExclusions = new JPanel();
        pnlExclusions.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<SkillType, Integer> skills = new HashMap<>();
        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        Map<CampaignOptionsAbilityInfo, Integer> abilities = new HashMap<>();
        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        List<Faction> factions = new ArrayList<>();
        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddFaction)
        );
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        List<UUID> lifePaths = new ArrayList<>();
        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddLifePath)
        );
        btnAddLifePath.setEnabled(false); // TODO Implement
        buttonsPanel.add(btnAddLifePath);

        // Categories
        Map<LifePathCategory, Integer> categories = new HashMap<>();
        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.exclusions.button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddCategory)
        );
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.exclusions.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtExclusions = new JEditorPane();
        txtExclusions.setName("txtExclusions");
        txtExclusions.setContentType("text/html");
        txtExclusions.setEditable(false);
        LifePathTabStorage initialStorage = getExclusionsTabStorage(gameYear, factions, lifePaths, categories,
              attributes, traits, skills, abilities);
        String initialExclusionsText = buildExclusionsText(initialStorage);
        exclusionsTabStorage = initialStorage;
        exclusionsTabTextStorage = initialExclusionsText;

        txtExclusions.setText(initialExclusionsText);

        FastJScrollPane scrollExclusions = new FastJScrollPane(txtExclusions);
        scrollExclusions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollExclusions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollExclusions.setBorder(null);

        pnlDisplay.add(scrollExclusions, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            LifePathAttributePicker picker = new LifePathAttributePicker(attributes, EXCLUSIONS);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits, EXCLUSIONS);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills, EXCLUSIONS);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo, EXCLUSIONS);
            abilities.clear();
            abilities.putAll(picker.getSelectedAbilities());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);
            LifePathFactionPicker picker = new LifePathFactionPicker(factions, gameYear, EXCLUSIONS);
            factions.clear();
            factions.addAll(picker.getSelectedFactions());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);
            // TODO launch a dialog that lists the current exclusions and allows the user to remove one

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);
            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(categories, EXCLUSIONS);
            categories.clear();
            categories.putAll(picker.getSelectedCategoryCounts());

            standardizedActions(gameYear, attributes, traits, skills, abilities, factions, lifePaths,
                  categories, txtExclusions, initialStorage);
        });

        // Add panels and then add Tab
        pnlExclusions.add(buttonsPanel, BorderLayout.NORTH);
        pnlExclusions.add(pnlDisplay, BorderLayout.CENTER);

        return pnlExclusions;
    }

    private void standardizedActions(int gameYear, Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<SkillType, Integer> skills,
          Map<CampaignOptionsAbilityInfo, Integer> abilities, List<Faction> factions, List<UUID> lifePaths,
          Map<LifePathCategory, Integer> categories, JEditorPane txtExclusions,
          LifePathTabStorage initialStorage) {
        LifePathTabStorage storage = getExclusionsTabStorage(gameYear, factions, lifePaths, categories,
              attributes, traits, skills, abilities);
        String exclusionsText = buildExclusionsText(storage);
        txtExclusions.setText(exclusionsText);

        exclusionsTabStorage = initialStorage;
        exclusionsTabTextStorage = exclusionsText;

        parent.updateTxtProgress();
        parent.setVisible(true);
    }

    private static LifePathTabStorage getExclusionsTabStorage(int gameYear, List<Faction> factions,
          List<UUID> lifePaths, Map<LifePathCategory, Integer> categories,
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

    private static String buildExclusionsText(LifePathTabStorage storage) {
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
        List<UUID> lifePaths = storage.lifePaths();
        if (!lifePaths.isEmpty()) {
            appendComma(progressText);

            for (int i = 0; i < lifePaths.size(); i++) {
                UUID lifePath = lifePaths.get(i);
                // TODO lookup the lifePath via its UUID and display its name instead of its UUID
                progressText.append(lifePath.toString());
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
