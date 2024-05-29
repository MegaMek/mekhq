package mekhq.campaign.personnel.turnoverAndRetention.Morale;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.dialog.moraleDialogs.TransitMutinyDialog;
import mekhq.gui.dialog.moraleDialogs.TransitMutinyToe;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mekhq.campaign.personnel.turnoverAndRetention.Morale.Desertion.processDesertion;
import static mekhq.gui.dialog.moraleDialogs.TransitMutinyBattleDialog.transitMutinyBattleConditionDialog;

public class Mutiny {
    static void processMutiny(Campaign campaign, List<Person> loyalists, HashMap<Person, Integer> mutineers, int targetNumber, ResourceBundle resources) {
        Random random = new Random();

        if (campaign.getCampaignOptions().getMutinyMethod().isCampaignOperations()) {
            processCamOpsMutiny(campaign, random, loyalists, mutineers, targetNumber, resources);
        } else {
            // TODO this is where Advanced Mutinies call goes
            return;
        }
    }

    static void processCamOpsMutiny(Campaign campaign, Random random, List<Person> loyalists, HashMap<Person, Integer> mutineers, int targetNumber, ResourceBundle resources) {
        // if the mutineers are out-numbered 4:1 (or worse), they're treated as if having deserted
        if ((loyalists.size() / 4) >= mutineers.keySet().size()) {
            if (mutineers.keySet().size() == 1) {
                campaign.addReport(resources.getString("mutinyThwartedSingular.text"));
            } else {
                campaign.addReport(String.format(resources.getString("mutinyThwartedPlural.text"), mutineers.keySet().size()));
            }

            processDesertion(campaign, mutineers, targetNumber, resources);

            return;
        }

        // next we pick the mutiny & loyalist leaders.
        // CamOps doesn't have any special handling for unit leader, so we don't either
        Person mutineerLeader = campaign.getHighestRankedPerson(new ArrayList<>(mutineers.keySet()), true);
        Person loyalistLeader = campaign.getHighestRankedPerson(loyalists, true);

        // this dialog alerts the player to the mutiny
        TransitMutinyDialog.transitMutinyOnsetDialog(resources);

        // this dialog tells the player about the array of forces and gives them the option to support the mutineers or loyalists

        if (campaign.getLocation().isOnPlanet()) {
            processAbstractBattle(campaign, random, resources, loyalistLeader, loyalists, mutineerLeader, mutineers);
        }


    }

    private static void processAbstractBattle(Campaign campaign, Random random, ResourceBundle resources,
                                              Person loyalistLeader, List<Person> loyalists,
                                              Person mutineerLeader, HashMap<Person, Integer> mutineers) {
        HashMap<String, Integer> loyalistBattlePower = getAbstractBattlePower(loyalistLeader, loyalists);
        HashMap<String, Integer> mutinyBattlePower = getAbstractBattlePower(mutineerLeader, new ArrayList<>(mutineers.keySet()));


        int support = TransitMutinyToe.transitMutinyToeDialog(resources,
                loyalistLeader, loyalists.size(), loyalistBattlePower.get("attack"), loyalistBattlePower.get("defense"),
                mutineerLeader, mutineers.keySet().size(), mutinyBattlePower.get("attack"), mutinyBattlePower.get("defense"));

        int victor = processAbstractBattleRound(campaign, random, loyalists, loyalistBattlePower, new ArrayList<>(mutineers.keySet()), mutinyBattlePower);

        transitMutinyBattleConditionDialog(resources, random);
    }

    private static HashMap<String, Integer> getAbstractBattlePower(Person loyalistLeader, List<Person> loyalists) {
        HashMap<String, Integer> battlePower = new HashMap<>();

        int attackPower = 0;
        int defensePower = 0;

        for (Person person : loyalists) {
            if (person.hasSkill(SkillType.S_SMALL_ARMS)) {
                attackPower += person.getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() / 2;
                defensePower += person.getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_ADMIN)) {
                defensePower += person.getSkill(SkillType.S_ADMIN).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_DOCTOR)) {
                defensePower += person.getSkill(SkillType.S_DOCTOR).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_TACTICS)) {
                attackPower += person.getSkill(SkillType.S_TACTICS).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_STRATEGY)) {
                attackPower += person.getSkill(SkillType.S_STRATEGY).getExperienceLevel() / 2;
            }
        }

        if (loyalistLeader.hasSkill(SkillType.S_LEADER)) {
            attackPower += loyalistLeader.getSkill(SkillType.S_LEADER).getExperienceLevel() / 2;
            defensePower += loyalistLeader.getSkill(SkillType.S_LEADER).getExperienceLevel() / 2;
        }

        battlePower.put("attack", attackPower);
        battlePower.put("defense", defensePower);

        return battlePower;
    }

    public static int processAbstractBattleRound(Campaign campaign, Random random,
                                                 List<Person> loyalists, HashMap<String, Integer> loyalistBattlePower,
                                                 List<Person> mutineers, HashMap<String, Integer> mutinyBattlePower) {

        // the loyalist side of the battle (ten rounds of combat)
        int hits = 0;
        int combatRound = 0;

        while (combatRound < 10) {
            hits += combatRound(loyalistBattlePower.get("attack"), mutinyBattlePower.get("defense"));
            combatRound++;
        }

        distributeHits(campaign, mutineers, hits, random);

        List<Person> mutineerGraveyard = fillGraveyard(campaign, mutineers);

        for (Person person : mutineerGraveyard) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
        }

        mutineers.removeAll(mutineerGraveyard);

        // the mutineer side of the battle (ten rounds of combat)
        hits = 0;
        combatRound = 0;

        while (combatRound < 10) {
            hits += combatRound(mutinyBattlePower.get("attack"), loyalistBattlePower.get("defense"));
            combatRound++;
        }

        distributeHits(campaign, loyalists, hits, random);

        List<Person> loyalistsGraveyard = fillGraveyard(campaign, loyalists);

        for (Person person : loyalistsGraveyard) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
        }

        loyalists.removeAll(loyalistsGraveyard);

        // calculate results
        if ((mutineers.isEmpty()) && (loyalists.isEmpty())) {
            // they wiped each other out
            return -1;
        }

        if (mutineerGraveyard.size() == loyalistsGraveyard.size()) {
            // if the battle is drawn, we open another instance of the fight
            return processAbstractBattleRound(campaign, random,
                    loyalists, loyalistBattlePower,
                    mutineers, mutinyBattlePower);
        } else if (loyalistsGraveyard.size() > mutineerGraveyard.size()) {
            // loyalist victory
            return 0;
        } else {
            // mutineer victory
            return 1;
        }
    }

    private static List<Person> fillGraveyard(Campaign campaign, List<Person> personnel) {
        List<Person> graveyard;

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            graveyard = personnel.stream()
                    .filter(person -> person.getInjuries().size() > 5)
                    .collect(Collectors.toList());
        } else {
            graveyard = personnel.stream()
                    .filter(person -> person.getHits() > 5)
                    .collect(Collectors.toList());
        }
        return graveyard;
    }

    private static void distributeHits(Campaign campaign, List<Person> potentialVictims, int hits, Random random) {
        for (int hit = 0; hit < hits; hit++) {
            boolean victimFound = false;
            int maxAttempts = 3;

            Person victim = null;

            while ((!victimFound) || (maxAttempts == 0)) {
                victim = potentialVictims.get(random.nextInt(potentialVictims.size()));

                if ((campaign.getCampaignOptions().isUseAdvancedMedical()) && (victim.getInjuries().size() < 6)) {
                    victimFound = true;
                } else if (victim.getHits() < 6) {
                    victimFound = true;
                }

                // this prevents an infinite loop from occurring when all combatants are dead
                maxAttempts--;
            }

            int injuryCount = MathUtility.clamp(Compute.randomInt(4) + Compute.randomInt(4) + 2, 2, 5);

            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                InjuryUtil.resolveCombatDamage(campaign, victim, injuryCount);
                victim.setHits(victim.getHits() + injuryCount);
            } else {
                victim.setHits(victim.getHits() + injuryCount);
            }
        }
    }

    private static int combatRound(int attackerAttackPower, int defenderDefensePower) {
        int attackerHits = 0;
        int attackerCriticalHits = 0;

        for (int attackRoll = 0; attackRoll < attackerAttackPower; attackRoll++) {
            int roll = Compute.d6(1);

            if (roll == 6) {
                attackerCriticalHits++;
            } else if (roll >= 4) {
                attackerHits++;
            }
        }

        int defenderDefense = (int) IntStream.range(0, defenderDefensePower)
                .filter(defenseRoll -> Compute.d6(1) == 6)
                .count();

        return Math.max(0, (attackerHits - defenderDefense)) + attackerCriticalHits;
    }
}
