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
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mekhq.utilities.MMDataLicenseHeader;

/**
 * JSON serialization for {@link StratConFacility}. Mirrors {@code AtBScenarioModifierJson} and
 * {@code StratConContractDefinitionJson}: a field-based mapper, a leading {@code #} license header on write, and
 * {@code #}-comment skipping on read.
 */
public final class StratConFacilityJson {

    private static final ObjectMapper MAPPER = buildMapper();

    private StratConFacilityJson() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth; ignore getters/setters/creators so accessor naming and the many
        // computed getters (isVisible, isAvailable, getBiomeTempMap, ...) do not distort the JSON shape.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Tolerate fields absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Skip the leading '#' license-header comment lines that lead every saved file.
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        return mapper;
    }

    /**
     * Reads a StratCon facility from a JSON file.
     *
     * @param inputFile the JSON file
     *
     * @return the deserialized facility
     *
     * @throws IOException if the file cannot be read or parsed
     */
    static StratConFacility fromFile(File inputFile) throws IOException {
        return MAPPER.readValue(inputFile, StratConFacility.class);
    }

    /**
     * Writes a StratCon facility to a JSON file, led by the MegaMek Data license header.
     *
     * @param facility   the facility to write
     * @param outputFile the destination file
     *
     * @throws IOException if the file cannot be written
     */
    static void toFile(StratConFacility facility, File outputFile) throws IOException {
        // Lead the file with the license header (as '#' comment lines) then a blank line, then the JSON. The header
        // carries the file's copyright year forward; ALLOW_YAML_COMMENTS skips it on read.
        String content = MMDataLicenseHeader.licenseHeader(outputFile) + '\n' + MAPPER.writeValueAsString(facility);
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
    }
}
