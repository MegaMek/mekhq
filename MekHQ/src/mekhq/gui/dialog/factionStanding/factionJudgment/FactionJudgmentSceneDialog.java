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

import static megamek.common.Compute.randomInt;
import static mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneType.SEPPUKU;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQInternationalization;

/**
 * Displays an immersive dialog for "faction judgment" story scenes.
 *
 * <p>This dialog formats in-character and contextually aware narrative based on campaign and personnel data,
 * including faction, location, involved personnel, and scene type.</p>
 *
 * <p>Responsible for constructing the text content, button labels, and instantiating the dialog.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionJudgmentSceneDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentSceneDialog";

    private final static String DIALOG_KEY_FORWARD = "FactionJudgmentSceneDialog.";
    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";
    private final static String DIALOG_KEY_AFFIX_PLANETSIDE = "planetside";
    private final static String DIALOG_KEY_AFFIX_IN_TRANSIT = "inTransit";

    /** The {@link Campaign} associated with this dialog. */
    private final Campaign campaign;

    /**
     * Constructs a faction judgment scene dialog and displays it immediately.
     *
     * @param campaign        the campaign for which the dialog is constructed
     * @param commander       the primary character representing the command
     * @param secondCharacter a secondary character involved in the scene (nullable)
     * @param sceneType       the type of judgment scene to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentSceneDialog(Campaign campaign, Person commander, @Nullable Person secondCharacter,
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

    /**
     * Constructs the full in-character dialog text for a faction judgment scene by formatting a localized template with
     * campaign and personnel context.
     *
     * @param commander       the primary character for pronoun/identity substitution
     * @param secondCharacter the secondary character for pronoun/identity substitution (nullable)
     * @param sceneType       the type of scene used to select the appropriate template
     *
     * @return the formatted, story-driven dialog text to be displayed
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterText(Person commander, @Nullable Person secondCharacter,
          FactionJudgmentSceneType sceneType) {
        Faction campaignFaction = campaign.getFaction();
        String campaignFactionCode = campaignFaction.getShortName();
        boolean isPlanetside = campaign.getLocation().isOnPlanet();

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
        // {19} = planet name
        String planetName = isPlanetside ? campaign.getLocation().getPlanet().getName(campaign.getLocalDate()) : "";
        // {20} = commander address
        String commanderAddress = campaign.getCommanderAddress(false);

        String seppukuVariant = "." + randomInt(10);
        String dialogKey = DIALOG_KEY_FORWARD
                                 + sceneType.getLookUpName() + '.'
                                 + (isPlanetside ? DIALOG_KEY_AFFIX_PLANETSIDE : DIALOG_KEY_AFFIX_IN_TRANSIT) + '.'
                                 + campaignFactionCode
                                 + (sceneType == SEPPUKU ? seppukuVariant : "");

        // Attempt a faction-localized template first, then fall back to general grouping
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
                              + (sceneType == SEPPUKU ? seppukuVariant : "");
        }

        // Format and return the localized dialog text with the current context.
        return getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commanderHyperlinkedFullTitle, commanderFirstName,
              commanderHeSheTheyCapitalized, commanderHeSheTheyLowercase, commanderHimHerThemCapitalized,
              commanderHimHerThemLowercase, commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase,
              commanderPluralizer, secondHyperlinkedFullTitle, secondFirstName, secondHeSheTheyCapitalized,
              secondHeSheTheyLowercase, secondHimHerThemCapitalized, secondHimHerThemLowercase,
              secondHisHerTheirCapitalized, secondHisHerTheirLowercase, secondPluralizer, campaignName, planetName,
              commanderAddress);
    }

    /**
     * Returns the set of button labels for the dialog based on the scene type, including color coding for different
     * outcomes.
     *
     * @param sceneType the type of faction judgment scene
     * @return a list of formatted button label strings for display
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<String> getButtonLabels(FactionJudgmentSceneType sceneType) {
        String key = "FactionJudgmentSceneDialog.button.";
        key += sceneType.getLookUpName();
        String color = switch (sceneType) {
            case DISBAND -> getNegativeColor();
            case GO_ROGUE -> getPositiveColor();
            case SEPPUKU -> getWarningColor();
        };

        return List.of(getFormattedTextAt(RESOURCE_BUNDLE,
              key,
              spanOpeningWithCustomColor(color),
              CLOSING_SPAN_TAG));
    }
}
