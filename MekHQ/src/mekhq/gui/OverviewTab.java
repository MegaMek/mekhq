/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.PartEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingFactory;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.RatingReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.dialog.PartsInUseBulkActionDialog;
import mekhq.gui.model.PartsInUseTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TwoNumbersSorter;

/**
 *
 */
public final class OverviewTab extends CampaignGuiTab {

	private static final long serialVersionUID = -564451623308341081L;

	public interface FILTER_PART_TYPE {
		public static final int ALL = 0;
		public static final int ARMOR = 1;
		public static final int SYSTEM = 2;
		public static final int EQUIP = 3;
		public static final int LOC = 4;
		public static final int WEAP = 5;
		public static final int AMMO = 6;
		public static final int AMMO_BIN = 7;
		public static final int MISC = 8;
		public static final int ENGINE = 9;
		public static final int GYRO = 10;
		public static final int ACT = 11;
		public static final int MAX_INDEX = 11;
	}

	public interface FILTER_IN_USE {
		public static final int ALL = 0;
		public static final int IN_USE = 1;
		public static final int NOT_IN_USE = 2;
		public static final int MAX_INDEX = 2;
	}

	public interface FILTER_PURCHASABLE {
		public static final int ALL = 0;
		public static final int YES_ONLY = 1;
		public static final int NO_ONLY = 2;
		public static final int MAX_INDEX = 2;
	}

	private JTabbedPane tabOverview;

	// Overview Parts In Use
	private JScrollPane scrollOverviewParts;
	private JPanel overviewPartsPanel;
	private JTable overviewPartsInUseTable;
	private JComboBox<String> overviewPartsTypeSelector;
	private JComboBox<String> overviewPartsInUseSelector;
	private JComboBox<String> overviewPartsPurchasableSelector;
	private JTextField overviewPartsNameFilter;
	private JButton overviewPartsBtnBuySingle;
	private JButton overviewPartsBtnBuyMultiple;
	private JButton overviewPartsBtnAddSingle;
	private JButton overviewPartsBtnAddMultiple;
	private int[] overviewPartsInUseSelectedRows = null;
	
	private PartsInUseTableModel overviewPartsModel;
	private TableRowSorter<PartsInUseTableModel> partsInUseSorter;

	// Overview Transport
	private JScrollPane scrollOverviewTransport;
	// Overview Personnel
	private JScrollPane scrollOverviewCombatPersonnel;
	private JScrollPane scrollOverviewSupportPersonnel;
	private JSplitPane splitOverviewPersonnel;
	// Overview Hangar
	private JScrollPane scrollOverviewHangar;
	private JTextArea overviewHangarArea;
	private JSplitPane splitOverviewHangar;
	// Overview Rating
	private JScrollPane scrollOverviewUnitRating;
	private IUnitRating rating;
	// Overview Cargo
	private JScrollPane scrollOverviewCargo;

	ResourceBundle resourceMap;

	OverviewTab(CampaignGUI gui, String name) {
		super(gui, name);
		MekHQ.registerHandler(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see mekhq.gui.CampaignGuiTab#initTab()
	 */
	@Override
	public void initTab() {
		resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
				new EncodeControl());
		GridBagConstraints gridBagConstraints;

		setTabOverview(new JTabbedPane());
		scrollOverviewParts = new JScrollPane();
		initOverviewPartsInUse();
		scrollOverviewTransport = new JScrollPane();
		scrollOverviewCombatPersonnel = new JScrollPane();
		scrollOverviewSupportPersonnel = new JScrollPane();
		scrollOverviewHangar = new JScrollPane();
		overviewHangarArea = new JTextArea();
		splitOverviewHangar = new JSplitPane();
		scrollOverviewUnitRating = new JScrollPane();
		scrollOverviewCargo = new JScrollPane();

		// Overview tab
		setName("panelOverview"); // NOI18N
		setLayout(new java.awt.GridBagLayout());

		getTabOverview().setToolTipText(resourceMap.getString("tabOverview.toolTipText")); // NOI18N
		getTabOverview().setMinimumSize(new java.awt.Dimension(250, 250));
		getTabOverview().setName("tabOverview"); // NOI18N
		getTabOverview().setPreferredSize(new java.awt.Dimension(800, 300));

		scrollOverviewTransport
				.setToolTipText(resourceMap.getString("scrollOverviewTransport.TabConstraints.toolTipText")); // NOI18N
		scrollOverviewTransport.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewTransport.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewTransport.TabConstraints.tabTitle"),
				scrollOverviewTransport);

		scrollOverviewCargo.setToolTipText(resourceMap.getString("scrollOverviewCargo.TabConstraints.toolTipText")); // NOI18N
		scrollOverviewCargo.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewCargo.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewCargo.TabConstraints.tabTitle"),
				scrollOverviewCargo);

		scrollOverviewCombatPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewCombatPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
		scrollOverviewSupportPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewSupportPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());

		splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewCombatPersonnel,
				scrollOverviewSupportPersonnel);
		splitOverviewPersonnel.setName("splitOverviewPersonnel");
		splitOverviewPersonnel.setOneTouchExpandable(true);
		splitOverviewPersonnel.setResizeWeight(0.5);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		getTabOverview().addTab(resourceMap.getString("scrollOverviewPersonnel.TabConstraints.tabTitle"),
				splitOverviewPersonnel);

		scrollOverviewHangar.setViewportView(new HangarReport(getCampaign()).getHangarTree());
		overviewHangarArea.setName("overviewHangarArea"); // NOI18N
		overviewHangarArea.setLineWrap(false);
		overviewHangarArea.setFont(new Font("Courier New", Font.PLAIN, 18));
		overviewHangarArea.setText("");
		overviewHangarArea.setEditable(false);
		overviewHangarArea.setName("overviewHangarArea"); // NOI18N
		splitOverviewHangar = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewHangar, overviewHangarArea);
		splitOverviewHangar.setName("splitOverviewHangar");
		splitOverviewHangar.setOneTouchExpandable(true);
		splitOverviewHangar.setResizeWeight(0.5);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		getTabOverview().addTab(resourceMap.getString("scrollOverviewHangar.TabConstraints.tabTitle"),
				splitOverviewHangar);

		overviewPartsPanel.setName("overviewPartsPanel"); // NOI18N
		scrollOverviewParts.setViewportView(overviewPartsPanel);
		getTabOverview().addTab(resourceMap.getString("scrollOverviewParts.TabConstraints.tabTitle"),
				scrollOverviewParts);

		rating = UnitRatingFactory.getUnitRating(getCampaign());
		rating.reInitialize();
		scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
				scrollOverviewUnitRating);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		// gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
		add(getTabOverview(), gridBagConstraints);
	}

	/**
	 * @return the tabOverview
	 */
	public JTabbedPane getTabOverview() {
		return tabOverview;
	}

	/**
	 * @param tabOverview
	 *            the tabOverview to set
	 */
	public void setTabOverview(JTabbedPane tabOverview) {
		this.tabOverview = tabOverview;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see mekhq.gui.CampaignGuiTab#refreshAll()
	 */
	@Override
	public void refreshAll() {
		refreshOverview();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see mekhq.gui.CampaignGuiTab#tabType()
	 */
	@Override
	public GuiTabType tabType() {
		return GuiTabType.OVERVIEW;
	}

	private void initOverviewPartsInUse() {
		overviewPartsPanel = new JPanel(new GridBagLayout());

		overviewPartsModel = new PartsInUseTableModel(getCampaign());
		overviewPartsInUseTable = new JTable(overviewPartsModel);
		overviewPartsInUseTable.setRowSelectionAllowed(true);
		overviewPartsInUseTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		overviewPartsInUseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumn column = null;
		for (int i = 0; i < overviewPartsModel.getColumnCount(); ++i) {
			column = overviewPartsInUseTable.getColumnModel().getColumn(i);
			column.setCellRenderer(overviewPartsModel.getRenderer());
			if (overviewPartsModel.hasConstantWidth(i)) {
				column.setMinWidth(overviewPartsModel.getWidth(i));
				column.setMaxWidth(overviewPartsModel.getWidth(i));
			} else {
				column.setPreferredWidth(overviewPartsModel.getPreferredWidth(i));
			}
		}
		overviewPartsInUseTable.setIntercellSpacing(new Dimension(0, 0));
		overviewPartsInUseTable.setShowGrid(false);
		partsInUseSorter = new TableRowSorter<PartsInUseTableModel>(overviewPartsModel);
		partsInUseSorter.setSortsOnUpdates(true);

		// Numeric columns
		partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_USE, new FormattedNumberSorter());
		partsInUseSorter.setComparator(PartsInUseTableModel.COL_STORED, new FormattedNumberSorter());
		partsInUseSorter.setComparator(PartsInUseTableModel.COL_TONNAGE, new FormattedNumberSorter());
		partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_TRANSFER, new TwoNumbersSorter());
		partsInUseSorter.setComparator(PartsInUseTableModel.COL_COST, new FormattedNumberSorter());
		// Default starting sort
		partsInUseSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
		overviewPartsInUseTable.setRowSorter(partsInUseSorter);

		// START: Selector panel
		DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i <= FILTER_PART_TYPE.MAX_INDEX; i++) {
			partsGroupModel.addElement(getPartsGroupName(i));
		}

		overviewPartsTypeSelector = new JComboBox<String>(partsGroupModel);
		overviewPartsTypeSelector.setSelectedIndex(0);
		overviewPartsTypeSelector.addActionListener(ev -> filterParts());

		DefaultComboBoxModel<String> partsInUseSelectorModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i <= FILTER_IN_USE.MAX_INDEX; i++) {
			partsInUseSelectorModel.addElement(getPartsInUseFilterName(i));
		}

		overviewPartsInUseSelector = new JComboBox<String>(partsInUseSelectorModel);
		overviewPartsInUseSelector.setSelectedIndex(0);
		overviewPartsInUseSelector.addActionListener(ev -> filterParts());

		JLabel lblPartsType = new JLabel(resourceMap.getString("lblPartsChoice.text"));
		JLabel lblViewType = new JLabel("View:");
		JLabel lblName = new JLabel("Name:");
		JLabel lblPurchasable = new JLabel("Purchasable:");

		DefaultComboBoxModel<String> partsInUsePurchasableModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i <= FILTER_PURCHASABLE.MAX_INDEX; i++) {
			partsInUsePurchasableModel.addElement(getPartsPurchasableFilterName(i));
		}

		overviewPartsPurchasableSelector = new JComboBox<String>(partsInUsePurchasableModel);
		overviewPartsPurchasableSelector.setSelectedIndex(0);
		overviewPartsPurchasableSelector.addActionListener(ev -> filterParts());
		
		overviewPartsNameFilter = new JTextField();
		overviewPartsNameFilter.setMinimumSize(new java.awt.Dimension(200, 20));
		overviewPartsNameFilter.setPreferredSize(new java.awt.Dimension(200, 20));
		overviewPartsNameFilter.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				filterParts();
			}

			public void insertUpdate(DocumentEvent e) {
				filterParts();
			}

			public void removeUpdate(DocumentEvent e) {
				filterParts();
			}
		});

		JPanel pnlSelector = new JPanel(new GridBagLayout());

		GridBagConstraints gbcSelector = new GridBagConstraints();
		gbcSelector = new java.awt.GridBagConstraints();
		gbcSelector.gridx = 0;
		gbcSelector.gridy = 0;
		gbcSelector.weightx = 0.1;
		gbcSelector.fill = GridBagConstraints.NONE;
		gbcSelector.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbcSelector.insets = new java.awt.Insets(5, 5, 5, 5);

		pnlSelector.add(lblPartsType, gbcSelector);

		gbcSelector.gridx = 1;
		pnlSelector.add(overviewPartsTypeSelector, gbcSelector);

		gbcSelector.gridx = 2;
		pnlSelector.add(lblName, gbcSelector);

		gbcSelector.gridx = 3;
		pnlSelector.add(overviewPartsNameFilter, gbcSelector);

		gbcSelector.gridx = 0;
		gbcSelector.gridy = 1;
		pnlSelector.add(lblViewType, gbcSelector);

		gbcSelector.gridx = 1;
		pnlSelector.add(overviewPartsInUseSelector, gbcSelector);		
		
		gbcSelector.gridx = 2;
		pnlSelector.add(lblPurchasable, gbcSelector);
		
		gbcSelector.gridx = 3;
		pnlSelector.add(overviewPartsPurchasableSelector, gbcSelector);
		// END: Selector panel

		// START: Action button panel
        @SuppressWarnings("serial")
        Action multipleCallbackAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	String[] actionCommand = e.getActionCommand().split(":");
            	int actionType = Integer.parseInt(actionCommand[0]);
            	boolean isBuy = Integer.parseInt(actionCommand[1]) == PartsInUseBulkActionDialog.MODE.BUY;
            	int amount = Integer.parseInt(actionCommand[2]);
            	
				registerSelectedPartsInUse();

				for (int selectedIndex : overviewPartsInUseTable.getSelectedRows()) {
					int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(selectedIndex);

					switch (actionType) {
						case PartsInUseBulkActionDialog.ACTION_TYPE.EXACT:
							overviewPartsModel.buyOrAddPartByExactAmount(rowIndex, amount, isBuy, false);
							break;
							
						case PartsInUseBulkActionDialog.ACTION_TYPE.MINIMUM:
							overviewPartsModel.buyOrAddPartByMinimum(rowIndex, amount, isBuy);
							
						case PartsInUseBulkActionDialog.ACTION_TYPE.PERCENTAGE:
							overviewPartsModel.buyOrAddPartByPercentage(rowIndex, amount, isBuy);
					}
					
					Thread.yield();
				}

				for (int selectedIndex : overviewPartsInUseTable.getSelectedRows()) {
					int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(selectedIndex);
				
					refreshOverviewSpecificPart(rowIndex);
				}
            }
        };
		
		overviewPartsBtnBuySingle = new JButton();
		overviewPartsBtnBuySingle.setText("Buy Selected Parts"); // NOI18N
		overviewPartsBtnBuySingle.setToolTipText("Buy one of each selected part"); // NOI18N
		overviewPartsBtnBuySingle.setName("overviewPartsBtnBuySingle"); // NOI18N
		overviewPartsBtnBuySingle.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (!verifySelectedPartsType()) {
					return;
				}

				registerSelectedPartsInUse();

				for (int selectedIndex : overviewPartsInUseTable.getSelectedRows()) {
					int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(selectedIndex);

					overviewPartsModel.buyOrAddPartByExactAmount(rowIndex, 1, true, true);
					refreshOverviewSpecificPart(rowIndex);
					
					Thread.yield();
				}
			}
		});

		overviewPartsBtnBuyMultiple = new JButton();
		overviewPartsBtnBuyMultiple.setText("Buy Selected Parts (Bulk)"); // NOI18N
		overviewPartsBtnBuyMultiple.setToolTipText("Buy multiple of each selected part"); // NOI18N
		overviewPartsBtnBuyMultiple.setName("overviewPartsBtnBuyMultiple"); // NOI18N
		overviewPartsBtnBuyMultiple.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (!verifySelectedPartsType()) {
					return;
				}

				int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(overviewPartsInUseTable.getSelectedRows()[0]);

				PartInUse piu = overviewPartsModel.getPartInUse(rowIndex);
				Part part = piu.getPartToBuy().getAcquisitionPart();
				
				boolean buyByTonnage = isPartInGroup(part, FILTER_PART_TYPE.ARMOR) || isPartInGroup(part, FILTER_PART_TYPE.AMMO);
				
				PartsInUseBulkActionDialog dlg = new PartsInUseBulkActionDialog(getCampaignGui().getFrame(), true, PartsInUseBulkActionDialog.MODE.BUY, overviewPartsInUseTable.getSelectedRows().length, buyByTonnage, multipleCallbackAction);
	            dlg.setVisible(true);
			}
		});

		overviewPartsBtnAddSingle = new JButton();
		overviewPartsBtnAddSingle.setText("[GM] Add Selected Parts"); // NOI18N
		overviewPartsBtnAddSingle.setToolTipText("GM add one of each selected part"); // NOI18N
		overviewPartsBtnAddSingle.setName("overviewPartsBtnAddSingle"); // NOI18N
		overviewPartsBtnAddSingle.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (!verifySelectedPartsType()) {
					return;
				}

				registerSelectedPartsInUse();

				for (int selectedIndex : overviewPartsInUseTable.getSelectedRows()) {
					int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(selectedIndex);

					overviewPartsModel.buyOrAddPartByExactAmount(rowIndex, 1, false, true);
					refreshOverviewSpecificPart(rowIndex);
					
					Thread.yield();
				}
			}
		});

		overviewPartsBtnAddMultiple = new JButton();
		overviewPartsBtnAddMultiple.setText("[GM] Add Selected Parts (Bulk)"); // NOI18N
		overviewPartsBtnAddMultiple.setToolTipText("GM add multiple of each selected part"); // NOI18N
		overviewPartsBtnAddMultiple.setName("overviewPartsBtnAddMultiple"); // NOI18N
		overviewPartsBtnAddMultiple.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (!verifySelectedPartsType()) {
					return;
				}

				int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(overviewPartsInUseTable.getSelectedRows()[0]);

				PartInUse piu = overviewPartsModel.getPartInUse(rowIndex);
				Part part = piu.getPartToBuy().getAcquisitionPart();
				
				boolean buyByTonnage = isPartInGroup(part, FILTER_PART_TYPE.ARMOR) || isPartInGroup(part, FILTER_PART_TYPE.AMMO);
				
				PartsInUseBulkActionDialog dlg = new PartsInUseBulkActionDialog(getCampaignGui().getFrame(), true, PartsInUseBulkActionDialog.MODE.ADD, overviewPartsInUseTable.getSelectedRows().length, buyByTonnage, multipleCallbackAction);
	            dlg.setVisible(true);
			}
		});

		JPanel pnlButtons = new JPanel();
		pnlButtons.setLayout(new GridLayout(2, 2, 5, 5));

		pnlButtons.add(overviewPartsBtnBuySingle);
		pnlButtons.add(overviewPartsBtnBuyMultiple);
		pnlButtons.add(overviewPartsBtnAddSingle);
		pnlButtons.add(overviewPartsBtnAddMultiple);
		// END: Action button panel

		GridBagConstraints gbcMain = new GridBagConstraints();
		gbcMain = new java.awt.GridBagConstraints();
		gbcMain.gridx = 0;
		gbcMain.gridy = 0;
		gbcMain.weightx = 0.1;
		gbcMain.fill = GridBagConstraints.NONE;
		gbcMain.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbcMain.insets = new java.awt.Insets(5, 5, 5, 5);

		overviewPartsPanel.add(pnlSelector, gbcMain);

		gbcMain.gridx = 1;
		gbcMain.anchor = java.awt.GridBagConstraints.NORTHEAST;

		overviewPartsPanel.add(pnlButtons, gbcMain);

		gbcMain.gridx = 0;
		gbcMain.gridy = 1;
		gbcMain.gridwidth = 2;
		gbcMain.fill = GridBagConstraints.BOTH;
		gbcMain.weightx = 1.0;
		gbcMain.weighty = 1.0;
		gbcMain.anchor = java.awt.GridBagConstraints.NORTHWEST;

		overviewPartsPanel.add(new JScrollPane(overviewPartsInUseTable), gbcMain);
	}

	private boolean verifySelectedPartsType() {
		if (overviewPartsInUseTable.getSelectedRowCount() == 0) {
			JOptionPane.showMessageDialog(tabOverview, "You must have at least one part selected", "No parts selected",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		boolean hasArmor = false;
		boolean hasAmmo = false;
		boolean hasOther = false;

		for (int selectedIndex : overviewPartsInUseTable.getSelectedRows()) {
			int rowIndex = overviewPartsInUseTable.convertRowIndexToModel(selectedIndex);

			PartInUse piu = overviewPartsModel.getPartInUse(rowIndex);
			Part part = piu.getPartToBuy().getAcquisitionPart();

			if (isPartInGroup(part, FILTER_PART_TYPE.ARMOR)) {
				hasArmor = true;
			} else if (isPartInGroup(part, FILTER_PART_TYPE.AMMO)) {
				hasAmmo = true;
			} else {
				hasOther = true;
			}
			
			int groupCount = (hasArmor ? 1 : 0) + (hasAmmo ? 1 : 0) + (hasOther ? 1 : 0);
			
			if (groupCount > 1) {
				JOptionPane.showMessageDialog(tabOverview, "When selecting parts, if you selected 'armor' or 'ammunition' parts, you may not select other types of parts at the same time.", "Invalid selection combination",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		return true;
	}

	private void registerSelectedPartsInUse() {
		overviewPartsInUseSelectedRows = overviewPartsInUseTable.getSelectedRows();
	}

	private void reselectPartsInUseRows() {
		if (null == overviewPartsInUseSelectedRows) {
			return;
		}

		overviewPartsInUseTable.clearSelection();

		for (int rowIdx : overviewPartsInUseSelectedRows) {
			overviewPartsInUseTable.addRowSelectionInterval(rowIdx, rowIdx);
		}

		overviewPartsInUseSelectedRows = null;
	}

	public void refreshOverview() {
		SwingUtilities.invokeLater(() -> {
			int drIndex = getTabOverview().indexOfComponent(scrollOverviewUnitRating);
			if (!getCampaign().getCampaignOptions().useDragoonRating() && drIndex != -1) {
				getTabOverview().removeTabAt(drIndex);
			} else {
				if (drIndex == -1) {
					getTabOverview().addTab(
							resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
							scrollOverviewUnitRating);
				}
			}

			scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
			scrollOverviewCombatPersonnel
					.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
			scrollOverviewSupportPersonnel
					.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());
			scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
			scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
			HangarReport hr = new HangarReport(getCampaign());
			overviewHangarArea.setText(hr.getHangarTotals());
			scrollOverviewHangar.setViewportView(hr.getHangarTree());
			refreshOverviewPartsInUse();

			reselectPartsInUseRows();
		});
	}

	private void refreshOverviewSpecificPart(int row) {
		PartInUse piu = overviewPartsModel.getPartInUse(row);
		IAcquisitionWork newPart = piu.getPartToBuy();

		//TODO: This is horribly inefficient. Redo it. Why do we even bother doing this?
		if (piu.equals(new PartInUse((Part) newPart, getCampaign()))) {
			// Simple update
			getCampaign().updatePartInUse(piu);
			overviewPartsModel.fireTableRowsUpdated(row, row);
		} else {
			// Some other part changed; fire a full refresh to be sure
			refreshOverviewPartsInUse();
		}
	}

	public void refreshOverviewPartsInUse() {
		overviewPartsModel.setData(getCampaign().getPartsInUse());

		overviewPartsBtnAddSingle.setEnabled(getCampaign().isGM());
		overviewPartsBtnAddMultiple.setEnabled(getCampaign().isGM());
	}

	private ActionScheduler overviewScheduler = new ActionScheduler(this::refreshOverview);

	@Subscribe
	public void handle(OptionsChangedEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(DeploymentChangedEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(ScenarioResolvedEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(UnitEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(PersonEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(PartEvent ev) {
		overviewScheduler.schedule();
	}

	@Subscribe
	public void handle(PartWorkEvent ev) {
		overviewScheduler.schedule();
	}

	public void filterParts() {
		overviewPartsInUseTable.clearSelection();

		RowFilter<PartsInUseTableModel, Integer> partsTypeFilter = null;
		final int nGroup = overviewPartsTypeSelector.getSelectedIndex();
		final int nInUse = overviewPartsInUseSelector.getSelectedIndex();
		final int nPurchasable = overviewPartsPurchasableSelector.getSelectedIndex();
		
		partsTypeFilter = new RowFilter<PartsInUseTableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends PartsInUseTableModel, ? extends Integer> entry) {
				PartsInUseTableModel partsModel = entry.getModel();
				PartInUse piu = partsModel.getPartInUse(entry.getIdentifier());
				Part part = piu.getPartToBuy().getAcquisitionPart();
				boolean inGroup = isPartInGroup(part, nGroup);
				boolean inInUseFilter = false;
				boolean inName = false;
				boolean inPurchasable = false;
				
				switch (nInUse) {
				case FILTER_IN_USE.ALL:
					inInUseFilter = true;
					break;

				case FILTER_IN_USE.IN_USE:
					inInUseFilter = piu.getUseCount() > 0;
					break;

				case FILTER_IN_USE.NOT_IN_USE:
					inInUseFilter = piu.getUseCount() == 0;
					break;
				}
				
				switch (nPurchasable) {
				case FILTER_PURCHASABLE.ALL:
					inPurchasable = true;
					break;

				case FILTER_PURCHASABLE.YES_ONLY:
					inPurchasable = getCampaign().canAcquireEquipment(piu.getPartToBuy(), false);
					break;

				case FILTER_PURCHASABLE.NO_ONLY:
					inPurchasable = !getCampaign().canAcquireEquipment(piu.getPartToBuy(), false);
					break;
				}

				String nameFilter = overviewPartsNameFilter.getText();

				if (StringUtil.isNullOrEmpty(nameFilter)) {
					inName = true;
				} else {
					inName = piu.getDescription().toLowerCase().indexOf(nameFilter.toLowerCase()) > -1;
				}
				
				return (inGroup && inInUseFilter && inName && inPurchasable);
			}
		};

		partsInUseSorter.setRowFilter(partsTypeFilter);
	}

	public static boolean isPartInGroup(Part part, int group) {
		switch (group) {
		case FILTER_PART_TYPE.ALL:
			return true;

		case FILTER_PART_TYPE.ARMOR:
			return part instanceof Armor;

		case FILTER_PART_TYPE.SYSTEM:
			return part instanceof MekGyro || part instanceof MekLifeSupport || part instanceof MekSensor;

		case FILTER_PART_TYPE.EQUIP:
			return ((part instanceof EquipmentPart) && !(part instanceof AmmoBin) && !(((EquipmentPart) part).getType() instanceof AmmoType)) && !(((EquipmentPart) part).getType() instanceof WeaponType);

		case FILTER_PART_TYPE.LOC:
			return part instanceof MekLocation || part instanceof TankLocation;

		case FILTER_PART_TYPE.WEAP:
			return part instanceof EquipmentPart && ((EquipmentPart) part).getType() instanceof WeaponType;

		case FILTER_PART_TYPE.AMMO:
			return part instanceof EquipmentPart && !(part instanceof AmmoBin)
					&& ((EquipmentPart) part).getType() instanceof AmmoType;

		case FILTER_PART_TYPE.AMMO_BIN:
			return part instanceof EquipmentPart && (part instanceof AmmoBin)
					&& ((EquipmentPart) part).getType() instanceof AmmoType;

		case FILTER_PART_TYPE.MISC:
			return part instanceof EquipmentPart && ((EquipmentPart) part).getType() instanceof MiscType;

		case FILTER_PART_TYPE.ENGINE:
			return part instanceof EnginePart;

		case FILTER_PART_TYPE.GYRO:
			return part instanceof MekGyro;

		case FILTER_PART_TYPE.ACT:
			return part instanceof MekActuator;
		}

		return false;
	}

	public static String getPartsGroupName(int group) {
		switch (group) {
		case FILTER_PART_TYPE.ALL:
			return "All Parts";
		case FILTER_PART_TYPE.ARMOR:
			return "Armor";
		case FILTER_PART_TYPE.SYSTEM:
			return "System Components";
		case FILTER_PART_TYPE.EQUIP:
			return "Equipment";
		case FILTER_PART_TYPE.LOC:
			return "Locations";
		case FILTER_PART_TYPE.WEAP:
			return "Weapons";
		case FILTER_PART_TYPE.AMMO:
			return "Ammunition";
		case FILTER_PART_TYPE.AMMO_BIN:
			return "Ammunition Bins";
		case FILTER_PART_TYPE.MISC:
			return "Miscellaneous Equipment";
		case FILTER_PART_TYPE.ENGINE:
			return "Engines";
		case FILTER_PART_TYPE.GYRO:
			return "Gyros";
		case FILTER_PART_TYPE.ACT:
			return "Actuators";
		default:
			return "?";
		}
	}

	public static String getPartsInUseFilterName(int filterIdx) {
		switch (filterIdx) {
		case FILTER_IN_USE.ALL:
			return "All Parts";
		case FILTER_IN_USE.IN_USE:
			return "'In Use' only";
		case FILTER_IN_USE.NOT_IN_USE:
			return "'Not In Use' only";
		default:
			return "?";
		}
	}

	public static String getPartsPurchasableFilterName(int filterIdx) {
		switch (filterIdx) {
		case FILTER_PURCHASABLE.ALL:
			return "All Parts";
		case FILTER_PURCHASABLE.YES_ONLY:
			return "Purchasable only";
		case FILTER_PURCHASABLE.NO_ONLY:
			return "Not purchasable only";
		default:
			return "?";
		}
	}
}
