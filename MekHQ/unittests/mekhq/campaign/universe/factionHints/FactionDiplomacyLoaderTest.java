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

import static megamek.common.universe.Factions2.FACTIONS2_TEST_DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDate;

import megamek.common.universe.Factions2;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RegionPerimeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests loading the per-conflict faction diplomacy YAML files from
 * {@code testresources/data/universe/factionDiplomacy_loader_test/}. The fixture directory also contains a
 * deliberately malformed file ({@code broken.yml}); every assertion below implicitly verifies that the loader
 * survives it and keeps loading the remaining files.
 */
class FactionDiplomacyLoaderTest {
    private static final String LOADER_TEST_DIR = "testresources/data/universe/factionDiplomacy_loader_test";

    private Factions factions;
    private FactionHints hints;

    @BeforeEach
    void setUp() {
        assertDoesNotThrow(() -> {
            Factions2.setInstance(new Factions2(FACTIONS2_TEST_DIRECTORY));
            Factions.setInstance(Factions.loadDefault(true));
        });
        factions = Factions.getInstance();
        assertFalse(factions.getFactions().isEmpty(), "Factions list is empty");

        hints = new FactionHints();
        FactionDiplomacyLoader.load(hints, new File(LOADER_TEST_DIR));
    }

    private Faction getFaction(final String factionCode) {
        return factions.getFaction(factionCode);
    }

    @Test
    void testWarLoadedWithDateRangeAndName() {
        Faction federatedSuns = getFaction("FS");
        Faction capellans = getFaction("CC");

        assertTrue(hints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3028, 6, 1)));
        assertTrue(hints.isAtWarWith(capellans, federatedSuns, LocalDate.of(3028, 6, 1)));
        assertFalse(hints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3025, 12, 31)));
        assertFalse(hints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3031, 1, 1)));
        assertEquals("Test War", hints.getCurrentWar(federatedSuns, capellans, LocalDate.of(3028, 6, 1)));
    }

    @Test
    void testPartyLevelStartOverridesWarStart() {
        Faction lyrans = getFaction("LA");
        Faction capellans = getFaction("CC");

        assertFalse(hints.isAtWarWith(lyrans, capellans, LocalDate.of(3027, 1, 1)),
              "The LA-CC party overrides the war start to 3027-06-01");
        assertTrue(hints.isAtWarWith(lyrans, capellans, LocalDate.of(3027, 6, 1)));
    }

    @Test
    void testMultiFactionAllianceCreatesAllPairs() {
        Faction federatedSuns = getFaction("FS");
        Faction lyrans = getFaction("LA");
        Faction comStar = getFaction("CS");
        LocalDate inRange = LocalDate.of(3040, 1, 1);

        assertTrue(hints.isAlliedWith(federatedSuns, lyrans, inRange));
        assertTrue(hints.isAlliedWith(federatedSuns, comStar, inRange));
        assertTrue(hints.isAlliedWith(lyrans, comStar, inRange));
        assertFalse(hints.isAlliedWith(federatedSuns, lyrans, LocalDate.of(3058, 1, 1)));
        assertFalse(hints.isAlliedWith(federatedSuns, lyrans, LocalDate.of(3019, 12, 31)));
    }

    @Test
    void testRivalryLoadedWithOpenEnd() {
        Faction jadeFalcons = getFaction("CJF");
        Faction ghostBears = getFaction("CGB");

        assertFalse(hints.isRivalOf(jadeFalcons, ghostBears, LocalDate.of(3049, 12, 31)));
        assertTrue(hints.isRivalOf(jadeFalcons, ghostBears, LocalDate.of(3055, 1, 1)));
    }

    @Test
    void testNeutralFactionWithDatedException() {
        Faction mercenaries = getFaction("MERC");
        Faction marians = getFaction("MH");
        Faction capellans = getFaction("CC");

        assertTrue(hints.isNeutral(mercenaries));
        assertTrue(hints.isNeutral(mercenaries, marians, LocalDate.of(3039, 12, 31)),
              "The MH exception only starts 3040-01-01");
        assertFalse(hints.isNeutral(mercenaries, marians, LocalDate.of(3045, 1, 1)));
        assertTrue(hints.isNeutral(mercenaries, capellans, LocalDate.of(3045, 1, 1)));
    }

    @Test
    void testContainedFactionWithFractionAndOpponents() {
        Faction federatedSuns = getFaction("FS");
        Faction comStar = getFaction("CS");
        Faction capellans = getFaction("CC");
        Faction lyrans = getFaction("LA");
        LocalDate inRange = LocalDate.of(3035, 1, 1);

        assertTrue(hints.getContainedFactions(federatedSuns, inRange).contains(comStar));
        assertTrue(hints.getContainedFactions(federatedSuns, LocalDate.of(3029, 12, 31)).isEmpty());
        assertEquals(federatedSuns, hints.getContainedFactionHost(comStar, inRange));
        assertEquals(0.25, hints.getAltLocationFraction(federatedSuns, comStar, inRange), RegionPerimeter.EPSILON);
        assertTrue(hints.isContainedFactionOpponent(federatedSuns, comStar, capellans, inRange));
        assertFalse(hints.isContainedFactionOpponent(federatedSuns, comStar, lyrans, inRange));
    }

    @Test
    void testInvalidFactionCodeIsDroppedWithoutPoisoningValidParties() {
        Faction smokeJaguars = getFaction("CSJ");
        Faction ghostBears = getFaction("CGB");

        assertTrue(hints.isAtWarWith(smokeJaguars, ghostBears, LocalDate.of(3051, 1, 1)),
              "The valid CSJ-CGB pair must load even though ZZZ in the same party list is unknown");
    }

    @Test
    void testMultiFactionWarCreatesAllPairs() {
        Faction jadeFalcons = getFaction("CJF");
        Faction smokeJaguars = getFaction("CSJ");
        Faction diamondSharks = getFaction("CDS");
        LocalDate inRange = LocalDate.of(3070, 3, 1);

        assertTrue(hints.isAtWarWith(jadeFalcons, smokeJaguars, inRange));
        assertTrue(hints.isAtWarWith(jadeFalcons, diamondSharks, inRange));
        assertTrue(hints.isAtWarWith(smokeJaguars, diamondSharks, inRange));
    }

    @Test
    void testPartyLevelEndOverridesWarEnd() {
        Faction ghostBears = getFaction("CGB");
        Faction smokeJaguars = getFaction("CSJ");

        assertTrue(hints.isAtWarWith(ghostBears, smokeJaguars, LocalDate.of(3070, 6, 30)));
        assertFalse(hints.isAtWarWith(ghostBears, smokeJaguars, LocalDate.of(3070, 7, 1)),
              "The CGB-CSJ party overrides the war end to 3070-06-30");
    }

    @Test
    void testDateBoundsAreInclusive() {
        Faction jadeFalcons = getFaction("CJF");
        Faction smokeJaguars = getFaction("CSJ");

        assertTrue(hints.isAtWarWith(jadeFalcons, smokeJaguars, LocalDate.of(3070, 1, 1)),
              "The exact start day is in range");
        assertTrue(hints.isAtWarWith(jadeFalcons, smokeJaguars, LocalDate.of(3071, 12, 31)),
              "The exact end day is in range");
        assertFalse(hints.isAtWarWith(jadeFalcons, smokeJaguars, LocalDate.of(3069, 12, 31)));
        assertFalse(hints.isAtWarWith(jadeFalcons, smokeJaguars, LocalDate.of(3072, 1, 1)));
    }

    @Test
    void testCivilWarMakesFactionItsOwnEnemy() {
        Faction capellans = getFaction("CC");

        assertTrue(hints.isAtWarWith(capellans, capellans, LocalDate.of(3060, 6, 1)));
        assertFalse(hints.isAtWarWith(capellans, capellans, LocalDate.of(3061, 1, 1)));
    }

    @Test
    void testActiveWarOverridesNeutrality() {
        Faction mercenaries = getFaction("MERC");
        Faction capellans = getFaction("CC");

        assertFalse(hints.isNeutral(mercenaries, capellans, LocalDate.of(3055, 6, 1)),
              "MERC neutrality is overridden while the Neutrality Override War is active");
        assertTrue(hints.isNeutral(mercenaries, capellans, LocalDate.of(3057, 1, 1)),
              "Neutrality resumes once the war ends");
    }

    @Test
    void testSameFactionPairMergesAcrossFiles() {
        Faction federatedSuns = getFaction("FS");
        Faction capellans = getFaction("CC");

        assertTrue(hints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3090, 6, 1)),
              "The Later Test War from secondTestWar.yml applies alongside Test War from conflicts.yml");
        assertEquals("Later Test War", hints.getCurrentWar(federatedSuns, capellans, LocalDate.of(3090, 6, 1)));
        assertFalse(hints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3085, 1, 1)),
              "The gap between the two wars is peaceful");
    }

    @Test
    void testOmittedFractionIsNoSplitSentinel() {
        Faction lyrans = getFaction("LA");
        Faction marians = getFaction("MH");
        LocalDate inRange = LocalDate.of(3045, 1, 1);

        assertTrue(hints.getContainedFactions(lyrans, inRange).contains(marians));
        assertEquals(0.0, hints.getAltLocationFraction(lyrans, marians, inRange), RegionPerimeter.EPSILON,
              "An omitted fraction resolves to the 0.0 no-split sentinel");
    }

    @Test
    void testFullFractionTransfersWholeBorder() {
        Faction ghostBears = getFaction("CGB");
        Faction bloodSpirits = getFaction("CBS");
        LocalDate inRange = LocalDate.of(3062, 1, 1);

        assertEquals(1.0, hints.getAltLocationFraction(ghostBears, bloodSpirits, inRange),
              RegionPerimeter.EPSILON);
    }

    @Test
    void testContainedFactionWithoutOpponentsAllowsAnyoneExceptItself() {
        Faction lyrans = getFaction("LA");
        Faction marians = getFaction("MH");
        Faction capellans = getFaction("CC");
        LocalDate inRange = LocalDate.of(3045, 1, 1);

        assertTrue(hints.isContainedFactionOpponent(lyrans, marians, capellans, inRange),
              "Without an opponents list, any faction is a valid opponent");
        assertFalse(hints.isContainedFactionOpponent(lyrans, marians, marians, inRange),
              "A contained faction is not its own opponent unless it is in a civil war");
    }

    @Test
    void testMissingDirectoryLoadsNothingWithoutThrowing() {
        FactionHints emptyHints = new FactionHints();
        assertDoesNotThrow(() -> FactionDiplomacyLoader.load(emptyHints,
              new File("testresources/data/universe/factionDiplomacy_does_not_exist")));

        Faction federatedSuns = getFaction("FS");
        Faction capellans = getFaction("CC");
        assertFalse(emptyHints.isAtWarWith(federatedSuns, capellans, LocalDate.of(3028, 6, 1)));
    }

    @Test
    void testLoadReportsWhetherAnyYamlDataWasFound() {
        assertTrue(FactionDiplomacyLoader.load(new FactionHints(), new File(LOADER_TEST_DIR)),
              "A directory with readable YAML files must report success");
        assertFalse(FactionDiplomacyLoader.load(new FactionHints(),
                    new File("testresources/data/universe/factionDiplomacy_does_not_exist")),
              "A missing directory must report no data so the caller can fall back to the legacy XML");
    }

    @Test
    void testLoadReportsNoDataForDirectoryWithoutYamlFiles(@TempDir File emptyDirectory) {
        assertFalse(FactionDiplomacyLoader.load(new FactionHints(), emptyDirectory),
              "A directory without any YAML files must report no data so the caller can fall back to the legacy XML");
    }

    @Test
    void testCustomLegacyXmlIsDetectedAsDifferent() {
        File customLegacyXml = new File(LOADER_TEST_DIR, "factionhints_custom_test.xml");

        assertTrue(hints.legacyXmlDiffers(customLegacyXml),
              "The Homebrew War only exists in the player's XML, so the file must be flagged as custom");
    }

    @Test
    void testStockLegacyXmlIsNotFlaggedAsCustom() {
        FactionHints.initializeTestInstance();

        assertFalse(FactionHints.getInstance().isCustomLegacyDataDetected(),
              "factionhints_test.xml mirrors the factionDiplomacy_test YAML directory, so no custom data warning "
                    + "may fire; if this fails, the two test fixtures have drifted apart");
    }

    @Test
    void testInitializeTestInstanceLoadsYamlDirectory() {
        FactionHints.initializeTestInstance();
        FactionHints testInstance = FactionHints.getInstance();
        Faction federatedSuns = getFaction("FS");
        Faction lyrans = getFaction("LA");
        Faction capellans = getFaction("CC");

        assertTrue(testInstance.isAlliedWith(federatedSuns, lyrans, LocalDate.of(3030, 1, 1)));
        assertTrue(testInstance.isAtWarWith(federatedSuns, capellans, LocalDate.of(3028, 1, 1)));
        assertEquals("Fourth Succession War",
              testInstance.getCurrentWar(federatedSuns, capellans, LocalDate.of(3028, 1, 1)));
    }
}
