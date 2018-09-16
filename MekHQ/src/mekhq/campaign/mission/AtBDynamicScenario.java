package mekhq.campaign.mission;

import java.util.HashMap;
import java.util.Map;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;

/**
 * Data structure intended to hold data relevant to AtB Scenarios (AtB 3.0) 
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
    
    // fields intrinsic to the scenario. Map size for now.
    private int mapSizeX;
    private int mapSizeY;
    
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
        
        // loop through all player-supplied forces in the template
        // assign the newly-added force to the first template we find
        for(ScenarioForceTemplate forceTemplate : template.scenarioForces.values()) {
            if((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) &&
                    !playerForceTemplates.containsValue(forceTemplate)) {
                playerForceTemplates.put(forceID, forceTemplate);
                return;
            }
        }
        
        playerForceTemplates.put(forceID, null);
    }
    
    @Override
    public void removeForce(int fid) {
        super.removeForce(fid);
        playerForceTemplates.remove(fid);
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

    @Override
    public int getScenarioType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getScenarioTypeDescription() {
        // TODO Auto-generated method stub
        return "Dynamic Scenario";
    }

    @Override
    public String getResourceKey() {
        // TODO Auto-generated method stub
        return "baseAttack";
    }
}
