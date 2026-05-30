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

import static mekhq.gui.CampaignGUI.MAX_QUANTITY_SPINNER;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.xml.parsers.DocumentBuilder;

import megamek.Version;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.CopySystemDataAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.divorce.RandomDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.marriage.RandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Systems;
import mekhq.gui.CampaignGUI;
import mekhq.gui.CampaignGuiTab;
import mekhq.gui.FileDialogs;
import mekhq.gui.GUI;
import mekhq.gui.HangarTab;
import mekhq.gui.PersonnelTab;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog;
import mekhq.gui.dialog.reportDialogs.CargoReportDialog;
import mekhq.gui.dialog.reportDialogs.HangarReportDialog;
import mekhq.gui.dialog.reportDialogs.PersonnelReportDialog;
import mekhq.gui.dialog.reportDialogs.ReputationReportDialog;
import mekhq.gui.dialog.reportDialogs.TransportReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.io.FileType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MekHQMenuBar extends JMenuBar {

    private static final MMLogger logger = MMLogger.create(MekHQMenuBar.class);

    private final CampaignGUI gui;
    private final ResourceBundle resourceMap;
    private final MekHQ app;

    private JMenu menuThemes;
    private JMenuItem miRetirementDefectionDialog;
    private JMenuItem miAwardEligibilityDialog;
    private JMenuItem miCompanyGenerator;
    private JMenuItem miPlanetarySystemEditor;

    // Blob crew menu references for visibility control
    private JMenu menuSoldierPool;
    private JMenu menuBattleArmorPool;
    private JMenu menuVehicleCrewGroundPool;
    private JMenu menuVehicleCrewVTOLPool;
    private JMenu menuVehicleCrewNavalPool;
    private JMenu menuVesselPilotPool;
    private JMenu menuVesselGunnerPool;
    private JMenu menuVesselCrewPool;

    private static final List<PersonnelRole> BLOB_CREW_ROLES = List.of(
          PersonnelRole.SOLDIER, PersonnelRole.BATTLE_ARMOUR,
          PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.VEHICLE_CREW_VTOL,
          PersonnelRole.VEHICLE_CREW_NAVAL, PersonnelRole.VESSEL_PILOT,
          PersonnelRole.VESSEL_GUNNER, PersonnelRole.VESSEL_CREW);

    /**
     * This is used to initialize the top menu bar. All the top level menu bar and {@link MHQTabType} mnemonics must be
     * unique, as they are both accessed through the same GUI page. The following mnemonic keys are being used as of
     * 30-MAR-2020: A, B, C, E, F, H, I, L, M, N, O, P, R, S, T, V, W, /
     * <p>
     * Note 1: the slash is used for the help, as it is normally the same key as the ?
     * <p>
     * Note 2: the A mnemonic is used for the Advance Day button
     * <p>
     * Note 3: Only essential actions (Save, Load, New) have global Ctrl+key accelerators. All other menu items use
     * mnemonics only (accessible via keyboard menu navigation) to avoid duplicate accelerator conflicts.
     */
    public MekHQMenuBar(MekHQ app, CampaignGUI gui, ResourceBundle resourceMap) {
        super();
        this.app = app;
        this.gui = gui;
        this.resourceMap = resourceMap;
        initMenu();
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

    private void initMenu() {
        // TODO : Implement "Export All" versions for Personnel and Parts
        // See the JavaDoc comment for used mnemonic keys
        getAccessibleContext().setAccessibleName("Main Menu");

        // region File Menu
        // The File menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, E, H, I, L, M, N, R, S, T, U, X
        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text"));
        menuFile.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuLoad = new JMenuItem(resourceMap.getString("menuLoad.text"));
        menuLoad.setMnemonic(KeyEvent.VK_L);
        menuLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        menuLoad.addActionListener(evt -> {
            final File file = FileDialogs.openCampaign(getFrame()).orElse(null);
            if (file == null) {
                return;
            }
            new DataLoadingDialog(getFrame(), getApplication(), file).setVisible(true);
            // Unregister event handlers for CampaignGUI and tabs
            for (int i = 0; i < getTabMain().getTabCount(); i++) {
                if (getTabMain().getComponentAt(i) instanceof CampaignGuiTab) {
                    ((CampaignGuiTab) getTabMain().getComponentAt(i)).disposeTab();
                }
            }
            MekHQ.unregisterHandler(this);
            // check for a loaded story arc and unregister that handler as well
            if (null != getCampaign().getStoryArc()) {
                MekHQ.unregisterHandler(getCampaign().getStoryArc());
            }
        });
        menuFile.add(menuLoad);

        JMenuItem menuSave = new JMenuItem(resourceMap.getString("menuSave.text"));
        menuSave.setMnemonic(KeyEvent.VK_S);
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuSave.addActionListener(getGui()::saveCampaign);
        menuFile.add(menuSave);

        JMenuItem menuNew = new JMenuItem(resourceMap.getString("menuNew.text"));
        menuNew.setMnemonic(KeyEvent.VK_N);
        menuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        menuNew.addActionListener(evt -> handleInAppNewCampaign());
        menuFile.add(menuNew);

        // region menuImport
        // The Import menu uses the following Mnemonic keys as of 25-MAR-2022:
        // A, C, F, I, P
        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text"));
        menuImport.setMnemonic(KeyEvent.VK_I);

        JMenuItem miImportPerson = new JMenuItem(resourceMap.getString("miImportPerson.text"));
        miImportPerson.setMnemonic(KeyEvent.VK_P);
        miImportPerson.addActionListener(evt -> loadPersonFile());
        menuImport.add(miImportPerson);

        JMenuItem miImportIndividualRankSystem = new JMenuItem(resourceMap.getString("miImportIndividualRankSystem.text"));
        miImportIndividualRankSystem.setToolTipText(resourceMap.getString("miImportIndividualRankSystem.toolTipText"));
        miImportIndividualRankSystem.setName("miImportIndividualRankSystem");
        miImportIndividualRankSystem.setMnemonic(KeyEvent.VK_I);
        miImportIndividualRankSystem.addActionListener(
              evt -> getCampaign().setRankSystem(RankSystem.generateIndividualInstanceFromXML(
                    FileDialogs.openIndividualRankSystem(getFrame()).orElse(null))));
        menuImport.add(miImportIndividualRankSystem);

        JMenuItem miImportParts = new JMenuItem(resourceMap.getString("miImportParts.text"));
        miImportParts.setMnemonic(KeyEvent.VK_A);
        miImportParts.addActionListener(evt -> loadPartsFile());
        menuImport.add(miImportParts);

        JMenuItem miLoadForces = new JMenuItem(resourceMap.getString("miLoadForces.text"));
        miLoadForces.setMnemonic(KeyEvent.VK_F);
        miLoadForces.addActionListener(evt -> loadListFile(true));
        menuImport.add(miLoadForces);

        menuFile.add(menuImport);
        // endregion menuImport

        // region menuExport
        // The Export menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, X, S
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text"));
        menuExport.setMnemonic(KeyEvent.VK_X);

        // region CSV Export
        // The CSV menu uses the following Mnemonic keys as of 25-MAR-2022:
        // F, P, U
        JMenu miExportCSVFile = new JMenu(resourceMap.getString("menuExportCSV.text"));
        miExportCSVFile.setMnemonic(KeyEvent.VK_C);

        JMenuItem miExportPersonCSV = new JMenuItem(resourceMap.getString("miExportPersonnel.text"));
        miExportPersonCSV.setMnemonic(KeyEvent.VK_P);
        miExportPersonCSV.addActionListener(evt -> {
            try {
                exportPersonnel(FileType.CSV,
                      resourceMap.getString("dlgSavePersonnelCSV.text"),
                      getCampaign().getLocalDate()
                            .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                          .withLocale(MekHQ.getMHQOptions().getDateLocale())) + "_ExportedPersonnel");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(resourceMap.getString("miExportUnit.text"));
        miExportUnitCSV.setMnemonic(KeyEvent.VK_U);
        miExportUnitCSV.addActionListener(evt -> {
            try {
                exportUnits(FileType.CSV,
                      resourceMap.getString("dlgSaveUnitsCSV.text"),
                      getCampaign().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedUnits");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportUnitCSV);

        JMenuItem miExportFinancesCSV = new JMenuItem(resourceMap.getString("miExportFinances.text"));
        miExportFinancesCSV.setMnemonic(KeyEvent.VK_F);
        miExportFinancesCSV.addActionListener(evt -> {
            try {
                exportFinances(FileType.CSV,
                      resourceMap.getString("dlgSaveFinancesCSV.text"),
                      getCampaign().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedFinances");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportFinancesCSV);

        menuExport.add(miExportCSVFile);
        // endregion CSV Export

        // region XML Export
        // The XML menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, I, P, R
        JMenu miExportXMLFile = new JMenu(resourceMap.getString("menuExportXML.text"));
        miExportXMLFile.setMnemonic(KeyEvent.VK_X);

        JMenuItem miExportRankSystems = new JMenuItem(resourceMap.getString("miExportRankSystems.text"));
        miExportRankSystems.setName("miExportRankSystems");
        miExportRankSystems.setMnemonic(KeyEvent.VK_R);
        miExportRankSystems.addActionListener(evt -> Ranks.exportRankSystemsToFile(FileDialogs.saveRankSystems(getFrame())
                                                                                         .orElse(null),
              getCampaign().getRankSystem()));
        miExportXMLFile.add(miExportRankSystems);

        JMenuItem miExportIndividualRankSystem = new JMenuItem(resourceMap.getString("miExportIndividualRankSystem.text"));
        miExportIndividualRankSystem.setName("miExportIndividualRankSystem");
        miExportIndividualRankSystem.setMnemonic(KeyEvent.VK_I);
        miExportIndividualRankSystem.addActionListener(evt -> getCampaign().getRankSystem()
                                                                    .writeToFile(FileDialogs.saveIndividualRankSystem(
                                                                          getFrame()).orElse(null)));
        miExportXMLFile.add(miExportIndividualRankSystem);

        JMenuItem miExportPlanetsXML = new JMenuItem(resourceMap.getString("miExportPlanets.text"));
        miExportPlanetsXML.setMnemonic(KeyEvent.VK_P);
        miExportPlanetsXML.addActionListener(evt -> {
            try {
                exportPlanets(FileType.XML,
                      resourceMap.getString("dlgSavePlanetsXML.text"),
                      getCampaign().getName() +
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

        JMenuItem miExportCampaignSubset = new JMenuItem(resourceMap.getString("miExportCampaignSubset.text"));
        miExportCampaignSubset.setMnemonic(KeyEvent.VK_S);
        miExportCampaignSubset.addActionListener(evt -> {
            CampaignExportWizard cew = new CampaignExportWizard(getCampaign());
            cew.display(CampaignExportWizard.CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        menuFile.add(menuExport);
        // endregion menuExport

        // region Menu Refresh
        // The Refresh menu uses the following Mnemonic keys as of 12-APR-2022:
        // A, C, D, F, P, R, U
        JMenu menuRefresh = new JMenu(resourceMap.getString("menuRefresh.text"));
        menuRefresh.setMnemonic(KeyEvent.VK_R);

        JMenuItem miRefreshUnitCache = new JMenuItem(resourceMap.getString("miRefreshUnitCache.text"));
        miRefreshUnitCache.setName("miRefreshUnitCache");
        miRefreshUnitCache.setMnemonic(KeyEvent.VK_U);
        miRefreshUnitCache.addActionListener(evt -> MekSummaryCache.refreshUnitData(false));
        menuRefresh.add(miRefreshUnitCache);

        JMenuItem miRefreshCamouflage = new JMenuItem(resourceMap.getString("miRefreshCamouflage.text"));
        miRefreshCamouflage.setName("miRefreshCamouflage");
        miRefreshCamouflage.setMnemonic(KeyEvent.VK_C);
        miRefreshCamouflage.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshCamouflageDirectory();
            getGui().refreshAllTabs();
        });
        menuRefresh.add(miRefreshCamouflage);

        JMenuItem miRefreshPortraits = new JMenuItem(resourceMap.getString("miRefreshPortraits.text"));
        miRefreshPortraits.setName("miRefreshPortraits");
        miRefreshPortraits.setMnemonic(KeyEvent.VK_P);
        miRefreshPortraits.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshPortraitDirectory();
            getGui().refreshAllTabs();
        });
        menuRefresh.add(miRefreshPortraits);

        JMenuItem miRefreshFormationIcons = new JMenuItem(resourceMap.getString("miRefreshFormationIcons.text"));
        miRefreshFormationIcons.setName("miRefreshFormationIcons");
        miRefreshFormationIcons.setMnemonic(KeyEvent.VK_F);
        miRefreshFormationIcons.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshFormationIcons();
            getGui().refreshAllTabs();
        });
        menuRefresh.add(miRefreshFormationIcons);

        JMenuItem miRefreshAwards = new JMenuItem(resourceMap.getString("miRefreshAwards.text"));
        miRefreshAwards.setName("miRefreshAwards");
        miRefreshAwards.setMnemonic(KeyEvent.VK_A);
        miRefreshAwards.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshAwardIcons();
            getGui().refreshAllTabs();
        });
        menuRefresh.add(miRefreshAwards);

        JMenuItem miRefreshStoryIcons = new JMenuItem(resourceMap.getString("miRefreshStoryIcons.text"));
        miRefreshStoryIcons.setName("miRefreshAwards");
        miRefreshStoryIcons.setMnemonic(KeyEvent.VK_A);
        miRefreshStoryIcons.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshStorySplash();
            getGui().refreshAllTabs();
        });
        menuRefresh.add(miRefreshStoryIcons);

        JMenuItem miRefreshRanks = new JMenuItem(resourceMap.getString("miRefreshRanks.text"));
        miRefreshRanks.setName("miRefreshRanks");
        miRefreshRanks.setMnemonic(KeyEvent.VK_R);
        miRefreshRanks.addActionListener(evt -> Ranks.reinitializeRankSystems(getCampaign()));
        menuRefresh.add(miRefreshRanks);

        JMenuItem miRefreshFinancialInstitutions = new JMenuItem(resourceMap.getString(
              "miRefreshFinancialInstitutions.text"));
        miRefreshFinancialInstitutions.setToolTipText(resourceMap.getString("miRefreshFinancialInstitutions.toolTipText"));
        miRefreshFinancialInstitutions.setName("miRefreshFinancialInstitutions");
        miRefreshFinancialInstitutions.addActionListener(evt -> FinancialInstitutions.initializeFinancialInstitutions());
        menuRefresh.add(miRefreshFinancialInstitutions);

        menuFile.add(menuRefresh);
        // endregion Menu Refresh

        JMenuItem menuOptions = new JMenuItem(resourceMap.getString("menuOptions.text"));
        menuOptions.setMnemonic(KeyEvent.VK_C);
        menuOptions.addActionListener(this::menuOptionsActionPerformed);
        menuFile.add(menuOptions);

        final JMenuItem miMHQOptions = new JMenuItem(resourceMap.getString("miMHQOptions.text"));
        miMHQOptions.setToolTipText(resourceMap.getString("miMHQOptions.toolTipText"));
        miMHQOptions.setName("miMHQOptions");
        miMHQOptions.setMnemonic(KeyEvent.VK_H);
        miMHQOptions.addActionListener(evt -> new MHQOptionsDialog(getFrame()).setVisible(true));
        menuFile.add(miMHQOptions);

        final JMenuItem miGameOptions = new JMenuItem(resourceMap.getString("miGameOptions.text"));
        miGameOptions.setToolTipText(resourceMap.getString("miGameOptions.toolTipText"));
        miGameOptions.setName("miGameOptions");
        miGameOptions.setMnemonic(KeyEvent.VK_M);
        miGameOptions.addActionListener(evt -> {
            final GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
            god.setEditable(true);
            if (god.showDialog().isConfirmed()) {
                getCampaign().setGameOptions(god.getOptions());
                getGui().refreshWindowTitle();
            }
        });
        menuFile.add(miGameOptions);

        final JMenuItem miMMClientOptions = new JMenuItem(resourceMap.getString("miMMClientOptions.text"));
        miMMClientOptions.setToolTipText(resourceMap.getString("miMMClientOptions.toolTipText"));
        miMMClientOptions.setName("miMMClientOptions");
        miMMClientOptions.setMnemonic(KeyEvent.VK_O);
        miMMClientOptions.addActionListener(evt -> new CommonSettingsDialog(getFrame(), null).setVisible(true));
        menuFile.add(miMMClientOptions);

        menuThemes = new JMenu(resourceMap.getString("menuThemes.text"));
        menuThemes.setMnemonic(KeyEvent.VK_T);
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem(resourceMap.getString("menuExit.text"));
        menuExitItem.setMnemonic(KeyEvent.VK_E);
        menuExitItem.addActionListener(evt -> getApplication().exit(true));
        menuFile.add(menuExitItem);

        add(menuFile);
        // endregion File Menu

        // region Marketplace Menu
        // The Marketplace menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, C, H, M, N, P, R, S, U
        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text"));
        menuMarket.setMnemonic(KeyEvent.VK_M);

        JMenuItem miRecruitment = new JMenuItem(resourceMap.getString("miRecruitment.text"));
        miRecruitment.setMnemonic(KeyEvent.VK_R);
        miRecruitment.addActionListener(evt -> getGui().openRecruitmentDialog());
        menuMarket.add(miRecruitment);

        JMenuItem miContractMarket = new JMenuItem(resourceMap.getString("miContractMarket.text"));
        miContractMarket.setMnemonic(KeyEvent.VK_C);
        miContractMarket.addActionListener(evt -> getGui().showContractMarket());
        miContractMarket.setVisible(getCampaign().getCampaignOptions().isUseStratCon());
        menuMarket.add(miContractMarket);

        JMenuItem miUnitMarket = new JMenuItem(resourceMap.getString("miUnitMarket.text"));
        miUnitMarket.setMnemonic(KeyEvent.VK_U);
        miUnitMarket.addActionListener(evt -> getGui().showUnitMarket());
        miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        menuMarket.add(miUnitMarket);

        JMenuItem miPurchaseUnit = new JMenuItem(resourceMap.getString("miPurchaseUnit.text"));
        miPurchaseUnit.setMnemonic(KeyEvent.VK_N);
        miPurchaseUnit.addActionListener(evt -> {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(getFrame());
            if (!MekSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(getFrame(),
                  unitLoadingDialog,
                  getCampaign(),
                  true);
            usd.setVisible(true);
        });
        menuMarket.add(miPurchaseUnit);

        JMenuItem miBuyParts = new JMenuItem(resourceMap.getString("miBuyParts.text"));
        miBuyParts.setMnemonic(KeyEvent.VK_P);
        miBuyParts.addActionListener(evt -> new PartsStoreDialog(true, getGui()).setVisible(true));
        menuMarket.add(miBuyParts);

        JMenuItem miRecruitmentBulk = new JMenuItem(resourceMap.getString("miBulkRecruitment.text"));
        miRecruitmentBulk.setMnemonic(KeyEvent.VK_B);
        miRecruitmentBulk.addActionListener(evt -> getGui().openBulkRecruitmentDialog());
        menuMarket.add(miRecruitmentBulk);

        JMenu menuRecruitment = new JMenu(resourceMap.getString("menuRecruitment.text"));
        menuRecruitment.setMnemonic(KeyEvent.VK_H);

        JMenuItem menuRecruitmentBlank = new JMenuItem(resourceMap.getString("menuRecruitment.blank"));
        menuRecruitmentBlank.addActionListener(this::addBlankPerson);

        JMenu menuCombatRecruitment = new JMenu(resourceMap.getString("menuRecruitment.combat"));
        JMenu menuSupportRecruitment = new JMenu(resourceMap.getString("menuRecruitment.support"));
        JMenu menuCivilianRecruitment = new JMenu(resourceMap.getString("menuRecruitment.civilian"));

        PersonnelRole[] roles = PersonnelRole.getValuesSortedAlphabetically(getCampaign().isClanCampaign());
        for (PersonnelRole role : roles) {
            JMenuItem miRoleRecruitment = new JMenuItem(role.getLabel(getCampaign().getFaction().isClan()));
            if (role.getMnemonic() != KeyEvent.VK_UNDEFINED) {
                miRoleRecruitment.setMnemonic(role.getMnemonic());
            }
            miRoleRecruitment.setToolTipText(role.getDescription(getCampaign().isClanCampaign()));
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

        menuRecruitment.add(menuRecruitmentBlank);
        menuRecruitment.add(menuCombatRecruitment);
        menuRecruitment.add(menuSupportRecruitment);
        menuRecruitment.add(menuCivilianRecruitment);
        menuMarket.add(menuRecruitment);

        // region Temp Pool
        JMenu menuTempPool = new JMenu(resourceMap.getString("menuTempPool.text"));

        JMenuItem miTempPoolFullStrength = new JMenuItem(resourceMap.getString("miTempPoolFullStrength.text"));
        miTempPoolFullStrength.addActionListener(evt -> bringAllTempCrewsToFullStrength());
        menuTempPool.add(miTempPoolFullStrength);

        JMenuItem miTempPoolReleaseAll = new JMenuItem(resourceMap.getString("miTempPoolReleaseAll.text"));
        miTempPoolReleaseAll.addActionListener(evt -> releaseAllTempCrews());
        menuTempPool.add(miTempPoolReleaseAll);

        JMenuItem miTempPoolReleaseSurplus = new JMenuItem(resourceMap.getString("miTempPoolReleaseSurplus.text"));
        miTempPoolReleaseSurplus.addActionListener(evt -> releaseSurplusTempCrews());
        menuTempPool.add(miTempPoolReleaseSurplus);

        menuTempPool.addMenuListener(menuListenerFor(() -> {
            // For Astech/Medic: need + pool = what would be needed if there were no temp pool at all.
            // resetAsTechPool/resetMedicPool is a no-op only when pool == max(0, real need).
            int astechPool = getCampaign().getTemporaryAsTechPool();
            int astechIdealPool = Math.max(0, getCampaign().getAsTechNeed() + astechPool);
            int medicPool = getCampaign().getTemporaryMedicPool();
            int medicIdealPool = Math.max(0, getCampaign().getMedicsNeed() + medicPool);

            boolean anyNeed = astechPool != astechIdealPool
                                    || medicPool != medicIdealPool
                                    || anyBlobRoleHasNeed();
            setMenuItemState(miTempPoolFullStrength, anyNeed,
                  resourceMap.getString("miTempPoolFullStrength.disabledTip"));

            boolean anyPool = astechPool > 0
                                    || medicPool > 0
                                    || anyBlobRoleHasPool();
            setMenuItemState(miTempPoolReleaseAll, anyPool,
                  resourceMap.getString("miTempPoolReleaseAll.disabledTip"));

            boolean anySurplus = astechPool > astechIdealPool
                                       || medicPool > medicIdealPool
                                       || anyBlobRoleHasSurplus();
            setMenuItemState(miTempPoolReleaseSurplus, anySurplus,
                  resourceMap.getString("miTempPoolReleaseSurplus.disabledTip"));
        }));

        menuTempPool.addSeparator();

        // region Astech Pool
        // The Astech Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, F, H
        JMenu menuAstechPool = new JMenu(resourceMap.getString("menuAstechPool.text"));
        menuAstechPool.setMnemonic(KeyEvent.VK_A);

        JMenuItem miHireAstechs = new JMenuItem(resourceMap.getString("miHireAstechs.text"));
        miHireAstechs.setMnemonic(KeyEvent.VK_H);
        miHireAstechs.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupHireAstechsNum.text"),
                  1,
                  0,
                  MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAstechPool.add(miHireAstechs);

        JMenuItem miFireAstechs = new JMenuItem(resourceMap.getString("miFireAstechs.text"));
        miFireAstechs.setMnemonic(KeyEvent.VK_E);
        miFireAstechs.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupFireAstechsNum.text"),
                  1,
                  0,
                  getCampaign().getTemporaryAsTechPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAstechPool.add(miFireAstechs);

        JMenuItem miFullStrengthAstechs = new JMenuItem(resourceMap.getString("miFullStrengthAstechs.text"));
        miFullStrengthAstechs.setMnemonic(KeyEvent.VK_B);
        miFullStrengthAstechs.addActionListener(evt -> getCampaign().resetAsTechPool());
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = new JMenuItem(resourceMap.getString("miFireAllAstechs.text"));
        miFireAllAstechs.setMnemonic(KeyEvent.VK_R);
        miFireAllAstechs.addActionListener(evt -> getCampaign().emptyAsTechPool());
        menuAstechPool.add(miFireAllAstechs);

        menuAstechPool.addMenuListener(menuListenerFor(() -> {
            int pool = getCampaign().getTemporaryAsTechPool();
            int need = getCampaign().getAsTechNeed();
            // need + pool = what would be needed if there were no temp pool at all;
            // the action is a no-op only when pool == max(0, that real need)
            int idealPool = Math.max(0, need + pool);
            setMenuItemState(miFireAstechs, pool > 0,
                  resourceMap.getString("miFireAstechs.disabledTip"));
            setMenuItemState(miFullStrengthAstechs, pool != idealPool,
                  resourceMap.getString("miFullStrengthAstechs.disabledTip"));
            setMenuItemState(miFireAllAstechs, pool > 0,
                  resourceMap.getString("miFireAllAstechs.disabledTip"));
        }));

        menuTempPool.add(menuAstechPool);
        // endregion Astech Pool

        // region Medic Pool
        // The Medic Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, H, R
        JMenu menuMedicPool = new JMenu(resourceMap.getString("menuMedicPool.text"));
        menuMedicPool.setMnemonic(KeyEvent.VK_M);

        JMenuItem miHireMedics = new JMenuItem(resourceMap.getString("miHireMedics.text"));
        miHireMedics.setMnemonic(KeyEvent.VK_H);
        miHireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupHireMedicsNum.text"),
                  1,
                  0,
                  MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = new JMenuItem(resourceMap.getString("miFireMedics.text"));
        miFireMedics.setMnemonic(KeyEvent.VK_E);
        miFireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupFireMedicsNum.text"),
                  1,
                  0,
                  getCampaign().getTemporaryMedicPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miFireMedics);

        JMenuItem miFullStrengthMedics = new JMenuItem(resourceMap.getString("miFullStrengthMedics.text"));
        miFullStrengthMedics.setMnemonic(KeyEvent.VK_B);
        miFullStrengthMedics.addActionListener(evt -> getCampaign().resetMedicPool());
        menuMedicPool.add(miFullStrengthMedics);

        JMenuItem miFireAllMedics = new JMenuItem(resourceMap.getString("miFireAllMedics.text"));
        miFireAllMedics.setMnemonic(KeyEvent.VK_R);
        miFireAllMedics.addActionListener(evt -> getCampaign().emptyMedicPool());
        menuMedicPool.add(miFireAllMedics);

        menuMedicPool.addMenuListener(menuListenerFor(() -> {
            int pool = getCampaign().getTemporaryMedicPool();
            int need = getCampaign().getMedicsNeed();
            // need + pool = what would be needed if there were no temp pool at all;
            // the action is a no-op only when pool == max(0, that real need)
            int idealPool = Math.max(0, need + pool);
            setMenuItemState(miFireMedics, pool > 0,
                  resourceMap.getString("miFireMedics.disabledTip"));
            setMenuItemState(miFullStrengthMedics, pool != idealPool,
                  resourceMap.getString("miFullStrengthMedics.disabledTip"));
            setMenuItemState(miFireAllMedics, pool > 0,
                  resourceMap.getString("miFireAllMedics.disabledTip"));
        }));

        menuTempPool.add(menuMedicPool);
        // endregion Medic Pool

        // region Blob Crew Pools (Soldier, Battle Armor, Vehicle, Vessel)
        // Each pool follows the same 4-item structure; see buildBlobCrewPoolSubMenu.
        CampaignOptions opts = getCampaign().getCampaignOptions();

        menuSoldierPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.SOLDIER,
              opts.isUseBlobInfantry(),
              "menuSoldierPool.text",
              "miHireSoldiers.text", "popupHireSoldiersNum.text",
              "miFireSoldiers.text", "popupFireSoldiersNum.text",
              "miFullStrengthSoldiers.text", "miFireAllSoldiers.text");

        menuBattleArmorPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.BATTLE_ARMOUR,
              opts.isUseBlobBattleArmor(),
              "menuBattleArmorPool.text",
              "miHireBattleArmor.text", "popupHireBattleArmorNum.text",
              "miFireBattleArmor.text", "popupFireBattleArmorNum.text",
              "miFullStrengthBattleArmor.text", "miFireAllBattleArmor.text");

        menuVehicleCrewGroundPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VEHICLE_CREW_GROUND,
              opts.isUseBlobVehicleCrewGround(),
              "menuVehicleCrewGroundPool.text",
              "miHireVehicleCrewGround.text", "popupHireVehicleCrewGroundNum.text",
              "miFireVehicleCrewGround.text", "popupFireVehicleCrewGroundNum.text",
              "miFullStrengthVehicleCrewGround.text", "miFireAllVehicleCrewGround.text");

        menuVehicleCrewVTOLPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VEHICLE_CREW_VTOL,
              opts.isUseBlobVehicleCrewVTOL(),
              "menuVehicleCrewVTOLPool.text",
              "miHireVehicleCrewVTOL.text", "popupHireVehicleCrewVTOLNum.text",
              "miFireVehicleCrewVTOL.text", "popupFireVehicleCrewVTOLNum.text",
              "miFullStrengthVehicleCrewVTOL.text", "miFireAllVehicleCrewVTOL.text");

        menuVehicleCrewNavalPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VEHICLE_CREW_NAVAL,
              opts.isUseBlobVehicleCrewNaval(),
              "menuVehicleCrewNavalPool.text",
              "miHireVehicleCrewNaval.text", "popupHireVehicleCrewNavalNum.text",
              "miFireVehicleCrewNaval.text", "popupFireVehicleCrewNavalNum.text",
              "miFullStrengthVehicleCrewNaval.text", "miFireAllVehicleCrewNaval.text");

        menuVesselPilotPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VESSEL_PILOT,
              opts.isUseBlobVesselPilot(),
              "menuVesselPilotPool.text",
              "miHireVesselPilot.text", "popupHireVesselPilotNum.text",
              "miFireVesselPilot.text", "popupFireVesselPilotNum.text",
              "miFullStrengthVesselPilot.text", "miFireAllVesselPilot.text");

        menuVesselGunnerPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VESSEL_GUNNER,
              opts.isUseBlobVesselGunner(),
              "menuVesselGunnerPool.text",
              "miHireVesselGunner.text", "popupHireVesselGunnerNum.text",
              "miFireVesselGunner.text", "popupFireVesselGunnerNum.text",
              "miFullStrengthVesselGunner.text", "miFireAllVesselGunner.text");

        menuVesselCrewPool = buildBlobCrewPoolSubMenu(menuTempPool, PersonnelRole.VESSEL_CREW,
              opts.isUseBlobVesselCrew(),
              "menuVesselCrewPool.text",
              "miHireVesselCrew.text", "popupHireVesselCrewNum.text",
              "miFireVesselCrew.text", "popupFireVesselCrewNum.text",
              "miFullStrengthVesselCrew.text", "miFireAllVesselCrew.text");
        // endregion Blob Crew Pools

        menuMarket.add(menuTempPool);
        // endregion Temp Pool

        add(menuMarket);
        // endregion Marketplace Menu

        // region Reports Menu
        // The Reports menu uses the following Mnemonic keys as of 19-March-2020:
        // C, H, P, T, U
        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text"));
        menuReports.setMnemonic(KeyEvent.VK_E);

        JMenuItem miDragoonsRating = new JMenuItem(resourceMap.getString("miDragoonsRating.text"));
        miDragoonsRating.setMnemonic(KeyEvent.VK_U);
        miDragoonsRating.addActionListener(evt -> new ReputationReportDialog(getFrame(), getCampaign()).setVisible(
              true));
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(resourceMap.getString("miPersonnelReport.text"));
        miPersonnelReport.setMnemonic(KeyEvent.VK_P);
        miPersonnelReport.addActionListener(evt -> new PersonnelReportDialog(getFrame(),
              new PersonnelReport(getCampaign())).setVisible(true));
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(resourceMap.getString("miHangarBreakdown.text"));
        miHangarBreakdown.setMnemonic(KeyEvent.VK_H);
        miHangarBreakdown.addActionListener(evt -> new HangarReportDialog(getFrame(),
              new HangarReport(getCampaign())).setVisible(true));
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(resourceMap.getString("miTransportReport.text"));
        miTransportReport.setMnemonic(KeyEvent.VK_T);
        miTransportReport.addActionListener(evt -> new TransportReportDialog(getFrame(),
              new TransportReport(getCampaign())).setVisible(true));
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(resourceMap.getString("miCargoReport.text"));
        miCargoReport.setMnemonic(KeyEvent.VK_C);
        miCargoReport.addActionListener(evt -> new CargoReportDialog(getFrame(),
              new CargoReport(getCampaign())).setVisible(true));
        menuReports.add(miCargoReport);

        add(menuReports);
        // endregion Reports Menu

        // region View Menu
        // The View menu uses the following Mnemonic keys as of 02-June-2020:
        // H, R
        JMenu menuView = new JMenu(resourceMap.getString("menuView.text"));
        menuView.setMnemonic(KeyEvent.VK_V);

        JMenuItem miHistoricalDailyReportDialog = new JMenuItem(resourceMap.getString("miShowHistoricalReportLog.text"));
        miHistoricalDailyReportDialog.setMnemonic(KeyEvent.VK_H);
        miHistoricalDailyReportDialog.addActionListener(evt -> {
            HistoricalDailyReportDialog histDailyReportDialog = new HistoricalDailyReportDialog(getFrame(), getGui());
            histDailyReportDialog.setModal(true);
            histDailyReportDialog.setVisible(true);
            histDailyReportDialog.dispose();
        });
        menuView.add(miHistoricalDailyReportDialog);

        miRetirementDefectionDialog = new JMenuItem(resourceMap.getString("miRetirementDefectionDialog.text"));
        miRetirementDefectionDialog.setMnemonic(KeyEvent.VK_R);
        miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().isUseRandomRetirement());
        miRetirementDefectionDialog.addActionListener(evt -> getGui().showRetirementDefectionDialog());
        menuView.add(miRetirementDefectionDialog);

        miAwardEligibilityDialog = new JMenuItem(resourceMap.getString("miAwardEligibilityDialog.text"));
        miAwardEligibilityDialog.setMnemonic(KeyEvent.VK_R);
        miAwardEligibilityDialog.setVisible(getCampaign().getCampaignOptions().isEnableAutoAwards());
        miAwardEligibilityDialog.addActionListener(evt -> showAwardEligibilityDialog());
        menuView.add(miAwardEligibilityDialog);

        add(menuView);
        // endregion View Menu

        // region Manage Campaign Menu
        JMenu menuManage = new JMenu(resourceMap.getString("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_C);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = new JMenuItem(resourceMap.getString("miGMToolsDialog.text"));
        miGMToolsDialog.setMnemonic(KeyEvent.VK_G);
        miGMToolsDialog.addActionListener(evt -> new GMToolsDialog(getFrame(), getGui(), null).setVisible(true));
        menuManage.add(miGMToolsDialog);

        miPlanetarySystemEditor = new JMenuItem(resourceMap.getString("miPlanetarySystemEditor.text"));
        miPlanetarySystemEditor.setMnemonic(KeyEvent.VK_P);
        miPlanetarySystemEditor.setVisible(getCampaign().isGM());
        miPlanetarySystemEditor.addActionListener(evt -> new PlanetarySystemEditorDialog(getFrame(), getCampaign())
                                                               .setVisible(true));
        menuManage.add(miPlanetarySystemEditor);

        JMenuItem miBloodnames = new JMenuItem(resourceMap.getString("miRandomBloodnames.text"));
        miBloodnames.setMnemonic(KeyEvent.VK_B);
        miBloodnames.addActionListener(evt -> {
            for (final Person person : getCampaign().getPersonnel()) {
                getCampaign().checkBloodnameAdd(person, false);
            }
        });
        menuManage.add(miBloodnames);

        JMenuItem miScenarioEditor = new JMenuItem(resourceMap.getString("miScenarioEditor.text"));
        miScenarioEditor.setMnemonic(KeyEvent.VK_S);
        miScenarioEditor.addActionListener(evt -> new ScenarioTemplateEditorDialog(getFrame()).setVisible(true));
        menuManage.add(miScenarioEditor);

        miCompanyGenerator = new JMenuItem(resourceMap.getString("miCompanyGenerator.text"));
        miCompanyGenerator.setMnemonic(KeyEvent.VK_C);
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
        miCompanyGenerator.addActionListener(evt -> new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(
              true));
        menuManage.add(miCompanyGenerator);

        JMenuItem miAutoResolveBehaviorEditor = new JMenuItem(resourceMap.getString("miAutoResolveBehaviorSettings.text"));
        miAutoResolveBehaviorEditor.setMnemonic(KeyEvent.VK_T);
        miAutoResolveBehaviorEditor.addActionListener(evt -> {
            var autoResolveBehaviorSettingsDialog = new AutoResolveBehaviorSettingsDialog(getFrame(), getCampaign());
            autoResolveBehaviorSettingsDialog.setVisible(true);
            autoResolveBehaviorSettingsDialog.pack();
        });

        menuManage.add(miAutoResolveBehaviorEditor);

        JMenu menuRoleplay = new JMenu(resourceMap.getString("menuRoleplay.text"));
        JMenuItem miLifePathBuilder = new JMenuItem(resourceMap.getString("miLifePathBuilder.text"));
        miLifePathBuilder.addActionListener(evt -> {
            new LifePathBuilderDialog(getCampaign(), getFrame());
        });
        menuRoleplay.add(miLifePathBuilder);
        menuManage.add(menuRoleplay);

        add(menuManage);
        // endregion Manage Campaign Menu

        // region Help Menu
        // The Help menu uses the following Mnemonic keys as of 19-March-2020:
        // A
        JMenu menuHelp = new JMenu(resourceMap.getString("menuHelp.text"));
        menuHelp.setMnemonic(KeyEvent.VK_SLASH);
        menuHelp.setName("helpMenu");

        menuHelp.addSeparator();

        JMenuItem menuBugReportItem = new JMenuItem(resourceMap.getString("menuReportBug.text"));
        menuBugReportItem.setName("ReportBug");
        menuBugReportItem.addActionListener(evt -> new EasyBugReportDialog(getFrame(), getCampaign()));
        menuHelp.add(menuBugReportItem);

        menuHelp.add(new CopySystemDataAction(MHQConstants.PROJECT_NAME));

        menuHelp.addSeparator();

        JMenuItem menuAboutItem = new JMenuItem(resourceMap.getString("menuAbout.text"));
        menuAboutItem.setMnemonic(KeyEvent.VK_A);
        menuAboutItem.setName("aboutMenuItem");
        menuAboutItem.addActionListener(evt -> new MekHQAboutDialog(getFrame()).show());
        menuHelp.add(menuAboutItem);

        add(menuHelp);
        // endregion Help Menu

        MekHQ.registerHandler(this);
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
     * Exports Personnel to a file (CSV, XML, etc.)
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportPersonnel(FileType format, String dialogTitle, String filename) {
        if (((PersonnelTab) getGui().getTab(MHQTabType.PERSONNEL)).getPersonnelTable().getRowCount() != 0) {
            GUI.fileDialogSave(getFrame(),
                  dialogTitle,
                  format,
                  MekHQ.getPersonnelDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
                MekHQ.getPersonnelDirectory().setValue(f.getParent());
                File file = checkFileEnding(f, format.getRecommendedExtension());
                checkToBackupFile(file, file.getPath());
                String report;
                // TODO add support for xml and json export
                if (format.equals(FileType.CSV)) {
                    report = Utilities.exportTableToCSV(
                          ((PersonnelTab) getGui().getTab(MHQTabType.PERSONNEL)).getPersonnelTable(), file);
                } else {
                    report = "Unsupported FileType in Export Personnel";
                }
                JOptionPane.showMessageDialog(getFrame(), report);
            });
        } else {
            JOptionPane.showMessageDialog(getFrame(), resourceMap.getString("dlgNoPersonnel.text"));
        }
    }

    /**
     * Exports Units to a file (CSV, XML, etc.)
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportUnits(FileType format, String dialogTitle, String filename) {
        if (((HangarTab) getGui().getTab(MHQTabType.HANGAR)).getUnitTable().getRowCount() != 0) {
            GUI.fileDialogSave(getFrame(),
                  dialogTitle,
                  format,
                  MekHQ.getUnitsDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
                MekHQ.getUnitsDirectory().setValue(f.getParent());
                File file = checkFileEnding(f, format.getRecommendedExtension());
                checkToBackupFile(file, file.getPath());
                String report;
                // TODO add support for xml and json export
                if (format.equals(FileType.CSV)) {
                    report = Utilities.exportTableToCSV(
                          ((HangarTab) getGui().getTab(MHQTabType.HANGAR)).getUnitTable(), file);
                } else {
                    report = "Unsupported FileType in Export Units";
                }
                JOptionPane.showMessageDialog(getFrame(), report);
            });
        } else {
            JOptionPane.showMessageDialog(getFrame(), resourceMap.getString("dlgNoUnits.text"));
        }
    }

    /**
     * Exports Finances to a file (CSV, XML, etc.)
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportFinances(FileType format, String dialogTitle, String filename) {
        if (!getCampaign().getFinances().getTransactions().isEmpty()) {
            GUI.fileDialogSave(getFrame(),
                  dialogTitle,
                  format,
                  MekHQ.getFinancesDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
                MekHQ.getFinancesDirectory().setValue(f.getParent());
                File file = checkFileEnding(f, format.getRecommendedExtension());
                checkToBackupFile(file, file.getPath());
                String report;
                // TODO add support for xml and json export
                if (format.equals(FileType.CSV)) {
                    report = getCampaign().getFinances()
                                   .exportFinancesToCSV(file.getPath(), format.getRecommendedExtension());
                } else {
                    report = "Unsupported FileType in Export Finances";
                }
                JOptionPane.showMessageDialog(getFrame(), report);
            });
        } else {
            JOptionPane.showMessageDialog(getFrame(), resourceMap.getString("dlgNoFinances.text"));
        }
    }


    /**
     * Checks if a file already exists, if so it makes a backup copy.
     *
     * @param file to determine if there is an existing file with that name
     * @param path path to the file
     */
    private void checkToBackupFile(File file, String path) {
        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }
    }

    /**
     * Checks to make sure the file has the appropriate ending / extension.
     *
     * @param file   the file to check
     * @param format proper format for the ending/extension
     *
     * @return File with the appropriate ending/ extension
     */
    private File checkFileEnding(File file, String format) {
        String path = file.getPath();
        if (!path.endsWith('.' + format)) {
            path += '.' + format;
            file = new File(path);
        }
        return file;
    }

    protected void loadListFile(final boolean allowNewPilots) {
        final File unitFile = FileDialogs.openUnits(getFrame()).orElse(null);

        PartQuality quality = PartQuality.QUALITY_D;

        if (getCampaign().getCampaignOptions().isUseRandomUnitQualities()) {
            quality = Unit.getRandomUnitQuality(0);
        }

        if (unitFile != null) {
            try {
                for (Entity entity : new MULParser(unitFile, getCampaign().getGameOptions()).getEntities()) {
                    getCampaign().addNewUnit(entity, allowNewPilots, 0, quality);
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
                if ((person != null) && (getCampaign().getPerson(person.getId()) != null)) {
                    logger.error("ERROR: Cannot load person who exists, ignoring. (Name: {}, Id {})",
                          person.getFullName(),
                          person.getId());
                    person = null;
                }

                if (person != null) {
                    getCampaign().recruitPerson(person, person.getPrisonerStatus(), true, false, person.isEmployed(),
                          true);

                    // Clear some values we no longer should have set in case this
                    // has transferred campaigns or things in the campaign have
                    // changed...
                    person.setUnit(null);
                    person.clearTechUnits();
                }
            }

            // Fix Spouse Id Information - This is required to fix spouse NPEs where one doesn't export both members
            // of the couple
            // TODO : make it so that exports will automatically include both spouses
            for (Person p : getCampaign().getActivePersonnel(true, true)) {
                if (p.getGenealogy().hasSpouse() &&
                          !getCampaign().getPersonnel().contains(p.getGenealogy().getSpouse())) {
                    // If this happens, we need to clear the spouse
                    if (p.getMaidenName() != null) {
                        p.setSurname(p.getMaidenName());
                    }

                    p.getGenealogy().setSpouse(null);
                }

                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null) &&
                              !getCampaign().getPersonnel().contains(getCampaign().getPerson(fatherId))) {
                        p.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, null);
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
              resourceMap.getString("savePrompt.text"),
              resourceMap.getString("savePrompt.title"),
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);

        // Abort if the user cancels, closes the dialog, or fails to save
        if (savePrompt == JOptionPane.CANCEL_OPTION ||
                  savePrompt == JOptionPane.CLOSED_OPTION ||
                  (savePrompt == JOptionPane.YES_OPTION && !getGui().saveCampaign(null))) {
            return;
        }

        // Unregister handlers for campaign tabs
        for (int i = 0; i < getTabMain().getTabCount(); i++) {
            Component tab = getTabMain().getComponentAt(i);
            if (tab instanceof CampaignGuiTab) {
                ((CampaignGuiTab) tab).disposeTab();
            }
        }

        // Unregister other handlers
        MekHQ.unregisterHandler(this);

        if (getCampaign().getStoryArc() != null) {
            MekHQ.unregisterHandler(getCampaign().getStoryArc());
        }

        // Start a new campaign
        new DataLoadingDialog(getFrame(), getApplication(), null, null, true).setVisible(true);
    }


    public void showAwardEligibilityDialog() {
        AutoAwardsController autoAwardsController = new AutoAwardsController();

        autoAwardsController.ManualController(getCampaign(), true);
    }

    private void addBlankPerson(final ActionEvent evt) {
        Person person = new Person(getCampaign(), Faction.MERCENARY_FACTION_CODE);
        person.setOriginPlanet(Systems.getInstance().getSystemById("Terra").getPrimaryPlanet());
        person.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);

        getCampaign().recruitPerson(person, true, true);
    }

    private void hirePerson(final ActionEvent evt) {
        NewRecruitDialog npd = new NewRecruitDialog(getGui(), true,
              getCampaign().newPerson(PersonnelRole.valueOf(evt.getActionCommand())));
        npd.setVisible(true);
    }

    private boolean anyBlobRoleHasNeed() {
        return BLOB_CREW_ROLES.stream().anyMatch(this::blobRoleHasUnitsNeedingCrew);
    }

    private boolean anyBlobRoleHasPool() {
        Campaign c = getCampaign();
        return BLOB_CREW_ROLES.stream()
                     .anyMatch(role -> c.isBlobCrewEnabled(role) && c.getTempCrewPool(role) > 0);
    }

    private boolean anyBlobRoleHasSurplus() {
        Campaign c = getCampaign();
        for (PersonnelRole role : BLOB_CREW_ROLES) {
            if (!c.isBlobCrewEnabled(role)) {
                continue;
            }
            if (c.getAvailableTempCrewPool(role) > 0) {
                return true;
            }
            for (Unit unit : c.getUnits()) {
                int tempCrew = unit.getTempCrewByPersonnelRole(role);
                if (tempCrew > 0) {
                    int excess = (unit.getActiveCrew().size() + tempCrew) - unit.getFullCrewSize();
                    if (excess > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean blobRoleHasUnitsNeedingCrew(PersonnelRole role) {
        Campaign c = getCampaign();
        if (!c.isBlobCrewEnabled(role)) {
            return false;
        }
        for (Unit unit : c.getUnits()) {
            boolean unitMatchesRole = (role == PersonnelRole.VESSEL_CREW)
                                            ? unit.canTakeMoreVesselCrew()
                                            : (role == unit.getDriverRole() || role == unit.getGunnerRole());
            if (!unitMatchesRole || unit.getActiveCrew().isEmpty()) {
                continue;
            }
            // Use total temp crew across all roles (mirrors distributeTempCrewPoolToUnits logic)
            // so a vessel already full of another role's temp crew is not counted as needing crew
            int current = unit.getActiveCrew().size() + unit.getTotalTempCrew();
            if (current < unit.getFullCrewSize()) {
                return true;
            }
        }
        return false;
    }

    private void updateBlobCrewPoolMenuItems(PersonnelRole role,
          JMenuItem fireItem, JMenuItem fullStrengthItem, JMenuItem fireAllItem) {
        int pool = getCampaign().getTempCrewPool(role);
        boolean unitsNeedCrew = blobRoleHasUnitsNeedingCrew(role);

        setMenuItemState(fireItem, pool > 0,
              resourceMap.getString("miFireBlobCrew.disabledTip"));
        setMenuItemState(fullStrengthItem, unitsNeedCrew,
              resourceMap.getString("miFullStrengthBlobCrew.disabledTip"));
        setMenuItemState(fireAllItem, pool > 0,
              resourceMap.getString("miFireAllBlobCrew.disabledTip"));
    }

    private JMenu buildBlobCrewPoolSubMenu(JMenu parentMenu, PersonnelRole role, boolean isVisible,
          String menuKey, String hireKey, String hirePopupKey,
          String fireSomeKey, String fireSomePopupKey,
          String fullStrengthKey, String fireAllKey) {
        JMenu menu = new JMenu(resourceMap.getString(menuKey));
        menu.setVisible(isVisible);

        JMenuItem miHire = new JMenuItem(resourceMap.getString(hireKey));
        miHire.addActionListener(evt -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(getFrame(),
                  true, resourceMap.getString(hirePopupKey), 1, 0, MAX_QUANTITY_SPINNER);
            dialog.setVisible(true);
            if (dialog.getValue() >= 0) {
                getCampaign().increaseTempCrewPool(role, dialog.getValue());
            }
        });
        menu.add(miHire);

        JMenuItem miFireSome = new JMenuItem(resourceMap.getString(fireSomeKey));
        miFireSome.addActionListener(evt -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(getFrame(),
                  true, resourceMap.getString(fireSomePopupKey), 1, 0,
                  getCampaign().getTempCrewPool(role));
            dialog.setVisible(true);
            if (dialog.getValue() >= 0) {
                getCampaign().decreaseTempCrewPool(role, dialog.getValue());
            }
        });
        menu.add(miFireSome);

        JMenuItem miFullStrength = new JMenuItem(resourceMap.getString(fullStrengthKey));
        miFullStrength.addActionListener(evt -> {
            getCampaign().resetTempCrewPoolForRole(role);
            getCampaign().distributeTempCrewPoolToUnits(role);
        });
        menu.add(miFullStrength);

        JMenuItem miFireAll = new JMenuItem(resourceMap.getString(fireAllKey));
        miFireAll.addActionListener(evt -> getCampaign().setTempCrewPool(role, 0));
        menu.add(miFireAll);

        menu.addMenuListener(menuListenerFor(
              () -> updateBlobCrewPoolMenuItems(role, miFireSome, miFullStrength, miFireAll)));

        parentMenu.add(menu);
        return menu;
    }

    private void bringAllTempCrewsToFullStrength() {
        getCampaign().resetAsTechPool();
        getCampaign().resetMedicPool();
        if (getCampaign().getCampaignOptions().isUseBlobInfantry()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.SOLDIER);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.SOLDIER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobBattleArmor()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.BATTLE_ARMOUR);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.BATTLE_ARMOUR);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_GROUND);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_VTOL);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselPilot()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_PILOT);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_PILOT);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselGunner()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_GUNNER);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_GUNNER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselCrew()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_CREW);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_CREW);
        }
    }

    private void releaseAllTempCrews() {
        getCampaign().emptyAsTechPool();
        getCampaign().emptyMedicPool();
        if (getCampaign().getCampaignOptions().isUseBlobInfantry()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobBattleArmor()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselPilot()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselGunner()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselCrew()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_CREW);
        }
    }

    private void releaseSurplusTempCrews() {
        getCampaign().releaseSurplusAsTechPool();
        getCampaign().releaseSurplusMedicPool();
        if (getCampaign().getCampaignOptions().isUseBlobInfantry()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobBattleArmor()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselPilot()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselGunner()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselCrew()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_CREW);
        }
    }


    private static MenuListener menuListenerFor(Runnable onSelected) {
        return new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                onSelected.run();
            }

            @Override
            public void menuDeselected(MenuEvent e) {}

            @Override
            public void menuCanceled(MenuEvent e) {}
        };
    }

    private static void setMenuItemState(JMenuItem item, boolean enabled, String disabledTip) {
        item.setEnabled(enabled);
        item.setToolTipText(enabled ? null : disabledTip);
    }


    /**
     * @param evt the event triggering the opening of the Campaign Options Dialog
     */
    private void menuOptionsActionPerformed(final ActionEvent evt) {
        final CampaignOptions oldOptions = getCampaign().getCampaignOptions();
        // We need to handle it like this for now, as the options above get written to currently
        boolean atb = oldOptions.isUseStratCon();
        boolean factionIntroDate = oldOptions.isFactionIntroDate();
        final RandomDivorceMethod randomDivorceMethod = oldOptions.getRandomDivorceMethod();
        final RandomMarriageMethod randomMarriageMethod = oldOptions.getRandomMarriageMethod();
        final RandomProcreationMethod randomProcreationMethod = oldOptions.getRandomProcreationMethod();

        CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(getFrame(), getCampaign());
        optionsDialog.setVisible(true);

        final CampaignOptions newOptions = getCampaign().getCampaignOptions();

        if (randomDivorceMethod != newOptions.getRandomDivorceMethod()) {
            getCampaign().setDivorce(newOptions.getRandomDivorceMethod().getMethod(newOptions));
        } else {
            getCampaign().getDivorce().setUseClanPersonnelDivorce(newOptions.isUseClanPersonnelDivorce());
            getCampaign().getDivorce().setUsePrisonerDivorce(newOptions.isUsePrisonerDivorce());
            getCampaign().getDivorce().setUseRandomOppositeSexDivorce(newOptions.isUseRandomOppositeSexDivorce());
            getCampaign().getDivorce().setUseRandomSameSexDivorce(newOptions.isUseRandomSameSexDivorce());
            getCampaign().getDivorce().setUseRandomClanPersonnelDivorce(newOptions.isUseRandomClanPersonnelDivorce());
            getCampaign().getDivorce().setUseRandomPrisonerDivorce(newOptions.isUseRandomPrisonerDivorce());
            if (getCampaign().getDivorce().getMethod().isDiceRoll()) {
                ((RandomDivorce) getCampaign().getDivorce()).setDivorceDiceSize(newOptions.getRandomDivorceDiceSize());
            }
        }

        if (randomMarriageMethod != newOptions.getRandomMarriageMethod()) {
            getCampaign().setMarriage(newOptions.getRandomMarriageMethod().getMethod(newOptions));
        } else {
            getCampaign().getMarriage().setUseClanPersonnelMarriages(newOptions.isUseClanPersonnelMarriages());
            getCampaign().getMarriage().setUsePrisonerMarriages(newOptions.isUsePrisonerMarriages());
            getCampaign().getMarriage()
                  .setUseRandomClanPersonnelMarriages(newOptions.isUseRandomClanPersonnelMarriages());
            getCampaign().getMarriage().setUseRandomPrisonerMarriages(newOptions.isUseRandomPrisonerMarriages());
            if (getCampaign().getMarriage().getMethod().isDiceRoll()) {
                ((RandomMarriage) getCampaign().getMarriage()).setMarriageDiceSize(newOptions.getRandomMarriageDiceSize());
            }
        }

        if (randomProcreationMethod != newOptions.getRandomProcreationMethod()) {
            getCampaign().setProcreation(newOptions.getRandomProcreationMethod().getMethod(newOptions));
        } else {
            getCampaign().getProcreation().setUseClanPersonnelProcreation(newOptions.isUseClanPersonnelProcreation());
            getCampaign().getProcreation().setUsePrisonerProcreation(newOptions.isUsePrisonerProcreation());
            getCampaign().getProcreation()
                  .setUseRelationshiplessProcreation(newOptions.isUseRelationshiplessRandomProcreation());
            getCampaign().getProcreation()
                  .setUseRandomClanPersonnelProcreation(newOptions.isUseRandomClanPersonnelProcreation());
            getCampaign().getProcreation().setUseRandomPrisonerProcreation(newOptions.isUseRandomPrisonerProcreation());
            if (getCampaign().getProcreation().getMethod().isDiceRoll()) {
                ((RandomProcreation) getCampaign().getProcreation()).setRelationshipDieSize(newOptions.getRandomProcreationRelationshipDiceSize());
                ((RandomProcreation) getCampaign().getProcreation()).setRelationshiplessDieSize(newOptions.getRandomProcreationRelationshiplessDiceSize());
            }
        }

        // Clear Procreation Data if Disabled
        if (!newOptions.isUseManualProcreation() && newOptions.getRandomProcreationMethod().isNone()) {
            getCampaign().getPersonnel()
                  .parallelStream()
                  .filter(Person::isPregnant)
                  .forEach(person -> getCampaign().getProcreation().removePregnancy(person));
        }

        final AbstractUnitMarket unitMarket = getCampaign().getUnitMarket();
        if (unitMarket.getMethod() != newOptions.getUnitMarketMethod()) {
            getCampaign().setUnitMarket(newOptions.getUnitMarketMethod().getUnitMarket());
            getCampaign().getUnitMarket().setOffers(unitMarket.getOffers());
        }

        AbstractContractMarket contractMarket = getCampaign().getContractMarket();
        if (contractMarket.getMethod() != newOptions.getContractMarketMethod()) {
            getCampaign().setContractMarket(newOptions.getContractMarketMethod().getContractMarket());
        }

        if (atb != newOptions.isUseStratCon()) {
            if (newOptions.isUseStratCon()) {
                getCampaign().initAtB(false);
                // refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCampaign().getFormations()));
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

        if (factionIntroDate != newOptions.isFactionIntroDate()) {
            getCampaign().updateTechFactionCode();
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

    private void changeTheme(ActionEvent evt) {
        MekHQ.getSelectedTheme().setValue(evt.getActionCommand());
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
        // Update blob crew menu visibility based on campaign options
        menuSoldierPool.setVisible(getCampaign().getCampaignOptions().isUseBlobInfantry());
        menuBattleArmorPool.setVisible(getCampaign().getCampaignOptions().isUseBlobBattleArmor());
        menuVehicleCrewGroundPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround());
        menuVehicleCrewVTOLPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL());
        menuVehicleCrewNavalPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval());
        menuVesselPilotPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVesselPilot());
        menuVesselGunnerPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVesselGunner());
        menuVesselCrewPool.setVisible(getCampaign().getCampaignOptions().isUseBlobVesselCrew());

        miRetirementDefectionDialog.setVisible(optionsChangedEvent.getOptions().isUseRandomRetirement());
        miAwardEligibilityDialog.setVisible((optionsChangedEvent.getOptions().isEnableAutoAwards()));
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
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
    }

}
