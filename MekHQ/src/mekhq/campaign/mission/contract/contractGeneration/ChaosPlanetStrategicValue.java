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
import java.util.List;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.enums.HPGRating;

/**
 * Scores how strategically valuable a target {@link Planet} is, so that Chaos-contract force generation can commit
 * better troops and equipment to fights over genuine prizes and dregs to fights over backwaters.
 *
 * <p>The score is a pure function of the world's own recorded galaxy data at the contract date &mdash; its
 * industrial and manufacturing output, technological sophistication, communications infrastructure, population, and
 * whether control of it is contested. It is deliberately independent of the employer, the enemy, and the objective;
 * those are folded in later, alongside this value, when the overall contract importance is determined.</p>
 *
 * <p>Each component contributes {@code 0..4} points, except the contested bonus ({@code 0} or
 * {@value #CONTESTED_BONUS}), giving a total range of {@code 0..}{@link #MAX_STRATEGIC_VALUE}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ChaosPlanetStrategicValue {
    static final int COMPONENT_MAX = 4;

    /** Extra points when more than one faction is recorded on the world, i.e. control of it is contested. */
    static final int CONTESTED_BONUS = 2;

    /**
     * The largest score {@link #calculate(Planet, LocalDate)} can return: five {@value #COMPONENT_MAX}-point components
     * plus the contested bonus.
     */
    public static final int MAX_STRATEGIC_VALUE = (5 * COMPONENT_MAX) + CONTESTED_BONUS;

    // Population brackets (inhabitants) mapped onto 0..COMPONENT_MAX points.
    private static final long POPULATION_TIER_1 = 1_000_000L;
    private static final long POPULATION_TIER_2 = 100_000_000L;
    private static final long POPULATION_TIER_3 = 1_000_000_000L;

    private ChaosPlanetStrategicValue() {
    }

    /**
     * Calculates the strategic value of the given planet at the given date.
     *
     * @param planet the target planet
     * @param when   the date to read the planet's galaxy data at
     *
     * @return a strategic-value score in the range {@code 0..}{@link #MAX_STRATEGIC_VALUE}
     */
    public static int calculate(final Planet planet, final LocalDate when) {
        final SocioIndustrialData socioIndustrial = planet.getSocioIndustrial(when);

        int value = 0;
        value += industryPoints(socioIndustrial);
        value += outputPoints(socioIndustrial);
        value += techPoints(socioIndustrial);
        value += hpgPoints(planet.getHPG(when));
        value += populationPoints(planet.getPopulation(when));
        value += contestedBonus(planet.getFactions(when));
        return value;
    }

    /**
     * Manufacturing capacity: a world that can build war materiel is worth committing line units to hold or take.
     */
    static int industryPoints(final SocioIndustrialData socioIndustrial) {
        return (socioIndustrial == null) ? 0 : planetaryRatingPoints(socioIndustrial.industry);
    }

    /**
     * Economic output: a productive world is a richer prize and a costlier one to lose.
     */
    static int outputPoints(final SocioIndustrialData socioIndustrial) {
        return (socioIndustrial == null) ? 0 : planetaryRatingPoints(socioIndustrial.output);
    }

    /**
     * Technological sophistication: advanced worlds can field and repair the best equipment, so both sides invest more
     * in the fight over them.
     */
    static int techPoints(final SocioIndustrialData socioIndustrial) {
        if (socioIndustrial == null || socioIndustrial.tech == null) {
            return 0;
        }

        return switch (socioIndustrial.tech) {
            case ADVANCED, A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
            case F, REGRESSED -> 0;
        };
    }

    /**
     * Communications infrastructure: an A- or B-rated HPG marks a regional command and coordination node.
     */
    static int hpgPoints(final HPGRating hpg) {
        if (hpg == null) {
            return 0;
        }

        return switch (hpg) {
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
            case X -> 0;
        };
    }

    /**
     * Population: a heavily settled world carries more political weight and offers more to defend or seize.
     */
    static int populationPoints(final Long population) {
        if (population == null || population <= 0) {
            return 0;
        }

        if (population < POPULATION_TIER_1) {
            return 1;
        } else if (population < POPULATION_TIER_2) {
            return 2;
        } else if (population < POPULATION_TIER_3) {
            return 3;
        } else {
            return COMPONENT_MAX;
        }
    }

    /**
     * Contested control: more than one recorded faction means a frontier or disputed world, where both sides commit
     * heavier forces.
     */
    static int contestedBonus(final List<String> factions) {
        return (factions != null && factions.size() > 1) ? CONTESTED_BONUS : 0;
    }

    /**
     * Maps a {@link PlanetaryRating} (A best, F worst) onto {@code 0..}{@value #COMPONENT_MAX} points.
     */
    private static int planetaryRatingPoints(final PlanetaryRating rating) {
        return (rating == null) ? 0 : (COMPONENT_MAX - rating.getIndex());
    }
}
