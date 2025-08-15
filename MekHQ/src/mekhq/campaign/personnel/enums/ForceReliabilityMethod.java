/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum ForceReliabilityMethod {
    UNIT_RATING("ForceReliabilityMethod.UNIT_RATING.text", "ForceReliabilityMethod.UNIT_RATING.toolTipText"),
    LOYALTY("ForceReliabilityMethod.LOYALTY.text", "ForceReliabilityMethod.LOYALTY.toolTipText"),
    OVERRIDE_A("ForceReliabilityMethod.OVERRIDE_A.text", "ForceReliabilityMethod.OVERRIDE_A.toolTipText"),
    OVERRIDE_B("ForceReliabilityMethod.OVERRIDE_B.text", "ForceReliabilityMethod.OVERRIDE_B.toolTipText"),
    OVERRIDE_C("ForceReliabilityMethod.OVERRIDE_C.text", "ForceReliabilityMethod.OVERRIDE_C.toolTipText"),
    OVERRIDE_D("ForceReliabilityMethod.OVERRIDE_D.text", "ForceReliabilityMethod.OVERRIDE_D.toolTipText"),
    OVERRIDE_F("ForceReliabilityMethod.OVERRIDE_F.text", "ForceReliabilityMethod.OVERRIDE_F.toolTipText");

    private final String name;
    private final String toolTipText;

    ForceReliabilityMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isUnitRating() {
        return this == UNIT_RATING;
    }

    public boolean isLoyalty() {
        return this == LOYALTY;
    }

    public boolean isOverrideA() {
        return this == OVERRIDE_A;
    }

    public boolean isOverrideB() {
        return this == OVERRIDE_B;
    }

    public boolean isOverrideC() {
        return this == OVERRIDE_C;
    }

    public boolean isOverrideD() {
        return this == OVERRIDE_D;
    }

    public boolean isOverrideF() {
        return this == OVERRIDE_F;
    }

    @Override
    public String toString() {
        return name;
    }
}
