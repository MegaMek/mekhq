package mekhq.campaign.mission.atb;

import megamek.client.RandomSkillsGenerator;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.HitData;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.universe.Faction;

public class AtBScenarioModifierApplicator {
    
    /**
     * Adds the given force to the given scenario at the appropriate point in time.
     * @param campaign
     * @param scenario
     * @param forceToApply
     * @param eventTiming
     */
    public static void addForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate forceToApply, EventTiming eventTiming) {
        if(eventTiming == EventTiming.PostForceGeneration) {
            postAddForce(campaign, scenario, forceToApply);
        } else if (eventTiming == EventTiming.PreForceGeneration) { 
            preAddForce(campaign, scenario, forceToApply);
        }
    }
    
    /**
     * Adds the given force to the scenario after primary forces have been generated.
     * @param campaign
     * @param scenario
     * @param forceToApply
     */
    private static void postAddForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate forceToApply) {
        int effectiveBV = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign);
        int effectiveUnitCount = AtBDynamicScenarioFactory.calculateEffectiveUnitCount(scenario, campaign);
        
        AtBDynamicScenarioFactory.generateForce(scenario, scenario.getContract(campaign), campaign, 
                effectiveBV, effectiveUnitCount, EntityWeightClass.WEIGHT_ASSAULT, forceToApply);
    }
    
    /**
     * Adds the given force to the scenario template prior to primary force generation.
     * @param campaign
     * @param scenario
     * @param forceToApply
     */
    private static void preAddForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate forceToApply) {
        if(scenario.getTemplate() != null) {
            scenario.getTemplate().scenarioForces.put(forceToApply.getForceName(), forceToApply);
        }
    }
    
    /**
     * Worker function that removes the number of units from the specified side.
     * @param scenario
     * @param campaign
     */
    public static void removeUnits(AtBDynamicScenario scenario, Campaign campaign, ForceAlignment eventRecipient, int unitRemovalCount) {
        int actualUnitsToRemove = unitRemovalCount;
        
        if(unitRemovalCount == ScenarioForceTemplate.FIXED_UNIT_SIZE_LANCE) {
            String factionCode;
            
            if(eventRecipient == ForceAlignment.Allied) {
                factionCode = scenario.getContract(campaign).getEmployerCode();
            } else {
                factionCode = scenario.getContract(campaign).getEnemyCode();
            }
            
            actualUnitsToRemove = AtBDynamicScenarioFactory.getLanceSize(factionCode);
        }
        
        for(int x = 0; x < actualUnitsToRemove; x++) {
            int botForceIndex = Compute.randomInt(scenario.getNumBots());
            BotForce bf = scenario.getBotForce(botForceIndex);
            if(bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.size()) {
                int unitIndexToRemove = Compute.randomInt(bf.getEntityList().size()); 
                bf.getEntityList().remove(unitIndexToRemove);
            }
        }
    }
    
    /**
     * Worker function that inflicts battle damage on all units belonging to the side specified in this modifier.
     * May theoretically result in a crippled or destroyed unit (?)
     * @param scenario
     * @param campaign
     */
    public static void inflictBattleDamage(AtBDynamicScenario scenario, Campaign campaign, 
            ForceAlignment eventRecipient, int battleDamageIntensity) {
        // now go through all the entities belonging to the recipient currently in the scenario
        // and apply random battle damage
        for(int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce bf = scenario.getBotForce(botIndex);
            if(bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for(Entity en : bf.getEntityList()) {
                    int numClusters = Compute.randomInt(battleDamageIntensity);
                    
                    for(int clusterCount = 0; clusterCount < numClusters; clusterCount++) {
                        HitData hitData = en.rollHitLocation(ToHitData.HIT_NORMAL, Compute.randomInt(4));
                        int resultingArmor = en.getArmor(hitData) - 5;
                        
                        if(resultingArmor >= 0) {
                            en.setArmor(resultingArmor, hitData);
                        } else {
                            en.setArmor(0, hitData);
                            
                            // the resulting armor is less than 0 at this point, so we subtract it from the internal structure
                            // for sanity's sake, we don't inflict crits, and don't transfer damage inwards
                            int resultingStructure = Math.max(0, en.getInternal(hitData) + resultingArmor);
                            en.setInternal(resultingStructure, hitData);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Worker function that expends ammo from all units belonging to the side specified in this modifier.
     * @param scenario
     * @param campaign
     */
    public static void expendAmmo(AtBDynamicScenario scenario, Campaign campaign, 
            ForceAlignment eventRecipient, int ammoExpenditureIntensity) {
        // now go through all the entities belonging to the recipient currently in the scenario
        // and remove a random amount of ammo from each bin
        for(int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce bf = scenario.getBotForce(botIndex);
            if(bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for(Entity en : bf.getEntityList()) {
                    for(Mounted ammoBin : en.getAmmo()) {
                        int remainingShots = Math.max(0, ammoBin.getUsableShotsLeft() - Compute.randomInt(ammoExpenditureIntensity));
                        ammoBin.setShotsLeft(remainingShots);
                    }
                }
            }
        }
    }
    
    /**
     * Helper function that re-generates skill levels for all existing units in the scenario
     * @param scenario
     * @param campaign
     */
    public static void adjustSkill(AtBDynamicScenario scenario, Campaign campaign,
            ForceAlignment eventRecipient, int skillAdjustment) {
        // we want a number between 0 (which indicates "Green") and 3 ("Elite"), inclusive. Anything outside those bounds is
        // meaningless within the context of the skill generator
        int adjustedSkill = Math.min(RandomSkillsGenerator.L_ELITE, 
                Math.max(RandomSkillsGenerator.L_GREEN, scenario.getEffectiveOpforSkill() + skillAdjustment));
        
        // fire up a skill generator set to the appropriate skill model
        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(adjustedSkill);

        if (Faction.getFaction(scenario.getContract(campaign).getEnemyCode()).isClan()) {
            rsg.setType(RandomSkillsGenerator.T_CLAN);
        }
        
        // now go through all the opfor entities currently in the scenario
        // and re-generate their 
        for(int x = 0; x < scenario.getNumBots(); x++) {
            BotForce bf = scenario.getBotForce(x);
            if(bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for(Entity en : bf.getEntityList()) {
                    int[] skills = rsg.getRandomSkills(en);
                    
                    en.getCrew().setGunnery(skills[0]);
                    en.getCrew().setPiloting(skills[1]);
                }
            }
        }
    }
    
    public static void appendScenarioBriefingText(AtBDynamicScenario scenario, String additionalBriefingText) {
        scenario.setDesc(String.format("%s\n\n%s", scenario.getDescription(), additionalBriefingText));
    }
    
}
