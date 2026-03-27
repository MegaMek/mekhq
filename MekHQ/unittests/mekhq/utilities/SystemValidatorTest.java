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
import mekhq.utilities.SystemValidator.ValidationMessage;
import mekhq.utilities.SystemValidator.ValidationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that validate all planetary system data files using the same deserialization path as production
 * code. These tests act as a CI safety net to catch data file issues before they reach users.
 */
class SystemValidatorTest {
    private static ValidationResult result;

    @BeforeAll
    static void validateAllSystems() {
        SystemValidator validator = new SystemValidator();

        // Try staged data first (ZIP files from mm-data build), then fall back to
        // mm-data source directory for local development
        String stagedPath = MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH;
        File stagedDir = new File(stagedPath);

        if (stagedDir.exists() && stagedDir.isDirectory()) {
            result = validator.validate(stagedPath);
        } else {
            // Fall back to mm-data source when running without staged data
            String mmDataPath = "../../mm-data/" + MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH;
            File mmDataDir = new File(mmDataPath);
            assertTrue(mmDataDir.exists(),
                  "Neither staged data nor mm-data source found. Run stageDataFiles first, "
                        + "or ensure mm-data is available at: " + mmDataDir.getAbsolutePath());
            result = validator.validate(mmDataPath);
        }
    }

    @Test
    void allYamlFilesParse() {
        List<ValidationMessage> parseErrors = result.getErrors().stream()
                                                    .filter(m -> m.getMessage().startsWith("Failed to parse YAML"))
                                                    .toList();

        assertTrue(parseErrors.isEmpty(),
              "YAML parse failures found:\n" + formatMessages(parseErrors));
    }

    @Test
    void allSystemsHaveRequiredFields() {
        List<ValidationMessage> missingFields = result.getErrors().stream()
                                                      .filter(m -> m.getMessage().startsWith("Missing"))
                                                      .filter(m -> !m.getMessage().contains("planet type"))
                                                      .filter(m -> !m.getMessage().contains("system position"))
                                                      .toList();

        assertTrue(missingFields.isEmpty(),
              "Systems with missing required fields:\n" + formatMessages(missingFields));
    }

    @Test
    void allPrimaryPlanetSlotsValid() {
        List<ValidationMessage> slotErrors = result.getErrors().stream()
                                                   .filter(m -> m.getMessage().startsWith("Primary slot"))
                                                   .toList();

        assertTrue(slotErrors.isEmpty(),
              "Invalid primary planet slots:\n" + formatMessages(slotErrors));
    }

    @Test
    void noDuplicateSystemIds() {
        List<ValidationMessage> dupeErrors = result.getErrors().stream()
                                                   .filter(m -> m.getMessage().startsWith("Duplicate system ID"))
                                                   .toList();

        assertTrue(dupeErrors.isEmpty(),
              "Duplicate system IDs found:\n" + formatMessages(dupeErrors));
    }

    @Test
    void allPlanetsHaveRequiredFields() {
        List<ValidationMessage> planetErrors = result.getErrors().stream()
                                                     .filter(m -> m.getMessage().contains("system position")
                                                                        || m.getMessage().contains("planet type"))
                                                     .toList();

        assertTrue(planetErrors.isEmpty(),
              "Planets with missing required fields:\n" + formatMessages(planetErrors));
    }

    @Test
    void validationProcessedFiles() {
        assertTrue(result.getFilesProcessed() > 0,
              "No files were processed - check data directory path");
        assertTrue(result.getSystemsValidated() > 0,
              "No systems were validated - check data directory path");
    }

    @Test
    void noValidationErrors() {
        assertFalse(result.hasErrors(),
              "Validation errors found (" + result.getErrorCount() + " total):\n"
                    + formatMessages(result.getErrors()));
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
