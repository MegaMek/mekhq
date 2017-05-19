package mekhq.campaign.mission.atb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mekhq.MekHQ;
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
	private static Map<Integer, List<Class<IAtBScenario>>> scenarioMap = new HashMap<Integer, List<Class<IAtBScenario>>>();

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
		Class<IAtBScenario> selectedClass = null;

		if ((null == classList) || classList.isEmpty()) {
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
	public static boolean registerScenario(IAtBScenario scenario) {
		if (!IAtBScenario.class.isAssignableFrom(scenario.getClass())) {
			MekHQ.logMessage(String.format("Unable to register an AtBScenario of class '%s' because is does not implement '%s'.", scenario.getClass().getName(), IAtBScenario.class.getName()), 5);
			return false;
		}
		
		if (!scenario.getClass().isAnnotationPresent(AtBScenarioEnabled.class)) {
			MekHQ.logMessage(String.format("Unable to register an AtBScenario of class '%s' because is does not have the '%s' annotation.", scenario.getClass().getName(), AtBScenarioEnabled.class.getName()), 5);
			return false;
		}
		
		int type = scenario.getScenarioType();
		List<Class<IAtBScenario>> list = scenarioMap.get(type);

		if (null == list) {
			list = new ArrayList<Class<IAtBScenario>>();
			scenarioMap.put(type, list);
		}

		list.add((Class<IAtBScenario>) scenario.getClass());

		return true;
	}
	
	/* Iterate through the list of lances and make a battle roll for each,
	 * then sort them by date before adding them to the campaign.
	 * Contracts with enemy morale level of invincible have a base attack
	 * (defender) battle each week. If there is a base attack (attacker)
	 * battle, that is the only one for the week on that contract.
	 */
	public static void createScenariosForNewWeek(Campaign c) {
		Hashtable<Integer, Lance> lances = c.getLances();
		
		ArrayList<AtBScenario> sList = new ArrayList<AtBScenario>();
		AtBScenario baseAttack = null;

		for (Lance l : lances.values()) {
			if (null == l.getContract(c) || !l.getContract(c).isActive() ||
					!l.isEligible(c) ||
					c.getDate().before(l.getContract(c).getStartDate())) {
				continue;
			}
			
			if (l.getRole() == Lance.ROLE_TRAINING) {
				c.awardTrainingXP(l);
			}
			
			if (l.getContract(c).getMoraleLevel() <= AtBContract.MORALE_VERYLOW) {
				continue;
			}
			
			AtBScenario scenario = l.checkForBattle(c);
			if (null != scenario) {
				sList.add(scenario);
				if (scenario.getScenarioType() == AtBScenario.BASEATTACK && scenario.isAttacker()) {
					baseAttack = scenario;
					break;
				}
			}
		}

		/* If there is a base attack (attacker), all other battles on
		 * that contract are cleared.
		 */
		if (null != baseAttack) {
			ArrayList<Scenario> sameContract = new ArrayList<Scenario>();
			for (AtBScenario s : sList) {
				if (s != baseAttack && s.getMissionId() == baseAttack.getMissionId()) {
					sameContract.add(s);
				}
			}
			sList.removeAll(sameContract);
		}

		/* Make sure invincible morale has base attack */
		for (Mission m : c.getMissions()) {
			if (m.isActive() && m instanceof AtBContract &&
					((AtBContract)m).getMoraleLevel() == AtBContract.MORALE_INVINCIBLE) {
				boolean hasBaseAttack = false;
				for (AtBScenario s : sList) {
					if (s.getMissionId() == m.getId() &&
							s.getScenarioType() == AtBScenario.BASEATTACK &&
							!s.isAttacker()) {
						hasBaseAttack = true;
						break;
					}
				}
				if (!hasBaseAttack) {
					/* find a lance to act as defender, giving preference
					 * first to those assigned to the same contract,
					 * then to those assigned to defense roles
					 */
					ArrayList<Lance> lList = new ArrayList<Lance>();
    				for (Lance l : lances.values()) {
    					if (l.getMissionId() == m.getId()
    							&& l.getRole() == Lance.ROLE_DEFEND
    							&& l.isEligible(c)) {
    						lList.add(l);
    					}
    				}
    				if (lList.size() == 0) {
    					for (Lance l : lances.values()) {
    						if (l.getMissionId() == m.getId()
    								&& l.isEligible(c)) {
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
    					AtBScenario scenario = AtBScenarioFactory.createScenario(c, lance, AtBScenario.BASEATTACK, false,
    							Lance.getBattleDate(c.calendar));
    					for (int i = 0; i < sList.size(); i++) {
    						if (sList.get(i).getLanceForceId() ==
    								lance.getForceId()) {
    							sList.set(i, scenario);
    							break;
    						}
    					}
    					if (!sList.contains(scenario)) {
    						sList.add(scenario);
    					}
    				} else {
    					//TODO: What to do if there are no lances assigned to this contract?
    				}
				}
			}
		}

		/* Sort by date and add to the campaign */
		Collections.sort(sList, new Comparator<AtBScenario>() {
			@Override
            public int compare(AtBScenario s1, AtBScenario s2) {
				return s1.getDate().compareTo(s2.getDate());
			}
		});
		for (AtBScenario s : sList) {
			c.addScenario(s, c.getMission(s.getMissionId()));
			s.setForces(c);
		}		
	}
}
