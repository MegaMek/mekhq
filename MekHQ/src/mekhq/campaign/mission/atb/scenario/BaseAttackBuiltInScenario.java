package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class BaseAttackBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = -873528230365616996L;

	@Override
	public int getScenarioType() {
		return BASEATTACK;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Base Attack";
	}

	@Override
	public void setTerrain() {
		setTerrainType((Compute.d6() < 4) ? TER_LIGHTURBAN : TER_HEAVYURBAN);
	}

	@Override
	public int getMapX() {
		return getBaseMapX() + 10;
	}

	@Override
	public int getMapY() {
		return getBaseMapY() + 10;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int enemyStart;
		int playerHome;

		if (isAttacker()) {
			playerHome = startPos[Compute.randomInt(4)];
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(playerHome + 4);

			if (getEnemyHome() > 8) {
				setEnemyHome(getEnemyHome() - 8);
				;
			}
		} else {
			setStart(Board.START_CENTER);
			enemyStart = startPos[Compute.randomInt(4)];
			setEnemyHome(enemyStart);

			playerHome = getEnemyHome() + 4;

			if (playerHome > 8) {
				playerHome -= 8;
			}
		}

		/*
		 * Ally deploys 2 lances of a lighter weight class than the player,
		 * minium light
		 */
		int allyForce = Math.max(getLance(campaign).getWeightClass(campaign) - 1, EntityWeightClass.WEIGHT_LIGHT);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForce, campaign);
		addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
				getContract(campaign).getAllyQuality(), allyForce, campaign);

		addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));

		/* Roll 2x on bot lances roll */
		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 10, campaign);
		addBotForce(new BotForce("Civilians", isAttacker() ? 2 : 1, isAttacker() ? getEnemyHome() : playerHome,
				isAttacker() ? enemyStart : getStart(), otherForce));

		for (int i = 0; i < 6; i++) {
			if (isAttacker()) {
				enemyEntities.add(this.getEntityByName(randomGunEmplacement(), getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), campaign));
			} else {
				allyEntities.add(this.getEntityByName(randomGunEmplacement(), getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), campaign));
			}
		}
	}

	/* From chart provided by Makinus */
	private String randomGunEmplacement() {
		boolean dual = false;
		int roll = Compute.randomInt(20) + 1;
		if (roll >= 19) {
			dual = true;
			roll = Compute.randomInt(18) + 1;
		}
		if (roll < 4) {
			return dual ? "AC Turret (Dual) AC2" : "AC Turret AC2";
		}
		if (roll < 7) {
			return dual ? "AC Turret (Dual) AC5" : "AC Turret AC5";
		}
		if (roll == 7) {
			return dual ? "AC Turret (Dual) AC10" : "AC Turret AC10";
		}
		if (roll == 8) {
			return dual ? "SRM Turret (Dual) SRM2" : "SRM Turret SRM2";
		}
		if (roll == 9) {
			return dual ? "SRM Turret (Dual) SRM4" : "SRM Turret SRM4";
		}
		if (roll == 10) {
			return dual ? "SRM Turret (Dual) SRM6" : "SRM Turret SRM6";
		}
		if (roll < 13) {
			return dual ? "LRM Turret (Dual) LRM5" : "LRM Turret LRM5";
		}
		if (roll == 13) {
			return dual ? "LRM Turret (Dual) LRM10" : "LRM Turret LRM10";
		}
		if (roll == 14) {
			return dual ? "LRM Turret (Dual) LRM15" : "LRM Turret LRM15";
		}
		if (roll == 15) {
			return dual ? "LRM Turret (Dual) LRM20" : "LRM Turret LRM20";
		}
		if (roll == 16) {
			return dual ? "Laser Turret (Dual) SL" : "Laser Turret SL";
		}
		if (roll == 17) {
			return dual ? "Laser Turret (Dual) ML" : "Laser Turret ML";
		}
		return dual ? "Laser Turret (Dual) LL" : "Laser Turret LL";
	}
}
