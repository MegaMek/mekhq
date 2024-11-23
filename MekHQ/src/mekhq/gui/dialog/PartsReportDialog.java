/*
 * Copyright (c) 2020-2024 The MegaMek Team. All rights reserved.
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.Insets;
import java.util.Collections;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import static javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.swing.util.UIUtil;
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
 * A dialog to show parts in use, ordered, in transit with actionable buttons for buying or adding more
 * taken from the Overview tab originally but now a dialog.
 */
public class PartsReportDialog extends JDialog {

    private JCheckBox ignoreMothballedCheck, topUpWeeklyCheck;
    private JButton topUpButton;
    private JComboBox<String> ignoreSparesUnderQualityCB;
    private JTable overviewPartsInUseTable;
    private PartsInUseTableModel overviewPartsModel;

    private Campaign campaign;
    private CampaignGUI gui;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
        "mekhq.resources.PartsReportDialog", MekHQ.getMHQOptions().getLocale());

    public PartsReportDialog(CampaignGUI gui, boolean modal) {
        super(gui.getFrame(), modal);
        this.gui = gui;
        this.campaign = gui.getCampaign();
        initComponents();
        refreshOverviewPartsInUse();
        pack();
        setLocationRelativeTo(gui.getFrame());
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
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GMADD, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GMADD_BULK, false);
        // Numeric columns
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_USE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_STORED, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_TONNAGE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_REQUSTED_STOCK, new FormattedNumberSorter());
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
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                campaign.getShoppingList().addShoppingItem(partToBuy, 1, campaign);
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };

        Action buyInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1,
                        CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                if (quantity <= 0) {
                    return;
                }
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                campaign.getShoppingList().addShoppingItem(partToBuy, quantity, campaign);
                refreshOverviewSpecificPart(row, piu, partToBuy);
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
                    if (spare.getSellableQuantity() >= sellQty) {
                        quartermaster.sellPart(spare, sellQty);
                        break;
                    } else {
                        // Not enough quantity in this spare, so sell them all and move onto the next one
                        quartermaster.sellPart(spare, spare.getQuantity());
                        sellQty -= spare.getSellableQuantity();
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
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                campaign.getQuartermaster().addPart((Part) partToBuy.getNewEquipment(), 0);
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        Action addInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1,
                        CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                while (quantity > 0) {
                    campaign.getQuartermaster().addPart((Part) partToBuy.getNewEquipment(), 0);
                    --quantity;
                }
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };

        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buy, PartsInUseTableModel.COL_BUTTON_BUY);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buyInBulk,
                PartsInUseTableModel.COL_BUTTON_BUY_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, sell, PartsInUseTableModel.COL_BUTTON_SELL);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, sellInBulk,
            PartsInUseTableModel.COL_BUTTON_SELL_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, add, PartsInUseTableModel.COL_BUTTON_GMADD);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, addInBulk,
                PartsInUseTableModel.COL_BUTTON_GMADD_BULK);


        JScrollPane tableScroll = new JScrollPaneWithSpeed(overviewPartsInUseTable);

        ignoreMothballedCheck = new JCheckBox(resourceMap.getString("chkIgnoreMothballed.text"));
        ignoreMothballedCheck.addActionListener(evt -> refreshOverviewPartsInUse());

        topUpWeeklyCheck = new JCheckBox(resourceMap.getString("chkTopUpWeekly.text"));
        topUpWeeklyCheck.addActionListener(evt -> topUpWeekly());

        topUpButton = new JButton();
        topUpButton.setText(resourceMap.getString("topUpBtn.text"));
        topUpButton.setIcon(null);
        topUpButton.setFocusPainted(false);
        topUpButton.setEnabled(true);
        topUpButton.setBorder(null);
        topUpButton.setMargin(new Insets(10,20,10,20));

        boolean reverse = campaign.getCampaignOptions().isReverseQualityNames();
        String[] qualities = {
            " ", // Combo box is blank for first one because it accepts everything and is default
            PartQuality.QUALITY_B.toName(reverse),
            PartQuality.QUALITY_C.toName(reverse),
            PartQuality.QUALITY_D.toName(reverse),
            PartQuality.QUALITY_E.toName(reverse),
            PartQuality.QUALITY_F.toName(reverse)
        };

        ignoreSparesUnderQualityCB = new JComboBox<String>(qualities);
        ignoreSparesUnderQualityCB.setMaximumSize(ignoreSparesUnderQualityCB.getPreferredSize());
        ignoreSparesUnderQualityCB.addActionListener(evt -> refreshOverviewPartsInUse());
        JLabel ignorePartsUnderLabel = new JLabel(resourceMap.getString("lblIgnoreSparesUnderQuality.text"));

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(evt -> setVisible(false));
        
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(tableScroll)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createSequentialGroup()
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ignorePartsUnderLabel)
                    .addComponent(ignoreSparesUnderQualityCB)
                    .addComponent(ignoreMothballedCheck)
                    .addComponent(topUpWeeklyCheck)
                    .addComponent(topUpButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnClose))));

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(tableScroll)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(ignoreMothballedCheck)
                .addComponent(ignorePartsUnderLabel)
                .addComponent(ignoreSparesUnderQualityCB)
                .addComponent(topUpWeeklyCheck)
                .addComponent(topUpButton)
                .addComponent(btnClose)));

        setPreferredSize(UIUtil.scaleForGUI(1400,1000));

    }


    /**
     * @param rating String containing A to F or space, from combo box
     * @return minimum internal quality level to use
     */
    private PartQuality getMinimumQuality(String rating) {
        if (rating.equals(" ")) {
            // The blank spot always means "everything", so minimum = lowest
            return PartQuality.QUALITY_A;
        } else {
            return PartQuality.fromName(rating, campaign.getCampaignOptions().isReverseQualityNames());
        }
    }

    private void refreshOverviewSpecificPart(int row, PartInUse piu, IAcquisitionWork newPart) {
        if (piu.equals(new PartInUse((Part) newPart))) {
            // Simple update
            campaign.updatePartInUse(piu, ignoreMothballedCheck.isSelected(),
                    getMinimumQuality((String) ignoreSparesUnderQualityCB.getSelectedItem()));
            overviewPartsModel.fireTableRowsUpdated(row, row);
        } else {
            // Some other part changed; fire a full refresh to be sure
            refreshOverviewPartsInUse();
        }
    }

    private void refreshOverviewPartsInUse() {
        overviewPartsModel.setData(campaign.getPartsInUse(ignoreMothballedCheck.isSelected(),
                getMinimumQuality((String) ignoreSparesUnderQualityCB.getSelectedItem())));
        TableColumnModel tcm = overviewPartsInUseTable.getColumnModel();
        PartsInUseTableModel.ButtonColumn column = (PartsInUseTableModel.ButtonColumn) tcm
                .getColumn(PartsInUseTableModel.COL_BUTTON_GMADD)
                .getCellRenderer();
        column.setEnabled(campaign.isGM());
        column = (PartsInUseTableModel.ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GMADD_BULK)
                .getCellRenderer();
        column.setEnabled(campaign.isGM());
    }

    private void topUp() {

    }

    private void topUpWeekly() {

    }
}
