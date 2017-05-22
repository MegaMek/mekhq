package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ExtractionBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 2669891555728754709L;

	@Override
	public int getScenarioType() {
		return EXTRACTION;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Extraction";
	}

	@Override
	public String getResourceKey() {
		return "extraction";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int enemyStart;
		int otherStart;
		int playerHome;

		if (isAttacker()) {
			playerHome = startPos[Compute.randomInt(4)];
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(playerHome + 4);

			if (getEnemyHome() > 8) {
				setEnemyHome(getEnemyHome() - 8);
			}

			otherStart = getStart() + 4;
		} else {
			setStart(Board.START_CENTER);
			enemyStart = startPos[Compute.randomInt(4)];

			setEnemyHome(enemyStart);
			playerHome = getEnemyHome() + 4;

			if (playerHome > 8) {
				playerHome -= 8;
			}

			otherStart = enemyStart + 4;
		}
		if (otherStart > 8) {
			otherStart -= 8;
		}

		if (allyEntities.size() > 0) {
			addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
		}

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 4, campaign);

		try {
			if (isAttacker()) {
				BotForce bf = new BotForce("Civilians", 1, otherStart, playerHome, otherForce);
				bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());

				addBotForce(bf);
				
				for (Entity en : otherForce) {
					getSurvivalBonusIds().add(UUID.fromString(en.getExternalIdAsString()));
				}
			} else {
				BotForce bf = new BotForce("Civilians", 2, otherStart, enemyStart, otherForce);
				bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
				
				addBotForce(bf);
			}
		} catch (PrincessException e) {
			e.printStackTrace();
		}
	}
}
