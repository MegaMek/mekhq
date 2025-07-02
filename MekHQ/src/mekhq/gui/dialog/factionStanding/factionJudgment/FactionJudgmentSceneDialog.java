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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionJudgmentSceneType;
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

    private final Campaign campaign;

    /**
     * Constructs a faction judgment scene dialog and displays it immediately.
     *
     * @param campaign        the campaign for which the dialog is constructed
     * @param commander       the primary character representing the command
     * @param secondCharacter a secondary character involved in the scene (nullable)
     * @param sceneType       the type of judgment scene to display
     * @param censuringFaction the faction performing the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentSceneDialog(Campaign campaign, Person commander, @Nullable Person secondCharacter,
          FactionJudgmentSceneType sceneType, Faction censuringFaction) {
        this.campaign = campaign;

        boolean isPlanetside = campaign.getLocation().isOnPlanet();
        String dialogKey = getDialogKey(sceneType, censuringFaction, isPlanetside);

        //        new ImmersiveDialogSimple(
        //              campaign,
        //              commander,
        //              secondCharacter,
        //              getInCharacterText(commander, secondCharacter, sceneType, censuringFaction),
        //              getButtonLabels(sceneType),
        //              null,
        //              null,
        //              false,
        //              ImmersiveDialogWidth.MEDIUM);
    }

    private static String getDialogKey(FactionJudgmentSceneType sceneType, Faction censuringFaction,
          boolean isPlanetside) {
        String variant = "." + randomInt(10);

        String dialogKey = "";

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

            //            dialogKey = DIALOG_KEY_FORWARD
            //                              + sceneType.getLookUpName() + '.'
            //                              + (isPlanetside ? DIALOG_KEY_AFFIX_PLANETSIDE : DIALOG_KEY_AFFIX_IN_TRANSIT) + '.'
            //                              + affixKey
            //                              + (sceneType == SEPPUKU ? seppukuVariant : "");
        }
        return dialogKey;
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
            case BARRED, CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL, DISBAND, SEPPUKU -> getNegativeColor();
            case GO_ROGUE -> getPositiveColor();
            case CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL -> getWarningColor();
        };

        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, key, spanOpeningWithCustomColor(color),
              CLOSING_SPAN_TAG));
    }
}
