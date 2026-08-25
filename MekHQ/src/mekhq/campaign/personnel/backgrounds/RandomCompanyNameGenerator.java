/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.backgrounds;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;


/**
 * Save File Formatting: word, weight word is a String that does not include a ',' Weight is an integer weight used
 * during generation
 */
public class RandomCompanyNameGenerator implements Serializable {
    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = 4721410214327210288L;
    private static final int NAME_MIDDLE_WORD_CORPORATE = 0;
    private static final int NAME_END_WORD_CORPORATE = 1;
    private static final int NAME_MIDDLE_WORD_MERCENARY = 2;
    private static final int NAME_END_WORD_MERCENARY = 3;
    private static final int NAME_PRE_FAB = 4;
    private static final int NAME_MIDDLE_WORD_REBEL = 5;
    private static final int NAME_END_WORD_REBEL = 6;
    private static final int NAME_MIDDLE_WORD_MILITIA = 7;
    private static final int NAME_END_WORD_MILITIA = 8;
    private static final int NAME_MIDDLE_WORD_CIVILIAN = 9;
    private static final int NAME_END_WORD_CIVILIAN = 10;

    private static WeightedIntMap<String> weightedMiddleWordCorporate;
    private static WeightedIntMap<String> weightedEndWordCorporate;
    private static WeightedIntMap<String> weightedMiddleWordMercenary;
    private static WeightedIntMap<String> weightedEndWordMercenary;
    private static WeightedIntMap<String> weightedPreFab;
    private static WeightedIntMap<String> weightedMiddleWordRebel;
    private static WeightedIntMap<String> weightedEndWordRebel;
    private static WeightedIntMap<String> weightedMiddleWordMilitia;
    private static WeightedIntMap<String> weightedEndWordMilitia;
    private static WeightedIntMap<String> weightedMiddleWordCivilian;
    private static WeightedIntMap<String> weightedEndWordCivilian;

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

    public static WeightedIntMap<String> getWeightedMiddleWordRebel() {
        return weightedMiddleWordRebel;
    }

    public static void setWeightedMiddleWordRebel(final WeightedIntMap<String> weightedMiddleWordRebel) {
        RandomCompanyNameGenerator.weightedMiddleWordRebel = weightedMiddleWordRebel;
    }

    public static WeightedIntMap<String> getWeightedEndWordRebel() {
        return weightedEndWordRebel;
    }

    public static void setWeightedEndWordRebel(final WeightedIntMap<String> weightedEndWordRebel) {
        RandomCompanyNameGenerator.weightedEndWordRebel = weightedEndWordRebel;
    }

    public static WeightedIntMap<String> getWeightedMiddleWordMilitia() {
        return weightedMiddleWordMilitia;
    }

    public static void setWeightedMiddleWordMilitia(final WeightedIntMap<String> weightedMiddleWordMilitia) {
        RandomCompanyNameGenerator.weightedMiddleWordMilitia = weightedMiddleWordMilitia;
    }

    public static WeightedIntMap<String> getWeightedEndWordMilitia() {
        return weightedEndWordMilitia;
    }

    public static void setWeightedEndWordMilitia(final WeightedIntMap<String> weightedEndWordMilitia) {
        RandomCompanyNameGenerator.weightedEndWordMilitia = weightedEndWordMilitia;
    }

    public static WeightedIntMap<String> getWeightedMiddleWordCivilian() {
        return weightedMiddleWordCivilian;
    }

    public static void setWeightedMiddleWordCivilian(final WeightedIntMap<String> weightedMiddleWordCivilian) {
        RandomCompanyNameGenerator.weightedMiddleWordCivilian = weightedMiddleWordCivilian;
    }

    public static WeightedIntMap<String> getWeightedEndWordCivilian() {
        return weightedEndWordCivilian;
    }

    public static void setWeightedEndWordCivilian(final WeightedIntMap<String> weightedEndWordCivilian) {
        RandomCompanyNameGenerator.weightedEndWordCivilian = weightedEndWordCivilian;
    }
    //endregion Getters/Setters

    /**
     * Returns an instance of the RandomCompanyNameGenerator class. The method applies the double-check locking pattern
     * to ensure that only one instance of the class is created.
     *
     * @return The instance of RandomCompanyNameGenerator. If the instance does not exist, it creates a new instance and
     *       initializes it by running the thread loader for various origin values.
     */
    //region Synchronization
    public static RandomCompanyNameGenerator getInstance() {
        RandomCompanyNameGenerator instance = randomCompanyNameGenerator;
        if (instance == null) { // First check
            synchronized (RandomCompanyNameGenerator.class) {
                instance = randomCompanyNameGenerator;
                if (instance == null) { // Double check
                    instance = new RandomCompanyNameGenerator();
                    instance.runThreadLoader(NAME_MIDDLE_WORD_CORPORATE);
                    instance.runThreadLoader(NAME_END_WORD_CORPORATE);
                    instance.runThreadLoader(NAME_MIDDLE_WORD_MERCENARY);
                    instance.runThreadLoader(NAME_END_WORD_MERCENARY);
                    instance.runThreadLoader(NAME_PRE_FAB);
                    instance.runThreadLoader(NAME_MIDDLE_WORD_REBEL);
                    instance.runThreadLoader(NAME_END_WORD_REBEL);
                    instance.runThreadLoader(NAME_MIDDLE_WORD_MILITIA);
                    instance.runThreadLoader(NAME_END_WORD_MILITIA);
                    instance.runThreadLoader(NAME_MIDDLE_WORD_CIVILIAN);
                    instance.runThreadLoader(NAME_END_WORD_CIVILIAN);
                    // Publish only after the fully constructed instance has kicked off initialization,
                    // so no other thread can observe a partially-initialized instance through the field.
                    randomCompanyNameGenerator = instance;
                }
            }
        }
        return instance;
    }
    //endregion Synchronization

    /**
     * Generates a random company name segment based on the given origin.
     *
     * @param origin The origin of the company name. Possible values are: - NAME_MIDDLE_WORD_CORPORATE (0) -
     *               NAME_END_WORD_CORPORATE (1) - NAME_MIDDLE_WORD_MERCENARY (2) - NAME_END_WORD_MERCENARY (3) -
     *               NAME_PRE_FAB (4)
     *
     * @return The generated name segment as a string. If the list of company name segments is not initialized, it
     *       returns an empty string.
     *
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
                case NAME_MIDDLE_WORD_REBEL -> getWeightedMiddleWordRebel().randomItem();
                case NAME_END_WORD_REBEL -> getWeightedEndWordRebel().randomItem();
                case NAME_MIDDLE_WORD_MILITIA -> getWeightedMiddleWordMilitia().randomItem();
                case NAME_END_WORD_MILITIA -> getWeightedEndWordMilitia().randomItem();
                case NAME_MIDDLE_WORD_CIVILIAN -> getWeightedMiddleWordCivilian().randomItem();
                case NAME_END_WORD_CIVILIAN -> getWeightedEndWordCivilian().randomItem();
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
     * @param origin The origin of the company name segments. Possible values are: - NAME_MIDDLE_WORD_CORPORATE (0) -
     *               NAME_END_WORD_CORPORATE (1) - NAME_MIDDLE_WORD_MERCENARY (2) - NAME_END_WORD_MERCENARY (3) -
     *               NAME_PRE_FAB (4)
     *
     * @throws IllegalStateException if the given origin value is unexpected
     */
    //region Initialization
    private void runThreadLoader(int origin) {
        Thread loader = new Thread(() -> populateCompanyNameSegments(origin),
              "Random Company Name Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    /**
     * Populates the segments of the company name based on the given origin.
     *
     * @param origin The origin of the company name segments. Possible values are: - NAME_MIDDLE_WORD_CORPORATE (0) -
     *               NAME_END_WORD_CORPORATE (1) - NAME_MIDDLE_WORD_MERCENARY (2) - NAME_END_WORD_MERCENARY (3) -
     *               NAME_PRE_FAB (4)
     *
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
            case NAME_MIDDLE_WORD_REBEL -> {
                setWeightedMiddleWordRebel(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_MIDDLE_WORD_REBEL;
                userFilePath = MHQConstants.NAME_MIDDLE_WORD_REBEL_USER;
            }
            case NAME_END_WORD_REBEL -> {
                setWeightedEndWordRebel(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_END_WORD_REBEL;
                userFilePath = MHQConstants.NAME_END_WORD_REBEL_USER;
            }
            case NAME_MIDDLE_WORD_MILITIA -> {
                setWeightedMiddleWordMilitia(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_MIDDLE_WORD_MILITIA;
                userFilePath = MHQConstants.NAME_MIDDLE_WORD_MILITIA_USER;
            }
            case NAME_END_WORD_MILITIA -> {
                setWeightedEndWordMilitia(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_END_WORD_MILITIA;
                userFilePath = MHQConstants.NAME_END_WORD_MILITIA_USER;
            }
            case NAME_MIDDLE_WORD_CIVILIAN -> {
                setWeightedMiddleWordCivilian(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_MIDDLE_WORD_CIVILIAN;
                userFilePath = MHQConstants.NAME_MIDDLE_WORD_CIVILIAN_USER;
            }
            case NAME_END_WORD_CIVILIAN -> {
                setWeightedEndWordCivilian(new WeightedIntMap<>());
                filePath = MHQConstants.NAME_END_WORD_CIVILIAN;
                userFilePath = MHQConstants.NAME_END_WORD_CIVILIAN_USER;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/RandomCompanyNameGenerator.java/populateCompanyNameSegments 1 of 2: "
                        + origin);
        }

        final Map<String, Integer> nameSegments = new HashMap<>();
        loadCompanyNameSegments(new File(filePath), nameSegments);
        loadCompanyNameSegments(new File(userFilePath), nameSegments);

        for (final Entry<String, Integer> entry : nameSegments.entrySet()) {
            switch (origin) {
                case NAME_MIDDLE_WORD_CORPORATE ->
                      getWeightedMiddleWordCorporate().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_CORPORATE -> getWeightedEndWordCorporate().add(entry.getValue(), entry.getKey());
                case NAME_MIDDLE_WORD_MERCENARY ->
                      getWeightedMiddleWordMercenary().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_MERCENARY -> getWeightedEndWordMercenary().add(entry.getValue(), entry.getKey());
                case NAME_PRE_FAB -> getWeightedPreFab().add(entry.getValue(), entry.getKey());
                case NAME_MIDDLE_WORD_REBEL -> getWeightedMiddleWordRebel().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_REBEL -> getWeightedEndWordRebel().add(entry.getValue(), entry.getKey());
                case NAME_MIDDLE_WORD_MILITIA -> getWeightedMiddleWordMilitia().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_MILITIA -> getWeightedEndWordMilitia().add(entry.getValue(), entry.getKey());
                case NAME_MIDDLE_WORD_CIVILIAN -> getWeightedMiddleWordCivilian().add(entry.getValue(), entry.getKey());
                case NAME_END_WORD_CIVILIAN -> getWeightedEndWordCivilian().add(entry.getValue(), entry.getKey());
                default -> throw new IllegalStateException(
                      "Unexpected value in mekhq/campaign/personnel/backgrounds/RandomCompanyNameGenerator.java/populateCompanyNameSegments 2 of 2: "
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
