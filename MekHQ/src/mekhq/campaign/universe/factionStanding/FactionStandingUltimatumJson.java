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
package mekhq.campaign.universe.factionStanding;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mekhq.utilities.MMDataLicenseHeader;

/**
 * JSON reading and writing for Faction Standing ultimatum files and their manifest, configured to match the other data
 * JSON files: unknown fields are tolerated and a leading {@code #} license-header block is skipped.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class FactionStandingUltimatumJson {
    private static final ObjectMapper MAPPER = buildMapper();

    private FactionStandingUltimatumJson() {}

    /**
     * @return the mapper shared by every ultimatum read and write
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Tolerate fields absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Skip the leading '#' license-header comment lines that lead every data file.
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        return mapper;
    }

    /**
     * Reads a JSON file into the given type.
     *
     * @param inputFile the file to read
     * @param type      the type to read it as
     *
     * @return the parsed value
     *
     * @throws IOException if the file cannot be read or parsed
     * @author Illiani
     * @since 0.51.01
     */
    static <T> T fromFile(File inputFile, Class<T> type) throws IOException {
        return MAPPER.readValue(inputFile, type);
    }

    /**
     * Writes an ultimatum or manifest to a JSON file, led by the MegaMek Data license header (as {@code #} comment
     * lines, which reading skips). The header carries the file's existing copyright year forward.
     *
     * @param value      the ultimatum or manifest to write
     * @param outputFile the destination file
     *
     * @throws IOException if the file cannot be written
     * @author Illiani
     * @since 0.51.01
     */
    static void toFile(Object value, File outputFile) throws IOException {
        String content = MMDataLicenseHeader.licenseHeader(outputFile) + '\n' + MAPPER.writeValueAsString(value)
                               + '\n';
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
    }
}
