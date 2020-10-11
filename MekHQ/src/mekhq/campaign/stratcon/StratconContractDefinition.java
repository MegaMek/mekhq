package mekhq.campaign.stratcon;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import mekhq.MekHQ;

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
        StratconContractDefinition retVal = new StratconContractDefinition();
        
        retVal.contractTypeName = "Test Contract Type";
        retVal.briefing = "Test Contract Briefing.";
        retVal.alliedFacilityCount = 1;
        retVal.hostileFacilityCount = COUNT_SCALED;
        retVal.objectiveParameters = new ArrayList<>();
        
        ObjectiveParameters objective = new ObjectiveParameters();
        objective.objectiveType = StrategicObjectiveType.SpecificScenarioVictory;
        objective.objectiveCount = COUNT_SCALED;
        objective.objectiveScenarios = Arrays.asList("TestScenario.xml", "TestScenario.xml");
        objective.objectiveScenarioModifiers = Arrays.asList("TestFacMod1.xml", "TestFacMod2.xml");
        
        retVal.objectiveParameters.add(objective);
        
        retVal.allowedScenarios = Arrays.asList("TestAllowScenario.xml", "TestAllowScenario.xml");
        retVal.forbiddenScenarios = Arrays.asList("TestForbidScenario.xml", "TestForbidScenario.xml");
        
        
        return retVal;
    }
    
    private String contractTypeName;
    private String briefing;
    
    /**
     * How many allied facilities to generate for the contract, 
     * in addition to any facilities placed by objectives.
     * -1 indicates that the number of facilities should be scaled to the number of 
     * lances required by the contract. 0 indicates no additional allied facilities.
     */
    private int alliedFacilityCount;
    
    /**
     * How many hostile facilities to generate for the contract, 
     * in addition to any facilities placed by objectives.
     * -1 indicates that the number of facilities should be scaled to the number of 
     * lances required by the contract. 0 indicates no additional hostile facilities.
     */
    private int hostileFacilityCount;
    
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
    public int getAlliedFacilityCount() {
        return alliedFacilityCount;
    }

    /**
     * @param alliedFacilityCount the alliedFacilityCount to set
     */
    public void setAlliedFacilityCount(int alliedFacilityCount) {
        this.alliedFacilityCount = alliedFacilityCount;
    }

    /**
     * @return the hostileFacilityCount
     */
    public int getHostileFacilityCount() {
        return hostileFacilityCount;
    }

    /**
     * @param hostileFacilityCount the hostileFacilityCount to set
     */
    public void setHostileFacilityCount(int hostileFacilityCount) {
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

    public static class ObjectiveParameters {
        /**
         * The type of objective this is; 
         */
        StrategicObjectiveType objectiveType;
        
        /**
         * How many strategic objectives will be placed for this contract.
         * 0 means none. -1 indicates that the number of strategic objectives should be scaled 
         * to the number of lances required by the contract. 
         */
        int objectiveCount;
        
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
            MekHQ.getLogger().error(StratconContractDefinition.class, "Serialize", e.getMessage());
        }
    }
}
