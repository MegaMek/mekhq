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
