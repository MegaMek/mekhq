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

import static java.lang.Math.min;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_EDGE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_EDGE_SCORE;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

public class LifePathTab {
    private final static MMLogger LOGGER = MMLogger.create(LifePathTab.class);

    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    final static int ATTRIBUTE_SPINNER_LOWEST_VALUE = -1000;
    final static int ATTRIBUTE_SPINNER_HIGHEST_VALUE = 1000;

    private final Factions factions = Factions.getInstance();
    private final Systems planetarySystems = Systems.getInstance();

    private final LifePathBuilderDialog parent;
    private final EnhancedTabbedPane tabGlobal;
    private final EnhancedTabbedPane tabLocal = new EnhancedTabbedPane();
    private final LocalDate today;
    private final int gameYear;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();
    private final LifePathBuilderTabType tabType;
    private final String tabName;
    private final Map<UUID, LifePath> lifePathLibrary;

    /**
     * What every group in this section holds, keyed by group index.
     *
     * <p>One map of {@link LifePathGroup} rather than thirteen parallel maps keyed by the same index. Adding,
     * removing, duplicating and resetting a group each used to be written out thirteen times.</p>
     *
     * <p>A {@link TreeMap} so the groups iterate in index order. The old maps were {@link HashMap}s, and the progress
     * text relied on small {@link Integer} keys happening to come back in order.</p>
     */
    private final Map<Integer, LifePathGroup> groups = new TreeMap<>();

    private final JLabel lblFlexibleXPPicks = new JLabel();
    private final JSpinner spnFlexibleXPPicks = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));

    /**
     * Returns the group at the given index, creating it if this section does not have one yet.
     *
     * @param groupIndex the group's index
     *
     * @return the group
     *
     * @since 0.50.11
     */
    private LifePathGroup groupFor(int groupIndex) {
        return groups.computeIfAbsent(groupIndex, index -> new LifePathGroup());
    }

    /**
     * Returns one kind of entry from every group, keyed by group index.
     *
     * <p>This is how the section presents itself to the rest of the wizard, which still thinks in terms of
     * "the skills of every group" rather than "every group's skills".</p>
     *
     * @param accessor which part of a group to read
     * @param <T>      the type that part holds
     *
     * @return that part of every group, in index order
     *
     * @since 0.50.11
     */
    private <T> Map<Integer, T> collectFromGroups(Function<LifePathGroup, T> accessor) {
        Map<Integer, T> collected = new TreeMap<>();

        for (Map.Entry<Integer, LifePathGroup> entry : groups.entrySet()) {
            collected.put(entry.getKey(), accessor.apply(entry.getValue()));
        }

        return collected;
    }

    /**
     * Writes one kind of entry into the groups it belongs to, creating groups as needed.
     *
     * @param source   the entries to write, keyed by group index
     * @param mutator  which part of a group to write
     * @param <T>      the type that part holds
     *
     * @since 0.50.11
     */
    private <T> void applyToGroups(Map<Integer, T> source, BiConsumer<LifePathGroup, T> mutator) {
        if (source == null) {
            return;
        }

        for (Map.Entry<Integer, T> entry : source.entrySet()) {
            mutator.accept(groupFor(entry.getKey()), entry.getValue());
        }
    }

    public Map<Integer, Set<String>> getFactions() {
        return collectFromGroups(LifePathGroup::getFactions);
    }

    public void setFactions(Map<Integer, Set<String>> factions) {
        applyToGroups(factions, LifePathGroup::setFactions);
    }

    public Map<Integer, Set<String>> getSystems() {
        return collectFromGroups(LifePathGroup::getSystems);
    }

    public void setSystems(Map<Integer, Set<String>> systems) {
        applyToGroups(systems, LifePathGroup::setSystems);
    }

    public Map<Integer, Set<UUID>> getLifePaths() {
        return collectFromGroups(LifePathGroup::getLifePaths);
    }

    public void setLifePaths(Map<Integer, Set<UUID>> lifePaths) {
        applyToGroups(lifePaths, LifePathGroup::setLifePaths);
    }

    public Map<Integer, Map<LifePathCategory, Integer>> getCategories() {
        return collectFromGroups(LifePathGroup::getCategories);
    }

    public void setCategories(Map<Integer, Map<LifePathCategory, Integer>> categories) {
        applyToGroups(categories, LifePathGroup::setCategories);
    }

    public Map<Integer, Map<SkillAttribute, Integer>> getAttributes() {
        return collectFromGroups(LifePathGroup::getAttributes);
    }

    public void setAttributes(Map<Integer, Map<SkillAttribute, Integer>> attributes) {
        applyToGroups(attributes, LifePathGroup::setAttributes);
    }

    /**
     * Retrieves each group's Edge value.
     *
     * <p><b>Warning:</b> a value can be {@code null}, which means the author set no Edge rule for that group.</p>
     *
     * @return each group's Edge value, keyed by group index
     */
    public Map<Integer, Integer> getEdge() {
        return collectFromGroups(LifePathGroup::getEdge);
    }

    public void setEdge(Map<Integer, Integer> edge) {
        applyToGroups(edge, LifePathGroup::setEdge);
    }

    /**
     * Retrieves each group's "any attribute" value.
     *
     * <p><b>Warning:</b> a value can be {@code null}, which means the author set no rule for that group.</p>
     *
     * @return each group's "any attribute" value, keyed by group index
     */
    public Map<Integer, Integer> getFlexibleAttribute() {
        return collectFromGroups(LifePathGroup::getFlexibleAttribute);
    }

    public void setFlexibleAttribute(Map<Integer, Integer> flexibleAttribute) {
        applyToGroups(flexibleAttribute, LifePathGroup::setFlexibleAttribute);
    }

    public Map<Integer, Map<ATOWTraits, Integer>> getTraits() {
        return collectFromGroups(LifePathGroup::getTraits);
    }

    public void setTraits(Map<Integer, Map<ATOWTraits, Integer>> traits) {
        applyToGroups(traits, LifePathGroup::setTraits);
    }

    public Map<Integer, Map<String, Integer>> getSkills() {
        return collectFromGroups(LifePathGroup::getSkills);
    }

    public void setSkills(Map<Integer, Map<String, Integer>> skills) {
        applyToGroups(skills, LifePathGroup::setSkills);
    }

    public Map<Integer, Map<String, Integer>> getNaturalAptitudes() {
        return collectFromGroups(LifePathGroup::getNaturalAptitudes);
    }

    public void setNaturalAptitudes(Map<Integer, Map<String, Integer>> naturalAptitudes) {
        applyToGroups(naturalAptitudes, LifePathGroup::setNaturalAptitudes);
    }

    public Map<Integer, Map<SkillSubType, Integer>> getNaturalAptitudesMetaSkills() {
        return collectFromGroups(LifePathGroup::getNaturalAptitudesMetaSkills);
    }

    public void setNaturalAptitudesMetaSkills(Map<Integer, Map<SkillSubType, Integer>> naturalAptitudesMetaSkills) {
        applyToGroups(naturalAptitudesMetaSkills, LifePathGroup::setNaturalAptitudesMetaSkills);
    }

    public Map<Integer, Map<SkillSubType, Integer>> getMetaSkills() {
        return collectFromGroups(LifePathGroup::getMetaSkills);
    }

    public void setMetaSkills(Map<Integer, Map<SkillSubType, Integer>> metaSkills) {
        applyToGroups(metaSkills, LifePathGroup::setMetaSkills);
    }

    public Map<Integer, Map<String, Integer>> getAbilities() {
        return collectFromGroups(LifePathGroup::getAbilities);
    }

    public void setAbilities(Map<Integer, Map<String, Integer>> abilities) {
        applyToGroups(abilities, LifePathGroup::setAbilities);
    }

    public int getPickCount() {
        return min((int) spnFlexibleXPPicks.getValue(), getTabCount());
    }

    public void setPickCount(int pickCount) {
        spnFlexibleXPPicks.setValue(pickCount);
    }

    public int getTabCount() {
        return tabLocal.getTabCount();
    }

    EnhancedTabbedPane getLocalTab() {
        return tabLocal;
    }

    LifePathTab(LifePathBuilderDialog parent, EnhancedTabbedPane tabGlobal, LocalDate today,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo, LifePathBuilderTabType tabType,
          Map<UUID, LifePath> lifePathLibrary) {
        this.parent = parent;
        this.tabGlobal = tabGlobal;
        this.today = today;
        this.gameYear = today.getYear();
        this.allAbilityInfo.putAll(allAbilityInfo);
        this.tabType = tabType;
        this.tabName = tabType.getLookupName();
        this.lifePathLibrary = lifePathLibrary;
    }

    protected void buildTab() {
        final boolean enableGroupControls = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                                  tabType == LifePathBuilderTabType.FLEXIBLE_XP;

        JPanel pnlLocal = new JPanel(new BorderLayout());
        pnlLocal.setName(tabName);
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title." + tabName);
        tabGlobal.addTab(title, pnlLocal);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JPanel panelButtonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        RoundedJButton btnAddGroup = getAddGroup(panelButtonsRow);
        btnAddGroup.setVisible(enableGroupControls);
        btnAddGroup.addActionListener(actionEvent -> {
            addTab();
            tabLocal.setSelectedIndex(getTabCount() - 1);
        });

        RoundedJButton btnRemoveGroup = getRemoveGroup(panelButtonsRow);
        btnRemoveGroup.setVisible(enableGroupControls);
        btnRemoveGroup.addActionListener(actionEvent -> removeGroup());

        RoundedJButton btnDuplicateGroup = getDuplicateGroup(panelButtonsRow);
        btnDuplicateGroup.setVisible(enableGroupControls);
        btnDuplicateGroup.addActionListener(actionEvent -> duplicateGroup());

        JPanel panelPicksRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buildFlexiblePicksPanel(panelPicksRow);
        spnFlexibleXPPicks.setVisible(tabType == LifePathBuilderTabType.FLEXIBLE_XP);
        lblFlexibleXPPicks.setVisible(tabType == LifePathBuilderTabType.FLEXIBLE_XP);

        buttonPanel.add(panelButtonsRow);
        buttonPanel.add(panelPicksRow);

        if (!enableGroupControls) {
            addTab();
        }

        pnlLocal.add(buttonPanel, BorderLayout.NORTH);
        pnlLocal.add(tabLocal, BorderLayout.CENTER);
    }

    private void buildFlexiblePicksPanel(JPanel panelPicksRow) {
        String titlePicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexible_xp.button.pickCount.label");
        String tooltipPicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexible_xp.button.pickCount.tooltip");
        lblFlexibleXPPicks.setText(titlePicks);
        lblFlexibleXPPicks.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipPicks)
        );
        spnFlexibleXPPicks.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipPicks)
        );
        spnFlexibleXPPicks.addChangeListener(changeEvent -> parent.updateTxtProgress());
        panelPicksRow.add(lblFlexibleXPPicks);
        panelPicksRow.add(spnFlexibleXPPicks);
    }

    private RoundedJButton getDuplicateGroup(JPanel buttonPanel) {
        String titleDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.label");
        String tooltipDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.tooltip");
        RoundedJButton btnDuplicateGroup = new RoundedJButton(titleDuplicateGroup);
        btnDuplicateGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipDuplicateGroup)
        );
        buttonPanel.add(btnDuplicateGroup);
        return btnDuplicateGroup;
    }

    private RoundedJButton getRemoveGroup(JPanel buttonPanel) {
        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.tooltip");
        RoundedJButton btnRemoveGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveGroup);
        return btnRemoveGroup;
    }

    private RoundedJButton getAddGroup(JPanel buttonPanel) {
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.tooltip");
        RoundedJButton btnAddGroup = new RoundedJButton(titleAddGroup);
        btnAddGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddGroup)
        );
        buttonPanel.add(btnAddGroup);

        return btnAddGroup;
    }

    /**
     * Removes the selected group and closes the gap its index leaves behind.
     *
     * <p>Every group above the removed one shifts down by one, because the indexes are the keys the saved file uses
     * and they have to stay consecutive. This was 170 lines of thirteen identical shift blocks before the groups
     * became one object.</p>
     *
     * @since 0.50.11
     */
    private void removeGroup() {
        if (tabLocal.getTabCount() == 0) {
            return;
        }

        int selectedIndex = tabLocal.getSelectedIndex();

        Map<Integer, LifePathGroup> shifted = new TreeMap<>();
        for (Map.Entry<Integer, LifePathGroup> entry : groups.entrySet()) {
            int groupIndex = entry.getKey();

            if (groupIndex < selectedIndex) {
                shifted.put(groupIndex, entry.getValue());
            } else if (groupIndex > selectedIndex) {
                shifted.put(groupIndex - 1, entry.getValue());
            }
        }

        groups.clear();
        groups.putAll(shifted);

        // Remove the desired tab
        tabLocal.remove(selectedIndex);

        // The titles are the group indexes, and every index above the removed one has just shifted down, so the
        // labels have to be rewritten. Without this, removing group 1 of three leaves tabs titled 0 and 2.
        renumberGroupTabs();
        updateFlexiblePicksMaximum();

        // Update the progress panel
        parent.updateTxtProgress();
    }

    /**
     * Rewrites every group tab's title so the numbers run consecutively from zero again.
     *
     * <p>The numbers are the group indexes used as keys in the saved file, so they are not cosmetic.</p>
     *
     * @since 0.50.11
     */
    private void renumberGroupTabs() {
        for (int groupIndex = 0; groupIndex < getTabCount(); groupIndex++) {
            tabLocal.setTitleAt(groupIndex, buildGroupTabTitle(groupIndex));
        }
    }

    /**
     * Returns the title shown on a group tab.
     *
     * <p>Fixed XP and Exclusions have exactly one group, so they show no number.</p>
     *
     * @param groupIndex the group's index
     *
     * @return the tab title, which may be empty
     *
     * @since 0.50.11
     */
    private String buildGroupTabTitle(int groupIndex) {
        return switch (tabType) {
            case FIXED_XP, EXCLUSIONS -> "";
            case FLEXIBLE_XP, REQUIREMENTS -> "<html><b>" + groupIndex + "</b></html>";
        };
    }

    /**
     * Caps the "how many groups may the player pick?" spinner at the number of groups that exist.
     *
     * <p>The spinner used to accept up to 100 regardless, and {@link #getPickCount()} then quietly reduced the
     * number on save. That made the saved value disagree with the number on screen, and made
     * {@code TOO_MANY_FLEXIBLE_PICKS} impossible to produce from the wizard.</p>
     *
     * <p>Only the Flexible XP tab shows the spinner, so the other tabs leave the model alone. Changing a
     * {@link SpinnerNumberModel}'s maximum fires its change listener, which re-renders the wizard's progress panel,
     * and there is no reason to do that for a spinner nobody can see.</p>
     *
     * @since 0.50.11
     */
    private void updateFlexiblePicksMaximum() {
        if (tabType != LifePathBuilderTabType.FLEXIBLE_XP) {
            return;
        }

        SpinnerNumberModel model = (SpinnerNumberModel) spnFlexibleXPPicks.getModel();
        int groupCount = getTabCount();

        model.setMaximum(groupCount);

        if ((int) spnFlexibleXPPicks.getValue() > groupCount) {
            spnFlexibleXPPicks.setValue(groupCount);
        }
    }

    private void duplicateGroup() {
        int selectedIndex = tabLocal.getSelectedIndex();
        if (selectedIndex < 0) {
            return; // nothing selected, do nothing
        }

        addTab();

        int newIndex = getTabCount() - 1;

        // groupFor rather than a bare get: a group loaded from a hand-edited file need not be present, and copy()
        // deep-copies the collections so editing the duplicate cannot reach back into the original.
        groups.put(newIndex, groupFor(selectedIndex).copy());

        JPanel pnlNewTab = (JPanel) tabLocal.getComponentAt(newIndex);
        JPanel pnlMain = (JPanel) pnlNewTab.getComponent(1);
        JEditorPane editorProgress = findEditorPaneByName(pnlMain, "editorProgress");
        if (editorProgress != null) {
            editorProgress.setText(buildIndividualProgressText(selectedIndex).toString());
        } else {
            LOGGER.warn("Could not find editorProgress in duplicateGroup");
        }

        parent.updateTxtProgress();

        tabLocal.setSelectedIndex(newIndex);
    }

    JEditorPane findEditorPaneByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JEditorPane && name.equals(component.getName())) {
                return (JEditorPane) component;
            } else if (component instanceof Container) {
                JEditorPane result = findEditorPaneByName((Container) component, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    void addTab() {
        final boolean includeSupplementaryButtons = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                                          tabType == LifePathBuilderTabType.EXCLUSIONS;

        int index = getTabCount();

        // Create the panel to be used in the tab
        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // One object per group, created empty. Every kind of entry a group can hold starts empty inside it, and Edge
        // and the "any attribute" value start unset rather than at their sentinel, so a group nobody touches writes
        // nothing to the saved file.
        groupFor(index);

        // Attributes


        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits

        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        String titleAddNaturalAptitude = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addNaturalAptitude.label");
        String tooltipAddNaturalAptitude = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addNaturalAptitude.tooltip");
        RoundedJButton btnAddNaturalAptitude = new RoundedJButton(titleAddNaturalAptitude);
        btnAddNaturalAptitude.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddNaturalAptitude)
        );

        // Skills

        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        if (tabType == LifePathBuilderTabType.FIXED_XP || tabType == LifePathBuilderTabType.FLEXIBLE_XP) {
            buttonsPanel.add(btnAddNaturalAptitude);
        }

        // SPAs

        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions

        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddFaction)
        );
        btnAddFaction.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddFaction);

        // Systems

        String titleAddSystem = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addSystem.label");
        String tooltipAddSystems = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addSystem.tooltip");
        RoundedJButton btnAddSystem = new RoundedJButton(titleAddSystem);
        btnAddSystem.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddSystems)
        );
        btnAddSystem.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddSystem);

        // Life Paths

        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddLifePath)
        );
        btnAddLifePath.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddLifePath);

        // Categories

        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddCategory)
        );
        btnAddCategory.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog." + tabName + ".tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane editorProgress = new JEditorPane();
        editorProgress.setName("editorProgress");
        editorProgress.setContentType("text/html");
        editorProgress.setEditable(false);
        String progressText = buildIndividualProgressText(gameYear).toString();
        editorProgress.setText(progressText);

        FastJScrollPane scrollMain = new FastJScrollPane(editorProgress);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setBorder(null);

        pnlDisplay.add(scrollMain, BorderLayout.CENTER);

        // Add panels and then add Tab
        groupPanel.add(buttonsPanel, BorderLayout.NORTH);
        groupPanel.add(pnlDisplay, BorderLayout.CENTER);

        tabLocal.addTab(buildGroupTabTitle(index), groupPanel);
        updateFlexiblePicksMaximum();

        // Action Listeners
        // No hide and show around any of these: the pickers are owned by the wizard, so they cannot end up behind
        // it, and nesting a modal event loop inside the wizard's own was the usual cause of that report.
        btnAddAttribute.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathAttributePicker picker = new LifePathAttributePicker(parent,
                  group.getAttributes(),
                  group.getFlexibleAttribute(),
                  group.getEdge(),
                  tabType,
                  currentIndex);
            group.setAttributes(picker.getSelectedAttributeScores());
            group.setEdge(picker.getEdge());
            group.setFlexibleAttribute(picker.getFlexibleAttribute());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddTrait.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathTraitPicker picker = new LifePathTraitPicker(parent,
                  group.getTraits(), tabType, currentIndex);
            group.setTraits(picker.getSelectedTraitScores());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddSkill.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathSkillPicker picker = new LifePathSkillPicker(parent,
                  group.getSkills(), group.getMetaSkills(), tabType, currentIndex);
            group.setSkills(picker.getSelectedSkillLevels());
            group.setMetaSkills(picker.getSelectedMetaSkillLevels());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddNaturalAptitude.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathSkillPicker picker = new LifePathSkillPicker(parent,
                  group.getNaturalAptitudes(),
                  group.getNaturalAptitudesMetaSkills(),
                  tabType,
                  currentIndex);
            group.setNaturalAptitudes(picker.getSelectedSkillLevels());
            group.setNaturalAptitudesMetaSkills(picker.getSelectedMetaSkillLevels());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddSPA.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathSPAPicker picker = new LifePathSPAPicker(parent,
                  group.getAbilities(), allAbilityInfo, tabType, currentIndex);
            group.setAbilities(picker.getSelectedAbilities());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddFaction.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathFactionPicker picker = new LifePathFactionPicker(parent,
                  group.getFactions(), gameYear, tabType, currentIndex);
            group.setFactions(picker.getSelectedFactions());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddSystem.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathSystemPicker picker = new LifePathSystemPicker(parent,
                  group.getSystems(), today, tabType, currentIndex);
            group.setSystems(picker.getSelectedSystems());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddLifePath.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathLifePathPicker picker = new LifePathLifePathPicker(parent,
                  group.getLifePaths(),
                  lifePathLibrary,
                  parent.getLifePathId(),
                  tabType,
                  currentIndex);
            group.setLifePaths(picker.getSelectedLifePaths());
            standardizedActions(currentIndex, editorProgress);
        });
        btnAddCategory.addActionListener(actionEvent -> {
            int currentIndex = tabLocal.getSelectedIndex();
            LifePathGroup group = groupFor(currentIndex);

            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(parent,
                  group.getCategories(), tabType, currentIndex);
            group.setCategories(picker.getSelectedCategoryCounts());
            standardizedActions(currentIndex, editorProgress);
        });
    }

    /**
     * Returns the value that means "nothing selected" for an attribute row on the given tab.
     *
     * <p>A requirement of the lowest possible score asks for nothing, and an exclusion at the highest possible score
     * bans nothing, so each tab treats a different end of the range as empty. The XP tabs treat zero as empty.</p>
     *
     * @param tabType the tab the row belongs to
     * @param isEdge  {@code true} for the Edge row, which has its own bounds
     *
     * @return the value that represents an unset row
     *
     * @since 0.50.11
     */
    static int getDefaultAttributeValue(LifePathBuilderTabType tabType, boolean isEdge) {
        return switch (tabType) {
            case REQUIREMENTS -> getAttributeMinimumValue(tabType, isEdge);
            case EXCLUSIONS -> getAttributeMaximumValue(tabType, isEdge);
            case FIXED_XP, FLEXIBLE_XP -> 0;
        };
    }

    /**
     * Returns the highest value an attribute row will offer on the given tab.
     *
     * @param tabType the tab the row belongs to
     * @param isEdge  {@code true} for the Edge row, which has its own ceiling
     *
     * @return the highest selectable value
     *
     * @since 0.50.11
     */
    static int getAttributeMaximumValue(LifePathBuilderTabType tabType, boolean isEdge) {
        return switch (tabType) {
            case FIXED_XP, FLEXIBLE_XP -> ATTRIBUTE_SPINNER_HIGHEST_VALUE;
            case REQUIREMENTS, EXCLUSIONS -> isEdge ? MAXIMUM_EDGE_SCORE : MAXIMUM_ATTRIBUTE_SCORE;
        };
    }

    /**
     * Returns the lowest value an attribute row will offer on the given tab.
     *
     * @param tabType the tab the row belongs to
     * @param isEdge  {@code true} for the Edge row, which can legitimately sit at zero
     *
     * @return the lowest selectable value
     *
     * @since 0.50.11
     */
    static int getAttributeMinimumValue(LifePathBuilderTabType tabType, boolean isEdge) {
        return switch (tabType) {
            case FIXED_XP, FLEXIBLE_XP -> ATTRIBUTE_SPINNER_LOWEST_VALUE;
            case REQUIREMENTS, EXCLUSIONS -> isEdge ? MINIMUM_EDGE_SCORE : MINIMUM_ATTRIBUTE_SCORE;
        };
    }

    private void standardizedActions(int index, JEditorPane newText) {
        List<String> textArray = buildProgressText();
        newText.setText(textArray.get(index));
        parent.updateTxtProgress();
    }

    public List<String> buildProgressText() {
        List<String> progressText = new ArrayList<>();

        // Indexed from zero rather than over a map's key set: callers index this list by group index, and a
        // HashMap only happens to hand back small Integer keys in order.
        for (int groupIndex = 0; groupIndex < getTabCount(); groupIndex++) {
            progressText.add(buildIndividualProgressText(groupIndex).toString());
        }

        return progressText;
    }

    StringBuilder buildIndividualProgressText(int index) {
        StringBuilder individualProgressText = new StringBuilder();

        // Read without creating: building the progress text must not bring a group into existence, and an index with
        // no group simply has nothing to describe.
        LifePathGroup group = groups.getOrDefault(index, new LifePathGroup());

        boolean isXP = tabType == LifePathBuilderTabType.FIXED_XP || tabType == LifePathBuilderTabType.FLEXIBLE_XP;
        final String TEXT_LEAD_POSITIVE = isXP ? " +" : " ";
        final String TEXT_LEAD_NEGATIVE = " ";
        // "XP" and "+" are words a translator needs; the surrounding spacing is not, so it stays here.
        final String TEXT_TRAIL = isXP
              ? ' ' + getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.progress.trail.xp")
              : getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.progress.trail.level");

        // Factions
        Set<String> workingFactions = group.getFactions();
        if (workingFactions != null && !workingFactions.isEmpty()) {
            int workingIndex = 0;
            int total = workingFactions.size();
            for (String factionCode : workingFactions) {
                Faction faction = factions.getFaction(factionCode);
                if (faction == null) {
                    LOGGER.error("Faction not found: {}", factionCode);
                    continue;
                }
                individualProgressText.append(faction.getFullName(gameYear));
                if (workingIndex != total - 1) {
                    individualProgressText.append(", ");
                }
                workingIndex++;
            }
        }

        // Systems
        Set<String> workingSystems = group.getSystems();
        if (workingSystems != null && !workingSystems.isEmpty()) {
            // Without this, a group holding both factions and systems reads "Federated SunsTerra".
            appendBreaker(individualProgressText);

            int workingIndex = 0;
            int total = workingSystems.size();
            for (String systemCode : workingSystems) {
                PlanetarySystem system = planetarySystems.getSystemById(systemCode);
                if (system == null) {
                    LOGGER.error("System not found: {}", systemCode);
                    continue;
                }
                individualProgressText.append(system.getName(today));
                if (workingIndex != total - 1) {
                    individualProgressText.append(", ");
                }
                workingIndex++;
            }
        }

        // Life Paths
        Set<UUID> workingLifePaths = group.getLifePaths();
        if (workingLifePaths != null && !workingLifePaths.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingLifePaths.size();
            for (UUID id : workingLifePaths) {
                LifePath lifePath = lifePathLibrary.get(id);
                if (lifePath == null) {
                    LOGGER.error("Life Path not found: {}", id);
                    continue;
                }
                individualProgressText.append(lifePath.name())
                      .append(" (")
                      .append(lifePath.lifeStages().stream()
                                    .map(ATOWLifeStage::getDisplayName)
                                    .collect(Collectors.joining(",")))
                      .append(")");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Categories
        Map<LifePathCategory, Integer> workingCategories = group.getCategories();
        if (workingCategories != null && !workingCategories.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingCategories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : workingCategories.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Attributes
        Map<SkillAttribute, Integer> workingAttributes = group.getAttributes();
        if (workingAttributes != null && !workingAttributes.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingAttributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : workingAttributes.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getLabel());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Integer workingEdge = group.getEdge();
        int edgeDefaultValue = getDefaultAttributeValue(tabType, true);

        if (workingEdge != null && workingEdge != edgeDefaultValue) {
            appendBreaker(individualProgressText);

            individualProgressText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.edge.label"));
            individualProgressText.append(workingEdge >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
            individualProgressText.append(workingEdge);
            individualProgressText.append(TEXT_TRAIL);
        }

        Integer workingFlexibleAttributes = group.getFlexibleAttribute();
        // Not the Edge bounds: the flexible attribute row is an ordinary attribute, and LifePathAttributePicker
        // decides "unset" against the attribute default. The two have to agree or the progress text disagrees with
        // what the picker stored.
        int flexibleDefaultValue = getDefaultAttributeValue(tabType, false);
        if (workingFlexibleAttributes != null && workingFlexibleAttributes != flexibleDefaultValue) {
            appendBreaker(individualProgressText);

            individualProgressText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.flexibleAttribute.label"));
            individualProgressText.append(workingFlexibleAttributes >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
            individualProgressText.append(workingFlexibleAttributes);
            individualProgressText.append(TEXT_TRAIL);
        }

        // Traits
        Map<ATOWTraits, Integer> workingTraits = group.getTraits();
        if (workingTraits != null && !workingTraits.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingTraits.size();
            for (Map.Entry<ATOWTraits, Integer> entry : workingTraits.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Map<String, Integer> workingNaturalAptitudes = group.getNaturalAptitudes();
        if (workingNaturalAptitudes != null && !workingNaturalAptitudes.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingNaturalAptitudes.size();
            for (Map.Entry<String, Integer> entry : workingNaturalAptitudes.entrySet()) {
                String label = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.naturalAptitude.label",
                      entry.getKey());
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Map<SkillSubType, Integer> workingNaturalAptitudesMetaSkills = group.getNaturalAptitudesMetaSkills();
        if (workingNaturalAptitudesMetaSkills != null && !workingNaturalAptitudesMetaSkills.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingNaturalAptitudesMetaSkills.size();
            for (Map.Entry<SkillSubType, Integer> entry : workingNaturalAptitudesMetaSkills.entrySet()) {
                String label = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.naturalAptitude.label",
                      entry.getKey().getDisplayName());
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Skills
        Map<String, Integer> workingSkills = group.getSkills();
        if (workingSkills != null && !workingSkills.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingSkills.size();
            for (Map.Entry<String, Integer> entry : workingSkills.entrySet()) {
                String label = entry.getKey();
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Map<SkillSubType, Integer> workingMetaSkills = group.getMetaSkills();
        if (workingMetaSkills != null && !workingMetaSkills.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingMetaSkills.size();
            for (Map.Entry<SkillSubType, Integer> entry : workingMetaSkills.entrySet()) {
                String label = entry.getKey().getDisplayName();
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // SPAs
        Map<String, Integer> workingAbilities = group.getAbilities();
        if (workingAbilities != null && !workingAbilities.isEmpty()) {
            appendBreaker(individualProgressText);

            List<String> spas = workingAbilities.keySet().stream().toList();

            for (int i = 0; i < spas.size(); i++) {
                String abilityName = spas.get(i);
                CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);
                if (abilityInfo == null) {
                    LOGGER.warn("Could not find AbilityInfo for abilityName: {}", abilityName);
                    continue;
                }

                SpecialAbility ability = abilityInfo.getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
                if (isXP) {
                    Integer value = workingAbilities.get(abilityName);
                    if (value != null) {
                        individualProgressText.append(label);
                        individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                        individualProgressText.append(value);
                        individualProgressText.append(TEXT_TRAIL);
                    } else {
                        LOGGER.warn("Could not find value for abilityName in working abilities: {}", abilityName);
                    }
                } else {
                    individualProgressText.append(label);
                }

                if (i != spas.size() - 1) {
                    individualProgressText.append(", ");
                }
            }
        }
        return individualProgressText;
    }

    private void appendBreaker(StringBuilder progressText) {
        final boolean isFixedXP = tabType == LifePathBuilderTabType.FIXED_XP;
        final String BREAKER = isFixedXP ? "&#9654; " : ", ";

        if (isFixedXP) {
            if (!progressText.isEmpty()) {
                progressText.append("<br>");
            }

            progressText.append(BREAKER);
            return;
        }

        if (!progressText.isEmpty()) {
            progressText.append(BREAKER);
        }
    }

    public void resetTab() {
        tabLocal.removeAll();

        groups.clear();
        spnFlexibleXPPicks.setValue(0);

        parent.updateTxtProgress();
    }
}
