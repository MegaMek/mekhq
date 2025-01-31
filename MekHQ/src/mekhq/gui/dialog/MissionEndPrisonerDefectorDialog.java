package mekhq.gui.dialog;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.ArrayList;
import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class MissionEndPrisonerDefectorDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public MissionEndPrisonerDefectorDialog(Campaign campaign) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign),
            createButtons(), createOutOfCharacterMessage(), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
            "cancel.button"), null);
        buttons.add(btnAccept);

        ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
            "continue.button"), null);
        buttons.add(btnDecline);

        return buttons;
    }

    private static String createInCharacterMessage(Campaign campaign) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.message",
            commanderAddress);
    }

    private static Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.ooc");
    }
}
