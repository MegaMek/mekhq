package mekhq.campaign.mission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

public class ScenarioTemplate {

    public String name;
    public String shortBriefing;
    public String detailedBriefing;
    
    @XmlElementWrapper(name="scenarioForces")
    @XmlElement(name="scenarioForce")
    public List<ScenarioForceTemplate> scenarioForces = new ArrayList<>();
    
    public ScenarioMapParameters mapParameters = new ScenarioMapParameters();
    
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
    
    public void Deserialize(File inputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioTemplate> templateElement = (JAXBElement<ScenarioTemplate>) um.unmarshal(inputFile);
            ScenarioTemplate test = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Serialize", e.getMessage());
        }
    }
    
}
