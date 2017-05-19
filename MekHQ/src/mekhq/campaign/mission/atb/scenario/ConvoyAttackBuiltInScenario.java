package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ConvoyAttackBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 8487647534085152088L;

	@Override
	public int getScenarioType() {
		return CONVOYATTACK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Convoy Attack";
	}

	@Override
	public int getMapX() {
		return 45;
	}

	@Override
	public int getMapY() {
		return 65;
	}

	@Override
	public void setMapFile() {
		setMap("Convoy");
		setTerrainType(TER_WOODED);
	}

	@Override
	public boolean canRerollMapSize() {
		return false;
	}

	@Override
	public boolean canRerollMap() {
		return false;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(Board.START_S);

		for (int i = 0; i < 4; i++) {
			getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
					getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
					EntityWeightClass.WEIGHT_LIGHT, campaign));
		}

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 12, campaign);
		addBotForce(new BotForce("Convoy", 2, Board.START_CENTER, otherForce));

		for (int i = 0; i < 8; i++) {
			enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
					getContract(campaign).getEnemyQuality(), UnitType.MEK,
					UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
							campaign.getCampaignOptions().getRegionalMechVariations()),
					campaign));
		}

		addBotForce(getEnemyBotForce(getContract(campaign), Board.START_CENTER, enemyEntities));
	}
}
