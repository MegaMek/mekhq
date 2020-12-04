/*
 * Campaign.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.swing.JOptionPane;

import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.EncodeControl;
import megamek.utils.MegaMekXmlUtil;
import mekhq.*;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.event.MissionRemovedEvent;
import mekhq.campaign.event.ScenarioRemovedEvent;
import mekhq.campaign.finances.*;
import mekhq.campaign.log.*;
import mekhq.campaign.personnel.*;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.RandomPortraitGenerator;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;

import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.generator.RandomGenderGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.BuildingBlock;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.DayEndingEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.LoanNewEvent;
import mekhq.campaign.event.LoanPaidEvent;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.MedicPoolChangedEvent;
import mekhq.campaign.event.MissionNewEvent;
import mekhq.campaign.event.NetworkChangedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonNewEvent;
import mekhq.campaign.event.PersonRemovedEvent;
import mekhq.campaign.event.ReportEvent;
import mekhq.campaign.event.ScenarioNewEvent;
import mekhq.campaign.event.UnitNewEvent;
import mekhq.campaign.event.UnitRemovedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.SpacecraftCoolingSystem;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.rating.CampaignOpsReputation;
import mekhq.campaign.rating.FieldManualMercRevDragoonsRating;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.campaign.universe.AbstractFactionSelector;
import mekhq.campaign.universe.AbstractPlanetSelector;
import mekhq.campaign.universe.DefaultFactionSelector;
import mekhq.campaign.universe.DefaultPlanetSelector;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.News;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RATGeneratorConnector;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.RangedFactionSelector;
import mekhq.campaign.universe.RangedPlanetSelector;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.module.atb.AtBEventProcessor;
import mekhq.service.MassRepairService;

/**
 * The main campaign class, keeps track of teams and units
 * @author Taharqa
 */
public class Campaign implements Serializable, ITechManager {
    public static final String REPORT_LINEBREAK = "<br/><br/>";

    private static final long serialVersionUID = -6312434701389973056L;

    private UUID id;

    // we have three things to track: (1) teams, (2) units, (3) repair tasks
    // we will use the same basic system (borrowed from MegaMek) for tracking
    // all three
    // OK now we have more, parts, personnel, forces, missions, and scenarios.
    // and more still - we're tracking DropShips and WarShips in a separate set so that we can assign units to transports
    private Hangar units = new Hangar();
    private Set<Unit> transportShips = new HashSet<>();
    private Map<UUID, Person> personnel = new LinkedHashMap<>();
    private Warehouse parts = new Warehouse();
    private TreeMap<Integer, Force> forceIds = new TreeMap<>();
    private TreeMap<Integer, Mission> missions = new TreeMap<>();
    private TreeMap<Integer, Scenario> scenarios = new TreeMap<>();
    private Map<UUID, List<Kill>> kills = new HashMap<>();

    private Map<String, Integer> duplicateNameHash = new HashMap<>();

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
    private Game game;
    private Player player;

    private GameOptions gameOptions;

    private String name;
    private LocalDate currentDay;

    // hierarchically structured Force object to define TO&E
    private Force forces;
    private Hashtable<Integer, Lance> lances; //AtB

    private String factionCode;
    private int techFactionCode;
    private String retainerEmployerCode; //AtB
    private Ranks ranks;

    private ArrayList<String> currentReport;
    private transient String currentReportHTML;
    private transient List<String> newReports;

    //this is updated and used per gaming session, it is enabled/disabled via the Campaign options
    //we're re-using the LogEntry class that is used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private String camoCategory = Camouflage.NO_CAMOUFLAGE;
    private String camoFileName = null;
    private int colorIndex = 0;

    //unit icon
    private String iconCategory = AbstractIcon.ROOT_CATEGORY;
    private String iconFileName = AbstractIcon.DEFAULT_ICON_FILENAME;

    private Finances finances;

    private CurrentLocation location;

    private News news;

    private PartsStore partsStore;

    private List<String> customs;

    private CampaignOptions campaignOptions;
    private RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();
    private MekHQ app;

    private ShoppingList shoppingList;

    private PersonnelMarket personnelMarket;
    private ContractMarket contractMarket; //AtB
    private UnitMarket unitMarket; //AtB
    private RetirementDefectionTracker retirementDefectionTracker; // AtB
    private int fatigueLevel; //AtB
    private AtBConfiguration atbConfig; //AtB
    private AtBEventProcessor atbEventProcessor; //AtB
    private LocalDate shipSearchStart; //AtB
    private int shipSearchType;
    private String shipSearchResult; //AtB
    private LocalDate shipSearchExpiration; //AtB
    private IUnitGenerator unitGenerator;
    private IUnitRating unitRating;
    private CampaignSummary campaignSummary;
    private final Quartermaster quartermaster;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign", new EncodeControl());

    /** This is used to determine if the player has an active AtB Contract, and is recalculated on load */
    private transient boolean hasActiveContract;

    private final IAutosaveService autosaveService;

    public Campaign() {
        id = UUID.randomUUID();
        game = new Game();
        player = new Player(0, "self");
        game.addPlayer(0, player);
        currentDay = LocalDate.ofYearDay(3067, 1);
        CurrencyManager.getInstance().setCampaign(this);
        location = new CurrentLocation(Systems.getInstance().getSystems().get("Outreach"), 0);
        campaignOptions = new CampaignOptions();
        currentReport = new ArrayList<>();
        currentReportHTML = "";
        newReports = new ArrayList<>();
        name = "My Campaign";
        overtime = false;
        gmMode = false;
        factionCode = "MERC";
        techFactionCode = ITechnology.F_MERC;
        retainerEmployerCode = null;
        Ranks.initializeRankSystems();
        ranks = Ranks.getRanksFromSystem(Ranks.RS_SL);
        forces = new Force(name);
        forceIds.put(0, forces);
        lances = new Hashtable<>();
        finances = new Finances();
        SkillType.initializeTypes();
        SpecialAbility.initializeSPA();
        astechPool = 0;
        medicPool = 0;
        resetAstechMinutes();
        partsStore = new PartsStore(this);
        gameOptions = new GameOptions();
        gameOptions.initialize();
        gameOptions.getOption("year").setValue(getGameYear());
        game.setOptions(gameOptions);
        customs = new ArrayList<>();
        shoppingList = new ShoppingList();
        news = new News(getGameYear(), id.getLeastSignificantBits());
        personnelMarket = new PersonnelMarket();
        contractMarket = new ContractMarket();
        unitMarket = new UnitMarket();
        retirementDefectionTracker = new RetirementDefectionTracker();
        fatigueLevel = 0;
        atbConfig = null;
        autosaveService = new AutosaveService();
        hasActiveContract = false;
        campaignSummary = new CampaignSummary(this);
        quartermaster = new Quartermaster(this);
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
     * @return the overviewLoadingValue
     */
    public boolean isOverviewLoadingValue() {
        return overviewLoadingValue;
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

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public String getEraName() {
        return Era.getEraNameFromYear(getGameYear());
    }

    public int getEra() {
        return Era.getEra(getGameYear());
    }

    public String getTitle() {
        return getName() + " (" + getFactionName() + ")" + " - "
                + MekHQ.getMekHQOptions().getLongDisplayFormattedDate(getLocalDate())
                + " (" + getEraName() + ")";
    }

    public LocalDate getLocalDate() {
        return currentDay;
    }

    public void setLocalDate(LocalDate currentDay) {
        this.currentDay = currentDay;
    }

    public PlanetarySystem getCurrentSystem() {
        return location.getCurrentSystem();
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

    public void importLance(Lance l) {
        lances.put(l.getForceId(), l);
    }

    public Hashtable<Integer, Lance> getLances() {
        return lances;
    }

    public ArrayList<Lance> getLanceList() {
        ArrayList<Lance> retVal = new ArrayList<>();
        for (Lance l : lances.values()) {
            if (forceIds.containsKey(l.getForceId())) {
                retVal.add(l);
            }
        }
        return retVal;
    }

    public void setShoppingList(ShoppingList sl) {
        shoppingList = sl;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public void setPersonnelMarket(PersonnelMarket pm) {
        personnelMarket = pm;
    }

    public PersonnelMarket getPersonnelMarket() {
        return personnelMarket;
    }

    public void generateNewPersonnelMarket() {
        personnelMarket.generatePersonnelForDay(this);
    }

    public void setContractMarket(ContractMarket cm) {
        contractMarket = cm;
    }

    public ContractMarket getContractMarket() {
        return contractMarket;
    }

    public void generateNewContractMarket() {
        contractMarket.generateContractOffers(this);
    }

    public void setUnitMarket(UnitMarket um) {
        unitMarket = um;
    }

    public UnitMarket getUnitMarket() {
        return unitMarket;
    }

    public void generateNewUnitMarket() {
        unitMarket.generateUnitOffers(this);
    }

    public void setRetirementDefectionTracker(RetirementDefectionTracker rdt) {
        retirementDefectionTracker = rdt;
    }

    public RetirementDefectionTracker getRetirementDefectionTracker() {
        return retirementDefectionTracker;
    }

    public void setFatigueLevel(int fl) {
        fatigueLevel = fl;
    }

    public int getFatigueLevel() {
        return fatigueLevel;
    }

    /**
     * Initializes the unit generator based on the method chosen in campaignOptions.
     * Called when the unit generator is first used or when the method has been
     * changed in campaignOptions.
     */
    public void initUnitGenerator() {
        if (unitGenerator != null && unitGenerator instanceof RATManager) {
            MekHQ.unregisterHandler(unitGenerator);
        }
        if (campaignOptions.useStaticRATs()) {
            RATManager rm = new RATManager();
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    MekHQ.getLogger().error(this, e);
                }
            }
            rm.setSelectedRATs(campaignOptions.getRATs());
            rm.setIgnoreRatEra(campaignOptions.canIgnoreRatEra());
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

    //region Ship Search
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
        if (getFinances().debit(getAtBConfig().shipSearchCostPerWeek(), Transaction.C_UNIT, "Ship search", getLocalDate())) {
            report.append(getAtBConfig().shipSearchCostPerWeek().toAmountAndSymbolString())
                    .append(" deducted for ship search.");
        } else {
            addReport("<font color=\"red\">Insufficient funds for ship search.</font>");
            setShipSearchStart(null);
            return;
        }
        long numDays = ChronoUnit.DAYS.between(getShipSearchStart(), getLocalDate());
        if (numDays > 21) {
            int roll = Compute.d6(2);
            TargetRoll target = getAtBConfig().shipSearchTargetRoll(shipSearchType, this);
            setShipSearchStart(null);
            report.append("<br/>Ship search target: ").append(target.getValueAsString()).append(" roll: ")
                    .append(roll);
            // TODO: mos zero should make ship available on retainer
            if (roll >= target.getValue()) {
                report.append("<br/>Search successful. ");
                MechSummary ms = unitGenerator.generate(getFactionCode(), shipSearchType, -1,
                        getGameYear(), getUnitRatingMod());
                if (ms == null) {
                    ms = getAtBConfig().findShip(shipSearchType);
                }
                if (ms != null) {
                    setShipSearchResult(ms.getName());
                    setShipSearchExpiration(getLocalDate().plusDays(31));
                    report.append(getShipSearchResult()).append(" is available for purchase for ")
                            .append(Money.of(ms.getCost()).toAmountAndSymbolString())
                            .append(" until ")
                            .append(MekHQ.getMekHQOptions().getDisplayFormattedDate(getShipSearchExpiration()));
                } else {
                    report.append(" <font color=\"red\">Could not determine ship type.</font>");
                }
            } else {
                report.append("<br/>Ship search unsuccessful.");
            }
        }
        addReport(report.toString());
    }

    public void purchaseShipSearchResult() {
        final String METHOD_NAME = "purchaseShipSearchResult()";
        MechSummary ms = MechSummaryCache.getInstance().getMech(getShipSearchResult());
        if (ms == null) {
            MekHQ.getLogger().error(this, "Cannot find entry for " + getShipSearchResult());
            return;
        }

        Money cost = Money.of(ms.getCost());

        if (getFunds().isLessThan(cost)) {
            addReport("<font color='red'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
            return;
        }

        MechFileParser mechFileParser;
        try {
            mechFileParser = new MechFileParser(ms.getSourceFile(), ms.getEntryName());
        } catch (Exception ex) {
            MekHQ.getLogger().error(this, "Unable to load unit: " + ms.getEntryName(), ex);
            return;
        }
        Entity en = mechFileParser.getEntity();

        int transitDays = getCampaignOptions().getInstantUnitMarketDelivery() ? 0
                : calculatePartTransitTime(Compute.d6(2) - 2);

        getFinances().debit(cost, Transaction.C_UNIT, "Purchased " + en.getShortName(), getLocalDate());
        addNewUnit(en, true, transitDays);
        if (!getCampaignOptions().getInstantUnitMarketDelivery()) {
            addReport("<font color='green'>Unit will be delivered in " + transitDays + " days.</font>");
        }
        setShipSearchResult(null);
        setShipSearchExpiration(null);
    }
    //endregion Ship Search

    /**
     * Process retirements for retired personnel, if any.
     * @param totalPayout The total retirement payout.
     * @param unitAssignments List of unit assignments.
     * @return False if there were payments AND they were unable to be processed, true otherwise.
     */
    public boolean applyRetirement(Money totalPayout, HashMap<UUID, UUID> unitAssignments) {
        if ((totalPayout.isPositive()) || (null != getRetirementDefectionTracker().getRetirees())) {
            if (getFinances().debit(totalPayout, Transaction.C_SALARY, "Final Payout", getLocalDate())) {
                for (UUID pid : getRetirementDefectionTracker().getRetirees()) {
                    if (getPerson(pid).getStatus().isActive()) {
                        getPerson(pid).changeStatus(this, PersonnelStatus.RETIRED);
                        addReport(getPerson(pid).getFullName() + " has retired.");
                    }
                    if (Person.T_NONE != getRetirementDefectionTracker().getPayout(pid).getRecruitType()) {
                        getPersonnelMarket().addPerson(
                                newPerson(getRetirementDefectionTracker().getPayout(pid).getRecruitType()));
                    }
                    if (getRetirementDefectionTracker().getPayout(pid).hasHeir()) {
                        Person p = newPerson(getPerson(pid).getPrimaryRole());
                        p.setOriginalUnitWeight(getPerson(pid).getOriginalUnitWeight());
                        p.setOriginalUnitTech(getPerson(pid).getOriginalUnitTech());
                        p.setOriginalUnitId(getPerson(pid).getOriginalUnitId());
                        if (unitAssignments.containsKey(pid)) {
                            getPersonnelMarket().addPerson(p, getHangar().getUnit(unitAssignments.get(pid)).getEntity());
                        } else {
                            getPersonnelMarket().addPerson(p);
                        }
                    }
                    if (getCampaignOptions().canAtBAddDependents()) {
                        int dependents = getRetirementDefectionTracker().getPayout(pid).getDependents();
                        while (dependents > 0) {
                            Person person = newDependent(Person.T_ASTECH, false);
                            if (recruitPerson(person)) {
                                dependents--;
                            } else {
                                dependents = 0;
                            }
                        }
                    }
                    if (unitAssignments.containsKey(pid)) {
                        removeUnit(unitAssignments.get(pid));
                    }
                }
                getRetirementDefectionTracker().resolveAllContracts();
                return true;
            } else {
                addReport("<font color='red'>You cannot afford to make the final payments.</font>");
                return false;
            }
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
     * Add force to an existing superforce. This method will also assign the force an id and place it in the forceId hash
     *
     * @param force      - the Force to add
     * @param superForce - the superforce to add the new force to
     */
    public void addForce(Force force, Force superForce) {
        int id = lastForceId + 1;
        force.setId(id);
        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId());
        forceIds.put(id, force);
        lastForceId = id;

        if (campaignOptions.getUseAtB() && force.getUnits().size() > 0) {
            if (null == lances.get(id)) {
                lances.put(id, new Lance(force.getId(), this));
            }
        }
    }

    public void moveForce(Force force, Force superForce) {
        Force parentForce = force.getParentForce();
        if (null != parentForce) {
            parentForce.removeSubForce(force.getId());
        }
        superForce.addSubForce(force, true);
        force.setScenarioId(superForce.getScenarioId());
        for (Object o : force.getAllChildren(this)) {
            if (o instanceof Unit) {
                ((Unit) o).setScenarioId(superForce.getScenarioId());
            } else if (o instanceof Force) {
                ((Force) o).setScenarioId(superForce.getScenarioId());
            }
        }
    }

    /**
     * This is used by the XML loader. The id should already be set for this force so dont increment
     *
     * @param force
     */
    public void importForce(Force force) {
        lastForceId = Math.max(lastForceId, force.getId());
        forceIds.put(force.getId(), force);
    }

    /**
     * This is used by the XML loader. The id should already be set for this scenario so dont increment
     *
     * @param scenario
     */
    public void importScenario(Scenario scenario) {
        lastScenarioId = Math.max(lastScenarioId, scenario.getId());
        scenarios.put(scenario.getId(), scenario);
    }

    /**
     * Add unit to an existing force. This method will also assign that force's id to the unit.
     *
     * @param u
     * @param id
     */
    public void addUnitToForce(Unit u, int id) {
        Force prevForce = forceIds.get(u.getForceId());
        if (null != prevForce) {
            prevForce.removeUnit(u.getId());
            MekHQ.triggerEvent(new OrganizationChangedEvent(prevForce, u));
            if (null != prevForce.getTechID()) {
                u.removeTech();
            }
        }
        Force force = forceIds.get(id);
        if (null != force) {
            u.setForceId(id);
            force.addUnit(u.getId());
            u.setScenarioId(force.getScenarioId());
            MekHQ.triggerEvent(new OrganizationChangedEvent(force, u));
            if (null != force.getTechID()) {
                Person forceTech = getPerson(force.getTechID());
                if (forceTech.canTech(u.getEntity())) {
                    if (null != u.getTech()) {
                        u.removeTech();
                    }

                    u.setTech(forceTech);
                } else {
                    String cantTech = forceTech.getFullName() + " cannot maintain " + u.getName() + "\n"
                            + "You will need to assign a tech manually.";
                    JOptionPane.showMessageDialog(null, cantTech, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        if (campaignOptions.getUseAtB()) {
            if (null != prevForce && prevForce.getUnits().size() == 0) {
                lances.remove(prevForce.getId());
            }
            if (null == lances.get(id) && null != force) {
                lances.put(id, new Lance(force.getId(), this));
            }
        }
    }

    /** Adds force and all its subforces to the AtB lance table
     */

    private void addAllLances(Force force) {
        if (force.getUnits().size() > 0) {
            lances.put(force.getId(), new Lance(force.getId(), this));
        }
        for (Force f : force.getSubForces()) {
            addAllLances(f);
        }
    }

    /**
     * Add a mission to the campaign
     *
     * @param m The mission to be added
     */
    public void addMission(Mission m) {
        int id = lastMissionId + 1;
        m.setId(id);
        missions.put(id, m);
        lastMissionId = id;
        MekHQ.triggerEvent(new MissionNewEvent(m));
    }

    /**
     * Imports a {@link Mission} into a campaign.
     * @param m Mission to import into the campaign.
     */
    public void importMission(Mission m) {
        // add scenarios to the scenarioId hash
        for (Scenario s : m.getScenarios()) {
            importScenario(s);
        }

        addMissionWithoutId(m);
    }

    private void addMissionWithoutId(Mission m) {
        lastMissionId = Math.max(lastMissionId, m.getId());
        missions.put(m.getId(), m);
        MekHQ.triggerEvent(new MissionNewEvent(m));
    }

    /**
     * @return an <code>Collection</code> of missions in the campaign
     */
    public Collection<Mission> getMissions() {
        return missions.values();
    }

    /**
     * @return missions ArrayList sorted with complete missions at the bottom
     */
    public ArrayList<Mission> getSortedMissions() {
        ArrayList<Mission> msns = new ArrayList<>(missions.values());
        msns.sort((m1, m2) -> Boolean.compare(m2.isActive(), m1.isActive()));

        return msns;
    }

    /**
     * @param id the <code>int</code> id of the team
     * @return a <code>SupportTeam</code> object
     */
    public Mission getMission(int id) {
        return missions.get(id);
    }

    /**
     * Add scenario to an existing mission. This method will also assign the scenario an id, provided
     * that it is a new scenario. It then adds the scenario to the scenarioId hash.
     *
     * Scenarios with previously set ids can be sent to this mission, allowing one to remove
     * and then re-add scenarios if needed. This functionality is used in the
     * <code>AtBScenarioFactory</code> class in method <code>createScenariosForNewWeek</code> to
     * ensure that scenarios are generated properly.
     *
     * @param s - the Scenario to add
     * @param m - the mission to add the new scenario to
     */
    public void addScenario(Scenario s, Mission m) {
        final boolean newScenario = s.getId() == Scenario.S_DEFAULT_ID;
        final int id = newScenario ? ++lastScenarioId : s.getId();
        s.setId(id);
        m.addScenario(s);
        scenarios.put(id, s);

        if (newScenario) {
            addReport(MessageFormat.format(
                    resources.getString("newAtBMission.format"),
                    s.getName(), MekHQ.getMekHQOptions().getDisplayFormattedDate(s.getDate())));
        }

        MekHQ.triggerEvent(new ScenarioNewEvent(s));
    }

    public Scenario getScenario(int id) {
        return scenarios.get(id);
    }

    public void setLocation(CurrentLocation l) {
        location = l;
    }

    /**
     * Moves immediately to a {@link PlanetarySystem}.
     * @param s The {@link PlanetarySystem} the campaign
     *          has been moved to.
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
     * @param u A {@link Unit} to import into the campaign.
     */
    public void importUnit(Unit u) {
        Objects.requireNonNull(u);

        MekHQ.getLogger().debug("Importing unit: (" + u.getId() + "): " + u.getName());

        getHangar().addUnit(u);

        checkDuplicateNamesDuringAdd(u.getEntity());

        //If this is a ship, add it to the list of potential transports
        //Jumpships and space stations are intentionally ignored at present, because this functionality is being
        //used to auto-load ground units into bays, and doing this for large craft that can't transit is pointless.
        if ((u.getEntity() instanceof Dropship) || (u.getEntity() instanceof Warship)) {
            addTransportShip(u);
        }

        // Assign an entity ID to our new unit
        if (Entity.NONE == u.getEntity().getId()) {
            u.getEntity().setId(game.getNextEntityId());
        }

        game.addEntity(u.getEntity().getId(), u.getEntity());
    }

    /**
     * Adds an entry to the list of transit-capable transport ships. We'll use this
     * to look for empty bays that ground units can be assigned to
     * @param unit - The ship we want to add to this Set
     */
    public void addTransportShip(Unit unit) {
        MekHQ.getLogger().debug("Adding DropShip/WarShip: " + unit.getId());

        transportShips.add(Objects.requireNonNull(unit));
    }

    /**
     * Deletes an entry from the list of transit-capable transport ships. This gets updated when
     * the ship is removed from the campaign for one reason or another
     * @param unit - The ship we want to remove from this Set
     */
    public void removeTransportShip(Unit unit) {
        // If we remove a transport ship from the campaign,
        // we need to remove any transported units from it
        if (transportShips.remove(unit)
                && unit.hasTransportedUnits()) {
            List<Unit> transportedUnits = new ArrayList<>(unit.getTransportedUnits());
            for (Unit transportedUnit : transportedUnits) {
                unit.removeTransportedUnit(transportedUnit);
            }
        }
    }

    /**
     * This is for adding a TestUnit that was previously created and had parts added to
     * it. We need to do the normal stuff, but we also need to take the existing parts and
     * add them to the campaign.
     * @param tu
     */
    public void addTestUnit(TestUnit tu) {
        // we really just want the entity and the parts so lets just wrap that around a
        // new
        // unit.
        Unit unit = new Unit(tu.getEntity(), this);
        getHangar().addUnit(unit);

        // we decided we like the test unit so much we are going to keep it
        unit.getEntity().setOwner(player);
        unit.getEntity().setGame(game);
        unit.getEntity().setExternalIdAsString(unit.getId().toString());

        // now lets grab the parts from the test unit and set them up with this unit
        for (Part p : tu.getParts()) {
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
        game.addEntity(unit.getEntity().getId(), unit.getEntity());

        checkDuplicateNamesDuringAdd(unit.getEntity());
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");
    }

    /**
     * Add a new unit to the campaign.
     *
     * @param en An <code>Entity</code> object that the new unit will be wrapped around
     */
    public Unit addNewUnit(Entity en, boolean allowNewPilots, int days) {
        Unit unit = new Unit(en, this);
        getHangar().addUnit(unit);

        // reset the game object
        en.setOwner(player);
        en.setGame(game);
        en.setExternalIdAsString(unit.getId().toString());

        unit.initializeBaySpace();
        removeUnitFromForce(unit); // Added to avoid the 'default force bug'
        // when calculating cargo

        //If this is a ship, add it to the list of potential transports
        //Jumpships and space stations are intentionally ignored at present, because this functionality is being
        //used to auto-load ground units into bays, and doing this for large craft that can't transit is pointless.
        if ((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Warship)) {
            addTransportShip(unit);
        }

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }
        unit.setDaysToArrival(days);

        if (allowNewPilots) {
            Map<CrewType, Collection<Person>> newCrew = Utilities.genRandomCrewWithCombinedSkill(this, unit, getFactionCode());
            newCrew.forEach((type, personnel) -> personnel.forEach(p -> type.addMethod.accept(unit, p)));
        }
        unit.resetPilotAndEntity();

        // Assign an entity ID to our new unit
        if (Entity.NONE == en.getId()) {
            en.setId(game.getNextEntityId());
        }
        game.addEntity(en.getId(), en);

        checkDuplicateNamesDuringAdd(en);
        addReport(unit.getHyperlinkedName() + " has been added to the unit roster.");
        MekHQ.triggerEvent(new UnitNewEvent(unit));

        return unit;
    }

    /**
     * Gets the current hangar containing the player's units.
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

    public ArrayList<Entity> getEntities() {
        ArrayList<Entity> entities = new ArrayList<>();
        for (Unit unit : getUnits()) {
            entities.add(unit.getEntity());
        }
        return entities;
    }

    public Unit getUnit(UUID id) {
        return getHangar().getUnit(id);
    }

    //region Personnel
    //region Person Creation
    /**
     * Creates a new {@link Person}, who is a dependent, of a given primary role.
     * @param type The primary role of the {@link Person}, e.g. {@link Person#T_MECHWARRIOR}.
     * @return A new {@link Person} of the given primary role, who is a dependent.
     */
    public Person newDependent(int type, boolean baby) {
        Person person;

        if (!baby && campaignOptions.getRandomizeDependentOrigin()) {
            person = newPerson(type);
        } else {
            person = newPerson(type, Person.T_NONE, new DefaultFactionSelector(),
                    new DefaultPlanetSelector(), Gender.RANDOMIZE);
        }

        person.setDependent(true);
        return person;
    }

    /**
     * Generate a new pilotPerson of the given type using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param type The primary role
     * @return A new {@link Person}.
     */
    public Person newPerson(int type) {
        return newPerson(type, Person.T_NONE);
    }

    /**
     * Generate a new pilotPerson of the given type using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param type The primary role
     * @param secondary A secondary role; used for LAM pilots to generate MW + Aero pilot
     * @return A new {@link Person}.
     */
    public Person newPerson(int type, int secondary) {
        return newPerson(type, secondary, getFactionSelector(), getPlanetSelector(), Gender.RANDOMIZE);
    }

    /**
     * Generate a new pilotPerson of the given type using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param type The primary role
     * @param secondary A secondary role; used for LAM pilots to generate MW + Aero pilot
     * @param factionCode The code for the faction this person is to be generated from
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person}.
     */
    public Person newPerson(int type, int secondary, String factionCode, Gender gender) {
        return newPerson(type, secondary, new DefaultFactionSelector(factionCode), getPlanetSelector(), gender);
    }

    /**
     * Generate a new pilotPerson of the given type using whatever randomization options have been given in the
     * CampaignOptions
     *
     * @param type The primary role
     * @param secondary A secondary role; used for LAM pilots to generate MW + Aero pilot
     * @param factionSelector The faction selector to use for the person.
     * @param planetSelector The planet selector for the person.
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person}.
     */
    public Person newPerson(int type, int secondary, AbstractFactionSelector factionSelector,
                            AbstractPlanetSelector planetSelector, Gender gender) {
        AbstractPersonnelGenerator personnelGenerator = getPersonnelGenerator(factionSelector, planetSelector);
        return newPerson(type, secondary, personnelGenerator, gender);
    }

    /**
     * Generate a new {@link Person} of the given type, using the supplied {@link AbstractPersonnelGenerator}
     * @param type The primary role of the {@link Person}.
     * @param secondary The secondary role, or {@link Person#T_NONE}, of the {@link Person}.
     * @param personnelGenerator The {@link AbstractPersonnelGenerator} to use when creating the {@link Person}.
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person} configured using {@code personnelGenerator}.
     */
    public Person newPerson(int type, int secondary, AbstractPersonnelGenerator personnelGenerator, Gender gender) {
        if (type == Person.T_LAM_PILOT) {
            type = Person.T_MECHWARRIOR;
            secondary = Person.T_AERO_PILOT;
        }

        Person person = personnelGenerator.generate(this, type, secondary, gender);

        // Assign a random portrait after we generate a new person
        if (getCampaignOptions().usePortraitForType(type)) {
            assignRandomPortraitFor(person);
        }

        return person;
    }
    //endregion Person Creation

    //region Personnel Recruitment
    /**
     * @param p         the person being added
     * @return          true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p) {
        return recruitPerson(p, p.getPrisonerStatus(), p.isDependent(), false, true);
    }

    /**
     * @param p         the person being added
     * @param gmAdd     false means that they need to pay to hire this person, provided that
     *                  the campaign option to pay for new hires is set, while
     *                  true means they are added without paying
     * @return          true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, boolean gmAdd) {
        return recruitPerson(p, p.getPrisonerStatus(), p.isDependent(), gmAdd, true);
    }

    /**
     *
     * @param p              the person being added
     * @param prisonerStatus the person's prisoner status upon recruitment
     * @return               true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, PrisonerStatus prisonerStatus) {
        return recruitPerson(p, prisonerStatus, p.isDependent(), false, true);
    }

    /**
     * @param p              the person being added
     * @param prisonerStatus the person's prisoner status upon recruitment
     * @param dependent      if the person is a dependent or not. True means they are a dependent
     * @param gmAdd          false means that they need to pay to hire this person, true means it is added without paying
     * @param log            whether or not to write to logs
     * @return               true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, PrisonerStatus prisonerStatus, boolean dependent,
                                 boolean gmAdd, boolean log) {
        if (p == null) {
            return false;
        }
        // Only pay if option set, they weren't GM added, and they aren't a dependent, prisoner or bondsman
        if (getCampaignOptions().payForRecruitment() && !dependent && !gmAdd && prisonerStatus.isFree()) {
            if (!getFinances().debit(p.getSalary().multipliedBy(2), Transaction.C_SALARY,
                    "Recruitment of " + p.getFullName(), getLocalDate())) {
                addReport("<font color='red'><b>Insufficient funds to recruit "
                        + p.getFullName() + "</b></font>");
                return false;
            }
        }

        personnel.put(p.getId(), p);

        if (log) {
            String add = !prisonerStatus.isFree() ? (prisonerStatus.isBondsman() ? " as a bondsman" : " as a prisoner") : "";
            addReport(String.format("%s has been added to the personnel roster%s.", p.getHyperlinkedName(), add));
        }

        if (p.getPrimaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 480;
            astechPoolOvertime += 240;
        } else if (p.getSecondaryRole() == Person.T_ASTECH) {
            astechPoolMinutes += 240;
            astechPoolOvertime += 120;
        }

        p.setPrisonerStatus(prisonerStatus, log);

        MekHQ.triggerEvent(new PersonNewEvent(p));
        return true;
    }
    //endregion Personnel Recruitment

    //region Bloodnames
    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on
     * skill and rank. If the roll indicates there should be a bloodname, one is assigned as
     * appropriate to the person's phenotype and the player's faction.
     *
     * @param person     The Bloodname candidate
     * @param ignoreDice If true, skips the random roll and assigns a Bloodname automatically
     */
    public void checkBloodnameAdd(Person person, boolean ignoreDice) {
        // if a non-clanner or a clanner without a phenotype is here, we can just return
        if (!person.isClanner() || (person.getPhenotype() == Phenotype.NONE)) {
            return;
        }

        // Person already has a bloodname, we open up the dialog to ask if they want to keep the
        // current bloodname or assign a new one
        if (person.getBloodname().length() > 0) {
            int result = JOptionPane.showConfirmDialog(null,
                    person.getFullTitle() + " already has the bloodname " + person.getBloodname()
                            + "\nDo you wish to remove that bloodname and generate a new one?",
                    "Already Has Bloodname", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            } else {
                ignoreDice = true;
            }
        }

        // Go ahead and generate a new bloodname
        int bloodnameTarget = 6;
        if (!ignoreDice) {
            switch (person.getPhenotype()) {
                case MECHWARRIOR: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_MECH)
                            ? person.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_MECH)
                            ? person.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case AEROSPACE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_AERO)
                            ? person.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_AERO)
                            ? person.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case ELEMENTAL: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_BA)
                            ? person.getSkill(SkillType.S_GUN_BA).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_ANTI_MECH)
                            ? person.getSkill(SkillType.S_ANTI_MECH).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case VEHICLE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_VEE)
                            ? person.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL;
                    switch (person.getPrimaryRole()) {
                        case Person.T_VTOL_PILOT:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL)
                                    ? person.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case Person.T_NVEE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE)
                                    ? person.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case Person.T_GVEE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE)
                                    ? person.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                    }
                    break;
                }
                case PROTOMECH: {
                    bloodnameTarget += 2 * (person.hasSkill(SkillType.S_GUN_PROTO)
                            ? person.getSkill(SkillType.S_GUN_PROTO).getFinalSkillValue()
                            : TargetRoll.AUTOMATIC_FAIL);
                    break;
                }
                case NAVAL: {
                    switch (person.getPrimaryRole()) {
                        case Person.T_SPACE_CREW:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_TECH_VESSEL)
                                    ? person.getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case Person.T_SPACE_GUNNER:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_GUN_SPACE)
                                    ? person.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case Person.T_SPACE_PILOT:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_PILOT_SPACE)
                                    ? person.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case Person.T_NAVIGATOR:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_NAV)
                                    ? person.getSkill(SkillType.S_NAV).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            // Higher rated units are more likely to have Bloodnamed
            if (getCampaignOptions().getUnitRatingMethod().isEnabled()) {
                IUnitRating rating = getUnitRating();
                bloodnameTarget += IUnitRating.DRAGOON_C - (getCampaignOptions().getUnitRatingMethod().equals(
                        mekhq.campaign.rating.UnitRatingMethod.FLD_MAN_MERCS_REV)
                        ? rating.getUnitRatingAsInteger() : rating.getModifier());
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
            bloodnameTarget += Math.min(0, ranks.getOfficerCut() - person.getRankNumeric());
        }

        if (ignoreDice || (Compute.d6(2) >= bloodnameTarget)) {
            Phenotype phenotype = person.getPhenotype();
            if (phenotype == Phenotype.NONE) {
                phenotype = Phenotype.GENERAL;
            }

            Bloodname bloodname = Bloodname.randomBloodname(
                    (getFaction().isClan() ? getFaction() : person.getOriginFaction()).getShortName(),
                    phenotype, getGameYear());
            if (bloodname != null) {
                person.setBloodname(bloodname.getName());
                personUpdated(person);
            }
        }
    }
    //endregion Bloodnames

    //region Other Personnel Methods
    /**
     * Imports a {@link Person} into a campaign.
     * @param p A {@link Person} to import into the campaign.
     */
    public void importPerson(Person p) {
        personnel.put(p.getId(), p);
        MekHQ.triggerEvent(new PersonNewEvent(p));
    }

    public Person getPerson(UUID id) {
        return personnel.get(id);
    }

    public Collection<Person> getPersonnel() {
        return personnel.values();
    }

    /**
     * Provides a filtered list of personnel including only active Persons.
     * @return List<Person>
     */
    public List<Person> getActivePersonnel() {
        List<Person> activePersonnel = new ArrayList<>();
        for (Person p : getPersonnel()) {
            if (p.getStatus().isActive()) {
                activePersonnel.add(p);
            }
        }
        return activePersonnel;
    }
    //endregion Other Personnel Methods

    //region Personnel Selectors and Generators
    /**
     * Gets the {@link AbstractFactionSelector} to use with this campaign.
     * @return An {@link AbstractFactionSelector} to use when selecting a {@link Faction}.
     */
    public AbstractFactionSelector getFactionSelector() {
        if (getCampaignOptions().randomizeOrigin()) {
            RangedFactionSelector selector = new RangedFactionSelector(getCampaignOptions().getOriginSearchRadius());
            selector.setDistanceScale(getCampaignOptions().getOriginDistanceScale());
            return selector;
        } else {
            return new DefaultFactionSelector();
        }
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use with this campaign.
     * @return An {@link AbstractPlanetSelector} to use when selecting a {@link Planet}.
     */
    public AbstractPlanetSelector getPlanetSelector() {
        if (getCampaignOptions().randomizeOrigin()) {
            RangedPlanetSelector selector =
                    new RangedPlanetSelector(getCampaignOptions().getOriginSearchRadius(),
                            getCampaignOptions().isOriginExtraRandom());
            selector.setDistanceScale(getCampaignOptions().getOriginDistanceScale());
            return selector;
        } else {
            return new DefaultPlanetSelector();
        }
    }

    /**
     * Gets the {@link AbstractPersonnelGenerator} to use with this campaign.
     * @param factionSelector The {@link AbstractFactionSelector} to use when choosing a {@link Faction}.
     * @param planetSelector The {@link AbstractPlanetSelector} to use when choosing a {@link Planet}.
     * @return An {@link AbstractPersonnelGenerator} to use when creating new personnel.
     */
    public AbstractPersonnelGenerator getPersonnelGenerator(AbstractFactionSelector factionSelector, AbstractPlanetSelector planetSelector) {
        DefaultPersonnelGenerator generator = new DefaultPersonnelGenerator(factionSelector, planetSelector);
        generator.setNameGenerator(RandomNameGenerator.getInstance());
        generator.setSkillPreferences(getRandomSkillPreferences());
        return generator;
    }
    //endregion Personnel Selectors and Generators
    //endregion Personnel

    public List<Person> getPatients() {
        List<Person> patients = new ArrayList<>();
        for (Person p : getPersonnel()) {
            if (p.needsFixing()
                    || (getCampaignOptions().useAdvancedMedical() && p.hasInjuries(true) && p.getStatus().isActive())) {
                patients.add(p);
            }
        }
        return patients;
    }

    public List<Unit> getServiceableUnits() {
        List<Unit> service = new ArrayList<>();
        for (Unit u : getUnits()) {
            if (u.isAvailable() && u.isServiceable()) {
                service.add(u);
            }
        }
        return service;
    }

    /**
     * Imports a collection of parts into the campaign.
     *
     * @param newParts The collection of {@link Part} instances
     *                 to import into the campaign.
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
    @Deprecated
    public Collection<Part> getParts() {
        return parts.getParts();
    }

    private int getQuantity(Part p) {
        if (p instanceof Armor) {
            return ((Armor) p).getAmount();
        }
        if (p instanceof AmmoStorage) {
            return ((AmmoStorage) p).getShots();
        }
        return (p.getUnit() != null) ? 1 : p.getQuantity();
    }

    private PartInUse getPartInUse(Part p) {
        // SI isn't a proper "part"
        if (p instanceof StructuralIntegrity) {
            return null;
        }
        // Makes no sense buying those separately from the chasis
        if((p instanceof EquipmentPart)
                && ((EquipmentPart) p).getType() != null
                && (((EquipmentPart) p).getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)))
        {
            return null;
        }
        // Replace a "missing" part with a corresponding "new" one.
        if (p instanceof MissingPart) {
            p = ((MissingPart) p).getNewPart();
        }
        PartInUse result = new PartInUse(p);
        return (null != result.getPartToBuy()) ? result : null;
    }

    private void updatePartInUseData(PartInUse piu, Part p) {
        if ((p.getUnit() != null) || (p instanceof MissingPart)) {
            piu.setUseCount(piu.getUseCount() + getQuantity(p));
        } else {
            if (p.isPresent()) {
                piu.setStoreCount(piu.getStoreCount() + getQuantity(p));
            } else {
                piu.setTransferCount(piu.getTransferCount() + getQuantity(p));
            }
        }
    }

    /** Update the piu with the current campaign data */
    public void updatePartInUse(PartInUse piu) {
        piu.setUseCount(0);
        piu.setStoreCount(0);
        piu.setTransferCount(0);
        piu.setPlannedCount(0);
        getWarehouse().forEachPart(p -> {
            PartInUse newPiu = getPartInUse(p);
            if (piu.equals(newPiu)) {
                updatePartInUseData(piu, p);
            }
        });
        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            PartInUse newPiu = getPartInUse((Part) maybePart);
            if (piu.equals(newPiu)) {
                piu.setPlannedCount(piu.getPlannedCount()
                        + getQuantity((maybePart instanceof MissingPart) ? ((MissingPart) maybePart).getNewPart()
                                : (Part) maybePart) * maybePart.getQuantity());
            }
        }
    }

    public Set<PartInUse> getPartsInUse() {
        // java.util.Set doesn't supply a get(Object) method, so we have to use a java.util.Map
        Map<PartInUse, PartInUse> inUse = new HashMap<>();
        getWarehouse().forEachPart(p -> {
            PartInUse piu = getPartInUse(p);
            if (null == piu) {
                return;
            }
            if (inUse.containsKey(piu)) {
                piu = inUse.get(piu);
            } else {
                inUse.put(piu, piu);
            }
            updatePartInUseData(piu, p);
        });
        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            if (!(maybePart instanceof Part)) {
                continue;
            }
            PartInUse piu = getPartInUse((Part) maybePart);
            if (null == piu) {
                continue;
            }
            if ( inUse.containsKey(piu) ) {
                piu = inUse.get(piu);
            } else {
                inUse.put(piu, piu);
            }
            piu.setPlannedCount(piu.getPlannedCount()
                    + getQuantity((maybePart instanceof MissingPart) ? ((MissingPart) maybePart).getNewPart()
                            : (Part) maybePart) * maybePart.getQuantity());

        }
        return inUse.keySet();
    }

    @Deprecated
    public Part getPart(int id) {
        return parts.getPart(id);
    }

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
     * Finds the active person in a particular role with the highest level in a
     * given, with an optional secondary skill to break ties.
     *
     * @param role
     *            One of the Person.T_* constants
     * @param primary
     *            The skill to use for comparison.
     * @param secondary
     *            If not null and there is more than one person tied for the most
     *            the highest, preference will be given to the one with a higher
     *            level in the secondary skill.
     * @return The admin in the designated role with the most experience.
     */
    public Person findBestInRole(int role, String primary, String secondary) {
        int highest = 0;
        Person retVal = null;
        for (Person p : getActivePersonnel()) {
            if ((p.getPrimaryRole() == role || p.getSecondaryRole() == role) && p.getSkill(primary) != null) {
                if (p.getSkill(primary).getLevel() > highest) {
                    retVal = p;
                    highest = p.getSkill(primary).getLevel();
                } else if (secondary != null && p.getSkill(primary).getLevel() == highest &&
                /*
                 * If the skill level of the current person is the same as the previous highest,
                 * select the current instead under the following conditions:
                 */
                        (retVal == null || // None has been selected yet (current has level 0)
                                retVal.getSkill(secondary) == null || // Previous selection does not have secondary
                                                                      // skill
                                (p.getSkill(secondary) != null // Current has secondary skill and it is higher than the
                                                               // previous.
                                        && p.getSkill(secondary).getLevel() > retVal.getSkill(secondary).getLevel()))) {
                    retVal = p;
                }
            }
        }
        return retVal;
    }

    public Person findBestInRole(int role, String skill) {
        return findBestInRole(role, skill, null);
    }

    /**
     * @return The list of all active {@link Person}s who qualify as technicians ({@link Person#isTech()}));
     */
    public List<Person> getTechs() {
        return getTechs(false, null, true, false);
    }

    public List<Person> getTechs(boolean noZeroMinute) {
        return getTechs(noZeroMinute, null, true, false);
    }

    /**
     * Returns a list of active technicians.
     *
     * @param noZeroMinute
     *            If TRUE, then techs with no time remaining will be excluded from
     *            the list.
     * @param firstTechId
     *            The ID of the tech that should appear first in the list (assuming
     *            active and satisfies the noZeroMinute argument)
     * @param sorted
     *            If TRUE, then return the list sorted from worst to best
     * @param eliteFirst
     *            If TRUE and sorted also TRUE, then return the list sorted from
     *            best to worst
     * @return The list of active {@link Person}s who qualify as technicians
     *         ({@link Person#isTech()}).
     */
    public List<Person> getTechs(boolean noZeroMinute, UUID firstTechId, boolean sorted, boolean eliteFirst) {
        List<Person> techs = new ArrayList<>();

        // Get the first tech.
        Person firstTech = getPerson(firstTechId);
        if ((firstTech != null) && firstTech.isTech() && firstTech.getStatus().isActive()
                && (!noZeroMinute || firstTech.getMinutesLeft() > 0)) {
            techs.add(firstTech);
        }

        for (Person p : getActivePersonnel()) {
            if (p.isTech() && (!p.equals(firstTech)) && (!noZeroMinute || (p.getMinutesLeft() > 0))) {
                techs.add(p);
            }
        }
        // also need to loop through and collect engineers on self-crewed vessels
        for (Unit u : getUnits()) {
            if (u.isSelfCrewed() && !(u.getEntity() instanceof Infantry) && (null != u.getEngineer())) {
                techs.add(u.getEngineer());
            }
        }

        // Return the tech collection sorted worst to best
        // Reverse the sort if we've been asked for best to worst
        if (sorted) {
            // First order by the amount of time the person has remaining, based on the sorting order
            // comparison changes because locations that use elite first will want the person with
            // the most remaining time at the top of the list while locations that don't will want it
            // at the bottom of the list
            if (eliteFirst) {
                // We want the highest amount of remaining time at the top of the list, as that
                // makes it easy to compare between the two
                techs.sort(Comparator.comparingInt(Person::getMinutesLeft));
            } else {
                // Otherwise, we want the highest amount of time being at the bottom of the list
                techs.sort(Comparator.comparingInt(Person::getMinutesLeft).reversed());
            }
            // Then sort by the skill level, which puts Elite personnel first or last dependant on
            // the eliteFirst value
            techs.sort((person1, person2) -> {
                // default to 0, which means they're equal
                int retVal = 0;

                // Set up booleans to know if the tech is secondary only
                // this is to get the skill from getExperienceLevel(boolean) properly
                boolean p1Secondary = !person1.isTechPrimary() && person1.isTechSecondary();
                boolean p2Secondary = !person2.isTechPrimary() && person2.isTechSecondary();

                if (person1.getExperienceLevel(p1Secondary) > person2.getExperienceLevel(p2Secondary)) {
                    // Person 1 is better than Person 2.
                    retVal = 1;
                } else if (person1.getExperienceLevel(p1Secondary) < person2.getExperienceLevel(p2Secondary)) {
                    // Person 2 is better than Person 1
                    retVal = -1;
                }

                // Return, swapping the value if we're looking to have Elites ordered first
                return eliteFirst ? -retVal : retVal;
            });
        }

        return techs;
    }

    /**
     * Gets a list of all techs of a specific role type
     * @param roleType The filter role type
     * @return Collection of all techs that match the given tech role
     */
    public List<Person> getTechsByRole(int roleType) {
        List<Person> techs = getTechs(false, null, false, false);
        List<Person> retval = new ArrayList<>();

        for (Person tech : techs) {
            if((tech.getPrimaryRole() == roleType) ||
               (tech.getSecondaryRole() == roleType)) {
                retval.add(tech);
            }
        }

        return retval;
    }

    public List<Person> getAdmins() {
        List<Person> admins = new ArrayList<>();
        for (Person p : getActivePersonnel()) {
            if (p.isAdmin()) {
                admins.add(p);
            }
        }
        return admins;
    }

    public boolean isWorkingOnRefit(Person p) {
        Objects.requireNonNull(p);

        Unit unit = getHangar().findUnit(u ->
            u.isRefitting() && p.equals(u.getRefit().getTech()));
        return unit != null;
    }

    public List<Person> getDoctors() {
        List<Person> docs = new ArrayList<>();
        for (Person p : getActivePersonnel()) {
            if (p.isDoctor()) {
                docs.add(p);
            }
        }
        return docs;
    }

    public int getPatientsFor(Person doctor) {
        int patients = 0;
        for (Person person : getActivePersonnel()) {
            if ((null != person.getDoctorId()) && person.getDoctorId().equals(doctor.getId())) {
                patients++;
            }
        }
        return patients;
    }

    public String healPerson(Person medWork, Person doctor) {
        if (getCampaignOptions().useAdvancedMedical()) {
            return "";
        }
        String report = "";
        report += doctor.getHyperlinkedFullTitle() + " attempts to heal "
                + medWork.getFullName();
        TargetRoll target = getTargetFor(medWork, doctor);
        int roll = Compute.d6(2);
        report = report + ",  needs " + target.getValueAsString()
                + " and rolls " + roll + ":";
        int xpGained = 0;
        //If we get a natural 2 that isn't an automatic success, reroll if Edge is available and in use.
        if (getCampaignOptions().useSupportEdge()
                && doctor.getOptions().booleanOption(PersonnelOptions.EDGE_MEDICAL)) {
            if ((roll == 2) && (doctor.getCurrentEdge() > 0) && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
                doctor.changeCurrentEdge(-1);
                roll = Compute.d6(2);
                report += medWork.fail() + "\n" + doctor.getHyperlinkedFullTitle() + " uses Edge to reroll:"
                        + " rolls " + roll + ":";
            }
        }
        if (roll >= target.getValue()) {
            report = report + medWork.succeed();
            Unit u = medWork.getUnit();
            if (null != u) {
                u.resetPilotAndEntity();
            }
            if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                xpGained += getCampaignOptions().getSuccessXP();
            }
            if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                doctor.setNTasks(doctor.getNTasks() + 1);
            }
            if (doctor.getNTasks() >= getCampaignOptions().getNTasksXP()) {
                xpGained += getCampaignOptions().getTaskXP();
                doctor.setNTasks(0);
            }
        } else {
            report = report + medWork.fail();
            if (roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }
        if (xpGained > 0) {
            doctor.awardXP(xpGained);
            report += " (" + xpGained + "XP gained) ";
        }
        medWork.setDaysToWaitForHealing(getCampaignOptions()
                .getHealingWaitingPeriod());
        return report;
    }

    public TargetRoll getTargetFor(Person medWork, Person doctor) {
        Skill skill = doctor.getSkill(SkillType.S_DOCTOR);
        if (null == skill) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getFullName()
                    + " isn't a doctor, he just plays one on TV.");
        }
        if (medWork.getDoctorId() != null
                && !medWork.getDoctorId().equals(doctor.getId())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    medWork.getFullName() + " is already being tended by another doctor");
        }
        if (!medWork.needsFixing()
                && !(getCampaignOptions().useAdvancedMedical() && medWork.needsAMFixing())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    medWork.getFullName() + " does not require healing.");
        }
        if (getPatientsFor(doctor) > 25) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getFullName()
                    + " already has 25 patients.");
        }
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
                SkillType.getExperienceLevelName(skill.getExperienceLevel()));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }
        // understaffed mods
        int helpMod = getShorthandedMod(getMedicsPerDoctor(), true);
        if (helpMod > 0) {
            target.addModifier(helpMod, "shorthanded");
        }
        target.append(medWork.getHealingMods());
        return target;
    }

    public Person getLogisticsPerson() {
        int bestSkill = -1;
        int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
        Person admin = null;
        String skill = getCampaignOptions().getAcquisitionSkill();
        if (skill.equals(CampaignOptions.S_AUTO)) {
            return admin;
        } else if (skill.equals(CampaignOptions.S_TECH)) {
            for (Person p : getActivePersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(false)) {
                    continue;
                }
                if (maxAcquisitions > 0 && (p.getAcquisitions() >= maxAcquisitions)) {
                    continue;
                }
                if ((p.getBestTechSkill() != null) && p.getBestTechSkill().getLevel() > bestSkill) {
                    admin = p;
                    bestSkill = p.getBestTechSkill().getLevel();
                }
            }
        } else {
            for (Person p : getActivePersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(false)) {
                    continue;
                }
                if (maxAcquisitions > 0 && (p.getAcquisitions() >= maxAcquisitions)) {
                    continue;
                }
                if (p.hasSkill(skill) && (p.getSkill(skill).getLevel() > bestSkill)) {
                    admin = p;
                    bestSkill = p.getSkill(skill).getLevel();
                }
            }
        }
        return admin;
    }

    /**
     * Gets a list of applicable logistics personnel, or an empty list
     * if acquisitions automatically succeed.
     * @return A {@see List} of personnel who can perform logistical actions.
     */
    public List<Person> getLogisticsPersonnel() {
        String skill = getCampaignOptions().getAcquisitionSkill();
        if (skill.equals(CampaignOptions.S_AUTO)) {
            return Collections.emptyList();
        } else {
            List<Person> logisticsPersonnel = new ArrayList<>();
            int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
            for (Person p : getActivePersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(false)) {
                    continue;
                }
                if ((maxAcquisitions > 0) && (p.getAcquisitions() >= maxAcquisitions)) {
                    continue;
                }
                if (skill.equals(CampaignOptions.S_TECH)) {
                    if (null != p.getBestTechSkill()) {
                        logisticsPersonnel.add(p);
                    }
                } else if (p.hasSkill(skill)) {
                    logisticsPersonnel.add(p);
                }
            }

            // Sort by their skill level, descending.
            logisticsPersonnel.sort((a, b) -> {
                if (skill.equals(CampaignOptions.S_TECH)) {
                    return Integer.compare(b.getBestTechSkill().getLevel(), a.getBestTechSkill().getLevel());
                } else {
                    return Integer.compare(b.getSkill(skill).getLevel(), a.getSkill(skill).getLevel());
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
        for (IAcquisitionWork shoppingItem : sList.getAllShoppingItems()) {
            shoppingItem.decrementDaysToWait();
        }

        if (getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            return goShoppingAutomatically(sList);
        } else if (!getCampaignOptions().usesPlanetaryAcquisition()) {
            return goShoppingStandard(sList);
        } else {
            return goShoppingByPlanet(sList);
        }
    }

    /**
     * Shops for items on the {@link ShoppingList}, where each acquisition
     * automatically succeeds.
     *
     * @param sList The shopping list to use when shopping.
     * @return The new shopping list containing the items that were not
     *         acquired.
     */
    private ShoppingList goShoppingAutomatically(ShoppingList sList) {
        List<IAcquisitionWork> currentList = new ArrayList<>(sList.getAllShoppingItems());

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
     * Shops for items on the {@link ShoppingList}, where each acquisition
     * is performed by available logistics personnel.
     *
     * @param sList The shopping list to use when shopping.
     * @return The new shopping list containing the items that were not
     *         acquired.
     */
    private ShoppingList goShoppingStandard(ShoppingList sList) {
        List<Person> logisticsPersonnel = getLogisticsPersonnel();
        if (logisticsPersonnel.isEmpty()) {
            addReport("Your force has no one capable of acquiring equipment.");
            return sList;
        }

        List<IAcquisitionWork> currentList = new ArrayList<>(sList.getAllShoppingItems());
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
     * Shops for items on the {@link ShoppingList}, where each acquisition
     * is attempted on nearby planets by available logistics personnel.
     *
     * @param sList The shopping list to use when shopping.
     * @return The new shopping list containing the items that were not
     *         acquired.
     */
    private ShoppingList goShoppingByPlanet(ShoppingList sList) {
        List<Person> logisticsPersonnel = getLogisticsPersonnel();
        if (logisticsPersonnel.isEmpty()) {
            addReport("Your force has no one capable of acquiring equipment.");
            return sList;
        }

        // we are shopping by planets, so more involved
        List<IAcquisitionWork> currentList = sList.getAllShoppingItems();
        LocalDate currentDate = getLocalDate();

        // a list of items than can be taken out of the search and put back on the
        // shopping list
        List<IAcquisitionWork> shelvedItems = new ArrayList<>();

        //find planets within a certain radius - the function will weed out dead planets
        List<PlanetarySystem> systems = Systems.getInstance().getShoppingSystems(getCurrentSystem(),
                getCampaignOptions().getMaxJumpsPlanetaryAcquisition(), currentDate);

        for (Person person : logisticsPersonnel) {
            if (currentList.isEmpty()) {
                // Nothing left to shop for!
                break;
            }

            String personTitle = person.getHyperlinkedFullTitle() + " ";

            for (PlanetarySystem system: systems) {
                if (currentList.isEmpty()) {
                    // Nothing left to shop for!
                    break;
                }

                List<IAcquisitionWork> remainingItems = new ArrayList<>();

                //loop through shopping list. If its time to check, then check as appropriate. Items not
                //found get added to the remaining item list. Rotate through personnel
                boolean done = false;
                for (IAcquisitionWork shoppingItem : currentList) {
                    if (!canAcquireParts(person)) {
                        remainingItems.add(shoppingItem);
                        done = true;
                        continue;
                    }

                    if (shoppingItem.getDaysToWait() <= 0) {
                        if (findContactForAcquisition(shoppingItem, person, system)) {
                            int transitTime = calculatePartTransitTime(system);
                            int totalQuantity = 0;
                            while (shoppingItem.getQuantity() > 0
                                    && canAcquireParts(person)
                                    && acquireEquipment(shoppingItem, person, system, transitTime)) {
                                totalQuantity++;
                            }
                            if (totalQuantity > 0) {
                                addReport(personTitle + "<font color='green'><b> found "
                                        + shoppingItem.getQuantityName(totalQuantity)
                                        + " on "
                                        + system.getPrintableName(currentDate)
                                        + ". Delivery in " + transitTime + " days.</b></font>");
                            }
                        }
                    }
                    // if we didn't find everything on this planet, then add to the remaining list
                    if (shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {
                        // if we can't afford it, then don't keep searching for it on other planets
                        if (!canPayFor(shoppingItem)) {
                            if (!getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                                addReport("<font color='red'><b>You cannot afford to purchase another "
                                        + shoppingItem.getAcquisitionName() + "</b></font>");
                            }
                            shelvedItems.add(shoppingItem);
                        } else {
                            remainingItems.add(shoppingItem);
                        }
                    }
                }

                // we are done with this planet. replace our current list with the remaining items
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
     * @param person The {@link Person} to check if they have remaining
     *               time to perform acquisitions.
     * @return True if {@code person} could acquire another part, otherwise false.
     */
    public boolean canAcquireParts(@Nullable Person person) {
        if (person == null) {
            // CAW: in this case we're using automatic success
            //      and the logistics person will be null.
            return true;
        }
        int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
        return maxAcquisitions <= 0
            || person.getAcquisitions() < maxAcquisitions;
    }

    /***
     * Checks whether the campaign can pay for a given <code>IAcquisitionWork</code> item. This will check
     * both whether the campaign is required to pay for a given type of acquisition by the options and
     * if so whether it has enough money to afford it.
     * @param acquisition - An <code>IAcquisitionWork<code> object
     * @return true if the campaign can pay for the acquisition; false if it cannot.
     */
    public boolean canPayFor(IAcquisitionWork acquisition) {
        //SHOULD we check to see if this acquisition needs to be paid for
        if ( (acquisition instanceof UnitOrder && getCampaignOptions().payForUnits())
                ||(acquisition instanceof Part && getCampaignOptions().payForParts()) ) {
            //CAN the acquisition actually be paid for
            return getFunds().isGreaterOrEqualThan(acquisition.getBuyCost());
        }
        return true;
    }

    /**
     * Make an acquisition roll for a given planet to see if you can identify a contact. Used for planetary based acquisition.
     * @param acquisition - The <code> IAcquisitionWork</code> being acquired.
     * @param person - The <code>Person</code> object attempting to do the acquiring.  may be null if no one on the force has the skill or the user is using automatic acquisition.
     * @param system - The <code>PlanetarySystem</code> object where the acquisition is being attempted. This may be null if the user is not using planetary acquisition.
     * @return true if your target roll succeeded.
     */
    public boolean findContactForAcquisition(IAcquisitionWork acquisition, Person person, PlanetarySystem system) {
        TargetRoll target = getTargetForAcquisition(acquisition, person, false);
        target = system.getPrimaryPlanet().getAcquisitionMods(target, getLocalDate(), getCampaignOptions(), getFaction(),
                acquisition.getTechBase() == Part.T_CLAN);

        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                addReport("<font color='red'><b>Can't search for " + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + " because:</b></font> " + target.getDesc());
            }
            return false;
        }
        if (Compute.d6(2) < target.getValue()) {
            //no contacts on this planet, move along
            if (getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                addReport("<font color='red'><b>No contacts available for " + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + "</b></font>");
            }
            return false;
        } else {
            if (getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                addReport("<font color='green'>Possible contact for " + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + "</font>");
            }
            return true;
        }
    }

    /***
     * Attempt to acquire a given <code>IAcquisitionWork</code> object.
     * This is the default method used by for non-planetary based acquisition.
     * @param acquisition  - The <code> IAcquisitionWork</code> being acquired.
     * @param person - The <code>Person</code> object attempting to do the acquiring.  may be null if no one on the force has the skill or the user is using automatic acquisition.
     * @return a boolean indicating whether the attempt to acquire equipment was successful.
     */
    public boolean acquireEquipment(IAcquisitionWork acquisition, Person person) {
        return acquireEquipment(acquisition, person, null, -1);
    }

    /***
     * Attempt to acquire a given <code>IAcquisitionWork</code> object.
     * @param acquisition - The <code> IAcquisitionWork</code> being acquired.
     * @param person - The <code>Person</code> object attempting to do the acquiring.  may be null if no one on the force has the skill or the user is using automatic acquisition.
     * @param system - The <code>PlanetarySystem</code> object where the acquisition is being attempted. This may be null if the user is not using planetary acquisition.
     * @param transitDays - The number of days that the part should take to be delivered. If this value is entered as -1, then this method will determine transit time based on the users campaign options.
     * @return a boolean indicating whether the attempt to acquire equipment was successful.
     */
    private boolean acquireEquipment(IAcquisitionWork acquisition, Person person, PlanetarySystem system, int transitDays) {
        boolean found = false;
        String report = "";

        if (null != person) {
            report += person.getHyperlinkedFullTitle() + " ";
        }

        TargetRoll target = getTargetForAcquisition(acquisition, person, false);

        //check on funds
        if (!canPayFor(acquisition)) {
            target.addModifier(TargetRoll.IMPOSSIBLE, "Cannot afford this purchase");
        }

        if (null != system) {
            target = system.getPrimaryPlanet().getAcquisitionMods(target, getLocalDate(),
                    getCampaignOptions(), getFaction(), acquisition.getTechBase() == Part.T_CLAN);
        }

        report += "attempts to find " + acquisition.getAcquisitionName();

        //if impossible then return
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            report += ":<font color='red'><b> " + target.getDesc() + "</b></font>";
            if (!getCampaignOptions().usesPlanetaryAcquisition() || getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                addReport(report);
            }
            return false;
        }


        int roll = Compute.d6(2);
        report += "  needs " + target.getValueAsString();
        report += " and rolls " + roll + ":";
        //Edge reroll, if applicable
        if (getCampaignOptions().useSupportEdge() && (roll < target.getValue()) && (person != null)
                && person.getOptions().booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL)
                && (person.getCurrentEdge() > 0)) {
            person.changeCurrentEdge(-1);
            roll = Compute.d6(2);
            report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
        }
        int mos = roll - target.getValue();
        if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            mos = roll - 2;
        }
        int xpGained = 0;
        if (roll >= target.getValue()) {
            if (transitDays < 0) {
                transitDays = calculatePartTransitTime(mos);
            }
            report = report + acquisition.find(transitDays);
            found = true;
            if (person != null) {
                if (roll == 12
                        && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
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
            if (person != null && roll == 2
                    && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }

        if (null != person) {
            // The person should have their acquisitions incremented
            person.incrementAcquisition();

            if (xpGained > 0) {
                person.awardXP(xpGained);
                report += " (" + xpGained + "XP gained) ";
            }
        }

        if (found) {
            acquisition.decrementQuantity();
            MekHQ.triggerEvent(new AcquisitionEvent(acquisition));
        }
        if (!getCampaignOptions().usesPlanetaryAcquisition() || getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
            addReport(report);
        }
        return found;
    }

    /**
     * Performs work to either mothball or activate a unit.
     * @param u The unit to either work towards mothballing or activation.
     */
    public void workOnMothballingOrActivation(Unit u) {
        if (u.isMothballed()) {
            activate(u);
        } else {
            mothball(u);
        }
    }

    /**
     * Performs work to mothball a unit.
     * @param u The unit on which to perform mothball work.
     */
    public void mothball(Unit u) {
        if (u.isMothballed()) {
            MekHQ.getLogger().warning(Campaign.class, "mothball(Unit)", "Unit is already mothballed, cannot mothball.");
            return;
        }

        Person tech = u.getTech();
        if (null == tech) {
            //uh-oh
            addReport("No tech assigned to the mothballing of " + u.getHyperlinkedName());
            return;
        }

        //don't allow overtime minutes for mothballing because its cheating
        //since you don't roll
        int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());

        //check astech time
        if (!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
            //uh-oh
            addReport("Not enough astechs to work on mothballing of " + u.getHyperlinkedName());
            return;
        }

        u.setMothballTime(u.getMothballTime() - minutes);

        String report = tech.getHyperlinkedFullTitle() + " spent " + minutes + " minutes mothballing " + u.getHyperlinkedName();
        if (!u.isMothballing()) {
            u.completeMothball();
            report += ". Mothballing complete.";
        } else {
            report += ". " + u.getMothballTime() + " minutes remaining.";
        }

        tech.setMinutesLeft(tech.getMinutesLeft() - minutes);

        if (!u.isSelfCrewed()) {
            astechPoolMinutes -= 6 * minutes;
        }

        addReport(report);
    }

    /**
     * Performs work to activate a unit.
     * @param u The unit on which to perform activation work.
     */
    public void activate(Unit u) {
        if (!u.isMothballed()) {
            MekHQ.getLogger().warning(Campaign.class, "activate(Unit)", "Unit is already activated, cannot activate.");
            return;
        }

        Person tech = u.getTech();
        if (null == tech) {
            //uh-oh
            addReport("No tech assigned to the activation of " + u.getHyperlinkedName());
            return;
        }

        //don't allow overtime minutes for activation because its cheating
        //since you don't roll
        int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());

        //check astech time
        if (!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
            //uh-oh
            addReport("Not enough astechs to work on activation of " + u.getHyperlinkedName());
            return;
        }

        u.setMothballTime(u.getMothballTime() - minutes);

        String report = tech.getHyperlinkedFullTitle() + " spent " + minutes + " minutes activating " + u.getHyperlinkedName();

        tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        if (!u.isSelfCrewed()) {
            astechPoolMinutes -= 6 * minutes;
        }

        if (!u.isMothballing()) {
            u.completeActivation();
            report += ". Activation complete.";
        } else {
            report += ". " + u.getMothballTime() + " minutes remaining.";
        }

        addReport(report);
    }

    public void refit(Refit r) {
        Person tech = (r.getUnit().getEngineer() == null) ? r.getTech() : r.getUnit().getEngineer();
        if (tech == null) {
            addReport("No tech is assigned to refit " + r.getOriginalEntity().getShortName() + ". Refit cancelled.");
            r.cancel();
            return;
        }
        TargetRoll target = getTargetFor(r, tech);
        // check that all parts have arrived
        if (!r.acquireParts()) {
            return;
        }
        String report = tech.getHyperlinkedFullTitle() + " works on " + r.getPartName();
        int minutes = r.getTimeLeft();
        // FIXME: Overtime?
        if (minutes > tech.getMinutesLeft()) {
            r.addTimeSpent(tech.getMinutesLeft());
            tech.setMinutesLeft(0);
            report = report + ", " + r.getTimeLeft() + " minutes left.";
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
            r.addTimeSpent(minutes);
            if (r.hasFailedCheck()) {
                report = report + ", " + r.succeed();
            } else {
                int roll;
                String wrongType = "";
                if (tech.isRightTechTypeFor(r)) {
                    roll = Compute.d6(2);
                } else {
                    roll = Utilities.roll3d6();
                    wrongType = " <b>Warning: wrong tech type for this refit.</b>";
                }
                report = report + ",  needs " + target.getValueAsString() + " and rolls " + roll + ": ";
                if (getCampaignOptions().useSupportEdge() && (roll < target.getValue())
                        && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT)
                        && (tech.getCurrentEdge() > 0)) {
                    tech.changeCurrentEdge(-1);
                    roll = tech.isRightTechTypeFor(r) ? Compute.d6(2) : Utilities.roll3d6();
                    // This is needed to update the edge values of individual crewmen
                    if (tech.isEngineer()) {
                        tech.setEdgeUsed(tech.getEdgeUsed() - 1);
                    }
                    report += " <b>failed!</b> but uses Edge to reroll...getting a " + roll + ": ";
                }

                if (roll >= target.getValue()) {
                    report += r.succeed();
                } else {
                    report += r.fail(SkillType.EXP_GREEN);
                    // try to refit again in case the tech has any time left
                    if (!r.isBeingRefurbished()) {
                        refit(r);
                    }
                }
                report += wrongType;
            }
        }
        MekHQ.triggerEvent(new PartWorkEvent(tech, r));
        addReport(report);
    }

    public Part fixWarehousePart(Part part, Person tech) {
        // get a new cloned part to work with and decrement original
        Part repairable = part.clone();
        part.decrementQuantity();

        fixPart(repairable, tech);
        if (!(repairable instanceof OmniPod)) {
            getQuartermaster().addPart(repairable, 0);
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
     * @param partWork - the {@link IPartWork} to be fixed
     * @param tech - the {@link Person} who will attempt to fix the part
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
        if ((partWork instanceof ProtomekArmor) && !partWork.isSalvaging()) {
            if (!((ProtomekArmor) partWork).isInSupply()) {
                report += "<b>Not enough Protomech armor remaining.  Task suspended.</b>";
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
            //Change the string since we're not working on the part itself
            report += tech.getHyperlinkedFullTitle() + " attempts to" + action
                    + "a heat sink";
        } else {
            report += tech.getHyperlinkedFullTitle() + " attempts to" + action
                    + partWork.getPartName();
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
                int helpMod = getShorthandedMod(
                        getAvailableAstechs(minutesUsed, usedOvertime), false);
                if ((null != partWork.getUnit())
                        && ((partWork.getUnit().getEntity() instanceof Dropship)
                                || (partWork.getUnit().getEntity() instanceof Jumpship))) {
                    helpMod = 0;
                }
                if (partWork.getShorthandedMod() < helpMod) {
                    partWork.setShorthandedMod(helpMod);
                }
                partWork.setTech(tech);
                partWork.reservePart();
                report += " - <b>Not enough time, the remainder of the task";
                if (null != partWork.getUnit()) {
                    report += " on " + partWork.getUnit().getName();
                }
                if (minutesUsed > 0) {
                    report += " will be finished tomorrow.</b>";
                } else {
                    report += " cannot be finished because there was no time left after maintenance tasks.</b>";
                    partWork.resetTimeSpent();
                    partWork.resetOvertime();
                    partWork.setTech(null);
                    partWork.cancelReservation();
                }
                MekHQ.triggerEvent(new PartWorkEvent(tech, partWork));
                addReport(report);
                return report;
            }
        } else {
            tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
        }
        int astechMinutesUsed = minutesUsed
                * getAvailableAstechs(minutesUsed, usedOvertime);
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
            roll = Compute.d6(2);
        } else {
            roll = Utilities.roll3d6();
            wrongType = " <b>Warning: wrong tech type for this repair.</b>";
        }
        report = report + ",  needs " + target.getValueAsString()
                + " and rolls " + roll + ":";
        int xpGained = 0;
        //if we fail and would break a part, here's a chance to use Edge for a reroll...
        if (getCampaignOptions().useSupportEdge()
                && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART)
                && (tech.getCurrentEdge() > 0)
                && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
            if ((getCampaignOptions().isDestroyByMargin()
                    && (getCampaignOptions().getDestroyMargin() <= (target.getValue() - roll)))
                    || (!getCampaignOptions().isDestroyByMargin()
                            //if an elite, primary tech and destroy by margin is NOT on
                            && ((tech.getExperienceLevel(false) == SkillType.EXP_ELITE)
                                    || (tech.getPrimaryRole() == Person.T_SPACE_CREW))) // For vessel crews
                    && (roll < target.getValue())) {
                tech.changeCurrentEdge(-1);
                roll = tech.isRightTechTypeFor(partWork) ? Compute.d6(2) : Utilities.roll3d6();
                //This is needed to update the edge values of individual crewmen
                if (tech.isEngineer()) {
                    tech.setEdgeUsed(tech.getEdgeUsed() + 1);
                }
                report += " <b>failed!</b> and would destroy the part, but uses Edge to reroll...getting a " + roll + ":";
            }
        }

        if (roll >= target.getValue()) {
            report = report + partWork.succeed();
            if (getCampaignOptions().payForRepairs()
                    && action.equals(" fix ")
                    && !(partWork instanceof Armor)) {
                Money cost = ((Part) partWork).getStickerPrice().multipliedBy(0.2);
                report += "<br>Repairs cost " +
                        cost.toAmountAndSymbolString() +
                        " worth of parts.";
                finances.debit(cost, Transaction.C_REPAIRS, "Repair of " + partWork.getPartName(), getLocalDate());
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
            int effectiveSkillLvl = tech.getSkillForWorkingOn(partWork).getExperienceLevel() - modePenalty;
            if (getCampaignOptions().isDestroyByMargin()) {
                if (getCampaignOptions().getDestroyMargin() > (target.getValue() - roll)) {
                    // not destroyed - set the effective level as low as
                    // possible
                    effectiveSkillLvl = SkillType.EXP_ULTRA_GREEN;
                } else {
                    // destroyed - set the effective level to elite
                    effectiveSkillLvl = SkillType.EXP_ELITE;
                }
            }
            report = report + partWork.fail(effectiveSkillLvl);

            if ((roll == 2) && (target.getValue() != TargetRoll.AUTOMATIC_FAIL)) {
                xpGained += getCampaignOptions().getMistakeXP();
            }
        }
        if (xpGained > 0) {
            tech.awardXP(xpGained);
            report += " (" + xpGained + "XP gained) ";
        }
        report += wrongType;
        partWork.resetTimeSpent();
        partWork.resetOvertime();
        partWork.setTech(null);
        partWork.cancelReservation();
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
        //read the news
        for (NewsItem article : news.fetchNewsFor(getLocalDate())) {
            addReport(article.getHeadlineForReport());
        }
        for (NewsItem article : Systems.getInstance().getPlanetaryNews(getLocalDate())) {
            addReport(article.getHeadlineForReport());
        }
    }

    public int getDeploymentDeficit(AtBContract contract) {
        if (!contract.isActive()) {
            // Inactive contracts have no deficits.
            return 0;
        } else if (contract.getStartDate().compareTo(getLocalDate()) >= 0) {
            // Do not check for deficits if the contract has not started or
            // it is the first day of the contract, as players won't have
            // had time to assign forces to the contract yet
            return 0;
        }

        int total = -contract.getRequiredLances();
        int role = -Math.max(1, contract.getRequiredLances() / 2);

        for (Lance l : lances.values()) {
            if ((l.getMissionId() == contract.getId()) && (l.getRole() != AtBLanceRole.UNASSIGNED)) {
                total++;
                if (l.getRole() == contract.getRequiredLanceType()) {
                    role++;
                }
            }
        }

        if (total >= 0 && role >= 0) {
            return 0;
        }
        return Math.abs(Math.min(total, role));
    }

    private void processNewDayATBScenarios() {
        for (Mission m : getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract) || getLocalDate().isBefore(((Contract) m).getStartDate())) {
                continue;
            }
            /*
             * Situations like a delayed start or running out of funds during transit can
             * delay arrival until after the contract start. In that case, shift the
             * starting and ending dates before making any battle rolls. We check that the
             * unit is actually on route to the planet in case the user is using a custom
             * system for transport or splitting the unit, etc.
             */
            if (!getLocation().isOnPlanet() &&
                    !getLocation().getJumpPath().isEmpty() &&
                    getLocation().getJumpPath().getLastSystem().getId().equals(m.getSystemId())) {

                // transitTime is measured in days; so we round up to the next whole day
                ((AtBContract) m).setStartAndEndDate(getLocalDate().plusDays((int) Math.ceil(getLocation().getTransitTime())));
                addReport("The start and end dates of " + m.getName() + " have been shifted to reflect the current ETA.");
                continue;
            }
            if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                int deficit = getDeploymentDeficit((AtBContract) m);
                if (deficit > 0) {
                    ((AtBContract) m).addPlayerMinorBreaches(deficit);
                    addReport("Failure to meet " + m.getName() + " requirements resulted in " + deficit
                            + ((deficit == 1) ? " minor contract breach" : " minor contract breaches"));
                }
            }

            for (Scenario s : m.getScenarios()) {
                if (!s.isCurrent() || !(s instanceof AtBScenario)) {
                    continue;
                }
                if ((s.getDate() != null) && s.getDate().isBefore(getLocalDate())) {
                    s.setStatus(Scenario.S_DEFEAT);
                    s.clearAllForcesAndPersonnel(this);
                    ((AtBContract) m).addPlayerMinorBreach();
                    addReport("Failure to deploy for " + s.getName()
                            + " resulted in defeat and a minor contract breach.");
                    s.generateStub(this);
                }
            }
        }

        if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            AtBScenarioFactory.createScenariosForNewWeek(this);
        }

        for (Mission m : getMissions()) {
            if (m.isActive() && (m instanceof AtBContract)
                    && !((AtBContract) m).getStartDate().isAfter(getLocalDate())) {
                ((AtBContract) m).checkEvents(this);
            }
            /*
             * If there is a standard battle set for today, deploy the lance.
             */
            for (Scenario s : m.getScenarios()) {
                if ((s.getDate() != null) && s.getDate().equals(getLocalDate())) {
                    int forceId = ((AtBScenario) s).getLanceForceId();
                    if ((lances.get(forceId) != null) && !forceIds.get(forceId).isDeployed()) {

                        // If any unit in the force is under repair, don't deploy the force
                        // Merely removing the unit from deployment would break with user expectation
                        boolean forceUnderRepair = false;
                        for (UUID uid : forceIds.get(forceId).getAllUnits(true)) {
                            Unit u = getHangar().getUnit(uid);
                            if ((u != null) && u.isUnderRepair()) {
                                forceUnderRepair = true;
                                break;
                            }
                        }

                        if (!forceUnderRepair) {
                            forceIds.get(forceId).setScenarioId(s.getId());
                            s.addForces(forceId);
                            for (UUID uid : forceIds.get(forceId).getAllUnits(true)) {
                                Unit u = getHangar().getUnit(uid);
                                if (null != u) {
                                    u.setScenarioId(s.getId());
                                }
                            }

                            addReport(MessageFormat.format(
                                resources.getString("atbMissionTodayWithForce.format"),
                                s.getName(), forceIds.get(forceId).getName()));
                            MekHQ.triggerEvent(new DeploymentChangedEvent(forceIds.get(forceId), s));
                        } else {
                            addReport(MessageFormat.format(
                                resources.getString("atbMissionToday.format"), s.getName()));
                        }
                    } else {
                        addReport(MessageFormat.format(
                            resources.getString("atbMissionToday.format"), s.getName()));
                    }
                }
            }
        }
    }

    private void processNewDayATBFatigue() {
        boolean inContract = false;
        for (Mission m : getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract)
                    || getLocalDate().isBefore(((Contract) m).getStartDate())) {
                continue;
            }
            switch (((AtBContract) m).getMissionType()) {
                case AtBContract.MT_GARRISONDUTY:
                case AtBContract.MT_SECURITYDUTY:
                case AtBContract.MT_CADREDUTY:
                    fatigueLevel -= 1;
                    break;
                case AtBContract.MT_RIOTDUTY:
                case AtBContract.MT_GUERRILLAWARFARE:
                case AtBContract.MT_PIRATEHUNTING:
                    fatigueLevel += 1;
                    break;
                case AtBContract.MT_RELIEFDUTY:
                case AtBContract.MT_PLANETARYASSAULT:
                    fatigueLevel += 2;
                    break;
                case AtBContract.MT_DIVERSIONARYRAID:
                case AtBContract.MT_EXTRACTIONRAID:
                case AtBContract.MT_RECONRAID:
                case AtBContract.MT_OBJECTIVERAID:
                    fatigueLevel += 3;
                    break;
            }
            inContract = true;
        }
        if (!inContract && location.isOnPlanet()) {
            fatigueLevel -= 2;
        }
        fatigueLevel = Math.max(fatigueLevel, 0);
    }

    private void processNewDayATB() {
        contractMarket.generateContractOffers(this);
        unitMarket.generateUnitOffers(this);

        if ((getShipSearchExpiration() != null) && !getShipSearchExpiration().isAfter(getLocalDate())) {
            setShipSearchExpiration(null);
            if (getShipSearchResult() != null) {
                addReport("Opportunity for purchase of " + getShipSearchResult() + " has expired.");
                setShipSearchResult(null);
            }
        }

        if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            processShipSearch();
        }

        // Add or remove dependents - only if one of the two options makes this possible is enabled
        if ((getLocalDate().getDayOfYear() == 1)
                && (!getCampaignOptions().getDependentsNeverLeave() || getCampaignOptions().canAtBAddDependents())) {
            int numPersonnel = 0;
            List<Person> dependents = new ArrayList<>();
            for (Person p : getActivePersonnel()) {
                numPersonnel++;
                if (p.isDependent()) {
                    dependents.add(p);
                }
            }
            int roll = Compute.d6(2) + getUnitRatingMod() - 2;
            if (roll < 2) {
                roll = 2;
            } else if (roll > 12) {
                roll = 12;
            }
            int change = numPersonnel * (roll - 5) / 100;
            if (change < 0) {
                if (!getCampaignOptions().getDependentsNeverLeave()) {
                    while ((change < 0) && (dependents.size() > 0)) {
                        removePerson(Utilities.getRandomItem(dependents).getId());
                        change++;
                    }
                }
            } else {
                if (getCampaignOptions().canAtBAddDependents()) {
                    for (int i = 0; i < change; i++) {
                        Person p = newDependent(Person.T_ASTECH, false);
                        recruitPerson(p);
                    }
                }
            }
        }

        if (getLocalDate().getDayOfMonth() == 1) {
            /*
             * First of the month; roll morale, track unit fatigue.
             */

            IUnitRating rating = getUnitRating();
            rating.reInitialize();

            for (Mission m : getMissions()) {
                if (m.isActive() && (m instanceof AtBContract)
                        && !((AtBContract) m).getStartDate().isAfter(getLocalDate())) {
                    ((AtBContract) m).checkMorale(getLocalDate(), getUnitRatingMod());
                    addReport("Enemy morale is now " + ((AtBContract) m).getMoraleLevelName()
                            + " on contract " + m.getName());
                }
            }

            // Account for fatigue
            if (getCampaignOptions().getTrackUnitFatigue()) {
                processNewDayATBFatigue();
            }
        }

        processNewDayATBScenarios();
    }

    public void processNewDayPersonnel() {
        // This MUST use getActivePersonnel as we only want to process active personnel, and
        // furthermore this allows us to add and remove personnel without issue
        for (Person p : getActivePersonnel()) {
            // Random Death

            // Random Marriages
            if (getCampaignOptions().useRandomMarriages()) {
                p.randomMarriage(this);
            }

            p.resetMinutesLeft();
            // Reset acquisitions made to 0
            p.setAcquisition(0);
            if (p.needsFixing() && !getCampaignOptions().useAdvancedMedical()) {
                p.decrementDaysToWaitForHealing();
                Person doctor = getPerson(p.getDoctorId());
                if ((doctor != null) && doctor.isDoctor()) {
                    if (p.getDaysToWaitForHealing() <= 0) {
                        addReport(healPerson(p, doctor));
                    }
                } else if (p.checkNaturalHealing(15)) {
                    addReport(p.getHyperlinkedFullTitle() + " heals naturally!");
                    Unit u = p.getUnit();
                    if (u != null) {
                        u.resetPilotAndEntity();
                    }
                }
            }
            // TODO Advanced Medical needs to go away from here later on
            if (getCampaignOptions().useAdvancedMedical()) {
                InjuryUtil.resolveDailyHealing(this, p);
                Unit u = p.getUnit();
                if (u != null) {
                    u.resetPilotAndEntity();
                }
            }

            // TODO : Reset this based on hasSupportRole(false) instead of checking for each type
            // TODO : p.isEngineer will need to stay, however
            // Reset edge points to the purchased value each week. This should only
            // apply for support personnel - combat troops reset with each new mm game
            if ((p.isAdmin() || p.isDoctor() || p.isEngineer() || p.isTech())
                    && (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY)) {
                p.resetCurrentEdge();
            }

            if ((getCampaignOptions().getIdleXP() > 0) && (getLocalDate().getDayOfMonth() == 1)
                    && !p.getPrisonerStatus().isPrisoner()) { // Prisoners can't gain XP, while Bondsmen can gain xp
                p.setIdleMonths(p.getIdleMonths() + 1);
                if (p.getIdleMonths() >= getCampaignOptions().getMonthsIdleXP()) {
                    if (Compute.d6(2) >= getCampaignOptions().getTargetIdleXP()) {
                        p.awardXP(getCampaignOptions().getIdleXP());
                        addReport(p.getHyperlinkedFullTitle() + " has gained "
                                + getCampaignOptions().getIdleXP() + " XP");
                    }
                    p.setIdleMonths(0);
                }
            }

            // Procreation
            if (p.getGender().isFemale()) {
                if (p.isPregnant()) {
                    if (getCampaignOptions().useUnofficialProcreation()) {
                        if (getLocalDate().compareTo((p.getDueDate())) == 0) {
                            p.birth(this);
                        }
                    } else {
                        p.removePregnancy();
                    }
                } else if (getCampaignOptions().useUnofficialProcreation()) {
                    p.procreate(this);
                }
            }
        }
    }

    public void processNewDayUnits() {
        // need to loop through units twice, the first time to do all maintenance and
        // the second time to do whatever else. Otherwise, maintenance minutes might
        // get sucked up by other stuff. This is also a good place to ensure that a
        // unit's engineer gets reset and updated.
        for (Unit u : getUnits()) {
            u.resetEngineer();
            if (null != u.getEngineer()) {
                u.getEngineer().resetMinutesLeft();
            }

            // do maintenance checks
            doMaintenance(u);
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
                    fixPart(part, tech);
                } else {
                    addReport(String.format(
                            "%s looks at %s, recalls his total lack of skill for working with such technology, then slowly puts the tools down before anybody gets hurt.",
                            tech.getHyperlinkedFullTitle(), part.getName()));
                    part.setTech(null);
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "Could not find tech for part: " + part.getName() + " on unit: "
                                + part.getUnit().getHyperlinkedName(),
                        "Invalid Auto-continue", JOptionPane.ERROR_MESSAGE);
            }

            // check to see if this part can now be combined with other spare parts
            if (part.isSpare() && (part.getQuantity() > 0)) {
                getQuartermaster().addPart(part, 0);
            }
        }

        // ok now we can check for other stuff we might need to do to units
        List<UUID> unitsToRemove = new ArrayList<>();
        for (Unit u : getUnits()) {
            if (u.isRefitting()) {
                refit(u.getRefit());
            }
            if (u.isMothballing()) {
                workOnMothballingOrActivation(u);
            }
            if (!u.isPresent()) {
                u.checkArrival();
            }
            if (!u.isRepairable() && !u.hasSalvageableParts()) {
                unitsToRemove.add(u.getId());
            }
        }
        // Remove any unrepairable, unsalvageable units
        unitsToRemove.forEach(this::removeUnit);

        // Finally, run Mass Repair Mass Salvage if desired
        if (MekHQ.getMekHQOptions().getNewDayMRMS()) {
            MassRepairService.massRepairSalvageAllUnits(this);
        }
    }

    /** @return <code>true</code> if the new day arrived */
    public boolean newDay() {
        if (MekHQ.triggerEvent(new DayEndingEvent(this))) {
            return false;
        }

        // Autosave based on the previous day's information
        this.autosaveService.requestDayAdvanceAutosave(this);

        // Advance the day by one
        currentDay = currentDay.plus(1, ChronoUnit.DAYS);

        // Determine if we have an active contract or not, as this can get used elsewhere before
        // we actually hit the AtB new day (e.g. personnel market)
        if (getCampaignOptions().getUseAtB()) {
            setHasActiveContract();
        }

        // Clear Reports
        getCurrentReport().clear();
        setCurrentReportHTML("");
        newReports.clear();
        beginReport("<b>" + MekHQ.getMekHQOptions().getLongDisplayFormattedDate(getLocalDate()) + "</b>");

        // New Year Changes
        if (getLocalDate().getDayOfYear() == 1) {
            // News is reloaded
            reloadNews();

            // Change Year Game Option
            getGameOptions().getOption("year").setValue(getGameYear());
        }

        readNews();

        getLocation().newDay(this);

        // Manage the personnel market
        getPersonnelMarket().generatePersonnelForDay(this);

        // Process New Day for AtB
        if (getCampaignOptions().getUseAtB()) {
            processNewDayATB();
        }

        processNewDayPersonnel();

        resetAstechMinutes();

        processNewDayUnits();

        setShoppingList(goShopping(getShoppingList()));

        // check for anything in finances
        getFinances().newDay(this);

        MekHQ.triggerEvent(new NewDayEvent(this));
        return true;
    }

    /**
     * @return a list of all currently active contracts
     */
    public List<Contract> getActiveContracts() {
        List<Contract> active = new ArrayList<>();
        for (Mission mission : getMissions()) {
            if (!(mission instanceof Contract)) {
                continue;
            }
            Contract contract = (Contract) mission;
            if (contract.isActive()
                    && !getLocalDate().isAfter(contract.getEndingDate())
                    && !getLocalDate().isBefore(contract.getStartDate())) {
                active.add(contract);
            }
        }
        return active;
    }

    /**
     * @return whether or not the current campaign has an active contract for the current date
     */
    public boolean hasActiveContract() {
        return hasActiveContract;
    }

    /**
     * This is used to check if the current campaign has one or more active contacts, and sets the
     * value of hasActiveContract based on that check. This value should not be set elsewhere
     */
    public void setHasActiveContract() {
        hasActiveContract = false;
        for (Mission mission : getMissions()) {
            if (!(mission instanceof Contract)) {
                continue;
            }
            Contract contract = (Contract) mission;

            if (contract.isActive()
                    && !getLocalDate().isAfter(contract.getEndingDate())
                    && !getLocalDate().isBefore(contract.getStartDate())) {
                hasActiveContract = true;
                break;
            }
        }
    }

    public Person getFlaggedCommander() {
        for (Person p : getPersonnel()) {
            if (p.isCommander()) {
                return p;
            }
        }
        return null;
    }

    public void removeUnit(UUID id) {
        Unit unit = getHangar().getUnit(id);

        // remove all parts for this unit as well
        for (Part p : unit.getParts()) {
            getWarehouse().removePart(p);
        }

        // remove any personnel from this unit
        for (Person p : unit.getCrew()) {
            unit.remove(p, true);
        }

        Person tech = unit.getTech();
        if (null != tech) {
            unit.remove(tech, true);
        }

        // remove unit from any forces
        removeUnitFromForce(unit);

        // If this is a ship, remove it from the list of potential transports
        removeTransportShip(unit);

        // If this unit was assigned to a transport ship, remove it from the transport
        if (unit.hasTransportShipAssignment()) {
            unit.getTransportShipAssignment()
                    .getTransportShip()
                    .unloadFromTransportShip(unit);
        }

        // finally remove the unit
        getHangar().removeUnit(unit.getId());

        checkDuplicateNamesDuringDelete(unit.getEntity());
        addReport(unit.getName() + " has been removed from the unit roster.");
        MekHQ.triggerEvent(new UnitRemovedEvent(unit));
    }

    public void removePerson(UUID id) {
        removePerson(id, true);
    }

    public void removePerson(UUID id, boolean log) {
        Person person = getPerson(id);

        if (person == null) {
            return;
        }

        person.getGenealogy().clearGenealogy(this);

        Unit u = person.getUnit();
        if (null != u) {
            u.remove(person, true);
        }
        removeAllPatientsFor(person);
        removeAllTechJobsFor(person);
        removeKillsFor(person.getId());
        getRetirementDefectionTracker().removePerson(person);

        if (log) {
            addReport(person.getFullTitle() + " has been removed from the personnel roster.");
        }

        personnel.remove(id);

        // Deal with Astech Pool Minutes
        if (person.getPrimaryRole() == Person.T_ASTECH) {
            astechPoolMinutes = Math.max(0, astechPoolMinutes - 480);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - 240);
        } else if (person.getSecondaryRole() == Person.T_ASTECH) {
            astechPoolMinutes = Math.max(0, astechPoolMinutes - 240);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - 120);
        }
        MekHQ.triggerEvent(new PersonRemovedEvent(person));
    }

    public void awardTrainingXP(Lance l) {
        awardTrainingXPByMaximumRole(l);
    }

    /**
     * Awards XP to the lance based on the maximum experience level of its
     * commanding officer and the minimum experience level of the unit's
     * members.
     * @param l The {@link Lance} to calculate XP to award for training.
     */
    private void awardTrainingXPByMaximumRole(Lance l) {
        for (UUID trainerId : forceIds.get(l.getForceId()).getAllUnits(true)) {
            Unit trainerUnit = getHangar().getUnit(trainerId);

            // not sure how this occurs, but it probably shouldn't halt processing of a new day.
            if (trainerUnit == null) {
                continue;
            }

            Person commander = trainerUnit.getCommander();
            // AtB 2.31: Training lance – needs a officer with Veteran skill levels
            //           and adds 1xp point to every Green skilled unit.
            if (commander != null && commander.getRank().isOfficer()) {
                // Take the maximum of the commander's Primary and Secondary Role
                // experience to calculate their experience level...
                int commanderExperience = Math.max(commander.getExperienceLevel(false),
                        commander.getExperienceLevel(true));
                if (commanderExperience > SkillType.EXP_REGULAR) {
                    // ...and if the commander is better than a veteran, find all of
                    // the personnel under their command...
                    for (UUID traineeId : forceIds.get(l.getForceId()).getAllUnits(true)) {
                        Unit traineeUnit = getHangar().getUnit(traineeId);

                        if (traineeUnit == null) {
                            continue;
                        }

                        for (Person p : traineeUnit.getCrew()) {
                            if (p.equals(commander)) {
                                continue;
                            }
                            // ...and if their weakest role is Green or Ultra-Green
                            int experienceLevel = Math.min(p.getExperienceLevel(false),
                                    p.getSecondaryRole() != Person.T_NONE
                                            ? p.getExperienceLevel(true)
                                            : SkillType.EXP_ELITE);
                            if (experienceLevel >= 0 && experienceLevel < SkillType.EXP_REGULAR) {
                                // ...add one XP.
                                p.awardXP(1);
                                addReport(p.getHyperlinkedName() + " has gained 1 XP from training.");
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public void removeAllPatientsFor(Person doctor) {
        for (Person p : getPersonnel()) {
            if (null != p.getDoctorId()
                    && p.getDoctorId().equals(doctor.getId())) {
                p.setDoctorId(null, getCampaignOptions()
                        .getNaturalHealingWaitingPeriod());
            }
        }
    }

    public void removeAllTechJobsFor(Person tech) {
        if (tech == null || tech.getId() == null) {
            return;
        }
        getHangar().forEachUnit(u -> {
            if (tech.equals(u.getTech())) {
                u.removeTech();
            }
            if ((u.getRefit() != null) && tech.equals(u.getRefit().getTech())) {
                u.getRefit().setTech(null);
            }
        });
        for (Part p : getParts()) {
            if (tech.equals(p.getTech())) {
                p.setTech(null);
            }
        }
        for (Force f : forceIds.values()) {
            if (tech.getId().equals(f.getTechID())) {
                f.setTechID(null);
            }
        }
    }

    public void removeScenario(int id) {
        Scenario scenario = getScenario(id);
        scenario.clearAllForcesAndPersonnel(this);
        Mission mission = getMission(scenario.getMissionId());
        if (null != mission) {
            mission.removeScenario(scenario.getId());
        }
        scenarios.remove(id);
        MekHQ.triggerEvent(new ScenarioRemovedEvent(scenario));
    }

    public void removeMission(int id) {
        Mission mission = getMission(id);

        // Loop through scenarios here! We need to remove them as well.
        if (null != mission) {
            for (Scenario scenario : mission.getScenarios()) {
                scenario.clearAllForcesAndPersonnel(this);
                scenarios.remove(scenario.getId());
            }
            mission.clearScenarios();
        }

        missions.remove(id);
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
                u.setForceId(-1);
                if (force.isDeployed()) {
                    u.setScenarioId(-1);
                }
            }
        }
        MekHQ.triggerEvent(new OrganizationChangedEvent(force));
        // also remove this force's id from any scenarios
        if (force.isDeployed()) {
            Scenario s = getScenario(force.getScenarioId());
            s.removeForce(fid);
        }

        if (campaignOptions.getUseAtB()) {
            lances.remove(fid);
        }

        if (null != force.getParentForce()) {
            force.getParentForce().removeSubForce(fid);
        }
        ArrayList<Force> subs = new ArrayList<>(force.getSubForces());
        for (Force sub : subs) {
            removeForce(sub);
            MekHQ.triggerEvent(new OrganizationChangedEvent(sub));
        }
    }

    public void removeUnitFromForce(Unit u) {
        Force force = getForce(u.getForceId());
        if (null != force) {
            force.removeUnit(u.getId());
            u.setForceId(Force.FORCE_NONE);
            u.setScenarioId(-1);
            if (u.getEntity().hasNavalC3()
                    && u.getEntity().calculateFreeC3Nodes() < 5) {
                Vector<Unit> removedUnits = new Vector<>();
                removedUnits.add(u);
                removeUnitsFromNetwork(removedUnits);
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                refreshNetworks();
            } else if (u.getEntity().hasC3i()
                    && u.getEntity().calculateFreeC3Nodes() < 5) {
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


            if (campaignOptions.getUseAtB() && force.getUnits().size() == 0) {
                lances.remove(force.getId());
            }
        }
    }

    public Force getForceFor(Unit u) {
        return getForce(u.getForceId());
    }

    public Force getForceFor(Person p) {
        Unit u = p.getUnit();
        if (u != null) {
            return getForceFor(u);
        } else if (p.isTech()) {
            for (Force force : forceIds.values()) {
                if (p.getId().equals(force.getTechID())) {
                    return force;
                }
            }
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

                // Aerospace parts have changed after 0.45.4. Reinitialize parts for Small Craft and up
                if (unit.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                        || unit.getEntity().hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
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

        if (getCampaignOptions().getUseAtB()) {
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
        for (Person p : personnel.values()) {
            if (p.getGenealogy().hasSpouse()) {
                if (!personnel.containsKey(p.getGenealogy().getSpouseId())) {
                    p.getGenealogy().setSpouse(null);
                    if (!getCampaignOptions().getKeepMarriedNameUponSpouseDeath()
                            && (p.getMaidenName() != null)) {
                        p.setSurname(p.getMaidenName());
                    }
                    p.setMaidenName(null);
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
                force.removeUnit(unitID);
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
        return Faction.getFaction(factionCode);
    }

    public String getFactionName() {
        return getFaction().getFullName(getGameYear());
    }

    public void setFactionCode(String i) {
        this.factionCode = i;
        updateTechFactionCode();
    }

    public String getFactionCode() {
        return factionCode;
    }

    public String getRetainerEmployerCode() {
        return retainerEmployerCode;
    }

    public void setRetainerEmployerCode(String code) {
        retainerEmployerCode = code;
    }

    private void addInMemoryLogHistory(LogEntry le) {
        if (inMemoryLogHistory.size() != 0) {
            while (ChronoUnit.DAYS.between(inMemoryLogHistory.get(0).getDate(), le.getDate()) > MekHqConstants.MAX_HISTORICAL_LOG_DAYS) {
                //we've hit the max size for the in-memory based on the UI display limit prune the oldest entry
                inMemoryLogHistory.remove(0);
            }
        }
        inMemoryLogHistory.add(le);
    }

    /**
     * Starts a new day for the daily log
     * @param r - the report String
     */
    public void beginReport(String r) {
        if (MekHQ.getMekHQOptions().getHistoricalDailyLog()) {
            //add the new items to our in-memory cache
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), ""));
        }
        addReportInternal(r);
    }

    /**
     * Adds a report to the daily log
     * @param r - the report String
     */
    public void addReport(String r) {
        if (MekHQ.getMekHQOptions().getHistoricalDailyLog()) {
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), r));
        }
        addReportInternal(r);
    }

    private void addReportInternal(String r) {
        currentReport.add(r);
        if ( currentReportHTML.length() > 0 ) {
            currentReportHTML = currentReportHTML + REPORT_LINEBREAK + r;
            newReports.add(REPORT_LINEBREAK);
        } else {
            currentReportHTML = r;
        }
        newReports.add(r);
        MekHQ.triggerEvent(new ReportEvent(this, r));
    }

    public void addReports(ArrayList<String> reports) {
        for (String r : reports) {
            addReport(r);
        }
    }

    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        camoFileName = name;
    }

    public String getCamoFileName() {
        return camoFileName;
    }

    public AbstractIcon getCamouflage() {
        return new Camouflage(getCamoCategory(), getCamoFileName());
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        colorIndex = index;
    }

    public String getIconCategory() {
        return iconCategory;
    }

    public void setIconCategory(String s) {
        this.iconCategory = s;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    public void setIconFileName(String s) {
        this.iconFileName = s;
    }

    public void addFunds(Money quantity) {
        addFunds(quantity, "Rich Uncle", Transaction.C_MISC);
    }

    public void addFunds(Money quantity, String description, int category) {
        if (description == null || description.isEmpty()) {
            description = "Rich Uncle";
        }
        if (category == -1) {
            category = Transaction.C_MISC;
        }
        finances.credit(quantity, category, description, getLocalDate());
        String quantityString = quantity.toAmountAndSymbolString();
        addReport("Funds added : " + quantityString + " (" + description + ")");
    }

    public boolean hasEnoughFunds(Money cost) {
        return getFunds().isGreaterOrEqualThan(cost);
    }


    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public void setCampaignOptions(CampaignOptions options) {
        campaignOptions = options;
    }

    public void writeToXml(PrintWriter pw1) {
        int indent = 1;

        // File header
        pw1.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");
        // Start the XML root.
        pw1.println("<campaign version=\"" + resourceMap.getString("Application.version") + "\">");

        //region Basic Campaign Info
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "info");

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "id", id.toString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "name", name);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "faction", factionCode);
        if (retainerEmployerCode != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "retainerEmployerCode", retainerEmployerCode);
        }

        // Ranks
        ranks.writeToXml(pw1, indent + 1);

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nameGen",
                RandomNameGenerator.getInstance().getChosenFaction());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "percentFemale",
                RandomGenderGenerator.getPercentFemale());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "overtime", overtime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "gmMode", gmMode);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "astechPool", astechPool);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "astechPoolMinutes",
                astechPoolMinutes);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "astechPoolOvertime",
                astechPoolOvertime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "medicPool", medicPool);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "camoCategory", camoCategory);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "camoFileName", camoFileName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "iconCategory", iconCategory);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "iconFileName", iconFileName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "colorIndex", colorIndex);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "lastForceId", lastForceId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "lastMissionId", lastMissionId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "lastScenarioId", lastScenarioId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "calendar",
                MegaMekXmlUtil.saveFormattedDate(getLocalDate()));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "fatigueLevel", fatigueLevel);

        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "nameGen");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 2, "faction", RandomNameGenerator.getInstance().getChosenFaction());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 2, "percentFemale", RandomGenderGenerator.getPercentFemale());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "nameGen");

        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "currentReport");
        for (String s : currentReport) {
            // This cannot use the MekHQXMLUtil as it cannot be escaped
            pw1.println(MekHqXmlUtil.indentStr(indent + 2) + "<reportLine><![CDATA[" + s + "]]></reportLine>");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "currentReport");

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "info");
        //endregion Basic Campaign Info

        //region Campaign Options
        if (getCampaignOptions() != null) {
            getCampaignOptions().writeToXml(pw1, indent);
        }
        //endregion Campaign Options

        // Lists of objects:
        units.writeToXml(pw1, indent, "units"); // Units
        writeMapToXml(pw1, indent, "personnel", personnel); // Personnel
        writeMapToXml(pw1, indent, "missions", missions); // Missions
        // the forces structure is hierarchical, but that should be handled
        // internally from with writeToXML function for Force
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "forces");
        forces.writeToXml(pw1, indent + 1);
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "forces");
        finances.writeToXml(pw1, indent);
        location.writeToXml(pw1, indent);
        shoppingList.writeToXml(pw1, indent);

        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "kills");
        for (List<Kill> kills : kills.values()) {
            for (Kill k : kills) {
                k.writeToXml(pw1, indent + 1);
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "kills");
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "skillTypes");
        for (String name : SkillType.skillList) {
            SkillType type = SkillType.getType(name);
            if (null != type) {
                type.writeToXml(pw1, indent + 1);
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "skillTypes");
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "specialAbilities");
        for (String key : SpecialAbility.getAllSpecialAbilities().keySet()) {
            SpecialAbility.getAbility(key).writeToXml(pw1, indent + 1);
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "specialAbilities");
        rskillPrefs.writeToXml(pw1, indent);
        // parts is the biggest so it goes last
        parts.writeToXml(pw1, indent, "parts"); // Parts

        writeGameOptions(pw1);

        // Personnel Market
        personnelMarket.writeToXml(pw1, indent);

        // Against the Bot
        if (getCampaignOptions().getUseAtB()) {
            contractMarket.writeToXml(pw1, indent);
            unitMarket.writeToXml(pw1, indent);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "colorIndex", colorIndex);
            if (lances.size() > 0)   {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "lances");
                for (Lance l : lances.values()) {
                    if (forceIds.containsKey(l.getForceId())) {
                        l.writeToXml(pw1, indent + 1);
                    }
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "lances");
            }
            retirementDefectionTracker.writeToXml(pw1, indent);
            if (shipSearchStart != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "shipSearchStart",
                        MekHqXmlUtil.saveFormattedDate(getShipSearchStart()));
            }
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "shipSearchType", shipSearchType);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "shipSearchResult", shipSearchResult);
            if (shipSearchExpiration != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "shipSearchExpiration",
                        MekHqXmlUtil.saveFormattedDate(getShipSearchExpiration()));
            }
        }

        // Customised planetary events
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "customPlanetaryEvents");
        for (PlanetarySystem psystem : Systems.getInstance().getSystems().values()) {
            //first check for system-wide events
            List<PlanetarySystem.PlanetarySystemEvent> customSysEvents = new ArrayList<>();
            for (PlanetarySystem.PlanetarySystemEvent event : psystem.getEvents()) {
                if (event.custom) {
                    customSysEvents.add(event);
                }
            }
            boolean startedSystem = false;
            if (!customSysEvents.isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "system");
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 2, "id", psystem.getId());
                for (PlanetarySystem.PlanetarySystemEvent event : customSysEvents) {
                    Systems.getInstance().writePlanetarySystemEvent(pw1, event);
                    pw1.println();
                }
                startedSystem = true;
            }
            //now check for planetary events
            for (Planet p : psystem.getPlanets()) {
                List<Planet.PlanetaryEvent> customEvents = p.getCustomEvents();
                if (!customEvents.isEmpty()) {
                    if (!startedSystem) {
                        //only write this if we haven't already started the system
                        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "system");
                        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 2, "id", psystem.getId());
                    }
                    MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 2, "planet");
                    MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "sysPos", p.getSystemPosition());
                    for (Planet.PlanetaryEvent event : customEvents) {
                        Systems.getInstance().writePlanetaryEvent(pw1, event);
                        pw1.println();
                    }
                    MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 2, "planet");
                    startedSystem = true;
                }
            }
            if (startedSystem) {
                //close the system
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "system");
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "customPlanetaryEvents");

        if (MekHQ.getMekHQOptions().getWriteCustomsToXML()) {
            writeCustoms(pw1);
        }

        // Okay, we're done.
        // Close everything out and be done with it.
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent - 1, "campaign");
    }

    public void writeGameOptions(PrintWriter pw1) {
        pw1.println("\t<gameOptions>");
        for (IBasicOption option : getGameOptionsVector()) {
            pw1.println("\t\t<gameoption>"); //$NON-NLS-1$
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "name", option.getName());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "value", option.getValue()
                    .toString());
            pw1.println("\t\t</gameoption>"); //$NON-NLS-1$
        }
        pw1.println("\t</gameOptions>");
    }

    /**
     * A helper function to encapsulate writing the map entries out to XML.
     *
     * @param <keyType> The key type of the map.
     * @param <valueType> The object type of the map. Must implement MekHqXmlSerializable.
     * @param pw1       The PrintWriter to output XML to.
     * @param indent    The indentation level to use for writing XML (purely for neatness).
     * @param tag       The name of the tag to use to encapsulate it.
     * @param map       The map of objects to write out.
     */
    private <keyType, valueType extends MekHqXmlSerializable> void writeMapToXml(PrintWriter pw1,
            int indent, String tag, Map<keyType, valueType> map) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

        for (Map.Entry<keyType, valueType> x : map.entrySet()) {
            x.getValue().writeToXml(pw1, indent + 1);
        }

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
    }

    private void writeCustoms(PrintWriter pw1) {
        for (String name : customs) {
            MechSummary ms = MechSummaryCache.getInstance().getMech(name);
            if (ms == null) {
                continue;
            }

            MechFileParser mechFileParser = null;
            try {
                mechFileParser = new MechFileParser(ms.getSourceFile());
            } catch (EntityLoadingException ex) {
                MekHQ.getLogger().error(Campaign.class, "writeCustoms(PrintWriter)", ex);
            }
            if (mechFileParser == null) {
                continue;
            }
            Entity en = mechFileParser.getEntity();
            pw1.println("\t<custom>");
            pw1.println("\t\t<name>" + name + "</name>");
            if (en instanceof Mech) {
                pw1.print("\t\t<mtf><![CDATA[");
                pw1.print(((Mech) en).getMtf());
                pw1.println("]]></mtf>");
            } else {
                pw1.print("\t\t<blk><![CDATA[");

                BuildingBlock blk = BLKFile.getBlock(en);
                for (String s : blk.getAllDataAsString()) {
                    if (s.isEmpty()) {
                        continue;
                    }
                    pw1.println(s);
                }
                pw1.println("]]></blk>");
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

    public void setRanks(Ranks r) {
        ranks = r;
    }

    public Ranks getRanks() {
        return ranks;
    }

    public void setRankSystem(int system) {
        getRanks().setRankSystem(system);
    }

    public List<String> getAllRankNamesFor(int p) {
        List<String> retVal = new ArrayList<>();
        for (Rank rank : getRanks().getAllRanks()) {
            // Grab rank from correct profession as needed
            while (rank.getName(p).startsWith("--") && p != Ranks.RPROF_MW) {
                if (rank.getName(p).equals("--")) {
                    p = getRanks().getAlternateProfession(p);
                } else if (rank.getName(p).startsWith("--")) {
                    p = getRanks().getAlternateProfession(rank.getName(p));
                }
            }
            if (rank.getName(p).equals("-")) {
                continue;
            }

            retVal.add(rank.getName(p));
        }
        return retVal;
    }

    public ArrayList<Force> getAllForces() {
        return new ArrayList<>(forceIds.values());
    }

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
     * Use an A* algorithm to find the best path between two planets For right now, we are just going to minimize the number
     * of jumps but we could extend this to take advantage of recharge information or other variables as well Based on
     * http://www.policyalmanac.org/games/aStarTutorial.htm
     *
     * @param start
     * @param end
     * @return
     */
    public JumpPath calculateJumpPath(PlanetarySystem start, PlanetarySystem end) {
        if (null == start) {
            return null;
        }
        if ((null == end) || start.getId().equals(end.getId())) {
            JumpPath jpath = new JumpPath();
            jpath.addSystem(start);
            return jpath;
        }

        String startKey = start.getId();
        String endKey = end.getId();

        String current = startKey;
        Set<String> closed = new HashSet<>();
        Set<String> open = new HashSet<>();
        boolean found = false;
        int jumps = 0;

        // we are going to through and set up some hashes that will make our
        // work easier
        // hash of parent key
        Map<String, String> parent = new HashMap<>();
        // hash of H for each planet which will not change
        Map<String, Double> scoreH = new HashMap<>();
        // hash of G for each planet which might change
        Map<String, Double> scoreG = new HashMap<>();

        for (String key : Systems.getInstance().getSystems().keySet()) {
            scoreH.put(key, end.getDistanceTo(Systems.getInstance().getSystems().get(key)));
        }
        scoreG.put(current, 0.0);
        closed.add(current);

        while (!found && jumps < 10000) {
            jumps++;
            double currentG = scoreG.get(current) + Systems.getInstance().getSystemById(current).getRechargeTime(getLocalDate());

            final String localCurrent = current;
            Systems.getInstance().visitNearbySystems(Systems.getInstance().getSystemById(current), 30, p -> {
                if (closed.contains(p.getId())) {
                    return;
                } else if (open.contains(p.getId())) {
                    // is the current G better than the existing G
                    if (currentG < scoreG.get(p.getId())) {
                        // then change G and parent
                        scoreG.put(p.getId(), currentG);
                        parent.put(p.getId(), localCurrent);
                    }
                } else {
                    // put the current G for this one in memory
                    scoreG.put(p.getId(), currentG);
                    // put the parent in memory
                    parent.put(p.getId(), localCurrent);
                    open.add(p.getId());
                }
            });

            String bestMatch = null;
            double bestF = Double.POSITIVE_INFINITY;
            for (String possible : open) {
                // calculate F
                double currentF = scoreG.get(possible) + scoreH.get(possible);
                if (currentF < bestF) {
                    bestMatch = possible;
                    bestF = currentF;
                }
            }

            current = bestMatch;
            if (null == current) {
                // We're done - probably failed to find anything
                break;
            }

            closed.add(current);
            open.remove(current);
            if (current.equals(endKey)) {
                found = true;
            }
        }

        // now we just need to back up from the last current by parents until we
        // hit null
        List<PlanetarySystem> path = new ArrayList<>();
        String nextKey = current;
        while (null != nextKey) {
            path.add(Systems.getInstance().getSystemById(nextKey));
            // MekHQApp.logMessage(nextKey);
            nextKey = parent.get(nextKey);
        }

        // now reverse the direction
        JumpPath finalPath = new JumpPath();
        for (int i = (path.size() - 1); i >= 0; i--) {
            finalPath.addSystem(path.get(i));
        }

        return finalPath;
    }

    public List<PlanetarySystem> getAllReachableSystemsFrom(PlanetarySystem system) {
        return Systems.getInstance().getNearbySystems(system, 30);
    }

    /**
     * This method calculates the cost per jump for interstellar travel. It operates by fitting the part
     * of the force not transported in owned DropShips into a number of prototypical DropShips of a few
     * standard configurations, then adding the JumpShip charges on top. It remains fairly hacky, but
     * improves slightly on the prior implementation as far as following the rulebooks goes.
     *
     * It can be used to calculate total travel costs in the style of FM:Mercs (excludeOwnTransports
     * and campaignOpsCosts set to false), to calculate leased/rented travel costs only in the style
     * of FM:Mercs (excludeOwnTransports true, campaignOpsCosts false), or to calculate travel costs
     * for CampaignOps-style costs (excludeOwnTransports true, campaignOpsCosts true).
     *
     *  @param excludeOwnTransports If true, do not display maintenance costs in the calculated travel cost.
     * @param campaignOpsCosts If true, use the Campaign Ops method for calculating travel cost. (DropShip monthly fees
     *                         of 0.5% of purchase cost, 100,000 C-bills per collar.)
     */
    @SuppressWarnings("unused") // FIXME: Waiting for Dylan to finish re-writing
    public Money calculateCostPerJump(boolean excludeOwnTransports, boolean campaignOpsCosts) {
        HangarStatistics stats = getHangarStatistics();
        CargoStatistics cargoStats = getCargoStatistics();

        Money collarCost = Money.of(campaignOpsCosts ? 100000 : 50000);

        // first we need to get the total number of units by type
        int nMech = stats.getNumberOfUnitsByType(Entity.ETYPE_MECH);
        int nLVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int nHVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int nAero = stats.getNumberOfUnitsByType(Entity.ETYPE_AERO);
        int nSC = stats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int nCF = stats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int nBA = stats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int nMechInf = 0;
        int nMotorInf = 0;
        int nFootInf = 0;
        int nProto = stats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);
        int nDropship = stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int nCollars = stats.getTotalDockingCollars();
        double nCargo = cargoStats.getTotalCargoCapacity(); // ignoring refrigerated/insulated/etc.

        // get cargo tonnage including parts in transit, then get mothballed unit
        // tonnage
        double carriedCargo = cargoStats.getCargoTonnage(true, false) + cargoStats.getCargoTonnage(false, true);

        // calculate the number of units left untransported
        int noMech = Math.max(nMech - stats.getOccupiedBays(Entity.ETYPE_MECH), 0);
        int noDS = Math.max(nDropship - stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(nSC - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCF = Math.max(nCF - stats.getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(nAero - stats.getOccupiedBays(Entity.ETYPE_AERO), 0);
        int nolv = Math.max(nLVee - stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(nHVee - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noinf = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(nBA - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(nProto - stats.getOccupiedBays(Entity.ETYPE_PROTOMECH), 0);
        int freehv = Math.max(stats.getTotalHeavyVehicleBays() - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeinf = Math.max(stats.getTotalInfantryBays() - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int freeba = Math.max(stats.getTotalBattleArmorBays() - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int freeSC = Math.max(stats.getTotalSmallCraftBays() - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noCargo = (int) Math.ceil(Math.max(carriedCargo - nCargo, 0));

        int newNoASF = Math.max(noASF - freeSC, 0);
        int placedASF = Math.max(noASF - newNoASF, 0);
        freeSC -= placedASF;

        int newNolv = Math.max(nolv - freehv, 0);
        int placedlv = Math.max(nolv - newNolv, 0);
        freehv -= placedlv;
        int noVehicles = (nohv + newNolv);

        Money dropshipCost;
        // The cost-figuring process: using prototypical dropships, figure out how
        // many collars are required. Charge for the prototypical dropships and
        // the docking collar, based on the rules selected. Allow prototypical
        // dropships to be leased in 1/2 increments; designs of roughly 1/2
        // size exist for all of the prototypical variants chosen.

        // DropShip costs are for the duration of the trip for FM:Mercs rules,
        // and per month for Campaign Ops. The prior implementation here assumed
        // the FM:Mercs costs were per jump, which seems reasonable. To avoid having
        // to add a bunch of code to remember the total length of the current
        // jump path, CamOps costs are normalized to per-jump, using 175 hours charge
        // time as a baseline.

        // Roughly an Overlord
        int largeDropshipMechCapacity = 36;
        int largeMechDropshipASFCapacity = 6;
        int largeMechDropshipCargoCapacity = 120;
        Money largeMechDropshipCost = Money.of(campaignOpsCosts ? (1750000.0 / 4.2) : 400000);

        // Roughly a Union
        int averageDropshipMechCapacity = 12;
        int mechDropshipASFCapacity = 2;
        int mechDropshipCargoCapacity = 75;
        Money mechDropshipCost = Money.of(campaignOpsCosts ? (1450000.0 / 4.2) : 150000);

        // Roughly a Leopard CV
        int averageDropshipASFCapacity = 6;
        int asfDropshipCargoCapacity = 90;
        Money asfDropshipCost = Money.of(campaignOpsCosts ? (900000.0 / 4.2) : 80000);

        // Roughly a Triumph
        int largeDropshipVehicleCapacity = 50;
        int largeVehicleDropshipCargoCapacity = 750;
        Money largeVehicleDropshipCost = Money.of(campaignOpsCosts ? (1750000.0 / 4.2) : 430000);

        // Roughly a Gazelle
        int averageDropshipVehicleCapacity = 15;
        int vehicleDropshipCargoCapacity = 65;
        Money vehicleDropshipCost = Money.of(campaignOpsCosts ? (900000.0 / 4.2): 40000);

        // Roughly a Mule
        int largeDropshipCargoCapacity = 8000;
        Money largeCargoDropshipCost = Money.of(campaignOpsCosts ? (750000.0 / 4.2) : 800000);

        // Roughly a Buccaneer
        int averageDropshipCargoCapacity = 2300;
        Money cargoDropshipCost = Money.of(campaignOpsCosts ? (550000.0 / 4.2) : 250000);

        int mechCollars = 0;
        double leasedLargeMechDropships = 0;
        double leasedAverageMechDropships = 0;

        int asfCollars = 0;
        double leasedAverageASFDropships = 0;

        int vehicleCollars = 0;
        double leasedLargeVehicleDropships = 0;
        double leasedAverageVehicleDropships = 0;

        int cargoCollars = 0;
        double leasedLargeCargoDropships = 0;
        double leasedAverageCargoDropships = 0;

        int leasedASFCapacity = 0;
        int leasedCargoCapacity = 0;

        // For each type we're concerned with, calculate the number of dropships needed
        // to transport the force. Smaller dropships are represented by half-dropships.

        // If we're transporting more than a company, Overlord analogues are more efficient.
        if (noMech > 12) {
            leasedLargeMechDropships = noMech / (double) largeDropshipMechCapacity;
            noMech -= leasedLargeMechDropships * largeDropshipMechCapacity;
            mechCollars += (int) Math.ceil(leasedLargeMechDropships);

            // If there's more than a company left over, lease another Overlord. Otherwise
            // fall through and get a Union.
            if (noMech > 12) {
                leasedLargeMechDropships += 1;
                noMech -= largeDropshipMechCapacity;
                mechCollars += 1;
            }

            leasedASFCapacity += (int) Math.floor(leasedLargeMechDropships * largeMechDropshipASFCapacity);
            leasedCargoCapacity += (int) Math.floor(largeMechDropshipCargoCapacity);
        }

        // Unions
        if (noMech > 0) {
            leasedAverageMechDropships = noMech / (double) averageDropshipMechCapacity;
            noMech -= leasedAverageMechDropships * averageDropshipMechCapacity;
            mechCollars += (int) Math.ceil(leasedAverageMechDropships);

            // If we can fit in a smaller DropShip, lease one of those instead.
            if ((noMech > 0) && (noMech < (averageDropshipMechCapacity / 2))) {
                leasedAverageMechDropships += 0.5;
                mechCollars += 1;
            } else if (noMech > 0) {
                leasedAverageMechDropships += 1;
                mechCollars += 1;
            }

            // Our Union-ish DropShip can carry some ASFs and cargo.
            leasedASFCapacity += (int) Math.floor(leasedAverageMechDropships * mechDropshipASFCapacity);
            leasedCargoCapacity += (int) Math.floor(leasedAverageMechDropships * mechDropshipCargoCapacity);
        }

        // Leopard CVs
        if (noASF > leasedASFCapacity) {
            noASF -= leasedASFCapacity;

            if (noASF > 0) {
                leasedAverageASFDropships = noASF / (double) averageDropshipASFCapacity;
                noASF -= leasedAverageASFDropships * averageDropshipASFCapacity;
                asfCollars += (int) Math.ceil(leasedAverageASFDropships);

                if ((noASF > 0) && (noASF < (averageDropshipASFCapacity / 2))) {
                    leasedAverageASFDropships += 0.5;
                    asfCollars += 1;
                } else if (noASF > 0) {
                    leasedAverageASFDropships += 1;
                    asfCollars += 1;
                }
            }

            // Our Leopard-ish DropShip can carry some cargo.
            leasedCargoCapacity += (int) Math.floor(asfDropshipCargoCapacity * leasedAverageASFDropships);
        }

        // Triumphs
        if (noVehicles > averageDropshipVehicleCapacity) {
            leasedLargeVehicleDropships = noVehicles / (double) largeDropshipVehicleCapacity;
            noVehicles -= leasedLargeVehicleDropships * largeDropshipVehicleCapacity;
            vehicleCollars += (int) Math.ceil(leasedLargeVehicleDropships);

            if (noVehicles > averageDropshipVehicleCapacity) {
                leasedLargeVehicleDropships += 1;
                noVehicles -= largeDropshipVehicleCapacity;
                vehicleCollars += 1;
            }

            leasedCargoCapacity += (int) Math.floor(leasedLargeVehicleDropships * largeVehicleDropshipCargoCapacity);
        }

        // Gazelles
        if (noVehicles > 0) {
            leasedAverageVehicleDropships = (nohv + newNolv) / (double) averageDropshipVehicleCapacity;
            noVehicles = (int) ((nohv + newNolv) - leasedAverageVehicleDropships * averageDropshipVehicleCapacity);
            vehicleCollars += (int) Math.ceil(leasedAverageVehicleDropships);

            // Gazelles are pretty minimal, so no half-measures.
            if (noVehicles > 0) {
                leasedAverageVehicleDropships += 1;
                noVehicles -= averageDropshipVehicleCapacity;
                vehicleCollars += 1;
            }

            // Our Gazelle-ish DropShip can carry some cargo.
            leasedCargoCapacity += (int) Math.floor(vehicleDropshipCargoCapacity * leasedAverageVehicleDropships);
        }

        // Do we have any leftover cargo?
        noCargo -= leasedCargoCapacity;

        // Mules
        if (noCargo > averageDropshipCargoCapacity) {
            leasedLargeCargoDropships = noCargo / (double) largeDropshipCargoCapacity;
            noCargo -= leasedLargeCargoDropships * largeDropshipCargoCapacity;
            cargoCollars += (int) Math.ceil(leasedLargeCargoDropships);

            if (noCargo > averageDropshipCargoCapacity) {
                leasedLargeCargoDropships += 1;
                noCargo -= largeDropshipCargoCapacity;
                cargoCollars += 1;
            }
        }

        // Buccaneers
        if (noCargo > 0) {
            leasedAverageCargoDropships = noCargo / (double) averageDropshipCargoCapacity;
            cargoCollars += (int) Math.ceil(leasedAverageCargoDropships);
            noCargo -= leasedAverageCargoDropships * averageDropshipCargoCapacity;

            if (noCargo > 0 && noCargo < (averageDropshipCargoCapacity / 2)) {
                leasedAverageCargoDropships += 0.5;
                cargoCollars += 1;
            } else if (noCargo > 0) {
                leasedAverageCargoDropships += 1;
                cargoCollars += 1;
            }
        }

        dropshipCost = mechDropshipCost.multipliedBy(leasedAverageMechDropships);
        dropshipCost = dropshipCost.plus(largeMechDropshipCost.multipliedBy(leasedLargeMechDropships ));

        dropshipCost = dropshipCost.plus(asfDropshipCost.multipliedBy(leasedAverageASFDropships));

        dropshipCost = dropshipCost.plus(vehicleDropshipCost.multipliedBy(leasedAverageVehicleDropships));
        dropshipCost = dropshipCost.plus(largeVehicleDropshipCost.multipliedBy(leasedLargeVehicleDropships));

        dropshipCost = dropshipCost.plus(cargoDropshipCost.multipliedBy(leasedAverageCargoDropships));
        dropshipCost = dropshipCost.plus(largeCargoDropshipCost.multipliedBy(leasedLargeCargoDropships));

        // Smaller/half-DropShips are cheaper to rent, but still take one collar each
        int collarsNeeded = mechCollars + asfCollars + vehicleCollars + cargoCollars;

        // add owned DropShips
        collarsNeeded += nDropship;

        // now factor in owned JumpShips
        collarsNeeded = Math.max(0, collarsNeeded - nCollars);

        Money totalCost = dropshipCost.plus(collarCost.multipliedBy(collarsNeeded));

        // FM:Mercs reimburses for owned transport (CamOps handles it in peacetime costs)
        if (!excludeOwnTransports) {
            Money ownDropshipCost = Money.zero();
            Money ownJumpshipCost = Money.zero();
            for (Unit u : getUnits()) {
                if (!u.isMothballed()) {
                    Entity e = u.getEntity();
                    if ((e.getEntityType() & Entity.ETYPE_DROPSHIP) != 0) {
                        ownDropshipCost = ownDropshipCost.plus(mechDropshipCost.multipliedBy(u.getMechCapacity()).dividedBy(averageDropshipMechCapacity));
                        ownDropshipCost = ownDropshipCost.plus(asfDropshipCost.multipliedBy(u.getASFCapacity()).dividedBy(averageDropshipASFCapacity));
                        ownDropshipCost = ownDropshipCost.plus(vehicleDropshipCost.multipliedBy(u.getHeavyVehicleCapacity() + u.getLightVehicleCapacity()).dividedBy(averageDropshipVehicleCapacity));
                        ownDropshipCost = ownDropshipCost.plus(cargoDropshipCost.multipliedBy(u.getCargoCapacity()).dividedBy(averageDropshipCargoCapacity));
                    } else if ((e.getEntityType() & Entity.ETYPE_JUMPSHIP) != 0) {
                        ownJumpshipCost = ownDropshipCost.plus(collarCost.multipliedBy(e.getDockingCollars().size()));
                    }
                }
            }

            totalCost = totalCost.plus(ownDropshipCost).plus(ownJumpshipCost);
        }

        return totalCost;
    }

    public void personUpdated(Person p) {
        Unit u = p.getUnit();
        if (null != u) {
            u.resetPilotAndEntity();
        }
        MekHQ.triggerEvent(new PersonChangedEvent(p));
    }

    public TargetRoll getTargetFor(IPartWork partWork, Person tech) {
        Skill skill = tech.getSkillForWorkingOn(partWork);
        int modePenalty = partWork.getMode().expReduction;
        if (null != partWork.getUnit() && !partWork.getUnit().isAvailable(partWork instanceof Refit)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "This unit is not currently available!");
        }
        if ((partWork.getTech() != null) && !partWork.getTech().equals(tech)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Already being worked on by another team");
        }
        if (null == skill) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Assigned tech does not have the right skills");
        }
        if (!getCampaignOptions().isDestroyByMargin()
                && partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "Task is beyond this tech's skill level");
        }
        if (partWork.getSkillMin() > SkillType.EXP_ELITE) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
        }
        if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
        }
        if (partWork instanceof MissingPart
                && null == ((MissingPart) partWork).findReplacement(false)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Part not available.");
        }
        if (!(partWork instanceof Refit) && tech.getMinutesLeft() <= 0
                && (!isOvertimeAllowed() || tech.getOvertimeLeft() <= 0)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "No time left.");
        }
        String notFixable = partWork.checkFixable();
        if (null != notFixable) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, notFixable);
        }
        // if this is an infantry refit, then automatic success
        if (partWork instanceof Refit && null != partWork.getUnit()
                && partWork.getUnit().getEntity() instanceof Infantry
                && !(partWork.getUnit().getEntity() instanceof BattleArmor)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
                    "infantry refit");
        }

        //if we are using the MoF rule, then we will ignore mode penalty here
        //and instead assign it as a straight penalty
        if (getCampaignOptions().isDestroyByMargin()) {
            modePenalty = 0;
        }

        // this is ugly, if the mode penalty drops you to green, you drop two
        // levels instead of two
        int value = skill.getFinalSkillValue() + modePenalty;
        if (modePenalty > 0
                && SkillType.EXP_GREEN == (skill.getExperienceLevel() - modePenalty)) {
            value++;
        }
        TargetRoll target = new TargetRoll(value,
                SkillType.getExperienceLevelName(skill.getExperienceLevel() - modePenalty));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllMods(tech));

        if (getCampaignOptions().useEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        boolean isOvertime = false;
        if (isOvertimeAllowed()
                && (tech.isTaskOvertime(partWork) || partWork.hasWorkedOvertime())) {
            target.addModifier(3, "overtime");
            isOvertime = true;
        }

        int minutes = Math.min(partWork.getTimeLeft(), tech.getMinutesLeft());
        if (isOvertimeAllowed()) {
            minutes = Math.min(minutes, tech.getMinutesLeft() + tech.getOvertimeLeft());
        }
        int helpMod;
        if (null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
            int hits;
            if (null != partWork.getUnit().getEntity().getCrew()) {
                hits = partWork.getUnit().getEntity().getCrew().getHits();
            } else {
                hits = 6;
            }
            helpMod = getShorthandedModForCrews(hits);
        } else {
            int helpers = getAvailableAstechs(minutes, isOvertime);
            helpMod = getShorthandedMod(helpers, false);
            // we may have just gone overtime with our helpers
            if (!isOvertime && astechPoolMinutes < (minutes * helpers)) {
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

    public TargetRoll getTargetForMaintenance(IPartWork partWork, Person tech) {
        int value = 10;
        String skillLevel = "Unmaintained";
        if (null != tech) {
            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (null != skill) {
                value = skill.getFinalSkillValue();
                skillLevel = SkillType.getExperienceLevelName(skill
                        .getExperienceLevel());
            }
        }

        TargetRoll target = new TargetRoll(value, skillLevel);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllModsForMaintenance());

        if (getCampaignOptions().useEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        if (null != partWork.getUnit() && null != tech) {
            // we have no official rules for what happens when a tech is only
            // assigned
            // for part of the maintenance cycle, so we will create our own
            // penalties
            if (partWork.getUnit().getMaintainedPct() < .5) {
                target.addModifier(2, "partial maintenance");
            } else if (partWork.getUnit().getMaintainedPct() < 1) {
                target.addModifier(1, "partial maintenance");
            }

            // the astech issue is crazy, because you can actually be better off
            // not maintaining
            // than going it short-handed, but that is just the way it is.
            // Still, there is also some fuzziness about what happens if you are
            // short astechs
            // for part of the cycle. We will keep keep track of the total
            // "astech days" used over
            // the cycle and take the average per day rounding down as our team
            // size
            int helpMod = 0;
            if (null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
                int hits = 0;
                if (null != partWork.getUnit().getEntity().getCrew()) {
                    hits = partWork.getUnit().getEntity().getCrew().getHits();
                } else {
                    hits = 6;
                }
                helpMod = getShorthandedModForCrews(hits);
            } else {
                int helpers = partWork.getUnit().getAstechsMaintained();
                helpMod = getShorthandedMod(helpers, false);
            }
            if (helpMod > 0) {
                target.addModifier(helpMod, "shorthanded");
            }
        }

        return target;
    }

    public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
            Person person) {
        return getTargetForAcquisition(acquisition, person, true);
    }

    public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
            Person person, boolean checkDaysToWait) {
        if (getCampaignOptions().getAcquisitionSkill().equals(
                CampaignOptions.S_AUTO)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
                    "Automatic Success");
        }
        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "No one on your force is capable of acquiring parts");
        }
        Skill skill = person.getSkillForWorkingOn(
                getCampaignOptions().getAcquisitionSkill());
        if (null != getShoppingList().getShoppingItem(
                acquisition.getNewEquipment())
                && checkDaysToWait) {
            return new TargetRoll(
                    TargetRoll.AUTOMATIC_FAIL,
                    "You must wait until the new cycle to check for this part. Further attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == Part.T_CLAN
                && !getCampaignOptions().allowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == Part.T_IS
                && !getCampaignOptions().allowISPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire inner sphere parts");
        }
        if (getCampaignOptions().getTechLevel() < Utilities
                .getSimpleTechLevel(acquisition.getTechLevel())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire parts of this tech level");
        }
        if (getCampaignOptions().limitByYear()
                && !acquisition.isIntroducedBy(getGameYear(), useClanTechBase(), getTechFaction())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It has not been invented yet!");
        }
        if (getCampaignOptions().disallowExtinctStuff() &&
                (acquisition.isExtinctIn(getGameYear(), useClanTechBase(), getTechFaction())
                        || acquisition.getAvailability() == EquipmentType.RATING_X)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It is extinct!");
        }
        if (getCampaignOptions().getUseAtB() &&
                getCampaignOptions().getRestrictPartsByMission() && acquisition instanceof Part) {
            int partAvailability = ((Part) acquisition).getAvailability();
            EquipmentType et = null;
            if (acquisition instanceof EquipmentPart) {
                et = ((EquipmentPart) acquisition).getType();
            } else if (acquisition instanceof MissingEquipmentPart) {
                et = ((MissingEquipmentPart) acquisition).getType();
            }

            StringBuilder partAvailabilityLog = new StringBuilder();
            partAvailabilityLog.append("Part Rating Level: " + partAvailability)
                                .append("(" + EquipmentType.ratingNames[partAvailability] + ")");

            /*
             * Even if we can acquire Clan parts, they have a minimum availability of F for
             * non-Clan units
             */
            if (acquisition.getTechBase() == Part.T_CLAN && !getFaction().isClan()) {
                partAvailability = Math.max(partAvailability, EquipmentType.RATING_F);
                partAvailabilityLog.append(";[clan part for non clan faction]");
            } else if (et != null) {
                /*
                 * AtB rules do not simply affect difficulty of obtaining parts, but whether
                 * they can be obtained at all. Changing the system to use availability codes
                 * can have a serious effect on game play, so we apply a few tweaks to keep some
                 * of the more basic items from becoming completely unobtainable, while applying
                 * a minimum for non-flamer energy weapons, which was the reason this rule was
                 * included in AtB to begin with.
                 */
                if (et instanceof megamek.common.weapons.lasers.EnergyWeapon
                        && !(et instanceof megamek.common.weapons.flamers.FlamerWeapon)
                        && partAvailability < EquipmentType.RATING_C) {
                    partAvailability = EquipmentType.RATING_C;
                    partAvailabilityLog.append(";(non-flamer lasers)");
                }
                if (et instanceof megamek.common.weapons.autocannons.ACWeapon) {
                    partAvailability -= 2;
                    partAvailabilityLog.append(";(autocannon): -2");
                }
                if (et instanceof megamek.common.weapons.gaussrifles.GaussWeapon
                        || et instanceof megamek.common.weapons.flamers.FlamerWeapon) {
                    partAvailability--;
                    partAvailabilityLog.append(";(gauss rifle or flamer): -1");
                }
                if (et instanceof megamek.common.AmmoType) {
                    switch (((megamek.common.AmmoType) et).getAmmoType()) {
                        case megamek.common.AmmoType.T_AC:
                            partAvailability -= 2;
                            partAvailabilityLog.append(";(autocannon ammo): -2");
                            break;
                        case megamek.common.AmmoType.T_GAUSS:
                            partAvailability -= 1;
                            partAvailabilityLog.append(";(gauss ammo): -1");
                            break;
                    }
                    if (((megamek.common.AmmoType) et).getMunitionType() == megamek.common.AmmoType.M_STANDARD) {
                        partAvailability--;
                        partAvailabilityLog.append(";(standard ammo): -1");
                    }
                }
            }

            if (((getGameYear() < 2950) || (getGameYear() > 3040))
                    && (acquisition instanceof Armor || acquisition instanceof MissingMekActuator
                            || acquisition instanceof mekhq.campaign.parts.MissingMekCockpit
                            || acquisition instanceof mekhq.campaign.parts.MissingMekLifeSupport
                            || acquisition instanceof mekhq.campaign.parts.MissingMekLocation
                            || acquisition instanceof mekhq.campaign.parts.MissingMekSensor)) {
                partAvailability--;
                partAvailabilityLog.append("(Mek part prior to 2950 or after 3040): - 1");
            }

            int AtBPartsAvailability = findAtBPartsAvailabilityLevel(acquisition, null);
            partAvailabilityLog.append("; Total part availability: " + partAvailability)
                            .append("; Current campaign availability: " + AtBPartsAvailability);
            if (partAvailability > AtBPartsAvailability) {
                return new TargetRoll(TargetRoll.IMPOSSIBLE, partAvailabilityLog.toString());
            }
        }
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
                SkillType.getExperienceLevelName(skill.getExperienceLevel()));// person.getTarget(Modes.MODE_NORMAL);
        target.append(acquisition.getAllAcquisitionMods());
        return target;
    }

    public AtBContract getAttachedAtBContract(Unit unit) {
        if (null != unit && null != lances.get(unit.getForceId())) {
            return lances.get(unit.getForceId()).getContract(this);
        }
        return null;
    }

    /**
     * AtB: count all available bonus parts
     * @return the total <code>int</code> number of bonus parts for all active contracts
     */
    public int totalBonusParts() {
        int retVal = 0;
        if (hasActiveContract()) {
            for (Contract c : getActiveContracts()) {
                if (c instanceof AtBContract) {
                    retVal += ((AtBContract) c).getNumBonusParts();
                }
            }
        }
        return retVal;
    }

    public void spendBonusPart(IAcquisitionWork targetWork) {
        // Can only spend from active contracts, so if there are none we can't spend a bonus part
        if (!hasActiveContract()) {
            return;
        }

        String report = targetWork.find(0);

        if (report.endsWith("0 days.")) {
            // First, try to spend from the contact the Acquisition's unit is attached to
            AtBContract contract = getAttachedAtBContract(targetWork.getUnit());

            if (contract == null) {
                // Then, just the first free one that is active
                for (Contract c : getActiveContracts()) {
                    if (((AtBContract) c).getNumBonusParts() > 0) {
                        contract = (AtBContract) c;
                        break;
                    }
                }
            }

            if (contract == null) {
                MekHQ.getLogger().error(this, "AtB: used bonus part but no contract has bonus parts available.");
            } else {
                addReport(resources.getString("bonusPartLog.text") + " " + targetWork.getAcquisitionPart().getPartName());
                contract.useBonusPart();
            }
        }
    }

    public int findAtBPartsAvailabilityLevel(IAcquisitionWork acquisition, StringBuilder reportBuilder) {
        AtBContract contract = (acquisition != null) ? getAttachedAtBContract(acquisition.getUnit()) : null;

        /*
         * If the unit is not assigned to a contract, use the least restrictive active
         * contract. Don't restrict parts availability by contract if it has not started.
         */
        if (hasActiveContract()) {
            for (Contract c : getActiveContracts()) {
                if ((c instanceof AtBContract) &&
                        ((contract == null) ||
                        (((AtBContract) c).getPartsAvailabilityLevel() > contract.getPartsAvailabilityLevel()))) {
                    contract = (AtBContract) c;
                }
            }
        }

        // if we have a contract and it has started
        if ((null != contract) && getLocalDate().isBefore(contract.getStartDate())) {
            if (reportBuilder != null) {
                reportBuilder.append(contract.getPartsAvailabilityLevel() + " (" + contract.getType() +")");
            }
            return contract.getPartsAvailabilityLevel();
        }

        /* If contract is still null, the unit is not in a contract. */
        Person adminLog = findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN);
        int adminLogExp = (adminLog == null) ? SkillType.EXP_ULTRA_GREEN
                : adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        int adminMod = adminLogExp - SkillType.EXP_REGULAR;

        if (reportBuilder != null) {
            reportBuilder.append(getUnitRatingMod() + "(unit rating)");
            if (adminLog != null) {
                reportBuilder.append(adminMod + "(" + adminLog.getFullName() +", logistics admin)");
            } else {
                reportBuilder.append(adminMod + "(no logistics admin)");
            }
        }

        return getUnitRatingMod() + adminMod;
    }

    public void resetAstechMinutes() {
        astechPoolMinutes = 480 * getNumberPrimaryAstechs() + 240
                * getNumberSecondaryAstechs();
        astechPoolOvertime = 240 * getNumberPrimaryAstechs() + 120
                * getNumberSecondaryAstechs();
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

    public void increaseAstechPool(int i) {
        astechPool += i;
        astechPoolMinutes += (480 * i);
        astechPoolOvertime += (240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, i));
    }

    public void decreaseAstechPool(int i) {
        astechPool = Math.max(0, astechPool - i);
        // always assume that we fire the ones who have not yet worked
        astechPoolMinutes = Math.max(0, astechPoolMinutes - 480 * i);
        astechPoolOvertime = Math.max(0, astechPoolOvertime - 240 * i);
        MekHQ.triggerEvent(new AstechPoolChangedEvent(this, -i));
    }

    public int getNumberAstechs() {
        return getNumberPrimaryAstechs() + getNumberSecondaryAstechs();
    }

    public int getNumberPrimaryAstechs() {
        int astechs = getAstechPool();
        for (Person p : getActivePersonnel()) {
            if ((p.getPrimaryRole() == Person.T_ASTECH) && !p.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getNumberSecondaryAstechs() {
        int astechs = 0;
        for (Person p : getActivePersonnel()) {
            if ((p.getSecondaryRole() == Person.T_ASTECH) && !p.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getAvailableAstechs(int minutes, boolean alreadyOvertime) {
        int availableHelp = (int) Math.floor(((double) astechPoolMinutes)
                / minutes);
        if (isOvertimeAllowed() && availableHelp < 6) {
            // if we are less than fully staffed, then determine whether
            // we should dip into overtime or just continue as short-staffed
            int shortMod = getShorthandedMod(availableHelp, false);
            int remainingMinutes = astechPoolMinutes - availableHelp * minutes;
            int extraHelp = (remainingMinutes + astechPoolOvertime) / minutes;
            int helpNeeded = 6 - availableHelp;
            if (alreadyOvertime && shortMod > 0) {
                // then add whatever we can
                availableHelp += extraHelp;
            } else if (shortMod > 3) {
                // only dip in if we can bring ourselves up to full
                if (extraHelp >= helpNeeded) {
                    availableHelp = 6;
                }
            }
        }
        if (availableHelp > 6) {
            availableHelp = 6;
        }
        return Math.min(availableHelp, getNumberAstechs());
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

    public int getShorthandedModForCrews(int hits) {
        int helpMod = 0;
        if (hits >= 5) {
            helpMod = 4;
        } else if (hits == 4) {
            helpMod = 3;
        } else if (hits == 3) {
            helpMod = 2;
        } else if (hits > 0) {
            helpMod = 1;
        }
        return helpMod;
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
        int medics = getMedicPool(); // this uses a getter for unit testing
        for (Person p : getActivePersonnel()) {
            if (p.isMedic() && !p.isDeployed()) {
                medics++;
            }
        }
        return medics;
    }

    public void increaseMedicPool(int i) {
        medicPool += i;
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, i));
    }

    public void decreaseMedicPool(int i) {
        medicPool = Math.max(0, medicPool - i);
        MekHQ.triggerEvent(new MedicPoolChangedEvent(this, -i));
    }

    public void changeRank(Person person, int rank, boolean report) {
        changeRank(person, rank, 0, report);
    }

    public void changeRank(Person person, int rank, int rankLevel, boolean report) {
        int oldRank = person.getRankNumeric();
        int oldRankLevel = person.getRankLevel();
        person.setRankNumeric(rank);
        person.setRankLevel(rankLevel);

        if (getCampaignOptions().getUseTimeInRank()) {
            if (person.getPrisonerStatus().isFree() && !person.isDependent()) {
                person.setLastRankChangeDate(getLocalDate());
            } else {
                person.setLastRankChangeDate(null);
            }
        }

        personUpdated(person);

        if (report) {
            if (rank > oldRank || ((rank == oldRank) && (rankLevel > oldRankLevel))) {
                ServiceLogger.promotedTo(person, getLocalDate());
            } else if ((rank < oldRank) || (rankLevel < oldRankLevel)) {
                ServiceLogger.demotedTo(person, getLocalDate());
            }
        }
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

    public void setGameOptions(GameOptions gameOptions) {
        this.gameOptions = gameOptions;
    }

    public void setGameOptions(Vector<IBasicOption> options) {
        for (IBasicOption option : options) {
            gameOptions.getOption(option.getName()).setValue(option.getValue());
        }
    }

    /**
     * Imports a {@link Kill} into a campaign.
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
                Person p = getPerson(k.getPilotId());
                if (null != p) {
                    p.awardXP(getCampaignOptions().getKillXPAward());
                    MekHQ.triggerEvent(new PersonChangedEvent(p));
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
        return customs.contains(u.getEntity().getChassis() + " "
                + u.getEntity().getModel());
    }

    /**
     * borrowed from megamek.client
     */
    private void checkDuplicateNamesDuringAdd(Entity entity) {
        if (duplicateNameHash.get(entity.getShortName()) == null) {
            duplicateNameHash.put(entity.getShortName(), 1);
        } else {
            int count = duplicateNameHash.get(entity.getShortName());
            count++;
            duplicateNameHash.put(entity.getShortName(), count);
            entity.duplicateMarker = count;
            entity.generateShortName();
            entity.generateDisplayName();
        }
    }

    /**
     * If we remove a unit, we may need to update the duplicate identifier. TODO: This function is super slow :(
     *
     * @param entity This is the entity whose name is checked for any duplicates
     */
    private void checkDuplicateNamesDuringDelete(Entity entity) {
        Integer o = duplicateNameHash.get(entity.getShortNameRaw());
        if (o != null) {
            int count = o;
            if (count > 1) {
                for (Unit u : getUnits()) {
                    Entity e = u.getEntity();
                    if (e.getShortNameRaw().equals(entity.getShortNameRaw()) && (e.duplicateMarker > entity.duplicateMarker)) {
                        e.duplicateMarker--;
                        e.generateShortName();
                        e.generateDisplayName();
                    }
                }
                duplicateNameHash.put(entity.getShortNameRaw(), count - 1);
            } else {
                duplicateNameHash.remove(entity.getShortNameRaw());
            }
        }
    }

    public String getUnitRatingText() {
        return getUnitRating().getUnitRating();
    }

    /**
     * Against the Bot Calculates and returns dragoon rating if that is the chosen
     * method; for IOps method, returns unit reputation / 10. If the player chooses
     * not to use unit rating at all, use a default value of C. Note that the AtB
     * system is designed for use with FMMerc dragoon rating, and use of the IOps
     * Beta system may have unsatisfactory results, but we follow the options set by
     * the user here.
     */
    public int getUnitRatingMod() {
        if (!getCampaignOptions().getUnitRatingMethod().isEnabled()) {
            return IUnitRating.DRAGOON_C;
        }
        IUnitRating rating = getUnitRating();
        return getCampaignOptions().getUnitRatingMethod().isFMMR() ? rating.getUnitRatingAsInteger()
                : rating.getModifier();
    }

    /**
     * This is a better method for pairing AtB with IOpts with regards to Prisoner Capture
     */
    public int getUnitRatingAsInteger() {
        return getCampaignOptions().getUnitRatingMethod().isEnabled()
                ? getUnitRating().getUnitRatingAsInteger() : IUnitRating.DRAGOON_C;
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return rskillPrefs;
    }

    public void setRandomSkillPreferences(RandomSkillPreferences prefs) {
        rskillPrefs = prefs;
    }

    public void setStartingSystem() {
        Map<String, PlanetarySystem> systemList = Systems.getInstance().getSystems();
        PlanetarySystem startingSystem = systemList.get(getFaction().getStartingPlanet(getLocalDate()));

        if (startingSystem == null) {
            startingSystem = systemList.get(JOptionPane.showInputDialog(
                    "This faction does not have a starting planet for this era. Please choose a planet."));
            while (startingSystem == null) {
                startingSystem = systemList.get(JOptionPane
                        .showInputDialog("This planet you entered does not exist. Please choose a valid planet."));
            }
        }
        location = new CurrentLocation(startingSystem, 0);
    }

    public void addLogEntry(Person p, LogEntry entry) {
        p.addLogEntry(entry);
    }

    /**
     * Assigns a random portrait to a {@link Person}.
     * @param p The {@link Person} who should receive a randomized portrait.
     */
    public void assignRandomPortraitFor(Person p) {
        AbstractIcon portrait = RandomPortraitGenerator.generate(getPersonnel(), p);
        if (!portrait.isDefault()) {
            p.setPortrait(portrait);
        }
    }

    /**
     * Assigns a random origin to a {@link Person}.
     * @param p The {@link Person} who should receive a randomized origin.
     */
    public void assignRandomOriginFor(Person p) {
        AbstractFactionSelector factionSelector = getFactionSelector();
        AbstractPlanetSelector planetSelector = getPlanetSelector();

        Faction faction = factionSelector.selectFaction(this);
        Planet planet = planetSelector.selectPlanet(this, faction);

        p.setOriginFaction(faction);
        p.setOriginPlanet(planet);
    }

    public void clearGameData(Entity entity) {
        for (Mounted m : entity.getEquipment()) {
            m.setUsedThisRound(false);
            m.resetJam();
        }
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

        if (!entity.getSensors().isEmpty()) {
            entity.setNextSensor(entity.getSensors().firstElement());
        }

        if (entity instanceof IBomber) {
            IBomber bomber = (IBomber) entity;
            List<Mounted> mountedBombs = bomber.getBombs();
            if (mountedBombs.size() > 0) {
                //This should return an int[] filled with 0's
                int[] bombChoices = bomber.getBombChoices();
                for (Mounted m : mountedBombs) {
                    if (!(m.getType() instanceof BombType)) {
                        continue;
                    }
                    if (m.getBaseShotsLeft() == 1) {
                        bombChoices[BombType.getBombTypeFromInternalName(m.getType().getInternalName())] += 1;
                    }
                }
                bomber.setBombChoices(bombChoices);
                bomber.clearBombs();
            }
        }

        if (entity instanceof Mech) {
            Mech m = (Mech) entity;
            m.setCoolingFlawActive(false);
        } else if (entity instanceof Aero) {
            Aero a = (Aero) entity;

            if (a.isSpheroid()) {
                entity.setMovementMode(EntityMovementMode.SPHEROID);
            } else {
                entity.setMovementMode(EntityMovementMode.AERODYNE);
            }
            a.setAltitude(5);
            a.setCurrentVelocity(0);
            a.setNextVelocity(0);
        } else if (entity instanceof Tank) {
            Tank t = (Tank) entity;
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
                        if ((entity.getC3MasterIsUUIDAsString() != null)
                                && entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                            entity.setC3Master(e, false);
                            break;
                        }
                    }
                    //Naval C3 checks
                    if (entity.hasNavalC3() && !NC3Set) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        //Well, they're the same value of 6...
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getNC3NextUUIDAsString(pos) != null)
                                    && (e.getC3UUIDAsString() != null)
                                    && entity.getNC3NextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
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
                            if ((entity.getC3iNextUUIDAsString(pos) != null)
                                    && (e.getC3UUIDAsString() != null)
                                    && entity.getC3iNextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
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
        // collect all of the other units on this network to rebuild the uuids
        Vector<Unit> networkedUnits = new Vector<>();
        for (Unit unit : getUnits()) {
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(u.getEntity().getC3NetId())) {
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
        // collect all of the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<>();
        Vector<Unit> networkedUnits = new Vector<>();
        String network = removedUnits.get(0).getEntity().getC3NetId();
        for (Unit unit : getUnits()) {
            if (removedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(network)) {
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
                        nUnit.getEntity().setNC3NextUUIDAsString(pos,
                                uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos,
                                uuids.get(pos));
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
        // collect all of the other units on this network to rebuild the uuids
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
            if (null != unit.getEntity().getC3NetId()
                    && unit.getEntity().getC3NetId().equals(netid)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos,
                                uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos,
                                uuids.get(pos));
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
            if (en.hasC3i() && en.calculateFreeC3Nodes() < 5
                    && en.calculateFreeC3Nodes() > 0) {
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
     * Method that returns a Vector of the unique name Strings of all Naval C3 networks that have at least 1 free node
     * Adapted from getAvailableC3iNetworks() as the two technologies have very similar workings
     * @return
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
            if (en.hasNavalC3() && en.calculateFreeC3Nodes() < 5
                    && en.calculateFreeC3Nodes() > 0) {
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
                network[2] = "" + en.getShortName();
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
                network[2] = "" + en.getShortName();
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
            if (null != unit.getEntity().getC3MasterIsUUIDAsString()
                    && unit.getEntity().getC3MasterIsUUIDAsString().equals(master.getEntity().getC3UUIDAsString())) {
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
                game.addEntity(en.getId(), en);
            }
        });
    }

    public void completeMission(int id, int status) {
        Mission mission = getMission(id);
        if (null == mission) {
            return;
        }
        mission.setStatus(status);
        if (mission instanceof Contract) {
            Contract contract = (Contract) mission;
            Money remainingMoney = Money.zero();
            // check for money in escrow
            // According to FMM(r) pg 179, both failure and breach lead to no
            // further payment even though this seems stupid
            if ((contract.getStatus() == Mission.S_SUCCESS)
                    && contract.getMonthsLeft(getLocalDate()) > 0) {
                remainingMoney = contract.getMonthlyPayOut()
                        .multipliedBy(contract.getMonthsLeft(getLocalDate()));
            }

            // If overage repayment is enabled, we first need to check if the salvage percent is
            // under 100. 100 means you cannot have a overage.
            // Then, we check if the salvage percent is less than the percent salvaged by the
            // unit in question. If it is, then they owe the assigner some cash
            if (getCampaignOptions().getOverageRepaymentInFinalPayment()
                    && (contract.getSalvagePct() < 100)) {
                Money totalSalvaged = contract.getSalvagedByEmployer().plus(contract.getSalvagedByUnit());
                double percentSalvaged = contract.getSalvagedByUnit().getAmount().doubleValue() / totalSalvaged.getAmount().doubleValue();
                double salvagePercent = contract.getSalvagePct() / 100.0;

                if (salvagePercent < percentSalvaged) {
                    Money amountToRepay = totalSalvaged.multipliedBy(percentSalvaged - salvagePercent);
                    remainingMoney = remainingMoney.minus(amountToRepay);
                    contract.subtractSalvageByUnit(amountToRepay);
                }
            }

            if (remainingMoney.isPositive()) {
                finances.credit(remainingMoney, Transaction.C_CONTRACT,
                        "Remaining payment for " + contract.getName(), getLocalDate());
                addReport("Your account has been credited for " + remainingMoney.toAmountAndSymbolString()
                        + " for the remaining payout from contract " + contract.getName());
            } else if (remainingMoney.isNegative()) {
                finances.debit(remainingMoney, Transaction.C_CONTRACT,
                        "Repaying payment overages for " + contract.getName(), getLocalDate());
                addReport("Your account has been debited for " + remainingMoney.toAmountAndSymbolString()
                        + " to replay payment overages occurred during the contract " + contract.getName());
            }

            // This relies on the mission being a Contract, and AtB to be on
            if (getCampaignOptions().getUseAtB()) {
                setHasActiveContract();
            }
        }
    }

    /***
     * Calculate transit time for supplies based on what planet they are shipping from. To prevent extra
     * computation. This method does not calculate an exact jump path but rather determines the number of jumps
     * crudely by dividing distance in light years by 30 and then rounding up. Total part time is determined by
     * several by adding the following:
     * - (number of jumps - 1) * 7 days with a minimum value of zero.
     * - transit times from current planet and planet of supply origins in cases where the supply planet is not the same as current planet.
     * - a random 1d6 days for each jump plus 1d6 to simulate all of the other logistics of delivery.
     * @param system - A <code>PlanetarySystem</code> object where the supplies are shipping from
     * @return the number of days that supplies will take to arrive.
     */
    public int calculatePartTransitTime(PlanetarySystem system) {
        //calculate number of jumps by light year distance as the crow flies divided by 30
        //the basic formula assumes 7 days per jump + system transit time on each side + random days equal
        //to (1 + number of jumps) d6
        double distance = system.getDistanceTo(getCurrentSystem());
        //calculate number of jumps by dividing by 30
        int jumps = (int) Math.ceil(distance / 30.0);
        //you need a recharge except for the first jump
        int recharges = Math.max(jumps - 1, 0);
        //if you are delivering from the same planet then no transit times
        int currentTransitTime = (distance > 0) ? (int) Math.ceil(getCurrentSystem().getTimeToJumpPoint(1.0)) : 0;
        int originTransitTime = (distance > 0) ? (int) Math.ceil(system.getTimeToJumpPoint(1.0)) : 0;
        int amazonFreeShipping = Compute.d6(1 + jumps);
        return (recharges * 7) + currentTransitTime+originTransitTime + amazonFreeShipping;
    }

    /***
     * Calculate transit times based on the margin of success from an acquisition roll. The values here
     * are all based on what the user entered for the campaign options.
     * @param mos - an integer of the margin of success of an acquisition roll
     * @return the number of days that supplies will take to arrive.
     */
    public int calculatePartTransitTime(int mos) {
        int nDice = getCampaignOptions().getNDiceTransitTime();
        int time = getCampaignOptions().getConstantTransitTime();
        if (nDice > 0) {
            time += Compute.d6(nDice);
        }
        // now step forward through the calendar
        LocalDate arrivalDate = getLocalDate();
        switch (getCampaignOptions().getUnitTransitTime()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                arrivalDate = arrivalDate.plusMonths(time);
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                arrivalDate = arrivalDate.plusWeeks(time);
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                arrivalDate = arrivalDate.plusDays(time);
                break;
        }

        // now adjust for MoS and minimums
        int mosBonus = getCampaignOptions().getAcquireMosBonus() * mos;
        switch (getCampaignOptions().getAcquireMosUnit()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                arrivalDate = arrivalDate.minusMonths(mosBonus);
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                arrivalDate = arrivalDate.minusWeeks(mosBonus);
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                arrivalDate = arrivalDate.minusDays(mosBonus);
                break;
        }

        // now establish minimum date and if this is before
        LocalDate minimumDate = getLocalDate();
        switch (getCampaignOptions().getAcquireMinimumTimeUnit()) {
            case CampaignOptions.TRANSIT_UNIT_MONTH:
                minimumDate = minimumDate.plusMonths(getCampaignOptions().getAcquireMinimumTime());
                break;
            case CampaignOptions.TRANSIT_UNIT_WEEK:
                minimumDate = minimumDate.plusWeeks(getCampaignOptions().getAcquireMinimumTime());
                break;
            case CampaignOptions.TRANSIT_UNIT_DAY:
            default:
                minimumDate = minimumDate.plusDays(getCampaignOptions().getAcquireMinimumTime());
                break;
        }

        if (arrivalDate.isBefore(minimumDate)) {
            return Math.toIntExact(ChronoUnit.DAYS.between(getLocalDate(), minimumDate));
        } else {
            return Math.toIntExact(ChronoUnit.DAYS.between(getLocalDate(), arrivalDate));
        }
    }

    /**
     * This returns a PartInventory object detailing the current count
     * for a part on hand, in transit, and ordered.
     *
     * @param part A part to lookup its current inventory.
     * @return A PartInventory object detailing the current counts of
     * the part on hand, in transit, and ordered.
     * @see mekhq.campaign.parts.PartInventory
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
                    if (p instanceof Armor) { // ProtomekArmor and BaArmor are derived from Armor
                        nSupply += ((Armor) p).getAmount();
                    } else if (p instanceof AmmoStorage) {
                        nSupply += ((AmmoStorage) p).getShots();
                    } else {
                        nSupply += p.getQuantity();
                    }
                } else {
                    if (p instanceof Armor) { // ProtomekArmor and BaArmor are derived from Armor
                        nTransit += ((Armor) p).getAmount();
                    } else if (p instanceof AmmoStorage) {
                        nTransit += ((AmmoStorage) p).getShots();
                    } else {
                        nTransit += p.getQuantity();
                    }
                }
            }
        }

        inventory.setSupply(nSupply);
        inventory.setTransit(nTransit);

        int nOrdered = 0;
        IAcquisitionWork onOrder = getShoppingList().getShoppingItem(part);
        if (null != onOrder) {
            if (onOrder instanceof Armor) { // ProtoMech Armor and BaArmor are derived from Armor
                nOrdered += ((Armor) onOrder).getAmount();
            } else if (onOrder instanceof AmmoStorage) {
                nOrdered += ((AmmoStorage) onOrder).getShots();
            } else {
                nOrdered += onOrder.getQuantity();
            }
        }

        inventory.setOrdered(nOrdered);

        String countModifier = "";
        if (part instanceof Armor) { // ProtoMech Armor and BaArmor are derived from Armor
            countModifier = "points";
        }
        if (part instanceof AmmoStorage) {
            countModifier = "shots";
        }

        inventory.setCountModifier(countModifier);
        return inventory;
    }

    public void addLoan(Loan loan) {
        addReport("You have taken out loan " + loan.getDescription()
                + ". Your account has been credited "
                + loan.getPrincipal().toAmountAndSymbolString()
                + " for the principal amount.");
        finances.addLoan(loan);
        MekHQ.triggerEvent(new LoanNewEvent(loan));
        finances.credit(loan.getPrincipal(), Transaction.C_LOAN_PRINCIPAL,
                "loan principal for " + loan.getDescription(), getLocalDate());
    }

    public void payOffLoan(Loan loan) {
        if (finances.debit(loan.getRemainingValue(),
                Transaction.C_LOAN_PAYMENT, "loan payoff for " + loan.getDescription(), getLocalDate())) {
            addReport("You have paid off the remaining loan balance of "
                    + loan.getRemainingValue().toAmountAndSymbolString()
                    + "on " + loan.getDescription());
            finances.removeLoan(loan);
            MekHQ.triggerEvent(new LoanPaidEvent(loan));
        } else {
            addReport("<font color='red'>You do not have enough funds to pay off "
                    + loan.getDescription() + "</font>");
        }

    }

    public void setHealingTimeOptions(int newHeal, int newNaturalHeal) {
        // we need to check the current values and then if necessary change the
        // times for all
        // personnel, giving them credit for their current waiting time
        int currentHeal = getCampaignOptions().getHealingWaitingPeriod();
        int currentNaturalHeal = getCampaignOptions()
                .getNaturalHealingWaitingPeriod();

        getCampaignOptions().setHealingWaitingPeriod(newHeal);
        getCampaignOptions().setNaturalHealingWaitingPeriod(newNaturalHeal);

        int healDiff = newHeal - currentHeal;
        int naturalDiff = newNaturalHeal - currentNaturalHeal;

        if (healDiff != 0 || naturalDiff != 0) {
            for (Person p : getPersonnel()) {
                if (p.getDoctorId() != null) {
                    p.setDaysToWaitForHealing(Math.max(
                            p.getDaysToWaitForHealing() + healDiff, 1));
                } else {
                    p.setDaysToWaitForHealing(Math.max(
                            p.getDaysToWaitForHealing() + naturalDiff, 1));
                }
            }
        }
    }

    /**
     * Returns our list of potential transport ships
     * @return
     */
    public Set<Unit> getTransportShips() {
        return Collections.unmodifiableSet(transportShips);
    }

    public void doMaintenance(Unit u) {
        if (!u.requiresMaintenance() || !campaignOptions.checkMaintenance()) {
            return;
        }
        // lets start by checking times
        Person tech = u.getTech();
        int minutesUsed = u.getMaintenanceTime();
        int astechsUsed = getAvailableAstechs(minutesUsed, false);
        boolean maintained = ((tech != null) && (tech.getMinutesLeft() >= minutesUsed)
                && !tech.isMothballing());
        boolean paidMaintenance = true;
        if (maintained) {
            // use the time
            tech.setMinutesLeft(tech.getMinutesLeft() - minutesUsed);
            astechPoolMinutes -= astechsUsed * minutesUsed;
        }
        u.incrementDaysSinceMaintenance(maintained, astechsUsed);

        int ruggedMultiplier = 1;
        if (u.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_1)) {
            ruggedMultiplier = 2;
        }

        if (u.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_2)) {
            ruggedMultiplier = 3;
        }

        if (u.getDaysSinceMaintenance() >= (getCampaignOptions().getMaintenanceCycleDays() * ruggedMultiplier)) {
            // maybe use the money
            if (campaignOptions.payForMaintain()) {
                if (!(finances.debit(u.getMaintenanceCost(), Transaction.C_MAINTAIN, "Maintenance for "
                                + u.getName(), getLocalDate()))) {
                    addReport("<font color='red'><b>You cannot afford to pay maintenance costs for "
                            + u.getHyperlinkedName() + "!</b></font>");
                    paidMaintenance = false;
                }
            }
            // it is time for a maintenance check
            int qualityOrig = u.getQuality();
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
            StringBuilder maintenanceReport = new StringBuilder("<emph>" + techName + " performing maintenance</emph><br><br>");
            for (Part p : u.getParts()) {
                String partReport = "<b>" + p.getName() + "</b> (Quality " + p.getQualityName() + ")";
                if (!p.needsMaintenance()) {
                    continue;
                }
                int oldQuality = p.getQuality();
                TargetRoll target = getTargetForMaintenance(p, tech);
                if (!paidMaintenance) {
                    // TODO : Make this modifier user inputtable
                    target.addModifier(1, "did not pay for maintenance");
                }

                partReport += ", TN " + target.getValue() + "[" + target.getDesc() + "]";
                int roll = Compute.d6(2);
                int margin = roll - target.getValue();
                partReport += " rolled a " + roll + ", margin of " + margin;

                switch (p.getQuality()) {
                    case Part.QUALITY_A: {
                        if (margin >= 4) {
                            p.improveQuality();
                        }
                        if (!campaignOptions.useUnofficialMaintenance()) {
                            if (margin < -6) {
                                partsToDamage.put(p, 4);
                            } else if (margin < -4) {
                                partsToDamage.put(p, 3);
                            } else if (margin == -4) {
                                partsToDamage.put(p, 2);
                            } else if (margin < -1) {
                                partsToDamage.put(p, 1);
                            }
                        } else if (margin < -6) {
                            partsToDamage.put(p, 1);
                        }
                        break;
                    }
                    case Part.QUALITY_B: {
                        if (margin >= 4) {
                            p.improveQuality();
                        } else if (margin < -5) {
                            p.decreaseQuality();
                        }
                        if (!campaignOptions.useUnofficialMaintenance()) {
                            if (margin < -6) {
                                partsToDamage.put(p, 2);
                            } else if (margin < -2) {
                                partsToDamage.put(p, 1);
                            }
                        }
                        break;
                    }
                    case Part.QUALITY_C: {
                        if (margin < -4) {
                            p.decreaseQuality();
                        } else if (margin >= 5) {
                            p.improveQuality();
                        }
                        if (!campaignOptions.useUnofficialMaintenance()) {
                            if (margin < -6) {
                                partsToDamage.put(p, 2);
                            } else if (margin < -3) {
                                partsToDamage.put(p, 1);
                            }
                        }
                        break;
                    }
                    case Part.QUALITY_D: {
                        if (margin < -3) {
                            p.decreaseQuality();
                            if ((margin < -4) && !campaignOptions.useUnofficialMaintenance()) {
                                partsToDamage.put(p, 1);
                            }
                        } else if (margin >= 5) {
                            p.improveQuality();
                        }
                        break;
                    }
                    case Part.QUALITY_E:
                        if (margin < -2) {
                            p.decreaseQuality();
                            if ((margin < -5) && !campaignOptions.useUnofficialMaintenance()) {
                                partsToDamage.put(p, 1);
                            }
                        } else if (margin >= 6) {
                            p.improveQuality();
                        }
                        break;
                    case Part.QUALITY_F:
                    default:
                        if (margin < -2) {
                            p.decreaseQuality();
                            if (margin < -6 && !campaignOptions.useUnofficialMaintenance()) {
                                partsToDamage.put(p, 1);
                            }
                        }
                        // TODO: award XP point if margin >= 6 (make this optional)
                        //if (margin >= 6) {
                        //
                        //}
                        break;
                }
                if (p.getQuality() > oldQuality) {
                    partReport += ": <font color='green'>new quality is " + p.getQualityName() + "</font>";
                } else if (p.getQuality() < oldQuality) {
                    partReport += ": <font color='red'>new quality is " + p.getQualityName() + "</font>";
                } else {
                    partReport += ": quality remains " + p.getQualityName();
                }
                if (null != partsToDamage.get(p)) {
                    if (partsToDamage.get(p) > 3) {
                        partReport += ", <font color='red'><b>part destroyed</b></font>";
                    } else {
                        partReport += ", <font color='red'><b>part damaged</b></font>";
                    }
                }
                maintenanceReport.append(partReport).append("<br>");
            }

            int nDamage = 0;
            int nDestroy = 0;
            for (Map.Entry<Part, Integer> p : partsToDamage.entrySet()) {
                int damage = p.getValue();
                if (damage > 3) {
                    nDestroy++;
                    p.getKey().remove(false);
                } else {
                    p.getKey().doMaintenanceDamage(damage);
                    nDamage++;
                }
            }

            u.setLastMaintenanceReport(maintenanceReport.toString());

            if (getCampaignOptions().logMaintenance()) {
                MekHQ.getLogger().info(getClass(), "doMaintenance", maintenanceReport.toString());
            }

            int quality = u.getQuality();
            String qualityString;
            boolean reverse = getCampaignOptions().reverseQualityNames();
            if (quality > qualityOrig) {
                qualityString = "<font color='green'>Overall quality improves from "
                        + Part.getQualityName(qualityOrig, reverse) + " to " + Part.getQualityName(quality, reverse)
                        + "</font>";
            } else if (quality < qualityOrig) {
                qualityString = "<font color='red'>Overall quality declines from "
                        + Part.getQualityName(qualityOrig, reverse) + " to " + Part.getQualityName(quality, reverse)
                        + "</font>";
            } else {
                qualityString = "Overall quality remains " + Part.getQualityName(quality, reverse);
            }
            String damageString = "";
            if (nDamage > 0) {
                damageString += nDamage + " parts were damaged. ";
            }
            if (nDestroy > 0) {
                damageString += nDestroy + " parts were destroyed.";
            }
            if (!damageString.isEmpty()) {
                damageString = "<b><font color='red'>" + damageString + "</b></font> [<a href='REPAIR|" + u.getId()
                        + "'>Repair bay</a>]";
            }
            String paidString = "";
            if (!paidMaintenance) {
                paidString = "<font color='red'>Could not afford maintenance costs, so check is at a penalty.</font>";
            }
            addReport(techNameLinked + " performs maintenance on " + u.getHyperlinkedName() + ". " + paidString
                    + qualityString + ". " + damageString + " [<a href='MAINTENANCE|" + u.getId()
                    + "'>Get details</a>]");

            u.resetDaysSinceMaintenance();
        }
    }

    public void initTimeInService() {
        for (Person p : getPersonnel()) {
            if (!p.isDependent() && p.getPrisonerStatus().isFree()) {
                LocalDate join = null;
                for (LogEntry e : p.getPersonnelLog()) {
                    if (join == null) {
                        // If by some nightmare there is no Joined date just use the first entry.
                        join = e.getDate();
                    }
                    if (e.getDesc().startsWith("Joined ") || e.getDesc().startsWith("Freed ")) {
                        join = e.getDate();
                        break;
                    }
                }

                p.setRecruitment((join != null) ? join : getLocalDate().minusYears(1));
            }
        }
    }

    public void initTimeInRank() {
        for (Person p : getPersonnel()) {
            if (!p.isDependent() && p.getPrisonerStatus().isFree()) {

                LocalDate join = null;
                for (LogEntry e : p.getPersonnelLog()) {
                    if (join == null) {
                        // If by some nightmare there is no date from the below, just use the first entry.
                        join = e.getDate();
                    }

                    if (e.getDesc().startsWith("Joined ") || e.getDesc().startsWith("Freed ")
                            || e.getDesc().startsWith("Promoted ") || e.getDesc().startsWith("Demoted ")) {
                        join = e.getDate();
                    }
                }

                // For that one in a billion chance the log is empty. Clone today's date and subtract a year
                p.setLastRankChangeDate((join != null) ? join : getLocalDate().minusYears(1));
            }
        }
    }

    public void initRetirementDateTracking() {
        for (Person person : getPersonnel()) {
            if (person.getStatus().isRetired()) {
                LocalDate retired = null;
                LocalDate lastLoggedDate = null;
                for (LogEntry entry : person.getPersonnelLog()) {
                    lastLoggedDate = entry.getDate();
                    if (entry.getDesc().startsWith("Retired")) {
                        retired = entry.getDate();
                    }
                }

                if (retired == null) {
                    retired = lastLoggedDate;
                }

                // For that one in a billion chance the log is empty. Clone today's date and subtract a year
                person.setRetirement((retired != null) ? retired : getLocalDate().minusYears(1));
            }
        }
    }

    public void initAtB(boolean newCampaign) {
        getRetirementDefectionTracker().setLastRetirementRoll(getLocalDate());

        if (!newCampaign) {
            /*
            * Switch all contracts to AtBContract's
            */
            for (Map.Entry<Integer, Mission> me : missions.entrySet()) {
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
            for (Person p : getPersonnel()) {
                for (LogEntry e : p.getPersonnelLog()) {
                    if ((founding == null) || e.getDate().isBefore(founding)) {
                        founding = e.getDate();
                    }
                }
            }
            /*
            * Go through the personnel records again and assume that any person who joined
            * the unit on the founding date is one of the founding members. Also assume
            * that MWs assigned to a non-Assault 'Mech on the date they joined came with
            * that 'Mech (which is a less certain assumption)
            */
            for (Person p : getPersonnel()) {
                LocalDate join = null;
                for (LogEntry e : p.getPersonnelLog()) {
                    if (e.getDesc().startsWith("Joined ")) {
                        join = e.getDate();
                        break;
                    }
                }
                if ((join != null) && join.equals(founding)) {
                    p.setFounder(true);
                }
                if (p.getPrimaryRole() == Person.T_MECHWARRIOR
                        || (p.getPrimaryRole() == Person.T_AERO_PILOT && getCampaignOptions().getAeroRecruitsHaveUnits())
                        || p.getPrimaryRole() == Person.T_PROTO_PILOT) {
                    for (LogEntry e : p.getPersonnelLog()) {
                        if (e.getDate().equals(join) && e.getDesc().startsWith("Assigned to ")) {
                            String mech = e.getDesc().substring(12);
                            MechSummary ms = MechSummaryCache.getInstance().getMech(mech);
                            if (null != ms && (p.isFounder()
                                    || ms.getWeightClass() < megamek.common.EntityWeightClass.WEIGHT_ASSAULT)) {
                                p.setOriginalUnitWeight(ms.getWeightClass());
                                if (ms.isClan()) {
                                    p.setOriginalUnitTech(Person.TECH_CLAN);
                                } else if (ms.getYear() > 3050) {
                                    // TODO : Fix this so we aren't using a hack that just assumes IS2
                                    p.setOriginalUnitTech(Person.TECH_IS2);
                                }
                                if ((null != p.getUnit())
                                        && ms.getName().equals(p.getUnit().getEntity().getShortNameRaw())) {
                                    p.setOriginalUnitId(p.getUnit().getId());
                                }
                            }
                        }
                    }
                }
            }

            addAllLances(this.forces);

            // Determine whether or not there is an active contract
            setHasActiveContract();
        }

        setAtBConfig(AtBConfiguration.loadFromXml());
        RandomFactionGenerator.getInstance().startup(this);
        getContractMarket().generateContractOffers(this, newCampaign);
        getUnitMarket().generateUnitOffers(this);
        setAtBEventProcessor(new AtBEventProcessor(this));
    }

    /**
     * Stop processing AtB events and release memory.
     */
    public void shutdownAtB() {
        RandomFactionGenerator.getInstance().dispose();
        RandomUnitGenerator.getInstance().dispose();
        atbEventProcessor.shutdown();
    }

    public boolean checkOverDueLoans() {
        Money overdueAmount = getFinances().checkOverdueLoanPayments(this);
        if (overdueAmount.isPositive()) {
            JOptionPane.showMessageDialog(
                    null,
                    "You have overdue loan payments totaling "
                            + overdueAmount.toAmountAndSymbolString()
                            + "\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay off the collateral on the loan.\n  - Default on the loan.\n  - Just cheat and remove the loan via GM mode.",
                            "Overdue Loan Payments",
                            JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    public boolean checkRetirementDefections() {
        if (getRetirementDefectionTracker().getRetirees().size() > 0) {
            Object[] options = { "Show Payout Dialog", "Cancel" };
            return JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "You have personnel who have left the unit or been killed in action but have not received their final payout.\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay one or more personnel in equipment.\n  - Just cheat and use GM mode to edit the settlement.",
                            "Unresolved Final Payments",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0]);
        }
        return false;
    }

    public boolean checkYearlyRetirements() {
        if (getCampaignOptions().getUseAtB()
                && (ChronoUnit.DAYS.between(getRetirementDefectionTracker().getLastRetirementRoll(),
                getLocalDate()) == getRetirementDefectionTracker().getLastRetirementRoll().lengthOfYear())) {
            Object[] options = { "Show Retirement Dialog", "Not Now" };
            return JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "It has been a year since the last retirement/defection roll, and it is time to do another.",
                            "Retirement/Defection roll required",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0]);
        }
        return false;
    }

    /**
     * Sets the type of rating method used.
     */
    public void setUnitRating(IUnitRating rating) {
        unitRating = rating;
    }

    /**
     * Returns the type of rating method as selected in the Campaign Options dialog.
     * Lazy-loaded for performance. Default is CampaignOpsReputation
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
            } else {
                unitRating = new CampaignOpsReputation(this);
            }
        }

        return unitRating;
    }

    @Override
    public int getTechIntroYear() {
        if (getCampaignOptions().limitByYear()) {
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
    public int getTechFaction() {
        return techFactionCode;
    }

    public void updateTechFactionCode() {
        if (campaignOptions.useFactionIntroDate()) {
            for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
                if (ITechnology.MM_FACTION_CODES[i].equals(factionCode)) {
                    techFactionCode = i;
                    UnitTechProgression.loadFaction(techFactionCode);
                    return;
                }
            }
            // If the tech progression data does not include the current faction,
            // use a generic.
            if (getFaction().isClan()) {
                techFactionCode = ITechnology.F_CLAN;
            } else if (getFaction().isPeriphery()) {
                techFactionCode = ITechnology.F_PER;
            } else {
                techFactionCode = ITechnology.F_IS;
            }
        } else {
            techFactionCode = ITechnology.F_NONE;
        }
        // Unit tech level will be calculated if the code has changed.
        UnitTechProgression.loadFaction(techFactionCode);
    }

    @Override
    public boolean useClanTechBase() {
        return getFaction().isClan();
    }

    @Override
    public boolean useMixedTech() {
        if (useClanTechBase()) {
            return campaignOptions.allowISPurchases();
        } else {
            return campaignOptions.allowClanPurchases();
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
        return campaignOptions.useVariableTechLevel();
    }

    @Override
    public boolean showExtinct() {
        return !campaignOptions.disallowExtinctStuff();
    }
}
