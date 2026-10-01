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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.processIgnoredDynamicScenario;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.switchFacilityOwner;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationEnemy;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationTerms;
import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.dialog.nagDialogs.ContractSpecialMechanicsNagDialog;
import mekhq.gui.dialog.nagDialogs.StratConFacilityBriefingNagDialog;

/**
 * Handles a defensive StratCon contract whose employer loses control of the planet the contract is fought on.
 *
 * <p>Planet ownership follows the canon faction history, so this is a strategic collapse elsewhere, not a failure of
 * the player's unit. The change is spotted a day ahead, as the day ends: if the unit has arrived on the planet, the
 * player is asked (as a nag, so they can also cancel the day advance) whether to join the evacuation or lead the
 * resistance. The chosen response is then applied as the new day - the first day the employer no longer holds the
 * planet - is processed:</p>
 *
 * <ul>
 *     <li>every scenario still active on the contract is resolved as a Decisive Defeat;</li>
 *     <li><b>Join the Evacuation</b> - the contract ends the next day, like a rout, and is to be recorded as a partial
 *     success with no remaining pay;</li>
 *     <li><b>Lead the Resistance</b> - the contract is regenerated as a Guerrilla Warfare contract against the planet's
 *     new owner, starting that day;</li>
 *     <li>if the unit had not arrived, the contract is cancelled that day as a partial success with no remaining
 *     pay.</li>
 * </ul>
 *
 * <p>Only a change of hands triggers this: the employer must hold the planet on one day and not the next. A contract
 * on a world the employer never held is never affected, and nor is one on a world the employer still shares with
 * other factions.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class EmployerLostPlanet {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EmployerLostPlanet";

    private EmployerLostPlanet() {}

    /**
     * @param campaign the current campaign
     *
     * @return {@code true} if StratCon is in use and the campaign reacts to employers losing their planets
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isEnabled(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        return campaignOptions.isUseStratCon()
                     && campaignOptions.get(CampaignOption.USE_EMPLOYER_LOST_PLANET_REACTIONS);
    }

    /**
     * Decides whether a contract's employer loses control of the contract's planet between two days.
     *
     * <p>Only defensive contracts run on a StratCon map qualify. The employer (the real backer, for a covert
     * contract) must be among the planet's owners on {@code before} and absent from them on {@code after}.</p>
     *
     * @param contract the contract to test
     * @param before   the last day the employer might still hold the planet
     * @param after    the day to test for the loss
     *
     * @return {@code true} if the employer holds the planet on {@code before} but not on {@code after}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isEmployerLosingControl(AbstractContract contract, LocalDate before, LocalDate after) {
        if ((contract.getStratConCampaignState() == null) || (contract.getObjectiveType() == null)
                  || contract.isPlayerAttacker()) {
            return false;
        }

        Faction employer = contract.getStandingEmployerFaction();
        if (employer == null) {
            return false;
        }

        String employerCode = employer.getShortName();
        return getOwnerCodes(contract, before).contains(employerCode)
                     && !getOwnerCodes(contract, after).contains(employerCode);
    }

    /**
     * @return the codes of the factions holding the contract's planet (or, when the planet is unknown, its system) on
     *       the given day
     */
    private static List<String> getOwnerCodes(AbstractContract contract, LocalDate date) {
        Planet planet = contract.getTargetPlanet();
        if (planet != null) {
            return planet.getFactions(date);
        }

        PlanetarySystem system = contract.getTargetSystem();
        return (system == null) ? List.of() : system.getFactions(date);
    }

    /**
     * Finds the contracts whose employer loses control of their planet between two days, including contracts the
     * unit is still travelling to.
     *
     * @param campaign the current campaign
     * @param before   the last day the employer might still hold the planet
     * @param after    the day to test for the loss
     *
     * @return the affected contracts, empty when the campaign does not use these reactions
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<AbstractContract> getContractsLosingControl(Campaign campaign, LocalDate before,
          LocalDate after) {
        List<AbstractContract> affectedContracts = new ArrayList<>();
        if (!isEnabled(campaign)) {
            return affectedContracts;
        }

        for (AbstractContract contract : campaign.getActiveContracts(true)) {
            if (isEmployerLosingControl(contract, before, after)) {
                affectedContracts.add(contract);
            }
        }
        return affectedContracts;
    }

    /**
     * Finds the contracts that need the player's response before the day can end: those whose employer loses control
     * of their planet tomorrow, where the unit has already arrived. Contracts the unit has yet to reach are simply
     * cancelled when the day advances, so they need no response.
     *
     * @param campaign the current campaign
     *
     * @return the contracts awaiting a response
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<AbstractContract> getContractsAwaitingResponse(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();
        AbstractLocation location = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();

        List<AbstractContract> awaitingContracts = new ArrayList<>();
        for (AbstractContract contract : getContractsLosingControl(campaign, today, today.plusDays(1))) {
            if (ContractUtilities.hasArrivedAtContractLocation(location, contract)) {
                awaitingContracts.add(contract);
            }
        }
        return awaitingContracts;
    }

    /**
     * Applies the consequences of every employer that lost control of its contract's planet today. Called as the new
     * day is processed, once the date has advanced.
     *
     * <p>A contract carrying a response from the day-ending nag gets that response. One without was not arrived at
     * when the nag was shown, and is cancelled.</p>
     *
     * @param campaign the current campaign, already on the new day
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processNewDay(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();
        for (AbstractContract contract : getContractsLosingControl(campaign, today.minusDays(1), today)) {
            PlanetLossResponse response = contract.getPendingPlanetLossResponse();
            contract.setPendingPlanetLossResponse(null);

            resolveActiveScenariosAsDefeats(campaign, contract);

            if (response == null) {
                cancelBeforeArrival(campaign, contract, today);
            } else {
                switch (response) {
                    case JOIN_EVACUATION -> joinEvacuation(campaign, contract, today);
                    case LEAD_RESISTANCE -> leadResistance(campaign, contract, today);
                }
            }

            MekHQ.triggerEvent(new MissionChangedEvent(contract));
        }
    }

    /**
     * Resolves every scenario still active on the contract as a Decisive Defeat. StratCon scenarios are first
     * resolved on the map as ignored scenarios would be, so any allied facility they were fought over falls to the
     * enemy and any objective tied to them fails.
     */
    private static void resolveActiveScenariosAsDefeats(Campaign campaign, AbstractContract contract) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        List<Scenario> activeScenarios = contract.getCurrentScenarios();

        for (Scenario scenario : activeScenarios) {
            if ((campaignState != null) && (scenario instanceof AtBDynamicScenario)) {
                processIgnoredDynamicScenario(scenario.getId(), campaignState, campaign);
            }
            scenario.convertToStub(campaign, ScenarioStatus.DECISIVE_DEFEAT);
        }

        if (!activeScenarios.isEmpty()) {
            campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE, "scenariosLost.report",
                  contract.getHyperlinkedName(), activeScenarios.size()));
        }
    }

    /**
     * Ends the contract the next day, as a rout does. It is to be recorded as a partial success, which pays nothing
     * more.
     */
    private static void joinEvacuation(Campaign campaign, AbstractContract contract, LocalDate today) {
        contract.endContractEarly(today.plusDays(1), Money.zero());
        contract.setMandatedCompletionStatus(MissionStatus.PARTIAL);

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "evacuation.report",
              contract.getHyperlinkedName(), getPlanetName(contract, today)));
    }

    /**
     * Cancels a contract the unit has not yet arrived at: it ends today, and is to be recorded as a partial success,
     * which pays nothing more. The player is told why.
     */
    private static void cancelBeforeArrival(Campaign campaign, AbstractContract contract, LocalDate today) {
        contract.endContractEarly(today, Money.zero());
        // The contract has not started, so it is also started today, leaving it active - and so completable - today.
        // Its length is kept so length-based calculations are unaffected, as with any early finish.
        contract.setScheduleData(new ContractScheduleData(today, today, contract.getLengthInMonths()));
        contract.setMandatedCompletionStatus(MissionStatus.PARTIAL);

        String planetName = getPlanetName(contract, today);
        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "cancelled.report",
              contract.getHyperlinkedName(), planetName));
        new ImmersiveDialogNotification(campaign, getFormattedTextAt(RESOURCE_BUNDLE, "cancelled.notice",
              contract.getName(), planetName, contract.getEmployerDisplayName()), true);
    }

    /**
     * Regenerates the contract as a Guerrilla Warfare contract against the planet's new owner, starting today.
     *
     * <p>Its terms are rolled afresh for a Guerrilla operation, with the employer unable to transport the unit and
     * command rights set to Liaison (unless the unit already had Independent command). Its length, scenario schedule,
     * and pay are determined as for a new contract. Enemy morale is set to Advancing, as the enemy has just won the
     * planet. The contract becomes a Career Maker, doubling the reputation gained on success.</p>
     *
     * <p>The StratCon map is kept, with every allied facility falling to the enemy, and the Guerrilla objectives are
     * laid over it.</p>
     */
    private static void leadResistance(Campaign campaign, AbstractContract contract, LocalDate today) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        ContractCommandRights previousCommandRights = contract.getCommandRights();
        ChaosContractStepsTable previousCommandRightsStep = contract.getCommandRightsStep();

        // The enemy is now whoever took the planet
        updateEnemyToNewOwner(campaign, contract, today);

        // Objective - the new owner now holds the planet the unit is fighting over
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.GUERRILLA_WARFARE,
              ChaosObjectiveType.GARRISON.getCamOpsObjectiveType()));

        // Terms
        ContractTermsData terms = ChaosContractDeterminationTerms.determineInitialTerms(
              ChaosObjectiveType.GUERILLA_OPERATION,
              contract.getEmployerType(),
              contract.getEmployerFaction(),
              campaignOptions.get(CampaignOption.USE_CONTRACT_FACTION_MODIFIERS));
        ChaosContractStepsTable commandRightsStep = (previousCommandRights == ContractCommandRights.INDEPENDENT) ?
                                                          previousCommandRightsStep :
                                                          getLowestStepWithCommandRights(ContractCommandRights.LIAISON);
        // The employer has no way to move the unit off a planet it no longer holds; the lowest step carries no
        // transport.
        contract.setContractTerms(terms.withCommandRights(commandRightsStep)
                                        .withTransport(ChaosContractStepsTable.STEP_ONE));

        // Schedule
        int lengthInMonths = AbstractContractGeneration.determineLength(campaign, contract);
        contract.setScheduleData(new ContractScheduleData(today, today.plusMonths(lengthInMonths), lengthInMonths));
        contract.setTrackCount(AbstractContractGeneration.determineTrackCount(contract));
        contract.setScenarioSchedule(AbstractContractGeneration.rollScenarioSchedule(campaign, contract,
              lengthInMonths));

        // Pay (after the terms and schedule, which it depends on)
        contract.setContractFinanceData(new ContractFinanceData(Money.zero(),
              AbstractContractGeneration.determineMonthlyPay(campaign, contract),
              AbstractContractGeneration.determineCombatPay(campaign, contract)));

        // State
        contract.changeMorale(ContractMoraleLevel.ADVANCING);
        contract.setConsecutiveTrackResultTally(0);
        contract.setMandatedCompletionStatus(null);

        // Staying behind to lead the resistance makes the unit's name: Career Maker replaces whatever unit-reputation
        // characteristic the contract carried, as a contract carries at most one per category.
        List<ContractCharacteristic> characteristics = new ArrayList<>();
        for (ContractCharacteristic characteristic : contract.getCharacteristics()) {
            if (characteristic.getCategory() != ContractCharacteristic.Category.UNIT_REPUTATION) {
                characteristics.add(characteristic);
            }
        }
        characteristics.add(ContractCharacteristic.CAREER_MAKER);
        contract.setCharacteristics(characteristics);

        // StratCon - the enemy takes every allied facility, then the Guerrilla objectives are laid over the map
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState != null) {
            for (StratConTrackState track : campaignState.getTracks()) {
                for (StratConFacility facility : track.getFacilities().values()) {
                    if (facility.isOwnerAlliedToPlayer()) {
                        switchFacilityOwner(facility);
                    }
                }
            }

            StratConContractDefinition definition = StratConContractDefinition.getContractDefinition(
                  contract.getObjectiveType());
            if (definition != null) {
                StratConContractInitializer.reseedForNewObjective(contract, campaign, definition);
            }
        }

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "resistance.report",
              contract.getHyperlinkedName(), getPlanetName(contract, today), contract.getEnemyDisplayName()));

        // The contract is now a type the player may not have been briefed on
        ContractSpecialMechanicsNagDialog.showIfDue(campaign, contract);
        StratConFacilityBriefingNagDialog.showIfDue(campaign, contract);
    }

    /**
     * Makes the planet's new owner the contract's enemy. The current enemy is kept if it is among the new owners, or if
     * no new owner is known; otherwise the first-listed new owner becomes the enemy, fielding forces of the same skill
     * and equipment as the enemy it replaces.
     */
    private static void updateEnemyToNewOwner(Campaign campaign, AbstractContract contract, LocalDate today) {
        List<String> ownerCodes = getOwnerCodes(contract, today);
        if (ownerCodes.isEmpty()) {
            return;
        }

        EnemyData currentEnemy = contract.getEnemyData();
        for (String ownerCode : ownerCodes) {
            if (ownerCode.equals(currentEnemy.factionCode()) || ownerCode.equals(currentEnemy.sponsorFactionCode())) {
                return;
            }
        }

        Faction newOwner = Factions.getInstance().getFaction(ownerCodes.get(0));
        if (newOwner == null) {
            return;
        }

        EnemyData newEnemy = ChaosContractDeterminationEnemy.generateEnemyForFaction(campaign, newOwner, today);
        contract.setEnemyData(new EnemyData(newEnemy, currentEnemy.forceSkill(), currentEnemy.equipmentRating()));
    }

    /**
     * @return the lowest contract step granting the given command rights
     */
    private static ChaosContractStepsTable getLowestStepWithCommandRights(ContractCommandRights commandRights) {
        for (ChaosContractStepsTable step : ChaosContractStepsTable.values()) {
            if (step.getContractCommandRights() == commandRights) {
                return step;
            }
        }
        throw new IllegalStateException("No contract step grants " + commandRights);
    }

    /**
     * @return the name of the contract's planet (or, when the planet is unknown, its system) on the given day
     */
    public static String getPlanetName(AbstractContract contract, LocalDate date) {
        String planetName = contract.getTargetPlanetName(date);
        return (planetName == null) ? contract.getTargetSystemName(date) : planetName;
    }

    /**
     * @return the display name of the first faction holding the contract's planet on the given day, or {@code null}
     *       when there is none
     */
    public static @Nullable String getNewOwnerName(AbstractContract contract, LocalDate date) {
        for (String ownerCode : getOwnerCodes(contract, date)) {
            Faction owner = Factions.getInstance().getFaction(ownerCode);
            if (owner != null) {
                return owner.getFullName(date.getYear());
            }
        }
        return null;
    }
}
