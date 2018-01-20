package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.client.RandomSkillsGenerator;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;

@AtBScenarioEnabled
public class PirateFreeForAllBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 6410090692095923096L;

	@Override
	public boolean isBigBattle() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return PIRATEFREEFORALL;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Pirates Free-for-All";
	}

	@Override
	public String getResourceKey() {
		return "pirateFreeForAll";
	}

	@Override
	public int getMapX() {
		return 50;
	}

	@Override
	public int getMapY() {
		return 50;
	}

	@Override
    public boolean canRerollMapSize() {
        return false;
    }

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
        setStart(Board.START_CENTER);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, UnitMarket.getRandomAeroWeight(), // max heavy
                    campaign));
        }

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(),
                    getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                    UnitType.MEK,
                    UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
                            campaign.getCampaignOptions().getRegionalMechVariations()),
                    campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_N, enemyEntities));

        ArrayList<Entity>otherForce = new ArrayList<Entity>();

        for (int i = 0; i < 12; i++) {
            otherForce.add(getEntity("PIR",
                            RandomSkillsGenerator.L_REG, IUnitRating.DRAGOON_C,
                            UnitType.MEK,
                            UnitMarket.getRandomMechWeight(),
                            campaign));
        }

        addBotForce(new BotForce("Pirates", 3, Board.START_S, otherForce));
	}
}
