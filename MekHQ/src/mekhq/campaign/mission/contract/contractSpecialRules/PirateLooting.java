/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.contract.contractSpecialRules;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Implements the {@code USE_PIRATE_LOOTING} contract special rule: when a contract carrying that rule is completed the
 * force rolls to avoid getting caught, credits the looted booty to its finances, and - if caught - takes a
 * criminal-record penalty on the force or its personnel.
 *
 * <p>Extracted from the Chaos reputation system so the pirate-looting behaviour lives alongside the other contract
 * special rules. The in-character and finance text still reads from the shared {@code ChaosReputation} resource
 * bundle.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PirateLooting {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosReputation";

    private static final int PIRACY_AVOIDANCE_TN_SUCCESS = 7; // Hot Spots Draconis Reach pg25 1st printing
    private static final int PIRACY_AVOIDANCE_TN_FAILURE = 9; // Hot Spots Draconis Reach pg25 1st printing
    private static final double PIRACY_PENALTY_PROFIT_DIVIDER = 500; // Hot Spots Draconis Reach pg25 1st printing

    /**
     * Resolves an act of piracy: rolls to avoid getting caught, credits the looted booty to the force's finances, and -
     * if caught - applies a criminal-record penalty to the force or its personnel. Finally shows a summary dialog.
     *
     * @param campaign         the campaign performing the piracy
     * @param personnel        the personnel involved (penalized in per-character mode)
     * @param scale            the loot scale factor
     * @param scenarios        the contract's scenarios, used to value component loot
     * @param campaignState    the contract's StratCon campaign state, or {@code null} when the contract is not tracked
     *                         by StratCon; when present, only Essential scenarios contribute component loot
     * @param actWasSuccessful whether the underlying act succeeded (affects the avoidance target number)
     * @param contractName     the contract name, used in the finance and dialog text
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolveActOfPiracy(Campaign campaign, List<Person> personnel, int scale,
          List<Scenario> scenarios, @Nullable StratConCampaignState campaignState, boolean actWasSuccessful,
          String contractName) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Person commander = playerForce.getHumanResources().getCommander(campaign.getCampaignOptions(),
              playerForce.isClanForce(),
              campaign.getLocalDate());

        int roll = d6(2);
        int targetNumber = actWasSuccessful ? PIRACY_AVOIDANCE_TN_SUCCESS : PIRACY_AVOIDANCE_TN_FAILURE;
        // A "Slippery" commander makes the force harder to catch; a "Conspicuous" one makes it easier.
        if (commander != null) {
            if (commander.getOptions().booleanOption(PersonnelOptions.SLIPPERY)) {
                targetNumber -= 1;
            } else if (commander.getOptions().booleanOption(PersonnelOptions.CONSPICUOUS)) {
                targetNumber += 1;
            }
        }
        boolean gotCaught = roll < targetNumber;

        int supportPointsLoot = determinePiracySP(scale, scenarios, campaignState, commander);
        Money booty = ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(supportPointsLoot);
        creditFinancesForBooty(campaign, contractName, booty);

        int delta = (int) -ceil(supportPointsLoot / PIRACY_PENALTY_PROFIT_DIVIDER);

        if (campaign.getCampaignOptions().get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
            if (gotCaught) {
                campaign.getPlayerForce().changeChaosCampaignReputation(delta);
            }
        } else {
            // Always process personnel: the "Loose Lips" SPA records a criminal record even when the detachment was
            // not caught, so the base delta is 0 for a clean getaway.
            updatePersonnelForActOfPiracy(personnel, gotCaught ? delta : 0);
        }

        triggerPiracyDialog(campaign, roll, targetNumber, gotCaught, delta, booty);
    }

    /**
     * Credits looted booty to the force's finances as a theft transaction.
     *
     * @param campaign     the campaign whose finances are credited
     * @param contractName the contract name shown on the transaction
     * @param booty        the amount to credit
     */
    private static void creditFinancesForBooty(Campaign campaign, String contractName, Money booty) {
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.THEFT,
                    campaign.getLocalDate(),
                    booty,
                    getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.booty", contractName));
    }

    /**
     * Shows the in-character summary dialog reporting the outcome of an act of piracy.
     *
     * @param campaign     the campaign the dialog belongs to
     * @param roll         the avoidance roll made
     * @param targetNumber the target number the roll was compared against
     * @param gotCaught    whether the force was caught
     * @param delta        the (negative) reputation/criminal-record delta applied when caught
     * @param booty        the looted amount
     */
    private static void triggerPiracyDialog(Campaign campaign, int roll, int targetNumber, boolean gotCaught, int delta,
          Money booty) {
        PlayerForce playerForce = campaign.getPlayerForce();
        ForceHumanResources forceHumanResources = playerForce.getHumanResources();
        boolean isClanForce = playerForce.isClanForce();
        Person seniorAdmin = forceHumanResources.getSeniorAdminPerson(campaign.getCampaignOptions(),
              isClanForce,
              campaign.getLocalDate());

        String captureKey = gotCaught ? "caught" : "notCaught";
        String reportKey = "ChaosReputation.piracy.dialog." + captureKey;
        String addendum = getTextAt(RESOURCE_BUNDLE, reportKey);

        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "ChaosReputation.piracy.dialog.ic",
              campaign.getCommanderAddress(),
              booty.toAmountString(),
              addendum);

        new ImmersiveDialogSimple(campaign,
              seniorAdmin,
              inCharacterMessage,
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.dialog.ooc", roll, targetNumber, -delta));
    }

    /**
     * Determines the total support-point value of piracy loot: a random contract-loot roll plus component loot earned
     * from victorious scenarios, each scaled by {@code scale}.
     *
     * @param scale             the loot scale factor
     * @param scenarios         the contract's scenarios
     * @param campaignState     the contract's StratCon campaign state, or {@code null} when the contract is not tracked
     *                          by StratCon
     * @param campaignCommander the campaign commander
     *
     * @return the total looted support points
     */
    private static int determinePiracySP(int scale, List<Scenario> scenarios,
          @Nullable StratConCampaignState campaignState, @Nullable Person campaignCommander) {
        int roll = d6(1);
        if (campaignCommander != null && campaignCommander.getOptions().booleanOption(PersonnelOptions.LOOT_GOBLIN)) {
            roll = min(6, roll + 1);
        }

        int contractLootSP = determineContractLoot(roll, scale);

        int componentLootSP = determineComponentLoot(scenarios, campaignState);

        return contractLootSP + componentLootSP;
    }

    /**
     * Returns the contract-loot support points for a d6 roll, scaled by {@code scale}.
     *
     * @param roll  the 1-6 roll
     * @param scale the loot scale factor
     *
     * @return the scaled contract-loot support points
     */
    private static int determineContractLoot(int roll, int scale) {
        // Hot Spots Draconis Reach, first printing, pg 126
        int base = switch (roll) {
            case 1 -> 0;
            case 2 -> 375;
            case 3 -> 750;
            case 4 -> 1250;
            case 5 -> 1500;
            case 6 -> 1650;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        return base * scale;
    }

    /**
     * Returns the component-loot support points earned from the number of victorious scenarios.
     *
     * <p>When the contract is tracked by StratCon, only Essential scenarios (those tied to a StratCon strategic
     * objective) are counted. When the contract has no StratCon campaign state, every scenario is counted.</p>
     *
     * @param scenarios     the contract's scenarios
     * @param campaignState the contract's StratCon campaign state, or {@code null} when the contract is not tracked by
     *                      StratCon
     *
     * @return the scaled component-loot support points
     */
    private static int determineComponentLoot(List<Scenario> scenarios,
          @Nullable StratConCampaignState campaignState) {
        // A null set signals that the contract is not StratCon-tracked, so every scenario counts; otherwise only the
        // Essential scenarios (those whose IDs are in the set) contribute.
        Set<Integer> essentialScenarioIDs = getEssentialScenarioIDs(campaignState);

        int runningTotal = 0;
        for (Scenario scenario : scenarios) {
            if ((essentialScenarioIDs != null) && !essentialScenarioIDs.contains(scenario.getId())) {
                continue;
            }

            if (scenario.getStatus().isVictory()) {
                runningTotal++;
            }
        }

        // Hot Spots Draconis Reach, first printing, pg 126
        int total = 0;
        for (int i = 0; i <= runningTotal; i++) {
            int roll = d6(1);
            total += switch (roll) {
                case 0 -> 0;
                case 1 -> 250;
                case 2 -> 300;
                case 3 -> 400;
                case 4 -> 500;
                case 5 -> 550;
                case 6 -> 600;
                default -> 650; // Not normally reachable
            };
        }

        return total;
    }

    /**
     * Collects the backing scenario IDs of every Essential scenario - those tied to a StratCon strategic objective -
     * across all the contract's tracks.
     *
     * @param campaignState the contract's StratCon campaign state, or {@code null} when the contract is not tracked by
     *                      StratCon
     *
     * @return the set of Essential backing scenario IDs (empty when the contract has none), or {@code null} when there
     *       is no StratCon campaign state, signaling that every scenario should be counted
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @Nullable Set<Integer> getEssentialScenarioIDs(@Nullable StratConCampaignState campaignState) {
        if (campaignState == null) {
            return null;
        }

        Set<Integer> essentialScenarioIDs = new HashSet<>();
        for (StratConTrackState track : campaignState.getTracks()) {
            for (StratConScenario stratConScenario : track.getScenarios().values()) {
                if (stratConScenario.isStrategicObjective()) {
                    essentialScenarioIDs.add(stratConScenario.getBackingScenarioID());
                }
            }
        }
        return essentialScenarioIDs;
    }

    /**
     * Applies the per-character criminal-record change for an act of piracy.
     *
     * <p>Each employed character receives {@code baseDelta} (the negative penalty when the detachment was caught, or
     * {@code 0} for a clean getaway), adjusted by their SPAs: "Leaves Paper Trail" always records one extra point of
     * criminal record (even on a clean getaway), while "Leaves No Trail" reduces the criminal record gained by one, but
     * never turns a penalty into a bonus.</p>
     *
     * @param personnel the personnel involved in the act of piracy
     * @param baseDelta the base criminal-record change (negative when caught, {@code 0} otherwise)
     */
    private static void updatePersonnelForActOfPiracy(List<Person> personnel, int baseDelta) {
        for (Person person : personnel) {
            if (!person.isEmployed()) {
                continue;
            }

            int criminalRecordDelta = baseDelta;
            PersonnelOptions options = person.getOptions();
            if (options.booleanOption(PersonnelOptions.LOOSE_LIPS)) {
                criminalRecordDelta -= 1;
            } else if (options.booleanOption(PersonnelOptions.LEAVES_NO_TRAIL)) {
                criminalRecordDelta = min(criminalRecordDelta + 1, 0);
            }

            if (criminalRecordDelta != 0) {
                person.changeCriminalRecord(criminalRecordDelta);
            }
        }
    }
}
