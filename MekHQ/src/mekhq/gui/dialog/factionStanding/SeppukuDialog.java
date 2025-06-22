package mekhq.gui.dialog.factionStanding;

import static megamek.common.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class SeppukuDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SeppukuDialog";

    private final static String DIALOG_KEY_IN_CHARACTER_SEPPUKU = "FactionCensureEvent.inCharacter.seppuku.";
    private final static String KEY_AFFIX_PLANETSIDE = "planetside.";
    private final static String KEY_AFFIX_IN_TRANSIT = "inTransit.";
    private final static String KEY_AFFIX_SOLO = "solo";

    private final Campaign campaign;
    private final Person mostSeniorCharacter;

    public SeppukuDialog(final Campaign campaign, final Person mostSeniorCharacter) {
        this.campaign = campaign;
        this.mostSeniorCharacter = mostSeniorCharacter;

        new ImmersiveDialogSimple(campaign,
              mostSeniorCharacter,
              null,
              getInCharacterText(),
              null,
              null,
              null,
              false);
    }

    private String getInCharacterText() {
        final Person second = getSeppukuSecond();
        final String keyAffix = campaign.getLocation().isOnPlanet() ? KEY_AFFIX_PLANETSIDE : KEY_AFFIX_IN_TRANSIT;
        final String keyVariant = second == null ? KEY_AFFIX_SOLO : (randomInt(10) + "");
        final String RESOURCE_KEY = DIALOG_KEY_IN_CHARACTER_SEPPUKU + keyAffix + keyVariant;

        // COMMANDER
        final PronounData commanderPronounData = new PronounData(mostSeniorCharacter.getGender());
        // {0} hyperlinked full title
        final String commanderHyperlinkedFullTitle = mostSeniorCharacter.getHyperlinkedFullTitle();
        // {1} first name
        final String commanderFirstName = mostSeniorCharacter.getGivenName();
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
        final PronounData secondPronounData = second == null ? null : new PronounData(second.getGender());
        // {9} hyperlinked full title
        final String secondHyperlinkedFullTitle = second == null ? "" : second.getHyperlinkedFullTitle();
        // {10} first name
        final String secondFirstName = second == null ? "" : second.getGivenName();
        // {11} = He/She/They
        final String secondHeSheTheyCapitalized = second == null ? "" : secondPronounData.subjectPronoun();
        // {12} = he/she/they
        final String secondHeSheTheyLowercase = second == null ? "" : secondPronounData.subjectPronounLowerCase();
        // {13} = Him/Her/Them
        final String secondHimHerThemCapitalized = second == null ? "" : secondPronounData.objectPronoun();
        // {14} = him/her/them
        final String secondHimHerThemLowercase = second == null ? "" : secondPronounData.objectPronounLowerCase();
        // {15} = His/Her/Their
        final String secondHisHerTheirCapitalized = second == null ? "" : secondPronounData.possessivePronoun();
        // {16} = his/her/their
        final String secondHisHerTheirLowercase = second == null ? "" : secondPronounData.possessivePronounLowerCase();
        // {17} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int secondPluralizer = second == null ? 0 : secondPronounData.pluralizer();

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, commanderHyperlinkedFullTitle, commanderFirstName,
              commanderHeSheTheyCapitalized, commanderHeSheTheyLowercase, commanderHimHerThemCapitalized,
              commanderHimHerThemLowercase, commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase,
              commanderPluralizer, secondHyperlinkedFullTitle, secondFirstName, secondHeSheTheyCapitalized,
              secondHeSheTheyLowercase, secondHimHerThemCapitalized, secondHimHerThemLowercase,
              secondHisHerTheirCapitalized, secondHisHerTheirLowercase, secondPluralizer);
    }

    private @Nullable Person getSeppukuSecond() {
        Person second = null;
        for (Person person : campaign.getActivePersonnel(false)) {
            if (person == mostSeniorCharacter) {
                continue;
            }

            if (second == null || person.outRanksUsingSkillTiebreaker(campaign, second)) {
                second = person;
            }
        }

        return second;
    }
}
