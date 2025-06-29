package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.PRESS_RECOGNITION;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.TRIUMPH_OR_REMEMBRANCE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.NewsDialog;
import mekhq.utilities.MHQInternationalization;

public class FactionAccoladePropagandaDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private final static String DIALOG_KEY = "FactionAccoladeDialog.message.";
    private final static String AFFIX_INNER_SPHERE = "innerSphere";
    private final static String AFFIX_CLAN = "clan";
    private final static String AFFIX_PERIPHERY = "periphery";
    private final static String AFFIX_INTO = ".intro.";
    private final static String AFFIX_NORMAL = "normal";
    private final static String AFFIX_BUTTON = "button";

    private final Campaign campaign;
    private final Faction faction;

    public FactionAccoladePropagandaDialog(Campaign campaign, Faction faction, Person commander,
          FactionAccoladeLevel accoladeLevel) {
        this.campaign = campaign;
        this.faction = faction;

        if (!accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE)
                  || (accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE) && !faction.isClan())) {
            processIntroduction(accoladeLevel);
        }

        processPropagandaReel(accoladeLevel, commander);
    }

    private void processIntroduction(FactionAccoladeLevel accoladeLevel) {
        String dialogKey = DIALOG_KEY +
                                 accoladeLevel.name() +
                                 AFFIX_INTO +
                                 (faction.isClan() ? AFFIX_CLAN : AFFIX_NORMAL);

        String holovidContents = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, campaign.getCommanderAddress(false),
              faction.getFullName(campaign.getGameYear()));

        String buttonLabel = getTextAt(RESOURCE_BUNDLE, DIALOG_KEY + accoladeLevel.name() + AFFIX_INTO + AFFIX_BUTTON);

        new ImmersiveDialogSimple(campaign,
              campaign.getSecondInCommand(),
              null,
              holovidContents,
              List.of(buttonLabel),
              null,
              null,
              false);
    }

    private void processPropagandaReel(FactionAccoladeLevel accoladeLevel, Person commander) {
        String dialogKey = getMessageKey(accoladeLevel);

        String holovidContents = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commander.getFullTitle(),
              campaign.getName(), faction.getFullName(campaign.getGameYear()));

        String buttonLabel = getTextAt(RESOURCE_BUNDLE, DIALOG_KEY + accoladeLevel + '.' + AFFIX_BUTTON);

        if (accoladeLevel.is(PRESS_RECOGNITION)
                  || (accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE) && !faction.isClan())) {
            new NewsDialog(campaign, holovidContents);
        } else {
            new ImmersiveDialogSimple(campaign,
                  null,
                  null,
                  holovidContents,
                  List.of(buttonLabel),
                  null,
                  null,
                  false,
                  true);
        }
    }

    private String getMessageKey(FactionAccoladeLevel accoladeLevel) {
        String key = DIALOG_KEY + accoladeLevel + '.' + faction.getShortName();

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

        return DIALOG_KEY + accoladeLevel + '.' + affix;
    }
}
