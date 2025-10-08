/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.assets.AssetChangedEvent;
import mekhq.campaign.events.assets.AssetNewEvent;
import mekhq.campaign.events.assets.AssetRemovedEvent;
import mekhq.campaign.finances.Asset;
import mekhq.gui.model.DataTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Taharqa
 */
public class ManageAssetsDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ManageAssetsDialog.class);

    private final JFrame frame;
    private final Campaign campaign;
    private final AssetTableModel assetModel;

    private JButton btnEdit;
    private JButton btnDelete;
    private JTable assetTable;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ManageAssetsDialog",
          MekHQ.getMHQOptions().getLocale());

    /** Creates new form EditPersonnelLogDialog */
    public ManageAssetsDialog(JFrame parent, Campaign c) {
        super(parent, true);
        this.frame = parent;
        campaign = c;
        assetModel = new AssetTableModel(campaign.getFinances().getAssets());
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JButton btnOK = new JButton();
        JButton btnAdd = new JButton();
        btnEdit = new JButton();
        btnDelete = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("dialogTitle.text"));
        getContentPane().setLayout(new BorderLayout());

        JPanel panButtons = new JPanel(new GridLayout(1, 0));
        btnAdd.setText(resourceMap.getString("btnAddAsset.text"));
        btnAdd.addActionListener(evt -> addAsset());
        panButtons.add(btnAdd);

        btnEdit.setText(resourceMap.getString("btnEditAsset.text"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editAsset());
        panButtons.add(btnEdit);

        btnDelete.setText(resourceMap.getString("btnRemoveAsset.text"));
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteAsset());
        panButtons.add(btnDelete);

        getContentPane().add(panButtons, BorderLayout.PAGE_START);

        assetTable = new JTable(assetModel);
        TableColumn column;
        for (int i = 0; i < AssetTableModel.N_COL; i++) {
            column = assetTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(assetModel.getColumnWidth(i));
            column.setCellRenderer(assetModel.getRenderer());
        }
        assetTable.setIntercellSpacing(new Dimension(0, 0));
        assetTable.setShowGrid(false);
        assetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assetTable.getSelectionModel().addListSelectionListener(this::assetTableValueChanged);
        JScrollPane scrollAssetTable = new JScrollPaneWithSpeed(assetTable);
        getContentPane().add(scrollAssetTable, BorderLayout.CENTER);

        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
        setUserPreferences();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ManageAssetsDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    private void assetTableValueChanged(ListSelectionEvent evt) {
        int row = assetTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addAsset() {
        Asset a = new Asset();
        EditAssetDialog ead = new EditAssetDialog(frame, a);
        ead.setTitle(resourceMap.getString("addAssetDialogTitle.text"));
        ead.setVisible(true);
        if (!ead.wasCancelled()) {
            campaign.getFinances().getAssets().add(a);
            MekHQ.triggerEvent(new AssetNewEvent(a));
            refreshTable();
        }

        ead.dispose();
    }

    private void editAsset() {
        // TODO: fix this to use a cloned asset and the user has to confirm edits with OK
        Asset a = assetModel.getAssetAt(assetTable.getSelectedRow());
        if (null != a) {
            EditAssetDialog ead = new EditAssetDialog(frame, a);
            ead.setTitle(resourceMap.getString("editAssetDialogTitle.text"));
            ead.setVisible(true);
            MekHQ.triggerEvent(new AssetChangedEvent(a));
            refreshTable();
        }
    }

    private void deleteAsset() {
        MekHQ.triggerEvent(new AssetRemovedEvent(assetModel.getAssetAt(assetTable.getSelectedRow())));
        campaign.getFinances().getAssets().remove(assetTable.getSelectedRow());
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = assetTable.getSelectedRow();
        assetModel.setData(campaign.getFinances().getAssets());

        if (selectedRow != -1) {
            if (assetTable.getRowCount() > 0) {
                if (assetTable.getRowCount() == selectedRow) {
                    assetTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
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
        public final static int COL_NAME = 0;
        public final static int COL_VALUE = 1;
        public final static int COL_SCHEDULE = 2;
        public final static int COL_INCOME = 3;
        public final static int N_COL = 4;

        public AssetTableModel(List<Asset> assets) {
            data = assets;
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_NAME -> "Name";
                case COL_VALUE -> "Value";
                case COL_SCHEDULE -> "Pay Frequency";
                case COL_INCOME -> "Income";
                default -> "?";
            };
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
                return asset.getFinancialTerm();
            }
            return "?";
        }

        public Asset getAssetAt(int row) {
            return (Asset) data.get(row);
        }

        public int getColumnWidth(int c) {
            return 10;
        }

        public int getAlignment(int col) {
            if (col == COL_NAME) {
                return SwingConstants.LEFT;
            }
            return SwingConstants.RIGHT;
        }

        public String getTooltip(int row, int col) {
            return null;
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
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
