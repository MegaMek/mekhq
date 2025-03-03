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
package mekhq.campaign.stratcon;

import java.io.File;
import java.io.FileInputStream;
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
 * A manifest containing IDs and file names of stratcon facility definitions
 * 
 * @author NickAragua
 */
@XmlRootElement(name = "facilityManifest")
public class StratconFacilityManifest {
    private static final MMLogger logger = MMLogger.create(StratconFacilityManifest.class);

    @XmlElementWrapper(name = "facilityFileNames")
    @XmlElement(name = "facilityFileName")
    public List<String> facilityFileNames;

    /**
     * Attempt to deserialize an instance of a StratconFacilityManifest from the
     * passed-in file path
     * 
     * @return Possibly an instance of a StratconFacilityManifest
     */
    public static StratconFacilityManifest deserialize(String fileName) {
        StratconFacilityManifest resultingManifest = null;
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            logger.warn(String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconFacilityManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratconFacilityManifest> manifestElement = um.unmarshal(inputSource,
                        StratconFacilityManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            logger.error("Error Deserializing Facility Manifest", e);
        }

        return resultingManifest;
    }
}
