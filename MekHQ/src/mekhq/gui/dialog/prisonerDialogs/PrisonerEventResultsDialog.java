package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

public class PrisonerEventResultsDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public enum ResponseKey {
        RESPONSE_A('A'),
        RESPONSE_B('B'),
        RESPONSE_C('C');

        private final char choiceKey;

        ResponseKey(char choiceKey) {
            this.choiceKey = choiceKey;
        }

        public char getChoiceKey() {
            return choiceKey;
        }
    }

    public PrisonerEventResultsDialog(Campaign campaign, @Nullable Person speaker, int eventRoll,
                                     int choice, boolean isMinor, boolean isSuccessful) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, eventRoll,
                choice, isMinor, isSuccessful), createButtons(isSuccessful), createOutOfCharacterMessage(),
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(boolean isSuccessful) {
        String resourceKey = isSuccessful ? "successful.button" : "failure.button";

        ButtonLabelTooltipPair btnConfirmation =
            new ButtonLabelTooltipPair(resources.getString(resourceKey), null);

        return List.of(btnConfirmation);
    }

    private static String createInCharacterMessage(Campaign campaign, int eventRoll, int choice,
                                                   boolean isMinor, boolean isSuccessful) {
        String magnitudeKey = isMinor ? "Minor" : "Major";
        String resultsKey = isSuccessful ? "success" : "failure";
        char choiceKey = ResponseKey.values()[choice].getChoiceKey();
        String fullResourceKey = "response" + magnitudeKey + choiceKey + eventRoll + '.' + resultsKey;

        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString(fullResourceKey), commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("result.ooc");
    }
}
