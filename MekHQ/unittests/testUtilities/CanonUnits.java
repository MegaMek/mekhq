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
package testUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jakarta.annotation.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;

/**
 * Loads canon units for tests straight from the game's own unit data, so tests never need a copy of an mm-data unit
 * file that can fall out of date.
 *
 * <p>The build stages mm-data into {@code data/} before the tests run, including the unit archive this class reads.
 * A unit is found by the UUID in its file, which survives a rename or a move to another folder, and is then checked
 * against the name the test expects. The name keeps the test readable and catches a mistyped UUID; when a unit is
 * renamed, the failure says what it is now called.</p>
 *
 * <p>Use a hand-made file in {@code testresources} only for data that is not canon: a deliberately broken file, or
 * a unit edited to set up a case canon does not have.</p>
 */
public final class CanonUnits {
    private static final File UNIT_ARCHIVE = new File("data/mekfiles/unit_files.zip");
    /** Unit files put their UUID just after the license header, well inside this many lines. */
    private static final int HEADER_LINES_TO_SCAN = 60;
    private static final String MTF_UUID_PREFIX = "uuid:";
    private static final String BLK_UUID_TAG = "<UUID>";

    private static Map<String, String> entryNamesByUuid;
    private static Set<String> duplicatedUuids;

    private CanonUnits() {}

    /**
     * Loads a fresh entity for a canon unit. Each call parses the file again, so tests never share an entity.
     *
     * <p>Fails the calling test when no unit file has this UUID, the file cannot be parsed, or the unit is not called
     * {@code expectedName}. It never returns {@code null}.</p>
     *
     * @param expectedName the unit's name as chassis, Clan name in brackets, then model, for example
     *                     {@code "Skinwalker (Ryoken III) A"}
     * @param unitFileUuid the UUID in the unit's file
     *
     * @return the loaded entity
     */
    public static Entity loadUnit(String expectedName, String unitFileUuid) {
        String entryName = findEntryName(expectedName, unitFileUuid);
        Entity entity = parse(readEntry(entryName), entryName);
        assertEquals(expectedName, entity.getShortNameRaw(), () -> "Unit file UUID " + unitFileUuid + " ("
              + entryName + ") is now named " + entity.getShortNameRaw() + ", not " + expectedName
              + ". Update the name in the test.");
        return entity;
    }

    /**
     * Reads the text of a canon unit's file, for a test that edits the file before parsing it. The name is checked
     * the same way as {@link #loadUnit(String, String)}.
     *
     * @param expectedName the unit's name as chassis, Clan name in brackets, then model
     * @param unitFileUuid the UUID in the unit's file
     *
     * @return the file's text
     */
    public static String readUnitFile(String expectedName, String unitFileUuid) {
        loadUnit(expectedName, unitFileUuid);
        return new String(readEntry(findEntryName(expectedName, unitFileUuid)), StandardCharsets.UTF_8);
    }

    /**
     * @return the archive entry holding the unit file with this UUID; fails the test if there is none, or more than
     *       one
     */
    private static String findEntryName(String expectedName, String unitFileUuid) {
        Map<String, String> index = getIndex();
        assertTrue(!duplicatedUuids.contains(unitFileUuid),
              "More than one unit file in " + UNIT_ARCHIVE + " has UUID " + unitFileUuid);
        String entryName = index.get(unitFileUuid);
        assertNotNull(entryName, () -> "No unit file in " + UNIT_ARCHIVE.getAbsolutePath() + " has UUID "
              + unitFileUuid + " (expected " + expectedName + ")");
        return entryName;
    }

    /**
     * Builds the UUID index once per test run. Only the header of each file is read, so this takes a moment rather
     * than the time a full unit cache takes to parse every unit.
     */
    private static synchronized Map<String, String> getIndex() {
        if (entryNamesByUuid != null) {
            return entryNamesByUuid;
        }
        assertTrue(UNIT_ARCHIVE.isFile(), () -> "The unit archive " + UNIT_ARCHIVE.getAbsolutePath()
              + " is missing. Run ./gradlew :MekHQ:stageDataFiles, which the test task normally does first.");
        Map<String, String> index = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        try (ZipFile archive = new ZipFile(UNIT_ARCHIVE)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !isUnitFile(entry.getName())) {
                    continue;
                }
                String uuid;
                try (InputStream entryInput = archive.getInputStream(entry)) {
                    uuid = readUuid(entryInput);
                }
                if ((uuid != null) && (index.put(uuid, entry.getName()) != null)) {
                    duplicates.add(uuid);
                }
            }
        } catch (IOException exception) {
            fail("Could not index the unit archive " + UNIT_ARCHIVE.getAbsolutePath(), exception);
        }
        duplicatedUuids = Collections.unmodifiableSet(duplicates);
        entryNamesByUuid = Collections.unmodifiableMap(index);
        return entryNamesByUuid;
    }

    private static boolean isUnitFile(String entryName) {
        String lowerCaseName = entryName.toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".mtf") || lowerCaseName.endsWith(".blk");
    }

    /**
     * Finds the UUID in a unit file's header: a {@code uuid:} line in an MTF file, or the line after {@code <UUID>}
     * in a BLK file.
     *
     * @return the UUID, or {@code null} if the header has none
     */
    private static @Nullable String readUuid(InputStream entryInput) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(entryInput, StandardCharsets.UTF_8));
        for (int lineNumber = 0; lineNumber < HEADER_LINES_TO_SCAN; lineNumber++) {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            String trimmedLine = line.trim();
            if (trimmedLine.regionMatches(true, 0, MTF_UUID_PREFIX, 0, MTF_UUID_PREFIX.length())) {
                return trimmedLine.substring(MTF_UUID_PREFIX.length()).trim();
            }
            if (trimmedLine.equalsIgnoreCase(BLK_UUID_TAG)) {
                String uuidLine = reader.readLine();
                return (uuidLine == null) ? null : uuidLine.trim();
            }
        }
        return null;
    }

    private static byte[] readEntry(String entryName) {
        try (ZipFile archive = new ZipFile(UNIT_ARCHIVE)) {
            ZipEntry entry = archive.getEntry(entryName);
            try (InputStream entryInput = archive.getInputStream(entry)) {
                return entryInput.readAllBytes();
            }
        } catch (IOException exception) {
            fail("Could not read " + entryName + " from " + UNIT_ARCHIVE.getAbsolutePath(), exception);
            return new byte[0];
        }
    }

    private static Entity parse(byte[] unitFile, String entryName) {
        EquipmentType.initializeTypes();
        Entity entity = null;
        try {
            entity = new MekFileParser(new ByteArrayInputStream(unitFile), new File(entryName).getName()).getEntity();
        } catch (EntityLoadingException exception) {
            fail("Could not parse " + entryName + " from " + UNIT_ARCHIVE.getAbsolutePath(), exception);
        }
        assertNotNull(entity, () -> entryName + " parsed to no entity");
        return entity;
    }
}
