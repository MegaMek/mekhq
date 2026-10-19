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
package mekhq.campaign.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import mekhq.campaign.utilities.EasyBugReport.BugReportArchiveResult;
import mekhq.campaign.utilities.EasyBugReport.BugReportCandidate;
import mekhq.campaign.utilities.EasyBugReport.DropGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks what MekHQ's bug report collects and how it is split: the save and live logs first, the newest reports next,
 * rolled logs last and dropped before another archive part is added.
 */
class EasyBugReportTest {
    private static final String GAME_LOG_FILE_NAME = "gamelog.html";
    private static final String AUTO_RESOLVE_FILE_NAME = "simulation.html";
    private static final String MINIMAP_PATH = "gameSummaries/minimap";
    private static final String NEWEST_GAME_UUID = "5f0c7c1e-8a1b-4d2e-9f3a-0b1c2d3e4f50";
    private static final String OLDER_GAME_UUID = "0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";

    @TempDir
    private Path temporaryDirectory;

    private File logDirectory;
    private File campaignFile;
    private File savegameDirectory;
    private final Random random = new Random(9233L);

    @BeforeEach
    void setUp() throws IOException {
        logDirectory = temporaryDirectory.resolve("logs").toFile();
        assertTrue(logDirectory.mkdirs(), "test fixture: could not create the log directory");
        campaignFile = writeFile(temporaryDirectory.toFile(), "Campaign30250101_Ghost.cpnx.gz", 100);
        savegameDirectory = temporaryDirectory.resolve("savegames").toFile();
        assertTrue(savegameDirectory.mkdirs(), "test fixture: could not create the savegame directory");
    }

    @Test
    void collectsTheNewestBattleAutosaveRightAfterTheCampaign() throws IOException {
        writeFile(logDirectory, "mekhq.log", 10);
        writeFile(savegameDirectory, "autosave.sav.gz", 10, 1_000_000L);
        writeFile(savegameDirectory, "Round-3-autosave2026-10-17_11-05-00.sav.gz", 10, 2_000_000L);
        writeFile(savegameDirectory, "Round-2-autosave2026-10-17_11-01-00.sav.gz", 10, 1_500_000L);
        // A save the player named, and an autosave still being written
        writeFile(savegameDirectory, "Lyran Assault.sav.gz", 10, 9_000_000L);
        writeFile(savegameDirectory, "autosave2026-10-17_11-06-00.sav.gz", 0);

        List<BugReportCandidate> candidates = gather(logDirectory);

        assertEquals(List.of(campaignFile.getName(),
              "battle-autosave/Round-3-autosave2026-10-17_11-05-00.sav.gz",
              "logs/mekhq.log"), entryNamesOf(candidates));
        assertEquals(DropGroup.REQUIRED, candidates.get(1).dropGroup());
    }

    @Test
    void collectsInPriorityOrderAndIgnoresTheClutter() throws IOException {
        for (String baseName : EasyBugReport.LOG_BASE_NAMES) {
            writeFile(logDirectory, baseName + ".log", 10);
        }
        writeFile(logDirectory, "megameklab.log", 0); // declared by log4j2.xml but empty when MML never ran
        writeFile(logDirectory, "gamelog_2026-10-01_10-00-00.html", 10, 1_000_000L);
        writeFile(logDirectory, "gamelog_2026-10-02_10-00-00.html", 10, 2_000_000L);
        writeFile(logDirectory, "simulation.html", 10, 1_500_000L);
        writeFile(logDirectory, "princess_1.log.gz", 10, 1_000_000L);
        writeFile(logDirectory, "mekhq_1.log.gz", 10, 3_000_000L);
        writeFile(logDirectory, "megamek_2.log.gz", 10, 2_000_000L);
        // Clutter that must never be collected
        writeFile(logDirectory, "Bot_Clan Jade Falcon OpFor_0.mul", 10);
        writeFile(logDirectory, "Bot_Clan Jade Falcon OpFor_1.mul", 10);
        writeFile(logDirectory, "other.log", 10);
        writeFile(logDirectory, "unrelated_1.log.gz", 10);
        writeFile(logDirectory, "ai_match_results_1.tsv", 10);
        writeFile(logDirectory, "game_actions_1.tsv", 10, 1_000_000L);
        writeFile(logDirectory, "game_actions_2.tsv", 10, 2_000_000L);

        List<BugReportCandidate> candidates = gather(logDirectory);

        List<String> expected = List.of(campaignFile.getName(),
              "logs/mekhq.log",
              "logs/megamek.log",
              "logs/princess.log",
              "logs/unified_log.log",
              "logs/bot_path_ranker.log",
              "logs/gamelog_2026-10-02_10-00-00.html",
              "logs/simulation.html",
              "logs/game_actions_2.tsv",
              "logs/mekhq_1.log.gz",
              "logs/megamek_2.log.gz",
              "logs/princess_1.log.gz");
        assertEquals(expected, entryNamesOf(candidates));
    }

    @Test
    void eachFileKindHasItsDropGroup() throws IOException {
        writeFile(logDirectory, "mekhq.log", 10);
        writeFile(logDirectory, "gamelog.html", 10);
        writeFile(logDirectory, "game_actions_1.tsv", 10);
        writeFile(logDirectory, "mekhq_1.log.gz", 10);
        File gameDirectory = newGameSummaryDirectory(NEWEST_GAME_UUID, 2_000_000L);
        writeFile(gameDirectory, "round_1_04_MOVEMENT.png", 10);
        writeFile(gameDirectory, NEWEST_GAME_UUID + ".gif", 10);

        List<DropGroup> dropGroups = new ArrayList<>();
        for (BugReportCandidate candidate : gather(logDirectory)) {
            dropGroups.add(candidate.dropGroup());
        }

        assertEquals(List.of(DropGroup.REQUIRED,
              DropGroup.REQUIRED,
              DropGroup.REQUIRED,
              DropGroup.GAME_DATASET,
              DropGroup.ROLLED_LOG,
              DropGroup.MINIMAP_FRAME,
              DropGroup.MINIMAP_RECORDING), dropGroups);
    }

    @Test
    void collectsOnlyTheNewestGameMinimapFolderWithItsFileNames() throws IOException {
        File olderGame = newGameSummaryDirectory(OLDER_GAME_UUID, 1_000_000L);
        writeFile(olderGame, "round_9_18_FIRING.png", 10);
        File newestGame = newGameSummaryDirectory(NEWEST_GAME_UUID, 2_000_000L);
        writeFile(newestGame, "round_1_18_FIRING.png", 10, 1_000_000L);
        writeFile(newestGame, "round_2_04_MOVEMENT.png", 10, 2_000_000L);
        writeFile(newestGame, NEWEST_GAME_UUID + ".gif", 10, 3_000_000L);
        writeFile(newestGame, "round_2_18_FIRING.png", 0); // a frame still being written
        // Adding files moves a folder's time, so set the folder times last
        assertTrue(olderGame.setLastModified(1_000_000L), "test fixture: could not set a modification time");
        assertTrue(newestGame.setLastModified(2_000_000L), "test fixture: could not set a modification time");

        List<BugReportCandidate> candidates = gather(logDirectory);

        String prefix = "logs/gameSummaries/minimap/" + NEWEST_GAME_UUID + "/";
        assertEquals(List.of(campaignFile.getName(),
              prefix + "round_2_04_MOVEMENT.png",
              prefix + "round_1_18_FIRING.png",
              prefix + NEWEST_GAME_UUID + ".gif"), entryNamesOf(candidates));
    }

    @Test
    void dropsTheGifFirstThenTheOldestRounds() throws IOException {
        campaignFile = writeFile(temporaryDirectory.toFile(), "Campaign30250101_Ghost.cpnx.gz", 4_000);
        // Too big for what is left, but the minimap images after it still get their chance
        writeFile(logDirectory, "mekhq_1.log.gz", 30_000);
        File gameDirectory = newGameSummaryDirectory(NEWEST_GAME_UUID, 2_000_000L);
        writeFile(gameDirectory, "round_1_18_FIRING.png", 3_000, 1_000_000L);
        writeFile(gameDirectory, "round_2_18_FIRING.png", 3_000, 2_000_000L);
        writeFile(gameDirectory, "round_3_18_FIRING.png", 3_000, 3_000_000L);
        writeFile(gameDirectory, NEWEST_GAME_UUID + ".gif", 3_000, 4_000_000L);

        BugReportArchiveResult result = EasyBugReport.createBugReportArchive(gather(logDirectory),
              temporaryDirectory.resolve("report.zip").toFile(),
              15_000L);

        String prefix = "logs/gameSummaries/minimap/" + NEWEST_GAME_UUID + "/";
        assertEquals(1, result.parts().size(), "optional files must never start another part");
        assertEquals(List.of(campaignFile.getName(),
              prefix + "round_3_18_FIRING.png",
              prefix + "round_2_18_FIRING.png"), result.includedEntries());
        assertEquals(List.of("mekhq_1.log.gz", "round_1_18_FIRING.png", NEWEST_GAME_UUID + ".gif"),
              result.droppedEntries());
        assertEquals(result.includedEntries(), zipEntryNamesOf(result.parts().get(0)));
    }

    @Test
    void readsReportsFromARelocatedReportDirectory() throws IOException {
        File reportDirectory = temporaryDirectory.resolve("reports").toFile();
        assertTrue(reportDirectory.mkdirs(), "test fixture: could not create the report directory");
        writeFile(logDirectory, "mekhq.log", 10);
        writeFile(logDirectory, "gamelog.html", 10); // stale copy where the reports used to go
        writeFile(reportDirectory, "gamelog.html", 10);
        writeFile(reportDirectory, "simulation_2026-10-01_10-00-00.html", 10, 1_000_000L);
        writeFile(reportDirectory, "simulation_2026-10-02_10-00-00.html", 10, 2_000_000L);

        List<BugReportCandidate> candidates = EasyBugReport.gatherCandidates(campaignFile,
              logDirectory,
              reportDirectory,
              GAME_LOG_FILE_NAME,
              AUTO_RESOLVE_FILE_NAME,
              new File(reportDirectory, MINIMAP_PATH),
              savegameDirectory);

        assertEquals(List.of(campaignFile.getName(),
              "logs/mekhq.log",
              "logs/gamelog.html",
              "logs/simulation_2026-10-02_10-00-00.html"), entryNamesOf(candidates));
        assertEquals(reportDirectory, candidates.get(2).source().getParentFile());
    }

    @Test
    void missingLogDirectoryStillCollectsTheSave() {
        File absentDirectory = temporaryDirectory.resolve("no-such-directory").toFile();

        List<BugReportCandidate> candidates = gather(absentDirectory);

        assertEquals(List.of(campaignFile.getName()), entryNamesOf(candidates));
    }

    @Test
    void dropsOlderRolledLogsInsteadOfAddingAPart() throws IOException {
        campaignFile = writeFile(temporaryDirectory.toFile(), "Campaign30250101_Ghost.cpnx.gz", 4_000);
        writeFile(logDirectory, "mekhq.log", 4_000);
        writeFile(logDirectory, "mekhq_1.log.gz", 5_000, 3_000_000L);
        writeFile(logDirectory, "megamek_1.log.gz", 5_000, 2_000_000L);
        // Small enough to fit in what is left, but older than one that did not fit, so it goes too
        writeFile(logDirectory, "princess_1.log.gz", 100, 1_000_000L);

        BugReportArchiveResult result = EasyBugReport.createBugReportArchive(gather(logDirectory),
              temporaryDirectory.resolve("report.zip").toFile(),
              20_000L);

        assertEquals(1, result.parts().size(), "rolled logs must never start another part");
        assertEquals(List.of(campaignFile.getName(), "logs/mekhq.log", "logs/mekhq_1.log.gz"),
              result.includedEntries());
        assertEquals(List.of("megamek_1.log.gz", "princess_1.log.gz"), result.droppedEntries());
        assertEquals(result.includedEntries(), zipEntryNamesOf(result.parts().get(0)));
    }

    @Test
    void requiredFilesStillSplitIntoAnotherPart() throws IOException {
        campaignFile = writeFile(temporaryDirectory.toFile(), "Campaign30250101_Ghost.cpnx.gz", 4_000);
        writeFile(logDirectory, "mekhq.log", 4_000);
        writeFile(logDirectory, "mekhq_1.log.gz", 100);

        BugReportArchiveResult result = EasyBugReport.createBugReportArchive(gather(logDirectory),
              temporaryDirectory.resolve("report.zip").toFile(),
              10_000L);

        assertEquals(2, result.parts().size());
        assertEquals(List.of(campaignFile.getName()), zipEntryNamesOf(result.parts().get(0)));
        assertEquals(List.of("logs/mekhq.log", "logs/mekhq_1.log.gz"), zipEntryNamesOf(result.parts().get(1)));
        assertTrue(result.droppedEntries().isEmpty());
    }

    private List<BugReportCandidate> gather(File textLogDirectory) {
        return EasyBugReport.gatherCandidates(campaignFile,
              textLogDirectory,
              textLogDirectory,
              GAME_LOG_FILE_NAME,
              AUTO_RESOLVE_FILE_NAME,
              new File(textLogDirectory, MINIMAP_PATH),
              savegameDirectory);
    }

    private File newGameSummaryDirectory(String gameUuid, long lastModified) {
        File gameDirectory = new File(new File(logDirectory, MINIMAP_PATH), gameUuid);
        assertTrue(gameDirectory.mkdirs(), "test fixture: could not create the minimap folder");
        assertTrue(gameDirectory.setLastModified(lastModified), "test fixture: could not set a modification time");
        return gameDirectory;
    }

    /** Writes incompressible content so the archive size follows the file size. */
    private File writeFile(File directory, String fileName, int length) throws IOException {
        File file = new File(directory, fileName);
        byte[] content = new byte[length];
        random.nextBytes(content);
        Files.write(file.toPath(), content);
        return file;
    }

    private File writeFile(File directory, String fileName, int length, long lastModified) throws IOException {
        File file = writeFile(directory, fileName, length);
        assertTrue(file.setLastModified(lastModified), "test fixture: could not set a modification time");
        return file;
    }

    private static List<String> entryNamesOf(List<BugReportCandidate> candidates) {
        List<String> entryNames = new ArrayList<>();
        for (BugReportCandidate candidate : candidates) {
            entryNames.add(candidate.entryName());
        }
        return entryNames;
    }

    private static List<String> zipEntryNamesOf(File archive) throws IOException {
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archive),
              StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }
        return entryNames;
    }
}
