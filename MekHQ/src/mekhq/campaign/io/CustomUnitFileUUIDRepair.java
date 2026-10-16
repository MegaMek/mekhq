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
package mekhq.campaign.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nullable;
import megamek.common.Configuration;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.loaders.MtfFile;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.UUIDUtil;
import megamek.logging.MMLogger;

/**
 * Repairs custom unit files that share their unit-file UUID with a differently named unit file.
 *
 * <p>Before refits started generating their own UUID, a customized refit saved by MekHQ kept the UUID of the design
 * it was edited from. The unit cache then reports an ambiguous UUID, and a UUID-first lookup can load the source
 * design instead of the custom. This repair gives each such custom file a fresh UUID, written in place so nothing else
 * in the file changes, and records the shared UUID as the file's refit-from reference when it has none.</p>
 *
 * <p>Custom files are the plain unit files in the user unit directories: the MekHQ customs directory, the user data
 * unit directory and the external user directory. Official unit files outside the customs directory are never
 * rewritten, even when a user directory contains them. In each group of files sharing a UUID one file keeps it: a file
 * outside those directories when there is one (normally the canon design), otherwise the oldest custom file, which is
 * the design the others were refit from. A custom with the same chassis and model as that file is left alone, since
 * MegaMekLab deliberately keeps the UUID when a design is re-saved under its own name (for example as an override of a
 * canon unit).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CustomUnitFileUUIDRepair {
    private static final MMLogger LOGGER = MMLogger.create(CustomUnitFileUUIDRepair.class);

    // TODO : Remove inline file path (matches Refit#saveCustomization and CampaignXmlParser#processCustom)
    private static final Path CUSTOMS_DIRECTORY = Path.of("data", "mekfiles", "customs");

    // Unit files are rewritten as bytes-in, bytes-out; ISO-8859-1 maps every byte to one character and back, so any
    // non-UTF-8 text in a file survives the round trip unchanged.
    private static final Charset FILE_CHARSET = StandardCharsets.ISO_8859_1;

    private CustomUnitFileUUIDRepair() {
    }

    /**
     * Repairs every custom unit file in the user unit directories whose UUID collides with a differently named unit
     * file, then rescans the unit cache if any file was changed. Blocks until the unit cache has finished loading.
     *
     * @return the number of custom unit files that were given a new UUID
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int repairCustomUnitFiles() {
        MekSummaryCache cache = MekSummaryCache.getInstance();
        int repairedCount = repairCustomUnitFiles(cache.getAllMeks(), getUserUnitDirectories());
        if (repairedCount > 0) {
            LOGGER.info("Gave {} custom unit file(s) a new unit-file UUID; rescanning unit files.", repairedCount);
            cache.loadMekData();
        }
        return repairedCount;
    }

    /**
     * Lists the directories holding the player's own unit files, matching the user directories the unit cache scans:
     * the MekHQ customs directory, the unit directory inside the user data directory, and the external user directory
     * from the client preferences when one is set.
     *
     * @return the user unit directories; some may not exist
     *
     * @author Illiani
     * @since 0.51.01
     */
    static UserUnitDirectories getUserUnitDirectories() {
        List<Path> userDirectories = new ArrayList<>();
        userDirectories.add(new File(Configuration.userDataDir(), Configuration.unitsDir().toString()).toPath());
        String externalUserDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if ((externalUserDirectory != null) && !externalUserDirectory.isBlank()) {
            userDirectories.add(Path.of(externalUserDirectory));
        }
        return new UserUnitDirectories(CUSTOMS_DIRECTORY, Configuration.unitsDir().toPath(), userDirectories);
    }

    /**
     * The directories whose unit files the repair may rewrite.
     *
     * @param customsDirectory      the MekHQ customs directory; always eligible, even inside the official units
     * @param officialUnitDirectory the official unit directory; nothing in it outside the customs directory is ever
     *                              eligible, even when a user directory contains it
     * @param userDirectories       the other user unit directories
     *
     * @author Illiani
     * @since 0.51.01
     */
    record UserUnitDirectories(Path customsDirectory, Path officialUnitDirectory, List<Path> userDirectories) {
        UserUnitDirectories {
            customsDirectory = customsDirectory.toAbsolutePath().normalize();
            officialUnitDirectory = officialUnitDirectory.toAbsolutePath().normalize();
            List<Path> normalizedUserDirectories = new ArrayList<>();
            for (Path userDirectory : userDirectories) {
                normalizedUserDirectories.add(userDirectory.toAbsolutePath().normalize());
            }
            userDirectories = normalizedUserDirectories;
        }

        /**
         * @return {@code true} if the file lies in the customs directory, or in a user directory but outside the
         *       official unit directory
         */
        boolean contains(File file) {
            Path normalizedPath = file.toPath().toAbsolutePath().normalize();
            if (normalizedPath.startsWith(customsDirectory)) {
                return true;
            }
            if (normalizedPath.startsWith(officialUnitDirectory)) {
                return false;
            }
            for (Path userDirectory : userDirectories) {
                if (normalizedPath.startsWith(userDirectory)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Repairs every custom unit file under {@code userUnitDirectories} whose UUID collides with a differently named
     * unit file among the {@code summaries}. Does not touch the unit cache.
     *
     * @param summaries           the unit summaries to check
     * @param userUnitDirectories the directories whose unit files may be rewritten
     *
     * @return the number of custom unit files that were given a new UUID
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int repairCustomUnitFiles(MekSummary[] summaries, UserUnitDirectories userUnitDirectories) {
        int repairedCount = 0;
        for (MekSummary summary : findCustomsToRepair(summaries, userUnitDirectories)) {
            File sourceFile = summary.getSourceFile();
            String sharedUUID = summary.getUnitFileUUID();
            String newUUID = UUIDUtil.newUUIDv7().toString();
            try {
                if (rewriteUnitFileUUID(sourceFile, newUUID, sharedUUID)) {
                    LOGGER.info("Custom unit file {} shared unit-file UUID {} with another unit; it now has UUID {}.",
                          sourceFile, sharedUUID, newUUID);
                    repairedCount++;
                } else {
                    LOGGER.warn("Custom unit file {} shares unit-file UUID {} with another unit, but no UUID entry "
                          + "was found to replace.", sourceFile, sharedUUID);
                }
            } catch (IOException ex) {
                LOGGER.error(ex, "Could not give custom unit file {} a new unit-file UUID.", sourceFile);
            }
        }
        return repairedCount;
    }

    /**
     * Finds the custom unit files that need a new UUID: those under {@code userUnitDirectories} that share their UUID
     * with another unit file, are not the file chosen to keep it, and are named differently from that file.
     *
     * @param summaries           the unit summaries to check
     * @param userUnitDirectories the directories whose unit files may be rewritten
     *
     * @return the summaries of the custom unit files to repair
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<MekSummary> findCustomsToRepair(MekSummary[] summaries, UserUnitDirectories userUnitDirectories) {
        Map<String, List<MekSummary>> summariesByUUID = new HashMap<>();
        for (MekSummary summary : summaries) {
            String unitFileUUID = summary.getUnitFileUUID();
            if ((unitFileUUID == null) || unitFileUUID.isBlank() || summary.isBattlefieldSupportAsset()) {
                continue;
            }
            summariesByUUID.computeIfAbsent(unitFileUUID, key -> new ArrayList<>()).add(summary);
        }

        List<MekSummary> customsToRepair = new ArrayList<>();
        for (List<MekSummary> sharingSummaries : summariesByUUID.values()) {
            if (sharingSummaries.size() < 2) {
                continue;
            }

            MekSummary keeper = chooseUUIDKeeper(sharingSummaries, userUnitDirectories);
            for (MekSummary summary : sharingSummaries) {
                if ((summary != keeper)
                      && !Objects.equals(summary.getName(), keeper.getName())
                      && isRepairableCustom(summary, userUnitDirectories)) {
                    customsToRepair.add(summary);
                }
            }
        }
        return customsToRepair;
    }

    /**
     * Picks the file in a group sharing a UUID that keeps it: the first file that is not a repairable custom, or else
     * the least recently modified custom.
     */
    private static MekSummary chooseUUIDKeeper(List<MekSummary> sharingSummaries,
          UserUnitDirectories userUnitDirectories) {
        MekSummary oldestCustom = null;
        for (MekSummary summary : sharingSummaries) {
            if (!isRepairableCustom(summary, userUnitDirectories)) {
                return summary;
            }
            if ((oldestCustom == null)
                  || (summary.getSourceFile().lastModified() < oldestCustom.getSourceFile().lastModified())) {
                oldestCustom = summary;
            }
        }
        return oldestCustom;
    }

    private static boolean isRepairableCustom(MekSummary summary, UserUnitDirectories userUnitDirectories) {
        File sourceFile = summary.getSourceFile();
        if ((sourceFile == null) || (summary.getEntryName() != null) || !sourceFile.isFile()) {
            // Units inside archives cannot be rewritten in place
            return false;
        }
        String fileName = sourceFile.getName().toLowerCase();
        if (!fileName.endsWith(".mtf") && !fileName.endsWith(".blk")) {
            return false;
        }
        return userUnitDirectories.contains(sourceFile);
    }

    /**
     * Replaces the unit-file UUID in an MTF or BLK file in place, leaving every other line unchanged. When the file
     * has no refit-from reference yet, {@code refitFromUUID} is added directly after the UUID entry.
     *
     * @param unitFile      the {@code .mtf} or {@code .blk} file to rewrite
     * @param newUUID       the new unit-file UUID
     * @param refitFromUUID the refit-from reference to add if the file has none, or {@code null} to add none
     *
     * @return {@code true} if the file had a UUID entry and was rewritten
     *
     * @throws IOException if the file cannot be read or written
     * @author Illiani
     * @since 0.51.01
     */
    static boolean rewriteUnitFileUUID(File unitFile, String newUUID, @Nullable String refitFromUUID)
          throws IOException {
        String contents = Files.readString(unitFile.toPath(), FILE_CHARSET);
        String lineSeparator = contents.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(contents.split("\r?\n", -1)));

        boolean replaced = unitFile.getName().toLowerCase().endsWith(".mtf")
              ? rewriteMtfLines(lines, newUUID, refitFromUUID)
              : rewriteBlkLines(lines, newUUID, refitFromUUID);
        if (!replaced) {
            return false;
        }

        Files.writeString(unitFile.toPath(), String.join(lineSeparator, lines), FILE_CHARSET);
        return true;
    }

    private static boolean rewriteMtfLines(List<String> lines, String newUUID, @Nullable String refitFromUUID) {
        int uuidLineIndex = -1;
        boolean hasRefitFromUUID = false;
        for (int index = 0; index < lines.size(); index++) {
            String lowerCaseLine = lines.get(index).trim().toLowerCase();
            if ((uuidLineIndex < 0) && lowerCaseLine.startsWith(MtfFile.UUID)) {
                uuidLineIndex = index;
            } else if (lowerCaseLine.startsWith(MtfFile.REFIT_FROM_UUID)) {
                hasRefitFromUUID = true;
            }
        }
        if (uuidLineIndex < 0) {
            return false;
        }

        lines.set(uuidLineIndex, MtfFile.UUID + newUUID);
        if (!hasRefitFromUUID && (refitFromUUID != null)) {
            lines.add(uuidLineIndex + 1, MtfFile.REFIT_FROM_UUID + refitFromUUID);
        }
        return true;
    }

    private static boolean rewriteBlkLines(List<String> lines, String newUUID, @Nullable String refitFromUUID) {
        String uuidStartTag = '<' + BLKFile.UNIT_FILE_UUID + '>';
        String uuidEndTag = "</" + BLKFile.UNIT_FILE_UUID + '>';
        String refitFromStartTag = '<' + BLKFile.REFIT_FROM_UUID + '>';

        int uuidStartIndex = -1;
        boolean hasRefitFromUUID = false;
        for (int index = 0; index < lines.size(); index++) {
            String trimmedLine = lines.get(index).trim();
            if ((uuidStartIndex < 0) && trimmedLine.equalsIgnoreCase(uuidStartTag)) {
                uuidStartIndex = index;
            } else if (trimmedLine.equalsIgnoreCase(refitFromStartTag)) {
                hasRefitFromUUID = true;
            }
        }
        if ((uuidStartIndex < 0) || (uuidStartIndex + 2 >= lines.size())
              || !lines.get(uuidStartIndex + 2).trim().equalsIgnoreCase(uuidEndTag)) {
            return false;
        }

        lines.set(uuidStartIndex + 1, newUUID);
        if (!hasRefitFromUUID && (refitFromUUID != null)) {
            int insertIndex = uuidStartIndex + 3;
            lines.add(insertIndex, "");
            lines.add(insertIndex + 1, refitFromStartTag);
            lines.add(insertIndex + 2, refitFromUUID);
            lines.add(insertIndex + 3, "</" + BLKFile.REFIT_FROM_UUID + '>');
        }
        return true;
    }
}
