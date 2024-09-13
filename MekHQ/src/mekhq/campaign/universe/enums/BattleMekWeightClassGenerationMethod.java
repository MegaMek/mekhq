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
import mekhq.campaign.universe.generators.battleMekWeightClassGenerators.*;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekWeightClassGenerationMethod {
    //region Enum Declarations
    AGAINST_THE_BOT("BattleMekWeightClassGenerationMethod.AGAINST_THE_BOT.text", "BattleMekWeightClassGenerationMethod.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("BattleMekWeightClassGenerationMethod.WINDCHILD.text", "BattleMekWeightClassGenerationMethod.WINDCHILD.toolTipText"),
    WINDCHILD_LIGHT("BattleMekWeightClassGenerationMethod.WINDCHILD_LIGHT.text", "BattleMekWeightClassGenerationMethod.WINDCHILD_LIGHT.toolTipText"),
    WINDCHILD_MEDIUM("BattleMekWeightClassGenerationMethod.WINDCHILD_MEDIUM.text", "BattleMekWeightClassGenerationMethod.WINDCHILD_MEDIUM.toolTipText"),
    WINDCHILD_HEAVY("BattleMekWeightClassGenerationMethod.WINDCHILD_HEAVY.text", "BattleMekWeightClassGenerationMethod.WINDCHILD_HEAVY.toolTipText"),
    WINDCHILD_ASSAULT("BattleMekWeightClassGenerationMethod.WINDCHILD_ASSAULT.text", "BattleMekWeightClassGenerationMethod.WINDCHILD_ASSAULT.toolTipText"),
    LIGHT("BattleMekWeightClassGenerationMethod.LIGHT.text", "BattleMekWeightClassGenerationMethod.LIGHT.toolTipText"),
    MEDIUM("BattleMekWeightClassGenerationMethod.MEDIUM.text", "BattleMekWeightClassGenerationMethod.MEDIUM.toolTipText"),
    HEAVY("BattleMekWeightClassGenerationMethod.HEAVY.text", "BattleMekWeightClassGenerationMethod.HEAVY.toolTipText"),
    ASSAULT("BattleMekWeightClassGenerationMethod.ASSAULT.text", "BattleMekWeightClassGenerationMethod.ASSAULT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    BattleMekWeightClassGenerationMethod(final String name, final String toolTipText) {
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

    public boolean isWindchildLight() {
        return this == WINDCHILD_LIGHT;
    }

    public boolean isWindchildMedium() {
        return this == WINDCHILD_MEDIUM;
    }

    public boolean isWindchildHeavy() {
        return this == WINDCHILD_HEAVY;
    }

    public boolean isWindchildAssault() {
        return this == WINDCHILD_ASSAULT;
    }

    public boolean isLight() {
        return this == LIGHT;
    }

    public boolean isMedium() {
        return this == MEDIUM;
    }

    public boolean isHeavy() {
        return this == HEAVY;
    }

    public boolean isAssault() {
        return this == ASSAULT;
    }
    //endregion Boolean Comparison Methods

    public AbstractBattleMekWeightClassGenerator getGenerator() {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBBattleMekWeightClassGenerator();
            case WINDCHILD_LIGHT:
                return new WindchildLightBattleMekWeightClassGenerator();
            case WINDCHILD_MEDIUM:
                return new WindchildMediumBattleMekWeightClassGenerator();
            case WINDCHILD_HEAVY:
                return new WindchildHeavyBattleMekWeightClassGenerator();
            case WINDCHILD_ASSAULT:
                return new WindchildAssaultBattleMekWeightClassGenerator();
            case LIGHT:
                return new LightBattleMekWeightClassGenerator();
            case MEDIUM:
                return new MediumBattleMekWeightClassGenerator();
            case HEAVY:
                return new HeavyBattleMekWeightClassGenerator();
            case ASSAULT:
                return new AssaultBattleMekWeightClassGenerator();
            case WINDCHILD:
            default:
                return new WindchildBattleMekWeightClassGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
