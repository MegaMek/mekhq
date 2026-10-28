/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.time.LocalDate;
import java.util.*;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * Manages a library of {@link FactionStandingUltimatumData} objects, each loaded from its own JSON file.
 *
 * <p>Every {@code .json} file in {@code data/universe/factionStandingUltimatums/} is loaded. Ultimatums are mapped by
 * their {@link LocalDate} and then by each of their affected faction codes.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingUltimatumsLibrary {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUltimatumsLibrary.class);

    /**
     * Map storing Faction Standing ultimatums, keyed by date, then by affected faction code.
     */
    private final Map<LocalDate, Map<String, FactionStandingUltimatumData>> ultimatumMap = new HashMap<>();

    /**
     * Constructs a new {@link FactionStandingUltimatumsLibrary} and loads the ultimatums in the default directory.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingUltimatumsLibrary() {
        this(new File(MHQConstants.FACTION_STANDING_ULTIMATUM_PATH));
    }

    /**
     * Constructs a new {@link FactionStandingUltimatumsLibrary} and loads every ultimatum in the given directory.
     *
     * @param ultimatumsDirectory the directory holding the ultimatum files
     *
     * @throws RuntimeException if the directory cannot be read
     * @author Illiani
     * @since 0.51.01
     */
    FactionStandingUltimatumsLibrary(File ultimatumsDirectory) {
        indexUltimatums(readUltimatums(ultimatumsDirectory));
    }

    /**
     * Constructs a new {@link FactionStandingUltimatumsLibrary} from already-parsed ultimatums.
     *
     * @param ultimatums the ultimatums to index
     *
     * @author Illiani
     * @since 0.51.01
     */
    FactionStandingUltimatumsLibrary(List<FactionStandingUltimatumData> ultimatums) {
        indexUltimatums(ultimatums);
    }

    /**
     * Returns an unmodifiable map of all loaded Faction Standing ultimatums.
     *
     * <p>The map is keyed by {@link LocalDate} and affected faction code. An ultimatum with several affected factions
     * appears once under each of them.</p>
     *
     * @return unmodifiable map of ultimatums by date
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<LocalDate, Map<String, FactionStandingUltimatumData>> getUltimatums() {
        // Deeply unmodifiable for outside callers
        Map<LocalDate, Map<String, FactionStandingUltimatumData>> outer = new HashMap<>();
        for (var entry : ultimatumMap.entrySet()) {
            outer.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }

        return Collections.unmodifiableMap(outer);
    }

    /**
     * Looks up the {@link FactionStandingUltimatumData} for a given date and faction code.
     *
     * @param date                The date of interest
     * @param affectedFactionCode The code for the affected faction
     *
     * @return the matching {@link FactionStandingUltimatumData} or {@code null} if none is found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionStandingUltimatumData getUltimatum(LocalDate date, String affectedFactionCode) {
        Map<String, FactionStandingUltimatumData> ultimatumsOnDate = ultimatumMap.get(date);
        if (ultimatumsOnDate == null) {
            return null;
        }

        return ultimatumsOnDate.get(affectedFactionCode);
    }

    /**
     * Reads every {@code .json} file in a directory, in file name order. A file that is unreadable is logged and
     * skipped, so one bad file doesn't stop the others loading.
     *
     * @param ultimatumsDirectory the directory to read
     *
     * @return the ultimatums that loaded
     *
     * @throws RuntimeException if the directory itself cannot be read
     * @author Illiani
     * @since 0.51.01
     */
    private static List<FactionStandingUltimatumData> readUltimatums(File ultimatumsDirectory) {
        File[] ultimatumFiles = ultimatumsDirectory.listFiles(
              file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".json"));
        if (ultimatumFiles == null) {
            throw new RuntimeException("Could not read ultimatum directory: " + ultimatumsDirectory.getPath());
        }

        // listFiles has no guaranteed order, and the first ultimatum loaded wins a date clash
        Arrays.sort(ultimatumFiles, Comparator.comparing(File::getName));

        List<FactionStandingUltimatumData> ultimatums = new ArrayList<>();
        for (File ultimatumFile : ultimatumFiles) {
            FactionStandingUltimatumData ultimatum = FactionStandingUltimatumData.deserialize(ultimatumFile);
            if (ultimatum == null) {
                LOGGER.error("Ultimatum file {} could not be loaded", ultimatumFile.getPath());
                continue;
            }

            ultimatums.add(ultimatum);
        }

        return ultimatums;
    }

    /**
     * Finds a problem that would break an ultimatum when it is presented: a missing name, which its text keys are
     * built from, or a side missing its ID, leader name, role, or faction, or repeating another side's ID.
     *
     * @param ultimatum the ultimatum to check
     *
     * @return a description of the first problem found, or {@code null} if the ultimatum can be presented
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String findLoadProblem(FactionStandingUltimatumData ultimatum) {
        if (isBlank(ultimatum.name())) {
            return "it has no name";
        }

        Set<String> sideIds = new HashSet<>();
        for (FactionStandingUltimatumSide side : ultimatum.sides()) {
            if (isBlank(side.id()) || isBlank(side.name()) || side.role() == null || isBlank(side.factionCode())) {
                return "a side is missing its id, name, role, or faction code";
            }
            if (!sideIds.add(side.id())) {
                return "more than one side uses the id " + side.id();
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the dissenter preference is {@link FactionStandingUltimatumData#ROGUE_PREFERENCE} or
     *       names one of the ultimatum's sides
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isValidDissenterPreference(FactionStandingUltimatumData ultimatum) {
        String preference = ultimatum.dissenterPreference();
        return FactionStandingUltimatumData.ROGUE_PREFERENCE.equals(preference)
                     || ultimatum.getSide(preference) != null;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    /**
     * Adds each ultimatum to {@link #ultimatumMap} under its date and every one of its affected faction codes.
     *
     * <p>A faction can only receive one ultimatum per date. If two ultimatums claim the same date and faction, the
     * first one loaded is kept and the other is logged and skipped for that faction. An ultimatum with a malformed
     * date or no sides is logged and skipped.</p>
     *
     * @param ultimatums the ultimatums to index
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void indexUltimatums(List<FactionStandingUltimatumData> ultimatums) {
        for (FactionStandingUltimatumData ultimatum : ultimatums) {
            List<String> affectedFactionCodes = ultimatum.affectedFactionCodes();
            if (affectedFactionCodes.isEmpty()) {
                LOGGER.warn("Ultimatum {} has no affected factions and will never trigger", ultimatum.name());
                continue;
            }

            if (ultimatum.sides().isEmpty()) {
                LOGGER.warn("Ultimatum {} has no sides to choose between and will never trigger", ultimatum.name());
                continue;
            }

            String problem = findLoadProblem(ultimatum);
            if (problem != null) {
                LOGGER.error("Ultimatum {} will never trigger: {}", ultimatum.name(), problem);
                continue;
            }

            if (!isValidDissenterPreference(ultimatum)) {
                LOGGER.warn("Ultimatum {} has dissenter preference '{}', which names no side; the dissenter will "
                                  + "always leave", ultimatum.name(), ultimatum.dissenterPreference());
            }

            LocalDate date;
            try {
                date = ultimatum.getDate();
            } catch (RuntimeException exception) {
                LOGGER.error("Ultimatum {} has an invalid date '{}' and will never trigger", ultimatum.name(),
                      ultimatum.date());
                continue;
            }

            Map<String, FactionStandingUltimatumData> ultimatumsOnDate = ultimatumMap.computeIfAbsent(date,
                  ignored -> new HashMap<>());

            for (String factionCode : affectedFactionCodes) {
                FactionStandingUltimatumData existingUltimatum = ultimatumsOnDate.putIfAbsent(factionCode, ultimatum);
                if (existingUltimatum != null && existingUltimatum != ultimatum) {
                    LOGGER.warn("Ultimatum {} skipped for faction {} on {}: {} already uses that date",
                          ultimatum.name(), factionCode, ultimatum.date(), existingUltimatum.name());
                }
            }
        }
    }
}
