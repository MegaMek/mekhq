package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcsAttackReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public AcsAttackReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }
    public void reportAttackStart(AcsFormation attacker, int unitNumber, AcsFormation target) {
        var report = new AcsReportEntry(2001).noNL();
        report.add(new SBFUnitReportEntry(attacker, unitNumber, ownerColor(attacker, game)).text());
        report.add(new SBFFormationReportEntry(
            target.generalName(), UIUtil.hexColor(ownerColor(target, game))).text());
        reportConsumer.accept(report);
    }

    public void reportCannotSucceed(String toHitDesc) {
        reportConsumer.accept(new AcsReportEntry(2010).add(toHitDesc));
    }

    public void reportToHitValue(AcsToHitData toHitValue) {
        // e.g. "Needed X to hit"
        reportConsumer.accept(new AcsReportEntry(2003).indent().add(toHitValue.getValue())
            .add(toHitValue.toString()));
    }

    public void reportAttackRoll(Roll roll, AcsFormation attacker) {
        var report = new AcsReportEntry(2020).indent();
        report.add(new SBFPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new AcsRollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackMiss() {
        reportConsumer.accept(new AcsPublicReportEntry(2012).indent(2));
    }

    public void reportAttackHit() {
        reportConsumer.accept(new AcsPublicReportEntry(2013).indent(2));
    }

    public void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor) {
        reportConsumer.accept(new AcsPublicReportEntry(3100)
            .add(targetUnit.getName())
            .add(damage)
            .add(newArmor)
            .indent(2));
    }

    public void reportStressEpisode() {
        reportConsumer.accept(new AcsPublicReportEntry(3090).indent(3));
    }

    public void reportUnitDestroyed() {
        reportConsumer.accept(new AcsPublicReportEntry(3092).indent(3));
    }

    public void reportCriticalCheck() {
        // Called before rolling criticals
        reportConsumer.accept(new AcsPublicReportEntry(3095).indent(3));
    }

    public void reportNoCrit() {
        reportConsumer.accept(new AcsPublicReportEntry(3097).indent(3));
    }

    public void reportTargetingCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new AcsPublicReportEntry(3094)
            .add(targetUnit.getName())
            .add(targetUnit.getTargetingCrits())
            .indent(3));
    }

    public void reportDamageCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new AcsPublicReportEntry(3096)
            .add(targetUnit.getName())
            .add(targetUnit.getDamageCrits())
            .indent(3));
    }

    public void reportUnitCrippled() {
        reportConsumer.accept(new AcsPublicReportEntry(3091).indent(3));
    }
}
