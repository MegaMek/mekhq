/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomMercenaryCompanyNameGenerator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.zip.GZIPInputStream;

import megamek.Version;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Force;
import mekhq.campaign.io.CampaignXmlParseException;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;
import mekhq.campaign.personnel.death.RandomDeath;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.RandomEventLibraries;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumsLibrary;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.dialog.CampaignHasProblemOnLoad;
import mekhq.service.AutosaveService;

/**
 * Defines a factory API that enables {@link Campaign} instances to be created from its detected format.
 */
public class CampaignFactory {
    private static final MMLogger LOGGER = MMLogger.create(CampaignFactory.class);
    private MekHQ app;

    public enum CampaignProblemType {
        NONE,
        CANT_LOAD_FROM_NEWER_VERSION,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        CANT_LOAD_FROM_OLDER_VERSION,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        ACTIVE_OR_FUTURE_CONTRACT,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        NEW_VERSION_WITH_OLD_DATA
    }

    /**
     * Protected constructor to prevent instantiation.
     */
    protected CampaignFactory() {
    }

    /**
     * Obtain a new instance of a CampaignFactory.
     *
     * @return New instance of a CampaignFactory.
     */
    public static CampaignFactory newInstance(MekHQ app) {
        CampaignFactory factory = new CampaignFactory();
        factory.app = app;
        return factory;
    }

    /**
     * Creates a new instance of a {@link Campaign} from the input stream using the currently configured parameters.
     *
     * @param is The {@link InputStream} to create the {@link Campaign} from.
     *
     * @return A new instance of a {@link Campaign}.
     *
     * @throws CampaignXmlParseException if the XML for the campaign cannot be parsed.
     * @throws IOException               if an IO error is encountered reading the input stream.
     * @throws NullEntityException       if the campaign contains a null entity
     */
    public @Nullable Campaign createCampaign(InputStream is)
          throws CampaignXmlParseException, IOException, NullEntityException {
        if (!is.markSupported()) {
            is = new BufferedInputStream(is);
        }

        byte[] header = readHeader(is);

        // Check if the first two bytes are the GZIP magic bytes...
        if ((header.length >= 2) && (header[0] == (byte) 0x1f) && (header[1] == (byte) 0x8b)) {
            is = new GZIPInputStream(is);
        }
        // ...otherwise, assume we're an XML file.

        CampaignXmlParser parser = new CampaignXmlParser(is, this.app);
        Campaign campaign = parser.parse();

        return checkForLoadProblems(campaign);
    }

    /**
     * Creates a partially-configured CampaignConfiguration that is missing: 1. Systems (TestSystems for testing
     * purposes) 2. GameOptions (required for MegaMek, may be candidate for further test class development) 3. Player
     * instance 4. LocalDate for Campaign start day 5. CurrentLocation (created from a PlanetarySystem, which must be
     * retrieved using Systems or TestSystems) 6. Logistical classes: parts store, new-type personnel market, random
     * death generator, persistent campaign summary tracker. Useful for testing purposes, as the missing references can
     * be replaced with mocks or lightweight test classes.
     *
     * @param options CampaignOptions instance; used in Randomizer construction
     *
     * @return campaignConfig CampaignConfiguration with the above items unset
     */
    public static @Nullable CampaignConfiguration createPartialCampaignConfiguration(CampaignOptions options) {
        CampaignConfiguration campaignConfig = null;

        String name = randomMercenaryCompanyNameGenerator(null);
        LocalDate date = LocalDate.ofYearDay(3067, 1);
        Faction faction = new Faction();

        megamek.common.enums.Faction techFaction = megamek.common.enums.Faction.MERC;
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        ReputationController reputationController = new ReputationController();

        FactionStandings factionStandings = new FactionStandings();
        RankSystem rankSystem = Ranks.getRankSystemFromCode(Ranks.DEFAULT_SYSTEM_CODE);
        Force force = new Force(name);

        Finances finances = new Finances();
        RandomEventLibraries randomEvents = null;
        FactionStandingUltimatumsLibrary ultimatums = null;

        RetirementDefectionTracker retirementDefectionTracker = new RetirementDefectionTracker();
        AutosaveService autosave = new AutosaveService();
        BehaviorSettings behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;

        // Set up markets
        // TODO: Replace PersonnelMarket due to deprecation
        PersonnelMarket personnelMarket = new PersonnelMarket();
        AtbMonthlyContractMarket atbMonthlyContractMarket = new AtbMonthlyContractMarket();
        DisabledUnitMarket disabledUnitMarket = new DisabledUnitMarket();

        // Set up Randomizers based on campaignOptions
        DisabledRandomDivorce disabledRandomDivorce = new DisabledRandomDivorce(options);
        DisabledRandomMarriage disabledRandomMarriage = new DisabledRandomMarriage(options);
        DisabledRandomProcreation disabledRandomProcreation = new DisabledRandomProcreation(options);

        try {
            randomEvents = new RandomEventLibraries();
        } catch (Exception ex) {
            LOGGER.error("Unable to initialize RandomEventLibraries. If this wasn't during automated testing this " +
                               "must be investigated.", ex);
        }

        try {
            ultimatums = new FactionStandingUltimatumsLibrary();
        } catch (Exception ex) {
            LOGGER.error("Unable to initialize FactionStandingUltimatumsLibrary. If this wasn't during automated " +
                               "testing this must be investigated.", ex);
        }

        try {
            faction = Factions.getInstance().getDefaultFaction();
        } catch (Exception ex) {
            LOGGER.error("Unable to set faction to default faction. If this wasn't during automated testing this must" +
                               " be investigated.", ex);
        }

        try {
            campaignConfig = new CampaignConfiguration(name, date, options,
                  faction, techFaction, currencyManager, reputationController,
                  factionStandings, rankSystem, force, finances, randomEvents, ultimatums,
                  retirementDefectionTracker, autosave, behaviorSettings,
                  personnelMarket, atbMonthlyContractMarket, disabledUnitMarket,
                  disabledRandomDivorce, disabledRandomMarriage, disabledRandomProcreation);
        } catch (Exception e) {
            LOGGER.error("Unable to create campaign.", e);
        }

        return campaignConfig;
    }

    /**
     * Factory function to create an object containing all the configuration info needed to generate a Campaign
     * instance.  Useful for tweaking some settings prior to creating the Campaign. The standard CampaignConfiguration
     * uses all the settings that previously were generated or initialized within * `new Campaign()`. Note: this method
     * does load all Planetary Systems and PartsStore entries, which can take several seconds
     *
     * @return campaignConfig CampaignConfiguration with all of the default values set
     */
    public static @Nullable CampaignConfiguration createCampaignConfiguration() {

        Game game = new Game();
        Player player = new Player(0, "self");

        Systems systems = Systems.getInstance();

        // For simplicity, createPartialCampaignConfiguration needs a CampaignOptions instance.
        CampaignOptions campaignOptions = new CampaignOptions();
        CampaignConfiguration campaignConfig = CampaignFactory.createPartialCampaignConfiguration(campaignOptions);

        // A starting CurrentLocation is required
        PlanetarySystem starterSystem = systems.getSystems().get("Galatea");
        CurrentLocation location;
        try {
            location = new CurrentLocation(starterSystem, 0);
        } catch (Exception ex) {
            String message = String.format(
                  "Unable to set location to %s. If this wasn't during automated testing this must be investigated.",
                  starterSystem.getName(campaignConfig.getDate())
            );
            LOGGER.error(message, ex);
            return null;
        }

        GameOptions gameOptions = new GameOptions();
        gameOptions.getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaignConfig.getDate().getYear());

        // Instantiate default parts store, new-style personnel market, random death manager, and campaign summary
        // object.
        // Note: Campaign is responsible for correctly initializing these objects now!
        PartsStore partsStore = new PartsStore();
        NewPersonnelMarket newPersonnelMarket = new NewPersonnelMarket();
        RandomDeath randomDeath = new RandomDeath();
        CampaignSummary campaignSummary = new CampaignSummary();

        // Assign remaining values to CampaignConfig
        campaignConfig.setGame(game);
        campaignConfig.setPlayer(player);
        campaignConfig.setGameOptions(gameOptions);
        campaignConfig.setSystemsInstance(systems);
        campaignConfig.setLocation(location);
        campaignConfig.setPartsStore(partsStore);
        campaignConfig.setNewPersonnelMarket(newPersonnelMarket);
        campaignConfig.setRandomDeath(randomDeath);
        campaignConfig.setCampaignSummary(campaignSummary);

        return campaignConfig;
    }

    /**
     * Create a new Campaign() instance with all of the correct default values and data providers needed to run a MekHQ
     * game. Analogous to `new Campaign(...)` with all standard values. Note: calls `createCampaignConfiguration()` to
     * get these values in a compact object. Note: Side effect: loads all Planetary Systems and PartsStore entries,
     * which can take several seconds
     *
     * @return campaign Campaign object that is fully initialized and ready to run.
     */
    public static @Nullable Campaign createCampaign() {
        Campaign campaign = null;
        CampaignConfiguration campaignConfig = createCampaignConfiguration();

        if (campaignConfig == null) {
            LOGGER.error("Unable to create campaign configuration.");
            return null;
        }
        try {
            campaign = new Campaign(campaignConfig);
        } catch (Exception e) {
            LOGGER.error("Unable to create campaign.", e);
        }

        return campaign;
    }

    /**
     * Validates the campaign for loading issues and presents the user with dialogs for each problem encountered.
     *
     * <p>This method sequentially checks for three potential problems while loading the campaign:</p>
     * <ul>
     *   <li>If the campaign version is newer than the application's version.</li>
     *   <li>If the campaign version is older than the last supported milestone version.</li>
     *   <li>If the campaign has active or future AtB contracts.</li>
     * </ul>
     *
     * <p>For each issue encountered, a dialog is displayed to the user using {@link CampaignHasProblemOnLoad}.
     * The user can either cancel or proceed with loading. If the user cancels at any point, the method
     * returns {@code null}. Otherwise, if no problems remain or the user chooses to proceed for all
     * issues, the method returns the given {@code Campaign} object.</p>
     *
     * @param campaign the {@link Campaign} object to validate and load
     *
     * @return the {@link Campaign} object if the user chooses to proceed with all problems or if no problems are
     *       detected; {@code null} if the user chooses to cancel
     */
    private static Campaign checkForLoadProblems(Campaign campaign) {
        final Version campaignVersion = campaign.getVersion();

        // Check if the campaign is from a newer version
        if (campaignVersion.isHigherThan(MHQConstants.VERSION)) {
            if (triggerProblemDialog(campaign, CampaignProblemType.CANT_LOAD_FROM_NEWER_VERSION)) {
                return null;
            }
        }

        // All checks passed, return the campaign
        return campaign;
    }

    /**
     * Displays the {@link CampaignHasProblemOnLoad} dialog for a given problem type and returns whether the user
     * cancelled the loading process.
     *
     * <p>The dialog informs the user about the specific problem and allows them to either
     * cancel the loading process or continue despite the problem. If the user selects "Cancel," the method returns
     * {@code true}. Otherwise, it returns {@code false}.</p>
     *
     * @param campaign    the {@link Campaign} object associated with the problem
     * @param problemType the {@link CampaignProblemType} specifying the current issue
     *
     * @return {@code true} if the user chose to cancel loading, {@code false} otherwise
     */
    private static boolean triggerProblemDialog(Campaign campaign, CampaignProblemType problemType) {
        CampaignHasProblemOnLoad problemDialog = new CampaignHasProblemOnLoad(campaign, problemType);
        return problemDialog.wasCanceled();
    }

    private byte[] readHeader(InputStream is) throws IOException {
        is.mark(4);
        byte[] header = new byte[2];
        is.read(header);
        is.reset();

        return header;
    }

}
