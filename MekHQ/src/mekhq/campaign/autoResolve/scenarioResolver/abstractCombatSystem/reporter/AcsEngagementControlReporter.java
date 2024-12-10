package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.EngagementControl;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;


public class AcsEngagementControlReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public AcsEngagementControlReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportEngagementStart(AcsFormation attacker, AcsFormation target, EngagementControl control) {
        AcsReportEntry report = new AcsReportEntry(2200)
            .add(new SBFFormationReportEntry(attacker.generalName(),
                UIUtil.hexColor(ownerColor(attacker, game))).text())
            .add(new SBFFormationReportEntry(target.generalName(),
                UIUtil.hexColor(ownerColor(target, game))).text())
            .add(control.name());
        reportConsumer.accept(report);
    }

    public void reportAttackerToHitValue(int toHitValue) {
        reportConsumer.accept(new AcsReportEntry(2203).indent().add(toHitValue));
    }

    public void reportAttackerRoll(AcsFormation attacker, Roll attackerRoll) {
        AcsReportEntry report = new AcsReportEntry(2202);
        report.add(new SBFPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new AcsRollReportEntry(attackerRoll).reportText()).indent();
        reportConsumer.accept(report);
    }

    public void reportDefenderRoll(AcsFormation target, Roll defenderRoll) {
        AcsReportEntry report = new AcsReportEntry(2202).indent();
        report.add(new SBFPlayerNameReportEntry(game.getPlayer(target.getOwnerId())).text());
        report.add(new AcsRollReportEntry(defenderRoll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackerWin(AcsFormation attacker) {
        AcsReportEntry report = new AcsReportEntry(2204).indent()
            .add(new SBFPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        reportConsumer.accept(report);
    }

    public void reportAttackerLose(AcsFormation attacker) {
        reportConsumer.accept(new AcsPublicReportEntry(2205)
            .add(new SBFPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text())
            .indent());
    }
}
