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
package mekhq.campaign.mission.contract.contractGeneration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.enums.PlanetaryType;

/**
 * Picks which planet within an already-chosen target system a contract is fought over, weighting the choice the same
 * way the target <em>system</em> is chosen: a weighted random draw rather than a flat one, so the fiction of the
 * operation steers the pick without ever making it certain.
 *
 * <p>Where the system picker weights by raw population and industry, this weights by a world's overall
 * {@link ChaosPlanetStrategicValue}, and the direction of that weighting is set by the player's objective. Most
 * operations are drawn toward the valuable worlds worth conquering, garrisoning, or raiding; pirate work is instead
 * drawn toward the low-value fringe &mdash; a pirate hunt tracks raiders to the lawless backwaters they hole up on, and
 * a pirate raid seeks the weakest, least-defended world in the area &mdash; and objectives with no geographic preference
 * draw uniformly. Bodies with no ground to fight on (gas giants, ice giants, asteroid belts) are never chosen unless
 * someone actually lives there.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ChaosPlanetSelector {

    /** Which end of the strategic-value scale an objective's target world is drawn toward. */
    enum PlanetValuePreference {
        /** Favor valuable worlds &mdash; the prizes worth conquering, holding, or raiding. */
        HIGH_VALUE,
        /** Favor low-value worlds &mdash; the fringe backwaters raiders hide on. */
        LOW_VALUE,
        /** No geographic preference; draw uniformly. */
        NEUTRAL
    }

    /**
     * The draw weight an uninhabited body gets in a pirate hunt. Kept at the floor so that, while raiders can still be
     * tracked to a lifeless rock, the hunt is usually fought on an inhabited world: a low-value inhabited world weighs
     * {@code 1 + (max - strategicValue)}, which comfortably outweighs a handful of barren bodies.
     */
    static final int UNINHABITED_WORLD_WEIGHT = 1;

    private ChaosPlanetSelector() {
    }

    /**
     * Selects the target planet from a system's candidate planets, weighted by strategic value in the direction the
     * objective prefers.
     *
     * @param candidates    the candidate planets (a system's planets)
     * @param primaryPlanet the system's primary planet, used as the last resort when no body is otherwise eligible
     * @param objectiveType the player's objective, which sets the weighting direction
     * @param when          the date to read each world's strategic value at
     *
     * @return the chosen planet, or {@code null} if there are no candidates
     */
    public static @Nullable Planet selectTargetPlanet(final Collection<Planet> candidates,
          final @Nullable Planet primaryPlanet, final ChaosObjectiveType objectiveType, final LocalDate when) {
        if (candidates.isEmpty()) {
            return null;
        }

        Collection<Planet> pool = eligiblePlanets(objectiveType, candidates, primaryPlanet, when);

        WeightedIntMap<Planet> weightedCandidates = new WeightedIntMap<>();
        for (Planet planet : pool) {
            weightedCandidates.add(planetWeight(planet, objectiveType, when), planet);
        }

        Planet chosen = weightedCandidates.randomItem();
        return (chosen != null) ? chosen : ObjectUtility.getRandomItem(pool);
    }

    /**
     * Narrows the candidate planets to those worth situating this kind of contract on.
     *
     * <p>Only bodies a ground force can actually fight on are ever considered: any inhabited world (whatever its
     * type &mdash; a handful of canon settlements are habitats in asteroid belts or orbiting gas giants), plus
     * uninhabited {@link PlanetaryType#TERRESTRIAL terrestrial} and {@link PlanetaryType#DWARF_TERRESTRIAL dwarf
     * terrestrial} bodies. Uninhabited gas giants, ice giants, asteroid belts, and giant terrestrials (which in the
     * galaxy data are overwhelmingly hydrogen-shrouded gas worlds) are never eligible.</p>
     *
     * <p>A contract is fought where there is something to fight over, so for most objectives only inhabited worlds
     * are eligible &mdash; otherwise a lone inhabited world would be diluted by every lifeless rock sharing its system.
     * The pirate hunt is the deliberate exception: raiders hole up on the uninhabited fringe every other objective
     * ignores, so its pool keeps uninhabited ground as well (weighted down by {@link #planetWeight}).</p>
     *
     * <p>If nothing qualifies, a system with uninhabited ground falls back to that ground, and a system with none at
     * all falls back to its primary planet, so generation never fails for lack of a target.</p>
     *
     * @param objectiveType the player's objective
     * @param candidates    the system's planets
     * @param primaryPlanet the system's primary planet, or {@code null} if unknown
     * @param when          the date to check habitation at
     *
     * @return the eligible subset of the candidates, never empty when the candidates are not empty
     *
     * @author Illiani
     * @since 0.51.01
     */
    static Collection<Planet> eligiblePlanets(final ChaosObjectiveType objectiveType,
          final Collection<Planet> candidates, final @Nullable Planet primaryPlanet, final LocalDate when) {
        List<Planet> inhabitedWorlds = new ArrayList<>();
        List<Planet> uninhabitedGround = new ArrayList<>();
        for (Planet planet : candidates) {
            if (isInhabited(planet, when)) {
                inhabitedWorlds.add(planet);
            } else if (isUninhabitedGround(planet)) {
                uninhabitedGround.add(planet);
            }
        }

        List<Planet> eligible = new ArrayList<>(inhabitedWorlds);
        if (allowsUninhabitedWorlds(objectiveType) || inhabitedWorlds.isEmpty()) {
            eligible.addAll(uninhabitedGround);
        }

        if (!eligible.isEmpty()) {
            return eligible;
        }
        return (primaryPlanet != null) ? List.of(primaryPlanet) : candidates;
    }

    /**
     * @return whether the world has a recorded population, i.e. there is anyone there to fight over
     */
    static boolean isInhabited(final Planet planet, final LocalDate when) {
        Long population = planet.getPopulation(when);
        return population != null && population > 0;
    }

    /**
     * @return whether an uninhabited body still offers solid ground to fight on
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isUninhabitedGround(final Planet planet) {
        return switch (planet.getPlanetType()) {
            case TERRESTRIAL, DWARF_TERRESTRIAL -> true;
            case ASTEROID_BELT, GIANT_TERRESTRIAL, ICE_GIANT, GAS_GIANT -> false;
        };
    }

    /**
     * @return whether the objective may be situated on an uninhabited world even when an inhabited one is available
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean allowsUninhabitedWorlds(final ChaosObjectiveType objectiveType) {
        return objectiveType == ChaosObjectiveType.PIRATE_HUNT;
    }

    /**
     * The weight a single planet gets for a weighted draw, always at least {@code 1} so every world stays pickable.
     *
     * <p>A high-value preference weights a world by {@code 1 + strategicValue}; a low-value preference inverts that to
     * {@code 1 + (max - strategicValue)} for inhabited worlds, while uninhabited bodies (only reachable by a pirate
     * hunt) get the floor {@link #UNINHABITED_WORLD_WEIGHT}; a neutral preference weights every world equally.</p>
     *
     * @param planet        the planet to score
     * @param objectiveType the player's objective, which sets the weighting direction
     * @param when          the date to read the world's strategic value at
     *
     * @return the planet's draw weight
     */
    static int planetWeight(final Planet planet, final ChaosObjectiveType objectiveType, final LocalDate when) {
        PlanetValuePreference preference = preferenceFor(objectiveType);
        if (preference == PlanetValuePreference.NEUTRAL) {
            return 1;
        }

        if (preference == PlanetValuePreference.LOW_VALUE && !isInhabited(planet, when)) {
            return UNINHABITED_WORLD_WEIGHT;
        }

        int strategicValue = ChaosPlanetStrategicValue.calculate(planet, when);
        return switch (preference) {
            case HIGH_VALUE -> 1 + strategicValue;
            case LOW_VALUE -> 1 + (ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE - strategicValue);
            default -> throw new IllegalStateException("Unexpected value: " + preference);
        };
    }

    /**
     * Maps an objective to the kind of world it is drawn toward.
     */
    static PlanetValuePreference preferenceFor(final ChaosObjectiveType objectiveType) {
        return switch (objectiveType) {
            case INVASION, GARRISON, RAID, GUERILLA_OPERATION -> PlanetValuePreference.HIGH_VALUE;
            case PIRATE_HUNT, PIRATE_RAID -> PlanetValuePreference.LOW_VALUE;
            case EXPEDITION, CADRE_DUTY -> PlanetValuePreference.NEUTRAL;
        };
    }
}
