package mekhq.campaign.autoResolve.scenarioResolver;

import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitsMatterSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

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

    public abstract AutoResolveConcludedEvent resolveScenario(AutoResolveGame game);

}
