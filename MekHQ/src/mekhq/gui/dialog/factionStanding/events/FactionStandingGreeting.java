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
package mekhq.gui.dialog.factionStanding.events;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Handles the creation and display of Faction Standing greeting dialogs for contracts.
 *
 * <p>This class is responsible for assembling immersive dialog text and options that reflect faction standings,
 * contract details, and in-game situations such as guerrilla warfare, independence, and logistic status. It tailors
 * both the in-character and out-of-character information, including resupply requirements, to the current context.</p>
 *
 * <p>All dialog text is retrieved from a resource bundle to support localization.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingGreeting {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingGreeting";

    private static final String DIALOG_INTRODUCTION_TEXT_KEY = "FactionStandingGreeting.inCharacter.greeting.";
    private static final String DIALOG_FOLLOW_UP_TEXT_KEY = "FactionStandingGreeting.inCharacter.followUp";
    private static final String DIALOG_RESUPPLY_TEXT_KEY = "FactionStandingGreeting.inCharacter.resupply.";
    private static final String DIALOG_RESUPPLY_REGULAR_AFFIX = "regular";
    private static final String DIALOG_RESUPPLY_INDEPENDENT_AFFIX = "independent";
    private static final String DIALOG_RESUPPLY_SMUGGLER_AFFIX = "smuggler";
    private static final String DIALOG_RESUPPLY_OUT_OF_CHARACTER_KEY = "FactionStandingGreeting.outOfCharacter.resupply";

    /**
     * Constructs and immediately displays a Faction Standing greeting dialog based on the provided campaign and
     * contract.
     *
     * <p>If the contract is an instance of {@link AtBContract}, the dialog is customized using employer liaison,
     * contract type, command rights, and resupply information. If the contract is not an AtBContract,
     * {@link #FactionStandingGreeting(Campaign)} is called instead to display a default greeting.</p>
     *
     * @param campaign the current campaign instance
     * @param contract the contract whose context determines the dialog content; may be an AtBContract or another type
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingGreeting(Campaign campaign, Contract contract) {
        if (!(contract instanceof AtBContract atBContract)) {
            new FactionStandingGreeting(campaign);
            return;
        }

        if (atBContract.getEmployerCode().equals(PIRATE_FACTION_CODE)) {
            return;
        }

        final Person contractRepresentative = atBContract.getEmployerLiaison();
        final boolean isGuerrillaWarfare = atBContract.getContractType().isGuerrillaWarfare();
        final boolean isIndependent = atBContract.getCommandRights().isIndependent();
        final FactionStandingLevel factionStandingLevel = getFactionStandingsLevel(campaign.getFactionStandings(),
              contractRepresentative);

        int cargoRequirements = 0;
        int cargoAvailable = 0;
        final boolean isUseStratCon = campaign.getCampaignOptions().isUseStratCon();
        if (isUseStratCon) {
            cargoRequirements = ResupplyUtilities.estimateCargoRequirements(campaign, atBContract);
            cargoAvailable = ResupplyUtilities.estimateAvailablePlayerCargo(campaign);
        }

        new ImmersiveDialogSimple(campaign,
              contractRepresentative,
              null,
              getInCharacterText(factionStandingLevel,
                    contractRepresentative,
                    isGuerrillaWarfare,
                    isIndependent,
                    isUseStratCon),
              getDialogOptions(),
              isGuerrillaWarfare || !isUseStratCon ? null : getOutOfCharacterText(cargoRequirements, cargoAvailable),
              null,
              true);
    }

    /**
     * Constructs and immediately displays a Faction Standing greeting dialog in the absence of an {@link AtBContract}.
     *
     * <p>This form provides a simplified greeting dialog, using a generated employer liaison and default context
     * such as a neutral or non-contract scenario. No resupply or contract-specific information is included in the
     * dialog.</p>
     *
     * @param campaign the current campaign instance
     *
     * @author Illiani
     * @since 0.50.07
     */
    private FactionStandingGreeting(Campaign campaign) {
        final Person contractRepresentative = createEmployerLiaison(campaign);
        final FactionStandingLevel factionStandingLevel = getFactionStandingsLevel(campaign.getFactionStandings(),
              contractRepresentative);

        new ImmersiveDialogSimple(campaign,
              contractRepresentative,
              null,
              getInCharacterText(factionStandingLevel,
                    contractRepresentative,
                    false,
                    false,
                    false),
              getDialogOptions(),
              null,
              null,
              true);
    }

    /**
     * Determines the {@link FactionStandingLevel} for a given person using their origin faction and the current
     * campaign faction standings.
     *
     * @param factionStandings       the faction standings instance to query
     * @param contractRepresentative the representative whose faction to evaluate
     *
     * @return the calculated faction standing level
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static FactionStandingLevel getFactionStandingsLevel(FactionStandings factionStandings,
          Person contractRepresentative) {
        Faction relevantFaction = contractRepresentative.getOriginFaction();
        double regard = factionStandings.getRegardForFaction(relevantFaction.getShortName(), true);
        return FactionStandingUtilities.calculateFactionStandingLevel(regard);
    }

    /**
     * Assembles the in-character dialog text for the greeting dialog, tailoring content to faction standing and
     * contract context.
     *
     * @param factionStandingLevel   the standing level with the relevant faction
     * @param contractRepresentative the contract representative for addressing and personalization
     * @param isGuerrillaWarfare     {@code true} if the contract type is guerrilla warfare
     * @param isIndependent          {@code true} if the command is independent
     * @param isUseStratCon          {@code true} if strategic contracts (StratCon) are enabled
     *
     * @return the assembled in-character dialog text
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterText(FactionStandingLevel factionStandingLevel, Person contractRepresentative,
          boolean isGuerrillaWarfare, boolean isIndependent, boolean isUseStratCon) {
        String key = DIALOG_INTRODUCTION_TEXT_KEY + factionStandingLevel.toString() + '.' + randomInt(20);
        String introduction = getFormattedTextAt(RESOURCE_BUNDLE, key, contractRepresentative.getFullTitle());

        key = DIALOG_RESUPPLY_TEXT_KEY;
        if (isGuerrillaWarfare) {
            key += DIALOG_RESUPPLY_SMUGGLER_AFFIX;
        } else if (isIndependent) {
            key += DIALOG_RESUPPLY_INDEPENDENT_AFFIX;
        } else {
            key += DIALOG_RESUPPLY_REGULAR_AFFIX;
        }

        String resupply = "";
        if (isUseStratCon) {
            resupply = getFormattedTextAt(RESOURCE_BUNDLE, key);
        }

        String followUp = getTextAt(RESOURCE_BUNDLE, DIALOG_FOLLOW_UP_TEXT_KEY);

        return introduction + resupply + followUp;
    }

    /**
     * Assembles the out-of-character dialog text providing resupply and logistics details to the player.
     *
     * @param cargoRequirements the estimated amount of cargo required for the contract
     * @param cargoAvailable    the player's available cargo capacity
     *
     * @return the assembled out-of-character dialog text
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getOutOfCharacterText(int cargoRequirements, int cargoAvailable) {
        return getFormattedTextAt(RESOURCE_BUNDLE, DIALOG_RESUPPLY_OUT_OF_CHARACTER_KEY, cargoRequirements,
              GlossaryEntry.FORCE_TYPE_CONVOY.getLookUpName(), GlossaryEntry.TOE.getLookUpName(), cargoAvailable,
              GlossaryEntry.RESUPPLY.getLookUpName());
    }

    /**
     * Retrieves a list of localized text strings representing the player's response options for the greeting dialog.
     *
     * @return a list of reply option strings
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<String> getDialogOptions() {
        return List.of(
              getTextAt(RESOURCE_BUNDLE, "FactionStandingGreeting.reply.positive"),
              getTextAt(RESOURCE_BUNDLE, "FactionStandingGreeting.reply.neutral"),
              getTextAt(RESOURCE_BUNDLE, "FactionStandingGreeting.reply.negative")
        );
    }

    public Person createEmployerLiaison(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();
        Person employerLiaison = campaign.newPerson(PersonnelRole.MILITARY_LIAISON, campaignFaction.getShortName(),
              Gender.RANDOMIZE);

        final RankSystem rankSystem = campaignFaction.getRankSystem();

        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return employerLiaison;
        }

        employerLiaison.setRankSystem(rankValidator, rankSystem);
        employerLiaison.setRank(Rank.RWO_MIN);

        return employerLiaison;
    }
}
