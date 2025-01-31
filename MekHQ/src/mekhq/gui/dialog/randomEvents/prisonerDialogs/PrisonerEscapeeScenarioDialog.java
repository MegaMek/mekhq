package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class PrisonerEscapeeScenarioDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public PrisonerEscapeeScenarioDialog(Campaign campaign, StratconTrackState track, StratconCoords coords) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign, track, coords),
            createButtons(), createOutOfCharacterMessage(), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirmation = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
            "successful.button"), null);

        return List.of(btnConfirmation);
    }

    private static String createInCharacterMessage(Campaign campaign, StratconTrackState track,
                                                   StratconCoords coords) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "escapeeScenario.report",
            commanderAddress, track.getDisplayableName(), coords.toBTString());
    }

    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "escapeeScenario.ooc");
    }

    private static Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }
}
