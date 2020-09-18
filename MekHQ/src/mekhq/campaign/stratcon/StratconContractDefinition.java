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
import mekhq.campaign.mission.ScenarioTemplate;

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
         * Victory in scenarios - either designated by the "objectiveScenarios" collection or any scenarios.
         */
        ScenarioVictory,
        
        /**
         * Control of facilities generated at contract start time
         */
        FacilityControl,
        
        /**
         * Destruction of hostile facilities generated at contract start time
         */
        FacilityDestruction
    }
    
    public static StratconContractDefinition createTestContract() {
        StratconContractDefinition retVal = new StratconContractDefinition();
        
        retVal.contractTypeName = "Test Contract Type";
        retVal.briefing = "Test Contract Briefing.";
        retVal.alliedFacilityCount = 1;
        retVal.hostileFacilityCount = COUNT_SCALED;
        retVal.objectiveCount = COUNT_SCALED;
        retVal.objectiveScenarios = Arrays.asList("TestScenario.xml", "TestScenario.xml");
        retVal.objectiveOpenFieldModifiers = Arrays.asList("TestMod1.xml", "TestMod2.xml");
        retVal.objectiveFacilityModifiers = Arrays.asList("TestFacMod1.xml", "TestFacMod2.xml");
        retVal.objectivesTypes = Arrays.asList(StrategicObjectiveType.ScenarioVictory, StrategicObjectiveType.FacilityControl, StrategicObjectiveType.FacilityDestruction);
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
     * How many strategic objectives will be placed for this contract.
     * 0 means none. -1 indicates that the number of strategic objectives should be scaled 
     * to the number of lances required by the contract. 
     */
    private int objectiveCount;
    
    /**
     * Specific scenario IDs (file names) which must be completed in order to complete this contract
     */
    private List<String> objectiveScenarios;
    
    /**
     * Modifiers which will be applied to any open-field "objective" scenarios
     */
    private List<String> objectiveOpenFieldModifiers;
    
    /**
     * Modifiers which will be applied to any "objective" facility scenarios
     */
    private List<String> objectiveFacilityModifiers;
    
    /**
     * List of objective types, which determine what counts for 
     * "strategic objectives"
     */
    private List<StrategicObjectiveType> objectivesTypes;
    
    /**
     * List of scenario IDs (file names) that are allowed for this contract type
     */
    private List<String> allowedScenarios;
    
    /**
     * List of scenario IDs (file names) that are not allowed for this contract type
     */
    private List<String> forbiddenScenarios;

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

    /**
     * @return the objectiveCount
     */
    public int getObjectiveCount() {
        return objectiveCount;
    }

    /**
     * @param objectiveCount the objectiveCount to set
     */
    public void setObjectiveCount(int objectiveCount) {
        this.objectiveCount = objectiveCount;
    }

    /**
     * @return the objectiveScenarios
     */
    @XmlElementWrapper(name="objectiveScenarios")
    @XmlElement(name="objectiveScenario")
    public List<String> getObjectiveScenarios() {
        return objectiveScenarios;
    }

    /**
     * @param objectiveScenarios the objectiveScenarios to set
     */
    public void setObjectiveScenarios(List<String> objectiveScenarios) {
        this.objectiveScenarios = objectiveScenarios;
    }

    /**
     * @return the objectiveOpenFieldModifiers
     */
    @XmlElementWrapper(name="objectiveOpenFieldModifiers")
    @XmlElement(name="objectiveOpenFieldModifier")
    public List<String> getObjectiveOpenFieldModifiers() {
        return objectiveOpenFieldModifiers;
    }

    /**
     * @param objectiveOpenFieldModifiers the objectiveOpenFieldModifiers to set
     */
    public void setObjectiveOpenFieldModifiers(List<String> objectiveOpenFieldModifiers) {
        this.objectiveOpenFieldModifiers = objectiveOpenFieldModifiers;
    }

    /**
     * @return the objectiveFacilityModifiers
     */
    @XmlElementWrapper(name="objectiveFacilityModifiers")
    @XmlElement(name="objectiveFacilityModifier")
    public List<String> getObjectiveFacilityModifiers() {
        return objectiveFacilityModifiers;
    }

    /**
     * @param objectiveFacilityModifiers the objectiveFacilityModifiers to set
     */
    public void setObjectiveFacilityModifiers(List<String> objectiveFacilityModifiers) {
        this.objectiveFacilityModifiers = objectiveFacilityModifiers;
    }

    @XmlElementWrapper(name="objectivesTypes")
    @XmlElement(name="objectivesType")
    public List<StrategicObjectiveType> getObjectivesTypes() {
        return objectivesTypes;
    }

    public void setObjectivesTypes(List<StrategicObjectiveType> objectivesTypes) {
        this.objectivesTypes = objectivesTypes;
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
