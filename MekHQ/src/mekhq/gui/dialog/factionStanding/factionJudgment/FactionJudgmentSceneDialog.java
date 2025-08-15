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
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFallbackFactionKey;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getInCharacterText;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionJudgmentSceneType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
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

    /**
     * Constructs a faction judgment scene dialog and displays it immediately.
     *
     * @param campaign        the campaign for which the dialog is constructed
     * @param commander       the primary character representing the command
     * @param secondCharacter a secondary character involved in the scene (nullable)
     * @param sceneType       the type of judgment scene to display
     * @param judgingFaction  the faction performing the judgment
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentSceneDialog(Campaign campaign, Person commander, @Nullable Person secondCharacter,
          FactionJudgmentSceneType sceneType, Faction judgingFaction) {
        LocalDate today = campaign.getLocalDate();
        String factionName = judgingFaction.getFullName(today.getYear());
        String campaignName = campaign.getName();
        CurrentLocation location = campaign.getLocation();
        boolean isPlanetside = location.isOnPlanet();
        String locationName = isPlanetside
                                    ? location.getPlanet().getName(today)
                                    : location.getCurrentSystem().getName(today);
        String commanderAddress = campaign.getCommanderAddress(false);


        String dialogKey = getDialogKey(sceneType, judgingFaction);
        String inCharacterText = getInCharacterText(RESOURCE_BUNDLE, dialogKey, commander, secondCharacter,
              factionName, campaignName, locationName, 0, commanderAddress);

        new ImmersiveDialogSimple(
              campaign,
              commander,
              secondCharacter,
              inCharacterText,
              getButtonLabels(sceneType),
              null,
              null,
              false,
              ImmersiveDialogWidth.LARGE);
    }

    /**
     * Constructs and returns the dialog key used to look up text resources for a given judgment scene type and judging
     * faction. If a specific dialog key does not exist in the resource bundle, a fallback key will be generated based
     * on the judging faction.
     *
     * <p>For certain scene types, such as SEPPUKU, a random variant is appended to the key. The method checks if the
     * generated key maps to a valid resource; if not, it falls back to a generic version using
     * {@code getFallbackFactionKey}.
     *
     * @param sceneType      the type of judgment scene
     * @param judgingFaction the faction making the judgment
     *
     * @return the appropriate dialog key for resource lookup, or a fallback if none is found
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getDialogKey(FactionJudgmentSceneType sceneType, Faction judgingFaction) {
        String judgmentTypeLookupName = sceneType.getLookUpName();
        String judgingFactionCode = judgingFaction.getShortName();
        String dialogKey = DIALOG_KEY_FORWARD +
                                 judgmentTypeLookupName + '.' +
                                 judgingFactionCode;

        if (sceneType.equals(FactionJudgmentSceneType.SEPPUKU)) {
            int variant = randomInt(10);
            dialogKey += "." + variant;
        }

        // If testReturn fails, we use a fallback value
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        boolean testReturnIsValid = MHQInternationalization.isResourceKeyValid(testReturn);
        if (testReturnIsValid) {
            return dialogKey;
        }

        return getFallbackFactionKey(dialogKey, judgingFaction);
    }

    /**
     * Returns the set of button labels for the dialog based on the scene type, including color coding for different
     * outcomes.
     *
     * @param sceneType the type of faction judgment scene
     *
     * @return a list of formatted button label strings for display
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<String> getButtonLabels(FactionJudgmentSceneType sceneType) {
        String key = "FactionJudgmentSceneDialog.button.";
        key += sceneType.getLookUpName();

        String color = switch (sceneType) {
            case BARRED, DISBAND, SEPPUKU -> getNegativeColor();
            case GO_ROGUE -> getPositiveColor();
        };

        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, key, spanOpeningWithCustomColor(color),
              CLOSING_SPAN_TAG));
    }
}
