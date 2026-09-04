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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import megamek.common.CompositeTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MekSummary;
import mekhq.campaign.unit.UnitTechProgressionCache.Fingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The on-disk tech progression map is only ever a shortcut: it must come back exactly as written when the unit data
 * is unchanged, and must be refused, never half-used, when anything it was built from has changed.
 */
class UnitTechProgressionCacheTest {
    private static final Fingerprint FINGERPRINT = new Fingerprint("0.51.01", 1000L, 2000L, 3);

    @TempDir
    Path tempDir;

    @Test
    void mapComesBackWhenTheFingerprintMatches() {
        MekSummary atlas = summary("Atlas AS7-D");
        Map<MekSummary, ITechnology> map = new HashMap<>();
        map.put(atlas, progression(3025));
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();

        UnitTechProgressionCache.save(cacheFile, FINGERPRINT, map);
        Map<MekSummary, ITechnology> loaded = UnitTechProgressionCache.load(cacheFile, FINGERPRINT,
              Map.of("Atlas AS7-D", atlas));

        assertNotNull(loaded);
        assertEquals(3025, loaded.get(atlas).getIntroductionDate());
    }

    @Test
    void mapIsRefusedWhenTheFingerprintDiffers() {
        MekSummary atlas = summary("Atlas AS7-D");
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();
        UnitTechProgressionCache.save(cacheFile, FINGERPRINT, Map.of(atlas, progression(3025)));

        Fingerprint newerUnitData = new Fingerprint("0.51.01", 1000L, 2001L, 3);

        assertNull(UnitTechProgressionCache.load(cacheFile, newerUnitData, Map.of("Atlas AS7-D", atlas)));
    }

    @Test
    void unitsNoLongerInTheUnitCacheAreDropped() {
        MekSummary atlas = summary("Atlas AS7-D");
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();
        UnitTechProgressionCache.save(cacheFile, FINGERPRINT, Map.of(atlas, progression(3025)));

        Map<MekSummary, ITechnology> loaded = UnitTechProgressionCache.load(cacheFile, FINGERPRINT, Map.of());

        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void unitsThatCouldNotBeCalculatedAreNotWritten() {
        MekSummary atlas = summary("Atlas AS7-D");
        MekSummary broken = summary("Broken BRK-1");
        Map<MekSummary, ITechnology> map = new HashMap<>();
        map.put(atlas, progression(3025));
        map.put(broken, null);
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();

        UnitTechProgressionCache.save(cacheFile, FINGERPRINT, map);
        Map<MekSummary, ITechnology> loaded = UnitTechProgressionCache.load(cacheFile, FINGERPRINT,
              Map.of("Atlas AS7-D", atlas, "Broken BRK-1", broken));

        assertNotNull(loaded);
        assertFalse(loaded.containsKey(broken), "a missing entry is recalculated on demand rather than cached as null");
    }

    @Test
    void unreadableFileIsRefused() throws Exception {
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();
        Files.writeString(cacheFile.toPath(), "not a cache");

        assertNull(UnitTechProgressionCache.load(cacheFile, FINGERPRINT, Map.of()));
    }

    @Test
    void missingFileIsRefused() {
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();

        assertNull(UnitTechProgressionCache.load(cacheFile, FINGERPRINT, Map.of()));
    }

    @Test
    void implausibleEntryCountIsRefusedBeforeAnythingIsAllocated() throws Exception {
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();
        try (FileOutputStream fileOutput = new FileOutputStream(cacheFile);
              ObjectOutputStream output = new ObjectOutputStream(fileOutput)) {
            output.writeObject(FINGERPRINT);
            output.writeInt(Integer.MAX_VALUE);
        }

        assertNull(UnitTechProgressionCache.load(cacheFile, FINGERPRINT, Map.of()));
    }

    @Test
    void entryOfAClassAProgressionIsNotMadeOfIsRefused() throws Exception {
        MekSummary atlas = summary("Atlas AS7-D");
        File cacheFile = tempDir.resolve("techProgression-IS.cache").toFile();
        try (FileOutputStream fileOutput = new FileOutputStream(cacheFile);
              ObjectOutputStream output = new ObjectOutputStream(fileOutput)) {
            output.writeObject(FINGERPRINT);
            output.writeInt(1);
            output.writeUTF("Atlas AS7-D");
            output.writeObject(new File("not a tech level"));
        }

        assertNull(UnitTechProgressionCache.load(cacheFile, FINGERPRINT, Map.of("Atlas AS7-D", atlas)));
    }

    @Test
    void cacheFileIsNamedForTheFaction() {
        assertEquals("techProgression-CLAN.cache", UnitTechProgressionCache.cacheFile(Faction.CLAN).getName());
    }

    private static MekSummary summary(String name) {
        MekSummary summary = mock(MekSummary.class);
        when(summary.getName()).thenReturn(name);
        return summary;
    }

    private static CompositeTechLevel progression(int introYear) {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS)
                                            .setAdvancement(introYear, introYear + 5, introYear + 10);
        return new CompositeTechLevel(advancement, false, false, introYear, Faction.IS);
    }
}
