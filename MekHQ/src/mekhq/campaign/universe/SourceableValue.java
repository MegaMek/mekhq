/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

/**
 * This generic class is designed to hold an abstract value and a string that indicates the source of that value. It is
 * designed primarily to work with planetary information, but could be used for other in-universe sourceable
 * information.
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceableValue<T> {

    @JsonProperty("source")
    private String source;

    @JsonProperty("value")
    private T value;

    public String getSource() {
        return source;
    }

    public T getValue() {
        return value;
    }

    public boolean isCanon() {
        return (null != source);
    }

    public static class SourceableValueDeserializer extends JsonDeserializer<SourceableValue<Object>>
          implements ContextualDeserializer {

        private JavaType valueType;

        public SourceableValueDeserializer() {} // Default constructor for Jackson

        private SourceableValueDeserializer(JavaType valueType) {
            this.valueType = valueType;
        }

        /**
         * The purpose of this rather complex custom deserializer is that we want to allow for direct entry of values in
         * the yaml when it is not sourced, but we want it split into a source, value pair when it has been sourced.
         * That requires a custom deserializer of a generic class which is rather involved.
         */
        @Override
        public SourceableValue<Object> deserialize(JsonParser jsonParser, DeserializationContext context)
              throws IOException {
            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            JsonNode root = mapper.readTree(jsonParser);

            // set up initial values we need
            String source = null;
            Object value = null; // Use Object since we don’t know T

            // If the type is known, use it; otherwise, use Object
            JavaType actualType = (valueType != null) ? valueType : context.constructType(Object.class);

            if (root.isObject()) {
                // then we have children nodes, and we can get source and value from the children
                JsonNode sourceNode = root.get("source");
                if (sourceNode != null) {
                    source = sourceNode.asText();
                }

                JsonNode valueNode = root.get("value");
                if (valueNode != null) {
                    // this code will continue to process the node using our annotation structure
                    value = mapper.readValue(mapper.treeAsTokens(valueNode), actualType);
                }
            } else {
                // this code will continue to process the node using our annotation structure
                value = mapper.readValue(mapper.treeAsTokens(root), actualType);
            }

            // create final object for output
            SourceableValue<Object> sourceableValue = new SourceableValue<>();
            sourceableValue.source = source;
            sourceableValue.value = value;

            return sourceableValue;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            JavaType type = context.getContextualType();
            JavaType containedType = (type != null && type.containedTypeCount() > 0) ?
                                           type.containedType(0) :
                                           context.constructType(Object.class);
            return new SourceableValueDeserializer(containedType);
        }
    }


}
