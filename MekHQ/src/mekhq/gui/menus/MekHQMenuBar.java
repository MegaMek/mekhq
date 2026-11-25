/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

package mekhq.gui.menus;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;

import megamek.Version;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.CopySystemDataAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
import megamek.client.ui.util.MULVersionValidator;
import megamek.common.event.Subscribe;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.divorce.RandomDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.marriage.RandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitAcquisitionType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Systems;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.commandGeneration.CommandGenerationDialog;
import mekhq.gui.developerTools.ContractDefinitionEditorDialog;
import mekhq.gui.developerTools.FactionStandingUltimatumEditorDialog;
import mekhq.gui.developerTools.ScenarioModifierEditorDialog;
import mekhq.gui.developerTools.StratConFacilityEditorDialog;
import mekhq.gui.developerTools.StratConPointOfInterestEditorDialog;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog;
import mekhq.gui.dialog.releaseNotes.ReleaseNotesConsole;
import mekhq.gui.dialog.reportDialogs.CargoReportDialog;
import mekhq.gui.dialog.reportDialogs.ChaosReputationReportDialog;
import mekhq.gui.dialog.reportDialogs.HangarReportDialog;
import mekhq.gui.dialog.reportDialogs.PersonnelReportDialog;
import mekhq.gui.dialog.reportDialogs.ReputationReportDialog;
import mekhq.gui.dialog.reportDialogs.TransportReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.scenarioTemplateEditor.ScenarioTemplateEditorDialog;
import mekhq.io.FileType;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MekHQMenuBar extends JMenuBar {

    private static final MMLogger logger = MMLogger.create(MekHQMenuBar.class);
    private static final String MENU_ITEM_TEXT_SUFFIX = ".text";
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MekHQMenuBar";

    private final CampaignGUI gui;
    private final MekHQ app;

    private JMenu menuThemes;
    private JMenuItem miRetirementDefectionDialog;
    private JMenuItem miAwardEligibilityDialog;
    private JMenuItem miCommandGenerator;
    private JMenuItem miPlanetarySystemEditor;

    /**
     * This is used to initialize the top menu bar.
     * <p>
     * <b>Keyboard shortcut policy:</b>
     * <ul>
     *   <li>Alt+key (mnemonic) is reserved for things reachable from the main window: the top-level menus, the
     *   {@link MHQTabType} tabs and the Advance Day button. These must be unique. The keys in use are: A (Advance Day),
     *   B, C, H, I, L, N, O, P, R, T, W (tabs), D, E, F, G, M, V (top-level menus, G being Manage Campaign) and
     *   {@code /} (Help, as it is normally the same key as the ?).</li>
     *   <li>Menu items and sub-menus deliberately have <i>no</i> mnemonics. They are only ever usable while their
     *   parent menu is open, are rarely used, and reusing letters made them mislabelled and duplicated.</li>
     *   <li>Frequently used actions instead get global accelerators, which work from the main window without opening a
     *   menu: Ctrl+S/L/N (save, load, new), and Ctrl+Shift (Cmd+Shift on macOS) +O (Campaign Options), +H (MekHQ
     *   Options), +M (MegaMek Options), +G (MegaMek Client Options), +P (Recruitment), +C (Contract Market), +U (Unit
     *   Market) and +D (Historical Daily Report Log).</li>
     * </ul>
     */
    public MekHQMenuBar(MekHQ app, CampaignGUI gui) {
        super();
        this.app = app;
        this.gui = gui;

        getAccessibleContext().setAccessibleName("Main Menu");

        add(initFileMenu());
        add(initMarketMenu());
        add(initReportsMenu());
        add(initViewMenu());
        add(initManageCampaignMenu());
        add(initDeveloperToolsMenu());
        add(initHelpMenu());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        MekHQ.registerHandler(this);
    }

    @Override
    public void removeNotify() {
        MekHQ.unregisterHandler(this);
        super.removeNotify();
    }

    private MekHQ getApplication() {
        return app;
    }

    private CampaignGUI getGui() {
        return gui;
    }

    private Campaign getCampaign() {
        return getGui().getCampaign();
    }

    private JFrame getFrame() {
        return getGui().getFrame();
    }

    private EnhancedTabbedPane getTabMain() {
        return getGui().getTabMain();
    }

    /**
     * The File menu. Items have no mnemonics; see {@link #MekHQMenuBar(MekHQ, CampaignGUI)}.
     */
    private JMenu initFileMenu() {
        // TODO : Implement "Export All" versions for Personnel and Parts
        JMenu menuFile = new JMenu(getTextAt("fileMenu.text"));
        menuFile.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuLoad = createMenuItem("menuLoad.text", event -> {
            final File file = FileDialogs.openCampaign(getFrame()).orElse(null);
            if (file == null) {
                return;
            }
            getFrame().setVisible(false); // hide CampaignGUI
            // load the campaign
            new DataLoadingDialog(getFrame(), getApplication(), file, campaign -> {
                if (campaign != null) {
                    getApplication().activateCampaign(campaign);
                } else {
                    getFrame().setVisible(true); // return back to CampaignGUI if loading fails
                }
            }).setVisible(true);
        });
        menuLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuLoad);

        JMenuItem menuSave = createMenuItem("menuSave.text", getGui()::saveCampaign);
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuSave);

        JMenuItem menuNew = createMenuItem("menuNew.text", event -> handleInAppNewCampaign());
        menuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuNew);

        menuFile.add(initImportMenu());
        menuFile.add(initExportMenu());
        menuFile.add(initRefreshMenu());

        JMenuItem menuOptions = createMenuItem("menuOptions.text", this::menuOptionsActionPerformed);
        setShiftShortcut(menuOptions, KeyEvent.VK_O);
        menuFile.add(menuOptions);

        JMenuItem miMHQOptions = createMenuItem("miMHQOptions.text",
              event -> new MHQOptionsTreeDialog(getFrame()).setVisible(true));
        setShiftShortcut(miMHQOptions, KeyEvent.VK_H);
        miMHQOptions.setToolTipText(getTextAt("miMHQOptions.toolTipText"));
        menuFile.add(miMHQOptions);

        final JMenuItem miGameOptions = createMenuItem("miGameOptions.text", event -> {
            final GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
            god.setEditable(true);
            if (god.showDialog().isConfirmed()) {
                getCampaign().setGameOptions(god.getOptions());
                getGui().refreshWindowTitle();
            }
        });
        setShiftShortcut(miGameOptions, KeyEvent.VK_M);
        miGameOptions.setToolTipText(getTextAt("miGameOptions.toolTipText"));
        menuFile.add(miGameOptions);

        final JMenuItem miMMClientOptions = createMenuItem("miMMClientOptions.text",
              event -> new CommonSettingsDialog(getFrame(), null).setVisible(true));
        setShiftShortcut(miMMClientOptions, KeyEvent.VK_G);
        miMMClientOptions.setToolTipText(getTextAt("miMMClientOptions.toolTipText"));
        menuFile.add(miMMClientOptions);

        menuThemes = new JMenu(getTextAt("menuThemes.text"));
        refreshThemeChoices();
        menuFile.add(menuThemes);

        menuFile.add(createMenuItem("menuExit.text", event -> getApplication().exit(true)));

        return menuFile;
    }

    /**
     * The Import menu. Items have no mnemonics.
     */
    private JMenu initImportMenu() {
        JMenu menuImport = new JMenu(getTextAt("menuImport.text"));

        menuImport.add(createMenuItem("miImportPerson.text", event -> loadPersonFile()));

        JMenuItem miImportIndividualRankSystem = createMenuItem("miImportIndividualRankSystem.text", event -> {
                  Campaign campaign = getCampaign();
                  final RankSystem rankSystem = RankSystem.generateIndividualInstanceFromXML(
                        FileDialogs.openIndividualRankSystem(getFrame()).orElse(null));
                  campaign.getPlayerForce().setRankSystem(rankSystem);
              });
        miImportIndividualRankSystem.setToolTipText(getTextAt("miImportIndividualRankSystem.toolTipText"));
        menuImport.add(miImportIndividualRankSystem);

        menuImport.add(createMenuItem("miImportParts.text", event -> loadPartsFile()));
        menuImport.add(createMenuItem("miLoadForces.text", event -> loadListFile(true)));

        return menuImport;
    }

    /**
     * The Export menu. Items have no mnemonics.
     */
    private JMenu initExportMenu() {
        JMenu menuExport = new JMenu(getTextAt("menuExport.text"));

        // region CSV Export
        JMenu miExportCSVFile = new JMenu(getTextAt("menuExportCSV.text"));

        JMenuItem miExportPersonnel = createMenuItem("miExportPersonnel.text", event -> exportPersonnel());
        miExportPersonnel.setToolTipText(getTextAt("miExportPersonnel.toolTipText"));
        miExportCSVFile.add(miExportPersonnel);

        JMenuItem miExportUnit = createMenuItem("miExportUnit.text", event -> exportUnits());
        miExportUnit.setToolTipText(getTextAt("miExportUnit.toolTipText"));
        miExportCSVFile.add(miExportUnit);

        JMenuItem miExportFinances = createMenuItem("miExportFinances.text", event -> exportFinances());
        miExportFinances.setToolTipText(getTextAt("miExportFinances.toolTipText"));
        miExportCSVFile.add(miExportFinances);

        menuExport.add(miExportCSVFile);
        // endregion CSV Export

        // region XML Export
        JMenu miExportXMLFile = new JMenu(getTextAt("menuExportXML.text"));

        miExportXMLFile.add(createMenuItem("miExportRankSystems.text",
              event -> mekhq.campaign.personnel.ranks.Ranks.exportRankSystemsToFile(mekhq.gui.FileDialogs.saveRankSystems(
                          getFrame()).orElse(null),
                    getCampaign().getPlayerForce().getRankSystem())));

        miExportXMLFile.add(createMenuItem("miExportIndividualRankSystem.text", event -> getCampaign().getPlayerForce()
                           .getRankSystem()
                           .writeToFile(FileDialogs.saveIndividualRankSystem(getFrame()).orElse(null))));

        JMenuItem miExportPlanetsXML = createMenuItem("miExportPlanets.text", event -> {
            try {
                exportPlanets(FileType.XML,
                      getTextAt("dlgSavePlanetsXML.text"),
                      getCampaign().getPlayerForce().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedPlanets");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportXMLFile.add(miExportPlanetsXML);

        menuExport.add(miExportXMLFile);
        // endregion XML Export

        JMenuItem miExportCampaignSubset = createMenuItem("miExportCampaignSubset.text", event -> {
            CampaignExportWizard cew = new CampaignExportWizard(getApplication(), getCampaign());
            cew.display(CampaignExportWizard.CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        return menuExport;
    }

    /**
     * The Refresh menu. Items have no mnemonics.
     */
    private JMenu initRefreshMenu() {
        JMenu menuRefresh = new JMenu(getTextAt("menuRefresh.text"));

        menuRefresh.add(createMenuItem("miRefreshUnitCache.text", event -> MekSummaryCache.refreshUnitData(false)));
        menuRefresh.add(createMenuItem("miRefreshCamouflage.text", event -> {
            MHQStaticDirectoryManager.refreshCamouflageDirectory();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshPortraits.text", event -> {
            MHQStaticDirectoryManager.refreshPortraitDirectory();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshFormationIcons.text", event -> {
            MHQStaticDirectoryManager.refreshFormationIcons();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshAwards.text", event -> {
            MHQStaticDirectoryManager.refreshAwardIcons();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshStoryIcons.text", event -> {
            MHQStaticDirectoryManager.refreshStorySplash();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshRanks.text", event -> Ranks.reinitializeRankSystems(getCampaign())));

        JMenuItem miRefreshFinancialInstitutions = createMenuItem("miRefreshFinancialInstitutions.text",
              event -> FinancialInstitutions.initializeFinancialInstitutions());
        miRefreshFinancialInstitutions.setToolTipText(getTextAt("miRefreshFinancialInstitutions.toolTipText"));
        menuRefresh.add(miRefreshFinancialInstitutions);

        return menuRefresh;
    }

    /**
     * The Marketplace menu. Items have no mnemonics, but the main markets have global accelerators.
     */
    private JMenu initMarketMenu() {
        JMenu menuMarket = new JMenu(getTextAt("menuMarket.text"));
        menuMarket.setMnemonic(KeyEvent.VK_M);

        JMenuItem miRecruitment = createMenuItem("miRecruitment.text", event -> getGui().openRecruitmentDialog());
        setShiftShortcut(miRecruitment, KeyEvent.VK_P);
        menuMarket.add(miRecruitment);

        // The accelerator stays active while the item is hidden, so the action re-checks the campaign options
        JMenuItem miContractMarket = createMenuItem("miContractMarket.text", event -> {
            if (getCampaign().getCampaignOptions().isUseStratCon()) {
                getGui().showContractMarket();
            }
        });
        setShiftShortcut(miContractMarket, KeyEvent.VK_C);
        miContractMarket.setVisible(getCampaign().getCampaignOptions().isUseStratCon());
        menuMarket.add(miContractMarket);

        JMenuItem miUnitMarket = createMenuItem("miUnitMarket.text", event -> {
            if (!getCampaign().getUnitMarket().getMethod().isNone()) {
                getGui().showUnitMarket();
            }
        });
        setShiftShortcut(miUnitMarket, KeyEvent.VK_U);
        miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        menuMarket.add(miUnitMarket);

        JMenuItem miPurchaseUnit = createMenuItem("miPurchaseUnit.text", event -> {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(getFrame());
            if (!MekSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(
                  getFrame(), unitLoadingDialog, getCampaign(), true);
            usd.setVisible(true);
        });
        menuMarket.add(miPurchaseUnit);

        menuMarket.add(createMenuItem("miBuyParts.text",
              event -> new PartsStoreDialog(true, getGui()).setVisible(true)));

        menuMarket.add(createMenuItem("miBulkRecruitment.text", event -> getGui().openBulkRecruitmentDialog()));

        JMenu menuRecruitment = new JMenu(getTextAt("menuRecruitment.text"));

        menuRecruitment.add(createMenuItem("menuRecruitment.blank.text", this::addBlankPerson));

        JMenu menuCombatRecruitment = new JMenu(getTextAt("menuRecruitment.combat"));
        JMenu menuSupportRecruitment = new JMenu(getTextAt("menuRecruitment.support"));
        JMenu menuCivilianRecruitment = new JMenu(getTextAt("menuRecruitment.civilian"));

        PersonnelRole[] roles = PersonnelRole.getValuesSortedAlphabetically(getCampaign().getPlayerForce().isClanForce());
        for (PersonnelRole role : roles) {
            JMenuItem miRoleRecruitment = new JMenuItem(role.getLabel(getCampaign().getPlayerForce().getFaction().isClan()));
            miRoleRecruitment.setToolTipText(role.getDescription(getCampaign().getPlayerForce().isClanForce()));
            miRoleRecruitment.setActionCommand(role.name());
            miRoleRecruitment.addActionListener(this::hirePerson);

            if (role.isCombat()) {
                menuCombatRecruitment.add(miRoleRecruitment);
            } else if (role.isSupport(true)) {
                menuSupportRecruitment.add(miRoleRecruitment);
            } else if (role.isDependent()) {
                // Dependent is handled specially so that it's always at the top of the civilian category
                menuCivilianRecruitment.insert(miRoleRecruitment, 0);
            } else {
                menuCivilianRecruitment.add(miRoleRecruitment);
            }
        }

        menuRecruitment.add(menuCombatRecruitment);
        menuRecruitment.add(menuSupportRecruitment);
        menuRecruitment.add(menuCivilianRecruitment);
        menuMarket.add(menuRecruitment);

        menuMarket.add(new TemporaryPersonnelManagementMenu(getCampaign(), (title, maxCount) -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(getFrame(), true, title, 1, 0, maxCount);
            dialog.setVisible(true);
            return dialog.getValue();
        }));

        return menuMarket;
    }


    /**
     * The Reports menu. Items have no mnemonics.
     */
    private JMenu initReportsMenu() {
        JMenu menuReports = new JMenu(getTextAt("menuReports.text"));
        menuReports.setMnemonic(KeyEvent.VK_E);
        menuReports.add(createMenuItem("miDragoonsRating.text", event -> {
            if (getCampaign().getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION)) {
                new ChaosReputationReportDialog(getFrame(), getCampaign()).setVisible(true);
            } else {
                new ReputationReportDialog(getFrame(), getCampaign()).setVisible(true);
            }
        }));
        menuReports.add(createMenuItem("miPersonnelReport.text",
              event -> new PersonnelReportDialog(getFrame(), new PersonnelReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miHangarBreakdown.text",
              event -> new HangarReportDialog(getFrame(), new HangarReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miTransportReport.text",
              event -> new TransportReportDialog(getFrame(), new TransportReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miCargoReport.text",
              event -> new CargoReportDialog(getFrame(), new CargoReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miAlmanac.text", event -> new WarriorsAlmanacDialog(getCampaign(), false)));
        return menuReports;
    }

    /**
     * The View menu. Items have no mnemonics, but the Historical Daily Report Log has a global accelerator.
     */
    private JMenu initViewMenu() {
        JMenu menuView = new JMenu(getTextAt("menuView.text"));
        menuView.setMnemonic(KeyEvent.VK_V);

        JMenuItem miNavigationMap = createMenuItem("miNavigationMap.text", evt -> {
            getGui().setSelectedTab(getGui().getNavigationTab());
            getGui().getNavigationTab().showInterstellarMap();
        });
        miNavigationMap.setToolTipText(getTextAt("miNavigationMap.toolTipText"));
        miNavigationMap.getAccessibleContext().setAccessibleName(miNavigationMap.getText());
        miNavigationMap.getAccessibleContext().setAccessibleDescription(miNavigationMap.getToolTipText());
        menuView.add(miNavigationMap);
        menuView.addSeparator();

        JMenuItem miHistoricalDailyReportDialog = createMenuItem("miShowHistoricalReportLog.text", event -> {
            HistoricalDailyReportDialog histDailyReportDialog = new HistoricalDailyReportDialog(getFrame(),
                  getGui());
            histDailyReportDialog.setModal(true);
            histDailyReportDialog.setVisible(true);
            histDailyReportDialog.dispose();
        });
        setShiftShortcut(miHistoricalDailyReportDialog, KeyEvent.VK_D);
        menuView.add(miHistoricalDailyReportDialog);

        miRetirementDefectionDialog = createMenuItem("miRetirementDefectionDialog.text",
              event -> getGui().showRetirementDefectionDialog());
        miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().get(CampaignOption.USE_RANDOM_RETIREMENT));
        menuView.add(miRetirementDefectionDialog);

        miAwardEligibilityDialog = createMenuItem("miAwardEligibilityDialog.text",
              event -> showAwardEligibilityDialog());
        miAwardEligibilityDialog.setVisible(getCampaign().getCampaignOptions().get(CampaignOption.ENABLE_AUTO_AWARDS));
        menuView.add(miAwardEligibilityDialog);

        return menuView;
    }

    private JMenu initManageCampaignMenu() {
        JMenu menuManage = new JMenu(getTextAt("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_G);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = createMenuItem("miGMToolsDialog.text",
              event -> new GMToolsDialog(getFrame(), getGui(), null).setVisible(true));
        menuManage.add(miGMToolsDialog);

        miPlanetarySystemEditor = createMenuItem("miPlanetarySystemEditor.text",
              event -> new PlanetarySystemEditorDialog(getFrame(), getCampaign()).setVisible(true));
        miPlanetarySystemEditor.setVisible(getCampaign().isGM());
        menuManage.add(miPlanetarySystemEditor);

        JMenuItem miBloodnames = createMenuItem("miRandomBloodnames.text", event -> {
            for (final Person person : getCampaign().getPlayerForce().getHumanResources().getPersonnel()) {
                Campaign campaign = getCampaign();
                campaign.getPlayerForce().getHumanResources().checkBloodnameAdd(campaign, person, false);
            }
        });
        menuManage.add(miBloodnames);

        miCommandGenerator = createMenuItem("miCommandGenerator.text",
              event -> new CommandGenerationDialog(getFrame(), getCampaign()).setVisible(true));
        miCommandGenerator.setVisible(MekHQ.getMHQOptions().getShowCommandGenerator());
        menuManage.add(miCommandGenerator);

        JMenuItem miAutoResolveBehaviorEditor = createMenuItem("miAutoResolveBehaviorSettings.text", event -> {
                  var autoResolveBehaviorSettingsDialog = new AutoResolveBehaviorSettingsDialog(getFrame(),
                        getCampaign());
                  autoResolveBehaviorSettingsDialog.setVisible(true);
                  autoResolveBehaviorSettingsDialog.pack();
              });

        menuManage.add(miAutoResolveBehaviorEditor);

        return menuManage;
    }

    /**
     * Builds the "Developer Tools" menu, which groups the data-file editors: the scenario template editor and the new
     * scenario modifier and contract definition editors.
     */
    private JMenu initDeveloperToolsMenu() {
        JMenu menuDeveloperTools = new JMenu(getTextAt("menuDeveloperTools.text"));
        menuDeveloperTools.setMnemonic(KeyEvent.VK_D);
        menuDeveloperTools.setName("developerToolsMenu");

        JMenuItem miScenarioEditor = createMenuItem("miScenarioEditor.text",
              event -> new ScenarioTemplateEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miScenarioEditor);

        JMenuItem miScenarioModifierEditor = createMenuItem("miScenarioModifierEditor.text",
              event -> new ScenarioModifierEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miScenarioModifierEditor);

        JMenuItem miContractDefinitionEditor = createMenuItem("miContractDefinitionEditor.text",
              event -> new ContractDefinitionEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miContractDefinitionEditor);

        JMenuItem miFacilityEditor = createMenuItem("miFacilityEditor.text",
              event -> new StratConFacilityEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miFacilityEditor);

        JMenuItem miPointOfInterestEditor = createMenuItem("miPointOfInterestEditor.text",
              event -> new StratConPointOfInterestEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miPointOfInterestEditor);

        JMenuItem miFactionStandingUltimatumEditor = createMenuItem("miFactionStandingUltimatumEditor.text",
              event -> new FactionStandingUltimatumEditorDialog(getFrame()).setVisible(true));
        menuDeveloperTools.add(miFactionStandingUltimatumEditor);

        JMenuItem miLifePathBuilder = createMenuItem("miLifePathBuilder.text",
              event -> new LifePathBuilderDialog(getCampaign(), getFrame()));
        menuDeveloperTools.add(miLifePathBuilder);

        return menuDeveloperTools;
    }

    /**
     * The Help menu. Items have no mnemonics.
     */
    private JMenu initHelpMenu() {
        JMenu menuHelp = new JMenu(getTextAt("menuHelp.text"));
        menuHelp.setMnemonic(KeyEvent.VK_SLASH);
        menuHelp.setName("helpMenu");

        menuHelp.addSeparator();

        menuHelp.add(createMenuItem("menuReportBug.text", event -> new EasyBugReportDialog(getFrame(), getCampaign())));
        menuHelp.add(new CopySystemDataAction(MHQConstants.PROJECT_NAME));

        menuHelp.addSeparator();

        JMenuItem releaseNotes = createMenuItem("menuReleaseNotes.text",
              event -> ReleaseNotesConsole.showReleaseNotes(getFrame()));
        releaseNotes.setToolTipText(getTextAt("menuReleaseNotes.toolTipText"));
        menuHelp.add(releaseNotes);
        menuHelp.add(createMenuItem("menuAbout.text", event -> new MekHQAboutDialog(getFrame()).show()));

        return menuHelp;
    }


    /**
     * Exports Planets to a file (CSV, XML, etc.)
     *
     */
    protected void exportPlanets(FileType format, String dialogTitle, String filename) {
        // TODO: Fix this
        /*
         * GUI.fileDialogSave(
         * frame,
         * dialogTitle,
         * format,
         * MekHQ.getPlanetsDirectory().getValue(),
         * "planets." + format.getRecommendedExtension())
         * .ifPresent(f -> {
         * MekHQ.getPlanetsDirectory().setValue(f.getParent());
         * File file = checkFileEnding(f, format.getRecommendedExtension());
         * checkToBackupFile(file, file.getPath());
         * String report = Planets.getInstance().exportPlanets(file.getPath(),
         * format.getRecommendedExtension());
         * JOptionPane.showMessageDialog(mainPanel, report);
         * });
         *
         * GUI.fileDialogSave(getFrame(), dialogTitle, new File(".", "planets." +
         * format.getRecommendedExtension()), format).ifPresent(f -> {
         * File file = checkFileEnding(f, format.getRecommendedExtension());
         * checkToBackupFile(file, file.getPath());
         * String report = Planets.getInstance().exportPlanets(file.getPath(),
         * format.getRecommendedExtension());
         * JOptionPane.showMessageDialog(mainPanel, report);
         * });
         */
    }

    /**
     * Exports Personnel to a CSV file.
     */
    private void exportPersonnel() {
        JTable table = getGui().getPersonnelTab().getPersonnelTable();
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(getFrame(), getTextAt("dlgNoPersonnel.text"));
            return;
        }
        FileDialogs.savePersonnelCSV(getFrame(), getCampaign())
              .map(file -> Utilities.exportTableToCSV(table, file))
              .ifPresent(status -> JOptionPane.showMessageDialog(getFrame(), status));
    }

    /**
     * Exports Units to a CSV file.
     */
    private void exportUnits() {
        JTable table = getGui().getHangarTab().getUnitTable();
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(getFrame(), getTextAt("dlgNoUnits.text"));
            return;
        }
        FileDialogs.saveUnitsCSV(getFrame(), getCampaign())
              .map(file -> Utilities.exportTableToCSV(table, file))
              .ifPresent(status -> JOptionPane.showMessageDialog(getFrame(), status));
    }

    /**
     * Exports Finances to a CSV file.
     */
    private void exportFinances() {
        Finances finances = getCampaign().getPlayerForce().getFinances();
        if (finances.getTransactions().isEmpty()) {
            JOptionPane.showMessageDialog(getFrame(), getTextAt("dlgNoFinances.text"));
            return;
        }
        FileDialogs.saveFinancesCSV(getFrame(), getCampaign())
              .map(file -> finances.exportFinancesToCSV(file.getPath(), FileType.CSV.getRecommendedExtension()))
              .ifPresent(status -> JOptionPane.showMessageDialog(getFrame(), status));
    }

    protected void loadListFile(final boolean allowNewPilots) {
        final File unitFile = FileDialogs.openUnits(getFrame()).orElse(null);

        PartQuality quality = PartQuality.QUALITY_D;

        if (getCampaign().getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES)) {
            quality = Unit.getRandomUnitQuality(0);
        }

        if (unitFile != null) {
            try {
                final MULParser parser = new MULParser(unitFile, getCampaign().getGameOptions());
                if (!MULVersionValidator.isCorrectVersion(getFrame(), parser)) {
                    return;
                }
                for (Entity entity : parser.getEntities()) {
                    getCampaign().addNewUnit(entity, allowNewPilots, 0, quality, UnitAcquisitionType.GM_ADDED);
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    protected void loadPersonFile() {
        File personnelFile = FileDialogs.openPersonnel(getFrame()).orElse(null);

        if (personnelFile != null) {
            logger.info("Starting load of personnel file from XML...");
            // Initialize variables.
            Document xmlDoc;

            // Open the file
            try (InputStream is = new FileInputStream(personnelFile)) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(is);
            } catch (Exception ex) {
                logger.error("Cannot load person XML", ex);
                return; // otherwise we NPE out in the next line
            }

            Element personnelEle = xmlDoc.getDocumentElement();
            NodeList nl = personnelEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            personnelEle.normalize();

            final Version version = new Version(personnelEle.getAttribute("version"));
            final List<Person> importedPersonnel = new ArrayList<>();

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                    logger.error("Unknown node type not loaded in Personnel nodes: {}", wn2.getNodeName());
                    continue;
                }

                Person person = Person.generateInstanceFromXML(wn2, getCampaign(), version);
                if ((person != null)) {
                    Campaign campaign = getCampaign();
                    final UUID id = person.getId();
                    if (campaign.getPlayerForce().getHumanResources().getPerson(id) != null) {
                        logger.error("ERROR: Cannot load person who exists, ignoring. (Name: {}, Id {})",
                              person.getFullName(),
                              person.getId());
                        person = null;
                    }
                }

                if (person != null) {
                    Campaign campaign = getCampaign();
                    PrisonerStatus prisonerStatus = person.getPrisonerStatus();
                    boolean employ = person.isEmployed();
                    campaign.getPlayerForce()
                          .getHumanResources()
                          .recruitPerson(campaign, person, prisonerStatus, true, false, employ,
                                true);

                    // Clear some values we no longer should have set in case this
                    // has transferred campaigns or things in the campaign have
                    // changed...
                    person.setUnit(null);
                    person.clearTechUnits();

                    // <51.01 compatibility handler: an older export may hold a Natural Aptitude SPA we couldn't convert
                    person.reportUnresolvedLegacyNaturalAptitudes(campaign);
                    importedPersonnel.add(person);
                }
            }

            // Imported relatives are only ID placeholders until resolved against the campaign; relatives that weren't
            // exported alongside them (e.g., one half of a couple) are dropped here
            // TODO : make it so that exports will automatically include both spouses
            PersonIdReference.fixImportedPersonnelReferences(getCampaign(), importedPersonnel);

            Campaign campaign2 = getCampaign();
            for (Person p : campaign2.getPlayerForce().getHumanResources().getActivePersonnel(true, true)) {
                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null)) {
                        Campaign campaign = getCampaign();
                        if (campaign.getPlayerForce().getHumanResources().getPerson(fatherId) == null) {
                            p.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, null);
                        }
                    }
                }
            }

            logger.info("Finished load of personnel file");
        }
    }

    /**
     * Handles a new campaign event triggered from within an existing getCampaign().
     * <p>
     * This method performs the following actions in sequence:
     * <ul>
     * <li>Prompts the user to save any current progress through a confirmation
     * dialog.</li>
     * <li>If the user chooses to cancel or closes the dialog, the operation is
     * aborted.</li>
     * <li>If the user agrees to save and the save operation fails, the operation is
     * aborted.</li>
     * <li>Unregisters all event handlers associated with the current campaign,
     * including those
     * in the CampaignGUI and tabs.</li>
     * <li>Starts a new campaign by displaying a data loading dialog.</li>
     * </ul>
     * </p>
     */
    private void handleInAppNewCampaign() {
        int decision = new NewCampaignConfirmationDialog().YesNoOption();
        if (decision != JOptionPane.YES_OPTION) {
            return;
        }

        // Prompt the user to save
        int savePrompt = JOptionPane.showConfirmDialog(null,
              getTextAt("savePrompt.text"),
              getTextAt("savePrompt.title"),
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);

        // Abort if the user cancels, closes the dialog, or fails to save
        if (savePrompt == JOptionPane.CANCEL_OPTION ||
                  savePrompt == JOptionPane.CLOSED_OPTION ||
                  (savePrompt == JOptionPane.YES_OPTION && !getGui().saveCampaign(null))) {
            return;
        }
        getFrame().setVisible(false); // hide CampaignGUI
        // start a new campaign
        new DataLoadingDialog(getFrame(), getApplication(), null, true, campaign -> {
            if (campaign != null) {
                getApplication().activateCampaign(campaign);
            } else {
                getFrame().setVisible(true); // return back to CampaignGUI if creation fails
            }
        }).setVisible(true);
    }


    public void showAwardEligibilityDialog() {
        AutoAwardsController autoAwardsController = new AutoAwardsController();
        autoAwardsController.ManualController(getCampaign(), true);
    }

    private void addBlankPerson(final ActionEvent event) {
        Person person = new Person(getCampaign(), Faction.MERCENARY_FACTION_CODE);
        person.setOriginPlanet(Systems.getInstance().getSystemById("Terra").getPrimaryPlanet());
        person.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);

        Campaign campaign = getCampaign();
        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, person, true, true);
    }

    private void hirePerson(final ActionEvent event) {
        Campaign campaign = getCampaign();
        final PersonnelRole role = PersonnelRole.valueOf(event.getActionCommand());
        NewRecruitDialog npd = new NewRecruitDialog(getGui(), true,
              campaign.getPlayerForce().getHumanResources().newPerson(campaign, role));
        npd.setVisible(true);
    }

    /**
     * Gives a menu item a global accelerator of the platform's menu shortcut key (Ctrl, or Cmd on macOS) plus Shift
     * plus the given key.
     *
     * @param item    the menu item
     * @param keyCode the {@link KeyEvent} key code
     */
    private static void setShiftShortcut(final JMenuItem item, final int keyCode) {
        item.setAccelerator(KeyStroke.getKeyStroke(keyCode,
              Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
    }

    /**
     * Creates and configures a {@link JMenuItem} with localized text and an action
     * listener. Component's name is generated by removing {@code ".text"} suffix from {@code textKey} parameter.
     *
     * @param textKey the resource key with the menu item's localized display text, must end with {@code ".text"}
     * @param action  the {@link ActionListener} to trigger when the menu item is selected
     *
     * @return the initialized {@code JMenuItem}
     */
    private static JMenuItem createMenuItem(String textKey, ActionListener action) {
        if (!textKey.endsWith(MENU_ITEM_TEXT_SUFFIX)) {
            throw new IllegalArgumentException(String.format("Text key must end with '%s', actual: '%s'",
                  MENU_ITEM_TEXT_SUFFIX, textKey));
        }
        JMenuItem item = new JMenuItem(getTextAt(textKey));
        item.setName(textKey.substring(0, textKey.length() - MENU_ITEM_TEXT_SUFFIX.length()));
        item.addActionListener(action);
        return item;
    }

    /**
     * @param event the event triggering the opening of the Campaign Options Dialog
     */
    private void menuOptionsActionPerformed(final ActionEvent event) {
        final CampaignOptions oldOptions = getCampaign().getCampaignOptions();
        // We need to handle it like this for now, as the options above get written to currently
        boolean atb = oldOptions.isUseStratCon();
        boolean factionIntroDate = oldOptions.get(CampaignOption.FACTION_INTRO_DATE);
        final RandomDivorceMethod randomDivorceMethod = oldOptions.get(CampaignOption.RANDOM_DIVORCE_METHOD);
        final RandomMarriageMethod randomMarriageMethod = oldOptions.get(CampaignOption.RANDOM_MARRIAGE_METHOD);
        final RandomProcreationMethod randomProcreationMethod = oldOptions.get(CampaignOption.RANDOM_PROCREATION_METHOD);

        CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(getFrame(), getCampaign());
        optionsDialog.setVisible(true);

        final CampaignOptions newOptions = getCampaign().getCampaignOptions();

        if (randomDivorceMethod != newOptions.get(CampaignOption.RANDOM_DIVORCE_METHOD)) {
            Campaign campaign = getCampaign();
            final AbstractDivorce divorce = newOptions.get(CampaignOption.RANDOM_DIVORCE_METHOD).getMethod(newOptions);
            campaign.getPlayerForce().getHumanResources().setDivorce(divorce);
        } else {
            AbstractDivorce divorce = getCampaign().getPlayerForce().getHumanResources().getDivorce();
            divorce.setUseClanPersonnelDivorce(newOptions.get(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE));
            divorce.setUsePrisonerDivorce(newOptions.get(CampaignOption.USE_PRISONER_DIVORCE));
            divorce.setUseRandomOppositeSexDivorce(newOptions.get(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE));
            divorce.setUseRandomSameSexDivorce(newOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE));
            divorce.setUseRandomClanPersonnelDivorce(newOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE));
            divorce.setUseRandomPrisonerDivorce(newOptions.get(CampaignOption.USE_RANDOM_PRISONER_DIVORCE));
            if (divorce.getMethod().isDiceRoll()) {
                ((RandomDivorce) divorce).setDivorceDiceSize(newOptions.get(CampaignOption.RANDOM_DIVORCE_DICE_SIZE));
            }
        }

        if (randomMarriageMethod != newOptions.get(CampaignOption.RANDOM_MARRIAGE_METHOD)) {
            Campaign campaign = getCampaign();
            final AbstractMarriage marriage = newOptions.get(CampaignOption.RANDOM_MARRIAGE_METHOD).getMethod(newOptions);
            campaign.getPlayerForce().getHumanResources().setMarriage(marriage);
        } else {
            AbstractMarriage marriage = getCampaign().getPlayerForce().getHumanResources().getMarriage();
            marriage.setUseClanPersonnelMarriages(newOptions.get(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES));
            marriage.setUsePrisonerMarriages(newOptions.get(CampaignOption.USE_PRISONER_MARRIAGES));
            marriage.setUseRandomClanPersonnelMarriages(newOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES));
            marriage.setUseRandomPrisonerMarriages(newOptions.get(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES));
            if (marriage.getMethod().isDiceRoll()) {
                ((RandomMarriage) marriage).setMarriageDiceSize(newOptions.get(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE));
            }
        }

        if (randomProcreationMethod != newOptions.get(CampaignOption.RANDOM_PROCREATION_METHOD)) {
            Campaign campaign = getCampaign();
            final AbstractProcreation procreation = newOptions.get(CampaignOption.RANDOM_PROCREATION_METHOD).getMethod(newOptions);
            campaign.getPlayerForce().getHumanResources().setProcreation(procreation);
        } else {
            AbstractProcreation procreation = getCampaign().getPlayerForce().getHumanResources().getProcreation();
            procreation.setUseClanPersonnelProcreation(newOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION));
            procreation.setUsePrisonerProcreation(newOptions.get(CampaignOption.USE_PRISONER_PROCREATION));
            procreation.setUseRelationshiplessProcreation(newOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION));
            procreation.setUseRandomClanPersonnelProcreation(newOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION));
            procreation.setUseRandomPrisonerProcreation(newOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION));
            if (procreation.getMethod().isDiceRoll()) {
                ((RandomProcreation) procreation).setRelationshipDieSize(
                      newOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE));
                ((RandomProcreation) procreation).setRelationshiplessDieSize(
                      newOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE));
            }
        }

        // Clear Procreation Data if Disabled
        if (!newOptions.get(CampaignOption.USE_MANUAL_PROCREATION) && newOptions.get(CampaignOption.RANDOM_PROCREATION_METHOD).isNone()) {
            getCampaign().getPlayerForce().getHumanResources().getPersonnel()
                  .parallelStream()
                  .filter(Person::isPregnant)
                  .forEach(person -> getCampaign().getPlayerForce()
                                           .getHumanResources()
                                           .getProcreation()
                                           .removePregnancy(person));
        }

        final AbstractUnitMarket unitMarket = getCampaign().getUnitMarket();
        if (unitMarket.getMethod() != newOptions.get(CampaignOption.UNIT_MARKET_METHOD)) {
            getCampaign().setUnitMarket(newOptions.get(CampaignOption.UNIT_MARKET_METHOD).getUnitMarket());
            getCampaign().getUnitMarket().setOffers(unitMarket.getOffers());
        }

        if (atb != newOptions.isUseStratCon()) {
            if (newOptions.isUseStratCon()) {
                getCampaign().initAtB(false);
                // refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(),
                      getCampaign().getPlayerForce().getFormations()));
            }
            if (newOptions.isUseStratCon()) {
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
            } else {
                getCampaign().shutdownAtB();
            }
        }

        getCampaign().initTurnover();

        if (factionIntroDate != newOptions.get(CampaignOption.FACTION_INTRO_DATE)) {
            getCampaign().getPlayerForce().updateTechFactionCode();
        }
        getGui().refreshWindowTitle();
        getCampaign().reloadNews();
    }


    protected void loadPartsFile() {
        Optional<File> maybeFile = FileDialogs.openParts(getFrame());

        if (maybeFile.isEmpty()) {
            return;
        }

        File partsFile = maybeFile.get();

        logger.info("Starting load of parts file from XML...");
        // Initialize variables.
        Document xmlDoc;

        // Open up the file.
        try (InputStream is = new FileInputStream(partsFile)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            logger.error("", ex);
            return;
        }

        Element partsEle = xmlDoc.getDocumentElement();
        NodeList nl = partsEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        partsEle.normalize();

        final Version version = new Version(partsEle.getAttribute("version"));

        // we need to iterate through three times, the first time to collect
        // any custom units that might not be written yet
        List<Part> parts = new ArrayList<>();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                // Error condition of sorts!
                // Err, what should we do here?
                logger.error("Unknown node type not loaded in Parts nodes: {}", wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                parts.add(p);
            }
        }

        getCampaign().importParts(parts);
        logger.info("Finished load of parts file");
    }

    private void changeTheme(ActionEvent event) {
        MekHQ.getSelectedTheme().setValue(event.getActionCommand());
        refreshThemeChoices();
    }

    private void refreshThemeChoices() {
        menuThemes.removeAll();
        JCheckBoxMenuItem miPlaf;
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            // intentionally only limits the themes in the menu; as a last resort for GUI problems, other laf can be
            // used by hand-editing mhq.preferences
            if (GUIPreferences.isSupportedLookAndFeel(laf)) {
                miPlaf = new JCheckBoxMenuItem(laf.getName());
                if (laf.getClassName().equalsIgnoreCase(MekHQ.getSelectedTheme().getValue())) {
                    miPlaf.setSelected(true);
                }

                menuThemes.add(miPlaf);
                miPlaf.setActionCommand(laf.getClassName());
                miPlaf.addActionListener(this::changeTheme);
            }
        }
    }


    public void refreshGMMenuItems() {
        if (miPlanetarySystemEditor != null) {
            miPlanetarySystemEditor.setVisible(getCampaign().isGM());
        }
    }

    /**
     * Retrieves localized text from the panel's resource bundle.
     */
    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

    /**
     * Processes changes in campaign options.
     *
     * <p>Updates the visibility and availability of UI tabs and menu items based on the new campaign settings.
     * Also triggers a refresh of all tabs and schedules updates for funds and parts availability.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param optionsChangedEvent the event containing the updated options
     */
    @Subscribe
    public void handle(final OptionsChangedEvent optionsChangedEvent) {
        miRetirementDefectionDialog.setVisible(optionsChangedEvent.getOptions().get(CampaignOption.USE_RANDOM_RETIREMENT));
        miAwardEligibilityDialog.setVisible((optionsChangedEvent.getOptions().get(CampaignOption.ENABLE_AUTO_AWARDS)));
    }

    /**
     * Handles changes to general application options.
     *
     * <p>Updates the visibility of the company generator menu item according to the new option settings.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param mhqOptionsChangedEvent the event containing the updated general options
     */
    @Subscribe
    public void handle(final MHQOptionsChangedEvent mhqOptionsChangedEvent) {
        miCommandGenerator.setVisible(MekHQ.getMHQOptions().getShowCommandGenerator());
    }

}
