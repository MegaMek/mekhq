package mekhq.campaign.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;

/**
 * This is the root data structure for organizing information related to a scenario template.
 * @author NickAragua
 *
 */
@XmlRootElement(name="ScenarioTemplate")
public class ScenarioTemplate implements Cloneable {
    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioTemplate";
    public static final String PRIMARY_PLAYER_FORCE_ID = "Player";
    
    public String name;
    public String shortBriefing;
    public String detailedBriefing;
    
    public ScenarioMapParameters mapParameters = new ScenarioMapParameters();
    public List<String> scenarioModifiers = new ArrayList<>(); 
    
    @XmlElementWrapper(name="scenarioForces")
    @XmlElement(name="scenarioForce")
    public Map<String, ScenarioForceTemplate> scenarioForces = new HashMap<>();
    
    public Object clone() {
        ScenarioTemplate clone = new ScenarioTemplate();
        
        clone.name = name;
        clone.shortBriefing = shortBriefing;
        clone.detailedBriefing = detailedBriefing;
        clone.mapParameters = (ScenarioMapParameters) mapParameters.clone();
        clone.scenarioForces = new HashMap<>(scenarioForces);
        clone.scenarioModifiers = new ArrayList<>(scenarioModifiers);
        
        return clone;
    }
    
    /**
     * Returns the "primary" player force. This is always the force with the name "Player".
     * @return Primary player force.
     */
    public ScenarioForceTemplate getPrimaryPlayerForce() {
        return scenarioForces.get(PRIMARY_PLAYER_FORCE_ID);
    }
    
    public List<ScenarioForceTemplate> getAllScenarioForces() {
        return scenarioForces.values().stream().collect(Collectors.toList());
    }
    
    public List<ScenarioForceTemplate> getAllBotControlledAllies() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Allied.ordinal()) &&
            (forceTemplate.getGenerationMethod() != ForceGenerationMethod.PlayerSupplied.ordinal()))
                .collect(Collectors.toList());
    }
    
    public List<ScenarioForceTemplate> getAllBotControlledHostiles() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Opposing.ordinal()) ||
            (forceTemplate.getForceAlignment() == ForceAlignment.Third.ordinal()))
                .collect(Collectors.toList());
    }
    
    /**
     * All force templates that are controlled and supplied, or potentially supplied, by the player, that are not reinforcements
     * @return List of scenario force templates
     */
    public List<ScenarioForceTemplate> getAllPrimaryPlayerForces() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) &&
            (forceTemplate.getArrivalTurn() != ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) &&
                ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) ||
                 (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal())))  
                .collect(Collectors.toList());
    }
    
    /**
     * All force templates that are controlled and supplied, or potentially supplied, by the player, that are not reinforcements
     * @return List of scenario force templates
     */
    public List<ScenarioForceTemplate> getAllPlayerReinforcementForces() {
        List<ScenarioForceTemplate> retVal = new ArrayList<>();
        
        for(ScenarioForceTemplate forceTemplate : scenarioForces.values()) {
            if((forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) &&
                    (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) &&
                    ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) ||
                     (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal()))) {
                retVal.add(forceTemplate);
            }
        }
        
        return retVal;
    }
    
    /**
     * Serialize this instance of a scenario template to a File
     * Please pass in a non-null file.
     * @param outputFile The destination file.
     */
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            JAXBElement<ScenarioTemplate> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), ScenarioTemplate.class, this);
            Marshaller m = context.createMarshaller();
            m.marshal(templateElement, outputFile);
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Serialize", e.getMessage());
        }
    }
    
    /**
     * Serialize this instance of a scenario template to a PrintWriter
     * Omits initial xml declaration
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            JAXBElement<ScenarioTemplate> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), ScenarioTemplate.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.fragment", true);
            m.marshal(templateElement, pw);
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Serialize", e.getMessage());
        }
    }
    
    /**
     * Attempt to deserialize a file at the given path.
     * @param filePath The location of the file
     * @return Possibly an instance of a scenario template.
     */
    public static ScenarioTemplate Deserialize(String filePath) {
        File inputFile = new File(filePath);
        if(!inputFile.exists()) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", String.format("Cannot deserialize file %s, does not exist", filePath));
            return null;
        }
        
        return Deserialize(inputFile);
    }
    
    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in file 
     * @param inputFile The source file
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioTemplate Deserialize(File inputFile) {
        ScenarioTemplate resultingTemplate = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<ScenarioTemplate> templateElement = um.unmarshal(inputSource, ScenarioTemplate.class);
                resultingTemplate = templateElement.getValue();
            }
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", "Error Deserializing Scenario Template", e);
        }

        return resultingTemplate;
    }
    
    /**
     * Attempt to deserialize an instance of a ScenarioTemplate from the passed-in XML Node
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
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", "Error Deserializing Scenario Template", e);
        }
        
        return resultingTemplate;
    }
}
