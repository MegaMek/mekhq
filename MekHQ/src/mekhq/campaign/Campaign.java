/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_MONTH;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_WEEK;
import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.enums.DailyReportType.AGGREGATE;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_INTERSTELLAR_NEGOTIATOR;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_LOGISTICIAN;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_ADMIN_APPRAISAL_FAIL;
import static mekhq.campaign.personnel.ranks.Rank.RO_MIN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_MEDTECH;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC;
import static mekhq.campaign.personnel.skills.SkillType.getType;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;
import static mekhq.campaign.randomEvents.other.GrayMonday.isGrayMonday;
import static mekhq.campaign.reputation.chaosReputation.ChaosReputation.STARTING_REPUTATION_SCORE;
import static mekhq.campaign.unit.Unit.TECH_WORK_DAY;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.TORTUGA_DOMINIONS_FACTION_CODE;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import jakarta.annotation.Nonnull;
import megamek.Version;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.codeUtilities.ObjectUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.Player;
import megamek.common.SimpleTechLevel;
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Gender;
import megamek.common.enums.TechBase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.interfaces.ITechManager;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.ForceQuartermaster.PartAcquisitionResult;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsMarshaller;
import mekhq.campaign.dailyReportLog.DailyReportLog;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.*;
import mekhq.campaign.events.loans.LoanNewEvent;
import mekhq.campaign.events.loans.LoanPaidEvent;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.events.missions.MissionRemovedEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.parts.PartWorkEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.scenarios.ScenarioNewEvent;
import mekhq.campaign.events.scenarios.ScenarioRemovedEvent;
import mekhq.campaign.events.units.UnitNewEvent;
import mekhq.campaign.events.units.UnitRemovedEvent;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.log.HistoricalLogEntry;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.log.UnitLogger;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractHistoryData;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.utilities.ContractSettlement;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BAArmor;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.SpacecraftCoolingSystem;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.InfantryAmmoBin;
import mekhq.campaign.parts.equipment.InfantryDisposableWeaponPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.protomeks.ProtoMekArmor;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.death.RandomDeath;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.Appraisal;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventLibraries;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.storyArc.StoryArc;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitAcquisitionType;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.enums.StartingLocationMode;
import mekhq.campaign.universe.eras.Era;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumsLibrary;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.warriorsAlmanac.WarriorsAlmanacEntry;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IFabricatable;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.service.IAutosaveService;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;

/**
 * The main campaign class, keeps track of teams and units
 *
 * @author Taharqa
 */
public class Campaign implements ITechManager {
    private static final MMLogger LOGGER = MMLogger.create(Campaign.class);

    public static final String REPORT_LINEBREAK = "<br/><br/>";
    /**
     * When using the 'useful assistants' campaign options, the relevant skill levels possessed by each assistant is
     * divided by this value and then floored.\
     */
    public static final double ASSISTANT_SKILL_LEVEL_DIVIDER = 2.5;

    private UUID id;
    private Version version; // this is dynamically populated on load and doesn't need to be saved
    private final List<Version> pastVersions = new ArrayList<>();

    // we have three things to track: (1) teams, (2) units, (3) repair tasks
    // we will use the same basic system (borrowed from MegaMek) for tracking
    // all three
    // OK now we have more, parts, personnel, forces, missions, and scenarios.
    // and more still - we're tracking DropShips and WarShips in a separate set so
    // that we can assign units to transports
    /** The player's active force: faction identity, finances, reputation, and the owned hangar/warehouse/personnel. */
    @Nonnull
    private final PlayerForce playerForce;
    // TODO (campaign split): Quartermaster holds a Campaign back-reference. Remove that coupling so it can
    //   move onto the force (AbstractForce/PlayerForce) alongside the other owned state.
    private final ForceQuartermaster quartermaster;
    CampaignTransporterMap tacticalTransporters = new CampaignTransporterMap(this,
          CampaignTransportType.TACTICAL_TRANSPORT);
    CampaignTransporterMap towTransporters = new CampaignTransporterMap(this, CampaignTransportType.TOW_TRANSPORT);
    private final ContractHistoryData contractHistory = new ContractHistoryData();
    private final TreeMap<Integer, Scenario> scenarios = new TreeMap<>();
    private final Map<UUID, List<Kill>> kills = new HashMap<>();

    private transient final UnitNameTracker unitNameTracker = new UnitNameTracker();

    private int lastMissionId;
    private int lastScenarioId;

    // I need to put a basic game object in campaign so that I can
    // assign it to the entities, otherwise some entity methods may get NPE
    // if they try to call up game options
    private final Game game;
    private final Player player;

    private GameOptions gameOptions;

    private LocalDate currentDay;
    private LocalDate campaignStartDate;

    private transient CampaignNewDayManager newDayManager = null;

    private final DailyReportLog dailyReportLog = new DailyReportLog();

    private Person genericAcquisitionPerson;

    // this is updated and used per gaming session, it is enabled/disabled via the Campaign options we're re-using
    // the LogEntry class used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private Systems systemsInstance;
    private final Map<String, PlanetarySystem> planetarySystemOverrides = new LinkedHashMap<>();
    private final CampaignLocationManager locationManager = new CampaignLocationManager();

    private final News news;

    private PartsStore partsStore;

    private final List<String> customs;

    private CampaignOptions campaignOptions;
    private RandomSkillPreferences randomSkillPreferences = new RandomSkillPreferences();
    private CampaignGUI gui;

    private AbstractUnitMarket unitMarket;

    private RandomDeath randomDeath;
    private final List<String> turnoverRetirementInformation;

    private AtBConfiguration atbConfig; // AtB
    private IUnitGenerator unitGenerator; // deprecated
    private CampaignSummary campaignSummary;
    // TODO (campaign split): the transporter maps hold a Campaign back-reference. Remove that coupling so they
    //   can move onto the force (AbstractForce/PlayerForce) alongside the other owned state.
    CampaignTransporterMap shipTransporters = new CampaignTransporterMap(this, CampaignTransportType.SHIP_TRANSPORT);
    private StoryArc storyArc;
    private BehaviorSettings autoResolveBehaviorSettings;
    private boolean processProcurement;

    // Libraries
    // We deliberately don't write this data to the save file as we want it rebuilt
    // every time the campaign loads. This ensures updates can be applied and there is no risk of
    // bugs being permanently locked into the campaign file.
    RandomEventLibraries randomEventLibraries;
    FactionStandingUltimatumsLibrary factionStandingUltimatumsLibrary;

    private Map<Integer, List<WarriorsAlmanacEntry>> partsAlmanac;
    private Map<Integer, List<WarriorsAlmanacEntry>> unitsAlmanac;

    /**
     * A constant that provides the ISO-8601 definition of week-based fields.
     *
     * <p>This includes the first day of the week set to Monday and the minimal number of days in the first week of
     * the year set to 4.</p>
     */
    public static final WeekFields WEEK_FIELDS = WeekFields.ISO;

    @Deprecated(since = "0.51.0")
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
          MekHQ.getMHQOptions().getLocale());

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";
    private static final String ACTION_CHECK_BUNDLE = "mekhq.resources.ActionCheck";
    private static final String TERRA_ID = "Terra";

    /**
     * This is used to determine if the player has an active AtB Contract, and is recalculated on load
     */
    private transient boolean hasActiveContract;

    private final IAutosaveService autosaveService;

    public Campaign(CampaignConfiguration campConf) {
        this(
              campConf.getGame(),
              campConf.getPlayer(),
              campConf.getName(),
              campConf.getDate(),
              campConf.getCampaignOpts(),
              campConf.getGameOptions(),
              campConf.getPartsStore(),
              campConf.getNewPersonnelMarket(),
              campConf.getRandomDeath(),
              campConf.getCampaignSummary(),
              campConf.getfaction(),
              campConf.getTechFaction(),
              campConf.getCurrencyManager(),
              campConf.getSystemsInstance(),
              campConf.getLocation(),
              campConf.getReputationController(),
              campConf.getFactionStandings(),
              campConf.getRankSystem(),
              campConf.getFormations(),
              campConf.getfinances(),
              campConf.getRandomEvents(),
              campConf.getUltimatums(),
              campConf.getRetDefTracker(),
              campConf.getAutosave(),
              campConf.getBehaviorSettings(),
              campConf.getPersonnelMarket(),
              campConf.getUnitMarket(),
              campConf.getDivorce(),
              campConf.getMarriage(),
              campConf.getProcreation()
        );
    }

    public Campaign(Game game,
          Player player, String name, LocalDate date, CampaignOptions campaignOpts, GameOptions gameOptions,
          PartsStore partsStore, NewPersonnelMarket newPersonnelMarket,
          RandomDeath randomDeath, CampaignSummary campaignSummary,
          Faction faction, megamek.common.enums.Faction techFaction, CurrencyManager currencyManager,
          Systems systemsInstance, AbstractLocation startLocation, ForceReputationController reputationController,
          FactionStandings factionStandings, RankSystem rankSystem, Formation formation, Finances finances,
          RandomEventLibraries randomEvents, FactionStandingUltimatumsLibrary ultimatums,
          RetirementDefectionTracker retDefTracker, IAutosaveService autosave,
          BehaviorSettings behaviorSettings,
          PersonnelMarket persMarket,
          AbstractUnitMarket unitMarket,
          AbstractDivorce divorce, AbstractMarriage marriage,
          AbstractProcreation procreation) {
        MHQOptions mekhqOptions = MekHQ.getMHQOptions();

        // Essential state
        id = UUID.randomUUID();
        this.game = game;
        this.player = player;
        this.game.addPlayer(0, this.player);
        currentDay = date;
        campaignOptions = campaignOpts;
        this.gameOptions = gameOptions;
        game.setOptions(gameOptions);
        this.systemsInstance = systemsInstance;

        // The player force owns faction identity, finances, reputation, and the hangar/warehouse/personnel. It is the
        // IPlace anchored into the location tree, so it must exist before we set the campaign's location.
        playerForce = new PlayerForce(faction, techFaction, rankSystem, finances, reputationController,
              STARTING_REPUTATION_SCORE, factionStandings, campaignOpts);
        playerForce.setName(name);

        setLocation(startLocation);
        playerForce.getForceDetachment().setParent(startLocation);
        playerForce.setFormations(formation);
        playerForce.getFormationIds().put(0, formation);
        randomEventLibraries = randomEvents;
        factionStandingUltimatumsLibrary = ultimatums;
        getPlayerForce().getHumanResources().setRetirementDefectionTracker(retDefTracker);
        autosaveService = autosave;
        autoResolveBehaviorSettings = behaviorSettings;
        this.partsStore = partsStore;
        getPlayerForce().getHumanResources().setNewPersonnelMarket(newPersonnelMarket);
        this.randomDeath = randomDeath;
        this.campaignSummary = campaignSummary;

        // Members that take `this` as an argument
        this.quartermaster = new ForceQuartermaster(this);

        // Primary init, sets state from passed values
        getPlayerForce().setFaction(faction);
        getPlayerForce().setRankSystemDirect(rankSystem);
        getPlayerForce().getHumanResources().setPersonnelMarket(persMarket);
        setUnitMarket(unitMarket);
        getPlayerForce().getHumanResources().setDivorce(divorce);
        getPlayerForce().getHumanResources().setMarriage(marriage);
        getPlayerForce().getHumanResources().setProcreation(procreation);

        // Starting config / default values
        campaignStartDate = null;
        overtime = false;
        gmMode = false;
        customs = new ArrayList<>();
        turnoverRetirementInformation = new ArrayList<>();
        atbConfig = null;
        hasActiveContract = false;
        processProcurement = true;
        // The force initializes the migrated settings/capacities to their static defaults; only the
        // MHQ-options-derived one is asserted here where those options are available.
        playerForce.setTopUpWeekly(mekhqOptions.getNewDayAutoLogistics());

        // Secondary initialization from passed / derived values
        news = new News(getGameYear(), id.getLeastSignificantBits());
        getPlayerForce().getHumanResources().resetAsTechMinutes(getCampaignOptions());

        // These classes require a Campaign reference to operate/initialize
        currencyManager.setCampaign(this);
        this.partsStore.stock(this);
        getPlayerForce().getHumanResources().getNewPersonnelMarket().setCampaign(this);
        this.randomDeath.setCampaign(this);
        this.campaignSummary.setCampaign(this);
    }

    public IAutosaveService getAutosaveService() {
        return autosaveService;
    }

    /**
     * Returns the campaign's resource bundle (for use by extracted subsystems such as {@link ForceHumanResources}).
     *
     * @return the campaign {@link ResourceBundle}
     */
    public ResourceBundle getResources() {
        return resources;
    }

    public void setGUI(CampaignGUI gui) {
        this.gui = gui;
    }

    /**
     * @return the {@link CampaignGUI}
     */
    public CampaignGUI getGUI() {
        return gui;
    }

    /**
     * @param overviewLoadingValue the overviewLoadingValue to set
     */
    public void setOverviewLoadingValue(boolean overviewLoadingValue) {
        this.overviewLoadingValue = overviewLoadingValue;
    }

    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public @Nullable Version getVersion() {
        return version;
    }

    public List<Version> getPastVersions() {
        return pastVersions;
    }

    public void addPastVersion(Version pastVersion) {
        this.pastVersions.add(pastVersion);
    }

    public Era getEra() {
        return Eras.getInstance().getEra(getLocalDate());
    }

    public String getTitle() {
        MHQOptions options = MekHQ.getMHQOptions();
        String formattedDate = options.getLongDisplayFormattedDate(getLocalDate());

        // Only prepend the short weekday when the configured long date pattern does not already
        // contain an unquoted day-of-week field. Otherwise we duplicate the day on default
        // settings, e.g. "Sun, Sunday, 4 May 3025". Locale is sourced from the same getter the
        // date formatter uses, so the weekday and date are localized consistently.
        if (!patternHasWeekdayField(options.getLongDisplayDateFormat())) {
            String shortWeekday = getLocalDate().getDayOfWeek()
                                        .getDisplayName(TextStyle.SHORT, options.getDateLocale());
            formattedDate = shortWeekday + ", " + formattedDate;
        }

        return getPlayerForce().getName() +
                     " (" +
                     getPlayerForce().getFaction().getFullName(getGameYear()) +
                     ')' +
                     " - " +
                     formattedDate +
                     " (" +
                     getEra() +
                     ')';
    }

    /**
     * Returns {@code true} if the given {@link java.time.format.DateTimeFormatter} pattern contains an unquoted
     * day-of-week field token ({@code E}, {@code e}, or {@code c}). Single-quoted literal segments are skipped, and
     * {@code ''} is treated as a literal single quote.
     */
    private static boolean patternHasWeekdayField(String pattern) {
        boolean inQuote = false;
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    i += 2;
                    continue;
                }
                inQuote = !inQuote;
            } else if (!inQuote && (ch == 'E' || ch == 'e' || ch == 'c')) {
                return true;
            }
            i++;
        }
        return false;
    }

    public LocalDate getLocalDate() {
        return currentDay;
    }

    public void setLocalDate(LocalDate currentDay) {
        this.currentDay = currentDay;
    }

    public LocalDate getCampaignStartDate() {
        return campaignStartDate;
    }

    public void setCampaignStartDate(LocalDate campaignStartDate) {
        this.campaignStartDate = campaignStartDate;
    }

    public @Nullable PlanetarySystem getCurrentSystem() {
        AbstractLocation location = getPlayerForce().getForceDetachment().getCurrentLocation();
        return location != null ? location.getCurrentSystem() : null;
    }

    public boolean isUseCommandCircuitForContract(AbstractContract abstractContract) {
        return FactionStandingUtilities.isUseCommandCircuit(
              getPlayerForce().isOverridingCommandCircuitRequirements(), gmMode,
              campaignOptions.isUseFactionStandingCommandCircuitSafe(),
              getPlayerForce().getFactionStandings(), List.of(abstractContract));
    }

    public boolean isUseCommandCircuit() {
        return FactionStandingUtilities.isUseCommandCircuit(
              getPlayerForce().isOverridingCommandCircuitRequirements(), isGM(),
              getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              getPlayerForce().getFactionStandings(), getFutureContracts());
    }

    /**
     * Returns the Hiring Hall level from the force's current system on the current date. If there is no hiring hall
     * present, the level is HiringHallLevel.NONE.
     *
     * @return The Hiring Hall level of the current system at the present date.
     */
    public HiringHallLevel getSystemHiringHallLevel() {
        return getCurrentSystem().getHiringHallLevel(getLocalDate());
    }

    // region Markets

    public AbstractUnitMarket getUnitMarket() {
        return unitMarket;
    }

    public void setUnitMarket(final AbstractUnitMarket unitMarket) {
        this.unitMarket = unitMarket;
    }

    public void setNewPersonnelMarket(final NewPersonnelMarket newPersonnelMarket) {
        getPlayerForce().getHumanResources().setNewPersonnelMarket(newPersonnelMarket);
        getPlayerForce().getHumanResources().getNewPersonnelMarket().setCampaign(this);
    }
    // endregion Markets

    // region Personnel Modules
    public RandomDeath getRandomDeath() {
        return randomDeath;
    }

    public void resetRandomDeath() {
        setRandomDeath(new RandomDeath());
    }

    public void setRandomDeath(RandomDeath randomDeath) {
        this.randomDeath = randomDeath;
        this.randomDeath.setCampaign(this);
    }

    // endregion Personnel Modules

    /**
     * Initializes the unit generator. Called when the unit generator is first used or when the method has been changed
     * in {@link CampaignOptions}.
     */
    public void initUnitGenerator() {
        unitGenerator = new RATGeneratorConnector(getGameYear());
    }

    /**
     * @return - the class responsible for generating random units
     */
    public IUnitGenerator getUnitGenerator() {
        if (unitGenerator == null) {
            initUnitGenerator();
        }
        return unitGenerator;
    }

    public void setAtBConfig(AtBConfiguration config) {
        atbConfig = config;
    }

    public AtBConfiguration getAtBConfig() {
        if (atbConfig == null) {
            atbConfig = AtBConfiguration.loadFromXml();
        }
        return atbConfig;
    }

    /**
     * Process retirements for retired personnel, if any.
     *
     * @param totalPayout     The total retirement payout.
     * @param unitAssignments List of unit assignments.
     *
     * @return False if there were payments AND they were unable to be processed, true otherwise.
     */
    public boolean applyRetirement(Money totalPayout, Map<UUID, UUID> unitAssignments) {
        turnoverRetirementInformation.clear();

        if ((totalPayout.isPositive()) ||
                  (null != getPlayerForce().getHumanResources().getRetirementDefectionTracker().getRetirees())) {
            if (getPlayerForce().getFinances()
                      .debit(TransactionType.PAYOUT, getLocalDate(), totalPayout, "Final Payout")) {
                for (UUID pid : getPlayerForce().getHumanResources().getRetirementDefectionTracker().getRetirees()) {
                    Person person = getPlayerForce().getHumanResources().getPerson(pid);
                    boolean wasKilled = getPlayerForce().getHumanResources()
                                              .getRetirementDefectionTracker()
                                              .getPayout(pid).isWasKilled();
                    boolean wasSacked = getPlayerForce().getHumanResources()
                                              .getRetirementDefectionTracker()
                                              .getPayout(pid).isWasSacked();

                    if ((!wasKilled) && (!wasSacked)) {
                        if (!person.getPermanentInjuries().isEmpty()) {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.RETIRED);
                        }
                        if (isBreakingContract(person,
                              getLocalDate(),
                              getCampaignOptions().get(CampaignOption.SERVICE_CONTRACT_DURATION))) {
                            if (!getActiveContracts().isEmpty()) {
                                int roll = randomInt(20);

                                if (roll == 0) {
                                    person.changeStatus(this, getLocalDate(), PersonnelStatus.DEFECTED);
                                }
                            } else {
                                person.changeStatus(this, getLocalDate(), PersonnelStatus.RESIGNED);
                            }
                        } else if (person.getAge(getLocalDate()) >= 50) {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.RETIRED);
                        } else {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.RESIGNED);
                        }
                    }

                    if (!person.getStatus().isActive()) {
                        turnoverRetirementInformation.add(String.format(person.getStatus().getReportText(),
                              person.getHyperlinkedFullTitle()));
                    }

                    if (wasSacked) {
                        if (person.getPermanentInjuries().isEmpty()) {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.SACKED);
                        } else {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.RETIRED);
                        }
                    }

                    // civilian spouses follow their partner in departing
                    Person spouse = person.getGenealogy().getSpouse();

                    if ((spouse != null) && (spouse.getPrimaryRole().isCivilian())) {
                        addReport(PERSONNEL, spouse.getHyperlinkedFullTitle() +
                                                   ' ' +
                                                   resources.getString("turnoverJointDeparture.text"));
                        spouse.changeStatus(this, getLocalDate(), PersonnelStatus.LEFT);

                        turnoverRetirementInformation.add(spouse.getHyperlinkedFullTitle() +
                                                                ' ' +
                                                                resources.getString("turnoverJointDeparture.text"));
                    }

                    // non-civilian spouses may divorce the remaining partner
                    if ((person.getAge(getLocalDate()) >= 50) && (!campaignOptions.get(CampaignOption.RANDOM_DIVORCE_METHOD).isNone())) {
                        if ((spouse != null) && (spouse.isDivorceable()) && (!spouse.getPrimaryRole().isCivilian())) {
                            if ((person.getStatus().isDefected()) || (randomInt(6) == 0)) {
                                getPlayerForce().getHumanResources()
                                      .getDivorce()
                                      .divorce(this, getLocalDate(), person, SplittingSurnameStyle.WEIGHTED);

                                turnoverRetirementInformation.add(String.format(resources.getString("divorce.text"),
                                      person.getHyperlinkedFullTitle(),
                                      spouse.getHyperlinkedFullTitle()));
                            }
                        }
                    }

                    // This ensures children have a chance of following their parent into departure
                    // This needs to be after spouses, to ensure joint-departure spouses are
                    // factored in
                    for (Person child : person.getGenealogy().getChildren()) {
                        if ((child.isChild(getLocalDate())) && (!child.getStatus().isDepartedUnit())) {
                            boolean hasRemainingParent = child.getGenealogy()
                                                               .getParents()
                                                               .stream()
                                                               .anyMatch(parent -> (!parent.getStatus()
                                                                                           .isDepartedUnit()) &&
                                                                                         (!parent.getStatus()
                                                                                                 .isAbsent()));

                            // if there is a remaining parent, there is a 50/50 chance the child departs
                            if ((hasRemainingParent) && (randomInt(2) == 0)) {
                                addReport(PERSONNEL, child.getHyperlinkedFullTitle() +
                                                           ' ' +
                                                           resources.getString("turnoverJointDepartureChild.text"));
                                child.changeStatus(this, getLocalDate(), PersonnelStatus.LEFT);

                                turnoverRetirementInformation.add(child.getHyperlinkedFullTitle() +
                                                                        ' ' +
                                                                        resources.getString(
                                                                              "turnoverJointDepartureChild.text"));
                            }

                            // if there is no remaining parent, the child will always depart, unless the
                            // parents are dead
                            if ((!hasRemainingParent) && (child.getGenealogy().hasLivingParents())) {
                                addReport(PERSONNEL, child.getHyperlinkedFullTitle() +
                                                           ' ' +
                                                           resources.getString("turnoverJointDepartureChild.text"));
                                child.changeStatus(this, getLocalDate(), PersonnelStatus.LEFT);

                                turnoverRetirementInformation.add(child.getHyperlinkedFullTitle() +
                                                                        ' ' +
                                                                        resources.getString(
                                                                              "turnoverJointDepartureChild.text"));
                            } else if (!child.getGenealogy().hasLivingParents()) {
                                addReport(PERSONNEL, child.getHyperlinkedFullTitle() + ' ' + resources.getString(
                                      "orphaned.text"));

                                turnoverRetirementInformation.add(child.getHyperlinkedFullTitle() +
                                                                        ' ' +
                                                                        resources.getString("orphaned.text"));
                                ServiceLogger.orphaned(person, getLocalDate());
                            }
                        }
                    }

                    if (unitAssignments.containsKey(pid)) {
                        removeUnit(unitAssignments.get(pid));
                    }
                }
                getPlayerForce().getHumanResources().getRetirementDefectionTracker().resolveAllContracts();
                return true;
            }
        } else {
            addReport(FINANCES, "<font color='" +
                                      ReportingUtilities.getNegativeColor() +
                                      "'>You cannot afford to make the final payments.</font>");
            return false;
        }

        return true;
    }

    public CampaignSummary getCampaignSummary() {
        return campaignSummary;
    }

    public News getNews() {
        return news;
    }

    /**
     * This is used by the XML loader. The id should already be set for this scenario so don't increment
     *
     * @param scenario Scenario to Add.
     */
    public void importScenario(Scenario scenario) {
        lastScenarioId = max(lastScenarioId, scenario.getId());
        scenarios.put(scenario.getId(), scenario);
    }

    // region Missions/Contracts

    /**
     * Add a mission to the campaign
     *
     * @param mission The mission to be added
     */
    public void addMission(AbstractContract mission) {
        mission.setCampaignOptions(getCampaignOptions());
        contractHistory.contractHistory().put(mission.getId(), mission);
        MekHQ.triggerEvent(new MissionNewEvent(mission));
    }

    /**
     * Imports a {@link AbstractContract} into a campaign.
     *
     * @param mission Mission to import into the campaign.
     */
    public void importMission(final AbstractContract mission) {
        mission.setCampaignOptions(getCampaignOptions());
        mission.getScenarios().forEach(this::importScenario);
        contractHistory.contractHistory().put(mission.getId(), mission);
        MekHQ.triggerEvent(new MissionNewEvent(mission));
        StratConContractInitializer.restoreTransientStratconInformation(mission, this);
    }

    public ContractHistoryData getContractHistoryData() {
        return contractHistory;
    }

    public LinkedHashMap<UUID, AbstractContract> getContractHistoryAsMap() {
        return contractHistory.contractHistory();
    }

    public @jakarta.annotation.Nullable AbstractContract getContract(UUID contractId) {
        return contractHistory.get(contractId);
    }

    /**
     * @return missions sorted with active missions from oldest to newest, followed by completed missions from newest to
     *       oldest; active missions without a start date use the campaign date, while completed missions without one
     *       sort last
     */
    public List<AbstractContract> getSortedContracts() {
        return contractHistory.getSortedMissions(currentDay);
    }

    @Deprecated(since = "0.51.01", forRemoval = true)
    public List<AbstractContract> getActiveMissions() {
        return contractHistory.getActiveIncludingNotYetStarted();
    }

    public List<AbstractContract> getCompletedContracts() {
        return contractHistory.getCompleted();
    }

    /**
     * Retrieves a list of currently active contracts.
     *
     * <p>This method is a shorthand for {@link #getActiveContracts(boolean)} with {@code includeFutureContracts}
     * set to {@code false}. It fetches all contracts from the list of missions and filters them for those that are
     * currently active on the current local date.</p>
     *
     * @return A list of {@link AbstractContract} objects that are currently active.
     */
    public List<AbstractContract> getActiveContracts() {
        return getActiveContracts(false);
    }

    /**
     * Retrieves a list of active contracts, with an option to include future contracts.
     *
     * @param includeFutureContracts If {@code true}, contracts that are scheduled to start in the future will also be
     *                               included in the final result. If {@code false}, only contracts active on the
     *                               current local date are included.
     *
     * @return A list of {@link AbstractContract} objects that match the active criteria.
     */
    public List<AbstractContract> getActiveContracts(boolean includeFutureContracts) {
        return includeFutureContracts ? contractHistory.getActiveIncludingNotYetStarted() :
                     contractHistory.getActiveAndStarted(currentDay);
    }

    /**
     * Retrieves a list of future contracts.
     *
     * <p>This method fetches all missions and checks if they are instances of {@link AbstractContract}. It filters the
     * contracts where the start date is after the current day.</p>
     *
     * @return A list of {@link AbstractContract} objects whose start dates are in the future.
     */
    public List<AbstractContract> getFutureContracts() {
        return contractHistory.getActiveAndNotYetStarted(currentDay);
    }

    /**
     * Determines whether there is an active AtB (Against the Bot) contract. This method checks if there are contracts
     * currently active. Optionally, it can also consider future contracts that have been accepted but have not yet
     * started.
     *
     * @param includeFutureContracts a boolean indicating whether contracts that have been accepted but have not yet
     *                               started should also be considered as active.
     *
     * @return {@code true} if there is any currently active AtB contract, or if {@code includeFutureContracts} is
     *       {@code true} and there are future contracts starting after the current date. Otherwise, {@code false}.
     *
     * @see #hasFutureContract()
     */
    public boolean hasActiveAtBContract(boolean includeFutureContracts) {
        if (!getActiveContracts().isEmpty()) {
            return true;
        }

        if (includeFutureContracts) {
            return hasFutureContract();
        }

        return false;
    }

    /**
     * Checks if there is at least one active AtB (Against the Bot) contract, using the default search parameters.
     *
     * @return {@code true} if an active AtB contract exists; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.06
     */
    public boolean hasActiveAtBContract() {
        return hasActiveAtBContract(false);
    }

    public boolean hasFutureContract() {
        return !getFutureContracts().isEmpty();
    }

    /**
     * @return whether the current campaign has an active contract for the current date
     */
    public boolean hasActiveContract() {
        return hasActiveContract;
    }

    /**
     * This is used to check if the current campaign has one or more active contacts, and sets the value of
     * hasActiveContract based on that check. This value should not be set elsewhere
     */
    public void setHasActiveContract() {
        hasActiveContract = getActiveContracts().size() > 0;
    }
    // endregion Missions/Contracts

    /**
     * Adds scenario to existing mission, generating a report.
     */
    public void addScenario(Scenario scenario, AbstractContract mission) {
        addScenario(scenario, mission, false);
    }

    /**
     * Add scenario to an existing mission. This method will also assign the scenario an id, provided that it is a new
     * scenario. It then adds the scenario to the scenarioId hash.
     * <p>
     * Scenarios with previously set ids can be sent to this mission, allowing one to remove and then re-add scenarios
     * if needed.
     *
     * @param scenario       - the Scenario to add
     * @param mission        - the mission to add the new scenario to
     * @param suppressReport - whether to suppress the campaign report
     */
    public void addScenario(Scenario scenario, AbstractContract mission, boolean suppressReport) {
        final boolean newScenario = scenario.getId() == Scenario.S_DEFAULT_ID;
        final int id = newScenario ? ++lastScenarioId : scenario.getId();
        scenario.setId(id);
        mission.addScenario(scenario);
        scenarios.put(id, scenario);

        if (newScenario && !suppressReport) {
            addReport(BATTLE, MessageFormat.format(resources.getString("newAtBScenario.format"),
                  scenario.getHyperlinkedName(),
                  MekHQ.getMHQOptions().getDisplayFormattedDate(scenario.getDate())));
        }

        MekHQ.triggerEvent(new ScenarioNewEvent(scenario));
    }

    public Scenario getScenario(int id) {
        return scenarios.get(id);
    }

    public Collection<Scenario> getScenarios() {
        return scenarios.values();
    }

    public List<Scenario> getActiveScenarios() {
        return scenarios.values().stream().filter(s -> s.getStatus().isCurrent()).toList();
    }

    public void setLocation(AbstractLocation location) {
        getPlayerForce().getDetachmentLocationManager().setLocation(getCampaignLocationManager(), location);
    }

    @Nonnull
    public CampaignLocationManager getCampaignLocationManager() {
        return locationManager;
    }

    /**
     * Returns the player's active force — the {@link PlayerForce} this campaign is played through, which owns the
     * faction identity, finances, reputation, and the hangar/warehouse/personnel.
     */
    @Nonnull
    public PlayerForce getPlayerForce() {
        return playerForce;
    }

    @Override
    public megamek.common.enums.Faction getTechFaction() {
        return getPlayerForce().getTechFaction();
    }

    // The campaign is no longer an ILocation/IPlace itself; the main force (PlayerForce) is the location node.
    // These convenience accessors delegate to it so callers can still ask the campaign about its position.

    public boolean isOnContractAndPlanetside() {
        boolean isOnContract = !getActiveContracts().isEmpty();
        boolean isPlanetside = getPlayerForce().getForceDetachment().isOnPlanet();
        return isPlanetside && isOnContract;
    }

    public List<String> getTurnoverRetirementInformation() {
        return turnoverRetirementInformation;
    }

    public TransportCostCalculations getTransportCostCalculation(int crewExperienceLevel) {
        // Units queued for travel elsewhere (e.g. left behind at a base via the jump-blocker prompt) still sit in
        // the hangar until the queue is dispatched next day, but must not be billed as traveling with the campaign.
        List<Unit> travelingUnits = getPlayerForce().getHangar().getUnits().stream()
                                          .filter(unit -> !getCampaignLocationManager().isQueuedForTravel(unit))
                                          .toList();
        return new TransportCostCalculations(travelingUnits,
              LocalWarehouse.getSpareParts(getParts()),
              getPlayerForce().getHumanResources().getPersonnelFilteringOutDepartedAndAbsent(),
              crewExperienceLevel);
    }

    /**
     * Imports a {@link Unit} into a campaign.
     *
     * @param unit A {@link Unit} to import into the campaign.
     */
    public void importUnit(Unit unit) {
        Objects.requireNonNull(unit);

        LOGGER.debug("Importing unit: ({}): {}", unit.getId(), unit.getName());

        getPlayerForce().getHangar().addUnit(unit);

        checkDuplicateNamesDuringAdd(unit.getEntity());

        // Assign an entity ID to our new unit
        if (Entity.NONE == unit.getEntity().getId()) {
            unit.getEntity().setId(game.getNextEntityId());
        }

        // Entity should exist before we initialize transport space
        game.addEntity(unit.getEntity());

        unit.initializeAllTransportSpace();

        if (!unit.isMothballed()) {
            for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
                if (!unit.getTransportCapabilities(campaignTransportType).isEmpty()) {
                    addCampaignTransport(campaignTransportType, unit);
                }
            }
        }

    }

    /**
     * Adds a transport (Unit) to the list specified transporters map. This transporters map is used to store
     * transports, the kinds of transporters they have, and their remaining capacity. The transporters map is meant to
     * be utilized by the GUI.
     *
     * @param campaignTransportType Transport Type (enum) we're adding to
     * @param unit                  unit with transport capabilities
     *
     * @see CampaignTransporterMap
     */
    public void addCampaignTransport(CampaignTransportType campaignTransportType, Unit unit) {
        if (campaignTransportType.isShipTransport()) {
            shipTransporters.addTransporter(unit);
        } else if (campaignTransportType.isTacticalTransport()) {
            tacticalTransporters.addTransporter(unit);
        } else if (campaignTransportType.isTowTransport()) {
            towTransporters.addTransporter(unit);
        }
    }

    /**
     * This will update the transport in the transports list with current capacities. When a unit is added or removed
     * from a transport, that information needs updated in the campaign transport map. This method will update the map
     * for every {@code CampaignTransportType} for the given transport.
     *
     * @param transport Unit
     *
     * @see Campaign#updateTransportInTransports(CampaignTransportType, Unit)
     */
    public void updateTransportInTransports(Unit transport) {
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            updateTransportInTransports(campaignTransportType, transport);
        }
    }

    /**
     * This will update the transport in the transports list with current capacities. When a unit is added or removed
     * from a transport, that information needs updated in the campaign transport map. This method takes the
     * CampaignTransportType and transport as inputs and updates the map with the current capacities of the transport.
     *
     * @param campaignTransportType type (Enum) of TransportedUnitsSummary we're interested in
     * @param transport             Unit
     */
    public void updateTransportInTransports(CampaignTransportType campaignTransportType, Unit transport) {
        Objects.requireNonNull(getCampaignTransporterMap(campaignTransportType))
              .updateTransportInTransporterMap(transport);
    }

    /**
     * Deletes an entry from the list of specified list of transports. This gets updated when the transport should no
     * longer be in the CampaignTransporterMap, such as when Transport is mothballed or removed from the campaign.
     *
     * @param campaignTransportType Transport Type (enum) we're checking
     * @param unit                  - The ship we want to remove from this Set
     *
     * @see CampaignTransporterMap
     */
    public void removeCampaignTransporter(CampaignTransportType campaignTransportType, Unit unit) {
        if (campaignTransportType.isShipTransport()) {
            shipTransporters.removeTransport(unit);
        } else if (campaignTransportType.isTacticalTransport()) {
            tacticalTransporters.removeTransport(unit);
        } else if (campaignTransportType.isTowTransport()) {
            towTransporters.removeTransport(unit);
        }
    }

    /**
     * This is for adding a TestUnit that was previously created and had parts added to it. We need to do the normal
     * stuff, but we also need to take the existing parts and add them to the campaign.
     *
     * @param testUnit     TestUnit to add.
     * @param deliveryTime How many days until the unit arrives
     */
    public void addTestUnit(TestUnit testUnit, int deliveryTime) {
        // we really just want the entity and the parts so let's just wrap that around a new unit.
        Unit unit = new Unit(testUnit.getEntity(), this);
        getPlayerForce().getHangar().addUnit(unit);

        // record who the unit was salvaged from before we take ownership of it
        final String formerOwner = (unit.getEntity().getOwner() != null) ? unit.getEntity().getOwner().getName() : null;
        if ((formerOwner != null) && !formerOwner.isBlank()) {
            UnitLogger.salvagedFrom(unit, getLocalDate(), formerOwner);
        } else {
            UnitLogger.acquired(unit, getLocalDate(), UnitAcquisitionType.SALVAGED);
        }

        // we decided we like the test unit so much we are going to keep it
        unit.getEntity().setOwner(player);
        unit.getEntity().setGame(game);
        unit.getEntity().setExternalIdAsString(unit.getId().toString());
        if (!unit.isSelfCrewed()) {
            unit.setMaintenanceMultiplier(getCampaignOptions().get(CampaignOption.DEFAULT_MAINTENANCE_TIME));
        }

        // now lets grab the parts from the test unit and set them up with this unit
        for (Part p : testUnit.getParts()) {
            unit.addPart(p);
            getQuartermaster().addPart(p, deliveryTime, false);
        }

        unit.resetPilotAndEntity();

        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }

        // Assign an entity ID to our new unit
        if (Entity.NONE == unit.getEntity().getId()) {
            unit.getEntity().setId(game.getNextEntityId());
        }
        game.addEntity(unit.getEntity());

        checkDuplicateNamesDuringAdd(unit.getEntity());
        addReport(ACQUISITIONS, unit.getHyperlinkedName() + " has been added to the unit roster.");
    }

    /**
     * Add a new unit to the campaign and set its quality to D.
     *
     * @param en             An <code>Entity</code> object that the new unit will be wrapped around
     * @param allowNewPilots A boolean indicating whether to add new pilots for the unit
     * @param days           The number of days for the new unit to arrive
     *
     * @return The newly added unit
     */
    public Unit addNewUnit(Entity en, boolean allowNewPilots, int days) {
        return addNewUnit(en, allowNewPilots, days, PartQuality.QUALITY_D);
    }

    /**
     * Add a new unit to the campaign and set its quality, without recording a specific form of acquisition.
     *
     * @param en             An <code>Entity</code> object that the new unit will be wrapped around
     * @param allowNewPilots A boolean indicating whether to add new pilots for the unit
     * @param days           The number of days for the new unit to arrive
     * @param quality        The quality of the new unit (0-5)
     *
     * @return The newly added unit
     *
     * @throws IllegalArgumentException If the quality is not within the valid range (0-5)
     */
    public Unit addNewUnit(Entity en, boolean allowNewPilots, int days, PartQuality quality) {
        return addNewUnit(en, allowNewPilots, days, quality, UnitAcquisitionType.ACQUIRED);
    }

    /**
     * Add a new unit to the campaign and set its quality.
     *
     * <p>The acquisition is recorded in the new unit's log, so every unit added through this method carries the form
     * and date by which it entered the campaign.</p>
     *
     * @param en              An <code>Entity</code> object that the new unit will be wrapped around
     * @param allowNewPilots  A boolean indicating whether to add new pilots for the unit
     * @param days            The number of days for the new unit to arrive
     * @param quality         The quality of the new unit (0-5)
     * @param acquisitionType The means by which the campaign acquired the unit
     *
     * @return The newly added unit
     *
     * @throws IllegalArgumentException If the quality is not within the valid range (0-5)
     */
    public Unit addNewUnit(Entity en, boolean allowNewPilots, int days, PartQuality quality,
          UnitAcquisitionType acquisitionType) {
        Unit unit = new Unit(en, this);
        if (!unit.isSelfCrewed()) {
            unit.setMaintenanceMultiplier(getCampaignOptions().get(CampaignOption.DEFAULT_MAINTENANCE_TIME));
        }
        getPlayerForce().getHangar().addUnit(unit);

        // reset the game object
        en.setOwner(player);
        en.setGame(game);
        en.setExternalIdAsString(unit.getId().toString());

        // Added to avoid the 'default formation bug' when calculating cargo
        getPlayerForce().removeUnitFromFormation(unit, this);

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }

        unit.setDaysToArrival(days);

        if (days > 0) {
            unit.setMothballed(campaignOptions.get(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES));
        }

        if (allowNewPilots) {
            Map<CrewType, Collection<Person>> newCrew = Utilities.genRandomCrewWithCombinedSkill(this,
                  unit,
                  getPlayerForce().getFaction().getShortName());
            newCrew.forEach((type, personnel) -> personnel.forEach(p -> type.getAddMethod().accept(unit, p)));
        }

        unit.resetPilotAndEntity();

        unit.setQuality(quality);

        // Assign an entity ID to our new unit
        if (Entity.NONE == en.getId()) {
            en.setId(game.getNextEntityId());
        }
        game.addEntity(en);

        unit.initializeAllTransportSpace();

        if (!unit.isMothballed()) {
            for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
                if (!unit.getTransportCapabilities(campaignTransportType).isEmpty()) {
                    addCampaignTransport(campaignTransportType, unit);
                }
            }
        }

        UnitLogger.acquired(unit, getLocalDate(), acquisitionType);

        checkDuplicateNamesDuringAdd(en);
        addReport(ACQUISITIONS, unit.getHyperlinkedName() + " has been added to the unit roster.");
        MekHQ.triggerEvent(new UnitNewEvent(unit));

        return unit;
    }

    /**
     * Gets statistics related to units in the hangar.
     */
    public HangarStatistics getHangarStatistics() {
        return new HangarStatistics(getPlayerForce().getHangar());
    }

    /**
     * This method calculates the cost per jump for interstellar travel. It operates by fitting the part of the force
     * not transported in owned DropShips into a number of prototypical DropShips of a few standard configurations, then
     * adding the JumpShip charges on top. It remains fairly hacky, but improves slightly on the prior implementation as
     * far as following the rulebooks goes.
     * <p>
     * It can be used to calculate total travel costs in the style of FM:Mercs (excludeOwnTransports and
     * campaignOpsCosts set to false), to calculate leased/rented travel costs only in the style of FM:Mercs
     * (excludeOwnTransports true, campaignOpsCosts false), or to calculate travel costs for CampaignOps-style costs
     * (excludeOwnTransports true, campaignOpsCosts true).
     *
     * @param excludeOwnTransports If true, do not display maintenance costs in the calculated travel cost.
     * @param campaignOpsCosts     If true, use the Campaign Ops method for calculating travel cost. (DropShip monthly
     *                             fees of 0.5% of purchase cost, 100,000 C-bills per collar.)
     *
     * @deprecated used {@link TransportCostCalculations} instead
     */
    @Deprecated(since = "50.10", forRemoval = true)
    public Money calculateCostPerJump(boolean excludeOwnTransports, boolean campaignOpsCosts) {
        HangarStatistics stats = getHangarStatistics();
        CargoStatistics cargoStats = getCargoStatistics();

        Money collarCost = Money.of(campaignOpsCosts ? 100000 : 50000);

        // first we need to get the total number of units by type
        int nMek = stats.getNumberOfUnitsByType(Entity.ETYPE_MEK);
        int nLVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int nHVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int nAero = stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER);
        int nDropship = stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int nCollars = stats.getTotalDockingCollars();
        double nCargo = cargoStats.getTotalCargoCapacity(); // ignoring refrigerated/insulated/etc.

        // get cargo tonnage including parts in transit, then get mothballed unit tonnage
        double carriedCargo = cargoStats.getCargoTonnage(true, false) + cargoStats.getCargoTonnage(false, true);

        // calculate the number of units left not transported
        int noMek = max(nMek - stats.getOccupiedBays(Entity.ETYPE_MEK), 0);
        int noASF = max(nAero - stats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER), 0);
        int noLV = max(nLVee - stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int noHV = max(nHVee - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        //TODO: Do capacity calculations for Infantry, too.
        int freeHV = max(stats.getTotalHeavyVehicleBays() - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noCargo = (int) Math.ceil(max(carriedCargo - nCargo, 0));

        int newNoLV = max(noLV - freeHV, 0);
        int noVehicles = (noHV + newNoLV);

        Money dropshipCost;
        // The cost-figuring process: using prototypical drop-ships, figure out how many collars are required. Charge
        // for the prototypical drop-ships and the docking collar, based on the rules selected. Allow prototypical
        // drop-ships to be leased in 1/2 increments; designs of roughly 1/2 size exist for all the prototypical
        // variants chosen.

        // DropShip costs are for the duration of the trip for FM:Mercs rules, and per month for Campaign Ops. The
        // prior implementation here assumed the FM:Mercs costs were per jump, which seems reasonable. To avoid
        // having to add a bunch of code to remember the total length of the current jump path, CamOps costs are
        // normalized to per-jump, using 175 hours charge time as a baseline.

        // Roughly an Overlord
        int largeMekDropshipMekCapacity = 36;
        int largeMekDropshipASFCapacity = 6;
        int largeMekDropshipCargoCapacity = 120;
        Money largeMekDropshipCost = Money.of(campaignOpsCosts ? (1750000.0 / 4.2) : 400000);

        // Roughly a Union
        int averageMekDropshipMekCapacity = 12;
        int averageMekDropshipASFCapacity = 2;
        int averageMekDropshipCargoCapacity = 75;
        Money averageMekDropshipCost = Money.of(campaignOpsCosts ? (1450000.0 / 4.2) : 150000);

        // Roughly a Leopard
        int smallMekDropshipMekCapacity = 4;
        int smallMekDropshipASFCapacity = 2;
        int smallMekDropshipCargoCapacity = 5;
        Money smallMekDropshipCost = Money.of(campaignOpsCosts ? (750000.0 / 4.2) : 60000);

        // Roughly a Leopard CV
        int smallASFDropshipASFCapacity = 6;
        int smallASFDropshipCargoCapacity = 90;
        Money smallASFDropshipCost = Money.of(campaignOpsCosts ? (900000.0 / 4.2) : 80000);

        // Roughly a Triumph
        int largeVehicleDropshipVehicleCapacity = 50;
        int largeVehicleDropshipCargoCapacity = 750;
        Money largeVehicleDropshipCost = Money.of(campaignOpsCosts ? (1750000.0 / 4.2) : 430000);

        // Roughly a Gazelle
        int avgVehicleDropshipVehicleCapacity = 15;
        int avgVehicleDropshipCargoCapacity = 65;
        Money avgVehicleDropshipCost = Money.of(campaignOpsCosts ? (900000.0 / 4.2) : 40000);

        // Roughly a Mule
        int largeCargoDropshipCargoCapacity = 8000;
        Money largeCargoDropshipCost = Money.of(campaignOpsCosts ? (750000.0 / 4.2) : 800000);

        // Roughly a Buccaneer
        int avgCargoDropshipCargoCapacity = 2300;
        Money cargoDropshipCost = Money.of(campaignOpsCosts ? (550000.0 / 4.2) : 250000);

        int mekCollars = 0;
        double leasedLargeMekDropships = 0;
        double leasedAverageMekDropships = 0;
        double leasedSmallMekDropships = 0;

        int asfCollars = 0;
        double leasedSmallASFDropships = 0;

        int vehicleCollars = 0;
        double leasedLargeVehicleDropships = 0;
        double leasedAvgVehicleDropships = 0;

        int cargoCollars = 0;
        double leasedLargeCargoDropships = 0;
        double leasedAverageCargoDropships = 0;

        int leasedASFCapacity = 0;
        int leasedCargoCapacity = 0;

        // For each type we're concerned with, calculate the number of drop-ships needed to transport the force.
        // Smaller drop-ships are represented by half-dropships.

        // If we're transporting more than a company, Overlord or half-Overlord analogues are more efficient.
        if (noMek > largeMekDropshipMekCapacity / 3) {
            leasedLargeMekDropships = Math.round(2 * noMek / (double) largeMekDropshipMekCapacity) / 2.0;
            noMek -= (int) (leasedLargeMekDropships * largeMekDropshipMekCapacity);
            mekCollars += (int) Math.ceil(leasedLargeMekDropships);

            // If there's more than a company left over, lease another Overlord. Otherwise, fall through and get a Union.
            if (noMek > largeMekDropshipMekCapacity / 3) {
                if (noMek > largeMekDropshipMekCapacity / 2) {
                    leasedLargeMekDropships += 1;
                    noMek -= largeMekDropshipMekCapacity;
                } else {
                    leasedLargeMekDropships += 0.5;
                    noMek -= (int) (largeMekDropshipMekCapacity / 0.5);
                }
                mekCollars += 1;
            }

            leasedASFCapacity += (int) floor(leasedLargeMekDropships * largeMekDropshipASFCapacity);
            leasedCargoCapacity += largeMekDropshipCargoCapacity;
        }

        // Unions
        if (noMek > 4) {
            leasedAverageMekDropships = Math.round(2 * noMek / (double) averageMekDropshipMekCapacity) / 2.0;
            noMek -= (int) (leasedAverageMekDropships * averageMekDropshipMekCapacity);
            mekCollars += (int) Math.ceil(leasedAverageMekDropships);

            // If we can fit in a smaller DropShip, lease one of those instead.
            if ((noMek > 0) && (noMek < (averageMekDropshipMekCapacity / 2))) {
                leasedAverageMekDropships += 0.5;
                mekCollars += 1;
            } else if (noMek > 0) {
                leasedAverageMekDropships += 1;
                mekCollars += 1;
            }

            // Our Union-ish DropShip can carry some ASFs and cargo.
            leasedASFCapacity += (int) floor(leasedAverageMekDropships * averageMekDropshipASFCapacity);
            leasedCargoCapacity += (int) floor(leasedAverageMekDropships * averageMekDropshipCargoCapacity);
        }

        // Leopards for the rest, no halves here
        if (noMek > 0) {
            leasedSmallMekDropships = Math.ceil(noMek / (double) smallMekDropshipMekCapacity);
            mekCollars += (int) Math.ceil(leasedSmallMekDropships);
        }
        leasedASFCapacity += (int) floor(leasedSmallMekDropships * smallMekDropshipASFCapacity);
        leasedCargoCapacity += (int) floor(leasedSmallMekDropships * smallMekDropshipCargoCapacity);

        // Leopard CVs are (generally) the most efficient for raw wing transports even with collar fees
        if (noASF > leasedASFCapacity) {
            noASF -= leasedASFCapacity;

            if (noASF > 0) {
                leasedSmallASFDropships = Math.round(2 * noASF / (double) smallASFDropshipASFCapacity) / 2.0;
                noASF -= (int) (leasedSmallASFDropships * smallASFDropshipASFCapacity);
                asfCollars += (int) Math.ceil(leasedSmallASFDropships);

                if ((noASF > 0) && (noASF < (smallASFDropshipASFCapacity / 2))) {
                    leasedSmallASFDropships += 0.5;
                    asfCollars += 1;
                } else if (noASF > 0) {
                    leasedSmallASFDropships += 1;
                    asfCollars += 1;
                }
            }

            // Our Leopard-ish DropShip can carry some cargo.
            leasedCargoCapacity += (int) floor(leasedSmallASFDropships * smallASFDropshipCargoCapacity);
        }

        // Triumphs
        if (noVehicles > avgVehicleDropshipVehicleCapacity) {
            leasedLargeVehicleDropships = Math.round(2 * noVehicles / (double) largeVehicleDropshipVehicleCapacity) /
                                                2.0;
            noVehicles -= (int) (leasedLargeVehicleDropships * largeVehicleDropshipVehicleCapacity);
            vehicleCollars += (int) Math.ceil(leasedLargeVehicleDropships);

            if (noVehicles > avgVehicleDropshipVehicleCapacity) {
                leasedLargeVehicleDropships += 1;
                noVehicles -= largeVehicleDropshipVehicleCapacity;
                vehicleCollars += 1;
            }

            leasedCargoCapacity += (int) floor(leasedLargeVehicleDropships * largeVehicleDropshipCargoCapacity);
        }

        // Gazelles are pretty minimal, so no halfsies.
        if (noVehicles > 0) {
            leasedAvgVehicleDropships = Math.ceil((noHV + newNoLV) / (double) avgVehicleDropshipVehicleCapacity);
            noVehicles = (int) ((noHV + newNoLV) - leasedAvgVehicleDropships * avgVehicleDropshipVehicleCapacity);
            vehicleCollars += (int) Math.ceil(leasedAvgVehicleDropships);

            if (noVehicles > 0) { //shouldn't be necessary, but check?
                leasedAvgVehicleDropships += 1;
                vehicleCollars += 1;
            }

            // Our Gazelle-ish DropShip can carry some cargo.
            leasedCargoCapacity += (int) floor(avgVehicleDropshipCargoCapacity * leasedAvgVehicleDropships);
        }

        // Do we have any leftover cargo?
        noCargo -= leasedCargoCapacity;

        // Mules
        if (noCargo > avgCargoDropshipCargoCapacity) {
            leasedLargeCargoDropships = Math.round(2 * noCargo / (double) largeCargoDropshipCargoCapacity) / 2.0;
            noCargo -= (int) (leasedLargeCargoDropships * largeCargoDropshipCargoCapacity);
            cargoCollars += (int) Math.ceil(leasedLargeCargoDropships);

            if (noCargo > avgCargoDropshipCargoCapacity) {
                leasedLargeCargoDropships += 1;
                noCargo -= largeCargoDropshipCargoCapacity;
                cargoCollars += 1;
            }
        }

        // Buccaneers
        if (noCargo > 0) {
            leasedAverageCargoDropships = Math.round(2 * noCargo / (double) avgCargoDropshipCargoCapacity) / 2.0;
            cargoCollars += (int) Math.ceil(leasedAverageCargoDropships);
            noCargo -= (int) (leasedAverageCargoDropships * avgCargoDropshipCargoCapacity);

            if (noCargo > 0 && noCargo < (avgCargoDropshipCargoCapacity / 2)) {
                leasedAverageCargoDropships += 0.5;
                cargoCollars += 1;
            } else if (noCargo > 0) {
                leasedAverageCargoDropships += 1;
                cargoCollars += 1;
            }
        }

        dropshipCost = largeMekDropshipCost.multipliedBy(leasedLargeMekDropships);
        dropshipCost = dropshipCost.plus(averageMekDropshipCost.multipliedBy(leasedAverageMekDropships));
        dropshipCost = dropshipCost.plus(smallMekDropshipCost.multipliedBy(leasedSmallMekDropships));

        dropshipCost = dropshipCost.plus(smallASFDropshipCost.multipliedBy(leasedSmallASFDropships));

        dropshipCost = dropshipCost.plus(avgVehicleDropshipCost.multipliedBy(leasedAvgVehicleDropships));
        dropshipCost = dropshipCost.plus(largeVehicleDropshipCost.multipliedBy(leasedLargeVehicleDropships));

        dropshipCost = dropshipCost.plus(cargoDropshipCost.multipliedBy(leasedAverageCargoDropships));
        dropshipCost = dropshipCost.plus(largeCargoDropshipCost.multipliedBy(leasedLargeCargoDropships));

        // Smaller/half-DropShips are cheaper to rent, but still take one collar each
        int collarsNeeded = mekCollars + asfCollars + vehicleCollars + cargoCollars;

        // add owned DropShips
        collarsNeeded += nDropship;

        // now factor in owned JumpShips
        collarsNeeded = max(0, collarsNeeded - nCollars);

        Money totalCost = dropshipCost.plus(collarCost.multipliedBy(collarsNeeded));

        // FM:Mercs reimburses for owned transport (CamOps handles it in peacetime
        // costs)
        if (!excludeOwnTransports) {
            Money ownDropshipCost = Money.zero();
            Money ownJumpshipCost = Money.zero();
            for (Unit u : getUnits()) {
                if (!u.isMothballed()) {
                    Entity e = u.getEntity();
                    if ((e.getEntityType() & Entity.ETYPE_DROPSHIP) != 0) {
                        ownDropshipCost = ownDropshipCost.plus(averageMekDropshipCost.multipliedBy(u.getMekCapacity())
                                                                     .dividedBy(averageMekDropshipMekCapacity));
                        ownDropshipCost = ownDropshipCost.plus(smallASFDropshipCost.multipliedBy(u.getASFCapacity())
                                                                     .dividedBy(smallASFDropshipASFCapacity));
                        ownDropshipCost = ownDropshipCost.plus(avgVehicleDropshipCost.multipliedBy(u.getHeavyVehicleCapacity() +
                                                                                                         u.getLightVehicleCapacity())
                                                                     .dividedBy(avgVehicleDropshipVehicleCapacity));
                        ownDropshipCost = ownDropshipCost.plus(cargoDropshipCost.multipliedBy(u.getCargoCapacity())
                                                                     .dividedBy(avgCargoDropshipCargoCapacity));
                    } else if ((e.getEntityType() & Entity.ETYPE_JUMPSHIP) != 0) {
                        ownJumpshipCost = ownDropshipCost.plus(collarCost.multipliedBy(e.getDockingCollars().size()));
                    }
                }
            }

            totalCost = totalCost.plus(ownDropshipCost).plus(ownJumpshipCost);
        }

        Person negotiator = getPlayerForce().getHumanResources()
                                  .getSeniorAdminPerson(getCampaignOptions(),
                                        getPlayerForce().isClanForce(),
                                        getLocalDate());
        if (negotiator != null) {
            PersonnelOptions options = negotiator.getOptions();
            if (options.booleanOption(ADMIN_INTERSTELLAR_NEGOTIATOR) && totalCost.isPositive()) {
                totalCost = totalCost.multipliedBy(0.85);
            }
        }

        return totalCost;
    }

    /**
     * Gets statistics related to cargo in the hangar.
     */
    public CargoStatistics getCargoStatistics() {
        return new CargoStatistics(this);
    }

    public Collection<Unit> getUnits() {
        return getPlayerForce().getHangar().getUnits();
    }

    /**
     * @return All player's units in {@code campaign}, not just the ones located with the main force.
     */
    public Collection<Unit> getAllUnits() {
        List<Unit> units = new ArrayList<>();
        for (AbstractLocation location : getCampaignLocationManager().getLocations()) {
            Set<Unit> found = location.fetchUnitsAtLocation();
            units.addAll(found);
        }
        return units;
    }

    /**
     * Retrieves a collection of units that are not mothballed or being salvaged.
     *
     * @return a collection of active units
     */
    public Collection<Unit> getActiveUnits() {
        return getPlayerForce().getHangar()
                     .getUnits().stream().filter(unit -> !unit.isMothballed() && !unit.isSalvage()).toList();
    }

    public List<Entity> getEntities() {
        return getUnits().stream().map(Unit::getEntity).collect(Collectors.toList());
    }

    public Unit getUnit(UUID id) {
        Unit unit = getPlayerForce().getHangar().getUnit(id);
        if (unit != null) {
            return unit;
        }
        for (PlayerBase base : getCampaignLocationManager().getPlayerBases()) {
            unit = base.getBaseHangar().getUnit(id);
            if (unit != null) {
                return unit;
            }
        }
        return null;
    }

    // region Personnel
    // region Person Creation

    // endregion Person Creation

    // region Personnel Recruitment

    // endregion Personnel Recruitment

    // region Bloodnames

    // endregion Bloodnames

    // region Other Personnel Methods

    /**
     * Imports a {@link Person} into a campaign.
     *
     * <p><b>Notes:</b> This is a super lightweight way of adding a character to the campaign. It doesn't include
     * all the extra steps that the various {@code recruitPerson(Person)} methods need to go through. That makes this
     * method particularly useful for inclusion in Unit Tests.</p>
     *
     * @param person A {@link Person} to import into the campaign.
     */
    public void importPerson(Person person) {
        getPlayerForce().getHumanResources().importPerson(person);
        person.setParent(getPlayerForce().getPersonnel());
    }

    // endregion Other Personnel Methods

    // region Personnel Selectors and Generators

    // endregion Personnel Selectors and Generators
    // endregion Personnel

    /**
     * List of all units that can show up in the repair bay.
     */
    public List<Unit> getServiceableUnits() {
        List<Unit> service = new ArrayList<>();
        for (Unit u : getUnits()) {
            if (u.isAvailable() && u.isServiceable() && !StratConRulesManager.isUnitDeployedToStratCon(u)) {
                service.add(u);
            }
        }
        return service;
    }

    /**
     * Imports a collection of parts into the campaign.
     *
     * @param newParts The collection of {@link Part} instances to import into the campaign.
     */
    public void importParts(Collection<Part> newParts) {
        Objects.requireNonNull(newParts);

        for (Part p : newParts) {
            if ((p instanceof MissingPart) && (null == p.getUnit())) {
                // Let's not import missing parts without a valid unit.
                continue;
            }

            // Track this part as part of our Campaign
            p.setCampaign(this);

            // Add the part to the campaign, but do not
            // merge it with any existing parts
            getPlayerForce().getWarehouse().addPart(p, false);
        }
    }

    public ForceQuartermaster getQuartermaster() {
        return quartermaster;
    }

    /**
     * @return A collection of parts in the Warehouse.
     */
    public Collection<Part> getParts() {
        return getPlayerForce().getWarehouse().getParts();
    }

    public Part getPart(int id) {
        return getPlayerForce().getWarehouse().getPart(id);
    }

    /**
     * @return All player's parts in {@code campaign}, not just the ones located with the main force.
     */
    public Collection<Part> getAllParts() {
        List<Part> parts = new ArrayList<>();
        for (AbstractLocation location : getCampaignLocationManager().getLocations()) {
            Set<Part> found = location.fetchPartsAtLocation();
            parts.addAll(found);
        }
        return parts;
    }

    public DailyReportLog getDailyReportLog() {
        return dailyReportLog;
    }

    /***
     * This is the main function for getting stuff (parts, units, etc.) All non-GM
     * acquisition should go through this function to ensure the campaign rules for
     * acquisition are followed.
     *
     * @param sList - A <code>ShoppingList</code> object including items that need
     *              to be purchased
     * @return A <code>ShoppingList</code> object that includes all items that were
     *         not successfully acquired
     */
    public ForceShoppingList goShopping(ForceShoppingList sList) {
        // loop through shopping items and decrement days to wait
        for (IAcquisitionWork shoppingItem : sList.getShoppingList()) {
            shoppingItem.decrementDaysToWait();
        }

        if (getCampaignOptions().get(CampaignOption.ACQUISITIONS_TYPE) == AcquisitionsType.AUTOMATIC) {
            return goShoppingAutomatically(sList);
        } else if (!getCampaignOptions().get(CampaignOption.USE_PLANETARY_ACQUISITION)) {
            return goShoppingStandard(sList);
        } else {
            return goShoppingByPlanet(sList);
        }
    }

    /**
     * Shops for items on the {@link ForceShoppingList}, where each acquisition automatically succeeds.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ForceShoppingList goShoppingAutomatically(ForceShoppingList sList) {
        List<IAcquisitionWork> currentList = new ArrayList<>(sList.getShoppingList());

        List<IAcquisitionWork> remainingItems = new ArrayList<>(currentList.size());
        for (IAcquisitionWork shoppingItem : currentList) {
            if (shoppingItem.getDaysToWait() <= 0) {
                while (shoppingItem.getQuantity() > 0) {
                    if (!acquireEquipment(shoppingItem, null)) {
                        shoppingItem.resetDaysToWait();
                        break;
                    }
                }
            }
            if (shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {
                remainingItems.add(shoppingItem);
            }
        }

        return new ForceShoppingList(remainingItems);
    }

    /**
     * Shops for items on the {@link ForceShoppingList}, where each acquisition is performed by available logistics
     * personnel.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ForceShoppingList goShoppingStandard(ForceShoppingList sList) {
        List<Person> logisticsPersonnel = getPlayerForce().getHumanResources()
                                                .getLogisticsPersonnel(getCampaignOptions(),
                                                      getPlayerForce().isClanForce(),
                                                      getLocalDate());
        if (logisticsPersonnel.isEmpty()) {
            addReport(ACQUISITIONS, "Your force has no one capable of acquiring equipment.");
            return sList;
        }

        List<IAcquisitionWork> currentList = new ArrayList<>(sList.getShoppingList());
        for (Person person : logisticsPersonnel) {
            if (currentList.isEmpty()) {
                // Nothing left to shop for!
                break;
            }

            List<IAcquisitionWork> remainingItems = new ArrayList<>(currentList.size());
            for (IAcquisitionWork shoppingItem : currentList) {
                if (shoppingItem.getDaysToWait() <= 0) {
                    while (canAcquireParts(person) && shoppingItem.getQuantity() > 0) {
                        if (!acquireEquipment(shoppingItem, person)) {
                            shoppingItem.resetDaysToWait();
                            break;
                        }
                    }
                }
                if (shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {
                    remainingItems.add(shoppingItem);
                }
            }

            currentList = remainingItems;
        }

        return new ForceShoppingList(currentList);
    }

    /**
     * Shops for items on the {@link ForceShoppingList}, where each acquisition is attempted on nearby planets by
     * available logistics personnel.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ForceShoppingList goShoppingByPlanet(ForceShoppingList sList) {
        List<Person> logisticsPersonnel = getPlayerForce().getHumanResources()
                                                .getLogisticsPersonnel(getCampaignOptions(),
                                                      getPlayerForce().isClanForce(),
                                                      getLocalDate());
        if (logisticsPersonnel.isEmpty()) {
            addReport(ACQUISITIONS, "Your force has no one capable of acquiring equipment.");
            return sList;
        }

        // we are shopping by planets, so more involved
        List<IAcquisitionWork> currentList = sList.getShoppingList();
        LocalDate currentDate = getLocalDate();

        // a list of items than can be taken out of the search and put back on the
        // shopping list
        List<IAcquisitionWork> shelvedItems = new ArrayList<>();

        // find planets within a certain radius - the function will weed out dead
        // planets
        List<PlanetarySystem> systems = this.systemsInstance
                                              .getShoppingSystems(getCurrentSystem(),
                                                    getCampaignOptions().get(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION),
                                                    currentDate);

        for (Person person : logisticsPersonnel) {
            if (currentList.isEmpty()) {
                // Nothing left to shop for!
                break;
            }

            String personTitle = person.getHyperlinkedFullTitle() + ' ';

            for (PlanetarySystem system : systems) {
                if (currentList.isEmpty()) {
                    // Nothing left to shop for!
                    break;
                }

                List<IAcquisitionWork> remainingItems = new ArrayList<>();

                // loop through shopping list. If it's time to check, then check as appropriate.
                // Items not
                // found get added to the remaining item list. Rotate through personnel
                boolean done = false;
                for (IAcquisitionWork shoppingItem : currentList) {
                    if (!canAcquireParts(person)) {
                        remainingItems.add(shoppingItem);
                        done = true;
                        continue;
                    }

                    if (shoppingItem.getDaysToWait() <= 0) {
                        PartAcquisitionResult result = findContactForAcquisition(shoppingItem, person, system);
                        if (result == PartAcquisitionResult.Success) {
                            int transitTime = calculatePartTransitTime(system);

                            PersonnelOptions options = person.getOptions();
                            double logisticianModifier = options.booleanOption(ADMIN_LOGISTICIAN) ? 0.9 : 1.0;
                            transitTime = (int) Math.round(transitTime * logisticianModifier);

                            int requestedQuantity = shoppingItem.getQuantity();
                            int totalQuantity = 0;
                            while (shoppingItem.getQuantity() > 0 &&
                                         canAcquireParts(person) &&
                                         acquireEquipment(shoppingItem, person, system, transitTime)) {
                                totalQuantity++;
                            }
                            // A bulk infantry order is filled by a single roll; report the full amount delivered.
                            if (shoppingItem.isBulkAcquisition()) {
                                totalQuantity = requestedQuantity - shoppingItem.getQuantity();
                            }
                            if (totalQuantity > 0) {
                                addReport(ACQUISITIONS, personTitle +
                                                              "<font color='" +
                                                              ReportingUtilities.getPositiveColor() +
                                                              "'><b> found " +
                                                              shoppingItem.getQuantityName(totalQuantity) +
                                                              " on " +
                                                              system.getPrintableName(currentDate) +
                                                              ". Delivery in " +
                                                              transitTime +
                                                              " days.</b></font>");
                            }
                        } else if (result == PartAcquisitionResult.PartInherentFailure) {
                            shelvedItems.add(shoppingItem);
                            continue;
                        }
                    }

                    // if we didn't find everything on this planet, then add to the remaining list
                    if (shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {
                        // if we can't afford it, then don't keep searching for it on other planets
                        if (!canPayFor(shoppingItem)) {
                            if (!getCampaignOptions().get(CampaignOption.PLANET_ACQUISITION_VERBOSE)) {
                                addReport(FINANCES, "<font color='" +
                                                          ReportingUtilities.getNegativeColor() +
                                                          "'><b>You cannot afford to purchase another " +
                                                          shoppingItem.getAcquisitionName() +
                                                          "</b></font>");
                            }
                            shelvedItems.add(shoppingItem);
                        } else {
                            remainingItems.add(shoppingItem);
                        }
                    }
                }

                // we are done with this planet. replace our current list with the remaining
                // items
                currentList = remainingItems;

                if (done) {
                    break;
                }
            }
        }

        // add shelved items back to the current list
        currentList.addAll(shelvedItems);

        // loop through and reset waiting time on all items on the remaining shopping
        // list if they have no waiting time left
        for (IAcquisitionWork shoppingItem : currentList) {
            if (shoppingItem.getDaysToWait() <= 0) {
                shoppingItem.resetDaysToWait();
            }
        }

        return new ForceShoppingList(currentList);
    }

    /**
     * Gets a value indicating if {@code person} can acquire parts.
     *
     * @param person The {@link Person} to check if they have remaining time to perform acquisitions.
     *
     * @return True if {@code person} could acquire another part, otherwise false.
     */
    public boolean canAcquireParts(@Nullable Person person) {
        if (person == null) {
            // CAW: in this case we're using automatic success
            // and the logistics person will be null.
            return true;
        }
        int maxAcquisitions = getCampaignOptions().get(CampaignOption.MAX_ACQUISITIONS);
        return maxAcquisitions <= 0 || person.getAcquisitions() < maxAcquisitions;
    }

    /***
     * Checks whether the campaign can pay for a given <code>IAcquisitionWork</code>
     * item. This will check
     * both whether the campaign is required to pay for a given type of acquisition
     * by the options and
     * if so whether it has enough money to afford it.
     *
     * @param acquisition - An <code>IAcquisitionWork</code> object
     * @return true if the campaign can pay for the acquisition; false if it cannot.
     */
    public boolean canPayFor(IAcquisitionWork acquisition) {
        // SHOULD we check to see if this acquisition needs to be paid for
        if ((acquisition instanceof UnitOrder && getCampaignOptions().get(CampaignOption.PAY_FOR_UNITS)) ||
                  (acquisition instanceof Part && getCampaignOptions().get(CampaignOption.PAY_FOR_PARTS))) {
            // CAN the acquisition actually be paid for
            return getPlayerForce().getFunds().isGreaterOrEqualThan(acquisition.getBuyCost());
        }
        return true;
    }

    /**
     * Make an acquisition roll for a given planet to see if you can identify a contact. Used for planetary based
     * acquisition.
     *
     * @param acquisition The <code> IAcquisitionWork</code> being acquired.
     * @param person      The <code>Person</code> object attempting to do the acquiring
     * @param system      The <code>PlanetarySystem</code> object where the acquisition is being attempted. This may be
     *                    null if the user is not using planetary acquisition.
     *
     * @return The result of the rolls.
     */
    public PartAcquisitionResult findContactForAcquisition(IAcquisitionWork acquisition, Person person,
          PlanetarySystem system) {
        SkillCheck skillCheck = checkAcquisition(acquisition, person);
        boolean inherentFailure = skillCheck.getTargetNumber().isImpossible();
        List<TargetRollModifier> acquisitionMods = system.getPrimaryPlanet().getAcquisitionMods(
              getLocalDate(), getCampaignOptions(), getPlayerForce().getFaction(), acquisition.getTechBase() == TechBase.CLAN);
        skillCheck.withExternalModifiers(acquisitionMods);

        // if it's already impossible, don't bother with the rest
        if (skillCheck.getTargetNumber().isImpossible()) {
            if (getCampaignOptions().get(CampaignOption.PLANET_ACQUISITION_VERBOSE)) {
                addReport(ACQUISITIONS, getFormattedTextAt(ACTION_CHECK_BUNDLE, "acquisition.impossible",
                      ReportingUtilities.getNegativeColor(), person.getFullName(), acquisition.getAcquisitionName(),
                      system.getPrintableName(getLocalDate()), skillCheck.getTargetNumber().getDesc()));
            }
            return inherentFailure ? PartAcquisitionResult.PartInherentFailure :
                         PartAcquisitionResult.PlanetSpecificFailure;
        }

        ActionCheckResult result = skillCheck.resolve(false, null);
        if (getCampaignOptions().get(CampaignOption.PLANET_ACQUISITION_VERBOSE)) {
            SocioIndustrialData socioIndustrial = system.getPrimaryPlanet().getSocioIndustrial(getLocalDate());
            CampaignOptions options = getCampaignOptions();
            int techBonus = options.getPlanetTechAcquisitionBonus(socioIndustrial.tech);
            int industryBonus = options.getPlanetIndustryAcquisitionBonus(socioIndustrial.industry);
            int outputsBonus = options.getPlanetOutputAcquisitionBonus(socioIndustrial.output);

            String reportType = result.isSuccess() ? "acquisition.success" : "acquisition.failure";
            String highlightColor = result.isSuccess() ? ReportingUtilities.getPositiveColor() :
                                          ReportingUtilities.getNegativeColor();

            addReport(ACQUISITIONS, getFormattedTextAt(ACTION_CHECK_BUNDLE, reportType,
                  highlightColor, person.getFullName(), acquisition.getAcquisitionName(),
                  system.getPrintableName(getLocalDate()), skillCheck.getTargetNumber().getValue(),
                  techBonus, industryBonus, outputsBonus));
        }
        return result.isSuccess() ? PartAcquisitionResult.Success : PartAcquisitionResult.PlanetSpecificFailure;
    }

    /***
     * Attempt to acquire a given <code>IAcquisitionWork</code> object.
     * This is the default method used by for non-planetary based acquisition.
     *
     * @param acquisition - The <code> IAcquisitionWork</code> being acquired.
     * @param person      - The <code>Person</code> object attempting to do the
     *                    acquiring. may be null if no one on the force has the
     *                    skill or the user is using automatic acquisition.
     * @return a boolean indicating whether the attempt to acquire equipment was
     *         successful.
     */
    public boolean acquireEquipment(IAcquisitionWork acquisition, @Nullable Person person) {
        return acquireEquipment(acquisition, person, null, -1);
    }

    /***
     * Attempt to acquire a given <code>IAcquisitionWork</code> object.
     *
     * @param acquisition - The <code> IAcquisitionWork</code> being acquired.
     * @param person      - The <code>Person</code> object attempting to do the
     *                    acquiring. may be null if no one on the force has the
     *                    skill or the user is using automatic acquisition.
     * @param system      - The <code>PlanetarySystem</code> object where the
     *                    acquisition is being attempted. This may be null if the
     *                    user is not using planetary acquisition.
     * @param transitDays - The number of days that the part should take to be
     *                    delivered. If this value is entered as -1, then this
     *                    method will determine transit time based on the users
     *                    campaign options.
     * @return a boolean indicating whether the attempt to acquire equipment was
     *         successful.
     */
    private boolean acquireEquipment(IAcquisitionWork acquisition, @Nullable Person person, PlanetarySystem system,
          int transitDays) {
        boolean found = false;
        String report = "";

        if (null != person) {
            report += person.getHyperlinkedFullTitle() + ' ';
        }

        List<TargetRollModifier> extraModifiers = new ArrayList<>();
        // check on funds
        if (!canPayFor(acquisition)) {
            extraModifiers.add(new TargetRollModifier(TargetRoll.IMPOSSIBLE, "Cannot afford this purchase"));
        }

        if (system != null) {
            extraModifiers.addAll(system.getPrimaryPlanet().getAcquisitionMods(
                  getLocalDate(), getCampaignOptions(), getPlayerForce().getFaction(), acquisition.getTechBase() == TechBase.CLAN));
        }
        report += "attempts to find " + acquisition.getAcquisitionName();

        SkillCheck skillCheck = checkAcquisition(acquisition, person).withExternalModifiers(extraModifiers);

        // if impossible, then return
        if (skillCheck.getTargetNumber().isImpossible()) {
            report += ":<font color='" +
                            ReportingUtilities.getNegativeColor() +
                            "'><b> " +
                            skillCheck.getTargetNumber().getDesc() +
                            "</b></font>";
            if (!getCampaignOptions().get(CampaignOption.USE_PLANETARY_ACQUISITION) ||
                      getCampaignOptions().get(CampaignOption.PLANET_ACQUISITION_VERBOSE)) {
                addReport(ACQUISITIONS, report);
            }
            return false;
        }

        report += "  needs " + skillCheck.getTargetNumber().getValueAsString();

        int targetValue = skillCheck.getTargetNumber().getValue();
        String useEdgeOption;
        if (targetValue >= 11) {
            useEdgeOption = PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_ELEVEN;
        } else if (targetValue >= 8) {
            useEdgeOption = PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_EIGHT;
        } else {
            useEdgeOption = PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_OTHER;
        }
        boolean useEdge = getCampaignOptions().get(CampaignOption.USE_EDGE) &&
                                person != null && person.getOptions().booleanOption(useEdgeOption);

        ActionCheckResult skillCheckResult = skillCheck.resolve(useEdge, null);
        if (skillCheckResult.hasUsedEdge()) {
            report += " and <b>fails!</b> but uses Edge to reroll...getting a " +
                            skillCheckResult.getRollResult() +
                            ": ";
        } else {
            report += " and rolls " + skillCheckResult.getRollResult() + ':';
        }
        int xpGained = 0;
        if (skillCheckResult.isSuccess()) {
            double valueChange = 1.0;
            String appraisalReport = "";
            if (campaignOptions.get(CampaignOption.USE_FUNCTIONAL_APPRAISAL) && person != null) {
                boolean isUseEdge = campaignOptions.get(CampaignOption.USE_EDGE) &&
                                          person.getOptions().booleanOption(EDGE_ADMIN_APPRAISAL_FAIL);
                ActionCheckResult appraisalResult = Appraisal.performAppraisalCheck(person, currentDay, isUseEdge);
                valueChange = Appraisal.getAppraisalCostMultiplier(appraisalResult.getMarginOfSuccess());
                appraisalReport = Appraisal.getAppraisalReport(valueChange, appraisalResult.getReportMargin());
            }

            if (transitDays < 0) {
                transitDays = calculatePartTransitTime(acquisition.getAvailability());
            }
            report = report + acquisition.find(transitDays, valueChange) + ' ' + appraisalReport;
            found = true;
            // Infantry weapons and ammo ship as a single bulk order: this one successful roll supplies the rest of the
            // platoon's quantity too (while it can be paid for), counting as a single acquisition for the daily limit.
            if (acquisition.isBulkAcquisition()) {
                int bulkDelivered = 1;
                while ((acquisition.getQuantity() > 1) && canPayFor(acquisition)) {
                    acquisition.find(transitDays, valueChange);
                    acquisition.decrementQuantity();
                    bulkDelivered++;
                }
                if (bulkDelivered > 1) {
                    report += " " + getFormattedTextAt(RESOURCE_BUNDLE, "acquireEquipment.bulkOrder.format",
                          bulkDelivered);
                }
            }
            if (person != null) {
                if (!skillCheck.getTargetNumber().isAutomaticSuccess()) {
                    person.setNTasks(person.getNTasks() + 1);
                    if (skillCheckResult.getRollResult() == 12) {
                        xpGained += getCampaignOptions().get(CampaignOption.SUCCESS_XP);
                    }
                }
                if (person.getNTasks() >= getCampaignOptions().get(CampaignOption.N_TASKS_XP)) {
                    xpGained += getCampaignOptions().get(CampaignOption.TASKS_XP);
                    person.setNTasks(0);
                }
            }
        } else {
            report = report + acquisition.failToFind();
            if ((person != null) && (skillCheckResult.getRollResult() == 2) &&
                      !skillCheck.getTargetNumber().isAutomaticFail()) {
                xpGained += getCampaignOptions().get(CampaignOption.MISTAKE_XP);
            }
        }

        if (null != person) {
            // The person should have their acquisitions incremented
            person.incrementAcquisition();

            if (xpGained > 0) {
                person.awardXP(this, xpGained);
                report += " (" + xpGained + "XP gained) ";
            }
        }

        if (found) {
            acquisition.decrementQuantity();
            MekHQ.triggerEvent(new AcquisitionEvent(acquisition));
        }
        if (!getCampaignOptions().get(CampaignOption.USE_PLANETARY_ACQUISITION) || getCampaignOptions().get(CampaignOption.PLANET_ACQUISITION_VERBOSE)) {
            addReport(ACQUISITIONS, report);
        }
        return found;
    }

    /**
     * Performs work to either mothball or activate a unit.
     *
     * @param unit The unit to either work towards mothballing or activation.
     */
    public void workOnMothballingOrActivation(Unit unit) {
        if (unit.isMothballed()) {
            activate(unit);
        } else {
            mothball(unit);
        }
    }

    /**
     * Performs work to mothball a unit, preparing it for long-term storage.
     *
     * <p>Mothballing process varies based on unit type:</p>
     * <ul>
     *   <li>Non-Infantry Units:
     *     <ul>
     *       <li>Requires an assigned tech</li>
     *       <li>Consumes tech work minutes</li>
     *       <li>Requires AsTech support time (6 minutes per tech minute)</li>
     *     </ul>
     *   </li>
     *   <li>Infantry Units:
     *     <ul>
     *       <li>Uses standard work day time</li>
     *       <li>No tech required</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * The process tracks progress and can span multiple work periods until complete.
     *
     * @param unit The unit to mothball. Must be active (not already mothballed)
     */
    public void mothball(Unit unit) {
        if (unit.isMothballed()) {
            LOGGER.warn("Unit is already mothballed, cannot mothball.");
            return;
        }

        String report;
        if (!unit.isConventionalInfantry()) {
            Person tech = unit.getTech();
            if (null == tech) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("noTech.mothballing"),
                      unit.getHyperlinkedName()));
                unit.cancelMothballOrActivation();
                return;
            }

            // don't allow overtime minutes for mothballing because it's cheating since you don't roll
            int minutes = Math.min(tech.getMinutesLeft(), unit.getMothballTime());

            // check AsTech time
            if (!unit.isSelfCrewed() && getPlayerForce().getHumanResources().getAsTechPoolMinutes() < minutes * 6) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("notEnoughAstechTime.mothballing"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                getPlayerForce().getHumanResources()
                      .setAsTechPoolMinutes(getPlayerForce().getHumanResources().getAsTechPoolMinutes() - 6 * minutes);
            }

            report = String.format(resources.getString("timeSpent.mothballing.tech"),
                  tech.getHyperlinkedFullTitle(),
                  minutes,
                  unit.getHyperlinkedName());
        } else {
            unit.setMothballTime(unit.getMothballTime() - TECH_WORK_DAY);

            report = String.format(resources.getString("timeSpent.mothballing.noTech"),
                  TECH_WORK_DAY,
                  unit.getHyperlinkedName());
        }

        if (!unit.isMothballing()) {
            unit.completeMothball();
            report += String.format(resources.getString("complete.mothballing"));
        } else {
            report += String.format(resources.getString("remaining.text"), unit.getMothballTime());
        }

        addReport(TECHNICAL, report);
    }

    /**
     * Performs work to activate a unit from its mothballed state. This process requires either:
     *
     * <ul>
     *   <li>A tech and sufficient AsTech support time for non-self-crewed units</li>
     *   <li>Only time for self-crewed units</li>
     * </ul>
     *
     * <p>The activation process:</p>
     * <ol>
     *   <li>Verifies the unit is mothballed</li>
     *   <li>For non-self-crewed units:
     *     <ul>
     *       <li>Checks for assigned tech</li>
     *       <li>Verifies sufficient tech and AsTech time</li>
     *       <li>Consumes tech and AsTech time</li>
     *     </ul>
     *   </li>
     *   <li>For self-crewed units:
     *     <ul>
     *       <li>Uses standard work day time</li>
     *     </ul>
     *   </li>
     *   <li>Updates mothball status</li>
     *   <li>Reports progress or completion</li>
     * </ol>
     *
     * @param unit The unit to activate. Must be mothballed for activation to proceed.
     */
    public void activate(Unit unit) {
        if (!unit.isMothballed()) {
            LOGGER.warn("Unit is already activated, cannot activate.");
            return;
        }

        String report;
        if (!unit.isConventionalInfantry()) {
            Person tech = unit.getTech();
            if (null == tech) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("noTech.activation"),
                      unit.getHyperlinkedName()));
                unit.cancelMothballOrActivation();
                return;
            }

            // don't allow overtime minutes for activation because it's cheating since you don't roll
            int minutes = Math.min(tech.getMinutesLeft(), unit.getMothballTime());

            // check AsTech time
            if (!unit.isSelfCrewed() && getPlayerForce().getHumanResources().getAsTechPoolMinutes() < minutes * 6) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("notEnoughAstechTime.activation"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                getPlayerForce().getHumanResources()
                      .setAsTechPoolMinutes(getPlayerForce().getHumanResources().getAsTechPoolMinutes() - 6 * minutes);
            }

            report = String.format(resources.getString("timeSpent.activation.tech"),
                  tech.getHyperlinkedFullTitle(),
                  minutes,
                  unit.getHyperlinkedName());
        } else {
            unit.setMothballTime(unit.getMothballTime() - TECH_WORK_DAY);

            report = String.format(resources.getString("timeSpent.activation.noTech"),
                  TECH_WORK_DAY,
                  unit.getHyperlinkedName());
        }

        if (!unit.isMothballing()) {
            unit.completeActivation();
            report += String.format(resources.getString("complete.activation"));
        } else {
            report += String.format(resources.getString("remaining.text"), unit.getMothballTime());
        }

        addReport(TECHNICAL, report);
    }

    public void refit(Refit theRefit) {
        Person tech = (theRefit.getUnit().getEngineer() == null) ?
                            theRefit.getTech() :
                            theRefit.getUnit().getEngineer();
        if (tech == null) {
            addReport(TECHNICAL, "No tech is assigned to refit " +
                                       theRefit.getOriginalEntity().getShortName() +
                                       ". Refit cancelled.");
            theRefit.cancel();
            return;
        }
        TargetRoll target = getTargetFor(theRefit, tech);
        // check that all parts have arrived
        if (!theRefit.acquireParts()) {
            return;
        }
        String report = tech.getHyperlinkedFullTitle() + " works on " + theRefit.getPartName();
        int minutes = theRefit.getTimeLeft();
        // FIXME: Overtime?
        if (minutes > tech.getMinutesLeft()) {
            theRefit.addTimeSpent(tech.getMinutesLeft());
            tech.setMinutesLeft(0);
            report = report + ", " + theRefit.getTimeLeft() + " minutes left. Completion ";
            int daysLeft = (int) Math.ceil((double) theRefit.getTimeLeft() /
                                                 (double) tech.getDailyAvailableTechTime(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)));
            if (daysLeft == 1) {
                report += " tomorrow.</b>";
            } else {
                report += " in " + daysLeft + " days.</b>";
            }
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            theRefit.addTimeSpent(minutes);
            if (theRefit.hasFailedCheck()) {
                report = report + ", " + theRefit.succeed();
            } else {
                int roll;
                String wrongType = "";
                if (tech.isRightTechTypeFor(theRefit)) {
                    roll = d6(2);
                } else {
                    roll = Utilities.roll3d6();
                    wrongType = " <b>Warning: wrong tech type for this refit.</b>";
                }
                report = report + ",  needs " + target.getValueAsString() + " and rolls " + roll + ": ";
                if (getCampaignOptions().get(CampaignOption.USE_EDGE) &&
                          (roll < target.getValue()) &&
                          tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT) &&
                          (tech.getCurrentEdge() > 0)) {
                    tech.spendEdge();
                    roll = tech.isRightTechTypeFor(theRefit) ? d6(2) : Utilities.roll3d6();
                    // This is needed to update the edge values of individual crewmen
                    if (tech.isEngineer()) {
                        tech.setEdgeUsedThisRound(tech.getEdgeUsedThisRound() - 1);
                    }
                    report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
                }

                if (roll >= target.getValue()) {
                    report += theRefit.succeed();
                } else {
                    report += theRefit.fail(SkillType.EXP_GREEN);
                    // try to refit again in case the tech has any time left
                    if (!theRefit.isBeingRefurbished()) {
                        refit(theRefit);
                        report += " Completion ";
                        int daysLeft = (int) Math.ceil((double) theRefit.getTimeLeft() /
                                                             (double) tech.getDailyAvailableTechTime(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)));
                        if (daysLeft == 1) {
                            report += " tomorrow.</b>";
                        } else {
                            report += " in " + daysLeft + " days.</b>";
                        }
                    }
                }
                report += wrongType;
            }
        }
        MekHQ.triggerEvent(new PartWorkEvent(tech, theRefit));
        addReport(TECHNICAL, report);
    }

    /**
     * Repairs a specified part from the warehouse by creating a clone of it, decrementing the quantity in stock,
     * repairing the cloned part, and optionally adding the repaired part back to the warehouse inventory.
     *
     * <p>If the original part's quantity drops to zero or below, no event notification is triggered.
     * Otherwise, an event is triggered to update the system about changes in the spare part's stock.</p>
     *
     * @param part The {@link Part} object to be repaired. Its quantity is decremented by one during this operation.
     * @param tech The {@link Person} who is performing the repair.
     *
     * @return A new repaired {@link Part} cloned from the original.
     */
    public Part fixWarehousePart(Part part, Person tech) {
        // get a new cloned part to work with and decrement original
        Part repairable = part.clone();
        // Capture the original's effective warehouse before decrementing, since
        // decrementing to zero would remove the original and clear its locationNode.
        LocalWarehouse targetWarehouse = part.getWarehouse();
        part.changeQuantity(-1);

        fixPart(repairable, tech);
        if (!(repairable instanceof OmniPod)) {
            if (targetWarehouse == getPlayerForce().getWarehouse()) {
                // Main-force spare: use the Quartermaster for full processing.
                getQuartermaster().addPart(repairable, 0, false);
            } else {
                // Base spare: add directly to the base warehouse and wire the locationNode.
                repairable.setDaysToArrival(0);
                repairable.postProcessCampaignAddition();
                targetWarehouse.addPart(repairable, true);
                LocationNode.LocationManager.setLocation(repairable, targetWarehouse);
            }
        }

        // If there is at least one remaining unit of the part
        // then we need to notify interested parties that we have
        // changed the quantity of the spare part.
        if (part.getQuantity() > 0) {
            MekHQ.triggerEvent(new PartChangedEvent(part));
        }

        return repairable;
    }

    /**
     * Attempt to fix a part, which may have all kinds of effect depending on part type.
     *
     * @param partWork - the {@link IPartWork} to be fixed
     * @param tech     - the {@link Person} who will attempt to fix the part
     *
     * @return a <code>String</code> of the report that summarizes the outcome of the attempt to fix the part
     */
    public String fixPart(IPartWork partWork, Person tech) {
        // Enforce location constraint: tech must be at the same location as the repair target.
        ILocation repairTarget = (partWork instanceof Part p && p.getUnit() != null)
                                       ? p.getUnit() : (ILocation) partWork;
        if (!LocationUtils.areSameEffectiveLocation(tech, repairTarget)) {
            String report = getFormattedTextAt(RESOURCE_BUNDLE, "fixPart.locationMismatch.report",
                  tech.getFullName(), partWork.getPartName());
            addReport(TECHNICAL, report);
            return report;
        }
        TargetRoll target = getTargetFor(partWork, tech);
        String report = "";
        String action = getAction(partWork);
        if ((partWork instanceof Armor) && !partWork.isSalvaging()) {
            if (!((Armor) partWork).isInSupply()) {
                report += "<b>Not enough armor remaining.  Task suspended.</b>";
                addReport(TECHNICAL, report);
                return report;
            }
        }
        if ((partWork instanceof ProtoMekArmor) && !partWork.isSalvaging()) {
            if (!((ProtoMekArmor) partWork).isInSupply()) {
                report += "<b>Not enough Protomek armor remaining.  Task suspended.</b>";
                addReport(TECHNICAL, report);
                return report;
            }
        }
        if ((partWork instanceof BAArmor) && !partWork.isSalvaging()) {
            if (!((BAArmor) partWork).isInSupply()) {
                report += "<b>Not enough BA armor remaining.  Task suspended.</b>";
                addReport(TECHNICAL, report);
                return report;
            }
        }
        if (partWork instanceof SpacecraftCoolingSystem) {
            // Change the string since we're not working on the part itself
            report += tech.getHyperlinkedFullTitle() + " attempts to" + action + "a heat sink";
        } else {
            report += tech.getHyperlinkedFullTitle() + " attempts to" + action + partWork.getPartName();
        }
        if (null != partWork.getUnit()) {
            report += " on " + partWork.getUnit().getName();
        }

        int minutes = partWork.getTimeLeft();
        int minutesUsed = minutes;
        boolean usedOvertime = false;
        if (minutes > tech.getMinutesLeft()) {
            minutes -= tech.getMinutesLeft();
            // check for overtime first
            if (isOvertimeAllowed() && minutes <= tech.getOvertimeLeft()) {
                // we are working overtime
                usedOvertime = true;
                partWork.setWorkedOvertime(true);
                tech.setMinutesLeft(0);
                tech.setOvertimeLeft(tech.getOvertimeLeft() - minutes);
            } else {
                // we need to finish the task tomorrow
                minutesUsed = tech.getMinutesLeft();
                int overtimeUsed = 0;
                if (isOvertimeAllowed()) {
                    // Can't use more overtime than there are minutes remaining on the part
                    overtimeUsed = Math.min(minutes, tech.getOvertimeLeft());
                    minutesUsed += overtimeUsed;
                    partWork.setWorkedOvertime(true);
                    usedOvertime = true;
                }
                partWork.addTimeSpent(minutesUsed);
                tech.setMinutesLeft(0);
                tech.setOvertimeLeft(tech.getOvertimeLeft() - overtimeUsed);
                int helpMod = getShorthandedMod(getPlayerForce().getHumanResources().getAvailableAsTechs(minutesUsed,
                      usedOvertime,
                      isOvertimeAllowed(),
                      getCampaignOptions()), false);
                if ((null != partWork.getUnit()) &&
                          ((partWork.getUnit().getEntity() instanceof Dropship) ||
                                 (partWork.getUnit().getEntity() instanceof Jumpship))) {
                    helpMod = 0;
                }

                if (partWork.getShorthandedMod() < helpMod) {
                    partWork.setShorthandedMod(helpMod);
                }
                partWork.setTech(tech);
                partWork.reservePart();
                report += " - <b>";
                report += partWork.getTimeLeft();
                report += " minutes left. Work";
                if ((minutesUsed > 0) &&
                          (tech.getDailyAvailableTechTime(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)) > 0)) {
                    report += " will be finished ";
                    int daysLeft = (int) Math.ceil((double) partWork.getTimeLeft() /
                                                         (double) tech.getDailyAvailableTechTime(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)));
                    if (daysLeft == 1) {
                        report += " tomorrow.</b>";
                    } else {
                        report += " in " + daysLeft + " days.</b>";
                    }
                } else {
                    report += " cannot be finished because there was no time left after maintenance tasks.</b>";
                    partWork.cancelAssignment(true);
                }
                MekHQ.triggerEvent(new PartWorkEvent(tech, partWork));
                addReport(TECHNICAL, report);
                return report;
            }
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        }
        int asTechMinutesUsed = minutesUsed * getPlayerForce().getHumanResources().getAvailableAsTechs(minutesUsed,
              usedOvertime,
              isOvertimeAllowed(),
              getCampaignOptions());
        if (getPlayerForce().getHumanResources().getAsTechPoolMinutes() < asTechMinutesUsed) {
            asTechMinutesUsed -= getPlayerForce().getHumanResources().getAsTechPoolMinutes();
            getPlayerForce().getHumanResources().setAsTechPoolMinutes(0);
            getPlayerForce().getHumanResources()
                  .setAsTechPoolOvertime(getPlayerForce().getHumanResources().getAsTechPoolOvertime() -
                                               asTechMinutesUsed);
        } else {
            getPlayerForce().getHumanResources()
                  .setAsTechPoolMinutes(getPlayerForce().getHumanResources().getAsTechPoolMinutes() -
                                              asTechMinutesUsed);
        }
        // check for the type
        int roll;
        String wrongType = "";
        if (tech.isRightTechTypeFor(partWork)) {
            roll = d6(2);
        } else {
            roll = Utilities.roll3d6();
            // On an automatic success the tech type is irrelevant (e.g. a self-crewed infantry unit reloading its
            // disposables or field guns - there is no valid tech type for it), so do not show the misleading warning.
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                wrongType = " <b>Warning: wrong tech type for this repair.</b>";
            }
        }
        report = report + ",  needs " + target.getValueAsString() + " and rolls " + roll + ':';
        int xpGained = 0;
        // if we fail and would break apart, here's a chance to use Edge for a
        // re-roll...
        if (getCampaignOptions().get(CampaignOption.USE_EDGE) &&
                  tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART) &&
                  (tech.getCurrentEdge() > 0) &&
                  (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
            if ((getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN) &&
                       (getCampaignOptions().get(CampaignOption.DESTROY_MARGIN) <= (target.getValue() - roll))) ||
                      (!getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN)
                             // if a legendary, primary tech and destroy by margin is NOT on
                             &&
                             ((tech.getExperienceLevel(getCampaignOptions(),
                                   getPlayerForce().isClanForce(),
                                   getLocalDate(),
                                   false,
                                   true) == SkillType.EXP_LEGENDARY) ||
                                    tech.getPrimaryRole().isVesselCrew())) // For vessel crews
                            && (roll < target.getValue())) {
                tech.spendEdge();
                roll = tech.isRightTechTypeFor(partWork) ? d6(2) : Utilities.roll3d6();
                // This is needed to update the edge values of individual crewmen
                if (tech.isEngineer()) {
                    tech.setEdgeUsedThisRound(tech.getEdgeUsedThisRound() + 1);
                }
                report += " <b>failed!</b> and would destroy the part, but uses Edge to reroll...getting a " +
                                roll +
                                ':';
            }
        }

        final boolean taskSucceeded = roll >= target.getValue();
        if (taskSucceeded) {
            // capture the repair target before succeeding: replacing a MissingPart removes the placeholder from the
            // unit, which clears its unit reference and its part name along with it
            final Unit repairedUnit = partWork.getUnit();
            final String repairedPartName = partWork.getPartName();
            final boolean isRepair = !partWork.isSalvaging() && !(partWork instanceof AmmoBin);

            report = report + partWork.succeed();
            // log successful repairs (fixes and missing-part replacements) against the unit; salvage and ammo
            // reloads are not repairs
            if ((repairedUnit != null) && isRepair) {
                UnitLogger.repaired(repairedUnit, getLocalDate(), repairedPartName, tech.getFullName());
            }
            if (getCampaignOptions().get(CampaignOption.PAY_FOR_REPAIRS) && action.equals(" fix ") && !(partWork instanceof Armor)) {
                Money cost = partWork.getUndamagedValue().multipliedBy(0.2);
                report += "<br>Repairs cost " + cost.toAmountAndSymbolString() + " worth of parts.";
                getPlayerForce().getFinances().debit(TransactionType.REPAIRS,
                      getLocalDate(),
                      cost,
                      "Repair of " + partWork.getPartName());
            }
            if ((roll == 12) && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
                xpGained += getCampaignOptions().get(CampaignOption.SUCCESS_XP);
            }
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                tech.setNTasks(tech.getNTasks() + 1);
            }
            if (tech.getNTasks() >= getCampaignOptions().get(CampaignOption.N_TASKS_XP)) {
                xpGained += getCampaignOptions().get(CampaignOption.TASKS_XP);
                tech.setNTasks(0);
            }
        } else {
            int modePenalty = partWork.getMode().expReduction;
            Skill relevantSkill = tech.getSkillForWorkingOn(partWork);
            int actualSkillLevel = EXP_NONE;

            if (relevantSkill != null) {
                SkillModifierData skillModifierData = tech.getSkillModifierData();
                actualSkillLevel = relevantSkill.getExperienceLevel(skillModifierData);
            }
            int effectiveSkillLevel = actualSkillLevel - modePenalty;
            if (getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN)) {
                if (getCampaignOptions().get(CampaignOption.DESTROY_MARGIN) > (target.getValue() - roll)) {
                    // not destroyed - set the effective level as low as
                    // possible
                    effectiveSkillLevel = SkillType.EXP_ULTRA_GREEN;
                } else {
                    // destroyed - set the effective level to legendary
                    effectiveSkillLevel = SkillType.EXP_LEGENDARY;
                }
            }
            report = report + partWork.fail(effectiveSkillLevel);

            if ((roll == 2) && (target.getValue() != TargetRoll.AUTOMATIC_FAIL)) {
                xpGained += getCampaignOptions().get(CampaignOption.MISTAKE_XP);
            }
        }

        if (xpGained > 0) {
            tech.awardXP(this, xpGained);
            report += " (" + xpGained + "XP gained) ";
        }
        report += wrongType;
        partWork.cancelAssignment(true);

        if (!taskSucceeded
                  && (partWork instanceof IFabricatable fabricatable)
                  && fabricatable.isFabricating()
                  && fabricatable.isFabricateUntilSuccess()
                  && fabricatable.canFabricate(tech).isBlank()
                  && (tech.getSkillForWorkingOn(partWork) != null)) {
            final Money nextCost = fabricatable.getFabricationCost(tech);
            if (nextCost.isZero() || !playerForce.getFinances().getBalance().isLessThan(nextCost)) {
                partWork.setTech(tech);
                partWork.reservePart();
                report += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, "fixPart.retryFabrication.report",
                      tech.getHyperlinkedFullTitle(), partWork.getPartName());
            } else {
                // Can't afford the next attempt: leave the tech unassigned so the job pauses (rather than wasting the
                // tech's time each cycle). The fabrication mode is kept, so re-assigning a tech once funded resumes it.
                report += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, "fixPart.retryFabrication.unaffordable.report",
                      partWork.getPartName());
            }
        }

        MekHQ.triggerEvent(new PartWorkEvent(tech, partWork));
        addReport(TECHNICAL, report);
        return report;
    }

    private static String getAction(IPartWork partWork) {
        String action = " fix ";

        // TODO: this should really be a method on its own class
        if (partWork instanceof AmmoBin) {
            action = " reload ";
        }
        if (partWork.isSalvaging()) {
            action = " salvage ";
        }
        if (partWork instanceof MissingPart) {
            action = " replace ";
        }
        if (partWork instanceof MekLocation) {
            if (((MekLocation) partWork).isBlownOff()) {
                action = " re-attach ";
            } else if (((MekLocation) partWork).isBreached()) {
                action = " seal ";
            }
        }
        return action;
    }

    /**
     * Parses news file and loads news items for the current year.
     */
    public void reloadNews() {
        news.loadNewsFor(getGameYear(), id.getLeastSignificantBits());
    }

    /**
     * Checks for a news item for the current date. If found, adds it to the daily report.
     */
    public void readNews() {
        // read the news
        for (NewsItem article : news.fetchNewsFor(getLocalDate())) {
            addReport(GENERAL, article.getHeadlineForReport());
        }

        for (NewsItem article : this.systemsInstance.getPlanetaryNews(getLocalDate())) {
            addReport(GENERAL, article.getHeadlineForReport());
        }
    }

    /**
     * Advances the campaign by one day, processing all daily events and updates.
     *
     * <p>This method delegates to {@link CampaignNewDayManager} to handle all new day processing,
     * including personnel updates, contract management, financial transactions, maintenance tasks, and other
     * time-dependent campaign events.</p>
     *
     * @return {@code true} if the new day processing completed successfully; {@code false} if it was canceled or failed
     *
     * @see CampaignNewDayManager#newDay()
     */
    public boolean newDay() {
        if (newDayManager == null) {
            newDayManager = new CampaignNewDayManager(this);
        }

        return newDayManager.newDay();
    }

    public CampaignNewDayManager getNewDayManager() {
        return newDayManager;
    }

    /**
     * Computes the total rental fees for the campaign, including all rented hospital beds, kitchens, and holding
     * cells.
     *
     * <p>Fetches all active contracts and sums the rental costs for each facility type before adding any ongoing
     * bay rental fees.</p>
     *
     * <p>If you want to fetch the rent due for bays use
     * {@link FacilityRentals#getTotalRentSumFromRentedBays(Campaign, Finances)}</p>
     *
     * @return the combined {@link Money} amount representing all current rental fees owed
     *
     * @author Illiani
     * @since 0.50.10
     */
    public Money getTotalRentFeesExcludingBays() {
        List<AbstractContract> activeContracts = getActiveContracts();
        int hospitalRentalCost = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS);
        Money hospitalRentalFee = FacilityRentals.calculateContractRentalCost(hospitalRentalCost, activeContracts,
              ContractRentalType.HOSPITAL_BEDS);

        int kitchenRentalCost = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_KITCHENS);
        Money kitchenRentalFee = FacilityRentals.calculateContractRentalCost(kitchenRentalCost, activeContracts,
              ContractRentalType.KITCHENS);

        int holdingCellRentalCost = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS);
        Money holdingCellRentalFee = FacilityRentals.calculateContractRentalCost(holdingCellRentalCost, activeContracts,
              ContractRentalType.HOLDING_CELLS);

        return hospitalRentalFee.plus(kitchenRentalFee).plus(holdingCellRentalFee);
    }

    /**
     * Checks if a new mercenary organization is starting up in the current game year, and, if so, triggers a welcome
     * dialog introducing the organization's representative.
     *
     * <p>This method examines a prioritized list of known mercenary-related factions for their respective founding
     * (start) years matching the current year. The list is evaluated in the following order: Mercenary Review Board
     * (MRB), Mercenary Review Bonding Commission (MRBC), Mercenary Bonding Authority (MBA), and Mercenary Guild (MG),
     * with MG as the default fallback. If a matching faction is found (and is recognized as a mercenary organization),
     * it generates an appropriate speaker (as either a merchant or military liaison, depending on the faction) and
     * opens a welcome dialog for the player.</p>
     *
     * <p>The dialog serves to introduce the player to the new mercenary organization, using an in-universe character
     * as the spokesperson.</p>
     *
     * @param bypassStartYear {@code true} if the method should be checking if the mercenary organization is currently
     *                        active, rather than just checking whether it was founded in the current game year.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void checkForNewMercenaryOrganizationStartUp(boolean bypassStartYear, boolean isStartUp) {
        Factions factions = Factions.getInstance();
        int currentYear = getGameYear();
        Faction[] possibleFactions = new Faction[] {
              factions.getFaction("MRB"),
              factions.getFaction("MRBC"),
              factions.getFaction("MBA"),
              factions.getFaction("MG")
        };

        Faction chosenFaction = null;
        for (Faction faction : possibleFactions) {
            if (faction != null) {
                boolean isValidInYear = bypassStartYear && faction.validIn(currentYear);
                boolean isFoundedInYear = !bypassStartYear && faction.getStartYear() == currentYear;

                if (isValidInYear || isFoundedInYear) {
                    chosenFaction = faction;
                    break;
                }
            }
        }

        if (chosenFaction == null) {
            chosenFaction = factions.getFaction("MG"); // fallback
        }

        if (chosenFaction != null
                  && (chosenFaction.getStartYear() == currentYear || isStartUp)
                  && chosenFaction.isMercenaryOrganization()) {
            PersonnelRole role = chosenFaction.isClan() ? PersonnelRole.MERCHANT : PersonnelRole.MILITARY_LIAISON;
            final String factionCode = chosenFaction.getShortName();
            Person speaker = getPlayerForce().getHumanResources().newPerson(this, role, factionCode, Gender.RANDOMIZE);

            AutomaticRankAssigner.assignRankSystemFromFaction(speaker, RO_MIN);

            new FactionJudgmentDialog(this, speaker, getPlayerForce().getHumanResources()
                                                           .getCommander(getCampaignOptions(),
                                                                 getPlayerForce().isClanForce(),
                                                                 getLocalDate()),
                  "HELLO", chosenFaction,
                  FactionStandingJudgmentType.WELCOME, ImmersiveDialogWidth.MEDIUM, null, null);
        } else if (chosenFaction == null) {
            LOGGER.warn("Unable to find a suitable faction for a new mercenary organization start up");
        }
    }

    /**
     * Retrieves the flagged commander from the personnel list. If no flagged commander is found returns {@code null}.
     *
     * <p><b>Usage:</b> consider using {@code getCommander()} instead.</p>
     *
     * @return the flagged commander if present, otherwise {@code null}
     */
    public @Nullable Person getFlaggedCommander() {
        return getPlayerForce().getHumanResources()
                     .getPersonnel()
                     .stream()
                     .filter(Person::isCommander)
                     .findFirst()
                     .orElse(null);
    }

    /**
     * Retrieves the flagged second-in-command from the personnel list. If no flagged second-in-command is found returns
     * {@code null}.
     *
     * <p><b>Usage:</b> consider using {@code getSecondInCommand()} instead.</p>
     *
     * @return the flagged second-in-command if present, otherwise {@code null}
     */
    public @Nullable Person getFlaggedSecondInCommand() {
        return getPlayerForce().getHumanResources()
                     .getPersonnel()
                     .stream().filter(Person::isSecondInCommand).findFirst().orElse(null);
    }

    public void removeUnit(UUID id) {
        Unit unit = getPlayerForce().getHangar().getUnit(id);
        if (unit == null) {
            return;
        }

        // remove all parts for this unit as well
        for (Part p : unit.getParts()) {
            getPlayerForce().getWarehouse().removePart(p);
        }

        // remove any personnel from this unit
        for (Person person : unit.getCrew()) {
            unit.remove(person, true);
        }

        Person tech = unit.getTech();
        if (null != tech) {
            unit.remove(tech, true);
        }

        // remove unit from any formations
        getPlayerForce().removeUnitFromFormation(unit, this);

        // If this is a transport, remove it from the list of potential transports
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            if (hasTransports(campaignTransportType)) {
                removeCampaignTransporter(campaignTransportType, unit);
            }

            // If we remove a transport unit from the campaign,
            // we need to remove any transported units from it
            // and clear the transport assignments for those
            // transported units
            if (unit.getTransportedUnitsSummary(campaignTransportType).hasTransportedUnits()) {
                List<Unit> transportedUnits = new ArrayList<>(unit.getTransportedUnitsSummary(campaignTransportType)
                                                                    .getTransportedUnits());
                for (Unit transportedUnit : transportedUnits) {
                    transportedUnit.unloadFromTransport(campaignTransportType);
                }
            }
        }

        // If this unit was assigned to a transport ship, remove it from the transport
        if (unit.hasTransportShipAssignment()) {
            unit.getTransportShipAssignment().getTransportShip().unloadFromTransportShip(unit);
        }

        // remove from automatic mothballing
        getPlayerForce().getAutomatedMothballUnits().remove(unit.getId());

        // finally, remove the unit
        getPlayerForce().getHangar().removeUnit(unit.getId());

        checkDuplicateNamesDuringDelete(unit.getEntity());
        addReport(ACQUISITIONS, unit.getName() + " has been removed from the unit roster.");
        MekHQ.triggerEvent(new UnitRemovedEvent(unit));
    }

    public void removeScenario(final Scenario scenario) {
        scenario.clearAllFormationsAndPersonnel(this);
        final AbstractContract mission = getContract(scenario.getMissionId());
        if (mission != null) {
            mission.getScenarios().remove(scenario);

            // run through the StratCon campaign state where applicable and remove the
            // "parent" scenario as well
            if (mission.getStratConCampaignState() != null && scenario instanceof AtBDynamicScenario) {
                mission.getStratConCampaignState().removeStratConScenario(scenario.getId());
            }
        }
        scenarios.remove(scenario.getId());

        // https://github.com/MegaMek/mekhq/pull/7761
        // there's a bug preventing clearAllFormationsAndPersonnel from removing all scenario links,
        // hence we have to do an extra clean up here:
        cleanUp();

        MekHQ.triggerEvent(new ScenarioRemovedEvent(scenario));
    }

    public void removeMission(final AbstractContract mission) {
        // Loop through scenarios here! We need to remove them as well.
        for (Scenario scenario : mission.getScenarios()) {
            scenario.clearAllFormationsAndPersonnel(this);
            scenarios.remove(scenario.getId());
        }
        mission.clearScenarios();

        contractHistory.remove(mission.getId());

        // https://github.com/MegaMek/mekhq/pull/7761
        // there's a bug preventing clearAllFormationsAndPersonnel from removing all scenario links,
        // hence we have to do an extra clean up here:
        cleanUp();

        MekHQ.triggerEvent(new MissionRemovedEvent(mission));
    }

    public void removeKill(Kill k) {
        if (kills.containsKey(k.getPilotId())) {
            kills.get(k.getPilotId()).remove(k);
        }
    }

    public void removeKillsFor(UUID personID) {
        kills.remove(personID);
    }

    public void restore() {
        // if we fail to restore equipment parts then remove them
        // and possibly re-initialize and diagnose unit
        List<Part> partsToRemove = new ArrayList<>();
        Set<Unit> unitsToCheck = new HashSet<>();

        for (Part part : getParts()) {
            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).restore();
                if (null == ((EquipmentPart) part).getType()) {
                    partsToRemove.add(part);
                }
            }

            if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).restore();
                if (null == ((MissingEquipmentPart) part).getType()) {
                    partsToRemove.add(part);
                }
            }
        }

        for (Part remove : partsToRemove) {
            if (remove.getUnit() != null) {
                unitsToCheck.add(remove.getUnit());
            }
            getPlayerForce().getWarehouse().removePart(remove);
        }

        for (Unit unit : getUnits()) {
            if (null != unit.getEntity()) {
                unit.getEntity().setOwner(player);
                unit.getEntity().setGame(game);
                unit.getEntity().restore();

                // Aerospace parts have changed after 0.45.4. Reinitialize parts for Small Craft
                // and up
                if (unit.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                          unit.getEntity().hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                    unitsToCheck.add(unit);
                }
            }

            unit.resetEngineer();
        }

        for (Unit u : unitsToCheck) {
            u.initializeParts(true);
            u.runDiagnostic(false);
        }

        getPlayerForce().getShoppingList().restore();

        if (getCampaignOptions().isUseStratCon()) {
            RandomFactionGenerator.getInstance().startup(this);

            int loops = 0;
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                    if (++loops > 20) {
                        // Wait for up to a second
                        break;
                    }
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    /**
     * Cleans incongruent data present in the campaign
     */
    public void cleanUp() {
        // Cleans non-existing spouses
        for (Person person : getPlayerForce().getPersonnel().values()) {
            if (person.getGenealogy().hasSpouse()) {
                final UUID id1 = person.getGenealogy().getSpouse().getId();
                if (getPlayerForce().getHumanResources().getPerson(id1) == null) {
                    person.getGenealogy().setSpouse(null);
                    person.setMaidenName(null);
                }
            }
        }

        // clean up non-existent unit references in formation unit lists
        for (Formation formation : getPlayerForce().getFormationIds().values()) {
            List<UUID> orphanFormationUnitIDs = new ArrayList<>();

            for (UUID unitID : formation.getUnits()) {
                if (getPlayerForce().getHangar().getUnit(unitID) == null) {
                    orphanFormationUnitIDs.add(unitID);
                }
            }

            for (UUID unitID : orphanFormationUnitIDs) {
                formation.removeUnit(this, unitID, false);
            }

            int scenarioId = formation.getScenarioId();
            if ((scenarioId != Scenario.S_DEFAULT_ID) && (getScenario(scenarioId) == null)) {
                formation.setScenarioId(Scenario.S_DEFAULT_ID, this);
                LOGGER.error(String.format("Fixing a broken scenario link for formation %s", formation.getName()));
            }
        }

        // clean up units that are assigned to non-existing scenarios
        for (Unit unit : this.getUnits()) {
            int scenarioId = unit.getScenarioId();
            if ((scenarioId != Scenario.S_DEFAULT_ID) && (getScenario(scenarioId) == null)) {
                unit.setScenarioId(Scenario.S_DEFAULT_ID);
                LOGGER.error(String.format("Fixing a broken scenario link for unit %s", unit.getName()));
            }
        }
    }

    public boolean isOvertimeAllowed() {
        return overtime;
    }

    public void setOvertime(boolean b) {
        this.overtime = b;
        MekHQ.triggerEvent(new OvertimeModeEvent(b));
    }

    public boolean isGM() {
        return gmMode;
    }

    public void setGMMode(boolean b) {
        this.gmMode = b;
        MekHQ.triggerEvent(new GMModeEvent(b));
    }

    /**
     * Determines whether the current campaign is a pirate campaign.
     *
     * <p>This method checks if the faction associated with the campaign is Pirates, returning {@code true} if it is,
     * and {@code false} otherwise.</p>
     *
     * @return {@code true} if the campaign is Pirates, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isPirateCampaign() {
        return getPlayerForce().getFaction().getShortName().equals(PIRATE_FACTION_CODE);
    }

    /**
     * Determines whether the current campaign is a mercenary campaign.
     *
     * <p>This method checks if the faction associated with the campaign is Mercenary, returning {@code true} if it is,
     * and {@code false} otherwise.</p>
     *
     * @return {@code true} if the campaign is Mercenary, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isMercenaryCampaign() {
        return getPlayerForce().getFaction().getShortName().equals(MERCENARY_FACTION_CODE);
    }

    private void addInMemoryLogHistory(LogEntry le) {
        Iterator<LogEntry> iterator = inMemoryLogHistory.iterator();
        while (iterator.hasNext() &&
                     ChronoUnit.DAYS.between(iterator.next().getDate(), le.getDate()) >
                           MHQConstants.MAX_HISTORICAL_LOG_DAYS) {
            // we've hit the max size for the in-memory based on the UI display limit prune
            // the oldest entry
            iterator.remove();
        }
        inMemoryLogHistory.add(le);
    }

    /**
     * Starts a new day for the daily log
     *
     * @param report - the report String
     */
    public void beginReport(String report) {
        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            // add the new items to our in-memory cache
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), ""));
        }

        for (DailyReportType type : DailyReportType.values()) {
            addReportInternal(type, report);
        }
    }

    /**
     * Formats and then adds a report to the daily log
     *
     * @param type    what log to place the report in
     * @param format  String with format markers.
     * @param objects Variable list of objects to format into {@code format}
     */
    public void addReport(final DailyReportType type, final String format, final Object... objects) {
        addReport(type, String.format(format, objects));
    }

    /**
     * Adds a report to the daily log
     *
     * @param type   what log to place the report in
     * @param report - the report String
     */
    public void addReport(DailyReportType type, String report) {
        if (StringUtility.isNullOrBlank(report)) {
            return;
        }

        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), report));
        }

        // We handle this here, instead of 'addReportInternal' as we don't want to post multiple new day 'dates' to
        // the General tab
        if (MekHQ.getMHQOptions().getUnifiedDailyReport()) {
            type = GENERAL;
        }

        addReportInternal(type, report);

        if (type != AGGREGATE && MekHQ.getMHQOptions().isUseAggregateDailyReport()) {
            addReportInternal(AGGREGATE, report);
        }
    }

    private void addReportInternal(final DailyReportType type, final String report) {
        dailyReportLog.add(type, report);
        MekHQ.triggerEvent(new ReportEvent(this, report));
    }

    public void addFunds(final TransactionType type, final Money quantity, @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }
        playerForce.addFunds(type, getLocalDate(), quantity, description);
        addReport(FINANCES, "Funds added : " + quantity.toAmountAndSymbolString() + " (" + description + ')');
    }

    public void removeFunds(final TransactionType type, final Money quantity, @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }
        playerForce.removeFunds(type, getLocalDate(), quantity, description);
        addReport(FINANCES, "Funds removed : " + quantity.toAmountAndSymbolString() + " (" + description + ')');
    }

    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public void setCampaignOptions(CampaignOptions options) {
        // Check if blob crew was disabled for each role
        boolean infantryWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_INFANTRY);
        boolean baWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_BATTLE_ARMOR);
        boolean vehicleGroundWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND);
        boolean vehicleVTOLWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL);
        boolean vehicleNavalWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL);
        boolean vesselPilotWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VESSEL_PILOT);
        boolean vesselGunnerWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VESSEL_GUNNER);
        boolean vesselCrewWasEnabled = campaignOptions.get(CampaignOption.USE_BLOB_VESSEL_CREW);

        campaignOptions = options;
        // Keep the player force's ForceOptions pass-through pointed at the current campaign options.
        playerForce.getForceOptions().setCampaignOptions(options);

        // If blob crew was disabled for a specific role, clear only that role's blob crew
        if (infantryWasEnabled && !options.get(CampaignOption.USE_BLOB_INFANTRY)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.SOLDIER);
        }
        if (baWasEnabled && !options.get(CampaignOption.USE_BLOB_BATTLE_ARMOR)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.BATTLE_ARMOUR);
        }
        if (vehicleGroundWasEnabled && !options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (vehicleVTOLWasEnabled && !options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (vehicleNavalWasEnabled && !options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (vesselPilotWasEnabled && !options.get(CampaignOption.USE_BLOB_VESSEL_PILOT)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VESSEL_PILOT);
        }
        if (vesselGunnerWasEnabled && !options.get(CampaignOption.USE_BLOB_VESSEL_GUNNER)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VESSEL_GUNNER);
        }
        if (vesselCrewWasEnabled && !options.get(CampaignOption.USE_BLOB_VESSEL_CREW)) {
            getPlayerForce().getHumanResources().clearBlobCrewForRole(this, PersonnelRole.VESSEL_CREW);
        }
    }

    public StoryArc getStoryArc() {
        return storyArc;
    }

    public void useStoryArc(StoryArc arc, boolean initiate) {
        arc.setCampaign(this);
        arc.initializeDataDirectories();
        this.storyArc = arc;
        if (initiate) {
            storyArc.begin();
        }
    }

    public void unloadStoryArc() {
        MekHQ.unregisterHandler(storyArc);
        storyArc = null;
    }

    public List<String> getCurrentObjectives() {
        if (null != getStoryArc()) {
            return getStoryArc().getCurrentObjectives();
        }
        return new ArrayList<>();
    }

    public RandomEventLibraries getRandomEventLibraries() {
        return randomEventLibraries;
    }

    public FactionStandingUltimatumsLibrary getFactionStandingUltimatumsLibrary() {
        return factionStandingUltimatumsLibrary;
    }

    public Map<Integer, List<WarriorsAlmanacEntry>> getPartsAlmanac() {
        return partsAlmanac;
    }

    public void setPartsAlmanac(Map<Integer, List<WarriorsAlmanacEntry>> partsAlmanac) {
        this.partsAlmanac = partsAlmanac;
    }

    public Map<Integer, List<WarriorsAlmanacEntry>> getUnitsAlmanac() {
        return unitsAlmanac;
    }

    public void setUnitsAlmanac(Map<Integer, List<WarriorsAlmanacEntry>> unitsAlmanac) {
        this.unitsAlmanac = unitsAlmanac;
    }

    public void writeToXML(final PrintWriter writer, boolean isBugReportPrep) {
        int indent = 0;

        // File header
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Start the XML root.
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "campaign", "version", MHQConstants.VERSION);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "pastVersions");
        for (final Version pastVersion : pastVersions) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "pastVersion", pastVersion.toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "pastVersions");

        // region Basic Campaign Info
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "info");

        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "id", id.toString());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "calendar", getLocalDate());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", getPlayerForce().getName());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "faction", getPlayerForce().getFaction().getShortName());
        if (getPlayerForce().getRetainerEmployerCode() != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "retainerEmployerCode",
                  getPlayerForce().getRetainerEmployerCode());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "retainerStartDate",
                  getPlayerForce().getRetainerStartDate());
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "crimeRating", getPlayerForce().getRawCrimeRating());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "crimePirateModifier",
              getPlayerForce().getCampOpsCrimePirateModifier());

        if (getPlayerForce().getCampOpsDateOfLastCrime() != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer,
                  indent,
                  "dateOfLastCrime",
                  getPlayerForce().getCampOpsDateOfLastCrime());
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "reputation");
        getPlayerForce().getCamOpsReputation().writeReputationToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "reputation");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "chaosCampaignReputation",
              getPlayerForce().getChaosCampaignReputation());
        if (getPlayerForce().getHumanResources().getNewPersonnelMarket() != null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "newPersonnelMarket");
            getPlayerForce().getHumanResources().getNewPersonnelMarket().writePersonnelMarketDataToXML(writer, indent);
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "newPersonnelMarket");
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionStandings");
        getPlayerForce().getFactionStandings().writeFactionStandingsToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionStandings");

        // this handles campaigns that predate 49.20
        if (campaignStartDate == null) {
            setCampaignStartDate(getLocalDate());
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "campaignStartDate", getCampaignStartDate());

        getPlayerForce().getRankSystem().writeToXML(writer, indent, false);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "overtime", overtime);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "gmMode", gmMode);

        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "fieldKitchenWithinCapacity",
              getPlayerForce().getFieldKitchenWithinCapacity());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "mashTheatreCapacity",
              playerForce.getCachedMashTheaterCapacity());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "repairBaysRented", playerForce.getRepairBaysRented());
        getPlayerForce().getCamouflage().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "colour", getPlayerForce().getColour().name());
        getPlayerForce().getUnitIcon().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastFormationId", playerForce.getLastFormationId());
        getPlayerForce().writeContractsToXML(writer, indent, this);
        getPlayerForce().getContractMarket().writeToXML(writer, indent, this);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastMissionId", lastMissionId);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastScenarioId", lastScenarioId);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "initiativeBonus", getPlayerForce().getInitiativeBonus());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "initiativeMaxBonus", getPlayerForce().getInitiativeMaxBonus());
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "nameGen");
        MHQXMLUtility.writeSimpleXMLTag(writer,
              indent,
              "faction",
              RandomNameGenerator.getInstance().getChosenFaction());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "percentFemale", RandomGenderGenerator.getPercentFemale());
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "nameGen");

        dailyReportLog.writeToXML(writer, indent);

        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "info");
        // endregion Basic Campaign Info

        // region Options
        if (getCampaignOptions() != null) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(getCampaignOptions(), writer, indent);
        }

        // We've had instances where game options aren't loaded correctly from player campaigns, potentially due to
        // age. This safeguards against that occurance, preventing players entering a state where they cannot
        // continue their campaigns.
        if (gameOptions == null) {
            gameOptions = new GameOptions();
            LOGGER.errorDialog(new NullPointerException(),
                  getTextAt(RESOURCE_BUNDLE, "gameOptions.save.failure.body"),
                  getTextAt(RESOURCE_BUNDLE, "gameOptions.save.failure.title"));
        }

        getGameOptions().writeToXML(writer, indent);
        // endregion Options

        PlanetarySystemCampaignXmlIO.writeToXML(writer, indent, getPlanetarySystemOverrides());

        // Lists of objects:
        getPlayerForce().getHangar().writeToXML(writer, indent, "units"); // Units

        getPlayerForce().getHumanResources().writeToXML(writer, indent, this);

        // the formations structure is hierarchical, but that should be handled
        // internally from with writeToXML function for Formation
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "formations");
        getPlayerForce().getFormations().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "formations");
        getPlayerForce().getFinances().writeToXML(writer, indent);
        getPlayerForce().getDetachmentLocationManager().writeToXML(writer, indent);
        locationManager.writeToXML(this, writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "isAvoidingEmptySystems",
              getPlayerForce().isAvoidingEmptySystems());
        MHQXMLUtility.writeSimpleXMLTag(writer,
              indent,
              "isOverridingCommandCircuitRequirements",
              getPlayerForce().isOverridingCommandCircuitRequirements());
        getPlayerForce().getShoppingList().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "kills");
        for (List<Kill> kills : kills.values()) {
            for (Kill k : kills) {
                k.writeToXML(writer, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "kills");
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "skillTypes");
        for (final String skillName : SkillType.skillList) {
            final SkillType type = getType(skillName);
            if (type != null) {
                type.writeToXML(writer, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "skillTypes");
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "specialAbilities");
        for (String key : SpecialAbility.getSpecialAbilities().keySet()) {
            SpecialAbility.getAbility(key).writeToXML(writer, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "specialAbilities");
        randomSkillPreferences.writeToXML(writer, indent);

        // parts is the biggest so it goes last
        getPlayerForce().getWarehouse().writeToXML(writer, indent, "parts"); // Parts

        // current story arc
        if (null != storyArc) {
            storyArc.writeToXml(writer, indent);
        }

        // Markets
        if (getPlayerForce().getHumanResources().getPersonnelMarket() != null) {
            getPlayerForce().getHumanResources().getPersonnelMarket().writeToXML(writer, indent, this);
        }

        // Windchild: implicit DEPENDS-ON to the <campaignOptions> node, do not move
        // this above it
        getUnitMarket().writeToXML(writer, indent);

        // Against the Bot
        if (getCampaignOptions().isUseStratCon()) {
            if (!playerForce.getCombatTeamsMap().isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "combatTeams");
                for (CombatTeam combatTeam : playerForce.getCombatTeamsMap().values()) {
                    if (getPlayerForce().getFormationIds().containsKey(combatTeam.getFormationId())) {
                        combatTeam.writeToXML(writer, indent);
                    }
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "combatTeams");
            }
            MHQXMLUtility.writeSimpleXMLTag(writer,
                  indent,
                  "autoResolveBehaviorSettings",
                  autoResolveBehaviorSettings.getDescription());
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "automatedMothballUnits");
        for (UUID unitId : getPlayerForce().getAutomatedMothballUnits()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "mothballedUnit", unitId);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "automatedMothballUnits");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "temporaryPrisonerCapacity",
              getPlayerForce().getTemporaryPrisonerCapacity());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "processProcurement", processProcurement);

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, ++indent, "partsInUse");
        writePartInUseToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "partsInUse");

        boolean shouldSaveAllUnits = isBugReportPrep || MekHQ.getMHQOptions().getWriteAllUnitsToXML();
        boolean shouldSaveCustomsOnly = !shouldSaveAllUnits && MekHQ.getMHQOptions().getWriteCustomsToXML();
        if (shouldSaveAllUnits || shouldSaveCustomsOnly) {
            writeUnitDefinitionsIntoSave(writer, shouldSaveAllUnits, shouldSaveCustomsOnly);
        }

        // Okay, we're done.
        // Close everything out and be done with it.
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "campaign");
    }

    /**
     * Writes custom unit definitions to the campaign XML output.
     *
     * <p>This method can operate in two modes:</p>
     * <ul>
     *     <li>If {@code shouldSaveAllUnits} is {@code true}, it scans all units currently present in the campaign
     *     and collects each unit's raw short name (via {@code entity.getShortNameRaw()}) as a candidate custom
     *     definition.</li>
     *     <li>Else, if {@code shouldSaveAllCustoms} is {@code true}, it writes all names already present in the
     *     campaign's {@code customs} collection.</li>
     * </ul>
     *
     * <p>For each collected name, the corresponding {@link MekSummary} is looked up via {@link MekSummaryCache}. If
     * a summary and source file can be resolved, the source entity is parsed and serialized:</p>
     * <ul>
     *     <li>{@link Mek} entities are exported as embedded MTF text inside a CDATA section.</li>
     *     <li>All other supported entities are exported as BLK content (non-empty lines only) inside a CDATA
     *     section.</li>
     * </ul>
     *
     * <p>Entries that cannot be resolved (missing/blank short name, missing {@link MekSummary}, missing source file,
     * parse failures, null parsed entity, or save failures) are skipped and logged.</p>
     *
     * <p><b>Note:</b> This method is for enshrining unit definition data into the save (the BLK for the unit) and
     * not general unit data (who is crewing the unit, etc).</p>
     *
     * @param printWriter          the output writer that receives {@code <custom>} XML elements; must not be
     *                             {@code null}
     * @param shouldSaveAllUnits   when {@code true}, derive the custom list by scanning all current units
     * @param shouldSaveAllCustoms when {@code true} (and {@code shouldSaveAllUnits} is {@code false}), write all names
     *                             from the campaign's stored customs list
     */
    private void writeUnitDefinitionsIntoSave(PrintWriter printWriter, boolean shouldSaveAllUnits,
          boolean shouldSaveAllCustoms) {
        Set<String> customUnits = new HashSet<>();
        if (shouldSaveAllUnits) {
            for (Unit unit : getPlayerForce().getHangar().getUnits()) {
                Entity entity = unit.getEntity();
                if (entity != null) {
                    String shortName = entity.getShortNameRaw();
                    if (!StringUtility.isNullOrBlank(shortName)) {
                        customUnits.add(shortName);
                    } else {
                        LOGGER.warn("shortName was null or blank for {}. Skipping", unit.getName());
                    }
                }
            }
        } else if (shouldSaveAllCustoms) {
            customUnits = new HashSet<>(customs);
        }

        for (String name : customUnits) {
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(name);
            if (mekSummary == null) {
                LOGGER.warn("mekSummary was null for {}", name);
                continue;
            }

            MekFileParser mekFileParser = null;
            try {
                File sourceFile = mekSummary.getSourceFile();
                if (sourceFile == null) {
                    LOGGER.warn("sourceFile was null for {}", name);
                    continue;
                }

                mekFileParser = new MekFileParser(sourceFile, mekSummary.getEntryName());
            } catch (EntityLoadingException ex) {
                LOGGER.error("Failed to fetch MekFileParser for {} // {}",
                      mekSummary.getSourceFile(), mekSummary.getEntryName(), ex);
                continue;
            }

            Entity entity = mekFileParser.getEntity();
            if (entity == null) {
                LOGGER.warn("mekFileParser returned a null entity {}", name);
                continue;
            }

            printWriter.println("\t<custom>");
            String escapedName = MHQXMLUtility.escape(name);
            printWriter.println("\t\t<name>" + escapedName + "</name>");
            if (entity instanceof Mek) {
                printWriter.print("\t\t<mtf><![CDATA[");
                printWriter.print(((Mek) entity).getMtf());
                printWriter.println("]]></mtf>");
            } else {
                try {
                    BuildingBlock block = BLKFile.getBlock(entity);
                    printWriter.print("\t\t<blk><![CDATA[");
                    for (String data : block.getAllDataAsString()) {
                        if (data.isEmpty()) {
                            continue;
                        }
                        printWriter.println(data);
                    }
                    printWriter.println("]]></blk>");
                } catch (EntitySavingException e) {
                    LOGGER.error("Failed to save custom entity {}", entity.getDisplayName(), e);
                }
            }
            printWriter.println("\t</custom>");
        }
    }

    public ArrayList<PlanetarySystem> getSystems() {
        ArrayList<PlanetarySystem> systems = new ArrayList<>();
        for (String key : this.systemsInstance.getSystems().keySet()) {
            systems.add(this.systemsInstance.getSystems().get(key));
        }
        return systems;
    }

    public Collection<PlanetarySystem> getPlanetarySystemOverrides() {
        return Collections.unmodifiableCollection(planetarySystemOverrides.values());
    }

    public void setPlanetarySystemOverrides(Collection<PlanetarySystem> overrides) throws IOException {
        planetarySystemOverrides.clear();
        if (overrides != null) {
            for (PlanetarySystem override : overrides) {
                addPlanetarySystemOverride(override);
            }
        }
        refreshPlanetarySystemOverlay();
    }

    public PlanetarySystem putPlanetarySystemOverride(PlanetarySystem system) throws IOException {
        if ((system == null) || (system.getId() == null) || system.getId().isBlank()) {
            throw new IOException("Cannot save planetary system edits without a system id.");
        }
        PlanetarySystem savedSystem = PlanetarySystemYamlIO.copy(system);
        planetarySystemOverrides.put(savedSystem.getId(), savedSystem);
        refreshPlanetarySystemOverlay();
        return savedSystem;
    }

    public boolean removePlanetarySystemOverride(String systemId) {
        if ((systemId == null) || systemId.isBlank()) {
            return false;
        }
        boolean removed = planetarySystemOverrides.remove(systemId) != null;
        refreshPlanetarySystemOverlay();
        return removed;
    }

    public boolean hasPlanetarySystemOverride(String systemId) {
        return (systemId != null) && planetarySystemOverrides.containsKey(systemId);
    }

    public void refreshPlanetarySystemOverlay() {
        systemsInstance = Systems.activateCampaignSystems(planetarySystemOverrides.values());
    }

    private void addPlanetarySystemOverride(PlanetarySystem system) {
        if ((system != null) && (system.getId() != null) && !system.getId().isBlank()) {
            planetarySystemOverrides.put(system.getId(), system);
        }
    }

    public PlanetarySystem getSystemById(String id) {
        return this.systemsInstance.getSystemById(id);
    }

    public Vector<String> getSystemNames() {
        Vector<String> systemNames = new Vector<>();
        for (PlanetarySystem key : this.systemsInstance.getSystems().values()) {
            systemNames.add(key.getPrintableName(getLocalDate()));
        }
        return systemNames;
    }

    public PlanetarySystem getSystemByName(String name) {
        return this.systemsInstance.getSystemByName(name, getLocalDate());
    }

    // region Ranks

    // endregion Ranks

    public Accountant getAccountant() {
        return new Accountant(this);
    }

    /**
     * Calculates and returns a {@code JumpPath} between two planetary systems, using default parameters for jump range
     * and travel safety.
     *
     * <p>This method provides a convenient way to compute the most likely or optimal jump path from the specified
     * starting system to the destination system. Internal behavior and constraints are determined by the method's
     * default parameter settings.</p>
     *
     * @param start the starting {@link PlanetarySystem}
     * @param end   the destination {@link PlanetarySystem}
     *
     * @return the calculated {@link JumpPath} between the two systems
     */
    public JumpPath calculateJumpPath(PlanetarySystem start, PlanetarySystem end) {
        return calculateJumpPath(start, end, true, true);
    }

    /**
     * Calculates the optimal jump path between two planetary systems using the A* algorithm.
     *
     * <p>This implementation minimizes a combination of jump counts and recharge times to find the most efficient
     * route between systems. The algorithm uses a heuristic based on straight-line distance combined with actual path
     * costs from the starting system.</p>
     *
     * <p>The algorithm will optionally avoid systems without population when the {@code
     * isAvoidingEmptySystems} flag equals {@code true}.</p>
     *
     * <p>Implementation is based on:
     * <a href="http://www.policyalmanac.org/games/aStarTutorial.htm">Policy Almanac A* Tutorial</a></p>
     *
     * @param start                The starting planetary system
     * @param end                  The destination planetary system
     * @param skipAccessCheck      {@code true} to skip checking for Outlaw status in system, {@code false} otherwise.
     *                             Should be {@code false} when determining contract-related jump paths as system access
     *                             is guaranteed for contract target systems.
     * @param skipEmptySystemCheck {@code true} to skip checking for empty system status, {@code false} otherwise.
     *                             Should be {@code false} when determining contract-related jump paths.
     *
     * @return A {@link JumpPath} containing the sequence of systems to traverse, or {@code null} if no valid path
     *       exists between the systems. If start and end are the same system, returns a path containing only that
     *       system.
     */
    public JumpPath calculateJumpPath(PlanetarySystem start, PlanetarySystem end, boolean skipAccessCheck,
          boolean skipEmptySystemCheck) {
        // Handle edge cases
        if (null == start) {
            return new JumpPath();
        }

        if ((null == end) || start.getId().equals(end.getId())) {
            JumpPath jumpPath = new JumpPath();
            jumpPath.addSystem(start);
            return jumpPath;
        }

        // Shortcuts to ensure we're not processing a lot of data when we're unable to reach the target system
        if (!skipEmptySystemCheck && getPlayerForce().isAvoidingEmptySystems()
                  && end.getPopulation(currentDay) == 0) {
            new ImmersiveDialogSimple(this, getPlayerForce().getHumanResources()
                                                  .getSeniorAdminPerson(getCampaignOptions(),
                                                        getPlayerForce().isClanForce(),
                                                        getLocalDate()), null,
                  String.format(resources.getString("unableToEnterSystem.abandoned.ic"), getCommanderAddress()),
                  null, resources.getString("unableToEnterSystem.abandoned.ooc"), null, false);

            return new JumpPath();
        }

        List<AbstractContract> activeAtBContracts = getActiveContracts();

        FactionHints factionHints = FactionHints.getInstance();
        if (!skipAccessCheck && campaignOptions.isUseFactionStandingOutlawedSafe()) {
            boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(getPlayerForce().getFaction(),
                  getPlayerForce().getFactionStandings(),
                  start, end, currentDay, activeAtBContracts, factionHints);
            if (!canAccessSystem) {
                new ImmersiveDialogSimple(this, getPlayerForce().getHumanResources()
                                                      .getSeniorAdminPerson(getCampaignOptions(),
                                                            getPlayerForce().isClanForce(),
                                                            getLocalDate()), null,
                      String.format(resources.getString("unableToEnterSystem.outlawed.ic"), getCommanderAddress()),
                      null, resources.getString("unableToEnterSystem.outlawed.ooc"), null, false);

                return new JumpPath();
            }
        }

        // Initialize A* algorithm variables
        String startKey = start.getId();
        String endKey = end.getId();

        Set<String> closed = new HashSet<>();
        Set<String> open = new HashSet<>();

        Map<String, String> parent = new HashMap<>();
        Map<String, Double> scoreH = new HashMap<>(); // Heuristic scores (estimated cost to goal)
        Map<String, Double> scoreG = new HashMap<>(); // Path costs from start

        // Precompute heuristics
        Map<String, PlanetarySystem> allSystems = this.systemsInstance.getSystems();

        for (Entry<String, PlanetarySystem> entry : allSystems.entrySet()) {
            scoreH.put(entry.getKey(), end.getDistanceTo(entry.getValue()));
        }

        // Initialize starting node
        String current = startKey;
        scoreG.put(current, 0.0);
        closed.add(current);

        // We need this additional check as later we're going to be comparing neighbors, rather than start point.
        // Which means that if we're passing through more than one Outlawed system en route to our escape our
        // progress will be blocked.
        boolean isEscapingOutlawing = !FactionStandingUtilities.canEnterTargetSystem(getPlayerForce().getFaction(),
              getPlayerForce().getFactionStandings(),
              null, start, currentDay, activeAtBContracts, factionHints);

        // A* search
        final int MAX_JUMPS = 10000;
        for (int jumps = 0; jumps < MAX_JUMPS; jumps++) {
            PlanetarySystem currentSystem = systemsInstance.getSystemById(current);

            boolean isUseCommandCircuits =
                  FactionStandingUtilities.isUseCommandCircuit(getPlayerForce().isOverridingCommandCircuitRequirements(),
                        gmMode,
                        campaignOptions.isUseFactionStandingCommandCircuitSafe(),
                        getPlayerForce().getFactionStandings(), getFutureContracts());

            // Get current node's information
            double currentG = scoreG.get(current) + currentSystem.getRechargeTime(getLocalDate(), isUseCommandCircuits);
            final String localCurrent = current;

            // Explore neighbors
            systemsInstance.visitNearbySystems(currentSystem, 30, neighborSystem -> {
                String neighborId = neighborSystem.getId();

                // Skip systems without population if avoiding empty systems
                if (!skipEmptySystemCheck && getPlayerForce().isAvoidingEmptySystems()
                          && neighborSystem.getPopulation(currentDay) == 0) {
                    return;
                }

                // Skip systems where the campaign is outlawed
                if (!skipAccessCheck &&
                          !isEscapingOutlawing &&
                          campaignOptions.isUseFactionStandingOutlawedSafe()) {
                    boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(getPlayerForce().getFaction(),
                          getPlayerForce().getFactionStandings(),
                          currentSystem, neighborSystem, currentDay, activeAtBContracts, factionHints);
                    if (!canAccessSystem) {
                        return;
                    }
                }

                if (closed.contains(neighborId)) {
                    return; // Already evaluated
                }

                if (open.contains(neighborId)) {
                    // Check if this path is better than the previously found one
                    if (currentG < scoreG.get(neighborId)) {
                        scoreG.put(neighborId, currentG);
                        parent.put(neighborId, localCurrent);
                    }
                } else {
                    // Discover a new node
                    scoreG.put(neighborId, currentG);
                    parent.put(neighborId, localCurrent);
                    open.add(neighborId);
                }
            });

            // Find the open node with the lowest f score
            String bestMatch = findNodeWithLowestFScore(open, scoreG, scoreH);

            if (bestMatch == null) {
                break; // No path exists
            }

            // Move to the best node
            current = bestMatch;
            closed.add(current);
            open.remove(current);

            // Check if we've reached the destination
            if (current.equals(endKey)) {
                return reconstructPath(current, parent, systemsInstance);
            }
        }

        // No path found or maximum jumps reached
        return reconstructPath(current, parent, systemsInstance);
    }

    /**
     * Finds the node in the open set with the lowest f-score (g + h).
     *
     * @param openSet The set of nodes to evaluate
     * @param gScores Map of path costs from start
     * @param hScores Map of heuristic distances to goal
     *
     * @return The node with the lowest f-score, or null if openSet is empty
     */
    private String findNodeWithLowestFScore(Set<String> openSet, Map<String, Double> gScores,
          Map<String, Double> hScores) {
        String bestMatch = null;
        double bestF = Double.POSITIVE_INFINITY;

        for (String candidate : openSet) {
            double f = gScores.get(candidate) + hScores.get(candidate);
            if (f < bestF) {
                bestMatch = candidate;
                bestF = f;
            }
        }

        return bestMatch;
    }

    /**
     * Reconstructs the path from the parent map.
     *
     * @param current         The final node in the path
     * @param parent          Map of parent nodes
     * @param systemsInstance The systems registry
     *
     * @return A JumpPath containing the sequence of systems
     */
    private JumpPath reconstructPath(String current, Map<String, String> parent, Systems systemsInstance) {
        // Reconstruct path
        List<PlanetarySystem> path = new ArrayList<>();
        String nextKey = current;

        while (nextKey != null) {
            path.add(systemsInstance.getSystemById(nextKey));
            nextKey = parent.get(nextKey);
        }

        // Create the final path in the correct order (start to end)
        JumpPath finalPath = new JumpPath();
        for (int i = path.size() - 1; i >= 0; i--) {
            finalPath.addSystem(path.get(i));
        }

        return finalPath;
    }

    /**
     * Calculates simplified travel time. Travel time is calculated by dividing distance (in LY) by 20 and multiplying
     * the result by 7.
     *
     * @param destination the planetary system being traveled to
     *
     * @return the simplified travel time in days
     */
    public int getSimplifiedTravelTime(PlanetarySystem destination) {
        if (Objects.equals(getCurrentSystem(), destination)) {
            return 0;
        } else {
            // I came to the value of 20 by eyeballing the average distance between planets within the Inner Sphere.
            // It looked to be around 15-20LY, so 20LY seemed a good gauge
            return (int) ((getCurrentSystem().getDistanceTo(destination) / 20) * 7);
        }
    }

    /**
     * Calculates the {@link TargetRoll} required for a technician to work on a specific part task.
     *
     * <p>This method determines task difficulty and eligibility by evaluating the technician's skills, penalties due
     * to work mode, unit and part constraints, time availability, helper modifiers, and campaign options. It produces
     * context-specific messages when tasks are impossible due to skill, resource, or situation limitations.</p>
     *
     * <p>The result will reflect all applicable modifiers (such as overtime or era-based penalties) and communicates
     * if a task is impossible, or has automatic success (e.g., for infantry refits).</p>
     *
     * @param partWork the part work task to be performed
     * @param tech     the technician assigned to the task
     *
     * @return a {@link TargetRoll} capturing the total target value and reason for success or impossibility
     */
    public TargetRoll getTargetFor(final IPartWork partWork, final Person tech) {
        final Skill skill = tech.getSkillForWorkingOn(partWork);
        int modePenalty = partWork.getMode().expReduction;
        SkillModifierData skillModifierData = tech.getSkillModifierData();

        int actualSkillLevel = EXP_NONE;
        if (skill != null) {
            actualSkillLevel = skill.getExperienceLevel(skillModifierData);
        }
        int effectiveSkillLevel = actualSkillLevel - modePenalty;

        if ((partWork.getUnit() != null) && !partWork.getUnit().isAvailable(partWork instanceof Refit)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "This unit is not currently available!");
        } else if ((partWork.getTech() != null) && !partWork.getTech().equals(tech)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Already being worked on by another team");
        } else if (skill == null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Assigned tech does not have the right skills");
        } else if (!getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN) && (partWork.getSkillMin() > effectiveSkillLevel)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is beyond this tech's skill level");
        } else if (partWork.getSkillMin() > SkillType.EXP_LEGENDARY) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
        } else if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
        } else if ((partWork instanceof IFabricatable fabricatable) && fabricatable.isFabricating()) {
            // A task flagged for fabrication that is no longer eligible (tech rating above C without a
            // factory-grade facility) cannot be worked on.
            String cannotFabricateReason = fabricatable.canFabricate(tech);
            if (!cannotFabricateReason.isBlank()) {
                return new TargetRoll(TargetRoll.IMPOSSIBLE,
                      "This cannot be fabricated here: " + cannotFabricateReason);
            }
            // Each fabrication attempt is paid up front
            final Money fabricationCost = fabricatable.getFabricationCost(tech);
            if (!fabricationCost.isZero() && playerForce.getFinances().getBalance().isLessThan(fabricationCost)) {
                return new TargetRoll(TargetRoll.IMPOSSIBLE, "Cannot afford this fabrication attempt.");
            }
        } else if ((partWork instanceof MissingPart missingPart) && (missingPart.findReplacement(false) == null)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Replacement part not available.");
        }

        final int techTime = isOvertimeAllowed() ?
                                   tech.getMinutesLeft() + tech.getOvertimeLeft() :
                                   tech.getMinutesLeft();
        if (!(partWork instanceof Refit) && (techTime <= 0)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "The tech has no time left.");
        }

        final String notFixable = partWork.checkFixable();
        if (notFixable != null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, notFixable);
        }

        // if this is an infantry refit, then automatic success
        if ((partWork instanceof Refit) &&
                  (partWork.getUnit() != null) &&
                  partWork.getUnit().isConventionalInfantry()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "infantry refit");
        }

        // Reloading a Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) is a resupply, not a skill repair: it
        // succeeds automatically (the warehouse-stock check is handled by checkFixable above), so a self-crewed
        // infantry unit - which has no mechanic tech type - is not penalised or able to fail the reload.
        if (partWork instanceof InfantryDisposableWeaponPart) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "disposable weapon reload");
        }

        // Reloading field-gun ammo on a self-crewed conventional infantry unit is likewise a resupply. CI has no
        // mechanic tech type, so its own officer must not be penalised or able to fail the reload. (Field guns on
        // support vehicles keep their normal mechanic-teched repair roll, as those units have a valid tech type.)
        if ((partWork instanceof InfantryAmmoBin) && (partWork.getUnit() != null)
                  && partWork.getUnit().isConventionalInfantry()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "infantry field gun reload");
        }

        // If we are using the MoF rule, then we will ignore mode penalty here
        // and instead assign it as a straight penalty
        if (getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN)) {
            modePenalty = 0;
        }

        // this is ugly, if the mode penalty drops you to green, you drop two levels instead of one
        int value = skill.getFinalSkillValue(skillModifierData) + modePenalty;
        if ((modePenalty > 0) && (SkillType.EXP_GREEN == effectiveSkillLevel)) {
            value++;
        }
        final TargetRoll target = new TargetRoll(value, SkillType.getExperienceLevelName(effectiveSkillLevel));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllMods(tech));

        if (getCampaignOptions().get(CampaignOption.USE_ERA_MODS)) {
            target.addModifier(getPlayerForce().getFaction().getEraMod(getGameYear()), "era");
        }

        Familiarity familiarity = getCampaignOptions().get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        Unit partUnit = partWork.getUnit();
        if (familiarity.isEnabled() && partUnit != null) {
            Entity partEntity = partUnit.getEntity();

            if (partEntity != null) {
                int bonus = tech.getChassisFamiliarityTechBonus(familiarity, partEntity, true);
                if (bonus != 0) {
                    target.addModifier(-bonus, "Chassis Familiarity");
                }
            }
        }

        final boolean isOvertime;
        if (isOvertimeAllowed() && (tech.isTaskOvertime(partWork) || partWork.hasWorkedOvertime())) {
            target.addModifier(3, "overtime");
            isOvertime = true;
        } else {
            isOvertime = false;
        }

        final int minutes = Math.min(partWork.getTimeLeft(), techTime);

        int helpMod;
        if ((partWork.getUnit() != null) && partWork.getUnit().isSelfCrewed()) {
            helpMod = getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
        } else {
            final int helpers = getPlayerForce().getHumanResources().getAvailableAsTechs(minutes,
                  isOvertime,
                  isOvertimeAllowed(),
                  getCampaignOptions());
            helpMod = getShorthandedMod(helpers, false);
            // we may have just gone overtime with our helpers
            if (!isOvertime && getPlayerForce().getHumanResources().getAsTechPoolMinutes() < (minutes * helpers)) {
                target.addModifier(3, "overtime astechs");
            }
        }

        if (partWork.getShorthandedMod() > helpMod) {
            helpMod = partWork.getShorthandedMod();
        }

        if (helpMod > 0) {
            target.addModifier(helpMod, "shorthanded");
        }
        return target;
    }

    /**
     * Prepares a skill check for acquiring the specified item or unit using the default campaign logistics person,
     * applying all standard campaign rules and options.
     *
     * @param acquisition the {@link IAcquisitionWork} describing the part, supply, or unit to be acquired
     *
     * @return a {@link SkillCheck} reflecting the acquisition complexity
     */
    public SkillCheck checkAcquisition(final IAcquisitionWork acquisition) {
        return checkAcquisition(acquisition, getPlayerForce().getHumanResources()
                                                   .getLogisticsPerson(getCampaignOptions(),
                                                         getPlayerForce().isClanForce(),
                                                         getLocalDate()));
    }

    /**
     * Prepares a skill check for acquiring the specified item or unit with the given person, using default campaign
     * settings for other options.
     *
     * @param acquisition the {@link IAcquisitionWork} describing the part, supply, or unit to be acquired
     * @param person      the {@link Person} to attempt the acquisition, or {@code null} if unavailable
     *
     * @return a {@link SkillCheck} reflecting the acquisition complexity
     */
    public SkillCheck checkAcquisition(IAcquisitionWork acquisition, @Nullable Person person) {
        return checkAcquisition(acquisition, person, false);
    }

    /**
     * Prepares a skill check for acquiring the specified item or unit while ignoring real acquisition personnel. A
     * synthetic person with baseline skills is used.
     *
     * @param acquisition the {@link IAcquisitionWork} describing the part, supply, or unit to be acquired
     *
     * @return a {@link SkillCheck} reflecting the acquisition complexity
     */
    public SkillCheck checkGenericAcquisition(final IAcquisitionWork acquisition) {
        return checkAcquisition(acquisition, getGenericAcquisitionPerson(), false);
    }

    private Person getGenericAcquisitionPerson() {
        if (genericAcquisitionPerson == null) {
            genericAcquisitionPerson = createGenericAcquisitionPerson(S_NEGOTIATION, S_ADMIN, S_TECH_MECHANIC);
        }
        return genericAcquisitionPerson;
    }

    /**
     * Creates a person used for generic acquisitions. See {@link #checkGenericAcquisition(IAcquisitionWork)}
     *
     * @param skills the list of skills to prepopulate
     */
    private Person createGenericAcquisitionPerson(String... skills) {
        Person person = new Person(this);
        Arrays.stream(skills).forEach(skillName -> {
            person.addSkill(skillName, SkillType.getType(skillName).getRegularLevel(), 0);
        });
        return person;
    }

    public PlanetaryConditions getCurrentPlanetaryConditions(Scenario scenario) {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        if (scenario instanceof AtBScenario atBScenario) {
            if (getCampaignOptions().get(CampaignOption.USE_LIGHT_CONDITIONS)) {
                planetaryConditions.setLight(atBScenario.getLight());
            }
            if (getCampaignOptions().get(CampaignOption.USE_WEATHER_CONDITIONS)) {
                planetaryConditions.setWeather(atBScenario.getWeather());
                planetaryConditions.setWind(atBScenario.getWind());
                planetaryConditions.setFog(atBScenario.getFog());
                planetaryConditions.setEMI(atBScenario.getEMI());
                planetaryConditions.setBlowingSand(atBScenario.getBlowingSand());
                planetaryConditions.setTemperature(atBScenario.getModifiedTemperature());

            }
            if (getCampaignOptions().get(CampaignOption.USE_PLANETARY_CONDITIONS)) {
                planetaryConditions.setAtmosphere(atBScenario.getAtmosphere());
                planetaryConditions.setGravity(atBScenario.getGravity());
            }
        } else {
            planetaryConditions = scenario.createPlanetaryConditions();
        }

        return planetaryConditions;

    }

    /**
     * Prepares a skill check for acquiring a specific part or unit, factoring in campaign options, acquisition details,
     * the person attempting the acquisition, and whether acquisitions personnel should be ignored.
     *
     * <p>This method evaluates a sequence of rules and conditions to determine whether the acquisition is possible,
     * impossible, automatically successful, or automatically fails for the period due to cooldowns. Otherwise, it
     * computes the target roll value based on the skill of the assigned (real or synthetic) person and all relevant
     * modifiers such as item attributes, availability, campaign configuration (including AtB and "Gray Monday"
     * effects), technical year, and extinction.</p>
     *
     * <p>The possible skill check target numbers are:</p>
     * <ul>
     *   <li>{@code TargetRoll.AUTOMATIC_SUCCESS} if acquisitions are set to be automatic in the campaign options.</li>
     *   <li>{@code TargetRoll.IMPOSSIBLE} if the acquisition is forbidden due to campaign settings, unavailable technology,
     *   personnel limitations, date/tech restrictions, or extinct status.</li>
     *   <li>{@code TargetRoll.AUTOMATIC_FAIL} if the item cannot be acquired this period due to prior attempts
     *   (shopping list/cooldown restriction).</li>
     *   <li>A regular {@link TargetRoll} with calculated difficulty, reflecting the assigned person's skill and all
     *   item/campaign modifiers, if the acquisition is allowed and requires a roll.</li>
     * </ul>
     *
     * @param acquisition     an {@link IAcquisitionWork} object describing the item or unit being requested (contains
     *                        info such as tech base, tech level, and availability)
     * @param person          the {@link Person} assigned to make the acquisition roll; may be {@code null} if no one is
     *                        available/allowed, or if personnel are ignored
     * @param checkDaysToWait if {@code true}, checks for shopping list/cooldown period before allowing the roll
     *
     * @return a {@link SkillCheck} reflecting the acquisition complexity
     */
    public SkillCheck checkAcquisition(IAcquisitionWork acquisition, @Nullable Person person, boolean checkDaysToWait) {
        TargetRollModifier decisiveModifier = null;
        if (getCampaignOptions().get(CampaignOption.ACQUISITIONS_TYPE) == AcquisitionsType.AUTOMATIC) {
            decisiveModifier = new TargetRollModifier(TargetRoll.AUTOMATIC_SUCCESS,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.automaticSuccess"));
        } else if (acquisition.getTechBase() == TechBase.CLAN && !getCampaignOptions().get(CampaignOption.ALLOW_CLAN_PURCHASES)) {
            decisiveModifier = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.clanTech"));
        } else if (acquisition.getTechBase() == TechBase.IS && !getCampaignOptions().get(CampaignOption.ALLOW_IS_PURCHASES)) {
            decisiveModifier = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.ISTech"));
        } else if (getCampaignOptions().get(CampaignOption.TECH_LEVEL) < Utilities.getSimpleTechLevel(acquisition.getTechLevel())) {
            decisiveModifier = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.techLevel"));
        } else if (getCampaignOptions().get(CampaignOption.LIMIT_BY_YEAR) &&
                         !acquisition.isIntroducedBy(getGameYear(), useClanTechBase(), getTechFaction())) {
            decisiveModifier = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.notInvented"));
        } else if (getCampaignOptions().get(CampaignOption.DISALLOW_EXTINCT_STUFF) &&
                         (acquisition.isExtinctIn(getGameYear(), useClanTechBase(), getTechFaction()) ||
                                acquisition.getAvailability().equals(AvailabilityValue.X))) {
            decisiveModifier = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.extinct"));
        } else if (checkDaysToWait &&
                         (getPlayerForce().getShoppingList().getShoppingItem(acquisition.getNewEquipment()) != null)) {
            decisiveModifier = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.waitForNewCycle"));
        } else if (person == null) {
            decisiveModifier = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.noPersonnel"));
        }

        if (person == null) {
            // default to the acquisition person if person == null was passed
            person = getGenericAcquisitionPerson();
        }

        // if you change skill mappings here, make sure to update createGenericAcquisitionPerson,
        // so that it has skills for any case of campaignOptions.getAcquisitionType()
        SkillType skillType = switch (getCampaignOptions().get(CampaignOption.ACQUISITIONS_TYPE)) {
            case ADMINISTRATION -> SkillType.getType(S_ADMIN);
            case NEGOTIATION -> SkillType.getType(S_NEGOTIATION);
            case AUTOMATIC -> SkillType.getType(S_ADMIN); // used as a placeholder, the check succeeds automatically
            case ANY_TECH -> {
                Skill bestTechSkill = person.getBestTechSkill();
                // since the person has no tech skills, we can create the skill check for any of them
                yield (bestTechSkill == null) ? SkillType.getType(S_TECH_MECHANIC) : bestTechSkill.getType();
            }
        };
        if ((decisiveModifier == null) && !person.hasSkill(skillType.getName())) {
            decisiveModifier = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL,
                  getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.missingRequiredSkill"));
        }

        if (decisiveModifier != null) {
            return new SkillCheck(person, skillType, new TargetRoll(decisiveModifier));
        }

        List<TargetRollModifier> modifiers = new ArrayList<>(acquisition.getAllAcquisitionMods().getModifiers());
        if (getCampaignOptions().isUseStratCon() && getCampaignOptions().get(CampaignOption.RESTRICT_PARTS_BY_MISSION)) {
            int contractAvailability = findPartsAvailabilityLevel();
            if (contractAvailability != 0) {
                modifiers.add(new TargetRollModifier(contractAvailability,
                      getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.contractPartAvailability")));
            }
        }
        if (isGrayMonday(currentDay, getCampaignOptions().get(CampaignOption.SIMULATE_GRAY_MONDAY))) {
            modifiers.add(new TargetRollModifier(4, getTextAt(ACTION_CHECK_BUNDLE, "acquisition.modifier.grayMonday")));
        }

        return person.checkSkill(skillType.getName(), this).withExternalModifiers(modifiers);
    }

    public int findPartsAvailabilityLevel() {
        Integer availabilityModifier = null;
        for (AbstractContract contract : getActiveContracts()) {
            int contractAvailability = contract.getObjectiveType().calculatePartsAvailabilityLevel();

            if (availabilityModifier == null || contractAvailability < availabilityModifier) {
                availabilityModifier = contractAvailability;
            }
        }

        return Objects.requireNonNullElse(availabilityModifier, 0);
    }

    public void resetAsTechPool() {
        emptyAsTechPool();
        fillAsTechPool();
    }

    public void emptyAsTechPool() {
        final int currentAsTechs = getPlayerForce().getHumanResources().getTemporaryAsTechPool();
        getPlayerForce().getHumanResources().decreaseAsTechPool(this, currentAsTechs);
    }

    public void fillAsTechPool() {
        final int need = getPlayerForce().getHumanResources().getAsTechNeed(campaignOptions);
        if (need > 0) {
            getPlayerForce().getHumanResources().increaseAsTechPool(this, need);
        }
    }

    public int getNumberAsTechs() {
        return getNumberPrimaryAsTechs() + getNumberSecondaryAsTechs();
    }

    /**
     * Calculates the total number of primary AsTechs available in the campaign.
     *
     * <p>This method iterates through all active personnel whose <b>primary role</b> is AsTech, who are not
     * currently deployed, and are employed. For each such person, if the campaign option {@code isUseUsefulAsTechs} is
     * enabled, their total skill level in {@link SkillType#S_ASTECH} is added; otherwise, each person simply counts as
     * one AsTech regardless of skill.</p>
     *
     * @return the total number of primary AsTechs in the campaign
     */
    public int getNumberPrimaryAsTechs() {
        boolean isUseUsefulAsTechs = getCampaignOptions().get(CampaignOption.USE_USEFUL_AS_TECHS);

        int asTechs = getPlayerForce().getHumanResources().getTemporaryAsTechPool();

        for (Person person : getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isAstech() && !person.isDeployed() && person.isEmployed()) {
                // All skilled assistants contribute 1 to the pool, regardless of skill level
                asTechs++;

                // They then contribution additional 'assistants' to the pool based on their skill level
                asTechs += isUseUsefulAsTechs ? getAdvancedAsTechContribution(person) : 0;
            }
        }

        return asTechs;
    }

    /**
     * Calculates the individual AsTech contribution for a person based on their {@link SkillType#S_ASTECH} skill.
     *
     * <p>If the person has the {@link SkillType#S_ASTECH} skill, this returns their total skill level considering
     * all modifiers. If the skill is absent, returns {@code 0}.</p>
     *
     * @param person the {@link Person} whose contribution is to be calculated
     *
     * @return the total skill level for {@link SkillType#S_ASTECH}, or {@code 0} if not present
     *
     * @since 0.50.07
     */
    private static int getAdvancedAsTechContribution(Person person) {
        return person.getAdvancedAsTechContribution();
    }

    /**
     * Calculates the total number of secondary AsTechs available in the campaign.
     *
     * <p>This method iterates through all active personnel whose <b>secondary role</b> is AsTech, who are not
     * currently deployed, and are employed. For each such person, if the campaign option {@code isUseUsefulAsTechs} is
     * enabled, their total skill level in {@link SkillType#S_ASTECH} is added; otherwise, each person simply counts as
     * one AsTech regardless of skill.</p>
     *
     * @return the total number of secondary AsTechs in the campaign
     */
    public int getNumberSecondaryAsTechs() {
        boolean isUseUsefulAsTechs = getCampaignOptions().get(CampaignOption.USE_USEFUL_AS_TECHS);

        int asTechs = 0;

        for (Person person : getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.getSecondaryRole().isAstech() && !person.isDeployed() && person.isEmployed()) {
                // All skilled assistants contribute 1 to the pool, regardless of skill level
                asTechs++;

                // They then contribution additional 'assistants' to the pool based on their skill level
                asTechs += isUseUsefulAsTechs ? getAdvancedAsTechContribution(person) : 0;
            }
        }

        return asTechs;
    }

    public int getShorthandedMod(int availableHelp, boolean medicalStaff) {
        if (medicalStaff) {
            availableHelp += 2;
        }
        int helpMod = 0;
        if (availableHelp == 0) {
            helpMod = 4;
        } else if (availableHelp == 1) {
            helpMod = 3;
        } else if (availableHelp < 4) {
            helpMod = 2;
        } else if (availableHelp < 6) {
            helpMod = 1;
        }
        return helpMod;
    }

    public int getShorthandedModForCrews(final @Nullable Crew crew) {
        final int hits = (crew == null) ? 5 : crew.getHits();
        if (hits >= 5) {
            return 4;
        } else if (hits == 4) {
            return 3;
        } else if (hits == 3) {
            return 2;
        } else if (hits > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getMedicsPerDoctor() {
        int numDocs = getPlayerForce().getHumanResources().getDoctors().size();
        int numMedics = getNumberMedics();
        if (numDocs == 0) {
            return 0;
        }
        // TODO: figure out what to do with fractions
        return Math.min(numMedics / numDocs, 4);
    }

    /**
     * @return the number of medics in the campaign including any in the temporary medic pool
     */
    public int getNumberMedics() {
        int permanentMedicPool = getPermanentMedicPool();
        return getPlayerForce().getHumanResources().getTemporaryMedicPool() + permanentMedicPool;
    }

    /**
     * Calculates the total number of medics available in the campaign by summing the skill levels in the
     * {@link SkillType#S_MEDTECH} skill for all eligible personnel.
     *
     * <p>Eligible personnel must have either a primary or secondary role as a medic, must not be currently deployed,
     * and must be employed.</p>
     *
     * <p>For each eligible person, their total skill level in {@link SkillType#S_MEDTECH} (including all
     * modifiers) is added to the running total.</p>
     *
     * @return The total number of medics available.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getPermanentMedicPool() {
        final boolean isUseUsefulMedics = getCampaignOptions().get(CampaignOption.USE_USEFUL_MEDICS);
        int permanentMedicPool = 0;

        for (Person person : getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isMedic() || person.getSecondaryRole().isMedic()) {
                if (person.isDeployed()) {
                    continue;
                }

                if (!person.isEmployed()) {
                    continue;
                }

                if (!isUseUsefulMedics) {
                    permanentMedicPool++;
                } else {
                    Skill medicSkill = person.getSkill(S_MEDTECH);
                    if (medicSkill != null) {
                        PersonnelOptions options = person.getOptions();
                        Attributes attributes = person.getATOWAttributes();

                        SkillModifierData skillModifierData = person.getSkillModifierData();
                        int skillLevel = medicSkill.getTotalSkillLevel(skillModifierData);

                        // All skilled assistants contribute 1 to the pool, regardless of skill level
                        permanentMedicPool++;

                        // It is possible for very poorly skilled personnel to actually reduce the pool, this is by
                        // design. Not all help is helpful.
                        permanentMedicPool += (int) floor(skillLevel / ASSISTANT_SKILL_LEVEL_DIVIDER);
                    }
                }
            }
        }

        return permanentMedicPool;
    }

    public void resetMedicPool() {
        emptyMedicPool();
        fillMedicPool();
    }

    public void emptyMedicPool() {
        final int currentMedicPool = getPlayerForce().getHumanResources().getTemporaryMedicPool();
        getPlayerForce().getHumanResources().decreaseMedicPool(this, currentMedicPool);
    }

    public void fillMedicPool() {
        final int need = getPlayerForce().getHumanResources().getMedicsNeed();
        if (need > 0) {
            getPlayerForce().getHumanResources().increaseMedicPool(this, need);
        }
    }

    /**
     * Increases the temp crew pool for a specific personnel role and fires the appropriate event
     *
     * @param role   the personnel role
     * @param amount the amount to increase by
     */
    public void increaseTempCrewPool(PersonnelRole role, int amount) {
        // Event is fired in setTempCrewPool
        int size = getPlayerForce().getHumanResources().getTempCrewPool(role) + amount;
        getPlayerForce().getHumanResources().setTempCrewPool(this, role, size);
    }

    /**
     * Decreases the temp crew pool for a specific personnel role and fires the appropriate event
     *
     * @param role   the personnel role
     * @param amount the amount to decrease by
     */
    public void decreaseTempCrewPool(PersonnelRole role, int amount) {
        // Event is fired in setTempCrewPool
        int size = max(0, getPlayerForce().getHumanResources().getTempCrewPool(role) - amount);
        getPlayerForce().getHumanResources().setTempCrewPool(this, role, size);
    }

    /**
     * Fires the appropriate pool changed event for a specific personnel role
     *
     * @param role   the personnel role
     * @param change the change amount (positive for increase, negative for decrease)
     */
    private void fireTempCrewPoolChangedEvent(PersonnelRole role, int change) {
        switch (role) {
            case SOLDIER -> MekHQ.triggerEvent(new SoldierPoolChangedEvent(this, change));
            case BATTLE_ARMOUR -> MekHQ.triggerEvent(new BattleArmorPoolChangedEvent(this, change));
            case VEHICLE_CREW_GROUND -> MekHQ.triggerEvent(new VehicleCrewGroundPoolChangedEvent(this, change));
            case VEHICLE_CREW_VTOL -> MekHQ.triggerEvent(new VehicleCrewVTOLPoolChangedEvent(this, change));
            case VEHICLE_CREW_NAVAL -> MekHQ.triggerEvent(new VehicleCrewNavalPoolChangedEvent(this, change));
            case VESSEL_PILOT -> MekHQ.triggerEvent(new VesselPilotPoolChangedEvent(this, change));
            case VESSEL_GUNNER -> MekHQ.triggerEvent(new VesselGunnerPoolChangedEvent(this, change));
            case VESSEL_CREW -> MekHQ.triggerEvent(new VesselCrewPoolChangedEvent(this, change));
            default -> throw new IllegalStateException("Unexpected value: " + role);
        }
    }

    /**
     * Empties the temp crew pool for a specific role by setting it to the number of active temp crew for that role.
     *
     * @param role the personnel role to reduce to the minimum
     */
    public void emptyTempCrewPoolForRole(PersonnelRole role) {
        int size = getPlayerForce().getHumanResources().getTempCrewInUse(this, role);
        getPlayerForce().getHumanResources().setTempCrewPool(this, role, size);
    }

    /**
     * Resets the temp crew pool for a specific role by emptying and then filling it.
     *
     * @param role the personnel role to reset
     */
    public void resetTempCrewPoolForRole(PersonnelRole role) {
        emptyTempCrewPoolForRole(role);
        getPlayerForce().getHumanResources().fillTempCrewPoolForRole(this, getCampaignOptions(), role);
    }

    public GameOptions getGameOptions() {
        return gameOptions;
    }

    public Vector<IBasicOption> getGameOptionsVector() {
        Vector<IBasicOption> options = new Vector<>();
        for (Enumeration<IOptionGroup> i = gameOptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                options.add(option);
            }
        }
        return options;
    }

    public void setGameOptions(final GameOptions gameOptions) {
        this.gameOptions = gameOptions;
        // Keep the Game's reference in sync: MegaMek code (e.g. TeamLoadOutGenerator during scenario setup) reads
        // options through campaign.getGame().getOptions(). Without this, replacing the campaign's options (e.g. when
        // applying a campaign preset) leaves the Game holding a stale GameOptions object, so later updates such as
        // the ALLOWED_YEAR sync are never seen there and bot forces get munitions from the wrong era.
        game.setOptions(gameOptions);
    }

    public void setGameOptions(final Vector<IBasicOption> options) {
        for (final IBasicOption option : options) {
            getGameOptions().getOption(option.getName()).setValue(option.getValue());
        }
        campaignOptions.updateCampaignOptionsFromGameOptions(gameOptions);
        MekHQ.triggerEvent(new OptionsChangedEvent(this));
    }

    /**
     * Imports a {@link Kill} into a campaign.
     *
     * @param k A {@link Kill} to import into the campaign.
     */
    public void importKill(Kill k) {
        if (!kills.containsKey(k.getPilotId())) {
            kills.put(k.getPilotId(), new ArrayList<>());
        }

        kills.get(k.getPilotId()).add(k);
    }

    public void addKill(Kill k) {
        importKill(k);

        if ((getCampaignOptions().get(CampaignOption.KILLS_FOR_XP) > 0) && (getCampaignOptions().get(CampaignOption.KILL_XP_AWARD) > 0)) {
            if ((getKillsFor(k.getPilotId()).size() % getCampaignOptions().get(CampaignOption.KILLS_FOR_XP)) == 0) {
                final UUID id1 = k.getPilotId();
                Person person = getPlayerForce().getHumanResources().getPerson(id1);
                if (null != person) {
                    person.awardXP(this, getCampaignOptions().get(CampaignOption.KILL_XP_AWARD));
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
            }
        }
    }

    public List<Kill> getKills() {
        List<Kill> flattenedKills = new ArrayList<>();
        for (List<Kill> personKills : kills.values()) {
            flattenedKills.addAll(personKills);
        }

        return Collections.unmodifiableList(flattenedKills);
    }

    public List<Kill> getKillsFor(UUID pid) {
        List<Kill> personalKills = kills.get(pid);

        if (personalKills == null) {
            return Collections.emptyList();
        }

        personalKills.sort(Comparator.comparing(Kill::getDate));
        return personalKills;
    }

    public PartsStore getPartsStore() {
        return partsStore;
    }

    public void addCustom(String name) {
        customs.add(name);
    }

    public boolean isCustom(Unit u) {
        return customs.contains(u.getEntity().getShortNameRaw());
    }

    /**
     * borrowed from {@see megamek.MegaMek.Client}
     */
    private synchronized void checkDuplicateNamesDuringAdd(Entity entity) {
        unitNameTracker.add(entity);
    }

    /**
     * If we remove a unit, we may need to update the duplicate identifier.
     *
     * @param entity This is the entity whose name is checked for any duplicates
     */
    private synchronized void checkDuplicateNamesDuringDelete(Entity entity) {
        unitNameTracker.remove(entity, e -> {
            // Regenerate entity names after a deletion
            e.generateShortName();
            e.generateDisplayName();
        });
    }

    /**
     * Returns the text representation of the unit rating based on the selected unit rating method.
     *
     * @return The text representation of the unit rating
     */
    public String getUnitRatingText() {
        boolean useChaosReputation = getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION);

        if (useChaosReputation) {
            if (getCampaignOptions().get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
                // Campaign-level mode stores a pure base; the debt/manual/commander modifiers are applied live here.
                return String.valueOf(ChaosReputation.getEffectiveCampaignLevelReputation(getPlayerForce(),
                      getCampaignOptions(),
                      getLocalDate()));
            }
            return String.valueOf(getPlayerForce().getChaosCampaignReputation());
        } else {
            return String.valueOf(getPlayerForce().getReputationRating(false));
        }
    }

    /**
     * Retrieves the unit rating modifier based on campaign options.
     *
     * @return The unit rating modifier based on the campaign options.
     */
    public int getAtBUnitRatingMod() {
        return getPlayerForce().getAverageSkillLevel(campaignOptions, currentDay).ordinal();
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return randomSkillPreferences;
    }

    public void setRandomSkillPreferences(RandomSkillPreferences prefs) {
        randomSkillPreferences = prefs;
    }

    /**
     * @param planet the starting planet, or null to use the faction default
     */
    public void setStartingSystem(final @Nullable Planet planet) {
        PlanetarySystem startingSystem;
        if (planet == null) {
            final Map<String, PlanetarySystem> systemList = this.systemsInstance.getSystems();
            startingSystem = systemList.get(getPlayerForce().getFaction().getStartingPlanet(getLocalDate()));

            if (startingSystem == null) {
                startingSystem = systemList.get(JOptionPane.showInputDialog(
                      "This faction does not have a starting planet for this era. Please choose a planet."));
                while (startingSystem == null) {
                    startingSystem = systemList.get(JOptionPane.showInputDialog(
                          "This planet you entered does not exist. Please choose a valid planet."));
                }
            }
        } else {
            startingSystem = planet.getParentSystem();
        }
        setLocation(new CurrentLocation(startingSystem, 0));
    }

    /**
     * Clears Transient Game Data for an Entity
     *
     * @param entity the entity to clear the game data for
     */
    public void clearGameData(Entity entity) {
        // First, lets remove any improvised clubs picked up during the combat
        entity.removeMisc(EquipmentTypeLookup.LIMB_CLUB);
        entity.removeMisc(EquipmentTypeLookup.GIRDER_CLUB);
        entity.removeMisc(EquipmentTypeLookup.TREE_CLUB);

        // Then reset mounted equipment
        for (Mounted<?> m : entity.getEquipment()) {
            m.setUsedThisRound(false);
            m.resetJam();
        }

        // And clear out all the flags
        entity.setDeployed(false);
        entity.setElevation(0);
        entity.setPassedThrough(new Vector<>());
        entity.resetFiringArcs();
        entity.resetBays();
        entity.setEvading(false);
        entity.setFacing(0);
        entity.setPosition(null);
        entity.setProne(false);
        entity.setHullDown(false);
        entity.heat = 0;
        entity.heatBuildup = 0;
        entity.underwaterRounds = 0;
        entity.setTransportId(Entity.NONE);
        entity.resetTransporter();
        entity.setDeployRound(0);
        entity.setSwarmAttackerId(Entity.NONE);
        entity.setSwarmTargetId(Entity.NONE);
        entity.setUnloaded(false);
        entity.setDone(false);
        entity.setLastTarget(Entity.NONE);
        entity.setNeverDeployed(true);
        entity.setStuck(false);
        entity.resetCoolantFailureAmount();
        entity.setConversionMode(0);
        entity.setDoomed(false);
        entity.setDestroyed(false);
        entity.setHidden(false);
        entity.clearNarcAndiNarcPods();
        entity.setShutDown(false);
        entity.setSearchlightState(false);

        if (!entity.getSensors().isEmpty()) {
            if (entity.hasBAP()) {
                entity.setNextSensor(entity.getSensors().lastElement());
            } else {
                entity.setNextSensor(entity.getSensors().firstElement());
            }
        }

        if (entity instanceof IBomber bomber) {
            List<BombMounted> mountedBombs = bomber.getBombs();
            if (!mountedBombs.isEmpty()) {
                // These should return an int[] filled with 0's
                BombLoadout intBombChoices = bomber.getIntBombChoices();
                BombLoadout extBombChoices = bomber.getExtBombChoices();
                for (BombMounted m : mountedBombs) {
                    if (m.getBaseShotsLeft() == 1) {
                        if (m.isInternalBomb()) {
                            intBombChoices.addBombs(m.getType().getBombType(), 1);
                        } else {
                            extBombChoices.addBombs(m.getType().getBombType(), 1);
                        }
                    }
                }
                bomber.setIntBombChoices(intBombChoices);
                bomber.setExtBombChoices(extBombChoices);
                bomber.clearBombs();
            }
        }

        if (entity instanceof Mek m) {
            m.setCoolingFlawActive(false);
        } else if (entity instanceof Aero a) {

            if (a.isSpheroid()) {
                entity.setMovementMode(EntityMovementMode.SPHEROID);
            } else {
                entity.setMovementMode(EntityMovementMode.AERODYNE);
            }
            a.setAltitude(5);
            a.setCurrentVelocity(0);
            a.setNextVelocity(0);
        } else if (entity instanceof Tank t) {
            t.unjamTurret(t.getLocTurret());
            t.unjamTurret(t.getLocTurret2());
            t.resetJammedWeapons();
        }
        entity.getSecondaryPositions().clear();
        // TODO: still a lot of stuff to do here, but oh well
        entity.setOwner(player);
        entity.setGame(game);
    }

    /**
     * This function reloads the game entities into the game at the end of scenario resolution, so that entities are
     * properly updated and destroyed ones removed
     */
    public void reloadGameEntities() {
        game.reset();
        getPlayerForce().getHangar().forEachUnit(u -> {
            Entity en = u.getEntity();
            if (null != en) {
                game.addEntity(en, false);
            }
        });
    }

    public void completeMission(@Nullable AbstractContract mission, MissionStatus status) {
        if (mission == null) {
            return;
        }
        mission.setStatus(status);
        Money remainingMoney = Money.zero();
        // check for money in escrow According to FMM(r) pg 179, both failure and breach lead to no further
        // payment even though this seems foolish
        if (mission.getStatus().isSuccess()) {
            remainingMoney = mission.getMonthlyPayOut().multipliedBy(mission.getMonthsLeft(getLocalDate()));

            Money routedPayout = mission.getRoutPayout();

                    remainingMoney = routedPayout == null ? remainingMoney : routedPayout;
   }

        // Shareholders take their cut of the contract's gross final payout, mirroring the monthly share payout.
        final Money grossPayout = remainingMoney;

        // With OverageRepaymentInFinalPayment enabled, salvage the player kept beyond their rights is repaid as part of
        // the final payment, which can push it into a debit (the negative branch below).
        if (getCampaignOptions().get(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT)) {
            remainingMoney = remainingMoney.minus(ContractSettlement.salvageOverage(mission));
        }

        if (remainingMoney.isPositive()) {
            getPlayerForce().getFinances().credit(TransactionType.CONTRACT_PAYMENT,
                  getLocalDate(),
                  remainingMoney,
                  "Remaining payment for " + mission.getName());
            addReport(FINANCES, "Your account has been credited for " +
                                      remainingMoney.toAmountAndSymbolString() +
                                      " for the remaining payout from contract " +
                                      mission.getHyperlinkedName());
        } else if (remainingMoney.isNegative()) {
            getPlayerForce().getFinances().credit(TransactionType.CONTRACT_PAYMENT,
                  getLocalDate(),
                  remainingMoney,
                  "Repaying salvage overages for " + mission.getName());
            addReport(FINANCES, "Your account has been debited for " +
                                      remainingMoney.absolute().toAmountAndSymbolString() +
                                      " to repay salvage taken beyond your rights on the contract " +
                                      mission.getHyperlinkedName());
        }

        ContractSettlement.settleShares(this, mission, grossPayout);

        // This relies on the mission being a Contract, and AtB to be on
        if (getCampaignOptions().isUseStratCon()) {
            setHasActiveContract();
        }
    }

    /***
     * Calculate transit time for supplies based on what planet they are shipping from. To prevent extra computation.
     * This method does not calculate an exact jump path but rather determines the number of jumps crudely by
     * dividing distance in light years by 30 and then rounding up. Total part-time is determined by several by
     * adding the following:
     * - (number of jumps - 1) * 7 days with a minimum value of zero.
     * - transit times from current planet and planet of supply origins in cases where the supply planet is not the
     * same as current planet.
     * - a random 1d6 days for each jump plus 1d6 to simulate all the other
     * logistics of delivery.
     *
     * @param system - A <code>PlanetarySystem</code> object where the supplies are
     *               shipping from
     * @return the number of days that supplies will take to arrive.
     */
    public int calculatePartTransitTime(PlanetarySystem system) {
        // calculate number of jumps by light year distance as the crow flies divided by
        // 30
        // the basic formula assumes 7 days per jump + system transit time on each side
        // + random days equal
        // to (1 + number of jumps) d6
        double distance = system.getDistanceTo(getCurrentSystem());
        // calculate number of jumps by dividing by 30
        int jumps = (int) Math.ceil(distance / 30.0);
        // you need a recharge except for the first jump
        int recharges = max(jumps - 1, 0);
        // if you are delivering from the same planet then no transit times
        int currentTransitTime = (distance > 0) ? (int) Math.ceil(getCurrentSystem().getTimeToJumpPoint(1.0)) : 0;
        int originTransitTime = (distance > 0) ? (int) Math.ceil(system.getTimeToJumpPoint(1.0)) : 0;

        // CO 51 (errata) has much longer average part times.
        // Let's adjust amazonFreeShipping
        // based on what getUnitTransitTime is set in
        // the options in an attempt to get some
        // delivery times more in line with RAW's two-month minimum.
        // Default campaign option is TRANSIT_UNIT_MONTH
        int amazonFreeShipping = switch (campaignOptions.get(CampaignOption.UNIT_TRANSIT_TIME)) {
            case TRANSIT_UNIT_MONTH -> 30 + (d6(14 * (1 + jumps)));
            case TRANSIT_UNIT_WEEK -> 7 + (d6(4 * (1 + jumps)));
            default -> d6(1 + jumps);
        };
        return (recharges * 7) + currentTransitTime + originTransitTime + amazonFreeShipping;
    }

    /**
     * Calculates the transit time for the arrival of parts or supplies based on the availability of the item, a random
     * roll, and campaign-specific transit time settings.
     *
     * <p>
     * The transit time is calculated using the following factors:
     * <ul>
     * <li>A fixed base modifier value defined by campaign rules.</li>
     * <li>A random roll of 1d6 to add variability to the calculation.</li>
     * <li>The availability value of the requested parts or supplies from the
     * acquisition details.</li>
     * </ul>
     *
     * <p>
     * The calculated duration is applied in units (days, weeks, or months) based on
     * the campaign's
     * configuration for transit time.
     * </p>
     *
     * @param availability the availability code of the part or unit being acquired as an integer.
     *
     * @return the number of days required for the parts or units to arrive based on the calculated transit time.
     */
    public int calculatePartTransitTime(int availability) {
        // This is accurate as of the latest rules. It was (BASE_MODIFIER - (roll + availability) / 4) months in the
        // older version.
        final int BASE_MODIFIER = 7; // CamOps p51
        final int roll = d6(1);
        final int total = max(1, (BASE_MODIFIER + roll + availability) / 4); // CamOps p51

        // now step forward through the calendar
        LocalDate arrivalDate = currentDay;
        arrivalDate = switch (campaignOptions.get(CampaignOption.UNIT_TRANSIT_TIME)) {
            case TRANSIT_UNIT_MONTH -> arrivalDate.plusMonths(total);
            case TRANSIT_UNIT_WEEK -> arrivalDate.plusWeeks(total);
            default -> arrivalDate.plusDays(total);
        };

        return Math.toIntExact(ChronoUnit.DAYS.between(getLocalDate(), arrivalDate));
    }

    /**
     * Calculates the transit time for the arrival of parts or supplies based on the availability of the item, a random
     * roll, and campaign-specific transit time settings.
     *
     * <p>
     * The transit time is calculated using the following factors:
     * <ul>
     * <li>A fixed base modifier value defined by campaign rules.</li>
     * <li>A random roll of 1d6 to add variability to the calculation.</li>
     * <li>The availability value of the requested parts or supplies from the
     * acquisition details.</li>
     * </ul>
     *
     * <p>
     * The calculated duration is applied in units (days, weeks, or months) based on
     * the campaign's
     * configuration for transit time.
     * </p>
     *
     * @param availability the Availability of the part
     *
     * @return the number of days required for the parts or units to arrive based on the calculated transit time.
     */
    public int calculatePartTransitTime(AvailabilityValue availability) {
        return calculatePartTransitTime(availability.getIndex());
    }

    /**
     * This returns a PartInventory object detailing the current count for a part on hand, in transit, and ordered.
     *
     * @param part A part to look up its current inventory.
     *
     * @return A PartInventory object detailing the current counts of the part on hand, in transit, and ordered.
     *
     * @see PartInventory
     */
    public PartInventory getPartInventory(Part part) {
        PartInventory inventory = playerForce.getPartInventory(part);

        int nOrdered = 0;
        IAcquisitionWork onOrder = getPlayerForce().getShoppingList().getShoppingItem(part);
        if (null != onOrder) {
            nOrdered += onOrder.getTotalQuantity();
        }
        inventory.setOrdered(nOrdered);
        return inventory;
    }

    public void addLoan(Loan loan) {
        addReport(FINANCES, "You have taken out loan " +
                                  loan +
                                  ". Your account has been credited " +
                                  loan.getPrincipal().toAmountAndSymbolString() +
                                  " for the principal amount.");
        getPlayerForce().getFinances().addLoan(loan);
        MekHQ.triggerEvent(new LoanNewEvent(loan));
        getPlayerForce().getFinances().credit(TransactionType.LOAN_PRINCIPAL,
              getLocalDate(),
              loan.getPrincipal(),
              "Loan principal for " + loan);
    }

    public void payOffLoan(Loan loan) {
        if (getPlayerForce().getFinances().debit(TransactionType.LOAN_PAYMENT,
              getLocalDate(),
              loan.determineRemainingValue(),
              "Loan payoff for " + loan)) {
            addReport(FINANCES, "You have paid off the remaining loan balance of " +
                                      loan.determineRemainingValue().toAmountAndSymbolString() +
                                      " on " +
                                      loan);
            getPlayerForce().getFinances().removeLoan(loan);
            MekHQ.triggerEvent(new LoanPaidEvent(loan));
        } else {
            addReport(FINANCES, "<font color='" +
                                      ReportingUtilities.getNegativeColor() +
                                      "'>You do not have enough funds to pay off " +
                                      loan +
                                      "</font>");
        }
    }

    private CampaignTransporterMap getCampaignTransporterMap(CampaignTransportType campaignTransportType) {
        if (campaignTransportType.isTacticalTransport()) {
            return tacticalTransporters;
        } else if (campaignTransportType.isShipTransport()) {
            return shipTransporters;
        } else if (campaignTransportType.isTowTransport()) {
            return towTransporters;
        }
        return null;
    }

    /**
     * Returns a Map that maps Transporter types to another Map that maps capacity (Double) to UUID of transports for
     * the specific TransportedUnitSummary type
     *
     * @param campaignTransportType type (Enum) of TransportedUnitSummary
     *
     * @return the full map for that campaign transport type
     */
    public Map<TransporterType, Map<Double, Set<UUID>>> getTransports(CampaignTransportType campaignTransportType) {
        return Objects.requireNonNull(getCampaignTransporterMap(campaignTransportType)).getTransporters();
    }

    /**
     * Returns list of transports that have the provided TransporterType and CampaignTransportType
     *
     * @param campaignTransportType type of campaign transport
     * @param transporterType       type of Transporter
     *
     * @return units that have that transport type
     */
    public Set<Unit> getTransportsByType(CampaignTransportType campaignTransportType, TransporterType transporterType) {
        // include transports with no remaining capacity
        return Objects.requireNonNull(getCampaignTransporterMap(campaignTransportType))
                     .getTransportsByType(transporterType, -1.0);
    }

    /**
     * Returns list of transports for the specified AbstractTransportedUnitSummary class/subclass that has transport
     * capacity for the Transporter class/subclass For example, getTransportsByType(SHIP_TRANSPORT, MEK_BAY, 3.0) would
     * return all transports that have 3 or more Mek Bay slots open for the SHIP_TRANSPORT type of assignment.
     *
     * @param campaignTransportType type (Enum) of TransportedUnitSummary
     * @param transporterType       type (Enum) of Transporter
     * @param unitSize              capacity that the transport must be capable of
     *
     * @return units that have that transport type
     */
    public Set<Unit> getTransportsByType(CampaignTransportType campaignTransportType, TransporterType transporterType,
          double unitSize) {
        return Objects.requireNonNull(getCampaignTransporterMap(campaignTransportType))
                     .getTransportsByType(transporterType, unitSize);
    }

    private boolean hasTacticalTransports() {
        return tacticalTransporters.hasTransporters();
    }

    private boolean hasShipTransports() {
        return shipTransporters.hasTransporters();
    }

    private boolean hasTowTransports() {
        return towTransporters.hasTransporters();
    }

    /**
     * Do we have transports for the kind of transport?
     *
     * @param campaignTransportType class of the TransportDetail
     *
     * @return true if it has transporters, false otherwise
     */
    public boolean hasTransports(CampaignTransportType campaignTransportType) {
        if (campaignTransportType.isTacticalTransport()) {
            return hasTacticalTransports();
        } else if (campaignTransportType.isShipTransport()) {
            return hasShipTransports();
        } else if (campaignTransportType.isTowTransport()) {
            return hasTowTransports();
        }
        return false;
    }

    public void initTurnover() {
        getPlayerForce().getHumanResources().getRetirementDefectionTracker().setLastRetirementRoll(getLocalDate());
    }

    public void initAtB(boolean newCampaign) {
        if (!newCampaign) {
            /*
             * Go through all the personnel records and assume the earliest date is the date
             * the unit was founded.
             */
            LocalDate founding = null;
            for (Person person : getPlayerForce().getPersonnel().values()) {
                for (LogEntry logEntry : person.getPersonalLog()) {
                    if ((founding == null) || logEntry.getDate().isBefore(founding)) {
                        founding = logEntry.getDate();
                    }
                }
            }
            /*
             * Go through the personnel records again and assume that any person who joined the unit on the founding
             * date is one of the founding members. Also assume that MWs assigned to a non-Assault `Mek on the date
             * they joined came with that `Mek (which is a less certain assumption)
             */
            for (Person person : getPlayerForce().getPersonnel().values()) {
                LocalDate join = person.getPersonalLog()
                                       .stream()
                                       .filter(e -> e.getDesc().startsWith("Joined "))
                                       .findFirst()
                                       .map(LogEntry::getDate)
                                       .orElse(null);
                if ((join != null) && join.equals(founding)) {
                    person.setFounder(true);
                }
                if (person.getPrimaryRole().isMekWarrior() ||
                          (person.getPrimaryRole().isAerospacePilot() &&
                                 getCampaignOptions().get(CampaignOption.AERO_RECRUITS_HAVE_UNITS)) ||
                          person.getPrimaryRole().isProtoMekPilot()) {
                    for (LogEntry logEntry : person.getPersonalLog()) {
                        if (logEntry.getDate().equals(join) && logEntry.getDesc().startsWith("Assigned to ")) {
                            String mek = logEntry.getDesc().substring(12);
                            MekSummary ms = MekSummaryCache.getInstance().getMek(mek);
                            if (null != ms &&
                                      (person.isFounder() || ms.getWeightClass() < EntityWeightClass.WEIGHT_ASSAULT)) {
                                person.setOriginalUnitWeight(ms.getWeightClass());
                                if (ms.isClan()) {
                                    person.setOriginalUnitTech(Person.TECH_CLAN);
                                } else if (ms.getYear() > 3050) {
                                    // TODO : Fix this so we aren't using a hack that just assumes IS2
                                    person.setOriginalUnitTech(Person.TECH_IS2);
                                }
                                if ((null != person.getUnit()) &&
                                          ms.getName().equals(person.getUnit().getEntity().getShortNameRaw())) {
                                    person.setOriginalUnitId(person.getUnit().getId());
                                }
                            }
                        }
                    }
                }
            }

            playerForce.addAllCombatTeams(getPlayerForce().getFormations(), this);

            // Determine whether there is an active contract
            setHasActiveContract();
        }

        setAtBConfig(AtBConfiguration.loadFromXml());
        RandomFactionGenerator.getInstance().startup(this);
    }

    /**
     * Stop processing AtB events and release memory.
     */
    public void shutdownAtB() {
        RandomFactionGenerator.getInstance().dispose();
    }

    /**
     * Checks if an employee turnover prompt should be displayed based on campaign options, current date, and other
     * conditions (like transit status and campaign start date).
     *
     * <p>The turnover prompt is triggered based on the configured turnover frequency (weekly, monthly, quarterly, or
     * annually), but only after the campaign has been running for at least 6 days and when not in transit.<p>
     *
     * <p>The dialog will show different messages depending on whether there are pending retirees.</p>
     *
     * @return An integer representing the outcome: -1 if turnover prompt should not be displayed, 0 if user selected
     *       "Employee Turnover", 1 if user selected "Advance Day Regardless", 2 if user selected "Cancel Advance Day"
     */
    public int checkTurnoverPrompt() {
        if (!getPlayerForce().getForceDetachment().isOnPlanet()) {
            return -1;
        }

        if (getLocalDate().isBefore(getCampaignStartDate().plusDays(6))) {
            return -1;
        }

        boolean triggerTurnoverPrompt;
        switch (campaignOptions.get(CampaignOption.TURNOVER_FREQUENCY)) {
            case WEEKLY:
                triggerTurnoverPrompt = getLocalDate().getDayOfWeek().equals(DayOfWeek.MONDAY);
                break;
            case MONTHLY:
                triggerTurnoverPrompt = getLocalDate().getDayOfMonth() == getLocalDate().lengthOfMonth();
                break;
            case QUARTERLY:
                triggerTurnoverPrompt = (getLocalDate().getDayOfMonth() == getLocalDate().lengthOfMonth()) &&
                                              (List.of(Month.MARCH, Month.JUNE, Month.SEPTEMBER, Month.DECEMBER)
                                                     .contains(getLocalDate().getMonth()));
                break;
            case ANNUALLY:
                triggerTurnoverPrompt = getLocalDate().getDayOfYear() == getLocalDate().lengthOfYear();
                break;
            default:
                return -1;
        }

        if (!triggerTurnoverPrompt) {
            return -1;
        }

        String dialogTitle;
        String dialogBody;

        if (getPlayerForce().getHumanResources().getRetirementDefectionTracker().getRetirees().isEmpty()) {
            dialogTitle = resources.getString("turnoverRollRequired.text");
            dialogBody = resources.getString("turnoverDialogDescription.text");
        } else {
            dialogTitle = resources.getString("turnoverFinalPayments.text");
            dialogBody = resources.getString("turnoverPersonnelKilled.text");
        }

        Object[] options = { resources.getString("turnoverEmployeeTurnoverDialog.text"),
                             resources.getString("turnoverAdvanceRegardless"),
                             resources.getString("turnoverCancel.text") };

        return JOptionPane.showOptionDialog(null,
              dialogBody,
              dialogTitle,
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.INFORMATION_MESSAGE,
              null,
              options,
              options[0]);
    }

    /**
     * Checks if there are any scenarios that are due based on the current date.
     *
     * @return {@code true} if there are scenarios due, {@code false} otherwise
     */
    public boolean checkScenariosDue() {
        return getActiveContracts(true).stream()
                     .flatMap(m -> m.getCurrentScenarios().stream())
                     .anyMatch(s -> (s.getDate() != null) &&
                                          !(s instanceof AtBScenario) &&
                                          !getLocalDate().isBefore(s.getDate()));
    }

    @Override
    public int getTechIntroYear() {
        if (getCampaignOptions().get(CampaignOption.LIMIT_BY_YEAR)) {
            return getGameYear();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public List<Integer> getTechAvailabilityYears() {
        // Availability checks (introduction, extinction, and era-based tech level) are evaluated at the tech intro
        // year cutoff rather than the raw game year. This ensures that disabling "Limit Tech Purchases by Game Year"
        // - which makes getTechIntroYear() return Integer.MAX_VALUE - also lifts the era-based tech-level restriction,
        // so designs introduced after the current campaign year remain purchasable.
        return List.of(getTechIntroYear());
    }

    @Override
    public int getGameYear() {
        return getLocalDate().getYear();
    }

    @Override
    public boolean useClanTechBase() {
        return getPlayerForce().getFaction().isClan();
    }

    @Override
    public boolean useMixedTech() {
        if (useClanTechBase()) {
            return campaignOptions.get(CampaignOption.ALLOW_IS_PURCHASES);
        } else {
            return campaignOptions.get(CampaignOption.ALLOW_CLAN_PURCHASES);
        }
    }

    @Override
    public SimpleTechLevel getTechLevel() {
        for (SimpleTechLevel lvl : SimpleTechLevel.values()) {
            if (campaignOptions.get(CampaignOption.TECH_LEVEL) == lvl.ordinal()) {
                return lvl;
            }
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    @Override
    public boolean unofficialNoYear() {
        return false;
    }

    @Override
    public boolean useVariableTechLevel() {
        return campaignOptions.get(CampaignOption.VARIABLE_TECH_LEVEL);
    }

    @Override
    public boolean showExtinct() {
        return !campaignOptions.get(CampaignOption.DISALLOW_EXTINCT_STUFF);
    }

    public BehaviorSettings getAutoResolveBehaviorSettings() {
        return autoResolveBehaviorSettings;
    }

    public void setAutoResolveBehaviorSettings(BehaviorSettings settings) {
        autoResolveBehaviorSettings = settings;
    }

    /**
     * Retrieves the address or form of address for the commander.
     *
     * <p>This method determines the appropriate address based on whether the campaign is considered a pirate campaign.
     * It delegates to {@link #getCommanderAddress(boolean)} with the result of {@code isPirateCampaign()}.</p>
     *
     * @return the string used to address the commander
     */
    public String getCommanderAddress() {
        return getCommanderAddress(isPirateCampaign());
    }

    /**
     * Retrieves the address or title for the commanding officer, either in a formal or informal format.
     *
     * <p>
     * This method checks for the presence of a flagged commander. If no commander is found, a general fallback address
     * is returned based on the specified formality. If a commander is present, it further tailors the address based on
     * the gender of the commander (for informal styles) or their rank and surname (for formal styles).
     * </p>
     *
     * @param isInformal A boolean flag indicating whether the address should be informal (true for informal, false for
     *                   formal).
     *
     * @return A {@link String} representing the appropriate address for the commander, either formal or informal.
     */
    public String getCommanderAddress(boolean isInformal) {
        Person commander = getPlayerForce().getHumanResources()
                                 .getCommander(getCampaignOptions(), getPlayerForce().isClanForce(), getLocalDate());

        if (commander == null) {
            if (isInformal) {
                return resources.getString("generalFallbackAddressInformal.text");
            } else {
                return resources.getString("generalFallbackAddress.text");
            }
        }

        if (isInformal) {
            Gender commanderGender = commander.getGender();

            return switch (commanderGender) {
                case MALE -> resources.getString("informalAddressMale.text");
                case FEMALE -> resources.getString("informalAddressFemale.text");
                case OTHER_MALE, OTHER_FEMALE, RANDOMIZE -> resources.getString("generalFallbackAddressInformal.text");
            };
        }

        String commanderRank = commander.getRankName();

        if (commanderRank.equalsIgnoreCase("None") || commanderRank.equalsIgnoreCase("-") || commanderRank.isBlank()) {
            return resources.getString("generalFallbackAddress.text");
        }

        return commanderRank;
    }

    public boolean isProcessProcurement() {
        return processProcurement;
    }

    public void setProcessProcurement(boolean processProcurement) {
        this.processProcurement = processProcurement;
    }

    // Simple getters and setters for our stock map
    public Map<String, Double> getPartsInUseRequestedStockMap() {
        return getPlayerForce().getRequestedStockLevels().getStockMap();
    }

    public void setPartsInUseRequestedStockMap(Map<String, Double> partsInUseRequestedStockMap) {
        Map<String, Double> stockMap = getPlayerForce().getRequestedStockLevels().getStockMap();
        stockMap.clear();
        stockMap.putAll(partsInUseRequestedStockMap);
    }

    public void writePartInUseToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreMothBalled", getPlayerForce().getIgnoreMothballed());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "topUpWeekly", getPlayerForce().getTopUpWeekly());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "ignoreSparesUnderQuality",
              getPlayerForce().getIgnoreSparesUnderQuality()
                    .name());
        getPlayerForce().getRequestedStockLevels().writeToXML(pw, indent);
    }

    /**
     * Wipes the Parts in use map for the purpose of resetting all values to their default
     */
    public void wipePartsInUseMap() {
        getPlayerForce().getRequestedStockLevels().clear();
    }

    /**
     * Legacy discriminator for the main force in older saves. The referable main-force node is now the
     * {@link mekhq.campaign.force.PlayerForce}; this constant is retained only as a read alias in
     * {@link ILocation#REFERENCE_RESOLVERS} so pre-split saves still resolve. The campaign itself is no longer written
     * as a location reference, so it inherits {@link ILocation#locationReferenceType()}'s {@code null} default (not
     * referable).
     */
    public static final String LOCATION_REFERENCE_TYPE = "campaign";

    /**
     * Retrieves the campaign faction icon for the specified {@link Campaign}. If a custom icon is defined in the
     * campaign's unit icon configuration, that icon is used. Otherwise, a default faction logo is fetched based on the
     * campaign's faction short name.
     *
     * @return An {@link ImageIcon} representing the faction icon for the given campaign.
     */
    public ImageIcon getCampaignFactionIcon() {
        ImageIcon icon;
        StandardFormationIcon campaignIcon = getPlayerForce().getUnitIcon();

        if (campaignIcon.getFilename() == null) {
            icon = getFactionLogo(currentDay.getYear(), getPlayerForce().getFaction().getShortName());
        } else {
            icon = campaignIcon.getImageIcon();
        }
        return icon;
    }

    /**
     * Checks if another active scenario has this scenarioID as it's linkedScenarioID and returns true if it finds one.
     */
    public boolean checkLinkedScenario(int scenarioID) {
        for (Scenario scenario : getScenarios()) {
            if ((scenario.getLinkedScenario() == scenarioID) &&
                      (getScenario(scenario.getId()).getStatus().isCurrent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of entities (units) from all combat formations.
     *
     * @return a list of entities representing all combat units in the player formation
     */
    public List<Entity> getAllCombatEntities() {
        List<Entity> units = new ArrayList<>();
        for (CombatTeam combatTeam : getPlayerForce().getCombatTeamsAsList(this)) {
            int id1 = combatTeam.getFormationId();
            Formation formation = getPlayerForce().getFormation(id1);
            if (formation != null) {
                for (Unit unit : formation.getAllUnitsAsUnits(getPlayerForce().getHangar(), true)) {
                    Entity entity = unit.getEntity();
                    if (entity != null) {
                        units.add(entity);
                    }
                }
            }
        }
        return units;
    }

    /**
     * Determines the appropriate starting planet for a new campaign based on campaign type, faction, and the player's
     * starting-location choice made in the campaign options.
     *
     * <p>For mercenary and pirate campaigns the choice's {@link StartingLocationMode} (mercenary capital, random,
     * random Great House, random Periphery, or a specific faction) is resolved by
     * {@link #getMercenaryOrPirateStartingPlanet(Factions, StartingLocationChoice)}.</p>
     *
     * <p>All other campaigns begin with their own faction. The player only chooses whether to start on that faction's
     * capital or on a random hiring hall in its territory. If no valid system can be found, the logic falls back to a
     * default faction's starting planet and, ultimately, to the planet Terra as a default universal location.</p>
     *
     * <p>The method also includes special handling for Clan campaigns: if the fallback logic would result in the
     * campaign starting on Terra but the campaign is clan-based, it attempts to relocate the starting planet to Strana
     * Mechty.</p>
     *
     * @param choice the player's starting-location choice from the campaign options
     *
     * @return the {@link Planet} instance where the campaign should start
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Planet getNewCampaignStartingPlanet(StartingLocationChoice choice) {
        Factions factions = Factions.getInstance();

        final String CLAN_CODE = "CLAN";

        if (isMercenaryCampaign() || isPirateCampaign()) {
            return getMercenaryOrPirateStartingPlanet(factions, choice);
        }

        // All other campaigns begin with their own faction; the player only chooses capital vs. hiring hall
        boolean useFactionCapital = choice.useFactionCapital();

        Faction startingFaction = getPlayerForce().getFaction();
        Planet startingPlanet = resolveStartingPlanetForFaction(startingFaction, useFactionCapital);

        // Fallback if the faction has no usable starting location
        if (startingPlanet == null) {
            startingFaction = factions.getDefaultFaction();
            startingPlanet = resolveStartingPlanetForFaction(startingFaction, useFactionCapital);
            if (startingPlanet == null) {
                PlanetarySystem terra = systemsInstance.getSystemById(TERRA_ID);
                startingPlanet = (terra != null) ? terra.getPrimaryPlanet() : null;
            }
        }

        // Special case: a Clan campaign that would start on Terra swaps to the Clan homeworld
        if ((startingPlanet != null)
                  && getPlayerForce().isClanForce()
                  && TERRA_ID.equals(startingPlanet.getParentSystem().getId())) {
            Faction clanFaction = factions.getFaction(CLAN_CODE);
            if (clanFaction != null) {
                PlanetarySystem clanSystem = clanFaction.getStartingPlanet(this, currentDay);
                if (clanSystem != null) {
                    startingPlanet = clanSystem.getPrimaryPlanet();
                }
            }
        }

        return startingPlanet;
    }

    /**
     * Selects a starting planet for mercenary or pirate campaigns based on the player's
     * {@link StartingLocationChoice}.
     *
     * <p>The mercenary faction (or, for pirates, the Tortuga Dominions, falling back to the configured default
     * faction if they are not active at the campaign's start date) is used both as the "mercenary capital" option and
     * as the ultimate fallback if a more specific choice cannot be resolved.</p>
     *
     * <p>Once a faction is chosen — the fallback, a random faction from a mode-specific pool, or a specific faction
     * picked by the player — the campaign starts either on that faction's capital or on a random hiring hall in its
     * territory, according to the choice. If no world can be resolved, the logic falls back to Terra.</p>
     *
     * @param factions The {@link Factions} manager supplying access to all faction data.
     * @param choice   the player's starting-location choice
     *
     * @return the {@link Planet} used as the campaign start location.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Planet getMercenaryOrPirateStartingPlanet(Factions factions, StartingLocationChoice choice) {
        Faction startingFaction = resolveMercenaryStartingFaction(factions, choice);

        Planet startingPlanet = resolveStartingPlanetForFaction(startingFaction, choice.useFactionCapital());
        if (startingPlanet != null) {
            return startingPlanet;
        }

        // Fallback if no starting planet could be resolved
        PlanetarySystem terra = systemsInstance.getSystemById(TERRA_ID);
        return (terra != null) ? terra.getPrimaryPlanet() : null;
    }

    /**
     * Resolves which faction a mercenary or pirate campaign should attach to, based on the chosen
     * {@link StartingLocationMode}.
     *
     * @param factions the {@link Factions} manager supplying access to all faction data
     * @param choice   the player's starting-location choice
     *
     * @return the faction whose territory the campaign will start in
     *
     * @since 0.51.0
     */
    private Faction resolveMercenaryStartingFaction(Factions factions, StartingLocationChoice choice) {
        // The mercenary capital (or pirate haven) doubles as the fallback faction
        Faction fallbackFaction = isMercenaryCampaign()
                                        ? factions.getFaction(MERCENARY_FACTION_CODE)
                                        : factions.getFaction(TORTUGA_DOMINIONS_FACTION_CODE);
        if (isPirateCampaign() && !fallbackFaction.validIn(currentDay)) {
            fallbackFaction = factions.getDefaultFaction();
        }

        switch (choice.mode()) {
            case MERCENARY_CAPITAL -> {
                return fallbackFaction;
            }
            case SPECIFIC_FACTION -> {
                Faction specificFaction = choice.specificFaction();
                return (specificFaction != null) ? specificFaction : fallbackFaction;
            }
            default -> {
                List<Faction> pool = buildStartingFactionPool(
                      factions, choice.mode(), choice.includeDeepPeriphery());
                Faction randomFaction = ObjectUtility.getRandomItem(pool);
                return (randomFaction != null) ? randomFaction : fallbackFaction;
            }
        }
    }

    /**
     * Builds the pool of factions eligible for a random mercenary or pirate start, filtered by the chosen mode.
     *
     * <p>All candidate factions must be active on the campaign's start date, playable, and non-Clan. The
     * {@link StartingLocationMode} then narrows the pool further:</p>
     *
     * <ul>
     *   <li>{@link StartingLocationMode#RANDOM_GREAT_HOUSE} keeps Inner Sphere major and super powers, excluding
     *       mercenary and pirate factions.</li>
     *   <li>{@link StartingLocationMode#RANDOM_PERIPHERY} keeps Periphery factions, including the Deep Periphery only
     *       when {@code includeDeepPeriphery} is {@code true}.</li>
     *   <li>Any other mode keeps all eligible non-Deep-Periphery factions; for pirate campaigns, Periphery factions are
     *       added multiple times to weight the draw towards the Periphery.</li>
     * </ul>
     *
     * @param factions             the {@link Factions} manager supplying access to all faction data
     * @param mode                 the chosen starting-location mode
     * @param includeDeepPeriphery whether Deep Periphery factions are eligible for a random Periphery start
     *
     * @return the list of eligible factions (which may contain duplicates for weighting purposes)
     *
     * @since 0.51.0
     */
    private List<Faction> buildStartingFactionPool(Factions factions, StartingLocationMode mode,
          boolean includeDeepPeriphery) {
        List<Faction> pool = new ArrayList<>();

        for (Faction possibleFaction : factions.getActiveFactions(currentDay)) {
            if (!possibleFaction.isPlayable() || possibleFaction.isClan()) {
                continue;
            }

            switch (mode) {
                case RANDOM_GREAT_HOUSE -> {
                    if (possibleFaction.isISMajorOrSuperPower()
                              && !possibleFaction.isMercenary()
                              && !possibleFaction.isPirate()) {
                        pool.add(possibleFaction);
                    }
                }
                case RANDOM_PERIPHERY -> {
                    boolean isPeripheryFaction = possibleFaction.isPeriphery() || possibleFaction.isDeepPeriphery();
                    if (isPeripheryFaction && (includeDeepPeriphery || !possibleFaction.isDeepPeriphery())) {
                        pool.add(possibleFaction);
                    }
                }
                default -> {
                    if (!possibleFaction.isDeepPeriphery()) {
                        pool.add(possibleFaction);

                        // For pirate campaigns, triple the chance of a Periphery start
                        if (possibleFaction.isPeriphery() && isPirateCampaign()) {
                            pool.add(possibleFaction);
                            pool.add(possibleFaction);
                        }
                    }
                }
            }
        }

        return pool;
    }

    /**
     * Resolves the actual starting planet for a chosen faction, honouring the player's capital-vs-hiring-hall
     * preference.
     *
     * <p>When {@code useFactionCapital} is {@code false}, a random hiring hall in the faction's territory is used; if
     * the faction has no reachable hiring hall, the logic falls back to the faction's capital.</p>
     *
     * @param startingFaction   the faction whose starting world is being resolved
     * @param useFactionCapital {@code true} to use the faction capital, {@code false} to prefer a random hiring hall
     *
     * @return the resolved {@link Planet}, or {@code null} if the faction has no usable starting system
     *
     * @since 0.51.0
     */
    private Planet resolveStartingPlanetForFaction(Faction startingFaction, boolean useFactionCapital) {
        if (!useFactionCapital) {
            Planet hiringHallPlanet = getRandomHiringHallStartingPlanet(startingFaction);
            if (hiringHallPlanet != null) {
                return hiringHallPlanet;
            }
            // Fall through to the capital when the faction has no reachable hiring hall
        }

        PlanetarySystem capitalSystem = startingFaction.getStartingPlanet(this, currentDay);
        return (capitalSystem != null) ? capitalSystem.getPrimaryPlanet() : null;
    }

    /**
     * Picks a random hiring-hall world for a starting faction.
     *
     * <p>Factions that hold territory use a hiring hall within their own borders. Mercenary and pirate factions hold
     * no territory, so any Inner Sphere hiring hall is eligible for them.</p>
     *
     * @param startingFaction the faction whose territory is searched for hiring halls
     *
     * @return a random hiring-hall {@link Planet}, or {@code null} if none is available
     *
     * @since 0.51.0
     */
    private Planet getRandomHiringHallStartingPlanet(Faction startingFaction) {
        boolean holdsNoTerritory = startingFaction.isMercenary() || startingFaction.isPirate();

        List<PlanetarySystem> candidateSystems = new ArrayList<>();
        for (PlanetarySystem system : systemsInstance.getSystems().values()) {
            if (!system.isHiringHall(currentDay)) {
                continue;
            }

            if (holdsNoTerritory) {
                if (isInnerSphereSystem(system)) {
                    candidateSystems.add(system);
                }
            } else if (system.getFactionSet(currentDay).contains(startingFaction)) {
                candidateSystems.add(system);
            }
        }

        PlanetarySystem chosenSystem = ObjectUtility.getRandomItem(candidateSystems);
        return (chosenSystem != null) ? chosenSystem.getPrimaryPlanet() : null;
    }

    /**
     * Determines whether a system lies within the Inner Sphere for hiring-hall selection purposes.
     *
     * @param system the system to test
     *
     * @return {@code true} if the system has at least one owning faction that is neither a Clan nor a Deep Periphery
     *       power on the campaign's current date
     *
     * @since 0.51.0
     */
    private boolean isInnerSphereSystem(PlanetarySystem system) {
        for (Faction owner : system.getFactionSet(currentDay)) {
            if (!owner.isClan() && !owner.isDeepPeriphery()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the HPG network supplied by this campaign's systems data for the current campaign date.
     *
     * @return the campaign's current dated HPG links
     */
    public Collection<HPGLink> getHPGNetwork() {
        return systemsInstance.getHPGNetwork(currentDay);
    }
}
