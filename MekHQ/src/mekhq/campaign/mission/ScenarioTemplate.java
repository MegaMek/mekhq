/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

/**
 * This is the root data structure for organizing information related to a scenario template.
 *
 * @author NickAragua
 */
@XmlRootElement(name = "ScenarioTemplate")
public class ScenarioTemplate implements Cloneable {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioTemplate.class);

    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioTemplate";
    public static final String PRIMARY_PLAYER_FORCE_ID = "Player";

    public String name;
    @XmlElement(name = "stratConScenarioType")
    @XmlJavaTypeAdapter(value = ScenarioTypeAdapter.class)
    private ScenarioType stratConScenarioType = ScenarioType.NONE;
    public String shortBriefing;
    public String detailedBriefing;

    public boolean isHostileFacility;
    public boolean isAlliedFacility;

    @XmlElement(name = "battlefieldControl")
    @XmlJavaTypeAdapter(value = BattlefieldControlTypeAdapter.class)
    public BattlefieldControlType battlefieldControl = BattlefieldControlType.VICTOR;

    public ScenarioMapParameters mapParameters = new ScenarioMapParameters();
    public List<String> scenarioModifiers = new ArrayList<>();

    private Map<String, ScenarioForceTemplate> scenarioForces = new HashMap<>();

    @XmlElementWrapper(name = "scenarioObjectives")
    @XmlElement(name = "scenarioObjective")
    public List<ScenarioObjective> scenarioObjectives = new ArrayList<>();

    /**
     * Enum representing the different types of battlefield control during a scenario.
     */
    public enum BattlefieldControlType {
        /**
         * Indicates the victor controls the field.
         */
        VICTOR,

        /**
         * Battlefield control is always assigned to the player.
         */
        PLAYER,

        /**
         * Battlefield control is always assigned to the enemy.
         */
        ENEMY
    }

    @Override
    public ScenarioTemplate clone() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.name = this.name;
        template.stratConScenarioType = this.stratConScenarioType;
        template.shortBriefing = this.shortBriefing;
        template.detailedBriefing = this.detailedBriefing;
        template.isHostileFacility = this.isHostileFacility;
        template.isAlliedFacility = this.isAlliedFacility;
        template.battlefieldControl = this.battlefieldControl;
        for (ScenarioForceTemplate sft : scenarioForces.values()) {
            template.scenarioForces.put(sft.getForceName(), sft.clone());
        }

        template.scenarioModifiers.addAll(scenarioModifiers);

        for (ScenarioObjective obj : scenarioObjectives) {
            template.scenarioObjectives.add(new ScenarioObjective(obj));
        }

        template.mapParameters = mapParameters.clone();

        return template;
    }

    public ScenarioType getStratConScenarioType() {
        return (this.stratConScenarioType != null) ? this.stratConScenarioType : ScenarioType.NONE;
    }

    public void setStratConScenarioType(String scenarioType) {
        try {
            this.stratConScenarioType = ScenarioType.valueOf(scenarioType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.error(e, "Invalid ScenarioType: {}", scenarioType);
            this.stratConScenarioType = ScenarioType.NONE;
        }
    }

    /**
     * Returns the "primary" player force. This is always the force with the name "Player".
     *
     * @return Primary player force.
     */
    public ScenarioForceTemplate getPrimaryPlayerForce() {
        return scenarioForces.get(PRIMARY_PLAYER_FORCE_ID);
    }

    public List<ScenarioForceTemplate> getAllScenarioForces() {
        return new ArrayList<>(scenarioForces.values());
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

    public BattlefieldControlType getBattlefieldControl() {
        return battlefieldControl;
    }

    public List<ScenarioForceTemplate> getAllBotControlledAllies() {
        return scenarioForces.values().stream()
                     .filter(forceTemplate -> (forceTemplate.getForceAlignment() == ForceAlignment.Allied.ordinal()) &&
                                                    (forceTemplate.getGenerationMethod() !=
                                                           ForceGenerationMethod.PlayerSupplied.ordinal()))
                     .collect(Collectors.toList());
    }

    public List<ScenarioForceTemplate> getAllBotControlledHostiles() {
        return scenarioForces.values().stream()
                     .filter(forceTemplate -> (forceTemplate.getForceAlignment() ==
                                                     ForceAlignment.Opposing.ordinal()) ||
                                                    (forceTemplate.getForceAlignment() ==
                                                           ForceAlignment.Third.ordinal()))
                     .collect(Collectors.toList());
    }

    /**
     * All force templates that are controlled and supplied, or potentially supplied, by the player, that are not
     * reinforcements
     *
     * @return List of scenario force templates
     */
    public List<ScenarioForceTemplate> getAllPrimaryPlayerForces() {
        return scenarioForces.values().stream()
                     .filter(forceTemplate -> (forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) &&
                                                    (forceTemplate.getArrivalTurn() !=
                                                           ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) &&
                                                    ((forceTemplate.getGenerationMethod() ==
                                                            ForceGenerationMethod.PlayerSupplied.ordinal()) ||
                                                           (forceTemplate.getGenerationMethod() ==
                                                                  ForceGenerationMethod.PlayerOrFixedUnitCount
                                                                        .ordinal())))
                     .collect(Collectors.toList());
    }

    /**
     * All force templates that are controlled and supplied, or potentially supplied, by the player, that are not
     * reinforcements
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
     * Serialize this instance of a scenario template to a File Please pass in a non-null file.
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
            LOGGER.error("", e);
        }
    }

    /**
     * Serialize this instance of a scenario template to a PrintWriter Omits initial xml declaration
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
            LOGGER.error("", e);
        }
    }

    /**
     * Attempt to deserialize a file at the given path.
     *
     * @param filePath The location of the file
     *
     * @return Possibly an instance of a scenario template.
     */
    public static ScenarioTemplate Deserialize(String filePath) {
        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            LOGGER.error("Cannot deserialize file {}, does not exist", filePath);
            return null;
        }

        return Deserialize(inputFile);
    }

    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in file
     *
     * @param inputFile The source file
     *
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
            LOGGER.error("Error Deserializing Scenario Template", e);
        }

        return resultingTemplate;
    }

    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in XML Node
     *
     * @param xmlNode The node with the scenario template
     *
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioTemplate Deserialize(Node xmlNode) {
        ScenarioTemplate resultingTemplate = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<ScenarioTemplate> templateElement = unmarshaller.unmarshal(xmlNode, ScenarioTemplate.class);
            resultingTemplate = templateElement.getValue();
        } catch (Exception e) {
            LOGGER.error("Error Deserializing Scenario Template", e);
        }

        return resultingTemplate;
    }

    public static class ScenarioTypeAdapter extends XmlAdapter<String, ScenarioType> {
        @Override
        public ScenarioType unmarshal(String value) {
            try {
                return ScenarioType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                MMLogger.create(ScenarioTypeAdapter.class).error("Error Invalid ScenarioType in XML: {}", value);
                return ScenarioType.NONE; // Default for invalid values
            }
        }

        @Override
        public String marshal(ScenarioType scenarioType) {
            // Converts Enum back to String for XML
            return String.valueOf(scenarioType);
        }
    }

    /**
     * Adapter for converting between String and BattlefieldControlType during XML (un)marshalling.
     */
    public static class BattlefieldControlTypeAdapter extends XmlAdapter<String, BattlefieldControlType> {

        @Override
        public BattlefieldControlType unmarshal(String value) throws Exception {
            try {
                // Convert the string value to a BattlefieldControlType enum
                return BattlefieldControlType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // If the string does not match any enum, handle it gracefully (e.g., return a default value)
                return BattlefieldControlType.VICTOR;
            }
        }

        @Override
        public String marshal(BattlefieldControlType value) throws Exception {
            // Convert the BattlefieldControlType enum back to its string representation
            return value.name();
        }
    }
}
