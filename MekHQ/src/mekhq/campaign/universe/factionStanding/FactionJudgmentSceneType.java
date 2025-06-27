/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

/**
 * Enum representing the various judgment scene types that can occur during a faction censure event. Each constant is
 * associated with a unique lookup name.
 *
 * <p>This enum is used to identify different narrative or functional scenes that may be presented to the player,
 * such as disbanding a unit, going rogue, or seppuku.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionJudgmentSceneType {
    /**
     * Scene type representing the disbanding of a unit or force.
     */
    DISBAND("DISBAND"),

    /**
     * Scene type where the player goes rogue.
     */
    GO_ROGUE("GO_ROGUE"),

    /**
     * Scene type where the player responded to a {@link FactionCensureEvent} by committing seppuku
     */
    SEPPUKU("SEPPUKU");

    /**
     * The unique lookup name associated with this scene type.
     */
    private final String lookUpName;

    /**
     * Constructs a {@link FactionJudgmentSceneType} with the specified lookup name.
     *
     * @param lookUp the lookup name associated with the scene type
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionJudgmentSceneType(String lookUp) {
        this.lookUpName = lookUp;
    }

    /**
     * Returns the lookup name for this scene type.
     *
     * @return the lookup name associated with the enum constant
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookUpName() {
        return lookUpName;
    }
}
