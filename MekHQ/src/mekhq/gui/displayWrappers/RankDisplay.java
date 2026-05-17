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
package mekhq.gui.displayWrappers;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;

/**
 * @param rankNumeric region Variable Declarations
 */
public record RankDisplay(int rankNumeric, String displayName) {
    //endregion Variable Declarations

    /**
     * This creates a list of all valid rank displays, which can then be added to a checkbox or used to create menu
     * items
     *
     * @param rankSystem        the rank system to get all valid rank display from
     * @param initialProfession the initial profession for the ranks
     *
     * @return a list of all valid rank displays
     */
    public static List<RankDisplay> getRankDisplaysForSystem(final RankSystem rankSystem,
          final Profession initialProfession) {
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
    @Nonnull
    public String toString() {
        return displayName;
    }
}
