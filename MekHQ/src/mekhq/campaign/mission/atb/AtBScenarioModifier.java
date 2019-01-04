package mekhq.campaign.mission.atb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;

@XmlRootElement(name="AtBScenarioModifier")
public class AtBScenarioModifier {

    /**
     * Possible values for when a scenario modifier may occur: before or after primary force generation.
     * @author NickAragua
     *
     */
    public enum EventTiming {
        PreForceGeneration,
        PostForceGeneration
    }

    public String additionalBriefingText = null;
    public Boolean benefitsPlayer = false;
    public Boolean blockFurtherEvents = false;
    public EventTiming eventTiming = null;
    public ScenarioForceTemplate forceDefinition = null;
    public Integer skillAdjustment = null;
    public Integer qualityAdjustment = null;
    public ForceAlignment eventRecipient = null;
    public Integer battleDamageIntensity = null;
    public Integer ammoExpenditureIntensity = null;
    public Integer unitRemovalCount = null;
    public List<MapLocation> allowedMapLocations = null;
    public Boolean useAmbushLogic = null; 
    
    /**
     * Process this scenario modifier for a particular scenario, given a particular timing indicator.
     * @param scenario
     * @param campaign
     * @param eventTiming Whether this is occurring before or after primary forces have been generated.
     */
    public void processModifier(AtBDynamicScenario scenario, Campaign campaign, EventTiming eventTiming) {
        if(eventTiming == this.eventTiming) {
            if(additionalBriefingText != null & additionalBriefingText.length() > 0) {
                AtBScenarioModifierApplicator.appendScenarioBriefingText(scenario, additionalBriefingText);
            }
            
            if(forceDefinition != null) {
                AtBScenarioModifierApplicator.addForce(campaign, scenario, forceDefinition, eventTiming);
            }
            
            if(skillAdjustment != null && eventRecipient != null) {
                AtBScenarioModifierApplicator.adjustSkill(scenario, campaign, eventRecipient, skillAdjustment);
            }
            
            if(qualityAdjustment != null && eventRecipient != null) {
                // AtBScenarioModifierApplicator.adjustQuality(scenario, campaign, eventRecipient, skillAdjustment);
            }
            
            if(battleDamageIntensity != null && eventRecipient != null) {
                AtBScenarioModifierApplicator.inflictBattleDamage(scenario, campaign, eventRecipient, battleDamageIntensity);
            }
            
            if(ammoExpenditureIntensity != null && eventRecipient != null) {
                AtBScenarioModifierApplicator.expendAmmo(scenario, campaign, eventRecipient, ammoExpenditureIntensity);
            }
            
            if(unitRemovalCount != null && eventRecipient != null) {
                AtBScenarioModifierApplicator.removeUnits(scenario, campaign, eventRecipient, unitRemovalCount);
            }
            
            if(useAmbushLogic != null && eventRecipient != null) {
                AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, eventRecipient);
            }
        }
    }
    
    // ----------------------------------------------------------------
    // This section contains static variables and methods
    // 
    private static ScenarioModifierManifest scenarioModifierManifest;
    
    public static List<String> getScenarioFileNames() {
        return scenarioModifierManifest.fileNameList;
    }
    
    private static List<AtBScenarioModifier> scenarioModifiers;
    
    public static List<AtBScenarioModifier> getScenarioModifiers() {
        return scenarioModifiers;
    }
    
    static {
        loadManifest();
        loadScenarios();
    }
    
    /**
     * Loads the scenario modifier manifest.
     */
    private static void loadManifest() {
        scenarioModifierManifest = ScenarioModifierManifest.Deserialize("./data/ScenarioModifiers/modifiermanifest.xml");
        
        // load user-specified modifier list
        ScenarioModifierManifest userModList = ScenarioModifierManifest.Deserialize("./data/ScenarioModifiers/usermodifiermanifest.xml");
        if(userModList != null) {
            scenarioModifierManifest.fileNameList.addAll(userModList.fileNameList);
        }
        
        // go through each entry and clean it up for preceding/trailing white space
        for(int x = 0; x < scenarioModifierManifest.fileNameList.size(); x++) {
            scenarioModifierManifest.fileNameList.set(x, scenarioModifierManifest.fileNameList.get(x).trim());
        }
    }
    
    /** 
     * Loads the defined scenarios from the manifest.
     */
    private static void loadScenarios() {
        scenarioModifiers = new ArrayList<>();
        
        for(String fileName : scenarioModifierManifest.fileNameList) {
            String filePath = String.format("./data/ScenarioModifiers/%s", fileName);
            
            try {
                AtBScenarioModifier modifier = Deserialize(filePath);
                
                if(modifier != null) {
                    scenarioModifiers.add(modifier);
                }
            }
            catch(Exception e) {
                MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", 
                        String.format("Error Loading Scenario %s", filePath), e);
            }
        }
    }
    
    /**
     * Attempt to deserialize an instance of a scenario modifier from the passed-in file
     * @param xmlNode the node to deserialize
     * @return Possibly an instance of a scenario modifier list
     */
    public static AtBScenarioModifier Deserialize(String fileName) {
        AtBScenarioModifier resultingList = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioModifier.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if(!xmlFile.exists()) {
                MekHQ.getLogger().warning(AtBScenarioModifier.class, "Deserialize", String.format("Specified file %d does not exist", fileName));
                return null;
            }
            
            JAXBElement<AtBScenarioModifier> templateElement = um.unmarshal(new StreamSource(xmlFile), AtBScenarioModifier.class);
            resultingList = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", "Error Deserializing Scenario Modifier", e);
        }
        
        return resultingList;
    }    
}

/**
 * Class intended for local use that holds a manifest of scenario modifier definition file names.
 * @author NickAragua
 *
 */
@XmlRootElement(name="scenarioModifierManifest")
class ScenarioModifierManifest {
    @XmlElementWrapper(name="modifiers")
    @XmlElement(name="modifier")
    public List<String> fileNameList = new ArrayList<>();
    
    /**
     * Attempt to deserialize an instance of a scenario modifier list from the passed-in file
     * @param xmlNode the node to deserialize
     * @return Possibly an instance of a scenario modifier list
     */
    public static ScenarioModifierManifest Deserialize(String fileName) {
        ScenarioModifierManifest resultingList = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioModifierManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if(!xmlFile.exists()) {
                MekHQ.getLogger().warning(ScenarioModifierManifest.class, "Deserialize", String.format("Specified file %s does not exist", fileName));
                return null;
            }
            
            JAXBElement<ScenarioModifierManifest> templateElement = um.unmarshal(new StreamSource(xmlFile), ScenarioModifierManifest.class);
            resultingList = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", "Error Deserializing Scenario Modifier List", e);
        }
        
        return resultingList;
    }
}
