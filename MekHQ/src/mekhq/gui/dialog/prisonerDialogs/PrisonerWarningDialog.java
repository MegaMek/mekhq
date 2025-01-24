package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

public class PrisonerWarningDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public PrisonerWarningDialog(Campaign campaign, @Nullable Person speaker,
                                 @Nullable Integer executeCount, @Nullable Integer freeCount) {
        super(campaign, speaker, null, createInCharacterMessage(campaign),
            createButtons( executeCount, freeCount), createOutOfCharacterMessage(),
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(@Nullable Integer executeCount,
                                                              @Nullable Integer freeCount) {
        ButtonLabelTooltipPair btnDoNothing = new ButtonLabelTooltipPair(
            String.format(resources.getString("btnDoNothing.button"), freeCount),
            resources.getString("btnDoNothing.tooltip"));
        ButtonLabelTooltipPair btnFree = new ButtonLabelTooltipPair(
            String.format(resources.getString("free.button"), freeCount),
            resources.getString("free.tooltip"));
        ButtonLabelTooltipPair btnExecute = new ButtonLabelTooltipPair(
            String.format(resources.getString("execute.button"), executeCount),
            resources.getString("execute.tooltip"));

        return List.of(btnDoNothing, btnFree, btnExecute);
    }

    private static String createInCharacterMessage(Campaign campaign) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString("warning.message"), commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("warning.ooc");
    }
}
