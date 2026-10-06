/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios.atb;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.logging.MMLogger;

/**
 * A manifest containing IDs and file names of scenario template definitions
 *
 * @author NickAragua
 */
public class AtBScenarioManifest {
    private static final MMLogger logger = MMLogger.create(AtBScenarioManifest.class);

    private static final ObjectMapper MAPPER = buildMapper();

    public Map<Integer, String> scenarioFileNames;

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth, matching the shipped scenario template JSON files.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // Tolerate fields absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Skip the leading '#' license-header comment lines that lead every saved file.
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        return mapper;
    }

    /**
     * Attempt to deserialize an instance of an AtBScenarioManifest from the passed-in file path
     *
     * @return Possibly an instance of a ScenarioManifest
     */
    public static AtBScenarioManifest Deserialize(String fileName) {
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            logger.warn("Specified file {} does not exist", fileName);
            return null;
        }

        try {
            return MAPPER.readValue(inputFile, AtBScenarioManifest.class);
        } catch (Exception e) {
            logger.error("Error Deserializing Scenario Manifest", e);
            return null;
        }
    }
}
