package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.PlanetaryConditions;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class OfficerDualBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -1868853521323602877L;

	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return OFFICERDUEL;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Officer Duel";
	}

	@Override
	public void setLightConditions() {
		setLight(PlanetaryConditions.L_DAY);
	}

	@Override
	public void setWeather() {
		setWeather(PlanetaryConditions.WE_NONE);
		setWind(PlanetaryConditions.WI_NONE);
		setFog(PlanetaryConditions.FOG_NONE);
	}

	@Override
	public void setMapFile() {
		setMap("Savannah");
		setTerrainType(TER_FLATLANDS);
	}

	@Override
	public boolean canRerollMap() {
		return false;
	}

	@Override
	public boolean canRerollLight() {
		return false;
	}

	@Override
	public boolean canRerollWeather() {
		return false;
	}

	@Override
	public boolean canDeploy(Unit unit, Campaign campaign) {
		return unit.getCommander().getRank().isOfficer();
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(startPos[Compute.randomInt(4)]);
		int enemyStart = getStart() + 4;

		if (enemyStart > 8) {
			enemyStart -= 8;
		}

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
			Entity en = getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
					getContract(campaign).getEnemyQuality(), UnitType.MEK,
					Math.min(weight + 1, EntityWeightClass.WEIGHT_ASSAULT), campaign);
			if (weight == EntityWeightClass.WEIGHT_ASSAULT) {
				en.getCrew().setGunnery(en.getCrew().getGunnery() - 1);
				en.getCrew().setPiloting(en.getCrew().getPiloting() - 1);
			}

			enemyEntities.add(en);
			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));
	}
}
