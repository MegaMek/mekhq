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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.loaders.MekSummary;
import megamek.common.util.UUIDUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Older MekHQ refits saved custom unit files that kept the unit-file UUID of the design they were made from. The
 * repair gives those custom files their own UUID in place and records the shared UUID as their refit-from reference.
 */
class CustomUnitFileUUIDRepairTest {
    @TempDir
    Path tempDirectory;

    private Path officialDirectory() {
        return tempDirectory.resolve("mekfiles");
    }

    private Path customsDirectory() {
        return officialDirectory().resolve("customs");
    }

    private Path userDataUnitDirectory() {
        return tempDirectory.resolve("userdata/data/mekfiles");
    }

    private Path externalUserDirectory() {
        return tempDirectory.resolve("external");
    }

    private CustomUnitFileUUIDRepair.UserUnitDirectories directories() {
        return new CustomUnitFileUUIDRepair.UserUnitDirectories(customsDirectory(), officialDirectory(),
              List.of(userDataUnitDirectory(), externalUserDirectory()));
    }

    private File writeFile(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.ISO_8859_1);
        return path.toFile();
    }

    private static String mtfContents(String model, String unitFileUUID) {
        return "generator:MegaMek Suite\nchassis:Charger\nmodel:" + model + "\nuuid:" + unitFileUUID
              + "\nmul id:1\n\nConfig:Biped\n";
    }

    private static String blkContents(String name, String unitFileUUID) {
        return "<BlockVersion>\n1\n</BlockVersion>\n\n<UnitType>\nTank\n</UnitType>\n\n<Name>\n" + name
              + "\n</Name>\n\n<UUID>\n" + unitFileUUID + "\n</UUID>\n\n<Model>\nCargo\n</Model>\n";
    }

    private static MekSummary summaryFor(File sourceFile, String unitFileUUID, String name) {
        MekSummary summary = new MekSummary();
        summary.setSourceFile(sourceFile);
        summary.setUnitFileUUID(unitFileUUID);
        summary.setName(name);
        return summary;
    }

    private static String readFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void mtfGetsNewUUIDAndRefitFromReference() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        String newUUID = UUIDUtil.newUUIDv7().toString();
        File custom = writeFile(customsDirectory().resolve("Campaign/Charger CGR-1A5 Mk II.mtf"),
              mtfContents("CGR-1A5 Mk II", sharedUUID));

        assertTrue(CustomUnitFileUUIDRepair.rewriteUnitFileUUID(custom, newUUID, sharedUUID));

        String expected = "generator:MegaMek Suite\nchassis:Charger\nmodel:CGR-1A5 Mk II\nuuid:" + newUUID
              + "\nrefitfromuuid:" + sharedUUID + "\nmul id:1\n\nConfig:Biped\n";
        assertEquals(expected, readFile(custom), "Only the UUID changes and the reference is added after it");
    }

    @Test
    void mtfKeepsWindowsLineEndingsAndExistingRefitFromReference() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        String existingReference = UUIDUtil.newUUIDv7().toString();
        String newUUID = UUIDUtil.newUUIDv7().toString();
        File custom = writeFile(customsDirectory().resolve("Charger.mtf"),
              "chassis:Charger\r\nuuid:" + sharedUUID + "\r\nrefitfromuuid:" + existingReference + "\r\n");

        assertTrue(CustomUnitFileUUIDRepair.rewriteUnitFileUUID(custom, newUUID, sharedUUID));

        assertEquals("chassis:Charger\r\nuuid:" + newUUID + "\r\nrefitfromuuid:" + existingReference + "\r\n",
              readFile(custom));
    }

    @Test
    void blkGetsNewUUIDAndRefitFromBlock() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        String newUUID = UUIDUtil.newUUIDv7().toString();
        File custom = writeFile(customsDirectory().resolve("Campaign/Condor Heavy Hover Tank Cargo.blk"),
              blkContents("Condor Heavy Hover Tank", sharedUUID));

        assertTrue(CustomUnitFileUUIDRepair.rewriteUnitFileUUID(custom, newUUID, sharedUUID));

        String expected = "<BlockVersion>\n1\n</BlockVersion>\n\n<UnitType>\nTank\n</UnitType>\n\n<Name>\n"
              + "Condor Heavy Hover Tank\n</Name>\n\n<UUID>\n" + newUUID + "\n</UUID>\n\n<refitFromUUID>\n"
              + sharedUUID + "\n</refitFromUUID>\n\n<Model>\nCargo\n</Model>\n";
        assertEquals(expected, readFile(custom));
    }

    @Test
    void fileWithoutUUIDIsLeftUnchanged() throws IOException {
        String contents = "chassis:Charger\nmodel:CGR-1A5 Mk II\n";
        File custom = writeFile(customsDirectory().resolve("Charger.mtf"), contents);

        assertFalse(CustomUnitFileUUIDRepair.rewriteUnitFileUUID(custom, UUIDUtil.newUUIDv7().toString(), null));
        assertEquals(contents, readFile(custom));
    }

    @Test
    void customsSharingCanonUUIDAreRepairedAndCanonIsNot() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File canon = writeFile(officialDirectory().resolve("Charger CGR-1A5.mtf"),
              mtfContents("CGR-1A5", sharedUUID));
        File firstCustom = writeFile(customsDirectory().resolve("Alpha/Charger CGR-1A5 Mk II.mtf"),
              mtfContents("CGR-1A5 Mk II", sharedUUID));
        File secondCustom = writeFile(customsDirectory().resolve("Beta/Charger CGR-1A5 Mk III.mtf"),
              mtfContents("CGR-1A5 Mk III", sharedUUID));
        File uniqueCustom = writeFile(customsDirectory().resolve("Alpha/Charger CGR-1A5 Mk IV.mtf"),
              mtfContents("CGR-1A5 Mk IV", UUIDUtil.newUUIDv7().toString()));
        MekSummary canonSummary = summaryFor(canon, sharedUUID, "Charger CGR-1A5");
        MekSummary firstCustomSummary = summaryFor(firstCustom, sharedUUID, "Charger CGR-1A5 Mk II");
        MekSummary secondCustomSummary = summaryFor(secondCustom, sharedUUID, "Charger CGR-1A5 Mk III");
        MekSummary uniqueCustomSummary = summaryFor(uniqueCustom, UUIDUtil.newUUIDv7().toString(),
              "Charger CGR-1A5 Mk IV");

        List<MekSummary> customsToRepair = CustomUnitFileUUIDRepair.findCustomsToRepair(
              new MekSummary[] { firstCustomSummary, canonSummary, secondCustomSummary, uniqueCustomSummary },
              directories());

        assertEquals(2, customsToRepair.size());
        assertTrue(customsToRepair.contains(firstCustomSummary));
        assertTrue(customsToRepair.contains(secondCustomSummary));
    }

    @Test
    void oldestCustomKeepsUUIDWhenOnlyCustomsShareIt() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File sourceCustom = writeFile(customsDirectory().resolve("Charger Custom.mtf"),
              mtfContents("Custom", sharedUUID));
        File refitCustom = writeFile(customsDirectory().resolve("Campaign/Charger Custom Mk II.mtf"),
              mtfContents("Custom Mk II", sharedUUID));
        assertTrue(sourceCustom.setLastModified(1_000_000L));
        assertTrue(refitCustom.setLastModified(2_000_000L));
        MekSummary refitSummary = summaryFor(refitCustom, sharedUUID, "Charger Custom Mk II");

        List<MekSummary> customsToRepair = CustomUnitFileUUIDRepair.findCustomsToRepair(
              new MekSummary[] { refitSummary, summaryFor(sourceCustom, sharedUUID, "Charger Custom") },
              directories());

        assertEquals(List.of(refitSummary), customsToRepair, "The design the refit was made from keeps the UUID");
    }

    @Test
    void repairGivesEachCustomItsOwnUUID() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File canon = writeFile(officialDirectory().resolve("Condor Heavy Hover Tank.blk"),
              blkContents("Condor Heavy Hover Tank", sharedUUID));
        String canonContents = readFile(canon);
        File firstCustom = writeFile(customsDirectory().resolve("Alpha/Condor Heavy Hover Tank Cargo.blk"),
              blkContents("Condor Heavy Hover Tank", sharedUUID));
        File secondCustom = writeFile(userDataUnitDirectory().resolve("Condor Heavy Hover Tank Cargo.blk"),
              blkContents("Condor Heavy Hover Tank", sharedUUID));

        int repairedCount = CustomUnitFileUUIDRepair.repairCustomUnitFiles(new MekSummary[] {
              summaryFor(canon, sharedUUID, "Condor Heavy Hover Tank"),
              summaryFor(firstCustom, sharedUUID, "Condor Heavy Hover Tank Cargo"),
              summaryFor(secondCustom, sharedUUID, "Condor Heavy Hover Tank Cargo") }, directories());

        assertEquals(2, repairedCount);
        assertEquals(canonContents, readFile(canon), "The canon file is never rewritten");
        String firstUUID = uuidInBlk(readFile(firstCustom));
        String secondUUID = uuidInBlk(readFile(secondCustom));
        assertNotEquals(sharedUUID, firstUUID);
        assertNotEquals(sharedUUID, secondUUID);
        assertNotEquals(firstUUID, secondUUID);
        assertTrue(readFile(firstCustom).contains("<refitFromUUID>\n" + sharedUUID + "\n</refitFromUUID>"));
    }

    @Test
    void customsInUserDirectoriesAreRepaired() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File canon = writeFile(officialDirectory().resolve("Anhur Transport.blk"),
              blkContents("Anhur Transport", sharedUUID));
        File userDataCustom = writeFile(userDataUnitDirectory().resolve("Anhur Transport Mk II.blk"),
              blkContents("Anhur Transport", sharedUUID));
        File externalCustom = writeFile(externalUserDirectory().resolve("Ships/Anhur Transport Mk III.blk"),
              blkContents("Anhur Transport", sharedUUID));
        MekSummary userDataSummary = summaryFor(userDataCustom, sharedUUID, "Anhur Transport Mk II");
        MekSummary externalSummary = summaryFor(externalCustom, sharedUUID, "Anhur Transport Mk III");

        List<MekSummary> customsToRepair = CustomUnitFileUUIDRepair.findCustomsToRepair(new MekSummary[] {
              summaryFor(canon, sharedUUID, "Anhur Transport"), userDataSummary, externalSummary }, directories());

        assertEquals(2, customsToRepair.size());
        assertTrue(customsToRepair.contains(userDataSummary));
        assertTrue(customsToRepair.contains(externalSummary));
    }

    @Test
    void sameNamedOverrideKeepsCanonUUID() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File canon = writeFile(officialDirectory().resolve("Charger CGR-1A5.mtf"),
              mtfContents("CGR-1A5", sharedUUID));
        File override = writeFile(externalUserDirectory().resolve("Charger CGR-1A5.mtf"),
              mtfContents("CGR-1A5", sharedUUID));

        List<MekSummary> customsToRepair = CustomUnitFileUUIDRepair.findCustomsToRepair(new MekSummary[] {
              summaryFor(canon, sharedUUID, "Charger CGR-1A5"),
              summaryFor(override, sharedUUID, "Charger CGR-1A5") }, directories());

        assertTrue(customsToRepair.isEmpty(), "A design re-saved under its own name is meant to keep its UUID");
    }

    @Test
    void userDirectoryContainingOfficialUnitsNeverRewritesCanon() throws IOException {
        String sharedUUID = UUIDUtil.newUUIDv7().toString();
        File canon = writeFile(officialDirectory().resolve("Drillson Heavy Hover Tank.blk"),
              blkContents("Drillson Heavy Hover Tank", sharedUUID));
        File custom = writeFile(tempDirectory.resolve("Drillson Heavy Hover Tank Cargo.blk"),
              blkContents("Drillson Heavy Hover Tank", sharedUUID));
        assertTrue(custom.setLastModified(1_000_000L), "The custom is older than the canon file");
        MekSummary customSummary = summaryFor(custom, sharedUUID, "Drillson Heavy Hover Tank Cargo");
        CustomUnitFileUUIDRepair.UserUnitDirectories wholeTreeAsUserDirectory =
              new CustomUnitFileUUIDRepair.UserUnitDirectories(customsDirectory(), officialDirectory(),
                    List.of(tempDirectory));

        List<MekSummary> customsToRepair = CustomUnitFileUUIDRepair.findCustomsToRepair(new MekSummary[] {
              summaryFor(canon, sharedUUID, "Drillson Heavy Hover Tank"), customSummary }, wholeTreeAsUserDirectory);

        assertEquals(List.of(customSummary), customsToRepair);
    }

    private static String uuidInBlk(String contents) {
        int start = contents.indexOf("<UUID>\n") + "<UUID>\n".length();
        return contents.substring(start, contents.indexOf('\n', start));
    }
}
