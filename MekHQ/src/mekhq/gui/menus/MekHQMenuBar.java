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
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
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
        add(initHelpMenu());

        MekHQ.registerHandler(this);
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
     * The File menu uses the following Mnemonic keys as of 25-MAR-2022: C, E, H, I, L, M, N, R, S, T, U, X
     */
    private JMenu initFileMenu() {
        // TODO : Implement "Export All" versions for Personnel and Parts
        JMenu menuFile = new JMenu(getTextAt("fileMenu.text"));
        menuFile.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuLoad = createMenuItem("menuLoad.text", KeyEvent.VK_L, evt -> {
            final File file = FileDialogs.openCampaign(getFrame()).orElse(null);
            if (file == null) {
                return;
            }
            new DataLoadingDialog(getFrame(), getApplication(), file).setVisible(true);
            // Unregister event handlers for CampaignGUI and tabs
            for (int i = 0; i < getTabMain().getTabCount(); i++) {
                if (getTabMain().getComponentAt(i) instanceof CampaignGuiTab) {
                    ((CampaignGuiTab) getTabMain().getComponentAt(i)).deactivateTab();
                }
            }
            MekHQ.unregisterHandler(this);
            MekHQ.unregisterHandler(getGui());
            // check for a loaded story arc and unregister that handler as well
            if (null != getCampaign().getStoryArc()) {
                MekHQ.unregisterHandler(getCampaign().getStoryArc());
            }
        });
        menuLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuLoad);

        JMenuItem menuSave = createMenuItem("menuSave.text", KeyEvent.VK_S, getGui()::saveCampaign);
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuSave);

        JMenuItem menuNew = createMenuItem("menuNew.text", KeyEvent.VK_N, evt -> handleInAppNewCampaign());
        menuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        menuFile.add(menuNew);

        menuFile.add(initImportMenu());
        menuFile.add(initExportMenu());
        menuFile.add(initRefreshMenu());

        menuFile.add(createMenuItem("menuOptions.text", KeyEvent.VK_C, this::menuOptionsActionPerformed));

        JMenuItem miMHQOptions = createMenuItem("miMHQOptions.text", KeyEvent.VK_H,
              evt -> new MHQOptionsDialog(getFrame()).setVisible(true));
        miMHQOptions.setToolTipText(getTextAt("miMHQOptions.toolTipText"));
        menuFile.add(miMHQOptions);

        final JMenuItem miGameOptions = createMenuItem("miGameOptions.text", KeyEvent.VK_M, evt -> {
            final GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
            god.setEditable(true);
            if (god.showDialog().isConfirmed()) {
                getCampaign().setGameOptions(god.getOptions());
                getGui().refreshWindowTitle();
            }
        });
        miGameOptions.setToolTipText(getTextAt("miGameOptions.toolTipText"));
        menuFile.add(miGameOptions);

        final JMenuItem miMMClientOptions = createMenuItem("miMMClientOptions.text", KeyEvent.VK_O,
              evt -> new CommonSettingsDialog(getFrame(), null).setVisible(true));
        miMMClientOptions.setToolTipText(getTextAt("miMMClientOptions.toolTipText"));
        menuFile.add(miMMClientOptions);

        menuThemes = new JMenu(getTextAt("menuThemes.text"));
        menuThemes.setMnemonic(KeyEvent.VK_T);
        refreshThemeChoices();
        menuFile.add(menuThemes);

        menuFile.add(createMenuItem("menuExit.text", KeyEvent.VK_E, evt -> getApplication().exit(true)));

        return menuFile;
    }

    /**
     * The Import menu uses the following Mnemonic keys as of 25-MAR-2022: A, C, F, I, P
     */
    private JMenu initImportMenu() {
        JMenu menuImport = new JMenu(getTextAt("menuImport.text"));
        menuImport.setMnemonic(KeyEvent.VK_I);

        menuImport.add(createMenuItem("miImportPerson.text", KeyEvent.VK_P, evt -> loadPersonFile()));

        JMenuItem miImportIndividualRankSystem = createMenuItem("miImportIndividualRankSystem.text", KeyEvent.VK_I,
              evt -> getCampaign().setRankSystem(RankSystem.generateIndividualInstanceFromXML(
                    FileDialogs.openIndividualRankSystem(getFrame()).orElse(null))));
        miImportIndividualRankSystem.setToolTipText(getTextAt("miImportIndividualRankSystem.toolTipText"));
        menuImport.add(miImportIndividualRankSystem);

        menuImport.add(createMenuItem("miImportParts.text", KeyEvent.VK_A, evt -> loadPartsFile()));
        menuImport.add(createMenuItem("miLoadForces.text", KeyEvent.VK_F, evt -> loadListFile(true)));

        return menuImport;
    }

    /**
     * The Export menu uses the following Mnemonic keys as of 25-MAR-2022: C, X, S The CSV menu uses the following
     * Mnemonic keys as of 25-MAR-2022: F, P, U The XML menu uses the following Mnemonic keys as of 25-MAR-2022: C, I,
     * P, R
     */
    private JMenu initExportMenu() {
        JMenu menuExport = new JMenu(getTextAt("menuExport.text"));
        menuExport.setMnemonic(KeyEvent.VK_X);

        // region CSV Export
        JMenu miExportCSVFile = new JMenu(getTextAt("menuExportCSV.text"));
        miExportCSVFile.setMnemonic(KeyEvent.VK_C);

        miExportCSVFile.add(createMenuItem("miExportPersonnel.text", KeyEvent.VK_P, evt -> exportPersonnel()));
        miExportCSVFile.add(createMenuItem("miExportUnit.text", KeyEvent.VK_U, evt -> exportUnits()));
        miExportCSVFile.add(createMenuItem("miExportFinances.text", KeyEvent.VK_F, evt -> exportFinances()));

        menuExport.add(miExportCSVFile);
        // endregion CSV Export

        // region XML Export
        JMenu miExportXMLFile = new JMenu(getTextAt("menuExportXML.text"));
        miExportXMLFile.setMnemonic(KeyEvent.VK_X);

        miExportXMLFile.add(createMenuItem("miExportRankSystems.text", KeyEvent.VK_R,
              evt -> Ranks.exportRankSystemsToFile(FileDialogs.saveRankSystems(getFrame()).orElse(null),
                    getCampaign().getRankSystem())));

        miExportXMLFile.add(createMenuItem("miExportIndividualRankSystem.text", KeyEvent.VK_I,
              evt -> getCampaign().getRankSystem()
                           .writeToFile(FileDialogs.saveIndividualRankSystem(getFrame()).orElse(null))));

        JMenuItem miExportPlanetsXML = createMenuItem("miExportPlanets.text", KeyEvent.VK_P, evt -> {
            try {
                exportPlanets(FileType.XML,
                      getTextAt("dlgSavePlanetsXML.text"),
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

        JMenuItem miExportCampaignSubset = createMenuItem("miExportCampaignSubset.text", KeyEvent.VK_S, evt -> {
            CampaignExportWizard cew = new CampaignExportWizard(getCampaign());
            cew.display(CampaignExportWizard.CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        return menuExport;
    }

    /**
     * The Refresh menu uses the following Mnemonic keys as of 12-APR-2022: A, C, D, F, P, R, U
     */
    private JMenu initRefreshMenu() {
        JMenu menuRefresh = new JMenu(getTextAt("menuRefresh.text"));
        menuRefresh.setMnemonic(KeyEvent.VK_R);

        menuRefresh.add(createMenuItem("miRefreshUnitCache.text", KeyEvent.VK_U,
              evt -> MekSummaryCache.refreshUnitData(false)));
        menuRefresh.add(createMenuItem("miRefreshCamouflage.text", KeyEvent.VK_C, evt -> {
            MHQStaticDirectoryManager.refreshCamouflageDirectory();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshPortraits.text", KeyEvent.VK_P, evt -> {
            MHQStaticDirectoryManager.refreshPortraitDirectory();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshFormationIcons.text", KeyEvent.VK_F, evt -> {
            MHQStaticDirectoryManager.refreshFormationIcons();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshAwards.text", KeyEvent.VK_A, evt -> {
            MHQStaticDirectoryManager.refreshAwardIcons();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshStoryIcons.text", KeyEvent.VK_A, evt -> {
            MHQStaticDirectoryManager.refreshStorySplash();
            getGui().refreshAllTabs();
        }));
        menuRefresh.add(createMenuItem("miRefreshRanks.text", KeyEvent.VK_R,
              evt -> Ranks.reinitializeRankSystems(getCampaign())));

        JMenuItem miRefreshFinancialInstitutions = createMenuItem("miRefreshFinancialInstitutions.text",
              KeyEvent.VK_UNDEFINED, evt -> FinancialInstitutions.initializeFinancialInstitutions());
        miRefreshFinancialInstitutions.setToolTipText(getTextAt("miRefreshFinancialInstitutions.toolTipText"));
        menuRefresh.add(miRefreshFinancialInstitutions);

        return menuRefresh;
    }

    /**
     * The Marketplace menu uses the following Mnemonic keys as of 19-March-2020: A, B, C, H, M, N, P, R, S, U
     */
    private JMenu initMarketMenu() {
        JMenu menuMarket = new JMenu(getTextAt("menuMarket.text"));
        menuMarket.setMnemonic(KeyEvent.VK_M);

        menuMarket.add(createMenuItem("miRecruitment.text", KeyEvent.VK_R,
              evt -> getGui().openRecruitmentDialog()));

        JMenuItem miContractMarket = createMenuItem("miContractMarket.text", KeyEvent.VK_C,
              evt -> getGui().showContractMarket());
        miContractMarket.setVisible(getCampaign().getCampaignOptions().isUseStratCon());
        menuMarket.add(miContractMarket);

        JMenuItem miUnitMarket = createMenuItem("miUnitMarket.text", KeyEvent.VK_U, evt -> getGui().showUnitMarket());
        miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        menuMarket.add(miUnitMarket);

        JMenuItem miPurchaseUnit = createMenuItem("miPurchaseUnit.text", KeyEvent.VK_N, evt -> {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(getFrame());
            if (!MekSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(
                  getFrame(), unitLoadingDialog, getCampaign(), true);
            usd.setVisible(true);
        });
        menuMarket.add(miPurchaseUnit);

        menuMarket.add(createMenuItem("miBuyParts.text", KeyEvent.VK_P,
              evt -> new PartsStoreDialog(true, getGui()).setVisible(true)));

        menuMarket.add(createMenuItem("miBulkRecruitment.text", KeyEvent.VK_B,
              evt -> getGui().openBulkRecruitmentDialog()));

        JMenu menuRecruitment = new JMenu(getTextAt("menuRecruitment.text"));
        menuRecruitment.setMnemonic(KeyEvent.VK_H);

        menuRecruitment.add(createMenuItem("menuRecruitment.blank.text", KeyEvent.VK_UNDEFINED, this::addBlankPerson));

        JMenu menuCombatRecruitment = new JMenu(getTextAt("menuRecruitment.combat"));
        JMenu menuSupportRecruitment = new JMenu(getTextAt("menuRecruitment.support"));
        JMenu menuCivilianRecruitment = new JMenu(getTextAt("menuRecruitment.civilian"));

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

        menuRecruitment.add(menuCombatRecruitment);
        menuRecruitment.add(menuSupportRecruitment);
        menuRecruitment.add(menuCivilianRecruitment);
        menuMarket.add(menuRecruitment);

        menuMarket.add(initTempPoolMenu());

        return menuMarket;
    }

    /**
     * The Astech Pool menu uses the following Mnemonic keys as of 19-March-2020: B, E, F, H The Medic Pool menu uses
     * the following Mnemonic keys as of 19-March-2020: B, E, H, R
     */
    private JMenu initTempPoolMenu() {
        JMenu menuTempPool = new JMenu(getTextAt("menuTempPool.text"));

        JMenuItem miTempPoolFullStrength = createMenuItem("miTempPoolFullStrength.text", KeyEvent.VK_UNDEFINED,
              evt -> bringAllTempCrewsToFullStrength());
        menuTempPool.add(miTempPoolFullStrength);

        JMenuItem miTempPoolReleaseAll = createMenuItem("miTempPoolReleaseAll.text", KeyEvent.VK_UNDEFINED,
              evt -> releaseAllTempCrews());
        menuTempPool.add(miTempPoolReleaseAll);

        JMenuItem miTempPoolReleaseSurplus = createMenuItem("miTempPoolReleaseSurplus.text", KeyEvent.VK_UNDEFINED,
              evt -> releaseSurplusTempCrews());
        menuTempPool.add(miTempPoolReleaseSurplus);

        menuTempPool.addMenuListener(menuListenerFor(() -> {
            // For Astech/Medic: need + pool = what would be needed if there were no temp pool at all.
            // resetAsTechPool/resetMedicPool is a no-op only when pool == max(0, real need).
            int astechPool = getCampaign().getTemporaryAsTechPool();
            int astechIdealPool = Math.max(0, getCampaign().getAsTechNeed() + astechPool);
            int medicPool = getCampaign().getTemporaryMedicPool();
            int medicIdealPool = Math.max(0, getCampaign().getMedicsNeed() + medicPool);

            boolean anyNeed = (astechPool != astechIdealPool) || (medicPool != medicIdealPool) || anyBlobRoleHasNeed();
            setMenuItemState(miTempPoolFullStrength, anyNeed, getTextAt("miTempPoolFullStrength.disabledTip"));

            boolean anyPool = (astechPool > 0) || (medicPool > 0) || anyBlobRoleHasPool();
            setMenuItemState(miTempPoolReleaseAll, anyPool, getTextAt("miTempPoolReleaseAll.disabledTip"));

            boolean anySurplus =
                  (astechPool > astechIdealPool) || (medicPool > medicIdealPool) || anyBlobRoleHasSurplus();
            setMenuItemState(miTempPoolReleaseSurplus, anySurplus, getTextAt("miTempPoolReleaseSurplus.disabledTip"));
        }));

        menuTempPool.addSeparator();

        // region Astech Pool
        JMenu menuAstechPool = new JMenu(getTextAt("menuAstechPool.text"));
        menuAstechPool.setMnemonic(KeyEvent.VK_A);

        JMenuItem miHireAstechs = createMenuItem("miHireAstechs.text", KeyEvent.VK_H, evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt("popupHireAstechsNum.text"), 1, 0, MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAstechPool.add(miHireAstechs);

        JMenuItem miFireAstechs = createMenuItem("miFireAstechs.text", KeyEvent.VK_E, evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt("popupFireAstechsNum.text"), 1, 0, getCampaign().getTemporaryAsTechPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAstechPool.add(miFireAstechs);

        JMenuItem miFullStrengthAstechs = createMenuItem("miFullStrengthAstechs.text", KeyEvent.VK_B,
              evt -> getCampaign().resetAsTechPool());
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = createMenuItem("miFireAllAstechs.text", KeyEvent.VK_R,
              evt -> getCampaign().emptyAsTechPool());
        menuAstechPool.add(miFireAllAstechs);

        menuAstechPool.addMenuListener(menuListenerFor(() -> {
            int pool = getCampaign().getTemporaryAsTechPool();
            int need = getCampaign().getAsTechNeed();
            // need + pool = what would be needed if there were no temp pool at all;
            // the action is a no-op only when pool == max(0, that real need)
            int idealPool = Math.max(0, need + pool);
            setMenuItemState(miFireAstechs, pool > 0, getTextAt("miFireAstechs.disabledTip"));
            setMenuItemState(miFullStrengthAstechs, pool != idealPool, getTextAt("miFullStrengthAstechs.disabledTip"));
            setMenuItemState(miFireAllAstechs, pool > 0, getTextAt("miFireAllAstechs.disabledTip"));
        }));

        menuTempPool.add(menuAstechPool);
        // endregion Astech Pool

        // region Medic Pool
        JMenu menuMedicPool = new JMenu(getTextAt("menuMedicPool.text"));
        menuMedicPool.setMnemonic(KeyEvent.VK_M);

        JMenuItem miHireMedics = createMenuItem("miHireMedics.text", KeyEvent.VK_H, evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt("popupHireMedicsNum.text"), 1, 0, MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = createMenuItem("miFireMedics.text", KeyEvent.VK_E, evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt("popupFireMedicsNum.text"), 1, 0, getCampaign().getTemporaryMedicPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miFireMedics);

        JMenuItem miFullStrengthMedics = createMenuItem("miFullStrengthMedics.text", KeyEvent.VK_B,
              evt -> getCampaign().resetMedicPool());
        menuMedicPool.add(miFullStrengthMedics);

        JMenuItem miFireAllMedics = createMenuItem("miFireAllMedics.text", KeyEvent.VK_R,
              evt -> getCampaign().emptyMedicPool());
        menuMedicPool.add(miFireAllMedics);

        menuMedicPool.addMenuListener(menuListenerFor(() -> {
            int pool = getCampaign().getTemporaryMedicPool();
            int need = getCampaign().getMedicsNeed();
            // need + pool = what would be needed if there were no temp pool at all;
            // the action is a no-op only when pool == max(0, that real need)
            int idealPool = Math.max(0, need + pool);
            setMenuItemState(miFireMedics, pool > 0,
                  getTextAt("miFireMedics.disabledTip"));
            setMenuItemState(miFullStrengthMedics, pool != idealPool,
                  getTextAt("miFullStrengthMedics.disabledTip"));
            setMenuItemState(miFireAllMedics, pool > 0,
                  getTextAt("miFireAllMedics.disabledTip"));
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

        return menuTempPool;
    }

    /**
     * The Reports menu uses the following Mnemonic keys as of 19-March-2020: C, H, P, T, U
     */
    private JMenu initReportsMenu() {
        JMenu menuReports = new JMenu(getTextAt("menuReports.text"));
        menuReports.setMnemonic(KeyEvent.VK_E);
        menuReports.add(createMenuItem("miDragoonsRating.text", KeyEvent.VK_U,
              evt -> new ReputationReportDialog(getFrame(), getCampaign()).setVisible(true)));
        menuReports.add(createMenuItem("miPersonnelReport.text", KeyEvent.VK_P,
              evt -> new PersonnelReportDialog(getFrame(), new PersonnelReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miHangarBreakdown.text", KeyEvent.VK_H,
              evt -> new HangarReportDialog(getFrame(), new HangarReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miTransportReport.text", KeyEvent.VK_T,
              evt -> new TransportReportDialog(getFrame(), new TransportReport(getCampaign())).setVisible(true)));
        menuReports.add(createMenuItem("miCargoReport.text", KeyEvent.VK_C,
              evt -> new CargoReportDialog(getFrame(), new CargoReport(getCampaign())).setVisible(true)));
        return menuReports;
    }

    /**
     * The View menu uses the following Mnemonic keys as of 02-June-2020: H, R
     */
    private JMenu initViewMenu() {
        JMenu menuView = new JMenu(getTextAt("menuView.text"));
        menuView.setMnemonic(KeyEvent.VK_V);

        JMenuItem miHistoricalDailyReportDialog = createMenuItem("miShowHistoricalReportLog.text",
              KeyEvent.VK_H,
              evt -> {
                  HistoricalDailyReportDialog histDailyReportDialog = new HistoricalDailyReportDialog(getFrame(),
                        getGui());
                  histDailyReportDialog.setModal(true);
                  histDailyReportDialog.setVisible(true);
                  histDailyReportDialog.dispose();
              });
        menuView.add(miHistoricalDailyReportDialog);

        miRetirementDefectionDialog = createMenuItem("miRetirementDefectionDialog.text", KeyEvent.VK_R,
              evt -> getGui().showRetirementDefectionDialog());
        miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().isUseRandomRetirement());
        menuView.add(miRetirementDefectionDialog);

        miAwardEligibilityDialog = createMenuItem("miAwardEligibilityDialog.text", KeyEvent.VK_R,
              evt -> showAwardEligibilityDialog());
        miAwardEligibilityDialog.setVisible(getCampaign().getCampaignOptions().isEnableAutoAwards());
        menuView.add(miAwardEligibilityDialog);

        return menuView;
    }

    private JMenu initManageCampaignMenu() {
        JMenu menuManage = new JMenu(getTextAt("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_C);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = createMenuItem("miGMToolsDialog.text", KeyEvent.VK_G,
              evt -> new GMToolsDialog(getFrame(), getGui(), null).setVisible(true));
        menuManage.add(miGMToolsDialog);

        miPlanetarySystemEditor = createMenuItem("miPlanetarySystemEditor.text", KeyEvent.VK_P,
              evt -> new PlanetarySystemEditorDialog(getFrame(), getCampaign()).setVisible(true));
        miPlanetarySystemEditor.setVisible(getCampaign().isGM());
        menuManage.add(miPlanetarySystemEditor);

        JMenuItem miBloodnames = createMenuItem("miRandomBloodnames.text", KeyEvent.VK_B, evt -> {
            for (final Person person : getCampaign().getAllPersonnel()) {
                getCampaign().checkBloodnameAdd(person, false);
            }
        });
        menuManage.add(miBloodnames);

        JMenuItem miScenarioEditor = createMenuItem("miScenarioEditor.text", KeyEvent.VK_S,
              evt -> new ScenarioTemplateEditorDialog(getFrame()).setVisible(true));
        menuManage.add(miScenarioEditor);

        miCompanyGenerator = createMenuItem("miCompanyGenerator.text", KeyEvent.VK_C,
              evt -> new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(true));
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
        menuManage.add(miCompanyGenerator);

        JMenuItem miAutoResolveBehaviorEditor = createMenuItem("miAutoResolveBehaviorSettings.text",
              KeyEvent.VK_T,
              evt -> {
                  var autoResolveBehaviorSettingsDialog = new AutoResolveBehaviorSettingsDialog(getFrame(),
                        getCampaign());
                  autoResolveBehaviorSettingsDialog.setVisible(true);
                  autoResolveBehaviorSettingsDialog.pack();
              });

        menuManage.add(miAutoResolveBehaviorEditor);

        initMenuRoleplay(menuManage);

        return menuManage;
    }

    private void initMenuRoleplay(JMenu menuManage) {
        JMenu menuRoleplay = new JMenu(getTextAt("menuRoleplay.text"));
        menuRoleplay.setMnemonic(KeyEvent.VK_R);
        menuManage.setName("menuRoleplay");

        JMenuItem miLifePathBuilder = createMenuItem("miLifePathBuilder.text", KeyEvent.VK_L,
              evt -> new LifePathBuilderDialog(getCampaign(), getFrame()));
        menuRoleplay.add(miLifePathBuilder);
        menuManage.add(menuRoleplay);
    }

    /**
     * The Help menu uses the following Mnemonic keys as of 19-March-2020: A
     */
    private JMenu initHelpMenu() {
        JMenu menuHelp = new JMenu(getTextAt("menuHelp.text"));
        menuHelp.setMnemonic(KeyEvent.VK_SLASH);
        menuHelp.setName("helpMenu");

        menuHelp.addSeparator();

        menuHelp.add(createMenuItem("menuReportBug.text", KeyEvent.VK_UNDEFINED,
              evt -> new EasyBugReportDialog(getFrame(), getCampaign())));
        menuHelp.add(new CopySystemDataAction(MHQConstants.PROJECT_NAME));

        menuHelp.addSeparator();

        menuHelp.add(createMenuItem("menuAbout.text", KeyEvent.VK_A,
              evt -> new MekHQAboutDialog(getFrame()).show()));

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
        Finances finances = getCampaign().getFinances();
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
                Person spouse = p.getGenealogy().getSpouse();
                if (p.getGenealogy().hasSpouse() &&
                          ((spouse == null) || (getCampaign().getPerson(spouse.getId()) == null))) {
                    // If this happens, we need to clear the spouse
                    if (p.getMaidenName() != null) {
                        p.setSurname(p.getMaidenName());
                    }

                    p.getGenealogy().setSpouse(null);
                }

                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null) && (getCampaign().getPerson(fatherId) == null)) {
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

        // Unregister handlers for campaign tabs
        for (int i = 0; i < getTabMain().getTabCount(); i++) {
            Component tab = getTabMain().getComponentAt(i);
            if (tab instanceof CampaignGuiTab) {
                ((CampaignGuiTab) tab).deactivateTab();
            }
        }

        // Unregister other handlers
        MekHQ.unregisterHandler(this);
        MekHQ.unregisterHandler(getGui());

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
        return BLOB_CREW_ROLES.stream().anyMatch(role -> c.isBlobCrewEnabled(role) && c.getTempCrewPool(role) > 0);
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

        setMenuItemState(fireItem, pool > 0, getTextAt("miFireBlobCrew.disabledTip"));
        setMenuItemState(fullStrengthItem, unitsNeedCrew, getTextAt("miFullStrengthBlobCrew.disabledTip"));
        setMenuItemState(fireAllItem, pool > 0, getTextAt("miFireAllBlobCrew.disabledTip"));
    }

    private JMenu buildBlobCrewPoolSubMenu(JMenu parentMenu, PersonnelRole role, boolean isVisible,
          String menuKey, String hireKey, String hirePopupKey,
          String fireSomeKey, String fireSomePopupKey,
          String fullStrengthKey, String fireAllKey) {
        JMenu menu = new JMenu(getTextAt(menuKey));
        menu.setVisible(isVisible);

        JMenuItem miHire = createMenuItem(hireKey, KeyEvent.VK_UNDEFINED, evt -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt(hirePopupKey), 1, 0, MAX_QUANTITY_SPINNER);
            dialog.setVisible(true);
            if (dialog.getValue() >= 0) {
                getCampaign().increaseTempCrewPool(role, dialog.getValue());
            }
        });
        menu.add(miHire);

        JMenuItem miFireSome = createMenuItem(fireSomeKey, KeyEvent.VK_UNDEFINED, evt -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(getFrame(),
                  true, getTextAt(fireSomePopupKey), 1, 0,
                  getCampaign().getTempCrewPool(role));
            dialog.setVisible(true);
            if (dialog.getValue() >= 0) {
                getCampaign().decreaseTempCrewPool(role, dialog.getValue());
            }
        });
        menu.add(miFireSome);

        JMenuItem miFullStrength = createMenuItem(fullStrengthKey, KeyEvent.VK_UNDEFINED, evt -> {
            getCampaign().resetTempCrewPoolForRole(role);
            getCampaign().distributeTempCrewPoolToUnits(role);
        });
        menu.add(miFullStrength);

        JMenuItem miFireAll = createMenuItem(fireAllKey, KeyEvent.VK_UNDEFINED,
              evt -> getCampaign().setTempCrewPool(role, 0));
        menu.add(miFireAll);

        menu.addMenuListener(menuListenerFor(
              () -> updateBlobCrewPoolMenuItems(role, miFireSome, miFullStrength, miFireAll)));

        parentMenu.add(menu);
        return menu;
    }

    /**
     * Creates and configures a {@link JMenuItem} with localized text, an optional keyboard mnemonic, and an action
     * listener. Component's name is generated by removing {@code ".text"} suffix from {@code textKey} parameter.
     *
     * @param textKey  the resource key with the menu item's localized display text, must end with {@code ".text"}
     * @param mnemonic the keyboard shortcut or {@code KeyEvent.VK_UNDEFINED} if no mnemonic is required
     * @param action   the {@link ActionListener} to trigger when the menu item is selected
     *
     * @return the initialized {@code JMenuItem}
     */
    private static JMenuItem createMenuItem(String textKey, int mnemonic, ActionListener action) {
        if (!textKey.endsWith(MENU_ITEM_TEXT_SUFFIX)) {
            throw new IllegalArgumentException(String.format("Text key must end with '%s', actual: '%s'",
                  MENU_ITEM_TEXT_SUFFIX, textKey));
        }
        JMenuItem item = new JMenuItem(getTextAt(textKey));
        item.setName(textKey.substring(0, textKey.length() - MENU_ITEM_TEXT_SUFFIX.length()));
        if (mnemonic != KeyEvent.VK_UNDEFINED) {
            item.setMnemonic(mnemonic);
        }
        item.addActionListener(action);
        return item;
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
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseBlobInfantry()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (campaignOptions.isUseBlobBattleArmor()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (campaignOptions.isUseBlobVehicleCrewGround()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (campaignOptions.isUseBlobVehicleCrewVTOL()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (campaignOptions.isUseBlobVehicleCrewNaval()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (campaignOptions.isUseBlobVesselPilot()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (campaignOptions.isUseBlobVesselGunner()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (campaignOptions.isUseBlobVesselCrew()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_CREW);
        }
    }

    private void releaseSurplusTempCrews() {
        getCampaign().releaseSurplusAsTechPool();
        getCampaign().releaseSurplusMedicPool();
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseBlobInfantry()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (campaignOptions.isUseBlobBattleArmor()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (campaignOptions.isUseBlobVehicleCrewGround()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (campaignOptions.isUseBlobVehicleCrewVTOL()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (campaignOptions.isUseBlobVehicleCrewNaval()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (campaignOptions.isUseBlobVesselPilot()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (campaignOptions.isUseBlobVesselGunner()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (campaignOptions.isUseBlobVesselCrew()) {
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
            AbstractDivorce divorce = getCampaign().getDivorce();
            divorce.setUseClanPersonnelDivorce(newOptions.isUseClanPersonnelDivorce());
            divorce.setUsePrisonerDivorce(newOptions.isUsePrisonerDivorce());
            divorce.setUseRandomOppositeSexDivorce(newOptions.isUseRandomOppositeSexDivorce());
            divorce.setUseRandomSameSexDivorce(newOptions.isUseRandomSameSexDivorce());
            divorce.setUseRandomClanPersonnelDivorce(newOptions.isUseRandomClanPersonnelDivorce());
            divorce.setUseRandomPrisonerDivorce(newOptions.isUseRandomPrisonerDivorce());
            if (divorce.getMethod().isDiceRoll()) {
                ((RandomDivorce) divorce).setDivorceDiceSize(newOptions.getRandomDivorceDiceSize());
            }
        }

        if (randomMarriageMethod != newOptions.getRandomMarriageMethod()) {
            getCampaign().setMarriage(newOptions.getRandomMarriageMethod().getMethod(newOptions));
        } else {
            AbstractMarriage marriage = getCampaign().getMarriage();
            marriage.setUseClanPersonnelMarriages(newOptions.isUseClanPersonnelMarriages());
            marriage.setUsePrisonerMarriages(newOptions.isUsePrisonerMarriages());
            marriage.setUseRandomClanPersonnelMarriages(newOptions.isUseRandomClanPersonnelMarriages());
            marriage.setUseRandomPrisonerMarriages(newOptions.isUseRandomPrisonerMarriages());
            if (marriage.getMethod().isDiceRoll()) {
                ((RandomMarriage) marriage).setMarriageDiceSize(newOptions.getRandomMarriageDiceSize());
            }
        }

        if (randomProcreationMethod != newOptions.getRandomProcreationMethod()) {
            getCampaign().setProcreation(newOptions.getRandomProcreationMethod().getMethod(newOptions));
        } else {
            AbstractProcreation procreation = getCampaign().getProcreation();
            procreation.setUseClanPersonnelProcreation(newOptions.isUseClanPersonnelProcreation());
            procreation.setUsePrisonerProcreation(newOptions.isUsePrisonerProcreation());
            procreation.setUseRelationshiplessProcreation(newOptions.isUseRelationshiplessRandomProcreation());
            procreation.setUseRandomClanPersonnelProcreation(newOptions.isUseRandomClanPersonnelProcreation());
            procreation.setUseRandomPrisonerProcreation(newOptions.isUseRandomPrisonerProcreation());
            if (procreation.getMethod().isDiceRoll()) {
                ((RandomProcreation) procreation).setRelationshipDieSize(
                      newOptions.getRandomProcreationRelationshipDiceSize());
                ((RandomProcreation) procreation).setRelationshiplessDieSize(
                      newOptions.getRandomProcreationRelationshiplessDiceSize());
            }
        }

        // Clear Procreation Data if Disabled
        if (!newOptions.isUseManualProcreation() && newOptions.getRandomProcreationMethod().isNone()) {
            getCampaign().getAllPersonnel()
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
     * Retrieves localized text from the panel's resource bundle.
     */
    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

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
