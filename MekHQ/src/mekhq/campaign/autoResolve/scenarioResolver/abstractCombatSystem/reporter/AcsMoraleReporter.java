package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;

import java.util.function.Consumer;

/**
 * Reporter for morale checks.
 * Message IDs starting at 4500.
 */
public class AcsMoraleReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public AcsMoraleReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportMoraleCheckStart(AcsFormation formation, int toHitValue) {
        // 4500: Start of morale check
        var startReport = new AcsReportEntry(4500)
            .add(new AcsFormationReportEntry(formation, game).text())
            .add(toHitValue);
        reportConsumer.accept(startReport);
    }

    public void reportMoraleCheckRoll(AcsFormation formation, Roll roll) {
        // 4501: Roll result
        var rollReport = new AcsReportEntry(4501)
            .add(new AcsFormationReportEntry(formation, game).text())
            .add(new AcsRollReportEntry(roll).reportText());
        reportConsumer.accept(rollReport);
    }

    public void reportMoraleCheckSuccess(AcsFormation formation) {
        // 4502: Success - morale does not worsen
        var successReport = new AcsReportEntry(4502)
            .add(new AcsFormationReportEntry(formation, game).text());
        reportConsumer.accept(successReport);
    }

    public void reportMoraleCheckFailure(AcsFormation formation, SBFFormation.MoraleStatus oldStatus, SBFFormation.MoraleStatus newStatus) {
        // 4503: Failure - morale worsens
        var failReport = new AcsReportEntry(4503)
            .add(new AcsFormationReportEntry(formation, game).text())
            .add(oldStatus.name())
            .add(newStatus.name());

        reportConsumer.accept(failReport);
    }
}
