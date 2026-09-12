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
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Lets an author set skill levels a Life Path requires or excludes, or skill XP it awards.
 *
 * <p>Two kinds of row appear: one per named skill, and one per meta skill, which stands for a whole family of skills
 * at once. They are split across tabs because there are over a hundred skills.</p>
 *
 * <p>On the Fixed XP and Flexible XP tabs every skill and meta skill also gets a second spinner, labelled
 * {@code Natural Aptitude - [Skill]}. XP entered there goes towards the character gaining a Natural Aptitude for the
 * skill rather than towards the skill itself, and is stored separately from the skill XP.</p>
 *
 * @since 0.50.11
 */
class LifePathSkillPicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSkillPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(525);

    /** Highest level a meta skill row offers on the Requirements and Exclusions tabs. */
    private static final int MAXIMUM_META_SKILL_LEVEL = 10;

    /** Lowest and highest XP an award spinner offers. Negative awards are allowed, so a path can take XP away. */
    private static final int XP_SPINNER_LOWEST_VALUE = -1000;
    private static final int XP_SPINNER_HIGHEST_VALUE = 1000;

    /** Value that means "no entry" on an XP tab: an award of nothing is not stored. */
    private static final int XP_KEY_VALUE = 0;

    /** Rows per line on the Requirements and Exclusions tabs, which show one spinner per skill. */
    private static final int LEVEL_COLUMNS = 3;

    private final LifePathBuilderTabType tabType;
    private final boolean isXPTab;

    private final Map<String, Integer> storedSkillLevels;
    private final Map<String, Integer> selectedSkillLevels;

    private final Map<SkillSubType, Integer> storedMetaSkillLevels;
    private final Map<SkillSubType, Integer> selectedMetaSkillLevels;

    private final Map<String, Integer> storedNaturalAptitudes;
    private final Map<String, Integer> selectedNaturalAptitudes;

    private final Map<SkillSubType, Integer> storedNaturalAptitudesMetaSkills;
    private final Map<SkillSubType, Integer> selectedNaturalAptitudesMetaSkills;

    Map<String, Integer> getSelectedSkillLevels() {
        return selectedSkillLevels;
    }

    Map<SkillSubType, Integer> getSelectedMetaSkillLevels() {
        return selectedMetaSkillLevels;
    }

    /**
     * @return XP awarded towards each skill's Natural Aptitude, keyed by skill name; always empty for the Requirements
     *       and Exclusions tabs, which offer no such rows
     */
    Map<String, Integer> getSelectedNaturalAptitudes() {
        return selectedNaturalAptitudes;
    }

    /**
     * @return XP awarded towards each meta skill's Natural Aptitude; always empty for the Requirements and Exclusions
     *       tabs, which offer no such rows
     */
    Map<SkillSubType, Integer> getSelectedNaturalAptitudesMetaSkills() {
        return selectedNaturalAptitudesMetaSkills;
    }

    /**
     * Opens the picker.
     *
     * @param owner                              the wizard this picker belongs to
     * @param selectedSkillLevels                the per-skill values already on this group, which may be {@code null}
     * @param selectedMetaSkillLevels            the per-meta-skill values already on this group, which may be
     *                                           {@code null}
     * @param selectedNaturalAptitudes           the per-skill Natural Aptitude XP already on this group, which may be
     *                                           {@code null}; ignored on the Requirements and Exclusions tabs
     * @param selectedNaturalAptitudesMetaSkills the per-meta-skill Natural Aptitude XP already on this group, which
     *                                           may be {@code null}; ignored on the Requirements and Exclusions tabs
     * @param tabType                            the section being edited
     * @param groupIndex                         the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathSkillPicker(@Nullable Window owner, @Nullable Map<String, Integer> selectedSkillLevels,
          @Nullable Map<SkillSubType, Integer> selectedMetaSkillLevels,
          @Nullable Map<String, Integer> selectedNaturalAptitudes,
          @Nullable Map<SkillSubType, Integer> selectedNaturalAptitudesMetaSkills, LifePathBuilderTabType tabType,
          int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathSkillPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;
        isXPTab = tabType == LifePathBuilderTabType.FIXED_XP || tabType == LifePathBuilderTabType.FLEXIBLE_XP;

        // Defensive copies to avoid external modification. The "selected" maps are the live ones the spinners write
        // to; the "stored" maps are what Cancel puts back.
        this.selectedSkillLevels = copyOrEmpty(selectedSkillLevels);
        storedSkillLevels = new HashMap<>(this.selectedSkillLevels);

        this.selectedMetaSkillLevels = copyOrEmpty(selectedMetaSkillLevels);
        storedMetaSkillLevels = new HashMap<>(this.selectedMetaSkillLevels);

        this.selectedNaturalAptitudes = copyOrEmpty(selectedNaturalAptitudes);
        storedNaturalAptitudes = new HashMap<>(this.selectedNaturalAptitudes);

        this.selectedNaturalAptitudesMetaSkills = copyOrEmpty(selectedNaturalAptitudesMetaSkills);
        storedNaturalAptitudesMetaSkills = new HashMap<>(this.selectedNaturalAptitudesMetaSkills);

        buildAndShow();
    }

    private static <K> Map<K, Integer> copyOrEmpty(@Nullable Map<K, Integer> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
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
            FastJScrollPane pnlCombatSkills = getSkillOptions(combatSkills);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.combat.label"),
                  pnlCombatSkills);
        }

        if (!supportSkills.isEmpty()) {
            FastJScrollPane pnlSupportSkills = getSkillOptions(supportSkills);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.support.label"),
                  pnlSupportSkills);
        }

        if (!utilitySkills.isEmpty()) {
            FastJScrollPane pnlUtilitySkills = getSkillOptions(utilitySkills);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.utility.label"),
                  pnlUtilitySkills);
        }

        if (!roleplaySkills1.isEmpty()) {
            buildTab(roleplaySkills1, optionPane, getSkillOptions(roleplaySkills1));
        }

        if (!roleplaySkills2.isEmpty()) {
            buildTab(roleplaySkills2, optionPane, getSkillOptions(roleplaySkills2));
        }

        if (!roleplaySkills3.isEmpty()) {
            buildTab(roleplaySkills3, optionPane, getSkillOptions(roleplaySkills3));
        }

        if (!roleplaySkills4.isEmpty()) {
            buildTab(roleplaySkills4, optionPane, getSkillOptions(roleplaySkills4));
        }

        if (!roleplaySkills5.isEmpty()) {
            buildTab(roleplaySkills5, optionPane, getSkillOptions(roleplaySkills5));
        }

        // Meta Skills
        if (isXPTab) {
            FastJScrollPane pnlMetaSkills = getMetaSkillOptions(metaSkills);
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

    /**
     * Builds one tab of skill rows.
     *
     * <p>On the Requirements and Exclusions tabs the rows are one spinner each and sit three to a line. On the XP
     * tabs each skill takes a whole line: its XP spinner on the left and its Natural Aptitude spinner on the right,
     * so the two entries for a skill are always read together.</p>
     *
     * @param skills the skills this tab shows
     *
     * @return the scrollable tab contents
     */
    private FastJScrollPane getSkillOptions(List<SkillType> skills) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int minimumValue = getMinimumValue();

        for (int index = 0; index < skills.size(); index++) {
            SkillType type = skills.get(index);
            String skillName = type.getName();
            int maximumValue = getMaximumValue(type.getMaxLevel());
            int keyValue = getKeyValue(minimumValue, maximumValue);

            JPanel pnlSkillRow = buildSpinnerRow(skillName, skillName, selectedSkillLevels, minimumValue,
                  maximumValue, keyValue);

            if (isXPTab) {
                gbc.gridx = 0;
                gbc.gridy = index;
                pnlSkills.add(pnlSkillRow, gbc);

                String naturalAptitudeLabel = getFormattedTextAt(RESOURCE_BUNDLE,
                      "LifePathSkillPicker.naturalAptitude.label", skillName);
                JPanel pnlNaturalAptitudeRow = buildSpinnerRow(naturalAptitudeLabel, skillName,
                      selectedNaturalAptitudes, XP_SPINNER_LOWEST_VALUE, XP_SPINNER_HIGHEST_VALUE, XP_KEY_VALUE);
                gbc.gridx = 1;
                pnlSkills.add(pnlNaturalAptitudeRow, gbc);
            } else {
                gbc.gridx = index % LEVEL_COLUMNS;
                gbc.gridy = index / LEVEL_COLUMNS;
                pnlSkills.add(pnlSkillRow, gbc);
            }
        }

        return wrapInScrollPane(pnlSkills);
    }

    /**
     * Builds the meta skill tab, which only the XP tabs show.
     *
     * <p>Laid out like the XP skill tabs: the meta skill's XP spinner on the left, its Natural Aptitude spinner on
     * the right.</p>
     *
     * @param metaSkills the meta skills to show
     *
     * @return the scrollable tab contents
     */
    private FastJScrollPane getMetaSkillOptions(List<SkillSubType> metaSkills) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int minimumValue = getMinimumValue();
        int maximumValue = getMaximumValue(MAXIMUM_META_SKILL_LEVEL);
        int keyValue = getKeyValue(minimumValue, maximumValue);

        for (int index = 0; index < metaSkills.size(); index++) {
            SkillSubType metaSkill = metaSkills.get(index);
            String label = metaSkill.getDisplayName();

            JPanel pnlMetaSkillRow = buildSpinnerRow(label, metaSkill, selectedMetaSkillLevels, minimumValue,
                  maximumValue, keyValue);
            gbc.gridx = 0;
            gbc.gridy = index;
            pnlSkills.add(pnlMetaSkillRow, gbc);

            String naturalAptitudeLabel = getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathSkillPicker.naturalAptitude.label", label);
            JPanel pnlNaturalAptitudeRow = buildSpinnerRow(naturalAptitudeLabel, metaSkill,
                  selectedNaturalAptitudesMetaSkills, XP_SPINNER_LOWEST_VALUE, XP_SPINNER_HIGHEST_VALUE,
                  XP_KEY_VALUE);
            gbc.gridx = 1;
            pnlSkills.add(pnlNaturalAptitudeRow, gbc);
        }

        return wrapInScrollPane(pnlSkills);
    }

    /**
     * Builds a label and a spinner, and wires the spinner to one entry of a selection map.
     *
     * <p>Moving the spinner to {@code keyValue} removes the entry rather than storing the key value, so an untouched
     * or reset row leaves nothing in the map.</p>
     *
     * @param label        the text shown beside the spinner
     * @param key          the map entry this spinner edits
     * @param selection    the live map the spinner writes to
     * @param minimumValue the lowest value the spinner offers
     * @param maximumValue the highest value the spinner offers
     * @param keyValue     the value that means "no entry"
     * @param <K>          the map's key type
     *
     * @return the row, ready to add to a grid
     */
    private <K> JPanel buildSpinnerRow(String label, K key, Map<K, Integer> selection, int minimumValue,
          int maximumValue, int keyValue) {
        // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
        // SpinnerNumberModel throws when its initial value is out of range.
        int storedValue = selection.getOrDefault(key, keyValue);
        int defaultValue = clampSpinnerValue(storedValue, minimumValue, maximumValue, label);

        JLabel lblRow = new JLabel(label);
        JSpinner spnRow = new JSpinner(new SpinnerNumberModel(defaultValue, minimumValue, maximumValue, 1));

        spnRow.addChangeListener(changeEvent -> {
            int value = (int) spnRow.getValue();
            // Deliberately not compared against the value the spinner started at. Changing a spinner and
            // then changing it back must write the original number, otherwise the map keeps the stale one.
            if (value == keyValue) {
                selection.remove(key);
            } else {
                selection.put(key, value);
            }
        });

        JPanel pnlRow = new JPanel();
        pnlRow.setLayout(new BoxLayout(pnlRow, BoxLayout.X_AXIS));
        pnlRow.add(lblRow);
        pnlRow.add(Box.createHorizontalStrut(PADDING));
        pnlRow.add(spnRow);
        pnlRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        return pnlRow;
    }

    private static FastJScrollPane wrapInScrollPane(JPanel pnlSkills) {
        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    /** @return the lowest value a skill or meta skill spinner offers on this tab */
    private int getMinimumValue() {
        return switch (tabType) {
            case REQUIREMENTS, EXCLUSIONS -> 0;
            case FIXED_XP, FLEXIBLE_XP -> XP_SPINNER_LOWEST_VALUE;
        };
    }

    /**
     * @param maximumLevel the highest level the skill or meta skill can reach
     *
     * @return the highest value a skill or meta skill spinner offers on this tab
     */
    private int getMaximumValue(int maximumLevel) {
        return switch (tabType) {
            case REQUIREMENTS, EXCLUSIONS -> maximumLevel;
            case FIXED_XP, FLEXIBLE_XP -> XP_SPINNER_HIGHEST_VALUE;
        };
    }

    /**
     * @param minimumValue the row's minimum, which is the "no entry" value for a requirement
     * @param maximumValue the row's maximum, which is the "no entry" value for an exclusion, since barring a level
     *                     the skill cannot exceed bars nothing
     *
     * @return the value that means "no entry" for a skill or meta skill spinner on this tab
     */
    private int getKeyValue(int minimumValue, int maximumValue) {
        return switch (tabType) {
            case REQUIREMENTS -> minimumValue;
            case EXCLUSIONS -> maximumValue;
            case FIXED_XP, FLEXIBLE_XP -> XP_KEY_VALUE;
        };
    }

    @Override
    protected void restoreStoredSelection() {
        // Put back in place rather than reassigned: the spinner listeners hold these map instances.
        restore(selectedSkillLevels, storedSkillLevels);
        restore(selectedMetaSkillLevels, storedMetaSkillLevels);
        restore(selectedNaturalAptitudes, storedNaturalAptitudes);
        restore(selectedNaturalAptitudesMetaSkills, storedNaturalAptitudesMetaSkills);
    }

    private static <K> void restore(Map<K, Integer> selection, Map<K, Integer> stored) {
        selection.clear();
        selection.putAll(stored);
    }

    @Override
    protected void clearSelection() {
        selectedSkillLevels.clear();
        selectedMetaSkillLevels.clear();
        selectedNaturalAptitudes.clear();
        selectedNaturalAptitudesMetaSkills.clear();
        rebuildOptions();
    }
}
