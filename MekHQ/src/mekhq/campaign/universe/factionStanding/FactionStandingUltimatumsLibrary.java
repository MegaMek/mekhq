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
package mekhq.campaign.universe.factionStanding;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Manages a library of {@link FactionStandingUltimatumData} objects loaded from a YAML file.
 *
 * <p>This class provides loading, parsing, and retrieval utilities for the Faction Standing ultimatums, mapping each
 * ultimatum by its {@link LocalDate}.</p>
 *
 * <p>Data is loaded from {@code data/universe/factionStandingUltimatums.yml} upon instantiation.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingUltimatumsLibrary {
    private static final String DIRECTORY = "data" + File.separator + "universe" + File.separator;
    private static final String EXTENSION = ".yml";
    private static final String ULTIMATUMS_FILE = DIRECTORY + "factionStandingUltimatums" + EXTENSION;

    /** Map storing Faction Standing ultimatums, keyed by their date. */
    private final Map<LocalDate, FactionStandingUltimatumData> ultimatumMap = new HashMap<>();

    /**
     * Constructs a new {@link FactionStandingUltimatumsLibrary} and loads the ultimatums from the YAML data file.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingUltimatumsLibrary() {
        loadUltimatums();
    }

    /**
     * Returns an unmodifiable map of all loaded Faction Standing ultimatums.
     *
     * <p>The map is keyed by {@link LocalDate} and values are {@link FactionStandingUltimatumData} instances.</p>
     *
     * @return unmodifiable map of ultimatums by date
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<LocalDate, FactionStandingUltimatumData> getUltimatums() {
        return Collections.unmodifiableMap(ultimatumMap);
    }

    /**
     * Loads Faction Standing ultimatums from the YAML file, parsing them into the internal ultimatumMap.
     *
     * <p> Any IO or parsing errors will result in a {@link RuntimeException}.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void loadUltimatums() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            FactionStandingUltimatumsWrapper wrapper = mapper.readValue(
                  new File(ULTIMATUMS_FILE),
                  FactionStandingUltimatumsWrapper.class
            );

            for (FactionStandingUltimatumData data : wrapper.getUltimatums()) {
                LocalDate date = data.getDate();
                ultimatumMap.put(date, data);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read ultimatums YAML: " + ULTIMATUMS_FILE, e);
        }
    }
}
