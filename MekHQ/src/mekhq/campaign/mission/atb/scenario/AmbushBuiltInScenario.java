package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class AmbushBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 1223302967912696039L;

	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return AMBUSH;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Ambush";
	}

	@Override
	public String getResourceKey() {
		return "ambush";
	}

	@Override
	public void setMapFile() {
		setMap("Savannah");
		setTerrainType(TER_FLATLANDS);
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(Board.START_CENTER);
		int enemyStart = Board.START_CENTER;

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
		    enemyEntities = new ArrayList<Entity>();
			if (weight == EntityWeightClass.WEIGHT_LIGHT) {
				enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
						getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));

				enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
						getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
			} else {
				for (int i = 0; i < 3; i++) {
					enemyEntities
							.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
									getContract(campaign).getEnemyQuality(), UnitType.MEK, weight - 1, campaign));
				}
			}

			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));
	}
}
