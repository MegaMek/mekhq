package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

import static megamek.common.Compute.randomInt;

public class PrisonerWarningResultsDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public PrisonerWarningResultsDialog(Campaign campaign, @Nullable Person speaker, boolean isExecute) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, isExecute),
            createButtons(), createOutOfCharacterMessage(),
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnUnderstood = new ButtonLabelTooltipPair(
            resources.getString("btnUnderstood.button"), null);

        return List.of(btnUnderstood);
    }

    private static String createInCharacterMessage(Campaign campaign, boolean isExecute) {
        String executeKey = isExecute ? "execute" : "free";
        int eventRoll = randomInt(50);

        String resourceKey = executeKey + "Event" + eventRoll + ".message";

        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString(resourceKey), commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("result.ooc");
    }
}
