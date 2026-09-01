/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.skills.Skills;

/**
 * Shared helpers for the standardized skill-level pickers used across MekHQ dialogs (Hire Bulk Personnel, the contract
 * editor, the Bot Force customizer, and StratCon options).
 *
 * <p>Every such picker offers the full {@link SkillLevel} range from Ultra-Green through Legendary plus a "Random"
 * entry. Rather than introduce a new enum constant, {@link SkillLevel#NONE} - which is meaningless as a generation or
 * force skill and was previously filtered out of these pickers - is reused as the "Random" sentinel and rendered with a
 * localized label. A Random selection is resolved to a concrete level via {@link Skills#rollRandomSkillLevel()} at the
 * point of use, so persisted skill fields never store the sentinel.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SkillLevelPickerUtility {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillLevelPickerUtil";

    /**
     * The skill-level options for a standardized picker: Random (rendered from {@link SkillLevel#NONE}) then
     * Ultra-Green through Legendary, matching {@link SkillLevel#values()} order.
     */
    public static final SkillLevel[] PICKER_LEVELS = SkillLevel.values();

    private SkillLevelPickerUtility() {
    }

    /**
     * @return the localized label shown for the "Random" picker entry
     */
    public static String getRandomLabel() {
        return getTextAt(RESOURCE_BUNDLE, "random.text");
    }

    /**
     * Installs a renderer on the supplied combo box so that the {@link SkillLevel#NONE} sentinel displays as the
     * localized "Random" label. All other levels render with their normal names.
     *
     * @param combo the skill-level combo box to configure
     */
    public static void applyRandomRenderer(JComboBox<SkillLevel> combo) {
        final String randomLabel = getRandomLabel();
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == SkillLevel.NONE) {
                    setText(randomLabel);
                }
                return this;
            }
        });
    }

    /**
     * Resolves a picker selection to a concrete {@link SkillLevel}. A {@code null} or {@link SkillLevel#NONE} selection
     * is the "Random" option and rolls a fresh level via {@link Skills#rollRandomSkillLevel()}; any other selection is
     * returned unchanged.
     *
     * <p>Call this once per generated character to get per-character rolls, or once at commit time to resolve a Random
     * selection to a single stored level.</p>
     *
     * @param selected the picker's selected value, or {@code null}
     *
     * @return a concrete, non-{@code NONE} {@link SkillLevel}
     */
    public static SkillLevel resolve(SkillLevel selected) {
        return (selected == null || selected == SkillLevel.NONE) ? Skills.rollRandomSkillLevel() : selected;
    }

    /**
     * @param selected the picker's selected value, or {@code null}
     *
     * @return {@code true} if the selection is the "Random" sentinel
     */
    public static boolean isRandom(SkillLevel selected) {
        return selected == null || selected == SkillLevel.NONE;
    }
}
