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

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.universe.generators.partGenerators.AbstractPartGenerator;
import mekhq.campaign.universe.generators.partGenerators.MishraPartGenerator;
import mekhq.campaign.universe.generators.partGenerators.MultiplePartGenerator;
import mekhq.campaign.universe.generators.partGenerators.WindchildPartGenerator;

/**
 * @author Justin "Windchild" Bowen
 */
public enum PartGenerationMethod {
    // region Enum Declarations
    DISABLED("PartGenerationMethod.DISABLED.text", "PartGenerationMethod.DISABLED.toolTipText"),
    WINDCHILD("PartGenerationMethod.WINDCHILD.text", "PartGenerationMethod.WINDCHILD.toolTipText"),
    MISHRA("PartGenerationMethod.MISHRA.text", "PartGenerationMethod.MISHRA.toolTipText"),
    SINGLE("PartGenerationMethod.SINGLE.text", "PartGenerationMethod.SINGLE.toolTipText"),
    DOUBLE("PartGenerationMethod.DOUBLE.text", "PartGenerationMethod.DOUBLE.toolTipText"),
    TRIPLE("PartGenerationMethod.TRIPLE.text", "PartGenerationMethod.TRIPLE.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    PartGenerationMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isDisabled() {
        return this == DISABLED;
    }

    public boolean isWindchild() {
        return this == WINDCHILD;
    }

    public boolean isMishra() {
        return this == MISHRA;
    }

    public boolean isSingle() {
        return this == SINGLE;
    }

    public boolean isDouble() {
        return this == DOUBLE;
    }

    public boolean isTriple() {
        return this == TRIPLE;
    }
    // endregion Boolean Comparison Methods

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
                MMLogger.create(PartGenerationMethod.class)
                        .error("Attempted to get a generator when the part generator is Disabled. Returning Windchild");
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
