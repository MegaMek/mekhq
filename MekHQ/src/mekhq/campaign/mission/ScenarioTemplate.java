/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is the root data structure for organizing information related to a
 * scenario template.
 *
 * @author NickAragua
 */
@XmlRootElement(name = "ScenarioTemplate")
public class ScenarioTemplate implements Cloneable {
    private static final MMLogger logger = MMLogger.create(ScenarioTemplate.class);

    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioTemplate";
    public static final String PRIMARY_PLAYER_FORCE_ID = "Player";

    public String name;
    public ScenarioType stratConScenarioType;
    public String shortBriefing;
    public String detailedBriefing;

    public boolean isHostileFacility;
    public boolean isAlliedFacility;

    public ScenarioMapParameters mapParameters = new ScenarioMapParameters();
    public List<String> scenarioModifiers = new ArrayList<>();

    private Map<String, ScenarioForceTemplate> scenarioForces = new HashMap<>();

    @XmlElementWrapper(name = "scenarioObjectives")
    @XmlElement(name = "scenarioObjective")
    public List<ScenarioObjective> scenarioObjectives = new ArrayList<>();

    @Override
    public ScenarioTemplate clone() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.name = this.name;
        template.stratConScenarioType = this.stratConScenarioType;
        template.shortBriefing = this.shortBriefing;
        template.detailedBriefing = this.detailedBriefing;
        template.isHostileFacility = this.isHostileFacility;
        template.isAlliedFacility = this.isAlliedFacility;
        for (ScenarioForceTemplate sft : scenarioForces.values()) {
            template.scenarioForces.put(sft.getForceName(), sft.clone());
        }

        for (String mod : scenarioModifiers) {
            template.scenarioModifiers.add(mod);
        }

        for (ScenarioObjective obj : scenarioObjectives) {
            template.scenarioObjectives.add(new ScenarioObjective(obj));
        }

        template.mapParameters = mapParameters.clone();

        return template;
    }

    /**
     * Returns the "primary" player force. This is always the force with the name
     * "Player".
     *
     * @return Primary player force.
     */
    public ScenarioForceTemplate getPrimaryPlayerForce() {
        return scenarioForces.get(PRIMARY_PLAYER_FORCE_ID);
    }

    public List<ScenarioForceTemplate> getAllScenarioForces() {
        return scenarioForces.values().stream().collect(Collectors.toList());
    }

    @XmlElementWrapper(name = "scenarioForces")
    @XmlElement(name = "scenarioForce")
    public Map<String, ScenarioForceTemplate> getScenarioForces() {
        return scenarioForces;
    }

    public void setScenarioForces(Map<String, ScenarioForceTemplate> forces) {
        scenarioForces = forces;
    }

    public boolean isHostileFacility() {
        return isHostileFacility;
    }

    public boolean isAlliedFacility() {
        return isAlliedFacility;
    }

    public boolean isFacilityScenario() {
        return isHostileFacility || isAlliedFacility;
    }

    public List<ScenarioForceTemplate> getAllBotControlledAllies() {
        return scenarioForces.values().stream()
                .filter(forceTemplate -> (forceTemplate.getForceAlignment() == ForceAlignment.Allied.ordinal()) &&
                        (forceTemplate.getGenerationMethod() != ForceGenerationMethod.PlayerSupplied.ordinal()))
                .collect(Collectors.toList());
    }

    public List<ScenarioForceTemplate> getAllBotControlledHostiles() {
        return scenarioForces.values().stream()
                .filter(forceTemplate -> (forceTemplate.getForceAlignment() == ForceAlignment.Opposing.ordinal()) ||
                        (forceTemplate.getForceAlignment() == ForceAlignment.Third.ordinal()))
                .collect(Collectors.toList());
    }

    /**
     * All force templates that are controlled and supplied, or potentially
     * supplied, by the player, that are not reinforcements
     *
     * @return List of scenario force templates
     */
    public List<ScenarioForceTemplate> getAllPrimaryPlayerForces() {
        return scenarioForces.values().stream()
                .filter(forceTemplate -> (forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) &&
                        (forceTemplate.getArrivalTurn() != ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) &&
                        ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) ||
                                (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount
                                        .ordinal())))
                .collect(Collectors.toList());
    }

    /**
     * All force templates that are controlled and supplied, or potentially
     * supplied, by the player, that are not reinforcements
     *
     * @return List of scenario force templates
     */
    public List<ScenarioForceTemplate> getAllPlayerReinforcementForces() {
        List<ScenarioForceTemplate> retVal = new ArrayList<>();

        for (ScenarioForceTemplate forceTemplate : scenarioForces.values()) {
            if ((forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) &&
                    (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) &&
                    ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) ||
                            (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount
                                    .ordinal()))) {
                retVal.add(forceTemplate);
            }
        }

        return retVal;
    }

    /**
     * Is this template for a ground-side scenario?
     */
    public boolean isPlanetSurface() {
        return mapParameters.getMapLocation() == MapLocation.AllGroundTerrain ||
                mapParameters.getMapLocation() == MapLocation.SpecificGroundTerrain;
    }

    /**
     * Serialize this instance of a scenario template to a File
     * Please pass in a non-null file.
     *
     * @param outputFile The destination file.
     */
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            JAXBElement<ScenarioTemplate> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME),
                    ScenarioTemplate.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(templateElement, outputFile);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * Serialize this instance of a scenario template to a PrintWriter
     * Omits initial xml declaration
     *
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            JAXBElement<ScenarioTemplate> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME),
                    ScenarioTemplate.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(templateElement, pw);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * Attempt to deserialize a file at the given path.
     *
     * @param filePath The location of the file
     * @return Possibly an instance of a scenario template.
     */
    public static ScenarioTemplate Deserialize(String filePath) {
        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            logger.error(String.format("Cannot deserialize file %s, does not exist", filePath));
            return null;
        }

        return Deserialize(inputFile);
    }

    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in
     * file
     *
     * @param inputFile The source file
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioTemplate Deserialize(File inputFile) {
        ScenarioTemplate resultingTemplate = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<ScenarioTemplate> templateElement = um.unmarshal(inputSource, ScenarioTemplate.class);
                resultingTemplate = templateElement.getValue();
            }
        } catch (Exception e) {
            logger.error("Error Deserializing Scenario Template", e);
        }

        return resultingTemplate;
    }

    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in
     * XML Node
     *
     * @param xmlNode The node with the scenario template
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioTemplate Deserialize(Node xmlNode) {
        ScenarioTemplate resultingTemplate = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioTemplate> templateElement = um.unmarshal(xmlNode, ScenarioTemplate.class);
            resultingTemplate = templateElement.getValue();
        } catch (Exception e) {
            logger.error("Error Deserializing Scenario Template", e);
        }

        return resultingTemplate;
    }
}
