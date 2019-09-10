package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ReconRaidBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 8666518710079585834L;

	@Override
	public int getScenarioType() {
		return RECONRAID;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Recon Raid";
	}

	@Override
	public String getResourceKey() {
		return "reconRaid";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int enemyStart;
		int playerHome;

		if (isAttacker()) {
			playerHome = startPos[Compute.randomInt(4)];
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(playerHome + 4);

			if (getEnemyHome() > 8) {
				setEnemyHome(getEnemyHome() - 8);
			}
		} else {
			setStart(Board.START_CENTER);
			enemyStart = startPos[Compute.randomInt(4)];
			setEnemyHome(enemyStart);
			playerHome = getEnemyHome() + 4;

			if (playerHome > 8) {
				playerHome -= 8;
			}
		}

		if (allyEntities.size() > 0) {
			addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
		}

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign),
				isAttacker() ? EntityWeightClass.WEIGHT_ASSAULT : EntityWeightClass.WEIGHT_MEDIUM, 0, 0, campaign);

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));
	}

	@Override
	public boolean canAddDropShips() {
		return isAttacker() && (Compute.d6() <= 3);
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
	    super.setObjectives(campaign, contract);
	    
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract, this);
        
        if(keepAttachedUnitsAlive != null) {
            getObjectives().add(keepAttachedUnitsAlive);
        }
        
        if(isAttacker()) {
            ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 75, false);
            getObjectives().add(keepFriendliesAlive);
            
            ScenarioObjective raidObjective = new ScenarioObjective();
            raidObjective.setObjectiveCriterion(ObjectiveCriterion.Custom);
            raidObjective.setDescription("Recon Raid:");
            raidObjective.addDetail("Move one unit to opposite map edge from starting edge.");
            raidObjective.addDetail("Remain stationary for 2 turns.");
            raidObjective.addDetail("Return to starting edge.");
            raidObjective.addDetail("1d6-2 bonus rolls if victorious.");
            
            ObjectiveEffect victoryEffect = new ObjectiveEffect();
            victoryEffect.effectType = ObjectiveEffectType.AtBBonus;
            victoryEffect.howMuch = Compute.d6() - 2;
            raidObjective.addSuccessEffect(victoryEffect);
            
            getObjectives().add(raidObjective);
        } else {
            destroyHostiles.addDetail("Time limit: 10 turns");
            destroyHostiles.setTimeLimit(10);
            getObjectives().add(destroyHostiles);
        }
    }
}
