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

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_MONTH;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_WEEK;
import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.campaign.force.Force.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.force.ForceType.STANDARD;
import static mekhq.campaign.market.contractMarket.ContractAutomation.performAutomatedActivation;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.mission.AtBContract.pickRandomCamouflage;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_A;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_INTERSTELLAR_NEGOTIATOR;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_LOGISTICIAN;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateImplants.giveEIImplant;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_MEDTECH;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC;
import static mekhq.campaign.personnel.skills.SkillType.getType;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;
import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MINIMUM_TEMPORARY_CAPACITY;
import static mekhq.campaign.unit.Unit.TECH_WORK_DAY;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.isIneligibleToPerformProcurement;

import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import megamek.Version;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.ObjectUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.Player;
import megamek.common.SimpleTechLevel;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Gender;
import megamek.common.enums.TechBase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
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
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Quartermaster.PartAcquisitionResult;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.camOpsReputation.IUnitRating;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsMarshaller;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.events.*;
import mekhq.campaign.events.loans.LoanNewEvent;
import mekhq.campaign.events.loans.LoanPaidEvent;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.events.missions.MissionRemovedEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.parts.PartWorkEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonNewEvent;
import mekhq.campaign.events.persons.PersonRemovedEvent;
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
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.log.HistoricalLogEntry;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.parts.AmmoStorage;
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
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.protomeks.ProtoMekArmor;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.death.RandomDeath;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.RandomPortraitGenerator;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.medical.MASHCapacity;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.skills.Appraisal;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.RandomEventLibraries;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.storyArc.StoryArc;
import mekhq.campaign.stratCon.StratConContractInitializer;
import mekhq.campaign.stratCon.StratConRulesManager;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.eras.Era;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumsLibrary;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.fameAndInfamy.FameAndInfamyController;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.RangedFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.RangedPlanetSelector;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.module.atb.AtBEventProcessor;
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
    private final Hangar units = new Hangar();
    CampaignTransporterMap shipTransporters = new CampaignTransporterMap(this, CampaignTransportType.SHIP_TRANSPORT);
    CampaignTransporterMap tacticalTransporters = new CampaignTransporterMap(this,
          CampaignTransportType.TACTICAL_TRANSPORT);
    CampaignTransporterMap towTransporters = new CampaignTransporterMap(this, CampaignTransportType.TOW_TRANSPORT);
    private final Map<UUID, Person> personnel = new LinkedHashMap<>();

    /**
     * This can easily be expanded for other personnel lists by providing a unique String as the map's key.
     */
    private transient Map<String, List<Person>> activePersonnelCache = new HashMap<>();

    private Warehouse parts = new Warehouse();
    private final TreeMap<Integer, Force> forceIds = new TreeMap<>();
    private final TreeMap<Integer, Mission> missions = new TreeMap<>();
    private final TreeMap<Integer, Scenario> scenarios = new TreeMap<>();
    private final Map<UUID, List<Kill>> kills = new HashMap<>();

    // This maps PartInUse ToString() results to doubles, representing a mapping
    // of parts in use to their requested stock percentages to make these values
    // persistent
    private Map<String, Double> partsInUseRequestedStockMap = new LinkedHashMap<>();

    private transient final UnitNameTracker unitNameTracker = new UnitNameTracker();

    private int asTechPool;
    private int asTechPoolMinutes;
    private int asTechPoolOvertime;
    private int medicPool;

    private int lastForceId;
    private int lastMissionId;
    private int lastScenarioId;

    // I need to put a basic game object in campaign so that I can
    // assign it to the entities, otherwise some entity methods may get NPE
    // if they try to call up game options
    private final Game game;
    private final Player player;

    private GameOptions gameOptions;

    private String name;
    private LocalDate currentDay;
    private LocalDate campaignStartDate;

    // hierarchically structured Force object to define TO&E
    private Force forces;
    private Hashtable<Integer, CombatTeam> combatTeams; // AtB

    private Faction faction;
    private megamek.common.enums.Faction techFaction;
    private String retainerEmployerCode; // AtB
    private LocalDate retainerStartDate; // AtB
    private RankSystem rankSystem;

    private final ArrayList<String> currentReport;
    private transient String currentReportHTML;
    private transient List<String> newReports;

    private final ArrayList<String> personnelReport;
    private transient String personnelReportHTML;
    private transient List<String> newPersonnelReports;

    private final ArrayList<String> skillReport;
    private transient String skillReportHTML;
    private transient List<String> newSkillReports;

    private final ArrayList<String> technicalReport;
    private transient String technicalReportHTML;
    private transient List<String> newTechnicalReports;

    private final ArrayList<String> financesReport;
    private transient String financesReportHTML;
    private transient List<String> newFinancesReports;

    private final ArrayList<String> acquisitionsReport;
    private transient String acquisitionsReportHTML;
    private transient List<String> newAcquisitionsReports;

    private final ArrayList<String> medicalReport;
    private transient String medicalReportHTML;
    private transient List<String> newMedicalReports;

    private final ArrayList<String> battleReport;
    private transient String battleReportHTML;
    private transient List<String> newBattleReports;

    private final ArrayList<String> politicsReport;
    private transient String politicsReportHTML;
    private transient List<String> newPoliticsReports;

    private boolean fieldKitchenWithinCapacity;
    private int mashTheatreCapacity;
    private int repairBaysRented;

    // this is updated and used per gaming session, it is enabled/disabled via the Campaign options we're re-using
    // the LogEntry class used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private Camouflage camouflage = pickRandomCamouflage(3025, "Root");
    private PlayerColour colour = PlayerColour.BLUE;
    private StandardForceIcon unitIcon = new UnitIcon(null, null);

    private Finances finances;

    private Systems systemsInstance;
    private CurrentLocation location;
    private boolean isAvoidingEmptySystems;
    private boolean isOverridingCommandCircuitRequirements;

    private final News news;

    private PartsStore partsStore;

    private final List<String> customs;

    private CampaignOptions campaignOptions;
    private RandomSkillPreferences randomSkillPreferences = new RandomSkillPreferences();
    private MekHQ app;

    private ShoppingList shoppingList;

    private NewPersonnelMarket newPersonnelMarket;

    @Deprecated(since = "0.50.06")
    private PersonnelMarket personnelMarket;

    private AbstractContractMarket contractMarket;
    private AbstractUnitMarket unitMarket;

    private RandomDeath randomDeath;
    private transient AbstractDivorce divorce;
    private transient AbstractMarriage marriage;
    private transient AbstractProcreation procreation;
    private List<Person> personnelWhoAdvancedInXP;

    private RetirementDefectionTracker retirementDefectionTracker;
    private final List<String> turnoverRetirementInformation;

    private AtBConfiguration atbConfig; // AtB
    private AtBEventProcessor atbEventProcessor; // AtB
    private LocalDate shipSearchStart; // AtB
    private int shipSearchType;
    private String shipSearchResult; // AtB
    private LocalDate shipSearchExpiration; // AtB
    private IUnitGenerator unitGenerator; // deprecated
    @Deprecated(since = "0.50.10", forRemoval = true)
    private IUnitRating unitRating; // deprecated
    private ReputationController reputation;
    private int crimeRating;
    private int crimePirateModifier;
    private LocalDate dateOfLastCrime;
    private FactionStandings factionStandings;
    private int initiativeBonus;
    private int initiativeMaxBonus;
    private CampaignSummary campaignSummary;
    private final Quartermaster quartermaster;
    private StoryArc storyArc;
    private BehaviorSettings autoResolveBehaviorSettings;
    private List<UUID> automatedMothballUnits;
    private int temporaryPrisonerCapacity;
    private boolean processProcurement;

    // options relating to parts in use and restock
    private boolean ignoreMothballed;
    private boolean topUpWeekly;
    private PartQuality ignoreSparesUnderQuality;

    // Libraries
    // We deliberately don't write this data to the save file as we want it rebuilt
    // every time the campaign loads. This ensures updates can be applied and there is no risk of
    // bugs being permanently locked into the campaign file.
    RandomEventLibraries randomEventLibraries;
    FactionStandingUltimatumsLibrary factionStandingUltimatumsLibrary;

    /**
     * A constant that provides the ISO-8601 definition of week-based fields.
     *
     * <p>This includes the first day of the week set to Monday and the minimal number of days in the first week of
     * the year set to 4.</p>
     */
    public static final WeekFields WEEK_FIELDS = WeekFields.ISO;

    /**
     * Represents the different types of administrative specializations. Each specialization corresponds to a distinct
     * administrative role within the organization.
     *
     * <p>
     * These specializations are used to determine administrative roles and responsibilities, such as by identifying the
     * most senior administrator for a given role.
     * </p>
     */
    public enum AdministratorSpecialization {
        COMMAND, LOGISTICS, TRANSPORT, HR
    }

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
          MekHQ.getMHQOptions().getLocale());

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
              campConf.getforce(),
              campConf.getfinances(),
              campConf.getRandomEvents(),
              campConf.getUltimatums(),
              campConf.getRetDefTracker(),
              campConf.getAutosave(),
              campConf.getBehaviorSettings(),
              campConf.getPersonnelMarket(),
              campConf.getAtBMonthlyContractMarket(),
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
          Systems systemsInstance, CurrentLocation startLocation, ReputationController reputationController,
          FactionStandings factionStandings, RankSystem rankSystem, Force force, Finances finances,
          RandomEventLibraries randomEvents, FactionStandingUltimatumsLibrary ultimatums,
          RetirementDefectionTracker retDefTracker, IAutosaveService autosave,
          BehaviorSettings behaviorSettings,
          PersonnelMarket persMarket, AbstractContractMarket atbMonthlyContractMarket,
          AbstractUnitMarket unitMarket,
          AbstractDivorce divorce, AbstractMarriage marriage,
          AbstractProcreation procreation) {

        // Essential state
        id = UUID.randomUUID();
        this.game = game;
        this.player = player;
        this.game.addPlayer(0, this.player);
        this.name = name;
        currentDay = date;
        campaignOptions = campaignOpts;
        this.gameOptions = gameOptions;
        game.setOptions(gameOptions);
        this.techFaction = techFaction;
        this.systemsInstance = systemsInstance;
        location = startLocation;
        reputation = reputationController;
        this.factionStandings = factionStandings;
        forces = force;
        forceIds.put(0, forces);
        this.finances = finances;
        randomEventLibraries = randomEvents;
        factionStandingUltimatumsLibrary = ultimatums;
        retirementDefectionTracker = retDefTracker;
        autosaveService = autosave;
        autoResolveBehaviorSettings = behaviorSettings;
        this.partsStore = partsStore;
        this.newPersonnelMarket = newPersonnelMarket;
        this.randomDeath = randomDeath;
        this.campaignSummary = campaignSummary;

        // Members that take `this` as an argument
        this.quartermaster = new Quartermaster(this);

        // Primary init, sets state from passed values
        setFaction(faction);
        setRankSystemDirect(rankSystem);
        setPersonnelMarket(persMarket);
        setContractMarket(atbMonthlyContractMarket);
        setUnitMarket(unitMarket);
        setDivorce(divorce);
        setMarriage(marriage);
        setProcreation(procreation);

        // Starting config / default values
        campaignStartDate = null;
        shoppingList = new ShoppingList();
        isAvoidingEmptySystems = true;
        isOverridingCommandCircuitRequirements = false;
        overtime = false;
        gmMode = false;
        retainerEmployerCode = null;
        retainerStartDate = null;
        crimeRating = 0;
        crimePirateModifier = 0;
        dateOfLastCrime = null;
        initiativeBonus = 0;
        initiativeMaxBonus = 1;
        combatTeams = new Hashtable<>();
        asTechPool = 0;
        medicPool = 0;
        customs = new ArrayList<>();
        personnelWhoAdvancedInXP = new ArrayList<>();
        turnoverRetirementInformation = new ArrayList<>();
        atbConfig = null;
        hasActiveContract = false;
        fieldKitchenWithinCapacity = false;
        mashTheatreCapacity = 0;
        repairBaysRented = 0;
        automatedMothballUnits = new ArrayList<>();
        temporaryPrisonerCapacity = DEFAULT_TEMPORARY_CAPACITY;
        processProcurement = true;
        topUpWeekly = false;
        ignoreMothballed = true;
        ignoreSparesUnderQuality = QUALITY_A;

        // Reports
        currentReport = new ArrayList<>();
        currentReportHTML = "";
        newReports = new ArrayList<>();

        personnelReport = new ArrayList<>();
        personnelReportHTML = "";
        newPersonnelReports = new ArrayList<>();

        skillReport = new ArrayList<>();
        skillReportHTML = "";
        newSkillReports = new ArrayList<>();

        technicalReport = new ArrayList<>();
        technicalReportHTML = "";
        newTechnicalReports = new ArrayList<>();

        financesReport = new ArrayList<>();
        financesReportHTML = "";
        newFinancesReports = new ArrayList<>();

        acquisitionsReport = new ArrayList<>();
        acquisitionsReportHTML = "";
        newAcquisitionsReports = new ArrayList<>();

        medicalReport = new ArrayList<>();
        medicalReportHTML = "";
        newMedicalReports = new ArrayList<>();

        battleReport = new ArrayList<>();
        battleReportHTML = "";
        newBattleReports = new ArrayList<>();

        politicsReport = new ArrayList<>();
        politicsReportHTML = "";
        newPoliticsReports = new ArrayList<>();

        // Secondary initialization from passed / derived values
        news = new News(getGameYear(), id.getLeastSignificantBits());
        resetAsTechMinutes();

        // These classes require a Campaign reference to operate/initialize
        currencyManager.setCampaign(this);
        this.partsStore.stock(this);
        this.newPersonnelMarket.setCampaign(this);
        this.randomDeath.setCampaign(this);
        this.campaignSummary.setCampaign(this);
    }

    public IAutosaveService getAutosaveService() {
        return autosaveService;
    }

    /**
     * @return the app
     */
    public MekHQ getApp() {
        return app;
    }

    /**
     * @param app the app to set
     */
    public void setApp(MekHQ app) {
        this.app = app;
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

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public Era getEra() {
        return Eras.getInstance().getEra(getLocalDate());
    }

    public String getTitle() {
        return getName() +
                     " (" +
                     getFaction().getFullName(getGameYear()) +
                     ')' +
                     " - " +
                     MekHQ.getMHQOptions().getLongDisplayFormattedDate(getLocalDate()) +
                     " (" +
                     getEra() +
                     ')';
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

    public PlanetarySystem getCurrentSystem() {
        return location.getCurrentSystem();
    }

    public boolean isAvoidingEmptySystems() {
        return isAvoidingEmptySystems;
    }

    public void setIsAvoidingEmptySystems(boolean isAvoidingEmptySystems) {
        this.isAvoidingEmptySystems = isAvoidingEmptySystems;
    }

    public boolean isOverridingCommandCircuitRequirements() {
        return isOverridingCommandCircuitRequirements;
    }

    public void setIsOverridingCommandCircuitRequirements(boolean isOverridingCommandCircuitRequirements) {
        this.isOverridingCommandCircuitRequirements = isOverridingCommandCircuitRequirements;
    }

    public boolean isUseCommandCircuitForContract(Contract contract) {
        if (contract instanceof AtBContract atBContract) {

            return FactionStandingUtilities.isUseCommandCircuit(
                  isOverridingCommandCircuitRequirements, gmMode,
                  campaignOptions.isUseFactionStandingCommandCircuitSafe(),
                  factionStandings, List.of(atBContract));
        } else {
            return false;
        }
    }

    public boolean isUseCommandCircuit() {
        return FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements(), isGM(),
              getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              getFactionStandings(), getFutureAtBContracts());
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

    public Money getFunds() {
        return finances.getBalance();
    }

    public void setForces(Force f) {
        forces = f;
    }

    public Force getForces() {
        return forces;
    }

    public List<Force> getAllForces() {
        return new ArrayList<>(forceIds.values());
    }

    public TreeMap<Integer, Force> getForceIds() {
        return forceIds;
    }

    /**
     * Retrieves all units in the Table of Organization and Equipment (TOE).
     *
     * <p>This method provides a list of unique identifiers for all units currently included in the force's TOE
     * structure.</p>
     *
     * @param standardForcesOnly if {@code true}, returns only units in {@link ForceType#STANDARD} forces; if
     *                           {@code false}, returns all units.
     *
     * @return a List of UUID objects representing all units in the TOE according to the specified filter
     *
     * @author Illiani
     * @since 0.50.05
     */
    public List<UUID> getAllUnitsInTheTOE(boolean standardForcesOnly) {
        return forces.getAllUnits(standardForcesOnly);
    }

    /**
     * Adds a {@link CombatTeam} to the {@code combatTeams} {@link Hashtable} using {@code forceId} as the key.
     *
     * @param combatTeam the {@link CombatTeam} to be added to the {@link Hashtable}
     */
    public void addCombatTeam(CombatTeam combatTeam) {
        combatTeams.put(combatTeam.getForceId(), combatTeam);
    }

    /**
     * Removes a {@link CombatTeam} from the {@code combatTeams} {@link Hashtable} using {@code forceId} as the key.
     *
     * @param forceId the key of the {@link CombatTeam} to be removed from the {@link Hashtable}
     */
    public void removeCombatTeam(final int forceId) {
        this.combatTeams.remove(forceId);
    }

    /**
     * Returns the {@link Hashtable} using the combatTeam's {@code forceId} as the key and containing all the
     * {@link CombatTeam} objects after removing the ineligible ones. Although sanitization might not be necessary, it
     * ensures that there is no need for {@code isEligible()} checks when fetching the {@link Hashtable}.
     *
     * @return the sanitized {@link Hashtable} of {@link CombatTeam} objects stored in the current campaign.
     */
    public Hashtable<Integer, CombatTeam> getCombatTeamsAsMap() {
        // Here we sanitize the list, ensuring ineligible formations have been removed
        // before
        // returning the hashtable. In theory, this shouldn't be necessary, however,
        // having this
        // sanitizing step should remove the need for isEligible() checks whenever we
        // fetch the
        // hashtable.
        for (Force force : getAllForces()) {
            int forceId = force.getId();
            if (combatTeams.containsKey(forceId)) {
                CombatTeam combatTeam = combatTeams.get(forceId);

                if (combatTeam.isEligible(this)) {
                    continue;
                }
            } else {
                CombatTeam combatTeam = new CombatTeam(forceId, this);

                if (combatTeam.isEligible(this)) {
                    combatTeams.put(forceId, combatTeam);
                    continue;
                }
            }

            combatTeams.remove(forceId);
        }

        return combatTeams;
    }

    /**
     * Returns an {@link ArrayList} of all {@link CombatTeam} objects in the {@code combatTeams} {@link Hashtable}.
     * Calls the {@code getCombatTeamsTable()} method to sanitize the {@link Hashtable} before conversion to
     * {@link ArrayList}.
     *
     * @return an {@link ArrayList} of all the {@link CombatTeam} objects in the {@code combatTeams} {@link Hashtable}
     */
    public ArrayList<CombatTeam> getCombatTeamsAsList() {
        // This call allows us to utilize the self-sanitizing feature of getCombatTeamsTable(), without needing to
        // directly include the code here, too.
        combatTeams = getCombatTeamsAsMap();

        return combatTeams.values()
                     .stream()
                     .filter(l -> forceIds.containsKey(l.getForceId()))
                     .collect(Collectors.toCollection(ArrayList::new));
    }

    public void setShoppingList(ShoppingList sl) {
        shoppingList = sl;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    // region Markets
    public PersonnelMarket getPersonnelMarket() {
        return personnelMarket;
    }

    public void setPersonnelMarket(final PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
    }

    public AbstractContractMarket getContractMarket() {
        return contractMarket;
    }

    public void setContractMarket(final AbstractContractMarket contractMarket) {
        this.contractMarket = contractMarket;
    }

    public AbstractUnitMarket getUnitMarket() {
        return unitMarket;
    }

    public void setUnitMarket(final AbstractUnitMarket unitMarket) {
        this.unitMarket = unitMarket;
    }

    public NewPersonnelMarket getNewPersonnelMarket() {
        return newPersonnelMarket;
    }

    public void setNewPersonnelMarket(final NewPersonnelMarket newPersonnelMarket) {
        this.newPersonnelMarket = newPersonnelMarket;
        this.newPersonnelMarket.setCampaign(this);
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

    public AbstractDivorce getDivorce() {
        return divorce;
    }

    public void setDivorce(final AbstractDivorce divorce) {
        this.divorce = divorce;
    }

    public AbstractMarriage getMarriage() {
        return marriage;
    }

    public void setMarriage(final AbstractMarriage marriage) {
        this.marriage = marriage;
    }

    public AbstractProcreation getProcreation() {
        return procreation;
    }

    public void setProcreation(final AbstractProcreation procreation) {
        this.procreation = procreation;
    }
    // endregion Personnel Modules

    public void setRetirementDefectionTracker(RetirementDefectionTracker rdt) {
        retirementDefectionTracker = rdt;
    }

    public RetirementDefectionTracker getRetirementDefectionTracker() {
        return retirementDefectionTracker;
    }

    /**
     * Sets the list of personnel who have advanced in experience points (XP) via vocational xp.
     *
     * @param personnelWhoAdvancedInXP a {@link List} of {@link Person} objects representing personnel who have gained
     *                                 XP.
     */
    public void setPersonnelWhoAdvancedInXP(List<Person> personnelWhoAdvancedInXP) {
        this.personnelWhoAdvancedInXP = personnelWhoAdvancedInXP;
    }

    /**
     * Retrieves the list of personnel who have advanced in experience points (XP) via vocational xp.
     *
     * @return a {@link List} of {@link Person} objects representing personnel who have gained XP.
     */
    public List<Person> getPersonnelWhoAdvancedInXP() {
        return personnelWhoAdvancedInXP;
    }

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

    public void setAtBEventProcessor(AtBEventProcessor processor) {
        atbEventProcessor = processor;
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

    // region Ship Search

    /**
     * Sets the date a ship search was started, or null if no search is in progress.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public void setShipSearchStart(@Nullable LocalDate shipSearchStart) {
        this.shipSearchStart = shipSearchStart;
    }

    /**
     * @return The date a ship search was started, or null if none is in progress.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public LocalDate getShipSearchStart() {
        return shipSearchStart;
    }

    /**
     * Sets the lookup name of the available ship, or null if none were found.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public void setShipSearchResult(@Nullable String result) {
        shipSearchResult = result;
    }

    /**
     * @return The lookup name of the available ship, or null if none is available
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public String getShipSearchResult() {
        return shipSearchResult;
    }

    /**
     * @return The date the ship is no longer available, if there is one.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public LocalDate getShipSearchExpiration() {
        return shipSearchExpiration;
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    public void setShipSearchExpiration(LocalDate shipSearchExpiration) {
        this.shipSearchExpiration = shipSearchExpiration;
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    public int getShipSearchType() {
        return shipSearchType;
    }

    /**
     * Sets the unit type to search for.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public void setShipSearchType(int unitType) {
        shipSearchType = unitType;
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    public void startShipSearch(int unitType) {
        setShipSearchStart(getLocalDate());
        setShipSearchType(unitType);
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    public void purchaseShipSearchResult() {
        MekSummary ms = MekSummaryCache.getInstance().getMek(getShipSearchResult());
        if (ms == null) {
            LOGGER.error("Cannot find entry for {}", getShipSearchResult());
            return;
        }

        Money cost = Money.of(ms.getCost());

        if (getFunds().isLessThan(cost)) {
            addReport(FINANCES, "<font color='" +
                                      ReportingUtilities.getNegativeColor() +
                                      "'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
            return;
        }

        MekFileParser mekFileParser;

        try {
            mekFileParser = new MekFileParser(ms.getSourceFile(), ms.getEntryName());
        } catch (Exception ex) {
            LOGGER.error("Unable to load unit: {}", ms.getEntryName(), ex);
            return;
        }

        Entity en = mekFileParser.getEntity();

        int transitDays = getCampaignOptions().isInstantUnitMarketDelivery() ?
                                0 :
                                calculatePartTransitTime(en.calcYearAvailability(getGameYear(),
                                      useClanTechBase(),
                                      getTechFaction()));

        getFinances().debit(TransactionType.UNIT_PURCHASE, getLocalDate(), cost, "Purchased " + en.getShortName());
        PartQuality quality = PartQuality.QUALITY_D;

        if (campaignOptions.isUseRandomUnitQualities()) {
            quality = Unit.getRandomUnitQuality(0);
        }

        addNewUnit(en, true, transitDays, quality);

        if (!getCampaignOptions().isInstantUnitMarketDelivery()) {
            addReport(ACQUISITIONS, "<font color='" +
                                          ReportingUtilities.getPositiveColor() +
                                          "'>Unit will be delivered in " +
                                          transitDays +
                                          " days.</font>");
        }
        setShipSearchResult(null);
        setShipSearchExpiration(null);
    }
    // endregion Ship Search

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

        if ((totalPayout.isPositive()) || (null != getRetirementDefectionTracker().getRetirees())) {
            if (getFinances().debit(TransactionType.PAYOUT, getLocalDate(), totalPayout, "Final Payout")) {
                for (UUID pid : getRetirementDefectionTracker().getRetirees()) {
                    Person person = getPerson(pid);
                    boolean wasKilled = getRetirementDefectionTracker().getPayout(pid).isWasKilled();
                    boolean wasSacked = getRetirementDefectionTracker().getPayout(pid).isWasSacked();

                    if ((!wasKilled) && (!wasSacked)) {
                        if (!person.getPermanentInjuries().isEmpty()) {
                            person.changeStatus(this, getLocalDate(), PersonnelStatus.RETIRED);
                        }
                        if (isBreakingContract(person,
                              getLocalDate(),
                              getCampaignOptions().getServiceContractDuration())) {
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
                    if ((person.getAge(getLocalDate()) >= 50) && (!campaignOptions.getRandomDivorceMethod().isNone())) {
                        if ((spouse != null) && (spouse.isDivorceable()) && (!spouse.getPrimaryRole().isCivilian())) {
                            if ((person.getStatus().isDefected()) || (randomInt(6) == 0)) {
                                getDivorce().divorce(this, getLocalDate(), person, SplittingSurnameStyle.WEIGHTED);

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
                getRetirementDefectionTracker().resolveAllContracts();
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

    public void setCampaignSummary(CampaignSummary campaignSummary) {
        this.campaignSummary = campaignSummary;
        this.campaignSummary.setCampaign(this);
    }

    public News getNews() {
        return news;
    }

    /**
     * Add force to an existing superforce. This method will also assign the force an id and place it in the forceId
     * hash
     *
     * @param force      - the Force to add
     * @param superForce - the superforce to add the new force to
     */
    public void addForce(Force force, Force superForce) {
        int id = lastForceId + 1;
        force.setId(id);
        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId(), this);
        forceIds.put(id, force);
        lastForceId = id;

        force.updateCommander(this);

        if (campaignOptions.isUseAtB()) {
            recalculateCombatTeams(this);
        }
    }

    public void moveForce(Force force, Force superForce) {
        Force parentForce = force.getParentForce();

        if (null != parentForce) {
            parentForce.removeSubForce(force.getId());
        }

        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId(), this);

        ForceType forceType = force.getForceType();

        if (forceType.shouldStandardizeParents()) {
            for (Force individualParentForce : force.getAllParents()) {
                individualParentForce.setForceType(STANDARD, false);
            }
        }

        if (forceType.shouldChildrenInherit()) {
            for (Force childForce : force.getAllSubForces()) {
                childForce.setForceType(forceType, false);
            }
        }

        // repopulate formation levels across the TO&E
        Force.populateFormationLevelsFromOrigin(this);
    }

    /**
     * This is used by the XML loader. The id should already be set for this force so don't increment
     *
     * @param force Force to add
     */
    public void importForce(Force force) {
        lastForceId = max(lastForceId, force.getId());
        forceIds.put(force.getId(), force);
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

    public void addUnitToForce(final @Nullable Unit unit, final Force force) {
        addUnitToForce(unit, force.getId());
    }

    /**
     * Add unit to an existing force. This method will also assign that force's id to the unit.
     *
     * @param unit Unit to add to the existing force.
     * @param id   Force ID to add unit to
     */
    public void addUnitToForce(@Nullable Unit unit, int id) {
        if (unit == null) {
            return;
        }

        if (id == FORCE_NONE) {
            Force currentForce = getForce(unit.getForceId());
            unit.setForceId(FORCE_NONE);
            unit.setScenarioId(NO_ASSIGNED_SCENARIO);
            MekHQ.triggerEvent(new OrganizationChangedEvent(this, currentForce, unit));
            return;
        }

        Force force = forceIds.get(id);
        Force prevForce = forceIds.get(unit.getForceId());
        boolean useTransfers = false;
        boolean transferLog = !getCampaignOptions().isUseTransfers();

        if (null != prevForce) {
            if (null != prevForce.getTechID()) {
                unit.removeTech();
            }
            // We log removal if we don't use transfers or if it can't be assigned to a new
            // force
            prevForce.removeUnit(this, unit.getId(), transferLog || (force == null));
            useTransfers = !transferLog;
            MekHQ.triggerEvent(new OrganizationChangedEvent(this, prevForce, unit));
        }

        if (null != force) {
            unit.setForceId(id);
            unit.setScenarioId(force.getScenarioId());
            if (null != force.getTechID()) {
                Person forceTech = getPerson(force.getTechID());
                if (forceTech.canTech(unit.getEntity())) {
                    if (null != unit.getTech()) {
                        unit.removeTech();
                    }

                    unit.setTech(forceTech);
                } else {
                    String cantTech = forceTech.getFullName() +
                                            " cannot maintain " +
                                            unit.getName() +
                                            '\n' +
                                            "You will need to assign a tech manually.";
                    JOptionPane.showMessageDialog(null, cantTech, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
            force.addUnit(this, unit.getId(), useTransfers, prevForce);
            MekHQ.triggerEvent(new OrganizationChangedEvent(this, force, unit));
        }

        if (campaignOptions.isUseAtB()) {
            recalculateCombatTeams(this);
        }
    }

    /**
     * Adds force and all its sub-forces to the Combat Teams table
     */
    private void addAllCombatTeams(Force force) {
        recalculateCombatTeams(this);

        for (Force subForce : force.getSubForces()) {
            addAllCombatTeams(subForce);
        }
    }

    // region Missions/Contracts

    /**
     * Add a mission to the campaign
     *
     * @param mission The mission to be added
     */
    public void addMission(Mission mission) {
        int missionID = lastMissionId + 1;
        mission.setId(missionID);
        missions.put(missionID, mission);
        lastMissionId = missionID;
        MekHQ.triggerEvent(new MissionNewEvent(mission));
    }

    /**
     * Imports a {@link Mission} into a campaign.
     *
     * @param mission Mission to import into the campaign.
     */
    public void importMission(final Mission mission) {
        mission.getScenarios().forEach(this::importScenario);
        addMissionWithoutId(mission);
        StratConContractInitializer.restoreTransientStratconInformation(mission, this);
    }

    private void addMissionWithoutId(Mission m) {
        lastMissionId = max(lastMissionId, m.getId());
        missions.put(m.getId(), m);
        MekHQ.triggerEvent(new MissionNewEvent(m));
    }

    /**
     * @param id the mission's id
     *
     * @return the mission in question
     */
    public @Nullable Mission getMission(int id) {
        return missions.get(id);
    }

    /**
     * @return an <code>Collection</code> of missions in the campaign
     */
    public Collection<Mission> getMissions() {
        return missions.values();
    }

    /**
     * @return missions List sorted with complete missions at the bottom
     */
    public List<Mission> getSortedMissions() {
        return getMissions().stream()
                     .sorted(Comparator.comparing(Mission::getStatus)
                                   .thenComparing(m -> (m instanceof Contract) ?
                                                             ((Contract) m).getStartDate() :
                                                             LocalDate.now()))
                     .collect(Collectors.toList());
    }

    public List<Mission> getActiveMissions(final boolean excludeEndDateCheck) {
        return getMissions().stream()
                     .filter(m -> m.isActiveOn(getLocalDate(), excludeEndDateCheck))
                     .collect(Collectors.toList());
    }

    public List<Mission> getCompletedMissions() {
        return getMissions().stream().filter(m -> m.getStatus().isCompleted()).collect(Collectors.toList());
    }

    /**
     * Retrieves a list of currently active contracts.
     *
     * <p>This method is a shorthand for {@link #getActiveContracts(boolean)} with {@code includeFutureContracts}
     * set to {@code false}. It fetches all contracts from the list of missions and filters them for those that are
     * currently active on the current local date.</p>
     *
     * @return A list of {@link Contract} objects that are currently active.
     */
    public List<Contract> getActiveContracts() {
        return getActiveContracts(false);
    }

    /**
     * Retrieves a list of active contracts, with an option to include future contracts.
     *
     * <p>This method iterates through all missions and checks if they are instances of {@link Contract}.
     * If so, it filters them based on their active status, as determined by the
     * {@link Contract#isActiveOn(LocalDate, boolean)} method.</p>
     *
     * @param includeFutureContracts If {@code true}, contracts that are scheduled to start in the future will also be
     *                               included in the final result. If {@code false}, only contracts active on the
     *                               current local date are included.
     *
     * @return A list of {@link Contract} objects that match the active criteria.
     */
    public List<Contract> getActiveContracts(boolean includeFutureContracts) {
        List<Contract> activeContracts = new ArrayList<>();

        for (Mission mission : getMissions()) {
            // Skip if the mission is not a Contract
            if (!(mission instanceof Contract contract)) {
                continue;
            }

            if (contract.isActiveOn(getLocalDate(), includeFutureContracts)) {
                activeContracts.add(contract);
            }
        }

        return activeContracts;
    }

    /**
     * Retrieves a list of future contracts.
     *
     * <p>This method fetches all missions and checks if they are instances of {@link Contract}. It filters the
     * contracts where the start date is after the current day.</p>
     *
     * @return A list of {@link Contract} objects whose start dates are in the future.
     */
    public List<Contract> getFutureContracts() {
        List<Contract> activeContracts = new ArrayList<>();

        for (Mission mission : getMissions()) {
            // Skip if the mission is not a Contract
            if (!(mission instanceof Contract contract)) {
                continue;
            }

            if (contract.getStartDate().isAfter(currentDay)) {
                activeContracts.add(contract);
            }
        }

        return activeContracts;
    }

    public List<AtBContract> getAtBContracts() {
        return getMissions().stream()
                     .filter(c -> c instanceof AtBContract)
                     .map(c -> (AtBContract) c)
                     .collect(Collectors.toList());
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
     * @see #hasFutureAtBContract()
     */
    public boolean hasActiveAtBContract(boolean includeFutureContracts) {
        if (!getActiveAtBContracts().isEmpty()) {
            return true;
        }

        if (includeFutureContracts) {
            return hasFutureAtBContract();
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

    /**
     * Determines whether there are any future AtB (Against the Bot) contracts. A future contract is defined as a
     * contract that has been accepted but has a start date later than the current day.
     *
     * @return true if there is at least one future AtB contract (accepted but starting after the current date).
     *       Otherwise, false.
     */
    public boolean hasFutureAtBContract() {
        List<AtBContract> contracts = getAtBContracts();

        for (AtBContract contract : contracts) {
            // This catches any contracts that have been accepted, but haven't yet started
            if (contract.getStartDate().isAfter(currentDay)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves a list of {@link AtBContract} objects that have a start date after the current day.
     *
     * @return a list of future AtBContract objects whose start date is after the current day
     */
    public List<AtBContract> getFutureAtBContracts() {
        return getAtBContracts().stream()
                     .filter(c -> c.getStartDate().isAfter(currentDay))
                     .collect(Collectors.toList());
    }

    public List<AtBContract> getActiveAtBContracts() {
        return getActiveAtBContracts(false);
    }

    public List<AtBContract> getActiveAtBContracts(boolean excludeEndDateCheck) {
        return getMissions().stream()
                     .filter(c -> (c instanceof AtBContract) && c.isActiveOn(getLocalDate(), excludeEndDateCheck))
                     .map(c -> (AtBContract) c)
                     .collect(Collectors.toList());
    }

    public List<AtBContract> getCompletedAtBContracts() {
        return getMissions().stream()
                     .filter(c -> (c instanceof AtBContract) && c.getStatus().isCompleted())
                     .map(c -> (AtBContract) c)
                     .collect(Collectors.toList());
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
        hasActiveContract = getMissions().stream()
                                  .anyMatch(c -> (c instanceof Contract) && c.isActiveOn(getLocalDate()));
    }
    // endregion Missions/Contracts

    /**
     * Adds scenario to existing mission, generating a report.
     */
    public void addScenario(Scenario s, Mission m) {
        addScenario(s, m, false);
    }

    /**
     * Add scenario to an existing mission. This method will also assign the scenario an id, provided that it is a new
     * scenario. It then adds the scenario to the scenarioId hash.
     * <p>
     * Scenarios with previously set ids can be sent to this mission, allowing one to remove and then re-add scenarios
     * if needed. This functionality is used in the
     * <code>AtBScenarioFactory</code> class in method
     * <code>createScenariosForNewWeek</code> to
     * ensure that scenarios are generated properly.
     *
     * @param s              - the Scenario to add
     * @param m              - the mission to add the new scenario to
     * @param suppressReport - whether to suppress the campaign report
     */
    public void addScenario(Scenario s, Mission m, boolean suppressReport) {
        final boolean newScenario = s.getId() == Scenario.S_DEFAULT_ID;
        final int id = newScenario ? ++lastScenarioId : s.getId();
        s.setId(id);
        m.addScenario(s);
        scenarios.put(id, s);

        if (newScenario && !suppressReport) {
            addReport(BATTLE, MessageFormat.format(resources.getString("newAtBScenario.format"),
                  s.getHyperlinkedName(),
                  MekHQ.getMHQOptions().getDisplayFormattedDate(s.getDate())));
        }

        MekHQ.triggerEvent(new ScenarioNewEvent(s));
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

    public void setLocation(CurrentLocation l) {
        location = l;
    }

    /**
     * Relocates the campaign immediately to the specified {@link PlanetarySystem}, updating the current location and
     * firing any associated events or automated behaviors.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *     <li>Updates the campaign's {@link CurrentLocation} to the given planetary system.</li>
     *     <li>Triggers a {@link LocationChangedEvent} to notify listeners of the move.</li>
     *     <li>If there are no units in automated mothball mode, performs automated activation.</li>
     *     <li>If enabled by campaign options, checks for possible inoculation prompts related to the Random Diseases
     *     and Alternative Advanced Medical systems.</li>
     * </ul>
     *
     * @param planetarySystem the destination {@link PlanetarySystem} to move the campaign to
     */
    public void moveToPlanetarySystem(PlanetarySystem planetarySystem) {
        setLocation(new CurrentLocation(planetarySystem, 0.0));
        MekHQ.triggerEvent(new LocationChangedEvent(getLocation(), false));

        if (getAutomatedMothballUnits().isEmpty()) {
            performAutomatedActivation(this);
        }

        if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
            Inoculations.triggerInoculationPrompt(this, false);
        }
    }

    public CurrentLocation getLocation() {
        return location;
    }

    public boolean isOnContractAndPlanetside() {
        boolean isOnContract = !getActiveMissions(false).isEmpty();
        boolean isPlanetside = location.isOnPlanet();
        return isPlanetside && isOnContract;
    }

    public List<String> getTurnoverRetirementInformation() {
        return turnoverRetirementInformation;
    }

    public TransportCostCalculations getTransportCostCalculation(int crewExperienceLevel) {
        return new TransportCostCalculations(getHangar().getUnits(),
              getPersonnelFilteringOutDepartedAndAbsent(),
              getCargoStatistics(),
              getHangarStatistics(),
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

        getHangar().addUnit(unit);

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
        getHangar().addUnit(unit);

        // we decided we like the test unit so much we are going to keep it
        unit.getEntity().setOwner(player);
        unit.getEntity().setGame(game);
        unit.getEntity().setExternalIdAsString(unit.getId().toString());
        if (!unit.isSelfCrewed()) {
            unit.setMaintenanceMultiplier(getCampaignOptions().getDefaultMaintenanceTime());
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
     * Add a new unit to the campaign and set its quality.
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
        Unit unit = new Unit(en, this);
        if (!unit.isSelfCrewed()) {
            unit.setMaintenanceMultiplier(getCampaignOptions().getDefaultMaintenanceTime());
        }
        getHangar().addUnit(unit);

        // reset the game object
        en.setOwner(player);
        en.setGame(game);
        en.setExternalIdAsString(unit.getId().toString());

        // Added to avoid the 'default force bug' when calculating cargo
        removeUnitFromForce(unit);

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }

        unit.setDaysToArrival(days);

        if (days > 0) {
            unit.setMothballed(campaignOptions.isMothballUnitMarketDeliveries());
        }

        if (allowNewPilots) {
            Map<CrewType, Collection<Person>> newCrew = Utilities.genRandomCrewWithCombinedSkill(this,
                  unit,
                  getFaction().getShortName());
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

        checkDuplicateNamesDuringAdd(en);
        addReport(ACQUISITIONS, unit.getHyperlinkedName() + " has been added to the unit roster.");
        MekHQ.triggerEvent(new UnitNewEvent(unit));

        return unit;
    }

    /**
     * @return the current hangar containing the player's units.
     */
    public Hangar getHangar() {
        return units;
    }

    /**
     * Gets statistics related to units in the hangar.
     */
    public HangarStatistics getHangarStatistics() {
        return new HangarStatistics(getHangar());
    }

    /**
     * Gets statistics related to cargo in the hangar.
     */
    public CargoStatistics getCargoStatistics() {
        return new CargoStatistics(this);
    }

    public Collection<Unit> getUnits() {
        return getHangar().getUnits();
    }

    /**
     * Retrieves a collection of units that are not mothballed or being salvaged.
     *
     * @return a collection of active units
     */
    public Collection<Unit> getActiveUnits() {
        return getHangar().getUnits().stream().filter(unit -> !unit.isMothballed() && !unit.isSalvage()).toList();
    }

    public List<Entity> getEntities() {
        return getUnits().stream().map(Unit::getEntity).collect(Collectors.toList());
    }

    public Unit getUnit(UUID id) {
        return getHangar().getUnit(id);
    }

    // region Personnel
    // region Person Creation

    /**
     * Creates a new dependent with given gender. The origin faction and planet are set to null.
     *
     * @param gender The {@link Gender} of the new dependent.
     *
     * @return Return a {@link Person} object representing the new dependent.
     */
    public Person newDependent(Gender gender) {
        return newDependent(gender, null, null);
    }

    /**
     * Creates a new dependent with the given gender, origin faction, and origin planet.
     *
     * @param gender        The {@link Gender} of the new dependent.
     * @param originFaction The {@link Faction} that represents the origin faction for the new dependent. This can be
     *                      null, suggesting the faction will be chosen based on campaign options.
     * @param originPlanet  The {@link Planet} that represents the origin planet for the new dependent. This can be
     *                      null, suggesting the planet will be chosen based on campaign options.
     *
     * @return Return a {@link Person} object representing the new dependent.
     */
    public Person newDependent(Gender gender, @Nullable Faction originFaction, @Nullable Planet originPlanet) {
        PersonnelRole civilianProfession = PersonnelRole.MISCELLANEOUS_JOB;

        int dependentProfessionDieSize = campaignOptions.getDependentProfessionDieSize();
        if (dependentProfessionDieSize == 0 || randomInt(dependentProfessionDieSize) == 0) {
            civilianProfession = PersonnelRole.DEPENDENT;
        }

        int civilianProfessionDieSize = campaignOptions.getCivilianProfessionDieSize();
        if (civilianProfessionDieSize > 0) { // A value of 0 denotes that this system has been disabled
            if (randomInt(civilianProfessionDieSize) == 0) {
                List<PersonnelRole> civilianRoles = PersonnelRole.getCivilianRolesExceptNone();
                civilianProfession = ObjectUtility.getRandomItem(civilianRoles);
            }
        }

        // When a character is generated we include age checks to ensure they're old enough for the profession
        // chosen, so we don't need to include age-checks here.

        return newPerson(civilianProfession,
              PersonnelRole.NONE,
              new DefaultFactionSelector(getCampaignOptions().getRandomOriginOptions(), originFaction),
              new DefaultPlanetSelector(getCampaignOptions().getRandomOriginOptions(), originPlanet),
              gender);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param role The primary role
     *
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole role) {
        return newPerson(role, PersonnelRole.NONE);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param primaryRole   The primary role
     * @param secondaryRole A secondary role
     *
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole) {
        return newPerson(primaryRole, secondaryRole, getFactionSelector(), getPlanetSelector(), Gender.RANDOMIZE);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param primaryRole The primary role
     * @param factionCode The code for the faction this person is to be generated from
     * @param gender      The gender of the person to be generated, or a randomize it value
     *
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final String factionCode, final Gender gender) {
        return newPerson(primaryRole,
              PersonnelRole.NONE,
              new DefaultFactionSelector(getCampaignOptions().getRandomOriginOptions(),
                    (factionCode == null) ? null : Factions.getInstance().getFaction(factionCode)),
              getPlanetSelector(),
              gender);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param primaryRole     The primary role
     * @param secondaryRole   A secondary role
     * @param factionSelector The faction selector to use for the person.
     * @param planetSelector  The planet selector for the person.
     * @param gender          The gender of the person to be generated, or a randomize it value
     *
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
          final AbstractFactionSelector factionSelector, final AbstractPlanetSelector planetSelector,
          final Gender gender) {
        return newPerson(primaryRole, secondaryRole, getPersonnelGenerator(factionSelector, planetSelector), gender);
    }

    /**
     * Generate a new {@link Person} of the given role, using the supplied {@link AbstractPersonnelGenerator}
     *
     * @param primaryRole        The primary role of the {@link Person}.
     * @param personnelGenerator The {@link AbstractPersonnelGenerator} to use when creating the {@link Person}.
     *
     * @return A new {@link Person} configured using {@code personnelGenerator}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final AbstractPersonnelGenerator personnelGenerator) {
        return newPerson(primaryRole, PersonnelRole.NONE, personnelGenerator, Gender.RANDOMIZE);
    }

    /**
     * Generate a new {@link Person} of the given role, using the supplied {@link AbstractPersonnelGenerator}
     *
     * @param primaryRole        The primary role of the {@link Person}.
     * @param secondaryRole      The secondary role of the {@link Person}.
     * @param personnelGenerator The {@link AbstractPersonnelGenerator} to use when creating the {@link Person}.
     * @param gender             The gender of the person to be generated, or a randomize it value
     *
     * @return A new {@link Person} configured using {@code personnelGenerator}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
          final AbstractPersonnelGenerator personnelGenerator, final Gender gender) {
        final Person person = personnelGenerator.generate(this, primaryRole, secondaryRole, gender);

        // Assign a random portrait after we generate a new person
        if (getCampaignOptions().isUsePortraitForRole(primaryRole)) {
            assignRandomPortraitFor(person);
        }

        // Handle EI Implant distribution for ProtoMek Pilots & Clan MekWarriors
        if (getCampaignOptions().isUseImplants() && getCampaignOptions().isUseAlternativeAdvancedMedical()) {
            if (primaryRole.isProtoMekPilot() || secondaryRole.isProtoMekPilot()) {
                giveEIImplant(this, person);
            } else if (primaryRole.isMekWarrior() && person.isClanPersonnel()) {
                boolean isOver40 = person.getAge(currentDay) >= 40;
                boolean isOver30 = person.getAge(currentDay) >= 30;

                int implantChance = 100;
                if (isOver40) {
                    implantChance = 50;
                } else if (isOver30) {
                    implantChance = 75;
                }

                if (randomInt(implantChance) == 0) {
                    giveEIImplant(this, person);
                }
            }
        }

        return person;
    }

    public boolean getFieldKitchenWithinCapacity() {
        return fieldKitchenWithinCapacity;
    }

    public void setFieldKitchenWithinCapacity(boolean fieldKitchenWithinCapacity) {
        this.fieldKitchenWithinCapacity = fieldKitchenWithinCapacity;
    }

    public boolean getMashTheatresWithinCapacity() {
        return !isOnContractAndPlanetside() || calculateMASHTheaterCapacity() >= getPatientsAssignedToDoctors().size();
    }

    public int calculateMASHTheaterCapacity() {
        List<Unit> unitsInTOE = getForce(FORCE_ORIGIN).getAllUnitsAsUnits(units, false);
        int baseCapacity = MASHCapacity.checkMASHCapacity(unitsInTOE, campaignOptions.getMASHTheatreCapacity());
        int rentedCapacity = FacilityRentals.getCapacityIncreaseFromRentals(getActiveContracts(),
              ContractRentalType.HOSPITAL_BEDS);
        return baseCapacity + rentedCapacity;
    }

    public int getCachedMashTheaterCapacity() {
        return mashTheatreCapacity;
    }

    public void setMashTheatreCapacity(int mashTheatreCapacity) {
        this.mashTheatreCapacity = mashTheatreCapacity;
    }

    public int getRepairBaysRented() {
        return repairBaysRented;
    }

    public void setRepairBaysRented(int repairBaysRented) {
        this.repairBaysRented = repairBaysRented;
    }

    public void changeRepairBaysRented(int delta) {
        repairBaysRented = max(0, repairBaysRented + delta);
    }
    // endregion Person Creation

    // region Personnel Recruitment

    /**
     * Recruits a person into the campaign roster using their current prisoner status, assuming recruitment is not
     * performed by a game master that recruitment actions should be logged, and the character should be employed.
     *
     * @param person the person to recruit; must not be {@code null}
     *
     * @return {@code true} if recruitment was successful and the person was added or employed; {@code false} otherwise
     *
     * @see #recruitPerson(Person, PrisonerStatus, boolean, boolean, boolean, boolean)
     */
    public boolean recruitPerson(Person person) {
        return recruitPerson(person, person.getPrisonerStatus(), false, true, true, false);
    }

    /**
     * @deprecated use {@link #recruitPerson(Person, boolean, boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean recruitPerson(Person person, boolean gmAdd) {
        return recruitPerson(person, person.getPrisonerStatus(), gmAdd, true);
    }

    /**
     * Recruits a person into the campaign roster using their current prisoner status, allowing specification of both
     * game master and employment flags.
     * <p>
     * This is a convenience overload that enables logging and allows caller to choose whether the person is employed
     * upon recruitment.
     * </p>
     *
     * @param person the person to recruit; must not be {@code null}
     * @param gmAdd  if {@code true}, recruitment is performed by a game master (bypassing funds check)
     * @param employ if {@code true}, the person is marked as employed in the campaign
     *
     * @return {@code true} if recruitment was successful and personnel was added or employed; {@code false} otherwise
     *
     * @see #recruitPerson(Person, PrisonerStatus, boolean, boolean, boolean, boolean)
     */
    public boolean recruitPerson(Person person, boolean gmAdd, boolean employ) {
        return recruitPerson(person, person.getPrisonerStatus(), gmAdd, true, employ, false);
    }

    /**
     * @deprecated use {@link #recruitPerson(Person, PrisonerStatus, boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean recruitPerson(Person person, PrisonerStatus prisonerStatus) {
        return recruitPerson(person, prisonerStatus, false, true);
    }

    /**
     * Recruits a person into the campaign roster with default parameters for game master and logging options.
     * <p>
     * This is a convenience overload that assumes recruitment is not performed by a game master and that recruitment
     * actions should be logged. If successful, the person is marked as employed based on the given flag.
     * </p>
     *
     * @param person         the person to recruit; must not be {@code null}
     * @param prisonerStatus the prison status to assign to the person
     * @param employ         if {@code true}, the person is marked as employed in the campaign
     *
     * @return {@code true} if recruitment was successful and personnel was added or employed; {@code false} otherwise
     *
     * @see #recruitPerson(Person, PrisonerStatus, boolean, boolean, boolean, boolean)
     */
    public boolean recruitPerson(Person person, PrisonerStatus prisonerStatus, boolean employ) {
        return recruitPerson(person, prisonerStatus, false, true, employ, false);
    }

    /**
     * Attempts to recruit a given person into the campaign with the specified prisoner status.
     *
     * <p>This is a convenience method that calls
     * {@link #recruitPerson(Person, PrisonerStatus, boolean, boolean, boolean, boolean)} with
     * {@code bypassSimulateRelationships} set to {@code false}.</p>
     *
     * @param person         the {@link Person} to recruit
     * @param prisonerStatus the {@link PrisonerStatus} applied to the recruited person
     * @param gmAdd          if {@code true}, the person is added in GM Mode
     * @param log            if {@code true}, the recruitment is logged
     * @param employ         if {@code true}, the person is immediately employed
     *
     * @return {@code true} if the person was successfully recruited; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean recruitPerson(Person person, PrisonerStatus prisonerStatus, boolean gmAdd, boolean log,
          boolean employ) {
        return recruitPerson(person, prisonerStatus, gmAdd, log, employ, false);
    }

    /**
     * @deprecated use {@link #recruitPerson(Person, PrisonerStatus, boolean, boolean, boolean, boolean)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean recruitPerson(Person person, PrisonerStatus prisonerStatus, boolean gmAdd, boolean log) {
        return recruitPerson(person, prisonerStatus, gmAdd, log, true, false);
    }

    /**
     * Recruits a person into the campaign roster, handling employment status, prisoner status, finances, logging, and
     * optional relationship simulation.
     *
     * <p>If the {@code employ} parameter is {@code true} and the person is not already employed, this method
     * optionally deducts recruitment costs from campaign finances (unless performed by a game master). The person's
     * status and campaign logs are updated accordingly.</p>
     *
     * <p>If the person is a new recruit, their joining date and personnel entry are initialized, and relationship
     * history may be simulated based on campaign options and role.</p>
     *
     * <p>The method also manages staff role-specific timing pools and can log recruitment events.</p>
     *
     * @param person                      the person to recruit; must not be {@code null}
     * @param prisonerStatus              the prison status to assign to the person
     * @param gmAdd                       if {@code true}, indicates the recruitment is being performed by a game master
     *                                    (bypassing funds check)
     * @param log                         if {@code true}, a record of the recruitment will be added to campaign logs
     * @param employ                      if {@code true}, the person is marked as employed in the campaign
     * @param bypassSimulateRelationships if {@code true}, relationship simulation does not occur
     *
     * @return {@code true} if recruitment was successful and personnel was added or employed; {@code false} on failure
     *       or insufficient funds
     */
    public boolean recruitPerson(Person person, PrisonerStatus prisonerStatus, boolean gmAdd, boolean log,
          boolean employ, boolean bypassSimulateRelationships) {
        if (person == null) {
            LOGGER.warn("A null person was passed into recruitPerson.");
            return false;
        }

        if (employ && !person.isEmployed()) {
            if (getCampaignOptions().isPayForRecruitment() && !gmAdd) {
                if (!getFinances().debit(TransactionType.RECRUITMENT,
                      getLocalDate(),
                      person.getSalary(this).multipliedBy(2),
                      String.format(resources.getString("personnelRecruitmentFinancesReason.text"),
                            person.getFullName()))) {
                    addReport(FINANCES, String.format(resources.getString("personnelRecruitmentInsufficientFunds.text"),
                          ReportingUtilities.getNegativeColor(),
                          person.getFullName()));
                    return false;
                }
            }
        }

        String formerSurname = person.getSurname();

        if (!personnel.containsValue(person)) {
            person.setJoinedCampaign(currentDay);
            personnel.put(person.getId(), person);

            if (!bypassSimulateRelationships && getCampaignOptions().isUseSimulatedRelationships()) {
                if ((prisonerStatus.isFree()) &&
                          (!person.getOriginFaction().isClan()) &&
                          // We don't simulate for civilians, otherwise MekHQ will try to simulate the entire
                          // relationship history of everyone the recruit has ever married or birthed. This will
                          // cause a StackOverflow. -- Illiani, May/21/2025
                          (!person.getPrimaryRole().isCivilian())) {
                    simulateRelationshipHistory(person);
                }
            }
        }

        if (employ) {
            if (person.isAstech()) {
                asTechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
                asTechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
            }
        } else {
            person.setStatus(PersonnelStatus.CAMP_FOLLOWER);
        }

        person.setPrisonerStatus(this, prisonerStatus, log);

        if (log) {
            formerSurname = person.getSurname().equals(formerSurname) ?
                                  "" :
                                  ' ' +
                                        String.format(resources.getString("personnelRecruitmentFormerSurname.text") +
                                                            ' ', formerSurname);
            String add = !prisonerStatus.isFree() ?
                               (' ' +
                                      resources.getString(prisonerStatus.isBondsman() ?
                                                                "personnelRecruitmentBondsman.text" :
                                                                "personnelRecruitmentPrisoner.text")) :
                               "";
            addReport(PERSONNEL, String.format(resources.getString("personnelRecruitmentAddedToRoster.text"),
                  person.getHyperlinkedFullTitle(),
                  formerSurname,
                  add));
        }

        // Inoculations
        if (location.isOnPlanet()) {
            String planetId = location.getPlanet().getId();
            person.addPlanetaryInoculation(planetId);
        }

        String originPlanetId = person.getOriginPlanet().getId();
        person.addPlanetaryInoculation(originPlanetId);

        MekHQ.triggerEvent(new PersonNewEvent(person));
        return true;
    }

    /**
     * Employs the given camp follower and integrates them into the campaign.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Validates that the person is non-null (logging a warning and exiting otherwise).</li>
     *   <li>Changes the person's status to {@link PersonnelStatus#ACTIVE} effective on the current campaign day.</li>
     *   <li>Records the recruitment date.</li>
     *   <li>Increases the campaign's Astech support-time pools if the person has an Astech role (primary or
     *   secondary).</li>
     *   <li>Fires a {@link PersonNewEvent} to notify listeners about the new camp follower.</li>
     * </ul>
     *
     * @param person the {@code Person} being employed; may be {@code null}
     */
    public void employCampFollower(Person person) {
        if (person == null) {
            LOGGER.warn("A null person was passed into employCampFollower.");
            return;
        }

        person.changeStatus(this, currentDay, PersonnelStatus.ACTIVE);
        person.setRecruitment(currentDay);

        if (person.isAstech()) {
            asTechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
            asTechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
        }

        MekHQ.triggerEvent(new PersonNewEvent(person));
    }

    private void simulateRelationshipHistory(Person person) {
        // how many weeks should the simulation run?
        LocalDate localDate = getLocalDate();
        long weeksBetween = ChronoUnit.WEEKS.between(person.getDateOfBirth().plusYears(18), localDate);

        // this means there is nothing to simulate
        if (weeksBetween == 0) {
            return;
        }

        Person babysFather = null;
        Person spousesBabysFather = null;
        List<Person> currentChildren = new ArrayList<>(); // Children that join with the character
        List<Person> priorChildren = new ArrayList<>(); // Children that were lost during divorce

        Person currentSpouse = null; // The current spouse
        List<Person> allSpouses = new ArrayList<>(); // All spouses current or divorced


        // run the simulation
        for (long weeksRemaining = weeksBetween; weeksRemaining >= 0; weeksRemaining--) {
            LocalDate currentDate = getLocalDate().minusWeeks(weeksRemaining);

            // first, we check for old relationships ending and new relationships beginning
            if (currentSpouse != null) {
                getDivorce().processNewWeek(this, currentDate, person, true);

                if (!person.getGenealogy().hasSpouse()) {
                    List<Person> toRemove = new ArrayList<>();

                    // there is a chance a departing spouse might take some of their children with
                    // them
                    for (Person child : currentChildren) {
                        if (child.getGenealogy().getParents().contains(currentSpouse)) {
                            if (randomInt(2) == 0) {
                                toRemove.add(child);
                            }
                        }
                    }

                    currentChildren.removeAll(toRemove);

                    priorChildren.addAll(toRemove);

                    currentSpouse = null;
                }
            } else {
                getMarriage().processBackgroundMarriageRolls(this, currentDate, person);

                if (person.getGenealogy().hasSpouse()) {
                    currentSpouse = person.getGenealogy().getSpouse();
                    allSpouses.add(currentSpouse);
                }
            }

            // then we check for children
            if ((person.getGender().isFemale()) && (!person.isPregnant())) {
                getProcreation().processRandomProcreationCheck(this,
                      localDate.minusWeeks(weeksRemaining),
                      person,
                      true);

                if (person.isPregnant()) {

                    if ((currentSpouse != null) && (currentSpouse.getGender().isMale())) {
                        babysFather = currentSpouse;
                    }
                }
            }

            if ((currentSpouse != null) && (currentSpouse.getGender().isFemale()) && (!currentSpouse.isPregnant())) {
                getProcreation().processRandomProcreationCheck(this,
                      localDate.minusWeeks(weeksRemaining),
                      currentSpouse,
                      true);

                if (currentSpouse.isPregnant()) {
                    if (person.getGender().isMale()) {
                        spousesBabysFather = person;
                    }
                }
            }

            if ((person.isPregnant()) && (currentDate.isAfter(person.getDueDate()))) {
                currentChildren.addAll(getProcreation().birthHistoric(this, currentDate, person, babysFather));
                babysFather = null;
            }

            if ((currentSpouse != null) &&
                      (currentSpouse.isPregnant()) &&
                      (currentDate.isAfter(currentSpouse.getDueDate()))) {
                currentChildren.addAll(getProcreation().birthHistoric(this,
                      currentDate,
                      currentSpouse,
                      spousesBabysFather));
                spousesBabysFather = null;
            }
        }

        // with the simulation concluded, we add the current spouse (if any) and any
        // remaining children to the unit
        for (Person spouse : allSpouses) {
            recruitPerson(spouse, PrisonerStatus.FREE, true, false, false);

            if (currentSpouse == spouse) {
                addReport(PERSONNEL, String.format(resources.getString("relativeJoinsForce.text"),
                      spouse.getHyperlinkedFullTitle(),
                      person.getHyperlinkedFullTitle(),
                      resources.getString("relativeJoinsForceSpouse.text")));
            } else {
                spouse.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(spouse));
        }

        List<Person> allChildren = new ArrayList<>();
        allChildren.addAll(currentChildren);
        allChildren.addAll(priorChildren);

        for (Person child : allChildren) {
            child.setOriginFaction(person.getOriginFaction());
            child.setOriginPlanet(person.getOriginPlanet());

            int age = child.getAge(localDate);

            // Limit skills by age for children and adolescents
            if (age < 16) {
                child.removeAllSkills();
            } else if (age < 18) {
                child.limitSkills(0);
            }

            // re-roll SPAs to include in any age and skill adjustments
            Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);

            for (IOption option : Collections.list(options)) {
                child.getOptions().getOption(option.getName()).clearValue();
            }

            int experienceLevel = child.getExperienceLevel(this, false);

            // set loyalty
            if (experienceLevel <= 0) {
                person.setLoyalty(d6(3) + 2);
            } else if (experienceLevel == 1) {
                person.setLoyalty(d6(3) + 1);
            } else {
                person.setLoyalty(d6(3));
            }

            if (experienceLevel >= 0) {
                AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
                specialAbilityGenerator.setSkillPreferences(new RandomSkillPreferences());
                specialAbilityGenerator.generateSpecialAbilities(this, child, experienceLevel);
            }

            recruitPerson(child, PrisonerStatus.FREE, true, false, false);

            if (currentChildren.contains(child)) {
                addReport(PERSONNEL, String.format(resources.getString("relativeJoinsForce.text"),
                      child.getHyperlinkedFullTitle(),
                      person.getHyperlinkedFullTitle(),
                      resources.getString("relativeJoinsForceChild.text")));
            } else {
                child.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(child));
        }

        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }
    // endregion Personnel Recruitment

    // region Bloodnames

    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on skill and rank. If the
     * roll indicates there should be a bloodname, one is assigned as appropriate to the person's phenotype and the
     * player's faction.
     *
     * @param person     The Bloodname candidate
     * @param ignoreDice If true, skips the random roll and assigns a Bloodname automatically
     */
    public void checkBloodnameAdd(Person person, boolean ignoreDice) {
        // if person is non-clan or does not have a phenotype
        if (!person.isClanPersonnel() || person.getPhenotype().isNone()) {
            return;
        }

        // Person already has a bloodname, we open up the dialog to ask if they want to
        // keep the
        // current bloodname or assign a new one
        if (!person.getBloodname().isEmpty()) {
            int result = JOptionPane.showConfirmDialog(null,
                  person.getFullTitle() +
                        " already has the bloodname " +
                        person.getBloodname() +
                        "\nDo you wish to remove that bloodname and generate a new one?",
                  "Already Has Bloodname",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            } else {
                ignoreDice = true;
            }
        }

        // Go ahead and generate a new bloodname
        SkillModifierData skillModifierData = person.getSkillModifierData();

        int bloodnameTarget = 6;
        if (!ignoreDice) {
            switch (person.getPhenotype()) {
                case MEKWARRIOR: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_MEK) ?
                                             person.getSkill(SkillType.S_GUN_MEK)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_MEK) ?
                                             person.getSkill(SkillType.S_PILOT_MEK)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case AEROSPACE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_AERO) ?
                                             person.getSkill(SkillType.S_GUN_AERO)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_AERO) ?
                                             person.getSkill(SkillType.S_PILOT_AERO)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case ELEMENTAL: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_BA) ?
                                             person.getSkill(SkillType.S_GUN_BA)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_ANTI_MEK) ?
                                             person.getSkill(SkillType.S_ANTI_MEK)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case VEHICLE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_VEE) ?
                                             person.getSkill(SkillType.S_GUN_VEE)
                                                   .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    switch (person.getPrimaryRole()) {
                        case VEHICLE_CREW_GROUND:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_GVEE)
                                                           .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VEHICLE_CREW_NAVAL:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_NVEE)
                                                           .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VEHICLE_CREW_VTOL:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL) ?
                                                     person.getSkill(SkillType.S_PILOT_VTOL)
                                                           .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        default:
                            break;
                    }
                    break;
                }
                case PROTOMEK: {
                    bloodnameTarget += 2 *
                                             (person.hasSkill(SkillType.S_GUN_PROTO) ?
                                                    person.getSkill(SkillType.S_GUN_PROTO)
                                                          .getFinalSkillValue(skillModifierData) :
                                                    TargetRoll.AUTOMATIC_FAIL);
                    break;
                }
                case NAVAL: {
                    switch (person.getPrimaryRole()) {
                        case VESSEL_PILOT:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_PILOT_SPACE) ?
                                                            person.getSkill(SkillType.S_PILOT_SPACE)
                                                                  .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_GUNNER:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_GUN_SPACE) ?
                                                            person.getSkill(SkillType.S_GUN_SPACE)
                                                                  .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_CREW:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_TECH_VESSEL) ?
                                                            person.getSkill(SkillType.S_TECH_VESSEL)
                                                                  .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_NAVIGATOR:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_NAVIGATION) ?
                                                            person.getSkill(SkillType.S_NAVIGATION)
                                                                  .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        default:
                            break;
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            // Higher-rated units are more likely to have Blood named
            bloodnameTarget += DragoonRating.DRAGOON_C.getRating() - getAtBUnitRatingMod();

            // Reavings diminish the number of available Blood rights in later eras
            int year = getGameYear();
            if (year <= 2950) {
                bloodnameTarget--;
            }

            if (year > 3055) {
                bloodnameTarget++;
            }

            if (year > 3065) {
                bloodnameTarget++;
            }

            if (year > 3080) {
                bloodnameTarget++;
            }

            // Officers have better chance; no penalty for non-officer
            bloodnameTarget += Math.min(0, getRankSystem().getOfficerCut() - person.getRankNumeric());
        }

        if (ignoreDice || (d6(2) >= bloodnameTarget)) {
            final Phenotype phenotype = person.getPhenotype().isNone() ? Phenotype.GENERAL : person.getPhenotype();

            final Bloodname bloodname = Bloodname.randomBloodname((getFaction().isClan() ?
                                                                         getFaction() :
                                                                         person.getOriginFaction()).getShortName(),
                  phenotype,
                  getGameYear());
            if (bloodname != null) {
                person.setBloodname(bloodname.getName());
                personUpdated(person);
            }
        }
    }
    // endregion Bloodnames

    // region Other Personnel Methods

    /**
     * Imports a {@link Person} into a campaign.
     *
     * @param person A {@link Person} to import into the campaign.
     */
    public void importPerson(Person person) {
        personnel.put(person.getId(), person);
        MekHQ.triggerEvent(new PersonNewEvent(person));
    }

    public @Nullable Person getPerson(final UUID id) {
        return personnel.get(id);
    }

    public Collection<Person> getPersonnel() {
        return personnel.values();
    }

    /**
     * Retrieves a list of personnel, excluding those whose status indicates they have left the unit.
     * <p>
     * This method filters the personnel collection to only include individuals who are still part of the unit, as
     * determined by their status.
     * </p>
     *
     * @return a {@code List} of {@link Person} objects who have not left the unit
     */
    public List<Person> getPersonnelFilteringOutDeparted() {
        return getPersonnel().stream()
                     .filter(person -> !person.getStatus().isDepartedUnit())
                     .collect(Collectors.toList());
    }


    /**
     * Retrieves a list of personnel, excluding those whose status indicates they have either left the unit, or are
     * presently away.
     *
     * @return a {@code List} of {@link Person} objects who have not left the unit
     */
    public List<Person> getPersonnelFilteringOutDepartedAndAbsent() {
        return getPersonnel().stream()
                     .filter(person -> !person.getStatus().isDepartedUnit())
                     .filter(person -> !person.getStatus().isAbsent())
                     .collect(Collectors.toList());
    }

    /**
     * @deprecated use {@link #getActivePersonnel(boolean, boolean)} instead.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<Person> getActivePersonnel(boolean includePrisoners) {
        return getActivePersonnel(includePrisoners, false);
    }

    /**
     * Returns a list of personnel who are considered "active" according to various status filters.
     *
     * <p>This method iterates through all personnel and includes those whose status is considered "active," then
     * optionally excludes personnel based on the provided flags for prisoners and camp followers.</p>
     *
     * <ul>
     *   <li>If {@code includePrisoners} is {@code false}, any personnel who are currently prisoners (not free or
     *   bondsmen) will be excluded from the result.</li>
     *   <li>If {@code includeCampFollowers} is {@code false}, (non-prisoner) camp followers will be excluded from the
     *   result.</li>
     *   <li>All included personnel are guaranteed to have a status of {@link PersonnelStatus#ACTIVE} or
     *   {@link PersonnelStatus#CAMP_FOLLOWER} (if appropriate).</li>
     * </ul>
     *
     * <p><b>Notes:</b> It might be tempting to overload this method with a version that skips one of the boolean
     * params. I strongly recommend against this. By forcing developers to explicitly dictate prisoner and follower
     * inclusion we reduce the risk of either demographic being included/excluded by accident. As happened
     * frequently prior to these booleans being added. - Illiani, 5th Oct 2025</p>
     *
     * @param includePrisoners     {@code true} to include prisoners
     * @param includeCampFollowers {@code true} to include <b>non-prisoner</b> camp followers
     *
     * @return a {@link List} of {@link Person} objects matching the criteria
     */
    public List<Person> getActivePersonnel(boolean includePrisoners, boolean includeCampFollowers) {
        String cacheKey = "includePrisoners:" + includePrisoners + "_" + "includeCampFollowers:" + includeCampFollowers;

        // If the cache value is known and not empty, let's just use that
        // An empty list will be cached after loading so we will always
        // recalculate if it's empty. And if it's empty, it should be quick, right?
        if (activePersonnelCache != null &&
                  activePersonnelCache.containsKey(cacheKey) &&
                  !activePersonnelCache.get(cacheKey).isEmpty()) {
            return new ArrayList<>(activePersonnelCache.get(cacheKey));
        }

        List<Person> activePersonnel = new ArrayList<>();

        for (Person person : getPersonnel()) {
            PersonnelStatus status = person.getStatus();
            PrisonerStatus prisonerStatus = person.getPrisonerStatus();
            boolean isActive = status.isActiveFlexible();
            boolean isCampFollower = prisonerStatus.isFreeOrBondsman() && status.isCampFollower();
            boolean isActivePrisoner = person.getPrisonerStatus().isCurrentPrisoner() && isActive;

            if (!isActive) {
                continue;
            }

            if (!includeCampFollowers && isCampFollower) {
                continue;
            }

            if (!includePrisoners && isActivePrisoner) {
                continue;
            }

            activePersonnel.add(person);
        }

        if (activePersonnelCache == null) {
            activePersonnelCache = new HashMap<>();
        }
        activePersonnelCache.put(cacheKey, new ArrayList<>(activePersonnel));
        return activePersonnel;
    }

    /**
     * Clears the {@code activePersonnelCache} so it's recalculated next time we getActivePersonnel
     */
    public void invalidateActivePersonnelCache() {
        activePersonnelCache.clear();
    }

    /**
     * @return a list of people who are currently eligible to receive a salary.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public List<Person> getSalaryEligiblePersonnel() {
        return getActivePersonnel(false, false).stream()
                     .filter(person -> person.getStatus().isSalaryEligible())
                     .collect(Collectors.toList());
    }

    /**
     * Retrieves a filtered list of personnel who have at least one combat profession.
     * <p>
     * This method filters the list of all personnel to include only those whose primary or secondary role is designated
     * as a combat role.
     * </p>
     *
     * @return a {@link List} of {@link Person} objects representing combat-capable personnel
     */
    public List<Person> getActiveCombatPersonnel() {
        return getActivePersonnel(false, false).stream()
                     .filter(p -> p.getPrimaryRole().isCombat() || p.getSecondaryRole().isCombat())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active Dependents (including camp followers).
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getActiveDependents() {
        return getPersonnel().stream()
                     .filter(person -> person.getPrimaryRole().isDependent())
                     .filter(person -> person.getStatus().isActiveFlexible())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active prisoners.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getCurrentPrisoners() {
        return getActivePersonnel(true, false).stream()
                     .filter(person -> person.getPrisonerStatus().isCurrentPrisoner())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active prisoners who are willing to defect.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getPrisonerDefectors() {
        return getActivePersonnel(true, false).stream()
                     .filter(person -> person.getPrisonerStatus().isPrisonerDefector())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only friendly PoWs.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getFriendlyPrisoners() {
        return getPersonnel().stream().filter(p -> p.getStatus().isPoW()).collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only Persons with the Student status.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getStudents() {
        return getPersonnel().stream().filter(p -> p.getStatus().isStudent()).collect(Collectors.toList());
    }
    // endregion Other Personnel Methods

    // region Personnel Selectors and Generators

    /**
     * Gets the {@link AbstractFactionSelector} to use with this campaign.
     *
     * @return An {@link AbstractFactionSelector} to use when selecting a {@link Faction}.
     */
    public AbstractFactionSelector getFactionSelector() {
        return getFactionSelector(getCampaignOptions().getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractFactionSelector} to use
     *
     * @param options the random origin options to use
     *
     * @return An {@link AbstractFactionSelector} to use when selecting a {@link Faction}.
     */
    public AbstractFactionSelector getFactionSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedFactionSelector(options) : new DefaultFactionSelector(options);
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use with this campaign.
     *
     * @return An {@link AbstractPlanetSelector} to use when selecting a {@link Planet}.
     */
    public AbstractPlanetSelector getPlanetSelector() {
        return getPlanetSelector(getCampaignOptions().getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use
     *
     * @param options the random origin options to use
     *
     * @return An {@link AbstractPlanetSelector} to use when selecting a {@link Planet}.
     */
    public AbstractPlanetSelector getPlanetSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedPlanetSelector(options) : new DefaultPlanetSelector(options);
    }

    /**
     * Gets the {@link AbstractPersonnelGenerator} to use with this campaign.
     *
     * @param factionSelector The {@link AbstractFactionSelector} to use when choosing a {@link Faction}.
     * @param planetSelector  The {@link AbstractPlanetSelector} to use when choosing a {@link Planet}.
     *
     * @return An {@link AbstractPersonnelGenerator} to use when creating new personnel.
     */
    public AbstractPersonnelGenerator getPersonnelGenerator(final AbstractFactionSelector factionSelector,
          final AbstractPlanetSelector planetSelector) {
        final DefaultPersonnelGenerator generator = new DefaultPersonnelGenerator(factionSelector, planetSelector);
        generator.setNameGenerator(RandomNameGenerator.getInstance());
        generator.setSkillPreferences(getRandomSkillPreferences());
        return generator;
    }
    // endregion Personnel Selectors and Generators
    // endregion Personnel

    public List<Person> getPatients() {
        List<Person> patients = new ArrayList<>();
        for (Person person : getActivePersonnel(true, true)) {
            if (person.needsFixing()) {
                patients.add(person);
            }
        }
        return patients;
    }

    public List<Person> getPatientsAssignedToDoctors() {
        return getPatients()
                     .stream()
                     .filter(patient -> patient.getDoctorId() != null)
                     .toList();
    }

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
            parts.addPart(p, false);
        }
    }

    /**
     * Gets the Warehouse which stores parts.
     */
    public Warehouse getWarehouse() {
        return parts;
    }

    /**
     * Sets the Warehouse which stores parts for the campaign.
     *
     * @param warehouse The warehouse in which to store parts.
     */
    public void setWarehouse(Warehouse warehouse) {
        parts = Objects.requireNonNull(warehouse);
    }

    public Quartermaster getQuartermaster() {
        return quartermaster;
    }

    /**
     * @return A collection of parts in the Warehouse.
     */
    public Collection<Part> getParts() {
        return parts.getParts();
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    private int getQuantity(Part part) {
        return getWarehouse().getPartQuantity(part, true);
    }

    public Part getPart(int id) {
        return parts.getPart(id);
    }

    @Nullable
    public Force getForce(int id) {
        return forceIds.get(id);
    }

    public List<String> getCurrentReport() {
        return currentReport;
    }

    public void setCurrentReportHTML(String html) {
        currentReportHTML = html;
    }

    public String getCurrentReportHTML() {
        return currentReportHTML;
    }

    public List<String> getNewReports() {
        return newReports;
    }

    public void setNewReports(List<String> reports) {
        newReports = reports;
    }

    public List<String> fetchAndClearNewReports() {
        List<String> oldReports = newReports;
        setNewReports(new ArrayList<>());
        return oldReports;
    }

    public List<String> getSkillReport() {
        return skillReport;
    }

    public void setSkillReportHTML(String html) {
        skillReportHTML = html;
    }

    public String getSkillReportHTML() {
        return skillReportHTML;
    }

    public List<String> getNewSkillReports() {
        return newSkillReports;
    }

    public void setNewSkillReports(List<String> reports) {
        newSkillReports = reports;
    }

    public List<String> fetchAndClearNewSkillReports() {
        List<String> oldSkillReports = newSkillReports;
        setNewSkillReports(new ArrayList<>());
        return oldSkillReports;
    }

    public List<String> getTechnicalReport() {
        return technicalReport;
    }

    public void setTechnicalReportHTML(String html) {
        technicalReportHTML = html;
    }

    public String getTechnicalReportHTML() {
        return technicalReportHTML;
    }

    public List<String> getNewTechnicalReports() {
        return newTechnicalReports;
    }

    public void setNewTechnicalReports(List<String> reports) {
        newTechnicalReports = reports;
    }

    public List<String> fetchAndClearNewTechnicalReports() {
        List<String> oldTechnicalReports = newTechnicalReports;
        setNewTechnicalReports(new ArrayList<>());
        return oldTechnicalReports;
    }

    public List<String> getFinancesReport() {
        return financesReport;
    }

    public void setFinancesReportHTML(String html) {
        financesReportHTML = html;
    }

    public String getFinancesReportHTML() {
        return financesReportHTML;
    }

    public List<String> getNewFinancesReports() {
        return newFinancesReports;
    }

    public void setNewFinancesReports(List<String> reports) {
        newFinancesReports = reports;
    }

    public List<String> fetchAndClearNewFinancesReports() {
        List<String> oldFinancesReports = newFinancesReports;
        setNewFinancesReports(new ArrayList<>());
        return oldFinancesReports;
    }

    public List<String> getAcquisitionsReport() {
        return acquisitionsReport;
    }

    public void setAcquisitionsReportHTML(String html) {
        acquisitionsReportHTML = html;
    }

    public String getAcquisitionsReportHTML() {
        return acquisitionsReportHTML;
    }

    public List<String> getNewAcquisitionsReports() {
        return newAcquisitionsReports;
    }

    public void setNewAcquisitionsReports(List<String> reports) {
        newAcquisitionsReports = reports;
    }

    public List<String> fetchAndClearNewAcquisitionsReports() {
        List<String> oldAcquisitionsReports = newAcquisitionsReports;
        setNewAcquisitionsReports(new ArrayList<>());
        return oldAcquisitionsReports;
    }

    public List<String> getMedicalReport() {
        return medicalReport;
    }

    public void setMedicalReportHTML(String html) {
        medicalReportHTML = html;
    }

    public String getMedicalReportHTML() {
        return medicalReportHTML;
    }

    public List<String> getNewMedicalReports() {
        return newMedicalReports;
    }

    public void setNewMedicalReports(List<String> reports) {
        newMedicalReports = reports;
    }

    public List<String> fetchAndClearNewMedicalReports() {
        List<String> oldMedicalReports = newMedicalReports;
        setNewMedicalReports(new ArrayList<>());
        return oldMedicalReports;
    }

    public List<String> getPersonnelReport() {
        return personnelReport;
    }

    public void setPersonnelReportHTML(String html) {
        personnelReportHTML = html;
    }

    public String getPersonnelReportHTML() {
        return personnelReportHTML;
    }

    public List<String> getNewPersonnelReports() {
        return newPersonnelReports;
    }

    public void setNewPersonnelReports(List<String> reports) {
        newPersonnelReports = reports;
    }

    public List<String> fetchAndClearNewPersonnelReports() {
        List<String> oldPersonnelReports = newPersonnelReports;
        setNewPersonnelReports(new ArrayList<>());
        return oldPersonnelReports;
    }

    public List<String> getBattleReport() {
        return battleReport;
    }

    public void setBattleReportHTML(String html) {
        battleReportHTML = html;
    }

    public String getBattleReportHTML() {
        return battleReportHTML;
    }

    public List<String> getNewBattleReports() {
        return newBattleReports;
    }

    public void setNewBattleReports(List<String> reports) {
        newBattleReports = reports;
    }

    public List<String> fetchAndClearNewBattleReports() {
        List<String> oldBattleReports = newBattleReports;
        setNewBattleReports(new ArrayList<>());
        return oldBattleReports;
    }

    public List<String> getPoliticsReport() {
        return politicsReport;
    }

    public void setPoliticsReportHTML(String html) {
        politicsReportHTML = html;
    }

    public String getPoliticsReportHTML() {
        return politicsReportHTML;
    }

    public List<String> getNewPoliticsReports() {
        return newPoliticsReports;
    }

    public void setNewPoliticsReports(List<String> reports) {
        newPoliticsReports = reports;
    }

    public List<String> fetchAndClearNewPoliticsReports() {
        List<String> oldPoliticsReports = newPoliticsReports;
        setNewPoliticsReports(new ArrayList<>());
        return oldPoliticsReports;
    }

    /**
     * Finds the active person in a particular role with the highest level in a given, with an optional secondary skill
     * to break ties.
     *
     * @param role      One of the PersonnelRole enum values
     * @param primary   The skill to use for comparison.
     * @param secondary If not null and there is more than one person tied for the most the highest, preference will be
     *                  given to the one with a higher level in the secondary skill.
     *
     * @return The person in the designated role with the most experience.
     */
    public Person findBestInRole(PersonnelRole role, String primary, @Nullable String secondary) {
        int highest = 0;
        Person bestInRole = null;

        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isClanCampaign = isClanCampaign();

        for (Person person : getActivePersonnel(false, false)) {
            SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                  currentDay);
            if (((person.getPrimaryRole() == role) || (person.getSecondaryRole() == role)) &&
                      (person.getSkill(primary) != null)) {
                Skill primarySkill = person.getSkill(primary);
                int currentSkillLevel = Integer.MIN_VALUE;

                if (primarySkill != null) {
                    currentSkillLevel = primarySkill.getTotalSkillLevel(skillModifierData);
                }

                if (bestInRole == null || currentSkillLevel > highest) {
                    bestInRole = person;
                    highest = currentSkillLevel;
                } else if (secondary != null && currentSkillLevel == highest) {
                    Skill secondarySkill = person.getSkill(secondary);

                    if (secondarySkill == null) {
                        continue;
                    }

                    currentSkillLevel = secondarySkill.getTotalSkillLevel(skillModifierData);

                    int bestInRoleSecondarySkill = Integer.MIN_VALUE;
                    if (bestInRole.hasSkill(secondary)) {
                        bestInRoleSecondarySkill = secondarySkill.getTotalSkillLevel(skillModifierData);
                    }

                    if (currentSkillLevel > bestInRoleSecondarySkill) {
                        bestInRole = person;
                    }
                }
            }
        }
        return bestInRole;
    }

    public Person findBestInRole(PersonnelRole role, String skill) {
        return findBestInRole(role, skill, null);
    }

    /**
     * Finds and returns the {@link Person} with the highest total skill level for a specified skill.
     *
     * <p>This method iterates over all active personnel, calculates each individual's total skill level
     * for the given skill (taking into account campaign options, reputation modifiers, and attributes), and determines
     * who possesses the highest skill value. If none are found, {@code null} is returned.</p>
     *
     * @param skillName the name of the skill to evaluate among all active personnel
     *
     * @return the {@link Person} with the highest calculated total skill level in the specified skill, or {@code null}
     *       if no qualifying person is found
     */
    public @Nullable Person findBestAtSkill(String skillName) {
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isClanCampaign = isClanCampaign();
        Person bestAtSkill = null;
        int highest = 0;
        for (Person person : getActivePersonnel(false, false)) {
            Skill skill = person.getSkill(skillName);

            int totalSkillLevel = Integer.MIN_VALUE;
            if (skill != null) {
                SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                      currentDay);
                totalSkillLevel = skill.getTotalSkillLevel(skillModifierData);
            }

            if (totalSkillLevel > highest) {
                highest = totalSkillLevel;
                bestAtSkill = person;
            }
        }
        return bestAtSkill;
    }

    /**
     * @return The list of all active {@link Person}s who qualify as technicians ({@link Person#isTech()});
     */
    public List<Person> getTechs() {
        return getTechs(false);
    }

    public List<Person> getTechs(final boolean noZeroMinute) {
        return getTechs(noZeroMinute, false);
    }

    public List<Person> getTechsExpanded() {
        return getTechsExpanded(false, false, true);
    }

    public List<Person> getTechs(final boolean noZeroMinute, final boolean eliteFirst) {
        return getTechsExpanded(noZeroMinute, eliteFirst, false);
    }

    /**
     * Retrieves a list of active technicians, with options to include only those with time remaining, prioritize elite
     * technicians, and expand the search to include technicians with additional roles.
     *
     * <p>The resulting list includes {@link Person} objects who qualify as technicians ({@link Person#isTech()})
     * or, if specified, as expanded technicians ({@link Person#isTechExpanded()}). If the person is part of a
     * self-crewed unit (e.g., an engineer on a self-crewed vessel), they are also included in the list.</p>
     *
     * <p>The returned list can be customized and sorted based on a variety of criteria:</p>
     * <ul>
     *   <li>Technicians with no remaining available time can be excluded if {@code noZeroMinute} is set to {@code true}.</li>
     *   <li>The list can be sorted from elite (best) to the least skilled if {@code eliteFirst} is set to {@code true}.</li>
     *   <li>When {@code expanded} is set to {@code true}, technicians with expanded roles (e.g., dual skill sets) are included
     *       in addition to regular technicians.</li>
     *   <li>The list is further sorted in the following order:
     *     <ol>
     *       <li>By skill level (default: lowest to highest, or highest to lowest if elite-first enabled).</li>
     *       <li>By available daily tech time (highest to lowest).</li>
     *       <li>By rank (lowest to highest).</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @param noZeroMinute If {@code true}, excludes technicians with no remaining available minutes.
     * @param eliteFirst   If {@code true}, sorts the list to place the most skilled technicians at the top.
     * @param expanded     If {@code true}, includes technicians with expanded roles (e.g., those qualifying under
     *                     {@link Person#isTechExpanded()}).
     *
     * @return A list of active {@link Person} objects who qualify as technicians or expanded technicians, sorted by
     *       skill, available time, and rank as specified by the input parameters.
     */
    public List<Person> getTechsExpanded(final boolean noZeroMinute, final boolean eliteFirst, final boolean expanded) {
        final List<Person> techs = getActivePersonnel(false, false).stream()
                                         .filter(person -> (expanded ? person.isTechExpanded() : person.isTech()) &&
                                                                 (!noZeroMinute || (person.getMinutesLeft() > 0)))
                                         .collect(Collectors.toList());

        // also need to loop through and collect engineers on self-crewed vessels
        for (final Unit unit : getUnits()) {
            if (unit.isSelfCrewed() && !(unit.getEntity() instanceof Infantry) && (unit.getEngineer() != null)) {
                techs.add(unit.getEngineer());
            }
        }

        // Return the tech collection sorted worst to best Skill Level, or reversed if we want elites first
        techs.sort(Comparator.comparingInt(person -> person.getSkillLevel(this,
              !person.getPrimaryRole().isTech() && person.getSecondaryRole().isTechSecondary(), true).ordinal()));

        if (eliteFirst) {
            Collections.reverse(techs);
        }

        // sort based on available minutes (highest -> lowest)
        techs.sort(Comparator.comparingInt(person -> -person.getDailyAvailableTechTime(getCampaignOptions().isTechsUseAdministration())));

        // finally, sort based on rank (lowest -> highest)
        techs.sort((person1, person2) -> {
            if (person1.outRanks(person2)) {
                return 1; // person1 outranks person2 -> person2 should come first
            } else if (person2.outRanks(person1)) {
                return -1; // person2 outranks person1 -> person1 should come first
            } else {
                return 0; // They are considered equal
            }
        });

        return techs;
    }

    public List<Person> getAdmins() {
        List<Person> admins = new ArrayList<>();
        for (Person person : getActivePersonnel(false, false)) {
            if (person.isAdministrator()) {
                admins.add(person);
            }
        }
        return admins;
    }

    public boolean isWorkingOnRefit(Person person) {
        Objects.requireNonNull(person);

        Unit unit = getHangar().findUnit(u -> u.isRefitting() && person.equals(u.getRefit().getTech()));
        return unit != null;
    }

    public List<Person> getDoctors() {
        List<Person> docs = new ArrayList<>();
        for (Person person : getActivePersonnel(false, false)) {
            if (person.isDoctor()) {
                docs.add(person);
            }
        }
        return docs;
    }

    public int getPatientsFor(Person doctor) {
        int patients = 0;
        for (Person person : getActivePersonnel(true, true)) {
            if ((null != person.getDoctorId()) && person.getDoctorId().equals(doctor.getId())) {
                patients++;
            }
        }
        return patients;
    }

    /**
     * Retrieves the best logistics person based on the acquisition skill, personnel category, and maximum acquisitions
     * allowed for the campaign.
     *
     * <p>This method evaluates all active personnel to determine the most suitable candidate
     * for logistics tasks, depending on the specified acquisition skill and rules. The determination is made according
     * to the following logic:</p>
     * <ul>
     *   <li>If the skill is {@code S_AUTO}, the method immediately returns {@code null}.</li>
     *   <li>If the skill is {@code S_TECH}, the method evaluates personnel based on their technical
     *       skill level, ignoring those who are ineligible for procurement or who exceed
     *       the maximum acquisition limit.</li>
     *   <li>For all other skills, the method evaluates personnel who possess the specified skill,
     *       ensuring their eligibility for procurement and checking that they have not exceeded
     *       the maximum acquisition limit.</li>
     * </ul>
     *
     * <p>The "best" logistics person is selected as the one with the highest skill level (based on the skill being
     * evaluated). If no suitable candidate is found, the method returns {@code null}.
     *
     * @return The {@link Person} representing the best logistics character, or {@code null} if no suitable person is
     *       found.
     */
    public @Nullable Person getLogisticsPerson() {
        final AcquisitionsType acquisitionsType = campaignOptions.getAcquisitionType();
        String fixedSkillName = "";
        boolean isAnyTech = false;

        switch (acquisitionsType) {
            case ADMINISTRATION -> fixedSkillName = S_ADMIN;
            case ANY_TECH -> {
                isAnyTech = true;
            }
            case AUTOMATIC -> {
                return null;
            }
            case NEGOTIATION -> fixedSkillName = S_NEGOTIATION;
        }

        final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
        final int defaultMaxAcquisitions = campaignOptions.getMaxAcquisitions();

        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isClanCampaign = isClanCampaign();

        int bestSkill = -1;
        Person procurementCharacter = null;
        if (isAnyTech) {
            for (Person person : getActivePersonnel(false, false)) {
                if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                    continue;
                }

                if (defaultMaxAcquisitions > 0 && (person.getAcquisitions() >= defaultMaxAcquisitions)) {
                    continue;
                }

                SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                      currentDay);
                Skill skill = person.getBestTechSkill();

                int totalSkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    totalSkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                if (totalSkillLevel > bestSkill) {
                    procurementCharacter = person;
                    bestSkill = totalSkillLevel;
                }
            }
        } else {
            for (Person person : getActivePersonnel(false, false)) {
                if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                    continue;
                }

                if (defaultMaxAcquisitions > 0 && (person.getAcquisitions() >= defaultMaxAcquisitions)) {
                    continue;
                }

                SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                      currentDay);

                Skill skill = person.getSkill(fixedSkillName);

                int totalSkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    totalSkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                if (totalSkillLevel > bestSkill) {
                    procurementCharacter = person;
                    bestSkill = totalSkillLevel;
                }
            }
        }

        return procurementCharacter;
    }

    /**
     * Finds and returns the most senior administrator for a specific type of administrative role. Seniority is
     * determined using the {@link Person#outRanksUsingSkillTiebreaker} method when there are multiple eligible
     * administrators for the specified role.
     *
     * <p>
     * The method evaluates both the primary and secondary roles of each administrator against the provided
     * {@link AdministratorSpecialization} type.
     * </p>
     *
     * <p>
     * The valid types of administrative roles are represented by the {@link AdministratorSpecialization} enum:
     * </p>
     * <ul>
     * <li>{@link AdministratorSpecialization#COMMAND} - Command Administrator</li>
     * <li>{@link AdministratorSpecialization#LOGISTICS} - Logistics
     * Administrator</li>
     * <li>{@link AdministratorSpecialization#TRANSPORT} - Transport
     * Administrator</li>
     * <li>{@link AdministratorSpecialization#HR} - HR Administrator</li>
     * </ul>
     *
     * @param type the {@link AdministratorSpecialization} representing the administrative role to check for. Passing a
     *             {@code null} type will result in an {@link IllegalStateException}.
     *
     * @return the most senior {@link Person} with the specified administrative role, or {@code null} if no eligible
     *       administrator is found.
     *
     *       <p>
     *       <b>Behavior:</b>
     *       </p>
     *       <ul>
     *       <li>The method iterates through all administrators retrieved by
     *       {@link #getAdmins()}.</li>
     *       <li>For each {@link Person}, it checks if their primary or secondary
     *       role matches the specified type
     *       via utility methods like
     *       {@code AdministratorRole#isAdministratorCommand}.</li>
     *       <li>If no eligible administrators exist, the method returns
     *       {@code null}.</li>
     *       <li>If multiple administrators are eligible, the one with the highest
     *       seniority is returned.</li>
     *       <li>Seniority is determined by the
     *       {@link Person#outRanksUsingSkillTiebreaker} method,
     *       which uses a skill-based tiebreaker when necessary.</li>
     *       </ul>
     *
     * @throws IllegalStateException if {@code type} is null or an unsupported value.
     */
    public @Nullable Person getSeniorAdminPerson(AdministratorSpecialization type) {
        Person seniorAdmin = null;

        for (Person person : getAdmins()) {
            boolean isEligible = switch (type) {
                case COMMAND -> person.getPrimaryRole().isAdministratorCommand() ||
                                      person.getSecondaryRole().isAdministratorCommand();
                case LOGISTICS -> person.getPrimaryRole().isAdministratorLogistics() ||
                                        person.getSecondaryRole().isAdministratorLogistics();
                case TRANSPORT -> person.getPrimaryRole().isAdministratorTransport() ||
                                        person.getSecondaryRole().isAdministratorTransport();
                case HR -> person.getPrimaryRole().isAdministratorHR() || person.getSecondaryRole().isAdministratorHR();
            };

            if (isEligible) {
                if (seniorAdmin == null) {
                    seniorAdmin = person;
                    continue;
                }

                if (person.outRanksUsingSkillTiebreaker(this, seniorAdmin)) {
                    seniorAdmin = person;
                }
            }
        }
        return seniorAdmin;
    }

    /**
     * Retrieves the current campaign commander.
     *
     * <p>If a commander is specifically flagged, that person will be returned. Otherwise, the highest-ranking member
     * among the unit's active personnel is selected.</p>
     *
     * @return the {@link Person} who is the commander, or {@code null} if there are no suitable candidates.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Person getCommander() {
        return findTopCommanders()[0];
    }

    /**
     * Retrieves the second-in-command among the unit's active personnel.
     *
     * <p>The second-in-command is determined as the highest-ranking active personnel member who is not the flagged
     * commander (if one exists). If multiple candidates have the same rank, a skill-based tiebreaker is used.</p>
     *
     * @return the {@link Person} who is considered the second-in-command, or {@code null} if there are no suitable
     *       candidates.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Person getSecondInCommand() {
        return findTopCommanders()[1];
    }

    /**
     * Finds the current top two candidates for command among active personnel.
     *
     * <p>In a single pass, this method determines the commander and the second-in-command using a flagged commander
     * if one is specified, otherwise relying on rank and skill tiebreakers.</p>
     *
     * @return an array where index 0 is the commander (maybe the flagged commander), and index 1 is the
     *       second-in-command; either or both may be {@code null} if no suitable personnel are available.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Person[] findTopCommanders() {
        Person flaggedCommander = getFlaggedCommander();
        Person commander = flaggedCommander;

        Person flaggedSecondInCommand = getFlaggedSecondInCommand();
        Person secondInCommand = flaggedSecondInCommand;

        if (flaggedCommander != null && flaggedSecondInCommand != null) {
            return new Person[] { commander, secondInCommand };
        }

        for (Person person : getActivePersonnel(false, false)) {
            if (person == null) {
                continue;
            }

            if (person.equals(flaggedCommander) || person.equals(flaggedSecondInCommand)) {
                continue;
            }

            // Commander selection (if not locked)
            if (flaggedCommander == null) {
                if (commander == null) {
                    commander = person;
                    continue;
                }

                if (!person.equals(commander) && person.outRanksUsingSkillTiebreaker(this, commander)) {
                    Person previousCommander = commander;
                    commander = person;

                    // Previous commander becomes a candidate for second-in-command (if not locked)
                    if (flaggedSecondInCommand == null && !previousCommander.equals(commander)) {
                        if (secondInCommand == null) {
                            secondInCommand = previousCommander;
                        } else if (!previousCommander.equals(secondInCommand)
                                         && previousCommander.outRanksUsingSkillTiebreaker(this, secondInCommand)) {
                            secondInCommand = previousCommander;
                        }
                    }
                    continue;
                }
            }

            // Second-in-command selection (if not locked), excluding commander
            if (flaggedSecondInCommand == null) {
                if (person.equals(commander)) {
                    continue;
                }

                if (secondInCommand == null) {
                    secondInCommand = person;
                    continue;
                }

                if (!person.equals(secondInCommand) && person.outRanksUsingSkillTiebreaker(this, secondInCommand)) {
                    secondInCommand = person;
                }
            }
        }

        if (commander != null && commander.equals(secondInCommand)) {
            secondInCommand = null;
        }

        return new Person[] { commander, secondInCommand };
    }

    /**
     * Retrieves a list of eligible logistics personnel who can perform procurement actions based on the current
     * campaign options. If acquisitions are set to automatically succeed, an empty list is returned.
     *
     * <p>This method evaluates active personnel to determine who is eligible for procurement
     * actions under the current campaign configuration. Personnel are filtered and sorted based on specific
     * criteria:</p>
     * <ul>
     *   <li><strong>Automatic Success:</strong> If the acquisition skill equals {@code S_AUTO},
     *       an empty list is immediately returned.</li>
     *   <li><strong>Eligibility Filtering:</strong> The following checks are applied to filter personnel:
     *       <ul>
     *          <li>Personnel must not be ineligible based on the {@link ProcurementPersonnelPick} category.</li>
     *          <li>Personnel must not have exceeded the maximum acquisition limit, if specified.</li>
     *          <li>If the skill is {@code S_TECH}, the person must have a valid technical skill.</li>
     *          <li>For other skills, the person must have the specified skill.</li>
     *       </ul>
     *    </li>
     *   <li><b>Sorting:</b> The resulting list is sorted in descending order by skill level:
     *       <ul>
     *          <li>When the skill is {@code S_TECH}, sorting is based on the person's best technical skill level.</li>
     *          <li>For other skills, sorting is based on the level of the specified skill.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @return A {@link List} of {@link Person} objects who are eligible and sorted to perform logistical actions, or an
     *       empty list if acquisitions automatically succeed.
     */
    public List<Person> getLogisticsPersonnel() {
        final AcquisitionsType acquisitionsType = getCampaignOptions().getAcquisitionType();

        String fixedSkillName = "";
        boolean isAnyTech = false;

        switch (acquisitionsType) {
            case ADMINISTRATION -> fixedSkillName = S_ADMIN;
            case ANY_TECH -> {
                isAnyTech = true;
            }
            case AUTOMATIC -> {
                return Collections.emptyList();
            }
            case NEGOTIATION -> fixedSkillName = S_NEGOTIATION;
        }

        final int maxAcquisitions = campaignOptions.getMaxAcquisitions();
        final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
        List<Person> logisticsPersonnel = new ArrayList<>();

        for (Person person : getActivePersonnel(false, false)) {
            if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                continue;
            }

            if ((maxAcquisitions > 0) && (person.getAcquisitions() >= maxAcquisitions)) {
                continue;
            }
            if (isAnyTech) {
                if (null != person.getBestTechSkill()) {
                    logisticsPersonnel.add(person);
                }
            } else if (person.hasSkill(fixedSkillName)) {
                logisticsPersonnel.add(person);
            }
        }

        // Sort by their skill level, descending.
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isClanCampaign = isClanCampaign();
        boolean finalIsAnyTech = isAnyTech; // Needed for lamba
        String finalFixedSkillName = fixedSkillName; // Also needed for lamba
        logisticsPersonnel.sort((person1, person2) -> {
            if (finalIsAnyTech) {
                // Person 1
                Skill skill = person1.getBestTechSkill();

                int person1SkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    SkillModifierData skillModifierData = person1.getSkillModifierData(isUseAgingEffects,
                          isClanCampaign, currentDay);
                    person1SkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                // Person 2
                skill = person2.getBestTechSkill();

                int person2SkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    SkillModifierData skillModifierData = person2.getSkillModifierData(isUseAgingEffects,
                          isClanCampaign, currentDay);
                    person2SkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                return Integer.compare(person1SkillLevel, person2SkillLevel);
            } else {
                // Person 1
                Skill skill = person1.getSkill(finalFixedSkillName);

                int person1SkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    SkillModifierData skillModifierData = person1.getSkillModifierData(isUseAgingEffects,
                          isClanCampaign, currentDay);
                    person1SkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                // Person 2
                skill = person2.getSkill(finalFixedSkillName);

                int person2SkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    SkillModifierData skillModifierData = person2.getSkillModifierData(isUseAgingEffects,
                          isClanCampaign, currentDay);
                    person2SkillLevel = skill.getTotalSkillLevel(skillModifierData);
                }

                return Integer.compare(person1SkillLevel, person2SkillLevel);
            }
        });

        return logisticsPersonnel;
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
    public ShoppingList goShopping(ShoppingList sList) {
        // loop through shopping items and decrement days to wait
        for (IAcquisitionWork shoppingItem : sList.getShoppingList()) {
            shoppingItem.decrementDaysToWait();
        }

        if (getCampaignOptions().getAcquisitionType() == AcquisitionsType.AUTOMATIC) {
            return goShoppingAutomatically(sList);
        } else if (!getCampaignOptions().isUsePlanetaryAcquisition()) {
            return goShoppingStandard(sList);
        } else {
            return goShoppingByPlanet(sList);
        }
    }

    /**
     * Shops for items on the {@link ShoppingList}, where each acquisition automatically succeeds.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ShoppingList goShoppingAutomatically(ShoppingList sList) {
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

        return new ShoppingList(remainingItems);
    }

    /**
     * Shops for items on the {@link ShoppingList}, where each acquisition is performed by available logistics
     * personnel.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ShoppingList goShoppingStandard(ShoppingList sList) {
        List<Person> logisticsPersonnel = getLogisticsPersonnel();
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

        return new ShoppingList(currentList);
    }

    /**
     * Shops for items on the {@link ShoppingList}, where each acquisition is attempted on nearby planets by available
     * logistics personnel.
     *
     * @param sList The shopping list to use when shopping.
     *
     * @return The new shopping list containing the items that were not acquired.
     */
    private ShoppingList goShoppingByPlanet(ShoppingList sList) {
        List<Person> logisticsPersonnel = getLogisticsPersonnel();
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
                                                    getCampaignOptions().getMaxJumpsPlanetaryAcquisition(),
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

                            int totalQuantity = 0;
                            while (shoppingItem.getQuantity() > 0 &&
                                         canAcquireParts(person) &&
                                         acquireEquipment(shoppingItem, person, system, transitTime)) {
                                totalQuantity++;
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
                            if (!getCampaignOptions().isPlanetAcquisitionVerbose()) {
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

        return new ShoppingList(currentList);
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
        int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
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
        if ((acquisition instanceof UnitOrder && getCampaignOptions().isPayForUnits()) ||
                  (acquisition instanceof Part && getCampaignOptions().isPayForParts())) {
            // CAN the acquisition actually be paid for
            return getFunds().isGreaterOrEqualThan(acquisition.getBuyCost());
        }
        return true;
    }

    /**
     * Make an acquisition roll for a given planet to see if you can identify a contact. Used for planetary based
     * acquisition.
     *
     * @param acquisition - The <code> IAcquisitionWork</code> being acquired.
     * @param person      - The <code>Person</code> object attempting to do the acquiring. may be null if no one on the
     *                    force has the skill or the user is using automatic acquisition.
     * @param system      - The <code>PlanetarySystem</code> object where the acquisition is being attempted. This may
     *                    be null if the user is not using planetary acquisition.
     *
     * @return The result of the rolls.
     */
    public PartAcquisitionResult findContactForAcquisition(IAcquisitionWork acquisition, Person person,
          PlanetarySystem system) {
        TargetRoll target = getTargetForAcquisition(acquisition, person);

        String impossibleSentencePrefix = person == null ?
                                                "Can't search for " :
                                                person.getFullName() + " can't search for ";
        String failedSentencePrefix = person == null ?
                                            "No contacts available for " :
                                            person.getFullName() + " is unable to find contacts for ";
        String succeededSentencePrefix = person == null ?
                                               "Possible contact for " :
                                               person.getFullName() + " has found a contact for ";

        // if it's already impossible, don't bother with the rest
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(ACQUISITIONS, "<font color='" +
                                              ReportingUtilities.getNegativeColor() +
                                              "'><b>" +
                                              impossibleSentencePrefix +
                                              acquisition.getAcquisitionName() +
                                              " on " +
                                              system.getPrintableName(getLocalDate()) +
                                              " because:</b></font> " +
                                              target.getDesc());
            }
            return PartAcquisitionResult.PartInherentFailure;
        }

        target = system.getPrimaryPlanet()
                       .getAcquisitionMods(target,
                             getLocalDate(),
                             getCampaignOptions(),
                             getFaction(),
                             acquisition.getTechBase() == TechBase.CLAN);

        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(ACQUISITIONS, "<font color='" +
                                              ReportingUtilities.getNegativeColor() +
                                              "'><b>" +
                                              impossibleSentencePrefix +
                                              acquisition.getAcquisitionName() +
                                              " on " +
                                              system.getPrintableName(getLocalDate()) +
                                              " because:</b></font> " +
                                              target.getDesc());
            }
            return PartAcquisitionResult.PlanetSpecificFailure;
        }
        SocioIndustrialData socioIndustrial = system.getPrimaryPlanet().getSocioIndustrial(getLocalDate());
        CampaignOptions options = getCampaignOptions();
        int techBonus = options.getPlanetTechAcquisitionBonus(socioIndustrial.tech);
        int industryBonus = options.getPlanetIndustryAcquisitionBonus(socioIndustrial.industry);
        int outputsBonus = options.getPlanetOutputAcquisitionBonus(socioIndustrial.output);
        if (d6(2) < target.getValue()) {
            // no contacts on this planet, move along
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(ACQUISITIONS, "<font color='" +
                                              ReportingUtilities.getNegativeColor() +
                                              "'><b>" +
                                              failedSentencePrefix +
                                              acquisition.getAcquisitionName() +
                                              " on " +
                                              system.getPrintableName(getLocalDate()) +
                                              " at TN: " +
                                              target.getValue() +
                                              " - Modifiers (Tech: " +
                                              (techBonus > 0 ? "+" : "") +
                                              techBonus +
                                              ", Industry: " +
                                              (industryBonus > 0 ? "+" : "") +
                                              industryBonus +
                                              ", Outputs: " +
                                              (outputsBonus > 0 ? "+" : "") +
                                              outputsBonus +
                                              ") </font>");
            }
            return PartAcquisitionResult.PlanetSpecificFailure;
        } else {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(ACQUISITIONS, "<font color='" +
                                              ReportingUtilities.getPositiveColor() +
                                              "'>" +
                                              succeededSentencePrefix +
                                              acquisition.getAcquisitionName() +
                                              " on " +
                                              system.getPrintableName(getLocalDate()) +
                                              " at TN: " +
                                              target.getValue() +
                                              " - Modifiers (Tech: " +
                                              (techBonus > 0 ? "+" : "") +
                                              techBonus +
                                              ", Industry: " +
                                              (industryBonus > 0 ? "+" : "") +
                                              industryBonus +
                                              ", Outputs: " +
                                              (outputsBonus > 0 ? "+" : "") +
                                              outputsBonus +
                                              ") </font>");
            }
            return PartAcquisitionResult.Success;
        }
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
    public boolean acquireEquipment(IAcquisitionWork acquisition, Person person) {
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
    private boolean acquireEquipment(IAcquisitionWork acquisition, Person person, PlanetarySystem system,
          int transitDays) {
        boolean found = false;
        String report = "";

        if (null != person) {
            report += person.getHyperlinkedFullTitle() + ' ';
        }

        TargetRoll target = getTargetForAcquisition(acquisition, person);

        // check on funds
        if (!canPayFor(acquisition)) {
            target.addModifier(TargetRoll.IMPOSSIBLE, "Cannot afford this purchase");
        }

        if (null != system) {
            target = system.getPrimaryPlanet()
                           .getAcquisitionMods(target,
                                 getLocalDate(),
                                 getCampaignOptions(),
                                 getFaction(),
                                 acquisition.getTechBase() == TechBase.CLAN);
        }
        report += "attempts to find " + acquisition.getAcquisitionName();

        // if impossible, then return
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            report += ":<font color='" +
                            ReportingUtilities.getNegativeColor() +
                            "'><b> " +
                            target.getDesc() +
                            "</b></font>";
            if (!getCampaignOptions().isUsePlanetaryAcquisition() ||
                      getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(ACQUISITIONS, report);
            }
            return false;
        }

        int roll = d6(2);
        report += "  needs " + target.getValueAsString();
        report += " and rolls " + roll + ':';
        // Edge reroll, if applicable
        if (getCampaignOptions().isUseSupportEdge() &&
                  (roll < target.getValue()) &&
                  (person != null) &&
                  person.getOptions().booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL) &&
                  (person.getCurrentEdge() > 0)) {
            person.changeCurrentEdge(-1);
            roll = d6(2);
            report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
        }
        int xpGained = 0;
        if (roll >= target.getValue()) {
            boolean useFunctionalAppraisal = campaignOptions.isUseFunctionalAppraisal();
            double valueChange = useFunctionalAppraisal ? Appraisal.performAppraisalMultiplierCheck(person,
                  currentDay) : 1.0;
            String appraisalReport = useFunctionalAppraisal ? Appraisal.getAppraisalReport(valueChange) : "";

            if (transitDays < 0) {
                transitDays = calculatePartTransitTime(acquisition.getAvailability());
            }
            report = report + acquisition.find(transitDays, valueChange) + ' ' + appraisalReport;
            found = true;
            if (person != null) {
                if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    xpGained += getCampaignOptions().getSuccessXP();
                }
                if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    person.setNTasks(person.getNTasks() + 1);
                }
                if (person.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                    xpGained += getCampaignOptions().getTaskXP();
                    person.setNTasks(0);
                }
            }
        } else {
            report = report + acquisition.failToFind();
            if (person != null && roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
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
        if (!getCampaignOptions().isUsePlanetaryAcquisition() || getCampaignOptions().isPlanetAcquisitionVerbose()) {
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
            if (!unit.isSelfCrewed() && asTechPoolMinutes < minutes * 6) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("notEnoughAstechTime.mothballing"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                asTechPoolMinutes -= 6 * minutes;
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
            if (!unit.isSelfCrewed() && asTechPoolMinutes < minutes * 6) {
                // uh-oh
                addReport(TECHNICAL, String.format(resources.getString("notEnoughAstechTime.activation"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                asTechPoolMinutes -= 6 * minutes;
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
                                                 (double) tech.getDailyAvailableTechTime(campaignOptions.isTechsUseAdministration()));
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
                if (getCampaignOptions().isUseSupportEdge() &&
                          (roll < target.getValue()) &&
                          tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT) &&
                          (tech.getCurrentEdge() > 0)) {
                    tech.changeCurrentEdge(-1);
                    roll = tech.isRightTechTypeFor(theRefit) ? d6(2) : Utilities.roll3d6();
                    // This is needed to update the edge values of individual crewmen
                    if (tech.isEngineer()) {
                        tech.setEdgeUsed(tech.getEdgeUsed() - 1);
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
                                                             (double) tech.getDailyAvailableTechTime(campaignOptions.isTechsUseAdministration()));
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
        part.changeQuantity(-1);

        fixPart(repairable, tech);
        if (!(repairable instanceof OmniPod)) {
            getQuartermaster().addPart(repairable, 0, false);
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
                int helpMod = getShorthandedMod(getAvailableAsTechs(minutesUsed, usedOvertime), false);
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
                          (tech.getDailyAvailableTechTime(campaignOptions.isTechsUseAdministration()) > 0)) {
                    report += " will be finished ";
                    int daysLeft = (int) Math.ceil((double) partWork.getTimeLeft() /
                                                         (double) tech.getDailyAvailableTechTime(campaignOptions.isTechsUseAdministration()));
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
        int asTechMinutesUsed = minutesUsed * getAvailableAsTechs(minutesUsed, usedOvertime);
        if (asTechPoolMinutes < asTechMinutesUsed) {
            asTechMinutesUsed -= asTechPoolMinutes;
            asTechPoolMinutes = 0;
            asTechPoolOvertime -= asTechMinutesUsed;
        } else {
            asTechPoolMinutes -= asTechMinutesUsed;
        }
        // check for the type
        int roll;
        String wrongType = "";
        if (tech.isRightTechTypeFor(partWork)) {
            roll = d6(2);
        } else {
            roll = Utilities.roll3d6();
            wrongType = " <b>Warning: wrong tech type for this repair.</b>";
        }
        report = report + ",  needs " + target.getValueAsString() + " and rolls " + roll + ':';
        int xpGained = 0;
        // if we fail and would break apart, here's a chance to use Edge for a
        // re-roll...
        if (getCampaignOptions().isUseSupportEdge() &&
                  tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART) &&
                  (tech.getCurrentEdge() > 0) &&
                  (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
            if ((getCampaignOptions().isDestroyByMargin() &&
                       (getCampaignOptions().getDestroyMargin() <= (target.getValue() - roll))) ||
                      (!getCampaignOptions().isDestroyByMargin()
                             // if a legendary, primary tech and destroy by margin is NOT on
                             &&
                             ((tech.getExperienceLevel(this, false, true) == SkillType.EXP_LEGENDARY) ||
                                    tech.getPrimaryRole().isVesselCrew())) // For vessel crews
                            && (roll < target.getValue())) {
                tech.changeCurrentEdge(-1);
                roll = tech.isRightTechTypeFor(partWork) ? d6(2) : Utilities.roll3d6();
                // This is needed to update the edge values of individual crewmen
                if (tech.isEngineer()) {
                    tech.setEdgeUsed(tech.getEdgeUsed() + 1);
                }
                report += " <b>failed!</b> and would destroy the part, but uses Edge to reroll...getting a " +
                                roll +
                                ':';
            }
        }

        if (roll >= target.getValue()) {
            report = report + partWork.succeed();
            if (getCampaignOptions().isPayForRepairs() && action.equals(" fix ") && !(partWork instanceof Armor)) {
                Money cost = partWork.getUndamagedValue().multipliedBy(0.2);
                report += "<br>Repairs cost " + cost.toAmountAndSymbolString() + " worth of parts.";
                finances.debit(TransactionType.REPAIRS, getLocalDate(), cost, "Repair of " + partWork.getPartName());
            }
            if ((roll == 12) && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
                xpGained += getCampaignOptions().getSuccessXP();
            }
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                tech.setNTasks(tech.getNTasks() + 1);
            }
            if (tech.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                xpGained += getCampaignOptions().getTaskXP();
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
            if (getCampaignOptions().isDestroyByMargin()) {
                if (getCampaignOptions().getDestroyMargin() > (target.getValue() - roll)) {
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
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }

        if (xpGained > 0) {
            tech.awardXP(this, xpGained);
            report += " (" + xpGained + "XP gained) ";
        }
        report += wrongType;
        partWork.cancelAssignment(true);
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
     * TODO : I should be part of AtBContract, not Campaign
     *
     * @param contract an active AtBContract
     *
     * @return the current deployment deficit for the contract
     */
    public int getDeploymentDeficit(AtBContract contract) {
        if (!contract.isActiveOn(getLocalDate()) || contract.getStartDate().isEqual(getLocalDate())) {
            // Do not check for deficits if the contract has not started, or
            // it is the first day of the contract, as players won't have
            // had time to assign forces to the contract yet
            return 0;
        }

        int total = -contract.getRequiredCombatElements();
        int role = -max(1, contract.getRequiredCombatElements() / 2);

        final CombatRole requiredLanceRole = contract.getContractType().getRequiredCombatRole();
        for (CombatTeam combatTeam : combatTeams.values()) {
            CombatRole combatRole = combatTeam.getRole();

            if (!combatRole.isReserve() && !combatRole.isAuxiliary()) {
                if ((combatTeam.getMissionId() == contract.getId())) {
                    if (!combatRole.isTraining()) {
                        if (!combatRole.isCadre() || contract.getContractType().isCadreDuty()) {
                            total += combatTeam.getSize(this);
                        }
                    }
                }

                if (combatRole == requiredLanceRole) {
                    role += combatTeam.getSize(this);
                }
            }
        }

        if (total >= 0 && role >= 0) {
            return 0;
        }
        return Math.abs(Math.min(total, role));
    }

    /**
     * Advances the campaign by one day, processing all daily events and updates.
     *
     * <p>This method delegates to {@link CampaignNewDayManager} to handle all new day processing,
     * including personnel updates, contract management, financial transactions, maintenance tasks, and other
     * time-dependent campaign events.</p>
     *
     * @return {@code true} if the new day processing completed successfully; {@code false} if it was cancelled or
     *       failed
     *
     * @see CampaignNewDayManager#newDay()
     */
    public boolean newDay() {
        CampaignNewDayManager manager = new CampaignNewDayManager(this);
        return manager.newDay();
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
        List<Contract> activeContracts = getActiveContracts();
        int hospitalRentalCost = campaignOptions.getRentedFacilitiesCostHospitalBeds();
        Money hospitalRentalFee = FacilityRentals.calculateContractRentalCost(hospitalRentalCost, activeContracts,
              ContractRentalType.HOSPITAL_BEDS);

        int kitchenRentalCost = campaignOptions.getRentedFacilitiesCostKitchens();
        Money kitchenRentalFee = FacilityRentals.calculateContractRentalCost(kitchenRentalCost, activeContracts,
              ContractRentalType.KITCHENS);

        int holdingCellRentalCost = campaignOptions.getRentedFacilitiesCostHoldingCells();
        Money holdingCellRentalFee = FacilityRentals.calculateContractRentalCost(holdingCellRentalCost, activeContracts,
              ContractRentalType.HOLDING_CELLS);

        return hospitalRentalFee.plus(kitchenRentalFee).plus(holdingCellRentalFee);
    }

    /**
     * Use {@link #checkForNewMercenaryOrganizationStartUp(boolean, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void checkForNewMercenaryOrganizationStartUp(boolean bypassStartYear) {
        checkForNewMercenaryOrganizationStartUp(bypassStartYear, false);
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
            Person speaker = newPerson(role, chosenFaction.getShortName(), Gender.RANDOMIZE);
            new FactionJudgmentDialog(this, speaker, getCommander(),
                  "HELLO", chosenFaction,
                  FactionStandingJudgmentType.WELCOME, ImmersiveDialogWidth.MEDIUM, null, null);
        } else if (chosenFaction == null) {
            LOGGER.warn("Unable to find a suitable faction for a new mercenary organization start up");
        }
    }

    /** Use {@link #refreshPersonnelMarkets(boolean)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void refreshPersonnelMarkets() {
        refreshPersonnelMarkets(false);
    }

    /**
     * Refreshes the personnel markets based on the current market style and the current date.
     *
     * <p>If the new personnel market is disabled, generates a daily set of available personnel using the old
     * method. Otherwise, if it is the first day of the month, gathers new applications for the personnel market.
     *
     * <p>If rare professions are present, presents a dialog with options regarding these rare personnel. Optionally,
     * allowing the user to view the new personnel market dialog immediately.</p>
     *
     * @param isCampaignStart {@code true} if campaign method is being called at the start of the campaign
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void refreshPersonnelMarkets(boolean isCampaignStart) {
        PersonnelMarketStyle marketStyle = campaignOptions.getPersonnelMarketStyle();
        if (marketStyle == PERSONNEL_MARKET_DISABLED) {
            personnelMarket.generatePersonnelForDay(this);
        } else {
            if (currentDay.getDayOfMonth() == 1 || isCampaignStart) {
                newPersonnelMarket.gatherApplications();

                if (newPersonnelMarket.getHasRarePersonnel()) {
                    StringBuilder oocReport = new StringBuilder(resources.getString(
                          "personnelMarket.rareProfession.outOfCharacter"));
                    for (PersonnelRole profession : newPersonnelMarket.getRareProfessions()) {
                        oocReport.append("<p>- ").append(profession.getLabel(isClanCampaign())).append("</p>");
                    }

                    List<String> buttons = new ArrayList<>();
                    buttons.add(resources.getString("personnelMarket.rareProfession.button.later"));
                    buttons.add(resources.getString("personnelMarket.rareProfession.button.decline"));
                    // If the player attempts to jump to the personnel market, while the campaign is booting, they
                    // will get an NPE as the Campaign GUI won't have finished launching.
                    if (!isCampaignStart) {
                        buttons.add(resources.getString("personnelMarket.rareProfession.button.immediate"));
                    }

                    ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(this,
                          getSeniorAdminPerson(AdministratorSpecialization.HR),
                          null,
                          resources.getString("personnelMarket.rareProfession.inCharacter"),
                          buttons,
                          oocReport.toString(),
                          null,
                          true);

                    if (dialog.getDialogChoice() == 2) {
                        newPersonnelMarket.showPersonnelMarketDialog();
                    }
                }
            }
        }
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int bonus) {
        initiativeBonus = bonus;
    }

    public void applyInitiativeBonus(int bonus) {
        if (bonus > initiativeMaxBonus) {
            initiativeMaxBonus = bonus;
        }
        if ((bonus + initiativeBonus) > initiativeMaxBonus) {
            initiativeBonus = initiativeMaxBonus;
        } else {
            initiativeBonus += bonus;
        }
    }

    public void initiativeBonusIncrement(boolean change) {
        if (change) {
            setInitiativeBonus(++initiativeBonus);
        } else {
            setInitiativeBonus(--initiativeBonus);
        }
        if (initiativeBonus > initiativeMaxBonus) {
            initiativeBonus = initiativeMaxBonus;
        }
    }

    public int getInitiativeMaxBonus() {
        return initiativeMaxBonus;
    }

    public void setInitiativeMaxBonus(int bonus) {
        initiativeMaxBonus = bonus;
    }


    /**
     * Retrieves the flagged commander from the personnel list. If no flagged commander is found returns {@code null}.
     *
     * <p><b>Usage:</b> consider using {@link #getCommander()} instead.</p>
     *
     * @return the flagged commander if present, otherwise {@code null}
     */
    public @Nullable Person getFlaggedCommander() {
        return getPersonnel().stream().filter(Person::isCommander).findFirst().orElse(null);
    }

    public @Nullable Person getFlaggedSecondInCommand() {
        return getPersonnel().stream().filter(Person::isSecondInCommand).findFirst().orElse(null);
    }

    /**
     * Use {@link #getCommander()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public Person getSeniorCommander() {
        Person commander = null;
        for (Person person : getActivePersonnel(false, false)) {
            if (person.isCommander()) {
                return person;
            }
            if (null == commander || person.getRankNumeric() > commander.getRankNumeric()) {
                commander = person;
            }
        }
        return commander;
    }

    public void removeUnit(UUID id) {
        Unit unit = getHangar().getUnit(id);
        if (unit == null) {
            return;
        }

        // remove all parts for this unit as well
        for (Part p : unit.getParts()) {
            getWarehouse().removePart(p);
        }

        // remove any personnel from this unit
        for (Person person : unit.getCrew()) {
            unit.remove(person, true);
        }

        Person tech = unit.getTech();
        if (null != tech) {
            unit.remove(tech, true);
        }

        // remove unit from any forces
        removeUnitFromForce(unit);

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
        automatedMothballUnits.remove(unit.getId());

        // finally, remove the unit
        getHangar().removeUnit(unit.getId());

        checkDuplicateNamesDuringDelete(unit.getEntity());
        addReport(ACQUISITIONS, unit.getName() + " has been removed from the unit roster.");
        MekHQ.triggerEvent(new UnitRemovedEvent(unit));
    }

    public void removePerson(final @Nullable Person person) {
        removePerson(person, true);
    }

    public void removePerson(final @Nullable Person person, final boolean log) {
        if (person == null) {
            return;
        }


        Force force = getForceFor(person);
        if (force != null) {
            force.updateCommander(this);
        }

        person.getGenealogy().clearGenealogyLinks();

        final Unit unit = person.getUnit();
        if (unit != null) {
            unit.remove(person, true);
        }
        person.setDoctorId(null, 0);
        removeAllPatientsFor(person);
        person.removeAllTechJobs(this);
        removeKillsFor(person.getId());
        getRetirementDefectionTracker().removePerson(person);
        if (log) {
            addReport(PERSONNEL, person.getFullTitle() + " has been removed from the personnel roster.");
        }

        personnel.remove(person.getId());

        // Deal with Astech Pool Minutes
        if (person.isAstech()) {
            asTechPoolMinutes = max(0, asTechPoolMinutes - Person.PRIMARY_ROLE_SUPPORT_TIME);
            asTechPoolOvertime = max(0, asTechPoolOvertime - Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME);
        }
        MekHQ.triggerEvent(new PersonRemovedEvent(person));
    }

    public void removeAllPatientsFor(Person doctor) {
        for (Person person : getPersonnel()) {
            if (null != person.getDoctorId() && person.getDoctorId().equals(doctor.getId())) {
                person.setDoctorId(null, getCampaignOptions().getNaturalHealingWaitingPeriod());
            }
        }
    }

    public void removeScenario(final Scenario scenario) {
        scenario.clearAllForcesAndPersonnel(this);
        final Mission mission = getMission(scenario.getMissionId());
        if (mission != null) {
            mission.getScenarios().remove(scenario);

            // run through the StratCon campaign state where applicable and remove the
            // "parent" scenario as well
            if ((mission instanceof AtBContract) &&
                      (((AtBContract) mission).getStratconCampaignState() != null) &&
                      (scenario instanceof AtBDynamicScenario)) {
                ((AtBContract) mission).getStratconCampaignState().removeStratConScenario(scenario.getId());
            }
        }
        scenarios.remove(scenario.getId());
        MekHQ.triggerEvent(new ScenarioRemovedEvent(scenario));
    }

    public void removeMission(final Mission mission) {
        // Loop through scenarios here! We need to remove them as well.
        for (Scenario scenario : mission.getScenarios()) {
            scenario.clearAllForcesAndPersonnel(this);
            scenarios.remove(scenario.getId());
        }
        mission.clearScenarios();

        missions.remove(mission.getId());
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

    public void removeForce(Force force) {
        int fid = force.getId();
        forceIds.remove(fid);
        // clear forceIds of all personnel with this force
        for (UUID uid : force.getUnits()) {
            Unit u = getHangar().getUnit(uid);
            if (null == u) {
                continue;
            }
            if (u.getForceId() == fid) {
                u.setForceId(FORCE_NONE);
                if (force.isDeployed()) {
                    u.setScenarioId(NO_ASSIGNED_SCENARIO);
                }
            }
        }

        // also remove this force's id from any scenarios
        if (force.isDeployed()) {
            Scenario s = getScenario(force.getScenarioId());
            s.removeForce(fid);
        }

        if (null != force.getParentForce()) {
            force.getParentForce().removeSubForce(fid);
        }

        // clear out StratCon force assignments
        for (AtBContract contract : getActiveAtBContracts()) {
            if (contract.getStratconCampaignState() != null) {
                for (StratConTrackState track : contract.getStratconCampaignState().getTracks()) {
                    track.unassignForce(fid);
                }
            }
        }

        if (campaignOptions.isUseAtB()) {
            recalculateCombatTeams(this);
        }
    }

    public void removeUnitFromForce(Unit u) {
        Force force = getForce(u.getForceId());
        if (null != force) {
            force.removeUnit(this, u.getId(), true);
            u.setForceId(FORCE_NONE);
            u.setScenarioId(NO_ASSIGNED_SCENARIO);
            if (u.getEntity().hasNavalC3() && u.getEntity().calculateFreeC3Nodes() < 5) {
                Vector<Unit> removedUnits = new Vector<>();
                removedUnits.add(u);
                removeUnitsFromNetwork(removedUnits);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                refreshNetworks();
            } else if (u.getEntity().hasC3i() && u.getEntity().calculateFreeC3Nodes() < 5) {
                Vector<Unit> removedUnits = new Vector<>();
                removedUnits.add(u);
                removeUnitsFromNetwork(removedUnits);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                refreshNetworks();
            }
            if (u.getEntity().hasC3M()) {
                removeUnitsFromC3Master(u);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
            }

            if (campaignOptions.isUseAtB() && force.getUnits().isEmpty()) {
                combatTeams.remove(force.getId());
            }
        }
    }

    public @Nullable Force getForceFor(final @Nullable Unit unit) {
        return (unit == null) ? null : getForce(unit.getForceId());
    }

    public @Nullable Force getForceFor(final Person person) {
        final Unit unit = person.getUnit();
        if (unit != null) {
            return getForceFor(unit);
        } else if (person.isTech()) {
            return forceIds.values()
                         .stream()
                         .filter(force -> person.getId().equals(force.getTechID()))
                         .findFirst()
                         .orElse(null);
        }

        return null;
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
            getWarehouse().removePart(remove);
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

        shoppingList.restore();

        if (getCampaignOptions().isUseAtB()) {
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
        for (Person person : personnel.values()) {
            if (person.getGenealogy().hasSpouse()) {
                if (!personnel.containsKey(person.getGenealogy().getSpouse().getId())) {
                    person.getGenealogy().setSpouse(null);
                    person.setMaidenName(null);
                }
            }
        }

        // clean up non-existent unit references in force unit lists
        for (Force force : forceIds.values()) {
            List<UUID> orphanForceUnitIDs = new ArrayList<>();

            for (UUID unitID : force.getUnits()) {
                if (getHangar().getUnit(unitID) == null) {
                    orphanForceUnitIDs.add(unitID);
                }
            }

            for (UUID unitID : orphanForceUnitIDs) {
                force.removeUnit(this, unitID, false);
            }
        }

        // clean up units that are assigned to non-existing scenarios
        for (Unit unit : this.getUnits()) {
            if (this.getScenario(unit.getScenarioId()) == null) {
                unit.setScenarioId(Scenario.S_DEFAULT_ID);
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

    public Faction getFaction() {
        return faction;
    }

    /**
     * Determines whether the current campaign is a clan campaign.
     *
     * <p>This method checks if the faction associated with the campaign is a clan, returning {@code true}
     * if it is, and {@code false} otherwise.</p>
     *
     * @return {@code true} if the campaign belongs to a clan faction, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isClanCampaign() {
        return faction.isClan();
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
        return faction.getShortName().equals(PIRATE_FACTION_CODE);
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
        return faction.getShortName().equals(MERCENARY_FACTION_CODE);
    }

    public void setFaction(final Faction faction) {
        setFactionDirect(faction);
        updateTechFactionCode();
    }

    public void setFactionDirect(final Faction faction) {
        this.faction = faction;
    }

    public String getRetainerEmployerCode() {
        return retainerEmployerCode;
    }

    public void setRetainerEmployerCode(String code) {
        retainerEmployerCode = code;
    }

    public LocalDate getRetainerStartDate() {
        return retainerStartDate;
    }

    public void setRetainerStartDate(LocalDate retainerStartDate) {
        this.retainerStartDate = retainerStartDate;
    }

    public int getRawCrimeRating() {
        return crimeRating;
    }

    public void setCrimeRating(int crimeRating) {
        this.crimeRating = crimeRating;
    }

    /**
     * Updates the crime rating by the specified change. If improving crime rating, use a positive number, otherwise
     * negative
     *
     * @param change the change to be applied to the crime rating
     */
    public void changeCrimeRating(int change) {
        this.crimeRating = Math.min(0, crimeRating + change);
    }

    public int getCrimePirateModifier() {
        return crimePirateModifier;
    }

    public void setCrimePirateModifier(int crimePirateModifier) {
        this.crimePirateModifier = crimePirateModifier;
    }

    /**
     * Updates the crime pirate modifier by the specified change. If improving the modifier, use a positive number,
     * otherwise negative
     *
     * @param change the change to be applied to the crime modifier
     */
    public void changeCrimePirateModifier(int change) {
        this.crimePirateModifier = Math.min(0, crimePirateModifier + change);
    }

    /**
     * Calculates the adjusted crime rating by adding the crime rating with the pirate modifier.
     *
     * @return The adjusted crime rating.
     */
    public int getAdjustedCrimeRating() {
        return crimeRating + crimePirateModifier;
    }

    public @Nullable LocalDate getDateOfLastCrime() {
        return dateOfLastCrime;
    }

    public void setDateOfLastCrime(LocalDate dateOfLastCrime) {
        this.dateOfLastCrime = dateOfLastCrime;
    }

    public ReputationController getReputation() {
        return reputation;
    }

    public void setReputation(ReputationController reputation) {
        this.reputation = reputation;
    }

    public FactionStandings getFactionStandings() {
        return factionStandings;
    }

    public void setFactionStandings(FactionStandings factionStandings) {
        this.factionStandings = factionStandings;
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
    }

    private void addReportInternal(final DailyReportType type, final String report) {
        switch (type) {
            case GENERAL -> {
                currentReport.add(report);
                if (!currentReportHTML.isEmpty()) {
                    currentReportHTML = currentReportHTML + REPORT_LINEBREAK + report;
                    newReports.add(REPORT_LINEBREAK);
                } else {
                    currentReportHTML = report;
                }

                newReports.add(report);
            }
            case SKILL_CHECKS -> {
                skillReport.add(report);
                if (!skillReportHTML.isEmpty()) {
                    skillReportHTML = skillReportHTML + REPORT_LINEBREAK + report;
                    newSkillReports.add(REPORT_LINEBREAK);
                } else {
                    skillReportHTML = report;
                }

                newSkillReports.add(report);
            }
            case TECHNICAL -> {
                technicalReport.add(report);
                if (!technicalReportHTML.isEmpty()) {
                    technicalReportHTML = technicalReportHTML + REPORT_LINEBREAK + report;
                    newTechnicalReports.add(REPORT_LINEBREAK);
                } else {
                    technicalReportHTML = report;
                }

                newTechnicalReports.add(report);
            }
            case FINANCES -> {
                financesReport.add(report);
                if (!financesReportHTML.isEmpty()) {
                    financesReportHTML = financesReportHTML + REPORT_LINEBREAK + report;
                    newFinancesReports.add(REPORT_LINEBREAK);
                } else {
                    financesReportHTML = report;
                }

                newFinancesReports.add(report);
            }
            case ACQUISITIONS -> {
                acquisitionsReport.add(report);
                if (!acquisitionsReportHTML.isEmpty()) {
                    acquisitionsReportHTML = acquisitionsReportHTML + REPORT_LINEBREAK + report;
                    newAcquisitionsReports.add(REPORT_LINEBREAK);
                } else {
                    acquisitionsReportHTML = report;
                }

                newAcquisitionsReports.add(report);
            }
            case MEDICAL -> {
                medicalReport.add(report);
                if (!medicalReportHTML.isEmpty()) {
                    medicalReportHTML = medicalReportHTML + REPORT_LINEBREAK + report;
                    newMedicalReports.add(REPORT_LINEBREAK);
                } else {
                    medicalReportHTML = report;
                }

                newMedicalReports.add(report);
            }
            case PERSONNEL -> {
                personnelReport.add(report);
                if (!personnelReportHTML.isEmpty()) {
                    personnelReportHTML = personnelReportHTML + REPORT_LINEBREAK + report;
                    newPersonnelReports.add(REPORT_LINEBREAK);
                } else {
                    personnelReportHTML = report;
                }

                newPersonnelReports.add(report);
            }
            case BATTLE -> {
                battleReport.add(report);
                if (!battleReportHTML.isEmpty()) {
                    battleReportHTML = battleReportHTML + REPORT_LINEBREAK + report;
                    newBattleReports.add(REPORT_LINEBREAK);
                } else {
                    battleReportHTML = report;
                }

                newBattleReports.add(report);
            }
            case POLITICS -> {
                politicsReport.add(report);
                if (!politicsReportHTML.isEmpty()) {
                    politicsReportHTML = politicsReportHTML + REPORT_LINEBREAK + report;
                    newPoliticsReports.add(REPORT_LINEBREAK);
                } else {
                    politicsReportHTML = report;
                }

                newPoliticsReports.add(report);
            }
        }
        MekHQ.triggerEvent(new ReportEvent(this, report));
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public void setCamouflage(final Camouflage camouflage) {
        this.camouflage = camouflage;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(final PlayerColour colour) {
        this.colour = Objects.requireNonNull(colour, "Colour cannot be set to null");
    }

    public StandardForceIcon getUnitIcon() {
        return unitIcon;
    }

    public void setUnitIcon(final StandardForceIcon unitIcon) {
        this.unitIcon = unitIcon;
    }

    public void addFunds(final TransactionType type, final Money quantity, @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }

        finances.credit(type, getLocalDate(), quantity, description);
        String quantityString = quantity.toAmountAndSymbolString();
        addReport(FINANCES, "Funds added : " + quantityString + " (" + description + ')');
    }

    public void removeFunds(final TransactionType type, final Money quantity, @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }

        finances.debit(type, getLocalDate(), quantity, description);
        String quantityString = quantity.toAmountAndSymbolString();
        addReport(FINANCES, "Funds removed : " + quantityString + " (" + description + ')');
    }

    /**
     * Generic method for paying Personnel (Person) in the company. Debits money from the campaign and if the campaign
     * tracks total earnings it will account for that.
     *
     * @param type              TransactionType being debited
     * @param quantity          total money - it's usually displayed outside of this method
     * @param description       String displayed in the ledger and report
     * @param individualPayouts Map of Person to the Money they're owed
     */
    public void payPersonnel(TransactionType type, Money quantity, String description,
          Map<Person, Money> individualPayouts) {
        getFinances().debit(type,
              getLocalDate(),
              quantity,
              description,
              individualPayouts,
              getCampaignOptions().isTrackTotalEarnings());
        String quantityString = quantity.toAmountAndSymbolString();
        addReport(FINANCES, "Funds removed : " + quantityString + " (" + description + ')');

    }

    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public void setCampaignOptions(CampaignOptions options) {
        campaignOptions = options;
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

    @Deprecated(since = "0.50.07", forRemoval = true)
    public FameAndInfamyController getFameAndInfamy() {
        return null;
    }

    /**
     * Retrieves the list of units that are configured for automated mothballing.
     *
     * <p>
     * Automated mothballing is a mechanism where certain units are automatically placed into a mothballed state,
     * reducing their active maintenance costs and operational demands over time.
     * </p>
     *
     * @return A {@link List} of {@link UUID} objects that are set for automated mothballing. Returns an empty list if
     *       no units are configured.
     */
    public List<UUID> getAutomatedMothballUnits() {
        return automatedMothballUnits;
    }

    /**
     * Sets the list of units that are configured for automated mothballing.
     *
     * <p>
     * Replaces the current list of units that have undergone automated mothballing.
     * </p>
     *
     * @param automatedMothballUnits A {@link List} of {@link UUID} objects to configure for automated mothballing.
     */
    public void setAutomatedMothballUnits(List<UUID> automatedMothballUnits) {
        this.automatedMothballUnits = automatedMothballUnits;
    }

    public int getTemporaryPrisonerCapacity() {
        return temporaryPrisonerCapacity;
    }

    public void setTemporaryPrisonerCapacity(int temporaryPrisonerCapacity) {
        this.temporaryPrisonerCapacity = max(MINIMUM_TEMPORARY_CAPACITY, temporaryPrisonerCapacity);
    }

    /**
     * Adjusts the temporary prisoner capacity by the specified delta value.
     *
     * <p>the new capacity is constrained to be at least the minimum allowed temporary capacity, as defined by {@code
     * PrisonerEventManager.MINIMUM_TEMPORARY_CAPACITY}.</p>T
     *
     * @param delta the amount by which to change the temporary prisoner capacity. A positive value increases the
     *              capacity, while a negative value decreases it.
     */
    public void changeTemporaryPrisonerCapacity(int delta) {
        int newCapacity = temporaryPrisonerCapacity + delta;
        temporaryPrisonerCapacity = max(MINIMUM_TEMPORARY_CAPACITY, newCapacity);
    }

    public RandomEventLibraries getRandomEventLibraries() {
        return randomEventLibraries;
    }

    public FactionStandingUltimatumsLibrary getFactionStandingUltimatumsLibrary() {
        return factionStandingUltimatumsLibrary;
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
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "faction", getFaction().getShortName());
        if (retainerEmployerCode != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "retainerEmployerCode", retainerEmployerCode);
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "retainerStartDate", retainerStartDate);
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "crimeRating", crimeRating);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "crimePirateModifier", crimePirateModifier);

        if (dateOfLastCrime != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "dateOfLastCrime", dateOfLastCrime);
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "reputation");
        reputation.writeReputationToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "reputation");
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "newPersonnelMarket");
        newPersonnelMarket.writePersonnelMarketDataToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "newPersonnelMarket");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionStandings");
        factionStandings.writeFactionStandingsToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionStandings");

        // this handles campaigns that predate 49.20
        if (campaignStartDate == null) {
            setCampaignStartDate(getLocalDate());
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "campaignStartDate", getCampaignStartDate());

        getRankSystem().writeToXML(writer, indent, false);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "overtime", overtime);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "gmMode", gmMode);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPool", asTechPool);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPoolMinutes", asTechPoolMinutes);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPoolOvertime", asTechPoolOvertime);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "medicPool", medicPool);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "fieldKitchenWithinCapacity", fieldKitchenWithinCapacity);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "mashTheatreCapacity", mashTheatreCapacity);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "repairBaysRented", repairBaysRented);
        getCamouflage().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "colour", getColour().name());
        getUnitIcon().writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastForceId", lastForceId);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastMissionId", lastMissionId);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastScenarioId", lastScenarioId);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "initiativeBonus", initiativeBonus);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "initiativeMaxBonus", initiativeMaxBonus);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "nameGen");
        MHQXMLUtility.writeSimpleXMLTag(writer,
              indent,
              "faction",
              RandomNameGenerator.getInstance().getChosenFaction());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "percentFemale", RandomGenderGenerator.getPercentFemale());
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "nameGen");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "currentReport");
        for (String report : currentReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "currentReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "skillReport");
        for (String report : skillReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "skillReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "technicalReport");
        for (String report : technicalReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "technicalReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "financesReport");
        for (String report : financesReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "financesReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "acquisitionsReport");
        for (String report : acquisitionsReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "acquisitionsReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "medicalReport");
        for (String report : medicalReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "medicalReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "personnelReport");
        for (String report : personnelReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "personnelReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "battleReport");
        for (String report : battleReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "battleReport");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "politicsReport");
        for (String report : politicsReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + report + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "politicsReport");

        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "info");
        // endregion Basic Campaign Info

        // region Options
        if (getCampaignOptions() != null) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(getCampaignOptions(), writer, indent);
        }
        getGameOptions().writeToXML(writer, indent);
        // endregion Options

        // Lists of objects:
        units.writeToXML(writer, indent, "units"); // Units

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "personnel");
        for (final Person person : getPersonnel()) {
            person.writeToXML(writer, indent, this);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "personnel");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "missions");
        for (final Mission mission : getMissions()) {
            mission.writeToXML(this, writer, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "missions");

        // the forces structure is hierarchical, but that should be handled
        // internally from with writeToXML function for Force
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "forces");
        forces.writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "forces");
        finances.writeToXML(writer, indent);
        location.writeToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "isAvoidingEmptySystems", isAvoidingEmptySystems);
        MHQXMLUtility.writeSimpleXMLTag(writer,
              indent,
              "isOverridingCommandCircuitRequirements",
              isOverridingCommandCircuitRequirements);
        shoppingList.writeToXML(writer, indent);
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
        parts.writeToXML(writer, indent, "parts"); // Parts

        // current story arc
        if (null != storyArc) {
            storyArc.writeToXml(writer, indent);
        }

        // Markets
        getPersonnelMarket().writeToXML(writer, indent, this);

        // TODO : AbstractContractMarket : Uncomment
        // CAW: implicit DEPENDS-ON to the <missions> and <campaignOptions> node, do not
        // move this above it
        // getContractMarket().writeToXML(pw, indent);

        // Windchild: implicit DEPENDS-ON to the <campaignOptions> node, do not move
        // this above it
        getUnitMarket().writeToXML(writer, indent);

        // Against the Bot
        if (getCampaignOptions().isUseAtB()) {
            // TODO : AbstractContractMarket : Remove next two lines
            // CAW: implicit DEPENDS-ON to the <missions> node, do not move this above it
            contractMarket.writeToXML(this, writer, indent);

            if (!combatTeams.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "combatTeams");
                for (CombatTeam combatTeam : combatTeams.values()) {
                    if (forceIds.containsKey(combatTeam.getForceId())) {
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

        retirementDefectionTracker.writeToXML(writer, indent);

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "personnelWhoAdvancedInXP");
        for (Person person : personnelWhoAdvancedInXP) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "personWhoAdvancedInXP", person.getId());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "personnelWhoAdvancedInXP");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "automatedMothballUnits");
        for (UUID unitId : automatedMothballUnits) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "mothballedUnit", unitId);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "automatedMothballUnits");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "temporaryPrisonerCapacity", temporaryPrisonerCapacity);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "processProcurement", processProcurement);

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, ++indent, "partsInUse");
        writePartInUseToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "partsInUse");

        if (isBugReportPrep || MekHQ.getMHQOptions().getWriteCustomsToXML()) {
            writeCustoms(writer, isBugReportPrep);
        }

        // Okay, we're done.
        // Close everything out and be done with it.
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "campaign");
    }

    /**
     * Writes serialized custom unit definitions to the provided {@link PrintWriter}.
     *
     * <p>When invoked for bug report preparation, this method scans all units currently present in the campaign,
     * extracts their raw short names, and treats them as custom entries to be exported. Each candidate name is resolved
     * via the {@link MekSummaryCache}; if a definition is found, the source entity is parsed and serialized into
     * XML.</p>
     *
     * <p>BattleMeks are exported using embedded MTF data wrapped in CDATA; all other supported entity types are
     * exported as BLK content, line-by-line and wrapped in CDATA.</p>
     *
     * <p>Units that cannot be located in the cache or that fail parsing are skipped, with errors logged. The
     * ordering of exported units depends on the underlying {@link Set} implementation.</p>
     *
     * <p><b>Note:</b> When {@code isBugReportPrep} is {@code false}, this method replaces the custom set.</p>
     *
     * @param printWriter     the output writer used to emit formatted {@code <custom>} elements
     * @param isBugReportPrep whether campaign unit names should be collected for export; if {@code false}, no custom
     *                        entities will be written by this method
     */
    private void writeCustoms(PrintWriter printWriter, boolean isBugReportPrep) {
        Set<String> customUnits = new HashSet<>();
        if (isBugReportPrep) {
            for (Unit unit : units.getUnits()) {
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
        } else {
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

            if (mekFileParser == null) {
                LOGGER.warn("mekFileParser was null for {}", name);
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
    public RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final @Nullable RankSystem rankSystem) {
        // If they are the same object, there hasn't been a change and thus don't need
        // to process further
        if (Objects.equals(getRankSystem(), rankSystem)) {
            return;
        }

        // Then, we need to validate the rank system. Null isn't valid to be set but may
        // be the
        // result of a cancelled load. However, validation will prevent that
        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return;
        }

        // We need to know the old campaign rank system for personnel processing
        final RankSystem oldRankSystem = getRankSystem();

        // And with that, we can set the rank system
        setRankSystemDirect(rankSystem);

        // Finally, we fix all personnel ranks and ensure they are properly set
        getPersonnel().stream()
              .filter(person -> person.getRankSystem().equals(oldRankSystem))
              .forEach(person -> person.setRankSystem(rankValidator, rankSystem));
    }

    public void setRankSystemDirect(final RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }
    // endregion Ranks

    public void setFinances(Finances f) {
        finances = f;
    }

    public Finances getFinances() {
        return finances;
    }

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
        if (!skipEmptySystemCheck
                  && isAvoidingEmptySystems
                  && end.getPopulation(currentDay) == 0) {
            new ImmersiveDialogSimple(this, getSeniorAdminPerson(AdministratorSpecialization.TRANSPORT), null,
                  String.format(resources.getString("unableToEnterSystem.abandoned.ic"), getCommanderAddress()),
                  null, resources.getString("unableToEnterSystem.abandoned.ooc"), null, false);

            return new JumpPath();
        }

        List<AtBContract> activeAtBContracts = getActiveAtBContracts();

        FactionHints factionHints = FactionHints.getInstance();
        if (!skipAccessCheck && campaignOptions.isUseFactionStandingOutlawedSafe()) {
            boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(faction, factionStandings,
                  start, end, currentDay, activeAtBContracts, factionHints);
            if (!canAccessSystem) {
                new ImmersiveDialogSimple(this, getSeniorAdminPerson(AdministratorSpecialization.TRANSPORT), null,
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
        boolean isEscapingOutlawing = !FactionStandingUtilities.canEnterTargetSystem(faction, factionStandings,
              null, start, currentDay, activeAtBContracts, factionHints);

        // A* search
        final int MAX_JUMPS = 10000;
        for (int jumps = 0; jumps < MAX_JUMPS; jumps++) {
            PlanetarySystem currentSystem = systemsInstance.getSystemById(current);

            boolean isUseCommandCircuits =
                  FactionStandingUtilities.isUseCommandCircuit(isOverridingCommandCircuitRequirements, gmMode,
                        campaignOptions.isUseFactionStandingCommandCircuitSafe(),
                        factionStandings,
                        getFutureAtBContracts());

            // Get current node's information
            double currentG = scoreG.get(current) + currentSystem.getRechargeTime(getLocalDate(), isUseCommandCircuits);
            final String localCurrent = current;

            // Explore neighbors
            systemsInstance.visitNearbySystems(currentSystem, 30, neighborSystem -> {
                String neighborId = neighborSystem.getId();

                // Skip systems without population if avoiding empty systems
                if (!skipEmptySystemCheck
                          && isAvoidingEmptySystems
                          && neighborSystem.getPopulation(currentDay) == 0) {
                    return;
                }

                // Skip systems where the campaign is outlawed
                if (!skipAccessCheck &&
                          !isEscapingOutlawing &&
                          campaignOptions.isUseFactionStandingOutlawedSafe()) {
                    boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(faction, factionStandings,
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

        Person negotiator = getSeniorAdminPerson(AdministratorSpecialization.TRANSPORT);
        if (negotiator != null) {
            PersonnelOptions options = negotiator.getOptions();
            if (options.booleanOption(ADMIN_INTERSTELLAR_NEGOTIATOR) && totalCost.isPositive()) {
                totalCost = totalCost.multipliedBy(0.85);
            }
        }

        return totalCost;
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

    public void personUpdated(Person person) {
        Unit u = person.getUnit();
        if (null != u) {
            u.resetPilotAndEntity();
        }

        Force force = getForceFor(person);
        if (force != null) {
            force.updateCommander(this);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(person));
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
        } else if (!getCampaignOptions().isDestroyByMargin() && (partWork.getSkillMin() > effectiveSkillLevel)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is beyond this tech's skill level");
        } else if (partWork.getSkillMin() > SkillType.EXP_LEGENDARY) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
        } else if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
        } else if ((partWork instanceof MissingPart) && (((MissingPart) partWork).findReplacement(false) == null)) {
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

        // If we are using the MoF rule, then we will ignore mode penalty here
        // and instead assign it as a straight penalty
        if (getCampaignOptions().isDestroyByMargin()) {
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

        if (getCampaignOptions().isUseEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        final boolean isOvertime;
        if (isOvertimeAllowed() && (tech.isTaskOvertime(partWork) || partWork.hasWorkedOvertime())) {
            target.addModifier(3, "overtime");
            isOvertime = true;
        } else {
            isOvertime = false;
        }

        final int minutes = Math.min(partWork.getTimeLeft(), techTime);
        if (minutes <= 0) {
            LOGGER.error("Attempting to get the target number for a part with zero time left.");
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "No part repair time remaining.");
        }

        int helpMod;
        if ((partWork.getUnit() != null) && partWork.getUnit().isSelfCrewed()) {
            helpMod = getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
        } else {
            final int helpers = getAvailableAsTechs(minutes, isOvertime);
            helpMod = getShorthandedMod(helpers, false);
            // we may have just gone overtime with our helpers
            if (!isOvertime && (asTechPoolMinutes < (minutes * helpers))) {
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
     * Calculates the target roll for acquiring the specified item or unit using the default campaign logistics person,
     * applying all standard campaign rules and options.
     *
     * @param acquisition the {@link IAcquisitionWork} describing the part, supply, or unit to be acquired
     *
     * @return a {@link TargetRoll} indicating the outcome or difficulty of the acquisition attempt
     */
    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition) {
        return getTargetForAcquisition(acquisition, getLogisticsPerson());
    }

    /**
     * Calculates the target roll for acquiring the specified item or unit with the given person, using default campaign
     * settings for other options.
     *
     * @param acquisition the {@link IAcquisitionWork} describing the part, supply, or unit to be acquired
     * @param person      the {@link Person} to attempt the acquisition, or {@code null} if unavailable
     *
     * @return a {@link TargetRoll} indicating the outcome or difficulty of the acquisition attempt
     */
    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition, final @Nullable Person person) {
        return getTargetForAcquisition(acquisition, person, false, false);
    }

    /**
     * Calculates the target roll for acquiring the specified item or unit while optionally ignoring real acquisitions
     * personnel. A synthetic person with baseline skill is used if personnel are ignored.
     *
     * @param acquisition                 the {@link IAcquisitionWork} describing the part, supply, or unit to be
     *                                    acquired
     * @param ignoreAcquisitionsPersonnel if {@code true}, ignores available personnel and uses a synthetic baseline
     *                                    person for the roll
     *
     * @return a {@link TargetRoll} indicating the outcome or difficulty of the acquisition attempt
     */
    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition,
          final boolean ignoreAcquisitionsPersonnel) {
        return getTargetForAcquisition(acquisition, null, false, ignoreAcquisitionsPersonnel);
    }

    public PlanetaryConditions getCurrentPlanetaryConditions(Scenario scenario) {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        if (scenario instanceof AtBScenario atBScenario) {
            if (getCampaignOptions().isUseLightConditions()) {
                planetaryConditions.setLight(atBScenario.getLight());
            }
            if (getCampaignOptions().isUseWeatherConditions()) {
                planetaryConditions.setWeather(atBScenario.getWeather());
                planetaryConditions.setWind(atBScenario.getWind());
                planetaryConditions.setFog(atBScenario.getFog());
                planetaryConditions.setEMI(atBScenario.getEMI());
                planetaryConditions.setBlowingSand(atBScenario.getBlowingSand());
                planetaryConditions.setTemperature(atBScenario.getModifiedTemperature());

            }
            if (getCampaignOptions().isUsePlanetaryConditions()) {
                planetaryConditions.setAtmosphere(atBScenario.getAtmosphere());
                planetaryConditions.setGravity(atBScenario.getGravity());
            }
        } else {
            planetaryConditions = scenario.createPlanetaryConditions();
        }

        return planetaryConditions;

    }

    /**
     * Calculates the target roll required for successfully acquiring a specific part or unit, factoring in campaign
     * options, acquisition details, the person attempting the acquisition, and whether acquisitions personnel should be
     * ignored.
     *
     * <p>This method evaluates a sequence of rules and conditions to determine whether the acquisition is possible,
     * impossible, automatically successful, or automatically fails for the period due to cooldowns. Otherwise, it
     * computes the target roll value based on the skill of the assigned (real or synthetic) person and all relevant
     * modifiers such as item attributes, availability, campaign configuration (including AtB and "Gray Monday"
     * effects), technical year, and extinction.</p>
     *
     * <p>The possible results are:</p>
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
     * @param acquisition                 an {@link IAcquisitionWork} object describing the item or unit being requested
     *                                    (contains info such as tech base, tech level, and availability)
     * @param person                      the {@link Person} assigned to make the acquisition roll; may be {@code null}
     *                                    if no one is available/allowed, or if personnel are ignored
     * @param checkDaysToWait             if {@code true}, checks for shopping list/cooldown period before allowing the
     *                                    roll
     * @param ignoreAcquisitionsPersonnel if {@code true}, constructs a synthetic person with default skill for the
     *                                    roll, ignoring actual acquisitions personnel and their availability
     *
     * @return a {@link TargetRoll} describing the acquisition result, either as a constant value
     *       (automatic/impossible/fail) or a calculated result reflecting all applicable rules and modifiers
     */
    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition, @Nullable Person person,
          final boolean checkDaysToWait, final boolean ignoreAcquisitionsPersonnel) {
        if (getCampaignOptions().getAcquisitionType() == AcquisitionsType.AUTOMATIC) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "Automatic Success");
        }

        final AcquisitionsType acquisitionsType = campaignOptions.getAcquisitionType();
        String fixedSkillName = "";
        boolean isAnyTech = false;

        switch (acquisitionsType) {
            case ADMINISTRATION -> fixedSkillName = S_ADMIN;
            case ANY_TECH -> isAnyTech = true;
            case AUTOMATIC -> {
                return null;
            }
            case NEGOTIATION -> fixedSkillName = S_NEGOTIATION;
        }

        if (ignoreAcquisitionsPersonnel) {
            person = new Person(this);
            fixedSkillName = acquisitionsType == AcquisitionsType.ANY_TECH ? S_TECH_MECHANIC : fixedSkillName;
            SkillType skillType = getType(fixedSkillName);
            if (skillType != null) {
                int regularLevel = skillType.getRegularLevel();
                person.addSkill(fixedSkillName, regularLevel, 0);
            }
        }

        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                  "Your procurement personnel have used up all their acquisition attempts for this period");
        }

        final Skill skill = isAnyTech ? person.getBestTechSkill() : person.getSkillForWorkingOn(fixedSkillName);
        if (skill == null) {
            return new TargetRoll(TargetRoll.AUTOMATIC_FAIL, "No skill");
        }
        if (null != getShoppingList().getShoppingItem(acquisition.getNewEquipment()) && checkDaysToWait) {
            return new TargetRoll(TargetRoll.AUTOMATIC_FAIL,
                  "You must wait until the new cycle to check for this part. Further" +
                        " attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == TechBase.CLAN && !getCampaignOptions().isAllowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == TechBase.IS && !getCampaignOptions().isAllowISPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "You cannot acquire inner sphere parts");
        }
        if (getCampaignOptions().getTechLevel() < Utilities.getSimpleTechLevel(acquisition.getTechLevel())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "You cannot acquire parts of this tech level");
        }
        if (getCampaignOptions().isLimitByYear() &&
                  !acquisition.isIntroducedBy(getGameYear(), useClanTechBase(), getTechFaction())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "It has not been invented yet!");
        }
        if (getCampaignOptions().isDisallowExtinctStuff() &&
                  (acquisition.isExtinctIn(getGameYear(), useClanTechBase(), getTechFaction()) ||
                         acquisition.getAvailability().equals(AvailabilityValue.X))) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "It is extinct!");
        }

        SkillModifierData skillModifierData = person.getSkillModifierData(campaignOptions.isUseAgeEffects(),
              isClanCampaign(), currentDay);

        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(skillModifierData),
              skill.getSkillLevel(skillModifierData).toString());
        target.append(acquisition.getAllAcquisitionMods());

        if (getCampaignOptions().isUseAtB() && getCampaignOptions().isRestrictPartsByMission()) {
            int contractAvailability = findAtBPartsAvailabilityLevel();

            if (contractAvailability != 0) {
                target.addModifier(contractAvailability, "Contract");
            }
        }

        if (isGrayMonday(currentDay, campaignOptions.isSimulateGrayMonday())) {
            target.addModifier(4, "Gray Monday");
        }

        return target;
    }

    public int findAtBPartsAvailabilityLevel() {
        Integer availabilityModifier = null;
        for (AtBContract contract : getActiveAtBContracts()) {
            int contractAvailability = contract.getPartsAvailabilityLevel();

            if (availabilityModifier == null || contractAvailability < availabilityModifier) {
                availabilityModifier = contractAvailability;
            }
        }

        return Objects.requireNonNullElse(availabilityModifier, 0);
    }

    public void resetAsTechMinutes() {
        asTechPoolMinutes = Person.PRIMARY_ROLE_SUPPORT_TIME * getNumberAsTechs();
        asTechPoolOvertime = Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberAsTechs();
    }

    public void setAsTechPoolMinutes(int minutes) {
        asTechPoolMinutes = minutes;
    }

    public int getAsTechPoolMinutes() {
        return asTechPoolMinutes;
    }

    public void setAsTechPoolOvertime(int overtime) {
        asTechPoolOvertime = overtime;
    }

    public int getAsTechPoolOvertime() {
        return asTechPoolOvertime;
    }

    public int getPossibleAsTechPoolMinutes() {
        return 480 * getNumberPrimaryAsTechs() + 240 * getNumberSecondaryAsTechs();
    }

    public int getPossibleAsTechPoolOvertime() {
        return 240 * getNumberPrimaryAsTechs() + 120 * getNumberSecondaryAsTechs();
    }

    public void setAsTechPool(int size) {
        asTechPool = size;
    }

    /** @deprecated no longer in use **/
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getAsTechPool() {
        return getTemporaryAsTechPool();
    }

    public int getTemporaryAsTechPool() {
        return asTechPool;
    }

    public void setMedicPool(int size) {
        medicPool = size;
    }

    /** @deprecated no longer in use **/
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getMedicPool() {
        return getTemporaryMedicPool();
    }

    public int getTemporaryMedicPool() {
        return medicPool;
    }

    public boolean requiresAdditionalAsTechs() {
        return getAsTechNeed() > 0;
    }

    public int getAsTechNeed() {
        return (Math.toIntExact(getActivePersonnel(false, false).stream().filter(Person::isTech).count()) *
                      MHQConstants.AS_TECH_TEAM_SIZE) -
                     getNumberAsTechs();
    }

    public void increaseAsTechPool(int i) {
        asTechPool += i;
        asTechPoolMinutes += (480 * i);
        asTechPoolOvertime += (240 * i);
        MekHQ.triggerEvent(new AsTechPoolChangedEvent(this, i));
    }

    public void resetAsTechPool() {
        emptyAsTechPool();
        fillAsTechPool();
    }

    public void emptyAsTechPool() {
        final int currentAsTechs = getTemporaryAsTechPool();
        decreaseAsTechPool(currentAsTechs);
    }

    public void fillAsTechPool() {
        final int need = getAsTechNeed();
        if (need > 0) {
            increaseAsTechPool(need);
        }
    }

    public void decreaseAsTechPool(int i) {
        asTechPool = max(0, asTechPool - i);
        // always assume that we fire the ones who have not yet worked
        asTechPoolMinutes = max(0, asTechPoolMinutes - 480 * i);
        asTechPoolOvertime = max(0, asTechPoolOvertime - 240 * i);
        MekHQ.triggerEvent(new AsTechPoolChangedEvent(this, -i));
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
        boolean isUseUsefulAsTechs = getCampaignOptions().isUseUsefulAsTechs();

        int asTechs = getTemporaryAsTechPool();

        for (Person person : getActivePersonnel(false, false)) {
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
        boolean isUseUsefulAsTechs = getCampaignOptions().isUseUsefulAsTechs();

        int asTechs = 0;

        for (Person person : getActivePersonnel(false, false)) {
            if (person.getSecondaryRole().isAstech() && !person.isDeployed() && person.isEmployed()) {
                // All skilled assistants contribute 1 to the pool, regardless of skill level
                asTechs++;

                // They then contribution additional 'assistants' to the pool based on their skill level
                asTechs += isUseUsefulAsTechs ? getAdvancedAsTechContribution(person) : 0;
            }
        }

        return asTechs;
    }

    public int getAvailableAsTechs(final int minutes, final boolean alreadyOvertime) {
        if (minutes == 0) {
            // If 0 AsTechs are assigned to the task, return 0 minutes used
            return 0;
        }

        int availableHelp = (int) floor(((double) asTechPoolMinutes) / minutes);
        if (isOvertimeAllowed() && (availableHelp < MHQConstants.AS_TECH_TEAM_SIZE)) {
            // if we are less than fully staffed, then determine whether
            // we should dip into overtime or just continue as short-staffed
            final int shortMod = getShorthandedMod(availableHelp, false);
            final int remainingMinutes = asTechPoolMinutes - availableHelp * minutes;
            final int extraHelp = (remainingMinutes + asTechPoolOvertime) / minutes;
            final int helpNeeded = MHQConstants.AS_TECH_TEAM_SIZE - availableHelp;
            if (alreadyOvertime && (shortMod > 0)) {
                // then add whatever we can
                availableHelp += extraHelp;
            } else if (shortMod > 3) {
                // only dip in if we can bring ourselves up to full
                if (extraHelp >= helpNeeded) {
                    availableHelp = MHQConstants.AS_TECH_TEAM_SIZE;
                }
            }
        }
        return Math.min(Math.min(availableHelp, MHQConstants.AS_TECH_TEAM_SIZE), getNumberAsTechs());
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
        int numDocs = getDoctors().size();
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
        return getTemporaryMedicPool() + permanentMedicPool;
    }

    /**
     * Calculates the total number of medics available in the campaign by summing the skill levels in the
     * {@link SkillType#S_MEDTECH} skill for all eligible personnel.
     *
     * <p>Eligible personnel must have either a primary or secondary role as a medic, must not be currently deployed,
     * and must be employed.</p>
     *
     * <p></p>For each eligible person, their total skill level in {@link SkillType#S_MEDTECH} (including all
     * modifiers) is added to the running total.</p>
     *
     * @return The total number of medics available.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getPermanentMedicPool() {
        final boolean isUseUsefulMedics = getCampaignOptions().isUseUsefulMedics();
        int permanentMedicPool = 0;

        for (Person person : getActivePersonnel(false, false)) {
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

    public boolean requiresAdditionalMedics() {
        return getMedicsNeed() > 0;
    }

    public int getMedicsNeed() {
        return (getDoctors().size() * 4) - getNumberMedics();
    }

    public void increaseMedicPool(int i) {
        medicPool += i;
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, i));
    }

    public void resetMedicPool() {
        emptyMedicPool();
        fillMedicPool();
    }

    public void emptyMedicPool() {
        final int currentMedicPool = getTemporaryMedicPool();
        decreaseMedicPool(currentMedicPool);
    }

    public void fillMedicPool() {
        final int need = getMedicsNeed();
        if (need > 0) {
            increaseMedicPool(need);
        }
    }

    public void decreaseMedicPool(int i) {
        medicPool = max(0, medicPool - i);
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, -i));
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

        if ((getCampaignOptions().getKillsForXP() > 0) && (getCampaignOptions().getKillXPAward() > 0)) {
            if ((getKillsFor(k.getPilotId()).size() % getCampaignOptions().getKillsForXP()) == 0) {
                Person person = getPerson(k.getPilotId());
                if (null != person) {
                    person.awardXP(this, getCampaignOptions().getKillXPAward());
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

    public void setPartsStore(PartsStore partsStore) {
        this.partsStore = partsStore;
        this.partsStore.stock(this);
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
        return String.valueOf(reputation.getReputationRating());
    }

    /**
     * Retrieves the unit rating modifier based on campaign options.
     *
     * @return The unit rating modifier based on the campaign options.
     */
    public int getAtBUnitRatingMod() {
        return reputation.getAtbModifier();
    }

    /**
     * Returns the Strategy skill of the designated commander in the campaign.
     *
     * @return The value of the commander's strategy skill if a commander exists, otherwise 0.
     */
    public int getCommanderStrategy() {
        int commanderStrategy = 0;
        Person commander = getCommander();

        if (commander == null || !commander.hasSkill(S_STRATEGY)) {
            return commanderStrategy;
        }

        SkillModifierData skillModifierData = commander.getSkillModifierData();
        Skill strategy = commander.getSkill(S_STRATEGY);

        return strategy.getTotalSkillLevel(skillModifierData);
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
            startingSystem = systemList.get(getFaction().getStartingPlanet(getLocalDate()));

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
     * Assigns a random portrait to a {@link Person}.
     *
     * @param person The {@link Person} who should receive a randomized portrait.
     */
    public void assignRandomPortraitFor(final Person person) {
        final boolean allowDuplicatePortraits = campaignOptions.isAllowDuplicatePortraits();
        final boolean genderedPortraitsOnly = campaignOptions.isUseGenderedPortraitsOnly();
        final Portrait portrait = RandomPortraitGenerator.generate(getPersonnel(),
              person,
              allowDuplicatePortraits,
              genderedPortraitsOnly);
        if (!portrait.isDefault()) {
            person.setPortrait(portrait);
        }
    }

    /**
     * Assigns a random origin to a {@link Person}.
     *
     * @param person The {@link Person} who should receive a randomized origin.
     */
    public void assignRandomOriginFor(final Person person) {
        final Faction faction = getFactionSelector().selectFaction(this);
        if (faction != null) {
            person.setOriginFaction(faction);
        }

        final Planet planet = getPlanetSelector().selectPlanet(this, faction);
        if (planet != null) {
            person.setOriginPlanet(planet);
        }
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

    public void refreshNetworks() {
        for (Unit unit : getUnits()) {
            // we are going to rebuild the c3, nc3 and c3i networks based on
            // the c3UUIDs
            // TODO: can we do this more efficiently?
            // this code is cribbed from megamek.server#receiveEntityAdd
            Entity entity = unit.getEntity();
            if (null != entity && (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3())) {
                boolean C3iSet = false;
                boolean NC3Set = false;

                for (Entity e : game.getEntitiesVector()) {
                    // C3 Checks
                    if (entity.hasC3()) {
                        if ((entity.getC3MasterIsUUIDAsString() != null) &&
                                  entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                            entity.setC3Master(e, false);
                            break;
                        }
                    }
                    // Naval C3 checks
                    if (entity.hasNavalC3() && !NC3Set) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        // Well, they're the same value of 6...
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getNC3NextUUIDAsString(pos) != null) &&
                                      (e.getC3UUIDAsString() != null) &&
                                      entity.getNC3NextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                NC3Set = true;
                                break;
                            }

                            pos++;
                        }
                    }
                    // C3i Checks
                    if (entity.hasC3i() && !C3iSet) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getC3iNextUUIDAsString(pos) != null) &&
                                      (e.getC3UUIDAsString() != null) &&
                                      entity.getC3iNextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                C3iSet = true;
                                break;
                            }

                            pos++;
                        }
                    }
                }
            }
        }
    }

    public void disbandNetworkOf(Unit u) {
        // collect all the other units on this network to rebuild the uuids
        Vector<Unit> networkedUnits = new Vector<>();
        for (Unit unit : getUnits()) {
            if (null != unit.getEntity().getC3NetId() &&
                      unit.getEntity().getC3NetId().equals(u.getEntity().getC3NetId())) {
                networkedUnits.add(unit);
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (nUnit.getEntity().hasNavalC3()) {
                    nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                } else {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(networkedUnits));
    }

    public void removeUnitsFromNetwork(Vector<Unit> removedUnits) {
        // collect all the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<>();
        Vector<Unit> networkedUnits = new Vector<>();
        String network = removedUnits.get(0).getEntity().getC3NetId();
        for (Unit unit : getUnits()) {
            if (removedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId() && unit.getEntity().getC3NetId().equals(network)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit u : removedUnits) {
                if (u.getEntity().hasNavalC3()) {
                    u.getEntity().setNC3NextUUIDAsString(pos, null);
                } else {
                    u.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, uuids.get(pos));
                    }
                } else {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                    }
                }
            }
        }
        refreshNetworks();
    }

    public void addUnitsToNetwork(Vector<Unit> addedUnits, String networkID) {
        // collect all the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<>();
        Vector<Unit> networkedUnits = new Vector<>();
        for (Unit u : addedUnits) {
            uuids.add(u.getEntity().getC3UUIDAsString());
            networkedUnits.add(u);
        }
        for (Unit unit : getUnits()) {
            if (addedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId() && unit.getEntity().getC3NetId().equals(networkID)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, uuids.get(pos));
                    }
                } else {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                    }
                }
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(addedUnits));
    }

    public Vector<String[]> getAvailableC3iNetworks() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.hasC3i() && en.calculateFreeC3Nodes() < 5 && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    /**
     * @return returns a Vector of the unique name Strings of all Naval C3 networks that have at least 1 free node
     *       Adapted from getAvailableC3iNetworks() as the two technologies have very similar workings
     */
    public Vector<String[]> getAvailableNC3Networks() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.hasNavalC3() && en.calculateFreeC3Nodes() < 5 && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    public Vector<String[]> getAvailableC3MastersForSlaves() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            // count of free c3 nodes for single company-level masters
            // will not be right so skip
            if (en.hasC3M() && !en.hasC3MM() && en.C3MasterIs(en)) {
                continue;
            }
            if (en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3Nodes();
                network[2] = en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    public Vector<String[]> getAvailableC3MastersForMasters() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : getUnits()) {

            if (u.getForceId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.calculateFreeC3MNodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3MNodes();
                network[2] = en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    public void removeUnitsFromC3Master(Unit master) {
        List<Unit> removed = new ArrayList<>();
        for (Unit unit : getUnits()) {
            if (null != unit.getEntity().getC3MasterIsUUIDAsString() &&
                      unit.getEntity().getC3MasterIsUUIDAsString().equals(master.getEntity().getC3UUIDAsString())) {
                unit.getEntity().setC3MasterIsUUIDAsString(null);
                unit.getEntity().setC3Master(null, true);
                removed.add(unit);
            }
        }
        refreshNetworks();
        MekHQ.triggerEvent(new NetworkChangedEvent(removed));
    }

    /**
     * This function reloads the game entities into the game at the end of scenario resolution, so that entities are
     * properly updated and destroyed ones removed
     */
    public void reloadGameEntities() {
        game.reset();
        getHangar().forEachUnit(u -> {
            Entity en = u.getEntity();
            if (null != en) {
                game.addEntity(en, false);
            }
        });
    }

    public void completeMission(@Nullable Mission mission, MissionStatus status) {
        if (mission == null) {
            return;
        }
        mission.setStatus(status);
        if (mission instanceof Contract contract) {
            Money remainingMoney = Money.zero();
            // check for money in escrow According to FMM(r) pg 179, both failure and breach lead to no further
            // payment even though this seems foolish
            if (contract.getStatus().isSuccess()) {
                remainingMoney = contract.getMonthlyPayOut().multipliedBy(contract.getMonthsLeft(getLocalDate()));

                if (contract instanceof AtBContract) {
                    Money routedPayout = ((AtBContract) contract).getRoutedPayout();

                    remainingMoney = routedPayout == null ? remainingMoney : routedPayout;
                }
            }

            // If overage repayment is enabled, we first need to check if the salvage
            // percent is
            // under 100. 100 means you cannot have an overage.
            // Then, we check if the salvage percent is less than the percent salvaged by
            // the
            // unit in question. If it is, then they owe the assigner some cash
            if (getCampaignOptions().isOverageRepaymentInFinalPayment() && (contract.getSalvagePct() < 100.0)) {
                final double salvagePercent = contract.getSalvagePct() / 100.0;
                final Money maxSalvage = contract.getSalvagedByEmployer()
                                               .multipliedBy(salvagePercent / (1 - salvagePercent));
                if (contract.getSalvagedByUnit().isGreaterThan(maxSalvage)) {
                    final Money amountToRepay = contract.getSalvagedByUnit().minus(maxSalvage);
                    remainingMoney = remainingMoney.minus(amountToRepay);
                    contract.subtractSalvageByUnit(amountToRepay);
                }
            }

            if (getCampaignOptions().isUseShareSystem()) {
                ResourceBundle financeResources = ResourceBundle.getBundle("mekhq.resources.Finances",
                      MekHQ.getMHQOptions().getLocale());

                if (remainingMoney.isGreaterThan(Money.zero())) {
                    Money shares = remainingMoney.multipliedBy(contract.getSharesPercent()).dividedBy(100);
                    remainingMoney = remainingMoney.minus(shares);

                    if (getFinances().debit(TransactionType.SALARIES,
                          getLocalDate(),
                          shares,
                          String.format(financeResources.getString("ContractSharePayment.text"), contract.getName()))) {
                        addReport(FINANCES, financeResources.getString("DistributedShares.text"),
                              shares.toAmountAndSymbolString());

                        getFinances().payOutSharesToPersonnel(this, shares);
                    }
                }
            }

            if (remainingMoney.isPositive()) {
                getFinances().credit(TransactionType.CONTRACT_PAYMENT,
                      getLocalDate(),
                      remainingMoney,
                      "Remaining payment for " + contract.getName());
                addReport(FINANCES, "Your account has been credited for " +
                                          remainingMoney.toAmountAndSymbolString() +
                                          " for the remaining payout from contract " +
                                          contract.getHyperlinkedName());
            } else if (remainingMoney.isNegative()) {
                getFinances().credit(TransactionType.CONTRACT_PAYMENT,
                      getLocalDate(),
                      remainingMoney,
                      "Repaying payment overages for " + contract.getName());
                addReport(FINANCES, "Your account has been debited for " +
                                          remainingMoney.absolute().toAmountAndSymbolString() +
                                          " to repay payment overages occurred during the contract " +
                                          contract.getHyperlinkedName());
            }

            // This relies on the mission being a Contract, and AtB to be on
            if (getCampaignOptions().isUseAtB()) {
                setHasActiveContract();
            }
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
        int amazonFreeShipping = switch (campaignOptions.getUnitTransitTime()) {
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
        arrivalDate = switch (campaignOptions.getUnitTransitTime()) {
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
        PartInventory inventory = new PartInventory();

        int nSupply = 0;
        int nTransit = 0;
        for (Part p : getParts()) {
            if (!p.isSpare()) {
                continue;
            }
            if (part.isSamePartType(p)) {
                if (p.isPresent()) {
                    nSupply += p.getTotalQuantity();
                } else {
                    nTransit += p.getTotalQuantity();
                }
            }
        }

        inventory.setSupply(nSupply);
        inventory.setTransit(nTransit);

        int nOrdered = 0;
        IAcquisitionWork onOrder = getShoppingList().getShoppingItem(part);
        if (null != onOrder) {
            nOrdered += onOrder.getTotalQuantity();
        }

        inventory.setOrdered(nOrdered);

        String countModifier = "";
        if (part instanceof Armor) { // ProtoMek Armor and BAArmor are derived from Armor
            countModifier = "points";
        }
        if (part instanceof AmmoStorage) {
            countModifier = "shots";
        }

        inventory.setCountModifier(countModifier);
        return inventory;
    }

    public void addLoan(Loan loan) {
        addReport(FINANCES, "You have taken out loan " +
                                  loan +
                                  ". Your account has been credited " +
                                  loan.getPrincipal().toAmountAndSymbolString() +
                                  " for the principal amount.");
        finances.addLoan(loan);
        MekHQ.triggerEvent(new LoanNewEvent(loan));
        finances.credit(TransactionType.LOAN_PRINCIPAL,
              getLocalDate(),
              loan.getPrincipal(),
              "Loan principal for " + loan);
    }

    public void payOffLoan(Loan loan) {
        if (finances.debit(TransactionType.LOAN_PAYMENT,
              getLocalDate(),
              loan.determineRemainingValue(),
              "Loan payoff for " + loan)) {
            addReport(FINANCES, "You have paid off the remaining loan balance of " +
                                      loan.determineRemainingValue().toAmountAndSymbolString() +
                                      " on " +
                                      loan);
            finances.removeLoan(loan);
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

    /**
     * No longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void initTimeInService() {
        for (Person person : getPersonnel()) {
            if (!person.getPrimaryRole().isDependent() && person.getPrisonerStatus().isFree()) {
                LocalDate join = null;
                for (LogEntry logEntry : person.getPersonalLog()) {
                    if (join == null) {
                        // If by some nightmare there is no Joined date just use the first entry.
                        join = logEntry.getDate();
                    }
                    if (logEntry.getDesc().startsWith("Joined ") || logEntry.getDesc().startsWith("Freed ")) {
                        join = logEntry.getDate();
                        break;
                    }
                }

                person.setRecruitment((join != null) ? join : getLocalDate().minusYears(1));
            }
        }
    }

    /**
     * No longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void initTimeInRank() {
        for (Person person : getPersonnel()) {
            if (!person.getPrimaryRole().isDependent() && person.getPrisonerStatus().isFree()) {
                LocalDate join = null;
                for (LogEntry logEntry : person.getPersonalLog()) {
                    if (join == null) {
                        // If by some nightmare there is no date from the below, just use the first
                        // entry.
                        join = logEntry.getDate();
                    }

                    if (logEntry.getDesc().startsWith("Joined ") ||
                              logEntry.getDesc().startsWith("Freed ") ||
                              logEntry.getDesc().startsWith("Promoted ") ||
                              logEntry.getDesc().startsWith("Demoted ")) {
                        join = logEntry.getDate();
                    }
                }

                // For that one in a billion chance the log is empty. Clone today's date and
                // subtract a year
                person.setLastRankChangeDate((join != null) ? join : getLocalDate().minusYears(1));
            }
        }
    }

    public void initTurnover() {
        getRetirementDefectionTracker().setLastRetirementRoll(getLocalDate());
    }

    public void initAtB(boolean newCampaign) {
        if (!newCampaign) {
            /*
             * Switch all contracts to AtBContract's
             */
            for (Entry<Integer, Mission> me : missions.entrySet()) {
                Mission m = me.getValue();
                if (m instanceof Contract && !(m instanceof AtBContract)) {
                    me.setValue(new AtBContract((Contract) m, this));
                }
            }

            /*
             * Go through all the personnel records and assume the earliest date is the date
             * the unit was founded.
             */
            LocalDate founding = null;
            for (Person person : getPersonnel()) {
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
            for (Person person : getPersonnel()) {
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
                                 getCampaignOptions().isAeroRecruitsHaveUnits()) ||
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

            addAllCombatTeams(this.forces);

            // Determine whether there is an active contract
            setHasActiveContract();
        }

        setAtBConfig(AtBConfiguration.loadFromXml());
        RandomFactionGenerator.getInstance().startup(this);
        getContractMarket().generateContractOffers(this, newCampaign); // TODO : AbstractContractMarket : Remove
        setAtBEventProcessor(new AtBEventProcessor(this));
    }

    /**
     * Stop processing AtB events and release memory.
     */
    public void shutdownAtB() {
        RandomFactionGenerator.getInstance().dispose();
        atbEventProcessor.shutdown();
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
        if (!location.isOnPlanet()) {
            return -1;
        }

        if (getLocalDate().isBefore(getCampaignStartDate().plusDays(6))) {
            return -1;
        }

        boolean triggerTurnoverPrompt;
        switch (campaignOptions.getTurnoverFrequency()) {
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

        if (getRetirementDefectionTracker().getRetirees().isEmpty()) {
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
        return getActiveMissions(true).stream()
                     .flatMap(m -> m.getCurrentScenarios().stream())
                     .anyMatch(s -> (s.getDate() != null) &&
                                          !(s instanceof AtBScenario) &&
                                          !getLocalDate().isBefore(s.getDate()));
    }

    /**
     * Sets the type of rating method used.
     */
    public void setUnitRating(IUnitRating rating) {
        unitRating = rating;
    }

    /**
     * Returns the type of rating method as selected in the Campaign Options dialog. Lazy-loaded for performance.
     * Default is CampaignOpsReputation
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public IUnitRating getUnitRating() {
        return unitRating;
    }

    @Override
    public int getTechIntroYear() {
        if (getCampaignOptions().isLimitByYear()) {
            return getGameYear();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getGameYear() {
        return getLocalDate().getYear();
    }

    @Override
    public megamek.common.enums.Faction getTechFaction() {
        return techFaction;
    }

    public void updateTechFactionCode() {
        if (campaignOptions.isFactionIntroDate()) {
            for (megamek.common.enums.Faction f : megamek.common.enums.Faction.values()) {
                if (f.equals(megamek.common.enums.Faction.NONE)) {
                    continue;
                }
                if (f.getCodeMM().equals(getFaction().getShortName())) {
                    techFaction = f;
                    UnitTechProgression.loadFaction(techFaction);
                    return;
                }
            }
            // If the tech progression data does not include the current faction,
            // use a generic.
            if (getFaction().isClan()) {
                techFaction = megamek.common.enums.Faction.CLAN;
            } else if (getFaction().isPeriphery()) {
                techFaction = megamek.common.enums.Faction.PER;
            } else {
                techFaction = megamek.common.enums.Faction.IS;
            }
        } else {
            techFaction = megamek.common.enums.Faction.NONE;
        }
        // Unit tech level will be calculated if the code has changed.
        UnitTechProgression.loadFaction(techFaction);
    }

    @Override
    public boolean useClanTechBase() {
        return getFaction().isClan();
    }

    @Override
    public boolean useMixedTech() {
        if (useClanTechBase()) {
            return campaignOptions.isAllowISPurchases();
        } else {
            return campaignOptions.isAllowClanPurchases();
        }
    }

    @Override
    public SimpleTechLevel getTechLevel() {
        for (SimpleTechLevel lvl : SimpleTechLevel.values()) {
            if (campaignOptions.getTechLevel() == lvl.ordinal()) {
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
        return campaignOptions.isVariableTechLevel();
    }

    @Override
    public boolean showExtinct() {
        return !campaignOptions.isDisallowExtinctStuff();
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
        Person commander = getCommander();

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
        return partsInUseRequestedStockMap;
    }

    public void setPartsInUseRequestedStockMap(Map<String, Double> partsInUseRequestedStockMap) {
        this.partsInUseRequestedStockMap = partsInUseRequestedStockMap;
    }

    public boolean getIgnoreMothballed() {
        return ignoreMothballed;
    }

    public void setIgnoreMothballed(boolean ignoreMothballed) {
        this.ignoreMothballed = ignoreMothballed;
    }

    public boolean getTopUpWeekly() {
        return topUpWeekly;
    }

    public void setTopUpWeekly(boolean topUpWeekly) {
        this.topUpWeekly = topUpWeekly;
    }

    public PartQuality getIgnoreSparesUnderQuality() {
        return ignoreSparesUnderQuality;
    }

    public void setIgnoreSparesUnderQuality(PartQuality ignoreSparesUnderQuality) {
        this.ignoreSparesUnderQuality = ignoreSparesUnderQuality;
    }

    public void writePartInUseToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreMothBalled", ignoreMothballed);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "topUpWeekly", topUpWeekly);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreSparesUnderQuality", ignoreSparesUnderQuality.name());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "partInUseMap");
        writePartInUseMapToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "partInUseMap");
    }

    public void writePartInUseMapToXML(final PrintWriter pw, int indent) {
        for (String key : partsInUseRequestedStockMap.keySet()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "partInUseMapEntry");
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partInUseMapKey", key);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partInUseMapVal", partsInUseRequestedStockMap.get(key));
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "partInUseMapEntry");
        }
    }

    /**
     * Wipes the Parts in use map for the purpose of resetting all values to their default
     */
    public void wipePartsInUseMap() {
        this.partsInUseRequestedStockMap.clear();
    }

    /**
     * Retrieves the campaign faction icon for the specified {@link Campaign}. If a custom icon is defined in the
     * campaign's unit icon configuration, that icon is used. Otherwise, a default faction logo is fetched based on the
     * campaign's faction short name.
     *
     * @return An {@link ImageIcon} representing the faction icon for the given campaign.
     */
    public ImageIcon getCampaignFactionIcon() {
        ImageIcon icon;
        StandardForceIcon campaignIcon = getUnitIcon();

        if (campaignIcon.getFilename() == null) {
            icon = getFactionLogo(currentDay.getYear(), getFaction().getShortName());
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
     * Returns a list of entities (units) from all combat forces.
     *
     * @return a list of entities representing all combat units in the player force
     */
    public List<Entity> getAllCombatEntities() {
        List<Entity> units = new ArrayList<>();
        for (CombatTeam combatTeam : getCombatTeamsAsList()) {
            Force force = getForce(combatTeam.getForceId());
            if (force != null) {
                for (Unit unit : force.getAllUnitsAsUnits(getHangar(), true)) {
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
     * Determines the appropriate starting planet for a new campaign based on campaign type, faction, and various
     * fallback scenarios.
     *
     * <p>This method first checks if the campaign is classified as a mercenary or pirate campaign. If so, it
     * delegates responsibility to {@link #getMercenaryOrPirateStartingPlanet(Factions, String)}, which implements
     * special logic to handle those campaign types.</p>
     *
     * <p>For all other campaign types, it uses the current campaign's faction to attempt to retrieve that faction’s
     * canonical starting system for the current game date. If no valid system can be found (due to, for example, the
     * faction not having a valid capital), the logic falls back to a default faction’s starting planet, and, if
     * necessary, ultimately falls back to the planet Terra as a default universal location.</p>
     *
     * <p>The method also includes special handling for Clan campaigns: if the fallback logic would result in the
     * campaign starting on Terra but the campaign is clan-based, it attempts to relocate the starting planet to Strana
     * Mechty.</p>
     *
     * @return the {@link Planet} instance where the campaign should start
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Planet getNewCampaignStartingPlanet() {
        Factions factions = Factions.getInstance();

        final String TERRA_ID = "Terra";
        final String CLAN_CODE = "CLAN";

        Faction startingFaction;
        PlanetarySystem startingSystem;

        if (isMercenaryCampaign() || isPirateCampaign()) {
            return getMercenaryOrPirateStartingPlanet(factions, TERRA_ID);
        }

        // Default for non-merc/pirate campaigns
        startingFaction = faction;
        startingSystem = startingFaction.getStartingPlanet(this, currentDay);

        // Fallback if the system is unavailable
        if (startingSystem == null) {
            startingFaction = factions.getDefaultFaction();
            startingSystem = startingFaction.getStartingPlanet(this, currentDay);
            if (startingSystem == null) {
                startingSystem = this.systemsInstance.getSystemById(TERRA_ID);
            }
        }

        // Special case: Clan campaign starting on Terra, swap to Clan homeworld
        if (TERRA_ID.equals(startingSystem.getId()) && isClanCampaign()) {
            Faction clanFaction = factions.getFaction(CLAN_CODE);
            if (clanFaction != null) {
                PlanetarySystem clanSystem = clanFaction.getStartingPlanet(this, currentDay);
                if (clanSystem != null) {
                    startingSystem = clanSystem;
                }
            }
        }

        return startingSystem.getPrimaryPlanet();
    }

    /**
     * Selects a starting planet for mercenary or pirate campaigns by considering eligible factions, campaign date, and
     * appropriate weighting for periphery factions (if pirate).
     *
     * <p>For mercenary campaigns, the designated mercenary faction is used as the initial fallback. For pirate
     * campaigns, the Tortuga Dominions are preferred, but only if they are active at the campaign's start date;
     * otherwise, the game's configured default faction is used (usually Mercenary, but I opted not to hardcode
     * mercenary here in case the default changes).</p>
     *
     * <p>There is a two-thirds probability that the starting faction will be selected from all factions, subject to
     * several filters (playability, not a Clan, not deep periphery). For pirate campaigns, eligible periphery factions
     * are intentionally added multiple times to the selection pool to increase their likelihood of being chosen
     * (weighted randomness).</p>
     *
     * <p>After the faction is chosen, this method attempts to get that faction’s canonical starting world. If no
     * valid system is found, the logic falls back to Terra, ensuring that the campaign always has a valid starting
     * world even in case of missing data.</p>
     *
     * @param factions The {@link Factions} manager supplying access to all faction data.
     * @param TERRA_ID The globally unique identifier for the planet Terra, used for the ultimate fallback.
     *
     * @return the {@link Planet} used as the campaign start location.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Planet getMercenaryOrPirateStartingPlanet(Factions factions, String TERRA_ID) {
        final String TORTUGA_CODE = "TD";

        PlanetarySystem startingSystem;
        Faction startingFaction;
        // Determine fallback faction for merc/pirate
        startingFaction = isMercenaryCampaign()
                                ? factions.getFaction(MERCENARY_FACTION_CODE)
                                : factions.getFaction(TORTUGA_CODE);

        // If pirate fallback is unavailable at the campaign's start date, use the default faction
        if (isPirateCampaign() && !startingFaction.validIn(currentDay)) {
            startingFaction = factions.getDefaultFaction();
        }

        // 33% chance to start in fallback faction's capital
        if (randomInt(3) != 0) {
            // Pick a random, eligible recruiting faction
            List<Faction> recruitingFactions = new ArrayList<>();
            for (Faction possibleFaction : factions.getActiveFactions(currentDay)) {
                if (possibleFaction.isPlayable() && !possibleFaction.isClan() && !possibleFaction.isDeepPeriphery()) {
                    recruitingFactions.add(possibleFaction);

                    // If we're playing a pirate campaign, we want to triple the chance that we start in the periphery
                    if (possibleFaction.isPeriphery() && isPirateCampaign()) {
                        recruitingFactions.add(possibleFaction);
                        recruitingFactions.add(possibleFaction);
                    }
                }
            }
            if (!recruitingFactions.isEmpty()) {
                startingFaction = ObjectUtility.getRandomItem(recruitingFactions);
            }
        }

        startingSystem = startingFaction.getStartingPlanet(this, currentDay);
        if (startingSystem != null) {
            return startingSystem.getPrimaryPlanet();
        }

        // Fallback if no startingSystem
        startingSystem = this.systemsInstance.getSystemById(TERRA_ID);
        return startingSystem != null ? startingSystem.getPrimaryPlanet() : null;
    }

    /**
     * Now that systemsInstance is injectable and non-final, we may wish to update it on the fly.
     *
     * @return systemsInstance Systems instance used when instantiating this Campaign instance.
     */
    public Systems getSystemsInstance() {
        return systemsInstance;
    }

    /**
     * Set the systemsInstance to a new instance.  Useful for testing, or updating the set of systems within a running
     * Campaign.
     *
     * @param systemsInstance new Systems instance that this campaign should use.
     */
    public void setSystemsInstance(Systems systemsInstance) {
        this.systemsInstance = systemsInstance;
    }
}
