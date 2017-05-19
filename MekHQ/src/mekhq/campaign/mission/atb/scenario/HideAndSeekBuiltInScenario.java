package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class HideAndSeekBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 2914975739133286379L;

	@Override
	public int getScenarioType() {
		return HIDEANDSEEK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Hide and Seek";
	}

	@Override
	public void setTerrain() {
		do {
			setTerrainType(terrainChart[Compute.d6(2) - 2]);
		} while ((getTerrainType() == TER_WETLANDS || getTerrainType() == TER_COASTAL
				|| getTerrainType() == TER_FLATLANDS));
	}

	@Override
	public int getMapX() {
		return getBaseMapX() - 10;
	}

	@Override
	public int getMapY() {
		return getBaseMapY() - 10;
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

		if (isAttacker()) {
			addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_ASSAULT,
					2, 0, campaign);
		} else {
			addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_HEAVY, 0,
					0, campaign);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));
	}
}
