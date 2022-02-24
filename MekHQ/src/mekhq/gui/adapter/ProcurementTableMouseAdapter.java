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

import megamek.common.Entity;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProcurementTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private final CampaignGUI gui;
    private final JTable table;
    private final ProcurementTableModel model;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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
        if (table.getSelectedRowCount() == 0) {
            return Optional.empty();
        }

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        JMenu menu;
        final int[] rows = table.getSelectedRows();
        final List<IAcquisitionWork> acquisitions = new ArrayList<>();
        for (final int row : rows) {
            if (row < 0) {
                continue;
            }
            model.getAcquisition(table.convertRowIndexToModel(row)).ifPresent(acquisitions::add);
        }

        // Lets fill the popup menu
        menuItem = new JMenuItem(resources.getString("miClearItems.text"));
        menuItem.setToolTipText(resources.getString("miClearItems.toolTipText"));
        menuItem.setName("miClearItems");
        menuItem.addActionListener(evt -> {
            for (final IAcquisitionWork acquisition : acquisitions) {
                model.removeRow(acquisition);
                MekHQ.triggerEvent(new ProcurementEvent(acquisition));
            }
        });
        popup.add(menuItem);

        //region GM Menu
        if (gui.getCampaign().isGM()) {
            popup.addSeparator();

            menu = new JMenu(resources.getString("GMMode.text"));
            menu.setToolTipText(resources.getString("GMMode.toolTipText"));
            menu.setName("menuGMMode");

            menuItem = new JMenuItem(resources.getString("miProcureSingleItemImmediately.text"));
            menuItem.setToolTipText(resources.getString("miProcureSingleItemImmediately.toolTipText"));
            menuItem.setName("miProcureSingleItemImmediately");
            menuItem.addActionListener(evt -> {
                for (final IAcquisitionWork acquisition : acquisitions) {
                    tryProcureOneItem(acquisition);
                }
                model.fireTableDataChanged();
            });
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("miAddSingleItemImmediately.text"));
            menuItem.setToolTipText(resources.getString("miAddSingleItemImmediately.toolTipText"));
            menuItem.setName("miAddSingleItemImmediately");
            menuItem.addActionListener(evt -> {
                for (final IAcquisitionWork acquisition : acquisitions) {
                    addOneItem(acquisition);
                }
                model.fireTableDataChanged();
            });
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("miProcureAllItemsImmediately.text"));
            menuItem.setToolTipText(resources.getString("miProcureAllItemsImmediately.toolTipText"));
            menuItem.setName("miProcureAllItemsImmediately");
            menuItem.addActionListener(evt -> {
                for (final IAcquisitionWork acquisition : acquisitions) {
                    while (acquisition.getQuantity() > 0) {
                        if (!tryProcureOneItem(acquisition)) {
                            break;
                        }
                    }
                }
                model.fireTableDataChanged();
            });
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("miAddAllItemsImmediately.text"));
            menuItem.setToolTipText(resources.getString("miAddAllItemsImmediately.toolTipText"));
            menuItem.setName("miAddAllItemsImmediately");
            menuItem.addActionListener(evt -> {
                for (final IAcquisitionWork acquisition : acquisitions) {
                    while (acquisition.getQuantity() > 0) {
                        addOneItem(acquisition);
                    }
                }
                model.fireTableDataChanged();
            });
            menu.add(menuItem);

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }
        //endregion GM Menu

        return Optional.of(popup);
    }

    /**
     * @param acquisition the
     * @return whether the procurement attempt succeeded or not
     */
    private boolean tryProcureOneItem(final IAcquisitionWork acquisition) {
        if (acquisition.getQuantity() <= 0) {
            LogManager.getLogger().info("Attempted to acquire item with no quantity remaining, ignoring the attempt.");
            return false;
        }
        final Object equipment = acquisition.getNewEquipment();
        final int transitTime = gui.getCampaign().calculatePartTransitTime(0);
        final boolean success;
        if (equipment instanceof Part) {
            success = gui.getCampaign().getQuartermaster().buyPart((Part) equipment, transitTime);
        } else if (equipment instanceof Entity) {
            success = gui.getCampaign().getQuartermaster().buyUnit((Entity) equipment, transitTime);
        } else {
            LogManager.getLogger().error("Attempted to acquire unknown equipment of " + acquisition.getAcquisitionName());
            return false;
        }

        if (success) {
            gui.getCampaign().addReport(String.format(resources.getString("ProcurementTableMouseAdapter.ProcuredItem.report"),
                    acquisition.getAcquisitionName()));
            acquisition.decrementQuantity();
        } else {
            gui.getCampaign().addReport(String.format(resources.getString("ProcurementTableMouseAdapter.CannotAffordToPurchaseItem.report"),
                    acquisition.getAcquisitionName()));
        }
        return success;
    }

    private void addOneItem(final IAcquisitionWork acquisition) {
        if (acquisition.getQuantity() <= 0) {
            LogManager.getLogger().info("Attempted to add item with no quantity remaining, ignoring the attempt.");
            return;
        }

        final Object equipment = acquisition.getNewEquipment();
        if (equipment instanceof Part) {
            gui.getCampaign().getQuartermaster().addPart((Part) equipment, 0);
        } else if (equipment instanceof Entity) {
            gui.getCampaign().addNewUnit((Entity) equipment, false, 0);
        } else {
            LogManager.getLogger().error("Attempted to add unknown equipment of " + acquisition.getAcquisitionName());
            return;
        }

        gui.getCampaign().addReport(String.format(resources.getString("ProcurementTableMouseAdapter.GMAdded.report"),
                acquisition.getAcquisitionName()));
        acquisition.decrementQuantity();
    }
}
