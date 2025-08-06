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

import mekhq.MekHQ;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.ABattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.AStarBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.AbstractBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.AtBBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.BBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.CBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.DBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.FBattleMekQualityGenerator;
import mekhq.campaign.universe.generators.battleMekQualityGenerators.WindchildBattleMekQualityGenerator;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekQualityGenerationMethod {
    //region Enum Declarations
    AGAINST_THE_BOT("BattleMekQualityGenerationMethod.AGAINST_THE_BOT.text",
          "BattleMekQualityGenerationMethod.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("BattleMekQualityGenerationMethod.WINDCHILD.text",
          "BattleMekQualityGenerationMethod.WINDCHILD.toolTipText"),
    F("BattleMekQualityGenerationMethod.F.text", "BattleMekQualityGenerationMethod.F.toolTipText"),
    D("BattleMekQualityGenerationMethod.D.text", "BattleMekQualityGenerationMethod.D.toolTipText"),
    C("BattleMekQualityGenerationMethod.C.text", "BattleMekQualityGenerationMethod.C.toolTipText"),
    B("BattleMekQualityGenerationMethod.B.text", "BattleMekQualityGenerationMethod.B.toolTipText"),
    A("BattleMekQualityGenerationMethod.A.text", "BattleMekQualityGenerationMethod.A.toolTipText"),
    A_STAR("BattleMekQualityGenerationMethod.A_STAR.text", "BattleMekQualityGenerationMethod.A_STAR.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    BattleMekQualityGenerationMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isAgainstTheBot() {
        return this == AGAINST_THE_BOT;
    }

    public boolean isWindchild() {
        return this == WINDCHILD;
    }

    public boolean isF() {
        return this == F;
    }

    public boolean isD() {
        return this == D;
    }

    public boolean isC() {
        return this == C;
    }

    public boolean isB() {
        return this == B;
    }

    public boolean isA() {
        return this == A;
    }

    public boolean isAStar() {
        return this == A_STAR;
    }
    //endregion Boolean Comparison Methods

    public AbstractBattleMekQualityGenerator getGenerator() {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBBattleMekQualityGenerator();
            case F:
                return new FBattleMekQualityGenerator();
            case D:
                return new DBattleMekQualityGenerator();
            case C:
                return new CBattleMekQualityGenerator();
            case B:
                return new BBattleMekQualityGenerator();
            case A:
                return new ABattleMekQualityGenerator();
            case A_STAR:
                return new AStarBattleMekQualityGenerator();
            case WINDCHILD:
            default:
                return new WindchildBattleMekQualityGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
