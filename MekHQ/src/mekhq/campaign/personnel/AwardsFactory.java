/*
 * Copyright (C) 2018 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import mekhq.campaign.log.LogEntry;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.AwardSet;

/**
 * This class is responsible to control the awards. It loads one instance of each awards, then it creates a copy of it
 * once it needs to be awarded to someone.
 * @author Miguel Azevedo
 *
 */
public class AwardsFactory {
    private static final String AWARDS_XML_ROOT_PATH = "data/universe/awards/";

    private static AwardsFactory instance = null;

    /**
     * Here is where the blueprints are stored, mapped by set and name.
     */
    private Map<String, Map<String,Award>> awardsMap;

    private AwardsFactory(){
        awardsMap = new HashMap<>();

        loadAwards();
    }

    public static AwardsFactory getInstance(){
        if(instance == null){
            instance = new AwardsFactory();
        }

        return instance;
    }

    /**
     * @return the names of the all the award sets loaded.
     */
    public List<String> getAllSetNames(){
        return new ArrayList<>(awardsMap.keySet());
    }

    /**
     * Gets a list of all awards that belong to a given Set
     * @param setName is the name of the set
     * @return list with the awards belonging to that set
     */
    public List<Award> getAllAwardsForSet(String setName){
        return new ArrayList<>(awardsMap.get(setName).values());
    }

    /**
     * By searching the "blueprints" (i.e. awards instances that serve as data model), it generates a copy of that
     * award in order for it to be given to someone.
     * @param setName the name of the set
     * @param awardName the name of the award
     * @return list with the awards belonging to that set
     */
    public Award generateNew(String setName, String awardName){
        Map<String, Award> awardSet = awardsMap.get(setName);
        Award blueprintAward = awardSet.get(awardName);
        return blueprintAward.createCopy();
    }

    /**
     * Generates a new award from an XML entry (when loading game, for example)
     * @param node xml node
     * @return an award
     */
    public Award generateNewFromXML(Node node){
        final String METHOD_NAME = "generateNewFromXML(Node)"; //$NON-NLS-1$
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        String name = null;
        String set = null;
        ArrayList<Date> dates = new ArrayList<>();

        try {
            NodeList nl = node.getChildNodes();

            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    dates.add(DATE_FORMAT.parse(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("set")){
                    set = wn2.getTextContent();
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().error(LogEntry.class, METHOD_NAME, ex);
        }

        Award award = generateNew(set, name);
        award.setDates(dates);
        return award;
    }

    /**
     * Generates the "blueprint" awards by reading the data from XML sources.
     */
    private void loadAwards() {
        File dir = new File(AWARDS_XML_ROOT_PATH);
        File[] files =  dir.listFiles((dir1, filename) -> filename.endsWith(".xml"));

        if(files == null) {
            return;
        }
        
        for(File file : files) {
            try{
                InputStream inputStream = new FileInputStream(file);
                loadAwardsFromStream(inputStream, file.getName());
            }
            catch (FileNotFoundException e){
                MekHQ.getLogger().error(AwardsFactory.class, "loadAwards", e);
            }
        }
    }

    public void loadAwardsFromStream(InputStream inputStream, String fileName){
        AwardSet awardSet;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AwardSet.class, Award.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            awardSet = unmarshaller.unmarshal(MekHqXmlUtil.createSafeXmlSource(inputStream), AwardSet.class).getValue();

            Map<String, Award> tempAwardMap = new HashMap<>();
            String currentSetName = fileName.replaceFirst("[.][^.]+$", "");
            int i = 0;
            for (Award award : awardSet.getAwards()){
                award.setId(i);
                i++;
                award.setSet(currentSetName);
                tempAwardMap.put(award.getName(), award);
            }
            awardsMap.put(currentSetName, tempAwardMap);
        } catch (JAXBException e) {
            MekHQ.getLogger().error(AwardsFactory.class, "loadAwards", "Error loading XML for awards", e);
        }
    }
}

