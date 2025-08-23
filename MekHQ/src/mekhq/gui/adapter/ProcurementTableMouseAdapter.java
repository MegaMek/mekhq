/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.utilities.ReportingUtilities;

public class ProcurementTableMouseAdapter extends JPopupMenuAdapter {
    private static final MMLogger logger = MMLogger.create(ProcurementTableMouseAdapter.class);

    // region Variable Declarations
    private final CampaignGUI gui;
    private final JTable table;
    private final ProcurementTableModel model;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Constructors
    protected ProcurementTableMouseAdapter(final CampaignGUI gui, final JTable table,
          final ProcurementTableModel model) {
        this.gui = gui;
        this.table = table;
        this.model = model;
    }
    // endregion Constructors

    // region Initialization
    public static void connect(final CampaignGUI gui, final JTable table, final ProcurementTableModel model) {
        new ProcurementTableMouseAdapter(gui, table, model).connect(table);
    }
    // endregion Initialization

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

        // region GM Menu
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
        // endregion GM Menu

        return Optional.of(popup);
    }

    /**
     * @param acquisition the
     *
     * @return whether the procurement attempt succeeded or not
     */
    private boolean tryProcureOneItem(final IAcquisitionWork acquisition) {
        if (acquisition.getQuantity() <= 0) {
            logger.info("Attempted to acquire item with no quantity remaining, ignoring the attempt.");
            return false;
        }
        final Object equipment = acquisition.getNewEquipment();
        final int transitTime = gui.getCampaign().calculatePartTransitTime(acquisition.getAvailability());
        final boolean success;
        if (equipment instanceof Part) {
            success = gui.getCampaign().getQuartermaster().buyPart((Part) equipment, transitTime);
        } else if (equipment instanceof Entity) {
            success = gui.getCampaign().getQuartermaster().buyUnit((Entity) equipment, transitTime);
        } else {
            logger.error("Attempted to acquire unknown equipment of {}", acquisition.getAcquisitionName());
            return false;
        }

        if (success) {
            gui.getCampaign().addReport("<font color='" +
                                              ReportingUtilities.getPositiveColor() +
                                              "'>"
                                              +
                                              String.format(resources.getString(
                                                          "ProcurementTableMouseAdapter.ProcuredItem.report") + "</font>",
                                                    acquisition.getAcquisitionName()));
            acquisition.decrementQuantity();
        } else {
            gui.getCampaign().addReport("<font color='" + ReportingUtilities.getNegativeColor() + "'>"
                                              + String.format(
                  resources.getString("ProcurementTableMouseAdapter.CannotAffordToPurchaseItem.report")
                        + "</font>",
                  acquisition.getAcquisitionName()));
        }
        return success;
    }

    /**
     * Processes the acquisition of a single item, adding it to the campaign as either a part or a unit, or logging an
     * error if the acquisition type is unrecognized.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Checks if the acquisition's quantity is greater than zero. If not, it logs an informational
     *       message and exits without processing.</li>
     *   <li>Determines the type of equipment being acquired:
     *     <ul>
     *       <li>If the equipment is a {@code Part}, it adds the part to the quartermaster's inventory.</li>
     *       <li>If the equipment is an {@code Entity}, it adds a new unit to the campaign. The unit's quality
     *           is determined randomly if the campaign option enables it, otherwise a default quality of
     *           {@code QUALITY_D} is used.</li>
     *       <li>If the equipment type is unrecognized, it logs an error indicating the acquisition's name.</li>
     *     </ul>
     *   </li>
     *   <li>Adds a report to the campaign log indicating successful GM addition of the item.</li>
     *   <li>Decrements the acquisition's remaining quantity after the item is added.</li>
     * </ul>
     *
     * @param acquisition The acquisition work containing the item and metadata to process. Must not be {@code null}.
     *
     * @throws NullPointerException If {@code acquisition} is {@code null}.
     */
    private void addOneItem(final IAcquisitionWork acquisition) {
        if (acquisition.getQuantity() <= 0) {
            logger.info("Attempted to add item with no quantity remaining, ignoring the attempt.");
            return;
        }

        final Object equipment = acquisition.getNewEquipment();
        if (equipment instanceof Part) {
            gui.getCampaign().getQuartermaster().addPart((Part) equipment, 0, true);
        } else if (equipment instanceof Entity) {
            PartQuality quality;

            if (gui.getCampaign().getCampaignOptions().isUseRandomUnitQualities()) {
                quality = Unit.getRandomUnitQuality(0);
            } else {
                quality = PartQuality.QUALITY_D;
            }

            gui.getCampaign().addNewUnit((Entity) equipment, false, 0, quality);
        } else {
            logger.error("Attempted to add unknown equipment of {}", acquisition.getAcquisitionName());
            return;
        }

        gui.getCampaign().addReport("<font color='" +
                                          ReportingUtilities.getPositiveColor() +
                                          "'>"
                                          +
                                          String.format(resources.getString(
                                                      "ProcurementTableMouseAdapter.GMAdded.report") + "</font>",
                                                acquisition.getAcquisitionName()));
        acquisition.decrementQuantity();
    }
}
