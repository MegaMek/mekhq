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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * Manages a library of {@link FactionStandingUltimatumData} objects, each loaded from its own JSON file.
 *
 * <p>The files to load are listed in {@code data/universe/factionStandingUltimatums/ultimatummanifest.json}, and
 * live alongside it. Ultimatums are mapped by their {@link LocalDate} and then by each of their affected faction
 * codes.</p>
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
     * Constructs a new {@link FactionStandingUltimatumsLibrary} and loads the ultimatums listed in the default
     * manifest.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingUltimatumsLibrary() {
        this(new File(MHQConstants.FACTION_STANDING_ULTIMATUM_MANIFEST));
    }

    /**
     * Constructs a new {@link FactionStandingUltimatumsLibrary} and loads the ultimatums listed in the given manifest.
     *
     * @param manifestFile the manifest listing the ultimatum files, which sit in the same directory
     *
     * @throws RuntimeException if the manifest cannot be read
     * @author Illiani
     * @since 0.51.01
     */
    FactionStandingUltimatumsLibrary(File manifestFile) {
        indexUltimatums(readUltimatums(manifestFile));
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
     * Reads every ultimatum listed in a manifest. A listed file that is missing or unreadable is logged and skipped,
     * so one bad file doesn't stop the others loading.
     *
     * @param manifestFile the manifest to read
     *
     * @return the ultimatums that loaded
     *
     * @throws RuntimeException if the manifest itself cannot be read
     * @author Illiani
     * @since 0.51.01
     */
    private static List<FactionStandingUltimatumData> readUltimatums(File manifestFile) {
        FactionStandingUltimatumManifest manifest = FactionStandingUltimatumManifest.deserialize(manifestFile);
        if (manifest == null) {
            throw new RuntimeException("Could not read ultimatum manifest: " + manifestFile.getPath());
        }

        File ultimatumsDirectory = manifestFile.getAbsoluteFile().getParentFile();
        List<FactionStandingUltimatumData> ultimatums = new ArrayList<>();
        for (String fileName : manifest.ultimatumFileNames) {
            FactionStandingUltimatumData ultimatum = FactionStandingUltimatumData.deserialize(
                  new File(ultimatumsDirectory, fileName));
            if (ultimatum == null) {
                LOGGER.error("Ultimatum file {} listed in {} could not be loaded", fileName, manifestFile.getPath());
                continue;
            }

            ultimatums.add(ultimatum);
        }

        return ultimatums;
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
