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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.universe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import megamek.common.preference.PreferenceManager;
import mekhq.MHQConstants;

public final class PlanetarySystemYamlIO {

    private static final String EDITS_DIRECTORY = "edits";

    private PlanetarySystemYamlIO() {

    }

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(SocioIndustrialData.class, new SocioIndustrialData.SocioIndustrialDataDeserializer());
        module.addDeserializer(StarType.class, new StarType.StarTypeDeserializer());
        module.addDeserializer(SourceableValue.class, new SourceableValue.SourceableValueDeserializer());
        module.addSerializer(SourceableValue.class, new SourceableValue.SourceableValueSerializer());
        module.addSerializer(SocioIndustrialData.class, ToStringSerializer.instance);
        module.addSerializer(StarType.class, ToStringSerializer.instance);
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    public static PlanetarySystem read(InputStream source) throws IOException {
        return createMapper().readValue(source, PlanetarySystem.class);
    }

    public static void write(PlanetarySystem system, OutputStream destination) throws IOException {
        system.prepareForSerialization();
        createMapper().writeValue(destination, system);
    }

    public static PlanetarySystem copy(PlanetarySystem system) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(system, outputStream);
        return read(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    public static Path saveUserSystem(PlanetarySystem system) throws IOException {
        if ((system.getId() == null) || system.getId().isBlank()) {
            throw new IOException("Cannot save planetary system edits without a system id.");
        }

        Path destination = getUserSystemPath(system.getId());
        Files.createDirectories(destination.getParent());
        if (Files.exists(destination)) {
            Path backup = destination.resolveSibling(destination.getFileName() + "_backup");
            Files.copy(destination, backup, StandardCopyOption.REPLACE_EXISTING);
        }

        Path tempFile = Files.createTempFile(destination.getParent(), sanitizeFileName(system.getId()), ".tmp");
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                write(system, outputStream);
            }
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Files.deleteIfExists(tempFile);
            throw ex;
        }

        return destination;
    }

    public static Path getUserSystemPath(String systemId) throws IOException {
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if ((userDir == null) || userDir.isBlank()) {
            throw new IOException("Cannot locate planetary system edits until a user data directory is configured.");
        }
        if ((systemId == null) || systemId.isBlank()) {
            throw new IOException("Cannot locate planetary system edits without a system id.");
        }

        Path editsDirectory = Paths.get(userDir, MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH, EDITS_DIRECTORY);
        return editsDirectory.resolve(sanitizeFileName(systemId) + ".yml");
    }

    public static boolean hasUserSystemOverride(String systemId) throws IOException {
        return Files.exists(getUserSystemPath(systemId));
    }

    public static boolean deleteUserSystem(String systemId) throws IOException {
        return Files.deleteIfExists(getUserSystemPath(systemId));
    }

    /** Returns the directory containing all user planetary system override files. */
    public static Path getEditsDirectory() throws IOException {
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if ((userDir == null) || userDir.isBlank()) {
            throw new IOException("Cannot locate planetary system edits until a user data directory is configured.");
        }
        return Paths.get(userDir, MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH, EDITS_DIRECTORY);
    }

    /** Returns the .yml override files currently in the user edits directory. Returns an empty list if missing. */
    public static List<Path> listOverrideFiles() throws IOException {
        Path editsDirectory = getEditsDirectory();
        if (!Files.isDirectory(editsDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(editsDirectory)) {
            List<Path> files = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                  .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                  .forEach(files::add);
            return files;
        }
    }

    /**
     * Returns the (sanitized) base names of the .yml override files in the user edits directory, with the trailing
     * ".yml" suffix stripped. Useful for cheap membership checks (callers should sanitize their candidate ids the
     * same way via {@link #sanitizeFileName(String)} before lookup).
     */
    public static Set<String> listOverrideFileBaseNames() throws IOException {
        Set<String> names = new HashSet<>();
        for (Path file : listOverrideFiles()) {
            String fileName = file.getFileName().toString();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                names.add(fileName.substring(0, fileName.length() - 4));
            }
        }
        return names;
    }

    /**
     * Writes all override .yml files into the given zip file.
     *
     * @return the number of files exported.
     */
    public static int exportOverrides(Path zipFile) throws IOException {
        List<Path> overrides = listOverrideFiles();
        Files.createDirectories(zipFile.toAbsolutePath().getParent());
        try (ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path override : overrides) {
                ZipEntry entry = new ZipEntry(override.getFileName().toString());
                zipOutput.putNextEntry(entry);
                Files.copy(override, zipOutput);
                zipOutput.closeEntry();
            }
        }
        return overrides.size();
    }

    /** Resolution requested by the user when an imported file collides with an existing override. */
    public enum OverrideImportResolution {
        /** Skip importing this file. */
        SKIP,
        /** Overwrite the existing override file. */
        OVERWRITE,
        /** Cancel the entire import. */
        CANCEL
    }

    /** Callback invoked when an imported file already exists in the edits directory. */
    @FunctionalInterface
    public interface ConflictResolver {
        OverrideImportResolution resolve(String fileName);
    }

    /** Result of an override import operation. */
    public static final class ImportSummary {
        private final int imported;
        private final int skipped;
        private final boolean cancelled;

        ImportSummary(int imported, int skipped, boolean cancelled) {
            this.imported = imported;
            this.skipped = skipped;
            this.cancelled = cancelled;
        }

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * Reads {@code zipFile} and copies each .yml entry into the user edits directory. Existing files trigger the given
     * conflict resolver.
     */
    public static ImportSummary importOverrides(Path zipFile, ConflictResolver resolver) throws IOException {
        Path editsDirectory = getEditsDirectory();
        Files.createDirectories(editsDirectory);
        int imported = 0;
        int skipped = 0;
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String rawName = entry.getName();
                    if (!rawName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                        skipped++;
                        continue;
                    }
                    // Strip any directory components in the zip entry to avoid path traversal.
                    String safeName = Paths.get(rawName).getFileName().toString();
                    Path target = editsDirectory.resolve(safeName).normalize();
                    if (!target.startsWith(editsDirectory)) {
                        skipped++;
                        continue;
                    }
                    if (Files.exists(target) && (resolver != null)) {
                        OverrideImportResolution choice = resolver.resolve(safeName);
                        if (choice == OverrideImportResolution.CANCEL) {
                            return new ImportSummary(imported, skipped, true);
                        }
                        if (choice == OverrideImportResolution.SKIP) {
                            skipped++;
                            continue;
                        }
                    }
                    Files.copy(zipInput, target, StandardCopyOption.REPLACE_EXISTING);
                    imported++;
                } finally {
                    zipInput.closeEntry();
                }
            }
        }
        return new ImportSummary(imported, skipped, false);
    }

    static String sanitizeFileName(String fileName) {
        String sanitized = fileName.toLowerCase(Locale.ROOT)
                                 .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_")
                                 .replaceAll("[ .]+$", "")
                                 .trim();
        return sanitized.isBlank() ? "planetary_system" : sanitized;
    }
}

