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
import mekhq.campaign.universe.generators.battleMekWeightClassGenerators.*;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekWeightClassGenerationMethod {
    //region Enum Declarations
    AGAINST_THE_BOT("BattleMekWeightClassGenerationMethod.AGAINST_THE_BOT.text",
          "BattleMekWeightClassGenerationMethod.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("BattleMekWeightClassGenerationMethod.WINDCHILD.text",
          "BattleMekWeightClassGenerationMethod.WINDCHILD.toolTipText"),
    WINDCHILD_LIGHT("BattleMekWeightClassGenerationMethod.WINDCHILD_LIGHT.text",
          "BattleMekWeightClassGenerationMethod.WINDCHILD_LIGHT.toolTipText"),
    WINDCHILD_MEDIUM("BattleMekWeightClassGenerationMethod.WINDCHILD_MEDIUM.text",
          "BattleMekWeightClassGenerationMethod.WINDCHILD_MEDIUM.toolTipText"),
    WINDCHILD_HEAVY("BattleMekWeightClassGenerationMethod.WINDCHILD_HEAVY.text",
          "BattleMekWeightClassGenerationMethod.WINDCHILD_HEAVY.toolTipText"),
    WINDCHILD_ASSAULT("BattleMekWeightClassGenerationMethod.WINDCHILD_ASSAULT.text",
          "BattleMekWeightClassGenerationMethod.WINDCHILD_ASSAULT.toolTipText"),
    LIGHT("BattleMekWeightClassGenerationMethod.LIGHT.text", "BattleMekWeightClassGenerationMethod.LIGHT.toolTipText"),
    MEDIUM("BattleMekWeightClassGenerationMethod.MEDIUM.text",
          "BattleMekWeightClassGenerationMethod.MEDIUM.toolTipText"),
    HEAVY("BattleMekWeightClassGenerationMethod.HEAVY.text", "BattleMekWeightClassGenerationMethod.HEAVY.toolTipText"),
    ASSAULT("BattleMekWeightClassGenerationMethod.ASSAULT.text",
          "BattleMekWeightClassGenerationMethod.ASSAULT.toolTipText");
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
        return switch (this) {
            case AGAINST_THE_BOT -> new AtBBattleMekWeightClassGenerator();
            case WINDCHILD_LIGHT -> new WindchildLightBattleMekWeightClassGenerator();
            case WINDCHILD_MEDIUM -> new WindchildMediumBattleMekWeightClassGenerator();
            case WINDCHILD_HEAVY -> new WindchildHeavyBattleMekWeightClassGenerator();
            case WINDCHILD_ASSAULT -> new WindchildAssaultBattleMekWeightClassGenerator();
            case LIGHT -> new LightBattleMekWeightClassGenerator();
            case MEDIUM -> new MediumBattleMekWeightClassGenerator();
            case HEAVY -> new HeavyBattleMekWeightClassGenerator();
            case ASSAULT -> new AssaultBattleMekWeightClassGenerator();
            default -> new WindchildBattleMekWeightClassGenerator();
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
