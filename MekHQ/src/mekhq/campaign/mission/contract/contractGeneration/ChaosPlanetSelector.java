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
import java.util.Collection;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.universe.Planet;

/**
 * Picks which planet within an already-chosen target system a contract is fought over, weighting the choice the same
 * way the target <em>system</em> is chosen: a weighted random draw rather than a flat one, so the fiction of the
 * operation steers the pick without ever making it certain.
 *
 * <p>Where the system picker weights by raw population and industry, this weights by a world's overall
 * {@link ChaosPlanetStrategicValue}, and the direction of that weighting is set by the player's objective. Most
 * operations are drawn toward the valuable worlds worth conquering, garrisoning, or raiding; a pirate hunt is instead
 * drawn toward the lawless backwaters where raiders actually hole up; and objectives with no geographic preference draw
 * uniformly.</p>
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

    private ChaosPlanetSelector() {
    }

    /**
     * Selects the target planet from a system's candidate planets, weighted by strategic value in the direction the
     * objective prefers.
     *
     * @param candidates    the candidate planets (a system's planets)
     * @param objectiveType the player's objective, which sets the weighting direction
     * @param when          the date to read each world's strategic value at
     *
     * @return the chosen planet, or {@code null} if there are no candidates
     */
    public static @Nullable Planet selectTargetPlanet(final Collection<Planet> candidates,
          final ChaosObjectiveType objectiveType, final LocalDate when) {
        if (candidates.isEmpty()) {
            return null;
        }

        Collection<Planet> pool = eligiblePlanets(preferenceFor(objectiveType), candidates, when);

        WeightedIntMap<Planet> weightedCandidates = new WeightedIntMap<>();
        for (Planet planet : pool) {
            weightedCandidates.add(planetWeight(planet, objectiveType, when), planet);
        }

        Planet chosen = weightedCandidates.randomItem();
        return (chosen != null) ? chosen : ObjectUtility.getRandomItem(pool);
    }

    /**
     * Narrows the candidate planets to those worth situating this kind of contract on. A contract is fought where there
     * is something to fight over, so for most objectives only inhabited worlds are eligible &mdash; otherwise a lone
     * inhabited world would be diluted by every lifeless rock, moon, and iceball sharing its system. A system with no
     * inhabited world at all (a genuinely uninhabited system) falls back to all of its bodies so generation never fails
     * for lack of a target.
     *
     * <p>The pirate hunt is the deliberate exception: raiders hole up on exactly the uninhabited fringe worlds every
     * other objective ignores, so its candidate pool is left untouched.</p>
     *
     * @param preference the objective's value preference
     * @param candidates the system's planets
     * @param when       the date to check habitation at
     *
     * @return the eligible subset, or all candidates if none are inhabited (or for a low-value pirate hunt)
     */
    static Collection<Planet> eligiblePlanets(final PlanetValuePreference preference,
          final Collection<Planet> candidates, final LocalDate when) {
        if (preference == PlanetValuePreference.LOW_VALUE) {
            return candidates;
        }

        List<Planet> inhabited = candidates.stream().filter(planet -> isInhabited(planet, when)).toList();
        return inhabited.isEmpty() ? candidates : inhabited;
    }

    /**
     * @return whether the world has a recorded population, i.e. there is anyone there to fight over
     */
    static boolean isInhabited(final Planet planet, final LocalDate when) {
        Long population = planet.getPopulation(when);
        return (population != null) && (population > 0);
    }

    /**
     * The weight a single planet gets for a weighted draw, always at least {@code 1} so every world stays pickable.
     *
     * <p>A high-value preference weights a world by {@code 1 + strategicValue}; a low-value preference inverts that to
     * {@code 1 + (max - strategicValue)}; a neutral preference weights every world equally.</p>
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
            case INVASION, GARRISON, RAID, PIRATE_RAID, GUERILLA_OPERATION -> PlanetValuePreference.HIGH_VALUE;
            case PIRATE_HUNT -> PlanetValuePreference.LOW_VALUE;
            case EXPEDITION, CADRE_DUTY -> PlanetValuePreference.NEUTRAL;
        };
    }
}
