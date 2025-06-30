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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionCensureAction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.utilities.MHQInternationalization;

/**
 * Dialog for presenting a faction censure event and available choices to the player.
 *
 * <p>This class handles the construction and presentation of an immersive dialog when a censure event triggered by a
 * faction occurs within a campaign. It displays context-sensitive in-character and out-of-character messages along with
 * different branching options that the user can select.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionCensureDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureDialog";

    private static final String BUTTON_KEY_POSITIVE = "FactionCensureEvent.button.positive.";
    private static final String BUTTON_KEY_NEUTRAL = "FactionCensureEvent.button.neutral.";
    private static final String BUTTON_KEY_NEGATIVE = "FactionCensureEvent.button.negative.";
    private static final String BUTTON_KEY_ROGUE = "FactionCensureEvent.button.rogue.";
    private static final String BUTTON_KEY_SEPPUKU = "FactionCensureEvent.button.seppuku.";

    private static final String DIALOG_KEY_IN_CHARACTER = "FactionCensureEvent.inCharacter.";
    private static final String DIALOG_KEY_AFFIX_CLAN = "clan";
    private static final String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private static final String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";

    private static final String DIALOG_KEY_OUT_OF_CHARACTER = "FactionCensureEvent.outOfCharacter.";
    private static final String DRACONIS_COMBINE = "DC";

    private final Campaign campaign;
    private final FactionCensureAction censureAction;
    private final Faction censuringFaction;
    private int dialogChoiceIndex = 0;

    /**
     * Returns the dialog choice index selected by the user.
     *
     * <p>The value corresponds to the selected option in the dialog's choice list.</p>
     *
     * @return {@link Integer} representing the user's choice index (0-based)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getDialogChoiceIndex() {
        return dialogChoiceIndex;
    }

    /**
     * Constructs and shows a new FactionCensureDialog for the specified campaign, censure level, and character.
     *
     * @param campaign            the current campaign context
     * @param censureAction       the censure action being taken
     * @param commander           the campaign commander
     * @param censuringFaction    the faction performing the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureDialog(final Campaign campaign, final FactionCensureAction censureAction, Person commander,
          Faction censuringFaction) {
        this.campaign = campaign;
        this.censureAction = censureAction;
        this.censuringFaction = censuringFaction;

        String contextKey = censureAction.getLookupName();

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
              campaign,
              getSpeaker(),
              null,
              getInCharacterMessage(contextKey),
              getDialogOptions(contextKey, commander),
              getOutOfCharacterMessage(),
              null,
              true,
              ImmersiveDialogWidth.MEDIUM
        );
        dialogChoiceIndex = dialog.getDialogChoice();
    }

    /**
     * Generates a {@link Person} instance to serve as the "speaker" for the censure dialog, based on the campaign's
     * faction.
     *
     * <p>Generates a new person with an appropriate role, gender, and, if applicable, bloodname and rank.</p>
     *
     * @return the generated speaker {@link Person}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Person getSpeaker() {
        String factionCode = censuringFaction.getShortName();
        Person speaker = campaign.newPerson(PersonnelRole.MEKWARRIOR, factionCode, Gender.RANDOMIZE);

        if (censuringFaction.isClan()) {
            Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());

            if (bloodname != null) {
                speaker.setBloodname(bloodname.getName());
            }
        }

        RankSystem rankSystem = censuringFaction.getRankSystem();
        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return speaker;
        }

        speaker.setRankSystem(rankValidator, rankSystem);
        speaker.setRank(38);

        return speaker;
    }

    /**
     * Retrieves the in-character message to be presented at the start of the censure dialog.
     *
     * <p>This message is chosen based on the campaign's commander, faction, and censure level, and may fall back to
     * a more generic version if no faction-specific message is found.</p>
     *
     * @return the in-character message string to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getInCharacterMessage(String contextKey) {
        String commanderAddress = campaign.getCommanderAddress(false);
        String campaignName = campaign.getName();
        Faction campaignFaction = campaign.getFaction();
        String censuringFactionCode = censuringFaction.getShortName();

        String dialog = getFormattedTextAt(RESOURCE_BUNDLE,
              DIALOG_KEY_IN_CHARACTER + contextKey + '.' + censuringFactionCode, commanderAddress, campaignName);

        if (MHQInternationalization.isResourceKeyValid(dialog)) {
            return dialog;
        }

        String affixKey;
        if (campaignFaction.isClan()) {
            affixKey = DIALOG_KEY_AFFIX_CLAN;
        } else if (campaignFaction.isPeriphery()) {
            affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
        } else {
            affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, DIALOG_KEY_IN_CHARACTER + contextKey + '.' + affixKey,
              commanderAddress, campaignName);
    }

    /**
     * Builds a list of available options for the player to choose from in response to the censure.
     *
     * <p>Option text is localized and based on the current censure level. Includes a special "Seppuku" option if the
     * campaign faction is Draconis Combine and a senior character is specified.</p>
     *
     * @param contextKey
     * @param mostSeniorCharacter the most senior character (required for some special options)
     *
     * @return a list of localized option strings
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> getDialogOptions(String contextKey, Person mostSeniorCharacter) {
        List<String> options = new ArrayList<>();
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_POSITIVE + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_NEUTRAL + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_NEGATIVE + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_ROGUE + contextKey));

        boolean isDraconisCombineCampaign = Objects.equals(campaign.getFaction().getShortName(), DRACONIS_COMBINE);
        if (mostSeniorCharacter != null && isDraconisCombineCampaign) {
            options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_SEPPUKU + contextKey));
        }

        return options;
    }

    /**
     * Retrieves the out-of-character message to be presented in the dialog.
     *
     * @return the out-of-character message string to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, DIALOG_KEY_OUT_OF_CHARACTER + censureAction.getLookupName());
    }
}
