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
import static mekhq.campaign.personnel.ATOWTraits.TRAIT_MODIFICATION_COST;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathPickerUtilities.clampSpinnerValue;

import java.awt.Component;
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
import javax.swing.SpinnerNumberModel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author set trait levels a Life Path requires or excludes, or trait XP it awards.
 *
 * <p>What a number on this picker means depends on the section. On Requirements and Exclusions it is a trait level,
 * so the spinner offers that trait's own range. On the two XP tabs it is an XP amount, so the range is a flat plus or
 * minus and the spinner steps one whole trait level at a time.</p>
 *
 * @since 0.50.11
 */
class LifePathTraitPicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathTraitPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(400);

    /** Largest XP amount a single trait row can award or charge in the Fixed XP and Flexible XP tabs. */
    private static final int MAXIMUM_TRAIT_XP = 1000;

    private final LifePathBuilderTabType tabType;
    private final Map<ATOWTraits, Integer> storedTraitScores;
    private Map<ATOWTraits, Integer> selectedTraitScores;

    /**
     * The range and step one trait row offers.
     *
     * @param minimum lowest selectable value
     * @param maximum highest selectable value
     * @param step    amount one spinner click moves the value
     *
     * @since 0.50.11
     */
    private record TraitSpinnerBounds(int minimum, int maximum, int step) {}

    /**
     * Returns the trait values the author settled on.
     *
     * @return the selected traits and their values
     *
     * @since 0.50.11
     */
    Map<ATOWTraits, Integer> getSelectedTraitScores() {
        return selectedTraitScores;
    }

    /**
     * Opens the picker.
     *
     * @param owner               the wizard this picker belongs to
     * @param selectedTraitScores the traits already set on this group
     * @param tabType             the section being edited
     * @param groupIndex          the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathTraitPicker(@Nullable Window owner, Map<ATOWTraits, Integer> selectedTraitScores,
          LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathTraitPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;

        // Defensive copies to avoid external modification
        this.selectedTraitScores = new HashMap<>(selectedTraitScores);
        this.storedTraitScores = new HashMap<>(selectedTraitScores);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(createRoundedLineBorder(getPickerText("options.label")));

        // Sorted by display name, like every other picker. Enum order put the ORIGIN_ traits in the middle of the
        // list for no reason an author could see.
        List<ATOWTraits> traits = new ArrayList<>(List.of(ATOWTraits.values()));
        traits.sort(Comparator.comparing(ATOWTraits::getDisplayName));

        for (ATOWTraits trait : traits) {
            pnlOptions.add(buildTraitRow(trait));
        }

        return pnlOptions;
    }

    /**
     * Builds the label and spinner for one trait.
     *
     * @param trait the trait to build a row for
     *
     * @return the row
     *
     * @since 0.50.11
     */
    private JPanel buildTraitRow(ATOWTraits trait) {
        String label = trait.getDisplayName();
        String tooltip = trait.getDescription();

        // Requirements and exclusions are expressed as trait levels, so they use the trait's own bounds. The XP tabs
        // hold an XP amount instead, which is why they get the flat plus or minus range and step in whole trait
        // levels.
        TraitSpinnerBounds bounds = switch (tabType) {
            case FIXED_XP, FLEXIBLE_XP -> new TraitSpinnerBounds(-MAXIMUM_TRAIT_XP,
                  MAXIMUM_TRAIT_XP,
                  TRAIT_MODIFICATION_COST);
            case REQUIREMENTS, EXCLUSIONS -> new TraitSpinnerBounds(trait.getMinimum(), trait.getMaximum(), 1);
        };

        int keyValue = switch (tabType) {
            case REQUIREMENTS -> bounds.minimum();
            case EXCLUSIONS -> bounds.maximum();
            case FIXED_XP, FLEXIBLE_XP -> 0;
        };

        // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
        // SpinnerNumberModel throws when its initial value is out of range.
        int storedValue = selectedTraitScores.getOrDefault(trait, keyValue);
        int startingValue = clampSpinnerValue(storedValue, bounds.minimum(), bounds.maximum(), label);

        JLabel lblTrait = new JLabel(label);
        JSpinner spnTraitScore = new JSpinner(new SpinnerNumberModel(startingValue, bounds.minimum(),
              bounds.maximum(), bounds.step()));

        spnTraitScore.addChangeListener(changeEvent -> {
            int value = (int) spnTraitScore.getValue();
            // Deliberately not compared against the value the spinner started at. Changing a spinner and then
            // changing it back must write the original number, otherwise the map keeps the stale one.
            if (value == keyValue) {
                selectedTraitScores.remove(trait);
            } else {
                selectedTraitScores.put(trait, value);
            }
        });
        lblTrait.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltip));
        spnTraitScore.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltip));

        JPanel pnlRows = new JPanel();
        pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
        pnlRows.add(lblTrait);
        pnlRows.add(Box.createHorizontalStrut(PADDING));
        pnlRows.add(spnTraitScore);
        pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

        return pnlRows;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedTraitScores = new HashMap<>(storedTraitScores);
    }

    @Override
    protected void clearSelection() {
        selectedTraitScores.clear();
        rebuildOptions();
    }
}
