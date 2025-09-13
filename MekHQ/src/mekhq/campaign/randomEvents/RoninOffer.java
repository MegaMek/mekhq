/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;
import static mekhq.campaign.personnel.enums.PersonnelRole.AEROSPACE_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.AGGRESSION;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.AMBITION;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.GREED;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.SOCIAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.randomEvents.RoninEventDialog;

/**
 * Represents a dialog and associated logic for presenting the player with a Ronin offer. The Ronin offer involves
 * hiring a new character (a 'Ronin') with specific skills and attributes. The decision to hire the Ronin depends on
 * offering C-Bills or Support Points, depending on whether StratCon is enabled.
 *
 * <p>This class handles the creation of the Ronin character, the dialog interactions, and the recruitment
 * process based on the player’s choices.</p>
 */
public class RoninOffer {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RoninOffer";

    private final Campaign campaign;

    private static final int ACCEPT_DIALOG_CHOICE_INDEX = 0;

    private static final Money FALLBACK_HIRING_FEE = Money.of(500000);

    /**
     * Creates a new instance of the {@code RoninOffer} dialog for handling a Ronin hiring event. This creates a Ronin
     * character, randomizes their attributes and abilities, and initiates the dialog process where the player can
     * choose whether to recruit them.
     *
     * @param campaign            The current {@link Campaign} instance associated with the game.
     * @param campaignState       The optional {@link StratConCampaignState}. If {@code null} we will use a fallback
     *                            C-Bill cost, instead or Support Points.
     * @param requiredCombatTeams The number of combat teams needed, affecting the cost of hiring.
     */
    public RoninOffer(Campaign campaign, @Nullable StratConCampaignState campaignState, int requiredCombatTeams) {
        this.campaign = campaign;

        int roll = randomInt(5);

        PersonnelRole role = roll == 0 ? AEROSPACE_PILOT : MEKWARRIOR;
        Person ronin = campaign.newPerson(role);

        RandomSkillPreferences randomSkillPreferences = campaign.getRandomSkillPreferences();
        boolean useExtraRandomness = randomSkillPreferences.randomizeSkill();

        // We don't care about admin, doctor or tech settings, as they're not going to spawn here
        overrideSkills(false,
              false,
              false,
              campaign.getCampaignOptions().isUseArtillery(),
              useExtraRandomness,
              ronin,
              role,
              VETERAN);

        SkillLevel skillLevel = ronin.getSkillLevel(campaign, false);
        reRollLoyalty(ronin, skillLevel);
        reRollAdvantages(campaign, ronin, skillLevel);

        ronin.setCallsign(RandomCallsignGenerator.getInstance().generate());

        displayAndProcessConversation(campaign, ronin, campaignState, requiredCombatTeams);
    }

    /**
     * Displays the Ronin offer dialog and processes the player's choices throughout the interaction. This method
     * progresses through different message dialogs and updates the campaign if the Ronin is recruited.
     *
     * @param campaign            The active {@link Campaign} instance.
     * @param ronin               The Ronin {@link Person} being offered for recruitment.
     * @param campaignState       The optional {@link StratConCampaignState} providing strategic information.
     * @param requiredCombatTeams The number of combat teams required for the recruitment.
     */
    private void displayAndProcessConversation(Campaign campaign, Person ronin, StratConCampaignState campaignState,
          int requiredCombatTeams) {
        String commanderAddress = campaign.getCommanderAddress();
        int response = displayInitialMessage(commanderAddress, ronin.getCallsign());
        if (response != ACCEPT_DIALOG_CHOICE_INDEX) {
            return;
        }

        int cost = max((int) Math.round((double) requiredCombatTeams / 2), 1);

        response = displayRoninMessage(ronin,
              commanderAddress,
              campaignState == null,
              cost,
              campaignState == null ? null : campaignState.getSupportPoints());
        if (response != ACCEPT_DIALOG_CHOICE_INDEX) {
            return;
        }

        // Pay for the Ronin
        if (campaignState == null) {
            // This and the Support Point cost are designed to scale with campaign size.
            // The larger the campaign, the more resources they have to toss around.
            campaign.getFinances()
                  .debit(RECRUITMENT,
                        campaign.getLocalDate(),
                        FALLBACK_HIRING_FEE.multipliedBy(cost),
                        getFormattedTextAt(RESOURCE_BUNDLE, "message.hiring", ronin.getFullName()));
        } else {
            campaignState.changeSupportPoints(-cost);
        }

        campaign.recruitPerson(ronin, true, true);
    }


    /**
     * Displays the initial message of the Ronin hiring process, giving the player multiple response options. This is
     * the first step in the interaction flow.
     *
     * @return An integer representing the player's choice. Used to determine whether the process continues.
     */
    private int displayInitialMessage(String commanderAddress, String roninCallSign) {
        String centerMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "message.ic.fromHR",
              commanderAddress,
              roninCallSign);

        List<String> buttonLabels = new ArrayList<>();
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.fromHR.accept"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.fromHR.decline.polite"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.fromHR.decline.neutral"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.fromHR.decline.rude", roninCallSign));

        ImmersiveDialogSimple initialMessage = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(HR),
              null,
              centerMessage,
              buttonLabels,
              null,
              null,
              true);

        return initialMessage.getDialogChoice();
    }

    /**
     * Displays the main Ronin message dialog, including immersive text and out-of-character information such as skills,
     * abilities, and hiring costs.
     *
     * @param ronin                  The Ronin {@link Person} being offered for recruitment.
     * @param commanderAddress       The in-game address of the commander used in immersive text.
     * @param useFallbackHiringFee   Indicates if the fallback hiring fee (C-Bills) should be used instead of support
     *                               points.
     * @param requiredCombatTeams    The number of combat teams required for the recruitment cost calculation.
     * @param availableSupportPoints The support points available in the current campaign, if applicable; can be
     *                               {@code null} if using the fallback hiring fee.
     *
     * @return An integer representing the player's choice from the dialog.
     */
    private int displayRoninMessage(Person ronin, String commanderAddress, boolean useFallbackHiringFee,
          int requiredCombatTeams, @Nullable Integer availableSupportPoints) {
        String centerMessage = createRoninMessage(ronin, commanderAddress);

        String outOfCharacterMessage = createRoninOutOfCharacterMessage(
              useFallbackHiringFee,
              requiredCombatTeams,
              availableSupportPoints);

        RoninEventDialog initialMessage = new RoninEventDialog(campaign, ronin, centerMessage, outOfCharacterMessage);

        return initialMessage.getDialogChoice();
    }

    /**
     * Builds the immersive message shown to the player from the Ronin's perspective. The content of the message will
     * vary based on the Ronin's personality traits.
     *
     * @param ronin            The Ronin {@link Person} being offered for recruitment.
     * @param commanderAddress The in-game address of the commander used in immersive text.
     *
     * @return A {@link String} containing the message content from the Ronin's perspective.
     */
    private static String createRoninMessage(Person ronin, String commanderAddress) {
        List<PersonalityTraitType> normalTraits = new ArrayList<>();
        List<PersonalityTraitType> majorTraits = new ArrayList<>();

        Aggression aggression = ronin.getAggression();
        if (!aggression.isNone()) {
            if (aggression.isTraitMajor()) {
                majorTraits.add(AGGRESSION);
            } else {
                normalTraits.add(AGGRESSION);
            }
        }

        Ambition ambition = ronin.getAmbition();
        if (!ambition.isNone()) {
            if (ambition.isTraitMajor()) {
                majorTraits.add(AMBITION);
            } else {
                normalTraits.add(AMBITION);
            }
        }

        Greed greed = ronin.getGreed();
        if (!greed.isNone()) {
            if (greed.isTraitMajor()) {
                majorTraits.add(GREED);
            } else {
                normalTraits.add(GREED);
            }
        }

        Social social = ronin.getSocial();
        if (!social.isNone()) {
            if (social.isTraitMajor()) {
                majorTraits.add(SOCIAL);
            } else {
                normalTraits.add(SOCIAL);
            }
        }

        mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType chosenTrait = null;

        if (!majorTraits.isEmpty()) {
            chosenTrait = ObjectUtility.getRandomItem(majorTraits);
        }

        if (!normalTraits.isEmpty()) {
            chosenTrait = ObjectUtility.getRandomItem(normalTraits);
        }

        String message = "";
        if (chosenTrait != null) {
            message = switch (chosenTrait) {
                case AGGRESSION -> aggression.getRoninMessage(commanderAddress);
                case AMBITION -> ambition.getRoninMessage(commanderAddress);
                case GREED -> greed.getRoninMessage(commanderAddress);
                case SOCIAL -> social.getRoninMessage(commanderAddress);
                default -> "";
            };
        }

        if (message.isBlank()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "message.ic.fallback", commanderAddress);
        } else {
            return String.format(message, commanderAddress);
        }
    }

    /**
     * Constructs the out-of-character message to be displayed alongside the immersive Ronin message. This includes a
     * breakdown of the hiring cost.
     *
     * @param useFallbackHiringFee   Indicates if the fallback hiring fee (C-Bills) should be used instead of support
     *                               points.
     * @param requiredCombatTeams    The number of combat teams required for the recruitment cost calculation.
     * @param availableSupportPoints The support points available in the current campaign, if applicable; can be
     *                               {@code null} if using the fallback hiring fee.
     *
     * @return A {@link String} containing formatted out-of-character details for the player.
     */
    private String createRoninOutOfCharacterMessage(boolean useFallbackHiringFee, int requiredCombatTeams,
          @Nullable Integer availableSupportPoints) {
        return buildCostString(useFallbackHiringFee, requiredCombatTeams, availableSupportPoints);
    }

    /**
     * Builds a formatted string describing the cost of recruiting the Ronin based on their hiring type (C-Bills or
     * support points), the number of combat teams required, and currently available resources.
     *
     * @param useFallbackHiringFee   Indicates if the fallback hiring fee (C-Bills) should be used instead of support
     *                               points.
     * @param requiredCombatTeams    The number of combat teams required for the recruitment cost calculation.
     * @param availableSupportPoints The support points available in the current campaign, if applicable; can be
     *                               {@code null} if using the fallback hiring fee.
     *
     * @return A {@link String} containing the formatted cost information.
     */
    private String buildCostString(boolean useFallbackHiringFee, int requiredCombatTeams,
          @Nullable Integer availableSupportPoints) {
        String addendumKey = useFallbackHiringFee ? "message.ooc.cBills" : "message.ooc.supportPoints";
        String addendum = getFormattedTextAt(RESOURCE_BUNDLE, addendumKey);

        String cost = useFallbackHiringFee ?
                            FALLBACK_HIRING_FEE.multipliedBy(requiredCombatTeams).toAmountString() :
                            requiredCombatTeams + "";

        String availableResources = useFallbackHiringFee ?
                                          campaign.getFunds().toAmountString() :
                                          availableSupportPoints + "";

        return getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc.cost", cost, addendum, availableResources, addendum);
    }
}
