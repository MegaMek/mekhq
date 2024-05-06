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
package mekhq.campaign.personnel.education;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.common.preference.PreferenceManager;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The AcademyFactory class is responsible for generating academy blueprints by reading the data from XML sources.
 * It provides methods to retrieve a list of set names and a list of academies for a given set name.
 */
public class AcademyFactory {
    private static AcademyFactory instance = null;
    private final Map<String, Map<String, Academy>> academyMap;

    /**
     * This class is responsible for generating academy blueprint by reading the data from XML sources.
     */
    private AcademyFactory() {
        academyMap = new HashMap<>();
        loadAcademies(MHQConstants.ACADEMY_DIRECTORY_PATH);
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        loadAcademies(new File(userDir, MHQConstants.ACADEMY_DIRECTORY_PATH).toString());
    }

    /**
     * Returns an instance of the AcademyFactory.
     * If an instance already exists, it returns the existing instance.
     * If no instance exists, it creates a new instance and returns it.
     *
     * @return the AcademyFactory instance
     */
    public static AcademyFactory getInstance() {
        if (instance == null) {
            instance = new AcademyFactory();
        }

        return instance;
    }

    /**
     * Retrieves a list of all set names in the AcademyFactory.
     *
     * @return a list of set names
     */
    public List<String> getAllSetNames() {
        return new ArrayList<>(academyMap.keySet());
    }

    /**
     * Retrieves a list of all academies for a given set name.
     *
     * @param setName the name of the set
     * @return a list of academies for the given set
     */
    public List<Academy> getAllAcademiesForSet(String setName) {
        return new ArrayList<>(academyMap.get(setName).values());
    }

    /**
     * Generates the "blueprint" academy by reading the data from XML sources.
     */
    private void loadAcademies(String path) {
        for (String file : CommonSettingsDialog.filteredFilesWithSubDirs(new File(path), ".xml")) {
            try (InputStream inputStream = new FileInputStream(file)) {
                loadAcademyFromStream(inputStream, new File(file).getName());
            } catch (IOException e) {
                LogManager.getLogger().error("", e);
            }
        }
    }

    /**
     * Loads academy data from an input stream and adds it to the academy map.
     *
     * @param inputStream the input stream containing the academy data
     * @param fileName the name of the file containing the academy data
     */
    public void loadAcademyFromStream(InputStream inputStream, String fileName) {
        AcademySet academySet;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AcademySet.class, Academy.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            academySet = unmarshaller.unmarshal(MHQXMLUtility.createSafeXmlSource(inputStream), AcademySet.class).getValue();

            Map<String, Academy> tempAcademyMap = new HashMap<>();
            String currentSetName = fileName.replaceFirst("[.][^.]+$", "");
            int id = 0;
            for (Academy academy : academySet.getAcademies()) {
                academy.setId(id);
                id++;
                academy.setSet(currentSetName);
                tempAcademyMap.put(academy.getName(), academy);
            }
            academyMap.put(currentSetName, tempAcademyMap);
        } catch (JAXBException e) {
            LogManager.getLogger().error("Error loading XML for academies", e);
        }
    }
}
