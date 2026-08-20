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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.enums.HPGRating;

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
 * @param hpg                the planet's HPG rating, used as a technology/industry proxy ({@code X} when unknown)
 *
 * @author Illiani
 * @since 0.51.01
 */
public record PlanetProfile(int temperatureCelsius, double diameterKm, int waterPercent, boolean airless,
      @Nullable Atmosphere atmosphere, String composition, int landmassCount, double gravity,
      @Nullable Long population, HPGRating hpg) {
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
    /** The temperature (Celsius) at which habitability peaks. */
    public static final int HABITABLE_TEMPERATURE_CELSIUS = 15;

    private static final int HABITABLE_TEMPERATURE_SPAN = 60;
    private static final double TAINTED_HABITABILITY_FACTOR = 0.4;
    private static final double NON_BREATHABLE_HABITABILITY_FACTOR = 0.6;

    private static final double MIN_SIZE_FACTOR = 0.5;
    private static final double MAX_SIZE_FACTOR = 2.0;

    /**
     * Note that this is a ratio of <em>diameters</em>, while its caller multiplies it into an area (a hex count).
     * Surface area goes as the square of diameter, so the linear ratio understates how much larger a large world is.
     * That is deliberate and has been measured: across the 2430 habitable worlds in the universe data, diameters span
     * only 0.82 to 1.08 of Terra's - a world is largely habitable <em>because</em> it is near Terra-sized - so this
     * factor moves a sector by roughly a sixth either way, while water coverage moves it by up to four times. A
     * smaller, wetter world can therefore produce a physically larger sector than a bigger, drier one. Only the
     * sector's total extent inverts; its dry playable ground stays ordered correctly, because the water compensation in
     * {@code applyImprovedDimensions} exists to protect the recon budget.
     *
     * <p>Squaring this to correct the dimensions was considered and declined - it would leave the sector rules keyed
     * to a different quantity than the one the system was balanced against. If it is ever revisited, note that the
     * clamp below is applied to the <em>linear</em> ratio: squaring inside it would send a capped world to {@code 4.0}
     * rather than {@code 2.0}, so the bounds need re-deriving in the same change.</p>
     *
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
     * @return a technology/industry proxy in {@code [0.0, 1.0]} derived from the HPG rating ({@code X} = 0.0, {@code A}
     *       = 1.0)
     */
    public double techLevel() {
        return hpg.ordinal() / (double) (HPGRating.values().length - 1);
    }

    /**
     * @return how liveable the world is, {@code 0.0} (hostile) to {@code 1.0} (temperate and breathable). Airless
     *       worlds are {@code 0.0}; comfort peaks near {@link #HABITABLE_TEMPERATURE_CELSIUS} and falls off toward
     *       extremes, reduced further on tainted/toxic or non-breathable atmospheres.
     */
    public double habitability() {
        if (airless) {
            return 0.0;
        }

        // Comfort falls off linearly with distance from the ideal temperature, reaching zero a full span away. The
        // division runs distance-over-span, not span-over-distance: inverted, the ideal temperature divides by zero and
        // scores worst, every temperate world scores zero, and comfort rises with extremity.
        int temperatureDistance = Math.abs(temperatureCelsius - HABITABLE_TEMPERATURE_CELSIUS);
        double comfort = Math.max(0.0, 1.0 - (temperatureDistance / (double) HABITABLE_TEMPERATURE_SPAN));
        if (taintedOrToxic()) {
            comfort *= TAINTED_HABITABILITY_FACTOR;
        } else if (!breathable()) {
            comfort *= NON_BREATHABLE_HABITABILITY_FACTOR;
        }
        return comfort;
    }

    /**
     * @return {@code log10} of the population (e.g. 6 for a million, 9 for a billion), or {@code 0.0} when the
     *       population is unknown or zero
     */
    public double populationLog() {
        if ((population == null) || (population <= 0)) {
            return 0.0;
        }
        return Math.log10(population);
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
    public static PlanetProfile from(AbstractContract contract, Campaign campaign) {
        Planet planet = contract.getTargetPlanet();
        if (planet == null) {
            LOGGER.warn("No target planet; using a neutral planet profile");
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
        Atmosphere atmosphere = null;
        SourceableValue<Atmosphere> sourcedAtmosphere = planet.getSourcedAtmosphere(date);
        if (sourcedAtmosphere != null) {
            atmosphere = sourcedAtmosphere.getValue();
        }

        megamek.common.planetaryConditions.Atmosphere pressure = planet.getPressure(date);
        boolean airless = (pressure == megamek.common.planetaryConditions.Atmosphere.VACUUM) ||
                                ((atmosphere != null) && atmosphere.isNone());

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
        HPGRating hpg = planet.getHPG(date);

        return new PlanetProfile(resolvedTemperature,
              diameter,
              resolvedWater,
              airless,
              atmosphere,
              resolvedComposition,
              resolvedLandmassCount,
              resolvedGravity,
              population,
              (hpg != null) ? hpg : HPGRating.X);
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
              null,
              HPGRating.X);
    }
}
