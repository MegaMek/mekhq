/*
 * CampaignGUI.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import chat.ChatClient;
import megamek.client.RandomNameGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.client.ui.swing.GameOptionsDialog;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.TechConstants;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.event.AssetEvent;
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.LoanEvent;
import mekhq.campaign.event.MedicPoolChangedEvent;
import mekhq.campaign.event.MissionEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.ReportEvent;
import mekhq.campaign.event.TransactionEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.RatingReport;
import mekhq.campaign.report.Report;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.gui.dialog.AdvanceDaysDialog;
import mekhq.gui.dialog.BatchXPDialog;
import mekhq.gui.dialog.BloodnameDialog;
import mekhq.gui.dialog.CampaignOptionsDialog;
import mekhq.gui.dialog.ContractMarketDialog;
import mekhq.gui.dialog.DailyReportLogDialog;
import mekhq.gui.dialog.DataLoadingDialog;
import mekhq.gui.dialog.GMToolsDialog;
import mekhq.gui.dialog.HireBulkPersonnelDialog;
import mekhq.gui.dialog.MaintenanceReportDialog;
import mekhq.gui.dialog.MekHQAboutBox;
import mekhq.gui.dialog.MercRosterDialog;
import mekhq.gui.dialog.NewRecruitDialog;
import mekhq.gui.dialog.NewsReportDialog;
import mekhq.gui.dialog.PartsStoreDialog;
import mekhq.gui.dialog.PersonnelMarketDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.dialog.RefitNameDialog;
import mekhq.gui.dialog.ReportDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.dialog.ShipSearchDialog;
import mekhq.gui.dialog.UnitMarketDialog;
import mekhq.gui.dialog.UnitSelectorDialog;
import mekhq.gui.model.PartsTableModel;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {
    private static final long serialVersionUID = -687162569841072579L;
    
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;

    private JFrame frame;

    private MekHQ app;

    private ResourceBundle resourceMap;

    /* for the main panel */
    private JSplitPane mainPanel;
    private JTabbedPane tabMain;
    private DailyReportLogPanel panLog;

    /* For the menu bar */
    private JMenuBar menuBar;
    private JMenu menuThemes;
    private JMenuItem miDetachLog;
    private JMenuItem miAttachLog;
    private JMenuItem miContractMarket;
    private JMenuItem miUnitMarket;
    private JMenuItem miShipSearch;
    private JMenuItem miRetirementDefectionDialog;
    private JCheckBoxMenuItem miShowOverview;
    
    private EnumMap<GuiTabType,CampaignGuiTab> standardTabs;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblLocation;
    private JLabel lblRating;
    private JLabel lblFunds;
    private JLabel lblTempAstechs;
    private JLabel lblTempMedics;
    @SuppressWarnings("unused")
    private JLabel lblCargo; // FIXME: Re-add this in an optionized form

    /* for the top button panel */
    private JPanel btnPanel;
    private JToggleButton btnGMMode;
    private JToggleButton btnOvertime;
    private JButton btnAdvanceDay;

    ReportHyperlinkListener reportHLL;

    private DailyReportLogDialog logDialog;
    private AdvanceDaysDialog advanceDaysDialog;
    private BloodnameDialog bloodnameDialog;

    public CampaignGUI(MekHQ app) {
        this.app = app;
        reportHLL = new ReportHyperlinkListener(this);
    	standardTabs = new EnumMap<>(GuiTabType.class);
        initComponents();
        MekHQ.registerHandler(this);
    }

    public void showAboutBox() {
        MekHQAboutBox aboutBox = new MekHQAboutBox(getFrame());
        aboutBox.setLocationRelativeTo(getFrame());
        aboutBox.setVisible(true);
    }

    private void showDailyReportDialog() {
        mainPanel.remove(panLog);
        miDetachLog.setEnabled(false);
        miAttachLog.setEnabled(true);
        mainPanel.setOneTouchExpandable(false);
        logDialog.setVisible(true);
        refreshReport();
        this.revalidate();
        this.repaint();
    }

    public void hideDailyReportDialog() {
        logDialog.setVisible(false);
        mainPanel.setRightComponent(panLog);
        mainPanel.setOneTouchExpandable(true);
        miDetachLog.setEnabled(true);
        miAttachLog.setEnabled(false);
        this.revalidate();
        this.repaint();
    }

    public void showRetirementDefectionDialog() {
        /*
         * if there are unresolved personnel, show the results view; otherwise,
         * present the retirement view to give the player a chance to follow a
         * custom schedule
         */
        RetirementDefectionDialog rdd = new RetirementDefectionDialog(this,
                null, getCampaign().getRetirementDefectionTracker()
                        .getRetirees().size() == 0);
        rdd.setVisible(true);
        if (!rdd.wasAborted()) {
            getCampaign().applyRetirement(rdd.totalPayout(),
                    rdd.getUnitAssignments());
        }
    }

    public void toggleOverviewTab() {
    	boolean show = !hasTab(GuiTabType.OVERVIEW);
        miShowOverview.setSelected(show);
        showOverviewTab(show);
    }
    
    public void showOverviewTab(boolean show) {
    	if (show) {
    		addStandardTab(GuiTabType.OVERVIEW);
    	} else {
    		removeStandardTab(GuiTabType.OVERVIEW);
    	}    	
    }

    public void showGMToolsDialog() {
        GMToolsDialog gmTools = new GMToolsDialog(getFrame(), this);
        gmTools.setVisible(true);
    }

    public void showAdvanceDaysDialog() {
        advanceDaysDialog = new AdvanceDaysDialog(getFrame(), this, reportHLL);
        advanceDaysDialog.setVisible(true);
        advanceDaysDialog.dispose();
    }

    public void randomizeAllBloodnames() {
        for (Person p : getCampaign().getPersonnel()) {
            if (!p.isClanner()) {
                continue;
            }
            getCampaign().checkBloodnameAdd(p, p.getPrimaryRole());
            getCampaign().personUpdated(p);
        }
    }

    public void spendBatchXP() {
        BatchXPDialog batchXPDialog = new BatchXPDialog(getFrame(), getCampaign());
        batchXPDialog.setVisible(true);
        
        if(batchXPDialog.hasDataChanged()) {
            refreshReport();
        }
    }

    public void showBloodnameDialog() {
        bloodnameDialog.setFaction(getCampaign().getFactionCode());
        bloodnameDialog.setYear(getCampaign().getCalendar().get(
                java.util.Calendar.YEAR));
        bloodnameDialog.setVisible(true);
    }

    private void initComponents() {

        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", new EncodeControl()); //$NON-NLS-1$

        frame = new JFrame("MekHQ"); //$NON-NLS-1$
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabMain = new JTabbedPane();
        tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
        tabMain.setMinimumSize(new java.awt.Dimension(600, 200));
        tabMain.setPreferredSize(new java.awt.Dimension(900, 300));

        addStandardTab(GuiTabType.TOE);
        addStandardTab(GuiTabType.BRIEFING);
        addStandardTab(GuiTabType.MAP);
        addStandardTab(GuiTabType.PERSONNEL);
        addStandardTab(GuiTabType.HANGAR);
        addStandardTab(GuiTabType.WAREHOUSE);
        addStandardTab(GuiTabType.REPAIR);
        addStandardTab(GuiTabType.INFIRMARY);
        addStandardTab(GuiTabType.MEKLAB);
        addStandardTab(GuiTabType.FINANCES);
        addStandardTab(GuiTabType.OVERVIEW);

        initMain();
        initTopButtons();
        initStatusBar();

        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.PAGE_START);
        add(statusPanel, BorderLayout.PAGE_END);
        
        standardTabs.values().forEach(t -> t.refreshAll());

        refreshCalendar();
        initReport();
        refreshFunds();
        refreshRating();
        refreshLocation();
        refreshTempAstechs();
        refreshTempMedics();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(Math.min(MAX_START_WIDTH, dim.width),
                Math.min(MAX_START_HEIGHT, dim.height));

        // Determine the new location of the window
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the window
        frame.setLocation(x, y);

        initMenu();
        frame.setJMenuBar(menuBar);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();

        if (isMacOSX()) {
            enableFullScreenMode(frame);
        }

        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                getApplication().exit();
            }
        });

        mainPanel.setDividerLocation(0.75);
    }
    
    public CampaignGuiTab getTab(GuiTabType tabType) {
    	return standardTabs.get(tabType);
    }
    
    public boolean hasTab(GuiTabType tabType) {
    	return standardTabs.containsKey(tabType);
    }
    
    /**
     * Adds one of the built-in tabs to the gui, if it is not already present.
     * 
     * @param tab The type of tab to add
     */
    public void addStandardTab(GuiTabType tab) {
    	if (tab.equals(GuiTabType.CUSTOM)) {
    		throw new IllegalArgumentException("Attempted to add custom tab as standard");
    	}
    	if (!standardTabs.containsKey(tab)) {
    		CampaignGuiTab t = tab.createTab(this);
    		standardTabs.put(tab, t);
    		int index = tabMain.getTabCount();
    		for (int i = 0; i < tabMain.getTabCount(); i++) {
    			if (((CampaignGuiTab)tabMain.getComponentAt(i)).tabType().getDefaultPos() > tab.getDefaultPos()) {
    				index = i;
    				break;
    			}
    		}
    		tabMain.insertTab(t.getTabName(), null, t, null, index);
    	}
    }
    
    /**
     * Adds a custom tab to the gui at the end
     * 
     * @param tab The tab to add
     */
    public void addCustomTab(CampaignGuiTab tab) {
    	if (tabMain.indexOfComponent(tab) >= 0) {
    		return;
    	}
    	if (tab.tabType().equals(GuiTabType.CUSTOM)) {
    		tabMain.addTab(tab.getTabName(), tab);
    	} else {
    		addStandardTab(tab.tabType());
    	}
    }
    
    /**
     * Adds a custom tab to the gui in the specified position. If <code>tab</code> is a built-in
     * type it will be placed in its normal position if it does not already exist.
     * 
     * @param tab	The tab to add
     * @param index	The position to place the tab
     */
    public void insertCustomTab(CampaignGuiTab tab, int index) {
    	if (tabMain.indexOfComponent(tab) >= 0) {
    		return;
    	}
    	if (tab.tabType().equals(GuiTabType.CUSTOM)) {
    		tabMain.insertTab(tab.getTabName(), null, tab, null, Math.min(index, tabMain.getTabCount()));
    	} else {
    		addStandardTab(tab.tabType());
    	}
    }
    
    /**
     * Adds a custom tab to the gui positioned after one of the built-in tabs
     * 
     * @param tab		The tab to add
     * @param stdTab	The build-in tab after which to place the new one
     */
    public void insertCustomTabAfter(CampaignGuiTab tab, GuiTabType stdTab) {
    	if (tabMain.indexOfComponent(tab) >= 0) {
    		return;
    	}
    	if (tab.tabType().equals(GuiTabType.CUSTOM)) {
	    	int index = tabMain.indexOfTab(stdTab.getTabName());
	    	if (index < 0) {
	    		if (stdTab.getDefaultPos() == 0) {
	    			index = tabMain.getTabCount();
	    		} else {
		    		for (int i = stdTab.getDefaultPos() - 1; i >= 0; i--) {
		    			index = tabMain.indexOfTab(GuiTabType.values()[i].getTabName());
		    			if (index >= 0) {
		    				break;
		    			}
		    		}
	    		}
	    	}
			insertCustomTab(tab, index);
    	} else {
    		addStandardTab(tab.tabType());
    	}
    }
    
    /**
     * Adds a custom tab to the gui positioned before one of the built-in tabs
     * 
     * @param tab		The tab to add
     * @param stdTab	The build-in tab before which to place the new one
     */
    public void insertCustomTabBefore(CampaignGuiTab tab, GuiTabType stdTab) {
    	if (tabMain.indexOfComponent(tab) >= 0) {
    		return;
    	}
    	if (tab.tabType().equals(GuiTabType.CUSTOM)) {
	    	int index = tabMain.indexOfTab(stdTab.getTabName());
	    	if (index < 0) {
	    		if (stdTab.getDefaultPos() == GuiTabType.values().length - 1) {
	    			index = tabMain.getTabCount();
	    		} else {
		    		for (int i = stdTab.getDefaultPos() + 1; i >= GuiTabType.values().length; i++) {
		    			index = tabMain.indexOfTab(GuiTabType.values()[i].getTabName());
		    			if (index >= 0) {
		    				break;
		    			}
		    		}
	    		}
	    	}
			insertCustomTab(tab, Math.max(0, index - 1));
    	} else {
    		addStandardTab(tab.tabType());
    	}
    }
    
    /**
     * Removes one of the built-in tabs from the gui.
     * 
     * @param tabType	The tab to remove
     */
    public void removeStandardTab(GuiTabType tabType) {
    	CampaignGuiTab tab = standardTabs.get(tabType);
    	if (tab != null) {
    		MekHQ.unregisterHandler(tab);
        	removeTab(tab);
    	}
    }
    
    /**
     * Removes a tab from the gui.
     * 
     * @param tab	The tab to remove
     */
    public void removeTab(CampaignGuiTab tab) {
        tab.disposeTab();
    	removeTab(tab.getTabName());
    }
    
    /**
     * Removes a tab from the gui.
     * 
     * @param tabName	The name of the tab to remove
     */
    public void removeTab(String tabName) {
    	int index = tabMain.indexOfTab(tabName);
    	if (index >= 0) {
        	CampaignGuiTab tab = (CampaignGuiTab)tabMain.getComponentAt(index);
        	if (standardTabs.containsKey(tab.tabType())) {
        		standardTabs.remove(tab.tabType());
        	}
    		tabMain.removeTabAt(index);
    	}
    }
    
    private void initMenu() {

        menuBar = new JMenuBar();

        /* File Menu */
        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text")); // NOI18N

        JMenuItem menuLoad = new JMenuItem(
                resourceMap.getString("menuLoad.text")); // NOI18N
        menuLoad.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoadXmlActionPerformed(evt);
            }
        });
        menuFile.add(menuLoad);

        JMenuItem menuSave = new JMenuItem(
                resourceMap.getString("menuSave.text")); // NOI18N
        menuSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveXmlActionPerformed(evt);
            }
        });
        menuFile.add(menuSave);

        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text")); // NOI18N
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text")); // NOI18N

        /*
         * TODO: Implement these as "Export All" versions
         *
         * miExportPerson.setText(resourceMap.getString("miExportPerson.text"));
         * // NOI18N miExportPerson.addActionListener(new ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent evt) {
         * miExportPersonActionPerformed(evt); } });
         * menuExport.add(miExportPerson);
         *
         * miExportParts.setText(resourceMap.getString("miExportParts.text"));
         * // NOI18N miExportParts.addActionListener(new ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent evt) {
         * miExportPartsActionPerformed(evt); } });
         * menuExport.add(miExportParts);
         */

        JMenuItem miExportOptions = new JMenuItem(
                resourceMap.getString("miExportOptions.text")); // NOI18N
        miExportOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miExportOptionsActionPerformed(evt);
            }
        });
        menuExport.add(miExportOptions);

        JMenuItem miExportPersonCSV = new JMenuItem(
                resourceMap.getString("miExportPersonCSV.text")); // NOI18N
        miExportPersonCSV.addActionListener(ev -> {
        		if (getTab(GuiTabType.PERSONNEL) != null) {
        			exportTable(((PersonnelTab)getTab(GuiTabType.PERSONNEL)).getPersonnelTable(),
        					getCampaign().getName()
                        + getCampaign().getShortDateAsString()
                        + "_ExportedPersonnel" + ".csv");
            }
        });
        menuExport.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(
                resourceMap.getString("miExportUnitCSV.text")); // NOI18N
        miExportUnitCSV.addActionListener(ev -> {
        	if (getTab(GuiTabType.HANGAR) != null) {
                exportTable(((HangarTab)getTab(GuiTabType.HANGAR)).getUnitTable(),
                		getCampaign().getName()
                        + getCampaign().getShortDateAsString()
                        + "_ExportedUnit" + ".csv");
            }
        });
        menuExport.add(miExportUnitCSV);

        JMenuItem miImportOptions = new JMenuItem(
                resourceMap.getString("miImportOptions.text")); // NOI18N
        miImportOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportOptionsActionPerformed(evt);
            }
        });
        menuImport.add(miImportOptions);

        JMenuItem miImportPerson = new JMenuItem(
                resourceMap.getString("miImportPerson.text")); // NOI18N
        miImportPerson.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportPersonActionPerformed(evt);
            }
        });
        menuImport.add(miImportPerson);

        JMenuItem miImportParts = new JMenuItem(
                resourceMap.getString("miImportParts.text")); // NOI18N
        miImportParts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportPartsActionPerformed(evt);
            }
        });
        menuImport.add(miImportParts);

        JMenuItem miLoadForces = new JMenuItem(
                resourceMap.getString("miLoadForces.text")); // NOI18N
        miLoadForces.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miLoadForcesActionPerformed(evt);
            }
        });
        // miLoadForces.setEnabled(false);
        menuImport.add(miLoadForces);

        menuFile.add(menuImport);
        menuFile.add(menuExport);

        JMenuItem miMercRoster = new JMenuItem(
                resourceMap.getString("miMercRoster.text")); // NOI18N
        miMercRoster.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showMercRosterDialog();
            }
        });
        menuFile.add(miMercRoster);

        JMenuItem menuOptions = new JMenuItem(
                resourceMap.getString("menuOptions.text")); // NOI18N
        menuOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOptionsActionPerformed(evt);
            }
        });
        menuFile.add(menuOptions);

        JMenuItem menuOptionsMM = new JMenuItem(
                resourceMap.getString("menuOptionsMM.text")); // NOI18N
        menuOptionsMM.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOptionsMMActionPerformed(evt);
            }
        });

        menuFile.add(menuOptionsMM);

        menuThemes = new JMenu("Themes");
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem("Exit");
        menuExitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getApplication().exit();
            }
        });
        menuFile.add(menuExitItem);

        menuBar.add(menuFile);

        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text")); // NOI18N

        // Personnel Market
        JMenuItem miPersonnelMarket = new JMenuItem("Personnel Market");
        miPersonnelMarket.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hirePersonMarket();
            }
        });
        menuMarket.add(miPersonnelMarket);

        // Contract Market
        miContractMarket = new JMenuItem("Contract Market...");
        miContractMarket.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showContractMarket();
            }
        });
        menuMarket.add(miContractMarket);
        miContractMarket.setVisible(getCampaign().getCampaignOptions()
                .getUseAtB());

        miUnitMarket = new JMenuItem("Unit Market...");
        miUnitMarket.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showUnitMarket();
            }
        });
        menuMarket.add(miUnitMarket);
        miUnitMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());

        miShipSearch = new JMenuItem("Ship Search...");
        miShipSearch.addActionListener(ev -> showShipSearch());
        menuMarket.add(miShipSearch);
        miShipSearch.setVisible(getCampaign().getCampaignOptions().getUseAtB());

        JMenuItem miPurchaseUnit = new JMenuItem(
                resourceMap.getString("miPurchaseUnit.text")); // NOI18N
        miPurchaseUnit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPurchaseUnitActionPerformed(evt);
            }
        });
        menuMarket.add(miPurchaseUnit);

        JMenuItem miBuyParts = new JMenuItem(
                resourceMap.getString("miBuyParts.text")); // NOI18N
        miBuyParts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyParts();
            }
        });
        menuMarket.add(miBuyParts);
        JMenuItem miHireBulk = new JMenuItem("Hire Personnel in Bulk");
        miHireBulk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hireBulkPersonnel();
            }
        });
        menuMarket.add(miHireBulk);

        JMenu menuHire = new JMenu(resourceMap.getString("menuHire.text")); // NOI18N

        JMenuItem miHire;
        for (int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
            miHire = new JMenuItem(Person.getRoleDesc(i, getCampaign()
                    .getFaction().isClan()));
            miHire.setActionCommand(Integer.toString(i));
            miHire.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    hirePerson(evt);
                }
            });
            menuHire.add(miHire);
        }
        menuMarket.add(menuHire);

        JMenu menuAstechPool = new JMenu("Astech Pool");

        JMenuItem miHireAstechs = new JMenuItem("Hire Astechs");
        miHireAstechs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Hire How Many Astechs?", 1, 0, 100);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().increaseAstechPool(pvcd.getValue());
            }
        });
        menuAstechPool.add(miHireAstechs);

        JMenuItem miFireAstechs = new JMenuItem("Release Astechs");
        miFireAstechs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Release How Many Astechs?", 1, 0,
                        getCampaign().getAstechPool());
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().decreaseAstechPool(pvcd.getValue());
            }
        });
        menuAstechPool.add(miFireAstechs);

        JMenuItem miFullStrengthAstechs = new JMenuItem(
                "Bring All Tech Teams to Full Strength");
        miFullStrengthAstechs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int need = (getCampaign().getTechs().size() * 6)
                        - getCampaign().getNumberAstechs();
                if (need > 0) {
                    getCampaign().increaseAstechPool(need);
                }
            }
        });
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = new JMenuItem(
                "Release All Astechs from Pool");
        miFireAllAstechs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getCampaign().decreaseAstechPool(getCampaign().getAstechPool());
            }
        });
        menuAstechPool.add(miFireAllAstechs);
        menuMarket.add(menuAstechPool);

        JMenu menuMedicPool = new JMenu("Medic Pool");
        JMenuItem miHireMedics = new JMenuItem("Hire Medics");
        miHireMedics.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Hire How Many Medics?", 1, 0, 100);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().increaseMedicPool(pvcd.getValue());
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = new JMenuItem("Release Medics");
        miFireMedics.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Release How Many Medics?", 1, 0,
                        getCampaign().getMedicPool());
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().decreaseMedicPool(pvcd.getValue());
            }
        });
        menuMedicPool.add(miFireMedics);
        JMenuItem miFullStrengthMedics = new JMenuItem(
                "Bring All Medical Teams to Full Strength");
        miFullStrengthMedics.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int need = (getCampaign().getDoctors().size() * 4)
                        - getCampaign().getNumberMedics();
                if (need > 0) {
                    getCampaign().increaseMedicPool(need);
                }
            }
        });
        menuMedicPool.add(miFullStrengthMedics);
        JMenuItem miFireAllMedics = new JMenuItem(
                "Release All Medics from Pool");
        miFireAllMedics.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getCampaign().decreaseMedicPool(getCampaign().getMedicPool());
            }
        });
        menuMedicPool.add(miFireAllMedics);
        menuMarket.add(menuMedicPool);
        menuBar.add(menuMarket);

        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text")); // NOI18N

        JMenuItem miDragoonsRating = new JMenuItem(
                resourceMap.getString("miDragoonsRating.text")); // NOI18N
        miDragoonsRating.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new RatingReport(getCampaign()));
            }
        });
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(
                resourceMap.getString("miPersonnelReport.text")); // NOI18N
        miPersonnelReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new PersonnelReport(getCampaign()));
            }
        });
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(
                resourceMap.getString("miHangarBreakdown.text")); // NOI18N
        miHangarBreakdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new HangarReport(getCampaign()));
            }
        });
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(
                resourceMap.getString("miTransportReport.text")); // NOI18N
        miTransportReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new TransportReport(getCampaign()));
            }
        });
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(
                resourceMap.getString("miCargoReport.text")); // NOI18N
        miCargoReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new CargoReport(getCampaign()));
            }
        });
        menuReports.add(miCargoReport);

        menuBar.add(menuReports);

        JMenu menuCommunity = new JMenu(
                resourceMap.getString("menuCommunity.text")); // NOI18N

        JMenuItem miChat = new JMenuItem(resourceMap.getString("miChat.text")); // NOI18N
        miChat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miChatActionPerformed(evt);
            }
        });
        menuCommunity.add(miChat);

        // menuBar.add(menuCommunity);

        JMenu menuView = new JMenu("View"); // NOI18N
        miDetachLog = new JMenuItem("Detach Daily Report Log"); // NOI18N
        miDetachLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDailyReportDialog();
            }
        });
        menuView.add(miDetachLog);

        miAttachLog = new JMenuItem("Attach Daily Report Log"); // NOI18N
        miAttachLog.setEnabled(false);
        miAttachLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideDailyReportDialog();
            }
        });
        menuView.add(miAttachLog);

        JMenuItem miBloodnameDialog = new JMenuItem("Show Bloodname Dialog...");
        miBloodnameDialog.setEnabled(true);
        miBloodnameDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showBloodnameDialog();
            }
        });
        menuView.add(miBloodnameDialog);

        miRetirementDefectionDialog = new JMenuItem(
                "Show Retirement/Defection Dialog...");
        miRetirementDefectionDialog.setEnabled(true);
        miRetirementDefectionDialog.setVisible(getCampaign()
                .getCampaignOptions().getUseAtB());
        miRetirementDefectionDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showRetirementDefectionDialog();
            }
        });
        menuView.add(miRetirementDefectionDialog);

        miShowOverview = new JCheckBoxMenuItem("Show Overview Tab");
        miShowOverview.setSelected(hasTab(GuiTabType.OVERVIEW));
        miShowOverview.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                toggleOverviewTab();
            }
        });
        menuView.add(miShowOverview);

        menuBar.add(menuView);

        JMenu menuManage = new JMenu("Manage Campaign");
        menuManage.setName("manageMenu");
        JMenuItem miGMToolsDialog = new JMenuItem("Show GM Tools Dialog");
        miGMToolsDialog.setEnabled(true);
        miGMToolsDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showGMToolsDialog();
            }
        });
        menuManage.add(miGMToolsDialog);
        JMenuItem miAdvanceMultipleDays = new JMenuItem("Advance Multiple Days");
        miAdvanceMultipleDays.setEnabled(true);
        miAdvanceMultipleDays.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showAdvanceDaysDialog();
            }
        });
        menuManage.add(miAdvanceMultipleDays);
        JMenuItem miBloodnames = new JMenuItem("Randomize Bloodnames All Personnel");
        miBloodnames.setEnabled(true);
        miBloodnames.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                randomizeAllBloodnames();
            }
        });
        menuManage.add(miBloodnames);
        JMenuItem miBatchXP = new JMenuItem("Mass training");
        miBatchXP.setEnabled(true);
        miBatchXP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                spendBatchXP();
            }
        });
        menuManage.add(miBatchXP);

        menuBar.add(menuManage);

        JMenu menuHelp = new JMenu(resourceMap.getString("helpMenu.text")); // NOI18N
        menuHelp.setName("helpMenu"); // NOI18N
        JMenuItem menuAboutItem = new JMenuItem("aboutMenuItem"); // NOI18N
        menuAboutItem.setText("About");
        menuAboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAboutBox();
            }
        });
        menuHelp.add(menuAboutItem);
        menuBar.add(menuHelp);
    }

    private void initMain() {

        panLog = new DailyReportLogPanel(reportHLL);
        panLog.setMinimumSize(new java.awt.Dimension(150, 100));
        logDialog = new DailyReportLogDialog(getFrame(), this, reportHLL);
        bloodnameDialog = new BloodnameDialog(getFrame());

        mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabMain, panLog);
        mainPanel.setOneTouchExpandable(true);
        mainPanel.setResizeWeight(1.0);
    }

    private void initStatusBar() {

        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));

        lblRating = new JLabel();
        lblFunds = new JLabel();
        lblTempAstechs = new JLabel();
        lblTempMedics = new JLabel();

        statusPanel.add(lblRating);
        statusPanel.add(lblFunds);
        statusPanel.add(lblTempAstechs);
        statusPanel.add(lblTempMedics);
    }

    private void initTopButtons() {
        GridBagConstraints gridBagConstraints;

        lblLocation = new JLabel(getCampaign().getLocation().getReport(
                getCampaign().getCalendar().getTime())); // NOI18N

        btnPanel = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 3);
        btnPanel.add(lblLocation, gridBagConstraints);

        btnGMMode = new JToggleButton(resourceMap.getString("btnGMMode.text")); // NOI18N
        btnGMMode
                .setToolTipText(resourceMap.getString("btnGMMode.toolTipText")); // NOI18N
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGMModeActionPerformed(evt);
            }
        });
        btnGMMode.setMinimumSize(new Dimension(150, 25));
        btnGMMode.setPreferredSize(new Dimension(150, 25));
        btnGMMode.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        btnPanel.add(btnGMMode, gridBagConstraints);

        btnOvertime = new JToggleButton(
                resourceMap.getString("btnOvertime.text")); // NOI18N
        btnOvertime.setToolTipText(resourceMap
                .getString("btnOvertime.toolTipText")); // NOI18N
        btnOvertime.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOvertimeActionPerformed(evt);
            }
        });
        btnOvertime.setMinimumSize(new Dimension(150, 25));
        btnOvertime.setPreferredSize(new Dimension(150, 25));
        btnOvertime.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        btnPanel.add(btnOvertime, gridBagConstraints);

        btnAdvanceDay = new JButton(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
        btnAdvanceDay.setToolTipText(resourceMap
                .getString("btnAdvanceDay.toolTipText")); // NOI18N
        btnAdvanceDay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                advanceDay();
            }
        });
        btnAdvanceDay.setPreferredSize(new Dimension(250, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 15);
        btnPanel.add(btnAdvanceDay, gridBagConstraints);
    }

    private static void enableFullScreenMode(Window window) {
        String className = "com.apple.eawt.FullScreenUtilities";
        String methodName = "setWindowCanFullScreen";

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, new Class<?>[] {
                    Window.class, boolean.class });
            method.invoke(null, window, true);
        } catch (Throwable t) {
            System.err.println("Full screen mode is not supported");
            t.printStackTrace();
        }
    }

    private static boolean isMacOSX() {
        return System.getProperty("os.name").indexOf("Mac OS X") >= 0;
    }

    private void miChatActionPerformed(ActionEvent evt) {
        JDialog chatDialog = new JDialog(getFrame(), "MekHQ Chat", false); //$NON-NLS-1$

        ChatClient client = new ChatClient("test", "localhost");
        client.listen();
        // chatDialog.add(client);
        chatDialog.add(new JLabel("Testing"));
        chatDialog.setResizable(true);
        chatDialog.setVisible(true);
    }

    private void changeTheme(java.awt.event.ActionEvent evt) {
        final String lafClassName = evt.getActionCommand();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(lafClassName);
                    SwingUtilities.updateComponentTreeUI(frame);
                    refreshThemeChoices();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(frame,
                            "Can't change look and feel", "Invalid PLAF",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void refreshThemeChoices() {
        menuThemes.removeAll();
        JCheckBoxMenuItem miPlaf;
        for (LookAndFeelInfo plaf : UIManager.getInstalledLookAndFeels()) {
            miPlaf = new JCheckBoxMenuItem(plaf.getName());
            if (plaf.getName().equalsIgnoreCase(
                    UIManager.getLookAndFeel().getName())) {
                miPlaf.setSelected(true);
            }
            menuThemes.add(miPlaf);
            miPlaf.setActionCommand(plaf.getClassName());
            miPlaf.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeTheme(evt);
                }
            });
        }
    }
    //TODO: trigger from event
    public void filterTasks() {
    	if (getTab(GuiTabType.REPAIR) != null) {
    		((RepairTab)getTab(GuiTabType.REPAIR)).filterTasks();
    	}
    }
        
    public void focusOnUnit(UUID id) {
    	HangarTab ht = (HangarTab)getTab(GuiTabType.HANGAR);
        if (null == id || null == ht) {
            return;
        }
        if (mainPanel.getDividerLocation() < 700) {
            if (mainPanel.getLastDividerLocation() > 700) {
                mainPanel
                        .setDividerLocation(mainPanel.getLastDividerLocation());
            } else {
                mainPanel.resetToPreferredSizes();
            }
        }
        ht.focusOnUnit(id);
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap
                .getString("panHangar.TabConstraints.tabTitle")));
    }

    public void focusOnUnitInRepairBay(UUID id) {
        if (null == id) {
            return;
        }
        if (getTab(GuiTabType.REPAIR) != null) {
            if (mainPanel.getDividerLocation() < 700) {
                if (mainPanel.getLastDividerLocation() > 700) {
                    mainPanel
                            .setDividerLocation(mainPanel.getLastDividerLocation());
                } else {
                    mainPanel.resetToPreferredSizes();
                }
            }
            ((RepairTab)getTab(GuiTabType.REPAIR)).focusOnUnit(id);
	        tabMain.setSelectedComponent(getTab(GuiTabType.REPAIR));
        }
    }

    public void focusOnPerson(UUID id) {
        if (null == id) {
            return;
        }
        PersonnelTab pt = (PersonnelTab)getTab(GuiTabType.PERSONNEL);
        if (pt == null) {
        	return;
        }
        if (mainPanel.getDividerLocation() < 700) {
            if (mainPanel.getLastDividerLocation() > 700) {
                mainPanel
                        .setDividerLocation(mainPanel.getLastDividerLocation());
            } else {
                mainPanel.resetToPreferredSizes();
            }
        }
        pt.focusOnPerson(id);
        tabMain.setSelectedComponent(pt);
    }

    public void showNews(int id) {
        NewsItem news = getCampaign().getNews().getNewsItem(id);
        if (null != news) {
            NewsReportDialog nrd = new NewsReportDialog(frame, news);
            nrd.setVisible(true);
        }
    }

    private void advanceDay() {
        // first check for overdue loan payments - dont allow advancement until
        // these are addressed
        if (getCampaign().checkOverDueLoans()) {
            refreshFunds();
            refreshReport();
            return;
        }
        if (getCampaign().checkRetirementDefections()) {
            showRetirementDefectionDialog();
            return;
        }
        if (getCampaign().checkYearlyRetirements()) {
            showRetirementDefectionDialog();
            return;
        }
        if (nagShortMaintenance()) {
            return;
        }
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            if (nagShortDeployments()) {
                return;
            }
            if (nagOutstandingScenarios()) {
                return;
            }
        }
        if(!getCampaign().newDay()) {
            return;
        }
        
        refreshCalendar();
        refreshLocation();
        initReport();
        refreshFunds();
        
        refreshAllTabs();
    }// GEN-LAST:event_btnAdvanceDayActionPerformed

    public boolean nagShortMaintenance() {
        if (!getCampaign().getCampaignOptions().checkMaintenance()) {
            return false;
        }
        Vector<Unit> notMaintained = new Vector<Unit>();
        int totalAstechMinutesNeeded = 0;
        for (Unit u : getCampaign().getUnits()) {
            if (u.requiresMaintenance() && null == u.getTech()) {
                notMaintained.add(u);
            } else {
                // only add astech minutes for non-crewed units
                if (null == u.getEngineer()) {
                    totalAstechMinutesNeeded += (u.getMaintenanceTime() * 6);
                }
            }
        }

        if (notMaintained.size() > 0) {
            if (JOptionPane.YES_OPTION != JOptionPane
                    .showConfirmDialog(
                            null,
                            "You have unmaintained units. Do you really wish to advance the day?",
                            "Unmaintained Units", JOptionPane.YES_NO_OPTION)) {
                return true;
            }
        }

        int minutesAvail = getCampaign().getPossibleAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            minutesAvail += getCampaign().getPossibleAstechPoolOvertime();
        }
        if (minutesAvail < totalAstechMinutesNeeded) {
            int needed = (int) Math
                    .ceil((totalAstechMinutesNeeded - minutesAvail) / 480D);
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
                    "You do not have enough astechs to provide for full maintenance. You need "
                            + needed
                            + " more astech(s). Do you wish to proceed?",
                    "Astech shortage", JOptionPane.YES_NO_OPTION)) {
                return true;
            }
        }

        return false;
    }

    public boolean nagShortDeployments() {
        if (getCampaign().getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            return false;
        }
        for (Mission m : getCampaign().getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract)
                    || !getCampaign().getLocation().isOnPlanet()) {
                continue;
            }
            if (getCampaign().getDeploymentDeficit((AtBContract) m) > 0) {
                return 0 != JOptionPane
                        .showConfirmDialog(
                                null,
                                "You have not met the deployment levels required by contract. Do your really wish to advance the day?",
                                "Unmet deployment requirements",
                                JOptionPane.YES_NO_OPTION);
            }
        }
        return false;
    }

    public boolean nagOutstandingScenarios() {
        for (Mission m : getCampaign().getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract)) {
                continue;
            }
            for (Scenario s : m.getScenarios()) {
                if (!s.isCurrent() || !(s instanceof AtBScenario)) {
                    continue;
                }
                if (getCampaign().getDate().equals(s.getDate())) {
                    return 0 != JOptionPane
                            .showConfirmDialog(
                                    null,
                                    "You have a pending battle. Failure to deploy will result in a defeat and a minor contract breach. Do your really wish to advance the day?",
                                    "Pending battle", JOptionPane.YES_NO_OPTION);
                }
            }
        }
        return false;
    }

    private void hirePerson(java.awt.event.ActionEvent evt) {
        int type = Integer.parseInt(evt.getActionCommand());
        NewRecruitDialog npd = new NewRecruitDialog(this, true,
                getCampaign().newPerson(type));
        npd.setVisible(true);
    }

    public void hirePersonMarket() {
        PersonnelMarketDialog pmd = new PersonnelMarketDialog(getFrame(), this,
                getCampaign(), getIconPackage().getPortraits());
        pmd.setVisible(true);
    }

    private void hireBulkPersonnel() {
        HireBulkPersonnelDialog hbpd = new HireBulkPersonnelDialog(getFrame(),
                true, getCampaign());
        hbpd.setVisible(true);
    }

    public void showContractMarket() {
        ContractMarketDialog cmd = new ContractMarketDialog(getFrame(), getCampaign());
        cmd.setVisible(true);
    }

    public void showUnitMarket() {
        UnitMarketDialog umd = new UnitMarketDialog(getFrame(), getCampaign());
        umd.setVisible(true);
    }

    public void showShipSearch() {
        ShipSearchDialog ssd = new ShipSearchDialog(getFrame(), this);
        ssd.setVisible(true);
    }

    private void menuSaveXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuSaveActionPerformed
        final String METHOD_NAME = "menuSaveXmlActionPerformed(ActionEvent)";
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Saving campaign..."); //$NON-NLS-1$
        // Choose a file...
        File file = selectSaveCampaignFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".cpnx")) {
            path += ".cpnx";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));
            getCampaign().writeToXml(pw);
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Campaign saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly save your game. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.", "Could not save game",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    private File selectSaveCampaignFile() {
        JFileChooser saveCpgn = new JFileChooser("./campaigns/");
        saveCpgn.setDialogTitle("Save Campaign");
        saveCpgn.setFileFilter(new CampaignFileFilter());
        saveCpgn.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString() + ".cpnx")); //$NON-NLS-1$
        int returnVal = saveCpgn.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveCpgn.getSelectedFile() == null)) {
            // I want a file, y'know!
            return null;
        }

        File file = saveCpgn.getSelectedFile();

        return file;
    }

    private void menuLoadXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuLoadActionPerformed
        File f = selectLoadCampaignFile();
        if (null == f) {
            return;
        }
        boolean hadAtB = getCampaign().getCampaignOptions().getUseAtB();
        DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(
                getApplication(), getFrame(), f);
        // TODO: does this effectively deal with memory management issues?
        dataLoadingDialog.setVisible(true);
        if (hadAtB && !getCampaign().getCampaignOptions().getUseAtB()) {
            RandomFactionGenerator.getInstance().dispose();
            RandomUnitGenerator.getInstance().dispose();
            RandomNameGenerator.getInstance().dispose();
        }
        //Unregister event handlers for CampaignGUI and tabs
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            if (tabMain.getComponentAt(i) instanceof CampaignGuiTab) {
                ((CampaignGuiTab)tabMain.getComponentAt(i)).disposeTab();
            }
        }
        MekHQ.unregisterHandler(this);
    }

    private File selectLoadCampaignFile() {
        JFileChooser loadCpgn = new JFileChooser("./campaigns/");
        loadCpgn.setDialogTitle("Load Campaign");
        loadCpgn.setFileFilter(new CampaignFileFilter());
        int returnVal = loadCpgn.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadCpgn.getSelectedFile() == null)) {
            // I want a file, y'know!
            return null;
        }

        File file = loadCpgn.getSelectedFile();

        return file;
    }

    private void btnOvertimeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOvertimeActionPerformed
        getCampaign().setOvertime(btnOvertime.isSelected());
    }// GEN-LAST:event_btnOvertimeActionPerformed

    private void btnGMModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGMModeActionPerformed
        getCampaign().setGMMode(btnGMMode.isSelected());
    }// GEN-LAST:event_btnGMModeActionPerformed

    private void menuOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuOptionsActionPerformed
        boolean atb = getCampaign().getCampaignOptions().getUseAtB();
        boolean staticRATs = getCampaign().getCampaignOptions().useStaticRATs();
        boolean factionIntroDate = getCampaign().getCampaignOptions().useFactionIntroDate();
        CampaignOptionsDialog cod = new CampaignOptionsDialog(getFrame(), true,
                getCampaign(), getIconPackage().getCamos());
        cod.setVisible(true);
        if (atb != getCampaign().getCampaignOptions().getUseAtB()) {
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                getCampaign().initAtB();
                //refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign().getForces()));
            }
            miContractMarket.setVisible(getCampaign().getCampaignOptions()
                    .getUseAtB());
            miUnitMarket.setVisible(getCampaign().getCampaignOptions()
                    .getUseAtB());
            miShipSearch.setVisible(getCampaign().getCampaignOptions()
                    .getUseAtB());
            miRetirementDefectionDialog.setVisible(getCampaign()
                    .getCampaignOptions().getUseAtB());
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                while (!RandomFactionGenerator.getInstance().isInitialized()) {
                    //Sleep for up to one second.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignore) {

                    }
                }
                while (!RandomUnitGenerator.getInstance().isInitialized()) {
                    //Sleep for up to one second.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignore) {

                    }
                }
                RandomNameGenerator.getInstance();
                RandomFactionGenerator.getInstance().updateTables(getCampaign().getDate(),
                        getCampaign().getCurrentPlanet(), getCampaign().getCampaignOptions());
            } else {
                RandomFactionGenerator.getInstance().dispose();
                RandomUnitGenerator.getInstance().dispose();
                RandomNameGenerator.getInstance().dispose();
            }
        }
        if (staticRATs != getCampaign().getCampaignOptions().useStaticRATs()) {
        	getCampaign().initUnitGenerator();
        }
        if (factionIntroDate != getCampaign().getCampaignOptions().useFactionIntroDate()) {
            getCampaign().updateTechFactionCode();
        }
        refreshCalendar();
        getCampaign().reloadNews();
    }// GEN-LAST:event_menuOptionsActionPerformed

    private void menuOptionsMMActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuOptionsActionPerformed
        GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
        god.refreshOptions();
        god.setEditable(true);
        god.setVisible(true);
        if (!god.wasCancelled()) {
            getCampaign().setGameOptions(god.getOptions());
            setCampaignOptionsFromGameOptions();
            refreshCalendar();
        }
    }// GEN-LAST:event_menuOptionsActionPerformed

    private void miLoadForcesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miLoadForcesActionPerformed
        try {
            loadListFile(true);
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miLoadForcesActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miLoadForcesActionPerformed

    private void miImportPersonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miImportPersonActionPerformed
        try {
            loadPersonFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miImportPersonActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miImportPersonActionPerformed

    public void miExportPersonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            savePersonFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miExportPersonActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miExportOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            saveOptionsFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miExportOptionsActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miImportOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            loadOptionsFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miImportOptionsActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miImportPartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miImportPersonActionPerformed
        try {
            loadPartsFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miImportPartsActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miImportPersonActionPerformed

    public void miExportPartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            savePartsFile();
        } catch (IOException ex) {
            MekHQ.getLogger().log(getClass(), "miExportPartsActionPerformed(ActionEvent)", ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miPurchaseUnitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miPurchaseUnitActionPerformed
        UnitSelectorDialog usd = new UnitSelectorDialog(getFrame(),
                getCampaign(), true);

        usd.setVisible(true);
    }// GEN-LAST:event_miPurchaseUnitActionPerformed

    private void buyParts() {
        PartsStoreDialog psd = new PartsStoreDialog(true, this);
        psd.setVisible(true);
    }

    private void showMercRosterDialog() {
        MercRosterDialog mrd = new MercRosterDialog(getFrame(), true,
                getCampaign());
        mrd.setVisible(true);
    }

    public void refitUnit(Refit r, boolean selectModelName) {
        if (r.getOriginalEntity() instanceof Dropship
                || r.getOriginalEntity() instanceof Jumpship) {
            Person engineer = r.getOriginalUnit().getEngineer();
            if (engineer == null) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                "You cannot refit a ship that does not have an engineer. Assign a qualified vessel crew to this unit.",
                                "No Engineer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            r.setTeamId(engineer.getId());
        } else if (getCampaign().getTechs().size() > 0) {
            String name;
            HashMap<String, Person> techHash = new HashMap<String, Person>();
            String skillLvl = "Unknown";
            int TimePerDay = 0;
            for (Person tech : getCampaign().getTechs()) {
                if (getCampaign().isWorkingOnRefit(tech) || tech.isEngineer()) {
                    continue;
                }
                if (tech.getSecondaryRole() == Person.T_MECH_TECH || tech.getSecondaryRole() == Person.T_MECHANIC || tech.getSecondaryRole() == Person.T_AERO_TECH) {
                    TimePerDay = 240 - tech.getMaintenanceTimeUsing();
                } else {
                    TimePerDay = 480 - tech.getMaintenanceTimeUsing();
                }
                skillLvl = SkillType.getExperienceLevelName(tech.getExperienceLevel(false));
                name = tech.getFullName()
                        + ", "
                        + skillLvl
                        + " "
                        + tech.getPrimaryRoleDesc()
                        + " ("
                        + getCampaign().getTargetFor(r, tech).getValueAsString()
                        + "+)"
                        + ", "
                        + tech.getMinutesLeft() + "/" + TimePerDay
                        + " minutes";
                techHash.put(name, tech);
            }
            String[] techNames = new String[techHash.keySet().size()];
            int i = 0;
            for (String n : techHash.keySet()) {
                techNames[i] = n;
                i++;
            }
            String s = (String) JOptionPane.showInputDialog(frame,
                    "Which tech should work on the refit?", "Select Tech",
                    JOptionPane.PLAIN_MESSAGE, null, techNames, techNames[0]);
            if (null == s) {
                return;
            }
            r.setTeamId(techHash.get(s).getId());
        } else {
            JOptionPane.showMessageDialog(frame,
                    "You have no techs available to work on this refit.",
                    "No Techs", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectModelName) {
            // select a model name
            RefitNameDialog rnd = new RefitNameDialog(frame, true, r);
            rnd.setVisible(true);
            if (rnd.wasCancelled()) {
                // Set the tech team to null since we may want to change it when we re-do the refit
                r.setTeamId(null);
                return;
            }
        }
        // TODO: allow overtime work?
        // check to see if user really wants to do it - give some info on what
        // will be done
        // TODO: better information
        String RefitRefurbish;
        if (r.isBeingRefurbished()) {
            RefitRefurbish = "Refurbishment is a " + r.getRefitClassName() + " refit and must be done at a factory and costs 10% of the purchase price"
                             + ".\n Are you sure you want to refurbish "; 
        } else {
            RefitRefurbish = "This is a " + r.getRefitClassName() + " refit. Are you sure you want to refit ";
        }
        if (0 != JOptionPane
                .showConfirmDialog(null, RefitRefurbish
                        + r.getUnit().getName() + "?", "Proceed?",
                        JOptionPane.YES_NO_OPTION)) {
            return;
        }
        try {
            r.begin();
        } catch (EntityLoadingException ex) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            "For some reason, the unit you are trying to customize cannot be loaded\n and so the customization was cancelled. Please report the bug with a description\nof the unit being customized.",
                            "Could not customize unit",
                            JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "IO Exception",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        getCampaign().refit(r);
        if (hasTab(GuiTabType.MEKLAB)) {
        	((MekLabTab)getTab(GuiTabType.MEKLAB)).clearUnit();
        }
    }

    private void showReport(Report report) {
        ReportDialog rd = new ReportDialog(getFrame(), report);
        rd.setVisible(true);
    }

    public void showMaintenanceReport(UUID id) {
        if (null == id) {
            return;
        }
        Unit u = getCampaign().getUnit(id);
        if (null == u) {
            return;
        }
        MaintenanceReportDialog mrd = new MaintenanceReportDialog(getFrame(), u);
        mrd.setVisible(true);
    }

    public UUID selectTech(Unit u, String desc) {
        String name;
        HashMap<String, Person> techHash = new HashMap<String, Person>();
        for (Person tech : getCampaign().getTechs()) {
            if (tech.canTech(u.getEntity()) && !tech.isMothballing()) {
                name = tech.getFullName()
                        + ", "
                        + SkillType.getExperienceLevelName(tech
                                .getSkillForWorkingOn(u).getExperienceLevel())
                        + " (" + Math.max(0, (tech.getMinutesLeft() - tech.getMaintenanceTimeUsing()))
                        + "min)";
                techHash.put(name, tech);
            }
        }
        if (techHash.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "You have no techs available.", "No Techs",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String[] techNames = new String[techHash.keySet().size()];
        int i = 0;
        for (String n : techHash.keySet()) {
            techNames[i] = n;
            i++;
        }
        String s = (String) JOptionPane.showInputDialog(frame,
                "Which tech should work on " + desc + "?", "Select Tech",
                JOptionPane.PLAIN_MESSAGE, null, techNames, techNames[0]);
        if (null == s) {
            return null;
        }
        return techHash.get(s).getId();
    }

    public Part getPartByNameAndDetails(String pnd) {
        return getCampaign().getPartsStore().getByNameAndDetails(pnd);
    }

    protected void loadListFile(boolean allowNewPilots) throws IOException {
        final String METHOD_NAME = "loadListFile(boolean)";
        
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Units");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".mul");
            }

            @Override
            public String getDescription() {
                return "MUL file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File unitFile = loadList.getSelectedFile();

        if (unitFile != null) {
            // I need to get the parser myself, because I want to pull both
            // entities and pilots from it
            // Create an empty parser.
            MULParser parser = new MULParser();

            // Open up the file.
            InputStream listStream = new FileInputStream(unitFile);

            // Read a Vector from the file.
            try {
                parser.parse(listStream);
                listStream.close();
            } catch (Exception excep) {
                excep.printStackTrace(System.err);
                // throw new IOException("Unable to read from: " +
                // unitFile.getName());
            }

            // Was there any error in parsing?
            if (parser.hasWarningMessage()) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
                        parser.getWarningMessage());
            }

            // Add the units from the file.
            for (Entity entity : parser.getEntities()) {
                getCampaign().addUnit(entity, allowNewPilots, 0);
            }

            // add any ejected pilots
            for (Crew pilot : parser.getPilots()) {
                if (pilot.isEjected()) {
                    // getCampaign().addPilot(pilot, PilotPerson.T_MECHWARRIOR,
                    // false);
                }
            }
        }
    }

    protected void loadPersonFile() throws IOException {
        final String METHOD_NAME = "loadPersonFile()";
        
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Personnel");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".prsx");
            }

            @Override
            public String getDescription() {
                return "Personnel file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File personnelFile = loadList.getSelectedFile();

        if (personnelFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(personnelFile);

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Starting load of personnel file from XML..."); //$NON-NLS-1$
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            }

            Element personnelEle = xmlDoc.getDocumentElement();
            NodeList nl = personnelEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            personnelEle.normalize();

            Version version = new Version(personnelEle.getAttribute("version"));

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                            "Unknown node type not loaded in Personnel nodes: " //$NON-NLS-1$
                            + wn2.getNodeName());

                    continue;
                }

                Person p = Person.generateInstanceFromXML(wn2, getCampaign(),
                        version);
                if (getCampaign().getPerson(p.getId()) != null
                        && getCampaign().getPerson(p.getId()).getFullName()
                                .equals(p.getFullName())) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                            "ERROR: Cannot load person who exists, ignoring. (Name: " //$NON-NLS-1$
                            + p.getFullName() + ")"); //$NON-NLS-1$
                    p = null;
                }
                if (p != null) {
                    getCampaign().addPersonWithoutId(p, true);
                }

                // Clear some values we no longer should have set in case this
                // has transferred campaigns or things in the campaign have
                // changed...
                p.setUnitId(null);
                p.clearTechUnitIDs();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Finished load of personnel file"); //$NON-NLS-1$
        }
    }

    //TODO: disable if not using personnel tab
    private void savePersonFile() throws IOException {
        final String METHOD_NAME = "savePersonFile()";
        
        JFileChooser savePersonnel = new JFileChooser(".");
        savePersonnel.setDialogTitle("Save Personnel");
        savePersonnel.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".prsx");
            }

            @Override
            public String getDescription() {
                return "Personnel file";
            }
        });
        savePersonnel.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString()
                + "_ExportedPersonnel" + ".prsx")); //$NON-NLS-1$
        int returnVal = savePersonnel.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (savePersonnel.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = savePersonnel.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".prsx")) {
            path += ".prsx";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
        	PersonnelTab pt = (PersonnelTab)getTab(GuiTabType.PERSONNEL);
            int row = pt.getPersonnelTable().getSelectedRow();
            if (row < 0) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
                        "ERROR: Cannot export person if no one is selected! Ignoring."); //$NON-NLS-1$
                return;
            }
            Person selectedPerson = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                    .convertRowIndexToModel(row));
            int[] rows = pt.getPersonnelTable().getSelectedRows();
            Person[] people = new Person[rows.length];
            for (int i = 0; i < rows.length; i++) {
                people[i] = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                        .convertRowIndexToModel(rows[i]));
            }
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            ResourceBundle resourceMap = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // Start the XML root.
            pw.println("<personnel version=\""
                    + resourceMap.getString("Application.version") + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    people[i].writeToXml(pw, 1);
                }
            } else {
                selectedPerson.writeToXml(pw, 1);
            }
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.println("</personnel>");
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Personnel saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your personnel. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.",
                            "Could not export personnel",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    private void saveOptionsFile() throws IOException {
        final String METHOD_NAME = "saveOptionsFile()";
        
        JFileChooser saveOptions = new JFileChooser(".");
        saveOptions.setDialogTitle("Save Campaign Options");
        saveOptions.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "Campaign options file";
            }
        });
        saveOptions.setSelectedFile(new File("campaignOptions.xml")); //$NON-NLS-1$
        int returnVal = saveOptions.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveOptions.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = saveOptions.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            ResourceBundle resourceMap = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<options version=\""
                    + resourceMap.getString("Application.version") + "\">");
            // Start the XML root.
            getCampaign().getCampaignOptions().writeToXml(pw, 1);
            pw.println("\t<skillTypes>");
            for (String name : SkillType.skillList) {
                SkillType type = SkillType.getType(name);
                if (null != type) {
                    type.writeToXml(pw, 2);
                }
            }
            pw.println("\t</skillTypes>");
            pw.println("\t<specialAbilities>");
            for (String key : SpecialAbility.getAllSpecialAbilities().keySet()) {
                SpecialAbility.getAbility(key).writeToXml(pw, 2);
            }
            pw.println("\t</specialAbilities>");
            getCampaign().getRandomSkillPreferences().writeToXml(pw, 1);
            pw.println("</options>");
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Campaign Options saved saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your campaign options. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.",
                            "Could not export campaign options",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    protected void loadPartsFile() throws IOException {
        final String METHOD_NAME = "loadPartsFile()";
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Parts");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".parts");
            }

            @Override
            public String getDescription() {
                return "Parts file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File partsFile = loadList.getSelectedFile();

        if (partsFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(partsFile);

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Starting load of parts file from XML..."); //$NON-NLS-1$
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            }

            Element partsEle = xmlDoc.getDocumentElement();
            NodeList nl = partsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            partsEle.normalize();

            Version version = new Version(partsEle.getAttribute("version"));

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                            "Unknown node type not loaded in Parts nodes: " //$NON-NLS-1$
                            + wn2.getNodeName());

                    continue;
                }

                Part p = Part.generateInstanceFromXML(wn2, version);
                if (p != null) {
                    p.setCampaign(getCampaign());
                    getCampaign().addPartWithoutId(p);
                }
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Finished load of parts file"); //$NON-NLS-1$
        }
    }

    protected void loadOptionsFile() throws IOException {
        final String METHOD_NAME = "loadOptionsFile()";
        
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Campaign Options");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "Campaign options file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File optionsFile = loadList.getSelectedFile();

        if (optionsFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(optionsFile);

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Starting load of options file from XML..."); //$NON-NLS-1$
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            }

            Element partsEle = xmlDoc.getDocumentElement();
            NodeList nl = partsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            partsEle.normalize();

            Version version = new Version(partsEle.getAttribute("version"));

            CampaignOptions options = null;
            RandomSkillPreferences rsp = null;

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("campaignOptions")) {
                    options = CampaignOptions
                            .generateCampaignOptionsFromXml(wn);
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    rsp = RandomSkillPreferences
                            .generateRandomSkillPreferencesFromXml(wn);
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    NodeList wList = wn.getChildNodes();

                    // Okay, lets iterate through the children, eh?
                    for (int x2 = 0; x2 < wList.getLength(); x2++) {
                        Node wn2 = wList.item(x2);

                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (wn2.getNodeName().startsWith("ability-")) {
                            continue;
                        } else if (!wn2.getNodeName().equalsIgnoreCase(
                                "skillType")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                                    "Unknown node type not loaded in Skill Type nodes: " //$NON-NLS-1$
                                    + wn2.getNodeName());

                            continue;
                        }
                        SkillType.generateInstanceFromXML(wn2, version);
                    }
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    PilotOptions poptions = new PilotOptions();
                    SpecialAbility.clearSPA();

                    NodeList wList = wn.getChildNodes();

                    // Okay, lets iterate through the children, eh?
                    for (int x2 = 0; x2 < wList.getLength(); x2++) {
                        Node wn2 = wList.item(x2);

                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                                    "Unknown node type not loaded in Special Ability nodes: " //$NON-NLS-1$
                                    + wn2.getNodeName());

                            continue;
                        }

                        SpecialAbility.generateInstanceFromXML(wn2, poptions,
                                null);
                    }
                }

            }

            if (null != options) {
                this.getCampaign().setCampaignOptions(options);
            }
            if (null != rsp) {
                this.getCampaign().setRandomSkillPreferences(rsp);
            }

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Finished load of campaign options file"); //$NON-NLS-1$
            MekHQ.triggerEvent(new OptionsChangedEvent(getCampaign(), options));
        }

        refreshCalendar();
        getCampaign().reloadNews();
    }

    private void savePartsFile() throws IOException {
        String METHOD_NAME = "savePartsFile()";
        
        JFileChooser saveParts = new JFileChooser(".");
        saveParts.setDialogTitle("Save Parts");
        saveParts.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".parts");
            }

            @Override
            public String getDescription() {
                return "Parts file";
            }
        });
        saveParts.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString()
                + "_ExportedParts" + ".parts")); //$NON-NLS-1$
        int returnVal = saveParts.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveParts.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = saveParts.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".parts")) {
            path += ".parts";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        if (getTab(GuiTabType.WAREHOUSE) != null) {
	        try {
	        	JTable partsTable = ((WarehouseTab)getTab(GuiTabType.WAREHOUSE)).getPartsTable();
	        	PartsTableModel partsModel = ((WarehouseTab)getTab(GuiTabType.WAREHOUSE)).getPartsModel();
	            int row = partsTable.getSelectedRow();
	            if (row < 0) {
	                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
	                        "ERROR: Cannot export parts if none are selected! Ignoring."); //$NON-NLS-1$
	                return;
	            }
	            Part selectedPart = partsModel.getPartAt(partsTable
	                    .convertRowIndexToModel(row));
	            int[] rows = partsTable.getSelectedRows();
	            Part[] parts = new Part[rows.length];
	            for (int i = 0; i < rows.length; i++) {
	                parts[i] = partsModel.getPartAt(partsTable
	                        .convertRowIndexToModel(rows[i]));
	            }
	            fos = new FileOutputStream(file);
	            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));
	
	            // File header
	            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	
	            ResourceBundle resourceMap = ResourceBundle
	                    .getBundle("mekhq.resources.MekHQ");
	            // Start the XML root.
	            pw.println("<parts version=\""
	                    + resourceMap.getString("Application.version") + "\">");
	
	            if (rows.length > 1) {
	                for (int i = 0; i < rows.length; i++) {
	                    parts[i].writeToXml(pw, 1);
	                }
	            } else {
	                selectedPart.writeToXml(pw, 1);
	            }
	            // Okay, we're done.
	            // Close everything out and be done with it.
	            pw.println("</parts>");
	            pw.flush();
	            pw.close();
	            fos.close();
	            // delete the backup file because we didn't need it
	            if (backupFile.exists()) {
	                backupFile.delete();
	            }
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                        "Parts saved to " + file); //$NON-NLS-1$
	        } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
	            JOptionPane
	                    .showMessageDialog(
	                            getFrame(),
	                            "Oh no! The program was unable to correctly export your parts. We know this\n"
	                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
	                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
	                                    + "the future.", "Could not export parts",
	                            JOptionPane.ERROR_MESSAGE);
	            // restore the backup file
	            file.delete();
	            if (backupFile.exists()) {
	                Utilities.copyfile(backupFile, file);
	                backupFile.delete();
	            }
	        }
        }
    }

    private void exportTable(JTable table, String suggestedName) {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle("Save Table");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "comma-separated text file";
            }
        });
        fileChooser.setSelectedFile(new File(suggestedName)); //$NON-NLS-1$
        int returnVal = fileChooser.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (fileChooser.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".csv")) {
            path += ".csv";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        Utilities.exportTabletoCSV(table, file);
    }
    
    public void refreshAllTabs() {
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            ((CampaignGuiTab)tabMain.getComponentAt(i)).refreshAll();
        }
    }

    public void refreshLab() {
    	MekLabTab lab = (MekLabTab)getTab(GuiTabType.MEKLAB);
    	if (null == lab) {
    		return;
    	}
    	Unit u = lab.getUnit();
    	if (null == u) {
    		return;
    	}
    	if (null == getCampaign().getUnit(u.getId())) {
    		// this unit has been removed so clear the mek lab
    		lab.clearUnit();
    	} else {
    		// put a try-catch here so that bugs in the meklab don't screw up
    		// other stuff
    		try {
    			lab.refreshSummary();
    		} catch (Exception err) {
    			err.printStackTrace();
    		}
    	}
    }

    public void refreshCalendar() {
        getFrame().setTitle(getCampaign().getTitle());
    }

    synchronized private void refreshReport() {
        List<String> newLogEntries = getCampaign().fetchAndClearNewReports();
        panLog.appendLog(newLogEntries);
        logDialog.appendLog(newLogEntries);
    }
    
    public void initReport() {
        String report = getCampaign().getCurrentReportHTML();
        panLog.refreshLog(report);
        logDialog.refreshLog(report);
        getCampaign().fetchAndClearNewReports();
    }

    private void refreshFunds() {
        long funds = getCampaign().getFunds();
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        String inDebt = "";
        if (getCampaign().getFinances().isInDebt()) {
            inDebt = " <font color='red'>(in Debt)</font>";
        }
        String text = "<html><b>Funds:</b> " + numberFormat.format(funds)
                + " C-Bills" + inDebt + "</html>";
        lblFunds.setText(text);
    }

    private void refreshRating() {
        if (getCampaign().getCampaignOptions().useDragoonRating()) {
            String text = "<html><b>Dragoons Rating:</b> "
                    + getCampaign().getUnitRating() + "</html>";
            lblRating.setText(text);
        } else {
            lblRating.setText("");
        }
    }

    private void refreshTempAstechs() {
        String text = "<html><b>Temp Astechs:</b> "
                + getCampaign().getAstechPool() + "</html>";
        lblTempAstechs.setText(text);
    }
    
    private void refreshTempMedics() {
        String text = "<html><b>Temp Medics:</b> "
                + getCampaign().getMedicPool() + "</html>";
        lblTempMedics.setText(text);
    }

    private ActionScheduler fundsScheduler = new ActionScheduler(this::refreshFunds);
    private ActionScheduler ratingScheduler = new ActionScheduler(this::refreshRating);
    
    @Subscribe
    public void handle(ReportEvent ev) {
        refreshReport();
    }
    
    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        fundsScheduler.schedule();
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(TransactionEvent ev) {
        fundsScheduler.schedule();
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(LoanEvent ev) {
        fundsScheduler.schedule();
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(AssetEvent ev) {
        fundsScheduler.schedule();
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(MissionEvent ev) {
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(PersonEvent ev) {
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(UnitEvent ev) {
        ratingScheduler.schedule();
    }
    
    @Subscribe
    public void handle(AstechPoolChangedEvent ev) {
        refreshTempAstechs();
    }
    
    @Subscribe
    public void handle(MedicPoolChangedEvent ev) {
        refreshTempMedics();
    }

    public void refreshLocation() {
        lblLocation.setText(getCampaign().getLocation().getReport(
                getCampaign().getCalendar().getTime()));
    }

    protected MekHQ getApplication() {
        return app;
    }

    public Campaign getCampaign() {
        return getApplication().getCampaign();
    }

    public IconPackage getIconPackage() {
        return getApplication().getIconPackage();
    }

    public JFrame getFrame() {
        return frame;
    }

    public CampaignGUI getCampaignGUI() {
        return this;
    }

    public int getTabIndexByName(String tabTitle) {
        int retVal = -1;
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            if (tabMain.getTitleAt(i).equals(tabTitle)) {
                retVal = i;
                break;
            }
        }
        return retVal;
    }

    public void undeployUnit(Unit u) {
        Force f = getCampaign().getForce(u.getForceId());
        if (f != null) {
            undeployForce(f, false);
        }
        Scenario s = getCampaign().getScenario(u.getScenarioId());
        s.removeUnit(u.getId());
        u.undeploy();
        MekHQ.triggerEvent(new DeploymentChangedEvent(u, s));
    }

    public void undeployForce(Force f) {
        undeployForce(f, true);
    }

    public void undeployForce(Force f, boolean killSubs) {
        int sid = f.getScenarioId();
        Scenario scenario = getCampaign().getScenario(sid);
        if (null != f && null != scenario) {
            f.clearScenarioIds(getCampaign(), killSubs);
            scenario.removeForce(f.getId());
            if (killSubs) {
                for (UUID uid : f.getAllUnits()) {
                    Unit u = getCampaign().getUnit(uid);
                    if (null != u) {
                        scenario.removeUnit(u.getId());
                        u.undeploy();
                    }
                }
            }

            // We have to clear out the parents as well.
            Force parent = f;
            int prevId = f.getId();
            while ((parent = parent.getParentForce()) != null) {
                if (parent.getScenarioId() == -1) {
                    break;
                }
                parent.clearScenarioIds(getCampaign(), false);
                scenario.removeForce(parent.getId());
                for (Force sub : parent.getSubForces()) {
                    if (sub.getId() == prevId) {
                        continue;
                    }
                    scenario.addForces(sub.getId());
                    sub.setScenarioId(scenario.getId());
                }
                prevId = parent.getId();
            }
        }
        
        if (null != scenario) {
        	MekHQ.triggerEvent(new DeploymentChangedEvent(f, scenario));
        }
    }

    public JTabbedPane getTabMain() {
        return tabMain;
    }

    /**
     * @return the resourceMap
     */
    public ResourceBundle getResourceMap() {
        return resourceMap;
    }

    private void setCampaignOptionsFromGameOptions() {
        getCampaign().getCampaignOptions().setUseTactics(getCampaign().getGameOptions().getOption("command_init").booleanValue());
        getCampaign().getCampaignOptions().setInitBonus(getCampaign().getGameOptions().getOption("individual_initiative").booleanValue());
        getCampaign().getCampaignOptions().setToughness(getCampaign().getGameOptions().getOption("toughness").booleanValue());
        getCampaign().getCampaignOptions().setArtillery(getCampaign().getGameOptions().getOption("artillery_skill").booleanValue());
        getCampaign().getCampaignOptions().setAbilities(getCampaign().getGameOptions().getOption("pilot_advantages").booleanValue());
        getCampaign().getCampaignOptions().setEdge(getCampaign().getGameOptions().getOption("edge").booleanValue());
        getCampaign().getCampaignOptions().setImplants(getCampaign().getGameOptions().getOption("manei_domini").booleanValue());
        getCampaign().getCampaignOptions().setQuirks(getCampaign().getGameOptions().getOption("stratops_quirks").booleanValue());
        getCampaign().getCampaignOptions().setAllowCanonOnly(getCampaign().getGameOptions().getOption("canon_only").booleanValue());
        getCampaign().getCampaignOptions().setTechLevel(TechConstants.getSimpleLevel(getCampaign().getGameOptions().getOption("techlevel").stringValue()));
        MekHQ.triggerEvent(new OptionsChangedEvent(getCampaign()));
    }
}
