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
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.CAMPAIGN_OPERATIONS_REVISED;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.CAMPAIGN_OPERATIONS_STRICT;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Utility class for loading and managing PersonnelMarketEntry data from YAML files, storing them in maps keyed by
 * {@link PersonnelRole} for different market types.
 */
public class PersonnelMarketLibraries {
    // MekHQ
    private final Map<PersonnelRole, PersonnelMarketEntry> clanMarketMekHQ = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketMekHQ = new EnumMap<>(PersonnelRole.class);

    // Campaign Operations Revised
    private final Map<PersonnelRole, PersonnelMarketEntry> clanMarketCamOpsRevised = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketCamOpsRevised = new EnumMap<>(PersonnelRole.class);

    // Campaign Operations Strict
    private final Map<PersonnelRole, PersonnelMarketEntry> clanMarketCamOpsStrict = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketCamOpsStrict = new EnumMap<>(PersonnelRole.class);

    /**
     * Constructs a {@code PersonnelMarketLibraries} instance and loads market entries from YAML files into maps.
     */
    public PersonnelMarketLibraries() {
        // MekHQ
        clanMarketMekHQ.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH + MEKHQ.getFileNameClan()));
        innerSphereMarketMekHQ.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH +
                                                             MEKHQ.getFileNameInnerSphere()));

        // Campaign Operations Revised
        clanMarketCamOpsRevised.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH +
                                                              CAMPAIGN_OPERATIONS_REVISED.getFileNameClan()));
        innerSphereMarketCamOpsRevised.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH +
                                                                     CAMPAIGN_OPERATIONS_REVISED.getFileNameInnerSphere()));

        // Campaign Operations Strict
        clanMarketCamOpsRevised.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH +
                                                              CAMPAIGN_OPERATIONS_STRICT.getFileNameClan()));
        innerSphereMarketCamOpsRevised.putAll(readEntriesAsMap(PERSONNEL_MARKET_DIRECTORY_PATH +
                                                                     CAMPAIGN_OPERATIONS_STRICT.getFileNameInnerSphere()));
    }

    /**
     * Clan market entries configured for the MekHQ Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the MekHQ Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketMekHQ() {
        return clanMarketMekHQ;
    }

    /**
     * Inner Sphere market entries configured for the MekHQ Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the MekHQ Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketMekHQ() {
        return innerSphereMarketMekHQ;
    }

    /**
     * Clan market entries configured for the CamOps Revised Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the CamOps Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketCamOpsRevised() {
        return clanMarketCamOpsRevised;
    }

    /**
     * Inner Sphere market entries configured for the CamOps Revised Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the CamOps Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketCamOpsRevised() {
        return innerSphereMarketCamOpsRevised;
    }

    /**
     * Clan market entries configured for the CamOps Strict Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the CamOps Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketCamOpsStrict() {
        return clanMarketCamOpsStrict;
    }

    /**
     * Inner Sphere market entries configured for the CamOps Strict Market Style.
     *
     * @return a {@link Map} mapping {@link PersonnelRole} to {@link PersonnelMarketEntry} for the CamOps Market Style.
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketCamOpsStrict() {
        return innerSphereMarketCamOpsStrict;
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
