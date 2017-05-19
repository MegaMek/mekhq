package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class BreakthroughBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -4258866789358298684L;

	@Override
	public int getScenarioType() {
		return BREAKTHROUGH;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Breakthrough";
	}

	@Override
	public int getMapX() {
		return 18;
	}

	@Override
	public int getMapY() {
		return 50;
	}

	@Override
	public boolean canRerollMapSize() {
		return false;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int enemyStart;
		int playerHome;

		if (isAttacker()) {
			playerHome = Board.START_S;
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(Board.START_N);
		} else {
			setStart(Board.START_CENTER);
			playerHome = Board.START_N;
			enemyStart = Board.START_S;

			setEnemyHome(enemyStart);
		}

		BotForce allyEntitiesForce = null;

		if (allyEntities.size() > 0) {
			allyEntitiesForce = getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities);
			addBotForce(allyEntitiesForce);
		}

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		BotForce botForce = getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities);

		try {
			if (isAttacker()) {
				if (null != allyEntitiesForce) {
					allyEntitiesForce
							.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
				}
			} else {
				botForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
			}
		} catch (PrincessException e) {
			e.printStackTrace();
		}

		addBotForce(botForce);
	}

	@Override
	public boolean canAddDropShips() {
		return !isAttacker() && (Compute.d6() == 1);
	}
}
