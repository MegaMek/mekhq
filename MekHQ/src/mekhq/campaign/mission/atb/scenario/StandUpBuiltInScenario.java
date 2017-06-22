package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
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
		return Compute.d6() <= 2;
	}
}
