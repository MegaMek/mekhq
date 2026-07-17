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
package mekhq.campaign.mission.newContract;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.newContract.targetFinder.MissionTargetFinder;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Describes how a contract's mission location should be selected, based on what kind of operation the contract
 * represents. The default border-based search in {@link MissionTargetFinder} fits a conventional front-line campaign,
 * but not every contract type happens at the front: training cadres work in safe rear areas, riots need populated
 * worlds, raiders strike past the border, and guerrillas operate deep in occupied territory.
 *
 * <p>Each profile is a <em>preference</em>, not a hard restriction: if a profile's preferred tier finds no candidate
 * systems, selection falls back to the default border-based search so contract generation never fails just because the
 * fiction-preferred geography doesn't exist in the area. The one exception is {@link #INVASION}, which is a hard
 * restriction: an invader can only supply and hold a conquest adjacent to its own territory, so with no shared border
 * there is no viable target and the contract is regenerated instead.</p>
 *
 * <p>The profile is derived from the contract type at {@code setSystemId} time, after any contract-type overrides
 * (e.g. pity contracts) have run, so it always reflects the contract's final type.</p>
 */
public enum MissionLocationProfile {
    /** The standard shared-border search, unmodified. */
    DEFAULT {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return Collections.emptyList();
        }
    },
    /**
     * The mission happens in the defender's interior, away from the front: training cadres and standing retainers are
     * stationed where it's safe, not on a contested border world.
     */
    REAR_AREA {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return finder.findRearAreaTargets(attacker, defender, location, radius, date);
        }
    },
    /**
     * The mission can happen anywhere in the defender's territory, preferring heavily populated worlds: riots and
     * internal-security work need people, not proximity to the enemy.
     */
    INTERIOR_POPULATED {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return finder.findAllDefenderTargets(defender, location, radius, date);
        }
    },
    /**
     * The mission is a hit-and-run strike that can reach past the immediate border into the defender's near interior,
     * roughly twice as deep as a conventional front line extends.
     */
    DEEP_RAID {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return finder.findDeepRaidTargets(attacker, defender, location, radius);
        }
    },
    /**
     * The mission happens behind enemy lines, preferring worlds the attacker recently lost to the defender (occupied
     * territory with a sympathetic population), then any defender world away from the shared border.
     */
    OCCUPIED_TERRITORY {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return finder.findOccupiedTerritoryTargets(attacker, defender, location, radius, date);
        }
    },
    /**
     * The mission uses the standard candidate pool, but the final pick is weighted toward high-value worlds
     * (population, major HPG presence) instead of being uniformly random: a sabotage or espionage campaign aims at a
     * factory world, not an uninhabited iceball. Uses the default candidate pool unchanged (no preferred tier here),
     * differing only in how the final pick is weighted (see {@link #isPopulationWeighted()}).
     */
    HIGH_VALUE {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return Collections.emptyList();
        }
    },
    /**
     * The mission is a full planetary invasion, viable only on the shared border between the two factions: the attacker
     * can take, supply, and hold a world only from adjacent friendly territory. The pick among border worlds is
     * weighted like {@link #HIGH_VALUE}, but unlike every other profile there is no deep-placement fallback &mdash; no
     * shared border means no viable invasion target at all.
     */
    INVASION {
        @Override
        public List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
              ILocation location, double radius, LocalDate date) {
            return finder.findSharedBorderTargets(attacker, defender, location, radius, date);
        }
    };

    /**
     * Finds this profile's preferred-tier target systems, or an empty list to fall through to the finder's default
     * shared-border chain. The finder owns the actual geography searches (they depend on its border tracker and faction
     * hints); this method routes to the right one.
     *
     * <p>{@link #DEFAULT} and {@link #HIGH_VALUE} intentionally return nothing: DEFAULT has no preference, and
     * HIGH_VALUE uses the default candidate pool unchanged, differing only in how the final pick is weighted (see
     * {@link #isPopulationWeighted()}). An empty result from {@link #INVASION} does NOT fall through &mdash;
     * {@link MissionTargetFinder#find} blocks it from reaching the deep fallbacks, since an invasion with no shared
     * border has no viable target.</p>
     *
     * @param finder   the finder supplying the geography searches and regional context
     * @param attacker the attacking faction
     * @param defender the defending faction
     * @param location the location to center the search on
     * @param radius   the search radius in light years from {@code location}'s current system
     * @param date     the date to check faction control and diplomatic relations against
     *
     * @return the profile's preferred candidate systems, or an empty list to fall through to the default chain
     */
    public abstract List<PlanetarySystem> findTargets(MissionTargetFinder finder, Faction attacker, Faction defender,
          ILocation location, double radius, LocalDate date);

    /**
     * How many years back a "recently conquered" ownership check looks. Shared between the {@link #OCCUPIED_TERRITORY}
     * location tier and {@link EnemySelectionProfile#OCCUPYING_POWER}'s enemy preference, so that when guerrilla
     * contracts pick an occupying enemy, the location tier's flipped-world search agrees on what "recent" means.
     */
    public static final int OCCUPIED_TERRITORY_LOOKBACK_YEARS = 10;

    /**
     * @return {@code true} if the final pick from this profile's candidate list should be weighted by how valuable
     *       (populated, connected) each world is, rather than uniformly random
     */
    public boolean isPopulationWeighted() {
        return (this == HIGH_VALUE) || (this == INTERIOR_POPULATED) || (this == INVASION);
    }

    /**
     * @return {@code true} if the weighted pick should also factor in a world's industrial capacity (see
     *       {@link mekhq.campaign.universe.SocioIndustrialData}) on top of population and HPG presence.
     *       {@link #HIGH_VALUE} and {@link #INVASION} target worlds worth sabotaging or conquering for their industry,
     *       not just their head count; {@link #INTERIOR_POPULATED} cares only about people (riots and internal security
     *       happen wherever the crowds are, factory or farm alike).
     */
    public boolean isIndustriallyWeighted() {
        return (this == HIGH_VALUE) || (this == INVASION);
    }
}
