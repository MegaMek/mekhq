package mekhq.campaign.mission.atb;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;

public abstract class AtBScenarioModifier {
 // possible scenario effects:
 // add bot force(s) to template - preGeneration
 // re-generate bot skill levels - postGeneration
 // adjust equipment quality - preGeneration - scenario needs "current effective skill level" or "skill modifier"
 // modify one or more enemy units - postGeneration
 // remove units from bot force - postGeneration
 // switch scenario templates - preGeneration
 // set scenario flag - preGeneration
 // add objective - preGeneration
    public String additionalBriefingText;
        
    public void preApply(AtBDynamicScenario scenario, Campaign campaign) {
        
    }
    
    public void postApply(AtBDynamicScenario scenario, Campaign campaign) {
        scenario.setDesc(String.format("%s\n\n%s", scenario.getDescription(), additionalBriefingText));
    }
}
