package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.NewsDialog;
import mekhq.utilities.MHQInternationalization;

public class FactionAccoladePropagandaDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private final static String DIALOG_KEY = "FactionAccoladeDialog.message.";
    private final static String PRESS_RECOGNITION = "PRESS_RECOGNITION";
    private final static String PROPAGANDA_REEL = "PROPAGANDA_REEL";
    private final static String AFFIX_INNER_SPHERE = "innerSphere";
    private final static String AFFIX_CLAN = "clan";
    private final static String AFFIX_PERIPHERY = "periphery";
    private final static String AFFIX_INTO = ".intro.";
    private final static String AFFIX_NORMAL = "normal";
    private final static String AFFIX_BUTTON = "button";

    private final Campaign campaign;
    private final Faction faction;

    public FactionAccoladePropagandaDialog(Campaign campaign, Faction faction, Person commander,
          boolean isPressRelease) {
        this.campaign = campaign;
        this.faction = faction;

        processIntroduction(isPressRelease);
        processPropagandaReel(isPressRelease, commander);
    }

    private void processIntroduction(boolean isPressRelease) {
        String dialogKey = DIALOG_KEY +
                                 (isPressRelease ? PRESS_RECOGNITION : PROPAGANDA_REEL) +
                                 AFFIX_INTO +
                                 (faction.isClan() ? AFFIX_CLAN : AFFIX_NORMAL);

        String holovidContents = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, campaign.getCommanderAddress(false),
              faction.getFullName(campaign.getGameYear()));

        String buttonLabel = getTextAt(RESOURCE_BUNDLE, DIALOG_KEY +
                                                              (isPressRelease ? PRESS_RECOGNITION : PROPAGANDA_REEL) +
                                                              AFFIX_INTO +
                                                              AFFIX_BUTTON);

        new ImmersiveDialogSimple(campaign,
              campaign.getSecondInCommand(),
              null,
              holovidContents,
              List.of(buttonLabel),
              null,
              null,
              false);
    }

    private void processPropagandaReel(boolean isPressRelease, Person commander) {
        String dialogKey = getMessageKey(isPressRelease);

        String holovidContents = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commander.getFullTitle(),
              campaign.getName(), faction.getFullName(campaign.getGameYear()));

        ImmersiveDialogCore.ButtonLabelTooltipPair buttonLabelTooltipPair = new ImmersiveDialogCore.ButtonLabelTooltipPair(
              getTextAt(RESOURCE_BUNDLE, DIALOG_KEY + PROPAGANDA_REEL + '.' + AFFIX_BUTTON), null);

        if (isPressRelease) {
            new NewsDialog(campaign, holovidContents);
        } else {
            new ImmersiveDialogCore(campaign,
                  null,
                  null,
                  holovidContents,
                  List.of(buttonLabelTooltipPair),
                  null,
                  UIUtil.scaleForGUI(600),
                  false,
                  null,
                  null,
                  true);
        }
    }

    private String getMessageKey(boolean isPressRelease) {
        String key = DIALOG_KEY + (isPressRelease ? PRESS_RECOGNITION : PROPAGANDA_REEL) + '.' + faction.getShortName();

        String testReturn = getTextAt(RESOURCE_BUNDLE, key);
        if (MHQInternationalization.isResourceKeyValid(testReturn)) {
            return key;
        }

        String affix = AFFIX_INNER_SPHERE;
        if (faction.isClan()) {
            affix = AFFIX_CLAN;
        } else if (faction.isPeriphery()) {
            affix = AFFIX_PERIPHERY;
        }

        return DIALOG_KEY + (isPressRelease ? PRESS_RECOGNITION : PROPAGANDA_REEL) + '.' + affix;
    }
}
