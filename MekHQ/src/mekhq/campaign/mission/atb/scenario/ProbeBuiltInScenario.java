package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ProbeBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -2917489684589205519L;

	@Override
	public int getScenarioType() {
		return PROBE;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Probe";
	}

	@Override
	public void setTerrain() {
		do {
			setTerrainType(terrainChart[Compute.d6(2) - 2]);
		} while (getTerrainType() == TER_HEAVYURBAN);
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

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_MEDIUM, 0, 0,
				campaign);

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));
	}
}
