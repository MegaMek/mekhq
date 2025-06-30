/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionCensureAction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.gui.dialog.NewsDialog;
import mekhq.utilities.MHQInternationalization;

/**
 * This class is responsible for generating and displaying a news article dialog about a faction censure event. It
 * composes an in-character news article based on the participants and the nature of the censure and immediately
 * presents it to the player.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionCensureNewsArticle {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureNewsArticle";

    private final static String DIALOG_KEY_FORWARD = "FactionCensureNewsArticle.";
    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";

    private final Campaign campaign;

    /**
     * Constructs a new {@link FactionCensureNewsArticle} and immediately displays a {@link NewsDialog} dialog
     * describing the censure event.
     *
     * @param campaign         the current {@link Campaign}
     * @param commander        the {@link Person} who is the subject of the censure
     * @param secondInCommand  the {@link Person} acting as the second-in-command or witness
     * @param censureAction    the {@link FactionCensureAction} describing the censure type
     * @param censuringFaction the {@link Faction} delivering the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureNewsArticle(Campaign campaign, Person commander, Person secondInCommand,
          FactionCensureAction censureAction, Faction censuringFaction) {
        this.campaign = campaign;

        String inCharacterText = getInCharacterText(commander, secondInCommand, censureAction, censuringFaction);
        new NewsDialog(campaign, inCharacterText);
    }

    /**
     * Constructs the full in-character dialog text for a faction judgment scene by formatting a localized template with
     * campaign and personnel context.
     *
     * @param commander        the primary character for pronoun/identity substitution
     * @param secondCharacter  the secondary character for pronoun/identity substitution (nullable)
     * @param censureAction    the action being reported
     * @param censuringFaction the faction performing the censure
     *
     * @return the formatted, story-driven dialog text to be displayed
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterText(Person commander, @Nullable Person secondCharacter,
          FactionCensureAction censureAction, Faction censuringFaction) {
        // COMMANDER pronoun/identity context
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

        // SECOND pronoun/identity context
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

        // Miscellaneous campaign context
        // {18} = campaign name
        String campaignName = campaign.getName();
        // {19} = faction name
        String factionName = FactionStandingUtilities.getFactionName(censuringFaction, campaign.getGameYear());

        String dialogKey = DIALOG_KEY_FORWARD + censureAction.getLookupName() + '.' + censuringFaction.getShortName();

        // Attempt a faction-localized template first, then fall back to general grouping
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        if (!MHQInternationalization.isResourceKeyValid(testReturn)) {
            String affixKey;
            if (censuringFaction.isClan()) {
                affixKey = DIALOG_KEY_AFFIX_CLAN;
            } else if (censuringFaction.isPeriphery()) {
                affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
            } else {
                affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
            }

            dialogKey = DIALOG_KEY_FORWARD + censureAction.getLookupName() + '.' + affixKey;
        }

        // Format and return the localized dialog text with the current context.
        return getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commanderHyperlinkedFullTitle, commanderFirstName,
              commanderHeSheTheyCapitalized, commanderHeSheTheyLowercase, commanderHimHerThemCapitalized,
              commanderHimHerThemLowercase, commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase,
              commanderPluralizer, secondHyperlinkedFullTitle, secondFirstName, secondHeSheTheyCapitalized,
              secondHeSheTheyLowercase, secondHimHerThemCapitalized, secondHimHerThemLowercase,
              secondHisHerTheirCapitalized, secondHisHerTheirLowercase, secondPluralizer, campaignName, factionName);
    }
}
