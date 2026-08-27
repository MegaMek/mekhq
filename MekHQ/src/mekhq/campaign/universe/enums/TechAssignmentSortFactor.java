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
package mekhq.campaign.universe.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

/**
 * Factor by which {@code SupportPersonnelAssigner} sorts units when picking which Unit gets the
 * next-best Tech. Three slots ({@code primary} / {@code secondary} / {@code tertiary}) each carry
 * one of these values; the assignment comparator chains them in slot order.
 *
 * <p>{@link #NONE} disables that slot, so a user who only cares about rank can leave the secondary
 * and tertiary slots empty without forcing a fallback.</p>
 */
public enum TechAssignmentSortFactor {
    NONE("TechAssignmentSortFactor.NONE.text",
          "TechAssignmentSortFactor.NONE.toolTipText"),
    PILOT_RANK("TechAssignmentSortFactor.PILOT_RANK.text",
          "TechAssignmentSortFactor.PILOT_RANK.toolTipText"),
    UNIT_WEIGHT("TechAssignmentSortFactor.UNIT_WEIGHT.text",
          "TechAssignmentSortFactor.UNIT_WEIGHT.toolTipText"),
    PILOT_SKILL("TechAssignmentSortFactor.PILOT_SKILL.text",
          "TechAssignmentSortFactor.PILOT_SKILL.toolTipText");

    private final String displayName;
    private final String toolTipText;

    TechAssignmentSortFactor(final String displayNameKey, final String toolTipKey) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
              MekHQ.getMHQOptions().getLocale());
        this.displayName = resources.getString(displayNameKey);
        this.toolTipText = resources.getString(toolTipKey);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isNone() {
        return this == NONE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
