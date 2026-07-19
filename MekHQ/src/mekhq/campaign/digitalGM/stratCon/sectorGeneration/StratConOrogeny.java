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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * Loads the orogeny (mountain-building) profiles used by improved sector generation from {@code OrogenyProfiles.yaml}
 * and picks one for a planet from its combined conditions.
 *
 * <p>Unlike hydrology, which weights on a single scalar, orogeny selection is multifactor: a profile's weight is the
 * product of Gaussian factors for gravity, temperature, and water coverage (each skipped when the profile is
 * indifferent to it), times categorical multipliers for rocky, icy, and airless worlds. Neighboring profiles still
 * appear for variety; larger sigmas mean more variety.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConOrogeny {
    private static final MMLogger LOGGER = MMLogger.create(StratConOrogeny.class);

    /** Default Gaussian standard deviations (in each condition's own units) if the YAML omits them. */
    public static final double DEFAULT_GRAVITY_SIGMA = 0.4;
    public static final double DEFAULT_TEMPERATURE_SIGMA = 25.0;
    public static final double DEFAULT_WATER_SIGMA = 25.0;

    /** The profile fallen back to when the YAML is missing, empty, or unreadable: indifferent to everything. */
    private static final OrogenyProfile DEFAULT_PROFILE = new OrogenyProfile(OrogenyProfileType.CORDILLERA,
          null, null, null, null, null, null, null, null);

    private static final double PICK_RESOLUTION = 1000000.0;

    private final double gravitySigma;
    private final double temperatureSigma;
    private final double waterSigma;
    private final List<OrogenyProfile> profiles;

    private static StratConOrogeny instance;

    /** The root object of {@code OrogenyProfiles.yaml}. */
    public record Library(Double gravitySigma, Double temperatureSigma, Double waterSigma,
          List<OrogenyProfile> profiles) {}

    private StratConOrogeny(double gravitySigma, double temperatureSigma, double waterSigma,
          List<OrogenyProfile> profiles) {
        this.gravitySigma = gravitySigma;
        this.temperatureSigma = temperatureSigma;
        this.waterSigma = waterSigma;
        this.profiles = profiles;
    }

    /**
     * @return the singleton orogeny instance, loading it from the YAML on first use and falling back to a single
     *       indifferent default profile if the file cannot be read
     */
    public static StratConOrogeny getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static StratConOrogeny fallback() {
        return new StratConOrogeny(DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA,
              List.of(DEFAULT_PROFILE));
    }

    private static StratConOrogeny load() {
        File file = new File(MHQConstants.STRAT_CON_OROGENY_PROFILES_PATH);
        if (!file.exists()) {
            LOGGER.warn("Orogeny profiles file {} does not exist; using a single default profile",
                  MHQConstants.STRAT_CON_OROGENY_PROFILES_PATH);
            return fallback();
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            Library library = mapper.readValue(file, Library.class);
            if ((library == null) || (library.profiles() == null) || library.profiles().isEmpty()) {
                LOGGER.warn("Orogeny profiles file {} held no profiles; using a single default profile",
                      MHQConstants.STRAT_CON_OROGENY_PROFILES_PATH);
                return fallback();
            }

            // Drop any entries whose type failed to parse (READ_UNKNOWN_ENUM_VALUES_AS_NULL leaves them null).
            List<OrogenyProfile> validProfiles = library.profiles()
                                                       .stream()
                                                       .filter(profile -> profile.type() != null)
                                                       .toList();
            if (validProfiles.isEmpty()) {
                LOGGER.warn("Orogeny profiles file {} held no recognizable profiles; using a single default profile",
                      MHQConstants.STRAT_CON_OROGENY_PROFILES_PATH);
                return fallback();
            }

            return new StratConOrogeny(sigmaOrDefault(library.gravitySigma(), DEFAULT_GRAVITY_SIGMA),
                  sigmaOrDefault(library.temperatureSigma(), DEFAULT_TEMPERATURE_SIGMA),
                  sigmaOrDefault(library.waterSigma(), DEFAULT_WATER_SIGMA),
                  validProfiles);
        } catch (IOException e) {
            LOGGER.error("Error reading orogeny profiles from {}; using a single default profile",
                  MHQConstants.STRAT_CON_OROGENY_PROFILES_PATH, e);
            return fallback();
        }
    }

    private static double sigmaOrDefault(Double sigma, double fallback) {
        return ((sigma == null) || (sigma <= 0.0)) ? fallback : sigma;
    }

    public double getGravitySigma() {
        return gravitySigma;
    }

    public double getTemperatureSigma() {
        return temperatureSigma;
    }

    public double getWaterSigma() {
        return waterSigma;
    }

    public List<OrogenyProfile> getProfiles() {
        return profiles;
    }

    private static double gaussian(double value, double center, double sigma) {
        double delta = value - center;
        return Math.exp(-(delta * delta) / (2.0 * sigma * sigma));
    }

    /**
     * Computes a profile's unnormalized weight for a planet: the geometric mean of the Gaussian factors for the
     * conditions the profile cares about, times its categorical multipliers for the planet's composition and
     * atmosphere. The geometric mean keeps profiles that weight more conditions from being penalized simply for caring
     * about more of them.
     *
     * @param profile          the profile to weight
     * @param planet           the planet's resolved data
     * @param gravitySigma     gravity standard deviation
     * @param temperatureSigma temperature standard deviation
     * @param waterSigma       water standard deviation
     *
     * @return the profile's weight; higher means more likely to be chosen
     */
    static double weight(OrogenyProfile profile, PlanetProfile planet, double gravitySigma, double temperatureSigma,
          double waterSigma) {
        double logSum = 0.0;
        int factors = 0;

        if (profile.gravityCenter() != null) {
            logSum += Math.log(gaussian(planet.gravity(), profile.gravityCenter(), gravitySigma));
            factors++;
        }
        if (profile.temperatureCenter() != null) {
            logSum += Math.log(gaussian(planet.temperatureCelsius(), profile.temperatureCenter(), temperatureSigma));
            factors++;
        }
        if (profile.waterCenter() != null) {
            logSum += Math.log(gaussian(planet.waterPercent(), profile.waterCenter(), waterSigma));
            factors++;
        }

        double weight = (factors == 0) ? 1.0 : Math.exp(logSum / factors);

        if (planet.hasRockyComposition()) {
            weight *= profile.rockyMultiplierOrDefault();
        }
        if (planet.hasIcyComposition()) {
            weight *= profile.icyMultiplierOrDefault();
        }
        if (planet.airless()) {
            weight *= profile.airlessMultiplierOrDefault();
        }

        return weight;
    }

    private double weightOf(OrogenyProfile profile, PlanetProfile planet) {
        return weight(profile, planet, gravitySigma, temperatureSigma, waterSigma);
    }

    /**
     * Picks an orogeny profile for the given planet, weighted by how well the planet matches each profile's
     * conditions.
     *
     * @param planet the planet's resolved data
     *
     * @return the chosen profile
     */
    public OrogenyProfile selectProfile(PlanetProfile planet) {
        double totalWeight = 0.0;
        for (OrogenyProfile profile : profiles) {
            totalWeight += weightOf(profile, planet);
        }

        if (totalWeight <= 0.0) {
            return profiles.getFirst();
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * totalWeight;
        double cumulative = 0.0;
        for (OrogenyProfile profile : profiles) {
            cumulative += weightOf(profile, planet);
            if (roll < cumulative) {
                return profile;
            }
        }

        return profiles.getLast();
    }
}
