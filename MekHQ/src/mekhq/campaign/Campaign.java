/*
 * Campaign.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.equipment.BombMounted;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.*;
import megamek.common.util.BuildingBlock;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Quartermaster.PartAcquisitionResult;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.event.*;
import mekhq.campaign.finances.*;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.log.HistoricalLogEntry;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.*;
import mekhq.campaign.personnel.death.AbstractDeath;
import mekhq.campaign.personnel.death.DisabledRandomDeath;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.RandomPortraitGenerator;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.rating.CampaignOpsReputation;
import mekhq.campaign.rating.FieldManualMercRevDragoonsRating;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.*;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.eras.Era;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.RangedFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.RangedPlanetSelector;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.sorter.PersonTitleSorter;
import mekhq.module.atb.AtBEventProcessor;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;
import mekhq.service.mrms.MRMSService;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main campaign class, keeps track of teams and units
 * @author Taharqa
 */
public class Campaign implements ITechManager {
    public static final String REPORT_LINEBREAK = "<br/><br/>";

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
    private Game game;
    private Player player;

    private GameOptions gameOptions;

    private String name;
    private LocalDate currentDay;

    // hierarchically structured Force object to define TO&E
    private Force forces;
    private Hashtable<Integer, Lance> lances; // AtB

    private Faction faction;
    private int techFactionCode;
    private String retainerEmployerCode; // AtB
    private RankSystem rankSystem;

    private ArrayList<String> currentReport;
    private transient String currentReportHTML;
    private transient List<String> newReports;

    // this is updated and used per gaming session, it is enabled/disabled via the Campaign options
    // we're re-using the LogEntry class that is used to store Personnel entries
    public LinkedList<LogEntry> inMemoryLogHistory = new LinkedList<>();

    private boolean overtime;
    private boolean gmMode;
    private transient boolean overviewLoadingValue = true;

    private Camouflage camouflage = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name());
    private PlayerColour colour = PlayerColour.BLUE;
    private StandardForceIcon unitIcon = new UnitIcon(null, null);

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
    private AbstractUnitMarket unitMarket;

    private transient AbstractDeath death;
    private transient AbstractDivorce divorce;
    private transient AbstractMarriage marriage;
    private transient AbstractProcreation procreation;

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
    private StoryArc storyArc;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
            MekHQ.getMHQOptions().getLocale());

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
        setFaction(Factions.getInstance().getDefaultFaction());
        techFactionCode = ITechnology.F_MERC;
        retainerEmployerCode = null;
        setRankSystemDirect(Ranks.getRankSystemFromCode(Ranks.DEFAULT_SYSTEM_CODE));
        forces = new Force(name);
        forceIds.put(0, forces);
        lances = new Hashtable<>();
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
        news = new News(getGameYear(), id.getLeastSignificantBits());
        setPersonnelMarket(new PersonnelMarket());
        setContractMarket(new ContractMarket());
        setUnitMarket(new DisabledUnitMarket());
        setDeath(new DisabledRandomDeath(getCampaignOptions(), false));
        setDivorce(new DisabledRandomDivorce(getCampaignOptions()));
        setMarriage(new DisabledRandomMarriage(getCampaignOptions()));
        setProcreation(new DisabledRandomProcreation(getCampaignOptions()));
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

    public Era getEra() {
        return Eras.getInstance().getEra(getLocalDate());
    }

    public String getTitle() {
        return getName() + " (" + getFaction().getFullName(getGameYear()) + ")" + " - "
                + MekHQ.getMHQOptions().getLongDisplayFormattedDate(getLocalDate())
                + " (" + getEra() + ")";
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

    public List<Force> getAllForces() {
        return new ArrayList<>(forceIds.values());
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

    //region Markets
    public PersonnelMarket getPersonnelMarket() {
        return personnelMarket;
    }

    public void setPersonnelMarket(final PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
    }

    // TODO : AbstractContractMarket : Swap to AbstractContractMarket
    public ContractMarket getContractMarket() {
        return contractMarket;
    }

    // TODO : AbstractContractMarket : Swap to AbstractContractMarket
    public void setContractMarket(final ContractMarket contractMarket) {
        this.contractMarket = contractMarket;
    }

    public AbstractUnitMarket getUnitMarket() {
        return unitMarket;
    }

    public void setUnitMarket(final AbstractUnitMarket unitMarket) {
        this.unitMarket = unitMarket;
    }
    //endregion Markets

    //region Personnel Modules
    public AbstractDeath getDeath() {
        return death;
    }

    public void setDeath(final AbstractDeath death) {
        this.death = death;
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
    //endregion Personnel Modules

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
        if (campaignOptions.isUseStaticRATs()) {
            RATManager rm = new RATManager();
            while (!RandomUnitGenerator.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    LogManager.getLogger().error("", e);
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
        if (getFinances().debit(TransactionType.UNIT_PURCHASE, getLocalDate(),
                getAtBConfig().shipSearchCostPerWeek(), "Ship Search")) {
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
            // TODO : mos zero should make ship available on retainer
            if (roll >= target.getValue()) {
                report.append("<br/>Search successful. ");

                MechSummary ms = getUnitGenerator().generate(getFactionCode(), shipSearchType, -1,
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
                            .append(MekHQ.getMHQOptions().getDisplayFormattedDate(getShipSearchExpiration()));
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
        MechSummary ms = MechSummaryCache.getInstance().getMech(getShipSearchResult());
        if (ms == null) {
            LogManager.getLogger().error("Cannot find entry for " + getShipSearchResult());
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
            LogManager.getLogger().error("Unable to load unit: " + ms.getEntryName(), ex);
            return;
        }
        Entity en = mechFileParser.getEntity();

        int transitDays = getCampaignOptions().isInstantUnitMarketDelivery() ? 0
                : calculatePartTransitTime(Compute.d6(2) - 2);

        getFinances().debit(TransactionType.UNIT_PURCHASE, getLocalDate(), cost, "Purchased " + en.getShortName());
        addNewUnit(en, true, transitDays);
        if (!getCampaignOptions().isInstantUnitMarketDelivery()) {
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
    public boolean applyRetirement(Money totalPayout, Map<UUID, UUID> unitAssignments) {
        if ((totalPayout.isPositive()) || (null != getRetirementDefectionTracker().getRetirees())) {
            if (getFinances().debit(TransactionType.RETIREMENT, getLocalDate(), totalPayout, "Final Payout")) {
                for (UUID pid : getRetirementDefectionTracker().getRetirees()) {
                    if (getPerson(pid).getStatus().isActive()) {
                        getPerson(pid).changeStatus(this, getLocalDate(), PersonnelStatus.RETIRED);
                    }

                    if (getRetirementDefectionTracker().getPayout(pid).getRecruitRole().isCivilian()) {
                        LogManager.getLogger().error(String.format(
                                "Attempted to process a payout for %s, who has a civilian role.",
                                getRetirementDefectionTracker().getPayout(pid).getRecruitRole().name()));
                    } else {
                        getPersonnelMarket().addPerson(newPerson(getRetirementDefectionTracker().getPayout(pid).getRecruitRole()));
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

                    if (getCampaignOptions().getRandomDependentMethod().isAgainstTheBot()
                            && getCampaignOptions().isUseRandomDependentAddition()) {
                        int dependents = getRetirementDefectionTracker().getPayout(pid).getDependents();
                        while (dependents > 0) {
                            Person person = newDependent(false);
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

        if (campaignOptions.isUseAtB() && !force.getUnits().isEmpty()) {
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

    public void addUnitToForce(final @Nullable Unit unit, final Force force) {
        addUnitToForce(unit, force.getId());
    }

    /**
     * Add unit to an existing force. This method will also assign that force's id to the unit.
     *
     * @param u
     * @param id
     */
    public void addUnitToForce(@Nullable Unit u, int id) {
        if (u == null) {
            return;
        }

        Force force = forceIds.get(id);
        Force prevForce = forceIds.get(u.getForceId());
        boolean useTransfers = false;
        boolean transferLog = !getCampaignOptions().isUseTransfers();

        if (null != prevForce) {
            if (null != prevForce.getTechID()) {
                u.removeTech();
            }
            // We log removal if we don't use transfers or if it can't be assigned to a new force
            prevForce.removeUnit(this, u.getId(), transferLog || (force == null));
            useTransfers = !transferLog;
            MekHQ.triggerEvent(new OrganizationChangedEvent(prevForce, u));
        }

        if (null != force) {
            u.setForceId(id);
            u.setScenarioId(force.getScenarioId());
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
            force.addUnit(this, u.getId(), useTransfers, prevForce);
            MekHQ.triggerEvent(new OrganizationChangedEvent(force, u));
        }

        if (campaignOptions.isUseAtB()) {
            if ((null != prevForce) && prevForce.getUnits().isEmpty()) {
                lances.remove(prevForce.getId());
            }

            if ((null == lances.get(id)) && (null != force)) {
                lances.put(id, new Lance(force.getId(), this));
            }
        }
    }

    /** Adds force and all its subforces to the AtB lance table
     */

    private void addAllLances(Force force) {
        if (!force.getUnits().isEmpty()) {
            lances.put(force.getId(), new Lance(force.getId(), this));
        }
        for (Force f : force.getSubForces()) {
            addAllLances(f);
        }
    }

    //region Missions/Contracts
    /**
     * Add a mission to the campaign
     *
     * @param m The mission to be added
     * @return the id of the mission
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
     * @param mission Mission to import into the campaign.
     */
    public void importMission(final Mission mission) {
        mission.getScenarios().forEach(this::importScenario);
        addMissionWithoutId(mission);
        StratconContractInitializer.restoreTransientStratconInformation(mission, this);
    }

    private void addMissionWithoutId(Mission m) {
        lastMissionId = Math.max(lastMissionId, m.getId());
        missions.put(m.getId(), m);
        MekHQ.triggerEvent(new MissionNewEvent(m));
    }

    /**
     * @param id the mission's id
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
                .sorted(Comparator.comparing(Mission::getStatus).thenComparing(m ->
                        (m instanceof Contract) ? ((Contract) m).getStartDate() : LocalDate.now()))
                .collect(Collectors.toList());
    }

    public List<Mission> getActiveMissions(final boolean excludeEndDateCheck) {
        return getMissions().stream()
                .filter(m -> m.isActiveOn(getLocalDate(), excludeEndDateCheck))
                .collect(Collectors.toList());
    }

    public List<Mission> getCompletedMissions() {
        return getMissions().stream()
                .filter(m -> m.getStatus().isCompleted())
                .collect(Collectors.toList());
    }

    /**
     * @return a list of all currently active contracts
     */
    public List<Contract> getActiveContracts() {
        return getMissions().stream()
                .filter(c -> (c instanceof Contract) && c.isActiveOn(getLocalDate()))
                .map(c -> (Contract) c)
                .collect(Collectors.toList());
    }

    public List<AtBContract> getAtBContracts() {
        return getMissions().stream()
                .filter(c -> c instanceof AtBContract)
                .map(c -> (AtBContract) c)
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
        hasActiveContract = getMissions().stream()
                .anyMatch(c -> (c instanceof Contract) && c.isActiveOn(getLocalDate()));
    }
    //endregion Missions/Contracts

    /**
     * Adds scenario to existing mission, generating a report.
     */
    public void addScenario(Scenario s, Mission m) {
        addScenario(s, m, false);
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
     * @param suppressReport - whether or not to suppress the campaign report
     */
    public void addScenario(Scenario s, Mission m, boolean suppressReport) {
        final boolean newScenario = s.getId() == Scenario.S_DEFAULT_ID;
        final int id = newScenario ? ++lastScenarioId : s.getId();
        s.setId(id);
        m.addScenario(s);
        scenarios.put(id, s);

        if (newScenario && !suppressReport) {
            addReport(MessageFormat.format(
                    resources.getString("newAtBScenario.format"),
                    s.getName(), MekHQ.getMHQOptions().getDisplayFormattedDate(s.getDate())));
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
     * @param u A {@link Unit} to import into the campaign.
     */
    public void importUnit(Unit u) {
        Objects.requireNonNull(u);

        LogManager.getLogger().debug("Importing unit: (" + u.getId() + "): " + u.getName());

        getHangar().addUnit(u);

        checkDuplicateNamesDuringAdd(u.getEntity());

        // If this is a ship, add it to the list of potential transports
        if ((u.getEntity() instanceof Dropship) || (u.getEntity() instanceof Jumpship)) {
            addTransportShip(u);
        }

        // Assign an entity ID to our new unit
        if (Entity.NONE == u.getEntity().getId()) {
            u.getEntity().setId(game.getNextEntityId());
        }

        game.addEntity(u.getEntity());
    }

    /**
     * Adds an entry to the list of transit-capable transport ships. We'll use this
     * to look for empty bays that ground units can be assigned to
     * @param unit - The ship we want to add to this Set
     */
    public void addTransportShip(Unit unit) {
        LogManager.getLogger().debug("Adding DropShip/WarShip: " + unit.getId());
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
        if (transportShips.remove(unit) && unit.hasTransportedUnits()) {
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
        // we really just want the entity and the parts so let's just wrap that around a new unit.
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
        game.addEntity(unit.getEntity());

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
        // Added to avoid the 'default force bug' when calculating cargo
        removeUnitFromForce(unit);

        // If this is a ship, add it to the list of potential transports
        if ((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Jumpship)) {
            addTransportShip(unit);
        }

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        if (!unit.isRepairable()) {
            unit.setSalvage(true);
        }
        unit.setDaysToArrival(days);

        if (allowNewPilots) {
            Map<CrewType, Collection<Person>> newCrew = Utilities
                    .genRandomCrewWithCombinedSkill(this, unit, getFactionCode());
            newCrew.forEach((type, personnel) ->
                    personnel.forEach(p -> type.getAddMethod().accept(unit, p)));
        }
        unit.resetPilotAndEntity();

        // Assign an entity ID to our new unit
        if (Entity.NONE == en.getId()) {
            en.setId(game.getNextEntityId());
        }
        game.addEntity(en);

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

    public List<Entity> getEntities() {
        return getUnits().stream()
                .map(Unit::getEntity)
                .collect(Collectors.toList());
    }

    public Unit getUnit(UUID id) {
        return getHangar().getUnit(id);
    }

    //region Personnel
    //region Person Creation
    /**
     * @return A new {@link Person}, who is a dependent.
     */
    public Person newDependent(boolean baby) {
        Person person;

        if (!baby && getCampaignOptions().getRandomOriginOptions().isRandomizeDependentOrigin()) {
            person = newPerson(PersonnelRole.DEPENDENT);
        } else {
            person = newPerson(PersonnelRole.DEPENDENT, PersonnelRole.NONE,
                    new DefaultFactionSelector(getCampaignOptions().getRandomOriginOptions()),
                    new DefaultPlanetSelector(getCampaignOptions().getRandomOriginOptions()),
                    Gender.RANDOMIZE);
        }

        return person;
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given
     * in the CampaignOptions
     *
     * @param role The primary role
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole role) {
        return newPerson(role, PersonnelRole.NONE);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given
     * in the CampaignOptions
     *
     * @param primaryRole The primary role
     * @param secondaryRole A secondary role
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole) {
        return newPerson(primaryRole, secondaryRole, getFactionSelector(), getPlanetSelector(),
                Gender.RANDOMIZE);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given
     * in the CampaignOptions
     *
     * @param primaryRole The primary role
     * @param factionCode The code for the faction this person is to be generated from
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final String factionCode,
                            final Gender gender) {
        return newPerson(primaryRole, PersonnelRole.NONE,
                new DefaultFactionSelector(getCampaignOptions().getRandomOriginOptions(),
                        (factionCode == null) ? null : Factions.getInstance().getFaction(factionCode)),
                getPlanetSelector(), gender);
    }

    /**
     * Generate a new Person of the given role using whatever randomization options have been given
     * in the CampaignOptions
     *
     * @param primaryRole The primary role
     * @param secondaryRole A secondary role
     * @param factionSelector The faction selector to use for the person.
     * @param planetSelector The planet selector for the person.
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
                            final AbstractFactionSelector factionSelector,
                            final AbstractPlanetSelector planetSelector, final Gender gender) {
        return newPerson(primaryRole, secondaryRole,
                getPersonnelGenerator(factionSelector, planetSelector), gender);
    }

    /**
     * Generate a new {@link Person} of the given role, using the supplied {@link AbstractPersonnelGenerator}
     * @param primaryRole The primary role of the {@link Person}.
     * @param personnelGenerator The {@link AbstractPersonnelGenerator} to use when creating the {@link Person}.
     * @return A new {@link Person} configured using {@code personnelGenerator}.
     */
    public Person newPerson(final PersonnelRole primaryRole,
                            final AbstractPersonnelGenerator personnelGenerator) {
        return newPerson(primaryRole, PersonnelRole.NONE, personnelGenerator, Gender.RANDOMIZE);
    }

    /**
     * Generate a new {@link Person} of the given role, using the supplied {@link AbstractPersonnelGenerator}
     * @param primaryRole The primary role of the {@link Person}.
     * @param secondaryRole The secondary role of the {@link Person}.
     * @param personnelGenerator The {@link AbstractPersonnelGenerator} to use when creating the {@link Person}.
     * @param gender The gender of the person to be generated, or a randomize it value
     * @return A new {@link Person} configured using {@code personnelGenerator}.
     */
    public Person newPerson(final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
                            final AbstractPersonnelGenerator personnelGenerator,
                            final Gender gender) {
        final Person person = personnelGenerator.generate(this, primaryRole, secondaryRole, gender);

        // Assign a random portrait after we generate a new person
        if (getCampaignOptions().isUsePortraitForRole(primaryRole)) {
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
        return recruitPerson(p, p.getPrisonerStatus(), false, true);
    }

    /**
     * @param p         the person being added
     * @param gmAdd     false means that they need to pay to hire this person, provided that
     *                  the campaign option to pay for new hires is set, while
     *                  true means they are added without paying
     * @return          true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, boolean gmAdd) {
        return recruitPerson(p, p.getPrisonerStatus(), gmAdd, true);
    }

    /**
     *
     * @param p              the person being added
     * @param prisonerStatus the person's prisoner status upon recruitment
     * @return               true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, PrisonerStatus prisonerStatus) {
        return recruitPerson(p, prisonerStatus, false, true);
    }

    /**
     * @param p              the person being added
     * @param prisonerStatus the person's prisoner status upon recruitment
     * @param gmAdd          false means that they need to pay to hire this person, true means it is added without paying
     * @param log            whether or not to write to logs
     * @return               true if the person is hired successfully, otherwise false
     */
    public boolean recruitPerson(Person p, PrisonerStatus prisonerStatus, boolean gmAdd, boolean log) {
        if (p == null) {
            return false;
        }
        // Only pay if option set, they weren't GM added, and they aren't a dependent, prisoner or bondsman
        if (getCampaignOptions().isPayForRecruitment() && !p.getPrimaryRole().isDependent()
                && !gmAdd && prisonerStatus.isFree()) {
            if (!getFinances().debit(TransactionType.RECRUITMENT, getLocalDate(),
                    p.getSalary(this).multipliedBy(2), "Recruitment of " + p.getFullName())) {
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

        if (p.getPrimaryRole().isAstech()) {
            astechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
            astechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
        } else if (p.getSecondaryRole().isAstech()) {
            astechPoolMinutes += Person.SECONDARY_ROLE_SUPPORT_TIME;
            astechPoolOvertime += Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME;
        }

        p.setPrisonerStatus(this, prisonerStatus, log);

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
        // if person is non-clan or does not have a phenotype
        if (!person.isClanPersonnel() || person.getPhenotype().isNone()) {
            return;
        }

        // Person already has a bloodname, we open up the dialog to ask if they want to keep the
        // current bloodname or assign a new one
        if (!person.getBloodname().isEmpty()) {
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
                        case GROUND_VEHICLE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE)
                                    ? person.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case NAVAL_VEHICLE_DRIVER:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE)
                                    ? person.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VTOL_PILOT:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL)
                                    ? person.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL;
                            break;
                        default:
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
                        case VESSEL_PILOT:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_PILOT_SPACE)
                                    ? person.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_GUNNER:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_GUN_SPACE)
                                    ? person.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_CREW:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_TECH_VESSEL)
                                    ? person.getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_NAVIGATOR:
                            bloodnameTarget += 2 * (person.hasSkill(SkillType.S_NAV)
                                    ? person.getSkill(SkillType.S_NAV).getFinalSkillValue()
                                    : TargetRoll.AUTOMATIC_FAIL);
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
            bloodnameTarget += Math.min(0, getRankSystem().getOfficerCut() - person.getRankNumeric());
        }

        if (ignoreDice || (Compute.d6(2) >= bloodnameTarget)) {
            final Phenotype phenotype = person.getPhenotype().isNone() ? Phenotype.GENERAL : person.getPhenotype();

            final Bloodname bloodname = Bloodname.randomBloodname(
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

    public @Nullable Person getPerson(final UUID id) {
        return personnel.get(id);
    }

    public Collection<Person> getPersonnel() {
        return personnel.values();
    }

    /**
     * Provides a filtered list of personnel including only active Persons.
     * @return a {@link Person} <code>List</code> containing all active personnel
     */
    public List<Person> getActivePersonnel() {
        return getPersonnel().stream()
                .filter(p -> p.getStatus().isActive())
                .collect(Collectors.toList());
    }
    //endregion Other Personnel Methods

    //region Personnel Selectors and Generators
    /**
     * Gets the {@link AbstractFactionSelector} to use with this campaign.
     * @return An {@link AbstractFactionSelector} to use when selecting a {@link Faction}.
     */
    public AbstractFactionSelector getFactionSelector() {
        return getFactionSelector(getCampaignOptions().getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractFactionSelector} to use
     * @param options the random origin options to use
     * @return An {@link AbstractFactionSelector} to use when selecting a {@link Faction}.
     */
    public AbstractFactionSelector getFactionSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedFactionSelector(options)
                : new DefaultFactionSelector(options);
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use with this campaign.
     * @return An {@link AbstractPlanetSelector} to use when selecting a {@link Planet}.
     */
    public AbstractPlanetSelector getPlanetSelector() {
        return getPlanetSelector(getCampaignOptions().getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use
     * @param options the random origin options to use
     * @return An {@link AbstractPlanetSelector} to use when selecting a {@link Planet}.
     */
    public AbstractPlanetSelector getPlanetSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedPlanetSelector(options)
                : new DefaultPlanetSelector(options);
    }

    /**
     * Gets the {@link AbstractPersonnelGenerator} to use with this campaign.
     * @param factionSelector The {@link AbstractFactionSelector} to use when choosing a {@link Faction}.
     * @param planetSelector The {@link AbstractPlanetSelector} to use when choosing a {@link Planet}.
     * @return An {@link AbstractPersonnelGenerator} to use when creating new personnel.
     */
    public AbstractPersonnelGenerator getPersonnelGenerator(
            final AbstractFactionSelector factionSelector,
            final AbstractPlanetSelector planetSelector) {
        final DefaultPersonnelGenerator generator = new DefaultPersonnelGenerator(factionSelector, planetSelector);
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
                    || (getCampaignOptions().isUseAdvancedMedical() && p.hasInjuries(true) && p.getStatus().isActive())) {
                patients.add(p);
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
        // Skip out on "not armor" (as in 0 point armer on men or field guns)
        if ((p instanceof Armor) && ((Armor) p).getType() == EquipmentType.T_ARMOR_UNKNOWN) {
            return null;
        }
        // Makes no sense buying those separately from the chasis
        if ((p instanceof EquipmentPart)
                && ((EquipmentPart) p).getType() != null
                && (((EquipmentPart) p).getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION))) {
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
     * Finds the active person in a particular role with the highest level in a
     * given, with an optional secondary skill to break ties.
     *
     * @param role One of the PersonnelRole enum values
     * @param primary The skill to use for comparison.
     * @param secondary
     *            If not null and there is more than one person tied for the most
     *            the highest, preference will be given to the one with a higher
     *            level in the secondary skill.
     * @return The person in the designated role with the most experience.
     */
    public Person findBestInRole(PersonnelRole role, String primary, String secondary) {
        int highest = 0;
        Person retVal = null;
        for (Person p : getActivePersonnel()) {
            if (((p.getPrimaryRole() == role) || (p.getSecondaryRole() == role)) && (p.getSkill(primary) != null)) {
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

    public Person findBestInRole(PersonnelRole role, String skill) {
        return findBestInRole(role, skill, null);
    }

    /**
     * @return The list of all active {@link Person}s who qualify as technicians ({@link Person#isTech()}));
     */
    public List<Person> getTechs() {
        return getTechs(false);
    }

    public List<Person> getTechs(final boolean noZeroMinute) {
        return getTechs(noZeroMinute, false);
    }

    /**
     * Returns a list of active technicians.
     *
     * @param noZeroMinute If TRUE, then techs with no time remaining will be excluded from the list.
     * @param eliteFirst If TRUE and sorted also TRUE, then return the list sorted from best to worst
     * @return The list of active {@link Person}s who qualify as technicians ({@link Person#isTech()}).
     */
    public List<Person> getTechs(final boolean noZeroMinute, final boolean eliteFirst) {
        final List<Person> techs = getActivePersonnel().stream()
                .filter(person -> person.isTech() && (!noZeroMinute || (person.getMinutesLeft() > 0)))
                .collect(Collectors.toList());

        // also need to loop through and collect engineers on self-crewed vessels
        for (final Unit unit : getUnits()) {
            if (unit.isSelfCrewed() && !(unit.getEntity() instanceof Infantry) && (unit.getEngineer() != null)) {
                techs.add(unit.getEngineer());
            }
        }

        // Return the tech collection sorted worst to best Skill Level, or reversed if we want
        // elites first
        Comparator<Person> techSorter = Comparator.comparingInt(person ->
                person.getSkillLevel(this, !person.getPrimaryRole().isTech()
                        && person.getSecondaryRole().isTechSecondary()).ordinal());

        if (eliteFirst) {
            techSorter = techSorter.reversed().thenComparing(Comparator
                    .comparingInt(Person::getDailyAvailableTechTime).reversed());
        } else {
            techSorter = techSorter.thenComparing(Comparator.comparingInt(Person::getMinutesLeft).reversed());
        }

        techSorter = techSorter.thenComparing(new PersonTitleSorter());

        if (techs.size() > 1) {
            techs.subList(1, techs.size()).sort(techSorter);
        }

        return techs;
    }

    public List<Person> getAdmins() {
        List<Person> admins = new ArrayList<>();
        for (Person p : getActivePersonnel()) {
            if (p.isAdministrator()) {
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
        if (getCampaignOptions().isUseAdvancedMedical()) {
            return "";
        }
        String report = "";
        report += doctor.getHyperlinkedFullTitle() + " attempts to heal "
                + medWork.getFullName();
        TargetRoll target = getTargetFor(medWork, doctor);
        int roll = Compute.d6(2);
        report = report + ",  needs " + target.getValueAsString()
                + " and rolls " + roll + ':';
        int xpGained = 0;
        // If we get a natural 2 that isn't an automatic success, reroll if Edge is available and in use.
        if (getCampaignOptions().isUseSupportEdge()
                && doctor.getOptions().booleanOption(PersonnelOptions.EDGE_MEDICAL)) {
            if ((roll == 2) && (doctor.getCurrentEdge() > 0) && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
                doctor.changeCurrentEdge(-1);
                roll = Compute.d6(2);
                report += medWork.fail() + '\n' + doctor.getHyperlinkedFullTitle() + " uses Edge to reroll:"
                        + " rolls " + roll + ':';
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
            doctor.awardXP(this, xpGained);
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
                && !(getCampaignOptions().isUseAdvancedMedical() && medWork.needsAMFixing())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    medWork.getFullName() + " does not require healing.");
        }
        if (getPatientsFor(doctor) > 25) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getFullName()
                    + " already has 25 patients.");
        }
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(), skill.getSkillLevel().toString());
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }
        // understaffed mods
        int helpMod = getShorthandedMod(getMedicsPerDoctor(), true);
        if (helpMod > 0) {
            target.addModifier(helpMod, "shorthanded");
        }
        target.append(medWork.getHealingMods(this));
        return target;
    }

    public @Nullable Person getLogisticsPerson() {
        int bestSkill = -1;
        int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
        Person admin = null;
        String skill = getCampaignOptions().getAcquisitionSkill();
        if (skill.equals(CampaignOptions.S_AUTO)) {
            return null;
        } else if (skill.equals(CampaignOptions.S_TECH)) {
            for (Person p : getActivePersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(true)) {
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
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(true)) {
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
     * @return A <code>List</code> of {@link Person} who can perform logistical actions.
     */
    public List<Person> getLogisticsPersonnel() {
        String skill = getCampaignOptions().getAcquisitionSkill();
        if (skill.equals(CampaignOptions.S_AUTO)) {
            return Collections.emptyList();
        } else {
            List<Person> logisticsPersonnel = new ArrayList<>();
            int maxAcquisitions = getCampaignOptions().getMaxAcquisitions();
            for (Person p : getActivePersonnel()) {
                if (getCampaignOptions().isAcquisitionSupportStaffOnly() && !p.hasSupportRole(true)) {
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
        for (IAcquisitionWork shoppingItem : sList.getShoppingList()) {
            shoppingItem.decrementDaysToWait();
        }

        if (getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            return goShoppingAutomatically(sList);
        } else if (!getCampaignOptions().isUsePlanetaryAcquisition()) {
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
        List<IAcquisitionWork> currentList = sList.getShoppingList();
        LocalDate currentDate = getLocalDate();

        // a list of items than can be taken out of the search and put back on the
        // shopping list
        List<IAcquisitionWork> shelvedItems = new ArrayList<>();

        // find planets within a certain radius - the function will weed out dead planets
        List<PlanetarySystem> systems = Systems.getInstance().getShoppingSystems(getCurrentSystem(),
                getCampaignOptions().getMaxJumpsPlanetaryAcquisition(), currentDate);

        for (Person person : logisticsPersonnel) {
            if (currentList.isEmpty()) {
                // Nothing left to shop for!
                break;
            }

            String personTitle = person.getHyperlinkedFullTitle() + " ";

            for (PlanetarySystem system : systems) {
                if (currentList.isEmpty()) {
                    // Nothing left to shop for!
                    break;
                }

                List<IAcquisitionWork> remainingItems = new ArrayList<>();

                // loop through shopping list. If its time to check, then check as appropriate. Items not
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
     * @param acquisition - An <code>IAcquisitionWork</code> object
     * @return true if the campaign can pay for the acquisition; false if it cannot.
     */
    public boolean canPayFor(IAcquisitionWork acquisition) {
        // SHOULD we check to see if this acquisition needs to be paid for
        if ( (acquisition instanceof UnitOrder && getCampaignOptions().isPayForUnits())
                ||(acquisition instanceof Part && getCampaignOptions().isPayForParts()) ) {
            // CAN the acquisition actually be paid for
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
    public PartAcquisitionResult findContactForAcquisition(IAcquisitionWork acquisition, Person person, PlanetarySystem system) {
        TargetRoll target = getTargetForAcquisition(acquisition, person);

        String impossibleSentencePrefix = person == null ? "Can't search for " : person.getFullName() + " can't search for ";
        String failedSentencePrefix = person == null ? "No contacts available for " : person.getFullName() + " is unable to find contacts for ";
        String succeededSentencePrefix = person == null ? "Possible contact for " : person.getFullName() + " has found a contact for ";

        // if it's already impossible, don't bother with the rest
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport("<font color='red'><b>" + impossibleSentencePrefix + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + " because:</b></font> " + target.getDesc());
            }
            return PartAcquisitionResult.PartInherentFailure;
        }

        target = system.getPrimaryPlanet().getAcquisitionMods(target, getLocalDate(), getCampaignOptions(), getFaction(),
                acquisition.getTechBase() == Part.T_CLAN);

        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport("<font color='red'><b>" + impossibleSentencePrefix + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + " because:</b></font> " + target.getDesc());
            }
            return PartAcquisitionResult.PlanetSpecificFailure;
        }
        if (Compute.d6(2) < target.getValue()) {
            // no contacts on this planet, move along
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport("<font color='red'><b>" + failedSentencePrefix + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + "</b></font>");
            }
            return PartAcquisitionResult.PlanetSpecificFailure;
        } else {
            if (getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport("<font color='green'>" + succeededSentencePrefix + acquisition.getAcquisitionName()
                        + " on " + system.getPrintableName(getLocalDate()) + "</font>");
            }
            return PartAcquisitionResult.Success;
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

        TargetRoll target = getTargetForAcquisition(acquisition, person);

        // check on funds
        if (!canPayFor(acquisition)) {
            target.addModifier(TargetRoll.IMPOSSIBLE, "Cannot afford this purchase");
        }

        if (null != system) {
            target = system.getPrimaryPlanet().getAcquisitionMods(target, getLocalDate(),
                    getCampaignOptions(), getFaction(), acquisition.getTechBase() == Part.T_CLAN);
        }
        report += "attempts to find " + acquisition.getAcquisitionName();

        // if impossible then return
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            report += ":<font color='red'><b> " + target.getDesc() + "</b></font>";
            if (!getCampaignOptions().isUsePlanetaryAcquisition() || getCampaignOptions().isPlanetAcquisitionVerbose()) {
                addReport(report);
            }
            return false;
        }


        int roll = Compute.d6(2);
        report += "  needs " + target.getValueAsString();
        report += " and rolls " + roll + ":";
        // Edge reroll, if applicable
        if (getCampaignOptions().isUseSupportEdge() && (roll < target.getValue()) && (person != null)
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
            LogManager.getLogger().warn("Unit is already mothballed, cannot mothball.");
            return;
        }

        Person tech = u.getTech();
        if (null == tech) {
            // uh-oh
            addReport("No tech assigned to the mothballing of " + u.getHyperlinkedName());
            return;
        }

        // don't allow overtime minutes for mothballing because its cheating
        // since you don't roll
        int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());

        // check astech time
        if (!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
            // uh-oh
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
            LogManager.getLogger().warn("Unit is already activated, cannot activate.");
            return;
        }

        Person tech = u.getTech();
        if (null == tech) {
            // uh-oh
            addReport("No tech assigned to the activation of " + u.getHyperlinkedName());
            return;
        }

        // don't allow overtime minutes for activation because its cheating
        // since you don't roll
        int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());

        // check astech time
        if (!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
            // uh-oh
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
                if (getCampaignOptions().isUseSupportEdge() && (roll < target.getValue())
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
        // if we fail and would break a part, here's a chance to use Edge for a reroll...
        if (getCampaignOptions().isUseSupportEdge()
                && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART)
                && (tech.getCurrentEdge() > 0)
                && (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
            if ((getCampaignOptions().isDestroyByMargin()
                    && (getCampaignOptions().getDestroyMargin() <= (target.getValue() - roll)))
                    || (!getCampaignOptions().isDestroyByMargin()
                            // if an elite, primary tech and destroy by margin is NOT on
                            && ((tech.getExperienceLevel(this, false) == SkillType.EXP_ELITE)
                                    || tech.getPrimaryRole().isVehicleCrew())) // For vessel crews
                    && (roll < target.getValue())) {
                tech.changeCurrentEdge(-1);
                roll = tech.isRightTechTypeFor(partWork) ? Compute.d6(2) : Utilities.roll3d6();
                // This is needed to update the edge values of individual crewmen
                if (tech.isEngineer()) {
                    tech.setEdgeUsed(tech.getEdgeUsed() + 1);
                }
                report += " <b>failed!</b> and would destroy the part, but uses Edge to reroll...getting a " + roll + ":";
            }
        }

        if (roll >= target.getValue()) {
            report = report + partWork.succeed();
            if (getCampaignOptions().isPayForRepairs()
                    && action.equals(" fix ")
                    && !(partWork instanceof Armor)) {
                Money cost = ((Part) partWork).getActualValue().multipliedBy(0.2);
                report += "<br>Repairs cost " +
                        cost.toAmountAndSymbolString() +
                        " worth of parts.";
                finances.debit(TransactionType.REPAIRS, getLocalDate(), cost,
                        "Repair of " + partWork.getPartName());
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
            tech.awardXP(this, xpGained);
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
     * @param contract an active AtBContract
     * @return the current deployment deficit for the contract
     */
    public int getDeploymentDeficit(AtBContract contract) {
        if (!contract.isActiveOn(getLocalDate()) || contract.getStartDate().isEqual(getLocalDate())) {
            // Do not check for deficits if the contract has not started or
            // it is the first day of the contract, as players won't have
            // had time to assign forces to the contract yet
            return 0;
        }

        int total = -contract.getRequiredLances();
        int role = -Math.max(1, contract.getRequiredLances() / 2);

        final AtBLanceRole requiredLanceRole = contract.getContractType().getRequiredLanceRole();
        for (Lance l : lances.values()) {
            if (!l.getRole().isUnassigned() && (l.getMissionId() == contract.getId())) {
                total++;
                if (l.getRole() == requiredLanceRole) {
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
            if (!getLocation().isOnPlanet() && !getLocation().getJumpPath().isEmpty()
                    && getLocation().getJumpPath().getLastSystem().getId().equals(contract.getSystemId())) {
                // transitTime is measured in days; so we round up to the next whole day
                contract.setStartAndEndDate(getLocalDate().plusDays((int) Math.ceil(getLocation().getTransitTime())));
                addReport("The start and end dates of " + contract.getName() + " have been shifted to reflect the current ETA.");
                continue;
            }

            if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                int deficit = getDeploymentDeficit(contract);
                if (deficit > 0) {
                    contract.addPlayerMinorBreaches(deficit);
                    addReport("Failure to meet " + contract.getName() + " requirements resulted in " + deficit
                            + ((deficit == 1) ? " minor contract breach" : " minor contract breaches"));
                }
            }

            for (final Scenario scenario : contract.getCurrentAtBScenarios()) {
                if ((scenario.getDate() != null) && scenario.getDate().isBefore(getLocalDate())) {
                    if (getCampaignOptions().isUseStratCon() && (scenario instanceof AtBDynamicScenario)) {
                        final boolean stub = StratconRulesManager.processIgnoredScenario(
                                (AtBDynamicScenario) scenario, contract.getStratconCampaignState());

                        if (stub) {
                            scenario.convertToStub(this, ScenarioStatus.DEFEAT);
                            addReport("Failure to deploy for " + scenario.getName() + " resulted in defeat.");
                        } else {
                            scenario.clearAllForcesAndPersonnel(this);
                        }
                    } else {
                        scenario.convertToStub(this, ScenarioStatus.DEFEAT);
                        contract.addPlayerMinorBreach();

                        addReport("Failure to deploy for " + scenario.getName()
                            + " resulted in defeat and a minor contract breach.");
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
            for (final AtBScenario s : contract.getCurrentAtBScenarios()) {
                if ((s.getDate() != null) && s.getDate().equals(getLocalDate())) {
                    int forceId = s.getLanceForceId();
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
                                if (u != null) {
                                    u.setScenarioId(s.getId());
                                }
                            }

                            addReport(MessageFormat.format(
                                    resources.getString("atbScenarioTodayWithForce.format"),
                                    s.getName(), forceIds.get(forceId).getName()));
                            MekHQ.triggerEvent(new DeploymentChangedEvent(forceIds.get(forceId), s));
                        } else {
                            addReport(MessageFormat.format(
                                    resources.getString("atbScenarioToday.format"), s.getName()));
                        }
                    } else {
                        addReport(MessageFormat.format(
                                resources.getString("atbScenarioToday.format"), s.getName()));
                    }
                }
            }
        }
    }

    private void processNewDayATBFatigue() {
        boolean inContract = false;
        for (final AtBContract contract : getActiveAtBContracts()) {
            fatigueLevel += contract.getContractType().getFatigue();
            inContract = true;
        }

        if (!inContract && location.isOnPlanet()) {
            fatigueLevel -= 2;
        }
        fatigueLevel = Math.max(fatigueLevel, 0);
        addReport("Your fatigue level is: " + fatigueLevel);
    }

    private void processNewDayATB() {
        contractMarket.generateContractOffers(this); // TODO : AbstractContractMarket : Remove

        if ((getShipSearchExpiration() != null) && !getShipSearchExpiration().isAfter(getLocalDate())) {
            setShipSearchExpiration(null);
            if (getShipSearchResult() != null) {
                addReport("Opportunity for purchase of " + getShipSearchResult() + " has expired.");
                setShipSearchResult(null);
            }
        }

        if (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            processShipSearch();

            // Training Experience - Award to eligible training lances on active contracts
            getLances().values().stream()
                    .filter(lance -> lance.getRole().isTraining()
                            && (lance.getContract(this) != null) && lance.isEligible(this)
                            && lance.getContract(this).isActiveOn(getLocalDate(), true))
                    .forEach(this::awardTrainingXP);
        }

        // Add or remove dependents - only if one of the two options makes this possible is enabled
        if ((getLocalDate().getDayOfYear() == 1)
                && getCampaignOptions().getRandomDependentMethod().isAgainstTheBot()
                && (getCampaignOptions().isUseRandomDependentRemoval() || getCampaignOptions().isUseRandomDependentAddition())) {
            int numPersonnel = 0;
            List<Person> dependents = new ArrayList<>();
            for (Person p : getActivePersonnel()) {
                numPersonnel++;
                if (p.getPrimaryRole().isDependent() && p.getGenealogy().isEmpty()) {
                    dependents.add(p);
                }
            }

            final int roll = MathUtility.clamp(Compute.d6(2) + getUnitRatingMod() - 2, 2, 12);

            int change = numPersonnel * (roll - 5) / 100;
            if (change < 0) {
                if (getCampaignOptions().isUseRandomDependentRemoval()) {
                    while ((change < 0) && !dependents.isEmpty()) {
                        final Person person = ObjectUtility.getRandomItem(dependents);
                        addReport(String.format(resources.getString("dependentLeavesForce.text"),
                                person.getFullTitle()));
                        removePerson(person, false);
                        dependents.remove(person);
                        change++;
                    }
                }
            } else {
                if (getCampaignOptions().isUseRandomDependentAddition()) {
                    for (int i = 0; i < change; i++) {
                        final Person person = newDependent(false);
                        recruitPerson(person, PrisonerStatus.FREE, true, false);
                        addReport(String.format(resources.getString("dependentJoinsForce.text"),
                                person.getFullTitle()));
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

            for (AtBContract contract : getActiveAtBContracts()) {
                contract.checkMorale(getLocalDate(), getUnitRatingMod());
                addReport("Enemy morale is now " + contract.getMoraleLevel()
                        + " on contract " + contract.getName());
            }

            // Account for fatigue
            if (getCampaignOptions().isTrackUnitFatigue()) {
                processNewDayATBFatigue();
            }
        }

        processNewDayATBScenarios();
    }

    public void processNewDayPersonnel() {
        // This MUST use getActivePersonnel as we only want to process active personnel, and
        // furthermore this allows us to add and remove personnel without issue
        for (Person p : getActivePersonnel()) {
            // Death
            if (getDeath().processNewDay(this, getLocalDate(), p)) {
                // The person has died, so don't continue to process the dead
                continue;
            }

            // Marriage
            getMarriage().processNewDay(this, getLocalDate(), p);

            p.resetMinutesLeft();
            // Reset acquisitions made to 0
            p.setAcquisition(0);
            if (p.needsFixing() && !getCampaignOptions().isUseAdvancedMedical()) {
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
            if (getCampaignOptions().isUseAdvancedMedical()) {
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
            if ((p.isAdministrator() || p.isDoctor() || p.isEngineer() || p.isTech())
                    && (getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY)) {
                p.resetCurrentEdge();
            }

            if ((getCampaignOptions().getIdleXP() > 0) && (getLocalDate().getDayOfMonth() == 1)
                    && !p.getPrisonerStatus().isCurrentPrisoner()) { // Prisoners can't gain XP, while Bondsmen can gain xp
                p.setIdleMonths(p.getIdleMonths() + 1);
                if (p.getIdleMonths() >= getCampaignOptions().getMonthsIdleXP()) {
                    if (Compute.d6(2) >= getCampaignOptions().getTargetIdleXP()) {
                        p.awardXP(this, getCampaignOptions().getIdleXP());
                        addReport(p.getHyperlinkedFullTitle() + " has gained "
                                + getCampaignOptions().getIdleXP() + " XP");
                    }
                    p.setIdleMonths(0);
                }
            }

            // Divorce
            getDivorce().processNewDay(this, getLocalDate(), p);

            // Procreation
            getProcreation().processNewDay(this, getLocalDate(), p);
        }
    }

    public void processNewDayUnits() {
        // need to loop through units twice, the first time to do all maintenance and
        // the second time to do whatever else. Otherwise, maintenance minutes might
        // get sucked up by other stuff. This is also a good place to ensure that a
        // unit's engineer gets reset and updated.
        for (Unit u : getUnits()) {
            // do maintenance checks
            try {
                u.resetEngineer();
                if (null != u.getEngineer()) {
                    u.getEngineer().resetMinutesLeft();
                }

                doMaintenance(u);
            } catch (Exception e) {
                LogManager.getLogger().error(String.format(
                        "Unable to perform maintenance on %s (%s) due to an error",
                        u.getName(), u.getId().toString()), e);
                addReport(String.format("ERROR: An error occurred performing maintenance on %s, check the log",
                        u.getName()));
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
                    } catch (Exception e) {
                        LogManager.getLogger().error(String.format(
                                "Could not perform overnight maintenance on %s (%d) due to an error",
                                part.getName(), part.getId()), e);
                        addReport(String.format("ERROR: an error occurred performing overnight maintenance on %s, check the log",
                                part.getName()));
                    }
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
        if (MekHQ.getMHQOptions().getNewDayMRMS()) {
            try {
                MRMSService.mrmsAllUnits(this);
            } catch (Exception ex) {
                LogManager.getLogger().error("Could not perform mass repair/salvage on units due to an error", ex);
                addReport("ERROR: an error occurred performing mass repair/salvage on units, check the log");
            }
        }
    }

    private void processNewDayForces() {
        // Update the force icons based on the end of day unit status if desired
        if (MekHQ.getMHQOptions().getNewDayForceIconOperationalStatus()) {
            getForces().updateForceIconOperationalStatus(this);
        }
    }

    /**
     * @return <code>true</code> if the new day arrived
     */
    public boolean newDay() {
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
        final LocalDate yesterday = getLocalDate();
        setLocalDate(getLocalDate().plus(1, ChronoUnit.DAYS));

        // Determine if we have an active contract or not, as this can get used elsewhere before
        // we actually hit the AtB new day (e.g. personnel market)
        if (getCampaignOptions().isUseAtB()) {
            setHasActiveContract();
        }

        // Clear Reports
        getCurrentReport().clear();
        setCurrentReportHTML("");
        newReports.clear();
        beginReport("<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(getLocalDate()) + "</b>");

        // New Year Changes
        if (getLocalDate().getDayOfYear() == 1) {
            // News is reloaded
            reloadNews();

            // Change Year Game Option
            getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(getGameYear());
        }

        readNews();

        getLocation().newDay(this);

        // Manage the Markets
        getPersonnelMarket().generatePersonnelForDay(this);

        // TODO : AbstractContractMarket : Uncomment
        //getContractMarket().processNewDay(this);
        getUnitMarket().processNewDay(this);

        // Process New Day for AtB
        if (getCampaignOptions().isUseAtB()) {
            processNewDayATB();
        }

        processNewDayPersonnel();

        resetAstechMinutes();

        processNewDayUnits();

        processNewDayForces();

        setShoppingList(goShopping(getShoppingList()));

        // check for anything in finances
        getFinances().newDay(this, yesterday, getLocalDate());

        MekHQ.triggerEvent(new NewDayEvent(this));
        return true;
    }

    public @Nullable Person getFlaggedCommander() {
        for (Person p : getPersonnel()) {
            if (p.isCommander()) {
                return p;
            }
        }
        return null;
    }

    /**
     * return the probable commander. If we find a flagged commander, return that. Otherwise, return person
     * with most senior rank. Ties go to the first in the queue.
     * @return Person object of the commander
     */
    public Person getSeniorCommander() {
        Person commander = null;
        for (Person p : getActivePersonnel()) {
            if (p.isCommander()) {
                return p;
            }
            if (null == commander || p.getRankNumeric() > commander.getRankNumeric()) {
                commander = p;
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
            astechPoolMinutes = Math.max(0, astechPoolMinutes - Person.PRIMARY_ROLE_SUPPORT_TIME);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME);
        } else if (person.getSecondaryRole().isAstech()) {
            astechPoolMinutes = Math.max(0, astechPoolMinutes - Person.SECONDARY_ROLE_SUPPORT_TIME);
            astechPoolOvertime = Math.max(0, astechPoolOvertime - Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME);
        }
        MekHQ.triggerEvent(new PersonRemovedEvent(person));
    }

    /**
     * Awards XP to the lance based on the maximum experience level of its commanding officer and
     * the minimum experience level of the unit's members.
     * @param l The {@link Lance} to calculate XP to award for training.
     */
    private void awardTrainingXP(final Lance l) {
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
                int commanderExperience = Math.max(commander.getExperienceLevel(this, false),
                        commander.getExperienceLevel(this, true));
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
                            int experienceLevel = Math.min(p.getExperienceLevel(this, false),
                                    !p.getSecondaryRole().isNone()
                                            ? p.getExperienceLevel(this, true)
                                            : SkillType.EXP_ELITE);
                            if (experienceLevel >= 0 && experienceLevel < SkillType.EXP_REGULAR) {
                                // ...add one XP.
                                p.awardXP(this, 1);
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

    public void removeScenario(final Scenario scenario) {
        scenario.clearAllForcesAndPersonnel(this);
        final Mission mission = getMission(scenario.getMissionId());
        if (mission != null) {
            mission.getScenarios().remove(scenario);

            // run through the stratcon campaign state where applicable and remove the "parent" scenario as well
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

        if (campaignOptions.isUseAtB()) {
            lances.remove(fid);
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

        ArrayList<Force> subs = new ArrayList<>(force.getSubForces());
        for (Force sub : subs) {
            removeForce(sub);
            MekHQ.triggerEvent(new OrganizationChangedEvent(sub));
        }
    }

    public void removeUnitFromForce(Unit u) {
        Force force = getForce(u.getForceId());
        if (null != force) {
            force.removeUnit(this, u.getId(), true);
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


            if (campaignOptions.isUseAtB() && force.getUnits().isEmpty()) {
                lances.remove(force.getId());
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
            return forceIds.values().stream()
                    .filter(force -> person.getId().equals(force.getTechID()))
                    .findFirst().orElse(null);
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
        for (Person p : personnel.values()) {
            if (p.getGenealogy().hasSpouse()) {
                if (!personnel.containsKey(p.getGenealogy().getSpouse().getId())) {
                    p.getGenealogy().setSpouse(null);
                    if (!getCampaignOptions().isKeepMarriedNameUponSpouseDeath()
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

    public void setFaction(final Faction faction) {
        setFactionDirect(faction);
        updateTechFactionCode();
    }

    public void setFactionDirect(final Faction faction) {
        this.faction = faction;
    }

    @Deprecated // Use Campaign::getFaction::getShortName instead
    public String getFactionCode() {
        return getFaction().getShortName();
    }

    @Deprecated // Use Campaign::setFaction instead
    public void setFactionCode(final String factionCode) {
        setFaction(Factions.getInstance().getFaction(factionCode));
    }

    public Faction getRetainerEmployer() {
        return Factions.getInstance().getFaction(getRetainerEmployerCode());
    }

    public String getRetainerEmployerCode() {
        return retainerEmployerCode;
    }

    public void setRetainerEmployerCode(String code) {
        retainerEmployerCode = code;
    }

    private void addInMemoryLogHistory(LogEntry le) {
        if (!inMemoryLogHistory.isEmpty()) {
            while (ChronoUnit.DAYS.between(inMemoryLogHistory.get(0).getDate(), le.getDate()) > MHQConstants.MAX_HISTORICAL_LOG_DAYS) {
                // we've hit the max size for the in-memory based on the UI display limit prune the oldest entry
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
        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            // add the new items to our in-memory cache
            addInMemoryLogHistory(new HistoricalLogEntry(getLocalDate(), ""));
        }
        addReportInternal(r);
    }

    /**
     * Formats and then adds a report to the daily log
     * @param format
     * @param objects
     */
    public void addReport(final String format, final Object... objects) {
        addReport(String.format(format, objects));
    }

    /**
     * Adds a report to the daily log
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

    public void addFunds(final Money quantity) {
        addFunds(TransactionType.MISCELLANEOUS, quantity, null);
    }

    public void addFunds(final TransactionType type, final Money quantity,
                         @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }

        finances.credit(type, getLocalDate(), quantity, description);
        String quantityString = quantity.toAmountAndSymbolString();
        addReport("Funds added : " + quantityString + " (" + description + ")");
    }

    public void removeFunds(final TransactionType type, final Money quantity,
                         @Nullable String description) {
        if ((description == null) || description.isEmpty()) {
            description = "Rich Uncle";
        }

        finances.debit(type, getLocalDate(), quantity, description);
        String quantityString = quantity.toAmountAndSymbolString();
        addReport("Funds removed : " + quantityString + " (" + description + ")");
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
        return new ArrayList<String>();
    }

    public void writeToXML(final PrintWriter pw) {
        int indent = 0;

        // File header
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Start the XML root.
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "campaign", "version", MHQConstants.VERSION);

        //region Basic Campaign Info
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "info");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "faction", getFaction().getShortName());
        if (retainerEmployerCode != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "retainerEmployerCode", retainerEmployerCode);
        }

        getRankSystem().writeToXML(pw, indent, false);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overtime", overtime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gmMode", gmMode);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "astechPool", astechPool);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "astechPoolMinutes", astechPoolMinutes);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "astechPoolOvertime", astechPoolOvertime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "medicPool", medicPool);
        getCamouflage().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "colour", getColour().name());
        getUnitIcon().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastForceId", lastForceId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastMissionId", lastMissionId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastScenarioId", lastScenarioId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "calendar", getLocalDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigueLevel", fatigueLevel);

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "nameGen");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "faction", RandomNameGenerator.getInstance().getChosenFaction());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percentFemale", RandomGenderGenerator.getPercentFemale());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "nameGen");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "currentReport");
        for (String s : currentReport) {
            // This cannot use the MHQXMLUtility as it cannot be escaped
            pw.println(MHQXMLUtility.indentStr(indent) + "<reportLine><![CDATA[" + s + "]]></reportLine>");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "currentReport");

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "info");
        //endregion Basic Campaign Info

        //region Campaign Options
        if (getCampaignOptions() != null) {
            getCampaignOptions().writeToXml(pw, indent);
        }
        //endregion Campaign Options

        // Lists of objects:
        units.writeToXML(pw, indent, "units"); // Units

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "personnel");
        for (final Person person : getPersonnel()) {
            person.writeToXML(pw, indent, this);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "personnel");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "missions");
        for (final Mission mission : getMissions()) {
            mission.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "missions");

        // the forces structure is hierarchical, but that should be handled
        // internally from with writeToXML function for Force
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "forces");
        forces.writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "forces");
        finances.writeToXML(pw, indent);
        location.writeToXML(pw, indent);
        shoppingList.writeToXML(pw, indent);

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "kills");
        for (List<Kill> kills : kills.values()) {
            for (Kill k : kills) {
                k.writeToXML(pw, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "kills");
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skillTypes");
        for (final String skillName : SkillType.skillList) {
            final SkillType type = SkillType.getType(skillName);
            if (type != null) {
                type.writeToXML(pw, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skillTypes");
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "specialAbilities");
        for (String key : SpecialAbility.getSpecialAbilities().keySet()) {
            SpecialAbility.getAbility(key).writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "specialAbilities");
        rskillPrefs.writeToXML(pw, indent);

        // parts is the biggest so it goes last
        parts.writeToXML(pw, indent, "parts"); // Parts

        getGameOptions().writeToXML(pw, indent);

        //current story arc
        if (null != storyArc) {
            storyArc.writeToXml(pw, indent);
        }

        // Markets
        getPersonnelMarket().writeToXML(pw, indent, this);

        // TODO : AbstractContractMarket : Uncomment
        // CAW: implicit DEPENDS-ON to the <missions> and <campaignOptions> node, do not move this above it
        //getContractMarket().writeToXML(pw, indent);

        // Windchild: implicit DEPENDS-ON to the <campaignOptions> node, do not move this above it
        getUnitMarket().writeToXML(pw, indent);

        // Against the Bot
        if (getCampaignOptions().isUseAtB()) {
            // TODO : AbstractContractMarket : Remove next two lines
            // CAW: implicit DEPENDS-ON to the <missions> node, do not move this above it
            contractMarket.writeToXML(pw, indent);

            if (!lances.isEmpty())   {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "lances");
                for (Lance l : lances.values()) {
                    if (forceIds.containsKey(l.getForceId())) {
                        l.writeToXML(pw, indent);
                    }
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "lances");
            }
            retirementDefectionTracker.writeToXML(pw, indent);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shipSearchStart", getShipSearchStart());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shipSearchType", shipSearchType);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shipSearchResult", shipSearchResult);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shipSearchExpiration", getShipSearchExpiration());
        }

        // Customised planetary events
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "customPlanetaryEvents");
        for (PlanetarySystem psystem : Systems.getInstance().getSystems().values()) {
            // first check for system-wide events
            List<PlanetarySystemEvent> customSysEvents = new ArrayList<>();
            for (PlanetarySystemEvent event : psystem.getEvents()) {
                if (event.custom) {
                    customSysEvents.add(event);
                }
            }
            boolean startedSystem = false;
            if (!customSysEvents.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "system");
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", psystem.getId());
                for (PlanetarySystemEvent event : customSysEvents) {
                    Systems.getInstance().writePlanetarySystemEvent(pw, event);
                    pw.println();
                }
                startedSystem = true;
            }
            // now check for planetary events
            for (Planet p : psystem.getPlanets()) {
                List<PlanetaryEvent> customEvents = p.getCustomEvents();
                if (!customEvents.isEmpty()) {
                    if (!startedSystem) {
                        // only write this if we haven't already started the system
                        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "system");
                        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", psystem.getId());
                    }
                    MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "planet");
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sysPos", p.getSystemPosition());
                    for (PlanetaryEvent event : customEvents) {
                        Systems.getInstance().writePlanetaryEvent(pw, event);
                        pw.println();
                    }
                    MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "planet");
                    startedSystem = true;
                }
            }

            if (startedSystem) {
                //close the system
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "system");
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "customPlanetaryEvents");

        if (MekHQ.getMHQOptions().getWriteCustomsToXML()) {
            writeCustoms(pw);
        }

        // Okay, we're done.
        // Close everything out and be done with it.
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "campaign");
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
                LogManager.getLogger().error("", ex);
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

    //region Ranks
    public RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final @Nullable RankSystem rankSystem) {
        // If they are the same object, there hasn't been a change and thus don't need to process further
        if (getRankSystem() == rankSystem) {
            return;
        }

        // Then, we need to validate the rank system. Null isn't valid to be set but may be the
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
        getPersonnel().stream().filter(person -> person.getRankSystem().equals(oldRankSystem))
                .forEach(person -> person.setRankSystem(rankValidator, rankSystem));
    }

    public void setRankSystemDirect(final RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }
    //endregion Ranks

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
    public @Nullable JumpPath calculateJumpPath(PlanetarySystem start, PlanetarySystem end) {
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
    public Money calculateCostPerJump(boolean excludeOwnTransports, boolean campaignOpsCosts) {
        HangarStatistics stats = getHangarStatistics();
        CargoStatistics cargoStats = getCargoStatistics();

        Money collarCost = Money.of(campaignOpsCosts ? 100000 : 50000);

        // first we need to get the total number of units by type
        int nMech = stats.getNumberOfUnitsByType(Entity.ETYPE_MECH);
        int nLVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int nHVee = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int nAero = stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACEFIGHTER);
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
        int noASF = Math.max(nAero - stats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER), 0);
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

        Force force = getForceFor(p);
        if (force != null) {
            force.updateCommander(this);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(p));
    }

    public TargetRoll getTargetFor(final IPartWork partWork, final Person tech) {
        final Skill skill = tech.getSkillForWorkingOn(partWork);
        int modePenalty = partWork.getMode().expReduction;

        if ((partWork.getUnit() != null) && !partWork.getUnit().isAvailable(partWork instanceof Refit)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "This unit is not currently available!");
        } else if ((partWork.getTech() != null) && !partWork.getTech().equals(tech)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Already being worked on by another team");
        } else if (skill == null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Assigned tech does not have the right skills");
        } else if (!getCampaignOptions().isDestroyByMargin()
                && (partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty))) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is beyond this tech's skill level");
        } else if (partWork.getSkillMin() > SkillType.EXP_ELITE) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
        } else if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
        } else if ((partWork instanceof MissingPart)
                && (((MissingPart) partWork).findReplacement(false) == null)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Replacement part not available.");
        }

        final int techTime = isOvertimeAllowed() ? tech.getMinutesLeft() + tech.getOvertimeLeft()
                : tech.getMinutesLeft();
        if (!(partWork instanceof Refit) && (techTime <= 0)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "The tech has no time left.");
        }

        final String notFixable = partWork.checkFixable();
        if (notFixable != null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, notFixable);
        }

        // if this is an infantry refit, then automatic success
        if ((partWork instanceof Refit) && (partWork.getUnit() != null)
                && partWork.getUnit().isConventionalInfantry()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "infantry refit");
        }

        // If we are using the MoF rule, then we will ignore mode penalty here
        // and instead assign it as a straight penalty
        if (getCampaignOptions().isDestroyByMargin()) {
            modePenalty = 0;
        }

        // this is ugly, if the mode penalty drops you to green, you drop two
        // levels instead of two
        int value = skill.getFinalSkillValue() + modePenalty;
        if ((modePenalty > 0)
                && (SkillType.EXP_GREEN == (skill.getExperienceLevel() - modePenalty))) {
            value++;
        }
        final TargetRoll target = new TargetRoll(value,
                SkillType.getExperienceLevelName(skill.getExperienceLevel() - modePenalty));
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllMods(tech));

        if (getCampaignOptions().isUseEraMods()) {
            target.addModifier(getFaction().getEraMod(getGameYear()), "era");
        }

        final boolean isOvertime;
        if (isOvertimeAllowed()
                && (tech.isTaskOvertime(partWork) || partWork.hasWorkedOvertime())) {
            target.addModifier(3, "overtime");
            isOvertime = true;
        } else {
            isOvertime = false;
        }

        final int minutes = Math.min(partWork.getTimeLeft(), techTime);
        if (minutes <= 0) {
            LogManager.getLogger().error("Attempting to get the target number for a part with zero time left.");
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

    public TargetRoll getTargetForMaintenance(IPartWork partWork, Person tech) {
        int value = 10;
        String skillLevel = "Unmaintained";
        if (null != tech) {
            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (null != skill) {
                value = skill.getFinalSkillValue();
                skillLevel = skill.getSkillLevel().toString();
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
            final int helpMod;
            if (partWork.getUnit().isSelfCrewed()) {
                helpMod = getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
            } else {
                helpMod = getShorthandedMod(partWork.getUnit().getAstechsMaintained(), false);
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

    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition,
                                              final @Nullable Person person) {
        return getTargetForAcquisition(acquisition, person, false);
    }

    public TargetRoll getTargetForAcquisition(final IAcquisitionWork acquisition,
                                              final @Nullable Person person,
                                              final boolean checkDaysToWait) {
        if (getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "Automatic Success");
        }

        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "No one on your force is capable of acquiring parts");
        }
        final Skill skill = person.getSkillForWorkingOn(getCampaignOptions().getAcquisitionSkill());
        if (null != getShoppingList().getShoppingItem(
                acquisition.getNewEquipment())
                && checkDaysToWait) {
            return new TargetRoll(
                    TargetRoll.AUTOMATIC_FAIL,
                    "You must wait until the new cycle to check for this part. Further attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == Part.T_CLAN
                && !getCampaignOptions().isAllowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == Part.T_IS
                && !getCampaignOptions().isAllowISPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire inner sphere parts");
        }
        if (getCampaignOptions().getTechLevel() < Utilities
                .getSimpleTechLevel(acquisition.getTechLevel())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "You cannot acquire parts of this tech level");
        }
        if (getCampaignOptions().isLimitByYear()
                && !acquisition.isIntroducedBy(getGameYear(), useClanTechBase(), getTechFaction())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It has not been invented yet!");
        }
        if (getCampaignOptions().isDisallowExtinctStuff() &&
                (acquisition.isExtinctIn(getGameYear(), useClanTechBase(), getTechFaction())
                        || acquisition.getAvailability() == EquipmentType.RATING_X)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                    "It is extinct!");
        }
        if (getCampaignOptions().isUseAtB() &&
                getCampaignOptions().isRestrictPartsByMission() && acquisition instanceof Part) {
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
                    if (EnumSet.of(AmmoType.Munitions.M_STANDARD).containsAll(
                            ((megamek.common.AmmoType) et).getMunitionType())){
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
        TargetRoll target = new TargetRoll(skill.getFinalSkillValue(), skill.getSkillLevel().toString());
        target.append(acquisition.getAllAcquisitionMods());
        return target;
    }

    public @Nullable AtBContract getAttachedAtBContract(Unit unit) {
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
                LogManager.getLogger().error("AtB: used bonus part but no contract has bonus parts available.");
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
            for (final AtBContract c : getActiveAtBContracts()) {
                if ((contract == null)
                        || (c.getPartsAvailabilityLevel() > contract.getPartsAvailabilityLevel())) {
                    contract = c;
                }
            }
        }

        // if we have a contract and it has started
        if ((null != contract) && contract.isActiveOn(getLocalDate(), true)) {
            if (reportBuilder != null) {
                reportBuilder.append(contract.getPartsAvailabilityLevel()).append(" (").append(contract.getType()).append(")");
            }
            return contract.getPartsAvailabilityLevel();
        }

        /* If contract is still null, the unit is not in a contract. */
        final Person person = getLogisticsPerson();
        final int experienceLevel;
        if (person == null) {
            experienceLevel = SkillType.EXP_ULTRA_GREEN;
        } else if (CampaignOptions.S_TECH.equals(getCampaignOptions().getAcquisitionSkill())) {
            experienceLevel = person.getBestTechSkill().getExperienceLevel();
        } else {
            experienceLevel = person.getSkill(getCampaignOptions().getAcquisitionSkill()).getExperienceLevel();
        }

        final int modifier = experienceLevel - SkillType.EXP_REGULAR;

        if (reportBuilder != null) {
            reportBuilder.append(getUnitRatingMod()).append("(unit rating)");
            if (person != null) {
                reportBuilder.append(modifier).append("(").append(person.getFullName()).append(", logistics admin)");
            } else {
                reportBuilder.append(modifier).append("(no logistics admin)");
            }
        }

        return getUnitRatingMod() + modifier;
    }

    public void resetAstechMinutes() {
        astechPoolMinutes = Person.PRIMARY_ROLE_SUPPORT_TIME * getNumberPrimaryAstechs()
                + Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberSecondaryAstechs();
        astechPoolOvertime = Person.SECONDARY_ROLE_SUPPORT_TIME * getNumberPrimaryAstechs()
                + Person.SECONDARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberSecondaryAstechs();
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
        return (Math.toIntExact(getActivePersonnel().stream().filter(Person::isTech).count()) * 6)
                - getNumberAstechs();
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
            if (p.getPrimaryRole().isAstech() && !p.isDeployed()) {
                astechs++;
            }
        }
        return astechs;
    }

    public int getNumberSecondaryAstechs() {
        int astechs = 0;
        for (Person p : getActivePersonnel()) {
            if (p.getSecondaryRole().isAstech() && !p.isDeployed()) {
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

        int availableHelp = (int) Math.floor(((double) astechPoolMinutes) / minutes);
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
            return  3;
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
        return getMedicPool()
                + Math.toIntExact(getActivePersonnel().stream()
                        .filter(p -> (p.getPrimaryRole().isMedic() || p.getSecondaryRole().isMedic())
                                && !p.isDeployed())
                        .count());
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
        medicPool = Math.max(0, medicPool - i);
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
        updateCampaignOptionsFromGameOptions();
    }

    public void updateCampaignOptionsFromGameOptions() {
        getCampaignOptions().setUseTactics(getGameOptions().getOption(OptionsConstants.RPG_COMMAND_INIT).booleanValue());
        getCampaignOptions().setUseInitiativeBonus(getGameOptions().getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue());
        getCampaignOptions().setUseToughness(getGameOptions().getOption(OptionsConstants.RPG_TOUGHNESS).booleanValue());
        getCampaignOptions().setUseArtillery(getGameOptions().getOption(OptionsConstants.RPG_ARTILLERY_SKILL).booleanValue());
        getCampaignOptions().setUseAbilities(getGameOptions().getOption(OptionsConstants.RPG_PILOT_ADVANTAGES).booleanValue());
        getCampaignOptions().setUseEdge(getGameOptions().getOption(OptionsConstants.EDGE).booleanValue());
        getCampaignOptions().setUseImplants(getGameOptions().getOption(OptionsConstants.RPG_MANEI_DOMINI).booleanValue());
        getCampaignOptions().setQuirks(getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).booleanValue());
        getCampaignOptions().setAllowCanonOnly(getGameOptions().getOption(OptionsConstants.ALLOWED_CANON_ONLY).booleanValue());
        getCampaignOptions().setTechLevel(TechConstants.getSimpleLevel(getGameOptions().getOption(OptionsConstants.ALLOWED_TECHLEVEL).stringValue()));
        MekHQ.triggerEvent(new OptionsChangedEvent(this));
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
                    p.awardXP(this, getCampaignOptions().getKillXPAward());
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
                : MathUtility.clamp((rating.getModifier() / 3), IUnitRating.DRAGOON_F, IUnitRating.DRAGOON_ASTAR);
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
     * @param person The {@link Person} who should receive a randomized portrait.
     */
    public void assignRandomPortraitFor(final Person person) {
        final Portrait portrait = RandomPortraitGenerator.generate(getPersonnel(), person);
        if (!portrait.isDefault()) {
            person.setPortrait(portrait);
        }
    }

    /**
     * Assigns a random origin to a {@link Person}.
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
     * @param entity the entity to clear the game data for
     */
    public void clearGameData(Entity entity) {
        // First, lets remove any improvised clubs picked up during the combat
        entity.removeMisc(EquipmentTypeLookup.LIMB_CLUB);
        entity.removeMisc(EquipmentTypeLookup.GIRDER_CLUB);
        entity.removeMisc(EquipmentTypeLookup.TREE_CLUB);

        // Then reset mounted equipment
        for (Mounted m : entity.getEquipment()) {
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

        if (entity instanceof IBomber) {
            IBomber bomber = (IBomber) entity;
            List<BombMounted> mountedBombs = bomber.getBombs();
            if (!mountedBombs.isEmpty()) {
                // These should return an int[] filled with 0's
                int[] intBombChoices = bomber.getIntBombChoices();
                int[] extBombChoices = bomber.getExtBombChoices();
                for (BombMounted m : mountedBombs) {
                    if (m.getBaseShotsLeft() == 1) {
                        if (m.isInternalBomb()) {
                            intBombChoices[BombType.getBombTypeFromInternalName(m.getType().getInternalName())] += 1;
                        } else {
                            extBombChoices[BombType.getBombTypeFromInternalName(m.getType().getInternalName())] += 1;
                        }
                    }
                }
                bomber.setIntBombChoices(intBombChoices);
                bomber.setExtBombChoices(extBombChoices);
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

    public void completeMission(@Nullable Mission mission, MissionStatus status) {
        if (mission == null) {
            return;
        }
        mission.setStatus(status);
        if (mission instanceof Contract) {
            Contract contract = (Contract) mission;
            Money remainingMoney = Money.zero();
            // check for money in escrow
            // According to FMM(r) pg 179, both failure and breach lead to no
            // further payment even though this seems stupid
            if ((contract.getStatus().isSuccess())
                    && (contract.getMonthsLeft(getLocalDate()) > 0)) {
                remainingMoney = contract.getMonthlyPayOut()
                        .multipliedBy(contract.getMonthsLeft(getLocalDate()));
            }

            // If overage repayment is enabled, we first need to check if the salvage percent is
            // under 100. 100 means you cannot have a overage.
            // Then, we check if the salvage percent is less than the percent salvaged by the
            // unit in question. If it is, then they owe the assigner some cash
            if (getCampaignOptions().isOverageRepaymentInFinalPayment()
                    && (contract.getSalvagePct() < 100.0)) {
                final double salvagePercent = contract.getSalvagePct() / 100.0;
                final Money maxSalvage = contract.getSalvagedByEmployer().multipliedBy(salvagePercent / (1 - salvagePercent));
                if (contract.getSalvagedByUnit().isGreaterThan(maxSalvage)) {
                    final Money amountToRepay = contract.getSalvagedByUnit().minus(maxSalvage);
                    remainingMoney = remainingMoney.minus(amountToRepay);
                    contract.subtractSalvageByUnit(amountToRepay);
                }
            }

            if (remainingMoney.isPositive()) {
                getFinances().credit(TransactionType.CONTRACT_PAYMENT, getLocalDate(), remainingMoney,
                        "Remaining payment for " + contract.getName());
                addReport("Your account has been credited for " + remainingMoney.toAmountAndSymbolString()
                        + " for the remaining payout from contract " + contract.getName());
            } else if (remainingMoney.isNegative()) {
                getFinances().credit(TransactionType.CONTRACT_PAYMENT, getLocalDate(), remainingMoney,
                        "Repaying payment overages for " + contract.getName());
                addReport("Your account has been debited for " + remainingMoney.absolute().toAmountAndSymbolString()
                        + " to replay payment overages occurred during the contract " + contract.getName());
            }

            // This relies on the mission being a Contract, and AtB to be on
            if (getCampaignOptions().isUseAtB()) {
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
        addReport("You have taken out loan " + loan
                + ". Your account has been credited "
                + loan.getPrincipal().toAmountAndSymbolString()
                + " for the principal amount.");
        finances.addLoan(loan);
        MekHQ.triggerEvent(new LoanNewEvent(loan));
        finances.credit(TransactionType.LOAN_PRINCIPAL, getLocalDate(), loan.getPrincipal(),
                "Loan principal for " + loan);
    }

    public void payOffLoan(Loan loan) {
        if (finances.debit(TransactionType.LOAN_PAYMENT, getLocalDate(), loan.determineRemainingValue(),
                "Loan payoff for " + loan)) {
            addReport("You have paid off the remaining loan balance of "
                    + loan.determineRemainingValue().toAmountAndSymbolString()
                    + " on " + loan);
            finances.removeLoan(loan);
            MekHQ.triggerEvent(new LoanPaidEvent(loan));
        } else {
            addReport("<font color='red'>You do not have enough funds to pay off " + loan + "</font>");
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
        if (!u.requiresMaintenance() || !campaignOptions.isCheckMaintenance()) {
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
            if (campaignOptions.isPayForMaintain()) {
                if (!(finances.debit(TransactionType.MAINTENANCE, getLocalDate(), u.getMaintenanceCost(),
                        "Maintenance for " + u.getName()))) {
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
                try {
                    String partReport = doMaintenanceOnUnitPart(u, p, partsToDamage, paidMaintenance);
                    if (partReport != null) {
                        maintenanceReport.append(partReport).append("<br>");
                    }
                } catch (Exception e) {
                    LogManager.getLogger().error(String.format(
                            "Could not perform maintenance on part %s (%d) for %s (%s) due to an error",
                            p.getName(), p.getId(), u.getName(), u.getId().toString()), e);
                    addReport(String.format("ERROR: An error occurred performing maintenance on %s for unit %s, check the log",
                            p.getName(), u.getName()));
                }
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

            if (getCampaignOptions().isLogMaintenance()) {
                LogManager.getLogger().info(maintenanceReport.toString());
            }

            int quality = u.getQuality();
            String qualityString;
            boolean reverse = getCampaignOptions().isReverseQualityNames();
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

    private String doMaintenanceOnUnitPart(Unit u, Part p, Map<Part, Integer> partsToDamage, boolean paidMaintenance) {
        String partReport = "<b>" + p.getName() + "</b> (Quality " + p.getQualityName() + ")";
        if (!p.needsMaintenance()) {
            return null;
        }
        int oldQuality = p.getQuality();
        TargetRoll target = getTargetForMaintenance(p, u.getTech());
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
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                    if ((margin < -4) && !campaignOptions.isUseUnofficialMaintenance()) {
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
                    if ((margin < -5) && !campaignOptions.isUseUnofficialMaintenance()) {
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
                    if (margin < -6 && !campaignOptions.isUseUnofficialMaintenance()) {
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

        return partReport;
    }

    public void initTimeInService() {
        for (Person p : getPersonnel()) {
            if (!p.getPrimaryRole().isDependent() && p.getPrisonerStatus().isFree()) {
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
            if (!p.getPrimaryRole().isDependent() && p.getPrisonerStatus().isFree()) {
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
                if (p.getPrimaryRole().isMechWarrior()
                        || (p.getPrimaryRole().isAerospacePilot() && getCampaignOptions().isAeroRecruitsHaveUnits())
                        || p.getPrimaryRole().isProtoMechPilot()) {
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

    public boolean checkOverDueLoans() {
        Money overdueAmount = getFinances().checkOverdueLoanPayments(this);
        if (overdueAmount.isPositive()) {
            // FIXME : Localize
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
        if (!getRetirementDefectionTracker().getRetirees().isEmpty()) {
            // FIXME : Localize
            Object[] options = { "Show Payout Dialog", "Cancel" };
            return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(null,
                    "You have personnel who have left the unit or been killed in action but have not received their final payout.\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay one or more personnel in equipment.\n  - Just cheat and use GM mode to edit the settlement.",
                    "Unresolved Final Payments", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        }
        return false;
    }

    public boolean checkYearlyRetirements() {
        if (!getCampaignOptions().getRandomRetirementMethod().isNone()
                && getCampaignOptions().isUseYearEndRandomRetirement()
                && (ChronoUnit.DAYS.between(getRetirementDefectionTracker().getLastRetirementRoll(), getLocalDate())
                        == getRetirementDefectionTracker().getLastRetirementRoll().lengthOfYear())) {
            // FIXME : Localize
            Object[] options = { "Show Retirement Dialog", "Not Now" };
            return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(null,
                    "It has been a year since the last Employee Turnover roll, and it is time to do another.",
                    "Employee Turnover roll required", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        }
        return false;
    }

    public boolean checkScenariosDue() {
        for(Mission m : getActiveMissions(true)) {
            for(Scenario s : m.getCurrentScenarios()) {
                if((s.getDate() != null)
                        && !(s instanceof AtBScenario)
                        && !getLocalDate().isBefore(s.getDate())) {
                    return true;
                }
            }
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
    public int getTechFaction() {
        return techFactionCode;
    }

    public void updateTechFactionCode() {
        if (campaignOptions.isFactionIntroDate()) {
            for (int i = 0; i < ITechnology.MM_FACTION_CODES.length; i++) {
                if (ITechnology.MM_FACTION_CODES[i].equals(getFaction().getShortName())) {
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
}
