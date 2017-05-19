package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class PrisonBreakBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -2079887460549545045L;

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

		addBotForce(new BotForce("Guards", 2, enemyStart, getSpecMissionEnemies().get(0)));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();

		addCivilianUnits(otherForce, 4, campaign);

		for (Entity e : otherForce) {
			getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
		}
		
		addBotForce(new BotForce("Prisoners", 1, getStart(), otherForce));
	}
}
