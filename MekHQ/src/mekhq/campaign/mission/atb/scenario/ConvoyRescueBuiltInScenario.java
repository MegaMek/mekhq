package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

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
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ConvoyRescueBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 8487647534085152088L;
	
	private static String CONVOY_FORCE_ID = "Convoy";

	@Override
	public boolean isBigBattle() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return CONVOYRESCUE;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Convoy Rescue";
	}

	@Override
	public String getResourceKey() {
		return "convoyRescue";
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
        setStart(Board.START_N);
        setDeploymentDelay(7);
        
        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, EntityWeightClass.WEIGHT_LIGHT, campaign));
        }

        ArrayList<Entity> otherForce = new ArrayList<Entity>();
        addCivilianUnits(otherForce, 12, campaign);
        
        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }
        
        addBotForce(new BotForce(CONVOY_FORCE_ID, 1, Board.START_CENTER, otherForce));

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(),
                    getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                    UnitType.MEK,
                    UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
                            campaign.getCampaignOptions().getRegionalMechVariations()),
                    campaign));
        }
        
        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_S, enemyEntities));
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);
        
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 50, false);
        ScenarioObjective keepConvoyAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(CONVOY_FORCE_ID, 1, true);
        keepConvoyAlive.addDetail("(1 bonus roll per surviving unit)");
        
        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepConvoyAlive.addSuccessEffect(bonusEffect);
        
        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
        getObjectives().add(keepConvoyAlive);
    }
}
