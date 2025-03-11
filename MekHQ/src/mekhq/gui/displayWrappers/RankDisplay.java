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
 */
package mekhq.gui.displayWrappers;

import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;

import java.util.ArrayList;
import java.util.List;

public class RankDisplay {
    //region Variable Declarations
    private final int rankNumeric;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public RankDisplay(final int rankNumeric, final String displayName) {
        this.rankNumeric = rankNumeric;
        this.displayName = displayName;
    }
    //endregion Constructors

    //region Getters
    public int getRankNumeric() {
        return rankNumeric;
    }
    //endregion Getters

    /**
     * This creates a list of all valid rank displays, which can then be added to a checkbox or used
     * to create menu items
     * @param rankSystem the rank system to get all valid rank display from
     * @param initialProfession the initial profession for the ranks
     * @return a list of all valid rank displays
     */
    public static List<RankDisplay> getRankDisplaysForSystem(final RankSystem rankSystem, final Profession initialProfession) {
        final List<RankDisplay> rankDisplays = new ArrayList<>();
        final Profession profession = initialProfession.getBaseProfession(rankSystem);
        for (int i = 0; i < rankSystem.getRanks().size(); i++) {
            final Rank rank = rankSystem.getRanks().get(i);
            if (!rank.isEmpty(profession)) {
                rankDisplays.add(new RankDisplay(i, rank.getName(profession.getProfessionFromBase(rankSystem, rank))));
            }
        }
        return rankDisplays;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
