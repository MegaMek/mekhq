package mekhq.campaign.mission.atb;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

/**
 * A manifest containing IDs and file names of legacy AtB scenario definitions
 * @author NickAragua
 *
 */
@XmlRootElement(name="scenarioManifest")
public class AtBScenarioManifest {
    @XmlElementWrapper(name="scenarioFileNames")
    @XmlElement(name="scenarioFileName")
    public Map<Integer, String> scenarioFileNames;
    
    /**
     * Attempt to deserialize an instance of an AtBScenarioManifest from the passed-in file 
     * @param inputFile The path to the manifest
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static AtBScenarioManifest Deserialize(String fileName) {
        AtBScenarioManifest resultingManifest = null;
        File inputFile = new File(fileName);
        if(!inputFile.exists()) {
            MekHQ.getLogger().warning(ScenarioModifierManifest.class, "Deserialize", String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<AtBScenarioManifest> manifestElement = um.unmarshal(inputSource, AtBScenarioManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch(Exception e) {
            MekHQ.getLogger().error(AtBScenarioManifest.class, "Deserialize", "Error Deserializing Scenario Manifest", e);
        }

        return resultingManifest;
    }
}
