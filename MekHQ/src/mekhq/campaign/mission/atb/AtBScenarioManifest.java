/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.mission.atb;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

/**
 * A manifest containing IDs and file names of scenario template definitions
 * @author NickAragua
 */
@XmlRootElement(name = "scenarioManifest")
public class AtBScenarioManifest {
    @XmlElementWrapper(name = "scenarioFileNames")
    @XmlElement(name = "scenarioFileName")
    public Map<Integer, String> scenarioFileNames;
    
    /**
     * Attempt to deserialize an instance of an AtBScenarioManifest from the passed-in file path
     * @return Possibly an instance of a ScenarioManifest
     */
    public static AtBScenarioManifest Deserialize(String fileName) {
        AtBScenarioManifest resultingManifest = null;
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            MekHQ.getLogger().warning(String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<AtBScenarioManifest> manifestElement = um.unmarshal(inputSource, AtBScenarioManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            MekHQ.getLogger().error("Error Deserializing Scenario Manifest", e);
        }

        return resultingManifest;
    }
}
