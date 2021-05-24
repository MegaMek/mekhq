/*
 * ManageAssetsDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.AssetChangedEvent;
import mekhq.campaign.event.AssetNewEvent;
import mekhq.campaign.event.AssetRemovedEvent;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.Finances;
import mekhq.gui.model.DataTableModel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class ManageAssetsDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    private ResourceBundle resourceMap;
    private Frame frame;
    private Campaign campaign;
    private AssetTableModel assetModel;

    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnOK;
    private JTable assetTable;
    private JScrollPane scrollAssetTable;

    /** Creates new form EditPersonnelLogDialog */
    public ManageAssetsDialog(Frame parent, Campaign c) {
        super(parent, true);
        this.frame = parent;
        campaign = c;
        assetModel = new AssetTableModel(campaign.getFinances().getAllAssets());
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        btnOK = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();

        resourceMap = ResourceBundle.getBundle("mekhq.resources.ManageAssetsDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("dialogTitle.text"));
        getContentPane().setLayout(new java.awt.BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAdd.setText(resourceMap.getString("btnAddAsset.text")); // NOI18N
        btnAdd.addActionListener(evt -> addAsset());
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEditAsset.text")); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editAsset());
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnRemoveAsset.text")); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteAsset());
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);

        assetTable = new JTable(assetModel);
        TableColumn column;
        for (int i = 0; i <AssetTableModel.N_COL; i++) {
            column = assetTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(assetModel.getColumnWidth(i));
            column.setCellRenderer(assetModel.getRenderer());
        }
        assetTable.setIntercellSpacing(new Dimension(0, 0));
        assetTable.setShowGrid(false);
        assetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assetTable.getSelectionModel().addListSelectionListener(this::assetTableValueChanged);
        scrollAssetTable = new JScrollPane(assetTable);
        getContentPane().add(scrollAssetTable, BorderLayout.CENTER);

        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
        setUserPreferences();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ManageAssetsDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
    }

    private void assetTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = assetTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addAsset() {
        Asset a = new Asset();
        EditAssetDialog ead = new EditAssetDialog(frame, a);
        ead.setTitle(resourceMap.getString("addAssetDialogTitle.text"));
        ead.setVisible(true);
        if(!ead.wasCancelled()) {
            campaign.getFinances().getAllAssets().add(a);
            MekHQ.triggerEvent(new AssetNewEvent(a));
            refreshTable();
        }

        ead.dispose();
    }

    private void editAsset() {
        // TODO: fix this to use a cloned asset and the user has to confirm edits with OK
        Asset a = assetModel.getAssetAt(assetTable.getSelectedRow());
        if(null != a) {
            EditAssetDialog ead = new EditAssetDialog(frame, a);
            ead.setTitle(resourceMap.getString("editAssetDialogTitle.text"));
            ead.setVisible(true);
            MekHQ.triggerEvent(new AssetChangedEvent(a));
            refreshTable();
        }
    }

    private void deleteAsset() {
        campaign.getFinances().getAllAssets().remove(assetTable.getSelectedRow());
        MekHQ.triggerEvent(new AssetRemovedEvent(assetModel.getAssetAt(assetTable.getSelectedRow())));
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = assetTable.getSelectedRow();
        assetModel.setData(campaign.getFinances().getAllAssets());
        if(selectedRow != -1) {
            if(assetTable.getRowCount() > 0) {
                if(assetTable.getRowCount() == selectedRow) {
                    assetTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
                } else {
                    assetTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }

    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
     */
    public static class AssetTableModel extends DataTableModel {
        private static final long serialVersionUID = 534443424190075264L;

        public final static int COL_NAME    =    0;
        public final static int COL_VALUE    =   1;
        public final static int COL_SCHEDULE =   2;
        public final static int COL_INCOME   =   3;
        public final static int N_COL          = 4;

        public AssetTableModel(List<Asset> assets) {
            data = assets;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_NAME:
                    return "Name";
                case COL_VALUE:
                    return "Value";
                case COL_SCHEDULE:
                    return "Pay Frequency";
                case COL_INCOME:
                    return "Income";
                default:
                    return "?";
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            Asset asset;
            if (data.isEmpty()) {
                return "";
            } else {
                asset = getAssetAt(row);
            }
            if (col == COL_NAME) {
                return asset.getName();
            }
            if (col == COL_VALUE) {
                return asset.getValue().toAmountAndSymbolString();
            }
            if (col == COL_INCOME) {
                return asset.getIncome().toAmountAndSymbolString();
            }
            if (col == COL_SCHEDULE) {
                return Finances.getScheduleName(asset.getSchedule());
            }
            return "?";
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Asset getAssetAt(int row) {
            return (Asset) data.get(row);
        }

        public int getColumnWidth(int c) {
            switch (c) {
                default:
                    return 10;
            }
        }

        public int getAlignment(int col) {
            switch (col) {
                case COL_NAME:
                    return SwingConstants.LEFT;
                default:
                    return SwingConstants.RIGHT;
            }
        }

        public String getTooltip(int row, int col) {
            switch (col) {
                default:
                    return null;
            }
        }

        public AssetTableModel.Renderer getRenderer() {
            return new AssetTableModel.Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            private static final long serialVersionUID = 9054581142945717303L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                return this;
            }
        }
    }
}
