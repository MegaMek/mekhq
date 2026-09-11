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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
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
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author say how many Life Paths of a given category a character must, or must not, already have.
 *
 * <p>There are enough categories that they are split alphabetically across tabs. {@link LifePathCategory#NONE} is not
 * offered: it means "belongs to no category", so counting it says nothing a player could satisfy.</p>
 *
 * @since 0.50.11
 */
class LifePathCategoryCountPicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathCategoryCountPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(600);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(450);

    /** Fewest Life Paths of a category a group can call for. This is also the "no requirement" value. */
    private static final int MINIMUM_CATEGORY_COUNT = 0;

    /** Most Life Paths of a category a group can call for. */
    private static final int MAXIMUM_CATEGORY_COUNT = 10;

    /** How many alphabetical tabs the categories are spread across. */
    private static final int TAB_COUNT = 3;

    /** How many columns of rows each tab holds. */
    private static final int COLUMN_COUNT = 3;

    private final LifePathBuilderTabType tabType;
    private final Map<LifePathCategory, Integer> storedCategoryCounts;
    private Map<LifePathCategory, Integer> selectedCategoryCounts;

    /**
     * Returns the category counts the author settled on.
     *
     * @return the selected categories and their counts
     *
     * @since 0.50.11
     */
    Map<LifePathCategory, Integer> getSelectedCategoryCounts() {
        return selectedCategoryCounts;
    }

    /**
     * Opens the picker.
     *
     * @param owner                  the wizard this picker belongs to
     * @param selectedCategoryCounts the counts already set on this group
     * @param tabType                the section being edited
     * @param groupIndex             the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathCategoryCountPicker(@Nullable Window owner, Map<LifePathCategory, Integer> selectedCategoryCounts,
          LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathCategoryCountPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;

        // Defensive copies to avoid external modification
        this.selectedCategoryCounts = new HashMap<>(selectedCategoryCounts);
        this.storedCategoryCounts = new HashMap<>(selectedCategoryCounts);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(createRoundedLineBorder(getPickerText("options.label")));

        List<LifePathCategory> allCategories = new ArrayList<>();
        for (LifePathCategory category : LifePathCategory.values()) {
            // NONE means "no category", so there is nothing to count.
            if (category != LifePathCategory.NONE) {
                allCategories.add(category);
            }
        }
        allCategories.sort(Comparator.comparing(LifePathCategory::getDisplayName));

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        // One list per tab, sized by TAB_COUNT. This used to be five separately named lists for three tabs, two of
        // which could never be filled.
        for (List<LifePathCategory> tabCategories : splitIntoTabs(allCategories)) {
            if (!tabCategories.isEmpty()) {
                buildTab(tabCategories, optionPane);
            }
        }

        pnlOptions.add(optionPane);

        return pnlOptions;
    }

    /**
     * Splits the categories into one contiguous alphabetical block per tab.
     *
     * @param allCategories every category, already sorted by display name
     *
     * @return one list per tab
     *
     * @since 0.50.11
     */
    private static List<List<LifePathCategory>> splitIntoTabs(List<LifePathCategory> allCategories) {
        List<List<LifePathCategory>> tabs = new ArrayList<>();
        int perTab = (int) Math.ceil(allCategories.size() / (double) TAB_COUNT);

        for (int tabIndex = 0; tabIndex < TAB_COUNT; tabIndex++) {
            int firstEntry = Math.min(tabIndex * perTab, allCategories.size());
            int lastEntry = Math.min(firstEntry + perTab, allCategories.size());
            tabs.add(new ArrayList<>(allCategories.subList(firstEntry, lastEntry)));
        }

        return tabs;
    }

    /**
     * Adds one alphabetical tab, titled with the range of initials it covers.
     *
     * @param categories the categories on this tab
     * @param optionPane the tabbed pane to add to
     *
     * @since 0.50.11
     */
    private void buildTab(List<LifePathCategory> categories, EnhancedTabbedPane optionPane) {
        String firstName = categories.getFirst().getDisplayName();
        String lastName = categories.getLast().getDisplayName();

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.options.tab", firstLetter,
              lastLetter), getCategoryOptions(categories));
    }

    /**
     * Builds the rows for one tab's categories.
     *
     * @param categories the categories to build rows for
     *
     * @return the scrollable panel of rows
     *
     * @since 0.50.11
     */
    private FastJScrollPane getCategoryOptions(List<LifePathCategory> categories) {
        JPanel pnlCategories = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        // An exclusion is "must not already have this many", so its empty value is the top of the range; a
        // requirement is "must already have at least this many", so its empty value is the bottom.
        int keyValue = (tabType == LifePathBuilderTabType.EXCLUSIONS)
              ? MAXIMUM_CATEGORY_COUNT
              : MINIMUM_CATEGORY_COUNT;

        for (int categoryIndex = 0; categoryIndex < categories.size(); categoryIndex++) {
            LifePathCategory category = categories.get(categoryIndex);
            String label = category.getDisplayName();
            String description = category.getDescription();

            // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
            // SpinnerNumberModel throws when its initial value is out of range.
            int storedValue = selectedCategoryCounts.getOrDefault(category, keyValue);
            int startingValue = clampSpinnerValue(storedValue, MINIMUM_CATEGORY_COUNT, MAXIMUM_CATEGORY_COUNT, label);

            JLabel lblCategory = new JLabel(label);
            JSpinner spnCategoryCount = new JSpinner(new SpinnerNumberModel(startingValue, MINIMUM_CATEGORY_COUNT,
                  MAXIMUM_CATEGORY_COUNT, 1));

            spnCategoryCount.addChangeListener(changeEvent -> {
                int value = (int) spnCategoryCount.getValue();
                // Deliberately not compared against the value the spinner started at. Changing a spinner and then
                // changing it back must write the original number, otherwise the map keeps the stale one.
                if (value == keyValue) {
                    selectedCategoryCounts.remove(category);
                } else {
                    selectedCategoryCounts.put(category, value);
                }
            });

            lblCategory.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );
            spnCategoryCount.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            constraints.gridx = categoryIndex % COLUMN_COUNT;
            constraints.gridy = categoryIndex / COLUMN_COUNT;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblCategory);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnCategoryCount);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlCategories.add(pnlRows, constraints);
        }

        FastJScrollPane scrollCategories = new FastJScrollPane(pnlCategories);
        scrollCategories.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollCategories.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollCategories.setBorder(null);

        return scrollCategories;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedCategoryCounts = new HashMap<>(storedCategoryCounts);
    }

    @Override
    protected void clearSelection() {
        selectedCategoryCounts.clear();
        rebuildOptions();
    }
}
