package mekhq.campaign.mission;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;

@XmlRootElement(name="ScenarioTemplate")
public class ScenarioTemplate {

    public String name;
    public String shortBriefing;
    public String detailedBriefing;
    //public List<ScenarioForceTemplate> scenarioForces = new ArrayList<>();
    
    public ScenarioMapParameters mapParameters = new ScenarioMapParameters();
    
    @XmlElementWrapper(name="scenarioForces")
    @XmlElement(name="scenarioForce")
    public Map<String, ScenarioForceTemplate> scenarioForces = new HashMap<>();
    
    public List<ScenarioForceTemplate> getAllScenarioForces() {
        return (ArrayList) scenarioForces.values();
    }
    
    public List<ScenarioForceTemplate> getAllPlayerControlledAllies() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()))
                .collect(Collectors.toList());
    }
    
    public List<ScenarioForceTemplate> getAllBotControlledAllies() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Allied.ordinal()) &&
            (forceTemplate.getGenerationMethod() != ForceGenerationMethod.PlayerDeployed.ordinal()))
                .collect(Collectors.toList());
    }
    
    public List<ScenarioForceTemplate> getAllPlayerControlledHostiles() {
        return scenarioForces.values().stream().filter(forceTemplate -> 
            (forceTemplate.getForceAlignment() == ForceAlignment.Opposing.ordinal()) ||
            (forceTemplate.getForceAlignment() == ForceAlignment.Third.ordinal()))
                .collect(Collectors.toList());
    }
    
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            JAXBElement<ScenarioTemplate> templateElement = new JAXBElement<>(new QName("ScenarioTemplate"), ScenarioTemplate.class, this);
            Marshaller m = context.createMarshaller();
            m.marshal(templateElement, outputFile);
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Serialize", e.getMessage());
        }
    }
    
    public static ScenarioTemplate Deserialize(File inputFile) {
        ScenarioTemplate resultingTemplate = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioTemplate> templateElement = um.unmarshal(new StreamSource(inputFile), ScenarioTemplate.class);
            resultingTemplate = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", "Error Deserializing Scenario Template", e);
        }
        
        return resultingTemplate;
    }
    
}
