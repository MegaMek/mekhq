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
package mekhq.campaign.mission.newContract.targetFinder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionBorderTracker;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.HPGRating;

/**
 * Finds mission targets for missions against ComStar, whose HPG network matters more than its (minimal) sovereign
 * territory: only its own systems and A/B-rated HPG stations are valid targets, regardless of borders.
 * <p>Used by {@link MissionTargetFinder} for the ComStar-specific tier of
 * {@link RandomFactionGenerator#getMissionTargetList(Faction, Faction, ILocation)}.</p>
 */
class ComStarMissionTargetFinder {
    private final FactionBorderTracker borderTracker;

    ComStarMissionTargetFinder(FactionBorderTracker borderTracker) {
        this.borderTracker = borderTracker;
    }

    /**
     * Finds systems within {@code radius} light years of {@code location}'s current system that are either owned by
     * ComStar or have an A- or B-rated HPG station.
     *
     * @param comStar  the ComStar faction
     * @param location the location to center the search on
     * @param radius   the search radius in light years from {@code location}'s current system; a negative radius
     *                 includes every system returned by {@link FactionBorderTracker#getSystemList()}
     * @param date     the date to check faction control and HPG rating against
     *
     * @return matching systems, or an empty list if {@code location} has no current system or none are found in range
     */
    List<PlanetarySystem> findValidSystems(Faction comStar, ILocation location, double radius, LocalDate date) {
        PlanetarySystem origin = location.getCurrentSystem();
        if (origin == null) {
            return Collections.emptyList();
        }

        List<PlanetarySystem> validSystems = new ArrayList<>();
        for (PlanetarySystem system : borderTracker.systemsNear(origin, radius)) {
            boolean ownedByComStar = system.getFactionSet(date).contains(comStar);
            HPGRating hpg = system.getHPG(date);
            boolean highRatedHPG = (hpg != null) && (hpg.compareTo(HPGRating.B) >= 0);
            if (ownedByComStar || highRatedHPG) {
                validSystems.add(system);
            }
        }
        return validSystems;
    }
}
