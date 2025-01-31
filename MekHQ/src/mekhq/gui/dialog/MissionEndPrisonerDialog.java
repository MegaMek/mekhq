package mekhq.gui.dialog;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.ArrayList;
import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class MissionEndPrisonerDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public MissionEndPrisonerDialog(Campaign campaign, Money ransom, boolean isAllied,
                                    boolean isSuccess, boolean isGoodEvent) {
        super(campaign, getSpeaker(campaign), null,
            createInCharacterMessage(campaign, ransom, isAllied, isSuccess, isGoodEvent),
            createButtons(isAllied, isSuccess, isGoodEvent),
            createOutOfCharacterMessage(isAllied, isSuccess, isGoodEvent), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(boolean isAllied, boolean isSuccess,
                                                              boolean isGoodEvent) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        boolean isRansom = (!isAllied && isSuccess && isGoodEvent)
                || (!isAllied && !isSuccess && isGoodEvent)
                || (isAllied && !isSuccess && isGoodEvent);

        if (isRansom) {
            ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                "accept.button"), null);
            buttons.add(btnAccept);

            if (isAllied) {
                ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                    "decline.button"), null);
                buttons.add(btnDecline);
            }
        }

        if (!isAllied) {
            ButtonLabelTooltipPair btnReleaseThem = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                "releaseThem.button"), null);
            buttons.add(btnReleaseThem);

            ButtonLabelTooltipPair btnExecuteThem = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                "executeThem.button"), null);
            buttons.add(btnExecuteThem);
        }

        if (isAllied && !isRansom) {
            ButtonLabelTooltipPair btnConfirmation = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                "successful.button"), null);
            buttons.add(btnConfirmation);
        }

        return buttons;
    }

    private static String createInCharacterMessage(Campaign campaign, Money ransomSum, boolean isAllied,
                                                   boolean isSuccess, boolean isGoodEvent) {
        String key = "prisoners."
            + (isAllied ? "player" : "enemy") + '.'
            + (isSuccess ? "victory" : "defeat") + '.'
            + (isGoodEvent ? "good" : "bad");

        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, key,
            commanderAddress, ransomSum.toAmountAndSymbolString());
    }

    private static Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    private static @Nullable String createOutOfCharacterMessage(boolean isAllied, boolean isSuccess,
                                                                boolean isGoodEvent) {
        boolean showMessage = (isAllied && !isSuccess && isGoodEvent);

        if (showMessage) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "prisoners.ransom.ooc");
        } else {
            return null;
        }
    }
}
