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
 * Loads and provides access to different personnel market entry configurations  for various market styles (e.g., MekHQ,
 * Campaign Operations Revised, Campaign Operations Strict).
 *
 * <p>This class initializes its data from YAML configuration files, mapping each personnel role to its corresponding
 * market entry parameters for Clan and Inner Sphere contexts, according to the selected ruleset.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>Instantiate to load all market entry data from files.</li>
 *     <li>Retrieve Clan/Inner Sphere market maps with the appropriate getter.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.06
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
     * Initializes all personnel market maps by loading entry lists from YAML configuration files for each supported
     * market style and theater (Clan/Inner Sphere).
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
     * Reads a list of personnel market entries from a YAML file and returns a mapping from personnel role to entry.
     *
     * @param fileAddress the full path to the YAML file
     *
     * @return a map linking each {@link PersonnelRole} to its {@link PersonnelMarketEntry}, or an empty map if
     *       unavailable
     *
     * @throws RuntimeException if the file could not be read or parsed
     * @author Illiani
     * @since 0.50.06
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
