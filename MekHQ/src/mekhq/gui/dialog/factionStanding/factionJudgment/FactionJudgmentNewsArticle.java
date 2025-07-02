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

public class FactionJudgmentNewsArticle {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentNewsArticle";

    private final static String KEY_FORWARD = "FactionCensureNewsArticle.";

    private final static String KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String KEY_AFFIX_PERIPHERY = "periphery";
    private final static String KEY_AFFIX_CLAN = "clan";

    public FactionJudgmentNewsArticle(Campaign campaign, Person commander, Person secondInCommand, String lookupName,
          Faction censuringFaction, FactionStandingJudgmentType judgmentType, boolean useFactionCapitalAsLocation) {

        String dialogKey = getDialogKey(judgmentType, lookupName, censuringFaction);
        String factionName = getFactionName(censuringFaction, campaign.getGameYear());

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

    private static String getDialogKey(FactionStandingJudgmentType judgmentType, String lookupName,
          Faction judgingFaction) {
        String censuringFactionCode = judgingFaction.getShortName();
        String dialogKey = KEY_FORWARD + judgmentType.getLookupName() + '.' + lookupName + '.' + censuringFactionCode;

        // If testReturn fails, we use a fallback value
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        if (!MHQInternationalization.isResourceKeyValid(testReturn)) {
            String affixKey;
            if (judgingFaction.isClan()) {
                affixKey = KEY_AFFIX_CLAN;
            } else if (judgingFaction.isPeriphery()) {
                affixKey = KEY_AFFIX_PERIPHERY;
            } else {
                affixKey = KEY_AFFIX_INNER_SPHERE;
            }

            dialogKey = dialogKey.replace('.' + censuringFactionCode, '.' + affixKey);
        }

        return dialogKey;
    }
}
