package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcsRecoveringNerveActionReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public AcsRecoveringNerveActionReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void reportRecoveringNerveStart(AcsFormation formation) {
        // ownerColor(formation, game)
        reportConsumer.accept(new AcsReportEntry(4000)
            .add(new AcsFormationReportEntry(formation.generalName(), UIUtil.hexColor(ownerColor(formation, game))).text()));
    }

    public void reportToHitValue(int toHitValue) {
        reportConsumer.accept(new AcsReportEntry(4001).add(toHitValue).noNL());
    }

    public void reportSuccessRoll(Roll roll) {
        var report = new AcsReportEntry(4002).indent().noNL();
        report.add(new AcsRollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportMoraleStatusChange(SBFFormation.MoraleStatus newMoraleStatus) {
        reportConsumer.accept(new AcsReportEntry(4003).add(newMoraleStatus.name()));
    }

    public void reportFailureRoll(Roll roll) {
        reportConsumer.accept(new AcsReportEntry(4004));
    }
}
