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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.logging.MMLogger;

/**
 * Class intended for local use that holds a manifest of scenario modifier definition file names.
 *
 * @author NickAragua
 */
class ScenarioModifierManifest {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioModifierManifest.class);

    private static final ObjectMapper MAPPER = buildMapper();

    public List<String> fileNameList = new ArrayList<>();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth, matching the shipped scenario modifier JSON files.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // Tolerate fields absent from older files rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Skip the leading '#' license-header comment lines (and any '#' notes marking disabled modifiers).
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        return mapper;
    }

    /**
     * Attempt to deserialize an instance of a scenario modifier list from the passed-in file
     *
     * @param fileName Name of the file that contains the scenario modifier list
     *
     * @return Possibly an instance of a scenario modifier list
     */
    public static ScenarioModifierManifest Deserialize(String fileName) {
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", fileName);
            return null;
        }

        try {
            return MAPPER.readValue(inputFile, ScenarioModifierManifest.class);
        } catch (Exception ex) {
            LOGGER.error("Error Deserializing Scenario Modifier List", ex);
            return null;
        }
    }
}
