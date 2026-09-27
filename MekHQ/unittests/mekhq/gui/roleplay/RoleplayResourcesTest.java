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
package mekhq.gui.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import mekhq.campaign.roleplay.CampaignChronicle;
import mekhq.campaign.roleplay.TravelRecord;
import mekhq.campaign.roleplay.CheckDifficulty;
import mekhq.campaign.roleplay.JournalEntryType;
import mekhq.campaign.roleplay.NpcRating;
import org.junit.jupiter.api.Test;

/** Every piece of text the Oracle console and its engine ask for must exist, or players see a raw key. */
class RoleplayResourcesTest {
    private static final Path BUNDLE = Path.of("resources/mekhq/resources/Roleplay.properties");
    private static final List<Path> SOURCES = List.of(Path.of("src/mekhq/gui/roleplay"),
          Path.of("src/mekhq/campaign/roleplay"));
    /** A complete key in a string literal; keys built by adding text on the end stop at a dot and are skipped. */
    private static final Pattern KEY = Pattern.compile(
          "\"((?:OracleConsole|OracleLog|OracleGuide|ChecksPage|JournalExporter|Chronicle|TravelLog)\\.[A-Za-z0-9_.]*[A-Za-z0-9_])\"");

    private static Properties bundle() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(BUNDLE, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    @Test
    void everyKeyInTheCodeExists() throws IOException {
        Properties properties = bundle();
        List<String> missing = new ArrayList<>();
        for (Path directory : SOURCES) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    Matcher matcher = KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        if (!properties.containsKey(matcher.group(1))) {
                            missing.add(file.getFileName() + ": " + matcher.group(1));
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void everyPageHasItsLabelsAndAGuide() throws IOException {
        Properties properties = bundle();
        for (ConsolePage page : ConsolePage.values()) {
            String prefix = "OracleGuide." + page.name().toLowerCase() + ".";
            assertTrue(properties.containsKey("OracleConsole.page." + page.name()), page.name());
            assertTrue(properties.containsKey("OracleConsole.guide." + page.name()), page.name());
            assertTrue(properties.containsKey(prefix + "title"), page.name());
            assertTrue(properties.containsKey(prefix + "step.1.title"), page.name());
            assertTrue(properties.containsKey(prefix + "term.1.name"), page.name());
        }
    }

    @Test
    void everyCheckModeAndEnumHasText() throws IOException {
        Properties properties = bundle();
        for (ChecksPage.Mode mode : ChecksPage.Mode.values()) {
            assertTrue(properties.containsKey("ChecksPage.mode." + mode.name()), mode.name());
            assertTrue(properties.containsKey("ChecksPage.mode." + mode.name() + ".sub"), mode.name());
        }
        for (CheckDifficulty difficulty : CheckDifficulty.values()) {
            assertTrue(properties.containsKey("CheckDifficulty." + difficulty.name() + ".label"));
        }
        for (NpcRating rating : NpcRating.values()) {
            assertTrue(properties.containsKey("NpcRating." + rating.name() + ".label"));
        }
        for (TravelRecord.Kind kind : TravelRecord.Kind.values()) {
            assertTrue(properties.containsKey("TravelLog.kind." + kind.name()), kind.name());
        }
        for (String column : List.of("system", "first", "last", "visits", "days", "contracts", "present", "date",
              "who", "what")) {
            assertTrue(properties.containsKey("TravelLog.column." + column), column);
        }
        for (CampaignChronicle.PersonChange change : CampaignChronicle.PersonChange.values()) {
            assertTrue(properties.containsKey("Chronicle.person." + change.name()), change.name());
        }
        for (JournalEntryType type : JournalEntryType.values()) {
            assertTrue(properties.containsKey("JournalEntryType." + type.name() + ".label"));
        }
    }

    @Test
    void thereAreFiftyFlavourLines() throws IOException {
        Properties properties = bundle();
        for (int line = 0; line < 50; line++) {
            String text = properties.getProperty("ChecksPage.flavour." + line);
            assertFalse(text == null || text.isBlank(), "flavour line " + line);
        }
        assertFalse(properties.containsKey("ChecksPage.flavour.50"));
    }
}
