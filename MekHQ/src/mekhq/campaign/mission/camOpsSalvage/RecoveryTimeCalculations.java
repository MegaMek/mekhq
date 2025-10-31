/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.camOpsSalvage;

import static java.lang.Math.round;

import megamek.common.annotations.Nullable;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.universe.Planet;

/**
 * Utility class for calculating entity recovery times based on environmental conditions.
 *
 * <p>This class implements the recovery time calculation rules from Campaign Operations (CamOps), pages 193 and 209.
 * Recovery times are affected by various planetary conditions, including weather, wind, temperature, gravity,
 * atmospheric composition, and lighting conditions.</p>
 *
 * <p>Environmental factors apply multipliers to the base recovery time, with adverse conditions increasing the time
 * required. Multiple conditions can stack, resulting in significantly longer recovery periods under harsh environmental
 * circumstances.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class RecoveryTimeCalculations {
    private static final MMLogger LOGGER = MMLogger.create(RecoveryTimeCalculations.class);

    // Planetary condition multipliers. CamOps pg193 & pg 209
    // Heavy Snow, Ice Storm, Lightning Storm, Strong Gale, Torrential Downpour
    private final static double TERRIBLE_WEATHER = 0.25;
    private final static double ZERO_G = 0.5;
    private final static double LOW_G = 0.25; // <0.8G
    private final static double HIGH_G = 0.5; // >1.25G
    private final static double VERY_HIGH_G = 1.0; // >=2.0G
    private final static double VACUUM_OR_TAINTED_ATMOSPHERE = 0.5;
    private final static double TRACE_OR_VERY_HIGH_PRESSURE_ATMOSPHERE = 0.25;
    private final static double ABOVE_50_C_OR_BELOW_30_C = 0.25;
    private final static double HURRICANE_OR_TORNADO = 0.5;
    private final static double MOONLESS_NIGHT_OR_SOLAR_FLARE = 0.25;
    private final static double PITCH_BLACK = 0.5;

    final static double BASE_MULTIPLIER = 1.0;
    private final static double DEFAULT_MULTIPLIER = 0.0;

    /**
     * Calculates the total recovery time for an entity based on environmental conditions.
     *
     * <p>This method evaluates all environmental factors present in the scenario and applies appropriate multipliers
     * to the base recovery time. The calculation follows Campaign Operations rules (pg 214, pg 209), considering:</p>
     * <ul>
     *   <li>Weather conditions (storms, precipitation)</li>
     *   <li>Wind conditions (tornadoes, hurricanes)</li>
     *   <li>Temperature extremes (above 50°C or below -30°C)</li>
     *   <li>Gravity variations (zero-G, low-G, high-G)</li>
     *   <li>Atmospheric conditions (vacuum, tainted, pressure extremes)</li>
     *   <li>Lighting conditions (moonless night, pitch black, solar flares)</li>
     * </ul>
     *
     * <p>Each environmental factor contributes an additive multiplier to the base multiplier of 1.0. The final
     * recovery time is calculated as: {@code baseRecoveryTime × totalMultiplier}.</p>
     *
     * <p>If the calculated recovery time exceeds {@link Integer#MAX_VALUE}, the method logs a warning and returns
     * {@code Integer.MAX_VALUE}.</p>
     *
     * @param entityName       The name of the entity, used for logging purposes.
     * @param baseRecoveryTime The base recovery time in minutes before environmental modifiers are applied.
     * @param scenario         The {@link Scenario} containing the environmental conditions.
     * @param currentPlanet    The {@link Planet} where the scenario takes place, used to determine atmospheric
     *                         properties.
     *
     * @return A {@link RecoveryTimeData} record containing all multipliers and the calculated total recovery time in
     *       minutes.
     */
    public static RecoveryTimeData calculateRecoveryTimeForEntity(String entityName, int baseRecoveryTime,
          Scenario scenario, @Nullable Planet currentPlanet) {
        boolean isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;

        if (isInSpace) {
            int totalRecoveryTime = getTotalRecoveryTime(entityName, baseRecoveryTime, BASE_MULTIPLIER);
            return new RecoveryTimeData(DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                  DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER, baseRecoveryTime, totalRecoveryTime);
        } else {
            double weatherMultiplier = getWeatherMultiplier(scenario.getWeather());
            double windMultiplier = getWindMultiplier(scenario.getWind());
            double temperatureMultiplier = getTemperatureMultiplier(scenario.getTemperature());
            double gravityMultiplier = getGravityMultiplier(scenario.getGravity());
            double atmosphereMultiplier = getAtmosphereMultiplier(scenario.getAtmosphere(),
                  currentPlanet == null ? null : currentPlanet.getAtmosphere(scenario.getDate()));
            double lightMultiplier = getLightMultiplier(scenario.getLight());

            double totalMultiplier = BASE_MULTIPLIER +
                                           weatherMultiplier +
                                           windMultiplier +
                                           temperatureMultiplier +
                                           gravityMultiplier +
                                           atmosphereMultiplier +
                                           lightMultiplier;

            int totalRecoveryTime = getTotalRecoveryTime(entityName, baseRecoveryTime, totalMultiplier);
            return new RecoveryTimeData(weatherMultiplier, windMultiplier, temperatureMultiplier, gravityMultiplier,
                  atmosphereMultiplier, lightMultiplier, baseRecoveryTime, totalRecoveryTime);
        }
    }

    /**
     * Calculates the total recovery time by applying the multiplier to the base recovery time.
     *
     * <p>This method performs the final calculation, rounding the result to the nearest integer. If an
     * {@link ArithmeticException} occurs (typically due to integer overflow), the method logs a warning and returns
     * {@link Integer#MAX_VALUE} as a safe fallback.</p>
     *
     * @param entityName       The name of the entity, used for logging if an error occurs.
     * @param baseRecoveryTime The base recovery time in minutes.
     * @param totalMultiplier  The combined multiplier from all environmental conditions.
     *
     * @return The total recovery time in minutes, rounded to the nearest integer, or {@link Integer#MAX_VALUE} if the
     *       calculation overflows.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getTotalRecoveryTime(String entityName, int baseRecoveryTime, double totalMultiplier) {
        try {
            return Math.toIntExact(round(baseRecoveryTime * totalMultiplier));
        } catch (ArithmeticException e) {
            LOGGER.warn("ArithmeticException occurred while calculating recovery time for entity: {}",
                  entityName);
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Determines the recovery time multiplier based on lighting conditions.
     *
     * <p>Lighting conditions that impair visibility increase recovery time:</p>
     * <ul>
     *   <li><b>Moonless Night / Solar Flare:</b> +0.25 multiplier</li>
     *   <li><b>Pitch Black:</b> +0.5 multiplier</li>
     *   <li><b>Other conditions:</b> No multiplier</li>
     * </ul>
     *
     * @param light The {@link Light} condition, or {@code null} if not specified.
     *
     * @return The lighting multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getLightMultiplier(@Nullable Light light) {
        if (light == null) {
            return DEFAULT_MULTIPLIER;
        }

        return switch (light) {
            case MOONLESS, SOLAR_FLARE -> MOONLESS_NIGHT_OR_SOLAR_FLARE;
            case PITCH_BLACK -> PITCH_BLACK;
            default -> DEFAULT_MULTIPLIER;
        };
    }

    /**
     * Determines the recovery time multiplier based on atmospheric conditions.
     *
     * <p>Atmospheric conditions that require special equipment or pose hazards increase recovery time:</p>
     * <ul>
     *   <li><b>Vacuum or Tainted Atmosphere:</b> +0.5 multiplier</li>
     *   <li><b>Trace or Very High Pressure:</b> +0.25 multiplier</li>
     *   <li><b>Standard Atmosphere:</b> No multiplier</li>
     * </ul>
     *
     * <p>If the planetary atmosphere is tainted, the tainted atmosphere multiplier is applied regardless of the
     * scenario's atmospheric setting.</p>
     *
     * @param scenarioAtmosphere  The {@link Atmosphere} condition in the scenario, or {@code null} if not specified.
     * @param planetaryAtmosphere The planet's {@link mekhq.campaign.universe.Atmosphere}, used to check for tainted
     *                            conditions.
     *
     * @return The atmospheric multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getAtmosphereMultiplier(@Nullable Atmosphere scenarioAtmosphere,
          @Nullable mekhq.campaign.universe.Atmosphere planetaryAtmosphere) {
        double atmosphereMultiplier = DEFAULT_MULTIPLIER;

        if (scenarioAtmosphere != null) {
            atmosphereMultiplier = switch (scenarioAtmosphere) {
                case VACUUM -> VACUUM_OR_TAINTED_ATMOSPHERE;
                case TRACE, VERY_HIGH -> TRACE_OR_VERY_HIGH_PRESSURE_ATMOSPHERE;
                default -> DEFAULT_MULTIPLIER;
            };
        }

        if (planetaryAtmosphere != null && planetaryAtmosphere.isTainted()) {
            return VACUUM_OR_TAINTED_ATMOSPHERE;
        }

        return atmosphereMultiplier;
    }

    /**
     * Determines the recovery time multiplier based on gravity conditions.
     *
     * <p>Gravity variations from standard 1.0G affect recovery operations:</p>
     * <ul>
     *   <li><b>Zero-G (≤0.0G):</b> +0.5 multiplier</li>
     *   <li><b>Low-G (&lt;0.8G):</b> +0.25 multiplier</li>
     *   <li><b>High-G (&gt;1.25G to &lt;2.0G):</b> +0.5 multiplier</li>
     *   <li><b>Very High-G (≥2.0G):</b> +1.0 multiplier</li>
     *   <li><b>Standard Gravity (0.8G to 1.25G):</b> No multiplier</li>
     * </ul>
     *
     * @param gravity The gravity level as a multiple of standard gravity (1.0G = Earth normal).
     *
     * @return The gravity multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getGravityMultiplier(float gravity) {
        if (gravity <= 0.0f) {
            return ZERO_G;
        } else if (gravity < 0.8f) {
            return LOW_G;
        } else if (gravity >= 2.0f) {
            return VERY_HIGH_G;
        } else if (gravity > 1.25) {
            return HIGH_G;
        }
        return DEFAULT_MULTIPLIER;
    }

    /**
     * Determines the recovery time multiplier based on temperature conditions.
     *
     * <p>Extreme temperatures require additional precautions and specialized equipment:</p>
     * <ul>
     *   <li><b>Temperature &gt;50°C or &lt;-30°C:</b> +0.25 multiplier</li>
     *   <li><b>Temperature between -30°C and 50°C:</b> No multiplier</li>
     * </ul>
     *
     * @param temperature The ambient temperature in degrees Celsius.
     *
     * @return The temperature multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getTemperatureMultiplier(int temperature) {
        if (temperature > 50 || temperature < 30) {
            return ABOVE_50_C_OR_BELOW_30_C;
        }
        return DEFAULT_MULTIPLIER;
    }

    /**
     * Determines the recovery time multiplier based on wind conditions.
     *
     * <p>Severe wind conditions impair recovery operations:</p>
     * <ul>
     *   <li><b>Tornadoes (F1-F3 or F4):</b> +0.5 multiplier</li>
     *   <li><b>Other wind conditions:</b> No multiplier</li>
     * </ul>
     *
     * @param wind The {@link Wind} condition, or {@code null} if not specified.
     *
     * @return The wind multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getWindMultiplier(@Nullable Wind wind) {
        if (wind == null) {
            return DEFAULT_MULTIPLIER;
        }

        return switch (wind) {
            case TORNADO_F1_TO_F3, TORNADO_F4 -> HURRICANE_OR_TORNADO;
            default -> DEFAULT_MULTIPLIER;
        };
    }

    /**
     * Determines the recovery time multiplier based on weather conditions.
     *
     * <p>Severe weather conditions significantly impair recovery operations:</p>
     * <ul>
     *   <li><b>Heavy Snow, Ice Storm, Lightning Storm, or Torrential Downpour:</b> +0.25 multiplier</li>
     *   <li><b>Other weather conditions:</b> No multiplier</li>
     * </ul>
     *
     * @param weather The {@link Weather} condition, or {@code null} if not specified.
     *
     * @return The weather multiplier to add to the total multiplier.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static double getWeatherMultiplier(@Nullable Weather weather) {
        if (weather == null) {
            return DEFAULT_MULTIPLIER;
        }

        return switch (weather) {
            case DOWNPOUR, HEAVY_SNOW, ICE_STORM, LIGHTNING_STORM -> TERRIBLE_WEATHER;
            default -> DEFAULT_MULTIPLIER;
        };
    }
}
