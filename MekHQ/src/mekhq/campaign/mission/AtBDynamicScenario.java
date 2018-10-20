package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.personnel.Person;

/**
 * Data structure intended to hold data relevant to AtB Dynamic Scenarios (AtB 3.0) 
 * @author NickAragua
 *
 */
public class AtBDynamicScenario extends AtBScenario {

    /**
     * 
     */
    private static final long serialVersionUID = 4671466413188687036L;

    // derived fields used for various calculations
    private int effectivePlayerUnitCount;
    private int effectivePlayerBV;
    
    // convenient pointers that let us keep data around that would otherwise need reloading
    private ScenarioTemplate template;      // the template that is being used to generate this scenario
    
    private Map<BotForce, ScenarioForceTemplate> botForceTemplates;
    private Map<Integer, ScenarioForceTemplate> playerForceTemplates;
    
    public AtBDynamicScenario() {
        super();
        
        botForceTemplates = new HashMap<>();
        playerForceTemplates = new HashMap<>();
    }

    @Override
    public void addForces(int forceID) {
        super.addForces(forceID);
        
        // loop through all player-supplied forces in the template, if there is one
        // assign the newly-added force to the first template we find
        if(template != null) {
            for(ScenarioForceTemplate forceTemplate : template.scenarioForces.values()) {
                if((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) &&
                        !playerForceTemplates.containsValue(forceTemplate)) {
                    playerForceTemplates.put(forceID, forceTemplate);
                    return;
                }
            }
        }
        
        playerForceTemplates.put(forceID, null);
    }
    
    @Override
    public void removeForce(int fid) {
        super.removeForce(fid);
        playerForceTemplates.remove(fid);
    }
    
    @Override
    public int getStart() {
        // If we've assigned at least one force
        // and there's a player force template associated with the first force
        // then return the generated deployment zone associated with the first force
        if(!getForceIDs().isEmpty() &&
                playerForceTemplates.containsKey(getForceIDs().get(0))) {
            return playerForceTemplates.get(getForceIDs().get(0)).getActualDeploymentZone();
        }
        
        return super.getStart();
    }
    
    /**
     * Horizontal map size.
     * Unlike the AtBScenario, we only perform map size calculations once (once all primary forces are committed),
     * so we don't re-calculate the map size each time.
     */
    @Override
    public int getMapX() {
        return getMapSizeX();
    }
    
    /**
     * Vertical map size.
     * Unlike the AtBScenario, we only perform map size calculations once (once all primary forces are committed),
     * so we don't re-calculate the map size each time.
     */
    @Override
    public int getMapY() {
        return getMapSizeY();
    }
    
    public void addBotForce(BotForce botForce, ScenarioForceTemplate forceTemplate) {
        super.addBotForce(botForce);        
        botForceTemplates.put(botForce, forceTemplate);
    }
    
    public int getEffectivePlayerUnitCount() {
        return effectivePlayerUnitCount;
    }
    
    public void setEffectivePlayerUnitCount(int unitCount) {
        effectivePlayerUnitCount = unitCount;
    }
    
    public int getEffectivePlayerBV() {
        return effectivePlayerBV;
    }
    
    public void setEffectivePlayerBV(int unitCount) {
        effectivePlayerBV = unitCount;
    }
    
    public void setScenarioTemplate(ScenarioTemplate template) {
        this.template = template;
    }
    
    public ScenarioTemplate getTemplate() {
        return template;
    }
    
    public Map<Integer, ScenarioForceTemplate> getPlayerForceTemplates() {
        return playerForceTemplates;
    }
    
    public Map<BotForce, ScenarioForceTemplate> getBotForceTemplates() {
        return botForceTemplates;
    }
    
    /**
     * Convenience method that returns the commander of the first force assigned to this scenario.
     * @return
     */
    public Person getLanceCommander(Campaign campaign) {
        if(getForceIDs().isEmpty()) {
            return null; // if we don't have forces, just a bunch of units, then get the highest-ranked?
        }
        
        Lance lance = campaign.getLances().get(getForceIDs().get(0));
        
        if(lance != null) {
            lance.refreshCommander(campaign);
            return lance.getCommander(campaign);
        } else {
            return null;
        }
    }

    @Override
    public int getScenarioType() {
        return 0;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Dynamic Scenario";
    }

    @Override
    public String getResourceKey() {
        return null;
    }
    
    @Override
    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        if(template != null) {
            template.Serialize(pw1);
        }
        
        super.writeToXmlEnd(pw1, indent);
    }
    
    @Override
    protected void loadFieldsFromXmlNode(Node wn) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            
            if (wn2.getNodeName().equalsIgnoreCase(ScenarioTemplate.ROOT_XML_ELEMENT_NAME)) {
                template = ScenarioTemplate.Deserialize(wn2);
            }
        }
        
        super.loadFieldsFromXmlNode(wn);
    }
    
    @Override
    public void setTerrain() {
        AtBDynamicScenarioFactory.setTerrain(this);
    }
}
