/*
 * MekBayView.java
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
package mekhq;

import gd.xml.ParseException;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.MechView;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.XMLStreamParser;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.Force;
import mekhq.campaign.PartInventory;
import mekhq.campaign.SkillCosts;
import mekhq.campaign.Unit;
import mekhq.campaign.Utilities;
import mekhq.campaign.parts.GenericSparePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.personnel.SupportPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.SalvageItem;
import mekhq.campaign.work.WorkItem;

import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.TaskMonitor;

/**
 * The application's main frame.
 */
public class MekHQView extends FrameView {
	
	//personnel filter groups
	private static final int PG_ACTIVE =  0;
	private static final int PG_COMBAT =  1;
	private static final int PG_SUPPORT = 2;
	private static final int PG_MW =      3;
	private static final int PG_CREW =    4;
	private static final int PG_PILOT =   5;
	private static final int PG_PROTO =   6;
	private static final int PG_BA =      7;
	private static final int PG_TECH =    8;
	private static final int PG_DOC =     9;
	private static final int PG_RETIRE =  10;
	private static final int PG_MIA =     11;
	private static final int PG_KIA =     12;
	private static final int PG_NUM =     13;
	
	//personnel views
	private static final int PV_GENERAL = 0;
	private static final int PV_COMBAT  = 1;
	private static final int PV_FLUFF   = 2;
	private static final int PV_NUM     = 3;
	
	//unit views
	private static final int UV_GENERAL = 0;
	private static final int UV_DETAILS = 1;
	private static final int UV_STATUS  = 2;
	private static final int UV_NUM     = 3;
	
	class ExtFileFilter extends FileFilter {
		private String useExt = null;

		public ExtFileFilter(String ext) {
			useExt = ext;
		}

		@Override
		public boolean accept(File dir) {
			if (dir.isDirectory()) {
				return true;
			}
			return dir.getName().endsWith(useExt);
		}

		@Override
		public String getDescription() {
			return "campaign file (" + useExt + ")";
		}
	}

	private Campaign campaign = new Campaign();
	private TaskTableModel taskModel = new TaskTableModel();
	private AcquisitionTableModel acquireModel = new AcquisitionTableModel();
	private ServicedUnitTableModel servicedUnitModel = new ServicedUnitTableModel();
	private TechTableModel techsModel = new TechTableModel();
	private PatientTableModel patientModel = new PatientTableModel();
	private DocTableModel doctorsModel = new DocTableModel();
	private PersonnelTableModel personModel = new PersonnelTableModel();
	private UnitTableModel unitModel = new UnitTableModel();
	private PartsTableModel partsModel = new PartsTableModel();
	private DefaultTreeModel orgModel;
	private UnitTableMouseAdapter unitMouseAdapter;
	private ServicedUnitsTableMouseAdapter servicedUnitMouseAdapter;
	private PartsTableMouseAdapter partsMouseAdapter;
	private TaskTableMouseAdapter taskMouseAdapter;
	private PersonnelTableMouseAdapter personnelMouseAdapter;
	private OrgTreeMouseAdapter orgMouseAdapter;
	private TableRowSorter<PersonnelTableModel> personnelSorter;
	private TableRowSorter<UnitTableModel> unitSorter;
	private int currentServicedUnitId;
	private int currentTaskId;
	private int currentAcquisitionId;
	private int currentTechId;
	private int currentPatientId;
	private int currentDoctorId;
	private int currentPartsId;
	private int[] selectedTasksIds;

	//the various directory items we need to access
	private DirectoryItems portraits;
    private DirectoryItems camos;
    private DirectoryItems forceIcons;
	protected static MechTileset mt;
	
	public MekHQView(SingleFrameApplication app) {
		super(app);

		unitMouseAdapter = new UnitTableMouseAdapter();
		servicedUnitMouseAdapter = new ServicedUnitsTableMouseAdapter();
		partsMouseAdapter = new PartsTableMouseAdapter();
		taskMouseAdapter = new TaskTableMouseAdapter();
		personnelMouseAdapter = new PersonnelTableMouseAdapter(this);
		orgMouseAdapter = new OrgTreeMouseAdapter();
		
		//load in directory items and tilesets
		try {
            portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    PortraitFileFactory.getInstance());
        } catch (Exception e) {
            portraits = null;
        }
        try {
            camos = new DirectoryItems(new File("data/images/camo"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            camos = null;
        }
        try {
            forceIcons = new DirectoryItems(new File("data/images/force"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    PortraitFileFactory.getInstance());
        } catch (Exception e) {
            forceIcons = null;
        }
        mt = new MechTileset("data/images/units/");
        try {
            mt.loadFromFile("mechset.txt");
        } catch (IOException ex) {
        	MekHQApp.logError(ex);
            //TODO: do something here
        }
		
		initComponents();
		refreshCalendar();
		
		// status bar initialization - message timeout, idle icon and busy
		// animation, etc
		ResourceMap resourceMap = getResourceMap();
		int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
		messageTimer = new Timer(messageTimeout, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusMessageLabel.setText("");
			}
		});
		messageTimer.setRepeats(false);
		int busyAnimationRate = resourceMap
				.getInteger("StatusBar.busyAnimationRate");
		for (int i = 0; i < busyIcons.length; i++) {
			busyIcons[i] = resourceMap
					.getIcon("StatusBar.busyIcons[" + i + "]");
		}
		busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
				statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
			}
		});
		idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
		statusAnimationLabel.setIcon(idleIcon);
		progressBar.setVisible(false);

		// connecting action tasks to status bar via TaskMonitor
		TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
		taskMonitor
				.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
					public void propertyChange(
							java.beans.PropertyChangeEvent evt) {
						String propertyName = evt.getPropertyName();
						if ("started".equals(propertyName)) {
							if (!busyIconTimer.isRunning()) {
								statusAnimationLabel.setIcon(busyIcons[0]);
								busyIconIndex = 0;
								busyIconTimer.start();
							}
							progressBar.setVisible(true);
							progressBar.setIndeterminate(true);
						} else if ("done".equals(propertyName)) {
							busyIconTimer.stop();
							statusAnimationLabel.setIcon(idleIcon);
							progressBar.setVisible(false);
							progressBar.setValue(0);
						} else if ("message".equals(propertyName)) {
							String text = (String) (evt.getNewValue());
							statusMessageLabel.setText((text == null) ? ""
									: text);
							messageTimer.restart();
						} else if ("progress".equals(propertyName)) {
							int value = (Integer) (evt.getNewValue());
							progressBar.setVisible(true);
							progressBar.setIndeterminate(false);
							progressBar.setValue(value);
						}
					}
				});
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = MekHQApp.getApplication().getMainFrame();
			aboutBox = new MekHQAboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
		}
		MekHQApp.getApplication().show(aboutBox);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		mainPanel = new javax.swing.JPanel();
		tabMain = new javax.swing.JTabbedPane();
		tabTasks = new javax.swing.JTabbedPane();
		panOrganization = new javax.swing.JPanel();
		scrollOrgTree = new javax.swing.JScrollPane();
		orgTree = new javax.swing.JTree();
		panPersonnel = new javax.swing.JPanel();
		scrollPersonnelTable = new javax.swing.JScrollPane();
		personnelTable = new javax.swing.JTable();
		panHangar = new javax.swing.JPanel();
		scrollUnitTable = new javax.swing.JScrollPane();
		unitTable = new javax.swing.JTable();
		panRepairBay = new javax.swing.JPanel();
		scrollTaskTable = new javax.swing.JScrollPane();
		TaskTable = new javax.swing.JTable();
		scrollAcquisitionTable = new javax.swing.JScrollPane();
		AcquisitionTable = new javax.swing.JTable();
		scrollServicedUnitTable = new javax.swing.JScrollPane();
		servicedUnitTable = new javax.swing.JTable();
		scrollTechTable = new javax.swing.JScrollPane();
		TechTable = new javax.swing.JTable();
		btnUnitPanel = new javax.swing.JPanel();
		btnDeployUnits = new javax.swing.JButton();
		btnRetrieveUnits = new javax.swing.JButton();
		panelDoTask = new javax.swing.JPanel();
		btnDoTask = new javax.swing.JButton();
		lblTarget = new javax.swing.JLabel();
		lblTargetNum = new javax.swing.JLabel();
		jScrollPane6 = new javax.swing.JScrollPane();
		textTarget = new javax.swing.JTextArea();
		panSupplies = new javax.swing.JPanel();
		jScrollPane8 = new javax.swing.JScrollPane();
		PartsTable = new javax.swing.JTable();
		jScrollPane1 = new javax.swing.JScrollPane();
		PartsFilter = new javax.swing.JComboBox();
		btnSaveParts = new javax.swing.JButton();
		btnLoadParts = new javax.swing.JButton();
		panInfirmary = new javax.swing.JPanel();
		btnAssignDoc = new javax.swing.JButton();
		scrollPatientTable = new javax.swing.JScrollPane();
		patientTable = new javax.swing.JTable();
		scrollDocTable = new javax.swing.JScrollPane();
		DocTable = new javax.swing.JTable();
		panFinances = new javax.swing.JPanel();
		jLabel2 = new javax.swing.JLabel();
		txtPaneReportScrollPane = new javax.swing.JScrollPane();
		txtPaneReport = new javax.swing.JTextPane();
		panelMasterButtons = new javax.swing.JPanel();
		btnAdvanceDay = new javax.swing.JButton();
		btnOvertime = new javax.swing.JToggleButton();
		btnGMMode = new javax.swing.JToggleButton();
		fundsLabel = new javax.swing.JLabel();
		menuBar = new javax.swing.JMenuBar();
		javax.swing.JMenu fileMenu = new javax.swing.JMenu();
		menuLoad = new javax.swing.JMenuItem();
		menuSave = new javax.swing.JMenuItem();
		menuLoadXml = new javax.swing.JMenuItem();
		menuSaveXml = new javax.swing.JMenuItem();
		menuOptions = new javax.swing.JMenuItem();
		javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
		menuManage = new javax.swing.JMenu();
		miLoadForces = new javax.swing.JMenuItem();
		addFunds = new javax.swing.JMenuItem();
		menuMarket = new javax.swing.JMenu();
		miPurchaseUnit = new javax.swing.JMenuItem();
		menuHire = new javax.swing.JMenu();
		miHirePilot = new javax.swing.JMenuItem();
		miHireTech = new javax.swing.JMenuItem();
		miHireDoctor = new javax.swing.JMenuItem();
		javax.swing.JMenu helpMenu = new javax.swing.JMenu();
		javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
		statusPanel = new javax.swing.JPanel();
		javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
		statusMessageLabel = new javax.swing.JLabel();
		statusAnimationLabel = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();
		lblPersonChoice = new javax.swing.JLabel();
		choicePerson = new javax.swing.JComboBox();
		choicePersonView = new javax.swing.JComboBox();
		lblPersonView = new javax.swing.JLabel();
		scrollPersonnelView = new javax.swing.JScrollPane();
		lblUnitChoice = new javax.swing.JLabel();
		choiceUnit = new javax.swing.JComboBox();
		choiceUnitView = new javax.swing.JComboBox();
		lblUnitView = new javax.swing.JLabel();
		scrollUnitView = new javax.swing.JScrollPane();

		mainPanel.setAutoscrolls(true);
		mainPanel.setName("mainPanel"); // NOI18N
		mainPanel.setLayout(new java.awt.GridBagLayout());

		org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application
				.getInstance(mekhq.MekHQApp.class).getContext()
				.getResourceMap(MekHQView.class);
		tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
		tabMain.setMinimumSize(new java.awt.Dimension(600, 200));
		tabMain.setName("tabMain"); // NOI18N
		tabMain.setPreferredSize(new java.awt.Dimension(900, 300));

		panOrganization.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
		panOrganization.setName("panOrganization"); // NOI18N
		panOrganization.setLayout(new java.awt.GridBagLayout());
		
		refreshOrganization();
		orgTree.setModel(orgModel);
		orgTree.addMouseListener(orgMouseAdapter);
        orgTree.setCellRenderer(new ForceRenderer());
        orgTree.setRowHeight(0);
		scrollOrgTree.setViewportView(orgTree);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		panOrganization.add(scrollOrgTree, gridBagConstraints);
		
		tabMain.addTab(
				resourceMap.getString("panOrganization.TabConstraints.tabTitle"),
				panOrganization); // NOI18N
		
		panPersonnel.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
		panPersonnel.setName("panPersonnel"); // NOI18N
		panPersonnel.setLayout(new java.awt.GridBagLayout());
		
		lblPersonChoice.setText(resourceMap.getString("lblPersonChoice.text")); // NOI18N
		lblPersonChoice.setName("lblPersonChoice"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panPersonnel.add(lblPersonChoice, gridBagConstraints);
		
		DefaultComboBoxModel personGroupModel = new DefaultComboBoxModel();
		for (int i = 0; i < PG_NUM; i++) {
			personGroupModel.addElement(getPersonnelGroupName(i));
		}
		choicePerson.setModel(personGroupModel);
		choicePerson.setName("choicePerson"); // NOI18N
		choicePerson.setSelectedIndex(0);
		choicePerson.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				filterPersonnel();
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panPersonnel.add(choicePerson, gridBagConstraints);
			
		lblPersonView.setText(resourceMap.getString("lblPersonView.text")); // NOI18N
		lblPersonView.setName("lblPersonView"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panPersonnel.add(lblPersonView, gridBagConstraints);
		
		DefaultComboBoxModel personViewModel = new DefaultComboBoxModel();
		for (int i = 0; i < PV_NUM; i++) {
			personViewModel.addElement(getPersonnelViewName(i));
		}
		choicePersonView.setModel(personViewModel);
		choicePersonView.setName("choicePersonView"); // NOI18N
		choicePersonView.setSelectedIndex(0);
		choicePersonView.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				changePersonnelView();
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panPersonnel.add(choicePersonView, gridBagConstraints);
		
		personnelTable.setModel(personModel);
		personnelTable.setName("personnelTable"); // NOI18N
		personnelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel personColumnModel = new XTableColumnModel();
        personnelTable.setColumnModel(personColumnModel);
        personnelTable.createDefaultColumnsFromModel();
        personnelSorter = new TableRowSorter<PersonnelTableModel>(personModel);
        personnelSorter.setComparator(PersonnelTableModel.COL_RANK, new RankSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_GUN, new SkillSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_PILOT, new SkillSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_ARTY, new SkillSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_TACTICS, new BonusSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_INIT, new BonusSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_TOUGH, new BonusSorter());
        personnelTable.setRowSorter(personnelSorter);
		personnelTable.addMouseListener(personnelMouseAdapter);
		personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = personnelTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(personModel.getColumnWidth(i));
            column.setCellRenderer(personModel.getRenderer());
        }
        personnelTable.setIntercellSpacing(new Dimension(0, 0));
        changePersonnelView();
        personnelTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                refreshPersonnelView();
            }
        });
        scrollPersonnelTable.setViewportView(personnelTable);
	
		scrollPersonnelView.setMinimumSize(new java.awt.Dimension(450, 600));
		scrollPersonnelView.setPreferredSize(new java.awt.Dimension(450, 2000));
		scrollPersonnelView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPersonnelView.setViewportView(null);
		
		splitPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPersonnelTable, scrollPersonnelView);
		splitPersonnel.setOneTouchExpandable(true);
		splitPersonnel.setResizeWeight(1.0);
		splitPersonnel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				//this can mess up the pilot view pane so refresh it
				refreshPersonnelView();
			}
		});
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		panPersonnel.add(splitPersonnel, gridBagConstraints);
		
		tabMain.addTab(
				resourceMap.getString("panPersonnel.TabConstraints.tabTitle"),
				panPersonnel); // NOI18N
		
		panHangar.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
		panHangar.setName("panHangar"); // NOI18N
		panHangar.setLayout(new java.awt.GridBagLayout());
		
		btnDeployUnits.setText(resourceMap.getString("btnDeployUnits.text")); // NOI18N
		btnDeployUnits.setToolTipText(resourceMap
				.getString("btnDeployUnits.toolTipText")); // NOI18N
		btnDeployUnits.setName("btnDeployUnits"); // NOI18N
		btnDeployUnits.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDeployUnitsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.0;
		panHangar.add(btnDeployUnits, gridBagConstraints);

		btnRetrieveUnits
				.setText(resourceMap.getString("btnRetrieveUnits.text")); // NOI18N
		btnRetrieveUnits.setToolTipText(resourceMap
				.getString("btnRetrieveUnits.toolTipText")); // NOI18N
		btnRetrieveUnits.setName("btnRetrieveUnits"); // NOI18N
		btnRetrieveUnits.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnRetrieveUnitsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.0;
		panHangar.add(btnRetrieveUnits, gridBagConstraints);
		
		lblUnitChoice.setText(resourceMap.getString("lblUnitChoice.text")); // NOI18N
		lblUnitChoice.setName("lblUnitChoice"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panHangar.add(lblUnitChoice, gridBagConstraints);
		
		DefaultComboBoxModel unitGroupModel = new DefaultComboBoxModel();
		unitGroupModel.addElement("All Units");
		for (int i = 0; i < UnitType.SIZE; i++) {
			unitGroupModel.addElement(UnitType.getTypeDisplayableName(i));
		}
		choiceUnit.setModel(unitGroupModel);
		choiceUnit.setName("choiceUnit"); // NOI18N
		choiceUnit.setSelectedIndex(0);
		choiceUnit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				filterUnits();
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panHangar.add(choiceUnit, gridBagConstraints);
			
		lblUnitView.setText(resourceMap.getString("lblUnitView.text")); // NOI18N
		lblPersonView.setName("lblUnitView"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panHangar.add(lblUnitView, gridBagConstraints);
		
		DefaultComboBoxModel unitViewModel = new DefaultComboBoxModel();
		for (int i = 0; i < UV_NUM; i++) {
			unitViewModel.addElement(getUnitViewName(i));
		}
		choiceUnitView.setModel(unitViewModel);
		choiceUnitView.setName("choiceUnitView"); // NOI18N
		choiceUnitView.setSelectedIndex(0);
		choiceUnitView.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				changeUnitView();
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		panHangar.add(choiceUnitView, gridBagConstraints);
		
		unitTable.setModel(unitModel);
		unitTable.setName("unitTable"); // NOI18N
		unitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		XTableColumnModel unitColumnModel = new XTableColumnModel();
        unitTable.setColumnModel(unitColumnModel);
        unitTable.createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<UnitTableModel>(unitModel);
        unitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        unitSorter.setComparator(UnitTableModel.COL_WCLASS, new WeightClassSorter());
        unitSorter.setComparator(UnitTableModel.COL_COST, new FormattedNumberSorter());
        unitTable.setRowSorter(unitSorter);
		unitTable.addMouseListener(unitMouseAdapter);
		unitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = unitTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(unitModel.getColumnWidth(i));
            column.setCellRenderer(unitModel.getRenderer());
        }
        unitTable.setIntercellSpacing(new Dimension(0, 0));
        changeUnitView();
        unitTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                refreshUnitView();
            }
        });
        
        scrollUnitTable.setViewportView(unitTable);

		scrollUnitView.setMinimumSize(new java.awt.Dimension(500, 600));
		scrollUnitView.setPreferredSize(new java.awt.Dimension(500, 2000));
		scrollUnitView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollUnitView.setViewportView(null);
		
		splitUnit = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,scrollUnitTable, scrollUnitView);
		splitUnit.setOneTouchExpandable(true);
		splitUnit.setResizeWeight(1.0);
		splitUnit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				//this can mess up the unit view panel so refresh it
				refreshUnitView();
			}
		});
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		panHangar.add(splitUnit, gridBagConstraints);
		
		tabMain.addTab(
				resourceMap.getString("panHangar.TabConstraints.tabTitle"),
				panHangar); // NOI18N
		
		panRepairBay.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
		panRepairBay.setName("panRepairBay"); // NOI18N
		panRepairBay.setLayout(new java.awt.GridBagLayout());
		
		tabTasks.setToolTipText(resourceMap.getString("tabTasks.toolTipText")); // NOI18N
		tabTasks.setMinimumSize(new java.awt.Dimension(600, 200));
		tabTasks.setName("tabTasks"); // NOI18N
		tabTasks.setPreferredSize(new java.awt.Dimension(300, 300));
		tabTasks.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
		        taskTabChanged();
		    }
		});

		scrollTaskTable.setMinimumSize(new java.awt.Dimension(200, 200));
		scrollTaskTable.setName("scrollTaskTable"); // NOI18N
		scrollTaskTable.setPreferredSize(new java.awt.Dimension(300, 300));

		TaskTable.setModel(taskModel);
		TaskTable.setName("TaskTable"); // NOI18N
		TaskTable.setRowHeight(60);
		TaskTable.getColumnModel().getColumn(0)
				.setCellRenderer(taskModel.getRenderer());
		TaskTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						TaskTableValueChanged(evt);
					}
				});
		TaskTable.addMouseListener(taskMouseAdapter);
		scrollTaskTable.setViewportView(TaskTable);

		scrollAcquisitionTable.setMinimumSize(new java.awt.Dimension(200, 200));
		scrollAcquisitionTable.setName("scrollAcquisitionTable"); // NOI18N
		scrollAcquisitionTable.setPreferredSize(new java.awt.Dimension(300, 300));

		AcquisitionTable.setModel(acquireModel);
		AcquisitionTable.setName("AcquisitionTable"); // NOI18N
		AcquisitionTable.setRowHeight(60);
		AcquisitionTable.getColumnModel().getColumn(0)
				.setCellRenderer(acquireModel.getRenderer());
		AcquisitionTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						AcquisitionTableValueChanged(evt);
					}
				});
		//AcquisitionTable.addMouseListener(acquisitionMouseAdapter);
		scrollAcquisitionTable.setViewportView(AcquisitionTable);

		
		tabTasks.addTab(resourceMap.getString("scrollTaskTable.TabConstraints.tabTasks"), scrollTaskTable); // NOI18N
		tabTasks.addTab(resourceMap.getString("scrollAcquisitionTable.TabConstraints.tabTasks"), scrollAcquisitionTable); // NOI18N

		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		panRepairBay.add(tabTasks, gridBagConstraints);

		scrollServicedUnitTable.setMinimumSize(new java.awt.Dimension(300, 200));
		scrollServicedUnitTable.setName("scrollServicedUnitTable"); // NOI18N
		scrollServicedUnitTable.setPreferredSize(new java.awt.Dimension(300, 300));

		servicedUnitTable.setModel(servicedUnitModel);
		servicedUnitTable.setName("servicedUnitTable"); // NOI18N
		servicedUnitTable.setRowHeight(80);
		servicedUnitTable.getColumnModel().getColumn(0)
				.setCellRenderer(servicedUnitModel.getRenderer());
		servicedUnitTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						servicedUnitTableValueChanged(evt);
					}
				});
		servicedUnitTable.addMouseListener(servicedUnitMouseAdapter);
		scrollServicedUnitTable.setViewportView(servicedUnitTable);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		panRepairBay.add(scrollServicedUnitTable, gridBagConstraints);

		scrollTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
		scrollTechTable.setName("scrollTechTable"); // NOI18N
		scrollTechTable.setPreferredSize(new java.awt.Dimension(300, 300));

		TechTable.setModel(techsModel);
		TechTable.setName("TechTable"); // NOI18N
		TechTable.setRowHeight(60);
		TechTable.getColumnModel().getColumn(0)
				.setCellRenderer(techsModel.getRenderer());
		TechTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						TechTableValueChanged(evt);
					}
				});
		scrollTechTable.setViewportView(TechTable);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		panRepairBay.add(scrollTechTable, gridBagConstraints);

		panelDoTask.setMinimumSize(new java.awt.Dimension(300, 100));
		panelDoTask.setName("panelDoTask"); // NOI18N
		panelDoTask.setPreferredSize(new java.awt.Dimension(300, 100));
		panelDoTask.setLayout(new java.awt.GridBagLayout());

		btnDoTask.setText(resourceMap.getString("btnDoTask.text")); // NOI18N
		btnDoTask
				.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
		btnDoTask.setEnabled(false);
		btnDoTask.setName("btnDoTask"); // NOI18N
		btnDoTask.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDoTaskActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		panelDoTask.add(btnDoTask, gridBagConstraints);

		lblTarget.setText(resourceMap.getString("lblTarget.text")); // NOI18N
		lblTarget.setName("lblTarget"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
		panelDoTask.add(lblTarget, gridBagConstraints);

		lblTargetNum.setFont(resourceMap.getFont("lblTargetNum.font")); // NOI18N
		lblTargetNum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		lblTargetNum.setText(resourceMap.getString("lblTargetNum.text")); // NOI18N
		lblTargetNum.setName("lblTargetNum"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
		panelDoTask.add(lblTargetNum, gridBagConstraints);

		jScrollPane6.setName("jScrollPane6"); // NOI18N

		textTarget.setBackground(resourceMap.getColor("textTarget.background")); // NOI18N
		textTarget.setColumns(20);
		textTarget.setEditable(false);
		textTarget.setFont(resourceMap.getFont("textTarget.font")); // NOI18N
		textTarget.setLineWrap(true);
		textTarget.setRows(5);
		textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
		textTarget.setWrapStyleWord(true);
		textTarget.setBorder(null);
		textTarget.setName("textTarget"); // NOI18N
		jScrollPane6.setViewportView(textTarget);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		panelDoTask.add(jScrollPane6, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.5;
		panRepairBay.add(panelDoTask, gridBagConstraints);

		tabMain.addTab(
				resourceMap.getString("panRepairBay.TabConstraints.tabTitle"),
				panRepairBay); // NOI18N

		panSupplies.setName("panSupplies"); // NOI18N

		jScrollPane8.setName("jScrollPane8"); // NOI18N

		PartsTable.setModel(partsModel);
		PartsTable.setName("PartsTable"); // NOI18N
		PartsTable.setRowHeight(60);
		PartsTable.getColumnModel().getColumn(0)
				.setCellRenderer(partsModel.getRenderer());
		PartsTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						PartsTableValueChanged(evt);
					}
				});
		PartsTable.addMouseListener(partsMouseAdapter);
		jScrollPane8.setViewportView(PartsTable);

		jScrollPane1.setName("jScrollPane1"); // NOI18N

		DefaultComboBoxModel partTypesModel = new DefaultComboBoxModel();
		String[] partTypeLabels = Part.getPartTypeLabels();
		partTypesModel.addElement("All");
		for (int i = 0; i < partTypeLabels.length; i++) {
			partTypesModel.addElement(partTypeLabels[i]);
		}
		PartsFilter.setModel(partTypesModel);
		PartsFilter.setName("PartsFilter"); // NOI18N
		PartsFilter.setSelectedIndex(0);
		PartsFilter.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				PartsFilterActionPerformed(evt);
			}
		});

		btnSaveParts.setText(resourceMap.getString("btnSaveParts.text")); // NOI18N
		btnSaveParts.setToolTipText(resourceMap
				.getString("btnSaveParts.toolTipText")); // NOI18N
		btnSaveParts.setName("btnSaveParts"); // NOI18N
		btnSaveParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSavePartsActionPerformed(evt);
			}
		});

		btnLoadParts.setText(resourceMap.getString("btnLoadParts.text")); // NOI18N
		btnLoadParts.setToolTipText(resourceMap
				.getString("btnLoadParts.toolTipText")); // NOI18N
		btnLoadParts.setName("btnLoadParts"); // NOI18N
		btnLoadParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnLoadPartsActionPerformed(evt);
			}
		});

		org.jdesktop.layout.GroupLayout panSuppliesLayout = new org.jdesktop.layout.GroupLayout(
				panSupplies);
		panSupplies.setLayout(panSuppliesLayout);
		panSuppliesLayout
				.setHorizontalGroup(panSuppliesLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(panSuppliesLayout
								.createSequentialGroup()
								.add(panSuppliesLayout
										.createParallelGroup(
												org.jdesktop.layout.GroupLayout.LEADING)
										.add(panSuppliesLayout
												.createSequentialGroup()
												.add(jScrollPane8,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														428,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												.add(132, 132, 132)
												.add(jScrollPane1,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.add(panSuppliesLayout
												.createSequentialGroup()
												.add(PartsFilter,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														162,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(
														org.jdesktop.layout.LayoutStyle.RELATED)
												.add(btnSaveParts)
												.addPreferredGap(
														org.jdesktop.layout.LayoutStyle.RELATED)
												.add(btnLoadParts)))
								.addContainerGap(498, Short.MAX_VALUE)));
		panSuppliesLayout
				.setVerticalGroup(panSuppliesLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(panSuppliesLayout
								.createSequentialGroup()
								.add(panSuppliesLayout
										.createParallelGroup(
												org.jdesktop.layout.GroupLayout.LEADING)
										.add(panSuppliesLayout
												.createSequentialGroup()
												.add(panSuppliesLayout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(PartsFilter,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																20,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(btnSaveParts)
														.add(btnLoadParts))
												.addPreferredGap(
														org.jdesktop.layout.LayoutStyle.RELATED)
												.add(jScrollPane8,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														573,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.add(panSuppliesLayout
												.createSequentialGroup()
												.add(192, 192, 192)
												.add(jScrollPane1,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
								.addContainerGap(
										org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)));

		tabMain.addTab(
				resourceMap.getString("panSupplies.TabConstraints.tabTitle"),
				panSupplies); // NOI18N

		panInfirmary.setName("panInfirmary"); // NOI18N
		panInfirmary.setLayout(new java.awt.GridBagLayout());

		btnAssignDoc.setText(resourceMap.getString("btnAssignDoc.text")); // NOI18N
		btnAssignDoc.setToolTipText(resourceMap
				.getString("btnAssignDoc.toolTipText")); // NOI18N
		btnAssignDoc.setEnabled(false);
		btnAssignDoc.setName("btnAssignDoc"); // NOI18N
		btnAssignDoc.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnAssignDocActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
		panInfirmary.add(btnAssignDoc, gridBagConstraints);

		scrollPatientTable.setMinimumSize(new java.awt.Dimension(300, 200));
		scrollPatientTable.setName("scrollPatientTable"); // NOI18N
		scrollPatientTable.setPreferredSize(new java.awt.Dimension(300, 300));

		patientTable.setModel(patientModel);
		patientTable.setName("PersonTable"); // NOI18N
		patientTable.setRowHeight(80);
		patientTable.getColumnModel().getColumn(0)
				.setCellRenderer(patientModel.getRenderer());
		patientTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						patientTableValueChanged(evt);
					}
				});
		scrollPatientTable.setViewportView(patientTable);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		panInfirmary.add(scrollPatientTable, gridBagConstraints);

		scrollDocTable.setMinimumSize(new java.awt.Dimension(300, 300));
		scrollDocTable.setName("scrollDocTable"); // NOI18N
		scrollDocTable.setPreferredSize(new java.awt.Dimension(300, 300));

		DocTable.setModel(doctorsModel);
		DocTable.setName("DocTable"); // NOI18N
		DocTable.setRowHeight(60);
		DocTable.getColumnModel().getColumn(0)
				.setCellRenderer(doctorsModel.getRenderer());
		DocTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						DocTableValueChanged(evt);
					}
				});
		scrollDocTable.setViewportView(DocTable);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		panInfirmary.add(scrollDocTable, gridBagConstraints);

		tabMain.addTab(
				resourceMap.getString("panInfirmary.TabConstraints.tabTitle"),
				panInfirmary); // NOI18N

		panFinances.setName("panFinances"); // NOI18N

		jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
		jLabel2.setName("jLabel2"); // NOI18N

		org.jdesktop.layout.GroupLayout panFinancesLayout = new org.jdesktop.layout.GroupLayout(
				panFinances);
		panFinances.setLayout(panFinancesLayout);
		panFinancesLayout.setHorizontalGroup(panFinancesLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(panFinancesLayout.createSequentialGroup()
						.addContainerGap().add(jLabel2)
						.addContainerGap(891, Short.MAX_VALUE)));
		panFinancesLayout.setVerticalGroup(panFinancesLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(panFinancesLayout.createSequentialGroup()
						.addContainerGap().add(jLabel2)
						.addContainerGap(566, Short.MAX_VALUE)));

		tabMain.addTab(
				resourceMap.getString("panFinances.TabConstraints.tabTitle"),
				panFinances); // NOI18N

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		mainPanel.add(tabMain, gridBagConstraints);

		txtPaneReportScrollPane
				.setMinimumSize(new java.awt.Dimension(800, 200));
		txtPaneReportScrollPane.setName("txtPaneReportScrollPane"); // NOI18N
		txtPaneReportScrollPane.setPreferredSize(new java.awt.Dimension(800,
				200));

		txtPaneReport.setContentType(resourceMap
				.getString("txtPaneReport.contentType")); // NOI18N
		txtPaneReport.setEditable(false);
		txtPaneReport.setFont(resourceMap.getFont("txtPaneReport.font")); // NOI18N
		txtPaneReport.setText(campaign.getCurrentReportHTML());
		txtPaneReport.setMinimumSize(new java.awt.Dimension(800, 200));
		txtPaneReport.setName("txtPaneReport"); // NOI18N
		txtPaneReport.setPreferredSize(new java.awt.Dimension(800, 200));
		txtPaneReportScrollPane.setViewportView(txtPaneReport);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
		mainPanel.add(txtPaneReportScrollPane, gridBagConstraints);

		panelMasterButtons.setMinimumSize(new java.awt.Dimension(200, 200));
		panelMasterButtons.setName("panelMasterButtons"); // NOI18N
		panelMasterButtons.setPreferredSize(new java.awt.Dimension(200, 220));
		panelMasterButtons.setLayout(new java.awt.GridBagLayout());

		btnAdvanceDay.setText(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
		btnAdvanceDay.setToolTipText(resourceMap
				.getString("btnAdvanceDay.toolTipText")); // NOI18N
		btnAdvanceDay.setName("btnAdvanceDay"); // NOI18N
		btnAdvanceDay.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnAdvanceDayActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 15;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(12, 30, 0, 0);
		panelMasterButtons.add(btnAdvanceDay, gridBagConstraints);

		btnOvertime.setText(resourceMap.getString("btnOvertime.text")); // NOI18N
		btnOvertime.setToolTipText(resourceMap
				.getString("btnOvertime.toolTipText")); // NOI18N
		btnOvertime.setName("btnOvertime"); // NOI18N
		btnOvertime.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnOvertimeActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 15;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(20, 30, 0, 0);
		panelMasterButtons.add(btnOvertime, gridBagConstraints);

		btnGMMode.setText(resourceMap.getString("btnGMMode.text")); // NOI18N
		btnGMMode
				.setToolTipText(resourceMap.getString("btnGMMode.toolTipText")); // NOI18N
		btnGMMode.setName("btnGMMode"); // NOI18N
		btnGMMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnGMModeActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 15;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 30, 42, 0);
		panelMasterButtons.add(btnGMMode, gridBagConstraints);

		fundsLabel.setFont(resourceMap.getFont("fundsLabel.font")); // NOI18N
		fundsLabel.setText(resourceMap.getString("fundsLabel.text")); // NOI18N
		fundsLabel.setName("fundsLabel"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
		panelMasterButtons.add(fundsLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		mainPanel.add(panelMasterButtons, gridBagConstraints);

		menuBar.setName("menuBar"); // NOI18N

		fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
		fileMenu.setName("fileMenu"); // NOI18N

		menuLoad.setText(resourceMap.getString("menuLoad.text")); // NOI18N
		menuLoad.setName("menuLoad"); // NOI18N
		menuLoad.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuLoadActionPerformed(evt);
			}
		});
		fileMenu.add(menuLoad);

		menuSave.setText(resourceMap.getString("menuSave.text")); // NOI18N
		menuSave.setName("menuSave"); // NOI18N
		menuSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuSaveActionPerformed(evt);
			}
		});
		fileMenu.add(menuSave);

		menuLoadXml.setText(resourceMap.getString("menuLoadXml.text")); // NOI18N
		menuLoadXml.setName("menuLoadXml"); // NOI18N
		menuLoadXml.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuLoadXmlActionPerformed(evt);
			}
		});
		fileMenu.add(menuLoadXml);

		menuSaveXml.setText(resourceMap.getString("menuSaveXml.text")); // NOI18N
		menuSaveXml.setName("menuSaveXml"); // NOI18N
		menuSaveXml.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuSaveXmlActionPerformed(evt);
			}
		});
		fileMenu.add(menuSaveXml);

		menuOptions.setText(resourceMap.getString("menuOptions.text")); // NOI18N
		menuOptions.setName("menuOptions"); // NOI18N
		menuOptions.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuOptionsActionPerformed(evt);
			}
		});
		fileMenu.add(menuOptions);

		javax.swing.ActionMap actionMap = org.jdesktop.application.Application
				.getInstance(mekhq.MekHQApp.class).getContext()
				.getActionMap(MekHQView.class, this);
		exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
		exitMenuItem.setName("exitMenuItem"); // NOI18N
		fileMenu.add(exitMenuItem);

		menuBar.add(fileMenu);

		menuManage.setText(resourceMap.getString("menuManage.text")); // NOI18N
		menuManage.setName("menuManage"); // NOI18N

		miLoadForces.setText(resourceMap.getString("miLoadForces.text")); // NOI18N
		miLoadForces.setName("miLoadForces"); // NOI18N
		miLoadForces.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miLoadForcesActionPerformed(evt);
			}
		});
		menuManage.add(miLoadForces);

		addFunds.setText(resourceMap.getString("addFunds.text")); // NOI18N
		addFunds.setName("addFunds"); // NOI18N
		addFunds.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addFundsActionPerformed(evt);
			}
		});
		menuManage.add(addFunds);

		menuBar.add(menuManage);

		menuMarket.setText(resourceMap.getString("menuMarket.text")); // NOI18N
		menuMarket.setName("menuMarket"); // NOI18N

		miPurchaseUnit.setText(resourceMap.getString("miPurchaseUnit.text")); // NOI18N
		miPurchaseUnit.setName("miPurchaseUnit"); // NOI18N
		miPurchaseUnit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miPurchaseUnitActionPerformed(evt);
			}
		});
		menuMarket.add(miPurchaseUnit);

		menuHire.setText(resourceMap.getString("menuHire.text")); // NOI18N
		menuHire.setName("menuHire"); // NOI18N

		miHirePilot.setText(resourceMap.getString("miHirePilot.text")); // NOI18N
		miHirePilot.setName("miHirePilot"); // NOI18N
		miHirePilot.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miHirePilotActionPerformed(evt);
			}
		});
		menuHire.add(miHirePilot);

		miHireTech.setText(resourceMap.getString("miHireTech.text")); // NOI18N
		miHireTech.setName("miHireTech"); // NOI18N
		miHireTech.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miHireTechActionPerformed(evt);
			}
		});
		menuHire.add(miHireTech);

		miHireDoctor.setText(resourceMap.getString("miHireDoctor.text")); // NOI18N
		miHireDoctor.setName("miHireDoctor"); // NOI18N
		miHireDoctor.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miHireDoctorActionPerformed(evt);
			}
		});
		menuHire.add(miHireDoctor);
		menuMarket.add(menuHire);
		menuBar.add(menuMarket);
		helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
		helpMenu.setName("helpMenu"); // NOI18N
		aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
		aboutMenuItem.setName("aboutMenuItem"); // NOI18N
		helpMenu.add(aboutMenuItem);
		menuBar.add(helpMenu);
		statusPanel.setName("statusPanel"); // NOI18N
		statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N
		statusMessageLabel.setName("statusMessageLabel"); // NOI18N
		statusAnimationLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N
		progressBar.setName("progressBar"); // NOI18N

		org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(
				statusPanel);
		statusPanel.setLayout(statusPanelLayout);

		// Holy shit, that's a function call!!!
		statusPanelLayout
				.setHorizontalGroup(statusPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(statusPanelLayout
								.createSequentialGroup()
								.add(1104, 1104, 1104)
								.add(statusPanelLayout
										.createParallelGroup(
												org.jdesktop.layout.GroupLayout.LEADING)
										.add(statusPanelSeparator,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												164, Short.MAX_VALUE)
										.add(statusPanelLayout
												.createSequentialGroup()
												.add(statusMessageLabel)
												.addPreferredGap(
														org.jdesktop.layout.LayoutStyle.RELATED,
														org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)
												.add(progressBar,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
														org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(
														org.jdesktop.layout.LayoutStyle.RELATED)
												.add(statusAnimationLabel)
												.addContainerGap()))));
		statusPanelLayout
				.setVerticalGroup(statusPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(statusPanelLayout
								.createSequentialGroup()
								.add(statusPanelSeparator,
										org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
										2,
										org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										org.jdesktop.layout.LayoutStyle.RELATED,
										org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.add(statusPanelLayout
										.createParallelGroup(
												org.jdesktop.layout.GroupLayout.BASELINE)
										.add(statusMessageLabel)
										.add(statusAnimationLabel)
										.add(progressBar,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
								.add(3, 3, 3)));

		setComponent(mainPanel);
		setMenuBar(menuBar);
		setStatusBar(statusPanel);
	}// </editor-fold>//GEN-END:initComponents

	private void btnRetrieveUnitsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRetrieveUnitsActionPerformed
		try {
			loadListFile(false);
		}// GEN-LAST:event_btnRetrieveUnitsActionPerformed
		catch (IOException ex) {
			Logger.getLogger(MekHQView.class.getName()).log(Level.SEVERE, null,
					ex);
		}
	}

	private void btnDoTaskActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDoTaskActionPerformed
		// assign the task to the team here
		//for (int i = 0; i < selectedTasksIds.length; i++) {
		//	WorkItem task = campaign.getTask(selectedTasksIds[i]);
		WorkItem task = campaign.getTask(getSelectedTaskId());
		SupportTeam team = campaign.getTeam(currentTechId);		
	
		if ((null != task)
				&& (null != team)) {
			if(acquireSelected()) {
				campaign.getPartFor(task, team);
			} else if(repairsSelected() && (team.getTargetFor(task).getValue() != TargetRoll.IMPOSSIBLE)) {
				campaign.processTask(task, team);
			}
		}
		//}
		
		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		refreshTaskList();
		refreshAcquireList();
		refreshTechsList();
		refreshPartsList();
		refreshReport();
		refreshFunds();
	}// GEN-LAST:event_btnDoTaskActionPerformed

	private void TechTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = TechTable.getSelectedRow();
		
		if ((selected > -1) && (selected < campaign.getTechTeams().size())) {
			currentTechId = campaign.getTechTeams().get(selected).getId();
		} else if (selected < 0) {
			currentTechId = -1;
		}
		
		updateAssignEnabled();
		updateTargetText();
	}

	private void TaskTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = TaskTable.getSelectedRow();
		
		if ((selected > -1)
				&& (selected < campaign.getTasksForUnit(currentServicedUnitId).size())) {
			currentTaskId = campaign.getTasksForUnit(currentServicedUnitId)
					.get(selected).getId();
		} else {
			currentTaskId = -1;
		}

		selectedTasksIds = new int[TaskTable.getSelectedRowCount()];
		
		for (int i = 0; i < TaskTable.getSelectedRowCount(); i++) {
			int sel = TaskTable.getSelectedRows()[i];
			
			if ((sel > -1)
					&& (sel < campaign.getTasksForUnit(currentServicedUnitId).size())) {
				selectedTasksIds[i] = campaign.getTasksForUnit(currentServicedUnitId)
						.get(sel).getId();
			} else {
				selectedTasksIds[i] = -1;
			}
		}

		updateAssignEnabled();
		updateTargetText();
	}
	
	private void AcquisitionTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = AcquisitionTable.getSelectedRow();
		
		if ((selected > -1)
				&& (selected < campaign.getAcquisitionsForUnit(currentServicedUnitId).size())) {
			currentAcquisitionId = campaign.getAcquisitionsForUnit(currentServicedUnitId)
					.get(selected).getId();
		} else {
			currentAcquisitionId = -1;
		}

		updateAssignEnabled();
		updateTargetText();
	}

	private void servicedUnitTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = servicedUnitTable.getSelectedRow();
		
		if ((selected > -1) && (selected < campaign.getServiceableUnits().size())) {
			currentServicedUnitId = campaign.getServiceableUnits().get(selected).getId();
		} else if (selected < 0) {
			currentServicedUnitId = -1;
		}
		
		refreshTaskList();
		refreshAcquireList();
	}
	
	private void taskTabChanged() {
		updateAssignEnabled();
		updateTargetText();
	}

	private void patientTableValueChanged(
			javax.swing.event.ListSelectionEvent evt) {
		int selected = patientTable.getSelectedRow();
		
		if ((selected > -1) && (selected < campaign.getPatients().size())) {
			currentPatientId = campaign.getPatients().get(selected).getId();
		} else if (selected < 0) {
			currentPatientId = -1;
		}
		
		updateAssignDoctorEnabled();
	}

	private void DocTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = DocTable.getSelectedRow();
		
		if ((selected > -1) && (selected < campaign.getDoctors().size())) {
			currentDoctorId = campaign.getDoctors().get(selected).getId();
		} else if (selected < 0) {
			currentDoctorId = -1;
		}
		
		updateAssignDoctorEnabled();
	}

	private void PartsTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int selected = PartsTable.getSelectedRow();
		
		if ((selected > -1) && (selected < campaign.getParts().size())) {
			currentPartsId = campaign.getParts().get(selected).getId();
		} else if (selected < 0) {
			currentPartsId = -1;
		}
	}

	private void btnAdvanceDayActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAdvanceDayActionPerformed
		campaign.newDay();
		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		refreshTaskList();
		refreshAcquireList();
		refreshTechsList();
		refreshPatientList();
		refreshDoctorsList();
		refreshCalendar();
		refreshReport();
	}// GEN-LAST:event_btnAdvanceDayActionPerformed

	private void btnDeployUnitsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDeployUnitsActionPerformed
		deployListFile();
	}// GEN-LAST:event_btnDeployUnitsActionPerformed

	private void btnAssignDocActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAssignDocActionPerformed
		if (currentPatientId == -1) {
			return;
		}
		
		int row = patientTable.getSelectedRow();
		Person p = campaign.getPerson(currentPatientId);
		
		if ((null != p) && (null != p.getTask())) {
			p.getTask().setTeam(campaign.getTeam(currentDoctorId));
			row++;
		}
		
		refreshTechsList();
		refreshDoctorsList();
		refreshPatientList();	
		
		if (row >= patientTable.getRowCount()) {
			row = 0;
		}
		patientTable.setRowSelectionInterval(row, row);	
	}// GEN-LAST:event_btnAssignDocActionPerformed

	private void miHirePilotActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miHirePilotActionPerformed
		CustomizePilotDialog npd = new CustomizePilotDialog(getFrame(), true, 
				campaign.newPilotPerson(PilotPerson.T_MECHWARRIOR), 
				true,
				campaign,
				this);
		npd.setVisible(true);
	}// GEN-LAST:event_miHirePilotActionPerformed

	private void miHireTechActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miHireTechActionPerformed
		CustomizeSupportTeamDialog ntd = new CustomizeSupportTeamDialog(getFrame(), true, 
				campaign.newTechPerson(TechTeam.T_MECH),
				true,
				campaign, this);
		ntd.setVisible(true);
	}// GEN-LAST:event_miHireTechActionPerformed

	private void miHireDoctorActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miHireDoctorActionPerformed
		CustomizeSupportTeamDialog ntd = new CustomizeSupportTeamDialog(getFrame(), true, 
				campaign.newDoctorPerson(),
				true,
				campaign, this);
		ntd.setVisible(true);
	}// GEN-LAST:event_miHireDoctorActionPerformed

	private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuSaveActionPerformed
		MekHQApp.logMessage("Saving campaign...");
		File file = selectSaveCampaignFile(".cpn");

		if (file == null) {
			// I want a file, y'know!
			return;
		}

		FileOutputStream fos = null;
		ObjectOutputStream out = null;

		try {
			fos = new FileOutputStream(file);
			out = new ObjectOutputStream(fos);
			out.writeObject(campaign);
			out.close();
			MekHQApp.logMessage("Campaign saved to " + file);
		} catch (IOException ex) {
			MekHQApp.logError(ex);
		}
	}// GEN-LAST:event_menuSaveActionPerformed

	private void menuSaveXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuSaveActionPerformed
		MekHQApp.logMessage("Saving campaign...");
		// Choose a file...
		File file = selectSaveCampaignFile(".xml");

		if (file == null) {
			// I want a file, y'know!
			return;
		}

		// Then save it out to that file.
		FileOutputStream fos = null;
		PrintWriter pw = null;

		try {
			fos = new FileOutputStream(file);
			pw = new PrintWriter(new OutputStreamWriter(fos, "UTF8"));
			campaign.writeToXml(pw);
			pw.flush();
			pw.close();
			fos.close();
			MekHQApp.logMessage("Campaign saved to " + file);
		} catch (IOException ex) {
			MekHQApp.logError(ex);
		}
	}

	private File selectSaveCampaignFile(String fileExt) {
		JFileChooser saveCpgn = new JFileChooser(".");
		saveCpgn.setDialogTitle("Save Campaign");
		saveCpgn.setFileFilter(new ExtFileFilter(fileExt));
		saveCpgn.setSelectedFile(new File(campaign.getName()
				+ campaign.getShortDateAsString() + fileExt)); //$NON-NLS-1$
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
		MekHQApp.logMessage("Loading campaign file from XML...");

		// First select the file...
		File file = selectLoadCampaignFile(".xml");

		if(null == file) {
			return;
		}
		
		// And then load the campaign object from it.
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			Campaign tmpCampaign = Campaign
					.createCampaignFromXMLFileInputStream(fis);

			if (tmpCampaign == null) {
				// We're really expecting something here.
				// If we don't get it, fail out and do *not* replace the
				// existing one.
				return;
			}

			campaign = tmpCampaign;

			// Restores all transient attributes from serialized objects
			campaign.restore();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		changePersonnelView();
		refreshTaskList();
		refreshAcquireList();
		refreshTechsList();
		refreshPatientList();
		refreshDoctorsList();
		refreshPartsList();
		refreshCalendar();
		refreshReport();
		refreshFunds();
		refreshOrganization();

		// Without this, the report scrollbar doesn't seem to load properly
		// after loading a campaign
		Dimension size = getFrame().getSize();
		getFrame().pack();
		getFrame().setSize(size);

		MekHQApp.logMessage("Finished loading campaign!");
	}

	private void menuLoadActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuLoadActionPerformed
		MekHQApp.logMessage("Loading Campaign from binary...");

		File file = selectLoadCampaignFile(".cpn");
		FileInputStream fis = null;
		ObjectInputStream in = null;

		try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			campaign = (Campaign) in.readObject();

			// Restores all transient attributes from serialized objects
			campaign.restore();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}

		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		changePersonnelView();
		refreshTaskList();
		refreshAcquireList();
		refreshTechsList();
		refreshPatientList();
		refreshDoctorsList();
		refreshPartsList();
		refreshCalendar();
		refreshReport();
		refreshFunds();

		// Without this, the report scrollbar doesn't seem to load properly
		// after loading a campaign
		Dimension size = getFrame().getSize();
		getFrame().pack();
		getFrame().setSize(size);

		MekHQApp.logMessage("Finished loading campaign!");
	}// GEN-LAST:event_menuLoadActionPerformed

	private File selectLoadCampaignFile(String fileExt) {
		JFileChooser loadCpgn = new JFileChooser(".");
		loadCpgn.setDialogTitle("Load Campaign");
		loadCpgn.setFileFilter(new ExtFileFilter(fileExt));
		int returnVal = loadCpgn.showOpenDialog(mainPanel);

		if ((returnVal != JFileChooser.APPROVE_OPTION)
				|| (loadCpgn.getSelectedFile() == null)) {
			// I want a file, y'know!
			return null;
		}
		
		File file = loadCpgn.getSelectedFile();

		return file;
	}
	
	public static String getPersonnelGroupName(int group) {
    	switch(group) {
    	case PG_ACTIVE:
    		return "Active Personnel";
    	case PG_COMBAT:
    		return "Combat Personnel";
    	case PG_MW:
    		return "Mechwarriors";
    	case PG_CREW:
    		return "Vehicle Crews";
    	case PG_PILOT:
    		return "Aerospace Pilots";
    	case PG_PROTO:
    		return "Protomech Pilots";
    	case PG_BA:
    		return "Battle Armor Infantry";
    	case PG_SUPPORT:
    		return "Support Personnel";
    	case PG_TECH:
    		return "Techs";
    	case PG_DOC:
    		return "Doctors";
    	case PG_RETIRE:
    		return "Retired Personnel";
    	case PG_MIA:
    		return "Personnel MIA";
    	case PG_KIA:
    		return "Rolls of Honor (KIA)";
    	default:
    		return "?";
    	}
    }
	
	public static String getPersonnelViewName(int group) {
    	switch(group) {
    	case PV_GENERAL:
    		return "General";
    	case PV_COMBAT:
    		return "Combat Skills";
    	case PV_FLUFF:
    		return "Fluff Information";
    	default:
    		return "?";
    	}
    }
	
	public static String getUnitViewName(int group) {
    	switch(group) {
    	case UV_GENERAL:
    		return "General";
    	case UV_DETAILS:
    		return "Details";
    	case UV_STATUS:
    		return "Status";
    	default:
    		return "?";
    	}
    }

	private void btnOvertimeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOvertimeActionPerformed
		campaign.setOvertime(btnOvertime.isSelected());
		refreshTechsList();
		refreshTaskList();
		refreshAcquireList();
	}// GEN-LAST:event_btnOvertimeActionPerformed

	private void btnGMModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGMModeActionPerformed
		campaign.setGMMode(btnGMMode.isSelected());
	}// GEN-LAST:event_btnGMModeActionPerformed

	private void menuOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuOptionsActionPerformed
		CampaignOptionsDialog cod = new CampaignOptionsDialog(getFrame(), true,
				campaign, camos);
		cod.setVisible(true);
		refreshCalendar();
		changePersonnelView();
		refreshPersonnelList();
	}// GEN-LAST:event_menuOptionsActionPerformed

	private void miLoadForcesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miLoadForcesActionPerformed
		try {
			loadListFile(true);
		} catch (IOException ex) {
			Logger.getLogger(MekHQView.class.getName()).log(Level.SEVERE, null,
					ex);
		}
	}// GEN-LAST:event_miLoadForcesActionPerformed

	private void miPurchaseUnitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miPurchaseUnitActionPerformed
		UnitSelectorDialog usd = new UnitSelectorDialog(getFrame(), true,
				campaign, this);

		if (!campaign.isGM()) {
			usd.restrictToYear(campaign.getCalendar().get(Calendar.YEAR));
		}

		usd.setVisible(true);
		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		refreshReport();
		refreshFunds();
	}// GEN-LAST:event_miPurchaseUnitActionPerformed

	private void refreshPersonnelView() {
		int row = personnelTable.getSelectedRow();
		if(row < 0) {
			scrollPersonnelView.setViewportView(null);
			return;
		}
		Person selectedPerson = personModel.getPerson(personnelTable.convertRowIndexToModel(row));
		scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson, campaign, portraits));
		//This odd code is to make sure that the scrollbar stays at the top
		//I cant just call it here, because it ends up getting reset somewhere later
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
				scrollPersonnelView.getVerticalScrollBar().setValue(0);
			}
		});
		
	}
	
	private void refreshUnitView() {
		int row = unitTable.getSelectedRow();
		if(row < 0) {
			scrollUnitView.setViewportView(null);
			return;
		}
		Unit selectedUnit = unitModel.getUnit(unitTable.convertRowIndexToModel(row));
		scrollUnitView.setViewportView(new UnitViewPanel(selectedUnit, campaign, camos, mt));
		//This odd code is to make sure that the scrollbar stays at the top
		//I cant just call it here, because it ends up getting reset somewhere later
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
				scrollUnitView.getVerticalScrollBar().setValue(0);
			}
		});
		
	}
	
	private void PartsFilterActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_PartsFilterActionPerformed
		refreshPartsList();
	}// GEN-LAST:event_PartsFilterActionPerformed

	private void addFundsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_addFundsActionPerformed
		AddFundsDialog addFundsDialog = new AddFundsDialog(null, true);
		addFundsDialog.setVisible(true);
		long funds = addFundsDialog.getFundsQuantity();
		campaign.addFunds(funds);
		refreshReport();
		refreshFunds();
	}// GEN-LAST:event_addFundsActionPerformed

	private void btnSavePartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSavePartsActionPerformed
		Iterator<Part> itParts = campaign.getParts().iterator();
		StringBuffer stringBuffer = new StringBuffer();
		String newLine = System.getProperty("line.separator");
		
		while (itParts.hasNext()) {
			Part part = itParts.next();
			stringBuffer.append(part.getSaveString() + newLine);
		}
		
		JFileChooser jFileChooser = new JFileChooser(new File(
				System.getProperty("user.dir")));
		jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jFileChooser.setApproveButtonText("Save file");
		int retVal = jFileChooser.showSaveDialog(null);
		
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jFileChooser.getSelectedFile();
			
			try {
				FileWriter fileWriter = new FileWriter(selectedFile);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				bufferedWriter.write(stringBuffer.toString());
				bufferedWriter.close();
			} catch (IOException ex) {
				Logger.getLogger(MekHQView.class.getName()).log(
						Level.SEVERE,
						"Could not write to file "
								+ selectedFile.getAbsolutePath(), ex);
				AlertPopup alertPopup = new AlertPopup(null, true,
						"Could not write to file "
								+ selectedFile.getAbsolutePath());
				alertPopup.setVisible(true);
			}
		}
	}// GEN-LAST:event_btnSavePartsActionPerformed

	private void btnLoadPartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLoadPartsActionPerformed
		JFileChooser jFileChooser = new JFileChooser(new File(
				System.getProperty("user.dir")));
		jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jFileChooser.setApproveButtonText("Load file");
		int retVal = jFileChooser.showOpenDialog(null);
		
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jFileChooser.getSelectedFile();
			
			try {
				FileReader fileReader = new FileReader(selectedFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line = null;
				
				while ((line = bufferedReader.readLine()) != null) {
					Part part = Part.getPartByName(line);
					
					if (part != null) {
						campaign.addPart(part);
					}
				}
				refreshPartsList();
			} catch (IOException ex) {
				Logger.getLogger(MekHQView.class.getName())
						.log(Level.SEVERE,
								"Could not read file "
										+ selectedFile.getAbsolutePath(), ex);
				AlertPopup alertPopup = new AlertPopup(null, true,
						"Could not read file " + selectedFile.getAbsolutePath());
				alertPopup.setVisible(true);
			}
		}
	}// GEN-LAST:event_btnLoadPartsActionPerformed

	protected void loadListFile(boolean allowNewPilots) throws IOException {
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
			XMLStreamParser parser = new XMLStreamParser();

			// Open up the file.
			InputStream listStream = new FileInputStream(unitFile);

			// Read a Vector from the file.
			try {

				parser.parse(listStream);
				listStream.close();
			} catch (ParseException excep) {
				excep.printStackTrace(System.err);
				// throw new IOException("Unable to read from: " +
				// unitFile.getName());
			}

			// Was there any error in parsing?
			if (parser.hasWarningMessage()) {
				MekHQApp.logMessage(parser.getWarningMessage());
			}

			// Add the units from the file.
			for (Entity entity : parser.getEntities()) {
				campaign.addUnit(entity, allowNewPilots);
			}
			
			// add any ejected pilots
			for (Pilot pilot : parser.getPilots()) {
				if (pilot.isEjected()) {
					campaign.addPilot(pilot, PilotPerson.T_MECHWARRIOR, false);
				}
			}
		}
		
		refreshServicedUnitList();
		refreshUnitList();
		refreshPersonnelList();
		refreshPatientList();
		refreshReport();
	}

	protected void deployListFile() {
		if (unitTable.getSelectedRow() == -1) {
			return;
		}

		ArrayList<Entity> chosen = new ArrayList<Entity>();
		ArrayList<Unit> toDeploy = new ArrayList<Unit>();
		StringBuffer undeployed = new StringBuffer();
		
		for (int i : unitTable.getSelectedRows()) {
			Unit u = unitModel.getUnit(unitTable.convertRowIndexToModel(i));;
			
			if (null != u.getEntity()) {
				if (null == u.checkDeployment()) {
					chosen.add(u.getEntity());
					toDeploy.add(u);
				} else {
					undeployed.append("\n")
							.append(u.getEntity().getDisplayName())
							.append(" (").append(u.checkDeployment())
							.append(")");
				}
			}
		}

		JFileChooser saveList = new JFileChooser(".");
		saveList.setDialogTitle("Deploy Units");
		
		saveList.setFileFilter(new FileFilter() {
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
		
		saveList.setSelectedFile(new File(campaign.getName() + ".mul")); //$NON-NLS-1$
		int returnVal = saveList.showSaveDialog(mainPanel);
		
		if ((returnVal != JFileChooser.APPROVE_OPTION)
				|| (saveList.getSelectedFile() == null)) {
			// I want a file, y'know!
			return;
		}

		File unitFile = saveList.getSelectedFile();
		
		if (unitFile != null) {
			if (!(unitFile.getName().toLowerCase().endsWith(".mul") //$NON-NLS-1$
			|| unitFile.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
				try {
					unitFile = new File(unitFile.getCanonicalPath() + ".mul"); //$NON-NLS-1$
				} catch (IOException ie) {
					// nothing needs to be done here
					return;
				}
			}

			try {
				// Save the player's entities to the file.
				// FIXME: this is not working
				EntityListFile.saveTo(unitFile, chosen);

				// set the unit and pilot as deployed
				for (Unit u : toDeploy) {
					u.setDeployed(true);
				}

			} catch (IOException excep) {
				excep.printStackTrace(System.err);
			}
		}

		refreshServicedUnitList();
		refreshUnitList();
		refreshPatientList();
		refreshPersonnelList();

		if (undeployed.length() > 0) {
			JOptionPane.showMessageDialog(
					getFrame(),
					"The following units could not be deployed:"
							+ undeployed.toString(),
					"Could not deploy some units", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void refreshServicedUnitList() {
		int selected = servicedUnitTable.getSelectedRow();
		servicedUnitModel.setData(campaign.getServiceableUnits());
		if ((selected > -1) && (selected < campaign.getServiceableUnits().size())) {
			servicedUnitTable.setRowSelectionInterval(selected, selected);
		}
	}
	
	protected void refreshPersonnelList() {
		personModel.setData(campaign.getPersonnel());
	}
	
	protected void refreshUnitList() {
		unitModel.setData(campaign.getUnits());
	}

	protected void refreshTaskList() {
		taskModel.setData(campaign.getTasksForUnit(currentServicedUnitId));
	}
	
	protected void refreshAcquireList() {
		acquireModel.setData(campaign.getAcquisitionsForUnit(currentServicedUnitId));
	}

	protected void refreshTechsList() {
		int selected = TechTable.getSelectedRow();
		techsModel.setData(campaign.getTechTeams());
		if ((selected > -1) && (selected < campaign.getTechTeams().size())) {
			TechTable.setRowSelectionInterval(selected, selected);
		}
	}

	protected void refreshDoctorsList() {
		int selected = DocTable.getSelectedRow();
		doctorsModel.setData(campaign.getDoctors());
		if ((selected > -1) && (selected < campaign.getDoctors().size())) {
			DocTable.setRowSelectionInterval(selected, selected);
		}
	}

	protected void refreshPatientList() {
		int selected = patientTable.getSelectedRow();
		patientModel.setData(campaign.getPatients());
		if ((selected > -1) && (selected < campaign.getPatients().size())) {
			patientTable.setRowSelectionInterval(selected, selected);
		}
	}

	protected void refreshPartsList() {

		int partTypeFilter = PartsFilter.getSelectedIndex();
		ArrayList<PartInventory> partsInventory = null;
		if (partTypeFilter == 0) {
			partsInventory = campaign.getPartsInventory();
		} else {
			// -1 because "All" is appended at the begining of the list of part
			// types
			partsInventory = campaign.getPartsInventory(partTypeFilter - 1);
		}
		partsModel.setData(partsInventory);

		int selected = PartsTable.getSelectedRow();
		if ((selected > -1) && (selected < partsInventory.size())) {
			PartsTable.setRowSelectionInterval(selected, selected);
		}
	}

	protected void refreshCalendar() {
		getFrame().setTitle(campaign.getTitle());
	}

	protected void refreshReport() {
		txtPaneReport.setText(campaign.getCurrentReportHTML());
		txtPaneReport.setCaretPosition(0);
	}
	
	protected void refreshOrganization() {
		//traverse the force object and assign TreeNodes
		Force force = campaign.getForces();
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(campaign.getForces());
		Enumeration<Force> subforces = force.getSubForces().elements();
		while(subforces.hasMoreElements()) {
			Force subforce = subforces.nextElement();
			addForce(subforce, top);
		}
		if(null == orgModel) {
			orgModel = new DefaultTreeModel(top);
		} else {
			orgModel.setRoot(top);
		}
		//scrollOrgTree.setViewportView(orgTree);
		
	}
	
	private void addForce(Force force, DefaultMutableTreeNode top) {
		DefaultMutableTreeNode category = new DefaultMutableTreeNode(force);
		top.add(category);
		Enumeration<Force> subforces = force.getSubForces().elements();
		while(subforces.hasMoreElements()) {
			Force subforce = subforces.nextElement();
			addForce(subforce, category);
		}
		//add any personnel
		Enumeration<Integer> personnel = force.getPersonnel().elements();
		while(personnel.hasMoreElements()) {
			Person p = campaign.getPerson(personnel.nextElement());
			category.add(new DefaultMutableTreeNode(p));
		}
	}

	protected void refreshFunds() {
		long funds = campaign.getFunds();
		NumberFormat numberFormat = NumberFormat.getIntegerInstance();
		String text = numberFormat.format(funds) + " "
				+ (funds != 0 ? "CBills" : "CBill");
		fundsLabel.setText(text);
	}

	protected void updateAssignEnabled() {
		// must have a valid team and an unassigned task
		WorkItem curTask = campaign.getTask(getSelectedTaskId());
		SupportTeam team = campaign.getTeam(currentTechId);
		if ((null != curTask)
				&& (null != team)) {
			if(repairsSelected()) {
				btnDoTask.setEnabled(team.getTargetFor(curTask).getValue() != TargetRoll.IMPOSSIBLE);
			} else if(acquireSelected()) {
				btnDoTask.setEnabled(team.getTargetForAcquisition(curTask).getValue() != TargetRoll.IMPOSSIBLE);
			} else {
				btnDoTask.setEnabled(false);
			}
		} else {
			btnDoTask.setEnabled(false);
		}
	}

	protected void updateAssignDoctorEnabled() {
		// must have a valid doctor and an unassigned task
		Person curPerson = campaign.getPerson(currentPatientId);
		SupportTeam team = campaign.getTeam(currentDoctorId);
		PersonnelWorkItem pw = null;
		if (null != curPerson) {
			pw = curPerson.getTask();
		}
		if ((null != pw) && (null != team) && !pw.isAssigned()
				&& (team.getTargetFor(pw).getValue() != TargetRoll.IMPOSSIBLE)) {
			btnAssignDoc.setEnabled(true);
		} else {
			btnAssignDoc.setEnabled(false);
		}
	}

	protected void updateTargetText() {
		// must have a valid team and an unassigned task
		WorkItem task = campaign.getTask(getSelectedTaskId());
		SupportTeam team = campaign.getTeam(currentTechId);
		if ((null != task) && (null != team)) {
			TargetRoll target = team.getTargetFor(task);
			if(acquireSelected()) {
				target = team.getTargetForAcquisition(task);
			}
			textTarget.setText(target.getDesc());
			lblTargetNum.setText(target.getValueAsString());
		} else {
			textTarget.setText("");
			lblTargetNum.setText("-");
		}
	}
	
	void filterPersonnel() {
        RowFilter<PersonnelTableModel, Integer> personTypeFilter = null;
        final int nGroup = choicePerson.getSelectedIndex();
        personTypeFilter = new RowFilter<PersonnelTableModel,Integer>() {
        	@Override
        	public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
        		PersonnelTableModel personModel = entry.getModel();
        		Person person = personModel.getPerson(entry.getIdentifier());
        		int type = person.getType();
        		if ((nGroup == PG_ACTIVE) ||
        				(nGroup == PG_COMBAT && type <= Person.T_BA) ||
        				(nGroup == PG_SUPPORT && type > Person.T_BA) ||
        				(nGroup == PG_MW && type == Person.T_MECHWARRIOR) ||
        				(nGroup == PG_CREW && type == Person.T_VEE_CREW) ||
        				(nGroup == PG_PILOT && type == Person.T_AERO_PILOT) ||
        				(nGroup == PG_PROTO && type == Person.T_PROTO_PILOT) ||
        				(nGroup == PG_BA && type == Person.T_BA) ||
        				(nGroup == PG_TECH && type > Person.T_BA && type != Person.T_DOCTOR) ||
        				(nGroup == PG_DOC && type == Person.T_DOCTOR)) {
        			return person.isActive();
        		} else if(nGroup == PG_RETIRE) {
        			return person.getStatus() == Person.S_RETIRED;
        		} else if(nGroup == PG_MIA) {
        			return person.getStatus() == Person.S_MIA;
        		} else if(nGroup == PG_KIA) {
        			return person.getStatus() == Person.S_KIA;
        		}
        		return false;
        	}
        };
        personnelSorter.setRowFilter(personTypeFilter);
    }
	
	void filterUnits() {
		RowFilter<UnitTableModel, Integer> unitTypeFilter = null;
		final int nGroup = choiceUnit.getSelectedIndex() - 1;
        unitTypeFilter = new RowFilter<UnitTableModel,Integer>() {
        	@Override
        	public boolean include(Entry<? extends UnitTableModel, ? extends Integer> entry) {
        		if(nGroup < 0) {
        			return true;
        		}
        		UnitTableModel unitModel = entry.getModel();
        		Unit unit = unitModel.getUnit(entry.getIdentifier());
        		Entity en = unit.getEntity();
        		int type = -1;
        		if(null != en) {
        			type = UnitType.determineUnitTypeCode(en);
        		}
        		return type == nGroup;
        	}
        };
        unitSorter.setRowFilter(unitTypeFilter);
    }
	
	private void changePersonnelView() {
	
		int view = choicePersonView.getSelectedIndex();
		XTableColumnModel columnModel = (XTableColumnModel)personnelTable.getColumnModel();
		if(view == PV_GENERAL) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_PILOT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_INIT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), true);	
		} else if(view == PV_COMBAT) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_PILOT), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), campaign.getCampaignOptions().useArtillery());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), campaign.getCampaignOptions().useTactics());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_INIT), campaign.getCampaignOptions().useInitBonus());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), campaign.getCampaignOptions().useToughness());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), campaign.getCampaignOptions().useEdge());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), campaign.getCampaignOptions().useAbilities());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), campaign.getCampaignOptions().useImplants());
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);	
		} else if(view == PV_FLUFF) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_PILOT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_INIT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);	
		}
	}
	
	private void changeUnitView() {
		
		int view = choiceUnitView.getSelectedIndex();
		XTableColumnModel columnModel = (XTableColumnModel)unitTable.getColumnModel();
		if(view == PV_GENERAL) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CHASSIS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODEL), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_DEPLOY), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
		} else if(view == UV_DETAILS) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CHASSIS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODEL), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_DEPLOY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), true);
		} else if(view == UV_STATUS) {
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CHASSIS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODEL), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_DEPLOY), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), true);
			columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
		}
	}
	
	protected int getSelectedTaskId() {
		if(repairsSelected()) {
			return currentTaskId;
		} else if(acquireSelected()) {
			return currentAcquisitionId;
		} else {
			return -1;
		}
	}
	
	protected boolean repairsSelected() {
		return tabTasks.getSelectedIndex() == 0;
	}
	
	protected boolean acquireSelected() {
		return tabTasks.getSelectedIndex() == 1;
	}

	/**
	 * A table model for displaying work items
	 */
	public abstract class ArrayTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 9081706049165214129L;
		protected String[] columnNames;
		protected ArrayList<?> data;

		public int getRowCount() {
			return data.size();
		}

		public int getColumnCount() {
			return 1;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<? extends Object> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		// fill table with values
		public void setData(ArrayList<?> array) {
			data = array;
			fireTableDataChanged();
		}

	}

	/**
	 * A table model for displaying work items
	 */
	public class TaskTableModel extends ArrayTableModel {
		private static final long serialVersionUID = -6256038046416893994L;

		public TaskTableModel() {
			columnNames = new String[] { "Tasks" };
			data = new ArrayList<WorkItem>();
		}

		public Object getValueAt(int row, int col) {
			return ((WorkItem) data.get(row)).getDescHTML();
		}

		public WorkItem getTaskAt(int row) {
			return (WorkItem) data.get(row);
		}

		public WorkItem[] getTasksAt(int[] rows) {
			WorkItem[] tasks = new WorkItem[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				tasks[i] = (WorkItem) data.get(row);
			}
			return tasks;
		}

		public TaskTableModel.Renderer getRenderer() {
			return new TaskTableModel.Renderer();
		}

		public class Renderer extends TaskInfo implements TableCellRenderer {
			private static final long serialVersionUID = -3052618135259621130L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				WorkItem task = getTaskAt(row);
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				setToolTipText(task.getToolTip());
				if (isSelected) {
					select();
				} else {
					unselect();
				}

				if ((null != task) && (task instanceof ReplacementItem)
						&& !((ReplacementItem) task).hasPart()) {
					c.setBackground(Color.GRAY);
				} else if ((task != null)
						&& (task instanceof ReplacementItem)
						&& ((ReplacementItem) task).hasPart()
						&& (((ReplacementItem) task).partNeeded() instanceof GenericSparePart)
						&& (!((ReplacementItem) task)
								.hasEnoughGenericSpareParts())) {
					c.setBackground(Color.GRAY);
				} else {
					c.setBackground(new Color(220, 220, 220));
				}
				return c;
			}

		}
	}

	public class TaskTableMouseAdapter extends MouseInputAdapter implements
			ActionListener {

		public void actionPerformed(ActionEvent action) {
			String command = action.getActionCommand();
			WorkItem[] tasks = taskModel
					.getTasksAt(TaskTable.getSelectedRows());
			if (command.equalsIgnoreCase("REPLACE")) {
				for (WorkItem task : tasks) {
					if (task instanceof RepairItem) {
						if (((RepairItem) task).canScrap()) {
							campaign.mutateTask(task,
									((RepairItem) task).replace());
						}
					} else if (task instanceof ReplacementItem) {
						((ReplacementItem) task).useUpPart();
						task.setSkillMin(SupportTeam.EXP_GREEN);
					} else if (task instanceof SalvageItem) {
						SalvageItem salvage = (SalvageItem) task;
						salvage.scrap();
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshTaskList();
				refreshUnitView();
				refreshAcquireList();
			} else if (command.contains("SWAP_AMMO")) {
				WorkItem task = taskModel.getTaskAt(TaskTable.getSelectedRow());
				if (task instanceof ReloadItem) {
					ReloadItem reload = (ReloadItem) task;
					Entity en = reload.getUnit().getEntity();
					Mounted m = reload.getMounted();
					if (null == m) {
						return;
					}
					AmmoType curType = (AmmoType) m.getType();
					String sel = command.split(":")[1];
					int selType = Integer.parseInt(sel);
					AmmoType newType = Utilities.getMunitionsFor(en, curType)
							.get(selType);
					reload.swapAmmo(newType);
					refreshTaskList();
					refreshUnitView();
					refreshAcquireList();
				}
			} else if (command.contains("CHANGE_MODE")) {
				for (WorkItem task : tasks) {
					String sel = command.split(":")[1];
					int selected = Integer.parseInt(sel);
					task.setMode(selected);
					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshUnitView();
					refreshAcquireList();
				}
			} else if (command.contains("UNASSIGN")) {
				for (WorkItem task : tasks) {
					task.resetTimeSpent();
					task.setTeam(null);
					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
				}
			} else if (command.contains("FIX")) {
				for (WorkItem task : tasks) {
					if (task.checkFixable() == null) {
						if ((task instanceof ReplacementItem)
								&& !((ReplacementItem) task).hasPart()) {
							ReplacementItem replace = (ReplacementItem) task;
							Part part = replace.partNeeded();
							replace.setPart(part);
							campaign.addPart(part);
						}
						task.succeed();
						if (task.isCompleted()) {
							campaign.removeTask(task);
						}
					}
					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
					refreshPartsList();
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();
			if (e.isPopupTrigger()) {
				int row = TaskTable.rowAtPoint(e.getPoint());
				WorkItem task = taskModel.getTaskAt(row);
				JMenuItem menuItem = null;
				JMenu menu = null;
				JCheckBoxMenuItem cbMenuItem = null;
				// **lets fill the pop up menu**//
				if ((task instanceof RepairItem)
						|| (task instanceof ReplacementItem)
						|| (task instanceof SalvageItem)) {
					// Mode (extra time, rush job, ...
					menu = new JMenu("Mode");
					for (int i = 0; i < WorkItem.MODE_N; i++) {
						cbMenuItem = new JCheckBoxMenuItem(
								WorkItem.getModeName(i));
						if (task.getMode() == i) {
							cbMenuItem.setSelected(true);
						} else {
							cbMenuItem.setActionCommand("CHANGE_MODE:" + i);
							cbMenuItem.addActionListener(this);
						}
						menu.add(cbMenuItem);
					}
					popup.add(menu);
					// Scrap component
					menuItem = new JMenuItem("Scrap component");
					menuItem.setActionCommand("REPLACE");
					menuItem.addActionListener(this);

					// Everything needs to be scrapable
					// menuItem.setEnabled(((UnitWorkItem)task).canScrap());
					menuItem.setEnabled(true);
					popup.add(menuItem);
					// Remove assigned team for scheduled tasks
					menuItem = new JMenuItem("Remove Assigned Team");
					menuItem.setActionCommand("UNASSIGN");
					menuItem.addActionListener(this);
					menuItem.setEnabled(task.isAssigned());
					popup.add(menuItem);
				}
				if (task instanceof ReloadItem) {
					ReloadItem reload = (ReloadItem) task;
					Entity en = reload.getUnit().getEntity();
					Mounted m = reload.getMounted();
					// Swap ammo
					menu = new JMenu("Swap Ammo");
					int i = 0;
					AmmoType curType = (AmmoType) m.getType();
					for (AmmoType atype : Utilities
							.getMunitionsFor(en, curType)) {
						cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
						if (atype.equals(curType)) {
							cbMenuItem.setSelected(true);
						} else {
							cbMenuItem.setActionCommand("SWAP_AMMO:" + i);
							cbMenuItem.addActionListener(this);
						}
						menu.add(cbMenuItem);
						i++;
					}
					popup.add(menu);
				}
				menu = new JMenu("GM Mode");
				popup.add(menu);
				// Auto complete task
				menuItem = new JMenuItem("Complete Task");
				menuItem.setActionCommand("FIX");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM()
						&& (null == task.checkFixable()));
				menu.add(menuItem);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	

	/**
	 * A table model for displaying work items
	 */
	public class AcquisitionTableModel extends ArrayTableModel {
		private static final long serialVersionUID = -6256038046416893994L;

		public AcquisitionTableModel() {
			columnNames = new String[] { "Parts Needed" };
			data = new ArrayList<WorkItem>();
		}

		public Object getValueAt(int row, int col) {
			return ((WorkItem) data.get(row)).getPartDescHTML();
		}

		public WorkItem getTaskAt(int row) {
			return (WorkItem) data.get(row);
		}

		public WorkItem[] getTasksAt(int[] rows) {
			WorkItem[] tasks = new WorkItem[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				tasks[i] = (WorkItem) data.get(row);
			}
			return tasks;
		}

		public AcquisitionTableModel.Renderer getRenderer() {
			return new AcquisitionTableModel.Renderer();
		}

		public class Renderer extends TaskInfo implements TableCellRenderer {
			private static final long serialVersionUID = -3052618135259621130L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				WorkItem task = getTaskAt(row);
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				setToolTipText(task.getToolTip());
				if (isSelected) {
					select();
				} else {
					unselect();
				}

				c.setBackground(new Color(220, 220, 220));
				return c;
			}

		}
	}

	/**
	 * A table model for displaying units that are being serviced in the repair bay
	 */
	public class ServicedUnitTableModel extends ArrayTableModel {
		private static final long serialVersionUID = 3314061779690077204L;

		public ServicedUnitTableModel() {
			columnNames = new String[] { "Units" };
			data = new ArrayList<Unit>();
		}

		public Object getValueAt(int row, int col) {
			return campaign.getUnitDesc(((Unit) data.get(row)).getId());
		}

		public Unit getUnitAt(int row) {
			return (Unit) data.get(row);
		}

		public Unit[] getUnitsAt(int[] rows) {
			Unit[] units = new Unit[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				units[i] = (Unit) data.get(row);
			}
			return units;
		}

		public ServicedUnitTableModel.Renderer getRenderer() {
			return new ServicedUnitTableModel.Renderer(camos, mt);
		}

		public class Renderer extends MekInfo implements TableCellRenderer {
			
			public Renderer(DirectoryItems camo, MechTileset mt) {
				super(camo, mt);
				// TODO Auto-generated constructor stub
			}

			private static final long serialVersionUID = 6767431355690868748L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				Unit u = getUnitAt(row);
				setOpaque(true);
				setUnit(u);
				setText(getValueAt(row, column).toString());
				setToolTipText(campaign.getTaskListFor(u));
				if (isSelected) {
					select();
				} else {
					unselect();
				}
	
				c.setBackground(new Color(220, 220, 220));
				return c;
			}

		}
	}

	public class ServicedUnitsTableMouseAdapter extends MouseInputAdapter implements
			ActionListener {

		public void actionPerformed(ActionEvent action) {
			String command = action.getActionCommand();
			Unit selectedUnit = servicedUnitModel.getUnitAt(servicedUnitTable.getSelectedRow());
			Unit[] units = servicedUnitModel.getUnitsAt(servicedUnitTable.getSelectedRows());
			if (command.contains("ASSIGN_TECH")) {
				String sel = command.split(":")[1];
				int selected = Integer.parseInt(sel);
				if ((selected > -1)
						&& (selected < campaign.getTechTeams().size())) {
					SupportTeam team = campaign.getTechTeams().get(selected);
					if (null != team) {
						for (WorkItem task : campaign
								.getTasksForUnit(selectedUnit.getId())) {
							if (team.getTargetFor(task).getValue() != TargetRoll.IMPOSSIBLE) {
								campaign.processTask(task, team);
							}
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshTaskList();
				refreshAcquireList();
				refreshTechsList();
				refreshReport();
				refreshPartsList();
			} else if (command.contains("SWAP_AMMO")) {
				String sel = command.split(":")[1];
				int selMount = Integer.parseInt(sel);
				Mounted m = selectedUnit.getEntity().getEquipment(selMount);
				if (null == m) {
					return;
				}
				AmmoType curType = (AmmoType) m.getType();
				ReloadItem reload = campaign.getReloadWorkFor(m, selectedUnit);
				boolean newWork = false;
				if (null == reload) {
					newWork = true;
					reload = new ReloadItem(selectedUnit, m);
				}
				sel = command.split(":")[2];
				int selType = Integer.parseInt(sel);
				AmmoType newType = Utilities.getMunitionsFor(
						selectedUnit.getEntity(), curType).get(selType);
				reload.swapAmmo(newType);
				if (newWork) {
					campaign.addWork(reload);
				}
				refreshTaskList();
				refreshAcquireList();
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.contains("CHANGE_SITE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						String sel = command.split(":")[1];
						int selected = Integer.parseInt(sel);
						if ((selected > -1) && (selected < Unit.SITE_N)) {
							unit.setSite(selected);
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshTaskList();
				refreshAcquireList();
			} else if (command.equalsIgnoreCase("SALVAGE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						unit.setSalvage(true);
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.equalsIgnoreCase("REPAIR")) {
				for (Unit unit : units) {
					if (!unit.isDeployed() && unit.isRepairable()) {
						unit.setSalvage(false);
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.equalsIgnoreCase("REMOVE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						if (0 == JOptionPane.showConfirmDialog(null,
								"Do you really want to remove "
										+ unit.getEntity().getDisplayName()
										+ "?", "Remove Unit?",
								JOptionPane.YES_NO_OPTION)) {
							campaign.removeUnit(unit.getId());
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshReport();
			} else if (command.equalsIgnoreCase("UNDEPLOY")) {
				for (Unit unit : units) {
					if (unit.isDeployed()) {
						unit.setDeployed(false);
						if (null != unit.getPilot()) {
							unit.getPilot().setDeployed(false);
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.contains("CUSTOMIZE")
					&& !command.contains("CANCEL")) {
				if (!selectedUnit.isDeployed() && !selectedUnit.isDamaged()) {
					Entity targetEntity = null;
					String targetMechName = command.split(":")[1];
					if (targetMechName.equals("MML")) {
						if (selectedUnit.getEntity() instanceof megamek.common.Mech) {
							MechSummary mechSummary = MechSummaryCache
									.getInstance().getMech(
											selectedUnit.getEntity()
													.getShortName());
							megamek.common.Mech selectedMech = null;

							try {
								Entity e = (new MechFileParser(
										mechSummary.getSourceFile()))
										.getEntity();
								if (e instanceof megamek.common.Mech) {
									selectedMech = (megamek.common.Mech) e;
								}
							} catch (EntityLoadingException ex) {
								Logger.getLogger(MekHQView.class.getName())
										.log(Level.SEVERE, null, ex);
							}

							if (selectedMech == null) {
								return;
							}

							String modelTmp = "CST01";
							selectedMech.setModel(modelTmp);

							MMLMekUICustom megamekLabMekUI = new MMLMekUICustom();
							megamekLabMekUI.setVisible(false);
							megamekLabMekUI.setModal(true);

							megamekLabMekUI.loadUnit(selectedMech);
							megamekLabMekUI.setVisible(true);

							megamek.common.Mech mmlEntity = megamekLabMekUI
									.getEntity();
							if (MMLMekUICustom.isEntityValid(mmlEntity)
									&& mmlEntity.getChassis().equals(
											selectedMech.getChassis())
									&& (mmlEntity.getWeight() == selectedMech
											.getWeight())) {
								targetEntity = mmlEntity;
							}
						}

					} else if (targetMechName.equals("CHOOSE_VARIANT")) {
						UnitSelectorDialog usd = new UnitSelectorDialog(null,
								true, campaign, null);
						usd.restrictToChassis(selectedUnit.getEntity()
								.getChassis());
						usd.getComboUnitType().setSelectedIndex(UnitType.MEK);
						usd.getComboType()
								.setSelectedIndex(TechConstants.T_ALL);
						usd.getComboWeight().setSelectedIndex(
								selectedUnit.getEntity().getWeightClass());
						usd.changeBuyBtnToSelectBtn();

						if (!campaign.isGM()) {
							usd.restrictToYear(campaign.getCalendar().get(
									Calendar.YEAR));
						}

						usd.setVisible(true);

						megamek.common.Mech selectedMech = null;

						MechSummary mechSummary = MechSummaryCache
								.getInstance()
								.getMech(
										selectedUnit.getEntity().getShortName());
						try {
							Entity e = (new MechFileParser(
									mechSummary.getSourceFile())).getEntity();
							if (e instanceof megamek.common.Mech) {
								selectedMech = (megamek.common.Mech) e;
							}
						} catch (EntityLoadingException ex) {
							Logger.getLogger(MekHQView.class.getName()).log(
									Level.SEVERE, null, ex);
						}

						if (selectedMech == null) {
							return;
						}

						Entity chosenTarget = usd.getSelectedEntity();
						if ((chosenTarget instanceof megamek.common.Mech)
								&& chosenTarget.getChassis().equals(
										selectedMech.getChassis())
								&& (chosenTarget.getWeight() == selectedMech
										.getWeight())) {
							targetEntity = chosenTarget;
						}

					}

					if (targetEntity != null) {
						selectedUnit.setCustomized(true);
						selectedUnit.customize(targetEntity, campaign);
					}

					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
				}
			} else if (command.contains("CANCEL_CUSTOMIZE")) {
				if (selectedUnit.isCustomized()) {
					selectedUnit.setCustomized(false);
					selectedUnit.cancelCustomize(campaign);

					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {

			if (e.getClickCount() == 2) {
				int row = servicedUnitTable.rowAtPoint(e.getPoint());
				Unit unit = servicedUnitModel.getUnitAt(row);
				if (null != unit) {
					MechView mv = new MechView(unit.getEntity(), false);
					MekViewDialog mvd = new MekViewDialog(null, true, mv);
					mvd.setVisible(true);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();
			if (e.isPopupTrigger()) {
				int row = servicedUnitTable.rowAtPoint(e.getPoint());
				Unit unit = servicedUnitModel.getUnitAt(row);
				JMenuItem menuItem = null;
				JMenu menu = null;
				JCheckBoxMenuItem cbMenuItem = null;
				// **lets fill the pop up menu**//
				// change the location
				menu = new JMenu("Change site");
				int i = 0;
				for (i = 0; i < Unit.SITE_N; i++) {
					cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
					if (unit.getSite() == i) {
						cbMenuItem.setSelected(true);
					} else {
						cbMenuItem.setActionCommand("CHANGE_SITE:" + i);
						cbMenuItem.addActionListener(this);
					}
					menu.add(cbMenuItem);
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				// assign all tasks to a certain tech
				menu = new JMenu("Assign all tasks");
				i = 0;
				for (SupportTeam tech : campaign.getTechTeams()) {
					menuItem = new JMenuItem(tech.getDesc());
					menuItem.setActionCommand("ASSIGN_TECH:" + i);
					menuItem.addActionListener(this);
					menuItem.setEnabled(tech.getMinutesLeft() > 0);
					menu.add(menuItem);
					i++;
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				// swap ammo
				menu = new JMenu("Swap ammo");
				JMenu ammoMenu = null;
				for (Mounted m : unit.getEntity().getAmmo()) {
					ammoMenu = new JMenu(m.getDesc());
					i = 0;
					AmmoType curType = (AmmoType) m.getType();
					for (AmmoType atype : Utilities.getMunitionsFor(
							unit.getEntity(), curType)) {
						cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
						if (atype.equals(curType)) {
							cbMenuItem.setSelected(true);
						} else {
							cbMenuItem.setActionCommand("SWAP_AMMO:"
									+ unit.getEntity().getEquipmentNum(m) + ":"
									+ i);
							cbMenuItem.addActionListener(this);
						}
						ammoMenu.add(cbMenuItem);
						i++;
					}
					menu.add(ammoMenu);
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				// Salvage / Repair
				if (unit.isSalvage()) {
					menuItem = new JMenuItem("Repair");
					menuItem.setActionCommand("REPAIR");
					menuItem.addActionListener(this);
					menuItem.setEnabled(!unit.isDeployed()
							&& unit.isRepairable() && !unit.isCustomized());
					popup.add(menuItem);
				} else if (!unit.isSalvage()) {
					menuItem = new JMenuItem("Salvage");
					menuItem.setActionCommand("SALVAGE");
					menuItem.addActionListener(this);
					menuItem.setEnabled(!unit.isDeployed()
							&& !unit.isCustomized());
					popup.add(menuItem);
				}
				// Customize
				if (!unit.isCustomized()) {
					menu = new JMenu("Customize");

					menuItem = new JMenuItem("To existing variant");
					menuItem.setActionCommand("CUSTOMIZE:" + "CHOOSE_VARIANT");
					menuItem.addActionListener(this);
					menu.add(menuItem);

					menuItem = new JMenuItem("MegaMekLab");
					menuItem.setActionCommand("CUSTOMIZE:" + "MML");
					menuItem.addActionListener(this);
					menu.add(menuItem);

					menu.setEnabled(!unit.isDeployed()
							&& !unit.isDamaged()
							&& (unit.getEntity() instanceof megamek.common.Mech));
					popup.add(menu);
				} else if (unit.isCustomized()) {
					menuItem = new JMenuItem("Cancel Customize");
					menuItem.setActionCommand("CANCEL_CUSTOMIZE");
					menuItem.addActionListener(this);
					menuItem.setEnabled(unit.isCustomized());
					popup.add(menuItem);
				}
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	/**
	 * A table model for displaying work items
	 */
	public class TechTableModel extends ArrayTableModel {
		private static final long serialVersionUID = 2738333372316332962L;

		public TechTableModel() {
			columnNames = new String[] { "Techs" };
			data = new ArrayList<TechTeam>();
		}

		public Object getValueAt(int row, int col) {
			return ((TechTeam) data.get(row)).getDescHTML();
		}

		public TechTeam getTechAt(int row) {
			return (TechTeam) data.get(row);
		}

		public TechTableModel.Renderer getRenderer() {
			return new TechTableModel.Renderer();
		}

		public class Renderer extends TechInfo implements TableCellRenderer {
			private static final long serialVersionUID = -4951696376098422679L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				// setToolTipText(campaign.getToolTipFor(u));
				if (isSelected) {
					select();
				} else {
					unselect();
				}
				c.setBackground(new Color(220, 220, 220));
				return c;
			}

		}
	}

	/**
	 * A table model for displaying personnel in the infirmary
	 */
	public class PatientTableModel extends ArrayTableModel {
		private static final long serialVersionUID = -1615929049408417297L;

		public PatientTableModel() {
			columnNames = new String[] { "Personnel" };
			data = new ArrayList<Person>();
		}

		public Object getValueAt(int row, int col) {
			Person psn = ((Person) data.get(row));

			return psn.getDescHTML();
		}

		public Person getPersonAt(int row) {
			return (Person) data.get(row);
		}

		public Person[] getPersonsAt(int[] rows) {
			Person[] persons = new Person[rows.length];

			for (int i = 0; i < rows.length; i++) {
				persons[i] = getPersonAt(rows[i]);
			}

			return persons;
		}

		public PatientTableModel.Renderer getRenderer() {
			return new PatientTableModel.Renderer();
		}

		public class Renderer extends PersonInfo implements TableCellRenderer {
			private static final long serialVersionUID = -406535109900807837L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				Person p = getPersonAt(row);
				setPortrait(p);
				// setToolTipText(campaign.getToolTipFor(u));

				if (isSelected) {
					select();
				} else {
					unselect();
				}

				if ((null != p) && p.isDeployed()) {
					c.setBackground(Color.GRAY);
				} else if ((null != p) && (null != p.getTask())) {
					c.setBackground(new Color(205, 92, 92));
				} else {
					c.setBackground(new Color(220, 220, 220));
				}

				return c;
			}
		}
	}

	public class OrgTreeMouseAdapter extends MouseInputAdapter implements
	ActionListener {

		@Override
		public void actionPerformed(ActionEvent action) {
			StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            int id = Integer.parseInt(st.nextToken());
            Force force = campaign.getForce(id);
            Person person = campaign.getPerson(id);
            if(command.contains("ADD_FORCE")) {
            	if(null != force) {
	            	String name = (String)JOptionPane.showInputDialog(
	                        null,
	                        "Enter the force name",
	                        "Force Name",
	                        JOptionPane.PLAIN_MESSAGE,
	                        null,
	                        null,
	                        "My Lance");
	            	campaign.addForce(new Force(name), force);
	            	refreshOrganization();
            	}
            } if(command.contains("ADD_PERSON")) {
            	if(null != force) {
                    Person p = campaign.getPerson(Integer.parseInt(st.nextToken()));
                    if(null != p && p instanceof PilotPerson) {
                    	campaign.addPersonToForce(p, force.getId());
                    	refreshOrganization();
                    }
            	}
            } else if(command.contains("CHANGE_ICON")) {
            	if(null != force) {
            		PortraitChoiceDialog pcd = new PortraitChoiceDialog(null, true,
    						force.getIconCategory(),
    						force.getIconFileName(), forceIcons);
    				pcd.setVisible(true);
    				force.setIconCategory(pcd.getCategory());
    				force.setIconFileName(pcd.getFileName());
	            	refreshOrganization();
            	}
            } else if(command.contains("CHANGE_NAME")) {
            	if(null != force) {
	            	String name = (String)JOptionPane.showInputDialog(
	                        null,
	                        "Enter the force name",
	                        "Force Name",
	                        JOptionPane.PLAIN_MESSAGE,
	                        null,
	                        null,
	                        force.getName());
	            	force.setName(name);
	            	refreshOrganization();
            	}
            } else if(command.contains("CHANGE_DESC")) {
            	if(null != force) {
            		TextAreaDialog tad = new TextAreaDialog(null, true,
    						"Edit Force Description",
    						force.getDescription());
    				tad.setVisible(true);
    				if(tad.wasChanged()) {
    					force.setDescription(tad.getText());
    					refreshOrganization();
    				}        	
            	}
            } else if(command.contains("REMOVE_FORCE")) {
            	if(null != force) {
            		campaign.removeForce(force);
            		refreshOrganization();   	
            	}
            } else if(command.contains("REMOVE_PERSON")) {
            	if(null != person) {
            		Force parentForce = campaign.getForceFor(person);
            		if(null != parentForce) {
            			campaign.RemovePersonFromForce(person);
            			refreshOrganization();   	
            		}
            	}
            }
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
				
			if (e.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu();
				JMenuItem menuItem;
				JMenu menu;
				int x = e.getX();
                int y = e.getY();
                JTree tree = (JTree)e.getSource();
                TreePath path = tree.getPathForLocation(x, y);
                if (path == null)
                        return; 
                tree.setSelectionPath(path);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                Force force = null;
                PilotPerson person = null;
                if(node.getUserObject() instanceof Force) {
                	force = (Force)node.getUserObject();
                }
                if(node.getUserObject() instanceof PilotPerson) {
                	person = (PilotPerson)node.getUserObject();
                }
                if(null != force) {
                	int forceId = force.getId();
	                menuItem = new JMenuItem("Change Name...");
	                menuItem.setActionCommand("CHANGE_NAME|" + forceId);
					menuItem.addActionListener(this);
					menuItem.setEnabled(true);
	                popup.add(menuItem);
	                menuItem = new JMenuItem("Change Description...");
	                menuItem.setActionCommand("CHANGE_DESC|" + forceId);
					menuItem.addActionListener(this);
					menuItem.setEnabled(true);
	                popup.add(menuItem);
	                menuItem = new JMenuItem("Add New Force...");
	                menuItem.setActionCommand("ADD_FORCE|" + forceId);
					menuItem.addActionListener(this);
					menuItem.setEnabled(true);
	                popup.add(menuItem);
	                menuItem = new JMenuItem("Change Force Icon...");
	                menuItem.setActionCommand("CHANGE_ICON|" + forceId);
					menuItem.addActionListener(this);
					menuItem.setEnabled(true);
	                popup.add(menuItem);
	                menu = new JMenu("Add Personnel");
	                for(Person p : campaign.getPersonnel()) {
	                	if(p instanceof PilotPerson && p.isActive() && p.getForceId() < 1) {
			                menuItem = new JMenuItem(p.getDesc());
		                	menuItem.setActionCommand("ADD_PERSON|" + forceId + "|" + p.getId());
		                	menuItem.addActionListener(this);
		                	menuItem.setEnabled(!p.isDeployed());
		                	menu.add(menuItem);
	                	}
	                }
	                popup.add(menu);    
	                menuItem = new JMenuItem("Remove Force");
	                menuItem.setActionCommand("REMOVE_FORCE|" + forceId);
					menuItem.addActionListener(this);
					menuItem.setEnabled(null != force.getParentForce());
	                popup.add(menuItem);
                }
                else if(null != person) {
                	int personId = person.getId();
                	Force parentForce = campaign.getForceFor(person);
                	if(null != parentForce) {
	                	menuItem = new JMenuItem("Remove Person from " + parentForce.getName());
		                menuItem.setActionCommand("REMOVE_PERSON|" + personId);
						menuItem.addActionListener(this);
						menuItem.setEnabled(true);
		                popup.add(menuItem);
                	}
                }
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		
	}
	
	private class ForceRenderer extends DefaultTreeCellRenderer {
     
		private static final long serialVersionUID = -553191867660269247L;

		public ForceRenderer() {
        	
        }

        public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
                     
            setIcon(getIcon(value));
            
            return this;
        }
        
        protected Icon getIcon(Object value) {
        	DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        	if(node.getUserObject() instanceof Person) {
        		return getIconFrom((Person)node.getUserObject());
        	} else if(node.getUserObject() instanceof Force) {
        		return getIconFrom((Force)node.getUserObject());
        	} else {
        		return null;
        	}
        }
        
        protected Icon getIconFrom(Person person) {
             String category = person.getPortraitCategory();
             String file = person.getPortraitFileName();

             if(Pilot.ROOT_PORTRAIT.equals(category)) {
            	 category = "";
             }

             // Return a null if the player has selected no portrait file.
             if ((null == category) || (null == file) || Pilot.PORTRAIT_NONE.equals(file)) {
            	 file = "default.gif";
             }

             // Try to get the player's portrait file.
             Image portrait = null;
             try {
            	 portrait = (Image) portraits.getItem(category, file);
            	 //make sure no images are longer than 50 pixels
            	 if(null != portrait && portrait.getHeight(this) > 50) {
            		 portrait = portrait.getScaledInstance(-1, 50, Image.SCALE_DEFAULT);               
            	 }
            	 return new ImageIcon(portrait);
             } catch (Exception err) {
            	 err.printStackTrace();
            	 return null;     	
             }
        }
        
        protected Icon getIconFrom(Force force) {
            String category = force.getIconCategory();
            String file = force.getIconFileName();

            if(Pilot.ROOT_PORTRAIT.equals(category)) {
           	 category = "";
            }

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == file) || Pilot.PORTRAIT_NONE.equals(file)) {
           	 file = "empty.png";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
           	 portrait = (Image) forceIcons.getItem(category, file);
           	 //make sure no images are longer than 50 pixels
           	 if(null != portrait && portrait.getHeight(this) > 50) {
           		 portrait = portrait.getScaledInstance(-1, 50, Image.SCALE_DEFAULT);               
           	 }
           	 return new ImageIcon(portrait);
            } catch (Exception err) {
           	 err.printStackTrace();
           	 return null;     	
            }
       }
    }	
	public class PersonnelTableMouseAdapter extends MouseInputAdapter implements
			ActionListener {
		
		private MekHQView view;
		private ArrayList<Unit> units;
		
		public PersonnelTableMouseAdapter(MekHQView view) {
			super();
			this.view = view;
		}
		
		public void actionPerformed(ActionEvent action) {
			StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
			int row = personnelTable.getSelectedRow();
			if(row < 0) {
				return;
			}
			Person selectedPerson = personModel.getPerson(personnelTable.convertRowIndexToModel(row));
			
			if(command.contains("RANK")) {
				int rank = Integer.parseInt(st.nextToken());
				selectedPerson.setRank(rank);
				refreshServicedUnitList();
				refreshUnitList();
				refreshPatientList();
				refreshPersonnelList();
				refreshTechsList();
				refreshDoctorsList();
			} else if (command.contains("CHANGE_UNIT")) {
				int selected = Integer.parseInt(st.nextToken()) - 1;
				if(selected == -1 && selectedPerson instanceof PilotPerson) {
					Unit u = ((PilotPerson)selectedPerson).getAssignedUnit();
					if(null != u) {
						u.removePilot();
					}
				} else if(null != units
						&& (selected < units.size()) 
						&& selectedPerson instanceof PilotPerson) {
					campaign.changePilot(units.get(selected), (PilotPerson)selectedPerson);
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
			} else if (command.contains("IMPROVE")) {
				int selected = Integer.parseInt(st.nextToken());
				int cost =  Integer.parseInt(st.nextToken());
				selectedPerson.improveSkill(selected);
				selectedPerson.setXp(selectedPerson.getXp() - cost);
				campaign.addReport(selectedPerson.getName() + " improved " + SkillCosts.getSkillName(selected) + "!");
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
				refreshTechsList();
				refreshDoctorsList();
				refreshReport();
			} else if (command.contains("ABILITY")) {
				String selected = st.nextToken();
				int cost =  Integer.parseInt(st.nextToken());
				if(selectedPerson instanceof PilotPerson) {
					((PilotPerson)selectedPerson).acquireAbility(PilotOptions.LVL3_ADVANTAGES, selected, true);
					selectedPerson.setXp(selectedPerson.getXp() - cost);
					//TODO: add campaign report
					refreshServicedUnitList();
					refreshUnitList();
					refreshPersonnelList();
					refreshTechsList();
					refreshDoctorsList();
					refreshReport();
				}
			} else if (command.contains("WSPECIALIST")) {
				String selected = st.nextToken();
				int cost =  Integer.parseInt(st.nextToken());
				if(selectedPerson instanceof PilotPerson) {
					((PilotPerson)selectedPerson).acquireAbility(PilotOptions.LVL3_ADVANTAGES, "weapon_specialist", selected);
					selectedPerson.setXp(selectedPerson.getXp() - cost);
					//TODO: add campaign report
					refreshServicedUnitList();
					refreshUnitList();
					refreshPersonnelList();
					refreshTechsList();
					refreshDoctorsList();
					refreshReport();
				}
			} else if (command.contains("SPECIALIST")) {
				String selected = st.nextToken();
				int cost =  Integer.parseInt(st.nextToken());
				if(selectedPerson instanceof PilotPerson) {
					((PilotPerson)selectedPerson).acquireAbility(PilotOptions.LVL3_ADVANTAGES, "specialist", selected);
					selectedPerson.setXp(selectedPerson.getXp() - cost);
					//TODO: add campaign report
					refreshServicedUnitList();
					refreshUnitList();
					refreshPersonnelList();
					refreshTechsList();
					refreshDoctorsList();
					refreshReport();
				}
			} else if (command.equalsIgnoreCase("STATUS")) {
				int selected = Integer.parseInt(st.nextToken());
				if (selected == Person.S_ACTIVE
						|| (0 == JOptionPane.showConfirmDialog(
								null,
								"Do you really want to change the status of "
								+ selectedPerson.getDesc()
								+ " to a non-active status?", "KIA?",
								JOptionPane.YES_NO_OPTION))) {
					selectedPerson.setStatus(selected);
				}
			
				refreshServicedUnitList();
				refreshUnitList();
				refreshPatientList();
				refreshPersonnelList();
				filterPersonnel();
				refreshTechsList();
				refreshDoctorsList();
				refreshReport();
			} else if (command.equalsIgnoreCase("EDGE")) {
				String trigger = st.nextToken();
				if(selectedPerson instanceof PilotPerson) {
					((PilotPerson)selectedPerson).changeEdgeTrigger(trigger);
				}
				refreshPersonnelList();
				refreshPersonnelView();
			} else if (command.equalsIgnoreCase("REMOVE")) {
				if (0 == JOptionPane
						.showConfirmDialog(
								null,
								"Do you really want to remove "
								+ selectedPerson.getDesc() + "?",
								"Remove?", JOptionPane.YES_NO_OPTION)) {
					campaign.removePerson(selectedPerson.getId());
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPatientList();
				refreshPersonnelList();
				refreshTechsList();
				refreshDoctorsList();
				refreshReport();
			} else if (command.equalsIgnoreCase("UNDEPLOY")) {
				if (selectedPerson.isDeployed()) {
					selectedPerson.setDeployed(false);
				}
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("EDIT")) {
				if(selectedPerson instanceof PilotPerson) {
					CustomizePilotDialog npd = new CustomizePilotDialog(getFrame(), true, 
							(PilotPerson)selectedPerson, 
							false,
							campaign,
							view);
					npd.setVisible(true);
				} else if(selectedPerson instanceof SupportPerson) {			
					CustomizeSupportTeamDialog ntd = new CustomizeSupportTeamDialog(getFrame(), true, 
							(SupportPerson)selectedPerson, 
							false,
							campaign,
							view);
					ntd.setVisible(true);		
				}
			} else if (command.equalsIgnoreCase("HEAL")) {
				if (selectedPerson instanceof PilotPerson) {
					Pilot pilot = ((PilotPerson) selectedPerson).getPilot();
					pilot.setHits(0);
					selectedPerson.getTask().setTeam(null);
					selectedPerson.setTask(null);
				}
				refreshPatientList();
				refreshDoctorsList();
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("PORTRAIT")) {
				PortraitChoiceDialog pcd = new PortraitChoiceDialog(null, true,
						selectedPerson.getPortraitCategory(),
						selectedPerson.getPortraitFileName(), portraits);
				pcd.setVisible(true);
				selectedPerson.setPortraitCategory(pcd.getCategory());
				selectedPerson.setPortraitFileName(pcd.getFileName());
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("BIOGRAPHY")) {
				TextAreaDialog tad = new TextAreaDialog(null, true,
						"Edit Biography",
						selectedPerson.getBiography());
				tad.setVisible(true);
				if(tad.wasChanged()) {
					selectedPerson.setBiography(tad.getText());
				}
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("XP_ADD")) {
				if (selectedPerson instanceof PilotPerson) {
					selectedPerson.setXp(selectedPerson.getXp() + 1);
				}
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("XP_SET")) {
				PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
						null, true, "XP", selectedPerson.getXp(), 0, Math.max(selectedPerson.getXp()+10,100));
				pvcd.setVisible(true);
				int i = pvcd.getValue();
				selectedPerson.setXp(i);
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.equalsIgnoreCase("EDGE_SET")) {
				if(selectedPerson instanceof PilotPerson) {
					PilotPerson pp = (PilotPerson)selectedPerson;
					PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
							null, true, "Edge", pp.getEdge(), 0, 10);
					pvcd.setVisible(true);
					int i = pvcd.getValue();
					pp.setEdge(i);
					refreshPersonnelList();
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();

			if (e.isPopupTrigger()) {
				int row = personnelTable.rowAtPoint(e.getPoint());
				Person person = personModel.getPerson(personnelTable.convertRowIndexToModel(row));
				units = campaign.getEligibleUnitsFor(person);
				JMenuItem menuItem = null;
				JMenu menu = null;
				JCheckBoxMenuItem cbMenuItem = null;
				// **lets fill the pop up menu**//
				// retire

				menu = new JMenu("Change Rank");
				int rankOrder = 0;
				for(String rank : campaign.getRanks().getAllRanks()) {
					cbMenuItem = new JCheckBoxMenuItem(rank);
					cbMenuItem.setActionCommand("RANK|" + rankOrder);
					if(person.getRank() == rankOrder) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.addActionListener(this);
					cbMenuItem.setEnabled(true);
					menu.add(cbMenuItem);
					rankOrder++;
				}
				popup.add(menu);
				menu = new JMenu("Change Status");
				for(int s = 0; s < Person.S_NUM; s++) {
					cbMenuItem = new JCheckBoxMenuItem(Person.getStatusName(s));
					if(person.getStatus() == s) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("STATUS|" + s);
					cbMenuItem.addActionListener(this);
					cbMenuItem.setEnabled(true);
					menu.add(cbMenuItem);
				}
				popup.add(menu);
				// switch pilot
				if(person instanceof PilotPerson) {
					PilotPerson pp = (PilotPerson)person;
					menu = new JMenu("Assign to Unit");
					cbMenuItem = new JCheckBoxMenuItem("None");
					if(!pp.isAssigned()) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("CHANGE_UNIT|" + 0);
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					int i = 1;
					for (Unit unit : units) {
						cbMenuItem = new JCheckBoxMenuItem(unit.getEntity().getDisplayName());
						if (unit.hasPilot()
								&& (unit.getPilot().getId() == person.getId())) {
							cbMenuItem.setSelected(true);
						}
						cbMenuItem.setActionCommand("CHANGE_UNIT|" + i);
						cbMenuItem.addActionListener(this);
						menu.add(cbMenuItem);
						i++;
					}
					menu.setEnabled(!person.isDeployed());
					popup.add(menu);
				}
				menuItem = new JMenuItem("Add XP");
				menuItem.setActionCommand("XP_ADD");
				menuItem.addActionListener(this);
				menuItem.setEnabled(true);
				popup.add(menuItem);
				menu = new JMenu("Spend XP");
				if(person instanceof PilotPerson) {
					PilotPerson pp = (PilotPerson)person;
					//Gunnery
					int cost = campaign.getSkillCosts().getCost(SkillCosts.SK_GUN, pp.getPilot().getGunnery() - 1, false);
					String costDesc = " (" + cost + "XP)";
					if(cost < 0) {
						costDesc = " (Not Possible)";
					}
					menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_GUN)   + costDesc);
					menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_GUN + "|" + cost);
					menuItem.addActionListener(this);
					menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
					menu.add(menuItem);
					//Piloting
					cost = campaign.getSkillCosts().getCost(SkillCosts.SK_PILOT, pp.getPilot().getPiloting() - 1, false);
					costDesc = " (" + cost + "XP)";
					if(cost < 0) {
						costDesc = " (Not Possible)";
					}
					menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_PILOT)   + costDesc);
					menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_PILOT + "|" + cost);
					menuItem.addActionListener(this);
					menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
					menu.add(menuItem);
					//Artillery
					if(campaign.getCampaignOptions().useArtillery()) {
						cost = campaign.getSkillCosts().getCost(SkillCosts.SK_ARTY, pp.getPilot().getArtillery() - 1, false);
						costDesc = " (" + cost + "XP)";
						if(cost < 0) {
							costDesc = " (Not Possible)";
						}
						menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_ARTY)   + costDesc);
						menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_ARTY + "|" + cost);
						menuItem.addActionListener(this);
						menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
						menu.add(menuItem);
					}
					//Tactics
					if(campaign.getCampaignOptions().useTactics()) {
						cost = campaign.getSkillCosts().getCost(SkillCosts.SK_TAC, pp.getPilot().getCommandBonus() + 1, false);
						costDesc = " (" + cost + "XP)";
						if(cost < 0) {
							costDesc = " (Not Possible)";
						}
						menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_TAC)   + costDesc);
						menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_TAC + "|" + cost);
						menuItem.addActionListener(this);
						menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
						menu.add(menuItem);
					}
					//Init Bonus
					if(campaign.getCampaignOptions().useInitBonus()) {
						cost = campaign.getSkillCosts().getCost(SkillCosts.SK_INIT, pp.getPilot().getInitBonus() + 1, false);
						costDesc = " (" + cost + "XP)";
						if(cost < 0) {
							costDesc = " (Not Possible)";
						}
						menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_INIT)   + costDesc);
						menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_INIT + "|" + cost);
						menuItem.addActionListener(this);
						menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
						menu.add(menuItem);
					}
					//Toughness
					if(campaign.getCampaignOptions().useToughness()) {
						cost = campaign.getSkillCosts().getCost(SkillCosts.SK_TOUGH, pp.getPilot().getToughness() + 1, false);
						costDesc = " (" + cost + "XP)";
						if(cost < 0) {
							costDesc = " (Not Possible)";
						}
						menuItem = new JMenuItem(SkillCosts.getSkillName(SkillCosts.SK_TOUGH)   + costDesc);
						menuItem.setActionCommand("IMPROVE|" + SkillCosts.SK_TOUGH + "|" + cost);
						menuItem.addActionListener(this);
						menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
						menu.add(menuItem);
					}
					if(campaign.getCampaignOptions().useAbilities()) {
						JMenu abMenu = new JMenu("Special Abilities");
						for (Enumeration<IOption> i = pp.getPilot().getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements();) {
				        	IOption ability = i.nextElement();
				        	if(!ability.booleanValue()) {
				        		cost = campaign.getSkillCosts().getAbilityCost(ability.getName());
				        		costDesc = " (" + cost + "XP)";
								if(cost < 0) {
									costDesc = " (Not Possible)";
								}
								if(ability.getName().equals("weapon_specialist")) {
									if(null != pp.getAssignedUnit()) {
										JMenu specialistMenu = new JMenu("Weapon Specialist");
										TreeSet<String> uniqueWeapons = new TreeSet<String>();
										for (int j = 0; j < pp.getAssignedUnit().getEntity().getWeaponList().size(); j++) {
							                Mounted m = pp.getAssignedUnit().getEntity().getWeaponList().get(j);
							                uniqueWeapons.add(m.getName());
							            }
							            for (String name : uniqueWeapons) {
							            	menuItem = new JMenuItem(name + costDesc);
											menuItem.setActionCommand("WSPECIALIST|" + name + "|" + cost);
											menuItem.addActionListener(this);
											menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
											specialistMenu.add(menuItem);
							            }
							            abMenu.add(specialistMenu);
									}
					        	} else if(ability.getName().equals("specialist")) {
				        			JMenu specialistMenu = new JMenu("Specialist");
				        			menuItem = new JMenuItem("Laser Specialist" + costDesc);
									menuItem.setActionCommand("SPECIALIST|" + Pilot.SPECIAL_LASER + "|" + cost);
									menuItem.addActionListener(this);
									menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
									specialistMenu.add(menuItem);
									menuItem = new JMenuItem("Missile Specialist" + costDesc);
									menuItem.setActionCommand("SPECIALIST|" + Pilot.SPECIAL_MISSILE + "|" + cost);
									menuItem.addActionListener(this);
									menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
									specialistMenu.add(menuItem);
									menuItem = new JMenuItem("Ballistic Specialist" + costDesc);
									menuItem.setActionCommand("SPECIALIST|" + Pilot.SPECIAL_BALLISTIC + "|" + cost);
									menuItem.addActionListener(this);
									menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
									specialistMenu.add(menuItem);
									abMenu.add(specialistMenu);
				        		} else {
									menuItem = new JMenuItem(ability.getDisplayableName() + costDesc);
									menuItem.setActionCommand("ABILITY|" + ability.getName() + "|" + cost);
									menuItem.addActionListener(this);
									menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
									abMenu.add(menuItem);
				        		}
				        	}
						}
						menu.add(abMenu);
					}
				} else if(person instanceof SupportPerson) {
					SupportTeam team = ((SupportPerson)person).getTeam();
					int type = SkillCosts.SK_TECH;
					if(team instanceof MedicalTeam) {
						type = SkillCosts.SK_MED;
					}
					int cost = campaign.getSkillCosts().getCost(type, team.getRating() + 1, true);
					String costDesc = " (" + cost + "XP)";
					if(cost < 0) {
						costDesc = " (Not Possible)";
					}
					menuItem = new JMenuItem(SkillCosts.getSkillName(type)   + costDesc);
					menuItem.setActionCommand("IMPROVE|" + type + "|" + cost);
					menuItem.addActionListener(this);
					menuItem.setEnabled(cost >= 0 && person.getXp() >= cost);
					menu.add(menuItem);
				}
				popup.add(menu);
				if(person instanceof PilotPerson && campaign.getCampaignOptions().useEdge()) {
					menu = new JMenu("Set Edge Triggers");
					cbMenuItem = new JCheckBoxMenuItem("Head Hits");
					if (((PilotPerson)person).getPilot().getOptions().booleanOption("edge_when_headhit")) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("EDGE|edge_when_headhit");
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					cbMenuItem = new JCheckBoxMenuItem("Through Armor Crits");
					if (((PilotPerson)person).getPilot().getOptions().booleanOption("edge_when_tac")) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("EDGE|edge_when_tac");
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					cbMenuItem = new JCheckBoxMenuItem("Fail KO check");
					if (((PilotPerson)person).getPilot().getOptions().booleanOption("edge_when_ko")) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("EDGE|edge_when_ko");
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					cbMenuItem = new JCheckBoxMenuItem("Ammo Explosion");
					if (((PilotPerson)person).getPilot().getOptions().booleanOption("edge_when_explosion")) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("EDGE|edge_when_explosion");
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					popup.add(menu);
				}
				// change portrait
				menuItem = new JMenuItem("Change Portrait...");
				menuItem.setActionCommand("PORTRAIT");
				menuItem.addActionListener(this);
				menuItem.setEnabled(true);
				popup.add(menuItem);	
				// change Biography
				menuItem = new JMenuItem("Change Biography...");
				menuItem.setActionCommand("BIOGRAPHY");
				menuItem.addActionListener(this);
				menuItem.setEnabled(true);
				popup.add(menuItem);
				// TODO: add quirks?
				menu = new JMenu("GM Mode");
				menuItem = new JMenuItem("Remove Person");
				menuItem.setActionCommand("REMOVE");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				menuItem = new JMenuItem("Heal Person");
				menuItem.setActionCommand("HEAL");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				if (person instanceof PilotPerson) {
					menuItem = new JMenuItem("Undeploy Pilot");
					menuItem.setActionCommand("UNDEPLOY");
					menuItem.addActionListener(this);
					menuItem.setEnabled(campaign.isGM() && person.isDeployed());
					menu.add(menuItem);
				}
				menuItem = new JMenuItem("Edit...");
				menuItem.setActionCommand("EDIT");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				menuItem = new JMenuItem("Set XP");
				menuItem.setActionCommand("XP_SET");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				if(campaign.getCampaignOptions().useEdge() && person instanceof PilotPerson) {
					menuItem = new JMenuItem("Set Edge");
					menuItem.setActionCommand("EDGE_SET");
					menuItem.addActionListener(this);
					menuItem.setEnabled(campaign.isGM());
					menu.add(menuItem);
				}
				popup.addSeparator();
				popup.add(menu);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	/**
	 * A table Model for displaying information about personnel
	 * @author Jay lawson
	 */
	public class PersonnelTableModel extends AbstractTableModel {
	
		private static final long serialVersionUID = -5207167419079014157L;
		
		private final static int COL_RANK =    0;
		private final static int COL_NAME =    1;
        private final static int COL_CALL =    2;
        private final static int COL_AGE =     3;
        private final static int COL_GENDER =  4;
        private final static int COL_TYPE =    5;
        private final static int COL_SKILL =   6;
        private final static int COL_ASSIGN =  7;
        private final static int COL_DEPLOY =  8;
        private final static int COL_GUN =     9;
        private final static int COL_PILOT =   10;
        private final static int COL_ARTY  =   11;
        private final static int COL_TACTICS = 12;
        private final static int COL_INIT    = 13;
        private final static int COL_TOUGH =   14;
        private final static int COL_EDGE  =   15;
        private final static int COL_NABIL =   16;
        private final static int COL_NIMP  =   17;
        private final static int COL_HITS  =   18;
        private final static int COL_XP =      19;
        private final static int N_COL =       20;
        
        private ArrayList<Person> data = new ArrayList<Person>();
        
        public int getRowCount() {
            return data.size();
        }

        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            	case COL_RANK:
            		return "Rank";
                case COL_NAME:
                    return "Name";
                case COL_CALL:
                    return "Callsign";
                case COL_AGE:
                    return "Age";
                case COL_GENDER:
                    return "Gender";
                case COL_TYPE:
                    return "Type";
                case COL_GUN:
                    return "Gunnery";
                case COL_PILOT:
                    return "Piloting";
                case COL_ARTY:
                    return "Artillery";
                case COL_TACTICS:
                    return "Tactics";
                case COL_INIT:
                    return "Init Bonus";
                case COL_TOUGH:
                    return "Toughness";
                case COL_SKILL:
                    return "Skill Level";
                case COL_ASSIGN:
                    return "Unit Assignment";
                case COL_EDGE:
                    return "Edge";
                case COL_NABIL:
                    return "# Abilities";
                case COL_NIMP:
                    return "# Implants";
                case COL_HITS:
                    return "Hits";
                case COL_XP:
                    return "XP";
                case COL_DEPLOY:
                    return "Deployed";
                default:
                    return "?";
            }
        }
        
        public int getColumnWidth(int c) {
            switch(c) {
            case COL_TYPE:
        	case COL_RANK:
        		return 70;
            case COL_NAME:
                return 150;
            case COL_CALL:
                return 50;        
            case COL_SKILL:
                return 100;
            case COL_GENDER:
            case COL_AGE:
            case COL_GUN:
            case COL_PILOT:
            case COL_ARTY:
            case COL_TOUGH:
            case COL_TACTICS:
            case COL_INIT:
            case COL_XP:
            case COL_EDGE:
            case COL_NABIL:
            case COL_NIMP:
            case COL_HITS:
                return 20;
            case COL_ASSIGN:
                return 125;
            default:
                return 20;
            }
        }
        
        public int getAlignment(int col) {
            switch(col) {
            case COL_AGE:
            case COL_GUN:
            case COL_PILOT:
            case COL_ARTY:
            case COL_TOUGH:
            case COL_TACTICS:
            case COL_INIT:
            case COL_XP:
            case COL_EDGE:
            case COL_NABIL:
            case COL_NIMP:
            case COL_HITS:
            case COL_DEPLOY:
            	return SwingConstants.CENTER;
            default:
            	return SwingConstants.LEFT;
            }
        }

        public String getTooltip(int row, int col) {
        	Person p = data.get(row);
        	switch(col) {
        	case COL_NABIL:
        		if(p instanceof PilotPerson) {
        			return ((PilotPerson)p).getAbilityList(PilotOptions.LVL3_ADVANTAGES);
        		}
        	case COL_NIMP:
        		if(p instanceof PilotPerson) {
        			return ((PilotPerson)p).getAbilityList(PilotOptions.MD_ADVANTAGES);
        		}
            default:
            	return null;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Person getPerson(int i) {
            return data.get(i);
        }

        //fill table with values
        public void setData(ArrayList<Person> people) {
            data = people;
            fireTableDataChanged();
        }
        
        public boolean isDeployed(int row) {
        	return getPerson(row).isDeployed();
        }

        public Object getValueAt(int row, int col) {
        	Person p;
        	if(data.isEmpty()) {
        		return "";
        	} else {
        		p = data.get(row);
        	}
            if(col == COL_RANK) {
                return campaign.getRanks().getRank(p.getRank());
            }
            if(col == COL_NAME) {
                return p.getName();
            }
            if(col == COL_CALL) {
                return p.getCallsign();
            }
            if(col == COL_GENDER) {
                return p.getGenderName();
            }
            if(col == COL_AGE) {
                return p.getAge(campaign.getCalendar());
            }
            if(col == COL_TYPE) {
                return p.getTypeDesc();
            }
            if(col == COL_GUN) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getGunnery());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_ARTY) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getArtillery());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_TACTICS) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getCommandBonus());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_INIT) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getInitBonus());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_TOUGH) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getToughness());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_PILOT) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().getPiloting());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_EDGE) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getEdge());
            	} else {
            		return "-";
            	}
            }
            if(col == COL_NABIL) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().countOptions(PilotOptions.LVL3_ADVANTAGES));
            	} else {
            		return 0;
            	}
            }
            if(col == COL_NIMP) {
            	if(p instanceof PilotPerson) {
            		return Integer.toString(((PilotPerson)p).getPilot().countOptions(PilotOptions.MD_ADVANTAGES));
            	} else {
            		return 0;
            	}
            }
            if(col == COL_HITS) {
            	if(p instanceof PilotPerson) {
            		return ((PilotPerson)p).getPilot().getHits();
            	} else {
            		return 0;
            	}
            }
            if(col == COL_SKILL) {
            	return p.getSkillSummary();
            }
            if(col == COL_ASSIGN) {
            	if(p instanceof PilotPerson) {
            		Unit u = ((PilotPerson)p).getAssignedUnit();
            		if(null != u) {
            			return u.getEntity().getDisplayName();
            		}
            	}
            	return "-";
            }
            if(col == COL_XP) {
                return p.getXp();
            }
            if(col == COL_DEPLOY) {
            	if(p.isDeployed()) {
            		return "Y";
            	} else {
            		return "N";
            	}
            }
            return "?";
        }

        public PersonnelTableModel.Renderer getRenderer() {
			return new PersonnelTableModel.Renderer();
		}

		public class Renderer extends DefaultTableCellRenderer {

			private static final long serialVersionUID = 9054581142945717303L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);
				setOpaque(true);
				int actualCol = table.convertColumnIndexToModel(column);
				int actualRow = table.convertRowIndexToModel(row);
				setHorizontalAlignment(getAlignment(actualCol));
				setToolTipText(getTooltip(actualRow, actualCol));
				
				setForeground(Color.BLACK);
				if (isSelected) {
                    setBackground(Color.DARK_GRAY);
                    setForeground(Color.WHITE);
                } else {
                    // tiger stripes
                	if (isDeployed(actualRow)) {
                		setBackground(Color.LIGHT_GRAY);
                	} else if((Integer)getValueAt(actualRow,COL_HITS) > 0) {
                		setBackground(Color.RED);
                    } else {
                        setBackground(Color.WHITE);
                    }
                }
				return this;
			}

		}
        
	}
	
	/**
	 * A comparator for ranks written as strings with "-" sorted to the bottom always
	 * @author Jay Lawson
	 *
	 */
	public class RankSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			if(s0.equals("-") && s1.equals("-")) {
				return 0;
			} else if(s0.equals("-")) {
				return 1;
			} else if(s1.equals("-")) {
				return -1;
			} else {
				//get the numbers associated with each rank string
				int r0 = campaign.getRanks().getRankOrder(s0);
				int r1 = campaign.getRanks().getRankOrder(s1);
				return ((Comparable<Integer>)r0).compareTo(r1);
			}			
		}
	}
	
	/**
	 * A comparator for skills written as strings with "-" sorted to the bottom always
	 * @author Jay Lawson
	 *
	 */
	public class SkillSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			if(s0.equals("-") && s1.equals("-")) {
				return 0;
			} else if(s0.equals("-")) {
				return 1;
			} else if(s1.equals("-")) {
				return -1;
			} else {
				return ((Comparable<String>)s0).compareTo(s1);
			}			
		}
	}
	
	/**
	 * A comparator for bonuses written as strings with "-" sorted to the bottom always
	 * @author Jay Lawson
	 *
	 */
	public class BonusSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			if(s0.equals("-") && s1.equals("-")) {
				return 0;
			} else if(s0.equals("-")) {
				return 1;
			} else if(s1.equals("-")) {
				return -1;
			} else {
				return ((Comparable<String>)s1).compareTo(s0);
			}			
		}
	}
	
	/**
	 * A comparator for skills levels (e.g. Regular, Veteran, etc)
	 * 	 * @author Jay Lawson
	 *
	 */
	public class LevelSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			if(s0.equals("-") && s1.equals("-")) {
				return 0;
			} else if(s0.equals("-")) {
				return -1;
			} else if(s1.equals("-")) {
				return 1;
			} else {
				//probably easiest to turn into numbers and then sort that way
				int l0 = 0;
				int l1 = 0;
				//TODO: use the information on skill levels in SupportPerson
				if(s0.contains("Regular")) {
					l0 = 1;
				}
				if(s1.contains("Regular")) {
					l1 = 1;
				}
				if(s0.contains("Veteran")) {
					l0 = 2;
				}
				if(s1.contains("Veteran")) {
					l1 = 2;
				}
				if(s0.contains("Elite")) {
					l0 = 3;
				}
				if(s1.contains("Elite")) {
					l1 = 3;
				}
				return ((Comparable<Integer>)l0).compareTo(l1);
			}			
		}
	}
	
	/**
	 * A comparator for unit status strings
	 * @author Jay Lawson
	 *
	 */
	public class UnitStatusSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			//probably easiest to turn into numbers and then sort that way
			int l0 = 0;
			int l1 = 0;
			if(s0.contains("Salvage")) {
				l0 = 1;
			}
			if(s1.contains("Salvage")) {
				l1 = 1;
			}
			if(s0.contains("Inoperable")) {
				l0 = 2;
			}
			if(s1.contains("Inoperable")) {
				l1 = 2;
			}
			if(s0.contains("Crippled")) {
				l0 = 3;
			}
			if(s1.contains("Crippled")) {
				l1 = 3;
			}
			if(s0.contains("Heavy")) {
				l0 = 4;
			}
			if(s1.contains("Heavy")) {
				l1 = 4;
			}
			if(s0.contains("Light")) {
				l0 = 5;
			}
			if(s1.contains("Light")) {
				l1 = 5;
			}
			if(s0.contains("Undamaged")) {
				l0 = 6;
			}
			if(s1.contains("Undamaged")) {
				l1 = 6;
			}
			return ((Comparable<Integer>)l0).compareTo(l1);		
		}
	}
	
	/**
	 * A comparator for unit weight classes
	 * @author Jay Lawson
	 *
	 */
	public class WeightClassSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			//lets find the weight class integer for each name
			int l0 = 0;
			int l1 = 0;
			for(int i = 0; i < EntityWeightClass.SIZE; i++) {
				if(EntityWeightClass.getClassName(i).equals(s0)) {
					l0 = i;
				}
				if(EntityWeightClass.getClassName(i).equals(s1)) {
					l1 = i;
				}			
			}
			return ((Comparable<Integer>)l0).compareTo(l1);		
		}
	}
	
	/**
	 * A comparator for numbers that have been formatted with DecimalFormat
	 * @author Jay Lawson
	 *
	 */
	public class FormattedNumberSorter implements Comparator<String> {

		@Override
		public int compare(String s0, String s1) {	
			//lets find the weight class integer for each name
			DecimalFormat format = new DecimalFormat();
			int l0 = 0;
			try {
				l0 = format.parse(s0).intValue();
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int l1 = 0;
			try {
				l1 = format.parse(s1).intValue();
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ((Comparable<Integer>)l0).compareTo(l1);		
		}
	}
	
	/**
	 * A table model for displaying doctors
	 */
	public class DocTableModel extends ArrayTableModel {
		private static final long serialVersionUID = -6934834363013004894L;

		public DocTableModel() {
			columnNames = new String[] { "Doctors" };
			data = new ArrayList<MedicalTeam>();
		}

		public Object getValueAt(int row, int col) {
			return ((MedicalTeam) data.get(row)).getDescHTML();
		}

		public MedicalTeam getDoctorAt(int row) {
			return (MedicalTeam) data.get(row);
		}

		public DocTableModel.Renderer getRenderer() {
			return new DocTableModel.Renderer();
		}

		public class Renderer extends DoctorInfo implements TableCellRenderer {
			private static final long serialVersionUID = -818080358678474607L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				setToolTipText(campaign.getToolTipFor(getDoctorAt(row)));
				if (isSelected) {
					select();
				} else {
					unselect();
				}
				c.setBackground(new Color(220, 220, 220));
				return c;
			}

		}
	}

	/**
	 * A table model for displaying parts
	 */
	public class PartsTableModel extends ArrayTableModel {
		private static final long serialVersionUID = 534443424190075264L;

		public PartsTableModel() {
			columnNames = new String[] { "Parts" };
			data = new ArrayList<PartInventory>();
		}

		public Object getValueAt(int row, int col) {
			PartInventory partInventory = (PartInventory) data.get(row);
			StringBuffer descHTML = new StringBuffer(
					partInventory.getDescHTML());
			return descHTML.toString();
		}

		public Part getPartAt(int row) {
			return ((PartInventory) data.get(row)).getPart();
		}

		public Part[] getPartstAt(int[] rows) {
			Part[] parts = new Part[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				parts[i] = ((PartInventory) data.get(row)).getPart();
			}
			return parts;
		}

		public PartsTableModel.Renderer getRenderer() {
			return new PartsTableModel.Renderer();
		}

		public class Renderer extends PartInfo implements TableCellRenderer {
			private static final long serialVersionUID = -167722442590291248L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = this;
				setOpaque(true);
				setText(getValueAt(row, column).toString());
				// setToolTipText(campaign.getToolTipFor(u));
				if (isSelected) {
					select();
				} else {
					unselect();
				}
				c.setBackground(new Color(220, 220, 220));
				return c;
			}

		}
	}

	public class PartsTableMouseAdapter extends MouseInputAdapter implements
			ActionListener {

		public void actionPerformed(ActionEvent action) {
			String command = action.getActionCommand();
			Part[] parts = partsModel.getPartstAt(PartsTable.getSelectedRows());
			if (command.equalsIgnoreCase("SELL")) {
				for (Part part : parts) {
					campaign.sellPart(part);
				}
				refreshPartsList();
				refreshReport();
				refreshFunds();
			} else if (command.equalsIgnoreCase("REMOVE")) {
				for (Part part : parts) {
					campaign.removePart(part);
				}
				refreshPartsList();
				refreshReport();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();
			if (e.isPopupTrigger()) {
				int row = PartsTable.rowAtPoint(e.getPoint());
				Part part = partsModel.getPartAt(row);
				JMenuItem menuItem = null;
				JMenu menu = null;
				JCheckBoxMenuItem cbMenuItem = null;
				// **lets fill the pop up menu**//
				// sell part
				menuItem = new JMenuItem("Sell Part");
				menuItem.setActionCommand("SELL");
				menuItem.addActionListener(this);
				popup.add(menuItem);
				// GM mode
				menu = new JMenu("GM Mode");
				// remove part
				menuItem = new JMenuItem("Remove Part");
				menuItem.setActionCommand("REMOVE");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				// end
				popup.addSeparator();
				popup.add(menu);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	/**
	 * A table Model for displaying information about units
	 * @author Jay lawson
	 */
	public class UnitTableModel extends AbstractTableModel {
	
		private static final long serialVersionUID = -5207167419079014157L;
		
		private final static int COL_CHASSIS =    0;
		private final static int COL_MODEL   =    1;
        private final static int COL_TYPE    =    2;
        private final static int COL_WCLASS    =  3;
        private final static int COL_TECH     =   4;
        private final static int COL_WEIGHT =     5;    
        private final static int COL_COST    =    6;
        private final static int COL_QUALITY  =   7;
        private final static int COL_STATUS   =   8;
        private final static int COL_PILOT    =   9;
        private final static int COL_DEPLOY   =   10;
        private final static int COL_BV        =  11;
        private final static int COL_REPAIR  =    12;
        private final static int COL_PARTS    =   13;
        private final static int COL_QUIRKS   =   14;
        private final static int N_COL =          15;
        
        private ArrayList<Unit> data = new ArrayList<Unit>();
        
        public int getRowCount() {
            return data.size();
        }

        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            	case COL_CHASSIS:
            		return "Chassis";
                case COL_MODEL:
                    return "Model";
                case COL_TYPE:
                    return "Type";
                case COL_WEIGHT:
                    return "Weight";
                case COL_WCLASS:
                    return "Class";
                case COL_COST:
                    return "Value";
                case COL_TECH:
                    return "Tech";
                case COL_QUALITY:
                    return "Quality";
                case COL_STATUS:
                    return "Status";
                case COL_PILOT:
                    return "Assigned to";
                case COL_BV:
                    return "BV";
                case COL_REPAIR:
                    return "# Repairs";
                case COL_PARTS:
                    return "# Parts";
                case COL_QUIRKS:
                    return "Quirks";
                case COL_DEPLOY:
                    return "Deployed";
                default:
                    return "?";
            }
        }
        
        public int getColumnWidth(int c) {
            switch(c) {
        	case COL_CHASSIS:
        		return 100;
            case COL_MODEL:
            case COL_WCLASS:
            case COL_TYPE:
            	return 50;
            case COL_COST:
            case COL_STATUS:
                return 80;        
            case COL_PILOT:          
            case COL_TECH:
            	return 150;
            default:
                return 20;
            }
        }
        
        public int getAlignment(int col) {
            switch(col) {
            case COL_QUALITY:
            case COL_QUIRKS:
            case COL_DEPLOY:
            	return SwingConstants.CENTER;
            case COL_WEIGHT:
            case COL_COST:
            case COL_REPAIR:
            case COL_PARTS:
            case COL_BV:
            	return SwingConstants.RIGHT;
            default:
            	return SwingConstants.LEFT;
            }
        }

        public String getTooltip(int row, int col) {
        	Unit u = data.get(row);
        	switch(col) {
        	case COL_REPAIR:
        		return campaign.getTaskListFor(u);
        	case COL_QUIRKS:
        		return u.getQuirksList();
            default:		
            	return null;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Unit getUnit(int i) {
            return data.get(i);
        }

        //fill table with values
        public void setData(ArrayList<Unit> units) {
            data = units;
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
        	Unit u;
        	if(data.isEmpty()) {
        		return "";
        	} else {
        		u = data.get(row);
        	}
            Entity e = u.getEntity();
            PilotPerson pp = u.getPilot();
            DecimalFormat format = new DecimalFormat();
            if(null == e) {
            	return "?";
            }
            if(col == COL_CHASSIS) {
                return e.getChassis();
            }  
            if(col == COL_MODEL) {
                return e.getModel();
            }
            if(col == COL_TYPE) {
            	return UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(e));
            }
            if(col == COL_WEIGHT) {
                return e.getWeight();
            }
            if(col == COL_WCLASS) {
                return e.getWeightClassName();
            }
            if(col == COL_COST) {
                return format.format(u.getSellValue());
            }
            if(col == COL_TECH) {
                return TechConstants.getLevelDisplayableName(e.getTechLevel());
            }
            if(col == COL_QUALITY) {
                return u.getQualityName();
            }
            if(col == COL_STATUS) {
                return u.getStatus();
            }
            if(col == COL_PILOT) {
            	if(null == pp) {
            		return "-";
            	} else {
            		return campaign.getFullTitleFor(pp);
            	}
            }
            if(col == COL_BV) {
            	if(null == pp) {
            		return e.calculateBattleValue(true, true);
            	} else {
            		return e.calculateBattleValue(true, false);
            	}
            }
            if(col == COL_REPAIR) {
                return campaign.getTotalTasksFor(u.getId());
            }
            if(col == COL_PARTS) {
                return campaign.getTotalPartsFor(u.getId());
            }
            if(col == COL_QUIRKS) {
            	return e.countQuirks();
            }
            if(col == COL_DEPLOY) {
            	if(u.isDeployed()) {
            		return "Y";
            	} else {
            		return "N";
            	}
            }
            return "?";
        }

        public UnitTableModel.Renderer getRenderer() {
			return new UnitTableModel.Renderer();
		}

		public class Renderer extends DefaultTableCellRenderer {

			private static final long serialVersionUID = 9054581142945717303L;

			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);
				setOpaque(true);
				int actualCol = table.convertColumnIndexToModel(column);
				int actualRow = table.convertRowIndexToModel(row);
				setHorizontalAlignment(getAlignment(actualCol));
				setToolTipText(getTooltip(actualRow, actualCol));
				Unit u = getUnit(actualRow);
				
				setForeground(Color.BLACK);
				if (isSelected) {
                    setBackground(Color.DARK_GRAY);
                    setForeground(Color.WHITE);
                } else {
                	if (u.isDeployed()) {
                		setBackground(Color.LIGHT_GRAY);
                	}
                	else if (null != u && !u.isRepairable()) {
    					setBackground(new Color(190, 150, 55));
    				} else if ((null != u) && !u.isFunctional()) {
    					setBackground(new Color(205, 92, 92));
    				} else if ((null != u)
    						&& (campaign.countTasksFor(u.getId()) > 0)) {
    					setBackground(new Color(238, 238, 0));
                    } else {
                        setBackground(Color.WHITE);
                    }
                }
				return this;
			}

		}
        
	}
	
	public class UnitTableMouseAdapter extends MouseInputAdapter implements
		ActionListener {
	
		private ArrayList<PilotPerson> pilots;
		
		public void actionPerformed(ActionEvent action) {
			String command = action.getActionCommand();
			Unit selectedUnit = unitModel.getUnit(unitTable.convertRowIndexToModel(unitTable.getSelectedRow()));
			int[] rows = unitTable.getSelectedRows();
			Unit[] units = new Unit[rows.length];
			for(int i=0; i<rows.length; i++) {
				units[i] = unitModel.getUnit(unitTable.convertRowIndexToModel(rows[i]));
			}
			if (command.equalsIgnoreCase("REMOVE_PILOT")) {
				for (Unit unit : units) {
					unit.removePilot();
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshOrganization();
			} else if (command.contains("CHANGE_PILOT")) {
				String sel = command.split(":")[1];
				int selected = Integer.parseInt(sel);
				if ((null != pilots) && (selected > -1)
						&& (selected < pilots.size())) {
					campaign.changePilot(selectedUnit, pilots.get(selected));
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
				refreshOrganization();
			} else if (command.equalsIgnoreCase("SELL")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						int sellValue = unit.getSellValue();
						NumberFormat numberFormat = NumberFormat
								.getIntegerInstance();
						String text = numberFormat.format(sellValue) + " "
								+ (sellValue != 0 ? "CBills" : "CBill");
						if (0 == JOptionPane.showConfirmDialog(null,
								"Do you really want to sell "
										+ unit.getEntity().getDisplayName()
										+ " for " + text, "Sell Unit?",
								JOptionPane.YES_NO_OPTION)) {
							campaign.sellUnit(unit.getId());
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
				refreshOrganization();
				refreshReport();
				refreshFunds();
			} else if (command.equalsIgnoreCase("LOSS")) {
				for (Unit unit : units) {
					if (0 == JOptionPane.showConfirmDialog(null,
							"Do you really want to consider "
									+ unit.getEntity().getDisplayName()
									+ " a combat loss?", "Remove Unit?",
							JOptionPane.YES_NO_OPTION)) {
						campaign.removeUnit(unit.getId());
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
				refreshOrganization();
				refreshReport();
			} else if (command.contains("SWAP_AMMO")) {
				String sel = command.split(":")[1];
				int selMount = Integer.parseInt(sel);
				Mounted m = selectedUnit.getEntity().getEquipment(selMount);
				if (null == m) {
					return;
				}
				AmmoType curType = (AmmoType) m.getType();
				ReloadItem reload = campaign.getReloadWorkFor(m, selectedUnit);
				boolean newWork = false;
				if (null == reload) {
					newWork = true;
					reload = new ReloadItem(selectedUnit, m);
				}
				sel = command.split(":")[2];
				int selType = Integer.parseInt(sel);
				AmmoType newType = Utilities.getMunitionsFor(
						selectedUnit.getEntity(), curType).get(selType);
				reload.swapAmmo(newType);
				if (newWork) {
					campaign.addWork(reload);
				}
				refreshTaskList();
				refreshAcquireList();
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.contains("CHANGE_SITE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						String sel = command.split(":")[1];
						int selected = Integer.parseInt(sel);
						if ((selected > -1) && (selected < Unit.SITE_N)) {
							unit.setSite(selected);
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshTaskList();
				refreshAcquireList();
			} else if (command.equalsIgnoreCase("SALVAGE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						unit.setSalvage(true);
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.equalsIgnoreCase("REPAIR")) {
				for (Unit unit : units) {
					if (!unit.isDeployed() && unit.isRepairable()) {
						unit.setSalvage(false);
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
			} else if (command.equalsIgnoreCase("REMOVE")) {
				for (Unit unit : units) {
					if (!unit.isDeployed()) {
						if (0 == JOptionPane.showConfirmDialog(null,
								"Do you really want to remove "
										+ unit.getEntity().getDisplayName()
										+ "?", "Remove Unit?",
								JOptionPane.YES_NO_OPTION)) {
							campaign.removeUnit(unit.getId());
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPersonnelList();
				refreshOrganization();
				refreshReport();
			} else if (command.equalsIgnoreCase("UNDEPLOY")) {
				for (Unit unit : units) {
					if (unit.isDeployed()) {
						unit.setDeployed(false);
						if (null != unit.getPilot()) {
							unit.getPilot().setDeployed(false);
						}
					}
				}
				refreshServicedUnitList();
				refreshUnitList();
				refreshPatientList();
				refreshPersonnelList();
			} else if (command.contains("CUSTOMIZE")
					&& !command.contains("CANCEL")) {
				if (!selectedUnit.isDeployed() && !selectedUnit.isDamaged()) {
					Entity targetEntity = null;
					String targetMechName = command.split(":")[1];
					if (targetMechName.equals("MML")) {
						if (selectedUnit.getEntity() instanceof megamek.common.Mech) {
							MechSummary mechSummary = MechSummaryCache
									.getInstance().getMech(
											selectedUnit.getEntity()
													.getShortName());
							megamek.common.Mech selectedMech = null;
		
							try {
								Entity e = (new MechFileParser(
										mechSummary.getSourceFile()))
										.getEntity();
								if (e instanceof megamek.common.Mech) {
									selectedMech = (megamek.common.Mech) e;
								}
							} catch (EntityLoadingException ex) {
								Logger.getLogger(MekHQView.class.getName())
										.log(Level.SEVERE, null, ex);
							}
		
							if (selectedMech == null) {
								return;
							}
		
							String modelTmp = "CST01";
							selectedMech.setModel(modelTmp);
		
							MMLMekUICustom megamekLabMekUI = new MMLMekUICustom();
							megamekLabMekUI.setVisible(false);
							megamekLabMekUI.setModal(true);
		
							megamekLabMekUI.loadUnit(selectedMech);
							megamekLabMekUI.setVisible(true);
		
							megamek.common.Mech mmlEntity = megamekLabMekUI
									.getEntity();
							if (MMLMekUICustom.isEntityValid(mmlEntity)
									&& mmlEntity.getChassis().equals(
											selectedMech.getChassis())
									&& (mmlEntity.getWeight() == selectedMech
											.getWeight())) {
								targetEntity = mmlEntity;
							}
						}
		
					} else if (targetMechName.equals("CHOOSE_VARIANT")) {
						UnitSelectorDialog usd = new UnitSelectorDialog(null,
								true, campaign, null);
						usd.restrictToChassis(selectedUnit.getEntity()
								.getChassis());
						usd.getComboUnitType().setSelectedIndex(UnitType.MEK);
						usd.getComboType()
								.setSelectedIndex(TechConstants.T_ALL);
						usd.getComboWeight().setSelectedIndex(
								selectedUnit.getEntity().getWeightClass());
						usd.changeBuyBtnToSelectBtn();
		
						if (!campaign.isGM()) {
							usd.restrictToYear(campaign.getCalendar().get(
									Calendar.YEAR));
						}
		
						usd.setVisible(true);
		
						megamek.common.Mech selectedMech = null;
		
						MechSummary mechSummary = MechSummaryCache
								.getInstance()
								.getMech(
										selectedUnit.getEntity().getShortName());
						try {
							Entity e = (new MechFileParser(
									mechSummary.getSourceFile())).getEntity();
							if (e instanceof megamek.common.Mech) {
								selectedMech = (megamek.common.Mech) e;
							}
						} catch (EntityLoadingException ex) {
							Logger.getLogger(MekHQView.class.getName()).log(
									Level.SEVERE, null, ex);
						}
		
						if (selectedMech == null) {
							return;
						}
		
						Entity chosenTarget = usd.getSelectedEntity();
						if ((chosenTarget instanceof megamek.common.Mech)
								&& chosenTarget.getChassis().equals(
										selectedMech.getChassis())
								&& (chosenTarget.getWeight() == selectedMech
										.getWeight())) {
							targetEntity = chosenTarget;
						}
		
					}
		
					if (targetEntity != null) {
						selectedUnit.setCustomized(true);
						selectedUnit.customize(targetEntity, campaign);
					}
		
					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
				}
			} else if (command.contains("CANCEL_CUSTOMIZE")) {
				if (selectedUnit.isCustomized()) {
					selectedUnit.setCustomized(false);
					selectedUnit.cancelCustomize(campaign);
		
					refreshServicedUnitList();
					refreshUnitList();
					refreshTaskList();
					refreshAcquireList();
				}
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();
			if (e.isPopupTrigger()) {
				int row = unitTable.rowAtPoint(e.getPoint());
				Unit unit = unitModel.getUnit(unitTable.convertRowIndexToModel(row));
				pilots = campaign.getEligiblePilotsFor(unit);
				JMenuItem menuItem = null;
				JMenu menu = null;
				JCheckBoxMenuItem cbMenuItem = null;
				// **lets fill the pop up menu**//
				// change the location
				menu = new JMenu("Change site");
				int i = 0;
				for (i = 0; i < Unit.SITE_N; i++) {
					cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
					if (unit.getSite() == i) {
						cbMenuItem.setSelected(true);
					} else {
						cbMenuItem.setActionCommand("CHANGE_SITE:" + i);
						cbMenuItem.addActionListener(this);
					}
					menu.add(cbMenuItem);
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				// swap ammo
				menu = new JMenu("Swap ammo");
				JMenu ammoMenu = null;
				for (Mounted m : unit.getEntity().getAmmo()) {
					ammoMenu = new JMenu(m.getDesc());
					i = 0;
					AmmoType curType = (AmmoType) m.getType();
					for (AmmoType atype : Utilities.getMunitionsFor(
							unit.getEntity(), curType)) {
						cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
						if (atype.equals(curType)) {
							cbMenuItem.setSelected(true);
						} else {
							cbMenuItem.setActionCommand("SWAP_AMMO:"
									+ unit.getEntity().getEquipmentNum(m) + ":"
									+ i);
							cbMenuItem.addActionListener(this);
						}
						ammoMenu.add(cbMenuItem);
						i++;
					}
					menu.add(ammoMenu);
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				// Salvage / Repair
				if (unit.isSalvage()) {
					menuItem = new JMenuItem("Repair");
					menuItem.setActionCommand("REPAIR");
					menuItem.addActionListener(this);
					menuItem.setEnabled(!unit.isDeployed()
							&& unit.isRepairable() && !unit.isCustomized());
					popup.add(menuItem);
				} else if (!unit.isSalvage()) {
					menuItem = new JMenuItem("Salvage");
					menuItem.setActionCommand("SALVAGE");
					menuItem.addActionListener(this);
					menuItem.setEnabled(!unit.isDeployed()
							&& !unit.isCustomized());
					popup.add(menuItem);
				}
				// Customize
				if (!unit.isCustomized()) {
					menu = new JMenu("Customize");
		
					menuItem = new JMenuItem("To existing variant");
					menuItem.setActionCommand("CUSTOMIZE:" + "CHOOSE_VARIANT");
					menuItem.addActionListener(this);
					menu.add(menuItem);
		
					menuItem = new JMenuItem("MegaMekLab");
					menuItem.setActionCommand("CUSTOMIZE:" + "MML");
					menuItem.addActionListener(this);
					menu.add(menuItem);
		
					menu.setEnabled(!unit.isDeployed()
							&& !unit.isDamaged()
							&& (unit.getEntity() instanceof megamek.common.Mech));
					popup.add(menu);
				} else if (unit.isCustomized()) {
					menuItem = new JMenuItem("Cancel Customize");
					menuItem.setActionCommand("CANCEL_CUSTOMIZE");
					menuItem.addActionListener(this);
					menuItem.setEnabled(unit.isCustomized());
					popup.add(menuItem);
				}
				// remove pilot
				popup.addSeparator();
				menuItem = new JMenuItem("Remove pilot");
				menuItem.setActionCommand("REMOVE_PILOT");
				menuItem.addActionListener(this);
				menuItem.setEnabled(unit.hasPilot() && !unit.isDeployed());
				popup.add(menuItem);
				// switch pilot
				menu = new JMenu("Change pilot");
				i = 0;
				for (PilotPerson pp : pilots) {
					cbMenuItem = new JCheckBoxMenuItem(pp.getDesc());
					if (unit.hasPilot()
							&& (unit.getPilot().getId() == pp.getId())) {
						cbMenuItem.setSelected(true);
					}
					cbMenuItem.setActionCommand("CHANGE_PILOT:" + i);
					cbMenuItem.addActionListener(this);
					menu.add(cbMenuItem);
					i++;
				}
				menu.setEnabled(!unit.isDeployed());
				popup.add(menu);
				popup.addSeparator();
				// sell unit
				menuItem = new JMenuItem("Sell Unit");
				menuItem.setActionCommand("SELL");
				menuItem.addActionListener(this);
				menuItem.setEnabled(!unit.isDeployed());
				popup.add(menuItem);
				// TODO: scrap unit
				// combat loss
				menuItem = new JMenuItem("Combat Loss");
				menuItem.setActionCommand("LOSS");
				menuItem.addActionListener(this);
				menuItem.setEnabled(unit.isDeployed());
				popup.add(menuItem);
				// TODO: add quirks?
				// GM mode
				menu = new JMenu("GM Mode");
				menuItem = new JMenuItem("Remove Unit");
				menuItem.setActionCommand("REMOVE");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM());
				menu.add(menuItem);
				menuItem = new JMenuItem("Undeploy Unit");
				menuItem.setActionCommand("UNDEPLOY");
				menuItem.addActionListener(this);
				menuItem.setEnabled(campaign.isGM() && unit.isDeployed());
				menu.add(menuItem);
				popup.addSeparator();
				popup.add(menu);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	/**
	 * <code>XTableColumnModel</code> extends the DefaultTableColumnModel .
	 * It provides a comfortable way to hide/show columns.
	 * Columns keep their positions when hidden and shown again.
	 *
	 * In order to work with JTable it cannot add any events to <code>TableColumnModelListener</code>.
	 * Therefore hiding a column will result in <code>columnRemoved</code> event and showing it
	 * again will notify listeners of a <code>columnAdded</code>, and possibly a <code>columnMoved</code> event.
	 * For the same reason the following methods still deal with visible columns only:
	 * getColumnCount(), getColumns(), getColumnIndex(), getColumn()
	 * There are overloaded versions of these methods that take a parameter <code>onlyVisible</code> which let's
	 * you specify wether you want invisible columns taken into account.
	 *
	 * @version 0.9 04/03/01
	 * @author Stephen Kelvin, mail@StephenKelvin.de
	 * @see DefaultTableColumnModel
	 */
	public class XTableColumnModel extends DefaultTableColumnModel {
	    /** Array of TableColumn objects in this model.
	     *  Holds all column objects, regardless of their visibility
	     */
	    protected Vector allTableColumns = new Vector();
	    
	    /**
	     * Creates an extended table column model.
	     */
	    XTableColumnModel() {
	    }
	    
	    /**
	     * Sets the visibility of the specified TableColumn.
	     * The call is ignored if the TableColumn is not found in this column model
	     * or its visibility status did not change.
	     * <p>
	     *
	     * @param aColumn        the column to show/hide
	     * @param visible its new visibility status
	 */
	    // listeners will receive columnAdded()/columnRemoved() event
	    public void setColumnVisible(TableColumn column, boolean visible) {
	        if(!visible) {
	            super.removeColumn(column);
	        }
	        else {
	            // find the visible index of the column:
	            // iterate through both collections of visible and all columns, counting
	            // visible columns up to the one that's about to be shown again
	            int noVisibleColumns    = tableColumns.size();
	            int noInvisibleColumns  = allTableColumns.size();
	            int visibleIndex        = 0;
	            
	            for(int invisibleIndex = 0; invisibleIndex < noInvisibleColumns; ++invisibleIndex) {
	                TableColumn visibleColumn   = (visibleIndex < noVisibleColumns ? (TableColumn)tableColumns.get(visibleIndex) : null);
	                TableColumn testColumn      = (TableColumn)allTableColumns.get(invisibleIndex);
	                
	                if(testColumn == column) {
	                    if(visibleColumn != column) {
	                        super.addColumn(column);
	                        super.moveColumn(tableColumns.size() - 1, visibleIndex);
	                    }
	                    return; // ####################
	                }
	                if(testColumn == visibleColumn) {
	                    ++visibleIndex;
	                }
	            }
	        }
	    }
	    
	    /**
	     * Makes all columns in this model visible
	     */
	    public void setAllColumnsVisible() {
	        int noColumns       = allTableColumns.size();
	        
	        for(int columnIndex = 0; columnIndex < noColumns; ++columnIndex) {
	            TableColumn visibleColumn = (columnIndex < tableColumns.size() ? (TableColumn)tableColumns.get(columnIndex) : null);
	            TableColumn invisibleColumn = (TableColumn)allTableColumns.get(columnIndex);
	            
	            if(visibleColumn != invisibleColumn) {
	                super.addColumn(invisibleColumn);
	                super.moveColumn(tableColumns.size() - 1, columnIndex);
	            }
	        }
	    }
	    
	   /**
	    * Maps the index of the column in the table model at
	    * <code>modelColumnIndex</code> to the TableColumn object.
	    * There may me multiple TableColumn objects showing the same model column, though this is uncommon.
	    * This method will always return the first visible or else the first invisible column with the specified index.
	    * @param modelColumnIndex index of column in table model
	    * @return table column object or null if no such column in this column model
	 */
	    public TableColumn getColumnByModelIndex(int modelColumnIndex) {
	        for (int columnIndex = 0; columnIndex < allTableColumns.size(); ++columnIndex) {
	            TableColumn column = (TableColumn)allTableColumns.elementAt(columnIndex);
	            if(column.getModelIndex() == modelColumnIndex) {
	                return column;
	            }
	        }
	        return null;
	    }
	    
	/** Checks wether the specified column is currently visible.
	 * @param aColumn column to check
	 * @return visibility of specified column (false if there is no such column at all. [It's not visible, right?])
	 */    
	    public boolean isColumnVisible(TableColumn aColumn) {
	        return (tableColumns.indexOf(aColumn) >= 0);
	    }
	    
	/** Append <code>column</code> to the right of exisiting columns.
	 * Posts <code>columnAdded</code> event.
	 * @param column The column to be added
	 * @see #removeColumn
	 * @exception IllegalArgumentException if <code>column</code> is <code>null</code>
	 */    
	    public void addColumn(TableColumn column) {
	        allTableColumns.addElement(column);
	        super.addColumn(column);
	    }
	    
	/** Removes <code>column</code> from this column model.
	 * Posts <code>columnRemoved</code> event.
	 * Will do nothing if the column is not in this model.
	 * @param column the column to be added
	 * @see #addColumn
	 */    
	    public void removeColumn(TableColumn column) {
	        int allColumnsIndex = allTableColumns.indexOf(column);
	        if(allColumnsIndex != -1) {
	            allTableColumns.removeElementAt(allColumnsIndex);
	        }
	        super.removeColumn(column);
	    }
	    
	    /** Moves the column from <code>oldIndex</code> to <code>newIndex</code>.
	     * Posts  <code>columnMoved</code> event.
	     * Will not move any columns if <code>oldIndex</code> equals <code>newIndex</code>.
	     *
	     * @param	oldIndex			index of column to be moved
	     * @param	newIndex			new index of the column
	     * @exception IllegalArgumentException	if either <code>oldIndex</code> or
	     * 						<code>newIndex</code>
	     *						are not in [0, getColumnCount() - 1]
	     */
	    public void moveColumn(int oldIndex, int newIndex) {
		if ((oldIndex < 0) || (oldIndex >= getColumnCount()) ||
		    (newIndex < 0) || (newIndex >= getColumnCount()))
		    throw new IllegalArgumentException("moveColumn() - Index out of range");
	        
	        TableColumn fromColumn  = (TableColumn) tableColumns.get(oldIndex);
	        TableColumn toColumn    = (TableColumn) tableColumns.get(newIndex);
	        
	        int allColumnsOldIndex  = allTableColumns.indexOf(fromColumn);
	        int allColumnsNewIndex  = allTableColumns.indexOf(toColumn);

	        if(oldIndex != newIndex) {
	            allTableColumns.removeElementAt(allColumnsOldIndex);
	            allTableColumns.insertElementAt(fromColumn, allColumnsNewIndex);
	        }
	        
	        super.moveColumn(oldIndex, newIndex);
	    }

	    /**
	     * Returns the total number of columns in this model.
	     *
	     * @param   onlyVisible   if set only visible columns will be counted
	     * @return	the number of columns in the <code>tableColumns</code> array
	     * @see	#getColumns
	     */
	    public int getColumnCount(boolean onlyVisible) {
	        Vector columns = (onlyVisible ? tableColumns : allTableColumns);
		return columns.size();
	    }

	    /**
	     * Returns an <code>Enumeration</code> of all the columns in the model.
	     *
	     * @param   onlyVisible   if set all invisible columns will be missing from the enumeration.
	     * @return an <code>Enumeration</code> of the columns in the model
	     */
	    public Enumeration getColumns(boolean onlyVisible) {
	        Vector columns = (onlyVisible ? tableColumns : allTableColumns);
	        
		return columns.elements();
	    }

	    /**
	     * Returns the position of the first column whose identifier equals <code>identifier</code>.
	     * Position is the the index in all visible columns if <code>onlyVisible</code> is true or
	     * else the index in all columns.
	     *
	     * @param	identifier   the identifier object to search for
	     * @param	onlyVisible  if set searches only visible columns
	     *
	     * @return		the index of the first column whose identifier
	     *			equals <code>identifier</code>
	     *
	     * @exception       IllegalArgumentException  if <code>identifier</code>
	     *				is <code>null</code>, or if no
	     *				<code>TableColumn</code> has this
	     *				<code>identifier</code>
	     * @see		#getColumn
	     */
	    public int getColumnIndex(Object identifier, boolean onlyVisible) {
		if (identifier == null) {
		    throw new IllegalArgumentException("Identifier is null");
		}

	        Vector      columns     = (onlyVisible ? tableColumns : allTableColumns);
	        int         noColumns   = columns.size();
	        TableColumn column;
	        
	        for(int columnIndex = 0; columnIndex < noColumns; ++columnIndex) {
		    column = (TableColumn)columns.get(columnIndex);

	            if(identifier.equals(column.getIdentifier()))
			return columnIndex;
	        }
	        
		throw new IllegalArgumentException("Identifier not found");
	    }

	    /**
	     * Returns the <code>TableColumn</code> object for the column
	     * at <code>columnIndex</code>.
	     *
	     * @param	columnIndex	the index of the column desired
	     * @param	onlyVisible	if set columnIndex is meant to be relative to all visible columns only
	     *                          else it is the index in all columns
	     *
	     * @return	the <code>TableColumn</code> object for the column
	     *				at <code>columnIndex</code>
	     */
	    public TableColumn getColumn(int columnIndex, boolean onlyVisible) {
		return (TableColumn)tableColumns.elementAt(columnIndex);
	    }
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JTable DocTable;
	private javax.swing.JComboBox PartsFilter;
	private javax.swing.JTable PartsTable;
	private javax.swing.JTable patientTable;
	private javax.swing.JTable TaskTable;
	private javax.swing.JTable AcquisitionTable;
	private javax.swing.JTable TechTable;
	private javax.swing.JTable servicedUnitTable;
	private javax.swing.JTable unitTable;
	private javax.swing.JTable personnelTable;
	private javax.swing.JMenuItem addFunds;
	private javax.swing.JButton btnAdvanceDay;
	private javax.swing.JButton btnAssignDoc;
	private javax.swing.JButton btnDeployUnits;
	private javax.swing.JButton btnDoTask;
	private javax.swing.JToggleButton btnGMMode;
	private javax.swing.JButton btnLoadParts;
	private javax.swing.JToggleButton btnOvertime;
	private javax.swing.JButton btnRetrieveUnits;
	private javax.swing.JButton btnSaveParts;
	private javax.swing.JPanel btnUnitPanel;
	private javax.swing.JLabel fundsLabel;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JScrollPane jScrollPane6;
	private javax.swing.JScrollPane jScrollPane8;
	private javax.swing.JLabel lblTarget;
	private javax.swing.JLabel lblTargetNum;
	private javax.swing.JPanel mainPanel;
	private javax.swing.JMenuBar menuBar;
	private javax.swing.JMenu menuHire;
	private javax.swing.JMenuItem menuLoad;
	private javax.swing.JMenuItem menuLoadXml;
	private javax.swing.JMenu menuManage;
	private javax.swing.JMenu menuMarket;
	private javax.swing.JMenuItem menuOptions;
	private javax.swing.JMenuItem menuSave;
	private javax.swing.JMenuItem menuSaveXml;
	private javax.swing.JMenuItem miHireDoctor;
	private javax.swing.JMenuItem miHirePilot;
	private javax.swing.JMenuItem miHireTech;
	private javax.swing.JMenuItem miLoadForces;
	private javax.swing.JMenuItem miPurchaseUnit;
	private javax.swing.JPanel panFinances;
	private javax.swing.JPanel panHangar;
	private javax.swing.JPanel panOrganization;
	private javax.swing.JPanel panRepairBay;
	private javax.swing.JPanel panInfirmary;
	private javax.swing.JPanel panPersonnel;
	private javax.swing.JPanel panSupplies;
	private javax.swing.JPanel panelDoTask;
	private javax.swing.JPanel panelMasterButtons;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JScrollPane scrollDocTable;
	private javax.swing.JScrollPane scrollPatientTable;
	private javax.swing.JScrollPane scrollTaskTable;
	private javax.swing.JScrollPane scrollAcquisitionTable;
	private javax.swing.JScrollPane scrollTechTable;
	private javax.swing.JScrollPane scrollServicedUnitTable;
	private javax.swing.JScrollPane scrollPersonnelTable;
	private javax.swing.JScrollPane scrollUnitTable;
	private javax.swing.JLabel statusAnimationLabel;
	private javax.swing.JLabel statusMessageLabel;
	private javax.swing.JPanel statusPanel;
	private javax.swing.JTabbedPane tabMain;
	private javax.swing.JTabbedPane tabTasks;
	private javax.swing.JTextArea textTarget;
	private javax.swing.JTextPane txtPaneReport;
	private javax.swing.JScrollPane txtPaneReportScrollPane;
	private javax.swing.JComboBox choicePerson;
	private javax.swing.JLabel lblPersonChoice;
	private javax.swing.JComboBox choicePersonView;
	private javax.swing.JLabel lblPersonView;
	private javax.swing.JScrollPane scrollPersonnelView;
    private javax.swing.JSplitPane splitPersonnel;
    private javax.swing.JComboBox choiceUnit;
	private javax.swing.JLabel lblUnitChoice;
	private javax.swing.JComboBox choiceUnitView;
	private javax.swing.JLabel lblUnitView;
	private javax.swing.JScrollPane scrollUnitView;
    private javax.swing.JSplitPane splitUnit;
	private javax.swing.JScrollPane scrollOrgTree;
	private javax.swing.JTree orgTree;
	// End of variables declaration//GEN-END:variables

	private final Timer messageTimer;
	private final Timer busyIconTimer;
	private final Icon idleIcon;
	private final Icon[] busyIcons = new Icon[15];
	private int busyIconIndex = 0;

	private JDialog aboutBox;
}
