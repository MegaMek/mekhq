package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;

public class AcsEngagementControlActionHandler extends AbstractAcsActionHandler {

    public AcsEngagementControlActionHandler(AcsEngagementControlAction action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isMovement();
    }

    @Override
    public void handle() {
        performEngagementControl();
        setFinished();
    }

    private void performEngagementControl() {
        // WARNING: THIS IS NOT UP TO RULES AS WRITTEN
        AcsEngagementControlAction engagementControl = (AcsEngagementControlAction) getAction();
        if (engagementControl.isIllegal()) {
            return;
        }

        var attackerOpt = game().getFormation(engagementControl.getEntityId());
        var targetOpt = game().getFormation(engagementControl.getTargetFormationId());
        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return;
        }
        var attacker = attackerOpt.get();
        if (attacker.getEngagementControl() != null) {
            // the attacker is already tied down in an engagement and control
            return;
        }
        var target = targetOpt.get();

        if (engagementControl.getEngagementControl().equals(EngagementControl.NONE)) {
            attacker.setEngagementControl(EngagementControl.NONE);
            // Selected none, nothing to do here
            return;
        }

        var toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
        var toHitDefender = AcsEngagementControlToHitData.compileToHit(game(), new AcsEngagementControlAction(target.getId(), attacker.getId(), engagementControl.getEngagementControl()));

//        SBFReportEntry report = new SBFReportEntry(2001).noNL();
//        report.add(new SBFUnitReportEntry(attacker, -1, ownerColor(attacker, game())).text());
//        report.add(new SBFFormationReportEntry(
//            target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text());
//        addReport(report);
        // TODO : Change everything from here and down!

//            addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
        Roll attackerRoll = Compute.rollD6(2);
        Roll defenderRoll = Compute.rollD6(2);

//            report = new SBFReportEntry(2020).noNL();
//            report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
//            report.add(new SBFRollReportEntry(attackerRoll).noNL().text());
//            addReport(report);

        var attackerDelta = attackerRoll.getIntValue() - toHit.getValue();
        var defenderDelta = defenderRoll.getIntValue() - toHitDefender.getValue();

        if (attackerDelta > defenderDelta) {
            attacker.setEngagementControl(engagementControl.getEngagementControl());
            attacker.setEngagementControlFailed(false);

            switch (engagementControl.getEngagementControl()) {
                case NONE:
                    attacker.setEngagementControl(EngagementControl.NONE);
                    break;
                case FORCED_ENGAGEMENT:
                    attacker.setTargetFormationId(target.getId());
                    target.setTargetFormationId(attacker.getId());
                case EVADE:
                case OVERRUN:
                case STANDARD:
                    target.setEngagementControl(engagementControl.getEngagementControl());
            }

        } else {
            addReport(new SBFPublicReportEntry(3068));
        }


    }

}
