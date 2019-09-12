package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
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
public class ExtractionBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 2669891555728754709L;
	
	private static final String CIVILIAN_FORCE_ID = "Civilians";

	@Override
	public int getScenarioType() {
		return EXTRACTION;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Extraction";
	}

	@Override
	public String getResourceKey() {
		return "extraction";
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		int enemyStart;
		int otherStart;
		int otherHome;
		int playerHome;
		
		if (isAttacker()) {
			playerHome = startPos[Compute.randomInt(4)];
			setStart(playerHome);

			enemyStart = Board.START_CENTER;
			setEnemyHome(playerHome + 4);

			if (getEnemyHome() > 8) {
				setEnemyHome(getEnemyHome() - 8);
			}

			otherStart = getStart() + 4;
			otherHome = playerHome;
		} else {
			setStart(Board.START_CENTER);
			enemyStart = startPos[Compute.randomInt(4)];

			setEnemyHome(enemyStart);
			playerHome = getEnemyHome() + 4;

			if (playerHome > 8) {
				playerHome -= 8;
			}

			otherStart = enemyStart + 4;
			otherHome = enemyStart;
		}
		if (otherStart > 8) {
			otherStart -= 8;
		}

		if (allyEntities.size() > 0) {
			addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
		}

		addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 4, campaign);

		try {
			if (isAttacker()) {
				BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 1, otherStart, playerHome, otherForce);
				bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
				bf.setDestinationEdge(otherHome);
				
				addBotForce(bf);
				
				for (Entity en : otherForce) {
					getSurvivalBonusIds().add(UUID.fromString(en.getExternalIdAsString()));
				}
			} else {
				BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 2, otherStart, enemyStart, otherForce);
				bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
				bf.setDestinationEdge(otherHome);
				
				addBotForce(bf);
			}
		} catch (PrincessException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);
        
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 50, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract, this);
        ScenarioObjective destroyHostiles = null;
        ScenarioObjective civilianObjective;
        
        
        if(isAttacker()) {
            civilianObjective = CommonObjectiveFactory.getPreserveSpecificFriendlies(CIVILIAN_FORCE_ID, 66, false);
            
            // not losing the scenario also gets you a "bonus"
            ObjectiveEffect bonusEffect = new ObjectiveEffect();
            bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
            bonusEffect.effectScaling = EffectScalingType.Linear;
            bonusEffect.howMuch = 1;
            civilianObjective.addSuccessEffect(bonusEffect);
            civilianObjective.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"), bonusEffect.howMuch));
        } else {
            civilianObjective = CommonObjectiveFactory.getDestroyEnemies(CIVILIAN_FORCE_ID, 100);
            civilianObjective.setTimeLimit(10);
            civilianObjective.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.timeLimit.text"), civilianObjective.getTimeLimit()));
            destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 33);
            destroyHostiles.setTimeLimit(10);
            destroyHostiles.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.timeLimit.text"), destroyHostiles.getTimeLimit()));
        }
        
        if(destroyHostiles != null) {       
            getObjectives().add(destroyHostiles);
        }
        
        if(keepAttachedUnitsAlive != null) {
            getObjectives().add(keepAttachedUnitsAlive);
        }
        
        getObjectives().add(keepFriendliesAlive);
        getObjectives().add(civilianObjective);
    }
}
