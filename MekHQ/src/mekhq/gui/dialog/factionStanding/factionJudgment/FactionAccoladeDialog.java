package mekhq.gui.dialog.factionStanding.factionJudgment;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQInternationalization;

import java.util.ArrayList;
import java.util.List;

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_LANCE;
import static mekhq.utilities.MHQInternationalization.*;

public class FactionAccoladeDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private final static String BUTTON_KEY = "FactionAccoladeDialog.message.";
    private final static String BUTTON_AFFIX_POSITIVE = ".positive";
    private final static String BUTTON_AFFIX_NEUTRAL = ".neutral";
    private final static String BUTTON_AFFIX_NEGATIVE = ".negative";

    private final static String DIALOG_KEY_MESSAGE = "FactionAccoladeDialog.message.";
    private final static String DIALOG_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_AFFIX_CLAN = "clan";
    private final static String DIALOG_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_AFFIX_ADOPTION = ".adoption";
    private final static String DIALOG_AFFIX_LANCE = ".lance";

    private final static int DIALOG_CHOICE_REFUSE = 2;

    private final Campaign campaign;
    private final String factionCode;
    private final boolean wasRefused;

    public boolean wasRefused() {
        return wasRefused;
    }

    public FactionAccoladeDialog(Campaign campaign, String factionCode, FactionAccoladeLevel accoladeLevel,
          boolean isSameFaction, Person commander) {
        this.campaign = campaign;
        this.factionCode = factionCode;

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
                getSpeaker(accoladeLevel),
                null,
                getInCharacterMessage(accoladeLevel, isSameFaction),
                getButtons(accoladeLevel, isSameFaction),
              null,
              null,
              true);

        wasRefused = dialog.getDialogChoice() == DIALOG_CHOICE_REFUSE;
    }

    private @Nullable Person getSpeaker(FactionAccoladeLevel accoladeLevel) {
        Faction faction = Factions.getInstance().getFaction(factionCode);
        if (faction == null) {
            return null;
        }

        PersonnelRole primaryRole = faction.isClan() ? PersonnelRole.MEKWARRIOR : switch (accoladeLevel) {
            case LETTER_OF_DISTINCTION,
                 ADOPTION_OR_LANCE,
                 TRIUMPH_OR_REMEMBRANCE,
                 STATUE_OR_SIBKO,
                 LETTER_FROM_HEAD_OF_STATE -> PersonnelRole.NOBLE;
            default -> PersonnelRole.MILITARY_LIAISON;
        };

        Person speaker = campaign.newPerson(primaryRole, factionCode, Gender.RANDOMIZE);

        // Clan-specific attributes
        if (faction.isClan()) {
            Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());
            if (bloodname != null) {
                speaker.setBloodname(bloodname.getName());
            }
        } else {
            speaker.setSecondaryRole(PersonnelRole.MEKWARRIOR);
        }

        // Determine rank system
        RankSystem rankSystem;
        if (faction.isClan()) {
            rankSystem = Ranks.getRankSystemFromCode("CLAN");
        } else {
            rankSystem = faction.getRankSystem();
        }

        // Validate and set the rank system
        RankValidator rankValidator = new RankValidator();
        if (rankValidator.validate(rankSystem, false)) {
            speaker.setRankSystem(rankValidator, rankSystem);
            speaker.setRank(38);
        }

        return speaker;
    }

    private String getInCharacterMessage(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String messageKey = getMessageKey(accoladeLevel, isSameFaction);
        return getFormattedTextAt(RESOURCE_BUNDLE, messageKey);
    }

    private String getMessageKey(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String key = DIALOG_KEY_MESSAGE + accoladeLevel.name() + '.' + factionCode;
        if (accoladeLevel.is(ADOPTION_OR_LANCE)) {
            key += isSameFaction ? DIALOG_AFFIX_LANCE : DIALOG_AFFIX_ADOPTION;
        }

        String testReturn = getTextAt(RESOURCE_BUNDLE, key);
        if (MHQInternationalization.isResourceKeyValid(testReturn)) {
            return key;
        }

        Faction faction = Factions.getInstance().getFaction(factionCode);
        String affix = DIALOG_AFFIX_INNER_SPHERE;
        if (faction != null) {
            if (faction.isClan()) {
                affix = DIALOG_AFFIX_CLAN;
            } else if (faction.isPeriphery()) {
                affix = DIALOG_AFFIX_PERIPHERY;
            }
        }

        return key.replace(accoladeLevel.name() + '.' + factionCode,
              accoladeLevel.name() + '.' + affix);
    }

    private List<String> getButtons(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        List<String> buttonLabels = new ArrayList<>();

        String baseKey = BUTTON_KEY + accoladeLevel.name();

        String affix = "";
        if (accoladeLevel.is(ADOPTION_OR_LANCE)) {
            affix = isSameFaction ? DIALOG_AFFIX_LANCE : DIALOG_AFFIX_ADOPTION;
        }

        buttonLabels.add(getFormattedText(RESOURCE_BUNDLE, baseKey + affix + BUTTON_AFFIX_POSITIVE));
        buttonLabels.add(getFormattedText(RESOURCE_BUNDLE, baseKey + affix + BUTTON_AFFIX_NEUTRAL));
        buttonLabels.add(getFormattedText(RESOURCE_BUNDLE, baseKey + affix + BUTTON_AFFIX_NEGATIVE));

        return buttonLabels;
    }
}
