/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.logging.MMLogger;
import mekhq.utilities.MMDataLicenseHeader;

/**
 * A manifest containing IDs and file names of StratCon facility definitions
 *
 * @author NickAragua
 */
public class StratConFacilityManifest {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacilityManifest.class);

    private static final ObjectMapper MAPPER = buildMapper();

    public List<String> facilityFileNames = new ArrayList<>();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth, matching the shipped facility JSON files.
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
     * Attempt to deserialize an instance of a StratConFacilityManifest from the passed-in file path
     *
     * @return Possibly an instance of a StratConFacilityManifest
     */
    public static StratConFacilityManifest deserialize(String fileName) {
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", fileName);
            return null;
        }

        try {
            return MAPPER.readValue(inputFile, StratConFacilityManifest.class);
        } catch (Exception e) {
            LOGGER.error("Error Deserializing Facility Manifest", e);
            return null;
        }
    }

    /**
     * Writes this manifest to a JSON file, led by the MegaMek Data license header. The header carries the file's
     * existing copyright year forward; {@code ALLOW_YAML_COMMENTS} skips it on read.
     *
     * @param outputFile the destination file
     *
     * @return {@code true} if the file was written, {@code false} if an error occurred (logged)
     */
    public boolean serialize(File outputFile) {
        try {
            String content = MMDataLicenseHeader.licenseHeader(outputFile) + '\n' + MAPPER.writeValueAsString(this);
            Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error serializing facility manifest {}", outputFile.getPath(), e);
            return false;
        }
    }
}
