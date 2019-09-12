package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ConvoyAttackBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 8487647534085152088L;

	private static String CONVOY_FORCE_ID = "Convoy";
	
	@Override
	public boolean isBigBattle() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return CONVOYATTACK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Convoy Attack";
	}

	@Override
	public String getResourceKey() {
		return "convoyAttack";
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
		addBotForce(new BotForce(CONVOY_FORCE_ID, 2, Board.START_CENTER, otherForce));

		for (int i = 0; i < 8; i++) {
			enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
					getContract(campaign).getEnemyQuality(), UnitType.MEK,
					UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
							campaign.getCampaignOptions().getRegionalMechVariations()),
					campaign));
		}

		addBotForce(getEnemyBotForce(getContract(campaign), Board.START_CENTER, enemyEntities));
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);
        
        ScenarioObjective destroyConvoy = CommonObjectiveFactory.getDestroyEnemies(CONVOY_FORCE_ID, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 50, false);
        
        getObjectives().add(destroyConvoy);
        getObjectives().add(keepFriendliesAlive);
    }
}
