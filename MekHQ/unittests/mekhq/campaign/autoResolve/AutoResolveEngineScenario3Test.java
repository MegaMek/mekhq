package mekhq.campaign.autoResolve;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoResolveEngineScenario3Test extends AbstractAutoResolveEngineScenarios {

    @Override
    double lowerBoundTeam1() {
        return 0.57;
    }

    @Override
    double upperBoundTeam1() {
        return 0.64;
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
        return 0.03;
    }

    @Override
    double upperBoundDraw() {
        return 0.09;
    }

    @Override
    TeamArrangement getTeamArrangement() {
        return TeamArrangement.SAME_BV;
    }
}
