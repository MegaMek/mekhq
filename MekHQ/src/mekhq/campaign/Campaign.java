/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.campaignOptions.CampaignOptions.S_AUTO;
import static mekhq.campaign.campaignOptions.CampaignOptions.S_TECH;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_MONTH;
import static mekhq.campaign.campaignOptions.CampaignOptions.TRANSIT_UNIT_WEEK;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.campaign.force.Force.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.force.ForceType.STANDARD;
import static mekhq.campaign.market.contractMarket.ContractAutomation.performAutomatedActivation;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.mission.AtBContract.pickRandomCamouflage;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.performResupply;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.processAbandonedConvoy;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_A;
import static mekhq.campaign.personnel.Bloodmark.getBloodhuntSchedule;
import static mekhq.campaign.personnel.DiscretionarySpending.performDiscretionarySpending;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomMercenaryCompanyNameGenerator;
import static mekhq.campaign.personnel.education.EducationController.getAcademy;
import static mekhq.campaign.personnel.education.TrainingCombatTeams.processTrainingCombatTeams;
import static mekhq.campaign.personnel.enums.BloodmarkLevel.BLOODMARK_ZERO;
import static mekhq.campaign.personnel.lifeEvents.CommandersDayAnnouncement.isCommandersDay;
import static mekhq.campaign.personnel.lifeEvents.FreedomDayAnnouncement.isFreedomDay;
import static mekhq.campaign.personnel.lifeEvents.NewYearsDayAnnouncement.isNewYear;
import static mekhq.campaign.personnel.lifeEvents.WinterHolidayAnnouncement.isWinterHolidayMajorDay;
import static mekhq.campaign.personnel.skills.Aging.applyAgingSPA;
import static mekhq.campaign.personnel.skills.Aging.getMilestone;
import static mekhq.campaign.personnel.skills.Aging.updateAllSkillAgeModifiers;
import static mekhq.campaign.personnel.skills.AttributeCheckUtility.performQuickAttributeCheck;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.getType;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.areFieldKitchensWithinCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenUsage;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.processFatigueRecovery;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.RETIREMENT_AGE;
import static mekhq.campaign.randomEvents.GrayMonday.GRAY_MONDAY_EVENTS_BEGIN;
import static mekhq.campaign.randomEvents.GrayMonday.GRAY_MONDAY_EVENTS_END;
import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MINIMUM_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BONDSMAN;
import static mekhq.campaign.stratcon.StratconRulesManager.processIgnoredDynamicScenario;
import static mekhq.campaign.stratcon.SupportPointNegotiation.negotiateAdditionalSupportPoints;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;
import static mekhq.campaign.unit.Unit.TECH_WORK_DAY;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.isIneligibleToPerformProcurement;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

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
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.ITechnology.AvailabilityValue;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.equipment.BombMounted;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.EntitySavingException;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Quartermaster.PartAcquisitionResult;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsMarshaller;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.event.*;
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
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Bloodmark;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.RandomDependents;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.death.RandomDeath;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.BloodmarkLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.RandomPortraitGenerator;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.lifeEvents.ComingOfAgeAnnouncement;
import mekhq.campaign.personnel.lifeEvents.CommandersDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.FreedomDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.NewYearsDayAnnouncement;
import mekhq.campaign.personnel.lifeEvents.WinterHolidayAnnouncement;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.medical.MedicalController;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.AgingMilestone;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.GrayMonday;
import mekhq.campaign.randomEvents.RandomEventLibraries;
import mekhq.campaign.randomEvents.prisoners.PrisonerEventManager;
import mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.rating.FieldManualMercRevDragoonsRating;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
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
import mekhq.campaign.universe.factionStanding.*;
import mekhq.campaign.universe.fameAndInfamy.FameAndInfamyController;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.RangedFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.RangedPlanetSelector;
import mekhq.campaign.utilities.AutomatedPersonnelCleanUp;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.module.atb.AtBEventProcessor;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;
import mekhq.service.mrms.MRMSService;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;

/**
 * The main campaign class, keeps track of teams and units
 *
 * @author Taharqa
 */
public class Campaign implements ITechManager {
    private static final MMLogger logger = MMLogger.create(Campaign.class);

    public static final String REPORT_LINEBREAK = "<br/><br/>";

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

    private int astechPool;
    private int astechPoolMinutes;
    private int astechPoolOvertime;
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
    private ITechnology.Faction techFaction;
    private String retainerEmployerCode; // AtB
    private LocalDate retainerStartDate; // AtB
    private RankSystem rankSystem;

    private final ArrayList<String> currentReport;
    private transient String currentReportHTML;
    private transient List<String> newReports;

    private Boolean fieldKitchenWithinCapacity;

    // this is updated and used per gaming session, it is enabled/disabled via the
    // Campaign options
    // we're re-using the LogEntry class that is used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private Camouflage camouflage = pickRandomCamouflage(3025, "Root");
    private PlayerColour colour = PlayerColour.BLUE;
    private StandardForceIcon unitIcon = new UnitIcon(null, null);

    private Finances finances;

    private CurrentLocation location;
    private boolean isAvoidingEmptySystems;
    private boolean isOverridingCommandCircuitRequirements;

    private final News news;

    private final PartsStore partsStore;

    private final List<String> customs;

    private CampaignOptions campaignOptions;
    private RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();
    private MekHQ app;

    private ShoppingList shoppingList;

    private NewPersonnelMarket newPersonnelMarket;

    @Deprecated(since = "0.50.06", forRemoval = false)
    private PersonnelMarket personnelMarket;

    private AbstractContractMarket contractMarket;
    private AbstractUnitMarket unitMarket;

    private RandomDeath randomDeath;
    private transient AbstractDivorce divorce;
    private transient AbstractMarriage marriage;
    private transient AbstractProcreation procreation;
    private List<Person> personnelWhoAdvancedInXP;

    private RetirementDefectionTracker retirementDefectionTracker;
    private List<String> turnoverRetirementInformation;

    private AtBConfiguration atbConfig; // AtB
    private AtBEventProcessor atbEventProcessor; // AtB
    private LocalDate shipSearchStart; // AtB
    private int shipSearchType;
    private String shipSearchResult; // AtB
    private LocalDate shipSearchExpiration; // AtB
    private IUnitGenerator unitGenerator; // deprecated
    private IUnitRating unitRating; // deprecated
    private ReputationController reputation;
    private int crimeRating;
    private int crimePirateModifier;
    private LocalDate dateOfLastCrime;
    private FactionStandings factionStandings;
    private int initiativeBonus;
    private int initiativeMaxBonus;
    private final CampaignSummary campaignSummary;
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
    private static final WeekFields WEEK_FIELDS = WeekFields.ISO;

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

    public Campaign() {
        id = UUID.randomUUID();
        game = new Game();
        player = new Player(0, "self");
        game.addPlayer(0, player);
        currentDay = LocalDate.ofYearDay(3067, 1);
        campaignStartDate = null;
        campaignOptions = new CampaignOptions();
        try {
            setFaction(Factions.getInstance().getDefaultFaction());
        } catch (Exception ex) {
            logger.error("Unable to set faction to default faction. If this wasn't during automated testing this must" +
                               " be investigated.", ex);
            setFaction(new Faction());
        }
        techFaction = ITechnology.Faction.MERC;
        CurrencyManager.getInstance().setCampaign(this);
        try {
            location = new CurrentLocation(Systems.getInstance().getSystems().get("Galatea"), 0);
        } catch (Exception ex) {
            logger.error("Unable to set location to default galatea system. If this wasn't during automated testing " +
                               "this must be investigated.", ex);
            PlanetarySystem fallbackSystem = new PlanetarySystem("Galatea");
            location = new CurrentLocation(fallbackSystem, 0);
        }
        isAvoidingEmptySystems = true;
        isOverridingCommandCircuitRequirements = false;
        currentReport = new ArrayList<>();
        currentReportHTML = "";
        newReports = new ArrayList<>();
        name = randomMercenaryCompanyNameGenerator(null);
        overtime = false;
        gmMode = false;
        retainerEmployerCode = null;
        retainerStartDate = null;
        reputation = new ReputationController();
        factionStandings = new FactionStandings();
        crimeRating = 0;
        crimePirateModifier = 0;
        dateOfLastCrime = null;
        initiativeBonus = 0;
        initiativeMaxBonus = 1;
        setRankSystemDirect(Ranks.getRankSystemFromCode(Ranks.DEFAULT_SYSTEM_CODE));
        forces = new Force(name);
        forceIds.put(0, forces);
        combatTeams = new Hashtable<>();
        finances = new Finances();
        astechPool = 0;
        medicPool = 0;
        resetAstechMinutes();
        partsStore = new PartsStore(this);
        gameOptions = new GameOptions();
        gameOptions.getOption(OptionsConstants.ALLOWED_YEAR).setValue(getGameYear());
        game.setOptions(gameOptions);
        customs = new ArrayList<>();
        shoppingList = new ShoppingList();
        newPersonnelMarket = new NewPersonnelMarket(this);
        news = new News(getGameYear(), id.getLeastSignificantBits());
        setPersonnelMarket(new PersonnelMarket());
        setContractMarket(new AtbMonthlyContractMarket());
        setUnitMarket(new DisabledUnitMarket());
        randomDeath = new RandomDeath(this);
        setDivorce(new DisabledRandomDivorce(getCampaignOptions()));
        setMarriage(new DisabledRandomMarriage(getCampaignOptions()));
        setProcreation(new DisabledRandomProcreation(getCampaignOptions()));
        personnelWhoAdvancedInXP = new ArrayList<>();
        retirementDefectionTracker = new RetirementDefectionTracker();
        turnoverRetirementInformation = new ArrayList<>();
        atbConfig = null;
        autosaveService = new AutosaveService();
        hasActiveContract = false;
        campaignSummary = new CampaignSummary(this);
        quartermaster = new Quartermaster(this);
        fieldKitchenWithinCapacity = false;
        autoResolveBehaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
        automatedMothballUnits = new ArrayList<>();
        temporaryPrisonerCapacity = DEFAULT_TEMPORARY_CAPACITY;
        processProcurement = true;
        topUpWeekly = false;
        ignoreMothballed = true;
        ignoreSparesUnderQuality = QUALITY_A;

        // Library initialization
        try {
            randomEventLibraries = new RandomEventLibraries();
        } catch (Exception ex) {
            logger.error("Unable to initialize RandomEventLibraries. If this wasn't during automated testing this " +
                               "must be investigated.", ex);
        }

        try {
            factionStandingUltimatumsLibrary = new FactionStandingUltimatumsLibrary();
        } catch (Exception ex) {
            logger.error("Unable to initialize FactionStandingUltimatumsLibrary. If this wasn't during automated " +
                               "testing this must be investigated.", ex);
        }
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
    public Hashtable<Integer, CombatTeam> getCombatTeamsTable() {
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
    public ArrayList<CombatTeam> getAllCombatTeams() {
        // This call allows us to utilize the self-sanitizing feature of
        // getCombatTeamsTable(),
        // without needing to directly include the code here, too.
        combatTeams = getCombatTeamsTable();

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
    }
    // endregion Markets

    // region Personnel Modules
    public void resetRandomDeath() {
        this.randomDeath = new RandomDeath(this);
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
     * Initializes the unit generator based on the method chosen in campaignOptions. Called when the unit generator is
     * first used or when the method has been changed in campaignOptions.
     */
    public void initUnitGenerator() {
        if (unitGenerator != null && unitGenerator instanceof RATManager) {
            MekHQ.unregisterHandler(unitGenerator);
        }
        if (campaignOptions.isUseStaticRATs()) {
            RATManager rm = new RATManager();
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            }
            rm.setSelectedRATs(campaignOptions.getRATs());
            rm.setIgnoreRatEra(campaignOptions.isIgnoreRATEra());
            unitGenerator = rm;
        } else {
            unitGenerator = new RATGeneratorConnector(getGameYear());
        }
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
    public void setShipSearchStart(@Nullable LocalDate shipSearchStart) {
        this.shipSearchStart = shipSearchStart;
    }

    /**
     * @return The date a ship search was started, or null if none is in progress.
     */
    public LocalDate getShipSearchStart() {
        return shipSearchStart;
    }

    /**
     * Sets the lookup name of the available ship, or null if none were found.
     */
    public void setShipSearchResult(@Nullable String result) {
        shipSearchResult = result;
    }

    /**
     * @return The lookup name of the available ship, or null if none is available
     */
    public String getShipSearchResult() {
        return shipSearchResult;
    }

    /**
     * @return The date the ship is no longer available, if there is one.
     */
    public LocalDate getShipSearchExpiration() {
        return shipSearchExpiration;
    }

    public void setShipSearchExpiration(LocalDate shipSearchExpiration) {
        this.shipSearchExpiration = shipSearchExpiration;
    }

    /**
     * Sets the unit type to search for.
     */
    public void setShipSearchType(int unitType) {
        shipSearchType = unitType;
    }

    public void startShipSearch(int unitType) {
        setShipSearchStart(getLocalDate());
        setShipSearchType(unitType);
    }

    private void processShipSearch() {
        if (getShipSearchStart() == null) {
            return;
        }

        StringBuilder report = new StringBuilder();
        if (getFinances().debit(TransactionType.UNIT_PURCHASE,
              getLocalDate(),
              getAtBConfig().shipSearchCostPerWeek(),
              "Ship Search")) {
            report.append(getAtBConfig().shipSearchCostPerWeek().toAmountAndSymbolString())
                  .append(" deducted for ship search.");
        } else {
            addReport("<font color=" +
                            ReportingUtilities.getNegativeColor() +
                            ">Insufficient funds for ship search.</font>");
            setShipSearchStart(null);
            return;
        }

        long numDays = ChronoUnit.DAYS.between(getShipSearchStart(), getLocalDate());
        if (numDays > 21) {
            int roll = d6(2);
            TargetRoll target = getAtBConfig().shipSearchTargetRoll(shipSearchType, this);
            setShipSearchStart(null);
            report.append("<br/>Ship search target: ").append(target.getValueAsString()).append(" roll: ").append(roll);
            // TODO : mos zero should make ship available on retainer
            if (roll >= target.getValue()) {
                report.append("<br/>Search successful. ");

                MekSummary ms = getUnitGenerator().generate(getFaction().getShortName(),
                      shipSearchType,
                      -1,
                      getGameYear(),
                      getAtBUnitRatingMod());

                if (ms == null) {
                    ms = getAtBConfig().findShip(shipSearchType);
                }

                if (ms != null) {
                    setShipSearchResult(ms.getName());
                    setShipSearchExpiration(getLocalDate().plusDays(31));
                    report.append(getShipSearchResult())
                          .append(" is available for purchase for ")
                          .append(Money.of(ms.getCost()).toAmountAndSymbolString())
                          .append(" until ")
                          .append(MekHQ.getMHQOptions().getDisplayFormattedDate(getShipSearchExpiration()));
                } else {
                    report.append(" <font color=")
                          .append(ReportingUtilities.getNegativeColor())
                          .append(">Could not determine ship type.</font>");
                }
            } else {
                report.append("<br/>Ship search unsuccessful.");
            }
        }
        addReport(report.toString());
    }

    public void purchaseShipSearchResult() {
        MekSummary ms = MekSummaryCache.getInstance().getMek(getShipSearchResult());
        if (ms == null) {
            logger.error("Cannot find entry for {}", getShipSearchResult());
            return;
        }

        Money cost = Money.of(ms.getCost());

        if (getFunds().isLessThan(cost)) {
            addReport("<font color='" +
                            ReportingUtilities.getNegativeColor() +
                            "'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
            return;
        }

        MekFileParser mekFileParser;

        try {
            mekFileParser = new MekFileParser(ms.getSourceFile(), ms.getEntryName());
        } catch (Exception ex) {
            logger.error("Unable to load unit: {}", ms.getEntryName(), ex);
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
            addReport("<font color='" +
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
                        addReport(spouse.getHyperlinkedFullTitle() +
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
                                addReport(child.getHyperlinkedFullTitle() +
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
                                addReport(child.getHyperlinkedFullTitle() +
                                                ' ' +
                                                resources.getString("turnoverJointDepartureChild.text"));
                                child.changeStatus(this, getLocalDate(), PersonnelStatus.LEFT);

                                turnoverRetirementInformation.add(child.getHyperlinkedFullTitle() +
                                                                        ' ' +
                                                                        resources.getString(
                                                                              "turnoverJointDepartureChild.text"));
                            } else if (!child.getGenealogy().hasLivingParents()) {
                                addReport(child.getHyperlinkedFullTitle() + ' ' + resources.getString("orphaned.text"));

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
            addReport("<font color='" +
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
        StratconContractInitializer.restoreTransientStratconInformation(mission, this);
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
            addReport(MessageFormat.format(resources.getString("newAtBScenario.format"),
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

    public void setLocation(CurrentLocation l) {
        location = l;
    }

    /**
     * Moves immediately to a {@link PlanetarySystem}.
     *
     * @param s The {@link PlanetarySystem} the campaign has been moved to.
     */
    public void moveToPlanetarySystem(PlanetarySystem s) {
        setLocation(new CurrentLocation(s, 0.0));
        MekHQ.triggerEvent(new LocationChangedEvent(getLocation(), false));
    }

    public CurrentLocation getLocation() {
        return location;
    }

    /**
     * Imports a {@link Unit} into a campaign.
     *
     * @param unit A {@link Unit} to import into the campaign.
     */
    public void importUnit(Unit unit) {
        Objects.requireNonNull(unit);

        logger.debug("Importing unit: ({}): {}", unit.getId(), unit.getName());

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
     * @param testUnit TestUnit to add.
     */
    public void addTestUnit(TestUnit testUnit) {
        // we really just want the entity and the parts so let's just wrap that around a new unit.
        Unit unit = new Unit(testUnit.getEntity(), this);
        getHangar().addUnit(unit);

        // we decided we like the test unit so much we are going to keep it
        unit.getEntity().setOwner(player);
        unit.getEntity().setGame(game);
        unit.getEntity().setExternalIdAsString(unit.getId().toString());
        unit.setMaintenanceMultiplier(getCampaignOptions().getDefaultMaintenanceTime());

        // now lets grab the parts from the test unit and set them up with this unit
        for (Part p : testUnit.getParts()) {
            unit.addPart(p);
            getQuartermaster().addPart(p, 0);
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
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");
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
        unit.setMaintenanceMultiplier(getCampaignOptions().getDefaultMaintenanceTime());
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
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");
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

        return person;
    }

    public Boolean getFieldKitchenWithinCapacity() {
        return fieldKitchenWithinCapacity;
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
            logger.warn("A null person was passed into recruitPerson.");
            return false;
        }

        if (employ && !person.isEmployed()) {
            if (getCampaignOptions().isPayForRecruitment() && !gmAdd) {
                if (!getFinances().debit(TransactionType.RECRUITMENT,
                      getLocalDate(),
                      person.getSalary(this).multipliedBy(2),
                      String.format(resources.getString("personnelRecruitmentFinancesReason.text"),
                            person.getFullName()))) {
                    addReport(String.format(resources.getString("personnelRecruitmentInsufficientFunds.text"),
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
            person.setEmployed(true);
            if (person.getPrimaryRole().isAstech()) {
                astechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
                astechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
            } else if (person.getSecondaryRole().isAstech()) {
                astechPoolMinutes += Person.SECONDARY_ROLE_SUPPORT_TIME;
                astechPoolOvertime += Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME;
            }
        } else {
            person.setEmployed(false);
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
            addReport(String.format(resources.getString("personnelRecruitmentAddedToRoster.text"),
                  person.getHyperlinkedFullTitle(),
                  formerSurname,
                  add));
        }

        MekHQ.triggerEvent(new PersonNewEvent(person));
        return true;
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
                addReport(String.format(resources.getString("relativeJoinsForce.text"),
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
                addReport(String.format(resources.getString("relativeJoinsForce.text"),
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
        int bloodnameTarget = 6;
        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();
        if (!ignoreDice) {
            switch (person.getPhenotype()) {
                case MEKWARRIOR: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_MEK) ?
                                             person.getSkill(SkillType.S_GUN_MEK)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_MEK) ?
                                             person.getSkill(SkillType.S_PILOT_MEK)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case AEROSPACE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_AERO) ?
                                             person.getSkill(SkillType.S_GUN_AERO)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_AERO) ?
                                             person.getSkill(SkillType.S_PILOT_AERO)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case ELEMENTAL: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_BA) ?
                                             person.getSkill(SkillType.S_GUN_BA)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_ANTI_MEK) ?
                                             person.getSkill(SkillType.S_ANTI_MEK)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case VEHICLE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_VEE) ?
                                             person.getSkill(SkillType.S_GUN_VEE)
                                                   .getFinalSkillValue(options, attributes) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    switch (person.getPrimaryRole()) {
                        case GROUND_VEHICLE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_GVEE)
                                                           .getFinalSkillValue(options, attributes) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case NAVAL_VEHICLE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_NVEE)
                                                           .getFinalSkillValue(options, attributes) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VTOL_PILOT:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL) ?
                                                     person.getSkill(SkillType.S_PILOT_VTOL)
                                                           .getFinalSkillValue(options, attributes) :
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
                                                          .getFinalSkillValue(options, attributes) :
                                                    TargetRoll.AUTOMATIC_FAIL);
                    break;
                }
                case NAVAL: {
                    switch (person.getPrimaryRole()) {
                        case VESSEL_PILOT:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_PILOT_SPACE) ?
                                                            person.getSkill(SkillType.S_PILOT_SPACE)
                                                                  .getFinalSkillValue(options, attributes) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_GUNNER:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_GUN_SPACE) ?
                                                            person.getSkill(SkillType.S_GUN_SPACE)
                                                                  .getFinalSkillValue(options, attributes) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_CREW:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_TECH_VESSEL) ?
                                                            person.getSkill(SkillType.S_TECH_VESSEL)
                                                                  .getFinalSkillValue(options, attributes) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_NAVIGATOR:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_NAVIGATION) ?
                                                            person.getSkill(SkillType.S_NAVIGATION)
                                                                  .getFinalSkillValue(options, attributes) :
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
            // Higher-rated units are more likely to have Bloodnamed
            if (getCampaignOptions().getUnitRatingMethod().isEnabled()) {
                bloodnameTarget += IUnitRating.DRAGOON_C - getAtBUnitRatingMod();
            }

            // Reavings diminish the number of available Bloodrights in later eras
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
     * Retrieves a list of active personnel in the campaign, optionally including prisoners.
     *
     * <p>
     * This method iterates through all personnel and filters out inactive members. It then further filters prisoners
     * based on the provided parameter:
     * </p>
     * <ul>
     * <li>If {@code includePrisoners} is {@code true}, all active personnel,
     * including prisoners,
     * are included in the result.</li>
     * <li>If {@code includePrisoners} is {@code false}, only active personnel who
     * are either
     * free or classified as bondsmen are included.</li>
     * </ul>
     *
     * @param includePrisoners {@code true} to include all active prisoners in the result, {@code false} to exclude them
     *                         unless they are free or bondsmen.
     *
     * @return A {@link List} of {@link Person} objects representing the filtered active personnel.
     */
    public List<Person> getActivePersonnel(boolean includePrisoners) {
        List<Person> activePersonnel = new ArrayList<>();

        for (Person person : getPersonnel()) {
            if (!person.getStatus().isActive()) {
                continue;
            }

            PrisonerStatus prisonerStatus = person.getPrisonerStatus();
            if (includePrisoners || prisonerStatus.isFreeOrBondsman()) {
                activePersonnel.add(person);
            }
        }

        return activePersonnel;
    }

    /**
     * @return a list of people who are currently eligible to receive a salary.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public List<Person> getSalaryEligiblePersonnel() {
        return getActivePersonnel(false).stream()
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
        return getActivePersonnel(true).stream()
                     .filter(p -> p.getPrimaryRole().isCombat() || p.getSecondaryRole().isCombat())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active Dependents.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getActiveDependents() {
        return getPersonnel().stream()
                     .filter(person -> person.getPrimaryRole().isDependent())
                     .filter(person -> person.getStatus().isActive())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active prisoners.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getCurrentPrisoners() {
        return getActivePersonnel(true).stream()
                     .filter(person -> person.getPrisonerStatus().isCurrentPrisoner())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active prisoners who are willing to defect.
     *
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getPrisonerDefectors() {
        return getActivePersonnel(false).stream()
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
        for (Person person : getPersonnel()) {
            if (person.needsFixing() ||
                      (getCampaignOptions().isUseAdvancedMedical() &&
                             person.hasInjuries(true) &&
                             person.getStatus().isActive())) {
                patients.add(person);
            }
        }
        return patients;
    }

    /**
     * List of all units that can show up in the repair bay.
     */
    public List<Unit> getServiceableUnits() {
        List<Unit> service = new ArrayList<>();
        for (Unit u : getUnits()) {
            if (u.isAvailable() && u.isServiceable() && !StratconRulesManager.isUnitDeployedToStratCon(u)) {
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

    private int getQuantity(Part part) {
        return getWarehouse().getPartQuantity(part);
    }

    private PartInUse getPartInUse(Part part) {
        // SI isn't a proper "part"
        if (part instanceof StructuralIntegrity) {
            return null;
        }
        // Skip out on "not armor" (as in 0 point armer on men or field guns)
        if ((part instanceof Armor armor) && (armor.getType() == EquipmentType.T_ARMOR_UNKNOWN)) {
            return null;
        }
        // Makes no sense buying those separately from the chasis
        if ((part instanceof EquipmentPart equipmentPart) &&
                  (equipmentPart.getType() instanceof MiscType miscType) &&
                  (miscType.hasFlag(MiscType.F_CHASSIS_MODIFICATION))) {
            return null;
        }
        // Replace a "missing" part with a corresponding "new" one.
        if (part instanceof MissingPart missingPart) {
            part = missingPart.getNewPart();
        }
        PartInUse result = new PartInUse(part);
        result.setRequestedStock(getDefaultStockPercent(part));
        return (null != result.getPartToBuy()) ? result : null;
    }

    /**
     * Determines the default stock percentage for a given part type.
     *
     * <p>
     * This method uses the type of the provided {@link Part} to decide which default stock percentage to return. The
     * values for each part type are retrieved from the campaign options.
     * </p>
     *
     * @param part The {@link Part} for which the default stock percentage is to be determined. The part must not be
     *             {@code null}.
     *
     * @return An {@code int} representing the default stock percentage for the given part type, as defined in the
     *       campaign options.
     */
    private int getDefaultStockPercent(Part part) {
        if (part instanceof HeatSink) {
            return campaignOptions.getAutoLogisticsHeatSink();
        } else if (part instanceof MekLocation) {
            if (((MekLocation) part).getLoc() == Mek.LOC_HEAD) {
                return campaignOptions.getAutoLogisticsMekHead();
            }

            if (((MekLocation) part).getLoc() == Mek.LOC_CT) {
                return campaignOptions.getAutoLogisticsNonRepairableLocation();
            }

            return campaignOptions.getAutoLogisticsMekLocation();
        } else if (part instanceof TankLocation) {
            return campaignOptions.getAutoLogisticsNonRepairableLocation();
        } else if (part instanceof AmmoBin || part instanceof AmmoStorage) {
            return campaignOptions.getAutoLogisticsAmmunition();
        } else if (part instanceof Armor) {
            return campaignOptions.getAutoLogisticsArmor();
        } else if (part instanceof MekActuator) {
            return campaignOptions.getAutoLogisticsActuators();
        } else if (part instanceof JumpJet) {
            return campaignOptions.getAutoLogisticsJumpJets();
        } else if (part instanceof EnginePart) {
            return campaignOptions.getAutoLogisticsEngines();
        } else if (part instanceof EquipmentPart equipmentPart) {
            if (equipmentPart.getType() instanceof WeaponType) {
                return campaignOptions.getAutoLogisticsWeapons();
            }
        }

        return campaignOptions.getAutoLogisticsOther();
    }

    /**
     * Updates a {@link PartInUse} record with data from an incoming {@link Part}.
     *
     * <p>
     * This method processes the incoming part to update the usage, storage, or transfer count of the specified part in
     * use, based on the type, quality, and associated unit of the incoming part. Certain parts are ignored based on
     * their state or configuration, such as being part of conventional infantry, salvage, or mothballed units.
     * </p>
     *
     * @param partInUse                the {@link PartInUse} record to update.
     * @param incomingPart             the new {@link Part} that is being processed for this record.
     * @param ignoreMothballedUnits    if {@code true}, parts belonging to mothballed units are excluded.
     * @param ignoreSparesUnderQuality spares with a quality lower than this threshold are excluded from counting.
     */
    private void updatePartInUseData(PartInUse partInUse, Part incomingPart, boolean ignoreMothballedUnits,
          PartQuality ignoreSparesUnderQuality) {
        Unit unit = incomingPart.getUnit();
        if (unit != null) {
            // Ignore conventional infantry
            if (unit.isConventionalInfantry()) {
                return;
            }

            // Ignore parts if they are from mothballed units and the flag is set
            if (ignoreMothballedUnits && incomingPart.getUnit() != null && incomingPart.getUnit().isMothballed()) {
                return;
            }

            // Ignore units set to salvage
            if (unit.isSalvage()) {
                return;
            }
        }

        // Case 1: Part is associated with a unit or is a MissingPart
        if ((unit != null) || (incomingPart instanceof MissingPart)) {
            partInUse.setUseCount(partInUse.getUseCount() + getQuantity(incomingPart));
            return;
        }

        // Case 2: Part is present and meets quality requirements
        if (incomingPart.isPresent()) {
            if (incomingPart.getQuality().toNumeric() >= ignoreSparesUnderQuality.toNumeric()) {
                partInUse.setStoreCount(partInUse.getStoreCount() + getQuantity(incomingPart));
                partInUse.addSpare(incomingPart);
            }
            return;
        }

        // Case 3: Part is not present, update transfer count
        partInUse.setTransferCount(partInUse.getTransferCount() + getQuantity(incomingPart));
    }

    /**
     * Find all the parts that match this PartInUse and update their data
     *
     * @param partInUse                part in use record to update
     * @param ignoreMothballedUnits    don't count parts in mothballed units
     * @param ignoreSparesUnderQuality don't count spare parts lower than this quality
     */
    public void updatePartInUse(PartInUse partInUse, boolean ignoreMothballedUnits,
          PartQuality ignoreSparesUnderQuality) {
        partInUse.setUseCount(0);
        partInUse.setStoreCount(0);
        partInUse.setTransferCount(0);
        partInUse.setPlannedCount(0);
        getWarehouse().forEachPart(incomingPart -> {
            PartInUse newPartInUse = getPartInUse(incomingPart);
            if (partInUse.equals(newPartInUse)) {
                updatePartInUseData(partInUse, incomingPart, ignoreMothballedUnits, ignoreSparesUnderQuality);
            }
        });
        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            PartInUse newPartInUse = getPartInUse((Part) maybePart);
            if (partInUse.equals(newPartInUse)) {
                Part newPart = (maybePart instanceof MissingPart) ?
                                        (((MissingPart) maybePart).getNewPart())
                                        : (Part) maybePart;
                partInUse.setPlannedCount(partInUse.getPlannedCount() + newPart.getTotalQuantity());
            }
        }
    }

    /**
     * Analyzes the warehouse inventory and returns a data set that summarizes the usage state of all parts, including
     * their use counts, store counts, and planned counts, while filtering based on specific conditions.
     *
     * <p>
     * This method aggregates all parts currently in use or available as spares, while taking into account constraints
     * like ignoring mothballed units or filtering spares below a specific quality. It uses a map structure to
     * efficiently track and update parts during processing.
     * </p>
     *
     * @param ignoreMothballedUnits    If {@code true}, parts from mothballed units will not be included in the
     *                                 results.
     * @param isResupply               If {@code true}, specific units (e.g., prohibited unit types) are skipped based
     *                                 on the current context as defined in {@code Resupply.isProhibitedUnitType()}.
     * @param ignoreSparesUnderQuality Spare parts of a lower quality than the specified value will be excluded from the
     *                                 results.
     *
     * @return A {@link Set} of {@link PartInUse} objects detailing the state of each relevant part, including:
     *       <ul>
     *       <li>Use count: How many of this part are currently in use.</li>
     *       <li>Store count: How many of this part are available as spares in the
     *       warehouse.</li>
     *       <li>Planned count: The quantity of this part included in acquisition
     *       orders or
     *       planned procurement.</li>
     *       <li>Requested stock: The target or default quantity to maintain, as
     *       derived from
     *       settings or requests.</li>
     *       </ul>
     *       Only parts with non-zero counts (use, store, or planned) will be
     *       included in the
     *       result.
     */

    public Set<PartInUse> getPartsInUse(boolean ignoreMothballedUnits, boolean isResupply,
          PartQuality ignoreSparesUnderQuality) {
        // java.util.Set doesn't supply a get(Object) method, so we have to use a
        // java.util.Map
        Map<PartInUse, PartInUse> inUse = new HashMap<>();
        getWarehouse().forEachPart(incomingPart -> {
            if (isResupply) {
                Unit unit = incomingPart.getUnit();

                Entity entity = null;
                if (unit != null) {
                    entity = unit.getEntity();
                }

                if (entity != null) {
                    if (isProhibitedUnitType(entity, false, false)) {
                        return;
                    }
                }
            }

            PartInUse partInUse = getPartInUse(incomingPart);
            if (null == partInUse) {
                return;
            }
            if (inUse.containsKey(partInUse)) {
                partInUse = inUse.get(partInUse);
            } else {
                if (partsInUseRequestedStockMap.containsKey(partInUse.getDescription())) {
                    partInUse.setRequestedStock(partsInUseRequestedStockMap.get(partInUse.getDescription()));
                } else {
                    partInUse.setRequestedStock(getDefaultStockPercent(incomingPart));
                }
                inUse.put(partInUse, partInUse);
            }
            updatePartInUseData(partInUse, incomingPart, ignoreMothballedUnits, ignoreSparesUnderQuality);
        });
        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            if (!(maybePart instanceof Part)) {
                continue;
            }
            PartInUse partInUse = getPartInUse((Part) maybePart);
            if (null == partInUse) {
                continue;
            }
            if (inUse.containsKey(partInUse)) {
                partInUse = inUse.get(partInUse);
            } else {
                if (partsInUseRequestedStockMap.containsKey(partInUse.getDescription())) {
                    partInUse.setRequestedStock(partsInUseRequestedStockMap.get(partInUse.getDescription()));
                } else {
                    partInUse.setRequestedStock(getDefaultStockPercent((Part) maybePart));
                }
                inUse.put(partInUse, partInUse);
            }
            Part newPart = (maybePart instanceof MissingPart) ?
                    (((MissingPart) maybePart).getNewPart())
                    : (Part) maybePart;
            partInUse.setPlannedCount(partInUse.getPlannedCount() + newPart.getTotalQuantity());
        }
        return inUse.keySet()
                     .stream()
                     // Hacky but otherwise we end up with zero lines when filtering things out
                     .filter(p -> p.getUseCount() != 0 || p.getStoreCount() != 0 || p.getPlannedCount() != 0)
                     .collect(Collectors.toSet());
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

    public void setNewReports(List<String> reports) {
        newReports = reports;
    }

    public List<String> fetchAndClearNewReports() {
        List<String> oldReports = newReports;
        setNewReports(new ArrayList<>());
        return oldReports;
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

        for (Person person : getActivePersonnel(false)) {
            int adjustedReputation = person.getAdjustedReputation(isUseAgingEffects,
                  isClanCampaign,
                  currentDay,
                  person.getRankNumeric());

            if (((person.getPrimaryRole() == role) || (person.getSecondaryRole() == role)) &&
                      (person.getSkill(primary) != null)) {
                Skill primarySkill = person.getSkill(primary);
                int currentSkillLevel = Integer.MIN_VALUE;

                if (primarySkill != null) {
                    currentSkillLevel = primarySkill.getTotalSkillLevel(person.getOptions(),
                          person.getATOWAttributes(),
                          adjustedReputation);
                }

                if (bestInRole == null || currentSkillLevel > highest) {
                    bestInRole = person;
                    highest = currentSkillLevel;
                } else if (secondary != null && currentSkillLevel == highest) {
                    Skill secondarySkill = person.getSkill(secondary);

                    if (secondarySkill == null) {
                        continue;
                    }

                    currentSkillLevel = secondarySkill.getTotalSkillLevel(person.getOptions(),
                          person.getATOWAttributes(),
                          adjustedReputation);

                    int bestInRoleSecondarySkill = Integer.MIN_VALUE;
                    if (bestInRole.hasSkill(secondary)) {
                        int bestInRoleAdjustedReputation = bestInRole.getAdjustedReputation(isUseAgingEffects,
                              isClanCampaign,
                              currentDay,
                              bestInRole.getRankNumeric());
                        bestInRoleSecondarySkill = secondarySkill.getTotalSkillLevel(bestInRole.getOptions(),
                              bestInRole.getATOWAttributes(),
                              bestInRoleAdjustedReputation);
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
        Person bestAtSkill = null;
        int highest = 0;
        for (Person person : getActivePersonnel(false)) {
            int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                  isClanCampaign(),
                  currentDay,
                  person.getRankNumeric());
            Skill skill = person.getSkill(skillName);

            int totalSkillLevel = Integer.MIN_VALUE;
            if (skill != null) {
                totalSkillLevel = skill.getTotalSkillLevel(person.getOptions(),
                      person.getATOWAttributes(),
                      adjustedReputation);
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
     *   <li>The list can be sorted from elite (best) to least skilled if {@code eliteFirst} is set to {@code true}.</li>
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
        final List<Person> techs = getActivePersonnel(true).stream()
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
              !person.getPrimaryRole().isTech() && person.getSecondaryRole().isTechSecondary()).ordinal()));

        if (eliteFirst) {
            Collections.reverse(techs);
        }

        // sort based on available minutes (highest -> lowest)
        techs.sort(Comparator.comparingInt(person -> -person.getDailyAvailableTechTime(false)));

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
        for (Person person : getActivePersonnel(true)) {
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
        for (Person person : getActivePersonnel(true)) {
            if (person.isDoctor()) {
                docs.add(person);
            }
        }
        return docs;
    }

    public int getPatientsFor(Person doctor) {
        int patients = 0;
        for (Person person : getActivePersonnel(true)) {
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
        final String skillName = campaignOptions.getAcquisitionSkill();
        final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
        final int defaultMaxAcquisitions = campaignOptions.getMaxAcquisitions();

        int bestSkill = -1;
        Person procurementCharacter = null;
        if (skillName.equals(S_AUTO)) {
            return null;
        } else if (skillName.equals(S_TECH)) {
            for (Person person : getActivePersonnel(false)) {
                int effectiveMaxAcquisitions = defaultMaxAcquisitions;

                PersonnelOptions options = person.getOptions();
                if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                    continue;
                }

                if (defaultMaxAcquisitions > 0 && (person.getAcquisitions() >= effectiveMaxAcquisitions)) {
                    continue;
                }

                int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                      isClanCampaign(),
                      currentDay,
                      person.getRankNumeric());
                Skill skill = person.getSkill(skillName);

                int totalSkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    totalSkillLevel = skill.getTotalSkillLevel(person.getOptions(),
                          person.getATOWAttributes(),
                          adjustedReputation);
                }

                if (totalSkillLevel > bestSkill) {
                    procurementCharacter = person;
                    bestSkill = totalSkillLevel;
                }
            }
        } else {
            for (Person person : getActivePersonnel(false)) {
                int effectiveMaxAcquisitions = defaultMaxAcquisitions;

                if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                    continue;
                }

                if (defaultMaxAcquisitions > 0 && (person.getAcquisitions() >= effectiveMaxAcquisitions)) {
                    continue;
                }

                int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                      isClanCampaign(),
                      currentDay,
                      person.getRankNumeric());
                Skill skill = person.getSkill(skillName);

                int totalSkillLevel = Integer.MIN_VALUE;
                if (skill != null) {
                    totalSkillLevel = skill.getTotalSkillLevel(person.getOptions(),
                          person.getATOWAttributes(),
                          adjustedReputation);
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
     * @return an array where index 0 is the commander (may be the flagged commander), and index 1 is the
     *       second-in-command; either or both may be {@code null} if no suitable personnel are available.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Person[] findTopCommanders() {
        Person flaggedCommander = getFlaggedCommander();
        Person commander = flaggedCommander;
        Person secondInCommand = null;

        for (Person person : getActivePersonnel(false)) {
            // If we have a flagged commander, skip them
            if (flaggedCommander != null) {
                if (person.equals(flaggedCommander)) {
                    continue;
                }
                // Second in command is best among non-flagged
                if (secondInCommand == null || person.outRanksUsingSkillTiebreaker(this, secondInCommand)) {
                    secondInCommand = person;
                }
            } else {
                if (commander == null) {
                    commander = person;
                } else if (person.outRanksUsingSkillTiebreaker(this, commander)) {
                    secondInCommand = commander;
                    commander = person;
                } else if (secondInCommand == null || person.outRanksUsingSkillTiebreaker(this, secondInCommand)) {
                    if (!person.equals(commander)) {
                        secondInCommand = person;
                    }
                }
            }
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
        final String skillName = getCampaignOptions().getAcquisitionSkill();

        if (skillName.equals(S_AUTO)) {
            return Collections.emptyList();
        } else {
            final int maxAcquisitions = campaignOptions.getMaxAcquisitions();
            final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
            List<Person> logisticsPersonnel = new ArrayList<>();

            for (Person person : getActivePersonnel(true)) {
                if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                    continue;
                }

                if ((maxAcquisitions > 0) && (person.getAcquisitions() >= maxAcquisitions)) {
                    continue;
                }
                if (skillName.equals(S_TECH)) {
                    if (null != person.getBestTechSkill()) {
                        logisticsPersonnel.add(person);
                    }
                } else if (person.hasSkill(skillName)) {
                    logisticsPersonnel.add(person);
                }
            }

            // Sort by their skill level, descending.
            logisticsPersonnel.sort((person1, person2) -> {
                if (skillName.equals(S_TECH)) {
                    // Person 1
                    int adjustedReputation = person1.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                          isClanCampaign(),
                          currentDay,
                          person1.getRankNumeric());
                    Skill skill = person1.getBestTechSkill();

                    int person1SkillLevel = Integer.MIN_VALUE;
                    if (skill != null) {
                        person1SkillLevel = skill.getTotalSkillLevel(person1.getOptions(),
                              person1.getATOWAttributes(),
                              adjustedReputation);
                    }

                    // Person 2
                    adjustedReputation = person2.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                          isClanCampaign(),
                          currentDay,
                          person2.getRankNumeric());
                    skill = person2.getBestTechSkill();

                    int person2SkillLevel = Integer.MIN_VALUE;
                    if (skill != null) {
                        person2SkillLevel = skill.getTotalSkillLevel(person2.getOptions(),
                              person2.getATOWAttributes(),
                              adjustedReputation);
                    }

                    return Integer.compare(person1SkillLevel, person2SkillLevel);
                } else {
                    // Person 1
                    int adjustedReputation = person1.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                          isClanCampaign(),
                          currentDay,
                          person1.getRankNumeric());
                    Skill skill = person1.getSkill(S_TECH);

                    int person1SkillLevel = Integer.MIN_VALUE;
                    if (skill != null) {
                        person1SkillLevel = skill.getTotalSkillLevel(person1.getOptions(),
                              person1.getATOWAttributes(),
                              adjustedReputation);
                    }

                    // Person 2
                    adjustedReputation = person2.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
                          isClanCampaign(),
                          currentDay,
                          person2.getRankNumeric());
                    skill = person2.getSkill(S_TECH);

                    int person2SkillLevel = Integer.MIN_VALUE;
                    if (skill != null) {
                        person2SkillLevel = skill.getTotalSkillLevel(person2.getOptions(),
                              person2.getATOWAttributes(),
                              adjustedReputation);
                    }

                    return Integer.compare(person1SkillLevel, person2SkillLevel);
                }
            });

            return logisticsPersonnel;
        }
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

        if (getCampaignOptions().getAcquisitionSkill().equals(S_AUTO)) {
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
            addReport("Your force has no one capable of acquiring equipment.");
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
            addReport("Your force has no one capable of acquiring equipment.");
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
        List<PlanetarySystem> systems = Systems.getInstance()
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
                                addReport(personTitle +
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
                                addReport("<font color='" +
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

        // add shelved items back to the currentlist
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
                addReport("<font color='" +
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
                             acquisition.getTechBase() == Part.TechBase.CLAN);

        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport("<font color='" +
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
                addReport("<font color='" +
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
                addReport("<font color='" +
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
                                 acquisition.getTechBase() == Part.TechBase.CLAN);
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
                addReport(report);
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
            if (transitDays < 0) {
                transitDays = calculatePartTransitTime(acquisition.getAvailability());
            }
            report = report + acquisition.find(transitDays);
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
            addReport(report);
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
     *       <li>Requires astech support time (6 minutes per tech minute)</li>
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
            logger.warn("Unit is already mothballed, cannot mothball.");
            return;
        }

        String report;
        if (!unit.isConventionalInfantry()) {
            Person tech = unit.getTech();
            if (null == tech) {
                // uh-oh
                addReport(String.format(resources.getString("noTech.mothballing"), unit.getHyperlinkedName()));
                unit.cancelMothballOrActivation();
                return;
            }

            // don't allow overtime minutes for mothballing because it's cheating since you don't roll
            int minutes = Math.min(tech.getMinutesLeft(), unit.getMothballTime());

            // check astech time
            if (!unit.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
                // uh-oh
                addReport(String.format(resources.getString("notEnoughAstechTime.mothballing"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                astechPoolMinutes -= 6 * minutes;
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

        addReport(report);
    }

    /**
     * Performs work to activate a unit from its mothballed state. This process requires either:
     *
     * <ul>
     *   <li>A tech and sufficient astech support time for non-self-crewed units</li>
     *   <li>Only time for self-crewed units</li>
     * </ul>
     *
     * <p>The activation process:</p>
     * <ol>
     *   <li>Verifies the unit is mothballed</li>
     *   <li>For non-self-crewed units:
     *     <ul>
     *       <li>Checks for assigned tech</li>
     *       <li>Verifies sufficient tech and astech time</li>
     *       <li>Consumes tech and astech time</li>
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
            logger.warn("Unit is already activated, cannot activate.");
            return;
        }

        String report;
        if (!unit.isConventionalInfantry()) {
            Person tech = unit.getTech();
            if (null == tech) {
                // uh-oh
                addReport(String.format(resources.getString("noTech.activation"), unit.getHyperlinkedName()));
                unit.cancelMothballOrActivation();
                return;
            }

            // don't allow overtime minutes for activation because it's cheating since you don't roll
            int minutes = Math.min(tech.getMinutesLeft(), unit.getMothballTime());

            // check astech time
            if (!unit.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
                // uh-oh
                addReport(String.format(resources.getString("notEnoughAstechTime.activation"),
                      unit.getHyperlinkedName()));
                return;
            }

            unit.setMothballTime(unit.getMothballTime() - minutes);

            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            if (!unit.isSelfCrewed()) {
                astechPoolMinutes -= 6 * minutes;
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

        addReport(report);
    }

    public void refit(Refit theRefit) {
        Person tech = (theRefit.getUnit().getEngineer() == null) ?
                            theRefit.getTech() :
                            theRefit.getUnit().getEngineer();
        if (tech == null) {
            addReport("No tech is assigned to refit " +
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
        addReport(report);
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
        if ((partWork instanceof Armor) && !partWork.isSalvaging()) {
            if (!((Armor) partWork).isInSupply()) {
                report += "<b>Not enough armor remaining.  Task suspended.</b>";
                addReport(report);
                return report;
            }
        }
        if ((partWork instanceof ProtoMekArmor) && !partWork.isSalvaging()) {
            if (!((ProtoMekArmor) partWork).isInSupply()) {
                report += "<b>Not enough Protomek armor remaining.  Task suspended.</b>";
                addReport(report);
                return report;
            }
        }
        if ((partWork instanceof BaArmor) && !partWork.isSalvaging()) {
            if (!((BaArmor) partWork).isInSupply()) {
                report += "<b>Not enough BA armor remaining.  Task suspended.</b>";
                addReport(report);
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
                int helpMod = getShorthandedMod(getAvailableAstechs(minutesUsed, usedOvertime), false);
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
                addReport(report);
                return report;
            }
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        }
        int astechMinutesUsed = minutesUsed * getAvailableAstechs(minutesUsed, usedOvertime);
        if (astechPoolMinutes < astechMinutesUsed) {
            astechMinutesUsed -= astechPoolMinutes;
            astechPoolMinutes = 0;
            astechPoolOvertime -= astechMinutesUsed;
        } else {
            astechPoolMinutes -= astechMinutesUsed;
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
                             ((tech.getExperienceLevel(this, false) == SkillType.EXP_LEGENDARY) ||
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
                actualSkillLevel = relevantSkill.getExperienceLevel(tech.getOptions(), tech.getATOWAttributes());
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
        addReport(report);
        return report;
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
            addReport(article.getHeadlineForReport());
        }

        for (NewsItem article : Systems.getInstance().getPlanetaryNews(getLocalDate())) {
            addReport(article.getHeadlineForReport());
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
                    if (!combatRole.isTraining() || contract.getContractType().isCadreDuty()) {
                        total += combatTeam.getSize(this);
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

    private void processNewDayATBScenarios() {
        // First, we get the list of all active AtBContracts
        List<AtBContract> contracts = getActiveAtBContracts(true);

        // Second, we process them and any already generated scenarios
        for (AtBContract contract : contracts) {
            /*
             * Situations like a delayed start or running out of funds during transit can
             * delay arrival until after the contract start. In that case, shift the
             * starting and ending dates before making any battle rolls. We check that the
             * unit is actually on route to the planet in case the user is using a custom
             * system for transport or splitting the unit, etc.
             */
            if (!getLocation().isOnPlanet() &&
                      !getLocation().getJumpPath().isEmpty() &&
                      getLocation().getJumpPath().getLastSystem().getId().equals(contract.getSystemId())) {
                // transitTime is measured in days; so we round up to the next whole day
                contract.setStartAndEndDate(getLocalDate().plusDays((int) Math.ceil(getLocation().getTransitTime())));
                addReport("The start and end dates of " +
                                contract.getHyperlinkedName() +
                                " have been shifted to reflect the current ETA.");

                if (campaignOptions.isUseStratCon() && contract.getMoraleLevel().isRouted()) {
                    LocalDate newRoutEndDate = contract.getStartDate().plusMonths(max(1, d6() - 3)).minusDays(1);
                    contract.setRoutEndDate(newRoutEndDate);
                }

                continue;
            }

            if (getLocalDate().equals(contract.getStartDate())) {
                getUnits().forEach(unit -> unit.setSite(contract.getRepairLocation(getAtBUnitRatingMod())));
            }

            if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                int deficit = getDeploymentDeficit(contract);
                StratconCampaignState campaignState = contract.getStratconCampaignState();

                if (campaignState != null && deficit > 0) {
                    addReport(String.format(resources.getString("contractBreach.text"),
                          contract.getHyperlinkedName(),
                          spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                          CLOSING_SPAN_TAG));

                    campaignState.updateVictoryPoints(-1);
                } else if (deficit > 0) {
                    contract.addPlayerMinorBreaches(deficit);
                    addReport("Failure to meet " +
                                    contract.getHyperlinkedName() +
                                    " requirements resulted in " +
                                    deficit +
                                    ((deficit == 1) ? " minor contract breach" : " minor contract breaches"));
                }
            }

            if (Objects.equals(location.getCurrentSystem(), contract.getSystem())) {
                if (!automatedMothballUnits.isEmpty()) {
                    performAutomatedActivation(this);
                }
            }

            for (final Scenario scenario : contract.getCurrentAtBScenarios()) {
                if ((scenario.getDate() != null) && scenario.getDate().isBefore(getLocalDate())) {
                    if (getCampaignOptions().isUseStratCon() && (scenario instanceof AtBDynamicScenario)) {
                        StratconCampaignState campaignState = contract.getStratconCampaignState();

                        if (campaignState == null) {
                            return;
                        }

                        processIgnoredDynamicScenario(scenario.getId(), campaignState);

                        ScenarioType scenarioType = scenario.getStratConScenarioType();
                        if (scenarioType.isResupply()) {
                            processAbandonedConvoy(this, contract, (AtBDynamicScenario) scenario);
                        }

                        scenario.convertToStub(this, ScenarioStatus.REFUSED_ENGAGEMENT);
                        scenario.clearAllForcesAndPersonnel(this);
                    } else {
                        scenario.convertToStub(this, ScenarioStatus.REFUSED_ENGAGEMENT);
                        contract.addPlayerMinorBreach();

                        addReport("Failure to deploy for " +
                                        scenario.getHyperlinkedName() +
                                        " resulted in a minor contract breach.");
                    }
                }
            }
        }

        // Third, on Mondays we generate new scenarios for the week
        if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            AtBScenarioFactory.createScenariosForNewWeek(this);
        }

        // Fourth, we look at deployments for pre-existing and new scenarios
        for (AtBContract contract : contracts) {
            contract.checkEvents(this);

            // If there is a standard battle set for today, deploy the lance.
            for (final AtBScenario atBScenario : contract.getCurrentAtBScenarios()) {
                if ((atBScenario.getDate() != null) && atBScenario.getDate().equals(getLocalDate())) {
                    int forceId = atBScenario.getCombatTeamId();
                    if ((combatTeams.get(forceId) != null) && !forceIds.get(forceId).isDeployed()) {
                        // If any unit in the force is under repair, don't deploy the force
                        // Merely removing the unit from deployment would break with user expectation
                        boolean forceUnderRepair = false;
                        for (UUID uid : forceIds.get(forceId).getAllUnits(false)) {
                            Unit u = getHangar().getUnit(uid);
                            if ((u != null) && u.isUnderRepair()) {
                                forceUnderRepair = true;
                                break;
                            }
                        }

                        if (!forceUnderRepair) {
                            forceIds.get(forceId).setScenarioId(atBScenario.getId(), this);
                            atBScenario.addForces(forceId);

                            addReport(MessageFormat.format(resources.getString("atbScenarioTodayWithForce.format"),
                                  atBScenario.getHyperlinkedName(),
                                  forceIds.get(forceId).getName()));
                            MekHQ.triggerEvent(new DeploymentChangedEvent(forceIds.get(forceId), atBScenario));
                        } else {
                            if (atBScenario.getHasTrack()) {
                                addReport(MessageFormat.format(resources.getString("atbScenarioToday.stratCon"),
                                      atBScenario.getHyperlinkedName()));
                            } else {
                                addReport(MessageFormat.format(resources.getString("atbScenarioToday.atb"),
                                      atBScenario.getHyperlinkedName()));
                            }
                        }
                    } else {
                        if (atBScenario.getHasTrack()) {
                            addReport(MessageFormat.format(resources.getString("atbScenarioToday.stratCon"),
                                  atBScenario.getHyperlinkedName()));
                        } else {
                            addReport(MessageFormat.format(resources.getString("atbScenarioToday.atb"),
                                  atBScenario.getHyperlinkedName()));
                        }
                    }
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
        contractMarket.generateContractOffers(this);

        if ((getShipSearchExpiration() != null) && !getShipSearchExpiration().isAfter(getLocalDate())) {
            setShipSearchExpiration(null);
            if (getShipSearchResult() != null) {
                addReport("Opportunity for purchase of " + getShipSearchResult() + " has expired.");
                setShipSearchResult(null);
            }
        }

        if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            processShipSearch();
            processTrainingCombatTeams(this);
        }

        if (getLocalDate().getDayOfMonth() == 1) {
            /*
             * First of the month; roll Morale.
             */
            if (campaignOptions.getUnitRatingMethod().isFMMR()) {
                IUnitRating rating = getUnitRating();
                rating.reInitialize();
            }

            for (AtBContract contract : getActiveAtBContracts()) {
                AtBMoraleLevel oldMorale = contract.getMoraleLevel();

                contract.checkMorale(this, getLocalDate());
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
                    addReport(report);
                }
            }
        }

        // Resupply
        if (currentDay.getDayOfMonth() == 2) {
            // This occurs at the end of the 1st day, each month to avoid an awkward mechanics interaction where
            // personnel might quit or get taken out of fatigue without the player having any opportunity to
            // intervene before their resupply attempt becomes active.
            List<AtBContract> activeContracts = getActiveAtBContracts();
            AtBContract firstNonSubcontract = null;
            for (AtBContract contract : activeContracts) {
                if (!contract.isSubcontract()) {
                    firstNonSubcontract = contract;
                    break;
                }
            }

            if (firstNonSubcontract != null) {
                if (campaignOptions.isUseStratCon()) {
                    boolean inLocation = location.isOnPlanet() &&
                                               location.getCurrentSystem().equals(firstNonSubcontract.getSystem());

                    if (inLocation) {
                        processResupply(firstNonSubcontract);
                    }
                }
            }
        }

        int weekOfYear = currentDay.get(WEEK_FIELDS.weekOfYear());
        boolean isOddWeek = (weekOfYear % 2 == 1);
        if (campaignOptions.isUseStratCon()
                  && (currentDay.getDayOfWeek() == DayOfWeek.MONDAY)
                  && isOddWeek) {
            negotiateAdditionalSupportPoints(this);
        }

        processNewDayATBScenarios();

        for (AtBContract contract : getActiveAtBContracts()) {
            if (campaignOptions.isUseGenericBattleValue() &&
                      !contract.getContractType().isGarrisonType() &&
                      contract.getStartDate().equals(currentDay)) {
                Faction enemyFaction = contract.getEnemy();
                String enemyFactionCode = contract.getEnemyCode();

                boolean allowBatchalls = true;
                if (campaignOptions.isUseFactionStandingBatchallRestrictionsSafe()) {
                    double regard = factionStandings.getRegardForFaction(enemyFactionCode, true);
                    allowBatchalls = FactionStandingUtilities.isBatchallAllowed(regard);
                }

                if (enemyFaction.performsBatchalls() && allowBatchalls) {
                    PerformBatchall batchallDialog = new PerformBatchall(this,
                          contract.getClanOpponent(),
                          contract.getEnemyCode());

                    boolean batchallAccepted = batchallDialog.isBatchallAccepted();
                    contract.setBatchallAccepted(batchallAccepted);

                    if (!batchallAccepted && campaignOptions.isTrackFactionStanding()) {
                        List<String> reports = factionStandings.processRefusedBatchall(faction.getShortName(),
                              enemyFactionCode, currentDay.getYear(), campaignOptions.getRegardMultiplier());

                        for (String report : reports) {
                            addReport(report);
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes the resupply operation for a given contract.
     * <p>This method checks if the contract type is not Guerrilla Warfare or if randomInt(4) == 0. If any of
     * these conditions is met, it calculates the maximum resupply size based on the contract's required lances, creates
     * an instance of the {@link Resupply} class, and initiates a resupply action.</p>
     *
     * @param contract The relevant {@link AtBContract}
     */
    private void processResupply(AtBContract contract) {
        boolean isGuerrilla = contract.getContractType().isGuerrillaWarfare()
                                    || PIRATE_FACTION_CODE.equals(contract.getEmployerCode());

        if (!isGuerrilla || randomInt(4) == 0) {
            ResupplyType resupplyType = isGuerrilla ? ResupplyType.RESUPPLY_SMUGGLER : ResupplyType.RESUPPLY_NORMAL;
            Resupply resupply = new Resupply(this, contract, resupplyType);
            performResupply(resupply, contract);
        }
    }

    /**
     * Processes the daily activities and updates for all personnel that haven't already left the campaign.
     * <p>
     * This method iterates through all personnel and performs various daily updates, including health checks, status
     * updates, relationship events, and other daily or periodic tasks.
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
     * This method relies on several helper methods to perform specific tasks for
     * each person,
     * separating the responsibilities for modularity and readability.
     *
     * @see #getPersonnelFilteringOutDeparted() Filters out departed personnel before daily processing
     */
    public void processNewDayPersonnel() {
        RecoverMIAPersonnel recovery = new RecoverMIAPersonnel(this, faction, getAtBUnitRatingMod());
        MedicalController medicalController = new MedicalController(this);

        // This list ensures we don't hit a concurrent modification error
        List<Person> personnel = getPersonnelFilteringOutDeparted();

        // Prep some data for vocational xp
        int vocationalXpRate = campaignOptions.getVocationalXP();
        if (hasActiveContract) {
            if (campaignOptions.isUseAtB()) {
                for (AtBContract contract : getActiveAtBContracts()) {
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
        boolean isCommandersDay = isCommandersDay(currentDay) &&
                                        getCommander() != null &&
                                        campaignOptions.isShowLifeEventDialogCelebrations();
        boolean isCampaignPlanetside = location.isOnPlanet();
        boolean isUseAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        for (Person person : personnel) {
            if (person.getStatus().isDepartedUnit()) {
                continue;
            }

            PersonnelOptions personnelOptions = person.getOptions();

            // Daily events
            if (person.getStatus().isMIA()) {
                recovery.attemptRescueOfPlayerCharacter(person);
            }

            if (person.getPrisonerStatus().isBecomingBondsman()) {
                // We use 'isAfter' to avoid situations where we somehow manage to miss the
                // date.
                // This shouldn't be necessary, but a safety net never hurt
                if (currentDay.isAfter(person.getBecomingBondsmanEndDate().minusDays(1))) {
                    person.setPrisonerStatus(this, BONDSMAN, true);
                    addReport(String.format(resources.getString("becomeBondsman.text"),
                          person.getHyperlinkedName(),
                          spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                          CLOSING_SPAN_TAG));
                }
            }

            person.getATOWAttributes().setIlliterate(person.isIlliterate());

            person.resetMinutesLeft(campaignOptions.isTechsUseAdministration());
            person.setAcquisition(0);

            medicalController.processMedicalEvents(person,
                  campaignOptions.isUseAgeEffects(),
                  isClanCampaign(),
                  currentDay);

            processAnniversaries(person);

            // Weekly events
            if (currentDay.getDayOfWeek() == DayOfWeek.MONDAY) {
                if (!randomDeath.processNewWeek(this, getLocalDate(), person)) {
                    // If the character has died, we don't need to process relationship events
                    processWeeklyRelationshipEvents(person);
                }

                person.resetCurrentEdge();

                if (!person.getStatus().isMIA()) {
                    processFatigueRecovery(this, person, fieldKitchenWithinCapacity);
                }

                processCompulsionsAndMadness(person, personnelOptions, isUseAdvancedMedical, isUseFatigue);
            }

            // Monthly events
            if (currentDay.getDayOfMonth() == 1) {
                processMonthlyAutoAwards(person);

                if (vocationalXpRate > 0) {
                    if (processMonthlyVocationalXp(person, vocationalXpRate)) {
                        personnelWhoAdvancedInXP.add(person);
                    }
                }

                if (person.isCommander() &&
                          campaignOptions.isAllowMonthlyReinvestment() &&
                          !person.isHasPerformedExtremeExpenditure()) {
                    String reportString = performDiscretionarySpending(person, finances, currentDay);
                    if (reportString != null) {
                        addReport(reportString);
                    } else {
                        logger.error("Unable to process discretionary spending for {}", person.getFullTitle());
                    }
                }

                person.setHasPerformedExtremeExpenditure(false);

                int bloodmarkLevel = person.getBloodmark();
                if (bloodmarkLevel > BLOODMARK_ZERO.getLevel()) {
                    BloodmarkLevel bloodmark = BloodmarkLevel.parseBloodmarkLevelFromInt(bloodmarkLevel);
                    boolean hasAlternativeID = person.getOptions().booleanOption(ATOW_ALTERNATE_ID);
                    List<LocalDate> bloodmarkSchedule = getBloodhuntSchedule(bloodmark, currentDay, hasAlternativeID);
                    for (LocalDate assassinationAttempt : bloodmarkSchedule) {
                        person.addBloodhuntDate(assassinationAttempt);
                    }
                }

                if (currentDay.getMonthValue() % 3 == 0) {
                    if (person.hasDarkSecret()) {
                        String report = person.isDarkSecretRevealed(true, false);
                        if (report != null) {
                            addReport(report);
                        }
                    }
                }

                if (person.getBurnedConnectionsEndDate() != null) {
                    person.checkForConnectionsReestablishContact(currentDay);
                }

                if (campaignOptions.isAllowMonthlyConnections()) {
                    String report = person.performConnectionsWealthCheck(currentDay, finances);
                    if (!report.isBlank()) {
                        addReport(report);
                    }
                }
            }

            if (isCommandersDay && !faction.isClan() && (peopleWhoCelebrateCommandersDay < commanderDayTargetNumber)) {
                int age = person.getAge(currentDay);
                if (age >= 6 && age <= 12) {
                    peopleWhoCelebrateCommandersDay++;
                }
            }

            List<LocalDate> scheduledBloodhunts = person.getBloodhuntSchedule();
            if (!scheduledBloodhunts.isEmpty()) {
                boolean isDayOfBloodhunt = Bloodmark.checkForAssassinationAttempt(person,
                      currentDay,
                      isCampaignPlanetside);

                if (isDayOfBloodhunt) {
                    Bloodmark.performAssassinationAttempt(this, person, currentDay);
                }
            }
        }

        if (!personnelWhoAdvancedInXP.isEmpty()) {
            addReport(String.format(resources.getString("gainedExperience.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                  personnelWhoAdvancedInXP.size(),
                  CLOSING_SPAN_TAG));
        }

        // Commander's Day!
        if (isCommandersDay && (peopleWhoCelebrateCommandersDay >= commanderDayTargetNumber)) {
            new CommandersDayAnnouncement(this);
        }

        // Update the force icons based on the end-of-day unit status if desired
        if (MekHQ.getMHQOptions().getNewDayOptimizeMedicalAssignments()) {
            new OptimizeInfirmaryAssignments(this);
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
     * @param person               the person whose conditions are being processed
     * @param personnelOptions     the set of personnel options or traits affecting which conditions are relevant
     * @param isUseAdvancedMedical {@code true} if advanced medical rules are applied, {@code false} otherwise
     * @param isUseFatigue         {@code true} if fatigue rules are applied, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCompulsionsAndMadness(Person person, PersonnelOptions personnelOptions,
          boolean isUseAdvancedMedical, boolean isUseFatigue) {
        String gamblingReport = person.gambleWealth();
        if (!gamblingReport.isBlank()) {
            addReport(gamblingReport);
        }

        if (personnelOptions.booleanOption(COMPULSION_ADDICTION)) {
            int modifier = getCompulsionCheckModifier(COMPULSION_ADDICTION);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            person.processDiscontinuationSyndrome(this,
                  isUseAdvancedMedical,
                  isUseFatigue,
                  true,
                  failedWillpowerCheck);
        }

        if (personnelOptions.booleanOption(MADNESS_FLASHBACKS)) {
            int modifier = getCompulsionCheckModifier(MADNESS_FLASHBACKS);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            person.processCripplingFlashbacks(this,
                  isUseAdvancedMedical,
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
                addReport(report);
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
                addReport(report);
            }

            resetClinicalParanoia = false;
        }

        if (personnelOptions.booleanOption(MADNESS_REGRESSION)) {
            int modifier = getCompulsionCheckModifier(MADNESS_REGRESSION);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processChildlikeRegression(this,
                  isUseAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                addReport(report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_CATATONIA)) {
            int modifier = getCompulsionCheckModifier(MADNESS_CATATONIA);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processCatatonia(this,
                  isUseAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                addReport(report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_BERSERKER)) {
            int modifier = getCompulsionCheckModifier(MADNESS_BERSERKER);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processBerserkerFrenzy(this,
                  isUseAdvancedMedical,
                  true,
                  failedWillpowerCheck);
            if (!report.isBlank()) {
                addReport(report);
            }
        }

        if (personnelOptions.booleanOption(MADNESS_HYSTERIA)) {
            int modifier = getCompulsionCheckModifier(MADNESS_HYSTERIA);
            boolean failedWillpowerCheck = !performQuickAttributeCheck(person, SkillAttribute.WILLPOWER, null,
                  null, modifier);
            String report = person.processHysteria(this, true, isUseAdvancedMedical, failedWillpowerCheck);
            if (!report.isBlank()) {
                addReport(report);
            }

            resetClinicalParanoia = false;
        }

        // This is necessary to stop a character from getting permanently locked in a paranoia state if the
        // relevant madness are removed.
        if (resetClinicalParanoia) {
            person.setSufferingFromClinicalParanoia(false);
        }
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
     * <b>Note:</b> Bondsmen are exempt from this restriction and are eligible for
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

        if (person.isChild(currentDay)) {
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
                person.awardXP(this, vocationalXpRate);
                person.setVocationalXPTimer(0);
                return true;
            } else {
                person.setVocationalXPTimer(0);
            }
        }

        return false;
    }

    /**
     * Process weekly relationship events for a given {@link Person} on Monday. This method triggers specific events
     * related to divorce, marriage, procreation, and maternity leave.
     *
     * @param person The {@link Person} for which to process weekly relationship events
     */
    private void processWeeklyRelationshipEvents(Person person) {
        if (currentDay.getDayOfWeek() == DayOfWeek.MONDAY) {
            getDivorce().processNewWeek(this, getLocalDate(), person, false);
            getMarriage().processNewWeek(this, getLocalDate(), person, false);
            getProcreation().processNewWeek(this, getLocalDate(), person);
        }
    }

    /**
     * Process anniversaries for a given person, including birthdays and recruitment anniversaries.
     *
     * @param person The {@link Person} for whom the anniversaries will be processed
     */
    private void processAnniversaries(Person person) {
        LocalDate birthday = person.getBirthday(getGameYear());
        boolean isBirthday = birthday != null && birthday.equals(currentDay);
        int age = person.getAge(currentDay);

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

                addReport(report);
            }

            LocalDate recruitmentDate = person.getRecruitment();
            if (recruitmentDate != null) {
                LocalDate recruitmentAnniversary = recruitmentDate.withYear(getGameYear());
                int yearsOfEmployment = (int) ChronoUnit.YEARS.between(recruitmentDate, currentDay);

                if ((recruitmentAnniversary.isEqual(currentDay)) &&
                          (campaignOptions.isAnnounceRecruitmentAnniversaries())) {
                    addReport(String.format(resources.getString("anniversaryRecruitment.text"),
                          person.getHyperlinkedFullTitle(),
                          spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                          yearsOfEmployment,
                          CLOSING_SPAN_TAG,
                          name));
                }
            }
        } else if ((person.getAge(getLocalDate()) == 18) && (campaignOptions.isAnnounceChildBirthdays())) {
            if (isBirthday) {
                addReport(String.format(resources.getString("anniversaryBirthday.text"),
                      person.getHyperlinkedFullTitle(),
                      spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                      person.getAge(getLocalDate()),
                      CLOSING_SPAN_TAG));
            }
        }

        if (campaignOptions.isUseAgeEffects() && isBirthday) {
            // This is where we update all the aging modifiers for the character.
            updateAllSkillAgeModifiers(currentDay, person);
            applyAgingSPA(age, person);
        }

        // Coming of Age Events
        if (isBirthday && (person.getAge(currentDay) == 16)) {
            if (campaignOptions.isRewardComingOfAgeAbilities()) {
                SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
                singleSpecialAbilityGenerator.rollSPA(this, person);
            }

            if (campaignOptions.isRewardComingOfAgeRPSkills()) {
                AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(rskillPrefs);
                skillGenerator.generateRoleplaySkills(person);
            }

            // We want the event trigger to fire before the dialog is shown, so that the character will have finished
            // updating in the gui before the player has a chance to jump to them
            MekHQ.triggerEvent(new PersonChangedEvent(person));

            if (campaignOptions.isShowLifeEventDialogComingOfAge()) {
                new ComingOfAgeAnnouncement(this, person);
            }
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
            int dice = person.getExperienceLevel(this, false);

            if (dice > 0) {
                score = d6(dice);
            }

            multiplier += 0.5;
        }

        if (person.getSecondaryRole().isSupport(true)) {
            int dice = person.getExperienceLevel(this, true);

            if (dice > 0) {
                score += d6(dice);
            }

            multiplier += 0.5;
        } else if (person.getSecondaryRole().isNone()) {
            multiplier += 0.5;
        }

        person.changeAutoAwardSupportPoints((int) (score * multiplier));
    }

    public void processNewDayUnits() {
        // need to loop through units twice, the first time to do all maintenance and
        // the second time to do whatever else. Otherwise, maintenance minutes might
        // get sucked up by other stuff. This is also a good place to ensure that a
        // unit's engineer gets reset and updated.
        for (Unit unit : getUnits()) {
            // do maintenance checks
            try {
                unit.resetEngineer();
                if (null != unit.getEngineer()) {
                    unit.getEngineer().resetMinutesLeft(campaignOptions.isTechsUseAdministration());
                }

                doMaintenance(unit);
            } catch (Exception ex) {
                logger.error(ex,
                      "Unable to perform maintenance on {} ({}) due to an error",
                      unit.getName(),
                      unit.getId().toString());
                addReport(String.format("ERROR: An error occurred performing maintenance on %s, check the log",
                      unit.getName()));
            }
        }

        // need to check for assigned tasks in two steps to avoid
        // concurrent modification problems
        List<Part> assignedParts = new ArrayList<>();
        List<Part> arrivedParts = new ArrayList<>();
        getWarehouse().forEachPart(part -> {
            if (part instanceof Refit) {
                return;
            }

            if (part.getTech() != null) {
                assignedParts.add(part);
            }

            // If the part is currently in-transit...
            if (!part.isPresent()) {
                // ... decrement the number of days until it arrives...
                part.setDaysToArrival(part.getDaysToArrival() - 1);

                if (part.isPresent()) {
                    // ... and mark the part as arrived if it is now here.
                    arrivedParts.add(part);
                }
            }
        });

        // arrive parts before attempting refit or parts will not get reserved that day
        for (Part part : arrivedParts) {
            getQuartermaster().arrivePart(part);
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
                        fixPart(part, tech);
                    } catch (Exception ex) {
                        logger.error(ex,
                              "Could not perform overnight maintenance on {} ({}) due to an error",
                              part.getName(),
                              part.getId());
                        addReport(String.format(
                              "ERROR: an error occurred performing overnight maintenance on %s, check the log",
                              part.getName()));
                    }
                } else {
                    addReport(String.format(
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

            // check to see if this part can now be combined with other spare parts
            if (part.isSpare() && (part.getQuantity() > 0)) {
                getQuartermaster().addPart(part, 0, false);
            }
        }

        // ok now we can check for other stuff we might need to do to units
        List<UUID> unitsToRemove = new ArrayList<>();
        for (Unit unit : getUnits()) {
            if (unit.isRefitting()) {
                refit(unit.getRefit());
            }
            if (unit.isMothballing()) {
                workOnMothballingOrActivation(unit);
            }
            if (!unit.isPresent()) {
                unit.checkArrival();

                // Has unit just been delivered?
                if (unit.isPresent()) {
                    addReport(String.format(resources.getString("unitArrived.text"),
                          unit.getHyperlinkedName(),
                          spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                          CLOSING_SPAN_TAG));
                }
            }

            if (!unit.isRepairable() && !unit.hasSalvageableParts()) {
                unitsToRemove.add(unit.getId());
            }
        }
        // Remove any unrepairable, unsalvageable units
        unitsToRemove.forEach(this::removeUnit);

        // Finally, run Mass Repair Mass Salvage if desired
        if (MekHQ.getMHQOptions().getNewDayMRMS()) {
            try {
                MRMSService.mrmsAllUnits(this);
            } catch (Exception ex) {
                logger.error("Could not perform mass repair/salvage on units due to an error", ex);
                addReport("ERROR: an error occurred performing mass repair/salvage on units, check the log");
            }
        }
    }

    private void processNewDayForces() {
        // update formation levels
        Force.populateFormationLevelsFromOrigin(this);
        recalculateCombatTeams(this);

        // Update the force icons based on the end-of-day unit status if desired
        if (MekHQ.getMHQOptions().getNewDayForceIconOperationalStatus()) {
            getForces().updateForceIconOperationalStatus(this);
        }
    }

    /**
     * @return <code>true</code> if the new day arrived
     */
    public boolean newDay() {
        // clear previous retirement information
        turnoverRetirementInformation.clear();

        // Refill Automated Pools, if the options are selected
        if (MekHQ.getMHQOptions().getNewDayAstechPoolFill() && requiresAdditionalAstechs()) {
            fillAstechPool();
        }

        if (MekHQ.getMHQOptions().getNewDayMedicPoolFill() && requiresAdditionalMedics()) {
            fillMedicPool();
        }

        // Ensure we don't have anything that would prevent the new day
        if (MekHQ.triggerEvent(new DayEndingEvent(this))) {
            return false;
        }

        // Autosave based on the previous day's information
        autosaveService.requestDayAdvanceAutosave(this);

        // Advance the day by one
        final LocalDate yesterday = currentDay;
        currentDay = currentDay.plusDays(1);
        boolean isMonday = currentDay.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isFirstOfMonth = currentDay.getDayOfMonth() == 1;
        boolean isNewYear = currentDay.getDayOfYear() == 1;

        // Check for important dates
        if (campaignOptions.isShowLifeEventDialogCelebrations()) {
            fetchCelebrationDialogs();
        }

        // Determine if we have an active contract or not, as this can get used
        // elsewhere before we actually hit the AtB new day (e.g., personnel market)
        if (campaignOptions.isUseAtB()) {
            setHasActiveContract();
        }

        // Clear Reports
        currentReport.clear();
        currentReportHTML = "";
        newReports.clear();
        personnelWhoAdvancedInXP.clear();
        beginReport("<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(getLocalDate()) + "</b>");

        // New Year Changes
        if (isNewYear) {
            // News is reloaded
            reloadNews();

            // Change Year Game Option
            getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(getGameYear());

            // Degrade Regard
            List<String> degradedRegardReports = factionStandings.processRegardDegradation(faction.getShortName(),
                  currentDay.getYear(), campaignOptions.getRegardMultiplier());
            for (String report : degradedRegardReports) {
                addReport(report);
            }
        }

        readNews();

        location.newDay(this);

        updateFieldKitchenCapacity();

        processNewDayPersonnel();

        // Manage the Markets
        refreshPersonnelMarkets(false);

        // TODO : AbstractContractMarket : Uncomment
        // getContractMarket().processNewDay(this);
        unitMarket.processNewDay(this);

        // This needs to be after both personnel and markets
        if (campaignOptions.isAllowMonthlyConnections() && isFirstOfMonth) {
            checkForBurnedContacts();
        }

        // Needs to be before 'processNewDayATB' so that Dependents can't leave the
        // moment they arrive via AtB Bonus Events
        if (location.isOnPlanet() && isFirstOfMonth) {
            RandomDependents randomDependents = new RandomDependents(this);
            randomDependents.processMonthlyRemovalAndAddition();
        }

        // Process New Day for AtB
        if (campaignOptions.isUseAtB()) {
            processNewDayATB();
        }

        if (campaignOptions.getUnitRatingMethod().isCampaignOperations()) {
            processReputationChanges();
        }

        if (campaignOptions.isUseEducationModule()) {
            processEducationNewDay();
        }

        if (campaignOptions.isEnableAutoAwards() && isFirstOfMonth) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(this, false);
        }

        // Prisoner events can occur on Monday or the 1st of the month depending on the
        // type of event
        if (isMonday || isFirstOfMonth) {
            new PrisonerEventManager(this);
        }

        resetAstechMinutes();

        processNewDayUnits();

        processNewDayForces();

        if (processProcurement) {
            setShoppingList(goShopping(getShoppingList()));
        }

        // check for anything in finances
        finances.newDay(this, yesterday, getLocalDate());

        // process removal of old personnel data on the first day of each month
        if (campaignOptions.isUsePersonnelRemoval() && isFirstOfMonth) {
            performPersonnelCleanUp();
        }

        // this duplicates any turnover information so that it is still available on the
        // new day. otherwise, it's only available if the user inspects history records
        if (!turnoverRetirementInformation.isEmpty()) {
            for (String entry : turnoverRetirementInformation) {
                addReport(entry);
            }
        }

        if (topUpWeekly && isMonday) {
            int bought = stockUpPartsInUse(getPartsInUse(ignoreMothballed, false, ignoreSparesUnderQuality));
            addReport(String.format(resources.getString("weeklyStockCheck.text"), bought));
        }

        // Random Events
        if (currentDay.isAfter(GRAY_MONDAY_EVENTS_BEGIN) && currentDay.isBefore(GRAY_MONDAY_EVENTS_END)) {
            new GrayMonday(this, currentDay);
        }

        // Faction Standing
        performFactionStandingChecks(isFirstOfMonth, isNewYear);

        // This must be the last step before returning true
        MekHQ.triggerEvent(new NewDayEvent(this));
        return true;
    }

    /**
     * Checks if the commander has any burned contacts, and if so, generates and records a report.
     *
     * <p>This method is only executed if monthly connections are allowed by campaign options. If the commander
     * exists and their burned connections end date has not been set, it invokes the commander's check for burned
     * contacts on the current day. If a non-blank report is returned, the report is added to the campaign logs.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void checkForBurnedContacts() {
        if (campaignOptions.isAllowMonthlyConnections()) {
            Person commander = getCommander();
            if (commander != null && commander.getBurnedConnectionsEndDate() == null) {
                String report = commander.checkForBurnedContacts(currentDay);
                if (!report.isBlank()) {
                    addReport(report);
                }
            }
        }
    }

    /**
     * Performs all daily and periodic standing checks for factions relevant to this campaign.
     *
     * <p>On the first day of the month, this method updates the climate regard for the active campaign faction,
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
            checkForNewMercenaryOrganizationStartUp(false, false);
        }

        if (!campaignOptions.isTrackFactionStanding()) {
            return;
        }

        if (FactionStandingUltimatum.checkUltimatumForDate(currentDay,
              campaignFactionCode,
              factionStandingUltimatumsLibrary)) {
            new FactionStandingUltimatum(currentDay, this, factionStandingUltimatumsLibrary);
        }

        if (isFirstOfMonth) {
            String report = factionStandings.updateClimateRegard(faction,
                  currentDay,
                  campaignOptions.getRegardMultiplier());
            addReport(report);
        }

        List<Mission> activeMissions = getActiveMissions(false);
        boolean isInTransit = !location.isOnPlanet();
        Factions factions = Factions.getInstance();

        for (Entry<String, Double> standing : new HashMap<>(factionStandings.getAllFactionStandings()).entrySet()) {
            String relevantFactionCode = standing.getKey();
            Faction relevantFaction = factions.getFaction(relevantFactionCode);
            if (relevantFaction == null) {
                logger.warn("Unable to fetch faction standing for faction: {}", relevantFactionCode);
                continue;
            }

            // Censure check
            boolean isMercenarySpecialCase = campaignFactionCode.equals(MERCENARY_FACTION_CODE) &&
                                                   relevantFaction.isMercenaryOrganization();
            boolean isPirateSpecialCase = isPirateCampaign() &&
                                                relevantFactionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE);
            if (relevantFaction.equals(faction) || isMercenarySpecialCase || isPirateSpecialCase) {
                FactionCensureLevel newCensureLevel = factionStandings.checkForCensure(
                      relevantFaction, currentDay, activeMissions, isInTransit);
                if (newCensureLevel != null) {
                    new FactionCensureEvent(this, newCensureLevel, relevantFaction);
                }
            }

            // Accolade check
            boolean ignoreEmployer = relevantFaction.isMercenaryOrganization();
            boolean isOnMission = FactionStandingUtilities.isIsOnMission(
                  !isInTransit,
                  getActiveAtBContracts(),
                  activeMissions,
                  relevantFactionCode,
                  location.getCurrentSystem(),
                  ignoreEmployer);

            FactionAccoladeLevel newAccoladeLevel = factionStandings.checkForAccolade(
                  relevantFaction, currentDay, isOnMission);

            if (newAccoladeLevel != null) {
                new FactionAccoladeEvent(this, relevantFaction, newAccoladeLevel,
                      faction.equals(relevantFaction));
            }
        }

        // Censure degradation
        factionStandings.processCensureDegradation(currentDay);
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
            logger.warn("Unable to find a suitable faction for a new mercenary organization start up");
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
     * @param isCampaignStart {@code true} if this method is being called at the start of the campaign
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void refreshPersonnelMarkets(boolean isCampaignStart) {
        PersonnelMarketStyle marketStyle = campaignOptions.getPersonnelMarketStyle();
        if (marketStyle == PERSONNEL_MARKET_DISABLED) {
            personnelMarket.generatePersonnelForDay(this);
        } else {
            if (currentDay.getDayOfMonth() == 1) {
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

    /**
     * Performs cleanup of departed personnel by identifying and removing eligible personnel records.
     *
     * <p>This method uses the {@link AutomatedPersonnelCleanUp} utility to determine which {@link Person}
     * objects should be removed from the campaign based on current date and campaign configuration options. Identified
     * personnel are then removed, and a report entry is generated if any removals occur.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void performPersonnelCleanUp() {
        AutomatedPersonnelCleanUp removal = new AutomatedPersonnelCleanUp(currentDay,
              getPersonnel(),
              campaignOptions.isUseRemovalExemptRetirees(),
              campaignOptions.isUseRemovalExemptCemetery());

        List<Person> personnelToRemove = removal.getPersonnelToCleanUp();
        for (Person person : personnelToRemove) {
            removePerson(person, false);
        }

        if (!personnelToRemove.isEmpty()) {
            addReport(resources.getString("personnelRemoval.text"));
        }
    }

    /**
     * Fetches and handles the celebration dialogs specific to the current day.
     *
     * <p><b>Note:</b> Commanders day is handled as a part of the personnel processing, so we don't need to parse
     * personnel twice.</p>
     */
    private void fetchCelebrationDialogs() {
        if (!faction.isClan()) {
            if (isWinterHolidayMajorDay(currentDay)) {
                new WinterHolidayAnnouncement(this);
            }

            if (isFreedomDay(currentDay)) {
                new FreedomDayAnnouncement(this);
            }
        }

        if (isNewYear(currentDay)) {
            new NewYearsDayAnnouncement(this);
        }
    }

    /**
     * Updates the status of whether field kitchens are operating within their required capacity.
     *
     * <p>If fatigue is enabled in the campaign options, this method calculates the total available
     * field kitchen capacity and the required field kitchen usage, then updates the {@code fieldKitchenWithinCapacity}
     * flag to reflect whether the capacity meets the demand. If fatigue is disabled, the capacity is automatically set
     * to {@code false}.</p>
     */
    private void updateFieldKitchenCapacity() {
        if (campaignOptions.isUseFatigue()) {
            int fieldKitchenCapacity = checkFieldKitchenCapacity(getForce(FORCE_ORIGIN).getAllUnitsAsUnits(units,
                  false), campaignOptions.getFieldKitchenCapacity());
            int fieldKitchenUsage = checkFieldKitchenUsage(getActivePersonnel(false),
                  campaignOptions.isUseFieldKitchenIgnoreNonCombatants());
            fieldKitchenWithinCapacity = areFieldKitchensWithinCapacity(fieldKitchenCapacity, fieldKitchenUsage);
        } else {
            fieldKitchenWithinCapacity = false;
        }
    }

    /**
     * Processes reputation changes based on various conditions.
     */
    private void processReputationChanges() {
        if (faction.isPirate()) {
            dateOfLastCrime = currentDay;
            crimePirateModifier = -100;
        }

        if (currentDay.getDayOfMonth() == 1) {
            if (dateOfLastCrime != null) {
                long yearsBetween = ChronoUnit.YEARS.between(currentDay, dateOfLastCrime);

                int remainingCrimeChange = 2;

                if (yearsBetween >= 1) {
                    if (crimePirateModifier < 0) {
                        remainingCrimeChange = max(0, 2 + crimePirateModifier);
                        changeCrimePirateModifier(2); // this is the amount of change specified by CamOps
                    }

                    if (crimeRating < 0 && remainingCrimeChange > 0) {
                        changeCrimeRating(remainingCrimeChange);
                    }
                }
            }
        }

        if (currentDay.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            reputation.initializeReputation(this);
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
     * This method checks if any students in the academy should graduate, and updates their attributes and status
     * accordingly. If any students do graduate, it sends the graduation information to autoAwards.
     */
    private void processEducationNewDay() {
        List<UUID> graduatingPersonnel = new ArrayList<>();
        HashMap<UUID, List<Object>> academyAttributesMap = new HashMap<>();

        for (Person person : getStudents()) {
            List<Object> individualAcademyAttributes = new ArrayList<>();

            if (EducationController.processNewDay(this, person, false)) {
                Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

                if (academy == null) {
                    logger.debug("Found null academy for {} skipping", person.getFullTitle());
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
            autoAwardsController.PostGraduationController(this, graduatingPersonnel, academyAttributesMap);
        }
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

    /**
     * Use {@link #getCommander()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public Person getSeniorCommander() {
        Person commander = null;
        for (Person person : getActivePersonnel(true)) {
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
        addReport(unit.getName() + " has been removed from the unit roster.");
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
        removeAllPatientsFor(person);
        person.removeAllTechJobs(this);
        removeKillsFor(person.getId());
        getRetirementDefectionTracker().removePerson(person);
        if (log) {
            addReport(person.getFullTitle() + " has been removed from the personnel roster.");
        }

        personnel.remove(person.getId());

        // Deal with Astech Pool Minutes
        if (person.getPrimaryRole().isAstech()) {
            astechPoolMinutes = max(0, astechPoolMinutes - Person.PRIMARY_ROLE_SUPPORT_TIME);
            astechPoolOvertime = max(0, astechPoolOvertime - Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME);
        } else if (person.getSecondaryRole().isAstech()) {
            astechPoolMinutes = max(0, astechPoolMinutes - Person.SECONDARY_ROLE_SUPPORT_TIME);
            astechPoolOvertime = max(0, astechPoolOvertime - Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME);
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

            // run through the stratcon campaign state where applicable and remove the
            // "parent" scenario as well
            if ((mission instanceof AtBContract) &&
                      (((AtBContract) mission).getStratconCampaignState() != null) &&
                      (scenario instanceof AtBDynamicScenario)) {
                ((AtBContract) mission).getStratconCampaignState().removeStratconScenario(scenario.getId());
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
                for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
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
     * @param r - the report String
     */
    public void beginReport(String r) {
        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            // add the new items to our in-memory cache
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), ""));
        }
        addReportInternal(r);
    }

    /**
     * Formats and then adds a report to the daily log
     *
     * @param format  String with format markers.
     * @param objects Variable list of objects to format into {@code format}
     */
    public void addReport(final String format, final Object... objects) {
        addReport(String.format(format, objects));
    }

    /**
     * Adds a report to the daily log
     *
     * @param r - the report String
     */
    public void addReport(String r) {
        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), r));
        }
        addReportInternal(r);
    }

    private void addReportInternal(String r) {
        currentReport.add(r);
        if (!currentReportHTML.isEmpty()) {
            currentReportHTML = currentReportHTML + REPORT_LINEBREAK + r;
            newReports.add(REPORT_LINEBREAK);
        } else {
            currentReportHTML = r;
        }
        newReports.add(r);
        MekHQ.triggerEvent(new ReportEvent(this, r));
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
        addReport("Funds added : " + quantityString + " (" + description + ')');
    }

    public void removeFunds(final TransactionType type, final Money quantity, @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }

        finances.debit(type, getLocalDate(), quantity, description);
        String quantityString = quantity.toAmountAndSymbolString();
        addReport("Funds removed : " + quantityString + " (" + description + ')');
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
        addReport("Funds removed : " + quantityString + " (" + description + ')');

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
     * <p>he new capacity is constrained to be at least the minimum allowed temporary capacity, as defined by {@code
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

    public void writeToXML(final PrintWriter writer) {
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
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "astechPool", astechPool);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "astechPoolMinutes", astechPoolMinutes);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "astechPoolOvertime", astechPoolOvertime);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "medicPool", medicPool);
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
        for (String s : currentReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            writer.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + s + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "currentReport");

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
        rskillPrefs.writeToXML(writer, indent);

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
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "shipSearchStart", getShipSearchStart());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "shipSearchType", shipSearchType);
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "shipSearchResult", shipSearchResult);
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "shipSearchExpiration", getShipSearchExpiration());
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

        if (MekHQ.getMHQOptions().getWriteCustomsToXML()) {
            writeCustoms(writer);
        }

        // Okay, we're done.
        // Close everything out and be done with it.
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "campaign");
    }

    private void writeCustoms(PrintWriter pw1) {
        for (String name : customs) {
            MekSummary ms = MekSummaryCache.getInstance().getMek(name);
            if (ms == null) {
                continue;
            }

            MekFileParser mekFileParser = null;
            try {
                mekFileParser = new MekFileParser(ms.getSourceFile());
            } catch (EntityLoadingException ex) {
                logger.error("", ex);
            }
            if (mekFileParser == null) {
                continue;
            }
            Entity en = mekFileParser.getEntity();
            pw1.println("\t<custom>");
            pw1.println("\t\t<name>" + name + "</name>");
            if (en instanceof Mek) {
                pw1.print("\t\t<mtf><![CDATA[");
                pw1.print(((Mek) en).getMtf());
                pw1.println("]]></mtf>");
            } else {
                try {
                    BuildingBlock blk = BLKFile.getBlock(en);
                    pw1.print("\t\t<blk><![CDATA[");
                    for (String s : blk.getAllDataAsString()) {
                        if (s.isEmpty()) {
                            continue;
                        }
                        pw1.println(s);
                    }
                    pw1.println("]]></blk>");
                } catch (EntitySavingException e) {
                    logger.error("Failed to save custom entity {}", en.getDisplayName(), e);
                }
            }
            pw1.println("\t</custom>");
        }
    }

    public ArrayList<PlanetarySystem> getSystems() {
        ArrayList<PlanetarySystem> systems = new ArrayList<>();
        for (String key : Systems.getInstance().getSystems().keySet()) {
            systems.add(Systems.getInstance().getSystems().get(key));
        }
        return systems;
    }

    public PlanetarySystem getSystemById(String id) {
        return Systems.getInstance().getSystemById(id);
    }

    public Vector<String> getSystemNames() {
        Vector<String> systemNames = new Vector<>();
        for (PlanetarySystem key : Systems.getInstance().getSystems().values()) {
            systemNames.add(key.getPrintableName(getLocalDate()));
        }
        return systemNames;
    }

    public PlanetarySystem getSystemByName(String name) {
        return Systems.getInstance().getSystemByName(name, getLocalDate());
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

        if (!skipAccessCheck
                  && campaignOptions.isUseFactionStandingOutlawedSafe()) {
            FactionHints factionHints = FactionHints.defaultFactionHints();
            boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(faction, factionStandings,
                  getCurrentSystem(), end, currentDay, activeAtBContracts, factionHints);
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
        Systems systemsInstance = Systems.getInstance();
        Map<String, PlanetarySystem> allSystems = systemsInstance.getSystems();

        for (Entry<String, PlanetarySystem> entry : allSystems.entrySet()) {
            scoreH.put(entry.getKey(), end.getDistanceTo(entry.getValue()));
        }

        // Initialize starting node
        String current = startKey;
        scoreG.put(current, 0.0);
        closed.add(current);

        FactionHints factionHints = FactionHints.defaultFactionHints();

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
                if (isAvoidingEmptySystems && neighborSystem.getPopulation(currentDay) == 0) {
                    return;
                }

                // Skip systems where the campaign is outlawed
                if (!skipAccessCheck
                          && campaignOptions.isUseFactionStandingOutlawedSafe()) {
                    boolean canAccessSystem = FactionStandingUtilities.canEnterTargetSystem(faction, factionStandings,
                          getCurrentSystem(), neighborSystem, currentDay, activeAtBContracts, factionHints);
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
     */
    public Money calculateCostPerJump(boolean excludeOwnTransports, boolean campaignOpsCosts) {
        HangarStatistics stats = getHangarStatistics();
        CargoStatistics cargoStats = getCargoStatistics();

        Money collarCost = Money.of(campaignOpsCosts ? 100000 : 50000);

        // first we need to get the total number of units by type
        int nMek = stats.getNumberOfUnitsByType(Entity.ETYPE_MEK);
        int nLVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int nHVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int nAero = stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACEFIGHTER);
        int nSC = stats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int nCF = stats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int nBA = stats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int nMekInf = 0;
        int nMotorInf = 0;
        int nFootInf = 0;
        int nProto = stats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK);
        int nDropship = stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int nCollars = stats.getTotalDockingCollars();
        double nCargo = cargoStats.getTotalCargoCapacity(); // ignoring refrigerated/insulated/etc.

        // get cargo tonnage including parts in transit, then get mothballed unit tonnage
        double carriedCargo = cargoStats.getCargoTonnage(true, false) + cargoStats.getCargoTonnage(false, true);

        // calculate the number of units left not transported
        int noMek = max(nMek - stats.getOccupiedBays(Entity.ETYPE_MEK), 0);
        int noDS = max(nDropship - stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = max(nSC - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCF = max(nCF - stats.getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = max(nAero - stats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER), 0);
        int nolv = max(nLVee - stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = max(nHVee - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        //TODO: Do capacity calculations for Infantry, too.
        int noinf = max(stats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) -
                              stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = max(nBA - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = max(nProto - stats.getOccupiedBays(Entity.ETYPE_PROTOMEK), 0);
        int freehv = max(stats.getTotalHeavyVehicleBays() - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeinf = max(stats.getTotalInfantryBays() - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int freeba = max(stats.getTotalBattleArmorBays() - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int freeSC = max(stats.getTotalSmallCraftBays() - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCargo = (int) Math.ceil(max(carriedCargo - nCargo, 0));

        int newNoASF = max(noASF - freeSC, 0);
        int placedASF = max(noASF - newNoASF, 0);
        freeSC -= placedASF;

        int newNolv = max(nolv - freehv, 0);
        int placedlv = max(nolv - newNolv, 0);
        freehv -= placedlv;
        int noVehicles = (nohv + newNolv);

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
                    mekCollars += 1;
                } else {
                    leasedLargeMekDropships += 0.5;
                    noMek -= (int) (largeMekDropshipMekCapacity / 0.5);
                    mekCollars += 1;
                }
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

        // Leopards for the rest, no halvsies here
        if (noMek > 0) {
            leasedSmallMekDropships = Math.ceil(noMek / (double) smallMekDropshipMekCapacity);
            noMek -= (int) (leasedSmallMekDropships * smallMekDropshipMekCapacity);
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

        // Gazelles
        // Gazelles are pretty minimal, so no halfsies.
        if (noVehicles > 0) {
            leasedAvgVehicleDropships = Math.ceil((nohv + newNolv) / (double) avgVehicleDropshipVehicleCapacity);
            noVehicles = (int) ((nohv + newNolv) - leasedAvgVehicleDropships * avgVehicleDropshipVehicleCapacity);
            vehicleCollars += (int) Math.ceil(leasedAvgVehicleDropships);

            if (noVehicles > 0) { //shouldn't be necessary, but check?
                leasedAvgVehicleDropships += 1;
                noVehicles -= avgVehicleDropshipVehicleCapacity;
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

        int actualSkillLevel = EXP_NONE;
        if (skill != null) {
            actualSkillLevel = skill.getExperienceLevel(tech.getOptions(), tech.getATOWAttributes());
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

        // this is ugly, if the mode penalty drops you to green, you drop two
        // levels instead of two
        int value = skill.getFinalSkillValue(tech.getOptions(), tech.getATOWAttributes()) + modePenalty;
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
            logger.error("Attempting to get the target number for a part with zero time left.");
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "No part repair time remaining.");
        }

        int helpMod;
        if ((partWork.getUnit() != null) && partWork.getUnit().isSelfCrewed()) {
            helpMod = getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
        } else {
            final int helpers = getAvailableAstechs(minutes, isOvertime);
            helpMod = getShorthandedMod(helpers, false);
            // we may have just gone overtime with our helpers
            if (!isOvertime && (astechPoolMinutes < (minutes * helpers))) {
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

    public TargetRoll getTargetForMaintenance(IPartWork partWork, Person tech, int asTechsUsed) {
        int value = 10;
        String skillLevel = "Unmaintained";
        if (null != tech) {
            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (null != skill) {
                value = skill.getFinalSkillValue(tech.getOptions(), tech.getATOWAttributes());
                skillLevel = skill.getSkillLevel(tech.getOptions(), tech.getATOWAttributes()).toString();
            }
        }

        TargetRoll target = new TargetRoll(value, skillLevel);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllModsForMaintenance());

        if (getCampaignOptions().isUseEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        if (partWork.getUnit().getSite() < SITE_FACILITY_BASIC) {
            if (getLocation().isOnPlanet() && campaignOptions.isUsePlanetaryModifiers()) {
                Planet planet = getLocation().getPlanet();
                Atmosphere atmosphere = planet.getAtmosphere(getLocalDate());
                megamek.common.planetaryconditions.Atmosphere planetaryConditions = planet.getPressure(getLocalDate());
                int temperature = planet.getTemperature(getLocalDate());

                if (planet.getGravity() < 0.8) {
                    target.addModifier(2, "Low Gravity");
                } else if (planet.getGravity() >= 2.0) {
                    target.addModifier(4, "Very High Gravity");
                } else if (planet.getGravity() > 1.2) {
                    target.addModifier(1, "High Gravity");
                }

                if (atmosphere.isTainted() || atmosphere.isToxic()) {
                    target.addModifier(2, "Tainted or Toxic Atmosphere");
                } else if (planetaryConditions.isVacuum()) {
                    target.addModifier(2, "Vacuum");
                }

                if (planetaryConditions.isTrace() || planetaryConditions.isVeryHigh()) {
                    target.addModifier(1, "Trace or Very High Pressure Atmosphere");
                }

                if (temperature < -30 || temperature > 50) {
                    target.addModifier(1, "Extreme Temperature");
                }
            }
        }

        if (null != partWork.getUnit() && null != tech) {
            // the astech issue is crazy, because you can actually be better off
            // not maintaining
            // than going it short-handed, but that is just the way it is.
            // Still, there is also some fuzziness about what happens if you are
            // short astechs
            // for part of the cycle.
            final int helpMod;
            if (partWork.getUnit().isSelfCrewed()) {
                helpMod = getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
            } else {
                helpMod = getShorthandedMod(asTechsUsed, false);
            }

            if (helpMod > 0) {
                target.addModifier(helpMod, "shorthanded");
            }

            // like repairs, per CamOps page 208 extra time gives a
            // reduction to the TN based on x2, x3, x4
            if (partWork.getUnit().getMaintenanceMultiplier() > 1) {
                target.addModifier(-(partWork.getUnit().getMaintenanceMultiplier() - 1), "extra time");
            }
        }

        return target;
    }

    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition) {
        return getTargetForAcquisition(acquisition, getLogisticsPerson());
    }

    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition, final @Nullable Person person) {
        return getTargetForAcquisition(acquisition, person, false);
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
     * Determines the target roll required for successfully acquiring a specific part or unit based on various campaign
     * settings, the acquisition details, and the person attempting the acquisition.
     *
     * <p>
     * This method evaluates multiple conditions and factors to calculate the target roll, returning one of the
     * following outcomes:
     * <ul>
     * <li>{@code TargetRoll.AUTOMATIC_SUCCESS} if acquisitions are set to be
     * automatic in the campaign options.</li>
     * <li>{@code TargetRoll.IMPOSSIBLE} if the acquisition is not permitted based
     * on campaign settings,
     * such as missing personnel, parts restrictions, or unavailable
     * technology.</li>
     * <li>A calculated target roll value based on the skill of the assigned person,
     * acquisition modifiers,
     * and adjustments for specific campaign rules (e.g., {@code AtB}
     * restrictions).</li>
     * </ul>
     *
     * @param acquisition the {@link IAcquisitionWork} object containing details about the requested part or supply,
     *                    such as tech base, technology level, and availability.
     *
     * @return a {@link TargetRoll} object representing the roll required to successfully acquire the requested item, or
     *       an impossible/automatic result under specific circumstances.
     */
    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition, final @Nullable Person person,
          final boolean checkDaysToWait) {
        if (getCampaignOptions().getAcquisitionSkill().equals(S_AUTO)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "Automatic Success");
        }

        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                  "Your procurement personnel have used up all their acquisition attempts for this period");
        }
        final Skill skill = person.getSkillForWorkingOn(getCampaignOptions().getAcquisitionSkill());
        if (null != getShoppingList().getShoppingItem(acquisition.getNewEquipment()) && checkDaysToWait) {
            return new TargetRoll(TargetRoll.AUTOMATIC_FAIL,
                  "You must wait until the new cycle to check for this part. Further" +
                        " attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == Part.TechBase.CLAN && !getCampaignOptions().isAllowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == Part.TechBase.IS && !getCampaignOptions().isAllowISPurchases()) {
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

        int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
              isClanCampaign(),
              currentDay,
              person.getRankNumeric());

        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(person.getOptions(), person.getATOWAttributes()),
              skill.getSkillLevel(person.getOptions(), person.getATOWAttributes(), adjustedReputation).toString());
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

    public void resetAstechMinutes() {
        astechPoolMinutes = Person.PRIMARY_ROLE_SUPPORT_TIME * getNumberPrimaryAstechs() +
                                  Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberSecondaryAstechs();
        astechPoolOvertime = Person.SECONDARY_ROLE_SUPPORT_TIME * getNumberPrimaryAstechs() +
                                   Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberSecondaryAstechs();
    }

    public void setAstechPoolMinutes(int minutes) {
        astechPoolMinutes = minutes;
    }

    public int getAstechPoolMinutes() {
        return astechPoolMinutes;
    }

    public void setAstechPoolOvertime(int overtime) {
        astechPoolOvertime = overtime;
    }

    public int getAstechPoolOvertime() {
        return astechPoolOvertime;
    }

    public int getPossibleAstechPoolMinutes() {
        return 480 * getNumberPrimaryAstechs() + 240 * getNumberSecondaryAstechs();
    }

    public int getPossibleAstechPoolOvertime() {
        return 240 * getNumberPrimaryAstechs() + 120 * getNumberSecondaryAstechs();
    }

    public void setAstechPool(int size) {
        astechPool = size;
    }

    public int getAstechPool() {
        return astechPool;
    }

    public void setMedicPool(int size) {
        medicPool = size;
    }

    public int getMedicPool() {
        return medicPool;
    }

    public boolean requiresAdditionalAstechs() {
        return getAstechNeed() > 0;
    }

    public int getAstechNeed() {
        return (Math.toIntExact(getActivePersonnel(true).stream().filter(Person::isTech).count()) *
                      MHQConstants.ASTECH_TEAM_SIZE) -
                     getNumberAstechs();
    }

    public void increaseAstechPool(int i) {
        astechPool += i;
        astechPoolMinutes += (480 * i);
        astechPoolOvertime += (240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, i));
    }

    public void fillAstechPool() {
        final int need = getAstechNeed();
        if (need > 0) {
            increaseAstechPool(need);
        }
    }

    public void decreaseAstechPool(int i) {
        astechPool = max(0, astechPool - i);
        // always assume that we fire the ones who have not yet worked
        astechPoolMinutes = max(0, astechPoolMinutes - 480 * i);
        astechPoolOvertime = max(0, astechPoolOvertime - 240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, -i));
    }

    public int getNumberAstechs() {
        return getNumberPrimaryAstechs() + getNumberSecondaryAstechs();
    }

    /**
     * Returns the total number of primary astechs available.
     * <p>
     * This method calculates the number of astechs by adding the base astech pool to the count of active personnel
     * whose primary role is an astech, who are not currently deployed, and are employed.
     * </p>
     *
     * @return the total number of primary astechs
     */
    public int getNumberPrimaryAstechs() {
        int astechs = getAstechPool();
        for (Person person : getActivePersonnel(false)) {
            if (person.getPrimaryRole().isAstech() && !person.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    /**
     * Returns the total number of secondary astechs available.
     * <p>
     * This method calculates the number of astechs by adding the base astech pool to the count of active personnel
     * whose secondary role is an astech, who are not currently deployed, and are employed.
     * </p>
     *
     * @return the total number of secondary astechs
     */
    public int getNumberSecondaryAstechs() {
        int astechs = 0;
        for (Person person : getActivePersonnel(false)) {
            if (person.getSecondaryRole().isAstech() && !person.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getAvailableAstechs(final int minutes, final boolean alreadyOvertime) {
        if (minutes == 0) {
            // If 0 Astechs are assigned to the task, return 0 minutes used
            return 0;
        }

        int availableHelp = (int) floor(((double) astechPoolMinutes) / minutes);
        if (isOvertimeAllowed() && (availableHelp < MHQConstants.ASTECH_TEAM_SIZE)) {
            // if we are less than fully staffed, then determine whether
            // we should dip into overtime or just continue as short-staffed
            final int shortMod = getShorthandedMod(availableHelp, false);
            final int remainingMinutes = astechPoolMinutes - availableHelp * minutes;
            final int extraHelp = (remainingMinutes + astechPoolOvertime) / minutes;
            final int helpNeeded = MHQConstants.ASTECH_TEAM_SIZE - availableHelp;
            if (alreadyOvertime && (shortMod > 0)) {
                // then add whatever we can
                availableHelp += extraHelp;
            } else if (shortMod > 3) {
                // only dip in if we can bring ourselves up to full
                if (extraHelp >= helpNeeded) {
                    availableHelp = MHQConstants.ASTECH_TEAM_SIZE;
                }
            }
        }
        return Math.min(Math.min(availableHelp, MHQConstants.ASTECH_TEAM_SIZE), getNumberAstechs());
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
        int ndocs = getDoctors().size();
        int nmedics = getNumberMedics();
        if (ndocs == 0) {
            return 0;
        }
        // TODO: figure out what to do with fractions
        return Math.min(nmedics / ndocs, 4);
    }

    /**
     * @return the number of medics in the campaign including any in the temporary medic pool
     */
    public int getNumberMedics() {
        int count = 0;
        for (Person person : getActivePersonnel(false)) {
            if ((person.getPrimaryRole().isMedic() || person.getSecondaryRole().isMedic()) &&
                      !person.isDeployed() &&
                      person.isEmployed()) {
                count++;
            }
        }
        return getMedicPool() + count;
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
     * Returns the text representation of the unit rating based on the selected unit rating method. If the unit rating
     * method is FMMR, the unit rating value is returned. If the unit rating method is Campaign Operations, the
     * reputation rating and unit rating modification are combined and returned. If the unit rating method is neither
     * FMMR nor Campaign Operations, "N/A" is returned.
     *
     * @return The text representation of the unit rating
     */
    public String getUnitRatingText() {
        UnitRatingMethod unitRatingMethod = campaignOptions.getUnitRatingMethod();

        if (unitRatingMethod.isFMMR()) {
            return getUnitRating().getUnitRating();
        } else if (unitRatingMethod.isCampaignOperations()) {
            return String.valueOf(reputation.getReputationRating());
        } else {
            return "N/A";
        }
    }

    /**
     * Retrieves the unit rating modifier based on campaign options. If the unit rating method is not enabled, it
     * returns the default value of IUnitRating.DRAGOON_C. If the unit rating method uses FMMR, it returns the unit
     * rating as an integer. Otherwise, it calculates the modifier using the getAtBModifier method.
     *
     * @return The unit rating modifier based on the campaign options.
     */
    public int getAtBUnitRatingMod() {
        if (!getCampaignOptions().getUnitRatingMethod().isEnabled()) {
            return IUnitRating.DRAGOON_C;
        }

        return getCampaignOptions().getUnitRatingMethod().isFMMR() ?
                     getUnitRating().getUnitRatingAsInteger() :
                     reputation.getAtbModifier();
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

        Skill strategy = commander.getSkill(S_STRATEGY);

        return strategy.getTotalSkillLevel(commander.getOptions(), commander.getATOWAttributes());
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return rskillPrefs;
    }

    public void setRandomSkillPreferences(RandomSkillPreferences prefs) {
        rskillPrefs = prefs;
    }

    /**
     * @param planet the starting planet, or null to use the faction default
     */
    public void setStartingSystem(final @Nullable Planet planet) {
        PlanetarySystem startingSystem;
        if (planet == null) {
            final Map<String, PlanetarySystem> systemList = Systems.getInstance().getSystems();
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
        final Boolean allowDuplicatePortraits = getCampaignOptions().isAllowDuplicatePortraits();
        final Portrait portrait = RandomPortraitGenerator.generate(getPersonnel(), person, allowDuplicatePortraits);
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

    public void addUnitsToNetwork(Vector<Unit> addedUnits, String netid) {
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
            if (null != unit.getEntity().getC3NetId() && unit.getEntity().getC3NetId().equals(netid)) {
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
                        addReport(financeResources.getString("DistributedShares.text"),
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
                addReport("Your account has been credited for " +
                                remainingMoney.toAmountAndSymbolString() +
                                " for the remaining payout from contract " +
                                contract.getHyperlinkedName());
            } else if (remainingMoney.isNegative()) {
                getFinances().credit(TransactionType.CONTRACT_PAYMENT,
                      getLocalDate(),
                      remainingMoney,
                      "Repaying payment overages for " + contract.getName());
                addReport("Your account has been debited for " +
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
        if (part instanceof Armor) { // ProtoMek Armor and BaArmor are derived from Armor
            countModifier = "points";
        }
        if (part instanceof AmmoStorage) {
            countModifier = "shots";
        }

        inventory.setCountModifier(countModifier);
        return inventory;
    }

    public void addLoan(Loan loan) {
        addReport("You have taken out loan " +
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
            addReport("You have paid off the remaining loan balance of " +
                            loan.determineRemainingValue().toAmountAndSymbolString() +
                            " on " +
                            loan);
            finances.removeLoan(loan);
            MekHQ.triggerEvent(new LoanPaidEvent(loan));
        } else {
            addReport("<font color='" +
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

    public void doMaintenance(Unit unit) {
        if (!unit.requiresMaintenance() || !campaignOptions.isCheckMaintenance()) {
            return;
        }
        // let's start by checking times
        int minutesUsed = unit.getMaintenanceTime();
        int asTechsUsed = 0;
        boolean maintained = false;
        boolean paidMaintenance = true;

        unit.incrementDaysSinceMaintenance(this, maintained, asTechsUsed);

        int ruggedMultiplier = 1;
        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_1)) {
            ruggedMultiplier = 2;
        }

        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_2)) {
            ruggedMultiplier = 3;
        }

        if (unit.getDaysSinceMaintenance() >= (getCampaignOptions().getMaintenanceCycleDays() * ruggedMultiplier)) {
            Person tech = unit.getTech();
            if (tech != null) {
                int availableMinutes = tech.getMinutesLeft();
                maintained = (availableMinutes >= minutesUsed);

                if (!maintained) {
                    // At this point, insufficient minutes is the only reason why this would be failed.
                    addReport(String.format(resources.getString("maintenanceNotAvailable.text"), unit.getName()));
                } else {
                    maintained = !tech.isMothballing();
                }

                if (maintained) {
                    tech.setMinutesLeft(availableMinutes - minutesUsed);
                    asTechsUsed = getAvailableAstechs(minutesUsed, false);
                    astechPoolMinutes -= asTechsUsed * minutesUsed;
                }
            }

            // maybe use the money
            if (campaignOptions.isPayForMaintain()) {
                if (!(finances.debit(TransactionType.MAINTENANCE,
                      getLocalDate(),
                      unit.getMaintenanceCost(),
                      "Maintenance for " + unit.getName()))) {
                    addReport("<font color='" +
                                    ReportingUtilities.getNegativeColor() +
                                    "'><b>You cannot afford to pay maintenance costs for " +
                                    unit.getHyperlinkedName() +
                                    "!</b></font>");
                    paidMaintenance = false;
                }
            }
            // it is time for a maintenance check
            PartQuality qualityOrig = unit.getQuality();
            String techName = "Nobody";
            String techNameLinked = techName;
            if (null != tech) {
                techName = tech.getFullTitle();
                techNameLinked = tech.getHyperlinkedFullTitle();
            }
            // don't do actual damage until we clear the for loop to avoid
            // concurrent mod problems
            // put it into a hash - 4 points of damage will mean destruction
            Map<Part, Integer> partsToDamage = new HashMap<>();
            StringBuilder maintenanceReport = new StringBuilder("<strong>" +
                                                                      techName +
                                                                      " performing maintenance</strong><br><br>");
            for (Part part : unit.getParts()) {
                try {
                    String partReport = doMaintenanceOnUnitPart(unit,
                          part,
                          partsToDamage,
                          paidMaintenance,
                          asTechsUsed);
                    if (partReport != null) {
                        maintenanceReport.append(partReport).append("<br>");
                    }
                } catch (Exception ex) {
                    logger.error(ex,
                          "Could not perform maintenance on part {} ({}) for {} ({}) due to an error",
                          part.getName(),
                          part.getId(),
                          unit.getName(),
                          unit.getId().toString());
                    addReport(String.format(
                          "ERROR: An error occurred performing maintenance on %s for unit %s, check the log",
                          part.getName(),
                          unit.getName()));
                }
            }

            int nDamage = 0;
            int nDestroy = 0;
            for (Entry<Part, Integer> p : partsToDamage.entrySet()) {
                int damage = p.getValue();
                if (damage > 3) {
                    nDestroy++;
                    p.getKey().remove(false);
                } else {
                    p.getKey().doMaintenanceDamage(damage);
                    nDamage++;
                }
            }

            unit.setLastMaintenanceReport(maintenanceReport.toString());

            if (getCampaignOptions().isLogMaintenance()) {
                logger.info(maintenanceReport.toString());
            }

            PartQuality quality = unit.getQuality();
            String qualityString;
            boolean reverse = getCampaignOptions().isReverseQualityNames();
            if (quality.toNumeric() > qualityOrig.toNumeric()) {
                qualityString = ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorPositiveHexColor(),
                      "Overall quality improves from " +
                            qualityOrig.toName(reverse) +
                            " to " +
                            quality.toName(reverse));
            } else if (quality.toNumeric() < qualityOrig.toNumeric()) {
                qualityString = ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorNegativeHexColor(),
                      "Overall quality declines from " +
                            qualityOrig.toName(reverse) +
                            " to " +
                            quality.toName(reverse));
            } else {
                qualityString = "Overall quality remains " + quality.toName(reverse);
            }
            String damageString = "";
            if (nDamage > 0) {
                damageString += nDamage + " parts were damaged. ";
            }
            if (nDestroy > 0) {
                damageString += nDestroy + " parts were destroyed.";
            }
            if (!damageString.isEmpty()) {
                damageString = "<b><font color='" +
                                     ReportingUtilities.getNegativeColor() +
                                     "'>" +
                                     damageString +
                                     "</b></font> [<a href='REPAIR|" +
                                     unit.getId() +
                                     "'>Repair bay</a>]";
            }
            String paidString = "";
            if (!paidMaintenance) {
                paidString = "<font color='" +
                                   ReportingUtilities.getNegativeColor() +
                                   "'>Could not afford maintenance costs, so check is at a penalty.</font>";
            }
            addReport(techNameLinked +
                            " performs maintenance on " +
                            unit.getHyperlinkedName() +
                            ". " +
                            paidString +
                            qualityString +
                            ". " +
                            damageString +
                            " [<a href='MAINTENANCE|" +
                            unit.getId() +
                            "'>Get details</a>]");

            unit.resetDaysSinceMaintenance();
        }
    }

    private String doMaintenanceOnUnitPart(Unit unit, Part part, Map<Part, Integer> partsToDamage,
          boolean paidMaintenance, int asTechsUsed) {
        String partReport = "<b>" + part.getName() + "</b> (Quality " + part.getQualityName() + ')';
        if (!part.needsMaintenance()) {
            return null;
        }
        PartQuality oldQuality = part.getQuality();
        TargetRoll target = getTargetForMaintenance(part, unit.getTech(), asTechsUsed);
        if (!paidMaintenance) {
            // TODO : Make this modifier user inputable
            target.addModifier(1, "did not pay for maintenance");
        }

        partReport += ", TN " + target.getValue() + '[' + target.getDesc() + ']';
        int roll = d6(2);
        int margin = roll - target.getValue();
        partReport += " rolled a " + roll + ", margin of " + margin;

        switch (part.getQuality()) {
            case QUALITY_A: {
                if (margin >= 4) {
                    part.improveQuality();
                }
                if (!campaignOptions.isUseUnofficialMaintenance()) {
                    if (margin < -6) {
                        partsToDamage.put(part, 4);
                    } else if (margin < -4) {
                        partsToDamage.put(part, 3);
                    } else if (margin == -4) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -1) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin < -6) {
                    partsToDamage.put(part, 1);
                }
                break;
            }
            case QUALITY_B: {
                if (margin >= 4) {
                    part.improveQuality();
                } else if (margin < -5) {
                    part.reduceQuality();
                }
                if (!campaignOptions.isUseUnofficialMaintenance()) {
                    if (margin < -6) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -2) {
                        partsToDamage.put(part, 1);
                    }
                }
                break;
            }
            case QUALITY_C: {
                if (margin < -4) {
                    part.reduceQuality();
                } else if (margin >= 5) {
                    part.improveQuality();
                }
                if (!campaignOptions.isUseUnofficialMaintenance()) {
                    if (margin < -6) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -3) {
                        partsToDamage.put(part, 1);
                    }
                }
                break;
            }
            case QUALITY_D: {
                if (margin < -3) {
                    part.reduceQuality();
                    if ((margin < -4) && !campaignOptions.isUseUnofficialMaintenance()) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin >= 5) {
                    part.improveQuality();
                }
                break;
            }
            case QUALITY_E:
                if (margin < -2) {
                    part.reduceQuality();
                    if ((margin < -5) && !campaignOptions.isUseUnofficialMaintenance()) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin >= 6) {
                    part.improveQuality();
                }
                break;
            case QUALITY_F:
            default:
                if (margin < -2) {
                    part.reduceQuality();
                    if (margin < -6 && !campaignOptions.isUseUnofficialMaintenance()) {
                        partsToDamage.put(part, 1);
                    }
                }

                break;
        }
        if (part.getQuality().toNumeric() > oldQuality.toNumeric()) {
            partReport += ": " +
                                ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorPositiveHexColor(),
                                      "new quality is " + part.getQualityName());
        } else if (part.getQuality().toNumeric() < oldQuality.toNumeric()) {
            partReport += ": " +
                                ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorNegativeHexColor(),
                                      "new quality is " + part.getQualityName());
        } else {
            partReport += ": quality remains " + part.getQualityName();
        }
        if (null != partsToDamage.get(part)) {
            if (partsToDamage.get(part) > 3) {
                partReport += ", " +
                                    ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                              .getFontColorNegativeHexColor(),
                                          "<b>part destroyed</b>");
            } else {
                partReport += ", " +
                                    ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                              .getFontColorNegativeHexColor(),
                                          "<b>part damaged</b>");
            }
        }

        return partReport;
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
    public IUnitRating getUnitRating() {
        // if we switched unit rating methods,
        if (unitRating != null && (unitRating.getUnitRatingMethod() != getCampaignOptions().getUnitRatingMethod())) {
            unitRating = null;
        }

        if (unitRating == null) {
            UnitRatingMethod method = getCampaignOptions().getUnitRatingMethod();

            if (UnitRatingMethod.FLD_MAN_MERCS_REV.equals(method)) {
                unitRating = new FieldManualMercRevDragoonsRating(this);
            }
        }

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
    public ITechnology.Faction getTechFaction() {
        return techFaction;
    }

    public void updateTechFactionCode() {
        if (campaignOptions.isFactionIntroDate()) {
            for (ITechnology.Faction f : ITechnology.Faction.values()) {
                if (f.equals(ITechnology.Faction.NONE)) {
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
                techFaction = ITechnology.Faction.CLAN;
            } else if (getFaction().isPeriphery()) {
                techFaction = ITechnology.Faction.PER;
            } else {
                techFaction = ITechnology.Faction.IS;
            }
        } else {
            techFaction = ITechnology.Faction.NONE;
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

    public int stockUpPartsInUse(Set<PartInUse> partsInUse) {
        int bought = 0;
        for (PartInUse partInUse : partsInUse) {
            int toBuy = findStockUpAmount(partInUse);
            if (toBuy > 0) {
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                getShoppingList().addShoppingItem(partToBuy, toBuy, this);
                bought += 1;
            }
        }
        return bought;
    }

    public void stockUpPartsInUseGM(Set<PartInUse> partsInUse) {
        for (PartInUse partInUse : partsInUse) {
            int toBuy = findStockUpAmount(partInUse);
            while (toBuy > 0) {
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                getQuartermaster().addPart((Part) partToBuy.getNewEquipment(), 0, true);
                --toBuy;
            }
        }
    }

    private int findStockUpAmount(PartInUse PartInUse) {
        int inventory = PartInUse.getStoreCount() + PartInUse.getTransferCount() + PartInUse.getPlannedCount();
        int needed = (int) Math.ceil(PartInUse.getRequestedStock() / 100.0 * PartInUse.getUseCount());
        int toBuy = needed - inventory;

        if (PartInUse.getIsBundle()) {
            toBuy = (int) Math.ceil((float) toBuy * PartInUse.getTonnagePerItem() / 5);
            // special case for armor only, as it's bought in 5 ton blocks. Armor is the
            // only kind of item that's assigned isBundle()
        }

        return toBuy;
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
        for (Force force : getAllForces()) {
            if (!force.isForceType(ForceType.STANDARD)) {
                continue;
            }
            for (UUID unitID : force.getUnits()) {
                units.add(getUnit(unitID).getEntity());
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
                startingSystem = Systems.getInstance().getSystemById(TERRA_ID);
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
     * mercenary here incase the default changes).</p>
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
        startingSystem = Systems.getInstance().getSystemById(TERRA_ID);
        return startingSystem != null ? startingSystem.getPrimaryPlanet() : null;
    }
}
