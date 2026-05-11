/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;

/**
 * Applies rank values from {@link ForceDescriptor#getCo()} / {@code getXo()} {@link CrewDescriptor}s onto
 * MekHQ {@link Person}s.
 *
 * <p>Phase 1: minimal pass-through of {@code descriptor.getRank()} as the Person's rank index. Phase 3
 * will port the AtB starting-force preset's faction-aware rank rules
 * ({@code AtBCompanyGenerator.generateCommandingOfficerRank}) as a {@code factionRankPolicy(faction)}
 * lookup and translate via the campaign's rank system, so that ComStar / WoB ranks resolve correctly.</p>
 *
 * <p>This class exists as a seam now so Phase 3's tree-walk rank assignment plugs in without further
 * caller changes.</p>
 */
public final class RankAssigner {

    private static final MMLogger LOGGER = MMLogger.create(RankAssigner.class);

    private RankAssigner() {
        // utility class
    }

    /**
     * Applies the descriptor's rank value to the given Person, if both are non-null and the descriptor's
     * rank is meaningful (> 0).
     *
     * @param descriptor the source {@link CrewDescriptor}; may be {@code null}
     * @param person     the target Person; may be {@code null}
     */
    public static void apply(CrewDescriptor descriptor, Person person) {
        if (descriptor == null || person == null) {
            return;
        }
        int rank = descriptor.getRank();
        if (rank > 0) {
            // Phase 1: direct pass-through. Phase 3 will translate via Faction.getRankSystemCode().
            person.setRank(rank);
            LOGGER.info("[CompanyGen]         RankAssigner.apply set rank={} on person '{}'",
                  rank, person.getFullName());
        } else {
            LOGGER.info("[CompanyGen]         RankAssigner.apply: descriptor rank<=0, leaving default");
        }
    }
}
