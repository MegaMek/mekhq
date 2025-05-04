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
package mekhq.campaign.market.personnelMarket.yaml;

import static mekhq.MHQConstants.PERSONNEL_MARKET_DIRECTORY_PATH;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mekhq.campaign.market.personnelMarket.PersonnelMarketEntry;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Utility class for loading and managing PersonnelMarketEntry data from YAML files, storing them in maps keyed by
 * {@link PersonnelRole} for different market types.
 */
public class PersonnelMarketLibraries {
    private static final String CLAN_MARKET_FILE = PERSONNEL_MARKET_DIRECTORY_PATH + "clanMarket.yaml";
    private static final String INNER_SPHERE_MARKET_FILE = PERSONNEL_MARKET_DIRECTORY_PATH + "innerSphereMarket.yaml";

    private final Map<PersonnelRole, PersonnelMarketEntry> clanMarketEntries = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketEntries = new EnumMap<>(PersonnelRole.class);

    /**
     * Constructs a {@code PersonnelMarketLibraries} instance and loads market entries from YAML files into maps.
     */
    public PersonnelMarketLibraries() {
        clanMarketEntries.putAll(readEntriesAsMap(CLAN_MARKET_FILE));
        innerSphereMarketEntries.putAll(readEntriesAsMap(INNER_SPHERE_MARKET_FILE));
    }

    /**
     * Returns the map of PersonnelMarketEntry objects for the Clan market, keyed by profession.
     *
     * @return Clan market entries as a map from PersonnelRole to PersonnelMarketEntry
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketEntries() {
        return clanMarketEntries;
    }

    /**
     * Returns the map of PersonnelMarketEntry objects for the Inner Sphere market, keyed by profession.
     *
     * @return Inner Sphere market entries as a map from PersonnelRole to PersonnelMarketEntry
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketEntries() {
        return innerSphereMarketEntries;
    }

    /**
     * Reads a YAML file containing a list of PersonnelMarketEntry objects and converts it to a map keyed by their
     * {@code profession} field.
     *
     * @param fileAddress the path to the YAML file
     *
     * @return a map with PersonnelRole as the key and PersonnelMarketEntry as the value
     *
     * @throws RuntimeException if there is an error reading or parsing the file
     */
    private Map<PersonnelRole, PersonnelMarketEntry> readEntriesAsMap(String fileAddress) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            List<PersonnelMarketEntry> entries = objectMapper.readValue(new File(fileAddress),
                  objectMapper.getTypeFactory().constructCollectionType(List.class, PersonnelMarketEntry.class));
            Map<PersonnelRole, PersonnelMarketEntry> map = new EnumMap<>(PersonnelRole.class);
            if (entries != null) {
                for (PersonnelMarketEntry entry : entries) {
                    map.put(entry.profession(), entry);
                }
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException("Error reading personnel market entries from file: " + fileAddress, e);
        }
    }
}
