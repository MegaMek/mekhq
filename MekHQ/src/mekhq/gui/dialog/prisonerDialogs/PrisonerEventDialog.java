package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

public class PrisonerEventDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    static final String FORWARD_EVENT = "event.";
    static final String SUFFIX_MESSAGE = ".message";

    static final String FORWARD_RESPONSE = "response.";
    static final String OPTION_INDEX_0 = "0.";
    static final String OPTION_INDEX_1 = "1.";
    static final String OPTION_INDEX_2 = "2.";
    static final String SUFFIX_BUTTON = ".button";
    static final String SUFFIX_TOOLTIP = ".tooltip";

    public PrisonerEventDialog(Campaign campaign, @Nullable Person speaker, PrisonerEvent event) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, event),
            createButtons(event), createOutOfCharacterMessage(), null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(PrisonerEvent event) {
        ButtonLabelTooltipPair btnResponseA = new ButtonLabelTooltipPair(
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_0 + event.name() + SUFFIX_BUTTON),
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_0 + event.name() + SUFFIX_TOOLTIP));
        ButtonLabelTooltipPair btnResponseB = new ButtonLabelTooltipPair(
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_1 + event.name() + SUFFIX_BUTTON),
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_1 + event.name() + SUFFIX_TOOLTIP));
        ButtonLabelTooltipPair btnResponseC = new ButtonLabelTooltipPair(
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_2 + event.name() + SUFFIX_BUTTON),
            resources.getString(FORWARD_RESPONSE + OPTION_INDEX_2 + event.name() + SUFFIX_TOOLTIP));

        return List.of(btnResponseA, btnResponseB, btnResponseC);
    }

    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString(FORWARD_EVENT + event.name() + SUFFIX_MESSAGE),
            commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("result.ooc");
    }
}
