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
 * Loads the sector shape profiles used by improved sector generation from {@code SectorShapeProfiles.yaml} and picks
 * one for each generated sector.
 *
 * <p>Selection is a plain weighted roll, not a fit against planetary data. A sector's <em>area</em> already follows
 * the planet (scouting budget, world size, water coverage); its proportions are free, and varying them keeps two
 * contracts on similar worlds from producing identically shaped maps.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSectorShape {
    private static final MMLogger LOGGER = MMLogger.create(StratConSectorShape.class);

    /** The profile fallen back to when the YAML is missing, empty, or unreadable: a plain square sector. */
    private static final SectorShapeProfile DEFAULT_PROFILE = new SectorShapeProfile(SectorShapeProfileType.SQUARE,
          1.0,
          1.0);

    /** Granularity of the weighted roll. */
    private static final double PICK_RESOLUTION = 1_000_000.0;

    private final List<SectorShapeProfile> profiles;

    private static StratConSectorShape instance;

    /**
     * The root object of {@code SectorShapeProfiles.yaml}.
     *
     * @param profiles the authored sector shape profiles to pick between
     */
    public record Library(List<SectorShapeProfile> profiles) {}

    private StratConSectorShape(List<SectorShapeProfile> profiles) {
        this.profiles = profiles;
    }

    /**
     * @return the singleton shape instance, loading it from the YAML on first use and falling back to a single square
     *       profile if the file cannot be read
     */
    public static StratConSectorShape getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * Test seam: loads the sector shape profiles from an explicit path and installs the result as the singleton.
     *
     * <p>Production resolves {@link MHQConstants#STRAT_CON_SECTOR_SHAPE_PROFILES_PATH}, which lives under the
     * {@code data} directory that is built when the application launches. That directory does not exist in the test
     * environment, so tests point this at their own copy under {@code testresources} instead.</p>
     *
     * @param path the file to load the sector shape profiles from
     */
    public static void loadForTest(String path) {
        instance = load(path);
    }

    private static StratConSectorShape load() {
        return load(MHQConstants.STRAT_CON_SECTOR_SHAPE_PROFILES_PATH);
    }

    private static StratConSectorShape load(String path) {
        File file = new File(path);
        if (!file.exists()) {
            LOGGER.warn("Sector shape profiles file {} does not exist; sectors will be square",
                  path);
            return new StratConSectorShape(List.of(DEFAULT_PROFILE));
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            Library library = mapper.readValue(file, Library.class);
            if ((library == null) || (library.profiles() == null) || library.profiles().isEmpty()) {
                LOGGER.warn("Sector shape profiles file {} held no profiles; sectors will be square",
                      path);
                return new StratConSectorShape(List.of(DEFAULT_PROFILE));
            }

            // Drop any entries whose type failed to parse (READ_UNKNOWN_ENUM_VALUES_AS_NULL leaves them null).
            List<SectorShapeProfile> validProfiles = library.profiles()
                                                           .stream()
                                                           .filter(profile -> profile.type() != null)
                                                           .toList();
            if (validProfiles.isEmpty()) {
                LOGGER.warn("Sector shape profiles file {} held no recognizable profiles; sectors will be square",
                      path);
                return new StratConSectorShape(List.of(DEFAULT_PROFILE));
            }

            return new StratConSectorShape(validProfiles);
        } catch (IOException e) {
            LOGGER.error("Error reading sector shape profiles from {}; sectors will be square",
                  path, e);
            return new StratConSectorShape(List.of(DEFAULT_PROFILE));
        }
    }

    /**
     * @return the loaded shape profiles
     */
    public List<SectorShapeProfile> getProfiles() {
        return profiles;
    }

    /**
     * Picks a sector shape at random, weighted by each profile's {@code weight}.
     *
     * @return the chosen profile
     */
    public SectorShapeProfile selectProfile() {
        double totalWeight = 0.0;
        for (SectorShapeProfile profile : profiles) {
            totalWeight += profile.weightOrDefault();
        }

        if (totalWeight <= 0.0) {
            return profiles.get(0);
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * totalWeight;
        double cumulative = 0.0;
        for (SectorShapeProfile profile : profiles) {
            cumulative += profile.weightOrDefault();
            if (roll < cumulative) {
                return profile;
            }
        }

        return profiles.get(profiles.size() - 1);
    }
}
