package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class PrisonerEventResultsDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEventDialog";

    static final String FORWARD_RESPONSE = "response.";
    static final String SUFFIX_SUCCESS = ".success";
    static final String SUFFIX_FAILURE = ".failure";

    public PrisonerEventResultsDialog(Campaign campaign, @Nullable Person speaker, PrisonerEvent event,
                                     int choiceIndex, boolean isSuccessful) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, event,
                choiceIndex, isSuccessful), createButtons(isSuccessful), createOutOfCharacterMessage(),
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(boolean isSuccessful) {
        String resourceKey = isSuccessful ? "successful.button" : "failure.button";

        ButtonLabelTooltipPair btnConfirmation =
            new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, resourceKey), null);

        return List.of(btnConfirmation);
    }

    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event,
                                                   int choiceIndex, boolean isSuccessful) {
        String suffix = isSuccessful ? SUFFIX_SUCCESS : SUFFIX_FAILURE;
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + choiceIndex + '.' + event.name() + suffix,
            commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
