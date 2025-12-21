/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static megamek.common.enums.SkillLevel.parseFromString;
import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.force.CombatTeam.getStandardForceSize;
import static mekhq.campaign.force.FormationLevel.BATTALION;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.MAXIMUM_MORALE_LEVEL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.MINIMUM_MORALE_LEVEL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.FREE;
import static mekhq.campaign.stratCon.StratConContractDefinition.getContractDefinition;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.campaign.universe.factionStanding.BatchallFactions.BATCHALL_FACTIONS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
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
import java.util.ResourceBundle;
import java.util.stream.Stream;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
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
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
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
import mekhq.campaign.universe.factionStanding.BatchallFactions;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.factionStanding.PerformBatchall;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
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

    /** The minimum intensity below which no scenarios will be generated */
    public static final double MINIMUM_INTENSITY = 0.01;

    /* null unless subcontract */
    protected AtBContract parentContract;
    /* hired by another mercenary unit on contract to a third-party employer */ boolean mercSubcontract;

    protected Person employerLiaison;
    protected Person clanOpponent;
    protected String employerCode;
    protected String enemyCode;
    protected String enemyMercenaryEmployerCode;
    protected String enemyName;

    protected int difficulty;

    protected AtBContractType contractType;
    protected SkillLevel allySkill;
    protected int allyQuality;
    protected SkillLevel enemySkill;
    protected int enemyQuality;
    protected String allyBotName;
    protected String enemyBotName;
    protected Camouflage allyCamouflage;
    protected PlayerColour allyColour;
    protected Camouflage enemyCamouflage;
    protected PlayerColour enemyColour;

    protected int extensionLength;

    protected int requiredCombatTeams;
    protected int requiredCombatElements;
    protected AtBMoraleLevel moraleLevel;
    protected LocalDate routEnd;
    protected int partsAvailabilityLevel;
    protected int sharesPct;
    private boolean batchallAccepted;

    protected int playerMinorBreaches;
    protected int employerMinorBreaches;
    protected int contractScoreArbitraryModifier;

    protected int moraleMod = 0;
    private Money routedPayout = null;

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

    private StratConCampaignState stratconCampaignState;
    private boolean isAttacker;


    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBContract";
    @Deprecated(since = "0.50.10")
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBContract",
          MekHQ.getMHQOptions().getLocale());

    private int commandRoll;
    private int salvageRoll;
    private int supportRoll;
    private int transportRoll;

    protected AtBContract() {
        this(null);
    }

    /**
     * Sets the end date of the rout. This should only be applied on contracts whose morale equals ROUTED
     *
     * @param routEnd the {@code LocalDate} representing the end date of the rout
     */
    public void setRoutEndDate(LocalDate routEnd) {
        this.routEnd = routEnd;
    }

    public AtBContract(String name) {
        super(name, "Independent");
        employerLiaison = null;
        clanOpponent = null;
        employerCode = "IND";
        enemyCode = "IND";
        enemyMercenaryEmployerCode = null;
        enemyName = "Independent";

        difficulty = Integer.MIN_VALUE;

        parentContract = null;
        mercSubcontract = false;
        isAttacker = false;

        setContractType(AtBContractType.GARRISON_DUTY);
        setAllySkill(REGULAR);
        allyQuality = DragoonRating.DRAGOON_C.getRating();
        setEnemySkill(REGULAR);
        enemyQuality = DragoonRating.DRAGOON_C.getRating();
        allyBotName = "Ally";
        enemyBotName = "Enemy";
        setAllyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.RED.name()));
        setAllyColour(PlayerColour.RED);
        setEnemyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.GREEN.name()));
        setEnemyColour(PlayerColour.GREEN);

        extensionLength = 0;

        sharesPct = 0;
        batchallAccepted = true;
        setMoraleLevel(STALEMATE);
        routEnd = null;
        priorLogisticsFailure = false;
        specialEventScenarioDate = null;
        battleTypeMod = 0;
        nextWeekBattleTypeMod = 0;
    }

    public void initContractDetails(Campaign campaign) {
        int companySize = getStandardForceSize(campaign.getFaction(), COMPANY.getDepth());
        int battalionSize = getStandardForceSize(campaign.getFaction(), BATTALION.getDepth());

        if (ContractUtilities.getEffectiveNumUnits(campaign) <= companySize) {
            setOverheadComp(OH_FULL);
        } else if (ContractUtilities.getEffectiveNumUnits(campaign) <= battalionSize) {
            setOverheadComp(OH_HALF);
        } else {
            setOverheadComp(OH_NONE);
        }

        int currentYear = campaign.getGameYear();
        allyBotName = getEmployerName(currentYear);
        allyCamouflage = pickRandomCamouflage(currentYear, employerCode);

        enemyBotName = getEnemyName(currentYear);
        enemyCamouflage = pickRandomCamouflage(currentYear, enemyCode);
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
            fileCategory = fileCategory.replaceAll(ROOT_DIRECTORY, "");

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
        setLength(getContractType().calculateLength(variable));
    }

    /**
     * @param campaign The campaign to reference.
     *
     * @return The number of lances required.
     *
     * @deprecated use {@link ContractUtilities#calculateBaseNumberOfRequiredLances(Campaign, boolean, boolean, double)}
     *       <p>
     *       Calculates the number of lances required for this contract, based on [campaign].
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static int calculateBaseNumberOfRequiredLances(Campaign campaign) {
        return ContractUtilities.calculateBaseNumberOfRequiredLances(campaign, false, true, 1.0);
    }

    /**
     * @param campaign the campaign containing the combat teams and units to evaluate
     *
     * @return the effective number of units as an integer
     *
     * @deprecated use {@link ContractUtilities#getEffectiveNumUnits(Campaign)}
     *       <p>
     *       Calculates the effective number of units available in the given campaign based on unit types and roles.
     *
     *       <p>
     *       This method iterates through all combat teams in the specified campaign, ignoring combat teams with the
     *       auxiliary role. For each valid combat team, it retrieves the associated force and evaluates all units
     *       within that force. The unit contribution to the total is determined based on its type:
     *       <ul>
     *       <li><b>TANK, VTOL, NAVAL, CONV_FIGHTER, AEROSPACE_FIGHTER:</b> Adds 1 for
     *       non-clan factions,
     *       and 0.5 for clan factions.</li>
     *       <li><b>PROTOMEK:</b> Adds 0.2 to the total.</li>
     *       <li><b>BATTLE_ARMOR, INFANTRY:</b> Adds 0 (excluded from the total).</li>
     *       <li><b>Other types:</b> Adds 1 to the total.</li>
     *       </ul>
     *
     *       <p>
     *       Units that aren’t associated with a valid combat team or can’t be fetched due
     *       to missing
     *       data are ignored. The final result is returned as an integer by flooring the
     *       calculated total.
     *       </p>
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static int getEffectiveNumUnits(Campaign campaign) {
        return ContractUtilities.getEffectiveNumUnits(campaign);
    }

    /**
     * Checks and updates the morale which depends on various conditions such as the rout end date, skill levels,
     * victories, defeats, etc. This method also updates the enemy status based on the morale level.
     *
     * @param today The current date in the context.
     */
    public void checkMorale(Campaign campaign, LocalDate today) {

        // If there is a rout end date, and it's past today, update morale and enemy state accordingly
        if (routEnd != null) {
            // Check whether any current rout continues beyond its expected date. This is only applicable for
            // Garrison Type contracts. For all other types we reinforce immediately
            boolean routContinue = contractType.isGarrisonType() && randomInt(4) == 0;
            if (routContinue) {
                return;
            }

            if (today.isAfter(routEnd)) {
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
                if (stratconCampaignState != null) {
                    StratConContractDefinition contractDefinition = getContractDefinition(getContractType());

                    if (contractDefinition != null) {
                        for (StratConTrackState trackState : stratconCampaignState.getTracks()) {
                            int scenarioOdds = StratConContractInitializer.getScenarioOdds(contractDefinition);

                            trackState.setScenarioOdds(scenarioOdds);
                        }
                    }
                }

                moraleLevel = newMoraleLevel;
                routEnd = null;

                String key = "routEnded.reinforcements";
                if (contractType.isGarrisonDuty() || contractType.isRetainer()) {
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
                                  + moraleLevel.getToolTipText();
        new ImmersiveDialogNotification(campaign, flavorText, moraleReport, true);

        MHQMorale.routedMoraleUpdate(campaign, this);

        // Reset external morale modifier
        moraleMod = 0;
    }


    /**
     * Updates the enemy faction and enemy bot name for this contract.
     *
     * @param campaign The current campaign.
     * @param today    The current LocalDate object.
     */
    private void updateEnemy(Campaign campaign, LocalDate today) {
        String enemyCode = RandomFactionGenerator.getInstance()
                                 .getEnemy(Factions.getInstance().getFaction(employerCode), false, true);
        setEnemyCode(enemyCode);

        Faction enemyFaction = Factions.getInstance().getFaction(enemyCode);
        setEnemyBotName(enemyFaction.getFullName(today.getYear()));
        enemyName = ""; // wipe the old enemy name
        getEnemyName(today.getYear()); // we use this to update enemyName
        if (enemyFaction.isClan()) {
            createClanOpponent(campaign);
        }

        // We have a check in getEnemyName that prevents rolling over mercenary names, so we add this extra step to
        // force a mercenary name re-roll, in the event one Mercenary faction is replaced with another.
        if (Factions.getInstance().getFaction(enemyCode).isMercenary()) {
            enemyBotName = BackgroundsController.randomMercenaryCompanyNameGenerator(null);
        }

        allyCamouflage = pickRandomCamouflage(today.getYear(), employerCode);
        enemyCamouflage = pickRandomCamouflage(today.getYear(), enemyCode);

        // Update the Batchall information
        batchallAccepted = true;
        Faction faction = getEnemy();
        if (campaign.getCampaignOptions().isUseGenericBattleValue() && faction.performsBatchalls()) {
            boolean tracksStanding = campaign.getCampaignOptions().isTrackFactionStanding();
            FactionStandings factionStandings = campaign.getFactionStandings();

            boolean allowBatchalls = true;
            if (campaign.getCampaignOptions().isUseFactionStandingBatchallRestrictionsSafe()) {
                double regard = factionStandings.getRegardForFaction(faction.getShortName(), true);
                allowBatchalls = FactionStandingUtilities.isBatchallAllowed(regard);
            }

            double regardMultiplier = campaign.getCampaignOptions().getRegardMultiplier();
            String campaignFactionCode = campaign.getFaction().getShortName();
            if (faction.performsBatchalls() && allowBatchalls) {
                PerformBatchall batchallDialog = new PerformBatchall(campaign, clanOpponent, enemyCode);

                batchallAccepted = batchallDialog.isBatchallAccepted();

                if (!batchallAccepted && tracksStanding) {
                    List<String> reports = factionStandings.processRefusedBatchall(campaignFactionCode, enemyCode,
                          today.getYear(), regardMultiplier);

                    for (String report : reports) {
                        campaign.addReport(GENERAL, report);
                    }
                }
            }

            if (tracksStanding) {
                // Whenever we dynamically change the enemy faction, we update standing accordingly
                String report = factionStandings.processContractAccept(campaignFactionCode, faction, today,
                      regardMultiplier, getLength());
                if (report != null) {
                    campaign.addReport(GENERAL, report);
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
                int oldSalvagePercent = getSalvagePct();
                int newSalvagePercent = (int) max(100, round(oldSalvagePercent * 1.25));

                boolean isAlreadyMax = oldSalvagePercent >= 100;

                setSalvageExchange(true);
                setSalvagePct(newSalvagePercent);

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
        if (!isUseMaplessMode && stratconCampaignState != null) {
            return stratconCampaignState.getVictoryPoints();
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
        if (stratconCampaignState == null) {
            return 0;
        }

        return stratconCampaignState.getSupportPoints();
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
                    new RoninOffer(campaign, stratconCampaignState, requiredCombatTeams);
                }
                yield false;
            }
            case 2 -> {
                campaign.addReport(GENERAL, "Bonus: Ronin");
                new RoninOffer(campaign, stratconCampaignState, requiredCombatTeams);
                yield false;
            }
            case 3 -> { // Resupply
                if (campaignOptions.isUseAtB() && !campaignOptions.isUseStratCon()) {
                    campaign.addReport(GENERAL, "Bonus: Ronin");
                    new RoninOffer(campaign, stratconCampaignState, requiredCombatTeams);
                    yield false;
                } else {
                    if (isPostScenario) {
                        yield true;
                    } else {
                        campaign.addReport(GENERAL, "Bonus: Support Point");
                        stratconCampaignState.changeSupportPoints(1);
                        yield false;
                    }
                }
            }
            case 4 -> {
                new MercenaryAuction(campaign, requiredCombatTeams, stratconCampaignState, TANK);
                yield false;
            }
            case 5 -> {
                new MercenaryAuction(campaign, requiredCombatTeams, stratconCampaignState, AEROSPACE_FIGHTER);
                yield false;
            }
            case 6 -> {
                new MercenaryAuction(campaign, requiredCombatTeams, stratconCampaignState, MEK);
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

    public boolean isAttacker() {
        return isAttacker;
    }

    public void setAttacker(boolean isAttacker) {
        this.isAttacker = isAttacker;
    }

    public void checkEvents(Campaign campaign) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        boolean isUseStratCon = campaign.getCampaignOptions().isUseStratCon();

        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                partsAvailabilityLevel++;
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
                            partsAvailabilityLevel--;
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
                    partsAvailabilityLevel--;
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
                                StratConCampaignState campaignState = getStratconCampaignState();

                                if (campaignState != null) {
                                    text += ": -1 Support Point";
                                    campaignState.changeSupportPoints(-1);
                                }
                            }
                            break;
                        case 3:
                            text += "ComStar Interdict: Base availability level decreases one level for the rest of the contract.";
                            partsAvailabilityLevel--;
                            break;
                        case 4:
                            text += "Defectors: Next Enemy Morale roll gets a -1 modifier.";
                            moraleMod--;
                            break;
                        case 5:
                            text += "Free Trader: Base availability level increases one level for the rest of the contract.";
                            partsAvailabilityLevel++;
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
            extension = max(1, getLength() / 2);
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
        setEndDate(getEndingDate().plusMonths(extension));
        extensionLength += extension;

        // We spike morale to create a jump in contract difficulty - essentially the reason why the employer is using
        // the emergency clause.
        int moraleOrdinal = moraleLevel.ordinal();
        roll = d6(2) / 2;

        // we need to reset routEnd to null otherwise we'll attempt to rally
        if (routEnd != null) {
            routEnd = null;
        }

        moraleOrdinal = min(moraleOrdinal + roll, OVERWHELMING.ordinal());
        moraleLevel = AtBMoraleLevel.values()[moraleOrdinal];

        campaign.addReport(GENERAL, moraleLevel.getToolTipText());

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

        if (getLength() <= 0) {
            return Money.zero();
        }

        return getBaseAmount().multipliedBy(1.5)
                     .plus(getSupportAmount())
                     .plus(getOverheadAmount())
                     .dividedBy(getLength());
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(campaign, pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerCode", getEmployerCode());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCode", getEnemyCode());

        if (enemyMercenaryEmployerCode != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyMercenaryEmployerCode", enemyMercenaryEmployerCode);
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

        if (routEnd != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routEnd", routEnd);
        }

        if (routedPayout != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routedPayout", routedPayout);
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partsAvailabilityLevel", getPartsAvailabilityLevel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "extensionLength", extensionLength);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sharesPct", sharesPct);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "batchallAccepted", batchallAccepted);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerMinorBreaches", playerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerMinorBreaches", employerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractScoreArbitraryModifier", contractScoreArbitraryModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "priorLogisticsFailure", priorLogisticsFailure);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleTypeMod", battleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nextWeekBattleTypeMod", nextWeekBattleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandRoll", commandRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvageRoll", salvageRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportRoll", supportRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportRoll", transportRoll);

        if (parentContract != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "parentContractId", parentContract.getId());
        }

        if (specialEventScenarioDate != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioDate", specialEventScenarioDate);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioType", specialEventScenarioType);
        }

        if (stratconCampaignState != null) {
            stratconCampaignState.Serialize(pw);
        }

        if (employerLiaison != null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "employerLiaison");
            employerLiaison.writeToXMLHeadless(pw, indent, campaign);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "employerLiaison");
        }

        if (clanOpponent != null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "clanOpponent");
            clanOpponent.writeToXMLHeadless(pw, indent, campaign);
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
                    employerCode = item.getTextContent();
                } else if (item.getNodeName().equalsIgnoreCase("enemyCode")) {
                    enemyCode = item.getTextContent();
                } else if (item.getNodeName().equalsIgnoreCase("enemyMercenaryEmployerCode")) {
                    enemyMercenaryEmployerCode = item.getTextContent();
                } else if (item.getNodeName().equalsIgnoreCase("contractType")) {
                    setContractType(AtBContractType.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("allySkill")) {
                    setAllySkill(parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("allyQuality")) {
                    allyQuality = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("enemySkill")) {
                    setEnemySkill(parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("enemyQuality")) {
                    enemyQuality = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("difficulty")) {
                    difficulty = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("allyBotName")) {
                    allyBotName = item.getTextContent();
                } else if (item.getNodeName().equalsIgnoreCase("enemyBotName")) {
                    enemyBotName = item.getTextContent();
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
                    requiredCombatTeams = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("requiredCombatElements")) {
                    requiredCombatElements = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("moraleLevel")) {
                    setMoraleLevel(AtBMoraleLevel.parseFromString(item.getTextContent().trim()));
                } else if (item.getNodeName().equalsIgnoreCase("routEnd")) {
                    routEnd = MHQXMLUtility.parseDate(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("routedPayout")) {
                    String cleanValue = item.getTextContent().trim().replaceAll("[^0-9.]", "");
                    double value = Double.parseDouble(cleanValue);
                    routedPayout = Money.of(value);
                } else if (item.getNodeName().equalsIgnoreCase("partsAvailabilityLevel")) {
                    partsAvailabilityLevel = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("extensionLength")) {
                    extensionLength = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("sharesPct")) {
                    sharesPct = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("batchallAccepted")) {
                    batchallAccepted = Boolean.parseBoolean(item.getTextContent());
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
                    commandRoll = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("salvageRoll")) {
                    salvageRoll = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("supportRoll")) {
                    supportRoll = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("transportRoll")) {
                    transportRoll = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioDate")) {
                    specialEventScenarioDate = MHQXMLUtility.parseDate(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioType")) {
                    specialEventScenarioType = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase(StratConCampaignState.ROOT_XML_ELEMENT_NAME)) {
                    stratconCampaignState = StratConCampaignState.Deserialize(item);
                    stratconCampaignState.setContract(this);
                    this.setStratConCampaignState(stratconCampaignState);
                } else if (item.getNodeName().equalsIgnoreCase("parentContractId")) {
                    parentContract = new AtBContractRef(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("employerLiaison")) {
                    employerLiaison = Person.generateInstanceFromXML(item, campaign, version);
                } else if (item.getNodeName().equalsIgnoreCase("clanOpponent")) {
                    clanOpponent = Person.generateInstanceFromXML(item, campaign, version);
                }
            } catch (Exception e) {
                logger.error("", e);
            }

            if (employerLiaison == null) {
                createEmployerLiaison(campaign);
            }

            if (clanOpponent == null && getEnemy().isClan()) {
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

    public Person getEmployerLiaison() {
        return employerLiaison;
    }

    public void setEmployerLiaison(Person employerLiaison) {
        this.employerLiaison = employerLiaison;
    }

    public void createEmployerLiaison(Campaign campaign) {
        employerLiaison = campaign.newPerson(PersonnelRole.MILITARY_LIAISON, getEmployerCode(), Gender.RANDOMIZE);

        final RankSystem rankSystem = getEmployerFaction().getRankSystem();

        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return;
        }

        employerLiaison.setRankSystem(rankValidator, rankSystem);
        employerLiaison.setRank(Rank.RWO_MIN);
    }

    public Person getClanOpponent() {
        return clanOpponent;
    }

    public void setClanOpponent(Person clanOpponent) {
        this.clanOpponent = clanOpponent;
    }

    public void createClanOpponent(Campaign campaign) {
        clanOpponent = campaign.newPerson(PersonnelRole.MEKWARRIOR, getEnemyCode(), Gender.RANDOMIZE);

        Bloodname bloodname = Bloodname.randomBloodname(enemyCode, Phenotype.MEKWARRIOR, campaign.getGameYear());

        if (bloodname != null) {
            clanOpponent.setBloodname(bloodname.getName());
        }

        final RankSystem rankSystem = Ranks.getRankSystemFromCode("CLAN");

        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return;
        }

        clanOpponent.setRankSystem(rankValidator, rankSystem);
        clanOpponent.setRank(38);
    }

    public String getEmployerCode() {
        return employerCode;
    }

    public void setEmployerCode(String code, int year) {
        employerCode = code;
        setEmployer(getEmployerName(year));
        allyCamouflage = pickRandomCamouflage(year, employerCode);
    }

    public String getEmployerName(int year) {
        return isMercSubcontract() ?
                     "Mercenary (" + getEmployerFaction().getFullName(year) + ')' :
                     getEmployerFaction().getFullName(year);
    }

    public Faction getEnemy() {
        return Factions.getInstance().getFaction(getEnemyCode());
    }

    public String getEnemyCode() {
        return enemyCode;
    }

    public void setEnemyCode(String enemyCode) {
        this.enemyCode = enemyCode;
    }

    /**
     * Retrieves the name of the enemy for this contract.
     *
     * @param year The current year in the game.
     *
     * @return The name of the enemy.
     */
    public String getEnemyName(int year) {
        Faction faction = Factions.getInstance().getFaction(enemyCode);

        if (faction.isMercenary()) {
            if (Objects.equals(enemyBotName, "Enemy")) {
                return BackgroundsController.randomMercenaryCompanyNameGenerator(null);
            } else {
                return enemyBotName;
            }
        } else {
            return faction.getFullName(year);
        }
    }

    public @Nullable String getEnemyMercenaryEmployerCode() {
        return enemyMercenaryEmployerCode;
    }

    public @Nullable Faction getEnemyMercenaryEmployer() {
        return enemyMercenaryEmployerCode == null ? null :
                     Factions.getInstance().getFaction(enemyMercenaryEmployerCode);
    }

    /**
     * Sets the faction code representing the employer of the enemy mercenary forces.
     *
     * @param enemyMercenaryEmployerCode the faction code to assign as the employer of opposing mercenary units
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void setEnemyMercenaryEmployerCode(String enemyMercenaryEmployerCode) {
        this.enemyMercenaryEmployerCode = enemyMercenaryEmployerCode;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public AtBContractType getContractType() {
        return contractType;
    }

    public void setContractType(final AtBContractType contractType) {
        this.contractType = contractType;
        setType(contractType.toString());
    }

    public SkillLevel getAllySkill() {
        return allySkill;
    }

    public void setAllySkill(final SkillLevel allySkill) {
        this.allySkill = allySkill;
    }

    public SkillLevel getEnemySkill() {
        return enemySkill;
    }

    public void setEnemySkill(final SkillLevel enemySkill) {
        this.enemySkill = enemySkill;
    }

    public int getAllyQuality() {
        return allyQuality;
    }

    public void setAllyQuality(int allyQuality) {
        this.allyQuality = allyQuality;
    }

    public int getEnemyQuality() {
        return enemyQuality;
    }

    public void setEnemyQuality(int enemyQuality) {
        this.enemyQuality = enemyQuality;
    }

    public String getAllyBotName() {
        return allyBotName;
    }

    public void setAllyBotName(String name) {
        allyBotName = name;
    }

    public String getEnemyBotName() {
        return enemyBotName;
    }

    public void setEnemyBotName(String name) {
        enemyBotName = name;
    }

    public Camouflage getAllyCamouflage() {
        return allyCamouflage;
    }

    public void setAllyCamouflage(Camouflage allyCamouflage) {
        this.allyCamouflage = Objects.requireNonNull(allyCamouflage);
    }

    public PlayerColour getAllyColour() {
        return allyColour;
    }

    public void setAllyColour(PlayerColour allyColour) {
        this.allyColour = Objects.requireNonNull(allyColour);
    }

    public Camouflage getEnemyCamouflage() {
        return enemyCamouflage;
    }

    public void setEnemyCamouflage(Camouflage enemyCamouflage) {
        this.enemyCamouflage = enemyCamouflage;
    }

    public PlayerColour getEnemyColour() {
        return enemyColour;
    }

    public void setEnemyColour(PlayerColour enemyColour) {
        this.enemyColour = Objects.requireNonNull(enemyColour);
    }

    public int getRequiredCombatTeams() {
        return requiredCombatTeams;
    }

    public void setRequiredCombatTeams(int required) {
        requiredCombatTeams = required;
    }

    public int getRequiredCombatElements() {
        return requiredCombatElements;
    }

    public void setRequiredCombatElements(int required) {
        requiredCombatElements = required;
    }

    public int getPartsAvailabilityLevel() {
        return partsAvailabilityLevel;
    }

    public void setPartsAvailabilityLevel(final int partsAvailabilityLevel) {
        this.partsAvailabilityLevel = partsAvailabilityLevel;
    }

    public AtBMoraleLevel getMoraleLevel() {
        return moraleLevel;
    }

    public void setMoraleLevel(final AtBMoraleLevel moraleLevel) {
        this.moraleLevel = moraleLevel;
    }

    /**
     * Adjusts the current {@link AtBMoraleLevel} by the specified delta and returns the resulting morale level.
     *
     * <p>The method computes a new integer morale value by adding the given {@code delta} to the unit's current
     * morale level, then clamps the result to the valid range defined by {@code MINIMUM_MORALE_LEVEL} and
     * {@code MAXIMUM_MORALE_LEVEL}. It then attempts to resolve the resulting value to a corresponding
     * {@link AtBMoraleLevel}.</p>
     *
     * <p>If the resolved morale level is valid (i.e., non-{@code null}), the unit's internal morale state is updated.
     * If no valid enum constant exists for the computed level, the method leaves the current morale unchanged and
     * returns the existing level.</p>
     *
     * <p><b>Note:</b> a positive delta improves the enemy morale, a negative delta decreases enemy morale.</p>
     *
     * @param delta the amount to adjust the current morale level by; may be positive or negative
     *
     * @return the new {@link AtBMoraleLevel} after applying the delta; if no corresponding morale level exists for the
     *       computed value, the current morale level is returned unchanged
     *
     * @author Illiani
     * @since 0.50.10
     */
    public AtBMoraleLevel changeMoraleLevel(final int delta) {
        int currentLevel = moraleLevel.getLevel();
        int newLevel = clamp(currentLevel + delta, MINIMUM_MORALE_LEVEL, MAXIMUM_MORALE_LEVEL);

        AtBMoraleLevel newMoraleLevel = AtBMoraleLevel.parseFromLevel(newLevel);
        if (newMoraleLevel != null) {
            moraleLevel = newMoraleLevel;
        }

        return newMoraleLevel != null ? newMoraleLevel : moraleLevel;
    }

    public boolean isPeaceful() {
        return getContractType().isGarrisonType() && getMoraleLevel().isRouted();
    }

    public LocalDate getRoutEnd() {
        return routEnd;
    }

    public void setRoutEnd(LocalDate routEnd) {
        this.routEnd = routEnd;
    }

    @Override
    public int getSharesPercent() {
        return sharesPct;
    }

    public void setAtBSharesPercent(int pct) {
        sharesPct = pct;
    }

    /**
     * Checks if the Batchall has been accepted for the contract.
     *
     * @return {@code true} if the Batchall has been accepted, {@code false} otherwise.
     */
    public boolean isBatchallAccepted() {
        return batchallAccepted;
    }

    /**
     * Sets the {@code batchallAccepted} flag for this contract.
     *
     * @param batchallAccepted The value to set for the {@code batchallAccepted} flag.
     */
    public void setBatchallAccepted(final boolean batchallAccepted) {
        this.batchallAccepted = batchallAccepted;
    }

    public void addPlayerMinorBreach() {
        playerMinorBreaches++;
    }

    public void addPlayerMinorBreaches(int num) {
        playerMinorBreaches += num;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void addEmployerMinorBreaches(int num) {
        employerMinorBreaches += num;
    }

    public void setContractScoreArbitraryModifier(int newModifier) {
        contractScoreArbitraryModifier = newModifier;
    }

    public int getBattleTypeMod() {
        return battleTypeMod + nextWeekBattleTypeMod;
    }

    public StratConCampaignState getStratconCampaignState() {
        return stratconCampaignState;
    }

    public void setStratConCampaignState(StratConCampaignState state) {
        stratconCampaignState = state;
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

        setType(contract.getType());
        setSystemId(contract.getSystemId());
        setDesc(contract.getDescription());
        setStatus(contract.getStatus());
        for (Scenario s : contract.getScenarios()) {
            addScenario(s);
        }
        setId(contract.getId());
        setLength(contract.getLength());
        setStartDate(contract.getStartDate());
        /*
         * Set ending date; the other calculated values will be replaced
         * from the original contract
         */
        calculateContract(campaign);
        setMultiplier(contract.getMultiplier());
        setTransportComp(contract.getTransportComp());
        setStraightSupport(contract.getStraightSupport());
        setOverheadComp(contract.getOverheadComp());
        setCommandRights(contract.getCommandRights());
        setBattleLossComp(contract.getBattleLossComp());
        setSalvagePct(contract.getSalvagePct());
        setSalvageExchange(contract.isSalvageExchange());
        setSalvagedByUnit(contract.getSalvagedByUnit());
        setSalvagedByEmployer(contract.getSalvagedByEmployer());
        setSigningBonusPct(contract.getSigningBonusPct());
        setAdvancePct(contract.getAdvancePct());
        setMRBCFee(contract.payMRBCFee());
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
                          .getFactionFromFullNameAndYear(contract.getEmployer(), campaign.getGameYear());
        if (null == f) {
            employerCode = "IND";
        } else {
            employerCode = f.getShortName();
        }

        if (getContractType().isPirateHunting()) {
            Faction employer = getEmployerFaction();
            enemyCode = employer.isClan() ? "BAN" : PIRATE_FACTION_CODE;
        } else if (getContractType().isRiotDuty()) {
            enemyCode = "REB";
        }

        setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              contractType.isCadreDuty(), true, 1.0));
        setRequiredCombatElements(ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign));

        setPartsAvailabilityLevel(getContractType().calculatePartsAvailabilityLevel());

        int currentYear = campaign.getGameYear();
        allyBotName = getEmployerName(currentYear);
        allyCamouflage = pickRandomCamouflage(currentYear, employerCode);

        enemyBotName = getEnemyName(currentYear);
        enemyCamouflage = pickRandomCamouflage(currentYear, enemyCode);

        difficulty = calculateContractDifficulty(contract.getStartDate().getYear(),
              true,
              campaign.getAllCombatEntities());

        clanTechSalvageOverride();
    }

    private static AtBContractType getAtBContractType(Contract contract) {
        AtBContractType contractType = null;
        for (final AtBContractType type : AtBContractType.values()) {
            if (type.toString().equalsIgnoreCase(contract.getType())) {
                contractType = type;
                break;
            }
        }
        /* Make a rough guess */
        if (contractType == null) {
            if (contract.getLength() <= 3) {
                contractType = AtBContractType.OBJECTIVE_RAID;
            } else if (contract.getLength() < 12) {
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
     * This method initiates a batchall, a challenge/dialog to decide on the conduct of a campaign. Prompts the player
     * with a message and options to accept or refuse the batchall.
     *
     * @param campaign The current campaign.
     *
     * @return {@code true} if the batchall is accepted, {@code false} otherwise.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean initiateBatchall(Campaign campaign) {
        // Retrieves the title from the resources
        String title = resources.getString("incomingTransmission.title");

        // Retrieves the batchall statement based on infamy and enemy code
        String batchallStatement = BatchallFactions.getGreeting(campaign, enemyCode);

        // An ImageIcon to hold the clan's faction icon
        ImageIcon icon = getFactionLogo(campaign.getGameYear(), enemyCode);

        // Set the commander's rank and use a name generator to generate the commander's
        // name
        String rank = resources.getString("starColonel.text");
        RandomNameGenerator randomNameGenerator = new RandomNameGenerator();
        String commander = randomNameGenerator.generate(Gender.RANDOMIZE, true, enemyCode);
        commander += ' ' + Bloodname.randomBloodname(enemyCode, Phenotype.MEKWARRIOR, campaign.getGameYear()).getName();

        // Construct the batchall message
        String message = String.format(resources.getString("batchallOpener.text"),
              this.getName(),
              rank,
              commander,
              getEnemy().getFullName(campaign.getGameYear()),
              getSystemName(campaign.getLocalDate()));
        message = message + batchallStatement;

        // Append additional message text if the fame is less than 5
        if (campaign.getFameAndInfamy().getFameForFaction(enemyCode) < 5) {
            message = message + resources.getString("batchallCloser.text");
        }

        // Create a text pane to display the message
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(message);
        textPane.setEditable(false);

        // Create a panel to display the icon and the batchall message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(icon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(textPane, BorderLayout.SOUTH);

        // Choose dialog to display based on the fame
        if (campaign.getFameAndInfamy().getFameForFaction(enemyCode) > 4) {
            noBatchallOfferedDialog(panel, title);
            return false;
        } else {
            return batchallDialog(campaign, panel, title);
        }
    }

    /**
     * This function creates a dialog with accept and refuse buttons.
     *
     * @param campaign the current campaign
     * @param panel    the panel to display in the dialog
     * @param title    the title of the dialog
     *
     * @return {@code true} if the batchall is accepted, {@code false} otherwise
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    private boolean batchallDialog(Campaign campaign, JPanel panel, String title) {
        // We use a single-element array to store the result, because we need to modify it inside the action
        // listeners, which requires the variable to be effectively final
        final boolean[] result = { false };

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title); // Set the title of the dialog
        dialog.setLayout(new BorderLayout()); // Set a border layout manager

        // Create an accept button and add its action listener. When clicked, it will set the result to true and
        // close the dialog
        JButton acceptButton = new JButton(resources.getString("responseAccept.text"));
        acceptButton.setToolTipText(resources.getString("responseAccept.tooltip"));
        acceptButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        // Create a refuse button and add its action listener. When clicked, it will trigger a refusal confirmation
        // dialog
        String refusalOption = resources.getString("responseRefuse.text");

        // If the campaign is not a Clan faction, check whether this is the first contact they've had with the Clans.
        // If so, whether at least a month has passed since the Wolf's Dragoons conference on Outreach (which
        // explained who and what the Clans were).
        if (!campaign.getFaction().isClan() && campaign.getLocalDate().isBefore(LocalDate.of(3051, 2, 1))) {

            boolean isFirstClanEncounter = BATCHALL_FACTIONS.stream()
                                                 .mapToDouble(factionCode -> campaign.getFameAndInfamy()
                                                                                   .getFameLevelForFaction(factionCode))
                                                 .noneMatch(infamy -> infamy != 0);

            if (isFirstClanEncounter) {
                refusalOption = resources.getString("responseFirstEncounter.text");
            }
        }

        JButton refuseButton = new JButton(refusalOption);

        refuseButton.setToolTipText(resources.getString("responseRefuse.tooltip"));
        refuseButton.addActionListener(e -> {
            // Close the current dialog
            dialog.dispose();

            // Use another method to show a refusal confirmation dialog and store the result
            result[0] = refusalConfirmationDialog(campaign);
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack(); // Size the dialog to fit the preferred size and layouts of its components
        dialog.setLocationRelativeTo(null); // Center the dialog on the screen
        dialog.setModal(true); // Make the dialog block user input to other top-level windows
        dialog.setVisible(true); // Show the dialog

        return result[0]; // Return the result when the dialog is disposed
    }

    /**
     * This function displays a dialog asking for final confirmation to refuse a batchall, and performs related actions
     * if the refusal is confirmed.
     *
     * @param campaign the current campaign
     *
     * @return {@code true} if the user accepts the refusal, {@code false} if the user cancels the refusal
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    private boolean refusalConfirmationDialog(Campaign campaign) {
        // Create modal JDialog
        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());

        // Buffer for storing user response (acceptance/refusal)
        final boolean[] response = { false };

        // "Accept" Button
        JButton acceptButton = new JButton(resources.getString("responseAccept.text"));
        acceptButton.setToolTipText(resources.getString("responseAccept.tooltip"));
        acceptButton.addActionListener(e -> {
            response[0] = true; // User has accepted
            dialog.dispose(); // Close dialog
        });

        // "Refuse" Button
        JButton refuseButton = new JButton(resources.getString("responseRefuse.text"));
        refuseButton.setToolTipText(resources.getString("responseRefuse.tooltip"));
        refuseButton.addActionListener(e -> {
            // Update the campaign state on refusal
            campaign.addReport(GENERAL, resources.getString("refusalReport.text"));
            campaign.getFameAndInfamy().updateFameForFaction(campaign, enemyCode, -1);
            response[0] = false; // User has refused
            dialog.dispose(); // Close dialog
        });

        // Panel for hosting buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Message Label
        JLabel messageLabel = new JLabel(String.format(resources.getString("refusalConfirmation.text"),
              getEnemy().getFullName(campaign.getGameYear())));

        // Add Message and Buttons to the dialog
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Configure and display dialog
        dialog.pack(); // Fit dialog to its contents
        dialog.setLocationRelativeTo(null); // Center dialog
        dialog.setModal(true); // Block access to other windows
        dialog.setVisible(true); // Display dialog

        // Return user response
        return response[0];
    }

    /**
     * Displays a dialog with a message for when the faction has refused to offer a Batchall due to past player
     * refusals.
     *
     * @param panel The panel to display in the dialog.
     * @param title The title of the dialog.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    private void noBatchallOfferedDialog(JPanel panel, String title) {
        // Create a new JDialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        JButton responseButton = new JButton(resources.getString("responseBringItOn.text"));
        responseButton.setToolTipText(resources.getString("responseBringItOn.tooltip"));

        // Dispose the dialog when the button is clicked
        responseButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(responseButton); // Add the button to the panel

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Size the dialog to fit the preferred size and layouts of its components
        dialog.pack();

        // Center the dialog on the screen
        dialog.setLocationRelativeTo(null);

        // Set the dialog to be modal
        dialog.setModal(true);

        // Show the dialog
        dialog.setVisible(true);
    }

    /**
     * @deprecated use {@link #getContractDifficultySkulls()} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public JPanel getContractDifficultySkulls(Campaign campaign) {
        return getContractDifficultySkulls();
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
     * @param gameYear the year used to determine which faction logos to display
     *
     * @return a {@link JPanel} with the employer and enemy faction logos, with a divider in between
     *
     * @author Illiani
     * @since 0.50.06
     */
    public JPanel getBelligerentsPanel(int gameYear) {
        final int SIZE = 100;

        String employer = getEmployerCode();
        ImageIcon employerImage = getFactionLogo(gameYear, employer);
        employerImage = scaleImageIcon(employerImage, SIZE, true);

        JLabel divider = new JLabel("/");
        divider.setHorizontalAlignment(SwingConstants.CENTER);
        int fontSize = scaleForGUI(SIZE); // scaleImageIcon already includes the necessary scaling
        divider.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        divider.setForeground(new Color(0, 0, 0, 128));

        String enemy = getEnemyCode();
        ImageIcon enemyImage = getFactionLogo(gameYear, enemy);
        enemyImage = scaleImageIcon(enemyImage, SIZE, true);

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel(employerImage));
        panel.add(divider);
        panel.add(new JLabel(enemyImage));

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
        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(enemyCode, enemySkill);
        double enemySkillMultiplier = getSkillMultiplier(opposingSkill);
        double enemyPower = estimateMekStrength(gameYear, useGenericBV, enemyCode, enemyQuality);

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

        return min(max(mappedValue, 1), 10);
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
            // TODO implement getGBV(int index) in UnitTable to simplify this?
            // getMekSummary(int index) is NULL for salvage.
            int genericBattleValue = unitTable.getMekSummary(i).loadEntity().getGenericBattleValue();
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
     * @return the command roll that was used to determine command rights. Only used by CamOps Contract Market.
     */
    public int getCommandRoll() {
        return commandRoll;
    }

    /**
     * @param roll the command roll that was used to determine command rights. Only used by CamOps Contract Market.
     */
    public void setCommandRoll(int roll) {
        commandRoll = roll;
    }

    /**
     * @return the salvage roll that was used to determine salvage rights. Only used by CamOps Contract Market.
     */
    public int getSalvageRoll() {
        return salvageRoll;
    }

    /**
     * @param roll the salvage roll that was used to determine salvage rights. Only used by CamOps Contract Market.
     */
    public void setSalvageRoll(int roll) {
        salvageRoll = roll;
    }

    /**
     * @return the support roll that was used to determine support rights. Only used by CamOps Contract Market.
     */
    public int getSupportRoll() {
        return supportRoll;
    }

    /**
     * @param roll the support roll that was used to determine support rights. Only used by CamOps Contract Market.
     */
    public void setSupportRoll(int roll) {
        supportRoll = roll;
    }

    /**
     * @return the transport roll that was used to determine transport rights. Only used by CamOps Contract Market.
     */
    public int getTransportRoll() {
        return transportRoll;
    }

    /**
     * @param roll the transport roll that was used to determine transport rights. Only used by CamOps Contract Market.
     */
    public void setTransportRoll(int roll) {
        transportRoll = roll;
    }

    public void setRoutedPayout(@Nullable Money routedPayout) {
        this.routedPayout = routedPayout;
    }

    public @Nullable Money getRoutedPayout() {
        return routedPayout;
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
        if (stratconCampaignState == null) {
            return 0;
        }

        double baseRequirement = getRequiredCombatTeams();

        int duration = getLength();
        if (contractType.isGarrisonType()) {
            duration = (int) ceil(duration * 0.75); // We assume around 25% of the contract will be peaceful
        }

        double trackCount = 0;
        int totalScenarioOdds = 0;
        for (StratConTrackState trackState : stratconCampaignState.getTracks()) {
            trackCount++;
            totalScenarioOdds += trackState.getScenarioOdds();
        }

        double meanScenarioOdds = totalScenarioOdds / trackCount;
        double scenarioOdds = meanScenarioOdds / 100.0;
        double turningPointChance = switch (getCommandRights()) {
            case INTEGRATED -> 1.0;
            default -> 0.33;
        };

        // This result gives us the average number of Turning Points expected for the contract
        return (int) ceil(baseRequirement * duration * scenarioOdds * turningPointChance);
    }
}
