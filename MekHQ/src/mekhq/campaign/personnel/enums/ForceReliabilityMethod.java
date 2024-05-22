/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum ForceReliabilityMethod {
    EQUIPMENT("ForceReliabilityMethod.EQUIPMENT.text", "ForceReliabilityMethod.EQUIPMENT.toolTipText"),
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

    public boolean isEquipment() {
        return this == EQUIPMENT;
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
