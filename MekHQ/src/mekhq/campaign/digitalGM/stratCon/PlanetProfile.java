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
package mekhq.campaign.digitalGM.stratCon;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SourceableValue;

/**
 * Immutable snapshot of the planetary data that drives StratCon improved sector generation, resolved once from a
 * contract's destination planet with neutral fallbacks for anything the universe data does not record.
 *
 * <p>All inputs are optional in the universe data. When a value is unknown the profile substitutes a neutral,
 * roughly Terra-like default so generation always has something sensible to work with. In particular, an unknown
 * atmosphere is treated as breathable rather than airless: a world is only considered airless when its data
 * <em>explicitly</em> records a vacuum or a "None" atmosphere.</p>
 *
 * @param temperatureCelsius equatorial temperature in degrees Celsius
 * @param diameterKm         planetary diameter in kilometres ({@code 0} when unknown; {@link #sizeFactor()} treats that
 *                           as Terra-sized)
 * @param waterPercent       surface water coverage, clamped to {@code 0..100}
 * @param airless            {@code true} when the planet explicitly has no breathable atmosphere (vacuum or "None")
 * @param atmosphere         atmospheric composition, or {@code null} when unknown (treated as neutral/breathable)
 * @param composition        lower-cased surface composition string (e.g. {@code "ice"}, {@code "arid/rock"}), never
 *                           {@code null} but possibly empty
 * @param landmassCount      number of distinct landmasses (at least {@code 1})
 * @param gravity            surface gravity in G
 * @param population         primary-planet population, or {@code null} when unknown
 *
 * @author Illiani
 * @since 0.51.01
 */
public record PlanetProfile(int temperatureCelsius, double diameterKm, int waterPercent, boolean airless,
      @Nullable Atmosphere atmosphere, String composition, int landmassCount, double gravity,
      @Nullable Long population) {
    private static final MMLogger LOGGER = MMLogger.create(PlanetProfile.class);

    /** Standard room temperature, used when a planet records no temperature. */
    public static final int NEUTRAL_TEMPERATURE_CELSIUS = 25;
    /** Terra's diameter in kilometres; the reference point for {@link #sizeFactor()}. */
    public static final double TERRA_DIAMETER_KM = 12742.0;
    /** Neutral (mid-range) surface water coverage, used when a planet records none. */
    public static final int NEUTRAL_WATER_PERCENT = 50;
    /** Neutral landmass count, used when a planet records none. */
    public static final int NEUTRAL_LANDMASS_COUNT = 1;
    /** Neutral surface gravity in G, used when a planet records none. */
    public static final double NEUTRAL_GRAVITY = 1.0;

    private static final double MIN_SIZE_FACTOR = 0.5;
    private static final double MAX_SIZE_FACTOR = 2.0;

    /**
     * @return the sector-size scaling factor derived from planetary diameter, clamped to {@code [0.5, 2.0]}. An unknown
     *       diameter yields {@code 1.0} (Terra-sized).
     */
    public double sizeFactor() {
        if (diameterKm <= 0.0) {
            return 1.0;
        }
        return Math.clamp(diameterKm / TERRA_DIAMETER_KM, MIN_SIZE_FACTOR, MAX_SIZE_FACTOR);
    }

    /**
     * @return {@code true} if the atmosphere supports open-air vegetation. Airless worlds are never breathable; an
     *       unknown atmosphere is treated as breathable.
     */
    public boolean breathable() {
        return !airless && ((atmosphere == null) || (atmosphere == Atmosphere.BREATHABLE));
    }

    /**
     * @return {@code true} if the atmosphere is tainted or toxic, in which case open-air vegetation is excluded from
     *       terrain generation
     */
    public boolean taintedOrToxic() {
        return (atmosphere != null) && (atmosphere.isTainted() || atmosphere.isToxic());
    }

    /**
     * @return {@code true} if the surface composition reads as icy (ice or frozen)
     */
    public boolean hasIcyComposition() {
        return compositionContains("ice") || compositionContains("frozen");
    }

    /**
     * @return {@code true} if the surface composition reads as rocky (arid, rock, or desert)
     */
    public boolean hasRockyComposition() {
        return compositionContains("arid") || compositionContains("rock") || compositionContains("desert");
    }

    private boolean compositionContains(String token) {
        return !composition.isEmpty() && composition.contains(token);
    }

    /**
     * Builds a profile from the destination planet of the given contract. Falls back to a fully neutral profile when
     * the contract has no system or the system has no primary planet.
     *
     * @param contract the contract whose destination planet supplies the data
     * @param campaign the campaign, used only for the current date
     *
     * @return a resolved {@link PlanetProfile}
     */
    public static PlanetProfile from(AtBContract contract, Campaign campaign) {
        PlanetarySystem system = contract.getSystem();
        if (system == null) {
            LOGGER.warn("Contract {} has no system; using a neutral planet profile", contract.getName());
            return neutral(NEUTRAL_TEMPERATURE_CELSIUS);
        }

        Planet planet = system.getPrimaryPlanet();
        if (planet == null) {
            LOGGER.warn("System {} has no primary planet; using a neutral planet profile",
                  system.getName(campaign.getLocalDate()));
            return neutral(NEUTRAL_TEMPERATURE_CELSIUS);
        }

        return from(planet, campaign.getLocalDate());
    }

    /**
     * Builds a profile from an explicit planet and date. Every field falls back to a neutral default when the planet
     * does not record it.
     *
     * @param planet the planet to read
     * @param date   the date at which to evaluate time-varying planetary data
     *
     * @return a resolved {@link PlanetProfile}
     */
    public static PlanetProfile from(Planet planet, LocalDate date) {
        Integer temperature = planet.getTemperature(date);
        int resolvedTemperature = (temperature != null) ? temperature : NEUTRAL_TEMPERATURE_CELSIUS;

        double diameter = planet.getDiameter();

        int resolvedWater = NEUTRAL_WATER_PERCENT;
        SourceableValue<Integer> sourcedWater = planet.getSourcedPercentWater(date);
        if ((sourcedWater != null) && (sourcedWater.getValue() != null)) {
            resolvedWater = Math.clamp(sourcedWater.getValue(), 0, 100);
        }

        // getAtmosphere() collapses "unknown" and "None" onto NONE, so read the sourced value to tell them apart: a
        // null source means the datum is simply absent (treated as neutral), not that the world is airless.
        Atmosphere composition = null;
        SourceableValue<Atmosphere> sourcedAtmosphere = planet.getSourcedAtmosphere(date);
        if (sourcedAtmosphere != null) {
            composition = sourcedAtmosphere.getValue();
        }

        megamek.common.planetaryConditions.Atmosphere pressure = planet.getPressure(date);
        boolean airless = (pressure == megamek.common.planetaryConditions.Atmosphere.VACUUM) ||
                                ((composition != null) && composition.isNone());

        String resolvedComposition = "";
        SourceableValue<String> sourcedComposition = planet.getSourcedComposition(date);
        if ((sourcedComposition != null) && (sourcedComposition.getValue() != null)) {
            resolvedComposition = sourcedComposition.getValue().toLowerCase();
        }

        List<LandMass> landMasses = planet.getLandMasses();
        int resolvedLandmassCount = ((landMasses != null) && !landMasses.isEmpty()) ?
                                          landMasses.size() :
                                          NEUTRAL_LANDMASS_COUNT;

        Double gravity = planet.getGravity();
        double resolvedGravity = (gravity != null) ? gravity : NEUTRAL_GRAVITY;

        Long population = planet.getPopulation(date);

        return new PlanetProfile(resolvedTemperature,
              diameter,
              resolvedWater,
              airless,
              composition,
              resolvedComposition,
              resolvedLandmassCount,
              resolvedGravity,
              population);
    }

    /**
     * @param temperatureCelsius the equatorial temperature to record on the otherwise-neutral profile
     *
     * @return a Terra-like profile with the given temperature and neutral defaults for every other field
     */
    public static PlanetProfile neutral(int temperatureCelsius) {
        return new PlanetProfile(temperatureCelsius,
              TERRA_DIAMETER_KM,
              NEUTRAL_WATER_PERCENT,
              false,
              null,
              "",
              NEUTRAL_LANDMASS_COUNT,
              NEUTRAL_GRAVITY,
              null);
    }
}
