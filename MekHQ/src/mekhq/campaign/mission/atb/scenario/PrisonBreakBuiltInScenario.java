package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class PrisonBreakBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -2079887460549545045L;
	
	private static String GUARD_FORCE_ID = "Guards";
	private static String PRISONER_FORCE_ID = "Prisoners";

	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return PRISONBREAK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Prison Break";
	}

	@Override
	public String getResourceKey() {
		return "prisonBreak";
	}

	@Override
	public int getMapX() {
		return 20;
	}

	@Override
	public int getMapY() {
		return 30;
	}

	@Override
	public boolean canRerollMapSize() {
		return false;
	}

	@Override
	public boolean canDeploy(Unit unit, Campaign campaign) {
		return unit.getEntity().getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(Board.START_CENTER);
		int enemyStart = startPos[Compute.randomInt(4)];

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
			enemyEntities = new ArrayList<Entity>();
			for (int i = 0; i < 3; i++)
				enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
						getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(new BotForce(GUARD_FORCE_ID, 2, enemyStart, getSpecMissionEnemies().get(0)));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();

		addCivilianUnits(otherForce, 4, campaign);

		for (Entity e : otherForce) {
			getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
		}
		
		addBotForce(new BotForce(PRISONER_FORCE_ID, 1, getStart(), otherForce));
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);
        
        //ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 66);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 1, true);
        ScenarioObjective keepPrisonersAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(PRISONER_FORCE_ID, 1, true);
        
        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.scaledEffect = true;
        bonusEffect.howMuch = 1;
        keepPrisonersAlive.addSuccessEffect(bonusEffect);
        keepPrisonersAlive.addDetail("1 bonus roll per surviving unit");
        
        //getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
        getObjectives().add(keepPrisonersAlive);
    }
}
