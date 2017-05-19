package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class AllyRescueBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 6993274905243245321L;

	@Override
	public boolean isBigBattle() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return ALLYRESCUE;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Ally Rescue";
	}

	@Override
	public String getResourceKey() {
		return "allyRescue";
	}

	@Override
	public int getMapX() {
		return 65;
	}

	@Override
	public int getMapY() {
		return 45;
	}

	@Override
	public void setMapFile() {
		setMap("Ally-rescue");
		setTerrainType(TER_LIGHTURBAN);
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
		setDeploymentDelay(12);
		
		for (int i = 0; i < 4; i++) {
			getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
					getContract(campaign).getAllyQuality(), UnitType.MEK,
					UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEmployerCode(),
							campaign.getCampaignOptions().getRegionalMechVariations()),
					campaign));
		}

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		
		for (int i = 0; i < 8; i++) {
			otherForce.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
					getContract(campaign).getAllyQuality(), UnitType.MEK, UnitMarket.getRandomAeroWeight(), // max
																											// heavy
					campaign));
		}
		
		addBotForce(new BotForce(getContract(campaign).getAllyBotName(), 1, Board.START_CENTER, otherForce));

		for (int i = 0; i < 12; i++) {
			enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
					getContract(campaign).getEnemyQuality(), UnitType.MEK, UnitMarket.getRandomAeroWeight() + 1, // no
																													// light
																													// 'Mechs
					campaign));
		}
		
		addBotForce(getEnemyBotForce(getContract(campaign), Board.START_N, enemyEntities));
	}
}
