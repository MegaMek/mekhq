package mekhq.campaign.autoResolve;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoResolveEngineScenario1Test extends AbstractAutoResolveEngineScenarios {
    /**
     * Those values are used to determine the expected outcome of the auto resolve
     * Because I am running less than 1000 times, they may vary alot, this means that its not uncommon for them to fail
     * randomly
     */
    @Override
    double lowerBoundTeam1() {
        return 0.38;
    }

    @Override
    double upperBoundTeam1() {
        return 0.55;
    }

    @Override
    double lowerBoundTeam2() {
        return 0.38;
    }

    @Override
    double upperBoundTeam2() {
        return 0.55;
    }

    @Override
    double lowerBoundDraw() {
        return 0.08;
    }

    @Override
    double upperBoundDraw() {
        return 0.18;
    }

    @Override
    TeamArrangement getTeamArrangement() {
        return TeamArrangement.BALANCED;
    }

}
