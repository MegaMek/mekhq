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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author choose which categories a Life Path belongs to.
 *
 * <p>{@link LifePathCategory#NONE} is not offered. It means "belongs to no category", which is what leaving every box
 * unticked already says, and ticking it alongside a real category says both things at once.</p>
 *
 * @since 0.50.11
 */
class LifePathCategorySingletonPicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathCategorySingletonPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(500);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(625);

    /** How many columns the category checkboxes are laid out in. */
    private static final int COLUMN_COUNT = 3;

    private final Set<LifePathCategory> storedCategories;
    private Set<LifePathCategory> selectedCategories;

    /**
     * Returns the categories the author settled on.
     *
     * @return the selected categories
     *
     * @since 0.50.11
     */
    Set<LifePathCategory> getSelectedCategories() {
        return selectedCategories;
    }

    /**
     * Opens the picker.
     *
     * @param owner              the wizard this picker belongs to
     * @param selectedCategories the categories already chosen
     *
     * @since 0.50.11
     */
    LifePathCategorySingletonPicker(@Nullable Window owner, Set<LifePathCategory> selectedCategories) {
        super(owner, RESOURCE_BUNDLE, "LifePathCategorySingletonPicker", null, null, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        // Defensive copies to avoid external modification
        this.selectedCategories = new HashSet<>(selectedCategories);
        this.storedCategories = new HashSet<>(selectedCategories);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(RoundedLineBorder.createRoundedLineBorder(getPickerText("options.label")));

        List<LifePathCategory> categories = new ArrayList<>();
        for (LifePathCategory category : LifePathCategory.values()) {
            // NONE says "no category", which an empty selection already says.
            if (category != LifePathCategory.NONE) {
                categories.add(category);
            }
        }
        categories.sort(Comparator.comparing(LifePathCategory::getDisplayName));

        JPanel columnsPanel = new JPanel(new GridBagLayout());

        // Split into contiguous blocks so each column reads alphabetically top to bottom. Taking every third entry
        // instead runs the alphabet across the rows while the columns look shuffled.
        int categoriesPerColumn = (int) Math.ceil(categories.size() / (double) COLUMN_COUNT);

        for (int columnIndex = 0; columnIndex < COLUMN_COUNT; columnIndex++) {
            int firstEntry = Math.min(columnIndex * categoriesPerColumn, categories.size());
            int lastEntry = Math.min(firstEntry + categoriesPerColumn, categories.size());

            JPanel columnPanel = new JPanel();
            columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));

            for (LifePathCategory category : categories.subList(firstEntry, lastEntry)) {
                JCheckBox chkCategory = new JCheckBox(category.getDisplayName());
                chkCategory.setSelected(selectedCategories.contains(category));

                chkCategory.addActionListener(actionEvent -> {
                    if (chkCategory.isSelected()) {
                        selectedCategories.add(category);
                    } else {
                        selectedCategories.remove(category);
                    }
                });
                chkCategory.addMouseListener(
                      TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, category.getDescription())
                );

                columnPanel.add(chkCategory);
            }

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = columnIndex;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTH;
            constraints.fill = GridBagConstraints.VERTICAL;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            columnsPanel.add(columnPanel, constraints);
        }

        pnlOptions.add(columnsPanel);

        return pnlOptions;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedCategories = new HashSet<>(storedCategories);
    }

    @Override
    protected void clearSelection() {
        selectedCategories.clear();
        rebuildOptions();
    }
}
