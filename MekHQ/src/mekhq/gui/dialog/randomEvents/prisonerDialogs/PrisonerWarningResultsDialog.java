package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static megamek.common.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class PrisonerWarningResultsDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public PrisonerWarningResultsDialog(Campaign campaign, @Nullable Person speaker, boolean isExecute) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, isExecute),
            createButtons(), createOutOfCharacterMessage(), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnUnderstood = new ButtonLabelTooltipPair(
                getFormattedTextAt(RESOURCE_BUNDLE, "btnUnderstood.button"), null);

        return List.of(btnUnderstood);
    }

    private static String createInCharacterMessage(Campaign campaign, boolean isExecute) {
        String executeKey = isExecute ? "execute" : "free";
        int eventRoll = randomInt(50);

        String resourceKey = executeKey + "Event" + eventRoll + ".message";

        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
