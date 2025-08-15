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

import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFactionName;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFallbackFactionKey;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getInCharacterText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.gui.dialog.NewsDialog;
import mekhq.utilities.MHQInternationalization;

/**
 * Generates and displays a news article dialog related to a judgment event against a faction, including contextual
 * details such as faction name, location, and participants.
 *
 * <p>This class is used to create immersive, in-character news reports that notify the player about significant
 * faction standings within the campaign.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionJudgmentNewsArticle {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentNewsArticle";

    private final static String KEY_FORWARD = "FactionCensureNewsArticle.";

    /**
     * Constructs and immediately displays a news dialog representing a faction judgment event, assembling the news
     * article based on campaign state, involved personnel, and faction information.
     *
     * @param campaign                    The current campaign instance.
     * @param commander                   The commander referenced in the article.
     * @param secondInCommand             The second-in-command personnel, optionally referenced in the article.
     * @param judgmentLookupName          Used to fetch the article
     * @param censuringFaction            The faction issuing the judgment.
     * @param judgmentType                The specific type of judgment.
     * @param useFactionCapitalAsLocation If {@code true}, the faction's capital planet is used as the event location;
     *                                    otherwise, the campaign's current location is used.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentNewsArticle(Campaign campaign, Person commander, Person secondInCommand,
          String judgmentLookupName, Faction censuringFaction, FactionStandingJudgmentType judgmentType,
          boolean useFactionCapitalAsLocation) {
        String factionName = getFactionName(censuringFaction, campaign.getGameYear());

        constructDialog(campaign,
              commander,
              secondInCommand,
              judgmentLookupName,
              censuringFaction,
              judgmentType,
              useFactionCapitalAsLocation,
              factionName);
    }

    /**
     * Constructs and immediately displays a news dialog representing a faction judgment event, assembling the news
     * article based on campaign state, involved personnel, and faction information.
     *
     * @param campaign                    The current campaign instance.
     * @param commander                   The commander referenced in the article.
     * @param secondInCommand             The second-in-command personnel, optionally referenced in the article.
     * @param judgmentLookupName          Used to fetch the article
     * @param censuringFaction            The faction issuing the judgment.
     * @param judgmentType                The specific type of judgment.
     * @param useFactionCapitalAsLocation If {@code true}, the faction's capital planet is used as the event location;
     *                                    otherwise, the campaign's current location is used.
     * @param newFaction                  The faction the campaign is changing to (for going rogue events)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentNewsArticle(Campaign campaign, Person commander, Person secondInCommand,
          String judgmentLookupName, Faction censuringFaction, FactionStandingJudgmentType judgmentType,
          boolean useFactionCapitalAsLocation, Faction newFaction) {
        String factionName = getFactionName(newFaction, campaign.getGameYear());

        constructDialog(campaign,
              commander,
              secondInCommand,
              judgmentLookupName,
              censuringFaction,
              judgmentType,
              useFactionCapitalAsLocation,
              factionName);
    }

    /**
     * Constructs and displays the faction judgment news dialog with contextualized in-character content.
     *
     * <p>This method gathers relevant campaign data—including commander identity, faction names, location details,
     * and judgment information—and uses it to generate a formatted news article for display to the player.</p>
     *
     * <p>It determines the location of the event based on either the faction’s capital or the current campaign
     * position, then formats a localized article using the resource bundle and launches a {@link NewsDialog}.</p>
     *
     * @param campaign                    The current {@link Campaign} instance containing game state and context.
     * @param commander                   The {@link Person} identified as the commander in the article.
     * @param secondInCommand             The optional second-in-command {@link Person}; may be {@code null}.
     * @param judgmentLookupName          The string identifier used to select the specific judgment article.
     * @param censuringFaction            The {@link Faction} issuing the judgment or censure.
     * @param judgmentType                The {@link FactionStandingJudgmentType} describing the kind of judgment.
     * @param useFactionCapitalAsLocation If {@code true}, the location will use the faction’s capital planet; if
     *                                    {@code false}, the campaign’s current location will be used instead.
     * @param factionName                 The name of the faction being referenced in the article (may differ from the
     *                                    censuring faction in rogue events).
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void constructDialog(Campaign campaign, Person commander, Person secondInCommand,
          String judgmentLookupName, Faction censuringFaction, FactionStandingJudgmentType judgmentType,
          boolean useFactionCapitalAsLocation, String factionName) {
        String dialogKey = getDialogKey(judgmentType, judgmentLookupName, censuringFaction);

        LocalDate today = campaign.getLocalDate();
        CurrentLocation location = campaign.getLocation();

        String locationName;
        if (useFactionCapitalAsLocation) {
            PlanetarySystem capital = censuringFaction.getStartingPlanet(campaign, today);
            locationName = capital.getName(today);
        } else {
            boolean isPlanetside = location.isOnPlanet();
            locationName = isPlanetside
                                 ? location.getPlanet().getName(today)
                                 : location.getCurrentSystem().getName(today);
        }

        String commanderAddress = campaign.getCommanderAddress(false);

        String newsReport = getInCharacterText(RESOURCE_BUNDLE, dialogKey, commander, secondInCommand, factionName,
              campaign.getName(), locationName, null, commanderAddress);

        new NewsDialog(campaign, newsReport);
    }

    /**
     * Determines the correct localization key used to fetch the appropriate news template, falling back to general
     * groupings if a specific key is not available.
     *
     * @param judgmentType       The type of judgment or censure.
     * @param judgmentLookupName Added detail to refine the key.
     * @param judgingFaction     The faction issuing the judgment.
     *
     * @return The appropriate resource bundle key for the news article.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getDialogKey(FactionStandingJudgmentType judgmentType, String judgmentLookupName,
          Faction judgingFaction) {
        String censuringFactionCode = judgingFaction.getShortName();
        String dialogKey = KEY_FORWARD +
                                 judgmentType.getLookupName() +
                                 '.' +
                                 judgmentLookupName +
                                 '.' +
                                 censuringFactionCode;

        // If testReturn fails, we use a fallback value
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        boolean testReturnIsValid = MHQInternationalization.isResourceKeyValid(testReturn);
        if (testReturnIsValid) {
            return dialogKey;
        }

        return getFallbackFactionKey(dialogKey, judgingFaction);
    }
}
