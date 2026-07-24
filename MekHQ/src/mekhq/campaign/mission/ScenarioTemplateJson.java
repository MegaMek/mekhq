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
package mekhq.campaign.mission;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON serialization for {@link ScenarioTemplate} and its object graph. JSON is the write format for standalone
 * scenario template files; XML (JAXB) is retained on the model for reading legacy/user files and for the template
 * fragment embedded in campaign saves.
 *
 * <p>The mapper is configured for <strong>field-based</strong> access - it reads and writes the model's fields
 * directly, with getters/setters/creators disabled. This mirrors the state JAXB persists (which is field-backed) while
 * avoiding the model's accessor quirks, such as {@code getAllowedTerrainType()} not matching the
 * {@code allowedTerrainTypes} field name, or getter-only collections like {@code roleChoices}.
 *
 * <p>NOTE: Phase 3.0 spike. Only the mapper configuration and generic helpers exist so far; the per-class handling
 * (enum name-string adapters with graceful fallback, and the full round-trip API) is filled in during 3.1, gated by the
 * cross-format equivalence harness (0.5).
 */
public final class ScenarioTemplateJson {

    private static final ObjectMapper MAPPER = buildMapper();

    private ScenarioTemplateJson() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth; ignore getters/setters/creators so accessor naming and
        // getter-only collections do not distort the JSON shape.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Tolerate fields that are absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * Serializes an object from the scenario template graph to a JSON string.
     */
    static String toJson(Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsString(value);
    }

    /**
     * Deserializes a JSON string into an instance of the given type from the scenario template graph.
     */
    static <T> T fromJson(String json, Class<T> type) throws JsonProcessingException {
        return MAPPER.readValue(json, type);
    }

    /**
     * Reads a scenario template from a JSON file.
     *
     * @param inputFile the JSON file
     *
     * @return the deserialized template
     *
     * @throws IOException if the file cannot be read or parsed
     */
    static ScenarioTemplate fromFile(File inputFile) throws IOException {
        return MAPPER.readValue(inputFile, ScenarioTemplate.class);
    }

    /**
     * Writes a scenario template to a JSON file.
     *
     * @param template   the template to write
     * @param outputFile the destination file
     *
     * @throws IOException if the file cannot be written
     */
    static void toFile(ScenarioTemplate template, File outputFile) throws IOException {
        MAPPER.writeValue(outputFile, template);
    }
}
