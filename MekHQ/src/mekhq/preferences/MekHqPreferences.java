/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.preferences;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import mekhq.MekHQ;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The root class for MekHQ user nameToPreferencesMap system.
 */
public class MekHqPreferences {
    private static final String PREFERENCES_TOKEN = "preferences";
    private static final String CLASS_TOKEN = "class";
    private static final String ELEMENTS_TOKEN = "elements";
    private static final String NAME_TOKEN = "element";
    private static final String VALUE_TOKEN = "value";
    private final Map<String, PreferencesNode> nameToPreferencesMap;

    public MekHqPreferences() {
        this.nameToPreferencesMap = new HashMap<>();
    }

    public void loadFromFile(String filePath) {
        final String METHOD_NAME = "loadFromFile";

        try {
            try (FileInputStream input = new FileInputStream(filePath)) {
                MekHQ.getLogger().info(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Loading MekHQ user preferences from: " + filePath);

                JsonFactory factory = new JsonFactory();
                JsonParser parser = factory.createParser(input);

                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IOException("Expected an object start ({)" + getParserInformation(parser));
                }

                if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(PREFERENCES_TOKEN)) {
                    throw new IOException("Expected a field called (" + PREFERENCES_TOKEN + ")" + getParserInformation(parser));
                }

                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new IOException("Expected an array start ([)" + getParserInformation(parser));
                }

                // Parse all PreferencesNode
                while (parser.nextToken() != JsonToken.END_ARRAY){
                    try {
                        readPreferencesNode(parser, this.nameToPreferencesMap);
                    }
                    catch (IOException e) {
                        MekHQ.getLogger().error(
                                MekHqPreferences.class,
                                METHOD_NAME,
                                "Error reading node. " + getParserInformation(parser),
                                e);
                    }
                }

                parser.close();

                MekHQ.getLogger().info(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Finished loading user preferences");
            }
        }
        catch (FileNotFoundException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "No MekHQ user preferences file found: " + filePath,
                    e);
        }
        catch (IOException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Error reading from the user preferences file: " + filePath,
                    e);
        }
    }

    public void saveToFile(String filePath) {
        final String METHOD_NAME = "saveToFile";

        try {
            try (FileOutputStream output = new FileOutputStream(filePath)) {
                MekHQ.getLogger().debug(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Saving MekHQ nameToPreferencesMap to: " + filePath);

                JsonFactory factory = new JsonFactory();
                JsonGenerator writer = factory.createGenerator(output);
                writer = writer.useDefaultPrettyPrinter();
                writer.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);

                writer.writeStartObject();
                writer.writeFieldName(PREFERENCES_TOKEN);
                writer.writeStartArray();

                // Write each PreferencesNode
                for(Map.Entry<String, PreferencesNode> preferences : this.nameToPreferencesMap.entrySet()) {
                    writePreferencesNode(writer, preferences);
                }

                writer.writeEndArray();
                writer.writeEndObject();

                writer.close();
            }
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Could not save nameToPreferencesMap to: " + filePath,
                    e);
        } catch (IOException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Error writing to the nameToPreferencesMap file: " + filePath,
                    e);
        }
    }

    public PreferencesNode forClass(Class classToManage) {
        PreferencesNode preferences = this.nameToPreferencesMap.getOrDefault(classToManage.getName(), null);
        if (preferences == null) {
            preferences = new PreferencesNode(classToManage);
            this.nameToPreferencesMap.put(classToManage.getName(), preferences);
        }

        return preferences;
    }

    private static void readPreferencesNode(JsonParser parser, Map<String, PreferencesNode> nodes) throws IOException {
        final String METHOD_NAME = "readPreferencesNode";

        String className;
        HashMap<String, String> elements = new HashMap<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return;
        }

        if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(CLASS_TOKEN)) {
            return;
        }

        className = parser.nextTextValue();

        if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(ELEMENTS_TOKEN)) {
            return;
        }

        if (parser.nextToken() != JsonToken.START_ARRAY) {
            return;
        }

        // Parse all PreferenceElement in this node
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            try {
                readPreferenceElement(parser, elements);
            }
            catch (IOException e) {
                MekHQ.getLogger().warning(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Error reading elements for node: " + className + ".",
                        e);
            }
        }

        try {
            PreferencesNode node = new PreferencesNode(Class.forName(className));
            node.initialize(elements);
            nodes.put(node.getNodeName(), node);
        }
        catch (ClassNotFoundException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "No class with name " + className + " found",
                    e);
        }
    }

    private static void readPreferenceElement(JsonParser parser, HashMap<String, String> elements) throws IOException {
        String name;
        String value;

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return;
        }

        if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(NAME_TOKEN)) {
            return;
        }

        name = parser.nextTextValue();

        if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(VALUE_TOKEN)) {
            return;
        }

        value = parser.nextTextValue();

        if (parser.nextToken() != JsonToken.END_OBJECT) {
            return;
        }

        elements.put(name, value);
    }

    private static void writePreferencesNode(JsonGenerator writer, Map.Entry<String, PreferencesNode> nodeInfo) throws IOException {
        writer.writeStartObject();
        writer.writeStringField(CLASS_TOKEN, nodeInfo.getKey());
        writer.writeFieldName(ELEMENTS_TOKEN);
        writer.writeStartArray();

        // Write all PreferenceElement in this node
        for (Map.Entry<String, String> element : nodeInfo.getValue().getFinalValues().entrySet()) {
            writePreferenceElement(writer, element);
        }

        writer.writeEndArray();
        writer.writeEndObject();
    }

    private static void writePreferenceElement(JsonGenerator writer, Map.Entry<String, String> element) throws IOException {
        writer.writeStartObject();
        writer.writeStringField(NAME_TOKEN, element.getKey());
        writer.writeStringField(VALUE_TOKEN, element.getValue());
        writer.writeEndObject();
    }

    private static String getParserInformation(JsonParser parser) throws IOException {

        if (parser == null) {
            return "";
        }

        return ". Current token: " + parser.getCurrentName() +
                ". Line number: " + parser.getCurrentLocation().getLineNr() +
                ". Column number: " + parser.getCurrentLocation().getColumnNr();
    }
}
