package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.EngagementControl;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcsWithdrawReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public AcsWithdrawReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportStartWithdraw(AcsFormation withdrawingFormation, TargetRoll toHitData) {
        // Formation trying to withdraw
        var report = new AcsReportEntry(3330).noNL()
            .add(new SBFFormationReportEntry(
                withdrawingFormation.generalName(),
                UIUtil.hexColor(ownerColor(withdrawingFormation, game))
            ).text())
            .add(withdrawingFormation.moraleStatus().name().toLowerCase()).indent();
        reportConsumer.accept(report);

        // To-Hit Value
        reportConsumer.accept(new AcsReportEntry(3331).add(toHitData.getValue()).add(toHitData.toString()).indent());
    }

    public void reportWithdrawRoll(AcsFormation withdrawingFormation, Roll withdrawRoll) {
        var report = new AcsReportEntry(3332).noNL();
        report.add(new SBFPlayerNameReportEntry(game.getPlayer(withdrawingFormation.getOwnerId())).text());
        report.add(new AcsRollReportEntry(withdrawRoll).reportText()).indent();
        reportConsumer.accept(report);
    }

    public void reportSuccessfulWithdraw() {
        reportConsumer.accept(new AcsPublicReportEntry(3333).indent());
    }

    public void reportFailedWithdraw() {
        reportConsumer.accept(new AcsPublicReportEntry(3334).indent());
    }
}
