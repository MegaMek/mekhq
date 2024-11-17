package mekhq.campaign.autoResolve;

import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitsMatterSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

public enum AutoResolveMethod {
    UNITS_MATTER(){
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            return new UnitsMatterSimpleScenarioResolver(scenario);
        }
    },
    ABSTRACT_COMBAT_SYSTEM() {
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            return new AcsSimpleScenarioResolver(scenario);
        }
    };

    public static AutoResolveMethod fromString(String method) {
        return switch (method) {
            case "UNITS_MATTER" -> UNITS_MATTER;
            case "ABSTRACT_COMBAT_SYSTEM" -> ABSTRACT_COMBAT_SYSTEM;
            default -> throw new IllegalArgumentException("Invalid method: " + method);
        };
    }

    public abstract ScenarioResolver of(AtBScenario scenario);
}
