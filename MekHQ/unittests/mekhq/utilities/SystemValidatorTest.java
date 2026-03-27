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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import mekhq.MHQConstants;
import mekhq.utilities.SystemValidator.Category;
import mekhq.utilities.SystemValidator.ValidationMessage;
import mekhq.utilities.SystemValidator.ValidationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that validate all planetary system data files using the same deserialization path as production
 * code. These tests act as a CI safety net to catch data file issues before they reach users.
 *
 * <p>ERROR-level checks block the build. WARNING-level checks report counts but don't fail,
 * since they flag pre-existing data quality issues that don't break runtime behavior.</p>
 */
class SystemValidatorTest {
    private static ValidationResult result;

    @BeforeAll
    static void validateAllSystems() {
        SystemValidator validator = new SystemValidator();

        String stagedPath = MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH;
        File stagedDir = new File(stagedPath);

        if (stagedDir.exists() && stagedDir.isDirectory()) {
            result = validator.validate(stagedPath);
        } else {
            String mmDataPath = "../../mm-data/" + MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH;
            File mmDataDir = new File(mmDataPath);
            assertTrue(mmDataDir.exists(),
                  "Neither staged data nor mm-data source found. Run stageDataFiles first, "
                        + "or ensure mm-data is available at: " + mmDataDir.getAbsolutePath());
            result = validator.validate(mmDataPath);
        }
    }

    // -------------------------------------------------------------------
    // Critical ERROR checks (block the build)
    // -------------------------------------------------------------------

    @Test
    void allYamlFilesParse() {
        assertNoFindings(Category.YAML_PARSE_FAILURE, "YAML parse failures");
    }

    @Test
    void allSystemsHaveIds() {
        assertNoFindings(Category.MISSING_SYSTEM_ID, "Missing system IDs");
    }

    @Test
    void allSystemsHaveCoordinates() {
        assertNoFindings(Category.MISSING_COORDINATES, "Missing coordinates");
    }

    @Test
    void allSystemsHaveStars() {
        assertNoFindings(Category.MISSING_STAR, "Missing stars");
    }

    @Test
    void allPrimaryPlanetSlotsValid() {
        assertNoFindings(Category.INVALID_PRIMARY_SLOT, "Invalid primary planet slots");
    }

    @Test
    void noDuplicateSystemIds() {
        assertNoFindings(Category.DUPLICATE_SYSTEM_ID, "Duplicate system IDs");
    }

    @Test
    void noDuplicatePlanetPositions() {
        assertNoFindings(Category.DUPLICATE_PLANET_POSITION, "Duplicate planet positions");
    }

    @Test
    void allPlanetsHaveRequiredFields() {
        assertNoFindings(Category.MISSING_PLANET_FIELD, "Missing planet fields");
    }

    @Test
    void allPlanetPositionsValid() {
        assertNoFindings(Category.INVALID_PLANET_POSITION, "Invalid planet positions");
    }

    @Test
    void noValidationErrors() {
        assertFalse(result.hasErrors(),
              "Validation errors found (" + result.getErrorCount() + " total):\n"
                    + formatMessages(result.getErrors()));
    }

    // -------------------------------------------------------------------
    // Data quality WARNING checks (report but don't block build)
    // -------------------------------------------------------------------

    @Test
    void validationProcessedFiles() {
        assertTrue(result.getFilesProcessed() > 0,
              "No files were processed - check data directory path");
        assertTrue(result.getSystemsValidated() > 0,
              "No systems were validated - check data directory path");
    }

    @Test
    void reportWarnings() {
        List<ValidationMessage> warnings = result.getWarnings();
        System.out.println("--- Data Quality Warnings: " + warnings.size() + " ---");

        for (Category category : Category.values()) {
            long count = warnings.stream()
                               .filter(m -> m.getCategory() == category)
                               .count();
            if (count > 0) {
                System.out.println("  " + category + ": " + count);
            }
        }
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private void assertNoFindings(Category category, String label) {
        List<ValidationMessage> findings = result.getByCategory(category);
        assertTrue(findings.isEmpty(),
              label + " found:\n" + formatMessages(findings));
    }

    private static String formatMessages(List<ValidationMessage> messages) {
        if (messages.size() <= 20) {
            return messages.stream()
                         .map(ValidationMessage::toString)
                         .collect(Collectors.joining("\n"));
        }
        return messages.stream()
                     .limit(20)
                     .map(ValidationMessage::toString)
                     .collect(Collectors.joining("\n"))
                     + "\n... and " + (messages.size() - 20) + " more";
    }
}
