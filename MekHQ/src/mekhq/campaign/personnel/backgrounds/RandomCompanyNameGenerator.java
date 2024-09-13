/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.backgrounds;

import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;


/**
 * Save File Formatting:
 * word, weight
 * word is a String that does not include a ','
 * Weight is an integer weight used during generation
 */
public class RandomCompanyNameGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 4721410214327210288L;
    private static final int NAME_MIDDLE_WORD_CORPORATE = 0;
    private static final int NAME_END_WORD_CORPORATE = 1;
    private static final int NAME_MIDDLE_WORD_MERCENARY = 2;
    private static final int NAME_END_WORD_MERCENARY = 3;
    private static final int NAME_PRE_FAB = 4;

    private static WeightedIntMap<String> weightedMiddleWordCorporate;
    private static WeightedIntMap<String> weightedEndWordCorporate;
    private static WeightedIntMap<String> weightedMiddleWordMercenary;
    private static WeightedIntMap<String> weightedEndWordMercenary;
    private static WeightedIntMap<String> weightedPreFab;

    private static volatile RandomCompanyNameGenerator randomCompanyNameGenerator;
    private static volatile boolean initialized = false;

    private static final MMLogger logger = MMLogger.create(RandomCompanyNameGenerator.class);
    //endregion Variable Declarations

    //region Constructors
    private RandomCompanyNameGenerator() {}
    //endregion Constructors

    //region Getters/Setters
    public static WeightedIntMap<String> getWeightedMiddleWordCorporate() {
        return weightedMiddleWordCorporate;
    }

    public static void setWeightedMiddleWordCorporate(final WeightedIntMap<String> weightedMiddleWordCorporate) {
        RandomCompanyNameGenerator.weightedMiddleWordCorporate = weightedMiddleWordCorporate;
    }

    public static WeightedIntMap<String> getWeightedEndWordCorporate() {
        return weightedEndWordCorporate;
    }

    public static void setWeightedEndWordCorporate(final WeightedIntMap<String> weightedEndWordCorporate) {
        RandomCompanyNameGenerator.weightedEndWordCorporate = weightedEndWordCorporate;
    }

    public static WeightedIntMap<String> getWeightedMiddleWordMercenary() {
        return weightedMiddleWordMercenary;
    }

    public static void setWeightedMiddleWordMercenary(final WeightedIntMap<String> weightedMiddleWordMercenary) {
        RandomCompanyNameGenerator.weightedMiddleWordMercenary = weightedMiddleWordMercenary;
    }

    public static WeightedIntMap<String> getWeightedEndWordMercenary() {
        return weightedEndWordMercenary;
    }

    public static void setWeightedEndWordMercenary(final WeightedIntMap<String> weightedEndWordMercenary) {
        RandomCompanyNameGenerator.weightedEndWordMercenary = weightedEndWordMercenary;
    }

    public static WeightedIntMap<String> getWeightedPreFab() {
        return weightedPreFab;
    }

    public static void setWeightedPreFab(final WeightedIntMap<String> weightedPreFab) {
        RandomCompanyNameGenerator.weightedPreFab = weightedPreFab;
    }
    //endregion Getters/Setters

    /**
     * Returns an instance of the RandomCompanyNameGenerator class. The method applies the double-check locking
     * pattern to ensure that only one instance of the class is created.
     *
     * @return The instance of RandomCompanyNameGenerator. If the instance does not exist, it creates a new
     * instance and initializes it by running the thread loader for various origin values.
     */
    //region Synchronization
    public static RandomCompanyNameGenerator getInstance() {
        if (randomCompanyNameGenerator == null) { // First check
            synchronized (RandomCompanyNameGenerator.class) {
                if (randomCompanyNameGenerator == null) { // Double check
                    randomCompanyNameGenerator = new RandomCompanyNameGenerator();
                    randomCompanyNameGenerator.runThreadLoader(NAME_MIDDLE_WORD_CORPORATE);
                    randomCompanyNameGenerator.runThreadLoader(NAME_END_WORD_CORPORATE);
                    randomCompanyNameGenerator.runThreadLoader(NAME_MIDDLE_WORD_MERCENARY);
                    randomCompanyNameGenerator.runThreadLoader(NAME_END_WORD_MERCENARY);
                    randomCompanyNameGenerator.runThreadLoader(NAME_PRE_FAB);
                }
            }
        }
        return randomCompanyNameGenerator;
    }
    //endregion Synchronization

    /**
     * Generates a random company name segment based on the given origin.
     *
     * @param origin The origin of the company name. Possible values are:
     *               - NAME_MIDDLE_WORD_CORPORATE (0)
     *               - NAME_END_WORD_CORPORATE (1)
     *               - NAME_MIDDLE_WORD_MERCENARY (2)
     *               - NAME_END_WORD_MERCENARY (3)
     *               - NAME_PRE_FAB (4)
     * @return The generated name segment as a string. If the list of company name segments is not initialized,
     *         it returns an empty string.
     * @throws IllegalStateException if the given origin value is unexpected
     */
    //region Generation
    public String generate(int origin) {
        if (initialized) {
            return switch (origin) {
                case NAME_MIDDLE_WORD_CORPORATE -> getWeightedMiddleWordCorporate().randomItem();
                case NAME_END_WORD_CORPORATE -> getWeightedEndWordCorporate().randomItem();
                case NAME_MIDDLE_WORD_MERCENARY -> getWeightedMiddleWordMercenary().randomItem();
                case NAME_END_WORD_MERCENARY -> getWeightedEndWordMercenary().randomItem();
                case NAME_PRE_FAB -> getWeightedPreFab().randomItem();
                default -> throw new IllegalStateException("Unexpected value: " + origin);
            };
        } else {
            logger.warn("Attempted to generate a company name before the list was initialized.");
            return "";
        }
    }
    //endregion Generation

    /**
     * Runs a thread loader for populating company name segments based on the given origin.
     *
     * @param origin The origin of the company name segments. Possible values are:
     *               - NAME_MIDDLE_WORD_CORPORATE (0)
     *               - NAME_END_WORD_CORPORATE (1)
     *               - NAME_MIDDLE_WORD_MERCENARY (2)
     *               - NAME_END_WORD_MERCENARY (3)
     *               - NAME_PRE_FAB (4)
     *
     * @throws IllegalStateException if the given origin value is unexpected
     */
    //region Initialization
    private void runThreadLoader(int origin) {
        Thread loader = new Thread(() -> randomCompanyNameGenerator.populateCompanyNameSegments(origin),
                "Random Company Name Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    /**
     * Populates the segments of the company name based on the given origin.
     *
     * @param origin The origin of the company name segments.
     *               Possible values are:
     *               - NAME_MIDDLE_WORD_CORPORATE (0)
     *               - NAME_END_WORD_CORPORATE (1)
     *               - NAME_MIDDLE_WORD_MERCENARY (2)
     *               - NAME_END_WORD_MERCENARY (3)
     *               - NAME_PRE_FAB (4)
     * @throws IllegalStateException if the given origin value is unexpected
     */
    private void populateCompanyNameSegments(int origin) {
        String filePath;
        String userFilePath;

        switch (origin) {
            case NAME_MIDDLE_WORD_CORPORATE -> {
                setWeightedMiddleWordCorporate(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_MIDDLE_WORD_CORPORATE;
                userFilePath = MHQConstants.NAME_MIDDLE_WORD_CORPORATE_USER;
            }
            case NAME_END_WORD_CORPORATE -> {
                setWeightedEndWordCorporate(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_END_WORD_CORPORATE;
                userFilePath = MHQConstants.NAME_END_WORD_CORPORATE_USER;
            }
            case NAME_MIDDLE_WORD_MERCENARY -> {
                setWeightedMiddleWordMercenary(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_MIDDLE_WORD_MERCENARY;
                userFilePath = MHQConstants.NAME_MIDDLE_WORD_MERCENARY_USER;
            }
            case NAME_END_WORD_MERCENARY -> {
                setWeightedEndWordMercenary(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_END_WORD_MERCENARY;
                userFilePath = MHQConstants.NAME_END_WORD_MERCENARY_USER;
            }
            case NAME_PRE_FAB -> {
                setWeightedPreFab(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_PRE_FAB;
                userFilePath = MHQConstants.NAME_PRE_FAB_USER;
            }
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/backgrounds/RandomCompanyNameGenerator.java/populateCompanyNameSegments 1 of 2: "
                    + origin);
        }

        final Map<String, Integer> nameSegments = new HashMap<>();
        loadCompanyNameSegments(new File(filePath), nameSegments);
        loadCompanyNameSegments(new File(userFilePath), nameSegments);

        for (final Entry<String, Integer> entry : nameSegments.entrySet()) {
            switch (origin) {
                case NAME_MIDDLE_WORD_CORPORATE -> getWeightedMiddleWordCorporate().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_CORPORATE -> getWeightedEndWordCorporate().add(entry.getValue(), entry.getKey());
                case NAME_MIDDLE_WORD_MERCENARY -> getWeightedMiddleWordMercenary().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_MERCENARY -> getWeightedEndWordMercenary().add(entry.getValue(), entry.getKey());
                case NAME_PRE_FAB -> getWeightedPreFab().add(entry.getValue(), entry.getKey());
                default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/backgrounds/RandomCompanyNameGenerator.java/populateCompanyNameSegments 2 of 2: "
                        + origin);
            }
        }

        initialized = true;
    }

    /**
     * Loads the company name segments from the given file and populates the provided map.
     *
     * @param file         The file containing the company name segments.
     * @param nameSegments The map to populate with the loaded name segments.
     */
    private void loadCompanyNameSegments(final File file, final Map<String, Integer> nameSegments) {
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
            logger.error("Failed to populate company name from {}", file, e);
        }
    }
    //endregion Initialization
}
