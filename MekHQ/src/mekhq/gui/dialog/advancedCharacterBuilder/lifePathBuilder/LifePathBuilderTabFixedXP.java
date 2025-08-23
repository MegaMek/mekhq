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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathComponentStorage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabFixedXP {
    private static final MMLogger LOGGER = MMLogger.create(LifePathBuilderTabFixedXP.class);
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private LifePathComponentStorage fixedXPTabStorage;
    private String fixedXPTabTextStorage;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;

    public LifePathComponentStorage getFixedXPTabStorage() {
        return fixedXPTabStorage;
    }

    public void setFixedXPTabStorage(LifePathComponentStorage fixedXPTabStorage) {
        this.fixedXPTabStorage = fixedXPTabStorage;
    }

    public String getFixedXPTabTextStorage() {
        return fixedXPTabTextStorage;
    }

    public LifePathBuilderTabFixedXP(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo) {
        this.parent = parent;
        this.allAbilityInfo = allAbilityInfo;

        JPanel tabFixedXP = new JPanel(new BorderLayout());
        tabFixedXP.setName("fixedXP");
        String titleFixedXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.fixedXP");
        tabMain.addTab(titleFixedXP, tabFixedXP);

        JPanel pnlFixedXP = buildFixedXPPanel(gameYear);
        tabFixedXP.add(pnlFixedXP, BorderLayout.CENTER);
    }

    private JPanel buildFixedXPPanel(int gameYear) {
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
        Map<String, Integer> skills = new HashMap<>();
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
        Map<String, Integer> abilities = new HashMap<>();
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
        LifePathComponentStorage initialStorage = getFixedXPTabStorage(attributes, traits, skills, abilities);
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

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage, gameYear);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits, FIXED_XP);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage, gameYear);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills, FIXED_XP);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage, gameYear);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo, FIXED_XP);
            abilities.clear();
            abilities.putAll(picker.getSelectedAbilities());

            standardizedActions(attributes, traits, skills, abilities, txtFixedXP, initialStorage, gameYear);
        });

        // Add panels and then add Tab
        pnlFixedXP.add(buttonsPanel, BorderLayout.NORTH);
        pnlFixedXP.add(pnlDisplay, BorderLayout.CENTER);

        return pnlFixedXP;
    }

    private void standardizedActions(Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills,
          Map<String, Integer> abilities, JEditorPane txtFixedXP, LifePathComponentStorage initialStorage,
          int gameYear) {
        LifePathComponentStorage storage = getFixedXPTabStorage(attributes, traits, skills, abilities);
        String fixedXPText = buildFixedXPText(storage);
        txtFixedXP.setText(fixedXPText);

        fixedXPTabStorage = initialStorage;
        fixedXPTabTextStorage = fixedXPText;

        parent.updateTxtProgress(gameYear);
        parent.setVisible(true);
    }

    private static LifePathComponentStorage getFixedXPTabStorage(Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills,
          Map<String, Integer> abilities) {
        Map<String, Integer> skillNames = new HashMap<>();
        for (String skill : skills.keySet()) {
            skillNames.put(skill, skills.get(skill));
        }

        return new LifePathComponentStorage(0,
              new ArrayList<>(),
              new ArrayList<>(),
              new HashMap<>(),
              attributes,
              traits,
              skillNames,
              abilities);
    }

    private String buildFixedXPText(LifePathComponentStorage storage) {
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
        Map<String, Integer> skills = storage.skills();
        if (!skills.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = skills.size();
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                int value = entry.getValue();

                String label = entry.getKey().replace(SkillType.RP_ONLY_TAG, "");

                progressText.append(label);
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
        Map<String, Integer> selectedSPAs = storage.abilities();
        if (!selectedSPAs.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = selectedSPAs.size();
            for (Map.Entry<String, Integer> entry : selectedSPAs.entrySet()) {
                String abilityName = entry.getKey();
                CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);
                if (abilityInfo == null) {
                    LOGGER.warn("Could not find AbilityInfo for abilityName: {}", abilityName);
                    continue;
                }

                SpecialAbility ability = abilityInfo.getAbility();
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
