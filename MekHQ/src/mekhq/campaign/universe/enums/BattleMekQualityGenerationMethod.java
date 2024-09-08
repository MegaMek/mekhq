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

import mekhq.MekHQ;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.ABattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.AStarBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.AbstractBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.AtBBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.BBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.CBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.DBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.FBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.WindchildBattleMechQualityGenerator;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekQualityGenerationMethod {
    //region Enum Declarations
    AGAINST_THE_BOT("BattleMechQualityGenerationMethod.AGAINST_THE_BOT.text", "BattleMechQualityGenerationMethod.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("BattleMechQualityGenerationMethod.WINDCHILD.text", "BattleMechQualityGenerationMethod.WINDCHILD.toolTipText"),
    F("BattleMechQualityGenerationMethod.F.text", "BattleMechQualityGenerationMethod.F.toolTipText"),
    D("BattleMechQualityGenerationMethod.D.text", "BattleMechQualityGenerationMethod.D.toolTipText"),
    C("BattleMechQualityGenerationMethod.C.text", "BattleMechQualityGenerationMethod.C.toolTipText"),
    B("BattleMechQualityGenerationMethod.B.text", "BattleMechQualityGenerationMethod.B.toolTipText"),
    A("BattleMechQualityGenerationMethod.A.text", "BattleMechQualityGenerationMethod.A.toolTipText"),
    A_STAR("BattleMechQualityGenerationMethod.A_STAR.text", "BattleMechQualityGenerationMethod.A_STAR.toolTipText");
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

    public AbstractBattleMechQualityGenerator getGenerator() {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBBattleMechQualityGenerator();
            case F:
                return new FBattleMechQualityGenerator();
            case D:
                return new DBattleMechQualityGenerator();
            case C:
                return new CBattleMechQualityGenerator();
            case B:
                return new BBattleMechQualityGenerator();
            case A:
                return new ABattleMechQualityGenerator();
            case A_STAR:
                return new AStarBattleMechQualityGenerator();
            case WINDCHILD:
            default:
                return new WindchildBattleMechQualityGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
