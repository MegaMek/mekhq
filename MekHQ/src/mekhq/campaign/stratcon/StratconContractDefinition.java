/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratcon;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.enums.AtBContractType;

/**
 * This class holds data relevant to the various types of contract
 * that can occur in the StratCon campaign system.
 * @author NickAragua
 */
@XmlRootElement(name = "StratconContractDefinition")
public class StratconContractDefinition {
    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioTemplate";

    private static ContractDefinitionManifest definitionManifest;
    private static Map<AtBContractType, StratconContractDefinition> loadedDefinitions = new HashMap<>();

    private static ContractDefinitionManifest getContractDefinitionManifest() {
        if (definitionManifest == null) {
            definitionManifest = ContractDefinitionManifest.Deserialize(MekHqConstants.STRATCON_CONTRACT_MANIFEST);

            // load user-specified modifier list
            ContractDefinitionManifest userDefinitionList = ContractDefinitionManifest.Deserialize(MekHqConstants.STRATCON_USER_CONTRACT_MANIFEST);
            if (userDefinitionList != null) {
                definitionManifest.definitionFileNames.putAll(userDefinitionList.definitionFileNames);
            }
        }

        return definitionManifest;
    }

    /**
     * Returns the StratCon contract definition for the given {@link AtBContractType}
     */
    public static StratconContractDefinition getContractDefinition(final AtBContractType atbContractType) {
        if (!loadedDefinitions.containsKey(atbContractType)) {
            String filePath = Paths.get(MekHqConstants.STRATCON_CONTRACT_PATH,
                    getContractDefinitionManifest().definitionFileNames.get(atbContractType)).toString();
            StratconContractDefinition def = Deserialize(new File(filePath));

            if (def == null) {
                MekHQ.getLogger().error(String.format("Unable to load contract definition %s", filePath));
                return null;
            }

            loadedDefinitions.put(atbContractType, def);
        }

        return loadedDefinitions.get(atbContractType);
    }

    /**
     * The kind of actions that the player needs to undertake to complete
     * strategic objectives for this contract.
     */
    public enum StrategicObjectiveType {
        /**
         * Victory in any scenario
         */
        AnyScenarioVictory,

        /**
         * Victory in scenarios - designated by the "objectiveScenarios" collection.
         * These are one-off scenarios that stick around on tracks - when revealed,
         * they get a deploy/action/return date as usual, and the player gets one shot to complete them
         */
        SpecificScenarioVictory,

        /**
         * Victory in scenarios designated as "required" (usually per contract command clause)
         */
        RequiredScenarioVictory,

        /**
         * Control of allied facilities generated at contract start time
         * Each track will be seeded with some number of allied facilities
         * They must not be destroyed and the player must have control of them at the end-of-contract date
         */
        AlliedFacilityControl,

        /**
         * Control of hostile facilities generated at contract start time
         * Each track will be seeded with some number of hostile facilities
         * They must not be destroyed and the player must have control of them at the end-of-contract date
         */
        HostileFacilityControl,

        /**
         * Destruction of hostile facilities generated at contract start time
         * Each track will be seeded with some number of hostile facilities
         * They may either be destroyed or the player must have control of them at the end-of-contract date
         */
        FacilityDestruction
    }

    private String contractTypeName;
    private String briefing;

    /**
     * How many allied facilities to generate for the contract,
     * in addition to any facilities placed by objectives.
     * < 0 indicates that the number of facilities should be scaled to the number of
     * lances required by the contract. 0 indicates no additional allied facilities.
     */
    private double alliedFacilityCount;

    /**
     * How many hostile facilities to generate for the contract,
     * in addition to any facilities placed by objectives.
     * -1 indicates that the number of facilities should be scaled to the number of
     * lances required by the contract. 0 indicates no additional hostile facilities.
     */
    private double hostileFacilityCount;

    private boolean allowEarlyVictory;
    
    /**
     * List of scenario IDs (file names) that are allowed for this contract type
     */
    private List<String> allowedScenarios;

    /**
     * List of scenario IDs (file names) that are not allowed for this contract type
     */
    private List<String> forbiddenScenarios;

    /**
     * Strategic objectives for this contract.
     */
    private List<ObjectiveParameters> objectiveParameters;

    /**
     * This is a list of scenario modifier names that apply to
     * *every* mission generated in this contract. Use very sparingly.
     */
    private List<String> globalScenarioModifiers = new ArrayList<>();

    private List<Integer> scenarioOdds;

    private List<Integer> deploymentTimes;

    /**
     * @return the contract type name
     */
    public String getContractTypeName() {
        return contractTypeName;
    }

    /**
     * @param contractTypeName the contract type name to set
     */
    public void setContractTypeName(final String contractTypeName) {
        this.contractTypeName = contractTypeName;
    }

    /**
     * @return the briefing
     */
    public String getBriefing() {
        return briefing;
    }

    /**
     * @param briefing the briefing to set
     */
    public void setBriefing(String briefing) {
        this.briefing = briefing;
    }

    public boolean isAllowEarlyVictory() {
        return allowEarlyVictory;
    }

    public void setAllowEarlyVictory(boolean allowEarlyVictory) {
        this.allowEarlyVictory = allowEarlyVictory;
    }

    /**
     * @return the alliedFacilityCount
     */
    public double getAlliedFacilityCount() {
        return alliedFacilityCount;
    }

    /**
     * @param alliedFacilityCount the alliedFacilityCount to set
     */
    public void setAlliedFacilityCount(double alliedFacilityCount) {
        this.alliedFacilityCount = alliedFacilityCount;
    }

    /**
     * @return the hostileFacilityCount
     */
    public double getHostileFacilityCount() {
        return hostileFacilityCount;
    }

    /**
     * @param hostileFacilityCount the hostileFacilityCount to set
     */
    public void setHostileFacilityCount(double hostileFacilityCount) {
        this.hostileFacilityCount = hostileFacilityCount;
    }

    @XmlElementWrapper(name = "allowedScenarios")
    @XmlElement(name = "allowedScenario")
    public List<String> getAllowedScenarios() {
        return allowedScenarios;
    }

    public void setAllowedScenarios(List<String> allowedScenarios) {
        this.allowedScenarios = allowedScenarios;
    }

    @XmlElementWrapper(name = "forbiddenScenarios")
    @XmlElement(name = "forbiddenScenario")
    public List<String> getForbiddenScenarios() {
        return forbiddenScenarios;
    }

    public void setForbiddenScenarios(List<String> forbiddenScenarios) {
        this.forbiddenScenarios = forbiddenScenarios;
    }

    @XmlElementWrapper(name = "objectiveParameters")
    @XmlElement(name = "objectiveParameter")
    public List<ObjectiveParameters> getObjectiveParameters() {
        return objectiveParameters;
    }

    public void setObjectiveParameters(List<ObjectiveParameters> objectiveParameters) {
        this.objectiveParameters = objectiveParameters;
    }

    @XmlElementWrapper(name = "scenarioOdds")
    @XmlElement(name = "scenarioOdds")
    public List<Integer> getScenarioOdds() {
        return scenarioOdds;
    }

    public void setScenarioOdds(List<Integer> scenarioOdds) {
        this.scenarioOdds = scenarioOdds;
    }

    @XmlElementWrapper(name = "deploymentTimes")
    @XmlElement(name = "deploymentTimes")
    public List<Integer> getDeploymentTimes() {
        return deploymentTimes;
    }

    public void setDeploymentTimes(List<Integer> deploymentTimes) {
        this.deploymentTimes = deploymentTimes;
    }

    public List<String> getGlobalScenarioModifiers() {
        return globalScenarioModifiers;
    }

    public void setGlobalScenarioModifiers(List<String> globalScenarioModifiers) {
        this.globalScenarioModifiers = globalScenarioModifiers;
    }

    /**
     * Data structure that deals with the characteristics that a StratCon scenario objective may have
     */
    public static class ObjectiveParameters {
        /**
         * The type of objective this is;
         */
        @XmlElement(name = "objectiveType")
        StrategicObjectiveType objectiveType;

        /**
         * How many strategic objectives will be placed for this contract.
         * 0 means none. A number less than zero indicates that the number of strategic objectives
         * should be scaled to the number of lances required by the contract, and multiplied by that factor.
         */
        @XmlElement(name = "objectiveCount")
        double objectiveCount;

        /**
         * List of IDs (file names) of specific scenarios to use for this objective.
         * Ignored for AnyScenarioVictory or AlliedFacilityControl objective types
         */
        @XmlElementWrapper(name = "objectiveScenarios")
        @XmlElement(name = "objectiveScenario")
        List<String> objectiveScenarios = new ArrayList<>();

        /**
         * If a particular scenario being generated is a strategic objective, it will have
         * these modifiers applied to it
         */
        @XmlElementWrapper(name = "objectiveScenarioModifiers")
        @XmlElement(name = "objectiveScenarioModifier")
        List<String> objectiveScenarioModifiers = new ArrayList<>();
    }

    /**
     * Serialize this instance of a scenario template to a File
     * Please pass in a non-null file.
     * @param outputFile The destination file.
     */
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(StratconContractDefinition.class);
            JAXBElement<StratconContractDefinition> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), StratconContractDefinition.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(templateElement, outputFile);
        } catch (Exception e) {
            MekHQ.getLogger().error("Error serializing " + outputFile.getPath(), e);
        }
    }

    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in file
     * @param inputFile The source file
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static StratconContractDefinition Deserialize(File inputFile) {
        StratconContractDefinition resultingDefinition = null;

        try {
            JAXBContext context = JAXBContext.newInstance(StratconContractDefinition.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<StratconContractDefinition> definitionElement = um.unmarshal(inputSource, StratconContractDefinition.class);
                resultingDefinition = definitionElement.getValue();
            }
        } catch (Exception e) {
            MekHQ.getLogger().error("Error deserializing contract definition " + inputFile.getPath(), e);
        }

        return resultingDefinition;
    }

    /**
     * A manifest containing IDs and file names of scenario template definitions
     * @author NickAragua
     */
    @XmlRootElement(name = "contractDefinitionManifest")
    private static class ContractDefinitionManifest {
        @XmlElementWrapper(name = "contractDefinitions")
        @XmlElement(name = "contractDefinition")
        public Map<AtBContractType, String> definitionFileNames;

        /**
         * Attempt to deserialize an instance of an contract definition manifest from the passed-in file path
         * @return Possibly an instance of a contract definition Manifest
         */
        public static ContractDefinitionManifest Deserialize(String fileName) {
            ContractDefinitionManifest resultingManifest = null;
            File inputFile = new File(fileName);
            if (!inputFile.exists()) {
                MekHQ.getLogger().warning(String.format("Specified file %s does not exist", fileName));
                return null;
            }

            try {
                JAXBContext context = JAXBContext.newInstance(ContractDefinitionManifest.class);
                Unmarshaller um = context.createUnmarshaller();
                try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                    Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                    JAXBElement<ContractDefinitionManifest> manifestElement = um.unmarshal(inputSource, ContractDefinitionManifest.class);
                    resultingManifest = manifestElement.getValue();
                }
            } catch (Exception e) {
                MekHQ.getLogger().error("Error deserializing contract definition manifest", e);
            }

            return resultingManifest;
        }
    }
}

