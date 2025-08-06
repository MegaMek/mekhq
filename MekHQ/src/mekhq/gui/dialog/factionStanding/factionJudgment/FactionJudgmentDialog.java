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

/**
 * Provides a dialog for rendering the result of a factional judgment event in the campaign. This dialog allows the
 * player to interact with faction decisions, such as being censured, by presenting immersive in-character text and
 * contextual button choices. The available choices can vary depending on the type of judgment, the judging faction, and
 * the specific conditions in the campaign scenario.
 *
 * <p>This class is primarily responsible for assembling the dialog, populating it with localized text and button
 * labels, and capturing the user's response. It is typically invoked when a campaign event triggers a significant
 * factional standing change or disciplinary judgment.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionJudgmentDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionJudgmentDialog";

    private final static String DIALOG_KEY_FORWARD = "FactionJudgmentDialog.message";
    private final static String BUTTON_KEY_FORWARD = "FactionJudgmentDialog.button";
    private final static String BUTTON_KEY_POSITIVE = "positive";
    private final static String BUTTON_KEY_NEUTRAL = "neutral";
    private final static String BUTTON_KEY_NEGATIVE = "negative";
    private final static String BUTTON_KEY_GO_ROGUE = "goRogue";
    private final static String BUTTON_KEY_SEPPUKU = "seppuku";

    private final static String DRACONIS_COMBINE_FACTION_CODE = "DC";

    int responseIndex;

    /**
     * Gets the index of the button chosen by the user as a response to the faction judgment dialog.
     *
     * @return the numeric index of the selected button, corresponding to the choices presented.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getChoiceIndex() {
        return responseIndex;
    }

    /**
     * Returns the resource bundle location used for obtaining localized string values.
     *
     * @return a string key that identifies the dialog's resource bundle.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getFactionJudgmentDialogResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    /**
     * Constructs and displays a FactionJudgmentDialog based on a set of campaign and judgment parameters.
     *
     * <p>This method determines the dialog and button text based on faction, type of judgment, and campaign context,
     * and presents the dialog to the user, capturing their selected option.</p>
     *
     * @param campaign           the current {@link Campaign} instance in which the judgment is occurring
     * @param speaker            the {@link Person} who will give the dialog speech (can be null)
     * @param commander          the {@link Person} who is being judged or associated with the dialog (can be null)
     * @param judgmentLookupName a string identifier for the specific type of judgment event
     * @param judgingFaction     the {@link Faction} making the judgment
     * @param judgmentType       the {@link FactionStandingJudgmentType} describing the type of judgment
     * @param dialogWidth        the width to use for the dialog UI
     * @param outOfCharacterText any additional text to be shown outside of character context (can be null)
     * @param moneyReward        an optional monetary reward tied to the judgment (can be null)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgmentDialog(Campaign campaign, @Nullable Person speaker, @Nullable Person commander,
          String judgmentLookupName, Faction judgingFaction, FactionStandingJudgmentType judgmentType,
          ImmersiveDialogWidth dialogWidth, @Nullable String outOfCharacterText, @Nullable Integer moneyReward) {
        final String judgmentTypeLookupName = judgmentType.getLookupName();

        // Assembles dialog components
        String commanderAddress = campaign.getCommanderAddress(false);
        String dialogKey = getDialogKey(judgmentTypeLookupName, judgmentLookupName, judgingFaction);
        String factionName = getFactionName(judgingFaction, campaign.getGameYear());

        LocalDate today = campaign.getLocalDate();
        CurrentLocation location = campaign.getLocation();
        boolean isPlanetside = location.isOnPlanet();
        String locationName = isPlanetside
                                    ? location.getPlanet().getName(today)
                                    : location.getCurrentSystem().getName(today);

        String dialogText = getInCharacterText(
              RESOURCE_BUNDLE, dialogKey, commander, null, factionName,
              campaign.getName(), locationName, moneyReward, commanderAddress);

        // Determine available button choices
        boolean includeGoRogueOption = judgmentType.equals(FactionStandingJudgmentType.CENSURE);
        boolean isDraconisCombineCampaign = campaign.getFaction().getShortName().equals(DRACONIS_COMBINE_FACTION_CODE);
        boolean includeSeppukuOption = includeGoRogueOption && isDraconisCombineCampaign;

        List<String> buttonLabels = getButtonLabels(judgmentLookupName, includeGoRogueOption, includeSeppukuOption);

        // Display dialog and store selection
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
              campaign, speaker, null, dialogText,
              buttonLabels, outOfCharacterText, null, true, dialogWidth);

        responseIndex = dialog.getDialogChoice();
    }

    /**
     * Generates a lookup key for the dialog string resource based on the judgment type, event, and judging faction.
     *
     * <p>If a faction-specific dialog string is unavailable, it falls back to generic group-based (e.g., clan,
     * periphery) dialog strings.</p>
     *
     * @param judgmentTypeLookupName the lookup name for the type of judgment
     * @param lookupName             a unique name referring to the specific judgment event/action
     * @param judgingFaction         the {@link Faction} making the judgment
     *
     * @return the constructed string resource key for dialog text lookup
     *
     * @author Illiani
     * @since 0.50.07
     */
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
        boolean testReturnIsValid = MHQInternationalization.isResourceKeyValid(testReturn);
        if (testReturnIsValid) {
            return dialogKey;
        }

        return getFallbackFactionKey(dialogKey, judgingFaction);
    }

    /**
     * Gathers the set of button labels to be displayed in the dialog based on the judgment scenario.
     *
     * <p>The returned order of options matches their visual and logical order in the dialog interface.</p>
     *
     * @param judgmentLookupName   the identifier for this judgment scenario
     * @param includeGoRogueOption if {@code true}, adds a "go rogue" button to the dialog
     * @param includeSeppukuOption if {@code true}, adds a "commit seppuku" button to the dialog (only if "go rogue" is
     *                             present and faction is correct)
     *
     * @return a list of button label strings as shown to the user
     *
     * @author Illiani
     * @since 0.50.07
     */
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

            // The "commit seppuku" option is only valid when the "go rogue" option is displayed, and the campaign
            // faction is Draconis Combine
            if (includeSeppukuOption) {
                buttonKey = BUTTON_KEY_FORWARD + '.' + BUTTON_KEY_SEPPUKU + '.' + judgmentLookupName;
                String seppukuButtonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);
                buttonLabels.add(seppukuButtonLabel);
            }
        }

        return buttonLabels;
    }
}
