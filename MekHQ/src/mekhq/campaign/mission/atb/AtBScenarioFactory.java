package mekhq.campaign.mission.atb;

import java.awt.event.ActionListener;
import java.util.*;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
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

	public static AtBScenario createScenario(Campaign c, Lance lance, int type, boolean attacker, Date date) {
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
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static void registerScenario(IAtBScenario scenario) {
	    final String METHOD_NAME = "registerScenario(IAtBScenario)"; //$NON-NLS-1$

		if (!scenario.getClass().isAnnotationPresent(AtBScenarioEnabled.class)) {
            MekHQ.getLogger().log(AtBScenarioFactory.class, METHOD_NAME, LogLevel.ERROR,
                    String.format("Unable to register an AtBScenario of class '%s' because is does not have the '%s' annotation.", //$NON-NLS-1$
                            scenario.getClass().getName(), AtBScenarioEnabled.class.getName()));
		} else {
            int type = scenario.getScenarioType();
            List<Class<IAtBScenario>> list = scenarioMap.computeIfAbsent(type, k -> new ArrayList<>());

            list.add((Class<IAtBScenario>) scenario.getClass());
        }
	}

	/**
     * Iterate through the list of lances and make a battle roll for each,
	 * then sort them by date before adding them to the campaign.
	 * Contracts with enemy morale level of invincible have a base attack
	 * (defender) battle each week. If there is a base attack (attacker)
	 * battle, that is the only one for the week on that mission.
     *
     * Note that this handles having multiple missions at the same time
     * @param c the campaign for which to generate scenarios
	 */
	public static void createScenariosForNewWeek(Campaign c) {
		Hashtable<Integer, Lance> lances = c.getLances();

        AtBContract atbContract;
        List<AtBScenario> sList;
		List<Integer> assignedLances = new ArrayList<>();
		List<Integer> dontGenerateForces;
		boolean hasBaseAttack;
		boolean hasBaseAttackAttacker;

		// Determine active missions
        for (Mission mission : c.getMissions()) {
            if (!mission.isActive() || !(mission instanceof AtBContract) ) {
                continue; //if not active or an AtBContract, we don't care about the mission
            }

            //region Value Initialization
            atbContract = (AtBContract) mission;
            sList = new ArrayList<>();
            dontGenerateForces = new ArrayList<>();
            hasBaseAttack = false;
            hasBaseAttackAttacker = false;
            //endregion Value Initialization

            //region Current Scenarios
            // Determine active scenarios
            Iterator<Scenario> iter = atbContract.getScenarios().iterator();
            while (iter.hasNext()) {
                Scenario scenario = iter.next();
                if (!scenario.isCurrent() || !AtBScenario.class.isAssignableFrom(scenario.getClass())) {
                    continue; //if not current or not assignable to an AtB scenario, then we don't care about it
                }

                AtBScenario atbScenario = (AtBScenario) scenario;

                // Add any currently assigned lances to the assignedLances
                assignedLances.add(atbScenario.getLanceForceId());

                // Remove any active scenarios from the contract, and add them to the current scenarios list instead
                iter.remove();
                sList.add(atbScenario);
                dontGenerateForces.add(atbScenario.getId());

                MekHQ.getLogger().error(AtBScenarioFactory.class, "createScenariosForNewWeek",
                        "generateForces shouldn't contain " + atbScenario.getId());

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
            // Generate scenarios for lances based on whether the situation fits
            if (!hasBaseAttackAttacker) {
                for (Lance lance : lances.values()) {
                    // Don't generate again for any lances already assigned, assigned to a different
                    // mission, or illegible lances
                    if (assignedLances.contains(lance.getForceId()) || (lance.getContract(c) == null)
                            || !lance.isEligible(c) || !lance.getContract(c).isActive()
                            || !(lance.getMissionId() == atbContract.getId())
                            || c.getDate().before(lance.getContract(c).getStartDate())) {
                        continue;
                    }

                    if (lance.getRole() == Lance.ROLE_TRAINING) {
                        c.awardTrainingXP(lance);
                    }

                    if (lance.getContract(c).getMoraleLevel() <= AtBContract.MORALE_VERYLOW) {
                        continue;
                    }

                    AtBScenario atbScenario = lance.checkForBattle(c);
                    if (atbScenario != null) {
                        sList.add(atbScenario);
                        assignedLances.add(lance.getForceId());
                        if (atbScenario.getScenarioType() == AtBScenario.BASEATTACK) {
                            hasBaseAttack = true;
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
                    if ((l.getMissionId() == atbContract.getId()) && (l.getRole() == Lance.ROLE_DEFEND) && l.isEligible(c)) {
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
                    AtBScenario scenario = AtBScenarioFactory.createScenario(c, lance,
                            AtBScenario.BASEATTACK, false, Lance.getBattleDate(c.getCalendar()));
                    if (scenario != null) {
                        for (int i = 0; i < sList.size(); i++) {
                            if (sList.get(i).getLanceForceId() == lance.getForceId()) {
                                if (dontGenerateForces.contains(scenario.getId())) {
                                    dontGenerateForces.remove(scenario.getId());
                                }
                                sList.set(i, scenario);
                                break;
                            }
                        }
                        if (!sList.contains(scenario)) {
                            sList.add(scenario);
                        }
                        if (!assignedLances.contains(lance.getForceId())) {
                            assignedLances.add(lance.getForceId());
                        }
                    } else {
                        MekHQ.getLogger().error(AtBScenarioFactory.class, "createScenariosForNewWeek",
                                "Unable to generate Base Attack scenario.");
                    }
                } else {
                    MekHQ.getLogger().warning(AtBScenarioFactory.class, "createScenariosForNewWeek",
                            "No lances assigned to mission " + atbContract.getName() +
                                    ". Can't generate an Invincible Morale base defence mission for this force.");
                }
            }
            //endregion Invincible Morale Missions

            //region Base Attack (Attacker) Generated
            // If there is a base attack (attacker), all other currently generated scenarios are cleared
            if (hasBaseAttackAttacker) {
                sList.removeIf(atbScenario ->
                        !(atbScenario.isAttacker() && (atbScenario.getScenarioType() == AtBScenario.BASEATTACK)));
            }
            //endregion Base Attack (Attacker) Generated

            //region Add to Campaign
            // Finally, sort the scenarios by date and add to the campaign
            sList.sort(Comparator.comparing(Scenario::getDate));
            for (AtBScenario atbScenario : sList) {
                c.addScenario(atbScenario, atbContract);
                MekHQ.getLogger().error(AtBScenarioFactory.class, "createScenariosForNewWeek",
                        "dontGenerateForces contains " + dontGenerateForces + " and the scenario id is " + atbScenario.getId());
                if (!dontGenerateForces.contains(atbScenario.getId())) {
                    atbScenario.setForces(c);
                }
            }
            //endregion Add to Campaign
        }
	}
}
