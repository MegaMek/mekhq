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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Golden-master parts census for {@link Unit#initializeParts(boolean)}: every {@link UnitFixture} unit is added to a
 * campaign the way the game adds a purchased unit, and the parts it receives are compared with a reviewed listing
 * checked in under {@code testresources/data/parts-census/<FIXTURE>.txt}.
 *
 * <p>Each line of a listing describes one part: its class, its name, its location (the entity location abbreviation
 * and number, or {@code none}), its equipment number where it has one, and its quantity. Armor lines also give the
 * armor points and whether the armor is rear facing. Lines are sorted, so the listing does not depend on the order in
 * which parts were created. Lines starting with {@code #} are comments and are ignored when comparing.</p>
 *
 * <p>A normal test run never writes the listings. After a deliberate change to part generation, regenerate them and
 * review the resulting diff before committing:</p>
 *
 * <pre>{@code
 * PARTS_CENSUS_UPDATE=true ./gradlew :MekHQ:test --tests "mekhq.campaign.unit.PartsCensusGoldenTest"
 * }</pre>
 *
 * <p>The system property {@code -Dparts.census.update=true} does the same when the test is run from an IDE.</p>
 */
class PartsCensusGoldenTest {
    private static final MMLogger LOGGER = MMLogger.create(PartsCensusGoldenTest.class);

    /** The system property that switches the test from comparing listings to rewriting them. */
    static final String UPDATE_PROPERTY = "parts.census.update";

    /** The environment variable that switches the test from comparing listings to rewriting them. */
    static final String UPDATE_ENVIRONMENT_VARIABLE = "PARTS_CENSUS_UPDATE";

    /** The folder holding one golden listing per fixture, relative to the MekHQ project directory. */
    static final Path GOLDEN_DIRECTORY = Path.of("testresources", "data", "parts-census");

    private static final String COMMENT_PREFIX = "#";
    private static final String FIELD_SEPARATOR = " | ";
    private static final String NOT_APPLICABLE = "-";
    private static final int MAXIMUM_REPORTED_LINES = 15;

    @ParameterizedTest
    @EnumSource(UnitFixture.class)
    void partsMatchGoldenCensus(UnitFixture fixture) throws IOException {
        PartsScenario scenario = PartsScenario.create();
        Unit unit = scenario.withUnit(fixture);
        List<String> actualLines = censusLines(unit);
        Path goldenFile = GOLDEN_DIRECTORY.resolve(fixture.name() + ".txt");

        if (isUpdateRequested()) {
            writeGoldenFile(goldenFile, fixture, unit, actualLines);
            return;
        }

        assertTrue(Files.isRegularFile(goldenFile),
              "No parts census for fixture " + fixture.name() + " at " + goldenFile.toAbsolutePath()
                    + ". Generate it with " + UPDATE_ENVIRONMENT_VARIABLE + "=true (see the class JavaDoc).");
        List<String> expectedLines = readGoldenLines(goldenFile);
        if (!expectedLines.equals(actualLines)) {
            fail(describeDifference(fixture, unit, expectedLines, actualLines));
        }
    }

    /**
     * Builds the sorted census of a unit's parts, one line per part.
     *
     * @param unit the unit whose parts are listed
     *
     * @return the census lines in sorted order
     */
    static List<String> censusLines(Unit unit) {
        List<String> lines = new ArrayList<>();
        for (Part part : unit.getParts()) {
            lines.add(describePart(unit.getEntity(), part));
        }
        Collections.sort(lines);
        return lines;
    }

    /**
     * Describes one part in the census format, for example
     * {@code EquipmentPart | Medium Laser | location RA (1) | equipment 4 | quantity 1}.
     *
     * @param entity the entity the part belongs to, used to name its location
     * @param part   the part to describe
     *
     * @return the census line for the part
     */
    static String describePart(Entity entity, Part part) {
        StringBuilder line = new StringBuilder();
        line.append(part.getClass().getSimpleName());
        line.append(FIELD_SEPARATOR).append(part.getName());
        line.append(FIELD_SEPARATOR).append("location ").append(describeLocation(entity, realLocation(part)));
        line.append(FIELD_SEPARATOR).append("equipment ").append(describeEquipmentNumber(part));
        line.append(FIELD_SEPARATOR).append("quantity ").append(part.getQuantity());
        if (part instanceof Armor armor) {
            line.append(FIELD_SEPARATOR).append("armor points ").append(armor.getAmount());
            if (armor.isRearMounted()) {
                line.append(FIELD_SEPARATOR).append("rear");
            }
        }
        return line.toString();
    }

    /**
     * Mek and ProtoMek locations report {@link Entity#LOC_NONE} from {@link Part#getLocation()}, so their structural
     * location is read from {@code getLoc()} instead.
     */
    private static int realLocation(Part part) {
        if (part instanceof MekLocation mekLocation) {
            return mekLocation.getLoc();
        }
        if (part instanceof ProtoMekLocation protoMekLocation) {
            return protoMekLocation.getLoc();
        }
        return part.getLocation();
    }

    private static String describeLocation(Entity entity, int location) {
        if (location == Entity.LOC_NONE) {
            return "none";
        }
        boolean isEntityLocation = (location >= 0) && (location < entity.locations());
        if (!isEntityLocation) {
            return "(" + location + ")";
        }
        return entity.getLocationAbbr(location) + " (" + location + ")";
    }

    private static String describeEquipmentNumber(Part part) {
        if (part instanceof EquipmentPart equipmentPart) {
            return String.valueOf(equipmentPart.getEquipmentNum());
        }
        return NOT_APPLICABLE;
    }

    private static boolean isUpdateRequested() {
        boolean isPropertySet = Boolean.getBoolean(UPDATE_PROPERTY);
        boolean isEnvironmentSet = "true".equalsIgnoreCase(System.getenv(UPDATE_ENVIRONMENT_VARIABLE));
        return isPropertySet || isEnvironmentSet;
    }

    private static List<String> readGoldenLines(Path goldenFile) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(goldenFile, StandardCharsets.US_ASCII)) {
            boolean isComment = line.startsWith(COMMENT_PREFIX);
            if (!isComment && !line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void writeGoldenFile(Path goldenFile, UnitFixture fixture, Unit unit, List<String> lines)
          throws IOException {
        for (String line : lines) {
            boolean isAscii = StandardCharsets.US_ASCII.newEncoder().canEncode(line);
            assertTrue(isAscii, "Parts census line for fixture " + fixture.name() + " is not ASCII: " + line);
        }
        List<String> fileLines = new ArrayList<>();
        fileLines.add(COMMENT_PREFIX + " Parts census for " + fixture.name() + " (" + unit.getName() + ")");
        fileLines.add(COMMENT_PREFIX + " Generated by PartsCensusGoldenTest from current behaviour. Do not edit by "
              + "hand; regenerate with");
        fileLines.add(COMMENT_PREFIX + " " + UPDATE_ENVIRONMENT_VARIABLE + "=true and review the diff.");
        fileLines.addAll(lines);
        Files.createDirectories(goldenFile.getParent());
        Files.writeString(goldenFile, String.join("\n", fileLines) + "\n", StandardCharsets.US_ASCII);
        LOGGER.info("[PartsCensus] Wrote {} parts for {} to {}", lines.size(), fixture.name(), goldenFile);
    }

    private static String describeDifference(UnitFixture fixture, Unit unit, List<String> expectedLines,
          List<String> actualLines) {
        StringBuilder message = new StringBuilder();
        message.append("Parts census for ").append(fixture.name()).append(" (").append(unit.getName())
              .append(") differs from ").append(GOLDEN_DIRECTORY.resolve(fixture.name() + ".txt"))
              .append(": expected ").append(expectedLines.size()).append(" parts, found ")
              .append(actualLines.size()).append('\n');

        int firstDifference = 0;
        while ((firstDifference < expectedLines.size()) && (firstDifference < actualLines.size())
              && expectedLines.get(firstDifference).equals(actualLines.get(firstDifference))) {
            firstDifference++;
        }
        message.append("First difference at census line ").append(firstDifference + 1).append(":\n");
        message.append("  expected: ").append(lineOrEnd(expectedLines, firstDifference)).append('\n');
        message.append("  actual:   ").append(lineOrEnd(actualLines, firstDifference)).append('\n');

        appendLinesNotIn(message, "Missing from the current census", expectedLines, actualLines);
        appendLinesNotIn(message, "New in the current census", actualLines, expectedLines);
        message.append("If the change is deliberate, regenerate with ").append(UPDATE_ENVIRONMENT_VARIABLE)
              .append("=true and review the diff.");
        return message.toString();
    }

    private static String lineOrEnd(List<String> lines, int index) {
        return (index < lines.size()) ? lines.get(index) : "<end of census>";
    }

    /**
     * Appends the lines of {@code source} that have no matching line left in {@code other}, counting duplicates, so a
     * part that appears twice where it used to appear once is reported.
     */
    private static void appendLinesNotIn(StringBuilder message, String heading, List<String> source,
          List<String> other) {
        List<String> remaining = new ArrayList<>(other);
        List<String> unmatched = new ArrayList<>();
        for (String line : source) {
            if (!remaining.remove(line)) {
                unmatched.add(line);
            }
        }
        if (unmatched.isEmpty()) {
            return;
        }
        message.append(heading).append(" (").append(unmatched.size()).append("):\n");
        int reported = Math.min(unmatched.size(), MAXIMUM_REPORTED_LINES);
        for (int index = 0; index < reported; index++) {
            message.append("  ").append(unmatched.get(index)).append('\n');
        }
        if (unmatched.size() > reported) {
            message.append("  ... and ").append(unmatched.size() - reported).append(" more\n");
        }
    }
}
