package mekhq.campaign.autoResolve.scenarioResolver;

import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveForce;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitsMatterSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

import java.util.List;
import java.util.Map;

public abstract class ScenarioResolver {

    protected AtBScenario scenario;

    protected ScenarioResolver(AtBScenario scenario) {
        this.scenario = scenario;
    }

    public static ScenarioResolver of(AutoResolveMethod method, AtBScenario scenario) {
        return switch (method) {
            case UNITS_MATTER -> new UnitsMatterSimpleScenarioResolver(scenario);
            case ABSTRACT_COMBAT_SYSTEM -> new AcsSimpleScenarioResolver(scenario);
        };
    }

    public abstract AutoResolveConcludedEvent resolveScenario(Map<Integer, List<AutoResolveForce>> teams);

}
