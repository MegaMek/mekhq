/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.death.RandomDeath;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.RandomEventLibraries;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumsLibrary;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;

/**
 * A class for containing and passing Campaign configuration information. This is implemented as a Class rather than as
 * a Record because we need the ability to mutate any given field for testing purposes; the "Wither" functionality from
 * JEP 468 would allow us to do this easily with Records but is not yet supported.
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

    private Systems systemsInstance;
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
    private Map<UUID, LifePath> lifePathLibrary;
    private RetirementDefectionTracker retirementDefectionTracker;

    private ReputationController reputation;
    private FactionStandings factionStandings;
    private BehaviorSettings autoResolveBehaviorSettings;

    private IAutosaveService autosaveService;

    private PartsStore partsStore;
    private NewPersonnelMarket newPersonnelMarket;
    private RandomDeath randomDeath;
    private CampaignSummary campaignSummary;

    // Bare constructor for test purposes.
    public CampaignConfiguration() {

    }

    /**
     * Partial CampaignConfiguration constructor; takes _some_ information needed to instantiate a Campaign. Meant for
     * use by CampaignFactory and test methods.
     *
     * @param name                     Campaign name String
     * @param date                     LocalDate start date
     * @param campaignOpts             CampaignOptions instance
     * @param faction                  Faction instance
     * @param techFaction              Faction enum value describing tech base
     * @param currencyManager          Default
     * @param reputationController     Default
     * @param factionStandings         Default
     * @param rankSystem               Default Rank System
     * @param force                    List of player's TOE forces
     * @param finances                 Default
     * @param randomEvents             Default RandomEventsLibraries
     * @param ultimatums               Default
     * @param lifePaths                Default
     * @param retDefTracker            RetirementDefectionTracker instance
     * @param autosave                 Autosave service instance
     * @param behaviorSettings         Default behavior settings
     * @param persMarket               Personnel Market (deprecated; replace with new market after refactoring)
     * @param atbMonthlyContractMarket Contract Market
     * @param unitMarket               Unit Market
     * @param divorce                  AbstractDivorce instance, defaults to Disabled
     * @param marriage                 AbstractMarriage instance, defaults to Disabled
     * @param procreation              AbstractProcreation instance, defaults to Disabled
     */
    public CampaignConfiguration(
          String name,
          LocalDate date,
          CampaignOptions campaignOpts,
          Faction faction,
          megamek.common.enums.Faction techFaction,
          CurrencyManager currencyManager,
          ReputationController reputationController,
          FactionStandings factionStandings,
          RankSystem rankSystem,
          Force force,
          Finances finances,
          RandomEventLibraries randomEvents,
          FactionStandingUltimatumsLibrary ultimatums,
          Map<UUID, LifePath> lifePaths,
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
        this.name = name;
        this.currentDay = date;
        this.campaignOptions = campaignOpts;
        this.faction = faction;
        this.techFaction = techFaction;
        this.currencyManager = currencyManager;
        this.reputation = reputationController;
        this.factionStandings = factionStandings;
        this.rankSystem = rankSystem;
        this.forces = force;
        this.finances = finances;
        this.randomEventLibraries = randomEvents;
        this.factionStandingUltimatumsLibrary = ultimatums;
        this.lifePathLibrary = lifePaths;
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

    /**
     * Primary CampaignConfiguration constructor; takes all information needed to instantiate a Campaign. Meant for use
     * by CampaignFactory methods.
     *
     * @param game                     Game instance
     * @param player                   Player instance
     * @param name                     Campaign name String
     * @param date                     LocalDate start date
     * @param campaignOpts             CampaignOptions instance
     * @param gameOptions              GameOptions instance, for MegaMek
     * @param partsStore               PartsStore instance (Campaign or user must initialize with campaign reference!)
     * @param newPersonnelMarket       NewPersonnelMarket instance (Campaign or user must initialize with campaign
     *                                 reference!)
     * @param randomDeath              RandomDeath instance (Campaign or user must initialize with campaign reference!)
     * @param campaignSummary          CampaignSummary instance (Campaign or user must initialize with campaign
     *                                 reference!)
     * @param faction                  Faction instance
     * @param techFaction              Faction enum value describing tech base
     * @param currencyManager          Default
     * @param systemsInstance          Instance of Systems, for hooking into Systems lookups.
     * @param startLocation            Location of starting planetary system.
     * @param reputationController     Default
     * @param factionStandings         Default
     * @param rankSystem               Default Rank System
     * @param force                    List of player's TOE forces
     * @param finances                 Default
     * @param randomEvents             Default RandomEventsLibraries
     * @param ultimatums               Default
     * @param lifePaths                Default
     * @param retDefTracker            RetirementDefectionTracker instance
     * @param autosave                 Autosave service instance
     * @param behaviorSettings         Default behavior settings
     * @param persMarket               Personnel Market (deprecated; replace with new market after refactoring)
     * @param atbMonthlyContractMarket Contract Market
     * @param unitMarket               Unit Market
     * @param divorce                  AbstractDivorce instance, defaults to Disabled
     * @param marriage                 AbstractMarriage instance, defaults to Disabled
     * @param procreation              AbstractProcreation instance, defaults to Disabled
     */
    public CampaignConfiguration(
          Game game,
          Player player,
          String name,
          LocalDate date,
          CampaignOptions campaignOpts,
          GameOptions gameOptions,
          PartsStore partsStore,
          NewPersonnelMarket newPersonnelMarket,
          RandomDeath randomDeath,
          CampaignSummary campaignSummary,
          Faction faction,
          megamek.common.enums.Faction techFaction,
          CurrencyManager currencyManager,
          Systems systemsInstance,
          CurrentLocation startLocation,
          ReputationController reputationController,
          FactionStandings factionStandings,
          RankSystem rankSystem,
          Force force,
          Finances finances,
          RandomEventLibraries randomEvents,
          FactionStandingUltimatumsLibrary ultimatums,
          Map<UUID, LifePath> lifePaths,
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
        this.partsStore = partsStore;
        this.newPersonnelMarket = newPersonnelMarket;
        this.randomDeath = randomDeath;
        this.campaignSummary = campaignSummary;
        this.faction = faction;
        this.techFaction = techFaction;
        this.currencyManager = currencyManager;
        this.systemsInstance = systemsInstance;
        this.location = startLocation;
        this.reputation = reputationController;
        this.factionStandings = factionStandings;
        this.rankSystem = rankSystem;
        this.forces = force;
        this.finances = finances;
        this.randomEventLibraries = randomEvents;
        this.factionStandingUltimatumsLibrary = ultimatums;
        this.lifePathLibrary = lifePaths;
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

    public PartsStore getPartsStore() {
        return this.partsStore;
    }

    public NewPersonnelMarket getNewPersonnelMarket() {
        return this.newPersonnelMarket;
    }

    public RandomDeath getRandomDeath() {
        return this.randomDeath;
    }

    public CampaignSummary getCampaignSummary() {
        return this.campaignSummary;
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

    public Systems getSystemsInstance() {
        return systemsInstance;
    }

    public CurrentLocation getLocation() {
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

    public Map<UUID, LifePath> getLifePaths() {
        return this.lifePathLibrary;
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

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setGameOptions(GameOptions gameOptions) {
        this.gameOptions = gameOptions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentDay(LocalDate currentDay) {
        this.currentDay = currentDay;
    }

    public void setForces(Force forces) {
        this.forces = forces;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public void setTechFaction(megamek.common.enums.Faction techFaction) {
        this.techFaction = techFaction;
    }

    public void setRankSystem(RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }

    public void setCurrencyManager(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    public void setFinances(Finances finances) {
        this.finances = finances;
    }

    public void setSystemsInstance(Systems systemsInstance) {
        this.systemsInstance = systemsInstance;
    }

    public void setLocation(CurrentLocation location) {
        this.location = location;
    }

    public void setCampaignOptions(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
    }

    public void setPersonnelMarket(PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
    }

    public void setContractMarket(AbstractContractMarket contractMarket) {
        this.contractMarket = contractMarket;
    }

    public void setUnitMarket(AbstractUnitMarket unitMarket) {
        this.unitMarket = unitMarket;
    }

    public void setDivorce(AbstractDivorce divorce) {
        this.divorce = divorce;
    }

    public void setMarriage(AbstractMarriage marriage) {
        this.marriage = marriage;
    }

    public void setProcreation(AbstractProcreation procreation) {
        this.procreation = procreation;
    }

    public void setRandomEventLibraries(RandomEventLibraries randomEventLibraries) {
        this.randomEventLibraries = randomEventLibraries;
    }

    public void setFactionStandingUltimatumsLibrary(FactionStandingUltimatumsLibrary factionStandingUltimatumsLibrary) {
        this.factionStandingUltimatumsLibrary = factionStandingUltimatumsLibrary;
    }

    public void setLifePathLibrary(Map<UUID, LifePath> lifePathLibrary) {
        this.lifePathLibrary = lifePathLibrary;
    }

    public void setRetirementDefectionTracker(RetirementDefectionTracker retirementDefectionTracker) {
        this.retirementDefectionTracker = retirementDefectionTracker;
    }

    public void setReputation(ReputationController reputation) {
        this.reputation = reputation;
    }

    public void setFactionStandings(FactionStandings factionStandings) {
        this.factionStandings = factionStandings;
    }

    public void setAutoResolveBehaviorSettings(BehaviorSettings autoResolveBehaviorSettings) {
        this.autoResolveBehaviorSettings = autoResolveBehaviorSettings;
    }

    public void setAutosaveService(IAutosaveService autosaveService) {
        this.autosaveService = autosaveService;
    }

    public void setPartsStore(PartsStore partsStore) {
        this.partsStore = partsStore;
    }

    public void setNewPersonnelMarket(NewPersonnelMarket newPersonnelMarket) {
        this.newPersonnelMarket = newPersonnelMarket;
    }

    public void setRandomDeath(RandomDeath randomDeath) {
        this.randomDeath = randomDeath;
    }

    public void setCampaignSummary(CampaignSummary campaignSummary) {
        this.campaignSummary = campaignSummary;
    }
}
