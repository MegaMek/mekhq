/*
 * SourceableValue.java
 *
 * Copyright (c) 2025 - The MegaMek team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * This generic class is designed to hold an absract value and a string
 * that indicates the source of that value. It is designed primarily to work
 * with planetary information, but could be used for other in-universe
 * sourceable information.
**/
@JsonIgnoreProperties(ignoreUnknown=true)
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

    public static class SourceableValueDeserializer extends JsonDeserializer<SourceableValue<Object>> implements ContextualDeserializer {

        private JavaType valueType;

        public SourceableValueDeserializer() {} // Default constructor for Jackson

        private SourceableValueDeserializer(JavaType valueType) {
            this.valueType = valueType;
        }

        /**
         * The purpose of this rather complex custom deserializer is that we want to allow for direct entry of
         * values in the yaml when it is not sourced, but we want it split into a source, value pair when it has
         * been sourced. That requires a custom deserializer of a generic class which is rather involved.
         */
        @Override
        public SourceableValue<Object> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
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
        public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
            JavaType type = context.getContextualType();
            JavaType containedType = (type != null && type.containedTypeCount() > 0) ? type.containedType(0) : context.constructType(Object.class);
            return new SourceableValueDeserializer(containedType);
        }
    }


}
