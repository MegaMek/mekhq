package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class StandUpBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 9014090149648362938L;

	@Override
	public int getScenarioType() {
		return STANDUP;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Stand Up";
	}

	@Override
	public String getResourceKey() {
		return "standup";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int playerHome = startPos[Compute.randomInt(4)];
		setStart(playerHome);

		int enemyStart = getStart() + 4;

		if (enemyStart > 8) {
			enemyStart -= 8;
		}

		setEnemyHome(enemyStart);

		if (allyEntities.size() > 0) {
			addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
		}

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addBotForce(getEnemyBotForce(getContract(campaign), getEnemyHome(), getEnemyHome(), enemyEntities));
	}

	@Override
	public boolean canAddDropShips() {
	    return true;
		//return Compute.d6() <= 2;
	}
	
	@Override
	public void setObjectives(Campaign campaign, AtBContract contract) {
	    ScenarioObjective destroyHostiles = new ScenarioObjective();
	    destroyHostiles.setDescription("Destroy, cripple or force the withdrawal of at least 50% of the following enemy force(s):");
	    destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.ForceWithdraw);
	    destroyHostiles.setPercentage(50);
	    destroyHostiles.addForce(contract.getEnemyBotName());
	    
	    ObjectiveEffect successEffect = new ObjectiveEffect();
	    successEffect.effectType = ObjectiveEffectType.ContractScoreUpdate;
	    successEffect.howMuch = 1;
	    
	    ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
	    keepFriendliesAlive.setDescription("Ensure that at least 50% of the following force(s) and unit(s) survive:");
	    keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
	    keepFriendliesAlive.setPercentage(50);
	    keepFriendliesAlive.addForce(campaign.getForce(getLanceForceId()).getName());
	    for(int botForceID = 0; botForceID < getNumBots(); botForceID++) {
	        // kind of hack-ish:
	        // if there's an allied bot that shares employer name, then add it to the survival objective
	        // we know there's only one of those, so break out of the loop when we see it
	        if(getBotForce(botForceID).getName().equals(contract.getAllyBotName())) {
	            keepFriendliesAlive.addForce(contract.getAllyBotName());
	            break;
	        }
	    }
	    
	    for(UUID attachedAlly : this.getAttachedUnitIds()) {
	        keepFriendliesAlive.addUnit(attachedAlly.toString());
	    }
        
        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ContractScoreUpdate;
        friendlyFailureEffect.howMuch = -1;
        
        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
	}
}
