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
package mekhq.campaign.universe.factionHints;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Loads faction diplomacy data (wars, alliances, rivalries, neutral factions, and contained factions) from the
 * per-conflict YAML files in {@code data/universe/factionDiplomacy/} into a {@link FactionHints} instance.
 *
 * <p>Every {@code *.yml} file in the directory is read and merged; a file that fails to parse is logged and skipped
 * so a single bad file cannot take down the rest of the diplomacy data. The directory format is documented in
 * {@code docs/Faction Diplomacy Readme.md}.</p>
 */
final class FactionDiplomacyLoader {
    private static final MMLogger LOGGER = MMLogger.create(FactionDiplomacyLoader.class);

    private FactionDiplomacyLoader() {
    }

    /**
     * One faction diplomacy YAML file. Every section is optional.
     *
     * @param wars              wars declared in this file, or {@code null} if the section is absent
     * @param alliances         alliances declared in this file, or {@code null} if the section is absent
     * @param rivalries         rivalries declared in this file, or {@code null} if the section is absent
     * @param neutralFactions   neutral factions declared in this file, or {@code null} if the section is absent
     * @param containedFactions contained factions declared in this file, or {@code null} if the section is absent
     */
    private record DiplomacyFileData(@Nullable List<HintEntryData> wars,
          @Nullable List<HintEntryData> alliances,
          @Nullable List<PartyData> rivalries,
          @Nullable List<NeutralEntryData> neutralFactions,
          @Nullable List<ContainedEntryData> containedFactions) {
    }

    /**
     * A war or alliance entry. Entries without dates are perpetual.
     *
     * @param name    the display name of the war or alliance, or {@code null} for unnamed entries
     * @param start   the ISO-8601 start date, or {@code null} for no lower bound
     * @param end     the ISO-8601 end date, or {@code null} for no upper bound
     * @param parties the factions involved; each party may override the entry dates
     */
    private record HintEntryData(@Nullable String name, @Nullable String start, @Nullable String end,
          List<PartyData> parties) {
    }

    /**
     * A group of faction codes that are all at war with (or allied with, or rivals of) each other.
     *
     * @param factions the faction codes involved
     * @param start    the ISO-8601 start date overriding the containing entry, or {@code null} to inherit
     * @param end      the ISO-8601 end date overriding the containing entry, or {@code null} to inherit
     */
    private record PartyData(List<String> factions, @Nullable String start, @Nullable String end) {
    }

    /**
     * A faction considered allied with everyone except the listed exceptions.
     *
     * @param faction    the neutral faction code
     * @param exceptions the factions exempted from neutrality, or {@code null} for none
     */
    private record NeutralEntryData(String faction, @Nullable List<PartyData> exceptions) {
    }

    /**
     * A faction located within another faction's borders without controlling any worlds there.
     *
     * @param host      the faction code controlling the space
     * @param contained the faction code located within the host's space
     * @param start     the ISO-8601 start date, or {@code null} for no lower bound
     * @param end       the ISO-8601 end date, or {@code null} for no upper bound
     * @param fraction  the portion of the host's border apportioned to the contained faction, or {@code null}
     * @param opponents if non-{@code null}, restricts the contained faction's opponents to the listed codes
     */
    private record ContainedEntryData(String host, String contained, @Nullable String start, @Nullable String end,
          @Nullable Double fraction, @Nullable List<String> opponents) {
    }

    /**
     * Reads every {@code *.yml} file in the given directory and merges its contents into the given hints.
     *
     * @param hints     the instance to populate
     * @param directory the faction diplomacy data directory
     *
     * @return {@code true} if at least one YAML file was loaded; {@code false} if the directory holds no readable
     *       YAML data, so the caller can fall back to the legacy XML file
     */
    static boolean load(FactionHints hints, File directory) {
        File[] files = directory.listFiles((unusedDirectory, fileName) -> fileName.toLowerCase().endsWith(".yml"));
        if ((files == null) || (files.length == 0)) {
            LOGGER.error("[FactionDiplomacy] No YAML files found in {}", directory.getPath());
            return false;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        int loadedFileCount = 0;
        int failedFileCount = 0;
        for (File file : files) {
            try {
                DiplomacyFileData fileData = mapper.readValue(file, DiplomacyFileData.class);
                applyFile(hints, fileData, file.getName());
                loadedFileCount++;
            } catch (IOException | RuntimeException exception) {
                // A malformed file must not take down the remaining diplomacy data
                LOGGER.error(exception, "[FactionDiplomacy] Failed to load " + file.getName());
                failedFileCount++;
            }
        }
        LOGGER.info("[FactionDiplomacy] Loaded {} files from {} ({} failed)",
              loadedFileCount, directory.getPath(), failedFileCount);
        return loadedFileCount > 0;
    }

    private static void applyFile(FactionHints hints, DiplomacyFileData fileData, String fileName) {
        if (fileData.wars() != null) {
            for (HintEntryData war : fileData.wars()) {
                applyHintEntry(war, fileName,
                      (start, end, parties) -> hints.addWar(nameOrEmpty(war), start, end, parties));
            }
        }
        if (fileData.alliances() != null) {
            for (HintEntryData alliance : fileData.alliances()) {
                applyHintEntry(alliance, fileName,
                      (start, end, parties) -> hints.addAlliance(nameOrEmpty(alliance), start, end, parties));
            }
        }
        if (fileData.rivalries() != null) {
            for (PartyData rivalry : fileData.rivalries()) {
                Faction[] parties = resolveFactions(rivalry.factions(), fileName);
                hints.addRivalry("", parseDate(rivalry.start()), parseDate(rivalry.end()), parties);
            }
        }
        if (fileData.neutralFactions() != null) {
            for (NeutralEntryData neutral : fileData.neutralFactions()) {
                applyNeutralEntry(hints, neutral, fileName);
            }
        }
        if (fileData.containedFactions() != null) {
            for (ContainedEntryData contained : fileData.containedFactions()) {
                applyContainedEntry(hints, contained, fileName);
            }
        }
    }

    private static String nameOrEmpty(HintEntryData entry) {
        return (entry.name() != null) ? entry.name() : "";
    }

    /** Target method for a war or alliance party once its dates and factions are resolved. */
    @FunctionalInterface
    private interface HintConsumer {
        void accept(@Nullable LocalDate start, @Nullable LocalDate end, Faction... parties);
    }

    private static void applyHintEntry(HintEntryData entry, String fileName, HintConsumer consumer) {
        LocalDate entryStart = parseDate(entry.start());
        LocalDate entryEnd = parseDate(entry.end());
        for (PartyData party : entry.parties()) {
            LocalDate start = (party.start() != null) ? parseDate(party.start()) : entryStart;
            LocalDate end = (party.end() != null) ? parseDate(party.end()) : entryEnd;
            Faction[] parties = resolveFactions(party.factions(), fileName);
            consumer.accept(start, end, parties);
        }
    }

    private static void applyNeutralEntry(FactionHints hints, NeutralEntryData neutral, String fileName) {
        Faction faction = resolveFaction(neutral.faction(), fileName);
        if (faction == null) {
            return;
        }
        hints.addNeutralFaction(faction);
        if (neutral.exceptions() == null) {
            return;
        }
        for (PartyData exception : neutral.exceptions()) {
            Faction[] exceptedFactions = resolveFactions(exception.factions(), fileName);
            hints.addNeutralExceptions("", parseDate(exception.start()), parseDate(exception.end()),
                  faction, exceptedFactions);
        }
    }

    private static void applyContainedEntry(FactionHints hints, ContainedEntryData contained, String fileName) {
        Faction host = resolveFaction(contained.host(), fileName);
        Faction inner = resolveFaction(contained.contained(), fileName);
        if ((host == null) || (inner == null)) {
            return;
        }
        // 0.0 is the "no split" sentinel: consumers skip the border split for non-positive fractions,
        // apportioning the full border to both factions - same as the legacy XML loader's default
        double fraction = (contained.fraction() != null) ? contained.fraction() : 0.0;
        List<Faction> opponents = null;
        if (contained.opponents() != null) {
            opponents = new ArrayList<>(Arrays.asList(resolveFactions(contained.opponents(), fileName)));
        }
        hints.addContainedFaction(host, inner, parseDate(contained.start()), parseDate(contained.end()),
              fraction, opponents);
    }

    /**
     * Resolves faction codes, dropping (and logging) any code that does not match a known faction so one bad code
     * cannot poison the remaining parties in an entry.
     */
    private static Faction[] resolveFactions(List<String> factionCodes, String fileName) {
        List<Faction> resolvedFactions = new ArrayList<>();
        for (String factionCode : factionCodes) {
            Faction faction = resolveFaction(factionCode, fileName);
            if (faction != null) {
                resolvedFactions.add(faction);
            }
        }
        return resolvedFactions.toArray(new Faction[0]);
    }

    private static @Nullable Faction resolveFaction(String factionCode, String fileName) {
        Faction faction = Factions.getInstance().getFaction(factionCode);
        if (faction.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)) {
            LOGGER.error("[FactionDiplomacy] Invalid faction code {} in {}", factionCode, fileName);
            return null;
        }
        return faction;
    }

    private static @Nullable LocalDate parseDate(@Nullable String value) {
        return (value == null) ? null : LocalDate.parse(value);
    }
}
