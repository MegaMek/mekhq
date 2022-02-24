/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.atb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import mekhq.MekHqXmlUtil;
import org.apache.logging.log4j.LogManager;

import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

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
            LogManager.getLogger().warn(String.format("Specified file %s does not exist", fileName));
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
            LogManager.getLogger().error("Error Deserializing Scenario Manifest", e);
        }

        return resultingManifest;
    }
}
