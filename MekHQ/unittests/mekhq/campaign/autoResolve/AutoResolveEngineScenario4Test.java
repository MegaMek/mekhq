package mekhq.campaign.autoResolve;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoResolveEngineScenario4Test extends AbstractAutoResolveEngineScenarios {

    @Override
    double lowerBoundTeam1() {
        return 0.47;
    }

    @Override
    double upperBoundTeam1() {
        return 0.57;
    }

    @Override
    double lowerBoundTeam2() {
        return 0.3;
    }

    @Override
    double upperBoundTeam2() {
        return 0.4;
    }

    @Override
    double lowerBoundDraw() {
        return 0.08;
    }

    @Override
    double upperBoundDraw() {
        return 0.16;
    }

    @Override
    TeamArrangement getTeamArrangement() {
        return TeamArrangement.SAME_BV_SAME_SKILL;
    }
}
