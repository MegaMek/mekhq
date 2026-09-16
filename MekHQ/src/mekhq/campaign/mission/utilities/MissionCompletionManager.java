/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.utilities;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.force.Formation.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.mission.scenarios.ScenarioStatus.DRAW;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.swing.JFrame;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignNewDayManager;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.mission.contract.utilities.ContractEmergencyExtension;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.dialog.CompleteMissionDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;

/**
 * Orchestrates everything that happens when the player completes a mission (contract).
 *
 * <p>The work is split into two kinds of step. Steps that drive dialogs or can abort the whole process live as
 * instance methods and share the campaign, mission and resolved status through fields. Steps that are pure campaign
 * mutations are exposed as {@code static} helpers so they can be reused and tested independently of the GUI.</p>
 *
 * <p>Call {@link #completeMission()} once per completion attempt; it returns {@code false} if the player cancelled at
 * any of the confirmation points, in which case the caller should leave its own state untouched.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class MissionCompletionManager {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";

    private final MekHQ app;
    private final CampaignGUI campaignGui;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final JFrame frame;
    private final AbstractContract mission;

    // Resolved while the completion process runs.
    private MissionStatus missionStatus;
    private PrisonerMissionEndEvent prisonerMissionEndEvent;
    private boolean marketPreviouslyDisabled;
    private List<Person> powPersonnel;

    /**
     * Creates a manager for completing a single mission.
     *
     * @param app         the running application, used to request the pre-mission-end autosave
     * @param campaignGui the campaign GUI, used as the parent for dialogs raised during completion
     * @param mission     the mission to complete
     *
     * @author Illiani
     * @since 0.51.01
     */
    public MissionCompletionManager(MekHQ app, CampaignGUI campaignGui, AbstractContract mission) {
        this.app = app;
        this.campaignGui = campaignGui;
        this.campaign = campaignGui.getCampaign();
        this.campaignOptions = campaign.getCampaignOptions();
        this.frame = campaignGui.getFrame();
        this.mission = mission;
    }

    /**
     * Runs the full mission-completion process.
     *
     * <p>The player is offered several confirmation and resolution dialogs. If they cancel at any of them the process
     * stops immediately and this method returns {@code false} without further mutating the campaign beyond what had
     * already been applied up to that point (matching the previous inline behaviour).</p>
     *
     * @return {@code true} if the mission was fully completed, {@code false} if the player aborted at a confirmation
     *       point
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean completeMission() {
        app.getAutosaveService().requestBeforeMissionEndAutosave(campaign);

        if (!confirmCompletionAndResolveStatus()) {
            return false;
        }

        if (!resolvePrisonerDefectors()) {
            return false;
        }

        if (!resolveContractExtension()) {
            return false;
        }

        captureMarketState();

        campaign.completeMission(mission, missionStatus);
        MekHQ.triggerEvent(new MissionCompletedEvent(mission));

        payCompletionBonusAndReputation(campaign, mission, missionStatus);

        awardMissionExperience(campaign, campaignOptions, mission, missionStatus);

        powPersonnel = resolvePrisoners(campaign, prisonerMissionEndEvent, missionStatus);

        if (!resolveTurnover()) {
            return false;
        }

        promptAutoAwards();

        updateFactionStandings(campaign, campaignOptions, mission, missionStatus);

        refreshPersonnelMarketIfNeeded();

        boolean hadCadreForces = undeployFormations(campaign, mission);
        if (hadCadreForces) {
            new ImmersiveDialogNotification(campaign, getTextAt(RESOURCE_BUNDLE, "cadreReassignment.text"), true);
        }
        undeployUnits(campaign, mission);

        resolveOutstandingScenarios(mission);

        applyPirateCrimeModifier(campaign, mission);

        // Clear out any old StratCon campaign data (it's not going to be used, moving forward). We do this near the
        // end to ensure there isn't any risk of us accidentally killing the data when it's still required.
        clearStratConState(mission);

        return true;
    }

    /**
     * Shows the completion dialog and records the chosen {@link MissionStatus}.
     *
     * @return {@code true} to continue, {@code false} if the player cancelled or left the mission active
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean confirmCompletionAndResolveStatus() {
        final CompleteMissionDialog completeMissionDialog = new CompleteMissionDialog(frame, campaign, mission);
        if (!completeMissionDialog.showDialog().isConfirmed()) {
            return false;
        }

        missionStatus = completeMissionDialog.getStatus();
        return !missionStatus.isActive();
    }

    /**
     * Offers the player the chance to resolve any prisoner defectors before the mission ends.
     *
     * @return {@code true} to continue, {@code false} if the player chose to cancel
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean resolvePrisonerDefectors() {
        prisonerMissionEndEvent = new PrisonerMissionEndEvent(campaign, mission);

        // handlePrisonerDefectors() returns 0 for the cancel choice index.
        return campaign.getPlayerForce().getHumanResources().getPrisonerDefectors().isEmpty() ||
                     prisonerMissionEndEvent.handlePrisonerDefectors() != 0;
    }

    /**
     * Offers a StratCon emergency contract extension, if enabled.
     *
     * @return {@code true} to continue, {@code false} if the contract was extended (aborting completion)
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean resolveContractExtension() {
        // TODO make contract extension a campaign option
        if (campaignOptions.isUseStratCon()) {
            return !ContractEmergencyExtension.contractExtended(campaign, mission);
        }

        return true;
    }

    /**
     * Records whether the new personnel market was disabled before completion, so it can be refreshed afterwards.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void captureMarketState() {
        NewPersonnelMarket newPersonnelMarket = campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket();
        marketPreviouslyDisabled = !newPersonnelMarket.getAvailabilityMessage().isBlank();
    }

    /**
     * Resolves contract-completion turnover through the {@link RetirementDefectionDialog}, if enabled.
     *
     * @return {@code true} to continue, {@code false} if turnover could not be resolved and completion should abort
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean resolveTurnover() {
        if (!campaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT) ||
                  !campaignOptions.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT)) {
            return true;
        }

        RetirementDefectionDialog retirementDefectionDialog = new RetirementDefectionDialog(campaignGui, mission, true);

        PlayerForce playerForce = campaign.getPlayerForce();
        ForceHumanResources humanResources = playerForce.getHumanResources();

        if (retirementDefectionDialog.wasAborted()) {
            /*
             * Once the retirement rolls have been made, the outstanding payouts can be resolved without a reference to
             * the contract and the dialog can be accessed through the menu provided they aren't still assigned to the
             * mission in question.
             */
            return humanResources.getRetirementDefectionTracker().isOutstanding(mission.getId());
        }

        if ((humanResources.getRetirementDefectionTracker().getRetirees(mission) != null) &&
                  playerForce.getFinances()
                        .getBalance()
                        .isGreaterOrEqualThan(retirementDefectionDialog.totalPayout())) {
            for (PersonnelRole role : PersonnelRole.getAdministratorRoles()) {
                Person admin = humanResources.findBestInRole(role,
                      SkillType.S_ADMIN,
                      campaign.getCampaignOptions(),
                      playerForce.isClanForce(),
                      campaign.getLocalDate());
                if (admin != null) {
                    admin.awardXP(campaign, 1);
                    campaign.addReport(PERSONNEL, admin.getHyperlinkedName() + " has gained 1 XP.");
                }
            }
        }

        return campaign.applyRetirement(retirementDefectionDialog.totalPayout(),
              retirementDefectionDialog.getUnitAssignments());
    }

    /**
     * Prompts the post-mission auto awards ceremony, if enabled.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void promptAutoAwards() {
        if (campaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();

            // For the purposes of Mission Accomplished awards, we do not count partial Successes as Success.
            autoAwardsController.PostMissionController(campaign, mission, missionStatus.isSuccess(), powPersonnel);
        }
    }

    /**
     * Refreshes the personnel market if it had been disabled before completion.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void refreshPersonnelMarketIfNeeded() {
        if (marketPreviouslyDisabled) {
            campaign.getPlayerForce().getHumanResources().refreshApplicants(campaign, true);
            CampaignNewDayManager.showRarePersonnelDialog(campaign, false);
        }
    }

    /**
     * Pays any contract completion bonus and processes chaos reputation changes.
     *
     * @param campaign the campaign being updated
     * @param mission  the completed mission
     * @param status   the mission's final status
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void payCompletionBonusAndReputation(Campaign campaign, AbstractContract mission,
          MissionStatus status) {
        // Pay the completion bonus, if the contract earned one (Completion Bonus characteristic, success only).
        ContractCharacteristics.payCompletionBonus(campaign, mission, status);

        if (campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION)) {
            List<Person> personnel = campaign.getPlayerForce()
                                            .getHumanResources()
                                            .getPersonnelFilteringOutDepartedAndAbsent();
            ChaosReputation.processContractCompletion(campaign, status, personnel,
                  ContractCharacteristics.getUnitReputationMultiplier(mission, status));

            if (mission.getEmployerFactionCode().equals(PIRATE_FACTION_CODE)) {
                ChaosReputation.resolveActOfPiracy(campaign,
                      personnel,
                      mission.getScale(),
                      mission.getScenarios(),
                      status.isOverallSuccess(),
                      mission.getName());
            }
        }
    }

    /**
     * Awards mission-completion XP to all eligible active personnel.
     *
     * @param campaign        the campaign being updated
     * @param campaignOptions the campaign options controlling XP amounts
     * @param mission         the completed mission
     * @param status          the mission's final status
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void awardMissionExperience(Campaign campaign, CampaignOptions campaignOptions,
          AbstractContract mission, MissionStatus status) {
        int xpAward = getMissionExperienceAward(campaignOptions, status, mission);
        if (xpAward <= 0) {
            return;
        }

        LocalDate today = campaign.getLocalDate();
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.isChild(today)) {
                continue;
            }

            if (person.isDependent()) {
                continue;
            }

            person.awardXP(campaign, xpAward);
        }
    }

    /**
     * Resolves outstanding friendly and current prisoners once no active contracts remain.
     *
     * <p>The list of friendly prisoners is captured <em>before</em> any resolution so it can later be handed to the
     * auto awards ceremony.</p>
     *
     * @param campaign                the campaign being updated
     * @param prisonerMissionEndEvent the prisoner event used to resolve prisoners
     * @param status                  the mission's final status
     *
     * @return the friendly (POW) personnel captured before resolution
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<Person> resolvePrisoners(Campaign campaign, PrisonerMissionEndEvent prisonerMissionEndEvent,
          MissionStatus status) {
        PlayerForce playerForce = campaign.getPlayerForce();
        ForceHumanResources humanResources = playerForce.getHumanResources();
        List<Person> powPersonnel = humanResources.getFriendlyPrisoners();

        // We only resolve prisoners if there are no active Missions.
        if (campaign.getActiveContracts().isEmpty()) {
            if (!powPersonnel.isEmpty()) {
                prisonerMissionEndEvent.handlePrisoners(status.isOverallSuccess(), true);
            }

            if (!humanResources.getCurrentPrisoners().isEmpty()) {
                prisonerMissionEndEvent.handlePrisoners(status.isOverallSuccess(), false);
            }

            playerForce.setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }

        return powPersonnel;
    }

    /**
     * Applies the contract-completion changes to the player's faction standings, if tracked.
     *
     * @param campaign        the campaign being updated
     * @param campaignOptions the campaign options controlling faction standing changes
     * @param mission         the completed mission
     * @param status          the mission's final status
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void updateFactionStandings(Campaign campaign, CampaignOptions campaignOptions,
          AbstractContract mission, MissionStatus status) {
        if (!campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)) {
            return;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        FactionStandings factionStandings = playerForce.getFactionStandings();

        // The employer's disposition characteristic (Employer's Favorite / On Probation) scales the standing change.
        double regardMultiplier = campaignOptions.get(CampaignOption.REGARD_MULTIPLIER) *
                                        ContractCharacteristics.getEmployerRegardMultiplier(mission);

        // A covert sponsor, if any, takes the standing change in the visible employer's place.
        Faction employer = mission.getStandingEmployerFaction();
        factionStandings.processContractCompletion(playerForce.getFaction(),
              employer,
              campaign.getLocalDate(),
              status,
              regardMultiplier,
              mission.getLengthInMonths());
    }

    /**
     * Undeploys all formations from the completed mission's scenarios, reverting cadre roles to frontline.
     *
     * @param campaign the campaign being updated
     * @param mission  the completed mission
     *
     * @return {@code true} if any cadre formations were reassigned, so the caller can notify the player
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean undeployFormations(Campaign campaign, AbstractContract mission) {
        boolean isCadreDuty = mission.getObjectiveType().isCadreDuty();
        boolean hadCadreForces = false;
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            if (isCadreDuty && formation.getCombatRoleInMemory().isCadre()) {
                formation.setCombatRoleInMemory(CombatRole.FRONTLINE);
                hadCadreForces = true;
            }

            int scenarioAssignment = formation.getScenarioId();
            if (scenarioAssignment != NO_ASSIGNED_SCENARIO) {
                Scenario scenario = campaign.getScenario(scenarioAssignment);

                // This shouldn't be necessary, but now is as good a time as any to check for null scenarios.
                if (scenario == null || Objects.equals(scenario.getMissionId(), mission.getId())) {
                    formation.setScenarioId(NO_ASSIGNED_SCENARIO, campaign);
                }
            }
        }

        return hadCadreForces;
    }

    /**
     * Undeploys all units from the completed mission's scenarios.
     *
     * @param campaign the campaign being updated
     * @param mission  the completed mission
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void undeployUnits(Campaign campaign, AbstractContract mission) {
        for (Unit unit : campaign.getUnits()) {
            int scenarioAssignment = unit.getScenarioId();
            if (scenarioAssignment != NO_ASSIGNED_SCENARIO) {
                Scenario scenario = campaign.getScenario(scenarioAssignment);

                // This shouldn't be necessary, but now is as good a time as any to check for null scenarios.
                if (scenario == null || Objects.equals(scenario.getMissionId(), mission.getId())) {
                    unit.setScenarioId(NO_ASSIGNED_SCENARIO);
                }
            }
        }
    }

    /**
     * Marks any of the mission's still-current scenarios as a {@link mekhq.campaign.mission.scenarios.ScenarioStatus#DRAW}.
     *
     * @param mission the completed mission
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void resolveOutstandingScenarios(AbstractContract mission) {
        for (Scenario scenario : mission.getCurrentScenarios()) {
            scenario.setStatus(DRAW);
        }
    }

    /**
     * Applies the CamOps 'other crimes' pirate modifier when the employer is a pirate faction.
     *
     * @param campaign the campaign being updated
     * @param mission  the completed mission
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void applyPirateCrimeModifier(Campaign campaign, AbstractContract mission) {
        if (mission.getEmployerFactionCode().equals(PIRATE_FACTION_CODE)) {
            // CamOps 'other crimes' value
            campaign.getPlayerForce().changeCrimePirateModifier(10);
        }
    }

    /**
     * Clears any StratCon campaign state left on the completed mission.
     *
     * @param mission the completed mission
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void clearStratConState(AbstractContract mission) {
        mission.setStratConCampaignState(null);
    }

    /**
     * Calculates the XP award for completing a mission.
     *
     * @param campaignOptions the campaign options controlling XP amounts
     * @param missionStatus   the status of the mission as a {@link MissionStatus}
     * @param mission         the completed mission
     *
     * @return the XP award for completing the mission
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getMissionExperienceAward(CampaignOptions campaignOptions, MissionStatus missionStatus,
          AbstractContract mission) {
        return switch (missionStatus) {
            case FAILED, BREACH -> campaignOptions.get(CampaignOption.MISSION_XP_FAIL);
            case SUCCESS, PARTIAL -> {
                StratConCampaignState stratConCampaignState = mission.getStratConCampaignState();
                if (stratConCampaignState != null && stratConCampaignState.getVictoryPoints() >= 3) {
                    yield campaignOptions.get(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS);
                } else {
                    yield campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS);
                }
            }
            case ACTIVE -> 0;
        };
    }
}
