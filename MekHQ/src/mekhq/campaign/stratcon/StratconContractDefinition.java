package mekhq.campaign.stratcon;

import java.io.File;
import java.io.FileInputStream;
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
import mekhq.MekHqXmlUtil;

/**
 * This class holds data relevant to the various types of contract
 * that can occur in the StratCon campaign system.
 * @author NickAragua
 *
 */
@XmlRootElement(name="StratconContractDefinition")
public class StratconContractDefinition {
    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioTemplate";
    public static final int COUNT_SCALED = -1;
    
    private static ContractDefinitionManifest definitionManifest;
    private static Map<Integer, StratconContractDefinition> loadedDefinitions = new HashMap<>();
    
    static {
        definitionManifest = ContractDefinitionManifest.Deserialize("./data/stratconcontractdefinitions/ContractDefinitionManifest.xml");
        
        // load user-specified modifier list
        ContractDefinitionManifest userDefinitionList = ContractDefinitionManifest.Deserialize("./data/scenariomodifiers/UserContractDefinitionManifest.xml");
        if(userDefinitionList != null) {
            definitionManifest.definitionFileNames.putAll(userDefinitionList.definitionFileNames);
        }
    }
    
    /**
     * Returns the stratcon contract definition for the given AtB Contract Type 
     * as defined in AtBContract.java
     */
    public static StratconContractDefinition getContractDefinition(int atbContractType) {
        definitionManifest = ContractDefinitionManifest.Deserialize("./data/stratconcontractdefinitions/ContractDefinitionManifest.xml");
        loadedDefinitions.clear();
        
        if (!loadedDefinitions.containsKey(atbContractType)) {
            String filePath = String.format("./data/stratconcontractdefinitions/%s", 
                    definitionManifest.definitionFileNames.get(atbContractType));
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
     *
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
    
    public static StratconContractDefinition createTestContract() {
        /*StratconContractDefinition retVal = new StratconContractDefinition();
        
        retVal.contractTypeName = "Test Contract Type";
        retVal.briefing = "Test Contract Briefing.";
        retVal.alliedFacilityCount = 1;
        retVal.hostileFacilityCount = COUNT_SCALED;
        retVal.objectiveParameters = new ArrayList<>();
        
        ObjectiveParameters objective = new ObjectiveParameters();
        objective.objectiveType = StrategicObjectiveType.SpecificScenarioVictory;
        objective.objectiveCount = COUNT_SCALED;
        objective.objectiveScenarios = Arrays.asList("Capture.xml", "Assassinate.xml");
        objective.objectiveScenarioModifiers = Arrays.asList("AlliedTankGarrison.xml", "HostileAirGarrison.xml");
        
        retVal.objectiveParameters.add(objective);
        
        retVal.allowedScenarios = Arrays.asList("TestAllowScenario.xml", "TestAllowScenario.xml");
        retVal.forbiddenScenarios = Arrays.asList("TestForbidScenario.xml", "TestForbidScenario.xml");
        
        retVal.Serialize(new File("d:\\projects\\mekhq\\mekhq\\data\\stratconcontractdefinitions\\testcontract.xml"));
        
        return retVal;*/
        StratconContractDefinition retVal = Deserialize(new File("d:\\projects\\mekhq\\mekhq\\data\\stratconcontractdefinitions\\ObjectiveRaid.xml"));
        return retVal;
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
     * If true, strategic objective scenarios contribute to the VP count
     */
    private boolean objectivesBehaveAsVPs;
    
    private List<Integer> scenarioOdds;
    
    private List<Integer> deploymentTimes;

    public String getContractTypeName() {
        return contractTypeName;
    }

    public void setContractTypeName(String contractTypeName) {
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

    @XmlElementWrapper(name="allowedScenarios")
    @XmlElement(name="allowedScenario")
    public List<String> getAllowedScenarios() {
        return allowedScenarios;
    }

    public void setAllowedScenarios(List<String> allowedScenarios) {
        this.allowedScenarios = allowedScenarios;
    }

    @XmlElementWrapper(name="forbiddenScenarios")
    @XmlElement(name="forbiddenScenario")
    public List<String> getForbiddenScenarios() {
        return forbiddenScenarios;
    }

    public void setForbiddenScenarios(List<String> forbiddenScenarios) {
        this.forbiddenScenarios = forbiddenScenarios;
    }
    
    @XmlElementWrapper(name="objectiveParameters")
    @XmlElement(name="objectiveParameter")
    public List<ObjectiveParameters> getObjectiveParameters() {
        return objectiveParameters;
    }

    public void setObjectiveParameters(List<ObjectiveParameters> objectiveParameters) {
        this.objectiveParameters = objectiveParameters;
    }

    public boolean objectivesBehaveAsVPs() {
        return objectivesBehaveAsVPs;
    }

    public void setObjectivesBehaveAsVPs(boolean objectivesBehaveAsVPs) {
        this.objectivesBehaveAsVPs = objectivesBehaveAsVPs;
    }

    @XmlElementWrapper(name="scenarioOdds")
    @XmlElement(name="scenarioOdds")
    public List<Integer> getScenarioOdds() {
        return scenarioOdds;
    }

    public void setScenarioOdds(List<Integer> scenarioOdds) {
        this.scenarioOdds = scenarioOdds;
    }

    @XmlElementWrapper(name="deploymentTimes")
    @XmlElement(name="deploymentTimes")
    public List<Integer> getDeploymentTimes() {
        return deploymentTimes;
    }

    public void setDeploymentTimes(List<Integer> deploymentTimes) {
        this.deploymentTimes = deploymentTimes;
    }

    public static class ObjectiveParameters {
        /**
         * The type of objective this is; 
         */
        @XmlElement(name="objectiveType")
        StrategicObjectiveType objectiveType;
        
        /**
         * How many strategic objectives will be placed for this contract.
         * 0 means none. A number less than zero indicates that the number of strategic objectives 
         * should be scaled to the number of lances required by the contract, and multiplied by that factor. 
         */
        @XmlElement(name="objectiveCount")
        double objectiveCount;
        
        /**
         * List of IDs (file names) of specific scenarios to use for this objective.
         * Ignored for AnyScenarioVictory or AlliedFacilityControl objective types
         */
        @XmlElementWrapper(name="objectiveScenarios")
        @XmlElement(name="objectiveScenario")
        List<String> objectiveScenarios;
        
        /**
         * If a particular scenario being generated is a strategic objective, it will have
         * these modifiers applied to it
         */
        @XmlElementWrapper(name="objectiveScenarioModifiers")
        @XmlElement(name="objectiveScenarioModifier")
        List<String> objectiveScenarioModifiers;
    }
    
    // Garrison Duty: Defend X facilities
    // Cadre Duty: Victory in X Training Exercise scenarios
    // Security Duty: Defend X facilities
    // Riot Duty: Defend X facilities, Riot Suppression scenarios
    // Planetary Assault: Destroy or Capture and Hold X facilities
    // Relief Duty: Hold X facilities (on hostile world!)
    // Guerilla Warfare: Victory in X scenarios, soft time limit (reinforcements) on all scenarios
    // Pirate Hunting: Victory in X scenarios 
    // Diversionary Warfare: Victory in X scenarios
    // Objective Raid: Destroy X facilities
    // Extraction Raid: Extract from X facilities, victory in X capture scenarios
    // Recon Raid: Recon on X facilities
    // 
    // future expansion:
    // Assassination: Victory in X Assassination scenarios
    // Terrorism: Victory in X "Riot Suppression" scenarios
    // Bounty Hunt: Victory in X Assassination, X Capture scenarios
    // Mole Hunting: ?
    // Espionage: ?
    // Sabotage: ?
    // Observation Raid: ?
    
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
        } catch(Exception e) {
            MekHQ.getLogger().error("Erorr deserializing " + outputFile.getPath(), e);
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
        } catch(Exception e) {
            MekHQ.getLogger().error("Error Deserializing Contract Definition " + inputFile.getPath(), e);
        }

        return resultingDefinition;
    }
}

/**
 * A manifest containing IDs and file names of scenario template definitions
 * @author NickAragua
 *
 */
@XmlRootElement(name="contractDefinitionManifest")
class ContractDefinitionManifest {
    @XmlElementWrapper(name="contractDefinitions")
    @XmlElement(name="contractDefinition")
    public Map<Integer, String> definitionFileNames;
    
    /**
     * Attempt to deserialize an instance of an contract definition manifest from the passed-in file 
     * @param inputFile The path to the manifest
     * @return Possibly an instance of a contract definition Manifest
     */
    public static ContractDefinitionManifest Deserialize(String fileName) {
        ContractDefinitionManifest resultingManifest = null;
        File inputFile = new File(fileName);
        if(!inputFile.exists()) {
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
        } catch(Exception e) {
            MekHQ.getLogger().error("Error Deserializing Contract Definition Manifest", e);
        }

        return resultingManifest;
    }
}