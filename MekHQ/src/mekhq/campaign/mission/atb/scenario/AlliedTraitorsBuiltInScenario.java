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
public class AlliedTraitorsBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 1820229123054387445L;

	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return ALLIEDTRAITORS;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Allied Traitors";
	}

	@Override
	public String getResourceKey() {
		return "alliedTraitors";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(Board.START_CENTER);
		int enemyStart = Board.START_CENTER;

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
		    enemyEntities = new ArrayList<Entity>();
			enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
					getContract(campaign).getAllyQuality(), UnitType.MEK, weight, campaign));

			enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
					getContract(campaign).getAllyQuality(), UnitType.MEK, weight, campaign));

			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(
				new BotForce(getContract(campaign).getAllyBotName(), 2, enemyStart, getSpecMissionEnemies().get(0)));
	}
}
