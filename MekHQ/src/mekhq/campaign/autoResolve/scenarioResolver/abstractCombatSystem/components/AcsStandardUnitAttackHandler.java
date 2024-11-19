package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;

import java.util.List;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcsStandardUnitAttackHandler extends AbstractAcsActionHandler {

    public AcsStandardUnitAttackHandler(AcsStandardUnitAttack action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    @Override
    public void handle() {
        AcsStandardUnitAttack attack = (AcsStandardUnitAttack) getAction();
        if (attack.isDataValid(game())) {
            var attackerOpt = game().getFormation(attack.getEntityId());
            var targetOpt = game().getFormation(attack.getTargetId());
            if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
                return;
            }
            SBFFormation attacker = attackerOpt.get();
            SBFFormation target = targetOpt.get();
            SBFUnit attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
            List<SBFUnit> targetUnits = target.getUnits();
            SBFUnit targetUnit = targetUnits.get(0);

            AcsToHitData toHit = AcsToHitData.compileToHit(game(), attack);
            SBFReportEntry report = new SBFReportEntry(2001).noNL();
            report.add(new SBFUnitReportEntry(attacker, attack.getUnitNumber(), ownerColor(attacker, game())).text());
            report.add(new SBFFormationReportEntry(
                target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text());
            addReport(report);

            if (toHit.cannotSucceed()) {
                addReport(new SBFReportEntry(2010).add(toHit.getDesc()));
            } else {
                addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
                Roll roll = Compute.rollD6(2);
                report = new SBFReportEntry(2020).noNL();
                report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
                report.add(new SBFRollReportEntry(roll).noNL().text());
                addReport(report);

                if (roll.getIntValue() < toHit.getValue()) {
                    addReport(new SBFPublicReportEntry(2012));
                } else {
                    addReport(new SBFPublicReportEntry(2013));
                    int damage = attackingUnit.getCurrentDamage().getDamage(attack.getRange()).damage;
                    if (damage > 0) {
                        int newArmor = targetUnit.getCurrentArmor() - damage;
                        addReport(new SBFPublicReportEntry(3100).add(damage).add(damage));
                        if (newArmor < 0) {
                            newArmor = 0;
                        }
                        targetUnits.get(0).setCurrentArmor(newArmor);
                        if (newArmor == 0) {
                            addReport(new SBFPublicReportEntry(3092));
                        }
                        if (newArmor * 2 < targetUnit.getArmor()) {
                            targetUnit.addDamageCrit();
                        }
//                        gameManager().sendUnitUpdate(target);
                    } else {
                        addReport(new SBFPublicReportEntry(3068));
                    }
                }
            }
        }
        setFinished();
    }
}
