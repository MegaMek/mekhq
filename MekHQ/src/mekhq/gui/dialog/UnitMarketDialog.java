/*
 * UnitMarketDialog.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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


package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.universe.UnitTableData;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.UnitMarketTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.WeightClassSorter;

/**
 * Code copied heavily from PersonnelMarketDialog
 *
 * @author Neoancient
 *
 */
public class UnitMarketDialog extends JDialog {
	/**
	 *
	 */
	private static final long serialVersionUID = -7668601227249317220L;

	private static boolean showMeks = true;
	private static boolean showVees = true;
	private static boolean showAero = false;
	private static boolean pctThreshold = false;
	private static int threshold = 120;

	private UnitMarketTableModel marketModel;
	private Campaign campaign;
	private CampaignGUI hqView;
    private UnitMarket unitMarket;
    boolean addToCampaign;
    Entity selectedEntity = null;

    private JButton btnAdd;
    private JButton btnPurchase;
    private JButton btnClose;
    private JCheckBox chkShowMeks;
    private JCheckBox chkShowVees;
    private JCheckBox chkShowAero;
    private JCheckBox chkPctThreshold;
    private JLabel lblPctThreshold;
    private JSpinner spnThreshold;
    private JPanel panelOKBtns;
    private JPanel panelMain;
    private JPanel panelFilterBtns;
    private JTable tableUnits;
    private JLabel lblBlackMarketWarning;
    private MechViewPanel mechViewPanel;
    private JScrollPane scrollTableUnits;
    private JScrollPane scrollUnitView;
    private TableRowSorter<UnitMarketTableModel> sorter;
    ArrayList <RowSorter.SortKey> sortKeys;
    private JSplitPane splitMain;

    /** Creates new form UnitSelectorDialog */
    public UnitMarketDialog(Frame frame, CampaignGUI view, Campaign c) {
        super(frame, true);
        hqView = view;
        campaign = c;
        unitMarket = c.getUnitMarket();
        marketModel = new UnitMarketTableModel();
        marketModel.setData(unitMarket.getOffers());
        initComponents();
        filterOffers();
        setLocationRelativeTo(frame);
    }

    @SuppressWarnings("serial")
	private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();

        scrollTableUnits = new JScrollPane();
        scrollUnitView = new JScrollPane();
        mechViewPanel = new MechViewPanel();
        tableUnits = new JTable();
        panelMain = new JPanel();
        panelFilterBtns = new JPanel();
        chkShowMeks = new JCheckBox();
        chkShowVees = new JCheckBox();
        chkShowAero = new JCheckBox();
        chkPctThreshold = new JCheckBox();
        lblPctThreshold = new JLabel();
        spnThreshold = new JSpinner(new SpinnerNumberModel(threshold, 60, 130, 5));
        lblBlackMarketWarning = new JLabel();
        panelOKBtns = new JPanel();
        btnPurchase = new JButton();
        btnClose = new JButton();

		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitMarketDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        getContentPane().setLayout(new BorderLayout());

        panelFilterBtns.setLayout(new GridBagLayout());

        ItemListener checkboxListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				showMeks = chkShowMeks.isSelected();
				showAero = chkShowAero.isSelected();
				showVees = chkShowVees.isSelected();
				pctThreshold = chkPctThreshold.isSelected();
				spnThreshold.setEnabled(chkPctThreshold.isSelected());
				filterOffers();
			}
        };

        chkShowMeks.setText(resourceMap.getString("chkShowMeks.text"));
        chkShowMeks.setSelected(showMeks);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(chkShowMeks, gbc);
        chkShowMeks.addItemListener(checkboxListener);

        chkShowVees.setText(resourceMap.getString("chkShowVees.text"));
        chkShowVees.setSelected(showVees);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(chkShowVees, gbc);
        chkShowVees.addItemListener(checkboxListener);

        chkShowAero.setText(resourceMap.getString("chkShowAero.text"));
        chkShowAero.setSelected(showAero);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(chkShowAero, gbc);
        chkShowAero.addItemListener(checkboxListener);

        JPanel panel = new JPanel();
        chkPctThreshold.setText(resourceMap.getString("chkPctThreshold.text"));
        chkPctThreshold.setSelected(pctThreshold);
        spnThreshold.setEnabled(pctThreshold);
        lblPctThreshold.setText(resourceMap.getString("lblPctThreshold.text"));
        panel.add(chkPctThreshold);
        panel.add(spnThreshold);
        panel.add(lblPctThreshold);
        chkPctThreshold.addItemListener(checkboxListener);
        spnThreshold.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				threshold = (Integer)spnThreshold.getValue();
				filterOffers();
			}
        });

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(panel, gbc);

        scrollTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTableUnits.setName("srcTablePersonnel"); // NOI18N
        scrollTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));

        gbc = new GridBagConstraints();
        tableUnits.setModel(marketModel);
        tableUnits.setName("tableUnits"); // NOI18N
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableUnits.setColumnModel(new XTableColumnModel());
        tableUnits.createDefaultColumnsFromModel();
        sorter = new TableRowSorter<UnitMarketTableModel>(marketModel);
        sorter.setComparator(UnitMarketTableModel.COL_WEIGHTCLASS, new WeightClassSorter());
        Comparator<String> numComparator = new Comparator<String>() {
			public int compare(String arg0, String arg1) {
				if (arg0.length() != arg1.length()) {
					return arg0.length() - arg1.length();
				}
				return arg0.compareTo(arg1);
			}
        };
        sorter.setComparator(UnitMarketTableModel.COL_PRICE, numComparator);
        sorter.setComparator(UnitMarketTableModel.COL_PERCENT, numComparator);
        tableUnits.setRowSorter(sorter);
        tableUnits.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableUnits.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                offerChanged(evt);
            }
        });
        TableColumn column = null;
        for (int i = 0; i < UnitMarketTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel)tableUnits.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(marketModel.getColumnWidth(i));
            column.setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table,
                        Object value, boolean isSelected, boolean hasFocus,
                        int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected,
                            hasFocus, row, column);
                    setHorizontalAlignment(((UnitMarketTableModel)table.getModel()).
                    		getAlignment(table.convertColumnIndexToModel(column)));
                    return this;
                }
            });
        }

        tableUnits.setIntercellSpacing(new Dimension(0, 0));
        tableUnits.setShowGrid(false);
        scrollTableUnits.setViewportView(tableUnits);

        lblBlackMarketWarning.setText(resourceMap.getString("lblBlackMarketWarning.text"));

        scrollTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTableUnits.setName("scrollTableUnits"); // NOI18N
        scrollTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));
        panelMain.setLayout(new BorderLayout());
        panelMain.add(panelFilterBtns, BorderLayout.PAGE_START);
        panelMain.add(scrollTableUnits, BorderLayout.CENTER);
        panelMain.add(lblBlackMarketWarning, BorderLayout.PAGE_END);

        scrollUnitView.setMinimumSize(new java.awt.Dimension(500, 600));
        scrollUnitView.setPreferredSize(new java.awt.Dimension(500, 600));
        scrollUnitView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollUnitView.setViewportView(mechViewPanel);

        splitMain = new JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,panelMain, scrollUnitView);
        splitMain.setOneTouchExpandable(true);
        splitMain.setResizeWeight(0.0);
        getContentPane().add(splitMain, BorderLayout.CENTER);

        panelOKBtns.setLayout(new java.awt.GridBagLayout());

        btnPurchase.setText(resourceMap.getString("btnPurchase.text"));
        btnPurchase.setName("btnPurchase"); // NOI18N
        btnPurchase.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		purchaseUnit(evt);
        	}
        });
        panelOKBtns.add(btnPurchase, new java.awt.GridBagConstraints());
        btnPurchase.setEnabled(null != selectedEntity);

        btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addUnit();
            }
        });
        btnAdd.setEnabled(null !=  selectedEntity);
        panelOKBtns.add(btnAdd, new java.awt.GridBagConstraints());


        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		btnCloseActionPerformed(evt);
        	}
        });
        panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        getContentPane().add(panelOKBtns, BorderLayout.PAGE_END);

        pack();
    }

	public Entity getUnit() {
	    return selectedEntity;
	}

	private void purchaseUnit(ActionEvent evt) {
	    if(null != selectedEntity) {
	    	int transitDays = campaign.getCampaignOptions().getInstantUnitMarketDelivery()?0:
	    		campaign.calculatePartTransitTime(Compute.d6(2) - 2);
			UnitMarket.MarketOffer offer = marketModel.getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
			long cost = (long)Math.ceil(offer.unit.getCost() * offer.pct / 100.0);
			if (campaign.getFunds() < cost) {
				 campaign.addReport("<font color='red'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
				 refreshHqView();
				 return;
			}

			int roll = Compute.d6();
			if (offer.market == UnitMarket.MARKET_BLACK && roll <= 2) {
				campaign.getFinances().debit(cost / roll, Transaction.C_UNIT,
						"Purchased " + selectedEntity.getShortName() + " (lost on black market)",
						campaign.getCalendar().getTime());
				campaign.addReport("<font color='red'>Swindled! Money was paid, but no unit delivered.</font>");
			} else {
				campaign.getFinances().debit(cost, Transaction.C_UNIT,
						"Purchased " + selectedEntity.getShortName(),
						campaign.getCalendar().getTime());
				campaign.addUnit(selectedEntity, false, transitDays);
				if (!campaign.getCampaignOptions().getInstantUnitMarketDelivery()) {
					campaign.addReport("<font color='green'>Unit will be delivered in " + transitDays + " days.</font>");
				}
			}
			UnitMarket.MarketOffer selected = ((UnitMarketTableModel)tableUnits.getModel()).getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
			unitMarket.removeOffer(selected);
			((UnitMarketTableModel)tableUnits.getModel()).setData(unitMarket.getOffers());

	    	refreshHqView();
	    	refreshOfferView();
	    }
	}

	private void addUnit() {
		if (null != selectedEntity) {
			campaign.addUnit(selectedEntity, false, 0);
        	UnitMarket.MarketOffer selected = ((UnitMarketTableModel)tableUnits.getModel()).getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
	    	unitMarket.removeOffer(selected);
	    	((UnitMarketTableModel)tableUnits.getModel()).setData(unitMarket.getOffers());
			refreshHqView();
			refreshOfferView();
		}
	}

    private void refreshHqView() {
        hqView.refreshUnitList();
        hqView.refreshServicedUnitList();
        hqView.refreshFinancialTransactions();
        hqView.refreshReport();
    }

	private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
	    selectedEntity = null;
	    setVisible(false);
	}

    private void filterOffers() {
    	RowFilter<UnitMarketTableModel, Integer> unitTypeFilter = null;
    	unitTypeFilter = new RowFilter<UnitMarketTableModel,Integer>() {
    		@Override
    		public boolean include(Entry<? extends UnitMarketTableModel, ? extends Integer> entry) {
    			UnitMarket.MarketOffer offer = marketModel.getOffer(entry.getIdentifier());
    			boolean underThreshold = !chkPctThreshold.isSelected() ||
    					offer.pct <= (Integer)spnThreshold.getValue();
    			if (offer.unitType == UnitTableData.UNIT_MECH) {
    				return underThreshold && chkShowMeks.isSelected();
    			}
    			if (offer.unitType == UnitTableData.UNIT_VEHICLE) {
    				return underThreshold && chkShowVees.isSelected();
    			}
    			if (offer.unitType == UnitTableData.UNIT_AERO) {
    				return underThreshold && chkShowAero.isSelected();
    			}
    			return false;
    		}
    	};
        sorter.setRowFilter(unitTypeFilter);
   	}

    private void offerChanged(ListSelectionEvent evt) {
        int view = tableUnits.getSelectedRow();
        if(view < 0) {
            //selection got filtered away
            selectedEntity= null;
            refreshOfferView();
            return;
        }
		MechSummary ms = marketModel.getOffer(tableUnits.convertRowIndexToModel(view)).unit;
		try {
			selectedEntity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
		} catch (EntityLoadingException e) {
            selectedEntity = null;
            btnPurchase.setEnabled(false);
            MekHQ.logError("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + e.getMessage());
            MekHQ.logError(e);
            refreshOfferView();
            return;
		}
        refreshOfferView();
    }

     void refreshOfferView() {
    	 int row = tableUnits.getSelectedRow();
         if(row < 0 || selectedEntity == null) {
             mechViewPanel.reset();
         } else {
	    	 mechViewPanel.setMech(selectedEntity, true);
	 		//This odd code is to make sure that the scrollbar stays at the top
	 		//I cant just call it here, because it ends up getting reset somewhere later
	 		javax.swing.SwingUtilities.invokeLater(new Runnable() {
	 			public void run() {
	 				scrollUnitView.getVerticalScrollBar().setValue(0);
	 			}
	 		});
         }
         btnPurchase.setEnabled(null != selectedEntity);
         btnAdd.setEnabled(null != selectedEntity && campaign.isGM());
    }

	@Override
    public void setVisible(boolean visible) {
        filterOffers();
         super.setVisible(visible);
    }
}
