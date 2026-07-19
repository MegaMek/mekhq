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
 * Loads the hydrology profiles used by improved sector generation from {@code HydrologyProfiles.yaml} and picks one for
 * a planet based on its water coverage.
 *
 * <p>Selection is Gaussian-weighted: every profile's weight falls off with the distance between the planet's water
 * coverage and the profile's {@link HydrologyProfile#gaussianCenter()}, using a shared standard deviation
 * ({@code sigma}). A drier or wetter planet therefore skews toward the matching profiles, but neighboring profiles
 * still appear for variety. A larger {@code sigma} means more variety.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConHydrology {
    private static final MMLogger LOGGER = MMLogger.create(StratConHydrology.class);

    /** Standard deviation (in water-coverage percentage points) used if the YAML omits or zeroes it. */
    public static final double DEFAULT_SIGMA = 18.0;

    /** The profile fallen back to when the YAML is missing, empty, or unreadable. */
    private static final HydrologyProfile DEFAULT_PROFILE = new HydrologyProfile(HydrologyProfileType.COASTAL,
          25,
          45,
          35.0);

    /** A constant used to define the level of granularity or precision when selecting a hydrology profile. */
    private static final double PICK_RESOLUTION = 1000000.0;

    private final double sigma;
    private final List<HydrologyProfile> profiles;

    private static StratConHydrology instance;

    /** The root object of {@code HydrologyProfiles.yaml}. */
    public record Library(double sigma, List<HydrologyProfile> profiles) {}

    private StratConHydrology(double sigma, List<HydrologyProfile> profiles) {
        this.sigma = sigma;
        this.profiles = profiles;
    }

    /**
     * @return the singleton hydrology instance, loading it from the YAML on first use and falling back to a single
     *       default profile if the file cannot be read
     */
    public static StratConHydrology getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static StratConHydrology load() {
        File file = new File(MHQConstants.STRAT_CON_HYDROLOGY_PROFILES_PATH);
        if (!file.exists()) {
            LOGGER.warn("Hydrology profiles file {} does not exist; using a single default profile",
                  MHQConstants.STRAT_CON_HYDROLOGY_PROFILES_PATH);
            return new StratConHydrology(DEFAULT_SIGMA, List.of(DEFAULT_PROFILE));
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            Library library = mapper.readValue(file, Library.class);
            if ((library == null) || (library.profiles() == null) || library.profiles().isEmpty()) {
                LOGGER.warn("Hydrology profiles file {} held no profiles; using a single default profile",
                      MHQConstants.STRAT_CON_HYDROLOGY_PROFILES_PATH);
                return new StratConHydrology(DEFAULT_SIGMA, List.of(DEFAULT_PROFILE));
            }

            // Drop any entries whose type failed to parse (READ_UNKNOWN_ENUM_VALUES_AS_NULL leaves them null).
            List<HydrologyProfile> validProfiles = library.profiles()
                                                         .stream()
                                                         .filter(profile -> profile.type() != null)
                                                         .toList();
            if (validProfiles.isEmpty()) {
                LOGGER.warn("Hydrology profiles file {} held no recognizable profiles; using a single default profile",
                      MHQConstants.STRAT_CON_HYDROLOGY_PROFILES_PATH);
                return new StratConHydrology(DEFAULT_SIGMA, List.of(DEFAULT_PROFILE));
            }

            double sigma = (library.sigma() > 0.0) ? library.sigma() : DEFAULT_SIGMA;
            return new StratConHydrology(sigma, validProfiles);
        } catch (IOException e) {
            LOGGER.error("Error reading hydrology profiles from {}; using a single default profile",
                  MHQConstants.STRAT_CON_HYDROLOGY_PROFILES_PATH, e);
            return new StratConHydrology(DEFAULT_SIGMA, List.of(DEFAULT_PROFILE));
        }
    }

    /**
     * @return the standard deviation used for Gaussian profile weighting
     */
    public double getSigma() {
        return sigma;
    }

    /**
     * @return the loaded hydrology profiles
     */
    public List<HydrologyProfile> getProfiles() {
        return profiles;
    }

    /**
     * Computes a profile's unnormalized Gaussian weight for a given planetary water coverage.
     *
     * @param profile      the profile to weight
     * @param waterPercent the planet's water coverage, as a percentage
     * @param sigma        the standard deviation, in percentage points
     *
     * @return the profile's weight; higher means more likely to be chosen
     */
    static double weight(HydrologyProfile profile, int waterPercent, double sigma) {
        double delta = waterPercent - profile.gaussianCenter();
        return Math.exp(-(delta * delta) / (2.0 * sigma * sigma));
    }

    /**
     * Picks a hydrology profile for the given planetary water coverage, Gaussian-weighted toward the profiles whose
     * center is nearest.
     *
     * @param waterPercent the planet's water coverage, as a percentage
     *
     * @return the chosen profile
     */
    public HydrologyProfile selectProfile(int waterPercent) {
        double totalWeight = 0.0;
        for (HydrologyProfile profile : profiles) {
            totalWeight += weight(profile, waterPercent, sigma);
        }

        if (totalWeight <= 0.0) {
            return profiles.get(0);
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * totalWeight;
        double cumulative = 0.0;
        for (HydrologyProfile profile : profiles) {
            cumulative += weight(profile, waterPercent, sigma);
            if (roll < cumulative) {
                return profile;
            }
        }

        return profiles.get(profiles.size() - 1);
    }

    /**
     * Rolls an actual ocean coverage uniformly within the profile's band.
     *
     * @param profile the chosen profile
     *
     * @return the ocean coverage percentage for this sector
     */
    public int rollOceanPercent(HydrologyProfile profile) {
        int min = profile.minOceanPercent();
        int max = profile.maxOceanPercent();
        if (max <= min) {
            return min;
        }
        return min + Compute.randomInt(max - min + 1);
    }
}
