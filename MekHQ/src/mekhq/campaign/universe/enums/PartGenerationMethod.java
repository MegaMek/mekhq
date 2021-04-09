/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.universe.generators.partGenerators.AbstractPartGenerator;
import mekhq.campaign.universe.generators.partGenerators.MishraPartGenerator;
import mekhq.campaign.universe.generators.partGenerators.MultiplePartGenerator;
import mekhq.campaign.universe.generators.partGenerators.WindchildPartGenerator;

import java.util.ResourceBundle;

public enum PartGenerationMethod {
    //region Enum Declarations
    DISABLED("PartGenerationMethod.DISABLED.text", "PartGenerationMethod.DISABLED.toolTipText"),
    WINDCHILD("PartGenerationMethod.WINDCHILD.text", "PartGenerationMethod.WINDCHILD.toolTipText"),
    MISHRA("PartGenerationMethod.MISHRA.text", "PartGenerationMethod.MISHRA.toolTipText"),
    SINGLE("PartGenerationMethod.SINGLE.text", "PartGenerationMethod.SINGLE.toolTipText"),
    DOUBLE("PartGenerationMethod.DOUBLE.text", "PartGenerationMethod.DOUBLE.toolTipText"),
    TRIPLE("PartGenerationMethod.TRIPLE.text", "PartGenerationMethod.TRIPLE.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PartGenerationMethod(final String name, final String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isDisabled() {
        return this == DISABLED;
    }
    //endregion Boolean Comparisons

    public AbstractPartGenerator getGenerator() {
        switch (this) {
            case MISHRA:
                return new MishraPartGenerator();
            case SINGLE:
                return new MultiplePartGenerator(this, 1);
            case DOUBLE:
                return new MultiplePartGenerator(this, 2);
            case TRIPLE:
                return new MultiplePartGenerator(this, 3);
            case DISABLED:
                MekHQ.getLogger().error("Attempted to get generator for a disabled part generator. Returning Windchild");
            case WINDCHILD:
            default:
                return new WindchildPartGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
