package mekhq.campaign.universe.factionStanding;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class PerformBatchall {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PerformBatchall";

    private static final int BATCHALL_OPTIONS_COUNT = 10;
    private static final int INTRO_RESPONSE_COUNT = 5;
    private static final int DIALOG_DECLINE_OPTION_START_INDEX = 3;

    private final Campaign campaign;
    private final Person clanOpponent;
    private final String enemyFactionCode;
    private final FactionStandingLevel standingLevel;
    private final int batchallVersion;

    private boolean isBatchallAccepted = true;

    public PerformBatchall(Campaign campaign, Person clanOpponent, String enemyFactionCode) {
        this.campaign = campaign;
        this.clanOpponent = clanOpponent;
        this.enemyFactionCode = enemyFactionCode;
        standingLevel = getFactionStandingLevel(campaign.getFactionStandings());
        batchallVersion = randomInt(BATCHALL_OPTIONS_COUNT);

        if (getInitialChallengeDialog() < DIALOG_DECLINE_OPTION_START_INDEX) {
            getBatchallFollowUpDialog(false);
            return;
        }

        if (getAreYouSureDialog() >= DIALOG_DECLINE_OPTION_START_INDEX) {
            getBatchallFollowUpDialog(true);
            isBatchallAccepted = false;
        } else {
            getBatchallFollowUpDialog(false);
        }
    }

    private FactionStandingLevel getFactionStandingLevel(FactionStandings factionStanding) {
        double regard = factionStanding.getRegardForFaction(enemyFactionCode, true);
        return FactionStandingUtilities.calculateFactionStandingLevel(regard);
    }

    public boolean isBatchallAccepted() {
        return isBatchallAccepted;
    }

    public int getInitialChallengeDialog() {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              clanOpponent,
              null,
              getBatchallIntroText(),
              getInitialChallengeDialogOptions(),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.intro.ooc"),
              null,
              true);

        return dialog.getDialogChoice();
    }

    public String getBatchallIntroText() {
        final String bundleKey = "performBatchall." + standingLevel.name() + ".batchall." + batchallVersion + ".intro";
        final String campaignName = campaign.getName();
        final String opponentName = clanOpponent == null ? "" : clanOpponent.getFullName();

        Faction opponentClan = Factions.getInstance().getFaction(enemyFactionCode);
        String opponentClanName = opponentClan == null ? "" : opponentClan.getFullName(campaign.getGameYear());
        opponentClanName = opponentClanName.replace(getTextAt(RESOURCE_BUNDLE, "performBatchall.clan.prefix") + ' ',
              "");
        opponentClanName = getFormattedTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.formatted", opponentClanName);

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey, campaignName, opponentName, opponentClanName);
    }

    public List<String> getInitialChallengeDialogOptions() {
        List<String> responses = new ArrayList<>();

        for (int i = 0; i < INTRO_RESPONSE_COUNT; i++) {
            responses.add(getTextAt(RESOURCE_BUNDLE, "performBatchall.intro." + i));
        }

        return responses;
    }

    public void getBatchallFollowUpDialog(boolean isRefuse) {
        String message = getBatchallPostIntroText(isRefuse);

        new ImmersiveDialogSimple(campaign, clanOpponent, null, message, null, null, null, true);
    }

    public String getBatchallPostIntroText(boolean isRefuse) {
        final String keySuffix = isRefuse ? "refuse" : "accept";
        final String bundleKey = "performBatchall." +
                                       standingLevel.name() +
                                       ".batchall." +
                                       batchallVersion +
                                       "." +
                                       keySuffix;

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey);
    }

    public int getAreYouSureDialog() {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              getAreYouSureDialogText(),
              getAreYouSureDialogOptions(),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.outOfCharacter"),
              null,
              true);

        return dialog.getDialogChoice();
    }

    public String getAreYouSureDialogText() {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.inCharacter", commanderAddress);
    }

    public List<String> getAreYouSureDialogOptions() {
        return List.of(getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.confirm"),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.cancel"));
    }
}
