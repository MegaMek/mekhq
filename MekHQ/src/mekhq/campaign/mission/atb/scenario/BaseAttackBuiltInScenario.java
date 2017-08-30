package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.Calendar;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
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
		int enemyStart;
		int playerHome;

		if (isAttacker()) {
			playerHome = startPos[Compute.randomInt(4)];
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(playerHome + 4);

			if (getEnemyHome() > 8) {
				setEnemyHome(getEnemyHome() - 8);
				;
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

		/*
		 * Ally deploys 2 lances of a lighter weight class than the player,
		 * minimum light
		 */
		int allyForce = Math.max(getLance(campaign).getWeightClass(campaign) - 1, EntityWeightClass.WEIGHT_LIGHT);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForce, campaign);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForce, campaign);

		addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));

		/* Roll 2x on bot lances roll */
		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

		// "base" force gets 8 civilian units and six turrets
		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 8, campaign);
		addBotForce(new BotForce("Civilians", isAttacker() ? 2 : 1, isAttacker() ? getEnemyHome() : playerHome,
				isAttacker() ? enemyStart : getStart(), otherForce));

		if(isAttacker()) {
			addTurrets(otherForce, 6, getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(), campaign);
		}
		else {
			addTurrets(otherForce, 6, getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), campaign);
		}
	}
}
