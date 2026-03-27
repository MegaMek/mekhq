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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.StarType;

/**
 * Validates planetary system YAML data files for structural correctness and data integrity. Can be run as a standalone
 * Gradle task or called from unit tests.
 */
public class SystemValidator {

    /**
     * Severity levels for validation messages.
     */
    public enum Severity {
        ERROR,
        WARNING
    }

    /**
     * A single validation finding with full context.
     */
    public static class ValidationMessage {
        private final Severity severity;
        private final String fileName;
        private final String systemId;
        private final String planetInfo;
        private final String message;

        public ValidationMessage(Severity severity, String fileName, String systemId,
              String planetInfo, String message) {
            this.severity = severity;
            this.fileName = fileName;
            this.systemId = systemId;
            this.planetInfo = planetInfo;
            this.message = message;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getFileName() {
            return fileName;
        }

        public String getSystemId() {
            return systemId;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-7s", severity));
            sb.append("[").append(fileName).append("] ");
            sb.append("System '").append(systemId).append("'");
            if (planetInfo != null && !planetInfo.isEmpty()) {
                sb.append(", ").append(planetInfo);
            }
            sb.append(" - ").append(message);
            return sb.toString();
        }
    }

    /**
     * Results of a validation run.
     */
    public static class ValidationResult {
        private final List<ValidationMessage> messages = new ArrayList<>();
        private int systemsValidated = 0;
        private int planetsValidated = 0;
        private int filesProcessed = 0;

        public void addMessage(ValidationMessage msg) {
            messages.add(msg);
        }

        public List<ValidationMessage> getMessages() {
            return messages;
        }

        public List<ValidationMessage> getErrors() {
            return messages.stream()
                         .filter(m -> m.getSeverity() == Severity.ERROR)
                         .toList();
        }

        public List<ValidationMessage> getWarnings() {
            return messages.stream()
                         .filter(m -> m.getSeverity() == Severity.WARNING)
                         .toList();
        }

        public int getErrorCount() {
            return (int) messages.stream()
                               .filter(m -> m.getSeverity() == Severity.ERROR)
                               .count();
        }

        public int getWarningCount() {
            return (int) messages.stream()
                               .filter(m -> m.getSeverity() == Severity.WARNING)
                               .count();
        }

        public int getSystemsValidated() {
            return systemsValidated;
        }

        public void incrementSystemsValidated() {
            systemsValidated++;
        }

        public int getPlanetsValidated() {
            return planetsValidated;
        }

        public void addPlanetsValidated(int count) {
            planetsValidated += count;
        }

        public int getFilesProcessed() {
            return filesProcessed;
        }

        public void incrementFilesProcessed() {
            filesProcessed++;
        }

        public boolean hasErrors() {
            return getErrorCount() > 0;
        }

        public String getSummary() {
            return String.format("Validated %,d systems (%,d planets) from %,d files%n"
                                       + "Errors: %d  |  Warnings: %d",
                  systemsValidated, planetsValidated, filesProcessed,
                  getErrorCount(), getWarningCount());
        }
    }

    private final ObjectMapper mapper;

    public SystemValidator() {
        mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SocioIndustrialData.class,
              new SocioIndustrialData.SocioIndustrialDataDeserializer());
        module.addDeserializer(StarType.class, new StarType.StarTypeDeserializer());
        module.addDeserializer(SourceableValue.class, new SourceableValue.SourceableValueDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Validates all YAML files in a directory tree.
     *
     * @param dataPath the root directory containing planetary system YAML files
     *
     * @return the validation result with all findings
     */
    public ValidationResult validate(String dataPath) {
        ValidationResult result = new ValidationResult();
        Map<String, String> seenIds = new HashMap<>();

        File dir = new File(dataPath);
        if (!dir.exists() || !dir.isDirectory()) {
            result.addMessage(new ValidationMessage(Severity.ERROR, dataPath, "N/A", null,
                  "Data directory does not exist: " + dataPath));
            return result;
        }

        validateDirectory(dir, result, seenIds);
        return result;
    }

    private void validateDirectory(File dir, ValidationResult result, Map<String, String> seenIds) {
        // Process .yml files
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null && files.length > 0) {
            Arrays.sort(files, Comparator.comparing(File::getPath));
            for (File file : files) {
                if (file.isFile()) {
                    validateFile(file, result, seenIds);
                }
            }
        }

        // Process .zip archives containing .yml files
        File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
        if (zipFiles != null) {
            Arrays.sort(zipFiles, Comparator.comparing(File::getPath));
            for (File zipFile : zipFiles) {
                validateZipFile(zipFile, result, seenIds);
            }
        }

        // Recurse into subdirectories
        File[] subdirs = dir.listFiles();
        if (subdirs != null) {
            Arrays.sort(subdirs, Comparator.comparing(File::getPath));
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    validateDirectory(subdir, result, seenIds);
                }
            }
        }
    }

    private void validateZipFile(File zipFile, ValidationResult result, Map<String, String> seenIds) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    String entryName = entry.getName();
                    // Use just the filename portion for display
                    int lastSlash = entryName.lastIndexOf('/');
                    String displayName = (lastSlash >= 0)
                                               ? entryName.substring(lastSlash + 1)
                                               : entryName;
                    String contextName = zipFile.getName() + "!" + displayName;

                    result.incrementFilesProcessed();
                    try (InputStream is = zip.getInputStream(entry)) {
                        PlanetarySystem system = mapper.readValue(is, PlanetarySystem.class);
                        validateSystem(system, contextName, result, seenIds);
                    } catch (Exception ex) {
                        result.addMessage(new ValidationMessage(Severity.ERROR, contextName,
                              "N/A", null, "Failed to parse YAML: " + ex.getMessage()));
                    }
                }
            }
        } catch (Exception ex) {
            result.addMessage(new ValidationMessage(Severity.ERROR, zipFile.getName(), "N/A",
                  null, "Failed to read ZIP archive: " + ex.getMessage()));
        }
    }

    private void validateFile(File file, ValidationResult result, Map<String, String> seenIds) {
        String fileName = file.getName();
        result.incrementFilesProcessed();

        PlanetarySystem system;
        try (InputStream fis = new FileInputStream(file)) {
            system = mapper.readValue(fis, PlanetarySystem.class);
        } catch (Exception ex) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, "N/A", null,
                  "Failed to parse YAML: " + ex.getMessage()));
            return;
        }

        validateSystem(system, fileName, result, seenIds);
    }

    private void validateSystem(PlanetarySystem system, String fileName,
          ValidationResult result, Map<String, String> seenIds) {
        result.incrementSystemsValidated();
        String systemId = system.getId() != null ? system.getId() : "<null>";

        // Check duplicate IDs
        if (system.getId() != null) {
            String previousFile = seenIds.put(system.getId(), fileName);
            if (previousFile != null) {
                result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                      "Duplicate system ID (also in " + previousFile + ")"));
            }
        } else {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                  "Missing system ID"));
        }

        // Check coordinates
        if (system.getX() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                  "Missing X coordinate (xcood)"));
        }
        if (system.getY() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                  "Missing Y coordinate (ycood)"));
        }

        // Check star
        if (system.getSourcedStar() == null || system.getStar() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                  "Missing spectralType (star)"));
        }

        // Check primary slot vs planet count
        int primarySlot = system.getPrimaryPlanetPosition();
        int planetCount = system.getPlanets().size();
        if (primarySlot > planetCount) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId, null,
                  "Primary slot (" + primarySlot + ") exceeds planet count (" + planetCount + ")"));
        }

        // Validate each planet
        result.addPlanetsValidated(planetCount);
        for (Planet planet : system.getPlanets()) {
            validatePlanet(planet, fileName, systemId, result);
        }
    }

    private void validatePlanet(Planet planet, String fileName, String systemId,
          ValidationResult result) {
        String planetInfo = buildPlanetInfo(planet);

        // Check system position
        if (planet.getSystemPosition() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId,
                  planetInfo, "Missing system position (sysPos)"));
        }

        // Check planet type
        if (planet.getPlanetType() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR, fileName, systemId,
                  planetInfo, "Missing planet type"));
        }
    }

    private String buildPlanetInfo(Planet planet) {
        String name = planet.getId() != null ? planet.getId() : "unnamed";
        Integer pos = planet.getSystemPosition();
        if (pos != null) {
            return "Planet '" + name + "' (pos " + pos + ")";
        }
        return "Planet '" + name + "'";
    }

    /**
     * Entry point for the Gradle validateSystems task.
     *
     * @param args optional: path to data directory (defaults to MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH)
     */
    public static void main(String[] args) {
        String dataPath;
        if (args.length > 0) {
            dataPath = args[0];
        } else {
            dataPath = MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH;
        }

        System.out.println("Validating planetary systems in: " + dataPath);
        System.out.println();

        SystemValidator validator = new SystemValidator();
        ValidationResult result = validator.validate(dataPath);

        // Print all messages
        for (ValidationMessage msg : result.getMessages()) {
            System.out.println(msg);
        }

        if (!result.getMessages().isEmpty()) {
            System.out.println();
        }

        // Print summary
        System.out.println("--- Summary ---");
        System.out.println(result.getSummary());

        if (result.hasErrors()) {
            System.exit(1);
        }
    }
}
