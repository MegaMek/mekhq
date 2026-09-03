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
package mekhq.campaign.unit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * Keeps a faction's unit tech progression map on disk between sessions.
 *
 * <p>Working the map out means loading every unit file in the game, which costs several seconds of CPU on every
 * campaign load. The result only changes when the unit data or the program version changes, so the map is written to
 * {@code userdata/cache} together with a fingerprint of what it was computed from, and reused while that fingerprint
 * still matches.</p>
 */
final class UnitTechProgressionCache {
    private static final MMLogger LOGGER = MMLogger.create(UnitTechProgressionCache.class);

    private static final String CACHE_DIRECTORY = "cache";
    private static final String FILE_PREFIX = "techProgression-";
    private static final String FILE_SUFFIX = ".cache";

    private UnitTechProgressionCache() {
    }

    /**
     * What a cached map was computed from. Any difference between the stored and the current fingerprint means the
     * map is stale.
     *
     * @param suiteVersion      the MekHQ version that produced the map
     * @param unitCacheModified last-modified time of MegaMek's unit cache file
     * @param unitCacheLength   size in bytes of MegaMek's unit cache file
     * @param unitCount         number of units in the unit cache
     */
    record Fingerprint(String suiteVersion, long unitCacheModified, long unitCacheLength, int unitCount)
          implements Serializable {
    }

    /**
     * @param allUnits every unit currently in the unit cache
     *
     * @return the fingerprint a map computed right now would carry
     */
    static Fingerprint currentFingerprint(MekSummary[] allUnits) {
        File unitCache = new File(MekSummaryCache.getUnitCacheDir(), MekSummaryCache.FILENAME_UNITS_CACHE);
        return new Fingerprint(MHQConstants.VERSION.toString(),
              unitCache.lastModified(),
              unitCache.length(),
              allUnits.length);
    }

    /**
     * @param techFaction the faction the map was computed for
     *
     * @return where that faction's map is kept
     */
    static File cacheFile(Faction techFaction) {
        File cacheDirectory = new File(Configuration.userDataDir(), CACHE_DIRECTORY);
        return new File(cacheDirectory, FILE_PREFIX + techFaction.name() + FILE_SUFFIX);
    }

    /**
     * Reads a cached map back, provided it was computed from the same unit data and version.
     *
     * @param cacheFile   the file to read
     * @param expected    the fingerprint the map must carry to be usable
     * @param unitsByName the units currently in the unit cache, by their summary name
     *
     * @return the cached map, or {@code null} when there is no usable cache and the map must be recalculated
     */
    static @Nullable Map<MekSummary, ITechnology> load(File cacheFile, Fingerprint expected,
          Map<String, MekSummary> unitsByName) {
        if (!cacheFile.isFile()) {
            LOGGER.info("[TechProgression] No cache at {}; calculating", cacheFile);
            return null;
        }

        // The file stream is its own resource so it is closed even when the object stream refuses the file's header
        try (FileInputStream fileInput = new FileInputStream(cacheFile);
              ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(fileInput))) {
            Object storedFingerprint = input.readObject();
            if (!expected.equals(storedFingerprint)) {
                LOGGER.info("[TechProgression] {} was built from other unit data or another version; recalculating",
                      cacheFile.getName());
                return null;
            }

            int entryCount = input.readInt();
            Map<MekSummary, ITechnology> map = new HashMap<>(entryCount * 2);
            int unknownUnitCount = 0;
            for (int index = 0; index < entryCount; index++) {
                String unitName = input.readUTF();
                ITechnology progression = (ITechnology) input.readObject();
                MekSummary summary = unitsByName.get(unitName);
                if (summary == null) {
                    unknownUnitCount++;
                    continue;
                }
                map.put(summary, progression);
            }
            LOGGER.info("[TechProgression] Loaded {} entries from {}; {} named units are no longer in the unit cache",
                  map.size(), cacheFile.getName(), unknownUnitCount);
            return map;
        } catch (IOException | ClassNotFoundException | ClassCastException exception) {
            LOGGER.warn(exception, "[TechProgression] Could not read {}; recalculating", cacheFile);
            return null;
        }
    }

    /**
     * Writes a map out for the next session. Units whose progression could not be calculated are left out, so they
     * are tried again next time.
     *
     * @param cacheFile   the file to write
     * @param fingerprint what the map was computed from
     * @param map         the map to keep
     */
    static void save(File cacheFile, Fingerprint fingerprint, Map<MekSummary, ITechnology> map) {
        File directory = cacheFile.getParentFile();
        boolean directoryMissing = (directory != null) && !directory.isDirectory();
        if (directoryMissing && !directory.mkdirs()) {
            LOGGER.warn("[TechProgression] Could not create {}; the map will be recalculated next time", directory);
            return;
        }

        Map<String, ITechnology> storable = new HashMap<>();
        for (Map.Entry<MekSummary, ITechnology> entry : map.entrySet()) {
            if (entry.getValue() instanceof Serializable) {
                storable.put(entry.getKey().getName(), entry.getValue());
            }
        }

        try (FileOutputStream fileOutput = new FileOutputStream(cacheFile);
              ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(fileOutput))) {
            output.writeObject(fingerprint);
            output.writeInt(storable.size());
            for (Map.Entry<String, ITechnology> entry : storable.entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeObject(entry.getValue());
            }
            LOGGER.info("[TechProgression] Wrote {} entries to {}", storable.size(), cacheFile);
        } catch (IOException exception) {
            LOGGER.warn(exception, "[TechProgression] Could not write {}", cacheFile);
        }
    }
}
