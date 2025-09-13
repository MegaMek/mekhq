/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratCon;

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
 * A manifest containing IDs and file names of StratCon facility definitions
 *
 * @author NickAragua
 */
@XmlRootElement(name = "facilityManifest")
public class StratConFacilityManifest {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacilityManifest.class);

    @XmlElementWrapper(name = "facilityFileNames")
    @XmlElement(name = "facilityFileName")
    public List<String> facilityFileNames;

    /**
     * Attempt to deserialize an instance of a StratConFacilityManifest from the passed-in file path
     *
     * @return Possibly an instance of a StratConFacilityManifest
     */
    public static StratConFacilityManifest deserialize(String fileName) {
        StratConFacilityManifest resultingManifest = null;
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", fileName);
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratConFacilityManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratConFacilityManifest> manifestElement = um.unmarshal(inputSource,
                      StratConFacilityManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            LOGGER.error("Error Deserializing Facility Manifest", e);
        }

        return resultingManifest;
    }
}
