/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.campaign.force.Force.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.performResupply;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.processAbandonedConvoy;
import static mekhq.campaign.personnel.Bloodmark.getBloodhuntSchedule;
import static mekhq.campaign.personnel.DiscretionarySpending.performDiscretionarySpending;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.education.EducationController.getAcademy;
import static mekhq.campaign.personnel.education.TrainingCombatTeams.processTrainingCombatTeams;
import static mekhq.campaign.personnel.enums.BloodmarkLevel.BLOODMARK_ZERO;
import static mekhq.campaign.personnel.lifeEvents.CommandersDayAnnouncement.isCommandersDay;
import static mekhq.campaign.personnel.lifeEvents.FreedomDayAnnouncement.isFreedomDay;
import static mekhq.campaign.personnel.lifeEvents.NewYearsDayAnnouncement.isNewYear;
import static mekhq.campaign.personnel.lifeEvents.WinterHolidayAnnouncement.isWinterHolidayMajorDay;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.SECONDARY_POWER_SUPPLY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllNewCures;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllSystemSpecificDiseasesWithCures;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getNewBioweaponAttack;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getNewDiseaseOutbreaks;
import static mekhq.campaign.personnel.skills.Aging.applyAgingSPA;
import static mekhq.campaign.personnel.skills.Aging.getMilestone;
import static mekhq.campaign.personnel.skills.AttributeCheckUtility.performQuickAttributeCheck;
import static mekhq.campaign.personnel.skills.SkillModifierData.IGNORE_AGE;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.areFieldKitchensWithinCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenUsage;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.processFatigueRecovery;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.RETIREMENT_AGE;
import static mekhq.campaign.randomEvents.GrayMonday.GRAY_MONDAY_EVENTS_BEGIN;
import static mekhq.campaign.randomEvents.GrayMonday.GRAY_MONDAY_EVENTS_END;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BONDSMAN;
import static mekhq.campaign.stratCon.StratConRulesManager.processIgnoredDynamicScenario;
import static mekhq.campaign.stratCon.SupportPointNegotiation.negotiateAdditionalSupportPoints;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import javax.swing.JOptionPane;

import megamek.codeUtilities.StringUtility;
import megamek.common.loaders.MekSummary;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.DayEndingEvent;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PartsInUseManager;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Bloodmark;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.RandomDependents;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.BloodmarkLevel;
import mekhq.campaign.personnel.enums.ExtraIncome;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.lifeEvents.ComingOfAgeAnnouncement;
import mekhq.campaign.personnel.lifeEvents.CommandersDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.FreedomDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.NewYearsDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.WinterHolidayAnnouncement;
import mekhq.campaign.personnel.medical.MASHCapacity;
import mekhq.campaign.personnel.medical.MedicalController;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateImplants;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;
import mekhq.campaign.personnel.skills.EscapeSkills;
import mekhq.campaign.personnel.skills.QuickTrain;
import mekhq.campaign.personnel.skills.enums.AgingMilestone;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.randomEvents.GrayMonday;
import mekhq.campaign.randomEvents.RiotScenario;
import mekhq.campaign.randomEvents.prisoners.PrisonerEventManager;
import mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionHints.WarAndPeaceProcessor;
import mekhq.campaign.universe.factionStanding.FactionAccoladeEvent;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.campaign.universe.factionStanding.FactionCensureEvent;
import mekhq.campaign.universe.factionStanding.FactionCensureLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatum;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.PerformBatchall;
import mekhq.campaign.utilities.AutomatedPersonnelCleanUp;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.service.mrms.MRMSService;
import mekhq.utilities.ReportingUtilities;

/**
 * Handles daily updates, processing, and management related to campaign progression.
 *
 * <p>The {@code CampaignNewDayManager} class orchestrates day-to-day campaign activities and ensures necessary updates
 * and checks are performed for all entities and features tied to the campaign state.</p>
 *
 * <p><b>Note:</b> prior to 0.50.10 all the code in this class lived in {@link Campaign}. It was moved as part of an
 * effort to reduce code bloat in that class.</p>
 *
 * @since 0.50.10
 */
public class CampaignNewDayManager {
    private static final MMLogger LOGGER = MMLogger.create(CampaignNewDayManager.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    // Deprecated since 0.50.10, for removal false
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
          MekHQ.getMHQOptions().getLocale());

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final Faction faction;
    private final Hangar hangar;
    private final Warehouse warehouse;
    private final Quartermaster quartermaster;
    private final Finances finances;
    private LocalDate today;
    private CurrentLocation updatedLocation;

    public CampaignNewDayManager(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.faction = campaign.getFaction();
        this.hangar = campaign.getHangar();
        this.warehouse = campaign.getWarehouse();
        this.quartermaster = campaign.getQuartermaster();
        this.finances = campaign.getFinances();
    }

    /**
     * @return <code>true</code> if the new day arrived
     */
    public boolean newDay() {
        // clear previous retirement information
        campaign.getTurnoverRetirementInformation().clear();

        // Refill Automated Pools, if the options are selected
        if (MekHQ.getMHQOptions().getNewDayAsTechPoolFill()) {
            campaign.resetAsTechPool();
        }

        if (MekHQ.getMHQOptions().getNewDayMedicPoolFill()) {
            campaign.resetMedicPool();
        }

        // Ensure we don't have anything that would prevent the new day
        if (MekHQ.triggerEvent(new DayEndingEvent(campaign))) {
            return false;
        }

        // Autosave based on the previous day's information
        campaign.getAutosaveService().requestDayAdvanceAutosave(campaign);

        // Advance the day by one
        final LocalDate yesterday = campaign.getLocalDate();
        today = yesterday.plusDays(1);
        campaign.setLocalDate(today);
        boolean isMonday = today.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isSunday = today.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isFirstOfMonth = today.getDayOfMonth() == 1;
        boolean isNewYear = today.getDayOfYear() == 1;

        // Check for important dates
        if (campaignOptions.isShowLifeEventDialogCelebrations()) {
            fetchCelebrationDialogs();
        }

        // Determine if we have an active contract or not, as campaign can get used
        // elsewhere before we actually hit the AtB new day (e.g., personnel market)
        if (campaignOptions.isUseAtB()) {
            campaign.setHasActiveContract();
        }

        // Clear Reports
        campaign.getCurrentReport().clear();
        campaign.setCurrentReportHTML("");
        campaign.getNewReports().clear();

        campaign.getSkillReport().clear();
        campaign.setSkillReportHTML("");
        campaign.getNewSkillReports().clear();

        campaign.getBattleReport().clear();
        campaign.setBattleReportHTML("");
        campaign.getNewBattleReports().clear();

        campaign.getPoliticsReport().clear();
        campaign.setPoliticsReportHTML("");
        campaign.getNewPoliticsReports().clear();

        campaign.getPersonnelReport().clear();
        campaign.setPersonnelReportHTML("");
        campaign.getNewPersonnelReports().clear();

        campaign.getMedicalReport().clear();
        campaign.setMedicalReportHTML("");
        campaign.getNewMedicalReports().clear();

        campaign.getFinancesReport().clear();
        campaign.setFinancesReportHTML("");
        campaign.getNewFinancesReports().clear();

        campaign.getAcquisitionsReport().clear();
        campaign.setAcquisitionsReportHTML("");
        campaign.getNewAcquisitionsReports().clear();

        campaign.getTechnicalReport().clear();
        campaign.setTechnicalReportHTML("");
        campaign.getNewTechnicalReports().clear();

        campaign.beginReport("<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(today) + "</b>");

        campaign.getPersonnelWhoAdvancedInXP().clear();

        // New Year Changes
        if (isNewYear) {
            // News is reloaded
            campaign.reloadNews();

            // Change Year Game Option
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(today.getYear());

            // Degrade Regard
            List<String> degradedRegardReports =
                  campaign.getFactionStandings().processRegardDegradation(faction.getShortName(),
                        today.getYear(), campaignOptions.getRegardMultiplier());
            for (String report : degradedRegardReports) {
                campaign.addReport(GENERAL, report);
            }
        }

        campaign.readNews();

        campaign.getLocation().newDay(campaign);
        updatedLocation = campaign.getLocation();

        updateFacilities();

        processNewDayPersonnel();

        if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
            PlanetarySystem currentSystem = updatedLocation.getCurrentSystem();
            String currentSystemName = currentSystem.getName(today);
            String currentSystemId = currentSystem.getId();

            checkForBioweaponAttacksOrNewVaccines(currentSystemName, currentSystemId);
            checkForDiseaseOutbreaks(currentSystemName, currentSystemId);
            checkForNewVaccines(currentSystemId);
        }

        if (isMonday) {
            Fatigue.processDeploymentFatigueResponses(campaign);

            if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
                Inoculations.performDiseaseChecks(campaign);
            }
        }

        // Manage the Markets
        campaign.refreshPersonnelMarkets(false);

        // TODO : AbstractContractMarket : Uncomment
        // getContractMarket().processNewDay(campaign);
        campaign.getUnitMarket().processNewDay(campaign);

        // campaign needs to be after both personnel and markets
        if (campaignOptions.isAllowMonthlyConnections() && isFirstOfMonth) {
            checkForBurnedContacts();
        }

        // Needs to be before 'processNewDayATB' so that Dependents can't leave the
        // moment they arrive via AtB Bonus Events
        if (updatedLocation.isOnPlanet() && isFirstOfMonth) {
            RandomDependents randomDependents = new RandomDependents(campaign);
            randomDependents.processMonthlyRemovalAndAddition();
        }

        // Process New Day for AtB
        if (campaignOptions.isUseAtB()) {
            processNewDayATB();
        }

        processReputationChanges();

        if (campaignOptions.isUseEducationModule()) {
            processEducationNewDay();
        }

        if (campaignOptions.isEnableAutoAwards() && isFirstOfMonth) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        // Prisoner events can occur on Monday or the 1st of the month depending on the
        // type of event
        if (isMonday || isFirstOfMonth) {
            new PrisonerEventManager(campaign);
        }

        if (isFirstOfMonth) {
            payForRentedFacilities();
        }

        if (isMonday) {
            // Bays are handled weekly, all other facilities are handled monthly
            FacilityRentals.payForAllRentedBays(campaign);
        }

        campaign.resetAsTechMinutes();

        processNewDayUnits();

        processNewDayForces();

        if (campaign.isProcessProcurement()) {
            campaign.setShoppingList(campaign.goShopping(campaign.getShoppingList()));
        }

        // check for anything in finances
        finances.newDay(campaign, yesterday, today);

        // process removal of old personnel data on the first day of each month
        if (campaignOptions.isUsePersonnelRemoval() && isFirstOfMonth) {
            performPersonnelCleanUp();
        }

        // campaign duplicates any turnover information so that it is still available on the
        // new day. otherwise, it's only available if the user inspects history records
        for (String entry : campaign.getTurnoverRetirementInformation()) {
            campaign.addReport(PERSONNEL, entry);
        }

        if (campaign.getTopUpWeekly() && isMonday) {
            PartsInUseManager partsInUseManager = new PartsInUseManager(campaign);
            Set<PartInUse> actualPartsInUse = partsInUseManager.getPartsInUse(campaign.getIgnoreMothballed(),
                  false,
                  campaign.getIgnoreSparesUnderQuality());
            int bought = partsInUseManager.stockUpPartsInUse(actualPartsInUse);
            campaign.addReport(ACQUISITIONS, String.format(resources.getString("weeklyStockCheck.text"), bought));
        }

        // Random Events
        if (today.isAfter(GRAY_MONDAY_EVENTS_BEGIN) && today.isBefore(GRAY_MONDAY_EVENTS_END)) {
            new GrayMonday(campaign, today);
        }

        // Faction Standing
        performFactionStandingChecks(isFirstOfMonth, isNewYear);

        // War & Peace Notifications
        new WarAndPeaceProcessor(campaign, false);

        // campaign must be the last step before returning true
        MekHQ.triggerEvent(new NewDayEvent(campaign));
        return true;
    }

    private void checkForBioweaponAttacksOrNewVaccines(String systemName, String systemId) {
        InjuryType newBioweaponAttack = getNewBioweaponAttack(systemId, today, false);
        if (newBioweaponAttack != null) {
            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorMedicalPerson(),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "bioweaponAttack.inCharacter",
                        campaign.getCommanderAddress()),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "bioweaponAttack.outOfCharacter",
                        newBioweaponAttack.getSimpleName(), systemName),
                  null,
                  false,
                  ImmersiveDialogWidth.LARGE);
        }
    }

    private void checkForDiseaseOutbreaks(String systemName, String systemId) {
        Set<InjuryType> newOutbreaks = getNewDiseaseOutbreaks(systemId, today, false);
        Set<InjuryType> availableCures = getAllSystemSpecificDiseasesWithCures(systemId, today, false);
        for (InjuryType disease : newOutbreaks) {
            String keySuffix = availableCures.contains(disease) ? "yesCure" : "noCure";
            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorMedicalPerson(),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "diseaseOutbreak.inCharacter." + keySuffix,
                        campaign.getCommanderAddress()),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "diseaseOutbreak.outOfCharacter" + keySuffix,
                        disease.getSimpleName(), systemName),
                  null,
                  false,
                  ImmersiveDialogWidth.LARGE);
        }
    }

    private void checkForNewVaccines(String systemId) {
        Set<InjuryType> newCures = getAllNewCures(systemId, today);
        for (InjuryType injuryType : newCures) {
            new ImmersiveDialogNotification(campaign, getFormattedTextAt(RESOURCE_BUNDLE, "disease.newCure",
                  injuryType.getSimpleName()), true);
        }
    }

    /**
     * Gets all scenario IDs that have at least one standard force assigned to them.
     *
     * <p>This method iterates through all forces in the campaign and collects the scenario IDs of those forces that
     * are classified as standard force types and are currently assigned to a scenario.</p>
     *
     * @return a set of scenario IDs that have standard forces assigned, or an empty set if no standard forces are
     *       deployed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Set<Integer> getAllScenariosWithAssignedStandardForces() {
        Set<Integer> scenarios = new HashSet<>();

        for (Force force : campaign.getAllForces()) {
            if (force.getForceType().isStandard()) {
                int scenarioId = force.getScenarioId();
                if (scenarioId != NO_ASSIGNED_SCENARIO) {
                    scenarios.add(scenarioId);
                }
            }
        }

        return scenarios;
    }

    /**
     * Updates the campaign's facility capacities and validates against operational limits.
     *
     * <p>This method performs the following facility updates:</p>
     * <ol>
     *     <li>Recalculates the field kitchen capacity based on current campaign state</li>
     *     <li>Validates MASH (Mobile Army Surgical Hospital) capacity against configured theater limits,
     *         considering only units assigned to the force's table of organization</li>
     * </ol>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateFacilities() {
        updateFieldKitchenCapacity();
        updateMASHTheatreCapacity();
    }

    /**
     * Fetches and handles the celebration dialogs specific to the current day.
     *
     * <p><b>Note:</b> Commanders day is handled as a part of the personnel processing, so we don't need to parse
     * personnel twice.</p>
     */
    private void fetchCelebrationDialogs() {
        if (!faction.isClan()) {
            if (isWinterHolidayMajorDay(today)) {
                new WinterHolidayAnnouncement(campaign);
            }

            if (isFreedomDay(today)) {
                new FreedomDayAnnouncement(campaign);
            }
        }

        if (isNewYear(today)) {
            new NewYearsDayAnnouncement(campaign);
        }
    }

    /**
     * Updates the status of whether field kitchens are operating within their required capacity.
     *
     * <p>If fatigue is enabled in the campaign options, campaign method calculates the total available
     * field kitchen capacity and the required field kitchen usage, then updates the {@code fieldKitchenWithinCapacity}
     * flag to reflect whether the capacity meets the demand. If fatigue is disabled, the capacity is automatically set
     * to {@code false}.</p>
     */
    private void updateFieldKitchenCapacity() {
        if (campaignOptions.isUseFatigue()) {
            int fieldKitchenCapacity =
                  checkFieldKitchenCapacity(campaign.getForce(FORCE_ORIGIN).getAllUnitsAsUnits(hangar,
                        false), campaignOptions.getFieldKitchenCapacity());
            int fieldKitchenUsage = checkFieldKitchenUsage(campaign.getActivePersonnel(false, false),
                  campaignOptions.isUseFieldKitchenIgnoreNonCombatants());
            boolean withinCapacity = !campaign.isOnContractAndPlanetside() ||
                                           areFieldKitchensWithinCapacity(fieldKitchenCapacity, fieldKitchenUsage);
            campaign.setFieldKitchenWithinCapacity(withinCapacity);
        } else {
            campaign.setFieldKitchenWithinCapacity(false);
        }
    }

    /**
     * Processes the daily activities and updates for all personnel that haven't already left the campaign.
     * <p>
     * campaign method iterates through all personnel and performs various daily updates, including health checks,
     * status updates, relationship events, and other daily or periodic tasks.
     * <p>
     * The following tasks are performed for each person:
     * <ul>
     * <li><b>Death Handling:</b> If the person has died, their processing is
     * skipped for the day.</li>
     * <li><b>Relationship Events:</b> Processes relationship-related events, such
     * as marriage or divorce.</li>
     * <li><b>Reset Actions:</b> Resets the person's minutes left for work and sets
     * acquisitions made to 0.</li>
     * <li><b>Medical Events:</b></li>
     * <li>- If advanced medical care is available, processes the person's daily
     * healing.</li>
     * <li>- If advanced medical care is unavailable, decreases the healing wait
     * time and
     * applies natural or doctor-assisted healing.</li>
     * <li><b>Weekly Edge Resets:</b> Resets edge points to their purchased value
     * weekly (applies
     * to support personnel).</li>
     * <li><b>Vocational XP:</b> Awards monthly vocational experience points to the
     * person where
     * applicable.</li>
     * <li><b>Anniversaries:</b> Checks for birthdays or significant anniversaries
     * and announces
     * them as needed.</li>
     * <li><b>autoAwards:</b> On the first day of every month, calculates and awards
     * support
     * points based on roles and experience levels.</li>
     * </ul>
     * <p>
     * <b>Concurrency Note:</b>
     * A separate filtered list of personnel is used to avoid concurrent
     * modification issues during iteration.
     * <p>
     * campaign method relies on several helper methods to perform specific tasks for
     * each person,
     * separating the responsibilities for modularity and readability.
     *
     * @see Campaign#getPersonnelFilteringOutDeparted() Filters out departed personnel before daily processing
     */
    public void processNewDayPersonnel() {
        RecoverMIAPersonnel recovery = new RecoverMIAPersonnel(campaign, faction, campaign.getAtBUnitRatingMod());
        MedicalController medicalController = new MedicalController(campaign);

        // campaign list ensures we don't hit a concurrent modification error
        List<Person> personnel = campaign.getPersonnelFilteringOutDeparted();

        // Prep some data for vocational xp
        int vocationalXpRate = campaignOptions.getVocationalXP();
        if (campaign.hasActiveContract()) {
            if (campaignOptions.isUseAtB()) {
                for (AtBContract contract : campaign.getActiveAtBContracts()) {
                    if (!contract.getContractType().isGarrisonType()) {
                        vocationalXpRate *= 2;
                        break;
                    }
                }
            } else {
                vocationalXpRate *= 2;
            }
        }

        // Process personnel
        int peopleWhoCelebrateCommandersDay = 0;
        int commanderDayTargetNumber = 5;
        boolean isCommandersDay = isCommandersDay(today) &&
                                        campaign.getCommander() != null &&
                                        campaignOptions.isShowLifeEventDialogCelebrations();
        boolean isCampaignPlanetside = updatedLocation.isOnPlanet();
        boolean isUseAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        boolean isUseAltAdvancedMedical = campaignOptions.isUseAlternativeAdvancedMedical();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();
        boolean useBetterMonthlyIncome = campaignOptions.isUseBetterExtraIncome();
        boolean isUseAgeEffects = campaignOptions.isUseAgeEffects();
        for (Person person : personnel) {
            if (person.getStatus().isDepartedUnit()) {
                continue;
            }

            int age = person.getAge(today);
            person.setAgeForAttributeModifiers(isUseAgeEffects ? age : IGNORE_AGE);

            PersonnelOptions personnelOptions = person.getOptions();

            // Daily events
            if (person.getStatus().isMIA()) {
                recovery.attemptRescueOfPlayerCharacter(person);
            }

            if (person.getPrisonerStatus().isBecomingBondsman()) {
                // We use 'isAfter' to avoid situations where we somehow manage to miss the
                // date.
                // campaign shouldn't be necessary, but a safety net never hurt
                if (today.isAfter(person.getBecomingBondsmanEndDate().minusDays(1))) {
                    person.setPrisonerStatus(campaign, BONDSMAN, true);
                    campaign.addReport(PERSONNEL, String.format(resources.getString("becomeBondsman.text"),
                          person.getHyperlinkedName(),
                          spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                          CLOSING_SPAN_TAG));
                }
            }

            person.resetMinutesLeft(campaignOptions.isTechsUseAdministration());
            person.setAcquisition(0);

            medicalController.processMedicalEvents(person,
                  campaignOptions.isUseAgeEffects(),
                  campaign.isClanCampaign(),
                  today);

            processAnniversaries(person);

            person.checkForIlliterateRemoval();

            AdvancedMedicalAlternateImplants.checkForDermalEligibility(person);

            // Weekly events
            if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
                if (!campaign.getRandomDeath().processNewWeek(campaign, today, person)) {
                    // If the character has died, we don't need to process relationship events
                    processWeeklyRelationshipEvents(person);
                }

                person.resetCurrentEdge();

                if (!person.getStatus().isMIA()) {
                    boolean isWithinCapacity = !campaign.isOnContractAndPlanetside() ||
                                                     campaign.getFieldKitchenWithinCapacity();
                    processFatigueRecovery(campaign, person, isWithinCapacity);
                }

                processCompulsionsAndMadness(person, personnelOptions, isUseAdvancedMedical, isUseAltAdvancedMedical,
                      isUseFatigue, fatigueRate);
            }

            // Monthly events
            if (today.getDayOfMonth() == 1) {
                processMonthlyAutoAwards(person);

                if (vocationalXpRate > 0) {
                    if (processMonthlyVocationalXp(person, vocationalXpRate)) {
                        campaign.getPersonnelWhoAdvancedInXP().add(person);
                    }
                }

                if (person.isCommander() &&
                          campaignOptions.isAllowMonthlyReinvestment() &&
                          !person.isHasPerformedExtremeExpenditure()) {
                    String reportString = performDiscretionarySpending(person, finances, today);
                    if (reportString != null) {
                        campaign.addReport(FINANCES, reportString);
                    } else {
                        LOGGER.error("Unable to process discretionary spending for {}", person.getFullTitle());
                    }
                }

                String extraIncomeReport = ExtraIncome.processExtraIncome(finances, person, today,
                      useBetterMonthlyIncome);
                if (!StringUtility.isNullOrBlank(extraIncomeReport)) {
                    campaign.addReport(FINANCES, extraIncomeReport);
                }

                person.setHasPerformedExtremeExpenditure(false);

                int bloodmarkLevel = person.getBloodmark();
                if (bloodmarkLevel > BLOODMARK_ZERO.getLevel()) {
                    BloodmarkLevel bloodmark = BloodmarkLevel.parseBloodmarkLevelFromInt(bloodmarkLevel);
                    boolean hasAlternativeID = person.getOptions().booleanOption(ATOW_ALTERNATE_ID);
                    List<LocalDate> bloodmarkSchedule = getBloodhuntSchedule(bloodmark, today, hasAlternativeID);
                    for (LocalDate assassinationAttempt : bloodmarkSchedule) {
                        person.addBloodhuntDate(assassinationAttempt);
                    }
                }

                if (today.getMonthValue() % 3 == 0) {
                    if (person.hasDarkSecret()) {
                        String darkSecretReport = person.isDarkSecretRevealed(true, false);
                        if (!StringUtility.isNullOrBlank(darkSecretReport)) {
                            campaign.addReport(PERSONNEL, darkSecretReport);
                        }
                    }
                }

                if (person.getBurnedConnectionsEndDate() != null) {
                    person.checkForConnectionsReestablishContact(today);
                }

                if (campaignOptions.isAllowMonthlyConnections()) {
                    String connectionsReport = person.performConnectionsWealthCheck(today, finances);
                    if (!StringUtility.isNullOrBlank(connectionsReport)) {
                        campaign.addReport(PERSONNEL, connectionsReport);
                    }
                }

                if (campaignOptions.isUseFunctionalEscapeArtist() && person.getStatus().isPoW()) {
                    EscapeSkills.performEscapeAttemptCheck(campaign, person);
                }
            }

            if (today.getDayOfYear() == 1 && campaignOptions.isUseAlternativeAdvancedMedical()) {
                AdvancedMedicalAlternateImplants.performEnhancedImagingDegradationCheck(campaign, person);
            }

            if (isCommandersDay && !faction.isClan() && (peopleWhoCelebrateCommandersDay < commanderDayTargetNumber)) {
                if (age >= 6 && age <= 12) {
                    peopleWhoCelebrateCommandersDay++;
                }
            }

            List<LocalDate> scheduledBloodHunts = person.getBloodhuntSchedule();
            if (!scheduledBloodHunts.isEmpty()) {
                boolean isDayOfBloodHunt = Bloodmark.checkForAssassinationAttempt(person,
                      today,
                      isCampaignPlanetside);

                if (isDayOfBloodHunt) {
                    Bloodmark.performAssassinationAttempt(campaign, person, today);
                }
            }
        }

        if (!campaign.getPersonnelWhoAdvancedInXP().isEmpty()) {
            campaign.addReport(GENERAL, String.format(resources.getString("gainedExperience.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                  campaign.getPersonnelWhoAdvancedInXP().size(),
                  CLOSING_SPAN_TAG));
        }

        // Commander's Day!
        if (isCommandersDay && (peopleWhoCelebrateCommandersDay >= commanderDayTargetNumber)) {
            new CommandersDayAnnouncement(campaign);
        }

        if (MekHQ.getMHQOptions().getNewDayOptimizeMedicalAssignments()) {
            new OptimizeInfirmaryAssignments(campaign);
        }

        if (MekHQ.getMHQOptions().getNewMonthQuickTrain()) {
            final int newMonthQuickTrainTargetLevel = 5;
            QuickTrain.processQuickTraining(personnel, newMonthQuickTrainTargetLevel, campaign, true);
        }
    }


    /**
     * Checks if the commander has any burned contacts, and if so, generates and records a report.
     *
     * <p>campaign method is only executed if monthly connections are allowed by campaign options. If the commander
     * exists and their burned connections end date has not been set, it invokes the commander's check for burned
     * contacts on the current day. If a non-blank report is returned, the report is added to the campaign logs.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void checkForBurnedContacts() {
        if (campaignOptions.isAllowMonthlyConnections()) {
            Person commander = campaign.getCommander();
            if (commander != null && commander.getBurnedConnectionsEndDate() == null) {
                String report = commander.checkForBurnedContacts(today);
                if (!report.isBlank()) {
                    campaign.addReport(PERSONNEL, report);
                }
            }
        }
    }

    /**
     * Processes the new day actions for various AtB systems
     * <p>
     * It generates contract offers in the contract market, updates ship search expiration and results, processes ship
     * search on Mondays, awards training experience to eligible training lances on active contracts on Mondays, adds or
     * removes dependents at the start of the year if the options are enabled, rolls for morale at the start of the
     * month, and processes ATB scenarios.
     */
    private void processNewDayATB() {
        campaign.getContractMarket().generateContractOffers(campaign);

        if ((campaign.getShipSearchExpiration() != null) && !campaign.getShipSearchExpiration().isAfter(today)) {
            campaign.setShipSearchExpiration(null);
            if (campaign.getShipSearchResult() != null) {
                campaign.addReport(ACQUISITIONS, "Opportunity for purchase of " + campaign.getShipSearchResult() + " " +
                                                       "has expired.");
                campaign.setShipSearchResult(null);
            }
        }

        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            processShipSearch();
            processTrainingCombatTeams(campaign);
        }

        if (today.getDayOfMonth() == 1) {
            /*
             * First of the month; roll Morale.
             */
            for (AtBContract contract : campaign.getActiveAtBContracts()) {
                AtBMoraleLevel oldMorale = contract.getMoraleLevel();

                contract.checkMorale(campaign, today);
                AtBMoraleLevel newMorale = contract.getMoraleLevel();

                String report = "";
                if (contract.isPeaceful()) {
                    report = resources.getString("garrisonDutyRouted.text");
                } else if (oldMorale != newMorale) {
                    report = String.format(resources.getString("contractMoraleReport.text"),
                          newMorale,
                          contract.getHyperlinkedName(),
                          newMorale.getToolTipText());
                }

                if (!report.isBlank()) {
                    campaign.addReport(GENERAL, report);
                }
            }
        }

        // Resupply
        if (today.getDayOfMonth() == 2) {
            // campaign occurs at the end of the 1st day, each month to avoid an awkward mechanics interaction where
            // personnel might quit or get taken out of fatigue without the player having any opportunity to
            // intervene before their resupply attempt becomes active.
            List<AtBContract> activeContracts = campaign.getActiveAtBContracts();
            AtBContract firstNonSubcontract = null;
            for (AtBContract contract : activeContracts) {
                if (!contract.isSubcontract()) {
                    firstNonSubcontract = contract;
                    break;
                }
            }

            if (firstNonSubcontract != null) {
                if (campaignOptions.isUseStratCon()) {
                    boolean inLocation = updatedLocation.isOnPlanet() &&
                                               updatedLocation.getCurrentSystem()
                                                     .equals(firstNonSubcontract.getSystem());

                    if (inLocation) {
                        processResupply(firstNonSubcontract);
                    }
                }
            }
        }

        int weekOfYear = today.get(Campaign.WEEK_FIELDS.weekOfYear());
        boolean isOddWeek = (weekOfYear % 2 == 1);
        boolean isMonday = today.getDayOfWeek() == DayOfWeek.MONDAY;
        if (campaignOptions.isUseStratCon() && isMonday && isOddWeek) {
            negotiateAdditionalSupportPoints(campaign);
        }

        processNewDayATBScenarios();

        // Daily events
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (campaignOptions.isUseGenericBattleValue() &&
                      !contract.getContractType().isGarrisonType() &&
                      contract.getStartDate().equals(today)) {
                // Batchalls
                Faction enemyFaction = contract.getEnemy();
                String enemyFactionCode = contract.getEnemyCode();

                boolean allowBatchalls = true;
                if (campaignOptions.isUseFactionStandingBatchallRestrictionsSafe()) {
                    double regard = campaign.getFactionStandings().getRegardForFaction(enemyFactionCode, true);
                    allowBatchalls = FactionStandingUtilities.isBatchallAllowed(regard);
                }

                if (enemyFaction.performsBatchalls() && allowBatchalls) {
                    PerformBatchall batchallDialog = new PerformBatchall(campaign,
                          contract.getClanOpponent(),
                          contract.getEnemyCode());

                    boolean batchallAccepted = batchallDialog.isBatchallAccepted();
                    contract.setBatchallAccepted(batchallAccepted);

                    if (!batchallAccepted && campaignOptions.isTrackFactionStanding()) {
                        List<String> reports = campaign.getFactionStandings()
                                                     .processRefusedBatchall(faction.getShortName(),
                                                           enemyFactionCode,
                                                           today.getYear(),
                                                           campaignOptions.getRegardMultiplier());

                        for (String report : reports) {
                            campaign.addReport(GENERAL, report);
                        }
                    }
                }
            }

            if (isMonday && contract.getContractType().isRiotDuty() && contract.getStratconCampaignState() != null) {
                int riotChance = 4;
                if (randomInt(riotChance) == 0) {
                    new RiotScenario(campaign, contract);
                }
            }

            // Early Contract End (StratCon Only)
            StratConCampaignState campaignState = contract.getStratconCampaignState();
            if (campaignState != null && !contract.getEndingDate().equals(today)) {
                boolean isUseMaplessMode = campaignOptions.isUseStratConMaplessMode();
                int victoryPoints = contract.getContractScore(isUseMaplessMode);
                int requiredVictoryPoints = contract.getRequiredVictoryPoints();

                if (campaignState.canEndContractEarly() && victoryPoints >= requiredVictoryPoints) {
                    new ImmersiveDialogNotification(campaign,
                          String.format(resources.getString("stratCon.earlyContractEnd.objectives"),
                                contract.getHyperlinkedName()), true);

                    // This ensures any outstanding payout is paid out before the contract ends
                    LocalDate adjustedDate = today.plusDays(1);
                    int remainingMonths = contract.getMonthsLeft(adjustedDate);
                    Money finalPayout = contract.getMonthlyPayOut().multipliedBy(remainingMonths);
                    contract.setRoutedPayout(finalPayout);
                    contract.setEndDate(adjustedDate);
                }
            }
        }
    }

    /**
     * Processes reputation changes based on various conditions.
     */
    private void processReputationChanges() {
        if (faction.isPirate()) {
            campaign.setDateOfLastCrime(today);
            campaign.setCrimePirateModifier(-100);
        }

        LocalDate dateOfLastCrime = campaign.getDateOfLastCrime();
        int crimePirateModifier = campaign.getCrimePirateModifier();

        if (today.getDayOfMonth() == 1) {
            if (dateOfLastCrime != null) {
                long yearsBetween = ChronoUnit.YEARS.between(today, dateOfLastCrime);

                int remainingCrimeChange = 2;

                if (yearsBetween >= 1) {
                    if (crimePirateModifier < 0) {
                        remainingCrimeChange = max(0, 2 + crimePirateModifier);
                        campaign.changeCrimePirateModifier(2); // campaign is the amount of change specified by CamOps
                    }

                    if (campaign.getRawCrimeRating() < 0 && remainingCrimeChange > 0) {
                        campaign.changeCrimeRating(remainingCrimeChange);
                    }
                }
            }
        }

        if (today.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            campaign.getReputation().initializeReputation(campaign);
        }
    }

    /**
     * campaign method checks if any students in the academy should graduate, and updates their attributes and status
     * accordingly. If any students do graduate, it sends the graduation information to autoAwards.
     */
    private void processEducationNewDay() {
        List<UUID> graduatingPersonnel = new ArrayList<>();
        HashMap<UUID, List<Object>> academyAttributesMap = new HashMap<>();

        for (Person person : campaign.getStudents()) {
            List<Object> individualAcademyAttributes = new ArrayList<>();

            if (EducationController.processNewDay(campaign, person, false)) {
                Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

                if (academy == null) {
                    LOGGER.debug("Found null academy for {} skipping", person.getFullTitle());
                    continue;
                }

                graduatingPersonnel.add(person.getId());

                individualAcademyAttributes.add(academy.getEducationLevel(person));
                individualAcademyAttributes.add(academy.getType());
                individualAcademyAttributes.add(academy.getName());

                academyAttributesMap.put(person.getId(), individualAcademyAttributes);
            }
        }

        if (!graduatingPersonnel.isEmpty()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.PostGraduationController(campaign, graduatingPersonnel, academyAttributesMap);
        }
    }

    public void processNewDayUnits() {
        if (MekHQ.getMHQOptions().getSelfCorrectMaintenance()) {
            Maintenance.checkAndCorrectMaintenanceSchedule(campaign);
        }

        // need to loop through units twice, the first time to do all maintenance and
        // the second time to do whatever else. Otherwise, maintenance minutes might
        // get sucked up by other stuff. campaign is also a good place to ensure that a
        // unit's engineer gets reset and updated.
        for (Unit unit : hangar.getUnits()) {
            // do maintenance checks
            try {
                unit.resetEngineer();
                if (null != unit.getEngineer()) {
                    unit.getEngineer().resetMinutesLeft(campaignOptions.isTechsUseAdministration());
                }

                Maintenance.doMaintenance(campaign, unit);
            } catch (Exception ex) {
                LOGGER.error(ex,
                      "Unable to perform maintenance on {} ({}) due to an error",
                      unit.getName(),
                      unit.getId().toString());
                campaign.addReport(TECHNICAL, String.format("ERROR: An error occurred performing maintenance on %s, " +
                                                                  "check the log",
                      unit.getName()));
            }
        }

        // need to check for assigned tasks in two steps to avoid
        // concurrent modification problems
        List<Part> assignedParts = new ArrayList<>();
        List<Part> arrivedParts = new ArrayList<>();
        warehouse.forEachPart(part -> {
            if (part instanceof Refit) {
                return;
            }

            if (part.getTech() != null) {
                assignedParts.add(part);
            }

            // If the part is currently in-transit...
            if (!part.isPresent()) {
                // ... decrement the number of days until it arrives...
                int newDaysToArrival = part.getDaysToArrival() - 1;

                // If we're in transit and we don't allow deliveries while in transit the part will remain fixed with
                // a delivery time of 1 day until we arrive at our destination.
                if (campaignOptions.isNoDeliveriesInTransit() &&
                          !campaign.getLocation().isOnPlanet() &&
                          newDaysToArrival <= 0) {
                    return;
                }

                part.setDaysToArrival(part.getDaysToArrival() - 1);

                if (part.isPresent()) {
                    // ... and mark the part as arrived if it is now here.
                    arrivedParts.add(part);
                }
            }
        });

        // arrive parts before attempting refit or parts will not get reserved that day
        for (Part part : arrivedParts) {
            quartermaster.arrivePart(part);
        }

        // finish up any overnight assigned tasks
        for (Part part : assignedParts) {
            Person tech;
            if ((part.getUnit() != null) && (part.getUnit().getEngineer() != null)) {
                tech = part.getUnit().getEngineer();
            } else {
                tech = part.getTech();
            }

            if (null != tech) {
                if (null != tech.getSkillForWorkingOn(part)) {
                    try {
                        campaign.fixPart(part, tech);
                    } catch (Exception ex) {
                        LOGGER.error(ex,
                              "Could not perform overnight maintenance on {} ({}) due to an error",
                              part.getName(),
                              part.getId());
                        campaign.addReport(TECHNICAL, String.format(
                              "ERROR: an error occurred performing overnight maintenance on %s, check the log",
                              part.getName()));
                    }
                } else {
                    campaign.addReport(TECHNICAL, String.format(
                          "%s looks at %s, recalls his total lack of skill for working with such technology, then slowly puts the tools down before anybody gets hurt.",
                          tech.getHyperlinkedFullTitle(),
                          part.getName()));
                    part.cancelAssignment(false);
                }
            } else {
                JOptionPane.showMessageDialog(null,
                      "Could not find tech for part: " +
                            part.getName() +
                            " on unit: " +
                            part.getUnit().getHyperlinkedName(),
                      "Invalid Auto-continue",
                      JOptionPane.ERROR_MESSAGE);
            }

            // check to see if campaign part can now be combined with other spare parts
            if (part.isSpare() && (part.getQuantity() > 0)) {
                quartermaster.addPart(part, 0, false);
            }
        }

        // ok now we can check for other stuff we might need to do to units
        int defaultRepairSite = AtBContract.getBestRepairLocation(campaign.getActiveAtBContracts());
        List<UUID> unitsToRemove = new ArrayList<>();
        for (Unit unit : hangar.getUnits()) {
            if (unit.isRefitting()) {
                campaign.refit(unit.getRefit());
            }
            if (unit.isMothballing()) {
                campaign.workOnMothballingOrActivation(unit);
            }
            if (!unit.isPresent()) {
                unit.checkArrival(!campaign.getLocation().isOnPlanet() && campaignOptions.isNoDeliveriesInTransit());

                // Has unit just been delivered?
                if (unit.isPresent()) {
                    campaign.addReport(ACQUISITIONS, String.format(resources.getString("unitArrived.text"),
                          unit.getHyperlinkedName(),
                          spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                          CLOSING_SPAN_TAG));
                    unit.setSite(defaultRepairSite);
                }
            }

            if (!unit.isRepairable() && !unit.hasSalvageableParts()) {
                unitsToRemove.add(unit.getId());
            }
        }
        // Remove any unrepairable, unsalvageable units
        unitsToRemove.forEach(campaign::removeUnit);

        // Finally, run Mass Repair Mass Salvage if desired
        if (MekHQ.getMHQOptions().getNewDayMRMS()) {
            try {
                MRMSService.mrmsAllUnits(campaign);
            } catch (Exception ex) {
                LOGGER.error("Could not perform mass repair/salvage on units due to an error", ex);
                campaign.addReport(TECHNICAL,
                      "ERROR: an error occurred performing mass repair/salvage on units, check the log");
            }
        }
    }

    private void processNewDayForces() {
        // update formation levels
        Force.populateFormationLevelsFromOrigin(campaign);
        recalculateCombatTeams(campaign);

        // Update the force icons based on the end-of-day unit status if desired
        if (MekHQ.getMHQOptions().getNewDayForceIconOperationalStatus()) {
            campaign.getForces().updateForceIconOperationalStatus(campaign);
        }
    }

    /**
     * Performs cleanup of departed personnel by identifying and removing eligible personnel records.
     *
     * <p>campaign method uses the {@link AutomatedPersonnelCleanUp} utility to determine which {@link Person}
     * objects should be removed from the campaign based on current date and campaign configuration options. Identified
     * personnel are then removed, and a report entry is generated if any removals occur.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void performPersonnelCleanUp() {
        AutomatedPersonnelCleanUp removal = new AutomatedPersonnelCleanUp(today,
              campaign.getPersonnel(),
              campaignOptions.isUseRemovalExemptRetirees(),
              campaignOptions.isUseRemovalExemptCemetery());

        List<Person> personnelToRemove = removal.getPersonnelToCleanUp();
        for (Person person : personnelToRemove) {
            campaign.removePerson(person, false);
        }

        if (!personnelToRemove.isEmpty()) {
            campaign.addReport(PERSONNEL, resources.getString("personnelRemoval.text"));
        }
    }

    /**
     * Performs all daily and periodic standing checks for factions relevant to campaign campaign.
     *
     * <p>On the first day of the month, campaign method updates the climate regard for the active campaign faction,
     * storing a summary report. It then iterates once through all faction standings and, for each faction:</p>
     *
     * <ul>
     *     <li>Checks for new ultimatum events.</li>
     *     <li>Checks for new censure actions and handles the creation of related events.</li>
     *     <li>Evaluates for new accolade levels, creating corresponding events.</li>
     *     <li>Warns if any referenced faction cannot be resolved.</li>
     * </ul>
     *
     * <p>Finally, at the end of the checks, it processes censure degradation for all factions.</p>
     *
     * @param isFirstOfMonth {@code true} if called on the first day of the month.
     * @param isNewYear      {@code true} if called on the first day of a new year
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void performFactionStandingChecks(boolean isFirstOfMonth, boolean isNewYear) {
        String campaignFactionCode = faction.getShortName();
        if (isNewYear && campaignFactionCode.equals(MERCENARY_FACTION_CODE)) {
            campaign.checkForNewMercenaryOrganizationStartUp(false, false);
        }

        if (!campaignOptions.isTrackFactionStanding()) {
            return;
        }

        if (FactionStandingUltimatum.checkUltimatumForDate(today,
              campaignFactionCode,
              campaign.getFactionStandingUltimatumsLibrary())) {
            new FactionStandingUltimatum(today, campaign, campaign.getFactionStandingUltimatumsLibrary());
        }

        if (isFirstOfMonth) {
            String report = campaign.getFactionStandings().updateClimateRegard(faction,
                  today,
                  campaignOptions.getRegardMultiplier(),
                  campaignOptions.isTrackClimateRegardChanges());
            campaign.addReport(POLITICS, report);
        }

        List<Mission> activeMissions = campaign.getActiveMissions(false);
        boolean isInTransit = !updatedLocation.isOnPlanet();
        Factions factions = Factions.getInstance();

        for (Map.Entry<String, Double> standing : new HashMap<>(campaign.getFactionStandings()
                                                                      .getAllFactionStandings()).entrySet()) {
            String relevantFactionCode = standing.getKey();
            Faction relevantFaction = factions.getFaction(relevantFactionCode);
            if (relevantFaction == null) {
                LOGGER.warn("Unable to fetch faction standing for faction: {}", relevantFactionCode);
                continue;
            }

            // Censure check
            boolean isMercenarySpecialCase = campaignFactionCode.equals(MERCENARY_FACTION_CODE) &&
                                                   relevantFaction.isMercenaryOrganization();
            boolean isPirateSpecialCase = campaign.isPirateCampaign() &&
                                                relevantFactionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE);
            if (relevantFaction.equals(faction) || isMercenarySpecialCase || isPirateSpecialCase) {
                FactionCensureLevel newCensureLevel = campaign.getFactionStandings().checkForCensure(
                      relevantFaction, today, activeMissions, isInTransit);
                if (newCensureLevel != null) {
                    new FactionCensureEvent(campaign, newCensureLevel, relevantFaction);
                }
            }

            // Accolade check
            boolean ignoreEmployer = relevantFaction.isMercenaryOrganization();
            boolean isOnMission = FactionStandingUtilities.isIsOnMission(
                  !isInTransit,
                  campaign.getActiveAtBContracts(),
                  activeMissions,
                  relevantFactionCode,
                  updatedLocation.getCurrentSystem(),
                  ignoreEmployer);

            FactionAccoladeLevel newAccoladeLevel = campaign.getFactionStandings().checkForAccolade(
                  relevantFaction, today, isOnMission);

            if (newAccoladeLevel != null) {
                new FactionAccoladeEvent(campaign, relevantFaction, newAccoladeLevel,
                      faction.equals(relevantFaction));
            }
        }

        // Censure degradation
        campaign.getFactionStandings().processCensureDegradation(today);
    }

    /**
     * Process anniversaries for a given person, including birthdays and recruitment anniversaries.
     *
     * @param person The {@link Person} for whom the anniversaries will be processed
     */
    private void processAnniversaries(Person person) {
        LocalDate birthday = person.getBirthday(today.getYear());
        boolean isBirthday = birthday != null && birthday.equals(today);
        int age = person.getAge(today);

        boolean isUseEducation = campaignOptions.isUseEducationModule();
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isUseTurnover = campaignOptions.isUseRandomRetirement();

        final int JUNIOR_SCHOOL_AGE = 3;
        final int HIGH_SCHOOL_AGE = 10;
        final int EMPLOYMENT_AGE = 16;

        if ((person.getRank().isOfficer()) || (!campaignOptions.isAnnounceOfficersOnly())) {
            if (isBirthday && campaignOptions.isAnnounceBirthdays()) {
                String report = String.format(resources.getString("anniversaryBirthday.text"),
                      person.getHyperlinkedFullTitle(),
                      spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                      age,
                      CLOSING_SPAN_TAG);

                // Aging Effects
                AgingMilestone milestone = getMilestone(age);
                String milestoneText = "";
                if (isUseAgingEffects && milestone.getMinimumAge() == age) {
                    String milestoneLabel = milestone.getLabel();
                    milestoneText = String.format(resources.getString("anniversaryBirthday.milestone"), milestoneLabel);
                }
                if (!milestoneText.isBlank()) {
                    report += " " + milestoneText;
                }

                // Special Ages
                String addendum = "";
                if (isUseEducation && age == JUNIOR_SCHOOL_AGE) {
                    addendum = resources.getString("anniversaryBirthday.third");
                } else if (isUseEducation && age == HIGH_SCHOOL_AGE) {
                    addendum = resources.getString("anniversaryBirthday.tenth");
                } else if (age == EMPLOYMENT_AGE) { // This age is always relevant
                    addendum = resources.getString("anniversaryBirthday.sixteenth");
                }

                if (!addendum.isBlank()) {
                    report += " " + addendum;
                }

                // Retirement
                if (isUseTurnover && age >= RETIREMENT_AGE) {
                    report += " " + resources.getString("anniversaryBirthday.retirement");
                }

                campaign.addReport(PERSONNEL, report);
            }

            LocalDate recruitmentDate = person.getRecruitment();
            if (recruitmentDate != null) {
                LocalDate recruitmentAnniversary = recruitmentDate.withYear(today.getYear());
                int yearsOfEmployment = (int) ChronoUnit.YEARS.between(recruitmentDate, today);

                if ((recruitmentAnniversary.isEqual(today)) &&
                          (campaignOptions.isAnnounceRecruitmentAnniversaries())) {
                    campaign.addReport(PERSONNEL, String.format(resources.getString("anniversaryRecruitment.text"),
                          person.getHyperlinkedFullTitle(),
                          spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                          yearsOfEmployment,
                          CLOSING_SPAN_TAG,
                          campaign.getName()));
                }
            }
        } else if ((person.getAge(today) == 18) && (campaignOptions.isAnnounceChildBirthdays())) {
            if (isBirthday) {
                campaign.addReport(PERSONNEL, String.format(resources.getString("anniversaryBirthday.text"),
                      person.getHyperlinkedFullTitle(),
                      spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                      person.getAge(today),
                      CLOSING_SPAN_TAG));
            }
        }

        // This is where we update all the aging modifiers for the character.
        if (campaignOptions.isUseAgeEffects() && isBirthday) {
            applyAgingSPA(age, person);
        }

        // Coming of Age Events
        if (isBirthday && (person.getAge(today) == 16)) {
            if (campaignOptions.isRewardComingOfAgeAbilities()) {
                SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
                singleSpecialAbilityGenerator.rollSPA(campaign, person, true, true, false);
            }

            if (campaignOptions.isRewardComingOfAgeRPSkills()) {
                AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(campaign.getRandomSkillPreferences());
                skillGenerator.generateRoleplaySkills(person);
            }

            // We want the event trigger to fire before the dialog is shown, so that the character will have finished
            // updating in the gui before the player has a chance to jump to them
            MekHQ.triggerEvent(new PersonChangedEvent(person));

            if (campaignOptions.isShowLifeEventDialogComingOfAge()) {
                new ComingOfAgeAnnouncement(campaign, person);
            }
        }
    }

    /**
     * Process weekly relationship events for a given {@link Person} on Monday. This method triggers specific events
     * related to divorce, marriage, procreation, and maternity leave.
     *
     * @param person The {@link Person} for which to process weekly relationship events
     */
    private void processWeeklyRelationshipEvents(Person person) {
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            campaign.getDivorce().processNewWeek(campaign, today, person, false);
            campaign.getMarriage().processNewWeek(campaign, today, person, false);
            campaign.getProcreation().processNewWeek(campaign, today, person);
        }
    }

    /**
     * Processes all compulsions and madness-related effects for a given person, adjusting their status and generating
     * reports as needed.
     *
     * <p>This method checks for various mental conditions or compulsions that a person might suffer from, such as
     * addiction, flashbacks, split personality, paranoia, regression, catatonia, berserker rage, or hysteria. For each
     * condition the person possesses, the relevant check is performed and any resulting effects—such as status changes,
     * injuries, or event reports—are handled accordingly.</p>
     *
     * <p>The results of these checks may also generate narrative or status reports, which are added to the campaign
     * as appropriate. If certain conditions are no longer present, some status flags (such as clinical paranoia) may be
     * reset.</p>
     *
     * @param person                  the person whose conditions are being processed
     * @param personnelOptions        the set of personnel options or traits affecting which conditions are relevant
     * @param isUseAdvancedMedical    {@code true} if advanced medical rules are applied, {@code false} otherwise
     * @param isUseAltAdvancedMedical {@code true} if alt advanced medical rules are applied, {@code false} otherwise
     * @param isUseFatigue            {@code true} if fatigue rules are applied, {@code false} otherwise
     * @param fatigueRate             the user-defined rate at which fatigue is gained
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCompulsionsAndMadness(Person person, PersonnelOptions personnelOptions,
          boolean isUseAdvancedMedical, boolean isUseAltAdvancedMedical, boolean isUseFatigue, int fatigueRate) {
        String gamblingReport = person.gambleWealth();
        if (!gamblingReport.isBlank()) {
            campaign.addReport(PERSONNEL, gamblingReport);
        }

        if (personnelOptions.booleanOption(COMPULSION_PAINKILLER_ADDICTION)) {
            int prostheticMedicalReliance = 1; // Minimum of 1
            int myomerProsthetics = 0;
            boolean hasPowerSupply = false;

            for (Injury injury : person.getInjuries()) {
                InjurySubType injurySubType = injury.getSubType();
                if (injurySubType.isPermanentModification()) {
                    prostheticMedicalReliance++;
                }

                if (injurySubType.isMyomerProsthetic()) {
                    myomerProsthetics++;
                }

                if (!hasPowerSupply && injury.getType() == SECONDARY_POWER_SUPPLY) {
                    hasPowerSupply = true;
                }
            }

            if (!hasPowerSupply) {
                myomerProsthetics *= 2;
            }

            int totalProstheticCount = prostheticMedicalReliance + myomerProsthetics;

            Money cost = Money.of(PersonnelOptions.PAINKILLER_COST * totalProstheticCount);
            if (!finances.debit(TransactionType.MEDICAL_EXPENSES, today, cost,
                  getFormattedTextAt(RESOURCE_BUNDLE, "painkillerAddiction.transaction", person.getFullTitle()))) {
                checkForDiscontinuationSyndrome(person,
                      isUseAdvancedMedical,
                      isUseAltAdvancedMedical,
                      isUseFatigue,
                      fatigueRate);
            }
        }

        if (personnelOptions.booleanOption(COMPULSION_ADDICTION)) {
            checkForDiscontinuationSyndrome(person,
                  isUseAdvancedMedical,
                  isUseAltAdvancedMedical,
                  isUseFatigue,
                  fatigueRate);
        }

        if (personnelOptions.booleanOption(MADNESS_FLASHBACKS)) {
            int modifier = getCompulsionCheckModifier(MADNESS_FLASHBACKS);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            person.processCripplingFlashbacks(campaign,
                  isUseAdvancedMedical,
                  isUseAltAdvancedMedical,
                  true,
                  failedWillpowerCheck);
        }

        if (personnelOptions.booleanOption(MADNESS_SPLIT_PERSONALITY)) {
            int modifier = getCompulsionCheckModifier(MADNESS_SPLIT_PERSONALITY);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processSplitPersonality(true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }
        }

        boolean resetClinicalParanoia = true;
        if (personnelOptions.booleanOption(MADNESS_CLINICAL_PARANOIA)) {
            int modifier = getCompulsionCheckModifier(MADNESS_CLINICAL_PARANOIA);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processClinicalParanoia(true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }

            resetClinicalParanoia = false;
        }

        if (personnelOptions.booleanOption(MADNESS_REGRESSION)) {
            int modifier = getCompulsionCheckModifier(MADNESS_REGRESSION);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processChildlikeRegression(campaign,
                  isUseAdvancedMedical,
                  isUseAltAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_CATATONIA)) {
            int modifier = getCompulsionCheckModifier(MADNESS_CATATONIA);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processCatatonia(campaign,
                  isUseAdvancedMedical,
                  isUseAltAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_BERSERKER)) {
            int modifier = getCompulsionCheckModifier(MADNESS_BERSERKER);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processBerserkerFrenzy(campaign,
                  isUseAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_HYSTERIA)) {
            int modifier = getCompulsionCheckModifier(MADNESS_HYSTERIA);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processHysteria(campaign, true, isUseAdvancedMedical, failedWillpowerCheck);
            if (!report.isBlank()) {
                campaign.addReport(MEDICAL, report);
            }

            resetClinicalParanoia = false;
        }

        // This is necessary to stop a character from getting permanently locked in a paranoia state if the
        // relevant madness are removed.
        if (resetClinicalParanoia) {
            person.setSufferingFromClinicalParanoia(false);
        }
    }

    private void checkForDiscontinuationSyndrome(Person person, boolean isUseAdvancedMedical,
          boolean isUseAltAdvancedMedical, boolean isUseFatigue, int fatigueRate) {
        int modifier = getCompulsionCheckModifier(COMPULSION_ADDICTION);
        boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
              null, modifier);
        person.processDiscontinuationSyndrome(campaign,
              isUseAdvancedMedical,
              isUseAltAdvancedMedical,
              isUseFatigue,
              fatigueRate,
              true,
              failedWillpowerCheck);
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    void processShipSearch() {
        if (campaign.getShipSearchStart() == null) {
            return;
        }

        StringBuilder report = new StringBuilder();
        if (finances.debit(TransactionType.UNIT_PURCHASE,
              today,
              campaign.getAtBConfig().shipSearchCostPerWeek(),
              "Ship Search")) {
            report.append(campaign.getAtBConfig().shipSearchCostPerWeek().toAmountAndSymbolString())
                  .append(" deducted for ship search.");
        } else {
            campaign.addReport(FINANCES, "<font color=" +
                                               ReportingUtilities.getNegativeColor() +
                                               ">Insufficient funds for ship search.</font>");
            campaign.setShipSearchStart(null);
            return;
        }

        long numDays = ChronoUnit.DAYS.between(campaign.getShipSearchStart(), today);
        if (numDays > 21) {
            int roll = d6(2);
            TargetRoll target = campaign.getAtBConfig().shipSearchTargetRoll(campaign.getShipSearchType(), campaign);
            campaign.setShipSearchStart(null);
            report.append("<br/>Ship search target: ").append(target.getValueAsString()).append(" roll: ").append(roll);
            // TODO : mos zero should make ship available on retainer
            if (roll >= target.getValue()) {
                report.append("<br/>Search successful. ");

                MekSummary ms = campaign.getUnitGenerator().generate(faction.getShortName(),
                      campaign.getShipSearchType(),
                      -1,
                      today.getYear(),
                      campaign.getAtBUnitRatingMod());

                if (ms == null) {
                    ms = campaign.getAtBConfig().findShip(campaign.getShipSearchType());
                }

                if (ms != null) {
                    campaign.setShipSearchResult(ms.getName());
                    campaign.setShipSearchExpiration(today.plusDays(31));
                    report.append(campaign.getShipSearchResult())
                          .append(" is available for purchase for ")
                          .append(Money.of(ms.getCost()).toAmountAndSymbolString())
                          .append(" until ")
                          .append(MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getShipSearchExpiration()));
                } else {
                    report.append(" <font color=")
                          .append(ReportingUtilities.getNegativeColor())
                          .append(">Could not determine ship type.</font>");
                }
            } else {
                report.append("<br/>Ship search unsuccessful.");
            }
        }
        campaign.addReport(ACQUISITIONS, report.toString());
    }

    /**
     * Processes the resupply operation for a given contract.
     *
     * <p>For regular contracts, resupply always occurs. For guerrilla warfare contracts or contracts with pirate
     * employers, resupply occurs only 25% of the time (1 in 4 chance).</p>
     *
     * <p>The resupply type is determined by the contract nature:</p>
     * <ul>
     *     <li><b>Smuggler resupply:</b> Used for guerrilla warfare or pirate contracts</li>
     *     <li><b>Normal resupply:</b> Used for all other contract types</li>
     * </ul>
     *
     * @param contract the {@link AtBContract} for which resupply is being processed
     */
    private void processResupply(AtBContract contract) {
        boolean isGuerrilla = contract.getContractType().isGuerrillaType()
                                    || PIRATE_FACTION_CODE.equals(contract.getEmployerCode());

        if (!isGuerrilla || randomInt(4) == 0) {
            Resupply.ResupplyType resupplyType = isGuerrilla ?
                                                       Resupply.ResupplyType.RESUPPLY_SMUGGLER :
                                                       Resupply.ResupplyType.RESUPPLY_NORMAL;
            Resupply resupply = new Resupply(campaign, contract, resupplyType);
            performResupply(resupply, contract);
        }
    }

    /**
     * Process monthly auto awards for a given person based on their roles and experience level.
     *
     * @param person the person for whom the monthly auto awards are being processed
     */
    private void processMonthlyAutoAwards(Person person) {
        double multiplier = 0;

        int score = 0;

        if (person.getPrimaryRole().isSupport(true)) {
            int dice = person.getExperienceLevel(campaign, false);

            if (dice > 0) {
                score = d6(dice);
            }

            multiplier += 0.5;
        }

        if (person.getSecondaryRole().isSupport(true)) {
            int dice = person.getExperienceLevel(campaign, true);

            if (dice > 0) {
                score += d6(dice);
            }

            multiplier += 0.5;
        } else if (person.getSecondaryRole().isNone()) {
            multiplier += 0.5;
        }

        person.changeAutoAwardSupportPoints((int) (score * multiplier));
    }

    /**
     * Processes the monthly vocational experience (XP) gain for a given person based on their eligibility and the
     * vocational experience rules defined in campaign options.
     *
     * <p>
     * Eligibility for receiving vocational XP is determined by checking the following conditions:
     * <ul>
     * <li>The person must have an <b>active status</b> (e.g., not retired,
     * deceased, or in education).</li>
     * <li>The person must not be a <b>child</b> as of the current date.</li>
     * <li>The person must not be categorized as a <b>dependent</b>.</li>
     * <li>The person must not have the status of a <b>prisoner</b>.</li>
     * <b>Note:</b> Bondsmen are exempt from campaign restriction and are eligible for
     * vocational XP.
     * </ul>
     *
     * @param person           the {@link Person} whose monthly vocational XP is to be processed
     * @param vocationalXpRate the amount of XP awarded on a successful roll
     *
     * @return {@code true} if XP was successfully awarded during the process, {@code false} otherwise
     */
    private boolean processMonthlyVocationalXp(Person person, int vocationalXpRate) {
        if (!person.getStatus().isActive()) {
            return false;
        }

        if (person.isChild(today)) {
            return false;
        }

        if (person.isDependent()) {
            return false;
        }

        if (person.getPrisonerStatus().isCurrentPrisoner()) {
            // Prisoners can't gain vocational XP, while Bondsmen can
            return false;
        }

        int checkFrequency = campaignOptions.getVocationalXPCheckFrequency();
        int targetNumber = campaignOptions.getVocationalXPTargetNumber();

        person.setVocationalXPTimer(person.getVocationalXPTimer() + 1);
        if (person.getVocationalXPTimer() >= checkFrequency) {
            if (d6(2) >= targetNumber) {
                person.awardXP(campaign, vocationalXpRate);
                person.setVocationalXPTimer(0);
                return true;
            } else {
                person.setVocationalXPTimer(0);
            }
        }

        return false;
    }

    private void processNewDayATBScenarios() {
        // First, we get the list of all active AtBContracts
        List<AtBContract> contracts = campaign.getActiveAtBContracts(true);
        Set<Integer> allScenariosWithAssignedStandardForces = getAllScenariosWithAssignedStandardForces();

        // Second, we process them and any already generated scenarios
        for (AtBContract contract : contracts) {
            /*
             * Situations like a delayed start or running out of funds during transit can
             * delay arrival until after the contract start. In that case, shift the
             * starting and ending dates before making any battle rolls. We check that the
             * unit is actually on route to the planet in case the user is using a custom
             * system for transport or splitting the unit, etc.
             */
            if (!updatedLocation.isOnPlanet() &&
                      !updatedLocation.getJumpPath().isEmpty() &&
                      updatedLocation.getJumpPath().getLastSystem().getId().equals(contract.getSystemId())) {
                // transitTime is measured in days; so we round up to the next whole day
                contract.setStartAndEndDate(today.plusDays((int) ceil(updatedLocation.getTransitTime())));
                campaign.addReport(GENERAL, "The start and end dates of " +
                                                  contract.getHyperlinkedName() +
                                                  " have been shifted to reflect the current ETA.");

                if (campaignOptions.isUseStratCon() && contract.getMoraleLevel().isRouted()) {
                    LocalDate newRoutEndDate = contract.getStartDate().plusMonths(max(1, d6() - 3)).minusDays(1);
                    contract.setRoutEndDate(newRoutEndDate);
                }

                continue;
            }

            if (today.equals(contract.getStartDate())) {
                hangar.getUnits().forEach(unit -> unit.setSite(contract.getRepairLocation()));
            }

            if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
                int deficit = campaign.getDeploymentDeficit(contract);
                StratConCampaignState campaignState = contract.getStratconCampaignState();

                if (campaignState != null && deficit > 0) {
                    campaign.addReport(GENERAL, String.format(resources.getString("contractBreach.text"),
                          contract.getHyperlinkedName(),
                          spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                          CLOSING_SPAN_TAG));

                    campaignState.updateVictoryPoints(-1);
                } else if (deficit > 0) {
                    contract.addPlayerMinorBreaches(deficit);
                    campaign.addReport(GENERAL, "Failure to meet " +
                                                      contract.getHyperlinkedName() +
                                                      " requirements resulted in " +
                                                      deficit +
                                                      ((deficit == 1) ?
                                                             " minor contract breach" :
                                                             " minor contract breaches"));
                }
            }

            for (final Scenario scenario : contract.getCurrentAtBScenarios()) {
                if ((scenario.getDate() != null) && scenario.getDate().isBefore(today)) {
                    boolean hasForceDeployed = allScenariosWithAssignedStandardForces.contains(scenario.getId());
                    if (campaignOptions.isUseStratCon() && (scenario instanceof AtBDynamicScenario)) {
                        StratConCampaignState campaignState = contract.getStratconCampaignState();

                        if (campaignState == null) {
                            return;
                        }

                        processIgnoredDynamicScenario(scenario.getId(), campaignState);

                        ScenarioType scenarioType = scenario.getStratConScenarioType();
                        if (scenarioType.isResupply()) {
                            processAbandonedConvoy(campaign, contract, (AtBDynamicScenario) scenario);
                        }

                        scenario.clearAllForcesAndPersonnel(campaign);
                    } else {
                        contract.addPlayerMinorBreach();

                        campaign.addReport(BATTLE, "Failure to deploy for " +
                                                         scenario.getHyperlinkedName() +
                                                         " resulted in a minor contract breach.");
                    }

                    scenario.convertToStub(campaign,
                          hasForceDeployed ? ScenarioStatus.FLEET_IN_BEING : ScenarioStatus.REFUSED_ENGAGEMENT);
                }
            }
        }

        // Third, on Mondays we generate new scenarios for the week
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            AtBScenarioFactory.createScenariosForNewWeek(campaign);
        }

        // Fourth, we look at deployments for pre-existing and new scenarios
        for (AtBContract contract : contracts) {
            contract.checkEvents(campaign);

            // If there is a standard battle set for today, deploy the lance.
            for (final AtBScenario atBScenario : contract.getCurrentAtBScenarios()) {
                if ((atBScenario.getDate() != null) && atBScenario.getDate().equals(today)) {
                    int forceId = atBScenario.getCombatTeamId();
                    if ((campaign.getCombatTeamsAsMap().get(forceId) != null) &&
                              !campaign.getForceIds().get(forceId).isDeployed()) {
                        // If any unit in the force is under repair, don't deploy the force
                        // Merely removing the unit from deployment would break with user expectation
                        boolean forceUnderRepair = false;
                        for (UUID uid : campaign.getForceIds().get(forceId).getAllUnits(false)) {
                            Unit u = hangar.getUnit(uid);
                            if ((u != null) && u.isUnderRepair()) {
                                forceUnderRepair = true;
                                break;
                            }
                        }

                        if (!forceUnderRepair) {
                            campaign.getForceIds().get(forceId).setScenarioId(atBScenario.getId(), campaign);
                            atBScenario.addForces(forceId);

                            campaign.addReport(BATTLE, MessageFormat.format(resources.getString(
                                        "atbScenarioTodayWithForce.format"),
                                  atBScenario.getHyperlinkedName(),
                                  campaign.getForceIds().get(forceId).getName()));
                            MekHQ.triggerEvent(new DeploymentChangedEvent(campaign.getForceIds().get(forceId),
                                  atBScenario));
                        } else {
                            if (atBScenario.getHasTrack()) {
                                campaign.addReport(BATTLE, MessageFormat.format(resources.getString("atbScenarioToday" +
                                                                                                          ".stratCon"),
                                      atBScenario.getHyperlinkedName()));
                            } else {
                                campaign.addReport(BATTLE, MessageFormat.format(resources.getString("atbScenarioToday" +
                                                                                                          ".atb"),
                                      atBScenario.getHyperlinkedName()));
                            }
                        }
                    } else {
                        if (atBScenario.getHasTrack()) {
                            campaign.addReport(BATTLE,
                                  MessageFormat.format(resources.getString("atbScenarioToday.stratCon"),
                                        atBScenario.getHyperlinkedName()));
                        } else {
                            campaign.addReport(BATTLE, MessageFormat.format(resources.getString("atbScenarioToday.atb"),
                                  atBScenario.getHyperlinkedName()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates and processes payment for all types of rented facilities (hospital beds, kitchens, holding cells)
     * based on the active contracts and current campaign options.
     *
     * <p>Generates reports for any failed transactions or payment issues. Adds any generated reports to the campaign
     * log.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void payForRentedFacilities() {
        List<Contract> activeContracts = campaign.getActiveContracts();
        int hospitalRentalCost = campaignOptions.getRentedFacilitiesCostHospitalBeds();
        Money hospitalRentalFee = FacilityRentals.calculateContractRentalCost(hospitalRentalCost, activeContracts,
              ContractRentalType.HOSPITAL_BEDS);

        int kitchenRentalCost = campaignOptions.getRentedFacilitiesCostKitchens();
        Money kitchenRentalFee = FacilityRentals.calculateContractRentalCost(kitchenRentalCost, activeContracts,
              ContractRentalType.KITCHENS);

        int holdingCellRentalCost = campaignOptions.getRentedFacilitiesCostHoldingCells();
        Money holdingCellRentalFee = FacilityRentals.calculateContractRentalCost(holdingCellRentalCost, activeContracts,
              ContractRentalType.HOLDING_CELLS);

        List<String> reports = FacilityRentals.payForAllContractRentals(finances, today, hospitalRentalFee,
              kitchenRentalFee, holdingCellRentalFee);
        for (String report : reports) { // No report is generated if the transaction is successful
            campaign.addReport(FINANCES, report);
        }
    }

    /**
     * Updates the value of {@code mashTheatreCapacity} based on the current campaign options and force composition.
     *
     * <p>If the campaign is configured to use MASH theatres, this method calculates the available MASH theatre
     * capacity using the current force and campaign options. If MASH theatres are not enabled, the capacity is set to
     * zero.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateMASHTheatreCapacity() {
        if (campaignOptions.isUseMASHTheatres()) {
            int mashTheatreCapacity =
                  MASHCapacity.checkMASHCapacity(campaign.getForce(FORCE_ORIGIN).getAllUnitsAsUnits(hangar,
                        false), campaignOptions.getMASHTheatreCapacity());
            mashTheatreCapacity += FacilityRentals.getCapacityIncreaseFromRentals(campaign.getActiveContracts(),
                  ContractRentalType.HOSPITAL_BEDS);
            campaign.setMashTheatreCapacity(mashTheatreCapacity);
        } else {
            campaign.setMashTheatreCapacity(0);
        }
    }
}
