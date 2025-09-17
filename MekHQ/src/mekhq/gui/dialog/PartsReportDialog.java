/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.PartsInUseTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TwoNumbersSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * A dialog to show parts in use, ordered, in transit with actionable buttons for buying or adding more taken from the
 * Overview tab originally but now a dialog.
 */
public class PartsReportDialog extends JDialog {

    private JCheckBox ignoreMothballedCheck, topUpWeeklyCheck;
    private JButton topUpGMButton;
    private JComboBox<String> ignoreSparesUnderQualityCB;
    private JTable overviewPartsInUseTable;
    private PartsInUseTableModel overviewPartsModel;

    private final Campaign campaign;
    private final CampaignGUI gui;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.PartsReportDialog", MekHQ.getMHQOptions().getLocale());

    public PartsReportDialog(CampaignGUI gui, boolean modal) {
        super(gui.getFrame(), modal);
        this.gui = gui;
        this.campaign = gui.getCampaign();
        initComponents();
        updateOverviewPartsInUse();
        pack();
        setLocationRelativeTo(gui.getFrame());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose(); // Call a custom method
            }
        });
    }

    private void initComponents() {

        this.setTitle(resourceMap.getString("Form.title"));

        Container container = this.getContentPane();

        GroupLayout layout = new GroupLayout(container);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        container.setLayout(layout);

        overviewPartsModel = new PartsInUseTableModel();
        overviewPartsInUseTable = new JTable(overviewPartsModel);
        overviewPartsInUseTable.setRowSelectionAllowed(false);
        overviewPartsInUseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column;
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
        TableRowSorter<PartsInUseTableModel> partsInUseSorter = new TableRowSorter<>(overviewPartsModel);
        partsInUseSorter.setSortsOnUpdates(true);
        // Don't sort the buttons
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_BUY, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_BUY_BULK, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_SELL, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_SELL_BULK, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GM_ADD, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GM_ADD_BULK, false);
        // Numeric columns
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_USE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_STORED, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_TONNAGE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_REQUESTED_STOCK, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_TRANSFER, new TwoNumbersSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_COST, new FormattedNumberSorter());
        // Default starting sort
        partsInUseSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        overviewPartsInUseTable.setRowSorter(partsInUseSorter);

        // Add buttons and actions. TODO: Only refresh the row we are working
        // on, not the whole table
        Action buy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                campaign.getShoppingList().addShoppingItem(partToBuy, 1, campaign);
                refreshOverviewSpecificPart(row, partInUse, partToBuy);
            }
        };

        Action buyInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                      "How Many " + partInUse.getPartToBuy().getAcquisitionName(), quantity, 1,
                      CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                if (quantity <= 0) {
                    return;
                }
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                campaign.getShoppingList().addShoppingItem(partToBuy, quantity, campaign);
                refreshOverviewSpecificPart(row, partInUse, partToBuy);
            }
        };

        Action sell = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                Optional<Part> spare = partInUse.getSpare();
                spare.ifPresent(part -> campaign.getQuartermaster().sellPart(part, 1));
                refreshOverviewPartsInUse();
            }
        };

        Action sellInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                List<Part> spares = partInUse.getSpares();
                if (spares.isEmpty()) {
                    return;
                }
                int spareQty = spares.stream().mapToInt(Part::getSellableQuantity).sum();
                int sellQty = 1;
                PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Sell how many " + spares.get(0).getName(),
                      sellQty,
                      1,
                      CampaignGUI.MAX_QUANTITY_SPINNER);
                popupValueChoiceDialog.setVisible(true);
                sellQty = popupValueChoiceDialog.getValue();
                if (sellQty <= 0) {
                    return;
                }
                if (sellQty > spareQty) {
                    sellQty = spareQty;
                }
                Quartermaster quartermaster = campaign.getQuartermaster();
                int i = 0;
                while (sellQty > 0 && i < spares.size()) {
                    Part spare = spares.get(i);
                    int spareQuantity = spare.getSellableQuantity();
                    if (spareQuantity >= sellQty) {
                        quartermaster.sellPart(spare, sellQty);
                        break;
                    } else {
                        // Not enough quantity in this spare, so sell them all and move onto the next one
                        quartermaster.sellPart(spare, spareQuantity);
                        sellQty -= spareQuantity;
                    }
                    i++;
                }
                refreshOverviewPartsInUse();
            }
        };

        Action add = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                campaign.getQuartermaster().addPart((Part) partToBuy.getNewEquipment(), 0, false);
                refreshOverviewSpecificPart(row, partInUse, partToBuy);
            }
        };
        Action addInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse partInUse = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                      "How Many " + partInUse.getPartToBuy().getAcquisitionName(), quantity, 1,
                      CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                while (quantity > 0) {
                    campaign.getQuartermaster().addPart((Part) partToBuy.getNewEquipment(), 0, false);
                    --quantity;
                }
                refreshOverviewSpecificPart(row, partInUse, partToBuy);
            }
        };

        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buy, PartsInUseTableModel.COL_BUTTON_BUY);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buyInBulk,
              PartsInUseTableModel.COL_BUTTON_BUY_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, sell, PartsInUseTableModel.COL_BUTTON_SELL);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, sellInBulk,
              PartsInUseTableModel.COL_BUTTON_SELL_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, add, PartsInUseTableModel.COL_BUTTON_GM_ADD);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, addInBulk,
              PartsInUseTableModel.COL_BUTTON_GM_ADD_BULK);


        JScrollPane tableScroll = new JScrollPaneWithSpeed(overviewPartsInUseTable);

        ignoreMothballedCheck = new JCheckBox(resourceMap.getString("chkIgnoreMothballed.text"));
        ignoreMothballedCheck.addActionListener(evt -> refreshOverviewPartsInUse());
        ignoreMothballedCheck.setSelected(campaign.getIgnoreMothballed());


        topUpWeeklyCheck = new JCheckBox(resourceMap.getString("chkTopUpWeekly.text"));
        topUpWeeklyCheck.addActionListener(evt -> refreshOverviewPartsInUse());
        topUpWeeklyCheck.setSelected(campaign.getTopUpWeekly());


        JButton topUpButton = new JButton();
        topUpButton.setText(resourceMap.getString("topUpBtn.text"));
        topUpButton.setIcon(null);
        topUpButton.setFocusPainted(false);
        topUpButton.setEnabled(true);
        topUpButton.setBorder(null);
        topUpButton.setMargin(new Insets(10, 20, 10, 20));
        topUpButton.addActionListener(evt -> topUp());

        topUpGMButton = new JButton();
        topUpGMButton.setText(resourceMap.getString("topUpGMBtn.text"));
        topUpGMButton.setIcon(null);
        topUpGMButton.setFocusPainted(false);
        topUpGMButton.setEnabled(true);
        topUpGMButton.setBorder(null);
        topUpGMButton.setMargin(new Insets(10, 20, 10, 20));
        topUpGMButton.addActionListener(evt -> topUpGM());

        JButton resetRequestedStockButton = new JButton();
        resetRequestedStockButton.setText(resourceMap.getString("resetRequestedStockBtn.text"));
        resetRequestedStockButton.setIcon(null);
        resetRequestedStockButton.setFocusPainted(false);
        resetRequestedStockButton.setEnabled(true);
        resetRequestedStockButton.setBorder(null);
        resetRequestedStockButton.setMargin(new Insets(10, 20, 10, 20));
        resetRequestedStockButton.addActionListener(evt -> resetRequestedStock());

        boolean reverse = campaign.getCampaignOptions().isReverseQualityNames();
        String[] qualities = {
              " ", // Combo box is blank for first one because it accepts everything and is default
              PartQuality.QUALITY_B.toName(reverse),
              PartQuality.QUALITY_C.toName(reverse),
              PartQuality.QUALITY_D.toName(reverse),
              PartQuality.QUALITY_E.toName(reverse),
              PartQuality.QUALITY_F.toName(reverse)
        };

        ignoreSparesUnderQualityCB = new JComboBox<>(qualities);
        ignoreSparesUnderQualityCB.setMaximumSize(ignoreSparesUnderQualityCB.getPreferredSize());
        ignoreSparesUnderQualityCB.addActionListener(evt -> refreshOverviewPartsInUse());
        JLabel ignorePartsUnderLabel = new JLabel(resourceMap.getString("lblIgnoreSparesUnderQuality.text"));
        if (campaign.getIgnoreSparesUnderQuality() != null) {
            ignoreSparesUnderQualityCB.setSelectedItem(campaign.getIgnoreSparesUnderQuality());
        } else {
            ignoreSparesUnderQualityCB.setSelectedItem(" ");
        }


        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(evt -> {
            setVisible(false);
            onClose();
        });

        layout.setHorizontalGroup(layout.createParallelGroup()
                                        .addComponent(tableScroll)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addGroup(layout.createSequentialGroup()
                                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                                                              GroupLayout.DEFAULT_SIZE,
                                                                              Short.MAX_VALUE)
                                                                        .addComponent(ignorePartsUnderLabel)
                                                                        .addComponent(ignoreSparesUnderQualityCB)
                                                                        .addComponent(ignoreMothballedCheck)
                                                                        .addComponent(topUpWeeklyCheck)
                                                                        .addComponent(topUpButton)
                                                                        .addComponent(topUpGMButton)
                                                                        .addComponent(resetRequestedStockButton)
                                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                                                              GroupLayout.DEFAULT_SIZE,
                                                                              Short.MAX_VALUE)
                                                                        .addComponent(btnClose))));

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(tableScroll)
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(ignoreMothballedCheck)
                                                      .addComponent(ignorePartsUnderLabel)
                                                      .addComponent(ignoreSparesUnderQualityCB)
                                                      .addComponent(topUpWeeklyCheck)
                                                      .addComponent(topUpButton)
                                                      .addComponent(topUpGMButton)
                                                      .addComponent(resetRequestedStockButton)
                                                      .addComponent(btnClose)));

        setPreferredSize(UIUtil.scaleForGUI(1400, 1000));

    }


    /**
     * @param rating String containing A to F or space, from combo box
     *
     * @return minimum internal quality level to use
     */
    private PartQuality getMinimumQuality(String rating) {
        if (rating == null) {
            rating = " ";
        }
        if (rating.equals(" ")) {
            // The blank spot always means "everything", so minimum = lowest
            return PartQuality.QUALITY_A;
        } else {
            return PartQuality.fromName(rating, campaign.getCampaignOptions().isReverseQualityNames());
        }
    }

    private void refreshOverviewSpecificPart(int row, PartInUse partInUse, IAcquisitionWork newPart) {
        storePartInUseRequestedStock(partInUse);
        if (partInUse.equals(new PartInUse((Part) newPart))) {
            // Simple update
            campaign.updatePartInUse(partInUse, ignoreMothballedCheck.isSelected(),
                  getMinimumQuality((String) ignoreSparesUnderQualityCB.getSelectedItem()));
            overviewPartsModel.fireTableRowsUpdated(row, row);
        } else {
            // Some other part changed; fire a full refresh to be sure
            refreshOverviewPartsInUse();
        }
    }

    private void updateOverviewPartsInUse() {
        overviewPartsModel.setData(campaign.getPartsInUse(ignoreMothballedCheck.isSelected(),
              false, getMinimumQuality((String) ignoreSparesUnderQualityCB.getSelectedItem())));
        TableColumnModel tcm = overviewPartsInUseTable.getColumnModel();
        PartsInUseTableModel.ButtonColumn column = (PartsInUseTableModel.ButtonColumn) tcm
                                                                                             .getColumn(
                                                                                                   PartsInUseTableModel.COL_BUTTON_GM_ADD)
                                                                                             .getCellRenderer();
        column.setEnabled(campaign.isGM());
        column = (PartsInUseTableModel.ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GM_ADD_BULK)
                                                           .getCellRenderer();
        column.setEnabled(campaign.isGM());
        topUpGMButton.setEnabled(campaign.isGM());
    }

    private void refreshOverviewPartsInUse() {
        storePartInUseRequestedStockMap();
        updateOverviewPartsInUse();
    }

    private Set<PartInUse> getPartsInUseFromTable() {
        Set<PartInUse> partsInUse = new HashSet<>();
        for (int row = 0; row < overviewPartsInUseTable.getRowCount(); row++) {
            partsInUse.add(overviewPartsModel.getPartInUse(row));
        }
        return partsInUse;
    }

    private void topUp() {
        campaign.stockUpPartsInUse(getPartsInUseFromTable());
        storePartInUseRequestedStockMap(); // This is necessary to prevent request stock values from resetting when topping up
        refreshOverviewPartsInUse();
    }

    private void topUpGM() {
        campaign.stockUpPartsInUseGM(getPartsInUseFromTable());
        storePartInUseRequestedStockMap(); // This is necessary to prevent request stock values from resetting when topping up
        refreshOverviewPartsInUse();
    }

    public void storePartInUseRequestedStockMap() {
        campaign.setIgnoreMothballed(ignoreMothballedCheck.isSelected());
        campaign.setTopUpWeekly(topUpWeeklyCheck.isSelected());
        if (ignoreSparesUnderQualityCB == null) {
            campaign.setIgnoreSparesUnderQuality(getMinimumQuality(" "));
        } else {
            Object object = ignoreSparesUnderQualityCB.getSelectedItem();

            if (object instanceof String string) {
                campaign.setIgnoreSparesUnderQuality(getMinimumQuality(string));
            }
        }

        Map<String, Double> stockMap = new LinkedHashMap<>();
        for (int row = 0; row < overviewPartsInUseTable.getRowCount(); row++) {
            PartInUse partInUse = overviewPartsModel.getPartInUse(row);
            stockMap.put(partInUse.getDescription(), partInUse.getRequestedStock());
        }
        campaign.setPartsInUseRequestedStockMap(stockMap);
    }

    private void storePartInUseRequestedStock(PartInUse partInUse) {
        Map<String, Double> stockMap = campaign.getPartsInUseRequestedStockMap();
        stockMap.put(partInUse.getDescription(), partInUse.getRequestedStock());
    }

    private void onClose() {
        storePartInUseRequestedStockMap();
    }

    /**
     * Wipes the requested stock numbers back to their defaults
     */
    private void resetRequestedStock() {
        campaign.wipePartsInUseMap();
        updateOverviewPartsInUse();
    }
}
