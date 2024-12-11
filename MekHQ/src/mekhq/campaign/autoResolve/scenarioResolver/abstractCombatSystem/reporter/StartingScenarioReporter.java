package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.common.IGame;
import mekhq.campaign.autoResolve.AutoResolveGame;

import java.util.Date;
import java.util.function.Consumer;

public class StartingScenarioReporter {

    private final AutoResolveGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public StartingScenarioReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = (AutoResolveGame) game;
    }

    public void logHeader() {
        reportConsumer.accept(new AcsPublicReportEntry(1230).add(
                "# Starting scenario " + game.getScenario().getName() + " @ " + new Date()
            )
        );
    }
}
