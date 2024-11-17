package mekhq.campaign.autoResolve.scenarioResolver;

import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveForce;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.SimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

import java.util.List;
import java.util.Map;

public abstract class ScenarioResolver {

    protected AtBScenario scenario;

    protected ScenarioResolver(AtBScenario scenario) {
        this.scenario = scenario;
    }

    public static ScenarioResolver of(AtBScenario scenario) {
        return new SimpleScenarioResolver(scenario);
    }

    public abstract AutoResolveConcludedEvent resolveScenario(Map<Integer, List<AutoResolveForce>> teams);

}
