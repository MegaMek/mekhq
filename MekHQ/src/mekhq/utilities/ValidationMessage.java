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

/**
 * Represents a single validation finding (error or warning) produced during planetary system data validation.
 *
 * <p>Each message captures the severity, category, source file, system identifier, optional planet context, and a
 * human-readable description of the issue found.</p>
 */
public class ValidationMessage {

    /**
     * Severity levels for validation findings.
     */
    public enum Severity {
        /** A critical issue that should block the build. */
        ERROR,
        /** A data quality issue that is reported but does not block the build. */
        WARNING
    }

    /**
     * Categories for validation findings. Tests filter on these rather than message text, making assertions stable
     * across wording changes.
     */
    public enum Category {
        /** The YAML file could not be parsed at all. */
        YAML_PARSE_FAILURE,
        /** The system is missing its unique identifier. */
        MISSING_SYSTEM_ID,
        /** The system is missing X or Y coordinates. */
        MISSING_COORDINATES,
        /** The system is missing its spectral type (star). */
        MISSING_STAR,
        /** The primary planet slot does not correspond to any planet in the system. */
        INVALID_PRIMARY_SLOT,
        /** The system has no planets defined. */
        NO_PLANETS,
        /** Two or more systems share the same ID. */
        DUPLICATE_SYSTEM_ID,
        /** Two or more systems share the same SUCS ID. */
        DUPLICATE_SUCS_ID,
        /** Two or more planets within the same system claim the same sysPos. */
        DUPLICATE_PLANET_POSITION,
        /** A planet is missing a required field (e.g. sysPos, planetType). */
        MISSING_PLANET_FIELD,
        /** A planet has a sysPos value that is out of the valid range. */
        INVALID_PLANET_POSITION,
        /** A planet has a gravity value that is non-positive for a non-asteroid body. */
        INVALID_GRAVITY,
        /** A planet event has a water percentage outside 0-100. */
        INVALID_WATER,
        /** A planet event has a temperature below absolute zero. */
        INVALID_TEMPERATURE,
        /** A planet event has a negative population value. */
        NEGATIVE_POPULATION,
        /** A planet event references a faction code not found in the factions data. */
        UNKNOWN_FACTION,
        /** A terrestrial planet is missing atmosphere or pressure data. */
        MISSING_ATMOSPHERE_DATA,
        /** The specified data directory does not exist or is not a directory. */
        DATA_DIRECTORY_ERROR
    }

    private final Severity severity;
    private final Category category;
    private final String fileName;
    private final String systemId;
    private final String planetInfo;
    private final String message;

    /**
     * Creates a new validation message.
     *
     * @param severity   the severity level of this finding
     * @param category   the category of validation issue
     * @param fileName   the source file (or ZIP entry) where the issue was found
     * @param systemId   the system identifier, or a placeholder if unavailable
     * @param planetInfo optional planet context string, or {@code null} if not planet-specific
     * @param message    a human-readable description of the issue
     */
    public ValidationMessage(Severity severity, Category category, String fileName,
          String systemId, String planetInfo, String message) {
        this.severity = severity;
        this.category = category;
        this.fileName = fileName;
        this.systemId = systemId;
        this.planetInfo = planetInfo;
        this.message = message;
    }

    /**
     * @return the severity level of this finding
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return the category of validation issue
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the source file name where the issue was found
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the system identifier associated with this finding
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @return a human-readable description of the issue
     */
    public String getMessage() {
        return message;
    }

    /**
     * Formats this message as a single-line string suitable for console output or log files.
     *
     * @return a formatted string including severity, file, system, optional planet info, and message
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s", severity));
        sb.append("[").append(fileName).append("] ");
        sb.append("System '").append(systemId).append("'");
        if (planetInfo != null && !planetInfo.isEmpty()) {
            sb.append(", ").append(planetInfo);
        }
        sb.append(" - ").append(message);
        return sb.toString();
    }
}
