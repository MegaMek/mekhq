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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.gui.model.UnitMarketTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.preferences.JToggleButtonPreference;
import mekhq.gui.preferences.JIntNumberSpinnerPreference;
import mekhq.gui.preferences.JTablePreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.sorter.WeightClassSorter;
import mekhq.preferences.PreferencesNode;

/**
 * Code copied heavily from PersonnelMarketDialog
 *
 * @author Neoancient
 */
public class UnitMarketDialog extends JDialog {
    private static final long serialVersionUID = -7668601227249317220L;

    private static boolean showMeks = true;
    private static boolean showVees = true;
    private static boolean showAero = false;
    private static boolean pctThreshold = false;
    private static int threshold = 120;

    private UnitMarketTableModel marketModel;
    private Campaign campaign;
    private UnitMarket unitMarket;
    private Entity selectedEntity = null;

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
    private JSplitPane splitMain;

    /** Creates new form UnitMarketDialog */
    public UnitMarketDialog(JFrame frame, Campaign c) {
        super(frame, true);
        campaign = c;
        unitMarket = c.getUnitMarket();
        marketModel = new UnitMarketTableModel();
        marketModel.setData(unitMarket.getOffers());
        initComponents();
        filterOffers();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

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

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitMarketDialog", new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());

        panelFilterBtns.setLayout(new GridBagLayout());

        ItemListener checkboxListener = arg0 -> {
            showMeks = chkShowMeks.isSelected();
            showAero = chkShowAero.isSelected();
            showVees = chkShowVees.isSelected();
            pctThreshold = chkPctThreshold.isSelected();
            spnThreshold.setEnabled(chkPctThreshold.isSelected());
            filterOffers();
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
        spnThreshold.addChangeListener(arg0 -> {
            threshold = (Integer)spnThreshold.getValue();
            filterOffers();
        });

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(panel, gbc);

        scrollTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTableUnits.setName("srcTablePersonnel");
        scrollTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));

        tableUnits.setModel(marketModel);
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableUnits.setColumnModel(new XTableColumnModel());
        tableUnits.createDefaultColumnsFromModel();
        sorter = new TableRowSorter<>(marketModel);
        sorter.setComparator(UnitMarketTableModel.COL_WEIGHTCLASS, new WeightClassSorter());
        Comparator<String> numComparator = (arg0, arg1) -> {
            if (arg0.length() != arg1.length()) {
                return arg0.length() - arg1.length();
            }
            return arg0.compareTo(arg1);
        };
        sorter.setComparator(UnitMarketTableModel.COL_PRICE, numComparator);
        sorter.setComparator(UnitMarketTableModel.COL_PERCENT, numComparator);
        tableUnits.setRowSorter(sorter);
        tableUnits.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableUnits.getSelectionModel().addListSelectionListener(this::offerChanged);
        TableColumn column;
        for (int i = 0; i < UnitMarketTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel)tableUnits.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(marketModel.getColumnWidth(i));
            column.setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(((UnitMarketTableModel) table.getModel())
                            .getAlignment(table.convertColumnIndexToModel(column)));
                    return this;
                }
            });
        }

        tableUnits.setIntercellSpacing(new Dimension(0, 0));
        tableUnits.setShowGrid(false);
        scrollTableUnits.setViewportView(tableUnits);

        lblBlackMarketWarning.setText(resourceMap.getString("lblBlackMarketWarning.text"));

        scrollTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTableUnits.setName("scrollTableUnits");
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
        btnPurchase.setName("btnPurchase");
        btnPurchase.addActionListener(this::purchaseUnit);
        panelOKBtns.add(btnPurchase, new java.awt.GridBagConstraints());
        btnPurchase.setEnabled(null != selectedEntity);

        btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
        btnAdd.addActionListener(evt -> addUnit());
        btnAdd.setEnabled(null !=  selectedEntity);
        panelOKBtns.add(btnAdd, new java.awt.GridBagConstraints());

        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        getContentPane().add(panelOKBtns, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(UnitMarketDialog.class);

        chkShowMeks.setName("showMeks");
        preferences.manage(new JToggleButtonPreference(chkShowMeks));

        chkShowAero.setName("showAero");
        preferences.manage(new JToggleButtonPreference(chkShowAero));

        chkShowVees.setName("showVees");
        preferences.manage(new JToggleButtonPreference(chkShowVees));

        chkPctThreshold.setName("useThreshold");
        preferences.manage(new JToggleButtonPreference(chkPctThreshold));

        spnThreshold.setName("thresholdValue");
        preferences.manage(new JIntNumberSpinnerPreference(spnThreshold));

        tableUnits.setName("unitsTable");
        preferences.manage(new JTablePreference(tableUnits));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public Entity getUnit() {
        return selectedEntity;
    }

    private void purchaseUnit(ActionEvent evt) {
        if (null != selectedEntity) {
            int transitDays = campaign.getCampaignOptions().getInstantUnitMarketDelivery() ? 0
                    : campaign.calculatePartTransitTime(Compute.d6(2) - 2);
            UnitMarket.MarketOffer offer = marketModel.getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
            Money cost = Money.of(offer.unit.getCost() * offer.pct / 100.0);
            if (campaign.getFunds().isLessThan(cost)) {
                 campaign.addReport("<font color='red'><b> You cannot afford this unit. Transaction cancelled</b>.</font>");
                 return;
            }

            int roll = Compute.d6();
            if ((offer.market == UnitMarketType.BLACK_MARKET) && (roll <= 2)) {
                campaign.getFinances().debit(cost.dividedBy(roll), Transaction.C_UNIT,
                        "Purchased " + selectedEntity.getShortName() + " (lost on black market)",
                        campaign.getLocalDate());
                campaign.addReport("<font color='red'>Swindled! money was paid, but no unit delivered.</font>");
            } else {
                campaign.getFinances().debit(cost, Transaction.C_UNIT,
                        "Purchased " + selectedEntity.getShortName(),
                        campaign.getLocalDate());
                campaign.addNewUnit(selectedEntity, false, transitDays);
                if (!campaign.getCampaignOptions().getInstantUnitMarketDelivery()) {
                    campaign.addReport("<font color='green'>Unit will be delivered in " + transitDays + " days.</font>");
                }
            }
            UnitMarket.MarketOffer selected = ((UnitMarketTableModel) tableUnits.getModel())
                    .getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
            unitMarket.removeOffer(selected);
            ((UnitMarketTableModel) tableUnits.getModel()).setData(unitMarket.getOffers());
            refreshOfferView();
        }
    }

    private void addUnit() {
        if (null != selectedEntity) {
            campaign.addNewUnit(selectedEntity, false, 0);
            UnitMarket.MarketOffer selected = ((UnitMarketTableModel) tableUnits.getModel())
                    .getOffer(tableUnits.convertRowIndexToModel(tableUnits.getSelectedRow()));
            unitMarket.removeOffer(selected);
            ((UnitMarketTableModel) tableUnits.getModel()).setData(unitMarket.getOffers());
            refreshOfferView();
        }
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        selectedEntity = null;
        setVisible(false);
    }

    private void filterOffers() {
        sorter.setRowFilter(new RowFilter<UnitMarketTableModel,Integer>() {
            @Override
            public boolean include(Entry<? extends UnitMarketTableModel, ? extends Integer> entry) {
                UnitMarket.MarketOffer offer = marketModel.getOffer(entry.getIdentifier());
                boolean underThreshold = !chkPctThreshold.isSelected()
                        || (offer.pct <= (Integer) spnThreshold.getValue());
                if (offer.unitType == UnitType.MEK) {
                    return underThreshold && chkShowMeks.isSelected();
                } else if (offer.unitType == UnitType.TANK) {
                    return underThreshold && chkShowVees.isSelected();
                } else if (offer.unitType == UnitType.AERO) {
                    return underThreshold && chkShowAero.isSelected();
                } else {
                    return false;
                }
            }
        });
    }

    private void offerChanged(ListSelectionEvent evt) {
        int view = tableUnits.getSelectedRow();
        if (view < 0) {
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
            MekHQ.getLogger().error("Unable to load mech: " + ms.getSourceFile()
                    + ": " + ms.getEntryName() + ": " + e.getMessage(), e);
            refreshOfferView();
            return;
        }
        refreshOfferView();
    }

     void refreshOfferView() {
         int row = tableUnits.getSelectedRow();
         if ((row < 0) || (selectedEntity == null)) {
             mechViewPanel.reset();
         } else {
             mechViewPanel.setMech(selectedEntity, true);
            //This odd code is to make sure that the scrollbar stays at the top
            //I can't just call it here, because it ends up getting reset somewhere later
            javax.swing.SwingUtilities.invokeLater(() -> scrollUnitView.getVerticalScrollBar().setValue(0));
         }
         btnPurchase.setEnabled(null != selectedEntity);
         btnAdd.setEnabled((selectedEntity != null) && campaign.isGM());
    }

    @Override
    public void setVisible(boolean visible) {
        filterOffers();
         super.setVisible(visible);
    }
}
