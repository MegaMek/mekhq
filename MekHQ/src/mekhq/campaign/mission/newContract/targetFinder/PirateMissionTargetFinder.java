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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionBorderTracker;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.PlanetaryType;
import mekhq.campaign.universe.factionHints.FactionHints;

/**
 * Finds mission targets for pirates, who favor border worlds over the interior on either side of a conflict rather than
 * a normal shared-border search: as defender, a nearby empty/lawless system, then a border with a Periphery neighbor,
 * then a border with anyone; as attacker, the same border preference against the defender.
 * <p>Used by {@link MissionTargetFinder} for the pirate-specific tiers of
 * {@link RandomFactionGenerator#getMissionTargetList(Faction, Faction, ILocation)}.</p>
 */
class PirateMissionTargetFinder {
    private final FactionBorderTracker borderTracker;

    PirateMissionTargetFinder(FactionBorderTracker borderTracker) {
        this.borderTracker = borderTracker;
    }

    /**
     * Finds targets for a pirate defender: a nearby empty/lawless system first, then the attacker's own border with a
     * Periphery neighbor, then the attacker's border with anyone.
     *
     * @param attacker the attacking faction
     * @param defender the pirate defender
     * @param location the location to center the search on
     * @param radius   the search radius in light years from {@code location}'s current system
     * @param date     the date to check faction control against
     *
     * @return the preferred tier of matching systems, or an empty list if none match any tier
     */
    List<PlanetarySystem> findDefenderTargets(Faction attacker, Faction defender, ILocation location, double radius,
          LocalDate date) {
        List<PlanetarySystem> emptySystems = findEmptySystems(location, radius, date);
        if (!emptySystems.isEmpty()) {
            return emptySystems;
        }

        List<PlanetarySystem> peripheryBorder = findFactionBorderSystems(attacker, defender, location, radius, true);
        if (!peripheryBorder.isEmpty()) {
            return peripheryBorder;
        }

        return findFactionBorderSystems(attacker, defender, location, radius, false);
    }

    /**
     * Finds targets for a pirate attacker: the defender's own border with a Periphery neighbor first, then the
     * defender's border with anyone.
     *
     * @param attacker the pirate attacker
     * @param defender the defending faction
     * @param location the location to center the search on
     * @param radius   the search radius in light years from {@code location}'s current system
     *
     * @return the preferred tier of matching systems, or an empty list if neither tier matches
     */
    List<PlanetarySystem> findAttackerTargets(Faction attacker, Faction defender, ILocation location, double radius) {
        List<PlanetarySystem> peripheryBorder = findFactionBorderSystems(defender, attacker, location, radius, true);
        if (!peripheryBorder.isEmpty()) {
            return peripheryBorder;
        }

        return findFactionBorderSystems(defender, attacker, location, radius, false);
    }

    /**
     * Finds systems within {@code radius} light years of {@code location}'s current system that have no real
     * controlling faction &mdash; that is, genuinely lawless, uncontested space, as opposed to a system some real
     * faction happens to also claim. This includes systems with no faction data at all (many real, populated systems
     * have no owner tagged in the data at all, not even a placeholder) as well as ones whose only controlling "faction"
     * is an empty/placeholder faction (see {@link FactionHints#isEmptyFaction(Faction)}).
     * <p>Connector systems ({@link PlanetarySystem#isConnector()}) are synthetic jump-path waypoints with no lore
     * behind them, so they're excluded from mission targeting generally. Pirates are the one exception: a raiding band
     * hiding out at an otherwise-uncharted waypoint is plausible as long as it has a proper world to hide on, so a
     * connector system still qualifies here if its primary planet is {@link PlanetaryType#TERRESTRIAL} (as opposed to a
     * gas giant, asteroid belt, or similar place no one could actually raid).</p>
     *
     * @param location the location to center the search on
     * @param radius   the search radius in light years from {@code location}'s current system; a negative radius
     *                 includes every system returned by {@link FactionBorderTracker#getSystemList()}
     * @param date     the date to check faction control against
     *
     * @return a list of matching empty systems, or an empty list if {@code location} has no current system or none are
     *       found in range
     */
    private List<PlanetarySystem> findEmptySystems(ILocation location, double radius, LocalDate date) {
        PlanetarySystem origin = location.getCurrentSystem();
        if (origin == null) {
            return Collections.emptyList();
        }

        List<PlanetarySystem> emptySystems = new ArrayList<>();
        for (PlanetarySystem system : borderTracker.systemsNear(origin, radius)) {
            boolean isConnector = system.isConnector();
            Planet primaryPlanet = system.getPrimaryPlanet();

            boolean isTerrestrial = primaryPlanet != null &&
                                          primaryPlanet.getPlanetType() == PlanetaryType.TERRESTRIAL;
            if (isConnector && !isTerrestrial) {
                continue;
            }

            Set<Faction> factions = system.getFactionSet(date);
            if (factions.isEmpty() ||
                      (factions.size() == 1 && FactionHints.isEmptyFaction(factions.iterator().next()))) {
                emptySystems.add(system);
            }
        }
        return emptySystems;
    }

    /**
     * Finds {@code faction}'s own systems that border another faction, within {@code radius} light years of
     * {@code location}. Used to give a pirate attacker or defender a plausible frontier target/hideout.
     *
     * @param faction                the faction whose border systems are returned
     * @param excludedNeighbor       the opposing faction in the mission, excluded from the neighbor search
     * @param location               the location to center the search on
     * @param radius                 the search radius in light years from {@code location}'s current system
     * @param peripheryNeighborsOnly if {@code true}, only count borders with Periphery-tagged neighbors
     *
     * @return {@code faction}'s border systems matching the neighbor criteria, or an empty list if none are found
     */
    private List<PlanetarySystem> findFactionBorderSystems(Faction faction, Faction excludedNeighbor,
          ILocation location, double radius, boolean peripheryNeighborsOnly) {
        Set<PlanetarySystem> borderSystems = new HashSet<>();
        for (Faction neighbor : borderTracker.getFactionsInRegion(location, radius)) {
            if (neighbor.equals(faction) ||
                      neighbor.equals(excludedNeighbor) ||
                      FactionHints.isEmptyFaction(neighbor)) {
                continue;
            }
            if (peripheryNeighborsOnly && !neighbor.isPeriphery()) {
                continue;
            }
            borderSystems.addAll(borderTracker.getBorderSystems(neighbor, faction, location, radius));
        }
        return new ArrayList<>(borderSystems);
    }
}
