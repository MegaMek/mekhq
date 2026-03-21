/*
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static java.util.Arrays.sort;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP_ABRIDGED;
import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;

import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import megamek.Version;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.RawImagePanel;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignEventProcessor;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.storyArc.StoryArc;
import mekhq.campaign.storyArc.StoryArcStub;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.factionHints.WarAndPeaceProcessor;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.gui.baseComponents.AbstractMHQDialogBasic;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.CampaignOptionsPresetPicker;
import mekhq.gui.dialog.CampaignUpgradeDialog;
import mekhq.service.ai.CampaignProposal;

public class DataLoadingDialog extends AbstractMHQDialogBasic implements PropertyChangeListener {
    private static final MMLogger LOGGER = MMLogger.create(DataLoadingDialog.class);

    // region Variable Declarations
    private final MekHQ application;
    private final File campaignFile;
    private final Task task;
    private RawImagePanel splash;
    private JProgressBar progressBar;
    private final StoryArcStub storyArcStub;
    private final boolean isInAppNewCampaign;

    private final LocalDate DEFAULT_START_DATE = LocalDate.of(3051, 1, 1);

    // endregion Variable Declarations

    // region Constructors
    public DataLoadingDialog(final JFrame frame, final MekHQ application, final @Nullable File campaignFile) {
        this(frame, application, campaignFile, null, false);
    }

    public DataLoadingDialog(final JFrame frame, final MekHQ application, final @Nullable File campaignFile,
          StoryArcStub stub, final boolean isInAppNewCampaign) {
        super(frame, "DataLoadingDialog", "DataLoadingDialog.title");
        this.application = application;
        this.campaignFile = campaignFile;
        this.storyArcStub = stub;
        this.isInAppNewCampaign = isInAppNewCampaign;
        this.task = new Task(this);
        getTask().addPropertyChangeListener(this);
        initialize();
        getTask().execute();
    }
    // endregion Constructors

    // region Getters/Setters
    public MekHQ getApplication() {
        return application;
    }

    public @Nullable File getCampaignFile() {
        return campaignFile;
    }

    public Task getTask() {
        return task;
    }

    public RawImagePanel getSplash() {
        return splash;
    }

    public void setSplash(final RawImagePanel splash) {
        this.splash = splash;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(final JProgressBar progressBar) {
        this.progressBar = progressBar;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected void initialize() {
        setUndecorated(true);
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        add(createProgressBar(), BorderLayout.PAGE_END);
        finalizeInitialization();
    }

    @Override
    protected Container createCenterPane() {
        setSplash(UIUtil.createSplashComponent(getApplication().getIconPackage().getLoadingScreenImages(), getFrame()));
        return getSplash();
    }

    private JProgressBar createProgressBar() {
        setProgressBar(new JProgressBar(0, 7));
        getProgressBar().setString(resources.getString("loadingBaseData.text"));
        getProgressBar().setValue(0);
        getProgressBar().setStringPainted(true);
        getProgressBar().setVisible(true);
        return getProgressBar();
    }

    @Override
    protected void finalizeInitialization() {
        setPreferredSize(getSplash().getPreferredSize());
        setSize(getSplash().getPreferredSize());
        pack();
        fitAndCenter();
        getFrame().setVisible(true);
    }
    // endregion Initialization

    // region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        int progress = task.getProgress();
        progressBar.setValue(progress);

        // If you add a new tier you MUST increase the levels of the progressBar
        switch (progress) {
            case 0:
                progressBar.setString(resources.getString("loadingBaseData.text"));
                break;
            case 1:
                progressBar.setString(resources.getString("loadingFactionData.text"));
                break;
            case 2:
                progressBar.setString(resources.getString("loadingNameData.text"));
                break;
            case 3:
                progressBar.setString(resources.getString("loadingPlanetaryData.text"));
                break;
            case 4:
                progressBar.setString(resources.getString("loadingImageData.text"));
                break;
            case 5:
                progressBar.setString(resources.getString("loadingUnits.text"));
                break;
            case 6:
                progressBar.setString(resources.getString((getCampaignFile() == null) ?
                                                                "initializingNewCampaign.text" :
                                                                "loadingCampaign.text"));
                break;
            case 7:
                progressBar.setString(resources.getString((getCampaignFile() == null) ?
                                                                "applyingNewCampaign.text" :
                                                                "applyingLoadedCampaign.text"));
                break;
            default:
                progressBar.setString(resources.getString("Error.text"));
                break;
        }

        getAccessibleContext().setAccessibleDescription(String.format(resources.getString(
              "DataLoadingDialog.progress.accessibleDescription"), progressBar.getString()));
    }
    // endregion PropertyChangeListener

    /**
     * Main task. This is executed in a background thread.
     */
    public class Task extends SwingWorker<Campaign, Campaign> {
        JDialog dialog;

        public Task(JDialog dialog) {
            this.dialog = dialog;
        }

        /**
         * This uses the following stages of loading:
         * <ol>
         *     <li>Basics</li>
         *     <li>Factions</li>
         *     <li>Names</li>
         *     <li>Planetary Systems</li>
         *     <li>Portraits, Camouflage, Mek Tileset, Formation Icons, Awards</li>
         *     <li>Units</li>
         *     <li>New Campaign / Campaign Loading</li>
         *     <li>Campaign Application</li>
         * </ol>
         *
         * @return The loaded campaign
         *
         * @throws Exception if anything goes wrong
         */
        @Override
        public Campaign doInBackground() throws Exception {
            // region progress 0
            setProgress(0);
            CurrencyManager.getInstance().loadCurrencies();
            Eras.initializeEras();
            FinancialInstitutions.initializeFinancialInstitutions();
            InjuryTypes.registerAll(); // TODO : Isolate into an actual module
            Ranks.initializeRankSystems();
            SkillType.initializeTypes();
            sort(SkillType.getSkillList()); // sort all skills alphabetically
            SpecialAbility.initializeSPA(false);
            AtBScenarioModifier.initializeScenarioModifiers(false);
            // endregion Progress 0

            // region progress 1
            setProgress(1);
            Factions.setInstance(Factions.loadDefault(false));
            // endregion Progress 1

            // region progress 2
            setProgress(2);
            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            RandomCompanyNameGenerator.getInstance();
            Bloodname.loadBloodnameData();
            // endregion Progress 2

            // region progress 3
            setProgress(3);
            Systems.setInstance(Systems.loadDefault());
            // endregion Progress 3

            // region progress 4
            setProgress(4);
            MHQStaticDirectoryManager.initialize();
            // endregion Progress 4

            // region progress 5
            setProgress(5);
            while (!MekSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            // endregion Progress 5

            setProgress(6);
            final Campaign campaign;
            boolean isNewCampaign = false;
            if (getCampaignFile() == null) {
                // region progress 6
                LOGGER.info("Starting a new campaign");
                campaign = CampaignFactory.createCampaign();
                isNewCampaign = true;

                // Campaign Preset
                final CampaignOptionsPresetPicker campaignOptionsPresetPicker =
                      new CampaignOptionsPresetPicker(getFrame(), true);
                CampaignPreset preset;
                boolean isSelect;

                if (campaignOptionsPresetPicker.wasCanceled()) {
                    if (isInAppNewCampaign) {
                        application.exit(false);
                    }
                    return null;
                }

                preset = campaignOptionsPresetPicker.getSelectedPreset();
                isSelect = campaignOptionsPresetPicker.wasApplied();

                // MegaMek Options
                if ((preset != null) && (preset.getGameOptions() != null)) {
                    campaign.setGameOptions(preset.getGameOptions());
                }

                // Campaign Options
                // This needs to be before we trigger the customize preset dialog
                // AI Proposal Pre-fill
                CampaignProposal proposal = MekHQ.getPendingAiProposal();
                if (proposal != null) {
                    campaign.setName(proposal.mercenaryUnitName != null ? proposal.mercenaryUnitName : proposal.campaignName);
                    campaign.setLocalDate(LocalDate.of(proposal.startYear, 1, 1));
                    campaign.setBackstory(proposal.backgroundStory);
                    
                    // Try to find the faction
                    String factionCode = proposal.startingFactionCode;
                    if (factionCode != null) {
                        mekhq.campaign.universe.Faction f = Factions.getInstance().getFaction(factionCode);
                        if (f != null) {
                            campaign.setFaction(f);
                        }
                    }
                } else {
                    campaign.setLocalDate(DEFAULT_START_DATE);
                }
                campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());

                CampaignOptionsDialogMode mode = isSelect ? STARTUP_ABRIDGED : STARTUP;
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(getFrame(), campaign, preset, mode);
                setVisible(false); // cede visibility to `optionsDialog`
                optionsDialog.setVisible(true);
                if (optionsDialog.wasCanceled()) {
                    return null;
                } else {
                    setVisible(true); // restore loader visibility
                }

                // Starting planet
                Planet startingPlanet = (preset == null) ? null : preset.getPlanet();
                
                // AI Proposal Planet Pre-fill
                if (startingPlanet == null && proposal != null && proposal.startingPlanetName != null) {
                    mekhq.campaign.universe.PlanetarySystem sys = Systems.getInstance().getSystemByName(proposal.startingPlanetName, campaign.getLocalDate());
                    if (sys != null) {
                        startingPlanet = sys.getPrimaryPlanet();
                    } else {
                        LOGGER.error("AI proposed planet '" + proposal.startingPlanetName + "' was not found in the database. Using default.");
                    }
                }
                
                // If the player hasn't set a starting planet in the preset, use the default for their chosen faction
                if (startingPlanet == null) {
                    startingPlanet = campaign.getNewCampaignStartingPlanet();
                }
                campaign.setStartingSystem(startingPlanet);

                // region AI Initialization
                if (proposal != null) {
                    LOGGER.info("AIService: Starting AI initialization for campaign: " + proposal.campaignName);
                    
                    // 1. Finances
                    if (proposal.startingFunds > 0) {
                        try {
                            campaign.getFinances().credit(
                                TransactionType.STARTING_CAPITAL, 
                                campaign.getLocalDate(), 
                                Money.of((double)proposal.startingFunds), 
                                "AI Campaign Starting Funds"
                            );
                            LOGGER.info("AIService: Credited " + proposal.startingFunds + " C-Bills");
                        } catch (Exception e) {
                            LOGGER.error("AIService: Failed to add starting funds", e);
                        }
                    }

                    // 2. Personnel & Units
                    boolean unitsAdded = false;
                    if (proposal.startingUnits != null) {
                        DefaultPersonnelGenerator generator = new DefaultPersonnelGenerator(
                            new DefaultFactionSelector(campaign.getCampaignOptions().getRandomOriginOptions(), campaign.getFaction()), 
                            new DefaultPlanetSelector(campaign.getCampaignOptions().getRandomOriginOptions(), campaign.getLocation().getPlanet()));
                            
                        for (CampaignProposal.UnitProposal up : proposal.startingUnits) {
                            try {
                                // Enhanced Mek lookup
                                MekSummary summary = MekSummaryCache.getInstance().getMek(up.modelName);
                                if (summary == null) {
                                    // Try common variations if exact match fails
                                     LOGGER.warn("AIService: Exact match failed for '" + up.modelName + "'. Trying robust search...");
                                     for (MekSummary s : MekSummaryCache.getInstance().getAllMeks()) {
                                         if (s.getName().equalsIgnoreCase(up.modelName) || 
                                             s.getName().contains(up.modelName)) {
                                             summary = s;
                                             break;
                                         }
                                     }
                                 }

                                if (summary != null) {
                                    Entity entity = summary.loadEntity();
                                    if (entity != null) {
                                        Unit unit = new Unit(entity, campaign);
                                        Person pilot = campaign.newPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, 
                                            generator, megamek.common.enums.Gender.RANDOMIZE);
                                        pilot.setGivenName(up.pilotName);
                                        pilot.setSurname("");
                                        pilot.setStatus(PersonnelStatus.ACTIVE);
                                        
                                        if (up.pilotSkills != null && up.pilotSkills.contains("/")) {
                                            try {
                                                String[] skills = up.pilotSkills.split("/");
                                                int gunnery = Integer.parseInt(skills[0].trim());
                                                int piloting = Integer.parseInt(skills[1].trim());
                                                pilot.getSkills().addSkill(SkillType.S_GUN_MEK, new Skill(SkillType.getType(SkillType.S_GUN_MEK), gunnery, 0));
                                                pilot.getSkills().addSkill(SkillType.S_PILOT_MEK, new Skill(SkillType.getType(SkillType.S_PILOT_MEK), piloting, 0));
                                            } catch (Exception e) {
                                                LOGGER.error("AIService: Failed to parse skills: " + up.pilotSkills);
                                            }
                                        }
                                        
                                        pilot.setCallsign("AI " + pilot.getCallsign());
                                        pilot.setBiography(up.backstory);
                                        
                                        campaign.recruitPerson(pilot, true, true);
                                        unit.addDriver(pilot);
                                        campaign.getHangar().addUnit(unit);
                                        unitsAdded = true;
                                        LOGGER.info("AIService: Successfully added unit " + summary.getName() + " with pilot " + up.pilotName);
                                    }
                                } else {
                                    LOGGER.error("AIService: Could not find any unit matching '" + up.modelName + "' in the database.");
                                }
                            } catch (Exception e) {
                                LOGGER.error("AIService: Error creating unit: " + up.modelName, e);
                            }
                        }
                    }
                    
                    if (!unitsAdded) {
                        LOGGER.warn("AIService: No units were successfully added. Adding a fallback Shadow Hawk SHD-2H.");
                        try {
                            MekSummary summary = MekSummaryCache.getInstance().getMek("Shadow Hawk SHD-2H");
                            if (summary != null) {
                                Entity entity = summary.loadEntity();
                                Unit unit = new Unit(entity, campaign);
                                Person pilot = campaign.newPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, 
                                    new DefaultPersonnelGenerator(new DefaultFactionSelector(campaign.getCampaignOptions().getRandomOriginOptions(), campaign.getFaction()), 
                                    new DefaultPlanetSelector(campaign.getCampaignOptions().getRandomOriginOptions(), campaign.getLocation().getPlanet())), 
                                    megamek.common.enums.Gender.RANDOMIZE);
                                pilot.setStatus(PersonnelStatus.ACTIVE);
                                campaign.recruitPerson(pilot, true, true);
                                unit.addDriver(pilot);
                                campaign.getHangar().addUnit(unit);
                            }
                        } catch (Exception e) {
                            LOGGER.error("AIService: Failed to add fallback unit", e);
                        }
                    }
                    
                    // 3. Update Unit State
                    try {
                        recalculateCombatTeams(campaign);
                        if (campaign.getReputation() != null) {
                            campaign.getReputation().initializeReputation(campaign);
                        }
                        LOGGER.info("AIService: Recalculated unit state.");
                    } catch (Exception e) {
                        LOGGER.error("AIService: Failed to recalculate unit state", e);
                    }

                    // 4. Initial Mission
                    if (proposal.initialContract != null) {
                        try {
                            AtBContract contract = new AtBContract("Initial AI Mission");
                            contract.setEmployerCode(proposal.initialContract.employerCode, campaign.getGameYear());
                            contract.setEnemyCode(proposal.initialContract.enemyCode);
                            
                            try {
                                AtBContractType type = AtBContractType.valueOf(proposal.initialContract.missionType);
                                contract.setContractType(type);
                            } catch (IllegalArgumentException e) {
                                contract.setContractType(AtBContractType.GARRISON_DUTY);
                            }
                            
                            contract.setLength(proposal.initialContract.lengthMonths);
                            contract.setStartAndEndDate(campaign.getLocalDate());
                            contract.setDifficulty(proposal.initialContract.difficulty);
                            contract.setStatus(MissionStatus.ACTIVE);
                            contract.initContractDetails(campaign);
                            
                            Planet p = campaign.getLocation().getPlanet();
                            contract.setSystemId(p != null ? p.getId() : "Unknown System");
                            
                            campaign.addMission(contract);
                            LOGGER.info("AIService: Added initial mission: " + proposal.initialContract.missionType);
                        } catch (Exception e) {
                            LOGGER.error("AIService: Failed to setup initial contract", e);
                        }
                    }
                    
                    MekHQ.setPendingAiProposal(null);
                    LOGGER.info("AIService: AI initialization complete.");
                }
                // endregion AI Initialization

                // initialize reputation
                if (campaign.getReputation() == null) {
                    ReputationController reputationController = new ReputationController();
                    reputationController.initializeReputation(campaign);
                    campaign.setReputation(reputationController);
                } else {
                    campaign.getReputation().initializeReputation(campaign);
                }

                // initialize starting faction standings
                CampaignOptions campaignOptions = campaign.getCampaignOptions();
                if (campaignOptions.isTrackFactionStanding()) {
                    FactionStandings factionStandings = campaign.getFactionStandings();
                    String report = factionStandings.updateClimateRegard(campaign.getFaction(),
                          campaign.getLocalDate(), campaignOptions.getRegardMultiplier(),
                          true);
                    campaign.addReport(POLITICS, report);
                }
                // endregion Progress 6

                // region progress 7
                setProgress(7);
                campaign.beginReport("<b>" +
                                           MekHQ.getMHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) +
                                           "</b>");

                // Setup Personnel Modules
                campaign.setMarriage(campaignOptions
                                           .getRandomMarriageMethod()
                                           .getMethod(campaignOptions));
                campaign.setDivorce(campaignOptions
                                          .getRandomDivorceMethod()
                                          .getMethod(campaignOptions));
                campaign.setProcreation(campaignOptions
                                              .getRandomProcreationMethod()
                                              .getMethod(campaignOptions));

                // Setup Markets
                campaign.refreshPersonnelMarkets(true);
                ContractMarketMethod contractMarketMethod = campaignOptions.getContractMarketMethod();
                campaign.setContractMarket(contractMarketMethod.getContractMarket());
                if (!contractMarketMethod.isNone()) {
                    campaign.getContractMarket().generateContractOffers(campaign, true);
                }
                if (!campaignOptions.getUnitMarketMethod().isNone()) {
                    campaign.setUnitMarket(campaignOptions.getUnitMarketMethod().getUnitMarket());
                    campaign.getUnitMarket().generateUnitOffers(campaign);
                }

                // News
                campaign.reloadNews();
                campaign.readNews();

                // GM Mode
                campaign.setGMMode((preset == null) || preset.isGM());

                // AtB
                if (campaignOptions.isUseAtB()) {
                    campaign.initAtB(true);
                }

                // Turnover
                campaign.initTurnover();
                // endregion Progress 7
            } else {
                // region progress 6
                LOGGER.info("Loading campaign file from XML file {}", getCampaignFile());

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(getCampaignFile())) {
                    campaign = CampaignFactory.newInstance(getApplication()).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
                // endregion Progress 6

                // region progress 7
                setProgress(7);

                unassignCrewFromUnsupportedUnits(campaign.getUnits());

                // <50.10 compatibility handler
                final Version campaignVersion = campaign.getVersion();
                if (campaignVersion.isLowerThan(new Version("0.50.10"))) {
                    new WarAndPeaceProcessor(campaign, true);
                }

                // Campaign upgrading
                // This needs to be the final stage in Progress 7 as otherwise the display of any confirmation
                // dialogs will get 'stuck' behind other dialogs
                if (campaignVersion.isLowerThan(MHQConstants.VERSION)) {
                    handleCampaignUpgrading(campaign);
                }
                // endregion Progress 7
            }

            if (isNewCampaign) {
                new WarAndPeaceProcessor(campaign, true);
            }

            // Generic event processor
            campaign.setCampaignEventProcessor(new CampaignEventProcessor(campaign));

            campaign.setApp(getApplication());
            return campaign;
        }

        /**
         * Handles the upgrade process for a campaign in a thread-safe and blocking manner.
         *
         * <p>This method initiates the campaign upgrade dialog for the specified {@link Campaign}. While the upgrade
         * is in progress, the method blocks further execution by using a {@link CountDownLatch}. This ensures that
         * campaign loading or other interactions do not proceed while the campaign data may be in an inconsistent,
         * mid-upgrade state, which can prevent a variety of random and challenging-to-debug errors.</p>
         *
         * <p>Once the upgrade dialog completes, the provided callback triggers an {@link OptionsChangedEvent}
         * and signals the latch, allowing the method to return.</p>
         *
         * <p><b>Note:</b> This method should not be called from the Event Dispatch Thread (EDT), as it will block
         * the thread until the upgrade is finished.</p>
         *
         * @param campaign the {@link Campaign} instance to be upgraded
         *
         * @author Illiani
         * @since 0.50.07
         */
        private static void handleCampaignUpgrading(Campaign campaign) {
            CampaignUpgradeDialog.campaignUpgradeDialog(campaign);
        }

        /**
         * Unassigns the crew from unsupported units in the given collection of units.
         *
         * <p>This method iterates through the provided {@link Collection} of {@link Unit} objects
         * and checks if each unit's associated {@link Entity} is of an unsupported type.
         *
         * <p>If the entity is {@code null}, it is skipped. For unsupported unit types, the unit's
         * crew is cleared using {@link Unit#clearCrew()}.</p>
         *
         * @param units The {@link Collection} of {@link Unit} instances to process. Must not be {@code null}.
         */
        private void unassignCrewFromUnsupportedUnits(Collection<Unit> units) {
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (entity == null) {
                    continue;
                }

                if (isUnsupportedEntity(entity)) {
                    unit.clearCrew();
                }
            }
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Campaign campaign;
            try {
                campaign = get();
            } catch (InterruptedException | CancellationException ignored) {
                campaign = null;
            } catch (ExecutionException ex) {
                LOGGER.error("", ex);
                if (ex.getCause() instanceof NullEntityException) {
                    JOptionPane.showMessageDialog(null,
                          String.format(resources.getString("DataLoadingDialog.NullEntityException.text"),
                                ex.getCause().getMessage()),
                          resources.getString("DataLoadingDialog.NullEntityException.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof NullPointerException) {
                    JOptionPane.showMessageDialog(null,
                          String.format(resources.getString("DataLoadingDialog.NullPointerException.text"),
                                ex.getCause().getMessage()),
                          resources.getString("DataLoadingDialog.NullPointerException.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof OutOfMemoryError) {
                    JOptionPane.showMessageDialog(null,
                          resources.getString("DataLoadingDialog.OutOfMemoryError.text"),
                          resources.getString("DataLoadingDialog.OutOfMemoryError.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                          resources.getString("DataLoadingDialog.ExecutionException.text"),
                          resources.getString("DataLoadingDialog.ExecutionException.title"),
                          JOptionPane.ERROR_MESSAGE);
                }
                campaign = null;
            }

            setVisible(false);
            if (campaign != null) {
                getApplication().setCampaign(campaign);
                getApplication().getCampaignController().setHost(campaign.getId());
                getApplication().showNewView();
                getFrame().dispose();
                if (null != storyArcStub) {
                    StoryArc storyArc = storyArcStub.loadStoryArc(campaign);
                    if (null != storyArc) {
                        campaign.useStoryArc(storyArc, true);
                    }
                }
            } else {
                cancel(true);
                getFrame().setVisible(true);
            }
        }
    }
}
