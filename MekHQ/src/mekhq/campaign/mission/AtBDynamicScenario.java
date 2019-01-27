package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

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
    
    private int effectiveOpforSkill;
    private int effectiveOpforQuality;
    
    // convenient pointers that let us keep data around that would otherwise need reloading
    private ScenarioTemplate template;      // the template that is being used to generate this scenario
    
    private Map<BotForce, ScenarioForceTemplate> botForceTemplates;
    private Map<Integer, ScenarioForceTemplate> playerForceTemplates;
    
    private List<AtBScenarioModifier> scenarioModifiers;
    
    public AtBDynamicScenario() {
        super();
        
        botForceTemplates = new HashMap<>();
        playerForceTemplates = new HashMap<>();
        scenarioModifiers = new ArrayList<>();
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
    
    public int getEffectiveOpforSkill() {
        return effectiveOpforSkill;
    }
    
    public int getEffectiveOpforQuality() {
        return effectiveOpforQuality;
    }
    
    public void setEffectiveOpforSkill(int skillLevel) {
        effectiveOpforSkill = skillLevel;
    }
    
    public void setEffectiveOpforQuality(int qualityLevel) {
        effectiveOpforQuality = qualityLevel;
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
    
    /**
     * Convenience method to return the int value of the lance commander's skill in the specified area.
     * Encapsulates a fairly obnoxious number of null checks and other safety code.
     * @param skill The type of skill to check.
     * @return The skill level. SKILL_NONE (0) if not present.
     */
    public int getLanceCommanderSkill(String skillType, Campaign campaign) {
        Person commander = getLanceCommander(campaign);
        int skillValue = SkillType.SKILL_NONE;
        
        if((commander != null) &&
                commander.hasSkill(skillType)) {
            skillValue = commander.getSkill(skillType).getLevel();
        }
        
        return skillValue;
    }
    
    public void setScenarioModifiers(List<AtBScenarioModifier> scenarioModifiers) {
        scenarioModifiers = new ArrayList<>();
        Collections.copy(this.scenarioModifiers, scenarioModifiers);
    }
    
    public List<AtBScenarioModifier> getScenarioModifiers() {
        return scenarioModifiers;
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
        // if we have a scenario template and haven't played the scenario out yet, serialize the template
        // in its current state
        if(template != null && isCurrent()) {
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
