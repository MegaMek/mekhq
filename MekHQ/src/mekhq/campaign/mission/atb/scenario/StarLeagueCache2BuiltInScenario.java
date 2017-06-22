package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;

@AtBScenarioEnabled
public class StarLeagueCache2BuiltInScenario extends StarLeagueCache1BuiltInScenario {
	private static final long serialVersionUID = -4016473977755732211L;

	@Override
	public int getScenarioType() {
		return STARLEAGUECACHE2;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Star League Cache 2";
	}

	@Override
	public String getResourceKey() {
		return "starLeagueCache2";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(Board.START_N);
		int enemyStart = Board.START_S;

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
			enemyEntities = new ArrayList<Entity>();
			MechSummary ms = campaign.getUnitGenerator().generate("SL", UnitType.MEK, weight, 2750,
					(Compute.d6() == 6) ? IUnitRating.DRAGOON_A : IUnitRating.DRAGOON_D);

			if (ms != null) {
				enemyEntities.add(createEntityWithCrew(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), campaign, ms));
			} else {
				enemyEntities.add(null);
			}

			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));
	}
}
