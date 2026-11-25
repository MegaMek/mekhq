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
package mekhq.campaign.mission.contract.utilities;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;

/**
 * Works out which named war, if any, a contract is being fought as part of.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractWar {
    private ContractWar() {
    }

    /**
     * The war the contract's two sides are fighting on the given date, as recorded in the faction diplomacy data.
     *
     * <p>The employer side is its territorial anchor faction, so landless employers (rebels, corporations) still
     * resolve to the power whose war they are part of. An enemy fielded by a covert sponsor fights that sponsor's war,
     * so the sponsor is checked in place of the enemy when there is one.</p>
     *
     * @param contract the contract
     * @param date     the date to check
     *
     * @return the war's name (empty for an unnamed war), or {@code null} when the two sides are not at war on that date
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String getCurrentWar(AbstractContract contract, LocalDate date) {
        Faction employerFaction = Factions.getInstance().getFaction(contract.getEmployerData().anchorFactionCode());
        Faction enemySponsor = contract.getEnemySponsorFaction();
        Faction enemyFaction = (enemySponsor == null) ? contract.getEnemyFaction() : enemySponsor;
        return RandomFactionGenerator.getInstance()
                     .getFactionHints()
                     .getCurrentWar(employerFaction, enemyFaction, date);
    }
}
