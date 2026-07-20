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
 * Loads the urban (settlement) profiles used by improved sector generation from {@code UrbanProfiles.yaml} and picks
 * one for a planet from its combined conditions.
 *
 * <p>Selection is multi-factor, like orogeny: a profile's weight is the geometric mean of the Gaussian factors for
 * population ({@code log10}), water coverage, habitability, and technology level, each skipped when the profile is
 * indifferent to it. The geometric mean keeps profiles that weight more conditions from being penalized for it.
 * Neighboring profiles still appear for variety; larger sigmas mean more variety.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConUrban {
    private static final MMLogger LOGGER = MMLogger.create(StratConUrban.class);

    /** Default Gaussian standard deviations (in each condition's own units) if the YAML omits them. */
    public static final double DEFAULT_POPULATION_SIGMA = 1.5;
    public static final double DEFAULT_WATER_SIGMA = 20.0;
    public static final double DEFAULT_HABITABILITY_SIGMA = 0.3;
    public static final double DEFAULT_TECH_SIGMA = 0.3;

    /** The profile fallen back to when the YAML is missing, empty, or unreadable: indifferent to everything. */
    private static final UrbanProfile DEFAULT_PROFILE = new UrbanProfile(UrbanProfileType.DISPERSED,
          null, null, null, null, null, null, null, null, null);

    private static final double PICK_RESOLUTION = 1_000_000.0;

    private final double populationSigma;
    private final double waterSigma;
    private final double habitabilitySigma;
    private final double techSigma;
    private final List<UrbanProfile> profiles;

    private static StratConUrban instance;

    /** The root object of {@code UrbanProfiles.yaml}. */
    public record Library(Double populationSigma, Double waterSigma, Double habitabilitySigma, Double techSigma,
          List<UrbanProfile> profiles) {}

    private StratConUrban(double populationSigma, double waterSigma, double habitabilitySigma, double techSigma,
          List<UrbanProfile> profiles) {
        this.populationSigma = populationSigma;
        this.waterSigma = waterSigma;
        this.habitabilitySigma = habitabilitySigma;
        this.techSigma = techSigma;
        this.profiles = profiles;
    }

    /**
     * @return the singleton urban instance, loading it from the YAML on first use and falling back to a single
     *       indifferent default profile if the file cannot be read
     */
    public static StratConUrban getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static StratConUrban fallback() {
        return new StratConUrban(DEFAULT_POPULATION_SIGMA,
              DEFAULT_WATER_SIGMA,
              DEFAULT_HABITABILITY_SIGMA,
              DEFAULT_TECH_SIGMA,
              List.of(DEFAULT_PROFILE));
    }

    /**
     * Test seam: loads the urban profiles from an explicit path and installs the result as the singleton.
     *
     * <p>Production resolves {@link MHQConstants#STRAT_CON_URBAN_PROFILES_PATH}, which lives under the {@code data}
     * directory that is built when the application launches. That directory does not exist in the test environment, so
     * tests point this at their own copy under {@code testresources} instead.</p>
     *
     * @param path the file to load the urban profiles from
     */
    public static void loadForTest(String path) {
        instance = load(path);
    }

    private static StratConUrban load() {
        return load(MHQConstants.STRAT_CON_URBAN_PROFILES_PATH);
    }

    private static StratConUrban load(String path) {
        File file = new File(path);
        if (!file.exists()) {
            LOGGER.warn("Urban profiles file {} does not exist; using a single default profile",
                  path);
            return fallback();
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            Library library = mapper.readValue(file, Library.class);
            if ((library == null) || (library.profiles() == null) || library.profiles().isEmpty()) {
                LOGGER.warn("Urban profiles file {} held no profiles; using a single default profile",
                      path);
                return fallback();
            }

            // Drop any entries whose type failed to parse (READ_UNKNOWN_ENUM_VALUES_AS_NULL leaves them null).
            List<UrbanProfile> validProfiles = library.profiles()
                                                     .stream()
                                                     .filter(profile -> profile.type() != null)
                                                     .toList();
            if (validProfiles.isEmpty()) {
                LOGGER.warn("Urban profiles file {} held no recognizable profiles; using a single default profile",
                      path);
                return fallback();
            }

            return new StratConUrban(sigmaOrDefault(library.populationSigma(), DEFAULT_POPULATION_SIGMA),
                  sigmaOrDefault(library.waterSigma(), DEFAULT_WATER_SIGMA),
                  sigmaOrDefault(library.habitabilitySigma(), DEFAULT_HABITABILITY_SIGMA),
                  sigmaOrDefault(library.techSigma(), DEFAULT_TECH_SIGMA),
                  validProfiles);
        } catch (IOException e) {
            LOGGER.error("Error reading urban profiles from {}; using a single default profile",
                  path, e);
            return fallback();
        }
    }

    private static double sigmaOrDefault(Double sigma, double fallback) {
        return ((sigma == null) || (sigma <= 0.0)) ? fallback : sigma;
    }

    public double getPopulationSigma() {
        return populationSigma;
    }

    public double getWaterSigma() {
        return waterSigma;
    }

    public double getHabitabilitySigma() {
        return habitabilitySigma;
    }

    public double getTechSigma() {
        return techSigma;
    }

    public List<UrbanProfile> getProfiles() {
        return profiles;
    }

    private static double gaussian(double value, double center, double sigma) {
        double delta = value - center;
        return Math.exp(-(delta * delta) / (2.0 * sigma * sigma));
    }

    /**
     * Computes a profile's unnormalized weight for a planet: the product of Gaussian factors for the conditions the
     * profile cares about.
     *
     * @param profile           the profile to weight
     * @param planet            the planet's resolved data
     * @param populationSigma   population (log10) standard deviation
     * @param waterSigma        water standard deviation
     * @param habitabilitySigma habitability standard deviation
     * @param techSigma         technology standard deviation
     *
     * @return the profile's weight; higher means more likely to be chosen
     */
    static double weight(UrbanProfile profile, PlanetProfile planet, double populationSigma, double waterSigma,
          double habitabilitySigma, double techSigma) {
        double logSum = 0.0;
        int factors = 0;

        if (profile.populationCenter() != null) {
            logSum += Math.log(gaussian(planet.populationLog(), profile.populationCenter(), populationSigma));
            factors++;
        }
        if (profile.waterCenter() != null) {
            logSum += Math.log(gaussian(planet.waterPercent(), profile.waterCenter(), waterSigma));
            factors++;
        }
        if (profile.habitabilityCenter() != null) {
            logSum += Math.log(gaussian(planet.habitability(), profile.habitabilityCenter(), habitabilitySigma));
            factors++;
        }
        if (profile.techCenter() != null) {
            logSum += Math.log(gaussian(planet.techLevel(), profile.techCenter(), techSigma));
            factors++;
        }

        // Geometric mean of the per-condition Gaussians, so a profile that weights more conditions is not penalized for
        // it. A profile indifferent to everything scores a neutral 1.0.
        return (factors == 0) ? 1.0 : Math.exp(logSum / factors);
    }

    private double weightOf(UrbanProfile profile, PlanetProfile planet) {
        return weight(profile, planet, populationSigma, waterSigma, habitabilitySigma, techSigma);
    }

    /**
     * Picks an urban profile for the given planet, weighted by how well the planet matches each profile's conditions.
     *
     * @param planet the planet's resolved data
     *
     * @return the chosen profile
     */
    public UrbanProfile selectProfile(PlanetProfile planet) {
        double totalWeight = 0.0;
        for (UrbanProfile profile : profiles) {
            totalWeight += weightOf(profile, planet);
        }

        if (totalWeight <= 0.0) {
            return profiles.getFirst();
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * totalWeight;
        double cumulative = 0.0;
        for (UrbanProfile profile : profiles) {
            cumulative += weightOf(profile, planet);
            if (roll < cumulative) {
                return profile;
            }
        }

        return profiles.getLast();
    }
}
