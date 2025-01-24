package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

public class PrisonerEventDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public PrisonerEventDialog(Campaign campaign, @Nullable Person speaker, int eventRoll, boolean isMinor) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, eventRoll, isMinor),
            createButtons(eventRoll, isMinor), createOutOfCharacterMessage(),
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(int eventRoll, boolean isMinor) {
        String typeKey = isMinor ? "Minor" : "Major";

        ButtonLabelTooltipPair btnResponseA = new ButtonLabelTooltipPair(
            resources.getString("response" + typeKey + 'A' + eventRoll + ".button"),
            resources.getString("response" + typeKey + 'A' + eventRoll + ".tooltip"));
        ButtonLabelTooltipPair btnResponseB = new ButtonLabelTooltipPair(
            resources.getString("response" + typeKey + 'B' + eventRoll + ".button"),
            resources.getString("response" + typeKey + 'B' + eventRoll + ".tooltip"));
        ButtonLabelTooltipPair btnResponseC = new ButtonLabelTooltipPair(
            resources.getString("response" + typeKey + 'C' + eventRoll + ".button"),
            resources.getString("response" + typeKey + 'C' + eventRoll + ".tooltip"));

        return List.of(btnResponseA, btnResponseB, btnResponseC);
    }

    private static String createInCharacterMessage(Campaign campaign, int eventRoll, boolean isMinor) {
        String typeKey = isMinor ? "Minor" : "Major";
        String fullResourceKey = "event" + typeKey + eventRoll + ".message";

        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString(fullResourceKey), commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("result.ooc");
    }
}
