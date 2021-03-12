/*
 * Copyright (c) 2017, 2020 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.atb;

import java.time.LocalDate;
import java.util.*;

import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.scenario.AceDuelBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.AlliedTraitorsBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.AllyRescueBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.AmbushBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.BaseAttackBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.BreakthroughBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ChaseBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.CivilianHelpBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.CivilianRiotBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ConvoyAttackBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ConvoyRescueBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ExtractionBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.HideAndSeekBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.HoldTheLineBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.OfficerDualBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.PirateFreeForAllBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.PrisonBreakBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ProbeBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.ReconRaidBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.StandUpBuiltInScenario;
import mekhq.campaign.mission.atb.scenario.StarLeagueCache1BuiltInScenario;
import mekhq.campaign.mission.atb.scenario.StarLeagueCache2BuiltInScenario;

public class AtBScenarioFactory {
    private static Map<Integer, List<Class<IAtBScenario>>> scenarioMap = new HashMap<>();

    static {
        registerScenario(new AceDuelBuiltInScenario());
        registerScenario(new AlliedTraitorsBuiltInScenario());
        registerScenario(new AllyRescueBuiltInScenario());
        registerScenario(new AmbushBuiltInScenario());
        registerScenario(new BaseAttackBuiltInScenario());
        registerScenario(new BreakthroughBuiltInScenario());
        registerScenario(new ChaseBuiltInScenario());
        registerScenario(new CivilianHelpBuiltInScenario());
        registerScenario(new CivilianRiotBuiltInScenario());
        registerScenario(new ConvoyAttackBuiltInScenario());
        registerScenario(new ConvoyRescueBuiltInScenario());
        registerScenario(new ExtractionBuiltInScenario());
        registerScenario(new HideAndSeekBuiltInScenario());
        registerScenario(new HoldTheLineBuiltInScenario());
        registerScenario(new OfficerDualBuiltInScenario());
        registerScenario(new PirateFreeForAllBuiltInScenario());
        registerScenario(new PrisonBreakBuiltInScenario());
        registerScenario(new ProbeBuiltInScenario());
        registerScenario(new ReconRaidBuiltInScenario());
        registerScenario(new StandUpBuiltInScenario());
        registerScenario(new StarLeagueCache1BuiltInScenario());
        registerScenario(new StarLeagueCache2BuiltInScenario());
    }

    private AtBScenarioFactory() {

    }

    public static List<Class<IAtBScenario>> getScenarios(int type) {
        return scenarioMap.get(type);
    }

    public static AtBScenario createScenario(Campaign c, Lance lance, int type, boolean attacker, LocalDate date) {
        List<Class<IAtBScenario>> classList = getScenarios(type);
        Class<IAtBScenario> selectedClass;

        if ((classList == null) || classList.isEmpty()) {
            return null;
        }

        if (classList.size() > 1) {
            Random randomGenerator = new Random();
            selectedClass = classList.get(randomGenerator.nextInt(classList.size()));
        } else {
            selectedClass = classList.get(0);
        }

        try {
            AtBScenario s = (AtBScenario) selectedClass.newInstance();
            s.initialize(c, lance, attacker, date);

            return s;
        } catch (Exception e) {
            MekHQ.getLogger().error(AtBScenarioFactory.class, e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static void registerScenario(IAtBScenario scenario) {
        if (!scenario.getClass().isAnnotationPresent(AtBScenarioEnabled.class)) {
            MekHQ.getLogger().error(AtBScenarioFactory.class,
                    String.format("Unable to register an AtBScenario of class '%s' because is does not have the '%s' annotation.",
                            scenario.getClass().getName(), AtBScenarioEnabled.class.getName()));
        } else {
            int type = scenario.getScenarioType();
            List<Class<IAtBScenario>> list = scenarioMap.computeIfAbsent(type, k -> new ArrayList<>());

            list.add((Class<IAtBScenario>) scenario.getClass());
        }
    }

    /**
     * Iterate through the list of lances and make a scenario roll for each,
     * then sort them by date before adding them to the campaign.
     * Contracts with enemy morale level of invincible have a base attack
     * (defender) scenario each week. If there is a base attack (attacker)
     * scenario, that is the only one for the week on that contracts.
     *
     * Note that this handles having multiple active contracts at the same time
     * @param c the campaign for which to generate scenarios
     */
    public static void createScenariosForNewWeek(Campaign c) {
        // First, we only want to generate if we have an active contract
        if (!c.hasActiveContract()) {
            return;
        }

        // If we have an active contract, then we can progress with generation
        Hashtable<Integer, Lance> lances = c.getLances();

        AtBContract atbContract;
        List<AtBScenario> sList;
        List<Integer> assignedLances = new ArrayList<>();
        List<Integer> dontGenerateForces;
        boolean hasBaseAttack;
        boolean hasBaseAttackAttacker;

        // We only need to process active contracts
        for (Mission contract : c.getActiveContracts()) {
            if (!(contract instanceof AtBContract) ) {
                continue; //if not an AtBContract, we don't care about the mission
            }

            //region Value Initialization
            atbContract = (AtBContract) contract;
            sList = new ArrayList<>();
            dontGenerateForces = new ArrayList<>();
            hasBaseAttack = false;
            hasBaseAttackAttacker = false;
            //endregion Value Initialization

            //region Current Scenarios
            // Determine active scenarios, to ensure we don't generate a scenario for an already
            // assigned lance and to remove any currently active scenarios from the contract, so that
            // the generation rules are followed for all active scenarios not just new scenarios
            Iterator<Scenario> iterator = atbContract.getScenarios().iterator();
            while (iterator.hasNext()) {
                Scenario scenario = iterator.next();
                if (!scenario.isCurrent() || !AtBScenario.class.isAssignableFrom(scenario.getClass())) {
                    continue; //if not current or not assignable to an AtB scenario, then we don't care about it
                }

                AtBScenario atbScenario = (AtBScenario) scenario;

                // Add any currently assigned lances to the assignedLances
                assignedLances.add(atbScenario.getLanceForceId());

                // Remove any active scenarios from the contract, and add them to the current scenarios list instead
                iterator.remove();
                sList.add(atbScenario);
                dontGenerateForces.add(atbScenario.getId());

                // If we have a current base attack (attacker) scenario, no other scenarios should be generated
                // for that contract
                if ((atbScenario.getScenarioType() == AtBScenario.BASEATTACK)) {
                    hasBaseAttack = true;
                    if (atbScenario.isAttacker()) {
                        hasBaseAttackAttacker = true;
                        break;
                    }
                }
            }
            //endregion Current Scenarios

            //region Generate Scenarios
            // Generate scenarios for lances based on their current situation
            if (!hasBaseAttackAttacker) {
                for (Lance lance : lances.values()) {
                    // Don't generate scenarios for any lances already assigned, those assigned to a
                    // different contract, those not assigned to a contract, or for illegible lances
                    if (assignedLances.contains(lance.getForceId()) || (lance.getContract(c) == null)
                            || !lance.isEligible(c) || (lance.getMissionId() != atbContract.getId())
                            || !lance.getContract(c).isActiveOn(c.getLocalDate(), true)) {
                        continue;
                    }

                    // Assign training experience
                    if (lance.getRole() == AtBLanceRole.TRAINING) {
                        c.awardTrainingXP(lance);
                    }

                    // Don't generate scenarios for contracts with morale below the morale limit
                    if (atbContract.getMoraleLevel() <= AtBContract.MORALE_VERYLOW) {
                        continue;
                    }

                    // Attempt to generate a scenario for the lance
                    AtBScenario atbScenario = lance.checkForBattle(c);

                    // If one is generated, then add it to the scenario list
                    if (atbScenario != null) {
                        sList.add(atbScenario);
                        assignedLances.add(lance.getForceId());

                        // We care if the scenario is a Base Attack, as one must be generated if the
                        // current contract's morale is Invincible
                        if (atbScenario.getScenarioType() == AtBScenario.BASEATTACK) {
                            hasBaseAttack = true;

                            // If a Base Attack (Attacker) scenario is generated, this is the only
                            // scenario that will take place this week for this contract. We can
                            // therefore break out of the loop
                            if (atbScenario.isAttacker()) {
                                hasBaseAttackAttacker = true;
                                break;
                            }
                        }
                    }
                }
            }
            //endregion Generate Scenarios

            //region Invincible Morale Missions
            // Make sure invincible morale missions have a base attack scenario generated
            if (!hasBaseAttack && (atbContract.getMoraleLevel() == AtBContract.MORALE_INVINCIBLE)) {
                /* find a lance to act as defender, giving preference
                 * first to those assigned to the same contract,
                 * then to those assigned to defense roles
                 */
                List<Lance> lList = new ArrayList<>();
                for (Lance l : lances.values()) {
                    if ((l.getMissionId() == atbContract.getId()) && (l.getRole() == AtBLanceRole.DEFENCE)
                            && l.isEligible(c)) {
                        lList.add(l);
                    }
                }
                if (lList.size() == 0) {
                    for (Lance l : lances.values()) {
                        if (l.getMissionId() == atbContract.getId() && l.isEligible(c)) {
                            lList.add(l);
                        }
                    }
                }
                if (lList.size() == 0) {
                    for (Lance l : lances.values()) {
                        if (l.isEligible(c)) {
                            lList.add(l);
                        }
                    }
                }
                if (lList.size() > 0) {
                    Lance lance = Utilities.getRandomItem(lList);
                    AtBScenario atbScenario = AtBScenarioFactory.createScenario(c, lance,
                            AtBScenario.BASEATTACK, false, Lance.getBattleDate(c.getLocalDate()));
                    if (atbScenario != null) {
                        if ((lance.getMissionId() == atbScenario.getMissionId())
                                || (lance.getMissionId() == Lance.NO_MISSION)) {
                            for (int i = 0; i < sList.size(); i++) {
                                if (sList.get(i).getLanceForceId() == lance.getForceId()) {
                                    if (dontGenerateForces.contains(atbScenario.getId())) {
                                        dontGenerateForces.remove(atbScenario.getId());
                                    }
                                    sList.set(i, atbScenario);
                                    break;
                                }
                            }
                        } else {
                            // edge case: lance assigned to another mission gets assigned the scenario,
                            // we need to remove any scenario they are assigned to already
                            for (Scenario scenario : c.getMission(lance.getMissionId()).getScenarios()) {
                                if ((scenario instanceof AtBScenario)
                                        && (((AtBScenario) scenario).getLanceForceId() == lance.getForceId())) {
                                    c.getMission(lance.getMissionId()).removeScenario(scenario.getId());
                                }
                            }
                        }
                        if (!sList.contains(atbScenario)) {
                            sList.add(atbScenario);
                        }
                        if (!assignedLances.contains(lance.getForceId())) {
                            assignedLances.add(lance.getForceId());
                        }
                    } else {
                        MekHQ.getLogger().error(AtBScenarioFactory.class, "Unable to generate Base Attack scenario.");
                    }
                } else {
                    MekHQ.getLogger().warning(AtBScenarioFactory.class, "No lances assigned to mission "
                            + atbContract.getName() + ". Can't generate an Invincible Morale base defence mission for this force.");
                }
            }
            //endregion Invincible Morale Missions

            //region Base Attack (Attacker) Generated
            // If there is a base attack (attacker), it is the only one for this contract until it happens.
            // Therefore, all other currently generated scenarios need to be cleared
            if (hasBaseAttackAttacker) {
                sList.removeIf(atbScenario ->
                        !(atbScenario.isAttacker() && (atbScenario.getScenarioType() == AtBScenario.BASEATTACK)));
            }
            //endregion Base Attack (Attacker) Generated

            //region Add to Campaign
            // Finally, sort the scenarios by date and add to the campaign, and generate forces
            // for the scenario if required
            sList.sort(Comparator.comparing(Scenario::getDate));
            for (AtBScenario atbScenario : sList) {
                c.addScenario(atbScenario, atbContract);
                if (!dontGenerateForces.contains(atbScenario.getId())) {
                    atbScenario.setForces(c);
                }
            }
            //endregion Add to Campaign
        }
    }
}
