package mekhq.campaign.mission.atb.scenario;

import megamek.client.RandomSkillsGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;

import java.util.ArrayList;

@AtBScenarioEnabled
public class StarLeagueCache1BuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 1994382390878571793L;

	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return STARLEAGUECACHE1;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Star League Cache 1";
	}

	@Override
	public String getResourceKey() {
		return "starLeagueCache1";
	}

	@Override
	public int getMapX() {
		return 20;
	}

	@Override
	public int getMapY() {
		return 35;
	}

	@Override
	public void setMapFile() {
		setMap("Brian-cache");
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
		setStart(Board.START_CENTER);
		int enemyStart = Board.START_N;

		int roll = Compute.d6();
		/* Only has enemy if SL 'Mech is not primitive */
		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
			if (roll > 1) {
			    enemyEntities = new ArrayList<Entity>();
				for (int i = 0; i < 3; i++) {
					enemyEntities
							.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
									getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
				}
			}

			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		MechSummary ms = null;

		if (roll == 1) {
			RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits_PrimMech");
			ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
			if (msl.size() > 0) {
				ms = msl.get(0);
			}
		} else {
			ms = campaign.getUnitGenerator().generate("SL", UnitType.MEK, UnitMarket.getRandomMechWeight(), 2750,
					(roll == 6) ? IUnitRating.DRAGOON_A : IUnitRating.DRAGOON_D);
		}
		Entity en = (ms == null) ? null
				: createEntityWithCrew(campaign.getFactionCode(), RandomSkillsGenerator.L_GREEN, campaign, ms);
		otherForce.add(en);

		// TODO: During SW offer a choice between an employer exchange or a
		// contract breach
		Loot loot = new Loot();
		loot.setName("Star League Mek");
		loot.addUnit(en);
		getLoot().add(loot);
		addBotForce(new BotForce("Tech", 1, getStart(), otherForce));
	}
}
