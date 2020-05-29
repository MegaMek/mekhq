/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.unit.UnitOrder;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Collates important information about the campaign and displays it
 *
 */
public final class CommandCenterTab extends CampaignGuiTab {

    private JPanel panCommand;

    // procurement table
    private JTable procurementTable;
    private ProcurementTableModel procurementModel;



    CommandCenterTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    @Override
    public void initTab() {
        panCommand = new JPanel(new GridBagLayout());

        /* Shopping Table */
        procurementModel = new ProcurementTableModel(getCampaign());
        procurementTable = new JTable(procurementModel);
        TableRowSorter<ProcurementTableModel> shoppingSorter = new TableRowSorter<>(
                procurementModel);
        shoppingSorter.setComparator(ProcurementTableModel.COL_COST, new FormattedNumberSorter());
        shoppingSorter.setComparator(ProcurementTableModel.COL_TARGET, new TargetSorter());
        procurementTable.setRowSorter(shoppingSorter);
        TableColumn column;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = procurementTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(procurementModel.getColumnWidth(i));
            column.setCellRenderer(procurementModel.getRenderer());
        }
        procurementTable.setIntercellSpacing(new Dimension(0, 0));
        procurementTable.setShowGrid(false);
        procurementTable.addMouseListener(new ProcurementTableMouseAdapter(getCampaignGui()));
        procurementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0),
                "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                "REMOVE");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
                "REMOVE");

        procurementTable.getActionMap().put("ADD", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 4958203340754214211L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (procurementTable.getSelectedRow() < 0) {
                    return;
                }
                procurementModel
                        .incrementItem(procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()));
            }
        });

        procurementTable.getActionMap().put("REMOVE", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -8377486575329708963L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (procurementTable.getSelectedRow() < 0) {
                    return;
                }
                if (procurementModel
                        .getAcquisition(procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()))
                        .getQuantity() > 0) {
                    procurementModel.decrementItem(
                            procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()));
                }
            }
        });

        JScrollPane scrollProcurement = new JScrollPane(procurementTable);
        JPanel panProcurement = new JPanel(new GridLayout(0, 1));
        panProcurement.setBorder(BorderFactory.createTitledBorder("Active Procurement List"));
        panProcurement.add(scrollProcurement);
        panProcurement.setMinimumSize(new Dimension(200, 200));
        panProcurement.setPreferredSize(new Dimension(200, 200));

        setLayout(new BorderLayout());
        //add(panCommand, BorderLayout.CENTER);
        add(panProcurement, BorderLayout.CENTER);
    }

    @Override
    public void refreshAll() {
        refreshProcurementList();
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.COMMAND;
    }

    private void refreshProcurementList() {
        procurementModel.setData(getCampaign().getShoppingList().getAllShoppingItems());
    }

    private ActionScheduler procurementListScheduler = new ActionScheduler(this::refreshProcurementList);

    @Subscribe
    public void handle(ProcurementEvent ev) {
        procurementListScheduler.schedule();
    }

}
