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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MMDataLicenseHeader;

/**
 * JSON serialization for {@link StratConFacilityDefinition}. Mirrors {@code AtBScenarioModifierJson} and
 * {@code StratConContractDefinitionJson}: a field-based mapper, a leading {@code #} license header on write, and
 * {@code #}-comment skipping on read.
 *
 * <p>Files in the format used before 0.51.01 - one side of one facility type, with flat effect fields - still load.
 * Each becomes a one-sided definition, and {@link StratConFacilityFactory} merges the two sides of a type back
 * together through their captured definitions.</p>
 */
public final class StratConFacilityJson {

    private static final ObjectMapper MAPPER = buildMapper();

    private static final String JSON_EXTENSION = ".json";

    private StratConFacilityJson() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth; ignore getters/setters/creators so accessor naming and the many
        // computed getters (getProfileFor, getBiomeTempMap, ...) do not distort the JSON shape.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Leave out absent profiles and descriptions rather than writing nulls.
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        // Tolerate fields absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Skip the leading '#' license-header comment lines that lead every saved file.
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        return mapper;
    }

    /**
     * A facility definition read from a file, and what the file said about the definition's other side if it was in
     * the old format.
     *
     * @param definition         the definition; one-sided if the file was in the old format
     * @param isLegacyFormat     whether the file was in the format used before 0.51.01
     * @param capturedDefinition for an old-format file, the file holding the other side of the same type; otherwise
     *                           {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record LoadedFacilityDefinition(StratConFacilityDefinition definition, boolean isLegacyFormat,
          @Nullable String capturedDefinition) {
    }

    /**
     * Reads a StratCon facility definition from a JSON file, in either the current or the old format. A definition
     * with no {@code id} takes the file name, less its extension.
     *
     * @param inputFile the JSON file
     *
     * @return the definition
     *
     * @throws IOException if the file cannot be read or parsed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static LoadedFacilityDefinition fromFile(File inputFile) throws IOException {
        JsonNode tree = MAPPER.readTree(inputFile);
        String fileId = idFromFileName(inputFile.getName());

        if (isLegacyFormat(tree)) {
            LegacyStratConFacilityData legacyData = MAPPER.treeToValue(tree, LegacyStratConFacilityData.class);
            return new LoadedFacilityDefinition(legacyData.toDefinition(fileId), true, legacyData.capturedDefinition);
        }

        StratConFacilityDefinition definition = MAPPER.treeToValue(tree, StratConFacilityDefinition.class);
        if ((definition.getId() == null) || definition.getId().isBlank()) {
            definition.setId(fileId);
        }
        definition.rebuildBiomeTempMap();
        return new LoadedFacilityDefinition(definition, false, null);
    }

    /**
     * Writes a StratCon facility definition to a JSON file, led by the MegaMek Data license header.
     *
     * @param definition the definition to write
     * @param outputFile the destination file
     *
     * @throws IOException if the file cannot be written
     */
    public static void toFile(StratConFacilityDefinition definition, File outputFile) throws IOException {
        // Lead the file with the license header (as '#' comment lines) then a blank line, then the JSON. The header
        // carries the file's copyright year forward; ALLOW_YAML_COMMENTS skips it on read.
        String content = MMDataLicenseHeader.licenseHeader(outputFile) + '\n' + MAPPER.writeValueAsString(definition);
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
    }

    /**
     * @param tree a parsed facility file
     *
     * @return {@code true} if the file is in the format used before 0.51.01: it names an owner and has no profiles
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isLegacyFormat(JsonNode tree) {
        return tree.has("owner") && !tree.has("alliedProfile") && !tree.has("hostileProfile");
    }

    /**
     * @param fileName a facility file name
     *
     * @return the file name less its {@code .json} extension
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String idFromFileName(String fileName) {
        String trimmedName = fileName.trim();
        return trimmedName.toLowerCase().endsWith(JSON_EXTENSION) ?
                     trimmedName.substring(0, trimmedName.length() - JSON_EXTENSION.length()) :
                     trimmedName;
    }
}
