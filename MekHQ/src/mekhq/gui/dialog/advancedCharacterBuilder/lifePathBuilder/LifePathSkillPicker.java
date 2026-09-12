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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathPickerUtilities.clampSpinnerValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.common.annotations.Nullable;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Lets an author set skill levels a Life Path requires or excludes, or skill XP it awards.
 *
 * <p>Two kinds of row appear: one per named skill, and one per meta skill, which stands for a whole family of skills
 * at once. They are split across tabs because there are over a hundred skills.</p>
 *
 * <p>This picker also serves the Natural Aptitudes buttons, which award XP towards raising a skill's aptitude rather
 * than the skill itself.</p>
 *
 * @since 0.50.11
 */
class LifePathSkillPicker extends AbstractLifePathPicker {
    private static final MMLogger LOGGER = MMLogger.create(LifePathSkillPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSkillPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(525);

    /** Highest level a meta skill row offers on the Requirements and Exclusions tabs. */
    private static final int MAXIMUM_META_SKILL_LEVEL = 10;

    private final LifePathBuilderTabType tabType;

    private final Map<String, Integer> storedSkillLevels;
    private Map<String, Integer> selectedSkillLevels;

    private final Map<SkillSubType, Integer> storedMetaSkillLevels;
    private Map<SkillSubType, Integer> selectedMetaSkillLevels;

    Map<String, Integer> getSelectedSkillLevels() {
        return selectedSkillLevels;
    }

    Map<SkillSubType, Integer> getSelectedMetaSkillLevels() {
        return selectedMetaSkillLevels;
    }

    /**
     * Opens the picker.
     *
     * @param owner                   the wizard this picker belongs to
     * @param selectedSkillLevels     the per-skill values already on this group, which may be {@code null}
     * @param selectedMetaSkillLevels the per-meta-skill values already on this group, which may be {@code null}
     * @param tabType                 the section being edited
     * @param groupIndex              the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathSkillPicker(@Nullable Window owner, @Nullable Map<String, Integer> selectedSkillLevels,
          @Nullable Map<SkillSubType, Integer> selectedMetaSkillLevels, LifePathBuilderTabType tabType,
          int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathSkillPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;

        // Defensive copies to avoid external modification
        this.selectedSkillLevels = selectedSkillLevels == null ?
                                         new HashMap<>() :
                                         new HashMap<>(selectedSkillLevels);
        storedSkillLevels = new HashMap<>(this.selectedSkillLevels);

        this.selectedMetaSkillLevels = selectedMetaSkillLevels == null ?
                                             new HashMap<>() :
                                             new HashMap<>(selectedMetaSkillLevels);
        storedMetaSkillLevels = new HashMap<>(this.selectedMetaSkillLevels);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<SkillType> combatSkills = new ArrayList<>();
        List<SkillType> supportSkills = new ArrayList<>();
        List<SkillType> utilitySkills = new ArrayList<>();
        List<SkillType> roleplaySkills1 = new ArrayList<>();
        List<SkillType> roleplaySkills2 = new ArrayList<>();
        List<SkillType> roleplaySkills3 = new ArrayList<>();
        List<SkillType> roleplaySkills4 = new ArrayList<>();
        List<SkillType> roleplaySkills5 = new ArrayList<>();
        List<String> allSkills = new ArrayList<>(List.of(SkillType.getSkillList()));
        Collections.sort(allSkills);

        List<SkillSubType> metaSkills = new ArrayList<>(List.of(SkillSubType.values()));
        metaSkills.remove(SkillSubType.NONE);
        // By display name, not by ordinal: Collections.sort on an enum list orders by declaration, which is not the
        // order the author reads on screen.
        metaSkills.sort(Comparator.comparing(SkillSubType::getDisplayName));

        // Normal Skills
        for (String skillName : new ArrayList<>(allSkills)) {
            SkillType type = SkillType.getType(skillName);

            // SkillType.getType is @Nullable and logs its own error when a name does not resolve. Dereferencing it
            // straight away took the whole picker down over one bad name.
            if (type == null) {
                allSkills.remove(skillName);
                continue;
            }

            if (type.isCombatSkill()) {
                combatSkills.add(type);
                allSkills.remove(skillName);
            } else if (type.isSupportSkill()) {
                supportSkills.add(type);
                allSkills.remove(skillName);
            } else if (type.isUtilitySkill()) {
                utilitySkills.add(type);
                allSkills.remove(skillName);
            }
        }

        // Roleplay Skills
        int groups = 3; // Can go up to 5 without additional code changes
        int n = allSkills.size();
        for (int i = 0; i < n; i++) {
            String skillName = allSkills.get(i);
            SkillType skill = SkillType.getType(skillName);
            int groupIdx = (int) Math.floor(i * groups / (double) n);
            switch (groupIdx) {
                case 0 -> roleplaySkills1.add(skill);
                case 1 -> roleplaySkills2.add(skill);
                case 2 -> roleplaySkills3.add(skill);
                case 3 -> roleplaySkills4.add(skill);
                case 4 -> roleplaySkills5.add(skill);
            }
        }

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        if (!combatSkills.isEmpty()) {
            FastJScrollPane pnlCombatSkills = getSkillOptions(combatSkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.combat.label"),
                  pnlCombatSkills);
        }

        if (!supportSkills.isEmpty()) {
            FastJScrollPane pnlSupportSkills = getSkillOptions(supportSkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.support.label"),
                  pnlSupportSkills);
        }

        if (!utilitySkills.isEmpty()) {
            FastJScrollPane pnlUtilitySkills = getSkillOptions(utilitySkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.utility.label"),
                  pnlUtilitySkills);
        }

        if (!roleplaySkills1.isEmpty()) {
            buildTab(roleplaySkills1, optionPane, getSkillOptions(roleplaySkills1, tabType));
        }

        if (!roleplaySkills2.isEmpty()) {
            buildTab(roleplaySkills2, optionPane, getSkillOptions(roleplaySkills2, tabType));
        }

        if (!roleplaySkills3.isEmpty()) {
            buildTab(roleplaySkills3, optionPane, getSkillOptions(roleplaySkills3, tabType));
        }

        if (!roleplaySkills4.isEmpty()) {
            buildTab(roleplaySkills4, optionPane, getSkillOptions(roleplaySkills4, tabType));
        }

        if (!roleplaySkills5.isEmpty()) {
            buildTab(roleplaySkills5, optionPane, getSkillOptions(roleplaySkills5, tabType));
        }

        // Meta Skills
        if (tabType == LifePathBuilderTabType.FIXED_XP || tabType == LifePathBuilderTabType.FLEXIBLE_XP) {
            FastJScrollPane pnlMetaSkills = getMetaSkillOptions(metaSkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.meta.label"),
                  pnlMetaSkills);
        }

        pnlOptions.add(optionPane);
        return pnlOptions;
    }

    private static void buildTab(List<SkillType> skills, EnhancedTabbedPane optionPane,
          FastJScrollPane pnlOptions) {
        String firstName = skills.get(0).getName();
        String lastName = skills.get(skills.size() - 1).getName();

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.roleplay.label", firstLetter,
              lastLetter), pnlOptions);
    }

    private FastJScrollPane getSkillOptions(List<SkillType> skills, LifePathBuilderTabType tabType) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < skills.size(); i++) {
            SkillType type = skills.get(i);
            String label = type.getName();

            int minimumSkillLevel = switch (tabType) {
                case REQUIREMENTS, EXCLUSIONS -> 0;
                case FIXED_XP, FLEXIBLE_XP -> -1000;
            };
            int maximumSkillLevel = switch (tabType) {
                case REQUIREMENTS, EXCLUSIONS -> type.getMaxLevel();
                case FIXED_XP, FLEXIBLE_XP -> 1000;
            };

            int keyValue = switch (tabType) {
                case REQUIREMENTS -> minimumSkillLevel;
                case EXCLUSIONS -> maximumSkillLevel;
                case FIXED_XP, FLEXIBLE_XP -> 0;
            };

            // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
            // SpinnerNumberModel throws when its initial value is out of range.
            int storedValue = selectedSkillLevels.getOrDefault(type.getName(), keyValue);
            int defaultValue = clampSpinnerValue(storedValue, minimumSkillLevel, maximumSkillLevel, label);

            JLabel lblSkill = new JLabel(label);
            JSpinner spnSkillLevel = new JSpinner(new SpinnerNumberModel(defaultValue, minimumSkillLevel,
                  maximumSkillLevel, 1));

            spnSkillLevel.addChangeListener(changeEvent -> {
                int value = (int) spnSkillLevel.getValue();
                // Deliberately not compared against the value the spinner started at. Changing a spinner and
                // then changing it back must write the original number, otherwise the map keeps the stale one.
                if (value == keyValue) {
                    selectedSkillLevels.remove(type.getName());
                } else {
                    selectedSkillLevels.put(type.getName(), value);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblSkill);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnSkillLevel);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    private FastJScrollPane getMetaSkillOptions(List<SkillSubType> metaSkills, LifePathBuilderTabType tabType) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < metaSkills.size(); i++) {
            SkillSubType metaSkill = metaSkills.get(i);
            String label = metaSkill.getDisplayName();

            int minimumSkillLevel = switch (tabType) {
                case REQUIREMENTS, EXCLUSIONS -> 0;
                case FIXED_XP, FLEXIBLE_XP -> -1000;
            };
            int maximumSkillLevel = switch (tabType) {
                case REQUIREMENTS, EXCLUSIONS -> MAXIMUM_META_SKILL_LEVEL;
                case FIXED_XP, FLEXIBLE_XP -> 1000;
            };

            int keyValue = switch (tabType) {
                case REQUIREMENTS -> minimumSkillLevel;
                case EXCLUSIONS -> maximumSkillLevel;
                case FIXED_XP, FLEXIBLE_XP -> 0;
            };

            // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
            // SpinnerNumberModel throws when its initial value is out of range.
            int storedValue = selectedMetaSkillLevels.getOrDefault(metaSkill, keyValue);
            int defaultValue = clampSpinnerValue(storedValue, minimumSkillLevel, maximumSkillLevel, label);

            JLabel lblMetaSkill = new JLabel(label);
            JSpinner spnSkillLevel = new JSpinner(new SpinnerNumberModel(defaultValue, minimumSkillLevel,
                  maximumSkillLevel, 1));

            spnSkillLevel.addChangeListener(changeEvent -> {
                int value = (int) spnSkillLevel.getValue();
                // Deliberately not compared against the value the spinner started at. Changing a spinner and
                // then changing it back must write the original number, otherwise the map keeps the stale one.
                if (value == keyValue) {
                    selectedMetaSkillLevels.remove(metaSkill);
                } else {
                    selectedMetaSkillLevels.put(metaSkill, value);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblMetaSkill);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnSkillLevel);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedSkillLevels = new HashMap<>(storedSkillLevels);
        selectedMetaSkillLevels = new HashMap<>(storedMetaSkillLevels);
    }

    @Override
    protected void clearSelection() {
        selectedSkillLevels.clear();
        selectedMetaSkillLevels.clear();
        rebuildOptions();
    }
}
