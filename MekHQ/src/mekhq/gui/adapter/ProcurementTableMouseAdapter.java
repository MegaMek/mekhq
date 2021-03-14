/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import java.util.Optional;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.utilities.JMenuHelpers;

public class ProcurementTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private final CampaignGUI gui;
    private final JTable table;
    private final ProcurementTableModel model;
    //endregion Variable Declarations

    //region Constructors
    protected ProcurementTableMouseAdapter(final CampaignGUI gui, final JTable table,
                                           final ProcurementTableModel model) {
        this.gui = gui;
        this.table = table;
        this.model = model;
    }
    //endregion Constructors

    //region Initialization
    public static void connect(final CampaignGUI gui, final JTable table, final ProcurementTableModel model) {
        new ProcurementTableMouseAdapter(gui, table, model).connect(table);
    }
    //endregion Initialization

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        // GM Only (for now)
        if ((table.getSelectedRowCount() == 0) || !gui.getCampaign().isGM()) {
            return Optional.empty();
        }

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        JMenu menu;
        final int row = table.convertRowIndexToModel(table.getSelectedRow());
        final int[] rows = table.getSelectedRows();
        final boolean oneSelected = table.getSelectedRowCount() == 1;

        // lets fill the pop up menu
        // GM mode
        menu = new JMenu("GM Mode");

        menuItem = new JMenuItem("Procure single item now");
        menuItem.addActionListener(evt -> {
            if (row < 0) {
                return;
            }

            if (oneSelected) {
                model.getAcquisition(row)
                        .ifPresent(ProcurementTableMouseAdapter.this::tryProcureOneItem);
            } else {
                for (int curRow : rows) {
                    if (curRow < 0) {
                        continue;
                    }
                    model.getAcquisition(table.convertRowIndexToModel(curRow))
                            .ifPresent(ProcurementTableMouseAdapter.this::tryProcureOneItem);
                }
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Procure all items now");
        menuItem.addActionListener(evt -> {
            if (row < 0) {
                return;
            }

            if (oneSelected) {
                model.getAcquisition(row)
                        .ifPresent(ProcurementTableMouseAdapter.this::procureAllItems);
            } else {
                for (int curRow : rows) {
                    if (curRow < 0) {
                        continue;
                    }
                    model.getAcquisition(table.convertRowIndexToModel(curRow))
                            .ifPresent(ProcurementTableMouseAdapter.this::procureAllItems);
                }
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Clear From the List");
        menuItem.addActionListener(evt -> {
            if (row < 0) {
                return;
            }

            if (oneSelected) {
                model.getAcquisition(row).ifPresent(a -> {
                    model.removeRow(row);
                    MekHQ.triggerEvent(new ProcurementEvent(a));
                });
            } else {
                for (int curRow : rows) {
                    if (curRow < 0) {
                        continue;
                    }
                    model.getAcquisition(table.convertRowIndexToModel(curRow)).ifPresent(a -> {
                        model.removeRow(row);
                        MekHQ.triggerEvent(new ProcurementEvent(a));
                    });
                }
            }
        });
        menu.add(menuItem);

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        return Optional.of(popup);
    }

    private void procureAllItems(IAcquisitionWork acquisition) {
        while (acquisition.getQuantity() > 0) {
            if (!tryProcureOneItem(acquisition)) {
                break;
            }
        }
    }

    private boolean tryProcureOneItem(IAcquisitionWork acquisition) {
        Object equipment = acquisition.getNewEquipment();
        int transitTime = gui.getCampaign().calculatePartTransitTime(0);
        if (equipment instanceof Part) {
            if (gui.getCampaign().getQuartermaster().buyPart((Part) equipment, transitTime)) {
                reportAcquisitionSuccess(acquisition);
                acquisition.decrementQuantity();
                return true;
            } else {
                reportAcquisitionFailure(acquisition);
            }
        } else if (equipment instanceof Entity) {
            if (gui.getCampaign().getQuartermaster().buyUnit((Entity) equipment, transitTime)) {
                reportAcquisitionSuccess(acquisition);
                acquisition.decrementQuantity();
                return true;
            } else {
                reportAcquisitionFailure(acquisition);
            }
        }

        return false;
    }

    private void reportAcquisitionSuccess(IAcquisitionWork acquisition) {
        gui.getCampaign().addReport(String.format("<font color='Green'><b>Procured %s</b></font>",
                acquisition.getAcquisitionName()));
    }

    private void reportAcquisitionFailure(IAcquisitionWork acquisition) {
        gui.getCampaign().addReport(String.format("<font color='red'><b>You cannot afford to purchase %s</b></font>",
                acquisition.getAcquisitionName()));
    }
}
