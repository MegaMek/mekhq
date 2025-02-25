/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.atb;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;

/**
 * Class intended for local use that holds a manifest of scenario modifier
 * definition file names.
 *
 * @author NickAragua
 */
@XmlRootElement(name = "scenarioModifierManifest")
class ScenarioModifierManifest {
    private static final MMLogger logger = MMLogger.create(ScenarioModifierManifest.class);

    @XmlElementWrapper(name = "modifiers")
    @XmlElement(name = "modifier")
    public List<String> fileNameList = new ArrayList<>();

    /**
     * Attempt to deserialize an instance of a scenario modifier list from the
     * passed-in file
     *
     * @param fileName Name of the file that contains the scenario modifier list
     * @return Possibly an instance of a scenario modifier list
     */
    public static ScenarioModifierManifest Deserialize(String fileName) {
        ScenarioModifierManifest resultingList = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioModifierManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if (!xmlFile.exists()) {
                logger.warn(String.format("Specified file %s does not exist", fileName));
                return null;
            }

            try (FileInputStream fileStream = new FileInputStream(xmlFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<ScenarioModifierManifest> templateElement = um.unmarshal(inputSource,
                        ScenarioModifierManifest.class);
                resultingList = templateElement.getValue();
            }
        } catch (Exception ex) {
            logger.error("Error Deserializing Scenario Modifier List", ex);
        }

        return resultingList;
    }
}
