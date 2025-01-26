package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

public class PrisonerEventResultsDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

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
            new ButtonLabelTooltipPair(resources.getString(resourceKey), null);

        return List.of(btnConfirmation);
    }

    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event,
                                                   int choiceIndex, boolean isSuccessful) {
        String suffix = isSuccessful ? SUFFIX_SUCCESS : SUFFIX_FAILURE;
        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString(FORWARD_RESPONSE + choiceIndex + '.' + event.name() + suffix),
            commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("result.ooc");
    }
}
