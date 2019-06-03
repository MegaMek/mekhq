package mekhq.campaign.stratcon;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.ScenarioTemplate;

/**
 * Contract-level state object for a stratcon campaign.
 * @author NickAragua
 *
 */
@XmlRootElement(name="StratconCampaignState")
public class StratconCampaignState {
    public static final String ROOT_XML_ELEMENT_NAME = "StratconCampaignState";
    
    @XmlTransient
    private AtBContract contract;

    // these are all state variables that affect the current Stratcon Campaign
    private double globalOpforBVMultiplier;
    private int supportPoints;
    private int victoryPoints;
    private int strategicObjectivePoints;
    
    @XmlElementWrapper(name="campaignTracks")
    @XmlElement(name="campaignTrack")
    private List<StratconTrackState> tracks;

    @XmlTransient
    public AtBContract getContract() {
        return contract;
    }

    public void setContract(AtBContract contract) {
        this.contract = contract;
    }

    public StratconCampaignState() {
        tracks = new ArrayList<>();
    }
    
    public StratconCampaignState(AtBContract contract) {
        tracks = new ArrayList<>();
        this.setContract(contract);
    }

    /**
     * The opfor BV multiplier. Intended to be additive.
     * @return The additive opfor BV multiplier.
     */
    public double getGlobalOpforBVMultiplier() {
        return globalOpforBVMultiplier;
    }
    
    public StratconTrackState getTrack(int index) {
        return tracks.get(index);
    }
    
    public List<StratconTrackState> getTracks() {
        return tracks;
    }
    
    public void addTrack(StratconTrackState track) {
        tracks.add(track);
    }
    
    public int getSupportPoints() {
        return supportPoints;
    }

    public void setSupportPoints(int supportPoints) {
        this.supportPoints = supportPoints;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.victoryPoints = victoryPoints;
    }

    public int getStrategicObjectivePoints() {
        return strategicObjectivePoints;
    }

    public void setStrategicObjectivePoints(int strategicObjectivePoints) {
        this.strategicObjectivePoints = strategicObjectivePoints;
    }
    
    public void useSupportPoint() {
        supportPoints--;
    }
    
    public void convertVictoryToSupportPoint() {
        victoryPoints--;
        supportPoints++;
    }
    
    public void addStrategicObjectivePoint() {
        strategicObjectivePoints++;
    }
    
    /**
     * Convenience/speed method of determining whether or not a force with the given ID has been deployed to a track in this campaign.
     * @param forceID the force ID to check
     * @return Deployed or not.
     */
    public boolean isForceDeployedHere(int forceID) {
        for(StratconTrackState trackState : tracks) {
            if(trackState.getAssignedForceCoords().containsKey(forceID)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Serialize this instance of a campaign state to a PrintWriter
     * Omits initial xml declaration
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(StratconCampaignState.class);
            JAXBElement<StratconCampaignState> stateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), StratconCampaignState.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.fragment", true);
            m.marshal(stateElement, pw);
        } catch(Exception e) {
            MekHQ.getLogger().error(StratconCampaignState.class, "Serialize", e.getMessage());
        }
    }
    
    /**
     * Attempt to deserialize an instance of a Campaign State from the passed-in XML Node
     * @param xmlNode The node with the campaign state
     * @return Possibly an instance of a StratconCampaignState
     */
    public static StratconCampaignState Deserialize(Node xmlNode) {
        StratconCampaignState resultingCampaignState = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(StratconCampaignState.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<StratconCampaignState> templateElement = um.unmarshal(xmlNode, StratconCampaignState.class);
            resultingCampaignState = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(StratconCampaignState.class, "Deserialize", "Error Deserializing Campaign State", e);
        }
        
        return resultingCampaignState;
    }
}