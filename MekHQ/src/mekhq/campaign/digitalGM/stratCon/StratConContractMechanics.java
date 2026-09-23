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

import java.util.EnumMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.*;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;

/**
 * Everything that sets one contract type's StratCon mechanics apart from another's, in one place: which special point
 * of interest it uses, whether those replace its Essential scenarios and are strategic objectives, how it tracks
 * Escalation, whether its sectors must be scouted, and whether riots move its enemy's morale.
 *
 * <p>Look a contract type up with {@link #forObjectiveType} or {@link #forContract}. Most of these mechanics also
 * depend on the "Contracts Use Special Mechanics" option, as fixed on the contract when it was accepted (see
 * {@link StratConCampaignState#isContractsUseSpecialMechanics}); that check stays with the callers, since this
 * describes the contract type, not the campaign.</p>
 *
 * <p>The special point of interest's behavior is not recorded here: it is whatever that type's definition names, as
 * for any point of interest.</p>
 *
 * @param pointOfInterestTypeId        the type ID of the contract's special point of interest, or {@code null} if it
 *                                     has none
 * @param isReplacingEssentialScenarios whether its special points of interest replace the contract's Essential
 *                                     scenarios, the combat bonus being paid for each one dealt with instead
 * @param isPointOfInterestObjective   whether each special point of interest is a strategic objective
 * @param escalationMode               how the contract tracks Escalation (see {@link StratConEscalation})
 * @param isUsingReconnaissance        whether each of the contract's sectors must be scouted (see
 *                                     {@link StratConReconnaissance})
 * @param isRiotAffectingMorale        whether a riot's outcome moves the enemy's morale directly
 *
 * @author Illiani
 * @since 0.51.01
 */
public record StratConContractMechanics(@Nullable String pointOfInterestTypeId,
      boolean isReplacingEssentialScenarios, boolean isPointOfInterestObjective, EscalationMode escalationMode,
      boolean isUsingReconnaissance, boolean isRiotAffectingMorale) {

    /**
     * How a contract tracks Escalation (see {@link StratConEscalation}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum EscalationMode {
        /** The contract does not track Escalation. */
        NONE,
        /** Escalation starts at 0 and only ever rises. */
        ESCALATING,
        /** Escalation starts at its maximum and only ever falls: the unrest a garrison must calm. */
        DEESCALATING
    }

    /** A contract type with no special mechanics of its own. */
    private static final StratConContractMechanics NO_MECHANICS = new StratConContractMechanics(null,
          false,
          false,
          EscalationMode.NONE,
          false,
          false);

    private static final Map<ContractObjectiveType, StratConContractMechanics> MECHANICS_BY_OBJECTIVE_TYPE =
          buildMechanics();

    /**
     * @param objectiveType a contract objective type, or {@code null}
     *
     * @return that contract type's mechanics; none at all for {@code null} or an undefined type
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static StratConContractMechanics forObjectiveType(@Nullable ContractObjectiveType objectiveType) {
        if (objectiveType == null) {
            return NO_MECHANICS;
        }

        return MECHANICS_BY_OBJECTIVE_TYPE.getOrDefault(objectiveType, NO_MECHANICS);
    }

    /**
     * @param contract a contract
     *
     * @return the mechanics of the contract's objective type
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static StratConContractMechanics forContract(AbstractContract contract) {
        return forObjectiveType(contract.getObjectiveType());
    }

    /**
     * @param pointOfInterestTypeId the type ID of a special point of interest
     *
     * @return the mechanics of the contract type that uses it, or {@code null} if no contract type does
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConContractMechanics forPointOfInterestTypeId(
          @Nullable String pointOfInterestTypeId) {
        if (pointOfInterestTypeId == null) {
            return null;
        }

        for (StratConContractMechanics mechanics : MECHANICS_BY_OBJECTIVE_TYPE.values()) {
            if (pointOfInterestTypeId.equals(mechanics.pointOfInterestTypeId())) {
                return mechanics;
            }
        }

        return null;
    }

    /**
     * Decides whether a contract was set up without its Essential scenarios, its special points of interest taking
     * their place: it uses its type's special mechanics (see
     * {@link StratConCampaignState#isContractsUseSpecialMechanics}), and its type's special points of interest replace
     * Essential scenarios. Dealing with such a point of interest pays the combat bonus those scenarios would have.
     *
     * @param contract a contract
     *
     * @return {@code true} if the contract has no Essential scenarios of its own
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean areEssentialScenariosReplaced(AbstractContract contract) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        return (campaignState != null)
                     && campaignState.isContractsUseSpecialMechanics()
                     && forContract(contract).isReplacingEssentialScenarios();
    }

    /**
     * @return {@code true} if the contract type has a special point of interest
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean hasSpecialPointOfInterest() {
        return pointOfInterestTypeId != null;
    }

    private static Map<ContractObjectiveType, StratConContractMechanics> buildMechanics() {
        Map<ContractObjectiveType, StratConContractMechanics> mechanics = new EnumMap<>(ContractObjectiveType.class);

        // Every contract type starts with its Escalation mode and nothing else; those with more are filled in below.
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            mechanics.put(objectiveType, new StratConContractMechanics(null,
                  false,
                  false,
                  getEscalationMode(objectiveType),
                  false,
                  false));
        }

        // Special points of interest. Arguments: type ID, replaces Essential scenarios, is an objective.
        addPointOfInterest(mechanics, ContractObjectiveType.ESPIONAGE,
              StratConConfiguredPointOfInterestType.DATA_CACHE.getTypeId(), true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.GUERRILLA_WARFARE,
              StratConConfiguredPointOfInterestType.VULNERABLE_INFRASTRUCTURE.getTypeId(), true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.MOLE_HUNTING,
              StratConConfiguredPointOfInterestType.POTENTIAL_LEAD.getTypeId(), true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.ASSASSINATION,
              StratConAssassinationLeadBehavior.TYPE_ID, true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.OBSERVATION_RAID,
              StratConConfiguredPointOfInterestType.LOOKOUT_POINT.getTypeId(), true, true);
        // Relief Duty keeps its Essential scenarios alongside its beleaguered forces.
        addPointOfInterest(mechanics, ContractObjectiveType.RELIEF_DUTY,
              StratConConfiguredPointOfInterestType.BELEAGUERED_FORCES.getTypeId(), false, true);
        // Cadre Duty keeps its Essential scenarios alongside its training maneuvers.
        addPointOfInterest(mechanics, ContractObjectiveType.CADRE_DUTY,
              StratConTrainingManeuversBehavior.TYPE_ID, false, true);
        // A Diversionary Raid's objective is its Escalation, not its high profile targets.
        addPointOfInterest(mechanics, ContractObjectiveType.DIVERSIONARY_RAID,
              StratConHighProfileTargetBehavior.TYPE_ID, true, false);
        addPointOfInterest(mechanics, ContractObjectiveType.EXTRACTION_RAID,
              StratConConfiguredPointOfInterestType.VIP.getTypeId(), true, true);
        // Planetary Assault keeps its Essential scenarios alongside its strategic positions.
        addPointOfInterest(mechanics, ContractObjectiveType.PLANETARY_ASSAULT,
              StratConConfiguredPointOfInterestType.STRATEGIC_POSITION.getTypeId(), false, true);
        addPointOfInterest(mechanics, ContractObjectiveType.RETAINER,
              StratConScheduledParadeBehavior.TYPE_ID, true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.RIOT_DUTY,
              StratConCivilDisobedienceBehavior.TYPE_ID, true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.SABOTAGE,
              StratConSabotageTargetBehavior.TYPE_ID, true, true);
        addPointOfInterest(mechanics, ContractObjectiveType.TERRORISM,
              StratConCivilianInfrastructureBehavior.TYPE_ID, true, true);
        // Security Duty keeps its Essential scenarios alongside its security reviews.
        addPointOfInterest(mechanics, ContractObjectiveType.SECURITY_DUTY,
              StratConConfiguredPointOfInterestType.SECURITY_REVIEW.getTypeId(), false, true);
        addPointOfInterest(mechanics, ContractObjectiveType.PIRATE_RAID,
              StratConPlunderTargetBehavior.TYPE_ID, true, true);
        // Target intelligence's objectives are the facilities it leads to; an Objective Raid's combat bonus comes from
        // fighting at those.
        addPointOfInterest(mechanics, ContractObjectiveType.OBJECTIVE_RAID,
              StratConTargetIntelligenceBehavior.TYPE_ID, true, false);
        // Garrison Duty keeps its Essential scenarios; shows of force only calm its Escalation.
        addPointOfInterest(mechanics, ContractObjectiveType.GARRISON_DUTY,
              StratConShowOfForceBehavior.TYPE_ID, false, false);
        // A pirate captain's objective is the Essential scenario it is fought in.
        addPointOfInterest(mechanics, ContractObjectiveType.PIRATE_HUNTING,
              StratConPirateCaptainBehavior.TYPE_ID, true, false);

        // A Recon Raid has no special point of interest: its sectors must be scouted instead.
        StratConContractMechanics reconRaid = mechanics.get(ContractObjectiveType.RECON_RAID);
        mechanics.put(ContractObjectiveType.RECON_RAID, new StratConContractMechanics(null,
              false,
              false,
              reconRaid.escalationMode(),
              true,
              false));

        // Quelling riots is the work of a Riot Duty contract, so only there does a riot's outcome move morale.
        StratConContractMechanics riotDuty = mechanics.get(ContractObjectiveType.RIOT_DUTY);
        mechanics.put(ContractObjectiveType.RIOT_DUTY, new StratConContractMechanics(riotDuty.pointOfInterestTypeId(),
              riotDuty.isReplacingEssentialScenarios(),
              riotDuty.isPointOfInterestObjective(),
              riotDuty.escalationMode(),
              riotDuty.isUsingReconnaissance(),
              true));

        return mechanics;
    }

    private static void addPointOfInterest(Map<ContractObjectiveType, StratConContractMechanics> mechanics,
          ContractObjectiveType objectiveType, String pointOfInterestTypeId, boolean isReplacingEssentialScenarios,
          boolean isPointOfInterestObjective) {
        StratConContractMechanics existing = mechanics.get(objectiveType);
        mechanics.put(objectiveType, new StratConContractMechanics(pointOfInterestTypeId,
              isReplacingEssentialScenarios,
              isPointOfInterestObjective,
              existing.escalationMode(),
              existing.isUsingReconnaissance(),
              existing.isRiotAffectingMorale()));
    }

    /**
     * A raid or a guerrilla operation escalates, as do Sabotage, Terrorism, and Pirate Raid contracts; Garrison Duty
     * de-escalates; nothing else tracks Escalation.
     */
    private static EscalationMode getEscalationMode(ContractObjectiveType objectiveType) {
        if (objectiveType.isUndefined()) {
            return EscalationMode.NONE;
        }

        if (objectiveType.isGarrisonDuty()) {
            return EscalationMode.DEESCALATING;
        }

        if (objectiveType.isSabotage() || objectiveType.isTerrorism() || objectiveType.isPirateRaid()) {
            return EscalationMode.ESCALATING;
        }

        ChaosObjectiveType chaosObjectiveType = objectiveType.getChaosObjectiveType();
        if ((chaosObjectiveType == ChaosObjectiveType.RAID)
                  || (chaosObjectiveType == ChaosObjectiveType.GUERILLA_OPERATION)) {
            return EscalationMode.ESCALATING;
        }

        return EscalationMode.NONE;
    }
}
