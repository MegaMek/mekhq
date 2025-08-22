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

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType.FIXED_XP;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;

import java.awt.BorderLayout;
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
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTabStorage;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.pickers.LifePathAttributePicker;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.pickers.LifePathSPAPicker;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.pickers.LifePathSkillPicker;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.pickers.LifePathTraitPicker;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

public class LifePathBuilderTabFixedXP {
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private LifePathTabStorage fixedXPTabStorage;
    private String fixedXPTabTextStorage;
    private final List<String> level3Abilities = new ArrayList<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    public LifePathTabStorage getFixedXPTabStorage() {
        return fixedXPTabStorage;
    }

    public String getFixedXPTabTextStorage() {
        return fixedXPTabTextStorage;
    }

    public LifePathBuilderTabFixedXP(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain) {
        this.parent = parent;

        buildAllAbilityInfo();

        JPanel tabFixedXP = new JPanel(new BorderLayout());
        tabFixedXP.setName("fixedXP");
        String titleFixedXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.fixedXP");
        tabMain.addTab(titleFixedXP, tabFixedXP);

        JPanel pnlFixedXP = buildFixedXPPanel();
        tabFixedXP.add(pnlFixedXP, BorderLayout.CENTER);
    }

    private JPanel buildFixedXPPanel() {
        JPanel pnlFixedXP = new JPanel();
        pnlFixedXP.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<SkillType, Integer> skills = new HashMap<>();
        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        Map<CampaignOptionsAbilityInfo, Integer> abilities = new HashMap<>();
        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.fixedXP.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.fixedXP.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtFixedXP = new JEditorPane();
        txtFixedXP.setName("txtFixedXP");
        txtFixedXP.setContentType("text/html");
        txtFixedXP.setEditable(false);
        LifePathTabStorage initialStorage = getFixedXPTabStorage(attributes, traits, skills, abilities);
        String initialFixedXPText = buildFixedXPText(initialStorage);
        fixedXPTabStorage = initialStorage;
        fixedXPTabTextStorage = initialFixedXPText;

        txtFixedXP.setText(initialFixedXPText);

        FastJScrollPane scrollFixedXP = new FastJScrollPane(txtFixedXP);
        scrollFixedXP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollFixedXP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollFixedXP.setBorder(null);

        pnlDisplay.add(scrollFixedXP, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            LifePathAttributePicker picker = new LifePathAttributePicker(attributes, FIXED_XP);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits, FIXED_XP);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills, FIXED_XP);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo, FIXED_XP);
            abilities.clear();
            abilities.putAll(picker.getSelectedAbilities());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage);
        });

        // Add panels and then add Tab
        pnlFixedXP.add(buttonsPanel, BorderLayout.NORTH);
        pnlFixedXP.add(pnlDisplay, BorderLayout.CENTER);

        return pnlFixedXP;
    }

    private void standardizedActions(Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<SkillType, Integer> skills,
          Map<CampaignOptionsAbilityInfo, Integer> abilities, JEditorPane txtFixedXP,
          LifePathTabStorage initialStorage) {
        LifePathTabStorage storage = getFixedXPTabStorage(attributes, traits, skills, abilities);
        String fixedXPText = buildFixedXPText(storage);
        txtFixedXP.setText(fixedXPText);

        fixedXPTabStorage = initialStorage;
        fixedXPTabTextStorage = fixedXPText;

        parent.updateTxtProgress();
        parent.setVisible(true);
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

    private static LifePathTabStorage getFixedXPTabStorage(Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<SkillType, Integer> skills,
          Map<CampaignOptionsAbilityInfo, Integer> abilities) {
        return new LifePathTabStorage(0,
              new ArrayList<>(),
              new ArrayList<>(),
              new HashMap<>(),
              attributes,
              traits,
              skills,
              abilities);
    }

    private static String buildFixedXPText(LifePathTabStorage storage) {
        StringBuilder progressText = new StringBuilder();

        // Attributes
        Map<SkillAttribute, Integer> attributes = storage.attributes();
        if (!attributes.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = attributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : attributes.entrySet()) {
                int value = entry.getValue();

                progressText.append(entry.getKey().getLabel());
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
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
                int value = entry.getValue();

                progressText.append(entry.getKey().getDisplayName());
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
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
                int value = entry.getValue();

                progressText.append(entry.getKey().getName());
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(value);
                progressText.append(" XP");
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

            int counter = 0;
            int length = selectedSPAs.size();
            for (Map.Entry<CampaignOptionsAbilityInfo, Integer> entry : selectedSPAs.entrySet()) {
                SpecialAbility ability = entry.getKey().getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");

                int value = entry.getValue();

                progressText.append(label);
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
                counter++;
                if (counter != length) {
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
