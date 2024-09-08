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
import mekhq.campaign.universe.generators.battleMechWeightClassGenerators.*;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekWeightClassGenerationMethod {
    //region Enum Declarations
    AGAINST_THE_BOT("BattleMechWeightClassGenerationMethod.AGAINST_THE_BOT.text", "BattleMechWeightClassGenerationMethod.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("BattleMechWeightClassGenerationMethod.WINDCHILD.text", "BattleMechWeightClassGenerationMethod.WINDCHILD.toolTipText"),
    WINDCHILD_LIGHT("BattleMechWeightClassGenerationMethod.WINDCHILD_LIGHT.text", "BattleMechWeightClassGenerationMethod.WINDCHILD_LIGHT.toolTipText"),
    WINDCHILD_MEDIUM("BattleMechWeightClassGenerationMethod.WINDCHILD_MEDIUM.text", "BattleMechWeightClassGenerationMethod.WINDCHILD_MEDIUM.toolTipText"),
    WINDCHILD_HEAVY("BattleMechWeightClassGenerationMethod.WINDCHILD_HEAVY.text", "BattleMechWeightClassGenerationMethod.WINDCHILD_HEAVY.toolTipText"),
    WINDCHILD_ASSAULT("BattleMechWeightClassGenerationMethod.WINDCHILD_ASSAULT.text", "BattleMechWeightClassGenerationMethod.WINDCHILD_ASSAULT.toolTipText"),
    LIGHT("BattleMechWeightClassGenerationMethod.LIGHT.text", "BattleMechWeightClassGenerationMethod.LIGHT.toolTipText"),
    MEDIUM("BattleMechWeightClassGenerationMethod.MEDIUM.text", "BattleMechWeightClassGenerationMethod.MEDIUM.toolTipText"),
    HEAVY("BattleMechWeightClassGenerationMethod.HEAVY.text", "BattleMechWeightClassGenerationMethod.HEAVY.toolTipText"),
    ASSAULT("BattleMechWeightClassGenerationMethod.ASSAULT.text", "BattleMechWeightClassGenerationMethod.ASSAULT.toolTipText");
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

    public AbstractBattleMechWeightClassGenerator getGenerator() {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBBattleMechWeightClassGenerator();
            case WINDCHILD_LIGHT:
                return new WindchildLightBattleMechWeightClassGenerator();
            case WINDCHILD_MEDIUM:
                return new WindchildMediumBattleMechWeightClassGenerator();
            case WINDCHILD_HEAVY:
                return new WindchildHeavyBattleMechWeightClassGenerator();
            case WINDCHILD_ASSAULT:
                return new WindchildAssaultBattleMechWeightClassGenerator();
            case LIGHT:
                return new LightBattleMechWeightClassGenerator();
            case MEDIUM:
                return new MediumBattleMechWeightClassGenerator();
            case HEAVY:
                return new HeavyBattleMechWeightClassGenerator();
            case ASSAULT:
                return new AssaultBattleMechWeightClassGenerator();
            case WINDCHILD:
            default:
                return new WindchildBattleMechWeightClassGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
