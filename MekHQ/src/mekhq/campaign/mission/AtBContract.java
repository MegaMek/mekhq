/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.client.ratgenerator.ModelRecord.NETWORK_NONE;
import static megamek.client.ratgenerator.UnitTable.findTable;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static megamek.common.enums.SkillLevel.parseFromString;
import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.force.CombatTeam.getStandardFormationSize;
import static mekhq.campaign.force.FormationLevel.BATTALION;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.FREE;
import static mekhq.campaign.stratCon.StratConContractDefinition.getContractDefinition;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.Version;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.randomEvents.MercenaryAuction;
import mekhq.campaign.randomEvents.RoninOffer;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConContractDefinition;
import mekhq.campaign.stratCon.StratConContractInitializer;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.factionStanding.PerformBatchall;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.view.MoraleBar;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contract class for use with Against the Bot rules
 *
 * @author Neoancient
 */
public class AtBContract extends Contract {
    private static final MMLogger logger = MMLogger.create(AtBContract.class);

    /* null unless subcontract */
    protected AtBContract parentContract;
    /* hired by another mercenary unit on contract to a third-party employer */
    boolean mercSubcontract;

    protected int difficulty;

    protected int extensionLength;

    protected int playerMinorBreaches;
    protected int employerMinorBreaches;
    protected int contractScoreArbitraryModifier;

    protected int moraleMod = 0;

    /* lasts for a month, then removed at next events roll */
    protected boolean priorLogisticsFailure;
    /**
     * If the date is non-null, there will be a special scenario or big battle on that date, but the scenario is not
     * generated until the other battle rolls for the week.
     */
    protected LocalDate specialEventScenarioDate;
    protected int specialEventScenarioType;
    /* Lasts until end of contract */
    protected int battleTypeMod;
    /* Only applies to next week */
    protected int nextWeekBattleTypeMod;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBContract";

    protected AtBContract() {
        this(null);
    }

    public AtBContract(String name) {
        difficulty = Integer.MIN_VALUE;

        parentContract = null;
        setContractType(AtBContractType.GARRISON_DUTY);

        extensionLength = 0;

        setSharesPercent(0);
        priorLogisticsFailure = false;
        specialEventScenarioDate = null;
        battleTypeMod = 0;
        nextWeekBattleTypeMod = 0;
    }

    public void initContractDetails(Campaign campaign) {
        int companySize = getStandardFormationSize(campaign.getFaction(), COMPANY.getDepth());
        int battalionSize = getStandardFormationSize(campaign.getFaction(), BATTALION.getDepth());

        if (ContractUtilities.getEffectiveNumUnits(campaign) <= companySize) {
            setOverheadCompensation(OH_FULL);
        } else if (ContractUtilities.getEffectiveNumUnits(campaign) <= battalionSize) {
            setOverheadCompensation(OH_HALF);
        } else {
            setOverheadCompensation(OH_NONE);
        }

        int currentYear = campaign.getGameYear();
        setAllyBotName(getEmployerName(currentYear));
        setAllyCamouflage(pickRandomCamouflage(currentYear, getEmployerCode()));

        setEnemyBotName(generateEnemyName(currentYear));
        setEnemyCamouflage(pickRandomCamouflage(currentYear, getEnemyCode()));
    }

    /**
     * Selects a random camouflage for the given faction based on the faction code and year. If there are no available
     * files in the faction directory, it logs a warning and uses default camouflage.
     *
     * @param currentYear the current year in the game.
     * @param factionCode the code representing the faction for which the camouflage is to be selected.
     */
    public static Camouflage pickRandomCamouflage(int currentYear, String factionCode) {
        // Define the root directory and get the faction-specific camouflage directory
        final String ROOT_DIRECTORY = "data/images/camo/";

        String camouflageDirectory = "Standard Camouflage";

        if (factionCode != null) {
            camouflageDirectory = getCamouflageDirectory(currentYear, factionCode);
        }

        // Gather all files
        List<Path> allPaths = null;

        try (Stream<Path> stream = Files.find(Paths.get(ROOT_DIRECTORY + camouflageDirectory + '/'),
              Integer.MAX_VALUE,
              (path, bfa) -> bfa.isRegularFile())) {
            allPaths = stream.toList();
        } catch (IOException e) {
            logger.error("Error getting list of camouflages", e);
        }

        // Select a random file to set camouflage, if there are files available
        if ((null != allPaths) && (!allPaths.isEmpty())) {
            Path randomPath = allPaths.get(new Random().nextInt(allPaths.size()));

            String fileName = randomPath.getFileName().toString();
            String fileCategory = randomPath.getParent()
                                        .toString()
                                        .replaceAll("\\\\", "/"); // This is necessary for Windows machines
            fileCategory = fileCategory.replace(ROOT_DIRECTORY, "");

            return new Camouflage(fileCategory, fileName);
        } else {
            // Log if no files were found in the directory
            logger.warn("No files in directory {} - using default camouflage", camouflageDirectory);
            return new Camouflage(); // return no camouflage
        }
    }

    /**
     * Returns the directory for the camouflages of a faction based on the year and faction code.
     *
     * @param year        The year
     * @param factionCode The code representing the faction, e.g. FS or HL
     *
     * @return The directory under data/images/camo for the camouflages of the faction
     */
    private static String getCamouflageDirectory(int year, String factionCode) {
        return Factions.getInstance().getFaction(factionCode)
                     .getCamosFolder(year)
                     .orElse("Standard Camouflage");
    }

    public void calculateLength(final boolean variable) {
        setLengthInMonths(getContractType().calculateLength(variable));
    }

    /**
     * Checks and updates the morale which depends on various conditions such as the rout end date, skill levels,
     * victories, defeats, etc. This method also updates the enemy status based on the morale level.
     *
     * @param today The current date in the context.
     */
    public void checkMorale(Campaign campaign, LocalDate today) {

        // If there is a rout end date, and it's past today, update morale and enemy state accordingly
        if (getRoutEndDate() != null) {
            // Check whether any current rout continues beyond its expected date. This is only applicable for
            // Garrison Type contracts. For all other types we reinforce immediately
            boolean routContinue = getContractType().isGarrisonType() && randomInt(4) == 0;
            if (routContinue) {
                return;
            }

            if (today.isAfter(getRoutEndDate())) {
                int roll = randomInt(8);

                // We use variable morale levels to spike morale up to a value above Stalemate. This works with the
                // regenerated Scenario Odds to create very high intensity spikes in otherwise low-key Garrison-type
                // contracts.
                AtBMoraleLevel newMoraleLevel = switch (roll) {
                    case 2, 3, 4, 5 -> ADVANCING;
                    case 6, 7 -> DOMINATING;
                    case 8 -> OVERWHELMING;
                    default -> STALEMATE; // 0-1
                };

                // If we have a StratCon enabled contract, regenerate Scenario Odds
                if (getStratConCampaignState() != null) {
                    StratConContractDefinition contractDefinition = getContractDefinition(getContractType());

                    if (contractDefinition != null) {
                        for (StratConTrackState trackState : getStratConCampaignState().getTracks()) {
                            int scenarioOdds = StratConContractInitializer.getScenarioOdds(contractDefinition);

                            trackState.setScenarioOdds(scenarioOdds);
                        }
                    }
                }

                setMoraleLevel(newMoraleLevel);
                setRoutEndDate(null);

                String key = "routEnded.reinforcements";
                if (getContractType().isGarrisonDuty() || getContractType().isRetainer()) {
                    updateEnemy(campaign, today); // mix it up a little
                    key = "routEnded.aNewChallenger";
                }

                new ImmersiveDialogSimple(campaign,
                      getEmployerLiaison(),
                      null,
                      getFormattedTextAt(RESOURCE_BUNDLE,
                            key,
                            campaign.getCommanderAddress(),
                            FactionStandingUtilities.getFactionName(getEnemy(), today.getYear())),
                      null,
                      null,
                      null,
                      false);
            }

            return;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        String moraleReport = MHQMorale.performMoraleCheck(today, this,
              campaignOptions.getMoraleDecisiveVictoryEffect(), campaignOptions.getMoraleVictoryEffect(),
              campaignOptions.getMoraleDecisiveDefeatEffect(), campaignOptions.getMoraleDefeatEffect());
        String flavorText = MHQMorale.getFormattedTitle()
                                  + "<h2 style='text-align:center;'>" + getName() + "</h2>"
                                  + MoraleBar.getMoraleDisplay(this).tooltip();
        new ImmersiveDialogNotification(campaign, flavorText, moraleReport, MoraleBar.createDialogPanel(this),
              true);

        MHQMorale.routedMoraleUpdate(campaign, this);

        // Reset external morale modifier
        moraleMod = 0;
    }

    private void updateEnemy(Campaign campaign, LocalDate today) {
        updateEnemy(campaign, today, null);
    }

    /**
     * Updates the enemy faction and enemy bot name for this contract.
     *
     * @param campaign  The current campaign.
     * @param today     The current LocalDate object.
     * @param enemyCode {@code Nullable} the code for the new faction, if {@code null} an appropriate random faction
     *                  will be used
     */
    public void updateEnemy(Campaign campaign, LocalDate today, @Nullable String enemyCode) {
        if (enemyCode == null) {
            Faction employer = getEmployerFaction();
            enemyCode = RandomFactionGenerator.getInstance().getEnemy(employer, false, true);
        }
        setEnemyCode(enemyCode);

        Faction enemyFaction = Factions.getInstance().getFaction(enemyCode);
        setEnemyBotName(enemyFaction.getFullName(today.getYear()));
        setEnemyName(""); // wipe the old enemy name
        generateEnemyName(today.getYear()); // we use this to update enemyName
        if (enemyFaction.isClan()) {
            createClanOpponent(campaign);
        }

        // We have a check in getEnemyName that prevents rolling over mercenary names, so we add this extra step to
        // force a mercenary name re-roll, in the event one Mercenary faction is replaced with another.
        if (Factions.getInstance().getFaction(enemyCode).isMercenary()) {
            setEnemyBotName(BackgroundsController.randomMercenaryCompanyNameGenerator(null));
        }

        setAllyCamouflage(pickRandomCamouflage(today.getYear(), getEmployerCode()));
        setEnemyCamouflage(pickRandomCamouflage(today.getYear(), enemyCode));

        // Update the Batchall information
        setBatchallAccepted(true);
        if (campaign.getCampaignOptions().isUseGenericBattleValue() && enemyFaction.performsBatchalls()) {
            boolean tracksStanding = campaign.getCampaignOptions().isTrackFactionStanding();
            FactionStandings factionStandings = campaign.getFactionStandings();

            boolean allowBatchalls = true;
            if (campaign.getCampaignOptions().isUseFactionStandingBatchallRestrictionsSafe()) {
                double regard = factionStandings.getRegardForFaction(enemyFaction.getShortName(), true);
                allowBatchalls = FactionStandingUtilities.isBatchallAllowed(regard);
            }

            double regardMultiplier = campaign.getCampaignOptions().getRegardMultiplier();
            String campaignFactionCode = campaign.getFaction().getShortName();
            if (enemyFaction.performsBatchalls() && allowBatchalls) {
                PerformBatchall batchallDialog = new PerformBatchall(campaign, getClanOpponent(), enemyCode);

                setBatchallAccepted(batchallDialog.isBatchallAccepted());

                if (!isBatchallAccepted() && tracksStanding) {
                    List<String> reports = factionStandings.processRefusedBatchall(campaignFactionCode, enemyCode,
                          today.getYear(), regardMultiplier);

                    for (String report : reports) {
                        campaign.addReport(GENERAL, report);
                    }
                }
            }

            if (tracksStanding) {
                // Whenever we dynamically change the enemy faction, we update standing accordingly
                String report = factionStandings.processContractAccept(campaignFactionCode, enemyFaction, today,
                      regardMultiplier, getLengthInMonths());
                if (report != null) {
                    campaign.addReport(POLITICS, report);
                }
            }
        }

        // Check for emergency clause (this can trigger multiple times if the enemy faction keeps changing to a Clan
        // faction. This can be seen as the employer getting increasingly desperate and wanting to keep the player on
        // side.
        checkForSpecialClanSalvageClause(campaign, today, enemyFaction);
    }

    /**
     * Checks for and applies a special emergency salvage clause when fighting Clan forces prior to or during the Battle
     * of Tukayyid.
     *
     * <p>If the employer is non-Clan and the enemy is Clan, and the current date is on or before the Battle of
     * Tukayyid, the salvage percentage is increased by 25% (up to a minimum of 100%) and salvage exchange is enabled.
     * An immersive dialog is displayed to inform the player of the contract adjustment.</p>
     *
     * @param campaign     The current campaign instance.
     * @param today        The current game date to check against the Tukayyid threshold.
     * @param enemyFaction The faction being fought in the current contract or mission.
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void checkForSpecialClanSalvageClause(Campaign campaign, LocalDate today, Faction enemyFaction) {
        if (!getEmployerFaction().isClan() && enemyFaction.isClan()) {
            if (!today.isAfter(BATTLE_OF_TUKAYYID)) {
                int oldSalvagePercent = getSalvagePercent();
                int newSalvagePercent = (int) max(100, round(oldSalvagePercent * 1.25));

                boolean isAlreadyMax = oldSalvagePercent >= 100;

                setSalvageExchange(true);
                setSalvagePercent(newSalvagePercent);

                String message = getTextAt(RESOURCE_BUNDLE, "emergencySalvageClause.message");
                if (!isAlreadyMax) {
                    message += getFormattedTextAt(RESOURCE_BUNDLE, "emergencySalvageClause.addendum",
                          oldSalvagePercent, newSalvagePercent);
                }
                new ImmersiveDialogSimple(campaign, getEmployerLiaison(), null, message, null, null, null, false);
            }
        }
    }

    /**
     * Determines the repair location for the contract based on the contract type.
     *
     * <p>The returned repair location corresponds to the type of operation:</p>
     *
     * <ul>
     *   <li>Guerrilla warfare contracts: {@link Unit#SITE_IMPROVISED}</li>
     *   <li>Raid-type contracts: {@link Unit#SITE_FIELD_WORKSHOP}</li>
     *   <li>All other contracts: {@link Unit#SITE_FACILITY_BASIC}</li>
     * </ul>
     *
     * @return the repair location constant based on the contract type
     */
    @Override
    public int getRepairLocation() {
        int repairLocation = Unit.SITE_FACILITY_BASIC;

        AtBContractType contractType = getContractType();

        if (contractType.isGuerrillaType()) {
            repairLocation = Unit.SITE_IMPROVISED;
        } else if (contractType.isRaidType()) {
            repairLocation = Unit.SITE_FIELD_WORKSHOP;
        }

        return repairLocation;
    }

    /**
     * Determines the best available repair location from a list of active contracts.
     *
     * <p>This method evaluates all active contracts and returns the highest quality repair facility available.
     * Repair locations are ranked numerically, with higher values representing better facilities. If no active
     * contracts exist, a basic facility is assumed to be available.</p>
     *
     * @param activeContracts the list of active contracts to evaluate for repair facilities
     *
     * @return the numeric value of the best available repair location; returns {@link Unit#SITE_FACILITY_BASIC} if no
     *       contracts are active
     */
    public static int getBestRepairLocation(List<AtBContract> activeContracts) {
        if (activeContracts.isEmpty()) {
            return Unit.SITE_FACILITY_BASIC;
        }

        int bestSite = Unit.SITE_IMPROVISED;
        for (AtBContract contract : activeContracts) {
            int repairLocation = contract.getRepairLocation();
            if (repairLocation > bestSite) {
                bestSite = repairLocation;
            }
        }

        return bestSite;
    }

    /**
     * Calculates the overall contract score based on scenario outcomes and modifiers.
     *
     * <p>For StratCon campaigns, this returns the current victory points from the campaign state.</p>
     *
     * <p>For standard contracts, this aggregates scores from all completed scenarios and applies any arbitrary
     * modifiers that have been set for this contract.</p>
     *
     * @param isUseMaplessMode {@code true} if mapless mode is enabled in StratCon
     *
     * @return the total contract score, including victory points or scenario scores plus modifiers
     */
    public int getContractScore(boolean isUseMaplessMode) {
        if (!isUseMaplessMode && getStratConCampaignState() != null) {
            return getStratConCampaignState().getVictoryPoints();
        }

        return ContractScore.getContractScore(getCompletedScenarios()) + contractScoreArbitraryModifier;
    }


    /**
     * @return the total available support points, or 0 if StratCon is not enabled for this contract
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getCurrentSupportPoints() {
        if (getStratConCampaignState() == null) {
            return 0;
        }

        return getStratConCampaignState().getSupportPoints();
    }

    public int getContractScoreArbitraryModifier() {
        return contractScoreArbitraryModifier;
    }

    /**
     * Performs a bonus roll to determine and execute a random campaign bonus. The roll is simulated using 1d6, and the
     * outcome triggers different bonus effects based on the roll value. The effects may involve recruiting dependents,
     * adding new units, or other benefits as determined by the campaign options and roll outcome.
     *
     * @param campaign       the current {@link Campaign} instance.
     * @param isPostScenario a {@code boolean} indicating if this roll occurs post-scenario (used to determine specific
     *                       behaviors for roll = 3).
     *
     * @return {@code true} if specific post-scenario behavior is triggered (roll = 3), otherwise {@code false}.
     *
     * @throws IllegalStateException if an unexpected roll value is encountered.
     */
    public boolean doBonusRoll(Campaign campaign, boolean isPostScenario) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        int number;
        int roll = d6();

        return switch (roll) {
            case 1 -> { /* 1d6 dependents */
                if (campaignOptions.isUseRandomDependentAddition()) {
                    number = d6();
                    campaign.addReport(GENERAL, "Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));

                    for (int i = 0; i < number; i++) {
                        Person person = campaign.newDependent(Gender.RANDOMIZE);
                        campaign.recruitPerson(person, FREE, true, false, false);
                    }
                } else {
                    campaign.addReport(GENERAL, "Bonus: Ronin");
                    new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                }
                yield false;
            }
            case 2 -> {
                campaign.addReport(GENERAL, "Bonus: Ronin");
                new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                yield false;
            }
            case 3 -> { // Resupply
                if (!campaignOptions.isUseStratCon()) {
                    campaign.addReport(GENERAL, "Bonus: Ronin");
                    new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                    yield false;
                } else {
                    if (isPostScenario) {
                        yield true;
                    } else {
                        campaign.addReport(GENERAL, "Bonus: Support Point");
                        getStratConCampaignState().changeSupportPoints(1);
                        yield false;
                    }
                }
            }
            case 4 -> {
                new MercenaryAuction(campaign, getRequiredCombatElements(), getStratConCampaignState(), TANK);
                yield false;
            }
            case 5 -> {
                new MercenaryAuction(campaign,
                      getRequiredCombatElements(),
                      getStratConCampaignState(),
                      AEROSPACE_FIGHTER);
                yield false;
            }
            case 6 -> {
                new MercenaryAuction(campaign, getRequiredCombatElements(), getStratConCampaignState(), MEK);
                yield false;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/mission/AtBContract.java/doBonusRoll: " + roll);
        };
    }

    public boolean isSubcontract() {
        return parentContract != null;
    }

    public AtBContract getParentContract() {
        return parentContract;
    }

    public void setParentContract(AtBContract parent) {
        parentContract = parent;
    }

    public boolean isMercSubcontract() {
        return mercSubcontract;
    }

    public void setMercSubcontract(boolean sub) {
        mercSubcontract = sub;
    }

    public void checkEvents(Campaign campaign) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        boolean isUseStratCon = campaign.getCampaignOptions().isUseStratCon();

        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                changePartsAvailabilityLevel(-1);
                priorLogisticsFailure = false;
            }

            String text;
            switch (getContractType().generateEventType(campaign)) {
                case BONUS_ROLL:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> ");
                    doBonusRoll(campaign, false);
                    break;
                case SPECIAL_SCENARIO:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Special scenario this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getContractType().generateSpecialScenarioType(campaign);
                    break;
                case CIVIL_DISTURBANCE:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Civil disturbance<br />Next enemy morale roll gets +1 modifier");
                    moraleMod++;
                    break;
                case SPORADIC_UPRISINGS:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Sporadic uprisings<br />+2 to next enemy morale roll");
                    moraleMod += 2;
                    break;
                case REBELLION:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Rebellion<br />+2 to next enemy morale roll");
                    moraleMod += 2;

                    if (!isUseStratCon) {
                        specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                        specialEventScenarioType = AtBScenario.CIVILIAN_RIOT;
                    }
                    break;
                case BETRAYAL:
                    text = "<b>Special Event:</b> Betrayal (employer minor breach)<br />";
                    switch (d6()) {
                        case 1:
                            text += "Major logistics problem: parts availability level for the rest of the contract becomes one level lower.";
                            changePartsAvailabilityLevel(1);
                            break;
                        case 2:
                            text += "Transport: Player is abandoned in the field by employer transports; if he loses a Base Attack battle he loses all Meks on repair.";
                            break;
                        case 3:
                            text += "Diversion: All Battle Type rolls for the rest of the contract get a -5 modifier.";
                            battleTypeMod -= 5;
                            break;
                        case 4:
                            text += "False Intelligence: Next week Battle Type rolls get a -10 modifier.";
                            nextWeekBattleTypeMod -= 10;
                            break;
                        case 5:
                            text += "The Company Store: All equipment/supply prices are increased by 100% until the end of the contract.";
                            break;
                        case 6:
                            text += "False Alarm: No betrayal, but the employer still gets a minor breach.";
                            break;
                    }
                    employerMinorBreaches++;
                    campaign.addReport(GENERAL, text);
                    break;
                case TREACHERY:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Treachery<br />Bad information from employer. Next Enemy Morale roll gets +1. Employer minor breach.");
                    moraleMod++;
                    employerMinorBreaches++;
                    break;
                case LOGISTICS_FAILURE:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Logistics Failure<br />Parts availability for the next month are one level lower.");
                    changePartsAvailabilityLevel(1);
                    priorLogisticsFailure = true;
                    break;
                case REINFORCEMENTS:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Reinforcements<br />The next Enemy Morale roll gets a -1.");
                    moraleMod--;
                    break;
                case SPECIAL_EVENTS:
                    text = "<b>Special Event:</b> ";
                    switch (d6()) {
                        case 1:
                            text += "Change of Alliance: Next Enemy Morale roll gets a +1 modifier.";
                            moraleMod++;
                            break;
                        case 2:
                            text += "Internal Dissension";
                            if (!isUseStratCon) {
                                specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                                specialEventScenarioType = AtBScenario.AMBUSH;
                            } else {
                                StratConCampaignState campaignState = getStratConCampaignState();

                                if (campaignState != null) {
                                    text += ": -1 Support Point";
                                    campaignState.changeSupportPoints(-1);
                                }
                            }
                            break;
                        case 3:
                            text += "ComStar Interdict: Base availability level decreases one level for the rest of the contract.";
                            changePartsAvailabilityLevel(1);
                            break;
                        case 4:
                            text += "Defectors: Next Enemy Morale roll gets a -1 modifier.";
                            moraleMod--;
                            break;
                        case 5:
                            text += "Free Trader: Base availability level increases one level for the rest of the contract.";
                            changePartsAvailabilityLevel(-1);
                            break;
                        case 6:
                            final String unitName = campaign.getUnitMarket()
                                                          .addSingleUnit(campaign,
                                                                UnitMarketType.EMPLOYER,
                                                                MEK,
                                                                getEmployerFaction(),
                                                                DragoonRating.DRAGOON_F.getRating(),
                                                                50);
                            if (unitName != null) {
                                text += String.format(
                                      "Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>",
                                      unitName);
                            }
                            break;
                    }
                    campaign.addReport(GENERAL, text);
                    break;
                case BIG_BATTLE:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Big battle this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getContractType().generateBigBattleType();
                    break;
            }
        }

        /*
         * If the campaign somehow gets past the scheduled date (such as by changing the date in the campaign
         * options), ignore it rather than generating a new scenario in the past. The event will still be available
         * (if the campaign date is restored) until another special scenario or big battle event is rolled.
         */
        if ((specialEventScenarioDate != null) && !specialEventScenarioDate.isBefore(campaign.getLocalDate())) {
            LocalDate nextMonday = campaign.getLocalDate()
                                         .plusDays(8 - campaign.getLocalDate().getDayOfWeek().getValue());

            if (specialEventScenarioDate.isBefore(nextMonday)) {
                AtBScenario atBScenario = AtBScenarioFactory.createScenario(campaign,
                      null,
                      specialEventScenarioType,
                      false,
                      specialEventScenarioDate);

                if (atBScenario != null) {
                    campaign.addScenario(atBScenario, this);

                    if (campaign.getCampaignOptions().isUsePlanetaryConditions()) {
                        atBScenario.setPlanetaryConditions(this, campaign);
                    }

                    atBScenario.setForces(campaign);
                }

                specialEventScenarioDate = null;
            }
        }
    }

    public LocalDate getRandomDayOfMonth(LocalDate today) {
        return LocalDate.of(today.getYear(),
              today.getMonth(),
              randomInt(today.getMonth().length(today.isLeapYear())) + 1);
    }

    public boolean contractExtended(final Campaign campaign) {
        if (getContractType().isPirateHunting() || getContractType().isRiotDuty()) {
            return false;
        }

        final String warName = RandomFactionGenerator.getInstance()
                                     .getFactionHints()
                                     .getCurrentWar(getEmployerFaction(), getEnemy(), campaign.getLocalDate());
        if (warName == null) {
            return false;
        }

        final int extension;
        int roll = d6();

        if (roll == 1) {
            extension = max(1, getLengthInMonths() / 2);
        } else if (roll == 2) {
            extension = 1;
        } else {
            return false;
        }

        campaign.addReport(GENERAL, String.format(
              "Due to the %s crisis your employer has invoked the emergency clause and extended the contract %d %s",
              warName,
              extension,
              ((extension == 1) ? " month" : " months")));
        setEndingDate(getEndingDate().plusMonths(extension));
        extensionLength += extension;

        // We spike morale to create a jump in contract difficulty - essentially the reason why the employer is using
        // the emergency clause.
        int moraleOrdinal = getMoraleLevel().ordinal();
        roll = d6(2) / 2;

        // we need to reset routEnd to null otherwise we'll attempt to rally
        if (getRoutEndDate() != null) {
            setRoutEndDate(null);
        }

        moraleOrdinal = min(moraleOrdinal + roll, OVERWHELMING.ordinal());
        setMoraleLevel(AtBMoraleLevel.values()[moraleOrdinal]);

        campaign.addReport(GENERAL, getMoraleLevel().getToolTipText());

        MekHQ.triggerEvent(new MissionChangedEvent(this));
        return true;
    }

    @Override
    public Money getMonthlyPayOut() {
        if (extensionLength == 0) {
            return super.getMonthlyPayOut();
        }
        /*
         * The transport clause and the advance monies have already been accounted for over the original length of
         * the contract. The extension uses the base monthly amounts for support and overhead, with a 50% bonus to
         * the base amount.
         */

        if (getLengthInMonths() <= 0) {
            return Money.zero();
        }

        return getBaseAmount().multipliedBy(1.5)
                     .plus(getSupportAmount())
                     .plus(getOverheadAmount())
                     .dividedBy(getLengthInMonths());
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(campaign, pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerCode", getEmployerCode());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCode", getEnemyCode());

        if (getEnemyMercenaryEmployerCode() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyMercenaryEmployerCode", getEnemyMercenaryEmployerCode());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractType", getContractType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allySkill", getAllySkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyQuality", getAllyQuality());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemySkill", getEnemySkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyQuality", getEnemyQuality());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "difficulty", getDifficulty());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyBotName", getAllyBotName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyBotName", getEnemyBotName());

        if (!getAllyCamouflage().hasDefaultCategory()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyCamoCategory", getAllyCamouflage().getCategory());
        }

        if (!getAllyCamouflage().hasDefaultFilename()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyCamoFileName", getAllyCamouflage().getFilename());
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyColour", getAllyColour().name());

        if (!getEnemyCamouflage().hasDefaultCategory()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCamoCategory", getEnemyCamouflage().getCategory());
        }

        if (!getEnemyCamouflage().hasDefaultFilename()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCamoFileName", getEnemyCamouflage().getFilename());
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyColour", getEnemyColour().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "requiredCombatTeams", getRequiredCombatTeams());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "requiredCombatElements", getRequiredCombatElements());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "moraleLevel", getMoraleLevel().name());

        if (getRoutEndDate() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routEnd", getRoutEndDate());
        }

        if (getRoutedPayout() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routedPayout", getRoutedPayout());
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partsAvailabilityLevel", getPartsAvailabilityLevel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "extensionLength", extensionLength);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sharesPct", getSharesPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "batchallAccepted", isBatchallAccepted());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerMinorBreaches", playerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerMinorBreaches", employerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractScoreArbitraryModifier", contractScoreArbitraryModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "priorLogisticsFailure", priorLogisticsFailure);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleTypeMod", battleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nextWeekBattleTypeMod", nextWeekBattleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandRoll", getContractNegotiationCommandRoll());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvageRoll", getContractNegotiationSalvageRoll());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportRoll", getContractNegotiationSupportRoll());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportRoll", getContractNegotiationTransportRoll());

        if (parentContract != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "parentContractId", parentContract.getId());
        }

        if (specialEventScenarioDate != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioDate", specialEventScenarioDate);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioType", specialEventScenarioType);
        }

        if (getStratConCampaignState() != null) {
            getStratConCampaignState().Serialize(pw);
        }

        if (getEmployerLiaison() != null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "employerLiaison");
            getEmployerLiaison().writeToXMLHeadless(pw, indent, campaign);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "employerLiaison");
        }

        if (getClanOpponent() != null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "clanOpponent");
            getClanOpponent().writeToXMLHeadless(pw, indent, campaign);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "clanOpponent");
        }

        return indent;
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node node) throws ParseException {
        super.loadFieldsFromXmlNode(campaign, version, node);
        NodeList childNodes = node.getChildNodes();

        for (int x = 0; x < childNodes.getLength(); x++) {
            Node item = childNodes.item(x);

            try {
                if (item.getNodeName().equalsIgnoreCase("employerCode")) {
                    setEmployerCode(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("enemyCode")) {
                    setEnemyCode(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("enemyMercenaryEmployerCode")) {
                    setEnemyMercenaryEmployerCode(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("contractType")) {
                    setContractType(AtBContractType.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("allySkill")) {
                    setAllySkill(parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("allyQuality")) {
                    setAllyQuality(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("enemySkill")) {
                    setEnemySkill(parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("enemyQuality")) {
                    setEnemyQuality(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("difficulty")) {
                    difficulty = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("allyBotName")) {
                    setAllyBotName(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("enemyBotName")) {
                    setEnemyBotName(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("allyCamoCategory")) {
                    getAllyCamouflage().setCategory(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("allyCamoFileName")) {
                    getAllyCamouflage().setFilename(item.getTextContent().trim());
                } else if (item.getTextContent().equalsIgnoreCase("allyColour")) {
                    setAllyColour(PlayerColour.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("enemyCamoCategory")) {
                    getEnemyCamouflage().setCategory(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("enemyCamoFileName")) {
                    getEnemyCamouflage().setFilename(item.getTextContent().trim());
                } else if (item.getTextContent().equalsIgnoreCase("enemyColour")) {
                    setEnemyColour(PlayerColour.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("requiredCombatTeams")) {
                    setRequiredCombatTeams(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("requiredCombatElements")) {
                    setRequiredCombatElements(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("moraleLevel")) {
                    setMoraleLevel(AtBMoraleLevel.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("routEnd")) {
                    setRoutEndDate(MHQXMLUtility.parseDate(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("routedPayout")) {
                    String cleanValue = item.getTextContent().trim().replaceAll("[^0-9.]", "");
                    double value = Double.parseDouble(cleanValue);
                    setRoutedPayout(Money.of(value));
                } else if (item.getNodeName().equalsIgnoreCase("partsAvailabilityLevel")) {
                    setPartsAvailabilityLevel(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("extensionLength")) {
                    extensionLength = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("sharesPct")) {
                    setSharesPercent(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("batchallAccepted")) {
                    setBatchallAccepted(Boolean.parseBoolean(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("playerMinorBreaches")) {
                    playerMinorBreaches = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("employerMinorBreaches")) {
                    employerMinorBreaches = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("contractScoreArbitraryModifier")) {
                    contractScoreArbitraryModifier = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("priorLogisticsFailure")) {
                    priorLogisticsFailure = Boolean.parseBoolean(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("battleTypeMod")) {
                    battleTypeMod = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("nextWeekBattleTypeMod")) {
                    nextWeekBattleTypeMod = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("commandRoll")) {
                    setContractNegotiationCommandRoll(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("salvageRoll")) {
                    setContractNegotiationSalvageRoll(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("supportRoll")) {
                    setContractNegotiationSupportRoll(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("transportRoll")) {
                    setContractNegotiationTransportRoll(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioDate")) {
                    specialEventScenarioDate = MHQXMLUtility.parseDate(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioType")) {
                    specialEventScenarioType = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase(StratConCampaignState.ROOT_XML_ELEMENT_NAME)) {
                    setStratConCampaignState(StratConCampaignState.Deserialize(item));
                    getStratConCampaignState().setContract(this);
                    this.setStratConCampaignState(getStratConCampaignState());
                } else if (item.getNodeName().equalsIgnoreCase("parentContractId")) {
                    parentContract = new AtBContractRef(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("employerLiaison")) {
                    setEmployerLiaison(Person.generateInstanceFromXML(item, campaign, version));
                } else if (item.getNodeName().equalsIgnoreCase("clanOpponent")) {
                    setClanOpponent(Person.generateInstanceFromXML(item, campaign, version));
                }
            } catch (Exception e) {
                logger.error("", e);
            }

            if (getEmployerLiaison() == null) {
                createEmployerLiaison(campaign);
            }

            if (getClanOpponent() == null && getEnemy().isClan()) {
                createClanOpponent(campaign);
            }
        }
    }

    /**
     * Restores any references to other contracts.
     *
     * @param c The Campaign which holds this contract.
     */
    public void restore(Campaign c) {
        if (parentContract != null) {
            Mission m = c.getMission(parentContract.getId());
            if (m != null) {
                if (m instanceof AtBContract) {
                    setParentContract((AtBContract) m);
                } else {
                    logger.warn("Parent Contract reference #{} is not an AtBContract for contract {}",
                          parentContract.getId(),
                          getName());
                    setParentContract(null);
                }
            } else {
                logger.warn("Parent Contract #{} reference was not found for contract {}",
                      parentContract.getId(),
                      getName());
                setParentContract(null);
            }
        }
    }

    public Faction getEmployerFaction() {
        return Factions.getInstance().getFaction(getEmployerCode());
    }

    public void updateEmployer(String code, int year) {
        this.setEmployerCode(code);
        setEmployerName(getEmployerName(year));
        setAllyCamouflage(pickRandomCamouflage(year, getEmployerCode()));
    }

    public String getEmployerName(int year) {
        return isMercSubcontract() ?
                     "Mercenary (" + getEmployerFaction().getFullName(year) + ')' :
                     getEmployerFaction().getFullName(year);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void addPlayerMinorBreach() {
        playerMinorBreaches++;
    }

    public void addPlayerMinorBreaches(int num) {
        playerMinorBreaches += num;
    }

    public void setContractScoreArbitraryModifier(int newModifier) {
        contractScoreArbitraryModifier = newModifier;
    }

    public int getBattleTypeMod() {
        return battleTypeMod + nextWeekBattleTypeMod;
    }

    @Override
    public void acceptContract(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseStratCon()) {
            StratConContractDefinition stratconContractDefinition = getContractDefinition(getContractType());
            if (stratconContractDefinition != null) {
                StratConContractInitializer.initializeCampaignState(this, campaign, stratconContractDefinition);
            }
        }
    }

    public AtBContract(Contract contract, Campaign campaign) {
        this(contract.getName());

        setContractTypeName(contract.getContractTypeName());
        setSystemId(contract.getSystemId());
        setDescription(contract.getDescription());
        setStatus(contract.getStatus());
        for (Scenario s : contract.getScenarios()) {
            addScenario(s);
        }
        setId(contract.getId());
        setLengthInMonths(contract.getLengthInMonths());
        setStartDate(contract.getStartDate());
        /*
         * Set ending date; the other calculated values will be replaced
         * from the original contract
         */
        calculateContract(campaign);
        setPaymentMultiplier(contract.getPaymentMultiplier());
        setTransportCompensation(contract.getTransportCompensation());
        setStraightSupport(contract.getStraightSupport());
        setOverheadCompensation(contract.getOverheadCompensation());
        setCommandRights(contract.getCommandRights());
        setBattleLossCompensation(contract.getBattleLossCompensation());
        setSalvagePercent(contract.getSalvagePercent());
        setSalvageExchange(contract.isSalvageExchange());
        setSalvagedByUnit(contract.getSalvagedByUnit());
        setSalvagedByEmployer(contract.getSalvagedByEmployer());
        setSigningBonus(contract.getSigningBonus());
        setAdvancePercent(contract.getAdvancePercent());
        setPaidMRBCFee(contract.isPaidMRBCFee());
        setAdvanceAmount(contract.getAdvanceAmount());
        setFeeAmount(contract.getFeeAmount());
        setBaseAmount(contract.getBaseAmount());
        setOverheadAmount(contract.getOverheadAmount());
        setSupportAmount(contract.getSupportAmount());
        setTransportAmount(contract.getTransportAmount());
        setSigningBonusAmount(contract.getSigningBonusAmount());

        /* Guess at AtBContract values */
        AtBContractType contractType = getAtBContractType(contract);
        setContractType(contractType);

        Faction f = Factions.getInstance()
                          .getFactionFromFullNameAndYear(contract.getEmployerName(), campaign.getGameYear());
        if (null == f) {
            setEmployerCode("IND");
        } else {
            setEmployerCode(f.getShortName());
        }

        if (getContractType().isPirateHunting()) {
            Faction employer = getEmployerFaction();
            setEnemyCode(employer.isClan() ? "BAN" : PIRATE_FACTION_CODE);
        } else if (getContractType().isRiotDuty()) {
            setEnemyCode("REB");
        }

        setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              contractType.isCadreDuty(), true, 1.0));
        setRequiredCombatElements(ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign));

        setPartsAvailabilityLevel(getContractType().calculatePartsAvailabilityLevel());

        int currentYear = campaign.getGameYear();
        setAllyBotName(getEmployerName(currentYear));
        setAllyCamouflage(pickRandomCamouflage(currentYear, getEmployerCode()));

        setEnemyBotName(generateEnemyName(currentYear));
        setEnemyCamouflage(pickRandomCamouflage(currentYear, getEnemyCode()));

        difficulty = calculateContractDifficulty(contract.getStartDate().getYear(),
              true,
              campaign.getAllCombatEntities());

        clanTechSalvageOverride();
    }

    private static AtBContractType getAtBContractType(Contract contract) {
        AtBContractType contractType = null;
        for (final AtBContractType type : AtBContractType.values()) {
            if (type.toString().equalsIgnoreCase(contract.getContractTypeName())) {
                contractType = type;
                break;
            }
        }
        /* Make a rough guess */
        if (contractType == null) {
            if (contract.getLengthInMonths() <= 3) {
                contractType = AtBContractType.OBJECTIVE_RAID;
            } else if (contract.getLengthInMonths() < 12) {
                contractType = AtBContractType.GARRISON_DUTY;
            } else {
                contractType = AtBContractType.PLANETARY_ASSAULT;
            }
        }
        return contractType;
    }

    /**
     * Applies a salvage override rule for Clan technology based on the contract timeline and faction involvement. This
     * method checks the factions of both the enemy and employer and determines if a salvage exchange should be forced
     * based on whether the battle occurs before the Battle of Tukayyid.
     *
     * <p>
     * This rule was implemented to better match canon employer behavior during this period.
     * </p>
     */
    public void clanTechSalvageOverride() {
        if (getEnemy().isClan() && !getEmployerFaction().isClan()) {
            if (getStartDate().isBefore(BATTLE_OF_TUKAYYID)) {
                setSalvageExchange(true);
            }
        }
    }

    /**
     * Represents a reference to another AtBContract.
     */
    protected static class AtBContractRef extends AtBContract {
        public AtBContractRef(int id) {
            setId(id);
        }
    }

    /**
     * This method returns a {@link JPanel} that represents the difficulty skulls for a given mission.
     *
     * @return a {@link JPanel} with the difficulty skulls displayed
     */
    public JPanel getContractDifficultySkulls() {
        final int ERROR = -99;

        // Create a new JFrame
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a pane with FlowLayout
        JPanel panel = new JPanel(new FlowLayout());

        // Load and scale the images
        ImageIcon skullFull = scaleImageIcon(new ImageIcon("data/images/misc/challenge_estimate_full.png"), 50, true);
        ImageIcon skullHalf = scaleImageIcon(new ImageIcon("data/images/misc/challenge_estimate_half.png"), 50, true);

        int iterations = difficulty;

        if (difficulty == ERROR) {
            iterations = 5;
        }

        if (iterations % 2 != 0) {
            iterations--;
            iterations /= 2;

            for (int i = 0; i < iterations; i++) {
                panel.add(new JLabel(skullFull));
            }

            panel.add(new JLabel(skullHalf));
        } else {
            iterations /= 2;

            for (int i = 0; i < iterations; i++) {
                panel.add(new JLabel(skullFull));
            }
        }

        return panel;
    }

    /**
     * Creates and returns a {@link JPanel} containing the belligerent factions' logos for the specified game year.
     *
     * <p>This panel displays the employer and enemy faction logos side by side, separated by a styled divider.
     * The logos are determined based on the provided game year and faction codes, scaled appropriately for the
     * GUI.</p>
     *
     * @param gameYear        the year used to determine which faction logos to display
     * @param employerTooltip the tooltip to show on the employer (left) logo, or {@code null} for none
     * @param enemyTooltip    the tooltip to show on the enemy (right) logo, or {@code null} for none
     *
     * @return a {@link JPanel} with the employer and enemy faction logos, with a divider in between
     *
     * @author Illiani
     * @since 0.50.06
     */
    public JPanel getBelligerentsPanel(int gameYear, @Nullable String employerTooltip, @Nullable String enemyTooltip) {
        final int SIZE = 64;

        String employer = getEmployerCode();
        ImageIcon employerImage = getFactionLogo(gameYear, employer);
        employerImage = scaleImageIcon(employerImage, SIZE, true);
        JLabel employerLabel = new JLabel(employerImage);
        employerLabel.setToolTipText(employerTooltip);

        JLabel divider = new JLabel("/");
        divider.setHorizontalAlignment(SwingConstants.CENTER);
        int fontSize = scaleForGUI(SIZE); // scaleImageIcon already includes the necessary scaling
        divider.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        divider.setForeground(new Color(0, 0, 0, 128));

        String enemy = getEnemyCode();
        ImageIcon enemyImage = getFactionLogo(gameYear, enemy);
        enemyImage = scaleImageIcon(enemyImage, SIZE, true);
        JLabel enemyLabel = new JLabel(enemyImage);
        enemyLabel.setToolTipText(enemyTooltip);

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(employerLabel);
        panel.add(divider);
        panel.add(enemyLabel);

        return panel;
    }

    /**
     * Calculates the difficulty rating of a contract by comparing the estimated combat strength of the opposing force
     * to the combat strength of the player's participating units.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *     <li>Determines the opposing force's effective skill level by applying any faction-based adjustments to
     *     their base {@link SkillLevel}.</li>
     *     <li>Computes a skill multiplier and applies it to the estimated enemy force power, derived from the game
     *     year, force quality, and whether generic BV values are used.</li>
     *     <li>Estimates the total combat power of the player's units based on their BV values and optionally using
     *     generic BV rules.</li>
     *     <li>Computes the percentage difference between enemy and player power.</li>
     *     <li>Maps that percentage difference into a difficulty scale ranging from {@code 1} (easiest) to {@code 10}
     *     (hardest), centered around {@code 5} as an even match.</li>
     * </ol>
     *
     * <p>A negative percentage difference indicates that the player is stronger than the opposing force; a positive
     * difference indicates the enemy is stronger. Each 20% shift away from parity increases (or decreases)
     * difficulty by one step.</p>
     *
     * <p>If enemy combat strength cannot be computed, the method returns {@code -99} to signal an error.</p>
     *
     * @param gameYear          the current in-game year used for estimating enemy technology and BV baselines
     * @param useGenericBV      whether generic BV values should be used instead of unit-specific BV calculations
     * @param playerCombatUnits the list of player {@link Entity} objects expected to participate in the contract
     *
     * @return a difficulty rating from {@code 1} to {@code 10}, where {@code 5} represents roughly even forces; or
     *       {@code -99} if the enemy power estimation fails
     */
    public int calculateContractDifficulty(int gameYear, boolean useGenericBV, List<Entity> playerCombatUnits) {
        final int ERROR = -99;

        // Estimate the power of the enemy forces
        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(getEnemyCode(), getEnemySkill());
        double enemySkillMultiplier = getSkillMultiplier(opposingSkill);
        double enemyPower = estimateMekStrength(gameYear, useGenericBV, getEnemyCode(), getEnemyQuality());

        // If we cannot calculate enemy power, abort.
        if (enemyPower == 0) {
            return ERROR;
        }

        enemyPower = (int) round(enemyPower * enemySkillMultiplier);

        // Estimate player power
        double playerPower = estimatePlayerPower(playerCombatUnits, useGenericBV);

        // Calculate difficulty based on the percentage difference between the two forces.
        double difference = enemyPower - playerPower;
        // Divide by 0 protection
        double percentDifference = (playerPower != 0 ? (difference / playerPower) : difference) * 100;

        int mappedValue = (int) round(Math.abs(percentDifference) / 20);
        if (percentDifference < 0) {
            mappedValue = 5 - mappedValue;
        } else {
            mappedValue = 5 + mappedValue;
        }

        return Math.clamp(mappedValue, 1, 10);
    }

    /**
     * Modifies the skill level based on the faction code.
     *
     * @param factionCode the code of the faction
     * @param skillLevel  the original skill level
     *
     * @return the modified skill level
     */
    SkillLevel modifySkillLevelBasedOnFaction(String factionCode, SkillLevel skillLevel) {
        if (Objects.equals(factionCode, "SOC")) {
            return ELITE;
        }

        if (Factions.getInstance().getFaction(factionCode).isClan()) {
            return parseFromInteger(skillLevel.ordinal() + 1);
        }

        return skillLevel;
    }

    double estimatePlayerPower(List<Entity> units, boolean useGenericBV) {
        int playerPower = 0;
        int playerGBV = 0;
        int playerUnitCount = 0;
        for (Entity unit : units) {
            playerPower += unit.calculateBattleValue();
            playerGBV += unit.getGenericBattleValue();
            playerUnitCount++;
        }

        if (useGenericBV) {
            return ((double) playerPower) / playerGBV;
        } else {
            return ((double) playerPower) / playerUnitCount;
        }
    }

    /**
     * Returns the skill BV multiplier based on the given skill level.
     *
     * @param skillLevel the skill level to determine the multiplier
     *
     * @return the skill multiplier
     */
    private static double getSkillMultiplier(SkillLevel skillLevel) {
        return switch (skillLevel) {
            case NONE -> 0.68;
            case ULTRA_GREEN -> 0.77;
            case GREEN -> 0.86;
            case REGULAR -> 1.00;
            case VETERAN -> 1.32;
            case ELITE -> 1.68;
            case HEROIC -> 2.02;
            case LEGENDARY -> 2.31;
        };
    }

    /**
     * Estimates the relative strength for Mek units of a specific faction and quality. Excludes salvage.
     *
     * @param gameYear     the year of the current campaign
     * @param useGenericBV whether to use generic BV for strength calculations
     * @param factionCode  the code of the faction to estimate the average Mek strength for
     * @param quality      the quality of the Meks to calculate the average strength for
     *
     * @return the average battle value OR total BV2 divided by total GBV for Meks of the specified faction and quality
     *       OR 0 on error
     */
    double estimateMekStrength(int gameYear, boolean useGenericBV, String factionCode, int quality) {
        final double ERROR = 0;

        RATGenerator ratGenerator = Factions.getInstance().getRATGenerator();
        FactionRecord faction = ratGenerator.getFaction(factionCode);

        if (faction == null) {
            return ERROR;
        }

        UnitTable unitTable;
        try {
            unitTable = findTable(faction,
                  MEK,
                  gameYear,
                  String.valueOf(quality),
                  new ArrayList<>(),
                  NETWORK_NONE,
                  new ArrayList<>(),
                  new ArrayList<>(),
                  new ArrayList<>(),
                  0,
                  faction);
        } catch (Exception ignored) {
            return ERROR;
        }

        // Otherwise, calculate the estimated power of the faction
        int entries = unitTable.getNumEntries();

        int totalBattleValue = 0;
        int totalGBV = 0;
        int rollingCount = 0;

        for (int i = 0; i < entries; i++) {
            int battleValue = unitTable.getBV(i); // 0 for salvage
            if (0 == battleValue) {
                // Removing this check will break things, see the other comments.
                continue;
            }

            // getMekSummary(int index) is NULL for salvage.
            int genericBattleValue = unitTable.getMekSummary(i).getGenericBattleValue();
            int weight = unitTable.getEntryWeight(i); // NOT 0 for salvage

            totalBattleValue += battleValue * weight;
            totalGBV += genericBattleValue * weight;
            rollingCount += weight;
        }

        if (useGenericBV) {
            return ((double) totalBattleValue) / totalGBV;
        } else {
            return ((double) totalBattleValue) / rollingCount;
        }
    }

    /**
     * Calculates the number of required Victory Points (VP) needed to achieve overall success for this StratCon
     * contract.
     *
     * <p>The calculation is based on several averaged campaign parameters:
     * <ul>
     *     <li><b>Base requirement</b> — Required number of combat teams multiplied by the contract length.</li>
     *     <li><b>Scenario odds</b> — The mean scenario-odds percentage across all StratCon tracks, converted to a
     *     probability.</li>
     *     <li><b>Turning point chance</b> — A scaling factor based on command rights: {@code INTEGRATED} contracts
     *     assume a 100% chance, while all others use a one-third chance.</li>
     * </ul>
     *
     * <p>The final result estimates the expected number of Turning Points the player must win for overall contract
     * success. If the player loses a handful of Turning Points, they should still be able to win the contract by
     * being proactive in the Area of Operations.</p>
     *
     * @return the required number of Victory Points, rounded up to the nearest integer
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getRequiredVictoryPoints() {
        if (getStratConCampaignState() == null) {
            return 0;
        }

        double baseRequirement = getRequiredCombatTeams();

        int duration = getLengthInMonths();
        if (getContractType().isGarrisonType()) {
            duration = (int) ceil(duration * 0.75); // We assume around 25% of the contract will be peaceful
        }

        double trackCount = 0;
        int totalScenarioOdds = 0;
        for (StratConTrackState trackState : getStratConCampaignState().getTracks()) {
            trackCount++;
            totalScenarioOdds += trackState.getScenarioOdds();
        }

        double meanScenarioOdds = totalScenarioOdds / trackCount;
        double scenarioOdds = meanScenarioOdds / 100.0;
        double turningPointChance = (getCommandRights() == ContractCommandRights.INTEGRATED ? 1.0 : 0.33);

        // This result gives us the average number of Turning Points expected for the contract
        return (int) ceil(baseRequirement * duration * scenarioOdds * turningPointChance);
    }
}
