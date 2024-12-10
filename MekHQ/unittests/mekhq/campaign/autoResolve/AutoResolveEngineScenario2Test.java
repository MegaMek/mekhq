package mekhq.campaign.autoResolve;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoResolveEngineScenario2Test extends AbstractAutoResolveEngineScenarios {

    @Override
    double lowerBoundTeam1() {
        return 0.50;
    }

    @Override
    double upperBoundTeam1() {
        return 0.60;
    }

    @Override
    double lowerBoundTeam2() {
        return 0.30;
    }

    @Override
    double upperBoundTeam2() {
        return 0.40;
    }

    @Override
    double lowerBoundDraw() {
        return 0.07;
    }

    @Override
    double upperBoundDraw() {
        return 0.13;
    }

    @Override
    TeamArrangement getTeamArrangement() {
        return TeamArrangement.UNBALANCED;
    }
}
