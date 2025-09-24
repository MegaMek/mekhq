package mekhq.campaign;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.RandomEventLibraries;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumsLibrary;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;
import java.time.LocalDate;

/**
 * A class for containing and passing Campaign configuration information.
 * This is implemented as a Class rather than as a Record because we need the ability
 * to mutate any given field for testing purposes; the "Wither" functionality from JEP 468
 * would allow us to do this easily with Records but is not yet supported.
 */
public class CampaignConfiguration {

    private Game game;
    private Player player;

    private GameOptions gameOptions;

    private String name;
    private LocalDate currentDay;

    private Force forces;

    private Faction faction;
    private megamek.common.enums.Faction techFaction;
    private RankSystem rankSystem;
    private CurrencyManager currencyManager;

    private Finances finances;

    private CurrentLocation location;
    private CampaignOptions campaignOptions;

    @Deprecated(since = "0.50.06")
    private PersonnelMarket personnelMarket;

    private AbstractContractMarket contractMarket;
    private AbstractUnitMarket unitMarket;

    private AbstractDivorce divorce;
    private AbstractMarriage marriage;
    private AbstractProcreation procreation;

    private RandomEventLibraries randomEventLibraries;
    private FactionStandingUltimatumsLibrary factionStandingUltimatumsLibrary;
    private RetirementDefectionTracker retirementDefectionTracker;

    private ReputationController reputation;
    private FactionStandings factionStandings;
    private BehaviorSettings autoResolveBehaviorSettings;

    private IAutosaveService autosaveService;

    public CampaignConfiguration(
          Game game,
          Player player,
          String name,
          LocalDate date,
          CampaignOptions campaignOpts,
          GameOptions gameOptions,
          Faction faction,
          megamek.common.enums.Faction techFaction,
          CurrencyManager currencyManager,
          CurrentLocation startLocation,
          ReputationController reputationController,
          FactionStandings factionStandings,
          RankSystem rankSystem,
          Force force,
          Finances finances,
          RandomEventLibraries randomEvents,
          FactionStandingUltimatumsLibrary ultimatums,
          RetirementDefectionTracker retDefTracker,
          AutosaveService autosave,
          BehaviorSettings behaviorSettings,
          PersonnelMarket persMarket,
          AtbMonthlyContractMarket atbMonthlyContractMarket,
          AbstractUnitMarket unitMarket,
          AbstractDivorce divorce,
          AbstractMarriage marriage,
          AbstractProcreation procreation
    ) {
        this.game = game;
        this.player = player;
        this.name = name;
        this.currentDay = date;
        this.campaignOptions = campaignOpts;
        this.gameOptions = gameOptions;
        this.faction = faction;
        this.techFaction = techFaction;
        this.currencyManager = currencyManager;
        this.location = startLocation;
        this.reputation = reputationController;
        this.factionStandings = factionStandings;
        this.rankSystem = rankSystem;
        this.forces = force;
        this.finances = finances;
        this.randomEventLibraries = randomEvents;
        this.factionStandingUltimatumsLibrary = ultimatums;
        this.retirementDefectionTracker = retDefTracker;
        this.autosaveService = autosave;
        this.autoResolveBehaviorSettings = behaviorSettings;
        this.personnelMarket = persMarket;
        this.contractMarket = atbMonthlyContractMarket;
        this.unitMarket = unitMarket;
        this.divorce = divorce;
        this.marriage = marriage;
        this.procreation = procreation;
    }

    public Game getGame() {
        return this.game;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getName() {
        return this.name;
    }

    public LocalDate getDate() {
        return this.currentDay;
    }

    public CampaignOptions getCampaignOpts() {
        return this.campaignOptions;
    }

    public GameOptions getGameOptions() {
        return this.gameOptions;
    }

    public Faction getfaction() {
        return this.faction;
    }

    public megamek.common.enums.Faction getTechFaction() {
        return this.techFaction;
    }

    public CurrencyManager getCurrencyManager() {
        return this.currencyManager;
    }

    public CurrentLocation getStartLocation() {
        return this.location;
    }

    public ReputationController getReputationController() {
        return this.reputation;
    }

    public FactionStandings getFactionStandings() {
        return this.factionStandings;
    }

    public RankSystem getRankSystem() {
        return this.rankSystem;
    }

    public Force getforce() {
        return this.forces;
    }

    public Finances getfinances() {
        return this.finances;
    }

    public RandomEventLibraries getRandomEvents() {
        return this.randomEventLibraries;
    }

    public FactionStandingUltimatumsLibrary getUltimatums() {
        return this.factionStandingUltimatumsLibrary;
    }

    public RetirementDefectionTracker getRetDefTracker() {
        return this.retirementDefectionTracker;
    }

    public IAutosaveService getAutosave() {
        return this.autosaveService;
    }

    public BehaviorSettings getBehaviorSettings() {
        return this.autoResolveBehaviorSettings;
    }

    public PersonnelMarket getPersonnelMarket() {
        return this.personnelMarket;
    }

    public AbstractContractMarket getAtBMonthlyContractMarket() {
        return this.contractMarket;
    }

    public AbstractUnitMarket getUnitMarket() {
        return this.unitMarket;
    }

    public AbstractDivorce getDivorce() {
        return this.divorce;
    }

    public AbstractMarriage getMarriage() {
        return this.marriage;
    }

    public AbstractProcreation getProcreation() {
        return this.procreation;
    }
}
