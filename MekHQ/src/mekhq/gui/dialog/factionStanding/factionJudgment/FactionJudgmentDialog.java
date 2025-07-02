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
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.utilities.MHQInternationalization;

public class FactionJudgmentDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentDialog";

    private final static String DIALOG_KEY_FORWARD = "FactionJudgmentDialog.message";
    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";

    private final static String BUTTON_KEY_FORWARD = "FactionJudgmentDialog.button";
    private final static String BUTTON_KEY_POSITIVE = "positive";
    private final static String BUTTON_KEY_NEUTRAL = "neutral";
    private final static String BUTTON_KEY_NEGATIVE = "negative";
    private final static String BUTTON_KEY_GO_ROGUE = "goRogue";
    private final static String BUTTON_KEY_SEPPUKU = "seppuku";

    private final static String DRACONIS_COMBINE_FACTION_CODE = "DC";

    int responseIndex;

    public int getChoiceIndex() {
        return responseIndex;
    }

    public static String getFactionJudgmentDialogResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    public FactionJudgmentDialog(Campaign campaign, @Nullable Person speaker, @Nullable Person commander,
          String judgmentLookupName, Faction judgingFaction, FactionStandingJudgmentType judgmentType,
          ImmersiveDialogWidth dialogWidth, @Nullable String outOfCharacterText, @Nullable Integer moneyReward) {
        final String judgmentTypeLookupName = judgmentType.getLookupName();

        // Dialog
        String commanderAddress = campaign.getCommanderAddress(false);
        String dialogKey = getDialogKey(judgmentTypeLookupName, judgmentLookupName, judgingFaction);
        String factionName = getFactionName(judgingFaction, campaign.getGameYear());

        LocalDate today = campaign.getLocalDate();
        CurrentLocation location = campaign.getLocation();
        boolean isPlanetside = location.isOnPlanet();
        String locationName = isPlanetside
                                    ? location.getPlanet().getName(today)
                                    : location.getCurrentSystem().getName(today);

        String dialogText = getInCharacterText(RESOURCE_BUNDLE, dialogKey, commander, null, factionName,
              campaign.getName(), locationName, moneyReward, commanderAddress);

        // Buttons
        boolean includeGoRogueOption = judgmentType.equals(FactionStandingJudgmentType.CENSURE);

        boolean isDraconisCombineCampaign = campaign.getFaction().getShortName().equals(DRACONIS_COMBINE_FACTION_CODE);
        boolean includeSeppukuOption = includeGoRogueOption && isDraconisCombineCampaign;

        List<String> buttonLabels = getButtonLabels(judgmentLookupName, includeGoRogueOption, includeSeppukuOption);

        // Dialog
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign, speaker, null, dialogText,
              buttonLabels, outOfCharacterText, null, true, dialogWidth);

        responseIndex = dialog.getDialogChoice();
    }

    private static String getDialogKey(String judgmentTypeLookupName, String lookupName, Faction judgingFaction) {
        String censuringFactionCode = judgingFaction.getShortName();
        String dialogKey = DIALOG_KEY_FORWARD +
                                 '.' +
                                 judgmentTypeLookupName +
                                 '.' +
                                 lookupName +
                                 '.' +
                                 censuringFactionCode;

        // If testReturn fails, we use a fallback value
        String testReturn = getTextAt(RESOURCE_BUNDLE, dialogKey);
        if (!MHQInternationalization.isResourceKeyValid(testReturn)) {
            String affixKey;
            if (judgingFaction.isClan()) {
                affixKey = DIALOG_KEY_AFFIX_CLAN;
            } else if (judgingFaction.isPeriphery()) {
                affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
            } else {
                affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
            }

            dialogKey = dialogKey.replace('.' + censuringFactionCode, '.' + affixKey);
        }

        return dialogKey;
    }

    private static List<String> getButtonLabels(String judgmentLookupName, boolean includeGoRogueOption,
          boolean includeSeppukuOption) {
        List<String> buttonLabels = new ArrayList<>();

        String buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_POSITIVE + '.' + judgmentLookupName;
        String positiveButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
        buttonLabels.add(positiveButtonLabel);

        buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_NEUTRAL + '.' + judgmentLookupName;
        String neutralButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
        buttonLabels.add(neutralButtonLabel);

        buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_NEGATIVE + '.' + judgmentLookupName;
        String negativeButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
        buttonLabels.add(negativeButtonLabel);

        if (includeGoRogueOption) {
            buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_GO_ROGUE + '.' + judgmentLookupName;
            String goRogueButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
            buttonLabels.add(goRogueButtonLabel);

            // You shouldn't have the seppuku option without the going rogue option due to the way options index.
            // Otherwise, we run the risk of players selecting 'commit seppuku' and it being treated as the campaign
            // going rogue.
            if (includeSeppukuOption) {
                buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_SEPPUKU + '.' + judgmentLookupName;
                String seppukuButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
                buttonLabels.add(seppukuButtonLabel);
            }
        }

        return buttonLabels;
    }
}
