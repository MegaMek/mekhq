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
package mekhq.campaign.digitalGM.stratCon;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON serialization for {@link StratConContractDefinition}. JSON is the write format for the shipped contract
 * definition files; XML (JAXB) is retained on the model for reading legacy/user files.
 *
 * <p>The mapper is configured for <strong>field-based</strong> access - it reads and writes the model's fields
 * directly, with getters/setters/creators disabled. This mirrors the state JAXB persists (which is field-backed) and
 * sidesteps the deprecated, JAXB-annotated accessors on the model. Mirrors {@code ScenarioTemplateJson}.
 */
public final class StratConContractDefinitionJson {

    private static final ObjectMapper MAPPER = buildMapper();

    /**
     * The MegaMek Data legal notice, emitted as a leading {@code _license} array in every saved definition. JSON has no
     * comment syntax, so the notice the XML files carried in an XML comment is written as data instead. It is ignored
     * on read (the mapper tolerates unknown properties), so it never reaches the model.
     */
    private static final String[] LICENSE_NOTICE = {
          "MegaMek Data (C) 2025-2026 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.",
          "To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/",
          "",
          "NOTICE: The MegaMek organization is a non-profit group of volunteers creating free software for the "
                + "BattleTech community.",
          "",
          "MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks of The Topps Company, Inc. "
                + "All Rights Reserved.",
          "",
          "Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of InMediaRes Productions, LLC.",
          "",
          "MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under Microsoft's "
                + "\"Game Content Usage Rules\" <https://www.xbox.com/en-US/developers/rules> and it is not endorsed "
                + "by or affiliated with Microsoft." };

    private StratConContractDefinitionJson() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use fields as the single source of truth; ignore getters/setters/creators so accessor naming does not distort
        // the JSON shape.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Tolerate fields absent from older files (and the injected _license) rather than failing the whole load.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * Reads a contract definition from a JSON file.
     *
     * @param inputFile the JSON file
     *
     * @return the deserialized definition
     *
     * @throws IOException if the file cannot be read or parsed
     */
    static StratConContractDefinition fromFile(File inputFile) throws IOException {
        return MAPPER.readValue(inputFile, StratConContractDefinition.class);
    }

    /**
     * Writes a contract definition to a JSON file, with the license notice as the leading property.
     *
     * @param definition the definition to write
     * @param outputFile the destination file
     *
     * @throws IOException if the file cannot be written
     */
    static void toFile(StratConContractDefinition definition, File outputFile) throws IOException {
        // Emit the license notice as the first property, then the definition's own fields. Injecting it into the tree
        // (rather than adding a model field) keeps it out of the model entirely.
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode license = root.putArray("_license");
        for (String line : LICENSE_NOTICE) {
            license.add(line);
        }
        root.setAll((ObjectNode) MAPPER.valueToTree(definition));
        MAPPER.writeValue(outputFile, root);
    }
}
