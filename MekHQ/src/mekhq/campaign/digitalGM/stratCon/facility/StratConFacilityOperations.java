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
package mekhq.campaign.digitalGM.stratCon.facility;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * The rules for the orders a player gives deployed formations at facilities (see {@link FacilityOperation}), and for
 * what the player does with a facility they capture (see {@link FacilityCaptureChoice}). All of it applies only with
 * the Facility Operations campaign option; without it, deploying onto an enemy facility starts an assault, as it
 * always has.
 *
 * <p>Every order costs support points, paid when it is given. Raid and Assault start a scenario on the facility at
 * once. Sabotage is rolled at once, and only a caught saboteur fights. Fortify and Reinforce take effect at once.
 * Recon and Build take time: the formation stays deployed until the order completes, and the order is stored on the
 * sector until then.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilityOperations {
    static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";

    static final int RECON_COST = 0;
    static final int RAID_COST = 1;
    static final int SABOTAGE_COST = 2;
    static final int ASSAULT_COST = 0;
    static final int FORTIFY_COST = 3;
    static final int REINFORCE_COST = 1;
    static final int BUILD_COST = 3;
    static final int INTERDICT_COST = 1;

    static final int RECON_DAYS = 7;
    static final int BUILD_DAYS = 14;

    /** The 2d6 roll a recon must meet. */
    static final int RECON_TARGET_NUMBER = 7;
    /** Added to the recon roll when the formation is on Patrol. */
    static final int RECON_PATROL_BONUS = 2;
    /** The 2d6 roll a sabotage must meet. */
    static final int SABOTAGE_TARGET_NUMBER = 8;

    static final String ASSAULT_MODIFIER = "FacilityHostileCapture.json";
    static final String RAID_MODIFIER = "FacilityHostileExtract.json";
    static final String RECON_MODIFIER = "FacilityHostileRecon.json";
    static final String SABOTAGE_MODIFIER = "FacilityHostileEngage.json";
    /** The extra enemy force caught saboteurs face. */
    public static final String SABOTAGE_CAUGHT_MODIFIER = "HostileBVBudgetIncrease.json";

    private StratConFacilityOperations() {
    }

    /**
     * @param campaign the current campaign
     *
     * @return {@code true} if players give facility orders, rather than deploying onto a facility to assault it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isEnabled(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        // Compared rather than unboxed, so an options object without the value set reads as off.
        return Boolean.TRUE.equals(campaignOptions.get(CampaignOption.USE_FACILITY_OPERATIONS))
                     && !campaignOptions.isUseStratConMaplessMode();
    }

    /**
     * @param operation an order
     *
     * @return what the order costs in support points
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getSupportPointCost(FacilityOperation operation) {
        return switch (operation) {
            case RECON -> RECON_COST;
            case RAID -> RAID_COST;
            case SABOTAGE -> SABOTAGE_COST;
            case ASSAULT -> ASSAULT_COST;
            case FORTIFY -> FORTIFY_COST;
            case REINFORCE -> REINFORCE_COST;
            case BUILD -> BUILD_COST;
            case INTERDICT -> INTERDICT_COST;
        };
    }

    /**
     * @param operation an order that can lead to a fight on a facility
     *
     * @return the ID of the scenario modifier holding that fight's objective; an assault's for an order that never
     *       fights
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getObjectiveModifierId(FacilityOperation operation) {
        return switch (operation) {
            case RAID -> RAID_MODIFIER;
            case RECON -> RECON_MODIFIER;
            case SABOTAGE -> SABOTAGE_MODIFIER;
            default -> ASSAULT_MODIFIER;
        };
    }

    /**
     * @param track  a sector
     * @param coords a hex in it
     *
     * @return the orders that make sense on that hex, whether or not they can be given right now: the enemy facility
     *       orders on an enemy facility, Fortify and Reinforce on the player's side's facility, and Build on a hex
     *       with nothing on it, along with Interdict Supply if it carries a road
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<FacilityOperation> getOperationsFor(StratConTrackState track, StratConCoords coords) {
        List<FacilityOperation> operations = new ArrayList<>();
        StratConFacility facility = track.getFacility(coords);
        for (FacilityOperation operation : FacilityOperation.values()) {
            boolean isSuitable;
            if (facility == null) {
                isSuitable = (operation == FacilityOperation.BUILD)
                                   || ((operation == FacilityOperation.INTERDICT) && track.isRoad(coords));
            } else if (facility.isOwnerAlliedToPlayer()) {
                isSuitable = operation.isOnOwnFacility();
            } else {
                isSuitable = operation.isAgainstEnemyFacility();
            }

            if (isSuitable) {
                operations.add(operation);
            }
        }
        return operations;
    }

    /**
     * Checks whether an order can be given on a hex at all, leaving aside which formation would carry it out.
     *
     * @param campaign  the current campaign
     * @param contract  the contract whose map holds the sector
     * @param track     the sector
     * @param coords    the hex the order acts on
     * @param operation the order
     *
     * @return the resource key of the reason it cannot be given, or {@code null} if it can
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String getUnavailableReasonKey(Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords, FacilityOperation operation) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaignState == null) || (campaignState.getSupportPoints() < getSupportPointCost(operation))) {
            return "reason.supportPoints";
        }

        StratConFacility facility = track.getFacility(coords);
        if (operation == FacilityOperation.BUILD) {
            boolean isHexOccupied = (facility != null)
                                          || (track.getScenario(coords) != null)
                                          || track.isCity(coords)
                                          || StratConPointOfInterestRules.hasActivePointOfInterest(track, coords)
                                          || isBuildUnderway(track, coords);
            if (isHexOccupied) {
                return "reason.hexOccupied";
            }
            return getBuildableDefinitions().isEmpty() ? "reason.nothingToBuild" : null;
        }

        if (operation == FacilityOperation.INTERDICT) {
            return getInterdictUnavailableReasonKey(campaign, track, coords);
        }

        if (facility == null) {
            return "reason.noFacility";
        }

        if (operation.isOnOwnFacility()) {
            if (!facility.isOwnerAlliedToPlayer()) {
                return "reason.notHeld";
            }
            if ((operation == FacilityOperation.FORTIFY) && (facility.getTier() == FacilityTier.STRONGHOLD)) {
                return "reason.maximumTier";
            }
            if ((operation == FacilityOperation.REINFORCE)
                      && (facility.getGarrison() >= facility.getGarrisonMaximum())) {
                return "reason.garrisonFull";
            }
            return null;
        }

        if (facility.isOwnerAlliedToPlayer()) {
            return "reason.notHostile";
        }
        if (track.getScenario(coords) != null) {
            return "reason.scenarioUnderway";
        }

        return switch (operation) {
            case RECON -> facility.getIntel().isAtLeast(FacilityIntel.DETAILED) ? "reason.fullyScouted" : null;
            case RAID -> contract.isBatchallAccepted() ? "reason.batchall" : null;
            case SABOTAGE -> getSabotageUnavailableReasonKey(campaignState, contract, facility);
            default -> null;
        };
    }

    private static @Nullable String getSabotageUnavailableReasonKey(StratConCampaignState campaignState,
          AbstractContract contract, StratConFacility facility) {
        if (contract.isBatchallAccepted()) {
            return "reason.batchall";
        }

        ContractObjectiveType objectiveType = contract.getObjectiveType();
        boolean isCovertContract = (objectiveType == ContractObjectiveType.ESPIONAGE)
                                         || (objectiveType == ContractObjectiveType.SABOTAGE);
        if (!isCovertContract && !campaignState.isContractsUseSpecialMechanics()) {
            return "reason.sabotageContract";
        }
        if (!facility.getIntel().isAtLeast(FacilityIntel.DETAILED)) {
            return "reason.needsDetailedIntel";
        }
        return (facility.getCondition() == FacilityCondition.CRIPPLED) ? "reason.alreadyCrippled" : null;
    }

    private static @Nullable String getInterdictUnavailableReasonKey(Campaign campaign, StratConTrackState track,
          StratConCoords coords) {
        if (!StratConFacilitySupply.isSupplyLinesActive(campaign)) {
            return "reason.supplyLinesOff";
        }
        if (!track.isRoad(coords) || (track.getFacility(coords) != null)) {
            return "reason.notRoad";
        }
        if (track.getScenario(coords) != null) {
            return "reason.scenarioUnderway";
        }
        return track.isRoadCut(coords) ? "reason.alreadyCut" : null;
    }

    private static boolean isBuildUnderway(StratConTrackState track, StratConCoords coords) {
        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if ((order.getOperation() == FacilityOperation.BUILD) && order.getTargetCoords().equals(coords)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param campaign  the current campaign
     * @param track     the sector
     * @param coords    the hex the order acts on
     * @param operation the order
     *
     * @return the IDs of the formations that could carry out the order, in ID order: those deployed on the hex, or
     *       next to it for an order that allows that, that are neither in a scenario nor carrying out another order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Integer> getEligibleFormationIds(Campaign campaign, StratConTrackState track,
          StratConCoords coords, FacilityOperation operation) {
        List<Integer> formationIds = new ArrayList<>();
        for (Map.Entry<Integer, StratConCoords> assignment : track.getAssignedForceCoords().entrySet()) {
            int formationId = assignment.getKey();
            if (!isInReach(assignment.getValue(), coords, operation) || (track.getFacilityOrder(formationId) != null)) {
                continue;
            }

            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            if ((formation != null) && !formation.isDeployed()) {
                formationIds.add(formationId);
            }
        }
        Collections.sort(formationIds);
        return formationIds;
    }

    /**
     * @param formationCoords where a formation is deployed
     * @param targetCoords    the hex an order acts on
     * @param operation       the order
     *
     * @return {@code true} if the formation is on the hex, or next to it for an order that allows that
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isInReach(@Nullable StratConCoords formationCoords, StratConCoords targetCoords,
          FacilityOperation operation) {
        if (formationCoords == null) {
            return false;
        }
        if (formationCoords.equals(targetCoords)) {
            return true;
        }
        if (!operation.isAllowedFromAdjacentHex()) {
            return false;
        }

        for (int direction = 0; direction < 6; direction++) {
            if (targetCoords.translate(direction).equals(formationCoords)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the facility definitions a Build order can raise: those with a profile for the player's side
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConFacilityDefinition> getBuildableDefinitions() {
        return StratConFacilityFactory.getDefinitionsFor(ForceAlignment.Player);
    }

    /**
     * Gives an order, paying its support points. An order that cannot be given, or whose scenario could not be
     * generated, changes nothing and costs nothing.
     *
     * @param campaign        the current campaign
     * @param contract        the contract whose map holds the sector
     * @param track           the sector
     * @param coords          the hex the order acts on
     * @param formationId     the ID of the formation carrying it out
     * @param operation       the order
     * @param buildDefinition for {@link FacilityOperation#BUILD}, what to build; otherwise ignored
     *
     * @return {@code true} if the order was given
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean issueOrder(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords, int formationId, FacilityOperation operation,
          @Nullable StratConFacilityDefinition buildDefinition) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        boolean isBuildWithoutDefinition = (operation == FacilityOperation.BUILD) && (buildDefinition == null);
        if ((campaignState == null)
                  || isBuildWithoutDefinition
                  || (getUnavailableReasonKey(campaign, contract, track, coords, operation) != null)
                  || !getEligibleFormationIds(campaign, track, coords, operation).contains(formationId)) {
            return false;
        }

        int cost = getSupportPointCost(operation);
        campaignState.changeSupportPoints(-cost);

        boolean isGiven = switch (operation) {
            case RAID, ASSAULT -> StratConRulesManager.startFacilityOperationScenario(campaign,
                  contract,
                  track,
                  coords,
                  formationId,
                  operation) != null;
            case INTERDICT -> StratConRulesManager.startInterdictionScenario(campaign,
                  contract,
                  track,
                  coords,
                  formationId) != null;
            case SABOTAGE -> {
                resolveSabotage(campaign, contract, track, coords, formationId, d6(2));
                yield true;
            }
            case FORTIFY -> {
                StratConFacility facility = track.getFacility(coords);
                facility.setTier(facility.getTier().next());
                report(campaign, "report.fortified", facility.getDisplayableName());
                yield true;
            }
            case REINFORCE -> {
                StratConFacility facility = track.getFacility(coords);
                facility.setGarrison(facility.getGarrisonMaximum());
                report(campaign, "report.reinforced", facility.getDisplayableName());
                yield true;
            }
            case RECON, BUILD -> {
                int days = (operation == FacilityOperation.RECON) ? RECON_DAYS : BUILD_DAYS;
                String definitionId = (buildDefinition == null) ? null : buildDefinition.getId();
                track.addFacilityOrder(new StratConFacilityOrder(operation,
                      formationId,
                      coords,
                      campaign.getLocalDate().plusDays(days),
                      definitionId));
                // The formation holds its position until the order completes.
                track.addStickyForce(formationId);
                yield true;
            }
        };

        if (!isGiven) {
            campaignState.changeSupportPoints(cost);
        }
        return isGiven;
    }

    /**
     * Resolves a sabotage attempt. On a success the facility's condition drops two steps and Escalation rises by
     * 3d6; otherwise the saboteurs are caught, Escalation rises by 2d6, and they fight their way out against extra
     * enemy forces.
     *
     * @param campaign    the current campaign
     * @param contract    the contract whose map holds the sector
     * @param track       the sector
     * @param coords      the facility's hex
     * @param formationId the ID of the formation carrying out the sabotage
     * @param roll        the 2d6 roll
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void resolveSabotage(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords, int formationId, int roll) {
        StratConFacility facility = track.getFacility(coords);
        if (roll >= SABOTAGE_TARGET_NUMBER) {
            facility.setCondition(facility.getCondition().worsened().worsened());
            StratConEscalation.onTargetSabotaged(campaign, contract);
            report(campaign, "report.sabotage.success", facility.getDisplayableName());
            return;
        }

        StratConEscalation.onSaboteursCaught(campaign, contract);
        report(campaign, "report.sabotage.caught", facility.getDisplayableName());
        StratConRulesManager.startFacilityOperationScenario(campaign,
              contract,
              track,
              coords,
              formationId,
              FacilityOperation.SABOTAGE);
    }

    /**
     * Moves the sector's timed orders on by a day. An order is abandoned if its formation has left, or, for a
     * build, if anything else has turned up on the hex; the support points it cost are not returned. An order due
     * today completes.
     *
     * @param track    the sector
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processOrders(StratConTrackState track, Campaign campaign) {
        if (track.getFacilityOrders().isEmpty()) {
            return;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        LocalDate today = campaign.getLocalDate();
        for (StratConFacilityOrder order : List.copyOf(track.getFacilityOrders())) {
            if (!isOrderStillPossible(track, order)) {
                endOrder(track, order);
                report(campaign,
                      "report.abandoned." + order.getOperation().name(),
                      getFormationName(campaign, order.getFormationId()));
                continue;
            }

            if ((contract != null) && !today.isBefore(order.getCompletionDate())) {
                endOrder(track, order);
                if (order.getOperation() == FacilityOperation.RECON) {
                    resolveRecon(campaign, contract, track, order, d6(2));
                } else {
                    completeBuild(campaign, contract, track, order);
                }
            }
        }
    }

    /**
     * @param track a sector
     * @param order a timed order in it
     *
     * @return {@code true} if the formation is still in reach of the hex and nothing has spoiled the order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isOrderStillPossible(StratConTrackState track, StratConFacilityOrder order) {
        StratConCoords targetCoords = order.getTargetCoords();
        StratConCoords formationCoords = track.getAssignedForceCoords().get(order.getFormationId());
        if (!isInReach(formationCoords, targetCoords, order.getOperation())) {
            return false;
        }

        StratConFacility facility = track.getFacility(targetCoords);
        if (order.getOperation() == FacilityOperation.RECON) {
            return (facility != null) && !facility.isOwnerAlliedToPlayer();
        }

        // A build is interrupted by any scenario on its hex.
        return (facility == null) && (track.getScenario(targetCoords) == null);
    }

    private static void endOrder(StratConTrackState track, StratConFacilityOrder order) {
        track.removeFacilityOrder(order);
        track.removeStickyForce(order.getFormationId());
    }

    /**
     * Resolves a completed recon. A success raises what the player knows about the facility by one step; a failure
     * means the formation was spotted and must fight its way clear.
     *
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param order    the completed recon order, already ended
     * @param roll     the 2d6 roll, before the Patrol bonus
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void resolveRecon(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConFacilityOrder order, int roll) {
        StratConFacility facility = track.getFacility(order.getTargetCoords());
        int formationId = order.getFormationId();
        int total = roll + (isOnPatrol(campaign, formationId) ? RECON_PATROL_BONUS : 0);
        if (total >= RECON_TARGET_NUMBER) {
            facility.raiseIntel(facility.getIntel().next());
            report(campaign, "report.recon.success", facility.getDisplayableName());
            return;
        }

        report(campaign, "report.recon.spotted", facility.getDisplayableName());
        StratConRulesManager.startFacilityOperationScenario(campaign,
              contract,
              track,
              order.getTargetCoords(),
              formationId,
              FacilityOperation.RECON);
    }

    private static boolean isOnPatrol(Campaign campaign, int formationId) {
        CombatTeam combatTeam = campaign.getPlayerForce().getCombatTeamsAsMap(campaign).get(formationId);
        return (combatTeam != null) && combatTeam.getRole().isPatrol();
    }

    /**
     * Completes a build: a new Outpost, held by the player at full garrison, appears on the hex.
     *
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param order    the completed build order, already ended
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void completeBuild(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConFacilityOrder order) {
        StratConFacilityDefinition definition = StratConFacilityFactory.getDefinition(order.getDefinitionId());
        if (definition == null) {
            return;
        }

        StratConFacility facility = new StratConFacility(definition, ForceAlignment.Player);
        facility.setTier(FacilityTier.OUTPOST);
        facility.setGarrison(facility.getGarrisonMaximum());
        track.addFacility(order.getTargetCoords(), facility);
        track.getRevealedCoords().add(order.getTargetCoords());
        // A new base belongs on the road grid, as one placed by any other means does.
        StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);
        report(campaign, "report.built", facility.getDisplayableName());
    }

    /**
     * Asks the player what to do with a facility they have just captured. Without a GUI to ask through, they hold
     * it.
     *
     * @param campaign the current campaign
     * @param facility the captured facility
     *
     * @return the player's choice
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static FacilityCaptureChoice askCaptureChoice(Campaign campaign, StratConFacility facility) {
        if (campaign.getGUI() == null) {
            return FacilityCaptureChoice.HOLD;
        }

        List<String> buttonLabels = new ArrayList<>();
        for (FacilityCaptureChoice choice : FacilityCaptureChoice.values()) {
            buttonLabels.add(getTextAt(RESOURCE_BUNDLE, "capture.button." + choice.name()));
        }

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "capture.message", facility.getDisplayableName()),
              buttonLabels,
              getTextAt(RESOURCE_BUNDLE, "capture.ooc"));
        int choiceIndex = dialog.getDialogChoice();
        FacilityCaptureChoice[] choices = FacilityCaptureChoice.values();
        boolean isValidChoice = (choiceIndex >= 0) && (choiceIndex < choices.length);
        return isValidChoice ? choices[choiceIndex] : FacilityCaptureChoice.HOLD;
    }

    /**
     * Carries out the player's choice for a facility they have just captured, which is now Allied.
     *
     * <ul>
     *     <li>{@link FacilityCaptureChoice#HOLD}: the player holds it.</li>
     *     <li>{@link FacilityCaptureChoice#RAZE}: it is destroyed, raising Escalation by 3d6 for a civilian facility
     *     (a Spaceport, Data Center or Industrial Facility), or 1d6 otherwise.</li>
     *     <li>{@link FacilityCaptureChoice#HAND_OVER}: it stays with the employer, at full garrison, and the contract's
     *     combat bonus is paid.</li>
     * </ul>
     *
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param coords   the facility's hex
     * @param choice   the player's choice
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolveCapture(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords, FacilityCaptureChoice choice) {
        StratConFacility facility = track.getFacility(coords);
        if (facility == null) {
            return;
        }

        String facilityName = facility.getDisplayableName();
        switch (choice) {
            case HOLD -> facility.setOwner(ForceAlignment.Player);
            case RAZE -> {
                track.removeFacility(coords);
                if (isCivilian(facility.getFacilityType())) {
                    StratConEscalation.onCivilianInfrastructureDestroyed(campaign, contract);
                } else {
                    StratConEscalation.increaseEscalationByDice(campaign, contract, 1);
                }
            }
            case HAND_OVER -> {
                facility.setOwner(ForceAlignment.Allied);
                facility.setGarrison(facility.getGarrisonMaximum());
                StratConRulesManager.awardCombatBonus(campaign, contract);
            }
        }
        report(campaign, "report.capture." + choice.name(), facilityName);
    }

    /**
     * @param facilityType a facility type
     *
     * @return {@code true} if razing a facility of that type harms civilians
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isCivilian(@Nullable FacilityType facilityType) {
        return (facilityType == FacilityType.SpacePort)
                     || (facilityType == FacilityType.DataCenter)
                     || (facilityType == FacilityType.IndustrialFacility);
    }

    private static String getFormationName(Campaign campaign, int formationId) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        return (formation == null) ? String.valueOf(formationId) : formation.getName();
    }

    private static void report(Campaign campaign, String key, String name) {
        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE, key, name));
    }
}
