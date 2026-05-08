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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared YAML gateway for planetary system files.
 *
 * <p>Load, validation, tests, and editor saves should use this mapper so {@link SourceableValue}, {@link StarType},
 * and {@link SocioIndustrialData} round-trip consistently. Campaign saves embed complete override systems using this
 * same YAML representation inside the campaign XML.
 */
public final class PlanetarySystemYamlIO {

    private static final ObjectMapper MAPPER = buildMapper();

    private PlanetarySystemYamlIO() {

    }

    /** Returns the shared planetary-system YAML mapper. Treat the mapper as read-only after configuration. */
    public static ObjectMapper createMapper() {
        return MAPPER;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL,
              JsonInclude.Include.NON_NULL));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(SocioIndustrialData.class, new SocioIndustrialData.SocioIndustrialDataDeserializer());
        module.addDeserializer(StarType.class, new StarType.StarTypeDeserializer());
        module.addDeserializer(SourceableValue.class, new SourceableValue.SourceableValueDeserializer());
        module.addSerializer(SourceableValue.class, new SourceableValue.SourceableValueSerializer());
        module.addSerializer(SocioIndustrialData.class, ToStringSerializer.instance);
        module.addSerializer(StarType.class, ToStringSerializer.instance);
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    public static PlanetarySystem read(InputStream source) throws IOException {
        return MAPPER.readValue(source, PlanetarySystem.class);
    }

    public static PlanetarySystem read(String yaml) throws IOException {
        return read(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    public static void write(PlanetarySystem system, OutputStream destination) throws IOException {
        system.prepareForSerialization();
        MAPPER.writeValue(destination, system);
    }

    public static String writeToString(PlanetarySystem system) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(system, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /** Deep-copies a system through the same YAML path used for save/load round trips. */
    public static PlanetarySystem copy(PlanetarySystem system) throws IOException {
        return read(writeToString(system));
    }

}

