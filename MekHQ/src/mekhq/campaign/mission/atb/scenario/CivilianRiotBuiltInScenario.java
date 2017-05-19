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
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class CivilianRiotBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 6943760300767790979L;

	@Override
	public boolean isBigBattle() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return CIVILIANRIOT;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Big Battle: Civilian Riot";
	}

	@Override
	public String getResourceKey() {
		return "civilianRiot";
	}

	@Override
	public int getMapX() {
		return 65;
	}

	@Override
	public int getMapY() {
		return 65;
	}
	
	@Override
	public boolean canRerollMapSize() {
		return false;
	}
	
	@Override
	public boolean canDeploy(Unit unit, Campaign campaign) {
        for (Part p : unit.getParts()) {
            if (p instanceof EquipmentPart) {
                for (String weapon : antiRiotWeapons) {
                    if (((EquipmentPart)p).getType().getInternalName().equals(weapon)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
        setStart(Board.START_S);
        
        //TODO: only units with machine guns, flamers, or sm lasers
        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK,
                    (Compute.randomInt(7) < 3)?EntityWeightClass.WEIGHT_LIGHT:EntityWeightClass.WEIGHT_MEDIUM,
                    campaign));
        }

        ArrayList<Entity>otherForce = new ArrayList<Entity>();
        addCivilianUnits(otherForce, 8, campaign);
        
        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }
        
        addBotForce(new BotForce("Loyalists", 1, Board.START_CENTER, otherForce));

        otherForce = new ArrayList<Entity>();
        addCivilianUnits(otherForce, 12, campaign);
        addBotForce(new BotForce("Rioters", 2, Board.START_CENTER, otherForce));

        for (int i = 0; i < 3; i++) {
            //3 mech rebel lance, use employer RAT, enemy skill
            enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getEnemySkill(), IUnitRating.DRAGOON_F,
                    UnitType.MEK,
                    Compute.d6() < 4?EntityWeightClass.WEIGHT_LIGHT:EntityWeightClass.WEIGHT_MEDIUM,
                    campaign));
        }
        
        addBotForce(new BotForce("Rebels", 2, Board.START_N, enemyEntities));
	}
}
