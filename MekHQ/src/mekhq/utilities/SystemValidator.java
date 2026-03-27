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
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.StarType;
import mekhq.campaign.universe.enums.PlanetaryType;

/**
 * Validates planetary system YAML data files for structural correctness and data integrity. Can be run as a standalone
 * Gradle task or called from unit tests.
 */
public class SystemValidator {

    private static final int ABSOLUTE_ZERO_CELSIUS = -273;
    private static final int MIN_WATER_PERCENT = 0;
    private static final int MAX_WATER_PERCENT = 100;

    public enum Severity {
        ERROR,
        WARNING
    }

    /**
     * Categories for validation findings. Tests filter on these rather than message text.
     */
    public enum Category {
        YAML_PARSE_FAILURE,
        MISSING_SYSTEM_ID,
        MISSING_COORDINATES,
        MISSING_STAR,
        INVALID_PRIMARY_SLOT,
        NO_PLANETS,
        DUPLICATE_SYSTEM_ID,
        DUPLICATE_SUCS_ID,
        DUPLICATE_PLANET_POSITION,
        MISSING_PLANET_FIELD,
        INVALID_PLANET_POSITION,
        INVALID_GRAVITY,
        INVALID_WATER,
        INVALID_TEMPERATURE,
        NEGATIVE_POPULATION,
        UNKNOWN_FACTION,
        MISSING_ATMOSPHERE_DATA,
        DATA_DIRECTORY_ERROR
    }

    public static class ValidationMessage {
        private final Severity severity;
        private final Category category;
        private final String fileName;
        private final String systemId;
        private final String planetInfo;
        private final String message;

        public ValidationMessage(Severity severity, Category category, String fileName,
              String systemId, String planetInfo, String message) {
            this.severity = severity;
            this.category = category;
            this.fileName = fileName;
            this.systemId = systemId;
            this.planetInfo = planetInfo;
            this.message = message;
        }

        public Severity getSeverity() {
            return severity;
        }

        public Category getCategory() {
            return category;
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

        public List<ValidationMessage> getByCategory(Category category) {
            return messages.stream()
                         .filter(m -> m.getCategory() == category)
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
    private final Set<String> knownFactionCodes;

    public SystemValidator() {
        mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SocioIndustrialData.class,
              new SocioIndustrialData.SocioIndustrialDataDeserializer());
        module.addDeserializer(StarType.class, new StarType.StarTypeDeserializer());
        module.addDeserializer(SourceableValue.class, new SourceableValue.SourceableValueDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());

        knownFactionCodes = loadFactionCodes();
    }

    private Set<String> loadFactionCodes() {
        try {
            Factions factions = Factions.load(false);
            Collection<String> codes = factions.getFactionList();
            return new HashSet<>(codes);
        } catch (Exception ex) {
            return Set.of();
        }
    }

    public ValidationResult validate(String dataPath) {
        ValidationResult result = new ValidationResult();
        Map<String, String> seenIds = new HashMap<>();
        Map<Integer, String> seenSucsIds = new HashMap<>();

        File dir = new File(dataPath);
        if (!dir.exists() || !dir.isDirectory()) {
            result.addMessage(new ValidationMessage(Severity.ERROR, Category.DATA_DIRECTORY_ERROR,
                  dataPath, "N/A", null, "Data directory does not exist: " + dataPath));
            return result;
        }

        validateDirectory(dir, result, seenIds, seenSucsIds);
        return result;
    }

    private void validateDirectory(File dir, ValidationResult result,
          Map<String, String> seenIds, Map<Integer, String> seenSucsIds) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null && files.length > 0) {
            Arrays.sort(files, Comparator.comparing(File::getPath));
            for (File file : files) {
                if (file.isFile()) {
                    validateFile(file, result, seenIds, seenSucsIds);
                }
            }
        }

        File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
        if (zipFiles != null) {
            Arrays.sort(zipFiles, Comparator.comparing(File::getPath));
            for (File zipFile : zipFiles) {
                validateZipFile(zipFile, result, seenIds, seenSucsIds);
            }
        }

        File[] subdirs = dir.listFiles();
        if (subdirs != null) {
            Arrays.sort(subdirs, Comparator.comparing(File::getPath));
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    validateDirectory(subdir, result, seenIds, seenSucsIds);
                }
            }
        }
    }

    private void validateZipFile(File zipFile, ValidationResult result,
          Map<String, String> seenIds, Map<Integer, String> seenSucsIds) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    String entryName = entry.getName();
                    int lastSlash = entryName.lastIndexOf('/');
                    String displayName = (lastSlash >= 0)
                                               ? entryName.substring(lastSlash + 1)
                                               : entryName;
                    String contextName = zipFile.getName() + "!" + displayName;

                    result.incrementFilesProcessed();
                    try (InputStream is = zip.getInputStream(entry)) {
                        validateFromStream(is, contextName, result, seenIds, seenSucsIds);
                    } catch (Exception ex) {
                        result.addMessage(new ValidationMessage(Severity.ERROR,
                              Category.YAML_PARSE_FAILURE, contextName, "N/A", null,
                              "Failed to parse YAML: " + ex.getMessage()));
                    }
                }
            }
        } catch (Exception ex) {
            result.addMessage(new ValidationMessage(Severity.ERROR, Category.YAML_PARSE_FAILURE,
                  zipFile.getName(), "N/A", null,
                  "Failed to read ZIP archive: " + ex.getMessage()));
        }
    }

    private void validateFile(File file, ValidationResult result,
          Map<String, String> seenIds, Map<Integer, String> seenSucsIds) {
        String fileName = file.getName();
        result.incrementFilesProcessed();

        try (InputStream fis = new FileInputStream(file)) {
            validateFromStream(fis, fileName, result, seenIds, seenSucsIds);
        } catch (Exception ex) {
            result.addMessage(new ValidationMessage(Severity.ERROR, Category.YAML_PARSE_FAILURE,
                  fileName, "N/A", null, "Failed to parse YAML: " + ex.getMessage()));
        }
    }

    private void validateFromStream(InputStream is, String fileName, ValidationResult result,
          Map<String, String> seenIds, Map<Integer, String> seenSucsIds) throws Exception {
        JsonNode tree = mapper.readTree(is);

        String treeId = getTreeSystemId(tree);
        validateTreeStructure(tree, fileName, treeId, result, seenSucsIds);

        PlanetarySystem system = mapper.treeToValue(tree, PlanetarySystem.class);
        validateSystem(system, fileName, result, seenIds);
    }

    // -------------------------------------------------------------------
    // Raw YAML tree structural checks
    // -------------------------------------------------------------------

    private String getTreeSystemId(JsonNode tree) {
        JsonNode idNode = tree.get("id");
        if (idNode != null && idNode.isTextual()) {
            return idNode.asText();
        }
        JsonNode nameNode = tree.get("name");
        if (nameNode != null && nameNode.isTextual()) {
            return nameNode.asText();
        }
        return "<unknown>";
    }

    private void validateTreeStructure(JsonNode tree, String fileName, String systemId,
          ValidationResult result, Map<Integer, String> seenSucsIds) {
        JsonNode sucsNode = tree.get("sucsId");
        if (sucsNode != null && sucsNode.isInt()) {
            int sucsId = sucsNode.asInt();
            String previousFile = seenSucsIds.put(sucsId, fileName);
            if (previousFile != null) {
                result.addMessage(new ValidationMessage(Severity.WARNING,
                      Category.DUPLICATE_SUCS_ID, fileName, systemId, null,
                      "Duplicate sucsId " + sucsId + " (also in " + previousFile + ")"));
            }
        }

        JsonNode planetArray = tree.get("planet");
        if (planetArray != null && planetArray.isArray()) {
            Map<Integer, Integer> positionCounts = new HashMap<>();
            for (JsonNode planetNode : planetArray) {
                Integer sysPos = extractSysPos(planetNode);
                if (sysPos != null) {
                    positionCounts.merge(sysPos, 1, Integer::sum);
                }
            }
            for (Map.Entry<Integer, Integer> entry : positionCounts.entrySet()) {
                if (entry.getValue() > 1) {
                    result.addMessage(new ValidationMessage(Severity.ERROR,
                          Category.DUPLICATE_PLANET_POSITION, fileName, systemId, null,
                          "Duplicate planet sysPos " + entry.getKey()
                                + " (" + entry.getValue() + " planets claim this position)"));
                }
            }
        }
    }

    private Integer extractSysPos(JsonNode planetNode) {
        JsonNode sysPosNode = planetNode.get("sysPos");
        if (sysPosNode == null) {
            return null;
        }
        if (sysPosNode.isObject()) {
            JsonNode valueNode = sysPosNode.get("value");
            if (valueNode != null && valueNode.isInt()) {
                return valueNode.asInt();
            }
            return null;
        }
        if (sysPosNode.isInt()) {
            return sysPosNode.asInt();
        }
        return null;
    }

    // -------------------------------------------------------------------
    // Typed system and planet validation
    // -------------------------------------------------------------------

    private void validateSystem(PlanetarySystem system, String fileName,
          ValidationResult result, Map<String, String> seenIds) {
        result.incrementSystemsValidated();
        String systemId = system.getId() != null ? system.getId() : "<null>";

        if (system.getId() != null) {
            String previousFile = seenIds.put(system.getId(), fileName);
            if (previousFile != null) {
                result.addMessage(new ValidationMessage(Severity.ERROR,
                      Category.DUPLICATE_SYSTEM_ID, fileName, systemId, null,
                      "Duplicate system ID (also in " + previousFile + ")"));
            }
        } else {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_SYSTEM_ID, fileName, systemId, null,
                  "Missing system ID"));
        }

        if (system.getX() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_COORDINATES, fileName, systemId, null,
                  "Missing X coordinate (xcood)"));
        }
        if (system.getY() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_COORDINATES, fileName, systemId, null,
                  "Missing Y coordinate (ycood)"));
        }

        if (system.getSourcedStar() == null || system.getStar() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_STAR, fileName, systemId, null,
                  "Missing spectralType (star)"));
        }

        // Check primary slot points to an actual planet, not just within count range
        int planetCount = system.getPlanets().size();
        if (planetCount == 0) {
            result.addMessage(new ValidationMessage(Severity.WARNING,
                  Category.NO_PLANETS, fileName, systemId, null,
                  "System has no planets"));
        } else {
            int primarySlot = system.getPrimaryPlanetPosition();
            Planet primaryPlanet = system.getPlanet(primarySlot);
            if (primaryPlanet == null) {
                result.addMessage(new ValidationMessage(Severity.ERROR,
                      Category.INVALID_PRIMARY_SLOT, fileName, systemId, null,
                      "Primary slot (" + primarySlot
                            + ") does not correspond to any planet (planet count: "
                            + planetCount + ")"));
            }
        }

        result.addPlanetsValidated(planetCount);
        for (Planet planet : system.getPlanets()) {
            validatePlanet(planet, fileName, systemId, result);
        }
    }

    private void validatePlanet(Planet planet, String fileName, String systemId,
          ValidationResult result) {
        String planetInfo = buildPlanetInfo(planet);

        Integer sysPos = planet.getSystemPosition();
        if (sysPos == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_PLANET_FIELD, fileName, systemId, planetInfo,
                  "Missing system position (sysPos)"));
        } else if (sysPos <= 0) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.INVALID_PLANET_POSITION, fileName, systemId, planetInfo,
                  "Invalid system position: " + sysPos + " (must be > 0)"));
        }

        PlanetaryType type = planet.getPlanetType();
        if (planet.getSourcedPlanetType() == null) {
            result.addMessage(new ValidationMessage(Severity.ERROR,
                  Category.MISSING_PLANET_FIELD, fileName, systemId, planetInfo,
                  "Missing planet type"));
        }

        Double gravity = planet.getGravity();
        if (gravity != null && gravity <= 0 && type != PlanetaryType.ASTEROID_BELT) {
            result.addMessage(new ValidationMessage(Severity.WARNING,
                  Category.INVALID_GRAVITY, fileName, systemId, planetInfo,
                  "Gravity is " + gravity + " (expected > 0 for non-asteroid)"));
        }

        if (type == PlanetaryType.TERRESTRIAL || type == PlanetaryType.GIANT_TERRESTRIAL
                  || type == PlanetaryType.DWARF_TERRESTRIAL) {
            validateTerrestrialCompleteness(planet, fileName, systemId, planetInfo, result);
        }

        validatePlanetEvents(planet, fileName, systemId, planetInfo, result);
    }

    private void validateTerrestrialCompleteness(Planet planet, String fileName, String systemId,
          String planetInfo, ValidationResult result) {
        LocalDate baseDate = LocalDate.of(1, 1, 1);

        if (planet.getPressure(baseDate) == null) {
            result.addMessage(new ValidationMessage(Severity.WARNING,
                  Category.MISSING_ATMOSPHERE_DATA, fileName, systemId, planetInfo,
                  "Terrestrial planet missing pressure"));
        }
        if (planet.getSourcedAtmosphere(baseDate) == null) {
            result.addMessage(new ValidationMessage(Severity.WARNING,
                  Category.MISSING_ATMOSPHERE_DATA, fileName, systemId, planetInfo,
                  "Terrestrial planet missing atmosphere"));
        }
    }

    private void validatePlanetEvents(Planet planet, String fileName, String systemId,
          String planetInfo, ValidationResult result) {
        List<PlanetaryEvent> events = planet.getEvents();
        if (events == null) {
            return;
        }

        for (PlanetaryEvent event : events) {
            if (event.percentWater != null && event.percentWater.getValue() != null) {
                int water = event.percentWater.getValue();
                if (water < MIN_WATER_PERCENT || water > MAX_WATER_PERCENT) {
                    result.addMessage(new ValidationMessage(Severity.WARNING,
                          Category.INVALID_WATER, fileName, systemId, planetInfo,
                          "Water percentage " + water + " outside valid range (0-100)"
                                + " at event " + event.date));
                }
            }

            if (event.population != null && event.population.getValue() != null) {
                long population = event.population.getValue();
                if (population < 0) {
                    result.addMessage(new ValidationMessage(Severity.WARNING,
                          Category.NEGATIVE_POPULATION, fileName, systemId, planetInfo,
                          "Negative population " + population + " at event " + event.date));
                }
            }

            if (event.temperature != null && event.temperature.getValue() != null) {
                int temp = event.temperature.getValue();
                if (temp < ABSOLUTE_ZERO_CELSIUS) {
                    result.addMessage(new ValidationMessage(Severity.WARNING,
                          Category.INVALID_TEMPERATURE, fileName, systemId, planetInfo,
                          "Temperature " + temp + "C is below absolute zero (-273C)"
                                + " at event " + event.date));
                }
            }

            if (!knownFactionCodes.isEmpty() && event.faction != null
                      && event.faction.getValue() != null) {
                for (String factionCode : event.faction.getValue()) {
                    if (!knownFactionCodes.contains(factionCode)) {
                        result.addMessage(new ValidationMessage(Severity.WARNING,
                              Category.UNKNOWN_FACTION, fileName, systemId, planetInfo,
                              "Unknown faction code '" + factionCode + "'"
                                    + " at event " + event.date));
                    }
                }
            }
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

        for (ValidationMessage msg : result.getMessages()) {
            System.out.println(msg);
        }

        if (!result.getMessages().isEmpty()) {
            System.out.println();
        }

        System.out.println("--- Summary ---");
        System.out.println(result.getSummary());

        if (result.hasErrors()) {
            System.exit(1);
        }
    }
}
