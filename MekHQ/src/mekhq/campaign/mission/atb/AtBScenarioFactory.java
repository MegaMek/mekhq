package mekhq.campaign.mission.atb;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBScenario;
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
			return false;
		}
		
		if (!scenario.getClass().isAnnotationPresent(AtBScenarioEnabled.class)) {
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
}
