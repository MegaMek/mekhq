package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.*;

import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;

public class PrisonerEventDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public PrisonerEventDialog(Campaign campaign, int prisonerPortion, int eventRoll, boolean isMinor) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign, eventRoll, isMinor),
            createButtons(eventRoll, prisonerPortion, isMinor), createOutOfCharacterMessage(), 0,
            null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(int eventRoll, int prisonerPortion,
                                                              boolean isMinor) {
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

        if (isMinor) {
            ButtonLabelTooltipPair btnFree = new ButtonLabelTooltipPair(
                String.format(resources.getString("free.button"), prisonerPortion),
                resources.getString("free.tooltip"));
            ButtonLabelTooltipPair btnExecute = new ButtonLabelTooltipPair(
                String.format(resources.getString("execute.button"), prisonerPortion),
                resources.getString("execute.tooltip"));

            return List.of(btnResponseA, btnResponseB, btnResponseC, btnFree, btnExecute);
        }

        return List.of(btnResponseA, btnResponseB, btnResponseC);
    }

    private static @Nullable Person getSpeaker(Campaign campaign) {
        List<Force> securityForces = new ArrayList<>();

        for (Force force : campaign.getAllForces()) {
            if (force.getForceType().isSecurity()) {
                securityForces.add(force);
            }
        }

        Collections.shuffle(securityForces);
        Force designatedForce = securityForces.get(0);

        Person speaker = null;
        UUID speakerId = designatedForce.getForceCommanderID();
        if (speakerId != null) {
            speaker = campaign.getPerson(speakerId);
        }

        if (speaker == null) {
            return campaign.getSeniorAdminPerson(TRANSPORT);
        } else {
            return speaker;
        }
    }

    private static String createInCharacterMessage(Campaign campaign, int eventRoll, boolean isMinor) {
        String typeKey = isMinor ? "Minor" : "Major";

        String commanderAddress = campaign.getCommanderAddress(false);
        return String.format(resources.getString("event" + typeKey + eventRoll + ".message"),
            commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return resources.getString("message.ooc");
    }
}
