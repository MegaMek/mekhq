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
package mekhq.campaign.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * Decides whether a design built in the unit editor is the same as a unit already saved under the name the player
 * picked for it. A custom refit normally refuses a name that is already taken, but when the saved unit is the very
 * same design (for example a second foot platoon converted into the same custom jump infantry) the refit can simply
 * use it instead of saving a copy under a new name.
 *
 * <p>Two designs are the same when their unit files match line for line, leaving out what only names or describes
 * the unit: chassis, model, file identifiers, year, source, role and the descriptive text.</p>
 */
public final class RefitDesignMatch {
    private static final MMLogger LOGGER = MMLogger.create(RefitDesignMatch.class);

    /** Unit file entries, in either format, that name or describe a unit rather than define it. */
    private static final Set<String> DESCRIPTIVE_ENTRIES = Set.of("name", "chassis", "clanname", "model", "uuid",
          "refitfromuuid", "mul id", "generator", "year", "originalbuildyear", "era", "original era", "source",
          "published", "role", "missionroles", "availability", "faction", "bv", "overview", "capabilities",
          "deployment", "history", "manufacturer", "primaryfactory", "systemmanufacturer", "systemmanufacturers",
          "systemmode", "systemmodels", "notes", "fluffdate", "fluffimage", "icon");

    private RefitDesignMatch() {}

    /**
     * Looks for a unit already saved under the designed unit's chassis and model that is the same design.
     *
     * @param designed the unit as built in the unit editor, already given the name the player picked
     *
     * @return the saved unit's summary if one exists under that name and is the same design, otherwise {@code null}
     */
    public static @Nullable MekSummary findSameSavedDesign(Entity designed) {
        String name = designed.getShortNameRaw();
        MekSummary summary = MekSummaryCache.getInstance().getMek(name);
        if (summary == null) {
            return null;
        }
        try {
            Entity saved = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            if (isSameDesign(designed, saved)) {
                LOGGER.debug("[Refit] {}: the name is taken by the same design in {}", name, summary.getSourceFile());
                return summary;
            }
            LOGGER.debug("[Refit] {}: the name is taken by a different design in {}", name, summary.getSourceFile());
        } catch (EntityLoadingException exception) {
            LOGGER.warn(exception, "[Refit] {}: could not read the saved unit {} to compare it", name,
                  summary.getSourceFile());
        }
        return null;
    }

    /**
     * @param first  one design
     * @param second the other design
     *
     * @return {@code true} if both are the same kind of unit and their unit files match apart from descriptive
     *       entries
     */
    public static boolean isSameDesign(Entity first, Entity second) {
        if (first.getClass() != second.getClass()) {
            return false;
        }
        try {
            return definingLines(first).equals(definingLines(second));
        } catch (EntitySavingException exception) {
            LOGGER.warn(exception, "[Refit] could not write {} or {} out to compare them", first.getShortNameRaw(),
                  second.getShortNameRaw());
            return false;
        }
    }

    /**
     * @return the unit file of the entity as lines, without blank lines, comments and descriptive entries
     */
    private static List<String> definingLines(Entity entity) throws EntitySavingException {
        if (entity instanceof Mek mek) {
            return definingMekFileLines(mek.getMtf().split("\n"));
        }
        return definingBlockFileLines(BLKFile.getBlock(entity).getAllDataAsString());
    }

    /**
     * @param entry an entry name as written in a unit file; some block file entries end in a colon, such as
     *              {@code mul id:}
     */
    private static boolean isDescriptive(String entry) {
        String normalizedEntry = entry.trim().toLowerCase(Locale.ROOT);
        if (normalizedEntry.endsWith(":")) {
            normalizedEntry = normalizedEntry.substring(0, normalizedEntry.length() - 1);
        }
        return DESCRIPTIVE_ENTRIES.contains(normalizedEntry);
    }

    /** A Mek file has one entry per line, as {@code key:value}. */
    private static List<String> definingMekFileLines(String[] lines) {
        List<String> definingLines = new ArrayList<>();
        for (String line : lines) {
            String trimmedLine = line.trim();
            boolean isBlankOrComment = trimmedLine.isEmpty() || trimmedLine.startsWith("#");
            int separator = trimmedLine.indexOf(':');
            boolean isDescriptive = (separator > 0) && isDescriptive(trimmedLine.substring(0, separator));
            if (!isBlankOrComment && !isDescriptive) {
                definingLines.add(trimmedLine);
            }
        }
        return definingLines;
    }

    /** A block file wraps each entry's values in {@code <key>} and {@code </key>} lines. */
    private static List<String> definingBlockFileLines(String[] lines) {
        List<String> definingLines = new ArrayList<>();
        String skippedEntry = null;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (skippedEntry != null) {
                if (trimmedLine.equalsIgnoreCase("</" + skippedEntry + ">")) {
                    skippedEntry = null;
                }
                continue;
            }
            boolean isOpeningTag = trimmedLine.startsWith("<") && !trimmedLine.startsWith("</")
                  && trimmedLine.endsWith(">");
            if (isOpeningTag) {
                String entry = trimmedLine.substring(1, trimmedLine.length() - 1);
                if (isDescriptive(entry)) {
                    skippedEntry = entry;
                    continue;
                }
            }
            boolean isBlankOrComment = trimmedLine.isEmpty() || trimmedLine.startsWith("#");
            if (!isBlankOrComment) {
                definingLines.add(trimmedLine);
            }
        }
        return definingLines;
    }
}
