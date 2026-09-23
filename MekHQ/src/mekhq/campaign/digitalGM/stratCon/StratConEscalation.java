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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics.EscalationMode;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;

/**
 * Escalation: how far a raiding or guerrilla contract's hostilities have escalated, and so how hard the enemy is
 * pushing back. It belongs to the "Contracts Use Special Mechanics" option, and to contracts whose objective is a
 * {@link ChaosObjectiveType#RAID raid} or a {@link ChaosObjectiveType#GUERILLA_OPERATION guerrilla operation}, or is
 * Sabotage, Terrorism, or a Pirate Raid. Garrison Duty contracts track it too, but the other way around (see below).
 *
 * <p>Escalation runs from 0 to 100 per point of the contract's scale. On most contracts it starts at 0 and only ever
 * rises:</p>
 *
 * <ul>
 *     <li>deploying a force to an empty hex, with no scenario resulting: +1</li>
 *     <li>winning a scenario: +1d6</li>
 *     <li>fighting at a hostile facility: +2d6, or +3d6 if the fight is won (in place of the +1d6 for winning)</li>
 *     <li>striking a high profile target: +3d6 (see
 *     {@link mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConHighProfileTargetBehavior})</li>
 * </ul>
 *
 * <p>On a Garrison Duty contract, Escalation is the unrest the garrison must calm. It starts at its maximum and only
 * ever falls - nothing raises it:</p>
 *
 * <ul>
 *     <li>deploying a force to an empty hex, with no scenario resulting: -1</li>
 *     <li>winning a scenario: -1d6</li>
 *     <li>making a show of force: -3d6 (see
 *     {@link mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConShowOfForceBehavior})</li>
 * </ul>
 *
 * <p>Each ordinary morale check then rolls a die with as many sides as the contract's maximum Escalation. A roll no
 * higher than the current Escalation raises the enemy's morale by one level, on top of whatever the check itself did.
 * </p>
 *
 * <p>A Diversionary Raid's strategic objective is to raise Escalation to 50 per point of scale before the contract ends
 * (see {@link StrategicObjectiveType#Escalation}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConEscalation {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /** The maximum Escalation, per point of the contract's scale. */
    public static final int MAXIMUM_ESCALATION_PER_SCALE = 100;

    /** The Escalation a Diversionary Raid must reach, per point of the contract's scale. */
    public static final int DIVERSIONARY_RAID_TARGET_PER_SCALE = 50;

    /** The Escalation a deployment to an empty hex adds (or, on a Garrison Duty contract, removes), when no scenario
     * results. */
    public static final int EMPTY_HEX_DEPLOYMENT_ESCALATION = 1;

    private StratConEscalation() {
    }

    /**
     * @param campaign the current campaign
     * @param contract the contract, or {@code null}
     *
     * @return {@code true} if the contract tracks Escalation: the "Contracts Use Special Mechanics" option is on, the
     *       contract is one that escalates (see {@link #isEscalationContract}), and it has a StratCon state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isEscalationUsed(Campaign campaign, @Nullable AbstractContract contract) {
        return (contract != null)
                     && (contract.getStratConCampaignState() != null)
                     && Boolean.TRUE.equals(campaign.getCampaignOptions()
                                                  .get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS))
                     && isEscalationContract(contract);
    }

    /**
     * @param contract the contract
     *
     * @return {@code true} if the contract is one that tracks Escalation: a raid or a guerrilla operation, or a
     *       Sabotage, Terrorism, Pirate Raid, or Garrison Duty contract
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isEscalationContract(AbstractContract contract) {
        return StratConContractMechanics.forContract(contract).escalationMode() != EscalationMode.NONE;
    }

    /**
     * @param contract the contract
     *
     * @return {@code true} if the contract's Escalation runs the other way around: it starts at its maximum and only
     *       ever falls. That is a Garrison Duty contract, whose garrison must calm the unrest around it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isDeescalatingContract(AbstractContract contract) {
        return StratConContractMechanics.forContract(contract).escalationMode() == EscalationMode.DEESCALATING;
    }

    /**
     * Starts a newly accepted Garrison Duty contract's Escalation at its maximum (see
     * {@link #isDeescalatingContract}). Does nothing for any other contract, whose Escalation starts at 0.
     *
     * @param contract      the contract being accepted
     * @param campaignState its StratCon state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void startEscalation(AbstractContract contract, StratConCampaignState campaignState) {
        if (isDeescalatingContract(contract)) {
            campaignState.setEscalation(getMaximumEscalation(contract));
        }
    }

    /**
     * @param contract the contract
     *
     * @return the contract's maximum Escalation: 100 per point of its scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getMaximumEscalation(AbstractContract contract) {
        return MAXIMUM_ESCALATION_PER_SCALE * max(1, contract.getScale());
    }

    /**
     * @param contract the contract
     *
     * @return the Escalation a Diversionary Raid must reach: 50 per point of its scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getDiversionaryRaidTarget(AbstractContract contract) {
        return DIVERSIONARY_RAID_TARGET_PER_SCALE * max(1, contract.getScale());
    }

    /**
     * Raises the contract's Escalation, capped at its maximum, and brings any Escalation objectives up to date. Does
     * nothing if the contract does not track Escalation (see {@link #isEscalationUsed}), or if its Escalation only ever
     * falls (see {@link #isDeescalatingContract}).
     *
     * @param campaign the current campaign
     * @param contract the contract
     * @param amount   how much to raise it by; nothing happens if this is not positive
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void increaseEscalation(Campaign campaign, @Nullable AbstractContract contract, int amount) {
        if ((amount <= 0) || !isEscalationUsed(campaign, contract) || isDeescalatingContract(contract)) {
            return;
        }

        StratConCampaignState campaignState = contract.getStratConCampaignState();
        campaignState.setEscalation(min(getMaximumEscalation(contract), campaignState.getEscalation() + amount));
        updateEscalationObjectives(campaignState);

        // Escalation often rises after the event that prompted it has already refreshed the display - a deployment's
        // event fires before its outcome is known - so announce the change itself.
        MekHQ.triggerEvent(new MissionChangedEvent(contract));
    }

    /**
     * Lowers a Garrison Duty contract's Escalation, to no less than 0. Does nothing if the contract does not track
     * Escalation (see {@link #isEscalationUsed}), or if its Escalation only ever rises (see
     * {@link #isDeescalatingContract}).
     *
     * @param campaign the current campaign
     * @param contract the contract
     * @param amount   how much to lower it by; nothing happens if this is not positive
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void decreaseEscalation(Campaign campaign, @Nullable AbstractContract contract, int amount) {
        if ((amount <= 0) || !isEscalationUsed(campaign, contract) || !isDeescalatingContract(contract)) {
            return;
        }

        StratConCampaignState campaignState = contract.getStratConCampaignState();
        campaignState.setEscalation(max(0, campaignState.getEscalation() - amount));
        updateEscalationObjectives(campaignState);

        // As with a rise, the event that prompted the fall has usually already refreshed the display.
        MekHQ.triggerEvent(new MissionChangedEvent(contract));
    }

    /**
     * Raises the contract's Escalation by a number of d6 (see {@link #increaseEscalation}).
     *
     * @param campaign the current campaign
     * @param contract the contract
     * @param dice     how many d6 to roll; nothing happens if this is not positive
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void increaseEscalationByDice(Campaign campaign, @Nullable AbstractContract contract, int dice) {
        if (dice > 0) {
            increaseEscalation(campaign, contract, d6(dice));
        }
    }

    /**
     * A force has deployed to an empty hex - one with no scenario, facility, or point of interest - and no scenario
     * resulted: +1 Escalation, or -1 on a Garrison Duty contract.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onEmptyHexDeployment(Campaign campaign, AbstractContract contract) {
        if (isDeescalatingContract(contract)) {
            decreaseEscalation(campaign, contract, EMPTY_HEX_DEPLOYMENT_ESCALATION);
        } else {
            increaseEscalation(campaign, contract, EMPTY_HEX_DEPLOYMENT_ESCALATION);
        }
    }

    /**
     * A scenario has been resolved: +1d6 Escalation for a win; or, for a fight at a hostile facility, +2d6, or +3d6 if
     * it was won. On a Garrison Duty contract, a win - wherever it was fought - lowers Escalation by 1d6 instead, and a
     * loss does nothing.
     *
     * @param campaign                  the current campaign
     * @param contract                  the contract the scenario belongs to
     * @param isVictory                 whether the scenario was an overall victory for the player
     * @param isHostileFacilityScenario whether the scenario was fought at a hostile facility
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onScenarioCompleted(Campaign campaign, AbstractContract contract, boolean isVictory,
          boolean isHostileFacilityScenario) {
        if (isDeescalatingContract(contract)) {
            if (isVictory) {
                decreaseEscalation(campaign, contract, d6(1));
            }
            return;
        }

        increaseEscalation(campaign, contract, getScenarioEscalation(isVictory, isHostileFacilityScenario));
    }

    /**
     * @param isVictory                 whether the scenario was an overall victory for the player
     * @param isHostileFacilityScenario whether the scenario was fought at a hostile facility
     *
     * @return the number of d6 a resolved scenario rolls for Escalation
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getScenarioEscalationDice(boolean isVictory, boolean isHostileFacilityScenario) {
        if (isHostileFacilityScenario) {
            return isVictory ? 3 : 2;
        }

        return isVictory ? 1 : 0;
    }

    private static int getScenarioEscalation(boolean isVictory, boolean isHostileFacilityScenario) {
        int dice = getScenarioEscalationDice(isVictory, isHostileFacilityScenario);
        return (dice > 0) ? d6(dice) : 0;
    }

    /**
     * A high profile target has been struck without an ambush: +3d6 Escalation.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onHighProfileTargetStruck(Campaign campaign, AbstractContract contract) {
        increaseEscalation(campaign, contract, d6(3));
    }

    /**
     * A sabotage target has been sabotaged without the saboteurs being caught: +3d6 Escalation.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onTargetSabotaged(Campaign campaign, AbstractContract contract) {
        increaseEscalation(campaign, contract, d6(3));
    }

    /**
     * Saboteurs were caught at a sabotage target and fought, whatever the result: +2d6 Escalation. A won fight adds
     * the usual +1d6 for winning on top, for +3d6 in all - the same as a quiet success.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onSaboteursCaught(Campaign campaign, AbstractContract contract) {
        increaseEscalation(campaign, contract, d6(2));
    }

    /**
     * A show of force has been made without an ambush: -3d6 Escalation on a Garrison Duty contract.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void onShowOfForce(Campaign campaign, AbstractContract contract) {
        decreaseEscalation(campaign, contract, d6(3));
    }

    /**
     * Rolls Escalation's part of a morale check: a die with as many sides as the contract's maximum Escalation. A roll
     * no higher than the current Escalation raises the enemy's morale by one level.
     *
     * @param campaign the current campaign
     * @param contract the contract whose morale is being checked
     *
     * @return the report of the roll, to add to the morale check's report; or {@code null} if the contract does not
     *       track Escalation
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String rollForMorale(Campaign campaign, AbstractContract contract) {
        if (!isEscalationUsed(campaign, contract)) {
            return null;
        }

        return applyMoraleRoll(contract, randomInt(getMaximumEscalation(contract)) + 1);
    }

    /**
     * Applies an Escalation morale roll: if it is no higher than the current Escalation, the enemy's morale rises by
     * one level.
     *
     * @param contract the contract whose morale is being checked; it must have a StratCon state
     * @param roll     the roll, from 1 to the contract's maximum Escalation
     *
     * @return the report of the roll
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the rule can be tested without a random roll.
    static String applyMoraleRoll(AbstractContract contract, int roll) {
        int escalation = contract.getStratConCampaignState().getEscalation();
        int maximumEscalation = getMaximumEscalation(contract);

        boolean isMoraleRaised = roll <= escalation;
        if (isMoraleRaised) {
            contract.changeMorale(getRaisedMoraleLevel(contract.getMoraleLevel()));
        }

        // A garrison's Escalation is unrest rather than fighting, so it has its own wording.
        String keySuffix = isDeescalatingContract(contract) ? ".garrison.report" : ".report";
        return getFormattedTextAt(RESOURCE_BUNDLE,
              (isMoraleRaised ? "StratConEscalation.moraleRaised" : "StratConEscalation.moraleUnchanged") + keySuffix,
              spanOpeningWithCustomColor(isMoraleRaised ? getNegativeColor() : getWarningColor()),
              CLOSING_SPAN_TAG,
              roll,
              escalation,
              maximumEscalation);
    }

    /**
     * @param moraleLevel a morale level
     *
     * @return the next morale level up, or the same level if it is already the highest
     *
     * @author Illiani
     * @since 0.51.01
     */
    static ContractMoraleLevel getRaisedMoraleLevel(ContractMoraleLevel moraleLevel) {
        ContractMoraleLevel[] moraleLevels = ContractMoraleLevel.values();
        return moraleLevels[min(moraleLevel.ordinal() + 1, moraleLevels.length - 1)];
    }

    /**
     * Gives a Diversionary Raid its strategic objective: raise Escalation to 50 per point of scale before the contract
     * ends. It is added to the contract's first sector.
     *
     * @param contract      the Diversionary Raid being accepted
     * @param campaignState its StratCon state, with its sectors already set up
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void addDiversionaryRaidObjective(AbstractContract contract, StratConCampaignState campaignState) {
        if (campaignState.getTracks().isEmpty()) {
            return;
        }

        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveType(StrategicObjectiveType.Escalation);
        objective.setDesiredObjectiveCount(getDiversionaryRaidTarget(contract));
        campaignState.getTrack(0).addStrategicObjective(objective);
        updateEscalationObjectives(campaignState);
    }

    /**
     * @param campaignState a contract's StratCon state
     *
     * @return the Escalation the contract's Escalation objective asks for, or {@code null} if it has none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable Integer getEscalationTarget(StratConCampaignState campaignState) {
        for (StratConTrackState track : campaignState.getTracks()) {
            for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
                if (objective.getObjectiveType() == StrategicObjectiveType.Escalation) {
                    return objective.getDesiredObjectiveCount();
                }
            }
        }

        return null;
    }

    /**
     * Brings every Escalation objective's current count up to the contract's Escalation, capped at its desired count.
     *
     * @param campaignState the contract's StratCon state
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void updateEscalationObjectives(StratConCampaignState campaignState) {
        for (StratConTrackState track : campaignState.getTracks()) {
            for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
                if (objective.getObjectiveType() == StrategicObjectiveType.Escalation) {
                    objective.setCurrentObjectiveCount(min(campaignState.getEscalation(),
                          objective.getDesiredObjectiveCount()));
                }
            }
        }
    }
}
