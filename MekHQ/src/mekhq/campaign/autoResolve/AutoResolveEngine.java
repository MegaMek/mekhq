package mekhq.campaign.autoResolve;

import mekhq.MekHQ;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.helper.SetupTeams;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.List;

public class AutoResolveEngine {

    public AutoResolveEngine() {
    }

    public void resolveBattle(MekHQ app, List<Unit> units, AtBScenario scenario) {
        var scenarioSpecificResolutionResolver = ScenarioResolver.of(scenario);
        var teams = SetupTeams.setupTeams(app, units, scenario);
        var result = scenarioSpecificResolutionResolver.resolveScenario(teams);
        app.autoResolveConcluded(result);
    }

}
