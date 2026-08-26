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
import mekhq.MHQConstants;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;

/**
 * Supplies the two word pools used to build a contract's operation codename (for example, {@code RED} and {@code EAGLE}
 * in "Operation RED EAGLE"). One shared, objective-agnostic <em>descriptor</em> pool provides the leading modifier; a
 * per-{@link ContractObjectiveType} <em>noun</em> pool provides the head word, which is where the objective flavor
 * lives. A name is one weighted draw from the descriptor pool combined with one weighted draw from the objective's noun
 * pool, so a pool of {@code D} descriptors and {@code N} nouns yields {@code D * N} distinct names per objective.
 *
 * <p>Structured after {@link mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator}: a lazily created,
 * double-checked singleton whose pools are populated on background threads from {@code word,weight} CSV files (a base
 * file plus an optional {@code userdata/} override merged on top). The file paths are the explicit per-pool constants
 * in {@link MHQConstants}.</p>
 *
 * <p>Save File Formatting: {@code word,weight}. The word is a {@link String} that does not include a {@code ','};
 * weight is an integer weight used during generation. The first line of each file is a header and is skipped.</p>
 */
public class RandomOperationNameGenerator {
    //region Variable Declarations
    /** The shared, objective-agnostic pool of leading modifier words (e.g. RED, FALLEN, IRON). */
    private static WeightedIntMap<String> weightedDescriptor;

    /** The per-objective pools of head nouns (e.g. EAGLE, BULWARK). Keys cover every {@link ContractObjectiveType}. */
    private static final Map<ContractObjectiveType, WeightedIntMap<String>> weightedNouns =
          new EnumMap<>(ContractObjectiveType.class);

    private static volatile RandomOperationNameGenerator randomOperationNameGenerator;
    private static volatile boolean initialized = false;

    private static final MMLogger logger = MMLogger.create(RandomOperationNameGenerator.class);
    //endregion Variable Declarations

    //region Constructors
    private RandomOperationNameGenerator() {}
    //endregion Constructors

    //region Getters/Setters
    public static WeightedIntMap<String> getWeightedDescriptor() {
        return weightedDescriptor;
    }

    public static void setWeightedDescriptor(final WeightedIntMap<String> weightedDescriptor) {
        RandomOperationNameGenerator.weightedDescriptor = weightedDescriptor;
    }

    public static WeightedIntMap<String> getWeightedNoun(final ContractObjectiveType objectiveType) {
        return weightedNouns.get(objectiveType);
    }
    //endregion Getters/Setters

    /**
     * Returns the singleton instance, creating and kicking off initialization of every word pool on first use. Applies
     * the double-checked locking pattern so only one instance is ever created.
     *
     * @return the {@link RandomOperationNameGenerator} instance
     */
    //region Synchronization
    public static RandomOperationNameGenerator getInstance() {
        RandomOperationNameGenerator instance = randomOperationNameGenerator;
        if (instance == null) { // First check
            synchronized (RandomOperationNameGenerator.class) {
                instance = randomOperationNameGenerator;
                if (instance == null) { // Double check
                    instance = new RandomOperationNameGenerator();

                    // Pre-create every pool map under the lock, before any loader thread starts, so background threads
                    // only fill their own already-inserted map and never structurally modify the shared EnumMap
                    // concurrently.
                    setWeightedDescriptor(new WeightedIntMap<>());
                    for (final ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
                        weightedNouns.put(objectiveType, new WeightedIntMap<>());
                    }

                    instance.runDescriptorLoader();
                    for (final ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
                        instance.runNounLoader(objectiveType);
                    }

                    // Publish only after the fully constructed instance has kicked off initialization, so no other
                    // thread can observe a partially-initialized instance through the field.
                    randomOperationNameGenerator = instance;
                }
            }
        }
        return instance;
    }
    //endregion Synchronization

    //region Generation

    /**
     * Draws a random descriptor (the leading modifier word).
     *
     * @return a weighted-random descriptor, or {@code null} if the pool is not yet initialized or is empty
     */
    public @Nullable String generateDescriptor() {
        if (!initialized) {
            logger.warn("Attempted to generate an operation descriptor before the pools were initialized.");
            return null;
        }
        WeightedIntMap<String> pool = getWeightedDescriptor();
        return (pool == null) ? null : pool.randomItem();
    }

    /**
     * Draws a random noun (the head word) themed to the given objective. Falls back to the
     * {@link ContractObjectiveType#UNDEFINED} pool when the objective's own pool is empty (for example, when its data
     * file is missing).
     *
     * @param objectiveType the contract's objective, selecting the noun pool
     *
     * @return a weighted-random noun, or {@code null} if no pool is available or initialized
     */
    public @Nullable String generateNoun(final ContractObjectiveType objectiveType) {
        if (!initialized) {
            logger.warn("Attempted to generate an operation noun before the pools were initialized.");
            return null;
        }

        WeightedIntMap<String> pool = weightedNouns.get(objectiveType);
        String noun = (pool == null) ? null : pool.randomItem();
        if (noun != null) {
            return noun;
        }

        // Objective pool empty (e.g. missing data file) - fall back to the generic UNDEFINED pool.
        WeightedIntMap<String> fallbackPool = weightedNouns.get(ContractObjectiveType.UNDEFINED);
        return (fallbackPool == null) ? null : fallbackPool.randomItem();
    }
    //endregion Generation

    //region Initialization
    private void runDescriptorLoader() {
        Thread loader = new Thread(this::populateDescriptor, "Random Operation Name Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void runNounLoader(final ContractObjectiveType objectiveType) {
        Thread loader = new Thread(() -> populateNoun(objectiveType), "Random Operation Name Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void populateDescriptor() {
        final Map<String, Integer> nameSegments = new HashMap<>();
        loadNameSegments(new File(MHQConstants.OPERATION_NAME_DESCRIPTOR), nameSegments);
        loadNameSegments(new File(MHQConstants.OPERATION_NAME_DESCRIPTOR_USER), nameSegments);

        for (final Entry<String, Integer> entry : nameSegments.entrySet()) {
            getWeightedDescriptor().add(entry.getValue(), entry.getKey());
        }

        initialized = true;
    }

    private void populateNoun(final ContractObjectiveType objectiveType) {
        final String[] paths = nounFilePaths(objectiveType);

        final Map<String, Integer> nameSegments = new HashMap<>();
        loadNameSegments(new File(paths[0]), nameSegments);
        loadNameSegments(new File(paths[1]), nameSegments);

        final WeightedIntMap<String> pool = weightedNouns.get(objectiveType);
        for (final Entry<String, Integer> entry : nameSegments.entrySet()) {
            pool.add(entry.getValue(), entry.getKey());
        }

        initialized = true;
    }

    /**
     * Maps an objective to its explicit {@code {base, user}} noun-file path constants.
     *
     * @param objectiveType the objective whose noun-file paths are wanted
     *
     * @return a two-element array of {@code {basePath, userPath}}
     */
    static String[] nounFilePaths(final ContractObjectiveType objectiveType) {
        return switch (objectiveType) {
            case ASSASSINATION -> new String[] { MHQConstants.OPERATION_NAME_NOUN_ASSASSINATION,
                                                 MHQConstants.OPERATION_NAME_NOUN_ASSASSINATION_USER };
            case CADRE_DUTY -> new String[] { MHQConstants.OPERATION_NAME_NOUN_CADRE_DUTY,
                                              MHQConstants.OPERATION_NAME_NOUN_CADRE_DUTY_USER };
            case DIVERSIONARY_RAID -> new String[] { MHQConstants.OPERATION_NAME_NOUN_DIVERSIONARY_RAID,
                                                     MHQConstants.OPERATION_NAME_NOUN_DIVERSIONARY_RAID_USER };
            case ESPIONAGE -> new String[] { MHQConstants.OPERATION_NAME_NOUN_ESPIONAGE,
                                             MHQConstants.OPERATION_NAME_NOUN_ESPIONAGE_USER };
            case EXTRACTION_RAID -> new String[] { MHQConstants.OPERATION_NAME_NOUN_EXTRACTION_RAID,
                                                   MHQConstants.OPERATION_NAME_NOUN_EXTRACTION_RAID_USER };
            case GARRISON_DUTY -> new String[] { MHQConstants.OPERATION_NAME_NOUN_GARRISON_DUTY,
                                                 MHQConstants.OPERATION_NAME_NOUN_GARRISON_DUTY_USER };
            case GUERRILLA_WARFARE -> new String[] { MHQConstants.OPERATION_NAME_NOUN_GUERRILLA_WARFARE,
                                                     MHQConstants.OPERATION_NAME_NOUN_GUERRILLA_WARFARE_USER };
            case MOLE_HUNTING -> new String[] { MHQConstants.OPERATION_NAME_NOUN_MOLE_HUNTING,
                                                MHQConstants.OPERATION_NAME_NOUN_MOLE_HUNTING_USER };
            case OBJECTIVE_RAID -> new String[] { MHQConstants.OPERATION_NAME_NOUN_OBJECTIVE_RAID,
                                                  MHQConstants.OPERATION_NAME_NOUN_OBJECTIVE_RAID_USER };
            case OBSERVATION_RAID -> new String[] { MHQConstants.OPERATION_NAME_NOUN_OBSERVATION_RAID,
                                                    MHQConstants.OPERATION_NAME_NOUN_OBSERVATION_RAID_USER };
            case PIRATE_HUNTING -> new String[] { MHQConstants.OPERATION_NAME_NOUN_PIRATE_HUNTING,
                                                  MHQConstants.OPERATION_NAME_NOUN_PIRATE_HUNTING_USER };
            case PLANETARY_ASSAULT -> new String[] { MHQConstants.OPERATION_NAME_NOUN_PLANETARY_ASSAULT,
                                                     MHQConstants.OPERATION_NAME_NOUN_PLANETARY_ASSAULT_USER };
            case RECON_RAID -> new String[] { MHQConstants.OPERATION_NAME_NOUN_RECON_RAID,
                                              MHQConstants.OPERATION_NAME_NOUN_RECON_RAID_USER };
            case RELIEF_DUTY -> new String[] { MHQConstants.OPERATION_NAME_NOUN_RELIEF_DUTY,
                                               MHQConstants.OPERATION_NAME_NOUN_RELIEF_DUTY_USER };
            case RETAINER -> new String[] { MHQConstants.OPERATION_NAME_NOUN_RETAINER,
                                            MHQConstants.OPERATION_NAME_NOUN_RETAINER_USER };
            case RIOT_DUTY -> new String[] { MHQConstants.OPERATION_NAME_NOUN_RIOT_DUTY,
                                             MHQConstants.OPERATION_NAME_NOUN_RIOT_DUTY_USER };
            case SABOTAGE -> new String[] { MHQConstants.OPERATION_NAME_NOUN_SABOTAGE,
                                            MHQConstants.OPERATION_NAME_NOUN_SABOTAGE_USER };
            case SECURITY_DUTY -> new String[] { MHQConstants.OPERATION_NAME_NOUN_SECURITY_DUTY,
                                                 MHQConstants.OPERATION_NAME_NOUN_SECURITY_DUTY_USER };
            case TERRORISM -> new String[] { MHQConstants.OPERATION_NAME_NOUN_TERRORISM,
                                             MHQConstants.OPERATION_NAME_NOUN_TERRORISM_USER };
            case UNDEFINED -> new String[] { MHQConstants.OPERATION_NAME_NOUN_UNDEFINED,
                                             MHQConstants.OPERATION_NAME_NOUN_UNDEFINED_USER };
        };
    }

    /**
     * Loads {@code word,weight} rows from the given file into the provided map, skipping the header line. A missing
     * file is silently ignored (this is how the optional {@code userdata/} override is handled).
     *
     * @param file         the CSV file to read
     * @param nameSegments the map to populate with the loaded {@code word -> weight} entries
     */
    private void loadNameSegments(final File file, final Map<String, Integer> nameSegments) {
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
                String[] values = input.nextLine().split(",");
                if (values.length == 2) {
                    nameSegments.put(values[0], Integer.parseInt(values[1]));
                } else if (values.length < 2) {
                    logger.error("Not enough fields in {} on {}", file, lineNumber);
                } else {
                    logger.error("Too many fields in {} on {}", file, lineNumber);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to populate operation name from {}", file, e);
        }
    }
    //endregion Initialization

    //region Testing

    /**
     * Test seam: installs pre-built word pools and publishes a ready singleton, bypassing the background file loaders
     * so generation is deterministic and file-system independent. Pair with {@link #resetForTesting()} in test
     * teardown.
     *
     * @param descriptor the descriptor pool to install (may be {@code null} to simulate an unavailable pool)
     * @param nouns      the per-objective noun pools to install (missing keys behave like empty pools)
     *
     * @return the seeded singleton instance
     */
    static RandomOperationNameGenerator createForTesting(final @Nullable WeightedIntMap<String> descriptor,
          final @Nullable Map<ContractObjectiveType, WeightedIntMap<String>> nouns) {
        final RandomOperationNameGenerator instance = new RandomOperationNameGenerator();
        weightedDescriptor = descriptor;
        weightedNouns.clear();
        if (nouns != null) {
            weightedNouns.putAll(nouns);
        }
        initialized = true;
        randomOperationNameGenerator = instance;
        return instance;
    }

    /**
     * Test seam: clears all singleton state so the next {@link #getInstance()} rebuilds from scratch. Call in test
     * teardown to avoid leaking seeded pools into other tests.
     */
    static void resetForTesting() {
        randomOperationNameGenerator = null;
        weightedDescriptor = null;
        weightedNouns.clear();
        initialized = false;
    }
    //endregion Testing
}
