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
package mekhq.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Produces the MegaMek Data legal notice that leads every saved data file, as a block of {@code #} comment lines. This
 * mirrors the mechanism megamek's {@code UnitFileResaver} uses for plain-text unit files, so the notice is always
 * present and correct rather than authored by hand.
 *
 * <p>The copyright year is carried forward: a file whose existing header starts in an earlier year is rewritten as a
 * {@code start-current} range, so re-saving a shipped file preserves its original year. Files with no readable header
 * fall back to the current year.
 */
public final class MMDataLicenseHeader {

    private static final MMLogger LOGGER = MMLogger.create(MMDataLicenseHeader.class);

    private static final String LICENSE_HEADER_TEMPLATE = """
          # MegaMek Data (C) %s by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
          # To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
          #
          # NOTICE: The MegaMek organization is a non-profit group of volunteers
          # creating free software for the BattleTech community.
          #
          # MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
          # of The Topps Company, Inc. All Rights Reserved.
          #
          # Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
          # InMediaRes Productions, LLC.
          #
          # MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
          # Microsoft's "Game Content Usage Rules"
          # <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
          # affiliated with Microsoft.
          """;

    /** Matches the year, or first year of a year range, in an existing license header. */
    private static final Pattern COPYRIGHT_YEARS_PATTERN =
          Pattern.compile("MegaMek Data \\(C\\) (\\d{4})(?:-(\\d{4}))? by");

    /** How far into a source file to look for the license header before giving up. */
    private static final int HEADER_SEARCH_LINE_LIMIT = 20;

    private static final int CURRENT_YEAR = Year.now().getValue();

    private MMDataLicenseHeader() {
    }

    /**
     * Builds the license header block for a data file.
     *
     * @param targetFile the file the header will be written to, read for its existing copyright year (may be
     *                   {@code null} or not yet exist)
     *
     * @return the license notice, each line prefixed with {@code # } and the whole block ending in a newline
     */
    public static String licenseHeader(@Nullable File targetFile) {
        return LICENSE_HEADER_TEMPLATE.formatted(copyrightYears(targetFile));
    }

    /**
     * @return the copyright year to write: {@code start-current} when the file already carries an earlier start year,
     *       otherwise the current year
     */
    static String copyrightYears(@Nullable File sourceFile) {
        int startYear = readCopyrightStartYear(sourceFile);
        if ((startYear > 0) && (startYear < CURRENT_YEAR)) {
            return startYear + "-" + CURRENT_YEAR;
        }
        return String.valueOf(CURRENT_YEAR);
    }

    private static int readCopyrightStartYear(@Nullable File sourceFile) {
        if ((sourceFile == null) || !sourceFile.isFile()) {
            return 0;
        }

        try (BufferedReader reader = Files.newBufferedReader(sourceFile.toPath(), StandardCharsets.UTF_8)) {
            for (int lineNumber = 0; lineNumber < HEADER_SEARCH_LINE_LIMIT; lineNumber++) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                Matcher matcher = COPYRIGHT_YEARS_PATTERN.matcher(line);
                if (matcher.find()) {
                    return MathUtility.parseInt(matcher.group(1), 0);
                }
            }
        } catch (IOException exception) {
            LOGGER.debug("Could not read copyright year from {}: {}", sourceFile.getPath(), exception.getMessage());
        }

        return 0;
    }
}
