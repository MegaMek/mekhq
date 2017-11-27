package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class BaseAttackBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -873528230365616996L;

	@Override
	public int getScenarioType() {
		return BASEATTACK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Base Attack";
	}

	@Override
	public String getResourceKey() {
		return "baseAttack";
	}

	@Override
	public void setTerrain() {
		setTerrainType((Compute.d6() < 4) ? TER_LIGHTURBAN : TER_HEAVYURBAN);
	}

	@Override
	public int getMapX() {
		return getBaseMapX() + 10;
	}

	@Override
	public int getMapY() {
		return getBaseMapY() + 10;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
	    int attackerStartIndex = Compute.randomInt(4);
		int attackerStart = startPos[attackerStartIndex];
		int defenderStart = Board.START_CENTER;
		int defenderHome = (attackerStart + 4) % 8; // the defender's "retreat" edge should always be the opposite of the attacker's edge
		
		int enemyStart;
		
		// the attacker starts on an edge, the defender starts in the center and flees to the opposite edge of the attacker
		if (isAttacker()) {
		    setStart(attackerStart);
		    
			setEnemyHome(defenderHome);
			enemyStart = defenderStart;
		} else {
		    setStart(defenderStart);
		    
		    setEnemyHome(attackerStart);
		    enemyStart = attackerStart;
		}

		/*
		 * Ally deploys 2 lances of a lighter weight class than the player,
		 * minimum light
		 */
		int allyForceWeight = Math.max(getLance(campaign).getWeightClass(campaign) - 1, EntityWeightClass.WEIGHT_LIGHT);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForceWeight, campaign);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForceWeight, campaign);

		// the "second" force will be deployed (orthogonally) between 90 degrees clockwise and counterclockwise from the "primary force".
		int angleChange = Compute.randomInt(2) - 1;
		int secondAttackerForceStart = startPos[(attackerStartIndex + angleChange) % 4]; 
		
		// the ally is the "second force" and will flee either in the same direction as the player (in case of the player being the defender)
		// or where it came from (in case of the player being the attacker
		addBotForce(getAllyBotForce(getContract(campaign), isAttacker() ? secondAttackerForceStart : getStart(), 
		        isAttacker() ? secondAttackerForceStart : defenderHome, allyEntities));

		// "base" force gets 8 civilian units and six turrets
		// set the civilians to "cowardly" behavior by default so they don't run out and get killed. As much. 
		ArrayList<Entity> otherForce = new ArrayList<>();
		addCivilianUnits(otherForce, 8, campaign);
		BotForce civilianForce = new BotForce("Base Civilian Units", isAttacker() ? 2 : 1, defenderStart, defenderHome, otherForce);
		civilianForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().COWARDLY_BEHAVIOR);
		civilianForce.setHomeEdge(defenderHome); // setting behavior settings rewrites home edge, so we set it again
		addBotForce(civilianForce);

		ArrayList<Entity> turretForce = new ArrayList<>();
		addBotForce(new BotForce("Base Turrets", isAttacker() ? 2 : 1, defenderStart, defenderHome, turretForce));
		if(isAttacker()) {
			addTurrets(turretForce, 6, getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(), campaign);
		} else {
			addTurrets(turretForce, 6, getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), campaign);
		}
		
		/* Roll 2x on bot lances roll */
        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));
        
        // the "second" enemy force will either flee in the same direction as the first enemy force in case of the player being the attacker
        // or where it came from in case of player being defender
        ArrayList<Entity> secondBotEntities = new ArrayList<>();
        addEnemyForce(secondBotEntities, getLance(campaign).getWeightClass(campaign), campaign);
        BotForce secondBotForce = getEnemyBotForce(getContract(campaign), isAttacker() ? enemyStart : secondAttackerForceStart, 
                isAttacker() ? getEnemyHome() : secondAttackerForceStart, secondBotEntities);
        secondBotForce.setName(secondBotForce.getName() + " Force #2");
        addBotForce(secondBotForce);
	}
}
