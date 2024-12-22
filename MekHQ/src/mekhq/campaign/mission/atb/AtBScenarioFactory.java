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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.atb;

import megamek.codeUtilities.ObjectUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.scenario.*;

import java.time.LocalDate;
import java.util.*;

public class AtBScenarioFactory {
    private static final MMLogger logger = MMLogger.create(AtBScenarioFactory.class);

    private static final Map<Integer, List<Class<IAtBScenario>>> scenarioMap = new HashMap<>();

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
        registerScenario(new OfficerDuelBuiltInScenario());
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

    public static AtBScenario createScenario(Campaign c, CombatTeam lance, int type, boolean attacker, LocalDate date) {
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
            logger.error("", e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static void registerScenario(IAtBScenario scenario) {
        if (!scenario.getClass().isAnnotationPresent(AtBScenarioEnabled.class)) {
            logger.error(String.format(
                    "Unable to register an AtBScenario of class '%s' because is does not have the '%s' annotation.",
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
     * Contracts with enemy morale level of unbreakable have a base attack
     * (defender) scenario each week. If there is a base attack (attacker)
     * scenario, that is the only one for the week on that contracts.
     *
     * Note that this handles having multiple active contracts at the same time
     *
     * @param campaign the campaign for which to generate scenarios
     */
    public static void createScenariosForNewWeek(Campaign campaign) {
        // First, we only want to generate if we have an active contract
        if (!campaign.hasActiveContract()) {
            return;
        }

        // If we have an active contract, then we can progress with generation
        Hashtable<Integer, CombatTeam> combatTeamsTable = campaign.getCombatTeamsTable();

        List<AtBScenario> sList;
        List<Integer> assignedLances = new ArrayList<>();
        List<Integer> dontGenerateForces;
        boolean hasBaseAttack;
        boolean hasBaseAttackAttacker;

        // We only need to process active AtB contracts that haven't hit their end date
        for (final AtBContract contract : campaign.getActiveAtBContracts()) {
            // region Value Initialization
            sList = new ArrayList<>();
            dontGenerateForces = new ArrayList<>();
            hasBaseAttack = false;
            hasBaseAttackAttacker = false;
            // endregion Value Initialization

            // region Current Scenarios
            // Determine active scenarios, to ensure we don't generate a scenario for an
            // already
            // assigned lance and to remove any currently active scenarios from the
            // contract, so that
            // the generation rules are followed for all active scenarios not just new
            // scenarios
            for (final AtBScenario scenario : contract.getCurrentAtBScenarios()) {
                // Add any currently assigned combatTeamsTable to the assignedLances
                assignedLances.add(scenario.getCombatTeamId());

                // Remove any active scenarios from the contract, and add them to the current
                // scenarios list instead
                contract.getScenarios().remove(scenario);
                sList.add(scenario);
                dontGenerateForces.add(scenario.getId());

                // If we have a current base attack (attacker) scenario, no other scenarios
                // should
                // be generated for that contract
                if ((scenario.getScenarioType() == AtBScenario.BASEATTACK)) {
                    hasBaseAttack = true;
                    if (scenario.isAttacker()) {
                        hasBaseAttackAttacker = true;
                        break;
                    }
                }
            }
            // endregion Current Scenarios

            // region Generate Scenarios
            // Generate scenarios for combatTeamsTable based on their current situation
            if (!hasBaseAttackAttacker) {
                for (CombatTeam combatTeam : combatTeamsTable.values()) {
                    // Don't generate scenarios for any combatTeamsTable already assigned, those assigned to a
                    // different contract, those not assigned to a contract, or for illegible combatTeamsTable
                    if (assignedLances.contains(combatTeam.getForceId()) || (combatTeam.getContract(campaign) == null)
                            || !combatTeam.isEligible(campaign) || (combatTeam.getMissionId() != contract.getId())
                            || !combatTeam.getContract(campaign).isActiveOn(campaign.getLocalDate(), true)) {
                        continue;
                    }

                    // Don't generate scenarios for contracts with morale below the morale limit of
                    // Low
                    if (contract.getMoraleLevel().isCritical() || contract.getMoraleLevel().isRouted()) {
                        continue;
                    }

                    // Attempt to generate a scenario for the combatTeam
                    AtBScenario scenario = combatTeam.checkForBattle(campaign);

                    // If one is generated, then add it to the scenario list
                    if (scenario != null) {
                        sList.add(scenario);
                        assignedLances.add(combatTeam.getForceId());

                        // We care if the scenario is a Base Attack, as one must be generated if the
                        // current contract's morale is Unbreakable
                        if (scenario.getScenarioType() == AtBScenario.BASEATTACK) {
                            hasBaseAttack = true;

                            // If a Base Attack (Attacker) scenario is generated, this is the only
                            // scenario that will take place this week for this contract. We can
                            // therefore break out of the loop
                            if (scenario.isAttacker()) {
                                hasBaseAttackAttacker = true;
                                break;
                            }
                        }
                    }
                }
            }
            // endregion Generate Scenarios

            // region Overwhelming Morale Missions
            // Make sure Overwhelming morale missions have a base attack scenario generated
            if (!campaign.getCampaignOptions().isUseStratCon()) {
                if (!hasBaseAttack && contract.getMoraleLevel().isOverwhelming()) {
                    /*
                     * find a lance to act as defender, giving preference
                     * first to those assigned to the same contract,
                     * then to those assigned to defense roles
                     */
                    List<CombatTeam> lList = new ArrayList<>();
                    for (CombatTeam combatTeam : combatTeamsTable.values()) {
                        if ((combatTeam.getMissionId() == contract.getId())
                            && combatTeam.getRole().isGarrison() && combatTeam.isEligible(campaign)) {
                            lList.add(combatTeam);
                        }
                    }

                    if (lList.isEmpty()) {
                        for (CombatTeam combatTeam : combatTeamsTable.values()) {
                            if ((combatTeam.getMissionId() == contract.getId()) && combatTeam.isEligible(campaign)) {
                                lList.add(combatTeam);
                            }
                        }
                    }

                    if (lList.isEmpty()) {
                        for (CombatTeam combatTeam : combatTeamsTable.values()) {
                            if (combatTeam.isEligible(campaign)) {
                                lList.add(combatTeam);
                            }
                        }
                    }

                    if (!lList.isEmpty()) {
                        CombatTeam combatTeam = ObjectUtility.getRandomItem(lList);
                        AtBScenario atbScenario = AtBScenarioFactory.createScenario(campaign, combatTeam,
                                AtBScenario.BASEATTACK, false, CombatTeam.getBattleDate(campaign.getLocalDate()));
                        if (atbScenario != null) {
                            if ((combatTeam.getMissionId() == atbScenario.getMissionId())
                                    || (combatTeam.getMissionId() == CombatTeam.NO_MISSION)) {
                                for (int i = 0; i < sList.size(); i++) {
                                    if (sList.get(i).getCombatTeamId() == combatTeam.getForceId()) {
                                        if (dontGenerateForces.contains(atbScenario.getId())) {
                                            dontGenerateForces.remove(atbScenario.getId());
                                        }
                                        sList.set(i, atbScenario);
                                        break;
                                    }
                                }
                            } else {
                                // edge case: combatTeam assigned to another mission gets assigned the scenario,
                                // we need to remove any scenario they are assigned to already
                                campaign.getMission(combatTeam.getMissionId()).getScenarios()
                                        .removeIf(scenario -> (scenario instanceof AtBScenario)
                                                && (((AtBScenario) scenario).getCombatTeamId() == combatTeam.getForceId()));
                            }
                            if (!sList.contains(atbScenario)) {
                                sList.add(atbScenario);
                            }
                            if (!assignedLances.contains(combatTeam.getForceId())) {
                                assignedLances.add(combatTeam.getForceId());
                            }
                        } else {
                            logger.error("Unable to generate Base Attack scenario.");
                        }
                    } else {
                        logger.warn("No combatTeamsTable assigned to mission " + contract.getName()
                                + ". Can't generate an Unbreakable Morale base defense mission for this force.");
                    }
                }
            }
            // endregion Unbreakable Morale Missions

            // region Base Attack (Attacker) Generated
            // If there is a base attack (attacker), it is the only one for this contract
            // until it happens.
            // Therefore, all other currently generated scenarios need to be cleared
            if (hasBaseAttackAttacker) {
                sList.removeIf(atbScenario -> !(atbScenario.isAttacker()
                        && (atbScenario.getScenarioType() == AtBScenario.BASEATTACK)));
            }
            // endregion Base Attack (Attacker) Generated

            // region Add to Campaign
            // Finally, sort the scenarios by date and add to the campaign, and generate
            // forces
            // for the scenario if required
            sList.sort((s1, s2) -> ObjectUtility.compareNullable(s1.getDate(), s2.getDate(), LocalDate::compareTo));
            for (AtBScenario atbScenario : sList) {
                campaign.addScenario(atbScenario, contract);
                if (!dontGenerateForces.contains(atbScenario.getId())) {
                    atbScenario.setForces(campaign);
                }
            }
            // endregion Add to Campaign
        }
    }
}
