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

import java.util.ArrayList;
import java.util.List;

import mekhq.utilities.ValidationMessage.Category;
import mekhq.utilities.ValidationMessage.Severity;

/**
 * Accumulates the results of a planetary system data validation run, including all findings (errors and warnings) and
 * summary statistics.
 *
 * <p>Instances are populated by {@link SystemValidator} during validation and can then be queried by tests or the
 * Gradle task to determine pass/fail status and to report details.</p>
 */
public class ValidationResult {
    private final List<ValidationMessage> messages = new ArrayList<>();
    private int systemsValidated = 0;
    private int planetsValidated = 0;
    private int filesProcessed = 0;

    /**
     * Records a validation finding.
     *
     * @param message the validation message to add
     */
    public void addMessage(ValidationMessage message) {
        messages.add(message);
    }

    /**
     * @return all validation messages (both errors and warnings)
     */
    public List<ValidationMessage> getMessages() {
        return messages;
    }

    /**
     * @return only the ERROR-severity messages
     */
    public List<ValidationMessage> getErrors() {
        return messages.stream()
                     .filter(m -> m.getSeverity() == Severity.ERROR)
                     .toList();
    }

    /**
     * @return only the WARNING-severity messages
     */
    public List<ValidationMessage> getWarnings() {
        return messages.stream()
                     .filter(m -> m.getSeverity() == Severity.WARNING)
                     .toList();
    }

    /**
     * Returns all messages matching a specific category, useful for targeted test assertions.
     *
     * @param category the category to filter by
     *
     * @return messages matching the given category
     */
    public List<ValidationMessage> getByCategory(Category category) {
        return messages.stream()
                     .filter(m -> m.getCategory() == category)
                     .toList();
    }

    /**
     * @return the total number of ERROR-severity messages
     */
    public int getErrorCount() {
        return (int) messages.stream()
                           .filter(m -> m.getSeverity() == Severity.ERROR)
                           .count();
    }

    /**
     * @return the total number of WARNING-severity messages
     */
    public int getWarningCount() {
        return (int) messages.stream()
                           .filter(m -> m.getSeverity() == Severity.WARNING)
                           .count();
    }

    /**
     * @return the number of planetary systems that were validated
     */
    public int getSystemsValidated() {
        return systemsValidated;
    }

    /**
     * Increments the count of validated systems by one.
     */
    public void incrementSystemsValidated() {
        systemsValidated++;
    }

    /**
     * @return the number of individual planets that were validated
     */
    public int getPlanetsValidated() {
        return planetsValidated;
    }

    /**
     * Adds to the running count of validated planets.
     *
     * @param count the number of planets to add
     */
    public void addPlanetsValidated(int count) {
        planetsValidated += count;
    }

    /**
     * @return the number of data files (YAML or ZIP entries) that were processed
     */
    public int getFilesProcessed() {
        return filesProcessed;
    }

    /**
     * Increments the count of processed files by one.
     */
    public void incrementFilesProcessed() {
        filesProcessed++;
    }

    /**
     * @return {@code true} if any ERROR-severity messages were recorded
     */
    public boolean hasErrors() {
        return getErrorCount() > 0;
    }

    /**
     * Produces a human-readable summary line with counts of systems, planets, files, errors, and warnings.
     *
     * @return a formatted summary string
     */
    public String getSummary() {
        return String.format("Validated %,d systems (%,d planets) from %,d files%n"
                                   + "Errors: %d  |  Warnings: %d",
              systemsValidated, planetsValidated, filesProcessed,
              getErrorCount(), getWarningCount());
    }
}
