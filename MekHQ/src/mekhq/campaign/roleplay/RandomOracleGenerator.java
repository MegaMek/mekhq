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
package mekhq.campaign.roleplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import megamek.common.annotations.Nullable;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;

/**
 * Supplies the weighted meaning pools used by the solo-roleplay oracle system, one pool per {@link OracleTable}.
 *
 * <p>Structured after {@link mekhq.campaign.mission.contract.contractGeneration.RandomOperationNameGenerator}: a
 * lazily created, double-checked singleton whose pools are populated on a background thread from {@code meaning,weight}
 * CSV files (a base file plus an optional {@code userdata/} override merged on top).</p>
 *
 * <p>Save File Formatting: {@code meaning,weight}. The meaning is a {@link String} that does not include a
 * {@code ','}; weight is an integer weight used during generation. The first line of each file is a header and is
 * skipped.</p>
 */
public class RandomOracleGenerator {
    //region Variable Declarations
    /** The per-table pools of meanings. Keys cover every {@link OracleTable}. */
    private static final Map<OracleTable, WeightedIntMap<String>> weightedMeanings = new EnumMap<>(OracleTable.class);

    private static volatile RandomOracleGenerator randomOracleGenerator;
    private static volatile boolean initialized = false;

    private static final MMLogger logger = MMLogger.create(RandomOracleGenerator.class);
    //endregion Variable Declarations

    //region Constructors
    private RandomOracleGenerator() {}
    //endregion Constructors

    //region Getters/Setters
    public static WeightedIntMap<String> getWeightedMeanings(final OracleTable table) {
        return weightedMeanings.get(table);
    }

    public static boolean isInitialized() {
        return initialized;
    }
    //endregion Getters/Setters

    /**
     * Returns the singleton instance, creating and kicking off initialization of every oracle pool on first use.
     * Applies the double-checked locking pattern so only one instance is ever created.
     *
     * @return the {@link RandomOracleGenerator} instance
     */
    //region Synchronization
    public static RandomOracleGenerator getInstance() {
        RandomOracleGenerator instance = randomOracleGenerator;
        if (instance == null) { // First check
            synchronized (RandomOracleGenerator.class) {
                instance = randomOracleGenerator;
                if (instance == null) { // Double check
                    instance = new RandomOracleGenerator();

                    // Pre-create every pool map under the lock, before the loader thread starts, so the background
                    // thread only fills already-inserted maps and never structurally modifies the shared EnumMap.
                    for (final OracleTable table : OracleTable.values()) {
                        weightedMeanings.put(table, new WeightedIntMap<>());
                    }

                    instance.runLoader();

                    // Publish only after the fully constructed instance has kicked off initialization, so no other
                    // thread can observe a partially-initialized instance through the field.
                    randomOracleGenerator = instance;
                }
            }
        }
        return instance;
    }
    //endregion Synchronization

    //region Generation

    /**
     * Draws a random meaning from the given oracle table.
     *
     * @param table the oracle table to consult
     *
     * @return a weighted-random meaning, or {@code null} if the pools are not yet initialized or the table is empty
     */
    public @Nullable String generate(final OracleTable table) {
        if (!initialized) {
            logger.warn("Attempted to consult oracle table {} before the pools were initialized.", table);
            return null;
        }

        WeightedIntMap<String> pool = weightedMeanings.get(table);
        return (pool == null) ? null : pool.randomItem();
    }
    //endregion Generation

    //region Initialization
    private void runLoader() {
        Thread loader = new Thread(this::populateAll, "Random Oracle Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void populateAll() {
        for (final OracleTable table : OracleTable.values()) {
            populate(table);
        }

        initialized = true;
    }

    private void populate(final OracleTable table) {
        final Map<String, Integer> meanings = new HashMap<>();
        loadMeanings(new File(table.getFilePath()), meanings);
        loadMeanings(new File(table.getUserFilePath()), meanings);

        if (meanings.isEmpty()) {
            logger.warn("No meanings loaded for oracle table {}", table);
        }

        final WeightedIntMap<String> pool = weightedMeanings.get(table);
        for (final Entry<String, Integer> entry : meanings.entrySet()) {
            pool.add(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Loads {@code meaning,weight} rows from the given file into the provided map, skipping the header line. A
     * missing file is silently ignored (this is how the optional {@code userdata/} override is handled).
     *
     * @param file     the CSV file to read
     * @param meanings the map to populate with the loaded {@code meaning -> weight} entries
     */
    static void loadMeanings(final File file, final Map<String, Integer> meanings) {
        if (!file.exists()) {
            return;
        }

        int lineNumber = 0;

        try (InputStream is = new FileInputStream(file);
              Scanner input = new Scanner(is, StandardCharsets.UTF_8)) {
            // skip the first line, as that's the header
            lineNumber++;
            input.nextLine();

            while (input.hasNextLine()) {
                lineNumber++;
                String line = input.nextLine();
                if (line.isBlank()) {
                    continue;
                }

                String[] values = line.split(",");
                if (values.length == 2) {
                    meanings.put(values[0].trim(), Integer.parseInt(values[1].trim()));
                } else if (values.length < 2) {
                    logger.error("Not enough fields in {} on {}", file, lineNumber);
                } else {
                    logger.error("Too many fields in {} on {}", file, lineNumber);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to populate oracle table from {}", file, e);
        }
    }
    //endregion Initialization

    //region Testing

    /**
     * Test seam: installs pre-built oracle pools and publishes a ready singleton, bypassing the background file loader
     * so generation is deterministic and file-system independent. Pair with {@link #resetForTesting()} in test
     * teardown.
     *
     * @param pools the per-table pools to install (missing keys behave like empty pools)
     *
     * @return the seeded singleton instance
     */
    static RandomOracleGenerator createForTesting(final @Nullable Map<OracleTable, WeightedIntMap<String>> pools) {
        final RandomOracleGenerator instance = new RandomOracleGenerator();
        weightedMeanings.clear();
        if (pools != null) {
            weightedMeanings.putAll(pools);
        }
        initialized = true;
        randomOracleGenerator = instance;
        return instance;
    }

    /**
     * Test seam: clears all singleton state so the next {@link #getInstance()} rebuilds from scratch. Call in test
     * teardown to avoid leaking seeded pools into other tests.
     */
    static void resetForTesting() {
        randomOracleGenerator = null;
        weightedMeanings.clear();
        initialized = false;
    }
    //endregion Testing
}
