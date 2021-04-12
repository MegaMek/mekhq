/*
 * Copyright (C) 2018 - The MegaMek Team. All Rights Reserved
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.personnel;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import megamek.common.annotations.Nullable;
import mekhq.campaign.io.Migration.PersonMigrator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.AwardSet;

/**
 * This class is responsible to control the awards. It loads one instance of each awards, then it creates a copy of it
 * once it needs to be awarded to someone.
 * @author Miguel Azevedo
 */
public class AwardsFactory {
    private static AwardsFactory instance = null;

    /**
     * Here is where the blueprints are stored, mapped by set and name.
     */
    private Map<String, Map<String, Award>> awardsMap;

    private AwardsFactory() {
        awardsMap = new HashMap<>();

        loadAwards();
    }

    public static AwardsFactory getInstance() {
        if (instance == null) {
            instance = new AwardsFactory();
        }

        return instance;
    }

    /**
     * @return the names of the all the award sets loaded.
     */
    public List<String> getAllSetNames() {
        return new ArrayList<>(awardsMap.keySet());
    }

    /**
     * Gets a list of all awards that belong to a given Set
     * @param setName is the name of the set
     * @return list with the awards belonging to that set
     */
    public List<Award> getAllAwardsForSet(String setName) {
        return new ArrayList<>(awardsMap.get(setName).values());
    }

    /**
     * By searching the "blueprints" (i.e. awards instances that serve as data model), it generates
     * a copy of that award in order for it to be given to someone.
     * @param setName the name of the set
     * @param awardName the name of the award
     * @return the copied award, or null if one cannot be copied
     */
    public @Nullable Award generateNew(final String setName, final String awardName) {
        final Map<String, Award> awardSet = awardsMap.get(setName);
        if (awardSet == null) {
            return null;
        }
        final Award award = awardSet.get(awardName);
        return (award == null) ? null : award.createCopy();
    }

    /**
     * Generates a new award from an XML entry (when loading game, for example)
     * @param node xml node
     * @param defaultSetMigration whether or not to check if the default set needs to be migrated
     * @return an award
     */
    public @Nullable Award generateNewFromXML(final Node node, final boolean defaultSetMigration) {
        String name = null;
        String set = null;
        List<LocalDate> dates = new ArrayList<>();

        try {
            NodeList nl = node.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    dates.add(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("set")) {
                    set = wn2.getTextContent().trim();
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        if (defaultSetMigration && "Default Set".equalsIgnoreCase(set)) {
            name = (name == null) ? "" : name;
            final String newName = PersonMigrator.awardDefaultSetMigrator(name);
            if (newName != null) {
                set = "standard";
                name = newName;
            }
        }

        final Award award = generateNew(set, name);
        if (award != null) {
            award.setDates(dates);
        }
        return award;
    }

    /**
     * Generates the "blueprint" awards by reading the data from XML sources.
     */
    private void loadAwards() {
        File dir = new File(MekHQ.getMekHQOptions().getAwardsDirectoryPath());
        File[] files = dir.listFiles((dir1, filename) -> filename.endsWith(".xml"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            try (InputStream inputStream = new FileInputStream(file)) {
                loadAwardsFromStream(inputStream, file.getName());
            } catch (IOException e) {
                MekHQ.getLogger().error(e);
            }
        }
    }

    public void loadAwardsFromStream(InputStream inputStream, String fileName) {
        AwardSet awardSet;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AwardSet.class, Award.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            awardSet = unmarshaller.unmarshal(MekHqXmlUtil.createSafeXmlSource(inputStream), AwardSet.class).getValue();

            Map<String, Award> tempAwardMap = new HashMap<>();
            String currentSetName = fileName.replaceFirst("[.][^.]+$", "");
            int i = 0;
            for (Award award : awardSet.getAwards()) {
                award.setId(i);
                i++;
                award.setSet(currentSetName);
                tempAwardMap.put(award.getName(), award);
            }
            awardsMap.put(currentSetName, tempAwardMap);
        } catch (JAXBException e) {
            MekHQ.getLogger().error("Error loading XML for awards", e);
        }
    }
}

