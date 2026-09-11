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

import java.awt.Window;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author choose which life stages a Life Path belongs to.
 *
 * <p>A Life Path with no life stage is never offered to a player and cannot be listed by the Life Path picker, so
 * Confirm stays disabled until at least one stage is ticked.</p>
 *
 * @since 0.50.11
 */
class LifePathStagePicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathStagePicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(375);

    private final Set<ATOWLifeStage> storedLifeStages;
    private Set<ATOWLifeStage> selectedLifeStages;

    /**
     * Returns the life stages the author settled on.
     *
     * @return the selected life stages
     *
     * @since 0.50.11
     */
    Set<ATOWLifeStage> getSelectedLifeStages() {
        return selectedLifeStages;
    }

    /**
     * Opens the picker.
     *
     * @param owner             the wizard this picker belongs to
     * @param selectedLifeStages the life stages already chosen
     *
     * @since 0.50.11
     */
    LifePathStagePicker(@Nullable Window owner, Set<ATOWLifeStage> selectedLifeStages) {
        super(owner, RESOURCE_BUNDLE, "LifePathStagePicker", null, null, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        // Defensive copies to avoid external modification
        this.selectedLifeStages = new HashSet<>(selectedLifeStages);
        this.storedLifeStages = new HashSet<>(selectedLifeStages);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(RoundedLineBorder.createRoundedLineBorder(getPickerText("options.label")));

        // Ordered rather than declaration order: ATOWLifeStage carries an explicit order, which is the sequence a
        // character actually passes through.
        for (ATOWLifeStage lifeStage : ATOWLifeStage.getOrderedLifeStages()) {
            JCheckBox chkLifeStage = new JCheckBox(lifeStage.getDisplayName());
            chkLifeStage.setSelected(selectedLifeStages.contains(lifeStage));

            chkLifeStage.addActionListener(actionEvent -> {
                if (chkLifeStage.isSelected()) {
                    selectedLifeStages.add(lifeStage);
                } else {
                    selectedLifeStages.remove(lifeStage);
                }

                refreshConfirmEnabled();
            });
            chkLifeStage.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, lifeStage.getDescription())
            );

            pnlOptions.add(chkLifeStage);
        }

        return pnlOptions;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedLifeStages = new HashSet<>(storedLifeStages);
    }

    @Override
    protected void clearSelection() {
        selectedLifeStages.clear();
        rebuildOptions();
    }

    @Override
    protected boolean isConfirmEnabled() {
        return !selectedLifeStages.isEmpty();
    }
}
