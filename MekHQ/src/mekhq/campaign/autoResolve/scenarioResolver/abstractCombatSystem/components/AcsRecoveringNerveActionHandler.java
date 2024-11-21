package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;

import java.util.List;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcsRecoveringNerveActionHandler extends AbstractAcsActionHandler {

    public AcsRecoveringNerveActionHandler(AcsRecoveringNerveAction action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void handle() {
        AcsRecoveringNerveAction recoveringNerveAction = (AcsRecoveringNerveAction) getAction();
        if (!recoveringNerveAction.isIllegal()) {
            var formationOpt = game().getFormation(recoveringNerveAction.getEntityId());
            SBFFormation formation = formationOpt.get();

            // Process Engagement Controll roll in here
            AcsRecoveringNerveActionToHitData toHit = AcsRecoveringNerveActionToHitData.compileToHit(game(), recoveringNerveAction);

//            AcsEngagementControlToHitData toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
//            SBFReportEntry report = new SBFReportEntry(2001).noNL();
//            report.add(new SBFUnitReportEntry(attacker, -1, ownerColor(attacker, game())).text());
//            report.add(new SBFFormationReportEntry(
//                target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text());
//            addReport(report);
//            // TODO : Change everything from here and down!
//            if (toHit.cannotSucceed()) {
//                addReport(new SBFReportEntry(2010).add(toHit.getDesc()));
//            } else {
//                addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
//                Roll roll = Compute.rollD6(2);
//                report = new SBFReportEntry(2020).noNL();
//                report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
//                report.add(new SBFRollReportEntry(roll).noNL().text());
//                addReport(report);
//            }
        }
        setFinished();
    }
}
