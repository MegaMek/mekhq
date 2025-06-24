package mekhq.gui.dialog.factionStanding.factionJudgment;

import static megamek.common.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQInternationalization;

public class FactionJudgmentSceneDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentSceneDialog";

    private final static String DIALOG_KEY_FORWARD = "FactionJudgmentSceneDialog.";
    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";
    private final static String DIALOG_KEY_AFFIX_PLANETSIDE = "planetside";
    private final static String DIALOG_KEY_AFFIX_IN_TRANSIT = "inTransit";

    private final Campaign campaign;

    public FactionJudgmentSceneDialog(Campaign campaign, Person commander, Person secondCharacter,
          FactionJudgmentSceneType sceneType) {
        this.campaign = campaign;

        new ImmersiveDialogSimple(
              campaign,
              commander,
              secondCharacter,
              getInCharacterText(commander, secondCharacter, sceneType),
              getButtonLabels(sceneType),
              null,
              null,
              false);
    }

    private String getInCharacterText(Person commander, Person secondCharacter, FactionJudgmentSceneType sceneType) {
        Faction campaignFaction = campaign.getFaction();
        String campaignFactionCode = campaignFaction.getShortName();
        boolean isPlanetside = campaign.getLocation().isOnPlanet();

        // COMMANDER
        final PronounData commanderPronounData = new PronounData(commander.getGender());
        // {0} hyperlinked full title
        final String commanderHyperlinkedFullTitle = commander.getHyperlinkedFullTitle();
        // {1} first name
        final String commanderFirstName = commander.getGivenName();
        // {2} = He/She/They
        final String commanderHeSheTheyCapitalized = commanderPronounData.subjectPronoun();
        // {3} = he/she/they
        final String commanderHeSheTheyLowercase = commanderPronounData.subjectPronounLowerCase();
        // {4} = Him/Her/Them
        final String commanderHimHerThemCapitalized = commanderPronounData.objectPronoun();
        // {5} = him/her/them
        final String commanderHimHerThemLowercase = commanderPronounData.objectPronounLowerCase();
        // {6} = His/Her/Their
        final String commanderHisHerTheirCapitalized = commanderPronounData.possessivePronoun();
        // {7} = his/her/their
        final String commanderHisHerTheirLowercase = commanderPronounData.possessivePronounLowerCase();
        // {8} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int commanderPluralizer = commanderPronounData.pluralizer();

        // SECOND
        final PronounData secondPronounData = new PronounData(secondCharacter == null
                                                                    ? Gender.MALE
                                                                    : secondCharacter.getGender());
        // {9} hyperlinked full title
        final String secondHyperlinkedFullTitle = secondCharacter == null
                                                        ? "Sergeant Smith"
                                                        : secondCharacter.getHyperlinkedFullTitle();
        // {10} first name
        final String secondFirstName = secondCharacter == null ? "Smith" : secondCharacter.getGivenName();
        // {11} = He/She/They
        final String secondHeSheTheyCapitalized = secondPronounData.subjectPronoun();
        // {12} = he/she/they
        final String secondHeSheTheyLowercase = secondPronounData.subjectPronounLowerCase();
        // {13} = Him/Her/Them
        final String secondHimHerThemCapitalized = secondPronounData.objectPronoun();
        // {14} = him/her/them
        final String secondHimHerThemLowercase = secondPronounData.objectPronounLowerCase();
        // {15} = His/Her/Their
        final String secondHisHerTheirCapitalized = secondPronounData.possessivePronoun();
        // {16} = his/her/their
        final String secondHisHerTheirLowercase = secondPronounData.possessivePronounLowerCase();
        // {17} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int secondPluralizer = secondCharacter == null ? 0 : secondPronounData.pluralizer();

        // MISC
        // {18} = campaign name
        String campaignName = campaign.getName();
        // {19} = planet name
        String planetName = isPlanetside ? campaign.getLocation().getPlanet().getName(campaign.getLocalDate()) : "";
        // {20} = commander address
        String commanderAddress = campaign.getCommanderAddress(false);

        String seppukuVariant = "." + randomInt(10);
        String dialogKey = DIALOG_KEY_FORWARD
                                 + sceneType.getLookUpName() + '.'
                                 + (isPlanetside ? DIALOG_KEY_AFFIX_PLANETSIDE : DIALOG_KEY_AFFIX_IN_TRANSIT) + '.'
                                 + campaignFactionCode
                                 + (sceneType == FactionJudgmentSceneType.SEPPUKU ? seppukuVariant : "");

        // This will fail if a faction-specific dialog option doesn't exist for this faction, at which point we use a
        // catch-all
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        if (!MHQInternationalization.isResourceKeyValid(testReturn)) {
            String affixKey;
            if (campaignFaction.isClan()) {
                affixKey = DIALOG_KEY_AFFIX_CLAN;
            } else if (campaignFaction.isPeriphery()) {
                affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
            } else {
                affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
            }

            dialogKey = DIALOG_KEY_FORWARD
                              + sceneType.getLookUpName() + '.'
                              + (isPlanetside ? DIALOG_KEY_AFFIX_PLANETSIDE : DIALOG_KEY_AFFIX_IN_TRANSIT) + '.'
                              + affixKey
                              + (sceneType == FactionJudgmentSceneType.SEPPUKU ? seppukuVariant : "");
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commanderHyperlinkedFullTitle, commanderFirstName,
              commanderHeSheTheyCapitalized, commanderHeSheTheyLowercase, commanderHimHerThemCapitalized,
              commanderHimHerThemLowercase, commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase,
              commanderPluralizer, secondHyperlinkedFullTitle, secondFirstName, secondHeSheTheyCapitalized,
              secondHeSheTheyLowercase, secondHimHerThemCapitalized, secondHimHerThemLowercase,
              secondHisHerTheirCapitalized, secondHisHerTheirLowercase, secondPluralizer, campaignName, planetName,
              commanderAddress);
    }

    private static List<String> getButtonLabels(FactionJudgmentSceneType sceneType) {
        String key = "FactionJudgmentSceneDialog.button.";
        String color = "";
        switch (sceneType) {
            case DISBAND -> {
                key += "disband";
                color = getNegativeColor();
            }
            case GO_ROGUE_WARNING, GO_ROGUE_RETIRED, GO_ROGUE_IMPRISONED, GO_ROGUE_REPLACED, GO_ROGUE_DISBAND -> {
                key += "goRogue";
                color = getPositiveColor();
            }
            case SEPPUKU -> {
                key += "seppuku";
                color = getWarningColor();
            }
        }

        return List.of(getFormattedTextAt(RESOURCE_BUNDLE,
              key,
              spanOpeningWithCustomColor(color),
              CLOSING_SPAN_TAG));
    }
}
